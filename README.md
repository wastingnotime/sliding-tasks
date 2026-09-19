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
It currently implements a local today-board vertical slice while the product API
transport remains to be defined.

```bash
make mobile-test
make mobile-apk
```
