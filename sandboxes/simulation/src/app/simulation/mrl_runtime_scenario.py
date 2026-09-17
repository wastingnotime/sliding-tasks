from __future__ import annotations

from datetime import date, datetime, timezone

from app.application.simulation import SlidingTasksSimulation
from app.domain.model import Recurrence, RecurrenceKind, TaskSubtype, TaskType
from mrl_simulation_runtime.context import SimulationContext
from mrl_simulation_runtime.invariants import Invariant
from mrl_simulation_runtime.scenario import (
    InitialScheduledAction,
    ObservatoryEdge,
    ObservatoryNode,
    Scenario,
)


INITIAL_TIME = datetime(2026, 9, 14, 6, 0, tzinfo=timezone.utc)
_environment: SlidingTasksSimulation | None = None


def _environment_for(context: SimulationContext) -> SlidingTasksSimulation:
    global _environment
    if _environment is None:
        _environment = SlidingTasksSimulation(context.clock.now())

        def observe(event: object) -> None:
            context.emit(
                "domain_event",
                event.event_type,
                source="SlidingTasksSimulation",
                actor="user" if event.event_type in {"CardTouched", "CardDone", "CardDismissed"} else None,
                correlation_id=event.card_id or event.task_id,
                payload={
                    "event_id": event.event_id,
                    "task_id": event.task_id,
                    "card_id": event.card_id,
                    **event.payload,
                },
            )

        _environment.listen(observe)
    _environment.clock.set(context.clock.now())
    return _environment


def _open_day_one(context: SimulationContext) -> None:
    env = _environment_for(context)
    daily = Recurrence(RecurrenceKind.DAILY)
    env.create_task("pushups", TaskType.SKILL, TaskSubtype.REGULAR, daily)
    env.create_task("housekeeping", TaskType.CHORE, TaskSubtype.REGULAR, daily)
    env.create_task("play piano", TaskType.SKILL, TaskSubtype.REGULAR, daily)
    env.create_task("mount wardrobe", TaskType.CHORE, TaskSubtype.ONE_TIME)
    cards = env.open_day(date(2026, 9, 14))
    env.done_card(cards[0].id)
    env.dismiss_card(cards[1].id)
    env.touch_card(cards[2].id)
    env.touch_card(cards[2].id)
    context.emit("use_case", "day_opened_and_acted", source="Scenario", payload={"cards": len(cards)})


def _close_day(context: SimulationContext) -> None:
    env = _environment_for(context)
    env.close_day()
    context.emit("use_case", "day_closed", source="Scenario")


def _open_day_two(context: SimulationContext) -> None:
    env = _environment_for(context)
    cards = env.open_day(date(2026, 9, 15))
    context.emit("use_case", "one_time_carried_forward", source="Scenario", payload={"cards": len(cards)})


def _finish_day_two(context: SimulationContext) -> None:
    env = _environment_for(context)
    one_time = next(card for card in env.get_today_cards() if card.title_snapshot == "mount wardrobe")
    env.done_card(one_time.id)
    env.close_day()
    metrics = env.analytics()
    context.emit(
        "projection",
        "basic_analytics",
        source="AnalyticsProjection",
        payload=metrics.__dict__,
    )


def _all_cards_resolved(_: object) -> bool:
    return _environment is None or all(card.status.value != "pending" for card in _environment.cards.values())


def create_simulation() -> Scenario:
    global _environment
    _environment = None
    return Scenario(
        name="sliding-tasks-today-decision-loop",
        seed=20260917,
        initial_time=INITIAL_TIME,
        run_id="today-decision-loop-001",
        scheduled_actions=[
            InitialScheduledAction(INITIAL_TIME, _open_day_one, "open-day-one", "Scenario"),
            InitialScheduledAction(datetime(2026, 9, 14, 23, 59, tzinfo=timezone.utc), _close_day, "close-day-one", "Scenario"),
            InitialScheduledAction(datetime(2026, 9, 15, 6, 0, tzinfo=timezone.utc), _open_day_two, "open-day-two", "Scenario"),
            InitialScheduledAction(datetime(2026, 9, 15, 23, 59, tzinfo=timezone.utc), _finish_day_two, "finish-day-two", "Scenario"),
        ],
        invariants=[Invariant("no pending cards after final closure", _all_cards_resolved)],
        observatory_nodes=[
            ObservatoryNode("rules", "Task rules", "domain", "model"),
            ObservatoryNode("board", "Today's board", "projection", "model"),
            ObservatoryNode("events", "Event history", "event_store", "infrastructure"),
            ObservatoryNode("analytics", "Basic analytics", "projection", "model"),
        ],
        observatory_edges=[
            ObservatoryEdge("rules", "board", "generate"),
            ObservatoryEdge("board", "events", "observe decisions"),
            ObservatoryEdge("events", "analytics", "project"),
        ],
    )

