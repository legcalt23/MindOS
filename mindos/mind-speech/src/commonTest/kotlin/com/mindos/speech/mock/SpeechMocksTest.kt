package com.mindos.speech.mock

import com.mindos.speech.AsrResult
import com.mindos.speech.AudioOutputFailureReason
import com.mindos.speech.AudioOutputResult
import com.mindos.speech.BackendFailureReason
import com.mindos.speech.SpeechInputResult
import com.mindos.speech.TtsRequest
import com.mindos.speech.TtsResult
import com.mindos.speech.VoiceSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SpeechMocksTest {

    @Test
    fun asrScenariosAreDeterministicTypedResults() = runTest {
        val input = SpeechFixtures.audioInput()
        val success = MockASRBackend.success()
        val lowConfidence = MockASRBackend.lowConfidence()
        val empty = MockASRBackend.emptyTranscript()
        val partial = MockASRBackend.partial()
        val alternatives = MockASRBackend.withAlternatives(listOf("abre mensajes", "abre ajustes"))

        assertEquals("abre ajustes", assertIs<AsrResult.Success>(success.transcribe(input)).transcript.text)
        assertEquals(0.25f, assertIs<AsrResult.Success>(lowConfidence.transcribe(input)).transcript.confidence)
        assertEquals("", assertIs<AsrResult.Success>(empty.transcribe(input)).transcript.text)
        assertTrue(!assertIs<AsrResult.Success>(partial.transcribe(input)).transcript.isFinal)
        assertEquals(
            listOf("abre mensajes", "abre ajustes"),
            assertIs<AsrResult.Success>(alternatives.transcribe(input)).transcript.alternatives,
        )
        assertEquals(ObservedAudioInput(4, 250L, 16_000, 1), success.lastObservedInput)
    }

    @Test
    fun asrTimeoutAndBackendErrorDoNotThrow() = runTest {
        val input = SpeechFixtures.audioInput()

        assertEquals(
            BackendFailureReason.TIMEOUT,
            assertIs<AsrResult.Failure>(MockASRBackend.timeout().transcribe(input)).reason,
        )
        assertEquals(
            BackendFailureReason.BACKEND_ERROR,
            assertIs<AsrResult.Failure>(MockASRBackend.backendError().transcribe(input)).reason,
        )
    }

    @Test
    fun fixtureProducesExpectedTranscriptWithoutRetainingRawAudio() = runTest {
        val backend = MockASRBackend.success(SpeechFixtures.transcript(text = "abre mensajes"))

        val result = assertIs<AsrResult.Success>(backend.transcribe(SpeechFixtures.audioInput()))

        assertEquals("abre mensajes", result.transcript.text)
        assertEquals(4, backend.lastObservedInput?.sizeBytes)
    }

    @Test
    fun ttsScenariosAreDeterministicTypedResults() = runTest {
        val request = TtsRequest("Accion completada.", "es-US")

        assertEquals(
            4,
            assertIs<TtsResult.Success>(MockTTSBackend.success().synthesize(request)).audio.payload.sizeBytes,
        )
        assertEquals(
            BackendFailureReason.TIMEOUT,
            assertIs<TtsResult.Failure>(MockTTSBackend.timeout().synthesize(request)).reason,
        )
        assertEquals(
            BackendFailureReason.BACKEND_ERROR,
            assertIs<TtsResult.Failure>(MockTTSBackend.backendError().synthesize(request)).reason,
        )
        assertTrue(
            assertIs<TtsResult.Success>(MockTTSBackend.emptyAudio().synthesize(request)).audio.payload.isEmpty,
        )
    }

    @Test
    fun fakeInputAndOutputProvidersModelExpectedFailures() = runTest {
        assertIs<SpeechInputResult.Captured>(
            FakeSpeechInputProvider.captured().capture(VoiceSession("voice-req", "voice-session")),
        )
        val output = FakeAudioOutputProvider()

        val result = output.play(SpeechFixtures.synthesizedAudio(bytes = ByteArray(0), durationMs = 0L))

        assertEquals(
            AudioOutputFailureReason.EMPTY_AUDIO,
            assertIs<AudioOutputResult.Failure>(result).reason,
        )
    }

    @Test
    fun ttsFailureHasNoCommandExecutionOrRetryCapability() = runTest {
        var commandExecutionCount = 1
        val tts = MockTTSBackend.backendError()

        assertIs<TtsResult.Failure>(tts.synthesize(TtsRequest("Action completed.", "en")))

        assertEquals(1, tts.invocationCount)
        assertEquals(1, commandExecutionCount)
    }
}
