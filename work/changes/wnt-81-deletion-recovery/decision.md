# WNT-81 task deletion recovery decision

Decision: offer Undo for about five seconds after a task is successfully
removed. The Undo action is available in a temporary message on the current
app screen. It remains available when the user switches between Today, Plan,
and Review during that window. There is no later in-app trash, history, or
menu action for restoring a removed task in this version.

The removal is saved immediately, as it is today. Its `TaskRemoved` event and
existing cards and history remain intact. If Undo is chosen before expiry,
restore the same task record, including its ID, schedule, active and resolved
state, and its former position among planned tasks. Do not create a second
task, regenerate or alter existing cards, or erase the removal event. Record
the restoration as a new event so the event history reflects both actions.
Only show Undo after the removal was saved successfully; a save failure leaves
the task in place and uses the existing error feedback.

The five-second window starts when removal succeeds. Expiry dismisses the
message and requires no second deletion operation. Leaving the app or losing
its process ends access to Undo; reopening the app does not restart the window.
If another task is removed while an Undo message is showing, the new removal
replaces that message and the earlier task's Undo opportunity ends. The
message and action must be accessible, with the window extended when Android
accessibility timeout settings require it.

This keeps removal semantics simple and gives users a chance to correct an
immediate mistake. A later recovery screen would need durable tombstones,
retention rules, and a separate place in the app to manage removed tasks.
Existing exported or automatic backups remain whole-state recovery tools;
they are not an in-app task trash.

WNT-79 implements this decision. It should verify restoration of the original
task and order, expiry, navigation, a second removal, save failure, and the
accessibility timeout behavior.
