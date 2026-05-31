package com.mindos.action.matching

import com.mindos.action.entity.DeterministicConfidence
import com.mindos.action.entity.EntityExtractionResult
import com.mindos.action.entity.ExtractedEntity
import com.mindos.action.normalization.NormalizationPipeline
import com.mindos.action.normalization.NormalizedInput
import com.mindos.action.observability.CandidateTrace
import com.mindos.action.observability.AuditEvent
import com.mindos.action.observability.AuditOutcome
import com.mindos.action.observability.AuditSink
import com.mindos.action.observability.EntityTrace
import com.mindos.action.observability.MatchStage
import com.mindos.action.observability.MatchTraceBuilder
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
import com.mindos.action.pattern.PatternId
import com.mindos.action.understanding.ActionMatcher
import com.mindos.action.understanding.ActionUnderstandingResult
import com.mindos.action.understanding.MindIntent
import com.mindos.action.understanding.MindIntentCandidate
import com.mindos.action.understanding.MindIntentId
import com.mindos.action.understanding.RejectionReason
import com.mindos.action.understanding.SlotName
import com.mindos.action.understanding.UnderstandingContext
import com.mindos.security.AbuseDecision
import com.mindos.security.AbuseGuard
import com.mindos.security.InputSecurityCode
import com.mindos.security.InputSecurityInspector
import com.mindos.security.InputSecurityPolicy
import com.mindos.security.NoOpAbuseGuard
import com.mindos.security.SecurityClock
import com.mindos.security.SecurityClocks
import kotlin.time.TimeSource

/** Tunable runtime behavior. */
data class MatchPolicy(
    /** When true, a semantic tie across different intents yields [ActionUnderstandingResult.Ambiguous]
     *  instead of breaking the tie by declaration order. (The compiler prevents such ties by default.) */
    val surfaceAmbiguity: Boolean = true,
)

/**
 * The production [ActionMatcher]: Exact (O(1)) → Alias (O(1)) → Parametric trie (O(k)) +
 * typed entity validation → deterministic ranking. No fuzzy/typo/NLP. Pure and deterministic.
 *
 * Hot path: single normalization (no per-request regex), index lookups, bounded trie walk.
 * Telemetry and trace are off the decision path; trace is built only in explain mode.
 */
class DefaultActionMatcher(
    private val registry: CompiledRegistry,
    private val telemetry: MindTelemetrySink = NoOpMindTelemetrySink,
    private val policy: MatchPolicy = MatchPolicy(),
    private val securityPolicy: InputSecurityPolicy = InputSecurityPolicy.Default,
    private val abuseGuard: AbuseGuard = NoOpAbuseGuard,
    private val audit: AuditSink = NoOpAuditSink,
    private val clock: SecurityClock = SecurityClocks.Zero,
    private val metrics: MetricSink = NoOpMetricSink,
    private val logs: StructuredLogSink = NoOpStructuredLogSink,
    private val metricTags: MetricTags = MetricTags.Default,
) : ActionMatcher {

    private val pipeline = NormalizationPipeline(registry.normalizationConfig)

    override fun understand(input: String, context: UnderstandingContext): ActionUnderstandingResult {
        val mark = TimeSource.Monotonic.markNow()
        val result = understandInternal(input, context)
        val latencyMicros = mark.elapsedNow().inWholeMicroseconds
        telemetry.record(MindTelemetryEvent.MatchLatency(latencyMicros))
        recordOperationalObservation(result, context, latencyMicros)
        return result
    }

    private fun understandInternal(input: String, context: UnderstandingContext): ActionUnderstandingResult {
        when (val decision = abuseGuard.beforeCommand(context.abuseSubject)) {
            AbuseDecision.Allow -> Unit
            is AbuseDecision.Deny -> {
                telemetry.record(
                    MindTelemetryEvent.AbuseGuardDenied(
                        action = decision.action.name,
                        scope = decision.scope.name,
                        policyName = decision.policyName,
                    ),
                )
                recordAudit(context, AuditOutcome.RATE_LIMITED, "abuse_${decision.action.name.lowercase()}")
                return ActionUnderstandingResult.Rejected(
                    RejectionReason.UnsupportedInput("abuse guard denied request: ${decision.action.name.lowercase()}"),
                )
            }
        }

        val inputSecurity = InputSecurityInspector.inspect(
            input,
            securityPolicy.copy(maxInputLength = context.maxInputLength),
        )
        if (inputSecurity.findings.isNotEmpty()) {
            telemetry.record(MindTelemetryEvent.SuspiciousInput(inputSecurity.findings.map { it.code.name }))
        }
        if (inputSecurity.hasRejections) {
            abuseGuard.recordFailure(context.abuseSubject, "input_security")
            val reason = inputSecurity.rejectionCodes.joinToString("|") { it.name.lowercase() }
            telemetry.record(MindTelemetryEvent.InputRejected(reason))
            recordAudit(context, AuditOutcome.INPUT_REJECTED, reason)
            return ActionUnderstandingResult.Rejected(
                if (InputSecurityCode.INPUT_TOO_LONG in inputSecurity.rejectionCodes) {
                    RejectionReason.InputTooLong("input length ${input.length} exceeds ${context.maxInputLength}")
                } else {
                    RejectionReason.UnsupportedInput("input rejected by security policy: $reason")
                },
            )
        }

        val norm = pipeline.normalize(input)
        val trace = if (context.explain) MatchTraceBuilder(input, norm.text, context.localeTag) else null
        if (norm.isBlank) {
            abuseGuard.recordFailure(context.abuseSubject, "empty_input")
            return ActionUnderstandingResult.Rejected(
                RejectionReason.EmptyInput(),
                trace?.stage(MatchStage.NORMALIZATION)?.build("rejected_empty"),
            )
        }

        val candidates = ArrayList<EngineCandidate>()

        registry.exactIndex.lookup(norm.text)?.let { e ->
            candidates += EngineCandidate(
                Source.EXACT, e.intentId, e.patternId, emptyMap(), DeterministicConfidence.EXACT,
                MatchScore(e.priority.value, MatchScore.EXACTNESS_EXACT, norm.tokens.size, 0, 0, 0, e.declarationOrder),
            )
        }

        registry.aliasIndex.lookup(norm.text, context.localeTag)?.let { a ->
            val localeMatch = if (a.localeTag == context.localeTag) 1 else 0
            candidates += EngineCandidate(
                Source.ALIAS, a.intentId, null, emptyMap(), DeterministicConfidence.EXACT,
                MatchScore(0, MatchScore.EXACTNESS_ALIAS, norm.tokens.size, 0, 0, localeMatch, a.declarationOrder),
            )
        }

        for (trieMatch in registry.trie.walk(norm.tokens)) {
            buildParametric(trieMatch, norm, trace)?.let { candidates += it }
        }

        if (candidates.isEmpty()) {
            telemetry.record(MindTelemetryEvent.NoMatch)
            abuseGuard.recordFailure(context.abuseSubject, "no_match")
            return ActionUnderstandingResult.NoMatch(trace?.stage(MatchStage.DONE)?.build("no_match"))
        }

        val ranked = candidates.sortedWith(compareByDescending { it.score })
        val best = ranked.first()
        trace?.stage(MatchStage.RANKING)
        ranked.forEach { c ->
            trace?.candidate(CandidateTrace(c.patternId?.value ?: "(alias)", c.intentId.value, c.score.summary(), c === best))
        }

        val ambiguousPeers = ranked.filter { it !== best && it.score.semanticTie(best.score) && it.intentId != best.intentId }
        if (policy.surfaceAmbiguity && ambiguousPeers.isNotEmpty()) {
            val all = listOf(best) + ambiguousPeers
            telemetry.record(MindTelemetryEvent.Ambiguous(all.map { it.intentId.value }))
            abuseGuard.recordFailure(context.abuseSubject, "ambiguous")
            val out = all.map { MindIntentCandidate(MindIntent(it.intentId), it.entities, it.score.summary()) }
            return ActionUnderstandingResult.Ambiguous(out, trace?.winner(null)?.build("ambiguous"))
        }

        when (best.source) {
            Source.EXACT -> telemetry.record(MindTelemetryEvent.ExactHit(best.intentId.value))
            Source.ALIAS -> telemetry.record(MindTelemetryEvent.AliasHit(best.intentId.value, context.localeTag))
            Source.PARAMETRIC -> telemetry.record(MindTelemetryEvent.ParametricHit(best.intentId.value))
        }
        abuseGuard.recordSuccess(context.abuseSubject)
        return ActionUnderstandingResult.Understood(
            MindIntent(best.intentId),
            best.entities,
            best.confidence,
            trace?.winner(best.intentId.value)?.build("understood"),
        )
    }

    private fun recordAudit(context: UnderstandingContext, outcome: AuditOutcome, errorCode: String?) {
        audit.record(
            AuditEvent(
                requestId = context.requestId ?: "unassigned",
                actionType = "understanding",
                timestamp = clock.nowMillis(),
                outcome = outcome,
                errorCode = errorCode,
            ),
        )
    }

    private fun recordOperationalObservation(
        result: ActionUnderstandingResult,
        context: UnderstandingContext,
        latencyMicros: Long,
    ) {
        val commandType = when (result) {
            is ActionUnderstandingResult.Understood -> result.intent.id.value
            is ActionUnderstandingResult.Ambiguous -> "ambiguous"
            is ActionUnderstandingResult.Rejected -> "rejected"
            is ActionUnderstandingResult.NoMatch -> MetricTags.UnknownCommandType
        }
        val timestamp = clock.nowMillis()
        val tags = metricTags.withCommandType(commandType)
        val outcome = when (result) {
            is ActionUnderstandingResult.Understood -> OutcomeSuccess
            is ActionUnderstandingResult.Rejected -> OutcomeRejected
            is ActionUnderstandingResult.Ambiguous,
            is ActionUnderstandingResult.NoMatch,
            -> OutcomeFailure
        }

        metrics.record(MetricSample(OperationMetric.LATENCY_SAMPLE, latencyMicros / 1_000.0, tags, timestamp))
        metrics.record(MetricSample(OperationMetric.MATCH_RATE, 1.0, tags.withOutcome(outcome), timestamp))
        metrics.record(MetricSample(OperationMetric.INTENT_RESOLUTION_RATE, 1.0, tags.withOutcome(outcome), timestamp))
        when (result) {
            is ActionUnderstandingResult.Understood -> {
                if (result.entities.isNotEmpty()) {
                    metrics.record(MetricSample(OperationMetric.ENTITY_RESOLUTION_RATE, 1.0, tags.withOutcome(OutcomeSuccess), timestamp))
                }
            }
            is ActionUnderstandingResult.NoMatch -> {
                metrics.record(MetricSample(OperationMetric.UNKNOWN_COMMAND_RATE, 1.0, tags.withOutcome(OutcomeFailure), timestamp))
            }
            is ActionUnderstandingResult.Rejected -> {
                metrics.record(MetricSample(OperationMetric.VALIDATION_FAILURE_RATE, 1.0, tags.withOutcome(OutcomeRejected), timestamp))
            }
            is ActionUnderstandingResult.Ambiguous -> Unit
        }

        logs.record(
            StructuredLogEvent(
                requestId = context.requestId ?: StructuredLogEvent.UnassignedRequestId,
                sessionId = context.abuseSubject.sessionId ?: StructuredLogEvent.AnonymousSessionId,
                timestampMillis = timestamp,
                action = "understanding",
                outcome = when (result) {
                    is ActionUnderstandingResult.Understood -> "understood"
                    is ActionUnderstandingResult.Ambiguous -> "ambiguous"
                    is ActionUnderstandingResult.Rejected -> "rejected"
                    is ActionUnderstandingResult.NoMatch -> "no_match"
                },
                latencyMs = (latencyMicros + 999L) / 1_000L,
                errorCode = when (result) {
                    is ActionUnderstandingResult.Rejected -> result.reason.telemetryCode()
                    else -> null
                },
                attributes = mapOf("commandType" to commandType),
            ),
        )
    }

    private fun RejectionReason.telemetryCode(): String =
        when (this) {
            is RejectionReason.EmptyInput -> "empty_input"
            is RejectionReason.InputTooLong -> "input_too_long"
            is RejectionReason.InvalidEntity -> "invalid_entity"
            is RejectionReason.UnsupportedInput -> "unsupported_input"
        }

    private fun buildParametric(
        trieMatch: TrieMatch,
        norm: NormalizedInput,
        trace: MatchTraceBuilder?,
    ): EngineCandidate? {
        val pattern = trieMatch.entry.pattern
        val entities = LinkedHashMap<SlotName, ExtractedEntity>()
        var worst = DeterministicConfidence.EXACT
        pattern.slots.forEachIndexed { i, slot ->
            val span = trieMatch.slotSpans[i]
            val extractor = registry.entityRegistry.forType(slot.acceptedType) ?: return null
            when (val res = extractor.extract(norm, span.first, span.last + 1)) {
                is EntityExtractionResult.Rejected -> {
                    telemetry.record(
                        MindTelemetryEvent.EntityRejected(slot.name.value, slot.acceptedType.id, res.reason.detail),
                    )
                    metrics.record(
                        MetricSample(
                            OperationMetric.ENTITY_RESOLUTION_RATE,
                            1.0,
                            metricTags.withCommandType(pattern.intentId.value).withOutcome(OutcomeFailure),
                            clock.nowMillis(),
                        ),
                    )
                    trace?.rejected(
                        EntityTrace(slot.name.value, slot.acceptedType.id, norm.tokens.getOrNull(span.first)?.raw ?: "", res.reason.detail),
                    )
                    return null
                }
                is EntityExtractionResult.Extracted -> {
                    entities[slot.name] = res.entity
                    trace?.extracted(EntityTrace(slot.name.value, slot.acceptedType.id, res.entity.value))
                    if (res.entity.confidence.ordinal > worst.ordinal) worst = res.entity.confidence
                }
            }
        }
        val confidence = if (pattern.slots.isEmpty()) DeterministicConfidence.HIGH else worst
        val score = MatchScore(
            priority = pattern.priority.value,
            exactness = MatchScore.EXACTNESS_PARAMETRIC,
            staticTokenMatches = pattern.staticTokenCount,
            typedSlotMatches = pattern.typedSlotCount,
            freeTextPenalty = pattern.freeTextSlotCount,
            localeMatch = 0,
            declarationOrder = trieMatch.entry.declarationOrder,
        )
        return EngineCandidate(Source.PARAMETRIC, pattern.intentId, pattern.id, entities, confidence, score)
    }

    private enum class Source { EXACT, ALIAS, PARAMETRIC }

    private data class EngineCandidate(
        val source: Source,
        val intentId: MindIntentId,
        val patternId: PatternId?,
        val entities: Map<SlotName, ExtractedEntity>,
        val confidence: DeterministicConfidence,
        val score: MatchScore,
    )
}
