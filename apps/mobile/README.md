# Sliding Tasks Mobile

Native Android app for local-first Sliding Tasks planning and daily use.

## Scope

- implements the native mobile experience;
- owns task planning and operational state locally for Milestone 1;
- remains usable without a network service;
- may later emit immutable events to a receive-only API;
- may later synchronize planning across devices without requiring connectivity
  for daily operation;
- leaves full analytics to a deferred web surface.

## Development

Open `apps/mobile` in Android Studio, or use the Gradle wrapper from that
directory.

The first vertical slice renders today's ordered pending cards and translates
Done and Not today interactions into `Complete` and `Dismiss` command intents.
Milestone 1 stores planned tasks, generated cards, decisions, and immutable
event history on the device. Daily mobile operation does not wait for an API
transport contract. Planning may gain optional synchronization later.

Use the bottom navigation to create recurring or one-time tasks in **Plan**, act
on generated cards in **Today**, and inspect the local event stream in
**History**. Recurring tasks currently support daily, weekdays, and weekends.
Routine and one-time are the only task types in Milestone 1; the earlier Focus
label had no distinct behavior and stored Focus tasks migrate to Routine.

Cards remain visible in a vertical list. Slide any card right to mark it done,
slide it left for not today, or release before the threshold to return it to its
list position. Cards translate horizontally without rotation. Persistent action
buttons are intentionally omitted; assistive technologies receive equivalent
Mark done and Not today actions from each card.

## Run on an emulator

Create at least one Android Virtual Device in Android Studio, then run from the
repository root:

```bash
make mobile-run
```

The command reuses a running emulator or starts the first configured AVD, waits
for it to boot, builds and installs the debug APK, and launches the app. Select
a specific AVD when needed:

```bash
make mobile-run MOBILE_AVD=Pixel_8
```

## Install on a physical device

Enable USB debugging, connect and authorize the Android device, then run:

```bash
make mobile-install
```

The command ignores running emulators. If multiple physical devices are
connected, select one explicitly:

```bash
make mobile-install DEVICE_SERIAL=<adb-serial>
```

## Validation

```bash
make mobile-test
make mobile-build
make mobile-apk
```

With an emulator running, validate the gesture itself with:

```bash
cd apps/mobile
./gradlew connectedDebugAndroidTest
```

GitHub Actions should build the debug APK and upload it as an artifact from
`.github/workflows/build-mobile-apk.yml`.

The local APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
