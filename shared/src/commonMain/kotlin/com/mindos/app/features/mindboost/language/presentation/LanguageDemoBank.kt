package com.mindos.app.features.mindboost.language.presentation

import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.ResponseMode
import com.mindos.app.features.mindboost.language.domain.model.TaskId
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.scoring.AnswerKey

/** One option / evidence span / orderable item of a closed demo item. */
data class DemoOption(val id: String, val text: String)

/**
 * A minimal, provisional closed item for the demo flow. Carries the [responseMode] so the screen
 * can render the right interaction, the choosable [options], optional [evidence] spans (for
 * evidence-selection), and the matching [key] the scorer uses. Real content lives in the Content OS.
 */
@Suppress("LongParameterList") // declarative content descriptor; the wide constructor is the model.
data class DemoItem(
    val taskId: TaskId,
    val prompt: String,
    val responseMode: ResponseMode,
    val options: List<DemoOption>,
    val key: AnswerKey,
    val evidence: List<DemoOption> = emptyList(),
    val primaryTrait: TraitCode = TraitCode.INF,
    val operation: OperationType = OperationType.INFER,
    val bankType: BankType = BankType.TRAINING,
    /** Calibrated desirable-difficulty level (1 = base, 2 = intermediate, 3 = advanced). */
    val level: Int = 1,
)

/** Provisional offline bank with the three closed exercise models (INF). Replace with the Content OS. */
object LanguageDemoBank {

    val items: List<DemoItem> = listOf(
        single(
            id = "lang_demo_inf_1",
            prompt = "«Llovió toda la noche; por la mañana el suelo estaba seco.» ¿Qué se puede inferir?",
            options = listOf(
                DemoOption("a", "Alguien secó el suelo."),
                DemoOption("b", "El texto no permite concluir por qué está seco."),
                DemoOption("c", "No llovió."),
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.INF_OVER, "c" to ErrorType.LIT_DIST),
        ),
        single(
            id = "lang_demo_lit_1",
            prompt = "«El tren sale a las 9:00, tarda 2 horas y hace una parada de 15 minutos.» ¿A qué hora llega?",
            options = listOf(
                DemoOption("a", "A las 11:00."),
                DemoOption("b", "A las 11:15."),
                DemoOption("c", "A las 10:45."),
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.LIT_DIST, "c" to ErrorType.LIT_DIST),
            trait = TraitCode.LIT,
            operation = OperationType.LITERAL_EXTRACTION,
        ),
        single(
            id = "lang_demo_lexp_1",
            prompt = "«El informe fue ___: no dejó ninguna duda.» ¿Qué palabra es más precisa?",
            options = listOf(
                DemoOption("a", "bonito"),
                DemoOption("b", "largo"),
                DemoOption("c", "concluyente"),
            ),
            correct = "c",
            distractors = mapOf("a" to ErrorType.PROD_VAGUE, "b" to ErrorType.PROD_VAGUE),
            trait = TraitCode.LEX_P,
            operation = OperationType.IDENTIFY_NUANCE,
        ),
        single(
            id = "lang_demo_sum_1",
            prompt = "«Subió el precio del café por una sequía en Brasil; los bares ya lo notan.» ¿Mejor resumen?",
            options = listOf(
                DemoOption("a", "El texto habla de Brasil."),
                DemoOption("b", "Una sequía encarece el café y afecta a los bares."),
                DemoOption("c", "El café está caro porque sí."),
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.SUM_NO_THESIS, "c" to ErrorType.SUM_DISTORT),
            trait = TraitCode.SUM,
            operation = OperationType.SUMMARIZE,
        ),
        single(
            id = "lang_demo_arg_1",
            prompt = "«Llueve, así que habrá atasco; siempre que llueve hay atasco.» ¿Cuál es el eslabón más débil?",
            options = listOf(
                DemoOption("a", "Que esté lloviendo."),
                DemoOption("b", "Que «siempre» que llueve haya atasco."),
                DemoOption("c", "Que pueda haber atasco."),
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.ARG_GAP, "c" to ErrorType.ARG_GAP),
            trait = TraitCode.ARG,
            operation = OperationType.ARGUE,
        ),
        evidenceSelection(
            id = "lang_demo_inf_evidence_1",
            prompt = "«El parte: cielo nublado, viento del norte y bajada de temperatura.» " +
                "Elige la conclusión soportada y marca su evidencia.",
            options = listOf(
                DemoOption("a", "Va a nevar seguro."),
                DemoOption("b", "Puede hacer más frío."),
                DemoOption("c", "Hará calor."),
            ),
            evidence = listOf(
                DemoOption("e1", "cielo nublado"),
                DemoOption("e2", "viento del norte"),
                DemoOption("e3", "bajada de temperatura"),
            ),
            correct = "b",
            requiredEvidence = setOf("e3"),
            distractors = mapOf("a" to ErrorType.INF_OVER, "c" to ErrorType.LIT_DIST),
        ),
        ordering(
            id = "lang_demo_inf_order_1",
            prompt = "Ordena estas afirmaciones de MENOS a MÁS justificada por un texto.",
            ordered = listOf(
                DemoOption("o1", "Quizá ocurra."),
                DemoOption("o2", "Probablemente ocurra."),
                DemoOption("o3", "El texto afirma que ocurre."),
            ),
        ),
    )

    @Suppress("LongParameterList") // content factory: each field is one part of the item descriptor.
    private fun single(
        id: String,
        prompt: String,
        options: List<DemoOption>,
        correct: String,
        distractors: Map<String, ErrorType>,
        trait: TraitCode = TraitCode.INF,
        operation: OperationType = OperationType.INFER,
    ): DemoItem = DemoItem(
        taskId = TaskId(id),
        prompt = prompt,
        responseMode = ResponseMode.MULTIPLE_CHOICE,
        options = options,
        key = AnswerKey.SingleChoice(correctOptionId = correct, distractorErrors = distractors),
        primaryTrait = trait,
        operation = operation,
    )

    @Suppress("LongParameterList") // content factory: each field is one part of the item descriptor.
    private fun evidenceSelection(
        id: String,
        prompt: String,
        options: List<DemoOption>,
        evidence: List<DemoOption>,
        correct: String,
        requiredEvidence: Set<String>,
        distractors: Map<String, ErrorType>,
    ): DemoItem = DemoItem(
        taskId = TaskId(id),
        prompt = prompt,
        responseMode = ResponseMode.MULTIPLE_CHOICE_WITH_EVIDENCE_SELECTION,
        options = options,
        evidence = evidence,
        key = AnswerKey.EvidenceSelection(
            correctOptionId = correct,
            requiredEvidenceIds = requiredEvidence,
            distractorErrors = distractors,
        ),
    )

    private fun ordering(id: String, prompt: String, ordered: List<DemoOption>): DemoItem = DemoItem(
        taskId = TaskId(id),
        prompt = prompt,
        responseMode = ResponseMode.ORDERING,
        options = ordered,
        key = AnswerKey.Ordering(correctOrder = ordered.map { it.id }),
        operation = OperationType.INFER,
    )
}
