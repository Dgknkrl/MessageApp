import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:message_app/main.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('com.example.message_app/service');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      if (call.method == 'getSettings') {
        return <String, Object>{
          'botToken': '',
          'chatId': '',
          'enabled': false,
          'serviceRunning': false,
          'smsPermission': false,
          'notificationPermission': false,
          'batteryExempt': false,
        };
      }
      if (call.method == 'getLogs') return <String>[];
      return true;
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  testWidgets('ayar ekranı açılır', (tester) async {
    await tester.pumpWidget(const MessageApp());
    await tester.pumpAndSettle();

    expect(find.text('SMS ve çağrılar → Telegram'), findsOneWidget);
    expect(find.text('Telegram ayarları'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('Rehberdeki adları göster'),
      300,
      scrollable: find.byType(Scrollable).first,
    );
    expect(find.text('Telefon ve arayan numara izinleri'), findsOneWidget);
    expect(find.text('Rehberdeki adları göster'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('Yönlendirmeyi başlat'),
      300,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.pumpAndSettle();
    expect(find.text('Yönlendirmeyi başlat'), findsOneWidget);
  });
}
