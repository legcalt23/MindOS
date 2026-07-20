package com.mindos.app.features.mindboost.focus2.data.consent

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * Focus V5 · S1 · ADR-FOCUS-DATA — per-purpose consent, fail-closed.
 *
 * The ADR opened exactly one new surface of personal data (the per-trial journal in measurement sessions), so
 * the gate for it is built here, BEFORE anything can write. Every default in these tests is the denying one.
 */
class FocusDataConsentTest {

    private fun store() = SecureFocusConsentStore(InMemorySecureKeyValueStore())

    private suspend fun SecureFocusConsentStore.statusOf(userId: String, scope: FocusDataScope) =
        status(userId, scope).status

    @Test
    fun consent_is_denied_until_explicitly_granted() = runTest {
        val consent = store()
        FocusDataScope.entries.forEach { scope ->
            assertEquals(
                FocusConsentStatus.UNKNOWN,
                consent.statusOf("u1", scope),
                "$scope must deny before it has ever been asked — silence is not agreement.",
            )
        }
    }

    @Test
    fun the_status_is_bound_to_the_scope_it_was_read_for() = runTest {
        // So a gate can refuse a status fetched for some OTHER purpose instead of reading it as permission.
        val consent = store()
        assertEquals(
            FocusDataScope.TRIAL_JOURNAL,
            consent.status("u1", FocusDataScope.TRIAL_JOURNAL).scope,
        )
    }

    @Test
    fun a_granted_scope_survives_a_fresh_store_over_the_same_backing_storage() = runTest {
        val backing = InMemorySecureKeyValueStore()
        SecureFocusConsentStore(backing).grant("u1", FocusDataScope.TRIAL_JOURNAL, nowMillis = 1_000L)

        assertEquals(
            FocusConsentStatus.GRANTED,
            SecureFocusConsentStore(backing).statusOf("u1", FocusDataScope.TRIAL_JOURNAL),
            "Consent must be durable, not an in-memory flag that resets on relaunch.",
        )
    }

    @Test
    fun revoking_denies_and_leaves_a_tombstone_instead_of_deleting_the_record() = runTest {
        val consent = store()
        consent.grant("u1", FocusDataScope.TRIAL_JOURNAL, nowMillis = 1_000L)
        consent.revoke("u1", FocusDataScope.TRIAL_JOURNAL, nowMillis = 2_000L)

        assertEquals(FocusConsentStatus.REVOKED, consent.statusOf("u1", FocusDataScope.TRIAL_JOURNAL))
        assertNotEquals(
            FocusConsentStatus.UNKNOWN,
            consent.statusOf("u1", FocusDataScope.TRIAL_JOURNAL),
            "A deleted record would read as UNKNOWN — indistinguishable from never having asked. " +
                "'The user said no' is exactly the fact that must not be lost.",
        )
    }

    @Test
    fun a_read_failure_reports_UNAVAILABLE_and_never_downgrades_a_refusal_into_never_asked() = runTest {
        // The defect this pins: `readVersioned` maps a backend failure to null AND quarantines the entry, so a
        // REVOKED record would read UNKNOWN — permanently — and the app would re-ask someone who said no.
        val backing = InMemorySecureKeyValueStore()
        val consent = SecureFocusConsentStore(backing)
        consent.revoke("u1", FocusDataScope.TRIAL_JOURNAL, nowMillis = 2_000L)
        val key = "focus2_consent:u1:${FocusDataScope.TRIAL_JOURNAL.name}"

        backing.simulateReadFailure(key, SecureReadResult.KeyInvalidated)
        assertEquals(
            FocusConsentStatus.UNAVAILABLE,
            consent.statusOf("u1", FocusDataScope.TRIAL_JOURNAL),
            "An unreadable decision is its own answer — not UNKNOWN, and never GRANTED.",
        )

        // And the record is still THERE: a transient failure must not have destroyed the user's answer.
        assertNotEquals(
            SecureReadResult.Missing,
            backing.read(key),
            "Reading consent must never quarantine it: quarantine deletes the tombstone it is meant to protect.",
        )
    }

    @Test
    fun a_record_written_by_a_newer_build_is_unreadable_rather_than_absent() = runTest {
        // ADR-FOCUS-MIGRATION's N-1 reader window: a downgraded build must not mistake tomorrow's decision for
        // silence. Guessing "never asked" here would re-prompt a user whose newer record may say REVOKED.
        val backing = InMemorySecureKeyValueStore()
        backing.put(
            "focus2_consent:u1:${FocusDataScope.TRIAL_JOURNAL.name}",
            """{"schemaVersion":99,"userId":"u1","scope":"TRIAL_JOURNAL","granted":false,"policyVersion":1}""",
        )
        assertEquals(
            FocusConsentStatus.UNAVAILABLE,
            SecureFocusConsentStore(backing).statusOf("u1", FocusDataScope.TRIAL_JOURNAL),
        )
    }

    @Test
    fun a_corrupt_payload_is_unreadable_rather_than_absent() = runTest {
        val backing = InMemorySecureKeyValueStore()
        backing.put("focus2_consent:u1:${FocusDataScope.TRIAL_JOURNAL.name}", "{not json at all")
        assertEquals(
            FocusConsentStatus.UNAVAILABLE,
            SecureFocusConsentStore(backing).statusOf("u1", FocusDataScope.TRIAL_JOURNAL),
        )
    }

    @Test
    fun a_scope_can_be_re_granted_after_revocation() = runTest {
        val consent = store()
        consent.grant("u1", FocusDataScope.TRIAL_JOURNAL, nowMillis = 1_000L)
        consent.revoke("u1", FocusDataScope.TRIAL_JOURNAL, nowMillis = 2_000L)
        consent.grant("u1", FocusDataScope.TRIAL_JOURNAL, nowMillis = 3_000L)

        assertEquals(FocusConsentStatus.GRANTED, consent.statusOf("u1", FocusDataScope.TRIAL_JOURNAL))
    }

    @Test
    fun users_are_independent() = runTest {
        val consent = store()
        consent.grant("u1", FocusDataScope.TRIAL_JOURNAL, nowMillis = 1_000L)
        assertEquals(FocusConsentStatus.UNKNOWN, consent.statusOf("u2", FocusDataScope.TRIAL_JOURNAL))
    }

    @Test
    fun a_superseded_disclosure_forces_a_re_ask_for_that_scope_only() = runTest {
        // The improvement over the single-flag AICoachConsent model: the policy version is evaluated PER SCOPE,
        // so bumping the disclosure text of one purpose cannot silently revoke the others.
        val granted = FocusConsentRecord(
            userId = "u1",
            scope = FocusDataScope.TRIAL_JOURNAL,
            granted = true,
            grantedAtMillis = 1_000L,
            policyVersion = 1,
        )
        assertEquals(FocusConsentStatus.GRANTED, granted.toStatus(currentPolicyVersion = 1))
        assertEquals(
            FocusConsentStatus.UNKNOWN,
            granted.toStatus(currentPolicyVersion = 2),
            "A grant given under an older disclosure is not a grant under the new one.",
        )
    }

    @Test
    fun a_grant_we_cannot_date_is_not_a_grant() = runTest {
        // GDPR needs proof of WHEN permission was obtained; "granted, timestamp unknown" is not defensible.
        val undated = FocusConsentRecord(
            userId = "u1",
            scope = FocusDataScope.TRIAL_JOURNAL,
            granted = true,
            grantedAtMillis = null,
            policyVersion = 1,
        )
        assertEquals(FocusConsentStatus.UNKNOWN, undated.toStatus(currentPolicyVersion = 1))
        assertEquals(
            FocusConsentStatus.GRANTED,
            undated.copy(grantedAtMillis = 1L).toStatus(currentPolicyVersion = 1),
            "Discriminating half: the SAME record with a date must still be a grant.",
        )
    }

    @Test
    fun a_simultaneous_grant_and_revoke_resolves_to_the_option_that_collects_less() = runTest {
        val record = FocusConsentRecord(
            userId = "u1",
            scope = FocusDataScope.TRIAL_JOURNAL,
            granted = true,
            grantedAtMillis = 5_000L,
            revokedAtMillis = 5_000L,
            policyVersion = 1,
        )
        assertEquals(
            FocusConsentStatus.REVOKED,
            record.toStatus(currentPolicyVersion = 1),
            "When the ordering is genuinely ambiguous, denial wins.",
        )
    }

    @Test
    fun every_scope_declares_a_policy_version_and_an_undeclared_one_could_never_match() {
        FocusDataScope.entries.forEach { scope ->
            assertNotEquals(
                FocusConsentPolicy.UNDECLARED_POLICY_VERSION,
                FocusConsentPolicy.currentVersion(scope),
                "$scope must declare a real disclosure version.",
            )
        }
        // The fallback fails CLOSED: no realistically stored policyVersion can equal Int.MAX_VALUE, so an
        // undeclared scope reads UNKNOWN and denies rather than matching a default of 0 and permitting.
        val record = FocusConsentRecord(
            userId = "u1",
            scope = FocusDataScope.TRIAL_JOURNAL,
            granted = true,
            grantedAtMillis = 1L,
            policyVersion = 1,
        )
        assertEquals(
            FocusConsentStatus.UNKNOWN,
            record.toStatus(FocusConsentPolicy.UNDECLARED_POLICY_VERSION),
        )
    }

    @Test
    fun a_user_id_that_could_corrupt_the_key_namespace_is_rejected() = runTest {
        val consent = store()
        // ':' is the key separator: accepting it would let one user's id address another user's record.
        assertFailsWith<IllegalArgumentException> {
            consent.grant("u1:evil", FocusDataScope.TRIAL_JOURNAL, nowMillis = 1L)
        }
        assertFailsWith<IllegalArgumentException> { consent.status("", FocusDataScope.TRIAL_JOURNAL) }
    }
}
