package com.wastingnotime.slidingtasks.model

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

enum class Recurrence(val label: String) {
    ONCE("Once"),
    DAILY("Daily"),
    WEEKDAYS("Weekdays"),
    WEEKENDS("Weekends"),
    EVERY_TWO_DAYS("Every 2 days"),
    WEEKLY("Every week"),
    EVERY_TWO_WEEKS("Every 2 weeks"),
}

enum class AvailableDays(val label: String) {
    ANY_DAY("Any day"),
    WEEKDAYS("Weekdays"),
    SATURDAY("Saturday"),
    SUNDAY("Sunday"),
}

enum class CardStatus { PENDING, DONE, DISMISSED, MISSED }

data class PlannedTask(
    val id: String,
    val title: String,
    val type: TaskType,
    val recurrence: Recurrence,
    val active: Boolean,
    val createdOn: LocalDate,
    val resolved: Boolean = false,
    val availableDays: AvailableDays = AvailableDays.ANY_DAY,
    val startsOn: LocalDate = createdOn,
)

data class TaskCard(
    val id: String,
    val taskId: String,
    val boardDate: LocalDate,
    val title: String,
    val type: TaskType,
    val status: CardStatus = CardStatus.PENDING,
    val touches: Int = 0,
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
                )
            }
        next = next.copy(activeDate = date, cards = next.cards + generated)
        return generated.fold(next) { current, card -> current.withEvent("CardGenerated", card) }
    }

    fun createTask(
        state: SlidingTasksState,
        title: String,
        type: TaskType,
        recurrence: Recurrence,
        date: LocalDate,
        availableDays: AvailableDays = AvailableDays.ANY_DAY,
        startsOn: LocalDate = date,
    ): SlidingTasksState {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Task title cannot be empty" }
        require((type == TaskType.ONE_TIME) == (recurrence == Recurrence.ONCE)) {
            "One-time tasks use Once; recurring tasks require a recurrence"
        }
        require(recurrence in setOf(Recurrence.WEEKLY, Recurrence.EVERY_TWO_WEEKS) || availableDays == AvailableDays.ANY_DAY) {
            "Day windows are only available for weekly routines"
        }
        val task = PlannedTask(newId(), cleanTitle, type, recurrence, active = true, createdOn = date,
            availableDays = availableDays, startsOn = startsOn)
        val created = state.copy(tasks = state.tasks + task).withEvent("TaskCreated", task)
        return if (created.activeDate == date && task.isEligible(date, created.cards)) openDay(created, date) else created
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
        if (!active || resolved || createdOn > date || startsOn > date) return false
        return when (recurrence) {
            Recurrence.ONCE -> true
            Recurrence.DAILY -> true
            Recurrence.WEEKDAYS -> date.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            Recurrence.WEEKENDS -> date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            Recurrence.EVERY_TWO_DAYS -> ChronoUnit.DAYS.between(startsOn, date) % 2L == 0L
            Recurrence.WEEKLY, Recurrence.EVERY_TWO_WEEKS -> {
                val startWeek = startsOn.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val currentWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val weeks = ChronoUnit.WEEKS.between(startWeek, currentWeek)
                val matchingWeek = recurrence == Recurrence.WEEKLY || weeks % 2L == 0L
                val matchingDay = when (availableDays) {
                    AvailableDays.ANY_DAY -> true
                    AvailableDays.WEEKDAYS -> date.dayOfWeek.value <= DayOfWeek.FRIDAY.value
                    AvailableDays.SATURDAY -> date.dayOfWeek == DayOfWeek.SATURDAY
                    AvailableDays.SUNDAY -> date.dayOfWeek == DayOfWeek.SUNDAY
                }
                matchingWeek && matchingDay && cards.none {
                    it.taskId == id && it.status in setOf(CardStatus.DONE, CardStatus.DISMISSED) &&
                        !it.boardDate.isBefore(currentWeek) && it.boardDate.isBefore(currentWeek.plusWeeks(1))
                }
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
