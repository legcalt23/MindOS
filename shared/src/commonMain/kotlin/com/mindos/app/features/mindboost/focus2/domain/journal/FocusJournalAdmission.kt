package com.mindos.app.features.mindboost.focus2.domain.journal

import com.mindos.app.features.mindboost.core.session.purpose.ProfileEligibilityDisposition
import com.mindos.app.features.mindboost.core.session.purpose.ProfileMeasurementEligibility
import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose
import com.mindos.app.features.mindboost.focus2.data.consent.FocusConsentStatus
import com.mindos.app.features.mindboost.focus2.data.consent.FocusDataScope
import com.mindos.app.features.mindboost.focus2.data.consent.ScopedConsent

/**
 * Focus V5 · S1 · ADR-FOCUS-DATA (TIERED) — the admission gate every per-trial journal write must pass.
 *
 * The ADR made the journal conditional on three things at once: the session's purpose must be measurement,
 * the user must have consented to that purpose, and the volume must stay under a declared cap. This object is
 * where all three are decided, and it is built in S1 — BEFORE the journal writer exists in S8 — because a cap
 * added after the writer means shipping an uncapped writer first.
 *
 * It is a PURE function of explicit facts. Nothing here reads a store, a clock, or a flag: the caller supplies
 * the counts, so there is no path where the gate silently evaluates against data it failed to load.
 */
object FocusJournalAdmission {

    /**
     * Journaled sessions allowed per subject per calendar day.
     *
     * A dose cap, not a storage one: it bounds how much per-trial data a single day can produce even if the
     * user runs sessions continuously. Deliberately close to a realistic heavy day rather than generous —
     * an honest user never reaches it, and the value of data collected past it is negligible next to its cost.
     */
    const val MAX_JOURNALED_SESSIONS_PER_DAY: Int = 8

    /**
     * Total journal entries retained per subject. The storage DoS bound: without it, local encrypted storage
     * grows without limit on the user's own device, which is a real cost they never agreed to pay.
     *
     * A QUANTITY bound, not an age bound: below the ceiling an entry lives indefinitely. Retention by age is
     * declared missing — see the gap recorded against ADR-FOCUS-DATA; it blocks S8, not S1.
     */
    const val MAX_JOURNAL_ENTRIES_PER_SUBJECT: Int = 500

    /** The scope this gate decides. Stated once so the binding below cannot drift from the KDoc. */
    val GOVERNED_SCOPE: FocusDataScope = FocusDataScope.TRIAL_JOURNAL

    /**
     * Decides whether ONE session may be journaled.
     *
     * Every refusal is a VALUE. Nothing here throws — not even on a count that makes no sense, because these
     * counts come from storage, and corrupt storage is a thing to deny on, not a thing to crash on.
     *
     * A count of [JournalEntryCount.Unavailable] is NOT treated as zero: a gate that cannot count is a gate
     * that cannot enforce, so it denies. The alternative — assuming zero when the read failed — would lift the
     * cap precisely when storage is unhealthy.
     */
    @Suppress("ReturnCount") // Each gate returns a DIFFERENT payload, so the ordered-table pattern used by
    // ScoreStateMachine (which returns one shape) does not apply; early returns keep every refusal attributable.
    fun admit(
        purpose: SessionMeasurementPurpose,
        lineageComplete: Boolean,
        consent: ScopedConsent,
        entriesForSubject: JournalEntryCount,
        sessionsJournaledToday: JournalEntryCount,
    ): JournalAdmission {
        val disposition = ProfileMeasurementEligibility.disposition(purpose, lineageComplete)
        if (disposition !is ProfileEligibilityDisposition.Eligible) {
            return JournalAdmission.NotAMeasurementSession(disposition)
        }
        if (consent.scope != GOVERNED_SCOPE) return JournalAdmission.WrongConsentScope(consent.scope)
        if (consent.status != FocusConsentStatus.GRANTED) return JournalAdmission.NoConsent(consent.status)

        val today = sessionsJournaledToday.valueOrNull()
            ?: return JournalAdmission.Indeterminate(IndeterminateCount.DAILY_DOSE)
        if (today >= MAX_JOURNALED_SESSIONS_PER_DAY) {
            return JournalAdmission.OverDailyDose(limit = MAX_JOURNALED_SESSIONS_PER_DAY, observed = today)
        }

        val entries = entriesForSubject.valueOrNull()
            ?: return JournalAdmission.Indeterminate(IndeterminateCount.SUBJECT_ENTRIES)
        if (entries >= MAX_JOURNAL_ENTRIES_PER_SUBJECT) {
            return JournalAdmission.OverStorageCap(limit = MAX_JOURNAL_ENTRIES_PER_SUBJECT, observed = entries)
        }
        return JournalAdmission.Admitted
    }

    /** A negative count is corrupt, and corrupt is a kind of unknown — never a kind of zero. */
    private fun JournalEntryCount.valueOrNull(): Int? = when (this) {
        is JournalEntryCount.Counted -> value.takeIf { it >= 0 }
        JournalEntryCount.Unavailable -> null
    }
}

/**
 * How many of something there are, or the honest admission that we do not know.
 *
 * A nullable `Int?` would carry the same information and invite `?: 0` at the first call-site that finds the
 * null inconvenient — which is exactly how a cap stops being a cap. There is no arithmetic to perform on
 * [Unavailable], and this type makes that a compile-time fact rather than a review comment.
 */
sealed interface JournalEntryCount {
    data class Counted(val value: Int) : JournalEntryCount
    data object Unavailable : JournalEntryCount
}

/** Which count could not be determined. Names the failure instead of hiding it behind a generic denial. */
enum class IndeterminateCount {
    SUBJECT_ENTRIES,
    DAILY_DOSE,
}

/**
 * The typed outcome of the admission gate. A refusal is a VALUE, never a thrown exception and never a silently
 * skipped write: the caller must decide what to do, and "we did not journal this session" is a fact the session
 * record should be able to state.
 *
 * Every non-[Admitted] outcome degrades to the ADR's option (i) — summaries only. That degradation is the
 * designed fallback, not a failure: the session still runs, still scores, still counts as practice.
 */
sealed interface JournalAdmission {

    /** All conditions hold. The journal may be written. */
    data object Admitted : JournalAdmission

    /** The session is not a sealed measurement run, so it was never eligible for a journal in the first place. */
    data class NotAMeasurementSession(val disposition: ProfileEligibilityDisposition) : JournalAdmission

    /**
     * The consent presented governs a DIFFERENT scope, so it says nothing about journaling. A wiring bug, and
     * one that must deny rather than be read as permission for a purpose the user was never asked about.
     */
    data class WrongConsentScope(val presented: FocusDataScope) : JournalAdmission

    /** The user has not granted the trial-journal purpose. Summaries only. */
    data class NoConsent(val status: FocusConsentStatus) : JournalAdmission

    /** The subject already journaled the maximum number of sessions allowed today. */
    data class OverDailyDose(val limit: Int, val observed: Int) : JournalAdmission

    /** The subject's journal has reached its retained-entry ceiling. */
    data class OverStorageCap(val limit: Int, val observed: Int) : JournalAdmission

    /** A count needed to enforce a cap could not be read. Denies: an unenforceable cap is not a cap. */
    data class Indeterminate(val missing: IndeterminateCount) : JournalAdmission
}
