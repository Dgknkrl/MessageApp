import 'package:flutter_test/flutter_test.dart';
import 'package:message_app/service_bridge.dart';

void main() {
  test('missing native permissions default to false', () {
    final status = ServiceStatus.fromMap(const {});
    expect(status.phonePermission, isFalse);
    expect(status.callLogPermission, isFalse);
    expect(status.contactsPermission, isFalse);
  });

  test('phone, number and contact permissions are independent', () {
    final status = ServiceStatus.fromMap(const {
      'phonePermission': true,
      'callLogPermission': false,
      'contactsPermission': true,
    });
    expect(status.phonePermission, isTrue);
    expect(status.callLogPermission, isFalse);
    expect(status.contactsPermission, isTrue);
  });
}
