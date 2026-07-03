package com.mindos.app.features.mindboost.speed.domain

/**
 * Estimador de dificultad previo al trial (§4). Firma fiel al plan.
 *
 * MVP-0: dificultad FIJA, sin adaptación. La regla crítica del §7 (acceptance #5) es que
 * `deadlineNanos` nullable NUNCA entra en aritmética: el estimador omite por completo el
 * componente de presión temporal si es null, y jamás calcula `1/null`.
 */
interface DifficultyEstimator {
    /**
     * @param cce dificultad actual (Carga Cognitiva Efectiva, 0.0–1.0). En MVP-0 es fija.
     * @param deadlineNanos deadline de respuesta opcional; si es null, se omite la presión.
     * @return la dificultad estimada para el trial.
     */
    fun estimateDifficultyBefore(cce: Double, deadlineNanos: Long?): Double
}

/**
 * Estimador fijo de MVP-0: devuelve la `cce` recibida sin modificarla. El componente de
 * presión temporal está deshabilitado en MVP-0; la rama nula de `deadlineNanos` se trata
 * de forma explícita (no participa en ningún cálculo), satisfaciendo acceptance #5.
 */
class FixedDifficultyEstimator : DifficultyEstimator {
    // deadlineNanos es parte del contrato de la interfaz pero, por diseño de MVP-0, NO
    // participa en ningún cálculo (componente de presión temporal deshabilitado). Es la
    // realización directa de acceptance #5: la rama nullable nunca toca aritmética.
    @Suppress("UnusedParameter")
    override fun estimateDifficultyBefore(cce: Double, deadlineNanos: Long?): Double =
        cce.coerceIn(0.0, 1.0)
}
