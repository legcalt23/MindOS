---
title: "Is working memory training effective? (meta-analyses)"
authors: "Melby-Lervåg, M.; Hulme, C.; Redick, T. S."
year: "2013–2016"
tier: T1
domains: [working_memory, logical_reasoning]
constructs: [working_memory_training, near_transfer, far_transfer, fluid_intelligence]
paradigms: [n_back, complex_span]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/scoring/ComplexSpanScorer.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/MemoryProfileSource.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/model/CognitiveTaxonomy.kt
evidence_level: strong
---

## (a) Tesis central

Meta-análisis riguroso del entrenamiento de memoria de trabajo (Melby-Lervåg & Hulme 2013; ampliado
2016 con Redick). Hallazgo: el entrenamiento produce mejoras **cercanas** (en tareas de WM parecidas a
la entrenada) pero **NO transfiere** a inteligencia fluida, control atencional, lectura, aritmética ni
rendimiento cotidiano; y las ganancias cercanas **se desvanecen** con el tiempo. Es la refutación
central de la promesa "entrena tu memoria de trabajo y serás más inteligente".

## (b) Claims RESPALDADOS

- El entrenamiento de WM mejora tareas de WM cercanas a la entrenada, a corto plazo. [meta-análisis]
- Cuando se usan grupos de control **activos** (no pasivos), los efectos de transferencia lejana se
  reducen esencialmente a **cero**. [meta-análisis — la moderación por tipo de control es clave]
- Las mejoras no se mantienen en seguimientos; el efecto es frágil y dependiente del diseño.
  [meta-análisis]

## (c) Claims NO respaldados o refutados

- **REFUTADO:** que entrenar WM eleve la inteligencia fluida (Gf) o la capacidad de razonamiento.
- **REFUTADO:** transferencia a habilidades académicas (lectura, matemáticas) o a la vida diaria.
- **No respaldado:** que las mejoras de WM entrenada sean duraderas o generalizables.
- Los estudios con control pasivo que reportan transferencia probablemente reflejan efectos de
  expectativa/motivación, no cognición.

## (d) Estado de replicación / críticas

Es de los resultados más replicados y metodológicamente cuidadosos del campo; la moderación por
"control activo vs pasivo" explica por qué estudios más débiles hallaban transferencia. Defensores del
n-back (Jaeggi; ver [[T05_n_back_debate_jaeggi]]) han disputado detalles, pero el peso de la evidencia
—incluido [[T03_owen_2010_brain_training]] y [[T01_brain_training_simons_2016]]— respalda la conclusión
escéptica sobre transferencia lejana.

## (e) Implicaciones de diseño para MindBoost

- **Aplica directamente a EX1 (span complejo) y la dimensión `WORKING_MEMORY`.** MindBoost puede
  entrenar y medir progreso en la tarea de WM (transferencia cercana, real) pero **no** debe prometer
  que ello mejore inteligencia, estudio o vida — esta ficha lo prohíbe explícitamente.
- Coherente con que `MemoryProfileSource`/`WORKING_MEMORY` quede a menudo en `CALIBRATING` y con el
  aprendizaje del repo de que la memoria alimenta el perfil solo tras gates estrictos: la señal honesta
  es "progreso en la tarea", no "tu memoria de trabajo subió como capacidad".
- **Diseño de dosis/expectativa:** evitar cualquier copy que genere la expectativa de mejora general
  (que además contaminaría auto-informes). El feedback debe ceñirse al desempeño en la tarea.
- **Claim permitido:** "progreso en tu span de memoria de trabajo frente a tu línea base".
  **Prohibido:** "mejora tu memoria / tu inteligencia entrenando memoria de trabajo".

## (f) Referencias

- Melby-Lervåg, M., & Hulme, C. (2013). Is working memory training effective? A meta-analytic review.
  *Developmental Psychology, 49*(2), 270–291.
- Melby-Lervåg, M., Redick, T. S., & Hulme, C. (2016). Working memory training does not improve
  performance on measures of intelligence or other measures of "far transfer": Evidence from a
  meta-analytic review. *Perspectives on Psychological Science, 11*(4), 512–534.
