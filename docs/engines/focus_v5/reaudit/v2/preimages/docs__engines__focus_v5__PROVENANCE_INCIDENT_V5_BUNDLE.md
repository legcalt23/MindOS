# PROVENANCE_INCIDENT_V5_BUNDLE — sello del bundle Focus V5 vs árbol real

```
INCIDENT: FOCUS-V5-BUNDLE-SEAL-BIRTH-DEFECT
CUSTODY_DECISION: D (procedencia versionada + cuarentena del legado)
GENERATED_BY: TASK-2026-07-22-FOCUS-V5-S5-R3-PASO7 (doc-only, READ-ONLY sobre el bundle)
MEASURED_AT: HEAD 8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5 · rama product/focus2-v5
```

> **Qué es.** Registro forense del estado de procedencia del bundle `FOCUS_ENGINE_MASTER_PLAN_V5`
> (sellado en `SHA256_MANIFEST_V5.txt`, commit `15cb12a`). Todos los hashes son de **bytes crudos**
> (no git-blob-id) y fueron medidos en modo lectura, **sin ejecutar `tools/freeze_v5.cjs`** (ESCRIBE el
> manifest — prohibido en el dir real). El manifest legacy se conserva intacto (custodia D).

---

## 1. Timeline

1. `15cb12a` («feat(focus): Focus V5 internal tracers S1-S4 + iOS CI gate») introduce
   `SHA256_MANIFEST_V5.txt` (16 entradas, self-excluding).
2. **En el mismo commit** el bundle ya nace parcialmente incoherente: 2 de los 16 archivos sellados no
   corresponden a ningún byte commiteado (§2).
3. Paso 2 (R1) modifica `ADR_REGISTER_V5.md` en el worktree (añade la fila del ADR temporal) → su hash vivo
   diverge del sello, pero de forma **legítima** (el HEAD-committed sigue == sello; §3).
4. Paso 7 (esta tarea) mide la coherencia 16/16 y registra la disposición sin tocar el manifest.

## 2. Verificación 16/16 (bytes crudos, worktree vs sello del manifest)

**Resultado: 13 OK · 3 MISMATCH.** Los 3 mismatch se clasifican por causa (no se colapsan):

| # | Archivo | Sello (16hex/bytes) | Vivo (16hex/bytes) | Clase |
|---|---|---|---|---|
| A | `GATE_CATALOG_V5.md` | `02090ca6…` / 9249 | `8c4adeec…` / 10485 | **BIRTH DEFECT** (sello no reproducible) |
| B | `EXERCISE_PORTFOLIO_V5.md` | `dba3d15c…` / 16693 | `cc1f684e…` / 21775 | **BIRTH DEFECT** (sello no reproducible) |
| C | `ADR_REGISTER_V5.md` | `d77a9df9…` / 17142 | `8320d28f…` / 17584 | **EDICIÓN WORKTREE LEGÍTIMA** (no defecto) |

Los 13 fieles: `FOCUS_ENGINE_MASTER_PLAN_V5.md` (`a0bada21…`), `BASELINE_EVIDENCE_V5.md`,
`REDTEAM_TEST_MATRIX_V5.md`, `REQUIREMENTS_MATRIX_V5.csv`, `REVIEWS_V5.md`, los 6 `agent_outputs/*`,
`tools/freeze_v5.cjs`, e **`INPUTS_SHA256.txt`** (worktree == sello; §4).

## 3. Clase C — `ADR_REGISTER_V5.md` NO es un defecto

`git log --all -- ADR_REGISTER_V5.md` = **1 commit** (`15cb12a`); los bytes sellados se recuperan de esa
revisión (HEAD-committed == sello). El `git status` lo marca ` M` (worktree modificado): la divergencia es
la **edición sin commitear de Paso 2** (fila del ADR temporal), no una corrupción del sello. El register es
un documento VIVO indexado; Paso 7 lo edita de nuevo (bajo allowlist). Su sello se refrescará al versionar
el register en el cierre real de S5.

## 4. `INPUTS_SHA256.txt` — recuperable byte-a-byte (EV-R42-03, corrige rondas previas)

El **worktree** de `INPUTS_SHA256.txt` = `8f5f060988f8649fee01d3f4a48ae87d1829dbb220e257fd4eeb5b34dbc6c4a0`
(2220 B, CRLF) = **exactamente el valor sellado**. Solo la versión **commiteada en HEAD** difiere. Por tanto
el ancla de inputs es **fiel en el worktree** (recuperable) e inválida solo en HEAD hasta que la custodia D
la versione. **Corrección explícita:** rondas previas (R4/R4.1) contaron 3 birth-defects
`NOT_FOUND_IN_LOCAL_GIT_OBJECT_DATABASE` incluyendo INPUTS; la medición de Paso 7 muestra que INPUTS es
recuperable ⇒ **el conjunto real de birth-defects irrecuperables es 2 (GATE_CATALOG, PORTFOLIO), no 3**.

## 5. Forense de los 2 birth-defects (A, B)

Para GATE_CATALOG (sello 9249 B) y PORTFOLIO (sello 16693 B): `git log --all` sobre cada path = 1 commit
(`15cb12a`); el blob commiteado ≠ sello; ninguna revisión histórica del path reproduce los bytes sellados.
Alcance del "no encontrado": **`NOT_FOUND_IN_LOCAL_GIT_OBJECT_DATABASE`**. NO revisados (fuera de esta
máquina): CI, remotos no-fetched, backups, otras copias = `NOT_CHECKED`. Los blobs vivos en HEAD son mayores
que los sellados (10485>9249; 21775>16693), consistente con que el manifest congeló una versión **anterior**
(o de otra rama) que nunca aterrizó en git.

- **Causa raíz = `UNKNOWN`.** No se afirma un mecanismo.
- **`HYPOTHESIS` (separada del UNKNOWN):** el manifest se generó sobre un working-tree con esos 2 archivos en
  un estado que se editó DESPUÉS pero ANTES del commit `15cb12a` (edición post-generación pre-commit). Es una
  hipótesis, no la causa.

## 6. Disposición (custodia D)

- `LEGACY_MANIFEST_PRESERVED = YES` (no se edita `SHA256_MANIFEST_V5.txt` para "arreglar" el sello).
- `LEGACY_MANIFEST_VALIDATES_TREE = NO` (medido: 3/16 divergen; 2 irrecuperables).
- Los sellos de GATE_CATALOG y PORTFOLIO se marcan `VOID_PROVENANCE` en `CURRENT_AUTHORITY_INDEX.md §4.1`;
  su **contenido vivo** sigue `VIGENTE` (los archivos vivos son la autoridad, por el §0 del propio catálogo).
- El re-sellado fiel del bundle (regenerar el manifest desde el árbol vivo) es trabajo del **cierre real de
  S5**, con autorización separada; NO se hace en Paso 7 (que es doc-only y no ejecuta `freeze_v5.cjs`).

## 7. Etiquetas de evidencia

`VERIFIED_PRIMARY` para la verificación 16/16 y las clases A/B/C/INPUTS (medición directa, esta tarea,
capturada en `S5_R3_EVIDENCE_CAPSULE.json`). `NOT_CHECKED` para CI/remoto/backups. `UNKNOWN` para la causa
raíz; `HYPOTHESIS` para la edición post-generación pre-commit. `NOT_FOUND_IN_LOCAL_GIT_OBJECT_DATABASE` para
los bytes sellados de GATE_CATALOG y PORTFOLIO.
