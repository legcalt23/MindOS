package com.mindos.app.features.mindboost.speed.data.persistence

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Configuración JSON única de la persistencia de Velocidad Mental (espejo de `FocusJson`).
 * - `ignoreUnknownKeys`: lecturas hacia delante cuando el esquema gane campos.
 * - `encodeDefaults`: escribe siempre `schemaVersion` y demás campos por defecto.
 */
object SpeedJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}

/**
 * Envoltura persistida de una corrida con versión de esquema (regla de proyecto: versionar
 * DTOs con `schemaVersion`). Permite migrar el formato sin perder datos antiguos.
 */
@Serializable
data class PersistedRunDto(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val run: MeasurementRun,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
