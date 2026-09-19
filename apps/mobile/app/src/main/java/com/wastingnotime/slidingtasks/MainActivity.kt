package com.wastingnotime.slidingtasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wastingnotime.slidingtasks.data.LocalTaskStore
import com.wastingnotime.slidingtasks.model.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
private enum class AppSection(val label: String) { TODAY("Today"), PLAN("Plan"), HISTORY("History") }

@Composable
fun SlidingTasksApp() {
    val context = LocalContext.current
    val store = remember { LocalTaskStore(context.applicationContext) }
    val engine = remember { SlidingTasksEngine() }
    val today = remember { LocalDate.now() }
    var state by remember { mutableStateOf(engine.openDay(store.load(), today)) }
    var section by remember { mutableStateOf(AppSection.TODAY) }
    var saveError by remember { mutableStateOf<String?>(null) }

    fun commit(next: SlidingTasksState) {
        runCatching { store.save(next) }
            .onSuccess { state = next; saveError = null }
            .onFailure { saveError = "Could not save that change. Please try again." }
    }
    LaunchedEffect(Unit) { commit(state) }

    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
                    AppSection.PLAN -> PlanScreen(
                        tasks = state.tasks,
                        onCreate = { title, type, recurrence -> commit(engine.createTask(state, title, type, recurrence, today)) },
                        onSetActive = { id, active -> commit(engine.setTaskActive(state, id, active)) },
                        onMove = { id, offset -> commit(engine.moveTask(state, id, offset)) },
                        onRemove = { id -> commit(engine.removeTask(state, id)) },
                    )
                    AppSection.HISTORY -> HistoryScreen(state)
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
    onCreate: (String, TaskType, Recurrence) -> Unit,
    onSetActive: (String, Boolean) -> Unit,
    onMove: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TaskType.ROUTINE) }
    var recurrence by remember { mutableStateOf(Recurrence.DAILY) }
    var pendingRemoval by remember { mutableStateOf<PlannedTask?>(null) }

    pendingRemoval?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove from plan?") },
            text = { Text("${task.title} will stop appearing on future days. Existing cards and history are preserved.") },
            confirmButton = {
                TextButton(onClick = { onRemove(task.id); pendingRemoval = null }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") } },
        )
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp)) {
        PageHeader("PLAN", "Your intentions", "Create locally. Change them anytime.")
        Spacer(Modifier.height(20.dp))
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
                    },
                    label = { Text(candidate.label) },
                    modifier = Modifier.testTag("type-${candidate.name.lowercase()}"),
                )
            }
        }
        if (type != TaskType.ONE_TIME) {
            Text("Repeat", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Recurrence.entries.filter { it != Recurrence.ONCE }.forEach { candidate ->
                    FilterChip(selected = recurrence == candidate, onClick = { recurrence = candidate }, label = { Text(candidate.label) })
                }
            }
        }
        Button(
            onClick = { onCreate(title, type, recurrence); title = "" },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("add-task"),
        ) { Text("Add to plan") }
        Spacer(Modifier.height(18.dp))
        Text("PLANNED", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        if (tasks.isEmpty()) Text("No tasks yet.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f)) else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(task.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                                    Text("${task.type.label} · ${task.recurrence.label}${if (task.resolved) " · Resolved" else ""}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f))
                                }
                                Switch(checked = task.active, enabled = !task.resolved, onCheckedChange = { onSetActive(task.id, it) })
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(
                                    onClick = { onMove(task.id, -1) },
                                    enabled = index > 0,
                                    modifier = Modifier.testTag("move-up-${task.id}"),
                                ) { Text("Up") }
                                TextButton(
                                    onClick = { onMove(task.id, 1) },
                                    enabled = index < tasks.lastIndex,
                                    modifier = Modifier.testTag("move-down-${task.id}"),
                                ) { Text("Down") }
                                TextButton(
                                    onClick = { pendingRemoval = task },
                                    modifier = Modifier.testTag("remove-${task.id}"),
                                ) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(state: SlidingTasksState) {
    val resolved = state.cards.count { it.status != CardStatus.PENDING }
    val done = state.cards.count { it.status == CardStatus.DONE }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp)) {
        PageHeader("HISTORY", "$done done", "$resolved resolved cards · ${state.events.size} recorded events")
        Spacer(Modifier.height(20.dp))
        if (state.events.isEmpty()) Text("Your decisions will appear here.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f)) else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.events.asReversed(), key = { it.id }) { event -> EventRow(event) }
            }
        }
    }
}

@Composable
private fun EventRow(event: TaskEvent) {
    val time = event.occurredAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(event.type.eventLabel(), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(event.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(time, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), fontSize = 13.sp)
        }
    }
}

private fun String.eventLabel() = when (this) {
    "TaskCreated" -> "PLANNED"
    "TaskActivated" -> "ACTIVATED"
    "TaskDeactivated" -> "PAUSED"
    "TaskRemoved" -> "REMOVED FROM PLAN"
    "TaskReordered" -> "PLAN REORDERED"
    "CardGenerated" -> "ADDED TO DAY"
    "CardTouched" -> "TOUCHED"
    "CardDone" -> "DONE"
    "CardDismissed" -> "NOT TODAY"
    "CardMissed" -> "MISSED"
    else -> uppercase()
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
