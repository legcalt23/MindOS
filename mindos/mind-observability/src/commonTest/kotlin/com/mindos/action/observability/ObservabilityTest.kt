package com.mindos.action.observability

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObservabilityTest {

    @Test
    fun noOpSinkDiscardsEvents() {
        // Just ensure it does not throw and accepts any event.
        NoOpMindTelemetrySink.record(MindTelemetryEvent.NoMatch)
        NoOpMindTelemetrySink.record(MindTelemetryEvent.MatchLatency(42))
    }

    @Test
    fun recordingSinkRetainsOrderedEvents() {
        val sink = RecordingTelemetrySink()
        sink.record(MindTelemetryEvent.RegistryCompiled(3, 2))
        sink.record(MindTelemetryEvent.ExactHit("open_settings"))
        sink.record(MindTelemetryEvent.MatchLatency(10))
        assertEquals(3, sink.events.size)
        assertTrue(sink.events[0] is MindTelemetryEvent.RegistryCompiled)
        assertEquals("open_settings", (sink.events[1] as MindTelemetryEvent.ExactHit).intentId)
    }

    @Test
    fun recordingSinkClears() {
        val sink = RecordingTelemetrySink()
        sink.record(MindTelemetryEvent.NoMatch)
        sink.clear()
        assertTrue(sink.events.isEmpty())
    }

    @Test
    fun traceBuilderAccumulatesAndBuilds() {
        val trace = MatchTraceBuilder("Open Settings", "open settings", "en")
            .stage(MatchStage.RANKING)
            .candidate(CandidateTrace("p1", "open_settings", "prio=0", won = true))
            .extracted(EntityTrace("target", "username", "ana"))
            .winner("open_settings")
            .build("understood")

        assertTrue(trace.normalizedText.startsWith("<redacted:normalized_text:"))
        assertTrue(trace.rawInput.startsWith("<redacted:raw_input:"))
        assertTrue(trace.extractedEntities.single().value.startsWith("<redacted:username:"))
        assertEquals(MatchStage.RANKING, trace.stageReached)
        assertEquals(1, trace.evaluatedCandidates.size)
        assertEquals("open_settings", trace.winnerIntentId)
        assertEquals("understood", trace.outcome)
        assertTrue(trace.rejectedEntities.isEmpty())
    }

    @Test
    fun traceBuilderRecordsRejections() {
        val trace = MatchTraceBuilder("open $$$", "open $$$", "und")
            .rejected(EntityTrace("target", "username", "$$$", "shape mismatch"))
            .winner(null)
            .build("no_match")
        assertEquals(1, trace.rejectedEntities.size)
        assertNull(trace.winnerIntentId)
    }

    @Test
    fun metricCatalogContainsZip06CoreMetricsWithRequiredTags() {
        val names = MetricCatalog.CoreMetrics.map { it.metric }.toSet()

        assertTrue(OperationMetric.MATCH_RATE in names)
        assertTrue(OperationMetric.UNKNOWN_COMMAND_RATE in names)
        assertTrue(OperationMetric.INTENT_RESOLUTION_RATE in names)
        assertTrue(OperationMetric.ENTITY_RESOLUTION_RATE in names)
        assertTrue(OperationMetric.ACTION_EXECUTION_RATE in names)
        assertTrue(OperationMetric.VALIDATION_FAILURE_RATE in names)
        assertTrue(OperationMetric.P50_LATENCY in names)
        assertTrue(OperationMetric.P95_LATENCY in names)
        assertTrue(OperationMetric.P99_LATENCY in names)
        assertTrue(MetricCatalog.CoreMetrics.all { it.requiredTags == RequiredMetricTags })
    }

    @Test
    fun metricTagsRejectSensitiveAttributeKeys() {
        assertFailsWith<IllegalArgumentException> {
            MetricTags.Default.withAttribute("raw_input", "open ana")
        }
    }

    @Test
    fun structuredLogsRequireCorrelationFieldsAndRejectSensitiveData() {
        assertFailsWith<IllegalArgumentException> {
            StructuredLogEvent(
                requestId = "",
                sessionId = "s1",
                timestampMillis = 1L,
                action = "understanding",
                outcome = "no_match",
                latencyMs = 1L,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            StructuredLogEvent(
                requestId = "r1",
                sessionId = "s1",
                timestampMillis = 1L,
                action = "understanding",
                outcome = "no_match",
                latencyMs = 1L,
                attributes = mapOf("normalized_text" to "open ana"),
            )
        }
    }

    @Test
    fun metricAggregatorBuildsHealthSnapshot() {
        val aggregator = InMemoryMetricAggregator()
        val tags = MetricTags(appVersion = "1.0.0", platform = "jvm", commandType = "open_profile")

        aggregator.record(MetricSample(OperationMetric.MATCH_RATE, 1.0, tags.withOutcome(OutcomeSuccess), 10L))
        aggregator.record(MetricSample(OperationMetric.ACTION_EXECUTION_RATE, 1.0, tags.withOutcome(OutcomeFailure), 10L))
        aggregator.record(MetricSample(OperationMetric.VALIDATION_FAILURE_RATE, 1.0, tags.withOutcome(OutcomeRejected), 10L))
        aggregator.record(MetricSample(OperationMetric.LATENCY_SAMPLE, 25.0, tags, 10L))

        val snapshot = aggregator.snapshot()

        assertEquals(1L, snapshot.matchAttempts)
        assertEquals(1.0, snapshot.matchRate)
        assertEquals(0.0, snapshot.actionExecutionRate)
        assertEquals(1.0, snapshot.validationFailureRate)
        assertEquals(25.0, snapshot.p95LatencyMs)
    }

    @Test
    fun sloAndAlertPoliciesEvaluateOperationalState() {
        val snapshot = OperationHealthSnapshot(
            routerAvailable = false,
            actionExecutionAttempts = 1000,
            actionExecutionSuccesses = 990,
            latencySamplesMs = listOf(1.0, 5.0, 30.0, 35.0),
        )

        val sloResults = SloCatalog.evaluate(snapshot)
        val alerts = AlertPolicy.evaluate(snapshot)

        assertTrue(sloResults.any { it.target == SloCatalog.Availability && !it.met })
        assertTrue(alerts.any { it.code == AlertCode.ROUTER_UNAVAILABLE && it.severity == AlertSeverity.CRITICAL })
        assertTrue(alerts.any { it.code == AlertCode.EXECUTION_FAILURE_SPIKE })
    }

    @Test
    fun dashboardsAndRunbooksExposeOperationsViews() {
        assertEquals(3, DashboardCatalog.All.size)
        assertTrue(DashboardCatalog.Operations.views.contains(DashboardView.LATENCY))
        assertTrue(ProductionRunbook.All.any { it.name == "rollback" })
        assertTrue(ProductionRunbook.IncidentResponse.steps.contains("postmortem"))
    }
}
