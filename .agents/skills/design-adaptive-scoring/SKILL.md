---
name: design-adaptive-scoring
description: Use when designing scoring, progression, difficulty adaptation, baselines, progress snapshots or recommendation logic.
---

# Design Adaptive Scoring

A scoring system must define:

- raw inputs
- normalized score
- confidence/reliability notes
- difficulty state
- progression rule
- regression/stagnation handling
- fatigue signals
- user-facing explanation
- analytics events
- tests

Default metrics:

- accuracy
- reaction time
- consistency
- error pattern
- difficulty level
- cognitive load rating
- improvement against own baseline

Do not expose scores as medical or clinical values.
