package com.mindos.app.features.mindboost.emotions.domain.safety

/**
 * Live safety state the adaptive engine reads. [escalationLocked] forbids any step-up while overload
 * proxies fired this block OR the previous one — a resolved-low check-in clears re-prompting (anti-nag)
 * but never clears this lock while proxies are active.
 */
data class EmotionSafetyState(
    val firedProxies: Set<OverloadProxy> = emptySet(),
    val previousFiredProxies: Set<OverloadProxy> = emptySet(),
    val checkInResolvedThisBlock: Boolean = false,
) {
    val escalationLocked: Boolean
        get() = firedProxies.isNotEmpty() || previousFiredProxies.isNotEmpty()
}
