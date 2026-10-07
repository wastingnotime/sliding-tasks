# Sliding Tasks 0.3.0 delivery

Release source: `4845ad4`, incorporating swipe Undo fix `fc9e41e` and the
backup/export changes already on main. Version name `0.3.0`, version code `3`.
Artifact digests are recorded in `artifacts.json`.

## Validation

The debug unit suite, signed release bundle build, debug APK build, release
lint, and `verifyReleaseWithoutCrashlytics` passed. The AAB signature verified.
Play accepted the signed version 3 bundle. It reported two nonblocking warnings:
no deobfuscation mapping (the build does not enable code obfuscation) and no
native debug symbols for bundled dependency code.

## Firebase

The debug APK was distributed on October 7, 2026 to the two existing alpha
testers. The console shows release 0.3.0 (3), Invited 2 and Accepted 2. Release
notes describe swipe/removal Undo and backups and disclose Crashlytics, linking
the public privacy policy. The existing debug signing identity was retained.

Console: https://console.firebase.google.com/u/0/project/sliding-tasks/appdistribution/app/android:org.wastingnotime.slidingtasks/releases

## Google Play

The existing Closed testing - Alpha track received `3 (0.3.0) closed test`,
with English and Brazilian Portuguese notes and a 100% rollout to that track.
Review submission was confirmed. The final console shows **Changes in review**
for `3 (0.3.0) closed test` and **Start full rollout**. Automated quick checks
are running before review. Managed publishing is off, so the approved update
will publish automatically to the closed Alpha track. Version 3 is not yet
confirmed available to Play testers; Google's review remains pending.

Publishing: https://play.google.com/console/u/0/developers/8460748010140158851/app/4975120587850699857/publishing
