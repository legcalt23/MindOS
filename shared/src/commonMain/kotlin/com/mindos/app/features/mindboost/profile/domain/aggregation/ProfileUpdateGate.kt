@file:Suppress("MagicNumber") // 0.0/0..1 are domain bounds for the contribution mapping.

package com.mindos.app.features.mindboost.profile.domain.aggregation

import com.mindos.app.features.mindboost.core.scoring.Sampling
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreResult
import com.mindos.app.features.mindboost.core.scoring.canUpdateProfile
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution

/**
 * INFRA-06 · The ONLY authorized bridge from a sealed [ScoreResult] to the global profile. It consumes
 * `ScoreResult` exclusively (never a raw `Double`), and yields a [ModuleContribution] ONLY when the
 * verdict may update the profile — otherwise null (ABSENCE of update, never a low value; R-NO-REDUCE).
 *
 * Lives in the profile module (profile → core dependency, the correct direction). The real
 * [MindBoostProfileAggregator] is a PURE function over `List<ModuleContribution>`, so the gate produces
 * contributions for the caller to fold rather than mutating an aggregator — reconciling doc 04 §4, whose
 * `aggregator.contribute(...)` does not exist in the real (pure) aggregator.
 */
object ProfileUpdateGate {

    /** The contribution iff the result may update the profile, else null (absence, not a low value). */
    fun contributionFor(result: ScoreResult): ModuleContribution? =
        if (result.canUpdateProfile()) result.toModuleContribution() else null

    /** Folds many results into the qualifying contributions (silently drops non-updating ones). */
    fun contributions(results: List<ScoreResult>): List<ModuleContribution> =
        results.mapNotNull(::contributionFor)
}

/**
 * Maps a sealed verdict to a coarse, non-PII contribution. The adapter must have placed a NORMALIZED
 * (0..1) value in [ScoreResult.primarySignal]; an out-of-range value yields a null score (the aggregator
 * then treats it as non-qualifying) rather than throwing.
 *
 * [ModuleContribution.dataSufficiency] reflects SAMPLE sufficiency (nEffective vs the min-n gate), NOT
 * the metric's stability [confidence]. A VALID verdict already cleared MinSampleGate, so sufficiency is
 * 1.0 and the contribution clears the aggregator's sufficiency gate; the separate (possibly low)
 * [confidence] is what keeps a still-coarse signal EMERGING rather than ESTABLISHED. Conflating the two
 * (the original bug) silently dropped honest, sufficiently-sampled signals.
 */
fun ScoreResult.toModuleContribution(): ModuleContribution = ModuleContribution(
    moduleId = provenance.moduleId.toCognitiveModuleId(),
    normalizedScore = if (updatesProfile) primarySignal?.value?.takeIf { it in 0.0..1.0 } else null,
    dataSufficiency = if (updatesProfile) sufficiencyOf(sampling) else 0.0,
    confidence = if (updatesProfile) sampling.confidence else 0.0,
    preliminary = preliminary,
    updatedAtMs = provenance.timestampMs,
)

/** Sample sufficiency from the min-n gate. A VALID verdict has nEffective ≥ minNRequired, so this is 1.0. */
private fun sufficiencyOf(sampling: Sampling): Double =
    if (sampling.minNRequired <= 0) 1.0
    else (sampling.nEffective.toDouble() / sampling.minNRequired).coerceIn(0.0, 1.0)

/** Total mapping from the scoring module id to the profile's module id. */
fun ScoreModuleId.toCognitiveModuleId(): CognitiveModuleId = when (this) {
    ScoreModuleId.FOCUS -> CognitiveModuleId.ENFOQUE
    ScoreModuleId.SPEED -> CognitiveModuleId.VELOCIDAD
    ScoreModuleId.MEMORY -> CognitiveModuleId.MEMORIA
    ScoreModuleId.EPISODIC_MEMORY -> CognitiveModuleId.MEMORIA_EPISODICA
    ScoreModuleId.LOGIC -> CognitiveModuleId.LOGICA
    ScoreModuleId.LANGUAGE -> CognitiveModuleId.LENGUAJE
    ScoreModuleId.DECISIONS -> CognitiveModuleId.DECISIONES
    ScoreModuleId.EMOTIONS -> CognitiveModuleId.EMOCIONES
    ScoreModuleId.CREATIVITY -> CognitiveModuleId.CREATIVIDAD
}
