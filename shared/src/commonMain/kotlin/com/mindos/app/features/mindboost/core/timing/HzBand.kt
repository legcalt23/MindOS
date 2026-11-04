package com.mindos.app.features.mindboost.core.timing

import kotlin.math.abs

/**
 * S5 · ADR-FOCUS-TEMPORAL-QUALITY §11/§14 [R2-HIST] — detected refresh rates are persisted BANDED, never as
 * the raw float (data-minimisation: the exact panel cadence is closer to hardware fingerprinting
 * than to the comparability confound the charter needs). Classification still uses the real Hz
 * in memory; only the band crosses into any serialized type.
 */
@Suppress("MagicNumber") // The band values ARE the data (standard panel refresh rates).
enum class HzBand(val nominalHz: Double) {
    HZ_60(60.0),
    HZ_90(90.0),
    HZ_120(120.0),
    HZ_144(144.0),
    HZ_240(240.0),

    /** Non-standard cadence (unusual panel or unreliable detection). */
    OTHER(0.0),
    ;

    companion object {
        /** Bands match within ±5% of the standard rate (119.4 and 120.6 both land in [HZ_120]). */
        private const val BAND_TOLERANCE_FRACTION: Double = 0.05

        fun of(detectedHz: Double): HzBand = entries
            .filter { it != OTHER }
            .firstOrNull { abs(detectedHz - it.nominalHz) <= it.nominalHz * BAND_TOLERANCE_FRACTION }
            ?: OTHER
    }
}
