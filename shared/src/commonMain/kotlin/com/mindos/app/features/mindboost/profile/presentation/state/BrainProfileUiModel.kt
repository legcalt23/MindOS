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
    /**
     * confidence * dataSufficiency (the same weight the aggregator uses). Presentation-only signal, consumed
     * by [com.mindos.app.features.mindboost.profile.presentation.ProductMotorUiMapper] to weight sub-dimensions
     * when folding to the 8 product motors (e.g. Memoria). 0.0 when unknown -> simple-mean fallback.
     */
    val combineWeight: Double = 0.0,
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

// ESTABLISHED reads "Progresando" (never "Establecido"/"Official"): no ADR-approved equating exists yet for
// the sources that reach this status today, so the label must not claim a validated/final measurement
// (SYSTEM-1 Slice 6-R3, C2.4 item 5).
fun DimensionStatus.displayLabel(): String = when (this) {
    DimensionStatus.CALIBRATING -> "Calibrando"
    DimensionStatus.EMERGING -> "Emergente"
    DimensionStatus.ESTABLISHED -> "Progresando"
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

// --- Product-motor presentation layer: the 8 visible motors folded from the 9 internal dimensions ---

/**
 * The 8 product-facing cognitive motors the user sees. The internal WORKING_MEMORY + EPISODIC_MEMORY split
 * (kept for measurement rigor) folds under the single MEMORIA motor; every other motor is 1:1 with a
 * dimension. Declaration order IS the visible product order defined by the PRD (Enfoque first, Memoria last).
 * See docs/architecture/MINDBOOST_TAXONOMY_MAPPING.md.
 */
enum class ProductMotor { ENFOQUE, DECISIONES, LOGICA, CREATIVIDAD, LENGUAJE, EMOCIONES, VELOCIDAD, MEMORIA }

/** ONE visible product motor. Mirrors [DimensionUi]'s display fields plus optional internal sub-metrics. */
data class ProductMotorUi(
    val motor: ProductMotor,
    val label: String,
    val status: DimensionStatus,
    val statusLabel: String,
    /** "63" for a shown value, or "—" for missing data. NEVER "0". */
    val displayValue: String,
    /** 0..100 for the polygon vertex, or null (no vertex drawn). */
    val score0to100: Int?,
    val trend: ProfileTrend,
    val trendLabel: String,
    val trendSymbol: String,
    val preliminary: Boolean,
    val calibrationProgress: Int?,
    val calibrationLabel: String,
    /** Internal sub-metrics shown under the motor (e.g. Memoria -> Trabajo / Episódica); empty for 1:1 motors. */
    val submetrics: List<ProductSubMetricUi>,
)

/** A named internal sub-metric under a product motor. [displayValue] is "—" while its sub-dimension calibrates. */
data class ProductSubMetricUi(val label: String, val displayValue: String)

/** Non-clinical Spanish product label for a motor (the 8-motor product surface). */
fun ProductMotor.displayLabel(): String = when (this) {
    ProductMotor.ENFOQUE -> "Enfoque"
    ProductMotor.VELOCIDAD -> "Velocidad mental"
    ProductMotor.MEMORIA -> "Memoria"
    ProductMotor.LOGICA -> "Lógica"
    ProductMotor.LENGUAJE -> "Lenguaje"
    ProductMotor.DECISIONES -> "Decisiones"
    ProductMotor.EMOCIONES -> "Emociones"
    ProductMotor.CREATIVIDAD -> "Creatividad"
}

/** The internal dimension a 1:1 motor represents. MEMORIA returns WORKING_MEMORY as a representative only. */
fun ProductMotor.primaryDimension(): CognitiveDimension = when (this) {
    ProductMotor.ENFOQUE -> CognitiveDimension.ATTENTION
    ProductMotor.VELOCIDAD -> CognitiveDimension.PROCESSING_SPEED
    ProductMotor.MEMORIA -> CognitiveDimension.WORKING_MEMORY
    ProductMotor.LOGICA -> CognitiveDimension.REASONING
    ProductMotor.LENGUAJE -> CognitiveDimension.VERBAL
    ProductMotor.DECISIONES -> CognitiveDimension.DECISION_MAKING
    ProductMotor.EMOCIONES -> CognitiveDimension.EMOTIONAL_REGULATION
    ProductMotor.CREATIVIDAD -> CognitiveDimension.CREATIVITY
}

/**
 * The visible product-motor label for a training module. Both MEMORIA and MEMORIA_EPISODICA fold to the
 * single "Memoria" motor, so a recommendation never surfaces "Memoria episódica" as a standalone motor.
 */
fun CognitiveModuleId.toProductMotorLabel(): String = when (this) {
    CognitiveModuleId.ENFOQUE -> "Enfoque"
    CognitiveModuleId.VELOCIDAD -> "Velocidad mental"
    CognitiveModuleId.MEMORIA -> "Memoria"
    CognitiveModuleId.MEMORIA_EPISODICA -> "Memoria"
    CognitiveModuleId.LOGICA -> "Lógica"
    CognitiveModuleId.LENGUAJE -> "Lenguaje"
    CognitiveModuleId.DECISIONES -> "Decisiones"
    CognitiveModuleId.EMOCIONES -> "Emociones"
    CognitiveModuleId.CREATIVIDAD -> "Creatividad"
}

/**
 * The visible product-motor label for an internal dimension. WORKING_MEMORY and EPISODIC_MEMORY both fold to
 * "Memoria", so a main surface (overall summary, radar) never names the internal memory split to the user.
 */
fun CognitiveDimension.toProductMotorLabel(): String = when (this) {
    CognitiveDimension.ATTENTION -> "Enfoque"
    CognitiveDimension.PROCESSING_SPEED -> "Velocidad mental"
    CognitiveDimension.WORKING_MEMORY -> "Memoria"
    CognitiveDimension.EPISODIC_MEMORY -> "Memoria"
    CognitiveDimension.REASONING -> "Lógica"
    CognitiveDimension.VERBAL -> "Lenguaje"
    CognitiveDimension.DECISION_MAKING -> "Decisiones"
    CognitiveDimension.EMOTIONAL_REGULATION -> "Emociones"
    CognitiveDimension.CREATIVITY -> "Creatividad"
}
