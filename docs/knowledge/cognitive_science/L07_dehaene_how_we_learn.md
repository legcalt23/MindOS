---
title: "How We Learn: the four pillars of learning (popular synthesis)"
authors: "Dehaene, S."
year: 2020
tier: T3
domains: [attention, learning_strategy]
constructs: [attention, active_engagement, error_feedback, consolidation, prediction_error]
paradigms: []
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/coach
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreStateMachine.kt
evidence_level: moderate
---

## (a) Tesis central

**(Divulgación de alta calidad, por un neurocientífico en activo.)** Dehaene sintetiza la ciencia del
aprendizaje en **cuatro pilares**: (1) **atención** (seleccionar la señal relevante), (2) **compromiso
activo** (curiosidad, generar y probar hipótesis; el cerebro pasivo aprende poco), (3) **feedback de
error** (la señal de sorpresa/error de predicción impulsa el aprendizaje) y (4) **consolidación**
(automatización con la práctica y el sueño). Marco integrador útil para diseño instruccional.

## (b) Claims RESPALDADOS (por la literatura primaria subyacente)

- La atención modula qué se aprende; sin atención a la señal correcta, la práctica es ineficaz.
  [respaldo T1: [[F03_attention_networks_posner]]]
- El error/sorpresa (error de predicción) es un motor de aprendizaje; el feedback correctivo es esencial.
  [neurociencia del aprendizaje por refuerzo; convergente con L01]
- La consolidación depende del tiempo y del sueño. [respaldo T1: [[S01_sleep_and_cognition_walker]],
  [[F06_neuroplasticity_realistic]]]

## (c) Claims NO respaldados o refutados

- Es **divulgación**: los cuatro pilares son una organización pedagógica útil, no una teoría cerrada; la
  fuerza de cada claim depende de su literatura primaria.
- No respalda transferencia cognitiva general ni "entrenar el cerebro"; es sobre cómo se aprende
  contenido/habilidades.
- El neuromito debe evitarse: Dehaene mismo advierte contra sobre-interpretar la neurociencia.

## (d) Estado de replicación / críticas

Contenido sólido por descansar en literatura establecida (atención, error de predicción, consolidación).
Como T3, su valor es de **marco de diseño**, no de evidencia primaria. Cada pilar remite a T1 del corpus.

## (e) Implicaciones de diseño para MindBoost

- **Los cuatro pilares mapean el bucle nuclear de MindBoost:** atención (módulo Enfoque), compromiso
  activo (ejercicios interactivos, no consumo pasivo), feedback de error (feedback inmediato tras cada
  respuesta) y consolidación (spacing/periodización). Buen marco integrador para el coach y el diseño de
  sesión.
- **Pilar de feedback de error** ⇒ diseñar el feedback como señal correctiva informativa, no solo un
  marcador; conecta con la política de scoring honesto (`ScoreStateMachine`): el error es información,
  no castigo.
- **Uso permitido:** organizar la pedagogía del producto y el discurso del coach. **No permitido:**
  citarlo como prueba de eficacia; usar los T1 subyacentes.

## (f) Referencias

- Dehaene, S. (2020). *How we learn: Why brains learn better than any machine… for now.* Viking.
- (Pilares respaldados por literatura primaria: atención, error de predicción/refuerzo, consolidación —
  ver fichas F03, F06, S01, L01 del corpus.)
