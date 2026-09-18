#!/usr/bin/env python3
"""Run lightweight model-EGD checks for the current simulation scope."""

from __future__ import annotations

from pathlib import Path
import json
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
    scenario = create_simulation()
    observations = SimulationRunner().run(scenario).observations.observations
    names = {observation.name for observation in observations}
    invariant_results = [observation.payload.get("passed") for observation in observations if observation.type == "invariant_result"]
    analytics = next(observation for observation in observations if observation.name == "basic_analytics")
    records = [observation.to_record() for observation in observations]
    stable_fields = {"sim_time", "type", "name", "payload"}
    checks = {
        "required_domain_events": REQUIRED_EVENTS <= names,
        "invariants_pass": all(invariant_results),
        "analytics_by_task": "by_task" in analytics.payload,
        "analytics_by_type": "by_type" in analytics.payload,
        "sequence_activity": "sequence_activity" in analytics.payload,
        "runtime_record_shape": all(stable_fields <= set(record) for record in records),
        "runtime_jsonl_serializable": _is_json_serializable(records),
        "runtime_actors_present": {actor.name for actor in scenario.actors} == {"Planner", "User", "DayBoundary", "Analyst"},
        "behavior_beams_present": sum(observation.type == "beam" for observation in observations) == 4,
        "explicit_use_case_flow": {"actor_intention", "command", "use_case_decision", "domain_event"} <= {observation.type for observation in observations},
    }
    for name, passed in checks.items():
        print(f"{name}: {'PASS' if passed else 'FAIL'}")
    print(f"observations: {len(observations)}")
    return 0 if all(checks.values()) else 1


def _is_json_serializable(records: list[dict[str, object]]) -> bool:
    try:
        json.dumps(records)
    except TypeError:
        return False
    return True


if __name__ == "__main__":
    raise SystemExit(main())
