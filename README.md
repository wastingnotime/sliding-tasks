# Sliding Tasks

Sliding Tasks explores a present-focused task board whose ordinary interactions
produce useful behavioral history. The repository contains the released MRL
simulation and a native Android client scaffold.

The governing product principle is:

> The past is observed. The future is configured. The present is acted upon.

## Simulation

The simulation lives in [`sandboxes/simulation`](sandboxes/simulation/README.md).
It models task rules, today's generated cards, low-friction decisions, day
closure, event history, and basic analytics with deterministic in-memory state.

```bash
python3 -m pytest
mrl-simulation supervise --once
```

Source evidence and active MRL artifacts are retained under `work/`; validation
receipts belong under `runs/`.

## Mobile

The Kotlin and Jetpack Compose client lives in [`apps/mobile`](apps/mobile/README.md).
For Milestone 1, mobile owns task planning, today's board, decisions, and local
history without a product API. Daily operation must remain offline-capable in
later milestones; planning may gain optional synchronization without making the
API a runtime dependency.

```bash
make mobile-test
make mobile-apk
```

## Deferred surfaces

- A receive-only event API may later collect immutable events emitted by mobile.
- Optional synchronization may later extend planning across devices.
- A web application may later provide full analytics from those collected events.

None of these surfaces is required for daily mobile operation.
