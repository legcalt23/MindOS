package com.mindos.security

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

class InMemoryAbuseGuard(
    private val ratePolicies: List<RateLimitPolicy> = DefaultRatePolicies,
    private val failurePolicy: FailurePolicy = FailurePolicy(),
    private val clock: SecurityClock = SecurityClocks.Zero,
) : AbuseGuard {

    private val commandBuckets = HashMap<BucketKey, ArrayList<Long>>()
    private val failureBuckets = HashMap<BucketKey, ArrayList<Long>>()
    private val blockedUntil = HashMap<BucketKey, Long>()

    override fun beforeCommand(subject: AbuseSubject): AbuseDecision {
        val now = clock.nowMillis()
        val failureKey = keyFor(failurePolicy.scope, subject)
        if (failureKey != null) {
            val bucketKey = BucketKey(failurePolicy.name, failurePolicy.scope, failureKey)
            blockedUntil[bucketKey]?.let { until ->
                if (until > now) {
                    return AbuseDecision.Deny(AbuseAction.BLOCK, failurePolicy.scope, failurePolicy.name, until - now)
                }
                blockedUntil.remove(bucketKey)
            }
            failureBuckets[bucketKey]?.let { failures ->
                failures.pruneBefore(now - failurePolicy.windowMillis)
                if (failures.size >= failurePolicy.maxFailures) {
                    return AbuseDecision.Deny(
                        failurePolicy.action,
                        failurePolicy.scope,
                        failurePolicy.name,
                        failurePolicy.windowMillis,
                    )
                }
            }
        }

        for (policy in ratePolicies) {
            val key = keyFor(policy.scope, subject) ?: continue
            blockedUntil[BucketKey(policy.name, policy.scope, key)]?.let { until ->
                if (until > now) {
                    return AbuseDecision.Deny(AbuseAction.BLOCK, policy.scope, policy.name, until - now)
                }
                blockedUntil.remove(BucketKey(policy.name, policy.scope, key))
            }

            val bucketKey = BucketKey(policy.name, policy.scope, key)
            val bucket = commandBuckets.getOrPut(bucketKey) { ArrayList() }
            bucket.pruneBefore(now - policy.windowMillis)
            if (bucket.size >= policy.maxCommands) {
                if (policy.temporaryBlockMillis > 0L) {
                    blockedUntil[bucketKey] = now + policy.temporaryBlockMillis
                    return AbuseDecision.Deny(AbuseAction.BLOCK, policy.scope, policy.name, policy.temporaryBlockMillis)
                }
                return AbuseDecision.Deny(policy.action, policy.scope, policy.name, policy.windowMillis)
            }
            bucket += now
        }
        return AbuseDecision.Allow
    }

    override fun recordFailure(subject: AbuseSubject, reason: String) {
        val now = clock.nowMillis()
        val key = keyFor(failurePolicy.scope, subject) ?: return
        val bucketKey = BucketKey(failurePolicy.name, failurePolicy.scope, key)
        val bucket = failureBuckets.getOrPut(bucketKey) { ArrayList() }
        bucket.pruneBefore(now - failurePolicy.windowMillis)
        bucket += now
        if (bucket.size >= failurePolicy.maxFailures && failurePolicy.temporaryBlockMillis > 0L) {
            blockedUntil[bucketKey] = now + failurePolicy.temporaryBlockMillis
        }
    }

    override fun recordSuccess(subject: AbuseSubject) {
        val key = keyFor(failurePolicy.scope, subject) ?: return
        failureBuckets.remove(BucketKey(failurePolicy.name, failurePolicy.scope, key))
    }

    private fun keyFor(scope: AbuseScope, subject: AbuseSubject): String? =
        when (scope) {
            AbuseScope.USER -> subject.userId
            AbuseScope.DEVICE -> subject.deviceId
            AbuseScope.SESSION -> subject.sessionId
        }?.takeIf { it.isNotBlank() }

    private fun MutableList<Long>.pruneBefore(cutoff: Long) {
        var write = 0
        for (read in indices) {
            val value = this[read]
            if (value >= cutoff) {
                this[write] = value
                write += 1
            }
        }
        while (size > write) removeAt(lastIndex)
    }

    private data class BucketKey(
        val name: String,
        val scope: AbuseScope,
        val subjectKey: String,
    )

    companion object {
        val DefaultRatePolicies = listOf(
            RateLimitPolicy("user_command_window", AbuseScope.USER, maxCommands = 60, windowMillis = 60_000L),
            RateLimitPolicy("device_command_window", AbuseScope.DEVICE, maxCommands = 90, windowMillis = 60_000L),
            RateLimitPolicy("session_command_burst", AbuseScope.SESSION, maxCommands = 20, windowMillis = 10_000L),
        )
    }
}
