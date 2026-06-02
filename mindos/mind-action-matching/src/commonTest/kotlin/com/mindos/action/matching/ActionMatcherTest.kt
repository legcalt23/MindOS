package com.mindos.action.matching

import com.mindos.action.observability.MindTelemetryEvent
import com.mindos.action.observability.MetricTags
import com.mindos.action.observability.OperationMetric
import com.mindos.action.observability.OutcomeFailure
import com.mindos.action.observability.RecordingMetricSink
import com.mindos.action.observability.RecordingStructuredLogSink
import com.mindos.action.observability.RecordingTelemetrySink
import com.mindos.action.pattern.registrySource
import com.mindos.action.understanding.ActionUnderstandingResult
import com.mindos.action.understanding.RejectionReason
import com.mindos.action.understanding.SlotName
import com.mindos.action.understanding.UnderstandingContext
import com.mindos.security.AbuseSubject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActionMatcherTest {

    private val matcher = TestRegistries.matcher()

    private fun understood(input: String, ctx: UnderstandingContext = UnderstandingContext()) =
        matcher.understand(input, ctx) as ActionUnderstandingResult.Understood

    @Test
    fun exactMatchResolvesIntent() {
        val r = understood("open settings")
        assertEquals("open_settings", r.intent.id.value)
        assertTrue(r.entities.isEmpty())
    }

    @Test
    fun exactBeatsParametric() {
        // "open settings" matches both the exact phrase and "open {target:username}".
        assertEquals("open_settings", understood("open settings").intent.id.value)
    }

    @Test
    fun shortExactMatch() {
        assertEquals("open_settings", understood("settings").intent.id.value)
    }

    @Test
    fun aliasMatchWithLocale() {
        val r = understood("ajustes", UnderstandingContext(localeTag = "es"))
        assertEquals("open_settings", r.intent.id.value)
    }

    @Test
    fun aliasLocaleAgnosticFallback() {
        // "home" is declared under locale "und"; it resolves regardless of request locale.
        assertEquals("open_home", understood("home", UnderstandingContext(localeTag = "es")).intent.id.value)
    }

    @Test
    fun parametricUsernameExtractsEntity() {
        val r = understood("open ana")
        assertEquals("open_profile", r.intent.id.value)
        assertEquals("ana", r.entities[SlotName("target")]?.value)
    }

    @Test
    fun parametricUsernameAcceptsSigil() {
        val r = understood("open @Ana")
        assertEquals("open_profile", r.intent.id.value)
        assertEquals("ana", r.entities[SlotName("target")]?.value)
    }

    @Test
    fun parametricIdPreservesCase() {
        val r = understood("open story aB3xZ9kQ")
        assertEquals("open_story", r.intent.id.value)
        assertEquals("aB3xZ9kQ", r.entities[SlotName("storyId")]?.value)
    }

    @Test
    fun greedyFreeTextCapturesRemainder() {
        val r = understood("message ana hola que tal")
        assertEquals("send_message", r.intent.id.value)
        assertEquals("ana", r.entities[SlotName("user")]?.value)
        assertEquals("hola que tal", r.entities[SlotName("text")]?.value)
    }

    @Test
    fun invalidEntityYieldsNoFalsePositive() {
        // "$$$" is not a valid username; the only structural candidate is rejected → NoMatch.
        val r = matcher.understand("open \$\$\$")
        assertTrue(r is ActionUnderstandingResult.NoMatch, "expected NoMatch, got $r")
    }

    @Test
    fun unknownInputIsNoMatch() {
        assertTrue(matcher.understand("florble") is ActionUnderstandingResult.NoMatch)
    }

    @Test
    fun emptyInputIsRejected() {
        val r = matcher.understand("")
        assertTrue((r as ActionUnderstandingResult.Rejected).reason is RejectionReason.EmptyInput)
    }

    @Test
    fun blankInputIsRejected() {
        assertTrue((matcher.understand("    ") as ActionUnderstandingResult.Rejected).reason is RejectionReason.EmptyInput)
    }

    @Test
    fun tooLongInputIsRejected() {
        val r = matcher.understand("open settings", UnderstandingContext(maxInputLength = 3))
        assertTrue((r as ActionUnderstandingResult.Rejected).reason is RejectionReason.InputTooLong)
    }

    @Test
    fun explainModePopulatesTraceOnlyWhenRequested() {
        assertNull(understood("open settings").trace)
        val r = understood("open settings", UnderstandingContext(explain = true))
        assertNotNull(r.trace)
        assertTrue(r.trace!!.evaluatedCandidates.isNotEmpty())
    }

    @Test
    fun explainTraceRedactsRawInputAndEntityValues() {
        val r = understood("open marta", UnderstandingContext(explain = true))
        val trace = r.trace!!

        assertTrue("marta" !in trace.rawInput)
        assertTrue("marta" !in trace.normalizedText)
        assertTrue(trace.extractedEntities.none { it.value.contains("marta") })
        assertTrue(trace.extractedEntities.all { it.value.startsWith("<redacted:") })
    }

    @Test
    fun priorityCanOutrankExactness() {
        val source = registrySource {
            exact("p_exact", "open settings", intent = "open_settings", priority = 0)
            parametric("p_param", "open {target:username}", intent = "open_profile", priority = 100)
        }
        val m = TestRegistries.matcher(source)
        // parametric priority 100 > exact priority 0 → priority dominates exactness.
        assertEquals("open_profile", (m.understand("open settings") as ActionUnderstandingResult.Understood).intent.id.value)
    }

    @Test
    fun telemetryEmitsExactHitAndLatency() {
        val sink = RecordingTelemetrySink()
        TestRegistries.matcher(telemetry = sink).understand("open settings")
        assertTrue(sink.events.any { it is MindTelemetryEvent.ExactHit })
        assertTrue(sink.events.any { it is MindTelemetryEvent.MatchLatency })
    }

    @Test
    fun telemetryEmitsEntityRejectedAndNoMatch() {
        val sink = RecordingTelemetrySink()
        TestRegistries.matcher(telemetry = sink).understand("open \$\$\$")
        assertTrue(sink.events.any { it is MindTelemetryEvent.EntityRejected })
        assertTrue(sink.events.any { it is MindTelemetryEvent.NoMatch })
        assertTrue(sink.events.filterIsInstance<MindTelemetryEvent.NoMatch>().single().toString().contains("\$\$\$").not())
    }

    @Test
    fun operationsMetricsAndLogsAreStructuredAndTagged() {
        val metrics = RecordingMetricSink()
        val logs = RecordingStructuredLogSink()
        val tags = MetricTags(appVersion = "1.2.3", platform = "jvm", commandType = "unknown")

        TestRegistries.matcher(metrics = metrics, logs = logs, metricTags = tags, clock = { 123L })
            .understand(
                "not a command",
                UnderstandingContext(
                    requestId = "req-7",
                    abuseSubject = AbuseSubject(sessionId = "sess-7"),
                ),
            )

        assertTrue(metrics.samples.any {
            it.metric == OperationMetric.MATCH_RATE &&
                it.tags.appVersion == "1.2.3" &&
                it.tags.platform == "jvm" &&
                it.tags.commandType == "unknown" &&
                it.tags.attributes["outcome"] == OutcomeFailure
        })
        assertTrue(metrics.samples.any { it.metric == OperationMetric.UNKNOWN_COMMAND_RATE })
        assertEquals("req-7", logs.events.single().requestId)
        assertEquals("sess-7", logs.events.single().sessionId)
        assertEquals("understanding", logs.events.single().action)
        assertEquals("no_match", logs.events.single().outcome)
    }
}
