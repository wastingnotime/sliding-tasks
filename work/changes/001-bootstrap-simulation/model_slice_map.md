# Model slice map

The first coherent slice is `today-decision-loop`, documented at
`sandboxes/simulation/docs/slices/001-today-decision-loop.md`.

The next built experiment is `sequence-experiment`, documented at
`sandboxes/simulation/docs/slices/002-sequence-experiment.md`. It adds only
explicit reorder and jump observations to the same shared environment.

The next built slice is `metric-usefulness`, documented at
`sandboxes/simulation/docs/slices/003-metric-usefulness.md`. It projects
longer-history comparisons by task/type and touches before resolution.

The next built slice is `type-transition-projection`, documented at
`sandboxes/simulation/docs/slices/004-type-transition-projection.md`. It keeps
type analytics stable per occurrence when task classification changes.

The next built slice is `one-time-dismissal-policy`, documented at
`sandboxes/simulation/docs/slices/005-one-time-dismissal-policy.md`. It makes
the current dismissal assumption comparable with carry-forward behavior.

The next built slice is `recurrence-sufficiency`, documented at
`sandboxes/simulation/docs/slices/006-recurrence-sufficiency.md`. It validates
deterministic recurrence rules through end-to-end board generation.

The next built slice is `task-lifecycle`, documented at
`sandboxes/simulation/docs/slices/007-task-lifecycle.md`. It validates active
rule changes against today's immutable generated cards.

The next built slice is `sequence-signal-projection`, documented at
`sandboxes/simulation/docs/slices/008-sequence-signal-projection.md`. It pairs
raw reorder/jump counts with card outcomes without interpretation.

Later candidates are model release adapter contracts. They remain gated on
further refinement and model EGD.
