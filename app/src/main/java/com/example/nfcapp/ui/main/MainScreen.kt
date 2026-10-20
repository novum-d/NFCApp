package com.example.nfcapp.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nfcapp.R
import com.example.nfcapp.RawMemoryItem
import com.example.nfcapp.formatRawMemory
import com.example.nfcapp.theme.NFCAppTheme

@Composable
fun MainScreen(nfcReadStatus: NfcReadStatus, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.memory_title), style = MaterialTheme.typography.headlineMedium)
        NfcStatusMessage(nfcReadStatus)
        when (nfcReadStatus) {
            NfcReadStatus.Reading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            is NfcReadStatus.Completed -> {
                if (nfcReadStatus.items.isNotEmpty()) {
                    Text(
                        stringResource(R.string.memory_count, nfcReadStatus.items.size),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.memory_format_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    RawMemoryList(nfcReadStatus.items, Modifier.weight(1f))
                } else {
                    Text(stringResource(R.string.memory_empty))
                }
            }
            else -> Text(stringResource(R.string.memory_waiting))
        }
    }
}

@Composable
internal fun RawMemoryList(memoryItems: List<RawMemoryItem>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().testTag("raw_memory_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(memoryItems, key = { it.offset }) { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.memory_offset, item.offset),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    SelectionContainer {
                        Column {
                            Text(
                                stringResource(R.string.memory_hex, item.hex),
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(
                                stringResource(R.string.memory_ascii, item.ascii),
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NfcStatusMessage(status: NfcReadStatus) {
    val messageId = when (status) {
        NfcReadStatus.Ready -> R.string.nfc_ready
        NfcReadStatus.Reading -> R.string.nfc_reading
        is NfcReadStatus.Completed -> R.string.nfc_read_completed
        is NfcReadStatus.Failed -> when (status.reason) {
            NfcReadFailure.TAG_LOST -> R.string.nfc_tag_lost
            NfcReadFailure.IO -> R.string.nfc_io_failed
            NfcReadFailure.INVALID_DATA -> R.string.nfc_invalid_data
            NfcReadFailure.UNSUPPORTED_TAG -> R.string.nfc_unsupported_tag
            NfcReadFailure.NFC_UNAVAILABLE -> R.string.nfc_unavailable
        }
    }
    Text(stringResource(messageId))
}

@Preview(showBackground = true, widthDp = 340, heightDp = 640)
@Composable
fun MainScreenPreview() {
    NFCAppTheme {
        MainScreen(
            NfcReadStatus.Completed(formatRawMemory(byteArrayOf(0x41, 0x42, 0x00, 0x7F))),
            Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 340, heightDp = 640)
@Composable
fun MainScreenReadyPreview() {
    NFCAppTheme { MainScreen(NfcReadStatus.Ready, Modifier.padding(16.dp)) }
}
