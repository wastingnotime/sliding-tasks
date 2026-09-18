# Slice 008: sequence signal projection

## Question

Do factual reorder and jump observations become useful when viewed alongside a
card's eventual outcome?

## Contract

Project per-card counts of `CardReordered` and `CardJumped`, plus the factual
terminal outcome when one exists. The projection adds no behavioral labels and
does not infer causation.

## Done criteria

A replay with sequence actions and a terminal decision produces stable counts
that can be compared with untouched cards. The raw event stream remains the
source of truth.

## Out of scope

Hesitation/friction classification, causality, ranking, forecasting, and
automatic recommendations.

