from datetime import date, datetime

import pytest

from app.application.simulation import SlidingTasksSimulation
from app.domain.model import (
    CardStatus,
    DomainError,
    Recurrence,
    RecurrenceKind,
    TaskSubtype,
    TaskType,
)


DAY_ONE = date(2026, 9, 14)  # Monday
DAILY = Recurrence(RecurrenceKind.DAILY)


def simulation() -> SlidingTasksSimulation:
    return SlidingTasksSimulation(datetime(2026, 9, 14, 6, 0))


def event_types(env: SlidingTasksSimulation, task_id: str) -> list[str]:
    return [event.event_type for event in env.events() if event.task_id == task_id]


def test_regular_success_and_conscious_dismiss_are_distinct() -> None:
    env = simulation()
    pushups = env.create_task("pushups", TaskType.SKILL, TaskSubtype.REGULAR, DAILY)
    housekeeping = env.create_task("housekeeping", TaskType.CHORE, TaskSubtype.REGULAR, DAILY)
    first, second = env.open_day(DAY_ONE)

    env.done_card(first.id)
    env.dismiss_card(second.id)
    env.close_day()

    assert event_types(env, pushups.id)[-1] == "CardDone"
    assert event_types(env, housekeeping.id)[-1] == "CardDismissed"
    assert "CardMissed" not in event_types(env, housekeeping.id)


def test_untouched_regular_card_is_missed_at_day_close() -> None:
    env = simulation()
    task = env.create_task("play piano", TaskType.SKILL, TaskSubtype.REGULAR, DAILY)
    card = env.open_day(DAY_ONE)[0]

    env.close_day()

    assert card.status == CardStatus.MISSED
    assert event_types(env, task.id)[-1] == "CardMissed"


def test_touch_is_repeatable_and_does_not_resolve() -> None:
    env = simulation()
    task = env.create_task("write code", TaskType.SKILL, TaskSubtype.REGULAR, DAILY)
    card = env.open_day(DAY_ONE)[0]

    env.touch_card(card.id)
    env.touch_card(card.id)
    assert card.status == CardStatus.PENDING
    env.done_card(card.id)

    assert event_types(env, task.id)[-4:] == ["CardGenerated", "CardTouched", "CardTouched", "CardDone"]


def test_repeated_touches_preserve_facts_when_card_is_missed() -> None:
    env = simulation()
    task = env.create_task("play piano", TaskType.SKILL, TaskSubtype.REGULAR, DAILY)
    card = env.open_day(DAY_ONE)[0]
    env.touch_card(card.id)
    env.touch_card(card.id)

    env.close_day()

    assert event_types(env, task.id)[-4:] == ["CardGenerated", "CardTouched", "CardTouched", "CardMissed"]


def test_unresolved_one_time_task_carries_forward_until_explicit_done() -> None:
    env = simulation()
    task = env.create_task("mount wardrobe", TaskType.CHORE, TaskSubtype.ONE_TIME)
    day_one_card = env.open_day(DAY_ONE)[0]
    env.close_day()
    day_two_card = env.open_day(date(2026, 9, 15))[0]
    env.close_day()
    day_three_card = env.open_day(date(2026, 9, 16))[0]
    env.done_card(day_three_card.id)
    env.close_day()
    assert env.open_day(date(2026, 9, 17)) == ()

    assert day_one_card.id != day_two_card.id != day_three_card.id
    assert event_types(env, task.id).count("CardMissed") == 2
    assert event_types(env, task.id).count("CardDone") == 1


def test_regular_rule_change_does_not_rewrite_today_snapshot() -> None:
    env = simulation()
    task = env.create_task("old title", TaskType.TASK, TaskSubtype.REGULAR, DAILY)
    card = env.open_day(DAY_ONE)[0]

    env.update_task(task.id, title="new title", recurrence=Recurrence(RecurrenceKind.WEEKDAYS))

    assert card.title_snapshot == "old title"
    env.close_day()
    assert env.open_day(date(2026, 9, 15))[0].title_snapshot == "new title"


def test_one_time_task_added_during_today_is_immediately_actionable() -> None:
    env = simulation()
    env.open_day(DAY_ONE)

    task = env.create_task("one-time meeting", TaskType.TASK, TaskSubtype.ONE_TIME)

    cards = env.get_today_cards()
    assert len(cards) == 1
    assert cards[0].task_id == task.id


def test_eligible_regular_task_added_during_today_is_immediately_actionable() -> None:
    env = simulation()
    env.open_day(DAY_ONE)

    task = env.create_task("same-day practice", TaskType.SKILL, TaskSubtype.REGULAR, DAILY)

    cards = env.get_today_cards()
    assert [card.title_snapshot for card in cards] == ["same-day practice"]
    assert event_types(env, task.id) == ["TaskCreated", "CardGenerated"]


def test_resolved_card_rejects_later_outcome() -> None:
    env = simulation()
    env.create_task("lunch", TaskType.REQUIRED, TaskSubtype.REGULAR, DAILY)
    card = env.open_day(DAY_ONE)[0]
    env.done_card(card.id)

    with pytest.raises(DomainError, match="already resolved"):
        env.dismiss_card(card.id)


def test_sequence_reorder_and_jump_are_observations_not_decisions() -> None:
    env = simulation()
    first_task = env.create_task("first", TaskType.TASK, TaskSubtype.REGULAR, DAILY)
    second_task = env.create_task("second", TaskType.TASK, TaskSubtype.REGULAR, DAILY)
    third_task = env.create_task("third", TaskType.TASK, TaskSubtype.REGULAR, DAILY)
    first, second, third = env.open_day(DAY_ONE)

    env.reorder_today_cards(third.id, 0)
    env.jump_to_card(third.id)

    assert [card.id for card in env.get_today_cards()] == [third.id, first.id, second.id]
    assert third.status == CardStatus.PENDING
    assert event_types(env, third_task.id)[-2:] == ["CardReordered", "CardJumped"]
    assert event_types(env, first_task.id) == ["TaskCreated", "CardGenerated"]
    assert event_types(env, second_task.id) == ["TaskCreated", "CardGenerated"]


def test_sequence_actions_only_apply_to_pending_cards_on_today() -> None:
    env = simulation()
    env.create_task("one", TaskType.TASK, TaskSubtype.REGULAR, DAILY)
    card = env.open_day(DAY_ONE)[0]
    env.done_card(card.id)

    with pytest.raises(DomainError, match="already resolved"):
        env.jump_to_card(card.id)
    with pytest.raises(DomainError, match="already resolved"):
        env.reorder_today_cards(card.id, 0)


def test_recurrence_rules_are_deterministic() -> None:
    monday = DAY_ONE
    saturday = date(2026, 9, 19)
    assert Recurrence(RecurrenceKind.WEEKDAYS).occurs_on(monday)
    assert not Recurrence(RecurrenceKind.WEEKENDS).occurs_on(monday)
    assert Recurrence(RecurrenceKind.WEEKENDS).occurs_on(saturday)
    assert Recurrence(RecurrenceKind.SPECIFIC_WEEKDAYS, frozenset({0, 2})).occurs_on(monday)
    assert Recurrence(RecurrenceKind.NTH_DAY_OF_MONTH, day_of_month=14).occurs_on(monday)
    assert not Recurrence(RecurrenceKind.NTH_DAY_OF_MONTH, day_of_month=31).occurs_on(date(2026, 2, 28))


def test_basic_analytics_are_derived_from_factual_events() -> None:
    env = simulation()
    env.create_task("pushups", TaskType.SKILL, TaskSubtype.REGULAR, DAILY)
    env.create_task("piano", TaskType.SKILL, TaskSubtype.REGULAR, DAILY)
    first, second = env.open_day(DAY_ONE)
    env.touch_card(first.id)
    env.done_card(first.id)
    env.close_day()

    metrics = env.analytics()
    assert metrics.generated == 2
    assert metrics.touched == 1
    assert metrics.done == 1
    assert metrics.missed == 1
    assert metrics.completion_rate == 0.5


def test_longer_history_projects_metrics_by_task_and_type() -> None:
    env = simulation()
    pushups = env.create_task("pushups", TaskType.SKILL, TaskSubtype.REGULAR, DAILY)
    chores = env.create_task("dishes", TaskType.CHORE, TaskSubtype.REGULAR, DAILY)
    first = env.open_day(DAY_ONE)
    env.touch_card(first[0].id)
    env.done_card(first[0].id)
    env.close_day()
    env.open_day(date(2026, 9, 15))
    env.dismiss_card(env.get_today_cards()[1].id)
    env.close_day()

    by_task = env.metrics_by_task()
    assert by_task[pushups.id]["generated"] == 2
    assert by_task[pushups.id]["done"] == 1
    assert by_task[pushups.id]["missed"] == 1
    assert by_task[chores.id]["dismissed"] == 1
    assert env.metrics_by_type()["skill"]["done"] == 1
    assert env.metrics_by_type()["chore"]["dismissed"] == 1


def test_touches_before_resolution_preserves_per_card_signal() -> None:
    env = simulation()
    task = env.create_task("piano", TaskType.SKILL, TaskSubtype.REGULAR, DAILY)
    card = env.open_day(DAY_ONE)[0]
    env.touch_card(card.id)
    env.touch_card(card.id)
    env.done_card(card.id)
    assert env.touches_before_resolution(task_id=task.id) == {card.id: 2}


def test_metric_labels_keep_first_occurrence_snapshot_after_task_edit() -> None:
    env = simulation()
    task = env.create_task("old title", TaskType.CHORE, TaskSubtype.REGULAR, DAILY)
    card = env.open_day(DAY_ONE)[0]
    env.done_card(card.id)
    env.close_day()

    env.update_task(task.id, title="new title", task_type=TaskType.SKILL)
    next_card = env.open_day(date(2026, 9, 15))[0]
    env.close_day()

    metric = env.metrics_by_task()[task.id]
    assert metric["title"] == "old title"
    assert metric["task_type"] == "chore"
    assert next_card.title_snapshot == "new title"


def test_type_metrics_follow_each_occurrence_snapshot_across_task_edit() -> None:
    env = simulation()
    task = env.create_task("practice", TaskType.CHORE, TaskSubtype.REGULAR, DAILY)
    first = env.open_day(DAY_ONE)[0]
    env.done_card(first.id)
    env.close_day()

    env.update_task(task.id, task_type=TaskType.SKILL)
    second = env.open_day(date(2026, 9, 15))[0]
    env.dismiss_card(second.id)
    env.close_day()

    assert env.metrics_by_type()["chore"]["done"] == 1
    assert env.metrics_by_type()["chore"]["generated"] == 1
    assert env.metrics_by_type()["skill"]["dismissed"] == 1
    assert env.metrics_by_type()["skill"]["generated"] == 1


def test_one_time_dismissal_default_resolves_intention() -> None:
    env = simulation()
    env.create_task("mount wardrobe", TaskType.CHORE, TaskSubtype.ONE_TIME)
    card = env.open_day(DAY_ONE)[0]
    env.dismiss_card(card.id)
    env.close_day()
    assert env.open_day(date(2026, 9, 15)) == ()


def test_one_time_dismissal_experiment_can_carry_intention_forward() -> None:
    env = SlidingTasksSimulation(datetime(2026, 9, 14, 6, 0), one_time_dismissal_resolves=False)
    env.create_task("mount wardrobe", TaskType.CHORE, TaskSubtype.ONE_TIME)
    card = env.open_day(DAY_ONE)[0]
    env.dismiss_card(card.id)
    env.close_day()
    next_day = env.open_day(date(2026, 9, 15))
    assert len(next_day) == 1
    assert next_day[0].title_snapshot == "mount wardrobe"


def test_dismissal_policy_switch_does_not_change_one_time_done() -> None:
    env = SlidingTasksSimulation(datetime(2026, 9, 14, 6, 0), one_time_dismissal_resolves=False)
    task = env.create_task("mount wardrobe", TaskType.CHORE, TaskSubtype.ONE_TIME)
    card = env.open_day(DAY_ONE)[0]
    env.done_card(card.id)
    env.close_day()
    assert env.open_day(date(2026, 9, 15)) == ()
    assert event_types(env, task.id)[-1] == "CardDone"


def test_recurrence_rules_drive_end_to_end_board_generation() -> None:
    env = simulation()
    weekday = env.create_task("weekday", TaskType.TASK, TaskSubtype.REGULAR, Recurrence(RecurrenceKind.WEEKDAYS))
    weekend = env.create_task("weekend", TaskType.TASK, TaskSubtype.REGULAR, Recurrence(RecurrenceKind.WEEKENDS))
    wednesday = env.create_task("wednesday", TaskType.TASK, TaskSubtype.REGULAR, Recurrence(RecurrenceKind.SPECIFIC_WEEKDAYS, frozenset({2})))
    month_end = env.create_task("month day", TaskType.TASK, TaskSubtype.REGULAR, Recurrence(RecurrenceKind.NTH_DAY_OF_MONTH, day_of_month=30))
    assert {card.task_id for card in env.open_day(date(2026, 9, 14))} == {weekday.id}
    env.close_day()
    assert {card.task_id for card in env.open_day(date(2026, 9, 19))} == {weekend.id}
    env.close_day()
    assert {card.task_id for card in env.open_day(date(2026, 9, 16))} == {weekday.id, wednesday.id}
    env.close_day()
    assert {card.task_id for card in env.open_day(date(2026, 9, 30))} == {weekday.id, wednesday.id, month_end.id}


def test_task_deactivation_preserves_today_and_controls_future_generation() -> None:
    env = simulation()
    task = env.create_task("wellbeing", TaskType.TASK, TaskSubtype.REGULAR, DAILY)
    today_card = env.open_day(DAY_ONE)[0]
    env.deactivate_task(task.id)
    assert env.get_today_cards()[0].id == today_card.id
    assert event_types(env, task.id).count("CardGenerated") == 1
    assert event_types(env, task.id)[-2:] == ["TaskUpdated", "TaskDeactivated"]
    env.close_day()
    assert env.open_day(date(2026, 9, 15)) == ()
    env.close_day()
    env.update_task(task.id, active=True)
    next_card = env.open_day(date(2026, 9, 16))[0]
    assert next_card.id != today_card.id
    assert next_card.title_snapshot == "wellbeing"


def test_recurrence_rules_reject_invalid_parameters() -> None:
    with pytest.raises(DomainError):
        Recurrence(RecurrenceKind.SPECIFIC_WEEKDAYS)
    with pytest.raises(DomainError):
        Recurrence(RecurrenceKind.SPECIFIC_WEEKDAYS, frozenset({7}))
    with pytest.raises(DomainError):
        Recurrence(RecurrenceKind.NTH_DAY_OF_MONTH, day_of_month=0)
    with pytest.raises(DomainError):
        Recurrence(RecurrenceKind.DAILY, weekdays=frozenset({0}))


def test_sequence_activity_pairs_raw_navigation_counts_with_outcome() -> None:
    env = simulation()
    task = env.create_task("third", TaskType.TASK, TaskSubtype.REGULAR, DAILY)
    card = env.open_day(DAY_ONE)[0]
    env.reorder_today_cards(card.id, 0)
    env.jump_to_card(card.id)
    env.done_card(card.id)

    assert env.sequence_activity() == {card.id: {"reordered": 1, "jumped": 1, "outcome": "done"}}
    assert task.id == card.task_id


def test_sequence_activity_keeps_untouched_card_as_zero_activity_baseline() -> None:
    env = simulation()
    env.create_task("acted", TaskType.TASK, TaskSubtype.REGULAR, DAILY)
    env.create_task("untouched", TaskType.TASK, TaskSubtype.REGULAR, DAILY)
    acted, untouched = env.open_day(DAY_ONE)
    env.reorder_today_cards(acted.id, 1)
    env.done_card(acted.id)
    env.close_day()

    assert env.sequence_activity()[untouched.id] == {"reordered": 0, "jumped": 0, "outcome": "missed"}
