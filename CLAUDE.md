# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

PauseNow (停一下) is an Android short-video usage-intervention app. The product intervenes when a user opens a target app (e.g. Douyin/抖音), grants a time-limited "pass", and shows an intervention again when it expires. Docs, UI strings, and the first target market are Chinese.

The repo is currently in **Stage 1 (权限与前台事件 Spike)**: proving that, after explicit user authorization, the app can reliably capture foreground package-name events when third-party apps come to the front. Stage 1 deliberately does *not* intercept apps, show intervention UI, or read page content. Code and tooling are delivered; the stage gate requires ≥95% detection success on each of 3 real devices (see `docs/04_阶段1_权限与前台事件_Spike验证.md`).

Stack: **pure Kotlin native** - Jetpack Compose UI + Kotlin control engine, single process, no Flutter, no cross-process bridge. Local-first - no account, no server, no event upload. (The repo was originally Flutter+Kotlin; it was pivoted to pure native on 2026-07-17. Flutter artifacts are removed.)

## Commands

All builds run through Gradle from `android/` (PowerShell or bash):

```powershell
cd android
.\gradlew.bat testDebugUnitTest          # JVM unit tests (debouncer / exclusion / event bus)
.\gradlew.bat lintDebug                  # Android lint
.\gradlew.bat assembleDebug              # -> android/app/build/outputs/apk/debug/app-debug.apk
```

Run a single Kotlin test class:

```powershell
cd android
.\gradlew.bat testDebugUnitTest --tests "com.pausenow.app.events.ForegroundEventBusTest"
```

Project PowerShell scripts (Windows-oriented):

- `scripts/check-stage1-static.ps1` - verifies the privacy invariants in the manifests (no `QUERY_ALL_PACKAGES`/`INTERNET`/`SYSTEM_ALERT_WINDOW`, `canRetrieveWindowContent="false"`, window-state events only). Exits non-zero on violation. Run this after touching `AndroidManifest.xml` or `pause_accessibility_service.xml`.
- `scripts/android-env-check.ps1` - full local pipeline: java/adb checks + `testDebugUnitTest` + `lintDebug` + `assembleDebug`.
- `scripts/run-stage1-device-check.ps1 -TargetPackage <pkg> -Iterations 20` - ADB-driven spike sampler. Launches the target app N times, counts `PauseNow.ForegroundEvent` logcat lines, writes `docs/evidence/stage1-*/result.json`, fails below 95%.

Build baseline: Kotlin 2.3.20, AGP 9.0.1, JDK 17, compileSdk/targetSdk 36, minSdk 26, Jetpack Compose (BOM 2024.09.00), applicationId `com.pausenow.app`. Compose compiler comes from the `org.jetbrains.kotlin.plugin.compose` plugin (same version as Kotlin) - do not add a `composeOptions.kotlinCompilerExtensionVersion`.

## Architecture

### Single-process native (no bridge)

There is no Flutter and no MethodChannel/EventChannel. The Compose UI talks to the Kotlin control engine directly in one process. `MainActivity` is a `ComponentActivity` that `setContent { SpikeScreen() }`. `SpikeViewModel` (`AndroidViewModel`) holds the `AndroidPermissionGateway` and `ForegroundEventStore`, refreshes on resume, and registers a `ForegroundEventBus` listener (unregister on pause). The accessibility service pushes events to the bus on its event thread; the ViewModel's listener posts to the main thread to update a `StateFlow` consumed by Compose via `collectAsStateWithLifecycle`.

### Foreground event pipeline (Kotlin)

`PauseAccessibilityService` is the heart of Stage 1. The flow for each `AccessibilityEvent`:

```
onAccessibilityEvent
  └─ filter: TYPE_WINDOW_STATE_CHANGED only
  └─ EventDebouncer.shouldAccept(pkg)        # drop consecutive same-package events
  └─ PackageExclusionPolicy.shouldExclude(pkg) # drop self/system/launcher/keyboard/invalid names
  └─ ForegroundEventStore.append(record)      # SharedPreferences JSON, newest-first, max 50
  └─ ForegroundEventBus.publish(record)       # notify in-process listeners (event thread)
```

Key classes under `android/app/src/main/kotlin/com/pausenow/app/`:

- `accessibility/EventDebouncer` - holds `lastPackageName`; accepts a package only when it differs from the previous one. Lets a target app re-emit after the user leaves and returns, while collapsing repeats within one foreground session.
- `accessibility/PackageExclusionPolicy` - excludes own package, `android`, system UI, settings, permission controller, launchers, input methods, and anything not matching a valid package-name regex.
- `accessibility/ForegroundEventStore` - persists the last 50 events to SharedPreferences (`pausenow_spike_events`) as JSON. `append`/`recentEvents`/`clear` are guarded by a shared lock.
- `accessibility/ForegroundPackageEventRecord` - the only event shape: `packageName`, `eventType`, `detectedAtMs`.
- `events/ForegroundEventBus` - singleton observer registry (`register`/`unregister`/`publish`) replacing the old `EventChannel.StreamHandler`.
- `permissions/AndroidPermissionGateway` - Usage Access + Accessibility status checks and settings deep-links.
- `model/PermissionSnapshot`, `model/DeviceSnapshot` - snapshot data classes with JSON serialization for the evidence clipboard payload.
- `ui/SpikeScreen` (Compose) + `ui/SpikeViewModel` - the Stage 1 verification UI.

The permission flow has a **disclosure gate**: the accessibility settings button is disabled until the user checks the "I have read and understood" checkbox (`disclosureAccepted` in `SpikeViewModel`, surfaced in `AccessibilityDisclosureCard`).

### Data model invariant

Only three fields may ever leave the detection layer: `detectedAtMs`, `eventType`, `packageName`. The app must not collect page text, node trees, input, video titles, chat content, account info, payment info, or full installed-package lists.

## Privacy & permission invariants

These are hard constraints, not just conventions - `scripts/check-stage1-static.ps1` enforces the manifest ones, and the docs list the code-level prohibitions explicitly:

- Accessibility config (`res/xml/pause_accessibility_service.xml`): `accessibilityEventTypes="typeWindowStateChanged"`, `canRetrieveWindowContent="false"`, `isAccessibilityTool="false"` only.
- Main manifest (and debug/profile variants): no `QUERY_ALL_PACKAGES`, no `SYSTEM_ALERT_WINDOW`, no `INTERNET`. Debug/profile manifests are intentionally empty overlays (no INTERNET) - do not add it back.
- Kotlin must not call `getRootInActiveWindow()`, read `event.text`, traverse the node tree, perform auto-click/gestures, scan the installed-app list, or upload anything.
- The service only ever observes and records package transitions; it executes deterministic rules the user configured. (Rule execution is Stage 2+ - not yet built.)

When editing detection code, keep it observable-and-record-only.

## Repo layout gotchas

- **`RuoYi-Vue-master/`** - an unrelated SpringBoot+Vue (RuoYi v3.9.2) Java scaffold. It is *not* part of the Android build and *not* wired into the app. The architecture doc explicitly says don't build a server prematurely. Treat it as parked/unrelated unless the user says otherwise.
- **`project.yml`, `Configuration/`, `docs/technical-solution.md`, `docs/day-1-3-acceptance.md`, `docs/family-controls-entitlement.md`** - legacy iOS Screen Time / XcodeGen exploration. Not the current Android route.
- The old Flutter artifacts (`lib/`, `test/`, `pubspec.*`, `.metadata`, `analysis_options.yaml`) were removed in the native pivot. The active build is the `android/` Gradle project only.
- **`docs/evidence/`** is gitignored (contains device serials/build fingerprints from real-device runs).

## Roadmap & stage gate

Staged plan from `docs/03_停一下_系统架构与项目推进方案.md`. Each stage has Go/Pivot/Stop criteria; do not skip ahead.

- **Stage 0** - project init. Done.
- **Stage 1** - permission + foreground event spike. **Current.** Code done; gate = 3 devices each ≥95% detection, no content reading, no notable battery drain, build+lint+tests green.
- **Stage 2** - intervention + pass spike: native `InterventionActivity`, pre-open intervention, 5-min pass (±5s), expiry intervention, 3-min extension, return-to-desktop, native persistence, anti-repeat. Pivot if timed auto-popup is unreliable on a given OEM - fall back to a high-priority notification + intercept on next window interaction (never use policy-violating background launches).
- **Stage 3** - Alpha productization: onboarding, app-selection list, rule editing, rule/preference local persistence, today's report, data clear, diagnostics. (Originally "Flutter Alpha" with a Pigeon bridge - that plan is obsolete under the native pivot.)
- **Stage 4–6** - closed multi-device testing, Beta productization, commercial validation.

Architectural principles that carry across stages: detection/rule engine must **not** depend on the UI being in foreground (the AccessibilityService outlives the Activity); the AccessibilityService reads a **minimal synced snapshot** (`Native Snapshot Store`), never the UI-side DB directly; the **Rule Engine** should be pure logic so it unit-tests cleanly; rules/passes/events must all be persistable and recoverable across reboots.
