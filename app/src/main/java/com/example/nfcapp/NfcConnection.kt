package com.example.nfcapp

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import java.io.Closeable

/** Resultの失敗経路でも接続を閉じ、最初の読取エラーを保持する。 */
internal fun <T> useNfcConnection(
    connection: Closeable,
    block: () -> Result<T, NfcReadError>,
): Result<T, NfcReadError> {
    val readAttempt = runCatching(block)
    // 読取がErrでも例外でも一度だけ閉じ、その後で最初の失敗を上位へ伝える。
    val closeAttempt = runCatching { connection.close() }
    val result = readAttempt.getOrElse { failure ->
        closeAttempt.onFailure { closeFailure ->
            if (failure !== closeFailure) failure.addSuppressed(closeFailure)
        }
        throw failure
    }

    // 読取とcloseの両方が失敗した場合、読取エラーをcloseのエラーで上書きしない。
    result.onFailure { readError ->
        closeAttempt.onFailure { closeFailure ->
            if (readError.cause !== closeFailure) {
                readError.cause.addSuppressed(closeFailure)
            }
        }
    }
    return result.andThen { value ->
        closeAttempt.mapError { it.toNfcReadError() }.map { value }
    }
}
