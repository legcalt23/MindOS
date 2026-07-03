package com.mindos.android.voice

import com.mindos.command.runtime.CommandDispatchResult
import com.mindos.command.runtime.ConfirmationToken
import com.mindos.command.runtime.ResponseText
import com.mindos.speech.VoiceCommandOrchestrator
import com.mindos.speech.VoiceCommandOutcome
import com.mindos.speech.VoicePipelineTimings
import com.mindos.speech.VoiceRejectionReason
import com.mindos.speech.VoiceSession
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PushToTalkViewModelTest {

    @Test
    fun disabledFlagNeverInvokesVoice() = runTest {
        val voice = RecordingVoice { completed() }
        val viewModel = viewModel(enabled = false, voice = voice)

        viewModel.onMicrophonePressed(permissionGranted = true)
        advanceUntilIdle()

        assertIs<PushToTalkUiState.DisabledByFlag>(viewModel.state.value)
        assertEquals(0, voice.invocationCount)
    }

    @Test
    fun permissionDeniedNeverInvokesVoice() = runTest {
        val voice = RecordingVoice { completed() }
        val viewModel = viewModel(voice = voice)

        viewModel.onMicrophonePressed(permissionGranted = false)
        viewModel.onPermissionResult(granted = false)

        assertTrue(assertIs<PushToTalkUiState.PermissionRequired>(viewModel.state.value).denied)
        assertEquals(0, voice.invocationCount)
    }

    @Test
    fun cancellationReturnsToIdleAndCancelsBeforeOutcome() = runTest {
        var cancelled = false
        val voice = RecordingVoice {
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        val viewModel = viewModel(voice = voice)

        viewModel.onMicrophonePressed(permissionGranted = true)
        runCurrent()
        viewModel.cancelBeforeExecution()
        advanceUntilIdle()

        assertIs<PushToTalkUiState.Idle>(viewModel.state.value)
        assertTrue(cancelled)
    }

    @Test
    fun lowConfidenceIsRejectedWithText() = runTest {
        val viewModel = viewModel(
            voice = RecordingVoice {
                VoiceCommandOutcome.Rejected(VoiceRejectionReason.CONFIDENCE_TOO_LOW, timings())
            },
        )

        viewModel.onMicrophonePressed(permissionGranted = true)
        advanceUntilIdle()

        assertTrue(assertIs<PushToTalkUiState.Rejected>(viewModel.state.value).text.isNotBlank())
    }

    @Test
    fun needsConfirmationNeverAutoConfirms() = runTest {
        val pending = CommandDispatchResult.NeedsConfirmation(
            requestId = "req",
            actionType = "open_settings",
            prompt = "Confirm.",
            token = ConfirmationToken("token"),
            expiresAtMillis = 1_000L,
        )
        val viewModel = viewModel(
            voice = RecordingVoice {
                VoiceCommandOutcome.Completed(pending, ResponseText("Confirma."), spoken = true, timings())
            },
        )

        viewModel.onMicrophonePressed(permissionGranted = true)
        advanceUntilIdle()

        assertEquals(pending, assertIs<PushToTalkUiState.NeedsConfirmation>(viewModel.state.value).pending)
    }

    @Test
    fun ttsDegradedKeepsTextualResponse() = runTest {
        val viewModel = viewModel(
            voice = RecordingVoice {
                VoiceCommandOutcome.Degraded(
                    dispatch = executed(),
                    responseText = ResponseText("Accion completada."),
                    reason = VoiceRejectionReason.TTS_TIMEOUT,
                    timings = timings(),
                )
            },
        )

        viewModel.onMicrophonePressed(permissionGranted = true)
        advanceUntilIdle()

        assertEquals(
            "Accion completada.",
            assertIs<PushToTalkUiState.Degraded>(viewModel.state.value).text,
        )
    }

    private fun TestScope.viewModel(
        enabled: Boolean = true,
        voice: VoiceCommandOrchestrator,
    ) = AndroidPushToTalkViewModel(
        enabled = enabled,
        orchestrator = voice,
        sessionFactory = VoiceSessionFactory { VoiceSession("req", "session") },
        scope = this,
    )
}

private class RecordingVoice(
    private val outcome: suspend () -> VoiceCommandOutcome,
) : VoiceCommandOrchestrator {
    var invocationCount = 0
        private set

    override suspend fun runOnce(session: VoiceSession): VoiceCommandOutcome {
        invocationCount += 1
        return outcome()
    }
}

private fun completed() = VoiceCommandOutcome.Completed(
    dispatch = executed(),
    responseText = ResponseText("Accion completada."),
    spoken = true,
    timings = timings(),
)

private fun executed() = CommandDispatchResult.DryRun(
    requestId = "req",
    actionType = "open_settings",
    summary = "dry run",
)

private fun timings() = VoicePipelineTimings(e2eMs = 0L)
