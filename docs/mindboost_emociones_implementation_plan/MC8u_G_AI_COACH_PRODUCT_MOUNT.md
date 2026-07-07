# MC8u-G — AI Coach Elite Minimal Product Mount + Durable Access

MC8u-F built the *safe architecture*. MC8u-G makes the coach a **usable, testable product surface**: a Compose screen
driven by a durable state machine that runs the full flow **consent → question → orchestrator → response →
memory/analytics**, with **durable local consent + quota**, real interaction analytics, and a memory view/delete flow.
The only remaining real-integration piece is still the LLM provider (plus billing, cloud memory, and the navigation
mount, which is foreign-WIP). Nothing here is declared MVP-ready.

## 1. Reclassification of MC8u-F (honest naming)
| Layer | State | Why |
|---|---|---|
| AI Coach **safe architecture** slice | READY | contracts + safety + gating + prompt + memory (MC8u-F), gated |
| AI Coach **functional vertical slice** (flow exists) | READY (offline, safe) | MC8u-G: state holder runs consent→ask→orchestrator→response→memory/analytics, tested end-to-end on JVM |
| AI Coach **product UI** (mounted, reachable) | NOT READY | screen exists + tested via holder, but not wired into live navigation (entry points foreign-WIP) |
| AI Coach **MVP** | NOT READY | no real LLM provider, no billing, no iOS, no cloud memory, not mounted |

"Vertical slice ready" is used ONLY for the offline flow that genuinely exists and is tested; it is NOT used for the
mounted product (no navigation entry) or the MVP (no model).

## 2. Surface & foreign-WIP audit
| Surface | File(s) | Foreign-WIP? | Safe to edit? | Action taken |
|---|---|---|---|---|
| Navigation / hub | `presentation/ui/MindBoostHubScreen.kt` | Yes (parallel Decisiones WIP) | No | mount contract + guard only |
| App init | `androidApp/.../MindOSApplication.kt` | Yes (modified) | No | untouched; provider/analytics registration deferred |
| Dashboard / profile | `profile/presentation/**` | Yes (modified) | No | untouched |
| Feature flags | `core/config/FeatureFlags.kt` | Yes (modified) | No | untouched |
| Billing / wallet | `features/wallet/**` | Not this slice | N/A | `EliteAccessPolicy` stub + release guard (MC8u-F) |
| Local storage | `focus/data/local/SecureKeyValueStore.kt` | Yes (modified) | No (interface only used, never edited) | consumed via `rememberSecureKeyValueStore` |
| Account deletion | `core/di/AppGraph.kt` | No | Yes | added `Coach` namespace to the eraser |
| Namespaces | `storage/MindBoostStorageNamespaces.kt` | No | Yes | added `Coach` (account-bound) |
| Analytics hub | `core/analytics/MindBoostAnalytics.kt` | No | already wired (MC8u-E) | coach events flow through it |
| Coach module | `features/mindboost/coach/**` | No | Yes | all MC8u-G work here |
| Cloud sync | `core/sync/**`, `focus/data/sync/**` | Yes (untracked WIP) | No | untouched; cloud memory stays a documented blocker |
| Copy scanners | (coach tree is NOT scanned) | — | — | `AICoachStringsTest` enforces copy instead |

## 3. UI states covered (18/18)
Loading · Free/Elite-required · Elite-without-consent · Consent-explanation · Consent-granted · Quota-available ·
Quota-reached · Not-enough-data · Provider-unavailable · Safety-refusal · Crisis-safe-response · Normal-answer ·
Memory-empty · Memory-available · Memory-deleted · Recommendation-shown · Recommendation-applied-after-confirmation ·
Recommendation-rejected. Each is a representable configuration of `AICoachUiState` and is asserted in
`AICoachStateHolderTest`.

## 4. Durable consent (§4)
`AICoachConsentRecord(userId, granted, grantedAtMillis?, revokedAtMillis?, version, policyVersion)` in the local
encrypted `mindboost_coach` namespace. `toStatus()` is fail-closed: only a live grant at the current `policyVersion`
maps to GRANTED; a bumped policy forces re-consent. Revoke persists and NEVER deletes memory. Tests:
`AICoachConsentStoreTest` (+ holder tests for no-data-without-consent, revoke-keeps-memory, training-unaffected).

## 5. Durable quota (§5)
`AICoachQuotaRecord(dayEpoch, used)` in `mindboost_coach`; resets when the UTC day changes. **Explicit consume policy:
quota decrements ONLY on a served model answer.** Crisis, safety refusals, not-enough-data, blocked, and
**provider-unavailable NEVER consume** — this fixed a real MC8u-F defect where `quota.consume` ran *before* the
provider call (burning quota on every unavailable answer). Tests: `AICoachQuotaStoreTest`, orchestrator
`an_unavailable_provider_never_consumes_quota` / `a_crisis_neither_consumes_quota…`, holder durability test.

## 6. Memory UI + delete (§6)
`CoachMemoryUi` (Empty / Loaded / Deleted); Loaded shows the coarse summary + "solo en este dispositivo" until cloud
memory is wired. Delete requires confirmation and emits `ia_memory_deleted`; deleting memory does not touch consent.

## 12. Encryption verification — "local encrypted store"
| Store | Platform | Encrypted? | Evidence | Risk |
|---|---|---|---|---|
| Coach consent/quota/memory (`mindboost_coach`) | Android | **Yes** — AES-256-GCM, Android Keystore | `SecureKeyValueStore.android.kt` | none material (shipping target) |
| " | iOS | **Yes** — Keychain-backed durable store | `SecureKeyValueStore.ios.kt` / `IosKeychainBackend` | POSTPONED (not built on Windows) |
| " | Desktop | **No** — in-memory non-durable dev stub | `SecureKeyValueStore.kt` KDoc | dev-only, never shipped |
The "local encrypted store" claim holds on the shipping platforms (Android + iOS). Desktop is an explicit non-durable
dev stub and is never a release target.

## 13. PRD → AI Coach matrix (updated)
| PRD requirement | MC8u-F | MC8u-G | Final | Evidence | Blocks MVP? |
|---|---|---|---|---|---|
| IA Coach funcional (usable flow) | arch only | UI flow (offline) | PARTIAL | AICoachStateHolderTest | real model = MVP |
| Análisis / recomendaciones / explicaciones | contract | shown in UI | PARTIAL | holder/rec tests | model pending |
| Informes / resúmenes (memoria) | local store | UI view + delete | DONE (local) | memory tests | cloud = MVP |
| Consentimiento IA | in-memory gate | **durable** | DONE | consent tests | — |
| Límites diarios (quota) | in-memory | **durable + correct** | DONE | quota tests | — |
| Elite gating | gate + guard | gate + guard | DONE (gate) | access test | real billing = MVP |
| No diagnóstico / no medicación / no cura / no IQ | guards | guards + UI copy | DONE | safety + strings tests | — |
| No plan mutation sin confirmación | contract | UI confirm flow | DONE | holder apply/reject tests | real applier = MVP |
| Borrar memoria IA | store.delete | UI delete + confirm | DONE (local) | holder test | erased with account (AppGraph) |
| UI del coach | — | standalone screen | CONTRACT (unmounted) | AICoachScreen + mount guard | mount = beta/MVP |
| Provider LLM real | pending | pending | PENDING | `PendingAICoachProvider` + guard | MVP |
| Billing real | pending | pending | PENDING | `FreeEliteAccessPolicy` + guard | MVP |
| Cloud memory | — | — | PENDING | local-only | MVP |
| iOS | — | — | POSTPONED | no Mac | RC-multiplatform |

## 9. Entry point
`AICoachScreen` + `rememberAICoachStateHolder(userId)` are standalone under `coach/presentation`. `AICoachMountState.
MOUNTED_IN_NAVIGATION = false` and `AICoachMountPolicy.requireMountedForUserBeta(...)` block a user-beta build that
claims the coach without a visible entry. When the hub frees, mounting is: add a Coach card that builds the holder and
renders the screen. `AICoachMountPolicyTest` fails if a beta build is declared unmounted.

## Gates (this slice)
`compileKotlinDesktop` ✅ · `desktopTest` **2993 · 0 fail · 0 error** ✅ · `compileAndroidMain` ✅ · `detekt` → only the
foreign untracked `IndexedSecureKeyValueStore.kt:61` ✅ · mindboost copy scanners ×3 ✅ · `scan:secrets` ✅ · **iOS not
run (no Mac)** · **Android runtime not exercised (no instrumented test)** · **real provider not exercised (still
pending)**.

## GO / NO-GO
- AI Coach architecture: **GO**.
- AI Coach functional slice (offline): **GO**.
- AI Coach UI internal (build/preview/test): **GO** (safe, unmounted).
- Android internal user beta: **NO-GO** (not mounted; consent/quota durable but no visible entry, no real provider).
- Android RC: **NO-GO**.
- PRD MVP: **NO-GO** (no LLM, no billing, no iOS, no cloud memory, not mounted).
