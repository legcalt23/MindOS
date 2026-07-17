package com.mindos.app.features.mindboost.core.session

import com.mindos.app.features.mindboost.core.canonical.CanonicalizerVersion
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The identity/digest invariant: an identity that misdescribes its own form must not be a state this type can be
 * in — not "must be caught", not "must be validated by a diligent caller". Unrepresentable.
 *
 * The previous model was a `data class` taking `formId` as a constructor argument, which gave three ways to hold
 * an incoherent identity: pass any string to the constructor; `copy(plannedTrialCount = 99)` keeping the old
 * digest; or decode a row whose digest never matched its fields. `09_DECISION_LOG.md:390-391` fixes the required
 * shape of enforcement for this codebase — a constraint must be *"impuesto por la AUSENCIA del input, no por un
 * verdict que un consumidor podría ignorar"*. These tests assert the absence.
 */
class ReproducibleSessionIdentityTest {

    private fun descriptor(trials: Int = 40) = FormDescriptorV1(
        seedHex = "000000000000106a",
        rngVersionId = "splitmix64-v1",
        engineContractVersion = 1,
        generatorVersion = 2,
        exerciseId = "E2_FILTRO",
        exerciseVersion = 1,
        difficultyMappingVersion = 1,
        resolvedDifficulty = listOf(AxisValueV1("INTERFERENCE", 0.0)),
        sessionTemplateVersion = 1,
        plannedTrialCount = trials,
        resolvedTimingPolicy = listOf(TimingValueV1("isiMs", 700)),
        responseMappingVersion = 1,
        contentVersion = 1,
    )

    private val planDigest = "a".repeat(64)
    private fun identity(trials: Int = 40) = ReproducibleSessionIdentity.of(descriptor(trials), planDigest)

    // -------------------------------------------------------------- (1) no caller-supplied digest

    /**
     * There is no constructor taking a digest, so this test cannot be written as "passing a bad digest is
     * rejected" — the stronger statement is that the API offers no way to supply one at all. What IS assertable
     * is the consequence: the digest always equals what the descriptor actually produces.
     */
    @Test
    fun the_digest_is_computed_from_the_descriptor_never_supplied() {
        val subject = identity()
        assertEquals(
            ReproducibleSessionIdentity.definitionIdOf(subject.descriptor, CanonicalizerVersion.V1),
            subject.formDefinitionId,
            "the identity's digest must be exactly the digest of its own descriptor",
        )
        assertEquals(CanonicalizerVersion.V1.id, subject.canonicalizerVersionId)
        assertEquals("sha-256", subject.digestAlgorithmId)
    }

    // -------------------------------------------------------------- (2) copy() cannot desync

    /**
     * `ReproducibleSessionIdentity` is not a data class, so there is no `copy()` to desynchronize the digest
     * from the fields. Asserted through its consequence: two identities differing in ANY descriptor field have
     * different digests, so no pair of (fields, digest) can be recombined into an incoherent one.
     */
    @Test
    fun two_different_descriptors_can_never_share_a_digest() {
        assertNotEquals(
            identity(trials = 40).formDefinitionId,
            identity(trials = 41).formDefinitionId,
            "a different trial count must produce a different digest — this is the field that decides how many " +
                "draws the generator consumes, i.e. the form itself",
        )
    }

    // -------------------------------------------------------------- (3) defensive snapshot

    /**
     * The aliasing case. `List<T>` in Kotlin is a read-only VIEW, not an immutable type: a caller can pass a
     * `MutableList` and keep the reference. An identity holding that list would silently stop matching its own
     * digest the moment the caller mutated it.
     */
    @Test
    fun mutating_the_callers_list_afterwards_does_not_change_the_identity() {
        val live = mutableListOf(AxisValueV1("INTERFERENCE", 0.0))
        val subject = ReproducibleSessionIdentity.of(
            descriptor().copy(resolvedDifficulty = live),
            planDigest,
        )
        val before = subject.descriptor.resolvedDifficulty.toList()
        val digestBefore = subject.formDefinitionId

        live.add(AxisValueV1("PRESSURE", 1.0))
        live[0] = AxisValueV1("INTERFERENCE", 1.0)

        assertEquals(before, subject.descriptor.resolvedDifficulty, "the identity took an aliased list")
        assertEquals(
            digestBefore,
            ReproducibleSessionIdentity.definitionIdOf(subject.descriptor, CanonicalizerVersion.V1),
            "the identity's descriptor changed under it, so it no longer describes what its digest says",
        )
    }

    // -------------------------------------------------------------- (4) round-trip

    @Test
    fun serializing_and_deserializing_preserves_the_identity_exactly() {
        val subject = identity()
        val json = Json.encodeToString(ReproducibleSessionIdentity.serializer(), subject)
        val back = Json.decodeFromString(ReproducibleSessionIdentity.serializer(), json)
        assertEquals(subject, back, "the identity must survive a round-trip byte-for-byte")
        assertEquals(subject.formDefinitionId, back.formDefinitionId)
        assertEquals(subject.generatedPlanDigest, back.generatedPlanDigest)
        assertEquals(subject.descriptor, back.descriptor)
    }

    // -------------------------------------------------------------- (5) corruption detected

    /**
     * The load-bearing one: decode RECOMPUTES rather than trusting the row.
     *
     * A digest that is only ever written and never re-derived proves nothing — it would be trusting the row to
     * describe itself honestly, which is exactly what a corrupt row does not do.
     */
    @Test
    fun a_row_whose_descriptor_was_tampered_with_is_rejected_on_decode() {
        val subject = identity()
        val json = Json.encodeToString(ReproducibleSessionIdentity.serializer(), subject)
        // Change ONE field of the descriptor, leaving the persisted digest untouched — i.e. forge a row.
        val tampered = json.replace("\"plannedTrialCount\":40", "\"plannedTrialCount\":41")
        assertTrue(tampered != json, "the fixture must actually have been tampered with")

        val error = assertFailsWith<IncoherentIdentityException> {
            Json.decodeFromString(ReproducibleSessionIdentity.serializer(), tampered)
        }
        assertTrue(
            "corrupt" in error.message.orEmpty() || "does not match" in error.message.orEmpty(),
            "the failure must name the reason without echoing the row's contents",
        )
    }

    /** A forged digest with an honest descriptor is the same defect from the other side. */
    @Test
    fun a_row_whose_digest_was_tampered_with_is_rejected_on_decode() {
        val subject = identity()
        val json = Json.encodeToString(ReproducibleSessionIdentity.serializer(), subject)
        val tampered = json.replace(subject.formDefinitionId, "b".repeat(64))
        assertTrue(tampered != json, "the fixture must actually have been tampered with")
        assertFailsWith<IncoherentIdentityException> {
            Json.decodeFromString(ReproducibleSessionIdentity.serializer(), tampered)
        }
    }

    /** Neither the seed nor the digests may appear in the failure text: a corrupt row is not a reason to leak. */
    @Test
    fun the_rejection_message_never_echoes_the_rows_secrets() {
        val subject = identity()
        val json = Json.encodeToString(ReproducibleSessionIdentity.serializer(), subject)
        val tampered = json.replace("\"plannedTrialCount\":40", "\"plannedTrialCount\":41")
        val error = assertFailsWith<IncoherentIdentityException> {
            Json.decodeFromString(ReproducibleSessionIdentity.serializer(), tampered)
        }
        val text = error.message.orEmpty()
        assertTrue("000000000000106a" !in text, "the seed must never reach an exception message")
        assertTrue(subject.formDefinitionId !in text, "the digest must not be echoed")
        assertTrue(planDigest !in text, "the plan digest must not be echoed")
    }

    /** `toString` is the other place a seed escapes into a log. */
    @Test
    fun toString_redacts_the_seed() {
        val subject = identity()
        assertTrue("000000000000106a" !in subject.toString(), "identity.toString leaked the seed")
        assertTrue("000000000000106a" !in subject.descriptor.toString(), "descriptor.toString leaked the seed")
    }

    // -------------------------------------------------------------- (6) unknown future schema

    @Test
    fun a_row_from_a_future_schema_fails_closed_rather_than_being_reinterpreted() {
        val subject = identity()
        val json = Json.encodeToString(ReproducibleSessionIdentity.serializer(), subject)
        val future = json.replace(
            "\"replaySchemaVersion\":${ReproducibleSessionIdentity.REPLAY_SCHEMA_VERSION}",
            "\"replaySchemaVersion\":999",
        )
        assertTrue(future != json, "the fixture must actually declare a future schema")
        assertFailsWith<IncoherentIdentityException>("a future row must not be read under today's rules") {
            Json.decodeFromString(ReproducibleSessionIdentity.serializer(), future)
        }
    }

    // -------------------------------------------------------------- (7) unknown canonicalizer / algorithm

    /**
     * A row naming an encoder this build does not have cannot have its digest reproduced, so it cannot be
     * verified — and an unverifiable identity is not accepted. This is also the seam V2 will use: a V1 row keeps
     * validating under V1's frozen encoder rather than being re-read under V2's rules.
     */
    @Test
    fun a_row_naming_an_unknown_canonicalizer_fails_closed() {
        val subject = identity()
        val json = Json.encodeToString(ReproducibleSessionIdentity.serializer(), subject)
        val unknown = json.replace(CanonicalizerVersion.V1.id, "mindboost-canonical-v99")
        assertTrue(unknown != json, "the fixture must actually name an unknown canonicalizer")
        assertFailsWith<IncoherentIdentityException> {
            Json.decodeFromString(ReproducibleSessionIdentity.serializer(), unknown)
        }
    }

    @Test
    fun a_row_naming_an_unknown_digest_algorithm_fails_closed() {
        val subject = identity()
        val json = Json.encodeToString(ReproducibleSessionIdentity.serializer(), subject)
        val unknown = json.replace("\"sha-256\"", "\"md5\"")
        assertTrue(unknown != json, "the fixture must actually name an unknown algorithm")
        assertFailsWith<IncoherentIdentityException> {
            Json.decodeFromString(ReproducibleSessionIdentity.serializer(), unknown)
        }
    }

    /** An honest row must still decode — otherwise every test above passes for the wrong reason. */
    @Test
    fun an_honest_row_still_decodes() {
        val subject = identity()
        val json = Json.encodeToString(ReproducibleSessionIdentity.serializer(), subject)
        assertEquals(
            subject,
            Json.decodeFromString(ReproducibleSessionIdentity.serializer(), json),
            "if this is red, every rejection test above is satisfied by a decoder that rejects everything",
        )
    }
}
