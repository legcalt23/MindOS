package com.mindos.app.features.mindboost.core.copy

/**
 * INFRA-05 · Canonical forbidden-copy lexicon for ALL MindBoost modules.
 *
 * Source of truth: `docs/.../KMP_MindBoost_Cognitive_Catalog_Implementation_Plan/12_COPY_SAFETY_CLAIMS.md`
 * (§1 lexicon) and `INFRA_05_COPY_SAFETY_SCANNER.md`. This is the in-code counterpart consumed by the
 * runtime/test side; the Gradle `mindboostCopySafetyScanner` task (Phase 3) keeps a parity copy.
 *
 * Invariant #c (non-negotiable): zero claims of far-transfer / clinical / IQ / fake-news / gambling /
 * emotion-regulation — **not even to negate them**. The matcher is whole-word, case- and accent-folded,
 * and crosses hyphens, so a compounded or negated occurrence of a covered form is still caught.
 *
 * COVERAGE, stated exactly — this paragraph replaces an overclaim (`MB-EMO-0023`). The matcher compares
 * FORMS, and a form is caught only if it is in [ALL]. Until this revision every entry was a masculine
 * singular Spanish lemma, so `clinica` and `diagnostica` were invisible while `clinico` was caught —
 * and Spanish disclaimers are written in the feminine ("evaluacion clinica", "valoracion diagnostica"),
 * so the blind spot sat on the worst case rather than the rare one. Each term now DECLARES how it
 * inflects ([Inflection], no default) and [ALL] carries the expanded forms. What is still NOT covered,
 * declared instead of implied:
 *  - Multi-word phrases match literally: no gender/number agreement is propagated inside a phrase.
 *  - Verb conjugation is out of scope ("apostar" matches only the infinitive).
 *
 * ENGLISH, corrected (`MB-EMO-0029`). The paragraph above previously claimed English lexemes were
 * "absent from the lexicon entirely" and that the English words "only ever appear in KDoc". Both
 * clauses were false, and measurement is what showed it — 10 of the 63 lemmas below ARE English or
 * acronyms (IQ, Gf, ADHD, TDAH, Alzheimer, odds, bankroll, stake, bet, fake news), matched
 * whole-file like every other entry in [ALL]; and one English claim was shipping inside a production
 * string literal, in the header of every creativity export. The rest of the English clinical/claim
 * vocabulary lives in [ENGLISH_CLAIMS] and is deliberately NOT part of [ALL]: see its KDoc for the
 * measured reason.
 *
 * This file lives under `mindboost/core/copy/` — deliberately OUTSIDE the per-feature dirs the scanner
 * walks (focus/memory/speed/logic/decisiones/language/creativity/emotions + AppStrings). That is why it
 * may hold the raw literals without self-tripping the gate. The Phase-3 task MUST exclude this package.
 *
 * Polysemy decisions vs doc 12 §1 (documented, NOT invented — see §1 "Nota de afinado" + §riesgos).
 *
 * The criterion is MEASURED false positives, not polysemy in the abstract — `MB-EMO-0031` was opened
 * because the wording below read as the absolute rule "polysemic ⇒ phrase" and prescribed narrowing
 * three bare gambling lemmas on that basis alone. Counted over the same 934 files the gate walks, the
 * roots that DID become phrases hit honest copy constantly: "transferencia" 34 times across 20 files,
 * "mental" 13, "trabajo" 9, "CI" 5, "balance" 4, "cognitivo" and "conducir" 1 each. "cuota", "stake",
 * "odds" and "bet" hit ZERO. Narrowing those would not buy false-positive relief, because there is
 * none to buy; and since the matcher is `\b`-anchored, every occurrence of a phrase contains its head
 * word at the same index, so a phrase catches a strict SUBSET of what the bare word catches. Narrowing
 * a blocking gate with nothing to gain is a loosening, which is why they stay bare
 * (`CopySafetyLexiconParityTest.aPhraseIsStrictlyWeakerThanItsHeadWord` pins that direction).
 *  - Polysemic roots are encoded as PHRASES **when the bare root has measured false positives**:
 *    "deterioro cognitivo" (not bare "cognitivo" — "perfil cognitivo funcional" is permitted),
 *    "salud mental" (not bare "mental"), "rindes mejor en el trabajo" / "mejor en los estudios"
 *    (not bare "trabajo"/"estudios" — "memoria de trabajo" is permitted), "conduccion" / "al volante"
 *    (not bare "conducir" — Spanish "conducir a" = "lead to").
 *  - Deliberately EXCLUDED: bare "CI" (two-letter, high false-positive risk; covered by
 *    "coeficiente intelectual"), and "balance" (semantic/permitted; gambling sense covered by
 *    "bankroll").
 *  - "bote" and "cuota" were flagged here as the highest false-positive risk of the list. Measured
 *    when inflection landed, that prediction held for ONE of the two: pluralising "cuota" flags
 *    `VectorFilterPlanner`, which writes "cuotas balanceadas" about CONDITION quotas (sampling) — a
 *    meaning the gambling family does not own. "bote"/"botes" hit zero, then and now. "cuota" stays
 *    [Inflection.LITERAL], which is the whole fix: the widened form never enters, so the bare lemma
 *    keeps its full coverage and the honest plural is never seen. Exactly three lemmas sit at that
 *    one-inflection distance — "cuota"/"cuotas", "stake"/"stakes", "odds"/"odd" — and nothing else in
 *    the family is near a collision ("bets", "bankrolls", "botes", "casinos" all hit zero).
 *  - Broad-transfer claims are PHRASES ("transferencia amplia" / "lejana", "generalizacion amplia"), never
 *    the bare root "transferencia": the product legitimately names near-transfer and legitimately DENIES
 *    transfer in KDoc, and blocking the root would flag `TransferLabel`, `NearTransferVerifier` and every
 *    honest disclaimer. Added in R1 because the single strongest affirmative claim shipping in production
 *    (language `ClaimEngine` E5) used a lexeme the lexicon did not carry — doc 12 §1 "Candidato #1".
 *
 * Known doc-internal conflict surfaced for the user: doc 12 §2 lists "Proxy interno, no diagnostico"
 * as PERMITTED, yet "diagnostico" is forbidden in §1 and invariant #c forbids it even when negated.
 * The invariant wins: "diagnostico" stays forbidden; that permitted phrase must be reformulated in
 * Phase 2 (e.g. "proxy interno — practica funcional, no es una nota").
 */
object ForbiddenLexicon {

    /**
     * How a lemma is turned into the set of forms the matcher compares.
     *
     * Declared per term with **no default on purpose**: a default would silently restore the
     * masculine-singular-only matching that `MB-EMO-0023` measured, and the next Spanish adjective
     * added to this file would be fail-open in exactly the same way. Choosing a class is the decision.
     */
    enum class Inflection {
        /**
         * Matched exactly as written. Correct for acronyms, English terms, multi-word phrases,
         * already-plural forms, and polysemic words whose other sense the product uses legitimately.
         */
        LITERAL,

        /** Spanish noun: singular + plural. */
        NUMBER,

        /** Spanish adjective given in masculine singular: all four gender/number forms. */
        GENDER_NUMBER,
    }

    /** A lexicon entry: the lemma as written, plus how it inflects. */
    internal data class Term(val lemma: String, val inflection: Inflection)

    private val FAR_TRANSFER_TERMS: List<Term> = listOf(
        noun("inteligencia"), literal("IQ"), literal("Gf"),
        literal("coeficiente intelectual"), literal("coeficiente de inteligencia"),
        literal("te hace mas listo"), literal("mejores decisiones"), literal("decides mejor"),
        literal("en la vida real"),
        noun("multitarea"), noun("productividad"), literal("al volante"), noun("conduccion"),
        literal("regulacion emocional"), literal("inteligencia emocional"),
        literal("controlas tus emociones"),
        noun("autocontrol"), noun("personalidad"), literal("te distraes menos con el movil"),
        literal("usas menos el telefono"), literal("rindes mejor en el trabajo"),
        literal("mejor en los estudios"),
        literal("transferencia amplia"), literal("transferencia lejana"),
        literal("generalizacion amplia"),
    )

    private val CLINICAL_TERMS: List<Term> = listOf(
        literal("TDAH"), literal("ADHD"), noun("demencia"), literal("Alzheimer"),
        noun("ansiedad"), noun("depresion"),
        adjective("diagnostico"), adjective("clinico"), adjective("terapeutico"),
        noun("tratamiento"), noun("cura"), noun("sintoma"),
        noun("trastorno"), literal("deterioro cognitivo"), literal("salud mental"),
    )

    private val GAMBLING_TERMS: List<Term> = listOf(
        noun("apuesta"), literal("apostar"), literal("fichas"), noun("bote"), noun("casino"),
        // "odds" is already plural and English: singularising it yields the ordinary English word
        // "odd", measured as a false positive on `SeparatorItemFactoryTest` ("a handful of odd items").
        literal("odds"),
        // See the KDoc note above: "cuotas" is this repo's word for a condition/sampling quota.
        literal("cuota"),
        literal("bankroll"),
        // English, and "low-stakes" is honest KDoc prose in `EmotionCalibrationPrompts` (measured).
        literal("stake"),
        // English too, but for the ordinary reason and not a polysemy exception: neither "bet" nor
        // "bets" occurs anywhere in the scanned tree. `MB-EMO-0031` counted it among the polysemic
        // words; re-measured, it is not one.
        literal("bet"),
    )

    private val MISINFORMATION_TERMS: List<Term> = listOf(
        literal("fake news"), literal("noticias falsas"), noun("desinformacion"), literal("bulos"),
        literal("fuentes fiables"), literal("medios fiables"), literal("detectar mentiras"),
        literal("verificar hechos del mundo real"),
    )

    private val PSEUDOSCIENCE_TERMS: List<Term> = listOf(
        literal("reconfigura tu cerebro"), literal("neuroplasticidad garantizada"),
        literal("potencia tu cerebro"), literal("desbloquea tu mente"),
        literal("entrena tu cerebro como un musculo"),
    )

    /** Far-transfer / real-life claims (Regla #4). */
    val FAR_TRANSFER: List<String> = FAR_TRANSFER_TERMS.flatMap { it.forms() }

    /** Health / clinical claims. */
    val CLINICAL: List<String> = CLINICAL_TERMS.flatMap { it.forms() }

    /** Gambling lexicon (Decisiones must be renamed away from this). */
    val GAMBLING: List<String> = GAMBLING_TERMS.flatMap { it.forms() }

    /** Misinformation lexicon (Lenguaje E6 — regulatory). */
    val MISINFORMATION: List<String> = MISINFORMATION_TERMS.flatMap { it.forms() }

    /** Pseudoscience / outcome promises. */
    val PSEUDOSCIENCE: List<String> = PSEUDOSCIENCE_TERMS.flatMap { it.forms() }

    /** Every forbidden FORM across all families — each lemma plus the inflections it declares. */
    val ALL: List<String> =
        FAR_TRANSFER + CLINICAL + GAMBLING + MISINFORMATION + PSEUDOSCIENCE

    /**
     * English claim vocabulary, applied to PRODUCTION STRING LITERALS ONLY — never to whole files,
     * and deliberately kept OUT of [ALL] (`ShippedEnglishCopyGateTest` pins that separation).
     *
     * The scope is not a compromise, it is what the measurement produced. Across the same 934 files
     * the Spanish gate walks, a wide English clinical/claim vocabulary hits **159 times in KDoc and
     * 21 times inside test guard lists, against exactly ONE production string literal** — the
     * creativity export disclaimer. Whole-file English matching would therefore break 180 honest
     * sites to catch 1 real one, and it would force the fragment-splitting discipline ("i" + "q")
     * across dozens of test files: protection by vigilance, which is what invariant #c exists to
     * avoid. Scanning literals in `commonMain` instead measured 1 hit on 3976 literals — the defect,
     * and no false positive.
     *
     * The asymmetry with Spanish is deliberate and also measured. A Spanish lexeme in a comment is
     * evidence that someone is reasoning about shipping that claim — `MB-EMO-0023` traced three
     * shipped disclaimers back to a single KDoc line that granted itself an exception — and it costs
     * nothing today, because Spanish claim words do not otherwise occur in this codebase's prose.
     * English words do: they are the language the code documents itself in.
     *
     * Terms already carried by [ALL] are not repeated here. Excluded on purpose, and why:
     *  - "diagnostics" — the plural is this codebase's own identifier for measurement-quality
     *    structures (`GoNoGoQualityDiagnostics` and 50 further code occurrences). Same polysemy rule
     *    that keeps "cuota" literal rather than inflected.
     *  - bare "test", "graded", "score" — permitted product vocabulary, not claims.
     */
    val ENGLISH_CLAIMS: List<String> = listOf(
        // clinical — mirrors the Spanish CLINICAL family, minus what ALL already carries
        "clinical", "clinically", "diagnosis", "diagnoses", "diagnose", "diagnosed", "diagnostic",
        "therapy", "therapeutic", "disorder", "disorders", "symptom", "symptoms", "disease",
        "dementia", "anxiety", "depression", "treatment", "treatments", "cure",
        "mental health", "cognitive decline", "cognitive impairment",
        // far-transfer claims
        "intelligence", "smarter", "far transfer", "broad transfer", "far-transfer",
        "broad-transfer", "emotional regulation", "emotional intelligence",
        // gambling — "casino" is omitted because [ALL] already carries it whole-file (it is spelled
        // the same in both languages), and the two lists are pinned disjoint.
        "gambling", "gamble", "wager", "betting", "jackpot",
        // misinformation / pseudoscience
        "misinformation", "neuroplasticity", "rewire your brain",
    )

    /**
     * Claim-ladder vocabulary for the Enfoque engine, applied to PRODUCTION STRING LITERALS ONLY and
     * kept disjoint from [ALL] and [ENGLISH_CLAIMS] (`FocusClaimLevelGateTest` pins all three apart).
     *
     * The product sits at C0 — practice, session and operational progress — and may not assert that a
     * capacity was measured, improved, or transferred. Two of these families are ordinary claim
     * hygiene; the first is a statement of fact that the code contradicts:
     *
     *  (a) The "untrained probe" does not exist. [ProbeBank] reserves disjoint SEEDS, and its own
     *      KDoc is right about that — but its three probes run PVT, FLANKER and GO_NO_GO, and all
     *      three paradigms are in the training catalogue (E1/E12, E2, E3 respectively). A fresh
     *      seed of a trained task is a PARALLEL FORM, which is the most practised thing a user can be
     *      handed, not an untrained instrument. Copy that called it untrained was false, not merely
     *      strong, and it shipped on nine literals across seven files.
     *  (b) Reliability and transfer: no test-retest reliability, no reliable-change threshold derived
     *      from observed variance, and no external instrument exist yet, so "cambio fiable",
     *      "transferencia", "mejora real/general" have nothing behind them.
     *  (c) Measurement of the person: the verb plus a possessive capacity ("Mide tu resistencia",
     *      "Entrena tu capacidad") is the C1 claim in its shortest form.
     *
     * Scope, and why it is literals and not whole files: these are ordinary Spanish words that the
     * engine's own KDoc uses constantly to REASON about the defect — this very paragraph names five
     * of them. Whole-file matching would forbid documenting the problem. Measured over the 118
     * production files of `features/mindboost/focus`, matching literals alone caught 36 shipped
     * claims and no false positive.
     *
     * Deliberately NOT here, measured: "medida"/"medidas" (the honest denials "no una medida de
     * capacidad" and the withheld-sample clause both use the noun, and the noun asserts nothing on
     * its own), and bare "prueba"/"progreso"/"entrenamiento" (permitted C0 vocabulary).
     */
    val EFFICACY_CLAIMS_C0: List<String> = listOf(
        // (a) an instrument the product does not have
        "no entrenada", "no entrenadas", "no se entrena",
        // (b) reliability / efficacy / transfer above C0
        "cambio fiable", "cambios reales", "mejora real", "mejora general", "transferencia",
        // (c) measurement of a capacity asserted about the person
        "mide", "medir", "mediremos", "tu capacidad",
    )

    /** Every lemma with its declared inflection, so the build guards can check the declaration itself. */
    internal val TERMS: List<Term> =
        FAR_TRANSFER_TERMS + CLINICAL_TERMS + GAMBLING_TERMS +
            MISINFORMATION_TERMS + PSEUDOSCIENCE_TERMS

    private fun literal(lemma: String) = Term(lemma, Inflection.LITERAL)

    private fun noun(lemma: String) = Term(lemma, Inflection.NUMBER)

    private fun adjective(lemma: String) = Term(lemma, Inflection.GENDER_NUMBER)

    /**
     * The forms a term contributes. [Inflection.GENDER_NUMBER] assumes a masculine-singular lemma
     * ending in "o"; that precondition is pinned by `CopySafetyLexiconParityTest` rather than asserted
     * here, because a malformed lexicon must fail the BUILD, never crash a session at first access.
     */
    internal fun Term.forms(): List<String> = when (inflection) {
        Inflection.LITERAL -> listOf(lemma)
        Inflection.NUMBER -> listOf(lemma, pluralOf(lemma))
        Inflection.GENDER_NUMBER -> {
            val stem = lemma.dropLast(1)
            listOf(lemma, stem + "a", lemma + "s", stem + "as")
        }
    }

    /** Spanish plural: vowel-final takes "s", consonant-final takes "es" ("depresion" -> "depresiones"). */
    private fun pluralOf(lemma: String): String =
        if (lemma.last() in VOWELS) lemma + "s" else lemma + "es"

    private const val VOWELS = "aeiou"
}
