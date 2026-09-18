# Sliding Tasks simulation

This is the repository's MRL simulation project. It is a disposable learning
environment, not the product or a production architecture.

## Current slice

The shared environment demonstrates:

```text
Task definitions -> controlled day generation -> today's card sequence
-> touch / done / dismiss / no action -> day closure
-> event history -> basic analytics
```

The implementation uses the WNT-required Python event-sourced simulation
shape: commands decide factual events, an append-only in-memory store retains
them, and state and analytics are projections of those events. Time and IDs are
deterministic.

The shared environment is driven by four runtime-visible actors—Planner, User,
DayBoundary, and Analyst. Each actor activates a repository-owned behavior beam
that schedules independent intentions. Explicit application use cases translate
those intentions into domain behavior and observations.

Run the executable examples:

```bash
python3 -m pytest
```

Run the repository scenario through the WNT MRL Runtime:

```bash
mrl-simulation supervise --once
```

Write replayable JSONL evidence when needed:

```bash
python3 sandboxes/simulation/tools/write_runtime_log.py --output /tmp/sliding-tasks-runtime.jsonl
```

Run the lightweight model-EGD checks:

```bash
python3 sandboxes/simulation/tools/check_model_egd.py
```

The runtime adapter is intentionally thin. Domain code does not import the WNT
runtime.
