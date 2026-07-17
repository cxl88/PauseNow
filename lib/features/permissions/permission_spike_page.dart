import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:pausenow/bridge/native_control_bridge.dart';

class PermissionSpikePage extends StatefulWidget {
  const PermissionSpikePage({super.key, this.gateway});

  final NativeControlGateway? gateway;

  @override
  State<PermissionSpikePage> createState() => _PermissionSpikePageState();
}

class _PermissionSpikePageState extends State<PermissionSpikePage>
    with WidgetsBindingObserver {
  late final NativeControlGateway _gateway;
  StreamSubscription<ForegroundPackageEvent>? _eventSubscription;
  PermissionSnapshot _snapshot = PermissionSnapshot.unavailable;
  DeviceSnapshot _device = DeviceSnapshot.unavailable;
  List<ForegroundPackageEvent> _events = const [];
  bool _disclosureAccepted = false;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _gateway = widget.gateway ?? MethodChannelNativeControlGateway();
    _eventSubscription = _gateway.foregroundEvents.listen(
      _prependEvent,
      onError: (Object error) => _setError('事件流暂时不可用：$error'),
    );
    unawaited(_refresh());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    unawaited(_eventSubscription?.cancel());
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      unawaited(_refresh());
    }
  }

  Future<void> _refresh() async {
    if (mounted) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }
    try {
      final results = await Future.wait<Object>([
        _gateway.getPermissionSnapshot(),
        _gateway.getDeviceSnapshot(),
        _gateway.getRecentEvents(),
      ]);
      if (!mounted) return;
      setState(() {
        _snapshot = results[0] as PermissionSnapshot;
        _device = results[1] as DeviceSnapshot;
        _events = results[2] as List<ForegroundPackageEvent>;
        _loading = false;
      });
    } on PlatformException catch (error) {
      _setError('原生权限桥接失败：${error.code}');
    } on Object catch (error) {
      _setError('刷新失败：$error');
    }
  }

  void _prependEvent(ForegroundPackageEvent event) {
    if (!mounted) return;
    setState(() {
      _events = [
        event,
        ..._events.where((item) => item.detectedAtMs != event.detectedAtMs),
      ].take(50).toList(growable: false);
    });
  }

  void _setError(String value) {
    if (!mounted) return;
    setState(() {
      _loading = false;
      _error = value;
    });
  }

  Future<void> _clearEvents() async {
    await _gateway.clearRecentEvents();
    if (mounted) setState(() => _events = const []);
  }

  Future<void> _copyEvidence() async {
    final evidence = const JsonEncoder.withIndent('  ').convert({
      'generatedAt': DateTime.now().toIso8601String(),
      'device': _device.toJson(),
      'permissions': _snapshot.toJson(),
      'eventCount': _events.length,
      'events': _events.map((event) => event.toJson()).toList(growable: false),
    });
    await Clipboard.setData(ClipboardData(text: evidence));
    if (!mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('验收证据 JSON 已复制到剪贴板')));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('停一下 · 阶段 1 Spike'),
        actions: [
          IconButton(
            tooltip: '刷新权限状态',
            onPressed: _loading ? null : _refresh,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _refresh,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            const _PurposeCard(),
            const SizedBox(height: 12),
            _DeviceCard(device: _device),
            const SizedBox(height: 12),
            _PermissionCard(
              title: '使用情况访问',
              description: '阶段 1 仅验证授权状态，后续用于校验使用时长。',
              granted: _snapshot.usageAccessGranted,
              actionLabel: '打开使用情况访问设置',
              onPressed: _gateway.openUsageAccessSettings,
            ),
            const SizedBox(height: 12),
            _AccessibilityDisclosure(
              granted: _snapshot.accessibilityEnabled,
              accepted: _disclosureAccepted,
              onAcceptedChanged: (value) =>
                  setState(() => _disclosureAccepted = value),
              onOpenSettings: _disclosureAccepted
                  ? _gateway.openAccessibilitySettings
                  : null,
            ),
            if (_loading) ...[
              const SizedBox(height: 12),
              const LinearProgressIndicator(),
            ],
            if (_error != null) ...[
              const SizedBox(height: 12),
              Text(
                _error!,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
            ],
            const SizedBox(height: 24),
            Row(
              children: [
                Expanded(
                  child: Text(
                    '前台包名事件（本地最近 50 条）',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                TextButton(
                  onPressed: _events.isEmpty ? null : _clearEvents,
                  child: const Text('清空'),
                ),
                IconButton(
                  tooltip: '复制验收证据',
                  onPressed: _events.isEmpty ? null : _copyEvidence,
                  icon: const Icon(Icons.copy_all_outlined),
                ),
              ],
            ),
            const Text('开启无障碍服务后，切换到任意第三方 App 再返回。日志不包含页面文字。'),
            const SizedBox(height: 8),
            if (_events.isEmpty)
              const Card(
                child: Padding(
                  padding: EdgeInsets.all(16),
                  child: Text('尚未收到有效事件'),
                ),
              )
            else
              ..._events.map(_EventTile.new),
          ],
        ),
      ),
    );
  }
}

class _DeviceCard extends StatelessWidget {
  const _DeviceCard({required this.device});

  final DeviceSnapshot device;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        leading: const Icon(Icons.smartphone),
        title: Text('${device.manufacturer} ${device.model}'),
        subtitle: Text(
          'Android ${device.androidRelease} / API ${device.sdkInt} / '
          'Build ${device.buildId}\nPauseNow ${device.appVersion}',
        ),
        isThreeLine: true,
      ),
    );
  }
}

class _PurposeCard extends StatelessWidget {
  const _PurposeCard();

  @override
  Widget build(BuildContext context) {
    return Card(
      color: Theme.of(context).colorScheme.primaryContainer,
      child: const Padding(
        padding: EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('本阶段只证明一件事', style: TextStyle(fontWeight: FontWeight.bold)),
            SizedBox(height: 6),
            Text('在用户明确授权后，稳定获得第三方应用进入前台的包名事件。不会拦截应用，也不会读取页面内容。'),
          ],
        ),
      ),
    );
  }
}

class _PermissionCard extends StatelessWidget {
  const _PermissionCard({
    required this.title,
    required this.description,
    required this.granted,
    required this.actionLabel,
    required this.onPressed,
  });

  final String title;
  final String description;
  final bool granted;
  final String actionLabel;
  final Future<void> Function() onPressed;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    title,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                _StatusChip(granted: granted),
              ],
            ),
            const SizedBox(height: 8),
            Text(description),
            const SizedBox(height: 12),
            FilledButton.tonal(onPressed: onPressed, child: Text(actionLabel)),
          ],
        ),
      ),
    );
  }
}

class _AccessibilityDisclosure extends StatelessWidget {
  const _AccessibilityDisclosure({
    required this.granted,
    required this.accepted,
    required this.onAcceptedChanged,
    required this.onOpenSettings,
  });

  final bool granted;
  final bool accepted;
  final ValueChanged<bool> onAcceptedChanged;
  final Future<void> Function()? onOpenSettings;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    '无障碍服务醒目披露',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                _StatusChip(granted: granted),
              ],
            ),
            const SizedBox(height: 10),
            const Text(
              '“停一下”使用无障碍服务，仅用于识别你主动选择的应用何时进入前台，并执行你预先设置的确定性规则。阶段 1 只记录事件时间、事件类型和应用包名。',
            ),
            const SizedBox(height: 8),
            const Text(
              '我们不会读取、保存或上传页面文字、聊天内容、输入内容、视频标题、账号或支付信息；不会代替你点击其他应用。你可以随时在系统设置中关闭此权限。',
              style: TextStyle(fontWeight: FontWeight.w600),
            ),
            CheckboxListTile(
              contentPadding: EdgeInsets.zero,
              value: accepted,
              onChanged: (value) => onAcceptedChanged(value ?? false),
              title: const Text('我已阅读并理解上述用途'),
              controlAffinity: ListTileControlAffinity.leading,
            ),
            FilledButton(
              onPressed: onOpenSettings,
              child: const Text('打开无障碍设置'),
            ),
          ],
        ),
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.granted});

  final bool granted;

  @override
  Widget build(BuildContext context) {
    return Chip(
      avatar: Icon(
        granted ? Icons.check_circle : Icons.error_outline,
        size: 18,
      ),
      label: Text(granted ? '已开启' : '未开启'),
    );
  }
}

class _EventTile extends StatelessWidget {
  const _EventTile(this.event);

  final ForegroundPackageEvent event;

  @override
  Widget build(BuildContext context) {
    final time = DateTime.fromMillisecondsSinceEpoch(
      event.detectedAtMs,
    ).toLocal();
    final formatted =
        '${time.hour.toString().padLeft(2, '0')}:'
        '${time.minute.toString().padLeft(2, '0')}:'
        '${time.second.toString().padLeft(2, '0')}';
    return Card(
      child: ListTile(
        dense: true,
        leading: const Icon(Icons.phone_android),
        title: Text(event.packageName),
        subtitle: Text('$formatted · ${event.eventType}'),
      ),
    );
  }
}
