package com.mindos.app.features.mindboost.core.canonical

import com.mindos.app.features.mindboost.core.session.AxisValueV1
import com.mindos.app.features.mindboost.core.session.FormDescriptorV1
import com.mindos.app.features.mindboost.core.session.ReproducibleSessionIdentity
import com.mindos.app.features.mindboost.core.session.TimingValueV1
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.elementNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Pins the SHAPE of every canonicalized type, and the BYTES of the V1 encoder.
 *
 * ## What this is for
 *
 * The canonical digest is derived from each type's `SerialDescriptor`, which means a new property enters the
 * digest automatically — that is the whole point, and it removes the "someone forgot to add the field" failure
 * that killed the hand-written canonicalizer twice. But it introduces the mirror risk: a property added
 * *deliberately* silently changes every digest, and every previously persisted row stops validating, with no
 * test going red to say so.
 *
 * So the shape is pinned here. Adding, removing or renaming a canonicalized property fails this test, and the
 * only way past it is to decide, consciously: is this a new `CanonicalizerVersion` (old rows keep validating
 * under V1) or an accepted break? That decision is the thing that must not be automatic.
 *
 * `CanonicalizerVersion.V1` is FROZEN. The byte goldens below are what "frozen" means operationally: if someone
 * edits the V1 encoder, these literals move and the test fails. Without them, editing V1 would invalidate every
 * persisted row while the whole suite stayed green, because every test would move with the encoder.
 */
@OptIn(ExperimentalSerializationApi::class)
class CanonicalShapeGoldenTest {

    private val descriptor = FormDescriptorV1(
        seedHex = "000000000000106a",
        rngVersionId = "splitmix64-v1",
        engineContractVersion = 1,
        generatorVersion = 2,
        exerciseId = "E2_FILTRO",
        exerciseVersion = 1,
        difficultyMappingVersion = 1,
        resolvedDifficulty = listOf(AxisValueV1("INTERFERENCE", 0.0), AxisValueV1("PRESSURE", 0.5)),
        sessionTemplateVersion = 1,
        plannedTrialCount = 40,
        resolvedTimingPolicy = listOf(TimingValueV1("isiMs", 700), TimingValueV1("responseWindowMs", 1500)),
        responseMappingVersion = 1,
        contentVersion = 1,
    )

    // ---------------------------------------------------------------- shape

    /**
     * The descriptor's field list, pinned. This is the list the digest is computed over; if it changes, every
     * persisted `formDefinitionId` changes with it.
     */
    @Test
    fun form_descriptor_shape_is_pinned() {
        assertEquals(
            listOf(
                "seedHex", "rngVersionId", "engineContractVersion", "generatorVersion", "exerciseId",
                "exerciseVersion", "difficultyMappingVersion", "resolvedDifficulty", "sessionTemplateVersion",
                "plannedTrialCount", "resolvedTimingPolicy", "responseMappingVersion", "contentVersion",
            ),
            FormDescriptorV1.serializer().descriptor.elementNames.toList(),
            "FormDescriptorV1's canonicalized shape changed. Every previously persisted formDefinitionId is now " +
                "unreproducible. Publish CanonicalizerVersion.V2 and keep V1's encoder frozen, or accept the " +
                "break explicitly — do not just update this list.",
        )
    }

    /**
     * The stimulus shape, pinned — this is what `generatedPlanDigest` covers.
     *
     * `onsetMonotonicNs` must NOT appear: it is stamped by the runtime at presentation, so digesting it would
     * make a plan's digest depend on the wall clock and never match on restore.
     */
    @Test
    fun stimulus_shape_is_pinned_and_excludes_runtime_state() {
        val names = Stimulus.serializer().descriptor.elementNames.toList()
        assertEquals(
            listOf(
                "index", "isTarget", "requiresStop", "correctResponseKey", "payload",
                "interStimulusDelayMs", "responseWindowMs",
            ),
            names,
            "Stimulus's canonicalized shape changed, so every generatedPlanDigest changed.",
        )
        assertTrue(
            "onsetMonotonicNs" !in names,
            "onsetMonotonicNs is @Transient for a reason: it is what the runtime DID, not what the generator " +
                "planned. Digesting it makes a plan's digest depend on the clock and never match on restore.",
        )
    }

    // ---------------------------------------------------------------- frozen bytes

    /**
     * V1's bytes, pinned to a literal. Editing the V1 encoder moves this.
     *
     * Derived by running the encoder — which would normally make it a snapshot rather than a golden. It is
     * defensible here for a reason the RNG golden could not claim: this pins the encoder against *future* edits,
     * not against a reference implementation. The property it protects is "V1 never changes again", and the only
     * fact needed for that is what V1 emits today. The independence that matters for a golden of a *derived*
     * value (the RNG's) is not the property under test here.
     */
    @Test
    fun canonical_v1_bytes_are_frozen() {
        val digest = CanonicalEncoder.digest(CanonicalizerVersion.V1, FormDescriptorV1.serializer(), descriptor)
        assertEquals(SHA256_HEX_LENGTH, digest.length, "SHA-256 hex must be 64 chars")
        // The digest of THIS descriptor under V1. If this moves, V1's encoding moved.
        assertEquals(FROZEN_V1_DIGEST, digest, FROZEN_MESSAGE)
    }

    /** The version id is inside the digested bytes, so a V2 digest can never collide with a V1 digest. */
    @Test
    fun the_canonicalizer_version_is_inside_the_digested_bytes() {
        val bytes = CanonicalEncoder.encode(CanonicalizerVersion.V1, FormDescriptorV1.serializer(), descriptor)
        val text = bytes.decodeToString()
        assertTrue(
            CanonicalizerVersion.V1.id in text,
            "the encoder version must be part of what is digested, or two encoders could agree by accident",
        )
    }

    // ---------------------------------------------------------------- unambiguity

    /**
     * The concrete collision the old encoding had, asserted to be gone.
     *
     * The old canonicalizer joined map entries as `"$k=$v"` with `,`, so `{"a": 1.0, "b": 2.0}` and
     * `{"a=1.0,b": 2.0}` both rendered `a=1.0,b=2.0`. Whether a caller could inject such a key was a separate
     * question from whether the encoding distinguished them — it did not. Length-prefixed strings have no
     * delimiter to collide on, so the two are now different bytes by construction.
     */
    @Test
    fun a_delimiter_inside_a_key_cannot_forge_another_key() {
        val honest = descriptor.copy(
            resolvedDifficulty = listOf(AxisValueV1("a", 1.0), AxisValueV1("b", 0.5)),
        )
        val forged = descriptor.copy(
            resolvedDifficulty = listOf(AxisValueV1("a=1.0,b", 0.5)),
        )
        assertNotEquals(
            digestOf(honest),
            digestOf(forged),
            "two different difficulty sets produced the same digest — the encoding is ambiguous again",
        )
    }

    /** Insertion order is not part of the value; the descriptor requires sorted keys, so equal sets agree. */
    @Test
    fun the_same_values_digest_identically_regardless_of_how_they_were_built() {
        val a = descriptor.copy(
            resolvedDifficulty = listOf(AxisValueV1("INTERFERENCE", 0.0), AxisValueV1("PRESSURE", 0.5)),
        )
        val b = descriptor.copy(
            resolvedDifficulty = listOf(AxisValueV1("PRESSURE", 0.5), AxisValueV1("INTERFERENCE", 0.0))
                .sortedBy { it.axis },
        )
        assertEquals(digestOf(a), digestOf(b))
    }

    /** Every load-bearing field moves the digest. A field that does not is a field the digest does not protect. */
    @Test
    fun every_descriptor_field_changes_the_digest() {
        val base = digestOf(descriptor)
        val mutants = listOf(
            "seedHex" to descriptor.copy(seedHex = "000000000000106b"),
            "rngVersionId" to descriptor.copy(rngVersionId = "other-rng-v9"),
            "engineContractVersion" to descriptor.copy(engineContractVersion = 2),
            "generatorVersion" to descriptor.copy(generatorVersion = 3),
            "exerciseId" to descriptor.copy(exerciseId = "E5_CONFLICTO"),
            "exerciseVersion" to descriptor.copy(exerciseVersion = 2),
            "difficultyMappingVersion" to descriptor.copy(difficultyMappingVersion = 2),
            "resolvedDifficulty" to descriptor.copy(
                resolvedDifficulty = listOf(AxisValueV1("INTERFERENCE", 0.25), AxisValueV1("PRESSURE", 0.5)),
            ),
            "sessionTemplateVersion" to descriptor.copy(sessionTemplateVersion = 2),
            "plannedTrialCount" to descriptor.copy(plannedTrialCount = 41),
            "resolvedTimingPolicy" to descriptor.copy(
                resolvedTimingPolicy = listOf(TimingValueV1("isiMs", 701), TimingValueV1("responseWindowMs", 1500)),
            ),
            "responseMappingVersion" to descriptor.copy(responseMappingVersion = 2),
            "contentVersion" to descriptor.copy(contentVersion = 2),
        )
        assertEquals(
            FormDescriptorV1.serializer().descriptor.elementNames.toList().size,
            mutants.size,
            "every descriptor field must have a mutant here, or an unprotected field could be added unnoticed",
        )
        mutants.forEach { (field, mutant) ->
            assertNotEquals(base, digestOf(mutant), "changing $field did not change the digest")
        }
    }

    // ---------------------------------------------------------------- fail-closed values

    /** NaN/infinity have no single bit pattern and no meaning as a difficulty. Rejected at the boundary. */
    @Test
    fun non_finite_doubles_are_rejected_rather_than_digested() {
        assertFailsWith<IllegalArgumentException> { AxisValueV1("X", Double.NaN) }
        assertFailsWith<IllegalArgumentException> { AxisValueV1("X", Double.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { AxisValueV1("X", Double.NEGATIVE_INFINITY) }
    }

    /** `-0.0 == 0.0` under IEEE, so two equal values must not produce different bytes. */
    @Test
    fun negative_zero_and_zero_digest_identically() {
        val positive = descriptor.copy(resolvedDifficulty = listOf(AxisValueV1("A", 0.0)))
        val negative = descriptor.copy(resolvedDifficulty = listOf(AxisValueV1("A", -0.0)))
        assertEquals(
            digestOf(positive),
            digestOf(negative),
            "-0.0 and 0.0 are equal, so they must be the same value to the digest",
        )
    }

    /** A seed is 64 bits; "0" and its padded form must be one value, not two. */
    @Test
    fun a_seed_has_exactly_one_spelling() {
        assertFailsWith<IllegalArgumentException>("unpadded seed must be rejected") {
            descriptor.copy(seedHex = "0")
        }
        assertFailsWith<IllegalArgumentException>("uppercase seed must be rejected") {
            descriptor.copy(seedHex = "000000000000106A")
        }
        assertFailsWith<IllegalArgumentException>("over-long seed must be rejected") {
            descriptor.copy(seedHex = "000000000000106a0")
        }
    }

    /** An unbounded trial count read from a persisted row is an allocation instruction from untrusted input. */
    @Test
    fun the_trial_count_is_bounded() {
        assertFailsWith<IllegalArgumentException> { descriptor.copy(plannedTrialCount = 0) }
        assertFailsWith<IllegalArgumentException> { descriptor.copy(plannedTrialCount = -1) }
        assertFailsWith<IllegalArgumentException> { descriptor.copy(plannedTrialCount = Int.MAX_VALUE) }
        assertFailsWith<IllegalArgumentException> {
            descriptor.copy(plannedTrialCount = FormDescriptorV1.MAX_PLANNED_TRIALS + 1)
        }
    }

    /** A key declared twice makes the digest depend on which one a reader happens to take. */
    @Test
    fun duplicate_and_unordered_keys_are_rejected() {
        assertFailsWith<IllegalArgumentException>("duplicate axis") {
            descriptor.copy(resolvedDifficulty = listOf(AxisValueV1("A", 0.1), AxisValueV1("A", 0.2)))
        }
        assertFailsWith<IllegalArgumentException>("unordered axes") {
            descriptor.copy(resolvedDifficulty = listOf(AxisValueV1("B", 0.1), AxisValueV1("A", 0.2)))
        }
    }

    /** Version 0 or negative names no published contract. */
    @Test
    fun versions_must_be_positive() {
        assertFailsWith<IllegalArgumentException> { descriptor.copy(generatorVersion = 0) }
        assertFailsWith<IllegalArgumentException> { descriptor.copy(contentVersion = -1) }
    }

    private fun digestOf(d: FormDescriptorV1): String =
        ReproducibleSessionIdentity.definitionIdOf(d, CanonicalizerVersion.V1)

    private companion object {
        /**
         * V1's digest of the descriptor above, FROZEN. If you are here because this failed: you changed V1's
         * encoding, and every persisted row's formDefinitionId just became unreproducible.
         *
         * Taken from the encoder once and pinned — legitimate here, and NOT legitimate for a derived value. The
         * property under test is "V1 never changes again", and the only fact that needs is what V1 emits today.
         * The SHA-256 underneath is independently anchored to the published FIPS 180-4 vectors in Sha256Test,
         * so this is not a hash agreeing with itself.
         */
        const val SHA256_HEX_LENGTH = 64
        const val FROZEN_V1_DIGEST = "f28cd70ae0fc340921e81bb684d67a1fe1c3a9c40f160227df4eac6a4d134992"
        const val FROZEN_MESSAGE =
            "CanonicalizerVersion.V1's bytes changed. V1 is frozen: rows written under it are validated by " +
                "recomputing their digest, so editing its encoder silently invalidates every one of them while " +
                "every other test stays green. Publish V2 instead."
    }
}
