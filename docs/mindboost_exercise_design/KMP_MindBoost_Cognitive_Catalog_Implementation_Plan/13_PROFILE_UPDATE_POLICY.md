# 13 — Política de actualización de perfil: cuándo una sesión cuenta, calibra o no cuenta

> **Fuentes leídas:** `INFRA_P0.md`, `AUDIT_V2_TECNICO.md`, `AUDIT_V2_GROUND_TRUTH.md`.
> Confianza: `[hecho]` / `[inferido]` / `POR_CONFIRMAR_EN_REPO`. Depende de INFRA_01 (estados),
> INFRA_02 (timebase/jank), INFRA_06 (`MinSampleGate`/`ProfileUpdateGate`), doc 04 (arquitectura).

---

## 0. Invariantes (repetidos donde aplican)

1. **`null != 0`** `[hecho]`: solo-calibrando y no-cuenta NUNCA se serializan como número.
2. **Enfoque y Velocidad NO actualizan perfil** hasta timebase autoritativa + jank-gate real
   (`frameDropFraction` medido, no el `0.0` hardcoded) `[hecho, GROUND_TRUTH §timing]`.
3. **Cero claims** de far-transfer/clínico/IQ/fake-news/regulación emocional, ni para negarlos.
4. **Anti-gaming solo cuenta si implementado+testeado** con perfiles sintéticos adversarios.
5. **Banco pequeño/no validado bloquea perfil** `[hecho, GROUND_TRUTH §banks]`.

---

## 1. Las tres clases de resultado de sesión

| Clase | Significado | Efecto en perfil | UI |
|---|---|---|---|
| **ACTUALIZA** | señal válida y suficiente | `ProfileUpdateGate` contribuye al aggregator | valor + banda (preliminar o estable) |
| **SOLO-CALIBRANDO** | jugable, evidencia parcial/inmadura | NO toca perfil; acumula para futura agregación | `—` / "calibrando tu línea base" |
| **NO-CUENTA** | medición contaminada/inválida/no fiable | NO toca perfil; sesión descartada | "no contabilizada" / "no actualizado por calidad" |

**Regla nuclear:** SOLO-CALIBRANDO y NO-CUENTA son **ausencia** de actualización, nunca una
actualización a la baja. Una señal inválida jamás reduce el perfil (R-NO-REDUCE).

---

## 2. Matriz estado × acción (autoritativa)

`state` viene de `ScoreStateMachine` (INFRA_01). La acción es determinista — un único mapeo.

| `ScoreState` | Clase | `updatesProfile` | `preliminary` | Copy obligado (allowlist doc copy) |
|---|---|---|---|---|
| `VALID` (robusto: conf≥0.7 + inter-sesión) | **ACTUALIZA** | ✅ | false | "Progreso contra tu línea base personal (esta tarea)" |
| `VALID` (preliminar) | **ACTUALIZA** | ✅ | **true** | "Estimación preliminar" |
| `CALIBRATING` | SOLO-CALIBRANDO | ❌ | true | "Calibrando tu línea base" (`—`, nunca 0) |
| `INSUFFICIENT_N` | SOLO-CALIBRANDO | ❌ | true | "Datos insuficientes para estimar" |
| `DEVICE_UNRELIABLE` | NO-CUENTA | ❌ | true | "No actualizado por calidad de medición" |
| `BANK_CONTAMINATED` | NO-CUENTA | ❌ | true | "Estimación no fiable: contenido repetido" |
| `SUSPECTED_GAMING` | NO-CUENTA | ❌ | true | neutro, sin acusar |
| `INVALIDATED` | NO-CUENTA | ❌ | true | "Esta sesión no cuenta para tu perfil" |
| `ABANDONED_SAFE` | NO-CUENTA | ❌ | true | "Puedes parar cuando quieras" (sin culpa) |

> Todos los estados son **jugables** (siempre hay feedback local). "Jugable" ≠ "actualiza perfil".

---

## 3. Reglas de transición a ACTUALIZA

Una sesión llega a `VALID` solo si pasa, en cascada, TODOS los gates (INFRA_01):

1. **Completada** (o no es ejercicio emocional/frustración → si abandona ahí, `ABANDONED_SAFE`).
2. **Timebase autoritativa** si es `RT_CRITICAL` (INFRA_02). Focus/Speed-desktop/iOS NO la tienen hoy
   `[hecho]` → no-cuenta para RT-crítico.
3. **Jank bajo umbral** (`frameDropFraction` medido, no hardcoded; `criticalFrameJitter=false`).
4. **Banco no contaminado** (no holdout agotado, no memorizado; invariante 5).
5. **Sin flag anti-gaming real disparado**; y **detector adecuado existe** para la criticidad
   (detector ausente en RT-crítico ⇒ `CALIBRATING`, no `VALID`).
6. **No invalidada** (AGRS≤0.75, quality no LOW).
7. **`nEffective ≥ nMin`** por celda (INFRA_06) y señal primaria computable.
8. **Accuracy secundaria** sobre su gate (RT rápido con accuracy basura no es velocidad útil).
9. **Métrica estable**.

`preliminary=false` (consolidación plena) **solo** con `confidence ≥ 0.7` **y** agregación inter-sesión.

### Regla específica por familia
- **Comparativas** (CONGRUENCY/SWITCH/PRESSURE/DUAL/POST_ERROR): ACTUALIZA solo con ≥20/celda **y todas
  las celdas presentes** + IC.
- **SSRT:** ACTUALIZA solo con nStops≥30, `p∈[0.25,0.75]` y waiting-strategy descartada.
- **Brier:** NUNCA standalone; solo agregado inter-sesión (≥40-60) en capa metacognitiva separada.
- **RCI (Memoria):** ACTUALIZA solo con `isGenuine && reliable` (`|RCI|>1.96`, ≥2-3 admins equated);
  por defecto `NotEnoughData` → SOLO-CALIBRANDO.

---

## 4. Agregación inter-sesión

- SOLO-CALIBRANDO **acumula** `nEffective` entre sesiones hacia su `nMin`; cuando lo alcanza y pasa el
  resto de gates, transiciona a `VALID` (`CALIBRATING → VALID`). Nunca al revés por debajo de su `nMin`.
- NO-CUENTA **no acumula** nada (la sesión se descarta entera).
- El composite global del brain-profile sigue exigiendo `≥3 dimensiones ESTABLISHED` `[hecho]`.

---

## 5. Caso por módulo (estado HOY, honesto)

| Módulo | Clase típica HOY | Por qué |
|---|---|---|
| **Speed/Android** | ACTUALIZA-preliminar (cuando §15 cierre) | timebase autoritativa, pero `preliminary=true` SIEMPRE mientras §15 abierto + jank hardcoded `[hecho]` |
| **Speed/Desktop·iOS** | NO-CUENTA (RT-crítico) | fallback no-autoritativo `[hecho]` |
| **Focus** | SOLO-CALIBRANDO / NO-CUENTA | `MonotonicClock` corrutina, no frame-accurate `[hecho]` → RT-crítico no-update hasta INFRA_02 |
| **Decisiones** | ACTUALIZA (no-RT) tras gates | Brier/calibración no son RT; AGRS invalida sesiones `[hecho]` |
| **Logic** | SOLO-CALIBRANDO | sin detector anti-gaming + banco 6 fijos `[hecho]` |
| **Memory** | SOLO-CALIBRANDO por defecto | RCI `NotEnoughData` hasta ≥2-3 probes `[hecho]` |
| **Language** | SOLO-CALIBRANDO / NO-CUENTA | banco 36 fijo memorizable `[hecho]` → `BANK_CONTAMINATED` tras repetición |
| **Creativity** | SOLO-CALIBRANDO (tope) | content-integrity BLOCKED `[hecho]` → claim tope `PRACTICE_SIGNAL` |
| **Emotions** | SOLO-CALIBRANDO + `ABANDONED_SAFE` | banco pequeño + abandono seguro `[hecho]` |

---

## 6. Verificación

| Criterio | Check |
|---|---|
| Cada estado mapea a exactamente una clase | test exhaustivo de la matriz §2 |
| NO-CUENTA/SOLO-CALIBRANDO nunca escriben perfil | spy sobre aggregator: 0 invocaciones |
| Nunca actualización a la baja | property test: perfil tras no-VALID == perfil previo |
| `null != 0` en UI | canario: estado no-VALID ⇒ `ScoreUiState.Calibrating/InsufficientData`, jamás valor |
| Copy obligado presente | scanner copy (INFRA-5) verifica allowlist por estado |

---

## Resumen

Tres clases — **ACTUALIZA / SOLO-CALIBRANDO / NO-CUENTA** — derivadas de forma determinista del
`ScoreState` (matriz §2). Solo `VALID` actualiza; el resto es ausencia de actualización, jamás un valor
a la baja. Enfoque y Velocidad permanecen en SOLO-CALIBRANDO/NO-CUENTA para RT-crítico hasta tener
timebase autoritativa + jank real (INFRA_02). `null != 0`, cero far-transfer, anti-gaming solo si
testeado, banco no validado bloquea perfil.
