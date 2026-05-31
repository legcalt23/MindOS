package com.mindos.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AbuseGuardTest {

    @Test
    fun throttlesSessionCommandFloods() {
        val clock = MutableSecurityClock()
        val guard = InMemoryAbuseGuard(
            ratePolicies = listOf(
                RateLimitPolicy(
                    name = "session_test",
                    scope = AbuseScope.SESSION,
                    maxCommands = 2,
                    windowMillis = 1_000L,
                ),
            ),
            clock = clock,
        )
        val subject = AbuseSubject(sessionId = "s1")

        assertEquals(AbuseDecision.Allow, guard.beforeCommand(subject))
        assertEquals(AbuseDecision.Allow, guard.beforeCommand(subject))
        val denied = guard.beforeCommand(subject)

        assertTrue(denied is AbuseDecision.Deny)
        assertEquals(AbuseAction.THROTTLE, denied.action)
        assertEquals(AbuseScope.SESSION, denied.scope)
    }

    @Test
    fun failurePolicyChallengesRepeatedFailures() {
        val guard = InMemoryAbuseGuard(
            ratePolicies = emptyList(),
            failurePolicy = FailurePolicy(maxFailures = 2, windowMillis = 1_000L),
        )
        val subject = AbuseSubject(sessionId = "s1")

        guard.recordFailure(subject, "no_match")
        guard.recordFailure(subject, "no_match")
        val denied = guard.beforeCommand(subject)

        assertTrue(denied is AbuseDecision.Deny)
        assertEquals(AbuseAction.CHALLENGE, denied.action)
        assertEquals(AbuseScope.SESSION, denied.scope)
    }

    @Test
    fun successClearsFailureHistory() {
        val guard = InMemoryAbuseGuard(
            ratePolicies = emptyList(),
            failurePolicy = FailurePolicy(maxFailures = 2, windowMillis = 1_000L),
        )
        val subject = AbuseSubject(sessionId = "s1")

        guard.recordFailure(subject, "no_match")
        guard.recordSuccess(subject)
        guard.recordFailure(subject, "no_match")

        assertEquals(AbuseDecision.Allow, guard.beforeCommand(subject))
    }

    private class MutableSecurityClock(var now: Long = 0L) : SecurityClock {
        override fun nowMillis(): Long = now
    }
}
