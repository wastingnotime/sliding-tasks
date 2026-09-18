# Observatory topology correction

- Removed the generic use-case topology.
- Removed misleading inbound-adapter nodes named `Something Beam`; beams are
  rendered flows, not architecture components.
- Declared individual use-case and domain-event nodes.
- Added explicit actor → use case → aggregate → event paths.
- Added metadata validation for unique nodes and valid edge endpoints.
- Browser QA at `http://127.0.0.1:8765/observatory` confirmed distinct ranked
  nodes and visible animated effect beams during playback.
- Actors use an explicit leftmost rank, followed by use cases, aggregate, and
  events/projection.
- Use-case decision observations create visible effect beams between each use
  case and the aggregate.
- Browser playback confirmed actor → use case, use case → aggregate, and
  aggregate → event effect beams with no inbound-adapter beam nodes.
- Removed `use_case_id` from decision observations because the observatory
  interpreted it as a self-target and discarded the use case → aggregate beam.
- Refined topology semantics with explicit `route`, `command`, and `event`
  edge kinds, plus actor/use-case/aggregate/event/projection badges and node
  descriptions for the observatory inspector.
- Use-case decision payloads now identify the semantic role
  `use_case_to_aggregate` without changing target inference.
- Verification: 28 tests passed, model checks passed, and the existing
  supervised browser session remained available on port 8765.
