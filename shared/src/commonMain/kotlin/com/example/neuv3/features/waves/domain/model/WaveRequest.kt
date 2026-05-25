package com.example.neuv3.features.waves.domain.model

data class WaveRequest(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val status: WaveRequestStatus,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class WaveRequestStatus {
    Pending,
    Accepted,
    Rejected,
    Cancelled,
    Blocked,
}
