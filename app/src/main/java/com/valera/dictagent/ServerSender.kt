package com.valera.dictagent

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object ServerSender {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private const val SERVER = "http://144.91.98.6:8340"

    fun sendUrgent(ctx: Context, entry: RecordingEntry, file: File): Boolean {
        return sendAudio(ctx, entry, file, "$SERVER/api/urgent")
    }

    fun sendChunk(ctx: Context, entry: RecordingEntry, file: File): Boolean {
        return sendAudio(ctx, entry, file, "$SERVER/api/chunk")
    }

    private fun sendAudio(ctx: Context, entry: RecordingEntry, file: File, url: String): Boolean {
        repeat(3) { attempt ->
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("device_id", "android")
                    .addFormDataPart("audio", file.name, file.asRequestBody("audio/m4a".toMediaType()))
                    .build()
                val req = Request.Builder().url(url).post(body).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        entry.status = "SENT"
                        RecordingStorage.update(ctx, entry)
                        AppState.notifyUpdate(ctx)
                        return true
                    }
                }
            } catch (e: Exception) {
                if (attempt < 2) Thread.sleep(3000)
            }
        }
        entry.status = "ERROR"
        RecordingStorage.update(ctx, entry)
        AppState.notifyUpdate(ctx)
        return false
    }
}
