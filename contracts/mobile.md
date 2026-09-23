# Sliding Tasks Mobile Contract

## Purpose

For Milestone 1, the Sliding Tasks mobile app is the autonomous planning and
daily-use product. It owns the local operational state needed to plan tasks,
generate and use the today board, record decisions, and preserve history
without a network service.

## Boundary

Authority: `local-first-mobile`.

In Milestone 1, mobile does not depend on an API for task planning or ordinary
operation. Its local store and application layer implement the released model
semantics. A future API may receive immutable events emitted by mobile and may
support optional planning synchronization, but it must not be on the critical
path for daily user actions.

Offline-capable daily operation is a durable product invariant. Mobile-only
planning is a milestone choice, not a permanent product constraint; later
milestones may extend planning across devices while preserving local-first
behavior.

A future web surface may derive full analytics from received events. Event
ingestion, planning synchronization, and web analytics are explicitly deferred
and must not block the Milestone 1 mobile product.

## Today board

The first app-facing boundary is an ordered list of pending cards for the active
board date. Each visible card needs `id`, title snapshot, and task-type label.

User actions map to locally executed released commands:

| Mobile action | Command intent | Visible result after success |
| --- | --- | --- |
| Open today's board | `OpenDay` | Ordered pending cards or an empty board |
| Done | `CompleteCard` | Card leaves the pending board |
| Not today | `DismissCard` | Card leaves the pending board |
| Focus/view a card | `TouchCard` | Board order is unchanged |

Cards remain independently visible in a vertical list. A horizontal slide on
any card records a touch when dragging begins. Crossing 28% of the card width
and releasing maps right to `CompleteCard` and left to `DismissCard`; a shorter
slide snaps back without resolving the card. Sliding uses horizontal translation
only, without rotation. Moving right exposes `DONE` on the left; moving left
exposes `NOT TODAY` on the right. The card exposes equivalent custom
accessibility actions without persistent outcome buttons.

Local persistence, task planning, and board generation are required Milestone 1
mobile capabilities. Synchronization semantics, event transport routes, DTO
field names, delivery guarantees, identity, and authentication remain deferred
contract gaps.

## Plan management

- Mobile routines may repeat daily, on weekdays, on weekends, every two days,
  once per week, or once per two weeks. Two-day intervals use a chosen first
  day. Two-week intervals use the Monday–Sunday week containing the chosen
  first date as their anchor; the selected week and every second week after it
  are active.
- Weekly and two-week routines have one occurrence per active week. Their
  availability is any day, weekdays, Saturday, or Sunday. Dismissing or
  completing a card closes the occurrence until the next active week. A missed
  card can recur on the next available day of that week. Separate Saturday and
  Sunday routines can share an anchor week for paired events.
- Planned tasks can be reordered by dragging, with Move up and Move down
  accessibility actions. Plan order determines generation order for future
  boards and does not rewrite an already-generated board.
- Planned entries can be edited in place. Title, type, recurrence, availability,
  and first active date changes apply to future cards. An eligible edit may add
  a card to the open day if that task has no card for the day. Existing card
  snapshots and immutable history are preserved; changes record `TaskUpdated`.
- Removing a task prevents future card generation. Existing cards and immutable
  history remain available so removing a plan never rewrites past facts.
- Removal and reordering record `TaskRemoved` and `TaskReordered` events.

## Local review

- The device provides a short recent review from locally stored cards. It shows
  this week's Done, Not today, Missed, and still-open counts; recent tasks most
  often missed or marked Not today; a task completed consistently; and a
  comparison of Done share across two complete seven-day windows when each has
  enough closed cards.
- Task patterns use the last 14 full days, excluding the current incomplete
  day. Missed means a card remained pending when its day ended. Not today is an
  explicit dismissal. Rankings count recorded card days, not inferred intent or
  causes. The comparison is descriptive and does not claim that a changed rate
  was caused by a particular behavior.
- Recent days can be expanded to inspect their individual card outcomes. The
  immutable event history remains stored locally for later web analytics, but
  the device review does not expose the raw event stream as its primary view.

## Failure and freshness states

- A missing active board is shown as an explicit unavailable state, not as an
  empty successful board.
- Local command or persistence failures preserve a recoverable board state and
  expose retry where appropriate.
- Failure to emit an event to the future receiver does not prevent planning or
  card actions. Pending delivery must be durable and retryable once event
  ingestion exists.

## Expected Mapping

Translate released simulation behavior through this chain:

```text
simulation slice -> mobile use case -> local persistence -> UI state -> test evidence
```

The deferred reporting path is separate:

```text
mobile event history -> event receiver API -> web analytics projection
```

## Runtime Assumptions

- product API: not required for mobile operation
- event receiver: deferred and undefined
- planning synchronization: deferred and optional
- event receiver auth: deferred and undefined
- notifications: not implemented; no scheduling assumptions are made
- telemetry/event upload: not implemented; no user interaction leaves the
  device yet

## APK CI

The mobile surface should expose a debug APK artifact after unit tests pass.
Use `wnt mobile apk-ci init` when this workflow is missing.
