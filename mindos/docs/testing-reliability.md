# MindOS Testing & Reliability Platform (ZIP04)

A complete, deterministic testing platform over the understanding (ZIP02) and execution (ZIP03)
layers. The reusable infrastructure lives in **`mind-testkit`** (commonMain, zero production deps);
the suites that consume it live in each module's `commonTest`.

## Why an in-house, seeded harness

Every generator, mutator, corpus and fuzz pass is driven by a **seeded LCG RNG** (`SeededRng`), so
any failure is reproducible from its seed — consistent with the project's determinism principle. No
`kotest`/randomness dependency; the platform is KMP-pure Kotlin + `kotlin.time`.

## mind-testkit (the harness)

| Component | Purpose |
|-----------|---------|
| `SeededRng` | Deterministic LCG PRNG (`nextInt/nextDouble/pick/fork`). |
| `CommandGenerator` + `CommandGrammar` | Generate **valid** commands from a grammar mirroring a registry; each carries its expected intent. |
| `FuzzMutator` | Mutations: spacing, case, zero-width, NFD, typo, symbol, emoji. Each tagged *semantics-preserving* or *destructive*. Special chars built from code points (encoding-safe). |
| `CorpusBuilder` | Deterministic 10k+ corpus: valid, preserving-variant, alias, misspelling, edge-case, malformed. |
| `PropertyRunner` | `forAllStrings(seed, cases, gen, property)` with light delta-shrinking; reports minimal reproducer + seed. |
| `BenchmarkHarness` | `measure` → p50/p95/p99; `regression` → baseline-budget comparison. |

## Coverage of the seven ZIP04 categories

| Category | Where | What it asserts |
|----------|-------|-----------------|
| **Unit** | every module `commonTest` (ZIP01–03) | per-component behavior (normalization, entities, patterns, compiler, matcher, validator, resolver, executor) |
| **Property** | `PropertyBasedMatchingTest` | determinism, valid→expected intent, **stable ranking under semantics-preserving mutation**, only-known-intents |
| **Fuzz** | `FuzzMatchingTest` | 6k mutated + 6k random inputs → **no crashes**, deterministic output |
| **Corpus** | `CorpusMatchingTest` | 10k+ deterministic dataset → every entry deterministic & crash-free; VALID/ALIAS hit expected intent |
| **Integration** | `IntegrationCorpusTest` | full text→understanding→action→execution pipeline over corpus; never crashes; many commands execute |
| **E2E** | `E2EScenarioTest`, `FullPipelineE2ETest` | documented scenarios (open profile/story/settings/notifications/malformed) with expected outputs |
| **Benchmark** | `BenchmarkRegressionTest` | p50/p95/p99 vs committed baseline; gate fails on gross regression |

## Properties enforced (ZIP04 PROPERTY_BASED_TESTING)

- **Deterministic output** — `understand(x) == understand(x)`, even under fuzz.
- **Stable ranking** — semantics-preserving mutations (spacing/case/zero-width/NFD) never change the
  resolved intent.
- **Valid actions only / no null** — results are always one of four sealed types; understood intents
  are always in the known set; no exceptions escape.
- **Same input ⇒ same output** — verified across the 10k corpus.

## Fuzzing objective (ZIP04 FUZZING_STRATEGY)

No crashes over random spacing, unicode variants, emoji insertion, malformed symbols and typo
mutations. Every fuzz failure prints its **seed + minimal shrunk input** for one-line reproduction.

## Benchmark & regression tracking (ZIP04 PERFORMANCE_HARNESS)

Targets P50<5ms / P95<20ms / P99<50ms. Observed on the corpus: **p50≈4.6µs, p95≈13.5µs,
p99≈26.3µs** — three orders of magnitude of headroom. `BenchmarkRegressionTest` compares p99 against
a committed baseline (`BASELINE_P99_MICROS`) with a generous tolerance; a gross (e.g. O(N))
regression breaks the budget and fails the build. Update the baseline deliberately when the engine
legitimately changes.

## Running

```bash
cd mindos/
./gradlew allTests                       # everything (222 tests)
./gradlew :mind-action-matching:jvmTest  # property + fuzz + corpus + benchmark
./gradlew :mind-testkit:jvmTest          # harness self-tests
```

## Reproducing a fuzz/property failure

The report line `seed=<S> iter=<I> input=<<...>>` is self-contained: re-run the test (same seed →
same sequence) or feed the shrunk `input` directly into `matcher.understand(...)`.

## Deferred (v1.5+)

JaCoCo/Kover branch-coverage gate (>95% target tracked, not yet enforced in CI), JMH (the harness is
a deterministic substitute), persisted baseline file + CI trend storage, multilingual corpus
expansion (grammar already supports it via slot pools/locales).
