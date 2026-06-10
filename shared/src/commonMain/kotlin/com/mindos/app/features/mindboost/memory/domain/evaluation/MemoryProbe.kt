package com.mindos.app.features.mindboost.memory.domain.evaluation

import com.mindos.app.features.mindboost.memory.domain.model.AxisLevel
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity

/**
 * How a probe achieves "untrained" status (doc §9.3):
 * - [SAME_PARADIGM_RESERVED]: same exercise format but drawn ENTIRELY from the
 *   reserved transfer-probe content (Phase 3 partition) — content-level transfer.
 *   This is V1's mechanism.
 * - [DIFFERENT_PARADIGM]: a different exercise format measuring the same capacity
 *   (e.g. Span de Operaciones → Span de Lectura). DEFERRED until more exercises exist.
 */
enum class ProbeFormat {
    SAME_PARADIGM_RESERVED,
    DIFFERENT_PARADIGM,
}

/**
 * A reserved evaluation probe. NEVER presented in training. Improving on a probe
 * reflects (near-)transfer, not item familiarity — the only honest evidence of real
 * change (doc §6/§9/§13.4). It runs at a fixed difficulty so pre/post are comparable
 * and it draws from reserved content ([useReservedContent] is always true).
 *
 * The reserved-content sampler is supplied at runtime from the Phase-3
 * [com.mindos.app.features.mindboost.memory.domain.content.partition.TransferProbePartition]
 * (its `reservedWords`/`reservedPairs`); the probe definition only declares intent.
 */
data class MemoryProbe(
    val probeId: String,
    val subcapacity: MemorySubcapacity,
    val exerciseId: MemoryExerciseId,
    val fixedDifficulty: MemoryDifficultyVector,
    val format: ProbeFormat = ProbeFormat.SAME_PARADIGM_RESERVED,
    val useReservedContent: Boolean = true,
) {
    init {
        require(probeId.isNotBlank()) { "probeId cannot be blank." }
        require(useReservedContent) { "A probe must use reserved content (never trained), id=$probeId." }
    }
}

/**
 * The catalogue of reserved probes for the V1 subcapacities (WM / episodic /
 * associative). Each uses its training exercise's paradigm but reserved content
 * (V1 = content-level transfer); different-paradigm probes are deferred.
 */
object MemoryProbeBank {

    private const val PROBE_LOAD = 0.4

    private val probeDifficulty = MemoryDifficultyVector.EASIEST
        .adjust(MemoryDifficultyAxis.LOAD, AxisLevel(PROBE_LOAD))

    val WORKING_MEMORY = MemoryProbe(
        probeId = "probe_wm_span_reserved",
        subcapacity = MemorySubcapacity.WORKING_MEMORY,
        exerciseId = MemoryExerciseId.EX1_SPAN_COMPLEJO,
        fixedDifficulty = probeDifficulty,
    )

    val EPISODIC = MemoryProbe(
        probeId = "probe_episodic_list_reserved",
        subcapacity = MemorySubcapacity.EPISODIC,
        exerciseId = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
        fixedDifficulty = probeDifficulty,
    )

    val ASSOCIATIVE = MemoryProbe(
        probeId = "probe_associative_pairs_reserved",
        subcapacity = MemorySubcapacity.ASSOCIATIVE,
        exerciseId = MemoryExerciseId.EX4_PARES_ASOCIADOS,
        fixedDifficulty = probeDifficulty,
    )

    val all: List<MemoryProbe> = listOf(WORKING_MEMORY, EPISODIC, ASSOCIATIVE)

    fun forSubcapacity(subcapacity: MemorySubcapacity): MemoryProbe? =
        all.firstOrNull { it.subcapacity == subcapacity }
}
