package com.valera.dictagent

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class VoiceRecorderService : Service() {

    private var recorder: MediaRecorder? = null
    private var mediaSession: MediaSessionCompat? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private var isRecording = false
    private var recordStart = 0L
    private var currentFile: File? = null

    private var lastPressTime = 0L
    private var pendingPress: Runnable? = null
    private var keyDownTime = 0L

    companion object {
        const val CHANNEL_ID = "dictagent_channel"
        const val NOTIF_ID = 1
        const val DOUBLE_THRESHOLD = 500L
        const val LONG_THRESHOLD = 700L
    }

    override fun onCreate() {
        super.onCreate()
        Config.init(this)
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
                    else @Suppress("DEPRECATION")
                        event.getParcelableExtra(Intent.EXTRA_KEY_EVENT)

                    val code = keyEvent?.keyCode ?: return false
                    if (code != KeyEvent.KEYCODE_HEADSETHOOK && code != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                        return false

                    when (keyEvent.action) {
                        KeyEvent.ACTION_DOWN -> keyDownTime = System.currentTimeMillis()
                        KeyEvent.ACTION_UP -> handleButtonUp()
                    }
                    return true
                }
            })
            isActive = true
        }
    }

    private fun handleButtonUp() {
        val holdDuration = System.currentTimeMillis() - keyDownTime
        val now = System.currentTimeMillis()

        when (Config.headsetTrigger) {
            "LONG" -> {
                if (holdDuration >= LONG_THRESHOLD) toggle()
            }
            "DOUBLE" -> {
                val timeSinceLast = now - lastPressTime
                lastPressTime = now
                if (timeSinceLast < DOUBLE_THRESHOLD) {
                    pendingPress?.let { handler.removeCallbacks(it) }
                    pendingPress = null
                    toggle()
                } else {
                    val r = Runnable { /* single press ignored */ }
                    pendingPress = r
                    handler.postDelayed(r, DOUBLE_THRESHOLD)
                }
            }
            else -> toggle()
        }
    }

    private fun toggle() {
        if (isRecording) stopAndSend() else startRecording()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "TOGGLE") {
            toggle()
        } else {
            MediaButtonReceiver.handleIntent(mediaSession!!, intent)
        }
        return START_STICKY
    }

    private fun startRecording() {
        if (isRecording) return
        val dir = File(getExternalFilesDir(null), "Recordings").also { it.mkdirs() }
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        currentFile = File(dir, "voice_${sdf.format(Date())}.m4a")
        recordStart = System.currentTimeMillis()
        try {
            recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(64000)
                setOutputFile(currentFile!!.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            AppState.isRecording = true
            AppState.notifyUpdate(this)
            updateNotification("● Запись... нажми для остановки")
        } catch (e: Exception) {
            updateNotification("Ошибка записи: ${e.message}")
        }
    }

    private fun stopAndSend() {
        if (!isRecording) return
        isRecording = false
        AppState.isRecording = false
        AppState.notifyUpdate(this)
        try { recorder?.stop(); recorder?.release() } catch (_: Exception) {}
        recorder = null
        val file = currentFile ?: run { updateNotification("Готов к записи"); return }
        val dur = ((System.currentTimeMillis() - recordStart) / 1000).toInt()
        updateNotification("Отправка...")
        val entry = RecordingEntry(
            type = "MANUAL", timestamp = recordStart, durationSec = dur,
            filePath = file.absolutePath, status = "SENDING"
        )
        RecordingStorage.save(this, entry)
        executor.submit {
            try {
                val ok = sendSync(entry, file)
                entry.status = if (ok) "SENT" else "ERROR"
            } catch (_: Exception) { entry.status = "ERROR" }
            RecordingStorage.update(this, entry)
            AppState.notifyUpdate(this)
            updateNotification(if (entry.status == "SENT") "Готов к записи ✓" else "Ошибка отправки")
        }
    }

    private fun sendSync(entry: RecordingEntry, file: File): Boolean {
        if (!Config.ok) return false
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val caption = "[${sdf.format(Date(entry.timestamp))}] Запись (${entry.durationSec}с)"
        repeat(3) { attempt ->
            try {
                val body = okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("chat_id", Config.chatId)
                    .addFormDataPart("caption", caption)
                    .addFormDataPart("audio", file.name,
                        file.asRequestBody("audio/m4a".toMediaType()))
                    .build()
                val req = okhttp3.Request.Builder()
                    .url("https://api.telegram.org/bot${Config.botToken}/sendAudio")
                    .post(body).build()
                okhttp3.OkHttpClient().newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) return true
                }
            } catch (_: Exception) { if (attempt < 2) Thread.sleep(2000) }
        }
        return false
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DictAgent")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi).setOngoing(true).build()
    }

    private fun updateNotification(text: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "DictAgent", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        recorder?.release()
        mediaSession?.release()
        executor.shutdown()
        super.onDestroy()
    }
}
