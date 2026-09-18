from mrl_simulation_runtime.runner import SimulationRunner
from mrl_simulation_runtime.observations import ObservationLog
from pathlib import Path
import subprocess
import sys

from app.simulation.mrl_runtime_scenario import create_simulation


def test_runtime_scenario_emits_domain_evidence_and_finishes() -> None:
    result = SimulationRunner().run(create_simulation())
    observations = result.observations.observations
    names = [observation.name for observation in observations]

    assert "CardGenerated" in names
    assert "CardTouched" in names
    assert "CardDone" in names
    assert "CardDismissed" in names
    assert "CardMissed" in names
    assert "CardReordered" in names
    assert "CardJumped" in names
    assert "basic_analytics" in names
    assert observations[-1].name == "sliding-tasks-today-decision-loop"
    invariant_results = [o for o in observations if o.type == "invariant_result"]
    assert invariant_results
    assert all(o.payload["passed"] for o in invariant_results)


def test_runtime_log_tool_writes_parseable_jsonl(tmp_path: Path) -> None:
    output = tmp_path / "runtime.jsonl"
    tool = Path(__file__).parents[1] / "tools" / "write_runtime_log.py"
    subprocess.run([sys.executable, str(tool), "--output", str(output)], check=True)
    log = ObservationLog.from_jsonl(output.read_text(encoding="utf-8"))
    assert len(log.observations) == 35
    assert log.observations[0].name == "sliding-tasks-today-decision-loop"
