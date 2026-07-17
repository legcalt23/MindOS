---
title: "The Cattell–Horn–Carroll (CHC) theory of cognitive abilities"
authors: "Carroll, J. B.; Cattell, R. B.; Horn, J. L.; McGrew, K. S."
year: "1993–2012"
tier: T1
domains: [logical_reasoning, verbal_reasoning, spatial_reasoning, processing_speed, working_memory]
constructs: [fluid_intelligence, crystallized_intelligence, g_factor, broad_abilities, three_stratum]
paradigms: []
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/aggregation/MindBoostProfileAggregator.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/model/CognitiveTaxonomy.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/display/UserDisplayScoreMapper.kt
evidence_level: strong
---

## (a) Tesis central

La estructura de las capacidades cognitivas humanas es **jerárquica y de tres estratos** (Carroll):
un factor general **g** en la cima, ~8–10 **capacidades amplias** en el medio (razonamiento fluido
Gf, conocimiento cristalizado Gc, memoria de trabajo/corto plazo Gsm, velocidad de procesamiento Gs,
procesamiento visual Gv, recuperación a largo plazo Glr, etc.) y decenas de capacidades estrechas
abajo. Es el marco psicométrico más consensuado y la base de los tests de inteligencia modernos.

## (b) Claims RESPALDADOS

- Las capacidades cognitivas se organizan jerárquicamente; g y las capacidades amplias emergen
  consistentemente del análisis factorial de grandes baterías. [factor-analítico, décadas de datos]
- Gf (razonamiento con material novedoso) y Gc (conocimiento adquirido) son distinguibles y siguen
  trayectorias vitales distintas: Gf declina antes con la edad, Gc se mantiene o crece. [longitudinal]
- Las capacidades amplias son **parcialmente independientes**: una persona puede ser alta en Gc y
  media en Gs. Medir una no sustituye a medir otra. [factor-analítico]

## (c) Claims NO respaldados o refutados

- **CHC describe estructura, no maleabilidad.** Que Gf sea un factor real NO implica que sea
  entrenable de forma transferible; la evidencia de entrenamiento (ver [[T05_n_back_debate_jaeggi]],
  [[T01_brain_training_simons_2016]]) es mayoritariamente negativa para elevar g/Gf.
- **Ningún producto de consumo mide "tu IQ" o "tu g" de forma válida.** Estimar g requiere baterías
  amplias, estandarizadas y controladas; una app de entrenamiento no produce un puntaje de
  inteligencia interpretable.
- Colapsar varias capacidades amplias en un único "número de cerebro" es psicométricamente injusto y
  engañoso: pierde justo la información diagnóstica (el perfil, no el promedio).

## (d) Estado de replicación / críticas

CHC es el consenso de facto, pero hay debate sobre el número exacto de factores, sobre si g es una
entidad causal o un artefacto estadístico (posición de "mutualismo"/redes de van der Maas), y sobre la
validez transcultural de Gc. Nada de esto amenaza el uso práctico: la lección para producto es que la
cognición es **multidimensional** y que un promedio único oculta el perfil.

## (e) Implicaciones de diseño para MindBoost

- **El `MindBoostProfileAggregator` debe ser un perfil multidimensional, no un IQ.** CHC es la
  justificación teórica de las 9 `CognitiveDimension`: reportar dimensiones funcionales separadas
  (atención, velocidad, memoria de trabajo, razonamiento, verbal…) es lo correcto; un "índice global"
  (`OverallSummary.compositeIndex`) debe presentarse como **orientativo** y nunca como "tu
  inteligencia".
- `UserDisplayScoreMapper` y el brain-profile deben resistir la tentación de un único número:
  mostrar el radar por dimensión con dimensiones en `CALIBRATING` cuando falta señal (`null ≠ 0`).
- **Mapeo honesto:** los módulos MindBoost tocan capacidades estrechas dentro de capacidades amplias
  (E-módulos → facetas de Gf/Gsm/Gs/Gc), nunca g. El progreso se reporta *frente a la propia línea
  base del usuario*, no contra una norma poblacional de "inteligencia".
- **Claim prohibido absoluto:** cualquier variante de "mide/aumenta tu IQ / tu inteligencia / tu g".
  CHC es precisamente la autoridad que lo desautoriza.

## (f) Referencias

- Carroll, J. B. (1993). *Human cognitive abilities: A survey of factor-analytic studies.* Cambridge
  University Press.
- McGrew, K. S. (2009). CHC theory and the human cognitive abilities project. *Intelligence, 37*(1),
  1–10.
- Cattell, R. B. (1963). Theory of fluid and crystallized intelligence. *Journal of Educational
  Psychology, 54*(1), 1–22.
- van der Maas, H. L. J., et al. (2006). A dynamical model of general intelligence: the positive
  manifold of intelligence by mutualism. *Psychological Review, 113*(4), 842–861.
