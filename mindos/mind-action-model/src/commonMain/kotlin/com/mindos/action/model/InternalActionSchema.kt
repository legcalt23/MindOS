package com.mindos.action.model

import kotlinx.serialization.Serializable

object InternalActionSchema {
    const val Current: Int = 1
}

@Serializable
data class VersionedMindActionEnvelope(
    val schemaVersion: Int = InternalActionSchema.Current,
    val action: MindAction,
) {
    init {
        require(schemaVersion == InternalActionSchema.Current) {
            "Unsupported internal action schema version $schemaVersion."
        }
    }
}
