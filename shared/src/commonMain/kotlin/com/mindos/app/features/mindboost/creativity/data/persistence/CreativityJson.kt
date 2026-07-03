package com.mindos.app.features.mindboost.creativity.data.persistence

import kotlinx.serialization.json.Json

/**
 * Single JSON configuration for Creativity practice persistence (mirrors `SpeedJson`).
 * - `ignoreUnknownKeys`: forward-compatible reads when the schema gains fields.
 * - `encodeDefaults`: always writes `schemaVersion` and other default fields.
 *
 * Local-only: these blobs are written to the encrypted `SecureKeyValueStore`, never sent
 * over the network. No AI-derived field is persisted.
 */
object CreativityJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
