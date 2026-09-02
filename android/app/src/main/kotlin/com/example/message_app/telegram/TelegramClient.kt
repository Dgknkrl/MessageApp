package com.example.message_app.telegram

object TelegramClient {
    fun sendMessage(sender: String, body: String) {
        // TODO: Read locally injected configuration and make an HTTPS POST to
        // https://api.telegram.org/bot<TOKEN>/sendMessage.
        // Never log the token, chat ID, or SMS body.
        throw NotImplementedError("Telegram transport is intentionally not configured")
    }
}

