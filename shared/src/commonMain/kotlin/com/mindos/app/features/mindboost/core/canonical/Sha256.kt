@file:Suppress("MagicNumber") // The literals ARE FIPS 180-4: the round constants, shifts and rotations of
// the published algorithm. Naming them would rename the specification, not explain it, and any edit to one is
// caught by the NIST vectors in Sha256Test rather than by a reader recognising a constant.

package com.mindos.app.features.mindboost.core.canonical

/**
 * SHA-256 (FIPS 180-4), pinned in commonMain.
 *
 * Why hand-rolled rather than a dependency: the same reason this repo pins its own splitmix64 instead of trusting
 * `kotlin.random.Random` — a digest that persisted rows are validated against must not change when a library
 * changes. There is no multiplatform SHA-256 in the current dependency set, and adding one to compute a
 * ~200-byte digest would be a supply-chain surface for no capability. This is ~60 lines of a fully specified,
 * frozen algorithm, anchored to the published FIPS 180-4 vectors in `Sha256Test`.
 *
 * Why SHA-256 and not the FNV-1a this replaces: FNV-1a 64 is a hash, not a digest. Its birthday bound is ~2^32,
 * which is a real number, not a theoretical one — and the previous KDoc called it "the canonical digest of every
 * load-bearing field", i.e. described a non-collision-resistant hash as if it proved identity. Either the claim
 * or the algorithm had to go. This is not a security boundary either (nothing here is adversarial: the rows are
 * in the user's own encrypted, account-bound namespace) — it is a drift detector whose collision probability is
 * small enough that "the digests match" is a statement about the data rather than about the hash.
 */
internal object Sha256 {

    /** Lowercase hex of the 32-byte digest of [message]. */
    fun hex(message: ByteArray): String {
        val digest = hash(message)
        return buildString(digest.size * 2) {
            for (byte in digest) {
                val value = byte.toInt() and BYTE_MASK
                append(HEX_CHARS[value ushr NIBBLE_BITS])
                append(HEX_CHARS[value and NIBBLE_MASK])
            }
        }
    }

    fun hash(message: ByteArray): ByteArray {
        val state = INITIAL_STATE.copyOf()
        val padded = pad(message)
        val schedule = IntArray(ROUNDS)
        var offset = 0
        while (offset < padded.size) {
            compress(padded, offset, state, schedule)
            offset += BLOCK_BYTES
        }
        val out = ByteArray(DIGEST_BYTES)
        for (i in state.indices) {
            out[i * WORD_BYTES] = (state[i] ushr 24).toByte()
            out[i * WORD_BYTES + 1] = (state[i] ushr 16).toByte()
            out[i * WORD_BYTES + 2] = (state[i] ushr 8).toByte()
            out[i * WORD_BYTES + 3] = state[i].toByte()
        }
        return out
    }

    /** 0x80, zero fill to 56 mod 64, then the message length in BITS as big-endian 64. */
    private fun pad(message: ByteArray): ByteArray {
        val bitLength = message.size.toLong() * Byte.SIZE_BITS
        var padLength = BLOCK_BYTES - ((message.size + 1 + LENGTH_BYTES) % BLOCK_BYTES)
        if (padLength == BLOCK_BYTES) padLength = 0
        val out = ByteArray(message.size + 1 + padLength + LENGTH_BYTES)
        message.copyInto(out)
        out[message.size] = PAD_MARKER
        var index = out.size - LENGTH_BYTES
        for (shift in LENGTH_SHIFTS) {
            out[index++] = (bitLength ushr shift).toByte()
        }
        return out
    }

    @Suppress("LongMethod") // The 64-round compression function is one indivisible unit of the FIPS spec.
    private fun compress(block: ByteArray, offset: Int, state: IntArray, w: IntArray) {
        for (i in 0 until WORDS_PER_BLOCK) {
            val p = offset + i * WORD_BYTES
            w[i] = ((block[p].toInt() and BYTE_MASK) shl 24) or
                ((block[p + 1].toInt() and BYTE_MASK) shl 16) or
                ((block[p + 2].toInt() and BYTE_MASK) shl 8) or
                (block[p + 3].toInt() and BYTE_MASK)
        }
        for (i in WORDS_PER_BLOCK until ROUNDS) {
            val s0 = rotr(w[i - 15], 7) xor rotr(w[i - 15], 18) xor (w[i - 15] ushr 3)
            val s1 = rotr(w[i - 2], 17) xor rotr(w[i - 2], 19) xor (w[i - 2] ushr 10)
            w[i] = w[i - 16] + s0 + w[i - 7] + s1
        }
        var a = state[0]
        var b = state[1]
        var c = state[2]
        var d = state[3]
        var e = state[4]
        var f = state[5]
        var g = state[6]
        var h = state[7]
        for (i in 0 until ROUNDS) {
            val bigS1 = rotr(e, 6) xor rotr(e, 11) xor rotr(e, 25)
            val ch = (e and f) xor (e.inv() and g)
            val temp1 = h + bigS1 + ch + K[i] + w[i]
            val bigS0 = rotr(a, 2) xor rotr(a, 13) xor rotr(a, 22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val temp2 = bigS0 + maj
            h = g
            g = f
            f = e
            e = d + temp1
            d = c
            c = b
            b = a
            a = temp1 + temp2
        }
        state[0] += a
        state[1] += b
        state[2] += c
        state[3] += d
        state[4] += e
        state[5] += f
        state[6] += g
        state[7] += h
    }

    private fun rotr(x: Int, n: Int): Int = (x ushr n) or (x shl (Int.SIZE_BITS - n))

    private const val BYTE_MASK = 0xFF
    private const val NIBBLE_MASK = 0x0F
    private const val NIBBLE_BITS = 4
    private const val BLOCK_BYTES = 64
    private const val WORD_BYTES = 4
    private const val WORDS_PER_BLOCK = 16
    private const val LENGTH_BYTES = 8
    private const val DIGEST_BYTES = 32
    private const val ROUNDS = 64
    private const val PAD_MARKER = 0x80.toByte()
    private const val HEX_CHARS = "0123456789abcdef"
    private val LENGTH_SHIFTS = intArrayOf(56, 48, 40, 32, 24, 16, 8, 0)

    private val INITIAL_STATE = intArrayOf(
        0x6a09e667, -0x4498517b, 0x3c6ef372, -0x5ab00ac6,
        0x510e527f, -0x64fa9774, 0x1f83d9ab, 0x5be0cd19,
    )

    private val K = intArrayOf(
        0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b,
        0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
        -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
        -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039,
        -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
        -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d,
        -0x2e6d17e7, -0x2966f9dc, -0xbf1ca7b, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8,
        -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e,
    )
}
