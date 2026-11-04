package com.mindos.app.features.mindboost.core.copy

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeBank
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `MB-FOCUS-0089` / `FOC-P0-00` — the Enfoque claim freeze at C0, enforced rather than asked for.
 *
 * The product may say that a session happened, what it recorded, and how today compares with the
 * user's own earlier sessions. It may not say a capacity was MEASURED, IMPROVED or TRANSFERRED,
 * because nothing in the tree supports any of the three: there is no test-retest reliability, no
 * reliable-change threshold derived from observed variance, and no external instrument.
 *
 * One of the frozen families was not merely strong, it was FALSE, and
 * [everyProbeParadigmIsAlsoATrainedParadigm] is the fact that made it so — pinned here rather than
 * argued in a comment, because the copy is only wrong for as long as that fact holds. If somebody
 * later builds a probe on a paradigm the catalogue does not train, that test goes red, and its
 * failure is the invitation to revisit this gate with evidence.
 *
 * Scope is PRODUCTION STRING LITERALS under `features/mindboost/focus`, never whole files: the
 * forbidden words here are ordinary Spanish that the engine's own KDoc needs in order to reason
 * about the defect. See [ForbiddenLexicon.EFFICACY_CLAIMS_C0] for the measurement behind that.
 */
class FocusClaimLevelGateTest {

    @Test
    fun noFocusProductionLiteralClaimsAboveC0() {
        val files = ProductionCopyCorpus.ktFilesUnder(listOf(FOCUS_ROOT))
        assertTrue(files.size >= MIN_FILES, "walked only ${files.size} Enfoque files — root wrong?")

        var literals = 0
        val offenders = mutableListOf<String>()
        for (file in files) {
            for (literal in ProductionCopyCorpus.literalsOf(file.readText())) {
                literals++
                val hits = MindBoostCopySafetyScanner.scan(literal.text, ForbiddenLexicon.EFFICACY_CLAIMS_C0)
                if (hits.isNotEmpty()) {
                    offenders += "${file.name}:${literal.line} ${hits.map { it.term }} -> ${literal.text.take(120)}"
                }
            }
        }

        // Anti-vacuity: an extractor that yielded nothing would report the whole engine clean.
        assertTrue(literals >= MIN_LITERALS, "extracted only $literals literals — extractor broken?")
        assertTrue(
            offenders.isEmpty(),
            "Enfoque ships ${offenders.size} claim(s) above C0:\n${offenders.joinToString("\n")}",
        )
    }

    @Test
    fun theGateSeesLiteralsAndIgnoresComments_discriminantHalf() {
        // Assembled from the lexicon so this file carries no contiguous claim literal of its own.
        val term = ForbiddenLexicon.EFFICACY_CLAIMS_C0.first { it == "transfer" + "encia" }
        val source = """
            |// reasoning about $term is documentation, not copy
            |/* neither is this $term */
            |val shipped = "una $term demostrada"
            |val ident = ${term}Index
        """.trimMargin()

        val found = ProductionCopyCorpus.literalsOf(source)
            .flatMap { lit -> MindBoostCopySafetyScanner.scan(lit.text, ForbiddenLexicon.EFFICACY_CLAIMS_C0) }

        // Exactly one: the literal. Whole-file matching here would forbid the engine from documenting
        // its own defect — this very file names four of the frozen terms in prose.
        assertEquals(1, found.size, "expected only the literal to match, got ${found.map { it.term }}")
    }

    @Test
    fun theThreeLexiconsStayDisjoint() {
        // Promoting any of these into ALL restores whole-file matching for ordinary Spanish words and
        // the build would fail on honest KDoc; ENGLISH_CLAIMS is a different surface question again.
        val intoAll = ForbiddenLexicon.EFFICACY_CLAIMS_C0.filter { it in ForbiddenLexicon.ALL }
        val intoEnglish = ForbiddenLexicon.EFFICACY_CLAIMS_C0.filter { it in ForbiddenLexicon.ENGLISH_CLAIMS }
        assertTrue(intoAll.isEmpty(), "C0 claim terms must not enter the whole-file lexicon: $intoAll")
        assertTrue(intoEnglish.isEmpty(), "C0 claim terms must not enter the English lexicon: $intoEnglish")

        // Discriminant half: all three lists are non-empty, so the emptiness above means something.
        assertTrue(ForbiddenLexicon.EFFICACY_CLAIMS_C0.size >= MIN_TERMS)
        assertTrue(ForbiddenLexicon.ALL.size >= MIN_TERMS)
        assertTrue(ForbiddenLexicon.ENGLISH_CLAIMS.size >= MIN_TERMS)
    }

    @Test
    fun everyProbeParadigmIsAlsoATrainedParadigm() {
        val trained = ExerciseCatalog.implemented
            .map { ExerciseCatalog.definition(it).paradigm }
            .toSet()
        assertTrue(trained.size >= MIN_TRAINED_PARADIGMS, "read only ${trained.size} trained paradigms")

        val probeParadigms = ProbeBank.all.map { it.paradigm }.toSet()
        assertTrue(probeParadigms.isNotEmpty(), "no probes read — ProbeBank empty?")

        val untrained = probeParadigms - trained
        assertEquals(
            emptySet(), untrained,
            "A probe now runs a paradigm the catalogue does not train: $untrained. ProbeBank reserves " +
                "disjoint SEEDS, which makes its probes PARALLEL FORMS of trained tasks — that is why " +
                "copy calling them untrained was false. If this is a genuinely untrained instrument, the " +
                "claim ladder can be revisited with evidence, and this gate reviewed with it.",
        )
    }

    private companion object {
        val FOCUS_ROOT = "${ProductionCopyCorpus.COMMON_MAIN}/features/mindboost/focus"
        const val MIN_FILES = 100
        const val MIN_LITERALS = 400
        const val MIN_TERMS = 10
        const val MIN_TRAINED_PARADIGMS = 10
    }
}
