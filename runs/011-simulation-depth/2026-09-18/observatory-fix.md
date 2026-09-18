# Observatory topology correction

- Removed the generic use-case topology.
- Declared four behavior-beam nodes.
- Declared individual use-case and domain-event nodes.
- Added explicit actor → beam → use case → aggregate → event paths.
- Added metadata validation for unique nodes and valid edge endpoints.
- Browser QA at `http://127.0.0.1:8765/observatory` confirmed distinct ranked
  nodes and visible animated effect beams during playback.
