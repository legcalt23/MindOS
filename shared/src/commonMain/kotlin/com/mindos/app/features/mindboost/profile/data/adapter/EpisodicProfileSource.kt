package com.mindos.app.features.mindboost.profile.data.adapter

import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.repository.DeferredResultStore
import com.mindos.app.features.mindboost.memory.domain.scoring.MemoryDeferredScore
import com.mindos.app.features.mindboost.profile.domain.aggregation.ProfileUpdateGate
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.source.CognitiveModuleProfileSource

/**
 * Adapter: Memoria → EPISODIC_MEMORY contribution from EX3 Lista con Estrategia, via the 24h delayed-recall
 * loop. Reads ONLY the encrypted [DeferredResultStore] that the deferred-recall flow accumulates, folds it
 * through [MemoryDeferredScore] (the honest discrimination scorer whose validity gate IS the tested
 * anti-gaming) into a sealed `ScoreResult`, and lets the authoritative [ProfileUpdateGate] decide: a
 * contribution ONLY when the verdict is VALID, otherwise `null` (CALIBRATING, never a fabricated value) —
 * the same gate as every module, no bespoke scoring.
 *
 * OWN DIMENSION (the episodic taxonomy split): EX3's governing signal is EPISODIC (24h delayed retention),
 * NOT working memory, so it informs [CognitiveModuleId.MEMORIA_EPISODICA] → EPISODIC_MEMORY — distinct from
 * the working-memory sources [MemoryProfileSource] (EX1 complex-span) and [PairsProfileSource] (EX4
 * associative), which inform MEMORIA → WORKING_MEMORY. The Memoria feature feeds two profile dimensions;
 * the score module id is `ScoreModuleId.EPISODIC_MEMORY` (set by [MemoryDeferredScore]).
 *
 * Preliminary by construction (one accumulated block → confidence below ESTABLISHED), so EPISODIC_MEMORY
 * surfaces EMERGING/preliminary once ≥20 discriminating delayed-recall outcomes accumulate (≈20 days, one
 * per day — inherent to a 24h-retention measure), never ESTABLISHED yet. The provenance timestamp is
 * metadata only and deliberately 0L, so this source touches no `Clock.System` (avoiding the kotlinx-datetime
 * runtime fragility; project learning 2026-05-29).
 */
class EpisodicProfileSource(
    private val deferredStore: DeferredResultStore,
) : CognitiveModuleProfileSource {

    override val moduleId: CognitiveModuleId = CognitiveModuleId.MEMORIA_EPISODICA

    override suspend fun contribution(userId: String): ModuleContribution? {
        val log = deferredStore.load(userId)
        if (log.outcomes.isEmpty()) return null
        val result = MemoryDeferredScore.scoreOf(
            log = log,
            exerciseId = MemoryExerciseId.EX3_LISTA_ESTRATEGIA.name,
            sessionId = "$userId:deferred-aggregate",
            userId = userId,
            timestampMs = 0L,
        )
        return ProfileUpdateGate.contributionFor(result)
    }
}
