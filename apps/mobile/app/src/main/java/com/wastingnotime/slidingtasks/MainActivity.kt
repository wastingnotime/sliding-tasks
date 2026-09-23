package com.wastingnotime.slidingtasks

import android.os.Bundle
import android.app.DatePickerDialog
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.wastingnotime.slidingtasks.data.LocalTaskStore
import com.wastingnotime.slidingtasks.model.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_SlidingTasks)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SlidingTasksApp() }
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFFE85D3F),
    onPrimary = Color.White,
    secondary = Color(0xFF377A45),
    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF231F20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF231F20),
    surfaceVariant = Color(0xFFDDE9D7),
    onSurfaceVariant = Color(0xFF263528),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A2),
    onPrimary = Color(0xFF5F1607),
    secondary = Color(0xFFA2D5AA),
    background = Color(0xFF181211),
    onBackground = Color(0xFFF1DFDB),
    surface = Color(0xFF241C1A),
    onSurface = Color(0xFFF1DFDB),
    surfaceVariant = Color(0xFF243127),
    onSurfaceVariant = Color(0xFFD4E8D6),
)
private enum class AppSection(val label: String) { TODAY("Today"), PLAN("Plan"), REVIEW("Review") }

@Composable
fun SlidingTasksApp() {
    val context = LocalContext.current
    val store = remember { LocalTaskStore(context.applicationContext) }
    val engine = remember { SlidingTasksEngine() }
    val today = remember { LocalDate.now() }
    var state by remember { mutableStateOf(engine.openDay(store.load(), today)) }
    var section by remember { mutableStateOf(AppSection.TODAY) }
    var planEditorOpen by remember { mutableStateOf(false) }
    var planEditorTask by remember { mutableStateOf<PlannedTask?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }

    fun commit(next: SlidingTasksState): Boolean {
        return runCatching { store.save(next) }
            .onSuccess { state = next; saveError = null }
            .onFailure { saveError = "Could not save that change. Please try again." }
            .isSuccess
    }
    LaunchedEffect(Unit) { commit(state) }
    BackHandler(enabled = planEditorOpen) { planEditorOpen = false }

    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!planEditorOpen) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    AppSection.entries.forEach { item ->
                        NavigationBarItem(
                            selected = section == item,
                            onClick = { section = item },
                            icon = { Text(item.label.take(1), fontWeight = FontWeight.Black) },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag("nav-${item.name.lowercase()}"),
                        )
                    }
                }
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
                when (section) {
                    AppSection.TODAY -> TodayScreen(today, engine.pendingCards(state)) { commit(engine.apply(state, it)) }
                    AppSection.PLAN -> if (planEditorOpen) {
                        PlanEditor(
                            task = planEditorTask,
                            currentDate = today,
                            onCancel = { planEditorOpen = false },
                            onSave = { title, type, recurrence, availableDays, startsOn ->
                                val task = planEditorTask
                                val saved = if (task == null) {
                                    commit(engine.createTask(state, title, type, recurrence, today, availableDays, startsOn))
                                } else {
                                    commit(engine.updateTask(state, task.id, title, type, recurrence, availableDays, startsOn))
                                }
                                if (saved) planEditorOpen = false
                            },
                        )
                    } else {
                        PlanScreen(
                            tasks = state.tasks,
                            onAdd = { planEditorTask = null; planEditorOpen = true },
                            onEdit = { planEditorTask = it; planEditorOpen = true },
                            onSetActive = { id, active -> commit(engine.setTaskActive(state, id, active)) },
                            onMove = { id, offset -> commit(engine.moveTask(state, id, offset)) },
                            onRemove = { id -> commit(engine.removeTask(state, id)) },
                        )
                    }
                    AppSection.REVIEW -> ReviewScreen(state, today)
                }
            }
        }
    }
}

@Composable
private fun PageHeader(kicker: String, title: String, subtitle: String, subtitleTag: String? = null) {
    Text(kicker, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black)
    Text(
        subtitle,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f),
        fontSize = 16.sp,
        modifier = if (subtitleTag == null) Modifier else Modifier.testTag(subtitleTag),
    )
}

@Composable
private fun TodayScreen(date: LocalDate, cards: List<TaskCard>, onCommand: (CardCommand) -> Unit) {
    val count = "${cards.size} ${if (cards.size == 1) "card" else "cards"} left"
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp)) {
        PageHeader("TODAY", date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), count, "remaining-count")
        Spacer(Modifier.height(24.dp))
        if (cards.isEmpty()) EmptyBoard() else {
            Text("Slide left for not today · right for done", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f), fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(cards, key = { it.id }) { card -> SwipeableTaskCard(card, onCommand) }
            }
        }
    }
}

@Composable
private fun PlanScreen(
    tasks: List<PlannedTask>,
    onAdd: () -> Unit,
    onEdit: (PlannedTask) -> Unit,
    onSetActive: (String, Boolean) -> Unit,
    onMove: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
) {
    var pendingRemoval by remember { mutableStateOf<PlannedTask?>(null) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val currentOnMove by rememberUpdatedState(onMove)
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragPointerY by remember { mutableFloatStateOf(0f) }
    var dragGrabOffset by remember { mutableFloatStateOf(0f) }
    var draggedItemHeight by remember { mutableIntStateOf(0) }
    var dragTargetIndex by remember { mutableIntStateOf(-1) }

    fun targetIndexAt(y: Float): Int {
        val visible = listState.layoutInfo.visibleItemsInfo
        return visible.firstOrNull { y < it.offset + it.size / 2f }?.index
            ?: visible.lastOrNull()?.index ?: -1
    }

    fun finishDrag(save: Boolean) {
        val id = draggingId
        if (save && id != null && dragTargetIndex >= 0) {
            val from = tasks.indexOfFirst { it.id == id }
            if (from >= 0 && from != dragTargetIndex) currentOnMove(id, dragTargetIndex - from)
        }
        draggingId = null
        dragTargetIndex = -1
    }

    LaunchedEffect(draggingId) {
        while (draggingId != null) {
            val edge = with(density) { 64.dp.toPx() }
            val end = listState.layoutInfo.viewportEndOffset.toFloat()
            val scroll = when {
                dragPointerY < edge && listState.canScrollBackward -> -24f
                dragPointerY > end - edge && listState.canScrollForward -> 24f
                else -> 0f
            }
            if (scroll != 0f) {
                listState.scrollBy(scroll)
                dragTargetIndex = targetIndexAt(dragPointerY)
            }
            delay(16)
        }
    }

    pendingRemoval?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove from plan?") },
            text = { Text(task.title + " will stop appearing on future days. Existing cards and history are preserved.") },
            confirmButton = {
                TextButton(onClick = { onRemove(task.id); pendingRemoval = null }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") } },
        )
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp)) {
        PageHeader("PLAN", "Your intentions", tasks.size.toString() + " planned entries")
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().testTag("add-entry")) {
            Text("Add entry")
        }
        Spacer(Modifier.height(12.dp))
        if (tasks.isEmpty()) {
            Text("No tasks yet.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f))
        } else {
            Text("Touch and hold an entry to reorder", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().pointerInput(tasks) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { position ->
                            val item = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                                position.y >= it.offset && position.y < it.offset + it.size
                            }
                            if (item != null && item.index in tasks.indices) {
                                draggingId = tasks[item.index].id
                                dragPointerY = position.y
                                dragGrabOffset = position.y - item.offset
                                draggedItemHeight = item.size
                                dragTargetIndex = item.index
                            }
                        },
                        onDrag = { change, _ ->
                            if (draggingId != null) {
                                change.consume()
                                dragPointerY = change.position.y
                                dragTargetIndex = targetIndexAt(dragPointerY)
                            }
                        },
                        onDragEnd = { finishDrag(true) },
                        onDragCancel = { finishDrag(false) },
                    )
                },
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                    Card(
                        modifier = Modifier
                            .alpha(if (draggingId == task.id) .25f else 1f)
                            .semantics {
                                customActions = buildList {
                                    if (index > 0) add(CustomAccessibilityAction("Move up") {
                                        onMove(task.id, -1)
                                        true
                                    })
                                    if (index < tasks.lastIndex) add(CustomAccessibilityAction("Move down") {
                                        onMove(task.id, 1)
                                        true
                                    })
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = if (draggingId != null && dragTargetIndex == index && draggingId != task.id)
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(task.title, color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                                    Text(task.scheduleSummary(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f))
                                }
                                Switch(checked = task.active, enabled = !task.resolved,
                                    onCheckedChange = { onSetActive(task.id, it) })
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onEdit(task) }, modifier = Modifier.testTag("edit-" + task.id)) {
                                    Text("Edit")
                                }
                                TextButton(onClick = { pendingRemoval = task },
                                    modifier = Modifier.testTag("remove-" + task.id)) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
            tasks.firstOrNull { it.id == draggingId }?.let { dragged ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .offset { IntOffset(0, (dragPointerY - dragGrabOffset).roundToInt()) }
                        .height(with(density) { draggedItemHeight.toDp() }),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text(dragged.title, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text(dragged.scheduleSummary(), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .68f))
                    }
                }
            }
            }
        }
    }
}

private fun PlannedTask.scheduleSummary() = buildString {
    append(type.label)
    append(" · ")
    append(recurrence.label)
    if (recurrence == Recurrence.WEEKLY || recurrence == Recurrence.EVERY_TWO_WEEKS) {
        append(" · ")
        append(availableDays.label)
    }
    if (resolved) append(" · Resolved")
}

@Composable
private fun PlanEditor(
    task: PlannedTask?,
    currentDate: LocalDate,
    onCancel: () -> Unit,
    onSave: (String, TaskType, Recurrence, AvailableDays, LocalDate) -> Unit,
) {
    var title by remember(task?.id) { mutableStateOf(task?.title ?: "") }
    var type by remember(task?.id) { mutableStateOf(task?.type ?: TaskType.ROUTINE) }
    var recurrence by remember(task?.id) { mutableStateOf(task?.recurrence ?: Recurrence.DAILY) }
    var availableDays by remember(task?.id) { mutableStateOf(task?.availableDays ?: AvailableDays.ANY_DAY) }
    var startsOn by remember(task?.id) { mutableStateOf(task?.startsOn ?: currentDate) }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
        TextButton(onClick = onCancel, modifier = Modifier.testTag("cancel-edit")) { Text("Cancel") }
        PageHeader("PLAN", if (task == null) "New entry" else "Edit entry", "Set when this entry appears.")
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What do you want to do?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("task-title"),
            )
            Text("Type", modifier = Modifier.padding(top = 14.dp), fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskType.entries.forEach { candidate ->
                    FilterChip(
                        selected = type == candidate,
                        onClick = {
                            type = candidate
                            recurrence = if (candidate == TaskType.ONE_TIME) Recurrence.ONCE else Recurrence.DAILY
                            availableDays = AvailableDays.ANY_DAY
                            startsOn = currentDate
                        },
                        label = { Text(candidate.label) },
                        modifier = Modifier.testTag("type-" + candidate.name.lowercase()),
                    )
                }
            }
            if (type != TaskType.ONE_TIME) {
                Text("Repeat", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Recurrence.entries.filter { it != Recurrence.ONCE }.forEach { candidate ->
                        FilterChip(
                            selected = recurrence == candidate,
                            onClick = {
                                recurrence = candidate
                                availableDays = AvailableDays.ANY_DAY
                                startsOn = currentDate
                            },
                            label = { Text(candidate.label) },
                            modifier = Modifier.testTag("repeat-" + candidate.name.lowercase()),
                        )
                    }
                }
                if (recurrence == Recurrence.WEEKLY || recurrence == Recurrence.EVERY_TWO_WEEKS) {
                    Text("Available", fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AvailableDays.entries.forEach { candidate ->
                            FilterChip(
                                selected = availableDays == candidate,
                                onClick = { availableDays = candidate },
                                label = { Text(candidate.label) },
                                modifier = Modifier.testTag("available-" + candidate.name.lowercase()),
                            )
                        }
                    }
                }
                if (recurrence == Recurrence.EVERY_TWO_DAYS || recurrence == Recurrence.EVERY_TWO_WEEKS) {
                    TextButton(onClick = {
                        DatePickerDialog(context, { _, year, month, day ->
                            startsOn = LocalDate.of(year, month + 1, day)
                        }, startsOn.year, startsOn.monthValue - 1, startsOn.dayOfMonth).show()
                    }, modifier = Modifier.testTag("repeat-start")) {
                        Text((if (recurrence == Recurrence.EVERY_TWO_WEEKS) "First active week: " else "First day: ") + startsOn)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onSave(title, type, recurrence, availableDays, startsOn) },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag(if (task == null) "add-task" else "save-task"),
        ) { Text(if (task == null) "Add to plan" else "Save changes") }
    }
}

@Composable
private fun ReviewScreen(state: SlidingTasksState, today: LocalDate) {
    val review = remember(state.cards, state.tasks, today) { reviewInsights(state, today) }
    var expandedDay by remember { mutableStateOf<LocalDate?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PageHeader("REVIEW", "Your recent patterns", "A quick look at today and the recent past.")
        }
        item {
            ReviewMetricCard(
                title = "THIS WEEK",
                headline = if (review.week.closed + review.week.open == 0) "No cards yet" else
                    review.week.done.toString() + " done",
                detail = review.week.notToday.toString() + " Not today · " +
                    review.week.missed + " missed · " + review.week.open + " still open",
                tag = "review-week",
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("review-patterns"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("LAST 14 FULL DAYS", color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
                    ReviewPatternRow("Missed most", review.mostMissed, "No clear pattern yet", "missed")
                    HorizontalDivider()
                    ReviewPatternRow("Most often Not today", review.mostNotToday, "None recently", "marked Not today")
                    HorizontalDivider()
                    ReviewPatternRow("Kept up with", review.mostDone, "Not enough completed days yet", "done")
                }
            }
        }
        item {
            val trend = review.trend
            ReviewMetricCard(
                title = "RECENT DIRECTION",
                headline = if (trend == null) "Not enough history yet" else
                    trend.recent.donePercent.toString() + "% vs " + trend.previous.donePercent + "%",
                detail = if (trend == null) "Needs at least 3 closed cards in each 7-day period" else
                    "Done share of closed cards: last 7 full days vs previous 7",
                tag = "review-trend",
            )
        }
        item {
            Text("RECENT DAYS", color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        if (review.days.isEmpty()) {
            item { Text("Your recent card decisions will appear here.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f)) }
        } else {
            items(review.days, key = { it.first.toString() }) { (date, cards) ->
                val expanded = expandedDay == date
                Card(
                    onClick = { expandedDay = if (expanded) null else date },
                    modifier = Modifier.fillMaxWidth().testTag("review-day-" + date),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(if (expanded) "Hide" else "Show",
                                color = MaterialTheme.colorScheme.primary)
                        }
                        val counts = cards.groupingBy { it.status }.eachCount()
                        Text(
                            (counts[CardStatus.DONE] ?: 0).toString() + " done · " +
                                (counts[CardStatus.DISMISSED] ?: 0) + " Not today · " +
                                (counts[CardStatus.MISSED] ?: 0) + " missed" +
                                if ((counts[CardStatus.PENDING] ?: 0) > 0) " · " +
                                    counts[CardStatus.PENDING] + " open" else "",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f),
                        )
                        if (expanded) {
                            HorizontalDivider()
                            cards.forEach { card ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(card.title, modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(card.status.reviewLabel(), color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewMetricCard(title: String, headline: String, detail: String, tag: String) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(tag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
            Text(headline, color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Text(detail, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f))
        }
    }
}

@Composable
private fun ReviewPatternRow(label: String, pattern: ReviewPattern?, empty: String, measure: String) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f), fontSize = 13.sp)
        Text(pattern?.title ?: empty, color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold)
        if (pattern != null) Text(
            pattern.matching.toString() + " of " + pattern.total + " recent card days " + measure,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f), fontSize = 13.sp,
        )
    }
}

private fun CardStatus.reviewLabel() = when (this) {
    CardStatus.PENDING -> "OPEN"
    CardStatus.DONE -> "DONE"
    CardStatus.DISMISSED -> "NOT TODAY"
    CardStatus.MISSED -> "MISSED"
}

@Composable
private fun SwipeableTaskCard(card: TaskCard, onCommand: (CardCommand) -> Unit) {
    var offsetX by remember(card.id) { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val currentOnCommand by rememberUpdatedState(onCommand)
    var cardWidth by remember(card.id) { mutableFloatStateOf(0f) }
    val progress = if (cardWidth == 0f) 0f else (offsetX / (cardWidth * .28f)).coerceIn(-1f, 1f)
    Box(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("DONE", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, modifier = Modifier.alpha(progress.coerceAtLeast(0f)))
            Text("NOT TODAY", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, modifier = Modifier.alpha((-progress).coerceAtLeast(0f)))
        }
        TaskCardView(
            card,
            Modifier.testTag("card-${card.id}")
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("Mark done") { currentOnCommand(CardCommand.Complete(card.id)); true },
                        CustomAccessibilityAction("Not today") { currentOnCommand(CardCommand.Dismiss(card.id)); true },
                    )
                }
                .onSizeChanged { cardWidth = it.width.toFloat() }
                .graphicsLayer { translationX = offsetX }
                .pointerInput(card.id, cardWidth) {
                    detectHorizontalDragGestures(
                        onDragStart = { currentOnCommand(CardCommand.Touch(card.id)) },
                        onHorizontalDrag = { change, amount -> change.consume(); offsetX += amount },
                        onDragCancel = { scope.launch { animate(offsetX, 0f, animationSpec = tween(180)) { value, _ -> offsetX = value } } },
                        onDragEnd = {
                            scope.launch {
                                val decision = slideDecision(offsetX, cardWidth)
                                if (decision == null) animate(offsetX, 0f, animationSpec = tween(180)) { value, _ -> offsetX = value }
                                else {
                                    val target = if (decision == SlideDecision.COMPLETE) cardWidth * 1.25f else -cardWidth * 1.25f
                                    animate(offsetX, target, animationSpec = tween(180)) { value, _ -> offsetX = value }
                                    currentOnCommand(if (decision == SlideDecision.COMPLETE) CardCommand.Complete(card.id) else CardCommand.Dismiss(card.id))
                                }
                            }
                        },
                    )
                },
        )
    }
}

@Composable
private fun TaskCardView(card: TaskCard, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)))
                Text(card.type.label.uppercase(), Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f), fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
            }
            Text(card.title, Modifier.padding(vertical = 18.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyBoard() {
    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)).padding(28.dp)) {
        Column {
            Text("All clear.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("The present is handled.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f), fontSize = 17.sp)
        }
    }
}
