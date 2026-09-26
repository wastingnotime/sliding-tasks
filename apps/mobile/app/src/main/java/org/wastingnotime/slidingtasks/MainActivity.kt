package org.wastingnotime.slidingtasks

import android.os.Bundle
import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
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
private enum class AppSection(val label: Int, val icon: Int) {
    TODAY(R.string.today, R.drawable.ic_nav_today),
    PLAN(R.string.plan, R.drawable.ic_nav_plan),
    REVIEW(R.string.review, R.drawable.ic_nav_review),
}
private enum class PlanMode(val label: Int) {
    ROUTINE(R.string.routine), UNTIL_DECIDED(R.string.until_decided), ONE_TIME(R.string.one_time),
}

@Composable
fun SlidingTasksApp() {
    val context = LocalContext.current
    val store = remember { LocalTaskStore(context.applicationContext) }
    val engine = remember { SlidingTasksEngine() }
    val today = remember { LocalDate.now() }
    val initialState = remember { runCatching { engine.openDay(store.load(), today) } }
    if (initialState.isFailure) {
        MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors) {
            Column(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp, vertical = 48.dp)
                    .testTag("storage-load-error"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.storage_error_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.storage_error_message),
                    color = MaterialTheme.colorScheme.onBackground)
            }
        }
        return
    }
    var state by remember { mutableStateOf(initialState.getOrThrow()) }
    var section by remember { mutableStateOf(AppSection.TODAY) }
    var planEditorOpen by remember { mutableStateOf(false) }
    var planEditorTask by remember { mutableStateOf<PlannedTask?>(null) }
    var aboutOpen by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

    fun commit(next: SlidingTasksState): Boolean {
        return runCatching { store.save(next) }
            .onSuccess { state = next; saveError = null }
            .onFailure { saveError = context.getString(R.string.save_error) }
            .isSuccess
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not open selected file")
                store.readImport(raw).also { imported ->
                    check(imported.tasks.isNotEmpty() || imported.cards.isNotEmpty() || imported.events.isNotEmpty()) {
                        "Selected file contains no task data"
                    }
                }.let { engine.openDay(it, today) }
            }.onSuccess { imported ->
                if (commit(imported)) {
                    importError = null
                    aboutOpen = false
                    section = AppSection.TODAY
                }
            }.onFailure { importError = context.getString(R.string.import_error) }
        }
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
                            icon = { Icon(painterResource(item.icon), contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                            modifier = Modifier.testTag("nav-${item.name.lowercase()}"),
                        )
                    }
                }
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
                if (aboutOpen) AboutScreen(
                    onBack = { aboutOpen = false },
                    onImport = if (state.tasks.isEmpty() && state.cards.isEmpty() && state.events.isEmpty())
                        ({ importError = null; importLauncher.launch(arrayOf("application/json", "text/plain")) }) else null,
                    importError = importError,
                ) else when (section) {
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
private fun AboutScreen(onBack: () -> Unit, onImport: (() -> Unit)?, importError: String?) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader(stringResource(R.string.sliding_tasks), stringResource(R.string.about), stringResource(R.string.about_tagline), onBack = onBack)
        Text(stringResource(R.string.version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f))
        HorizontalDivider()
        Text(stringResource(R.string.quick_guide), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.guide_plan))
        Text(stringResource(R.string.guide_today))
        Text(stringResource(R.string.guide_review))
        if (onImport != null) {
            HorizontalDivider()
            Text(stringResource(R.string.restore_heading), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.restore_description))
            Button(onClick = onImport, modifier = Modifier.testTag("import-saved-tasks")) {
                Text(stringResource(R.string.import_saved_tasks))
            }
            importError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        HorizontalDivider()
        Text(stringResource(R.string.wasting_no_time), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.about_wnt))
        HorizontalDivider()
        Text(stringResource(R.string.privacy), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.privacy_effective_date))
        Text(stringResource(R.string.privacy_intro))
        Text(stringResource(R.string.privacy_storage))
        if (BuildConfig.DEBUG) {
            Text(stringResource(R.string.privacy_alpha_diagnostics))
        } else {
            Text(stringResource(R.string.privacy_play_diagnostics))
        }
        Text(stringResource(R.string.privacy_erasure))
        if (BuildConfig.DEBUG) {
            Text(stringResource(R.string.privacy_firebase_retention))
        }
        Text(stringResource(R.string.privacy_changes))
        Text(stringResource(R.string.privacy_contact))
        Text(stringResource(R.string.privacy_public_link), color = MaterialTheme.colorScheme.primary)
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
    val moreOptionsLabel = stringResource(R.string.more_options)
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
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.TopEnd)) { Text(stringResource(R.string.back)) }
        }
        if (onAbout != null) Box(Modifier.align(Alignment.TopEnd)) {
            Box(
                Modifier.size(48.dp)
                    .testTag("more-options")
                    .semantics { contentDescription = moreOptionsLabel }
                    .clickable(role = Role.Button) { moreMenuOpen = true },
            ) {
                Text("⋮", modifier = Modifier.align(Alignment.TopCenter), fontSize = 22.sp)
            }
            DropdownMenu(expanded = moreMenuOpen, onDismissRequest = { moreMenuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.about)) },
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
    val count = stringResource(if (cards.size == 1) R.string.card_left else R.string.cards_left, cards.size)
    var addingOneTime by remember { mutableStateOf(false) }
    var oneTimeTitle by remember { mutableStateOf("") }
    var pendingRemoval by remember { mutableStateOf<PlannedTask?>(null) }

    if (addingOneTime) {
        AlertDialog(
            onDismissRequest = { addingOneTime = false },
            title = { Text(stringResource(R.string.add_for_today)) },
            text = {
                OutlinedTextField(
                    value = oneTimeTitle,
                    onValueChange = { oneTimeTitle = it },
                    label = { Text(stringResource(R.string.what_needs_doing)) },
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
                ) { Text(stringResource(R.string.add)) }
            },
            dismissButton = { TextButton(onClick = { addingOneTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    pendingRemoval?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.remove_one_time_entry)) },
            text = { Text(stringResource(R.string.remove_one_time_message, task.title)) },
            confirmButton = {
                TextButton(onClick = { onRemoveOneTime(task.id); pendingRemoval = null }) {
                    Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PageHeader(stringResource(R.string.today_text), localizedDate(date, FormatStyle.FULL), count, "remaining-count", onAbout) }
        item {
            OutlinedButton(
                onClick = { oneTimeTitle = ""; addingOneTime = true },
                modifier = Modifier.fillMaxWidth().testTag("add-one-time"),
            ) { Text(stringResource(R.string.add_one_time_task)) }
        }
        if (pausedOneTimeTasks.isNotEmpty()) {
            item {
                Text(stringResource(R.string.paused_one_time_entries), color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            items(pausedOneTimeTasks, key = { "paused-" + it.id }) { task ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(task.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { onResumeOneTime(task.id) }) { Text(stringResource(R.string.resume)) }
                        TextButton(onClick = { pendingRemoval = task }) {
                            Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        if (cards.isEmpty()) {
            item { EmptyBoard() }
        } else {
            item {
                Text(stringResource(R.string.slide_left_to_skip_right_for_done),
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
    val moveUpLabel = stringResource(R.string.move_up)
    val moveDownLabel = stringResource(R.string.move_down)
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
            title = { Text(stringResource(R.string.remove_from_plan)) },
            text = { Text(stringResource(R.string.remove_plan_message, task.title)) },
            confirmButton = {
                TextButton(onClick = { onRemove(task.id); pendingRemoval = null }) {
                    Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp)) {
        PageHeader(stringResource(R.string.plan_text), stringResource(R.string.your_plans), quantity(R.plurals.planned_entries, tasks.size), onAbout = onAbout)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().testTag("add-entry")) {
            Text(stringResource(R.string.add_task))
        }
        Spacer(Modifier.height(12.dp))
        if (tasks.isEmpty()) {
            Text(stringResource(R.string.no_plans_yet), color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f))
        } else {
            Text(stringResource(R.string.reorder_hint), color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f), fontSize = 13.sp)
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
                                    if (index > 0) add(CustomAccessibilityAction(moveUpLabel) {
                                        onMove(task.id, -1)
                                        true
                                    })
                                    if (index < tasks.lastIndex) add(CustomAccessibilityAction(moveDownLabel) {
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
                                    Text(stringResource(R.string.edit))
                                }
                                TextButton(onClick = { pendingRemoval = task },
                                    modifier = Modifier.testTag("remove-" + task.id)) {
                                    Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error)
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

@Composable
private fun PlannedTask.scheduleSummary(): String {
    val cadence = when (schedule.kind) {
        ScheduleKind.ONCE -> stringResource(R.string.one_time)
        ScheduleKind.DAILY -> stringResource(R.string.routine_cadence,
            if (schedule.interval == 1) stringResource(R.string.daily) else stringResource(R.string.every_days, schedule.interval))
        ScheduleKind.WEEKLY_DAYS -> stringResource(R.string.routine_cadence,
            if (schedule.interval == 1) stringResource(R.string.weekly) else stringResource(R.string.every_weeks, schedule.interval))
        ScheduleKind.ONCE_PER_WEEK -> stringResource(R.string.until_decided_cadence,
            if (schedule.interval == 1) stringResource(R.string.every_week) else stringResource(R.string.every_weeks, schedule.interval))
    }
    val withDays = if (schedule.kind == ScheduleKind.WEEKLY_DAYS || schedule.kind == ScheduleKind.ONCE_PER_WEEK)
        stringResource(R.string.summary_join, cadence, schedule.days.daySelectionLabel()) else cadence
    return if (resolved) stringResource(R.string.summary_join, withDays, stringResource(R.string.resolved)) else withDays
}

@Composable
private fun Set<DayOfWeek>.daySelectionLabel(): String = when (this) {
    allWeekDays -> stringResource(R.string.any_day)
    weekdays -> stringResource(R.string.weekdays)
    setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> stringResource(R.string.weekend)
    else -> {
        val locale = LocalConfiguration.current.locales[0]
        DayOfWeek.entries.filter { it in this }.joinToString(", ") { it.getDisplayName(java.time.format.TextStyle.SHORT, locale) }
    }
}

@Composable
private fun localizedDate(date: LocalDate, style: FormatStyle): String =
    date.format(DateTimeFormatter.ofLocalizedDate(style).withLocale(LocalConfiguration.current.locales[0]))

@Composable
private fun quantity(id: Int, count: Int): String = LocalContext.current.resources.getQuantityString(id, count, count)

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
        TextButton(onClick = onCancel, modifier = Modifier.testTag("cancel-edit")) { Text(stringResource(R.string.cancel)) }
        PageHeader(stringResource(R.string.plan_text), if (task == null) stringResource(R.string.new_task) else stringResource(R.string.edit_task), stringResource(R.string.plan_editor_hint))
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.what_do_you_want_to_do)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("task-title"),
            )
            Text(stringResource(R.string.type), modifier = Modifier.padding(top = 14.dp), fontWeight = FontWeight.Bold)
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
                        label = { Text(stringResource(candidate.label)) },
                        modifier = Modifier.testTag("mode-" + candidate.name.lowercase()),
                    )
                }
            }
            Text(
                when (mode) {
                    PlanMode.ROUTINE -> stringResource(R.string.routine_explanation)
                    PlanMode.UNTIL_DECIDED -> stringResource(R.string.until_decided_explanation)
                    PlanMode.ONE_TIME -> stringResource(R.string.one_time_explanation)
                },
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f),
            )
            if (mode == PlanMode.ROUTINE) {
                Text(stringResource(R.string.schedule), modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
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
                            label = { Text(stringResource(if (candidate == ScheduleKind.DAILY) R.string.daily else R.string.weekly)) },
                            modifier = Modifier.testTag("repeat-" + candidate.name.lowercase()),
                        )
                    }
                }
            }
            if (mode != PlanMode.ONE_TIME) {
                Text(if (kind == ScheduleKind.DAILY) stringResource(R.string.every_how_many_days) else stringResource(R.string.every_how_many_weeks),
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
                                    contentDescription = context.getString(
                                        if (kind == ScheduleKind.DAILY) R.string.every_days else R.string.every_weeks, candidate)
                                },
                        )
                    }
                }
                if (interval !in intervalOptions) {
                    Text(if (kind == ScheduleKind.DAILY) stringResource(R.string.long_days_interval, interval)
                        else stringResource(R.string.long_weeks_interval, interval),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f))
                }
                if (kind != ScheduleKind.DAILY) {
                    Text(stringResource(R.string.days), modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(stringResource(R.string.any_day) to allWeekDays, stringResource(R.string.weekdays) to weekdays,
                            stringResource(R.string.weekend) to setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)).forEach { (label, selection) ->
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
                                label = { Text(day.getDisplayName(java.time.format.TextStyle.SHORT, LocalConfiguration.current.locales[0])) },
                                modifier = Modifier.testTag("day-" + day.name.lowercase()),
                            )
                        }
                    }
                    if (days.isEmpty()) Text(stringResource(R.string.select_at_least_one_day), color = MaterialTheme.colorScheme.error)
                }
                if (interval > 1) {
                    TextButton(onClick = {
                        DatePickerDialog(context, { _, year, month, day ->
                            startsOn = LocalDate.of(year, month + 1, day)
                        }, startsOn.year, startsOn.monthValue - 1, startsOn.dayOfMonth).show()
                    }, modifier = Modifier.testTag("repeat-start")) {
                        Text(stringResource(if (kind == ScheduleKind.DAILY) R.string.first_active_day else R.string.first_active_week,
                            localizedDate(startsOn, FormatStyle.MEDIUM)))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onSave(title, TaskSchedule(kind, interval, days, startsOn)) },
            enabled = title.isNotBlank() && (kind == ScheduleKind.ONCE || kind == ScheduleKind.DAILY || days.isNotEmpty()),
            modifier = Modifier.fillMaxWidth().testTag(if (task == null) "add-task" else "save-task"),
        ) { Text(if (task == null) stringResource(R.string.add_task) else stringResource(R.string.save_changes)) }
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
            PageHeader(stringResource(R.string.review_text), stringResource(R.string.your_recent_patterns), stringResource(R.string.review_intro), onAbout = onAbout)
        }
        item {
            ReviewMetricCard(
                title = stringResource(R.string.this_week),
                headline = if (review.week.closed + review.week.open == 0) stringResource(R.string.no_cards_yet) else
                    quantity(R.plurals.done_count, review.week.done),
                detail = listOf(quantity(R.plurals.skipped_count, review.week.skipped),
                    quantity(R.plurals.missed_count, review.week.missed),
                    quantity(R.plurals.still_open_count, review.week.open)).joinToString(" · "),
                tag = "review-week",
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("review-patterns"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.last_14_full_days), color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
                    ReviewPatternRow(stringResource(R.string.missed_most), review.mostMissed, stringResource(R.string.no_clear_pattern_yet), R.string.pattern_missed)
                    HorizontalDivider()
                    ReviewPatternRow(stringResource(R.string.most_often_skipped), review.mostSkipped, stringResource(R.string.none_recently), R.string.pattern_skipped)
                    HorizontalDivider()
                    ReviewPatternRow(stringResource(R.string.kept_up_with), review.mostDone, stringResource(R.string.not_enough_days), R.string.pattern_done)
                }
            }
        }
        item {
            val trend = review.trend
            ReviewMetricCard(
                title = stringResource(R.string.recent_direction),
                headline = if (trend == null) stringResource(R.string.not_enough_history_yet) else
                    stringResource(R.string.trend_compare, trend.recent.donePercent, trend.previous.donePercent),
                detail = if (trend == null) stringResource(R.string.trend_insufficient_cards) else
                    stringResource(R.string.trend_description),
                tag = "review-trend",
            )
        }
        item {
            Text(stringResource(R.string.recent_days), color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        if (review.days.isEmpty()) {
            item { Text(stringResource(R.string.recent_days_empty),
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
                            Text(localizedDate(date, FormatStyle.MEDIUM),
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(if (expanded) stringResource(R.string.hide) else stringResource(R.string.show),
                                color = MaterialTheme.colorScheme.primary)
                        }
                        val counts = cards.groupingBy { it.status }.eachCount()
                        Text(
                            (listOf(quantity(R.plurals.done_count, counts[CardStatus.DONE] ?: 0),
                                quantity(R.plurals.skipped_count, counts[CardStatus.DISMISSED] ?: 0),
                                quantity(R.plurals.missed_count, counts[CardStatus.MISSED] ?: 0)) +
                                if ((counts[CardStatus.PENDING] ?: 0) > 0) listOf(quantity(R.plurals.open_count, counts[CardStatus.PENDING] ?: 0)) else emptyList()).joinToString(" · "),
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
private fun ReviewPatternRow(label: String, pattern: ReviewPattern?, empty: String, measure: Int) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f), fontSize = 13.sp)
        Text(pattern?.title ?: empty, color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold)
        if (pattern != null) Text(
            stringResource(R.string.pattern_count, pattern.matching, pattern.total, stringResource(measure)),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f), fontSize = 13.sp,
        )
    }
}

@Composable
private fun CardStatus.reviewLabel() = when (this) {
    CardStatus.PENDING -> stringResource(R.string.open)
    CardStatus.DONE -> stringResource(R.string.done)
    CardStatus.DISMISSED -> stringResource(R.string.skipped)
    CardStatus.MISSED -> stringResource(R.string.missed)
}

@Composable
private fun SkipScope.localizedLabel(): String = stringResource(when (this) {
    SkipScope.TODAY -> R.string.skip_today
    SkipScope.WEEK -> R.string.skip_this_week
    SkipScope.TASK -> R.string.skip_task
})

@Composable
private fun SwipeableTaskCard(card: TaskCard, onCommand: (CardCommand) -> Unit) {
    val skipLabel = card.skipScope.localizedLabel()
    val markDoneLabel = stringResource(R.string.mark_done)
    var offsetX by remember(card.id) { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val currentOnCommand by rememberUpdatedState(onCommand)
    var cardWidth by remember(card.id) { mutableFloatStateOf(0f) }
    val progress = if (cardWidth == 0f) 0f else (offsetX / (cardWidth * .28f)).coerceIn(-1f, 1f)
    Box(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.done), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, modifier = Modifier.alpha(progress.coerceAtLeast(0f)))
            Text(skipLabel.uppercase(LocalConfiguration.current.locales[0]), color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black, modifier = Modifier.alpha((-progress).coerceAtLeast(0f)))
        }
        TaskCardView(
            card,
            Modifier.testTag("card-${card.id}")
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(markDoneLabel) { currentOnCommand(CardCommand.Complete(card.id)); true },
                        CustomAccessibilityAction(skipLabel) { currentOnCommand(CardCommand.Dismiss(card.id)); true },
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
    val skipLabel = card.skipScope.localizedLabel()
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
            Text(stringResource(R.string.card_slide_hint, skipLabel.replaceFirstChar { it.lowercase() }),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun TaskCard.planLabel(): String = when {
    type == TaskType.ONE_TIME -> stringResource(R.string.one_time)
    skipScope == SkipScope.WEEK -> stringResource(R.string.until_decided)
    else -> stringResource(R.string.routine)
}

@Composable
private fun EmptyBoard() {
    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)).padding(28.dp)) {
        Column {
            Text(stringResource(R.string.all_clear), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.the_present_is_handled), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f), fontSize = 17.sp)
        }
    }
}
