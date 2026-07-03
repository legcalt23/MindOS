package com.mindos.app.features.mindboost.core.bank

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * INFRA-04 · the build gate. Runs under `:shared:desktopTest` (part of `check`), so a profile-updating
 * exercise can never ship an invalid bank. The registry is empty today (no exercise is profile-updating),
 * so this passes vacuously; [BankValidatorTest] proves the rejection logic that protects future entries.
 */
class BankValidationGateTest {

    @Test
    fun everyProfileUpdatingBank_passesValidation() {
        for (bank in ProfileUpdatingBanks.all) {
            assertTrue(bank.profileUpdating, "${bank.bankId} is registered but not flagged profileUpdating")
            val result = BankValidator.validate(bank)
            assertTrue(result.isValid, "Profile-updating bank '${bank.bankId}' is invalid: ${result.violations}")
        }
    }

    @Test
    fun registry_onlyContainsProfileUpdatingBanks() {
        assertTrue(ProfileUpdatingBanks.all.all { it.profileUpdating })
    }
}
