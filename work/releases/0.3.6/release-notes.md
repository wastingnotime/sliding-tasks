# Sliding Tasks 0.3.6

- After restoring tasks and history, open Backup & restore when automatic backups are off and explain that a backup folder must be selected again.
- Preserve an existing backup configuration. Include English and Brazilian Portuguese messages.

Maintenance fix: [WNT-159](https://linear.app/wastingnotime/issue/WNT-159), [PR #15](https://github.com/wastingnotime/sliding-tasks/pull/15).

Validation: debug unit tests and Android 14 emulator regression tests passed for restore with and without an existing backup configuration. Signed release workflow validates unit tests, release dependency exclusion of Crashlytics, and bundle signature.

Target: Google Play internal testing, versionCode 9. Production promotion is separate.
