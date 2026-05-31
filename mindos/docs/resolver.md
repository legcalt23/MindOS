# Resolver Layer

## Purpose

Converts a `PatternMatch` (matched pattern + extracted parameters) into a typed `CommandAction`. The resolver knows the *intent* of a matched pattern — it does NOT execute anything.

## Contracts

```kotlin
interface CommandResolver {
    val id: ResolverId
    suspend fun resolve(match: PatternMatch, context: ResolutionContext): ResolutionResult
}

sealed interface ResolutionResult {
    data class Resolved(val action: CommandAction, val resolverId: ResolverId) : ResolutionResult
    data class Failed(val failure: ResolutionFailure) : ResolutionResult
}
```

## Failure types

```kotlin
sealed interface ResolutionFailure {
    class NoPatternMatched(val input: String)
    class AmbiguousPattern(val input: String, val candidateIds: List<String>)
    class InvalidParameters(val patternId: String, val reason: String)
    class UnsupportedCommand(val patternId: String)
    class Unauthorized(val patternId: String, val reason: String)
    class InternalResolutionError(val message: String)
}
```

No exception is used as a failure signal.

## NavigationIntentResolver

The foundational resolver. Handles patterns whose extracted parameters include `"target"` and produces a `NavigateAction`:

```kotlin
NavigationIntentResolver(factory)
```

Register it for any pattern that uses `{target}`:
- `"open {target}"`
- `"go to {target}"`
- `"navigate to {target}"`
- `"show {target}"`

If `"target"` is missing from `match.extractedParameters`, returns `Failed(InvalidParameters(...))`.

## ResolutionContext

```kotlin
data class ResolutionContext(
    val locale: String = "en",
    val correlationId: CorrelationId? = null,
    val source: CommandSource = CommandSource.UserInput,
    val extra: Map<String, String> = emptyMap()
)
```

Resolvers may use `locale` for localized resolution or `source` to apply different policies per source.

## ResolverRegistry

Maps `PatternId → CommandResolver`. A pattern with no registered resolver causes `CommandRouterFailure.NoResolverForPattern` at routing time.

## ActionFactory

Provides deterministic `ActionMetadata` construction:

```kotlin
val factory = ActionFactory(
    idFactory = ActionIdFactory { ActionId(uuid()) },
    timestampProvider = TimestampProvider { System.currentTimeMillis() }
)
val metadata = factory.metadata(ActionTypes.Navigate, source, correlationId)
```

All resolvers should use `ActionFactory` rather than constructing `ActionMetadata` inline, to keep ID and timestamp generation injectable.
