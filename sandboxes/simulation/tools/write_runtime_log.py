#!/usr/bin/env python3
"""Write the repository scenario's WNT Runtime observations as JSONL."""

from __future__ import annotations

import argparse
from pathlib import Path
import sys


SIMULATION_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SIMULATION_ROOT / "src"))
runtime = Path.home() / ".wnt" / "runtime" / "mrl"
if runtime.is_dir():
    sys.path.insert(0, str(runtime))

from app.simulation.mrl_runtime_scenario import create_simulation  # noqa: E402
from mrl_simulation_runtime.runner import SimulationRunner  # noqa: E402


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, help="write JSONL to this path instead of stdout")
    args = parser.parse_args()
    result = SimulationRunner().run(create_simulation())
    content = result.observations.to_jsonl()
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(content, encoding="utf-8")
    else:
        sys.stdout.write(content)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
