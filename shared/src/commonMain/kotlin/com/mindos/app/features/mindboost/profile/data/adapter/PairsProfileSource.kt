package com.mindos.app.features.mindboost.profile.data.adapter

import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.repository.PairsResultStore
import com.mindos.app.features.mindboost.memory.domain.scoring.MemoryPairsScore
import com.mindos.app.features.mindboost.profile.domain.aggregation.ProfileUpdateGate
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.source.CognitiveModuleProfileSource

/**
 * Adapter: Memoria → WORKING_MEMORY contribution from EX4 Pares Asociados (P3). Reads ONLY the encrypted
 * [PairsResultStore] that EX4 accumulates, folds it through [MemoryPairsScore] (the honest discrimination
 * scorer whose validity gate IS the tested anti-gaming) into a sealed `ScoreResult`, and lets the
 * authoritative [ProfileUpdateGate] decide: a contribution ONLY when the verdict is VALID, otherwise
 * `null` (CALIBRATING, never a fabricated value) — the same gate as every module, no bespoke scoring.
 *
 * COARSE BLEND (by design): this is a SECOND MEMORIA source alongside [MemoryProfileSource] (EX1 span).
 * Both carry `moduleId = MEMORIA`, so the aggregator BLENDS the complex-span and the associative signals
 * into the single WORKING_MEMORY dimension. One dimension per module is the taxonomy's deliberate
 * coarseness; a separate associative dimension would be a taxonomy change, out of scope here.
 *
 * Preliminary by construction (one accumulated block → confidence below ESTABLISHED), so MEMORIA can read
 * EMERGING/preliminary once ≥20 discriminating trials accumulate, never ESTABLISHED yet. The provenance
 * timestamp is metadata only and deliberately 0L, so this source touches no `Clock.System` (avoiding the
 * kotlinx-datetime runtime fragility; project learning 2026-05-29).
 */
class PairsProfileSource(
    private val pairsStore: PairsResultStore,
) : CognitiveModuleProfileSource {

    override val moduleId: CognitiveModuleId = CognitiveModuleId.MEMORIA

    override suspend fun contribution(userId: String): ModuleContribution? {
        val log = pairsStore.load(userId)
        if (log.trials.isEmpty()) return null
        val result = MemoryPairsScore.scoreOf(
            log = log,
            exerciseId = MemoryExerciseId.EX4_PARES_ASOCIADOS.name,
            sessionId = "$userId:pairs-aggregate",
            userId = userId,
            timestampMs = 0L,
        )
        return ProfileUpdateGate.contributionFor(result)
    }
}
