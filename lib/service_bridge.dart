import 'package:flutter/services.dart';

final class ServiceStatus {
  const ServiceStatus({
    required this.botToken,
    required this.chatId,
    required this.enabled,
    required this.serviceRunning,
    required this.smsPermission,
    required this.phonePermission,
    required this.callLogPermission,
    required this.contactsPermission,
    required this.notificationPermission,
    required this.batteryExempt,
  });

  factory ServiceStatus.fromMap(Map<Object?, Object?> map) => ServiceStatus(
        botToken: map['botToken'] as String? ?? '',
        chatId: map['chatId'] as String? ?? '',
        enabled: map['enabled'] as bool? ?? false,
        serviceRunning: map['serviceRunning'] as bool? ?? false,
        smsPermission: map['smsPermission'] as bool? ?? false,
        phonePermission: map['phonePermission'] as bool? ?? false,
        callLogPermission: map['callLogPermission'] as bool? ?? false,
        contactsPermission: map['contactsPermission'] as bool? ?? false,
        notificationPermission: map['notificationPermission'] as bool? ?? false,
        batteryExempt: map['batteryExempt'] as bool? ?? false,
      );

  static const empty = ServiceStatus(
    botToken: '',
    chatId: '',
    enabled: false,
    serviceRunning: false,
    smsPermission: false,
    phonePermission: false,
    callLogPermission: false,
    contactsPermission: false,
    notificationPermission: false,
    batteryExempt: false,
  );

  final String botToken;
  final String chatId;
  final bool enabled;
  final bool serviceRunning;
  final bool smsPermission;
  final bool phonePermission;
  final bool callLogPermission;
  final bool contactsPermission;
  final bool notificationPermission;
  final bool batteryExempt;

  bool get hasSettings => botToken.isNotEmpty && chatId.isNotEmpty;
}

final class ServiceBridge {
  const ServiceBridge();

  static const _channel = MethodChannel('com.example.message_app/service');

  Future<ServiceStatus> getStatus() async {
    final value = await _channel.invokeMethod<Map<Object?, Object?>>(
      'getSettings',
    );
    return ServiceStatus.fromMap(value ?? const {});
  }

  Future<ServiceStatus> saveSettings(String botToken, String chatId) async {
    final value = await _channel.invokeMethod<Map<Object?, Object?>>(
      'saveSettings',
      {'botToken': botToken, 'chatId': chatId},
    );
    return ServiceStatus.fromMap(value ?? const {});
  }

  Future<bool> requestPermissions() async =>
      await _channel.invokeMethod<bool>('requestPermissions') ?? false;

  Future<void> startService() => _channel.invokeMethod<void>('startService');
  Future<void> stopService() => _channel.invokeMethod<void>('stopService');
  Future<void> requestBatteryExemption() =>
      _channel.invokeMethod<void>('requestBatteryExemption');
  Future<void> testTelegram() => _channel.invokeMethod<void>('testTelegram');

  Future<List<String>> getLogs() async {
    final values = await _channel.invokeMethod<List<Object?>>('getLogs');
    return (values ?? const []).whereType<String>().toList(growable: false);
  }

  Future<void> clearLogs() => _channel.invokeMethod<void>('clearLogs');
}
