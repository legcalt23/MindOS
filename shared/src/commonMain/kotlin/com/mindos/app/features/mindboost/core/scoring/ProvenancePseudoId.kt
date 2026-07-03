package com.mindos.app.features.mindboost.core.scoring

/**
 * FNV-1a pseudonymization of the raw user id for [ScoreProvenance.pseudoUserId] — provenance must NEVER
 * carry a raw uid. Deterministic and non-reversible-enough for a local provenance tag (the encrypted store
 * is the real privacy boundary). This is the SINGLE shared implementation used by every module's score
 * derivation (Memoria span/pairs/deferred, Decisiones reversibility, …); module-local copies were
 * consolidated here so the hashing lives in exactly one place. Module-wide `internal` keeps it off the
 * public surface while reachable across feature packages within `:shared`.
 */
@Suppress("MagicNumber") // FNV-1a offset basis + prime.
internal fun pseudoUserId(userId: String): String {
    var hash = 0xcbf29ce484222325UL
    val prime = 0x100000001b3UL
    userId.encodeToByteArray().forEach { byte ->
        hash = hash xor byte.toUByte().toULong()
        hash *= prime
    }
    return hash.toString(36)
}
