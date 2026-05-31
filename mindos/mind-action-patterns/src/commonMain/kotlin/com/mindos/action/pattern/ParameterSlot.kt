package com.mindos.action.pattern

import com.mindos.action.entity.EntityType
import com.mindos.action.understanding.SlotName
import kotlinx.serialization.Serializable

/** How many tokens a slot may consume. [Greedy] is only valid for the last slot (FreeText). */
@Serializable
enum class SlotCardinality { Single, Greedy }

/**
 * A typed slot inside a parametric pattern. The slot declares the [acceptedType] so the
 * matching engine knows which entity extractor to run — this is what closes the
 * false-positive gap (a value that fails the type's shape is rejected, not coerced).
 */
@Serializable
data class ParameterSlot(
    val name: SlotName,
    val acceptedType: EntityType,
    val cardinality: SlotCardinality = SlotCardinality.Single,
    val required: Boolean = true,
)
