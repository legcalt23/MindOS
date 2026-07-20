package com.mindos.app.features.dm.presentation.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.setActive
import platform.Foundation.NSDate
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.dataWithContentsOfURL
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

class IosDMAudioRecorder : DMAudioRecorder {
    private val _state = MutableStateFlow(DMAudioRecorderState.Stopped)
    override val state: StateFlow<DMAudioRecorderState> = _state

    private var recorder: AVAudioRecorder? = null
    private var outputUrl: NSURL? = null
    private var outputFileName: String? = null
    private var startedAtMillis: Long = 0L

    @OptIn(ExperimentalForeignApi::class)
    override fun startRecording(onResult: (DMAudioRecordingStartResult) -> Unit) {
        if (_state.value == DMAudioRecorderState.Recording) {
            onResult(DMAudioRecordingStartResult.AlreadyRecording)
            return
        }

        AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            dispatch_async(dispatch_get_main_queue()) {
                if (granted) {
                    startRecordingInternal(onResult)
                } else {
                    onResult(DMAudioRecordingStartResult.PermissionDenied)
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun startRecordingInternal(onResult: (DMAudioRecordingStartResult) -> Unit) {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayAndRecord, null)
            session.setActive(true, null)

            val fileName = "dm_audio_${NSUUID().UUIDString}.m4a"
            val directoryPath = NSHomeDirectory() + "/Documents/dm_audio"
            NSFileManager.defaultManager.createDirectoryAtPath(directoryPath, true, null, null)
            val url = NSURL.fileURLWithPath("$directoryPath/$fileName")

            val settings = mapOf<Any?, Any?>(
                platform.AVFAudio.AVFormatIDKey to platform.CoreAudioTypes.kAudioFormatMPEG4AAC,
                platform.AVFAudio.AVSampleRateKey to 44100.0,
                platform.AVFAudio.AVNumberOfChannelsKey to 1,
                AVEncoderBitRateKey to 128000,
            )

            val newRecorder = AVAudioRecorder(url, settings, null)
            if (newRecorder.prepareToRecord() && newRecorder.record()) {
                recorder = newRecorder
                outputUrl = url
                outputFileName = fileName
                startedAtMillis = nowMillis()
                _state.value = DMAudioRecorderState.Recording
                onResult(DMAudioRecordingStartResult.Started)
            } else {
                onResult(DMAudioRecordingStartResult.Failure("No se pudo iniciar la grabacion."))
            }
        } catch (e: Exception) {
            releaseRecorder()
            outputUrl = null
            outputFileName = null
            startedAtMillis = 0L
            _state.value = DMAudioRecorderState.Stopped
            onResult(DMAudioRecordingStartResult.Failure(e.message ?: "No se pudo iniciar la grabacion."))
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun stopRecording(onResult: (DMAudioRecordingStopResult) -> Unit) {
        val activeRecorder = recorder
        val url = outputUrl
        if (activeRecorder == null || url == null) {
            _state.value = DMAudioRecorderState.Stopped
            onResult(DMAudioRecordingStopResult.NoActiveRecording)
            return
        }

        try {
            activeRecorder.stop()
            releaseRecorder()
            AVAudioSession.sharedInstance().setActive(false, null)

            val data = NSData.dataWithContentsOfURL(url)
            if (data != null && data.length > 0uL) {
                val bytes = data.toByteArray()
                val durationMs = (nowMillis() - startedAtMillis).coerceAtLeast(0L)
                val fileName = outputFileName ?: url.lastPathComponent ?: "dm_audio.m4a"
                val localPath = url.path
                outputUrl = null
                outputFileName = null
                startedAtMillis = 0L
                _state.value = DMAudioRecorderState.Stopped
                onResult(
                    DMAudioRecordingStopResult.Success(
                        DMAudioRecording(
                            bytes = bytes,
                            fileName = fileName,
                            mimeType = "audio/mp4",
                            sizeBytes = bytes.size.toLong(),
                            durationMs = durationMs,
                            localPath = localPath,
                        )
                    )
                )
                return
            }
            NSFileManager.defaultManager.removeItemAtURL(url, null)
            onResult(DMAudioRecordingStopResult.Failure("La grabacion esta vacia."))
        } catch (e: Exception) {
            NSFileManager.defaultManager.removeItemAtURL(url, null)
            onResult(DMAudioRecordingStopResult.Failure(e.message ?: "No se pudo finalizar la grabacion."))
        } finally {
            releaseRecorder()
            outputUrl = null
            outputFileName = null
            startedAtMillis = 0L
            _state.value = DMAudioRecorderState.Stopped
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun cancelRecording() {
        val url = outputUrl
        runCatching { recorder?.stop() }
        releaseRecorder()
        if (url != null) {
            NSFileManager.defaultManager.removeItemAtURL(url, null)
        }
        outputUrl = null
        outputFileName = null
        startedAtMillis = 0L
        _state.value = DMAudioRecorderState.Stopped
    }

    private fun releaseRecorder() {
        recorder = null
    }

    private fun nowMillis(): Long =
        (NSDate().timeIntervalSince1970 * 1000.0).toLong()

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        val bytes = ByteArray(size)
        if (size > 0) {
            memcpy(bytes.refTo(0), this.bytes, this.length)
        }
        return bytes
    }
}

@Composable
actual fun rememberDMAudioRecorder(): DMAudioRecorder {
    val recorder = remember { IosDMAudioRecorder() }
    DisposableEffect(recorder) {
        onDispose { recorder.cancelRecording() }
    }
    return recorder
}
