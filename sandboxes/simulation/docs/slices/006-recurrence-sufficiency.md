# Slice 006: recurrence sufficiency

## Question

Are deterministic recurrence rules sufficient for the present-day board before
introducing frequency targets such as N times per week/month?

## Contract

Opening a controlled date generates cards only for active regular tasks whose
recurrence occurs on that date. Daily, weekdays, weekends, specific weekdays,
and nth-day-of-month rules are evaluated without a wall-clock scheduler.

## Done criteria

End-to-end board generation demonstrates inclusion and exclusion across weekday,
weekend, and month dates. One-time carry-forward remains independent of
recurrence. Frequency targets remain out of scope.

