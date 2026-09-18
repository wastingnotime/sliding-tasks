from mrl_simulation_runtime.runner import SimulationRunner
from mrl_simulation_runtime.observations import ObservationLog
from mrl_simulation_runtime.control import start_control_session
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
    assert observations[-1].name == "sliding-tasks-shared-environment"
    assert {o.type for o in observations} >= {"beam", "actor_intention", "command", "use_case", "domain_event"}
    assert {o.actor for o in observations if o.actor} >= {"Planner", "User", "DayBoundary", "Analyst"}
    invariant_results = [o for o in observations if o.type == "invariant_result"]
    assert invariant_results
    assert all(o.payload["passed"] for o in invariant_results)


def test_runtime_log_tool_writes_parseable_jsonl(tmp_path: Path) -> None:
    output = tmp_path / "runtime.jsonl"
    tool = Path(__file__).parents[1] / "tools" / "write_runtime_log.py"
    subprocess.run([sys.executable, str(tool), "--output", str(output)], check=True)
    log = ObservationLog.from_jsonl(output.read_text(encoding="utf-8"))
    assert len(log.observations) == 80
    assert log.observations[0].name == "sliding-tasks-shared-environment"


def test_runtime_control_exposes_actor_driven_beams() -> None:
    scenario = create_simulation()
    assert scenario.scheduled_actions == []
    session = start_control_session(scenario)
    state = session.state()
    assert state.active_actors == ("Planner", "User", "DayBoundary", "Analyst")
    assert set(state.selected_actor_behaviors.values()) == {"BeamActorBehavior"}
    assert state.next_scheduled_event_time == scenario.initial_time


def test_observatory_declares_unique_beam_and_effect_endpoints() -> None:
    scenario = create_simulation()
    node_ids = [node.id for node in scenario.observatory_nodes]
    assert len(node_ids) == len(set(node_ids))
    assert {"planning-beam", "daily-life-beam", "day-boundary-beam", "analytics-beam"} <= set(node_ids)
    assert {"create_task", "open_day", "close_day", "complete_card", "get_analytics"} <= set(node_ids)
    assert {"CardGenerated", "CardDone", "CardMissed", "AnalyticsProjection"} <= set(node_ids)
    assert all(edge.from_node in node_ids and edge.to_node in node_ids for edge in scenario.observatory_edges)
