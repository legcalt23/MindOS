package com.mindos.app.features.mindboost.focus.domain.engine

import com.mindos.app.features.mindboost.core.canonical.CanonicalEncoder
import com.mindos.app.features.mindboost.core.canonical.CanonicalizerVersion
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import kotlinx.serialization.builtins.ListSerializer

/**
 * Digest of the stimulus sequence an engine ACTUALLY generated — the output, not the inputs.
 *
 * ## Why the input digest was not enough
 *
 * `ReproducibleSessionIdentity.formDefinitionId` digests the descriptor: seed, versions, resolved difficulty,
 * trial count. It answers *"were the inputs the same?"*. It is blind by construction to the one failure it most
 * needs to catch: someone edits a generator's draw logic and does not bump `generatorVersion`. In exactly that
 * case the inputs are identical, so the definition digest matches, so every check passes — and the session
 * silently presents a different form than the one its identity claims. The only thing standing between that and
 * production was a developer remembering to bump a number, which is not a control.
 *
 * This digest closes it: it covers the generated output, so a changed generator changes the digest whether or not
 * anyone bumped anything. `E2GeneratorDriftTest` proves it by mutating the generator's output without touching
 * its version and asserting the digest moves.
 *
 * ## Why it replaced `FocusFormFingerprint`
 *
 * The deleted fingerprint canonicalized stimuli with a hand-written `when` naming each field. It had two lives:
 * it shipped with an `else` branch that collapsed distinct payloads to one string (found by a diff audit), and
 * after that was "fixed" by making the `when` total over the 13 payload TYPES it was still omitting five FIELDS —
 * `Vigilance.lure`, `Vigilance.isSignal`, `MultiObjectTracking.targetCount`, `TaskSwitch.correctResponse`,
 * `DualTask.go` — because the compiler enforces exhaustive types and nothing enforces exhaustive fields. Two of
 * those were not masked by any other digested field, so two genuinely different forms digested identically.
 *
 * The replacement does not name fields at all: it digests via `Stimulus.serializer()`, so the field list is the
 * type. It also had zero production call-sites, which is why none of this was load-bearing until now.
 */
internal object FocusPlanDigest {

    /** Under [CanonicalizerVersion.V1]. The version travels in the identity, so an old plan is re-digested its way. */
    fun of(stimuli: List<Stimulus>, version: CanonicalizerVersion = CanonicalizerVersion.V1): String =
        CanonicalEncoder.digest(version, ListSerializer(Stimulus.serializer()), stimuli)
}
