package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.Capacity
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.Paradigm

/**
 * The curated, reserved probe bank. This catalogue is a SEPARATE list from
 * [com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog] (the training catalogue),
 * but what is disjoint between the two is the SEED, not the task: every probe here runs a paradigm the
 * catalogue also trains, so a probe is a PARALLEL FORM of a trained task, not an untrained instrument.
 * A gain on a probe therefore CAN be task-specific learning, which is why the Enfoque claim ladder is
 * frozen at C0 (`MB-FOCUS-0089` / `FOC-P0-00`). The fact is pinned by
 * `FocusClaimLevelGateTest.everyProbeParadigmIsAlsoATrainedParadigm`, which goes red the day a probe
 * runs a paradigm nobody trains — that failure is the invitation to revisit the ladder with evidence.
 *
 * The form seeds are the data here (reserved parallel forms), hence MagicNumber is suppressed.
 *
 * SUPPLY VERSUS RELIABILITY GATE (MB-FOCUS-0122, R13). The reliability gate opens for the administration
 * whose prior count reaches [ProbeReliabilityEstimator.MIN_ADMINISTRATIONS], i.e. administration number
 * MIN + 1, which draws reserved form index MIN. With exactly MIN forms that administration wraps to the
 * baseline's own form: the first retest that could ever carry a reliable-change claim would be the one
 * that repeats the baseline material. The supply therefore holds MORE forms than the gate requires, and
 * `ProbeBankTest.theFirstGatedRetestNeverLandsOnTheBaselineForm` is derived from both authorities so
 * editing either one alone turns it red. Each form beyond MIN + 1 buys one more fresh gated retest;
 * how many the product promises is a declared decision (MB-FOCUS-0133), not a side effect of this list.
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
        reservedForms = listOf(1_001L, 1_002L, 1_003L, 1_004L),
    )

    val SELECTIVE_PROBE = ProbeDefinition(
        probeId = "probe_selective_v1",
        capacity = Capacity.SELECTIVE_FILTERING,
        paradigm = Paradigm.FLANKER,
        fixedDifficulty = fixedProbeDifficulty,
        seedPolicy = SeedPolicy.ROTATED_RESERVED,
        reservedForms = listOf(2_001L, 2_002L, 2_003L, 2_004L),
    )

    val INHIBITION_PROBE = ProbeDefinition(
        probeId = "probe_inhibition_v1",
        capacity = Capacity.RESPONSE_INHIBITION,
        paradigm = Paradigm.GO_NO_GO,
        fixedDifficulty = fixedProbeDifficulty,
        seedPolicy = SeedPolicy.ROTATED_RESERVED,
        reservedForms = listOf(3_001L, 3_002L, 3_003L, 3_004L),
    )

    val all: List<ProbeDefinition> = listOf(VIGILANCE_PROBE, SELECTIVE_PROBE, INHIBITION_PROBE)

    /** Every reserved seed used by any probe — proven disjoint from training in tests. */
    val allReservedSeeds: Set<Long> = all.flatMap { it.reservedForms }.toSet()

    // A capacity -> probe lookup used to live here with no caller anywhere in the tree, kept for a phase
    // that never landed. Removed (R13): a selector preserved for later reads as a capability that already
    // exists, and nothing here decides which metric a verdict would then rest on. Whoever wires probe
    // selection by capacity writes it back with its consumer and its test, and owns that decision then.
}
