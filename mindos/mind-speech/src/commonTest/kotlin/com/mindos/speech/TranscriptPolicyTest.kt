package com.mindos.speech

import com.mindos.speech.mock.SpeechFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TranscriptPolicyTest {

    @Test
    fun rejectsPartialTranscript() {
        val result = DefaultTranscriptPolicy().evaluate(SpeechFixtures.transcript(isFinal = false))

        assertRejected(VoiceRejectionReason.TRANSCRIPT_NOT_FINAL, result)
    }

    @Test
    fun rejectsEmptyTranscript() {
        val result = DefaultTranscriptPolicy().evaluate(SpeechFixtures.transcript(text = "   "))

        assertRejected(VoiceRejectionReason.TRANSCRIPT_EMPTY, result)
    }

    @Test
    fun rejectsLowConfidence() {
        val result = DefaultTranscriptPolicy(
            TranscriptPolicyConfig(confidenceThreshold = 0.80f),
        ).evaluate(SpeechFixtures.transcript(confidence = 0.79f))

        assertRejected(VoiceRejectionReason.CONFIDENCE_TOO_LOW, result)
    }

    @Test
    fun nullableConfidenceIsConfigurable() {
        val transcript = SpeechFixtures.transcript(confidence = null)

        assertIs<TranscriptPolicyResult.Accepted>(
            DefaultTranscriptPolicy(
                TranscriptPolicyConfig(acceptNullConfidence = true),
            ).evaluate(transcript),
        )
        assertRejected(
            VoiceRejectionReason.CONFIDENCE_TOO_LOW,
            DefaultTranscriptPolicy(
                TranscriptPolicyConfig(acceptNullConfidence = false),
            ).evaluate(transcript),
        )
    }

    @Test
    fun stripsOptionalLeadingWakeWordAndTerminalAsrPunctuation() {
        val result = DefaultTranscriptPolicy().evaluate(
            SpeechFixtures.transcript(text = "MindVerse, abre ajustes."),
        )

        assertEquals(
            TranscriptPolicyResult.Accepted("abre ajustes", "es-US"),
            result,
        )
    }

    @Test
    fun requiredWakeWordRejectsWhenMissing() {
        val policy = DefaultTranscriptPolicy(TranscriptPolicyConfig(requireWakeWord = true))

        val result = policy.evaluate(SpeechFixtures.transcript(text = "abre ajustes"))

        assertRejected(VoiceRejectionReason.WAKE_WORD_MISSING, result)
    }

    @Test
    fun rejectsOriginalTranscriptOverMaximumLength() {
        val policy = DefaultTranscriptPolicy(TranscriptPolicyConfig(maxTextLength = 10))

        val result = policy.evaluate(SpeechFixtures.transcript(text = "MindVerse, a"))

        assertRejected(VoiceRejectionReason.TRANSCRIPT_TOO_LONG, result)
    }

    @Test
    fun stripsOnlyTerminalPunctuationAndPreservesInternalContentAndLocale() {
        val result = DefaultTranscriptPolicy().evaluate(
            SpeechFixtures.transcript(text = "abre v.anesa?!", languageTag = "es-MX"),
        )

        assertEquals(
            TranscriptPolicyResult.Accepted("abre v.anesa", "es-MX"),
            result,
        )
    }

    @Test
    fun doesNotStripWakeWordFromInsideTranscript() {
        val result = DefaultTranscriptPolicy().evaluate(
            SpeechFixtures.transcript(text = "abre MindVerse ajustes."),
        )

        assertEquals(
            TranscriptPolicyResult.Accepted("abre MindVerse ajustes", "es-US"),
            result,
        )
    }

    private fun assertRejected(
        expected: VoiceRejectionReason,
        result: TranscriptPolicyResult,
    ) {
        assertEquals(expected, assertIs<TranscriptPolicyResult.Rejected>(result).reason)
    }
}
