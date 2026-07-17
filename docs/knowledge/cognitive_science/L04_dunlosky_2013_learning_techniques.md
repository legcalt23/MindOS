---
title: "Improving students' learning with effective learning techniques"
authors: "Dunlosky, J.; Rawson, K. A.; Marsh, E. J.; Nathan, M. J.; Willingham, D. T."
year: 2013
tier: T1
domains: [learning_strategy, metacognition]
constructs: [study_techniques, utility_assessment, practice_testing, distributed_practice]
paradigms: [practice_testing, distributed_practice, interleaving, elaborative_interrogation]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/coach
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/orchestration/TransversalDailyPlanner.kt
evidence_level: strong
---

## (a) Tesis central

Revisión que evalúa **10 técnicas de estudio** por su utilidad, generalidad y respaldo empírico. Veredicto:
la **práctica de recuperación (practice testing)** y la **práctica distribuida (spacing)** son de **alta
utilidad** (funcionan a través de materiales, edades y contextos); el interleaving y la interrogación
elaborativa/auto-explicación son de utilidad **moderada**; y las técnicas más populares —**releer** y
**subrayar/resaltar**— son de **baja utilidad** pese a ser las más usadas.

## (b) Claims RESPALDADOS

- Practice testing y distributed practice: **alta utilidad**, amplio respaldo y generalidad. [revisión
  sistemática]
- Interleaving, elaborative interrogation, self-explanation: **utilidad moderada** (funcionan en
  condiciones acotadas). [revisión sistemática]
- Releer, resaltar, resumir, mnemónicos de palabra clave, imaginería: **baja utilidad** o muy limitada.
  [revisión sistemática]

## (c) Claims NO respaldados o refutados

- Popularidad ≠ eficacia: las técnicas más usadas por estudiantes (releer, subrayar) son de las menos
  efectivas. Ningún producto debe recomendar por defecto lo que "parece" estudiar.
- Estas técnicas mejoran el **aprendizaje de contenido**; no elevan capacidad cognitiva general.
- La utilidad "moderada" es condicional: el interleaving ayuda cuando hay categorías que discriminar,
  no siempre.

## (d) Estado de replicación / críticas

Es una síntesis de referencia, muy citada y consistente con la evidencia posterior. Su fuerza es
traducir la ciencia a recomendaciones accionables jerarquizadas por evidencia. Crítica: la implementación
efectiva por parte de estudiantes reales es difícil (requiere metacognición y autorregulación — ver
[[M02_self_regulated_learning_zimmerman]]).

## (e) Implicaciones de diseño para MindBoost

- **Currículo del coach basado en evidencia:** si MindBoost aconseja cómo estudiar/practicar, debe
  priorizar practice testing + spacing (alta utilidad) y desaconsejar releer/subrayar. El coach
  (`features/mindboost/coach`) tiene aquí una fuente jerarquizada y defendible.
- Refuerza el diseño del `TransversalDailyPlanner` alrededor de recuperación espaciada e interleaving
  (combina [[L01_retrieval_practice_testing_effect]] + [[L02_spacing_distributed_practice]]).
- **Honestidad:** presentar estas técnicas como formas de aprender mejor *lo que estudias*, no como
  "mejora tu cerebro". Es guía de aprendizaje, no claim cognitivo.
- Oportunidad de producto de alto valor y bajo riesgo: un coach de estrategia de estudio con respaldo
  T1, claramente separado de los claims de entrenamiento cognitivo.

## (f) Referencias

- Dunlosky, J., Rawson, K. A., Marsh, E. J., Nathan, M. J., & Willingham, D. T. (2013). Improving
  students' learning with effective learning techniques: Promising directions from cognitive and
  educational psychology. *Psychological Science in the Public Interest, 14*(1), 4–58.
