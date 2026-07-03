package com.mindos.app.core.logging

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ObservabilityHubTest {

    private val originalSink = ObservabilityHub.sink
    private val originalHandler = NonFatalReporter.handler

    @AfterTest
    fun restoreSink() {
        ObservabilityHub.sink = originalSink
        NonFatalReporter.handler = originalHandler
    }

    @Test
    fun defaultSinkIsSafeNoOp() {
        // Even without any platform wiring, the hub must not throw.
        ObservabilityHub.breadcrumb("hello")
        ObservabilityHub.customKey("k", "v")
        ObservabilityHub.customKey("flag", true)
        ObservabilityHub.customKey("count", 1)
        ObservabilityHub.recordNonFatal("default_noop", IllegalStateException("noop"))
    }

    @Test
    fun installedSinkReceivesAllCalls() {
        val recorder = RecordingSink()
        ObservabilityHub.sink = recorder

        ObservabilityHub.breadcrumb("nav_to:dm_list")
        ObservabilityHub.customKey("current_route", "dm_list")
        ObservabilityHub.customKey("authenticated", true)
        ObservabilityHub.customKey("nav_depth", 3)
        val err = RuntimeException("secret-canary", IllegalArgumentException("cause-canary"))
        ObservabilityHub.recordNonFatal("feed publish", err)

        assertEquals(listOf("nav_to:dm_list"), recorder.breadcrumbs)
        assertEquals(
            mapOf(
                "current_route" to "dm_list",
                "authenticated" to "true",
                "nav_depth" to "3",
            ),
            recorder.customKeys,
        )
        assertEquals(1, recorder.nonFatals.size)
        val event = recorder.nonFatals.first()
        assertEquals("feed_publish", event.operation)
        assertEquals("runtimeexception", event.errorType)
        assertFalse(event.toString().contains("secret-canary"))
        assertFalse(event.toString().contains("cause-canary"))

        val sanitized = SanitizedNonFatalException(event)
        assertFalse(sanitized.message.orEmpty().contains("secret-canary"))
        assertFalse(sanitized.message.orEmpty().contains("cause-canary"))
        assertNull(sanitized.cause)
    }

    @Test
    fun sinkSwapIsAtomic_furtherCallsRouteToNewSink() {
        val first = RecordingSink()
        val second = RecordingSink()

        ObservabilityHub.sink = first
        ObservabilityHub.breadcrumb("a")
        ObservabilityHub.sink = second
        ObservabilityHub.breadcrumb("b")

        assertEquals(listOf("a"), first.breadcrumbs)
        assertEquals(listOf("b"), second.breadcrumbs)
    }

    @Test
    fun safeErrorLog_usesStructuredSinkInsteadOfLegacyHandlerWhenSinkIsInstalled() {
        val recorder = RecordingSink()
        var legacyCalls = 0
        ObservabilityHub.sink = recorder
        NonFatalReporter.handler = { legacyCalls += 1 }

        val error = IllegalStateException("secret-canary")
        safeErrorLog("Test", "failure", error)

        assertEquals(1, recorder.nonFatals.size)
        assertEquals("test.failure", recorder.nonFatals.first().operation)
        assertEquals("illegalstateexception", recorder.nonFatals.first().errorType)
        assertFalse(recorder.nonFatals.first().toString().contains("secret-canary"))
        assertEquals(0, legacyCalls)
    }

    @Test
    fun safeErrorLog_usesLegacyHandlerWhenNoSinkIsInstalled() {
        var legacyCalls = 0
        NonFatalReporter.handler = { legacyCalls += 1 }

        safeErrorLog("Test", "failure", IllegalStateException("legacy"))

        assertEquals(1, legacyCalls)
    }

    private class RecordingSink : ObservabilitySink {
        val breadcrumbs = mutableListOf<String>()
        val customKeys = mutableMapOf<String, String>()
        val nonFatals = mutableListOf<NonFatalTelemetryEvent>()

        override fun breadcrumb(message: String) {
            breadcrumbs += message
        }

        override fun customKey(key: String, value: String) {
            customKeys[key] = value
        }

        override fun customKey(key: String, value: Boolean) {
            customKeys[key] = value.toString()
        }

        override fun customKey(key: String, value: Int) {
            customKeys[key] = value.toString()
        }

        override fun recordNonFatal(event: NonFatalTelemetryEvent) {
            nonFatals += event
        }
    }
}
