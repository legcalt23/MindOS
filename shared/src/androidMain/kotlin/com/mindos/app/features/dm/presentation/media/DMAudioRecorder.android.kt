package com.mindos.app.features.dm.presentation.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class AndroidDMAudioRecorder(
    private val context: Context,
    private val requestRecordAudioPermission: (((Boolean) -> Unit) -> Unit),
) : DMAudioRecorder {
    private val _state = MutableStateFlow(DMAudioRecorderState.Stopped)
    override val state: StateFlow<DMAudioRecorderState> = _state

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private val retainedFiles = mutableSetOf<File>()
    private var startedAtMillis: Long = 0L

    override fun startRecording(onResult: (DMAudioRecordingStartResult) -> Unit) {
        if (_state.value == DMAudioRecorderState.Recording) {
            onResult(DMAudioRecordingStartResult.AlreadyRecording)
            return
        }

        if (!hasRecordAudioPermission()) {
            requestRecordAudioPermission { granted ->
                if (granted) {
                    startRecordingInternal(onResult)
                } else {
                    onResult(DMAudioRecordingStartResult.PermissionDenied)
                }
            }
            return
        }

        startRecordingInternal(onResult)
    }

    private fun startRecordingInternal(onResult: (DMAudioRecordingStartResult) -> Unit) {
        try {
            val outputDir = File(context.cacheDir, "dm_audio").apply {
                mkdirs()
                deleteStaleAudioFiles(retainedFiles)
            }
            val file = File(outputDir, "dm_audio_${System.currentTimeMillis()}.m4a")
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44_100)
                setAudioEncodingBitRate(128_000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            outputFile = file
            startedAtMillis = System.currentTimeMillis()
            _state.value = DMAudioRecorderState.Recording
            onResult(DMAudioRecordingStartResult.Started)
        } catch (e: Exception) {
            releaseRecorder()
            outputFile?.delete()
            outputFile = null
            _state.value = DMAudioRecorderState.Stopped
            onResult(DMAudioRecordingStartResult.Failure(e.message ?: "No se pudo iniciar la grabacion."))
        }
    }

    override fun stopRecording(onResult: (DMAudioRecordingStopResult) -> Unit) {
        val activeRecorder = recorder
        val file = outputFile
        if (activeRecorder == null || file == null) {
            _state.value = DMAudioRecorderState.Stopped
            onResult(DMAudioRecordingStopResult.NoActiveRecording)
            return
        }

        try {
            activeRecorder.stop()
            val bytes = file.takeIf { it.exists() && it.length() > 0L }?.readBytes()
            if (bytes == null) {
                file.delete()
                onResult(DMAudioRecordingStopResult.Failure("La grabacion esta vacia."))
                return
            }

            val durationMs = readDurationMs(file) ?: (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0L)
            retainedFiles += file
            onResult(
                DMAudioRecordingStopResult.Success(
                    DMAudioRecording(
                        bytes = bytes,
                        fileName = file.name,
                        mimeType = "audio/mp4",
                        sizeBytes = bytes.size.toLong(),
                        durationMs = durationMs,
                        localPath = file.absolutePath,
                    )
                )
            )
        } catch (e: Exception) {
            file.delete()
            onResult(DMAudioRecordingStopResult.Failure(e.message ?: "No se pudo finalizar la grabacion."))
        } finally {
            releaseRecorder()
            outputFile = null
            startedAtMillis = 0L
            _state.value = DMAudioRecorderState.Stopped
        }
    }

    override fun cancelRecording() {
        val file = outputFile
        runCatching { recorder?.stop() }
        releaseRecorder()
        file?.delete()
        retainedFiles.forEach { it.delete() }
        retainedFiles.clear()
        outputFile = null
        startedAtMillis = 0L
        _state.value = DMAudioRecorderState.Stopped
    }

    private fun hasRecordAudioPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun releaseRecorder() {
        recorder?.release()
        recorder = null
    }

    private fun readDurationMs(file: File): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun File.deleteStaleAudioFiles(retained: Set<File>) {
        val cutoff = System.currentTimeMillis() - MaxRetainedAudioAgeMillis
        listFiles()
            ?.filter { it.isFile && it.name.startsWith("dm_audio_") && it !in retained && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }

    private companion object {
        const val MaxRetainedAudioAgeMillis = 24L * 60L * 60L * 1000L
    }
}

@Composable
actual fun rememberDMAudioRecorder(): DMAudioRecorder {
    val context = LocalContext.current
    var pendingPermissionResult by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingPermissionResult?.invoke(granted)
        pendingPermissionResult = null
    }
    val recorder = remember(context, permissionLauncher) {
        AndroidDMAudioRecorder(context) { onPermissionResult ->
            pendingPermissionResult = onPermissionResult
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    
    DisposableEffect(recorder) {
        onDispose {
            recorder.cancelRecording()
        }
    }
    
    return recorder
}
