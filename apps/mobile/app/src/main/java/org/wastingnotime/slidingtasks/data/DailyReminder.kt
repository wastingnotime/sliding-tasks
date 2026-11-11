package org.wastingnotime.slidingtasks.data

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.wastingnotime.slidingtasks.MainActivity
import org.wastingnotime.slidingtasks.R
import org.wastingnotime.slidingtasks.model.DailyReminderSettings
import java.time.Instant
import java.time.ZoneId

/** Installation preferences are separate from task data and backup files. */
class DailyReminder(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)

    fun settings() = DailyReminderSettings(
        enabled = preferences.getBoolean("enabled", false),
        hour = preferences.getInt("hour", 9),
        minute = preferences.getInt("minute", 0),
    )

    fun notificationsAllowed(): Boolean = notifications.areNotificationsEnabled() &&
        notifications.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE

    fun save(settings: DailyReminderSettings) {
        check(preferences.edit().putBoolean("enabled", settings.enabled)
            .putInt("hour", settings.hour).putInt("minute", settings.minute).commit()) {
            "Could not save daily reminder"
        }
        notifications.cancel(NOTIFICATION_ID)
        reschedule()
    }

    fun reschedule() {
        val settings = settings()
        if (!settings.enabled || !notificationsAllowed()) {
            alarms.cancel(alarmIntent())
            preferences.edit().remove("scheduled_for").apply()
            notifications.cancel(NOTIFICATION_ID)
            return
        }
        createChannel()
        val next = settings.nextOccurrence(Instant.now(), ZoneId.systemDefault()).toEpochMilli()
        preferences.edit().putLong("scheduled_for", next).apply()
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, alarmIntent(next))
    }

    internal fun deliver(scheduledFor: Long) {
        if (!settings().enabled || scheduledFor != preferences.getLong("scheduled_for", -1L)) return
        val now = Instant.now()
        if (now.toEpochMilli() < scheduledFor) return
        val today = now.atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val scheduledDate = Instant.ofEpochMilli(scheduledFor).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        // Do not replay yesterday's reminder or notify twice after a clock change.
        val shouldNotify = scheduledDate == today && preferences.getString("last_shown_date", null) != today
        reschedule()
        if (!shouldNotify || !notificationsAllowed()) return
        createChannel()
        val openApp = PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(context.getString(R.string.reminder_notification_body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .build()
        notifications.notify(NOTIFICATION_ID, notification)
        preferences.edit().putString("last_shown_date", today).apply()
    }

    private fun createChannel() {
        notifications.createNotificationChannel(NotificationChannel(
            CHANNEL_ID, context.getString(R.string.daily_reminder), NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) })
    }

    private fun alarmIntent(scheduledFor: Long = 0L): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, DailyReminderReceiver::class.java)
            .setAction(ACTION_REMIND).putExtra("scheduled_for", scheduledFor),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val PREFERENCES_NAME = "daily_reminder"
        internal const val CHANNEL_ID = "daily_reminder"
        internal const val NOTIFICATION_ID = 1001
        internal const val ACTION_REMIND = "org.wastingnotime.slidingtasks.DAILY_REMINDER"
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminder = DailyReminder(context)
        when (intent.action) {
            DailyReminder.ACTION_REMIND -> reminder.deliver(intent.getLongExtra("scheduled_for", -1L))
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED -> reminder.reschedule()
        }
    }
}
