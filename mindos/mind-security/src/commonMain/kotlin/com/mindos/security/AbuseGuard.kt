package com.mindos.security

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.serialization.Serializable

@Serializable
enum class AbuseScope { USER, DEVICE, SESSION }

@Serializable
enum class AbuseAction { THROTTLE, CHALLENGE, BLOCK }

@Serializable
data class AbuseSubject(
    val userId: String? = null,
    val deviceId: String? = null,
    val sessionId: String? = null,
) {
    companion object {
        val Anonymous = AbuseSubject()
    }
}

@Serializable
data class RateLimitPolicy(
    val name: String,
    val scope: AbuseScope,
    val maxCommands: Int,
    val windowMillis: Long,
    val action: AbuseAction = AbuseAction.THROTTLE,
    val temporaryBlockMillis: Long = 0L,
) {
    init {
        require(name.isNotBlank()) { "RateLimitPolicy.name must not be blank." }
        require(maxCommands > 0) { "maxCommands must be positive." }
        require(windowMillis > 0L) { "windowMillis must be positive." }
        require(temporaryBlockMillis >= 0L) { "temporaryBlockMillis cannot be negative." }
    }
}

@Serializable
data class FailurePolicy(
    val name: String = "repeated_failures",
    val scope: AbuseScope = AbuseScope.SESSION,
    val maxFailures: Int = 5,
    val windowMillis: Long = 60_000L,
    val action: AbuseAction = AbuseAction.CHALLENGE,
    val temporaryBlockMillis: Long = 0L,
) {
    init {
        require(name.isNotBlank()) { "FailurePolicy.name must not be blank." }
        require(maxFailures > 0) { "maxFailures must be positive." }
        require(windowMillis > 0L) { "windowMillis must be positive." }
        require(temporaryBlockMillis >= 0L) { "temporaryBlockMillis cannot be negative." }
    }
}

sealed interface AbuseDecision {
    data object Allow : AbuseDecision

    data class Deny(
        val action: AbuseAction,
        val scope: AbuseScope,
        val policyName: String,
        val retryAfterMillis: Long? = null,
    ) : AbuseDecision
}

fun interface SecurityClock {
    fun nowMillis(): Long
}

object SecurityClocks {
    val Zero = SecurityClock { 0L }
}

interface AbuseGuard {
    fun beforeCommand(subject: AbuseSubject): AbuseDecision
    fun recordFailure(subject: AbuseSubject, reason: String)
    fun recordSuccess(subject: AbuseSubject)
}

object NoOpAbuseGuard : AbuseGuard {
    override fun beforeCommand(subject: AbuseSubject): AbuseDecision = AbuseDecision.Allow
    override fun recordFailure(subject: AbuseSubject, reason: String) = Unit
    override fun recordSuccess(subject: AbuseSubject) = Unit
}

@OptIn(ExperimentalAtomicApi::class)
class InMemoryAbuseGuard(
    private val ratePolicies: List<RateLimitPolicy> = DefaultRatePolicies,
    private val failurePolicy: FailurePolicy = FailurePolicy(),
    private val clock: SecurityClock = SecurityClocks.Zero,
    private val maxTrackedBuckets: Int = DefaultMaxTrackedBuckets,
) : AbuseGuard {

    init {
        require(maxTrackedBuckets > 0) { "maxTrackedBuckets must be positive." }
    }

    private val state = AtomicReference(GuardState())

    override fun beforeCommand(subject: AbuseSubject): AbuseDecision {
        val now = clock.nowMillis()
        return updateState { current ->
            var next = current.pruneExpiredBlocks(now)
            val failureKey = keyFor(failurePolicy.scope, subject)
            if (failureKey != null) {
                val bucketKey = BucketKey(failurePolicy.name, failurePolicy.scope, failureKey)
                next.blockedUntil[bucketKey]?.let { until ->
                    return@updateState next to AbuseDecision.Deny(
                        AbuseAction.BLOCK,
                        failurePolicy.scope,
                        failurePolicy.name,
                        until - now,
                    )
                }
                val failures = next.failureBuckets[bucketKey].orEmpty().pruneBefore(now - failurePolicy.windowMillis)
                next = next.copy(failureBuckets = next.failureBuckets.putOrRemove(bucketKey, failures))
                if (failures.size >= failurePolicy.maxFailures) {
                    return@updateState next to AbuseDecision.Deny(
                        failurePolicy.action,
                        failurePolicy.scope,
                        failurePolicy.name,
                        failurePolicy.windowMillis,
                    )
                }
            }

            for (policy in ratePolicies) {
                val key = keyFor(policy.scope, subject) ?: continue
                val bucketKey = BucketKey(policy.name, policy.scope, key)
                next.blockedUntil[bucketKey]?.let { until ->
                    return@updateState next to AbuseDecision.Deny(
                        AbuseAction.BLOCK,
                        policy.scope,
                        policy.name,
                        until - now,
                    )
                }

                val bucket = next.commandBuckets[bucketKey].orEmpty().pruneBefore(now - policy.windowMillis)
                if (bucket.size >= policy.maxCommands) {
                    next = if (policy.temporaryBlockMillis > 0L) {
                        next.copy(
                            commandBuckets = next.commandBuckets.putOrRemove(bucketKey, bucket),
                            blockedUntil = next.blockedUntil
                                .putBounded(bucketKey, now + policy.temporaryBlockMillis, maxTrackedBuckets),
                        )
                    } else {
                        next.copy(commandBuckets = next.commandBuckets.putOrRemove(bucketKey, bucket))
                    }
                    val action = if (policy.temporaryBlockMillis > 0L) AbuseAction.BLOCK else policy.action
                    val retryAfter = if (policy.temporaryBlockMillis > 0L) {
                        policy.temporaryBlockMillis
                    } else {
                        policy.windowMillis
                    }
                    return@updateState next to AbuseDecision.Deny(action, policy.scope, policy.name, retryAfter)
                }
                next = next.copy(
                    commandBuckets = next.commandBuckets
                        .putBounded(bucketKey, bucket + now, maxTrackedBuckets),
                )
            }
            next to AbuseDecision.Allow
        }
    }

    override fun recordFailure(subject: AbuseSubject, reason: String) {
        val now = clock.nowMillis()
        val key = keyFor(failurePolicy.scope, subject) ?: return
        val bucketKey = BucketKey(failurePolicy.name, failurePolicy.scope, key)
        updateState { current ->
            val bucket = current.failureBuckets[bucketKey]
                .orEmpty()
                .pruneBefore(now - failurePolicy.windowMillis) + now
            val blocked = if (bucket.size >= failurePolicy.maxFailures && failurePolicy.temporaryBlockMillis > 0L) {
                current.blockedUntil.putBounded(bucketKey, now + failurePolicy.temporaryBlockMillis, maxTrackedBuckets)
            } else {
                current.blockedUntil
            }
            current.copy(
                failureBuckets = current.failureBuckets.putBounded(bucketKey, bucket, maxTrackedBuckets),
                blockedUntil = blocked,
            ) to Unit
        }
    }

    override fun recordSuccess(subject: AbuseSubject) {
        val key = keyFor(failurePolicy.scope, subject) ?: return
        val bucketKey = BucketKey(failurePolicy.name, failurePolicy.scope, key)
        updateState { current ->
            current.copy(failureBuckets = current.failureBuckets - bucketKey) to Unit
        }
    }

    private fun keyFor(scope: AbuseScope, subject: AbuseSubject): String? =
        when (scope) {
            AbuseScope.USER -> subject.userId
            AbuseScope.DEVICE -> subject.deviceId
            AbuseScope.SESSION -> subject.sessionId
        }?.takeIf { it.isNotBlank() }

    private fun List<Long>.pruneBefore(cutoff: Long): List<Long> = filter { it >= cutoff }

    private inline fun <T> updateState(transform: (GuardState) -> Pair<GuardState, T>): T {
        while (true) {
            val current = state.load()
            val (next, result) = transform(current)
            if (state.compareAndSet(current, next)) return result
        }
    }

    private data class BucketKey(
        val name: String,
        val scope: AbuseScope,
        val subjectKey: String,
    )

    private data class GuardState(
        val commandBuckets: Map<BucketKey, List<Long>> = emptyMap(),
        val failureBuckets: Map<BucketKey, List<Long>> = emptyMap(),
        val blockedUntil: Map<BucketKey, Long> = emptyMap(),
    ) {
        fun pruneExpiredBlocks(now: Long): GuardState =
            copy(blockedUntil = blockedUntil.filterValues { it > now })
    }

    companion object {
        const val DefaultMaxTrackedBuckets: Int = 10_000

        val DefaultRatePolicies = listOf(
            RateLimitPolicy("user_command_window", AbuseScope.USER, maxCommands = 60, windowMillis = 60_000L),
            RateLimitPolicy("device_command_window", AbuseScope.DEVICE, maxCommands = 90, windowMillis = 60_000L),
            RateLimitPolicy("session_command_burst", AbuseScope.SESSION, maxCommands = 20, windowMillis = 10_000L),
        )
    }
}

private fun <K, V> Map<K, V>.putOrRemove(key: K, value: V?): Map<K, V> =
    if (value == null || value is Collection<*> && value.isEmpty()) this - key else this + (key to value)

private fun <K, V> Map<K, V>.putBounded(key: K, value: V, maxSize: Int): Map<K, V> {
    val updated = LinkedHashMap<K, V>(this)
    updated.remove(key)
    updated[key] = value
    while (updated.size > maxSize) {
        val eldest = updated.keys.firstOrNull() ?: break
        updated.remove(eldest)
    }
    return updated
}
