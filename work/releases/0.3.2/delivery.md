# Sliding Tasks 0.3.2 delivery

On October 10, 2026, version 5 (0.3.2) was submitted to Closed testing - Alpha
with a 100% rollout and English and Brazilian Portuguese release notes.

Play confirms **Changes in review** for all five changes: the Alpha release,
English and Brazilian Portuguese full descriptions, and phone screenshots for
both listings. Quick checks are running before review. Managed publishing is
off, so approved changes publish automatically. Availability of version 5 and
the refreshed listing remains pending Google checks and review. Version 4
(0.3.1) was confirmed available to selected Alpha testers before this submission.

Source: `e907334fb25bb86fe82e5c11edec01be3e2061e2`.
[Signed build](https://github.com/wastingnotime/sliding-tasks/actions/runs/38098339307)
passed unit tests, the Crashlytics exclusion check, bundle build, and signature
verification. Play accepted version 5 with its ReTrace mapping. The only warning
is missing native debug symbols. Supported device counts are unchanged.

Artifact SHA-256 digests are recorded in `artifacts.json`. The bundle and mapping
are archived locally in `build/releases/0.3.2/`, ignored by Git. CI artifacts
expire after seven days.

The store screenshots now show Today, Plan, and Review in that order. Both
listing descriptions explain Review and the backup controls. Screenshot evidence
is retained in `store-listing.jpg` and `play-submission.jpg`.

Tracking: WNT-130 owns the completed backup implementation; WNT-101 now targets
0.3.2 / code 5 for Play-installed device validation.

[Publishing overview](https://play.google.com/console/u/0/developers/8460748010140158851/app/4975120587850699857/publishing)
