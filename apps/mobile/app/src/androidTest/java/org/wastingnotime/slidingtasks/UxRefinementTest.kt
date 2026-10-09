package org.wastingnotime.slidingtasks

import android.graphics.Bitmap
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wastingnotime.slidingtasks.data.LocalTaskStore
import org.wastingnotime.slidingtasks.model.*

class UxRefinementTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun seedTasks() {
        val today = LocalDate.now()
        LocalTaskStore(compose.activity).save(SlidingTasksState(tasks = listOf(
            PlannedTask("read", "Read a chapter", TaskType.ONE_TIME, TaskSchedule(ScheduleKind.ONCE, startsOn = today), true, today),
            PlannedTask("walk", "Take a short walk", TaskType.ROUTINE, TaskSchedule(ScheduleKind.DAILY, startsOn = today), true, today),
            PlannedTask("review", "Review priorities", TaskType.ROUTINE, TaskSchedule(ScheduleKind.ONCE_PER_WEEK, days = allWeekDays, startsOn = today), true, today),
        )))
        compose.activityRule.scenario.recreate()
    }

    @Test fun task_scope_and_backup_destination_are_discoverable() {
        compose.onNodeWithText("One-time · Skip task").assertExists()
        compose.onNodeWithText("Routine · Skip today").assertExists()
        compose.onNodeWithText("Until decided · Skip this week").assertExists()
        screenshot("today")
        compose.onNodeWithTag("nav-plan").performClick()
        screenshot("plan")
        compose.onNodeWithTag("more-options").performClick()
        compose.onNodeWithTag("backup-menu-item").performClick()
        compose.onNodeWithTag("export-tasks").assertExists()
        compose.onNodeWithTag("choose-backup-folder").assertExists()
        screenshot("backup")
    }

    @Test fun editor_preview_explains_scope_and_initial_review_has_one_learning_message() {
        compose.onNodeWithTag("nav-review").performClick()
        compose.onNodeWithTag("review-patterns").assertDoesNotExist()
        compose.onNodeWithTag("review-trend").assertDoesNotExist()
        screenshot("review")
        compose.onNodeWithTag("nav-plan").performClick()
        compose.onNodeWithTag("plan-review").performClick()
        compose.onNodeWithTag("schedule-preview").performScrollTo()
            .assertTextContains("One occurrence; Done or Skip closes the active week.", substring = true)
        screenshot("editor-weekly")
        compose.onNodeWithTag("mode-one_time").performScrollTo().performClick()
        compose.onNodeWithTag("schedule-preview").performScrollTo()
            .assertTextContains("carries forward", substring = true)
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "ux-refinement").apply { mkdirs() }
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
