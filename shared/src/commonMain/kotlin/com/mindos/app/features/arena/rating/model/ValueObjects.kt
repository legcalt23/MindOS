package com.mindos.app.features.arena.rating.model

import kotlinx.serialization.Serializable

/**
 * Phase 02: Auditable Data Model
 * Numeric modeling requirement: No raw Double for critical decision values.
 * Uses Kotlin inline value classes where applicable to avoid allocation overhead
 * while enforcing type safety.
 */

@Serializable
@JvmInline
value class Rating(val value: Double) {
    init {
        require(!value.isNaN() && !value.isInfinite()) { "Rating must be a finite value" }
    }
}

@Serializable
@JvmInline
value class Sigma(val value: Double) {
    init {
        require(value >= 0.0 && !value.isNaN() && !value.isInfinite()) {
            "Sigma must be a finite, non-negative value"
        }
    }
}

@Serializable
@JvmInline
value class Confidence(val value: Double) {
    init {
        require(value in 0.0..1.0) { "Confidence must be between 0.0 and 1.0" }
    }
}

@Serializable
@JvmInline
value class LatencyMs(val value: Long) {
    init {
        require(value >= 0L) { "Latency must be non-negative" }
    }
}

@Serializable
@JvmInline
value class JitterMs(val value: Long) {
    init {
        require(value >= 0L) { "Jitter must be non-negative" }
    }
}

@Serializable
@JvmInline
value class SEM(val value: Double) {
    init {
        require(value >= 0.0 && !value.isNaN()) { "SEM must be non-negative and not NaN" }
    }
}

@Serializable
data class ConfidenceInterval(val lowerBound: Double, val upperBound: Double) {
    init {
        require(lowerBound <= upperBound) { "lowerBound must be <= upperBound" }
    }
}

@Serializable
@JvmInline
value class Reliability(val value: Double) {
    init {
        require(value in 0.0..1.0) { "Reliability must be between 0.0 and 1.0" }
    }
}

@Serializable
@JvmInline
value class Drift(val value: Double) {
    init {
        require(!value.isNaN()) { "Drift cannot be NaN" }
    }
}

@Serializable
@JvmInline
value class MeasurementQuality(val value: Double) {
    init {
        require(value in 0.0..1.0) { "MeasurementQuality must be between 0.0 and 1.0" }
    }
}

@Serializable
@JvmInline
value class MatchQuality(val value: Double) {
    init {
        require(value in 0.0..1.0) { "MatchQuality must be between 0.0 and 1.0" }
    }
}
