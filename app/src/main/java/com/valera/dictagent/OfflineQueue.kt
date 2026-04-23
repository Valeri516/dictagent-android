package com.valera.dictagent

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class QueuedRequest(
    val filePath: String,
    val entryId: Long,
    val endpoint: String,
    val deviceId: String,
    val timestamp: Long = System.currentTimeMillis()
)

object OfflineQueue {
    private val gson = Gson()
    private const val FILE = "offline_queue.json"

    private fun file(ctx: Context) = File(ctx.filesDir, FILE)

    fun add(ctx: Context, req: QueuedRequest) {
        val list = getAll(ctx).toMutableList()
        list.add(req)
        file(ctx).writeText(gson.toJson(list))
    }

    fun getAll(ctx: Context): List<QueuedRequest> {
        val f = file(ctx)
        if (!f.exists()) return emptyList()
        return try {
            gson.fromJson(f.readText(), object : TypeToken<List<QueuedRequest>>() {}.type)
        } catch (_: Exception) { emptyList() }
    }

    fun remove(ctx: Context, entryId: Long) {
        val list = getAll(ctx).filter { it.entryId != entryId }
        file(ctx).writeText(gson.toJson(list))
    }

    fun clear(ctx: Context) = file(ctx).delete()
}
