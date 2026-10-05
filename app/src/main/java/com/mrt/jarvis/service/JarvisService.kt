package com.mrt.jarvis.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.mrt.jarvis.voice.LocalVoiceController
import java.util.Random

class JarvisService : Service() {

    private lateinit var audioManager: AudioManager
    private lateinit var voiceController: LocalVoiceController
    private val handler = Handler(Looper.getMainLooper())
    private var audioFocusRequest: AudioFocusRequest? = null

    private val fatigueCheckRunnable = object : Runnable {
        override fun run() {
            triggerFatigueCheck()
            // تکرار هر ۲ ساعت یک بار (۲ ساعت = ۷,۲۰۰,۰۰۰ میلی‌ثانیه)
            handler.postDelayed(this, 2 * 60 * 60 * 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        voiceController = LocalVoiceController(this)
        
        // شروع تایمر ۲ ساعته یادآوری و بررسی خستگی
        handler.postDelayed(fatigueCheckRunnable, 2 * 60 * 60 * 1000L)
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    fun speakWithAudioPause(text: String) {
        // ۱. قطع یا کم کردن صدای موزیک (Audio Focus)
        requestAudioFocus()

        // ۲. صحبت کردن دستیار
        voiceController.speak(text)

        // ۳. بازگرداندن صدای موزیک پس از اتمام صحبت
        handler.postDelayed({
            abandonAudioFocus()
        }, 4000)
    }

    private fun triggerFatigueCheck() {
        val prompts = listOf(
            "ام ار تی خسته نیستی؟ می‌خوای بزنی کنار یه استراحتی بکنی؟",
            "دو ساعته داری می‌رونی‌ها! یه چایی بزن، بزن کنار حالشو ببر.",
            "ام ار تی چشات خسته نشد؟ اگه خسته‌ای یکم بزن کنار استراحت کن."
        )
        val randomPrompt = prompts[Random().nextInt(prompts.size)]
        speakWithAudioPause(randomPrompt)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(fatigueCheckRunnable)
        voiceController.release()
        super.onDestroy()
    }
}