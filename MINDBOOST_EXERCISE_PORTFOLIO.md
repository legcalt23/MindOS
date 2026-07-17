# MindBoost Exercise Portfolio — canonical view (GENERATED from the derived model)

> **GENERATED — do NOT hand-edit.** Regenerate with `node scripts/portfolio/build-portfolio.mjs`,
> then `node scripts/portfolio/validate-portfolio.mjs`. A byte-exact gate guards this file.
>
> **What this view is.** Every number below is derived from independent authorities:
> editorial (`MINDBOOST_EXERCISE_PORTFOLIO.csv` — which slots exist and what they are assigned to),
> code-observation (`PORTFOLIO_CODE_OBSERVATIONS.json` — per-axis semantic bindings in hashed source),
> runtime/QA (`PORTFOLIO_RUNTIME_EVIDENCE.json` — executed runs only), and scientific review.
> No authority may attest another, and this view introduces no fact its inputs do not carry.
>
> **What "static components verified" does NOT mean.** It means the declaration, mechanic and generator
> were each bound to a real semantic construct in hashed source. It does **not** mean the exercise is
> reachable, playable, tested, measurable or ready to ship. Those are separate axes, each reported below.
>
> **UNRESOLVED is not absence.** It means the available strategy cannot resolve that architecture.
>
> **Retired.** The V2c status columns (`implementationStatus`, `mechanicImplemented`, `launchReadiness`, …)
> were moved to `PORTFOLIO_V2C_LEGACY_CLAIMS.csv` (`HISTORICAL_UNVERIFIED_CLAIMS`). They govern nothing:
> the numbers they produced are no longer expressible by this pipeline.

## Metrics (/121 target slots)

| Metric | Value | Formula | Means |
|---|---|---|---|
| Target slots | 121 | `slots.length` | The denominator: 8 blocks × 15 + EXTRA-121. Auxiliaries are never counted here. |
| Assigned candidates | 84/121 | `assignment != TARGET_UNASSIGNED` | A human editor assigned this slot to a code id or a design id. Says NOTHING about whether the code exists. |
| Code-assigned | 65/121 | `assignment == CODE_ASSIGNED` | The editorial authority names a codeExerciseId for this slot. Not a claim that the code was found. |
| Static components verified | 9/121 | `staticComponents == VERIFIED_PRESENT` | Every component axis (declaration, mechanic, generator) was bound to a real semantic construct in hashed source. This is NOT live, NOT reachable, NOT playable, NOT implemented-and-working, NOT tested. |
| Static components unresolved | 56/121 | `staticComponents == UNRESOLVED` | The slot names code, but at least one component axis could not be resolved by the available strategy. UNRESOLVED means "our strategy cannot resolve this architecture" — it is NOT evidence of absence. |
| Design-only assigned | 19/121 | `assignment == DESIGN_ONLY_ASSIGNED` | A design exists and no code is claimed. No code observation is possible or expected. |
| Target unassigned | 37/121 | `assignment == TARGET_UNASSIGNED` | The slot exists in the target of 121 and has neither a design nor code. Genuinely empty. |
| Exercise route verified | 0/121 | `exerciseRoute == VERIFIED_PRESENT` | This specific exercise id was proven to be dispatched. Block reachability does NOT satisfy this axis. |
| Renderer verified | 0/121 | `renderer == VERIFIED_PRESENT` | A renderer construct was bound for this id. Several blocks dispatch on state, not on id, so this axis is architecturally unresolvable there. |
| Runtime verified | 0/121 | `runtime == VERIFIED` | An executed run in the runtime authority verified this slot. Never inferred from static presence. |
| Distinctness approved | 0/121 | `scientificDistinctness == APPROVED` | A human scientific review approved this exercise as distinct from its siblings. |
| TRAINING_READY | 0/121 | `readiness == TRAINING_READY` | Derived only. Requires positive evidence on every axis; any UNRESOLVED, NOT_RUN, UNKNOWN or BLOCKED_PLATFORM blocks it. |

## Per-block

| Block | Slots | Assigned | Code-assigned | Static verified | Static unresolved | Design-only | Unassigned | Route verified | Runtime verified | TRAINING_READY |
|---|---|---|---|---|---|---|---|---|---|---|
| Enfoque | 15 | 13 | 13 | 0 | 13 | 0 | 2 | 0 | 0 | 0 |
| Velocidad | 15 | 8 | 8 | 0 | 8 | 0 | 7 | 0 | 0 | 0 |
| Logica | 15 | 10 | 4 | 0 | 4 | 6 | 5 | 0 | 0 | 0 |
| Lenguaje | 15 | 8 | 6 | 6 | 0 | 2 | 7 | 0 | 0 | 0 |
| Memoria | 15 | 15 | 15 | 3 | 12 | 0 | 0 | 0 | 0 | 0 |
| Decisiones | 15 | 7 | 2 | 0 | 2 | 5 | 8 | 0 | 0 | 0 |
| Emociones | 15 | 15 | 15 | 0 | 15 | 0 | 0 | 0 | 0 | 0 |
| Creatividad | 15 | 8 | 2 | 0 | 2 | 6 | 7 | 0 | 0 | 0 |
| EXTRA (extra) | 1 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 |
| **Total** | **121** | **84** | **65** | **9** | **56** | **19** | **37** | **0** | **0** | **0** |

## Runtime

Authority: `PORTFOLIO_RUNTIME_EVIDENCE.json` · state: **NOT_RUN** · runs recorded: 0 · slots runtime-verified: 0

NOT_RUN by ABSENCE OF A VALID RUN — derived from the runtime authority, never hard-coded and never inferred from static presence

## TRAINING_READY

**0/121.** DERIVED from the readiness rule over every axis. Never read from any authority file.

TRAINING_READY requires POSITIVE evidence on every TRAINING axis (assignment · staticComponents · renderer · exerciseRoute · runtime · platform.* · qa.* · scientificDistinctness). UNRESOLVED, NOT_RUN, BLOCKED_PLATFORM or missing evidence each block it. measurement is NOT a training axis (MB-V2D-R09). Derived only — never read from an authority file.

## Auxiliaries (4, excluded from the 121)

| auxId | kind | codeExerciseId | distinctnessReviewStatus | reasonExcluded |
|---|---|---|---|---|
| AUX-DECIDE-CHECKIN | context_checkin | CheckIn | APPROVED | entry gate / availability check-in; not a standalone training exercise |
| AUX-LOGIC-CONDITIONAL | alternate_form | CONDITIONAL_TEST | PROVISIONAL | conditional alternate form of Cadena Deductiva (design #1); dedup NOT scientifically verified — could split if distinct construct/scoring proven |
| AUX-MEMORY-EX16 | overlay | EX16_CALIBRACION_CONFIANZA | PROVISIONAL | transversal confidence-calibration overlay; not a standalone exercise; 16th code id over the 15-slot base; NOT an EXTRA-121 candidate |
| AUX-LANG-MIXED | composite | mixed_session | APPROVED | mixed/composite session over the 6 language exercises; not a distinct mechanic |

