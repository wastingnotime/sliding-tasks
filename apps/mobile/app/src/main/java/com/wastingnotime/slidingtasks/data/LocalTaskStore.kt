package com.wastingnotime.slidingtasks.data

import android.content.Context
import com.wastingnotime.slidingtasks.model.CardStatus
import com.wastingnotime.slidingtasks.model.AvailableDays
import com.wastingnotime.slidingtasks.model.PlannedTask
import com.wastingnotime.slidingtasks.model.Recurrence
import com.wastingnotime.slidingtasks.model.SlidingTasksState
import com.wastingnotime.slidingtasks.model.TaskCard
import com.wastingnotime.slidingtasks.model.TaskEvent
import com.wastingnotime.slidingtasks.model.TaskType
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

class LocalTaskStore(context: Context) {
    private val preferences = context.getSharedPreferences("sliding_tasks", Context.MODE_PRIVATE)

    fun load(): SlidingTasksState = runCatching {
        preferences.getString(KEY_STATE, null)?.let { decode(JSONObject(it)) } ?: SlidingTasksState()
    }.getOrElse { SlidingTasksState() }

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
                    put("recurrence", task.recurrence.name)
                    put("active", task.active)
                    put("createdOn", task.createdOn.toString())
                    put("availableDays", task.availableDays.name)
                    put("startsOn", task.startsOn.toString())
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

    private fun decode(root: JSONObject) = SlidingTasksState(
        activeDate = root.optString("activeDate").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
        tasks = root.getJSONArray("tasks").mapObjects { json ->
            PlannedTask(
                id = json.getString("id"),
                title = json.getString("title"),
                type = decodeTaskType(json.getString("type")),
                recurrence = Recurrence.valueOf(json.getString("recurrence")),
                active = json.getBoolean("active"),
                createdOn = LocalDate.parse(json.getString("createdOn")),
                resolved = json.optBoolean("resolved"),
                availableDays = AvailableDays.valueOf(json.optString("availableDays", AvailableDays.ANY_DAY.name)),
                startsOn = LocalDate.parse(json.optString("startsOn", json.getString("createdOn"))),
            )
        },
        cards = root.getJSONArray("cards").mapObjects { json ->
            TaskCard(
                id = json.getString("id"),
                taskId = json.getString("taskId"),
                boardDate = LocalDate.parse(json.getString("boardDate")),
                title = json.getString("title"),
                type = decodeTaskType(json.getString("type")),
                status = CardStatus.valueOf(json.getString("status")),
                touches = json.optInt("touches"),
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
