---
title: "Reaction-time methodology: distributions, speed–accuracy tradeoff, timing precision"
authors: "Whelan, R.; Ratcliff, R.; Heathcote, A.; Luce, R. D."
year: "1986–2008"
tier: T2
domains: [processing_speed]
constructs: [reaction_time, rt_distribution, ex_gaussian, drift_diffusion, speed_accuracy_tradeoff, timing_precision]
paradigms: [choice_rt, simple_rt]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/timing
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/SpeedProfileSource.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/metrics/MetricKey.kt
evidence_level: strong
---

## (a) Tesis central

El tiempo de reacción es una de las medidas más ricas de la psicología cognitiva, pero **tratarlo como
una media es un error**: las distribuciones de RT son asimétricas (cola derecha), por lo que la
**mediana** y la variabilidad (RT-CV) son más honestas que la media, y la modelización de la
distribución completa (ex-Gaussian; drift-diffusion) separa componentes cognitivos de motores y de
estrategia. Además, la **precisión de la base temporal** y el **compromiso velocidad-precisión (SATF)**
determinan si una medida de RT es interpretable.

## (b) Claims RESPALDADOS

- Las distribuciones de RT son asimétricas; medias simples están sesgadas por la cola y por outliers.
  Mediana, RT-CV y modelos de distribución son preferibles. [metodológico, establecido]
- El **speed–accuracy tradeoff** es real: se puede bajar el RT a costa de precisión; sin controlarlo,
  "más rápido" es ambiguo. [experimental, fundamental]
- El **drift-diffusion model** descompone la RT en tasa de acumulación de evidencia (velocidad
  cognitiva), umbral de decisión (cautela/estrategia) y tiempo no-decisional (motor/perceptivo).
  [modelado, validado]
- La **precisión de medición** (jitter de frame, resolución del timestamp, latencia de input) puede
  dominar señales de RT finas; medir RT en móvil exige base temporal de alta resolución. [metodológico]

## (c) Claims NO respaldados o refutados

- Una media de RT más baja **no** es evidencia de mejora cognitiva sin controlar precisión (SATF) ni
  base temporal.
- Un RT medido con base temporal no-autoritativa (jitter de frame, timestamps de baja resolución)
  **no** es interpretable como velocidad de procesamiento — el ruido de medición contamina la señal.
- Los outliers (anticipaciones ultra-rápidas, lapsos ultra-lentos) requieren tratamiento explícito; no
  se cuentan como el resto de la distribución (ver el aprendizaje del repo sobre MAD y lapsos).

## (d) Estado de replicación / críticas

Metodología consolidada. El drift-diffusion es el estándar moderno para descomponer RT. El mensaje
práctico para producto móvil: la validez de cualquier índice de velocidad depende **primero** de la
ingeniería de medición (timebase) y **segundo** del control estadístico (distribución, SATF, outliers).

## (e) Implicaciones de diseño para MindBoost

- **Respalda toda la arquitectura de timing del repo:** `core/timing` (FrameStats/probe),
  `TimebaseClassifier` y el gate de `SpeedProfileSource` que exige `CHOREOGRAPHER_FRAME` +
  `NANOS_NATIVE` antes de alimentar el perfil. Sin timebase autoritativo, la señal RT se excluye — esta
  ficha es la razón científica.
- **Métricas honestas ya presentes:** `RT_MEDIAN` y `RT_CV` (no media) en `MetricKey`; el tratamiento de
  outliers por MAD para tendencia central y el conteo de lapsos sobre RT crudos (aprendizaje 2026-06-04)
  son exactamente lo que la metodología prescribe.
- **SATF obligatorio:** cualquier índice de velocidad debe pareja con precisión o penalizar respuestas
  rápidas-imprecisas; considerar drift-diffusion (tasa de deriva vs umbral) para separar velocidad
  cognitiva de estrategia arriesgada — remit del psychometrician.
- **Claim permitido:** "progreso en tu velocidad de respuesta con precisión controlada y medición
  fiable". **Prohibido:** presentar un RT más rápido como mejora de "velocidad mental" sin controlar
  SATF/timebase.

## (f) Referencias

- Whelan, R. (2008). Effective analysis of reaction time data. *The Psychological Record, 58*(3),
  475–482.
- Ratcliff, R., & McKoon, G. (2008). The diffusion decision model: Theory and data for two-choice
  decision tasks. *Neural Computation, 20*(4), 873–922.
- Heathcote, A., Popiel, S. J., & Mewhort, D. J. K. (1991). Analysis of response time distributions:
  An example using the Stroop task. *Psychological Bulletin, 109*(2), 340–347.
- Luce, R. D. (1986). *Response times: Their role in inferring elementary mental organization.* Oxford
  University Press.
