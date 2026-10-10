# Backup status and manual backup — 2026-10-10

The Backup & restore screen leads with the configured backup controls so the
last successful backup date and time are easier to find. Before the first
successful write, it explicitly says no backup has completed yet.

Back up now reuses the persisted folder permission and the existing backup
worker, JSON format, safe temporary-file publication and seven-copy retention.
It does not open a file picker. The UI observes the immediate WorkManager job,
shows Saving backup while pending, and disables duplicate requests and folder
changes during that job. A failed write preserves the last successful date and
shows the existing failure message. English and Brazilian Portuguese supported.
Export to a separately chosen file remains available below these controls.

WNT-72 was accepted by the account owner and closed in Linear. WNT-101 remains
for the updated Play-installed candidate; no release has been published for
this change.

Validation: debug unit tests, app APK and instrumentation APK builds passed.
Three Android 14 emulator tests passed (backup failure regression and two existing UX checks). The regression checks that manual backup uses the stored folder
and a failed write preserves the previous successful timestamp. Debug lint
reports 18 existing LocalContextGetResourceValueCall errors in unchanged
resource lookups; the new manual-backup message uses stringResource. Successful
document-provider writes on a physical device remain part of WNT-101.
