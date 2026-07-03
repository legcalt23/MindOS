package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.domain.SpeedVersions
import com.mindos.app.features.mindboost.speed.domain.model.SessionMetrics

/**
 * Macro-categoría DESCRIPTIVA (no clínica, no diagnóstica) del patrón de una corrida.
 * MVP-3 expone 3–5 categorías (NO los 7 perfiles avanzados — eso es Post-MVP). Es
 * **preliminar** y se rotula como tal en la UI.
 */
enum class SpeedMacroCategory {
    /** Datos insuficientes para describir un patrón. */
    CALIBRATING,

    /** Rápido pero con bajo control (impulsividad). */
    FAST_IMPULSIVE,

    /** Lento pero con alto control/precisión. */
    SLOW_PRECISE,

    /** Rendimiento inestable (alta variabilidad). */
    VARIABLE,

    /** Velocidad, control y estabilidad equilibrados. */
    BALANCED,
}

/**
 * Perfil de puntuación de MVP-3. Tres barras 0–1 (§9) + total con techo + macro-categoría.
 *
 * **[preliminary] es SIEMPRE true** mientras el gate de validación física (§3/§15) siga
 * abierto: el score es informativo, no calibrado, y NO es una nota ni autoritativo. La UI debe
 * rotularlo como preliminar. Barras null = "no computable" (nunca 0 silencioso, §7).
 */
data class CognitiveProfileScore(
    val usefulSpeed: Double?,
    val control: Double?,
    val stability: Double?,
    val total: Double?,
    val macroCategory: SpeedMacroCategory,
    val ceilingPenaltyApplied: Boolean,
    val scoreVersion: String = SpeedVersions.SCORE_VERSION,
    val preliminary: Boolean = true,
)

/**
 * Parámetros del modelo de puntuación. NO calibrados (sin norma poblacional): la velocidad
 * útil se auto-referencia contra [usefulSpeedReferenceVce] en lugar de un percentil de
 * población (que requeriría datos validados que aún no existen). Documentado y versionado.
 */
@Suppress("MagicNumber", "LongParameterList") // Defaults = calibración preliminar del §9.
data class SpeedScoreConfig(
    /** Referencia (no calibrada) para mapear VCE → [0,1]. */
    val usefulSpeedReferenceVce: Double = 5.0,
    /** §9: precisión por debajo de la cual se penaliza el total. */
    val ceilingAccuracy: Double = 0.80,
    /** §9: CV por encima del cual se penaliza el total. */
    val ceilingCv: Double = 0.30,
    /** §9: factor de penalización del total (−50%). */
    val ceilingPenaltyFactor: Double = 0.50,
    // Umbrales de macro-clasificación (descriptivos, preliminares).
    val fastControlMax: Double = 0.60,
    val fastSpeedMin: Double = 0.50,
    val preciseControlMin: Double = 0.75,
    val preciseSpeedMax: Double = 0.40,
    val variableStabilityMax: Double = 0.50,
) {
    companion object {
        val DEFAULT: SpeedScoreConfig = SpeedScoreConfig()
    }
}

/**
 * Calcula [CognitiveProfileScore] a partir de [SessionMetrics] (§9). Puro, testeable. No
 * decide adaptación (eso es el staircase) ni emite claims: solo agrega métricas en un perfil
 * informativo y PRELIMINAR.
 *
 * Barras (§9):
 * - **Velocidad Útil** = VCE normalizada (auto-referenciada, no percentil poblacional).
 * - **Precisión/Control** = `(1 − ImpIdx) · PC`.
 * - **Estabilidad** = `(1 − CV) · (1 − tasaLapsos)`.
 *
 * Techo (§9): si `pc < ceilingAccuracy` **o** `cv > ceilingCv`, el total se penaliza ×0.5.
 */
class SpeedScoreEngine(
    private val config: SpeedScoreConfig = SpeedScoreConfig.DEFAULT,
) {

    fun score(metrics: SessionMetrics): CognitiveProfileScore {
        val usefulSpeed = metrics.vce?.let { (it / config.usefulSpeedReferenceVce).coerceIn(0.0, 1.0) }
        val control = ((1.0 - (metrics.impIdx ?: 0.0)) * metrics.pc).coerceIn(0.0, 1.0)
        val stability = metrics.cv?.let { cv -> ((1.0 - cv) * (1.0 - metrics.lapseRate)).coerceIn(0.0, 1.0) }

        val bars = listOfNotNull(usefulSpeed, control, stability)
        val rawTotal = if (bars.isEmpty()) null else bars.sum() / bars.size

        val penalize = metrics.pc < config.ceilingAccuracy ||
            (metrics.cv != null && metrics.cv > config.ceilingCv)
        val total = rawTotal?.let { if (penalize) it * config.ceilingPenaltyFactor else it }

        return CognitiveProfileScore(
            usefulSpeed = usefulSpeed,
            control = control,
            stability = stability,
            total = total,
            macroCategory = classify(usefulSpeed, control, stability),
            ceilingPenaltyApplied = penalize && rawTotal != null,
        )
    }

    private fun classify(usefulSpeed: Double?, control: Double, stability: Double?): SpeedMacroCategory {
        if (usefulSpeed == null || stability == null) return SpeedMacroCategory.CALIBRATING
        return when {
            control < config.fastControlMax && usefulSpeed >= config.fastSpeedMin ->
                SpeedMacroCategory.FAST_IMPULSIVE
            control >= config.preciseControlMin && usefulSpeed < config.preciseSpeedMax ->
                SpeedMacroCategory.SLOW_PRECISE
            stability < config.variableStabilityMax -> SpeedMacroCategory.VARIABLE
            else -> SpeedMacroCategory.BALANCED
        }
    }
}
