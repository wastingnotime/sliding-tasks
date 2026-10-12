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

Done and Not today expose a temporary Undo action after the outcome is saved.
Undo returns the same card to pending, restores the prior active/resolved state
of a one-time task, and appends `CardResolutionUndone` while retaining the
outcome event. It shares the five-second, accessibility-adjusted window and
navigation/expiry behavior of removal Undo. Only the latest card outcome or
task removal can be undone; touching a card does not replace that opportunity.

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

- Today accepts an ad-hoc one-time title and immediately creates its pending
  card on the open board. The local task record still supports carry-forward
  after a missed day and preserves card history. One-time creation and management
  do not require the Plan tab; older paused one-time entries can be resumed or
  removed from Today.
- Plan lists routines and unresolved one-time tasks. Its editor offers Routine,
  Once per week, and One-time. Once per week is a weekly schedule for a stored
  Routine task, not a separate stored task type.
- Routine offers daily repetition every 1–7 days or a separate occurrence on
  each selected weekday every 1–4 weeks. Once per week offers one occurrence
  across selected eligible weekdays every 1–4 weeks. Existing schedules with
  larger intervals remain intact until changed. Daily intervals use the chosen
  first date as their anchor; weekly intervals use the Monday–Sunday week
  containing that date. Neither schedule generates cards before its first date.
- Weekly day selections support any combination, with any-day, weekday, and
  weekend presets. For Routine, Done or Skip today closes only that day's card.
  For Once per week, Done or Skip this week closes the occurrence until the
  next active week; a missed card can recur on the next selected day. Separate
  Saturday and Sunday routines can share an anchor week for paired events.
- Planned tasks can be reordered by dragging, with Move up and Move down
  accessibility actions. Plan order determines generation order for future
  boards and does not rewrite an already-generated board.
- Planned routines can be edited in place. Title, recurrence, availability,
  and first active date changes apply to future cards. An eligible edit may add
  a card to the open day if that task has no card for the day. Existing card
  snapshots and immutable history are preserved; changes record `TaskUpdated`.
- Removing a task prevents future card generation. Existing cards and immutable
  history remain available so removing a plan never rewrites past facts.
- After a successful removal, a temporary Undo action restores the same task
  and its plan position for approximately five seconds, extended by Android
  accessibility timeout settings. It remains available across tab navigation;
  expiry, leaving the app, process loss, or a subsequent removal ends that
  opportunity. There is no later task recovery screen. Undo preserves cards and
  existing events and records `TaskRestored`. A one-time task whose retained card
  was resolved meanwhile remains resolved when restored.
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

## First-use learning

- An empty installation offers an optional practice guide, immediate Start, and
  Restore backup. Installations containing tasks or history bypass the welcome.
- Practice teaches Routine, Once per week and One-time through separate Plan
  exercises, then example cards using the actual Today swipe interaction,
  equivalent custom accessibility actions, Undo, and the actual local Review
  presentation. Swipe practice has no persistent Done or Skip buttons.
- Practice state is isolated from operational task storage and backup/export
  files. It must never create real tasks or history. First-use preferences may
  retain tutorial progress independently of operational state.
- Skip remains available throughout; About can reopen the guide. Finishing or
  skipping remembers the choice per installation. Successful restore bypasses
  the guide; failed or cancelled restore keeps Learn and Start available.
- Completing the guide opens task creation when operational state is empty,
  otherwise returns to the existing app. Human comprehension evidence remains
  separate from automated verification.

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

## Optional video walkthrough

About places a localized “Watch walkthrough” link alongside the offline practice
guide. It opens the English or Brazilian Portuguese YouTube video externally,
using the app's resource language, and states that internet is required. A
missing external handler reports an error without affecting task data. The
interactive practice guide remains available offline.
