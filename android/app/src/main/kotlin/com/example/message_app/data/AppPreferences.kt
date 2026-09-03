package com.example.message_app.data

import android.content.Context

object AppPreferences {
    private const val FILE = "app_prefs"
    private const val BOT_TOKEN = "bot_token"
    private const val CHAT_ID = "chat_id"
    private const val ENABLED = "forwarding_enabled"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun saveTelegramSettings(context: Context, token: String, chatId: String) {
        prefs(context).edit()
            .putString(BOT_TOKEN, token)
            .putString(CHAT_ID, chatId)
            .apply()
    }

    fun botToken(context: Context): String = prefs(context).getString(BOT_TOKEN, "").orEmpty()
    fun chatId(context: Context): String = prefs(context).getString(CHAT_ID, "").orEmpty()
    fun hasTelegramSettings(context: Context) = botToken(context).isNotBlank() && chatId(context).isNotBlank()
    fun isEnabled(context: Context) = prefs(context).getBoolean(ENABLED, false)
    fun setEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(ENABLED, enabled).apply()
}
