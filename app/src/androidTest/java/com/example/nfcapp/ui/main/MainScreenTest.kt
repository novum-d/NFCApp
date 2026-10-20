package com.example.nfcapp.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.example.nfcapp.formatRawMemory
import com.example.nfcapp.theme.NFCAppTheme
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun completedMemoryCanBeScrolledToTheLastItem() {
        val items = formatRawMemory(ByteArray(112) { it.toByte() })
        composeTestRule.setContent {
            NFCAppTheme { MainScreen(NfcReadStatus.Completed(items)) }
        }
        composeTestRule.onNodeWithText("読み取ったメモリ：28件").assertIsDisplayed()
        composeTestRule.onNodeWithText("HEX 00 01 02 03").assertIsDisplayed()
        composeTestRule.onNodeWithTag("raw_memory_list")
            .performScrollToNode(hasText("オフセット 0x006C"))
        composeTestRule.onNodeWithText("HEX 6C 6D 6E 6F").assertIsDisplayed()
        composeTestRule.onNodeWithText("ASCII lmno").assertIsDisplayed()
    }

    @Test fun newReadAndFailureDoNotDisplayThePreviousTagsMemory() {
        val status = mutableStateOf<NfcReadStatus>(
            NfcReadStatus.Completed(formatRawMemory(byteArrayOf(0x41, 0x42, 0x43, 0x44)))
        )
        composeTestRule.setContent { NFCAppTheme { MainScreen(status.value) } }
        composeTestRule.onNodeWithText("ASCII ABCD").assertIsDisplayed()

        composeTestRule.runOnIdle { status.value = NfcReadStatus.Reading }
        composeTestRule.onNodeWithText("NFCタグを読み取り中です").assertIsDisplayed()
        composeTestRule.onNodeWithText("ASCII ABCD").assertDoesNotExist()

        composeTestRule.runOnIdle {
            status.value = NfcReadStatus.Failed(NfcReadFailure.TAG_LOST)
        }
        composeTestRule.onNodeWithText("読み取り中にタグが離れました。もう一度かざしてください")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("raw_memory_list").assertDoesNotExist()
    }

    @Test fun waitingScreenDoesNotDisplayTemplateOrSampleData() {
        composeTestRule.setContent { NFCAppTheme { MainScreen(NfcReadStatus.Ready) } }
        composeTestRule.onNodeWithText("NFCタグをかざしてください").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello Android!").assertDoesNotExist()
        composeTestRule.onNodeWithTag("raw_memory_list").assertDoesNotExist()
    }
}
