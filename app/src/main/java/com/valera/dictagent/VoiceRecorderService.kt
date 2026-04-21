package com.valera.dictagent

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class VoiceRecorderService : Service() {

    private var recorder: MediaRecorder? = null
    private var mediaSession: MediaSessionCompat? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var lastButtonTime = 0L

    companion object {
        const val CHANNEL_ID = "dictagent_channel"
        const val NOTIF_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        startForeground(NOTIF_ID, buildNotification("Готов к записи"))
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "DictAgent").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                    .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1f)
                    .build()
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onMediaButtonEvent(event: Intent): Boolean {
                    val keyEvent: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        event.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    else
                        @Suppress("DEPRECATION")
                        event.getParcelableExtra(Intent.EXTRA_KEY_EVENT)

                    if (keyEvent?.action == KeyEvent.ACTION_DOWN &&
                        (keyEvent.keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                         keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)) {
                        val now = System.currentTimeMillis()
                        if (now - lastButtonTime > Config.BUTTON_DEBOUNCE_MS) {
                            lastButtonTime = now
                            onTriggerPressed()
                        }
                        return true
                    }
                    return false
                }
            })
            isActive = true
        }
    }

    fun onTriggerPressed() {
        when (AppState.get()) {
            State.IDLE -> startRecording()
            State.RECORDING -> stopAndSend()
            else -> {}
        }
    }

    private fun startRecording() {
        if (!AppState.compareAndSet(State.IDLE, State.RECORDING)) return

        val dir = File(getExternalFilesDir(null), "Recordings")
        dir.mkdirs()
        val filename = "voice_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.m4a"
        val filePath = File(dir, filename).absolutePath
        AppState.lastFilePath = filePath

        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
        ).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16000)
            setAudioEncodingBitRate(64000)
            setOutputFile(filePath)
            setMaxDuration(Config.MAX_RECORDING_SECONDS * 1000)
            prepare()
            start()
        }
        updateNotification("Запись... нажми кнопку для остановки")
    }

    private fun stopAndSend() {
        if (!AppState.compareAndSet(State.RECORDING, State.SENDING)) return

        try { recorder?.stop(); recorder?.release() } catch (e: Exception) {}
        recorder = null
        updateNotification("Отправка в Telegram...")

        val filePath = AppState.lastFilePath ?: run {
            AppState.set(State.IDLE)
            updateNotification("Готов к записи")
            return
        }

        executor.submit {
            val ok = TelegramSender.send(filePath)
            AppState.set(State.IDLE)
            updateNotification(if (ok) "Готов к записи ✓" else "Ошибка отправки")
        }
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DictAgent")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "DictAgent", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession!!, intent)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        recorder?.release()
        mediaSession?.release()
        executor.shutdown()
        super.onDestroy()
    }
}
