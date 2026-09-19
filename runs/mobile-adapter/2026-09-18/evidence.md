# Mobile Adapter Evidence

Date: 2026-09-18

## Scope

- product: `sliding-tasks`
- adapter authority: `direct-api`
- APK artifact: `sliding-tasks-mobile-debug-apk`

## Validation

- `make mobile-test`: passed locally (3 unit tests)
- `make mobile-build`: passed locally
- `make mobile-apk`: passed locally; `app-debug.apk` is 11,630,153 bytes
- `./gradlew connectedDebugAndroidTest`: passed on Pixel Android 14 AVD;
  right swipe advanced the stack from three cards to two
- CI workflow: `.github/workflows/build-mobile-apk.yml`

## Runtime

- emulator base URL: `http://10.0.2.2:18080`
- physical device validation: _not recorded_

## Contract Coverage

- Today decision loop: ordered pending-card list, independent card slides,
  slide threshold, snap-back, and empty-board UI states.
- Task lifecycle: complete intent removes the resolved card after success.
- One-time dismissal policy: dismiss intent removes the resolved card after success.
- Runtime evidence: touch intent preserves board contents and order.

Transport-backed loading, stale-record handling, authentication, and physical
device behavior remain unvalidated because the product API contract does not yet
define routes or DTOs.
