# Crashlytics alpha privacy policy promotion

The private alpha debug APK now includes Firebase Crashlytics. The Play release
bundle has no Crashlytics runtime classes or Firebase components in its merged
manifest. The app's offline privacy screen and this repository's public policy
source describe the alpha diagnostic reports.

The public `/sliding-tasks/privacy/` route still serves the September 23 policy
until `infra-platform` promotes the site image produced from commit
`e66a065d88e10572fbd87cacc64c80f6de6ae524`. Do not distribute the new
alpha APK before the public route shows the September 25 policy.

Validation:

- `./gradlew testDebugUnitTest assembleDebug verifyReleaseWithoutCrashlytics bundleRelease` passed.
- The alpha APK contains Crashlytics classes; the Play AAB does not.
- The debug merged manifest includes Firebase initialization and network access;
  the release manifest includes neither.
- An emulator test crash appeared in the Firebase Crashlytics dashboard.

Site promotion input: the `Build Sliding Tasks site image` workflow for source
commit `e66a065d88e10572fbd87cacc64c80f6de6ae524` succeeded. Its immutable
digest is `sha256:04ad5b022dba1c8d3c9bf08517e574fd3cc318c5798c2325449fbc01d231bb6d`.
The infra promotion is [PR #655](https://github.com/wastingnotime/infra-platform/pull/655)
for [issue #654](https://github.com/wastingnotime/infra-platform/issues/654),
coordinated by [campaign #6](https://github.com/wastingnotime/sliding-tasks/issues/6).
After the reviewed promotion deploys, verify the public route before uploading
the alpha APK.
