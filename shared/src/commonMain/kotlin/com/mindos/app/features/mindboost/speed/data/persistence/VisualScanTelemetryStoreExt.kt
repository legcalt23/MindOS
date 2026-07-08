package com.mindos.app.features.mindboost.speed.data.persistence

import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.domain.score.VisualScanTelemetrySummaryV1
import kotlinx.serialization.encodeToString

/**
 * MC9-C-R3 — helpers TIPADOS de E5 sobre [ExerciseTelemetryStore]: ninguna otra capa debe (de)serializar
 * `summaryJson` a mano. Internamente usan el record genérico; la (de)serialización vive aquí, en un único sitio.
 * Un JSON corrupto o un `exerciseId` que no sea E5 NUNCA rompe la sesión: se descarta con seguridad (→ null).
 */

/** Persiste un summary de E5 (idempotente por clave `userId::sessionId`: re-guardar sobrescribe, no duplica). */
suspend fun ExerciseTelemetryStore.saveVisualScanSummary(
    userId: String,
    summary: VisualScanTelemetrySummaryV1,
    createdAtEpochMs: Long = summary.createdAtEpochMs,
) {
    save(
        ExerciseTelemetryRecord(
            sessionId = summary.sessionId,
            userId = userId,
            exerciseId = summary.exerciseId,
            // El wrapper refleja la versión de FÓRMULAS del payload; el esquema del summary va en summaryVersion.
            telemetryVersion = summary.metricsVersion,
            summaryJson = SpeedJson.instance.encodeToString(summary),
            createdAtEpochMs = createdAtEpochMs,
        ),
    )
}

/** Summary tipado de E5 para [sessionId] de [userId], o `null` si no existe / no es E5 / JSON corrupto. */
suspend fun ExerciseTelemetryStore.visualScanSummaryForSession(
    userId: String,
    sessionId: String,
): VisualScanTelemetrySummaryV1? = forSession(userId, sessionId)?.let(::decodeVisualScanSummary)

/** Historial tipado de summaries de E5 de [userId], reciente primero; descarta records no-E5 o corruptos. */
suspend fun ExerciseTelemetryStore.visualScanSummariesForUser(
    userId: String,
): List<VisualScanTelemetrySummaryV1> = forUser(userId).mapNotNull(::decodeVisualScanSummary)

/** Decodifica un record a summary de E5, o `null` si no es E5 (mismatch de ejercicio) o el JSON está corrupto. */
private fun decodeVisualScanSummary(record: ExerciseTelemetryRecord): VisualScanTelemetrySummaryV1? {
    if (record.exerciseId != SpeedExerciseId.E5_BUSQUEDA.code) return null
    // decodeFromString es CPU-puro (no suspend): runCatching no puede tragar una cancelación aquí.
    return runCatching {
        SpeedJson.instance.decodeFromString<VisualScanTelemetrySummaryV1>(record.summaryJson)
    }.getOrNull()
}
