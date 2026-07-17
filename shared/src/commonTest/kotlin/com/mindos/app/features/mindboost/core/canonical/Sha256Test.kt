package com.mindos.app.features.mindboost.core.canonical

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Anchors the hand-rolled SHA-256 to the PUBLISHED FIPS 180-4 vectors.
 *
 * This is the only kind of golden that is genuinely independent: the expected values are not derived from this
 * implementation, or from any Kotlin — they are the vectors NIST published, reproduced in every SHA-256
 * implementation in existence, and verifiable by the reader against the standard without running this code.
 *
 * That distinction matters here because the sibling `E2FormGoldenTest` cannot make the same claim: its literals
 * came from a reference that is not committed, so its independence is REPORTED rather than VERIFIED. These are
 * VERIFIED. Everything digest-related upstream rests on this file being right, so it must not rest on the
 * implementation agreeing with itself.
 */
class Sha256Test {

    /** FIPS 180-4 / NIST: the empty string. Also exercises the pure-padding block path. */
    @Test
    fun empty_input_matches_the_published_vector() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Sha256.hex(ByteArray(0)))
    }

    /** FIPS 180-4 / NIST: "abc" — the canonical one-block vector. */
    @Test
    fun abc_matches_the_published_vector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Sha256.hex("abc".encodeToByteArray()),
        )
    }

    /** FIPS 180-4 / NIST: the 448-bit multi-block vector — exercises the message schedule across two blocks. */
    @Test
    fun the_two_block_vector_matches() {
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            Sha256.hex("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray()),
        )
    }

    /** FIPS 180-4 / NIST: the 896-bit vector — a longer multi-block message. */
    @Test
    fun the_long_multi_block_vector_matches() {
        assertEquals(
            "cf5b16a778af8380036ce59e7b0492370b249b11e8f07a51afac45037afee9d1",
            Sha256.hex(
                ("abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmno" +
                    "ijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu").encodeToByteArray(),
            ),
        )
    }

    /** One million 'a' — the standard long-message vector. Exercises length encoding beyond a byte. */
    @Test
    fun the_million_a_vector_matches() {
        assertEquals(
            "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
            Sha256.hex(ByteArray(1_000_000) { 'a'.code.toByte() }),
        )
    }

    /** The digest is exactly 32 bytes / 64 lowercase hex chars, always — including when a byte is zero. */
    @Test
    fun the_digest_is_always_thirty_two_bytes_of_lowercase_hex() {
        listOf(ByteArray(0), ByteArray(1), ByteArray(64), ByteArray(65), "x".encodeToByteArray()).forEach { input ->
            val hex = Sha256.hex(input)
            assertEquals(64, hex.length, "digest length for ${input.size} bytes")
            assertEquals(hex.lowercase(), hex, "hex must be lowercase")
            assertEquals(32, Sha256.hash(input).size, "digest byte length")
        }
    }

    /** Block-boundary lengths: 55/56/57 and 63/64/65 are where padding logic breaks if it is wrong. */
    @Test
    fun block_boundary_lengths_do_not_collide() {
        val digests = listOf(55, 56, 57, 63, 64, 65, 119, 120).map { n ->
            Sha256.hex(ByteArray(n) { 'a'.code.toByte() })
        }
        assertEquals(digests.size, digests.toSet().size, "padding must not make two lengths collide")
    }
}
