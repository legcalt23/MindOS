package com.mindos.app.features.mindboost.speed.debug

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mindos.app.features.mindboost.speed.data.persistence.MeasurementRun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Actual de Android (§16). El gating es `FLAG_DEBUGGABLE`: en builds release el flag es 0
 * y se devuelve un [DisabledDebugExporter] ⇒ la exportación es inaccesible (acceptance #19).
 * En debug, los archivos se escriben en `filesDir` (almacenamiento privado de la app); no
 * se exponen por compartición pública.
 */
@Composable
actual fun rememberDebugExporter(): DebugExporter {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) AndroidDebugExporter(context) else DisabledDebugExporter()
    }
}

private class AndroidDebugExporter(private val context: Context) : DebugExporter {
    override val isEnabled: Boolean = true

    override suspend fun export(run: MeasurementRun, metadata: DebugExportMetadata): DebugExportResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, EXPORT_DIR).apply { mkdirs() }
                val exportId = UUID.randomUUID().toString()
                val jsonFile = File(dir, "speed_run_$exportId.json")
                val csvFile = File(dir, "speed_trials_$exportId.csv")
                jsonFile.writeText(DebugExport.toPrivacySafeJson(run, metadata))
                csvFile.writeText(DebugExport.trialsToPrivacySafeCsv(run))
                DebugExportResult.Written(jsonFile.absolutePath, csvFile.absolutePath)
            }.getOrElse { DebugExportResult.Failed(it::class.simpleName ?: "unknown_error") }
        }

    private companion object {
        const val EXPORT_DIR = "speed_debug_exports"
    }
}
