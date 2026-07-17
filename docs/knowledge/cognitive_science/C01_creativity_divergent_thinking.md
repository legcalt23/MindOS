---
title: "Creativity: divergent thinking, measurement and the training question"
authors: "Guilford, J. P.; Torrance, E. P.; Runco, M. A.; Silvia, P. J.; Baer, J."
year: "1967–2016"
tier: T2
domains: [creativity]
constructs: [divergent_thinking, originality, fluency, flexibility, remote_associates, domain_specificity]
paradigms: [alternate_uses, torrance_tests, remote_associates_test]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/creativity/domain/exercise/CreativityExerciseCatalog.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/creativity/domain/scoring/CreativityRubricScorer.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/model/CognitiveTaxonomy.kt
evidence_level: weak
---

## (a) Tesis central

La creatividad se operacionaliza a menudo como **pensamiento divergente** (generar muchas ideas
variadas y originales, p. ej. "usos alternativos de un ladrillo"), medido por fluidez, flexibilidad,
originalidad y elaboración (Guilford, Torrance). Es **distinta de la inteligencia** (correlación
modesta) y fuertemente **específica de dominio**. La medición es notoriamente difícil (subjetividad de
la originalidad, dependencia del evaluador), y la evidencia de que la creatividad se **entrena de forma
transferible** es débil.

## (b) Claims RESPALDADOS

- El pensamiento divergente es medible (fluidez/flexibilidad/originalidad/elaboración) y **parcialmente
  distinto** de la inteligencia (correlación modesta). [factor-analítico/correlacional] La *hipótesis
  del umbral* (haría falta una inteligencia mínima y por encima la correlación caería) es **un modelo
  discutido**, con réplicas fallidas (Karwowski et al. 2016) — no un hecho establecido.
- La creatividad es **específica de dominio**: alguien creativo en escritura no lo es necesariamente en
  matemáticas. [correlacional — Baer]
- Ciertas técnicas (p. ej. instrucción de "sé original", brainstorming estructurado, tiempo/exploración)
  aumentan la **producción divergente en la tarea**. [experimental — efectos locales]

## (c) Claims NO respaldados o refutados

- La evidencia de **transferencia** del entrenamiento de creatividad a creatividad general o a
  rendimiento creativo del mundo real es **débil**; mejorar en "usos alternativos" no te hace más
  creativo en general.
- La **puntuación de originalidad es subjetiva** y depende del evaluador y del baseline de la muestra
  (lo "original" es relativo a lo que otros dijeron) — reto psicométrico serio.
- No respalda claims de "desbloquea/aumenta tu creatividad" como capacidad transferible.

## (d) Estado de replicación / críticas

La medición de creatividad tiene problemas de fiabilidad y validez reconocidos (la puntuación de
originalidad es especialmente inestable). Enfoques modernos (subjective scoring / Consensual Assessment
Technique de Amabile; snapshot scoring; modelos semánticos de distancia) mejoran algo la fiabilidad pero
no resuelven la especificidad de dominio ni la transferencia. Es de los dominios con evidencia **más
débil** para el entrenamiento — de ahí `evidence_level: weak`.

## (e) Implicaciones de diseño para MindBoost

- **Fija expectativas honestas para el módulo Creatividad** (`CreativityExerciseCatalog`,
  `CreativityRubricScorer`): se puede entrenar y medir *producción divergente en la tarea* (fluidez,
  flexibilidad, originalidad relativa), pero NO prometer "más creatividad" general — el charter y esta
  ficha lo prohíben. La dimensión `CREATIVITY` del perfil debe mantenerse conservadora (fácil a
  `CALIBRATING`).
- **Reto de scoring:** la originalidad es relativa a una muestra; un `CreativityRubricScorer` honesto
  debe reconocer la subjetividad (rúbrica explícita, o distancia semántica) y NO presentar un número de
  "creatividad" con falsa precisión. Remit del psychometrician + efficacy-guardian.
- **Especificidad de dominio** ⇒ ejercicios y feedback ceñidos al tipo de tarea practicada (verbal,
  visual…), sin agregarlos en una "creatividad" única.
- **Claim permitido:** "práctica de generar ideas variadas y originales en esta tarea". **Prohibido:**
  "aumenta/desbloquea tu creatividad" como rasgo general transferible.

## (f) Referencias

- Guilford, J. P. (1967). *The nature of human intelligence.* McGraw-Hill.
- Torrance, E. P. (1974). *Torrance Tests of Creative Thinking: Norms-technical manual.* Scholastic
  Testing Service.
- Runco, M. A., & Acar, S. (2012). Divergent thinking as an indicator of creative potential. *Creativity
  Research Journal, 24*(1), 66–75.
- Karwowski, M., et al. (2016). Is creativity without intelligence possible? Evidence from the threshold
  hypothesis. *Intelligence, 57*, 105–117.
- Baer, J. (2016). *Domain specificity of creativity.* Academic Press.
- Silvia, P. J., et al. (2008). Assessing creativity with divergent thinking tasks. *Psychology of
  Aesthetics, Creativity, and the Arts, 2*(2), 68–85.
