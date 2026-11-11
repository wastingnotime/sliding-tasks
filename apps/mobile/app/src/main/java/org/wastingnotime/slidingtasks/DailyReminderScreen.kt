package org.wastingnotime.slidingtasks

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.wastingnotime.slidingtasks.data.DailyReminder
import org.wastingnotime.slidingtasks.model.DailyReminderSettings
import java.util.Calendar

@Composable
internal fun DailyReminderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val reminder = remember { DailyReminder(context.applicationContext) }
    var settings by remember { mutableStateOf(reminder.settings()) }
    var allowed by remember { mutableStateOf(reminder.notificationsAllowed()) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    fun save(next: DailyReminderSettings) {
        error = runCatching { reminder.save(next) }.isFailure
        settings = reminder.settings()
        allowed = reminder.notificationsAllowed()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = reminder.notificationsAllowed()
        permissionDenied = !granted || !allowed
        if (granted && allowed) save(reminder.settings().copy(enabled = true))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, reminder) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                settings = reminder.settings()
                allowed = reminder.notificationsAllowed()
                error = runCatching { reminder.reschedule() }.isFailure
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.daily_reminder), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f))
            TextButton(onClick = onBack, modifier = Modifier.testTag("reminder-back")) {
                Text(stringResource(R.string.back))
            }
        }
        Text(stringResource(R.string.reminder_description))
        val switchLabel = stringResource(R.string.daily_reminder)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(if (settings.enabled) R.string.reminder_on else R.string.reminder_off),
                modifier = Modifier.weight(1f))
            Switch(
                checked = settings.enabled,
                onCheckedChange = { enabled ->
                    error = false
                    permissionDenied = false
                    if (enabled && Build.VERSION.SDK_INT >= 33 &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else if (enabled && !reminder.notificationsAllowed()) {
                        allowed = false
                        permissionDenied = true
                    } else {
                        save(settings.copy(enabled = enabled))
                    }
                },
                modifier = Modifier.testTag("reminder-toggle").semantics { contentDescription = switchLabel },
            )
        }
        val time = DateFormat.getTimeFormat(context).format(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.hour)
            set(Calendar.MINUTE, settings.minute)
        }.time)
        OutlinedButton(
            onClick = {
                TimePickerDialog(context, { _, hour, minute ->
                    save(reminder.settings().copy(hour = hour, minute = minute))
                }, settings.hour, settings.minute, DateFormat.is24HourFormat(context)).show()
            },
            modifier = Modifier.testTag("reminder-time"),
        ) { Text(stringResource(R.string.reminder_time, time)) }
        Text(stringResource(R.string.reminder_timing_hint), style = MaterialTheme.typography.bodySmall)
        if (!allowed && (settings.enabled || permissionDenied)) {
            Text(stringResource(R.string.reminder_permission_blocked), color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("reminder-blocked"))
            TextButton(onClick = {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }.onFailure { error = true }
            }, modifier = Modifier.testTag("reminder-system-settings")) {
                Text(stringResource(R.string.reminder_open_settings))
            }
        }
        if (error) Text(stringResource(R.string.reminder_save_error), color = MaterialTheme.colorScheme.error)
    }
}
