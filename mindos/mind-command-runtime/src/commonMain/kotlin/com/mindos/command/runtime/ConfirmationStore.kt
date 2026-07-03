package com.mindos.command.runtime

import com.mindos.action.model.MindAction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.jvm.JvmInline

@JvmInline
value class ConfirmationToken(val value: String) {
    init {
        require(value.isNotBlank()) { "ConfirmationToken must not be blank." }
    }
}

fun interface ConfirmationTokenFactory {
    fun next(): ConfirmationToken
}

fun interface ConfirmationClock {
    fun nowMillis(): Long
}

data class PendingConfirmation(
    val token: ConfirmationToken,
    val requestId: String,
    val sessionId: String,
    val action: MindAction,
    val safetyContext: CommandSafetyContext,
    val expiresAtMillis: Long,
)

sealed interface ConfirmationIssueResult {
    data class Issued(val pending: PendingConfirmation) : ConfirmationIssueResult
    data object TokenCollision : ConfirmationIssueResult
}

enum class ConfirmationRejectionReason {
    NOT_FOUND,
    EXPIRED,
    REPLAY,
    REQUEST_MISMATCH,
    SESSION_MISMATCH,
}

sealed interface ConfirmationConsumeResult {
    data class Accepted(val pending: PendingConfirmation) : ConfirmationConsumeResult
    data class Rejected(val reason: ConfirmationRejectionReason) : ConfirmationConsumeResult
}

interface ConfirmationStore {
    suspend fun issue(
        requestId: String,
        sessionId: String,
        action: MindAction,
        safetyContext: CommandSafetyContext,
        ttlMs: Long,
    ): ConfirmationIssueResult

    suspend fun consume(
        token: ConfirmationToken,
        requestId: String,
        sessionId: String,
    ): ConfirmationConsumeResult
}

class InMemoryConfirmationStore(
    private val tokenFactory: ConfirmationTokenFactory,
    private val clock: ConfirmationClock,
    private val maxPending: Int = 100,
    private val maxConsumedTokens: Int = 100,
) : ConfirmationStore {
    private val mutex = Mutex()
    private val pendingByToken = LinkedHashMap<ConfirmationToken, PendingConfirmation>()
    private val terminalReasons = LinkedHashMap<ConfirmationToken, ConfirmationRejectionReason>()

    init {
        require(maxPending > 0) { "maxPending must be positive." }
        require(maxConsumedTokens > 0) { "maxConsumedTokens must be positive." }
    }

    override suspend fun issue(
        requestId: String,
        sessionId: String,
        action: MindAction,
        safetyContext: CommandSafetyContext,
        ttlMs: Long,
    ): ConfirmationIssueResult = mutex.withLock {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
        require(sessionId.isNotBlank()) { "sessionId must not be blank." }
        require(ttlMs > 0) { "ttlMs must be positive." }
        require(action.requestId.value == requestId) { "Action and confirmation requestId must match." }

        pruneExpired()
        val token = tokenFactory.next()
        if (token in pendingByToken || token in terminalReasons) {
            return@withLock ConfirmationIssueResult.TokenCollision
        }
        while (pendingByToken.size >= maxPending) {
            pendingByToken.remove(pendingByToken.keys.first())
        }
        val pending = PendingConfirmation(
            token = token,
            requestId = requestId,
            sessionId = sessionId,
            action = action,
            safetyContext = safetyContext,
            expiresAtMillis = clock.nowMillis() + ttlMs,
        )
        pendingByToken[token] = pending
        ConfirmationIssueResult.Issued(pending)
    }

    override suspend fun consume(
        token: ConfirmationToken,
        requestId: String,
        sessionId: String,
    ): ConfirmationConsumeResult = mutex.withLock {
        terminalReasons[token]?.let { reason ->
            if (reason == ConfirmationRejectionReason.EXPIRED) {
                rememberTerminal(token, ConfirmationRejectionReason.REPLAY)
            }
            return@withLock ConfirmationConsumeResult.Rejected(reason)
        }
        val pending = pendingByToken[token]
            ?: return@withLock ConfirmationConsumeResult.Rejected(ConfirmationRejectionReason.NOT_FOUND)
        if (pending.requestId != requestId) {
            return@withLock ConfirmationConsumeResult.Rejected(ConfirmationRejectionReason.REQUEST_MISMATCH)
        }
        if (pending.sessionId != sessionId) {
            return@withLock ConfirmationConsumeResult.Rejected(ConfirmationRejectionReason.SESSION_MISMATCH)
        }
        pendingByToken.remove(token)
        if (clock.nowMillis() >= pending.expiresAtMillis) {
            rememberTerminal(token, ConfirmationRejectionReason.REPLAY)
            return@withLock ConfirmationConsumeResult.Rejected(ConfirmationRejectionReason.EXPIRED)
        }
        rememberTerminal(token, ConfirmationRejectionReason.REPLAY)
        ConfirmationConsumeResult.Accepted(pending)
    }

    private fun pruneExpired() {
        val now = clock.nowMillis()
        val expired = pendingByToken.values.filter { now >= it.expiresAtMillis }.map { it.token }
        expired.forEach {
            pendingByToken.remove(it)
            rememberTerminal(it, ConfirmationRejectionReason.EXPIRED)
        }
    }

    private fun rememberTerminal(token: ConfirmationToken, reason: ConfirmationRejectionReason) {
        terminalReasons[token] = reason
        while (terminalReasons.size > maxConsumedTokens) {
            terminalReasons.remove(terminalReasons.keys.first())
        }
    }
}
