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
) : ActionExecutor {

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

        // 2) Resolve every reference to a canonical id.
        val resolved = HashMap<ReferenceKind, String>()
        for (reference in action.references()) {
            val resolver = resolvers.forKind(reference.kind)
            if (resolver == null) {
                telemetry.record(MindTelemetryEvent.ResolverFailed(reference.kind.id, "no_resolver"))
                recordAudit(action, AuditOutcome.RESOLUTION_FAILED, "resolver_missing")
                return ExecutionResult.Failed(
                    ExecutionFailure.ResolutionFailed(reference.kind, reference.rawValue, "no resolver for '${reference.kind.id}'"),
                )
            }
            when (val outcome = resolver.resolve(reference.rawValue)) {
                is ReferenceResolutionResult.Resolved -> {
                    resolved[reference.kind] = outcome.canonicalId
                    telemetry.record(MindTelemetryEvent.ResolverSuccess(reference.kind.id))
                }
                is ReferenceResolutionResult.NotFound -> {
                    telemetry.record(MindTelemetryEvent.ResolverFailed(reference.kind.id, "not_found"))
                    recordAudit(action, AuditOutcome.RESOLUTION_FAILED, "not_found")
                    return ExecutionResult.Failed(
                        ExecutionFailure.ResolutionFailed(reference.kind, reference.rawValue, "'${reference.rawValue}' not found"),
                    )
                }
                is ReferenceResolutionResult.Failed -> {
                    telemetry.record(MindTelemetryEvent.ResolverFailed(reference.kind.id, "error"))
                    recordAudit(action, AuditOutcome.RESOLUTION_FAILED, "resolver_error")
                    return ExecutionResult.Failed(
                        ExecutionFailure.ResolutionFailed(reference.kind, reference.rawValue, outcome.message),
                    )
                }
            }
        }

        // 3) Build the typed navigation command from resolved ids.
        val command = buildCommand(action, resolved)

        // 4) Hand to the coordinator, which owns the navigation side-effect.
        return when (val outcome = coordinator.navigate(command)) {
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
    }

    private fun buildCommand(action: MindAction, resolved: Map<ReferenceKind, String>): MindNavigationCommand =
        when (action) {
            is MindAction.OpenProfile -> MindNavigationCommand.OpenProfile(resolved.getValue(ReferenceKind.USERNAME_TO_USER_ID))
            is MindAction.OpenStory -> MindNavigationCommand.OpenStory(resolved.getValue(ReferenceKind.STORY_SLUG_TO_STORY_ID))
            is MindAction.OpenChat -> MindNavigationCommand.OpenChat(resolved.getValue(ReferenceKind.CHAT_ALIAS_TO_CHAT_ID))
            is MindAction.OpenMessages -> MindNavigationCommand.OpenMessages
            is MindAction.OpenSettings -> MindNavigationCommand.OpenSettings
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
        }
}
