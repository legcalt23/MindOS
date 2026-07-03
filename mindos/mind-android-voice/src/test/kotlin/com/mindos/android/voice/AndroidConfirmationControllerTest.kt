package com.mindos.android.voice

import com.mindos.command.runtime.CommandConfirmationRequest
import com.mindos.command.runtime.CommandConfirmationCancellationResult
import com.mindos.command.runtime.CommandDispatchResult
import com.mindos.command.runtime.ConfirmationToken
import com.mindos.command.runtime.TextCommandGateway
import com.mindos.command.runtime.TextCommandRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidConfirmationControllerTest {

    @Test
    fun acceptedWithinTtlUsesBoundRequestSessionAndToken() = runTest {
        val clock = MutableUiClock(100L)
        val gateway = RecordingConfirmationGateway()
        val controller = controller(gateway, clock)
        val binding = binding(expiresAtMillis = 200L)

        controller.present(binding)
        controller.confirmByText(currentActionType = binding.actionType)
        advanceUntilIdle()

        assertEquals(
            CommandConfirmationRequest(binding.token, binding.requestId, binding.sessionId),
            gateway.requests.single(),
        )
        assertIs<ConfirmationUiState.Completed>(controller.state.value)
    }

    @Test
    fun expiredConfirmationRevokesCanonicalBindingWithoutExecuting() = runTest {
        val clock = MutableUiClock(200L)
        val gateway = RecordingConfirmationGateway()
        val controller = controller(gateway, clock)

        controller.present(binding(expiresAtMillis = 200L))
        controller.confirmByText(currentActionType = "open_settings")
        advanceUntilIdle()

        assertIs<ConfirmationUiState.ConfirmationExpired>(controller.state.value)
        assertEquals(1, gateway.cancellations.size)
        assertTrue(gateway.requests.isEmpty())
    }

    @Test
    fun cancellationRejectsPendingWithoutCallingGateway() = runTest {
        val gateway = RecordingConfirmationGateway()
        val controller = controller(gateway, MutableUiClock(100L))

        controller.present(binding())
        controller.cancelPending()
        advanceUntilIdle()

        assertIs<ConfirmationUiState.ConfirmationRejected>(controller.state.value)
        assertEquals(1, gateway.cancellations.size)
        assertTrue(gateway.requests.isEmpty())
    }

    @Test
    fun cancellationAfterTtlReportsExpiredAndRevokesCanonicalBinding() = runTest {
        val clock = MutableUiClock(100L)
        val gateway = RecordingConfirmationGateway()
        val controller = controller(gateway, clock)

        controller.present(binding(expiresAtMillis = 200L))
        clock.currentMillis = 200L
        controller.cancelPending()
        advanceUntilIdle()

        assertIs<ConfirmationUiState.ConfirmationExpired>(controller.state.value)
        assertEquals(1, gateway.cancellations.size)
    }

    @Test
    fun replayAfterCompletionDoesNotCallGatewayTwice() = runTest {
        val gateway = RecordingConfirmationGateway()
        val controller = controller(gateway, MutableUiClock(100L))
        val binding = binding()

        controller.present(binding)
        controller.confirmByText(binding.actionType)
        advanceUntilIdle()
        controller.confirmByText(binding.actionType)
        advanceUntilIdle()

        assertEquals(1, gateway.requests.size)
        assertIs<ConfirmationUiState.ConfirmationRejected>(controller.state.value)
    }

    @Test
    fun changedActionIsRejectedAndRevokedBeforeExecution() = runTest {
        val gateway = RecordingConfirmationGateway()
        val controller = controller(gateway, MutableUiClock(100L))

        controller.present(binding(actionType = "open_settings"))
        controller.confirmByText(currentActionType = "open_messages")
        advanceUntilIdle()

        assertIs<ConfirmationUiState.ConfirmationRejected>(controller.state.value)
        assertEquals(1, gateway.cancellations.size)
        assertTrue(gateway.requests.isEmpty())
    }

    @Test
    fun voiceYesRequiresFinalHighConfidenceEvidence() = runTest {
        val gateway = RecordingConfirmationGateway()
        val controller = controller(gateway, MutableUiClock(100L))
        val binding = binding()

        controller.present(binding)
        controller.confirmByVoice(
            currentActionType = binding.actionType,
            evidence = VoiceConfirmationEvidence("si", confidence = 0.89f, isFinal = true),
        )
        advanceUntilIdle()

        assertIs<ConfirmationUiState.ConfirmationRejected>(controller.state.value)
        assertEquals(1, gateway.cancellations.size)
        assertTrue(gateway.requests.isEmpty())
    }

    private fun TestScope.controller(
        gateway: TextCommandGateway,
        clock: MutableUiClock,
    ) = AndroidConfirmationController(gateway, this, clock)
}

private class MutableUiClock(var currentMillis: Long) : ConfirmationUiClock {
    override fun nowMillis(): Long = currentMillis
}

private class RecordingConfirmationGateway : TextCommandGateway {
    val requests = mutableListOf<CommandConfirmationRequest>()
    val cancellations = mutableListOf<CommandConfirmationRequest>()

    override suspend fun routeAndExecute(request: TextCommandRequest): CommandDispatchResult =
        error("not used")

    override suspend fun confirmAndExecute(request: CommandConfirmationRequest): CommandDispatchResult {
        requests += request
        return CommandDispatchResult.DryRun(
            requestId = request.requestId,
            actionType = "open_settings",
            summary = "confirmed",
        )
    }

    override suspend fun cancelConfirmation(
        request: CommandConfirmationRequest,
    ): CommandConfirmationCancellationResult {
        cancellations += request
        return CommandConfirmationCancellationResult.Cancelled
    }
}

private fun binding(
    actionType: String = "open_settings",
    expiresAtMillis: Long = 1_000L,
) = ConfirmationBinding(
    token = ConfirmationToken("token"),
    requestId = "request",
    sessionId = "session",
    actionType = actionType,
    expiresAtMillis = expiresAtMillis,
)
