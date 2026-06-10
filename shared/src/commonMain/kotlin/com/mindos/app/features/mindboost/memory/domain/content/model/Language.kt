package com.mindos.app.features.mindboost.memory.domain.content.model

/**
 * Content language. V1 ships Spanish only ([SPANISH_ES]); the enum exists so every
 * content API takes an explicit [Language] parameter instead of hardcoding ES —
 * adding EN/PT in V2 then touches data, not interfaces (doc §16 Q4).
 */
enum class Language {
    SPANISH_ES,
}

/** ISO 639-1 code, for persistence keys and a future language selector. */
fun Language.isoCode(): String = when (this) {
    Language.SPANISH_ES -> "es"
}

/** Human-readable name for a future UI selector. */
fun Language.displayName(): String = when (this) {
    Language.SPANISH_ES -> "Español"
}
