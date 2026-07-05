package com.mindos.app.features.mindboost.core.bank

import com.mindos.app.features.mindboost.core.scoring.BankIntegrity

/**
 * Identifies the profile-updating bank a source claims ([bankId]) and the registry to resolve it against.
 * Production uses the real [ProfileUpdatingBanks] registry; tests inject a registry to exercise the
 * registered/valid paths. Carrying the registry here (not a `BankIntegrity`) is what stops a source from
 * hardcoding its own maturity.
 */
data class BankRef(val bankId: String, val registry: List<ItemBank> = ProfileUpdatingBanks.all)

/**
 * INFRA-04 · B-1 · The SINGLE authority that derives a runtime [BankIntegrity] for a profile-updating
 * source, closing the bypass where an adapter could hardcode a non-blocking literal.
 *
 * A source names its bank ([BankRef.bankId]); the authority looks it up in the registry, validates it with
 * [BankValidator], and ONLY a REGISTERED + VALID bank yields a non-blocking integrity:
 *  - registered + valid + [BankKind.STATIC_FIXED] → [BankIntegrity.OK];
 *  - registered + valid + [BankKind.ALGORITHMIC_GENERATOR] → [BankIntegrity.PROCEDURAL].
 *
 * Everything else is FAIL-CLOSED [BankIntegrity.SMALL_UNVALIDATED] (blocks the profile): an unregistered
 * bank, an invalid bank, or an empty registry. No adapter can decide its own maturity with a local literal.
 */
object BankIntegrityAuthority {

    fun integrityFor(ref: BankRef): BankIntegrity {
        val bank = ref.registry.firstOrNull { it.bankId == ref.bankId }
        return when {
            bank == null -> BankIntegrity.SMALL_UNVALIDATED
            !BankValidator.validate(bank).isValid -> BankIntegrity.SMALL_UNVALIDATED
            bank.kind == BankKind.STATIC_FIXED -> BankIntegrity.OK
            else -> BankIntegrity.PROCEDURAL
        }
    }
}
