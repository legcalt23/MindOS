package com.mindos.app.features.mindboost.speed.domain.exercise

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId

/** La dimensión que importa en el trial actual (la "regla activa"). */
enum class SpeedRule { COLOR, SHAPE }

/**
 * Estímulo de 2 dimensiones × 2 valores. Color ∈ {azul, naranja}, forma ∈ {círculo, triángulo}. La regla activa
 * decide qué dimensión determina la respuesta; la otra es irrelevante (interferencia).
 */
data class RuleSwitchStimulus(val colorIsBlue: Boolean, val shapeIsCircle: Boolean)

/**
 * MC9-B — lógica PURA de E4 · Cambio de Regla (Rule Switch Sprint). Reglas: COLOR ⇒ azul=izquierda / naranja=
 * derecha; FORMA ⇒ círculo=izquierda / triángulo=derecha. La regla activa alterna cada `trialsPerRule` trials.
 * Todo determinista y testeable sin dispositivo. Sin score ni claims.
 */
object RuleSwitchLogic {

    /** Respuesta correcta bajo [rule] para [stimulus]. */
    fun correctChoice(rule: SpeedRule, stimulus: RuleSwitchStimulus): SpeedChoice = when (rule) {
        SpeedRule.COLOR -> if (stimulus.colorIsBlue) SpeedChoice.LEFT else SpeedChoice.RIGHT
        SpeedRule.SHAPE -> if (stimulus.shapeIsCircle) SpeedChoice.LEFT else SpeedChoice.RIGHT
    }

    /** Regla activa en el trial [index] (0-based): alterna COLOR/FORMA cada [trialsPerRule]. */
    fun ruleAt(index: Int, trialsPerRule: Int): SpeedRule =
        if ((index / trialsPerRule) % 2 == 0) SpeedRule.COLOR else SpeedRule.SHAPE

    /** Un trial es de CAMBIO si la regla difiere del trial anterior (primer trial de un bloque de regla nuevo). */
    fun isSwitch(index: Int, trialsPerRule: Int): Boolean = index > 0 && index % trialsPerRule == 0

    /**
     * Error de PERSEVERACIÓN: la respuesta es incorrecta bajo la regla activa PERO coincide con la que habría sido
     * correcta bajo la regla anterior (el usuario siguió aplicando la regla previa). Solo tiene sentido cuando la
     * regla cambió y el estímulo discrimina entre ambas reglas.
     */
    fun isPerseveration(
        active: SpeedRule,
        previous: SpeedRule,
        stimulus: RuleSwitchStimulus,
        chosen: SpeedChoice,
    ): Boolean {
        if (active == previous) return false
        val correct = correctChoice(active, stimulus)
        val underPrevious = correctChoice(previous, stimulus)
        return chosen != correct && chosen == underPrevious
    }
}

/**
 * Dificultad de CONTENIDO de E4. [trialsPerRule] controla la frecuencia de cambio (menos trials por regla = más
 * cambios = más difícil). La presión temporal (deadline del staircase) sigue siendo la palanca adaptativa en vivo.
 */
data class RuleSwitchConfig(val trialsPerRule: Int = DEFAULT_TRIALS_PER_RULE) {
    init { require(trialsPerRule >= MIN_TRIALS_PER_RULE) { "trialsPerRule debe ser >= $MIN_TRIALS_PER_RULE." } }

    companion object {
        const val MIN_TRIALS_PER_RULE = 2
        const val DEFAULT_TRIALS_PER_RULE = 5
        val DEFAULT: RuleSwitchConfig = RuleSwitchConfig()
    }
}

/**
 * Generador de E4 · Cambio de Regla. Es el ÚNICO generador con estado de secuencia (un contador de trial que fija
 * la regla activa); [reset] lo vuelve a cero al empezar la corrida. El estímulo (color/forma) es aleatorio por
 * trial; la REGLA es determinista por índice, así que el calendario de cambios es reproducible para el analizador.
 *
 * La señal ([SpeedTrialPrompt.cue], visible en la fase Stimulus) muestra la regla + el estímulo; las etiquetas de
 * panel muestran el MAPPING de la regla activa (Azul/Naranja o Círculo/Triángulo), no la respuesta — el usuario
 * debe leer el estímulo y aplicar la regla. Por eso NO se gatea la revelación de etiquetas (no filtran la respuesta).
 */
class RuleSwitchGenerator(private val config: RuleSwitchConfig = RuleSwitchConfig.DEFAULT) : SpeedPromptGenerator {

    override val exerciseId: SpeedExerciseId = SpeedExerciseId.E4_CAMBIO

    private var index = 0

    override fun reset() { index = 0 }

    override fun next(rng: SpeedRng): SpeedTrialPrompt {
        val i = index++
        val rule = RuleSwitchLogic.ruleAt(i, config.trialsPerRule)
        val stimulus = RuleSwitchStimulus(colorIsBlue = rng.nextBoolean(), shapeIsCircle = rng.nextBoolean())
        val (leftLabel, rightLabel) = when (rule) {
            SpeedRule.COLOR -> "Azul" to "Naranja"
            SpeedRule.SHAPE -> "Círculo" to "Triángulo"
        }
        val ruleLabel = if (rule == SpeedRule.COLOR) "COLOR" else "FORMA"
        val glyph = if (stimulus.shapeIsCircle) "●" else "▲"
        val colorWord = if (stimulus.colorIsBlue) "azul" else "naranja"
        return SpeedTrialPrompt(
            correctChoice = RuleSwitchLogic.correctChoice(rule, stimulus),
            cue = "$ruleLabel · $glyph $colorWord",
            leftLabel = leftLabel,
            rightLabel = rightLabel,
        )
    }
}
