package com.mindos.app.features.mindboost.focus.data.local

import kotlinx.serialization.json.Json

/**
 * The single JSON configuration for MindBoost local persistence.
 * - `ignoreUnknownKeys`: forward-compatible reads when the schema gains fields.
 * - `encodeDefaults`: always writes `schemaVersion` (and other defaulted fields).
 */
object FocusJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
