package com.example.nfcapp

import android.nfc.FormatException
import android.nfc.TagLostException
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import java.io.IOException
import java.util.concurrent.CancellationException

/** タグ種別に依存しない、Data層から上位へ返す読取エラー。元の例外も保持する。 */
sealed interface NfcReadError {
    val cause: Exception

    data class TagLost(override val cause: TagLostException) : NfcReadError
    data class Io(override val cause: IOException) : NfcReadError
    data class InvalidData(override val cause: Exception) : NfcReadError
    data class UnsupportedTag(override val cause: Exception) : NfcReadError
    data class AccessDenied(override val cause: SecurityException) : NfcReadError
    data class Unexpected(override val cause: Exception) : NfcReadError
}

/**
 * NFC-A / NFC-Vなどのタグ種別に依存せず、処理の戻り値をOk、例外をErrへ変換する。
 *
 * 使用例: `nfcResult { ntag213Reader.read(tag) }`
 * コルーチンのキャンセルとJVMのErrorはエラー結果にせず、そのまま呼び出し元へ伝える。
 * I/Oの実行スレッドやリトライは、この関数では変更しない。
 *
 * kotlin-result: https://github.com/michaelbull/kotlin-result#creating-results
 */
fun <T> nfcResult(block: () -> T): Result<T, NfcReadError> =
    runCatching(block).mapError { it.toNfcReadError() }

internal fun Throwable.toNfcReadError(): NfcReadError =
    when (this) {
        // runCatchingはThrowableを捕捉するため、キャンセルとJVMのErrorは再送出する。
        is CancellationException -> throw this
        // TagLostExceptionはIOExceptionの派生型なので、先に判定する。
        is TagLostException -> NfcReadError.TagLost(this)
        is UnsupportedTagException -> NfcReadError.UnsupportedTag(this)
        is IOException -> NfcReadError.Io(this)
        is FormatException, is IllegalArgumentException -> NfcReadError.InvalidData(this)
        is SecurityException -> NfcReadError.AccessDenied(this)
        is Exception -> NfcReadError.Unexpected(this)
        else -> throw this
    }
