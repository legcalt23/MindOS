package com.mindos.action.observability

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Structured, PII-free telemetry emitted by the matching engine. The app decides
 * how to forward these (counters, latency histograms). The engine never depends on
 * a concrete backend (OpenTelemetry, Firebase) — see [MindTelemetrySink].
 */
@Serializable
sealed interface MindTelemetryEvent {

    @Serializable @SerialName("exact_hit")
    data class ExactHit(val intentId: String) : MindTelemetryEvent

    @Serializable @SerialName("alias_hit")
    data class AliasHit(val intentId: String, val localeTag: String) : MindTelemetryEvent

    @Serializable @SerialName("parametric_hit")
    data class ParametricHit(val intentId: String) : MindTelemetryEvent

    @Serializable @SerialName("entity_rejected")
    data class EntityRejected(val slot: String, val type: String, val detail: String) : MindTelemetryEvent

    @Serializable @SerialName("ambiguous")
    data class Ambiguous(val candidateIntentIds: List<String>) : MindTelemetryEvent

    @Serializable @SerialName("no_match")
    data object NoMatch : MindTelemetryEvent

    @Serializable @SerialName("registry_compiled")
    data class RegistryCompiled(val patternCount: Int, val aliasCount: Int) : MindTelemetryEvent

    @Serializable @SerialName("registry_compile_failed")
    data class RegistryCompileFailed(val errorCount: Int) : MindTelemetryEvent

    @Serializable @SerialName("match_latency")
    data class MatchLatency(val micros: Long) : MindTelemetryEvent

    @Serializable @SerialName("suspicious_input")
    data class SuspiciousInput(val codes: List<String>) : MindTelemetryEvent

    @Serializable @SerialName("input_rejected")
    data class InputRejected(val reason: String) : MindTelemetryEvent

    @Serializable @SerialName("abuse_guard_denied")
    data class AbuseGuardDenied(
        val action: String,
        val scope: String,
        val policyName: String,
    ) : MindTelemetryEvent

    // ── Execution layer (ZIP03) ────────────────────────────────────────
    @Serializable @SerialName("action_executed")
    data class ActionExecuted(val actionType: String) : MindTelemetryEvent

    @Serializable @SerialName("validation_failed")
    data class ValidationFailed(val actionType: String, val reason: String) : MindTelemetryEvent

    @Serializable @SerialName("resolver_success")
    data class ResolverSuccess(val referenceKind: String) : MindTelemetryEvent

    @Serializable @SerialName("resolver_failed")
    data class ResolverFailed(val referenceKind: String, val reason: String) : MindTelemetryEvent

    @Serializable @SerialName("navigation_success")
    data class NavigationSuccess(val commandType: String) : MindTelemetryEvent

    @Serializable @SerialName("navigation_failed")
    data class NavigationFailed(val commandType: String, val reason: String) : MindTelemetryEvent

    @Serializable @SerialName("execution_latency")
    data class ExecutionLatency(val micros: Long) : MindTelemetryEvent
}
