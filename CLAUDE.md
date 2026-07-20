# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

PauseNow (停一下) is an Android short-video usage-intervention app. The product intervenes when a user opens a target app (e.g. Douyin/抖音), grants a time-limited "pass", and shows an intervention again when it expires. Docs, UI strings, and the first target market are Chinese.

The repo is currently in **Stage 3 (Alpha 产品化)**. Stage 1 proved foreground package-name detection after authorization; Stage 2 pivoted the main detector to a foreground-service UsageStats poll after OEM background restrictions made raw Accessibility events unreliable; Stage 3 adds onboarding, launchable-app selection, multi-rule editing, local persistence, today's report, data clearing, and diagnostics. Stage 1/2/3 real-device evidence remains documented in `docs/project-progress.md`; do not claim the broader production stage is complete without the documented device and long-duration checks.

Stack: **pure Kotlin native** - Jetpack Compose UI + Kotlin control engine, single process, no Flutter, no cross-process bridge. Local-first - no account, no server, no event upload. (The repo was originally Flutter+Kotlin; it was pivoted to pure native on 2026-07-17. Flutter artifacts are removed.)

## Commands

All builds run through Gradle from `android/` (PowerShell or bash):

```powershell
cd android
.\gradlew.bat testDebugUnitTest          # JVM unit tests (debouncer / exclusion / event bus / rule engine / pass / snapshot)
.\gradlew.bat lintDebug                  # Android lint
.\gradlew.bat assembleDebug              # -> android/app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat assembleRelease            # -> android/app/build/outputs/apk/release/app-release.apk (debug-signed, Alpha distribution)
```

Run a single Kotlin test class:

```powershell
cd android
.\gradlew.bat testDebugUnitTest --tests "com.pausenow.app.events.ForegroundEventBusTest"
```

Project PowerShell scripts (Windows-oriented):

- `scripts/check-stage1-static.ps1` - verifies the privacy invariants in the manifests (no `QUERY_ALL_PACKAGES`/`INTERNET`/`SYSTEM_ALERT_WINDOW`, `canRetrieveWindowContent="false"`, window-state events only). Exits non-zero on violation. Run this after touching `AndroidManifest.xml` or `pause_accessibility_service.xml`.
- `scripts/android-env-check.ps1` - full local pipeline: java/adb checks + `testDebugUnitTest` + `lintDebug` + `assembleDebug`.
- `scripts/run-stage1-device-check.ps1 -TargetPackage <pkg> -Iterations 20` - ADB-driven Stage 1 sampler. It is not the Stage 2/3 product-flow test.

Build baseline: Kotlin 2.3.20, AGP 9.0.1, JDK 17, compileSdk/targetSdk 36, minSdk 26, Jetpack Compose (BOM 2024.09.00), applicationId `com.pausenow.app`. Compose compiler comes from the `org.jetbrains.kotlin.plugin.compose` plugin (same version as Kotlin) - do not add a `composeOptions.kotlinCompilerExtensionVersion`.

## Architecture

### Single-process native (no bridge)

`MainActivity` is a `ComponentActivity` that sets `PauseNowNavHost()`. Stage 3 routes include onboarding, home, rules, rule editing, launchable-app picker, report, and settings. `SpikeScreen` remains the Stage 1/2 diagnostic surface; `HomeScreen` is the Alpha entry point. `SpikeViewModel` still owns the legacy diagnostic state and event stream; rule CRUD uses `RulesViewModel`, onboarding state uses `OnboardingStore`, and the detection layer reads `SharedPreferencesSnapshotStore`.

### Detection pipeline (two inputs)

`PauseAccessibilityService` is both the AccessibilityService receiver and the foreground-service host for the main detector.

**Primary detector (Stage 2/3)** - runs inside the foreground service because Accessibility events are throttled or frozen on many OEM ROMs once PauseNow leaves the foreground:

```
onServiceConnected
  └─ promoteToForeground()                   # startForeground(FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
  └─ startDetectionLoop()                    # Handler posts every DETECTION_INTERVAL_MS (3s)
       └─ snapshotStore.read()               # minimal synced snapshot (rules/settings)
       └─ queryCurrentForeground()            # UsageStatsManager.queryUsageStats + max(lastTimeUsed)
       └─ RuleEngine.evaluate(...)            # pure logic decision
       └─ InterventionLauncher.launchOpen / launchExpired
```

`queryCurrentForeground` queries a 60-second window and uses `lastTimeUsed`, not `UsageEvents MOVE_TO_FOREGROUND`, because the latter misses the current app after the user has stayed in it for more than a few seconds.

**Secondary/legacy input** - Accessibility events are still accepted when the app is foreground-visible and provide the real-time stream used by `SpikeScreen`:

```
onAccessibilityEvent
  └─ filter: TYPE_WINDOW_STATE_CHANGED only
  └─ EventDebouncer.shouldAccept(pkg)        # drop consecutive same-package events
  └─ PackageExclusionPolicy.shouldExclude(pkg) # drop self/system/launcher/keyboard/invalid names
  └─ ForegroundEventStore.append(record)      # SharedPreferences JSON, newest-first, max 50
  └─ ForegroundEventBus.publish(record)       # notify in-process listeners (event thread)
```

Key classes under `android/app/src/main/kotlin/com/pausenow/app/`:

- `accessibility/EventDebouncer` - holds `lastPackageName`; accepts a package only when it differs from the previous one.
- `accessibility/PackageExclusionPolicy` - excludes own package, `android`, system UI, settings, permission controller, launchers, input methods, and invalid package names.
- `accessibility/ForegroundEventStore` - persists the last 50 events to SharedPreferences (`pausenow_spike_events`) as JSON.
- `accessibility/ForegroundPackageEventRecord` - the only event shape: `packageName`, `eventType`, `detectedAtMs`.
- `events/ForegroundEventBus` - singleton observer registry (`register`/`unregister`/`publish`).
- `permissions/AndroidPermissionGateway` - Usage Access + Accessibility status checks and settings deep-links.
- `model/PermissionSnapshot`, `model/DeviceSnapshot` - snapshot data classes with JSON serialization for the evidence clipboard payload.
- `ui/SpikeScreen` (Compose) + `ui/SpikeViewModel` - the Stage 1 verification UI.

### Rule / pass / intervention flow

The pure-logic **Rule Engine** is unit-testable and lives in `rule/RuleEngine.kt`. It decides `Degraded`, `Ignore`, `Allow`, `RequireOpenIntervention`, or `RequireExpiredIntervention` from an `EvaluationInput` containing the current package, active pass, rules, and time.

- `pass/PassManager` + `pass/PassStore` - grant, extend, end, and persist active passes per package. `SharedPreferencesPassStore` stores a map of `packageName -> ActivePass` as JSON.
- `intervention/InterventionLauncher` - checks `InterventionState` (cooldown + in-flight mutex) before starting `InterventionActivity`.
- `intervention/InterventionState` - singleton in-memory anti-repeat: per-package cooldown and a mutex so only one intervention Activity is in flight at a time.
- `intervention/InterventionActivity` - the intervention page in `MODE_OPEN` or `MODE_EXPIRED`; user choices call `PassManager` directly, then return to the target app or to the home screen.
- `intervention/ExpiryController` + `ExpiryAlarmReceiver` - schedules an AlarmManager exact/alarm for pass expiry that posts a high-priority notification; the actual re-intervention is event-driven by the next detection loop iteration.
- `report/InterventionEventStore` - records `open`/`expired`/`grant`/`extend`/`end` events (SharedPreferences, newest 500) for `ReportScreen`'s today summary.

### Native Snapshot Store

The AccessibilityService must not depend on the UI being alive. The UI writes rules/settings to `snapshot/SharedPreferencesSnapshotStore`; the service reads the same store on each detection tick.

- `snapshot/ProtectionSnapshot` is schema v2: `rules: List<ProtectionRule>`, `settings: ProtectionSettings`, `updatedAt`, `schemaVersion`.
- `snapshot/ProtectionSnapshot.fromJson` migrates legacy v1 `protectedPackages` into a single rule on read.
- `snapshot/SharedPreferencesSnapshotStore` is the production implementation; tests inject a fake `SnapshotStore`.

### UI navigation and stores

`ui/nav/PauseNowNavHost.kt` defines Stage 3 routes: onboarding, home, rules, rule edit, app picker, report, settings. The start destination is determined by `OnboardingStore.isCompleted()`.

- `ui/screen/RulesViewModel` reads/writes `ProtectionRule` lists through `SharedPreferencesSnapshotStore`.
- `applist/AppRepository` loads launchable apps with `queryIntentActivities(MAIN+LAUNCHER)`, relying on the manifest `<queries>` declaration. It does not scan all installed packages.
- `onboarding/OnboardingStore` persists completion in SharedPreferences.
- `MainActivity` requests `POST_NOTIFICATIONS` at runtime on Android 13+.

The permission flow has a **disclosure gate**: the accessibility settings button is disabled until the user checks the "I have read and understood" checkbox (`disclosureAccepted` in `SpikeViewModel`, surfaced in `AccessibilityDisclosureCard`).

### Data model invariant

Only three fields may ever leave the detection layer: `detectedAtMs`, `eventType`, `packageName`. The app must not collect page text, node trees, input, video titles, chat content, account info, payment info, or full installed-package lists.

## Privacy & permission invariants

These are hard constraints, not just conventions - `scripts/check-stage1-static.ps1` enforces the manifest ones, and the docs list the code-level prohibitions explicitly:

- Accessibility config (`res/xml/pause_accessibility_service.xml`): `accessibilityEventTypes="typeWindowStateChanged"`, `canRetrieveWindowContent="false"`, `isAccessibilityTool="false"` only.
- Main manifest (and debug/profile variants): no `QUERY_ALL_PACKAGES`, no `SYSTEM_ALERT_WINDOW`, no `INTERNET`. Debug/profile manifests are intentionally empty overlays (no INTERNET) - do not add it back.
- The service observes package transitions through AccessibilityService and polls UsageStats in the foreground service for OEM reliability; it executes deterministic rules configured by the user.
- Stage 3 app selection queries only launchable activities via `MAIN` + `LAUNCHER` and the manifest `<queries>` declaration; do not add `QUERY_ALL_PACKAGES` or scan all installed packages.
- Kotlin must not call `getRootInActiveWindow()`, read `event.text`, traverse the node tree, perform auto-click/gestures, or upload anything.

When editing detection code, keep it observable-and-record-only.

## Repo layout gotchas

- **`android/`** - the active build: pure Kotlin native Android app (Jetpack Compose + Kotlin control engine, single process, local-first). This is the current mainline.
- **`deliberate-app/`** - the project marketing/landing site (Bun + web stack) for product promotion. Not part of the Android build.
- **`server/`** - the future backend service (RuoYi v3.9.2 SpringBoot+Vue scaffold). Will provide API for the app/frontend in later stages; not yet wired into the app. The architecture doc says don't build a server prematurely, so treat as parked until its stage.
- **`project.yml`, `Configuration/`, `docs/technical-solution.md`, `docs/day-1-3-acceptance.md`, `docs/family-controls-entitlement.md`** - legacy iOS Screen Time / XcodeGen exploration. Not the current Android route.
- The old Flutter artifacts (`lib/`, `test/`, `pubspec.*`, `.metadata`, `analysis_options.yaml`) were removed in the native pivot.
- **`docs/evidence/`** is gitignored (contains device serials/build fingerprints from real-device runs).

## Roadmap & stage gate

Staged plan from `docs/03_停一下_系统架构与项目推进方案.md`. Each stage has Go/Pivot/Stop criteria; do not skip ahead.

- **Stage 0** - project init. Done.
- **Stage 1** - permission + foreground event spike. Completed as a development gate; OEM background limitations and evidence are recorded in `docs/project-progress.md`.
- **Stage 2** - intervention + pass spike. Implemented with native `InterventionActivity`, foreground service, UsageStats polling, 5-min default pass, expiry notification/intervention, 3-min extension, native persistence, anti-repeat, and real-device closure on OPPO.
- **Stage 3** - Alpha productization. **Current.** Onboarding, launchable-app selection, multi-rule editing, rule/preference local persistence, today's report, data clearing, diagnostics, and debug-signed release APK are implemented; broader production validation remains.
- **Stage 4–6** - closed multi-device testing, Beta productization, commercial validation.

Architectural principles that carry across stages: detection/rule engine must **not** depend on the UI being in foreground (the AccessibilityService outlives the Activity); the AccessibilityService reads a **minimal synced snapshot** (`Native Snapshot Store`), never the UI-side DB directly; the **Rule Engine** should be pure logic so it unit-tests cleanly; rules/passes/events must all be persistable and recoverable across reboots.
