# Play Console findings — 2026-10-07

The Test and release overview flagged DEX obfuscation and edge-to-edge display
for the 0.3.0 closed test, plus third-party store listing preferences.

## Third-party store preferences

Sliding Tasks has an app-specific **Manage individually** override. Aptoide
Games and Galaxy Store are set to **Do not publish** while the app is in closed
testing. Reconsider these choices when preparing the public launch.

For new store requests, Play requires a response within 30 days of notification;
otherwise it automatically publishes the listing. Individual management does
not prevent publication indefinitely without a response. These preferences
were saved and read back in Play Console.

## DEX optimization

Version 0.3.1 (code 4) enables R8 shrinking, optimization, obfuscation, and resource
shrinking. The backup worker keeps its class name for existing WorkManager jobs.
The release artifact retains the obfuscation mapping. The release build passed
and the mapping confirms renamed application classes. Play must analyze an
uploaded replacement bundle before the console finding can be considered resolved.

## Edge-to-edge verification

`MainActivity` already calls `enableEdgeToEdge()`. Verification found and fixed
two editor issues:

- Scaffold padding was applied without consuming its insets. The editor then
  counted navigation bar padding again when applying keyboard insets, leaving
  a 115-pixel gap above the keyboard on the Android 14 emulator. Content now
  consumes Scaffold padding. A regression assertion failed before this fix.
- The editor header occupied fixed space above the scrollable form. With the
  Android 15 keyboard open, the title field was partially clipped. The header
  now scrolls with the form; Cancel and Add task remain outside the scroll area.

The focused `EdgeToEdgeTest` checks control bounds against system bar and display
cutout insets, keyboard clearance, full title field visibility, excess keyboard
spacing, and landscape navigation. Screenshots were inspected as well as the
automated assertions. The tested app is the debug build of 0.3.1, with target SDK
37, at 1080×1920 and density 480.

| Emulator | Navigation and display | Theme | Result |
| --- | --- | --- | --- |
| Android 14 / API 34 | Gesture navigation | Dark | Passed |
| Android 14 / API 34 | Three-button navigation and tall cutout | Dark | Passed |
| Android 15 / API 35 | Gesture navigation | Light | Passed |
| Android 15 / API 35 | Three-button navigation | Light | Passed |
| Android 15 / API 35 | Three-button navigation and tall cutout | Dark | Passed |

Each configuration includes portrait screen controls, the editor keyboard,
About export controls, and landscape navigation across Today, Plan, and Review.
This is emulator coverage; physical devices, other cutout shapes, large font
settings, and the release runtime were not checked.

Relevant Android guidance:
[Material 3 insets](https://developer.android.com/develop/ui/compose/system/material-insets),
[inset consumption](https://developer.android.com/develop/ui/compose/system/insets-ui).
