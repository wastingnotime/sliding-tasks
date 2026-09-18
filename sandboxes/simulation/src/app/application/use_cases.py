from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from typing import Callable

from app.application.simulation import Analytics, SlidingTasksSimulation
from app.domain.model import Card, Recurrence, Task, TaskSubtype, TaskType


@dataclass(frozen=True)
class ApplicationObservation:
    type: str
    name: str
    actor: str | None
    source: str
    payload: dict[str, object]


Observer = Callable[[ApplicationObservation], None]


class UseCase:
    name = "use_case"

    def __init__(self, environment: SlidingTasksSimulation, observe: Observer) -> None:
        self.environment = environment
        self.observe = observe

    def _run(self, actor: str, payload: dict[str, object], operation: Callable[[], object]) -> object:
        self.observe(ApplicationObservation("command", self.name, actor, actor, payload))
        self.observe(ApplicationObservation("use_case_decision", "SlidingTasksSimulation", None, self.name, {"beam_role": "use_case_to_aggregate", **payload}))
        return operation()


class CreateTask(UseCase):
    name = "create_task"

    def execute(self, actor: str, *, title: str, task_type: TaskType, subtype: TaskSubtype, recurrence: Recurrence | None = None) -> Task:
        return self._run(actor, {"title": title, "task_type": task_type.value, "subtype": subtype.value}, lambda: self.environment.create_task(title, task_type, subtype, recurrence))  # type: ignore[return-value]


class UpdateTask(UseCase):
    name = "update_task"

    def execute(self, actor: str, task_id: str, **changes: object) -> Task:
        return self._run(actor, {"task_id": task_id, **changes}, lambda: self.environment.update_task(task_id, **changes))  # type: ignore[return-value]


class OpenDay(UseCase):
    name = "open_day"

    def execute(self, actor: str, day: date) -> tuple[Card, ...]:
        return self._run(actor, {"day": day.isoformat()}, lambda: self.environment.open_day(day))  # type: ignore[return-value]


class CloseDay(UseCase):
    name = "close_day"

    def execute(self, actor: str) -> None:
        self._run(actor, {}, self.environment.close_day)


class TouchCard(UseCase):
    name = "touch_card"

    def execute(self, actor: str, card_id: str) -> None:
        self._run(actor, {"card_id": card_id}, lambda: self.environment.touch_card(card_id))


class CompleteCard(UseCase):
    name = "complete_card"

    def execute(self, actor: str, card_id: str) -> None:
        self._run(actor, {"card_id": card_id}, lambda: self.environment.done_card(card_id))


class DismissCard(UseCase):
    name = "dismiss_card"

    def execute(self, actor: str, card_id: str) -> None:
        self._run(actor, {"card_id": card_id}, lambda: self.environment.dismiss_card(card_id))


class ReorderCard(UseCase):
    name = "reorder_card"

    def execute(self, actor: str, card_id: str, position: int) -> None:
        self._run(actor, {"card_id": card_id, "position": position}, lambda: self.environment.reorder_today_cards(card_id, position))


class JumpToCard(UseCase):
    name = "jump_to_card"

    def execute(self, actor: str, card_id: str) -> None:
        self._run(actor, {"card_id": card_id}, lambda: self.environment.jump_to_card(card_id))


class GetAnalytics(UseCase):
    name = "get_analytics"

    def execute(self, actor: str) -> Analytics:
        return self._run(actor, {}, self.environment.analytics)  # type: ignore[return-value]


@dataclass(frozen=True)
class SlidingTasksUseCases:
    create_task: CreateTask
    update_task: UpdateTask
    open_day: OpenDay
    close_day: CloseDay
    touch_card: TouchCard
    complete_card: CompleteCard
    dismiss_card: DismissCard
    reorder_card: ReorderCard
    jump_to_card: JumpToCard
    get_analytics: GetAnalytics

    @classmethod
    def build(cls, environment: SlidingTasksSimulation, observe: Observer) -> SlidingTasksUseCases:
        return cls(*(use_case(environment, observe) for use_case in (CreateTask, UpdateTask, OpenDay, CloseDay, TouchCard, CompleteCard, DismissCard, ReorderCard, JumpToCard, GetAnalytics)))
