package com.mindos.app.features.mindboost.memory.domain.transfer

import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity.ASSOCIATIVE
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity.AUDITORY_VERBAL
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity.DELAYED_RETRIEVAL
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity.EPISODIC
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity.INTERFERENCE_RESISTANCE
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity.METAMEMORY_CALIBRATION
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity.PROSPECTIVE
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity.SEMANTIC_FUNCTIONAL
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity.SEQUENTIAL
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity.WORKING_MEMORY

/**
 * A declarative functional-transfer micro-protocol (doc §11): an explicit bridge
 * between a trained capacity and a real-world situation. Pure metadata (like the
 * exercise catalogue) — no runtime here. [v1Available] marks the protocols that the
 * V1 content (words/pairs of EX1/EX3/EX4) can already support; the rest need later
 * exercises (faces, narrative, instructions, prospective…).
 */
data class TransferProtocol(
    val id: String,
    val title: String,
    val linkedSubcapacities: Set<MemorySubcapacity>,
    val bridge: String,
    val measurement: String,
    val v1Available: Boolean,
) {
    init {
        require(id.isNotBlank()) { "TransferProtocol id cannot be blank." }
        require(title.isNotBlank()) { "TransferProtocol title cannot be blank." }
        require(linkedSubcapacities.isNotEmpty()) { "A protocol must link at least one sub-capacity." }
    }
}

/** The 10 functional-transfer micro-protocols of doc §11. */
object TransferProtocols {

    val STUDY = TransferProtocol(
        id = "study_efficient",
        title = "Estudio eficiente",
        linkedSubcapacities = setOf(EPISODIC, SEMANTIC_FUNCTIONAL, DELAYED_RETRIEVAL),
        bridge = "Codificar y autoevaluar 'apuntes' (conceptos/datos) con codificación elaborativa.",
        measurement = "Retención a 24h del material académico vs. material genérico.",
        v1Available = true,
    )

    val ACTIVE_READING = TransferProtocol(
        id = "active_reading",
        title = "Lectura activa",
        linkedSubcapacities = setOf(EPISODIC, WORKING_MEMORY, SEMANTIC_FUNCTIONAL),
        bridge = "Leer un párrafo, cerrarlo y responder preguntas de comprensión y detalle.",
        measurement = "Precisión de recuerdo y comprensión; beneficio de la codificación entrenada.",
        v1Available = false,
    )

    val VOCABULARY = TransferProtocol(
        id = "language_vocabulary",
        title = "Aprendizaje de idiomas (vocabulario)",
        linkedSubcapacities = setOf(ASSOCIATIVE, DELAYED_RETRIEVAL),
        bridge = "Pares L1-L2 con keyword method y repaso espaciado adaptativo.",
        measurement = "Retención de vocabulario nuevo a 24h y 7 días.",
        v1Available = true,
    )

    val NAMES_FACES = TransferProtocol(
        id = "names_faces_context",
        title = "Nombres y rostros en contexto",
        linkedSubcapacities = setOf(ASSOCIATIVE),
        bridge = "Simular networking: perfil (rostro+nombre+dato) y saludo por nombre al reaparecer.",
        measurement = "Precisión de naming y uso de datos contextuales entre sesiones.",
        v1Available = false,
    )

    val MEETINGS = TransferProtocol(
        id = "work_meetings",
        title = "Reuniones de trabajo",
        linkedSubcapacities = setOf(AUDITORY_VERBAL, EPISODIC, PROSPECTIVE),
        bridge = "Audio de reunión: retener decisiones, tareas asignadas, plazos y preguntas.",
        measurement = "Precisión por tipo de información; retención a 1h y 24h.",
        v1Available = false,
    )

    val INSTRUCTIONS = TransferProtocol(
        id = "real_instructions",
        title = "Instrucciones complejas del mundo real",
        linkedSubcapacities = setOf(WORKING_MEMORY, SEQUENTIAL, PROSPECTIVE),
        bridge = "Ejecutar instrucciones condicionales recibidas, en orden, sin re-leer.",
        measurement = "Instrucciones correctas, errores de orden y de condición.",
        v1Available = false,
    )

    val PRESENTATIONS = TransferProtocol(
        id = "presentations",
        title = "Presentaciones",
        linkedSubcapacities = setOf(SEQUENTIAL, EPISODIC),
        bridge = "Preparar 3-5 puntos con método de loci y 'darlos' en orden tras una demora.",
        measurement = "Completitud, orden, riqueza de detalle y degradación con demora.",
        v1Available = false,
    )

    val DECISIONS = TransferProtocol(
        id = "decisions_under_pressure",
        title = "Toma de decisiones bajo presión",
        linkedSubcapacities = setOf(WORKING_MEMORY, INTERFERENCE_RESISTANCE, METAMEMORY_CALIBRATION),
        bridge = "Recordar datos clave (con distractores) bajo presión temporal y justificar.",
        measurement = "Datos clave recordados, distractores intruidos, calibración.",
        v1Available = false,
    )

    val PENDING_TASKS = TransferProtocol(
        id = "pending_tasks",
        title = "Tareas pendientes",
        linkedSubcapacities = setOf(PROSPECTIVE, SEQUENTIAL),
        bridge = "Recordar al final de la sesión 3-5 'pendientes' dados al inicio, sin aviso.",
        measurement = "Tareas recordadas, omitidas, orden y coste en el rendimiento.",
        v1Available = false,
    )

    val DIGITAL_DISTRACTION = TransferProtocol(
        id = "digital_distraction_resistance",
        title = "Resistencia a distracciones digitales",
        linkedSubcapacities = setOf(WORKING_MEMORY, INTERFERENCE_RESISTANCE, PROSPECTIVE),
        bridge = "Codificar manteniendo el foco pese a 'notificaciones' simuladas en pantalla.",
        measurement = "Coste de distracción y recuperación post-interrupción.",
        v1Available = false,
    )

    val all: List<TransferProtocol> = listOf(
        STUDY, ACTIVE_READING, VOCABULARY, NAMES_FACES, MEETINGS,
        INSTRUCTIONS, PRESENTATIONS, DECISIONS, PENDING_TASKS, DIGITAL_DISTRACTION,
    )

    /** Protocols supportable with the current V1 content (words/pairs). */
    val v1Available: List<TransferProtocol> = all.filter { it.v1Available }

    fun forSubcapacity(subcapacity: MemorySubcapacity): List<TransferProtocol> =
        all.filter { subcapacity in it.linkedSubcapacities }
}
