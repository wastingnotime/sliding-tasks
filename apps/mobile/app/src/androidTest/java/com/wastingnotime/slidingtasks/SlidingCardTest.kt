package com.wastingnotime.slidingtasks

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SlidingCardTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun startWithEmptyLocalState() {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("sliding_tasks", 0)
            .edit()
            .clear()
            .commit()
        composeRule.activityRule.scenario.recreate()
    }

    @Test
    fun planning_then_sliding_a_card_right_completes_it() {
        composeRule.onNodeWithTag("nav-plan").performClick()
        composeRule.onNodeWithTag("add-entry").performClick()
        composeRule.onNodeWithTag("task-title").performTextInput("Write the project brief")
        composeRule.onNodeWithTag("add-task").performClick()
        composeRule.onNodeWithTag("nav-today").performClick()
        composeRule.onAllNodesWithText("Write the project brief")[0]
            .performTouchInput { swipeRight(durationMillis = 500) }

        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("0 cards left").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("remaining-count").assertTextEquals("0 cards left")
    }

    @Test
    fun planned_task_survives_activity_recreation() {
        composeRule.onNodeWithTag("nav-plan").performClick()
        composeRule.onNodeWithTag("add-entry").performClick()
        composeRule.onNodeWithTag("task-title").performTextInput("Persist this plan")
        composeRule.onNodeWithTag("add-task").performClick()

        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("nav-plan").performClick()

        composeRule.onAllNodesWithText("Persist this plan")[0].assertTextEquals("Persist this plan")
    }
}
