package com.example.kiemtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.*
import com.example.kiemtrack.ui.FlashcardViewModel
import com.example.kiemtrack.ui.StudyScreen
import com.example.kiemtrack.ui.TtsHelper
import com.example.kiemtrack.ui.theme.KiemTraCkTheme
import com.example.kiemtrack.worker.ReminderWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var ttsHelper: TtsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ttsHelper = TtsHelper(this)
        
        scheduleDailyReminder()

        setContent {
            KiemTraCkTheme {
                val navController = rememberNavController()
                val viewModel: FlashcardViewModel = viewModel()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel,
                            onStartStudy = { courseId -> 
                                navController.navigate("study/$courseId") 
                            },
                            onAddCard = { navController.navigate("add") }
                        )
                    }
                    composable(
                        route = "study/{courseId}",
                        arguments = listOf(navArgument("courseId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val courseId = backStackEntry.arguments?.getString("courseId")
                        StudyScreen(
                            viewModel = viewModel,
                            ttsHelper = ttsHelper,
                            courseId = courseId,
                            onFinish = { navController.popBackStack() }
                        )
                    }
                    composable("add") {
                        AddCardScreen(
                            viewModel = viewModel,
                            onFinish = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    private fun scheduleDailyReminder() {
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsHelper.shutdown()
    }
}

@Composable
fun HomeScreen(
    viewModel: FlashcardViewModel, 
    onStartStudy: (String) -> Unit,
    onAddCard: () -> Unit
) {
    val allCards by viewModel.allCards.collectAsState(initial = emptyList())
    val courses by viewModel.getAllCourses().collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCard) {
                Icon(Icons.Default.Add, contentDescription = "Thêm thẻ")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Học qua Flashcard", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Danh sách bài học:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Khóa học "Tất cả thẻ" - CHỈ HIỆN TÊN VÀ TỔNG SỐ
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onStartStudy("All") },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        ListItem(
                            headlineContent = { Text("Tất cả thẻ", style = MaterialTheme.typography.titleMedium) },
                            supportingContent = { Text("Tổng số: ${allCards.size} thẻ") },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }

                // Từng khóa học cụ thể - HIỆN THỜI GIAN VÀ TRẠNG THÁI
                items(courses) { course ->
                    if (course != "All") {
                        val cardsInCourse = allCards.filter { it.courseId == course }
                        val isCompleted = cardsInCourse.isNotEmpty() && cardsInCourse.all { it.nextReviewDate > System.currentTimeMillis() }
                        val lastReview = cardsInCourse.map { it.lastReviewDate }.maxOrNull() ?: 0L

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onStartStudy(course) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCompleted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            ListItem(
                                headlineContent = { Text(course, style = MaterialTheme.typography.titleMedium) },
                                supportingContent = { 
                                    Column {
                                        Text("Tổng số: ${cardsInCourse.size} thẻ")
                                        if (lastReview > 0L) {
                                            Text("Học xong lúc: ${dateFormat.format(Date(lastReview))}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(
                                            if (isCompleted) "Trạng thái: Đã hoàn thành" else "Trạng thái: Chưa hoàn thành",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                trailingContent = {
                                    Row {
                                        Icon(
                                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(onClick = { viewModel.deleteCourse(course) }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(viewModel: FlashcardViewModel, onFinish: () -> Unit) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var courseId by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo thẻ mới") },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = courseId,
                onValueChange = { courseId = it },
                label = { Text("Tên khóa học (VD: Tiếng Anh)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nhập tên nhóm/khóa học") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = front,
                onValueChange = { front = it },
                label = { Text("Mặt trước (Từ vựng)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = back,
                onValueChange = { back = it },
                label = { Text("Mặt sau (Kết quả/Nghĩa)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (front.isNotBlank() && back.isNotBlank() && courseId.isNotBlank()) {
                        viewModel.addFlashcard(front, back, courseId)
                        onFinish()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Lưu vào khóa học")
            }
        }
    }
}
