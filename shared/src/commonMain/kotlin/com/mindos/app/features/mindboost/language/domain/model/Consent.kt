package com.mindos.app.features.mindboost.language.domain.model

import kotlinx.serialization.Serializable

/**
 * Granular consent model (v6 §13 privacy, §10.5 own material). Consent is per-purpose: granting
 * cloud evaluation does NOT grant audio capture. The default for every type is *not granted*.
 */
@Serializable
enum class ConsentType {
    CLOUD_EVALUATION,
    OWN_MATERIAL,
    AUDIO_CAPTURE,
    ANALYTICS,
}

/**
 * Retention policy for data tied to a consent (v6 §13). [NONE] = process transiently, persist
 * nothing; [ABSTRACTION_ONLY] = keep evidence abstractions, never source content; [SESSION_ONLY]
 * = drop after the session; [FULL_OPT_IN] = retain per explicit opt-in.
 */
@Serializable
enum class RetentionPolicy {
    NONE,
    SESSION_ONLY,
    ABSTRACTION_ONLY,
    FULL_OPT_IN,
}

/**
 * One consent decision for one [userIdHash] and [consentType]. Immutable; updating consent
 * appends a new state with a fresh timestamp.
 */
@Serializable
data class ConsentState(
    val userIdHash: UserIdHash,
    val consentType: ConsentType,
    val granted: Boolean,
    val retention: RetentionPolicy,
    val updatedAtEpochMs: Long,
) {
    companion object {
        /** The safe default: nothing granted, nothing retained. */
        fun denied(userIdHash: UserIdHash, type: ConsentType, atEpochMs: Long): ConsentState =
            ConsentState(
                userIdHash = userIdHash,
                consentType = type,
                granted = false,
                retention = RetentionPolicy.NONE,
                updatedAtEpochMs = atEpochMs,
            )
    }
}
