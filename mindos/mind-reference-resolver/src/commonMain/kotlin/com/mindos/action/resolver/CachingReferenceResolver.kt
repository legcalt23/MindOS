package com.mindos.action.resolver

import com.mindos.action.model.ReferenceKind
import kotlinx.coroutines.CancellationException

/**
 * Caches successful resolutions from a delegate. Only [ReferenceResolutionResult.Resolved] is
 * cached (negative results stay re-queryable — a username may be created later). Deterministic for
 * a fixed backing store. Re-throws cancellation so structured concurrency is preserved.
 *
 * NOTE: the cache has no eviction/TTL in v1 (deferred). Suitable for short-lived resolution where
 * the canonical mapping is stable within a session.
 */
class CachingReferenceResolver(
    private val delegate: ReferenceResolver,
) : ReferenceResolver {

    override val kind: ReferenceKind get() = delegate.kind

    private val cache = HashMap<String, String>()

    override suspend fun resolve(rawValue: String): ReferenceResolutionResult {
        cache[rawValue]?.let { return ReferenceResolutionResult.Resolved(it) }
        return try {
            delegate.resolve(rawValue).also { result ->
                if (result is ReferenceResolutionResult.Resolved) cache[rawValue] = result.canonicalId
            }
        } catch (e: CancellationException) {
            throw e
        }
    }

    /** Visible for tests/diagnostics. */
    fun cachedCount(): Int = cache.size
}
