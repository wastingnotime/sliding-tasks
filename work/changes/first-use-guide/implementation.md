# Optional first-use practice guide

Tracking: [WNT-131](https://linear.app/wastingnotime/issue/WNT-131).

The owner reported that early testers understood the app after a personal
explanation of Routine, Until decided and One-time. The guide supplies that
explanation in the app. This is an implementation response, not evidence that
unassisted users understand it. WNT-117 remains pending actual tester feedback.

## Behavior

An empty installation offers Show me how, Start using the app and Restore
backup. Existing task or card/event history bypasses the welcome. Starting or
skipping remembers that choice per installation. Successful restore dismisses
the guide; failed or cancelled restore keeps all initial choices available.

Three stages explain the task types using localized cards, let the user practice
with the same swipe interaction used in Today (with visible outcome buttons and
Undo), and display the resulting practice history in the real Review component.
Routine Skip affects today, Until decided Skip closes the active week, and
One-time Skip closes the task. Completing the guide opens the first task editor
when the real store is empty, otherwise returns to existing tasks.

Back and Skip guide remain visible during practice. About includes a way to
restart the guide. Guide progress and practice date/decisions are cached in
separate first-use preferences, so interruption does not lose progress. Finishing
or skipping clears that progress. Practice state is rebuilt using the real task
engine and never enters LocalTaskStore, export files, or scheduled backups.
English and Brazilian Portuguese are supported; content scrolls for large text.

## Validation

- 22 JVM unit tests passed, including three practice-state tests.
- Debug application APK and instrumentation APK built successfully.
- Release Crashlytics exclusion check passed.
- All 28 Android instrumentation tests passed on an Android 14 Pixel emulator,
  including all six first-use tests and existing backup, gesture, planning,
  restoration, UX and edge-to-edge regressions.
- First-use checks cover immediate Start and re-entry, existing-history bypass,
  cancelled/invalid/successful restore via the actual activity-result callback,
  real right/left swipes, buttons, Undo, Review, recreation/persistence and
  isolation from operational task/card/event storage.
- Portuguese guide actions remained reachable with 2× text in dark mode.
- Reviewed the welcome, example types, practice and Review screenshots, including
  the Portuguese large-text captures. Evidence is in `screenshots/` and
  `validation/android-instrumentation.txt`.

The existing navigation tests explicitly skip the optional welcome before
checking normal app screens. A read-only fixture provider and intent stubbing
exist only in the test APK to exercise restore success and failure.

## Human follow-up

When WNT-117 resumes, ask a new tester to use Show me how without verbal help.
Observe whether they can predict each Skip scope, slide and Undo, find their
results in Review, and create a real task afterward. Also try the immediate
Start and Restore paths with returning users. Include TalkBack and physical
devices; automated interaction checks do not establish comprehension.

The release version is unchanged; this change has not been submitted to Play.
