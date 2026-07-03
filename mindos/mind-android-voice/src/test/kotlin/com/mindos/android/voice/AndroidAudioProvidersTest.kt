package com.mindos.android.voice

import com.mindos.speech.AudioFormat
import com.mindos.speech.AudioOutputFailureReason
import com.mindos.speech.AudioOutputResult
import com.mindos.speech.AudioPayload
import com.mindos.speech.CaptureFailureReason
import com.mindos.speech.SpeechInputResult
import com.mindos.speech.SynthesizedAudio
import com.mindos.speech.VoiceSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidAudioProvidersTest {

    @Test
    fun deniedPermissionDoesNotOpenCaptureEngine() = runTest {
        var opened = false
        val provider = AndroidSpeechInputProvider(
            permissionChecker = MicrophonePermissionChecker { false },
            captureEngine = PcmCaptureEngine {
                opened = true
                byteArrayOf(1, 2)
            },
        )

        val result = provider.capture(session())

        assertEquals(CaptureFailureReason.PERMISSION_DENIED, assertIs<SpeechInputResult.Failure>(result).reason)
        assertTrue(!opened)
    }

    @Test
    fun captureReturnsBoundedPcmWithComputedDuration() = runTest {
        val provider = AndroidSpeechInputProvider(
            permissionChecker = MicrophonePermissionChecker { true },
            captureEngine = PcmCaptureEngine { ByteArray(32_000) { 1 } },
            config = AndroidAudioInputConfig(maxDurationMs = 1_000L),
        )

        val audio = assertIs<SpeechInputResult.Captured>(provider.capture(session())).audio

        assertEquals(32_000, audio.payload.sizeBytes)
        assertEquals(1_000L, audio.durationMs)
        assertEquals(AudioFormat(16_000, 1), audio.format)
        assertEquals("req", audio.requestId)
        assertEquals("session", audio.sessionId)
    }

    @Test
    fun silentCaptureIsRejectedAsEmptyAudio() = runTest {
        val provider = AndroidSpeechInputProvider(
            permissionChecker = MicrophonePermissionChecker { true },
            captureEngine = PcmCaptureEngine { ByteArray(64) },
        )

        val result = provider.capture(session())

        assertEquals(CaptureFailureReason.EMPTY_AUDIO, assertIs<SpeechInputResult.Failure>(result).reason)
    }

    @Test
    fun captureTimeoutIsTyped() = runTest {
        val provider = AndroidSpeechInputProvider(
            permissionChecker = MicrophonePermissionChecker { true },
            captureEngine = PcmCaptureEngine { awaitCancellation() },
            config = AndroidAudioInputConfig(timeoutMs = 100L),
        )

        val deferred = launch {
            assertEquals(CaptureFailureReason.TIMEOUT, failureReason(provider.capture(session())))
        }
        advanceTimeBy(101L)
        advanceUntilIdle()

        assertTrue(deferred.isCompleted)
    }

    @Test
    fun captureCancellationPropagatesToEngine() = runTest {
        var cancelled = false
        val provider = AndroidSpeechInputProvider(
            permissionChecker = MicrophonePermissionChecker { true },
            captureEngine = PcmCaptureEngine {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            },
        )

        val job = launch { provider.capture(session()) }
        runCurrent()
        job.cancel()
        advanceUntilIdle()

        assertTrue(cancelled)
        assertTrue(job.isCancelled)
    }

    @Test
    fun outputEmptyAudioDoesNotOpenPlaybackEngine() = runTest {
        var opened = false
        val provider = AndroidAudioOutputProvider(PcmPlaybackEngine { opened = true })

        val result = provider.play(synthesized(ByteArray(0)))

        assertEquals(AudioOutputFailureReason.EMPTY_AUDIO, assertIs<AudioOutputResult.Failure>(result).reason)
        assertTrue(!opened)
    }

    @Test
    fun outputTimeoutIsTypedAndCancellationPropagates() = runTest {
        var cancelled = false
        val provider = AndroidAudioOutputProvider(
            playbackEngine = PcmPlaybackEngine {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            },
            config = AndroidAudioOutputConfig(timeoutMs = 100L),
        )

        val deferred = launch {
            assertEquals(
                AudioOutputFailureReason.TIMEOUT,
                assertIs<AudioOutputResult.Failure>(provider.play(synthesized())).reason,
            )
        }
        advanceTimeBy(101L)
        advanceUntilIdle()

        assertTrue(deferred.isCompleted)
        assertTrue(cancelled)
    }

    @Test
    fun externalOutputCancellationIsNotConvertedToBackendFailure() = runTest {
        val provider = AndroidAudioOutputProvider(PcmPlaybackEngine { throw CancellationException("cancel") })

        assertFailsWith<CancellationException> { provider.play(synthesized()) }
    }
}

private fun failureReason(result: SpeechInputResult): CaptureFailureReason =
    assertIs<SpeechInputResult.Failure>(result).reason

private fun synthesized(bytes: ByteArray = byteArrayOf(1, 2, 3, 4)) = SynthesizedAudio(
    payload = AudioPayload.fromBytes(bytes),
    format = AudioFormat(16_000, 1),
    durationMs = 100L,
    backendId = "test",
)

private fun session() = VoiceSession("req", "session", "es-US")
