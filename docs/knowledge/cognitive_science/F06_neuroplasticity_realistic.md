---
title: "Synaptic plasticity and the realistic bounds of adult learning"
authors: "Kandel, E. R.; et al. (Principles of Neural Science)"
year: "2000–2021"
tier: T2
domains: [working_memory, learning_strategy]
constructs: [synaptic_plasticity, long_term_potentiation, memory_consolidation, learning_specificity, neurogenesis]
paradigms: []
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/orchestration/MindBoostProfileOrchestrator.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreStateMachine.kt
evidence_level: strong
---

## (a) Tesis central

El aprendizaje modifica el cerebro a nivel sináptico: la **potenciación a largo plazo (LTP)** y la
depresión a largo plazo cambian la fuerza de las conexiones; el trabajo de Kandel en *Aplysia* mostró
la base molecular de la memoria (cambios de corto plazo por modificación de proteínas existentes; de
largo plazo por síntesis de proteínas y crecimiento sináptico nuevo). El cerebro adulto es plástico
—pero la plasticidad es **específica** de lo que se practica y está acotada.

## (b) Claims RESPALDADOS

- El aprendizaje y la memoria tienen sustrato sináptico (LTP/LTD, consolidación dependiente de
  síntesis proteica). [neurociencia celular, robusto]
- La consolidación de la memoria a largo plazo requiere tiempo y depende del sueño y la repetición
  espaciada. [experimental — ver [[S01_sleep_and_cognition_walker]], [[L02_spacing_distributed_practice]]]
- La plasticidad experiencia-dependiente persiste en la adultez: la práctica reorganiza
  representaciones corticales de las habilidades entrenadas. [neuroimagen/animal]

## (c) Claims NO respaldados o refutados

- **La plasticidad NO es ilimitada ni general.** Que el cerebro "se pueda cambiar" no implica que
  cualquier entrenamiento produzca mejoras amplias transferibles: la reorganización es
  **específica de la tarea/estímulo** entrenado.
- **"Neuroplasticidad" no es una licencia de marketing.** Invocar plasticidad para prometer mejoras de
  capacidad general es exactamente el uso vago de neurociencia que la frontera de honestidad prohíbe.
- La neurogénesis adulta en humanos (hipocampo) es real pero modesta y su relación causal con mejoras
  cognitivas en personas sanas está lejos de establecida.

## (d) Estado de replicación / críticas

La base celular del aprendizaje (LTP, cascada de Kandel) es de las más sólidas de la neurociencia
(Nobel 2000). La sobre-extensión popular —"reconecta tu cerebro", "desbloquea tu potencial"— es donde
está el problema: es un salto injustificado desde "las sinapsis cambian con la práctica" hasta
"nuestro producto mejora tu cerebro en general". La especificidad del aprendizaje es la regla, no la
excepción.

## (e) Implicaciones de diseño para MindBoost

- **Justifica el modelo de progreso "frente a tu propia línea base":** la plasticidad específica
  predice que el usuario mejorará *en las tareas que practica* — eso es lo honesto de medir y celebrar,
  no una transferencia general fabricada.
- **Prohíbe el lenguaje de neurociencia vaga** en producto: nada de "reconecta/reprograma tu cerebro".
  Esta ficha es el respaldo científico de `ForbiddenLexicon`.
- La dependencia de consolidación (sueño, spacing) informa la **periodización** del plan diario
  (`MindBoostProfileOrchestrator` / planner): distribuir la práctica en el tiempo tiene base
  neurobiológica, no es un truco de engagement.
- **Claim permitido:** "con práctica, mejoras en lo que entrenas". **Prohibido:** "aprovecha la
  neuroplasticidad para potenciar tu cerebro".

## (f) Referencias

- Kandel, E. R., Schwartz, J. H., Jessell, T. M., Siegelbaum, S. A., & Hudspeth, A. J. (2021).
  *Principles of Neural Science* (6th ed.). McGraw-Hill. [Cap. sobre plasticidad sináptica y memoria]
- Kandel, E. R. (2001). The molecular biology of memory storage: a dialogue between genes and
  synapses. *Science, 294*(5544), 1030–1038.
- Lövdén, M., Bäckman, L., Lindenberger, U., Schaefer, S., & Schmiedek, F. (2010). A theoretical
  framework for the study of adult cognitive plasticity. *Psychological Bulletin, 136*(4), 659–676.
