---
title: "The multicomponent model of working memory"
authors: "Baddeley, A. D.; Hitch, G. J."
year: "1974–2012"
tier: T2
domains: [working_memory]
constructs: [working_memory, phonological_loop, visuospatial_sketchpad, central_executive, episodic_buffer]
paradigms: [complex_span, simple_span, dual_task]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/scoring/ComplexSpanScorer.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/scoring/AssociativePairsScorer.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/model/CognitiveTaxonomy.kt
evidence_level: strong
---

## (a) Tesis central

La memoria de trabajo no es un almacén único sino un sistema de varios componentes: un **ejecutivo
central** que asigna atención, dos sistemas esclavos de almacenamiento a corto plazo (el **bucle
fonológico** verbal y la **agenda visuoespacial**) y, desde 2000, un **búfer episódico** que integra
información multimodal con la memoria a largo plazo. Es el modelo de referencia para entender por qué
la capacidad de trabajar con información en la mente predice el rendimiento cognitivo.

## (b) Claims RESPALDADOS

- La capacidad de memoria de trabajo correlaciona fuerte y consistentemente con comprensión lectora,
  razonamiento fluido (Gf) y rendimiento académico. [correlacional, replicado]
- Almacenamiento y procesamiento son demandas separables: las tareas de **span complejo** (mantener
  ítems mientras se procesa una tarea intercalada) predicen mejor las capacidades de orden superior
  que el **span simple** (solo mantener). [correlacional/experimental]
- El bucle fonológico y la agenda visuoespacial son sistemas disociables (doble tarea: una tarea
  verbal interfiere poco con una espacial y viceversa). [experimental de doble tarea]
- La memoria de trabajo (mantener/manipular ahora) es funcionalmente distinta de la memoria episódica
  a largo plazo (recuperar tras demora). [teoría + doble disociación neuropsicológica]

## (c) Claims NO respaldados o refutados

- **No** implica que entrenar el span de memoria de trabajo eleve la inteligencia general ni la
  capacidad cognitiva más allá de las tareas entrenadas (esa es una cuestión de *transferencia*, que
  la evidencia de entrenamiento —ver [[T02_wm_training_melby_lervag_hulme]]— responde en gran medida
  que NO).
- **No** existe un "número único de memoria de trabajo" que sea un rasgo fijo e inmutable; es una
  medida con error, sensible al contenido y al estado.
- El modelo describe *estructura*, no *maleabilidad*: explicar qué es la WM no autoriza a prometer que
  la WM crece con la práctica de forma transferible.

## (d) Estado de replicación / críticas

El modelo multicomponente es de los más replicados de la psicología cognitiva; el bucle fonológico y
la agenda visuoespacial tienen apoyo robusto. El **búfer episódico** (2000) está peor especificado y
es más teórico. Existen modelos rivales (Cowan: WM como el foco de atención sobre la MLP;
Engle/Kane: WM como control atencional/capacidad de mantener metas frente a interferencia) que
compiten y complementan; la convergencia entre ellos es que **el control atencional** es el núcleo
predictivo, no el almacenamiento pasivo.

## (e) Implicaciones de diseño para MindBoost

- **EX1 span complejo → dimensión `WORKING_MEMORY`.** Que `ComplexSpanScorer` exija un *gate de
  procesamiento* (la tarea intercalada, con umbral de acierto ≥0.85) NO es burocracia: es lo que
  distingue un span diagnóstico de un "span simple disfrazado". Un span sin demanda de procesamiento
  concurrente mide almacenamiento, no memoria de trabajo — el gate protege la validez del constructo.
- **Split `WORKING_MEMORY` vs `EPISODIC_MEMORY`** (`CognitiveTaxonomy.kt`) está científicamente
  justificado: span/pares (EX1/EX4) informan WM; recuerdo diferido 24h (EX3) informa memoria
  episódica. No colapsar ambas en un solo "memoria".
- **Claim permitido:** "práctica de mantener y manipular información" / "progreso frente a tu propia
  línea base en tareas de memoria de trabajo". **Claim prohibido:** "aumenta tu memoria" como
  capacidad general transferible.
- El núcleo predictivo es el **control atencional**, lo que conecta la memoria de trabajo con el
  módulo de Enfoque (interferencia, E2/E5) — coherente con tratar ambas como facetas del mismo
  sistema de control, no como silos.

## (f) Referencias

- Baddeley, A. D., & Hitch, G. (1974). Working memory. *Psychology of Learning and Motivation, 8*,
  47–89.
- Baddeley, A. D. (2000). The episodic buffer: a new component of working memory? *Trends in
  Cognitive Sciences, 4*(11), 417–423.
- Baddeley, A. D. (2012). Working memory: Theories, models, and controversies. *Annual Review of
  Psychology, 63*, 1–29.
- Conway, A. R. A., et al. (2005). Working memory span tasks: A methodological review. *Psychonomic
  Bulletin & Review, 12*(5), 769–786.
