package com.valera.dictagent

import android.app.*
import android.content.Intent
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import java.util.Calendar

class VoicePhraseService : Service() {
    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private val CH = "dictagent_voice"

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Config.init(this)
        createChannel()
        startForeground(5, buildNotif("Слушаю фразу..."))
        running = true
        handler.post(startListening)
    }

    private val startListening = object : Runnable {
        override fun run() {
            if (!running || !Config.voicePhraseEnabled) return
            if (!isInSchedule()) { handler.postDelayed(this, 60_000); return }
            listen()
        }
    }

    private fun isInSchedule(): Boolean {
        return when (Config.voicePhraseSchedule) {
            "SCREEN_ON" -> true
            "SCHEDULE" -> {
                val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                h >= Config.voiceScheduleFrom && h < Config.voiceScheduleTo
            }
            else -> true
        }
    }

    private fun listen() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(bundle: Bundle) {
                    val results = bundle.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                    val heard = results.firstOrNull()?.lowercase() ?: ""
                    val startPhrase = Config.voiceStartPhrase.lowercase()
                    val stopPhrase = Config.voiceStopPhrase.lowercase()
                    when {
                        heard.contains(startPhrase) && !AppState.isRecording -> {
                            val i = Intent(this@VoicePhraseService, VoiceRecorderService::class.java)
                            i.action = "START"; startService(i)
                        }
                        heard.contains(stopPhrase) && AppState.isRecording -> {
                            val i = Intent(this@VoicePhraseService, VoiceRecorderService::class.java)
                            i.action = "STOP"; startService(i)
                        }
                    }
                    if (running) handler.postDelayed(startListening, 500)
                }
                override fun onError(err: Int) { if (running) handler.postDelayed(startListening, 2000) }
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(b: Bundle?) {}
                override fun onEvent(t: Int, b: Bundle?) {}
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            })
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CH, "Голосовая фраза", NotificationManager.IMPORTANCE_MIN))
        }
    }

    private fun buildNotif(text: String) = NotificationCompat.Builder(this, CH)
        .setContentTitle("DictAgent").setContentText(text)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now).build()

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(startListening)
        recognizer?.destroy()
        super.onDestroy()
    }
}
