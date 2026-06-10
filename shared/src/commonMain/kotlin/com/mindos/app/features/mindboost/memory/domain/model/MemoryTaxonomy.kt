package com.mindos.app.features.mindboost.memory.domain.model

/**
 * Stable taxonomy of the MindBoost · Memoria (memory) training system. These
 * enums are the backbone of the cognitive model: every exercise declares which
 * [MemorySubcapacity] it trains, which [MemoryModule] it belongs to, and which
 * scientific [MemoryParadigm] it descends from. Adding a value here is a
 * deliberate, documented act — they are referenced across content generation,
 * metrics, adaptation, evaluation and reporting.
 *
 * Source of truth: memoria_sistema_entrenamiento.md (§2 subcapacidades, §3
 * módulos, §4 catálogo de ejercicios). Mirrors the focus feature's
 * CognitiveTaxonomy.kt convention.
 */

/**
 * The 12 independent (but interconnected) memory sub-capacities the system
 * trains and measures separately (doc §2). They are NOT a single "memory"
 * capacity — reporting is always per sub-capacity (doc §13.15).
 */
enum class MemorySubcapacity {
    /** Working memory: hold + manipulate under concurrent processing (Baddeley). */
    WORKING_MEMORY,

    /** Episodic: encode/retrieve events with their context (what/where/when/order). */
    EPISODIC,

    /** Functional semantic: integrate new info into existing knowledge networks. */
    SEMANTIC_FUNCTIONAL,

    /** Associative: bind unrelated elements (name-face, word-image, concept-source). */
    ASSOCIATIVE,

    /** Visual: encode/retain visual detail (shapes, colours, scenes, change). */
    VISUAL,

    /** Spatial: locations, configuration, navigated routes (Corsi, method of loci). */
    SPATIAL,

    /** Auditory/verbal: information arriving by the auditory/verbal channel. */
    AUDITORY_VERBAL,

    /** Sequential: temporal order of events or items (serial order). */
    SEQUENTIAL,

    /** Prospective: remembering to do something in the future (time/event based). */
    PROSPECTIVE,

    /** Delayed retrieval: recall after a filled delay — the truest learning signal. */
    DELAYED_RETRIEVAL,

    /** Interference resistance: keep targets accessible despite competing material. */
    INTERFERENCE_RESISTANCE,

    /** Metamemory & confidence calibration: knowing what you know. */
    METAMEMORY_CALIBRATION,
}

/**
 * Cognitive module grouping a family of exercises (the 6 modules of doc §3).
 * Each module owns dedicated exercises and shares cross-cutting metrics.
 */
enum class MemoryModule {
    /** Módulo 1: Memoria de Trabajo Ejecutiva. */
    M1_EXECUTIVE_WM,

    /** Módulo 2: Codificación Profunda y Memoria Episódica. */
    M2_DEEP_ENCODING,

    /** Módulo 3: Memoria Asociativa y Vinculación. */
    M3_ASSOCIATIVE,

    /** Módulo 4: Memoria Espacial y Visual. */
    M4_SPATIAL_VISUAL,

    /** Módulo 5: Resistencia, Interferencia y Recuperación Bajo Presión. */
    M5_RESISTANCE,

    /** Módulo 6: Memoria Prospectiva y Metacognición. */
    M6_PROSPECTIVE_META,
}

/** Catalogue identity of each exercise (the 16 of doc §4). */
enum class MemoryExerciseId {
    EX1_SPAN_COMPLEJO,
    EX2_ACTUALIZACION_SET,
    EX3_LISTA_ESTRATEGIA,
    EX4_PARES_ASOCIADOS,
    EX5_GRID_ESPACIAL,
    EX6_DETECCION_CAMBIO,
    EX7_SECUENCIAS,
    EX8_NOMBRES_ROSTROS,
    EX9_INSTRUCCIONES,
    EX10_PROSPECTIVA,
    EX11_INTERFERENCIA,
    EX12_DISCRIMINACION_FINA,
    EX13_RECONSTRUCCION,
    EX14_DUAL_TASK,
    EX15_NARRATIVA_AUDITIVA,

    /** Transversal overlay, not a standalone exercise (doc §4 Ej.16). */
    EX16_CALIBRACION_CONFIANZA,
}

/** Scientific paradigm an exercise is modelled on (documents its lineage). */
enum class MemoryParadigm {
    COMPLEX_SPAN,
    RUNNING_MEMORY,
    FREE_RECALL,
    PAIRED_ASSOCIATES,
    SPATIAL_GRID,
    CHANGE_DETECTION,
    SERIAL_ORDER,
    FACE_NAME,
    INSTRUCTION_FOLLOWING,
    PROSPECTIVE_MEMORY,
    AB_INTERFERENCE,
    MNEMONIC_SIMILARITY,
    CONSTRUCTIVE_RECALL,
    DUAL_TASK,
    NARRATIVE_RECALL,
    METACOGNITIVE_CALIBRATION,
}

/**
 * Operating mode — the critical separation between training and measuring
 * (doc §6 principio anti-engaño, §9 evaluación).
 *
 * - [TRAINING]: adaptive difficulty, learning is the goal.
 * - [EVALUATION]: fixed difficulty, reserved (untrained) probes, no adaptation,
 *   measures real change with new material + delayed recall.
 * - [CALIBRATION]: finds the individual baseline and initial thresholds (doc §9).
 *
 * The adaptive engine MUST only run in [TRAINING].
 */
enum class Mode {
    TRAINING,
    EVALUATION,
    CALIBRATION,
}

/**
 * Classified result of a single memory trial. Richer than a go/no-go task
 * because memory errors are diagnostic: a binding error (right item, wrong
 * slot) is a different failure from an intrusion (item from another list).
 */
enum class MemoryTrialOutcome {
    /** Correct retrieval of a studied target. */
    HIT,

    /** Correctly rejected a non-studied item (true negative). */
    CORRECT_REJECTION,

    /** A target was studied but not retrieved. */
    MISS,

    /** Accepted/produced a non-studied item as if studied (commission). */
    FALSE_ALARM,

    /** Produced an item from another list/block or extra-list (source error). */
    INTRUSION,

    /** Mixed components of different pairs (e.g. A's cue with B's response). */
    CONJUNCTION_ERROR,

    /** Correct item recalled in the wrong location/slot/frame. */
    BINDING_ERROR,

    /** Correct items recalled in the wrong serial order (transposition). */
    ORDER_ERROR,

    /** The item was not attempted (left blank). */
    OMISSION,

    /** The response window elapsed with no response. */
    TIMEOUT,
}
