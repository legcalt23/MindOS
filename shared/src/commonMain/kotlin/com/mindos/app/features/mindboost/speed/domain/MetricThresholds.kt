package com.mindos.app.features.mindboost.speed.domain

import com.mindos.app.features.mindboost.core.scoring.ScoringVersionRegistry
import kotlinx.serialization.Serializable

/**
 * Umbrales psicométricos versionados (§5). Valores iniciales de calibración, sujetos a
 * recalibración mediante snapshots de versión ([SpeedVersions]). Una corrida persiste
 * los umbrales activos vía [com.mindos.app.features.mindboost.speed.domain.model.VersionSnapshot]
 * para que los datos históricos no pierdan valor al recalibrar.
 *
 * @property anticipationThresholdNanos RT por debajo del cual la respuesta es una
 *   anticipación (100 ms por defecto).
 * @property lapseMultiplier multiplicador sobre la mediana de RT de aciertos para marcar
 *   un lapso (cola lenta).
 * @property highCvThreshold CV por encima del cual la estabilidad se considera baja.
 * @property highAnticipationRateThreshold tasa de anticipación que dispara baja calidad.
 * @property highOmissionRateThreshold tasa de omisión que dispara baja calidad.
 * @property contaminatedFrameDropThreshold fracción de frames perdidos que invalida
 *   (contamina) el bloque/sesión.
 */
@Serializable
// Los defaults SON las constantes de calibración (documentadas arriba); 6 umbrales por diseño.
@Suppress("MagicNumber", "LongParameterList")
data class MetricThresholds(
    val anticipationThresholdNanos: Long = 100_000_000L,
    val lapseMultiplier: Double = 2.5,
    val highCvThreshold: Double = 0.30,
    val highAnticipationRateThreshold: Double = 0.15,
    val highOmissionRateThreshold: Double = 0.20,
    val contaminatedFrameDropThreshold: Double = 0.05,
) {
    companion object {
        /** Conjunto de calibración por defecto de MVP-0. */
        val DEFAULT: MetricThresholds = MetricThresholds()
    }
}

/**
 * Versiones de los parámetros activos, persistidas en cada corrida (acceptance #15/#19).
 * Cambiar cualquier semántica de cálculo obliga a subir la versión correspondiente para
 * preservar la trazabilidad histórica.
 */
object SpeedVersions {
    const val METRICS_VERSION: String = ScoringVersionRegistry.SPEED_METRICS

    /** Staircase pc-driven unidimensional de MVP-1 (Hito 1). */
    const val STAIRCASE_VERSION: String = ScoringVersionRegistry.SPEED_STAIRCASE

    const val DIFFICULTY_MODEL_VERSION: String = ScoringVersionRegistry.SPEED_DIFFICULTY_MODEL

    /**
     * Versión del modelo de puntuación de MVP-3 (Hito 3). El sufijo `-preliminary` señala
     * que el score es informativo y NO está calibrado ni validado físicamente (§3/§15).
     */
    const val SCORE_VERSION: String = ScoringVersionRegistry.SPEED_SCORE

    /** Versión del esquema de exportación de depuración (§16). */
    const val DEBUG_EXPORT_VERSION: String = "v0"
}

/**
 * Motivos estandarizados que justifican una clasificación de calidad `LOW`/`CONTAMINATED`
 * (§8, acceptance #17). Se almacenan como flags string en
 * [com.mindos.app.features.mindboost.speed.domain.model.QualityDecisionLog.reasons].
 */
object QualityReason {
    const val INPUT_PRECISION_MILLIS_DERIVED = "INPUT_PRECISION_MILLIS_DERIVED"
    const val TIMESTAMP_SOURCE_NON_FRAME = "TIMESTAMP_SOURCE_NON_FRAME"
    const val HIGH_CV = "HIGH_CV"
    const val HIGH_ANTICIPATION_RATE = "HIGH_ANTICIPATION_RATE"
    const val HIGH_OMISSION_RATE = "HIGH_OMISSION_RATE"
    const val FRAME_JITTER_DETECTED = "FRAME_JITTER_DETECTED"

    /**
     * No hubo NINGUNA medición de frames en la corrida (sonda no-op, o el display no declaró cadencia,
     * que es la referencia contra la que un drop está definido). No es una acusación: la corrida sigue
     * siendo jugable y NUNCA se marca `CONTAMINATED`/`LOW` por esto — solo se retiene `HIGH`, que
     * afirmaría un dispositivo medido y limpio (`MB-FOCUS-0071`).
     */
    const val FRAME_TIMING_UNMEASURED = "FRAME_TIMING_UNMEASURED"
    const val LOW_TRIAL_COUNT = "LOW_TRIAL_COUNT"
    const val CONTAMINATED_SYSTEM_NOISE = "CONTAMINATED_SYSTEM_NOISE"
}
