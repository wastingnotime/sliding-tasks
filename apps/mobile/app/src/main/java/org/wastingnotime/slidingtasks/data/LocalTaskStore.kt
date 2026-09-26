package org.wastingnotime.slidingtasks.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.wastingnotime.slidingtasks.model.CardStatus
import org.wastingnotime.slidingtasks.model.PlannedTask
import org.wastingnotime.slidingtasks.model.ScheduleKind
import org.wastingnotime.slidingtasks.model.SkipScope
import org.wastingnotime.slidingtasks.model.SlidingTasksState
import org.wastingnotime.slidingtasks.model.TaskCard
import org.wastingnotime.slidingtasks.model.TaskEvent
import org.wastingnotime.slidingtasks.model.TaskSchedule
import org.wastingnotime.slidingtasks.model.TaskType
import org.wastingnotime.slidingtasks.model.allWeekDays
import org.wastingnotime.slidingtasks.model.weekdays
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class LocalTaskStore(context: Context) {
    private val preferences = context.getSharedPreferences("sliding_tasks", Context.MODE_PRIVATE)

    fun load(): SlidingTasksState = try {
        preferences.getString(KEY_STATE, null)?.let { decode(JSONObject(it)) } ?: SlidingTasksState()
    } catch (error: Exception) {
        throw IllegalStateException("Could not read saved task state", error)
    }

    fun readImport(raw: String): SlidingTasksState = decode(JSONObject(raw))

    fun save(state: SlidingTasksState) {
        check(preferences.edit().putString(KEY_STATE, encode(state).toString()).commit()) {
            "Could not save local task state"
        }
    }

    private fun encode(state: SlidingTasksState) = JSONObject().apply {
        put("activeDate", state.activeDate?.toString())
        put("tasks", JSONArray().apply {
            state.tasks.forEach { task ->
                put(JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("type", task.type.name)
                    put("schedule", JSONObject().apply {
                        put("kind", task.schedule.kind.name)
                        put("interval", task.schedule.interval)
                        put("days", JSONArray(task.schedule.days.map { it.name }))
                        put("startsOn", task.schedule.startsOn.toString())
                    })
                    put("active", task.active)
                    put("createdOn", task.createdOn.toString())
                    put("resolved", task.resolved)
                })
            }
        })
        put("cards", JSONArray().apply {
            state.cards.forEach { card ->
                put(JSONObject().apply {
                    put("id", card.id)
                    put("taskId", card.taskId)
                    put("boardDate", card.boardDate.toString())
                    put("title", card.title)
                    put("type", card.type.name)
                    put("status", card.status.name)
                    put("touches", card.touches)
                    put("skipScope", card.skipScope.name)
                })
            }
        })
        put("events", JSONArray().apply {
            state.events.forEach { event ->
                put(JSONObject().apply {
                    put("id", event.id)
                    put("type", event.type)
                    put("occurredAt", event.occurredAt.toString())
                    put("taskId", event.taskId)
                    put("cardId", event.cardId)
                    put("title", event.title)
                })
            }
        })
    }

    private fun decode(root: JSONObject): SlidingTasksState {
        val tasks = root.getJSONArray("tasks").mapObjects { json ->
            PlannedTask(
                id = json.getString("id"),
                title = json.getString("title"),
                type = decodeTaskType(json.getString("type")),
                schedule = decodeSchedule(json),
                active = json.getBoolean("active"),
                createdOn = LocalDate.parse(json.getString("createdOn")),
                resolved = json.optBoolean("resolved"),
            )
        }
        return SlidingTasksState(
            activeDate = root.optString("activeDate").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
            tasks = tasks,
            cards = root.getJSONArray("cards").mapObjects { json ->
                val task = tasks.firstOrNull { it.id == json.getString("taskId") }
                TaskCard(
                    id = json.getString("id"),
                    taskId = json.getString("taskId"),
                    boardDate = LocalDate.parse(json.getString("boardDate")),
                    title = json.getString("title"),
                    type = decodeTaskType(json.getString("type")),
                    status = CardStatus.valueOf(json.getString("status")),
                    touches = json.optInt("touches"),
                    skipScope = json.optString("skipScope").takeIf { it.isNotBlank() }
                        ?.let(SkipScope::valueOf) ?: when {
                        json.getString("type") == TaskType.ONE_TIME.name -> SkipScope.TASK
                        task?.schedule?.kind == ScheduleKind.ONCE_PER_WEEK -> SkipScope.WEEK
                        else -> SkipScope.TODAY
                    },
                )
            },
            events = root.getJSONArray("events").mapObjects { json ->
                TaskEvent(
                    id = json.getString("id"),
                    type = json.getString("type"),
                    occurredAt = Instant.parse(json.getString("occurredAt")),
                    taskId = json.getString("taskId"),
                    cardId = json.optString("cardId").takeIf { it.isNotBlank() && it != "null" },
                    title = json.getString("title"),
                )
            },
        )
    }

    private fun decodeSchedule(task: JSONObject): TaskSchedule {
        val modern = task.optJSONObject("schedule")
        if (modern != null) return TaskSchedule(
            kind = ScheduleKind.valueOf(modern.getString("kind")),
            interval = modern.getInt("interval"),
            days = modern.getJSONArray("days").let { days ->
                (0 until days.length()).map { DayOfWeek.valueOf(days.getString(it)) }.toSet()
            },
            startsOn = LocalDate.parse(modern.getString("startsOn")),
        )
        val startsOn = LocalDate.parse(task.optString("startsOn", task.getString("createdOn")))
        return when (task.getString("recurrence")) {
            "ONCE" -> TaskSchedule(ScheduleKind.ONCE, startsOn = startsOn)
            "DAILY" -> TaskSchedule(ScheduleKind.DAILY, startsOn = startsOn)
            "EVERY_TWO_DAYS" -> TaskSchedule(ScheduleKind.DAILY, interval = 2, startsOn = startsOn)
            "WEEKDAYS" -> TaskSchedule(ScheduleKind.WEEKLY_DAYS, days = weekdays, startsOn = startsOn)
            "WEEKENDS" -> TaskSchedule(ScheduleKind.WEEKLY_DAYS,
                days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), startsOn = startsOn)
            "WEEKLY", "EVERY_TWO_WEEKS" -> {
                val days = when (task.optString("availableDays", "ANY_DAY")) {
                    "WEEKDAYS" -> weekdays
                    "SATURDAY" -> setOf(DayOfWeek.SATURDAY)
                    "SUNDAY" -> setOf(DayOfWeek.SUNDAY)
                    else -> allWeekDays
                }
                TaskSchedule(ScheduleKind.ONCE_PER_WEEK,
                    interval = if (task.getString("recurrence") == "WEEKLY") 1 else 2,
                    days = days, startsOn = startsOn)
            }
            else -> error("Unknown legacy recurrence")
        }
    }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }

    private fun decodeTaskType(value: String): TaskType = when (value) {
        "FOCUS" -> TaskType.ROUTINE
        else -> TaskType.valueOf(value)
    }

    private companion object {
        const val KEY_STATE = "state-v1"
    }
}
