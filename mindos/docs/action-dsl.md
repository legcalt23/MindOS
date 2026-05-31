# Action DSL

## Sealed hierarchy

```
CommandAction (sealed interface)
├── NoOpAction          — no-op; used when a command intentionally does nothing
├── UnknownAction       — unknown/unrecognized input (carries rawInput)
├── SystemAction        — internal system operations (operation + payload map)
└── NavigateAction      — navigation intent (targetId + parameters map)
```

All subtypes are `@Serializable` with stable `@SerialName` annotations. The hierarchy is closed — new actions require an explicit addition to the sealed hierarchy (intentional friction against accidental extension).

## Value types

| Type | Underlying type | Purpose |
|------|----------------|---------|
| `ActionId` | `String` | Identifies a single action instance |
| `ActionType` | `String` | Identifies the semantic kind of action |
| `CommandId` | `String` | Correlates to the originating command |
| `CorrelationId` | `String` | Cross-cutting correlation (request tracing) |

All are `@JvmInline value class` — type-safe, zero overhead on JVM, serializable as their underlying String.

## ActionMetadata

```kotlin
ActionMetadata(
    actionId    : ActionId,
    actionType  : ActionType,
    correlationId: CorrelationId? = null,
    source      : CommandSource,
    timestamp   : Long
)
```

**Timestamp is not set inside constructors.** It is provided by `TimestampProvider`, which is injected into `ActionFactory`. This keeps the model deterministic in tests.

## ActionTypes constants

```kotlin
object ActionTypes {
    val NoOp     = ActionType("noop")
    val Unknown  = ActionType("unknown")
    val System   = ActionType("system")
    val Navigate = ActionType("navigate")
}
```

All production action types are registered here. Custom types for application-specific actions should define their own `object AppActionTypes`.

## Factories

```kotlin
// Deterministic ID generation
fun interface ActionIdFactory { fun create(): ActionId }

// Deterministic timestamp
fun interface TimestampProvider { fun nowMillis(): Long }

// Combines both — use ActionFactory to build ActionMetadata
ActionFactory(idFactory, timestampProvider)
    .metadata(type, source, correlationId)
```

**No `UUID.randomUUID()` or `System.currentTimeMillis()` in the core model.** The calling layer provides these — tests use counters and fixed timestamps.

## Serialization

All `CommandAction` subtypes are fully round-trip serializable via kotlinx-serialization JSON:

```kotlin
val json = Json { encodeDefaults = true }
val action: CommandAction = NavigateAction(...)
val encoded: String = json.encodeToString(action)
val decoded: CommandAction = json.decodeFromString(encoded)
```

`@SerialName` values are stable identifiers — do not change them without a migration plan.
