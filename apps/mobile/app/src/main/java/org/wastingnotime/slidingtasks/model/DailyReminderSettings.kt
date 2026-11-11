package org.wastingnotime.slidingtasks.model

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class DailyReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 9,
    val minute: Int = 0,
) {
    init {
        require(hour in 0..23 && minute in 0..59)
    }

    // Recompute from the local calendar each day, rather than adding 24 hours:
    // the elapsed interval changes when daylight saving time starts or ends.
    fun nextOccurrence(now: Instant, zone: ZoneId): Instant {
        val date = now.atZone(zone).toLocalDate()
        val time = LocalTime.of(hour, minute)
        val today = date.atTime(time).atZone(zone).toInstant()
        return if (today > now) today else date.plusDays(1).atTime(time).atZone(zone).toInstant()
    }
}
