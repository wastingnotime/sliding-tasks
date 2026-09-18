# Model EGD candidate — Sliding Tasks

## Scope compared

- Governing sentence and product constraints from the supplied handoff
- Semantic hypothesis and slices 001–009
- Deterministic test suite
- Runtime JSONL evidence and invariant observations

## Candidate checks

- Task → Card → Event boundary is observable.
- Done, dismissed, missed, touch, reorder, and jump remain factual distinctions.
- One-time carry-forward and configurable dismissal policy are explicit.
- Recurrence and active eligibility are deterministic date predicates.
- Historical card/type snapshots protect analytics from later task edits.
- Runtime adapter and evidence writer remain thin non-domain boundaries.
- Runtime observations satisfy the stable JSONL field and serialization
  contract.

## Current result

The model is coherent enough for an EGD review. The checker is evidence
preparation, not a release decision. Release remains deferred until the open
policy and usefulness questions are explicitly accepted.
