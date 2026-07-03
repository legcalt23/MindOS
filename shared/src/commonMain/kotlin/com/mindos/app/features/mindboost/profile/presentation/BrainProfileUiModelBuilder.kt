package com.mindos.app.features.mindboost.profile.presentation

import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.DimensionState
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile
import com.mindos.app.features.mindboost.profile.domain.model.OverallSummary
import com.mindos.app.features.mindboost.profile.domain.orchestration.RecommendationReason
import com.mindos.app.features.mindboost.profile.domain.orchestration.TrainingRecommendation
import com.mindos.app.features.mindboost.profile.presentation.state.BrainProfileUiModel
import com.mindos.app.features.mindboost.profile.presentation.state.DimensionUi
import com.mindos.app.features.mindboost.profile.presentation.state.OverallUi
import com.mindos.app.features.mindboost.profile.presentation.state.RecommendationUi
import com.mindos.app.features.mindboost.profile.presentation.state.displayLabel
import com.mindos.app.features.mindboost.profile.presentation.state.displaySymbol
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
        val score = state.value?.let { (it * PERCENT_SCALE).roundToInt() }
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
        )
    }

    /** First-reading progress hint for a CALIBRATING dimension; empty for measured dimensions. */
    private fun calibrationLabel(progress: Int?): String = when {
        progress == null -> ""
        progress <= 0 -> "Aún sin datos — entrena para empezar tu primera lectura."
        progress >= MAX_PERCENT -> "Casi listo — afinando tu primera lectura."
        else -> "Reuniendo datos · $progress% para tu primera lectura."
    }

    private fun overallUi(summary: OverallSummary?): OverallUi {
        val composite = summary?.compositeIndex
        if (composite == null) {
            return OverallUi(
                indexLabel = EM_DASH,
                hasIndex = false,
                caption = "Sigue entrenando para construir tu perfil.",
            )
        }
        val strongest = summary.strongest?.displayLabel()
        val developing = summary.developing?.displayLabel()
        val caption = if (strongest != null && developing != null) {
            "Más desarrollada: $strongest. A desarrollar: $developing."
        } else {
            "Perfil preliminar en construcción."
        }
        return OverallUi(
            indexLabel = (composite * PERCENT_SCALE).roundToInt().toString(),
            hasIndex = true,
            caption = caption,
        )
    }

    private fun recommendationUi(recommendation: TrainingRecommendation): RecommendationUi {
        val moduleLabel = recommendation.moduleId?.displayLabel()
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
