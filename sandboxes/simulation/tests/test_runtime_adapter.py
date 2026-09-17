from mrl_simulation_runtime.runner import SimulationRunner

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
    assert "basic_analytics" in names
    assert observations[-1].name == "sliding-tasks-today-decision-loop"
    invariant_results = [o for o in observations if o.type == "invariant_result"]
    assert invariant_results
    assert all(o.payload["passed"] for o in invariant_results)
