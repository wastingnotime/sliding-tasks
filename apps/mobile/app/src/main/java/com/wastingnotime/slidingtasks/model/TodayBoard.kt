package com.wastingnotime.slidingtasks.model

data class TodayCard(
    val id: String,
    val title: String,
    val typeLabel: String,
)

data class TodayBoard(
    val dateLabel: String,
    val cards: List<TodayCard>,
)

sealed interface CardCommand {
    val cardId: String

    data class Complete(override val cardId: String) : CardCommand
    data class Dismiss(override val cardId: String) : CardCommand
    data class Touch(override val cardId: String) : CardCommand
}

enum class SlideDecision {
    COMPLETE,
    DISMISS,
}

fun slideDecision(offsetX: Float, containerWidth: Float, thresholdFraction: Float = 0.28f): SlideDecision? {
    if (containerWidth <= 0f) return null
    val threshold = containerWidth * thresholdFraction
    return when {
        offsetX >= threshold -> SlideDecision.COMPLETE
        offsetX <= -threshold -> SlideDecision.DISMISS
        else -> null
    }
}

fun TodayBoard.after(command: CardCommand): TodayBoard = when (command) {
    is CardCommand.Complete,
    is CardCommand.Dismiss -> copy(cards = cards.filterNot { it.id == command.cardId })
    is CardCommand.Touch -> this
}
