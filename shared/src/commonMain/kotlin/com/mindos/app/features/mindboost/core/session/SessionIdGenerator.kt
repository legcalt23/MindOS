package com.mindos.app.features.mindboost.core.session

import kotlin.random.Random

/**
 * Produces opaque, unlinkable session identifiers for a [SessionRecord]. The id is deliberately NOT derived
 * from any user attribute (no userId, email, display name, or `"$userId:$startedAtMs"`), so a SessionRecord
 * cannot be correlated to a person from its id alone. Injectable so tests can supply deterministic ids.
 */
fun interface SessionIdGenerator {
    fun newSessionId(): String
}

/**
 * Default generator: two random 64-bit values rendered as fixed-width hex — an opaque ~128-bit token with a
 * neutral prefix. Uses [kotlin.random.Random]; non-determinism across runs is intentional (uniqueness). No
 * user data is involved, so the id carries no personal information.
 */
class RandomSessionIdGenerator(private val random: Random = Random) : SessionIdGenerator {
    override fun newSessionId(): String {
        val high = random.nextLong().toULong().toString(HEX_RADIX).padStart(HEX_WIDTH, '0')
        val low = random.nextLong().toULong().toString(HEX_RADIX).padStart(HEX_WIDTH, '0')
        return "$PREFIX$high$low"
    }

    private companion object {
        const val PREFIX = "sess_"
        const val HEX_RADIX = 16
        const val HEX_WIDTH = 16
    }
}
