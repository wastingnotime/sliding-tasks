# Slice 003: metric usefulness

## Question

Which low-cost projections from the factual event stream help explain a
history without asking the user for separate journaling?

## Contract

Given a multi-day event history, expose metrics grouped by task and task type,
plus the number of touches recorded before each card's first resolution. All
values are derived from events; no behavioral interpretation is introduced.

## Scenario and done criteria

Replay a regular task across successful, touched-then-done, and missed days,
alongside a dismissed task. The projection reports generated/done/dismissed/
missed/touched counts by task and type, and preserves touch counts per card.

## Decision boundary

Retain metrics that make task history comparable. Defer weekly/monthly targets,
forecasting, inferred co-occurrence, and navigation-specific interpretation.

