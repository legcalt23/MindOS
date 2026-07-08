package com.mindos.app.features.mindboost.speed.domain.score

import kotlinx.serialization.Serializable

/**
 * MC9-D — telemetría específica de E8 · Go/No-Go Velocity (inhibición rápida).
 *
 * Un valor de métrica es SIEMPRE explícito: [Measured] (derivado de datos reales) o [Unavailable] (con la razón
 * exacta por la que no se puede derivar). NUNCA un 0 fabricado ni una heurística que finja medición. Espejo del
 * patrón de E5 ([VisualScanSignal]); tipo propio para no acoplar E8 a E5 (ni migrar E5). `@Serializable` para
 * persistir el summary.
 */
@Serializable
sealed interface GoNoGoSignal {
    @Serializable
    data class Measured(val value: Double) : GoNoGoSignal

    @Serializable
    data class Unavailable(val reason: String) : GoNoGoSignal
}

/** El valor si la señal fue medida, o `null` si es [GoNoGoSignal.Unavailable]. Nunca fabrica un 0. */
fun GoNoGoSignal.measuredOrNull(): Double? = (this as? GoNoGoSignal.Measured)?.value

/** Tipo de trial: responder ([GO]) o inhibir la respuesta ([NO_GO]). */
@Serializable
enum class GoNoGoTrialType { GO, NO_GO }

/**
 * Estímulo generado de un trial de Go/No-Go — la FUENTE DE VERDAD estructurada de la telemetría (como
 * `VisualScanPromptMetadata` en E5): tipo, identidad, familia visual, ventanas temporales, nivel de dificultad y
 * posición en la corrida. La produce [com.mindos.app.features.mindboost.speed.domain.exercise.GoNoGoVelocityGenerator].
 */
@Serializable
data class GoNoGoStimulus(
    val trialType: GoNoGoTrialType,
    val stimulusId: String,
    val stimulusFamily: String,
    val deadlineMs: Long,
    val foreperiodMs: Long,
    val difficultyLevel: Int,
    val runPosition: Int,
    val trialConfigVersion: String,
)

/**
 * Respuesta capturada por trial (transitoria, NO persistida): si el usuario tocó durante el foreperiodo
 * (respuesta prematura) y, si tocó tras el onset, su tiempo desde el onset ([responseTimeMs], `null` = sin
 * respuesta post-onset). La produce el driver de sesión; el análisis la interpreta contra el estímulo.
 */
data class GoNoGoResponse(
    val respondedDuringForeperiod: Boolean,
    val responseTimeMs: Long?,
)

/**
 * Trial resuelto de E8: el resultado honesto de cruzar un [GoNoGoStimulus] con su [GoNoGoResponse]. Deriva las
 * cinco clases de resultado del paradigma Go/No-Go, mutuamente coherentes:
 * - **GO + tap dentro del deadline** ⇒ `correct`.
 * - **GO + sin tap** ⇒ `omission`.
 * - **GO + tap tarde** ⇒ `lateResponse` (no correcto).
 * - **GO + tap en foreperiodo** ⇒ `anticipation` (no correcto; respuesta prematura).
 * - **NO_GO + sin tap** ⇒ `correct` (inhibición exitosa).
 * - **NO_GO + tap post-onset** ⇒ `commission` (fallo de inhibición).
 * - **NO_GO + tap en foreperiodo** ⇒ `anticipation` (no correcto; prematura, no cuenta como comisión post-onset).
 */
@Serializable
data class GoNoGoOutcome(
    val trialType: GoNoGoTrialType,
    val stimulusId: String,
    val stimulusFamily: String,
    val responseMade: Boolean,
    val responseTimeMs: Long?,
    val correct: Boolean,
    val omission: Boolean,
    val commission: Boolean,
    val anticipation: Boolean,
    val lateResponse: Boolean,
    val deadlineMs: Long,
    val foreperiodMs: Long,
    val difficultyLevel: Int,
    val runPosition: Int,
    val trialConfigVersion: String,
) {
    companion object {
        /** Resuelve un trial cruzando su estímulo con la respuesta capturada (función pura, un solo return). */
        fun resolve(stimulus: GoNoGoStimulus, response: GoNoGoResponse): GoNoGoOutcome {
            val anticipation = response.respondedDuringForeperiod
            val postOnsetRt = response.responseTimeMs
            val respondedPostOnset = postOnsetRt != null
            val withinDeadline = postOnsetRt != null && postOnsetRt <= stimulus.deadlineMs
            val isGo = stimulus.trialType == GoNoGoTrialType.GO
            return GoNoGoOutcome(
                trialType = stimulus.trialType,
                stimulusId = stimulus.stimulusId,
                stimulusFamily = stimulus.stimulusFamily,
                responseMade = anticipation || respondedPostOnset,
                responseTimeMs = postOnsetRt,
                correct = if (isGo) !anticipation && withinDeadline else !(anticipation || respondedPostOnset),
                omission = isGo && !anticipation && !respondedPostOnset,
                commission = !isGo && respondedPostOnset,
                anticipation = anticipation,
                lateResponse = isGo && !anticipation && respondedPostOnset && !withinDeadline,
                deadlineMs = stimulus.deadlineMs,
                foreperiodMs = stimulus.foreperiodMs,
                difficultyLevel = stimulus.difficultyLevel,
                runPosition = stimulus.runPosition,
                trialConfigVersion = stimulus.trialConfigVersion,
            )
        }
    }
}

/** Balance GO/NO-GO de un nivel de dificultad dentro de una corrida (para `difficultyAdjustedControl`). */
@Serializable
data class GoNoGoDifficultyBucket(
    val difficultyLevel: Int,
    val trials: Int,
    val goTrials: Int,
    val goCorrect: Int,
    val noGoTrials: Int,
    val noGoCorrect: Int,
) {
    /** Precisión balanceada del bucket (media de acierto GO y NO-GO), o `null` si falta uno de los dos tipos. */
    val balancedAccuracyOrNull: Double?
        get() = if (goTrials > 0 && noGoTrials > 0) {
            (goCorrect.toDouble() / goTrials + noGoCorrect.toDouble() / noGoTrials) / 2.0
        } else {
            null
        }
}

/**
 * Agregados de una corrida de E8 (el "run summary"), derivado al terminar la sesión de los outcomes resueltos.
 * Alimenta el feedback inmediato, el mapper de métricas y los diagnostics. Puro y testeable.
 */
data class GoNoGoRunSummary(
    val totalTrials: Int,
    val goTrials: Int,
    val noGoTrials: Int,
    val goCorrect: Int,
    val noGoCorrect: Int,
    val commissions: Int,
    val omissions: Int,
    val anticipations: Int,
    val lateResponses: Int,
    val fastCommissions: Int,
    val correctGoRtsMs: List<Double>,
    val byDifficulty: List<GoNoGoDifficultyBucket>,
) {
    companion object {
        /** Umbral (ms) por debajo del cual una COMISIÓN cuenta como "rápida" (impulsiva) — speed-accuracy tradeoff. */
        const val FAST_COMMISSION_MS = 250L

        fun from(outcomes: List<GoNoGoOutcome>): GoNoGoRunSummary {
            val go = outcomes.filter { it.trialType == GoNoGoTrialType.GO }
            val noGo = outcomes.filter { it.trialType == GoNoGoTrialType.NO_GO }
            return GoNoGoRunSummary(
                totalTrials = outcomes.size,
                goTrials = go.size,
                noGoTrials = noGo.size,
                goCorrect = go.count { it.correct },
                noGoCorrect = noGo.count { it.correct },
                commissions = noGo.count { it.commission },
                omissions = go.count { it.omission },
                anticipations = outcomes.count { it.anticipation },
                lateResponses = go.count { it.lateResponse },
                fastCommissions = noGo.count { isFastCommission(it) },
                correctGoRtsMs = go.filter { it.correct }.mapNotNull { it.responseTimeMs?.toDouble() },
                byDifficulty = difficultyBuckets(outcomes),
            )
        }

        /** Comisión con RT (ms) por debajo del umbral rápido — respuesta impulsiva en NO-GO. */
        private fun isFastCommission(o: GoNoGoOutcome): Boolean =
            o.commission && (o.responseTimeMs ?: Long.MAX_VALUE) < FAST_COMMISSION_MS

        private fun difficultyBuckets(outcomes: List<GoNoGoOutcome>): List<GoNoGoDifficultyBucket> =
            outcomes.groupBy { it.difficultyLevel }.map { (level, group) ->
                val go = group.filter { it.trialType == GoNoGoTrialType.GO }
                val noGo = group.filter { it.trialType == GoNoGoTrialType.NO_GO }
                GoNoGoDifficultyBucket(
                    difficultyLevel = level,
                    trials = group.size,
                    goTrials = go.size,
                    goCorrect = go.count { it.correct },
                    noGoTrials = noGo.size,
                    noGoCorrect = noGo.count { it.correct },
                )
            }.sortedBy { it.difficultyLevel }
    }
}
