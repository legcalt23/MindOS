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
        val points = ordered.map { LogicIcrPoint(it.result.averageIcr, it.result.scoringVersion) }
        uiState = LogicProgressUiState.Ready(
            totalSessions = ordered.size,
            latestAverageIcr = points.lastOrNull()?.averageIcr,
            icrTrend = logicIcrTrend(points),
            skills = skills.map { LogicSkillProgress(it.skillId, it.recentIcr, it.attemptsCount) },
        )
    }

    private companion object {
        const val RECENT_LIMIT = 30
    }
}

/** One session's average ICR together with the scoring version that produced it. */
internal data class LogicIcrPoint(val averageIcr: Double, val scoringVersion: Int)

/**
 * Pure ICR-trend classifier over sessions ordered OLDEST-first. UNKNOWN until [MIN_FOR_TREND] sessions.
 *
 * A trend is a claim about the user, so it may only be made over points measured by the SAME
 * instrument. When the series straddles a scoring-version boundary the classifier declines instead:
 * the older half was scored by an equation that inflated it, so a naive comparison would report the
 * correction as the user getting worse. Refusing the verdict is the honest outcome — the series
 * becomes comparable again on its own once the older sessions age out.
 *
 * The guard lives here, in the pure function, rather than at the call site: a caller cannot assemble
 * the verdict while skipping it.
 */
internal fun logicIcrTrend(pointsOldestFirst: List<LogicIcrPoint>): LogicProgressTrend {
    val half = pointsOldestFirst.size / 2
    val older = pointsOldestFirst.take(half).map { it.averageIcr }
    val newer = pointsOldestFirst.drop(pointsOldestFirst.size - half).map { it.averageIcr }
    val enoughHistory = pointsOldestFirst.size >= MIN_FOR_TREND && older.isNotEmpty() && newer.isNotEmpty()
    val oneInstrument = pointsOldestFirst.distinctBy { it.scoringVersion }.size == 1
    if (!enoughHistory || !oneInstrument) {
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
