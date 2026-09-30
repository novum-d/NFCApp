package com.example.nfcapp.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class NfcStatusMessageTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun tagLostPromptsUserToPresentTagAgain() {
        composeTestRule.setContent {
            NfcStatusMessage(NfcReadStatus.Failed(NfcReadFailure.TAG_LOST))
        }
        composeTestRule.onNodeWithText("読み取り中にタグが離れました。もう一度かざしてください").assertExists()
    }
}
