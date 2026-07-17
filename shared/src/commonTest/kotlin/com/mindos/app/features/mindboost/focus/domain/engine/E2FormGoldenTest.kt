package com.mindos.app.features.mindboost.focus.domain.engine

import com.mindos.app.features.mindboost.core.session.TimingValueV1
import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.FocusExerciseGenerators
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.elementNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The literal golden for E2's form.
 *
 * ## History, because it explains the shape of this file
 *
 * This is the third version. The first compared E2's 40 directions against `kotlin.random.Random(seed)
 * .nextBoolean()` ×40 and passed when they *disagreed* — but E2 draws a `nextBoolean` AND a `nextDouble` per
 * trial, so the two streams would not line up even if E2 were still on the stdlib. It agreed with itself.
 *
 * The second (this file's previous form) fixed that with literals, but claimed at line 60 to pin *"every field of
 * every trial"* while projecting six of `Stimulus`'s eight fields. `correctResponseKey` — the field
 * `TrialClassifier.classifyChoice` scores every trial against — was never asserted. The generator happens to
 * derive it and `centerDirection` from one shared local, so the omission was invisible; decouple them and every
 * honest E2 trial scores wrong with the suite green. That is the exact defect class this repo's own learning of
 * 2026-07-15 records twice: *"un golden pinea literales o no es un golden"*. It also pinned exactly one point of
 * the space (seed 4242 × EASIEST × n=40), and EASIEST is the one difficulty where a correct lerp and a hardcoded
 * constant are indistinguishable.
 *
 * ## Independence — stated precisely, because it is the only property a reader cannot verify from the test
 *
 * The direction/congruency literals came from a reference implementation of splitmix64 + Kotlin's
 * `nextBits`/`doubleFromParts` derivations + E2's mapping, written outside Kotlin and anchored to the published
 * splitmix64 vector for seed 0 (`0xE220A8397B1DCDAF`). **That reference is not committed to this repo, and
 * without its source and checksum this claim is unverifiable and unreproducible** — if the literals had in fact
 * been read out of the Kotlin they check, this file would be byte-identical and still green. So the honest
 * statement is: these literals are *pinned* (they detect any future drift, which is most of the value) but their
 * *independence* is REPORTED, not VERIFIED. Committing the reference is open work, declared in the close-out.
 *
 * The RNG itself is independently anchored by `DeterministicRngGoldenTest`, which does pin published vectors —
 * so the layer beneath this one is verified even though this one's derivation is not.
 */
@OptIn(ExperimentalSerializationApi::class)
class E2FormGoldenTest {

    private val spec = BlockSpec(targetTrialCount = 40, mode = Mode.TRAINING)

    private fun engineWithSeed(value: Long) = DefaultFocusEngine(
        generators = FocusExerciseGenerators.versioned,
        seedSource = SeedSource { SessionSeed(value, RngVersion.V1) },
    )

    private fun plan(seedValue: Long, difficulty: DifficultyVector = DifficultyVector.EASIEST, trials: Int = 40) =
        engineWithSeed(seedValue).createSession(
            CreateFocusSessionRequest(
                exerciseId = ExerciseId.E2_FILTRO,
                difficulty = difficulty,
                spec = BlockSpec(targetTrialCount = trials, mode = Mode.TRAINING),
                sessionTemplateVersion = SessionTemplate.TEMPLATE_VERSION,
                mode = FocusSessionMode.TRAINING_ONLY,
            ),
        )

    /**
     * The projector. Every field of `Stimulus` except the `@Transient` runtime onset — including the three the
     * previous version silently dropped (`correctResponseKey`, `isTarget`, `requiresStop`).
     */
    private fun form(seedValue: Long, difficulty: DifficultyVector = DifficultyVector.EASIEST): List<String> =
        plan(seedValue, difficulty).stimuli.map { stimulus ->
            val payload = stimulus.payload as StimulusPayload.Flanker
            listOf(
                stimulus.index.toString(),
                stimulus.correctResponseKey?.name ?: "NONE",
                stimulus.isTarget.toString(),
                stimulus.requiresStop.toString(),
                payload.centerDirection.name,
                if (payload.congruent) "C" else "I",
                payload.flankerCount.toString(),
                stimulus.responseWindowMs.toString(),
                stimulus.interStimulusDelayMs.toString(),
            ).joinToString("|")
        }

    /**
     * The projector covers every canonicalized field. Without this, a field added to `Stimulus` would silently
     * fall outside the golden — which is how `correctResponseKey` went unasserted for the whole of v2.
     *
     * The expected side is READ FROM `Stimulus.serializer().descriptor`, not typed out here. The first version
     * of this test compared two hand-written literal lists in this same file, which proved only that a developer
     * had typed the same thing twice: adding a field to `Stimulus` would leave it green, because neither side
     * knew what `Stimulus` contains. A test-engineer review caught it — and it was the *same* defect this whole
     * slice exists to remove (a hand-maintained field list drifting from the type), reintroduced one layer up,
     * inside the test written to prevent it. The list now comes from the type, so it cannot drift from it.
     */
    @Test
    fun the_projector_covers_every_field_the_plan_digest_covers() {
        // `form()` emits every Stimulus field EXCEPT `payload`, which it expands into that payload's own fields.
        // Both halves are read from the serializers, so adding a field to Stimulus OR to Flanker turns this red.
        val expected = (Stimulus.serializer().descriptor.elementNames.toSet() - "payload") +
            StimulusPayload.Flanker.serializer().descriptor.elementNames.toSet()
        assertEquals(
            expected,
            PROJECTED_FIELDS,
            "the golden's projector and the plan digest must cover the same fields; if they diverge, the golden " +
                "stops protecting whatever the digest still covers",
        )
        // ...and the projector's real OUTPUT has one column per covered field, so deleting a column cannot pass
        // while the name list still claims it is there.
        form(4242L).forEach { line ->
            assertEquals(expected.size, line.split("|").size, "one projected column per covered field")
        }
    }

    @Test
    fun the_form_for_seed_4242_at_easiest_matches_the_reference() {
        val expectedDirections = "LLLRRLLLRLRRLRRLLRLLLLLRLLRRLRLRRRLRRRRR"
        val expectedCongruency = "CCCCCCICICICICCCCCCCCCCCCICCICCCCICCCCCC"
        val actual = form(4242L)

        assertEquals(40, actual.size, "the form must have exactly the requested trial count")
        assertEquals(
            expectedDirections,
            actual.joinToString("") { it.split("|")[4].first().toString() },
            "E2's target directions drifted from the splitmix64 reference",
        )
        assertEquals(
            expectedCongruency,
            actual.joinToString("") { it.split("|")[5] },
            "E2's congruency sequence drifted from the splitmix64 reference",
        )
        assertEquals(
            List(40) { "2|1500|700" },
            actual.map { it.split("|").drop(6).joinToString("|") },
            "E2's resolved flankers/window/ISI drifted from the reference mapping at EASIEST",
        )
    }

    /**
     * The fix for the defect this file's previous version had: `correctResponseKey` is asserted, and asserted to
     * AGREE with the payload's direction.
     *
     * The generator derives both from one local, so this looks redundant — it is not. That coupling is an
     * implementation detail nothing enforces, and it is the entire reason the omission was invisible. Decouple
     * them (`correctResponseKey = opposite(direction)`) and every trial scores inverted: honest accuracy ≈ 0,
     * with the previous golden still green.
     */
    @Test
    fun the_scored_response_key_is_pinned_and_agrees_with_the_presented_direction() {
        plan(4242L).stimuli.forEach { stimulus ->
            val payload = stimulus.payload as StimulusPayload.Flanker
            assertEquals(
                payload.centerDirection,
                stimulus.correctResponseKey,
                "trial ${stimulus.index}: the key the trial is SCORED by must be the direction the user is " +
                    "SHOWN. If these decouple, every honest response is marked wrong.",
            )
            assertTrue(stimulus.isTarget, "every E2 trial is a target: the user always responds")
            assertTrue(!stimulus.requiresStop, "E2 is not a stop-signal paradigm")
        }
    }

    /** The first three trials spelled out in full, so a failure above is readable rather than a wall of diff. */
    @Test
    fun the_first_three_trials_are_exactly_the_reference_trials() {
        assertEquals(
            listOf(
                "0|LEFT|true|false|LEFT|C|2|1500|700",
                "1|LEFT|true|false|LEFT|C|2|1500|700",
                "2|LEFT|true|false|LEFT|C|2|1500|700",
            ),
            form(4242L).take(3),
        )
    }

    /**
     * A second point of the space, at a difficulty where the mapping's lerp is NOT at t=0.
     *
     * At EASIEST every axis is 0, so `lerp(a, b, 0) == a` — a correct lerp and a hardcoded constant produce the
     * same numbers, and the previous golden could not tell them apart. PRESSURE=1.0 puts the window/ISI at the
     * far end (`lerp(1500,900,1)=900`, `lerp(700,500,1)=500`), so a broken mapping moves these literals.
     */
    @Test
    fun the_mapping_is_pinned_away_from_the_lerp_origin() {
        val hard = DifficultyVector.EASIEST.adjust(
            DifficultyAxis.PRESSURE,
            com.mindos.app.features.mindboost.focus.domain.model.AxisLevel(1.0),
        )
        val actual = form(4242L, hard)
        assertEquals(
            List(40) { "2|900|500" },
            actual.map { it.split("|").drop(6).joinToString("|") },
            "E2's mapping at PRESSURE=1.0 drifted: window/ISI must be lerp(1500,900,1)=900 and lerp(700,500,1)=500",
        )
        assertEquals(
            form(4242L).map { it.split("|")[4] },
            actual.map { it.split("|")[4] },
            "PRESSURE must not touch the drawn directions — it maps to timing only",
        )
    }

    /** INTERFERENCE at the far end must actually change what is drawn, not just what is displayed. */
    @Test
    fun interference_changes_the_drawn_congruency_not_only_the_timing() {
        val hard = DifficultyVector.EASIEST.adjust(
            DifficultyAxis.INTERFERENCE,
            com.mindos.app.features.mindboost.focus.domain.model.AxisLevel(1.0),
        )
        val easiestIncongruent = form(4242L).count { it.split("|")[5] == "I" }
        val hardIncongruent = form(4242L, hard).count { it.split("|")[5] == "I" }
        assertTrue(
            hardIncongruent > easiestIncongruent,
            "INTERFERENCE=1.0 must yield more incongruent trials than EASIEST " +
                "(was $hardIncongruent vs $easiestIncongruent)",
        )
    }

    @Test
    fun a_different_seed_yields_a_different_form() {
        assertNotEquals(form(4242L), form(7L), "distinct seeds must yield distinct forms")
    }

    /** Boundary seeds: the extremes must produce a well-formed form and a well-formed seedHex. */
    @Test
    fun boundary_seeds_round_trip_exactly() {
        listOf(0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE).forEach { value ->
            val identity = plan(value).identity
            assertEquals(
                value,
                identity.descriptor.seedHex.toULong(HEX_RADIX).toLong(),
                "seedHex must round-trip to the exact Long for $value",
            )
            assertEquals(
                SEED_HEX_LENGTH,
                identity.descriptor.seedHex.length,
                "seedHex must be fixed-width for $value: '0' and its padded form must be one value",
            )
        }
        assertEquals("ffffffffffffffff", plan(-1L).identity.descriptor.seedHex)
        assertEquals("0000000000000000", plan(0L).identity.descriptor.seedHex)
        assertEquals("8000000000000000", plan(Long.MIN_VALUE).identity.descriptor.seedHex)
        assertEquals("7fffffffffffffff", plan(Long.MAX_VALUE).identity.descriptor.seedHex)
        assertEquals("splitmix64-v1", plan(0L).identity.descriptor.rngVersionId, "a stable id, never an ordinal")
    }

    /** Trial counts at the boundaries the model allows. */
    @Test
    fun the_form_honours_its_declared_trial_count() {
        listOf(1, 40, 55).forEach { n ->
            assertEquals(n, plan(4242L, trials = n).stimuli.size, "form size for n=$n")
            assertEquals(n, plan(4242L, trials = n).identity.descriptor.plannedTrialCount, "declared n=$n")
        }
    }

    /**
     * The definition digest separates two forms that differ ONLY in resolved difficulty — every version is
     * identical, so nothing else could.
     */
    @Test
    fun the_definition_digest_changes_when_the_resolved_difficulty_changes() {
        val easiest = plan(4242L).identity
        val harder = plan(
            4242L,
            DifficultyVector.EASIEST.stepUp(DifficultyAxis.INTERFERENCE, 0.5),
        ).identity
        assertNotEquals(
            easiest.formDefinitionId,
            harder.formDefinitionId,
            "a different resolved difficulty must change the definition digest",
        )
        assertEquals(
            easiest.descriptor.generatorVersion,
            harder.descriptor.generatorVersion,
            "sanity: the versions are identical, so the digest is the only thing separating these two forms",
        )
    }

    /** Same inputs in, same digests out — otherwise the digests could not detect drift at all. */
    @Test
    fun the_same_inputs_yield_the_same_digests() {
        assertEquals(plan(4242L).identity.formDefinitionId, plan(4242L).identity.formDefinitionId)
        assertEquals(plan(4242L).identity.generatedPlanDigest, plan(4242L).identity.generatedPlanDigest)
    }

    /**
     * The descriptor the ENGINE builds must carry the timing it actually resolved.
     *
     * Added because the mutation suite found this uncovered: replacing the engine's
     * `resolvedTimingPolicy = FocusSessionResolution.timingPolicyFor(...)` with `emptyList()` left the whole
     * suite green. `CanonicalShapeGoldenTest` proves the FIELD moves the digest, but it builds its own
     * descriptor fixture; the tests above read window/ISI off the STIMULI, not off the descriptor. So nothing
     * asserted that the engine populates it — the exact shape of gap this slice exists to remove, found in the
     * slice's own work. Two builds whose mapping differs would present the same items at different speeds, and
     * the identity would not record which.
     */
    @Test
    fun the_engine_records_the_timing_it_actually_resolved() {
        assertEquals(
            listOf(TimingValueV1("isiMs", 700), TimingValueV1("responseWindowMs", 1500)),
            plan(4242L).identity.descriptor.resolvedTimingPolicy,
            "the identity must record the window/ISI this session resolved, or the form cannot be rebuilt at " +
                "the speed it was presented",
        )
        val hard = DifficultyVector.EASIEST.adjust(
            DifficultyAxis.PRESSURE,
            com.mindos.app.features.mindboost.focus.domain.model.AxisLevel(1.0),
        )
        assertEquals(
            listOf(TimingValueV1("isiMs", 500), TimingValueV1("responseWindowMs", 900)),
            plan(4242L, hard).identity.descriptor.resolvedTimingPolicy,
            "the recorded timing must track the resolved difficulty, not a constant",
        )
        assertNotEquals(
            plan(4242L).identity.formDefinitionId,
            plan(4242L, hard).identity.formDefinitionId,
            "a different resolved timing must change the definition digest",
        )
    }

    private companion object {
        const val HEX_RADIX = 16
        const val SEED_HEX_LENGTH = 16

        /**
         * What `form()` actually projects, in its emitted column order — the ONE hand-written list here, and it
         * is the thing under test rather than the expectation. It is compared against
         * `Stimulus.serializer().descriptor.elementNames` (the digest's real field set) and against the
         * projector's real column count, so it cannot silently disagree with either.
         *
         * An earlier version kept a second literal copy of the field names as the *expectation* too. That made
         * the test a comparison of two hand-typed lists, which is exactly the drift this slice removed from
         * production. The expectation must come from the type.
         */
        val PROJECTED_FIELDS = setOf(
            "index", "correctResponseKey", "isTarget", "requiresStop",
            // `payload` expanded into Flanker's own fields — the projector reads through it rather than past it.
            "centerDirection", "congruent", "flankerCount",
            "responseWindowMs", "interStimulusDelayMs",
        )
    }
}
