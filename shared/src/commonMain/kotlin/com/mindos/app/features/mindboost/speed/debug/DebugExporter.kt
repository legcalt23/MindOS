package com.mindos.app.features.mindboost.speed.debug

import androidx.compose.runtime.Composable
import com.mindos.app.features.mindboost.speed.data.persistence.MeasurementRun

/** Resultado de un intento de exportación de depuración. */
sealed interface DebugExportResult {
    /** Exportación deshabilitada (build release). Nunca escribe nada (§16, acceptance #19). */
    data object DisabledInRelease : DebugExportResult

    /** Exportación escrita en almacenamiento privado de la app. */
    data class Written(val jsonPath: String, val csvPath: String) : DebugExportResult

    /** Fallo de escritura. */
    data class Failed(val reason: String) : DebugExportResult
}

/**
 * Escritor de exportaciones de depuración. Cifrar/serializar es puro ([DebugExport]); esta
 * superficie añade el gating debug/release y la escritura a almacenamiento privado (§16).
 *
 * - En builds release [isEnabled] es `false` y [export] devuelve [DebugExportResult.DisabledInRelease].
 * - Los archivos se guardan en almacenamiento privado de la app; NO se comparten por defecto.
 */
interface DebugExporter {
    /** `false` en release ⇒ la UI oculta la opción y [export] no escribe nada. */
    val isEnabled: Boolean

    suspend fun export(run: MeasurementRun, metadata: DebugExportMetadata): DebugExportResult
}

/** Exportador deshabilitado (no-op). Base de release y de fallbacks no soportados. */
class DisabledDebugExporter : DebugExporter {
    override val isEnabled: Boolean = false
    override suspend fun export(run: MeasurementRun, metadata: DebugExportMetadata): DebugExportResult =
        DebugExportResult.DisabledInRelease
}

/**
 * Factory por plataforma. Android decide el gating con `FLAG_DEBUGGABLE` (release ⇒
 * deshabilitado) y escribe en `filesDir`. Desktop escribe en un directorio temporal de
 * desarrollo. iOS devuelve un exportador deshabilitado (MVP-0 es solo Android).
 */
@Composable
expect fun rememberDebugExporter(): DebugExporter
