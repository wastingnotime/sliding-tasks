package com.wastingnotime.slidingtasks.model

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
        val state = engine.createTask(opened, "Write brief", TaskType.FOCUS, Recurrence.DAILY, friday)

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
    fun a_slide_past_threshold_selects_an_outcome() {
        assertEquals(SlideDecision.COMPLETE, slideDecision(120f, 400f))
        assertEquals(SlideDecision.DISMISS, slideDecision(-120f, 400f))
        assertEquals(null, slideDecision(100f, 400f))
    }
}
