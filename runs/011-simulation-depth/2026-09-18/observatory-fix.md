# Observatory topology correction

- Removed the generic use-case topology.
- Declared four behavior-beam nodes.
- Declared individual use-case and domain-event nodes.
- Added explicit actor → beam → use case → aggregate → event paths.
- Added metadata validation for unique nodes and valid edge endpoints.
- Browser QA at `http://127.0.0.1:8765/observatory` confirmed distinct ranked
  nodes and visible animated effect beams during playback.
- Actors use an explicit leftmost rank, followed by behavior beams and use
  cases, so runtime style mappings cannot invert those two layers.
- Browser QA confirmed the rendered order is actors → beams → use cases →
  aggregate → events/projection.
