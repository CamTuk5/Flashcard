package com.example.kiemtrack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kiemtrack.model.Flashcard
import com.example.kiemtrack.srs.SM2Logic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    viewModel: FlashcardViewModel = viewModel(),
    ttsHelper: TtsHelper,
    onFinish: () -> Unit
) {
    val dueCards by viewModel.getDueCards(System.currentTimeMillis()).collectAsState(initial = emptyList())
    var currentIndex by remember { mutableStateOf(0) }
    var showBack by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ôn tập") },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        if (dueCards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Không có thẻ nào cần học hôm nay!")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onFinish) {
                        Text("Quay lại")
                    }
                }
            }
        } else if (currentIndex < dueCards.size) {
            val currentCard = dueCards[currentIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Thẻ ${currentIndex + 1} / ${dueCards.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (showBack) currentCard.back else currentCard.front,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!showBack) {
                    Button(onClick = { 
                        showBack = true
                        ttsHelper.speak(currentCard.front)
                    }) {
                        Text("Hiện mặt sau")
                    }
                } else {
                    Text("Đánh giá độ khó:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        (0..5).forEach { quality ->
                            Button(
                                onClick = {
                                    viewModel.updateFlashcardQuality(currentCard, quality)
                                    showBack = false
                                    currentIndex++
                                },
                                modifier = Modifier.weight(1f).padding(2.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(quality.toString())
                            }
                        }
                    }
                    if (currentIndex >= dueCards.size) {
                        LaunchedEffect(Unit) {
                            onFinish()
                        }
                    }
                }
            }
        } else {
            // Trường hợp hoàn thành
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Chúc mừng! Bạn đã hoàn thành bài học.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onFinish) {
                        Text("Xong")
                    }
                }
            }
        }
    }
}
