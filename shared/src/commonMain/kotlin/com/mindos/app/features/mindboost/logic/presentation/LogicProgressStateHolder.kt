package com.mindos.app.features.mindboost.logic.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.logic.di.LogicGraph
import com.mindos.app.features.mindboost.logic.domain.repository.LogicTrainingRepository
import com.mindos.app.features.mindboost.logic.presentation.state.LogicProgressTrend
import com.mindos.app.features.mindboost.logic.presentation.state.LogicProgressUiState
import com.mindos.app.features.mindboost.logic.presentation.state.LogicSkillProgress

/**
 * Read-only progress holder for Lógica, mirroring the FocusProgressScreen pattern: total sessions, the
 * latest average ICR, its trend over recent sessions, and per-subcapacity recent ICR. Honest by design —
 * a subcapacity with no recent ICR shows "en calibración" (never 0), and the trend is UNKNOWN until there
 * is enough history. Reuses the module's existing [LogicTrainingRepository]; no new persistence.
 */
class LogicProgressStateHolder(private val repository: LogicTrainingRepository) {
    var uiState: LogicProgressUiState by mutableStateOf(LogicProgressUiState.Loading)
        private set

    suspend fun load(userId: String) {
        val ordered = repository.recentSessions(userId, RECENT_LIMIT).sortedBy { it.endedAt }
        val skills = repository.loadSkillStates(userId)
        val icrs = ordered.map { it.result.averageIcr }
        uiState = LogicProgressUiState.Ready(
            totalSessions = ordered.size,
            latestAverageIcr = icrs.lastOrNull(),
            icrTrend = logicIcrTrend(icrs),
            skills = skills.map { LogicSkillProgress(it.skillId, it.recentIcr, it.attemptsCount) },
        )
    }

    private companion object {
        const val RECENT_LIMIT = 30
    }
}

/** Pure ICR-trend classifier over sessions ordered OLDEST-first. UNKNOWN until [MIN_FOR_TREND] sessions. */
internal fun logicIcrTrend(icrsOldestFirst: List<Double>): LogicProgressTrend {
    val half = icrsOldestFirst.size / 2
    val older = icrsOldestFirst.take(half)
    val newer = icrsOldestFirst.drop(icrsOldestFirst.size - half)
    if (icrsOldestFirst.size < MIN_FOR_TREND || older.isEmpty() || newer.isEmpty()) {
        return LogicProgressTrend.UNKNOWN
    }
    val delta = newer.average() - older.average()
    return when {
        delta > TREND_EPSILON -> LogicProgressTrend.IMPROVING
        delta < -TREND_EPSILON -> LogicProgressTrend.DECLINING
        else -> LogicProgressTrend.STABLE
    }
}

private const val MIN_FOR_TREND = 4
private const val TREND_EPSILON = 2.0 // ICR points

@Composable
fun rememberLogicProgressStateHolder(graph: LogicGraph): LogicProgressStateHolder =
    remember(graph) { LogicProgressStateHolder(graph.repository) }
