# CURRENT_AUTHORITY_INDEX — Focus Engine V5 (índice de autoridad vigente)

```
AUTHORITY_INDEX_ROLE: CURRENT
FOCUS_V5_CURRENT_AUTHORITY_INDEX: docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md
GENERATED_BY: TASK-2026-07-22-FOCUS-V5-S5-R3-PASO7 (doc-only; NO ejecuta implementación; NO congela; NO monta)
CUSTODY: D (procedencia versionada + cuarentena del legado) · DISCOVERABILITY: ADR_REGISTER + CLAUDE.md + scanner
REVIEW_CLASS: AI_ASSISTED_INDEPENDENT_REVIEW (la falsación R2 y la reaudit 2026-07-22 las hizo Claude; EXTERNAL_AUDIT humano = NOT_OBTAINED)
PACKAGE: UNTRACKED (working-tree; NO en HEAD 8a06b9a) — integridad por reaudit/S5_R3_EXTERNAL_MANIFEST (SHA-256 completo), no por checkout
REAUDIT 2026-07-22: reaudit/S5_R3_REAUDIT_MASTER_REPORT.md + S5_R3_MULTIDIMENSIONAL_VERDICTS.md (5/7 lentes FAIL; R3 sigue CANDIDATA)
```

> **Qué es.** El único índice de "qué artefacto manda hoy" para el motor Focus V5. Los artefactos legacy
> del bundle V5 (sellados en `SHA256_MANIFEST_V5.txt` en el commit `15cb12a`) se leen **SOLO a través de
> este índice**: 2 de ellos tienen el **sello de procedencia inválido** (bytes sellados no reproducibles
> desde git — birth defect), y este índice enumera su disposición. El scanner
> `scripts/validate-authority-entrypoints.mjs` verifica que los entrypoints obligatorios (`CLAUDE.md`,
> `ADR_REGISTER_V5.md`) referencian este índice. **Este archivo es el ÚNICO** en `docs/engines/focus_v5/*.md`
> que declara `AUTHORITY_INDEX_ROLE: CURRENT`.

---

## 1. Estado del motor (2026-07-22)

`ADR_STATUS(ADR-FOCUS-TEMPORAL-QUALITY) = REOPENED_AFTER_EXTERNAL_AUDIT · R3 CANDIDATA` (la R2 quedó FALSADA,
ver §3 y `S5_REOPEN_STATUS.md §0`). `MOUNT = BLOCKED` (focus2 fuera del paquete; `focus2Enabled` no existe).
`APPROVAL_PENDING` (autoridad que promueve CANDIDATA→aprobada = CEO / revisor externo humano si el CEO lo dispone;
`EXTERNAL_HUMAN_AUDIT = NOT_OBTAINED`). `EXECUTION_AUTHORIZATION = GRANTED (scope plan V5, T-DR por slice)`. Techo
honesto = **R3 CANDIDATA**, NO `FROZEN_FOR_S5_R3`. La reaudit 2026-07-22 devolvió **5/7 lentes FAIL** (psicometría,
arquitectura, privacidad, provenance, a11y) — la R3 NO se promueve.

## 2. Autoridad por artefacto

| Artefacto | Rol | Estado / disposición |
|---|---|---|
| `ADR_FOCUS_TEMPORAL_QUALITY.md` (untracked) | contrato de diseño de calidad temporal (S5) | **VIGENTE** como contrato R3; `REOPENED_AFTER_EXTERNAL_AUDIT`; umbrales = config sombra `DESIGNED_NOT_RUN`; se versiona al cierre real de S5, no ahora |
| `S5_REOPEN_STATUS.md` (untracked) | artefacto vivo de la reapertura S5 (baseline + progreso) | **VIGENTE**; supersede operativamente a `S5_EXECUTION_PROPOSAL.md` |
| `ADR_REGISTER_V5.md` (tracked, worktree-modificado) | índice de ADRs del motor | **VIGENTE**; fila temporal = `REOPENED_AFTER_EXTERNAL_AUDIT / R3 CANDIDATA`; contiene el marcador de este índice |
| `GATE_CATALOG_V5.md` (tracked) | autoridad única del vocabulario de gates (V0–V8, T-*) + readiness 5 campos | **CONTENIDO VIGENTE** (el archivo vivo es la autoridad, por su propio §0); **SELLO del manifest = `VOID_PROVENANCE`** (ver §4) |
| `FOCUS_ENGINE_MASTER_PLAN_V5.md` (tracked) | plan maestro del motor | **VIGENTE**; sello del manifest FIEL (`a0bada21…`); NO es entrypoint del índice (permanece en denylist del Paso 7) |
| `EXERCISE_PORTFOLIO_V5.md` (tracked) | portfolio de 18 ejercicios | **CONTENIDO VIGENTE** (vivo); **SELLO del manifest = `VOID_PROVENANCE`** (ver §4) |
| `INPUTS_SHA256.txt` (tracked) | ancla de inputs del bundle | worktree == sello (recuperable byte-a-byte, EV-R42-03); la versión commiteada en HEAD difiere ⇒ el ancla es fiel en el worktree, no en HEAD |
| `MINDBOOST_AUDIT_LEDGER.md` (tracked) | ledger de auditoría acumulativo | **VIGENTE**; sellado self-excluding §0; findings del motor = `MB-FOCUS-####` |
| `S5_R3_EVIDENCE_CAPSULE.md`/`.json` (nuevo) | cápsula de evidencia inmutable de la R3 | **VIGENTE** como EVIDENCIA (no canon paralelo; las transiciones viven en el ledger) |
| `PROVENANCE_INCIDENT_V5_BUNDLE.md` (nuevo) | incidente de procedencia del bundle | **VIGENTE**; registra los 2 birth-defects + custodia D |
| `S5_EXECUTION_PROPOSAL.md` (untracked) | propuesta previa | **SUPERSEDED** por `S5_REOPEN_STATUS.md` (conservada como evidencia) |
| `S5_R4_PASO7_PLAN_REVIEWED.md` · `S5_R4_1_*` · `S5_R4_2_*` · `S5_R3_PASO7_PLAN.md` | planes/auditorías del Paso 7 | **HISTÓRICO / proceso**; no son autoridad de contenido del motor |

## 3. Falsación R2→R3 (por qué el ADR está reabierto)

El gate de elegibilidad temporal R2 §6.1 (`S_full`-o-`Absent` con `monotonicGuard mg-1`) es **no-monótono**
bajo el scorer real: más exclusión técnica abre el gate que menos exclusión cerraba (`Absent → Present`).
Contraejemplo reproducido (30H/10M/10FA/30CR; valores del scorer real 6 dp, triple-verificados; D1 excluye 5
misses → d′ 1.680016 → Absent; D2 excluye 5 misses + 15 hits, exclusión 25% < 50% backstop → d′ 1.292908 ≤
`S_full` 1.310847 → Present). `CI-lower(S_full)>0` hace
la elegibilidad dependiente del rendimiento (sesgo de selección longitudinal). **Mecanismo retirado como
gate de elegibilidad** (D-R3-01: elegibilidad OUTCOME-INDEPENDENT). Detalle: `S5_REOPEN_STATUS.md §0.1`,
finding `MB-FOCUS-0047`.

## 4. Disposición fila-a-fila del `GATE_CATALOG_V5.md` (eje RELEASE lee SOLO `VIGENTE`)

El **contenido vivo** de `GATE_CATALOG_V5.md` es la autoridad única del vocabulario de gates (su propio §0).
El eje `RELEASE` de la state machine lee los estados de gate del archivo VIVO, NO del sello del manifest. El
**sello del manifest** de este archivo es `VOID_PROVENANCE` (§4.1): un defecto de la instantánea congelada,
no del vocabulario vivo.

| Gate (live) | Disposición | Estado de gate (evidencia: ledger checkpoints) |
|---|---|---|
| `T-PLAN` · `T-DR` | VIGENTE | PASS (bundle) / GRANTED por-slice hasta S4; **S5 espera su propio `T-DR`** |
| `T-BASE` | VIGENTE | PASS (R0: desktopTest 3760/3760, detekt 0) |
| `T-S0` · `T-S1` · `T-ADV` · `T-S3` · `T-S4` | VIGENTE | PASS (INTERNAL_TECHNICAL_TRACER ×2 + ENGINE_GENERALIZED en S4) |
| `T-S5` (V1 técnico + paridad) | VIGENTE | **NOT MET / EN REAPERTURA** (R3 candidata; iOS `BLOCKED_IOS`; físico `NOT_RUN`) |
| `T-S7` · `T-S8` · `T-SCI-n` · `T-REL` | VIGENTE | `NOT_RUN` (slices futuras) |
| `V0`–`V8` (científicos) | VIGENTE | `DESIGNED_NOT_RUN` (ninguna evidencia humana; ningún claim > C0) |

### 4.1 Sellos `VOID_PROVENANCE` (birth defect medido, ver `PROVENANCE_INCIDENT_V5_BUNDLE.md`)

- `GATE_CATALOG_V5.md`: sello `02090ca6…` / 9249 B ≠ vivo `8c4adeec…` / 10485 B; bytes sellados
  `NOT_FOUND_IN_LOCAL_GIT_OBJECT_DATABASE`.
- `EXERCISE_PORTFOLIO_V5.md`: sello `dba3d15c…` / 16693 B ≠ vivo `cc1f684e…` / 21775 B; bytes sellados
  `NOT_FOUND_IN_LOCAL_GIT_OBJECT_DATABASE`.

Ambos nacieron rotos en el commit `15cb12a` (el manifest selló bytes que nunca se commitearon). CI / remotos
no-fetched / backups / otras máquinas = `NOT_CHECKED`. El manifest legacy se **CONSERVA** intacto (custodia D:
no se edita para "arreglar" el sello); su reemplazo por un sello fiel es trabajo del cierre real de S5, con
autorización separada.

### 4.2 Residuo conocido — frontmatter stale de `GATE_CATALOG_V5.md` (reaudit Paso 7)

`GATE_CATALOG_V5.md` línea 9 dice «Estado de TODOS los gates hoy: DESIGNED_NOT_RUN», heredado del snapshot
congelado `BASE_SNAPSHOT=27849450` (anterior a S0–S4). Esta línea **INFRA-declara** (contradice los PASS de
T-BASE/T-S0/T-S1/T-ADV/T-S3/T-S4 registrados en el ledger) — dirección OPUESTA a un claim inflado, por lo que el
barrido T5 (que busca inflación) no la captura. **El estado LIVE de cada gate se lee en §4 de este índice**
(re-derivado del ledger), NO del frontmatter congelado del catálogo. El catálogo NO se edita (custodia D); residuo
registrado también en `S5_REOPEN_STATUS §7` y `S5_R3_EVIDENCE_CAPSULE §6`. **Cuarentena reforzada (reaudit):**
`scripts/validate-authority-entrypoints.mjs` ahora FALLA si `GATE_CATALOG_V5.md` adquiere un marcador `CURRENT` o
si esta disposición (§4/§4.1/§4.2) desaparece del índice — así el catálogo congelado no puede volver a pasar por
autoridad vigente sin romper el gate.

## 5. Nota de clase de revisión y límites del scanner (reaudit 2026-07-22)

- **Clase de revisión.** Todo lo etiquetado «auditoría»/«external audit» en este motor hasta hoy es
  `AI_ASSISTED_INDEPENDENT_REVIEW` (ejecutado por Claude). **Ningún `EXTERNAL_AUDIT` humano/institucional ha
  ocurrido** (`NOT_OBTAINED`). El token de estado del ADR `REOPENED_AFTER_EXTERNAL_AUDIT` se conserva como
  historia; léase como `REOPENED_AFTER_INDEPENDENT_REVIEW`.
- **Qué NO hace el scanner.** `validate-authority-entrypoints.mjs` verifica *discoverability* (marcadores
  presentes, un único rol `CURRENT`, sin enmascaramiento del legado, cuarentena del catálogo) — NO valida el
  CONTENIDO de la cadena de autoridad ni fuerza el orden de lectura de los ficheros `VOID_PROVENANCE`. Es defensa
  de descubrimiento, no de corrección semántica.
- **Provenance.** Este índice y todo el paquete R3 están UNTRACKED (no en HEAD 8a06b9a). La integridad se ancla
  en `reaudit/S5_R3_EXTERNAL_MANIFEST` (SHA-256 completo por miembro), no en un checkout. Commitear el paquete es
  una decisión CEO pendiente.
