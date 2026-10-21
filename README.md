# NFCApp

NFCのRaw I/Oをボトムアップで実装する学習用アプリです。このブランチではData層までを実装しています。

## 今回のデータ構造

対象はICODE SLIの**実ブロック番号1〜4**です。1ブロックは4バイトで、先頭から2バイトずつを1データとして扱います。

| 実ブロック番号 | バイト0〜1 | バイト2〜3 |
|---|---|---|
| 1 | データ1 | データ2 |
| 2 | データ3 | データ4 |
| 3 | データ5 | データ6 |
| 4 | データ7 | データ8 |

合計16バイトから8件の `RawMemoryItem` を作り、`ImmutableList` で返します。

- `blockNumber`: タグ上の実ブロック番号。
- `byteOffset`: そのブロック内でデータが始まるバイト位置。今回の配置では0または2。
- `hex`: 2バイトを大文字・空白区切りで表した文字列。例: `"01 FF"`。

バイト列の変換は `ByteArray` の拡張関数です。`bytes.toDisplayHex()`、`bytes.parseBlockItems(blockNumber = 1)`、`bytes.parseMemoryItems()` のように、対象のバイト列から直接呼び出せます。

2バイトの数値化や文字コードの解釈は行いません。タグから取得したバイト順を保ちます。

## NFCの処理の流れ

```text
ReaderCallbackが受け取ったTag
  → IcodeSliReader.read(tag)
  → nfcResult { ... } の中でNfcVを取得
  → andThenでconnect()の成功後だけ読取へ進む
  → binding内でreadNfcVBlock(...).bind()をブロック1〜4へ順番に実行
  → 各応答の先頭にあるResponse Flagsを除き、4バイトずつ連結
  → parseMemoryItems()で16バイトを8件のデータに変換
  → useNfcConnectionがclose()を呼び、接続を解放
  → Ok(ImmutableList<RawMemoryItem>)を返す
```

送信例は次のとおりです。

```text
02 20 01  → 実ブロック1の読取
02 20 02  → 実ブロック2の読取
02 20 03  → 実ブロック3の読取
02 20 04  → 実ブロック4の読取
```

`02` は高速・非アドレス指定・OptionなしのFlags、`20` はRead Single Blockです。CRCはAndroidが付加するため、アプリ側では付けません。Optionなしの正常応答は、Response Flags 1バイトとデータ4バイトです。

例えば応答が `00 10 11 12 13` なら、データとして使うのは `10 11 12 13` です。先頭の `00` はデータに含めません。

接続と読取の連結は `IcodeSliReader`、接続の解放は `NfcConnection.kt`、1回の送受信は `NfcVBlockReader.kt`、配置とHEX表示への変換は `RawMemoryItem.kt` / `ByteArrayHex.kt` が担当します。

## ROPによるResultの連結

1ブロックの読取は `readNfcVBlock()` が `Result<ByteArray, NfcReadError>` として返します。連続読取は、このResultを `binding` / `.bind()` で連結します。

```kotlin
binding {
    val memory = ByteArrayOutputStream(targetBlockNumbers.count() * BYTES_PER_BLOCK)
    for (blockNumber in targetBlockNumbers) {
        val blockBytes = readNfcVBlock(blockNumber, transceive).bind()
        memory.write(blockBytes)
    }
    memory.toByteArray().parseMemoryItems()
}
```

`bind()` は `Ok` の値を取り出します。`Err` ならそのエラーを返して `binding` を打ち切るため、後続ブロックの送信と変換は実行しません。`readNfcVBlock()` のResultを利用し、内部関数から例外を投げ直して上位で再変換する処理は設けていません。

タグ取得、接続、読取の順序は `andThen` で連結しています。`useNfcConnection` は成功・Err・キャンセルの各経路で接続を閉じます。読取と切断が両方失敗した場合は、元の読取エラーを保持し、切断時の例外を `cause.suppressed` に追加します。

`binding` と `andThen` は追加済みの `kotlin-result:2.0.1` に含まれます。この同期読取に追加の依存関係は必要ありません。[公式のbindingの説明](https://github.com/michaelbull/kotlin-result#binding)

## Data層APIの呼び出し例

`connect()` と `transceive()` はブロッキングI/Oなので、メインスレッド以外から呼びます。以下は、ReaderCallbackから受け取ったTagをData層へ渡す処理のサンプルです。

```kotlin
import android.nfc.Tag
import com.example.nfcapp.IcodeSliReader
import com.example.nfcapp.NfcReadError
import com.example.nfcapp.RawMemoryItem
import com.github.michaelbull.result.Result
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun loadItems(tag: Tag): Result<ImmutableList<RawMemoryItem>, NfcReadError> =
    withContext(Dispatchers.IO) {
        IcodeSliReader().read(tag)
    }
```

この段階では新しいData層APIをActivityや画面へ接続していません。既存ActivityのNFC-V読取は、ブロック0を1回読み、完了状態を表示する従来の処理です。新APIの結果を画面へ渡す実装は次の工程で行います。

## 共通の例外変換

`nfcResult { ... }` は、ラムダの最後の値を `Ok` に包み、発生した例外を `Err(NfcReadError)` に変換します。タグ種別を引数に取らないため、他のタグの取得処理でも使用できます。

例えば、既存のNTAG213読取を同じ形式で呼ぶ場合は次のように書けます。

```kotlin
val result: Result<Ntag213Read, NfcReadError> = nfcResult {
    Ntag213Reader().read(tag)
}
```

`IcodeSliReader.read()` は既に内部で `nfcResult` を使っているため、そのまま呼び出せます。

| 発生した例外 | 返すエラー型 |
|---|---|
| `TagLostException` | `NfcReadError.TagLost` |
| `UnsupportedTagException` | `NfcReadError.UnsupportedTag` |
| その他の `IOException` | `NfcReadError.Io` |
| `FormatException` / `IllegalArgumentException` | `NfcReadError.InvalidData` |
| `SecurityException` | `NfcReadError.AccessDenied` |
| その他の `Exception` | `NfcReadError.Unexpected` |

元の例外は `cause` に保持します。コルーチンの `CancellationException` とJVMの `Error` は捕捉せず、そのまま伝えます。

Data層ではエラー型への変換と、ROPによって同じエラーを上位へ伝えるところまで扱っています。上位でのエラー表示、リトライ、タグ応答の詳細なエラーコード別処理は今後扱います。ブロック読取には、エラー応答をデータと誤認しないためのFlags・応答長の確認を入れています。

## テストと確認範囲

```shell
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

今回追加したテストはData層の正常系、共通例外変換、ROPの失敗経路と接続解放の基本動作を対象にしています。

- HEX表示と2バイト単位への分割。
- 実ブロック番号1〜4・ブロック内のバイト位置の保持。
- Read Single Blockの送信内容と、正常応答からの4バイト抽出。
- 実ブロック1〜4への4回の送信順序と、取得データから8件への変換。
- 共通ラッパーの正常値、I/O・不正データ・権限例外の変換、キャンセルの再送出。
- 途中の読取エラーで後続ブロックを送信せず、同じ原因を上位へ伝えること。
- 成功・Err・キャンセル後の接続解放と、読取エラー・切断エラーの優先順位。

JVMテストでは送受信関数に模擬応答を渡しています。実タグとの通信、Androidの接続・切断、対象端末での読取は未確認です。新規のViewModel・UIテストは追加していません。

## 参考資料・サンプル

- [Android公式: Advanced NFC overview](https://developer.android.com/develop/connectivity/nfc/advanced-nfc#read-write): `TagTechnology` の取得、`connect()`、読取、`use` / `close()` のコード例。接続から解放までの流れを読むのに役立ちます。
- [Android公式: NfcV](https://developer.android.com/reference/android/nfc/tech/NfcV): `get()`、`connect()`、`transceive()`、`close()` の仕様。特に `transceive()` のCRCと実行スレッドの説明を参照してください。
- [AOSP公式: CardReaderサンプル](https://android.googlesource.com/platform/developers/samples/android/+/master/connectivity/nfc/CardReader/): Reader Modeから低レベル通信へ進む実装例です。IsoDep用のサンプルなので、NFC-Vのコマンド部分は本アプリの `NfcVBlockReader.kt` を参照してください。
- [NXP公式: ICODE SLIデータシート](https://www.nxp.com/docs/en/data-sheet/058031.pdf): 「Memory Organisation」「Supported Commands」「Read single block」を参照してください。
- [kotlin-result公式](https://github.com/michaelbull/kotlin-result#creating-results): `Ok` / `Err` による成功・失敗の表現と、結果の変換例。
