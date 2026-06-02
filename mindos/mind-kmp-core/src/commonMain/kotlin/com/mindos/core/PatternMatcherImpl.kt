package com.mindos.core

import com.mindos.action.matching.CompileOptions
import com.mindos.action.matching.DefaultActionMatcher
import com.mindos.action.matching.RegistryCompilationResult
import com.mindos.action.matching.RegistryCompiler
import com.mindos.action.normalization.NormalizationConfig
import com.mindos.action.observability.AuditSink
import com.mindos.action.observability.MetricSink
import com.mindos.action.observability.MetricTags
import com.mindos.action.observability.MindTelemetrySink
import com.mindos.action.observability.NoOpAuditSink
import com.mindos.action.observability.NoOpMetricSink
import com.mindos.action.observability.NoOpMindTelemetrySink
import com.mindos.action.observability.NoOpStructuredLogSink
import com.mindos.action.observability.StructuredLogSink
import com.mindos.action.understanding.ActionUnderstandingResult
import com.mindos.action.understanding.RejectionReason
import com.mindos.action.understanding.UnderstandingContext
import com.mindos.contracts.RouterRequestDto
import com.mindos.security.AbuseGuard
import com.mindos.security.InputSecurityPolicy
import com.mindos.security.NoOpAbuseGuard
import com.mindos.security.SecurityClock
import com.mindos.security.SecurityClocks

sealed interface CoreMatchResult {
    data class Matched(
        val actionType: String,
        val entities: Map<String, String>,
    ) : CoreMatchResult

    data class Ambiguous(val candidateActionTypes: List<String>) : CoreMatchResult
    data class Failed(val error: RouterError) : CoreMatchResult
}

fun interface PatternMatcher {
    fun match(request: RouterRequestDto): CoreMatchResult
}

class PatternMatcherImpl(
    registry: CoreRouterRegistry = CoreRouterRegistry.default(),
    private val config: CoreRouterConfig = CoreRouterConfig(),
    normalizationConfig: NormalizationConfig = NormalizationConfig.Default,
    telemetry: MindTelemetrySink = NoOpMindTelemetrySink,
    securityPolicy: InputSecurityPolicy = InputSecurityPolicy.Default,
    abuseGuard: AbuseGuard = NoOpAbuseGuard,
    audit: AuditSink = NoOpAuditSink,
    clock: SecurityClock = SecurityClocks.Zero,
    metrics: MetricSink = NoOpMetricSink,
    logs: StructuredLogSink = NoOpStructuredLogSink,
    metricTags: MetricTags = MetricTags.Default,
) : PatternMatcher {
    private val matcher: DefaultActionMatcher
    val warnings: List<String>

    init {
        when (val compiled = RegistryCompiler.compile(registry.toRegistrySource(), CompileOptions(normalization = normalizationConfig))) {
            is RegistryCompilationResult.Success -> {
                warnings = compiled.warnings.map { it.message }
                matcher = DefaultActionMatcher(
                    registry = compiled.registry,
                    telemetry = telemetry,
                    securityPolicy = securityPolicy,
                    abuseGuard = abuseGuard,
                    audit = audit,
                    clock = clock,
                    metrics = metrics,
                    logs = logs,
                    metricTags = metricTags,
                )
            }
            is RegistryCompilationResult.Failure -> {
                throw IllegalArgumentException(
                    compiled.errors.joinToString(separator = "\n") { "[${it.code}] ${it.message}" },
                )
            }
        }
    }

    override fun match(request: RouterRequestDto): CoreMatchResult {
        val result = matcher.understand(
            request.input,
            UnderstandingContext(
                localeTag = request.localeTag.ifBlank { config.defaultLocaleTag },
                explain = request.explain || config.explain,
                maxInputLength = config.maxInputLength,
                requestId = request.requestId,
            ),
        )
        return result.toCoreMatchResult(request)
    }

    private fun ActionUnderstandingResult.toCoreMatchResult(request: RouterRequestDto): CoreMatchResult =
        when (this) {
            is ActionUnderstandingResult.Understood -> CoreMatchResult.Matched(
                actionType = intent.id.value,
                entities = entities.mapKeys { it.key.value }.mapValues { it.value.value },
            )
            is ActionUnderstandingResult.Ambiguous -> CoreMatchResult.Ambiguous(
                candidates.map { it.intent.id.value },
            )
            is ActionUnderstandingResult.NoMatch -> CoreMatchResult.Failed(RouterError.UnknownCommand)
            is ActionUnderstandingResult.Rejected -> CoreMatchResult.Failed(reason.toRouterError(request))
        }

    private fun RejectionReason.toRouterError(request: RouterRequestDto): RouterError =
        when (this) {
            is RejectionReason.EmptyInput -> RouterError.EmptyInput
            is RejectionReason.InputTooLong -> RouterError.InputTooLong(request.input.length, config.maxInputLength)
            is RejectionReason.InvalidEntity -> RouterError.InvalidEntity(slot.value, detail)
            is RejectionReason.UnsupportedInput -> RouterError.UnsafeInput(listOf("UNSUPPORTED_INPUT"))
        }
}
