package org.wastingnotime.slidingtasks

import android.content.Context
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.wastingnotime.slidingtasks.data.ScheduledBackup

class BackupStatusTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @After fun cleanUp() {
        compose.runOnIdle { ScheduledBackup(compose.activity).disable() }
    }

    @Test fun failed_manual_backup_preserves_last_success_and_uses_saved_folder() {
        val preferences = compose.activity.getSharedPreferences(
            ScheduledBackup.PREFERENCES_NAME, Context.MODE_PRIVATE,
        )
        val timestamp = 1748736000000L
        compose.runOnIdle {
            preferences.edit().putString("folder_uri", "content://missing.provider/tree/backup")
                .putLong("last_success", timestamp).putBoolean("last_failure", false).commit()
        }
        compose.onNodeWithTag("more-options").performClick()
        compose.onNodeWithTag("backup-menu-item").performClick()
        compose.onNodeWithTag("last-backup").assertTextContains("2025", substring = true)
        compose.onNodeWithTag("backup-now").performScrollTo().performClick()
        compose.waitUntil(10_000) { preferences.getBoolean("last_failure", false) }
        org.junit.Assert.assertEquals(timestamp, preferences.getLong("last_success", 0))
        org.junit.Assert.assertEquals(
            "content://missing.provider/tree/backup", preferences.getString("folder_uri", null),
        )
        compose.onNodeWithTag("last-backup").assertTextContains("2025", substring = true)
    }
}
