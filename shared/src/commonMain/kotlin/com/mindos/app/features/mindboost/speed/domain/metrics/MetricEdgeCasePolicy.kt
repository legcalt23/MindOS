// Las reglas de bordes son funciones puras con guard-clauses (varios `return` tempranos
// de null = "no computable"); es la forma más legible. Se suprime ReturnCount.
@file:Suppress("ReturnCount")

package com.mindos.app.features.mindboost.speed.domain.metrics

/**
 * Política de casos límite de las métricas (§7). ES la autoridad de cómo se trata cada
 * borde — las funciones son puras y se testean directamente (acceptance #14).
 *
 * Regla general (§7): **ninguna métrica nula se convierte silenciosamente en 0**. Un
 * nulo en VCE/ImpIdx/CV significa "no computable", nunca "valor bajo". Las capas
 * superiores manejan los nulos explícitamente.
 *
 * Tabla de comportamiento implementada:
 * | Situación | Resultado |
 * |---|---|
 * | 0 trials en bloque | bloque descartado (lo decide [MetricCalculator], devuelve null) |
 * | 0 trials correctos | pc=0.0, rtMedian=null, cv=null, vce=null |
 * | 1 trial correcto | pc calculable, rtMedian=ese trial, cv=null (n=1), vce=null |
 * | 0 trials no-go | faRate=null; ImpIdx por re-normalización sin no-go |
 * | 0 errores | fastErrorShare=0.0 (definido, no null) |
 * | todos anticipaciones | pc/rtMedian solo sobre no-anticipaciones; 0 → como 0 correctos |
 * | deadlineNanos=null | no entra en aritmética; el estimador omite presión temporal |
 */
object MetricEdgeCasePolicy {

    /** Peso exponencial de la precisión en VCE. */
    const val BETA = 2.0

    /** Peso de penalización por variabilidad en VCE. */
    const val LAMBDA = 0.7

    /** Re-normalización de ImpIdx sin condición no-go: wb' = wb/(wb+wc) = 0.3/0.5. */
    const val IMP_WB_PRIME = 0.6

    /** Re-normalización de ImpIdx sin condición no-go: wc' = wc/(wb+wc) = 0.2/0.5. */
    const val IMP_WC_PRIME = 0.4

    private const val NANOS_PER_SECOND = 1_000_000_000.0

    /**
     * Proporción de aciertos sobre trials válidos (no-anticipación). Con 0 válidos se
     * trata como "0 correctos" ⇒ 0.0 (caso "todos anticipaciones").
     */
    fun proportionCorrect(correctCount: Int, validCount: Int): Double =
        if (validCount <= 0) 0.0 else correctCount.toDouble() / validCount

    /**
     * Coeficiente de variación CV = SD / Mean (§17). Null si SD o media no son
     * computables (n<2) o la media no es positiva.
     */
    fun coefficientOfVariation(sd: Double?, mean: Double?): Double? {
        if (sd == null || mean == null || mean <= 0.0) return null
        return sd / mean
    }

    /**
     * Tasa de falsas alarmas. **null** cuando no hay condición no-go (E1/E6 en MVP-0):
     * no es 0.0, es "no aplicable / no computable".
     */
    fun falseAlarmRate(falseAlarms: Int, noGoTrials: Int): Double? =
        if (noGoTrials <= 0) null else falseAlarms.toDouble() / noGoTrials

    /**
     * Tasa de omisiones sobre los intentos presentados. Con 0 intentos ⇒ 0.0. En MVP-0
     * E1/E6 usan diseño de respuesta obligatoria (sin canal de omisión) ⇒ 0.0.
     */
    fun omissionRate(omissions: Int, presentedTrials: Int): Double =
        if (presentedTrials <= 0) 0.0 else omissions.toDouble() / presentedTrials

    /**
     * Fracción de errores cometidos con TR menor que la mediana de TR de los aciertos.
     * - Sin errores ⇒ 0.0 (definido: no hay errores rápidos si no hay errores).
     * - Con errores pero sin mediana de aciertos (0 correctos) ⇒ null (no computable).
     */
    fun fastErrorShare(errorRtsNanos: List<Double>, hitMedianNanos: Double?): Double? {
        if (errorRtsNanos.isEmpty()) return 0.0
        if (hitMedianNanos == null) return null
        val fast = errorRtsNanos.count { it < hitMedianNanos }
        return fast.toDouble() / errorRtsNanos.size
    }

    /**
     * Tasa de lapsos = fracción de TR de aciertos por encima de
     * `lapseMultiplier × mediana(TR crudos de aciertos)`. Se calcula sobre TR CRUDOS
     * (no filtrados por MAD): un lapso es una respuesta legítimamente lenta, no un
     * artefacto (CLAUDE.md 2026-06-04). Sin aciertos ⇒ 0.0 (no hay lapsos).
     */
    fun lapseRate(rawHitRtsNanos: List<Double>, lapseMultiplier: Double): Double {
        val median = SpeedRtStatistics.medianOrNull(rawHitRtsNanos) ?: return 0.0
        val threshold = lapseMultiplier * median
        val lapses = rawHitRtsNanos.count { it > threshold }
        return lapses.toDouble() / rawHitRtsNanos.size
    }

    /** Cuenta de lapsos crudos (para marcar `isLapse` y para inspección). */
    fun lapseCount(rawHitRtsNanos: List<Double>, lapseMultiplier: Double): Int {
        val median = SpeedRtStatistics.medianOrNull(rawHitRtsNanos) ?: return 0
        val threshold = lapseMultiplier * median
        return rawHitRtsNanos.count { it > threshold }
    }

    /**
     * Índice de Impulsividad en ejercicios sin no-go (E1/E6), con pesos re-normalizados
     * (§7): `ImpIdx = 0.6·anticipationRate + 0.4·fastErrorShare`. Null si
     * `fastErrorShare` es null (no computable).
     */
    fun impulsivityIndexNoNoGo(anticipationRate: Double, fastErrorShare: Double?): Double? {
        if (fastErrorShare == null) return null
        return IMP_WB_PRIME * anticipationRate + IMP_WC_PRIME * fastErrorShare
    }

    /**
     * Velocidad Cognitiva Efectiva (§7), **informativa** (§6):
     * `VCE = pc^beta / (rtMedianSeconds · (1 + lambda·cv))`.
     * Null estricto en MVP-0 si pc==0, rtMedianNanos==null o cv==null.
     */
    fun vce(pc: Double, rtMedianNanos: Double?, cv: Double?): Double? {
        if (pc <= 0.0 || rtMedianNanos == null || cv == null) return null
        val rtMedianSeconds = rtMedianNanos / NANOS_PER_SECOND
        if (rtMedianSeconds <= 0.0) return null
        val denominator = rtMedianSeconds * (1.0 + LAMBDA * cv)
        if (denominator <= 0.0) return null
        return pcPow(pc) / denominator
    }

    /** pc^beta con beta=2.0 (evita `kotlin.math.pow` para mantener el módulo sin imports extra). */
    private fun pcPow(pc: Double): Double = pc * pc
}
