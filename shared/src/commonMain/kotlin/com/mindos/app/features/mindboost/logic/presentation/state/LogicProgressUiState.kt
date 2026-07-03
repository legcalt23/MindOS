package com.mindos.app.features.mindboost.logic.presentation.state

import com.mindos.app.features.mindboost.logic.domain.model.LogicSkill

/** Read-only progress view for Lógica: totals, latest ICR + its trend, and per-subcapacity recent ICR. */
sealed interface LogicProgressUiState {
    data object Loading : LogicProgressUiState

    data class Ready(
        val totalSessions: Int,
        /** Mean ICR of the most recent session, or null when no session exists yet (→ "en calibración"). */
        val latestAverageIcr: Double?,
        val icrTrend: LogicProgressTrend,
        val skills: List<LogicSkillProgress>,
    ) : LogicProgressUiState
}

/** Direction of the ICR trend across recent sessions. UNKNOWN until there is enough history. */
enum class LogicProgressTrend { IMPROVING, STABLE, DECLINING, UNKNOWN }

data class LogicSkillProgress(
    val skillId: LogicSkill,
    /** Recent ICR for this subcapacity, or null while it is still calibrating (never rendered as 0). */
    val recentIcr: Double?,
    val attempts: Int,
)
