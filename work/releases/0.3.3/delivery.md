# Sliding Tasks 0.3.3 closed-test delivery

On October 11, 2026, version 6 (0.3.3) was submitted to Closed testing - Alpha
with a 100% rollout and English and Brazilian Portuguese release notes.

Play confirms **Changes in review** for the Alpha release. Quick checks are
running; Google review and tester availability remain pending. Managed
publishing is off, so approved changes publish automatically. Version 5 (0.3.2)
was confirmed available to Alpha testers before this submission.

Source: `24aafdd19285152630ec7ded11f5e0df6c24c839` on main, including merged
PR #11 and its clean-install backup-test review fix.
[Signed build](https://github.com/wastingnotime/sliding-tasks/actions/runs/38149855236)
passed unit tests, the release Crashlytics exclusion check, bundle compilation
and signature verification. Play accepted the bundle and its ReTrace mapping.
Device support counts are unchanged. The only warning is missing native debug
symbols, as in the preceding release.

The full onboarding change passed 22 unit and 28 Android instrumentation tests
before merge. The reviewed backup regression additionally passed alone on a
clean Android 14 emulator installation for this release preparation; evidence
is retained in `backup-clean-install-validation.txt`.

Bundle and mapping digests are recorded in `artifacts.json`. Files are archived
locally under `build/releases/0.3.3/`, ignored by Git. CI artifacts expire after
seven days. Submission evidence is retained in `play-submission.jpg`.

WNT-131 tracks the completed onboarding implementation. WNT-117 remains pending
actual unassisted tester feedback. The earlier WNT-101 acceptance concerns
0.3.2 and is not new Play-installed validation of 0.3.3.

[Publishing overview](https://play.google.com/console/u/0/developers/8460748010140158851/app/4975120587850699857/publishing)
