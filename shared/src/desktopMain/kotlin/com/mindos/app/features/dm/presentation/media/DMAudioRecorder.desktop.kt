package com.mindos.app.features.dm.presentation.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DesktopDMAudioRecorder : DMAudioRecorder {
    private val _state = MutableStateFlow(DMAudioRecorderState.Stopped)
    override val state: StateFlow<DMAudioRecorderState> = _state

    override fun startRecording(onResult: (DMAudioRecordingStartResult) -> Unit) {
        onResult(DMAudioRecordingStartResult.Failure("La grabacion de audio no esta disponible en escritorio."))
    }

    override fun stopRecording(onResult: (DMAudioRecordingStopResult) -> Unit) {
        onResult(DMAudioRecordingStopResult.NoActiveRecording)
    }

    override fun cancelRecording() {
        _state.value = DMAudioRecorderState.Stopped
    }
}

@Composable
actual fun rememberDMAudioRecorder(): DMAudioRecorder = remember { DesktopDMAudioRecorder() }
