from __future__ import annotations

from dataclasses import dataclass

from app.simulation.beams import BehaviorBeam


@dataclass(frozen=True)
class BeamActorBehavior:
    actor_name: str
    beam: BehaviorBeam

    def on_start(self, context: object) -> None:
        self.beam.activate(context, self.actor_name)

