package com.example.message_app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.message_app.telegram.TelegramClient

class SmsForwardingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: Create the notification channel and call startForeground immediately.
        val sender = intent?.getStringExtra(EXTRA_SENDER).orEmpty()
        val body = intent?.getStringExtra(EXTRA_BODY).orEmpty()

        Thread {
            try {
                TelegramClient.sendMessage(sender, body)
            } finally {
                stopSelf(startId)
            }
        }.start()

        return START_NOT_STICKY
    }

    companion object {
        private const val EXTRA_SENDER = "sender"
        private const val EXTRA_BODY = "body"

        fun createIntent(context: Context, sender: String, body: String) =
            Intent(context, SmsForwardingService::class.java).apply {
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_BODY, body)
            }
    }
}

