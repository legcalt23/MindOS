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
 * and crosses hyphens/negations, so "non-clinical" / "no es un test de inteligencia" are both caught.
 *
 * This file lives under `mindboost/core/copy/` — deliberately OUTSIDE the per-feature dirs the scanner
 * walks (focus/memory/speed/logic/decisiones/language/creativity + AppStrings). That is why it may hold
 * the raw literals without self-tripping the gate (same strategy as the build-task lists for
 * arena/emotions, which live outside the scanned tree). The Phase-3 task MUST exclude this package.
 *
 * Polysemy decisions vs doc 12 §1 (documented, NOT invented — see §1 "Nota de afinado" + §riesgos):
 *  - Polysemic roots are encoded as PHRASES, never bare words, to avoid false positives on legitimate
 *    copy: "deterioro cognitivo" (not bare "cognitivo" — "perfil cognitivo funcional" is permitted),
 *    "salud mental" (not bare "mental"), "rindes mejor en el trabajo" / "mejor en los estudios"
 *    (not bare "trabajo"/"estudios" — "memoria de trabajo" is permitted), "conduccion" / "al volante"
 *    (not bare "conducir" — Spanish "conducir a" = "lead to").
 *  - Deliberately EXCLUDED: bare "CI" (two-letter, high false-positive risk; covered by
 *    "coeficiente intelectual"), and "balance" (semantic/permitted; gambling sense covered by
 *    "bankroll").
 *  - Flagged for Phase-2 review (low value / higher FP risk, kept faithful to doc 12): "bote", "cuota".
 *
 * Known doc-internal conflict surfaced for the user: doc 12 §2 lists "Proxy interno, no diagnostico"
 * as PERMITTED, yet "diagnostico" is forbidden in §1 and invariant #c forbids it even when negated.
 * The invariant wins: "diagnostico" stays forbidden; that permitted phrase must be reformulated in
 * Phase 2 (e.g. "proxy interno — practica funcional, no es una nota").
 */
object ForbiddenLexicon {

    /** Far-transfer / real-life claims (Regla #4). */
    val FAR_TRANSFER: List<String> = listOf(
        "inteligencia", "IQ", "Gf", "coeficiente intelectual", "coeficiente de inteligencia",
        "te hace mas listo", "mejores decisiones", "decides mejor", "en la vida real",
        "multitarea", "productividad", "al volante", "conduccion",
        "regulacion emocional", "inteligencia emocional", "controlas tus emociones",
        "autocontrol", "personalidad", "te distraes menos con el movil",
        "usas menos el telefono", "rindes mejor en el trabajo", "mejor en los estudios",
    )

    /** Health / clinical claims. */
    val CLINICAL: List<String> = listOf(
        "TDAH", "ADHD", "demencia", "Alzheimer", "ansiedad", "depresion",
        "diagnostico", "clinico", "terapeutico", "tratamiento", "cura", "sintoma",
        "trastorno", "deterioro cognitivo", "salud mental",
    )

    /** Gambling lexicon (Decisiones must be renamed away from this). */
    val GAMBLING: List<String> = listOf(
        "apuesta", "apostar", "fichas", "bote", "casino", "odds", "cuota",
        "bankroll", "stake", "bet",
    )

    /** Misinformation lexicon (Lenguaje E6 — regulatory). */
    val MISINFORMATION: List<String> = listOf(
        "fake news", "noticias falsas", "desinformacion", "bulos",
        "fuentes fiables", "medios fiables", "detectar mentiras",
        "verificar hechos del mundo real",
    )

    /** Pseudoscience / outcome promises. */
    val PSEUDOSCIENCE: List<String> = listOf(
        "reconfigura tu cerebro", "neuroplasticidad garantizada", "potencia tu cerebro",
        "desbloquea tu mente", "entrena tu cerebro como un musculo",
    )

    /** Every forbidden term across all families. */
    val ALL: List<String> =
        FAR_TRANSFER + CLINICAL + GAMBLING + MISINFORMATION + PSEUDOSCIENCE
}
