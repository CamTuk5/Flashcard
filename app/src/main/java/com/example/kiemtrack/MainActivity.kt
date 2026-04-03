package com.example.kiemtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.example.kiemtrack.ui.FlashcardViewModel
import com.example.kiemtrack.ui.StudyScreen
import com.example.kiemtrack.ui.TtsHelper
import com.example.kiemtrack.ui.theme.KiemTraCkTheme
import com.example.kiemtrack.worker.ReminderWorker
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
                            onStartStudy = { navController.navigate("study") },
                            onAddCard = { navController.navigate("add") }
                        )
                    }
                    composable("study") {
                        StudyScreen(
                            viewModel = viewModel,
                            ttsHelper = ttsHelper,
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
fun HomeScreen(onStartStudy: () -> Unit, onAddCard: () -> Unit) {
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
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Học qua Flashcard", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onStartStudy, modifier = Modifier.size(200.dp, 60.dp)) {
                Text("Bắt đầu ôn tập")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(viewModel: FlashcardViewModel, onFinish: () -> Unit) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm thẻ mới") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            TextField(
                value = front,
                onValueChange = { front = it },
                label = { Text("Mặt trước (Từ vựng)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = back,
                onValueChange = { back = it },
                label = { Text("Mặt sau (Nghĩa)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (front.isNotBlank() && back.isNotBlank()) {
                        viewModel.addFlashcard(front, back)
                        onFinish()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lưu thẻ")
            }
        }
    }
}
