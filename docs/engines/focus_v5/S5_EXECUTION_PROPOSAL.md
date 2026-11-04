# S5 — Propuesta de ejecución (SUPERSEDED)

> **ESTADO: SUPERSEDED (2026-07-21).** El CEO emitió la reapertura obligatoria de S5 con contrato
> propio (decisiones cerradas: estados de evidencia de sesión, rechazo de `1 − frameDropFraction`
> como default, Android API 26 / iOS 15, paridad real, ejecución de gates por Claude). Este documento
> se conserva solo como evidencia histórica de la propuesta previa. El estado vigente vive en
> `S5_REOPEN_STATUS.md` y el ADR de calidad temporal correspondiente.

> Focus V5 · Slice **S5 — V1 técnico y paridad**. Este documento describe TODO lo que propongo hacer
> ahora, para que ratifiques el enfoque antes de que toque el núcleo de medición. **Nada de esto está
> implementado todavía** salvo lo marcado ✅. No se ha tocado el scoring.

---

## 0. Contexto y estado

- **Entry gate `T-S4`:** ✅ (S4 cerrada).
- **Autorización:** tu "continúa con el plan" = el `CONTINUAR` per-slice + la autorización expresa que exige el plan.
- **Restricción de entorno:** no corro Gradle en local (Android Studio posee el daemon Gradle de este worktree;
  regla del repo + el propio plan asigna la ejecución de gates al CEO). **Yo entrego código + tests + verificación
  estática + los comandos de gate; tú los corres.**
- **Modelo de datos que condiciona todo (crítico):** `FocusRuntimeSnapshot` guarda **solo COUNTS** (tallies por
  condición), nunca trials — es `ADR-FOCUS-DATA (iii) TIERED` hecho estructural ("el motor no puede persistir lo
  que no tiene"). Todo timestamp es **nanos monotónicos**; el reducer es **puro** (nunca lee un reloj).

### Estado de las tareas de S5

| Task | Descripción | Estado | Depende de |
|---|---|---|---|
| (0) | Hz real por segmento + test REDTEAM #19 | ✅ **hecho** (working tree) | — |
| (3-CI) | iOS compila (CI) + suite en simulador | ✅ **hecho** (del port iOS) | — |
| (2) | Clase T en sesión + exclusión proporcional de segmentos degradados | ⏳ **propuesto (este doc)** | ratificación de política de medición |
| (4) | Verificación export/delete (eraser + coverage + revocación) | ⏳ autónomo | — |
| (5) | Red-team técnico (matriz) | ⏳ autónomo | — |
| (6) | Presupuestos de rendimiento (parte testeable) | ⏳ autónomo | — |
| (1) | Protocolo de timing por dispositivo | ⛔ **tú** | inventario de dispositivos |
| (3-manual) | Pasada manual en iPhone | ⛔ **tú** | tu iPhone físico |
| ADR-FOCUS-PLATFORM-MIN | API 26 vs 29 (+ posible slice de migración) | ⛔ **tú** | decisión CEO |

---

## 1. Task (2) — Clase T en sesión + exclusión proporcional  *(el núcleo — lo que quiero que ratifiques)*

### 1.1 El problema

El plan pide: *"derivación de clase T en sesión (FrameStats + fuente de timestamps → clase persistida con la
sesión); exclusión de segmentos degradados (proporcional, no binaria)"*.

Tensión de diseño real: focus2 **solo tiene el agregado** (counts por condición de toda la sesión). No puedes
excluir *proporcionalmente* un segmento degradado de algo que ya está sumado. Y focus2 **hoy no captura
frame-timing en absoluto** (cero usos de `FrameStats`/`FrameTimingProbe`). Así que task (2) es una **integración
del núcleo de medición**, no un tweak.

### 1.2 Paso 0 (obligatorio antes de diseñar): reconciliar con lo que ya existe

focus2 ya tiene conceptos de `quality`/`authoritative`/`reliab` en `FocusSessionReducer`, `FocusJournalAdmission`,
`FocusSessionLifecycle`, etc. **Antes de añadir una clase T, mapearé esos conceptos** para construir sobre ellos y
no duplicar autoridad de calidad. (No los he leído en profundidad aún; lo haré como primer paso de implementación.)

### 1.3 Diseño propuesto

1. **Captura de `FrameStats` por SEGMENTO (bloque), no por sesión.** Cablear `rememberFrameTimingProbe()` en
   `FocusTracerStateHolder`: `start()` al empezar cada bloque de medición, `stop()` al cerrarlo → un `FrameStats`
   por bloque. (En desktop/iOS el probe es `NoOp` → `FrameStats.UNAVAILABLE`.)

2. **Clase T POR SEGMENTO**, derivada de (`FrameStats` del bloque + autoridad de la fuente de timestamps). Taxonomía
   propuesta (sujeta a tu revisión):
   - `AUTHORITATIVE` — fuente de timestamps fiable **y** `FrameStats` dentro de presupuesto → cuenta pleno.
   - `DEGRADED` — jank medible por encima del umbral → **exclusión proporcional** (ver 1.4, decisión abierta A).
   - `UNRELIABLE` — `FrameStats.UNAVAILABLE` (sin probe) o fuente no autoritativa → **no cuenta para el perfil**
     (fail-closed: nunca se fabrica "autoritativo"; la dimensión queda `CALIBRATING`).

3. **Sub-tallies por segmento.** Extender el snapshot para conservar los counts **por bloque** además del agregado
   (esto **sigue siendo el tier resumen — NO son trials crudos**, no viola `ADR-FOCUS-DATA (iii)`; confirmar con
   privacy). Es lo que permite ponderar/excluir un bloque sin tirar la sesión entera. Requiere `schemaVersion++`
   del snapshot con restore fail-closed (ya existe la maquinaria en S3).

4. **Exclusión proporcional en el scoring.** El scorer pondera los counts de cada bloque por su clase T. La
   elegibilidad de la sesión depende de la **fracción de evidencia autoritativa** (p. ej. si <X% de los bloques son
   AUTHORITATIVE, la sesión es `CALIBRATING`, no un número fabricado).

5. **Persistencia.** La clase T de sesión (derivada de las clases de segmento) + las clases por segmento se
   persisten con el registro de sesión.

6. **Fail-closed en todos los bordes:** sin `FrameStats` (plataformas fallback), fuente no autoritativa, o schema
   futuro ⇒ **no autoritativo**, nunca un valor inventado. `null ≠ 0` en ambas direcciones.

### 1.4 Decisiones ABIERTAS que quiero que ratifiques (medición — no las decido yo solo)

| # | Decisión | Opciones | Mi recomendación (a validar) |
|---|---|---|---|
| **A** | Política de exclusión de un bloque `DEGRADED` | (a) binaria: excluir el bloque entero; (b) proporcional: ponderar por `1 − frameDropFraction` | El plan pide **proporcional**; propongo down-weight por `(1 − frameDropFraction)` con piso, pero la función de ponderación merece la lente `psychometrician` antes de tocar el scoring |
| **B** | Umbral `AUTHORITATIVE→DEGRADED` y `→UNRELIABLE` | `frameDropFraction`/`criticalFrameJitter` thresholds | Derivar del presupuesto de rendimiento (task 6), no un número mágico |
| **C** | ¿Sub-tallies por bloque violan el no-raw? | — | **No** (siguen siendo counts, no trials), pero lo paso por `privacy-security` en el gate de salida |
| **D** | "Fuente de timestamps" autoritativa | ¿basta el monotónico que focus2 ya usa, o hay que exigir Choreographer/nanos nativos como Velocidad? | Reconciliar con el concepto existente (paso 0) antes de decidir |

### 1.5 Archivos afectados (estimado)

- `focus2/domain/runtime/FocusRuntimeModel.kt` (sub-tallies por segmento, `schemaVersion++`)
- `focus2/domain/runtime/FocusSessionReducer.kt` (acumular por segmento)
- `focus2/domain/runtime/FocusSessionRestore.kt` (restore del nuevo schema, fail-closed)
- `focus2/presentation/FocusTracerStateHolder.kt` (cablear `FrameTimingProbe` por bloque)
- NUEVO: tipos de clase T + derivación pura (`core/timing` o `focus2/domain`)
- El scorer/elegibilidad focus2 (ponderación proporcional)
- Tests en `desktopTest` (derivación, ponderación, fail-closed, REDTEAM "bloque degradado excluido proporcional no binario")

### 1.6 Riesgos

- **Sesgo de medición** si la función de ponderación está mal → por eso pido ratificación antes de tocar scoring.
- Cambio de schema del snapshot (mitigado: restore fail-closed ya existe).
- Interacción con `ADR-FOCUS-DATA` (mitigado: sub-tallies = resúmenes, no raw; lo verifica privacy).

---

## 2. Task (4) — Verificación export/delete  *(autónomo)*

- **Alcance:** demostrar por test que el eraser borra TODOS los namespaces de focus2 (`mindboost_focus2` + los
  que S1 añadió), con cobertura (`AccountBoundErasureCoverageTest` espejo), y que la revocación de consentimiento
  se respeta (fail-closed: nunca reportar borrado sin demostrarlo).
- **Entregable:** tests de cobertura + revocación; sin cambio de producto salvo huecos que surjan.

## 3. Task (5) — Red-team técnico  *(autónomo)*

- **Matriz REDTEAM (escenarios marcados S5):** process death (restore), reloj civil/DST (que no mueva ventanas —
  ya estructural, lo demuestro), schema antiguo (rechazo fail-closed), permiso revocado, flag mal configurada
  (default deniega), disk-full (escritura write-ahead que falla → outcome tipado, no falso éxito).
- **Entregable:** un test por escenario, cada uno demostrando el comportamiento seguro.

## 4. Task (6) — Presupuestos de rendimiento  *(autónomo, parte testeable)*

- **Alcance:** presupuestos de frame-pacing como tests/benchmarks donde sea posible (deriva los umbrales de la
  decisión B de task 2). La medición en dispositivo real es task (1) (tuya).

---

## 5. Items que dependen de TI (CEO) — no los invento

- **(1) Protocolo de timing por dispositivo:** necesito tu **inventario de dispositivos** (modelos + Hz de pantalla).
- **(3-manual) Pasada en iPhone:** requiere tu iPhone físico (la parte CI ya está verde).
- **ADR-FOCUS-PLATFORM-MIN (API 26 vs 29):** decisión tuya. No bloquea (0/2/4/5); sí condiciona (1) y si hay slice
  de migración. Te la plantearé como "una pregunta por ADR" cuando lleguemos a (1), o antes si prefieres.

---

## 6. Orden propuesto, gates y cierre

1. **Orden:** (2) → (4) → (5) → (6). Cada uno con su gate.
2. **Gates (los corres tú):** `:shared:desktopTest` (filtrado por la suite tocada) + `:shared:detekt` +
   `:androidApp:compileDebugKotlin`. El gate iOS ya es verde en CI.
3. **Gate de salida de S5 (no negociable):** `efficacy-guardian` + `psychometrician` (la ponderación de task 2) +
   `diff-auditor` + `privacy-security` (export/delete + no-raw de sub-tallies), en contexto fresco.
4. **Ledger:** sello con las findings de S5 al cierre (single-writer), recomputo SHA-256, valido.
5. **Cierre de 9 partes** al terminar la fase (o punto de control si un eje queda CEO-bloqueado).

---

## 7. Qué NO haré sin tu confirmación

- **No tocaré el núcleo de scoring** (task 2) hasta que ratifiques la política de exclusión (decisión A) y el
  enfoque de clase T. Es medición: prefiero tu visto bueno a arriesgar un sesgo silencioso.
- No abordaré los items CEO-dependientes (1, 3-manual, PLATFORM-MIN) sin tu input.
- No haré commit/PR (regla del repo): reporto cambios del working tree.

---

**En una frase:** propongo implementar (2) como una integración honesta del núcleo de medición (frame-timing por
segmento → clase T → exclusión proporcional dentro del tier resumen, fail-closed), pero **primero quiero tu OK a la
política de exclusión (decisión A) y a la taxonomía**, porque toca el scoring; y luego seguir con (4)(5)(6)
autónomas. Dime si el enfoque es el correcto o qué ajustar.
