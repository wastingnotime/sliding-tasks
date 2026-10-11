# Plan practice and Review completion order

Tracking: [WNT-134](https://linear.app/wastingnotime/issue/WNT-134).

The first-use guide explained task types but only offered hands-on sliding.
The owner also found Create your first task before the Review results.

The types stage now offers Try planning a task. It opens the real Plan editor,
where a learner enters a title, chooses Routine, Until decided or One-time,
and sets the schedule. Saving shows the practice plan and its behavior, with
options to edit it or continue to the three existing sliding examples. The
transition explains that those sample cards are used for sliding practice.
Plan practice is optional, preserving the existing direct path to sliding.

Practice plans live only in first-use preferences. The task store's codec
serializes them, but they never enter the operational store or its backups.
Saved plans and the active editor survive recreation; editor fields use saved
instance state for draft restoration. Cancel or Back discards unsaved edits.
Finish, Skip and restarting from About clear the practice plan and editor flag.

Create your first task (or Back to tasks for returning users) is the final item
in practice Review, after recent-day results, including expanded details.
Both English and Brazilian Portuguese strings are included.

The shared editor also avoids constructing an invalid weekly preview when the
last selected weekday is removed. The existing missing-day guidance remains
visible and saving is disabled until the schedule becomes valid.

## Validation

Debug app and instrumentation builds passed. All 15 targeted Android
instrumentation tests passed on an isolated Android 14 Pixel emulator.

Evidence and reviewed screenshots accompany this change. Tests cover draft and
saved-plan recreation, editing, cancel/back/skip, cleanup on completion/restart,
isolation from task/card/event storage, Review action order, and Portuguese
with 2× font scale in dark mode. Existing Plan and Review regression checks
cover regular task creation, weekly schedules, long intervals and persistence.

WNT-117 remains pending unassisted tester feedback. This change does not bump
the release version or update the Play testing track.
