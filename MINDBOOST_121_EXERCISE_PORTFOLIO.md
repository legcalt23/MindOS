# MindBoost — Exercise Portfolio (real state, code-reconstructed)

> **⛔ SUPERSEDED (2026-07-14 V2 audit).** This file grouped exercises into ~61 non-atomic rows and is
> **no longer the authority.** The canonical portfolio is now the machine-readable
> **`MINDBOOST_EXERCISE_PORTFOLIO.csv`** (121 one-slot rows, `slotId`≠`exerciseId`) with its generated
> summary **`MINDBOOST_EXERCISE_PORTFOLIO.md`** and structural validator
> `scripts/portfolio/validate-portfolio.mjs`. Kept for history (append-only); do not cite for counts.
> See ledger `MB-PORTFOLIO-0001`/`MB-PORTFOLIO-0002`.

> **Type:** READ-ONLY forensic reconstruction. No production code changed. **Base commit:**
> `71e17038733e23a603c8ecb85ee5ff9f965a62db` (working tree dirty; cognitive package UNTRACKED —
> ledger finding `integration-baseline`). **Date:** 2026-07-14. **Author:** primary Claude session.
> **Canonical target:** 120 (CAT-003 APPROVED: ≥15/motor × 8). See `MB-CATALOG-0001` for the "121" question.

---

## 0. Denominator reconciliation (`MB-CATALOG-0001`)

Three different numbers exist in the repo. This audit does **not** silently pick one:

| Source | Number | Authority |
|---|---:|---|
| **Approved target** | **120** | `09_DECISION_LOG` **CAT-003 `APPROVED`** ("≥15 por motor: mínimo 120"), `CAT-004 APPROVED` (>120 if value-adding), `01_MINDBOOST_PRODUCT_PRD:300-301`, `04_EXERCISE_QUALITY_STANDARD:7,39`. Supersedes old `PRD1-CAT` (~38). |
| **CEO-stated** | **121** | Appears in **no approved decision**. `EXERCISE_QUALITY_CONTRACT.md:3` says "~121 planificados" (informal prose, not a decision). Treated as a likely off-by-one / informal count of 120. |
| **Actually designed** | **66** | `docs/mindboost_exercise_design/INDEX.md` = "69" incl. 3 summary rows → 66 real (Enfoque 8, Velocidad 8, Lógica 10, Lenguaje 8, Memoria 8, Decisiones 8, Emociones 8, Creatividad 8). |
| **Code-catalog defined** | **~75** | Sum of the 8 code catalogs/enums (Focus 13, Speed 8, Logic 5, Language 6, Memory 16, Decisiones ~4, Emotions 15, Creativity 8). Blocks diverge from design (Focus/Memory/Emotions have MORE ids than design; Logic/Decisiones fewer). |

**Verdict:** the approved number is **120**, not 121. "121" is unsourced. There is no `CAT` decision for 121, and no artifact enumerates 121 ids. **Do not adopt 121 as a target without a CEO decision.** The remainder of this portfolio uses **120** as the denominator.

`05_TECHNICAL_CURRENT_STATE:56`: the catalog was flagged `TARGET` "hasta auditar catálogo" — **this is the first catalog audit.**

---

## 1. Method, lifecycle legend, and uniform caps

**Method:** code is truth. Enumerated each block's catalog/enum + `implemented`/registry set + hub reachability, cross-checked against design docs and the completed Lógica inventory agent. Prior summaries treated as hypotheses.

**Lifecycle states (CEO rubric):** `NOT_FOUND` < `IDEA` < `DESIGNED` < `IMPLEMENTED` < `TECHNICALLY_VERIFIED` < `TRAINING_READY` < `PROVISIONAL_MEASUREMENT` < `CALIBRATION_DATA_COLLECTING` < `OFFICIAL_MEASUREMENT`; plus `DEPRECATED`/`RETIRED`.

**Uniform caps (stated once; apply to EVERY row — this is why no exercise reaches TRAINING_READY):**
- **iOS = `UNKNOWN`/`BLOCKED`** on this Windows host (no macOS/CI). The Quality Standard requires Android+iOS parity → **iOS alone caps every exercise below `TRAINING_READY`.**
- **Tutorial / práctica / cierre, accessibility, reduced-motion, permission-fallback, on-device reproducibility, per-exercise dossier = mostly `UNKNOWN`/absent** — not verified present for any exercise this session.
- **Program-wide open P0s** (`decisiones-resurrection`, `journal-resurrection-retention`, `read-gating`, `quarantine` — ledger §2) are "applicable P0/P1" that further block `TRAINING_READY`.
- **Measurement:** every block's profile contribution is **gated OFF / CALIBRATING** by design (Logic/Language/Memory profile sources return null or CALIBRATING; Speed provisional; §15 physical gate open). ⇒ **No exercise is `OFFICIAL_MEASUREMENT`.** Best measurement state anywhere = `PROVISIONAL_MEASUREMENT` (Speed E8).

**Ceiling reached this session = `TECHNICALLY_VERIFIED`.** Columns not shown per-row (iOS, tutorial, a11y, reduced-motion, permissions, dossier) are uniformly `UNKNOWN`/BLOCKED as above.

**Epistemic tags:** `VER` = verified from code this session or by a completed agent; `INF` = inferred from code catalog + memory/prior audit, not re-run this session.

---

## 2. Per-block portfolio

### Enfoque (Focus) — code 13 / wired 13 / design 8 / target 15
`ExerciseCatalog.kt`: 13 definitions; `implemented` = all 13; `pendingUiIntegration` = **empty** (all wired: generator + renderer + metrics + selectable). `ExerciseCatalogConsistencyTest` pins registry⟺implemented. Strongest block.

| id | name | lifecycle | motor | route | scoring | difficulty | persistence | tests | tag |
|---|---|---|---|---|---|---|---|---|---|
| E1_VIGIA | Vigía (PVT) | TECHNICALLY_VERIFIED | ✓ | hub→Focus ✓ | RT_CV/lapses | ✓ | encrypted | ✓ | VER |
| E2_FILTRO | Filtro (flanker) | TECHNICALLY_VERIFIED | ✓ | ✓ | congruency | ✓ | ✓ | ✓ | VER |
| E3_GATILLO | Gatillo (go/no-go) | TECHNICALLY_VERIFIED | ✓ | ✓ | false-alarms | ✓ | ✓ | ✓ | VER |
| E4_FRENO | Freno (stop-signal) | TECHNICALLY_VERIFIED | ✓ | ✓ | SSRT | ✓ | ✓ | ✓ | VER |
| E5_CONFLICTO | Conflicto (Stroop) | TECHNICALLY_VERIFIED | ✓ | ✓ | congruency | ✓ | ✓ | ✓ | VER |
| E6_CAMBIO | Cambio (task-switch) | TECHNICALLY_VERIFIED | ✓ | ✓ | switch-cost | ✓ | ✓ | ✓ | VER |
| E7_RASTREO | Rastreo (visual search) | TECHNICALLY_VERIFIED | ✓ | ✓ | set-size-slope | ✓ | ✓ | ✓ | VER |
| E8_DUO | Dúo (dual-task) | TECHNICALLY_VERIFIED | ✓ | ✓ | dual-task-cost | ✓ | ✓ | ✓ | VER |
| E9_ENJAMBRE | Enjambre (MOT) | TECHNICALLY_VERIFIED | ✓ | ✓ | accuracy | ✓ | ✓ | ✓ | VER |
| E10_FLUJO | Flujo (RSVP) | TECHNICALLY_VERIFIED | ✓ | ✓ | accuracy | ✓ | ✓ | ✓ | VER |
| E11_ANCLA | Ancla (digital distraction) | TECHNICALLY_VERIFIED | ✓ | ✓ | distraction-idx | ✓ | ✓ | ✓ | VER |
| E12_REENGANCHE | Re-enganche (post-error) | TECHNICALLY_VERIFIED | ✓ | ✓ | post-error-acc | ✓ | ✓ | ✓ | VER |
| E13_PRESION | Presión (deadline) | TECHNICALLY_VERIFIED | ✓ | ✓ | pressure-delta | ✓ | ✓ | ✓ | VER |

Blockers (all): iOS UNKNOWN; no verified tutorial/a11y/reduced-motion; profile contribution gated (session-eligibility); PRD-fields/Firestore/analytics for L3 absent. **13 TECHNICALLY_VERIFIED, 0 TRAINING_READY.** Design named 8; code has 13 (exceeds); 2 short of 15.

### Velocidad (Speed) — code 8 / wired 8 / design 8 / target 15
`SpeedExerciseId`: E1–E8. `SpeedRootScreen` renders 8 cards; generators exist (E1 special/null-gen, E2 NumberPulseCompare, E4 RuleSwitch, E5 VisualScanRace, …). Heavy SYSTEM-1 work on E8.

| id | name | lifecycle | scoring | route | tag |
|---|---|---|---|---|---|
| E1_DESTELLO | Discriminación cromática | TECHNICALLY_VERIFIED | fixed-difficulty | ✓ | VER |
| E2_COMPARA | Comparación numérica | TECHNICALLY_VERIFIED | ✓ | ✓ | VER |
| E3_CIFRADO | Sustitución símbolo-dígito | TECHNICALLY_VERIFIED | adaptive | ✓ | VER |
| E4_CAMBIO | Conmutación perceptiva | TECHNICALLY_VERIFIED | ✓ | ✓ | VER |
| E5_BUSQUEDA | Búsqueda visual veloz | TECHNICALLY_VERIFIED | ✓ | ✓ | VER |
| E6_MULTIVIA | Multivía | TECHNICALLY_VERIFIED | adaptive | ✓ | VER |
| E7_COMPUERTA | Compuerta lógica express | TECHNICALLY_VERIFIED | adaptive | ✓ | VER |
| E8_INHIBICION | Go/No-Go inhibición | TECHNICALLY_VERIFIED + **PROVISIONAL_MEASUREMENT** | scored, PROVISIONAL (not official) | ✓ | VER |
| — | Cadencia anticipatoria (design #5) | design maps to E-set; **naming drift** vs INDEX | — | — | INF |

Blockers: iOS UNKNOWN; §15 physical gate (90/120 Hz, jitter) OPEN → measurement provisional; profile only Android API≥34 + authoritative timebase. E8 is the closest thing to measured, but PROVISIONAL, not official. **8 TECHNICALLY_VERIFIED (E8 also PROVISIONAL_MEASUREMENT), 0 TRAINING_READY.** 7 short of 15.

### Lógica (Logic) — code 5 types/6 items / wired 6 / design 10 / target 15
(Completed inventory agent, VER.) One unified session engine runs 6 authored items over 5 `LogicExerciseType`; reachable hub→Logica→session. `LogicProfileSource.contribution()` **fail-closes to null** → LOGICAL_REASONING stays CALIBRATING.

| id | name | lifecycle | tag |
|---|---|---|---|
| DEDUCTION_FROM_PREMISES | Cadena Deductiva (single-step) | TECHNICALLY_VERIFIED | VER |
| CONDITIONAL_TEST | Refutación condicional (Wason) | TECHNICALLY_VERIFIED | VER |
| NECESSARY_SUFFICIENT | Necesario vs Suficiente | TECHNICALLY_VERIFIED | VER |
| CONTRADICTION_DETECTION | Detector de Contradicción | TECHNICALLY_VERIFIED | VER |
| COUNTEREXAMPLE | Caza-Contraejemplo | TECHNICALLY_VERIFIED | VER |
| (isomorphic transfer probe) | Transferencia Espejo (probe, not standalone) | IMPLEMENTED | VER |
| — | Silogismo Categórico | NOT_FOUND | VER |
| — | Inferencia Bayesiana Cualitativa | NOT_FOUND (skill enum only) | VER |
| — | Ordenar la Derivación | NOT_FOUND | VER |
| — | Inferencia Suprimida | NOT_FOUND | VER |
| — | Puente de Analogía Estructural | NOT_FOUND (skill enum only) | VER |

Blockers: iOS UNKNOWN; static unvalidated 6-item bank (no generator, no BankValidator); no tutorial/a11y; profile fail-closed. **5–6 TECHNICALLY_VERIFIED, 5 of 10 design exercises NOT_FOUND, 0 TRAINING_READY.** ~9 short of 15.

### Lenguaje (Language) — code 6 (+mixed) / wired 6 / design 8 / target 15
`LanguageExerciseCatalog`: 6 standalone + 1 mixed session, all in the menu. Memory: green tests+detekt+Android, installed on emulator. VERBAL profile stays CALIBRATING (bank demo/unvalidated / provenance stub).

| id | name | lifecycle | tag |
|---|---|---|---|
| evidence_lock | Evidence Lock Pro | TECHNICALLY_VERIFIED | VER |
| semantic_surgery | Semantic Surgery | TECHNICALLY_VERIFIED | VER |
| compression_ladder | Compression Ladder | TECHNICALLY_VERIFIED | VER |
| argument_xray | Argument X-Ray | TECHNICALLY_VERIFIED | VER |
| scope_negation | Scope & Negation | TECHNICALLY_VERIFIED | VER |
| source_triage | Source Triage | TECHNICALLY_VERIFIED | VER |
| mixed_session | Sesión mixta (interleaved) | IMPLEMENTED (composite, not a distinct mechanic) | VER |
| — | Lexical Access Sprint (design #7) | NOT_FOUND | VER |
| — | Calibrated Comprehension (design #8) | NOT_FOUND | VER |

Blockers: iOS UNKNOWN; banks not validated → not profile-eligible; no verified tutorial/a11y. **6 TECHNICALLY_VERIFIED, 2 of 8 NOT_FOUND, 0 TRAINING_READY.** 9 short of 15.

### Memoria (Memory) — code 16 defined / wired 3 / design 8 / target 15
`MemoryExerciseCatalog`: 16 declarative tech-sheets (EX1–EX16; EX16 is a transversal overlay, not standalone). `implemented` = **only 3** (EX1 Span, EX3 Lista, EX4 Pares). The other 13 are **DESIGNED** (tech-sheet, no generator/render). Scorers for the 3 have discriminating tests; profile CALIBRATING by design.

| id | name | lifecycle | tag |
|---|---|---|---|
| EX1_SPAN_COMPLEJO | Span Complejo | TECHNICALLY_VERIFIED | VER |
| EX3_LISTA_ESTRATEGIA | Lista con Estrategia (deferred recall) | TECHNICALLY_VERIFIED | VER |
| EX4_PARES_ASOCIADOS | Pares Asociados | TECHNICALLY_VERIFIED | VER |
| EX2,EX5,EX6,EX7,EX8,EX9,EX10,EX11,EX12,EX13,EX14,EX15 | (Actualización, Grid, Detección Cambio, Secuencias, Nombres-Rostros, Instrucciones, Prospectiva, Interferencia, Discriminación, Reconstrucción, Dual-Task, Narrativa) | **DESIGNED** (catalog tech-sheet only, no generator/render/route) | VER |
| EX16_CALIBRACION_CONFIANZA | Calibración de Confianza | DESIGNED (transversal overlay, not standalone) | VER |

Blockers: 13 have no generator/renderer; iOS UNKNOWN; profile CALIBRATING. **3 TECHNICALLY_VERIFIED, 12 DESIGNED-only, 1 overlay, 0 TRAINING_READY.** 12 short of 15 wired.

### Decisiones (Decisions) — live-reachable 2 / built-hidden 1 / design 8 / target 15
`DecisionsFlowStage`: CheckIn, BayesArena, Reversibility, Result. **`includeReversibility=false` by default** → live flow = **Check-in → Bayes → Result**. Reversibility is fully built (ReversibilityScorer VALID/INVALIDATED/CALIBRATING + `ReversibilityScenarioBank` 6 domains + ProfileAdapter) but **HIDDEN** from the live flow.

| id | name | lifecycle | tag |
|---|---|---|---|
| CheckIn (≈ design E4) | Check-in cognitivo Go/No-Go | IMPLEMENTED (reachable, scored) | INF |
| BayesArena (≈ design E1) | Bayes Arena calibrador | IMPLEMENTED (reachable, scored) | INF |
| Reversibility (≈ design E2) | Triaje de Reversibilidad | IMPLEMENTED but **HIDDEN** (`includeReversibility=false`); DISCRIMINATION signal built | VER |
| — | E3 Forecast, E5 Escáner, E6 Sesgo, E7 Presupuesto, E8 Ambigüedad | NOT_FOUND | INF |

Blockers: iOS UNKNOWN; Reversibility hidden by default; 5 of 8 design exercises absent; profile signal exists (REVERSIBILITY_DISCRIMINATION) but exercise is hidden. **~2 reachable (IMPLEMENTED), 1 built-but-hidden, 5 NOT_FOUND, 0 TRAINING_READY.** ~12 short of 15. **Naming drift:** design E1–E8 vs code B1–B6/stage names — reconcile.

### Emociones (Emotions) — code 15 defined / playable 3–6 / design 8 / target 15
`EmotionCatalogIds`: EM1–EM15. Code comment: "EM1..EM3 are the v1 playable set; the rest are [catalog-only]". Memory (Fase 2B): OLA1+OLA3 wired **6 playable** (EM1, EM2, EM3 + EM8, EM10, EM11 reusing the 3 renderers). Discrepancy: catalog comment says 3, memory says 6 → **INF, needs on-device confirm.**

| id | name | lifecycle | tag |
|---|---|---|---|
| EM1_PAUSA | Pausa | TECHNICALLY_VERIFIED | INF |
| EM2_RECUPERACION | Recuperación | TECHNICALLY_VERIFIED | INF |
| EM3_SEPARADOR | Separador | TECHNICALLY_VERIFIED | INF |
| EM8_ANTIIMPULSO | Anti-impulso | IMPLEMENTED (renderer reuse) | INF |
| EM10_SEP_PRESION | Separación bajo presión | IMPLEMENTED (renderer reuse) | INF |
| EM11_FEEDBACK_NEG | Continuidad | IMPLEMENTED (renderer reuse) | INF |
| EM4,EM5,EM6,EM7,EM9,EM12,EM13,EM14,EM15 | (Calibrador, Sesgo afectivo, Tolerancia, Reentrada, Etiquetado, Espera, Variabilidad, Confianza, Recup-cadena) | **DESIGNED / catalog-only** | VER |

Blockers: iOS UNKNOWN; induction-intensity constraints; no measurement profile; 9 catalog-only. **3–6 IMPLEMENTED/VERIFIED, 9 DESIGNED-only, 0 TRAINING_READY.** ≥9 short of 15.

### Creatividad (Creativity) — code 8 defined / `implemented` 2 / design 8 / target 15
`CreativityExerciseCatalog.implemented` = **2** (EX01 Suppress First Idea, EX02 Category Rotation). Offline-only Module IV; **AI scoring BLOCKED** (Phases 2–9 remaining). Even the 2 implemented cannot reach OFFICIAL_MEASUREMENT (AI-blocked).

| id | name | lifecycle | tag |
|---|---|---|---|
| EX01_SUPPRESS_FIRST_IDEA | Supresión del Primer Impulso | IMPLEMENTED (offline; AI-scoring blocked) | VER |
| EX02_CATEGORY_ROTATION | Rotación Forzada de Categorías | IMPLEMENTED (offline; AI-scoring blocked) | VER |
| EX03..EX08 | (Síntesis, Analogías Remotas, Elaboración, Inverosímil, Reencuadre, Improvisación) | DESIGNED / NOT_FOUND (not in `implemented`) | VER |

Blockers: iOS UNKNOWN; AI-scoring blocked (no OFFICIAL_MEASUREMENT possible offline for divergent-thinking scoring); 6 not wired. **2 IMPLEMENTED, 6 DESIGNED/NOT_FOUND, 0 TRAINING_READY.** 13 short of 15.

---

## 3. Counts (per block) — against approved target 120 (15/block)

| Bloque | Target | NOT_FOUND (design-named, no code) | DESIGNED-only | IMPLEMENTED | TECHNICALLY_VERIFIED | TRAINING_READY+ | Code-defined ids | Wired/playable |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Enfoque | 15 | 0 | 0 | 0 | **13** | 0 | 13 | 13 |
| Velocidad | 15 | 0 | 0 | 0 | **8** (E8 also PROVISIONAL_MEAS.) | 0 | 8 | 8 |
| Lógica | 15 | 5 | 0 | 1 (probe) | **5** | 0 | 5 | 6 |
| Lenguaje | 15 | 2 | 0 | 1 (mixed) | **6** | 0 | 6 | 6 |
| Memoria | 15 | 0 | 13 | 0 | **3** | 0 | 16 | 3 |
| Decisiones | 15 | 5 | 0 | 2 (+1 hidden) | 0 | 0 | ~4 | 2 live |
| Emociones | 15 | 0 | 9 | 3 | **3** (INF) | 0 | 15 | 3–6 |
| Creatividad | 15 | 0 | 6 | 2 | 0 | 0 | 8 | 2 |
| **TOTAL** | **120** | **12** | **41** | **~9** | **~38** | **0** | **~75** | **~43–46** |

### The three mandated progress metrics (denominator = approved 120)

1. **Inventory coverage** (exerciseIds identified in code OR design) ≈ **75 code-defined + 12 design-only names ≈ 80 / 120 (≈ 67%)** identified; **~40 slots to 120 are undesigned** (`NOT_FOUND`/IDEA).
2. **Implemented** (truly playable end-to-end, `IMPLEMENTED`+) ≈ **43–46 / 120 (≈ 36–38%)**.
3. **Launch-ready** (`TRAINING_READY`+, full Quality Standard) = **0 / 120 (0%)**. **Blocks with ≥15 TRAINING_READY: 0/8.**
   - Reason: iOS unverifiable (BLOCKED) caps everything; no exercise has verified tutorial/a11y/reduced-motion/permissions/reproducibility/dossier; program-wide P0 erasure findings open; all measurement gated OFF/PROVISIONAL.

> **Progress that is *not verifiable* this session:** iOS parity (all), on-device execution/QA (all), 2-week authorship attribution for pre-2026-07-03 files (git history is a detached squash to 2026-07-03), Emotions playable-count (3 vs 6), and per-exercise a11y/tutorial/dossier presence. These are `UNKNOWN`, not assumed-present.

---

## 4. Two-week delta (≈ since 2026-06-30) — where the effort actually went

**Finding (`MB-EXECUTION-0001`):** the exercise MECHANICS across every block **predate the 2-week window**. The Lógica and Enfoque inventory agents both reported the same: in the last ~2 weeks, the files that changed under `mindboost/**` were **session-recorder / recovery / revocation plumbing** (SYSTEM-1 Slice 6-R3), *not* exercise domain/scoring/content. Git history for these files is a detached squash reaching only 2026-07-03, so exact authorship/date pre-window is `UNKNOWN` from git alone; CLAUDE.md learnings place the exercise code at ~2026-06-06…06-25.

Categorization of the last ~2 weeks:
- **A. Direct exercise work:** minimal — Speed **E8 Go/No-Go** (MC9-D series) is the main exercise-level delta; a few honesty fixes (Focus lapse-counting, Logic transfer/impulsivity) landed early in the window.
- **B. Reusable infrastructure:** the bulk — SYSTEM-1 (`SessionRecord`, `SecureSessionRecorder`, `SessionRecoveryScanner`, `CognitiveSessionRun` evidence, `SubjectRevocationAuthority`/triadic, payload journal, quarantine). Benefits **all** blocks eventually but ships **0** new exercises.
- **C. Rework from audits:** substantial — P2-01 reopened twice; 6-R2/6-R3 evidence-only rewrite; the triadic P0-C regression + fix.
- **D. Incomplete:** P2-01 (REOPEN), journal/Decisiones/quarantine guards (OPEN P0s).
- **E. Blocked:** iOS (BLOCKED_IOS), full-battery freeze (BLOCKED_REPO_BASELINE), credential rotation (user).
- **F. Not part of the 121 plan:** Arena, social/DM, profile-card, wallet, account-deletion backend — real work, orthogonal to the exercise catalog.

**Net:** ~2 weeks produced **deep measurement-integrity infrastructure and audit rework, and ~1 exercise's worth of direct catalog progress (E8).** The catalog moved from "unaudited TARGET" to "audited: ~43–46 playable, 0 launch-ready."

---

## 5. Duplicates, dead routes, placeholder content

- **Decisiones Reversibility** — fully built but **HIDDEN** (`includeReversibility=false`); dead in the live flow.
- **Memoria** — 13 of 16 catalog entries are tech-sheets with **no generator/render/route** (design present, code absent).
- **Emociones** — 9 of 15 catalog ids have no confirmed playable renderer; EM8/EM10/EM11 **reuse** the EM1/EM2/EM3 renderers (variety via config, not new mechanics).
- **Lógica** — `logic-e3-access` and `logic-e3-library-iso` intentionally share structure (isomorphic probe), not a bug.
- **Naming drift** — Decisiones design E1–E8 vs code stage/B-names; Speed design names vs E1–E8 codes; Emotions EM-A..H (design) vs EM1..15 (code). Reconcile before counting.
- **No design→120 mapping exists** — 40 of 120 approved slots are undesigned.

---

## 6. Bottleneck & critical path

**Why SYSTEM-1 6-R3 stays open and absorbs flow:** it is the *shared measurement-integrity spine* (honest session lifecycle, revocation/erasure, cognitive-run history) every exercise needs to become `OFFICIAL_MEASUREMENT`. It reopened twice (6-R2 evidence rewrite; 6-R3 P0-C regression) and consumed most of the window with **no burn-up on the catalog** — the CEO cannot see exercise progress because there was little.

**Reusable vs blocking:** 6-R3 benefits the *measurement* tier (L2+), but exercises can reach **`TECHNICALLY_VERIFIED`/`TRAINING_READY` (L1/L3-minus) without it** — the current ceiling is set by **iOS parity + per-exercise QA (tutorial/a11y/reproducibility) + Firestore/analytics**, none of which depend on 6-R3.

**Critical path to a real 120 launch:**
1. Decouple: freeze 6-R3 at "profile-gated OFF" (already true) and stop letting it block catalog work.
2. Per-block **vertical slices to TRAINING_READY**: pick the strongest (Focus 13, Speed 8, Language 6) and drive them through the Quality Standard (tutorial/práctica/cierre + a11y + reduced-motion + permission-fallback + iOS parity on macOS + dossier + tests) — L1→L3, measurement deferred.
3. **Fill design gaps**: 40 undesigned slots + the NOT_FOUND design exercises (Logic 5, Language 2, Decisiones 5) before any "120" claim.
4. Only then resume 6-R3 measurement wiring for L2/OFFICIAL_MEASUREMENT.

**Proposed WIP limit:** max **1 active infrastructure slice + 1 active exercise vertical** at a time; a "mini-phase" = one reviewable unit with its own gate; **escape criterion**: if an infra slice reopens ≥2×, freeze it at its honest partial state and switch to exercise verticals (exactly the 6-R3 situation now).

---

## 7. Evidence & limitations

- **Base commit:** `71e17038733e23a603c8ecb85ee5ff9f965a62db`. **Production code changed:** NO.
- **Verified this session (VER):** Focus catalog (13/13), Speed enum+root (8/8), Memory catalog (16 def/3 impl), Language catalog (6+mixed), Creativity `implemented` (2), Decisiones flow (`includeReversibility=false`), Emotions catalog ids (15), + completed Lógica inventory agent.
- **Inferred (INF):** Emotions playable count (3 vs 6), Decisiones scoring depth, Speed per-exercise test depth — from code catalog + memory, not re-run this session.
- **Not verifiable here:** iOS (all), on-device QA (all), 2-week authorship (detached git squash to 2026-07-03), exhaustive per-exercise a11y/tutorial/dossier.
- **Agent fan-out:** 8 parallel block-inventory agents dispatched; **only Lógica completed** (session token limit); the rest were reconstructed by the owner from direct catalog reads + breadcrumbs + memory. This is honest partial verification, not full per-exercise audit.
