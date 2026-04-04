package com.example.kiemtrack.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kiemtrack.model.Flashcard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    viewModel: FlashcardViewModel = viewModel(),
    ttsHelper: TtsHelper,
    courseId: String? = null,
    onFinish: () -> Unit
) {
    val allCards by viewModel.allCards.collectAsState(initial = emptyList())
    
    val courseCards = remember(allCards, courseId) {
        if (courseId != null && courseId != "All") {
            allCards.filter { it.courseId == courseId }
        } else {
            allCards
        }
    }

    var currentIndex by remember { mutableStateOf(0) }
    var isFrontRevealed by remember { mutableStateOf(false) }
    var isFlippedToBack by remember { mutableStateOf(false) }
    var showReviewSummary by remember { mutableStateOf(false) }
    var lastReviewedCard by remember { mutableStateOf<Flashcard?>(null) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlippedToBack) 180f else 0f,
        animationSpec = tween(durationMillis = 500)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đang học: ${courseId ?: "Tất cả"}") },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        if (courseCards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Chưa có thẻ nào!")
                    Button(onClick = onFinish, modifier = Modifier.padding(top = 16.dp)) { Text("Quay lại") }
                }
            }
        } else if (showReviewSummary && lastReviewedCard != null) {
            // MÀN HÌNH SAU KHI ĐÁNH GIÁ XONG 1 THẺ
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text("Đã ghi nhận kết quả!", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Ngày ôn tập tiếp theo: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(lastReviewedCard!!.nextReviewDate))}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row {
                        Button(onClick = {
                            showReviewSummary = false
                            if (currentIndex >= courseCards.size) {
                                onFinish()
                            }
                        }) {
                            Text(if (currentIndex < courseCards.size) "Thẻ tiếp theo" else "Hoàn thành")
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteFlashcard(lastReviewedCard!!)
                                showReviewSummary = false
                                if (currentIndex >= courseCards.size) {
                                    onFinish()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Text(" Xóa thẻ này")
                        }
                    }
                }
            }
        } else if (currentIndex < courseCards.size) {
            val currentCard = courseCards[currentIndex]

            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Thẻ số ${currentIndex + 1} / ${courseCards.size}", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12 * density
                        }
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    onClick = { 
                        if (!isFrontRevealed) {
                            isFrontRevealed = true
                            ttsHelper.speak(currentCard.front)
                        } else if (!isFlippedToBack) {
                            isFlippedToBack = true
                        }
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (rotation <= 90f) {
                            Text(
                                text = if (!isFrontRevealed) "Bấm để xem từ vựng" else currentCard.front,
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = if (!isFrontRevealed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = currentCard.back,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.graphicsLayer { rotationY = 180f },
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!isFrontRevealed) {
                    Button(onClick = { 
                        isFrontRevealed = true 
                        ttsHelper.speak(currentCard.front)
                    }) { Text("Xem từ vựng") }
                } else if (!isFlippedToBack) {
                    Button(onClick = { isFlippedToBack = true }) { Text("Xem nghĩa (Lật thẻ)") }
                } else {
                    Text("Đánh giá độ khó:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        (0..5).forEach { quality ->
                            Button(
                                onClick = {
                                    // Tính toán và cập nhật logic SM-2
                                    val updatedCard = com.example.kiemtrack.srs.SM2Logic.calculateNextReview(currentCard, quality)
                                    viewModel.updateFlashcardQuality(currentCard, quality)
                                    
                                    // Hiển thị tóm tắt và lưu lại thẻ vừa học để xoá nếu cần
                                    lastReviewedCard = updatedCard
                                    showReviewSummary = true
                                    
                                    // Reset cho thẻ thật sự tiếp theo (sau summary)
                                    isFrontRevealed = false
                                    isFlippedToBack = false
                                    currentIndex++
                                },
                                modifier = Modifier.weight(1f).padding(2.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text(quality.toString()) }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉 Hoàn thành bài học!")
                    Button(onClick = onFinish, modifier = Modifier.padding(top = 16.dp)) { Text("Xong") }
                }
            }
        }
    }
}
