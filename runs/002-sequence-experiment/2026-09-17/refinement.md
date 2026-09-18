# Sequence experiment refinement receipt

- Slice: `002-sequence-experiment`
- Date: 2026-09-17

## Evidence

```text
python3 -m pytest
13 passed in 0.03s

Deterministic replay:
  visible after reorder + jump + done: first, second
  sequence events: CardReordered, CardJumped
  card outcome: CardDone (only after explicit decision)
```

## Decision

Retain reorder and jump as factual event types in the shared simulation. Do
not add navigation-specific analytics yet; gather longer histories first.
Sequence remains guidance, not scheduling, and the model is not released.

