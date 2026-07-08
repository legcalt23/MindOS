package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.exercise.SpeedTrialPrompt

/**
 * MC9-C-R — telemetría específica de E5 · Búsqueda Visual.
 *
 * Un valor de métrica es SIEMPRE explícito: [Measured] (derivado de datos reales) o [Unavailable] (con la razón
 * exacta por la que no se puede derivar). NUNCA un 0 fabricado ni una heurística que finja medición (Regla #5).
 */
sealed interface VisualScanSignal {
    data class Measured(val value: Double) : VisualScanSignal
    data class Unavailable(val reason: String) : VisualScanSignal
}

/** El valor si la señal fue medida, o `null` si es [VisualScanSignal.Unavailable]. Nunca fabrica un 0. */
fun VisualScanSignal.measuredOrNull(): Double? = (this as? VisualScanSignal.Measured)?.value

/**
 * Respuesta capturada por corrida (transitoria, NO persistida): el prompt presentado + el lado tocado + el
 * resultado. La produce el session holder en el choke-point de respuesta; el análisis de E5 la interpreta. Es
 * genérica (cualquier ejercicio 2-vías la puede llenar); solo E5 la consume.
 */
data class RecordedResponse(
    val prompt: SpeedTrialPrompt?,
    val responseSide: SpeedChoice?,
    val rtNanos: Long,
    val correct: Boolean,
    val anticipation: Boolean,
)

/** Un trial de E5 reducido a lo que el análisis necesita, DERIVADO del prompt presentado + la respuesta real. */
data class VisualScanTrialOutcome(
    val itemsPerSide: Int,
    val targetSide: SpeedChoice,
    val responseSide: SpeedChoice?,
    val rtNanos: Long,
    val correct: Boolean,
    val anticipation: Boolean,
) {
    companion object {
        /**
         * Reconstruye los outcomes de E5 a partir de las respuestas capturadas: `itemsPerSide` = nº de símbolos
         * del panel izquierdo (los paneles de E5 van separados por espacios), `targetSide` = lado correcto del
         * prompt (en E5 el lado correcto ES el que tiene el objetivo). Descarta respuestas sin prompt (p. ej. E1).
         */
        fun from(responses: List<RecordedResponse>): List<VisualScanTrialOutcome> =
            responses.mapNotNull { r ->
                val prompt = r.prompt ?: return@mapNotNull null
                val itemsPerSide = prompt.leftLabel.split(" ").count { it.isNotBlank() }
                if (itemsPerSide <= 0) return@mapNotNull null
                VisualScanTrialOutcome(
                    itemsPerSide = itemsPerSide,
                    targetSide = prompt.correctChoice,
                    responseSide = r.responseSide,
                    rtNanos = r.rtNanos,
                    correct = r.correct,
                    anticipation = r.anticipation,
                )
            }
    }
}

/** Accuracy y RT mediano (ms) de un nivel de carga (`itemsPerSide`) dentro de una corrida. */
data class VisualScanLoadBucket(
    val itemsPerSide: Int,
    val trials: Int,
    val accuracy: Double,
    val medianRtMs: Double?,
)

/**
 * Agregados de una corrida de E5 (el "run summary" del §2), derivado al terminar la sesión a partir de los
 * outcomes capturados. NO se persiste al store cifrado en V1 (telemetría histórica DIFERIDA); alimenta el
 * feedback inmediato y el mapper de métricas.
 */
data class VisualScanRunSummary(
    val totalTrials: Int,
    val correctTrials: Int,
    val anticipations: Int,
    val fastWrong: Int,
    val respondedTrials: Int,
    val respondedLeft: Int,
    val targetLeft: Int,
    val correctRtsMs: List<Double>,
    val byLoad: List<VisualScanLoadBucket>,
) {
    companion object {
        /** Umbral (ms) por debajo del cual una respuesta INCORRECTA cuenta como "rápida-incorrecta" (impulsiva). */
        const val FAST_WRONG_MS = 250.0
        private const val NANOS_PER_MS = 1_000_000.0

        fun from(outcomes: List<VisualScanTrialOutcome>): VisualScanRunSummary {
            val responded = outcomes.filter { it.responseSide != null }
            val correct = outcomes.filter { it.correct && !it.anticipation }
            val byLoad = outcomes.groupBy { it.itemsPerSide }.map { (n, group) ->
                val correctInBucket = group.count { it.correct }
                val rts = group.filter { it.correct && !it.anticipation }.map { it.rtNanos / NANOS_PER_MS }
                VisualScanLoadBucket(
                    itemsPerSide = n,
                    trials = group.size,
                    accuracy = if (group.isEmpty()) 0.0 else correctInBucket.toDouble() / group.size,
                    medianRtMs = median(rts),
                )
            }.sortedBy { it.itemsPerSide }
            return VisualScanRunSummary(
                totalTrials = outcomes.size,
                correctTrials = outcomes.count { it.correct },
                anticipations = outcomes.count { it.anticipation },
                fastWrong = outcomes.count { isFastWrong(it) },
                respondedTrials = responded.size,
                respondedLeft = responded.count { it.responseSide == SpeedChoice.LEFT },
                targetLeft = outcomes.count { it.targetSide == SpeedChoice.LEFT },
                correctRtsMs = correct.map { it.rtNanos / NANOS_PER_MS },
                byLoad = byLoad,
            )
        }

        /** Respuesta INCORRECTA (no anticipación) por debajo del umbral rápido = error impulsivo. */
        private fun isFastWrong(o: VisualScanTrialOutcome): Boolean =
            !o.correct && !o.anticipation && o.rtNanos / NANOS_PER_MS < FAST_WRONG_MS

        private fun median(values: List<Double>): Double? {
            if (values.isEmpty()) return null
            val sorted = values.sorted()
            val mid = sorted.size / 2
            return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
        }
    }
}
