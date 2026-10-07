package org.wastingnotime.slidingtasks

import android.graphics.Bitmap
import android.graphics.Rect
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowInsets
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.SdkSuppress
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Run on devices with gesture/three-button navigation and display cutouts. */
@SdkSuppress(minSdkVersion = 30)
class EdgeToEdgeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun landscape_navigation_stays_clear_of_side_insets() {
        try {
            composeRule.runOnIdle {
                composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            composeRule.waitUntil(10_000) {
                composeRule.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
            // Rotation is a system compositor animation, outside Compose's idle clock.
            android.os.SystemClock.sleep(1_000)
            for (section in listOf("today", "plan", "review")) {
                composeRule.onNodeWithTag("nav-$section").performClick()
                for (navigation in listOf("today", "plan", "review")) assertSafe("nav-$navigation")
                screenshot("landscape-$section")
            }
        } finally {
            composeRule.runOnIdle {
                composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    @Test
    fun controls_stay_clear_of_system_bars_cutouts_and_keyboard() {
        assertSafe("more-options")
        for (section in listOf("today", "plan", "review")) {
            composeRule.onNodeWithTag("nav-$section").performClick()
            for (navigation in listOf("today", "plan", "review")) {
                assertSafe("nav-$navigation")
            }
            screenshot(section)
        }
        composeRule.onNodeWithTag("nav-plan").performClick()
        assertSafe("add-entry")
        composeRule.onNodeWithTag("add-entry").performClick()
        assertSafe("cancel-edit")
        assertSafe("task-title")
        assertSafe("add-task")
        screenshot("editor")
        composeRule.onNodeWithTag("task-title").performClick()
        composeRule.onNodeWithTag("task-title").performTextInput("Inset verification")
        composeRule.waitUntil(10_000) {
            composeRule.runOnIdle {
                composeRule.activity.window.decorView.rootWindowInsets
                    ?.isVisible(WindowInsets.Type.ime()) == true
            }
        }
        // Wait for the platform keyboard animation to reach its final insets.
        android.os.SystemClock.sleep(500)
        assertSafe("task-title", keyboard = true)
        val titleBounds = composeRule.onNodeWithTag("task-title").getUnclippedBoundsInRoot()
        val visibleTitleBounds = composeRule.onNodeWithTag("task-title").fetchSemanticsNode().boundsInRoot
        val density = composeRule.activity.resources.displayMetrics.density
        assertTrue("Title field is clipped by the editor scroll viewport",
            visibleTitleBounds.height >= (titleBounds.bottom - titleBounds.top).value * density - 1)
        assertSafe("add-task", keyboard = true)
        screenshot("editor-keyboard")
        composeRule.onNodeWithTag("task-title").performImeAction()
        composeRule.onNodeWithTag("cancel-edit").performClick()
        composeRule.onNodeWithTag("more-options").performClick()
        composeRule.onNodeWithTag("about-menu-item").performClick()
        composeRule.onNodeWithTag("export-tasks").performScrollTo()
        assertSafe("export-tasks")
        screenshot("about")
    }

    private fun assertSafe(tag: String, keyboard: Boolean = false) {
        val node = composeRule.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode()
        val bounds = node.boundsInRoot
        val safe = composeRule.runOnIdle {
            val decor = composeRule.activity.window.decorView
            val types = WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or
                (if (keyboard) WindowInsets.Type.ime() else 0)
            val insets = decor.rootWindowInsets.getInsets(types)
            Rect(insets.left, insets.top, decor.width - insets.right, decor.height - insets.bottom)
        }
        assertTrue("$tag bounds $bounds overlap unsafe region outside $safe",
            bounds.left >= safe.left - 1 && bounds.top >= safe.top - 1 &&
                bounds.right <= safe.right + 1 && bounds.bottom <= safe.bottom + 1)
        if (keyboard && tag == "add-task") {
            val maxGap = composeRule.activity.resources.displayMetrics.density * 20
            assertTrue("Editor applies system bar padding twice: gap ${safe.bottom - bounds.bottom}px",
                safe.bottom - bounds.bottom <= maxGap)
        }
    }

    private fun screenshot(name: String) {
        composeRule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "edge-to-edge")
        directory.mkdirs()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
