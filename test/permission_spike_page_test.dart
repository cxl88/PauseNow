import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pausenow/bridge/native_control_bridge.dart';
import 'package:pausenow/features/permissions/permission_spike_page.dart';

void main() {
  testWidgets(
    'requires disclosure acknowledgement before accessibility settings',
    (tester) async {
      final gateway = _FakeGateway();

      await tester.pumpWidget(
        MaterialApp(home: PermissionSpikePage(gateway: gateway)),
      );
      await tester.pumpAndSettle();

      final settingsButton = find.widgetWithText(FilledButton, '打开无障碍设置');
      expect(tester.widget<FilledButton>(settingsButton).onPressed, isNull);

      await tester.drag(find.byType(ListView), const Offset(0, -450));
      await tester.pumpAndSettle();
      final disclosureCheckbox = find.byType(Checkbox);
      await tester.tap(disclosureCheckbox);
      await tester.pump();
      expect(tester.widget<FilledButton>(settingsButton).onPressed, isNotNull);

      await tester.tap(settingsButton);
      await tester.pump();
      expect(gateway.accessibilitySettingsOpenCount, 1);
    },
  );

  testWidgets('shows persisted foreground package events', (tester) async {
    final gateway = _FakeGateway(
      events: const [
        ForegroundPackageEvent(
          packageName: 'com.example.video',
          eventType: 'TYPE_WINDOW_STATE_CHANGED',
          detectedAtMs: 1,
        ),
      ],
    );

    await tester.pumpWidget(
      MaterialApp(home: PermissionSpikePage(gateway: gateway)),
    );
    await tester.pumpAndSettle();

    await tester.drag(find.byType(ListView), const Offset(0, -600));
    await tester.pumpAndSettle();
    expect(find.text('com.example.video'), findsOneWidget);
  });
}

class _FakeGateway implements NativeControlGateway {
  _FakeGateway({this.events = const []});

  final List<ForegroundPackageEvent> events;
  final StreamController<ForegroundPackageEvent> _controller =
      StreamController.broadcast();
  int accessibilitySettingsOpenCount = 0;

  @override
  Future<void> clearRecentEvents() async {}

  @override
  Stream<ForegroundPackageEvent> get foregroundEvents => _controller.stream;

  @override
  Future<PermissionSnapshot> getPermissionSnapshot() async =>
      const PermissionSnapshot(
        usageAccessGranted: false,
        accessibilityEnabled: false,
      );

  @override
  Future<DeviceSnapshot> getDeviceSnapshot() async =>
      DeviceSnapshot.unavailable;

  @override
  Future<List<ForegroundPackageEvent>> getRecentEvents() async => events;

  @override
  Future<void> openAccessibilitySettings() async {
    accessibilitySettingsOpenCount += 1;
  }

  @override
  Future<void> openUsageAccessSettings() async {}
}
