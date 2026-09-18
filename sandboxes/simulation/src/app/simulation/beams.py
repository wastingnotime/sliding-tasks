from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from typing import Callable


BeamActionCallback = Callable[[object, str], None]


@dataclass(frozen=True)
class BeamAction:
    when: datetime
    name: str
    use_case_id: str
    execute: BeamActionCallback


@dataclass(frozen=True)
class BehaviorBeam:
    """One deterministic stream of external pressure on the shared model."""

    name: str
    actions: tuple[BeamAction, ...]

    def activate(self, context: object, actor: str) -> None:
        context.emit("beam", "beam_activated", source=self.name, actor=actor, payload={"actions": len(self.actions), "use_case_id": self.name})
        for action in self.actions:
            def run(current_context: object, selected: BeamAction = action) -> None:
                current_context.emit("actor_intention", selected.name, source=self.name, actor=actor, payload={"use_case_id": selected.use_case_id})
                selected.execute(current_context, actor)

            context.scheduler.schedule_at(action.when, run, name=action.name, source=self.name, correlation_id=actor)
