package org.wastingnotime.slidingtasks.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

enum class TaskType(val label: String) {
    ROUTINE("Routine"),
    ONE_TIME("One-time"),
}

enum class ScheduleKind(val label: String) {
    ONCE("One-time"),
    DAILY("Daily"),
    WEEKLY_DAYS("Weekly"),
    ONCE_PER_WEEK("Once per week"),
}

val weekdays: Set<DayOfWeek> = DayOfWeek.entries.take(5).toSet()
val allWeekDays: Set<DayOfWeek> = DayOfWeek.entries.toSet()

data class TaskSchedule(
    val kind: ScheduleKind,
    val interval: Int = 1,
    val days: Set<DayOfWeek> = weekdays,
    val startsOn: LocalDate,
) {
    init {
        require(interval > 0) { "Repeat interval must be positive" }
        require(kind == ScheduleKind.ONCE || kind == ScheduleKind.DAILY || days.isNotEmpty()) {
            "Select at least one day"
        }
    }
}

enum class SkipScope(val label: String) {
    TODAY("Skip today"), WEEK("Skip this week"), TASK("Skip task"),
}

enum class CardStatus { PENDING, DONE, DISMISSED, MISSED }

data class PlannedTask(
    val id: String,
    val title: String,
    val type: TaskType,
    val schedule: TaskSchedule,
    val active: Boolean,
    val createdOn: LocalDate,
    val resolved: Boolean = false,
)

data class TaskCard(
    val id: String,
    val taskId: String,
    val boardDate: LocalDate,
    val title: String,
    val type: TaskType,
    val status: CardStatus = CardStatus.PENDING,
    val touches: Int = 0,
    val skipScope: SkipScope = SkipScope.TODAY,
)

data class TaskEvent(
    val id: String,
    val type: String,
    val occurredAt: Instant,
    val taskId: String,
    val cardId: String? = null,
    val title: String,
)

data class SlidingTasksState(
    val tasks: List<PlannedTask> = emptyList(),
    val cards: List<TaskCard> = emptyList(),
    val events: List<TaskEvent> = emptyList(),
    val activeDate: LocalDate? = null,
)

sealed interface CardCommand {
    val cardId: String

    data class Complete(override val cardId: String) : CardCommand
    data class Dismiss(override val cardId: String) : CardCommand
    data class Touch(override val cardId: String) : CardCommand
}

enum class SlideDecision { COMPLETE, DISMISS }

fun slideDecision(offsetX: Float, containerWidth: Float, thresholdFraction: Float = 0.28f): SlideDecision? {
    if (containerWidth <= 0f) return null
    val threshold = containerWidth * thresholdFraction
    return when {
        offsetX >= threshold -> SlideDecision.COMPLETE
        offsetX <= -threshold -> SlideDecision.DISMISS
        else -> null
    }
}

class SlidingTasksEngine(
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val now: () -> Instant = { Instant.now() },
) {
    fun openDay(state: SlidingTasksState, date: LocalDate): SlidingTasksState {
        var next = state
        if (state.activeDate != null && state.activeDate < date) {
            val missed = state.cards.filter { it.boardDate < date && it.status == CardStatus.PENDING }
            next = missed.fold(state) { current, card ->
                var missedState = current.updateCard(card.copy(status = CardStatus.MISSED))
                    .withEvent("CardMissed", card)
                val task = current.tasks.firstOrNull { it.id == card.taskId }
                if (task?.type == TaskType.ONE_TIME) {
                    missedState = missedState.withEvent("TaskCarriedForward", task)
                }
                missedState
            }
        }

        val existingTaskIds = next.cards.filter { it.boardDate == date }.mapTo(mutableSetOf()) { it.taskId }
        val generated = next.tasks
            .filter { it.id !in existingTaskIds && it.isEligible(date, next.cards) }
            .map { task ->
                TaskCard(
                    id = newId(),
                    taskId = task.id,
                    boardDate = date,
                    title = task.title,
                    type = task.type,
                    skipScope = when (task.schedule.kind) {
                        ScheduleKind.ONCE -> SkipScope.TASK
                        ScheduleKind.ONCE_PER_WEEK -> SkipScope.WEEK
                        else -> SkipScope.TODAY
                    },
                )
            }
        next = next.copy(activeDate = date, cards = next.cards + generated)
        return generated.fold(next) { current, card -> current.withEvent("CardGenerated", card) }
    }

    fun createTask(
        state: SlidingTasksState,
        title: String,
        type: TaskType,
        schedule: TaskSchedule,
        date: LocalDate,
    ): SlidingTasksState {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Task title cannot be empty" }
        require((type == TaskType.ONE_TIME) == (schedule.kind == ScheduleKind.ONCE)) {
            "One-time tasks use Once; recurring tasks require a recurrence"
        }
        val task = PlannedTask(newId(), cleanTitle, type, schedule, active = true, createdOn = date)
        val created = state.copy(tasks = state.tasks + task).withEvent("TaskCreated", task)
        return if (created.activeDate == date && task.isEligible(date, created.cards)) openDay(created, date) else created
    }

    fun updateTask(
        state: SlidingTasksState,
        taskId: String,
        title: String,
        type: TaskType,
        schedule: TaskSchedule,
    ): SlidingTasksState {
        val task = state.tasks.firstOrNull { it.id == taskId } ?: return state
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Task title cannot be empty" }
        require((type == TaskType.ONE_TIME) == (schedule.kind == ScheduleKind.ONCE)) {
            "One-time tasks use Once; recurring tasks require a recurrence"
        }
        val updated = task.copy(title = cleanTitle, type = type, schedule = schedule)
        if (updated == task) return state
        val changed = state.copy(tasks = state.tasks.map { if (it.id == taskId) updated else it })
            .withEvent("TaskUpdated", updated)
        val activeDate = changed.activeDate
        return if (activeDate != null && updated.isEligible(activeDate, changed.cards)) {
            openDay(changed, activeDate)
        } else changed
    }

    fun setTaskActive(state: SlidingTasksState, taskId: String, active: Boolean): SlidingTasksState {
        val task = state.tasks.firstOrNull { it.id == taskId } ?: return state
        if (task.active == active || task.resolved) return state
        val updated = task.copy(active = active)
        return state.copy(tasks = state.tasks.map { if (it.id == taskId) updated else it })
            .withEvent(if (active) "TaskActivated" else "TaskDeactivated", updated)
    }

    fun removeTask(state: SlidingTasksState, taskId: String): SlidingTasksState {
        val task = state.tasks.firstOrNull { it.id == taskId } ?: return state
        return state.copy(tasks = state.tasks.filterNot { it.id == taskId })
            .withEvent("TaskRemoved", task)
    }

    fun moveTask(state: SlidingTasksState, taskId: String, offset: Int): SlidingTasksState {
        val from = state.tasks.indexOfFirst { it.id == taskId }
        if (from == -1) return state
        val to = (from + offset).coerceIn(state.tasks.indices)
        if (from == to) return state
        val reordered = state.tasks.toMutableList().apply {
            add(to, removeAt(from))
        }
        return state.copy(tasks = reordered).withEvent("TaskReordered", reordered[to])
    }

    fun apply(state: SlidingTasksState, command: CardCommand): SlidingTasksState {
        val card = state.cards.firstOrNull { it.id == command.cardId } ?: return state
        if (card.status != CardStatus.PENDING || card.boardDate != state.activeDate) return state
        return when (command) {
            is CardCommand.Touch -> state.updateCard(card.copy(touches = card.touches + 1))
                .withEvent("CardTouched", card)
            is CardCommand.Complete -> resolve(state, card, CardStatus.DONE, "CardDone")
            is CardCommand.Dismiss -> resolve(state, card, CardStatus.DISMISSED, "CardDismissed")
        }
    }

    fun pendingCards(state: SlidingTasksState): List<TaskCard> = state.cards.filter {
        it.boardDate == state.activeDate && it.status == CardStatus.PENDING
    }

    private fun resolve(
        state: SlidingTasksState,
        card: TaskCard,
        status: CardStatus,
        eventType: String,
    ): SlidingTasksState {
        var next = state.updateCard(card.copy(status = status)).withEvent(eventType, card)
        val task = next.tasks.firstOrNull { it.id == card.taskId }
        if (task?.type == TaskType.ONE_TIME) {
            next = next.copy(tasks = next.tasks.map {
                if (it.id == task.id) it.copy(active = false, resolved = true) else it
            })
        }
        return next
    }

    private fun PlannedTask.isEligible(date: LocalDate, cards: List<TaskCard>): Boolean {
        if (!active || resolved || createdOn > date || schedule.startsOn > date) return false
        return when (schedule.kind) {
            ScheduleKind.ONCE -> true
            ScheduleKind.DAILY -> ChronoUnit.DAYS.between(schedule.startsOn, date) % schedule.interval == 0L
            ScheduleKind.WEEKLY_DAYS, ScheduleKind.ONCE_PER_WEEK -> {
                val startWeek = schedule.startsOn.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val currentWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val weeks = ChronoUnit.WEEKS.between(startWeek, currentWeek)
                val activeDay = weeks % schedule.interval == 0L && date.dayOfWeek in schedule.days
                activeDay && (schedule.kind == ScheduleKind.WEEKLY_DAYS || cards.none {
                    it.taskId == id && it.status in setOf(CardStatus.DONE, CardStatus.DISMISSED) &&
                        !it.boardDate.isBefore(currentWeek) && it.boardDate.isBefore(currentWeek.plusWeeks(1))
                })
            }
        }
    }

    private fun SlidingTasksState.updateCard(card: TaskCard) =
        copy(cards = cards.map { if (it.id == card.id) card else it })

    private fun SlidingTasksState.withEvent(type: String, card: TaskCard) = copy(
        events = events + TaskEvent(newId(), type, now(), card.taskId, card.id, card.title),
    )

    private fun SlidingTasksState.withEvent(type: String, task: PlannedTask) = copy(
        events = events + TaskEvent(newId(), type, now(), task.id, title = task.title),
    )
}
