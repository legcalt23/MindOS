package com.mindos.app.features.mindboost.core.bank

import com.mindos.app.features.mindboost.core.scoring.BankIntegrity
import kotlin.test.Test
import kotlin.test.assertEquals

/** B-1 · the single authority that derives runtime [BankIntegrity] from the validated registry. */
class BankIntegrityAuthorityTest {

    @Test
    fun unregisteredBank_isSmallUnvalidated_failClosed() {
        val integrity = BankIntegrityAuthority.integrityFor(BankRef("not-registered", registry = emptyList()))
        assertEquals(BankIntegrity.SMALL_UNVALIDATED, integrity)
    }

    @Test
    fun emptyRegistry_anyLookup_isSmallUnvalidated_noNullCrash() {
        // The production reality today: ProfileUpdatingBanks.all is empty → every lookup fails closed.
        assertEquals(BankIntegrity.SMALL_UNVALIDATED, BankIntegrityAuthority.integrityFor(BankRef("anything")))
    }

    @Test
    fun registeredButInvalidBank_isSmallUnvalidated() {
        val bank = BankTestFixtures.invalidBank("b", BankKind.STATIC_FIXED)
        assertEquals(BankIntegrity.SMALL_UNVALIDATED, BankIntegrityAuthority.integrityFor(BankRef("b", listOf(bank))))
    }

    @Test
    fun registeredValidStaticBank_isOk() {
        val bank = BankTestFixtures.validBank("b", BankKind.STATIC_FIXED)
        assertEquals(BankIntegrity.OK, BankIntegrityAuthority.integrityFor(BankRef("b", listOf(bank))))
    }

    @Test
    fun registeredValidGeneratorBank_isProcedural() {
        val bank = BankTestFixtures.validBank("b", BankKind.ALGORITHMIC_GENERATOR)
        assertEquals(BankIntegrity.PROCEDURAL, BankIntegrityAuthority.integrityFor(BankRef("b", listOf(bank))))
    }

    @Test
    fun neitherOkNorProcedural_leaks_whenBankIsUnregistered_regressionForTheBypass() {
        // The exact bypass B-1 closes: a caller that "wants" a non-blocking integrity but names an
        // unregistered bank gets SMALL_UNVALIDATED — it CANNOT self-promote to OK/PROCEDURAL.
        val integrity = BankIntegrityAuthority.integrityFor(BankRef("ex1-complex-span"))
        assertEquals(BankIntegrity.SMALL_UNVALIDATED, integrity, "unregistered memory generator must not feed profile")
    }
}
