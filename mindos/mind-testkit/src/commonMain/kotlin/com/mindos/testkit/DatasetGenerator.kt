package com.mindos.testkit

/**
 * Large-scale, category-aware, deterministic dataset generator (ZIP11 COMMAND_GENERATOR_SPEC).
 * Produces valid commands plus typo / spacing / capitalization / alias variants, each tagged with a
 * [CommandCategory] and [VariantKind]. Reproducible by seed; scales to 10k+/50k+ rows.
 */
class DatasetGenerator(
    grammar: CommandGrammar,
    private val aliases: List<AliasRow>,
    private val intentToCategory: Map<String, CommandCategory>,
) {
    private val generator = CommandGenerator(grammar)
    private val mutator = FuzzMutator()

    private fun category(intentId: String?): CommandCategory =
        intentId?.let { intentToCategory[it] } ?: CommandCategory.MIXED

    fun validRow(rng: SeededRng): DatasetRow {
        val g = generator.generate(rng)
        return DatasetRow(g.text, category(g.expectedIntentId), VariantKind.VALID, g.expectedIntentId)
    }

    fun aliasRow(rng: SeededRng): DatasetRow {
        val a = rng.pick(aliases)
        return DatasetRow(a.text, category(a.intentId), VariantKind.ALIAS, a.intentId)
    }

    fun typoRow(rng: SeededRng): DatasetRow {
        val g = generator.generate(rng)
        return DatasetRow(mutator.typoMutate(g.text, rng), category(g.expectedIntentId), VariantKind.TYPO, null)
    }

    fun spacingRow(rng: SeededRng): DatasetRow {
        val g = generator.generate(rng)
        return DatasetRow(mutator.spacingVariant(g.text, rng), category(g.expectedIntentId), VariantKind.SPACING, g.expectedIntentId)
    }

    fun capitalizationRow(rng: SeededRng): DatasetRow {
        val g = generator.generate(rng)
        return DatasetRow(mutator.capitalizationVariant(g.text, rng), category(g.expectedIntentId), VariantKind.CAPITALIZATION, g.expectedIntentId)
    }

    /** Mostly-valid corpus (valid + aliases), every row carries an expected intent. */
    fun buildCorpus(targetSize: Int, seed: Long = 11L): List<DatasetRow> {
        val rng = SeededRng(seed)
        val out = ArrayList<DatasetRow>(targetSize)
        while (out.size < targetSize) {
            out += if (aliases.isNotEmpty() && rng.nextInt(5) == 0) aliasRow(rng) else validRow(rng)
        }
        return out
    }

    /** Variation set: typo / spacing / capitalization / alias mix (semantics-preserving variants keep expected). */
    fun buildVariations(targetSize: Int, seed: Long = 50L): List<DatasetRow> {
        val rng = SeededRng(seed)
        val out = ArrayList<DatasetRow>(targetSize)
        while (out.size < targetSize) {
            out += when (rng.nextInt(4)) {
                0 -> typoRow(rng)
                1 -> spacingRow(rng)
                2 -> capitalizationRow(rng)
                else -> aliasRow(rng)
            }
        }
        return out
    }
}
