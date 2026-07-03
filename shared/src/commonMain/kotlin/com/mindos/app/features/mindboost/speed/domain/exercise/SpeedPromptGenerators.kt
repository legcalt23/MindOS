package com.mindos.app.features.mindboost.speed.domain.exercise

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId

/**
 * Generadores de trials de 2 vías de MVP-2. Mecánicas deterministas y testeables; el plan
 * nombra E3/E7 pero no fija su mecánica, así que estas son interpretaciones razonables
 * (verificación de código / regla condicional) que reutilizan la infraestructura de 2 vías.
 * Sin score ni claims.
 */

/**
 * E6 Multivía — elección-reacción simple: una flecha indica el lado correcto. (Se modela
 * como generador para unificar el bucle de 2 vías con E3/E7.)
 */
class MultiviaGenerator : SpeedPromptGenerator {
    override val exerciseId: SpeedExerciseId = SpeedExerciseId.E6_MULTIVIA

    override fun next(rng: SpeedRng): SpeedTrialPrompt {
        val side = if (rng.nextBoolean()) SpeedChoice.LEFT else SpeedChoice.RIGHT
        return SpeedTrialPrompt(
            correctChoice = side,
            cue = if (side == SpeedChoice.LEFT) "◀" else "▶",
            leftLabel = "Izquierda",
            rightLabel = "Derecha",
        )
    }
}

/**
 * E3 Cifrado — verificación de código: un símbolo y un valor candidato; el usuario juzga si
 * el candidato coincide con la clave [key]. `correctChoice = LEFT` (coincide) / `RIGHT`
 * (no coincide), ~50% cada uno. La pantalla muestra la clave de forma persistente.
 */
class CifradoGenerator(
    val key: Map<String, String> = DEFAULT_KEY,
) : SpeedPromptGenerator {
    override val exerciseId: SpeedExerciseId = SpeedExerciseId.E3_CIFRADO

    init {
        require(key.size >= 2) { "La clave de cifrado necesita >= 2 entradas." }
    }

    override fun next(rng: SpeedRng): SpeedTrialPrompt {
        val symbols = key.keys.toList()
        val symbol = symbols[rng.nextInt(symbols.size)]
        val trueValue = key.getValue(symbol)
        val matches = rng.nextBoolean()
        val candidate = if (matches) trueValue else wrongValue(trueValue, rng)
        return SpeedTrialPrompt(
            correctChoice = if (matches) SpeedChoice.LEFT else SpeedChoice.RIGHT,
            cue = "$symbol → $candidate",
            leftLabel = "Coincide",
            rightLabel = "No coincide",
        )
    }

    private fun wrongValue(trueValue: String, rng: SpeedRng): String {
        val others = key.values.distinct().filter { it != trueValue }
        return others[rng.nextInt(others.size)]
    }

    companion object {
        val DEFAULT_KEY: Map<String, String> = mapOf("▲" to "1", "●" to "2", "■" to "3", "◆" to "4")
    }
}

/**
 * E7 Compuerta — regla condicional: una flecha y una puerta. Puerta abierta ⇒ pulsa la
 * dirección de la flecha; puerta cerrada ⇒ pulsa la dirección OPUESTA (inversión de regla).
 */
class CompuertaGenerator : SpeedPromptGenerator {
    override val exerciseId: SpeedExerciseId = SpeedExerciseId.E7_COMPUERTA

    override fun next(rng: SpeedRng): SpeedTrialPrompt {
        val arrow = if (rng.nextBoolean()) SpeedChoice.LEFT else SpeedChoice.RIGHT
        val gateOpen = rng.nextBoolean()
        val correct = if (gateOpen) arrow else arrow.opposite()
        val arrowCue = if (arrow == SpeedChoice.LEFT) "◀" else "▶"
        val gateCue = if (gateOpen) "puerta abierta" else "puerta cerrada"
        return SpeedTrialPrompt(
            correctChoice = correct,
            cue = "$arrowCue · $gateCue",
            leftLabel = "Izquierda",
            rightLabel = "Derecha",
        )
    }
}
