# R3-A — Sums migration: two documents, two semantics, zero crossings

## Why the rename happened

Two different documents were both named `SHA256SUMS.txt`:

| | Repo delivery | Bundle |
|---|---|---|
| **Name** | `SHA256SUMS.txt` | `BUNDLE_SHA256SUMS.txt` (renamed) |
| **Describes** | the 40-line repo delivery set | the 48 artifacts inside the container |
| **Written by** | `gen-delivery.mjs` | `build-bundle.mjs` (stage **and** repo) |
| **Self-exclusion** | `SHA256SUMS.txt` + `DELIVERY_MANIFEST.json` | `BUNDLE_MANIFEST.json` **only** |
| **Mode** | repo | bundle |

Same filename, different sets, different self-exclusion rules. A reader — human or code — could hold one and apply
the other's semantics without any error firing.

## Consumer inventory (complete)

### `SHA256SUMS.txt` — repo delivery, repo mode only

| Consumer | Line | Role |
|---|---|---|
| `gen-delivery.mjs` | 66 | **writer** |
| `validate-portfolio.mjs` | 40, 323–334 | reader — `IN_REPO_PACKAGE_GATE`, runs under `--mode=repo` |
| `mutation-test.mjs` | 25, 58, 59 | reader — mutates it, asserts `IN_REPO_PACKAGE_GATE` goes red |
| `phase4-fail-before.mjs` | 33–45 | reader — pre-flight pin check |
| `phase4-r1-fail-before.mjs` | 55–64 | reader — pre-flight pin check |

### `BUNDLE_SHA256SUMS.txt` — bundle, bundle mode only

| Consumer | Line | Role |
|---|---|---|
| `build-bundle.mjs` | 165–168 | **writer** (stage + repo), then hashed into `BUNDLE_MANIFEST.json` |
| `validate-bundle.mjs` | 38–65 | reader — `DELIVERY_INTEGRITY_GATE`, both set directions |
| `clean-room-test.mjs` | 124–132 | reader — re-verified from `inputRoot` |
| `phase5-mutation-tests.mjs` | 351–380 | reader — P5-19, both divergence directions |

**No consumer reads one and applies the other's semantics.** The two sets are disjoint by writer, by reader, and
by mode.

## One crossing found and fixed (R3-A3)

`phase5-mutation-tests.mjs` P5-19 held the **repo-mode filename in a bundle-mode SELF set**:

```js
const SELF = new Set(['BUNDLE_MANIFEST.json', 'SHA256SUMS.txt']);   // 'SHA256SUMS.txt' is never in a bundle
```

The stale name was the visible symptom. The disease underneath was worse: **P5-19 was a tautology.**

- `m.artifacts` never contains `BUNDLE_MANIFEST.json` (self-excluded) and never contains `SHA256SUMS.txt`, so
  `SELF` filtered nothing → `declared.size === m.artifacts.length` was **trivially true**.
- `divergent` was built by deleting one element from `declared`; `missing` then recovered exactly that element →
  `missing.length === 1` was **arithmetic on a set built one line earlier**.
- **`BUNDLE_SHA256SUMS.txt` was never opened.** The test that existed to prove sums and manifest agree never read
  the sums.
- Its message claimed "the reverse is checked too". Nothing was checked.

This is the third oracle of this family in V2d (after the clean room's `6/10 as 6/6` and P8A-08). All three shared
one shape: **an oracle whose terms cannot be false.**

**Fix:** P5-19 now reads both files off disk, asserts they agree today (`agreesNow`), then mutates each direction
independently — drop a path from the sums (*manifest declares X, sums omits*) and add a ghost (*sums lists Y, the
manifest does not declare*).

**Discrimination proved by divergence, not asserted:**

| Arm | P5-19 |
|---|---|
| sums intact | **PASS** |
| one line dropped from `BUNDLE_SHA256SUMS.txt` | **FAIL** |

(File restored byte-identical afterwards: `79ed4255…` before and after.)

## Residual

`build-bundle.mjs` comments still discuss the rule using the bare word "SHA256SUMS". Prose, not code — no consumer
resolves a path from it. Left as-is; renaming prose carries no integrity benefit and would churn the snapshot.
