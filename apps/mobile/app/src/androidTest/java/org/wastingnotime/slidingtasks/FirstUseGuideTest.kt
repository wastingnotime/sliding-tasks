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
    @After fun releaseIntents() { Intents.release() }

    private fun click(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (tag != "skip-guide" && tag != "guide-back") node.performScrollTo()
        node.performClick()
    }
    private fun startPractice() { click("learn-guide"); click("guide-practice") }
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
        click("guide-practice")
        val first = compose.onAllNodes(hasTestTagPrefix("card-practice-")).fetchSemanticsNodes().single().config[androidx.compose.ui.semantics.SemanticsProperties.TestTag]
        compose.onNodeWithTag(first).performTouchInput { swipeRight(durationMillis = 500) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("practice-result").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("practice-result").assertTextContains("Take a short walk", substring = true)
        click("practice-undo")
        compose.onNodeWithTag(first).assertExists()
        click("practice-done")
        screenshot("practice")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("practice-result").assertTextContains("Take a short walk", substring = true)
        val second = compose.onAllNodes(hasTestTagPrefix("card-practice-")).fetchSemanticsNodes().single().config[androidx.compose.ui.semantics.SemanticsProperties.TestTag]
        compose.onNodeWithTag(second).performTouchInput { swipeLeft(durationMillis = 500) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Skip task").fetchSemanticsNodes().isNotEmpty() }
        click("practice-skip")
        click("practice-undo")
        compose.onNodeWithTag("practice-done").assertExists()
        click("practice-done")
        click("guide-review")
        compose.onNodeWithTag("review-week").assertExists()
        screenshot("review")
        compose.onNodeWithTag("review-day-${LocalDate.now()}").performScrollTo().performClick()
        compose.onNodeWithText("Book a dentist appointment").assertExists()
        assertNoUserTasks()
        click("finish-guide")
        compose.onNodeWithTag("task-title").assertIsDisplayed()
        assertNoUserTasks()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("practice-guide").assertDoesNotExist()
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

    @Test fun portuguese_large_text_dark_guide_keeps_actions_reachable() {
        compose.runOnUiThread {
            val config = Configuration(compose.activity.resources.configuration).apply {
                setLocale(Locale.forLanguageTag("pt-BR")); fontScale = 2f
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
            }
            val localized = compose.activity.createConfigurationContext(config)
            compose.activity.setContent {
                key("localized-guide") {
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalContext provides localized, LocalConfiguration provides config, androidx.activity.compose.LocalActivityResultRegistryOwner provides compose.activity) { SlidingTasksApp() }
                }
            }
        }
        compose.onNodeWithText("Mostre como funciona").performScrollTo().performClick()
        screenshot("types-pt-large-dark")
        click("guide-practice")
        click("practice-done")
        click("practice-skip")
        click("practice-done")
        click("guide-review")
        screenshot("review-pt-large-dark")
        click("finish-guide")
        compose.onNodeWithTag("task-title").assertExists()
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
