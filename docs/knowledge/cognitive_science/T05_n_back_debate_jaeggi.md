---
title: "The n-back / fluid intelligence debate"
authors: "Jaeggi, S. M.; Buschkuehl, M.; Redick, T. S.; Chooi, W.-T.; Thompson, L. A."
year: "2008–2016"
tier: T1
domains: [working_memory, logical_reasoning]
constructs: [n_back, fluid_intelligence, transfer, dose_response]
paradigms: [n_back, dual_n_back]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/scoring/ComplexSpanScorer.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/model/CognitiveTaxonomy.kt
evidence_level: contested
---

## (a) Tesis central

Jaeggi et al. (2008) publicaron el hallazgo más citado a favor del brain training: entrenar el
**dual n-back** mejoraba la inteligencia fluida (Gf), con una relación dosis-respuesta. Desató una
década de intentos de replicación. El balance actual: los efectos originales **no se replican de forma
robusta** cuando se usan controles activos, múltiples medidas de Gf y muestras grandes. El "debate
n-back" es el caso de estudio de por qué un resultado llamativo no basta.

## (b) Claims RESPALDADOS

- El entrenamiento en n-back mejora el rendimiento en n-back y tareas de WM muy cercanas.
  [meta-análisis — transferencia cercana]
- Existe una relación dosis-respuesta en la *mejora de la tarea entrenada*. [experimental]

## (c) Claims NO respaldados o refutados

- **No se replica robustamente** que el n-back eleve Gf: Redick et al. (2013, control activo, múltiples
  medidas) y Chooi & Thompson (2012) no hallaron el efecto; el meta-análisis de Melby-Lervåg et al.
  (2016) lo confirma. [ver [[T02_wm_training_melby_lervag_hulme]]]
- La aparente mejora de Gf en estudios positivos se atribuye a **medida única de Gf** (efecto de
  familiaridad con el test), **control pasivo** y expectativas.
- **No** autoriza el claim "entrena n-back y sube tu IQ/Gf".

## (d) Estado de replicación / críticas

Es el ejemplo canónico de fallo de replicación en cognición: hallazgo inicial mediático → réplicas
fallidas con mejor diseño → meta-análisis escéptico. Lecciones metodológicas: usar **control activo**,
**múltiples medidas** de la capacidad diana (no un solo test), muestras grandes y pre-registro. Jaeggi y
colaboradores mantienen que ciertas condiciones (individuos, dosis, mindset) modulan el efecto, pero la
carga de la prueba no se ha satisfecho para un claim general.

## (e) Implicaciones de diseño para MindBoost

- **Advertencia directa contra un futuro módulo n-back con claim de Gf.** Si MindBoost implementa
  n-back (paradigma clásico de WM), debe tratarse como práctica de WM con progreso propio, jamás como
  "sube tu inteligencia fluida".
- Refuerza reglas psicométricas del repo: **nunca una sola medida** decide una dimensión; controlar
  familiaridad con el test (efectos de práctica, ver [[P02_practice_effects_test_retest]]); bancos con
  holdout para que "mejorar" no sea "memorizar los ítems".
- Caso didáctico para el equipo: un resultado llamativo sin réplica con control activo **no** es base de
  producto. Alinea con `null ≠ 0` y con exigir evidencia fuerte antes de afirmar.
- **Claim permitido:** "progreso en tu tarea de memoria de trabajo". **Prohibido:** "eleva tu
  inteligencia fluida entrenando n-back".

## (f) Referencias

- Jaeggi, S. M., Buschkuehl, M., Jonides, J., & Perrig, W. J. (2008). Improving fluid intelligence with
  training on working memory. *PNAS, 105*(19), 6829–6833.
- Redick, T. S., et al. (2013). No evidence of intelligence improvement after working memory training:
  A randomized, placebo-controlled study. *Journal of Experimental Psychology: General, 142*(2),
  359–379.
- Chooi, W.-T., & Thompson, L. A. (2012). Working memory training does not improve intelligence in
  healthy young adults. *Intelligence, 40*(6), 531–542.
