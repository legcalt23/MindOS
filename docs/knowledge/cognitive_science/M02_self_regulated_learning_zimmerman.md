---
title: "Self-regulated learning: a cyclical model (forethought–performance–reflection)"
authors: "Zimmerman, B. J.; Schunk, D. H."
year: "1989–2013"
tier: T1
domains: [metacognition, learning_strategy]
constructs: [self_regulation, forethought, performance_control, self_reflection, goal_setting, self_efficacy]
paradigms: []
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/coach
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/orchestration/TransversalDailyPlanner.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/domain
evidence_level: moderate
---

## (a) Tesis central

El aprendizaje autorregulado (SRL) es un proceso **cíclico de tres fases**: **forethought** (fijar
metas, planificar, activar la auto-eficacia antes de la tarea), **performance** (auto-observación y
control de estrategias durante), y **self-reflection** (auto-evaluación y atribución después, que
retroalimenta la siguiente vuelta). Los aprendices que autorregulan bien fijan metas específicas,
monitorean su progreso y ajustan estrategias — y este proceso es **enseñable**.

## (b) Claims RESPALDADOS

- La autorregulación (fijar metas, auto-monitorear, reflexionar) correlaciona con mejor rendimiento
  académico y es entrenable. [correlacional + intervenciones educativas]
- Las metas específicas y de proceso (vs de resultado vago) mejoran el aprendizaje y la persistencia.
  [experimental — Locke & Latham, teoría de fijación de metas]
- La auto-eficacia (creer que puedes con esfuerzo) predice esfuerzo y persistencia. [correlacional —
  Bandura]
- La reflexión estructurada tras el desempeño mejora la calibración y las decisiones futuras.
  [convergente con [[M01_metacognition_dunlosky]]]

## (c) Claims NO respaldados o refutados

- SRL mejora el **aprendizaje y la adherencia**, no la capacidad cognitiva bruta; no es un claim de
  entrenamiento cognitivo.
- La auto-eficacia mal calibrada (exceso de confianza) puede ser contraproducente; el objetivo es
  confianza *realista*, no inflada.
- No es una receta universal: la eficacia depende de la implementación y del apoyo (andamiaje).

## (d) Estado de replicación / críticas

Marco muy influyente en educación con apoyo empírico sólido, aunque los tamaños de efecto de las
intervenciones de SRL varían y dependen de la fidelidad de implementación. Es la contraparte "de
autorregulación" del cambio de conducta ([[B01_behaviour_change_wheel_michie]]).

## (e) Implicaciones de diseño para MindBoost

- **Arquitectura del coach y del bucle nuclear:** el bucle de MindBoost (baseline → plan → sesión →
  feedback → reflexión → hábito) **es** el ciclo de Zimmerman. El coach debe operar las tres fases:
  planificar la sesión (forethought), dar feedback durante/después (performance), y facilitar reflexión
  (self-reflection) — el diario de Decisiones ya materializa la fase de reflexión.
- **Fijación de metas de proceso** (no de resultado): "practica 3 sesiones espaciadas" mejor que "sé más
  inteligente". Encaja con progreso frente a la propia línea base.
- **Auto-eficacia realista:** el feedback honesto (`null ≠ 0`, sin inflar) construye confianza calibrada,
  no falsa. El planner transversal puede secuenciar metas alcanzables para sostener adherencia.
- **Claim permitido:** "te ayudamos a planificar, monitorear y reflexionar sobre tu práctica".
  **Prohibido:** presentar SRL como "mejora cognitiva".

## (f) Referencias

- Zimmerman, B. J. (2002). Becoming a self-regulated learner: An overview. *Theory into Practice,
  41*(2), 64–70.
- Zimmerman, B. J., & Schunk, D. H. (Eds.). (2011). *Handbook of self-regulation of learning and
  performance.* Routledge.
- Locke, E. A., & Latham, G. P. (2002). Building a practically useful theory of goal setting and task
  motivation. *American Psychologist, 57*(9), 705–717.
