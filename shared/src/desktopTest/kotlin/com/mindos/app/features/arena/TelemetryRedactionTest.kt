package com.mindos.app.features.arena

import com.mindos.app.features.arena.observability.ArenaLogger
import com.mindos.app.features.arena.observability.NonFatalTelemetryEvent
import com.mindos.app.features.arena.observability.ObservabilitySink
import com.mindos.app.features.arena.sanitization.DataSanitizer
import com.mindos.app.features.arena.sanitization.SafeArenaException
import com.mindos.app.features.arena.sanitization.SyntheticIdMapper
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Task 9: Telemetry redaction — no rating values or raw throwables ever reach a sink. */
class TelemetryRedactionTest {

    private class RecordingSink : ObservabilitySink {
        val events = mutableListOf<NonFatalTelemetryEvent>()
        override fun record(event: NonFatalTelemetryEvent) {
            events.add(event)
        }
    }

    private fun logger(sink: ObservabilitySink) = ArenaLogger(sink, DataSanitizer(SyntheticIdMapper(1L)))

    @Test
    fun passingRatingStateToLoggerProducesAnObfuscatedString() {
        val sink = RecordingSink()
        val line = logger(sink).logRatingStateAsSafeEvent(
            ArenaTestFixtures.playerRatingState(mu = 1450.0, sigma = 200.0),
        )
        assertFalse(line.contains("1450"), "rating mean must not be logged")
        assertFalse(line.contains("200"), "rating sigma must not be logged")
        assertTrue(sink.events.single() is NonFatalTelemetryEvent.RatingStateSnapshot)
    }

    @Test
    fun rawThrowableIsNotForwardedAndIsReducedToASafeCategory() {
        val sink = RecordingSink()
        sink.recordNonFatal(
            RuntimeException("uid=aB3xKp9QwErTyUiOpAsDfGhJ28kL rating=1450", IllegalStateException("inner")),
        )
        val event = sink.events.single()
        assertTrue(event is NonFatalTelemetryEvent.SafeError)
        val serialized = event.toString()
        assertFalse(serialized.contains("1450"))
        assertFalse(serialized.contains("uid="))
        assertFalse(serialized.contains("inner"))
    }

    @Test
    fun safeExceptionStripsThrowableCause() {
        val safe = SafeArenaException.from(RuntimeException("outer", RuntimeException("cause with uid=xyz")))
        assertNull(safe.cause)
    }

    @Test
    fun uidAndRatingInThrowableAreRemoved() {
        val safe = SafeArenaException.from(RuntimeException("uid=aB3xKp9QwErTyUiOpAsDfGhJ28kL rating dropped 1450"))
        assertFalse(safe.message.contains("aB3xKp9QwErTyUiOpAsDfGhJ28kL"))
        assertFalse(safe.message.contains("1450"))
    }

    @Test
    fun onlySafeEnumBasedEventTypesAreRecorded() {
        val sink = RecordingSink()
        val log = logger(sink)
        log.log(NonFatalTelemetryEvent.AggregateMatchMetrics("IOS-HZ_120", 45_000L))
        log.logRatingStateAsSafeEvent(ArenaTestFixtures.playerRatingState())
        sink.recordNonFatal(RuntimeException("boom"))
        // Every recorded event is one of the sealed, safe variants — enforced by the API signature.
        assertTrue(
            sink.events.all {
                it is NonFatalTelemetryEvent.AggregateMatchMetrics ||
                    it is NonFatalTelemetryEvent.RatingStateSnapshot ||
                    it is NonFatalTelemetryEvent.SafeError
            },
        )
    }

    @Test
    fun arenaLoggerSafeLineScrubsFreeText() {
        val scrubbed = logger(RecordingSink()).safeLine("contact jane@example.com")
        assertFalse(scrubbed.contains("@"))
    }
}
