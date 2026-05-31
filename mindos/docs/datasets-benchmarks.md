# MindOS Datasets, Benchmarks & Regression (ZIP11)

Production-grade datasets, benchmark infrastructure and regression discipline, extending the
`mind-testkit` harness (ZIP04). Everything is **deterministic and seeded** — any dataset or fuzz
failure is reproducible from its seed.

## Datasets (machine-readable assets)

Generated, not hand-written. Strategy: **exporter + committed sample + on-demand full**.

| Artifact | Where | Scale |
|----------|-------|-------|
| Per-category sample (`profiles/stories/chats/notifications/settings.json`) | committed `mind-testkit/datasets/sample/` | ~150 rows/category |
| Alias dataset (`aliases.json`) | committed | ES/EN, separate from patterns |
| Ambiguity dataset (`ambiguity.json`) | committed | curated conflict/overlap cases + expected resolution |
| Full corpus (`corpus-10k.json`) | on-demand `build/datasets/` (gitignored) | **10k+** |
| Full variations (`variations-50k.json`) | on-demand `build/datasets/` | **50k+** |

The `DatasetAssetsTest` regenerates samples deterministically (identical bytes ⇒ committed files
stay golden) and writes the full sets to `build/`. Schema (`DatasetRow`): `text`, `category`
(PROFILES/STORIES/CHATS/NOTIFICATIONS/SETTINGS), `kind`
(VALID/TYPO/SPACING/CAPITALIZATION/ALIAS/EDGE_CASE/MALFORMED), `expectedIntentId?`.

### Generator (`DatasetGenerator`)

Category-aware, deterministic. Variants: valid · typo · **spacing** · **capitalization** · alias.
`buildCorpus(n)` (valid+alias, expected intents) and `buildVariations(n)` (variant mix) scale to
10k/50k. `DatasetExporter` serializes any set to stable JSON.

### Alias dataset (separate from patterns)

`AliasDataset` stores ES/EN aliases (`settings/ajustes`, `messages/mensajes`, `notifications`,
`inbox/bandeja`…) as data, keyed to entity-less intents so each resolves standalone.

### Ambiguity dataset

`AmbiguityDataset` curates conflicting/overlapping/uncertain inputs with a **deterministic** expected
outcome; `AmbiguityDatasetTest` asserts the engine resolves each exactly the same way every run.

## Benchmarks (`BenchmarkHarness`)

Per-stage latency + throughput + scalability, deterministic, with regression budgets. Observed:

| Stage | p50 | p95 | p99 |
|-------|-----|-----|-----|
| Normalization | 0.7µs | 1.1µs | **1.4µs** |
| Matching (full understand) | 3.2µs | 7.2µs | **16µs** |
| Execution pipeline (validate→resolve→navigate) | — | — | <50µs |

Throughput: **~275k understand ops/s**. Scalability (the key property): p99 stays flat as the
registry grows — `10→13µs, 100→15µs, 500→7µs` — confirming the token-trie's O(k) lookup is
**independent of registry size** (vs the old O(N) scan). `BenchmarkRegressionTest` gates p99 against
a committed baseline so a gross regression breaks the build.

## Regression suite (`RegressionSuiteTest`)

**Every production bug becomes a permanent case here and is never removed** (ZIP11
REGRESSION_SUITE). Each case has a stable id + note and pins a behaviour forever. Current cases:

- REG-001 invalid entity ⇒ no false positive
- REG-002 exact phrase outranks equal/lower-priority parametric
- REG-003 zero-width/format injection handled safely (security guard rejects spoofing — never a
  wrong match)
- REG-004 same input fully deterministic
- REG-005 case + extra spacing normalized away
- REG-006 Spanish diacritic folding matches accented/unaccented forms

## Property generators

`PropertyRunner` + `CommandGenerator`/`FuzzMutator` drive determinism / stability / no-crash /
valid-output properties over generated and mutated commands (see `docs/testing-reliability.md`).

## Running

```bash
cd mindos/
./gradlew :mind-testkit:jvmTest            # regenerate sample assets + full sets (build/)
./gradlew :mind-action-matching:jvmTest    # ambiguity, regression, stage benchmarks, 10k/50k scale
./gradlew :mind-action-execution:jvmTest   # execution benchmark
```

## Deferred (v1.5+)

Historical performance trend storage (CI artifact + chart), memory profiling (JVM-only,
approximate), multilingual corpus expansion beyond ES/EN, and a JMH harness if sub-µs precision is
ever needed (the deterministic harness is the current substitute).
