package com.example.kiemtrack.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kiemtrack.model.Flashcard
import com.example.kiemtrack.srs.SM2Logic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    viewModel: FlashcardViewModel = viewModel(),
    ttsHelper: TtsHelper,
    categoryName: String? = null,
    onFinish: () -> Unit
) {
    val currentTime = remember { System.currentTimeMillis() }
    
    // SỬA: Lọc thẻ theo chủ đề (categoryName) nếu có
    val dueCards by if (categoryName == null || categoryName == "All") {
        viewModel.getDueCards(currentTime)
    } else {
        viewModel.getDueCardsByCourse(categoryName, currentTime)
    }.collectAsState(initial = emptyList())
    
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFrontRevealed by remember { mutableStateOf(false) }
    var isFlippedToBack by remember { mutableStateOf(false) }
    var showReviewSummary by remember { mutableStateOf(false) }
    var lastReviewedCard by remember { mutableStateOf<Flashcard?>(null) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlippedToBack) 180f else 0f,
        animationSpec = tween(durationMillis = 500), label = "cardRotation"
    )

    val currentCard = dueCards.getOrNull(currentIndex)
    LaunchedEffect(currentCard?.id) {
        isFrontRevealed = false
        isFlippedToBack = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (categoryName == null || categoryName == "All") "Ôn tập tổng hợp" else "Ôn tập: $categoryName",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            if (dueCards.isEmpty()) {
                EmptyState(onFinish)
            } else if (showReviewSummary && lastReviewedCard != null) {
                ReviewSummaryState(
                    card = lastReviewedCard!!,
                    onNext = {
                        showReviewSummary = false
                        isFrontRevealed = false
                        isFlippedToBack = false
                    },
                    onDelete = {
                        viewModel.deleteFlashcard(lastReviewedCard!!)
                        showReviewSummary = false
                    }
                )
            } else if (currentIndex < dueCards.size) {
                StudyContent(
                    total = dueCards.size,
                    index = currentIndex,
                    card = dueCards[currentIndex],
                    rotation = rotation,
                    isFrontRevealed = isFrontRevealed,
                    isFlippedToBack = isFlippedToBack,
                    onRevealFront = {
                        isFrontRevealed = true
                        ttsHelper.speak(dueCards[currentIndex].front)
                    },
                    onFlip = { isFlippedToBack = true },
                    onQualitySelected = { quality ->
                        val cardToUpdate = dueCards[currentIndex]
                        val updated = SM2Logic.calculateNextReview(cardToUpdate, quality)
                        viewModel.updateFlashcardQuality(cardToUpdate, quality)
                        lastReviewedCard = updated
                        showReviewSummary = true
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyState(onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle, 
                contentDescription = null, 
                modifier = Modifier.size(64.dp), 
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Hoàn thành mục tiêu!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Bạn đã hoàn thành hết các thẻ cần ôn tập cho chủ đề này.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Quay lại màn hình chính")
        }
    }
}

@Composable
fun ReviewSummaryState(card: Flashcard, onNext: () -> Unit, onDelete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Đã ghi nhận!", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lần ôn tập tới vào:", style = MaterialTheme.typography.labelLarge)
                val dateFormat = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
                Text(
                    dateFormat.format(Date(card.nextReviewDate)),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Từ tiếp theo", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        TextButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Xóa từ này", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun StudyContent(
    total: Int,
    index: Int,
    card: Flashcard,
    rotation: Float,
    isFrontRevealed: Boolean,
    isFlippedToBack: Boolean,
    onRevealFront: () -> Unit,
    onFlip: () -> Unit,
    onQualitySelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LinearProgressIndicator(
                progress = { (index + 1).toFloat() / total },
                modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "${index + 1}/$total",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12 * density
                }
                .clickable(enabled = !isFlippedToBack) {
                    if (!isFrontRevealed) onRevealFront() else onFlip()
                },
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                if (rotation <= 90f) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!isFrontRevealed) {
                            Icon(
                                Icons.Default.Refresh, 
                                contentDescription = null, 
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Chạm để xem từ vựng",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        } else {
                            Text(
                                text = card.front,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Chạm để xem nghĩa",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.graphicsLayer { rotationY = 180f },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Ý nghĩa",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = card.back,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (!isFlippedToBack) {
            Button(
                onClick = { if (!isFrontRevealed) onRevealFront() else onFlip() },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Text(
                    if (!isFrontRevealed) "Xem từ vựng" else "Xem nghĩa",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Bạn nhớ từ này mức độ nào?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernQualityButton(Modifier.weight(1f), "Khó", Color(0xFFEF4444)) { onQualitySelected(0) }
                    ModernQualityButton(Modifier.weight(1f), "Vừa", Color(0xFFF59E0B)) { onQualitySelected(3) }
                    ModernQualityButton(Modifier.weight(1f), "Dễ", Color(0xFF10B981)) { onQualitySelected(5) }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ModernQualityButton(modifier: Modifier, label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
