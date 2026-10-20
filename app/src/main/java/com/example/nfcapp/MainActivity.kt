package com.example.nfcapp

import android.nfc.NfcAdapter
import android.nfc.FormatException
import android.nfc.TagLostException
import android.nfc.tech.NfcV
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.nfcapp.theme.NFCAppTheme
import com.example.nfcapp.ui.main.NfcReadFailure
import com.example.nfcapp.ui.main.NfcReadStatusStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {

    @Volatile
    private var readerModeEnabledAtNanos = 0L

    @Volatile
    private var skipNdefCheck = true

    private val ntag213Reader = Ntag213Reader()
    private val businessIdParser: BusinessIdParser = UnspecifiedBusinessIdParser
    private val readInProgress = AtomicBoolean(false)
    private val nfcReadStatusStore = NfcReadStatusStore()

    // Activityが前景になってNFCを使う時点まで、端末のNFCアダプター取得を遅延する。
    private val nfcAdapter by lazy {
        NfcAdapter.getDefaultAdapter(this)
    }

    // NFCのByteは符号付きなので、ログでは0x00〜0xFFの2桁16進数へ変換する。
    private fun Byte.toHex(): String =
        "%02X".format(toInt() and 0xFF)

    // UID・リクエスト・レスポンスを、バイト境界が分かる形式で表示する。
    private fun ByteArray.toHex(): String =
        joinToString(":") { it.toHex() }

    // Reader Modeの入口。重複コールバックを抑え、タグの技術に応じてNFC-VかNTAG213の読取へ進む。
    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        // 前回の読取中に再検出されても、同じActivityからタグへ二重接続しない。
        if (!readInProgress.compareAndSet(false, true)) return@ReaderCallback
        nfcReadStatusStore.onTagDiscovered()
        // 検出時間の計測はタグの内容に依存せず、コールバックを受けた時点で記録する。
        val callbackAtNanos = SystemClock.elapsedRealtimeNanos()
        val sinceEnableMillis =
            (callbackAtNanos - readerModeEnabledAtNanos) / NANOS_PER_MILLISECOND
        Log.d(
            TIMING_LOG_TAG,
            "event=callback skipNdef=$skipNdefCheck " +
                    "elapsedRealtimeNanos=$callbackAtNanos " +
                    "sinceEnableMs=%.3f".format(sinceEnableMillis)
        )

        // connect()とtransceive()はブロッキングI/Oなので、メインスレッドでは実行しない。
        lifecycleScope.launch(Dispatchers.IO) {
            try {
            // 技術別の処理へ分岐する前に、タグの共通情報を取得する。
            val id = tag.id.toHex()
            val technologies = tag.techList.toList()

            // NFC-Vを優先する。ISO-DEPはNTAG213のRaw READ対象外とする。
            val nfcV = NfcV.get(tag)

            if (nfcV == null) {
                // NFC-AだけのタグはReaderでNTAG213か判定し、それ以外は対象外。
                if (routeForTechnologies(technologies) != NfcTagRoute.NTAG213) {
                    Log.w("MainActivity", "Unsupported tag technologies: $technologies")
                    nfcReadStatusStore.onReadFailed(NfcReadFailure.UNSUPPORTED_TAG)
                    return@launch
                }
                // 読取で失敗した理由を分類して記録し、次のタグ検出を継続する。
                try {
                    // タグI/OとNDEFの比較はReader、業務IDへの変換はParserに分ける。
                    val read = ntag213Reader.read(tag)
                    val businessId = businessIdParser.parse(read)
                    // UIDやNDEF本文は出さず、比較結果と長さだけを検証用に記録する。
                    Log.d(
                        "MainActivity",
                        "NTAG213 read: comparison=${read.comparison}, " +
                            "platformNdefBytes=${read.platformNdefMessage?.size}, " +
                            "rawNdefBytes=${read.rawNdefMessage?.size}, " +
                            "userMemoryBytes=${read.userMemory.size}, " +
                            "businessIdPresent=${businessId != null}"
                    )
                    nfcReadStatusStore.onReadCompleted(formatRawMemory(read.userMemory))
                } catch (e: UnsupportedTagException) {
                    Log.w("MainActivity", "Unsupported NFC-A tag: ${e.message}")
                    nfcReadStatusStore.onReadFailed(NfcReadFailure.UNSUPPORTED_TAG)
                } catch (e: TagLostException) {
                    Log.w("MainActivity", "NTAG213 left the field", e)
                    nfcReadStatusStore.onReadFailed(NfcReadFailure.TAG_LOST)
                } catch (e: IOException) {
                    Log.e("MainActivity", "NTAG213 I/O failed", e)
                    nfcReadStatusStore.onReadFailed(NfcReadFailure.IO)
                } catch (e: FormatException) {
                    Log.e("MainActivity", "Malformed NDEF message", e)
                    nfcReadStatusStore.onReadFailed(NfcReadFailure.INVALID_DATA)
                } catch (e: SecurityException) {
                    Log.e("MainActivity", "NTAG213 handle is no longer valid", e)
                    nfcReadStatusStore.onReadFailed(NfcReadFailure.IO)
                } catch (e: IllegalArgumentException) {
                    Log.e("MainActivity", "Malformed Type 2 TLV", e)
                    nfcReadStatusStore.onReadFailed(NfcReadFailure.INVALID_DATA)
                }
                return@launch
            }

            // ここからNFC-Vの処理。検出時にAndroidが取得した情報は接続せず参照できる。
            val discoveryFlags = nfcV.responseFlags.toInt() and 0xFF
            val dsfId = nfcV.dsfId.toInt() and 0xFF

            Log.d(
                "MainActivity",
                "ID: $id, Technologies: $technologies, " +
                        "DiscoveryResponseFlags: 0x%02X, DSFID: 0x%02X, MaxTransceive: %d"
                            .format(discoveryFlags, dsfId, nfcV.maxTransceiveLength)
            )

            try {
                // 以降のRaw I/Oを実行できるよう、NFC-Vタグとの接続を開く。
                nfcV.connect()

                // SLIのユーザー領域をすべて読めた場合だけ、整形した一覧を公開する。
                val memory = readIcodeSliUserMemory(nfcV::transceive)
                Log.d("MainActivity", "NFC-V user memory read: bytes=${memory.size}")
                nfcReadStatusStore.onReadCompleted(formatRawMemory(memory))
            } catch (e: TagLostException) {
                Log.e("MainActivity", "Tag left the field", e)
                nfcReadStatusStore.onReadFailed(NfcReadFailure.TAG_LOST)
            } catch (e: InvalidNfcVResponseException) {
                Log.e("MainActivity", "Malformed NFC-V response", e)
                nfcReadStatusStore.onReadFailed(NfcReadFailure.INVALID_DATA)
            } catch (e: IOException) {
                Log.e("MainActivity", "NFC-V I/O failed", e)
                nfcReadStatusStore.onReadFailed(NfcReadFailure.IO)
            } finally {
                // 成功・失敗・タグ離脱のどの場合でも、接続とNFCリソースを解放する。
                runCatching { nfcV.close() }
                    .onFailure { Log.e("MainActivity", "Failed to close NFC-V", it) }
            }
            } finally {
                // どの分岐・例外から抜けても、次のタグ検出を受け付けられるようにする。
                readInProgress.set(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // NFCの検出処理とは独立して、通常のCompose画面を構築する。
        enableEdgeToEdge()
        setContent {
            val nfcReadStatus by nfcReadStatusStore.status.collectAsStateWithLifecycle()
            NFCAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) { MainNavigation(nfcReadStatus) }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // 通常はNDEFの自動検出を有効にし、Androidの読取結果とRaw READを比較できるようにする。
        // Intentで指定した場合だけ自動検出をスキップし、その差を検証する。
        skipNdefCheck = intent.getBooleanExtra(EXTRA_SKIP_NDEF_CHECK, false)
        val readerFlags =
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_V or
                    if (skipNdefCheck) NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK else 0

        // Activityが前景にありNFCが有効な間だけ、Reader Modeでタグを待ち受ける。
        nfcAdapter
            ?.takeIf { adapter -> adapter.isEnabled }
            ?.also { adapter ->
                nfcReadStatusStore.onReady()
                readerModeEnabledAtNanos = SystemClock.elapsedRealtimeNanos()
                Log.d(
                    TIMING_LOG_TAG,
                    "event=enable skipNdef=$skipNdefCheck " +
                            "elapsedRealtimeNanos=$readerModeEnabledAtNanos"
                )
                adapter.enableReaderMode(
                    this,
                    readerCallback,
                    readerFlags,
                    null,
                )
            } ?: nfcReadStatusStore.onReadFailed(NfcReadFailure.NFC_UNAVAILABLE)
    }

    override fun onPause() {
        super.onPause()

        // Activityが背景へ移るときに読取を止め、端末を通常のNFC動作へ戻す。
        nfcAdapter?.disableReaderMode(this)
    }

    private companion object {
        const val EXTRA_SKIP_NDEF_CHECK = "skipNdefCheck"
        const val TIMING_LOG_TAG = "NfcTiming"
        const val NANOS_PER_MILLISECOND = 1_000_000.0
    }
}
