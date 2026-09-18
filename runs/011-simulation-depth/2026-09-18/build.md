# Actors, beams, and use cases build receipt

- Actors: Planner, User, DayBoundary, Analyst
- Behavior beams: 4
- Scheduled actor intentions: 8
- Runtime observations: 80
- Semantic layers: actor intention, command, use case, domain event, projection
- Validation: 26 passed; all updated EGD checks passed
- Prior EGD: superseded pending refinement
- Rebuild verification: 28 tests passed; all model checks passed; runtime
  supervision started successfully on port 8766 for an isolated smoke run.
- The existing observatory remains supervised on port 8765, so the default
  port was intentionally not interrupted during this build.
- Domain refinement: missed one-time cards now expose an explicit
  carry-forward/expire policy; the default path emits `TaskCarriedForward` and
  the expire experiment emits `TaskExpired`.
- Validation after policy refinement: 31 tests passed; model checks passed;
  runtime observations were 81 after carry-forward was recorded.
- Public-boundary refinement: `UpdateTask` is now an explicit application use
  case, exercised by the Planner beam and declared in the observatory; replay
  evidence now contains 84 observations.
