package com.valera.dictagent

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object TelegramSender {
    private val client = OkHttpClient()
    private val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    fun send(ctx: Context, entry: RecordingEntry, file: File): Boolean {
        if (!Config.ok) return false
        val caption = buildCaption(entry)
        repeat(3) { attempt ->
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", Config.chatId)
                    .addFormDataPart("caption", caption)
                    .addFormDataPart("audio", file.name, file.asRequestBody("audio/m4a".toMediaType()))
                    .build()
                val req = Request.Builder()
                    .url("https://api.telegram.org/bot${Config.botToken}/sendAudio")
                    .post(body).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        entry.status = "SENT"
                        RecordingStorage.update(ctx, entry)
                        AppState.notifyUpdate(ctx)
                        return true
                    }
                }
            } catch (e: Exception) {
                if (attempt < 2) Thread.sleep(2000)
            }
        }
        entry.status = "ERROR"
        RecordingStorage.update(ctx, entry)
        AppState.notifyUpdate(ctx)
        return false
    }

    fun sendText(ctx: Context, entry: RecordingEntry): Boolean {
        if (!Config.ok) return false
        val text = buildCaption(entry)
        repeat(3) { attempt ->
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", Config.chatId)
                    .addFormDataPart("text", text)
                    .build()
                val req = Request.Builder()
                    .url("https://api.telegram.org/bot${Config.botToken}/sendMessage")
                    .post(body).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        entry.status = "SENT"
                        RecordingStorage.update(ctx, entry)
                        AppState.notifyUpdate(ctx)
                        return true
                    }
                }
            } catch (e: Exception) {
                if (attempt < 2) Thread.sleep(2000)
            }
        }
        entry.status = "ERROR"
        RecordingStorage.update(ctx, entry)
        AppState.notifyUpdate(ctx)
        return false
    }

    private fun buildCaption(e: RecordingEntry): String {
        val time = sdf.format(Date(e.timestamp))
        val typeLabel = when(e.type) {
            "CALL_IN" -> "Входящий звонок"
            "CALL_OUT" -> "Исходящий звонок"
            "VIBER" -> "Viber"
            else -> "Запись"
        }
        val contact = if (!e.contact.isNullOrBlank()) " от ${e.contact}" else ""
        val dur = if (e.durationSec > 0) " (${e.durationSec}с)" else ""
        val text = if (!e.text.isNullOrBlank()) "\n${e.text}" else ""
        return "[$time] $typeLabel$contact$dur$text"
    }
}
