---
title: "The malleability of spatial skills: a meta-analysis of training studies"
authors: "Uttal, D. H.; et al."
year: 2013
tier: T1
domains: [spatial_reasoning]
constructs: [spatial_ability, mental_rotation, spatial_visualization, transfer_within_domain]
paradigms: [mental_rotation, spatial_visualization, multiple_object_tracking]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/catalog/ExerciseCatalog.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/model/CognitiveTaxonomy.kt
evidence_level: moderate
---

## (a) Tesis central

A diferencia del entrenamiento de memoria de trabajo, la habilidad espacial **SÍ es maleable** (con
transferencia *dentro del dominio espacial*): el meta-análisis de Uttal et al. (2013, 217 estudios)
encontró que el entrenamiento
espacial produce mejoras de tamaño medio (g ≈ 0.47), **duraderas** y que **transfieren a tareas
espaciales no entrenadas**. Es el contraejemplo importante que impide un escepticismo total sobre el
entrenamiento cognitivo: la transferencia *dentro de un dominio* puede ser real.

## (b) Claims RESPALDADOS

- El entrenamiento espacial mejora el rendimiento en tareas espaciales, con efectos de tamaño medio.
  [meta-análisis]
- Las ganancias **transfieren dentro del dominio espacial** (a tareas espaciales distintas de la
  entrenada) y **se mantienen** en el tiempo. [meta-análisis]
- La habilidad espacial **predice** el éxito en STEM. [correlacional/longitudinal]
- Diversos formatos funcionan (videojuegos, cursos, práctica de tareas): la mejora no depende de un
  producto concreto. [meta-análisis]

## (c) Claims NO respaldados o refutados

- La transferencia es **dentro del dominio espacial**, no a inteligencia general ni a dominios no
  espaciales. No autoriza claims de "mejora cognitiva global".
- **No hay evidencia directa de que las ganancias ESPACIALES ENTRENADAS transfieran a rendimiento
  STEM/académico:** el vínculo espacial↔STEM es correlacional (predicción), no una demostración de que
  entrenar lo espacial mejore el desempeño en STEM.
- Las mejoras no son ilimitadas: se moderan por dosis, tipo de tarea y línea base; parte del efecto es
  aprendizaje de estrategia específica.
- **No** valida por analogía el entrenamiento de OTROS dominios: que lo espacial transfiera no implica
  que lo haga la memoria de trabajo o la velocidad (cada dominio se juzga con su propia evidencia).

## (d) Estado de replicación / críticas

El meta-análisis es influyente y metodológicamente serio, aunque como todo meta-análisis está sujeto a
sesgo de publicación y heterogeneidad. La distinción clave que aporta —transferencia *cercana/dentro
de dominio* es plausible; transferencia *lejana/entre dominios* no— es coherente con el resto de la
literatura (ver [[T07_far_vs_near_transfer]]). La lección no es "el brain training funciona" sino "la
transferencia se acota al dominio y al proceso entrenados".

## (e) Implicaciones de diseño para MindBoost

- **Hueco de producto declarado:** MindBoost tiene `spatial_reasoning` como dominio canónico pero **no**
  hay una dimensión `SPATIAL` en `CognitiveTaxonomy.kt` ni un módulo espacial dedicado. E9 Enjambre
  (MOT) tiene componente espacio-atencional pero informa ATTENTION, no una capacidad espacial. Dado que
  lo espacial es de lo *más entrenable con transferencia real dentro de dominio*, es la **oportunidad de
  mayor evidencia** para un módulo futuro (mental rotation / visualización espacial).
- Si se añade, el claim honesto sería fuerte y defendible: "practicar rotación/visualización espacial
  mejora tu desempeño en tareas espaciales" — respaldado por T1, cercano al dominio.
- Refuerza el principio de MindBoost de **medir progreso dentro de la tarea/dominio entrenado**, que es
  justo donde la transferencia tiene apoyo empírico.

## (f) Referencias

- Uttal, D. H., Meadow, N. G., Tipton, E., Hand, L. L., Alden, A. R., Warren, C., & Newcombe, N. S.
  (2013). The malleability of spatial skills: A meta-analysis of training studies. *Psychological
  Bulletin, 139*(2), 352–402.
- Wai, J., Lubinski, D., & Benbow, C. P. (2009). Spatial ability for STEM domains. *Journal of
  Educational Psychology, 101*(4), 817–835.
