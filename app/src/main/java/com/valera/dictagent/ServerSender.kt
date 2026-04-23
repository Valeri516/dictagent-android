package com.valera.dictagent

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object ServerSender {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    fun isOnline(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun sendUrgent(ctx: Context, entry: RecordingEntry, file: File): Boolean {
        if (!isOnline(ctx)) {
            OfflineQueue.add(ctx, QueuedRequest(file.absolutePath, entry.id, "urgent", Config.deviceId))
            entry.status = "QUEUED"
            RecordingStorage.update(ctx, entry)
            AppState.notifyUpdate(ctx)
            return false
        }
        val headphones = AppState.headphonesConnected
        val ok = postAudio(ctx, entry, file, "${Config.serverUrl}/api/urgent", headphones)
        if (ok && Config.deleteAudioAfterSend) file.delete()
        return ok
    }

    fun sendChunk(ctx: Context, entry: RecordingEntry, file: File): Boolean {
        if (!isOnline(ctx)) {
            OfflineQueue.add(ctx, QueuedRequest(file.absolutePath, entry.id, "chunk", Config.deviceId))
            entry.status = "QUEUED"
            RecordingStorage.update(ctx, entry)
            AppState.notifyUpdate(ctx)
            return false
        }
        val ok = postAudio(ctx, entry, file, "${Config.serverUrl}/api/chunk", false)
        if (ok && Config.deleteAudioAfterSend) file.delete()
        return ok
    }

    fun sendViberText(ctx: Context, entry: RecordingEntry): Boolean {
        if (!isOnline(ctx)) {
            entry.status = "QUEUED"
            RecordingStorage.update(ctx, entry)
            return false
        }
        return postText(ctx, entry)
    }

    fun retryQueued(ctx: Context) {
        if (!isOnline(ctx)) return
        val queue = OfflineQueue.getAll(ctx)
        for (req in queue) {
            val entry = RecordingStorage.getAll(ctx).find { it.id == req.entryId } ?: continue
            val file = File(req.filePath)
            if (!file.exists()) { OfflineQueue.remove(ctx, req.entryId); continue }
            val ok = postAudio(ctx, entry, file, "${Config.serverUrl}/api/${req.endpoint}", false)
            if (ok) {
                OfflineQueue.remove(ctx, req.entryId)
                if (Config.deleteAudioAfterSend) file.delete()
            }
        }
    }

    private fun postAudio(ctx: Context, entry: RecordingEntry, file: File, url: String, headphones: Boolean): Boolean {
        repeat(3) { attempt ->
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("device_id", Config.deviceId)
                    .addFormDataPart("headphones_connected", headphones.toString())
                    .addFormDataPart("ai_model", Config.aiModel)
                    .addFormDataPart("audio", file.name, file.asRequestBody("audio/m4a".toMediaType()))
                    .build()
                val req = Request.Builder().url(url).post(body).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = try { JSONObject(resp.body?.string() ?: "{}") } catch (_: Exception) { JSONObject() }
                        val transcript = json.optString("transcript", "")
                        if (transcript.isNotBlank()) {
                            entry.text = transcript
                            AppState.lastTranscript = transcript
                        }
                        entry.status = "SENT"
                        RecordingStorage.update(ctx, entry)
                        AppState.notifyUpdate(ctx)
                        return true
                    }
                }
            } catch (_: Exception) { if (attempt < 2) Thread.sleep(3000) }
        }
        entry.status = "ERROR"
        RecordingStorage.update(ctx, entry)
        AppState.notifyUpdate(ctx)
        return false
    }

    private fun postText(ctx: Context, entry: RecordingEntry): Boolean {
        repeat(3) { attempt ->
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("device_id", Config.deviceId)
                    .addFormDataPart("ai_model", Config.aiModel)
                    .addFormDataPart("text", entry.text ?: "")
                    .addFormDataPart("source", "viber")
                    .addFormDataPart("contact", entry.contact ?: "")
                    .build()
                val req = Request.Builder().url("${Config.serverUrl}/api/viber_text").post(body).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        entry.status = "SENT"
                        RecordingStorage.update(ctx, entry)
                        AppState.notifyUpdate(ctx)
                        return true
                    }
                }
            } catch (_: Exception) { if (attempt < 2) Thread.sleep(2000) }
        }
        entry.status = "ERROR"
        RecordingStorage.update(ctx, entry)
        AppState.notifyUpdate(ctx)
        return false
    }
}
