package com.mindos.contracts

import kotlinx.serialization.Serializable

object ContractSchema {
    const val Current = 1
}

@Serializable
data class ContractVersion(
    val schemaVersion: Int = ContractSchema.Current,
) {
    init {
        require(schemaVersion > 0) { "schemaVersion must be positive." }
    }
}
