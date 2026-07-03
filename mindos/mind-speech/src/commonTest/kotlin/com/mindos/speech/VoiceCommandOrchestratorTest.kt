package com.mindos.speech

import com.mindos.action.execution.ActionExecutor
import com.mindos.action.execution.ExecutionContext
import com.mindos.action.execution.ExecutionResult
import com.mindos.action.model.MindAction
import com.mindos.action.navigation.MindNavigationCommand
import com.mindos.command.runtime.CommandRuntimeConfig
import com.mindos.command.runtime.CommandDispatchResult
import com.mindos.command.runtime.DefaultCommandSafetyPolicy
import com.mindos.command.runtime.DefaultResponseTextResolver
import com.mindos.command.runtime.DefaultRouterActionExecutionAdapter
import com.mindos.command.runtime.DefaultTextCommandGateway
import com.mindos.command.runtime.InMemoryConfirmationStore
import com.mindos.command.runtime.ConfirmationClock
import com.mindos.command.runtime.ConfirmationToken
import com.mindos.command.runtime.ConfirmationTokenFactory
import com.mindos.command.runtime.TextCommandGateway
import com.mindos.command.runtime.TextCommandRequest
import com.mindos.contracts.AmbiguousFailureDto
import com.mindos.contracts.KmpCommandRouter
import com.mindos.contracts.OpenSettingsActionDto
import com.mindos.contracts.RouterFailureResultDto
import com.mindos.contracts.RouterRequestDto
import com.mindos.contracts.RouterResultDto
import com.mindos.contracts.RouterSuccessDto
import com.mindos.contracts.RouterAbuseContextDto
import com.mindos.core.CommandRouterImpl
import com.mindos.speech.mock.FakeAudioOutputBehavior
import com.mindos.speech.mock.FakeAudioOutputProvider
import com.mindos.speech.mock.FakeSpeechInputProvider
import com.mindos.speech.mock.MockASRBackend
import com.mindos.speech.mock.MockTTSBackend
import com.mindos.speech.mock.SpeechFixtures
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VoiceCommandOrchestratorTest {

    @Test
    fun fullHeadlessSuccessExecutesOnceSpeaksAndPropagatesCorrelation() = runTest {
        val router = RecordingRouter { RouterSuccessDto(it.requestId, OpenSettingsActionDto(it.requestId)) }
        val executor = RecordingExecutor()
        val output = FakeAudioOutputProvider()
        val orchestrator = orchestrator(gateway(router, executor), output = output)

        val result = assertIs<VoiceCommandOutcome.Completed>(
            orchestrator.runOnce(VoiceSession("voice-req", "voice-session")),
        )

        assertEquals(1, router.requests.size)
        assertEquals("voice-req", router.requests.single().requestId)
        assertEquals(1, executor.actions.size)
        assertEquals("voice-session", executor.contexts.single().sessionId)
        assertEquals(1, output.invocationCount)
        assertTrue(result.spoken)
        assertNotNull(result.timings.captureMs)
        assertNotNull(result.timings.e2eMs)
    }

    @Test
    fun finalTranscriptRoutesThroughCanonicalRouterWithRegionalLocaleAndAbuseContext() = runTest {
        val executor = RecordingExecutor()
        val gateway = gateway(CommandRouterImpl(), executor)
        val session = VoiceSession(
            requestId = "voice-real-router",
            sessionId = "voice-session",
            localeTag = "es-US",
            abuseContext = RouterAbuseContextDto(sessionHash = "voice-session"),
        )

        val result = assertIs<VoiceCommandOutcome.Completed>(orchestrator(gateway).runOnce(session))

        assertEquals("open_settings", assertIs<CommandDispatchResult.Executed>(result.dispatch).actionType)
        assertEquals(1, executor.actions.size)
    }

    @Test
    fun orchestratorOwnsCaptureAndRoutingTimeouts() = runTest {
        val captureDeferred = async {
            orchestrator(
                gateway = RecordingGateway(),
                input = SpeechInputProvider { awaitCancellation() },
                config = VoiceRuntimeConfig(enabled = true, captureTimeoutMs = 100L),
            ).runOnce(VoiceSession("voice-req"))
        }
        advanceTimeBy(101L)
        assertEquals(
            VoiceRejectionReason.CAPTURE_TIMEOUT,
            assertIs<VoiceCommandOutcome.Rejected>(captureDeferred.await()).reason,
        )

        val routingDeferred = async {
            orchestrator(
                gateway = object : TextCommandGateway {
                    override suspend fun routeAndExecute(request: TextCommandRequest): CommandDispatchResult =
                        awaitCancellation()

                    override suspend fun confirmAndExecute(request: com.mindos.command.runtime.CommandConfirmationRequest) =
                        error("not used")

                    override suspend fun cancelConfirmation(request: com.mindos.command.runtime.CommandConfirmationRequest) =
                        error("not used")
                },
                config = VoiceRuntimeConfig(enabled = true, routingTimeoutMs = 100L),
            ).runOnce(VoiceSession("voice-req"))
        }
        advanceTimeBy(101L)
        val routingFailure = assertIs<VoiceCommandOutcome.Failed>(routingDeferred.await())
        assertEquals(VoiceStage.ROUTING, routingFailure.stage)
        assertEquals(VoiceRejectionReason.ROUTING_TIMEOUT, routingFailure.reason)
    }

    @Test
    fun textOnlyModeCompletesWithoutCallingTtsOrOutput() = runTest {
        val tts = MockTTSBackend.backendError()
        val output = FakeAudioOutputProvider(FakeAudioOutputBehavior.BACKEND_ERROR)

        val result = orchestrator(
            gateway = gateway(CommandRouterImpl(), RecordingExecutor()),
            tts = tts,
            output = output,
            config = VoiceRuntimeConfig(enabled = true, ttsEnabled = false),
        ).runOnce(VoiceSession("voice-req", "voice-session"))

        assertTrue(!assertIs<VoiceCommandOutcome.Completed>(result).spoken)
        assertEquals(0, tts.invocationCount)
        assertEquals(0, output.invocationCount)
    }

    @Test
    fun lowConfidenceAndPartialTranscriptsNeverReachGateway() = runTest {
        listOf(
            MockASRBackend.lowConfidence(),
            MockASRBackend.partial(),
        ).forEach { asr ->
            val gateway = RecordingGateway()
            val result = orchestrator(gateway, asr = asr).runOnce(VoiceSession("voice-req"))

            assertIs<VoiceCommandOutcome.Rejected>(result)
            assertEquals(0, gateway.invocationCount)
        }
    }

    @Test
    fun ambiguousRouterNeverCallsExecutor() = runTest {
        val router = RecordingRouter {
            RouterFailureResultDto(it.requestId, AmbiguousFailureDto(listOf("open_profile", "open_chat")))
        }
        val executor = RecordingExecutor()

        val result = orchestrator(gateway(router, executor)).runOnce(VoiceSession("voice-req"))

        assertEquals(
            VoiceRejectionReason.AMBIGUOUS_COMMAND,
            assertIs<VoiceCommandOutcome.Rejected>(result).reason,
        )
        assertEquals(0, executor.actions.size)
    }

    @Test
    fun ttsTimeoutDegradesAfterExactlyOneExecution() = runTest {
        val executor = RecordingExecutor()
        val result = orchestrator(
            gateway(
                RecordingRouter { RouterSuccessDto(it.requestId, OpenSettingsActionDto(it.requestId)) },
                executor,
            ),
            tts = MockTTSBackend.timeout(),
        ).runOnce(VoiceSession("voice-req"))

        assertEquals(
            VoiceRejectionReason.TTS_TIMEOUT,
            assertIs<VoiceCommandOutcome.Degraded>(result).reason,
        )
        assertEquals(1, executor.actions.size)
    }

    @Test
    fun outputFailureDegradesWithoutReexecution() = runTest {
        val executor = RecordingExecutor()
        val result = orchestrator(
            gateway(
                RecordingRouter { RouterSuccessDto(it.requestId, OpenSettingsActionDto(it.requestId)) },
                executor,
            ),
            output = FakeAudioOutputProvider(FakeAudioOutputBehavior.BACKEND_ERROR),
        ).runOnce(VoiceSession("voice-req"))

        assertEquals(
            VoiceRejectionReason.AUDIO_OUTPUT_ERROR,
            assertIs<VoiceCommandOutcome.Degraded>(result).reason,
        )
        assertEquals(1, executor.actions.size)
    }

    @Test
    fun dryRunNeverCallsExecutorAndStillProducesResponse() = runTest {
        val executor = RecordingExecutor()
        val result = orchestrator(
            gateway(
                RecordingRouter { RouterSuccessDto(it.requestId, OpenSettingsActionDto(it.requestId)) },
                executor,
            ),
            config = VoiceRuntimeConfig(enabled = true, dryRun = true),
        ).runOnce(VoiceSession("voice-req"))

        assertIs<VoiceCommandOutcome.Completed>(result)
        assertEquals(0, executor.actions.size)
    }

    @Test
    fun disabledVoiceDoesNotCaptureOrRoute() = runTest {
        val input = FakeSpeechInputProvider.captured()
        val gateway = RecordingGateway()

        val result = orchestrator(
            gateway = gateway,
            input = input,
            config = VoiceRuntimeConfig(),
        ).runOnce(VoiceSession("voice-req"))

        assertEquals(
            VoiceRejectionReason.VOICE_DISABLED,
            assertIs<VoiceCommandOutcome.Rejected>(result).reason,
        )
        assertEquals(0, input.invocationCount)
        assertEquals(0, gateway.invocationCount)
    }

    @Test
    fun successEmitsRequiredPiiSafeTelemetry() = runTest {
        val telemetry = RecordingSpeechTelemetrySink()
        val executor = RecordingExecutor()
        val result = orchestrator(
            gateway = gateway(
                RecordingRouter { RouterSuccessDto(it.requestId, OpenSettingsActionDto(it.requestId)) },
                executor,
            ),
            telemetry = telemetry,
        ).runOnce(VoiceSession("voice-req", "voice-session"))

        assertIs<VoiceCommandOutcome.Completed>(result)
        val metrics = telemetry.events.map { it.metric }.toSet()
        assertTrue(SpeechMetric.CAPTURE_DURATION_MS in metrics)
        assertTrue(SpeechMetric.ASR_LATENCY_MS in metrics)
        assertTrue(SpeechMetric.ASR_RESULT in metrics)
        assertTrue(SpeechMetric.ASR_CONFIDENCE_BUCKET in metrics)
        assertTrue(SpeechMetric.TRANSCRIPT_POLICY_RESULT in metrics)
        assertTrue(SpeechMetric.ROUTER_LATENCY_MS in metrics)
        assertTrue(SpeechMetric.ROUTER_RESULT in metrics)
        assertTrue(SpeechMetric.CONFIRMATION_RESULT in metrics)
        assertTrue(SpeechMetric.EXECUTION_LATENCY_MS in metrics)
        assertTrue(SpeechMetric.TTS_LATENCY_MS in metrics)
        assertTrue(SpeechMetric.OUTPUT_RESULT in metrics)
        assertTrue(SpeechMetric.E2E_LATENCY_MS in metrics)
        assertTrue(telemetry.events.all { it.requestId == "voice-req" })
        assertTrue(telemetry.events.all { it.sessionId == "voice-session" })
        assertEquals(SpeechTelemetryActionType.OPEN_SETTINGS, telemetry.events.last().actionType)
    }

    @Test
    fun policyRejectionEmitsEventsWithoutRouting() = runTest {
        val telemetry = RecordingSpeechTelemetrySink()
        val gateway = RecordingGateway()

        val result = orchestrator(
            gateway = gateway,
            asr = MockASRBackend.lowConfidence(),
            telemetry = telemetry,
        ).runOnce(VoiceSession("voice-req"))

        assertEquals(
            VoiceRejectionReason.CONFIDENCE_TOO_LOW,
            assertIs<VoiceCommandOutcome.Rejected>(result).reason,
        )
        assertEquals(0, gateway.invocationCount)
        assertTrue(telemetry.events.any { it.metric == SpeechMetric.TRANSCRIPT_POLICY_RESULT })
        assertTrue(telemetry.events.none { it.metric == SpeechMetric.ROUTER_RESULT })
        assertTrue(telemetry.events.any { it.metric == SpeechMetric.E2E_LATENCY_MS })
    }

    @Test
    fun telemetryContainsNoRawTranscriptOrAudioPayload() = runTest {
        val telemetry = RecordingSpeechTelemetrySink()
        val executor = RecordingExecutor()

        orchestrator(
            gateway = gateway(
                RecordingRouter { RouterSuccessDto(it.requestId, OpenSettingsActionDto(it.requestId)) },
                executor,
            ),
            telemetry = telemetry,
        ).runOnce(VoiceSession("voice-req"))

        val serializedEvents = telemetry.events.joinToString()
        assertTrue(!serializedEvents.contains("abre ajustes"))
        assertTrue(!serializedEvents.contains("AudioPayload"))
        assertTrue(!serializedEvents.contains("[1, 2, 3, 4]"))
    }

    @Test
    fun telemetryFailureDoesNotAlterOrRetryCommand() = runTest {
        val executor = RecordingExecutor()
        val throwingTelemetry = SpeechTelemetrySink { error("telemetry unavailable") }

        val result = orchestrator(
            gateway = gateway(
                RecordingRouter { RouterSuccessDto(it.requestId, OpenSettingsActionDto(it.requestId)) },
                executor,
            ),
            telemetry = throwingTelemetry,
        ).runOnce(VoiceSession("voice-req"))

        assertIs<VoiceCommandOutcome.Completed>(result)
        assertEquals(1, executor.actions.size)
    }

    @Test
    fun telemetryCancellationPropagatesAfterExactlyOneExecution() = runTest {
        val executor = RecordingExecutor()
        val cancellation = CancellationException("cancel telemetry")
        val cancellingTelemetry = SpeechTelemetrySink { throw cancellation }
        var observedCancellation: CancellationException? = null

        try {
            orchestrator(
                gateway = gateway(
                    RecordingRouter { RouterSuccessDto(it.requestId, OpenSettingsActionDto(it.requestId)) },
                    executor,
                ),
                telemetry = cancellingTelemetry,
            ).runOnce(VoiceSession("voice-req"))
        } catch (caught: CancellationException) {
            observedCancellation = caught
        }

        assertTrue(observedCancellation === cancellation)
        assertEquals(1, executor.actions.size)
    }

    private fun orchestrator(
        gateway: TextCommandGateway,
        input: SpeechInputProvider = FakeSpeechInputProvider.captured(),
        asr: ASRBackend = MockASRBackend.success(SpeechFixtures.transcript(text = "abre ajustes")),
        tts: TTSBackend = MockTTSBackend.success(),
        output: AudioOutputProvider = FakeAudioOutputProvider(),
        config: VoiceRuntimeConfig = VoiceRuntimeConfig(enabled = true),
        telemetry: SpeechTelemetrySink = NoOpSpeechTelemetrySink,
    ) = DefaultVoiceCommandOrchestrator(
        speechInput = input,
        asr = asr,
        transcriptPolicy = DefaultTranscriptPolicy(),
        gateway = gateway,
        responseTextResolver = DefaultResponseTextResolver(),
        tts = tts,
        audioOutput = output,
        config = config,
        clock = IncrementingVoiceClock(),
        telemetry = telemetry,
    )

    private fun gateway(router: KmpCommandRouter, executor: RecordingExecutor) =
        DefaultTextCommandGateway(
            router = router,
            adapter = DefaultRouterActionExecutionAdapter(),
            executor = executor,
            safetyPolicy = DefaultCommandSafetyPolicy(),
            confirmationStore = InMemoryConfirmationStore(
                tokenFactory = ConfirmationTokenFactory { ConfirmationToken("token") },
                clock = ConfirmationClock { 0L },
            ),
            config = CommandRuntimeConfig(enabled = true),
        )
}

private class RecordingGateway : TextCommandGateway {
    var invocationCount = 0
        private set

    override suspend fun routeAndExecute(request: com.mindos.command.runtime.TextCommandRequest): com.mindos.command.runtime.CommandDispatchResult {
        invocationCount += 1
        error("No dispatch configured")
    }

    override suspend fun confirmAndExecute(request: com.mindos.command.runtime.CommandConfirmationRequest) =
        error("Voice orchestrator must never confirm automatically")

    override suspend fun cancelConfirmation(request: com.mindos.command.runtime.CommandConfirmationRequest) =
        error("Voice orchestrator must never cancel confirmations")

    init {
        invocationCount = 0
    }
}

private class RecordingRouter(
    private val result: (RouterRequestDto) -> RouterResultDto,
) : KmpCommandRouter {
    val requests = mutableListOf<RouterRequestDto>()

    override suspend fun route(request: RouterRequestDto): RouterResultDto {
        requests += request
        return result(request)
    }
}

private class RecordingExecutor : ActionExecutor {
    val actions = mutableListOf<MindAction>()
    val contexts = mutableListOf<ExecutionContext>()

    override suspend fun execute(action: MindAction, context: ExecutionContext): ExecutionResult {
        actions += action
        contexts += context
        return ExecutionResult.Executed(MindNavigationCommand.OpenSettings)
    }
}

private class IncrementingVoiceClock : VoiceClock {
    private var current = 0L
    override fun nowMillis(): Long = current++
}
