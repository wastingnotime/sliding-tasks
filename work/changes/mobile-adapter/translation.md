# Sliding Tasks Mobile Translation Map

## Boundary Decision

- adapter authority: `local-first-mobile`
- Milestone 1 network dependency: none
- future event receiver: deferred
- future planning synchronization: optional and deferred

## Translation Chain

Use this table before implementation starts. Do not translate simulation code
directly into mobile code; translate released semantics into adapter behavior.

| Simulation slice | Local mobile behavior | Mobile use case | UI state | Evidence |
| --- | --- | --- | --- | --- |
| `001-today-decision-loop` | Open and persist active board | View today | Cards or empty | engine tests and APK build |
| `006-recurrence-sufficiency` | Generate daily, weekday, and weekend cards | Plan recurrence | Task form and plan list | recurrence unit test |
| `007-task-lifecycle` | Create, pause, and reactivate tasks | Manage plan | Active switch and resolved state | lifecycle unit test |
| `005-one-time-dismissal-policy` | Resolve one-time task on done or dismiss | Decide card | Card leaves today | engine unit test |
| `009-runtime-evidence-packet` | Append immutable local events | Review history | History event feed | engine and gesture tests |

## Implementation status

- Native project: Kotlin, Jetpack Compose, Android API 26+.
- Implemented: local task creation, task types, daily/weekday/weekend/one-time
  recurrence, activation, day generation and rollover, durable device storage,
  immutable event history, independently sliding today cards, accessibility
  actions, unit tests, an instrumented planning-to-completion test, and debug
  APK CI.
- Deferred: event upload, synchronization, authentication, and web analytics.

## Checks

- Local-first authority is explicit.
- Daily operations have no transport dependency.
- Failed local writes do not advance visible state.
- APK CI runs unit tests before artifact upload.
