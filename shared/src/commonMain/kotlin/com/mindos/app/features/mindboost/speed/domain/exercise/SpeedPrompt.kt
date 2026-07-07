package com.mindos.app.features.mindboost.speed.domain.exercise

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId

/**
 * Un estímulo de 2 vías y su respuesta correcta (ground truth). Modelo de dominio puro,
 * compartido por E6/E3/E7/E2 (la pantalla lo renderiza; el holder lo puntúa).
 *
 * @property correctChoice lado correcto para este trial.
 * @property cue contenido a mostrar (flecha / símbolo→candidato / puerta+flecha / regla).
 * @property leftLabel etiqueta del panel izquierdo.
 * @property rightLabel etiqueta del panel derecho.
 * @property revealLabelsOnlyAtStimulus si true, las etiquetas de panel se muestran SOLO desde la fase
 *   Stimulus (no durante el foreperiodo). Lo usa E2 · Comparación, donde las etiquetas SON las magnitudes:
 *   revelarlas antes del onset filtraría la respuesta y contaminaría el TR. Default false ⇒ E3/E6/E7 sin cambio.
 */
data class SpeedTrialPrompt(
    val correctChoice: SpeedChoice,
    val cue: String,
    val leftLabel: String,
    val rightLabel: String,
    val revealLabelsOnlyAtStimulus: Boolean = false,
)

/** Generador de estímulos de 2 vías de un ejercicio. Determinista dado un [SpeedRng]. */
interface SpeedPromptGenerator {
    val exerciseId: SpeedExerciseId
    fun next(rng: SpeedRng): SpeedTrialPrompt

    /**
     * Reinicia el estado interno de secuencia al empezar una corrida. Default no-op: los generadores sin estado
     * (E2/E3/E6/E7) no lo necesitan. E4 · Cambio de Regla lo usa para volver a la primera regla en cada sesión.
     */
    fun reset() {}
}

/** Fuente de aleatoriedad determinista (no usa `Math.random()`). */
interface SpeedRng {
    /** Entero uniforme en [0, bound). */
    fun nextInt(bound: Int): Int
    fun nextBoolean(): Boolean
}

/** PRNG xorshift64 determinista por semilla. */
class XorShiftSpeedRng(seed: Long) : SpeedRng {
    private var state: Long = if (seed == 0L) 1L else seed

    private fun next(): Long {
        var x = state
        x = x xor (x shl SHIFT_A)
        x = x xor (x ushr SHIFT_B)
        x = x xor (x shl SHIFT_C)
        state = x
        return x
    }

    override fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound debe ser > 0." }
        return ((next() ushr 1) % bound).toInt()
    }

    override fun nextBoolean(): Boolean = (next() ushr 1) % 2L == 0L

    private companion object {
        const val SHIFT_A = 13
        const val SHIFT_B = 7
        const val SHIFT_C = 17
    }
}

/** Lado opuesto (para reglas de inversión como E7). */
fun SpeedChoice.opposite(): SpeedChoice =
    if (this == SpeedChoice.LEFT) SpeedChoice.RIGHT else SpeedChoice.LEFT
