package com.example.spacedrepetitionsystem.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spacedrepetitionsystem.data.model.Flashcard
import com.example.spacedrepetitionsystem.data.repository.FlashcardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: FlashcardRepository
) : ViewModel() {

    private val _currentTime = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            while (true) {
                _currentTime.value = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val cardsToReview: StateFlow<List<Flashcard>> = _currentTime
        .flatMapLatest { time -> repository.getCardsToReview(time) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCards: StateFlow<List<Flashcard>> = repository.getAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCard(front: String, back: String) {
        viewModelScope.launch {
            repository.insertCard(Flashcard(front = front, back = back))
        }
    }

    fun reviewCard(card: Flashcard, quality: Int) {
        viewModelScope.launch {
            repository.reviewCard(card, quality)
        }
    }

    fun syncWithBackend() {
        viewModelScope.launch {
            repository.syncWithBackend()
        }
    }
}
