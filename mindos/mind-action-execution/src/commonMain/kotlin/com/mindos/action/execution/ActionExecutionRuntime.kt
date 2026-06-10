package com.mindos.action.execution

import com.mindos.action.navigation.NavigationCoordinator
import com.mindos.action.observability.AuditSink
import com.mindos.action.observability.MetricSink
import com.mindos.action.observability.MetricTags
import com.mindos.action.observability.MindTelemetrySink
import com.mindos.action.observability.NoOpAuditSink
import com.mindos.action.observability.NoOpMetricSink
import com.mindos.action.observability.NoOpMindTelemetrySink
import com.mindos.action.observability.NoOpStructuredLogSink
import com.mindos.action.observability.StructuredLogSink
import com.mindos.action.resolver.ResolverRegistry
import com.mindos.action.validation.ActionValidator
import com.mindos.action.validation.DefaultActionValidator

enum class ActionExecutionEnvironment {
    TEST,
    DEVELOPMENT,
    PRODUCTION,
}

data class ActionExecutionSafetyPolicy(
    val requireSecureWiring: Boolean = true,
    val metricsExplicitlyDisabled: Boolean = false,
    val logsExplicitlyDisabled: Boolean = false,
    val telemetryExplicitlyDisabled: Boolean = false,
    val allowInMemoryIdempotencyInProduction: Boolean = false,
)

data class ActionExecutionRuntimeConfig(
    val environment: ActionExecutionEnvironment,
    val validator: ActionValidator,
    val resolvers: ResolverRegistry,
    val coordinator: NavigationCoordinator,
    val telemetry: MindTelemetrySink = NoOpMindTelemetrySink,
    val audit: AuditSink = NoOpAuditSink,
    val clock: ExecutionClock = ExecutionClocks.Zero,
    val metrics: MetricSink = NoOpMetricSink,
    val logs: StructuredLogSink = NoOpStructuredLogSink,
    val metricTags: MetricTags = MetricTags.Default,
    val idempotencyStore: IdempotencyStore? = null,
    val idempotencyTtlMillis: Long = IdempotentActionExecutor.DefaultTtlMillis,
    val safetyPolicy: ActionExecutionSafetyPolicy = ActionExecutionSafetyPolicy(),
) {
    init {
        if (environment == ActionExecutionEnvironment.PRODUCTION && safetyPolicy.requireSecureWiring) {
            val failures = productionWiringFailures()
            require(failures.isEmpty()) {
                "Unsafe ActionExecutor production wiring: ${failures.joinToString()}"
            }
        }
    }

    private fun productionWiringFailures(): List<String> = buildList {
        if (resolvers.supportedKinds.isEmpty()) add("ResolverRegistry has no resolvers")
        if (validator is DefaultActionValidator && validator.authorizesAll) {
            add("DefaultActionValidator uses AllowAllAuthorizer (no real authorization policy)")
        }
        if (audit === NoOpAuditSink) add("AuditSink is NoOpAuditSink")
        if (clock === ExecutionClocks.Zero) add("ExecutionClock is zero")
        if (idempotencyStore == null) add("IdempotencyStore is missing")
        if (idempotencyStore is InMemoryIdempotencyStore && !safetyPolicy.allowInMemoryIdempotencyInProduction) {
            add("InMemoryIdempotencyStore is process-local and not allowed for production")
        }
        if (telemetry === NoOpMindTelemetrySink && !safetyPolicy.telemetryExplicitlyDisabled) {
            add("MindTelemetrySink is NoOp without explicit disable")
        }
        if (metrics === NoOpMetricSink && !safetyPolicy.metricsExplicitlyDisabled) {
            add("MetricSink is NoOp without explicit disable")
        }
        if (logs === NoOpStructuredLogSink && !safetyPolicy.logsExplicitlyDisabled) {
            add("StructuredLogSink is NoOp without explicit disable")
        }
    }
}

object ActionExecutorFactory {
    fun forTests(config: ActionExecutionRuntimeConfig): ActionExecutor =
        build(config.copy(environment = ActionExecutionEnvironment.TEST))

    fun forDevelopment(config: ActionExecutionRuntimeConfig): ActionExecutor =
        build(config.copy(environment = ActionExecutionEnvironment.DEVELOPMENT))

    fun forProduction(config: ActionExecutionRuntimeConfig): ActionExecutor =
        build(config.copy(environment = ActionExecutionEnvironment.PRODUCTION))

    fun build(config: ActionExecutionRuntimeConfig): ActionExecutor =
        DefaultActionExecutor(
            validator = config.validator,
            resolvers = config.resolvers,
            coordinator = config.coordinator,
            telemetry = config.telemetry,
            audit = config.audit,
            clock = config.clock,
            metrics = config.metrics,
            logs = config.logs,
            metricTags = config.metricTags,
            idempotencyStore = config.idempotencyStore,
            idempotencyTtlMillis = config.idempotencyTtlMillis,
        )
}
