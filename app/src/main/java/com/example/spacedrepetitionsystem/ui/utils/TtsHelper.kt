package com.example.spacedrepetitionsystem.ui.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Thiết lập ngôn ngữ là Tiếng Anh (Mỹ)
            val result = tts?.setLanguage(Locale.US)
            
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Ngôn ngữ này không hỗ trợ hoặc thiếu dữ liệu!")
            } else {
                isReady = true
                // Tinh chỉnh để giọng đọc tự nhiên hơn
                tts?.setPitch(1.0f) // Cao độ bình thường
                tts?.setSpeechRate(0.9f) // Đọc chậm lại một chút để nghe rõ hơn
            }
        } else {
            Log.e("TTS", "Khởi tạo thất bại!")
        }
    }

    fun speak(text: String) {
        if (isReady) {
            // QUEUE_FLUSH: Ngắt câu đang đọc dở để đọc câu mới ngay lập tức
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
