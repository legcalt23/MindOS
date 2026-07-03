package com.mindos.android.voice

import com.mindos.speech.SpeechBrokerResult
import com.mindos.speech.mock.SpeechFixtures
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidSpeechBrokerTransportTest {

    @Test
    fun requiresHttpsWithoutEmbeddedCredentialsOrQuery() {
        assertFailsWith<IllegalArgumentException> {
            AndroidSpeechBrokerConfig("http://broker.example/asr")
        }
        assertFailsWith<IllegalArgumentException> {
            AndroidSpeechBrokerConfig("https://user:pass@broker.example/asr")
        }
        assertFailsWith<IllegalArgumentException> {
            AndroidSpeechBrokerConfig("https://broker.example/asr?token=secret")
        }
    }

    @Test
    fun sendsOnlyBrokerCredentialsAndCorrelatedAudioMetadata() = runTest {
        var captured: SpeechBrokerHttpRequest? = null
        val input = SpeechFixtures.audioInput()
        val transport = AndroidSpeechBrokerTransport(
            tokenProvider = SpeechBrokerTokenProvider { SpeechBrokerTokens("firebase-id", "app-check") },
            config = AndroidSpeechBrokerConfig("https://broker.example/speechAsrRecognize"),
            httpEngine = SpeechBrokerHttpEngine { request ->
                captured = request
                SpeechBrokerHttpResponse(
                    200,
                    """
                    {
                      "requestId":"${input.requestId}",
                      "sessionId":"${input.sessionId}",
                      "transcript":"abre ajustes",
                      "confidence":0.95,
                      "languageTag":"${input.languageTag}",
                      "durationMs":${input.durationMs},
                      "backendId":"nvidia-nim",
                      "final":true
                    }
                    """.trimIndent(),
                )
            },
        )

        val transcript = assertIs<SpeechBrokerResult.Success>(transport.transcribe(input)).transcript
        val request = requireNotNull(captured)

        assertEquals("abre ajustes", transcript.text)
        assertEquals("Bearer firebase-id", request.headers["Authorization"])
        assertEquals("app-check", request.headers["X-Firebase-AppCheck"])
        assertEquals(input.requestId, request.headers["X-MindOS-Request-Id"])
        assertEquals("pcm_s16le", request.headers["X-MindOS-Audio-Encoding"])
        assertNull(request.headers["NVIDIA_API_KEY"])
        assertTrue(request.body.all { it == 0.toByte() }, "transport must clear its copied audio buffer")
    }

    @Test
    fun mapsAuthorizationAndRateLimitFailuresToRejected() = runTest {
        val input = SpeechFixtures.audioInput()
        for (status in listOf(401, 403, 429)) {
            val transport = AndroidSpeechBrokerTransport(
                tokenProvider = SpeechBrokerTokenProvider { SpeechBrokerTokens("id", "app") },
                config = AndroidSpeechBrokerConfig("https://broker.example/asr"),
                httpEngine = SpeechBrokerHttpEngine { SpeechBrokerHttpResponse(status, "{}") },
            )

            assertEquals(SpeechBrokerResult.Rejected, transport.transcribe(input))
        }
    }
}
