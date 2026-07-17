package com.mindos.app.features.mindboost.focus.domain.engine

import com.mindos.app.features.mindboost.core.canonical.CanonicalizerVersion
import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.session.AxisValueV1
import com.mindos.app.features.mindboost.core.session.FormDescriptorV1
import com.mindos.app.features.mindboost.core.session.ReproducibleSessionIdentity
import com.mindos.app.features.mindboost.core.session.TimingValueV1
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.E2DifficultyMapping
import com.mindos.app.features.mindboost.focus.domain.exercise.VersionedTrialGenerator
import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus

/**
 * Production [FocusEngine] over the feature's VERSIONED generator registry.
 *
 * It can only be constructed over [VersionedTrialGenerator]s, so "the engine built a form from an unversioned
 * generator" is not a state that compiles — the map's value type says so.
 *
 * A `data class` deliberately: [com.mindos.app.features.mindboost.focus.di.FocusGraph] is a data class whose
 * equality is relied on, so this engine must compare structurally rather than by identity.
 */
data class DefaultFocusEngine(
    private val generators: Map<ExerciseId, VersionedTrialGenerator>,
    private val seedSource: SeedSource,
) : FocusEngine {

    /**
     * Mints the seed, resolves versions, generates the form, then digests BOTH the inputs and the output.
     *
     * The order is the acceptance criterion, not a preference: validate → generate → validate the output →
     * digest → (caller persists) → trial 1. The identity is built last, from what was actually produced, so it
     * cannot describe a form that was never generated.
     */
    override fun createSession(request: CreateFocusSessionRequest): FocusSessionPlan {
        val generator = versionedGeneratorFor(request.exerciseId)
        val versions = authorizedVersionsFor(request.exerciseId, generator)
        val seed = seedSource.mint()
        val descriptor = descriptorFor(
            SessionInputs(
                seed = seed,
                exerciseId = request.exerciseId,
                difficulty = request.difficulty,
                sessionTemplateVersion = request.sessionTemplateVersion,
                plannedTrialCount = request.spec.targetTrialCount,
            ),
            versions,
        )
        val stimuli = generator.generate(request.difficulty, request.spec, seed)
        requireWellFormed(stimuli, descriptor.plannedTrialCount)
        val identity = ReproducibleSessionIdentity.of(descriptor, FocusPlanDigest.of(stimuli))
        return planFor(request.exerciseId, identity, stimuli, request.mode)
    }

    /**
     * Rebuilds a persisted session's form, treating the identity as untrusted input.
     *
     * Two independent questions are asked, because they fail differently:
     * - do the INPUTS still rebuild to the same definition digest? Catches drift in a version or in the resolved
     *   difficulty — the latter being something no version number would ever catch.
     * - does the regenerated OUTPUT still digest the same? Catches a generator whose draw logic changed without
     *   its version being bumped, which no input-derived check can see, because in that case the inputs are
     *   identical and every input-derived digest agrees.
     *
     * Every failure is a typed rejection: nothing is presented, nothing becomes eligible, nothing is repaired,
     * and no seed is ever silently re-minted to make the restore "work".
     */
    @Suppress("ReturnCount") // One early return per rejection reason IS the fail-closed structure.
    override fun restoreSession(request: RestoreFocusSessionRequest): FocusRestoreResult {
        val identity = request.identity
        val descriptor = identity.descriptor
        if (identity.replaySchemaVersion > ReproducibleSessionIdentity.REPLAY_SCHEMA_VERSION) {
            return FocusRestoreResult.Rejected.UnsupportedSchema
        }
        val canonicalizer = CanonicalizerVersion.fromId(identity.canonicalizerVersionId)
            ?: return FocusRestoreResult.Rejected.UnsupportedSchema
        val exerciseId = ExerciseId.entries.firstOrNull { it.name == descriptor.exerciseId }
            ?: return FocusRestoreResult.Rejected.UnknownExercise
        val generator = generators[exerciseId]
            ?: return FocusRestoreResult.Rejected.UnsupportedHistoricalImplementation
        val versions = FocusFormContract.versionsFor(exerciseId)
            ?: return FocusRestoreResult.Rejected.UnsupportedHistoricalImplementation
        if (generator.generatorVersion != versions.generatorVersion) {
            return FocusRestoreResult.Rejected.UnsupportedGeneratorVersion
        }
        val rngVersion = RngVersion.fromId(descriptor.rngVersionId)
            ?: return FocusRestoreResult.Rejected.UnsupportedRng
        versionDrift(descriptor, versions)?.let { return it }
        return rebuildAndCompare(
            identity, descriptor, exerciseId, generator, versions, rngVersion, canonicalizer, request.mode,
        )
    }

    @Suppress("LongParameterList", "ReturnCount") // Each parameter is already-validated state; each return is a gate.
    private fun rebuildAndCompare(
        identity: ReproducibleSessionIdentity,
        descriptor: FormDescriptorV1,
        exerciseId: ExerciseId,
        generator: VersionedTrialGenerator,
        versions: FocusFormVersions,
        rngVersion: RngVersion,
        canonicalizer: CanonicalizerVersion,
        mode: FocusSessionMode,
    ): FocusRestoreResult {
        val seed = SessionSeed(descriptor.seedHex.toULong(HEX_RADIX).toLong(), rngVersion)
        val difficulty = FocusSessionResolution.difficultyFrom(descriptor.resolvedDifficulty)
        val rebuilt = runCatching {
            descriptorFor(
                SessionInputs(
                    seed = seed,
                    exerciseId = exerciseId,
                    difficulty = difficulty,
                    sessionTemplateVersion = descriptor.sessionTemplateVersion,
                    plannedTrialCount = descriptor.plannedTrialCount,
                ),
                versions,
            )
        }.getOrElse { return FocusRestoreResult.Rejected.InvalidDescriptor }
        if (ReproducibleSessionIdentity.definitionIdOf(rebuilt, canonicalizer) != identity.formDefinitionId) {
            return FocusRestoreResult.Rejected.DefinitionDigestMismatch
        }
        // The spec is DERIVED from the identity, never taken from the caller: the trial count decides the form.
        val spec = BlockSpec(targetTrialCount = descriptor.plannedTrialCount, mode = Mode.TRAINING)
        val stimuli = generator.generate(difficulty, spec, seed)
        if (FocusPlanDigest.of(stimuli, canonicalizer) != identity.generatedPlanDigest) {
            return FocusRestoreResult.Rejected.PlanDigestMismatch
        }
        return FocusRestoreResult.Restored(planFor(exerciseId, identity, stimuli, mode))
    }

    private fun versionedGeneratorFor(exerciseId: ExerciseId): VersionedTrialGenerator =
        requireNotNull(generators[exerciseId]) {
            "$exerciseId is not wired to FocusEngine. An unversioned generator cannot name the algorithm its " +
                "form was drawn against, so the engine will not build a session for it. Implement " +
                "VersionedTrialGenerator and move it to FocusExerciseGenerators.versioned first."
        }

    /**
     * The generator's own declared version must equal the canonical one. The two are declared INDEPENDENTLY
     * ([FocusFormContract] vs the generator's own constant) precisely so this comparison is not vacuous.
     *
     * Honest limit, recorded rather than implied: this holds for `generatorVersion` ALONE. The contract's other
     * four versions have no second declaration to disagree with, so comparing them would be comparing a value
     * with itself. That is why `generatedPlanDigest` exists — it detects a changed form regardless of whether
     * any number was bumped.
     */
    private fun authorizedVersionsFor(
        exerciseId: ExerciseId,
        generator: VersionedTrialGenerator,
    ): FocusFormVersions {
        val versions = requireNotNull(FocusFormContract.versionsFor(exerciseId)) {
            "$exerciseId has no authorized versions in FocusFormContract — it is not wired to the engine."
        }
        require(generator.generatorVersion == versions.generatorVersion) {
            "$exerciseId generator is v${generator.generatorVersion} but FocusFormContract authorizes " +
                "v${versions.generatorVersion}. A version change means the same seed now yields a DIFFERENT " +
                "form, so it must fail closed and force a deliberate migration decision."
        }
        return versions
    }

    /** What the generator actually produced must match what the descriptor says it was asked for. */
    private fun requireWellFormed(stimuli: List<Stimulus>, plannedTrialCount: Int) {
        require(stimuli.size == plannedTrialCount) {
            "The generator produced ${stimuli.size} trials for a form declared as $plannedTrialCount, so the " +
                "identity would describe a form that was never presented."
        }
        require(stimuli.withIndex().all { (i, s) -> s.index == i }) {
            "Stimulus indices must be 0..n-1 in order, or the plan digest and the presented order disagree."
        }
    }

    private fun versionDrift(descriptor: FormDescriptorV1, versions: FocusFormVersions): FocusRestoreResult? {
        if (descriptor.generatorVersion != versions.generatorVersion) {
            return FocusRestoreResult.Rejected.UnsupportedGeneratorVersion
        }
        val drifted = descriptor.engineContractVersion != FocusFormContract.ENGINE_CONTRACT_VERSION ||
            descriptor.exerciseVersion != versions.exerciseVersion ||
            descriptor.difficultyMappingVersion != versions.difficultyMappingVersion ||
            descriptor.responseMappingVersion != versions.responseMappingVersion ||
            descriptor.contentVersion != versions.contentVersion
        return if (drifted) FocusRestoreResult.Rejected.VersionDrift else null
    }

    /** What a session was built from, before versions are resolved. */
    private data class SessionInputs(
        val seed: SessionSeed,
        val exerciseId: ExerciseId,
        val difficulty: DifficultyVector,
        val sessionTemplateVersion: Int,
        val plannedTrialCount: Int,
    )

    /** ONE set of inputs, used for both the identity and its digest — they cannot describe different forms. */
    private fun descriptorFor(inputs: SessionInputs, versions: FocusFormVersions): FormDescriptorV1 =
        FormDescriptorV1(
            seedHex = inputs.seed.value.toULong().toString(HEX_RADIX)
                .padStart(FormDescriptorV1.SEED_HEX_LENGTH, '0'),
            rngVersionId = inputs.seed.version.id,
            engineContractVersion = FocusFormContract.ENGINE_CONTRACT_VERSION,
            generatorVersion = versions.generatorVersion,
            exerciseId = inputs.exerciseId.name,
            exerciseVersion = versions.exerciseVersion,
            difficultyMappingVersion = versions.difficultyMappingVersion,
            resolvedDifficulty = FocusSessionResolution.difficultySnapshot(inputs.difficulty),
            sessionTemplateVersion = inputs.sessionTemplateVersion,
            plannedTrialCount = inputs.plannedTrialCount,
            resolvedTimingPolicy = FocusSessionResolution.timingPolicyFor(inputs.exerciseId, inputs.difficulty),
            responseMappingVersion = versions.responseMappingVersion,
            contentVersion = versions.contentVersion,
        )

    private fun planFor(
        exerciseId: ExerciseId,
        identity: ReproducibleSessionIdentity,
        stimuli: List<Stimulus>,
        mode: FocusSessionMode,
    ): FocusSessionPlan {
        val policy = FocusSessionResolution.policyFor(mode)
        return FocusSessionPlan(
            exerciseId = exerciseId,
            identity = identity,
            stimuli = stimuli,
            mode = mode,
            adaptation = policy.adaptation,
            measurementEligible = policy.measurementEligible,
            officialScoringEligible = policy.officialScoringEligible,
        )
    }

    private companion object {
        const val HEX_RADIX = 16
    }
}

/**
 * How a session's concrete parameters are resolved from its inputs.
 *
 * Split out of [DefaultFocusEngine] so the engine reads as the sequence of decisions it makes (resolve →
 * validate → mint → generate → digest) rather than as a pile of helpers. These are pure functions of their
 * arguments.
 */
internal object FocusSessionResolution {

    /** EFFECTIVE values, snapshotted and ordered — never a reference to a profile that can move underneath. */
    fun difficultySnapshot(difficulty: DifficultyVector): List<AxisValueV1> =
        DifficultyAxis.entries
            .map { axis -> AxisValueV1(axis = axis.name, value = difficulty.level(axis).value) }
            .sortedBy { it.axis }

    /**
     * Rebuilds the vector from a snapshot. An axis this build does not know is IGNORED rather than honoured: the
     * axis set is closed. The rebuilt descriptor then fails the digest comparison, which is the intended
     * outcome — a form we cannot rebuild exactly is a form we do not present.
     */
    fun difficultyFrom(snapshot: List<AxisValueV1>): DifficultyVector {
        val byAxis = snapshot.associate { it.axis to it.value }
        return DifficultyAxis.entries.fold(DifficultyVector()) { vector, axis ->
            byAxis[axis.name]?.let { vector.adjust(axis, AxisLevel(it)) } ?: vector
        }
    }

    /**
     * The timing actually resolved for this session. E2's window/ISI come from its difficulty mapping, so they
     * belong in the descriptor: two builds whose mapping differs would present the same items at different
     * speeds, which is a different form.
     */
    fun timingPolicyFor(exerciseId: ExerciseId, difficulty: DifficultyVector): List<TimingValueV1> =
        when (exerciseId) {
            ExerciseId.E2_FILTRO -> E2DifficultyMapping.map(difficulty).let {
                listOf(
                    TimingValueV1("isiMs", it.isiMs),
                    TimingValueV1("responseWindowMs", it.responseWindowMs),
                )
            }
            else -> emptyList()
        }.sortedBy { it.key }

    /**
     * What a mode permits. Exhaustive on purpose: a new mode cannot be added without deciding, here, whether it
     * feeds measurement or official scoring — the decision cannot be defaulted into existence.
     */
    fun policyFor(mode: FocusSessionMode): ModePolicy = when (mode) {
        FocusSessionMode.TRAINING_ONLY -> ModePolicy(
            adaptation = FocusAdaptationPolicy.FIXED,
            measurementEligible = false,
            officialScoringEligible = false,
        )
    }

    data class ModePolicy(
        val adaptation: FocusAdaptationPolicy,
        val measurementEligible: Boolean,
        val officialScoringEligible: Boolean,
    )
}
