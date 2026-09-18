# Model hypothesis

## Governing sentence

Turn recurring intentions into today's decisions, capture the resulting
behavioral signals with minimal interaction, and use those signals to learn
over time.

## Vocabulary and boundary

- **Task** is an intention and generation rule.
- **Card** is one task occurrence on one date, with historical snapshots.
- **Event** is an immutable lifecycle or interaction fact.
- **Board** is the ordered set of actionable cards for the active date.

The simulation boundary owns deterministic task configuration, day lifecycle,
card decisions, factual history, and basic derived metrics. It excludes UI,
persistence, authentication, scheduling infrastructure, external enrichment,
and production technology choices.

## State transitions

```text
pending --done------> done
pending --dismiss---> dismissed
pending --close day-> missed
pending --touch-----> pending
```

A one-time task becomes resolved only on explicit done or dismiss. A missed
one-time card closes that occurrence but leaves the task eligible tomorrow.

## Current recurrence hypothesis

The first slice supports `daily`, `weekdays`, `weekends`, specific weekdays,
and nth day of month. Frequency targets are deliberately excluded.

## Candidate slice map

1. **Today decision loop** — built: task generation, interactions, day closure,
   carry-forward, event-derived analytics, runtime observation.
2. **Sequence experiment** — built: explicit reorder and jump observations are
   factual, non-resolving actions; usefulness remains under evaluation.
3. **Metric usefulness** — built: task/type groupings and touches before
   resolution are derived from longer deterministic histories; usefulness is
   still evaluated before release.
4. **Type-transition projection** — built: type metrics use each occurrence's
   card snapshot when task classification changes.
5. **One-time dismissal policy** — built as a controlled experiment; default
   behavior resolves the intention, alternate behavior carries it forward.
6. **Recurrence sufficiency** — built: deterministic date predicates drive
   board generation; frequency targets remain excluded.
4. **Model release adapters** — gated: define mobile/API-facing contracts only
   after model EGD accepts the domain.

## Open questions

- Is every expansion worth a `CardTouched`, or only the first?
- Does sequence need explicit reordering semantics?
- Is task type domain behavior or visual classification?
- Which time-derived and touch-derived metrics are genuinely useful?
- Should one-time dismissal permanently resolve the intention? The current
  experiment says yes and makes that assumption visible in tests.
