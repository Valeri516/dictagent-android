package com.valera.dictagent

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RecordingStorage {
    private const val PREFS = "dictagent_records"
    private const val KEY = "entries"
    private val gson = Gson()

    fun save(ctx: Context, entry: RecordingEntry) {
        val list = getAll(ctx).toMutableList()
        list.add(0, entry)
        if (list.size > 500) list.subList(500, list.size).clear()
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, gson.toJson(list)).apply()
    }

    fun update(ctx: Context, entry: RecordingEntry) {
        val list = getAll(ctx).toMutableList()
        val idx = list.indexOfFirst { it.id == entry.id }
        if (idx >= 0) list[idx] = entry else list.add(0, entry)
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, gson.toJson(list)).apply()
    }

    fun getAll(ctx: Context): List<RecordingEntry> {
        val json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<RecordingEntry>>() {}.type)
        } catch (e: Exception) { emptyList() }
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
