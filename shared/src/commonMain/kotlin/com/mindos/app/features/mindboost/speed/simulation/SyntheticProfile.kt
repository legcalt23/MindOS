package com.mindos.app.features.mindboost.speed.simulation

/**
 * Perfil conductual sintético (§12). Define cómo "responde" un usuario simulado para
 * verificar la robustez de las métricas y la persistencia ANTES de cualquier uso físico
 * (acceptance #4/#23). Determinista: el [SyntheticUserRunner] usa un PRNG sembrado.
 *
 * @property baseRtMs TR base de un acierto típico.
 * @property rtSpreadMs amplitud de la variabilidad de TR (jitter ± alrededor de la base).
 * @property accuracy probabilidad de respuesta correcta en trials no-anticipados (0.0–1.0).
 * @property anticipationRate fracción de trials emitidos como anticipación (TR ultra-bajo).
 * @property fatigueOnsetBlock índice de bloque (0-based) a partir del cual aparece fatiga.
 * @property fatigueRtGrowthMsPerBlock crecimiento lineal del TR base por bloque tras el onset.
 * @property fatigueSpreadGrowthMsPerBlock crecimiento de la variabilidad por bloque tras el onset.
 */
data class SyntheticProfile(
    val name: String,
    val baseRtMs: Double,
    val rtSpreadMs: Double,
    val accuracy: Double,
    val anticipationRate: Double,
    val fatigueOnsetBlock: Int = Int.MAX_VALUE,
    val fatigueRtGrowthMsPerBlock: Double = 0.0,
    val fatigueSpreadGrowthMsPerBlock: Double = 0.0,
) {
    companion object {
        /** Muy bajo TR (~200 ms) casi constante, alta impulsividad y error ⇒ PC ~0.55. */
        val IMPULSIVE = SyntheticProfile(
            name = "impulsive",
            baseRtMs = 200.0,
            rtSpreadMs = 35.0,
            accuracy = 0.55,
            anticipationRate = 0.30,
        )

        /** Usuario cauteloso: TR alto (~600 ms) pero PC ~0.98 y casi sin anticipaciones. */
        val SLOW_PRECISE = SyntheticProfile(
            name = "slow_precise",
            baseRtMs = 600.0,
            rtSpreadMs = 30.0,
            accuracy = 0.98,
            anticipationRate = 0.01,
        )

        /** Empieza rápido/estable; a partir del bloque 3 sube TR, variabilidad y lapsos. */
        val FATIGABLE = SyntheticProfile(
            name = "fatigable",
            baseRtMs = 320.0,
            rtSpreadMs = 30.0,
            accuracy = 0.92,
            anticipationRate = 0.03,
            fatigueOnsetBlock = 3,
            fatigueRtGrowthMsPerBlock = 140.0,
            fatigueSpreadGrowthMsPerBlock = 60.0,
        )
    }
}
