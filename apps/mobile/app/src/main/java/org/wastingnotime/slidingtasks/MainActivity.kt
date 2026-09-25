package org.wastingnotime.slidingtasks

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import org.wastingnotime.slidingtasks.data.LocalTaskStore
import org.wastingnotime.slidingtasks.model.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.DayOfWeek
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
private enum class PlanMode(val label: String) {
    ROUTINE("Routine"), UNTIL_DECIDED("Until decided"), ONE_TIME("One-time"),
}

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
    var aboutOpen by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    fun commit(next: SlidingTasksState): Boolean {
        return runCatching { store.save(next) }
            .onSuccess { state = next; saveError = null }
            .onFailure { saveError = "Could not save that change. Please try again." }
            .isSuccess
    }
    LaunchedEffect(Unit) { commit(state) }
    BackHandler(enabled = planEditorOpen || aboutOpen) {
        if (aboutOpen) aboutOpen = false else planEditorOpen = false
    }

    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!planEditorOpen && !aboutOpen) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
                if (aboutOpen) AboutScreen(onBack = { aboutOpen = false }) else when (section) {
                    AppSection.TODAY -> TodayScreen(
                        date = today,
                        cards = engine.pendingCards(state),
                        onAbout = { aboutOpen = true },
                        pausedOneTimeTasks = state.tasks.filter { task ->
                            task.type == TaskType.ONE_TIME && !task.active && !task.resolved &&
                                state.cards.none { it.taskId == task.id && it.boardDate == today && it.status == CardStatus.PENDING }
                        },
                        onCommand = { commit(engine.apply(state, it)) },
                        onCreateOneTime = { title ->
                            commit(engine.createTask(state, title, TaskType.ONE_TIME,
                                TaskSchedule(ScheduleKind.ONCE, startsOn = today), today))
                        },
                        onResumeOneTime = { id -> commit(engine.openDay(engine.setTaskActive(state, id, true), today)) },
                        onRemoveOneTime = { id -> commit(engine.removeTask(state, id)) },
                    )
                    AppSection.PLAN -> if (planEditorOpen) {
                        PlanEditor(
                            task = planEditorTask,
                            currentDate = today,
                            onCancel = { planEditorOpen = false },
                            onSave = { title, schedule ->
                                val task = planEditorTask
                                val type = if (schedule.kind == ScheduleKind.ONCE) TaskType.ONE_TIME else TaskType.ROUTINE
                                val saved = if (task == null) {
                                    commit(engine.createTask(state, title, type, schedule, today))
                                } else {
                                    commit(engine.updateTask(state, task.id, title, type, schedule))
                                }
                                if (saved) planEditorOpen = false
                            },
                        )
                    } else {
                        PlanScreen(
                            tasks = state.tasks.filter { it.type == TaskType.ROUTINE || !it.resolved },
                            onAbout = { aboutOpen = true },
                            onAdd = { planEditorTask = null; planEditorOpen = true },
                            onEdit = { planEditorTask = it; planEditorOpen = true },
                            onSetActive = { id, active -> commit(engine.setTaskActive(state, id, active)) },
                            onMove = { id, offset ->
                                val planned = state.tasks.filter { it.type == TaskType.ROUTINE || !it.resolved }
                                val from = planned.indexOfFirst { it.id == id }
                                if (from >= 0) {
                                    val to = (from + offset).coerceIn(planned.indices)
                                    val absoluteFrom = state.tasks.indexOfFirst { it.id == id }
                                    val absoluteTo = state.tasks.indexOfFirst { it.id == planned[to].id }
                                    commit(engine.moveTask(state, id, absoluteTo - absoluteFrom))
                                }
                            },
                            onRemove = { id -> commit(engine.removeTask(state, id)) },
                        )
                    }
                    AppSection.REVIEW -> ReviewScreen(state, today, onAbout = { aboutOpen = true })
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader("SLIDING TASKS", "About", "The past is observed. The future is configured. The present is acted upon.", onBack = onBack)
        Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f))
        HorizontalDivider()
        Text("Quick guide", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Plan: add routines, until-decided tasks, or one-time tasks.")
        Text("Today: add one-time tasks, slide a card right for Done, or left to skip its current occurrence.")
        Text("Review: see recent decisions and patterns over time.")
        HorizontalDivider()
        Text("Wasting No Time", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Wasting No Time (WNT) makes Sliding Tasks. The app helps you plan ahead, make a simple choice today, and learn from what you did over time.")
        HorizontalDivider()
        Text("Privacy", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Effective September 23, 2026")
        Text("Sliding Tasks is provided by Wasting No Time. It works offline and does not require an account.")
        Text("Your task titles, schedules, card decisions, and history are stored on this device. The current app does not send them to us or other companies. It has no ads, analytics, or sync service.")
        Text("You can remove individual planned tasks in the app. To erase all Sliding Tasks data, use Android Settings → Apps → Sliding Tasks → Storage → Clear storage, or uninstall the app. Device backup is disabled for this app.")
        Text("If the app later adds sync, analytics, or other data processing, this policy will be updated before those features are released.")
        Text("Questions? Contact sliding-tasks@wastingnotime.org.")
        Text("Public policy: https://wastingnotime.org/sliding-tasks/privacy/", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PageHeader(
    kicker: String,
    title: String,
    subtitle: String,
    subtitleTag: String? = null,
    onAbout: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    var moreMenuOpen by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Column {
            Text(kicker, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(
                title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                modifier = if (onAbout == null && onBack == null) Modifier else Modifier.padding(end = 48.dp),
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f),
                fontSize = 16.sp,
                modifier = if (subtitleTag == null) Modifier else Modifier.testTag(subtitleTag),
            )
        }
        if (onBack != null) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.TopEnd)) { Text("Back") }
        }
        if (onAbout != null) Box(Modifier.align(Alignment.TopEnd)) {
            Box(
                Modifier.size(48.dp)
                    .testTag("more-options")
                    .semantics { contentDescription = "More options" }
                    .clickable(role = Role.Button) { moreMenuOpen = true },
            ) {
                Text("⋮", modifier = Modifier.align(Alignment.TopCenter), fontSize = 22.sp)
            }
            DropdownMenu(expanded = moreMenuOpen, onDismissRequest = { moreMenuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = { moreMenuOpen = false; onAbout() },
                    modifier = Modifier.testTag("about-menu-item"),
                )
            }
        }
    }
}

@Composable
private fun TodayScreen(
    date: LocalDate,
    cards: List<TaskCard>,
    onAbout: () -> Unit,
    pausedOneTimeTasks: List<PlannedTask>,
    onCommand: (CardCommand) -> Unit,
    onCreateOneTime: (String) -> Boolean,
    onResumeOneTime: (String) -> Unit,
    onRemoveOneTime: (String) -> Unit,
) {
    val count = cards.size.toString() + " " + if (cards.size == 1) "card left" else "cards left"
    var addingOneTime by remember { mutableStateOf(false) }
    var oneTimeTitle by remember { mutableStateOf("") }
    var pendingRemoval by remember { mutableStateOf<PlannedTask?>(null) }

    if (addingOneTime) {
        AlertDialog(
            onDismissRequest = { addingOneTime = false },
            title = { Text("Add for today") },
            text = {
                OutlinedTextField(
                    value = oneTimeTitle,
                    onValueChange = { oneTimeTitle = it },
                    label = { Text("What needs doing?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("one-time-title"),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (onCreateOneTime(oneTimeTitle)) {
                            addingOneTime = false
                            oneTimeTitle = ""
                        }
                    },
                    enabled = oneTimeTitle.isNotBlank(),
                    modifier = Modifier.testTag("save-one-time"),
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { addingOneTime = false }) { Text("Cancel") } },
        )
    }
    pendingRemoval?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove one-time entry?") },
            text = { Text(task.title + " will be removed. Existing cards and history are preserved.") },
            confirmButton = {
                TextButton(onClick = { onRemoveOneTime(task.id); pendingRemoval = null }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") } },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PageHeader("TODAY", date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), count, "remaining-count", onAbout) }
        item {
            OutlinedButton(
                onClick = { oneTimeTitle = ""; addingOneTime = true },
                modifier = Modifier.fillMaxWidth().testTag("add-one-time"),
            ) { Text("Add one-time task") }
        }
        if (pausedOneTimeTasks.isNotEmpty()) {
            item {
                Text("PAUSED ONE-TIME ENTRIES", color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            items(pausedOneTimeTasks, key = { "paused-" + it.id }) { task ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(task.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { onResumeOneTime(task.id) }) { Text("Resume") }
                        TextButton(onClick = { pendingRemoval = task }) {
                            Text("Remove", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        if (cards.isEmpty()) {
            item { EmptyBoard() }
        } else {
            item {
                Text("Slide left to skip · right for done",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f), fontSize = 14.sp)
            }
            items(cards, key = { it.id }) { card -> SwipeableTaskCard(card, onCommand) }
        }
    }
}

@Composable
private fun PlanScreen(
    tasks: List<PlannedTask>,
    onAbout: () -> Unit,
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
        PageHeader("PLAN", "Your plans", tasks.size.toString() + " planned entries", onAbout = onAbout)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().testTag("add-entry")) {
            Text("Add task")
        }
        Spacer(Modifier.height(12.dp))
        if (tasks.isEmpty()) {
            Text("No plans yet.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f))
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
    append(when (schedule.kind) {
        ScheduleKind.ONCE -> "One-time"
        ScheduleKind.DAILY -> "Routine · " +
            if (schedule.interval == 1) "Daily" else "Every ${schedule.interval} days"
        ScheduleKind.WEEKLY_DAYS -> "Routine · " +
            if (schedule.interval == 1) "Weekly" else "Every ${schedule.interval} weeks"
        ScheduleKind.ONCE_PER_WEEK -> "Until decided · " +
            if (schedule.interval == 1) "Every week" else "Every ${schedule.interval} weeks"
    })
    if (schedule.kind == ScheduleKind.WEEKLY_DAYS || schedule.kind == ScheduleKind.ONCE_PER_WEEK) {
        append(" · ")
        append(schedule.days.daySelectionLabel())
    }
    if (resolved) append(" · Resolved")
}

private fun Set<DayOfWeek>.daySelectionLabel(): String = when (this) {
    allWeekDays -> "Any day"
    weekdays -> "Weekdays"
    setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekend"
    else -> DayOfWeek.entries.filter { it in this }.joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar(Char::uppercase) }
}

@Composable
private fun PlanEditor(
    task: PlannedTask?,
    currentDate: LocalDate,
    onCancel: () -> Unit,
    onSave: (String, TaskSchedule) -> Unit,
) {
    var title by remember(task?.id) { mutableStateOf(task?.title ?: "") }
    var mode by remember(task?.id) {
        mutableStateOf(when (task?.schedule?.kind) {
            ScheduleKind.ONCE -> PlanMode.ONE_TIME
            ScheduleKind.ONCE_PER_WEEK -> PlanMode.UNTIL_DECIDED
            else -> PlanMode.ROUTINE
        })
    }
    var routineCadence by remember(task?.id) {
        mutableStateOf(
            if (task?.schedule?.kind == ScheduleKind.WEEKLY_DAYS) ScheduleKind.WEEKLY_DAYS else ScheduleKind.DAILY
        )
    }
    var interval by remember(task?.id) { mutableIntStateOf(task?.schedule?.interval ?: 1) }
    var days by remember(task?.id) { mutableStateOf(task?.schedule?.days ?: weekdays) }
    var startsOn by remember(task?.id) { mutableStateOf(task?.schedule?.startsOn ?: currentDate) }
    val kind = when (mode) {
        PlanMode.ROUTINE -> routineCadence
        PlanMode.UNTIL_DECIDED -> ScheduleKind.ONCE_PER_WEEK
        PlanMode.ONE_TIME -> ScheduleKind.ONCE
    }
    val intervalOptions = if (kind == ScheduleKind.DAILY) 1..7 else 1..4
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
        TextButton(onClick = onCancel, modifier = Modifier.testTag("cancel-edit")) { Text("Cancel") }
        PageHeader("PLAN", if (task == null) "New task" else "Edit task", "Choose when it appears and what closes it.")
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What do you want to do?") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("task-title"),
            )
            Text("Type", modifier = Modifier.padding(top = 14.dp), fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlanMode.entries.forEach { candidate ->
                    FilterChip(
                        selected = mode == candidate,
                        onClick = {
                            if (mode != candidate) {
                                mode = candidate
                                interval = 1
                                days = weekdays
                                startsOn = currentDate
                            }
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        label = { Text(candidate.label) },
                        modifier = Modifier.testTag("mode-" + candidate.name.lowercase()),
                    )
                }
            }
            Text(
                when (mode) {
                    PlanMode.ROUTINE -> "A new card appears on each scheduled day."
                    PlanMode.UNTIL_DECIDED -> "One card per active week. Done or Skip this week closes it."
                    PlanMode.ONE_TIME -> "One card carries forward until you mark it Done or Skip task."
                },
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f),
            )
            if (mode == PlanMode.ROUTINE) {
                Text("Schedule", modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ScheduleKind.DAILY, ScheduleKind.WEEKLY_DAYS).forEach { candidate ->
                        FilterChip(
                            selected = routineCadence == candidate,
                            onClick = {
                                if (routineCadence != candidate) {
                                    routineCadence = candidate
                                    interval = 1
                                    days = weekdays
                                    startsOn = currentDate
                                }
                            },
                            label = { Text(candidate.label) },
                            modifier = Modifier.testTag("repeat-" + candidate.name.lowercase()),
                        )
                    }
                }
            }
            if (mode != PlanMode.ONE_TIME) {
                Text(if (kind == ScheduleKind.DAILY) "Every how many days?" else "Every how many weeks?",
                    modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    intervalOptions.forEach { candidate ->
                        FilterChip(
                            selected = interval == candidate,
                            onClick = {
                                if (interval != candidate && candidate == 1) startsOn = currentDate
                                interval = candidate
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            },
                            label = { Text(candidate.toString()) },
                            modifier = Modifier
                                .testTag("repeat-interval-$candidate")
                                .semantics {
                                    contentDescription = "Every $candidate " +
                                        if (kind == ScheduleKind.DAILY) "days" else "weeks"
                                },
                        )
                    }
                }
                if (interval !in intervalOptions) {
                    Text("This routine is currently set to every $interval " +
                        if (kind == ScheduleKind.DAILY) "days. Choose 1–7 to change it."
                        else "weeks. Choose 1–4 to change it.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f))
                }
                if (kind != ScheduleKind.DAILY) {
                    Text("Days", modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Any day" to allWeekDays, "Weekdays" to weekdays,
                            "Weekend" to setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)).forEach { (label, selection) ->
                            FilterChip(
                                selected = days == selection,
                                onClick = { days = selection },
                                label = { Text(label) },
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            FilterChip(
                                selected = day in days,
                                onClick = { days = if (day in days) days - day else days + day },
                                label = { Text(day.name.take(3).lowercase().replaceFirstChar(Char::uppercase)) },
                                modifier = Modifier.testTag("day-" + day.name.lowercase()),
                            )
                        }
                    }
                    if (days.isEmpty()) Text("Select at least one day", color = MaterialTheme.colorScheme.error)
                }
                if (interval > 1) {
                    TextButton(onClick = {
                        DatePickerDialog(context, { _, year, month, day ->
                            startsOn = LocalDate.of(year, month + 1, day)
                        }, startsOn.year, startsOn.monthValue - 1, startsOn.dayOfMonth).show()
                    }, modifier = Modifier.testTag("repeat-start")) {
                        Text((if (kind == ScheduleKind.DAILY) "First active day: " else "First active week: ") + startsOn)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onSave(title, TaskSchedule(kind, interval, days, startsOn)) },
            enabled = title.isNotBlank() && (kind == ScheduleKind.ONCE || kind == ScheduleKind.DAILY || days.isNotEmpty()),
            modifier = Modifier.fillMaxWidth().testTag(if (task == null) "add-task" else "save-task"),
        ) { Text(if (task == null) "Add task" else "Save changes") }
    }
}

@Composable
private fun ReviewScreen(state: SlidingTasksState, today: LocalDate, onAbout: () -> Unit) {
    val review = remember(state.cards, state.tasks, today) { reviewInsights(state, today) }
    var expandedDay by remember { mutableStateOf<LocalDate?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PageHeader("REVIEW", "Your recent patterns", "A quick look at today and the recent past.", onAbout = onAbout)
        }
        item {
            ReviewMetricCard(
                title = "THIS WEEK",
                headline = if (review.week.closed + review.week.open == 0) "No cards yet" else
                    review.week.done.toString() + " done",
                detail = review.week.skipped.toString() + " Skipped · " +
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
                    ReviewPatternRow("Most often skipped", review.mostSkipped, "None recently", "skipped")
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
                                (counts[CardStatus.DISMISSED] ?: 0) + " Skipped · " +
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
    CardStatus.DISMISSED -> "SKIPPED"
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
            Text(card.skipScope.label.uppercase(), color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black, modifier = Modifier.alpha((-progress).coerceAtLeast(0f)))
        }
        TaskCardView(
            card,
            Modifier.testTag("card-${card.id}")
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("Mark done") { currentOnCommand(CardCommand.Complete(card.id)); true },
                        CustomAccessibilityAction(card.skipScope.label) { currentOnCommand(CardCommand.Dismiss(card.id)); true },
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
                Text(card.planLabel().uppercase(), Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f), fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
            }
            Text(card.title, Modifier.padding(vertical = 18.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text("Slide right for Done · left to ${card.skipScope.label.lowercase()}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f), fontSize = 14.sp)
        }
    }
}

private fun TaskCard.planLabel(): String = when {
    type == TaskType.ONE_TIME -> "One-time"
    skipScope == SkipScope.WEEK -> "Until decided"
    else -> "Routine"
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
