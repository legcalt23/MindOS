package com.mindos.app.features.mindboost.emotions.data.local

import kotlinx.serialization.json.Json

/**
 * The single JSON codec for emotions persistence. `ignoreUnknownKeys` makes reads forward-compatible
 * (older builds tolerate newer fields); `encodeDefaults` guarantees `schemaVersion` is always written.
 */
internal val EmotionJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
