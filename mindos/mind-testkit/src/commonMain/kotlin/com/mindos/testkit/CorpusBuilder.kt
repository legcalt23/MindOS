package com.mindos.testkit

/** Category of a corpus entry — drives which invariants a test asserts for it. */
enum class CorpusCategory { VALID, ALIAS, MISSPELLING, EDGE_CASE, MALFORMED }

/**
 * One corpus row. [expectedIntentId] is non-null only when the entry should deterministically
 * resolve to a known intent (VALID/ALIAS); for MISSPELLING/EDGE_CASE/MALFORMED the only guarantees
 * are "no crash" and "deterministic".
 */
data class CorpusEntry(
    val text: String,
    val category: CorpusCategory,
    val expectedIntentId: String?,
)

/**
 * Builds a large, deterministic command corpus (ZIP04 COMMAND_CORPUS_GUIDE) from a grammar + alias
 * list: valid commands, semantics-preserving variants, aliases, misspellings, edge cases and
 * malformed inputs. Generated programmatically (reproducible by seed) rather than stored as a
 * 10k-line file.
 */
class CorpusBuilder(
    private val grammar: CommandGrammar,
    private val aliases: List<Pair<String, String>> = emptyList(),
) {
    private val generator = CommandGenerator(grammar)
    private val mutator = FuzzMutator()

    fun build(targetSize: Int, seed: Long = 1L): List<CorpusEntry> {
        val rng = SeededRng(seed)
        val out = ArrayList<CorpusEntry>(targetSize + EDGE_CASES.size)
        out += EDGE_CASES
        while (out.size < targetSize) {
            val generated = generator.generate(rng)
            out += when (rng.nextInt(6)) {
                0, 1 -> CorpusEntry(generated.text, CorpusCategory.VALID, generated.expectedIntentId)
                2 -> {
                    val variant = mutator.mutatePreserving(generated.text, rng)
                    CorpusEntry(variant.text, CorpusCategory.VALID, generated.expectedIntentId)
                }
                3 -> if (aliases.isNotEmpty()) {
                    val alias = rng.pick(aliases)
                    CorpusEntry(alias.first, CorpusCategory.ALIAS, alias.second)
                } else {
                    CorpusEntry(generated.text, CorpusCategory.VALID, generated.expectedIntentId)
                }
                4 -> CorpusEntry(mutator.typoMutate(generated.text, rng), CorpusCategory.MISSPELLING, null)
                else -> CorpusEntry(mutator.corruptMutate(generated.text, rng), CorpusCategory.MALFORMED, null)
            }
        }
        return out
    }

    private companion object {
        val EDGE_CASES: List<CorpusEntry> = listOf(
            CorpusEntry("", CorpusCategory.EDGE_CASE, null),
            CorpusEntry("   ", CorpusCategory.EDGE_CASE, null),
            CorpusEntry("\t\n", CorpusCategory.EDGE_CASE, null),
            CorpusEntry("a", CorpusCategory.EDGE_CASE, null),
            CorpusEntry("x".repeat(600), CorpusCategory.EDGE_CASE, null),
        )
    }
}
