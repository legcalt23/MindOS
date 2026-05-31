# MindOS Action Understanding Layer (ZIP02)

A deterministic, typed, explainable **action understanding** layer that sits *upstream* of the
ZIP01 command-router foundation. It turns raw text into a recognized **intent + typed entities**;
it does **not** execute and does **not** navigate.

```
input ──► [understanding] ──► MindIntent + entities ──► resolver ──► CommandAction ──► executor ──► Navigator
          (mind-* modules)                              (foundation: command-router-resolver / action-executor)
```

> The matcher never executes. The resolver maps intent→action. The **executor owns navigation**.
> All preserved from ZIP01.

## Modules & boundaries

| Module | Package | Owns | Depends on |
|--------|---------|------|------------|
| `mind-normalization` | `com.mindos.action.normalization` | Pipeline, locale/Unicode handling, single tokenization | — |
| `mind-observability` | `com.mindos.action.observability` | `MatchTrace`, `MindTelemetrySink`, diagnostics | — |
| `mind-entity-extraction` | `com.mindos.action.entity` | `EntityType`, `ExtractedEntity`, extractors, validators | normalization |
| `mind-action-core` | `com.mindos.action.understanding` | `ActionMatcher`, `ActionUnderstandingResult`, `MindIntent` (contracts) | entity, observability |
| `mind-action-patterns` | `com.mindos.action.pattern` | `ActionPattern`, `ParameterSlot`, `RegistrySource`, DSL | core, entity |
| `mind-action-matching` | `com.mindos.action.matching` | `RegistryCompiler`, `CompiledRegistry`, indices, trie, ranking, `DefaultActionMatcher` | core, patterns, entity, normalization, observability |

Dependency graph is acyclic. **`mind-action-core` depends only on interfaces** — the concrete
engine (`mind-action-matching`) can be swapped without touching callers.

## Matching strategy

Hybrid layered, deterministic, no fuzzy/NLP/ML:

```
Exact (O(1) hash) → Alias (O(1) hash, by intent+locale) → Parametric trie (O(k)) + typed entity validation → deterministic ranking
```

- **Exact / Alias**: `HashMap` lookups on the normalized text.
- **Parametric**: static-token trie with typed slot branches; the candidate set per request is
  proportional to input length, not registry size.
- **Entity validation**: each slot declares an `EntityType`; the extractor validates the shape.
  A value that fails its type is **rejected**, never coerced — this is the false-positive guard.

## Ranking (fully auditable, no magic)

`MatchScore` orders by, in strict priority: `priority` → `exactness (exact>alias>parametric)` →
`staticTokenMatches` → `typedSlotMatches` → `freeTextPenalty (lower better)` → `localeMatch` →
`declarationOrder (stable, lower better)`. **There is no lexicographic PatternId tie-break.**

## Registry compiler (fail at build, not in prod)

`RegistryCompiler.compile(source)` validates and indexes once, collecting **all** issues:
duplicate ids/phrases/aliases, template parse errors, slots without an extractor, illegal greedy
slots, and same-shape/same-priority/different-intent **pattern conflicts** (error by default;
warning with `failOnConflicts = false`). Incomplete locale coverage and empty registries warn.

## Failure model (all typed, never exceptions for control flow)

- `ActionUnderstandingResult`: `Understood` | `Ambiguous` | `Rejected` | `NoMatch`
- `RejectionReason`: `EmptyInput` | `InputTooLong` | `InvalidEntity` | `UnsupportedInput`
- `EntityExtractionResult.Rejected(EntityRejectionReason)` — shape/length/token-count failures
- `RegistryCompilationResult.Failure(errors)` — build-time

## Minimal usage

```kotlin
val source = registrySource {
    exact("p_settings", "open settings", intent = "open_settings")
    alias("ajustes", locale = "es", intent = "open_settings")
    parametric("p_profile", "open {target:username}", intent = "open_profile")
}
val registry = RegistryCompiler.compile(source).getOrThrow()
val matcher: ActionMatcher = DefaultActionMatcher(registry)

when (val r = matcher.understand("open @ana")) {
    is ActionUnderstandingResult.Understood -> {
        // r.intent.id == open_profile, r.entities["target"].value == "ana"
        // App resolver: MindIntent + entities → CommandAction → executor.execute(...)
    }
    is ActionUnderstandingResult.Ambiguous -> { /* surface candidates */ }
    is ActionUnderstandingResult.Rejected  -> { /* r.reason */ }
    is ActionUnderstandingResult.NoMatch   -> { /* fall through */ }
}
```

## Entity set (v1)

`Username, Mention, Hashtag, UserId, ProfileId, StoryId, ChatId, Numeric, FreeText`.
**Deferred to v1.5**: `DateValue, DateRange, RelativeDate, Duration, Location, Url, Email`
(date/time normalization is ambiguous and timezone-dependent).

## Normalization (moderate, locale-configurable)

trim → strip control/zero-width → Unicode NFC → invariant case-fold → collapse whitespace →
single tokenization. Diacritic folding is **opt-in per locale** (`NormalizationConfig.Spanish`).
Each token keeps `raw` (case-preserved, for ids/free-text), `display` (lowercased), and
`normalized` (optionally folded). **No per-request regex.**

## Observability

`MindTelemetrySink` (default `NoOpMindTelemetrySink`) emits: `exact_hit`, `alias_hit`,
`parametric_hit`, `entity_rejected`, `ambiguous`, `no_match`, `registry_compiled`,
`registry_compile_failed`, `match_latency`. `no_match` carries no raw or normalized user text.
`MatchTrace` (explain mode only) records candidates,
extracted/rejected entities and the winner. Both are **off the hot path**.

## Performance

Smoke benchmark on the sample registry: **p50 ≈ 4µs, p95 ≈ 9µs, p99 ≈ 14µs** — far under the
P50<5ms / P95<20ms / P99<50ms targets. Hot path: one normalization (no regex), O(1) index
lookups, bounded trie walk.

## Prohibited (by design)

No fuzzy/typo tolerance, no NLP, no intent graph, no ML ranking, no reflection, no hidden global
mutable state, no lexicographic id tie-break, no UI/Android/Firebase coupling, no executing or
navigating from the matcher. Precision before tolerance.
