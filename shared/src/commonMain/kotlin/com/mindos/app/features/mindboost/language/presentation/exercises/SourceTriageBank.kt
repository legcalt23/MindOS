// Dense calibrated content bank; the literal difficulty levels are the data, not a magic-number smell.
@file:Suppress("MagicNumber")

package com.mindos.app.features.mindboost.language.presentation.exercises

import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.presentation.DemoItem

/**
 * E6 · Source Triage — critical reading (CRIT). The user separates a verifiable fact from an opinion,
 * a speculation, or a claim with missing context — literacy without credulity or cynicism. Distractors
 * are diagnostic: confusing fact and judgement is a source error (CRIT_SOURCE); treating a prediction
 * as established fact is over-claiming (INF_OVER); picking an irrelevant detail is a literal slip
 * (LIT_DIST). The correct answer names the claim's real epistemic status.
 */
object SourceTriageBank {

    @Suppress("LongParameterList") // content factory: each field is one part of the item descriptor.
    private fun item(
        id: String,
        prompt: String,
        options: List<Pair<String, String>>,
        correct: String,
        distractors: Map<String, ErrorType>,
        level: Int,
    ): DemoItem = single(
        id = id,
        prompt = prompt,
        options = options.map { opt(it.first, it.second) },
        correct = correct,
        distractors = distractors,
        trait = TraitCode.CRIT,
        operation = OperationType.CRITIQUE_SOURCE,
        level = level,
    )

    val items: List<DemoItem> = listOf(
        item(
            id = "src_triage_1",
            prompt = "«El agua hierve a 100 °C a nivel del mar.» ¿Qué tipo de afirmación es?",
            options = listOf(
                "a" to "Dato verificable.",
                "b" to "Opinión.",
                "c" to "Especulación.",
            ),
            correct = "a",
            distractors = mapOf("b" to ErrorType.CRIT_SOURCE, "c" to ErrorType.CRIT_SOURCE),
            level = 1,
        ),
        item(
            id = "src_triage_2",
            prompt = "«Esta película es la mejor del año.» ¿Qué tipo de afirmación es?",
            options = listOf(
                "a" to "Dato verificable.",
                "b" to "Opinión.",
                "c" to "Especulación.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.CRIT_SOURCE, "c" to ErrorType.CRIT_SOURCE),
            level = 1,
        ),
        item(
            id = "src_triage_3",
            prompt = "«El próximo año subirán los precios de la vivienda.» ¿Qué tipo de afirmación es?",
            options = listOf(
                "a" to "Dato verificable hoy.",
                "b" to "Opinión sobre el pasado.",
                "c" to "Especulación sobre el futuro.",
            ),
            correct = "c",
            distractors = mapOf("a" to ErrorType.INF_OVER, "b" to ErrorType.CRIT_SOURCE),
            level = 2,
        ),
        item(
            id = "src_triage_4",
            prompt = "«La empresa facturó un 10% más que el año pasado, según su informe anual.» " +
                "¿Qué tipo de afirmación es?",
            options = listOf(
                "a" to "Dato verificable, con fuente.",
                "b" to "Opinión del autor.",
                "c" to "Especulación.",
            ),
            correct = "a",
            distractors = mapOf("b" to ErrorType.CRIT_SOURCE, "c" to ErrorType.CRIT_SOURCE),
            level = 2,
        ),
        item(
            id = "src_triage_5",
            prompt = "«El plan, que sin duda fracasará, recortará el gasto un 5%.» " +
                "¿Cuál de estas partes es opinión, no dato?",
            options = listOf(
                "a" to "Que recortará el gasto un 5%.",
                "b" to "Que «sin duda fracasará».",
                "c" to "Que el plan existe.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.CRIT_SOURCE, "c" to ErrorType.LIT_DIST),
            level = 3,
        ),
        item(
            id = "src_triage_6",
            prompt = "Un anuncio dice: «9 de cada 10 dentistas recomiendan nuestra pasta». " +
                "¿Qué falta para poder fiarse del dato?",
            options = listOf(
                "a" to "Nada; el dato ya es completo.",
                "b" to "Saber cuántos dentistas y quién hizo el estudio.",
                "c" to "Que la pasta sea barata.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.CRIT_SOURCE, "c" to ErrorType.LIT_DIST),
            level = 3,
        ),
    )

    val exercise: LanguageExercise = LanguageExercise(
        id = "source_triage",
        title = "Source Triage",
        subtitle = "Separa dato verificable, opinión y especulación; detecta lo que falta para fiarte.",
        primaryTrait = TraitCode.CRIT,
        family = LanguageExerciseFamily.SOURCE_TRIAGE,
        items = items,
    )
}
