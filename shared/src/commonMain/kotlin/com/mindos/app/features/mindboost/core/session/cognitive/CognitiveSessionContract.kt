package com.mindos.app.features.mindboost.core.session.cognitive

/**
 * SYSTEM-1 · The cognitive-session data-contract authority. Documents IN CODE — not only in a report — which
 * fields are Not Applicable to a [CognitiveSessionRun] BY CHARTER, so a future change to add one is a conscious
 * charter decision, not an accident. A test pins this set. Mirrors the Emociones session contract; the
 * exclusions here are the GLOBAL charter ones (a raw/delta/XP economy is the removed "loaded gun"),
 * paradigm-specific exclusions are not listed here.
 */
object CognitiveSessionContract {
    /**
     * Fields Not Applicable BY CHARTER — never faked, never added without a charter decision:
     *  - rawScore / scoreDelta / xpEarned: the honest currency is [CognitiveSessionScore.normalized]
     *    (0..1, `null != 0`); a raw / delta / XP economy would re-introduce the removed "loaded gun".
     */
    val NOT_APPLICABLE_BY_CHARTER: Set<String> = setOf(
        "rawScore",
        "scoreDelta",
        "xpEarned",
    )
}
