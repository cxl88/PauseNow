import 'package:flutter_test/flutter_test.dart';
import 'package:pausenow/bridge/native_control_bridge.dart';

void main() {
  test('permission snapshot parses missing values as false', () {
    final snapshot = PermissionSnapshot.fromMap(const {
      'usageAccessGranted': true,
    });

    expect(snapshot.usageAccessGranted, isTrue);
    expect(snapshot.accessibilityEnabled, isFalse);
  });

  test('foreground event parses native map', () {
    final event = ForegroundPackageEvent.fromMap(const {
      'packageName': 'com.example.video',
      'eventType': 'TYPE_WINDOW_STATE_CHANGED',
      'detectedAtMs': 42,
    });

    expect(event.packageName, 'com.example.video');
    expect(event.eventType, 'TYPE_WINDOW_STATE_CHANGED');
    expect(event.detectedAtMs, 42);
  });

  test('device snapshot parses evidence fields', () {
    final snapshot = DeviceSnapshot.fromMap(const {
      'manufacturer': 'Example',
      'model': 'Phone',
      'androidRelease': '16',
      'sdkInt': 36,
      'buildId': 'BP1A',
      'appVersion': '0.1.0 (1)',
    });

    expect(snapshot.sdkInt, 36);
    expect(snapshot.toJson()['model'], 'Phone');
  });
}
