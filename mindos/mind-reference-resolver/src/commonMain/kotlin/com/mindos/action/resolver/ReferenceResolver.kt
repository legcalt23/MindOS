package com.mindos.action.resolver

import com.mindos.action.model.ReferenceKind

/** Typed outcome of resolving one reference. A miss is a result, never an exception. */
sealed interface ReferenceResolutionResult {
    data class Resolved(val canonicalId: String) : ReferenceResolutionResult
    data class NotFound(val rawValue: String) : ReferenceResolutionResult
    data class Failed(val message: String) : ReferenceResolutionResult
}

/**
 * Resolves a human-facing reference (username, slug, alias) to a canonical id for one
 * [ReferenceKind] (ZIP03 Resolver Engine).
 *
 * Contract: deterministic (same input ⇒ same output for a fixed backing store), cacheable, and
 * testable. `suspend` because real implementations perform a lookup (the first I/O boundary in the
 * system). Must re-throw `CancellationException`.
 */
interface ReferenceResolver {
    val kind: ReferenceKind
    suspend fun resolve(rawValue: String): ReferenceResolutionResult
}
