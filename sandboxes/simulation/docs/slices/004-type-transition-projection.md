# Slice 004: type-transition projection

## Question

When a task changes type, should historical type analytics follow the current
definition or the classification captured on each occurrence?

## Contract

`metrics_by_type` groups every generated card by its immutable
`task_type_snapshot`, then attributes touches and terminal outcomes to that
same occurrence classification. A later Task edit affects only future cards.

## Done criteria

A task generated as `chore`, edited to `skill`, and generated again produces
separate type buckets with correct outcomes. No historical event is rewritten.

## Out of scope

Renaming policy, category migration, forecasting, and user-facing taxonomy.

