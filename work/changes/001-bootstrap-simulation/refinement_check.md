# Refinement check

## Compared

- Initial handoff and product constraints
- Slice 001 contract
- Deterministic tests for scenarios A-H
- WNT runtime scenario observations

## Current result

The model supports distinct conscious outcomes and end-of-day non-action while
keeping one-time carry-forward at the Task boundary. Card snapshots protect
historical meaning from task edits. Repeated touches remain uninterpreted facts.

## Learning and next decision

Counts and completion rate fall naturally out of the event stream. Richer
metrics should wait for longer histories. Continue refinement before model EGD;
the sequence/reordering question and metric usefulness remain open. This is not
a model release decision.

## Refinement update — runtime invariant semantics

The first runtime replay showed that a "no pending cards after final closure"
invariant was evaluated during intermediate open-day states and emitted
expected-but-noisy failures. The invariant now checks the actual invariant at
all observation points: a card may be pending, but it cannot acquire more than
one terminal outcome. The adapter test requires every emitted invariant result
to pass. Final-closure behavior remains covered by the domain tests.

## Build update — sequence experiment

The sequence question is now executable in the shared environment. Reorder and
jump are explicit, factual, non-resolving events; reordering changes only the
current board's guidance order. This keeps sequence separate from calendar
rigidity. Longer replay histories are still needed before deciding whether
these events materially improve analytics, so the model remains unreleased.
