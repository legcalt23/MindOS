package com.example.mindos.features.waves.domain

import com.example.mindos.features.waves.data.InMemoryWaveRequestRepository
import com.example.mindos.features.waves.data.WaveRequestIdGenerator
import com.example.mindos.features.waves.data.WaveTimeProvider
import com.example.mindos.features.waves.domain.model.WaveRelationshipStatus
import com.example.mindos.features.waves.domain.service.WaveChatAccessResult
import com.example.mindos.features.waves.domain.service.WaveRequestService
import com.example.mindos.features.waves.domain.service.WaveSendRequestResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WaveRequestServiceTest {
    @Test
    fun noSePuedeChatearSinSolicitudAceptada() = runTest {
        val service = testService()
        val access = service.canStartWaveChat("alice", "bob")

        assertIs<WaveChatAccessResult.Denied>(access)
    }

    @Test
    fun sePuedeChatearConSolicitudAceptada() = runTest {
        val service = testService()
        val request = assertIs<WaveSendRequestResult.Created>(service.sendRequest("alice", "bob")).request

        service.acceptRequest(request.id, "bob")

        val access = service.canStartWaveChat("alice", "bob")

        assertIs<WaveChatAccessResult.Allowed>(access)
    }

    @Test
    fun noSeDuplicanSolicitudesPendientes() = runTest {
        val service = testService()
        val first = assertIs<WaveSendRequestResult.Created>(service.sendRequest("alice", "bob")).request
        val second = service.sendRequest("alice", "bob")

        val duplicate = assertIs<WaveSendRequestResult.DuplicatePending>(second)
        assertEquals(first.id, duplicate.request.id)
    }

    @Test
    fun solicitudInversaPendienteSeDetecta() = runTest {
        val service = testService()
        val first = assertIs<WaveSendRequestResult.Created>(service.sendRequest("alice", "bob")).request
        val second = service.sendRequest("bob", "alice")

        val duplicate = assertIs<WaveSendRequestResult.DuplicatePending>(second)
        assertEquals(first.id, duplicate.request.id)

        val relationship = service.getRelationshipStatus("bob", "alice")
        assertIs<WaveRelationshipStatus.PendingIncoming>(relationship)
    }

    @Test
    fun contadorDePendientesFunciona() = runTest {
        val service = testService()
        assertEquals(0, service.observePendingRequestCountReceived("bob").first())

        service.sendRequest("alice", "bob")
        service.sendRequest("carol", "bob")

        assertEquals(2, service.observePendingRequestCountReceived("bob").first())
    }

    @Test
    fun aceptarCambiaPermisosDeChat() = runTest {
        val service = testService()
        val request = assertIs<WaveSendRequestResult.Created>(service.sendRequest("alice", "bob")).request

        val beforeAccept = service.canStartWaveChat("alice", "bob")
        assertIs<WaveChatAccessResult.Denied>(beforeAccept)

        service.acceptRequest(request.id, "bob")

        val afterAccept = service.canStartWaveChat("alice", "bob")
        assertIs<WaveChatAccessResult.Allowed>(afterAccept)
    }

    private fun testService(): WaveRequestService {
        var nextId = 1
        var nextTimestamp = 1_000L
        val repository = InMemoryWaveRequestRepository(
            idGenerator = WaveRequestIdGenerator { "wave_${nextId++}" },
            timeProvider = WaveTimeProvider { nextTimestamp++ },
        )
        return WaveRequestService(repository)
    }
}
