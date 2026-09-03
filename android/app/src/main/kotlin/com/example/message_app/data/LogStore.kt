package com.example.message_app.data

import android.content.Context
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogStore {
    private const val FILE = "app_logs"
    private const val KEY = "entries"
    private const val MAX_ENTRIES = 100

    @Synchronized
    fun add(context: Context, message: String) {
        val entries = mutableListOf<String>()
        val existing = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        runCatching {
            val array = JSONArray(existing)
            for (index in 0 until array.length()) entries += array.getString(index)
        }
        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        entries.add(0, "$timestamp — $message")
        while (entries.size > MAX_ENTRIES) entries.removeAt(entries.lastIndex)
        val updated = JSONArray().apply { entries.forEach(::put) }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY, updated.toString()).apply()
    }

    @Synchronized
    fun read(context: Context): List<String> {
        val raw = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { array.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun clear(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
