package com.mindos.app.features.mindboost.focus.domain.reporting

import com.mindos.app.features.mindboost.focus.domain.evaluation.EvaluationResult
import com.mindos.app.features.mindboost.focus.domain.evaluation.MetricChange
import com.mindos.app.features.mindboost.focus.domain.evaluation.RciResult
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey

/** One metric's change in the transfer report, flagged as strong (reliable) or not. */
data class TransferLine(
    val metric: MetricKey,
    val label: String,
    val change: MetricChange,
    val strong: Boolean,
)

/**
 * The honest near-transfer report. Reliable changes on UN-TRAINED probes are the
 * strongest evidence; everything else is preliminary. The [honestNote] NEVER claims
 * far transfer (intelligence, general attention, daily-life carryover).
 */
data class TransferReport(
    val strongEvidence: List<TransferLine>,
    val preliminary: List<TransferLine>,
    val honestNote: String,
) {
    val hasStrongEvidence: Boolean get() = strongEvidence.isNotEmpty()
}

/** Builds a [TransferReport] from a probe-based [EvaluationResult]. */
object TransferReportBuilder {

    fun build(evaluation: EvaluationResult): TransferReport {
        val lines = evaluation.changes.map { change ->
            val strong = (change.rci as? RciResult.Value)?.reliable == true
            TransferLine(metric = change.metric, label = label(change.metric), change = change, strong = strong)
        }
        return TransferReport(
            strongEvidence = lines.filter { it.strong },
            preliminary = lines.filterNot { it.strong },
            honestNote = honestNote(lines.any { it.strong }),
        )
    }

    private fun label(metric: MetricKey): String = when (metric) {
        MetricKey.ACCURACY -> "Precisión (prueba)"
        MetricKey.RT_MEDIAN -> "Reacción (prueba)"
        MetricKey.RT_CV -> "Estabilidad (prueba)"
        MetricKey.LAPSES -> "Lapsos (prueba)"
        else -> metric.name
    }

    private fun honestNote(hasStrong: Boolean): String = if (hasStrong) {
        "Cambio fiable en pruebas NO entrenadas — la evidencia más fuerte de transferencia " +
            "cercana. NO demuestra mejora general ni de la atención en la vida diaria."
    } else {
        "Aún sin cambio fiable en pruebas no entrenadas. El progreso dentro de los ejercicios " +
            "es una señal secundaria, no una prueba de mejora real."
    }
}
