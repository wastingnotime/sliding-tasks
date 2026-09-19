package com.wastingnotime.slidingtasks

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import org.junit.Rule
import org.junit.Test

class SlidingCardTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun sliding_the_active_card_right_completes_it_and_advances_the_stack() {
        composeRule.onNodeWithTag("card-card-1").performTouchInput { swipeRight(durationMillis = 500) }

        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("2 cards left").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("remaining-count").assertTextEquals("2 cards left")
    }
}
