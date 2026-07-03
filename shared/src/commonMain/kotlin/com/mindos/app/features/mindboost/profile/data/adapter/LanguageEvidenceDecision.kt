package com.mindos.app.features.mindboost.profile.data.adapter

import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution

/**
 * Observable, non-PII reason the Lenguaje evidence was refused for the OFFICIAL cognitive profile.
 * Surfaced by [LanguageProfileSource.evaluate] so a caller can log WHY the VERBAL dimension stays
 * CALIBRATING — never fabricates a number, never carries user content.
 *
 * Evaluated in priority order (first match wins): a demo/unvalidated bank is the strongest reason, then
 * stub provenance, then simply-not-enough valid evidence.
 */
enum class LanguageEvidenceBlockReason {
    /** The content bank backing the session is demo/provisional (BankIntegrity.SMALL_UNVALIDATED et al.). */
    SMALL_UNVALIDATED_BANK,

    /** The evaluator/rubric/projector producing the evidence is a local stub, not a validated model. */
    STUB_PROVENANCE,

    /** Bank + provenance are valid, but no trait cleared calibrating and there is no session signal yet. */
    INSUFFICIENT_VALID_EVIDENCE,
}

/**
 * Outcome of the Lenguaje read-side integrity gate. [Blocked] ⇒ `contribution()` returns `null` ⇒ the
 * VERBAL dimension stays CALIBRATING and never reaches an official score, rank, or the Global Score.
 */
sealed interface LanguageEvidenceDecision {
    /** The evidence is eligible; [contribution] may still carry a null score (honest CALIBRATING). */
    data class Eligible(val contribution: ModuleContribution) : LanguageEvidenceDecision

    /** The evidence is refused for the official profile; [reason] is the observable, non-PII cause. */
    data class Blocked(val reason: LanguageEvidenceBlockReason) : LanguageEvidenceDecision
}
