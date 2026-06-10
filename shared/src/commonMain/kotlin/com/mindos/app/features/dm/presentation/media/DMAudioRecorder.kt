package com.mindos.app.features.dm.presentation.media

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

enum class DMAudioRecorderState {
    Stopped,
    Recording,
}

data class DMAudioRecording(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long?,
    val localPath: String?,
)

sealed interface DMAudioRecordingStartResult {
    data object Started : DMAudioRecordingStartResult
    data object AlreadyRecording : DMAudioRecordingStartResult
    data object PermissionDenied : DMAudioRecordingStartResult
    data class Failure(val message: String) : DMAudioRecordingStartResult
}

sealed interface DMAudioRecordingStopResult {
    data class Success(val recording: DMAudioRecording) : DMAudioRecordingStopResult
    data object NoActiveRecording : DMAudioRecordingStopResult
    data class Failure(val message: String) : DMAudioRecordingStopResult
}

interface DMAudioRecorder {
    val state: StateFlow<DMAudioRecorderState>

    fun startRecording(onResult: (DMAudioRecordingStartResult) -> Unit)
    fun stopRecording(onResult: (DMAudioRecordingStopResult) -> Unit)
    fun cancelRecording()
}

@Composable
expect fun rememberDMAudioRecorder(): DMAudioRecorder
