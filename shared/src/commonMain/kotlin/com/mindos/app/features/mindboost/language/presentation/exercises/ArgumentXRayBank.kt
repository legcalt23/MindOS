// Dense calibrated content bank; the literal difficulty levels are the data, not a magic-number smell.
@file:Suppress("MagicNumber")

package com.mindos.app.features.mindboost.language.presentation.exercises

import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.presentation.DemoItem

/**
 * E4 · Argument X-Ray — argument anatomy (ARG). The user finds the single weakest link of a short
 * argument: the unsupported jump or overgeneralization. Distractors are diagnostic: one points at a
 * given fact (ARG_GAP — failing to locate the real gap), one at an invented caricature of the claim
 * (ARG_STRAW). The real weak link is always a genuine inferential leap, never a fact or a fabrication.
 */
object ArgumentXRayBank {

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
        trait = TraitCode.ARG,
        operation = OperationType.ARGUE,
        level = level,
    )

    val items: List<DemoItem> = listOf(
        item(
            id = "arg_xray_1",
            prompt = "«Llueve, así que habrá atasco; siempre que llueve hay atasco.» " +
                "¿Cuál es el eslabón más débil?",
            options = listOf(
                "a" to "Que esté lloviendo.",
                "b" to "Que «siempre» que llueve haya atasco.",
                "c" to "Que conducir bajo la lluvia esté prohibido.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.ARG_GAP, "c" to ErrorType.ARG_STRAW),
            level = 1,
        ),
        item(
            id = "arg_xray_2",
            prompt = "«Este móvil es el mejor: es el más caro de la tienda.» " +
                "¿Cuál es el eslabón más débil?",
            options = listOf(
                "a" to "Que sea el más caro.",
                "b" to "Que ser el más caro implique ser el mejor.",
                "c" to "Que la tienda quiera engañar a los clientes.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.ARG_GAP, "c" to ErrorType.ARG_STRAW),
            level = 1,
        ),
        item(
            id = "arg_xray_3",
            prompt = "«Deberíamos cerrar el parque para siempre: ayer hubo un robo allí.» " +
                "¿Cuál es el eslabón más débil?",
            options = listOf(
                "a" to "Que ayer hubiera un robo.",
                "b" to "Que un robo justifique cerrar el parque para siempre.",
                "c" to "Que quien lo propone odie los parques.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.ARG_GAP, "c" to ErrorType.ARG_STRAW),
            level = 2,
        ),
        item(
            id = "arg_xray_4",
            prompt = "«Los videojuegos son malos: un estudio vio que algunos jugadores duermen poco.» " +
                "¿Cuál es el eslabón más débil?",
            options = listOf(
                "a" to "Que el estudio exista.",
                "b" to "Que «algunos» duerman poco pruebe que son malos para todos.",
                "c" to "Que quien los defiende sea un vago.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.ARG_GAP, "c" to ErrorType.ARG_STRAW),
            level = 2,
        ),
        item(
            id = "arg_xray_5",
            prompt = "«La nueva ley no sirve: total, los políticos solo piensan en sus votos.» " +
                "¿Cuál es el eslabón más débil?",
            options = listOf(
                "a" to "Que exista una nueva ley.",
                "b" to "Que la motivación de quien la propone diga si la ley funciona.",
                "c" to "Que la ley obligue a votar a todo el mundo.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.ARG_GAP, "c" to ErrorType.ARG_STRAW),
            level = 3,
        ),
        item(
            id = "arg_xray_6",
            prompt = "«Hay que prohibir las bicis en la ciudad: mi vecino se cayó de una.» " +
                "¿Cuál es el eslabón más débil?",
            options = listOf(
                "a" to "Que el vecino se cayera.",
                "b" to "Que un caso aislado justifique una prohibición general.",
                "c" to "Que a quien va en bici no le importe la seguridad.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.ARG_GAP, "c" to ErrorType.ARG_STRAW),
            level = 3,
        ),
    )

    val exercise: LanguageExercise = LanguageExercise(
        id = "argument_xray",
        title = "Argument X-Ray",
        subtitle = "Localiza el eslabón débil: el salto lógico real, no un hecho ni una caricatura.",
        primaryTrait = TraitCode.ARG,
        family = LanguageExerciseFamily.ARGUMENT_XRAY,
        items = items,
    )
}
