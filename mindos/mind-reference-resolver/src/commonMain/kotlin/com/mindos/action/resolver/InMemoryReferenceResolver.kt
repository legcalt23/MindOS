package com.mindos.action.resolver

import com.mindos.action.model.ReferenceKind

/**
 * Deterministic in-memory resolver backed by a fixed table. Used for tests and as a reference
 * implementation; the real (Firestore-backed) resolver is injected by the app and kept out of
 * this KMP-pure module.
 */
class InMemoryReferenceResolver(
    override val kind: ReferenceKind,
    private val table: Map<String, String>,
) : ReferenceResolver {
    override suspend fun resolve(rawValue: String): ReferenceResolutionResult =
        table[rawValue]?.let { ReferenceResolutionResult.Resolved(it) }
            ?: ReferenceResolutionResult.NotFound(rawValue)
}
