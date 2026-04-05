package com.example.kiemtrack

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private var ttsHelper: TtsHelper? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handled if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize TTS lazily or handle it in a way that doesn't block startup
        ttsHelper = TtsHelper(this)
        
        scheduleDailyReminder()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            KiemTraCkTheme {
                val navController = rememberNavController()
                // The viewModel is scoped to the NavHost or Activity, initialized lazily on first access
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
                            ttsHelper = ttsHelper ?: TtsHelper(this@MainActivity),
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
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()

        // Thiết lập thời gian thông báo vào 9:00 AM
        dueDate.set(Calendar.HOUR_OF_DAY, 9)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)

        // Nếu đã qua 9:00 AM thì hẹn vào sáng mai
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderRequest
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsHelper?.shutdown()
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
                item {
                    val allDueCardsCount = allCards.count { it.nextReviewDate <= System.currentTimeMillis() }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onStartStudy("All") },
                        colors = CardDefaults.cardColors(
                            containerColor = if (allDueCardsCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        ListItem(
                            headlineContent = { Text("Tất cả thẻ", style = MaterialTheme.typography.titleMedium) },
                            supportingContent = { 
                                Column {
                                    Text("Tổng số: ${allCards.size} thẻ")
                                    if (allDueCardsCount > 0) {
                                        Text("Cần ôn tập ngay: $allDueCardsCount thẻ", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    } else {
                                        Text("Tuyệt vời! Bạn đã học hết thẻ hôm nay", color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }

                items(courses) { course ->
                    if (course != "All") {
                        val cardsInCourse = allCards.filter { it.courseId == course }
                        val dueInCourse = cardsInCourse.count { it.nextReviewDate <= System.currentTimeMillis() }
                        val isCompleted = cardsInCourse.isNotEmpty() && dueInCourse == 0
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
                                        if (dueInCourse > 0) {
                                            Text("Cần ôn tập: $dueInCourse thẻ", color = MaterialTheme.colorScheme.error)
                                        }
                                        if (lastReview > 0L) {
                                            Text("Lần cuối: ${dateFormat.format(Date(lastReview))}", style = MaterialTheme.typography.bodySmall)
                                        }
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
