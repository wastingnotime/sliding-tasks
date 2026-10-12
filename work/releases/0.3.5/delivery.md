# Sliding Tasks 0.3.5 closed-test delivery

On October 12, 2026, version 8 (0.3.5) was submitted to Closed testing - Alpha
with a 100% rollout and English and Brazilian Portuguese release notes.

Play confirms **Changes in review** for `8 (0.3.5) closed test`, with
**Start full rollout** and **1 change sent for review**. Quick checks are running
before Google review. Managed publishing is off, so approved changes publish
automatically. Tester availability of 0.3.5 remains pending. Version 7 (0.3.4)
was confirmed available to selected testers before submission.

Source: `8667b6d5e32d5bd30202f29cc229efcabffa7f17` on main, including the
swipe practice fix in `72d2cc1`.
[Signed build](https://github.com/wastingnotime/sliding-tasks/actions/runs/38284533048)
passed unit tests, the Crashlytics exclusion check, bundle compilation and
signature verification. Play accepted the bundle with its ReTrace mapping.
Device support counts are unchanged. The only warning is missing native debug
symbols, as in previous releases.

The focused Portuguese large-text dark-mode guide instrumentation test passed
before release preparation, checking swipe progression, absence of visible
Done/Skip buttons, and the card's two accessibility actions. The earlier full
guide suite had six failures on the Portuguese emulator. A subsequent complete
guide rerun passed all 11 tests with the emulator's default locale set to English;
the Portuguese large-text dark-mode test sets its own configuration. These were
test-environment failures, not outstanding product defects; see
`guide-validation-recheck.md`.

Bundle and mapping digests are recorded in `artifacts.json`. Files are archived
locally under `build/releases/0.3.5/`, ignored by Git. CI artifacts expire after
seven days. Submission evidence is retained in `play-submission.png`.

[Publishing overview](https://play.google.com/console/u/0/developers/8460748010140158851/app/4975120587850699857/publishing)

## Availability and production readiness recheck

On October 12, the owner's Brave `wasting no time` profile confirmed version
`8 (0.3.5) closed test` is **Available to testers on Google Play**, with a full
rollout to 178 countries/regions synced with production. Bundle 8 is Active.
Publishing overview reports last published October 12, with managed publishing
off. This supersedes the pending-availability statement above.

The production track remains Inactive. **Create new release** is available,
and the dashboard lists production countries/regions as completed. The remaining
dashboard steps are creating a release, previewing/confirming it, sending it to
Google for review, and publication. No production release was created or
submitted during this read-only check. WNT-117's real tester feedback remains
the project's only open issue; it is a product-readiness follow-up, not a
displayed Play Console production-access restriction.
