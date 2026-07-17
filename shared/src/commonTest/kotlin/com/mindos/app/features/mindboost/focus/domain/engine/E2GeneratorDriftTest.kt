package com.mindos.app.features.mindboost.focus.domain.engine

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.E2FiltroGenerator
import com.mindos.app.features.mindboost.focus.domain.exercise.VersionedTrialGenerator
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves the one failure that every version check is structurally blind to: a generator whose OUTPUT changes
 * while its VERSION does not.
 *
 * ## Why this test has to exist
 *
 * `formDefinitionId` digests the inputs — seed, versions, resolved difficulty, trial count. When a developer
 * edits a generator's draw logic and forgets to bump `generatorVersion`, every one of those inputs is
 * *identical*, so the definition digest matches, so every check passes, and the session presents a different form
 * than its identity claims. Nothing stood between that and production except someone remembering to change a
 * number — which is not a control, it is a hope.
 *
 * `generatedPlanDigest` covers the output, so the change is detected whether or not anyone remembered anything.
 * This test is the proof, and it is written the only way that proves it: by actually being a generator that lies
 * about its version.
 *
 * ## Why a mutant class and not an edit to the real generator
 *
 * The property under test is "same declared version, different drawn form ⇒ rejected". [DriftedE2Generator]
 * declares **exactly** the version `FocusFormContract` authorizes (so every version gate passes, as it would for
 * a real unbumped edit) and draws a different sequence. Editing `E2FiltroGenerator` in the tree would prove the
 * same thing but only while the edit existed; this proves it permanently, in the build.
 */
class E2GeneratorDriftTest {

    private val difficulty = DifficultyVector.EASIEST
    private val spec = BlockSpec(targetTrialCount = 12, mode = Mode.TRAINING)

    private fun engineWith(generator: VersionedTrialGenerator): DefaultFocusEngine = DefaultFocusEngine(
        generators = mapOf(ExerciseId.E2_FILTRO to generator),
        seedSource = FixedSeedSource,
    )

    private fun createWithRealGenerator(): FocusSessionPlan = engineWith(E2FiltroGenerator()).createSession(
        CreateFocusSessionRequest(
            exerciseId = ExerciseId.E2_FILTRO,
            difficulty = difficulty,
            spec = spec,
            sessionTemplateVersion = 1,
            mode = FocusSessionMode.TRAINING_ONLY,
        ),
    )

    /**
     * The headline. A drifted generator that kept its version number is rejected — by the PLAN digest
     * specifically, not by any version gate, because every version still agrees.
     */
    @Test
    fun a_generator_whose_output_changed_without_a_version_bump_is_rejected_by_the_plan_digest() {
        val original = createWithRealGenerator()
        val restored = engineWith(DriftedE2Generator).restoreSession(
            RestoreFocusSessionRequest(original.identity, FocusSessionMode.TRAINING_ONLY),
        )
        assertEquals(
            FocusRestoreResult.Rejected.PlanDigestMismatch,
            restored,
            "A generator drew a DIFFERENT form while declaring the same generatorVersion, and the engine " +
                "accepted it. Every version number matches — that is the whole point of this failure mode — so " +
                "only a digest of the generated output can catch it.",
        )
    }

    /**
     * The discriminating half, without which the test above passes for the wrong reason: an engine that rejected
     * everything would satisfy it. The REAL generator must still restore cleanly.
     */
    @Test
    fun the_undrifted_generator_still_restores_cleanly() {
        val original = createWithRealGenerator()
        val restored = engineWith(E2FiltroGenerator()).restoreSession(
            RestoreFocusSessionRequest(original.identity, FocusSessionMode.TRAINING_ONLY),
        )
        assertTrue(
            restored is FocusRestoreResult.Restored,
            "The unmodified generator must reproduce its own form; if this is red the digest is unstable and " +
                "the test above proves nothing (a restore that always fails would also be 'safe').",
        )
        assertEquals(original.stimuli, restored.plan.stimuli, "the restored form must be the form that ran")
        assertEquals(original.identity.generatedPlanDigest, restored.plan.identity.generatedPlanDigest)
    }

    /** The drift is real and not an artefact of the test: the two generators genuinely disagree. */
    @Test
    fun the_mutant_really_does_draw_a_different_form() {
        val seed = SessionSeed(4242L, RngVersion.V1)
        val real = E2FiltroGenerator().generate(difficulty, spec, seed)
        val drifted = DriftedE2Generator.generate(difficulty, spec, seed)
        assertTrue(real != drifted, "the mutant must actually differ, or the rejection above is vacuous")
        assertEquals(
            E2FiltroGenerator().generatorVersion,
            DriftedE2Generator.generatorVersion,
            "the mutant must declare the SAME version — the version gates must pass so the plan digest is " +
                "demonstrably the thing doing the catching",
        )
    }

    /** A generator that draws a different form while claiming E2's authorized version. */
    private object DriftedE2Generator : VersionedTrialGenerator {
        override val exerciseId: ExerciseId = ExerciseId.E2_FILTRO

        /** Deliberately the authorized version: this mutant models a forgotten bump, not a declared change. */
        override val generatorVersion: Int = E2FiltroGenerator.E2_GENERATOR_VERSION

        override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: SessionSeed): List<Stimulus> =
            E2FiltroGenerator().generate(difficulty, spec, seed).map { stimulus ->
                val payload = stimulus.payload as StimulusPayload.Flanker
                // One flipped direction is enough: a real unbumped edit is usually this small.
                val flipped = if (payload.centerDirection == ResponseKey.LEFT) ResponseKey.RIGHT else ResponseKey.LEFT
                stimulus.copy(
                    correctResponseKey = flipped,
                    payload = payload.copy(centerDirection = flipped),
                )
            }
    }

    private object FixedSeedSource : SeedSource {
        override fun mint(): SessionSeed = SessionSeed(4242L, RngVersion.V1)
    }
}
