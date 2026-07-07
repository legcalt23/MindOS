package com.mindos.app.features.mindboost.speed.domain.exercise

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId

/**
 * Configuración de dificultad de CONTENIDO de [NumberPulseCompareGenerator] (MC9-A). La cercanía entre
 * magnitudes ([minGap]/[maxGap]: un gap pequeño es más difícil de discriminar) y el rango numérico
 * ([minValue]/[maxValue]) fijan cuán exigente es la comparación. La presión temporal (el `deadline` que
 * gobierna el `StaircaseEngine`) sigue siendo la palanca ADAPTATIVA en vivo, igual que en E3/E6/E7: así el
 * ejercicio reutiliza el bucle de 2 vías sin cambiar el motor de sesión.
 */
data class NumberPulseConfig(
    val minValue: Int = 1,
    val maxValue: Int = 9,
    val minGap: Int = 1,
    val maxGap: Int = 8,
) {
    init {
        require(minValue < maxValue) { "minValue debe ser < maxValue." }
        require(minGap in 1..(maxValue - minValue)) { "minGap debe estar en 1..(maxValue-minValue)." }
        require(maxGap in minGap..(maxValue - minValue)) { "maxGap debe estar en minGap..(maxValue-minValue)." }
    }

    companion object {
        val DEFAULT: NumberPulseConfig = NumberPulseConfig()
    }
}

/** Par de magnitudes de un trial y el lado correcto (el mayor). Ground truth para el holder y los tests. */
data class NumberPulsePair(val left: Int, val right: Int, val correct: SpeedChoice)

/**
 * Number Pulse Compare (E2 · Comparación) — comparación rápida de magnitudes bajo presión temporal. Muestra
 * dos números; el usuario toca el lado del MAYOR. "La velocidad sin precisión no se premia": una respuesta
 * fuera del `deadline` cuenta como incorrecta (bucle de 2 vías) y el motor de score preliminar penaliza la
 * baja precisión — este generador solo produce el contenido, sin score ni claims.
 *
 * Las dos magnitudes SON las etiquetas de los paneles (izquierdo/derecho), alineadas con el área táctil real;
 * la señal ([SpeedTrialPrompt.cue]) lleva solo la REGLA ("¿Cuál es mayor?"). Con [revealLabelsOnlyAtStimulus]
 * = true, la pantalla revela los números SOLO desde la fase Stimulus (no en el foreperiodo), para que el TR
 * mida discriminación+decisión desde el onset y no una respuesta pre-decidida. Determinista dado un [SpeedRng].
 */
class NumberPulseCompareGenerator(
    private val config: NumberPulseConfig = NumberPulseConfig.DEFAULT,
) : SpeedPromptGenerator {

    override val exerciseId: SpeedExerciseId = SpeedExerciseId.E2_COMPARA

    /** Dos magnitudes distintas con |diff| ∈ [minGap, maxGap], ambas en [minValue, maxValue]. */
    fun pair(rng: SpeedRng): NumberPulsePair {
        val span = config.maxValue - config.minValue
        val gapSpan = config.maxGap - config.minGap
        val gap = config.minGap + if (gapSpan > 0) rng.nextInt(gapSpan + 1) else 0
        val low = config.minValue + rng.nextInt(span - gap + 1)
        val high = low + gap
        val leftIsHigh = rng.nextBoolean()
        return NumberPulsePair(
            left = if (leftIsHigh) high else low,
            right = if (leftIsHigh) low else high,
            correct = if (leftIsHigh) SpeedChoice.LEFT else SpeedChoice.RIGHT,
        )
    }

    override fun next(rng: SpeedRng): SpeedTrialPrompt {
        val p = pair(rng)
        return SpeedTrialPrompt(
            correctChoice = p.correct,
            cue = RULE_CUE,
            leftLabel = p.left.toString(),
            rightLabel = p.right.toString(),
            revealLabelsOnlyAtStimulus = true,
        )
    }

    private companion object {
        const val RULE_CUE = "¿Cuál es mayor?"
    }
}
