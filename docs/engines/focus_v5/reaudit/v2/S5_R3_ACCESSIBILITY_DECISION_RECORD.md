# S5_R3_ACCESSIBILITY_DECISION_RECORD — modality equivalence (Part 9C)

The verified defect (F-05 / L7-01): `FocusTracerScreen.kt:135-138` `contentDescription` announces the correct
flanker direction AND the congruency condition — under a screen reader the conflict is voided and the answer is
given. So **accuracy is NOT modality-fair** as the `FocusEvidenceStatePolicy` KDoc claims.

Honest re-split of the evidence class (per mandate Part 7):
- **A · ANNOUNCEMENT_CONTENT_VERIFIED_IN_SOURCE** — the string literally encodes the answer + congruency (static source, verified).
- **B · MEASUREMENT_EQUIVALENCE_FAILURE = STRONG_EXPERT_INFERENCE_PENDING_EMPIRICAL_VALIDATION** — that this destroys construct validity for AT users is a strong inference, not yet empirically measured.

## Provisional status by modality (until Paso 4/6 + a11y+efficacy design)

| modality | status | rationale |
|---|---|---|
| visual (default) | measurement-eligible (subject to other gates) | the construct as designed |
| TalkBack (Android SR) | **TRAINING_ONLY / UNMEASURED** | answer announced → not comparable to visual norms |
| VoiceOver (iOS SR) | **TRAINING_ONLY / UNMEASURED** | same; iOS build BLOCKED anyway |
| Switch Access / SWITCH_SEQUENTIAL | **TRAINING_ONLY / UNMEASURED** | sequential presentation changes the construct (L7-02); fixed 1200 ms window not modality-adjusted |
| Dynamic-Type / large font | UNVERIFIED | no font-scale capture / overflow-safe layout (L7-04) |

## Decisions

1. **AT sessions are TRAINING_ONLY / not compared to visual norms** until a modality-fair design exists. Do NOT
   "just strip the direction" — that makes the flanker impossible; design an **auditory/haptic conflict task with
   its own estimand + norms** (F-05 definitive fix).
2. **Separate estimand + norms per modality** — never pool AT and visual d′.
3. **Closing test = construct-preservation**, NOT a text-does-not-contain-"left/right" check (that would pass a
   still-broken task). Must fail if the description encodes the answer AND verify the conflict survives.
4. **Manual a11y validation required**: TalkBack + VoiceOver + Switch Access, real devices (Paso 6), + user testing
   with AT users before any measurement claim for those modalities.

Owner: `compose-ui-dev` + `efficacy-guardian` + a11y specialist. Launch-blocker for any user-facing measurement
that includes AT users (F-05 = BLOCKER_BEFORE_LAUNCH). Inert today (unmounted).
