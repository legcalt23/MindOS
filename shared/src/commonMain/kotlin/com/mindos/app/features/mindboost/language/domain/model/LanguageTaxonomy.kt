package com.mindos.app.features.mindboost.language.domain.model

import kotlinx.serialization.Serializable

/**
 * Backbone taxonomy of the MindBoost · Lenguaje (language) training system.
 *
 * Pure `commonMain`, no platform / Compose / Firebase dependency, so it compiles
 * everywhere and is exhaustively unit-testable. Every other layer (scoring, repair,
 * adaptation, evidence, claims, content) references these closed sets — adding or
 * removing a value is a deliberate, test-locked decision (see [LanguageTaxonomyTest]).
 *
 * Source of truth: `MindBoost_Lenguaje_v6_NoCompromise_WorldClass.md`
 * (§4.2 traits, §6.6 error taxonomy, §8.3 banks, §5.2 evidence — the latter lives
 * in [EvidenceState]).
 */

/**
 * First-class language traits (v6 §4.2). [code] is the stable spec identifier used in
 * content passports and analytics; [display] is a short claim-safe Spanish label.
 *
 * The taxonomy is a *graph* of operations, not a single ability bar (§4.1): the graph
 * edges (prerequisites/precursors) live in a later `ConstructGraph` slice, not here.
 */
@Serializable
enum class TraitCode(val code: String, val display: String) {
    LEX_A("LEX-A", "Acceso léxico automático"),
    LEX_D("LEX-D", "Profundidad léxica"),
    LEX_P("LEX-P", "Precisión semántica"),
    SYN("SYN", "Procesamiento sintáctico"),
    LIT("LIT", "Comprensión literal"),
    INF("INF", "Inferencia legítima"),
    SIT("SIT", "Modelo de situación"),
    WM_V("WM-V", "Memoria verbal funcional"),
    SUM("SUM", "Síntesis"),
    PAR("PAR", "Parafraseo"),
    ARG("ARG", "Razonamiento argumental"),
    CRIT("CRIT", "Lectura crítica"),
    PRAG("PRAG", "Adaptación pragmática"),
    PROD_W("PROD-W", "Producción escrita"),
    PROD_O("PROD-O", "Producción oral"),
    MED("MED", "Mediación lingüística"),
    EDIT("EDIT", "Edición iterativa"),
    META("META", "Metacognición"),
    TRANS("TRANS", "Transferencia funcional"),
}

/**
 * Cognitive operation a task exercises (v6 §4.5 / task passports). This is *what the
 * user does*, decoupled from how the response is captured ([ResponseMode]) and how it
 * is scored ([ScoringRule]).
 */
@Serializable
enum class OperationType {
    IDENTIFY_NUANCE,
    EVIDENCE_SUPPORTED_INFERENCE,
    INFER,
    LITERAL_EXTRACTION,
    SYNTACTIC_MAPPING,
    SUMMARIZE,
    PARAPHRASE,
    ARGUE,
    CRITIQUE_SOURCE,
    PRAGMATIC_REFRAME,
    EDIT,
    MEDIATE,
    CALIBRATE_CONFIDENCE,
}

/**
 * How a response is captured. The closed-scoring spine (Fase 1) handles the first three;
 * the open ones route to the optional Evaluator OS (later slice) and never block the
 * offline core.
 */
@Serializable
enum class ResponseMode(val isClosed: Boolean) {
    MULTIPLE_CHOICE(isClosed = true),
    MULTIPLE_CHOICE_WITH_EVIDENCE_SELECTION(isClosed = true),
    ORDERING(isClosed = true),
    SHORT_TEXT(isClosed = false),
    LONG_TEXT(isClosed = false),
    AUDIO(isClosed = false),
}

/**
 * Separated item banks (v6 §8.3). Bank separation is a first-class evidence gate: an item
 * trained on cannot, by itself, produce strong transfer evidence.
 */
@Serializable
enum class BankType {
    TRAINING,
    REPAIR,
    CALIBRATION,
    EVALUATION,
    PARALLEL_FORM,
    TRANSFER_LAB,
    RETENTION,
    GOLD_SET,
    RED_TEAM,
}

/**
 * Error taxonomy (v6 §6.6). Each recurring error names a reparable pattern and points at
 * the construct(s) it degrades; the RepairProtocol (later slice) maps these to plans.
 */
@Serializable
enum class ErrorType(val code: String, val primaryTraits: List<TraitCode>) {
    INF_OVER("INF_OVER", listOf(TraitCode.INF, TraitCode.SIT, TraitCode.META)),
    INF_UNDER("INF_UNDER", listOf(TraitCode.INF, TraitCode.SIT)),
    LIT_DIST("LIT_DIST", listOf(TraitCode.LIT)),
    SYN_ROLE("SYN_ROLE", listOf(TraitCode.SYN, TraitCode.LIT)),
    SYN_SCOPE("SYN_SCOPE", listOf(TraitCode.SYN)),
    SUM_NO_THESIS("SUM_NO_THESIS", listOf(TraitCode.SUM)),
    SUM_DISTORT("SUM_DISTORT", listOf(TraitCode.SUM, TraitCode.PAR)),
    ARG_GAP("ARG_GAP", listOf(TraitCode.ARG)),
    ARG_STRAW("ARG_STRAW", listOf(TraitCode.ARG, TraitCode.CRIT)),
    CRIT_SOURCE("CRIT_SOURCE", listOf(TraitCode.CRIT)),
    PRAG_TONE("PRAG_TONE", listOf(TraitCode.PRAG)),
    PROD_VAGUE("PROD_VAGUE", listOf(TraitCode.PROD_W, TraitCode.LEX_P)),
    PROD_BLOAT("PROD_BLOAT", listOf(TraitCode.PROD_W, TraitCode.SUM)),
    META_MIS("META_MIS", listOf(TraitCode.META)),
}

/**
 * Privacy classification of a piece of data (v6 §13 privacy-by-flow). Drives retention,
 * redaction and whether content may ever leave the device. Free user text is never
 * treated as [NON_SENSITIVE_ORIGINAL_CONTENT].
 */
@Serializable
enum class PrivacyClass(val isUserContent: Boolean) {
    NON_SENSITIVE_ORIGINAL_CONTENT(isUserContent = false),
    MINIMAL_TELEMETRY(isUserContent = false),
    EVIDENCE_ABSTRACTION(isUserContent = false),
    USER_GENERATED_TEXT(isUserContent = true),
    USER_DOCUMENT(isUserContent = true),
    USER_AUDIO(isUserContent = true),
    SENSITIVE_DETECTED(isUserContent = true),
}

/** Content lifecycle / QA status of an item (v6 §8.1 pipeline, §8.4 quality bands). */
@Serializable
enum class QaStatus(val isReleasable: Boolean) {
    DRAFT(isReleasable = false),
    LAB(isReleasable = false),
    PILOT(isReleasable = false),
    PRODUCTION(isReleasable = true),
    BENCHMARK_CANDIDATE(isReleasable = false),
    RETIRED(isReleasable = false),
}

/**
 * Closed-form scoring rule declared by an item. Only the deterministic rules are honored
 * by the offline scoring spine; rubric rules require the optional Evaluator OS.
 */
@Serializable
enum class ScoringRule(val isDeterministic: Boolean) {
    DETERMINISTIC_KEY(isDeterministic = true),
    DETERMINISTIC_KEY_PLUS_EVIDENCE(isDeterministic = true),
    ORDERING_EXACT(isDeterministic = true),
    ORDERING_RANK_CORRELATION(isDeterministic = true),
    RUBRIC_LOCAL(isDeterministic = false),
    RUBRIC_HYBRID(isDeterministic = false),
    RUBRIC_REMOTE(isDeterministic = false),
}

/** Coarse dependency level used in stimulus metadata (prior-knowledge / cultural). */
@Serializable
enum class DependencyLevel { LOW, MEDIUM, HIGH }

/** Provenance of a task stimulus. User-provided material carries stricter privacy. */
@Serializable
enum class StimulusSource(val privacyClass: PrivacyClass) {
    ORIGINAL(PrivacyClass.NON_SENSITIVE_ORIGINAL_CONTENT),
    LICENSED(PrivacyClass.NON_SENSITIVE_ORIGINAL_CONTENT),
    USER_PROVIDED(PrivacyClass.USER_DOCUMENT),
}
