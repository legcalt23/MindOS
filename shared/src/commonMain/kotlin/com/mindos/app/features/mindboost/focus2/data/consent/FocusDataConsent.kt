package com.mindos.app.features.mindboost.focus2.data.consent

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Focus V5 · S1 · ADR-FOCUS-DATA (decided 2026-07-20, Decision Log §20.2) — consent BY PURPOSE for Focus v2.
 *
 * The ADR chose the TIERED option: session summaries by default, plus a per-trial journal ONLY in sessions whose
 * purpose is measurement. That journal is a new surface of personal data, so it gets its own control rather than
 * riding on a blanket agreement (PRI-001/002: separate purposes, separate controls).
 *
 * This is modelled on the existing `AICoachConsent` record, with one deliberate difference: that mechanism has a
 * SINGLE consent flag and a single `policyVersion`, so bumping the disclosure text for one purpose would revoke
 * every purpose at once. Here the record is per-scope and so is the policy version.
 */
enum class FocusDataScope {
    /**
     * Per-trial records retained for measurement sessions, enabling internal-coherence auditing and re-scoring.
     *
     * Honesty requirement carried from the ADR: a LOCAL journal is not anti-tamper evidence — it lives on the
     * user's device, unsigned and unsynced. It must never be described as proof of anything.
     *
     * The ONLY scope. An earlier draft also declared a RESEARCH scope; it was removed because nothing consumed
     * it — [FocusJournalAdmission] structurally denies research purposes, so the scope enforced exactly nothing.
     * A control with no call-site is cover, not protection (learning 2026-07-04). The scope for a research path
     * gets added in the same change that creates the path.
     */
    TRIAL_JOURNAL,
}

/**
 * Runtime consent state for one scope.
 *
 * FOUR values, not three, and the fourth is the one that matters: "never asked", "said no" and "we could not
 * find out" are three different facts, and collapsing the last two into either of the others is a lie in one
 * direction or the other. All of them except [GRANTED] deny collection; the distinction is what the app may do
 * NEXT, which is why [UNAVAILABLE] must not be reported as [UNKNOWN].
 */
enum class FocusConsentStatus {
    /** Never asked, or granted under a superseded disclosure. Denies, and re-asking is appropriate. */
    UNKNOWN,

    /** Granted under the CURRENT disclosure for this scope. The only value that permits anything. */
    GRANTED,

    /** Explicitly withdrawn. Denies, and re-asking would be badgering someone who already said no. */
    REVOKED,

    /**
     * The stored decision could not be read: backend failure, corrupt payload, or a record written by a NEWER
     * app version this build cannot parse.
     *
     * Denies, like [UNKNOWN] — but it is NOT [UNKNOWN], because treating an unreadable record as "never asked"
     * silently re-prompts a user who may have refused. The read that produces this deliberately does NOT
     * quarantine the entry: quarantine deletes it, which would turn a transient failure into a permanent
     * forgetting of the user's answer (learning 2026-07-05).
     */
    UNAVAILABLE,
}

/** The persisted consent decision for ONE (user, scope) pair. */
@Serializable
data class FocusConsentRecord(
    val schemaVersion: Int = SCHEMA_VERSION,
    val userId: String,
    val scope: FocusDataScope,
    val granted: Boolean,
    val grantedAtMillis: Long? = null,
    val revokedAtMillis: Long? = null,
    /** Monotonic per-record revision. Not a schema version: it counts grant/revoke cycles. */
    val revision: Int = 1,
    /** The disclosure version THIS scope was granted under. */
    val policyVersion: Int,
) {
    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}

/**
 * The current disclosure version PER SCOPE. Bumping one invalidates prior grants for that scope only — they
 * become [FocusConsentStatus.UNKNOWN], forcing a re-ask without deleting the history of what was agreed when.
 */
object FocusConsentPolicy {

    private val currentVersions: Map<FocusDataScope, Int> = mapOf(
        FocusDataScope.TRIAL_JOURNAL to 1,
    )

    /**
     * A scope with no declared version is a programming error, and it fails CLOSED at a very high version: an
     * absent policy can never match a stored `policyVersion`, so the scope reads UNKNOWN and denies rather than
     * silently matching version 0 and permitting collection.
     */
    fun currentVersion(scope: FocusDataScope): Int = currentVersions[scope] ?: UNDECLARED_POLICY_VERSION

    internal const val UNDECLARED_POLICY_VERSION: Int = Int.MAX_VALUE
}

/**
 * Maps a stored record to its runtime status, fail-closed.
 *
 * A missing record, a record for a superseded disclosure, and a revoked record are all non-permitting. The
 * revocation comparison uses `>=` so that a grant and a revoke landing in the SAME millisecond resolve to
 * REVOKED — when the ordering is genuinely ambiguous, the answer that collects less wins.
 *
 * GRANTED additionally requires a `grantedAtMillis`. A grant we cannot date is a grant we cannot evidence, and
 * "we have their permission but cannot say when they gave it" is not a defensible position to be in.
 */
fun FocusConsentRecord?.toStatus(currentPolicyVersion: Int): FocusConsentStatus = when {
    this == null -> FocusConsentStatus.UNKNOWN
    revokedAtMillis != null && (grantedAtMillis == null || revokedAtMillis >= grantedAtMillis) ->
        FocusConsentStatus.REVOKED
    granted && grantedAtMillis != null && policyVersion == currentPolicyVersion -> FocusConsentStatus.GRANTED
    else -> FocusConsentStatus.UNKNOWN
}

/**
 * Consent for one scope, bound to the scope it was read for.
 *
 * The pairing is the point: a bare status is a value any caller can hand to any gate, so a later slice could
 * check the journal gate against a status read for a different purpose and never notice. Carrying the scope
 * makes that mismatch a thing the gate can refuse rather than a thing nobody can see.
 */
data class ScopedConsent(val scope: FocusDataScope, val status: FocusConsentStatus)

/** Per-scope consent persistence. */
interface FocusConsentStore {
    suspend fun status(userId: String, scope: FocusDataScope): ScopedConsent
    suspend fun grant(userId: String, scope: FocusDataScope, nowMillis: Long)
    suspend fun revoke(userId: String, scope: FocusDataScope, nowMillis: Long)
}

/**
 * [FocusConsentStore] over the encrypted [SecureKeyValueStore] (namespace `mindboost_focus2`).
 *
 * Revocation REWRITES the record rather than deleting it: a deleted record is indistinguishable from one that
 * was never asked, and "the user said no" is exactly the fact we must not lose. That guarantee is why this
 * class does its own reading instead of using `readVersioned`: the shared helper QUARANTINES on a corrupt or
 * future-version payload, and quarantine removes the live entry — which would delete precisely the tombstone
 * the paragraph above promises to keep. Here an unreadable record stays on disk and reports
 * [FocusConsentStatus.UNAVAILABLE], so a later version (or a fixed backend) can still recover the answer.
 */
class SecureFocusConsentStore(
    private val store: SecureKeyValueStore,
) : FocusConsentStore {

    override suspend fun status(userId: String, scope: FocusDataScope): ScopedConsent {
        val status = when (val load = load(userId, scope)) {
            is ConsentLoad.Found -> load.record.toStatus(FocusConsentPolicy.currentVersion(scope))
            ConsentLoad.Absent -> FocusConsentStatus.UNKNOWN
            ConsentLoad.Unreadable -> FocusConsentStatus.UNAVAILABLE
        }
        return ScopedConsent(scope, status)
    }

    override suspend fun grant(userId: String, scope: FocusDataScope, nowMillis: Long) {
        val prior = priorRecord(userId, scope)
        write(
            FocusConsentRecord(
                userId = userId,
                scope = scope,
                granted = true,
                grantedAtMillis = nowMillis,
                revokedAtMillis = null,
                revision = (prior?.revision ?: 0) + 1,
                policyVersion = FocusConsentPolicy.currentVersion(scope),
            ),
        )
    }

    override suspend fun revoke(userId: String, scope: FocusDataScope, nowMillis: Long) {
        val prior = priorRecord(userId, scope)
        write(
            FocusConsentRecord(
                userId = userId,
                scope = scope,
                granted = false,
                // Preserved so the record still says WHEN it had been granted; only the verdict changes.
                grantedAtMillis = prior?.grantedAtMillis,
                revokedAtMillis = nowMillis,
                revision = (prior?.revision ?: 0) + 1,
                policyVersion = prior?.policyVersion ?: FocusConsentPolicy.currentVersion(scope),
            ),
        )
    }

    /**
     * The prior record for revision bookkeeping only. An unreadable prior is treated as no prior: a fresh
     * grant or revoke is an explicit act the user just performed, and it stands regardless of what the
     * unreadable bytes said. Only the revision counter loses continuity, which is bookkeeping, not consent.
     */
    private suspend fun priorRecord(userId: String, scope: FocusDataScope): FocusConsentRecord? =
        (load(userId, scope) as? ConsentLoad.Found)?.record

    @Suppress("SwallowedException")
    private suspend fun load(userId: String, scope: FocusDataScope): ConsentLoad =
        when (val result = store.read(keyOf(userId, scope))) {
            SecureReadResult.Missing -> ConsentLoad.Absent
            is SecureReadResult.Failure -> ConsentLoad.Unreadable
            is SecureReadResult.Value -> {
                val record = try {
                    json.decodeFromString(FocusConsentRecord.serializer(), result.value)
                } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                    null
                }
                // A record from a NEWER app version is unreadable, not absent: the N-1 reader window of
                // ADR-FOCUS-MIGRATION means a downgraded build must not mistake tomorrow's "no" for silence.
                when {
                    record == null -> ConsentLoad.Unreadable
                    record.schemaVersion > FocusConsentRecord.SCHEMA_VERSION -> ConsentLoad.Unreadable
                    else -> ConsentLoad.Found(record)
                }
            }
        }

    private suspend fun write(record: FocusConsentRecord) {
        store.put(
            keyOf(record.userId, record.scope),
            json.encodeToString(FocusConsentRecord.serializer(), record),
        )
    }

    private sealed interface ConsentLoad {
        data class Found(val record: FocusConsentRecord) : ConsentLoad
        data object Absent : ConsentLoad
        data object Unreadable : ConsentLoad
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        /** Rejects anything that could corrupt the key namespace, `:` above all (it is the key separator). */
        val USER_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,128}$")

        fun keyOf(userId: String, scope: FocusDataScope): String {
            require(USER_ID_REGEX.matches(userId)) { "userId must match $USER_ID_REGEX" }
            return "focus2_consent:$userId:${scope.name}"
        }
    }
}
