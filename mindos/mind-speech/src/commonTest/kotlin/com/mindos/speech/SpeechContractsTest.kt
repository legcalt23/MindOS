package com.mindos.speech

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class SpeechContractsTest {

    @Test
    fun audioPayloadDefensivelyCopiesInputAndOutput() {
        val source = byteArrayOf(1, 2, 3)
        val payload = AudioPayload.fromBytes(source)
        source[0] = 9

        val firstRead = payload.copyBytes()
        firstRead[1] = 9

        assertContentEquals(byteArrayOf(1, 2, 3), payload.copyBytes())
        assertEquals("AudioPayload(sizeBytes=3)", payload.toString())
    }

    @Test
    fun transcriptAllowsNullableConfidenceAndRejectsInvalidConfidence() {
        val transcript = TranscriptResult("settings", null, "en", 100, "mock", isFinal = true)

        assertNull(transcript.confidence)
        assertFailsWith<IllegalArgumentException> {
            TranscriptResult("settings", 1.1f, "en", 100, "mock", isFinal = true)
        }
    }

    @Test
    fun runtimeConfigIsFailClosedByDefault() {
        val config = VoiceRuntimeConfig()

        assertFalse(config.canRun)
        assertFalse(config.logTranscripts)
        assertFalse(config.streamingEnabled)
    }

    @Test
    fun killSwitchOverridesEnabledFlag() {
        assertFalse(VoiceRuntimeConfig(enabled = true, killSwitch = true).canRun)
    }

    @Test
    fun expectedBackendFailuresAreTyped() {
        val result: AsrResult = AsrResult.Failure(BackendFailureReason.TIMEOUT)

        assertEquals(
            BackendFailureReason.TIMEOUT,
            assertIs<AsrResult.Failure>(result).reason,
        )
    }
}
