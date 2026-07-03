package com.mindos.app.features.arena.integrity.engine

/**
 * Phase 05: Anti-cheat secrecy policy (anti_cheat_secrecy_rules.md).
 *
 * User-facing copy must NEVER reveal detection internals — thresholds, canary triggers, entropy
 * methods, the collusion graph, solver detection, smurf scoring, or geo-velocity limits. This policy
 * exposes the forbidden tokens and a guard so any user-bound string can be checked before it ships.
 */
object SecrecyPolicy {

    /** Lowercased tokens that must never appear in user-facing copy. */
    val SECRET_TOKENS: Set<String> = setOf(
        "agrs",
        "canary",
        "entropy",
        "collusion",
        "solver",
        "smurf",
        "boosting",
        "geo-velocity",
        "geovelocity",
        "fingerprint",
        "threshold",
    )

    /** True if [text] reveals none of the secret detection internals. */
    fun isSafeForUser(text: String): Boolean {
        val lower = text.lowercase()
        return SECRET_TOKENS.none { lower.contains(it) }
    }
}
