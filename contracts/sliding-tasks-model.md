# Sliding Tasks model contract

Status: release candidate; gated on model-EGD acceptance.

## Owned boundary

Sliding Tasks turns task intentions and recurrence rules into dated cards on a
single active-day board. It records immutable facts about generation,
interaction, resolution, navigation, and task lifecycle, then derives analytics
from those facts.

The contract does not define authentication, persistence, transport, UI, push
notifications, or scheduling infrastructure.

## Concepts

| Concept | Meaning | Stable fields |
| --- | --- | --- |
| Task | An intention and generation rule | `id`, `title`, `task_type`, `subtype`, `recurrence`, `active` |
| Card | One task occurrence on one board date | `id`, `task_id`, `board_date`, snapshots, `status` |
| Event | Immutable historical fact | `event_id`, `event_type`, `occurred_at`, `task_id`, optional `card_id`, payload |
| Board | Ordered pending cards for the active date | `board_date`, ordered card IDs |

## Commands

`CreateTask`, `OpenDay`, `CloseDay`, `TouchCard`, `CompleteCard`,
`DismissCard`, `ReorderCard`, `JumpToCard`, and `GetAnalytics` are the current
runtime application boundary. Task updates and activation changes are domain
operations in the current simulation and remain a release-candidate extension
until exposed through an actor-driven application use case. Commands are
validated against the active-day and card state rules.

## Event vocabulary

Stable domain facts include:

- task lifecycle: `TaskCreated`, `TaskUpdated`, `TaskActivated`,
  `TaskDeactivated`, `TaskCarriedForward`, `TaskExpired`;
- occurrence lifecycle: `CardGenerated`, `CardDone`, `CardDismissed`,
  `CardMissed`;
- interaction/navigation: `CardTouched`, `CardReordered`, `CardJumped`.

Card title, type, and start-time snapshots are preserved at generation time so
later task edits do not rewrite history.

## Policies and invariants

- Only pending cards on the active board accept touch, navigation, or outcome
  commands.
- A card has at most one terminal outcome.
- Regular tasks require a valid recurrence; one-time tasks cannot have one.
- Eligible tasks created during an open day generate immediately; ineligible
  recurring tasks wait for their next matching date.
- Missed one-time tasks default to `carry_forward` and emit
  `TaskCarriedForward`; the opt-in `expire` policy emits `TaskExpired`.
- Done and dismissed one-time tasks resolve the intention by default.
- Closing a day converts remaining pending cards to `CardMissed`.

## Projections

The current read models are today’s ordered board, analytics by task, analytics
by task type, touches before resolution, and sequence activity. Projection
fields are derived and may evolve without changing event meaning.

## Stability classification

- **Stable candidate:** Task/Card/Event boundary, event vocabulary, command
  names, recurrence validation, terminal-card invariant, and missed-task
  policy names.
- **Experimental:** analytics usefulness, sequence interpretation, default
  dismissal policy, and projection field shapes.
- **Internal only:** Python classes, in-memory event store, deterministic IDs,
  runtime actors/beams, and observatory topology.
