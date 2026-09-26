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

## Languages

The app includes English (US) fallback strings in `app/src/main/res/values/`
and Brazilian Portuguese in `values-pt-rBR/`. Android chooses the device
language, and Android 13+ also offers an app-specific language choice in
system settings. Dates and weekday names use the active app locale; persisted
dates remain ISO strings. Keep both resource files complete when changing
user-facing copy, including accessibility text and the offline privacy notice.

## Saved data and upgrades

Play updates within `org.wastingnotime.slidingtasks` keep its private
`sliding_tasks` preferences. The reader accepts both the original recurrence
fields and the current schedule fields. If saved data cannot be decoded, the
app blocks writes and asks the user to keep app storage intact.

An empty installation can import a Sliding Tasks JSON state file from
**About → Restore tasks → Import saved tasks**. This supports moving data from
the earlier `com.wastingnotime.slidingtasks` debug package, whose private
storage Android cannot share automatically with the Play package. Import is
only offered while the current installation has no tasks, cards, or events;
the file is validated before its contents are saved.

## Alpha distribution with Firebase

Use Firebase App Distribution for the first private alpha with a small group
of trusted Android testers. This repository's Firebase project is
`sliding-tasks`; its Android app is registered as
`org.wastingnotime.slidingtasks` (nickname **Sliding Tasks Android**). Firebase
distributes the alpha builds and receives their crash reports.
The debug APK now includes Firebase Crashlytics for automatic crash and ANR
reports from alpha testers. Google Analytics is not included. Debug builds run
locally also send reports when online. The Firebase Android configuration in
`app/google-services.json` contains project identifiers, not credentials.
The release bundle excludes the Crashlytics SDK.

Build and upload the debug APK:

```bash
cd apps/mobile
./gradlew assembleDebug
```

In the Firebase console, open **App Distribution** for **Sliding Tasks
Android**, upload `app/build/outputs/apk/debug/app-debug.apk`, select the
intended testers, add short release notes, and distribute. Firebase sends
testers an invitation email. Release notes should say what to try and how to
send feedback. Increment `versionCode` in `app/build.gradle.kts` for each new
build testers should install as an update. Firebase App Distribution keeps
releases for 150 days, so retain any build that needs longer-term archiving
separately.

The debug APK is signed with Android's debug key. A later build signed with the
release key, or an install from Google Play, cannot update that installation in
place; the tester must uninstall and reinstall, which erases tasks stored on
that device. Use a consistent release signing key for successive Firebase
release APKs once testers need in-place updates. Do not distribute the debug
APK as a public release.

## Play testing and production

Use Play Console tracks as the tester group and launch readiness grow:

1. **Internal testing** for a small, Play-installed group and quick checks.
2. **Closed testing** for a broader, controlled beta.
3. **Open testing** when public sign-up is appropriate and Play makes it
   available for the account.
4. **Production** for the public release.

The Play Console workflow in this repository produces a signed Android App
Bundle (`.aab`) for Play testing and release. For personal developer accounts
created after November 13, 2023, Google currently requires a closed test with
at least 12 continuously opted-in testers for 14 days before production access
can be requested. Firebase alpha testers do not count toward that Play closed
test requirement. See [Google Play's testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465)
for current account and track rules.

The Play bundle must remain free of Firebase Crashlytics. The release workflow
checks the resolved release dependencies and fails before upload if Crashlytics
is present, including as a transitive dependency.

## Google Play upload bundle

Run the **Build signed Sliding Tasks release bundle** workflow on `main` to
produce a signed `.aab` artifact for Play Console internal testing. The workflow
uses a dedicated upload key from GitHub Actions secrets, runs release unit
tests, verifies the bundle signature, and records its SHA-256 digest. Download
the artifact from the workflow run within seven days. The bundle uses package
`org.wastingnotime.slidingtasks`, version code `2`, and version name `0.2.0`.

The repository secrets are `SLIDING_TASKS_UPLOAD_KEYSTORE_BASE64`,
`SLIDING_TASKS_UPLOAD_STORE_PASSWORD`, and
`SLIDING_TASKS_UPLOAD_KEY_PASSWORD`. The alias is `sliding-tasks-upload`.
Keep the original upload keystore and its password in private storage; GitHub
secrets cannot be read back. Never commit the keystore or passwords. Google
Play App Signing uses this as the upload key for the first release and future
updates; if it is lost, request an upload key reset in Play Console.

For a local signed build, set `SLIDING_TASKS_UPLOAD_KEYSTORE` to the absolute
keystore path, `SLIDING_TASKS_UPLOAD_STORE_PASSWORD`,
`SLIDING_TASKS_UPLOAD_KEY_PASSWORD`, and
`SLIDING_TASKS_UPLOAD_KEY_ALIAS=sliding-tasks-upload`, then run
`./gradlew testDebugUnitTest verifyReleaseWithoutCrashlytics bundleRelease` from
`apps/mobile`. The output is
`app/build/outputs/bundle/release/app-release.aab`.

The first vertical slice renders today's ordered pending cards and translates
Done and Skip interactions into `Complete` and `Dismiss` command intents.
Milestone 1 stores planned tasks, generated cards, decisions, and immutable
event history on the device. Daily mobile operation does not wait for an API
transport contract. Planning may gain optional synchronization later.

Use the bottom navigation to open **Plan**, where routines and unresolved
one-time tasks are listed. Tap **Add task** or a task's **Edit** action to open its form.
Choose **Routine** for a new card on every scheduled day, **Until decided** for
one occurrence across selected days of an active week, or **One-time** for a
single task that carries forward until acted on. Today's cards use the same
type labels, including **Until decided**. In **Today**, **Add one-time
task** remains a shortcut for an ad-hoc card. Act on today's cards there. Use
**Review** for this week's outcomes,
recent task patterns, a seven-day comparison, and expandable recent days.
Plan offers daily routines every 1–7 days, weekly routines with a separate card
on each selected day every 1–4 weeks, and once-per-week routines with one
occurrence across selected valid days every 1–4 weeks. The default is 1.
The Plan editor uses single-choice intervals; existing schedules with larger
intervals remain intact until changed.
Weekly selections default to weekdays; choose all days, weekends, or any
combination such as Tuesday and Friday. When N is greater than 1, choose the
first active day or week to align the cycle.

For **Until decided**, **Done** and **Skip this week** close the week's
occurrence. A card left open becomes missed at day end and can appear again on
the next selected day that week. For a weekly routine with separate selected
days, **Skip today** closes only that day's card.

For example, plan a vitamin every two days; a haircut every two weeks on
weekdays; separate Saturday pickup and Sunday drop-off routines anchored in the
same two-week cycle; and trash once per week on any day. Morning and night can
be included in the titles; the app does not schedule time-of-day reminders.
Routine and one-time are the stored task types in Milestone 1; **Until decided**
is a weekly schedule for a routine. The earlier Focus label had no distinct
behavior and stored Focus tasks migrate to Routine. One-time tasks can be
created in Plan or Today. Older paused one-time entries can be
resumed or removed there; resolved ones remain in local history.
Planned tasks can be edited, reordered by touching and holding an entry while
dragging it, or removed with confirmation. Move up and Move down remain available
as accessibility actions.
Edits update future cards while keeping existing card snapshots and history.
Reordering controls future board order; removal preserves cards and history
already recorded.
The app follows the Android system light or dark appearance setting.
Open **More → About** for the installed version, a quick guide, an introduction
to Wasting No Time, and the current privacy policy offline. The canonical
repository copy of the privacy policy is
[`docs/privacy-policy.md`](../../docs/privacy-policy.md),
with a public copy at `https://wastingnotime.org/sliding-tasks/privacy/`.

Review uses only cards stored on the device. Missed means a card was still open
when its day ended; Skipped is an explicit choice. Pattern rankings use the
last 14 full days, while the completion comparison uses the last seven full
days and the seven before them. Recent days can be opened to see the cards
behind the counts. Broader analytics remain planned for the web surface.

Cards remain visible in a vertical list. Slide any card right to mark it done,
slide it left to skip the current occurrence, or release before the threshold to return it to its
list position. Cards translate horizontally without rotation. Persistent action
buttons are intentionally omitted; assistive technologies receive equivalent
Mark done and context-specific Skip actions from each card.

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
