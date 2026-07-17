# ENG-01 — FORENSIC REOPENING REPORT (2026-07-16)

Response to the executive-technical-scientific committee's forensic-closure reopening.
**Phase A completed READ-ONLY and terminated there, by the reopening's own rule.** Phases B–I were **not
executed**: they require productive changes, and the gate blocks them. Nothing was self-approved; no document
status was changed.

---

## 1. PHASE_STATUS

```
PHASE_STATUS = REOPENED_NO_APTO
```
Unchanged, and now supported by evidence rather than assertion. Three independent grounds, any one sufficient:
the primary authority is missing (§3); the slice is not separable, so no evidence is attributable to it (§5);
and the design review the protocol *does* define returned **two independent NO APTO** (§18).

## 2. GOVERNANCE_STATUS

```
GOVERNANCE_STATUS  = PROVISIONAL_UNAUTHORIZED_BY_GATE
DESIGN_REVIEW_GATE = PENDING          (unchanged — this session did not move it)
```
`SLICE_ENG00_ENG_CORE_CLOSEOUT.md:103-104` — *"Do not wire `core/runtime` into the generators until the
blueprint is APTO — that wiring is the ENG-01 slice"* — was violated: `E2FiltroGenerator.kt:64` calls
`DeterministicRng.forSeed(seed)`. That is precisely the wiring the instruction named. **Verified, admitted, and
not remediable by me**: the remedy (ratify ex-post vs revert) is a CEO decision (packet D-2).

Aggravating, and my own doing: `ReproducibleSessionIdentity.kt:21-22` cites **V3 §7 as "the operative
acceptance criterion"** while V3 is unapproved. The design justifies the code and the code justifies the design.
Both reviewers caught this independently (efficacy F9; psychometrician B-8).

## 3. AUTHORITY_RESOLUTION

```
AUTHORITY_RESOLUTION = BLOCKED_MISSING_PRIMARY_AUTHORITY
```

| Question asked | Answer | Evidence |
|---|---|---|
| Owner of `DESIGN_REVIEW_GATE` | **UNRESOLVED** | No file names one. |
| Procedure `PENDING`→`APTO` | **PARTIALLY defined** | `06_AI_DEVELOPMENT_OPERATING_SYSTEM.md` §7.1 defines the entry gate (`efficacy-guardian` + `psychometrician`, parallel, with required outputs). V1's header is the precedent that these two set a blueprint's status. **What a verdict *authorizes* is undefined.** |
| Exact source of "CEO minifase ENG-01" | **ABSENT** | Cited at `ENG_CORE_AND_FOCUS_BLUEPRINT_V2.md:5`; contained nowhere. |
| The rule "per §9, a NO APTO blocks implementation" | **ABSENT** | `ENG_CORE_AND_FOCUS_BLUEPRINT.md:11` cites it; `:4` sources it to *CEO pivot directive 2026-07-15 §9*. **The blueprint has no §9** — V1's headers stop at §6; V2 has none. So §9 belongs to the directive, and the directive is cited by exactly two files and contained by none. |
| Version and precedence | `00_CONTEXT_INDEX.md` §3 ranks: law/safety → CEO truth → charter → PRD → decision log → slice contract → code. **The directive sits at rank 2 and is absent.** |
| Freeze criterion | **Claimed, never performed.** `V3:5` says *"frozen at the hash below"*; **zero** 40–64-char hex matches across all 167 lines. |
| Non-self-referential hash mechanism | **Answered: the hash must live outside the document.** A hash printed inside a file changes that file's hash — a self-referential freeze cannot exist. Precedent already in this repo: `MINDBOOST_AUDIT_LEDGER.md:70` uses a self-excluding hash validated by `scripts/validate-audit-ledger.mjs`. Recommendation: external freeze (packet §5a). |

**The structural finding (`MB-ENG-0004`).** `.gitignore:80-93` forbids committing ChatGPT/Claude conversation
exports ("NEVER commit"). The primary authority lives in a conversation. **The governance model cites an
authority its own ignore rules prevent it from holding.** This is not a missing file to paste in; it is a
defect in the model.

**Deliverable emitted:** [`docs/engines/DESIGN_REVIEW_PACKET_ENG01_2026-07-16.md`](DESIGN_REVIEW_PACKET_ENG01_2026-07-16.md)
— exact missing text, 4 decisions (D-1 authority, D-2 bypass remedy, D-3 the two live P0s, D-4 duplicate packs),
with approve/reject risks.

## 4. FINAL_SNAPSHOT_FINGERPRINTS

```
HEAD                      = 71e17038733e23a603c8ecb85ee5ff9f965a62db   (detached)
A1_MANIFEST_SHA256        = 54d2dc72fc9e62131a82db78ebadb3df659afdda29ef89218cf7eea9e4c33909
ENG01_PATCH_SHA256        = 1a79fb837d5ae2c5ab15e691fbdc9be9212222b647a20438752e8c384188366c
BLUEPRINT_V3_SHA256       = a30856eb15e240556abb48c8d75899f63c9c196ebf51caaa8e24af8017bfb761
BLUEPRINT_V2_SHA256       = 3a03b168541d7e5780f8d7fd43f5fa14c423d9124744c8d96c5959387a937dba
BLUEPRINT_V1_SHA256       = ddcda94c71068cf7039e0d2b560f6037ba5f6c4026be38be430081a3a56e66a6
SLICE_ENG00_SHA256        = 5c3beccea0596634fb33c083824c9c56a281960e87675a3c3c4d0ace15178ab2
LEDGER_CANONICAL_SHA256   = 3ec51c67b9562439…  (self-excluding; validator: LEDGER OK — 71 findings)
staged = none · renames = 0 · mode changes = none · all files 100644
```

**A1 supersedes the prior manifest, which was defective on four counts:**

| Defect | Evidence |
|---|---|
| Its stored hash did not match its own contents | `manifest.sha256` = `e18fb3c8…` (written 05:07); `manifest.txt` recomputes to `72fb77f8…` (written 05:22). **The reviewers were given `e18fb3c8…`; the tree they were said to have reviewed was `72fb77f8…`.** The committee's finding #5 is confirmed *from artifacts on disk*, not merely accepted. |
| It omitted docs and the ledger | 33 code/test files only. A1 = 38 entries incl. 4 blueprints + ledger. |
| It omitted deletions | See below. |
| It under-covered ENG-01 itself | `FocusSessionStateHolder.kt:304` reads `SessionTemplate.TEMPLATE_VERSION`; `SessionTemplate.kt` is **not** in the manifest. |

**Deletions — an unprovable class.** `FocusFormFingerprint.kt` and `E2LegacySeedCallerTest.kt` were **never
tracked** (`git cat-file -e HEAD:<path>` → does not exist). An untracked create+delete leaves **zero** trace:
no blob, no status entry, no content. **I cannot prove from this repository that they were ever deleted, or
ever existed.** The only record is narrative. A1 marks them `DEL_UNTRACKED | NO_BLOB_IN_GIT_NO_CONTENT_ON_DISK_UNPROVABLE`.

## 5. CLEAN_PATCH_VS_INTEGRATION_MATRIX

```
CHANGED_SCOPE_TESTS      = NOT_RUN (gate-blocked; no productive change was made to test)
CLEAN_PATCH_SUITE        = NOT_SEPARABLE   ← not "RED". Structurally impossible.
INTEGRATION_FULL_SUITE   = BASELINE_RED_UNCHANGED (prior run; 17 foreign failures — §15)
BASELINE_FAILURE_SET     = EXACT_MATCH (17/17 by identity — §15)
```

**This is the reopening's most consequential result, and it invalidates the evidence base rather than the code.**

| Matrix | Result | Evidence |
|---|---|---|
| **CLEAN_HEAD_PLUS_ENG01_PATCH** | **compile exit=1** | Clean worktree at HEAD + `git apply` of the 14 tracked ENG-01 files (apply exit=0) + the 19 new files. `:shared:compileKotlinDesktop` → **exit=1, 83 s**. Every unresolved symbol belongs to **SYSTEM-1 6R3**: `SubjectRevocationAuthority`, `GuardedRead`/`GuardedWrite`, `SessionRecordUpsertOutcome`, `LegacyNamespacePurge`, `MindBoostLegacyPurgeSpecs`. |
| **+ dependency closure (round 2)** | **still exit=1** | Adding the 5 files that define the missing symbols (`DeterministicRng`, `SubjectRevocationAuthority`, `AdaptiveModels`, `LegacyNamespacePurge`, `SessionRecordStore`) cascades into `creativity/di/CreativityGraph`, `decisiones/presentation/DecisionsSessionRecorder`, `emotions/di/EmotionGraph` (`revocationStore`/`subject` params), `AdaptiveEngine` (`Hold` signature), `RememberFocusStateHolders` (type drift). The true closure is **most of the uncommitted tree**. |
| **INTEGRATION_WORKTREE_WITH_FOREIGN_WIP** | the only tree that compiles | 557 changed/untracked entries; ENG-01 = 38; **foreign = 519**. |

**Per-file ownership (marker counts over `git diff HEAD -- <path>`):**

| File | changed lines | ENG-01 markers | 6R3 markers |
|---|---|---|---|
| `SecureSessionRecordStore.kt` | 245 | **1** | **30** |
| `SessionRecorder.kt` | 119 | 3 | 11 |
| `FocusGraph.kt` | 92 | 8 | 0 |
| `FocusExerciseGenerators.kt` | 61 | 5 | 0 |
| `E2FiltroGenerator.kt` | 27 | 4 | 0 |

*Method limit, stated:* marker-grep has false negatives — `FocusFixedBlockRunner.kt` (20 lines, 0/0) is a real
ENG-01 change (generator+seed params removed) that names no marker symbol. The counts bound the contamination;
they do not partition it.

**Consequence, stated plainly:** the 33-file manifest hashes **whole files**, so it silently fingerprinted
another slice's uncommitted work. `SecureSessionRecordStore.kt` is 30:1 foreign. Therefore **the 17-failure
baseline, the "in-scope GREEN" claim, and all three prior reviewer verdicts are statements about the
integration tree — never about ENG-01.** The committee asked me to separate own evidence from a contaminated
baseline. The answer is that **they were never separable**, because nothing was ever committed and several
slices mutate the same files in one working tree.

## 6. ACCEPTANCE_CRITERIA_TRACEABILITY

| Reopening requirement | Status | Evidence |
|---|---|---|
| A1 full fingerprint incl. deletions/renames/modes/untracked/ledger | **PASS** | §4. Deletions: `DEL_UNTRACKED`, unprovable — declared, not hidden. |
| A2 clean-HEAD reproduction, two matrices | **PASS (negative result)** | §5. |
| A3 authority resolution | **BLOCKED_MISSING_PRIMARY_AUTHORITY** | §3 + packet. |
| A3 design review if the protocol permits | **PASS** | §18 — executed on frozen bytes; **2× NO APTO**; doc status untouched. |
| B E2 route matrix | **PASS (read-only)** | §7. |
| C lifecycle fixes | **NOT_EXECUTED — gate-blocked** | Audit only (§8). Verifier proved the races **pre-date ENG-01**. |
| D restore branch tests | **NOT_EXECUTED — gate-blocked** | Audit only (§9). |
| E semantic plan coverage | **PASS (read-only)** | §10. |
| F version authority | **PASS (read-only)** — the fifth found | §11. |
| G SHA/canonicalization audit | **PARTIAL** | §12 — 5 of 9 verifiers died on session limit. |
| H platform matrix | **PASS (read-only)** | §14. |
| I evidence bundle + 3 reviewers on the frozen snapshot | **NOT_EXECUTED** | Requires a finished, gate-approved change. §21. |

## 7. E2_PRODUCTION_ROUTE_MATRIX

**Three production form-building routes, not one.** The decisive question — *do baseline/calibration/probe
influence profile, score, calibration, difficulty or any metric?* — is answered **YES**, with code.

| | **R1 — TRAINING** | **R2 — BASELINE/CALIBRATION** | **R3 — EVALUATION PROBE** |
|---|---|---|---|
| Entry point | `FocusRootScreen.kt:117-123` → `FocusSessionStateHolder.kt:297-309` → `focusEngine.createSession` | `FocusRootScreen.kt:69-71` → `FocusBaselineStateHolder.kt:92-110` → `FocusGraph.fixedBlockForm:131-140` | `FocusEvaluationStateHolder.kt:91-97` → `fixedBlockForm` |
| Measurement role | training (`TRAINING_ONLY`, `:306`) | **calibration** (`Mode.CALIBRATION`, `BaselineProtocol.kt:95-102`) | evaluation probe |
| Generator path | **VERSIONED** (`FocusGraph.kt:107`) | versioned **draw**, unversioned **provenance** (`FocusGraph.kt:137`) | same as R2 |
| Who mints the seed | `DefaultFocusEngine.kt:45 seedSource.mint()` → `PlatformSeedSource` (`SessionSeed.kt:74-76`). **UI cannot supply one.** | **the wall clock, in the UI**: `FocusRoute.Baseline(graph.epochClock.nowMillis())` (`FocusRootScreen.kt:70`) → `seed + index` (`:192`) → `SessionSeed(plan.seed, V1)` (`:103`) | reserved probe bank (`administration.formSeed`) — stable per administration |
| Descriptor persisted | **yes** (`SessionRecord.kt:53`, schemaVersion 2) | **no** — `fixedBlockForm` returns a bare `List<Stimulus>` | **no** |
| definitionId / planDigest | **yes / yes** | **no / no** | **no / no** |
| Persisted before trial 1 | **yes, fail-closed** — `:223-226` awaits `openSession()` before WARMUP; aborts on non-`Stored` (`:279-289`). Proven by `E2SessionReopenTest.kt:251-277` (injected `put()` failure → Aborted, no trial). | **N/A — no recorder on this route at all** | **N/A** |
| Restore path | **NOT WIRED** (zero production callers) | none — seed reaches no store | none |
| **Measurement-bearing?** | Brain profile: **no** (`FocusProfileSource.kt:55-61` fail-closed → ATTENTION CALIBRATING). Focus-local profile: **yes** (`:357` append, `:370` save). | **YES — this is the one that moves difficulty.** `BaselineAssembler.kt:42-58` → `thresholds` + `startingDifficulty[E2]` → `profileRepository.save` (`:157`) → read at `FocusSessionStateHolder.kt:191,198` as the resolved difficulty of **every later E2 session**, and `thresholds` → `anticipationFloorMs`/`lapseRtMs` in trial classification (`:256,500,507`). | feeds the evaluation service / RCI |
| Status | **VERIFIED** | **VERIFIED — NOT REPRODUCIBLE** | **VERIFIED — NOT REPRODUCIBLE** |

> **The route whose output actually moves difficulty is the route with no form identity.** R1 — the one with
> the minted, persisted, digested identity — is `measurementEligible = false` by policy. This is the sharpest
> statement of the committee's finding #6, and it is **already logged as `MB-ENG-0003 / MITIGATED`**, whose
> limitation field reads verbatim: *"baseline/probe now draw VERSIONED, but their seeds are still
> wall-clock/bank values never persisted."* **MITIGATED ≠ CLOSED.** It stays open.

`E2_FORM_REPRODUCTION_VERIFIED` is **NOT** declared: two measurement-bearing routes have no persisted identity.

## 8. LIFECYCLE_STATE_MACHINE_AND_RACE_RESULTS

**NOT_EXECUTED (gate-blocked).** Audit only — and it **corrects the committee's attribution**.

The reopening states start re-entrancy and abort/finalize TOCTOU "siguen abiertos". Both halves verified:

- **They are real and open.** `FocusSessionStateHolder.kt:175` `abortAndAwait` has no re-entry guard; `:183-184`
  reads `recorderSessionId`/`recorderFinalized` unguarded; both callers reach `recorder.onAbandon` at `:185`.
- **They are NOT ENG-01's.** The verifier proved it with `git show HEAD`: the entire alleged defect exists
  verbatim **before the slice** — `recorderMutex` (HEAD:110), `recorderFinalized` (HEAD:114), the unguarded read
  (HEAD:157), `recorderMutex.withLock { recorder.onAbandon }` (HEAD:159), the finalize sequence (HEAD:302-305).
  The ENG-01 diff touches `recorderMutex` at exactly two lines, both inside the new `openSession` (`:279-289`),
  and never touches the finalize/abort race. `:131` labels the structure **MC7** — a prior slice. ENG-01 only
  *removed* `startJob?.join()` from abort because STARTED is now awaited inline: **strengthening** the adjacent
  path, not weakening it.

**One CONFIRMED finding that IS ENG-01-adjacent (`C-03`, P2):** `FocusSessionStateHolder.kt:283` reads
`if (plan != null && started !is SessionRecordUpsertOutcome.Stored) return false`, but `:287`
`recorderSessionId = sessionId` executes **unconditionally**. On the legacy route (`plan == null`, i.e. **12 of
13 exercises**) a failed `onStart` still publishes the id — so the KDoc invariant *"a non-empty
`recorderSessionId` means the row provably exists"* is false for every exercise except E2.

8-state machine (`NOT_STARTED…FAILED`): **not representable today** — but the verifier correctly noted the
8-state vocabulary comes from the reopening prompt, not from any repository authority, so "2 of 8 representable"
is a fabricated denominator. Recorded as a *design target*, not a measured deficit.

## 9. RESTORE_BRANCH_MATRIX

**NOT_EXECUTED (gate-blocked).** Audit findings, adversarially verified:

| Finding | Verdict | Evidence |
|---|---|---|
| **`restoreSession` has ZERO production callers** | **fact CONFIRMED** | Only `FocusEngine.kt:150` (decl), `DefaultFocusEngine.kt:76` (impl), and tests. Production calls `planFor` → `createSession` unconditionally (`FocusSessionStateHolder.kt:217,297-309`); there is no reopen branch. **⇒ it is an `ENGINE_REBUILD_CONFORMANCE_API`, not a process-death control.** `PROCESS_DEATH_RESTORE_PROVEN = false` stands. |
| The "honest label is missing" assertion | **REFUTED** | The label already exists at the authority: `ReproducibleSessionIdentity.kt:19-25` declares FORM_REPRODUCTION scope explicitly. |
| **8 of 10 public restore outcomes UNTESTED_AT_ENGINE_BOUNDARY** (`D-03`, P2) | **CONFIRMED** | Matches the committee's "seven of nine" within enumeration differences. |
| **`generatedPlanDigest` is never recomputed on decode and never checked in production** (`D-02`, P2) | **CONFIRMED** | It is a **write-only field at runtime**. It is compared only inside `rebuildAndCompare`, which no production path calls. |
| Recompute-on-decode semantics | **CONFIRMED** | (a) tamper digest only → rejected; (b) tamper descriptor, keep digest → rejected; (c) tamper descriptor **and** recompute its definitionId → **only the PLAN digest catches it — and it never runs in production**. |
| **No rejection path leaks seed/digest/difficulty** (`D-05`) | **CONFIRMED CLEAN** | `rejectionFor` messages read directly. |
| **No rejection path presents a trial or leaves a session eligible** (`D-06`) | **CONFIRMED CLEAN** | |

## 10. SEMANTIC_PLAN_COVERAGE_MATRIX

The audited claim — *"SerialDescriptor makes the plan digest total by construction"* — is **true only over
serialized properties**, exactly as the committee framed it.

| Finding | Verdict | Note |
|---|---|---|
| **The shape pin covers 3 of 15 canonicalized types**; 12 of 13 payload variants have no descriptor pin (`E-01`) | facts CONFIRMED, framing refuted | `CanonicalShapeGoldenTest.kt:60-73,81-97` pins `FormDescriptorV1` + `Stimulus` top-level; Flanker is read dynamically. The per-field mutation matrix (`:169-198`) exists **only** for `FormDescriptorV1`. |
| Three renderers draw target position from unpinned `kotlin.random.Random` (`E-02`) | **REFUTED — E2 cannot reach them** | `E2FiltroGenerator.kt:72` emits `Flanker` → `choiceFieldFor` (`:538-541`). Search/Stream/Tracking fields are fed only by other payloads. Real for other exercises; **out of ENG-01's scope**. |
| **E4's stop-signal SSD is a runtime staircase, never a plan value** (`E-03`) | facts confirmed | The plan digest can say *which* trials stop, never *the delay* — the parameter the paradigm exists to measure. Scope limit, correctly declared. |
| Presentation-rate constants (RSVP frame rate, dual-task rate) hardcoded in composables (`E-05`) | facts confirmed | Outside plan and descriptor. |
| `microPauseWindowMs` can discard a real tap and lives outside both digests (`E-06`) | facts confirmed | While `responseMappingVersion` claims to cover response mapping. |

## 11. VERSION_AUTHORITY_MATRIX

**The fifth was found, and it is `sessionTemplateVersion`** (`F-VER-01`, **CONFIRMED**, P1):

> **caller-supplied, owned by no authority, and omitted from the drift check — contradicting the engine's own KDoc.**

`FocusSessionStateHolder.kt:304` passes `sessionTemplateVersion = SessionTemplate.TEMPLATE_VERSION`. The value
is handed in **by the caller**, not minted by an authority — and `SessionTemplate.kt` is **not in the ENG-01
manifest**, so the fifth version's owner was never even fingerprinted. This is the same defect class the slice
existed to remove: an authority that lives wherever a caller happens to read it.

| Finding | Verdict |
|---|---|
| `F-VER-01` — the fifth: `sessionTemplateVersion` caller-supplied, unowned, not drift-checked | **CONFIRMED (P1)** |
| `F-VER-02` — `digestAlgorithmId` is recorded and validated-as-known but **never dispatched**: `CanonicalEncoder.digest` hardcodes SHA-256 | **CONFIRMED (P2)** |
| `F-VER-03` — no compatibility registry on any version; restore accepts only today's implementation, and `Rejected.UnsupportedHistoricalImplementation` names a registry that was never built | facts confirmed, severity refuted |
| `F-VER-04` — 4 of 5 are single-sourced **and self-comparing**, three of them pointing at no implementation | facts confirmed |

Aggravating context: `SessionTemplate.TEMPLATE_VERSION = 1` versions a mapping **its own KDoc admits is wrong
for E2** (`:37-39`). Every E2 identity persisted today is stamped at a version known to be wrong; fixing B1 will
bump it and turn all of them into `VersionDrift` on restore. Correct fail-closed behaviour — a declared cost.

## 12. SHA_AND_CANONICALIZATION_EVIDENCE

**PARTIAL — 5 of 9 verifiers died on the session limit.** Findings raised but **UNVERIFIED**, reported as such:

| Finding | State |
|---|---|
| `G-01` (P1) — the whole `generatedPlanDigest` encoder path (enum, null, boolean, defaults, sealed subtypes) is pinned by **zero literals**; half of V1 could be edited with the suite green | **UNVERIFIED** |
| `G-05` (P2) — `Sha256.pad()`'s only conditional branch (`:60`) is reached by no pinned vector | **UNVERIFIED** |
| `G-06` (P2) — "V1 is frozen" is enforced by a **re-blessable const in the test's own companion**, not by a barrier — contrary to this repo's precedent (`evidenceBoundaryScanner`) | **UNVERIFIED** |
| `G-03` (P2) — `CanonicalEncoder.kt:104` KDoc states the **opposite** of what the encoder does | **UNVERIFIED** |
| `G-04` (P2) — no `@SerialName` on any canonicalized type: fully-qualified class names are inside every digest, so a package move invalidates all persisted rows | **UNVERIFIED** |
| `G-02` — "the BYTES of V1 are frozen" is false; only a digest is pinned, no `byteArrayOf` anywhere | facts confirmed, headline refuted |
| `G-08` (P3) — no Unicode test data; the UTF-8 length-prefixed path is never exercised beyond ASCII | **UNVERIFIED** |
| `G-09` (P3) — dangling KDoc citation: `CanonicalV1GoldenTest` does not exist | **UNVERIFIED** |

The committee asked whether a maintained KMP SHA-256 already exists in the dependency set, so hand-rolled crypto
would be unnecessary. **That question is UNVERIFIED** (`verify:G-01` died before answering). It is the right
question and it is now the first item of the G re-run.

**Reconciliation the committee asked for ("7 tests vs 5 published vectors"): NOT_VERIFIED.** I did not
re-enumerate `Sha256Test`'s vectors this session.

## 13. MUTATION_LEDGER_COMPLETO

**Status: INCOMPLETE against the standard this reopening sets.** 12 entries exist (11 mutations + 1 retry).
Recorded per entry: mutation ID · hash before→after · test that went RED · failure count · suite totals ·
`restored=yes`. **Missing per entry: the exact command, the exit code, and the hash after restoration.**

| # | Mutation | before→after | Result |
|---|---|---|---|
| M1 | rng-to-stdlib | `d858ef6325fd`→`84502e0a5ca4` | RED — `E2FormGoldenTest` ×2 |
| M2 | draw-order-swap | `d858ef6325fd`→`cf225b9bf877` | RED — `E2FormGoldenTest` ×2 |
| M3 | correctResponseKey-decouple | `d858ef6325fd`→`7d082d276de8` | RED — `E2FormGoldenTest` ×2 |
| M4 | isTarget-flip | `d858ef6325fd`→`46ef702fa3e1` | RED — `E2FormGoldenTest` ×2 |
| M5 | generator-version-bump | `d858ef6325fd`→`0805641a0b4c` | RED — `ProductionFocusGraphWiringTest` ×1 |
| M6 | trialcount-constant | `c7c3e58c71b3`→`6cf516352fd6` | RED — `E2FormGoldenTest` ×1 |
| **M7** | **timing-omitted** | `c7c3e58c71b3`→`2b3ee0c28e76` | **GREEN — NOT CAUGHT** |
| M7′ | timing-omitted (after covering the gap) | same | RED — `E2FormGoldenTest` ×1 |
| M8 | canonicalizer-drop-fieldname | `18bd58459933`→`a43b73e6fe66` | RED — `CanonicalShapeGoldenTest` ×1 |
| M9 | drop-defensive-copy | `81aba01c03c3`→`dbc8b6f82f05` | RED — `ReproducibleSessionIdentityTest` ×1 |
| M10 | decode-trusts-row | `81aba01c03c3`→`e496bff68ff5` | RED — `ReproducibleSessionIdentityTest` ×3 |
| M11 | projector-drops-a-column | `6a527f35c730`→`413fd9668b12` | RED — `E2FormGoldenTest` ×5 |

**M7 is the honest one:** the engine could emit `resolvedTimingPolicy = emptyList()` with the whole suite green.
It was only caught because the mutation was run. That is the case for mutation testing, and it is also why
`G-01`'s claim (half of V1 editable with the suite green) is credible and must be verified, not dismissed.

**Suite totals drift across runs (3759/3762/3763) because mutations change test counts.** Reported, not smoothed.

## 14. PLATFORM_EXECUTION_MATRIX

```
DESKTOP_JVM_TESTS   = REPORTED_VERIFIED   (prior run; not re-run this session — gate-blocked)
ANDROID_RUNTIME_TESTS = NOT_RUN
IOS_NATIVE_TESTS    = BLOCKED_NO_MACOS
CROSS_PLATFORM_IOS_GOLDEN = BLOCKED
```
Findings `H-01`/`H-02` (no ENG-01 assertion has executed on Kotlin/Native; ENG-01 test code is never compiled
against the Android target either) were **refuted on framing** by their verifiers but their facts stand at the
level the committee already asserts. `commonTest` placement ⇒ compiled for every target, **executed only where a
test task runs** — which is Desktop/JVM. **No claim of cross-platform is made.**

## 15. BASELINE_FAILURE_IDENTITY_COMPARISON

```
BASELINE_FAILURE_SET = EXACT_MATCH (17/17 by identity, not by count)
```
All 17 recovered by identity from `baseline_failures.json`. All are `[desktop]`; **none is ENG-01's** — they are
evidence/scale/purpose tests from SYSTEM-1 6R3 / V2D:

`case1_n_equals_0_absence_stays_absence_never_a_fabricated_value` · `case2_n_equals_1_…provisional_ceiling…` ·
`case3_n_equals_8_same_utc_day_grinding_never_inflates_distinct_days` · `case4a_equal_timestamps_…` ·
`case4b_true_identity_conflict_both_dropped…` · `case5_incompatible_scoring_versions_are_never_pooled…` ·
`case6_device_change_uncaptured_in_the_scale_key…` · `an_rt_non_critical_exercise_is_never_excluded_by_clock_quality` ·
`n_equals_0_is_the_zero_cost_floor` · `n_equals_1_yields_exactly_one_bounded_group` ·
`n_equals_10_output_size_tracks_scale_variety_not_n` · `n_equals_large_n_output_size_stays_bounded…` ·
`output_size_is_proportional_to_distinct_scales_not_to_raw_n_across_three_scales` ·
`a_full_training_free_session_is_purpose_excluded…` · `a_registry_miss_on_a_non_frame_clock_excludes_the_run…` ·
`rtCriticalityRegistry_shape_is_exactly_declared_and_fallback` ·
`purpose_timebase_criticality_and_validity_exclusions_map_to_training_only`

**Never described as "green".** Per §5, this baseline belongs to the integration tree, not to ENG-01.

## 16. PRIVACY_AND_SUBJECT_ISOLATION

No productive change was made, so no new surface was created. Verified this session:

- **No rejection path leaks** seed, digest or difficulty into a message or log (`D-05`, CONFIRMED).
- **`FormDescriptorV1.toString()`** redacts (`:107`); `IdentitySurrogate.toString()` = `"(redacted)"`;
  `ReproducibleSessionIdentity.toString()` exposes only `exerciseId` + `formDefinitionId`.
- **Subject scope:** identity rows are subject-bound at store construction (`FocusGraph.kt:206-214`), namespace
  `MindBoostStorageNamespaces.Sessions`, account-bound and covered by `AccountBoundErasureCoverageTest`.
- **`resolvedDifficulty` is admitted as an ability-inference vector and NEW information**
  (`ReproducibleSessionIdentity.kt:53-63`) — the KDoc records that an earlier draft's contrary claim was refuted
  by review. `psychometrician` called this unusually honest. It stands.
- **`DEVICE_ENCRYPTED_PERSISTENCE_PROVEN = false`** — unchanged. The KDoc claims encryption only where verified
  (Android Keystore AES-256-GCM); desktop is in-memory and encrypts nothing, and says so.

## 17. AUDIT_LEDGER_STATUS

```
validator: LEDGER OK — 71 findings, hash matches (3ec51c67b956…)
rows before = 74 → after = 79 (no loss; guard asserted before write)
```
Added: `MB-ENG-0004` (missing primary authority, **BLOCKED**) · `MB-ENG-0005` (not separable, OPEN) ·
`MB-ENG-0006` (duplicate divergent context packs, OPEN) · `MB-ENG-0007` (incentive inversion, **P0** OPEN) ·
`MB-ENG-0008` (archetype route, **P0** OPEN).
Updated: `MB-ENG-0001` with the executed design review (2× NO APTO; gate state unchanged).
Re-confirmed, not reopened: `MB-ENG-0003` (baseline/probe seeds) stays **MITIGATED ≠ CLOSED**.
Event `TASK-2026-07-16-ENG01-REOPEN-2` + checkpoint written. **Tree drift after the audit = 0** (read-only).

## 18. REVIEWER_OUTPUTS_AND_LIMITATIONS

**Design review (06 §7.1) — executed on V3 frozen at `a30856eb…`. Both NO APTO, independently.**

| Reviewer | Verdict | Blockers |
|---|---|---|
| `efficacy-guardian` | **NO APTO** | F1 incentive inversion (P0) · F2 §4 unimplemented (P0) · F3 §6 unimplemented (P0) · F4 §5 undelivered (P0) · F5 §8 renderer (P0) · F6 T3 must be retyped `DISPROVED — FIXED IN CODE` (P1) · F9 authority circularity (P1) |
| `psychometrician` | **NO APTO** | B-1 **per-cell `n` unrepresentable** (new DESIGN_BLOCKER) · B-2 endogenous-n confound (new DESIGN_BLOCKER) · B-3 §6 unbacked promise · B-4 unguarded archetype route · B-5 renderer destroys the construct · B-6 feedback surface · B-7 T1/T4 unimplemented · B-8 gate bypassed |

**Convergent, and I verified the load-bearing ones myself at file:line:**
- **V3 misdescribes the tree in both directions.** Its most severe finding (T3, "live product bug") is
  **already fixed**: `AdaptiveEngine.kt:83-86` uses `confidentValueOf` with an explicit comment, `:98`
  `Hold(HoldReason.METRIC_UNAVAILABLE)`. Approving V3 would ratify a false record.
- **`MB-ENG-0007` (P0, live).** `PersonalizedSessionScorer.kt:50-51` consumes `LAPSES`, `:73-74` `RT_CV`;
  `lapseRtMs = 500` (`UserCognitiveProfile.kt:25`) is a **PVT constant** on a 900–1500 ms flanker. Masher
  ≈70/100 vs honest ≈44/100. **Direction follows from the constants; magnitude needs device data.**
- **`MB-ENG-0008` (P0, live).** `ArchetypeInference.kt:61` — `IMPULSIVE to mean(faScore, faScore, rtFast)`,
  **`faScore` double-weighted**; `:114` reads `RT_CV` → next session's difficulty. `FIXED` doesn't cover it.

**Both reviewers agree the brain profile is NOT corrupted** (`FocusProfileSource.kt:55-61` fail-closed) and that
the canonical/identity layer is genuinely sound. Damage is confined to feedback, local persistence and the
difficulty loop.

**Limitations, enumerated as the reopening requires:**

| ID | Scope | Blocking? | Evidence needed to retire | Authority |
|---|---|---|---|---|
| L-1 | `IN_SCOPE_TESTS = GREEN` unverified — neither reviewer ran Gradle | non-blocking | a gated run | primary session |
| L-2 | RT magnitudes (~250/~520 ms) are literature estimates, not device data | non-blocking (direction holds) | on-device measurement | CEO |
| L-3 | 9 of 55 workflow agents died on session limit (mostly `verify:G-*`) | **blocking for §12** | re-run the G lens | primary session |
| L-4 | iOS/native | **blocking for cross-platform** | macOS runner | CEO |

**The three final reviewers (`diff-auditor`, `privacy-security`, `test-engineer`) were NOT run** — correctly.
Phase I requires a finished, frozen, gate-approved change. There is none: the gate is PENDING and no productive
change was made. Running them now would repeat the exact defect the committee flagged as finding #5.

## 19. REMAINING_BLOCKERS

| # | Blocker | Owner | Why I cannot clear it |
|---|---|---|---|
| 1 | **Primary authority absent** (`MB-ENG-0004`) — and `.gitignore:80-93` prevents the repo from holding it | **CEO** | Self-approval is forbidden and would be the same failure in a new coat. |
| 2 | `DESIGN_REVIEW_GATE = PENDING` + **2× NO APTO** | **CEO** | The rule for what a verdict authorizes is blocker 1. |
| 3 | **Slice not separable** (`MB-ENG-0005`) | primary session, once unblocked | Requires committing 6R3 first, or declaring ENG-01 non-separable. |
| 4 | **Baseline/probe seeds unpersisted** (`MB-ENG-0003`, MITIGATED) — the measurement-bearing routes | primary session, once unblocked | Productive change; gate-blocked. |
| 5 | **Two live P0s** (`MB-ENG-0007`, `MB-ENG-0008`) — not ENG-01's | **CEO** (packet D-3) | Outside ENG-01's scope; own slice. |
| 6 | Duplicate divergent context packs (`MB-ENG-0006`) | **CEO** (packet D-4) | Which source of truth binds is not mine to choose. |
| 7 | Lifecycle races — real, **pre-date ENG-01** (MC7) | primary session, once unblocked | Productive change. |
| 8 | 8 of 10 restore outcomes untested; `generatedPlanDigest` never checked in production | primary session, once unblocked | Productive change. |
| 9 | The fifth version (`sessionTemplateVersion`) unowned | primary session, once unblocked | Productive change. |
| 10 | iOS | **CEO** (macOS runner) | No macOS. |

## 20. DENOMINATORS

```
TRAINING_READY             = 0/121
PROVEN_EXERCISE_ROUTES     = 0/121
E2 production routes       = 3  (1 reproducible / 2 measurement-bearing WITHOUT identity)
Restore outcomes tested at the engine boundary = 2/10
Contract versions with a real single owner     = 4/5   (the fifth: sessionTemplateVersion)
Canonicalized types with a descriptor pin      = 3/15
ENG-01 files vs foreign tree entries           = 38 / 557 (519 foreign)
Design review verdicts                         = 0 APTO / 2 NO APTO
Workflow agents                                = 46/55 done, 9 died (session limit)
Findings: 10 CONFIRMED · 29 REFUTED (nearly all on attribution/severity, not fact) · 9 UNVERIFIED
```

## 21. FINAL_DECISION

```
PHASE_STATUS         = REOPENED_NO_APTO
GOVERNANCE_STATUS    = PROVISIONAL_UNAUTHORIZED_BY_GATE
AUTHORITY_RESOLUTION = BLOCKED_MISSING_PRIMARY_AUTHORITY
DESIGN_REVIEW_GATE   = PENDING   (2× NO APTO recorded, not applied)
TRAINING_READY       = 0/121
PROVEN_EXERCISE_ROUTES = 0/121
```

ENG-01 is **not closed and cannot be closed by me.** Phase A ran to its own stopping rule and stopped.

**What changed:** the reopening asked me to try to refute the previous work rather than defend it. Doing that
honestly produced a worse answer than expected, and the worst part is not in the code:

1. The gate is **unresolvable from the repository**, and structurally so.
2. The slice was **never independently verifiable** — so the previous close-out's evidence never meant what it
   claimed. The committee's finding #5 (stale reviewers) is real, and *narrower* than the truth: even a fresh
   review of that manifest would have been reviewing another slice's work.
3. The design the code cites as its acceptance criterion **fails its own review, twice** — and misdescribes the
   tree in both directions.

**What is genuinely good, and both reviewers said so independently:** the canonical/identity layer
(`CanonicalEncoder`, `ReproducibleSessionIdentity`, `FocusPlanDigest`) — computed-never-accepted digests,
recompute-on-decode, descriptor-driven canonicalization, honest privacy KDoc. T3 and T5 are genuinely resolved.
The brain profile is not corrupted. That work should survive whatever the CEO decides.

**Recommended sequence** (all CEO-gated): D-1 authority → D-3 the two live P0s (highest value per line, and
independent of everything else — `MB-ENG-0007` can be fixed today by removing the score from E2's path) → D-2
bypass remedy → D-4 context packs → then a V4 blueprint that retypes T3, drops the false freeze claim, and
answers `B-1` (per-cell `n`).

## 22. EVIDENCE_BUNDLE_PATHS

**In-repo (durable):**
- `docs/engines/ENG01_FORENSIC_REOPENING_2026-07-16.md` — this report
- `docs/engines/DESIGN_REVIEW_PACKET_ENG01_2026-07-16.md` — the packet (missing text, 4 decisions, risks)
- `MINDBOOST_AUDIT_LEDGER.md` — 71 findings, canonical `3ec51c67b956…`, validator OK

**Scratchpad (session-scoped, `…/4d83c382-…/scratchpad/`):**
- `forensic/A1_MANIFEST.txt` (38 entries) · `forensic/eng01_tracked.patch` (`1a79fb83…`)
- `forensic/eng01_tracked.txt` (14) · `forensic/eng01_new.txt` (19) · `forensic/status_v2.txt`
- `forensic/a2.log` · `forensic/a2_compile.log` (exit=1) · `forensic/a2b.log` · `forensic/a2b_round{1,2}.log`
- `forensic/closure_added.txt` (5 foreign files) · `forensic/a1_fingerprint.sh` · `forensic/a2_clean_patch.sh` · `forensic/a2_closure.sh` · `forensic/ledger2.js`
- `reopen/manifest.txt` (`72fb77f8…`) · `reopen/manifest.sha256` (**`e18fb3c8…` — the stale hash that proves finding #5**) · `reopen/baseline_failures.json` (17) · `reopen/mutation_ledger.txt` (12)

**Workflow (per-agent returns):**
`…/subagents/workflows/wf_43c364da-40d/journal.jsonl` — 55 agents, one `{"type":"result"}` per completed agent.

**Known bundle gaps, declared:** per-mutation command/exit-code/restoration-hash (§13); the G-lens verifications
(§12); Android/iOS execution (§14).
