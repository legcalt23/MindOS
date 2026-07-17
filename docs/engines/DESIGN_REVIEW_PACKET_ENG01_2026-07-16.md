# DESIGN_REVIEW_PACKET — ENG-01 (Focus engine + E2 form reproducibility)

**Emitted:** 2026-07-16 · **By:** primary session (Claude), under the ENG-01 forensic reopening
**Status:** `BLOCKED_MISSING_PRIMARY_AUTHORITY`
**Purpose:** this packet exists because the gate that governs ENG-01 cannot be resolved from the repository.
It asks the CEO for the exact missing text and for four decisions. It does **not** approve anything, and no
document status was changed to produce it.

---

## 0. Frozen subjects of this packet

External, non-self-referential hashes (see §5 for why they are external):

| Artifact | SHA-256 |
|---|---|
| `docs/engines/ENG_CORE_AND_FOCUS_BLUEPRINT_V3.md` | `a30856eb15e240556abb48c8d75899f63c9c196ebf51caaa8e24af8017bfb761` |
| `docs/engines/ENG_CORE_AND_FOCUS_BLUEPRINT_V2.md` | `3a03b168541d7e5780f8d7fd43f5fa14c423d9124744c8d96c5959387a937dba` |
| `docs/engines/ENG_CORE_AND_FOCUS_BLUEPRINT.md` (V1) | `ddcda94c71068cf7039e0d2b560f6037ba5f6c4026be38be430081a3a56e66a6` |
| `docs/engines/SLICE_ENG00_ENG_CORE_CLOSEOUT.md` | `5c3beccea0596634fb33c083824c9c56a281960e87675a3c3c4d0ace15178ab2` |
| `MINDBOOST_AUDIT_LEDGER.md` | `44a56a1dbddda1e2a05581af8f500838bab249aa915786bf8de27841535a5ee7` |
| ENG-01 A1 manifest (38 entries) | `54d2dc72fc9e62131a82db78ebadb3df659afdda29ef89218cf7eea9e4c33909` |
| ENG-01 tracked patch | `1a79fb837d5ae2c5ab15e691fbdc9be9212222b647a20438752e8c384188366c` |

HEAD: `71e17038733e23a603c8ecb85ee5ff9f965a62db` (detached).

---

## 1. What is missing — the exact text this packet needs

`docs/engines/ENG_CORE_AND_FOCUS_BLUEPRINT.md:4` reads, verbatim:

> **Authority:** CEO pivot directive 2026-07-15 §1.1, §2.1, §3, §9.

and `ENG_CORE_AND_FOCUS_BLUEPRINT.md:11` reads, verbatim:

> Per §9, a `NO APTO` blocks implementation of the reviewed design.

**That §9 is the rule that gives `DESIGN_REVIEW_GATE` its blocking force, and it does not exist in this
repository.** Verified, not assumed:

| Check | Result |
|---|---|
| Does the blueprint itself have a §9? | **No.** V1's section headers stop at §6 (`## 6. Non-negotiables carried from the charter`). V2 has no §9 header. So "§9" is a section of the *directive*, not of the blueprint. |
| Does any file contain the directive? | **No.** It is *cited* by exactly two files (`ENG_CORE_AND_FOCUS_BLUEPRINT.md:4`, `docs/portfolio/V2D_PAUSED_REOPEN_KNOWN_RISKS.md:3`) and *contained* by none. |
| Does `06_AI_DEVELOPMENT_OPERATING_SYSTEM.md` define the blocking force? | **No.** Its only mention of the phrase is `:443`, listing `veredicto APTO/NO APTO` as an agent request's required output format. It never says a NO APTO blocks implementation, and never names a ratifier. |
| Does `07_PHASE_REVIEW_PROTOCOL.md` define it? | **No.** It governs ChatGPT's post-phase audit of Claude and defines `APROBADA/REOPEN/BLOCKED/CONDITIONAL` for *that* process — not the blueprint's design gate. |
| Does `09_DECISION_LOG.md` record ENG-01? | **No.** Zero hits for `ENG-01`/`ENG_CORE`/`blueprint`/`focus engine`. |
| Could the directive ever live here? | **Structurally, no.** `.gitignore:80-93` explicitly forbids committing ChatGPT/Claude conversation exports ("NEVER commit"). The authority lives in a conversation, and the repo is configured to reject exactly that artifact class. |

**This is not an oversight to patch by pasting a file.** The governance model cites a primary authority that its
own ignore rules prevent it from holding. That is the finding.

### 1.1 What we need from you, verbatim

Please supply the literal text of **CEO pivot directive 2026-07-15, §1.1, §2.1, §3 and §9**, or state that it
is withdrawn. §9 specifically must answer:

1. Does a `NO APTO` block implementation, and of what exactly — the design, the wiring, or the release?
2. **Who** may move `DESIGN_REVIEW_GATE` from `PENDING` to `APTO`? The two reviewer agents alone, or does the
   CEO ratify?
3. Does an `APTO` from `efficacy-guardian` + `psychometrician` *authorize* implementation, or only *unblock* it
   pending a further step?
4. What is the remedy when wiring has already shipped against a `PENDING` gate (which is what happened)?

---

## 2. What the repository *does* define (so you know what is not missing)

`06_AI_DEVELOPMENT_OPERATING_SYSTEM.md` §7.1 defines the entry gate as `efficacy-guardian` + `psychometrician`
in parallel, with a required output (constructo/mecanismo · qué se mide y qué no · métricas y amenazas · estado
de evidencia · criterios de aceptación). V1's own header is the precedent that these two agents' verdicts set a
blueprint's status (`BLUEPRINT_REJECTED` — `efficacy-guardian: NO APTO`; `psychometrician: REVIEW_INCOMPLETE`).

**So the procedure for *running* the review is discoverable, and this session ran it (§3). What is missing is
only the rule for what the result *authorizes*.** That is why this packet stops here rather than at the review.

---

## 3. The design review was executed — both verdicts are NO APTO

Run 2026-07-16 against V3 frozen at `a30856eb…`, per 06 §7.1. Outputs preserved in full in the reopening
report. **No document status was changed.**

| Reviewer | Verdict |
|---|---|
| `efficacy-guardian` | **NO APTO** — 5 × P0, 2 × P1 |
| `psychometrician` | **NO APTO** — 8 blockers (2 new `DESIGN_BLOCKER`s) |

They converge independently on: §4, §5, §6 and §8 of V3 are **unimplemented**; T4 (Bernoulli(.5) direction, 0
MISS) is still live; T7's invented indices still feed feedback, persistence *and* difficulty; the B3 renderer
pop-out defeats the construct; and V3's most severe finding (T3) is **already fixed in code**, i.e. the design
misdescribes the tree in both directions.

Two findings warrant the CEO's direct attention because they concern users today, and this session verified both
independently at file:line:

- **Incentive inversion (P0).** `PersonalizedSessionScorer.kt:50-51,73-74` feeds `LAPSES` and `RT_CV` into a
  0–100 score shown at `FocusSummaryScreen.kt:94-96`. The lapse threshold is `lapseRtMs = 500`
  (`UserCognitiveProfile.kt:25`) — a PVT constant — applied to a flanker whose response window is 900–1500 ms.
  A key-masher scores ≈70/100 (0 MISS, fast metronomic RT, **zero** lapse penalty); an honest filterer scores
  ≈44/100 (normal 400–650 ms RTs cross the 500 ms threshold and hit the −0.30 penalty cap after just 10
  lapses). **The flagship exercise rewards mashing over filtering.** Direction follows from the constants;
  exact magnitude needs device data.
- **Unguarded archetype route (P0/HIGH).** `ArchetypeInference.kt:54,61` scores
  `IMPULSIVE to mean(faScore, faScore, rtFast)` — `faScore` **double-weighted** — from `falseAlarmRate`, which
  in E2 is a wrong-*direction* choice error, not a commission. `:114` reads `RT_CV`. The result sets the next
  session's starting difficulty. `FocusAdaptationPolicy.FIXED` blocks `adaptiveEngine.decide` and nothing else.

**Not corrupted:** the cognitive profile. `FocusProfileSource.kt:55-61` is fail-closed and ATTENTION stays
CALIBRATING. Both reviewers confirm this holds. The damage is confined to user-facing feedback, local
persistence, and the difficulty/archetype loop.

**Genuinely sound, per both reviewers:** the canonical/identity layer (`CanonicalEncoder`,
`ReproducibleSessionIdentity`), T3 and T5.

---

## 4. Decisions required

### D-1 — Supply or withdraw the primary authority
Give §9's text, or declare the directive withdrawn and name what replaces it.
· **Approve-risk:** none.
· **Reject/ignore-risk:** every future ENG slice inherits the same unresolvable gate; "APTO" stays undefined.

### D-2 — Remedy for the gate that was already bypassed
ENG-01's wiring shipped while the gate read `PENDING`, against `SLICE_ENG00_ENG_CORE_CLOSEOUT.md:103-104`
("do not wire `core/runtime` into the generators until the blueprint is APTO"). `E2FiltroGenerator.kt:64`
(`DeterministicRng.forSeed`) is exactly the wiring that instruction named. Options:
1. **Ratify ex-post** — record a decision that the wiring stands despite the PENDING gate.
2. **Revert the wiring** until a V4 is APTO.
3. **Leave as-is** — not recommended: the code currently cites V3 §7 as "the operative acceptance criterion"
   while V3 is unapproved, so the design justifies the code and the code justifies the design.
· **Approve-risk (option 1):** normalizes shipping against open gates; the next slice cites this precedent.
· **Reject-risk (option 2):** discards a genuinely good identity layer that both reviewers want kept; the
  reproducibility gap re-opens.

### D-3 — The two P0s that affect users today
The incentive inversion and the archetype route are **live**, are **not** ENG-01's doing (they pre-date it), and
are **outside** ENG-01's declared scope. Do you want them fixed now as their own slice, or logged and scheduled?
· **Fix-now-risk:** scope creep into a slice already blocked; touches the feedback surface.
· **Defer-risk:** the flagship exercise keeps rewarding mashing over filtering for as long as it is deferred.

### D-4 — Duplicate, divergent context packs
`docs/context-pack/` and `docs/context_pack/` both declare `Versión del paquete: 2.0.0`, **neither is tracked by
git**, and they diverge:

| File | hyphen | underscore | diverging lines |
|---|---|---|---|
| `03_SCIENTIFIC_MEASUREMENT_CHARTER.md` (precedence rank **3**) | 1029 L | 1038 L | 13 |
| `05_TECHNICAL_CURRENT_STATE.md` | 769 L | 804 L | 35 |
| `09_DECISION_LOG.md` (precedence rank **5**) | 413 L | 464 L | 55 |

The `context_pack/` (underscore) copy carries the 2026-07-11 correction 6R3-P2-05 fixing the PMID attributions
(35953659 = **Nicosia** BYOD latency, not Passell; adds Passell 33954913). The `context-pack/` (hyphen) copy
retains the **incorrect** attribution — **and has the later mtime** (2026-07-12 vs 2026-07-11) and the only
`README.md`. **Resolving this duplicate by timestamp selects the scientifically wrong charter.**
· **Decision needed:** which path is authoritative, and should the pack be tracked in git?
· **Defer-risk:** every "we followed the charter" claim is ambiguous, including this packet's.

---

## 5. The freeze mechanism (answering "hash without self-reference")

`ENG_CORE_AND_FOCUS_BLUEPRINT_V3.md:5` says: *"`CANDIDATE` — frozen at the hash below."* **No hash appears
anywhere in that file** (verified: zero 40–64-char hex matches across all 167 lines). The document claims a
freeze it never performed.

This is not a clerical slip — it is unimplementable as written. **A hash printed inside a document changes the
document's hash.** A self-referential freeze cannot exist.

The correct mechanism already has a precedent in this repo: `MINDBOOST_AUDIT_LEDGER.md:70` uses a
**self-excluding** canonical hash (computed over the file with the hash line removed), and
`scripts/validate-audit-ledger.mjs` verifies it. Two options:

- **(a) External freeze (recommended):** the hash lives *outside* the document — in this packet, in the ledger,
  and in the review record. The document stays byte-stable; the record binds the verdict to `sha256:a30856eb…`.
  Zero new machinery.
- **(b) Self-excluding freeze:** copy the ledger's convention into the blueprint plus a validator.

Either way, **V3's line 5 is currently false** and must be corrected in a V4 (an edit, hence gated).

---

## 6. What this packet does NOT do

- It does **not** approve the blueprint. This session cannot self-approve a gate it is subject to.
- It does **not** change `DESIGN_REVIEW_GATE`, which remains `PENDING` — now with two `NO APTO` verdicts against
  the frozen bytes.
- It does **not** implement Phases C–I of the reopening (lifecycle races, restore branch tests, semantic plan
  coverage). Those are productive changes and the gate blocks them.

**Return state: `BLOCKED_MISSING_PRIMARY_AUTHORITY`.**
