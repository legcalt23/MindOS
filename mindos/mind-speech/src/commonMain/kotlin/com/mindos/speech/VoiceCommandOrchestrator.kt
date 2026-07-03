package com.mindos.speech

import com.mindos.command.runtime.CommandDispatchResult
import com.mindos.command.runtime.CommandRejectionReason
import com.mindos.command.runtime.CommandSource
import com.mindos.command.runtime.ResponseText
import com.mindos.command.runtime.ResponseTextResolver
import com.mindos.command.runtime.TextCommandGateway
import com.mindos.command.runtime.TextCommandRequest
import com.mindos.contracts.AmbiguousFailureDto
import com.mindos.contracts.RouterAbuseContextDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

data class VoiceSession(
    val requestId: String,
    val sessionId: String? = null,
    val localeTag: String = "es-US",
    val abuseContext: RouterAbuseContextDto = RouterAbuseContextDto(),
) {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
        require(sessionId == null || sessionId.isNotBlank()) { "sessionId must be null or non-blank." }
        require(localeTag.isNotBlank()) { "localeTag must not be blank." }
    }
}

fun interface VoiceClock {
    fun nowMillis(): Long
}

object VoiceClocks {
    val Zero = VoiceClock { 0L }
}

fun interface VoiceCommandOrchestrator {
    suspend fun runOnce(session: VoiceSession): VoiceCommandOutcome
}

class DefaultVoiceCommandOrchestrator(
    private val speechInput: SpeechInputProvider,
    private val asr: ASRBackend,
    private val transcriptPolicy: TranscriptPolicy,
    private val gateway: TextCommandGateway,
    private val responseTextResolver: ResponseTextResolver,
    private val tts: TTSBackend,
    private val audioOutput: AudioOutputProvider,
    private val config: VoiceRuntimeConfig = VoiceRuntimeConfig(),
    private val clock: VoiceClock = VoiceClocks.Zero,
    private val telemetry: SpeechTelemetrySink = NoOpSpeechTelemetrySink,
) : VoiceCommandOrchestrator {

    override suspend fun runOnce(session: VoiceSession): VoiceCommandOutcome {
        val telemetryContext = PipelineTelemetryContext()
        val outcome = runPipeline(session, telemetryContext)
        emitTelemetry(session, telemetryContext, outcome)
        return outcome
    }

    private suspend fun runPipeline(
        session: VoiceSession,
        telemetryContext: PipelineTelemetryContext,
    ): VoiceCommandOutcome {
        val timings = TimingBuilder(clock)
        if (!config.canRun) {
            return VoiceCommandOutcome.Rejected(VoiceRejectionReason.VOICE_DISABLED, timings.finish())
        }

        val capture = when (
            val call = timings.measure(VoiceStage.CAPTURE) {
                withTimeoutOrNull(config.captureTimeoutMs) { speechInput.capture(session) }
                    ?: SpeechInputResult.Failure(CaptureFailureReason.TIMEOUT)
            }
        ) {
            is TimedCall.Failed ->
                return VoiceCommandOutcome.Failed(
                    VoiceStage.CAPTURE,
                    VoiceRejectionReason.CAPTURE_BACKEND_ERROR,
                    timings.finish(),
                )
            is TimedCall.Succeeded -> call.value
        }
        val audio = when (capture) {
            is SpeechInputResult.Captured -> {
                telemetryContext.captureResult = SpeechTelemetryResult.SUCCESS
                capture.audio
            }
            is SpeechInputResult.Failure -> {
                telemetryContext.captureResult = capture.reason.toTelemetryResult()
                return VoiceCommandOutcome.Rejected(capture.reason.toVoiceReason(), timings.finish())
            }
        }
        if (audio.payload.isEmpty) {
            telemetryContext.captureResult = SpeechTelemetryResult.EMPTY
            return VoiceCommandOutcome.Rejected(VoiceRejectionReason.EMPTY_AUDIO, timings.finish())
        }
        if (audio.durationMs > config.audioMaxDurationMs) {
            telemetryContext.captureResult = SpeechTelemetryResult.REJECTED
            return VoiceCommandOutcome.Rejected(VoiceRejectionReason.AUDIO_TOO_LONG, timings.finish())
        }
        if (
            audio.requestId != session.requestId ||
            audio.sessionId != session.sessionId ||
            audio.languageTag != session.localeTag
        ) {
            telemetryContext.captureResult = SpeechTelemetryResult.REJECTED
            return VoiceCommandOutcome.Rejected(VoiceRejectionReason.AUDIO_CORRELATION_MISMATCH, timings.finish())
        }

        val asrResult = when (
            val call = timings.measure(VoiceStage.ASR) {
                withTimeoutOrNull(config.asrTimeoutMs) { asr.transcribe(audio) }
                    ?: AsrResult.Failure(BackendFailureReason.TIMEOUT)
            }
        ) {
            is TimedCall.Failed ->
                return VoiceCommandOutcome.Failed(
                    VoiceStage.ASR,
                    VoiceRejectionReason.ASR_BACKEND_ERROR,
                    timings.finish(),
                )
            is TimedCall.Succeeded -> call.value
        }
        val transcript = when (asrResult) {
            is AsrResult.Success -> {
                telemetryContext.asrResult = SpeechTelemetryResult.SUCCESS
                telemetryContext.backendId = asrResult.transcript.backendId.toSafeBackendId()
                telemetryContext.localeTag = asrResult.transcript.languageTag.toSafeLocaleTag()
                telemetryContext.confidenceBucket = confidenceBucket(asrResult.transcript.confidence)
                asrResult.transcript
            }
            is AsrResult.Failure -> {
                telemetryContext.asrResult = asrResult.reason.toTelemetryResult()
                return VoiceCommandOutcome.Rejected(asrResult.reason.toAsrVoiceReason(), timings.finish())
            }
        }

        val policyResult = when (
            val call = timings.measure(VoiceStage.TRANSCRIPT_POLICY) {
                transcriptPolicy.evaluate(transcript)
            }
        ) {
            is TimedCall.Failed ->
                return VoiceCommandOutcome.Failed(
                    VoiceStage.TRANSCRIPT_POLICY,
                    VoiceRejectionReason.TRANSCRIPT_POLICY_ERROR,
                    timings.finish(),
                )
            is TimedCall.Succeeded -> call.value
        }
        val accepted = when (policyResult) {
            is TranscriptPolicyResult.Accepted -> {
                telemetryContext.transcriptPolicyResult = SpeechTelemetryResult.SUCCESS
                policyResult
            }
            is TranscriptPolicyResult.Rejected -> {
                telemetryContext.transcriptPolicyResult = policyResult.reason.toTelemetryResult()
                return VoiceCommandOutcome.Rejected(policyResult.reason, timings.finish())
            }
        }

        val dispatch = when (
            val call = timings.measure(VoiceStage.ROUTING) {
                withTimeoutOrNull(config.routingTimeoutMs) {
                    gateway.routeAndExecute(
                        TextCommandRequest(
                            input = accepted.text,
                            requestId = session.requestId,
                            sessionId = session.sessionId,
                            localeTag = accepted.languageTag,
                            source = CommandSource.VOICE_FINAL_TRANSCRIPT,
                            abuseContext = session.abuseContext,
                            dryRun = config.dryRun,
                        ),
                    )
                }
            }
        ) {
            is TimedCall.Failed ->
                return VoiceCommandOutcome.Failed(
                    VoiceStage.ROUTING,
                    VoiceRejectionReason.ROUTING_FAILED,
                    timings.finish(),
                )
            is TimedCall.Succeeded -> call.value
                ?: return VoiceCommandOutcome.Failed(
                    VoiceStage.ROUTING,
                    VoiceRejectionReason.ROUTING_TIMEOUT,
                    timings.finish(),
                )
        }
        telemetryContext.dispatch = dispatch
        dispatch.toVoiceRejectionReason()?.let { reason ->
            return VoiceCommandOutcome.Rejected(reason, timings.finish())
        }

        val response = when (
            val call = timings.measure(VoiceStage.RESPONSE_TEXT) {
                responseTextResolver.resolve(dispatch, accepted.languageTag)
            }
        ) {
            is TimedCall.Failed ->
                return VoiceCommandOutcome.Failed(
                    VoiceStage.RESPONSE_TEXT,
                    VoiceRejectionReason.RESPONSE_TEXT_ERROR,
                    timings.finish(),
                )
            is TimedCall.Succeeded -> call.value
        }

        if (!config.ttsEnabled) {
            telemetryContext.ttsResult = SpeechTelemetryResult.SKIPPED
            telemetryContext.outputResult = SpeechTelemetryResult.SKIPPED
            return VoiceCommandOutcome.Completed(dispatch, response, spoken = false, timings.finish())
        }

        val ttsResult = when (
            val call = timings.measure(VoiceStage.TTS) {
                withTimeoutOrNull(config.ttsTimeoutMs) {
                    tts.synthesize(TtsRequest(response.value, accepted.languageTag))
                } ?: TtsResult.Failure(BackendFailureReason.TIMEOUT)
            }
        ) {
            is TimedCall.Failed ->
                return degraded(dispatch, response, VoiceRejectionReason.TTS_BACKEND_ERROR, timings)
            is TimedCall.Succeeded -> call.value
        }
        val synthesized = when (ttsResult) {
            is TtsResult.Success -> {
                telemetryContext.ttsResult = SpeechTelemetryResult.SUCCESS
                ttsResult.audio
            }
            is TtsResult.Failure -> {
                telemetryContext.ttsResult = ttsResult.reason.toTelemetryResult()
                return degraded(dispatch, response, ttsResult.reason.toTtsVoiceReason(), timings)
            }
        }
        if (synthesized.payload.isEmpty) {
            telemetryContext.ttsResult = SpeechTelemetryResult.EMPTY
            return degraded(dispatch, response, VoiceRejectionReason.TTS_EMPTY_AUDIO, timings)
        }

        val outputResult = when (
            val call = timings.measure(VoiceStage.AUDIO_OUTPUT) {
                withTimeoutOrNull(config.outputTimeoutMs) { audioOutput.play(synthesized) }
                    ?: AudioOutputResult.Failure(AudioOutputFailureReason.TIMEOUT)
            }
        ) {
            is TimedCall.Failed ->
                return degraded(dispatch, response, VoiceRejectionReason.AUDIO_OUTPUT_ERROR, timings)
            is TimedCall.Succeeded -> call.value
        }
        return when (outputResult) {
            AudioOutputResult.Played -> {
                telemetryContext.outputResult = SpeechTelemetryResult.SUCCESS
                VoiceCommandOutcome.Completed(dispatch, response, spoken = true, timings.finish())
            }
            is AudioOutputResult.Failure -> {
                telemetryContext.outputResult = outputResult.reason.toTelemetryResult()
                degraded(dispatch, response, outputResult.reason.toVoiceReason(), timings)
            }
        }
    }

    private fun degraded(
        dispatch: CommandDispatchResult,
        response: ResponseText,
        reason: VoiceRejectionReason,
        timings: TimingBuilder,
    ) = VoiceCommandOutcome.Degraded(dispatch, response, reason, timings.finish())

    private fun CaptureFailureReason.toVoiceReason(): VoiceRejectionReason = when (this) {
        CaptureFailureReason.PERMISSION_DENIED -> VoiceRejectionReason.CAPTURE_PERMISSION_DENIED
        CaptureFailureReason.TIMEOUT -> VoiceRejectionReason.CAPTURE_TIMEOUT
        CaptureFailureReason.EMPTY_AUDIO -> VoiceRejectionReason.EMPTY_AUDIO
        CaptureFailureReason.BACKEND_ERROR -> VoiceRejectionReason.CAPTURE_BACKEND_ERROR
    }

    private fun CaptureFailureReason.toTelemetryResult(): SpeechTelemetryResult = when (this) {
        CaptureFailureReason.PERMISSION_DENIED -> SpeechTelemetryResult.REJECTED
        CaptureFailureReason.TIMEOUT -> SpeechTelemetryResult.TIMEOUT
        CaptureFailureReason.EMPTY_AUDIO -> SpeechTelemetryResult.EMPTY
        CaptureFailureReason.BACKEND_ERROR -> SpeechTelemetryResult.BACKEND_ERROR
    }

    private fun BackendFailureReason.toAsrVoiceReason(): VoiceRejectionReason = when (this) {
        BackendFailureReason.TIMEOUT -> VoiceRejectionReason.ASR_TIMEOUT
        BackendFailureReason.BACKEND_ERROR,
        BackendFailureReason.INVALID_INPUT,
        -> VoiceRejectionReason.ASR_BACKEND_ERROR
    }

    private fun BackendFailureReason.toTtsVoiceReason(): VoiceRejectionReason = when (this) {
        BackendFailureReason.TIMEOUT -> VoiceRejectionReason.TTS_TIMEOUT
        BackendFailureReason.BACKEND_ERROR,
        BackendFailureReason.INVALID_INPUT,
        -> VoiceRejectionReason.TTS_BACKEND_ERROR
    }

    private fun BackendFailureReason.toTelemetryResult(): SpeechTelemetryResult = when (this) {
        BackendFailureReason.TIMEOUT -> SpeechTelemetryResult.TIMEOUT
        BackendFailureReason.BACKEND_ERROR,
        BackendFailureReason.INVALID_INPUT,
        -> SpeechTelemetryResult.BACKEND_ERROR
    }

    private fun AudioOutputFailureReason.toVoiceReason(): VoiceRejectionReason = when (this) {
        AudioOutputFailureReason.TIMEOUT -> VoiceRejectionReason.AUDIO_OUTPUT_TIMEOUT
        AudioOutputFailureReason.EMPTY_AUDIO -> VoiceRejectionReason.TTS_EMPTY_AUDIO
        AudioOutputFailureReason.BACKEND_ERROR -> VoiceRejectionReason.AUDIO_OUTPUT_ERROR
    }

    private fun AudioOutputFailureReason.toTelemetryResult(): SpeechTelemetryResult = when (this) {
        AudioOutputFailureReason.TIMEOUT -> SpeechTelemetryResult.TIMEOUT
        AudioOutputFailureReason.EMPTY_AUDIO -> SpeechTelemetryResult.EMPTY
        AudioOutputFailureReason.BACKEND_ERROR -> SpeechTelemetryResult.BACKEND_ERROR
    }

    private fun VoiceRejectionReason.toTelemetryResult(): SpeechTelemetryResult = when (this) {
        VoiceRejectionReason.TRANSCRIPT_NOT_FINAL -> SpeechTelemetryResult.PARTIAL
        VoiceRejectionReason.CONFIDENCE_TOO_LOW -> SpeechTelemetryResult.LOW_CONFIDENCE
        VoiceRejectionReason.TRANSCRIPT_EMPTY,
        VoiceRejectionReason.EMPTY_AUDIO,
        -> SpeechTelemetryResult.EMPTY
        VoiceRejectionReason.AMBIGUOUS_COMMAND -> SpeechTelemetryResult.AMBIGUOUS
        VoiceRejectionReason.SAFETY_DENIED -> SpeechTelemetryResult.SAFETY_DENIED
        VoiceRejectionReason.ASR_TIMEOUT,
        VoiceRejectionReason.CAPTURE_TIMEOUT,
        VoiceRejectionReason.ROUTING_TIMEOUT,
        VoiceRejectionReason.TTS_TIMEOUT,
        VoiceRejectionReason.AUDIO_OUTPUT_TIMEOUT,
        -> SpeechTelemetryResult.TIMEOUT
        VoiceRejectionReason.ASR_BACKEND_ERROR,
        VoiceRejectionReason.CAPTURE_BACKEND_ERROR,
        VoiceRejectionReason.TTS_BACKEND_ERROR,
        VoiceRejectionReason.AUDIO_OUTPUT_ERROR,
        -> SpeechTelemetryResult.BACKEND_ERROR
        else -> SpeechTelemetryResult.REJECTED
    }

    private fun CommandDispatchResult.toVoiceRejectionReason(): VoiceRejectionReason? = when (this) {
        is CommandDispatchResult.RoutingFailed ->
            if (failure.reason is AmbiguousFailureDto) {
                VoiceRejectionReason.AMBIGUOUS_COMMAND
            } else {
                VoiceRejectionReason.ROUTING_FAILED
            }
        is CommandDispatchResult.Rejected -> when (reason) {
            is CommandRejectionReason.SafetyDenied -> VoiceRejectionReason.SAFETY_DENIED
            is CommandRejectionReason.ConfirmationRejected -> VoiceRejectionReason.CONFIRMATION_REJECTED
            else -> VoiceRejectionReason.ROUTING_FAILED
        }
        is CommandDispatchResult.DryRun,
        is CommandDispatchResult.Executed,
        is CommandDispatchResult.ExecutionFailed,
        is CommandDispatchResult.NeedsConfirmation,
        -> null
    }

    private fun emitTelemetry(
        session: VoiceSession,
        context: PipelineTelemetryContext,
        outcome: VoiceCommandOutcome,
    ) {
        val timings = outcome.timings
        recordDuration(SpeechMetric.CAPTURE_DURATION_MS, timings.captureMs, session, context)
        recordDuration(SpeechMetric.ASR_LATENCY_MS, timings.asrMs, session, context)
        context.asrResult?.let {
            record(SpeechMetric.ASR_RESULT, session, context, result = it)
        }
        context.confidenceBucket?.let {
            record(SpeechMetric.ASR_CONFIDENCE_BUCKET, session, context, confidenceBucket = it)
        }
        context.transcriptPolicyResult?.let {
            record(SpeechMetric.TRANSCRIPT_POLICY_RESULT, session, context, result = it)
        }
        context.dispatch?.let { dispatch ->
            recordDuration(SpeechMetric.ROUTER_LATENCY_MS, dispatch.timings.routingMs, session, context)
            record(SpeechMetric.ROUTER_RESULT, session, context, result = dispatch.routerTelemetryResult())
            record(
                SpeechMetric.CONFIRMATION_RESULT,
                session,
                context,
                result = dispatch.confirmationTelemetryResult(),
            )
            recordDuration(SpeechMetric.EXECUTION_LATENCY_MS, dispatch.timings.executionMs, session, context)
        }
        recordDuration(SpeechMetric.TTS_LATENCY_MS, timings.ttsMs, session, context)
        context.outputResult?.let {
            record(SpeechMetric.OUTPUT_RESULT, session, context, result = it)
        }
        recordDuration(SpeechMetric.E2E_LATENCY_MS, timings.e2eMs, session, context)
    }

    private fun recordDuration(
        metric: SpeechMetric,
        value: Long?,
        session: VoiceSession,
        context: PipelineTelemetryContext,
    ) {
        if (value != null) record(metric, session, context, valueMs = value)
    }

    private fun record(
        metric: SpeechMetric,
        session: VoiceSession,
        context: PipelineTelemetryContext,
        valueMs: Long? = null,
        result: SpeechTelemetryResult? = null,
        confidenceBucket: SpeechConfidenceBucket? = null,
    ) {
        try {
            telemetry.record(
                SpeechTelemetryEvent(
                    metric = metric,
                    requestId = session.requestId,
                    sessionId = session.sessionId,
                    valueMs = valueMs,
                    result = result,
                    confidenceBucket = confidenceBucket,
                    backendId = context.backendId,
                    localeTag = context.localeTag,
                    actionType = context.dispatch?.telemetryActionType(),
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Observability must never alter or retry a command.
        }
    }

    private fun CommandDispatchResult.routerTelemetryResult(): SpeechTelemetryResult = when (this) {
        is CommandDispatchResult.RoutingFailed ->
            if (failure.reason is AmbiguousFailureDto) SpeechTelemetryResult.AMBIGUOUS else SpeechTelemetryResult.REJECTED
        is CommandDispatchResult.Rejected ->
            if (reason is CommandRejectionReason.SafetyDenied) {
                SpeechTelemetryResult.SAFETY_DENIED
            } else {
                SpeechTelemetryResult.REJECTED
            }
        is CommandDispatchResult.DryRun -> SpeechTelemetryResult.DRY_RUN
        is CommandDispatchResult.Executed -> SpeechTelemetryResult.EXECUTED
        is CommandDispatchResult.ExecutionFailed -> SpeechTelemetryResult.EXECUTION_FAILED
        is CommandDispatchResult.NeedsConfirmation -> SpeechTelemetryResult.NEEDS_CONFIRMATION
    }

    private fun CommandDispatchResult.confirmationTelemetryResult(): SpeechTelemetryResult = when (this) {
        is CommandDispatchResult.NeedsConfirmation -> SpeechTelemetryResult.NEEDS_CONFIRMATION
        is CommandDispatchResult.Rejected ->
            if (reason is CommandRejectionReason.ConfirmationRejected) {
                SpeechTelemetryResult.CONFIRMATION_REJECTED
            } else {
                SpeechTelemetryResult.NOT_REQUIRED
            }
        else -> SpeechTelemetryResult.NOT_REQUIRED
    }

    private fun CommandDispatchResult.telemetryActionType(): SpeechTelemetryActionType? {
        val actionType = when (this) {
            is CommandDispatchResult.DryRun -> actionType
            is CommandDispatchResult.Executed -> actionType
            is CommandDispatchResult.ExecutionFailed -> actionType
            is CommandDispatchResult.NeedsConfirmation -> actionType
            is CommandDispatchResult.Rejected,
            is CommandDispatchResult.RoutingFailed,
            -> return null
        }
        return when (actionType) {
            "open_profile" -> SpeechTelemetryActionType.OPEN_PROFILE
            "open_story" -> SpeechTelemetryActionType.OPEN_STORY
            "open_story_of_user" -> SpeechTelemetryActionType.OPEN_STORY_OF_USER
            "open_chat" -> SpeechTelemetryActionType.OPEN_CHAT
            "open_messages" -> SpeechTelemetryActionType.OPEN_MESSAGES
            "open_settings" -> SpeechTelemetryActionType.OPEN_SETTINGS
            else -> null
        }
    }

    private fun confidenceBucket(confidence: Float?): SpeechConfidenceBucket = when {
        confidence == null -> SpeechConfidenceBucket.UNKNOWN
        confidence < config.confidenceThreshold -> SpeechConfidenceBucket.BELOW_THRESHOLD
        confidence < 0.90f -> SpeechConfidenceBucket.MEDIUM
        else -> SpeechConfidenceBucket.HIGH
    }

    private fun String.toSafeBackendId(): String? =
        takeIf { length in 1..64 && all { character -> character.isLetterOrDigit() || character in "._-" } }

    private fun String.toSafeLocaleTag(): String =
        takeIf { length in 2..35 && all { character -> character.isLetterOrDigit() || character == '-' } }
            ?: "und"
}

private class PipelineTelemetryContext {
    var captureResult: SpeechTelemetryResult? = null
    var asrResult: SpeechTelemetryResult? = null
    var confidenceBucket: SpeechConfidenceBucket? = null
    var transcriptPolicyResult: SpeechTelemetryResult? = null
    var backendId: String? = null
    var localeTag: String? = null
    var dispatch: CommandDispatchResult? = null
    var ttsResult: SpeechTelemetryResult? = null
    var outputResult: SpeechTelemetryResult? = null
}

private sealed interface TimedCall<out T> {
    data class Succeeded<T>(val value: T) : TimedCall<T>
    data object Failed : TimedCall<Nothing>
}

private class TimingBuilder(
    private val clock: VoiceClock,
) {
    private val startedAt = clock.nowMillis()
    private var captureMs: Long? = null
    private var asrMs: Long? = null
    private var transcriptPolicyMs: Long? = null
    private var gatewayMs: Long? = null
    private var responseTextMs: Long? = null
    private var ttsMs: Long? = null
    private var audioOutputMs: Long? = null

    suspend fun <T> measure(stage: VoiceStage, block: suspend () -> T): TimedCall<T> {
        val start = clock.nowMillis()
        val call = try {
            TimedCall.Succeeded(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            TimedCall.Failed
        }
        val duration = elapsed(start)
        when (stage) {
            VoiceStage.CAPTURE -> captureMs = duration
            VoiceStage.ASR -> asrMs = duration
            VoiceStage.TRANSCRIPT_POLICY -> transcriptPolicyMs = duration
            VoiceStage.ROUTING,
            VoiceStage.SAFETY,
            VoiceStage.EXECUTION,
            -> gatewayMs = duration
            VoiceStage.RESPONSE_TEXT -> responseTextMs = duration
            VoiceStage.TTS -> ttsMs = duration
            VoiceStage.AUDIO_OUTPUT -> audioOutputMs = duration
        }
        return call
    }

    fun finish() = VoicePipelineTimings(
        captureMs = captureMs,
        asrMs = asrMs,
        transcriptPolicyMs = transcriptPolicyMs,
        gatewayMs = gatewayMs,
        responseTextMs = responseTextMs,
        ttsMs = ttsMs,
        audioOutputMs = audioOutputMs,
        e2eMs = elapsed(startedAt),
    )

    private fun elapsed(start: Long): Long = (clock.nowMillis() - start).coerceAtLeast(0L)
}
