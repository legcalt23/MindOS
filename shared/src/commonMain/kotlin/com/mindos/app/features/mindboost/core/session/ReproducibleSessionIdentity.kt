package com.mindos.app.features.mindboost.core.session

import com.mindos.app.features.mindboost.core.canonical.CanonicalEncoder
import com.mindos.app.features.mindboost.core.canonical.CanonicalizerVersion
import com.mindos.app.features.mindboost.core.canonical.DigestAlgorithmId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** A persisted identity that cannot be trusted as written. Surfaces through the store's existing quarantine path. */
class IncoherentIdentityException(message: String) : IllegalArgumentException(message)

/**
 * Everything needed to rebuild a session's FORM, persisted before its first trial is presented.
 *
 * ## Scope, stated exactly
 *
 * **FORM_REPRODUCTION**, not SESSION_REPLAY — the operative acceptance criterion
 * (`ENG_CORE_AND_FOCUS_BLUEPRINT_V3.md:§7`). It reproduces the potential stimulus sequence a session was built
 * from. It does not — and by [SessionRecord]'s minimization contract must not — carry the trials actually
 * presented, the responses, or their timings, so it cannot reconstruct what the user did. That remains open work,
 * declared rather than implied.
 *
 * ## Why identity and digest are inseparable here
 *
 * The previous version was a `data class` taking `formId` as a constructor argument. That made the digest a
 * *claim a caller supplied* rather than a *property of the data*, and gave three ways to hold an identity whose
 * digest describes something it is not: pass any string to the constructor; `copy(plannedTrialCount = 99)` and
 * keep the old `formId`; or decode a row whose `formId` never matched its fields, since `@Serializable` on a data
 * class just assigns what it reads. Nothing checked. `09_DECISION_LOG.md:390-391` fixes the required shape of
 * enforcement for this codebase — a constraint must be *"impuesto por la AUSENCIA del input, no por un verdict
 * que un consumidor podría ignorar"*. So:
 *
 * - the constructor is private and the digest is **computed**, never accepted ([of]);
 * - this is not a `data class`, so there is no `copy()` to desynchronize the two;
 * - the [descriptor] is the single source of both the identity's fields and its digest — there is no second
 *   hand-written field list that could drift from the first (see [CanonicalEncoder]);
 * - deserialization goes through [ReproducibleSessionIdentitySerializer], which **recomputes** the digest from
 *   the decoded descriptor and refuses a row where the two disagree.
 *
 * ## The two digests are different questions
 *
 * [formDefinitionId] answers *"were the inputs the same?"* — it is a digest of [descriptor] alone. It cannot
 * detect a generator whose output changed without its version being bumped, because the inputs are identical in
 * exactly that case. [generatedPlanDigest] answers *"was the output the same?"* — it is a digest of the stimulus
 * sequence the engine actually produced. Only the second turns "the developer remembered to bump the version"
 * from an assumption into a checked fact.
 *
 * ## Privacy
 *
 * No free text, no device fingerprint, no identifiers. `descriptor.resolvedDifficulty` is the one field that says
 * anything about the user, and it must be described accurately: it is the user's EFFECTIVE per-axis difficulty at
 * that moment — an ability inference vector, and NEW information. An earlier draft of this KDoc claimed it was
 * "already implied by the session's own outcome"; that was asserted, not verified, and a privacy review refuted
 * it — nothing else in [SessionRecord] encodes a per-axis difficulty. It is admitted because a form cannot be
 * reproduced without the difficulty it was resolved at, and a session whose form cannot be rebuilt cannot be
 * audited at all. It lives in the same per-user, account-bound namespace as the record that carries it
 * (`MindBoostStorageNamespaces.Sessions`, in `AccountBound`, covered by `AccountBoundErasureCoverageTest`).
 * Encryption is the platform store's property, not this type's, and is claimed only where it is verified: the
 * Android backend is AES-256-GCM via the Keystore; the desktop backend is in-memory and encrypts nothing.
 */
@Serializable(with = ReproducibleSessionIdentitySerializer::class)
class ReproducibleSessionIdentity private constructor(
    /** Schema of THIS identity. A row from the future fails closed rather than being read under today's rules. */
    val replaySchemaVersion: Int,
    /** The byte encoding [formDefinitionId] was computed under. Frozen per version; never reinterpreted. */
    val canonicalizerVersionId: String,
    /** The digest function [formDefinitionId] and [generatedPlanDigest] were computed with. */
    val digestAlgorithmId: String,
    /** The load-bearing inputs. The identity IS its descriptor plus the digests derived from it. */
    val descriptor: FormDescriptorV1,
    /** Derived from [descriptor]. Never supplied: see [of]. */
    val formDefinitionId: String,
    /** Derived from the stimulus sequence the engine actually generated. */
    val generatedPlanDigest: String,
) {
    /** Redacted: the descriptor carries the seed, which must never reach a log or a crash report. */
    override fun toString(): String =
        "ReproducibleSessionIdentity(exerciseId=${descriptor.exerciseId}, formDefinitionId=$formDefinitionId)"

    override fun equals(other: Any?): Boolean = this === other || (
        other is ReproducibleSessionIdentity &&
            replaySchemaVersion == other.replaySchemaVersion &&
            canonicalizerVersionId == other.canonicalizerVersionId &&
            digestAlgorithmId == other.digestAlgorithmId &&
            descriptor == other.descriptor &&
            formDefinitionId == other.formDefinitionId &&
            generatedPlanDigest == other.generatedPlanDigest
        )

    override fun hashCode(): Int {
        var result = replaySchemaVersion
        result = HASH_MULTIPLIER * result + canonicalizerVersionId.hashCode()
        result = HASH_MULTIPLIER * result + digestAlgorithmId.hashCode()
        result = HASH_MULTIPLIER * result + descriptor.hashCode()
        result = HASH_MULTIPLIER * result + formDefinitionId.hashCode()
        result = HASH_MULTIPLIER * result + generatedPlanDigest.hashCode()
        return result
    }

    companion object {
        const val REPLAY_SCHEMA_VERSION = 1
        private const val HASH_MULTIPLIER = 31

        /**
         * The ONLY way to mint an identity. Both digests are computed from what is actually there, so an
         * identity that misdescribes its own form is not a state this type can be in.
         */
        fun of(descriptor: FormDescriptorV1, generatedPlanDigest: String): ReproducibleSessionIdentity {
            require(generatedPlanDigest.isNotBlank()) { "generatedPlanDigest must not be blank." }
            val snapshot = descriptor.defensiveCopy()
            return ReproducibleSessionIdentity(
                replaySchemaVersion = REPLAY_SCHEMA_VERSION,
                canonicalizerVersionId = CanonicalizerVersion.V1.id,
                digestAlgorithmId = DigestAlgorithmId.SHA_256.id,
                descriptor = snapshot,
                formDefinitionId = definitionIdOf(snapshot, CanonicalizerVersion.V1),
                generatedPlanDigest = generatedPlanDigest,
            )
        }

        /**
         * Digest of the inputs, under a NAMED canonicalizer: an old row is read under the encoder it was
         * written with, never under today's.
         */
        fun definitionIdOf(descriptor: FormDescriptorV1, version: CanonicalizerVersion): String =
            CanonicalEncoder.digest(version, FormDescriptorV1.serializer(), descriptor)

        /**
         * Rebuilds a persisted identity, treating every field as untrusted. Fails closed — never repairs, never
         * fabricates, never silently accepts a digest it cannot reproduce.
         */
        internal fun fromPersisted(surrogate: IdentitySurrogate): ReproducibleSessionIdentity {
            rejectionFor(surrogate)?.let { throw IncoherentIdentityException(it) }
            return ReproducibleSessionIdentity(
                replaySchemaVersion = surrogate.replaySchemaVersion,
                canonicalizerVersionId = surrogate.canonicalizerVersionId,
                digestAlgorithmId = surrogate.digestAlgorithmId,
                descriptor = surrogate.descriptor.defensiveCopy(),
                formDefinitionId = surrogate.formDefinitionId,
                generatedPlanDigest = surrogate.generatedPlanDigest,
            )
        }

        /**
         * Why the row cannot be trusted, or null if it can. A table of gates rather than a chain of throws: the
         * order is the priority, and adding a gate means adding a row.
         *
         * None of these messages echoes the seed, either digest, or any descriptor field. A corrupt row is
         * already suspect; it is not a reason to start emitting reproducibility material into whatever logs the
         * failure.
         */
        private fun rejectionFor(surrogate: IdentitySurrogate): String? = when {
            surrogate.replaySchemaVersion > REPLAY_SCHEMA_VERSION ->
                "Identity replaySchemaVersion is newer than this build supports ($REPLAY_SCHEMA_VERSION). A row " +
                    "from a later schema must not be reinterpreted under today's rules."

            CanonicalizerVersion.fromId(surrogate.canonicalizerVersionId) == null ->
                "Identity names a canonicalizer unknown to this build, so its digest cannot be reproduced and " +
                    "therefore cannot be verified."

            DigestAlgorithmId.fromId(surrogate.digestAlgorithmId) == null ->
                "Identity names a digest algorithm unknown to this build."

            surrogate.generatedPlanDigest.isBlank() ->
                "Identity carries no generatedPlanDigest, so the form it ran cannot be checked against the form " +
                    "this build would rebuild."

            !digestReproduces(surrogate) ->
                "Persisted formDefinitionId does not match the digest recomputed from its own descriptor. The " +
                    "row is corrupt, or was written by a build whose canonicalization differed under the same " +
                    "version id. It is not trusted."

            else -> null
        }

        private fun digestReproduces(surrogate: IdentitySurrogate): Boolean {
            val canonicalizer = CanonicalizerVersion.fromId(surrogate.canonicalizerVersionId) ?: return false
            return definitionIdOf(surrogate.descriptor, canonicalizer) == surrogate.formDefinitionId
        }
    }
}

/**
 * The persisted shape. Separate from the domain type precisely so that decoding cannot bypass
 * [ReproducibleSessionIdentity.of] and hand back an identity whose digest was never recomputed.
 */
@Serializable
@SerialName("ReproducibleSessionIdentity")
internal data class IdentitySurrogate(
    val replaySchemaVersion: Int,
    val canonicalizerVersionId: String,
    val digestAlgorithmId: String,
    val descriptor: FormDescriptorV1,
    val formDefinitionId: String,
    val generatedPlanDigest: String,
) {
    /** Redacted for the same reason as [FormDescriptorV1.toString]. */
    override fun toString(): String = "IdentitySurrogate(redacted)"
}

/**
 * Recomputes on the way in. A digest that is only ever written and never re-derived proves nothing: it would be
 * trusting the row to describe itself honestly, which is exactly what a corrupt row does not do.
 */
object ReproducibleSessionIdentitySerializer : KSerializer<ReproducibleSessionIdentity> {
    override val descriptor: SerialDescriptor = IdentitySurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: ReproducibleSessionIdentity) {
        encoder.encodeSerializableValue(
            IdentitySurrogate.serializer(),
            IdentitySurrogate(
                replaySchemaVersion = value.replaySchemaVersion,
                canonicalizerVersionId = value.canonicalizerVersionId,
                digestAlgorithmId = value.digestAlgorithmId,
                descriptor = value.descriptor,
                formDefinitionId = value.formDefinitionId,
                generatedPlanDigest = value.generatedPlanDigest,
            ),
        )
    }

    override fun deserialize(decoder: Decoder): ReproducibleSessionIdentity =
        ReproducibleSessionIdentity.fromPersisted(decoder.decodeSerializableValue(IdentitySurrogate.serializer()))
}
