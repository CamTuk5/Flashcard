package com.example.spacedrepetitionsystem.data.remote

import com.example.spacedrepetitionsystem.data.model.Flashcard
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("sync")
    suspend fun getFlashcards(): List<Flashcard>

    @POST("sync")
    suspend fun syncFlashcards(@Body flashcards: List<Flashcard>)
}
