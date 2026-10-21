package com.example.nfcapp

import android.nfc.FormatException
import android.nfc.TagLostException
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
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
    try {
        Ok(block())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        val error = when (exception) {
            // TagLostExceptionはIOExceptionの派生型なので、先に判定する。
            is TagLostException -> NfcReadError.TagLost(exception)
            is UnsupportedTagException -> NfcReadError.UnsupportedTag(exception)
            is IOException -> NfcReadError.Io(exception)
            is FormatException, is IllegalArgumentException -> NfcReadError.InvalidData(exception)
            is SecurityException -> NfcReadError.AccessDenied(exception)
            else -> NfcReadError.Unexpected(exception)
        }
        Err(error)
    }
