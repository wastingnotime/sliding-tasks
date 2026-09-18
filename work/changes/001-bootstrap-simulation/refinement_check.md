# Refinement check

## Compared

- Initial handoff and product constraints
- Slice 001 contract
- Slice 002 sequence experiment contract
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

## Refinement update — sequence experiment

The replay confirms that explicit reorder and jump can be represented as
low-cost facts: the selected card remains pending until a decision, the other
cards retain their relative order, and task rules are untouched. The current
analytics projection intentionally counts neither as an interpretation. Keep
both events in the raw history for now, but defer navigation-specific metrics
until a longer multi-day replay can show whether they explain outcomes or
change user understanding. No new domain boundary was discovered.

## Build update — metric usefulness

The shared projection now compares generated, touched, done, dismissed, and
missed outcomes by task and task type, and retains touch counts per card until
first resolution. A multi-day deterministic replay makes the comparisons
observable without adding journaling or behavioral labels. Weekly targets,
forecasting, and inferred interpretation remain intentionally out of scope.

## Refinement update — historical metric labels

Review of the multi-day projection found that task metrics used the current
Task title/type, which could rewrite historical meaning after a task edit. Task
metrics now label themselves from the first immutable `CardGenerated` snapshot;
the edit scenario verifies that old history remains attributable to its
original classification while later cards use the new definition. Per-period
type transitions remain an open experiment before model EGD.

## Build update — type-transition projection

The open type-transition question is now executable. Type analytics classify
each occurrence from its card-generation snapshot, so a later task edit splits
old and new outcomes into their respective buckets without rewriting history.
Taxonomy migration and naming policy remain out of scope.

## Refinement update — type-transition replay

The full runtime replay remains invariant-clean, and the dedicated transition
scenario separates one historical `chore` occurrence from a later `skill`
occurrence after a task edit. This confirms the projection boundary is stable
without introducing a second event stream or rewriting facts. The core model is
coherent for the four built slices, but EGD is still deferred while one-time
dismissal policy, recurrence sufficiency, and the usefulness of sequence and
navigation facts remain open.

## Build update — one-time dismissal policy

The current assumption is now parameterized as a controlled experiment: the
default resolves a dismissed one-time intention, while the alternate mode keeps
the Task eligible for tomorrow. Both paths preserve the same factual card
event. This exposes the product choice for evaluation without changing the
default model.

## Refinement update — dismissal-policy isolation

The alternate policy replay now verifies that the switch affects only
`CardDismissed` eligibility. Explicit `CardDone` still resolves a one-time
Task in both modes, so the experiment does not blur decision semantics. Current
evidence supports retaining the default-resolves assumption while gathering
user-behavior evidence before selecting a final product policy.

## Build update — recurrence sufficiency

The deterministic recurrence vocabulary is now exercised through controlled
board generation across weekday, weekend, specific-weekday, and nth-day dates.
No scheduling policy or frequency target was introduced; recurrence remains a
date eligibility predicate for today's board.
