---
title: "The testing effect: retrieval practice enhances learning (and transfers)"
authors: "Roediger, H. L.; Karpicke, J. D.; Butler, A. C."
year: "2006–2013"
tier: T1
domains: [learning_strategy, working_memory]
constructs: [retrieval_practice, testing_effect, long_term_retention, transfer_of_learning]
paradigms: [free_recall, cued_recall, recognition]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/presentation/DeferredRecallSessionStateHolder.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/orchestration/TransversalDailyPlanner.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/coach
evidence_level: strong
---

## (a) Tesis central

Recuperar activamente la información de la memoria (hacerse un test) produce **mejor retención a largo
plazo** que releer o re-estudiar pasivamente el mismo material — el "testing effect" o efecto de la
práctica de recuperación. Butler (2010, 2013) mostró además que la práctica de recuperación **transfiere**
a preguntas nuevas y a la aplicación de conceptos, no solo a la repetición literal.

## (b) Claims RESPALDADOS

- Recuperar activamente supera al re-estudio pasivo para la retención duradera. [experimental,
  meta-analítico, muy replicado]
- El beneficio crece con el intervalo de retención: cuanto más tarde se evalúa, mayor la ventaja del
  testing frente al releer. [experimental]
- La práctica de recuperación **transfiere** a preguntas de inferencia/aplicación y a contextos nuevos
  dentro del dominio. [experimental — Butler 2010]
- El feedback tras la recuperación amplifica el efecto (corrige errores y consolida aciertos).
  [experimental]

## (c) Claims NO respaldados o refutados

- El efecto es sobre **aprendizaje de contenido**, no sobre elevar capacidad cognitiva general: mejora
  cuánto recuerdas de lo que estudias, no tu "memoria" como rasgo.
- La transferencia demostrada es *dentro del dominio del material* (a preguntas nuevas del mismo tema),
  no transferencia lejana entre dominios.
- El beneficio depende de una recuperación **con esfuerzo exitosa**; recuperación trivial o fallida sin
  feedback aporta poco.

## (d) Estado de replicación / críticas

Es de los efectos más robustos y replicados de la ciencia del aprendizaje (cientos de estudios, aula y
laboratorio). Matices: el tamaño del efecto varía con la dificultad de la recuperación y con el
feedback; en algunos materiales muy complejos la ventaja se atenúa. La dirección es sólida y aplicable.

## (e) Implicaciones de diseño para MindBoost

- **Justifica el paradigma de recuerdo diferido (EX3) y el diseño de discriminación** en Memoria:
  probar recuperación tras demora (24h) es exactamente el testing effect. El aprendizaje del repo de
  exigir *señuelos completos* (false-alarm rate) para que el test discrimine encaja: una recuperación
  con esfuerzo real (no reconocimiento trivial) es la que produce y mide aprendizaje.
- **Feedback inmediato** (uno de los pilares del producto) tiene aquí respaldo directo: el feedback tras
  la recuperación es lo que consolida — el coach debe dar feedback correctivo, no solo puntuar.
- El `TransversalDailyPlanner` debería programar **re-tests espaciados** del material/ejercicios, no
  solo práctica masiva (combinar con [[L02_spacing_distributed_practice]]).
- **Claim permitido:** "recuperar con esfuerzo consolida lo que practicas". **Prohibido:** "mejora tu
  memoria" como capacidad general.

## (f) Referencias

- Roediger, H. L., & Karpicke, J. D. (2006). Test-enhanced learning: Taking memory tests improves
  long-term retention. *Psychological Science, 17*(3), 249–255.
- Butler, A. C. (2010). Repeated testing produces superior transfer of learning relative to repeated
  studying. *Journal of Experimental Psychology: LMC, 36*(5), 1118–1133.
- Rowland, C. A. (2014). The effect of testing versus restudy on retention: A meta-analytic review of
  the testing effect. *Psychological Bulletin, 140*(6), 1432–1463.
