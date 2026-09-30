package com.example.nfcapp.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.nfcapp.data.DefaultDataRepository
import com.example.nfcapp.R
import com.example.nfcapp.theme.NFCAppTheme



@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  nfcReadStatus: NfcReadStatus,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository()) },
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  Column(modifier) {
    NfcStatusMessage(nfcReadStatus)
    when (state) {
      MainScreenUiState.Loading -> {
        // Blank
      }
      is MainScreenUiState.Success -> {
        MainScreen(data = (state as MainScreenUiState.Success).data)
      }
      is MainScreenUiState.Error -> {
        Text("Error loading data: ${(state as MainScreenUiState.Error).throwable.message}")
      }
    }
  }
}

@Composable
internal fun NfcStatusMessage(status: NfcReadStatus) {
  val messageId = when (status) {
    NfcReadStatus.Ready -> R.string.nfc_ready
    NfcReadStatus.Reading -> R.string.nfc_reading
    NfcReadStatus.Completed -> R.string.nfc_read_completed
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

@Composable
internal fun MainScreen(data: List<String>, modifier: Modifier = Modifier) {
  Column(modifier) { data.forEach { Greeting(it) } }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  NFCAppTheme { MainScreen(listOf("Android")) }
}

@Preview(showBackground = true, widthDp = 340)
@Composable
fun MainScreenPortraitPreview() {
  NFCAppTheme { MainScreen(listOf("Android")) }
}
