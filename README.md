# NFCApp

AndroidのReader Modeでタグを読み取り、ユーザーメモリを一覧表示する検証用アプリです。

## Raw I/Oによるメモリ一覧

タグをかざすと読み取り中の表示になり、対象領域の読み取りがすべて成功した後に一覧を表示します。各項目は4バイト単位で、次を表示します。

- 読み取ったユーザー領域の先頭からのオフセット（16進数）
- バイト列の16進数表現（HEX）
- 印字可能なASCII文字。制御文字・ASCII範囲外のバイトは`.`として表示

例えば`41 42 00 FF`は、HEX欄にそのまま、ASCII欄に`AB..`と表示します。オフセットは読み取った領域内での相対位置であり、タグ全体の物理アドレスではありません。

業務IDやアイテムのレコード形式は未定義のため、メモリを4バイトずつ表示する段階の実装です。文字コードやレコード境界を推測した業務データへの変換は行いません。

### 読み取り対象

| 経路 | 読み取り・表示する範囲 |
|---|---|
| NFC-V | ICODE SLI向けのブロック0〜27、4バイト×28＝112バイト |
| NFC-A | 既存のNTAG213 Readerが取得する144バイトのユーザーメモリ |

NFC-Vは既存と同じ非アドレス指定のRead Single Block（`02 20 <block>`）を順に送信します。ICの型番・メモリ構成を自動判定する実装ではないため、ほかのNFC-V製品の全メモリを列挙する機能とは区別してください。

NFC-Aのコマンド、ページ範囲、タイムアウト、Raw読取からAndroidのNDEF読取へ進む順序は維持しています。Reader Modeのフラグ、開始・終了のライフサイクル、I/Oディスパッチャー、読取中の重複コールバック抑止も既存のままです。

### 失敗時の扱い

- NFC-Vの応答フラグ・エラー・データ長を検査し、応答フラグをメモリ本文へ混ぜません。
- 読取途中の離脱、通信失敗、不正な応答では、途中までのメモリを完了結果として表示しません。
- 次のタグ検出・待受への再開・失敗通知時には、前回の結果一覧をクリアします。
- 自動再試行は追加せず、既存どおり再提示による新しい検出を待ちます。接続は成功・失敗のいずれでも閉じます。

タグへの書き込み、NDEFフォーマット、ロック操作は追加していません。メモリ本文はNFC-Vの成功ログへ出力せず、読み取ったサイズだけを記録します。

## コードの責務

- `IcodeSliMemoryReader.kt`：単一ブロックREADの繰り返しと応答検証。接続・切断は`MainActivity`が管理
- `RawMemoryItem.kt`：Android APIに依存しない、バイト列から表示項目への整形
- `NfcReadStatusStore.kt`：読取状態と、その完了結果の一覧
- `MainScreen.kt`：状態表示と、スクロールできるメモリ一覧

固定の`Hello Android!`を表示していたテンプレートのRepository・ViewModelは、読取結果に置き換えたため削除しました。

## ビルド・テスト

JDK 17とAndroid SDKを用意し、`local.properties`でSDKの位置を設定します。

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

エミュレーターまたは端末を接続した場合のUIテスト：

```sh
./gradlew :app:connectedDebugAndroidTest
```

### 確認記録（2026-10-20）

- 変更前：既存のJVMテストとDebugビルドが成功。
- 変更後：JVMテスト33件とDebugビルドが成功。正常応答、ブロック範囲、エラー、不正な長さ、途中のI/O失敗、表示整形、結果のクリアを確認。
- Android 15エミュレーターでUIテスト4件を実施。一覧のHEX・ASCII表示、最終項目までのスクロール、新しい読取・失敗時の旧データ非表示、待受表示を確認。
- 実タグ・対象タブレットは未接続。実際のNFC通信、NFC-A／NFC-Vの回帰比較、読取時間、離脱・再提示、画面復帰の挙動は実機での確認が必要。

単体テストの通信応答とUIテストのデータは合成データで、実タグの読取成功を示すものではありません。

## 参照

- [Android Developers — NfcV](https://developer.android.com/reference/android/nfc/tech/NfcV)
- [Android Developers — Advanced NFC overview](https://developer.android.com/develop/connectivity/nfc/advanced-nfc)
- [NXP — ICODE SLIデータシート](https://www.nxp.com/docs/en/data-sheet/058031.pdf)
