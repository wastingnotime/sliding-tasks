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

Use the bottom navigation to open **Plan**, where recurring routines are listed
first. Tap **Add routine** or a routine's **Edit** action to open its form. In
**Today**, tap **Add one-time task** for an ad-hoc card, then act on today's
cards. Use **Review** for this week's outcomes,
recent task patterns, a seven-day comparison, and expandable recent days.
Routines support daily, weekdays, weekends, every two days, once
per week, and once per two weeks. Weekly routines can be available any day,
on weekdays, on Saturday, or on Sunday. **Done** and **Not today** close that
week's occurrence; a missed card appears again on the next available day.
The next matching week starts a new occurrence. For two-week
routines, choose a date in the first active week to align the alternating weeks.
For every-two-days routines, choose the first active day.

For example, plan a vitamin every two days; a haircut every two weeks on
weekdays; separate Saturday pickup and Sunday drop-off routines anchored in the
same two-week cycle; and trash once per week on any day. Morning and night can
be included in the titles; the app does not schedule time-of-day reminders.
Routine and one-time are the only task types in Milestone 1; the earlier Focus
label had no distinct behavior and stored Focus tasks migrate to Routine.
One-time tasks are created on Today. Older paused one-time entries can be
resumed or removed there; resolved ones remain in local history.
Planned tasks can be edited, reordered by touching and holding an entry while
dragging it, or removed with confirmation. Move up and Move down remain available
as accessibility actions.
Edits update future cards while keeping existing card snapshots and history.
Reordering controls future board order; removal preserves cards and history
already recorded.
The app follows the Android system light or dark appearance setting.

Review uses only cards stored on the device. Missed means a card was still open
when its day ended; Not today is an explicit choice. Pattern rankings use the
last 14 full days, while the completion comparison uses the last seven full
days and the seven before them. Recent days can be opened to see the cards
behind the counts. Broader analytics remain planned for the web surface.

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
