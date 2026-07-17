import 'dart:async';

import 'package:flutter/services.dart';

class PermissionSnapshot {
  const PermissionSnapshot({
    required this.usageAccessGranted,
    required this.accessibilityEnabled,
  });

  factory PermissionSnapshot.fromMap(Map<Object?, Object?> map) {
    return PermissionSnapshot(
      usageAccessGranted: map['usageAccessGranted'] == true,
      accessibilityEnabled: map['accessibilityEnabled'] == true,
    );
  }

  static const unavailable = PermissionSnapshot(
    usageAccessGranted: false,
    accessibilityEnabled: false,
  );

  final bool usageAccessGranted;
  final bool accessibilityEnabled;

  Map<String, Object> toJson() => {
    'usageAccessGranted': usageAccessGranted,
    'accessibilityEnabled': accessibilityEnabled,
  };
}

class ForegroundPackageEvent {
  const ForegroundPackageEvent({
    required this.packageName,
    required this.eventType,
    required this.detectedAtMs,
  });

  factory ForegroundPackageEvent.fromMap(Map<Object?, Object?> map) {
    return ForegroundPackageEvent(
      packageName: map['packageName'] as String? ?? 'unknown',
      eventType: map['eventType'] as String? ?? 'unknown',
      detectedAtMs: (map['detectedAtMs'] as num?)?.toInt() ?? 0,
    );
  }

  final String packageName;
  final String eventType;
  final int detectedAtMs;

  Map<String, Object> toJson() => {
    'packageName': packageName,
    'eventType': eventType,
    'detectedAtMs': detectedAtMs,
  };
}

class DeviceSnapshot {
  const DeviceSnapshot({
    required this.manufacturer,
    required this.model,
    required this.androidRelease,
    required this.sdkInt,
    required this.buildId,
    required this.appVersion,
  });

  factory DeviceSnapshot.fromMap(Map<Object?, Object?> map) {
    String value(String key) => map[key] as String? ?? 'unknown';
    return DeviceSnapshot(
      manufacturer: value('manufacturer'),
      model: value('model'),
      androidRelease: value('androidRelease'),
      sdkInt: (map['sdkInt'] as num?)?.toInt() ?? 0,
      buildId: value('buildId'),
      appVersion: value('appVersion'),
    );
  }

  static const unavailable = DeviceSnapshot(
    manufacturer: 'unknown',
    model: 'unknown',
    androidRelease: 'unknown',
    sdkInt: 0,
    buildId: 'unknown',
    appVersion: 'unknown',
  );

  final String manufacturer;
  final String model;
  final String androidRelease;
  final int sdkInt;
  final String buildId;
  final String appVersion;

  Map<String, Object> toJson() => {
    'manufacturer': manufacturer,
    'model': model,
    'androidRelease': androidRelease,
    'sdkInt': sdkInt,
    'buildId': buildId,
    'appVersion': appVersion,
  };
}

abstract interface class NativeControlGateway {
  Future<PermissionSnapshot> getPermissionSnapshot();
  Future<DeviceSnapshot> getDeviceSnapshot();
  Future<void> openUsageAccessSettings();
  Future<void> openAccessibilitySettings();
  Future<List<ForegroundPackageEvent>> getRecentEvents();
  Future<void> clearRecentEvents();
  Stream<ForegroundPackageEvent> get foregroundEvents;
}

class MethodChannelNativeControlGateway implements NativeControlGateway {
  MethodChannelNativeControlGateway({
    MethodChannel? methodChannel,
    EventChannel? eventChannel,
  }) : _methodChannel =
           methodChannel ?? const MethodChannel(_methodChannelName),
       _eventChannel = eventChannel ?? const EventChannel(_eventChannelName);

  static const _methodChannelName = 'pausenow/native_control';
  static const _eventChannelName = 'pausenow/foreground_events';

  final MethodChannel _methodChannel;
  final EventChannel _eventChannel;

  @override
  Future<PermissionSnapshot> getPermissionSnapshot() async {
    final result = await _methodChannel.invokeMapMethod<Object?, Object?>(
      'getPermissionSnapshot',
    );
    return result == null
        ? PermissionSnapshot.unavailable
        : PermissionSnapshot.fromMap(result);
  }

  @override
  Future<DeviceSnapshot> getDeviceSnapshot() async {
    final result = await _methodChannel.invokeMapMethod<Object?, Object?>(
      'getDeviceSnapshot',
    );
    return result == null
        ? DeviceSnapshot.unavailable
        : DeviceSnapshot.fromMap(result);
  }

  @override
  Future<void> openUsageAccessSettings() =>
      _methodChannel.invokeMethod<void>('openUsageAccessSettings');

  @override
  Future<void> openAccessibilitySettings() =>
      _methodChannel.invokeMethod<void>('openAccessibilitySettings');

  @override
  Future<List<ForegroundPackageEvent>> getRecentEvents() async {
    final result =
        await _methodChannel.invokeListMethod<Object?>('getRecentEvents') ??
        const [];
    return result
        .whereType<Map<Object?, Object?>>()
        .map(ForegroundPackageEvent.fromMap)
        .toList(growable: false);
  }

  @override
  Future<void> clearRecentEvents() =>
      _methodChannel.invokeMethod<void>('clearRecentEvents');

  @override
  Stream<ForegroundPackageEvent> get foregroundEvents => _eventChannel
      .receiveBroadcastStream()
      .where((event) => event is Map)
      .map(
        (event) => ForegroundPackageEvent.fromMap(
          Map<Object?, Object?>.from(event as Map),
        ),
      );
}
