#!/usr/bin/env python3
"""Run lightweight model-EGD checks for the current simulation scope."""

from __future__ import annotations

from pathlib import Path
import sys

SIMULATION_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SIMULATION_ROOT / "src"))
runtime = Path.home() / ".wnt" / "runtime" / "mrl"
if runtime.is_dir():
    sys.path.insert(0, str(runtime))

from app.simulation.mrl_runtime_scenario import create_simulation  # noqa: E402
from mrl_simulation_runtime.runner import SimulationRunner  # noqa: E402

REQUIRED_EVENTS = {"TaskCreated", "CardGenerated", "CardTouched", "CardDone", "CardDismissed", "CardMissed", "CardReordered", "CardJumped"}


def main() -> int:
    observations = SimulationRunner().run(create_simulation()).observations.observations
    names = {observation.name for observation in observations}
    invariant_results = [observation.payload.get("passed") for observation in observations if observation.type == "invariant_result"]
    analytics = next(observation for observation in observations if observation.name == "basic_analytics")
    checks = {
        "required_domain_events": REQUIRED_EVENTS <= names,
        "invariants_pass": all(invariant_results),
        "analytics_by_task": "by_task" in analytics.payload,
        "analytics_by_type": "by_type" in analytics.payload,
        "sequence_activity": "sequence_activity" in analytics.payload,
    }
    for name, passed in checks.items():
        print(f"{name}: {'PASS' if passed else 'FAIL'}")
    print(f"observations: {len(observations)}")
    return 0 if all(checks.values()) else 1


if __name__ == "__main__":
    raise SystemExit(main())

