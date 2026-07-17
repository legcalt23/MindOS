---
title: "Distributed practice and the spacing effect"
authors: "Cepeda, N. J.; Pashler, H.; Rohrer, D.; Bjork, R. A."
year: "2006–2008"
tier: T1
domains: [learning_strategy]
constructs: [spacing_effect, distributed_practice, massed_practice, retention_interval]
paradigms: [spaced_retrieval, distributed_learning]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/orchestration/TransversalDailyPlanner.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/orchestration/MindBoostProfileOrchestrator.kt
evidence_level: strong
---

## (a) Tesis central

Distribuir la práctica en el tiempo (sesiones espaciadas) produce **mejor retención a largo plazo** que
concentrarla (práctica masiva/"cramming"), aunque la práctica masiva a menudo *se siente* más eficaz en
el momento. El meta-análisis de Cepeda et al. (2006, 254 estudios) cuantifica el efecto y muestra que el
intervalo óptimo entre sesiones **escala con el intervalo de retención deseado**.

## (b) Claims RESPALDADOS

- La práctica espaciada supera a la masiva para la retención duradera. [meta-análisis, muy replicado]
- El intervalo óptimo entre repasos crece con el tiempo hasta la evaluación (para recordar a un mes,
  espaciar más que para recordar a un día). [meta-análisis — Cepeda 2008]
- El efecto es robusto a través de materiales, edades y modalidades. [meta-análisis]

## (c) Claims NO respaldados o refutados

- El spacing mejora la **retención del material practicado**, no la capacidad de memoria general.
- La percepción del aprendiz es un mal guía: la fluidez del cramming es una **ilusión de competencia**
  (conecta con [[L03_desirable_difficulties_bjork]] y [[M01_metacognition_dunlosky]]).
- No hay un único intervalo mágico universal; depende del horizonte de retención.

## (d) Estado de replicación / críticas

Es uno de los efectos más sólidos y antiguos (Ebbinghaus, 1885) de la psicología. La aplicación práctica
(algoritmos de repetición espaciada tipo SM-2/Leitner) funciona. Crítica menor: la mayoría de estudios
usan material verbal simple; en habilidades complejas el patrón se mantiene pero la calibración del
intervalo es más difícil.

## (e) Implicaciones de diseño para MindBoost

- **Base científica del planificador diario transversal** (`TransversalDailyPlanner`): distribuir la
  práctica de cada módulo/ejercicio en el tiempo tiene respaldo directo — NO es un truco de engagement,
  es diseño instruccional válido. Programar re-encuentros espaciados con ejercicios/material.
- Combinar con retrieval practice ([[L01_retrieval_practice_testing_effect]]): **recuperación espaciada**
  es la receta de mayor evidencia.
- Advertencia de honestidad: el spacing mejora *lo que se practica*; el copy debe evitar convertirlo en
  "mejora tu memoria". El valor es "recuerdas mejor lo que entrenas si lo distribuyes".
- Anti-cramming del coach: desincentivar sesiones maratón; el `MindBoostProfileOrchestrator` puede
  penalizar/omitir señal de sesiones masivas atípicas (coherente con validez de sesión).
- **Claim permitido:** "distribuir tu práctica mejora cuánto retienes". **Prohibido:** claim de
  capacidad general.

## (f) Referencias

- Cepeda, N. J., Pashler, H., Vul, E., Wixted, J. T., & Rohrer, D. (2006). Distributed practice in
  verbal recall tasks: A review and quantitative synthesis. *Psychological Bulletin, 132*(3), 354–380.
- Cepeda, N. J., Vul, E., Rohrer, D., Wixted, J. T., & Pashler, H. (2008). Spacing effects in learning:
  A temporal ridgeline of optimal retention. *Psychological Science, 19*(11), 1095–1102.
