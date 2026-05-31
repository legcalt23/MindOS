package com.mindos.action.understanding

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Stable, app-defined identifier for a recognized intent (e.g. "open_settings").
 * The engine is intent-agnostic: it recognizes an id; the application maps that id to its
 * own sealed action/route downstream. This keeps the understanding layer reusable.
 */
@Serializable
@JvmInline
value class MindIntentId(val value: String) {
    init {
        require(value.isNotBlank()) { "MindIntentId must not be blank" }
    }
}

/** A recognized intent. Minimal by design; richer mapping is the resolver's job. */
@Serializable
data class MindIntent(val id: MindIntentId)

/** Name of a slot inside a parametric pattern (e.g. "target", "user", "text"). */
@Serializable
@JvmInline
value class SlotName(val value: String) {
    init {
        require(value.isNotBlank()) { "SlotName must not be blank" }
    }
}
