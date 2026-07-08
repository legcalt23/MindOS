package com.mindos.app.features.mindboost.speed.domain.exercise

import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoStimulus
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoTrialType

/**
 * MC9-D — configuración del generador de E8 · Go/No-Go Velocity. Defaults = calibración preliminar V1 (§4):
 * GO frecuente (70–80%), NO-GO 20–30%, deadline gobernado por nivel de dificultad, foreperiodo aleatorio.
 *
 * @property goRatio proporción objetivo de trials GO (0,1). V1 = 0.75.
 * @property scheduleBlock tamaño de bloque del scheduler balanceado (fija la proporción, no azar).
 * @property minNoGoPerBlock NO-GO mínimos garantizados por bloque (muestra mínima de inhibición).
 * @property baseDeadlineMs deadline (ms) del nivel 0; cada nivel resta [deadlineStepMs] hasta [minDeadlineMs].
 * @property difficultyLevels niveles a mezclar (bag barajado). `null` ⇒ un solo [baseDifficultyLevel] (V1
 *   producción) y `difficultyAdjustedControl` queda `Unavailable`; el mapper ya está listo para runs mixtas.
 */
data class GoNoGoConfig(
    val goRatio: Double = 0.75,
    val scheduleBlock: Int = 8,
    val minNoGoPerBlock: Int = 2,
    val baseDeadlineMs: Long = 600L,
    val deadlineStepMs: Long = 80L,
    val minDeadlineMs: Long = 320L,
    val foreperiodMinMs: Long = 500L,
    val foreperiodMaxMs: Long = 1100L,
    val baseDifficultyLevel: Int = 0,
    val difficultyLevels: List<Int>? = null,
    val goFamily: String = GO_FAMILY,
    val noGoFamily: String = NO_GO_FAMILY,
) {
    init {
        require(goRatio > 0.0 && goRatio < 1.0) { "goRatio debe estar en (0,1): $goRatio" }
        require(scheduleBlock >= MIN_BLOCK) { "scheduleBlock debe ser >= $MIN_BLOCK: $scheduleBlock" }
        require(minNoGoPerBlock in 1 until scheduleBlock) { "minNoGoPerBlock debe estar en [1, scheduleBlock)" }
        require(baseDeadlineMs > 0 && minDeadlineMs in 1..baseDeadlineMs) { "deadlines inválidos" }
        require(deadlineStepMs >= 0) { "deadlineStepMs debe ser >= 0" }
        require(foreperiodMinMs in 1..foreperiodMaxMs) { "foreperiodo inválido" }
        require(baseDifficultyLevel >= 0) { "baseDifficultyLevel debe ser >= 0" }
        require(difficultyLevels == null || (difficultyLevels.isNotEmpty() && difficultyLevels.all { it >= 0 })) {
            "difficultyLevels debe ser no vacía y no negativa"
        }
    }

    companion object {
        const val GO_FAMILY = "go"
        const val NO_GO_FAMILY = "nogo"
        private const val MIN_BLOCK = 4

        /** Corrida de dificultad mixta (habilita `difficultyAdjustedControl`): niveles 0..2 barajados por trial. */
        fun mixedDifficulty(): GoNoGoConfig = GoNoGoConfig(difficultyLevels = listOf(0, 1, 2))
    }
}

/**
 * Generador PURO y determinista de estímulos de E8 · Go/No-Go Velocity. Dado un [SpeedRng] fijo produce la misma
 * secuencia. El scheduler es un bag barajado (Fisher-Yates), NO azar puro: garantiza la proporción GO/NO-GO y un
 * mínimo de NO-GO por bloque (muestra de inhibición). El deadline se deriva del nivel de dificultad; el foreperiodo
 * es aleatorio dentro de [GoNoGoConfig.foreperiodMinMs]..[GoNoGoConfig.foreperiodMaxMs] (evita anticipación por ritmo).
 */
class GoNoGoVelocityGenerator(private val config: GoNoGoConfig = GoNoGoConfig()) {

    private var typeBag: MutableList<GoNoGoTrialType> = mutableListOf()
    private var difficultyBag: MutableList<Int> = mutableListOf()
    private var position: Int = 0

    /** Reinicia el estado de secuencia (bags + posición) para una corrida nueva. */
    fun reset() {
        typeBag = mutableListOf()
        difficultyBag = mutableListOf()
        position = 0
    }

    /** Siguiente estímulo de la corrida. Determinista dado [rng]. */
    fun next(rng: SpeedRng): GoNoGoStimulus {
        val trialType = nextTrialType(rng)
        val difficultyLevel = nextDifficultyLevel(rng)
        val family = if (trialType == GoNoGoTrialType.GO) config.goFamily else config.noGoFamily
        val stimulus = GoNoGoStimulus(
            trialType = trialType,
            stimulusId = "$family-$position",
            stimulusFamily = family,
            deadlineMs = deadlineForLevel(difficultyLevel),
            foreperiodMs = nextForeperiod(rng),
            difficultyLevel = difficultyLevel,
            runPosition = position,
            trialConfigVersion = CONFIG_VERSION,
        )
        position += 1
        return stimulus
    }

    private fun deadlineForLevel(level: Int): Long =
        (config.baseDeadlineMs - level * config.deadlineStepMs).coerceAtLeast(config.minDeadlineMs)

    private fun nextForeperiod(rng: SpeedRng): Long {
        val span = (config.foreperiodMaxMs - config.foreperiodMinMs + 1L).toInt()
        return config.foreperiodMinMs + rng.nextInt(span)
    }

    private fun nextTrialType(rng: SpeedRng): GoNoGoTrialType {
        if (typeBag.isEmpty()) refillTypeBag(rng)
        return typeBag.removeAt(typeBag.size - 1)
    }

    private fun refillTypeBag(rng: SpeedRng) {
        val block = config.scheduleBlock
        val rawGo = (config.goRatio * block).toInt()
        // Clamp so there is always at least minNoGoPerBlock NO-GO and at least one GO in the block.
        val goCount = rawGo.coerceIn(1, block - config.minNoGoPerBlock)
        val bag = ArrayList<GoNoGoTrialType>(block)
        repeat(goCount) { bag.add(GoNoGoTrialType.GO) }
        repeat(block - goCount) { bag.add(GoNoGoTrialType.NO_GO) }
        typeBag = shuffled(bag, rng)
    }

    private fun nextDifficultyLevel(rng: SpeedRng): Int {
        val levels = config.difficultyLevels ?: return config.baseDifficultyLevel
        if (difficultyBag.isEmpty()) difficultyBag = shuffled(levels.toMutableList(), rng)
        return difficultyBag.removeAt(difficultyBag.size - 1)
    }

    private fun <T> shuffled(items: MutableList<T>, rng: SpeedRng): MutableList<T> {
        for (i in items.size - 1 downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = items[i]
            items[i] = items[j]
            items[j] = tmp
        }
        return items
    }

    companion object {
        const val CONFIG_VERSION = "1.0.0-mc9d"
    }
}
