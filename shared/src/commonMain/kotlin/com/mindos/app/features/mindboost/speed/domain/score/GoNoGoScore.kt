package com.mindos.app.features.mindboost.speed.domain.score

/** Versión del scorer de E8 (para trazabilidad; se bumpea si cambia una fórmula o un peso). */
const val GONOGO_SCORE_VERSION: String = "1.0.0-mc9d"

/** Patrón DESCRIPTIVO de la corrida de E8 · Go/No-Go Velocity (lenguaje funcional, no sanitario). */
enum class GoNoGoCategory {
    /** Muestra insuficiente para describir un patrón. */
    CALIBRATING,

    /** Muchas comisiones en NO-GO — respuestas que no se inhibieron. */
    IMPULSIVE_COMMISSION,

    /** Muchas omisiones en GO — señales GO sin responder. */
    OMISSION_PRONE,

    /** Muchas respuestas prematuras (antes de confirmar el estímulo). */
    ANTICIPATORY,

    /** Rendimiento inestable (alta variabilidad del TR). */
    UNSTABLE,

    /** Buen control inhibitorio con pocas comisiones. */
    STRONG_CONTROL,

    /** Velocidad, control e inhibición equilibrados. */
    BALANCED,
}

/**
 * Índice PRELIMINAR de E8 (0–1) + patrón descriptivo. [index] es `null` si la muestra es insuficiente (nunca un 0
 * fabricado). [preliminary] es SIEMPRE true mientras el gate de validación física siga abierto: informativo, no
 * autoritativo. Los `*CapApplied` documentan por qué el índice quedó limitado (transparencia, no castigo).
 */
data class GoNoGoScore(
    val index: Double?,
    val category: GoNoGoCategory,
    val lowConfidence: Boolean,
    val commissionCapApplied: Boolean,
    val goAccuracyCapApplied: Boolean,
    val anticipationCapApplied: Boolean,
    val preliminary: Boolean = true,
    val version: String = GONOGO_SCORE_VERSION,
)

/**
 * Scorer honesto de E8. Combina componentes MEDIDOS con pesos, renormalizando sobre los disponibles (una señal
 * `Unavailable` se descarta, no se rellena con 0). Luego aplica TECHOS multiplicativos: comisión alta, precisión
 * GO baja o anticipación alta limitan fuertemente el índice. Invariante de producto (§7): la velocidad en GO NO
 * compensa las comisiones en NO-GO — tocar casi todo o no tocar nada puntúa bajo aunque el TR de GO sea excelente.
 *
 * El componente central `inhibitoryControlIndex` es precisión BALANCEADA (tap-all ⇒ 0.5, tap-none ⇒ 0.5).
 */
object GoNoGoScorer {

    // Pesos (suman 1.0). Ajustables con justificación (§7).
    const val W_INHIBITORY = 0.35
    const val W_GO_ACCURACY = 0.25
    const val W_GO_SPEED = 0.15
    const val W_STABILITY = 0.10
    const val W_DIFFICULTY_CONTROL = 0.10
    const val W_ANTICIPATION_CONTROL = 0.05

    // Techos: umbral que dispara el cap y factor multiplicativo aplicado.
    const val COMMISSION_CAP_THRESHOLD = 0.20
    const val COMMISSION_CAP_FACTOR = 0.30
    const val GO_ACCURACY_CAP_THRESHOLD = 0.60
    const val GO_ACCURACY_CAP_FACTOR = 0.45
    const val ANTICIPATION_CAP_THRESHOLD = 0.20
    const val ANTICIPATION_CAP_FACTOR = 0.50

    // Normalización de velocidad correcta (no calibrada): rt <= FAST ⇒ 1.0; rt >= SLOW ⇒ 0.0.
    const val SPEED_REF_FAST_MS = 250.0
    const val SPEED_REF_SLOW_MS = 650.0

    // Umbrales de clasificación descriptiva.
    const val HIGH_COMMISSION = 0.20
    const val HIGH_OMISSION = 0.20
    const val HIGH_ANTICIPATION = 0.20
    const val LOW_STABILITY = 0.60
    const val STRONG_CONTROL_MIN = 0.85
    const val LOW_COMMISSION = 0.05

    /** NO-GO por debajo del cual el índice es de baja confianza (muestra de inhibición pequeña). */
    const val CONFIDENT_NO_GO = 8

    fun score(metrics: GoNoGoMetrics): GoNoGoScore {
        val insufficient = metrics.goTrials < GoNoGoMetricsMapper.MIN_GO ||
            metrics.noGoTrials < GoNoGoMetricsMapper.MIN_NO_GO
        if (insufficient) {
            return GoNoGoScore(
                index = null,
                category = GoNoGoCategory.CALIBRATING,
                lowConfidence = true,
                commissionCapApplied = false,
                goAccuracyCapApplied = false,
                anticipationCapApplied = false,
            )
        }
        val raw = rawIndex(metrics)
        val commission = metrics.commissionRate.measuredOrNull() ?: 0.0
        val goAccuracy = metrics.goAccuracy.measuredOrNull() ?: 0.0
        val anticipation = metrics.anticipationRate.measuredOrNull() ?: 0.0
        val commissionCap = commission >= COMMISSION_CAP_THRESHOLD
        val goAccuracyCap = goAccuracy < GO_ACCURACY_CAP_THRESHOLD
        val anticipationCap = anticipation >= ANTICIPATION_CAP_THRESHOLD
        return GoNoGoScore(
            index = raw?.let { applyCaps(it, commissionCap, goAccuracyCap, anticipationCap) },
            category = classify(metrics, commission, anticipation),
            lowConfidence = metrics.noGoTrials < CONFIDENT_NO_GO,
            commissionCapApplied = commissionCap && raw != null,
            goAccuracyCapApplied = goAccuracyCap && raw != null,
            anticipationCapApplied = anticipationCap && raw != null,
        )
    }

    private fun rawIndex(metrics: GoNoGoMetrics): Double? {
        val speedNorm = metrics.correctGoSpeedMs.measuredOrNull()?.let { normalizeSpeed(it) }
        val antControl = metrics.anticipationRate.measuredOrNull()?.let { (1.0 - it).coerceIn(0.0, 1.0) }
        val parts = listOf(
            metrics.inhibitoryControlIndex.measuredOrNull() to W_INHIBITORY,
            metrics.goAccuracy.measuredOrNull() to W_GO_ACCURACY,
            speedNorm to W_GO_SPEED,
            metrics.stabilityIndex.measuredOrNull() to W_STABILITY,
            metrics.difficultyAdjustedControl.measuredOrNull() to W_DIFFICULTY_CONTROL,
            antControl to W_ANTICIPATION_CONTROL,
        ).filter { it.first != null }
        val weightSum = parts.sumOf { it.second }
        return if (parts.isEmpty() || weightSum <= 0.0) {
            null
        } else {
            parts.sumOf { (it.first ?: 0.0) * it.second } / weightSum
        }
    }

    private fun applyCaps(
        index: Double,
        commissionCap: Boolean,
        goAccuracyCap: Boolean,
        anticipationCap: Boolean,
    ): Double {
        var value = index
        if (commissionCap) value *= COMMISSION_CAP_FACTOR
        if (goAccuracyCap) value *= GO_ACCURACY_CAP_FACTOR
        if (anticipationCap) value *= ANTICIPATION_CAP_FACTOR
        return value.coerceIn(0.0, 1.0)
    }

    private fun normalizeSpeed(rtMs: Double): Double =
        ((SPEED_REF_SLOW_MS - rtMs) / (SPEED_REF_SLOW_MS - SPEED_REF_FAST_MS)).coerceIn(0.0, 1.0)

    private fun classify(metrics: GoNoGoMetrics, commission: Double, anticipation: Double): GoNoGoCategory {
        val stability = metrics.stabilityIndex.measuredOrNull()
        val omission = metrics.omissionRate.measuredOrNull() ?: 0.0
        val inhibitory = metrics.inhibitoryControlIndex.measuredOrNull() ?: 0.0
        return when {
            commission >= HIGH_COMMISSION -> GoNoGoCategory.IMPULSIVE_COMMISSION
            omission >= HIGH_OMISSION -> GoNoGoCategory.OMISSION_PRONE
            anticipation >= HIGH_ANTICIPATION -> GoNoGoCategory.ANTICIPATORY
            stability != null && stability < LOW_STABILITY -> GoNoGoCategory.UNSTABLE
            inhibitory >= STRONG_CONTROL_MIN && commission <= LOW_COMMISSION -> GoNoGoCategory.STRONG_CONTROL
            else -> GoNoGoCategory.BALANCED
        }
    }
}
