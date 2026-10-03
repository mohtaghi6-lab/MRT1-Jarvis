package com.mrt.jarvis.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class LocalVoiceController(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("fa", "IR"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
                configureVoice()
            }
        }
    }

    private fun configureVoice() {
        val voices = tts?.voices ?: return
        for (v in voices) {
            if (v.name.lowercase().contains("female") || v.name.lowercase().contains("fa")) {
                tts?.voice = v
                break
            }
        }
        tts?.setPitch(1.1f)
        tts?.setSpeechRate(0.95f)
    }

    fun processCommand(input: String) {
        val text = input.trim().lowercase()
        if (text.contains("mrt") || text.contains("ام ار تی") || text.contains("ام‌ار‌تی")) {
            speak("در خدمتیم!")
        }
    }

    fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MRT_VOICE")
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
    }
}