package com.mindos.app.features.mindboost.memory.domain.catalog

import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricKey
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseDefinition
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryModule
import com.mindos.app.features.mindboost.memory.domain.model.MemoryParadigm
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity
import com.mindos.app.features.mindboost.memory.domain.model.PracticeEffectRisk

/**
 * The canonical, immutable catalogue of the 16 memory exercises (doc §4 catálogo +
 * §15 tabla maestra). These are declarative tech-sheets only (no render or
 * stimulus-generation logic). They are the single source of truth that content
 * generation, metrics, adaptation, evaluation, periodization and reporting read
 * from. Mirrors the focus feature's `ExerciseCatalog`.
 *
 * Ej.16 (Calibración de Confianza) is modelled here for completeness but is a
 * transversal overlay applied on top of other exercises, not a standalone drill
 * (doc §4 Ej.16 "no es un ejercicio independiente sino un módulo transversal").
 */
object MemoryExerciseCatalog {

    val EX1_SPAN_COMPLEJO = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX1_SPAN_COMPLEJO,
        subcapacity = MemorySubcapacity.WORKING_MEMORY,
        module = MemoryModule.M1_EXECUTIVE_WM,
        paradigm = MemoryParadigm.COMPLEX_SPAN,
        relevantAxes = setOf(
            MemoryDifficultyAxis.LOAD,
            MemoryDifficultyAxis.CONCURRENCY,
            MemoryDifficultyAxis.SIMILARITY,
            MemoryDifficultyAxis.RETRIEVAL,
        ),
        governingAxis = MemoryDifficultyAxis.LOAD,
        governingMetric = MemoryMetricKey.WORKING_MEMORY_SPAN,
        primaryMetrics = setOf(
            MemoryMetricKey.WORKING_MEMORY_SPAN,
            MemoryMetricKey.DUAL_TASK_COST,
            MemoryMetricKey.INTRUSION_RATE,
            MemoryMetricKey.RETRIEVAL_SPEED,
        ),
        practiceEffectRisk = PracticeEffectRisk.MEDIUM,
        displayName = "Span Complejo",
        plainDescription = "Mantén una serie de ítems mientras resuelves otra tarea entre " +
            "medias, y recupéralos en orden. Mide tu memoria de trabajo bajo carga.",
    )

    val EX2_ACTUALIZACION_SET = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX2_ACTUALIZACION_SET,
        subcapacity = MemorySubcapacity.WORKING_MEMORY,
        module = MemoryModule.M1_EXECUTIVE_WM,
        paradigm = MemoryParadigm.RUNNING_MEMORY,
        relevantAxes = setOf(
            MemoryDifficultyAxis.LOAD,
            MemoryDifficultyAxis.INTERFERENCE,
            MemoryDifficultyAxis.CONCURRENCY,
            MemoryDifficultyAxis.SIMILARITY,
        ),
        governingAxis = MemoryDifficultyAxis.LOAD,
        governingMetric = MemoryMetricKey.WORKING_MEMORY_SPAN,
        primaryMetrics = setOf(
            MemoryMetricKey.WORKING_MEMORY_SPAN,
            MemoryMetricKey.INTRUSION_RATE,
            MemoryMetricKey.BINDING_ERROR_RATE,
        ),
        practiceEffectRisk = PracticeEffectRisk.MEDIUM,
        displayName = "Actualización",
        plainDescription = "Mantén solo la versión más reciente de cada hueco y descarta la " +
            "anterior. Mide tu capacidad de actualizar e inhibir lo viejo.",
    )

    val EX3_LISTA_ESTRATEGIA = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
        subcapacity = MemorySubcapacity.EPISODIC,
        module = MemoryModule.M2_DEEP_ENCODING,
        paradigm = MemoryParadigm.FREE_RECALL,
        relevantAxes = setOf(
            MemoryDifficultyAxis.LOAD,
            MemoryDifficultyAxis.SIMILARITY,
            MemoryDifficultyAxis.DELAY,
            MemoryDifficultyAxis.INTERFERENCE,
            MemoryDifficultyAxis.DEPTH,
            MemoryDifficultyAxis.RETRIEVAL,
        ),
        governingAxis = MemoryDifficultyAxis.LOAD,
        governingMetric = MemoryMetricKey.DELAYED_RECALL_ACCURACY,
        primaryMetrics = setOf(
            MemoryMetricKey.DELAYED_RECALL_ACCURACY,
            MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY,
            MemoryMetricKey.ARC_CLUSTERING,
            MemoryMetricKey.INTRUSION_RATE,
            MemoryMetricKey.CONSOLIDATION_INDEX,
        ),
        practiceEffectRisk = PracticeEffectRisk.HIGH,
        displayName = "Lista con Estrategia",
        plainDescription = "Estudia una lista y recupérala usando estrategias de codificación " +
            "cada vez más profundas. Mide cuánto retienes tras una demora.",
    )

    val EX4_PARES_ASOCIADOS = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX4_PARES_ASOCIADOS,
        subcapacity = MemorySubcapacity.ASSOCIATIVE,
        module = MemoryModule.M3_ASSOCIATIVE,
        paradigm = MemoryParadigm.PAIRED_ASSOCIATES,
        relevantAxes = setOf(
            MemoryDifficultyAxis.LOAD,
            MemoryDifficultyAxis.INTERFERENCE,
            MemoryDifficultyAxis.SIMILARITY,
            MemoryDifficultyAxis.DELAY,
            MemoryDifficultyAxis.RETRIEVAL,
            MemoryDifficultyAxis.DEPTH,
        ),
        governingAxis = MemoryDifficultyAxis.LOAD,
        governingMetric = MemoryMetricKey.ASSOCIATIVE_INDEX,
        primaryMetrics = setOf(
            MemoryMetricKey.ASSOCIATIVE_INDEX,
            MemoryMetricKey.CONJUNCTION_ERROR_RATE,
            MemoryMetricKey.INTRUSION_RATE,
            MemoryMetricKey.DELAYED_RECALL_ACCURACY,
            MemoryMetricKey.RETRIEVAL_SPEED,
        ),
        practiceEffectRisk = PracticeEffectRisk.HIGH,
        displayName = "Pares Asociados",
        plainDescription = "Aprende vínculos entre elementos sin relación y recupera uno dado " +
            "el otro. Mide tu memoria asociativa y su resistencia a la confusión.",
    )

    val EX5_GRID_ESPACIAL = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX5_GRID_ESPACIAL,
        subcapacity = MemorySubcapacity.SPATIAL,
        module = MemoryModule.M4_SPATIAL_VISUAL,
        paradigm = MemoryParadigm.SPATIAL_GRID,
        relevantAxes = setOf(
            MemoryDifficultyAxis.LOAD,
            MemoryDifficultyAxis.SIMILARITY,
            MemoryDifficultyAxis.DELAY,
            MemoryDifficultyAxis.INTERFERENCE,
        ),
        governingAxis = MemoryDifficultyAxis.LOAD,
        governingMetric = MemoryMetricKey.COWAN_K,
        primaryMetrics = setOf(
            MemoryMetricKey.COWAN_K,
            MemoryMetricKey.BINDING_ERROR_RATE,
            MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY,
            MemoryMetricKey.DELAYED_RECALL_ACCURACY,
        ),
        practiceEffectRisk = PracticeEffectRisk.MEDIUM,
        displayName = "Grid Espacial",
        plainDescription = "Recuerda qué objeto estaba en cada posición de una cuadrícula. " +
            "Mide tu memoria espacial y el vínculo objeto-posición.",
    )

    val EX6_DETECCION_CAMBIO = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX6_DETECCION_CAMBIO,
        subcapacity = MemorySubcapacity.VISUAL,
        module = MemoryModule.M4_SPATIAL_VISUAL,
        paradigm = MemoryParadigm.CHANGE_DETECTION,
        relevantAxes = setOf(
            MemoryDifficultyAxis.LOAD,
            MemoryDifficultyAxis.SIMILARITY,
            MemoryDifficultyAxis.DELAY,
        ),
        governingAxis = MemoryDifficultyAxis.LOAD,
        governingMetric = MemoryMetricKey.COWAN_K,
        primaryMetrics = setOf(
            MemoryMetricKey.COWAN_K,
            MemoryMetricKey.RECOGNITION_DPRIME,
            MemoryMetricKey.FALSE_ALARM_RATE,
            MemoryMetricKey.RETRIEVAL_SPEED,
        ),
        practiceEffectRisk = PracticeEffectRisk.LOW,
        displayName = "Detección de Cambio",
        plainDescription = "Detecta qué ha cambiado en una escena que desaparece y reaparece. " +
            "Mide tu memoria visual y tu atención al detalle.",
    )

    val EX7_SECUENCIAS = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX7_SECUENCIAS,
        subcapacity = MemorySubcapacity.SEQUENTIAL,
        module = MemoryModule.M4_SPATIAL_VISUAL,
        paradigm = MemoryParadigm.SERIAL_ORDER,
        relevantAxes = setOf(
            MemoryDifficultyAxis.LOAD,
            MemoryDifficultyAxis.SIMILARITY,
            MemoryDifficultyAxis.DELAY,
            MemoryDifficultyAxis.RETRIEVAL,
            MemoryDifficultyAxis.CONCURRENCY,
        ),
        governingAxis = MemoryDifficultyAxis.LOAD,
        governingMetric = MemoryMetricKey.SERIAL_ORDER_ACCURACY,
        primaryMetrics = setOf(
            MemoryMetricKey.SERIAL_ORDER_ACCURACY,
            MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY,
            MemoryMetricKey.INTRUSION_RATE,
        ),
        practiceEffectRisk = PracticeEffectRisk.MEDIUM,
        displayName = "Secuencias",
        plainDescription = "Reproduce una secuencia de elementos en el orden correcto (o " +
            "inverso). Mide tu memoria del orden temporal.",
    )

    val EX8_NOMBRES_ROSTROS = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX8_NOMBRES_ROSTROS,
        subcapacity = MemorySubcapacity.ASSOCIATIVE,
        module = MemoryModule.M3_ASSOCIATIVE,
        paradigm = MemoryParadigm.FACE_NAME,
        relevantAxes = setOf(
            MemoryDifficultyAxis.LOAD,
            MemoryDifficultyAxis.SIMILARITY,
            MemoryDifficultyAxis.DELAY,
            MemoryDifficultyAxis.DEPTH,
            MemoryDifficultyAxis.INTERFERENCE,
        ),
        governingAxis = MemoryDifficultyAxis.LOAD,
        governingMetric = MemoryMetricKey.ASSOCIATIVE_INDEX,
        primaryMetrics = setOf(
            MemoryMetricKey.ASSOCIATIVE_INDEX,
            MemoryMetricKey.CONJUNCTION_ERROR_RATE,
            MemoryMetricKey.RECOGNITION_DPRIME,
            MemoryMetricKey.DELAYED_RECALL_ACCURACY,
        ),
        practiceEffectRisk = PracticeEffectRisk.HIGH,
        displayName = "Nombres y Rostros",
        plainDescription = "Recuerda el nombre de cada rostro cuando vuelve a aparecer. Es la " +
            "tarea asociativa con mayor utilidad práctica.",
    )

    val EX9_INSTRUCCIONES = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX9_INSTRUCCIONES,
        subcapacity = MemorySubcapacity.SEQUENTIAL,
        module = MemoryModule.M1_EXECUTIVE_WM,
        paradigm = MemoryParadigm.INSTRUCTION_FOLLOWING,
        relevantAxes = setOf(
            MemoryDifficultyAxis.LOAD,
            MemoryDifficultyAxis.DELAY,
            MemoryDifficultyAxis.CONCURRENCY,
            MemoryDifficultyAxis.RETRIEVAL,
        ),
        governingAxis = MemoryDifficultyAxis.LOAD,
        governingMetric = MemoryMetricKey.SERIAL_ORDER_ACCURACY,
        primaryMetrics = setOf(
            MemoryMetricKey.SERIAL_ORDER_ACCURACY,
            MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY,
            MemoryMetricKey.OMISSION_RATE,
        ),
        practiceEffectRisk = PracticeEffectRisk.MEDIUM,
        displayName = "Instrucciones Complejas",
        plainDescription = "Recibe un conjunto de instrucciones y ejecútalas en orden, a veces " +
            "tras una demora. Simula recibir indicaciones en el trabajo.",
    )

    val EX10_PROSPECTIVA = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX10_PROSPECTIVA,
        subcapacity = MemorySubcapacity.PROSPECTIVE,
        module = MemoryModule.M6_PROSPECTIVE_META,
        paradigm = MemoryParadigm.PROSPECTIVE_MEMORY,
        relevantAxes = setOf(
            MemoryDifficultyAxis.CONCURRENCY,
            MemoryDifficultyAxis.DELAY,
            MemoryDifficultyAxis.SIMILARITY,
            MemoryDifficultyAxis.LOAD,
        ),
        governingAxis = MemoryDifficultyAxis.CONCURRENCY,
        governingMetric = MemoryMetricKey.PROSPECTIVE_SUCCESS_RATE,
        primaryMetrics = setOf(
            MemoryMetricKey.PROSPECTIVE_SUCCESS_RATE,
            MemoryMetricKey.DUAL_TASK_COST,
            MemoryMetricKey.FALSE_ALARM_RATE,
        ),
        practiceEffectRisk = PracticeEffectRisk.LOW,
        displayName = "Prospectiva Embebida",
        plainDescription = "Recuerda ejecutar una acción ante una señal mientras haces otra " +
            "tarea. Mide tu memoria de intenciones futuras.",
    )

    val EX11_INTERFERENCIA = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX11_INTERFERENCIA,
        subcapacity = MemorySubcapacity.INTERFERENCE_RESISTANCE,
        module = MemoryModule.M5_RESISTANCE,
        paradigm = MemoryParadigm.AB_INTERFERENCE,
        relevantAxes = setOf(
            MemoryDifficultyAxis.INTERFERENCE,
            MemoryDifficultyAxis.SIMILARITY,
            MemoryDifficultyAxis.LOAD,
            MemoryDifficultyAxis.DELAY,
        ),
        governingAxis = MemoryDifficultyAxis.INTERFERENCE,
        governingMetric = MemoryMetricKey.INTERFERENCE_INDEX,
        primaryMetrics = setOf(
            MemoryMetricKey.INTERFERENCE_INDEX,
            MemoryMetricKey.INTRUSION_RATE,
            MemoryMetricKey.DELAYED_RECALL_ACCURACY,
        ),
        practiceEffectRisk = PracticeEffectRisk.LOW,
        displayName = "Listas con Interferencia",
        plainDescription = "Recuerda la lista correcta sin confundirla con otra que la solapa. " +
            "Mide tu resistencia a la interferencia proactiva y retroactiva.",
    )

    val EX12_DISCRIMINACION_FINA = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX12_DISCRIMINACION_FINA,
        subcapacity = MemorySubcapacity.INTERFERENCE_RESISTANCE,
        module = MemoryModule.M5_RESISTANCE,
        paradigm = MemoryParadigm.MNEMONIC_SIMILARITY,
        relevantAxes = setOf(
            MemoryDifficultyAxis.SIMILARITY,
            MemoryDifficultyAxis.INTERFERENCE,
            MemoryDifficultyAxis.DELAY,
            MemoryDifficultyAxis.LOAD,
        ),
        governingAxis = MemoryDifficultyAxis.SIMILARITY,
        governingMetric = MemoryMetricKey.RECOGNITION_DPRIME,
        primaryMetrics = setOf(
            MemoryMetricKey.RECOGNITION_DPRIME,
            MemoryMetricKey.FALSE_ALARM_RATE,
            MemoryMetricKey.CALIBRATION_INDEX,
        ),
        practiceEffectRisk = PracticeEffectRisk.LOW,
        displayName = "Discriminación Fina",
        plainDescription = "Distingue lo que viste de señuelos muy parecidos que no viste. " +
            "Mide tu capacidad de evitar falsas memorias.",
    )

    val EX13_RECONSTRUCCION = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX13_RECONSTRUCCION,
        subcapacity = MemorySubcapacity.METAMEMORY_CALIBRATION,
        module = MemoryModule.M6_PROSPECTIVE_META,
        paradigm = MemoryParadigm.CONSTRUCTIVE_RECALL,
        relevantAxes = setOf(
            MemoryDifficultyAxis.LOAD,
            MemoryDifficultyAxis.DELAY,
            MemoryDifficultyAxis.INTERFERENCE,
            MemoryDifficultyAxis.DEPTH,
            MemoryDifficultyAxis.RETRIEVAL,
        ),
        governingAxis = MemoryDifficultyAxis.LOAD,
        governingMetric = MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY,
        primaryMetrics = setOf(
            MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY,
            MemoryMetricKey.DELAYED_RECALL_ACCURACY,
            MemoryMetricKey.CALIBRATION_INDEX,
            MemoryMetricKey.INTRUSION_RATE,
        ),
        practiceEffectRisk = PracticeEffectRisk.LOW,
        displayName = "Reconstrucción Parcial",
        plainDescription = "Completa información estudiada a partir de fragmentos con huecos. " +
            "Mide tu recuperación constructiva y tu calibración.",
    )

    val EX14_DUAL_TASK = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX14_DUAL_TASK,
        subcapacity = MemorySubcapacity.WORKING_MEMORY,
        module = MemoryModule.M1_EXECUTIVE_WM,
        paradigm = MemoryParadigm.DUAL_TASK,
        relevantAxes = setOf(
            MemoryDifficultyAxis.CONCURRENCY,
            MemoryDifficultyAxis.LOAD,
            MemoryDifficultyAxis.SIMILARITY,
            MemoryDifficultyAxis.DELAY,
        ),
        governingAxis = MemoryDifficultyAxis.CONCURRENCY,
        governingMetric = MemoryMetricKey.DUAL_TASK_COST,
        primaryMetrics = setOf(
            MemoryMetricKey.DUAL_TASK_COST,
            MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY,
            MemoryMetricKey.RETRIEVAL_SPEED,
        ),
        practiceEffectRisk = PracticeEffectRisk.MEDIUM,
        displayName = "Dual-Task Recall",
        plainDescription = "Recupera lo memorizado mientras realizas otra tarea a la vez. " +
            "Mide tu recuperación en condiciones realistas, no ideales.",
    )

    val EX15_NARRATIVA_AUDITIVA = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX15_NARRATIVA_AUDITIVA,
        subcapacity = MemorySubcapacity.AUDITORY_VERBAL,
        module = MemoryModule.M2_DEEP_ENCODING,
        paradigm = MemoryParadigm.NARRATIVE_RECALL,
        relevantAxes = setOf(
            MemoryDifficultyAxis.LOAD,
            MemoryDifficultyAxis.DELAY,
            MemoryDifficultyAxis.INTERFERENCE,
            MemoryDifficultyAxis.DEPTH,
        ),
        governingAxis = MemoryDifficultyAxis.LOAD,
        governingMetric = MemoryMetricKey.DELAYED_RECALL_ACCURACY,
        primaryMetrics = setOf(
            MemoryMetricKey.DELAYED_RECALL_ACCURACY,
            MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY,
            MemoryMetricKey.INTRUSION_RATE,
        ),
        practiceEffectRisk = PracticeEffectRisk.LOW,
        displayName = "Memoria Auditiva Narrativa",
        plainDescription = "Escucha una narración y responde sobre su contenido, orden y " +
            "detalles. Entrena la escucha profunda de reuniones y clases.",
    )

    val EX16_CALIBRACION_CONFIANZA = MemoryExerciseDefinition(
        id = MemoryExerciseId.EX16_CALIBRACION_CONFIANZA,
        subcapacity = MemorySubcapacity.METAMEMORY_CALIBRATION,
        module = MemoryModule.M6_PROSPECTIVE_META,
        paradigm = MemoryParadigm.METACOGNITIVE_CALIBRATION,
        relevantAxes = setOf(
            MemoryDifficultyAxis.DELAY,
            MemoryDifficultyAxis.RETRIEVAL,
        ),
        governingAxis = MemoryDifficultyAxis.DELAY,
        governingMetric = MemoryMetricKey.CALIBRATION_INDEX,
        primaryMetrics = setOf(
            MemoryMetricKey.CALIBRATION_INDEX,
            MemoryMetricKey.BRIER_SCORE,
            MemoryMetricKey.GAMMA_CORRELATION,
            MemoryMetricKey.CONFIDENCE_BIAS,
            MemoryMetricKey.METACOGNITIVE_RESOLUTION,
        ),
        practiceEffectRisk = PracticeEffectRisk.LOW,
        displayName = "Calibración de Confianza",
        plainDescription = "Tras cada respuesta indicas cuán seguro estás; el sistema alinea tu " +
            "confianza con tu acierto real. Overlay transversal a los demás ejercicios.",
    )

    /** All 16 definitions, indexed by id. */
    val all: Map<MemoryExerciseId, MemoryExerciseDefinition> = listOf(
        EX1_SPAN_COMPLEJO, EX2_ACTUALIZACION_SET, EX3_LISTA_ESTRATEGIA, EX4_PARES_ASOCIADOS,
        EX5_GRID_ESPACIAL, EX6_DETECCION_CAMBIO, EX7_SECUENCIAS, EX8_NOMBRES_ROSTROS,
        EX9_INSTRUCCIONES, EX10_PROSPECTIVA, EX11_INTERFERENCIA, EX12_DISCRIMINACION_FINA,
        EX13_RECONSTRUCCION, EX14_DUAL_TASK, EX15_NARRATIVA_AUDITIVA, EX16_CALIBRACION_CONFIANZA,
    ).associateBy { it.id }

    /** Definition for [id]; throws if somehow missing (catalogue is exhaustive). */
    fun definition(id: MemoryExerciseId): MemoryExerciseDefinition =
        requireNotNull(all[id]) { "No catalogue definition for $id." }

    /** Exercises belonging to [module]. */
    fun byModule(module: MemoryModule): List<MemoryExerciseDefinition> =
        all.values.filter { it.module == module }

    /** Exercises that primarily train [subcapacity]. */
    fun bySubcapacity(subcapacity: MemorySubcapacity): List<MemoryExerciseDefinition> =
        all.values.filter { it.subcapacity == subcapacity }

    /**
     * Exercises implemented end-to-end and selectable today. V1 first wave
     * (confirmed scope 2026-06-06): Span Complejo (WM), Lista con Estrategia
     * (episódica) y Pares Asociados (asociativa) — las subcapacidades de mayor
     * impacto funcional, suficientes para validar el pipeline completo.
     */
    val implemented: List<MemoryExerciseId> = listOf(
        MemoryExerciseId.EX1_SPAN_COMPLEJO,
        MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
        MemoryExerciseId.EX4_PARES_ASOCIADOS,
    )
}
