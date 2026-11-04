# S5_R3_R2_DOMAIN_SPEC — the exact enumerated domain behind "1363/1363"

```
SOURCE: docs/engines/focus_v5/reaudit/r2_falsification_harness.mjs (searchFlips, lines 252-288)
RESULT: docs/engines/focus_v5/reaudit/r2_falsification_result.json (broad_search)
CLAIM WORDING (mandated): 1363/1363 WITHIN_ENUMERATED_DOMAIN — NOT "generic", "universal", or "near-universal".
DETERMINISM: no RNG, no clock. Same input → same output → same output-digest. No seeds needed.
```

## 1. The count space (before any filter)

Four nested integer loops over SDT cell counts:

| dimension | range | step | values |
|---|---|---|---|
| hits | 20 … 60 | 5 | 9 |
| misses | 5 … 30 | 5 | 6 |
| false alarms (fa) | 5 … 30 | 5 | 6 |
| correct rejections (cr) | 20 … 60 | 10 | 5 |

Cartesian product = 9 × 6 × 6 × 5 = **1620 candidate base cells**.

## 2. Feasibility filter (what "feasible" means)

A base is **feasible** iff the FULL cell's d′ 95% interval clears zero: `ciLower(full) > 0`. Bases failing this are
`continue`-skipped and never counted, because the R2 gate can never return `Present` for them — there is no
`Absent → Present` transition to look for.

- Feasible (tested): **1363**
- Infeasible (structural no-flip space): 1620 − 1363 = **257** — these are the honest "cases without a flip":
  bases where the gate is unconditionally not-Present, so no monotonicity violation is even definable.

## 3. Search strategy per feasible base (nested exclusion)

The realistic exclusion path (ADR §R3-2): exclude technically-invalid windows in a **nested (superset) sequence**,
never a different set.

1. For `em = 0 … misses`: build `admLess` = drop `em` misses (keep all hits). If `r2Gate(full, admLess)` is
   **Absent**, proceed (this is the state raising d′ above `S_full`).
2. Then for `eh = 1 … maxEh` (with `maxEh` keeping admissible signal ≥ `MIN_ADMISSIBLE = 20`): build `admMore`
   = additionally drop `eh` hits (a strict superset of the exclusion in step 1). If `r2Gate(full, admMore)` is
   **Present**, record a **flip** and stop.

`admMore` removes strictly MORE windows than `admLess`, so the transition is genuinely "more technically-invalid
windows removed → gate flips Absent→Present" — the non-monotonicity the R2 design cannot survive.

## 4. Result

- Feasible bases tested: **1363**
- Bases exhibiting an `Absent → Present`-under-more-exclusion flip: **1363**
- Fraction: **1.0** → **1363/1363 WITHIN_ENUMERATED_DOMAIN**

This is a bounded property over the enumerated grid in §1 filtered by §2 — NOT a universal claim over all real
number cells. The honest statement: *within this enumerated, feasibility-filtered domain, every base admits a
nested-exclusion path that drives the R2 gate from Absent to Present*. The two hand-built cases (Base A
30/10/10/30, Base B 60/20/20/60) are members of this set, not the whole evidence.

## 5. Negative controls

- **Structural negative space:** the 257 infeasible bases (§2) — no flip is definable; the search correctly finds
  none because it never enters them.
- **The FIXED contract is the positive negative-control:** `r3_contract_checks` in the same harness sweeps the R3
  replacement `E = g(excludedFraction)` and finds **non_interference_violations = 0 / 63 checks** and
  **monotonicity_violations = 0** over a 0→1 sweep (101 steps). The same machinery that finds 1363 flips in R2
  finds **zero** in R3 — so the search is not trivially "always finds a flip". (Contract-level only; MNAR of
  `excludedFraction` itself is a separate empirical question — MB-FOCUS-0053.)
- **Production parity (NEW, EXECUTED_PRIMARY):** the canonical counterexample's three d′ values
  (1.310847010469 / 1.680015897775 / 1.292907666197) and the non-monotonic flip are pinned on the REAL Kotlin
  `DescriptiveScorer` by `DescriptiveScorerGoldenReauditTest` (2/2 green — see `S5_R3_KOTLIN_EXECUTION_REPORT.md`).

## 6. Proof sketch (why the flip is structural, not coincidental)

Dropping misses raises the admissible hit-rate, so `S_adm` rises above `S_full`; the R2 gate flags this
score-inflation-under-admission as **Absent** (a within-session integrity trip). Dropping hits then lowers
`S_adm` back below `S_full`, at which point the gate no longer trips and returns **Present**. The gate's verdict
is thus **non-monotone in the amount of exclusion** — more cleaning can turn a rejected session into an accepted
one. The R3 replacement removes the score dependence entirely (`E` depends only on `excludedFraction`, one-sided),
which is why its monotonicity sweep is violation-free.

## 7. Reproduce

```bash
node docs/engines/focus_v5/reaudit/r2_falsification_harness.mjs
# stdout ends: self_digest_sha256=2df300f3e80f361e62eabfb97176bc04dd1e96acb44978a4052a6c839df27452
```

`self_digest_sha256` (`2df300f3…`) is the sha256 of the deterministic OUTPUT JSON. The harness SOURCE file sha256
is `6d7220ff5e4af474faf6337066227d2bf9a072f59e479b5c518dbd2c20e9a0e5` (in the external manifest). Two different
hashes of two different things — do not conflate them.
