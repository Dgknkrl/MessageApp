package com.example.message_app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.message_app.MainActivity
import com.example.message_app.calls.CallStateStore
import com.example.message_app.data.AppPreferences
import com.example.message_app.data.LogStore
import com.example.message_app.sms.SmsReceiver
import com.example.message_app.telegram.TelegramSender

class SmsForwardingService : Service() {
    private var receiver: SmsReceiver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        registerSmsReceiver()
        isRunning = true
        LogStore.add(this, "SMS ve çağrı yönlendirme servisi başladı.")
        TelegramSender.drainPending(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !AppPreferences.isEnabled(this)) {
            AppPreferences.setEnabled(this, false)
            CallStateStore.reset(this)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        receiver?.let { runCatching { unregisterReceiver(it) } }
        receiver = null
        isRunning = false
        LogStore.add(this, "SMS ve çağrı yönlendirme servisi durdu.")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerSmsReceiver() {
        if (receiver != null) return
        receiver = SmsReceiver()
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS ve çağrı yönlendirme",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "SMS ve çağrı yönlendirme servisi çalışırken gösterilir."
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_notify_chat)
        .setContentTitle("SMS ve çağrı yönlendirme aktif")
        .setContentText("İzin verilen SMS ve çağrı bildirimleri Telegram'a iletilecek.")
        .setOngoing(true)
        .setSilent(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .addAction(
            android.R.drawable.ic_media_pause,
            "Durdur",
            PendingIntent.getService(
                this,
                1,
                stopIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .build()

    companion object {
        private const val CHANNEL_ID = "sms_forwarding"
        private const val NOTIFICATION_ID = 8127
        private const val ACTION_START = "com.example.message_app.START_FORWARDING"
        private const val ACTION_STOP = "com.example.message_app.STOP_FORWARDING"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun startIntent(context: Context) =
            Intent(context, SmsForwardingService::class.java).setAction(ACTION_START)

        fun stopIntent(context: Context) =
            Intent(context, SmsForwardingService::class.java).setAction(ACTION_STOP)

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, startIntent(context))
        }
    }
}
