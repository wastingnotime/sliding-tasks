from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date, datetime
from enum import Enum
from typing import Any


class TaskType(str, Enum):
    CHORE = "chore"
    SKILL = "skill"
    REQUIRED = "required"
    TASK = "task"


class TaskSubtype(str, Enum):
    REGULAR = "regular"
    ONE_TIME = "one_time"


class CardStatus(str, Enum):
    PENDING = "pending"
    DONE = "done"
    DISMISSED = "dismissed"
    MISSED = "missed"


class RecurrenceKind(str, Enum):
    DAILY = "daily"
    WEEKDAYS = "weekdays"
    WEEKENDS = "weekends"
    SPECIFIC_WEEKDAYS = "specific_weekdays"
    NTH_DAY_OF_MONTH = "nth_day_of_month"


class OneTimeMissedPolicy(str, Enum):
    CARRY_FORWARD = "carry_forward"
    EXPIRE = "expire"


@dataclass(frozen=True)
class Recurrence:
    kind: RecurrenceKind
    weekdays: frozenset[int] = frozenset()
    day_of_month: int | None = None

    def __post_init__(self) -> None:
        if self.kind == RecurrenceKind.SPECIFIC_WEEKDAYS:
            if not self.weekdays or not self.weekdays.issubset(range(7)):
                raise DomainError("specific weekday recurrence requires weekdays 0..6")
        elif self.weekdays:
            raise DomainError("weekdays are only valid for specific weekday recurrence")
        if self.kind == RecurrenceKind.NTH_DAY_OF_MONTH:
            if self.day_of_month is None or not 1 <= self.day_of_month <= 31:
                raise DomainError("nth day recurrence requires day_of_month 1..31")
        elif self.day_of_month is not None:
            raise DomainError("day_of_month is only valid for nth day recurrence")

    def occurs_on(self, day: date) -> bool:
        if self.kind == RecurrenceKind.DAILY:
            return True
        if self.kind == RecurrenceKind.WEEKDAYS:
            return day.weekday() < 5
        if self.kind == RecurrenceKind.WEEKENDS:
            return day.weekday() >= 5
        if self.kind == RecurrenceKind.SPECIFIC_WEEKDAYS:
            return day.weekday() in self.weekdays
        if self.kind == RecurrenceKind.NTH_DAY_OF_MONTH:
            return day.day == self.day_of_month
        return False


@dataclass
class Task:
    id: str
    title: str
    task_type: TaskType
    subtype: TaskSubtype
    recurrence: Recurrence | None
    start_time: str | None
    active: bool
    created_order: int
    resolved: bool = False


@dataclass
class Card:
    id: str
    task_id: str
    board_date: date
    title_snapshot: str
    task_type_snapshot: TaskType
    start_time_snapshot: str | None
    status: CardStatus = CardStatus.PENDING
    decided_at: datetime | None = None


@dataclass(frozen=True)
class DomainEvent:
    event_id: str
    event_type: str
    occurred_at: datetime
    task_id: str
    card_id: str | None = None
    payload: dict[str, Any] = field(default_factory=dict)
    context: dict[str, Any] | None = None


class DomainError(ValueError):
    pass
