package com.mindos.app.features.mindboost.focus.domain.reporting

import com.mindos.app.features.mindboost.focus.domain.evaluation.EvaluationResult
import com.mindos.app.features.mindboost.focus.domain.evaluation.MetricChange
import com.mindos.app.features.mindboost.focus.domain.evaluation.RciResult
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * One metric's change in the transfer report, flagged as strong (reliable) or not.
 *
 * [deltaText] is the delta already written for the screen. It lives here, not in the composables, because
 * both screens rendered the identical three lines and integer rounding turned every sub-unit change into a
 * bare zero; formatting once in the domain leaves one owner and makes the rule testable without Compose.
 */
data class TransferLine(
    val metric: MetricKey,
    val label: String,
    val change: MetricChange,
    val strong: Boolean,
    val deltaText: String,
)

/**
 * The probe report. A probe is a PARALLEL FORM of a trained task — reserved SEED, shared paradigm — so
 * nothing here is evidence about a capacity, and [honestNote] claims no carry-over of any kind
 * (`MB-FOCUS-0089`; ladder frozen at C0 by `FocusClaimLevelGateTest`).
 *
 * [strongEvidence] is UNREACHABLE in production today: a line is strong only when its RCI says
 * `reliable`, the RCI needs a reliability estimate, and the only reliability source wired is
 * `ReliabilitySource.EMPTY`. Every row therefore lands in [preliminary]. Whoever wires a real
 * reliability source owns the copy above that section too (`MB-FOCUS-0102`).
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
            TransferLine(
                metric = change.metric,
                label = label(change.metric),
                change = change,
                strong = strong,
                deltaText = deltaText(change.rawDelta),
            )
        }
        return TransferReport(
            strongEvidence = lines.filter { it.strong },
            preliminary = lines.filterNot { it.strong },
            honestNote = honestNote(
                hasStrong = lines.any { it.strong },
                hasWithheld = evaluation.withheldForSample.isNotEmpty(),
            ),
        )
    }

    /**
     * The user-facing name of a probe row. EXHAUSTIVE on purpose: the `else -> metric.name` this replaced
     * rendered the enum identifier as if it were copy, and it was reachable — `perMetric` carries every
     * metric the engine marked `confident`, and only four of them had a name. A new [MetricKey] must now
     * fail the BUILD until someone decides what the user is shown, rather than silently shipping its
     * identifier.
     *
     * This paragraph used to add "nine of which clear the central floor at probe length". That was true
     * when it was written and is FALSE since every metric began sealing its OWN cell (`MB-FOCUS-0128`):
     * the nine that share the `ACCURACY_RT_BASE` floor are still the ones the probe block emits, but on
     * the probe the protocol actually serves exactly ONE of them clears — ACCURACY, whose cell is the
     * trials (55 against a floor of 20). Every other member seals `cleanRts.size`, the caught signals,
     * about 11 on each reserved form. Both halves are pinned by `ProbeMeasurementReachabilityTest`, and
     * they stop holding the day the probe is lengthened (`MB-FOCUS-0130`).
     *
     * Every name keeps the "(prueba)" marker: it is the only thing on the row telling the reader this is
     * the reserved probe and not their training. No screen carries a section header saying so — the header
     * this KDoc used to cite was never shipped, and the claim it made (an untrained test) would have been
     * false anyway: the probe runs a trained paradigm (`MB-FOCUS-0089`).
     *
     * Names describe what was measured in plain language — never what it is supposed to mean about the person.
     */
    @Suppress("CyclomaticComplexMethod") // A naming table: one branch per metric, no logic to simplify.
    private fun label(metric: MetricKey): String = when (metric) {
        MetricKey.ACCURACY -> "Precisión (prueba)"
        MetricKey.RT_MEDIAN -> "Reacción (prueba)"
        MetricKey.RT_CV -> "Estabilidad (prueba)"
        MetricKey.LAPSES -> "Lapsos (prueba)"
        MetricKey.FALSE_ALARMS -> "Respuestas de más (prueba)"
        MetricKey.OMISSIONS -> "Respuestas que faltaron (prueba)"
        MetricKey.ANTICIPATIONS -> "Respuestas adelantadas (prueba)"
        MetricKey.POST_ERROR_SLOWING -> "Freno tras un fallo (prueba)"
        MetricKey.POST_ERROR_ACCURACY -> "Acierto tras un fallo (prueba)"
        MetricKey.BLOCK_STABILITY -> "Regularidad del bloque (prueba)"
        MetricKey.FATIGUE_INDEX -> "Desgaste durante la sesión (prueba)"
        MetricKey.INHIBITORY_CONTROL_INDEX -> "Índice de frenado (prueba)"
        MetricKey.SUSTAINED_FOCUS_INDEX -> "Regularidad y lapsos (prueba)"
        MetricKey.NEAR_TRANSFER_INDEX -> "Índice compuesto (prueba)"
        MetricKey.IES -> "Eficiencia (prueba)"
        MetricKey.RATE_CORRECT_SCORE -> "Aciertos por segundo (prueba)"
        MetricKey.LISAS -> "Eficiencia combinada (prueba)"
        MetricKey.SSRT -> "Tiempo hasta frenar (prueba)"
        MetricKey.CONGRUENCY_EFFECT -> "Coste de la interferencia (prueba)"
        MetricKey.SWITCH_COST -> "Coste de cambiar de regla (prueba)"
        MetricKey.SET_SIZE_SLOPE -> "Coste por elemento añadido (prueba)"
        MetricKey.DUAL_TASK_COST -> "Coste de hacer dos cosas (prueba)"
        MetricKey.DISTRACTION_RESISTANCE_INDEX -> "Aguante ante distracciones (prueba)"
        MetricKey.PRESSURE_DELTA -> "Cambio bajo presión (prueba)"
        MetricKey.VIGILANCE_DECREMENT_SLOPE -> "Caída a lo largo del bloque (prueba)"
    }

    /**
     * [hasWithheld] is APPENDED, never substituted. Without the clause, a measure that was computed and
     * withheld for sample simply disappears — it reaches neither `strongEvidence` nor `preliminary` —
     * and the "todavía no se observa" branch then reads as a fact about the user when the cause may be
     * that the sample could never have reached its floor at this probe length. The "todavía" promises a
     * reachability that, in that case, no amount of practice delivers.
     *
     * The two notes were byte-identical from `MB-CATALOG-0020` until `FOC-P0-00`, which rewrote them.
     * They said "cambio fiable en pruebas NO entrenadas", and the probe is a PARALLEL FORM of a trained
     * task, not an untrained instrument — `ProbeBank` reserves disjoint SEEDS, but its three paradigms
     * (PVT, FLANKER, GO_NO_GO) are every one of them in the training catalogue. That made the sentence
     * false rather than merely strong, so the append discipline — do not reword a claim in order to
     * disclose an omission — was never what protected it. The discipline still governs the CLAUSE; the
     * wording itself is now frozen at C0 by `FocusClaimLevelGateTest`.
     */
    private fun honestNote(hasStrong: Boolean, hasWithheld: Boolean): String {
        val base = if (hasStrong) {
            "Cambio observado en la prueba reservada, que usa el mismo tipo de tarea que el " +
                "entrenamiento con material distinto. Es un resultado de hoy, no una conclusión sobre ti."
        } else {
            "Todavía no se observa un cambio claro en la prueba reservada. Lo registrado dentro " +
                "de los ejercicios describe tu práctica, no una conclusión sobre ti."
        }
        return if (hasWithheld) "$base $WITHHELD_CLAUSE" else base
    }

    private const val WITHHELD_CLAUSE =
        "Algunas medidas de esta prueba no reunieron muestra suficiente para el mínimo exigido, " +
            "así que sobre ellas no se afirma ni se descarta nada."

    /**
     * Writes a raw delta for the screen, choosing precision from the MAGNITUDE — deliberately not from
     * [com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit], which does not carry a scale:
     * `INDEX` covers both `SUSTAINED_FOCUS_INDEX` (0..100) and `BLOCK_STABILITY` (0..1), and `RATIO` covers
     * both `RT_CV` (0..1) and `RATE_CORRECT_SCORE` (correct per second, unbounded). Formatting by unit would
     * have scaled a three-point focus change into "+300". A `MetricKey -> scale` table would be a second
     * source of truth alongside the engine, so neither is used.
     *
     * Below the unit, two decimals with an explicit sign in BOTH directions: rounding to an integer wrote a
     * measured change as a bare zero, and since `Int` has no negative zero a real decline was indistinguishable
     * from stillness. At or above the unit the integer stays byte-identical to what already shipped.
     *
     * Ceiling: a change under half a hundredth still reads `0,00`. That is honest rounding — it is written as
     * a decimal, not as an integer zero — and finer than a probe of this length can resolve anyway.
     */
    internal fun deltaText(delta: Double): String {
        val hundredths = (abs(delta) * HUNDREDTHS).roundToInt()
        if (delta != 0.0 && hundredths < HUNDREDTHS) {
            val sign = if (delta > 0.0) "+" else "-"
            return "${sign}0,${hundredths.toString().padStart(2, '0')}"
        }
        return "${if (delta > 0.0) "+" else ""}${delta.roundToInt()}"
    }

    private const val HUNDREDTHS = 100
}
