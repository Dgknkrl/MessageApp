package com.example.message_app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.message_app.calls.CallStateStore
import com.example.message_app.data.AppPreferences
import com.example.message_app.data.LogStore
import com.example.message_app.service.SmsForwardingService
import com.example.message_app.telegram.TelegramSender
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private var permissionResult: MethodChannel.Result? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL,
        ).setMethodCallHandler(::handleMethodCall)
    }

    private fun handleMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getSettings" -> result.success(statusMap())
            "saveSettings" -> {
                val token = call.argument<String>("botToken")?.trim().orEmpty()
                val chatId = call.argument<String>("chatId")?.trim().orEmpty()
                if (token.isBlank() || chatId.isBlank()) {
                    result.error("INVALID_SETTINGS", "Bot token ve chat ID boş olamaz.", null)
                    return
                }
                AppPreferences.saveTelegramSettings(this, token, chatId)
                LogStore.add(this, "Telegram ayarları kaydedildi.")
                result.success(statusMap())
            }
            "requestPermissions" -> requestRequiredPermissions(result)
            "startService" -> {
                if (!hasSmsPermission()) {
                    result.error("PERMISSION_REQUIRED", "SMS izni verilmedi.", null)
                    return
                }
                if (!AppPreferences.hasTelegramSettings(this)) {
                    result.error("SETTINGS_REQUIRED", "Önce bot token ve chat ID kaydedin.", null)
                    return
                }
                if (!AppPreferences.isEnabled(this)) CallStateStore.reset(this)
                AppPreferences.setEnabled(this, true)
                SmsForwardingService.start(this)
                result.success(true)
            }
            "stopService" -> {
                AppPreferences.setEnabled(this, false)
                CallStateStore.reset(this)
                startService(SmsForwardingService.stopIntent(this))
                result.success(true)
            }
            "requestBatteryExemption" -> {
                requestBatteryExemption()
                result.success(true)
            }
            "testTelegram" -> {
                if (!AppPreferences.hasTelegramSettings(this)) {
                    result.error("SETTINGS_REQUIRED", "Önce ayarları kaydedin.", null)
                    return
                }
                TelegramSender.enqueue(
                    context = applicationContext,
                    sender = "MessageApp test",
                    body = "Bağlantı başarılı. SMS ve çağrı yönlendirme yapılandırıldı.",
                ) { success, message ->
                    runOnUiThread {
                        if (success) result.success(true)
                        else result.error("TELEGRAM_ERROR", message, null)
                    }
                }
            }
            "getLogs" -> result.success(LogStore.read(this))
            "clearLogs" -> {
                LogStore.clear(this)
                result.success(true)
            }
            else -> result.notImplemented()
        }
    }

    private fun statusMap(): Map<String, Any> = mapOf(
        "botToken" to AppPreferences.botToken(this),
        "chatId" to AppPreferences.chatId(this),
        "enabled" to AppPreferences.isEnabled(this),
        "serviceRunning" to SmsForwardingService.isRunning,
        "smsPermission" to hasSmsPermission(),
        "phonePermission" to hasPermission(Manifest.permission.READ_PHONE_STATE),
        "callLogPermission" to hasPermission(Manifest.permission.READ_CALL_LOG),
        "contactsPermission" to hasPermission(Manifest.permission.READ_CONTACTS),
        "notificationPermission" to hasNotificationPermission(),
        "batteryExempt" to isBatteryExempt(),
    )

    private fun requestRequiredPermissions(result: MethodChannel.Result) {
        if (permissionResult != null) {
            result.error("REQUEST_ACTIVE", "İzin isteği zaten açık.", null)
            return
        }
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            result.success(true)
            return
        }
        permissionResult = result
        requestPermissions(missing.toTypedArray(), PERMISSION_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            val granted = grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            permissionResult?.success(granted)
            permissionResult = null
        }
    }

    private fun hasPermission(permission: String) = ContextCompat.checkSelfPermission(
        this, permission,
    ) == PackageManager.PERMISSION_GRANTED

    private fun hasSmsPermission() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECEIVE_SMS,
    ) == PackageManager.PERMISSION_GRANTED

    private fun hasNotificationPermission() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun isBatteryExempt(): Boolean =
        (getSystemService(POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(packageName)

    @SuppressLint("BatteryLife")
    private fun requestBatteryExemption() {
        if (isBatteryExempt()) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        runCatching { startActivity(intent) }
    }

    companion object {
        private const val CHANNEL = "com.example.message_app/service"
        private const val PERMISSION_REQUEST = 4102
    }
}
