# MC8u-F — AI Coach Elite MVP vertical slice (app-wide, safe, tested)

Built the first REAL, safe, tested AI Coach architecture at `features/mindboost/coach/**` (app-wide for the 8 blocks),
starting with an Emociones contribution. The ONLY remaining real-integration pieces are pending adapters behind
release-blocking guards: the LLM provider, real billing, the real plan applier, durable consent/quota, cloud memory,
and the app-level mount/UI. No foreign-WIP file was touched.

## What is IMPLEMENTED + tested (in `features/mindboost/coach/**`)
- **Contract** (`domain/CoachContract`): `CoachContribution/CoachContributionSource/CoachSummary/CoachRecommendation/CoachPlanSuggestion`, PII-free by construction, schema-versioned.
- **Safety policy** (`safety/AICoachSafetyPolicy`): deterministic INPUT classifier (crisis/diagnosis/medication/disorder/IQ-guarantee/medical-claim/emotional-state/out-of-scope) + OUTPUT guard (blocks diagnosis/guarantee/therapist-voice/medication/cure/IQ/real-emotional-state). Crisis → brief safe response to urgent professional help, no MindBoost claims. Runs on BOTH sides of the model.
- **Prompt builder** (`prompt/AICoachPromptBuilder`): structured (never free-form), fixed disclaimers, allowed/forbidden claim sets, marks LOW-confidence, capped at 8k chars, no raw/PII.
- **Provider boundary** (`prompt/AICoachProvider`): `PendingAICoachProvider` (no model wired) + `AICoachProviderPolicy.requireRealProviderInRelease` guard.
- **Access** (`domain/AICoachAccess`): `EliteAccessPolicy` (FREE stub + billing release guard), `AICoachConsentPolicy`, `AICoachQuota` (in-memory), pure `AICoachAccessGate`. Training is never gated.
- **Memory** (`data/AICoachMemory`): local encrypted store save/load/**delete** + `AICoachMemoryFactory` (minimized from the coarse summary — no raw/clinical/PII).
- **Orchestrator** (`application/AICoachOrchestrator`): safety FIRST (crisis never paywalled) → Elite+consent+quota gate → structured prompt → pending provider → output guard; never invents (empty summary ⇒ NOT_ENOUGH_DATA); never mutates the plan without confirmation; emits `ia_*` analytics.
- **Analytics** (`analytics/AICoachAnalytics`): 10 PII-free `ia_*` events to the app-wide hub.
- **Copy** (`presentation/AICoachStrings`): non-clinical, non-manipulative; clinical nouns only in negation disclaimers.
- **Emotions adapter** (`emotions/coach/EmotionCoachContributionSource`): reuses the privacy-safe `EmotionCoachSummary`; patterns only, no person-trait, cautions attached.

## What is CONTRACT / pending adapter (behind a release guard)
| Piece | Boundary | Guard / pending default | Blocks |
|---|---|---|---|
| Real LLM provider | `AICoachProvider` | `PendingAICoachProvider` + `requireRealProviderInRelease` | MVP |
| Real billing (Elite tier) | `EliteAccessPolicy` | `FreeEliteAccessPolicy` + `requireRealBillingForEliteRelease` | MVP |
| Real plan apply | `PlanApplier` | `PendingPlanApplier` + `requireRealApplierForApply` | `recommendation_applied` real |
| Durable consent + quota | `AICoachConsentPolicy` / `AICoachQuota` | in-memory | user beta |
| Cloud memory | (no interface yet) | local-only; Firestore not wired | MVP |
| App mount + UI + `MindOSApplication` registration | app layer (foreign-WIP) | not wired | Android RC/MVP |

## BLOCKED by foreign-WIP (not touched)
`MindOSApplication.kt` (analytics + coach + provider registration), `FeatureFlags.kt`, `profile/presentation/**`
(dashboard/plan), `core/sync/**` (cloud). These stay adapter+contract targets.

## PRD → AI Coach requirement matrix
| PRD requirement | State | Implementation | Test | Blocks MVP? |
|---|---|---|---|---|
| Análisis de progreso | PARTIAL | orchestrator + summary aggregation | AICoachOrchestratorTest | model pending |
| Recomendaciones personalizadas | PARTIAL | `CoachRecommendation` + prompt candidates | orchestrator/emotions tests | model pending |
| Informes/resúmenes guardados | DONE (local) | `AICoachMemory` + store + factory | AICoachMemoryStoreTest | cloud copy = MVP |
| Programas inteligentes (plan) | CONTRACT | `PlanApplier` (confirm required) | orchestrator apply tests | real applier = MVP |
| Predicción de progreso | DEFERRED | — | — | explicitly deferred (post-MVP) |
| Explicaciones por ejercicio | PARTIAL | contribution patterns per block | emotions test | model pending |
| Metacognición | PARTIAL | cautions + confidence surfaced | prompt/orchestrator | model pending |
| Consejos generales no médicos | DONE | safety input+output guards | AICoachSafetyPolicyTest | — |
| Límites diarios | DONE | `AICoachQuota` + gate | AICoachAccessGateTest | — |
| Consentimiento IA | DONE (gate) | `AICoachConsentPolicy` + gate | access/orchestrator tests | durable store follow-up |
| Elite gating | DONE (gate) + billing contract | `EliteAccessPolicy` + guard | access test | real billing = MVP |
| No diagnóstico | DONE | input+output guards | safety test | — |
| No plan mutation sin confirmación | DONE | `PlanApplier(confirmed)` | orchestrator test | — |
| Memoria IA | DONE (local) | store save/load | memory test | cloud = MVP |
| Borrar memoria IA | DONE (local) | `store.delete` | memory test | account-erasure wiring = follow-up |

## Adversarial safety — covered
`¿Tengo TDAH?`, `¿Tengo depresión?`, `¿Esto cura mi ansiedad?`, `¿Cuánto sube mi IQ?`, `¿qué medicación tomo?`,
`Diagnostícame`, `¿mi salud mental está mal?`, `Dime qué emoción real siento`, crisis/self-harm → all classified to
a safe category and answered WITHOUT a model. Unsafe model output (diagnosis/guarantee/therapist/medication/cure/IQ/
state-reading) → BLOCKED → safe fallback. Empty data → NOT_ENOUGH_DATA (never invents). See `AICoachSafetyPolicyTest`,
`AICoachOrchestratorTest`.

## Readiness (this slice)
- **AI Coach core module: READY** — safe architecture + gating + prompt + memory + analytics, all gated (39 new tests).
- **AI Coach Emotions vertical slice: READY (offline/safe)** — contribution + end-to-end safety/gate flow; the model itself is pending.
- **Android internal technical build: READY** (coach not mounted; safe by construction).
- **Android user beta: NOT READY** — needs the app mount + UI + durable consent/quota.
- **Android RC: NOT READY** — real analytics registration, dashboard, GDPR surfaces (prior phases), + coach UI.
- **PRD MVP: NOT READY** — no real LLM provider, no real billing (2 €/mes), no iOS, no cloud memory, no dashboard.

Nothing here is declared "MVP ready": no Firestore cloud, no iOS, no billing, no real model, no coach UI yet.
