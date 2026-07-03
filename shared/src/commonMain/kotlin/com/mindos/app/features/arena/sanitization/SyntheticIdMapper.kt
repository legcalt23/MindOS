package com.mindos.app.features.arena.sanitization

import com.mindos.app.features.arena.config.UuidV4
import kotlin.random.Random

/**
 * Phase 07.5: One-way mapper from any raw identifier to a synthetic UUIDv4.
 *
 * The mapping is internal and never reversible from the outside (no reverse lookup is exposed). The
 * same raw id maps to the same synthetic id within a mapper instance; with a fixed [seed] and a
 * fixed call order the synthetic ids are deterministic, supporting reproducible simulations.
 */
class SyntheticIdMapper(seed: Long) {

    private val random = Random(seed)
    private val mapping = mutableMapOf<String, String>()

    /** Returns a stable synthetic UUIDv4 for [rawId], minting one on first encounter. */
    fun map(rawId: String): String = mapping.getOrPut(rawId) { UuidV4.random(random) }

    /** Number of distinct raw ids mapped so far. */
    fun size(): Int = mapping.size
}
