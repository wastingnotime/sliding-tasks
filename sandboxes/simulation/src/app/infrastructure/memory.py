from __future__ import annotations

from datetime import datetime

from app.domain.model import DomainEvent


class DeterministicClock:
    def __init__(self, now: datetime) -> None:
        self._now = now

    def now(self) -> datetime:
        return self._now

    def set(self, value: datetime) -> None:
        self._now = value


class SequentialIds:
    def __init__(self) -> None:
        self._counts: dict[str, int] = {}

    def next(self, prefix: str) -> str:
        value = self._counts.get(prefix, 0) + 1
        self._counts[prefix] = value
        return f"{prefix}-{value:04d}"


class InMemoryEventStore:
    def __init__(self) -> None:
        self._events: list[DomainEvent] = []

    def append(self, event: DomainEvent) -> None:
        self._events.append(event)

    def all(self) -> tuple[DomainEvent, ...]:
        return tuple(self._events)

