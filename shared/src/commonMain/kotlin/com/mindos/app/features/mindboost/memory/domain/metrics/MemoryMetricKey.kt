package com.mindos.app.features.mindboost.memory.domain.metrics

/**
 * Catalogue of every metric the memory system can compute (doc §6 taxonomy).
 * Honest by construction: delayed recall and signal-detection measures (d-prime,
 * not raw hit rate) are first-class, and every value carries its sample size and
 * a confidence flag (see [MemoryMetricValue]) so the UI never presents an
 * under-powered estimate as a hard fact (doc §13.16, §6 principio anti-engaño).
 *
 * Grouped per doc §6: performance / learning / consolidation / reliability.
 */
enum class MemoryMetricKey {
    // --- Performance (how it does right now) ---
    IMMEDIATE_RECALL_ACCURACY,
    RECOGNITION_DPRIME,
    RESPONSE_CRITERION,
    RETRIEVAL_SPEED,
    INTRUSION_RATE,
    OMISSION_RATE,
    FALSE_ALARM_RATE,

    // --- Learning (how it improves with practice) ---
    DELAYED_RECALL_ACCURACY,
    FORGETTING_RATE,
    BLOCK_STABILITY,
    LEARNING_CURVE_SLOPE,

    // --- Consolidation (how it retains long-term) ---
    CONSOLIDATION_INDEX,
    INTERFERENCE_INDEX,
    SPACED_REPETITION_BENEFIT,

    // --- Reliability (can I trust the result) ---
    CALIBRATION_INDEX,
    METACOGNITIVE_RESOLUTION,
    CONFIDENCE_BIAS,
    BRIER_SCORE,
    GAMMA_CORRELATION,
    WORKING_MEMORY_SPAN,
    ASSOCIATIVE_INDEX,
    NEAR_TRANSFER_INDEX,

    // --- Diagnostic / sub-capacity specific ---
    ARC_CLUSTERING,
    COWAN_K,
    CONJUNCTION_ERROR_RATE,
    BINDING_ERROR_RATE,
    SERIAL_ORDER_ACCURACY,
    DUAL_TASK_COST,
    PROSPECTIVE_SUCCESS_RATE,
}

/** The unit a [MemoryMetricValue] is expressed in (drives formatting in the UI). */
enum class MemoryMetricUnit {
    PROPORTION,
    MILLISECONDS,
    RATIO,
    COUNT,
    SLOPE,
    INDEX,
    SPAN,
}

/**
 * A single computed metric plus the metadata that keeps reporting honest.
 *
 * @param value the numeric value (callers must pass a finite number, using
 *   [confident] = false when the underlying sample is too small).
 * @param n the sample size that supports this value.
 * @param confident false when [n] is below the threshold needed to trust it; the
 *   UI must then show it as "insufficient data", never as a hard number.
 */
data class MemoryMetricValue(
    val key: MemoryMetricKey,
    val value: Double,
    val n: Int,
    val confident: Boolean,
    val unit: MemoryMetricUnit,
) {
    companion object {
        /** A value the system could not compute with confidence (e.g. zero hits). */
        fun unavailable(key: MemoryMetricKey, unit: MemoryMetricUnit, n: Int = 0): MemoryMetricValue =
            MemoryMetricValue(key = key, value = 0.0, n = n, confident = false, unit = unit)
    }
}

/** A keyed bundle of computed metrics for one block / session / cycle. */
typealias MemoryMetricSet = Map<MemoryMetricKey, MemoryMetricValue>

/** Convenience accessor: numeric value of a metric, or [default] when absent/unconfident. */
fun MemoryMetricSet.valueOf(key: MemoryMetricKey, default: Double = 0.0): Double {
    val metric = this[key] ?: return default
    return if (metric.confident) metric.value else default
}

/** Raw numeric value of a metric regardless of confidence, or [default] when absent. */
fun MemoryMetricSet.rawValueOf(key: MemoryMetricKey, default: Double = 0.0): Double =
    this[key]?.value ?: default
