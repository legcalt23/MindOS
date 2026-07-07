package com.mindos.app.features.mindboost.speed.domain.exercise

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId

/** Familia de similitud target/distractor de E5 (más similitud = búsqueda más difícil). */
enum class VisualSimilarity { LOW, MEDIUM, HIGH }

/**
 * Configuración de CONTENIDO de [VisualScanRaceGenerator] (MC9-C). La carga visual (número de elementos por
 * panel [itemsPerSide]) y la similitud target/distractor ([similarity], vía [VisualScanConfig.forSimilarity])
 * fijan la dificultad de detección. La presión temporal (el `deadline` que gobierna el `StaircaseEngine`) sigue
 * siendo la palanca ADAPTATIVA en vivo, igual que en E2/E3/E6/E7: E5 reutiliza el bucle de 2 vías sin cambiar
 * el motor de sesión.
 *
 * Invariante de integridad de banco: [targetPool] y [distractorPool] NO se solapan, así que un distractor nunca
 * puede ser el objetivo (garantiza "exactamente un lado contiene el target" a nivel estructural, no por azar).
 */
data class VisualScanConfig(
    val itemsPerSide: Int = 4,
    val targetPool: List<String> = listOf("★"),
    val distractorPool: List<String> = listOf("●", "▲", "◆", "■"),
    val minItemsPerSide: Int = 3,
    val maxItemsPerSide: Int = 6,
    val allowRepeatedDistractors: Boolean = true,
    val similarity: VisualSimilarity = VisualSimilarity.LOW,
    val separator: String = " ",
) {
    init {
        require(minItemsPerSide >= 2) { "minItemsPerSide debe ser >= 2 (target + al menos un distractor)." }
        require(maxItemsPerSide >= minItemsPerSide) { "maxItemsPerSide debe ser >= minItemsPerSide." }
        require(itemsPerSide in minItemsPerSide..maxItemsPerSide) {
            "itemsPerSide debe estar en $minItemsPerSide..$maxItemsPerSide."
        }
        require(targetPool.isNotEmpty()) { "targetPool no puede estar vacío." }
        require(distractorPool.isNotEmpty()) { "distractorPool no puede estar vacío." }
        require(targetPool.none { it in distractorPool }) { "targetPool y distractorPool no pueden solaparse." }
        if (!allowRepeatedDistractors) {
            require(distractorPool.size >= maxItemsPerSide) {
                "sin distractores repetidos, distractorPool debe tener >= maxItemsPerSide símbolos."
            }
        }
    }

    companion object {
        val DEFAULT: VisualScanConfig = VisualScanConfig()

        /**
         * Familias de similitud de V1 (glyphs seguros cross-platform de Geometric Shapes). LOW = target muy
         * saliente; HIGH = target parecido a los distractores (búsqueda serial más costosa). Escalable: la
         * variabilidad del target y densidades mayores son knobs preparados vía el constructor.
         */
        fun forSimilarity(level: VisualSimilarity, itemsPerSide: Int = 4): VisualScanConfig = when (level) {
            VisualSimilarity.LOW -> VisualScanConfig(
                itemsPerSide = itemsPerSide,
                targetPool = listOf("★"),
                distractorPool = listOf("●", "▲", "◆", "■"),
                similarity = level,
            )
            VisualSimilarity.MEDIUM -> VisualScanConfig(
                itemsPerSide = itemsPerSide,
                targetPool = listOf("◆"),
                distractorPool = listOf("◇", "■", "□", "●"),
                similarity = level,
            )
            VisualSimilarity.HIGH -> VisualScanConfig(
                itemsPerSide = itemsPerSide,
                targetPool = listOf("○"),
                distractorPool = listOf("●", "◇", "□", "△"),
                similarity = level,
            )
        }
    }
}

/** Un trial de búsqueda visual: el objetivo, ambos paneles y el lado correcto. Ground truth para holder/tests. */
data class VisualScanTrial(
    val target: String,
    val leftItems: List<String>,
    val rightItems: List<String>,
    val correct: SpeedChoice,
)

/**
 * Visual Scan Race (E5 · Búsqueda Visual) — detección de un objetivo entre distractores bajo presión temporal.
 * Dos paneles: EXACTAMENTE uno contiene el objetivo; el usuario toca ese lado. Entrena velocidad de búsqueda
 * visual, atención selectiva, discriminación target/distractor y velocidad-con-precisión. "La velocidad sin
 * precisión no se premia": una respuesta fuera del `deadline` cuenta como incorrecta (bucle de 2 vías) y el
 * motor de score preliminar penaliza la baja precisión — este generador solo produce el contenido, sin score
 * ni claims.
 *
 * Los paneles SON las etiquetas izquierda/derecha (alineadas con el área táctil real); la señal
 * ([SpeedTrialPrompt.cue]) lleva solo QUÉ buscar ("Busca ★"), nunca el lado. Con [revealLabelsOnlyAtStimulus]
 * = true, la pantalla revela cue+paneles SOLO desde la fase Stimulus (no en el foreperiodo), para que el TR mida
 * búsqueda+decisión desde el onset y no una respuesta pre-decidida. Determinista dado un [SpeedRng].
 */
class VisualScanRaceGenerator(
    private val config: VisualScanConfig = VisualScanConfig.DEFAULT,
) : SpeedPromptGenerator {

    override val exerciseId: SpeedExerciseId = SpeedExerciseId.E5_BUSQUEDA

    /** Construye un trial: objetivo aleatorio del pool, lado del objetivo aleatorio, ambos paneles llenos. */
    fun trial(rng: SpeedRng): VisualScanTrial {
        val target = config.targetPool[rng.nextInt(config.targetPool.size)]
        val targetOnLeft = rng.nextBoolean()
        val targetSide = side(rng, target, withTarget = true)
        val plainSide = side(rng, target, withTarget = false)
        return VisualScanTrial(
            target = target,
            leftItems = if (targetOnLeft) targetSide else plainSide,
            rightItems = if (targetOnLeft) plainSide else targetSide,
            correct = if (targetOnLeft) SpeedChoice.LEFT else SpeedChoice.RIGHT,
        )
    }

    override fun next(rng: SpeedRng): SpeedTrialPrompt {
        val t = trial(rng)
        return SpeedTrialPrompt(
            correctChoice = t.correct,
            cue = "$SEARCH_CUE ${t.target}",
            leftLabel = t.leftItems.joinToString(config.separator),
            rightLabel = t.rightItems.joinToString(config.separator),
            // MC9-C: the panels ARE the visual stimulus, so hide them until the onset (no pre-onset scanning).
            revealLabelsOnlyAtStimulus = true,
        )
    }

    /** Un panel de [config].itemsPerSide celdas; si [withTarget], una celda es el objetivo, el resto distractores. */
    private fun side(rng: SpeedRng, target: String, withTarget: Boolean): List<String> {
        val distractorCount = if (withTarget) config.itemsPerSide - 1 else config.itemsPerSide
        val ds = distractors(rng, distractorCount)
        if (!withTarget) return ds
        val insertAt = rng.nextInt(config.itemsPerSide)
        var di = 0
        return (0 until config.itemsPerSide).map { i -> if (i == insertAt) target else ds[di++] }
    }

    /** [count] distractores; con repetición (uniforme) o distintos (Fisher-Yates parcial determinista). */
    private fun distractors(rng: SpeedRng, count: Int): List<String> {
        if (config.allowRepeatedDistractors) {
            return List(count) { config.distractorPool[rng.nextInt(config.distractorPool.size)] }
        }
        val pool = config.distractorPool.toMutableList()
        return List(count) { pool.removeAt(rng.nextInt(pool.size)) }
    }

    private companion object {
        const val SEARCH_CUE = "Busca"
    }
}
