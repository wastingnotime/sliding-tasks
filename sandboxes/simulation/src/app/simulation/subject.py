from __future__ import annotations

from datetime import datetime

from app.application.simulation import SlidingTasksSimulation
from app.application.use_cases import ApplicationObservation, SlidingTasksUseCases


class SimulationSubject:
    """Repository-owned shared environment used by every actor and beam."""

    def __init__(self, initial_time: datetime) -> None:
        self.environment = SlidingTasksSimulation(initial_time)
        self.context: object | None = None
        self.use_cases = SlidingTasksUseCases.build(self.environment, self._observe_application)
        self.environment.listen(self._observe_domain)

    def bind(self, context: object) -> None:
        self.context = context
        self.environment.clock.set(context.clock.now())

    def _observe_application(self, observation: ApplicationObservation) -> None:
        if self.context is None:
            return
        self.context.emit(observation.type, observation.name, source=observation.source, actor=observation.actor, payload=observation.payload)

    def _observe_domain(self, event: object) -> None:
        if self.context is None:
            return
        self.context.emit("domain_event", event.event_type, source="SlidingTasksSimulation", correlation_id=event.card_id or event.task_id, payload={"event_id": event.event_id, "task_id": event.task_id, "card_id": event.card_id, **event.payload})
