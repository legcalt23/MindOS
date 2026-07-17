---
title: "The unity and diversity of executive functions"
authors: "Miyake, A.; Friedman, N. P.; et al."
year: "2000–2017"
tier: T1
domains: [attention, working_memory, decision_making]
constructs: [executive_function, inhibition, shifting, updating, cognitive_control]
paradigms: [stroop, flanker, stop_signal, go_no_go, task_switching, n_back]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/catalog/ExerciseCatalog.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/exercise/TrialClassifier.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/model/CognitiveTaxonomy.kt
evidence_level: strong
---

## (a) Tesis central

Las funciones ejecutivas no son ni una capacidad única ni tres capacidades independientes: son
**separables pero correlacionadas** ("unidad y diversidad"). El análisis de variables latentes de
Miyake et al. (2000) aisló tres: **updating** (actualizar el contenido de la memoria de trabajo),
**shifting** (cambiar entre tareas/reglas) e **inhibition** (suprimir respuestas dominantes). La
revisión de Friedman & Miyake (2017) refina el modelo: existe un factor **Común de EF** más factores
específicos de updating y shifting, y la inhibición **ya no se separa** del factor Común.

## (b) Claims RESPALDADOS

- Las funciones ejecutivas son constructos latentes disociables pero correlacionados. [modelado de
  ecuaciones estructurales / CFA]
- Las EF predicen resultados de vida: rendimiento académico, salud, control de impulsos, estatus
  socioeconómico. [correlacional/longitudinal — p. ej. Moffitt et al. 2011]
- El factor Común de EF es altamente heredable (~0.99 en el estudio de gemelos de Friedman et al.
  2008, para la varianza estable). [genética conductual]
- La inhibición de respuesta y el control de interferencia son medibles con paradigmas clásicos
  (Stroop, flanker, stop-signal, go/no-go) con firmas conductuales estables (efecto de congruencia,
  SSRT, falsas alarmas). [experimental, validado]

## (c) Claims NO respaldados o refutados

- **No** implica que las EF se entrenen fácilmente hasta elevar la capacidad cognitiva general de
  forma transferible; su alta heredabilidad y la especificidad del entrenamiento militan en contra.
- **Problema de impureza de tarea:** ninguna tarea única mide "inhibición pura" — toda tarea ejecutiva
  arrastra procesos no-ejecutivos (percepción, velocidad, motor). Un "score de inhibición" de una sola
  tarea es psicométricamente frágil.
- La revisión de 2017 desaconseja tratar la **inhibición** como una capacidad independiente
  entrenable: se solapa con el factor Común. Prometer "mejora tu inhibición" como dimensión aislada no
  está respaldado.

## (d) Estado de replicación / críticas

La estructura unidad/diversidad se ha replicado ampliamente, aunque el número exacto de factores y su
etiquetado varían entre muestras (especialmente en niños, donde las EF están menos diferenciadas). La
fiabilidad de las tareas ejecutivas individuales es a menudo **baja** (paradoja de fiabilidad: tareas
robustas a nivel de grupo, ruidosas a nivel individual), lo que complica su uso para medir diferencias
individuales o cambio intra-sujeto — una advertencia central para cualquier producto que puntúe EF.

## (e) Implicaciones de diseño para MindBoost

- **Mapa Enfoque ↔ EF:** E2 Filtro / E5 Conflicto (control de interferencia, `CONGRUENCY_EFFECT`),
  E3 Gatillo (go/no-go, `FALSE_ALARMS`) y E4 Freno (stop-signal, `SSRT`) tocan inhibición;
  E6 Cambio (task-switching, `SWITCH_COST`) toca shifting. El repo ya trata cada paradigma con su
  firma conductual específica en `TrialClassifier` — correcto.
- **La impureza de tarea + la baja fiabilidad individual justifican** que MindBoost reporte
  **dimensiones funcionales** ("control atencional", "flexibilidad controlada") y NO un "score de tu
  inhibición" de una única sesión. Refuerza la política `null ≠ 0` / `CALIBRATING`: pocas sesiones ⇒
  no hay señal estable de EF.
- La revisión 2017 (inhibición ≈ Común) recomienda **no** exponer una dimensión de perfil
  "inhibición" independiente; agregarla dentro de ATTENTION/control es más honesto.
- **Claim permitido:** "práctica de control atencional bajo interferencia". **Prohibido:** "entrena y
  mejora tus funciones ejecutivas" como transferencia general.

## (f) Referencias

- Miyake, A., Friedman, N. P., Emerson, M. J., Witzki, A. H., Howerter, A., & Wager, T. D. (2000).
  The unity and diversity of executive functions and their contributions to complex "frontal lobe"
  tasks. *Cognitive Psychology, 41*(1), 49–100.
- Friedman, N. P., & Miyake, A. (2017). Unity and diversity of executive functions: Individual
  differences as a window on cognitive structure. *Cortex, 86*, 186–204.
- Friedman, N. P., et al. (2008). Individual differences in executive functions are almost entirely
  genetic in origin. *Journal of Experimental Psychology: General, 137*(2), 201–225.
- Moffitt, T. E., et al. (2011). A gradient of childhood self-control predicts health, wealth, and
  public safety. *PNAS, 108*(7), 2693–2698.
