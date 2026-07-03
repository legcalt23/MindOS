// DebugExportMetadata agrupa todo el contexto de una exportación (muchos campos por §16).
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.speed.debug

import com.mindos.app.features.mindboost.speed.data.persistence.MeasurementRun
import com.mindos.app.features.mindboost.speed.data.persistence.SpeedJson
import com.mindos.app.features.mindboost.speed.domain.DeviceTimingRisk
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.MetricThresholds
import com.mindos.app.features.mindboost.speed.domain.SessionQuality
import com.mindos.app.features.mindboost.speed.domain.Timebase
import com.mindos.app.features.mindboost.speed.domain.model.TrialRecord
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Metadata de una exportación `DebugExport v0` (§16). Incluye versión de build/métricas,
 * base temporal, precisión, riesgo de dispositivo, calidad de sesión y umbrales activos.
 */
@Serializable
data class DebugExportMetadata(
    val appVersion: String,
    val metricsVersion: String,
    val debugExportVersion: String,
    val timebase: Timebase,
    val inputTimestampPrecision: InputTimestampPrecision,
    val deviceTimingRisk: DeviceTimingRisk,
    val sessionQuality: SessionQuality,
    val thresholds: MetricThresholds,
)

/** Envoltura serializada de la exportación: metadata + corrida completa de datos brutos. */
@Serializable
data class DebugExportEnvelope(
    val metadata: DebugExportMetadata,
    val run: MeasurementRun,
)

/**
 * Serializador puro de la exportación de depuración (sin I/O ni plataforma; testeable en
 * `commonTest`). La escritura a disco y el gating debug/release los aporta [DebugExporter].
 *
 * Exporta trials, bloques, métricas de bloque, métricas de sesión, snapshot de versión y
 * logs de calidad (vía [MeasurementRun]) en JSON, y los [TrialRecord] en CSV.
 */
object DebugExport {

    private val prettyJson: Json = Json(from = SpeedJson.instance) { prettyPrint = true }

    fun toJson(envelope: DebugExportEnvelope): String = prettyJson.encodeToString(envelope)

    fun toJson(run: MeasurementRun, metadata: DebugExportMetadata): String =
        toJson(DebugExportEnvelope(metadata, run))

    /** Export-safe JSON: preserves timing evidence while removing stable user/device values. */
    fun toPrivacySafeJson(run: MeasurementRun, metadata: DebugExportMetadata): String =
        toJson(run, metadata).redactStableIdentifiers(run)

    /** CSV de todos los [TrialRecord] de la corrida. Una fila por trial. */
    fun trialsToCsv(run: MeasurementRun): String {
        val header = listOf(
            "id", "blockId", "trialIndex", "exerciseId",
            "stimulusUptimeNanos", "responseUptimeNanos", "rtNanos",
            "isCorrect", "isAnticipation", "isLapse", "lapseMultiplier",
            "deadlineNanos", "difficultyEstimate",
            "timestampSource", "inputTimestampPrecision", "timebase",
        ).joinToString(",")
        val rows = run.trials.map { it.toCsvRow() }
        return (listOf(header) + rows).joinToString("\n")
    }

    /** Export-safe CSV with the same relational shape but no stable user/device values. */
    fun trialsToPrivacySafeCsv(run: MeasurementRun): String =
        trialsToCsv(run).redactStableIdentifiers(run)

    private fun TrialRecord.toCsvRow(): String = listOf(
        quote(id),
        quote(blockId),
        trialIndex.toString(),
        quote(exerciseId),
        stimulusUptimeNanos.toString(),
        responseUptimeNanos.toString(),
        rtNanos.toString(),
        isCorrect.toString(),
        isAnticipation.toString(),
        isLapse.toString(),
        lapseMultiplier.toString(),
        deadlineNanos?.toString() ?: "",
        difficultyEstimate.toString(),
        timestampSource.name,
        inputTimestampPrecision.name,
        timebase.name,
    ).joinToString(",")

    /** Entrecomilla y escapa comillas internas para campos de texto del CSV. */
    private fun quote(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

    private fun String.redactStableIdentifiers(run: MeasurementRun): String {
        val replacements = listOf(
            run.deviceProfile.deviceModel to "redacted_device_model",
            run.deviceProfile.osVersion to "redacted_os_version",
            run.deviceProfile.id to "debug_device",
            run.session.userId to "debug_user",
        )
            .filter { (source, _) -> source.isNotBlank() }
            .distinctBy { (source, _) -> source }
            .sortedByDescending { (source, _) -> source.length }
        return replacements.fold(this) { output, (source, replacement) ->
            output.replace(source, replacement)
        }
    }
}
