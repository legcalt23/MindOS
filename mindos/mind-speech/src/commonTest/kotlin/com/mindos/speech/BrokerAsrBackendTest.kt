package com.mindos.speech

import com.mindos.speech.mock.SpeechFixtures
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BrokerAsrBackendTest {

    @Test
    fun mapsCorrelatedBrokerResponseToFinalTranscript() = runTest {
        val input = SpeechFixtures.audioInput()
        val backend = BrokerAsrBackend(
            transport = SpeechBrokerTransport {
                SpeechBrokerResult.Success(
                    SpeechBrokerTranscript(
                        requestId = input.requestId,
                        sessionId = input.sessionId,
                        text = "abre ajustes",
                        confidence = 0.94f,
                        languageTag = input.languageTag,
                        durationMs = input.durationMs,
                        backendId = "nvidia-nim",
                        isFinal = true,
                    ),
                )
            },
        )

        val transcript = assertIs<AsrResult.Success>(backend.transcribe(input)).transcript

        assertEquals("abre ajustes", transcript.text)
        assertEquals("nvidia-nim", transcript.backendId)
    }

    @Test
    fun rejectsMismatchedCorrelationWithoutExposingTranscript() = runTest {
        val input = SpeechFixtures.audioInput()
        val backend = BrokerAsrBackend(
            transport = SpeechBrokerTransport {
                SpeechBrokerResult.Success(
                    SpeechBrokerTranscript(
                        requestId = "different",
                        sessionId = input.sessionId,
                        text = "abre ajustes",
                        confidence = null,
                        languageTag = input.languageTag,
                        durationMs = input.durationMs,
                        backendId = "nvidia-nim",
                        isFinal = true,
                    ),
                )
            },
        )

        assertEquals(
            BackendFailureReason.BACKEND_ERROR,
            assertIs<AsrResult.Failure>(backend.transcribe(input)).reason,
        )
    }

    @Test
    fun rejectsUnsupportedAudioBeforeCallingTransport() = runTest {
        var calls = 0
        val backend = BrokerAsrBackend(
            transport = SpeechBrokerTransport {
                calls += 1
                SpeechBrokerResult.Unavailable
            },
        )
        val input = SpeechFixtures.audioInput().copy(format = AudioFormat(44_100, 2))

        assertEquals(
            BackendFailureReason.INVALID_INPUT,
            assertIs<AsrResult.Failure>(backend.transcribe(input)).reason,
        )
        assertEquals(0, calls)
    }
}
