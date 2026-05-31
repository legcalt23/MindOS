# MindOS Action Execution Layer (ZIP03)

The **execution half** of MindOS, downstream of the understanding layer (ZIP02). It takes a
recognized intent, turns it into a typed action, validates it, resolves human-facing references to
canonical ids, produces a typed navigation command, and hands it to a coordinator that **owns the
navigation side-effect**. Telemetry and an immutable audit trail wrap every stage.

```
text ─[ZIP02]─► MindIntent + entities
     ─[IntentToActionMapper]─► MindAction (raw refs)
     ─[ActionValidator]──────► valid | Rejected (typed)
     ─[ReferenceResolver]────► canonical ids | Failed (typed)
     ─► MindNavigationCommand (typed, resolved ids)
     ─[NavigationCoordinator]► NavigationOutcome   ◄── owns the side-effect (Decompose boundary)
     ─► telemetry + immutable audit
```

## Modules & boundaries

| Module | Package | Owns | Depends on |
|--------|---------|------|------------|
| `mind-action-model` | `com.mindos.action.model` | sealed `MindAction`, `ReferenceKind`, `RequestId` | — |
| `mind-navigation` | `com.mindos.action.navigation` | sealed `MindNavigationCommand`, `NavigationCoordinator`, `NavigationOutcome` | — |
| `mind-reference-resolver` | `com.mindos.action.resolver` | `ReferenceResolver`, `ResolverRegistry`, in-memory + caching impls | model |
| `mind-action-validation` | `com.mindos.action.validation` | `ActionValidator`, `Authorizer`, `ValidationResult` | model |
| `mind-action-execution` | `com.mindos.action.execution` | `ActionExecutor`, `IntentToActionMapper`, results | model, navigation, resolver, validation, observability, understanding, entity |
| `mind-observability` (extended) | `com.mindos.action.observability` | `AuditSink`/`AuditEvent`, execution telemetry events | — |

Acyclic. The executor depends on **interfaces** (`ActionValidator`, `ReferenceResolver`,
`NavigationCoordinator`) — every collaborator is swappable and fakeable.

## Interfaces

```kotlin
interface ActionExecutor { suspend fun execute(action: MindAction, context: ExecutionContext): ExecutionResult }
interface ActionValidator { suspend fun validate(action: MindAction, context: ValidationContext): ValidationResult }
interface ReferenceResolver { val kind: ReferenceKind; suspend fun resolve(rawValue: String): ReferenceResolutionResult }
interface NavigationCoordinator { suspend fun navigate(command: MindNavigationCommand): NavigationOutcome }
fun interface Authorizer { suspend fun isAuthorized(action: MindAction, context: ValidationContext): Boolean }
```

## Sequence (happy path — "open ana")

```
caller → ActionExecutor.execute(OpenProfile(req, "ana"))
       → ActionValidator.validate  ............ Valid
       → ReferenceResolver(USERNAME_TO_USER_ID).resolve("ana") → Resolved("u_ana")
       → build MindNavigationCommand.OpenProfile("u_ana")
       → NavigationCoordinator.navigate(OpenProfile("u_ana")) → Success
       → telemetry: ResolverSuccess, NavigationSuccess, ActionExecuted, ExecutionLatency
       → audit: AuditEvent(req, open_profile, ts, EXECUTED)
       ← ExecutionResult.Executed(OpenProfile("u_ana"))
```

Short-circuits (coordinator is **never** reached):
- validation fails → `Rejected(failures)` + audit `VALIDATION_REJECTED`
- reference unresolved/missing resolver → `Failed(ResolutionFailed)` + audit `RESOLUTION_FAILED`

## Error strategy

All outcomes are **typed**, never exceptions for expected paths:
- `ExecutionResult`: `Executed | Rejected(failures) | Failed(failure)`
- `ExecutionFailure`: `ResolutionFailed | NavigationRejected | NavigationFailed | UnsupportedAction`
- `ValidationFailure`: `MissingField | MalformedField | Unauthorized | ResolverNotReady`
- `ReferenceResolutionResult`: `Resolved | NotFound | Failed`
- `NavigationOutcome`: `Success | Rejected | Failed`

`CancellationException` is always re-thrown (structured concurrency preserved); only genuine
errors map to typed failures.

## Retry strategy

**No hidden retries** — determinism first. `execute` is pure given a fixed resolver store, so retry
is **caller-driven and idempotent**: the `RequestId` correlates attempts, and re-executing the same
action is safe (validation + resolution recompute identically; the coordinator is responsible for
idempotent navigation). A backoff/retry policy around `ReferenceResolver` (transient I/O) is a
deliberate v1.5 extension behind an explicit policy object — never an implicit loop.

## Validation strategy

`DefaultActionValidator` collects **all** failures (not fail-fast): required fields (non-blank
references), field format (length bound), **authorization** (injected `Authorizer`), and **resolver
readiness** (`ValidationContext.readyReferenceKinds`). **Invalid actions never execute** — the
executor returns `Rejected` before touching the resolver or coordinator.

## Reference resolution

`ReferenceResolver` maps `username→userId`, `storySlug→storyId`, `chatAlias→chatId` per
`ReferenceKind`. Deterministic, cacheable (`CachingReferenceResolver` caches hits only — a missing
username may be created later), testable. The real (Firestore) implementation is injected by the
app; this module ships an `InMemoryReferenceResolver` reference impl.

## Observability

- **Telemetry** (`MindTelemetrySink`): `action_executed`, `validation_failed`, `resolver_success`,
  `resolver_failed`, `navigation_success`, `navigation_failed`, `execution_latency`.
- **Operations** (`MetricSink`, `StructuredLogSink`): action execution rate, validation failure
  rate, resolver degradation rate, latency samples, and request/session-correlated structured logs.
- **Audit** (`AuditSink`, immutable `AuditEvent`): `requestId`, `actionType`, `timestamp`
  (injected `ExecutionClock`), `outcome` (`EXECUTED | VALIDATION_REJECTED | RESOLUTION_FAILED |
  NAVIGATION_FAILED`), `errorCode`. Append-only.

Both default to no-op and are injected — no OpenTelemetry/Firebase coupling in these modules.

## Minimal usage

```kotlin
val executor = DefaultActionExecutor(
    validator   = DefaultActionValidator(authorizer = myAuthorizer),
    resolvers   = ResolverRegistry(listOf(myUsernameResolver, myStoryResolver, myChatResolver)),
    coordinator = myDecomposeCoordinator,   // owns navigation
    telemetry   = myTelemetrySink,
    audit       = myAuditSink,
    clock       = ExecutionClock { realEpochMillis() },
)

// Bridge from understanding (ZIP02):
val mapper = IntentToActionMapper(requestIds = RequestIdFactory { RequestId(uuid()) })
when (val u = matcher.understand("open ana")) {
    is ActionUnderstandingResult.Understood ->
        mapper.map(u.intent, u.entities)?.let { executor.execute(it) }
    else -> Unit
}
```

## Prohibited (by design)

The executor never parses text, never touches UI/Compose, never navigates directly. The matcher
never executes. Only the `NavigationCoordinator` performs navigation. No reflection, no hidden
global state, no implicit retries, no Decompose/Android dependency in these modules.
