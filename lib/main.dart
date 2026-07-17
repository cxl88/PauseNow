import 'package:flutter/material.dart';
import 'package:pausenow/features/permissions/permission_spike_page.dart';

void main() => runApp(const PauseNowApp());

class PauseNowApp extends StatelessWidget {
  const PauseNowApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: '停一下',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF2E6B62)),
        useMaterial3: true,
      ),
      home: const PermissionSpikePage(),
    );
  }
}
