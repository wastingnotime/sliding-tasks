package org.wastingnotime.slidingtasks

import android.Manifest
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Run with POST_NOTIFICATIONS revoked and its user-set/user-fixed flags cleared. */
class DailyReminderPermissionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun permission_is_requested_only_on_opt_in_and_denial_keeps_reminder_off() {
        org.junit.Assume.assumeTrue("Run with notification permission revoked",
            compose.activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED)
        if (compose.onAllNodesWithTag("start-without-guide").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("start-without-guide").performScrollTo().performClick()
        }
        compose.onNodeWithTag("more-options").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("reminder-menu-item").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("reminder-menu-item").performClick()
        compose.onNodeWithTag("reminder-toggle").assertIsOff()
        compose.onNodeWithTag("reminder-toggle").performClick()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val denyId = "com.android.permissioncontroller:id/permission_deny_button"
        compose.waitUntil(10_000) {
            automation.rootInActiveWindow?.findAccessibilityNodeInfosByViewId(denyId)?.isNotEmpty() == true
        }
        automation.rootInActiveWindow.findAccessibilityNodeInfosByViewId(denyId).first()
            .performAction(AccessibilityNodeInfo.ACTION_CLICK)
        compose.onNodeWithTag("reminder-toggle").assertIsOff()
        compose.onNodeWithTag("reminder-blocked").assertExists()
        compose.onNodeWithTag("reminder-system-settings").assertExists()
    }
}
