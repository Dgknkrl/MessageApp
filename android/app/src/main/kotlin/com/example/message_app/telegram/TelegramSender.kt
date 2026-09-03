package com.example.message_app.telegram

import android.content.Context
import com.example.message_app.calls.CallStateTracker
import com.example.message_app.data.AppPreferences
import com.example.message_app.data.LogStore
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object TelegramSender {
    private const val QUEUE_FILE = "telegram_queue"
    private const val QUEUE_KEY = "messages"
    private const val MAX_QUEUE_SIZE = 100
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun enqueue(
        context: Context,
        sender: String,
        body: String,
        callback: ((Boolean, String) -> Unit)? = null,
    ) {
        val appContext = context.applicationContext
        executor.execute {
            appendToQueue(appContext, sender, body)
            drain(appContext, callback)
        }
    }

    fun drainPending(context: Context) {
        executor.execute { drain(context.applicationContext, null) }
    }

    /** Persist on the receiver worker before its PendingResult is finished. */
    fun enqueueCall(
        context: Context,
        number: String?,
        name: String?,
        event: CallStateTracker.Event,
        occurredAt: Long,
    ) {
        val title = when (event) {
            CallStateTracker.Event.INCOMING -> "📞 Gelen arama"
            CallStateTracker.Event.ANSWERED -> "✅ Cevaplandı"
            CallStateTracker.Event.UNANSWERED -> "❌ Cevapsız / reddedildi"
        }
        val caller = number?.takeIf { it.isNotBlank() } ?: "Gizli / bilinmeyen numara"
        val identity = name?.takeIf { it.isNotBlank() }?.let { "$it\n$caller" } ?: caller
        val time = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(occurredAt))
        appendTextToQueue(context, "$title\n$identity\nSaat: $time")
        LogStore.add(context, "Çağrı bildirimi Telegram kuyruğuna eklendi.")
        drainPending(context)
    }

    private fun drain(context: Context, callback: ((Boolean, String) -> Unit)?) {
        var firstResultReported = false
        while (true) {
            val item = firstQueued(context) ?: return
            val result = sendNow(context, item.text)
            if (!firstResultReported) {
                callback?.invoke(result.first, result.second)
                firstResultReported = true
            }
            if (result.first) {
                removeQueued(context, item)
                LogStore.add(context, "Mesaj Telegram'a iletildi.")
            } else {
                LogStore.add(context, "Telegram gönderimi başarısız; mesaj kuyrukta tutuluyor.")
                executor.schedule({ drain(context, null) }, 60, TimeUnit.SECONDS)
                return
            }
        }
    }

    private fun sendNow(context: Context, text: String): Pair<Boolean, String> {
        val token = AppPreferences.botToken(context)
        val chatId = AppPreferences.chatId(context)
        if (token.isBlank() || chatId.isBlank()) return false to "Telegram ayarları eksik."

        val form = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", text)
            .build()
        val request = Request.Builder()
            .url("https://api.telegram.org/bot$token/sendMessage")
            .post(form)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                val apiOk = runCatching {
                    JSONObject(responseBody).optBoolean("ok", false)
                }.getOrDefault(false)
                if (response.isSuccessful && apiOk) true to "Gönderildi."
                else false to "Telegram isteği reddedildi (HTTP ${response.code})."
            }
        } catch (_: Exception) {
            false to "Telegram'a bağlanılamadı. Ağ bağlantısını kontrol edin."
        }
    }

    @Synchronized
    private fun appendToQueue(context: Context, sender: String, body: String) {
        appendTextToQueue(context, "📩 Gönderen: $sender\nMesaj: $body")
    }

    @Synchronized
    private fun appendTextToQueue(context: Context, text: String) {
        val queue = readQueue(context)
        while (queue.length() >= MAX_QUEUE_SIZE) queue.remove(0)
        queue.put(JSONObject().put("id", UUID.randomUUID().toString()).put("text", text))
        writeQueue(context, queue)
    }

    @Synchronized
    private fun firstQueued(context: Context): QueueItem? {
        val queue = readQueue(context)
        if (queue.length() == 0) return null
        val item = queue.getJSONObject(0)
        // Pending SMS entries from earlier app versions remain readable.
        val text = if (item.has("text")) item.getString("text") else
            "📩 Gönderen: ${item.optString("sender", "Bilinmiyor")}\nMesaj: ${item.optString("body", "")}"
        return QueueItem(text, item.toString())
    }

    @Synchronized
    private fun removeQueued(context: Context, item: QueueItem) {
        val queue = readQueue(context)
        // A concurrent append can evict the in-flight entry when the queue is full.
        // Remove only the entry actually sent, never the new first entry.
        for (index in 0 until queue.length()) {
            if (queue.getJSONObject(index).toString() == item.raw) {
                queue.remove(index)
                break
            }
        }
        writeQueue(context, queue)
    }

    private fun readQueue(context: Context): JSONArray {
        val raw = context.getSharedPreferences(QUEUE_FILE, Context.MODE_PRIVATE)
            .getString(QUEUE_KEY, "[]") ?: "[]"
        return runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
    }

    private fun writeQueue(context: Context, queue: JSONArray) {
        context.getSharedPreferences(QUEUE_FILE, Context.MODE_PRIVATE)
            .edit().putString(QUEUE_KEY, queue.toString()).commit()
    }

    private data class QueueItem(val text: String, val raw: String)
}
