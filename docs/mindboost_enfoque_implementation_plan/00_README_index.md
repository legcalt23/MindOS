# MindBoost · Enfoque — Plan de Implementación

> **Estado:** PLAN. No hay código de producción escrito todavía.
> Este conjunto de documentos describe *cómo* construir el sistema de
> entrenamiento de control atencional **Enfoque** (apartado **MindBoost** de
> MindVerse) respetando la arquitectura real de este repositorio KMP.

## Principio rector

La implementación sigue una jerarquía estricta **dominio → medición → adaptación
→ evaluación → ejercicios → sesiones → periodización → personalización → UI →
gamificación → analítica**. No se empieza por pantallas ni por minijuegos. Se
empieza por modelo cognitivo, métricas honestas y motor de dificultad.

## Índice de documentos

| # | Archivo | Contenido |
|---|---------|-----------|
| 01 | [01_project_architecture_audit.md](01_project_architecture_audit.md) | Arquitectura real del repo, módulos, convenciones, punto de integración, riesgos. |
| 02 | [02_domain_model_plan.md](02_domain_model_plan.md) | Entidades, enums, value objects, aggregates, interfaces, paquetes. |
| 03 | [03_metrics_engine_plan.md](03_metrics_engine_plan.md) | Fórmulas de cada métrica, inputs, outliers, RT mediano/RT-CV/SSRT/IES/LISAS/RCI. |
| 04 | [04_adaptive_engine_plan.md](04_adaptive_engine_plan.md) | DifficultyVector, reglas SUBIR/MANTENER/BAJAR/CAMBIAR_EJE/COOLDOWN, anti-oscilación, fatiga. |
| 05 | [05_exercise_engine_plan.md](05_exercise_engine_plan.md) | Arquitectura común de los 13 ejercicios, TrialGenerator, timing, tabla ejercicio↔métrica↔eje. |
| 06 | [06_evaluation_and_baseline_plan.md](06_evaluation_and_baseline_plan.md) | Training/Evaluation/Calibration, baseline, probes reservados, retests, RCI. |
| 07 | [07_session_periodization_plan.md](07_session_periodization_plan.md) | Sesiones 5/10/20/30, ciclo 8 semanas, deload, DailyFocusPlan. |
| 08 | [08_personalization_plan.md](08_personalization_plan.md) | Perfiles, inferencia desde métricas, pesos, anti-sobreentrenamiento. |
| 09 | [09_ui_ux_integration_plan.md](09_ui_ux_integration_plan.md) | Pantallas, integración con HomeDestination/AppRoute, StateHolders, incertidumbre honesta. |
| 10 | [10_data_persistence_plan.md](10_data_persistence_plan.md) | Qué guardar, esquema local-first cifrado, Firestore para resúmenes, privacidad. |
| 11 | [11_testing_validation_plan.md](11_testing_validation_plan.md) | Unit/integration/timing/regresión, simuladores de usuario. |
| 12 | [12_incremental_implementation_roadmap.md](12_incremental_implementation_roadmap.md) | Fases 0–11, orden, dependencias, criterios de aceptación, checklists. |
| 13 | [13_claude_code_execution_prompts.md](13_claude_code_execution_prompts.md) | Prompts independientes y verificables para cada fase. |
| 14 | [14_risks_and_decisions.md](14_risks_and_decisions.md) | Riesgos técnicos/científicos/producto, timing en móvil, decisiones abiertas. |
| 15 | [15_master_summary.md](15_master_summary.md) | Resumen ejecutivo, primeros 5 pasos, siguiente instrucción ideal. |

## Cómo usar este plan

1. Lee `15_master_summary.md` para la vista ejecutiva.
2. Lee `01` y `02` para entender dónde y cómo encaja.
3. Cuando quieras construir, di: **"Implementa la Fase 1 siguiendo
   /docs/mindboost_enfoque_implementation_plan"** y usa el prompt 1 de `13`.

## Convenciones heredadas del repo (resumen)

- Paquete raíz: `com.mindos.app`. Feature nueva: `features/mindboost/focus`.
- Clean arch por feature: `domain/ · data/ · presentation/`.
- Presentación = `StateHolder` (no ViewModel Android) con `mutableStateOf` +
  `CoroutineScope`, factory `rememberXStateHolder()`.
- DI manual: `AppGraph` + `rememberAppGraph()`, se inyectan **interfaces**.
- Tiempo: **nunca** `Clock.System.now()` en presentación/dominio — inyectar reloj.
- Navegación: `AppRoute` + `AppRouteStack`; tabs vía `HomeDestination`.
- Tests: `kotlin-test` + `kotlinx-coroutines-test` (`runTest`), fakes, `cancelChildren()`.
- Persistencia local: patrón `expect fun rememberXStore(namespace)` + interfaz
  `suspend load/save/clear` (Android: SharedPreferences cifradas con Keystore).
