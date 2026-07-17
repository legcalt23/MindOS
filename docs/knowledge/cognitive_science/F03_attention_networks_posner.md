---
title: "The attention system of the human brain: alerting, orienting, executive control"
authors: "Posner, M. I.; Petersen, S. E.; Fan, J."
year: "1990–2012"
tier: T1
domains: [attention]
constructs: [alerting, orienting, executive_attention, vigilance, conflict_monitoring]
paradigms: [pvt, visual_search, flanker, stroop, attention_network_test]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/catalog/ExerciseCatalog.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/metrics
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/model/CognitiveTaxonomy.kt
evidence_level: strong
---

## (a) Tesis central

La atención no es un recurso único sino **tres redes cerebrales parcialmente independientes**:
**alerting** (alcanzar y mantener un estado de alerta/vigilancia), **orienting** (seleccionar
información en el espacio, orientar el foco) y **executive/conflict** (resolver conflicto entre
respuestas competidoras y controlar la interferencia). El Attention Network Test (ANT) mide las tres
en una sola tarea.

## (b) Claims RESPALDADOS

- Existen redes atencionales disociables con sustratos neurales y firmas conductuales distintas.
  [neuroimagen + lesión + conductual]
- El estado de alerta/vigilancia decae con el tiempo en tarea (vigilance decrement) y se mide por la
  variabilidad y los lapsos del tiempo de reacción. [experimental — paradigma PVT robusto]
- El control ejecutivo/de conflicto se indexa por el coste de las condiciones incongruentes (efecto
  de congruencia en flanker/Stroop). [experimental, replicado]
- Las tres redes muestran independencia parcial: entrenar/afectar una no cambia necesariamente las
  otras. [ANT, correlaciones bajas entre redes]

## (c) Claims NO respaldados o refutados

- **No** existe una "atención" única que se pueda "subir de nivel" como una barra; mejorar en una red
  no transfiere automáticamente a las otras ni a la atención cotidiana.
- La fiabilidad test-retest de las puntuaciones de red del ANT (sobre todo alerting y executive) es
  **modesta**, lo que limita su uso para medir cambio individual fino.
- **No** respalda que la vigilancia entrenada en una tarea de laboratorio transfiera a "estar más
  concentrado en el trabajo/estudio" — eso es transferencia lejana, no demostrada aquí.

## (d) Estado de replicación / críticas

La taxonomía de tres redes es sólida y ampliamente adoptada. La crítica principal es psicométrica: los
scores de diferencia del ANT (incongruente − congruente, etc.) heredan baja fiabilidad porque restan
dos medidas ruidosas. La red de alerting es la más inestable. Esto no invalida el modelo teórico, pero
sí obliga a prudencia al puntuar redes a nivel individual.

## (e) Implicaciones de diseño para MindBoost

- **Cada red tiene su ejercicio de Enfoque:** E1 Vigía (PVT, `RT_CV`/`LAPSES`/`VIGILANCE_DECREMENT_
  SLOPE`) → alerting/vigilancia; E7 Rastreo (visual search, `SET_SIZE_SLOPE`) → orienting; E2 Filtro /
  E5 Conflicto (`CONGRUENCY_EFFECT`) → executive/conflict. El catálogo ya respeta esta separación.
- **La baja fiabilidad de los scores de diferencia** justifica que el perfil trate ATTENTION como una
  dimensión que necesita muchas sesiones para estabilizar (`confidence` bajo con pocos bloques) y que
  nunca reporte "tu atención = X" tras una sesión.
- El vigilance decrement es una señal *funcional* real y medible (pendiente del RT en el tiempo), buen
  candidato para feedback honesto ("cómo cambia tu estabilidad a lo largo de la sesión") sin claim de
  transferencia.
- **Claim permitido:** "práctica de sostener la atención cuando casi no pasa nada" (E1). **Prohibido:**
  "mejora tu atención" como capacidad global transferible al día a día.

## (f) Referencias

- Posner, M. I., & Petersen, S. E. (1990). The attention system of the human brain. *Annual Review of
  Neuroscience, 13*, 25–42.
- Petersen, S. E., & Posner, M. I. (2012). The attention system of the human brain: 20 years after.
  *Annual Review of Neuroscience, 35*, 73–89.
- Fan, J., McCandliss, B. D., Sommer, T., Raz, A., & Posner, M. I. (2002). Testing the efficiency and
  independence of attentional networks. *Journal of Cognitive Neuroscience, 14*(3), 340–347.
- MacLeod, J. W., et al. (2010). Appraising the ANT: Psychometric and theoretical considerations.
  *Neuropsychology, 24*(5), 637–651.
