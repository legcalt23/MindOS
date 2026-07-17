package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus

/**
 * A generator whose form is reproducible: the seed carries the algorithm it was drawn against, and the generator
 * names the version of the form it produces.
 *
 * ## Why this does NOT extend [TrialGenerator]
 *
 * It used to, and that made the barrier decorative. The sequence is worth keeping, because each fix addressed
 * only the level the previous one had made visible:
 *
 * 1. `@Deprecated` on [TrialGenerator.generate] was not a barrier: deprecation is checked against the STATIC
 *    type, and the live caller resolves its generator as a [TrialGenerator], which is not deprecated. E2's own
 *    annotation was invisible at the only call site that mattered.
 * 2. A source scanner was not a barrier either: it gates on files that name `E2FiltroGenerator`, and a
 *    polymorphic caller never does. Its own test documented that blind spot — as hypothetical.
 * 3. This interface was then introduced as "a TYPE the caller can see". But as `VersionedTrialGenerator :
 *    TrialGenerator`, E2 still WAS a `TrialGenerator`, so the unversioned overload stayed callable on it. The
 *    type let the engine *select* migrated exercises; it did nothing to *prevent* the legacy path on one. An
 *    audit then found the blind spot was never hypothetical: `FocusFixedBlockRunner` called the bare-`Long`
 *    overload on E2 through two live production routes — baseline (`Mode.CALIBRATION`) and the evaluation probe
 *    — i.e. the two measurement-relevant routes, from a wall-clock seed that is never persisted.
 *
 * So the split is the point. E2 is not a [TrialGenerator], therefore no `generate(difficulty, spec, seed: Long)`
 * call can dispatch to it — not from the runner, not polymorphically, not from a caller written next year. The
 * barrier is the type system rather than an annotation, a scanner, or a reviewer noticing.
 * `E2HasNoLegacyRouteTest` asserts that property directly instead of grepping for a name.
 *
 * The twelve unmigrated exercises stay on plain [TrialGenerator] with their unversioned path until each is
 * migrated deliberately. That is a declared ledger, not a pretence that they are done.
 */
interface VersionedTrialGenerator {

    val exerciseId: ExerciseId

    /**
     * The version of the form this generator produces. Bump on ANY change to the drawn sequence: the same seed
     * then yields a different form, so the two are not comparable and the form's identity must keep them apart.
     *
     * Declared here AND in `FocusFormContract`, independently, so that `DefaultFocusEngine` comparing the two is
     * a real check rather than a value compared with itself. The honest limit: of the five versions the contract
     * declares, only this one has a second declaration to disagree with — the other four are single-sourced, and
     * an unbumped `contentVersion` is caught by no check. That is why `generatedPlanDigest` exists: it detects a
     * changed form regardless of whether any number was bumped.
     */
    val generatorVersion: Int

    /**
     * The versioned path. Unlike [TrialGenerator.generate], the seed names its own algorithm, so the form is
     * reproducible across Kotlin versions rather than silently becoming a different form on a stdlib change.
     */
    fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: SessionSeed): List<Stimulus>
}
