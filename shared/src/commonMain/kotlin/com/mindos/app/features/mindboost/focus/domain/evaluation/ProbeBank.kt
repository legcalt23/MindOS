package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.Capacity
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.Paradigm

/**
 * The curated, reserved probe bank. This catalogue is intentionally SEPARATE from
 * [com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog]
 * (the training catalogue): probe content (seeds + forms) never appears in training,
 * so a gain on a probe cannot be task-specific learning.
 *
 * The form seeds are the data here (reserved parallel forms), hence MagicNumber is suppressed.
 */
@Suppress("MagicNumber")
object ProbeBank {

    /** A fixed, mid-level difficulty so pre/post probe runs are comparable. */
    private val fixedProbeDifficulty = DifficultyVector(
        perceptualLoad = AxisLevel(0.4),
        temporalUncertainty = AxisLevel(0.4),
    )

    val VIGILANCE_PROBE = ProbeDefinition(
        probeId = "probe_vigilance_v1",
        capacity = Capacity.SUSTAINED_VIGILANCE,
        paradigm = Paradigm.PVT,
        fixedDifficulty = fixedProbeDifficulty,
        seedPolicy = SeedPolicy.ROTATED_RESERVED,
        reservedForms = listOf(1_001L, 1_002L, 1_003L),
    )

    val SELECTIVE_PROBE = ProbeDefinition(
        probeId = "probe_selective_v1",
        capacity = Capacity.SELECTIVE_FILTERING,
        paradigm = Paradigm.FLANKER,
        fixedDifficulty = fixedProbeDifficulty,
        seedPolicy = SeedPolicy.ROTATED_RESERVED,
        reservedForms = listOf(2_001L, 2_002L, 2_003L),
    )

    val INHIBITION_PROBE = ProbeDefinition(
        probeId = "probe_inhibition_v1",
        capacity = Capacity.RESPONSE_INHIBITION,
        paradigm = Paradigm.GO_NO_GO,
        fixedDifficulty = fixedProbeDifficulty,
        seedPolicy = SeedPolicy.ROTATED_RESERVED,
        reservedForms = listOf(3_001L, 3_002L, 3_003L),
    )

    val all: List<ProbeDefinition> = listOf(VIGILANCE_PROBE, SELECTIVE_PROBE, INHIBITION_PROBE)

    /** Every reserved seed used by any probe — proven disjoint from training in tests. */
    val allReservedSeeds: Set<Long> = all.flatMap { it.reservedForms }.toSet()

    fun forCapacity(capacity: Capacity): ProbeDefinition? = all.firstOrNull { it.capacity == capacity }
}
