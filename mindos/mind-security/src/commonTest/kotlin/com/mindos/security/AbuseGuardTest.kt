package com.mindos.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest

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

    @Test
    fun concurrentAccessDoesNotCorruptRateBuckets() = runTest {
        val guard = InMemoryAbuseGuard(
            ratePolicies = listOf(
                RateLimitPolicy("session_concurrent", AbuseScope.SESSION, maxCommands = 50, windowMillis = 60_000L),
            ),
            clock = MutableSecurityClock(1L),
        )
        val subject = AbuseSubject(sessionId = "s-concurrent")

        val decisions = (1..250).map {
            async { guard.beforeCommand(subject) }
        }.awaitAll()

        assertTrue(decisions.count { it == AbuseDecision.Allow } <= 50)
        assertTrue(decisions.any { it is AbuseDecision.Deny })
    }

    @Test
    fun temporaryBlockExpiresByClock() {
        val clock = MutableSecurityClock()
        val guard = InMemoryAbuseGuard(
            ratePolicies = listOf(
                RateLimitPolicy(
                    name = "session_block",
                    scope = AbuseScope.SESSION,
                    maxCommands = 1,
                    windowMillis = 1_000L,
                    temporaryBlockMillis = 500L,
                ),
            ),
            clock = clock,
        )
        val subject = AbuseSubject(sessionId = "s-block")

        assertEquals(AbuseDecision.Allow, guard.beforeCommand(subject))
        assertTrue(guard.beforeCommand(subject) is AbuseDecision.Deny)
        clock.now = 1_001L

        assertEquals(AbuseDecision.Allow, guard.beforeCommand(subject))
    }

    private class MutableSecurityClock(var now: Long = 0L) : SecurityClock {
        override fun nowMillis(): Long = now
    }
}
