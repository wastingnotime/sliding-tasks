package com.wastingnotime.slidingtasks.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TodayBoardTest {
    private val board = TodayBoard(
        dateLabel = "Today",
        cards = listOf(
            TodayCard("first", "First", "Routine"),
            TodayCard("second", "Second", "One-time"),
        ),
    )

    @Test
    fun completing_a_card_removes_it_from_the_visible_board() {
        val result = board.after(CardCommand.Complete("first"))

        assertEquals(listOf("second"), result.cards.map { it.id })
    }

    @Test
    fun dismissing_a_card_removes_it_from_the_visible_board() {
        val result = board.after(CardCommand.Dismiss("second"))

        assertEquals(listOf("first"), result.cards.map { it.id })
    }

    @Test
    fun touching_a_card_does_not_change_board_order() {
        val result = board.after(CardCommand.Touch("second"))

        assertEquals(board, result)
    }

    @Test
    fun a_slide_past_the_threshold_selects_an_outcome() {
        assertEquals(SlideDecision.COMPLETE, slideDecision(offsetX = 120f, containerWidth = 400f))
        assertEquals(SlideDecision.DISMISS, slideDecision(offsetX = -120f, containerWidth = 400f))
    }

    @Test
    fun a_short_slide_snaps_back_without_an_outcome() {
        assertEquals(null, slideDecision(offsetX = 100f, containerWidth = 400f))
        assertEquals(null, slideDecision(offsetX = -100f, containerWidth = 400f))
    }
}
