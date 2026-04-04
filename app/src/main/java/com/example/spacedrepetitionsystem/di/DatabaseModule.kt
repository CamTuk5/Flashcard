package com.example.spacedrepetitionsystem.di

import android.content.Context
import androidx.room.Room
import com.example.spacedrepetitionsystem.data.local.AppDatabase
import com.example.spacedrepetitionsystem.data.local.FlashcardDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "srs_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFlashcardDao(database: AppDatabase): FlashcardDao {
        return database.flashcardDao()
    }
}
