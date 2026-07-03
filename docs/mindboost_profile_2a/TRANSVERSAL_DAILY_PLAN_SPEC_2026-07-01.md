# Spec + deferral: "Plan de hoy" transversal (cross-módulo) — MVP Fase 1, Prompt 5 (parte 2)

- **Fecha:** 2026-07-01
- **Estado:** DIFERIDO (implementación); spec listo para construir
- **Ámbito:** un plan diario que recomiende 2–3 ejercicios ACROSS módulos desde el hub
- **Relacionado:** onboarding (parte 1 de Prompt 5) SÍ implementado (nota en el hub).

## Por qué se difiere la implementación

1. **`DefaultDailyPlanGenerator` es de Enfoque, no transversal.** Genera bloques de `ExerciseId`
   de FOCUS a partir de un `UserCognitiveProfile` + `TrainingCycle`. No conoce Memoria/Lógica/etc.
   "Reutilizarlo" para un plan cross-módulo NO es reutilización directa: hace falta un planificador
   nuevo a nivel MindBoost.
2. **Requiere estado cross-módulo.** Un plan honesto (no arbitrario) debe leer la señal de cada
   módulo, que vive en namespaces cifrados distintos (`mindboost_focus`, `_memory`, `_logic`, …).
   Eso es wiring nuevo y sustancial.
3. **Sin gate de compilación/tests disponible.** La sesión se ejecutó con Android Studio abierto
   controlando el daemon de Gradle (Gradle concurrente sobre el mismo worktree está prohibido:
   CLAUDE.md 2026-06-19). Construir una feature nueva multi-archivo sin poder compilar/testear
   dejaría el repo en un estado no verificado — contra la regla "no empieces cambios que no puedas
   terminar/verificar". Un plan **arbitrario** (rotación por día del mes, etc.) sería señal
   fabricada y viola la doctrina de honestidad.

## Spec de implementación (cuando el IDE esté libre para verificar)

Feature nueva, pura y testeable, en `features/mindboost/plan/`:

1. **Dominio (commonMain, puro):**
   - `data class TrainingSuggestion(val moduleId: CognitiveModuleId, val reason: PlanReason)`
   - `enum class PlanReason { GATHER_DATA, DEVELOP_AREA, MAINTAIN, START_HERE }`
   - `object TransversalDailyPlanner { fun plan(profile: MindBoostProfile, maxItems: Int = 3): List<TrainingSuggestion> }`
     - Reglas honestas, derivadas del perfil YA agregado (no re-lee módulos):
       - Prioriza dimensiones `CALIBRATING` con menor `dataSufficiency` → `GATHER_DATA`
         (reusa la misma señal que Prompt 4 expone en el UI).
       - Luego la dimensión `EMERGING`/`ESTABLISHED` más baja → `DEVELOP_AREA`.
       - Mapea `CognitiveDimension → CognitiveModuleId` con el mapeo canónico ya existente
         (`CognitiveModuleId.primaryDimension`), evitando módulos BETA/PRÁCTICA (Velocidad/Creatividad)
         y los no cableados al perfil (Lenguaje) para no recomendar lo que no mide.
       - Determinista; sin reloj (orden por sufficiency/valor), como el resto del dominio.
   - **Reutiliza `MindBoostProfile`** (ya lo produce `MindBoostProfileService`/aggregator) — NO
     `DefaultDailyPlanGenerator`. El planner de Enfoque sigue rigiendo la recomendación DENTRO de Enfoque.

2. **Presentación:** una `TodayPlanCard` en el hub (`MindBoostHubScreen`) que muestre 2–3
   `TrainingSuggestion` con copy no-clínico y un tap → navega al track correspondiente (reusa los
   callbacks `onEnfoque/onMemoria/…` ya existentes en el hub).

3. **Wiring:** el hub obtiene el `MindBoostProfile` vía `rememberMindBoostBrainProfileStateHolder`
   (ya existe) o un state holder ligero que llame a `MindBoostProfileService`. Sin nuevos namespaces.

4. **Tests (desktopTest):** `TransversalDailyPlannerTest` con perfiles input→output conocidos:
   - perfil vacío → sugiere START_HERE en el módulo de mayor impacto (p. ej. Enfoque).
   - dimensión con menor `dataSufficiency` → primera sugerencia GATHER_DATA de su módulo.
   - excluye Velocidad/Creatividad/Lenguaje del plan (no miden autoritativamente).
   - determinista (mismo perfil → mismo plan).

## Gate

Ninguna feature de plan se da por hecha sin: (a) `:shared:desktopTest` verde incl. el nuevo test,
(b) `:shared:detekt` verde, (c) revisión `efficacy-guardian` de que las reglas del plan no fabrican
prioridad. Ejecutar con el IDE cerrado (Gradle serial).
