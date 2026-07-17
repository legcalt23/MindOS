package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * SYSTEM-1 6R3 (§18 C4) · [RtCriticalityRegistry] is the single, injectable, fail-closed authority for an
 * exercise's [RtCriticality]. The catalog-pin test fixes the EXACT production set as an expectation: a new
 * exercise/version added without updating this pin (and this test) silently degrades to
 * [RtCriticality.UNKNOWN] — changing the pin is a deliberate act requiring a psychometrician-approved dossier
 * entry (see [RtCriticalityRegistry]'s own KDoc for the honest reason only ONE entry exists today).
 */
class RtCriticalityRegistryTest {

    @Test
    fun the_production_catalog_pin_is_exactly_one_sourced_entry_today() {
        assertEquals(
            listOf(RtCriticalityDeclaration("E8", "1.0.0-mc9d", RtCriticality.RT_CRITICAL)),
            RtCriticalityRegistry.PRODUCTION_DECLARED,
            "the production registry must declare EXACTLY the one real, sourced (exerciseId, exerciseVersion) " +
                "pair documented on RtCriticalityRegistry — adding an entry here is a deliberate, dossier-backed " +
                "change, never a silent default; the code-cited file:line sources live in that KDoc.",
        )
    }

    @Test
    fun a_production_registry_resolves_the_declared_entry_to_rt_critical() {
        val registry = RtCriticalityRegistry()
        assertEquals(RtCriticality.RT_CRITICAL, registry.criticalityOf("E8", "1.0.0-mc9d"))
    }

    @Test
    fun a_production_registry_fails_closed_to_unknown_for_an_undeclared_pair() {
        val registry = RtCriticalityRegistry()
        assertEquals(RtCriticality.UNKNOWN, registry.criticalityOf("E1", "1.0.0"))
        assertEquals(RtCriticality.UNKNOWN, registry.criticalityOf("E8", "1.0.0"), "same id, wrong version => UNKNOWN")
        assertEquals(RtCriticality.UNKNOWN, registry.criticalityOf("e8", "1.0.0-mc9d"), "case-sensitive id => UNKNOWN")
    }

    @Test
    fun a_test_seam_registry_can_override_both_declared_entries_and_the_fallback() {
        val allNonCritical = RtCriticalityRegistry(declared = emptyList(), fallback = RtCriticality.RT_NON_CRITICAL)
        assertEquals(RtCriticality.RT_NON_CRITICAL, allNonCritical.criticalityOf("anything", "anywhere"))

        val bespoke = RtCriticalityRegistry(
            declared = listOf(RtCriticalityDeclaration("fixture-ex", "1.0", RtCriticality.RT_CRITICAL)),
            fallback = RtCriticality.RT_NON_CRITICAL,
        )
        assertEquals(RtCriticality.RT_CRITICAL, bespoke.criticalityOf("fixture-ex", "1.0"))
        assertEquals(RtCriticality.RT_NON_CRITICAL, bespoke.criticalityOf("other-ex", "1.0"))
    }
}
