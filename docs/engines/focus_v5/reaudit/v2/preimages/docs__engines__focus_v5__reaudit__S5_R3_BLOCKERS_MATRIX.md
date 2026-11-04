# S5_R3_BLOCKERS_MATRIX — what keeps Focus V5 below each ceiling

```
REVIEW_CLASS: AI_ASSISTED_INDEPENDENT_REVIEW · HEAD 8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5
CEILING TODAY: R3 CANDIDATA (doc contract) · CONTRACT_INERT_NO_WIRING · not mounted, not frozen, not parity
```

Every row is OPEN. None was closed by this reaudit (doc/evidence-only). "Owner" is the phase that can close it.

| ID | Blocker | Class | Ceiling it gates | Owner phase | Closing test / criterion |
|---|---|---|---|---|---|
| EXT-AUDIT | R3 contract awaits an approving authority. This reaudit is `AI_ASSISTED_INDEPENDENT_REVIEW`, **not** the human/institutional sign-off that promotes CANDIDATA→approved. | STATICALLY_VERIFIED | R3 approved | CEO / external reviewer | Documented external reviewer identity + scope + verdict over this package |
| MB-FOCUS-0047 | R2 gate non-monotonic (falsified). **Reframed by this reaudit:** design-only (0 `.kt` hits); code already R3-monotone. Pending = seal R2-as-superseded in a versioned ADR + guarantee wiring never reintroduces `S_adm≤S_full`. | REPRODUCED (flip) + STATICALLY_VERIFIED (design-only) | ADR versioning; Paso 4 wiring | ADR register shows R2 `SUPERSEDED`; Paso 4 wiring PR contains no d′-recompute-under-exclusion; harness stays green |
| MB-FOCUS-0048 | R2 status-inflation + wiring PENDING: render-marker still gates classification (§R3-9/L1), subject-revocation NOT wired (GAP-1/GAP-2), ONSET_INTEGRITY not persisted, `seal()` keeps 5 optional temporal params, tallies `validate()` missing, thresholds/versions/tertiles/`TemporalCapture`/retention. | STATICALLY_VERIFIED (from ADR §R3-0) | Paso 4 wiring | Each sub-item has DECISION/IMPLEMENTATION/VALIDATION separated (ADR §R3-15) and an implemented+tested state |
| MB-FOCUS-0026 | Participant modality not resolved. | (ledger) | S6 mount | per ledger criterion |
| MB-FOCUS-0028 | Global recovery scanner not present. | (ledger) | S6 mount | recovery scanner + dedup + logical exactly-once tests |
| MB-FOCUS-0035 | Single-instance-per-user not enforced. | (ledger) | S6 mount | concurrency/replay tests |
| MB-FOCUS-0042 | UI/restore deferred. | (ledger) | S6 mount | restore UX + fail-closed reasons wired |
| IOS | iOS `BLOCKED_IOS` — no macOS host; compile/sim/device `NOT_CHECKED`. | BLOCKED | parity / TRAINING_READY / alpha | `T-TIMING-IOS` real evidence on a macOS runner |
| PHYS | Physical device matrix `NOT_RUN` (S6 design only). | NOT_RUN | any physical timing claim | `T-DEVICE-MATRIX` executed with criteria |
| W1 | Seal `SessionEligibility`. | DESIGNED_NOT_RUN | wiring | sealed type + test |
| W2 | Purge on sign-out. | DESIGNED_NOT_RUN | wiring | eraser coverage test |
| W3 | Product decision `TRAINING_FREE` (train without official measurement). | DECISION_PENDING (CEO) | wiring + honest UI | decided + UI reflects it |
| W4 | Exercise∈block registry. | DESIGNED_NOT_RUN | wiring | registry + test |
| W5 | `CalibrationBlueprint` + equating per device class. | DESIGNED_NOT_RUN | cross-device comparison | blueprint + equating test |

## Ceiling ladder (nothing may jump a rung without its blockers cleared)

```
C0 descriptive shadow  ── (now)     : R3 CANDIDATA, inert, Android internal tracer only
  ↑ EXT-AUDIT                        : R3 contract approved
  ↑ MB-FOCUS-0047/0048 + W1..W5      : candidate WIRED (Paso 4), still Android-only, still C0
  ↑ IOS + PHYS + T-DEVICE-MATRIX     : parity claim possible
  ↑ C1 evidence ladder (prereg…)     : any capability/transfer claim
```

No blocker on this list was cleared here. The reaudit changed only **documentation, tooling, and ledger**.
