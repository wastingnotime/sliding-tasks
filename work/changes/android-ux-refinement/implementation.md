# Android UX refinement

Linear: [P-WNT-21](https://linear.app/wastingnotime/project/sliding-tasks-clear-minimal-android-ux-164d4d0ae0e3)

## Intent

Minimum necessary UI, clean visual hierarchy, and quick daily operation.
Preserve offline operation, recurrence semantics, gesture actions, Undo, and
English/Brazilian Portuguese support.

## Implementation

- WNT-111: Today uses a compact heading/date/count. Task titles lead compact,
  flat cards. One shared direction hint replaces repeated gesture sentences;
  each card retains its task type and exact skip scope.
- WNT-112: Darker light-theme orange supports text contrast. Navigation selection
  uses explicit green container colors in both themes instead of default purple.
- WNT-113: Tap a Plan entry to edit. Active/Paused labels explain switches;
  a drag handle indicates reorder; overflow contains Edit and Remove. Removal
  confirmation, Undo, and custom accessibility reorder actions remain.
- WNT-114: Schedule controls wrap instead of scrolling sideways. An outcome
  preview describes cadence, valid days, closure behavior, and interval anchor.
  The preview scrolls with the form so keyboard and large text do not crowd Save.
- WNT-115: Review combines its initial learning placeholders. More exposes a
  dedicated Backup & restore view; About retains guide, version, and privacy.
- WNT-116: Existing UI checks follow the new interaction paths; focused checks
  cover contextual card labels, direct backup discovery, schedule previews and
  initial Review. A human tester protocol is included below.

## Tester protocol

Use a fresh installation or disposable test data. Give the task without explaining
how to use the UI. Record completion, time, hesitation, wrong actions, and whether
help was needed. Observe at least one new user and one returning user.

1. Create a task and mark it done. Then undo the action.
2. Before acting, predict what Skip today, Skip this week and Skip task do.
   Check understanding of the next day/week and one-time closure.
3. Schedule a task for Tuesday and Friday every two weeks. Read the preview and
   explain when it will appear. Then create a weekly occurrence that can be
   handled on any weekday.
4. Pause a plan, edit it, reorder it, then remove and undo removal.
5. Find backup/export without hints.
6. Repeat key flows in Portuguese, dark mode, and large system text. Include a
   TalkBack user when possible; verify Done, Skip and reorder actions.

Success signals: first task completed without coaching, correct skip predictions,
intended schedule saved without help, no accidental destructive actions, and no
clipped essential controls. Tester results are pending; automated checks cannot
establish comprehension or speed.

## Android guidance

- [Content structure](https://developer.android.com/design/ui/mobile/guides/layout-and-content/content-structure)
- [Accessibility and contrast](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility)
- [Gesture accessibility actions](https://developer.android.com/develop/ui/compose/accessibility/semantics)

## Validation

Light primary `#B63820` has calculated WCAG relative-luminance contrast of
5.87:1 against white and 5.59:1 against `#FFF8F3`. The retained dark primary /
on-primary pair measures 7.67:1. These checks cover these explicit color pairs,
not a complete accessibility audit.

- `./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest`: passed;
  19 unit tests, zero failures.
- Full `AndroidJUnitRunner` run on the Android 14 Pixel emulator: 21 tests passed,
  including removal Undo, recurrence editing, persistence, gesture outcomes,
  system insets/keyboard, and the two UX refinement scenarios.
- Focused UX scenarios also passed in light mode (2 tests).
- Visually inspected Today, Plan, weekly editor, Review and backup screenshots in
  dark mode, plus light Today/Plan and Portuguese Today. Screenshots are retained
  under `screenshots/`; task titles are disposable English test fixtures.
- No release was published. Large-text, physical-device and TalkBack usability
  remain explicit tester follow-up in WNT-117; the automated checks do not
  substitute for those observations.

Initial validation failures were corrected by updating the UI tests to scroll to
wrapped choices, target the compact text, and wait for overflow animation before
confirming removal. The removal tests still assert persisted state and Undo
expiry/restore behavior. An initial overlapping Gradle invocation caused a
transient build-output collision; serial builds passed afterward.
