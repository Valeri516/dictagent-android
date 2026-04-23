package com.valera.dictagent

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PollService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private var running = false

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Config.init(this)
        running = true
        handler.postDelayed(pollRunnable, 5000)
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            try {
                val req = Request.Builder()
                    .url("${Config.serverUrl}/api/commands/${Config.deviceId}")
                    .get().build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val json = JSONObject(body)
                        val cmds = json.optJSONArray("commands") ?: return@use
                        for (i in 0 until cmds.length()) {
                            val cmd = cmds.getJSONObject(i)
                            val type = cmd.optString("type")
                            val payload = cmd.optString("payload")
                            if (type == "tts_response" && payload.isNotBlank()) {
                                handleResponse(payload)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
            handler.postDelayed(this, 5000)
        }
    }

    private fun handleResponse(text: String) {
        AppState.lastTranscript = text
        AppState.notifyUpdate(this)
        if (AppState.headphonesConnected && Config.ttsEnabled) {
            startService(Intent(this, TtsPlayerService::class.java).apply {
                putExtra(TtsPlayerService.EXTRA_TEXT, text)
            })
        }
        // Retry отправка из офлайн-очереди заодно
        Thread { ServerSender.retryQueued(this) }.start()
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(pollRunnable)
        super.onDestroy()
    }
}
