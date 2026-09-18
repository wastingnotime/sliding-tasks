from __future__ import annotations

from datetime import date, datetime, timezone
from typing import Callable

from app.domain.model import Recurrence, RecurrenceKind, TaskSubtype, TaskType
from app.simulation.actors import BeamActorBehavior
from app.simulation.beams import BeamAction, BehaviorBeam
from app.simulation.subject import SimulationSubject
from mrl_simulation_runtime.actors import Actor
from mrl_simulation_runtime.invariants import Invariant
from mrl_simulation_runtime.scenario import ObservatoryEdge, ObservatoryNode, Scenario

INITIAL_TIME = datetime(2026, 9, 14, 6, 0, tzinfo=timezone.utc)


def _at(day: int, hour: int, minute: int = 0) -> datetime:
    return datetime(2026, 9, day, hour, minute, tzinfo=timezone.utc)


def _card_id(subject: SimulationSubject, title: str) -> str:
    return next(card.id for card in subject.environment.get_today_cards() if card.title_snapshot == title)


def _setup_day_one(subject: SimulationSubject, context: object, actor: str) -> None:
    subject.bind(context)
    daily = Recurrence(RecurrenceKind.DAILY)
    subject.use_cases.create_task.execute(actor, title="pushups", task_type=TaskType.SKILL, subtype=TaskSubtype.REGULAR, recurrence=daily)
    subject.use_cases.create_task.execute(actor, title="housekeeping", task_type=TaskType.CHORE, subtype=TaskSubtype.REGULAR, recurrence=daily)
    subject.use_cases.create_task.execute(actor, title="play piano", task_type=TaskType.SKILL, subtype=TaskSubtype.REGULAR, recurrence=daily)
    subject.use_cases.create_task.execute(actor, title="mount wardrobe", task_type=TaskType.CHORE, subtype=TaskSubtype.ONE_TIME)
    subject.use_cases.open_day.execute(actor, date(2026, 9, 14))


def _act_day_one(subject: SimulationSubject, context: object, actor: str) -> None:
    subject.bind(context)
    subject.use_cases.complete_card.execute(actor, _card_id(subject, "pushups"))
    subject.use_cases.dismiss_card.execute(actor, _card_id(subject, "housekeeping"))
    piano = _card_id(subject, "play piano")
    subject.use_cases.touch_card.execute(actor, piano)
    subject.use_cases.touch_card.execute(actor, piano)


def _close_day(subject: SimulationSubject, context: object, actor: str) -> None:
    subject.bind(context)
    subject.use_cases.close_day.execute(actor)


def _open_day_two(subject: SimulationSubject, context: object, actor: str) -> None:
    subject.bind(context)
    subject.use_cases.open_day.execute(actor, date(2026, 9, 15))


def _navigate_day_two(subject: SimulationSubject, context: object, actor: str) -> None:
    subject.bind(context)
    one_time = _card_id(subject, "mount wardrobe")
    subject.use_cases.reorder_card.execute(actor, one_time, 0)
    subject.use_cases.jump_to_card.execute(actor, one_time)


def _complete_one_time(subject: SimulationSubject, context: object, actor: str) -> None:
    subject.bind(context)
    subject.use_cases.complete_card.execute(actor, _card_id(subject, "mount wardrobe"))


def _observe_analytics(subject: SimulationSubject, context: object, actor: str) -> None:
    subject.bind(context)
    metrics = subject.use_cases.get_analytics.execute(actor)
    env = subject.environment
    context.emit("projection", "basic_analytics", source="AnalyticsProjection", actor=actor, payload={**metrics.__dict__, "by_task": env.metrics_by_task(), "by_type": env.metrics_by_type(), "touches_before_resolution": env.touches_before_resolution(), "sequence_activity": env.sequence_activity()})


def _action(subject: SimulationSubject, callback: Callable[[SimulationSubject, object, str], None]):
    return lambda context, actor: callback(subject, context, actor)


def _outcomes_are_consistent(subject: SimulationSubject) -> bool:
    terminal = {"CardDone", "CardDismissed", "CardMissed"}
    outcomes: dict[str, set[str]] = {}
    for event in subject.environment.events():
        if event.card_id and event.event_type in terminal:
            outcomes.setdefault(event.card_id, set()).add(event.event_type)
    return all(len(values) <= 1 for values in outcomes.values())


def create_simulation() -> Scenario:
    subject = SimulationSubject(INITIAL_TIME)
    planning = BehaviorBeam("planning-beam", (BeamAction(_at(14, 6), "configure intentions and open today", _action(subject, _setup_day_one)),))
    daily_life = BehaviorBeam("daily-life-beam", (
        BeamAction(_at(14, 8), "act on morning cards", _action(subject, _act_day_one)),
        BeamAction(_at(15, 7), "navigate today's sequence", _action(subject, _navigate_day_two)),
        BeamAction(_at(15, 18), "complete carried intention", _action(subject, _complete_one_time)),
    ))
    boundary = BehaviorBeam("day-boundary-beam", (
        BeamAction(_at(14, 23, 59), "close first day", _action(subject, _close_day)),
        BeamAction(_at(15, 6), "open second day", _action(subject, _open_day_two)),
        BeamAction(_at(15, 23, 59), "close second day", _action(subject, _close_day)),
    ))
    analytics = BehaviorBeam("analytics-beam", (BeamAction(_at(15, 23, 59), "inspect behavioral history", _action(subject, _observe_analytics)),))
    actors = [
        Actor("Planner", BeamActorBehavior("Planner", planning)),
        Actor("User", BeamActorBehavior("User", daily_life)),
        Actor("DayBoundary", BeamActorBehavior("DayBoundary", boundary)),
        Actor("Analyst", BeamActorBehavior("Analyst", analytics)),
    ]
    return Scenario(
        name="sliding-tasks-shared-environment",
        seed=20260918,
        initial_time=INITIAL_TIME,
        run_id="shared-environment-actors-beams-001",
        actors=actors,
        invariants=[Invariant("cards have consistent terminal outcomes", lambda _: _outcomes_are_consistent(subject))],
        observatory_nodes=[
            *(ObservatoryNode(actor.name, actor.name, "actor", "actors") for actor in actors),
            ObservatoryNode("use-cases", "Application use cases", "use_case", "use_cases"),
            ObservatoryNode("domain", "Task/Card domain", "domain", "model"),
            ObservatoryNode("events", "Event history", "event_store", "infrastructure"),
            ObservatoryNode("analytics", "Analytics", "projection", "model"),
        ],
        observatory_edges=[
            *(ObservatoryEdge(actor.name, "use-cases", "intends") for actor in actors),
            ObservatoryEdge("use-cases", "domain", "commands"),
            ObservatoryEdge("domain", "events", "emits"),
            ObservatoryEdge("events", "analytics", "projects"),
        ],
    )
