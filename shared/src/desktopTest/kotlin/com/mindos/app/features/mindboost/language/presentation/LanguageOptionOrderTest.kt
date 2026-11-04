package com.mindos.app.features.mindboost.language.presentation

import com.mindos.app.features.mindboost.core.runtime.DeterministicRng
import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.language.domain.model.ResponseMode
import com.mindos.app.features.mindboost.language.domain.model.ResponsePayload
import com.mindos.app.features.mindboost.language.domain.scoring.AnswerKey
import com.mindos.app.features.mindboost.language.presentation.exercises.ArgumentXRayBank
import com.mindos.app.features.mindboost.language.presentation.exercises.CompressionLadderBank
import com.mindos.app.features.mindboost.language.presentation.exercises.EvidenceLockBank
import com.mindos.app.features.mindboost.language.presentation.exercises.ScopeNegationBank
import com.mindos.app.features.mindboost.language.presentation.exercises.SemanticSurgeryBank
import com.mindos.app.features.mindboost.language.presentation.exercises.SourceTriageBank
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * R7 · `MB-CATALOG-0010` / `MB-CATALOG-0012` — position must carry no information.
 *
 * Authored, four of the six Lenguaje banks put the correct answer in slot "b" in **every** item and a
 * fifth in five of six (only SourceTriage is balanced): 32 of 36 closed items, so "always tap the second
 * option" scored 89% without reading anything. The ordering items were worse — they were handed to the
 * screen in exactly their key order, so tapping top to bottom was a perfect score.
 *
 * Rebalancing the content would fix today's items and nothing else — the next authored item can
 * re-degenerate it silently. Shuffling at serve time makes position structurally uninformative
 * regardless of how a bank is written, which is why the guard lives here and not in the content.
 */
class LanguageOptionOrderTest {

    @Test
    fun theAuthoredBanksArePositionallyDegenerate_discriminantHalf() {
        // Anti-vacuity: if this fails, the metric below is measuring nothing. It documents the defect as
        // authored — and it must keep failing the balance bar the served items are held to.
        val authoredSlots = allClosedItems().map { it.correctSlot() }
        val worstSlotShare = authoredSlots.groupingBy { it }.eachCount().values.max().toDouble() / authoredSlots.size

        assertTrue(
            worstSlotShare > 0.80,
            "the authored banks are supposed to be degenerate (that is the finding); worst slot share=$worstSlotShare",
        )
    }

    @Test
    fun servedItemsDoNotPinTheAnswerToOneSlot() {
        // The chi-square below has df = options - 1; assert the shape it was written for instead of
        // silently testing the wrong distribution if a bank ever ships a four-option item.
        assertEquals(setOf(THREE_OPTIONS), allClosedItems().map { it.options.size }.toSet())

        val slotCounts = IntArray(MAX_OPTIONS)
        var total = 0
        repeat(SEEDS) { seed ->
            val rng = rngFor(seed.toLong())
            ItemPresentation.shuffleOptions(allClosedItems(), rng).forEach { item ->
                slotCounts[item.correctSlot()]++
                total++
            }
        }

        // Every slot must be used, and the spread must survive a chi-square goodness-of-fit test against
        // uniform (df = 2, alpha = 0.001). Deterministic RNG + fixed seeds, so this cannot flake.
        val expected = total.toDouble() / THREE_OPTIONS
        val chiSquare = (0 until THREE_OPTIONS).sumOf { slot ->
            val diff = slotCounts[slot] - expected
            diff * diff / expected
        }

        assertTrue(slotCounts.take(THREE_OPTIONS).all { it > 0 }, "some slot never held the answer: ${slotCounts.toList()}")
        assertTrue(chiSquare < CHI_SQUARE_DF2_ALPHA_001, "served answer positions are not uniform: chi2=$chiSquare")
    }

    @Test
    fun orderingItemsAreNotHandedToTheScreenInTheirKeyOrder() {
        val orderingItems = LanguageDemoBank.items.filter { it.responseMode == ResponseMode.ORDERING }
        assertTrue(orderingItems.isNotEmpty(), "no ordering item to check — has the bank changed?")

        val presentedInKeyOrder = (0 until SEEDS).count { seed ->
            val rng = rngFor(seed.toLong())
            ItemPresentation.shuffleOptions(orderingItems, rng).any { item ->
                val key = item.key as AnswerKey.Ordering
                item.options.map { it.id } == key.correctOrder
            }
        }

        // With 3 options, 1 of 6 permutations is the answer; "tap top to bottom" must stop being a
        // strategy, not become impossible. Anything near SEEDS means the order is still being handed over.
        assertTrue(
            presentedInKeyOrder < SEEDS / 2,
            "the key order is still being presented $presentedInKeyOrder/$SEEDS times",
        )
    }

    @Test
    fun shufflingChangesOnlyTheOrder() {
        val rng = rngFor(7L)
        val original = allClosedItems()
        val presented = ItemPresentation.shuffleOptions(original, rng)

        assertEquals(original.size, presented.size)
        original.zip(presented).forEach { (before, after) ->
            assertEquals(before.taskId, after.taskId)
            assertEquals(before.key, after.key, "the answer key must never be rewritten by presentation")
            assertEquals(before.prompt, after.prompt)
            assertEquals(before.evidence, after.evidence, "evidence spans keep their passage order")
            assertEquals(before.options.toSet(), after.options.toSet(), "no option may be added, dropped or edited")
        }
    }

    @Test
    fun presentationIsDeterministicForASeed() {
        val first = ItemPresentation.shuffleOptions(allClosedItems(), rngFor(42L)).map { it.options.map { o -> o.id } }
        val second = ItemPresentation.shuffleOptions(allClosedItems(), rngFor(42L)).map { it.options.map { o -> o.id } }
        val other = ItemPresentation.shuffleOptions(allClosedItems(), rngFor(43L)).map { it.options.map { o -> o.id } }

        assertEquals(first, second, "same seed must replay the same presentation")
        assertTrue(first != other, "different seeds must not produce the same presentation")
    }

    @Test
    fun theHolderServesShuffledOptions() {
        // The pure function is worthless if the live serve path does not call it.
        val holder = LanguageSessionStateHolder(userId = "u", seedSource = fixedSeed(11L))
        val servedFirstItem = holder.state.value.item

        val authoredFirstItem = LanguageDemoBank.items.first()
        assertEquals(authoredFirstItem.taskId, servedFirstItem?.taskId)
        val anyReordered = LanguageDemoBank.items.zip(holderServedItems(fixedSeed(11L)))
            .any { (authored, served) -> authored.options.map { it.id } != served.options.map { it.id } }
        assertTrue(anyReordered, "the holder served every item in its authored option order")
    }

    private fun holderServedItems(seedSource: SeedSource): List<DemoItem> {
        val holder = LanguageSessionStateHolder(userId = "u", seedSource = seedSource)
        return buildList {
            repeat(LanguageDemoBank.items.size) { index ->
                holder.state.value.item?.let { add(it) }
                if (index < LanguageDemoBank.items.size - 1) {
                    holder.submit(firstResponseFor(holder.state.value.item))
                    holder.next()
                }
            }
        }
    }

    private fun firstResponseFor(item: DemoItem?) = when (item?.responseMode) {
        ResponseMode.ORDERING -> ResponsePayload.Ordering(item.options.map { it.id })
        ResponseMode.MULTIPLE_CHOICE_WITH_EVIDENCE_SELECTION -> ResponsePayload.EvidenceSelection(item.options.first().id, emptyList())
        else -> ResponsePayload.MultipleChoice(item?.options?.first()?.id.orEmpty())
    }

    /** The correct option's slot, for items whose key names a single correct option. */
    private fun DemoItem.correctSlot(): Int {
        val correctId = when (val k = key) {
            is AnswerKey.SingleChoice -> k.correctOptionId
            is AnswerKey.EvidenceSelection -> k.correctOptionId
            else -> error("only single-answer items have a correct slot")
        }
        return options.indexOfFirst { it.id == correctId }
    }

    private fun allClosedItems(): List<DemoItem> = listOf(
        EvidenceLockBank.items,
        SemanticSurgeryBank.items,
        CompressionLadderBank.items,
        ArgumentXRayBank.items,
        ScopeNegationBank.items,
        SourceTriageBank.items,
    ).flatten().filter { it.responseMode != ResponseMode.ORDERING }

    private fun rngFor(seed: Long) = DeterministicRng.forSeed(SessionSeed(seed, RngVersion.V1))

    private fun fixedSeed(seed: Long) = SeedSource { SessionSeed(seed, RngVersion.V1) }

    private companion object {
        const val SEEDS = 200
        const val MAX_OPTIONS = 8
        const val THREE_OPTIONS = 3
        const val CHI_SQUARE_DF2_ALPHA_001 = 13.816
    }
}
