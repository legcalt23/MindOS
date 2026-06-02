package com.mindos.core

import com.mindos.action.observability.AuditSink
import com.mindos.action.observability.MetricSink
import com.mindos.action.observability.MetricTags
import com.mindos.action.observability.MindTelemetrySink
import com.mindos.action.observability.NoOpAuditSink
import com.mindos.action.observability.NoOpMetricSink
import com.mindos.action.observability.NoOpMindTelemetrySink
import com.mindos.action.observability.NoOpStructuredLogSink
import com.mindos.action.observability.StructuredLogSink
import com.mindos.contracts.ActionContractValidator
import com.mindos.security.AbuseGuard
import com.mindos.security.CommandInputSecurityGate
import com.mindos.security.DefaultCommandInputSecurityGate
import com.mindos.security.InputSecurityPolicy
import com.mindos.security.NoOpAbuseGuard
import com.mindos.security.SecurityClock
import com.mindos.security.SecurityClocks

enum class CommandRouterEnvironment {
    TEST,
    DEVELOPMENT,
    PRODUCTION,
}

data class ProductionSafetyPolicy(
    val requireSecureWiring: Boolean = true,
    val metricsExplicitlyDisabled: Boolean = false,
    val logsExplicitlyDisabled: Boolean = false,
    val telemetryExplicitlyDisabled: Boolean = false,
)

data class CommandRouterRuntimeConfig(
    val environment: CommandRouterEnvironment,
    val core: CoreRouterConfig = CoreRouterConfig(),
    val actionRegistry: CoreActionRegistry = CoreActionRegistry.default(),
    val securityPolicy: InputSecurityPolicy = InputSecurityPolicy.Default,
    val securityGate: CommandInputSecurityGate = DefaultCommandInputSecurityGate(securityPolicy),
    val abuseGuard: AbuseGuard = NoOpAbuseGuard,
    val audit: AuditSink = NoOpAuditSink,
    val clock: SecurityClock = SecurityClocks.Zero,
    val telemetry: MindTelemetrySink = NoOpMindTelemetrySink,
    val metrics: MetricSink = NoOpMetricSink,
    val logs: StructuredLogSink = NoOpStructuredLogSink,
    val metricTags: MetricTags = MetricTags.Default,
    val validator: ActionContractValidator = ActionValidatorImpl(),
    val safetyPolicy: ProductionSafetyPolicy = ProductionSafetyPolicy(),
) {
    init {
        if (environment == CommandRouterEnvironment.PRODUCTION && safetyPolicy.requireSecureWiring) {
            val failures = productionWiringFailures()
            require(failures.isEmpty()) {
                "Unsafe CommandRouter production wiring: ${failures.joinToString()}"
            }
        }
    }

    private fun productionWiringFailures(): List<String> = buildList {
        if (abuseGuard === NoOpAbuseGuard) add("AbuseGuard is NoOpAbuseGuard")
        if (audit === NoOpAuditSink) add("AuditSink is NoOpAuditSink")
        if (clock === SecurityClocks.Zero) add("SecurityClock is zero")
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

object CommandRouterFactory {
    fun forTests(
        config: CoreRouterConfig = CoreRouterConfig(),
        actionRegistry: CoreActionRegistry = CoreActionRegistry.default(),
    ): CommandRouterImpl =
        build(
            CommandRouterRuntimeConfig(
                environment = CommandRouterEnvironment.TEST,
                core = config,
                actionRegistry = actionRegistry,
            ),
        )

    fun forDevelopment(config: CommandRouterRuntimeConfig): CommandRouterImpl =
        build(config.copy(environment = CommandRouterEnvironment.DEVELOPMENT))

    fun forProduction(config: CommandRouterRuntimeConfig): CommandRouterImpl =
        build(config.copy(environment = CommandRouterEnvironment.PRODUCTION))

    fun build(config: CommandRouterRuntimeConfig): CommandRouterImpl {
        val matcher = PatternMatcherImpl(
            registry = config.actionRegistry.routerRegistry,
            config = config.core,
            securityPolicy = config.securityPolicy,
            abuseGuard = config.abuseGuard,
            audit = config.audit,
            clock = config.clock,
            telemetry = config.telemetry,
            metrics = config.metrics,
            logs = config.logs,
            metricTags = config.metricTags,
        )
        return CommandRouterImpl(
            actionRegistry = config.actionRegistry,
            matcher = matcher,
            validator = config.validator,
            securityGate = config.securityGate,
        )
    }
}
