package com.example.message_app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.message_app.data.LogStore
import com.example.message_app.telegram.TelegramSender

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent).toList()
        if (messages.isEmpty()) return
        val sender = messages.first().originatingAddress ?: "Bilinmiyor"
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        if (body.isBlank()) return

        LogStore.add(context, "SMS alındı; Telegram kuyruğuna eklendi.")
        TelegramSender.enqueue(context.applicationContext, sender, body)
    }
}
