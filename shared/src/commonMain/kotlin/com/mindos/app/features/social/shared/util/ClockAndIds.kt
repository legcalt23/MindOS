package com.mindos.app.features.social.shared.util

import com.mindos.app.features.social.domain.valueobject.AuditEventId
import com.mindos.app.features.social.domain.valueobject.RequestId
import com.mindos.app.features.social.domain.valueobject.Timestamp

fun interface AppClock {
    fun now(): Timestamp
}

class IncrementingAppClock(
    startEpochMillis: Long = 1_700_000_000_000,
    private val stepMillis: Long = 1,
) : AppClock {
    private var nextEpochMillis = startEpochMillis

    override fun now(): Timestamp {
        val current = Timestamp(nextEpochMillis)
        nextEpochMillis += stepMillis
        return current
    }
}

fun interface RequestIdGenerator {
    fun nextRequestId(): RequestId
}

class SequentialRequestIdGenerator(
    private val prefix: String = "follow_request",
) : RequestIdGenerator {
    private var next = 1L

    override fun nextRequestId(): RequestId =
        RequestId("${prefix}_${next++}")
}

fun interface AuditEventIdGenerator {
    fun nextAuditEventId(): AuditEventId
}

class SequentialAuditEventIdGenerator(
    private val prefix: String = "audit",
) : AuditEventIdGenerator {
    private var next = 1L

    override fun nextAuditEventId(): AuditEventId =
        AuditEventId("${prefix}_${next++}")
}

