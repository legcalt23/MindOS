# MindOS KMP Core Implementation (ZIP09)

ZIP09 provides the production KMP implementation over the public ZIP08 contracts. The code lives in
`mind-kmp-core` under package `com.mindos.core`.

## Implemented Components

| ZIP09 file | Implementation |
|------------|----------------|
| `ROUTER/CommandRouterImpl.kt` | `CommandRouterImpl : KmpCommandRouter` |
| `MATCHING/PatternMatcherImpl.kt` | `PatternMatcherImpl` using the deterministic `DefaultActionMatcher` engine |
| `MATCHING/AliasMatcherImpl.kt` | `AliasMatcherImpl` for localized and `und` fallback aliases |
| `NORMALIZATION/NormalizationPipeline.kt` | `NormalizationPipelineImpl` wrapper over `mind-normalization` |
| `EXTRACTION/EntityExtractorImpl.kt` | `EntityExtractorImpl` over v1 extractors plus ISO date shape support |
| `VALIDATION/ActionValidatorImpl.kt` | `ActionValidatorImpl` over ZIP08 contract validation |
| `ERRORS/ErrorModel.kt` | `RouterError` and typed mapping to `RouterFailureDto` |

## Architecture

`mind-kmp-core` is an adapter layer, not a second matcher. It reuses:

- `mind-action-matching` for exact, alias, parametric, and ranking behavior
- `mind-normalization` for deterministic Unicode/space/case normalization
- `mind-entity-extraction` for username, mention, hashtag, id, numeric, and free text extraction
- `mind-kmp-contracts` for public DTOs and JSON boundaries

This keeps the public KMP API stable while preserving the tested internal engine.

## API

```kotlin
val router = CommandRouterImpl()
val result = router.route(RouterRequestDto("req-1", "open @ana"))
```

For stream processing:

```kotlin
router.routeAll(requestFlow)
```

## Determinism

The router is deterministic for fixed registry/config/input. Pattern registries compile at
construction time, so invalid registries fail before serving requests. Runtime expected failures are
typed `RouterFailureDto` values, not thrown exceptions.
