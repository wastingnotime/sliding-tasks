package org.wastingnotime.slidingtasks.model

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PracticeGuideTest {
    private val date = LocalDate.of(2026, 10, 11)
    private val titles = listOf("Walk", "Priorities", "Dentist")

    @Test fun practice_uses_real_task_scopes_and_review_outcomes() {
        val state = practiceGuideState(date, titles, listOf("DONE", "DISMISSED", "DONE"))
        assertEquals(listOf(SkipScope.TODAY, SkipScope.WEEK, SkipScope.TASK), state.cards.map { it.skipScope })
        assertEquals(2, reviewInsights(state, date).week.done)
        assertEquals(1, reviewInsights(state, date).week.skipped)
        assertTrue(state.tasks.last().resolved)
        assertEquals(0, SlidingTasksEngine().pendingCards(state).size)
    }

    @Test fun undo_rebuilds_the_same_pending_card_and_reopens_one_time_task() {
        val decisions = listOf("DONE", "DISMISSED", "DISMISSED")
        val before = practiceGuideState(date, titles, decisions)
        val undone = practiceGuideState(date, titles, decisions.dropLast(1))
        assertEquals(before.cards.last().id, undone.cards.last().id)
        assertEquals(CardStatus.PENDING, undone.cards.last().status)
        assertFalse(undone.tasks.last().resolved)
        assertEquals(1, SlidingTasksEngine().pendingCards(undone).size)
    }

    @Test fun active_date_alone_is_not_user_data_but_history_is() {
        assertFalse(SlidingTasksState(activeDate = date).hasUserData())
        assertTrue(SlidingTasksState(cards = practiceGuideState(date, titles, emptyList()).cards).hasUserData())
    }
}
