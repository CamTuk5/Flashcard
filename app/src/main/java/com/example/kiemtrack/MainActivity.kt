package com.example.kiemtrack

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.*
import com.example.kiemtrack.model.Flashcard
import com.example.kiemtrack.ui.FlashcardViewModel
import com.example.kiemtrack.ui.StudyScreen
import com.example.kiemtrack.ui.TtsHelper
import com.example.kiemtrack.ui.theme.KiemTraCkTheme
import com.example.kiemtrack.worker.ReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private var ttsHelper: TtsHelper? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        ttsHelper = TtsHelper(this)
        scheduleDailyReminder()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            KiemTraCkTheme {
                val navController = rememberNavController()
                val viewModel: FlashcardViewModel = viewModel()

                NavHost(navController = navController, startDestination = "category_list") {
                    // Màn hình 1: Danh sách các chủ đề
                    composable("category_list") {
                        CategoryListScreen(
                            viewModel = viewModel,
                            onCategoryClick = { categoryName ->
                                navController.navigate("flashcard_list/$categoryName")
                            },
                            onAddCard = { navController.navigate("add") }
                        )
                    }
                    
                    // Màn hình 2: Danh sách từ vựng trong một chủ đề
                    composable(
                        "flashcard_list/{categoryName}",
                        arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                        FlashcardListScreen(
                            categoryName = categoryName,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onStartStudy = { navController.navigate("study/$categoryName") }
                        )
                    }

                    // Màn hình 3: Ôn tập
                    composable(
                        "study/{categoryName}",
                        arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val categoryName = backStackEntry.arguments?.getString("categoryName") ?: "All"
                        StudyScreen(
                            viewModel = viewModel,
                            ttsHelper = ttsHelper ?: TtsHelper(this@MainActivity),
                            onFinish = { navController.popBackStack() }
                        )
                    }

                    // Màn hình 4: Thêm từ mới
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
        dueDate.set(Calendar.HOUR_OF_DAY, 16)
        dueDate.set(Calendar.MINUTE, 22)
        dueDate.set(Calendar.SECOND, 0)

        val diff = currentDate.timeInMillis - dueDate.timeInMillis
        if (diff > 0 && diff > 30 * 60 * 1000) {
            dueDate.add(Calendar.DAY_OF_YEAR, 1)
        }

        var initialDelay = dueDate.timeInMillis - currentDate.timeInMillis
        if (initialDelay < 0) initialDelay = 0

        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    viewModel: FlashcardViewModel,
    onCategoryClick: (String) -> Unit,
    onAddCard: () -> Unit
) {
    val allCards by viewModel.allCards.collectAsState(initial = emptyList())
    val categories = allCards.map { it.courseId.ifEmpty { "Chung" } }.distinct().sorted()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Chủ đề học tập", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCard, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            items(categories) { category ->
                val cardCount = allCards.count { (it.courseId.ifEmpty { "Chung" }) == category }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onCategoryClick(category) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    ListItem(
                        headlineContent = { Text(category, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("$cardCount từ vựng") },
                        trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardListScreen(
    categoryName: String,
    viewModel: FlashcardViewModel,
    onBack: () -> Unit,
    onStartStudy: () -> Unit
) {
    val allCards by viewModel.allCards.collectAsState(initial = emptyList())
    val categoryCards = allCards.filter { (it.courseId.ifEmpty { "Chung" }) == categoryName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Button(
                onClick = onStartStudy,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Bắt đầu ôn tập chủ đề này", style = MaterialTheme.typography.titleMedium)
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoryCards) { card ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        ListItem(
                            headlineContent = { Text(card.front, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(card.back) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.deleteFlashcard(card) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                }
                            }
                        )
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
    var category by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Thêm từ mới", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Chủ đề (VD: Động vật, Du lịch...)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                placeholder = { Text("Mặc định là 'Chung'") }
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = front,
                onValueChange = { front = it },
                label = { Text("Từ vựng (Tiếng Anh)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = back,
                onValueChange = { back = it },
                label = { Text("Nghĩa (Tiếng Việt)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    if (front.isNotBlank() && back.isNotBlank()) {
                        viewModel.addFlashcard(front.trim(), back.trim(), category.trim().ifEmpty { "Chung" })
                        onFinish()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = front.isNotBlank() && back.isNotBlank()
            ) {
                Text("Lưu vào bộ từ vựng")
            }
        }
    }
}
