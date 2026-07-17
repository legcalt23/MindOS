# MindBoost — Auditoría técnica MVP y plan de refactor (Fase A + B)

> Fecha: 2026-07-04 · Rama: `feat/mindboost-enfoque-integration` · HEAD: `2a06c37c`
> Autor: auditoría estática (Principal/Staff/Security). Alcance verificado: **espina dorsal del MVP**
> (scoring, calibración, perfil global, persistencia, reglas Firestore, taxonomía, Auth). **No** es una
> revisión línea-a-línea exhaustiva de los ~cientos de archivos del feature. La verificación es **estática**
> (sin builds: el IDE posee el daemon Gradle en este worktree; los gates se ejecutan por el usuario).

---

## 0. Resumen ejecutivo (léelo primero)

La premisa del brief ("elimina spaghetti, hacks, lógica mezclada, mocks en producción, scoring incorrecto")
**no coincide con la realidad del core de MindBoost**. Lo que hay es un sistema **maduro, endurecido y
rigor-first**, con:

- **Máquina de estados de score honesta** (`CALIBRATING/EMERGING/ESTABLISHED`, `null ≠ 0`, `ScoreStateMachine`,
  `MinSampleGate`, `ProfileUpdateGate`).
- **Scorers bespoke por módulo** (span complejo, razonamiento ICR, Brier/CDE de decisiones, pares asociados,
  recuerdo diferido, rúbrica de creatividad…), **no** una fórmula genérica.
- **Agregador puro** que compone solo dimensiones `ESTABLISHED` (`MindBoostProfileAggregator`,
  `OverallSummary.compositeIndex`).
- **Persistencia local cifrada** (AES-256-GCM + Android Keystore vía `SecureKeyValueStore`); **los datos
  cognitivos nunca suben a la nube**.
- **Reglas Firestore endurecidas** para lo que sí es cloud (social/DM/wallet/marketplace): `uid == userId`,
  `request.query.limit`, App Check.
- **Suite de tests amplia** (cientos de `desktopTest`) y una disciplina de "aprendizajes acumulados" en
  `CLAUDE.md` que ya documenta y previene decenas de fallos.

**Conclusión núcleo:** ejecutar el brief **al pie de la letra sería una REGRESIÓN**, no una mejora, en tres
frentes (ver §1). El trabajo de valor real es **distinto** al que asume el brief y está en §3 (plan por
prioridades). Antes de tocar código hay **3 decisiones de producto/arquitectura** que solo el dueño puede
tomar (§2).

---

## 1. Conflictos: dónde el brief REGRESARÍA el código

Estos tres puntos son la parte más importante de la auditoría. El brief pide implementarlos, pero hacerlo
**degradaría** la calidad actual. Regla de proyecto: *no cambiar semántica de scoring ni alcance de datos en
silencio*.

### C1 — Fórmula de scoring del PRD vs. modelo riguroso existente  ⚠️ REGRESIÓN si se aplica literal
- **PRD pide:** `BlockSessionScore = 0.30·Accuracy + 0.20·Speed + 0.20·Difficulty + 0.15·Consistency +
  0.10·Progress + 0.05·ErrorControl`; histórico `0.85·prev + 0.15·session` (EWMA); `GlobalScore = media(8
  bloques)`; rango tras 5 sesiones.
- **Realidad (verificada):**
  - Cada módulo tiene su scorer honesto: `ComplexSpanScorer`, `ReasoningScorer`, `AssociativePairsScorer`,
    `DeferredRecallScorer`, `ReversibilityScorer` + `ScoringEquations` (CDE/PHD/ICD/IED/CRI/Brier),
    `CreativityRubricScorer`. La velocidad **no** premia a costa de precisión (gates anti-gaming testeados).
  - `MindBoostProfileAggregator` **no** hace media simple: pondera por `confidence·dataSufficiency` y el
    `compositeIndex` es la **media SOLO de dimensiones `ESTABLISHED`** (mín. 3), o `null`.
  - `CalibrationPolicy`: `EMERGING` a ≥2 sesiones; `ESTABLISHED` a ≥5 sesiones válidas repartidas en ≥3 días
    distintos. **Más estricto** que "5 y ya".
- **Por qué la fórmula del PRD es peor aquí:** (a) una media de 8 bloques fabrica un número global aunque
  varias dimensiones estén sin evidencia (viola `null ≠ 0` y el charter del proyecto); (b) un EWMA plano
  ignora la incertidumbre por-dimensión; (c) un "score de velocidad" genérico reintroduce el gaming que los
  detectores actuales bloquean.
- **Recomendación:** **preservar** el modelo riguroso. Si se necesita un "Global Score 0–100" de cara al
  usuario, derivarlo como **capa de presentación** desde `compositeIndex` (`UserDisplayScoreMapper` ya mapea
  0..1 → display), sin tocar la matemática interna. NO reemplazar los scorers.

### C2 — "Firestore cloud + sync + protección cross-user" vs. diseño local-only  ⚠️ CHOCA con privacy-by-design
- **PRD pide:** sesiones/scores en `users/{uid}/sessions`, `blockScores/{blockId}`, cola offline con
  `SyncStatus`, protección de acceso cruzado.
- **Realidad (verificada):** `firestore.rules` **no tiene ninguna colección MindBoost** (solo users, profiles,
  social, DM, wallet, marketplace). `MindBoostProfile` documenta explícitamente *"local-only, never uploaded"*.
  Un grep por uso de Firestore en todo `mindboost/` devuelve prácticamente cero (1 archivo de repositorio a
  revisar). Los datos cognitivos son **sensibles** y viven **cifrados en el dispositivo** (minimización GDPR).
- **Implicación real (riesgo de producto, no bug):** al ser cifrado *device-bound* y excluido de backups, si el
  usuario pierde/resetea el dispositivo **el historial cognitivo se pierde**. Ese es el auténtico riesgo de
  "pérdida de progreso" — y es una **decisión** (privacidad total vs. continuidad multi-dispositivo), no un
  fallo a "arreglar".
- **Nota:** la sección de "protección cross-user en Firestore" del PRD **no aplica** a datos cognitivos (no hay
  cloud que proteger). Para lo que **sí** es cloud (social/wallet), las reglas ya están endurecidas.
- **Recomendación:** por defecto **mantener local-first**; si se quiere continuidad, añadir **export/backup
  cifrado opt-in** o un sync explícito (nueva superficie con reglas + consentimiento GDPR) — pero como decisión
  consciente, no como "cumplir el PRD".

### C3 — Elite (2 €/mes) + IA Coach  ❌ GAP real vs. PRD, pero fuera del alcance construido
- **PRD pide:** MindBoost Elite + IA Coach con resúmenes estructurados, límites diarios, gating no-Elite.
- **Realidad (verificada):** no existe subsistema Elite/suscripción/AI-Coach en `mindboost/`. La monetización
  hoy es la **economía wallet/marketplace** (colecciones `wallets`, `ledger_*`, `purchases`, `marketplace_*`
  en reglas), que es un sistema de tokens, no "Elite 2 €/mes + coach". La capa IA está **diferida/bloqueada**
  (p. ej. Creatividad "AI BLOCKED" en memoria de proyecto).
- **Recomendación:** tratarlo como **feature nueva grande** (no refactor). Requiere su propio diseño de
  producto + backend + privacidad + gating. No es parte de "limpiar el código".

---

## 2. Decisiones que necesito del dueño antes de Fase C

1. **Modelo de scoring** (C1): ¿preservar el modelo riguroso actual (recomendado) / reemplazar por la fórmula
   simple del PRD / híbrido con "Global Score 0–100" solo en presentación?
2. **Topología de persistencia** (C2): ¿mantener local-only + export/backup opt-in (recomendado) / añadir sync
   Firestore multi-dispositivo / decidir más adelante?
3. **Punto de arranque de la ejecución** (§3): ¿P0 de endurecimiento / unificación de namespace / solo
   entregar auditoría por ahora / abrir Elite+IA Coach?

---

## 3. Hallazgos priorizados (Fase A) + plan (Fase B)

Prioridad: **P0** seguridad/pérdida de datos/scoring incorrecto/cross-user · **P1** arquitectura/capas ·
**P2** limpieza/tests/naming · **P3** opcional.

| # | Hallazgo | Riesgo | Impacto | Archivo/área | Prioridad | Confianza |
|---|----------|--------|---------|--------------|-----------|-----------|
| H1 | **Pérdida de progreso por diseño device-bound**: datos cognitivos cifrados y no respaldados; reset de dispositivo = pérdida total del historial. | Medio-Alto (UX/retención) | Alto | `SecureKeyValueStore`, `Local*Repository` | P0 (decisión) | Alta |
| H2 | **Split de namespaces**: coexisten `com.mindos.app`, `com.mindverse.mindboost` (core scoring legacy) y referencias `com.example.mindos` en aprendizajes. Un core de dominio duplicado dificulta razonar sobre la fuente de verdad. | Medio | Alto (mantenibilidad) | `com/mindverse/mindboost/core/**` vs `com/mindos/app/**` | P1 | Alta |
| H3 | **PRD scoring vs. modelo real** (C1): riesgo de que un agente "cumpla el PRD" y regrese el rigor. | Alto (integridad de producto) | Alto | `profile/domain/aggregation`, `core/scoring` | P0 (decisión) | Alta |
| H4 | **Elite + IA Coach ausentes** (C3): gap MVP vs PRD. | Bajo (no es deuda, es scope) | Medio | — (feature nueva) | P2/P3 | Alta |
| H5 | **`GlobalScore`/rango no se surface hasta ≥3 dimensiones ESTABLISHED**: correcto y honesto, pero el usuario ve "—" mucho tiempo; falta claridad de expectativas en UI. | Bajo | Medio (UX) | `BrainProfileUiModelBuilder`, copy | P2 | Media |
| H6 | **Módulos deliberadamente CALIBRATING** (Memoria/Decisiones/Lenguaje sin evidencia de sesión o sin banco validado): correcto por diseño; documentar como estado esperado, no bug. | Bajo | Bajo | `*ProfileSource`, `CalibrationPolicy` KDoc | P3 | Alta |
| H7 | **`FocusRepositories.kt`** es el único archivo de `mindboost/` con posible superficie Firestore/collection — verificar que no filtre datos cognitivos a cloud. | Bajo-Medio (privacidad) | Medio | `focus/domain/repository/FocusRepositories.kt` | P1 | Media (pendiente lectura) |
| H8 | **Cobertura de tests de scoring/calibración**: amplia (`CalibrationPolicyTest`, `MindBoostProfileAggregatorCalibrationTest`, `MindBoostProfileAggregatorTest`). Gap cerrado: faltaba probar que una dimensión EMERGING con valor se EXCLUYE del `compositeIndex`. | Bajo | Medio | `desktopTest/**/profile/**` | P2 | ✅ Resuelto (§7) |

### Orden recomendado de ejecución (cuando el dueño decida)
- **P0-a (decisión):** resolver C1/C2/C3 (§2). Sin esto, cualquier edición de scoring/persistencia es riesgo.
- **P0-b:** si se elige continuidad → diseñar **export/backup cifrado opt-in** (aditivo, sin exponer cloud).
- **P1-a:** **unificar namespace** (`com.mindverse.mindboost.core.scoring` → `com.mindos.app…`) en fases
  pequeñas, un símbolo a la vez, con tests verdes entre pasos (H2).
- **P1-b:** auditar `FocusRepositories.kt` (H7) y sellar por escrito el invariante "cero cloud para cognitivo".
- **P2:** endurecer/completar tests faltantes de composite/ceiling (H8); clarificar copy de expectativas de
  calibración en la UI del perfil (H5).
- **P3:** Elite + IA Coach como feature nueva (H4), con su propio brief.

**Cómo validaré sin romper nada:** cambios en fases pequeñas; tras cada fase, el usuario ejecuta los gates
(`:shared:desktopTest`, `:shared:detekt`, `mindboostCopySafetyScanner*`, `:androidApp:compileDebugKotlin`),
porque el IDE posee el daemon en este worktree. Nada de `clean`/`--stop`. Sin commits salvo que se pidan.

---

## 4. Checklist PRD (estado real)

| Requisito PRD | Estado | Nota |
|---|---|---|
| Cuenta obligatoria (Auth) | ✅ | `FirebaseSessionRepository` (email/pass + verificación); app gatea por `authState`. *Spot-check recomendado.* |
| Onboarding / evaluación inicial | ⚠️ Parcial | Existen hub + planner + progreso; confirmar flujo de evaluación inicial formal. |
| 8 motores cognitivos | ✅ | 8 módulos / 9 dimensiones (`CognitiveModuleId`, `CognitiveDimension`). |
| Sesiones por duración (120s…30min) | ✅ (gate 120s) | `ProfileSessionEligibility.MIN_DURATION_MS = 120_000`. |
| Score 0–100 por bloque | ✅ (modelo distinto) | Normalizado 0..1 internamente; display mapea a 0–100. |
| Global Score = media de 8 bloques | ⚠️ Diverge (mejor) | `compositeIndex` = media de dimensiones ESTABLISHED (≥3), no media ciega. |
| Calibración tras 5 sesiones | ✅ (más estricto) | ≥5 válidas en ≥3 días distintos. |
| Rango solo tras calibración | ✅ | `CALIBRATING` → `value=null`, jamás 0. |
| Perfil provisional pre-calibración | ✅ | `preliminary=true`, copy no-clínico. |
| Firestore seguro (cognitivo) | ⚠️ N/A | Cognitivo es local-only cifrado (no hay cloud que proteger). |
| Firestore seguro (social/wallet) | ✅ | `uid==userId`, `query.limit`, App Check. |
| Offline / sync sin pérdida | ⚠️ Parcial | Local inmediato sí; **sin** sync/backup cloud → riesgo H1. |
| Sin DNI / sin anuncios | ✅ | — |
| Sin claims médicos | ✅ Fuerte | Copy scanner + taxonomía funcional + estados honestos. |
| Sin acceso cruzado entre usuarios | ✅ | Cognitivo local; cloud con reglas `uid==userId`. |
| Elite + IA Coach limitada | ❌ | No construido (feature nueva). |
| GDPR (borrado de cuenta) | ✅ | Cloud Function de borrado con erasure + namespaces AccountBound. |
| Tests en lógica crítica | ✅ (amplia) | Cientos de `desktopTest`; completar H8. |

---

## 5. Deuda técnica restante (real)

- **H2 namespace split** — P1. Riesgo: confusión sobre la fuente de verdad del scoring. Recom.: migración por
  fases con tests verdes entre pasos.
- **H1 continuidad de datos** — P0-decisión. Riesgo: pérdida de historial al perder dispositivo. Recom.:
  export/backup cifrado opt-in.
- **H7 superficie Firestore en Focus** — P1. Recom.: leer y sellar invariante "cero cloud cognitivo".
- **H4 Elite/IA Coach** — scope nuevo, no deuda.

---

## 6. Qué NO hice (honestidad de alcance)

- No ejecuté builds ni tests (el IDE posee el daemon; verificación estática).
- No abrí línea-a-línea Auth (`FirebaseSessionRepository`), el flujo de onboarding/evaluación inicial, ni
  todos los `*StateHolder` de sesión — apoyado en evidencia secundaria (reglas, KDoc, aprendizajes de
  `CLAUDE.md`). Marcado como "spot-check recomendado".
- La entrega inicial fue Fase A (auditoría) + Fase B (plan). Fase C arrancó tras las decisiones del dueño
  (§7); el resto de la Fase C sigue esperando el siguiente lote.

---

## 7. Fase C — ejecutado (2026-07-04)

**Decisiones del dueño:** (1) scoring → **preservar el modelo riguroso** (no se toca la matemática interna);
(2) persistencia → **añadir sync Firestore** (objetivo del siguiente arco, feature nueva sensible);
(3) arranque → **P1/P2 Endurecer + verificar**.

**Lote 1 ejecutado:**
- **H7 (resuelto):** `FocusRepositories.kt` es solo contratos (interfaces) `local-first`; *"raw trials never leave
  the device"*. **Sin fuga cloud hoy.** Además, `SessionHistoryRepository.recent(userId, limit)` ya exige
  lecturas acotadas *"mirroring the Firestore rules convention"* → el diseño ya anticipa un backend remoto: el
  **sync = añadir una implementación remota de estas interfaces**, no reescribir. Invariante revisado: el
  "cero-cloud cognitivo" deja de ser meta (el dueño eligió sync); pasa a ser consentimiento GDPR + reglas
  `uid==userId` + cobertura del eraser de borrado de cuenta al añadir la colección.
- **H8 (resuelto):** cobertura de composite/ceiling verificada como fuerte. **Añadido 1 test de regresión**
  `composite_averagesOnlyEstablished_excludesEmergingWithAValue` en `MindBoostProfileAggregatorTest` que fija el
  invariante del Global Score honesto (una dimensión EMERGING con valor NO entra en `compositeIndex`).

**Verificación pendiente (la ejecuta el dueño; el IDE posee el daemon):**
`:shared:desktopTest` (o el filtro de clase) + `:shared:detekt` + `:shared:mindboostCopySafetyScannerDesktopTest`.

**Siguiente arco recomendado:** nota de diseño del **sync Firestore** (colección, reglas, consentimiento GDPR,
eraser, e implementación remota de `FocusProfileRepository`/`SessionHistoryRepository`/`BaselineRepository`/
`ProbeBankRepository`) — a la espera de tu OK.

---

## 8. CEO decisions applied (2026-07-04)

Directiva ejecutiva del CEO recibida y sellada. Traducción a arquitectura (documentos asociados en
`docs/architecture/`):

- **Scoring — preservar el modelo riguroso.** Se conservan scorers bespoke por módulo, la máquina de estados
  `CALIBRATING/EMERGING/ESTABLISHED`, `null ≠ 0`, el `compositeIndex` solo sobre dimensiones ESTABLISHED, la
  exclusión de datos insuficientes y los gates anti-gaming. El PRD es **contrato de producto**, no orden de
  degradar. Capa de compatibilidad PRD→implementación en
  [`MINDBOOST_SCORING_MODEL_DECISION.md`](../architecture/MINDBOOST_SCORING_MODEL_DECISION.md).
- **Taxonomía — 8 motores de cara al usuario, 9 dimensiones internas.** Motores: Enfoque, Decisiones, Lógica,
  Creatividad, Lenguaje, Emociones, Velocidad mental, Memoria. Memoria = `WORKING_MEMORY` + `EPISODIC_MEMORY`
  (única relación 1:2). Mapper producto↔interno y riesgo de confusión en
  [`MINDBOOST_TAXONOMY_MAPPING.md`](../architecture/MINDBOOST_TAXONOMY_MAPPING.md).
- **Persistencia — añadir sync Firestore privacy-by-design.** Local-first se mantiene; guardado local
  inmediato; **sin subir raw trials** en MVP; solo métricas estructuradas; reglas estrictas
  (`auth.uid == userId`, App Check, lecturas acotadas, escrituras validadas, sin elevación de suscripción por
  cliente); consentimiento GDPR; integración con el eraser de borrado de cuenta; el sync nunca bloquea el
  entrenamiento. Diseño en
  [`MINDBOOST_FIRESTORE_SYNC_DESIGN.md`](../architecture/MINDBOOST_FIRESTORE_SYNC_DESIGN.md).
- **Elite / IA Coach — diferidos.** Orden: scoring+taxonomía sellados → sync diseñado → sync mínimo seguro →
  Elite → IA Coach. El diseño de sync se prepara **compatible** con IA Coach futura (resúmenes estructurados,
  nunca raw trials), sin construirla.
- **Namespace — migración por fases, no destructiva.** Deuda real (`com.mindos.app` fuente de verdad vs
  `com.mindverse.mindboost` legacy vs `com.example.mindos` histórico). Plan por lotes en
  [`MINDBOOST_NAMESPACE_MIGRATION_PLAN.md`](../architecture/MINDBOOST_NAMESPACE_MIGRATION_PLAN.md). Nada
  migrado este turno.

**Restricciones del turno (cumplidas):** solo documentación; sin reemplazar scoring, sin subir raw trials, sin
implementar IA/Elite, sin migrar namespaces, sin cambiar archivos de reglas Firestore, sin commits.
