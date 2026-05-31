package com.mindos.testkit

/** Whether a mutation should preserve the command's meaning (for stability properties) or may break it. */
enum class FuzzKind(val semanticsPreserving: Boolean) {
    EXTRA_SPACING(true),
    CASE_FLIP(true),
    ZERO_WIDTH(false),
    NFD_DECOMPOSE(true),
    TYPO(false),
    SYMBOL_INJECTION(false),
    EMOJI_INJECTION(false),
}

data class MutatedInput(val text: String, val kind: FuzzKind)

/**
 * Generates input mutations for fuzzing (ZIP04 FUZZING_STRATEGY): random spacing, unicode variants,
 * emoji, malformed symbols, typos. Semantics-preserving mutations must NOT change the resolved
 * intent (used to assert stable output); destructive ones must merely never crash the engine.
 *
 * Every non-ASCII character is built from its code point at runtime (no literal special chars in
 * source) to stay encoding-safe across platforms and editors.
 */
class FuzzMutator {

    fun mutate(input: String, rng: SeededRng): MutatedInput {
        val kind = rng.pick(FuzzKind.entries.toList())
        return MutatedInput(apply(kind, input, rng), kind)
    }

    fun mutatePreserving(input: String, rng: SeededRng): MutatedInput {
        val kind = rng.pick(FuzzKind.entries.filter { it.semanticsPreserving })
        return MutatedInput(apply(kind, input, rng), kind)
    }

    /** A pure typo (delete/duplicate/replace/swap) — used to build the "misspellings" corpus set. */
    fun typoMutate(input: String, rng: SeededRng): String = typo(input, rng)

    /** Inject a malformed symbol or emoji — used to build the "malformed" corpus set. */
    fun corruptMutate(input: String, rng: SeededRng): String =
        if (rng.nextBoolean()) insertAt(input, rng.pick(SYMBOLS).toString(), rng) else insertAt(input, rng.pick(EMOJI), rng)

    private fun apply(kind: FuzzKind, input: String, rng: SeededRng): String = when (kind) {
        FuzzKind.EXTRA_SPACING -> extraSpacing(input, rng)
        FuzzKind.CASE_FLIP -> caseFlip(input, rng)
        FuzzKind.ZERO_WIDTH -> insertAt(input, ZERO_WIDTH, rng)
        FuzzKind.NFD_DECOMPOSE -> nfdSwap(input)
        FuzzKind.TYPO -> typo(input, rng)
        FuzzKind.SYMBOL_INJECTION -> insertAt(input, rng.pick(SYMBOLS).toString(), rng)
        FuzzKind.EMOJI_INJECTION -> insertAt(input, rng.pick(EMOJI), rng)
    }

    private fun extraSpacing(input: String, rng: SeededRng): String {
        val sb = StringBuilder()
        for (ch in input) {
            sb.append(ch)
            if (ch == ' ' && rng.nextBoolean()) sb.append(" ".repeat(rng.nextIntInRange(1, 3)))
        }
        return if (rng.nextBoolean()) " $sb " else sb.toString()
    }

    private fun caseFlip(input: String, rng: SeededRng): String =
        buildString {
            for (ch in input) append(if (rng.nextBoolean()) ch.uppercaseChar() else ch.lowercaseChar())
        }

    /** Replace precomposed accents with their NFD form (base letter + combining mark). */
    private fun nfdSwap(input: String): String {
        var out = input
        for ((precomposed, decomposed) in NFD_PAIRS) out = out.replace(precomposed, decomposed)
        return out
    }

    private fun typo(input: String, rng: SeededRng): String {
        if (input.isEmpty()) return input
        val i = rng.nextInt(input.length)
        return when (rng.nextInt(4)) {
            0 -> input.removeRange(i, i + 1)
            1 -> input.substring(0, i) + input[i] + input.substring(i)
            2 -> input.substring(0, i) + rng.pick(LETTERS) + input.substring(i + 1)
            else -> if (i < input.length - 1) {
                val c = input.toCharArray(); val t = c[i]; c[i] = c[i + 1]; c[i + 1] = t; c.concatToString()
            } else {
                input
            }
        }
    }

    private fun insertAt(input: String, fragment: String, rng: SeededRng): String {
        val i = if (input.isEmpty()) 0 else rng.nextInt(input.length + 1)
        return input.substring(0, i) + fragment + input.substring(i)
    }

    private companion object {
        val ZERO_WIDTH: String = Char(0x200B).toString()
        val SYMBOLS = "!@#\$%^&*()<>?/\\|".toList()
        val LETTERS = "abcdefghijklmnopqrstuvwxyz".toList()

        // Emoji built from surrogate pairs: grinning, thumbs-up, party, rocket.
        val EMOJI: List<String> = listOf(
            surrogate(0xD83D, 0xDE00),
            surrogate(0xD83D, 0xDC4D),
            surrogate(0xD83C, 0xDF89),
            surrogate(0xD83D, 0xDE80),
        )

        // Precomposed accent -> base + combining mark (acute U+0301, tilde U+0303).
        val NFD_PAIRS: List<Pair<String, String>> = listOf(
            Char(0x00E1).toString() to charArrayOf('a', Char(0x0301)).concatToString(),
            Char(0x00E9).toString() to charArrayOf('e', Char(0x0301)).concatToString(),
            Char(0x00ED).toString() to charArrayOf('i', Char(0x0301)).concatToString(),
            Char(0x00F3).toString() to charArrayOf('o', Char(0x0301)).concatToString(),
            Char(0x00FA).toString() to charArrayOf('u', Char(0x0301)).concatToString(),
            Char(0x00F1).toString() to charArrayOf('n', Char(0x0303)).concatToString(),
        )

        private fun surrogate(high: Int, low: Int): String =
            charArrayOf(Char(high), Char(low)).concatToString()
    }
}
