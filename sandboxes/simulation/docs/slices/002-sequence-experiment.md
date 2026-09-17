# Slice 002: sequence experiment

## Question

Does the board need explicit sequence behavior, and is moving to another card
itself useful factual history, without making the board a rigid calendar?

## Contract

While today is open, a user may move a pending card to another sequence
position (`reorder_today_cards`) or jump directly to a pending card
(`jump_to_card`). Each action records a factual event and leaves card status
unchanged. Reordering changes today's guidance order only; it does not rewrite
task rules, historical cards, or time slots.

## Scenario and done criteria

Generate three cards, move the third to the front, jump to it, and complete it.
The remaining cards retain their relative order and remain pending. The event
stream contains `CardReordered` and `CardJumped`; no calendar or scheduling
semantics are introduced.

## Out of scope

Automatic ranking, start-time enforcement, drag gesture details, persistence,
and interpreting navigation as hesitation or procrastination.

## Refinement target

Use the event sequence and simple replay to decide whether these facts improve
understanding enough to keep the behavior in the model.

