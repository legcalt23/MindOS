package com.mindos.app.features.arena.observability

import com.mindos.app.features.arena.config.ActivityStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.VerificationStateEnum
import com.mindos.app.features.arena.sanitization.ArenaErrorCategory

/**
 * Phase 07.5 / Task 9: Stable, safe event names for arena telemetry.
 */
enum class ArenaTelemetryEventName {
    RATING_STATE_SNAPSHOT,
    AGGREGATE_MATCH_METRICS,
    NON_FATAL_ERROR,
}

/**
 * Phase 07.5 / Task 9: The ONLY shapes that may be recorded as telemetry. Every variant carries
 * exclusively safe fields — aggregate metrics, enum states, error categories — and never a UID,
 * rating value, email, or free-text payload. Raw domain objects cannot be recorded directly.
 */
sealed interface NonFatalTelemetryEvent {
    val eventName: ArenaTelemetryEventName

    /** Allowed aggregate payload, e.g. {"hardwareClass":"IOS-HZ_120","averageMatchDurationMs":45000}. */
    data class AggregateMatchMetrics(
        val hardwareClass: String,
        val averageMatchDurationMs: Long,
    ) : NonFatalTelemetryEvent {
        override val eventName: ArenaTelemetryEventName = ArenaTelemetryEventName.AGGREGATE_MATCH_METRICS

        init {
            require(averageMatchDurationMs >= 0) { "averageMatchDurationMs must be >= 0" }
        }
    }

    /** Redacted projection of a rating state: enum states only, NEVER the rating mean/uncertainty. */
    data class RatingStateSnapshot(
        val integrityState: IntegrityStateEnum,
        val activityState: ActivityStateEnum,
        val verificationState: VerificationStateEnum,
        val provisional: Boolean,
    ) : NonFatalTelemetryEvent {
        override val eventName: ArenaTelemetryEventName = ArenaTelemetryEventName.RATING_STATE_SNAPSHOT
    }

    /** A non-fatal error reduced to a safe category (no message, no cause, no identifiers). */
    data class SafeError(
        val category: ArenaErrorCategory,
    ) : NonFatalTelemetryEvent {
        override val eventName: ArenaTelemetryEventName = ArenaTelemetryEventName.NON_FATAL_ERROR
    }
}
