# Slice 009: runtime evidence packet

## Question

Can the repository produce replayable runtime evidence from its standard WNT
scenario without coupling domain code to the runtime?

## Contract

`tools/write_runtime_log.py` runs the conventional adapter through
`SimulationRunner` and writes the runtime observation log as JSONL to stdout or
an explicit output path. It is a local evidence tool, not a product API.

## Done criteria

The output can be parsed by the runtime observation log, includes scenario and
domain observations, and is suitable for inspection/replay preparation.

