package com.mindos.action.matching

import com.mindos.action.observability.AuditOutcome
import com.mindos.action.observability.MindTelemetryEvent
import com.mindos.action.observability.RecordingAuditSink
import com.mindos.action.observability.RecordingTelemetrySink
import com.mindos.action.understanding.ActionUnderstandingResult
import com.mindos.action.understanding.RejectionReason
import com.mindos.action.understanding.UnderstandingContext
import com.mindos.security.AbuseAction
import com.mindos.security.AbuseDecision
import com.mindos.security.AbuseGuard
import com.mindos.security.AbuseScope
import com.mindos.security.AbuseSubject
import com.mindos.security.SecurityClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActionMatcherSecurityTest {

    @Test
    fun rejectsUnsafeControlInputBeforeMatchingAndAuditsIt() {
        val telemetry = RecordingTelemetrySink()
        val audit = RecordingAuditSink()
        val matcher = TestRegistries.matcher(telemetry = telemetry, audit = audit, clock = SecurityClock { 123L })

        val result = matcher.understand(
            "open\u0001settings",
            UnderstandingContext(requestId = "req-1", abuseSubject = AbuseSubject(sessionId = "s1")),
        )

        assertTrue(result is ActionUnderstandingResult.Rejected)
        assertTrue(result.reason is RejectionReason.UnsupportedInput)
        assertTrue(telemetry.events.any { it is MindTelemetryEvent.SuspiciousInput })
        assertTrue(telemetry.events.any { it is MindTelemetryEvent.InputRejected })
        assertEquals(1, audit.events.size)
        assertEquals("req-1", audit.events.single().requestId)
        assertEquals(AuditOutcome.INPUT_REJECTED, audit.events.single().outcome)
        assertEquals(123L, audit.events.single().timestamp)
    }

    @Test
    fun logsSuspiciousHomoglyphWithoutRejectingValidShape() {
        val telemetry = RecordingTelemetrySink()
        val matcher = TestRegistries.matcher(telemetry = telemetry)

        val result = matcher.understand("open m\u0430rta")

        assertTrue(result is ActionUnderstandingResult.NoMatch)
        val suspicious = telemetry.events.filterIsInstance<MindTelemetryEvent.SuspiciousInput>().single()
        assertTrue("MIXED_SCRIPT" in suspicious.codes)
        assertTrue("HOMOGLYPH_CONFUSABLE" in suspicious.codes)
    }

    @Test
    fun abuseGuardDenialStopsBeforeMatchingAndAuditsRateLimit() {
        val telemetry = RecordingTelemetrySink()
        val audit = RecordingAuditSink()
        val matcher = TestRegistries.matcher(
            telemetry = telemetry,
            audit = audit,
            abuseGuard = DenyingGuard,
        )

        val result = matcher.understand(
            "open settings",
            UnderstandingContext(requestId = "req-2", abuseSubject = AbuseSubject(userId = "u1")),
        )

        assertTrue(result is ActionUnderstandingResult.Rejected)
        assertTrue(telemetry.events.any { it is MindTelemetryEvent.AbuseGuardDenied })
        assertEquals(AuditOutcome.RATE_LIMITED, audit.events.single().outcome)
        assertEquals("abuse_throttle", audit.events.single().errorCode)
    }

    private object DenyingGuard : AbuseGuard {
        override fun beforeCommand(subject: AbuseSubject): AbuseDecision =
            AbuseDecision.Deny(AbuseAction.THROTTLE, AbuseScope.USER, "test_policy")

        override fun recordFailure(subject: AbuseSubject, reason: String) = Unit

        override fun recordSuccess(subject: AbuseSubject) = Unit
    }
}
