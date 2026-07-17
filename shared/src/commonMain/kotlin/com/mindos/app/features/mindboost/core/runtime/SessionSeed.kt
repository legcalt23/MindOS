package com.mindos.app.features.mindboost.core.runtime

import kotlin.random.Random

/**
 * The pinned random-stream algorithm a [SessionSeed] was drawn against.
 *
 * A seed without its version is not reproducible: the same `Long` fed to two different algorithms yields two
 * different sessions. Every seed therefore carries the version that gives it meaning, and [DeterministicRng]
 * refuses to interpret a seed under an algorithm it was not drawn for.
 *
 * Adding a version is allowed; changing what an existing version computes is not — `DeterministicRngGoldenTest`
 * pins each version's stream to fixed vectors, so a silent algorithm change fails the build rather than
 * re-rolling every historical session.
 */
enum class RngVersion(val id: String) {
    /** splitmix64, as implemented by [DeterministicRng]. The only version in existence. */
    V1("splitmix64-v1"),
    ;

    companion object {
        /**
         * The version whose [id] is [id], or null if unknown — fail-closed by construction: a persisted identity
         * naming an algorithm this build does not have must NOT be silently reinterpreted under another one.
         *
         * Persist [id], never `ordinal` or `name`: an ordinal reorders the moment someone alphabetizes the enum,
         * and would silently re-point every stored session at a different algorithm.
         */
        fun fromId(id: String): RngVersion? = entries.firstOrNull { it.id == id }
    }
}

/**
 * The complete, self-describing origin of a session's stimulus sequence.
 *
 * The generators are already pure — `TrialGenerator.generate(difficulty, spec, seed)` is a function, not a
 * process — so `(difficulty, spec, seed)` fully determines what the user saw. Persisting or replaying a session
 * therefore only requires keeping this value; re-running the generator reproduces the sequence exactly.
 *
 * Deliberately not a bare `Long`: a bare seed loses its [version], and a bare seed is what invites a wall-clock
 * value to be passed as one (see [SeedSource]).
 */
data class SessionSeed(
    val value: Long,
    val version: RngVersion,
)

/**
 * Mints the seed for a new session.
 *
 * Injected rather than called statically so tests can pin a sequence without pinning a clock. Production must
 * supply an unpredictable source; a test supplies a fixed one.
 */
fun interface SeedSource {
    fun mint(): SessionSeed
}

/**
 * The production seed source.
 *
 * Draws from the platform's own generator rather than the wall clock. Two of the wall clock's defects are real
 * measurement problems: sessions started in the same millisecond receive *identical* sequences, and the value is
 * observable to the user, which makes an upcoming stimulus sequence predictable to anyone who wants to game it.
 *
 * A third claim — that consecutive wall-clock seeds are "near-neighbours" producing similar sequences — was
 * asserted in the first draft of this work and is **false**: both XorWow and splitmix64 scramble the seed into
 * state, so seed `t` and seed `t+1` yield statistically unrelated streams (pinned by
 * `DeterministicRngGoldenTest`). The correction matters because it changes what this class does and does not
 * fix: an unpredictable seed solves **predictability**; it does nothing whatsoever for **item exposure**. Only a
 * persisted seed plus an exposure ledger can make exposure computable at all, and neither exists yet — a seed
 * that is never stored means the item history of every session ever run is unrecoverable. Entropy is not
 * exposure control, and this class must not be cited as if it were.
 */
object PlatformSeedSource : SeedSource {
    override fun mint(): SessionSeed = SessionSeed(Random.Default.nextLong(), RngVersion.V1)
}
