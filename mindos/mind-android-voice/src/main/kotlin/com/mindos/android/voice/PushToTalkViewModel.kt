package com.mindos.android.voice

import com.mindos.command.runtime.CommandDispatchResult
import com.mindos.speech.VoiceCommandOrchestrator
import com.mindos.speech.VoiceCommandOutcome
import com.mindos.speech.VoiceSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

fun interface VoiceSessionFactory {
    fun create(): VoiceSession
}

sealed interface PushToTalkUiState {
    val text: String

    data object DisabledByFlag : PushToTalkUiState {
        override val text: String = "Los comandos de voz no estan disponibles."
    }

    data object Idle : PushToTalkUiState {
        override val text: String = "Pulsa para hablar."
    }

    data class PermissionRequired(val denied: Boolean = false) : PushToTalkUiState {
        override val text: String =
            if (denied) "Permiso de microfono denegado." else "Se necesita permiso de microfono."
    }

    data object Listening : PushToTalkUiState {
        override val text: String = "Escuchando..."
    }

    data object Processing : PushToTalkUiState {
        override val text: String = "Procesando comando..."
    }

    data class NeedsConfirmation(
        val pending: CommandDispatchResult.NeedsConfirmation,
        override val text: String,
    ) : PushToTalkUiState

    data class Executed(override val text: String) : PushToTalkUiState
    data class Rejected(override val text: String) : PushToTalkUiState
    data class Degraded(override val text: String) : PushToTalkUiState
    data class Failed(override val text: String) : PushToTalkUiState
}

class AndroidPushToTalkViewModel(
    enabled: Boolean,
    private val orchestrator: VoiceCommandOrchestrator,
    private val sessionFactory: VoiceSessionFactory,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<PushToTalkUiState>(
        if (enabled) PushToTalkUiState.Idle else PushToTalkUiState.DisabledByFlag,
    )
    val state: StateFlow<PushToTalkUiState> = mutableState.asStateFlow()

    private var activeJob: Job? = null
    private val enabled = enabled

    fun onMicrophonePressed(permissionGranted: Boolean) {
        if (!enabled || activeJob?.isActive == true) return
        if (!permissionGranted) {
            mutableState.value = PushToTalkUiState.PermissionRequired()
            return
        }
        start()
    }

    fun onPermissionResult(granted: Boolean) {
        if (!enabled || activeJob?.isActive == true) return
        if (!granted) {
            mutableState.value = PushToTalkUiState.PermissionRequired(denied = true)
            return
        }
        start()
    }

    fun cancelBeforeExecution() {
        activeJob?.cancel()
        activeJob = null
        if (enabled) mutableState.value = PushToTalkUiState.Idle
    }

    private fun start() {
        val job = scope.launch {
            mutableState.value = PushToTalkUiState.Listening
            yield()
            mutableState.value = PushToTalkUiState.Processing
            try {
                mutableState.value = orchestrator.runOnce(sessionFactory.create()).toUiState()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.value = PushToTalkUiState.Failed("No se pudo procesar el comando.")
            }
        }
        activeJob = job
        job.invokeOnCompletion {
            if (activeJob === job) activeJob = null
        }
    }

    private fun VoiceCommandOutcome.toUiState(): PushToTalkUiState = when (this) {
        is VoiceCommandOutcome.Completed -> when (val result = dispatch) {
            is CommandDispatchResult.NeedsConfirmation ->
                PushToTalkUiState.NeedsConfirmation(result, responseText.value)
            is CommandDispatchResult.Executed,
            is CommandDispatchResult.DryRun,
            -> PushToTalkUiState.Executed(responseText.value)
            is CommandDispatchResult.ExecutionFailed,
            is CommandDispatchResult.Rejected,
            is CommandDispatchResult.RoutingFailed,
            -> PushToTalkUiState.Rejected(responseText.value)
        }
        is VoiceCommandOutcome.Degraded -> PushToTalkUiState.Degraded(responseText.value)
        is VoiceCommandOutcome.Rejected -> PushToTalkUiState.Rejected("Comando rechazado: ${reason.name}.")
        is VoiceCommandOutcome.Failed -> PushToTalkUiState.Failed("Fallo de voz: ${reason.name}.")
    }
}
