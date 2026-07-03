package com.mindos.app.features.mindboost.profile.presentation.state

import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.domain.orchestration.RecommendationReason

/**
 * Presentation model for the brain-profile screen. Built by [BrainProfileUiModelBuilder] from the pure
 * domain [com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile]. Carries display-ready,
 * non-clinical Spanish copy. A CALIBRATING dimension shows `displayValue = "—"` and `score0to100 = null`
 * — NEVER a 0.
 */
data class BrainProfileUiModel(
    val title: String,
    val subtitle: String,
    val overall: OverallUi,
    val dimensions: List<DimensionUi>,
    val recommendation: RecommendationUi,
    val disclaimer: String,
)

data class OverallUi(
    val indexLabel: String,
    val hasIndex: Boolean,
    val caption: String,
)

data class DimensionUi(
    val dimension: CognitiveDimension,
    val label: String,
    val status: DimensionStatus,
    val statusLabel: String,
    /** "63" for a measured value, or "—" while calibrating. Never "0" for missing data. */
    val displayValue: String,
    /** 0..100 for the polygon, or null while calibrating (no vertex drawn). */
    val score0to100: Int?,
    val trend: ProfileTrend,
    val trendLabel: String,
    val trendSymbol: String,
    val preliminary: Boolean,
    /** 0..100 progress toward the FIRST reading; non-null ONLY while CALIBRATING (else null). */
    val calibrationProgress: Int?,
    /** First-reading hint shown only while CALIBRATING (e.g. "Reuniendo datos · 40% …"); empty otherwise. */
    val calibrationLabel: String,
)

data class RecommendationUi(
    val reason: RecommendationReason,
    val text: String,
    val moduleLabel: String?,
)

/** Non-clinical Spanish display label for a dimension. */
fun CognitiveDimension.displayLabel(): String = when (this) {
    CognitiveDimension.ATTENTION -> "Atención"
    CognitiveDimension.PROCESSING_SPEED -> "Velocidad de procesamiento"
    CognitiveDimension.WORKING_MEMORY -> "Memoria de trabajo"
    CognitiveDimension.EPISODIC_MEMORY -> "Memoria episódica"
    CognitiveDimension.REASONING -> "Razonamiento"
    CognitiveDimension.VERBAL -> "Lenguaje"
    CognitiveDimension.DECISION_MAKING -> "Toma de decisiones"
    CognitiveDimension.EMOTIONAL_REGULATION -> "Regulación emocional"
    CognitiveDimension.CREATIVITY -> "Creatividad"
}

/** Non-clinical Spanish display label for a training module. */
fun CognitiveModuleId.displayLabel(): String = when (this) {
    CognitiveModuleId.ENFOQUE -> "Enfoque"
    CognitiveModuleId.VELOCIDAD -> "Velocidad"
    CognitiveModuleId.MEMORIA -> "Memoria"
    CognitiveModuleId.MEMORIA_EPISODICA -> "Memoria episódica"
    CognitiveModuleId.LOGICA -> "Lógica"
    CognitiveModuleId.LENGUAJE -> "Lenguaje"
    CognitiveModuleId.DECISIONES -> "Decisiones"
    CognitiveModuleId.EMOCIONES -> "Emociones"
    CognitiveModuleId.CREATIVIDAD -> "Creatividad"
}

fun DimensionStatus.displayLabel(): String = when (this) {
    DimensionStatus.CALIBRATING -> "Calibrando"
    DimensionStatus.EMERGING -> "Emergente"
    DimensionStatus.ESTABLISHED -> "Establecido"
}

fun ProfileTrend.displayLabel(): String = when (this) {
    ProfileTrend.IMPROVING -> "Mejorando"
    ProfileTrend.STABLE -> "Estable"
    ProfileTrend.DECLINING -> "A la baja"
    ProfileTrend.UNKNOWN -> ""
}

fun ProfileTrend.displaySymbol(): String = when (this) {
    ProfileTrend.IMPROVING -> "↗"
    ProfileTrend.STABLE -> "→"
    ProfileTrend.DECLINING -> "↘"
    ProfileTrend.UNKNOWN -> ""
}
