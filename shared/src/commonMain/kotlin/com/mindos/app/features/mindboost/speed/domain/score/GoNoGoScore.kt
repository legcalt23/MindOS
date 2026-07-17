package com.mindos.app.features.mindboost.speed.domain.score

/** Versión del scorer de E8 (para trazabilidad; se bumpea si cambia una fórmula o un peso). */
const val GONOGO_SCORE_VERSION: String = "1.1.0-mc9d-r3"

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
 * MC9-D-R3 — nivel de INTEGRIDAD DE TEMPORIZACIÓN de la medición de RT, ESCALONADO (no un cap binario):
 * - [CLEAN]: sin violaciones de temporización (RT físicamente imposibles + taps sin onset = 0).
 * - [MINOR_ANOMALIES]: pocas violaciones (≤ [GoNoGoScorer.MINOR_INTEGRITY_MAX_RATE] del total) ⇒ penalización
 *   PROPORCIONAL; sigue siendo elegible para perfil (que hoy está diferido igualmente), NO para calibración/ranking.
 * - [COMPROMISED]: violaciones sistemáticas ⇒ medición no fiable ⇒ cap fuerte, baja confianza e INELEGIBLE.
 */
enum class TimingIntegrityLevel { CLEAN, MINOR_ANOMALIES, COMPROMISED }

/**
 * Índice PRELIMINAR de E8 (0–1) + patrón descriptivo + elegibilidad honesta. [index] es `null` si la muestra es
 * insuficiente (nunca un 0 fabricado). [preliminary] es SIEMPRE true mientras el gate de validación física siga
 * abierto: informativo, no autoritativo. Los `*CapApplied` documentan por qué el índice quedó limitado.
 *
 * MC9-D-R3 — elegibilidad por CAPA (rule 6: separar los niveles de readiness EN el dato, no solo en prosa):
 * [profileEligible] (contribuir al perfil de bloque), [calibrationEligible] (informar el baseline de 5 sesiones),
 * [rankingEligible] (competición). Todas parten de "índice computable" y suben la barra de integridad/confianza.
 */
data class GoNoGoScore(
    val index: Double?,
    val category: GoNoGoCategory,
    val lowConfidence: Boolean,
    val commissionCapApplied: Boolean,
    val goAccuracyCapApplied: Boolean,
    val anticipationCapApplied: Boolean,
    /** true SII [timingIntegrity] == [TimingIntegrityLevel.COMPROMISED] (medición no fiable). */
    val dataQualityCompromised: Boolean = false,
    val timingIntegrity: TimingIntegrityLevel = TimingIntegrityLevel.CLEAN,
    /** Elegible para calibración/baseline: exige integridad CLEAN, índice computable y confianza suficiente. */
    val calibrationEligible: Boolean = false,
    /** Elegible para contribuir al perfil de bloque (aparte del gate §15): integridad no comprometida + índice. */
    val profileEligible: Boolean = false,
    /** Elegible para ranking/competición: la barra más estricta (CLEAN + confianza + patrón no CALIBRATING). */
    val rankingEligible: Boolean = false,
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

    /** Factor de cap para la peor integridad ([TimingIntegrityLevel.COMPROMISED]): neutraliza RT fabricados. */
    const val DATA_QUALITY_CAP_FACTOR = 0.30

    /** violations/total ≤ este ⇒ [TimingIntegrityLevel.MINOR_ANOMALIES]; por encima ⇒ COMPROMISED. */
    const val MINOR_INTEGRITY_MAX_RATE = 0.10

    /** La penalización PROPORCIONAL de MINOR_ANOMALIES nunca cae por debajo de este factor (un desliz no anula). */
    const val MINOR_INTEGRITY_FLOOR = 0.50

    /**
     * MC9-D-R3 — gate de CALIDAD DE DATOS, ESCALONADO y NO BYPASEABLE. [diagnostics] es OBLIGATORIO (el tipo lo
     * exige en compilación: ningún caller puede saltarse el gate por disciplina humana — rule 4). Las "violaciones
     * de temporización" (RT físicamente imposibles `< 120 ms` + taps sin onset capturado) definen el
     * [TimingIntegrityLevel]: 0 ⇒ CLEAN; pocas (≤ [MINOR_INTEGRITY_MAX_RATE]) ⇒ MINOR (penalización proporcional,
     * un evento aislado de plataforma NO anula toda la sesión); sistemáticas ⇒ COMPROMISED (cap fuerte + baja
     * confianza). `roboticTiming`/`improbablePerfectUnderDifficulty` siguen como diagnostics en `anomalyFlags`
     * (heurísticos: un ejecutor uniforme legítimo no debe ser penalizado), no como auto-invalidación del índice.
     */
    fun score(metrics: GoNoGoMetrics, diagnostics: GoNoGoDiagnostics): GoNoGoScore {
        val violations = diagnostics.impossiblyFastTrials + diagnostics.missingOnsetTaps
        val level = integrityLevel(violations, metrics.totalTrials)
        val insufficient = metrics.goTrials < GoNoGoMetricsMapper.MIN_GO ||
            metrics.noGoTrials < GoNoGoMetricsMapper.MIN_NO_GO
        val commission = metrics.commissionRate.measuredOrNull() ?: 0.0
        val goAccuracy = metrics.goAccuracy.measuredOrNull() ?: 0.0
        val anticipation = metrics.anticipationRate.measuredOrNull() ?: 0.0
        val commissionCap = !insufficient && commission >= COMMISSION_CAP_THRESHOLD
        val goAccuracyCap = !insufficient && goAccuracy < GO_ACCURACY_CAP_THRESHOLD
        val anticipationCap = !insufficient && anticipation >= ANTICIPATION_CAP_THRESHOLD
        val lowConfidence = insufficient || metrics.noGoTrials < CONFIDENT_NO_GO ||
            level == TimingIntegrityLevel.COMPROMISED
        val index = if (insufficient) {
            null
        } else {
            cappedIndex(metrics, commissionCap, goAccuracyCap, anticipationCap, level, violations)
        }
        val category = if (insufficient) GoNoGoCategory.CALIBRATING else classify(metrics, commission, anticipation)
        val eligibility = eligibility(index, level, lowConfidence, category)
        return GoNoGoScore(
            index = index,
            category = category,
            lowConfidence = lowConfidence,
            commissionCapApplied = commissionCap,
            goAccuracyCapApplied = goAccuracyCap,
            anticipationCapApplied = anticipationCap,
            dataQualityCompromised = level == TimingIntegrityLevel.COMPROMISED,
            timingIntegrity = level,
            calibrationEligible = eligibility.calibration,
            profileEligible = eligibility.profile,
            rankingEligible = eligibility.ranking,
        )
    }

    private data class Eligibility(val profile: Boolean, val calibration: Boolean, val ranking: Boolean)

    /** Elegibilidad por capa: profile (integridad no comprometida) ⊂ calibration (CLEAN + confianza) ⊂ ranking. */
    private fun eligibility(
        index: Double?,
        level: TimingIntegrityLevel,
        lowConfidence: Boolean,
        category: GoNoGoCategory,
    ): Eligibility {
        if (index == null) return Eligibility(profile = false, calibration = false, ranking = false)
        val calibration = level == TimingIntegrityLevel.CLEAN && !lowConfidence
        // `category != CALIBRATING` is defensive: with a non-null index `classify()` never yields CALIBRATING (that
        // value only accompanies index==null, already short-circuited above). Kept explicit so the invariant is
        // visible and survives any future change to classify()/the insufficiency branch.
        return Eligibility(
            profile = level != TimingIntegrityLevel.COMPROMISED,
            calibration = calibration,
            ranking = calibration && category != GoNoGoCategory.CALIBRATING,
        )
    }

    /** Nivel de integridad escalonado a partir del nº de violaciones de temporización sobre el total de trials. */
    private fun integrityLevel(violations: Int, total: Int): TimingIntegrityLevel = when {
        violations == 0 -> TimingIntegrityLevel.CLEAN
        total > 0 && violations.toDouble() / total <= MINOR_INTEGRITY_MAX_RATE ->
            TimingIntegrityLevel.MINOR_ANOMALIES
        else -> TimingIntegrityLevel.COMPROMISED
    }

    /** Factor multiplicativo por integridad: CLEAN=1.0; MINOR=proporcional (piso MINOR_INTEGRITY_FLOOR); COMP=0.30. */
    private fun integrityFactor(level: TimingIntegrityLevel, violations: Int, total: Int): Double = when (level) {
        TimingIntegrityLevel.CLEAN -> 1.0
        TimingIntegrityLevel.MINOR_ANOMALIES -> {
            val factor = if (total > 0) 1.0 - violations.toDouble() / total else MINOR_INTEGRITY_FLOOR
            factor.coerceIn(MINOR_INTEGRITY_FLOOR, 1.0)
        }
        TimingIntegrityLevel.COMPROMISED -> DATA_QUALITY_CAP_FACTOR
    }

    @Suppress("LongParameterList") // Cap inputs are all distinct, named, and computed once in score().
    private fun cappedIndex(
        metrics: GoNoGoMetrics,
        commissionCap: Boolean,
        goAccuracyCap: Boolean,
        anticipationCap: Boolean,
        level: TimingIntegrityLevel,
        violations: Int,
    ): Double? {
        val raw = rawIndex(metrics) ?: return null
        val afterCaps = applyCaps(raw, commissionCap, goAccuracyCap, anticipationCap)
        return (afterCaps * integrityFactor(level, violations, metrics.totalTrials)).coerceIn(0.0, 1.0)
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
