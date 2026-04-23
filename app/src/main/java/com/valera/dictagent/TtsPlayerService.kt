package com.valera.dictagent

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsPlayerService : Service() {
    private var tts: TextToSpeech? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    companion object {
        const val EXTRA_TEXT = "text"
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ru", "RU")
                tts?.setSpeechRate(1.0f)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: run { stopSelf(); return START_NOT_STICKY }
        if (!Config.ttsEnabled) { stopSelf(); return START_NOT_STICKY }
        if (!AppState.headphonesConnected) { stopSelf(); return START_NOT_STICKY }
        if (Config.silentSkipTts && isSilent()) { stopSelf(); return START_NOT_STICKY }

        val maxWords = Config.ttsMaxSeconds * 2
        val truncated = text.split(" ").take(maxWords).joinToString(" ")

        requestFocus()

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { abandonFocus(); stopSelf() }
            override fun onError(id: String?) { abandonFocus(); stopSelf() }
        })
        tts?.speak(truncated, TextToSpeech.QUEUE_FLUSH, null, "da_tts")

        return START_NOT_STICKY
    }

    private fun isSilent(): Boolean {
        return audioManager?.ringerMode == AudioManager.RINGER_MODE_SILENT ||
               audioManager?.ringerMode == AudioManager.RINGER_MODE_VIBRATE
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .build()
            audioManager?.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown()
        super.onDestroy()
    }
}
