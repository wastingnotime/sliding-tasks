package org.wastingnotime.slidingtasks.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SlidingTasksEngineTest {
    private var id = 0
    private val engine = SlidingTasksEngine(
        newId = { "id-${++id}" },
        now = { Instant.parse("2026-09-18T12:00:00Z") },
    )
    private val friday = LocalDate.parse("2026-09-18")

    @Test
    fun task_created_during_open_day_generates_a_persistable_card_and_events() {
        val opened = engine.openDay(SlidingTasksState(), friday)
        val state = engine.createTask(opened, "Write brief", TaskType.ROUTINE, Recurrence.DAILY, friday)

        assertEquals(listOf("Write brief"), engine.pendingCards(state).map { it.title })
        assertEquals(listOf("TaskCreated", "CardGenerated"), state.events.map { it.type })
    }

    @Test
    fun recurrence_filters_weekdays_and_weekends() {
        var state = SlidingTasksState()
        state = engine.createTask(state, "Work", TaskType.ROUTINE, Recurrence.WEEKDAYS, friday)
        state = engine.createTask(state, "Rest", TaskType.ROUTINE, Recurrence.WEEKENDS, friday)

        val weekday = engine.openDay(state, friday)
        assertEquals(listOf("Work"), engine.pendingCards(weekday).map { it.title })

        val saturday = engine.openDay(weekday, friday.plusDays(1))
        assertEquals(listOf("Rest"), engine.pendingCards(saturday).map { it.title })
        assertEquals(CardStatus.MISSED, saturday.cards.first { it.title == "Work" }.status)
    }

    @Test
    fun every_two_days_uses_the_chosen_first_day() {
        val firstDay = friday.plusDays(1)
        var state = engine.createTask(SlidingTasksState(), "Vitamin", TaskType.ROUTINE,
            Recurrence.EVERY_TWO_DAYS, friday, startsOn = firstDay)

        assertTrue(engine.pendingCards(engine.openDay(state, friday)).isEmpty())
        state = engine.openDay(state, firstDay)
        assertEquals(listOf("Vitamin"), engine.pendingCards(state).map { it.title })
        assertTrue(engine.pendingCards(engine.openDay(state, firstDay.plusDays(1))).isEmpty())
        assertEquals(listOf("Vitamin"), engine.pendingCards(engine.openDay(state, firstDay.plusDays(2))).map { it.title })
    }

    @Test
    fun fortnightly_weekday_occurrence_waits_until_done_then_returns_next_fortnight() {
        var state = engine.createTask(SlidingTasksState(), "Cut hair", TaskType.ROUTINE,
            Recurrence.EVERY_TWO_WEEKS, friday, AvailableDays.WEEKDAYS)
        state = engine.openDay(state, friday)
        state = engine.openDay(state, friday.plusDays(3))
        assertTrue(engine.pendingCards(state).isEmpty())
        val nextMonday = friday.plusDays(10)
        state = engine.openDay(state, nextMonday)
        assertEquals(listOf("Cut hair"), engine.pendingCards(state).map { it.title })
        state = engine.apply(state, CardCommand.Complete(engine.pendingCards(state).single().id))
        assertTrue(engine.pendingCards(engine.openDay(state, nextMonday.plusDays(1))).isEmpty())
        assertEquals(listOf("Cut hair"), engine.pendingCards(engine.openDay(state, nextMonday.plusWeeks(2))).map { it.title })
    }

    @Test
    fun pickup_and_dropoff_can_share_one_alternating_weekend() {
        var state = engine.createTask(SlidingTasksState(), "Pick up Saturday morning", TaskType.ROUTINE,
            Recurrence.EVERY_TWO_WEEKS, friday, AvailableDays.SATURDAY)
        state = engine.createTask(state, "Drop off Sunday night", TaskType.ROUTINE,
            Recurrence.EVERY_TWO_WEEKS, friday, AvailableDays.SUNDAY)

        state = engine.openDay(state, friday.plusDays(1))
        assertEquals(listOf("Pick up Saturday morning"), engine.pendingCards(state).map { it.title })
        state = engine.openDay(state, friday.plusDays(2))
        assertEquals(listOf("Drop off Sunday night"), engine.pendingCards(state).map { it.title })
        assertTrue(engine.pendingCards(engine.openDay(state, friday.plusDays(8))).isEmpty())
        assertEquals(listOf("Pick up Saturday morning"), engine.pendingCards(engine.openDay(state, friday.plusDays(15))).map { it.title })
    }

    @Test
    fun weekly_dismissal_skips_the_rest_of_the_week() {
        var state = engine.createTask(SlidingTasksState(), "Put trash out", TaskType.ROUTINE,
            Recurrence.WEEKLY, friday)
        state = engine.openDay(state, friday)
        state = engine.apply(state, CardCommand.Dismiss(engine.pendingCards(state).single().id))
        state = engine.openDay(state, friday.plusDays(1))
        assertTrue(engine.pendingCards(state).isEmpty())
        state = engine.openDay(state, friday.plusDays(3))
        assertEquals(listOf("Put trash out"), engine.pendingCards(state).map { it.title })
        state = engine.apply(state, CardCommand.Complete(engine.pendingCards(state).single().id))
        assertTrue(engine.pendingCards(engine.openDay(state, friday.plusDays(4))).isEmpty())
        assertEquals(listOf("Put trash out"), engine.pendingCards(engine.openDay(state, friday.plusDays(10))).map { it.title })
    }

    @Test
    fun fortnightly_dismissal_waits_for_the_next_active_week() {
        var state = engine.createTask(SlidingTasksState(), "Cut hair", TaskType.ROUTINE,
            Recurrence.EVERY_TWO_WEEKS, friday, AvailableDays.WEEKDAYS)
        state = engine.openDay(state, friday)
        state = engine.apply(state, CardCommand.Dismiss(engine.pendingCards(state).single().id))
        assertTrue(engine.pendingCards(engine.openDay(state, friday.plusDays(3))).isEmpty())
        assertEquals(listOf("Cut hair"), engine.pendingCards(engine.openDay(state, friday.plusDays(10))).map { it.title })
    }

    @Test
    fun missed_weekly_card_remains_available_in_its_week() {
        var state = engine.createTask(SlidingTasksState(), "Put trash out", TaskType.ROUTINE,
            Recurrence.WEEKLY, friday)
        state = engine.openDay(state, friday)
        state = engine.openDay(state, friday.plusDays(1))
        assertEquals(CardStatus.MISSED, state.cards.first().status)
        assertEquals(listOf("Put trash out"), engine.pendingCards(state).map { it.title })
    }

    @Test
    fun completing_card_records_history_and_resolves_one_time_task() {
        var state = engine.openDay(SlidingTasksState(), friday)
        state = engine.createTask(state, "Call dentist", TaskType.ONE_TIME, Recurrence.ONCE, friday)
        val card = engine.pendingCards(state).single()
        state = engine.apply(state, CardCommand.Touch(card.id))
        state = engine.apply(state, CardCommand.Complete(card.id))

        assertTrue(engine.pendingCards(state).isEmpty())
        assertEquals(CardStatus.DONE, state.cards.single().status)
        assertEquals(1, state.cards.single().touches)
        assertTrue(state.tasks.single().resolved)
        assertFalse(state.tasks.single().active)
        assertEquals(listOf("TaskCreated", "CardGenerated", "CardTouched", "CardDone"), state.events.map { it.type })
    }

    @Test
    fun deactivation_changes_future_generation_but_not_today_card() {
        var state = engine.openDay(SlidingTasksState(), friday)
        state = engine.createTask(state, "Read", TaskType.ROUTINE, Recurrence.DAILY, friday)
        state = engine.setTaskActive(state, state.tasks.single().id, false)

        assertEquals(1, engine.pendingCards(state).size)
        state = engine.openDay(state, friday.plusDays(1))
        assertTrue(engine.pendingCards(state).isEmpty())
    }

    @Test
    fun removing_a_task_preserves_existing_card_and_history_but_stops_future_generation() {
        var state = engine.openDay(SlidingTasksState(), friday)
        state = engine.createTask(state, "Read", TaskType.ROUTINE, Recurrence.DAILY, friday)
        val taskId = state.tasks.single().id

        state = engine.removeTask(state, taskId)

        assertTrue(state.tasks.isEmpty())
        assertEquals(listOf("Read"), engine.pendingCards(state).map { it.title })
        assertEquals("TaskRemoved", state.events.last().type)
        state = engine.openDay(state, friday.plusDays(1))
        assertTrue(engine.pendingCards(state).isEmpty())
    }

    @Test
    fun reordered_plan_controls_future_card_order() {
        var state = SlidingTasksState()
        state = engine.createTask(state, "First", TaskType.ROUTINE, Recurrence.DAILY, friday)
        state = engine.createTask(state, "Second", TaskType.ROUTINE, Recurrence.DAILY, friday)
        state = engine.moveTask(state, state.tasks[1].id, -1)

        assertEquals(listOf("Second", "First"), state.tasks.map { it.title })
        state = engine.openDay(state, friday)
        assertEquals(listOf("Second", "First"), engine.pendingCards(state).map { it.title })
        assertEquals("TaskReordered", state.events.first { it.type == "TaskReordered" }.type)
    }

    @Test
    fun a_slide_past_threshold_selects_an_outcome() {
        assertEquals(SlideDecision.COMPLETE, slideDecision(120f, 400f))
        assertEquals(SlideDecision.DISMISS, slideDecision(-120f, 400f))
        assertEquals(null, slideDecision(100f, 400f))
    }
}
