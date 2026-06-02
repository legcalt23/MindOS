package com.mindos.action.execution

import com.mindos.action.model.MindAction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class IdempotencyKey(
    val sessionId: String,
    val requestId: String,
) {
    init {
        require(sessionId.isNotBlank()) { "sessionId must not be blank." }
        require(requestId.isNotBlank()) { "requestId must not be blank." }
    }
}

sealed interface IdempotencyDecision {
    data object Begin : IdempotencyDecision
    data class Replay(val existingFingerprint: String) : IdempotencyDecision
    data class Conflict(val existingFingerprint: String, val incomingFingerprint: String) : IdempotencyDecision
}

interface IdempotencyStore {
    suspend fun tryBegin(key: IdempotencyKey, fingerprint: String, ttlMillis: Long): IdempotencyDecision
    suspend fun markSucceeded(key: IdempotencyKey, resultFingerprint: String)
    suspend fun markFailed(key: IdempotencyKey, failureCode: String)
}

class InMemoryIdempotencyStore(
    private val clock: ExecutionClock = ExecutionClocks.Zero,
    private val maxEntries: Int = DefaultMaxEntries,
) : IdempotencyStore {

    init {
        require(maxEntries > 0) { "maxEntries must be positive." }
    }

    private val mutex = Mutex()
    private val entries = LinkedHashMap<IdempotencyKey, Entry>()

    override suspend fun tryBegin(key: IdempotencyKey, fingerprint: String, ttlMillis: Long): IdempotencyDecision {
        require(ttlMillis > 0L) { "ttlMillis must be positive." }
        return mutex.withLock {
            pruneExpiredLocked()
            val existing = entries[key]
            when {
                existing == null -> {
                    entries[key] = Entry(fingerprint, safeExpiresAt(clock.nowMillis(), ttlMillis), status = Status.STARTED)
                    trimLocked()
                    IdempotencyDecision.Begin
                }
                existing.fingerprint == fingerprint -> IdempotencyDecision.Replay(existing.fingerprint)
                else -> IdempotencyDecision.Conflict(existing.fingerprint, fingerprint)
            }
        }
    }

    override suspend fun markSucceeded(key: IdempotencyKey, resultFingerprint: String) {
        mutex.withLock {
            entries[key]?.let { current ->
                entries[key] = current.copy(resultFingerprint = resultFingerprint, status = Status.SUCCEEDED)
            }
        }
    }

    override suspend fun markFailed(key: IdempotencyKey, failureCode: String) {
        mutex.withLock {
            entries[key]?.let { current ->
                entries[key] = current.copy(resultFingerprint = failureCode, status = Status.FAILED)
            }
        }
    }

    suspend fun size(): Int = mutex.withLock {
        pruneExpiredLocked()
        entries.size
    }

    private fun pruneExpiredLocked() {
        val now = clock.nowMillis()
        val expired = entries.filterValues { it.expiresAtMillis <= now }.keys
        expired.forEach { entries.remove(it) }
    }

    private fun trimLocked() {
        while (entries.size > maxEntries) {
            val oldest = entries.keys.firstOrNull() ?: break
            entries.remove(oldest)
        }
    }

    private fun safeExpiresAt(now: Long, ttl: Long): Long =
        if (Long.MAX_VALUE - now < ttl) Long.MAX_VALUE else now + ttl

    private data class Entry(
        val fingerprint: String,
        val expiresAtMillis: Long,
        val resultFingerprint: String? = null,
        val status: Status,
    )

    private enum class Status { STARTED, SUCCEEDED, FAILED }

    companion object {
        const val DefaultMaxEntries: Int = 10_000
    }
}

class IdempotentActionExecutor(
    private val delegate: ActionExecutor,
    private val store: IdempotencyStore,
    private val ttlMillis: Long = DefaultTtlMillis,
    private val defaultSessionId: String = "anonymous",
) : ActionExecutor {

    init {
        require(ttlMillis > 0L) { "ttlMillis must be positive." }
        require(defaultSessionId.isNotBlank()) { "defaultSessionId must not be blank." }
    }

    override suspend fun execute(action: MindAction, context: ExecutionContext): ExecutionResult {
        val sessionId = context.sessionId?.takeIf { it.isNotBlank() } ?: defaultSessionId
        val key = IdempotencyKey(sessionId = sessionId, requestId = action.requestId.value)
        val fingerprint = action.idempotencyFingerprint()
        return when (val decision = store.tryBegin(key, fingerprint, ttlMillis)) {
            IdempotencyDecision.Begin -> {
                val result = delegate.execute(action, context)
                when (result) {
                    is ExecutionResult.Executed -> store.markSucceeded(key, result.command.commandType)
                    is ExecutionResult.Rejected -> store.markFailed(key, "validation")
                    is ExecutionResult.Failed -> store.markFailed(key, result.failure.idempotencyFailureCode())
                }
                result
            }
            is IdempotencyDecision.Replay -> ExecutionResult.Failed(
                ExecutionFailure.IdempotentReplay("request already processed"),
            )
            is IdempotencyDecision.Conflict -> ExecutionResult.Failed(
                ExecutionFailure.ReplayRejected("requestId reused with a different action fingerprint"),
            )
        }
    }

    companion object {
        const val DefaultTtlMillis: Long = 5 * 60 * 1_000L
    }
}

internal fun MindAction.idempotencyFingerprint(): String =
    when (this) {
        is MindAction.OpenProfile -> "${actionType}:${stableHash(userRef)}"
        is MindAction.OpenStory -> "${actionType}:${stableHash(storyRef)}"
        is MindAction.OpenStoryOfUser -> "${actionType}:${stableHash(userRef)}"
        is MindAction.OpenChat -> "${actionType}:${stableHash(chatRef)}"
        is MindAction.OpenMessages,
        is MindAction.OpenSettings,
        -> actionType
    }

internal fun ExecutionFailure.idempotencyFailureCode(): String =
    when (this) {
        is ExecutionFailure.ResolutionFailed -> "resolution_failed"
        is ExecutionFailure.NavigationRejected -> "navigation_rejected"
        is ExecutionFailure.NavigationFailed -> "navigation_failed"
        is ExecutionFailure.UnsupportedAction -> "unsupported_action"
        is ExecutionFailure.IdempotentReplay -> "idempotent_replay"
        is ExecutionFailure.ReplayRejected -> "replay_rejected"
    }

private fun stableHash(value: String): String {
    var hash = FnvOffset
    for (ch in value) {
        hash = hash xor ch.code.toUInt()
        hash *= FnvPrime
    }
    return hash.toString(16).padStart(8, '0')
}

private val FnvOffset: UInt = 0x811C9DC5u
private val FnvPrime: UInt = 0x01000193u
