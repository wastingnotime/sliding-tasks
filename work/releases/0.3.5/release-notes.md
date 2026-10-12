# Sliding Tasks 0.3.5

- Restore swipe practice in guide step 2 by removing visible Done and Skip buttons.
- Keep Undo and card accessibility actions available.

Validation: the focused Portuguese, large-text, dark-mode guide instrumentation test passed, including swipe progression and absence of outcome buttons. The prior six failures occurred with the emulator configured for Portuguese while general guide tests expected English text. On October 12, the complete current guide suite passed with an English-configured Android 14 emulator: 11 tests, zero failures, including the test that explicitly sets Portuguese, 2× text and dark mode. See `guide-validation-recheck.md`.
