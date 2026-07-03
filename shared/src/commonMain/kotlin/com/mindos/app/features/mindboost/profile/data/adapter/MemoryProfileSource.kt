package com.mindos.app.features.mindboost.profile.data.adapter

import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.repository.SpanResultStore
import com.mindos.app.features.mindboost.memory.domain.scoring.MemorySpanScore
import com.mindos.app.features.mindboost.profile.domain.aggregation.ProfileUpdateGate
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.source.CognitiveModuleProfileSource

/**
 * Adapter: Memoria → WORKING_MEMORY contribution (P5). Reads ONLY the encrypted [SpanResultStore]
 * that EX1 accumulates, folds it through [MemorySpanScore] (the honest complex-span scorer + the real
 * processing anti-gaming detector) into a sealed `ScoreResult`, and lets the authoritative
 * [ProfileUpdateGate] decide: a contribution ONLY when the verdict is VALID, otherwise `null`
 * (CALIBRATING, never a fabricated value). This is the only place Memoria reaches the global profile,
 * and it goes through the same gate as every other module — no bespoke scoring.
 *
 * The contribution is preliminary by construction: the accumulated log forms one block, so the scorer's
 * confidence stays below the aggregator's ESTABLISHED threshold. Once ≥20 valid trials accumulate the
 * WORKING_MEMORY dimension therefore surfaces a value as EMERGING/preliminary, but never ESTABLISHED yet
 * (sample sufficiency clears the gate; the low one-block confidence holds it at EMERGING). The provenance
 * timestamp is metadata only and deliberately 0L ("unknown"): the accumulated log carries no single
 * session time, and the aggregator stamps the profile with its own clock — so this source touches no
 * `Clock.System` (avoiding the kotlinx-datetime runtime fragility; project learning 2026-05-29).
 */
class MemoryProfileSource(
    private val spanStore: SpanResultStore,
) : CognitiveModuleProfileSource {

    override val moduleId: CognitiveModuleId = CognitiveModuleId.MEMORIA

    override suspend fun contribution(userId: String): ModuleContribution? {
        val log = spanStore.load(userId)
        if (log.trials.isEmpty()) return null
        val result = MemorySpanScore.scoreOf(
            log = log,
            exerciseId = MemoryExerciseId.EX1_SPAN_COMPLEJO.name,
            sessionId = "$userId:span-aggregate",
            userId = userId,
            timestampMs = 0L,
        )
        return ProfileUpdateGate.contributionFor(result)
    }
}
