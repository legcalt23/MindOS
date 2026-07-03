@file:Suppress("CyclomaticComplexMethod", "LongParameterList", "MagicNumber")

package com.mindverse.mindboost.core.coach

import com.mindverse.mindboost.core.domain.CoachTemplate

data class CoachSignals(
    val reactionTimeMs: Long? = null,
    val icd: Double? = null,
    val ied: Double? = null,
    val agrs: Double? = null,
    val cag: Double? = null,
    val averageConfidence: Double? = null,
    val actualAccuracy: Double? = null,
    val brierScore: Double? = null,
    val bur: Double? = null,
    val decisionalCourage: Double? = null,
    val likelihoodRatio: Double? = null,
    val frs: Double? = null,
    val reactionTimeCv: Double? = null,
    val goNoGoAccuracyDrop: Double? = null,
    val reversibleTimeLimitUsage: Double? = null,
    val repeatedConfirmationUnder500Ms: Boolean = false,
    val confidence100Failure: Boolean = false,
    val systematicFloorConfidence: Boolean = false,
    val lowConfidenceHighReliability: Boolean = false,
    val warmupConsecutiveFailures: Int = 0,
    val isIrreversible: Boolean = false,
    val isReversible: Boolean = false,
    val inactivityMs: Long? = null,
)

object CoachEngine {
    val templates: List<CoachTemplate> = listOf(
        CoachTemplate(
            id = "T01",
            targetProfile = "impulsivo",
            messageText = "Decision irreversible confirmada en {RT}ms. Auditoria minima: 5s. ICD: {ICD}.",
        ),
        CoachTemplate(
            id = "T02",
            targetProfile = "impulsivo",
            messageText = "Patron anomalo de reaccion y dial. IED penalizado a 0.0. Recalibra el ritmo de la ronda.",
        ),
        CoachTemplate(
            id = "T03",
            targetProfile = "impulsivo",
            messageText = "Pulsaciones continuas detectadas. Motor congelado para revisar la telemetria de esta ronda.",
        ),
        CoachTemplate(
            id = "T04",
            targetProfile = "sobreconfiado",
            messageText = "Certeza promedio {CONF}%, precision real {PREC}%. Revisa falsos positivos antes de confiar.",
        ),
        CoachTemplate(
            id = "T05",
            targetProfile = "sobreconfiado",
            messageText = "Certeza absoluta en una estimacion erronea. Brier de sesion: {BS}. Ajusta la calibracion.",
        ),
        CoachTemplate(
            id = "T06",
            targetProfile = "sobreconfiado",
            messageText = "Actualizacion desproporcionada ante evidencia marginal. BUR: {BUR}. Reduce sobrerreaccion.",
        ),
        CoachTemplate(
            id = "T07",
            targetProfile = "subconfiante",
            messageText = "Precision {PREC}%, certeza {CONF}%. El coraje decisional es bajo para esta evidencia.",
        ),
        CoachTemplate(
            id = "T08",
            targetProfile = "subconfiante",
            messageText = "Uso sistematico de 50% de certeza. CD: {CD}. La siguiente ronda retirara el punto medio.",
        ),
        CoachTemplate(
            id = "T09",
            targetProfile = "subconfiante",
            messageText = "Canal de alta fiabilidad. LR: {LR}. Pondera el peso matematico de la evidencia.",
        ),
        CoachTemplate(
            id = "T10",
            targetProfile = "fatigado",
            messageText = "Variabilidad inestable. CV: {CV}. Modo deload: menos presion de reloj en esta practica.",
        ),
        CoachTemplate(
            id = "T11",
            targetProfile = "fatigado",
            messageText = "Precision go/no-go deprimida ({ACC}%). " +
                "Cierra este bloque y pasa a recuperacion de practica.",
        ),
        CoachTemplate(
            id = "T12",
            targetProfile = "fatigado",
            messageText = "Tres fallos consecutivos en calentamiento. Bloque interrumpido y enviado a cooldown.",
        ),
        CoachTemplate(
            id = "T13",
            targetProfile = "paralisis",
            messageText = "Decision reversible demorando {T}s. IED degradado a {IED} por exceso de analisis.",
        ),
        CoachTemplate(
            id = "T14",
            targetProfile = "paralisis",
            messageText = "Deliberacion ineficiente en Tipo 2. " +
                "Bajo impacto y coste de error cero dentro del ejercicio.",
        ),
        CoachTemplate(
            id = "T15",
            targetProfile = "paralisis",
            messageText = "Consola inactiva. Modo simplificacion: usa solo las dos variables criticas visibles.",
        ),
    )

    init {
        check(templates.size <= MAX_TEMPLATES)
    }

    fun selectTemplate(signals: CoachSignals): CoachTemplate? = when {
        signals.repeatedConfirmationUnder500Ms -> template("T03")
        (signals.agrs ?: 0.0) > AGRS_THRESHOLD -> template("T02")
        signals.isIrreversible && (signals.reactionTimeMs ?: Long.MAX_VALUE) < IRREVERSIBLE_MIN_REVIEW_MS ->
            template("T01")
        signals.confidence100Failure -> template("T05")
        (signals.bur ?: 1.0) > BUR_OVERREACTION_THRESHOLD -> template("T06")
        (signals.cag ?: 0.0) > CAG_OVERCONFIDENT_THRESHOLD -> template("T04")
        (signals.cag ?: 0.0) < CAG_UNDERCONFIDENT_THRESHOLD && hasHighAccuracy(signals) -> template("T07")
        signals.systematicFloorConfidence -> template("T08")
        signals.lowConfidenceHighReliability -> template("T09")
        (signals.frs ?: 0.0) > FRS_THRESHOLD -> template("T10")
        (signals.goNoGoAccuracyDrop ?: 0.0) > ACCURACY_DROP_THRESHOLD -> template("T11")
        signals.warmupConsecutiveFailures >= WARMUP_FAILURE_LIMIT -> template("T12")
        signals.isReversible && (signals.reactionTimeMs ?: 0L) > REVERSIBLE_PARALYSIS_MS -> template("T13")
        signals.isReversible && (signals.reversibleTimeLimitUsage ?: 0.0) >= TIME_LIMIT_EXHAUSTION -> template("T14")
        (signals.inactivityMs ?: 0L) >= INACTIVITY_THRESHOLD_MS -> template("T15")
        else -> null
    }

    fun render(signals: CoachSignals): String? = selectTemplate(signals)?.let { render(it, signals) }

    private fun render(template: CoachTemplate, signals: CoachSignals): String =
        template.messageText
            .replace("{RT}", (signals.reactionTimeMs ?: 0L).toString())
            .replace("{ICD}", percent(signals.icd))
            .replace("{CONF}", percent(signals.averageConfidence?.times(100.0)))
            .replace("{PREC}", percent(signals.actualAccuracy?.times(100.0)))
            .replace("{BS}", decimal(signals.brierScore))
            .replace("{BUR}", decimal(signals.bur))
            .replace("{CD}", decimal(signals.decisionalCourage))
            .replace("{LR}", decimal(signals.likelihoodRatio))
            .replace("{CV}", decimal(signals.reactionTimeCv))
            .replace("{ACC}", percent(signals.actualAccuracy?.times(100.0)))
            .replace("{T}", ((signals.reactionTimeMs ?: 0L) / MILLIS_PER_SECOND).toString())
            .replace("{IED}", percent(signals.ied))

    private fun template(id: String): CoachTemplate? = templates.firstOrNull { it.id == id }

    private fun percent(value: Double?): String = decimal(value?.coerceIn(0.0, MAX_PERCENT))

    private fun hasHighAccuracy(signals: CoachSignals): Boolean =
        (signals.actualAccuracy ?: 0.0) > HIGH_ACCURACY_THRESHOLD

    private fun decimal(value: Double?): String {
        val normalized = value ?: 0.0
        val rounded = (normalized * DECIMAL_SCALE).toInt() / DECIMAL_SCALE
        return rounded.toString()
    }

    private const val MAX_TEMPLATES = 15
    private const val MAX_PERCENT = 100.0
    private const val AGRS_THRESHOLD = 0.75
    private const val IRREVERSIBLE_MIN_REVIEW_MS = 2_000L
    private const val BUR_OVERREACTION_THRESHOLD = 1.30
    private const val CAG_OVERCONFIDENT_THRESHOLD = 0.15
    private const val CAG_UNDERCONFIDENT_THRESHOLD = -0.15
    private const val HIGH_ACCURACY_THRESHOLD = 0.75
    private const val FRS_THRESHOLD = 0.20
    private const val ACCURACY_DROP_THRESHOLD = 0.20
    private const val WARMUP_FAILURE_LIMIT = 3
    private const val REVERSIBLE_PARALYSIS_MS = 25_000L
    private const val TIME_LIMIT_EXHAUSTION = 0.90
    private const val INACTIVITY_THRESHOLD_MS = 90_000L
    private const val MILLIS_PER_SECOND = 1_000L
    private const val DECIMAL_SCALE = 10.0
}
