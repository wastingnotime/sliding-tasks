package org.wastingnotime.slidingtasks

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wastingnotime.slidingtasks.data.LocalTaskStore
import org.wastingnotime.slidingtasks.model.ScheduleKind
import org.wastingnotime.slidingtasks.model.SkipScope
import org.wastingnotime.slidingtasks.model.PlannedTask
import org.wastingnotime.slidingtasks.model.SlidingTasksState
import org.wastingnotime.slidingtasks.model.TaskSchedule
import org.wastingnotime.slidingtasks.model.TaskType
import org.wastingnotime.slidingtasks.model.weekdays
import java.time.DayOfWeek
import java.time.LocalDate

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

    @Test
    fun privacy_policy_is_available_offline_in_the_app() {
        composeRule.onNodeWithTag("more-options").performClick()
        composeRule.onNodeWithTag("about-menu-item").performClick()
        composeRule.onAllNodesWithText("Sliding Tasks is provided", substring = true)[0]
            .assertExists()
        composeRule.onAllNodesWithText("stored on this device", substring = true)[0]
            .assertExists()
    }

    @Test
    fun about_shows_installed_version_guide_and_wnt() {
        composeRule.onNodeWithTag("more-options").performClick()
        composeRule.onNodeWithTag("about-menu-item").performClick()

        composeRule.onAllNodesWithText("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")[0]
            .assertExists()
        composeRule.onAllNodesWithText("Quick guide")[0].assertExists()
        composeRule.onAllNodesWithText("slide a card right for Done", substring = true)[0]
            .assertExists()
        composeRule.onAllNodesWithText("Wasting No Time (WNT)", substring = true)[0]
            .assertExists()
    }

    @Test
    fun legacy_schedules_and_card_actions_survive_storage_migration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val legacy = """{
            "activeDate":"2026-09-18",
            "tasks":[
                {"id":"weekday","title":"Work","type":"ROUTINE","recurrence":"WEEKDAYS","active":true,"createdOn":"2026-09-18","startsOn":"2026-09-18"},
                {"id":"fortnight","title":"Haircut","type":"ROUTINE","recurrence":"EVERY_TWO_WEEKS","availableDays":"WEEKDAYS","active":true,"createdOn":"2026-09-18","startsOn":"2026-09-18"},
                {"id":"vitamin","title":"Vitamin","type":"ROUTINE","recurrence":"EVERY_TWO_DAYS","active":true,"createdOn":"2026-09-18","startsOn":"2026-09-19"}
            ],
            "cards":[{"id":"card","taskId":"fortnight","boardDate":"2026-09-18","title":"Haircut","type":"ROUTINE","status":"PENDING","touches":0}],
            "events":[]
        }""".trimIndent()
        context.getSharedPreferences("sliding_tasks", 0).edit().putString("state-v1", legacy).commit()
        val store = LocalTaskStore(context)

        val migrated = store.load()
        assertEquals(ScheduleKind.WEEKLY_DAYS, migrated.tasks[0].schedule.kind)
        assertEquals(weekdays, migrated.tasks[0].schedule.days)
        assertEquals(ScheduleKind.ONCE_PER_WEEK, migrated.tasks[1].schedule.kind)
        assertEquals(2, migrated.tasks[1].schedule.interval)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), migrated.tasks[1].schedule.days)
        assertEquals(2, migrated.tasks[2].schedule.interval)
        assertEquals(SkipScope.WEEK, migrated.cards.single().skipScope)

        store.save(migrated)
        assertEquals(migrated, store.load())
    }

    @Test
    fun plan_editor_saves_a_custom_weekly_day_schedule() {
        composeRule.onNodeWithTag("nav-plan").performClick()
        composeRule.onNodeWithTag("add-entry").performClick()
        composeRule.onNodeWithTag("repeat-interval-7").assertExists()
        composeRule.onNodeWithTag("repeat-interval-8").assertDoesNotExist()
        composeRule.onNodeWithTag("task-title").performTextInput("Exercise")
        composeRule.onNodeWithTag("task-title").performImeAction()
        composeRule.onNodeWithTag("repeat-weekly_days").performClick()
        composeRule.onNodeWithTag("repeat-interval-4").assertExists()
        composeRule.onNodeWithTag("repeat-interval-5").assertDoesNotExist()
        composeRule.onNodeWithTag("repeat-interval-2").performClick()
        composeRule.onNodeWithTag("day-monday").performClick()
        composeRule.onNodeWithTag("day-wednesday").performClick()
        composeRule.onNodeWithTag("day-thursday").performClick()
        composeRule.onNodeWithTag("add-task").performClick()

        composeRule.onAllNodesWithText("Routine · Every 2 weeks · Tue, Fri")[0].assertExists()
    }

    @Test
    fun editing_an_existing_long_interval_preserves_it_until_changed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val today = LocalDate.now()
        val store = LocalTaskStore(context)
        store.save(SlidingTasksState(tasks = listOf(PlannedTask(
            id = "long-interval",
            title = "Long cycle",
            type = TaskType.ROUTINE,
            schedule = TaskSchedule(ScheduleKind.DAILY, interval = 10, startsOn = today),
            active = true,
            createdOn = today,
        ))))
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("nav-plan").performClick()
        composeRule.onNodeWithTag("edit-long-interval").performClick()
        composeRule.onNodeWithTag("repeat-daily").performClick()
        composeRule.onAllNodesWithText("currently set to every 10 days", substring = true)[0]
            .assertExists()
        composeRule.onNodeWithTag("save-task").performClick()
        assertEquals(10, store.load().tasks.single().schedule.interval)
    }

    @Test
    fun plan_can_create_a_one_time_task_that_appears_today() {
        composeRule.onNodeWithTag("nav-plan").performClick()
        composeRule.onNodeWithTag("add-entry").performClick()
        composeRule.onNodeWithTag("mode-one_time").performClick()
        composeRule.onNodeWithTag("repeat-interval-1").assertDoesNotExist()
        composeRule.onNodeWithTag("task-title").performTextInput("Send invoice")
        composeRule.onNodeWithTag("task-title").performImeAction()
        composeRule.onNodeWithTag("add-task").performClick()
        composeRule.onAllNodesWithText("One-time")[0].assertExists()
        composeRule.onNodeWithTag("nav-today").performClick()
        composeRule.onAllNodesWithText("Send invoice")[0].assertExists()
        composeRule.onAllNodesWithText("left to skip task", substring = true)[0].assertExists()
        composeRule.onAllNodesWithText("Send invoice")[0]
            .performTouchInput { swipeRight(durationMillis = 500) }
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("0 cards left").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav-plan").performClick()
        assertEquals(0, composeRule.onAllNodesWithText("Send invoice").fetchSemanticsNodes().size)
    }

    @Test
    fun plan_labels_a_weekly_occurrence_until_decided() {
        composeRule.onNodeWithTag("nav-plan").performClick()
        composeRule.onNodeWithTag("add-entry").performClick()
        composeRule.onNodeWithTag("mode-until_decided").performClick()
        composeRule.onNodeWithTag("repeat-weekly_days").assertDoesNotExist()
        composeRule.onNodeWithTag("repeat-interval-2").performClick()
        composeRule.onNodeWithTag("task-title").performTextInput("Check mail")
        composeRule.onNodeWithTag("task-title").performImeAction()
        composeRule.onNodeWithTag("add-task").performClick()

        composeRule.onAllNodesWithText("Until decided · Every 2 weeks · Weekdays")[0]
            .assertExists()
    }
}
