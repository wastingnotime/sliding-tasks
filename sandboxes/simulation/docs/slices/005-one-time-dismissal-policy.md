# Slice 005: one-time dismissal policy experiment

## Question

Does dismissing a one-time intention resolve it permanently, or should it remain
eligible for another decision?

## Contract

The simulation defaults to dismissal resolving a one-time Task, matching the
current product hypothesis. A deterministic environment may opt into an
alternate experiment where dismissal closes only today's Card and the Task is
eligible tomorrow. Both modes retain the same `CardDismissed` event and do not
rewrite history.

Missed one-time cards use a separate explicit policy. The default carries the
intention forward and records `TaskCarriedForward`; an expire experiment records
`TaskExpired` and ends future eligibility.

## Done criteria

The default mode produces no next-day card after dismissal. The alternate mode
produces a fresh next-day occurrence, making the policy difference observable.

## Out of scope

User-facing settings, automatic policy selection, snooze semantics, and
multi-dismissal limits.
