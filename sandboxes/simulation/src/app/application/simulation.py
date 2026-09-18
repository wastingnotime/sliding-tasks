from __future__ import annotations

from collections import Counter
from dataclasses import dataclass
from datetime import date, datetime, time
from typing import Callable

from app.domain.model import (
    Card,
    CardStatus,
    DomainError,
    DomainEvent,
    Recurrence,
    Task,
    TaskSubtype,
    TaskType,
)
from app.infrastructure.memory import DeterministicClock, InMemoryEventStore, SequentialIds


@dataclass(frozen=True)
class Analytics:
    generated: int
    touched: int
    done: int
    dismissed: int
    missed: int
    completion_rate: float


EventListener = Callable[[DomainEvent], None]


class SlidingTasksSimulation:
    """One deterministic, event-recording Sliding Tasks environment."""

    def __init__(self, initial_time: datetime, *, one_time_dismissal_resolves: bool = True) -> None:
        self.clock = DeterministicClock(initial_time)
        self.ids = SequentialIds()
        self.store = InMemoryEventStore()
        self.tasks: dict[str, Task] = {}
        self.cards: dict[str, Card] = {}
        self.card_order: list[str] = []
        self.today: date | None = None
        self.one_time_dismissal_resolves = one_time_dismissal_resolves
        self._listeners: list[EventListener] = []

    def listen(self, listener: EventListener) -> None:
        self._listeners.append(listener)

    def create_task(
        self,
        title: str,
        task_type: TaskType,
        subtype: TaskSubtype,
        recurrence: Recurrence | None = None,
        start_time: str | None = None,
    ) -> Task:
        if subtype == TaskSubtype.REGULAR and recurrence is None:
            raise DomainError("regular tasks require recurrence")
        if subtype == TaskSubtype.ONE_TIME and recurrence is not None:
            raise DomainError("one-time tasks cannot have recurrence")
        task = Task(
            id=self.ids.next("task"),
            title=title,
            task_type=task_type,
            subtype=subtype,
            recurrence=recurrence,
            start_time=start_time,
            active=True,
            created_order=len(self.tasks),
        )
        self.tasks[task.id] = task
        self._record("TaskCreated", task.id, payload=self._task_payload(task))
        if self.today is not None and self._eligible(task, self.today):
            self._generate_card(task, self.today)
        return task

    def update_task(self, task_id: str, **changes: object) -> Task:
        task = self._task(task_id)
        allowed = {"title", "task_type", "recurrence", "start_time", "active"}
        unknown = set(changes) - allowed
        if unknown:
            raise DomainError(f"unsupported task fields: {sorted(unknown)}")
        was_active = task.active
        for name, value in changes.items():
            setattr(task, name, value)
        if task.subtype == TaskSubtype.REGULAR and task.recurrence is None:
            raise DomainError("regular tasks require recurrence")
        self._record("TaskUpdated", task.id, payload=self._task_payload(task))
        if was_active != task.active:
            self._record("TaskActivated" if task.active else "TaskDeactivated", task.id, payload={"active": task.active})
        return task

    def deactivate_task(self, task_id: str) -> None:
        self.update_task(task_id, active=False)

    def open_day(self, day: date) -> tuple[Card, ...]:
        if self.today is not None:
            raise DomainError("close the active day before opening another")
        self.clock.set(datetime.combine(day, time(6, 0)))
        self.today = day
        self.card_order = []
        for task in sorted(self.tasks.values(), key=lambda item: item.created_order):
            if self._eligible(task, day):
                self._generate_card(task, day)
        return self.get_today_cards()

    def close_day(self) -> None:
        if self.today is None:
            raise DomainError("no active day")
        self.clock.set(datetime.combine(self.today, time(23, 59)))
        for card in self.get_today_cards(include_resolved=True):
            if card.status == CardStatus.PENDING:
                self._resolve(card, CardStatus.MISSED, "CardMissed")
        self.today = None
        self.card_order = []

    def get_today_cards(self, *, include_resolved: bool = False) -> tuple[Card, ...]:
        cards = tuple(self.cards[card_id] for card_id in self.card_order)
        if include_resolved:
            return cards
        return tuple(card for card in cards if card.status == CardStatus.PENDING)

    def touch_card(self, card_id: str) -> None:
        card = self._pending_today_card(card_id)
        self._record("CardTouched", card.task_id, card.id)

    def done_card(self, card_id: str) -> None:
        card = self._pending_today_card(card_id)
        self._resolve(card, CardStatus.DONE, "CardDone")
        task = self._task(card.task_id)
        if task.subtype == TaskSubtype.ONE_TIME:
            task.resolved = True

    def dismiss_card(self, card_id: str) -> None:
        card = self._pending_today_card(card_id)
        self._resolve(card, CardStatus.DISMISSED, "CardDismissed")
        task = self._task(card.task_id)
        if task.subtype == TaskSubtype.ONE_TIME and self.one_time_dismissal_resolves:
            task.resolved = True

    def reorder_today_cards(self, card_id: str, position: int) -> None:
        """Experiment: make guidance order explicit without changing outcomes."""
        self._pending_today_card(card_id)
        if position < 0 or position >= len(self.card_order):
            raise DomainError("card position is outside today's board")
        self.card_order.remove(card_id)
        self.card_order.insert(position, card_id)
        self._record("CardReordered", self.cards[card_id].task_id, card_id, {"position": position})

    def jump_to_card(self, card_id: str) -> None:
        """Experiment: observe navigation without treating it as a decision."""
        card = self._pending_today_card(card_id)
        self._record("CardJumped", card.task_id, card.id)

    def events(self) -> tuple[DomainEvent, ...]:
        return self.store.all()

    def analytics(self, *, task_id: str | None = None) -> Analytics:
        relevant = [
            event
            for event in self.events()
            if task_id is None or event.task_id == task_id
        ]
        counts = Counter(event.event_type for event in relevant)
        generated = counts["CardGenerated"]
        return Analytics(
            generated=generated,
            touched=counts["CardTouched"],
            done=counts["CardDone"],
            dismissed=counts["CardDismissed"],
            missed=counts["CardMissed"],
            completion_rate=(counts["CardDone"] / generated if generated else 0.0),
        )

    def metrics_by_task(self) -> dict[str, dict[str, object]]:
        result: dict[str, dict[str, object]] = {}
        for task in self.tasks.values():
            snapshot = next(
                (event.payload for event in self.events() if event.task_id == task.id and event.event_type == "CardGenerated"),
                {"title_snapshot": task.title, "task_type_snapshot": task.task_type.value},
            )
            metric = self._metric_record(
                task.id,
                str(snapshot["title_snapshot"]),
                str(snapshot["task_type_snapshot"]),
            )
            if metric["generated"]:
                result[task.id] = metric
        return result

    def metrics_by_type(self) -> dict[str, dict[str, object]]:
        grouped: dict[str, dict[str, object]] = {}
        card_types: dict[str, str] = {}
        for event in self.events():
            if event.event_type == "CardGenerated" and event.card_id:
                task_type = str(event.payload.get("task_type_snapshot", self.tasks[event.task_id].task_type.value))
                card_types[event.card_id] = task_type
                grouped.setdefault(task_type, {"generated": 0, "touched": 0, "done": 0, "dismissed": 0, "missed": 0})["generated"] += 1
            elif event.card_id and event.card_id in card_types:
                event_type = {"CardTouched": "touched", "CardDone": "done", "CardDismissed": "dismissed", "CardMissed": "missed"}.get(event.event_type)
                if event_type:
                    grouped[card_types[event.card_id]][event_type] += 1
        for bucket in grouped.values():
            bucket["completion_rate"] = bucket["done"] / bucket["generated"] if bucket["generated"] else 0.0
        return grouped

    def touches_before_resolution(self, *, task_id: str | None = None) -> dict[str, int]:
        touches: dict[str, int] = {}
        resolved: set[str] = set()
        for event in self.events():
            if event.card_id is None or (task_id is not None and event.task_id != task_id):
                continue
            if event.event_type == "CardTouched" and event.card_id not in resolved:
                touches[event.card_id] = touches.get(event.card_id, 0) + 1
            elif event.event_type in {"CardDone", "CardDismissed", "CardMissed"}:
                resolved.add(event.card_id)
        return touches

    def sequence_activity(self) -> dict[str, dict[str, object]]:
        activity: dict[str, dict[str, object]] = {}
        outcomes = {"CardDone": "done", "CardDismissed": "dismissed", "CardMissed": "missed"}
        for event in self.events():
            if event.card_id is None:
                continue
            record = activity.setdefault(event.card_id, {"reordered": 0, "jumped": 0, "outcome": None})
            if event.event_type == "CardReordered":
                record["reordered"] = int(record["reordered"]) + 1
            elif event.event_type == "CardJumped":
                record["jumped"] = int(record["jumped"]) + 1
            elif event.event_type in outcomes:
                record["outcome"] = outcomes[event.event_type]
        return activity

    def _metric_record(self, task_id: str, title: str, task_type: str) -> dict[str, object]:
        metric = self.analytics(task_id=task_id)
        return {"title": title, "task_type": task_type, "generated": metric.generated, "touched": metric.touched, "done": metric.done, "dismissed": metric.dismissed, "missed": metric.missed, "completion_rate": metric.completion_rate}

    def _eligible(self, task: Task, day: date) -> bool:
        if not task.active or task.resolved:
            return False
        if task.subtype == TaskSubtype.ONE_TIME:
            return True
        return bool(task.recurrence and task.recurrence.occurs_on(day))

    def _generate_card(self, task: Task, day: date) -> Card:
        existing = next(
            (card for card in self.cards.values() if card.task_id == task.id and card.board_date == day),
            None,
        )
        if existing:
            return existing
        card = Card(
            id=self.ids.next("card"),
            task_id=task.id,
            board_date=day,
            title_snapshot=task.title,
            task_type_snapshot=task.task_type,
            start_time_snapshot=task.start_time,
        )
        self.cards[card.id] = card
        self.card_order.append(card.id)
        self._record(
            "CardGenerated",
            task.id,
            card.id,
            {
                "board_date": day.isoformat(),
                "title_snapshot": card.title_snapshot,
                "task_type_snapshot": card.task_type_snapshot.value,
                "start_time_snapshot": card.start_time_snapshot,
            },
        )
        return card

    def _resolve(self, card: Card, status: CardStatus, event_type: str) -> None:
        card.status = status
        card.decided_at = self.clock.now()
        self._record(event_type, card.task_id, card.id)

    def _pending_today_card(self, card_id: str) -> Card:
        card = self.cards.get(card_id)
        if card is None or self.today is None or card.board_date != self.today:
            raise DomainError("card is not on today's board")
        if card.status != CardStatus.PENDING:
            raise DomainError("card is already resolved")
        return card

    def _task(self, task_id: str) -> Task:
        try:
            return self.tasks[task_id]
        except KeyError as error:
            raise DomainError("unknown task") from error

    def _record(
        self,
        event_type: str,
        task_id: str,
        card_id: str | None = None,
        payload: dict[str, object] | None = None,
    ) -> None:
        event = DomainEvent(
            event_id=self.ids.next("event"),
            event_type=event_type,
            occurred_at=self.clock.now(),
            task_id=task_id,
            card_id=card_id,
            payload=payload or {},
        )
        self.store.append(event)
        for listener in self._listeners:
            listener(event)

    @staticmethod
    def _task_payload(task: Task) -> dict[str, object]:
        return {
            "title": task.title,
            "task_type": task.task_type.value,
            "subtype": task.subtype.value,
            "active": task.active,
        }
