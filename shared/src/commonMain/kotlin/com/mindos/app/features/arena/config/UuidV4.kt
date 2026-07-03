@file:Suppress("MagicNumber") // Bit masks / byte indices are intrinsic to the RFC-4122 layout.

package com.mindos.app.features.arena.config

import kotlin.random.Random

/**
 * Phase 06/07/07.5: Pure, multiplatform UUIDv4 generation and validation.
 *
 * Deliberately avoids java.util.UUID so it is usable from commonMain on every target. Used to mint
 * synthetic identifiers for profiles, simulation runs, and the sanitization id mapper — there are no
 * real-UID code paths anywhere in the arena module.
 */
object UuidV4 {

    private val HEX = "0123456789abcdef".toCharArray()
    private val DASH_POSITIONS = setOf(4, 6, 8, 10)

    private val PATTERN = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
    )

    /** Generates a random RFC-4122 version-4 UUID string using [random] (seedable for determinism). */
    fun random(random: Random): String {
        val bytes = ByteArray(16) { random.nextInt(0, 256).toByte() }
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte() // version 4
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte() // RFC-4122 variant
        return buildString {
            for (i in bytes.indices) {
                if (i in DASH_POSITIONS) append('-')
                val v = bytes[i].toInt() and 0xff
                append(HEX[v ushr 4])
                append(HEX[v and 0x0f])
            }
        }
    }

    /** True if [value] is a syntactically valid UUIDv4 (version 4, RFC-4122 variant). */
    fun isValid(value: String): Boolean = PATTERN.matches(value)
}
