package com.mindos.app.features.mindboost.core.session.cognitive

import kotlinx.serialization.json.JsonObject

/**
 * An opaque, versioned, exercise-specific metric payload. The unified [CognitiveSessionRun] stays
 * exercise-agnostic: a new exercise adds a [type] + [schemaVersion] + JSON [payload] and its OWN codec,
 * editing NOTHING in `core/session`. [type] namespaces the payload (e.g. "gonogo_inhibition"); [schemaVersion]
 * versions its shape so a reader can migrate or skip an unknown version.
 */
data class ExerciseSpecificMetricPayload(
    val type: String,
    val schemaVersion: Int,
    val payload: JsonObject,
) {
    init {
        require(type.isNotBlank()) { "type must not be blank." }
        require(schemaVersion >= 1) { "schemaVersion must be >= 1 (was $schemaVersion)." }
    }
}

/**
 * Encodes / decodes / validates a typed exercise payload [T] to and from an [ExerciseSpecificMetricPayload].
 * Each FEATURE owns its codec (the E8 codec lives in the speed feature); `core/session` defines only this
 * contract, so adding an exercise never touches the core model.
 */
interface ExerciseSpecificMetricCodec<T> {
    /** The payload [ExerciseSpecificMetricPayload.type] this codec handles. */
    val type: String

    /** The payload [ExerciseSpecificMetricPayload.schemaVersion] this codec writes. */
    val schemaVersion: Int

    /** Serializes [value] into a typed, versioned payload. */
    fun encode(value: T): ExerciseSpecificMetricPayload

    /** Parses a payload back into [T]. Throws only on a payload this codec cannot honor. */
    fun decode(payload: ExerciseSpecificMetricPayload): T

    /** True when [payload] matches this codec's [type] and a supported [schemaVersion] with required fields. */
    fun validate(payload: ExerciseSpecificMetricPayload): Boolean
}
