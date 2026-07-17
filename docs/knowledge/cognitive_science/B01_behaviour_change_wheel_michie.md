---
title: "The Behaviour Change Wheel and the COM-B model"
authors: "Michie, S.; van Stralen, M. M.; West, R."
year: "2011–2014"
tier: T1
domains: [learning_strategy]
constructs: [com_b, capability_opportunity_motivation, behaviour_change_techniques, intervention_functions]
paradigms: []
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/coach
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/orchestration/TransversalDailyPlanner.kt
evidence_level: moderate
---

## (a) Tesis central

Marco integrador para **diseñar intervenciones de cambio de conducta**. En el centro está **COM-B**:
para que ocurra una conducta (**B**) se necesitan **Capacidad** (física/psicológica), **Oportunidad**
(física/social) y **Motivación** (reflexiva/automática). Alrededor, la "rueda" mapea funciones de
intervención (educación, persuasión, incentivación, entrenamiento, ambiente, etc.) y políticas. Es la
base de la taxonomía de 93 técnicas de cambio de conducta (BCTTv1).

## (b) Claims RESPALDADOS

- Las conductas se sostienen sobre Capacidad, Oportunidad y Motivación; una intervención efectiva
  diagnostica **cuál** falta antes de actuar. [marco validado + revisiones]
- Técnicas concretas de cambio de conducta (auto-monitoreo, fijación de metas, feedback, recordatorios,
  planes de implementación if-then) tienen respaldo empírico para la adherencia. [meta-análisis de BCTs]
- Los planes de implementación (intención de implementación "si X, entonces haré Y") mejoran la
  ejecución de intenciones. [meta-análisis — Gollwitzer & Sheeran]

## (c) Claims NO respaldados o refutados

- El cambio de conducta es sobre **adherencia y hábito**, NO sobre capacidad cognitiva: mejorar la
  constancia del usuario no es un claim cognitivo.
- No toda técnica funciona en todo contexto; el marco exige **diagnóstico** (qué componente COM-B falla),
  no aplicar técnicas a ciegas.
- La gamificación mal diseñada puede dañar la motivación intrínseca (sobre-justificación); no toda
  "motivación" añadida es buena.

## (d) Estado de replicación / críticas

Es el marco de referencia en salud pública y diseño conductual, ampliamente adoptado. Crítica: es un
marco *organizador* más que una teoría predictiva fina; su valor está en la sistematicidad del diseño y
en la taxonomía compartida de técnicas. La evidencia por técnica individual varía.

## (e) Implicaciones de diseño para MindBoost

- **Marco para retención/adherencia ética y planner**, claramente separado de los claims cognitivos: la
  constancia (streaks, recordatorios, metas) se diseña con COM-B (¿al usuario le falta capacidad,
  oportunidad o motivación para practicar?), no con patrones oscuros.
- Técnicas con respaldo aplicables al coach: auto-monitoreo (mostrar progreso honesto), fijación de
  metas de proceso, planes if-then ("cuando termine de desayunar, haré una sesión"), feedback.
- **Ética:** el charter prohíbe dark patterns; COM-B "motivación" debe reforzar motivación *intrínseca*
  (dominio, progreso frente a la línea base), no manipulación. Streaks sí, culpabilización no.
- **Claim permitido:** "te ayudamos a construir el hábito de practicar". **Prohibido:** mezclar
  adherencia con mejora cognitiva ("mantén tu racha y serás más inteligente").

## (f) Referencias

- Michie, S., van Stralen, M. M., & West, R. (2011). The behaviour change wheel: A new method for
  characterising and designing behaviour change interventions. *Implementation Science, 6*, 42.
- Michie, S., et al. (2013). The behavior change technique taxonomy (v1) of 93 hierarchically clustered
  techniques. *Annals of Behavioral Medicine, 46*(1), 81–95.
- Gollwitzer, P. M., & Sheeran, P. (2006). Implementation intentions and goal achievement:
  A meta-analysis. *Advances in Experimental Social Psychology, 38*, 69–119.
