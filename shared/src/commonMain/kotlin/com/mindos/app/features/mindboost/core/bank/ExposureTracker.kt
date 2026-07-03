@file:Suppress("MagicNumber") // 0.0 baseline for the empty-set fraction is a domain neutral.

package com.mindos.app.features.mindboost.core.bank

/**
 * INFRA-04 · Per-user item exposure tracking + holdout-exhaustion detection. In-memory here (a durable
 * layer reuses `SecureKeyValueStore` when wired). Feeds anti-gaming re-exposure detectors (D5/D7) and the
 * bank `HOLDOUT_EXHAUSTED` state.
 */
class ExposureTracker {
    private val served: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()

    fun recordServed(userId: String, itemId: String) {
        val perItem = served.getOrPut(userId) { mutableMapOf() }
        perItem[itemId] = (perItem[itemId] ?: 0) + 1
    }

    fun exposureCount(userId: String, itemId: String): Int = served[userId]?.get(itemId) ?: 0

    /** Fraction of [itemIds] this user has seen at least once. */
    fun servedFraction(userId: String, itemIds: Collection<String>): Double {
        if (itemIds.isEmpty()) return 0.0
        return itemIds.count { exposureCount(userId, it) > 0 }.toDouble() / itemIds.size
    }

    /** True once EVERY holdout item has been served (pool exhausted → rotate / HOLDOUT_EXHAUSTED). */
    fun isHoldoutExhausted(userId: String, holdoutItemIds: Collection<String>): Boolean =
        holdoutItemIds.isNotEmpty() && holdoutItemIds.all { exposureCount(userId, it) > 0 }
}

/**
 * INFRA-04 · Stateless rotation policy. Serves TRAINING items ONLY (NEVER a holdout item in practice) and
 * prefers the least-exposed item, so practice never leaks the holdout and never fixes a memorizable order.
 */
object BankRotation {
    fun selectNextTraining(bank: ItemBank, tracker: ExposureTracker, userId: String): BankItem? =
        bank.items
            .filter { it.partition == ItemPartition.TRAINING }
            .minByOrNull { tracker.exposureCount(userId, it.id) }
}
