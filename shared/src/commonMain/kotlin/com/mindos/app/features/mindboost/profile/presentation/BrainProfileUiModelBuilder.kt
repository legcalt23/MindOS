package com.mindos.app.features.mindboost.profile.presentation

import com.mindos.app.features.mindboost.profile.domain.display.DisplayScore
import com.mindos.app.features.mindboost.profile.domain.display.UserDisplayScoreMapper
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.DimensionState
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile
import com.mindos.app.features.mindboost.profile.domain.model.OverallSummary
import com.mindos.app.features.mindboost.profile.domain.model.ProvisionalGlobalIndex
import com.mindos.app.features.mindboost.profile.domain.orchestration.RecommendationReason
import com.mindos.app.features.mindboost.profile.domain.orchestration.TrainingRecommendation
import com.mindos.app.features.mindboost.profile.presentation.state.BrainProfileUiModel
import com.mindos.app.features.mindboost.profile.presentation.state.DimensionUi
import com.mindos.app.features.mindboost.profile.presentation.state.OverallUi
import com.mindos.app.features.mindboost.profile.presentation.state.RecommendationUi
import com.mindos.app.features.mindboost.profile.presentation.state.displayLabel
import com.mindos.app.features.mindboost.profile.presentation.state.displaySymbol
import com.mindos.app.features.mindboost.profile.presentation.state.toProductMotorLabel
import kotlin.math.roundToInt

/**
 * Pure mapper: domain [MindBoostProfile] + [TrainingRecommendation] → display-ready [BrainProfileUiModel].
 * This is where the honesty rules become UI: a CALIBRATING dimension renders "—" (never 0); the copy is
 * functional/non-clinical; the screen always carries the not-a-diagnosis disclaimer.
 */
object BrainProfileUiModelBuilder {

    private const val PERCENT_SCALE = 100.0
    private const val MAX_PERCENT = 100
    private const val EM_DASH = "—"
    private const val TITLE = "Tu perfil cognitivo"
    private const val SUBTITLE = "Tu progreso funcional frente a tu propia línea base."
    private const val DISCLAIMER =
        "Perfil funcional de práctica, orientativo y preliminar. No es un diagnóstico ni una medida clínica."

    fun build(profile: MindBoostProfile, recommendation: TrainingRecommendation): BrainProfileUiModel =
        BrainProfileUiModel(
            title = TITLE,
            subtitle = SUBTITLE,
            overall = overallUi(profile.overall),
            dimensions = CognitiveDimension.entries.map { dimensionUi(profile.dimension(it)) },
            recommendation = recommendationUi(recommendation),
            disclaimer = DISCLAIMER,
        )

    private fun dimensionUi(state: DimensionState): DimensionUi {
        // DC-2 display gate (B6): a 0-100 number is shown ONLY when the mapper allows it — ESTABLISHED, or
        // EMERGING with confidence >= gate. An EMERGING low-confidence value renders "—", never a number.
        val score = when (val display = UserDisplayScoreMapper.displayScore(state)) {
            is DisplayScore.ProvisionalStable -> display.value0to100
            is DisplayScore.ProvisionalEmerging -> display.value0to100
            DisplayScore.Withheld -> null
        }
        // While CALIBRATING there is no value to show; surface progress toward the FIRST reading
        // (data gathered / data needed) instead of a bare "—". The dash + null vertex still hold.
        val progress = if (state.status == DimensionStatus.CALIBRATING) {
            (state.dataSufficiency * PERCENT_SCALE).roundToInt().coerceIn(0, MAX_PERCENT)
        } else {
            null
        }
        return DimensionUi(
            dimension = state.dimension,
            label = state.dimension.displayLabel(),
            status = state.status,
            statusLabel = state.status.displayLabel(),
            displayValue = score?.toString() ?: EM_DASH, // "—" while calibrating, never "0"
            score0to100 = score,
            trend = state.trend,
            trendLabel = state.trend.displayLabel(),
            trendSymbol = state.trend.displaySymbol(),
            preliminary = state.preliminary,
            calibrationProgress = progress,
            calibrationLabel = calibrationLabel(progress),
            combineWeight = state.confidence * state.dataSufficiency,
        )
    }

    /** First-reading progress hint for a CALIBRATING dimension; empty for measured dimensions. */
    private fun calibrationLabel(progress: Int?): String = when {
        progress == null -> ""
        progress <= 0 -> "Aún sin datos — entrena para empezar tu primera lectura."
        progress >= MAX_PERCENT -> "Casi listo — afinando tu primera lectura."
        else -> "Reuniendo datos · $progress% para tu primera lectura."
    }

    // §33/§34 D4: there is NO global number — the composite is structurally coverage-only. overallUi always
    // reports hasIndex=false + EM_DASH; the caption carries coverage/state (covered vs total areas) when the
    // flag-gated provisional coverage view exists, otherwise a "keep training" prompt. No flag flip can surface a
    // number because ProvisionalGlobalIndex has no scalar.
    private fun overallUi(summary: OverallSummary?): OverallUi = OverallUi(
        indexLabel = EM_DASH,
        hasIndex = false,
        caption = overallCaption(summary, summary?.provisionalIndex),
    )

    /**
     * Overall caption uses the 8-motor product labels, never the internal 9-dimension names (e.g.
     * WORKING_MEMORY/EPISODIC_MEMORY both read "Memoria"). When [provisional] is non-null (flag ON) it EXPOSES
     * covered/total areas explicitly (SCO-014) — a COVERAGE statement, never a composite number.
     */
    private fun overallCaption(summary: OverallSummary?, provisional: ProvisionalGlobalIndex?): String {
        if (summary == null) return "Sigue entrenando para construir tu perfil."
        val strongest = summary.strongest?.toProductMotorLabel()
        val developing = summary.developing?.toProductMotorLabel()
        val areas = if (strongest != null && developing != null) {
            "Más desarrollada: $strongest. A desarrollar: $developing."
        } else {
            "Perfil preliminar en construcción."
        }
        val coverage = provisional?.let {
            val covered = it.coveredDimensions.size
            val total = CognitiveDimension.entries.size
            " Cobertura provisional: $covered de $total áreas."
        }.orEmpty()
        return "$areas$coverage"
    }

    private fun recommendationUi(recommendation: TrainingRecommendation): RecommendationUi {
        // Visible label uses the 8-motor product taxonomy: MEMORIA_EPISODICA folds to "Memoria", so a
        // suggestion never surfaces "Memoria episódica" as a standalone motor.
        val moduleLabel = recommendation.moduleId?.toProductMotorLabel()
        val text = when (recommendation.reason) {
            RecommendationReason.NONE_AVAILABLE -> "Aún no hay módulos disponibles para entrenar."
            RecommendationReason.GATHER_DATA ->
                "Entrena ${moduleLabel.orEmpty()} para reunir más datos de esta área."
            RecommendationReason.DEVELOP_AREA ->
                "Entrena ${moduleLabel.orEmpty()} para desarrollar tu área en progreso."
        }
        return RecommendationUi(reason = recommendation.reason, text = text.trim(), moduleLabel = moduleLabel)
    }
}

/** Convenience for [DimensionStatus] ordering when rendering (established first). */
fun DimensionStatus.sortPriority(): Int = when (this) {
    DimensionStatus.ESTABLISHED -> 0
    DimensionStatus.EMERGING -> 1
    DimensionStatus.CALIBRATING -> 2
}
