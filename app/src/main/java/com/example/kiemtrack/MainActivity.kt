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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToDecks = { navController.navigate("category_list") },
                            onNavigateToFlagNotes = { navController.navigate("flag_notes") },
                            onNavigateToCreate = { navController.navigate("add") }
                        )
                    }

                    composable("category_list") {
                        CategoryListScreen(
                            viewModel = viewModel,
                            onCategoryClick = { categoryName ->
                                navController.navigate("flashcard_list/$categoryName")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("flag_notes") {
                        FlagNotesScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
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

                    composable(
                        "study/{categoryName}",
                        arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val categoryName = backStackEntry.arguments?.getString("categoryName") ?: "All"
                        StudyScreen(
                            viewModel = viewModel,
                            ttsHelper = ttsHelper ?: TtsHelper(this@MainActivity),
                            categoryName = categoryName,
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
        dueDate.set(Calendar.HOUR_OF_DAY, 16)
        dueDate.set(Calendar.MINUTE, 22)
        dueDate.set(Calendar.SECOND, 0)

        if (dueDate.before(currentDate)) {
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
fun DashboardScreen(
    viewModel: FlashcardViewModel,
    onNavigateToDecks: () -> Unit,
    onNavigateToFlagNotes: () -> Unit,
    onNavigateToCreate: () -> Unit
) {
    val allCards by viewModel.allCards.collectAsState(initial = emptyList())
    
    val dueToday = allCards.count { it.nextReviewDate <= System.currentTimeMillis() }
    val totalLearned = allCards.size
    
    val streak = if (allCards.any { it.lastReviewDate > 0 && System.currentTimeMillis() - it.lastReviewDate < 24 * 60 * 60 * 1000 }) 5 else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F7FB))
        ) {
            // Lời chào
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    "Chào mừng bạn quay lại! 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    "Hôm nay bạn muốn học gì nào?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Thống kê học tập
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatSmallCard(Modifier.weight(1f), "Hôm nay", "$dueToday thẻ", Color(0xFF6366F1))
                StatSmallCard(Modifier.weight(1f), "Streak", "$streak ngày", Color(0xFFF59E0B))
                StatSmallCard(Modifier.weight(1f), "Đã học", "$totalLearned từ", Color(0xFF10B981))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Các thẻ chức năng (LazyRow)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    DashboardLargeCard(
                        title = "Bộ thẻ\n(Decks)",
                        icon = Icons.Default.LibraryBooks,
                        color = Color(0xFF00B4D8),
                        onClick = onNavigateToDecks
                    )
                }
                item {
                    DashboardLargeCard(
                        title = "Đánh dấu\n(Flag Notes)",
                        icon = Icons.Default.Flag,
                        color = Color(0xFFFFB703),
                        onClick = onNavigateToFlagNotes
                    )
                }
                item {
                    DashboardLargeCard(
                        title = "Tạo mới\n(Create)",
                        icon = Icons.Default.AddCircle,
                        color = Color(0xFFFB8500),
                        onClick = onNavigateToCreate
                    )
                }
            }
        }
    }
}

@Composable
fun StatSmallCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun DashboardLargeCard(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .height(320.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    tint = Color.White
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlagNotesScreen(viewModel: FlashcardViewModel, onBack: () -> Unit) {
    val allCards by viewModel.allCards.collectAsState(initial = emptyList())
    
    val easyCards = allCards.filter { it.lastQuality == 5 }
    val mediumCards = allCards.filter { it.lastQuality == 3 }
    val hardCards = allCards.filter { it.lastQuality == 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ghi chú đánh dấu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { FlagSection("Khó (Hard)", hardCards, Color.Red) }
            item { FlagSection("Vừa (Medium)", mediumCards, Color(0xFFFFB703)) }
            item { FlagSection("Dễ (Easy)", easyCards, Color(0xFF10B981)) }
        }
    }
}

@Composable
fun FlagSection(title: String, cards: List<Flashcard>, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Flag, contentDescription = null, tint = color)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(8.dp))
        if (cards.isEmpty()) {
            Text("Không có thẻ nào ở mức độ này", color = Color.Gray, modifier = Modifier.padding(start = 32.dp))
        } else {
            cards.forEach { card ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
                ) {
                    ListItem(
                        headlineContent = { Text(card.front, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(card.back) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    viewModel: FlashcardViewModel,
    onCategoryClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val allCards by viewModel.allCards.collectAsState(initial = emptyList())
    val categories = allCards.map { it.courseId.ifEmpty { "Chung" } }.distinct().sorted()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bộ thẻ của tôi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            items(categories) { category ->
                val cardCount = allCards.count { (it.courseId.ifEmpty { "Chung" }) == category }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onCategoryClick(category) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    ListItem(
                        headlineContent = { Text(category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("$cardCount thẻ") },
                        trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) }
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
                Text("Học bộ thẻ này", style = MaterialTheme.typography.titleMedium)
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoryCards) { card ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        ListItem(
                            headlineContent = { Text(card.front, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(card.back) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.deleteFlashcard(card) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f))
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
                title = { Text("Tạo thẻ mới", style = MaterialTheme.typography.titleLarge) },
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
                label = { Text("Tên bộ thẻ (Chủ đề)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = front,
                onValueChange = { front = it },
                label = { Text("Mặt trước (Từ vựng/Câu hỏi)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = back,
                onValueChange = { back = it },
                label = { Text("Mặt sau (Nghĩa/Trả lời)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                minLines = 3
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    if (front.isNotBlank() && back.isNotBlank()) {
                        viewModel.addFlashcard(front.trim(), back.trim(), category.trim().ifEmpty { "Chung" })
                        onFinish()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Tạo ngay", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
