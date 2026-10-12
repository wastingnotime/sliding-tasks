# Guide validation recheck — October 12, 2026

Verified WNT-134 and WNT-135 against current main (`bd8a371`). Both
implementations are ancestors of the 0.3.5 release source (`8667b6d`).

The owner identified the earlier six failures as emulator configuration issues.
General `FirstUseGuideTest` cases assert English text. The dedicated Portuguese
test supplies its own Portuguese configuration, 2× text scale and dark mode.
The recheck used Android 14 Pixel `emulator-5554`, reporting `en-rUS` with no
app-specific locale override.

```bash
cd apps/mobile
ANDROID_SERIAL=emulator-5554 ./gradlew --no-daemon connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.wastingnotime.slidingtasks.FirstUseGuideTest
```

Result: **11 tests, zero failures, zero ignored; BUILD SUCCESSFUL**.

Coverage includes practice-plan editing, draft and saved-plan recreation,
cancel/back/skip and storage isolation; sequential task-type exercises;
resetting sliding practice when returning from Plan or Review; gestures and
Undo; final Review completion; restore and existing-user paths; and Portuguese
large-text dark-mode interaction. No product code changed for this recheck.

WNT-134's initial 15-test and WNT-135's initial 10-test evidence remains under
their respective `work/changes/` directories. Both Linear issues were still
marked In Review when checked. The 0.3.5 submission record establishes Play
submission, not current tester availability. WNT-117's unassisted tester
evidence is a separate follow-up.
