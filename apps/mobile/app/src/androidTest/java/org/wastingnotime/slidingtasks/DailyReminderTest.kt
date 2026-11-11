package org.wastingnotime.slidingtasks

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TimePicker
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.UiController
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matcher
import org.junit.*
import org.junit.Assert.*
import org.wastingnotime.slidingtasks.data.DailyReminder
import org.wastingnotime.slidingtasks.data.DailyReminderReceiver
import org.wastingnotime.slidingtasks.model.DailyReminderSettings

class DailyReminderTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = compose.activity
    private val preferences get() = context.getSharedPreferences(DailyReminder.PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val reminder get() = DailyReminder(context)
    private val notifications get() = context.getSystemService(NotificationManager::class.java)

    @Before fun prepare() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        compose.runOnIdle {
            preferences.edit().clear().commit()
            reminder.save(DailyReminderSettings())
        }
        if (compose.onAllNodesWithTag("start-without-guide").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("start-without-guide").performScrollTo().performClick()
        }
    }

    @After fun cleanup() { compose.runOnIdle { reminder.save(DailyReminderSettings()) } }

    private fun openSettings() {
        compose.onNodeWithTag("more-options").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("reminder-menu-item").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("reminder-menu-item").performClick()
    }

    @Test fun settings_default_off_time_edit_cancel_persistence_and_disable() {
        openSettings()
        compose.onNodeWithTag("reminder-toggle").assertIsOff()
        compose.onNodeWithTag("reminder-time").performClick()
        onView(isAssignableFrom(TimePicker::class.java)).inRoot(isDialog()).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(TimePicker::class.java)
            override fun getDescription() = "Choose 18:35"
            override fun perform(controller: UiController, view: View) {
                (view as TimePicker).hour = 18
                view.minute = 35
            }
        })
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
        assertEquals(DailyReminderSettings(false, 18, 35), reminder.settings())
        compose.onNodeWithTag("reminder-time").performClick()
        onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click())
        assertEquals(35, reminder.settings().minute)
        compose.onNodeWithTag("reminder-toggle").performClick().assertIsOn()
        assertTrue(preferences.getLong("scheduled_for", 0) > System.currentTimeMillis())
        compose.activityRule.scenario.recreate()
        openSettings()
        compose.onNodeWithTag("reminder-toggle").assertIsOn()
        assertEquals(DailyReminderSettings(true, 18, 35), reminder.settings())
        compose.onNodeWithTag("reminder-toggle").performClick().assertIsOff()
        assertFalse(preferences.contains("scheduled_for"))
    }

    @Test fun receiver_posts_once_rearms_and_disable_rejects_old_delivery() {
        compose.runOnIdle {
            reminder.save(DailyReminderSettings(true))
            val due = System.currentTimeMillis() - 1000
            preferences.edit().putLong("scheduled_for", due).commit()
            DailyReminderReceiver().onReceive(context, Intent(DailyReminder.ACTION_REMIND).putExtra("scheduled_for", due))
        }
        compose.waitUntil(5_000) { notifications.activeNotifications.size == 1 }
        compose.runOnIdle {
            val due = System.currentTimeMillis() - 1000
            val notification = notifications.activeNotifications.single().notification
            assertEquals(context.getString(R.string.reminder_notification_title),
                notification.extras.getString(android.app.Notification.EXTRA_TITLE))
            assertNotNull(notification.contentIntent)
            assertTrue(preferences.getLong("scheduled_for", 0) > System.currentTimeMillis())
            notifications.cancelAll()
        }
        compose.waitUntil(5_000) { notifications.activeNotifications.isEmpty() }
        compose.runOnIdle {
            val due = System.currentTimeMillis() - 1000
            preferences.edit().putLong("scheduled_for", due).commit()
            DailyReminderReceiver().onReceive(context, Intent(DailyReminder.ACTION_REMIND).putExtra("scheduled_for", due))
            assertTrue(notifications.activeNotifications.isEmpty())
            reminder.save(DailyReminderSettings(false))
            DailyReminderReceiver().onReceive(context, Intent(DailyReminder.ACTION_REMIND).putExtra("scheduled_for", due))
            assertTrue(notifications.activeNotifications.isEmpty())
        }
    }

    @Test fun reboot_and_timezone_broadcasts_rearm_only_when_enabled() {
        compose.runOnIdle {
            reminder.save(DailyReminderSettings(true, 7, 45))
            for (action in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)) {
                preferences.edit().remove("scheduled_for").commit()
                DailyReminderReceiver().onReceive(context, Intent(action))
                assertTrue(preferences.getLong("scheduled_for", 0) > System.currentTimeMillis())
                assertEquals(DailyReminderSettings(true, 7, 45), reminder.settings())
            }
            reminder.save(DailyReminderSettings(false))
            DailyReminderReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
            assertFalse(preferences.contains("scheduled_for"))
        }
    }
}
