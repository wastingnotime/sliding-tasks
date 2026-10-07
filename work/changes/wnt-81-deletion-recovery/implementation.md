# WNT-79 deletion Undo implementation

Both task removal entry points offer a localized Undo snackbar after the
removal is saved. The app retains only the latest removed task and its plan
position in memory. The five-second timer respects Android's recommended
accessibility timeout and survives tab navigation. Expiry or stopping the
activity ends the opportunity; reopening does not restore it.

Undo inserts the original task into the current state, preserving intervening
card actions and events, and appends `TaskRestored`. Restoring twice cannot
create duplicates. A one-time task completed or dismissed through its retained
card while removed is restored as resolved. Failed removal saves leave the
task untouched; failed Undo saves retain the action for another attempt within
the remaining window and show the existing save error.

Validation: the debug APK build and unit suite passed. New model tests cover
original task/order restoration, intervening card changes, immutable event
history, duplicate prevention, and one-time resolution during removal. Three
focused instrumentation tests passed on the API 34 emulator: Undo across tab
navigation with persisted order, replacement by another removal and expiry,
and an extended accessibility timeout followed by backgrounding. Storage
failure behavior was reviewed against the existing commit/save boundary;
these instrumentation checks do not inject storage failures.


## Swipe Undo correction

The owner clarified that Undo was expected after Done and Not today swipes.
Those successful card outcomes now use the same app-level snackbar as task
removal, including the equivalent accessibility actions. Undo restores the
same card to pending and prior one-time task flags, preserves current task
edits and other cards, and records `CardResolutionUndone`. The action is
localized in English and Brazilian Portuguese. Touching another card does
not replace Undo; another saved outcome or removal does.

Validation of this correction: debug APK build and unit suite passed. The
focused emulator test performs both right and left swipes, asserts that each
notice and Undo action is displayed, restores the original card and one-time
task state, and verifies expiry of a later swipe. The removal/navigation test
also passed with explicit visibility assertions after the shared Undo change.
