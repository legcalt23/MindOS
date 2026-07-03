# Architecture Overview

## Core Principle
The Bayesian Competitive Rating system is a multi-layered architecture where:
1. **Observed Performance** (task-level or match-level) is the sole driver of rating change.
2. **Measurement Confidence** modulates the magnitude of the update.
3. **Comparability** can reduce, freeze, or nullify the update.
4. **Integrity** can freeze, hold, or invalidate the session.
5. **Readiness, self-report, fatigue, Premium, and metacognition** NEVER affect rating.

## Data Flow
```
SessionEnvironmentSnapshot
        │
        ▼
┌─────────────────────┐
│   Motor Execution   │──► ObservedTaskPerformance
└─────────────────────┘
        │
        ▼
┌─────────────────────┐
│ Comparability Gate  │──► ComparabilityDecision
└─────────────────────┘
        │
        ▼
┌─────────────────────┐
│   Integrity Gate    │──► IntegrityGateDecision
└─────────────────────┘
        │
        ▼
┌─────────────────────┐
│  Measurement Conf.  │──► MeasurementConfidenceDecision
└─────────────────────┘
        │
        ▼
┌─────────────────────┐
│ Session Validity    │──► SessionValidityDecision
└─────────────────────┘
        │
        ▼
┌─────────────────────┐
│  Rating Update Alg  │──► RatingUpdateDelta + SigmaUpdateDelta
└─────────────────────┘
        │
        ▼
┌─────────────────────┐
│    Audit Log        │──► Immutable record
└─────────────────────┘
```
