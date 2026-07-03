package com.mindos.app.features.mindboost.core.bank

/**
 * INFRA-04 · Registry of banks backing a PROFILE-UPDATING exercise. The build gate
 * (`BankValidationGateTest`, run under `:shared:desktopTest` → part of `check`) validates EVERY entry, so
 * a profile-updating exercise can never ship an invalid bank.
 *
 * EMPTY today by design: no exercise is profile-updating yet, so the existing fixed banks (Logic 6,
 * Language 36, Emotions, Creativity 14) are NOT listed here — i.e. they are explicitly non-profile. An
 * exercise registers its bank here when (and only when) it turns on profile-update — a per-exercise P1
 * task that must migrate the bank to satisfy [BankValidator] first.
 */
object ProfileUpdatingBanks {
    val all: List<ItemBank> = emptyList()
}
