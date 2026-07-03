package com.mindverse.mindboost.core.mdp.bayes

import com.mindverse.mindboost.core.domain.BayesScenario
import com.mindverse.mindboost.core.domain.DifficultyVector
import com.mindverse.mindboost.core.domain.SensorEvidence
import kotlin.math.abs
import kotlin.math.ln
import kotlin.random.Random

object BayesEngine {
    fun generateScenario(
        difficulty: DifficultyVector,
        random: Random = Random.Default,
    ): BayesScenario {
        val scale = difficulty.noiseLevel.coerceIn(0.0, 1.0)
        val baseRate = jittered(
            value = BASE_RATE_MIN + BASE_RATE_SPAN * (1.0 - scale),
            radius = BASE_RATE_JITTER,
            min = PROBABILITY_FLOOR,
            max = PROBABILITY_CEILING,
            random = random,
        )
        val sensitivity = jittered(
            value = SENSITIVITY_MAX - SENSITIVITY_SPAN * scale,
            radius = SENSITIVITY_JITTER,
            min = SENSITIVITY_FLOOR,
            max = PROBABILITY_CEILING,
            random = random,
        )
        val falsePositiveRate = jittered(
            value = FALSE_POSITIVE_MIN + FALSE_POSITIVE_SPAN * scale,
            radius = FALSE_POSITIVE_JITTER,
            min = PROBABILITY_FLOOR,
            max = FALSE_POSITIVE_CEILING,
            random = random,
        )

        return BayesScenario(
            baseRate = baseRate,
            sensitivity = sensitivity,
            falsePositiveRate = falsePositiveRate,
            expectedPosterior = calculatePosterior(baseRate, sensitivity, falsePositiveRate),
        )
    }

    fun calculatePosterior(
        baseRate: Double,
        sensitivity: Double,
        falsePositiveRate: Double,
    ): Double {
        val prior = baseRate.coerceIn(PROBABILITY_FLOOR, PROBABILITY_CEILING)
        val truePositive = sensitivity.coerceIn(SENSITIVITY_FLOOR, PROBABILITY_CEILING)
        val falsePositive = falsePositiveRate.coerceIn(PROBABILITY_FLOOR, FALSE_POSITIVE_CEILING)
        val numerator = truePositive * prior
        val denominator = numerator + falsePositive * (1.0 - prior)

        return if (denominator <= 0.0) {
            0.0
        } else {
            (numerator / denominator).coerceIn(0.0, 1.0)
        }
    }

    fun calculatePosterior(scenario: BayesScenario): Double =
        calculatePosterior(scenario.baseRate, scenario.sensitivity, scenario.falsePositiveRate)

    fun calculatePosterior(
        baseRate: Double,
        evidence: List<SensorEvidence>,
    ): Double {
        val prior = baseRate.coerceIn(PROBABILITY_FLOOR, PROBABILITY_CEILING)
        val posteriorOdds = evidence.fold(prior / (1.0 - prior)) { odds, sensor ->
            val reliability = sensor.reliability.coerceIn(SENSITIVITY_FLOOR, PROBABILITY_CEILING)
            val likelihoodRatio = if (sensor.reportedValue) {
                reliability / (1.0 - reliability)
            } else {
                (1.0 - reliability) / reliability
            }
            odds * likelihoodRatio
        }

        return (posteriorOdds / (1.0 + posteriorOdds)).coerceIn(0.0, 1.0)
    }

    fun evaluateBayesianUpdate(
        scenario: BayesScenario,
        userPosteriorEstimation: Double,
    ): Double {
        val user = userPosteriorEstimation.coerceIn(PROBABILITY_FLOOR, PROBABILITY_CEILING)
        val math = scenario.expectedPosterior.coerceIn(PROBABILITY_FLOOR, PROBABILITY_CEILING)
        val priori = scenario.baseRate.coerceIn(PROBABILITY_FLOOR, PROBABILITY_CEILING)
        val logitPriori = logit(priori)
        val logitUser = logit(user) - logitPriori
        val logitMath = logit(math) - logitPriori

        if (abs(logitMath) < LOGIT_EPSILON) return 1.0

        return (logitUser / logitMath).coerceIn(0.0, BUR_MAX)
    }

    private fun logit(probability: Double): Double = ln(probability / (1.0 - probability))

    private fun jittered(
        value: Double,
        radius: Double,
        min: Double,
        max: Double,
        random: Random,
    ): Double {
        val centeredNoise = random.nextDouble() * DOUBLE_UNIT_SPAN - 1.0
        return (value + centeredNoise * radius).coerceIn(min, max)
    }

    private const val PROBABILITY_FLOOR = 0.001
    private const val PROBABILITY_CEILING = 0.999
    private const val SENSITIVITY_FLOOR = 0.5
    private const val FALSE_POSITIVE_CEILING = 0.5
    private const val BASE_RATE_MIN = 0.01
    private const val BASE_RATE_SPAN = 0.48
    private const val SENSITIVITY_MAX = 0.99
    private const val SENSITIVITY_SPAN = 0.39
    private const val FALSE_POSITIVE_MIN = 0.01
    private const val FALSE_POSITIVE_SPAN = 0.29
    private const val BASE_RATE_JITTER = 0.015
    private const val SENSITIVITY_JITTER = 0.015
    private const val FALSE_POSITIVE_JITTER = 0.015
    private const val DOUBLE_UNIT_SPAN = 2.0
    private const val LOGIT_EPSILON = 0.0001
    private const val BUR_MAX = 3.0
}
