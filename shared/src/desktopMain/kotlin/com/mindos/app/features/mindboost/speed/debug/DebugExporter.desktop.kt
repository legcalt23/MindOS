package com.mindos.app.features.mindboost.speed.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mindos.app.features.mindboost.speed.data.persistence.MeasurementRun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Actual de Desktop: exportador de desarrollo que escribe a un directorio temporal del
 * host. Desktop no es un target de producción de MVP-0 (Android-only), pero el exportador
 * se mantiene funcional para análisis local de datos del simulador.
 */
@Composable
actual fun rememberDebugExporter(): DebugExporter = remember { DesktopDebugExporter() }

private class DesktopDebugExporter : DebugExporter {
    override val isEnabled: Boolean = true

    override suspend fun export(run: MeasurementRun, metadata: DebugExportMetadata): DebugExportResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(System.getProperty("java.io.tmpdir"), EXPORT_DIR).apply { mkdirs() }
                val exportId = UUID.randomUUID().toString()
                val jsonFile = File(dir, "speed_run_$exportId.json")
                val csvFile = File(dir, "speed_trials_$exportId.csv")
                jsonFile.writeText(DebugExport.toPrivacySafeJson(run, metadata))
                csvFile.writeText(DebugExport.trialsToPrivacySafeCsv(run))
                DebugExportResult.Written(jsonFile.absolutePath, csvFile.absolutePath)
            }.getOrElse { DebugExportResult.Failed(it::class.simpleName ?: "unknown_error") }
        }

    private companion object {
        const val EXPORT_DIR = "mindos_speed_debug"
    }
}
