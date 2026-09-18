# Slice 007: task lifecycle eligibility

## Question

How do active task-rule changes interact with today's already-generated board?

## Contract

Deactivating a Task prevents future card generation but does not rewrite or
remove the current day's Card. Reactivating the Task makes it eligible again on
later dates when its recurrence matches.

## Done criteria

An end-to-end scenario deactivates a daily task after today's generation,
confirms today's card remains stable, confirms tomorrow has no card, then
reactivates the task and confirms a later day generates a fresh occurrence.

