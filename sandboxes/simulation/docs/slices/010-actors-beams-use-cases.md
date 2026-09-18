# Slice 010: actors, beams, and explicit use cases

## Problem corrected

The first environment was a scripted deterministic harness: scenario callbacks
called one large service directly, the runtime had no actors, and there were no
independent behavior streams. That was insufficient as an MRL simulation.

## Architecture

- Explicit application classes represent create/open/close/touch/complete/
  dismiss/reorder/jump/analytics use cases.
- Planner, User, DayBoundary, and Analyst are runtime-visible actors.
- Repository-owned behavior beams are deterministic streams of pressure that
  actors activate and schedule into the shared environment.
- Observations expose `actor_intention → command → use_case → domain_event`.
- The WNT runtime adapter wires the subject, actors, beams, invariant, and
  observatory graph; it does not own domain behavior.

The installed WNT Runtime has no first-class `Beam` primitive. `BehaviorBeam`
is therefore repository-owned and uses the runtime scheduler as its port.

## Done criteria

No initial scripted actions exist on the Scenario. Actors schedule all behavior,
control state exposes each actor, the event flow includes every semantic layer,
and the prior domain suite remains green.

## Observatory topology

Actors, behavior beams, individual use cases, the Task/Card aggregate, domain
event types, and analytics projection are declared as distinct ranked nodes.
Actor intentions carry explicit use-case targets, so runtime effect beams can
traverse declared structural paths instead of being discarded by the UI.
