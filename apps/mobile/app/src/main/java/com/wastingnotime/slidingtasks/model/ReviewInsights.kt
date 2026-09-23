package com.wastingnotime.slidingtasks.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

data class ReviewCounts(
    val done: Int,
    val notToday: Int,
    val missed: Int,
    val open: Int,
) {
    val closed: Int get() = done + notToday + missed
    val donePercent: Int get() = if (closed == 0) 0 else (done * 100.0 / closed).roundToInt()
}

data class ReviewPattern(val title: String, val matching: Int, val total: Int)

data class ReviewTrend(val recent: ReviewCounts, val previous: ReviewCounts)

data class ReviewInsights(
    val week: ReviewCounts,
    val mostMissed: ReviewPattern?,
    val mostNotToday: ReviewPattern?,
    val mostDone: ReviewPattern?,
    val trend: ReviewTrend?,
    val days: List<Pair<LocalDate, List<TaskCard>>>,
)

private data class TaskWindow(
    val title: String,
    val total: Int,
    val done: Int,
    val notToday: Int,
    val missed: Int,
)

fun reviewInsights(state: SlidingTasksState, today: LocalDate): ReviewInsights {
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekCards = state.cards.filter { !it.boardDate.isBefore(weekStart) && !it.boardDate.isAfter(today) }
    val recentStart = today.minusDays(14)
    val recentCards = state.cards.filter {
        !it.boardDate.isBefore(recentStart) && it.boardDate.isBefore(today) && it.status != CardStatus.PENDING
    }
    val titles = state.tasks.associate { it.id to it.title }
    val patterns = recentCards.groupBy { it.taskId }.map { (taskId, cards) ->
        TaskWindow(
            title = titles[taskId] ?: cards.last().title,
            total = cards.size,
            done = cards.count { it.status == CardStatus.DONE },
            notToday = cards.count { it.status == CardStatus.DISMISSED },
            missed = cards.count { it.status == CardStatus.MISSED },
        )
    }

    val mostMissed = patterns.filter { it.total >= 2 && it.missed > 0 }
        .maxWithOrNull(compareBy<TaskWindow> { it.missed.toDouble() / it.total }.thenBy { it.missed })
        ?.let { ReviewPattern(it.title, it.missed, it.total) }

    val mostNotToday = patterns.filter { it.notToday > 0 }
        .maxWithOrNull(compareBy<TaskWindow> { it.notToday }.thenBy { it.notToday.toDouble() / it.total })
        ?.let { ReviewPattern(it.title, it.notToday, it.total) }

    val mostDone = patterns.filter { it.total >= 3 && it.done >= 2 }
        .maxWithOrNull(compareBy<TaskWindow> { it.done.toDouble() / it.total }.thenBy { it.done })
        ?.let { ReviewPattern(it.title, it.done, it.total) }

    val recentWeek = counts(recentCards.filter { !it.boardDate.isBefore(today.minusDays(7)) })
    val previousWeek = counts(recentCards.filter { it.boardDate.isBefore(today.minusDays(7)) })
    val trend = if (recentWeek.closed >= 3 && previousWeek.closed >= 3) {
        ReviewTrend(recentWeek, previousWeek)
    } else null
    val days = state.cards.filter {
        !it.boardDate.isBefore(today.minusDays(13)) && !it.boardDate.isAfter(today)
    }.groupBy { it.boardDate }.toList().sortedByDescending { it.first }

    return ReviewInsights(counts(weekCards), mostMissed, mostNotToday, mostDone, trend, days)
}

private fun counts(cards: List<TaskCard>) = ReviewCounts(
    done = cards.count { it.status == CardStatus.DONE },
    notToday = cards.count { it.status == CardStatus.DISMISSED },
    missed = cards.count { it.status == CardStatus.MISSED },
    open = cards.count { it.status == CardStatus.PENDING },
)
