---
title: "Sleep and cognition: memory consolidation and the limits of popular claims"
authors: "Walker, M. P.; Diekelmann, S.; Born, J."
year: "2010–2017"
tier: T3
domains: [working_memory, learning_strategy]
constructs: [sleep, memory_consolidation, attention, sleep_deprivation]
paradigms: []
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/coach
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/orchestration/MindBoostProfileOrchestrator.kt
evidence_level: moderate
---

## (a) Tesis central

El sueño desempeña un papel causal en la **consolidación de la memoria** y en la restauración de la
atención/función ejecutiva; la privación de sueño degrada la vigilancia y el aprendizaje. La divulgación
más conocida (*Why We Sleep*, Walker) popularizó estos hallazgos, pero **contiene errores factuales y
sobre-afirmaciones documentadas** (crítica de Alexey Guzey y otros): debe tratarse como T3 y rastrearse a
la literatura primaria (Diekelmann & Born) para cualquier claim.

## (b) Claims RESPALDADOS (por la literatura primaria)

- El sueño (especialmente SWS y REM en distintas fases) favorece la consolidación de memorias recién
  adquiridas. [experimental — Diekelmann & Born 2010]
- La privación aguda de sueño reduce la vigilancia sostenida (más lapsos en PVT) y la memoria de trabajo.
  [experimental — robusto]
- Dormir tras aprender protege mejor lo aprendido que un intervalo equivalente de vigilia. [experimental]

## (c) Claims NO respaldados o refutados

- **Sobre-afirmaciones de *Why We Sleep*** (p. ej. relaciones causales exageradas con enfermedad/mortalidad,
  algunas cifras erróneas) **no** deben citarse. El libro es T3 con críticas de exactitud.
- MindBoost **no** es un producto de salud ni mide sueño clínicamente: no puede diagnosticar ni prometer
  mejoras de sueño/salud.
- La consolidación por sueño es sobre **lo aprendido**, no un multiplicador mágico de capacidad general.

## (d) Estado de replicación / críticas

La ciencia básica sueño-memoria (Diekelmann & Born) es sólida. El caso *Why We Sleep* es un recordatorio
de por qué la divulgación no se cita como evidencia: incluso un experto puede sobre-afirmar. Usar T1
subyacente y evitar el libro para claims concretos.

## (e) Implicaciones de diseño para MindBoost

- **Periodización con respaldo:** distribuir práctica con sueño de por medio ayuda a consolidar
  ([[L02_spacing_distributed_practice]], [[F06_neuroplasticity_realistic]]). El `MindBoostProfileOrchestrator`
  y el planner pueden favorecer la distribución día-a-día en vez de maratones.
- **Rol de coach acotado y honesto:** MindBoost puede *sugerir* que dormir bien ayuda a rendir en la
  práctica, sin medir sueño ni hacer claims de salud (el charter lo veta). Nada de "mejora tu sueño para
  potenciar tu cerebro".
- **Readiness/check-in:** un auto-reporte de estado (sueño/energía) puede modular la sesión sin tratarse
  como dato clínico; modelar "desconocido" como `null`, nunca fabricar.
- **Claim permitido:** "el descanso ayuda a consolidar lo que practicas". **Prohibido:** claims de salud
  del sueño o de prevención de enfermedad.

## (f) Referencias

- Diekelmann, S., & Born, J. (2010). The memory function of sleep. *Nature Reviews Neuroscience, 11*(2),
  114–126.
- Lim, J., & Dinges, D. F. (2010). A meta-analysis of the impact of short-term sleep deprivation on
  cognitive variables. *Psychological Bulletin, 136*(3), 375–389.
- Walker, M. (2017). *Why We Sleep.* Scribner. [T3 — con críticas de exactitud documentadas; no citar
  para claims concretos]
