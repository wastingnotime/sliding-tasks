# Task lifecycle refinement receipt

Deactivation was checked at the event boundary: the existing day retains its
single `CardGenerated` fact, while deactivation contributes only `TaskUpdated`.
Future suppression and later reactivation remain deterministic.

Validation: `22 passed`.

