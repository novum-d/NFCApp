package com.example.nfcapp

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import java.io.Closeable

/** Resultの失敗経路でも接続を閉じ、最初の読取エラーを保持する。 */
internal fun <T> useNfcConnection(
    connection: Closeable,
    block: () -> Result<T, NfcReadError>,
): Result<T, NfcReadError> {
    val result = try {
        block()
    } catch (failure: Throwable) {
        // useと同様、キャンセルなどの例外時も閉じる。closeの例外は元の例外に添える。
        try {
            connection.close()
        } catch (closeFailure: Throwable) {
            if (failure !== closeFailure) failure.addSuppressed(closeFailure)
        }
        throw failure
    }

    val closeResult = try {
        nfcResult { connection.close() }
    } catch (closeFailure: Throwable) {
        if (result.isErr) {
            return result.onFailure { error -> error.cause.addSuppressed(closeFailure) }
        }
        throw closeFailure
    }

    // 読取とcloseの両方が失敗した場合、読取エラーをcloseのエラーで上書きしない。
    result.onFailure { readError ->
        closeResult.onFailure { closeError ->
            if (readError.cause !== closeError.cause) {
                readError.cause.addSuppressed(closeError.cause)
            }
        }
    }
    return result.andThen { value -> closeResult.map { value } }
}
