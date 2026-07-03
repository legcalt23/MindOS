package com.mindos.app.features.arena.evidence.motors

import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.hardware.IllegalMatchException
import com.mindos.app.features.arena.rating.engine.TrialEvidence

/**
 * Phase 05 / Task 4: Evidence-acceptance rules for the three MVP cognitive motors.
 *
 * - FOCUS_INTERFERENCE_CONTROL  -> MOT_FOC_01
 * - WORKING_MEMORY_SEQUENCE_UPDATING -> MOT_MEM_01
 * - MENTAL_SPEED_CHOICE_REACTION -> MOT_SPD_01 (hardware-class sensitive)
 *
 * The motor type can never be unknown/null: [TaskMotorType] is a closed enum with exactly these
 * three values. Mental Speed evidence is only acceptable when it carries a hardware class, and Mental
 * Speed results from differing hardware classes are not comparable.
 */
object MotorEvidenceRules {

    val SUPPORTED: Set<TaskMotorType> = setOf(
        TaskMotorType.FOCUS_INTERFERENCE_CONTROL,
        TaskMotorType.WORKING_MEMORY_SEQUENCE_UPDATING,
        TaskMotorType.MENTAL_SPEED_CHOICE_REACTION,
    )

    fun motorId(type: TaskMotorType): String = when (type) {
        TaskMotorType.FOCUS_INTERFERENCE_CONTROL -> "MOT_FOC_01"
        TaskMotorType.WORKING_MEMORY_SEQUENCE_UPDATING -> "MOT_MEM_01"
        TaskMotorType.MENTAL_SPEED_CHOICE_REACTION -> "MOT_SPD_01"
    }

    /** A motor requires hardware-class context if and only if it is Mental Speed. */
    fun requiresHardwareClass(type: TaskMotorType): Boolean =
        type == TaskMotorType.MENTAL_SPEED_CHOICE_REACTION

    /** True when this evidence is acceptable for its motor (Mental Speed must carry a hardware class). */
    fun acceptsEvidence(evidence: TrialEvidence): Boolean {
        val supported = evidence.motorType in SUPPORTED
        val hardwareOk = !requiresHardwareClass(evidence.motorType) || evidence.hardwareClass != null
        return supported && hardwareOk
    }

    /**
     * Two Mental Speed evidences are comparable only within the same hardware class. Focus and
     * Working Memory evidences are always comparable (they do not inherit the Mental Speed boundary).
     */
    fun areComparable(a: TrialEvidence, b: TrialEvidence): Boolean = when {
        a.motorType != b.motorType -> false
        a.motorType != TaskMotorType.MENTAL_SPEED_CHOICE_REACTION -> true
        else -> a.hardwareClass != null && a.hardwareClass == b.hardwareClass
    }

    /** Throws [IllegalMatchException] if the two evidences are not comparable. */
    fun requireComparable(a: TrialEvidence, b: TrialEvidence) {
        if (!areComparable(a, b)) {
            throw IllegalMatchException(
                "Incomparable evidence for ${a.motorType.name}: " +
                    "${a.hardwareClass?.id} vs ${b.hardwareClass?.id}",
            )
        }
    }
}
