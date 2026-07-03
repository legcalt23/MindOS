package com.mindos.app.features.mindboost.profile.data.adapter

import com.mindos.app.features.mindboost.decisiones.domain.repository.ReversibilityResultStore
import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityScore
import com.mindos.app.features.mindboost.profile.domain.aggregation.ProfileUpdateGate
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.source.CognitiveModuleProfileSource

/**
 * Adapter: Decisiones → DECISION_MAKING contribution from E2 Reversibilidad (P3c). Reads ONLY the
 * encrypted [ReversibilityResultStore] that the live reversibility-triage flow accumulates, folds it
 * through [ReversibilityScore] (balanced-accuracy discrimination, whose base-rate/constant-responder gates
 * ARE the tested anti-gaming) into a sealed `ScoreResult`, and lets the authoritative [ProfileUpdateGate]
 * decide: a contribution ONLY when the verdict is VALID, otherwise `null` (CALIBRATING, never a fabricated
 * value) — the same gate as every module, no bespoke scoring. This is the only place Decisiones reaches the
 * global profile.
 *
 * Preliminary by construction (one accumulated block → confidence below ESTABLISHED), so DECISION_MAKING
 * surfaces EMERGING/preliminary once ≥20 honest discriminating reversibility cards accumulate (≈3–4
 * sessions of ~6 cards), never ESTABLISHED yet. The provenance timestamp is metadata only and deliberately
 * 0L, so this source touches no `Clock.System` (avoiding the kotlinx-datetime runtime fragility; project
 * learning 2026-05-29); identity is the pseudonymous user id, never the raw uid.
 */
class DecisionsProfileSource(
    private val reversibilityStore: ReversibilityResultStore,
) : CognitiveModuleProfileSource {

    override val moduleId: CognitiveModuleId = CognitiveModuleId.DECISIONES

    override suspend fun contribution(userId: String): ModuleContribution? {
        val log = reversibilityStore.load(userId)
        if (log.trials.isEmpty()) return null
        val result = ReversibilityScore.scoreOf(
            log = log,
            exerciseId = "E2_REVERSIBILIDAD",
            sessionId = "$userId:reversibility-aggregate",
            userId = userId,
            timestampMs = 0L,
        )
        return ProfileUpdateGate.contributionFor(result)
    }
}
