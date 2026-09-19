# Sliding Tasks Mobile Translation Map

## Boundary Decision

- adapter authority: `direct-api`
- API name: `the product API`
- emulator base URL: `http://10.0.2.2:18080`
- auth strategy: `none yet`

## Translation Chain

Use this table before implementation starts. Do not translate simulation code
directly into mobile code; translate released semantics into adapter behavior.

| Simulation slice | Released API behavior | Mobile use case | UI state | Unit test | Instrumented test | Contract gap |
| --- | --- | --- | --- | --- | --- | --- |
| `001-today-decision-loop` | Read/open active board | View today | Loading, cards, empty, unavailable, stale, failure | reducer/state mapping | board renders ordered cards | API route and DTO pending |
| `007-task-lifecycle` | Complete pending card | Mark done | Command pending, removed after success, failure | complete removes matching card | Done action dispatches command | API route/error mapping pending |
| `005-one-time-dismissal-policy` | Dismiss pending card | Not today | Command pending, removed after success, failure | dismiss removes matching card | Dismiss action dispatches command | API route/error mapping pending |
| `009-runtime-evidence-packet` | Record touch | Focus/view card | Board unchanged | touch preserves board | card focus dispatches touch | Trigger and API route pending |

## Implementation status

- Native project: Kotlin, Jetpack Compose, Android API 26+.
- Implemented: independently sliding cards in the today list,
  right-to-complete and left-to-dismiss thresholds with snap-back and no
  rotation, accessible outcome buttons, empty state,
  configurable emulator/physical-device API base URL, unit tests, debug APK CI.
- Temporary: sample state in `MainActivity`; it is not domain authority.
- Blocked on released API behavior: network routes, DTOs, auth, freshness token,
  and public transport errors.

## Checks

- Adapter authority named before scaffolding.
- Transport field mapping recorded.
- Stale or missing records represented as user-visible state.
- Runtime base URL works for Android emulator.
- APK CI runs unit tests before artifact upload.
