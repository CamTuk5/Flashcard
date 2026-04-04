package com.example.spacedrepetitionsystem.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.spacedrepetitionsystem.data.model.Flashcard

@Database(entities = [Flashcard::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao
}
