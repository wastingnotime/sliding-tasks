# Slice 001: today decision loop

## Shape

- Implementation pack: WNT MRL Python event-sourced simulation
- Runtime targets: headless pytest and WNT MRL Runtime supervision
- Architecture mode: deterministic in-memory event stream and projections
- Discovery scope: Task → Card → Event, day lifecycle, one-time carry-forward

## Use-case contract

Create and update task definitions; open exactly one operational day; generate
eligible cards in task creation order; add a one-time task during today; touch,
complete, or dismiss pending cards; close unresolved cards as missed; derive
basic counts and completion rate from events.

## Rules

- Card identity is unique per task and board date.
- Touch does not resolve and may repeat.
- Only pending cards accept interactions or closure.
- Regular recurrence creates a fresh occurrence, never carries a card.
- Missed one-time intentions remain eligible; done/dismissed ones do not.
- Task edits never mutate card snapshots.
- Past and future boards are not exposed as operational surfaces.

## Ports and tests

The slice needs a clock, ID source, and append-only event store; deterministic
in-memory implementations satisfy all three. Tests cover handoff scenarios A-H
and core invalid-transition invariants.

## Runtime scenario

The repository scenario runs three days containing regular success, dismissal,
repeated touch then miss, and one-time carry-forward then completion. Domain
events are translated to JSONL-compatible runtime observations.

## Done criteria

All rules above are executable; runtime factory loads and finishes with passing
invariants; source evidence and refinement findings are durable.

## Out of scope

UI gestures, databases, real clocks, authentication, external services,
frequency targets, reordering, forecasting, enriched context, and production
adapter contracts.

