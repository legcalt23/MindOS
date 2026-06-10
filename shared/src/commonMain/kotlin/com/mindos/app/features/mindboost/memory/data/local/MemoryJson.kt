package com.mindos.app.features.mindboost.memory.data.local

import kotlinx.serialization.json.Json

/**
 * The single JSON configuration for MindBoost · Memoria local persistence.
 * - `ignoreUnknownKeys`: forward-compatible reads when the schema gains fields.
 * - `encodeDefaults`: always writes `schemaVersion` (and other defaulted fields).
 */
object MemoryJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
