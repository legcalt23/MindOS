package com.mindos.app.features.mindboost.speed.domain.cognitive

import com.mindos.app.features.mindboost.core.session.cognitive.ExerciseSpecificMetricCodec
import com.mindos.app.features.mindboost.core.session.cognitive.ExerciseSpecificMetricPayload
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

/**
 * SYSTEM-1 slice 4 · The exercise-specific detail of an E8 · Go/No-Go run that the generic [CognitiveSessionRun]
 * intentionally does NOT carry — the raw outcome counts + the descriptive pattern + the honest timing-integrity
 * level + which caps fired. [lostInputTaps] is the count of taps received WITHOUT a captured onset (the run's
 * `missingOnsetTaps`): a measured-but-un-timestampable input, COUNTED here and flagged as an anomaly, NEVER
 * turned into a fabricated reaction time (see the [com.mindos.app.features.mindboost.core.session.cognitive.
 * TrialOutcomeKind.LOST_INPUT] contract). Holds NO per-trial content, stimulus ids, or PII.
 */
data class GoNoGoInhibitionPayload(
    val goTrials: Int,
    val noGoTrials: Int,
    val goCorrect: Int,
    val noGoCorrect: Int,
    val commissions: Int,
    val omissions: Int,
    val anticipations: Int,
    val lateResponses: Int,
    val fastCommissions: Int,
    val lostInputTaps: Int,
    val category: String,
    val timingIntegrityLevel: String,
    val commissionCapApplied: Boolean,
    val goAccuracyCapApplied: Boolean,
    val anticipationCapApplied: Boolean,
    val lowConfidence: Boolean,
)

/**
 * The E8 codec for [GoNoGoInhibitionPayload]. Lives in the SPEED feature (each exercise owns its codec;
 * `core/session` defines only the [ExerciseSpecificMetricCodec] contract). [decode] is FAIL-CLOSED: a payload of
 * the wrong [type], an unsupported [schemaVersion], or a missing/mistyped field throws, so a tampered or
 * partial payload is treated as unusable rather than silently defaulted.
 */
object GoNoGoInhibitionPayloadCodec : ExerciseSpecificMetricCodec<GoNoGoInhibitionPayload> {

    const val TYPE: String = "gonogo_inhibition"
    const val SCHEMA_VERSION: Int = 1

    override val type: String = TYPE
    override val schemaVersion: Int = SCHEMA_VERSION

    private val requiredKeys: List<String> = listOf(
        "goTrials", "noGoTrials", "goCorrect", "noGoCorrect", "commissions", "omissions", "anticipations",
        "lateResponses", "fastCommissions", "lostInputTaps", "category", "timingIntegrityLevel",
        "commissionCapApplied", "goAccuracyCapApplied", "anticipationCapApplied", "lowConfidence",
    )

    override fun encode(value: GoNoGoInhibitionPayload): ExerciseSpecificMetricPayload =
        ExerciseSpecificMetricPayload(
            type = TYPE,
            schemaVersion = SCHEMA_VERSION,
            payload = buildJsonObject {
                put("goTrials", value.goTrials)
                put("noGoTrials", value.noGoTrials)
                put("goCorrect", value.goCorrect)
                put("noGoCorrect", value.noGoCorrect)
                put("commissions", value.commissions)
                put("omissions", value.omissions)
                put("anticipations", value.anticipations)
                put("lateResponses", value.lateResponses)
                put("fastCommissions", value.fastCommissions)
                put("lostInputTaps", value.lostInputTaps)
                put("category", value.category)
                put("timingIntegrityLevel", value.timingIntegrityLevel)
                put("commissionCapApplied", value.commissionCapApplied)
                put("goAccuracyCapApplied", value.goAccuracyCapApplied)
                put("anticipationCapApplied", value.anticipationCapApplied)
                put("lowConfidence", value.lowConfidence)
            },
        )

    override fun decode(payload: ExerciseSpecificMetricPayload): GoNoGoInhibitionPayload {
        require(payload.type == TYPE) { "Not a $TYPE payload (was '${payload.type}')." }
        require(payload.schemaVersion == SCHEMA_VERSION) {
            "Unsupported $TYPE schemaVersion ${payload.schemaVersion} (supported: $SCHEMA_VERSION)."
        }
        val o = payload.payload
        return GoNoGoInhibitionPayload(
            goTrials = o.reqInt("goTrials"),
            noGoTrials = o.reqInt("noGoTrials"),
            goCorrect = o.reqInt("goCorrect"),
            noGoCorrect = o.reqInt("noGoCorrect"),
            commissions = o.reqInt("commissions"),
            omissions = o.reqInt("omissions"),
            anticipations = o.reqInt("anticipations"),
            lateResponses = o.reqInt("lateResponses"),
            fastCommissions = o.reqInt("fastCommissions"),
            lostInputTaps = o.reqInt("lostInputTaps"),
            category = o.reqStr("category"),
            timingIntegrityLevel = o.reqStr("timingIntegrityLevel"),
            commissionCapApplied = o.reqBool("commissionCapApplied"),
            goAccuracyCapApplied = o.reqBool("goAccuracyCapApplied"),
            anticipationCapApplied = o.reqBool("anticipationCapApplied"),
            lowConfidence = o.reqBool("lowConfidence"),
        )
    }

    override fun validate(payload: ExerciseSpecificMetricPayload): Boolean =
        payload.type == TYPE &&
            payload.schemaVersion == SCHEMA_VERSION &&
            requiredKeys.all { it in payload.payload }

    private fun JsonObject.reqInt(key: String): Int =
        (this[key] as? JsonPrimitive)?.intOrNull ?: error("$TYPE payload missing Int '$key'.")

    private fun JsonObject.reqBool(key: String): Boolean =
        (this[key] as? JsonPrimitive)?.booleanOrNull ?: error("$TYPE payload missing Boolean '$key'.")

    private fun JsonObject.reqStr(key: String): String =
        (this[key] as? JsonPrimitive)?.contentOrNull ?: error("$TYPE payload missing String '$key'.")
}
