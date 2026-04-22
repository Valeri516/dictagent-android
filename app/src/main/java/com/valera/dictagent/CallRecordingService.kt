package com.valera.dictagent

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class CallRecordingService : Service() {
    private val executor = Executors.newSingleThreadExecutor()
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTime = 0L
    private var callType = "CALL_IN"
    private var contact = ""
    private val CH = "dictagent_calls"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CH, "Call Recording", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                callType = intent.getStringExtra("type") ?: "CALL_IN"
                contact = intent.getStringExtra("contact") ?: ""
                startForeground(3, buildNotification("Запись звонка..."))
                startRecording()
            }
            "STOP" -> stopRecordingAndSend()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        val dir = getExternalFilesDir(null) ?: filesDir
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        currentFile = File(dir, "call_${sdf.format(Date())}.m4a")
        startTime = System.currentTimeMillis()
        try {
            recorder = newRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_CALL)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(64000)
                setOutputFile(currentFile!!.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            try {
                recorder = newRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(currentFile!!.absolutePath)
                    prepare()
                    start()
                }
            } catch (e2: Exception) { stopSelf() }
        }
    }

    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this)
        else @Suppress("DEPRECATION") MediaRecorder()

    private fun stopRecordingAndSend() {
        try { recorder?.stop(); recorder?.release() } catch (_: Exception) {}
        recorder = null
        val file = currentFile ?: run { stopSelf(); return }
        val dur = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        val entry = RecordingEntry(
            type = callType, timestamp = startTime, durationSec = dur,
            filePath = file.absolutePath, contact = contact, status = "SENDING"
        )
        RecordingStorage.save(this, entry)
        AppState.notifyUpdate(this)
        executor.submit {
            TelegramSender.send(this, entry, file)
            stopSelf()
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CH)
            .setContentTitle("DictAgent")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
}
