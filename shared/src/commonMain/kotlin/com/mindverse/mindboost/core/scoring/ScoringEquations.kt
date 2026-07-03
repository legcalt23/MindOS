@file:Suppress("LongParameterList", "MagicNumber", "ReturnCount", "TooManyFunctions")

package com.mindverse.mindboost.core.scoring

import com.mindverse.mindboost.core.domain.BrierPrediction
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

object ScoringEquations {
    fun calculateCDE(
        nVar: Int,
        hProb: Double,
        hMax: Double,
        timePressure: Double,
        memoryLoad: Int,
        reversibility: Double,
    ): Double {
        val nNorm = ((nVar.toDouble() - MIN_VARIABLES) / VARIABLE_SPAN).coerceIn(0.0, 1.0)
        val hNorm = if (hMax <= 0.0) 0.0 else (hProb / hMax).coerceIn(0.0, 1.0)
        val tpNorm = timePressure.coerceIn(0.0, 1.0)
        val mlNorm = ((memoryLoad.toDouble() - MIN_MEMORY_LOAD) / MEMORY_LOAD_SPAN).coerceIn(0.0, 1.0)
        val rNorm = (1.0 - reversibility).coerceIn(0.0, 1.0)
        val cde = VARIABLE_WEIGHT * nNorm +
            ENTROPY_WEIGHT * hNorm +
            TIME_PRESSURE_WEIGHT * tpNorm +
            MEMORY_LOAD_WEIGHT * mlNorm +
            REVERSIBILITY_WEIGHT * rNorm

        return cde.coerceIn(0.0, MAX_PERCENT_SCORE)
    }

    fun calculatePHD(
        icdRolling: Double,
        ceRolling: Double,
        iedRolling: Double,
        agrsRolling: Double,
        frsRolling: Double,
    ): Double {
        if (agrsRolling > AGRS_INVALID_SESSION_THRESHOLD) return 0.0

        val icd = icdRolling.coerceIn(0.0, MAX_PERCENT_SCORE)
        val ce = ceRolling.coerceIn(0.0, 1.0)
        val ied = iedRolling.coerceIn(0.0, MAX_PERCENT_SCORE)
        var rawPhd = PHD_ICD_WEIGHT * icd + PHD_CALIBRATION_WEIGHT * (1.0 - ce) + PHD_IED_WEIGHT * ied

        if (icd < ICD_COMPENSATION_THRESHOLD) {
            rawPhd = rawPhd.coerceAtMost(ICD_COMPENSATION_CAP)
        }
        if (ce > CE_COMPENSATION_THRESHOLD) {
            rawPhd = rawPhd.coerceAtMost(CE_COMPENSATION_CAP)
        }
        if (frsRolling > FRS_DELOAD_THRESHOLD) {
            rawPhd -= FATIGUE_PHD_PENALTY
        }

        return rawPhd.coerceIn(0.0, MAX_PERCENT_SCORE)
    }

    fun calculateICD(
        objectivesAlignment: Double,
        bur: Double,
        biasDetected: Boolean,
        alternativesScannedRatio: Double,
    ): Double {
        val cObj = objectivesAlignment.coerceIn(0.0, 1.0)
        val burError = if (bur <= 0.0) {
            1.0
        } else {
            (abs(ln(bur)) / ln(BUR_ERROR_BASE)).coerceIn(0.0, 1.0)
        }
        val biasValue = if (biasDetected) 1.0 else 0.0
        val altRatio = alternativesScannedRatio.coerceIn(0.0, 1.0)
        val icd = ICD_OBJECTIVE_WEIGHT * cObj +
            ICD_BUR_WEIGHT * (1.0 - burError) +
            ICD_BIAS_WEIGHT * (1.0 - biasValue) +
            ICD_ALTERNATIVES_WEIGHT * altRatio

        return icd.coerceIn(0.0, MAX_PERCENT_SCORE)
    }

    fun calculateIED(
        icd: Double,
        reactionTimeMs: Long,
        timeLimitMs: Long,
        isReversible: Boolean,
    ): Double {
        if (reactionTimeMs < IMPULSIVE_RESPONSE_MS) return 0.0
        if (timeLimitMs <= 0L) return icd.coerceIn(0.0, MAX_PERCENT_SCORE)

        val delayRatio = reactionTimeMs.toDouble() / timeLimitMs.toDouble()
        val lambda = if (isReversible) REVERSIBLE_DELAY_LAMBDA else IRREVERSIBLE_DELAY_LAMBDA
        val ied = icd * exp(-lambda * delayRatio * delayRatio)

        return ied.coerceIn(0.0, MAX_PERCENT_SCORE)
    }

    fun calculateCRI(
        goNoGoAccuracy: Double,
        reactionTimeCV: Double,
        subjectiveFatigue: Double,
    ): Double {
        val precision = goNoGoAccuracy.coerceIn(0.0, 1.0)
        val cv = reactionTimeCV.coerceIn(0.0, 1.0)
        val fatigueReadiness = subjectiveFatigue.coerceIn(0.0, 1.0)
        val cri = CRI_ACCURACY_WEIGHT * precision +
            CRI_STABILITY_WEIGHT * (1.0 - cv) +
            CRI_FATIGUE_WEIGHT * fatigueReadiness

        return cri.coerceIn(0.0, MAX_PERCENT_SCORE)
    }

    fun calculateBrierScore(predictions: List<*>): Double {
        if (predictions.isEmpty()) return EMPTY_BRIER_SCORE

        val points = predictions.toForecastOutcomePairs()
        val sum = points.sumOf { (forecast, outcome) ->
            val diff = forecast.coerceIn(0.5, 1.0) - outcome.coerceIn(0.0, 1.0)
            diff * diff
        }

        return (sum / points.size.toDouble()).coerceIn(0.0, EMPTY_BRIER_SCORE)
    }

    fun calculateCalibrationError(predictions: List<*>): Double {
        if (predictions.isEmpty()) return 0.0

        val points = predictions.toForecastOutcomePairs()
        val bins = List(CALIBRATION_BIN_COUNT) { mutableListOf<Pair<Double, Double>>() }
        points.forEach { prediction ->
            val forecast = prediction.first.coerceIn(0.5, 1.0)
            val binIndex = ((forecast - 0.5) / CALIBRATION_BIN_WIDTH).toInt()
                .coerceIn(0, CALIBRATION_BIN_COUNT - 1)
            bins[binIndex].add(prediction)
        }

        val totalN = points.size.toDouble()
        val errorSum = bins.sumOf { bin ->
            if (bin.isEmpty()) {
                0.0
            } else {
                val binForecastMean = bin.map { it.first.coerceIn(0.5, 1.0) }.average()
                val binOutcomeMean = bin.map { it.second.coerceIn(0.0, 1.0) }.average()
                (bin.size.toDouble() / totalN) * abs(binForecastMean - binOutcomeMean)
            }
        }

        return errorSum.coerceIn(0.0, 1.0)
    }

    fun calculateCAG(avgConfidence: Double, actualAccuracy: Double): Double =
        (avgConfidence.coerceIn(0.5, 1.0) - actualAccuracy.coerceIn(0.0, 1.0)).coerceIn(-1.0, 1.0)

    fun calculateDecisionalCourage(confidences: List<Double>): Double {
        if (confidences.size < 2) return 0.0

        val mean = confidences.average()
        val variance = confidences.sumOf { confidence ->
            val diff = confidence - mean
            diff * diff
        } / (confidences.size - 1)

        return sqrt(variance).coerceIn(0.0, DECISIONAL_COURAGE_MAX)
    }

    fun calculateBUR(
        pPriori: Double,
        pPostUser: Double,
        pPostMath: Double,
    ): Double {
        val priori = pPriori.coerceIn(PROBABILITY_FLOOR, PROBABILITY_CEILING)
        val user = pPostUser.coerceIn(PROBABILITY_FLOOR, PROBABILITY_CEILING)
        val math = pPostMath.coerceIn(PROBABILITY_FLOOR, PROBABILITY_CEILING)
        val logitPriori = logit(priori)
        val logitUser = logit(user) - logitPriori
        val logitMath = logit(math) - logitPriori

        if (abs(logitMath) < LOGIT_EPSILON) return 1.0

        return (logitUser / logitMath).coerceIn(0.0, BUR_MAX)
    }

    fun calculateFRS(
        reactionTimeActualMs: Long,
        reactionTimeMediaMs: Long,
        goNoGoErrorRate: Double,
    ): Double {
        if (reactionTimeMediaMs <= 0L) return 0.0

        val rtDiff = (reactionTimeActualMs.toDouble() - reactionTimeMediaMs.toDouble()) /
            reactionTimeMediaMs.toDouble()
        val rtFactor = rtDiff.coerceAtLeast(0.0)
        val errorRate = goNoGoErrorRate.coerceIn(0.0, 1.0)

        return (rtFactor * errorRate).coerceIn(0.0, 1.0)
    }

    fun calculateAGRS(
        reactionTimeMs: Long,
        spatialEntropy: Double,
        isDialDefault: Boolean,
    ): Double {
        val rtFactor = if (reactionTimeMs < AGRS_FAST_RESPONSE_MS) 1.0 else 0.0
        val entropyFactor = 1.0 - spatialEntropy.coerceIn(0.0, 1.0)
        val dialFactor = if (isDialDefault) 1.0 else 0.0
        val agrs = AGRS_RT_WEIGHT * rtFactor + AGRS_ENTROPY_WEIGHT * entropyFactor + AGRS_DIAL_WEIGHT * dialFactor

        return agrs.coerceIn(0.0, 1.0)
    }

    private fun logit(probability: Double): Double = ln(probability / (1.0 - probability))

    private fun List<*>.toForecastOutcomePairs(): List<Pair<Double, Double>> = map { prediction ->
        when (prediction) {
            is BrierPrediction -> prediction.forecast to prediction.outcome
            is Pair<*, *> -> {
                val forecast = prediction.first as? Number
                val outcome = prediction.second as? Number
                require(forecast != null && outcome != null) {
                    "Brier pairs must contain numeric forecast and outcome values."
                }
                forecast.toDouble() to outcome.toDouble()
            }
            else -> error("Predictions must be BrierPrediction or Pair<Double, Double> values.")
        }
    }

    private const val MAX_PERCENT_SCORE = 100.0
    private const val MIN_VARIABLES = 1.0
    private const val VARIABLE_SPAN = 9.0
    private const val MIN_MEMORY_LOAD = 1.0
    private const val MEMORY_LOAD_SPAN = 6.0
    private const val VARIABLE_WEIGHT = 25.0
    private const val ENTROPY_WEIGHT = 25.0
    private const val TIME_PRESSURE_WEIGHT = 20.0
    private const val MEMORY_LOAD_WEIGHT = 20.0
    private const val REVERSIBILITY_WEIGHT = 10.0
    private const val AGRS_INVALID_SESSION_THRESHOLD = 0.75
    private const val PHD_ICD_WEIGHT = 0.4
    private const val PHD_CALIBRATION_WEIGHT = 30.0
    private const val PHD_IED_WEIGHT = 0.3
    private const val ICD_COMPENSATION_THRESHOLD = 60.0
    private const val ICD_COMPENSATION_CAP = 70.0
    private const val CE_COMPENSATION_THRESHOLD = 0.35
    private const val CE_COMPENSATION_CAP = 75.0
    private const val FRS_DELOAD_THRESHOLD = 0.20
    private const val FATIGUE_PHD_PENALTY = 20.0
    private const val BUR_ERROR_BASE = 10.0
    private const val ICD_OBJECTIVE_WEIGHT = 30.0
    private const val ICD_BUR_WEIGHT = 35.0
    private const val ICD_BIAS_WEIGHT = 20.0
    private const val ICD_ALTERNATIVES_WEIGHT = 15.0
    private const val IMPULSIVE_RESPONSE_MS = 500L
    private const val REVERSIBLE_DELAY_LAMBDA = 0.8
    private const val IRREVERSIBLE_DELAY_LAMBDA = 0.15
    private const val CRI_ACCURACY_WEIGHT = 50.0
    private const val CRI_STABILITY_WEIGHT = 30.0
    private const val CRI_FATIGUE_WEIGHT = 20.0
    private const val EMPTY_BRIER_SCORE = 0.25
    private const val CALIBRATION_BIN_COUNT = 5
    private const val CALIBRATION_BIN_WIDTH = 0.1
    private const val DECISIONAL_COURAGE_MAX = 0.5
    private const val PROBABILITY_FLOOR = 0.001
    private const val PROBABILITY_CEILING = 0.999
    private const val LOGIT_EPSILON = 0.0001
    private const val BUR_MAX = 3.0
    private const val AGRS_FAST_RESPONSE_MS = 400L
    private const val AGRS_RT_WEIGHT = 0.4
    private const val AGRS_ENTROPY_WEIGHT = 0.3
    private const val AGRS_DIAL_WEIGHT = 0.3
}
