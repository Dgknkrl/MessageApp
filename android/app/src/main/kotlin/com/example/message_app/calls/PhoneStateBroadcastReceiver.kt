package com.example.message_app.calls

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.message_app.data.AppPreferences
import com.example.message_app.data.LogStore
import java.util.concurrent.Executors

class PhoneStateBroadcastReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        if (!AppPreferences.isEnabled(context) || !AppPreferences.hasTelegramSettings(context)) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val hasCallLog = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALL_LOG,
        ) == PackageManager.PERMISSION_GRANTED
        // The duplicate broadcasts arrive in unspecified order. Only the extra-bearing
        // RINGING event supplies identity (an empty value means a private caller).
        if (state == TelephonyManager.EXTRA_STATE_RINGING && hasCallLog &&
            !intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        ) return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        val occurredAt = System.currentTimeMillis()
        val appContext = context.applicationContext
        val pending = goAsync()
        executor.execute {
            try {
                CallStateStore.handle(appContext, state, number, occurredAt)
            } catch (_: Exception) {
                LogStore.add(appContext, "Çağrı bildirimi işlenemedi.")
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val executor = Executors.newSingleThreadExecutor()
    }
}
