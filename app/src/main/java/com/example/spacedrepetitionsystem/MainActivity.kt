package com.example.spacedrepetitionsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.spacedrepetitionsystem.data.model.Flashcard
import com.example.spacedrepetitionsystem.ui.MainViewModel
import com.example.spacedrepetitionsystem.ui.theme.SpacedRepetitionSystemTheme
import com.example.spacedrepetitionsystem.ui.utils.TtsHelper
import com.example.spacedrepetitionsystem.work.ReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpacedRepetitionSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel(), modifier: Modifier = Modifier) {
    val reviewCards by viewModel.cardsToReview.collectAsState()
    val allCards by viewModel.allCards.collectAsState()
    val context = LocalContext.current
    
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(24, TimeUnit.HOURS)
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
        viewModel.syncWithBackend()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Flashcard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Còn lại: ${reviewCards.size} thẻ", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { viewModel.syncWithBackend() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Study Area
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = reviewCards.firstOrNull(),
                    transitionSpec = {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    },
                    label = "CardTransition"
                ) { currentCard ->
                    if (currentCard != null) {
                        key(currentCard.id) {
                            FlashcardStudyView(
                                card = currentCard,
                                onReview = { quality -> viewModel.reviewCard(currentCard, quality) }
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎉", fontSize = 64.sp)
                            Text("Tuyệt vời! Bạn đã hoàn thành bài học.", textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Bộ sưu tập (${allCards.size})", fontWeight = FontWeight.Bold)
            LazyColumn(modifier = Modifier.height(150.dp)) {
                items(allCards) { card ->
                    Text("• ${card.front}", fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Card")
        }

        if (showAddDialog) {
            AddCardDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { front, back -> 
                    viewModel.addCard(front, back)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun FlashcardStudyView(card: Flashcard, onReview: (Int) -> Unit) {
    val context = LocalContext.current
    val ttsHelper = remember { TtsHelper(context) }
    var isRevealed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { ttsHelper.shutdown() } }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .clickable { ttsHelper.speak(card.front) },
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = card.front,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isRevealed) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
                        Text(
                            text = card.back,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Chạm để nghe phát âm", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (!isRevealed) {
            Button(
                onClick = { isRevealed = true },
                modifier = Modifier.fillMaxWidth(0.7f).height(56.dp)
            ) {
                Text("Hiện đáp án")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ReviewButton("Làm lại", "5s", Color.Red) { onReview(0) }
                ReviewButton("Tạm được", "30s", Color.Gray) { onReview(3) }
                ReviewButton("Rất tốt", "1m", Color(0xFF4CAF50)) { onReview(5) }
            }
        }
    }
}

@Composable
fun ReviewButton(label: String, time: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier.width(110.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 12.sp)
            Text(time, fontSize = 10.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
fun AddCardDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm thẻ mới") },
        text = {
            Column {
                OutlinedTextField(value = front, onValueChange = { front = it }, label = { Text("Mặt trước") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = back, onValueChange = { back = it }, label = { Text("Mặt sau") })
            }
        },
        confirmButton = {
            Button(onClick = { if(front.isNotBlank() && back.isNotBlank()) onAdd(front, back) }) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
