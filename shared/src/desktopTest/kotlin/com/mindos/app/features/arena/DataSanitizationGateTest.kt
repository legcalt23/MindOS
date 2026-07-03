package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.UuidV4
import com.mindos.app.features.arena.sanitization.DataSanitizer
import com.mindos.app.features.arena.sanitization.SafeArenaException
import com.mindos.app.features.arena.sanitization.SyntheticIdMapper
import com.mindos.app.features.arena.simulation.SimulationConfig
import com.mindos.app.features.arena.simulation.SimulationHarness
import com.mindos.app.features.arena.simulation.SimulationSeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Phase 07.5 / Task 8: The Data Sanitization Gate — the final boundary before any output. */
class DataSanitizationGateTest {

    private val rawUid = "aB3xKp9QwErTyUiOpAsDfGhJ28kL" // 28-char Firebase-style UID

    private fun sanitizer() = DataSanitizer(SyntheticIdMapper(seed = 1L))

    @Test
    fun simulationOutputContainsZeroTrueUids() {
        val result = SimulationHarness().run(
            SimulationConfig(SimulationSeed(5L), populationSize = 50, matchCount = 500),
        )
        val text = "run=${result.runId.value} ids=${result.syntheticProfileIds} leaked=$rawUid"
        val scrubbed = sanitizer().scrub(text, knownRawIds = setOf(rawUid))
        assertFalse(scrubbed.contains(rawUid), "true UID must not survive sanitization")
        assertTrue(result.syntheticProfileIds.all { UuidV4.isValid(it) })
    }

    @Test
    fun emailLikeStringsAreRedacted() {
        val scrubbed = sanitizer().scrub("reach me at jane.doe@example.com please")
        assertFalse(scrubbed.contains("@"))
        assertFalse(scrubbed.contains("example.com"))
    }

    @Test
    fun ipLikeStringsAreRedacted() {
        val scrubbed = sanitizer().scrub("client from 192.168.10.254 connected")
        assertFalse(scrubbed.contains("192.168.10.254"))
    }

    @Test
    fun deviceIdLikeTokensAreRedacted() {
        val deviceId = "ABCD1234567890ABCDEF1234"
        val scrubbed = sanitizer().scrub("device=$deviceId")
        assertFalse(scrubbed.contains(deviceId))
    }

    @Test
    fun exceptionWithUidInMessageIsScrubbedBeforeReachingCaller() {
        val safe = SafeArenaException.from(RuntimeException("failure for uid=$rawUid"))
        assertFalse(safe.message.contains(rawUid))
        assertEquals("ArenaError:UNKNOWN", safe.message)
    }

    @Test
    fun exceptionWithRatingValueInMessageIsScrubbed() {
        val safe = SafeArenaException.from(IllegalStateException("rating dropped to 1450 for SYNTH"))
        assertFalse(safe.message.contains("1450"))
    }

    @Test
    fun safeExceptionStripsCauseButKeepsCategoryAndStackTrace() {
        val withCause = RuntimeException("outer", IllegalStateException("uid=$rawUid rating=1450"))
        val safe = SafeArenaException.from(withCause)
        assertNull(safe.cause)
        assertTrue(safe.stackTrace.isNotEmpty(), "stack trace should remain usable for debugging")
        assertEquals("ArenaError:UNKNOWN", safe.message)
    }

    @Test
    fun noCognitiveProfilingPhrasesAreEmitted() {
        val text = "report: " + DataSanitizer.PROHIBITED_PHRASES.joinToString(", ")
        val scrubbed = sanitizer().scrub(text)
        DataSanitizer.PROHIBITED_PHRASES.forEach { phrase ->
            assertFalse(scrubbed.contains(phrase, ignoreCase = true), "phrase leaked: $phrase")
        }
    }

    @Test
    fun mappedIdsAreSyntheticUuidV4AndStable() {
        val mapper = SyntheticIdMapper(seed = 2L)
        val mapped = mapper.map(rawUid)
        assertTrue(UuidV4.isValid(mapped))
        assertEquals(mapped, mapper.map(rawUid), "same raw id must map to same synthetic id")
    }

    @Test
    fun sanitizeWrapsContentThatClearedTheGate() {
        val output = sanitizer().sanitize("contact jane@example.com from 10.0.0.1")
        assertFalse(output.content.contains("@"))
        assertFalse(output.content.contains("10.0.0.1"))
    }
}
