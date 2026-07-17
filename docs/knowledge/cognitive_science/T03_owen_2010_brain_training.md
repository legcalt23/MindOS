---
title: "Putting brain training to the test (large-scale online RCT)"
authors: "Owen, A. M.; Hampshire, A.; Grahn, J. A.; et al."
year: 2010
tier: T1
domains: [attention, working_memory, processing_speed, logical_reasoning, learning_strategy]
constructs: [cognitive_training, transfer, generalization]
paradigms: [reasoning, memory, attention_tasks]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/ForbiddenLexicon.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/aggregation/MindBoostProfileAggregator.kt
evidence_level: strong
---

## (a) Tesis central

RCT online masivo (n = 11 430) emitido con la BBC: seis semanas de entrenamiento cognitivo en tareas
diseñadas para mejorar razonamiento, memoria, atención y planificación. Resultado: los participantes
mejoraron en **las tareas entrenadas**, pero **no hubo transferencia** a tareas cognitivas no
entrenadas, ni siquiera a tareas que compartían procesos cognitivos con las entrenadas. Un golpe
empírico directo, a gran escala, contra la promesa de generalización.

## (b) Claims RESPALDADOS

- Con entrenamiento se mejora en las tareas practicadas. [RCT masivo]
- **No** hubo transferencia a tareas no entrenadas, incluso relacionadas cognitivamente. [RCT masivo]
- El tamaño de muestra hace improbable que "no hubo efecto" se deba a falta de potencia estadística.
  [RCT masivo — potencia alta]

## (c) Claims NO respaldados o refutados

- **REFUTADO a gran escala:** que el entrenamiento en juegos cognitivos generalice a la capacidad
  cognitiva general.
- **No respaldado:** que mejorar en un juego de "razonamiento" mejore el razonamiento en otras tareas.
- Cuestiona directamente las afirmaciones de marketing de "entrena tu cerebro para ser más listo".

## (d) Estado de replicación / críticas

Críticas: la "dosis" de entrenamiento fue relativamente baja (pocas sesiones cortas a lo largo de las
seis semanas), lo que algunos usaron para matizar el resultado. Un estudio posterior de mayor dosis (Corbett et al. 2015, con la
misma plataforma, en adultos mayores) halló alguna transferencia a medidas funcionales cotidianas en
mayores, pero el patrón general —transferencia lejana ausente o pequeña y específica de población— se
sostiene. Sigue siendo una piedra angular del escepticismo empírico.

## (e) Implicaciones de diseño para MindBoost

- Junto con [[T01_brain_training_simons_2016]] y [[T02_wm_training_melby_lervag_hulme]], forma el
  **triángulo de evidencia** que sostiene la frontera de claims: MindBoost mide progreso en tareas, no
  promete generalización.
- La matización de "dosis" no rescata el claim de transferencia general para producto de consumo:
  incluso si más dosis ayudara en poblaciones específicas (mayores), afirmarlo en general sin ese RCT
  sería deshonesto.
- Refuerza que el `MindBoostProfileAggregator` debe presentarse como **perfil de desempeño en la
  práctica**, no como medida de capacidad cognitiva que predice la vida real.
- **Claim permitido:** "mejoras en lo que practicas". **Prohibido:** "entrena tu cerebro para rendir
  más en general / ser más inteligente".

## (f) Referencias

- Owen, A. M., Hampshire, A., Grahn, J. A., Stenton, R., Dajani, S., Burns, A. S., Howard, R. J., &
  Ballard, C. G. (2010). Putting brain training to the test. *Nature, 465*(7299), 775–778.
- Corbett, A., et al. (2015). The effect of an online cognitive training package in healthy older
  adults. *Journal of the American Medical Directors Association, 16*(11), 990–997.
