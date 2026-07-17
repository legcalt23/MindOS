# Slice ENG-00 + ENG-CORE (kernel) — close-out

**Verdict: PARTIAL — checkpoint compilable, tested, honest. ENG-01 Focus NOT started (blocked by review).**

## 1. Outcome for the user

One thing changed for real users: **a Focus block with no responses no longer reports 100/100 sustained focus and
perfect stability.** It now reports the metric as unavailable. Nothing else user-visible changed; no new exercise
shipped, no route added.

## 2. Engine status

| | Before | After |
|---|---|---|
| Shared runtime | partially existed and unnamed (`core/session`, `core/scoring`, `core/antigaming`, `core/bank`, `core/timing`, `core/copy` — all 8 blocks import them) | unchanged + **`core/runtime` added** (seed + pinned RNG) |
| Focus | `IMPLEMENTING` (13 exercises wired) | `BLUEPRINT_PENDING` — blueprint **rejected**, see below |
| Other 7 | `IMPLEMENTING` | unchanged, untouched |

`ENGINE_CONFORMANCE_VERIFIED: 0/8` — unchanged, honestly.

## 3. Exercise status

`TRAINING_READY: 0/121` — **unchanged**. No exercise reached DoD. Reproduced, not assumed:
`validate-portfolio --mode=repo` → `TRAINING_READY 0/121`, `121/121 slots have no proven per-exercise route`,
`37 slots have no assignment`, `56 code-assigned slots`.

## 4. The baseline finding that changed the plan

MindBoost is **not greenfield**: ~58,000 lines of MindBoost commonMain across 8 blocks, **424 test files**, and
**all eight blocks reachable from the hub** (`FocusRootScreen` … `EmotionRootScreen`, verified in
`MindBoostHubScreen.kt:117-252`). Focus alone has **13 exercises wired end-to-end** with a consistency test that
fails the build if a generator is wired without a render path.

Both statements are true at once: substantial product exists **and** no exercise has passed the §10 DoD.
Building eight fresh engines beside eight real ones would have produced exactly the *ocho motores vacíos* the
directive forbids. The plan is therefore **extract and prove**, not invent.

## 5. What was implemented

`core/runtime/` — the one transversal capability from §1.1 that genuinely did not exist:

- `RngVersion`, `SessionSeed(value, version)` — a seed never travels as a bare `Long`.
- `DeterministicRng` — splitmix64, pinned, extends `kotlin.random.Random` (drop-in via `nextBits`), fail-closed
  per version.
- `SeedSource` / `PlatformSeedSource` — injected entropy, not the wall clock.

**Not built, declared:** seed persistence, exposure ledger, replay type, Focus facade. See §9.

## 6. Evidence

| Gate | Result |
|---|---|
| `:shared:desktopTest` (focus.domain.metrics + core.runtime) | **25/25 green**, exit 0 |
| `:shared:compileKotlinDesktop`, `:shared:compileAndroidMain` | exit 0 |
| `:shared:detekt` | **my files clean** (0 findings). 2 pre-existing foreign offenders remain (`FirestoreDefaults.kt`, `TimeoutDefaultsTest.kt` — `M` at session start, never touched by me) |
| copy-safety scanners ×3 | exit 0 |

**Determinism evidence:** golden vectors computed by an **independent BigInt reference**, not read back from the
Kotlin — and anchored to splitmix64's own published vector `0xE220A8397B1DCDAF` (seed 0, first draw), which the
implementation reproduces. **Mutation-tested**: flipping one bit in `MIX_2` turns the suite red; restore turns it
green. A golden test whose expectations come from its subject is a tautology; this one is not.

**Foreign failures, proven not mine:** 12 failures in `core/session/cognitive/evidence/**`. That package is
**untracked and not in HEAD** (never committed, never gated). Zero imports of my code. Decisive proof: reverting
my `DefaultMetricsEngine` hunks to HEAD leaves them failing identically (7 in the sampled class). Per CLAUDE.md
§4.10 I did not touch foreign untracked WIP.

## 7. Reviewers

- **`efficacy-guardian`: NO APTO** — 11 blockers. Four verified against code before acceptance (F6, F4, F7,
  `MIN_N_CONGRUENCY=3`). Gold ruling: **E1 Vigía conditional**; E4 and E5 disqualified; E2 runner-up.
- **`psychometrician`: REVIEW_INCOMPLETE** — the agent died with an API error mid-response. **No verdict. Not an
  approval.** Must be re-run.

Per §9, `NO APTO` ⇒ ENG-01 Focus was **not** implemented.

## 8. P0/P1

- **P0 (fixed):** F6 — zero data fabricated a perfect score. Reproduced as a failing test, fixed, regression
  test discriminating. Not a live profile breach only because `FocusProfileSource.kt:55-61` is fail-closed.
- **P1 (open, declared):** F4 inert governing axis · F7 index ignores false alarms (tap-everything wins) ·
  `MIN_N_CONGRUENCY = 3` · `vigilanceDecrement` regresses on hit ordinal not elapsed time ·
  `MetricValue.value: Double?` (the `unavailable` 0.0 sentinel).

## 9. Risks

1. **Time-critical and unrecoverable.** Seed persistence and administration context (time-of-day) cannot be
   reconstructed retroactively. Every session run before they land is permanently unauditable and unattributable.
2. `core/runtime` currently has **no production consumer** — the generators still call `Random(seed)`. It is a
   tested kernel, not yet an integration, and this close-out does not claim otherwise.
3. The psychometric lens has not reviewed anything.

## 10. Claims permitted by this slice

That a Focus block with no responses no longer fabricates a perfect score. That `core/runtime` is pinned and
tested. **Nothing about engines, exercises, TRAINING_READY, or determinism of any real session** — no real
session is reproducible, because no seed is stored.

## 11. Next cursor

**Re-run `psychometrician` on the blueprint** (never completed), then correct the blueprint against the
guardian's 11 blockers — in particular decide F1 (land seed persistence in the ENG-01 slice, or restate the gap
as open) and the E1→PVT-B reconfiguration. Only then implement ENG-01 Focus + gold. Do not wire `core/runtime`
into the generators until the blueprint is APTO — that wiring is the ENG-01 slice, not this one.
