package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionExerciseCardUi

/**
 * The emotions home menu: every exercise wired end-to-end (mirrors `EmotionExerciseCatalog.implemented`,
 * so a playable exercise can never be invisible). Static — the catalogue is the source of truth.
 */
class EmotionHomeStateHolder {

    val exercises: List<EmotionExerciseCardUi> = EmotionExerciseCatalog.implemented.map { id ->
        val definition = EmotionExerciseCatalog.definition(id)
        EmotionExerciseCardUi(id = id, name = definition.displayName, description = definition.plainDescription)
    }
}
