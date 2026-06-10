# 12 · Roadmap de Implementación Incremental

> Orden exacto, dependencias, criterios de aceptación y checklists por fase.
> **Regla de oro:** dominio → métricas → adaptación → evaluación → vertical slice
> (E1) → núcleo → sesiones → personalización → avanzados → reportes → validación.
> Nunca empezar por UI, minijuegos ni gamificación.

## Dependencias entre fases

```
F0 Auditoría ─▶ F1 Dominio ─▶ F2 Métricas ─▶ F3 Adaptación ─▶ F4 Evaluación
                                   │              │               │
                                   └──────┬───────┴───────┬───────┘
                                          ▼               ▼
                                   F5 Vertical slice (E1) ───▶ F6 Núcleo (E2-E5)
                                          │
                                          ▼
                                   F7 Sesiones/Periodización ─▶ F8 Personalización
                                          │
                                          ▼
                                   F9 Avanzados (E6-E13) ─▶ F10 Reportes ─▶ F11 Validación
```

Regla: **no avanzar de fase sin verde en sus criterios de aceptación.** F2 no
empieza sin F1 compilando; F5 (UI) no empieza sin F2+F3 testeados.

---

## FASE 0 — Auditoría ✅ (ya hecha → doc 01)
- [x] Estructura, arquitectura, convenciones, punto de integración, riesgos.
- **Done:** doc `01_project_architecture_audit.md` existe y es correcto.

## FASE 1 — Dominio puro
**Construir:** enums, value objects, `DifficultyVector`, `ExerciseDefinition`,
`Stimulus/Response`, `TrialResult/BlockResult/SessionResult`,
`UserCognitiveProfile`, interfaces de repo. (doc 02)
**NO construir:** lógica de cálculo, UI, persistencia real.
- [ ] Compila en `commonMain` sin Compose/Firebase/plataforma.
- [ ] Value objects validan invariantes (tests).
- [ ] Inmutable; cero `var` público.
- [ ] Cero deps nuevas.
**Criterio de aceptación:** `:shared:compileKotlinDesktop` + tests de dominio verdes.

## FASE 2 — Motor de métricas
**Construir:** `MetricsEngine`, limpieza RT (MAD/anticipaciones), accuracy, RT
mediano, RT-CV, lapsos, FA/omisiones, PES, IES/RCS/LISAS, SSRT, efectos
(congruencia/switch/set-size/dual/pressure), índices, RCI gated. (doc 03)
**NO construir:** adaptación, UI.
- [ ] Cada `MetricKey` con función pura + test de vector conocido.
- [ ] RT mediano/RT-CV primarias; media nunca primaria.
- [ ] SSRT por integración + detección de gaming.
- [ ] Edge cases doc 03 §8 cubiertos; `MetricValue` con `n`/`confident`.

## FASE 3 — Motor adaptativo
**Construir:** `AdaptiveEngine.decide`, reglas SUBIR/MANTENER/BAJAR/CAMBIAR_EJE/
COOLDOWN/STOP/CONTAIN_IMPULSIVITY, histéresis, fatiga, mejora falsa. (doc 04)
**NO construir:** UI, ejercicios concretos.
- [ ] Función pura determinista; un eje por decisión.
- [ ] Seguridad > progresión; anti-oscilación testeada.
- [ ] Nunca afirma "mejora real"; solo opera en TRAINING.

## FASE 4 — Baseline y evaluación
**Construir:** `Mode` enforced, `BaselineProtocol`, `CalibrationEngine`,
`ProbeBankRepository` (disjunto), retests, RCI gated, cuestionario de estado. (doc 06)
- [ ] Test que falla si se adapta en EVALUATION/CALIBRATION.
- [ ] Banco de probes disjunto del de training.
- [ ] Calibración produce umbrales/dificultad inicial.
- [ ] RCI gated por `reliability`.

## FASE 5 — Primer vertical slice (E1 Vigía)
**Construir:** `E1VigiaGenerator`, `E1DifficultyMapping`, `ExerciseRuntime` +
`TrialClock` (expect/actual monótono), `E1Renderer`, `SessionRunnerStateHolder`,
`FocusLocalStore` (Android real + stubs desktop/iOS), `FocusHomeScreen` mínima,
sesión 5/10 min, wiring `HomeDestination.MindBoost`. (docs 05/09/10)
**NO construir:** otros ejercicios, periodización completa, reportes.
- [ ] Lazo completo E1: generar→presentar→medir→adaptar→persistir resumen.
- [ ] RT con reloj monótono (test de timing).
- [ ] Persistencia local cifrada (patrón del repo).
- [ ] Wiring por un solo `HomeDestination` (cero cambios en otras features).
- [ ] `actual` en android/desktop/ios (stubs documentados).
**Criterio:** una persona puede hacer una sesión de 5 min de E1 en desktop/Android
y ver un resumen honesto; el resumen queda persistido.

## FASE 6 — Ejercicios núcleo (E2, E3, E4, E5)
**Construir:** generadores + mappings + renderers de E2 Filtro, E3 Gatillo,
E4 Freno (SSD adaptativo), E5 Conflicto. Métricas específicas ya existen (F2).
- [ ] Cada uno: test de generación determinista + clasificación de outcomes.
- [ ] E4: SSD converge a p(stop)≈0.5; SSRT calculado y gaming detectado.
- [ ] Reutilizan `ExerciseRuntime`/`StimulusPayload` (sin duplicar runtime).

## FASE 7 — Sesiones y periodización
**Construir:** `SessionBlueprints` (5/10/20/30), `TrainingCycle` (8 sem + deload),
`RecoveryPolicy`, `DailyPlanGenerator`. (doc 07)
- [ ] Plantillas suman duración exacta.
- [ ] Deload en semanas 4 y 8; retest completo.
- [ ] `DailyPlanGenerator` respeta semana/disponibilidad/RecoveryGate.

## FASE 8 — Personalización
**Construir:** `ArchetypeInference`, `PersonalizationProfile`, `ProfileEvolution`,
`OvertrainingGuard`. (doc 08)
- [ ] Inferencia testeada con perfiles sintéticos (simuladores doc 11).
- [ ] Impulsivo no premia velocidad; micro-pausa opcional.
- [ ] Perfil evoluciona con histéresis; guard puede forzar descanso.

## FASE 9 — Ejercicios avanzados (E6–E13)
**Construir:** E6 Cambio, E7 Rastreo, E8 Dúo, E9 Enjambre (MOT), E10 Flujo (RSVP),
E11 Ancla, E12 Re-enganche, E13 Presión. Render con movimiento/timing fino.
- [ ] Cada uno con tests; los de movimiento (E7/E9) verificados en device.
- [ ] E10 RSVP: timing de stream estable; attentional blink medible.
- [ ] Ninguno bloquea el lazo de medición ya validado.

## FASE 10 — Reportes y transferencia
**Construir:** `ProgressScreen`, Focus Score con incertidumbre, tendencias,
near-transfer (probes), microprotocolos funcionales etiquetados como débiles. (docs 09/14)
- [ ] Probes destacados como evidencia fuerte; intra-ejercicio como secundaria.
- [ ] Toda cifra con `confident`/banda.
- [ ] Cero afirmaciones de transferencia lejana.

## FASE 11 — Testing fuerte y validación
**Construir:** simuladores de usuario completos, tests de motores, timing,
persistencia, regresión; revisión científica. (doc 11)
- [ ] 5 simuladores con asserts de comportamiento del sistema.
- [ ] ≥80% cobertura en dominio puro.
- [ ] Revisión científica de fórmulas y claims firmada.

---

## Anti-objetivos por fase (qué NO hacer todavía)

| Fase | NO hacer |
|------|----------|
| 1–4 | Nada de UI, nada de Compose, nada de Firebase real. |
| 5 | Otros ejercicios, gamificación, reportes, periodización completa. |
| 6 | Animaciones decorativas, ranking, badges. |
| 7–8 | Ejercicios con movimiento (van en F9). |
| 9 | Sobre-optimizar render antes de que el ejercicio funcione. |
| 10 | Prometer transferencia lejana / vanity metrics. |

## Gates de calidad (cada fase)

1. Compila (`:shared:compileKotlinDesktop` mínimo; Android cuando hay actuals).
2. Tests verdes (`:shared:desktopTest`).
3. Detekt sin findings nuevos fuera de baseline.
4. Cero dependencias nuevas sin justificación escrita en doc 14.
5. Actuals en los 3 targets (android/desktop/ios) cuando hay `expect`.
