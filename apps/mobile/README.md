# Sliding Tasks Mobile

Native Android client for the Sliding Tasks product API.

## Scope

- implements the native mobile experience;
- consumes the selected adapter boundary: `direct-api`;
- keeps domain authority in the backend or released adapter contract;
- records runtime assumptions before device-specific work starts.

## Development

Open `apps/mobile` in Android Studio, or use the Gradle wrapper from that
directory.

The default emulator base URL is `http://10.0.2.2:18080`.
Override it without editing source:

```bash
./gradlew assembleDebug -PSLIDING_TASKS_API_URL=http://192.168.1.20:18080
```

The first vertical slice renders today's ordered pending cards and translates
Done and Not today interactions into `Complete` and `Dismiss` command intents.
The sample board is intentionally local until the API transport contract exists.

Cards remain visible in a vertical list. Slide any card right to mark it done,
slide it left for not today, or release before the threshold to return it to its
list position. Cards translate horizontally without rotation. The buttons
provide equivalent non-gesture controls.

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
