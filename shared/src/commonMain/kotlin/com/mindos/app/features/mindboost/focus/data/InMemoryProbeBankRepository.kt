package com.mindos.app.features.mindboost.focus.data

import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeAdministration
import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeBank
import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeDefinition
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.repository.ProbeBankRepository

/**
 * In-memory probe scheduler/recorder. Rotates each probe through its reserved
 * parallel forms so the same form is not re-administered until the cycle wraps,
 * and stores probe results in a dedicated map — never in the training history.
 */
class InMemoryProbeBankRepository(
    private val probes: List<ProbeDefinition> = ProbeBank.all,
) : ProbeBankRepository {

    // Per (user,probe) rotation cursor over the probe's reserved forms.
    private val formCursors = mutableMapOf<String, Int>()
    private val probeResults = mutableMapOf<String, MutableList<SessionResult>>()

    override suspend fun nextProbe(userId: String, week: Int): ProbeAdministration {
        require(probes.isNotEmpty()) { "Probe bank is empty." }
        // Choose a probe deterministically by week (rotates across capacities).
        val probe = probes[((week - 1).coerceAtLeast(0)) % probes.size]
        val cursorKey = "$userId:${probe.probeId}"
        val index = formCursors.getOrElse(cursorKey) { 0 }
        val formSeed = probe.reservedForms[index % probe.reservedForms.size]
        formCursors[cursorKey] = index + 1
        return ProbeAdministration(probe = probe, formSeed = formSeed, week = week)
    }

    override suspend fun recordProbe(userId: String, result: SessionResult) {
        probeResults.getOrPut(userId) { mutableListOf() }.add(result)
    }

    override suspend fun probeHistory(userId: String, limit: Int): List<SessionResult> {
        require(limit > 0) { "limit must be > 0." }
        return probeResults[userId].orEmpty().sortedByDescending { it.endedAtMs }.take(limit)
    }
}
