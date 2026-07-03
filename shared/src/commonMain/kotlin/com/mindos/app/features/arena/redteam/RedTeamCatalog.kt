@file:Suppress("MaxLineLength") // Dense one-row-per-threat documentation table; wrapping each row hurts readability.

package com.mindos.app.features.arena.redteam

/** Phase 11: Whether a threat's mitigation is verified offline or its subsystem is out of scope. */
enum class ThreatStatus {
    MITIGATED,
    NOT_MODELED,
}

/**
 * Phase 11: A documented red-team threat with its mapped offline mitigation and verification test.
 * Synthetic / documentation only — no production threat response.
 */
data class RedTeamThreat(
    val id: String,
    val attack: String,
    val prevention: String,
    val expectedOutcome: String,
    val mappedTest: String,
    val status: ThreatStatus = ThreatStatus.MITIGATED,
)

/**
 * Phase 11: The 27-threat red-team pack (RT_01..RT_27), each mapped to the offline defense that
 * mitigates it and the simulation test that verifies it (exit criterion: "all 27 threats documented
 * with simulation test mappings"). Two threats touch subsystems not modeled offline (RT_19 protected
 * item pool, RT_21 ops review queue) and are flagged NOT_MODELED rather than claimed mitigated.
 */
object RedTeamCatalog {

    val THREATS: List<RedTeamThreat> = listOf(
        RedTeamThreat("RT_01", "New account stomps low-rated players", "Sigma kept high + smurf calibration queue", "Isolated, sigma not collapsed", "SIM_04"),
        RedTeamThreat("RT_02", "Stronger player boosts an account", "Boosting signals -> integrity HOLD", "Rejected (HOLD)", "SIM_05"),
        RedTeamThreat("RT_03", "Coordinated rating transfer", "Collusion signals -> INVALIDATED", "Rejected (INVALIDATED)", "SIM_06"),
        RedTeamThreat("RT_04", "Win-trading pairs", "Collusion signals -> INVALIDATED", "Rejected (INVALIDATED)", "SIM_06"),
        RedTeamThreat("RT_05", "Shared account high-skill play", "Account-sharing signals -> integrity HOLD", "Rejected (HOLD)", "SIM_07"),
        RedTeamThreat("RT_06", "Friends snipe same queue window", "Same-motor + hard boundaries + timeout", "Match constrained / timeout", "MatchmakingEngineTest"),
        RedTeamThreat("RT_07", "Lag spike during loss", "Jitter -> NOT_COMPARABLE", "Rejected (NOT_COMPARABLE)", "SIM_08"),
        RedTeamThreat("RT_08", "Faster device advantage", "Hardware-class segmentation + latency tiering", "Speed segmented by hardware", "SIM_09"),
        RedTeamThreat("RT_09", "Language group mismatch", "Language is not a rating input", "No rating effect", "SIM_10"),
        RedTeamThreat("RT_10", "Accessibility-mode comparison", "Accessibility comparability tier", "Gated by comparability", "SIM_16"),
        RedTeamThreat("RT_11", "Grinding protected items", "Insufficient-confidence gate", "No rating gain", "SIM_24"),
        RedTeamThreat("RT_12", "External logic assistance", "Latency/cadence -> boosting HOLD", "Rejected (HOLD)", "SIM_05"),
        RedTeamThreat("RT_13", "Automated task solver", "Latency/cadence -> boosting HOLD", "Rejected (HOLD)", "SIM_05"),
        RedTeamThreat("RT_14", "Falsely flagged legit player", "Human review + false-positive restore", "Rating restored", "SIM_14"),
        RedTeamThreat("RT_15", "Abuse not detected", "Multi-signal detection coverage", "Detection rate gated", "IntegrityAbuseSimTest"),
        RedTeamThreat("RT_16", "Rollback exploited", "Rollback requires human review + exact inversion", "Human-reviewed, consistent", "SIM_15"),
        RedTeamThreat("RT_17", "Lose deliberately to lower rank", "Non-compensatory rank; mu not punitively dropped", "Badge only, no elite gain", "SIM_22"),
        RedTeamThreat("RT_18", "Repeated low-quality trials", "Insufficient-confidence gate", "No rating gain", "SIM_24"),
        RedTeamThreat("RT_19", "Protected item pool leakage", "Item-exposure subsystem not modeled offline", "Out of offline scope", "NOT_MODELED", ThreatStatus.NOT_MODELED),
        RedTeamThreat("RT_20", "Premium buys rating advantage", "Premium absent from rating math", "No advantage (equivalence)", "SIM_25"),
        RedTeamThreat("RT_21", "Mass false reports flood Ops", "Ops auto-triage not modeled offline", "Out of offline scope", "NOT_MODELED", ThreatStatus.NOT_MODELED),
        RedTeamThreat("RT_22", "Spam appeals to overwhelm Ops", "Appeal rate-limit + window", "Appeals blocked", "PostReviewResolverTest"),
        RedTeamThreat("RT_23", "Manipulate comparability to dodge rating", "NOT_COMPARABLE rejected", "Rejected (NOT_COMPARABLE)", "SIM_16"),
        RedTeamThreat("RT_24", "Exploit partial comparability", "Reduced update multiplier", "Reduced rating update", "SIM_17"),
        RedTeamThreat("RT_25", "Abuse low population for easy matches", "Low-population fallback cancels", "Match cancelled, fairness preserved", "QueueRouterTest"),
        RedTeamThreat("RT_26", "Farm specialist badges for rank", "Badge does not elevate global rank", "No rank elevation", "SIM_21"),
        RedTeamThreat("RT_27", "Exploit one strong block for elite", "Non-compensatory rank; weak block blocks elite", "Elite blocked", "SIM_22"),
    )
}
