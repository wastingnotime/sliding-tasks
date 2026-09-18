# Type-transition projection refinement receipt

## Replay evidence

```text
python3 -m pytest
17 passed in 0.03s

mrl-simulation supervise --once
listening on http://127.0.0.1:8765

runtime invariant results: [True]
runtime type projection: skill and chore buckets remain occurrence-based
```

## Decision

Keep occurrence-snapshot classification. The four built slices form a coherent
core model, but do not start model EGD yet: policy and usefulness questions in
the semantic hypothesis still need explicit experiments or decisions.

