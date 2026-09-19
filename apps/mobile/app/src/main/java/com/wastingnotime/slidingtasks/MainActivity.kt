package com.wastingnotime.slidingtasks

import android.os.Bundle
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wastingnotime.slidingtasks.model.CardCommand
import com.wastingnotime.slidingtasks.model.TodayBoard
import com.wastingnotime.slidingtasks.model.TodayCard
import com.wastingnotime.slidingtasks.model.after

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
            color = Ink.copy(alpha = .58f),
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(28.dp))

        if (board.cards.isEmpty()) {
            EmptyBoard()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(board.cards, key = { it.id }) { card ->
                    TaskCard(card = card, onCommand = onCommand)
                }
            }
        }
    }
}

@Composable
private fun TaskCard(card: TodayCard, onCommand: (CardCommand) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onCommand(CardCommand.Complete(card.id)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink),
                ) { Text("Done") }
                OutlinedButton(onClick = { onCommand(CardCommand.Dismiss(card.id)) }) {
                    Text("Not today", color = Ink)
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
