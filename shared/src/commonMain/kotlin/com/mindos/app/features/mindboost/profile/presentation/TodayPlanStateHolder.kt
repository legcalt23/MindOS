package com.mindos.app.features.mindboost.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.profile.domain.application.MindBoostProfileService
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.orchestration.TrainingRecommendation
import com.mindos.app.features.mindboost.profile.domain.orchestration.TransversalDailyPlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Computes the hub's cross-module "plan de hoy": loads the cached (else freshly computed) profile and ranks
 * the plannable tracks via [TransversalDailyPlanner]. Thin glue — all ranking lives in the domain.
 *
 * [plannable] intentionally EXCLUDES Velocidad/Creatividad (beta/practice, not an authoritative measure),
 * Lenguaje (no production store yet) and MEMORIA_EPISODICA (same navigable track as Memoria) — so the plan
 * only ever suggests tracks that both measure honestly and map to a distinct hub destination.
 */
class TodayPlanStateHolder(
    private val service: MindBoostProfileService,
    private val userId: String,
    private val scope: CoroutineScope,
    private val plannable: Set<CognitiveModuleId> = DEFAULT_PLANNABLE,
    private val planner: TransversalDailyPlanner = TransversalDailyPlanner(),
) {
    var plan by mutableStateOf<List<TrainingRecommendation>>(emptyList())
        private set

    fun load() {
        scope.launch {
            val profile = service.cached(userId) ?: service.refresh(userId)
            plan = planner.plan(profile, plannable)
        }
    }

    companion object {
        val DEFAULT_PLANNABLE: Set<CognitiveModuleId> = setOf(
            CognitiveModuleId.ENFOQUE,
            CognitiveModuleId.MEMORIA,
            CognitiveModuleId.LOGICA,
            CognitiveModuleId.DECISIONES,
            CognitiveModuleId.EMOCIONES,
        )
    }
}

/** Production factory: reuses the shared profile service wiring. */
@Composable
fun rememberTodayPlanStateHolder(userId: String): TodayPlanStateHolder {
    val service = rememberMindBoostProfileService(userId)
    val scope = rememberCoroutineScope()
    return remember(service, userId, scope) {
        TodayPlanStateHolder(service = service, userId = userId, scope = scope)
    }
}
