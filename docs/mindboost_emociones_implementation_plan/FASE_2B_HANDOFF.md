# FASE 2B — Handoff: Módulo de EMOCIONES de MindBoost

> **Propósito de este documento.** Es el contexto completo para diseñar e implementar el
> módulo de Emociones de MindBoost en un chat NUEVO, sin tener que re-derivar nada.
> El usuario lo pega (o dice: *"lee `docs/mindboost_emociones_implementation_plan/FASE_2B_HANDOFF.md`
> y arranca la Fase 2B"*) y el agente nuevo debe: (1) hacer las preguntas de la §8,
> (2) esperar respuestas, (3) diseñar e implementar el módulo con la máxima calidad.

---

## 0. Reglas del agente que recoja esto

- **No improvises el diseño final sin respuestas a la §8.** Haz las preguntas primero.
- **No es terapia ni diagnóstico** (ver §4, innegociable).
- **Reutiliza los patrones del repo** (ver §5), no reinventes arquitectura.
- Usa los subagentes/skills relevantes: `mindboost-cognitive-science`, `mindboost-assessment-psychometrics`,
  `mindboost-personalization-engine`, `mindboost-qa-safety`, `mindboost-security-privacy`,
  y las skills `create-cognitive-exercise`, `design-adaptive-scoring`, `review-cognitive-claims`,
  `privacy-safety-review`.
- Antes de tareas no triviales corre el budget gate (`node .claude/scripts/budget-gate.mjs --task "..."`)
  — es solo para Claude Code.

---

## 1. Qué es MindBoost (contexto producto)

Entrenador cognitivo dentro de la app **MindOS**, en proceso de extraerse como producto independiente.
- **Stack:** Kotlin Multiplatform / Compose Multiplatform / Firebase (+ persistencia local-first cifrada).
- **Módulos existentes (7):** Enfoque (13 ejercicios E1–E13, el más maduro), Memoria (16 en catálogo / 3 jugables),
  Velocidad Mental, Lógica, Lenguaje (6 ejercicios), Decisiones (Bayes/Reversibilidad), Creatividad (2 ejercicios).
- **Emociones = NO EXISTE.** Es el hueco P0 y el objeto de esta fase.
- **Hub:** `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/presentation/ui/MindBoostHubScreen.kt`
  monta cada módulo como un *track* (`when(track)`). El módulo de Emociones se añade aquí como un track nuevo.

---

## 2. Veredicto de la auditoría (Fase 1) que justifica este módulo

- La **transferencia lejana** del brain-training clásico (Flanker/Stroop/N-back/PVT…) es ~0
  (Owen 2010 N=11.430; Melby-Lervåg & Hulme 2013; Simons 2016; Sala & Gobet). Mejora la tarea, no "la inteligencia".
- Las **dos palancas con transferencia real** que el producto infra-explota son **metacognición** y
  **regulación emocional**, *entrenadas como estrategia/control*, no como tarea ni como mood-tracking.
- **Conclusión:** el módulo de Emociones es de los de **mayor valor** del producto — si se diseña como
  entrenamiento de regulación/decisión bajo carga emocional, con mecanismo claro y métrica de progreso real
  (no placebo). Si se diseña como "diario de emociones bonito", es relleno.

---

## 3. Decisiones ya tomadas (Fase 2A) que mandan sobre este módulo

| Decisión | Valor elegido | Implicación para Emociones |
|---|---|---|
| Propósito del perfil | **Científico-adaptativo** | Medir con precisión + alimentar el motor adaptativo; la UI traduce, no decora. |
| Alcance | **Arquitectura definitiva por oleadas** | Diseña el sistema completo; implementa por olas (empezar por 2-3 ejercicios sólidos). |
| Emociones en v1 | **Completo** (constructo + captura de señal + ejercicios) | Es P0, no placeholder. |
| Persistencia | **Sin cerrar** (la profile-card acabó en Firebase; antes se mencionó Supabase) | Dato emocional = el MÁS sensible → ver §8-I. Patrón por defecto del repo: **local cifrado**. |

El perfil cognitivo global (`MindBoostProfile`, Fase 2A) **aún no está construido**; el módulo de Emociones
debe exponer su constructo "regulación emocional" de forma que ese perfil futuro lo consuma (overlay transversal,
como la metacognición).

---

## 4. Límites de seguridad NO negociables (emociones)

- **NO es producto médico, clínico, diagnóstico ni terapéutico.** No diagnostica ni trata ansiedad, depresión,
  trauma, TDAH ni ningún trastorno. No promete curar nada.
- **Lenguaje funcional, no clínico:** "regulación", "claridad", "práctica", "perfil funcional", "rendimiento bajo
  carga". Nunca lenguaje clínico o de patología. (El repo ya tiene un *terminology scanner* en el módulo Arena que
  prohíbe lexemas clínicos; aplica el mismo criterio aquí.)
- **Minimización de datos:** recoger lo mínimo. El estado emocional es dato sensible.
- **Señales de sobrecarga → parar:** todo ejercicio que pueda inducir malestar debe tener salida inmediata,
  límite de intensidad y mensajes de seguridad (recursos no clínicos, nunca consejo terapéutico).
- **Sin fabricar estado:** nada de presencia/valores inventados (aprendizaje del repo: no renderizar como hecho
  lo que no se mide).

---

## 5. Patrones técnicos del repo a REUTILIZAR (no reinventar)

Espejo de `features/mindboost/focus/` y `.../speed/`:
- `domain/catalog/` → tech-sheets declarativas (un catálogo inmutable id→definición; test de consistencia
  catálogo ⟺ generadores).
- `domain/exercise/` → generadores **deterministas** (sin reloj/aleatoriedad oculta; `Random(seed)` por trial).
- `domain/metrics/` → motor de métricas; **outliers (MAD/SD) solo para tendencia central**, eventos extremos
  (lapsos, impulsividad) se cuentan sobre crudos.
- `domain/adaptive/` → dificultad adaptativa (staircase / zona óptima ~80-85% acierto).
- `presentation/` → state holder (Compose state, copy-on-write) + UI por componentes.
- `data/local/` → **`SecureKeyValueStore`** (AES-GCM + Keystore en Android; in-memory en desktop/iOS) +
  DTOs `@Serializable` con `schemaVersion`. **Se persisten RESÚMENES de sesión, nunca trials crudos.**
  `kotlinx-serialization-json` está disponible.
- **Wiring:** añadir un track en `MindBoostHubScreen`.

**Guardrails de build (de CLAUDE.md, evita perder tiempo):**
- Compilar: `:shared:compileKotlinDesktop` (desktop) y `:shared:compileAndroidMain` (Android del módulo).
  Tests: `:shared:desktopTest`. Estilo: `:shared:detekt` (ratchet: archivos nuevos pasan reglas por defecto).
- Detekt en UI Compose: `@file:Suppress("FunctionNaming","MagicNumber","LongMethod","MaxLineLength")` con
  justificación; `ReturnCount` ≤2 (refactor con `runCatching`/`when`, no suprimir); en repos Firebase evitar
  catch genérico (usar `runCatching{}.getOrElse{}` y relanzar `CancellationException`).
- Compose: nunca `background(if cond Color else Brush)` (resuelve a `Any`; ramificar el `Modifier`);
  `weight`/`align` exigen receptor `ColumnScope`/`RowScope`; `cos/sin` devuelven `Double` → `.toFloat()`;
  importar `androidx.compose.runtime.getValue`/`setValue` al usar `by`; **no** `Brush.radialGradient` con
  `radius=Float.POSITIVE_INFINITY` (no pinta).
- No correr tareas Gradle en paralelo sobre el mismo worktree (serializar).

---

## 6. Las 20 capacidades emocionales objetivo (brief del usuario)

1. Identificación emocional · 2. Granularidad emocional · 3. Diferenciar emoción / pensamiento / impulso / conducta ·
4. Tolerancia a la frustración · 5. Regulación de impulsos · 6. Recuperación tras error · 7. Control de reactividad ·
8. Decisiones bajo emoción intensa · 9. Atención bajo estrés · 10. Flexibilidad cognitiva emocional ·
11. Reencuadre cognitivo · 12. Autoconciencia · 13. Calibración de intensidad emocional ·
14. Detección de sesgos emocionales · 15. Metacognición emocional · 16. Persistencia ante incomodidad ·
17. Claridad de valores · 18. Separación estado emocional ↔ verdad objetiva · 19. Reconocimiento de patrones internos ·
20. Capacidad de pausar antes de actuar.

No hay que cubrir las 20 en v1: priorizar las de mayor transferencia y mecanismo más medible (ver §8-A).

---

## 7. Marco de diseño obligatorio por ejercicio

Para CADA ejercicio propuesto, definir: **nombre · objetivo · mecánica exacta · duración · progresión ·
dificultad adaptativa · métrica principal + secundarias · feedback (inmediato/post/estratégico) ·
beneficios esperados · riesgos · límites · señales de sobrecarga · conexión cross-módulo.**
Cada ejercicio debe tener **mecanismo cognitivo-emocional claro** y **métrica de progreso real**
(distinguir mejora-en-la-tarea vs transferencia; marcar `[Riesgo de placebo]` donde aplique).

---

## 8. PREGUNTAS PARA EL USUARIO (responder antes de cerrar el diseño)

### A. Filosofía y alcance
- A1. ¿El módulo es 100% **entrenamiento de regulación/rendimiento**, o incluye también una capa de
  **autoconocimiento/introspección** (más conceptual: valores, patrones internos, claridad)?
- A2. ¿Cuántos ejercicios en v1? (¿3-4 muy sólidos vs 8-10 amplios?)
- A3. **La pregunta clave — "frío" vs "caliente":** ¿el módulo debe **inducir estados emocionales reales**
  (frustración por dificultad, presión por tiempo, fracaso provocado) para entrenar regulación *en caliente*,
  o entrenar conceptos/estrategias *en frío* (escenarios, auto-reporte, decisiones simuladas)? ¿O una mezcla?

### B. Inducción emocional (lo más delicado)
- B1. ¿Permites inducir incomodidad **leve-moderada** de forma controlada (p. ej. una tarea que se vuelve
  injustamente difícil para entrenar recuperación)? ¿Hasta qué intensidad como techo?
- B2. ¿Qué salida de seguridad quieres (botón "parar", pausa obligatoria, mensaje, recursos no clínicos)?

### C. Captura de señal emocional
- C1. ¿Auto-reporte de estado (valencia / activación / intensidad / **granularidad** = nombrar la emoción)?
  ¿Antes, después, o ambos? ¿Cada cuánto?
- C2. ¿Usar **señales de comportamiento** (tiempo de reacción, errores, impulsividad, abandono) como proxy de
  estado? (el repo ya captura un `mood1to5` en Enfoque).
- C3. ¿Algo que **NO** quieras capturar nunca (privacidad)?

### D. Conexión con otros módulos
- D1. ¿Standalone, o también **overlay** que se activa sobre Enfoque/Decisiones/Lógica
  (p. ej. "inhibe el impulso emocional", "decide bajo presión")?
- D2. ¿Debe alimentar el futuro `MindBoostProfile` con un constructo "regulación emocional"? (recomendado: sí).

### E. Personalización / perfil
- E1. ¿Empieza con un **baseline emocional** (calibración de reactividad / tolerancia a frustración)?
- E2. ¿Adapta dificultad por tolerancia a frustración / reactividad del usuario?

### F. Métricas de éxito
- F1. ¿Qué señal define "**mejoró su regulación**"? (recuperación tras error más rápida, menos impulsividad
  bajo presión, mejor calibración emoción↔rendimiento, mayor persistencia ante incomodidad…)
- F2. ¿Quieres **transfer probe / holdout** desde v1 para no confundir práctica con mejora real?

### G. UX / tono
- G1. ¿Tono del feedback: coach sobrio y técnico, o cálido? ¿Sin gamificación tipo casino?
- G2. ¿Duración/formato de sesión: micro (3-5 min) o profunda (10-20 min)?

### H. Seguridad / ética
- H1. Disclaimers concretos que quieres mostrar. Público/edad objetivo.
- H2. ¿Qué pasa si el usuario se desregula durante un ejercicio?

### I. Persistencia / backend (dato emocional = el más sensible)
- I1. ¿**Local cifrado** (patrón actual de MindBoost, máxima privacidad) o **Firebase/Supabase**?
  Recordatorio: la profile-card acabó en Firebase, pero el dato emocional es más sensible.

### J. Agresividad de implementación
- J1. ¿v1 **jugable end-to-end ya** (2-3 ejercicios completos + wiring + tests), o **diseño completo primero**
  y luego implementación?

---

## 9. Qué debe entregar el chat de Fase 2B

1. **Diseño** (científico + producto): tabla de ejercicios con el marco §7, mecanismo y métrica por ejercicio,
   curva de progresión, motor adaptativo, baseline, sistema de métricas y de feedback, claims permitidos/prohibidos,
   plan de validación (test-retest, holdout, control de práctica).
2. **Implementación** por olas siguiendo §5: feature nueva `features/mindboost/emotions/` (o nombre acordado),
   wired en `MindBoostHubScreen`, persistencia según §8-I, con tests (dominio en commonTest/desktopTest),
   compilando (desktop + Android) y Detekt limpio.
3. **Resumen de cierre:** qué cambió, archivos, tests, cómo verificar, riesgos/limitaciones.

---

## 10. Estado del repo ahora (para el agente nuevo)

- Rama: `feat/mindboost-enfoque-integration`.
- Último trabajo: **app profile card** (`features/profile/card/`) implementada desde un diseño de Claude Design
  y **persistida en Firebase** (`profile_cards/{uid}`, owner-only; reglas 104/104 verde). Esa es la pantalla de
  perfil de la APP; el **perfil del cerebro** (MindBoostProfile cognitivo) es otra pantalla aún sin construir.
- Memoria del proyecto: `~/.claude/projects/.../memory/MEMORY.md` + `CLAUDE.md` (§"Aprendizajes acumulados") —
  léelos antes de tocar código.
- Las fases previas (auditoría global = Fase 1; perfil/métricas/orquestación = Fase 2A) están resumidas arriba;
  este módulo es **Fase 2B**.
