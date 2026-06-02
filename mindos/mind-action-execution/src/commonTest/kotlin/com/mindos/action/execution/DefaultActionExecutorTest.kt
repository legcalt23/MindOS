package com.mindos.action.execution

import com.mindos.action.model.MindAction
import com.mindos.action.model.RequestId
import com.mindos.action.navigation.MindNavigationCommand
import com.mindos.action.navigation.NavigationOutcome
import com.mindos.action.observability.AuditOutcome
import com.mindos.action.observability.MetricTags
import com.mindos.action.observability.MindTelemetryEvent
import com.mindos.action.observability.OperationMetric
import com.mindos.action.observability.OutcomeSuccess
import com.mindos.action.observability.RecordingAuditSink
import com.mindos.action.observability.RecordingMetricSink
import com.mindos.action.observability.RecordingStructuredLogSink
import com.mindos.action.observability.RecordingTelemetrySink
import com.mindos.action.validation.DefaultActionValidator
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultActionExecutorTest {

    private val id = RequestId("req-1")

    private fun executor(
        coordinator: FakeCoordinator,
        telemetry: RecordingTelemetrySink = RecordingTelemetrySink(),
        audit: RecordingAuditSink = RecordingAuditSink(),
        clock: ExecutionClock = ExecutionClocks.Zero,
        metrics: RecordingMetricSink = RecordingMetricSink(),
        logs: RecordingStructuredLogSink = RecordingStructuredLogSink(),
        metricTags: MetricTags = MetricTags.Default,
    ) = DefaultActionExecutor(DefaultActionValidator(), sampleResolvers(), coordinator, telemetry, audit, clock, metrics, logs, metricTags)

    @Test
    fun happyPathResolvesAndNavigates() = runTest {
        val coordinator = FakeCoordinator()
        val audit = RecordingAuditSink()
        val telemetry = RecordingTelemetrySink()
        val result = executor(coordinator, telemetry, audit).execute(MindAction.OpenProfile(id, "ana"))

        assertEquals(MindNavigationCommand.OpenProfile("u_ana"), (result as ExecutionResult.Executed).command)
        assertEquals(1, coordinator.calls)
        assertEquals(MindNavigationCommand.OpenProfile("u_ana"), coordinator.lastCommand)
        assertTrue(audit.events.any { it.outcome == AuditOutcome.EXECUTED })
        assertTrue(telemetry.events.any { it is MindTelemetryEvent.ActionExecuted })
        assertTrue(telemetry.events.any { it is MindTelemetryEvent.ResolverSuccess })
        assertTrue(telemetry.events.any { it is MindTelemetryEvent.ExecutionLatency })
    }

    @Test
    fun referencelessActionExecutesWithoutResolver() = runTest {
        val coordinator = FakeCoordinator()
        val result = executor(coordinator).execute(MindAction.OpenSettings(id))
        assertEquals(MindNavigationCommand.OpenSettings, (result as ExecutionResult.Executed).command)
        assertEquals(MindNavigationCommand.OpenSettings, coordinator.lastCommand)
    }

    @Test
    fun storyOfUserResolvesUsernameThenCurrentStoryBeforeNavigating() = runTest {
        val coordinator = FakeCoordinator()
        val result = executor(coordinator).execute(MindAction.OpenStoryOfUser(id, "ana"))

        assertEquals(MindNavigationCommand.OpenStory("st_ana_current"), (result as ExecutionResult.Executed).command)
        assertEquals(MindNavigationCommand.OpenStory("st_ana_current"), coordinator.lastCommand)
    }

    @Test
    fun storyOfUserFailsBeforeNavigationWhenCurrentStoryIsMissing() = runTest {
        val coordinator = FakeCoordinator()
        val result = executor(coordinator).execute(MindAction.OpenStoryOfUser(id, "bob"))

        assertTrue((result as ExecutionResult.Failed).failure is ExecutionFailure.ResolutionFailed)
        assertEquals(0, coordinator.calls)
    }

    @Test
    fun invalidActionIsRejectedAndNeverNavigates() = runTest {
        val coordinator = FakeCoordinator()
        val audit = RecordingAuditSink()
        val result = executor(coordinator, audit = audit).execute(MindAction.OpenProfile(id, "  "))
        assertTrue(result is ExecutionResult.Rejected)
        assertEquals(0, coordinator.calls) // invalid actions must never execute
        assertTrue(audit.events.any { it.outcome == AuditOutcome.VALIDATION_REJECTED })
    }

    @Test
    fun unresolvedReferenceFailsAndNeverNavigates() = runTest {
        val coordinator = FakeCoordinator()
        val audit = RecordingAuditSink()
        val telemetry = RecordingTelemetrySink()
        val result = executor(coordinator, telemetry, audit).execute(MindAction.OpenProfile(id, "ghost"))
        assertTrue((result as ExecutionResult.Failed).failure is ExecutionFailure.ResolutionFailed)
        assertEquals(0, coordinator.calls)
        assertTrue(audit.events.any { it.outcome == AuditOutcome.RESOLUTION_FAILED })
        assertTrue(telemetry.events.any { it is MindTelemetryEvent.ResolverFailed })
    }

    @Test
    fun navigationRejectionIsTypedFailure() = runTest {
        val coordinator = FakeCoordinator(NavigationOutcome.Rejected("denied"))
        val audit = RecordingAuditSink()
        val result = executor(coordinator, audit = audit).execute(MindAction.OpenSettings(id))
        assertTrue((result as ExecutionResult.Failed).failure is ExecutionFailure.NavigationRejected)
        assertTrue(audit.events.any { it.outcome == AuditOutcome.NAVIGATION_FAILED })
    }

    @Test
    fun navigationErrorIsTypedFailure() = runTest {
        val coordinator = FakeCoordinator(NavigationOutcome.Failed("boom"))
        val result = executor(coordinator).execute(MindAction.OpenSettings(id))
        assertTrue((result as ExecutionResult.Failed).failure is ExecutionFailure.NavigationFailed)
    }

    @Test
    fun auditTimestampComesFromInjectedClock() = runTest {
        val coordinator = FakeCoordinator()
        val audit = RecordingAuditSink()
        executor(coordinator, audit = audit, clock = { 42L }).execute(MindAction.OpenSettings(id))
        assertEquals(42L, audit.events.single().timestamp)
        assertEquals("req-1", audit.events.single().requestId)
    }

    @Test
    fun executionIsDeterministic() = runTest {
        val a = executor(FakeCoordinator()).execute(MindAction.OpenProfile(id, "ana"))
        val b = executor(FakeCoordinator()).execute(MindAction.OpenProfile(id, "ana"))
        assertEquals(a, b)
    }

    @Test
    fun operationsMetricsAndLogsAreRecordedForExecution() = runTest {
        val coordinator = FakeCoordinator()
        val metrics = RecordingMetricSink()
        val logs = RecordingStructuredLogSink()
        val tags = MetricTags(appVersion = "1.2.3", platform = "jvm", commandType = "unknown")

        executor(coordinator, metrics = metrics, logs = logs, metricTags = tags, clock = { 321L })
            .execute(MindAction.OpenSettings(id), ExecutionContext(sessionId = "sess-1"))

        assertTrue(metrics.samples.any {
            it.metric == OperationMetric.ACTION_EXECUTION_RATE &&
                it.tags.appVersion == "1.2.3" &&
                it.tags.platform == "jvm" &&
                it.tags.commandType == "open_settings" &&
                it.tags.attributes["outcome"] == OutcomeSuccess
        })
        assertTrue(metrics.samples.any { it.metric == OperationMetric.LATENCY_SAMPLE })
        assertEquals("req-1", logs.events.single().requestId)
        assertEquals("sess-1", logs.events.single().sessionId)
        assertEquals("open_settings", logs.events.single().action)
        assertEquals("executed", logs.events.single().outcome)
    }

    @Test
    fun idempotencyReplayDoesNotNavigateTwice() = runTest {
        val coordinator = FakeCoordinator()
        val store = InMemoryIdempotencyStore(clock = { 1L })
        val executor = DefaultActionExecutor(
            DefaultActionValidator(),
            sampleResolvers(),
            coordinator,
            idempotencyStore = store,
        )

        val first = executor.execute(MindAction.OpenSettings(RequestId("req-replay")), ExecutionContext(sessionId = "sess"))
        val second = executor.execute(MindAction.OpenSettings(RequestId("req-replay")), ExecutionContext(sessionId = "sess"))

        assertTrue(first is ExecutionResult.Executed)
        assertTrue((second as ExecutionResult.Failed).failure is ExecutionFailure.IdempotentReplay)
        assertEquals(1, coordinator.calls)
    }

    @Test
    fun idempotencyRejectsSameRequestIdWithDifferentAction() = runTest {
        val coordinator = FakeCoordinator()
        val executor = DefaultActionExecutor(
            DefaultActionValidator(),
            sampleResolvers(),
            coordinator,
            idempotencyStore = InMemoryIdempotencyStore(clock = { 1L }),
        )

        executor.execute(MindAction.OpenSettings(RequestId("req-conflict")), ExecutionContext(sessionId = "sess"))
        val conflict = executor.execute(MindAction.OpenMessages(RequestId("req-conflict")), ExecutionContext(sessionId = "sess"))

        assertTrue((conflict as ExecutionResult.Failed).failure is ExecutionFailure.ReplayRejected)
        assertEquals(1, coordinator.calls)
    }
}
