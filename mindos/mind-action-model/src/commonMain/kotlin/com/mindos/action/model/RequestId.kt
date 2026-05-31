package com.mindos.action.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Correlates one execution request across understanding → validation → resolution → navigation → audit. */
@Serializable
@JvmInline
value class RequestId(val value: String) {
    init {
        require(value.isNotBlank()) { "RequestId must not be blank" }
    }
}

/**
 * Deterministic request-id source (no hidden randomness in the model). The application injects a
 * real factory (e.g. UUID); tests inject a counter. Mirrors the foundation's ActionIdFactory.
 */
fun interface RequestIdFactory {
    fun next(): RequestId
}
