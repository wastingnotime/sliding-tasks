# WNT-84 backup and restore release decision

Decision: include task export in the first public version before WNT-73's broad
production rollout. With Android device backup disabled, a user otherwise has
no in-app way to create the file required by Restore tasks. This is a data-loss
risk for anyone who clears storage, uninstalls, or loses a device.

The implementation exports the complete local state through Android's system
document picker as version 1 JSON. After a one-time folder choice, automatic
backups use the same format approximately daily and retain seven copies. The
existing import reads that format and legacy unversioned state files. Restore
is still limited to an installation without tasks, cards, or events. The app
remains usable offline; destination storage is chosen by the user and may be
a provider that syncs files.

The Android instrumentation round trip passed on a Pixel 8 API 34 emulator on
October 6, 2026. The automatic backup setup also wrote a version 1 JSON file
to a chosen folder on that emulator, recorded its success, and scheduled the
next periodic job for approximately 24 hours later. The first public release
should include this change after the updated privacy policy is published.
This is a product risk decision, not a claim that Google Play requires an
export feature.
