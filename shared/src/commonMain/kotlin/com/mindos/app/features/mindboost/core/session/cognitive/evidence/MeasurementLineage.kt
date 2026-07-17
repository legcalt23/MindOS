package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId

/**
 * SYSTEM-1 slice 6-R2 · The comparability key of a run's score. Two runs are score-comparable ONLY if their keys
 * are equal; the extractor NEVER pools runs with differing keys (no equating migrator exists). A key match is
 * NECESSARY but NOT sufficient even for WITHIN-scale comparability, and definitely not for RANK: adaptive
 * difficulty, device and modality are construct-irrelevant confounds NOT captured on the run
 * ([com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun] has `difficulty` and a coarse
 * `platform` but neither is in this key), so device/modality can shift scores materially (Passell 2021, PMID
 * 33954913; Nicosia 2023, PMID 35953659). This contract ships no block-readiness-for-ranking vocabulary at all
 * (6R3 §18 C2.3), so a scale-key match alone is never sufficient grounds to unlock anything beyond the honest
 * within-scale ceiling on [MeasurementLineage.comparability] — an equating authority for rank is a separate,
 * not-yet-built bridge.
 */
data class ScoreScaleKey(
    val blockId: ScoreModuleId,
    val exerciseId: String,
    val exerciseVersion: String,
    val scoringVersion: String,
)

/**
 * SYSTEM-1 6R3 (§7) · The strongest comparability the contract may HONESTLY assert about a group of runs. There
 * is no [ScoreScaleKey]-equating authority, so an equal scale key alone is NOT sufficient for a genuine
 * "comparable" claim — construct/metric/norm/device/modality lineage beyond the key is not captured on the run.
 * [ComparabilityNotEstablished] is the default and only within-scale state this contract can reach today, never a
 * cross-scale or rank comparability.
 */
sealed interface ComparabilityVerdict {

    /** All runs share the exact scale key, but lineage beyond the key (difficulty/device/modality) is uncaptured,
     *  so key equality alone is NOT evidence of genuine comparability — the honest ceiling this contract can
     *  assert today. */
    data object ComparabilityNotEstablished : ComparabilityVerdict

    /** Differing scale keys, or a captured-and-differing lineage component: these observations are NEVER pooled.
     *  More than one [ExerciseScaleEvidence] group present for the same block is itself this cross-scale signal. */
    data object Incompatible : ComparabilityVerdict

    /** Only reachable once a real, versioned equating authority exists for [bridgeVersion] — UNREACHABLE / fail-
     *  closed from this contract today; no construction site produces it. */
    data class Bridged(val bridgeVersion: String) : ComparabilityVerdict
}

/**
 * SYSTEM-1 slice 6-R2 · The versioned lineage of one evidence group: its [scaleKey] and the honest
 * [comparability] ceiling it may claim. The [lineagePolicyVersion] pins the compatibility rule so a later change
 * (e.g. adding normalization/difficulty/device components once the run carries them) is detectable.
 */
data class MeasurementLineage(
    val scaleKey: ScoreScaleKey,
    val comparability: ComparabilityVerdict,
    val lineagePolicyVersion: String,
)

/** SYSTEM-1 slice 6-R2 · The versioned compatibility rule. Bump [VERSION] when the comparability logic changes. */
object LineagePolicy {
    const val VERSION: String = "measurement-lineage-v1"
}
