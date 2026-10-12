package org.wastingnotime.slidingtasks

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import org.wastingnotime.slidingtasks.data.LocalTaskStore
import org.wastingnotime.slidingtasks.model.*
import java.io.File
import java.time.LocalDate
import java.util.Locale

class FirstUseGuideTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun freshInstallation() {
        val context = compose.activity
        context.getSharedPreferences("sliding_tasks", 0).edit().clear().commit()
        context.getSharedPreferences("first_use", 0).edit().clear().commit()
        context.getSharedPreferences("scheduled_backup", 0).edit().clear().commit()
        compose.runOnUiThread { context.setContent { key("guide-test") { SlidingTasksApp() } } }
        Intents.init()
    }
    @After fun releaseIntents() {
        Intents.release()
        compose.activity.getSharedPreferences("first_use", 0).edit().clear().commit()
    }

    private fun click(tag: String) {
        if (tag in listOf("finish-guide", "guide-go-to-today")) {
            compose.onNodeWithTag("review-list").performScrollToNode(hasTestTag(tag))
        }
        val node = compose.onNodeWithTag(tag)
        if (tag !in listOf("skip-guide", "guide-back", "finish-guide", "guide-go-to-today", "add-task", "save-task", "cancel-edit")) node.performScrollTo()
        node.performClick()
    }
    @Test fun completed_practice_can_go_to_today_without_creating_a_task() {
        compose.activity.getSharedPreferences("first_use", 0).edit()
            .putInt("step", 3)
            .putString("decisions", "DONE,DISMISSED,DONE")
            .commit()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("review-list").performScrollToNode(hasTestTag("guide-go-to-today"))
        compose.onNodeWithText("Practice complete. You’re ready to use Sliding Tasks.").assertExists()
        compose.onNodeWithTag("finish-guide").assertTextContains("Create your first task")
        click("guide-go-to-today")
        compose.onNodeWithTag("practice-guide").assertDoesNotExist()
        compose.onNodeWithTag("task-title").assertDoesNotExist()
        compose.onNodeWithText("The present is handled.").assertExists()
        assertTrue(compose.activity.getSharedPreferences("first_use", 0).getBoolean("completed", false))
        assertNoUserTasks()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("practice-guide").assertDoesNotExist()
    }

    @Test fun sliding_practice_can_miss_cards_recreate_undo_and_review_without_saving_tasks() {
        compose.activity.getSharedPreferences("first_use", 0).edit().putInt("step", 2).commit()
        compose.activityRule.scenario.recreate()
        click("practice-let-day-pass")
        compose.onAllNodes(hasTestTagPrefix("card-practice-")).assertCountEquals(0)
        compose.onNodeWithTag("guide-review").assertIsEnabled()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("practice-let-day-pass").assertDoesNotExist()
        click("practice-undo")
        compose.onAllNodes(hasTestTagPrefix("card-practice-")).assertCountEquals(1)
        compose.onNodeWithTag("guide-review").assertIsNotEnabled()
        click("practice-let-day-pass")
        click("guide-review")
        compose.onNodeWithTag("review-day-${LocalDate.now()}").performScrollTo().performClick()
        compose.onAllNodesWithText("MISSED", useUnmergedTree = true).assertCountEquals(3)
        assertNoUserTasks()
    }

    private fun completePlanExercises(titles: List<String> = listOf("Daily walk", "Weekly priorities", "Dentist appointment")) {
        listOf("routine", "until_decided", "one_time").forEachIndexed { index, mode ->
            compose.onNodeWithTag("plan-exercise").assertTextContains("${index + 1}", substring = true)
            compose.onNodeWithTag("guide-practice").assertDoesNotExist()
            click("guide-plan")
            compose.onNodeWithTag("add-task").assertIsNotEnabled()
            compose.onNodeWithTag("task-title").performScrollTo().performTextInput(titles[index])
            compose.onNodeWithTag("task-title").performImeAction()
            if (index > 0) compose.onNodeWithTag("add-task").assertIsNotEnabled()
            click("mode-$mode")
            screenshot("plan-before-save-$index")
            compose.onNodeWithTag("mode-$mode").assertIsSelected()
            compose.onNodeWithTag("add-task").assertIsEnabled()
            click("add-task")
            compose.onNodeWithText(titles[index]).assertExists()
            if (titles.first() == "Daily walk") screenshot("plan-$mode")
            if (index < 2) click("guide-next-plan")
        }
    }
    private fun slidePractice(right: Boolean) {
        val card = compose.onAllNodes(hasTestTagPrefix("card-practice-")).fetchSemanticsNodes().single()
        val tag = card.config[androidx.compose.ui.semantics.SemanticsProperties.TestTag]
        // Step 2 teaches the swipe; assistive technology still has card actions.
        compose.onNodeWithTag("practice-done").assertDoesNotExist()
        compose.onNodeWithTag("practice-skip").assertDoesNotExist()
        assertEquals(2, card.config[androidx.compose.ui.semantics.SemanticsActions.CustomActions].size)
        compose.onNodeWithTag(tag).performScrollTo().performTouchInput {
            if (right) swipeRight(durationMillis = 500) else swipeLeft(durationMillis = 500)
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }
    }

    private fun assertNoUserTasks() {
        val state = LocalTaskStore(compose.activity).load()
        assertTrue(state.tasks.isEmpty()); assertTrue(state.cards.isEmpty()); assertTrue(state.events.isEmpty())
    }

    @Test fun first_use_can_start_immediately_and_reopen_guide_without_adding_examples() {
        compose.onNodeWithTag("guide-restore").assertExists()
        screenshot("welcome")
        click("start-without-guide")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("nav-today").assertIsDisplayed()
        compose.onNodeWithTag("more-options").performClick()
        compose.onNodeWithTag("about-menu-item").performClick()
        click("reopen-guide")
        click("skip-guide")
        compose.onNodeWithTag("nav-today").assertIsDisplayed()
        assertNoUserTasks()
    }

    @Test fun swiping_undo_review_and_recreation_keep_practice_out_of_real_storage() {
        click("learn-guide")
        screenshot("types")
        completePlanExercises()
        click("guide-practice")
        val first = compose.onAllNodes(hasTestTagPrefix("card-practice-")).fetchSemanticsNodes().single().config[androidx.compose.ui.semantics.SemanticsProperties.TestTag]
        slidePractice(right = true)
        compose.onNodeWithTag("practice-result").assertTextContains("Take a short walk", substring = true)
        click("practice-undo")
        compose.onNodeWithTag(first).assertExists()
        slidePractice(right = true)
        screenshot("practice")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("practice-result").assertTextContains("Take a short walk", substring = true)
        slidePractice(right = false)
        slidePractice(right = false)
        click("practice-undo")
        compose.onAllNodes(hasTestTagPrefix("card-practice-")).assertCountEquals(1)
        slidePractice(right = true)
        click("guide-review")
        compose.onNodeWithTag("review-week").assertExists()
        screenshot("review")
        compose.onNodeWithTag("review-day-${LocalDate.now()}").performScrollTo().performClick()
        compose.onNodeWithText("Book a dentist appointment").assertExists()
        compose.onNodeWithTag("review-list").performScrollToNode(hasTestTag("finish-guide"))
        val dayBottom = compose.onNodeWithTag("review-day-${LocalDate.now()}").getUnclippedBoundsInRoot().bottom
        val finishTop = compose.onNodeWithTag("finish-guide").getUnclippedBoundsInRoot().top
        assertTrue("Finish follows all expanded Review results", finishTop >= dayBottom)
        screenshot("review-finish")
        assertNoUserTasks()
        click("finish-guide")
        compose.onNodeWithTag("task-title").assertIsDisplayed()
        assertNoUserTasks()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("practice-guide").assertDoesNotExist()
    }

    @Test fun planning_practice_preserves_drafts_and_saved_plans_without_creating_real_tasks() {
        // Use the production composition before testing saved editor state across recreation.
        compose.activityRule.scenario.recreate()
        click("learn-guide")
        click("guide-plan")
        compose.onNodeWithTag("task-title").performTextInput("Call a friend")
        click("mode-until_decided")
        compose.onNodeWithTag("add-task").assertIsNotEnabled()
        compose.onNodeWithTag("guide-required-type").assertExists()
        click("mode-routine")
        click("repeat-weekly_days")
        click("repeat-interval-2")
        click("day-sunday")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("task-title").assertTextContains("Call a friend")
        compose.onNodeWithTag("mode-routine").assertIsSelected()
        compose.onNodeWithTag("repeat-interval-2").assertIsSelected()
        compose.onNodeWithTag("day-sunday").assertIsSelected()
        click("add-task")
        compose.onNodeWithText("Call a friend").assertIsDisplayed()
        screenshot("practice-plan")
        assertNoUserTasks()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Call a friend").assertIsDisplayed()
        click("guide-plan")
        // An empty weekly selection should explain what is missing and disable saving.
        listOf("monday", "tuesday", "wednesday", "thursday", "friday", "sunday").forEach { click("day-$it") }
        compose.onNodeWithTag("save-task").assertIsNotEnabled()
        click("day-monday")
        click("save-task")
        val preferences = compose.activity.getSharedPreferences("first_use", 0)
        val saved = LocalTaskStore(compose.activity).readImport(requireNotNull(preferences.getString("practice_plan", null)))
        assertEquals(ScheduleKind.WEEKLY_DAYS, saved.tasks.single().schedule.kind)
        click("guide-plan")
        compose.onNodeWithTag("task-title").performTextReplacement("Discard this edit")
        click("cancel-edit")
        compose.onNodeWithText("Call a friend").assertIsDisplayed()
        click("guide-plan")
        click("guide-back")
        compose.onNodeWithTag("practice-plan-result").assertIsDisplayed()
        click("skip-guide")
        assertFalse(preferences.contains("practice_plan"))
        assertFalse(preferences.contains("plan_editing"))
        assertFalse(preferences.contains("plan_exercise"))
        assertNoUserTasks()
        compose.onNodeWithTag("more-options").performClick()
        compose.onNodeWithTag("about-menu-item").performClick()
        click("reopen-guide")
        compose.onNodeWithTag("practice-plan-result").assertDoesNotExist()
        click("guide-plan")
        compose.onNodeWithTag("add-task").assertIsNotEnabled()
    }

    @Test fun sliding_practice_restarts_when_reentered_from_plan_or_review() {
        click("learn-guide")
        completePlanExercises()
        click("guide-practice")
        slidePractice(right = true)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("practice-result").assertExists()
        compose.onNodeWithText("Review this week’s priorities").assertExists()
        // Back to Plan, then start sliding again: no previous decisions or card state.
        click("guide-back")
        click("guide-practice")
        compose.onNodeWithText("Take a short walk").assertExists()
        compose.onNodeWithTag("practice-result").assertDoesNotExist()
        compose.onNodeWithTag("practice-undo").assertDoesNotExist()
        compose.onNodeWithTag("guide-review").assertIsNotEnabled()
        slidePractice(right = true)
        slidePractice(right = false)
        slidePractice(right = true)
        click("guide-review")
        compose.onNodeWithTag("review-week").assertExists()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        screenshot("sliding-after-back")
        compose.onNodeWithText("Take a short walk").assertExists()
        compose.onNodeWithTag("practice-result").assertDoesNotExist()
        compose.onNodeWithTag("practice-undo").assertDoesNotExist()
        compose.onNodeWithTag("guide-review").assertIsNotEnabled()
        assertEquals("", compose.activity.getSharedPreferences("first_use", 0).getString("decisions", null))
        assertNoUserTasks()
    }

    @Test fun cancelled_and_failed_restores_keep_welcome_choices_available() {
        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(ActivityResult(Activity.RESULT_CANCELED, null))
        click("guide-restore")
        compose.onNodeWithTag("guide-restore-error").assertExists()
        compose.onNodeWithTag("learn-guide").assertExists()
        Intents.release(); Intents.init()
        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(ActivityResult(Activity.RESULT_OK,
            Intent().setData(Uri.parse("content://org.wastingnotime.slidingtasks.test.restore/invalid"))))
        click("guide-restore")
        compose.onNodeWithTag("guide-restore-error").assertTextContains("Could not import", substring = true)
        assertNoUserTasks()
        click("start-without-guide")
        compose.onNodeWithTag("nav-today").assertIsDisplayed()
    }

    @Test fun successful_restore_bypasses_guide_and_preserves_restored_task() {
        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(ActivityResult(Activity.RESULT_OK,
            Intent().setData(Uri.parse("content://org.wastingnotime.slidingtasks.test.restore/valid"))))
        click("guide-restore")
        compose.onNodeWithTag("practice-guide").assertDoesNotExist()
        compose.onNodeWithText("Restored task").assertExists()
        assertEquals("restored", LocalTaskStore(compose.activity).load().tasks.single().id)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("practice-guide").assertDoesNotExist()
    }

    @Test fun existing_history_bypasses_first_use() {
        val day = LocalDate.now()
        LocalTaskStore(compose.activity).save(practiceGuideState(day, listOf("Existing walk", "Existing week", "Existing once"), listOf("DONE")))
        compose.runOnUiThread { compose.activity.setContent { key("existing-user") { SlidingTasksApp() } } }
        compose.onNodeWithTag("practice-guide").assertDoesNotExist()
        compose.onNodeWithTag("nav-today").assertIsDisplayed()
    }

    @Test fun finishing_reopened_guide_returns_to_existing_app_without_opening_task_editor() {
        val day = LocalDate.now()
        LocalTaskStore(compose.activity).save(practiceGuideState(day, listOf("Existing walk", "Existing week", "Existing once"), listOf("DONE")))
        compose.runOnUiThread { compose.activity.setContent { key("existing-user-guide") { SlidingTasksApp() } } }
        compose.onNodeWithTag("more-options").performClick()
        compose.onNodeWithTag("about-menu-item").performClick()
        click("reopen-guide")
        completePlanExercises()
        click("guide-practice")
        repeat(3) { slidePractice(right = true) }
        click("guide-review")
        compose.onNodeWithTag("review-list").performScrollToNode(hasTestTag("finish-guide"))
        compose.onNodeWithTag("finish-guide").assertTextContains("Back to your tasks")
        click("finish-guide")
        compose.onNodeWithTag("task-title").assertDoesNotExist()
        compose.onNodeWithTag("nav-today").assertIsDisplayed()
        assertTrue(compose.activity.getSharedPreferences("first_use", 0).getBoolean("completed", false))
    }

    @Test fun portuguese_large_text_dark_guide_keeps_actions_reachable() {
        compose.runOnUiThread {
            val config = Configuration(compose.activity.resources.configuration).apply {
                setLocale(Locale.forLanguageTag("pt-BR")); fontScale = 2f
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
            }
            val localized = compose.activity.createConfigurationContext(config)
            compose.activity.setContent {
                key("localized-guide") {
                    CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalContext provides localized,
                        LocalConfiguration provides config,
                        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                            compose.activity.resources.displayMetrics.density, config.fontScale),
                        androidx.activity.compose.LocalActivityResultRegistryOwner provides compose.activity,
                    ) { SlidingTasksApp() }
                }
            }
        }
        compose.onNodeWithText("Mostre como funciona").performScrollTo().performClick()
        screenshot("types-pt-large-dark")
        completePlanExercises(listOf("Caminhar", "Revisar prioridades", "Marcar consulta"))
        compose.onNodeWithTag("guide-practice").performScrollTo()
        screenshot("practice-plan-pt-large-dark")
        click("guide-practice")
        slidePractice(right = true)
        slidePractice(right = false)
        slidePractice(right = true)
        click("guide-review")
        screenshot("review-pt-large-dark")
        click("finish-guide")
        compose.onNodeWithTag("task-title").assertExists()
        assertFalse(compose.activity.getSharedPreferences("first_use", 0).contains("practice_plan"))
        assertNoUserTasks()
    }

    private fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("Tag starts with $prefix") {
        it.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { "" }.startsWith(prefix)
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val dir = File(compose.activity.getExternalFilesDir(null), "first-use-guide").apply { mkdirs() }
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
