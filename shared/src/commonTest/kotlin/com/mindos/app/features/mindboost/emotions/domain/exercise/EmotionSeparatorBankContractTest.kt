@file:Suppress("MagicNumber") // The bank sizes and the chi-square limit ARE the contract under test.

package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.core.bank.BankValidator
import com.mindos.app.features.mindboost.core.bank.BankViolation
import com.mindos.app.features.mindboost.core.bank.ItemPartition
import com.mindos.app.features.mindboost.core.bank.ProfileUpdatingBanks
import com.mindos.app.features.mindboost.core.scoring.BankIntegrity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ola 4 · P1, revised in Ola 2 · R3 · P2 — pins the EM3/EM10 bank against the repo's ONE bank contract
 * (INFRA-04) and the honest consequences of routing it there. One of those consequences changed: the bank
 * used to be measured TOO SMALL and now clears its designed size. The other did not — the ground truth is
 * still single-rater — and it is asserted separately so closing the first gap cannot be read as closing both.
 */
class EmotionSeparatorBankContractTest {

    private val bank = EmotionSeparatorBank

    @Test
    fun every_eval_item_is_a_holdout_partition_and_the_rest_are_training() {
        val holdout = bank.items.filter { it.partition == ItemPartition.HOLDOUT }.map { it.id }.toSet()
        val training = bank.items.filter { it.partition == ItemPartition.TRAINING }.map { it.id }.toSet()
        val expectedHoldout = SeparatorItemFactory.ALL.filter { it.isEval }.map { it.phrase }.toSet()

        assertEquals(expectedHoldout, holdout, "isEval must map 1:1 to ItemPartition.HOLDOUT")
        assertEquals(SeparatorItemFactory.ALL.size, holdout.size + training.size, "no item may be dropped")
        assertTrue(holdout.intersect(training).isEmpty(), "an item cannot be both trained on and held out")
        assertTrue(bank.items.none { it.partition == ItemPartition.UNTRAINED }, "this bank declares no UNTRAINED split")
    }

    @Test
    fun item_ids_are_stable_phrases_and_unique() {
        // The id is the phrase on purpose: exposure history is keyed by phrase, so a positional id would
        // silently re-point every stored exposure the first time an item is inserted mid-list.
        assertEquals(SeparatorItemFactory.ALL.map { it.phrase }.toSet(), bank.items.map { it.id }.toSet())
        assertEquals(bank.items.size, bank.items.map { it.id }.toSet().size, "duplicate bank item id")
    }

    @Test
    fun correct_answer_positions_are_balanced_so_tapping_one_target_cannot_win() {
        val positions = bank.items.mapNotNull { it.correctAnswerPosition }
        assertEquals(bank.items.size, positions.size, "every classification item has a correct target position")
        val chi = BankValidator.chiSquareUniform(positions, EmotionSeparatorBank.CATEGORY_COUNT)
        assertTrue(
            chi <= bank.requirements.maxPositionChiSquare,
            "correct-target distribution is imbalanced (chi=$chi): always tapping one target would pay off",
        )
    }

    @Test
    fun the_bank_now_meets_the_size_its_own_design_asks_for() {
        // Ola 2 · R3 · P2 inverted this test. It used to pin the shortfall — `TooSmall(40, …)` — because the
        // authored bank was the whole bank and the ceiling had to stay visible rather than be explained away.
        // The compositional extension actually closed it, so the assertion now states the fact that replaced it.
        // It still fails if the bank shrinks back, which is the property that mattered in both directions.
        val result = BankValidator.validate(bank)

        assertTrue(
            bank.items.size >= EmotionSeparatorBank.DESIGN_MIN_BANK_SIZE,
            "bank holds ${bank.items.size}, design floor is ${EmotionSeparatorBank.DESIGN_MIN_BANK_SIZE}",
        )
        assertTrue(
            result.violations.none { it is BankViolation.TooSmall },
            "size shortfall reported for a bank of ${bank.items.size}: ${result.violations}",
        )
        assertFalse(
            result.violations.any { it is BankViolation.NoHoldoutServed },
            "the held-out partition is declared, so this violation must not fire",
        )
    }

    @Test
    fun the_single_rater_ground_truth_stays_declared_and_unmeasured() {
        // The size gap closed; this one did not. Every phrase is still labelled by one author, so the bank
        // cannot claim a verified ground truth and must not quietly start looking like it does just because it
        // now passes the size check.
        assertTrue(bank.items.all { it.interRaterAgreement == null }, "an invented agreement figure is not evidence")
        assertFalse(
            bank.requirements.requiresInterRater,
            "the bank must not claim a bar it has never taken",
        )
        assertTrue(bank.items.all { it.culturalLoad == null }, "cultural load is unmeasured, not zero")
    }

    @Test
    fun integrity_fails_closed_and_blocks_any_profile_contribution() {
        assertEquals(
            BankIntegrity.SMALL_UNVALIDATED,
            bank.integrity,
            "unregistered content must derive the blocking integrity, never a friendly literal",
        )
        assertTrue(bank.integrity.blocksProfileEligibility(), "this content cannot back a profile reading")
    }

    @Test
    fun the_bank_is_not_registered_as_profile_updating() {
        // Negative barrier: registering a bank is an explicit product + efficacy decision. If a future change
        // adds this one to the registry, the build-time bank gate would start judging it as profile-updating
        // content while its ground truth is still single-rater. Fail here first.
        assertFalse(bank.profileUpdating, "Emociones contributes no cognitive-profile score")
        assertTrue(
            ProfileUpdatingBanks.all.none { it.bankId == EmotionSeparatorBank.BANK_ID },
            "the separator bank must not be registered as profile-updating",
        )
    }
}
