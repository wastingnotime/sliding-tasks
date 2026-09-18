# Metric usefulness refinement receipt

The projection review found and corrected mutable-label leakage: historical
task metrics now use the first card-generation snapshot rather than today's
Task definition. The regression suite covers a title/type edit between days.

Validation: `16 passed`.

