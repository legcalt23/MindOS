package com.mindos.android.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat as PlatformAudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import com.mindos.speech.AudioFormat
import com.mindos.speech.AudioInput
import com.mindos.speech.AudioOutputFailureReason
import com.mindos.speech.AudioOutputProvider
import com.mindos.speech.AudioOutputResult
import com.mindos.speech.AudioPayload
import com.mindos.speech.CaptureFailureReason
import com.mindos.speech.SpeechInputProvider
import com.mindos.speech.SpeechInputResult
import com.mindos.speech.SynthesizedAudio
import com.mindos.speech.VoiceSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class AndroidAudioInputConfig(
    val sampleRateHz: Int = 16_000,
    val channelCount: Int = 1,
    val maxDurationMs: Long = 10_000L,
    val timeoutMs: Long = 12_000L,
) {
    internal val maxPcmBytes: Int =
        ((sampleRateHz.toLong() * channelCount * PCM_BYTES_PER_SAMPLE * maxDurationMs) / 1_000L).toInt()

    init {
        require(sampleRateHz in 8_000..48_000) { "sampleRateHz must be between 8000 and 48000." }
        require(channelCount == 1) { "Android push-to-talk supports mono input only." }
        require(maxDurationMs in 1L..MAX_CAPTURE_DURATION_MS) {
            "maxDurationMs must be between 1 and $MAX_CAPTURE_DURATION_MS."
        }
        require(timeoutMs in 1L..MAX_CAPTURE_TIMEOUT_MS) {
            "timeoutMs must be between 1 and $MAX_CAPTURE_TIMEOUT_MS."
        }
    }

    private companion object {
        const val PCM_BYTES_PER_SAMPLE = 2
        const val MAX_CAPTURE_DURATION_MS = 30_000L
        const val MAX_CAPTURE_TIMEOUT_MS = 35_000L
    }
}

data class AndroidAudioOutputConfig(
    val timeoutMs: Long = 10_000L,
    val maxPayloadBytes: Int = 4 * 1024 * 1024,
) {
    init {
        require(timeoutMs in 1L..30_000L) { "timeoutMs must be between 1 and 30000." }
        require(maxPayloadBytes in 1..(8 * 1024 * 1024)) {
            "maxPayloadBytes must be between 1 and 8388608."
        }
    }
}

fun interface MicrophonePermissionChecker {
    fun isGranted(): Boolean
}

class AndroidMicrophonePermissionChecker(
    private val context: Context,
) : MicrophonePermissionChecker {
    override fun isGranted(): Boolean =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
}

fun interface PcmCaptureEngine {
    suspend fun capture(config: AndroidAudioInputConfig): ByteArray
}

fun interface PcmPlaybackEngine {
    suspend fun play(audio: SynthesizedAudio)
}

class AndroidSpeechInputProvider(
    private val permissionChecker: MicrophonePermissionChecker,
    private val captureEngine: PcmCaptureEngine = AndroidPcmCaptureEngine(),
    private val config: AndroidAudioInputConfig = AndroidAudioInputConfig(),
) : SpeechInputProvider {
    override suspend fun capture(session: VoiceSession): SpeechInputResult {
        if (!permissionChecker.isGranted()) {
            return SpeechInputResult.Failure(CaptureFailureReason.PERMISSION_DENIED)
        }

        return try {
            val captured = withTimeout(config.timeoutMs) { captureEngine.capture(config) }
            try {
                val bounded = captured.copyOf(minOf(captured.size, config.maxPcmBytes))
                try {
                    if (bounded.isEmpty() || bounded.all { it == 0.toByte() }) {
                        SpeechInputResult.Failure(CaptureFailureReason.EMPTY_AUDIO)
                    } else {
                        SpeechInputResult.Captured(
                            AudioInput(
                                requestId = session.requestId,
                                sessionId = session.sessionId,
                                languageTag = session.localeTag,
                                payload = AudioPayload.fromBytes(bounded),
                                format = AudioFormat(config.sampleRateHz, config.channelCount),
                                durationMs = pcmDurationMs(bounded.size, config.sampleRateHz, config.channelCount),
                            ),
                        )
                    }
                } finally {
                    bounded.fill(0)
                }
            } finally {
                captured.fill(0)
            }
        } catch (timeout: TimeoutCancellationException) {
            SpeechInputResult.Failure(CaptureFailureReason.TIMEOUT)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: SecurityException) {
            SpeechInputResult.Failure(CaptureFailureReason.PERMISSION_DENIED)
        } catch (_: Exception) {
            SpeechInputResult.Failure(CaptureFailureReason.BACKEND_ERROR)
        }
    }
}

class AndroidAudioOutputProvider(
    private val playbackEngine: PcmPlaybackEngine = AndroidPcmPlaybackEngine(),
    private val config: AndroidAudioOutputConfig = AndroidAudioOutputConfig(),
) : AudioOutputProvider {
    override suspend fun play(audio: SynthesizedAudio): AudioOutputResult {
        if (audio.payload.isEmpty) {
            return AudioOutputResult.Failure(AudioOutputFailureReason.EMPTY_AUDIO)
        }
        if (audio.payload.sizeBytes > config.maxPayloadBytes) {
            return AudioOutputResult.Failure(AudioOutputFailureReason.BACKEND_ERROR)
        }

        return try {
            withTimeout(config.timeoutMs) { playbackEngine.play(audio) }
            AudioOutputResult.Played
        } catch (timeout: TimeoutCancellationException) {
            AudioOutputResult.Failure(AudioOutputFailureReason.TIMEOUT)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            AudioOutputResult.Failure(AudioOutputFailureReason.BACKEND_ERROR)
        }
    }
}

class AndroidPcmCaptureEngine : PcmCaptureEngine {
    override suspend fun capture(config: AndroidAudioInputConfig): ByteArray = withContext(Dispatchers.IO) {
        val channelMask = PlatformAudioFormat.CHANNEL_IN_MONO
        val minBufferBytes = AudioRecord.getMinBufferSize(
            config.sampleRateHz,
            channelMask,
            PlatformAudioFormat.ENCODING_PCM_16BIT,
        )
        check(minBufferBytes > 0) { "Unsupported Android microphone format." }

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            config.sampleRateHz,
            channelMask,
            PlatformAudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferBytes, config.maxPcmBytes),
        )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "Android microphone did not initialize." }

        val result = ByteArray(config.maxPcmBytes)
        var offset = 0
        try {
            recorder.startRecording()
            while (offset < result.size) {
                currentCoroutineContext().ensureActive()
                val read = recorder.read(
                    result,
                    offset,
                    minOf(minBufferBytes, result.size - offset),
                    AudioRecord.READ_NON_BLOCKING,
                )
                when {
                    read > 0 -> offset += read
                    read == 0 -> delay(10L)
                    else -> error("Android microphone read failed.")
                }
            }
            result.copyOf(offset)
        } finally {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop()
            }
            recorder.release()
        }
    }
}

class AndroidPcmPlaybackEngine : PcmPlaybackEngine {
    override suspend fun play(audio: SynthesizedAudio): Unit = withContext(Dispatchers.IO) {
        check(audio.format.channelCount == 1) { "Android push-to-talk supports mono output only." }
        val bytes = audio.payload.copyBytes()
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .build(),
            )
            .setAudioFormat(
                PlatformAudioFormat.Builder()
                    .setEncoding(PlatformAudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(audio.format.sampleRateHz)
                    .setChannelMask(PlatformAudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bytes.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        check(track.state == AudioTrack.STATE_INITIALIZED) { "Android audio output did not initialize." }

        try {
            check(track.write(bytes, 0, bytes.size, AudioTrack.WRITE_BLOCKING) == bytes.size) {
                "Android audio output write failed."
            }
            currentCoroutineContext().ensureActive()
            track.play()
            delay(audio.durationMs.coerceAtLeast(1L))
        } finally {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.release()
            bytes.fill(0)
        }
    }
}

private fun pcmDurationMs(sizeBytes: Int, sampleRateHz: Int, channelCount: Int): Long =
    (sizeBytes.toLong() * 1_000L) / (sampleRateHz * channelCount * 2L)
