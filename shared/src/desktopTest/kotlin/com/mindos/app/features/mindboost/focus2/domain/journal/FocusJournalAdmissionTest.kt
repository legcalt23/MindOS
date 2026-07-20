package com.mindos.app.features.mindboost.focus2.domain.journal

import com.mindos.app.features.mindboost.core.session.purpose.ProfileEligibilityDisposition
import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose
import com.mindos.app.features.mindboost.focus2.data.consent.FocusConsentStatus
import com.mindos.app.features.mindboost.focus2.data.consent.FocusDataScope
import com.mindos.app.features.mindboost.focus2.data.consent.ScopedConsent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Focus V5 · S1 · ADR-FOCUS-DATA (TIERED) — the journal admission gate.
 *
 * The ADR's whole point is that a journal exists only where all conditions hold at once. Each test below
 * removes exactly ONE condition from an otherwise-admissible call, so every denial is attributable.
 */
class FocusJournalAdmissionTest {

    /** An admissible call. Every test starts from this and breaks one thing. */
    private fun admit(
        purpose: SessionMeasurementPurpose = SessionMeasurementPurpose.MEASUREMENT,
        lineageComplete: Boolean = true,
        consent: FocusConsentStatus = FocusConsentStatus.GRANTED,
        scope: FocusDataScope = FocusDataScope.TRIAL_JOURNAL,
        entriesForSubject: JournalEntryCount = JournalEntryCount.Counted(0),
        sessionsJournaledToday: JournalEntryCount = JournalEntryCount.Counted(0),
    ) = FocusJournalAdmission.admit(
        purpose = purpose,
        lineageComplete = lineageComplete,
        consent = ScopedConsent(scope, consent),
        entriesForSubject = entriesForSubject,
        sessionsJournaledToday = sessionsJournaledToday,
    )

    @Test
    fun a_consented_measurement_session_under_both_caps_is_admitted() {
        // The discriminating baseline: without this passing, every denial below could be denial-by-accident.
        assertEquals(JournalAdmission.Admitted, admit())
    }

    @Test
    fun training_is_never_journaled_however_clean_it_is() {
        // The TIERED contract in one assertion: ordinary training produces summaries, never per-trial data.
        listOf(
            SessionMeasurementPurpose.TRAINING_FREE,
            SessionMeasurementPurpose.TRAINING_RECOMMENDED,
            SessionMeasurementPurpose.PRACTICE,
            SessionMeasurementPurpose.TUTORIAL,
            SessionMeasurementPurpose.FAMILIARIZATION,
        ).forEach { purpose ->
            assertIs<JournalAdmission.NotAMeasurementSession>(
                admit(purpose = purpose),
                "$purpose must not be journaled — TIERED means measurement sessions only.",
            )
        }
    }

    @Test
    fun an_unsealed_purpose_falls_back_to_summaries_instead_of_journaling_by_default() {
        val outcome = admit(purpose = SessionMeasurementPurpose.UNKNOWN)
        val denial = assertIs<JournalAdmission.NotAMeasurementSession>(outcome)
        assertIs<ProfileEligibilityDisposition.Indeterminate>(
            denial.disposition,
            "An indeterminable purpose must degrade to option (i), exactly as the ADR requires.",
        )
    }

    @Test
    fun consent_is_required_and_no_state_but_an_explicit_current_grant_counts_as_agreement() {
        // UNAVAILABLE matters most here: an unreadable decision must deny, never be optimistically permitted.
        listOf(
            FocusConsentStatus.UNKNOWN,
            FocusConsentStatus.REVOKED,
            FocusConsentStatus.UNAVAILABLE,
        ).forEach { status ->
            assertEquals(
                JournalAdmission.NoConsent(status),
                admit(consent = status),
                "$status must deny: only an explicit, current GRANTED permits collection.",
            )
        }
    }

    @Test
    fun the_gate_governs_the_trial_journal_scope_and_says_so() {
        assertEquals(FocusDataScope.TRIAL_JOURNAL, FocusJournalAdmission.GOVERNED_SCOPE)
        // TRIPWIRE: while exactly one scope exists, a mismatched ScopedConsent is unconstructible, so the
        // WrongConsentScope branch cannot be exercised. When a second scope is added, this fails — add a case
        // asserting that a consent read for the OTHER scope is refused rather than read as permission.
        assertEquals(
            1,
            FocusDataScope.entries.size,
            "A new scope exists: add the WrongConsentScope test this tripwire is holding the place for.",
        )
    }

    @Test
    fun the_daily_dose_cap_denies_at_the_limit_not_after_it() {
        val limit = FocusJournalAdmission.MAX_JOURNALED_SESSIONS_PER_DAY
        assertEquals(JournalAdmission.Admitted, admit(sessionsJournaledToday = JournalEntryCount.Counted(limit - 1)))
        assertEquals(
            JournalAdmission.OverDailyDose(limit = limit, observed = limit),
            admit(sessionsJournaledToday = JournalEntryCount.Counted(limit)),
            "The Nth session is the one that must be refused, not the N+1th.",
        )
    }

    @Test
    fun the_storage_cap_denies_at_the_limit_not_after_it() {
        val limit = FocusJournalAdmission.MAX_JOURNAL_ENTRIES_PER_SUBJECT
        assertEquals(JournalAdmission.Admitted, admit(entriesForSubject = JournalEntryCount.Counted(limit - 1)))
        assertEquals(
            JournalAdmission.OverStorageCap(limit = limit, observed = limit),
            admit(entriesForSubject = JournalEntryCount.Counted(limit)),
        )
    }

    @Test
    fun a_count_that_could_not_be_read_denies_instead_of_being_treated_as_zero() {
        // The failure mode this guards: assuming zero on a failed read would LIFT the cap at exactly the moment
        // storage is unhealthy — an unenforceable cap that reports itself as enforced.
        assertEquals(
            JournalAdmission.Indeterminate(IndeterminateCount.SUBJECT_ENTRIES),
            admit(entriesForSubject = JournalEntryCount.Unavailable),
        )
        assertEquals(
            JournalAdmission.Indeterminate(IndeterminateCount.DAILY_DOSE),
            admit(sessionsJournaledToday = JournalEntryCount.Unavailable),
        )
    }

    @Test
    fun a_nonsensical_count_denies_rather_than_throwing() {
        // A corrupt stored count is a thing to refuse on, not to crash on: the contract says every refusal is a
        // VALUE, and a gate that throws takes the caller down instead of degrading to summaries.
        assertEquals(
            JournalAdmission.Indeterminate(IndeterminateCount.SUBJECT_ENTRIES),
            admit(entriesForSubject = JournalEntryCount.Counted(-1)),
        )
        assertEquals(
            JournalAdmission.Indeterminate(IndeterminateCount.DAILY_DOSE),
            admit(sessionsJournaledToday = JournalEntryCount.Counted(-1)),
        )
    }

    @Test
    fun purpose_is_checked_before_consent_so_a_denial_names_the_first_real_reason() {
        // Ordering matters for diagnosis: a training session with no consent is refused because it is training,
        // and reporting "no consent" would send someone hunting for a consent bug that does not exist.
        assertIs<JournalAdmission.NotAMeasurementSession>(
            admit(purpose = SessionMeasurementPurpose.TRAINING_FREE, consent = FocusConsentStatus.UNKNOWN),
        )
    }

    @Test
    fun a_measurement_purpose_without_lineage_is_still_refused() {
        assertIs<JournalAdmission.NotAMeasurementSession>(admit(lineageComplete = false))
    }
}
