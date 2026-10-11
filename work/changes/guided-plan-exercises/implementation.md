# One Plan exercise per task type

Tracking: [WNT-135](https://linear.app/wastingnotime/issue/WNT-135).

Owner review of WNT-134 asked to practice each task type immediately after its
explanation. The owner clarified that Step 2 should start fresh when returning
from Plan or Review.

## Behavior

Step 1 now presents Routine, Until decided and One-time as three sequential
exercises. Each explains its scope and provides a concrete example with
instructions for the real Plan editor. Save requires the exercise's task type;
a visible hint explains a type mismatch. Titles and valid schedules can be
customized. The saved plan shows its resulting schedule before Next exercise.
The third result leads to sliding practice. Skip guide remains available.

Each new exercise starts with an empty editor. Back from the editor cancels;
Back between Plan exercises opens a fresh attempt at the previous type. The
current exercise, saved plan and interrupted editor retain the existing
recreation support. Practice state is isolated from operational tasks and
backups, and cleared on finish, skip or guide restart. Older single-exercise
practice plans do not count as completing one of the new exercises.

Entering Step 2 from Plan or returning from Review clears previous sliding
decisions. Both the visible Back button and Android Back use the guide's
navigation handler. Normal card progression, Undo and interrupted-session
recovery within Step 2 are preserved. Review still ends with the completion
button below all results.

Step 2 teaches the gesture itself: visible Done/Skip buttons are removed, and
English/Portuguese instructions direct the learner to slide right or left.
Undo remains visible. The shared card retains its custom accessibility actions
so assistive-technology users can resolve practice cards.

Copy is available in English and Brazilian Portuguese. Exercise transitions
reset the explanation's scroll position.

## Validation

Debug app and test builds passed. All 10 targeted instrumentation tests passed
on an isolated Android 14 Pixel emulator.

Focused Android tests cover the sequential exercises, required type, fresh
forms, recreation, cancel/back/skip, isolation from real storage, resetting
Step 2 from Plan and Review, swiping/Undo, restore paths, final Review ordering
and Portuguese at 2× font scale in dark mode. Existing Plan/Review checks cover
the unrestricted real editor. Results and screenshots accompany this record.

WNT-117 remains pending unassisted tester evidence. No Play release is included.
