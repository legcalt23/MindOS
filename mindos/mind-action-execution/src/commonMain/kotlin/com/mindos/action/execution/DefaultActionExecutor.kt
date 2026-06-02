package com.mindos.action.execution

import com.mindos.action.model.MindAction
import com.mindos.action.model.ReferenceKind
import com.mindos.action.navigation.MindNavigationCommand
import com.mindos.action.navigation.NavigationCoordinator
import com.mindos.action.navigation.NavigationOutcome
import com.mindos.action.observability.AuditEvent
import com.mindos.action.observability.AuditOutcome
import com.mindos.action.observability.AuditSink
import com.mindos.action.observability.MetricSample
import com.mindos.action.observability.MetricSink
import com.mindos.action.observability.MetricTags
import com.mindos.action.observability.MindTelemetryEvent
import com.mindos.action.observability.MindTelemetrySink
import com.mindos.action.observability.NoOpAuditSink
import com.mindos.action.observability.NoOpMetricSink
import com.mindos.action.observability.NoOpMindTelemetrySink
import com.mindos.action.observability.NoOpStructuredLogSink
import com.mindos.action.observability.OperationMetric
import com.mindos.action.observability.OutcomeFailure
import com.mindos.action.observability.OutcomeRejected
import com.mindos.action.observability.OutcomeSuccess
import com.mindos.action.observability.StructuredLogEvent
import com.mindos.action.observability.StructuredLogSink
import com.mindos.action.resolver.ReferenceResolutionResult
import com.mindos.action.resolver.ResolverRegistry
import com.mindos.action.validation.ActionValidator
import com.mindos.action.validation.ValidationResult
import kotlin.time.TimeSource

/**
 * The execution orchestrator. Strict, typed pipeline with observability at every stage:
 *
 *   validate ──fail──► Rejected (+ audit VALIDATION_REJECTED)
 *      │ ok
 *   resolve refs ──miss/err──► Failed (+ audit RESOLUTION_FAILED)
 *      │ ok
 *   build MindNavigationCommand
 *      │
 *   coordinator.navigate ──reject/fail──► Failed (+ audit NAVIGATION_FAILED)
 *      │ ok
 *   Executed (+ audit EXECUTED)
 *
 * Determinism: same action + same resolver store + same validator ⇒ same result. The executor
 * never parses text and never navigates directly — the coordinator owns the side-effect.
 */
class DefaultActionExecutor(
    private val validator: ActionValidator,
    private val resolvers: ResolverRegistry,
    private val coordinator: NavigationCoordinator,
    private val telemetry: MindTelemetrySink = NoOpMindTelemetrySink,
    private val audit: AuditSink = NoOpAuditSink,
    private val clock: ExecutionClock = ExecutionClocks.Zero,
    private val metrics: MetricSink = NoOpMetricSink,
    private val logs: StructuredLogSink = NoOpStructuredLogSink,
    private val metricTags: MetricTags = MetricTags.Default,
    private val idempotencyStore: IdempotencyStore? = null,
    private val idempotencyTtlMillis: Long = IdempotentActionExecutor.DefaultTtlMillis,
) : ActionExecutor {

    init {
        require(idempotencyTtlMillis > 0L) { "idempotencyTtlMillis must be positive." }
    }

    override suspend fun execute(action: MindAction, context: ExecutionContext): ExecutionResult {
        val mark = TimeSource.Monotonic.markNow()
        val result = executeInternal(action, context)
        val latencyMicros = mark.elapsedNow().inWholeMicroseconds
        telemetry.record(MindTelemetryEvent.ExecutionLatency(latencyMicros))
        recordOperationalObservation(action, context, result, latencyMicros)
        return result
    }

    private suspend fun executeInternal(action: MindAction, context: ExecutionContext): ExecutionResult {
        // 1) Validate — invalid actions never execute.
        when (val validation = validator.validate(action, context.validation)) {
            is ValidationResult.Invalid -> {
                telemetry.record(
                    MindTelemetryEvent.ValidationFailed(action.actionType, validation.failures.joinToString(";") { it.detail }),
                )
                recordAudit(action, AuditOutcome.VALIDATION_REJECTED, "validation")
                return ExecutionResult.Rejected(validation.failures)
            }
            ValidationResult.Valid -> Unit
        }

        // 2) Resolve every reference to a canonical id. Chained actions must resolve in order.
        val resolved = when (val resolution = resolveAllReferences(action)) {
            is ReferenceResolution.Success -> resolution.resolved
            is ReferenceResolution.Failed -> return resolution.result
        }

        // 3) Build the typed navigation command from resolved ids.
        val command = buildCommand(action, resolved)

        // 4) Replay/idempotency check after validation and resolution, before navigation.
        val idempotencyKey = context.sessionId
            ?.takeIf { it.isNotBlank() }
            ?.let { IdempotencyKey(sessionId = it, requestId = action.requestId.value) }
        if (idempotencyStore != null && idempotencyKey != null) {
            when (
                val decision = idempotencyStore.tryBegin(
                    idempotencyKey,
                    action.idempotencyFingerprint(),
                    idempotencyTtlMillis,
                )
            ) {
                IdempotencyDecision.Begin -> Unit
                is IdempotencyDecision.Replay -> {
                    recordAudit(action, AuditOutcome.IDEMPOTENT_REPLAY, "idempotent_replay")
                    return ExecutionResult.Failed(ExecutionFailure.IdempotentReplay("request already processed"))
                }
                is IdempotencyDecision.Conflict -> {
                    recordAudit(action, AuditOutcome.REPLAY_REJECTED, "replay_rejected")
                    return ExecutionResult.Failed(
                        ExecutionFailure.ReplayRejected("requestId reused with a different action fingerprint"),
                    )
                }
            }
        }

        // 5) Hand to the coordinator, which owns the navigation side-effect.
        val result = when (val outcome = coordinator.navigate(command)) {
            NavigationOutcome.Success -> {
                telemetry.record(MindTelemetryEvent.NavigationSuccess(command.commandType))
                telemetry.record(MindTelemetryEvent.ActionExecuted(action.actionType))
                recordAudit(action, AuditOutcome.EXECUTED, null)
                ExecutionResult.Executed(command)
            }
            is NavigationOutcome.Rejected -> {
                telemetry.record(MindTelemetryEvent.NavigationFailed(command.commandType, outcome.reason))
                recordAudit(action, AuditOutcome.NAVIGATION_FAILED, "rejected")
                ExecutionResult.Failed(ExecutionFailure.NavigationRejected(outcome.reason))
            }
            is NavigationOutcome.Failed -> {
                telemetry.record(MindTelemetryEvent.NavigationFailed(command.commandType, outcome.message))
                recordAudit(action, AuditOutcome.NAVIGATION_FAILED, "failed")
                ExecutionResult.Failed(ExecutionFailure.NavigationFailed(outcome.message))
            }
        }
        if (idempotencyStore != null && idempotencyKey != null) {
            when (result) {
                is ExecutionResult.Executed -> idempotencyStore.markSucceeded(idempotencyKey, command.commandType)
                is ExecutionResult.Failed -> idempotencyStore.markFailed(idempotencyKey, result.failure.idempotencyFailureCode())
                is ExecutionResult.Rejected -> idempotencyStore.markFailed(idempotencyKey, "validation")
            }
        }
        return result
    }

    private fun buildCommand(action: MindAction, resolved: Map<ReferenceKind, String>): MindNavigationCommand =
        when (action) {
            is MindAction.OpenProfile -> MindNavigationCommand.OpenProfile(resolved.getValue(ReferenceKind.USERNAME_TO_USER_ID))
            is MindAction.OpenStory -> MindNavigationCommand.OpenStory(resolved.getValue(ReferenceKind.STORY_SLUG_TO_STORY_ID))
            is MindAction.OpenStoryOfUser -> MindNavigationCommand.OpenStory(resolved.getValue(ReferenceKind.CURRENT_STORY_BY_USER_ID))
            is MindAction.OpenChat -> MindNavigationCommand.OpenChat(resolved.getValue(ReferenceKind.CHAT_ALIAS_TO_CHAT_ID))
            is MindAction.OpenMessages -> MindNavigationCommand.OpenMessages
            is MindAction.OpenSettings -> MindNavigationCommand.OpenSettings
        }

    private suspend fun resolveAllReferences(action: MindAction): ReferenceResolution {
        return when (action) {
            is MindAction.OpenStoryOfUser -> {
                val userId = when (
                    val user = resolveReference(action, ReferenceKind.USERNAME_TO_USER_ID, action.userRef)
                ) {
                    is SingleReferenceResolution.Success -> user.canonicalId
                    is SingleReferenceResolution.Failed -> return ReferenceResolution.Failed(user.result)
                }
                val storyId = when (
                    val story = resolveReference(action, ReferenceKind.CURRENT_STORY_BY_USER_ID, userId)
                ) {
                    is SingleReferenceResolution.Success -> story.canonicalId
                    is SingleReferenceResolution.Failed -> return ReferenceResolution.Failed(story.result)
                }
                ReferenceResolution.Success(
                    mapOf(
                        ReferenceKind.USERNAME_TO_USER_ID to userId,
                        ReferenceKind.CURRENT_STORY_BY_USER_ID to storyId,
                    ),
                )
            }
            else -> {
                val resolved = HashMap<ReferenceKind, String>()
                for (reference in action.references()) {
                    when (val outcome = resolveReference(action, reference.kind, reference.rawValue)) {
                        is SingleReferenceResolution.Success -> resolved[reference.kind] = outcome.canonicalId
                        is SingleReferenceResolution.Failed -> return ReferenceResolution.Failed(outcome.result)
                    }
                }
                ReferenceResolution.Success(resolved)
            }
        }
    }

    private suspend fun resolveReference(
        action: MindAction,
        kind: ReferenceKind,
        rawValue: String,
    ): SingleReferenceResolution {
        val resolver = resolvers.forKind(kind)
        if (resolver == null) {
            telemetry.record(MindTelemetryEvent.ResolverFailed(kind.id, "no_resolver"))
            recordAudit(action, AuditOutcome.RESOLUTION_FAILED, "resolver_missing")
            return SingleReferenceResolution.Failed(
                ExecutionResult.Failed(
                    ExecutionFailure.ResolutionFailed(kind, rawValue, "no resolver for '${kind.id}'"),
                ),
            )
        }
        return when (val outcome = resolver.resolve(rawValue)) {
            is ReferenceResolutionResult.Resolved -> {
                telemetry.record(MindTelemetryEvent.ResolverSuccess(kind.id))
                SingleReferenceResolution.Success(outcome.canonicalId)
            }
            is ReferenceResolutionResult.NotFound -> {
                telemetry.record(MindTelemetryEvent.ResolverFailed(kind.id, "not_found"))
                recordAudit(action, AuditOutcome.RESOLUTION_FAILED, "not_found")
                SingleReferenceResolution.Failed(
                    ExecutionResult.Failed(
                        ExecutionFailure.ResolutionFailed(kind, rawValue, "'$rawValue' not found"),
                    ),
                )
            }
            is ReferenceResolutionResult.Failed -> {
                telemetry.record(MindTelemetryEvent.ResolverFailed(kind.id, "error"))
                recordAudit(action, AuditOutcome.RESOLUTION_FAILED, "resolver_error")
                SingleReferenceResolution.Failed(
                    ExecutionResult.Failed(
                        ExecutionFailure.ResolutionFailed(kind, rawValue, outcome.message),
                    ),
                )
            }
        }
    }

    private fun recordAudit(action: MindAction, outcome: AuditOutcome, errorCode: String?) {
        audit.record(AuditEvent(action.requestId.value, action.actionType, clock.nowMillis(), outcome, errorCode))
    }

    private fun recordOperationalObservation(
        action: MindAction,
        context: ExecutionContext,
        result: ExecutionResult,
        latencyMicros: Long,
    ) {
        val timestamp = clock.nowMillis()
        val tags = metricTags.withCommandType(action.actionType)
        val outcome = when (result) {
            is ExecutionResult.Executed -> OutcomeSuccess
            is ExecutionResult.Rejected -> OutcomeRejected
            is ExecutionResult.Failed -> OutcomeFailure
        }

        metrics.record(MetricSample(OperationMetric.LATENCY_SAMPLE, latencyMicros / 1_000.0, tags, timestamp))
        metrics.record(MetricSample(OperationMetric.ACTION_EXECUTION_RATE, 1.0, tags.withOutcome(outcome), timestamp))
        when (result) {
            is ExecutionResult.Executed -> Unit
            is ExecutionResult.Rejected -> {
                metrics.record(MetricSample(OperationMetric.VALIDATION_FAILURE_RATE, 1.0, tags.withOutcome(OutcomeRejected), timestamp))
            }
            is ExecutionResult.Failed -> {
                if (result.failure is ExecutionFailure.ResolutionFailed) {
                    metrics.record(MetricSample(OperationMetric.RESOLVER_DEGRADATION_RATE, 1.0, tags.withOutcome(OutcomeFailure), timestamp))
                }
            }
        }

        logs.record(
            StructuredLogEvent(
                requestId = action.requestId.value,
                sessionId = context.sessionId ?: StructuredLogEvent.AnonymousSessionId,
                timestampMillis = timestamp,
                action = action.actionType,
                outcome = when (result) {
                    is ExecutionResult.Executed -> "executed"
                    is ExecutionResult.Rejected -> "rejected"
                    is ExecutionResult.Failed -> "failed"
                },
                latencyMs = (latencyMicros + 999L) / 1_000L,
                errorCode = when (result) {
                    is ExecutionResult.Executed -> null
                    is ExecutionResult.Rejected -> "validation"
                    is ExecutionResult.Failed -> result.failure.telemetryCode()
                },
                attributes = mapOf("commandType" to action.actionType),
            ),
        )
    }

    private fun ExecutionFailure.telemetryCode(): String =
        when (this) {
            is ExecutionFailure.ResolutionFailed -> "resolution_failed"
            is ExecutionFailure.NavigationRejected -> "navigation_rejected"
            is ExecutionFailure.NavigationFailed -> "navigation_failed"
            is ExecutionFailure.UnsupportedAction -> "unsupported_action"
            is ExecutionFailure.IdempotentReplay -> "idempotent_replay"
            is ExecutionFailure.ReplayRejected -> "replay_rejected"
        }

    private sealed interface ReferenceResolution {
        data class Success(val resolved: Map<ReferenceKind, String>) : ReferenceResolution
        data class Failed(val result: ExecutionResult.Failed) : ReferenceResolution
    }

    private sealed interface SingleReferenceResolution {
        data class Success(val canonicalId: String) : SingleReferenceResolution
        data class Failed(val result: ExecutionResult.Failed) : SingleReferenceResolution
    }
}
