package com.example.message_app.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.message_app.calls.CallStateStore
import com.example.message_app.data.AppPreferences
import com.example.message_app.data.LogStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        CallStateStore.reset(context)
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        if (AppPreferences.isEnabled(context) &&
            AppPreferences.hasTelegramSettings(context) &&
            hasPermission
        ) {
            runCatching { SmsForwardingService.start(context) }
                .onFailure { LogStore.add(context, "Yeniden başlatma sonrası servis başlatılamadı.") }
        }
    }
}
