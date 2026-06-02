package com.mindos.action.resolver

import com.mindos.action.model.ReferenceKind
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.coroutines.CancellationException

/**
 * Caches successful resolutions from a delegate. Negative results stay re-queryable: a username may
 * be created later. The in-memory cache is process-local, thread-safe, TTL-bound, and max-size
 * bounded. Distributed production deployments should inject a durable resolver/cache outside this
 * reference implementation.
 */
@OptIn(ExperimentalAtomicApi::class)
class CachingReferenceResolver(
    private val delegate: ReferenceResolver,
    private val ttlMillis: Long = DefaultTtlMillis,
    private val maxEntries: Int = DefaultMaxEntries,
    private val clock: ResolverClock = ResolverClocks.Zero,
) : ReferenceResolver {

    override val kind: ReferenceKind get() = delegate.kind

    init {
        require(ttlMillis > 0L) { "ttlMillis must be positive." }
        require(maxEntries > 0) { "maxEntries must be positive." }
    }

    private val cache = AtomicReference<Map<String, CacheEntry>>(emptyMap())

    override suspend fun resolve(rawValue: String): ReferenceResolutionResult {
        lookup(rawValue)?.let { return ReferenceResolutionResult.Resolved(it) }
        return try {
            delegate.resolve(rawValue).also { result ->
                if (result is ReferenceResolutionResult.Resolved) store(rawValue, result.canonicalId)
            }
        } catch (e: CancellationException) {
            throw e
        }
    }

    /** Visible for tests/diagnostics. */
    fun cachedCount(): Int {
        pruneExpired()
        return cache.load().size
    }

    private fun lookup(rawValue: String): String? {
        val now = clock.nowMillis()
        while (true) {
            val current = cache.load()
            val pruned = current.filterValues { it.expiresAtMillis > now }
            val hit = pruned[rawValue]?.canonicalId
            if (current.size == pruned.size || cache.compareAndSet(current, pruned)) return hit
        }
    }

    private fun store(rawValue: String, canonicalId: String) {
        val now = clock.nowMillis()
        val entry = CacheEntry(canonicalId = canonicalId, expiresAtMillis = safeExpiresAt(now, ttlMillis))
        while (true) {
            val current = cache.load()
            val next = LinkedHashMap<String, CacheEntry>(current.filterValues { it.expiresAtMillis > now })
            next.remove(rawValue)
            next[rawValue] = entry
            while (next.size > maxEntries) {
                val oldest = next.keys.firstOrNull() ?: break
                next.remove(oldest)
            }
            if (cache.compareAndSet(current, next)) return
        }
    }

    private fun pruneExpired() {
        val now = clock.nowMillis()
        while (true) {
            val current = cache.load()
            val pruned = current.filterValues { it.expiresAtMillis > now }
            if (current.size == pruned.size || cache.compareAndSet(current, pruned)) return
        }
    }

    private fun safeExpiresAt(now: Long, ttl: Long): Long =
        if (Long.MAX_VALUE - now < ttl) Long.MAX_VALUE else now + ttl

    private data class CacheEntry(
        val canonicalId: String,
        val expiresAtMillis: Long,
    )

    companion object {
        const val DefaultTtlMillis: Long = 5 * 60 * 1_000L
        const val DefaultMaxEntries: Int = 10_000
    }
}

fun interface ResolverClock {
    fun nowMillis(): Long
}

object ResolverClocks {
    val Zero = ResolverClock { 0L }
}
