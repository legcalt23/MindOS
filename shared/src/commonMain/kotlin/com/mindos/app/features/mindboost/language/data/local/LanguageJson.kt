package com.mindos.app.features.mindboost.language.data.local

import com.mindos.app.features.mindboost.language.domain.adaptation.ExposureSnapshot
import com.mindos.app.features.mindboost.language.domain.model.ConsentState
import com.mindos.app.features.mindboost.language.domain.model.EvidencePacket
import com.mindos.app.features.mindboost.language.domain.model.SessionSummary
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Single JSON configuration for the language feature's local persistence (mirrors `FocusJson`
 * / `SpeedJson`).
 * - `ignoreUnknownKeys`: forward reads when the schema gains fields.
 * - `encodeDefaults`: always writes `schemaVersion` and other default fields.
 */
object LanguageJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    const val CURRENT_SCHEMA_VERSION = 1
}

/** Versioned persisted envelopes (project rule: version DTOs with `schemaVersion`). */
@Serializable
data class PersistedPacket(
    val schemaVersion: Int = LanguageJson.CURRENT_SCHEMA_VERSION,
    val packet: EvidencePacket,
)

@Serializable
data class PersistedSummary(
    val schemaVersion: Int = LanguageJson.CURRENT_SCHEMA_VERSION,
    val summary: SessionSummary,
)

@Serializable
data class PersistedConsent(
    val schemaVersion: Int = LanguageJson.CURRENT_SCHEMA_VERSION,
    val consent: ConsentState,
)

@Serializable
data class PersistedExposure(
    val schemaVersion: Int = LanguageJson.CURRENT_SCHEMA_VERSION,
    val snapshot: ExposureSnapshot,
)
