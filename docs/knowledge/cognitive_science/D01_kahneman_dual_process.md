---
title: "Thinking, Fast and Slow: dual-process theory and the replication caveat"
authors: "Kahneman, D.; Tversky, A."
year: "1974–2011"
tier: T3
domains: [decision_making]
constructs: [system1_system2, heuristics_and_biases, dual_process, priming, prospect_theory]
paradigms: []
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/domain
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/domain/scoring/ReversibilityScorer.kt
evidence_level: contested
---

## (a) Tesis central

**(Libro de divulgación T3; la obra primaria de Kahneman & Tversky es T1.)** Distingue dos modos de
pensamiento: **Sistema 1** (rápido, automático, intuitivo, propenso a sesgos) y **Sistema 2** (lento,
deliberado, costoso). El programa "heurísticos y sesgos" documentó desviaciones sistemáticas de la
racionalidad (anclaje, disponibilidad, representatividad, aversión a la pérdida). La teoría prospectiva
(1979) es su núcleo T1 y le valió el Nobel.

## (b) Claims RESPALDADOS

- Existen sesgos cognitivos sistemáticos y replicables (anclaje, encuadre, aversión a la pérdida,
  disponibilidad). [experimental, robusto — T1]
- La teoría prospectiva describe bien decisiones bajo riesgo (asimetría ganancias/pérdidas, ponderación
  no lineal de probabilidades). [experimental, muy replicado — T1]
- La distinción rápido/lento es un marco descriptivo útil de la cognición de la decisión. [teoría]

## (c) Claims NO respaldados o refutados

- **Crisis de replicación del priming social/conductual:** varios estudios de priming citados en el
  libro **no se replican**; el propio Kahneman reconoció públicamente (2017) que confió demasiado en
  esa literatura ("you have to admit the ship is sinking"). No usar claims de priming.
- La dicotomía Sistema 1/2 está **sobre-simplificada**; muchos investigadores la ven como metáfora, no
  como dos sistemas neuronales reales.
- No respalda que se pueda "entrenar para eliminar sesgos" de forma general y duradera (debiasing
  robusto es difícil).

## (d) Estado de replicación / críticas

Núcleo T1 (prospect theory, sesgos clásicos) sólido; partes del libro (priming) afectadas por la crisis
de replicación. Es el ejemplo perfecto de por qué un T3 —aun escrito por un Nobel— se cita con cuidado y
se rastrea a la evidencia primaria. Debiasing: la evidencia de intervenciones que reducen sesgos es
mixta y específica.

## (e) Implicaciones de diseño para MindBoost

- **Fundamenta el módulo Decisiones** (Bayes Arena, `ReversibilityScorer`, triaje de reversibilidad):
  entrenar a distinguir decisiones reversibles de irreversibles y a razonar probabilísticamente es
  aplicar Sistema 2 deliberado — con respaldo como *habilidad de decisión practicable*.
- **Honestidad:** no prometer "elimina tus sesgos" (debiasing general no está probado); el claim honesto
  es "práctica de razonar de forma más deliberada y calibrada en decisiones concretas".
- Evitar claims de priming en cualquier feature; usar solo los efectos replicados (encuadre, anclaje,
  aversión a la pérdida) si se diseñan ejercicios sobre sesgos.
- **Claim permitido:** "práctica de decisión deliberada y calibrada". **Prohibido:** "elimina tus sesgos
  cognitivos" / claims basados en priming.

## (f) Referencias

- Kahneman, D. (2011). *Thinking, fast and slow.* Farrar, Straus and Giroux. [T3]
- Kahneman, D., & Tversky, A. (1979). Prospect theory: An analysis of decision under risk.
  *Econometrica, 47*(2), 263–291. [T1]
- Tversky, A., & Kahneman, D. (1974). Judgment under uncertainty: Heuristics and biases. *Science,
  185*(4157), 1124–1131. [T1]
