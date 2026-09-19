package com.wastingnotime.slidingtasks

import android.os.Bundle
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wastingnotime.slidingtasks.model.CardCommand
import com.wastingnotime.slidingtasks.model.TodayBoard
import com.wastingnotime.slidingtasks.model.TodayCard
import com.wastingnotime.slidingtasks.model.SlideDecision
import com.wastingnotime.slidingtasks.model.after
import com.wastingnotime.slidingtasks.model.slideDecision
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SlidingTasksApp() }
    }
}

private val Ink = Color(0xFF231F20)
private val Paper = Color(0xFFFFF8F3)
private val Accent = Color(0xFFE85D3F)
private val SoftGreen = Color(0xFFDDE9D7)

@Composable
fun SlidingTasksApp() {
    var board by remember { mutableStateOf(sampleBoard()) }

    MaterialTheme {
        Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
            TodayScreen(board = board, onCommand = { board = board.after(it) })
        }
    }
}

@Composable
private fun TodayScreen(board: TodayBoard, onCommand: (CardCommand) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 48.dp),
    ) {
        Text("TODAY", color = Accent, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text(board.dateLabel, color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text(
            "${board.cards.size} ${if (board.cards.size == 1) "card" else "cards"} left",
            modifier = Modifier.testTag("remaining-count"),
            color = Ink.copy(alpha = .58f),
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(28.dp))

        if (board.cards.isEmpty()) {
            EmptyBoard()
        } else {
            Text(
                "Slide left for not today · right for done",
                color = Ink.copy(alpha = .55f),
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(14.dp))
            SlidingCardStack(cards = board.cards, onCommand = onCommand)
        }
    }
}

@Composable
private fun SlidingCardStack(cards: List<TodayCard>, onCommand: (CardCommand) -> Unit) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxWidth(),
    ) {
        cards.getOrNull(2)?.let { card ->
            TaskCard(
                card = card,
                onCommand = onCommand,
                modifier = Modifier
                    .padding(top = 24.dp, start = 20.dp, end = 20.dp)
                    .alpha(.42f),
                interactive = false,
            )
        }
        cards.getOrNull(1)?.let { card ->
            TaskCard(
                card = card,
                onCommand = onCommand,
                modifier = Modifier
                    .padding(top = 12.dp, start = 10.dp, end = 10.dp)
                    .alpha(.72f),
                interactive = false,
            )
        }
        SwipeableTaskCard(card = cards.first(), onCommand = onCommand)
    }
}

@Composable
private fun SwipeableTaskCard(card: TodayCard, onCommand: (CardCommand) -> Unit) {
    var offsetX by remember(card.id) { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var cardWidth by remember(card.id) { mutableStateOf(0f) }
    val progress = if (cardWidth == 0f) 0f else (offsetX / (cardWidth * .28f)).coerceIn(-1f, 1f)

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "NOT TODAY",
                color = Accent,
                fontWeight = FontWeight.Black,
                modifier = Modifier.alpha((-progress).coerceAtLeast(0f)),
            )
            Text(
                "DONE",
                color = Color(0xFF377A45),
                fontWeight = FontWeight.Black,
                modifier = Modifier.alpha(progress.coerceAtLeast(0f)),
            )
        }

        TaskCard(
            card = card,
            onCommand = onCommand,
            modifier = Modifier
                .testTag("active-card")
                .onSizeChanged { cardWidth = it.width.toFloat() }
                .graphicsLayer {
                    translationX = offsetX
                    rotationZ = progress * 5f
                }
                .pointerInput(card.id, cardWidth) {
                    detectHorizontalDragGestures(
                        onDragStart = { onCommand(CardCommand.Touch(card.id)) },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount
                        },
                        onDragCancel = {
                            scope.launch {
                                animate(offsetX, 0f, animationSpec = tween(180)) { value, _ ->
                                    offsetX = value
                                }
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                when (slideDecision(offsetX, cardWidth)) {
                                    SlideDecision.COMPLETE -> {
                                        animate(offsetX, cardWidth * 1.25f, animationSpec = tween(180)) { value, _ ->
                                            offsetX = value
                                        }
                                        onCommand(CardCommand.Complete(card.id))
                                    }
                                    SlideDecision.DISMISS -> {
                                        animate(offsetX, -cardWidth * 1.25f, animationSpec = tween(180)) { value, _ ->
                                            offsetX = value
                                        }
                                        onCommand(CardCommand.Dismiss(card.id))
                                    }
                                    null -> animate(offsetX, 0f, animationSpec = tween(180)) { value, _ ->
                                        offsetX = value
                                    }
                                }
                            }
                        },
                    )
                },
        )
    }
}

@Composable
private fun TaskCard(
    card: TodayCard,
    onCommand: (CardCommand) -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(Accent, RoundedCornerShape(50)),
                )
                Text(
                    card.typeLabel.uppercase(),
                    modifier = Modifier.padding(start = 8.dp),
                    color = Ink.copy(alpha = .55f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                )
            }
            Text(
                card.title,
                modifier = Modifier.padding(vertical = 18.dp),
                color = Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (interactive) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { onCommand(CardCommand.Dismiss(card.id)) }) {
                        Text("Not today", color = Ink)
                    }
                    Button(
                        onClick = { onCommand(CardCommand.Complete(card.id)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Ink),
                    ) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun EmptyBoard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftGreen, RoundedCornerShape(24.dp))
            .padding(28.dp),
    ) {
        Column {
            Text("All clear.", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("The present is handled.", color = Ink.copy(alpha = .65f), fontSize = 17.sp)
        }
    }
}

private fun sampleBoard() = TodayBoard(
    dateLabel = "Friday, September 18",
    cards = listOf(
        TodayCard("card-1", "Write the project brief", "Focus"),
        TodayCard("card-2", "Walk for twenty minutes", "Routine"),
        TodayCard("card-3", "Call the dentist", "One-time"),
    ),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TodayPreview() = SlidingTasksApp()
