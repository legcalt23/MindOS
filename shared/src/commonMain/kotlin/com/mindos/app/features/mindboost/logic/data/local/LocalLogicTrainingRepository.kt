package com.mindos.app.features.mindboost.logic.data.local

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readVersioned
import com.mindos.app.features.mindboost.logic.domain.model.LogicConfiguration
import com.mindos.app.features.mindboost.logic.domain.model.LogicSession
import com.mindos.app.features.mindboost.logic.domain.model.SkillState
import com.mindos.app.features.mindboost.logic.domain.repository.LogicTrainingRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Encrypted local-first repository. Attempts are structured and bounded; no free-text
 * justification is collected or persisted. The user aggregate can be deleted atomically.
 */
class LocalLogicTrainingRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = LogicJson.instance,
) : LogicTrainingRepository {

    private val mutex = Mutex()

    override suspend fun saveSession(session: LogicSession) = mutex.withLock {
        val current = load(session.userId)
        val sessions = (current.sessions.filterNot { it.id == session.id } + session)
            .sortedByDescending { it.endedAt }
            .take(MAX_SESSIONS)
        save(session.userId, current.copy(sessions = sessions))
    }

    override suspend fun recentSessions(userId: String, limit: Int): List<LogicSession> {
        require(limit > 0) { "limit must be > 0." }
        return load(userId).sessions.sortedByDescending { it.endedAt }.take(limit)
    }

    override suspend fun saveSkillStates(userId: String, states: List<SkillState>) = mutex.withLock {
        save(userId, load(userId).copy(skillStates = states))
    }

    override suspend fun loadSkillStates(userId: String): List<SkillState> = load(userId).skillStates

    override suspend fun saveConfiguration(userId: String, configuration: LogicConfiguration) = mutex.withLock {
        save(userId, load(userId).copy(configuration = configuration))
    }

    override suspend fun loadConfiguration(userId: String): LogicConfiguration = load(userId).configuration

    override suspend fun deleteUserData(userId: String) {
        mutex.withLock { store.remove(key(userId)) }
    }

    private suspend fun load(userId: String): PersistedLogicData =
        store.readVersioned(
            key = key(userId),
            currentVersion = PersistedLogicData.CURRENT_SCHEMA_VERSION,
            parser = { raw -> json.decodeFromString<PersistedLogicData>(raw) },
            versionOf = PersistedLogicData::schemaVersion,
        ) ?: PersistedLogicData()

    private suspend fun save(userId: String, data: PersistedLogicData) {
        store.put(key(userId), json.encodeToString(data))
    }

    private fun key(userId: String): String = "$USER_PREFIX$userId"

    private companion object {
        const val USER_PREFIX = "logic_user:"
        const val MAX_SESSIONS = 30
    }
}

object LogicJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}

@Serializable
data class PersistedLogicData(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val sessions: List<LogicSession> = emptyList(),
    val skillStates: List<SkillState> = emptyList(),
    val configuration: LogicConfiguration = LogicConfiguration(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
