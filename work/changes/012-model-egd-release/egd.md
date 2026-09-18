# Model EGD candidate — Sliding Tasks (current)

## Evidence scope

- Current semantic hypothesis and slices 001–010
- Explicit task lifecycle and missed-one-time policy refinements
- Release-candidate contract in `contracts/sliding-tasks-model.md`
- Deterministic domain and runtime test suite
- Runtime JSONL evidence and model checker output

## Candidate checks

- Task → Card → Event remains the owned domain boundary.
- Actors, behavior beams, and explicit application use cases are visible in
  the runtime adapter without moving domain behavior into the runtime.
- Lifecycle facts, recurrence validation, and missed-one-time policy are
  deterministic and replayable.
- Card snapshots preserve historical analytics after task edits.
- Runtime observations remain JSONL-serializable and invariant-backed.

## Release decision

The model was accepted by the repository owner on 2026-09-18. Production
mobile/API adapters may now synchronize from the accepted contract.
