package org.wastingnotime.slidingtasks.model

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class DailyReminderSettingsTest {
    private val utc = ZoneId.of("UTC")

    @Test fun new_installation_is_off_with_a_morning_time() {
        assertEquals(DailyReminderSettings(false, 9, 0), DailyReminderSettings())
    }

    @Test fun next_reminder_is_today_before_time_and_tomorrow_at_or_after_time() {
        val reminder = DailyReminderSettings(true, 9, 15)
        assertEquals(Instant.parse("2026-11-11T09:15:00Z"),
            reminder.nextOccurrence(Instant.parse("2026-11-11T09:14:59Z"), utc))
        assertEquals(Instant.parse("2026-11-12T09:15:00Z"),
            reminder.nextOccurrence(Instant.parse("2026-11-11T09:15:00Z"), utc))
        assertEquals(Instant.parse("2026-11-12T09:15:00Z"),
            reminder.nextOccurrence(Instant.parse("2026-11-11T23:00:00Z"), utc))
    }

    @Test fun midnight_rolls_over_the_year() {
        assertEquals(Instant.parse("2027-01-01T00:00:00Z"),
            DailyReminderSettings(true, 0, 0).nextOccurrence(Instant.parse("2026-12-31T23:59:59Z"), utc))
    }

    @Test fun timezone_change_keeps_the_selected_wall_clock_time() {
        val now = Instant.parse("2026-11-11T10:00:00Z")
        val reminder = DailyReminderSettings(true, 9, 0)
        assertEquals(Instant.parse("2026-11-11T12:00:00Z"),
            reminder.nextOccurrence(now, ZoneId.of("America/Sao_Paulo")))
        assertEquals(Instant.parse("2026-11-11T17:00:00Z"),
            reminder.nextOccurrence(now, ZoneId.of("America/Los_Angeles")))
    }

    @Test fun daylight_saving_gap_moves_forward_and_next_day_returns_to_selected_time() {
        val zone = ZoneId.of("America/New_York")
        val reminder = DailyReminderSettings(true, 2, 30)
        assertEquals(Instant.parse("2026-03-08T07:30:00Z"),
            reminder.nextOccurrence(Instant.parse("2026-03-08T05:00:00Z"), zone))
        assertEquals(Instant.parse("2026-03-09T06:30:00Z"),
            reminder.nextOccurrence(Instant.parse("2026-03-08T07:30:00Z"), zone))
    }

    @Test fun daylight_saving_overlap_uses_first_occurrence_then_the_next_day() {
        val zone = ZoneId.of("America/New_York")
        val reminder = DailyReminderSettings(true, 1, 30)
        assertEquals(Instant.parse("2026-11-01T05:30:00Z"),
            reminder.nextOccurrence(Instant.parse("2026-11-01T04:00:00Z"), zone))
        assertEquals(Instant.parse("2026-11-02T06:30:00Z"),
            reminder.nextOccurrence(Instant.parse("2026-11-01T05:45:00Z"), zone))
    }
}
