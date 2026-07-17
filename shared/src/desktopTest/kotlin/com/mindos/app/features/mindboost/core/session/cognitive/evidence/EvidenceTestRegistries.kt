package com.mindos.app.features.mindboost.core.session.cognitive.evidence

/**
 * SYSTEM-1 6R3 (§18 C4) · Shared desktopTest fixture: a registry that treats EVERY (exerciseId, exerciseVersion)
 * as [RtCriticality.RT_NON_CRITICAL], for the many pre-existing evidence-contract tests (scale-key grouping,
 * dedup, conservation, temporal handling, adversarial cases, performance benchmarks) that predate the fail-closed
 * RT-criticality gate and are not themselves about RT-criticality/timebase.
 * [com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunFixtures.validRun]'s default
 * (exerciseId="e8", exerciseVersion="1.0.0") does not match [RtCriticalityRegistry.PRODUCTION_DECLARED]'s only
 * real entry ("E8"/"1.0.0-mc9d") — case AND version both differ — so without this override every one of those
 * tests would start failing closed via [ExclusionReason.RT_CRITICALITY_UNKNOWN] for a reason unrelated to what
 * they actually test. Tests that DO exercise the RT-criticality gate itself construct their OWN bespoke
 * [RtCriticalityRegistry] instead of using this fixture.
 */
internal object EvidenceTestRegistries {
    val PERMISSIVE: RtCriticalityRegistry = RtCriticalityRegistry(
        declared = emptyList(),
        fallback = RtCriticality.RT_NON_CRITICAL,
    )
}
