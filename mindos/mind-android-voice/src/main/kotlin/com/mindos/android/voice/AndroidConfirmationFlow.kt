package com.mindos.android.voice

import com.mindos.command.runtime.CommandConfirmationRequest
import com.mindos.command.runtime.CommandConfirmationCancellationResult
import com.mindos.command.runtime.CommandDispatchResult
import com.mindos.command.runtime.CommandRejectionReason
import com.mindos.command.runtime.ConfirmationRejectionReason
import com.mindos.command.runtime.ConfirmationToken
import com.mindos.command.runtime.TextCommandGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfirmationBinding(
    val token: ConfirmationToken,
    val requestId: String,
    val sessionId: String,
    val actionType: String,
    val expiresAtMillis: Long,
) {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
        require(sessionId.isNotBlank()) { "sessionId must not be blank." }
        require(actionType.isNotBlank()) { "actionType must not be blank." }
        require(expiresAtMillis >= 0L) { "expiresAtMillis cannot be negative." }
    }

    companion object {
        fun from(
            pending: CommandDispatchResult.NeedsConfirmation,
            sessionId: String,
        ) = ConfirmationBinding(
            token = pending.token,
            requestId = pending.requestId,
            sessionId = sessionId,
            actionType = pending.actionType,
            expiresAtMillis = pending.expiresAtMillis,
        )
    }
}

data class VoiceConfirmationEvidence(
    val text: String,
    val confidence: Float?,
    val isFinal: Boolean,
) {
    init {
        require(confidence == null || confidence in 0.0f..1.0f) {
            "confidence must be null or between 0 and 1."
        }
    }
}

fun interface ConfirmationUiClock {
    fun nowMillis(): Long
}

sealed interface ConfirmationUiState {
    val text: String

    data object Inactive : ConfirmationUiState {
        override val text: String = ""
    }

    data class NeedsConfirmation(val binding: ConfirmationBinding) : ConfirmationUiState {
        override val text: String = "Se necesita confirmacion para continuar."
    }

    data class Confirming(val binding: ConfirmationBinding) : ConfirmationUiState {
        override val text: String = "Verificando confirmacion..."
    }

    data object ConfirmationExpired : ConfirmationUiState {
        override val text: String = "La confirmacion ha caducado."
    }

    data object ConfirmationRejected : ConfirmationUiState {
        override val text: String = "Confirmacion rechazada."
    }

    data class ConfirmedExecuting(val binding: ConfirmationBinding) : ConfirmationUiState {
        override val text: String = "Confirmado. Ejecutando..."
    }

    data class Completed(val result: CommandDispatchResult) : ConfirmationUiState {
        override val text: String = "Accion completada."
    }
}

class AndroidConfirmationController(
    private val gateway: TextCommandGateway,
    private val scope: CoroutineScope,
    private val clock: ConfirmationUiClock = ConfirmationUiClock { System.currentTimeMillis() },
    private val voiceConfidenceThreshold: Float = 0.90f,
) {
    private val mutableState = MutableStateFlow<ConfirmationUiState>(ConfirmationUiState.Inactive)
    val state: StateFlow<ConfirmationUiState> = mutableState.asStateFlow()

    private var pending: ConfirmationBinding? = null
    private var activeJob: Job? = null

    init {
        require(voiceConfidenceThreshold in 0.0f..1.0f) {
            "voiceConfidenceThreshold must be between 0 and 1."
        }
    }

    fun present(binding: ConfirmationBinding) {
        if (activeJob?.isActive == true) return
        if (clock.nowMillis() >= binding.expiresAtMillis) {
            revoke(binding, ConfirmationUiState.ConfirmationExpired)
            return
        }
        pending = binding
        mutableState.value = ConfirmationUiState.NeedsConfirmation(binding)
    }

    fun confirmByText(currentActionType: String) {
        confirm(currentActionType, accepted = true)
    }

    fun confirmByVoice(
        currentActionType: String,
        evidence: VoiceConfirmationEvidence,
    ) {
        val accepted = evidence.isFinal &&
            evidence.confidence != null &&
            evidence.confidence >= voiceConfidenceThreshold &&
            evidence.text.trim().lowercase() in AcceptedVoiceConfirmations
        confirm(currentActionType, accepted)
    }

    fun cancelPending() {
        if (activeJob?.isActive == true) return
        val binding = pending
        if (binding == null) {
            if (mutableState.value != ConfirmationUiState.ConfirmationExpired) {
                mutableState.value = ConfirmationUiState.ConfirmationRejected
            }
            return
        }
        if (clock.nowMillis() >= binding.expiresAtMillis) {
            revoke(binding, ConfirmationUiState.ConfirmationExpired)
            return
        }
        revoke(binding, ConfirmationUiState.ConfirmationRejected)
    }

    private fun revoke(
        binding: ConfirmationBinding,
        terminalState: ConfirmationUiState,
    ) {
        pending = null
        val job = scope.launch {
            mutableState.value = ConfirmationUiState.Confirming(binding)
            try {
                when (
                    gateway.cancelConfirmation(
                        CommandConfirmationRequest(binding.token, binding.requestId, binding.sessionId),
                    )
                ) {
                    CommandConfirmationCancellationResult.Cancelled ->
                        mutableState.value = terminalState
                    is CommandConfirmationCancellationResult.Rejected ->
                        mutableState.value = terminalState
                    CommandConfirmationCancellationResult.InternalFailure,
                    CommandConfirmationCancellationResult.RuntimeDisabled,
                    -> restorePendingOrTerminal(binding, terminalState)
                }
            } catch (cancellation: CancellationException) {
                restorePendingOrTerminal(binding, terminalState)
                throw cancellation
            } catch (_: Exception) {
                restorePendingOrTerminal(binding, terminalState)
            }
        }
        activeJob = job
        job.invokeOnCompletion {
            if (activeJob === job) activeJob = null
        }
    }

    private fun confirm(currentActionType: String, accepted: Boolean) {
        if (activeJob?.isActive == true) return
        val binding = pending
        if (binding == null) {
            if (mutableState.value != ConfirmationUiState.ConfirmationExpired) {
                mutableState.value = ConfirmationUiState.ConfirmationRejected
            }
            return
        }
        if (!accepted) {
            revoke(binding, ConfirmationUiState.ConfirmationRejected)
            return
        }

        mutableState.value = ConfirmationUiState.Confirming(binding)
        if (clock.nowMillis() >= binding.expiresAtMillis) {
            revoke(binding, ConfirmationUiState.ConfirmationExpired)
            return
        }
        if (currentActionType != binding.actionType) {
            revoke(binding, ConfirmationUiState.ConfirmationRejected)
            return
        }

        pending = null
        val job = scope.launch {
            mutableState.value = ConfirmationUiState.ConfirmedExecuting(binding)
            try {
                mutableState.value = gateway.confirmAndExecute(
                    CommandConfirmationRequest(binding.token, binding.requestId, binding.sessionId),
                ).toConfirmationState(binding)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.value = ConfirmationUiState.ConfirmationRejected
            }
        }
        activeJob = job
        job.invokeOnCompletion {
            if (activeJob === job) activeJob = null
        }
    }

    private fun CommandDispatchResult.toConfirmationState(
        binding: ConfirmationBinding,
    ): ConfirmationUiState = when (this) {
        is CommandDispatchResult.Executed ->
            if (requestId == binding.requestId && actionType == binding.actionType) {
                ConfirmationUiState.Completed(this)
            } else {
                ConfirmationUiState.ConfirmationRejected
            }
        is CommandDispatchResult.DryRun ->
            if (requestId == binding.requestId && actionType == binding.actionType) {
                ConfirmationUiState.Completed(this)
            } else {
                ConfirmationUiState.ConfirmationRejected
            }
        is CommandDispatchResult.Rejected -> when (
            (reason as? CommandRejectionReason.ConfirmationRejected)?.reason
        ) {
            ConfirmationRejectionReason.EXPIRED -> ConfirmationUiState.ConfirmationExpired
            else -> ConfirmationUiState.ConfirmationRejected
        }
        is CommandDispatchResult.ExecutionFailed,
        is CommandDispatchResult.NeedsConfirmation,
        is CommandDispatchResult.RoutingFailed,
        -> ConfirmationUiState.ConfirmationRejected
    }

    private fun restorePendingOrTerminal(
        binding: ConfirmationBinding,
        terminalState: ConfirmationUiState,
    ) {
        if (clock.nowMillis() >= binding.expiresAtMillis) {
            mutableState.value = terminalState
        } else {
            pending = binding
            mutableState.value = ConfirmationUiState.NeedsConfirmation(binding)
        }
    }

    private companion object {
        val AcceptedVoiceConfirmations = setOf("si", "sí", "confirmo", "confirmar")
    }
}
