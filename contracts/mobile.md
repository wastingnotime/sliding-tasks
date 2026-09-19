# Sliding Tasks Mobile Contract

## Purpose

The Sliding Tasks mobile app is the native Android consumer of the product API.
It does not own lifecycle policy, persistence, or projection semantics.

## Boundary

Adapter authority: `direct-api`.

The app will consume the product API directly. There is no mobile BFF and the
client does not own an offline-sync authority. Until an API transport contract
is released, the executable scaffold uses local sample state only.

## Today board

The first app-facing boundary is an ordered list of pending cards for the active
board date. Each visible card needs `id`, title snapshot, and task-type label.

User actions map to released commands:

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

Transport routes, DTO field names, and authentication are unresolved API
contract gaps; they must be added here before real network wiring.

## Failure and freshness states

- A missing active board is shown as an explicit unavailable state, not as an
  empty successful board.
- A stale server revision keeps the card visible and asks the user to refresh.
- Transport failures preserve the last visible board and expose retry.
- Command failures do not optimistically remove a card unless rollback is
  implemented and tested.

## Expected Mapping

Translate released simulation behavior through this chain:

```text
simulation slice -> released API behavior -> mobile use case -> UI state -> test evidence
```

## Runtime Assumptions

- emulator API base URL: `http://10.0.2.2:18080`
- physical device API base URL: override through Gradle or platform config
- auth: `none yet`
- notifications: not implemented; no scheduling assumptions are made
- telemetry: not implemented; no user interaction leaves the device yet

## APK CI

The mobile surface should expose a debug APK artifact after unit tests pass.
Use `wnt mobile apk-ci init` when this workflow is missing.
