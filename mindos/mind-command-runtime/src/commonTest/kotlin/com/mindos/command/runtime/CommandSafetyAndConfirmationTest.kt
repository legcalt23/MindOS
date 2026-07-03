package com.mindos.command.runtime

import com.mindos.action.model.MindAction
import com.mindos.action.model.RequestId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CommandSafetyAndConfirmationTest {
    private val context = CommandSafetyContext("req", "session", "en", CommandSource.TYPED_TEXT)
    private val action = MindAction.OpenSettings(RequestId("req"))

    @Test
    fun defaultNavigationIsLowRiskAndAllowed() {
        assertIs<CommandSafetyDecision.Allow>(DefaultCommandSafetyPolicy().evaluate(action, context))
    }

    @Test
    fun configuredSensitiveActionRequiresConfirmation() {
        val policy = DefaultCommandSafetyPolicy(
            lowRiskActionTypes = emptySet(),
            sensitiveActionTypes = setOf(MindAction.OpenSettings.TYPE),
            confirmationTtlMs = 5_000L,
        )

        val result = assertIs<CommandSafetyDecision.RequireConfirmation>(policy.evaluate(action, context))

        assertEquals(5_000L, result.ttlMs)
    }

    @Test
    fun unclassifiedActionIsDenied() {
        val result = DefaultCommandSafetyPolicy(lowRiskActionTypes = emptySet()).evaluate(action, context)

        assertEquals("unclassified_action", assertIs<CommandSafetyDecision.Deny>(result).reason)
    }

    @Test
    fun confirmationIsBoundToRequestSessionTtlAndSingleUse() = runTest {
        val clock = MutableConfirmationClock(100L)
        val store = testConfirmationStore(clock)
        val issued = assertIs<ConfirmationIssueResult.Issued>(
            store.issue("req", "session", action, context, ttlMs = 50L),
        ).pending

        assertEquals(
            ConfirmationRejectionReason.REQUEST_MISMATCH,
            assertIs<ConfirmationConsumeResult.Rejected>(
                store.consume(issued.token, "other-request", "session"),
            ).reason,
        )
        assertEquals(
            ConfirmationRejectionReason.SESSION_MISMATCH,
            assertIs<ConfirmationConsumeResult.Rejected>(
                store.consume(issued.token, "req", "other-session"),
            ).reason,
        )
        assertIs<ConfirmationConsumeResult.Accepted>(store.consume(issued.token, "req", "session"))
        assertEquals(
            ConfirmationRejectionReason.REPLAY,
            assertIs<ConfirmationConsumeResult.Rejected>(
                store.consume(issued.token, "req", "session"),
            ).reason,
        )
    }

    @Test
    fun expiredConfirmationIsRejectedAndCannotReplay() = runTest {
        val clock = MutableConfirmationClock(100L)
        val store = testConfirmationStore(clock)
        val issued = assertIs<ConfirmationIssueResult.Issued>(
            store.issue("req", "session", action, context, ttlMs = 50L),
        ).pending
        clock.currentMillis = 150L

        assertEquals(
            ConfirmationRejectionReason.EXPIRED,
            assertIs<ConfirmationConsumeResult.Rejected>(
                store.consume(issued.token, "req", "session"),
            ).reason,
        )
        assertEquals(
            ConfirmationRejectionReason.REPLAY,
            assertIs<ConfirmationConsumeResult.Rejected>(
                store.consume(issued.token, "req", "session"),
            ).reason,
        )
    }

    @Test
    fun cleanupPreservesExpiredReasonUntilFirstConsumeAttempt() = runTest {
        val clock = MutableConfirmationClock(100L)
        val store = testConfirmationStore(clock)
        val expired = assertIs<ConfirmationIssueResult.Issued>(
            store.issue("req", "session", action, context, ttlMs = 50L),
        ).pending
        clock.currentMillis = 150L
        store.issue(
            "req-2",
            "session",
            MindAction.OpenSettings(RequestId("req-2")),
            context.copy(requestId = "req-2"),
            ttlMs = 50L,
        )

        assertEquals(
            ConfirmationRejectionReason.EXPIRED,
            assertIs<ConfirmationConsumeResult.Rejected>(
                store.consume(expired.token, "req", "session"),
            ).reason,
        )
        assertEquals(
            ConfirmationRejectionReason.REPLAY,
            assertIs<ConfirmationConsumeResult.Rejected>(
                store.consume(expired.token, "req", "session"),
            ).reason,
        )
    }
}
