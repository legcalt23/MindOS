package com.example.mindos.features.waves.domain.model

sealed interface WaveRelationshipStatus {
    data object None : WaveRelationshipStatus
    data class PendingIncoming(val request: WaveRequest) : WaveRelationshipStatus
    data class PendingOutgoing(val request: WaveRequest) : WaveRelationshipStatus
    data class Accepted(val request: WaveRequest) : WaveRelationshipStatus
    data class Rejected(val request: WaveRequest) : WaveRelationshipStatus
    data class Cancelled(val request: WaveRequest) : WaveRelationshipStatus
    data class Blocked(val request: WaveRequest) : WaveRelationshipStatus
}
