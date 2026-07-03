package com.mindos.speech

import kotlin.test.Test
import kotlin.test.assertFalse

class SpeechTelemetryPrivacyTest {
    @Test
    fun textualRepresentationDoesNotExposeCorrelationIdentifiers() {
        val event = SpeechTelemetryEvent(
            metric = SpeechMetric.ASR_RESULT,
            requestId = "private-request",
            sessionId = "private-session",
            result = SpeechTelemetryResult.SUCCESS,
        )

        assertFalse(event.toString().contains("private-request"))
        assertFalse(event.toString().contains("private-session"))
    }
}
