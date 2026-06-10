package com.mindos.app.features.mindboost.memory.domain.session

import kotlin.math.abs

/**
 * Session length templates from the functional design (doc §7). Durations drive
 * the phase blueprints in [MemorySessionBlueprints].
 */
@Suppress("MagicNumber") // The per-template durations ARE the data, named by the parameters.
enum class MemorySessionTemplate(
    val displayName: String,
    val totalMinutes: Int,
) {
    /** 5 min — maintenance / fatigue days; no interference or delayed recall (doc §7). */
    MICRO_5("Micro", 5),

    /** 10 min — short standard day. */
    STANDARD_10("Estándar corta", 10),

    /** 20 min — full reference session. */
    FULL_20("Estándar completa", 20),

    /** 30 min — intensive (two blocks + cross-interference + rest, doc §7). */
    INTENSIVE_30("Intensiva", 30),
    ;

    companion object {
        /** Template whose length is closest to the available [minutes]. */
        fun forMinutes(minutes: Int): MemorySessionTemplate =
            entries.minByOrNull { abs(it.totalMinutes - minutes) } ?: STANDARD_10
    }
}
