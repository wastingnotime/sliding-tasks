package org.wastingnotime.slidingtasks.model

import java.time.LocalDate

/** Rebuildable practice data. Never passed to the user's task store. */
fun practiceGuideState(date: LocalDate, titles: List<String>, decisions: List<String>): SlidingTasksState {
    var nextId = 0
    val engine = SlidingTasksEngine(newId = { "practice-${nextId++}" }, now = { date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC) })
    var state = engine.openDay(SlidingTasksState(), date)
    listOf(ScheduleKind.DAILY, ScheduleKind.ONCE_PER_WEEK, ScheduleKind.ONCE).forEachIndexed { index, kind ->
        state = engine.createTask(state, titles[index], if (kind == ScheduleKind.ONCE) TaskType.ONE_TIME else TaskType.ROUTINE,
            TaskSchedule(kind, days = allWeekDays, startsOn = date), date)
    }
    val cards = state.cards
    decisions.forEachIndexed { index, decision ->
        val card = cards[index]
        state = engine.apply(state, if (decision == CardStatus.DONE.name) CardCommand.Complete(card.id) else CardCommand.Dismiss(card.id))
    }
    return state
}

fun SlidingTasksState.hasUserData(): Boolean = tasks.isNotEmpty() || cards.isNotEmpty() || events.isNotEmpty()
