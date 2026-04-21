package com.valera.dictagent

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object TelegramSender {
    private val client = OkHttpClient()

    fun send(filePath: String): Boolean {
        val file = File(filePath)
        if (!file.exists()) return false

        Thread.sleep(Config.FILE_STABILIZE_WAIT_MS)
        val sizeBefore = file.length()
        Thread.sleep(500)
        if (file.length() != sizeBefore) Thread.sleep(1000)

        repeat(3) { attempt ->
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", Config.CHAT_ID)
                    .addFormDataPart(
                        "audio", file.name,
                        file.asRequestBody("audio/m4a".toMediaType())
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://api.telegram.org/bot${Config.BOT_TOKEN}/sendAudio")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) return true
                }
            } catch (e: Exception) {
                if (attempt < 2) Thread.sleep(2000)
            }
        }
        return false
    }
}
