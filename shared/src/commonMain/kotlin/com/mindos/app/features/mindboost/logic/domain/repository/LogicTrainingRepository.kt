package com.mindos.app.features.mindboost.logic.domain.repository

import com.mindos.app.features.mindboost.logic.domain.model.LogicConfiguration
import com.mindos.app.features.mindboost.logic.domain.model.LogicSession
import com.mindos.app.features.mindboost.logic.domain.model.SkillState

interface LogicTrainingRepository {
    suspend fun saveSession(session: LogicSession)
    suspend fun recentSessions(userId: String, limit: Int): List<LogicSession>
    suspend fun saveSkillStates(userId: String, states: List<SkillState>)
    suspend fun loadSkillStates(userId: String): List<SkillState>
    suspend fun saveConfiguration(userId: String, configuration: LogicConfiguration)
    suspend fun loadConfiguration(userId: String): LogicConfiguration
    suspend fun deleteUserData(userId: String)
}

