# S5_R4_2_AUDIT_ANNEX — adjudicaciones, retracción y evidencia (miembro NO normativo del bundle R4.2)

> Este annex preserva historia, adjudicaciones y outputs crudos. Ejecutar el Paso 7 NO requiere leerlo:
> todo lo normativo vive en `S5_R4_2_PASO7_EXECUTION_PLAN.md`. Los bloques de §A6 están escritos por
> máquina (re-medición directa), no transcritos a mano.

---

## A1. Resolución de R41-P0-01 — RETRACTION del conteo «449 líneas» (causa raíz instrumental)

**Veredicto: CONFIRMED — la contradicción era real y el número era mío.** La cifra «449 líneas» que R4.1
(§1, tabla) y mis reportes atribuyeron a la R4 original era FALSA. Medición sobre bytes crudos
(EV-R42-01): la R4 original (`829537af8c79394d…`, 48 448 B) tiene **LF_COUNT = 532** y último byte
`0x0A` ⇒ **532 líneas lógicas**. El prefijo de 45 056 B tiene **482 LF** y termina en `0x75` (`u` de
«fu») ⇒ 483 líneas lógicas del adjunto. **483 ≤ 532: la relación de prefijo es consistente** y sigue
PROBADA criptográficamente: `sha256(bytes[0..45055]) = 5c1463a3f29581873b51c13ea9cab7ebfed913b7a2067b0367966e5a4bc0a15c`
== hash del adjunto del auditor.

**Causa raíz demostrada (no conjeturada):** `Measure-Object -Line` de PowerShell **ignora las líneas en
blanco**. Re-ejecutado sobre el MISMO archivo (EV-R42-01): `NONBLANK_LINES = 449` y
`MEASURE_OBJECT_LINE = 449` — exactamente la cifra retractada. Reporté un conteo de líneas no-vacías
como conteo de líneas. Mismo mecanismo en mi «464 líneas» de R4.1: era `SPLIT_LF_ELEMENTS` (split por LF
produce un elemento vacío final); el valor correcto es **463** (EV-R42-02), que coincide con la medición
del auditor (463/463).

**Acciones:** (1) RETRACTION formal: R4 = 532 líneas lógicas, no 449; R4.1 = 463, no 464. (2) Convención
normativa única en el plan §13 (`LF_COUNT` / `LOGICAL_LINES`) + prohibición instrumental de
`Measure-Object -Line` y `Get-Content -Tail`. (3) R4 queda utilizable SOLO como evidencia histórica no
normativa (§A4) — condición que R41-P0-02 impone de todos modos.

---

## A2. Adjudicación de los 12 P0 residuales (R41-P0-01..12)

| ID | Veredicto | Corrección → plan |
|---|---|---|
| 01 contradicción prefijo | **CONFIRMED** (cifra falsa mía; prefijo intacto) | RETRACTION §A1; convención §13 |
| 02 no autocontenida / dependencia R4 sin ligar | **CONFIRMED** | Bundle manifest + binding en el mandato (§1); plan autocontenido; R3/R4/R4.1 demovidas a histórico (§A4) |
| 03 lock por RUN_ID no excluye runs | **CONFIRMED** | Lock COMPARTIDO por repo (`${OPS_ROOT}/locks/<repo_key>.lock`, `repo_key=1c6b17edc619597d`) con run_id/host/PID/process-start dentro (§2.3) |
| 04 autorización omite D-P7-B + placeholder en allowlist | **CONFIRMED** | `DISCOVERABILITY` DENTRO del mandato; allowlist 13 paths literales, cero placeholders; Master Plan solo denylist (§1, §4.1) |
| 05 `COMPLETED` antes de handoff/packages | **CONFIRMED** | Estados intermedios + `RUN_CLOSURE_ATTESTATION`; `COMPLETED` es el penúltimo estado (antes solo de `DELIVERED`) (§10–§12) |
| 06 Fase B sin rollback/recovery | **CONFIRMED** | Opción preferida adoptada: handoff Y paquetes construidos/verificados desde staging ANTES de promover; handoff = target promovido; `PROMOTED_PENDING_CLOSURE` recuperable (§5.12, §9, §12) |
| 07 rollback destruye cambio concurrente en REPLACE/HUNK | **CONFIRMED** | Guard `current_sha == promoted_sha` para TODO target (no solo CREATE), orden inverso, `PARTIAL_RECOVERY_REQUIRED` (§9) |
| 08 evidencia sin destino durable | **CONFIRMED** | `INTERNAL_EVIDENCE_BUNDLE` durable con ubicación, retención por clase, root hash y control de acceso (§2.2, §7.2) |
| 09 handoff referencia journal excluido | **CONFIRMED** | Handoff referencia el `PROMOTION_PLAN` (incluido en el reproduction bundle, acíclico); journal privado; su hash final vive en la attestation (§5.12, §7.3, §11) |
| 10 diff sin pathspec re-incluye ajenos | **CONFIRMED** | `git diff --binary --no-ext-diff HEAD -- <miembros>` con pathspec DESDE la membership congelada + gate negativo cero-paths-fuera + `FOREIGN ∩ = ∅` (§7.3) |
| 11 estado ADR viola sus entry criteria | **CONFIRMED** | Rectificación: hoy `ADR_STATUS = DRAFT_PENDING_REAUDIT`; `CANDIDATE` solo tras reaudit ligado al hash final (§10) — R4.1 §9 declaraba CANDIDATE sin cumplirlos |
| 12 sin verificación end-to-end de entrega | **CONFIRMED** | Handshake bilateral de 4 valores (`sha256, bytes, LF_COUNT, last_byte`); `DELIVERY_INTEGRITY=FAIL` posible con origen completo; `DELIVERED` solo con PASS (§11.3) |

## A3. Adjudicación de los 10 P1

| # | Veredicto | Corrección → plan |
|---|---|---|
| 1 pre-state solo-status | CONFIRMED | `HEAD tree + dirty overlay (sha por archivo) + target/denylist map` + algoritmo post-state (§3) |
| 2 scanner abstracto | CONFIRMED | Paths literales, marcador máquina exacto (clasificador de citas históricas = solo-marcador), oráculo `--self-test` con fixtures embebidos (§5.5) |
| 3 «§17 confirma con output real» era falso | CONFIRMED | R4.1 §16 citaba §17, que contiene decisiones CEO, no outputs; el output vivía en el chat. Corregido: evidencia por EVIDENCE_ID con bloques crudos preservados (§13; §A6) |
| 4 RELEASE colgado del catálogo legacy | CONFIRMED | El índice enumera disposición fila-a-fila del GATE_CATALOG (`VIGENTE`/`SUPERSEDED_BY`/`VOID_PROVENANCE`, ligada a su hash); RELEASE solo lee `VIGENTE` (§5.1, §10) |
| 5 registro run-ids indefinido | CONFIRMED | Path, append-only, escritura solo-bajo-lock (concurrencia), cross-check con journal en recovery (§2.4) |
| 6 retención uniforme de `${OPS}` | CONFIRMED | Retención por clase: backups vs evidence/packages vs journal/closure/registry (§2.2) |
| 7 comparadores efímeros | CONFIRMED | Copias hasheadas de validador/scanner/medidor DENTRO del evidence bundle (§7.2) |
| 8 reaudit sin scopes | CONFIRMED | `REAUDIT=FULL_SCOPED`: psicometría, eficacia/claims, timing/KMP, seguridad, privacidad, a11y/fairness + integrador (§6) |
| 9 semántica replace sin especificar | CONFIRMED | `File.Replace`/write-through en Windows (fsync de directorio NO disponible — limitación declarada, journaling NTFS), `rename(2)+fsync(dir)` en POSIX; modo 100644; sin symlinks; NFC; EOL por miembro (§8.2–§8.3) |
| 10 literal de autorización reutilizado | CONFIRMED | `EJECUTAR_PASO7_R4_2` con `PLAN_BUNDLE_ROOT/CUSTODY/DISCOVERABILITY/REAUDIT/RUN_ID` (§1) |

---

## A4. Dependencias históricas demovidas + sustancia conservada (autocontención)

**Histórico NO normativo** (se citan por hash; ejecutar no exige leerlas):
`S5_R3_PASO7_PLAN_REVIEWED.md` en scratchpad (`25dfe9ff59124630…`, sobrevivió al reinicio) ·
`S5_R4_PASO7_PLAN_REVIEWED.md` (`829537af8c79394d…`, 48 448 B, 532 líneas) ·
`S5_R4_1_PASO7_PLAN_REVIEWED.md` (`b462948a91cba6a7…`, 36 953 B, 463 líneas). Los 64-hex completos
constan en EV-R42-01/02 y en el campo `historical_non_normative` del manifest. La adjudicación de las
64 hipótesis de R4 (61 CONFIRMED / 2 PARTIAL / 1 UNKNOWN) NO es necesaria para ejecutar: sus
correcciones están absorbidas por el diseño del plan.

**Sustancia re-declarada (lo que la ejecución SÍ usa):**

- **S-01 · bundle roto de nacimiento (3/16, único commit `15cb12a`):** `GATE_CATALOG_V5.md` sellado
  `02090ca6…`/9249 B vs HEAD `8c4adeec…`/10 485 B · `EXERCISE_PORTFOLIO_V5.md` sellado `dba3d15c…`/16 693 B
  vs HEAD `cc1f684e…` · `INPUTS_SHA256.txt` sellado `8f5f0609…`/2220 B vs HEAD `8d5a0fda…`. Forense local:
  bytes sellados de GATE_CATALOG y PORTFOLIO = `NOT_FOUND_IN_LOCAL_GIT_OBJECT_DATABASE` (CI/remotos/
  backups = `NOT_CHECKED`).
- **HALLAZGO NUEVO (esta ronda, EV-R42-03): los bytes sellados de `INPUTS_SHA256.txt` EXISTEN en el
  worktree**, sin commitear: sha256 completo del worktree =
  `8f5f060988f8649fee01d3f4a48ae87d1829dbb220e257fd4eeb5b34dbc6c4a0` (2220 B) == la línea del manifest
  sellado, verificado a 64 hex. Refina S-01: **1/3 recuperable byte-a-byte**; el ancla de inputs sigue
  inválida EN GIT (HEAD difiere) hasta la custodia D. Entra al `PROVENANCE_INCIDENT` (plan §5.2).
- **S-03:** consecuencia de S-01 sobre el ancla de inputs — matizada por el hallazgo anterior.
- **S-04:** focus2 sin feature-flag ⇒ flag/kill-switch = precondición NUEVA de S6 (fuera del alcance P7).
- **S-05:** mandato duplicado sin rastro en repo = `UNKNOWN` (reporte CEO aceptado).
- **Custodia D:** procedencia versionada + cuarentena del legado; los legacy NO se editan; la
  disposición vive en el índice (plan §5.1–§5.2).
- **CEO assurance (18 dominios, R4 §12):** sin cambio material; se reconstruye como proyección del
  capsule en ejecución. Correcciones de fuerza ya adoptadas: #17 observabilidad-sin-PII =
  `STATIC_SEARCH_ONLY` → runtime audit PRE_RELEASE; #2/#3/#5/#6 = `DOCUMENTED` ≠ `VALIDATED`; #14
  flag/kill-switch = `PRE_MOUNT` (S-04).
- **Estados hoy (2026-07-22):** `ADR_STATUS=DRAFT_PENDING_REAUDIT` (rectificado, §A2-11) ·
  `INTERNAL_AUDIT=HISTORICAL_INTERNAL_AUDIT_UNBOUND` · `MOUNT=BLOCKED` · `EXTERNAL_AUDIT=PENDING×5` ·
  `VALIDATION` mayormente `NOT_RUN`(físico)/`STATIC`(plataforma)/`UNIT_SIM`(dominio) · `EXECUTION=n/a`.

---

## A5. Self-red-team de la R4.2 (12 ítems; los 6 exigidos, primero)

1. **Deadlock del lock compartido** (proceso muerto deja el repo bloqueado para siempre): stale =
   PID inexistente O `(pid, pid_start)` sin proceso vivo; ruptura SOLO con `BREAK_STALE_LOCK=YES` del
   CEO registrada; el lock vive fuera de `${RUN}` y se libera tanto en closure como en rollback.
2. **TOCTOU staging→promoción:** la precondition se re-verifica INMEDIATAMENTE antes del replace de
   CADA archivo, no solo una vez global; ventana residual (writers que no honran nuestro lock — el
   árbol es compartido por diseño) declarada y cubierta por post-validation + guard de rollback.
3. **Rollback concurrente:** restaurar solo si `current_sha == promoted_sha` de ESTE run — para CREATE
   Y REPLACE/HUNK; orden inverso; si no coincide, `PARTIAL_RECOVERY_REQUIRED` con lista exacta, jamás
   sobrescribir trabajo ajeno.
4. **Package leakage:** fingerprint y evidence bundle NUNCA se exportan completos; patch path-limited
   desde membership congelada + gate negativo (cero paths fuera; `FOREIGN ∩ = ∅`); external =
   allowlist-extraction + secret/PII scan + revisión humana.
5. **Cierre prematuro:** `COMPLETED` es inalcanzable antes de `CLOSURE_ATTESTED` (orden total §10);
   crash post-promoción ⇒ `PROMOTED_PENDING_CLOSURE` recuperable; post-validation registrada es
   precondición de la attestation.
6. **Truncación en transferencia:** handshake bilateral de 4 valores por artefacto; el fallo es
   `DELIVERY_INTEGRITY=FAIL` aunque el origen esté completo; esta MISMA entrega R4.2 usa el protocolo
   (reporte out-of-band → verificación del receptor).
7. **Manifest manipulado** (autorizaría otro bundle): el mandato liga `PLAN_BUNDLE_ROOT` = sha256 de
   bytes exactos; R42-0 verifica manifest Y cada miembro contra él; mismatch ⇒ `BLOCKED_DECISION`.
8. **Shadow-validación de bytes distintos a los promovidos:** el shadow se copia DE staging ya sellado;
   `promoted_sha == staged_sha` se verifica por archivo; el validador re-corre sobre el VIVO en
   post-validation.
9. **Membership congelada en el momento equivocado:** se congela en R42-1 (pre-writes); los
   deliverables nuevos entran por ALLOWLIST (targets explícitos), no por glob de membership; los
   paquetes solo leen staging + membership.
10. **Reaudit citado sobre hash viejo tras una corrección:** regla dura — cualquier cambio en staging
    repite el reaudit COMPLETO sobre los hashes nuevos; el capsule se congela DESPUÉS (R42-3).
11. **`${OPS_ROOT}` borrado por limpieza:** no es `%TEMP%` (sin auto-clean); retención por clase
    documentada; riesgo residual = borrado manual humano — mitigado porque la aceptación externa
    registra los roots fuera de esta máquina (packet + attestation reportada).
12. **Recurrencia del «449» con otro instrumento:** convención única de conteo (plan §13),
    prohibiciones instrumentales explícitas (`Measure-Object -Line`, `Get-Content -Tail`), y los
    contraejemplos numéricos del capsule exigen DOS cálculos independientes por EVIDENCE_ID.

---

## A6. Evidencias crudas (escritas por máquina, verbatim)

Comando de medición (PS 5.1, .NET puro; sin `Get-Content`): lee bytes crudos, sha256, cuenta `0x0A`,
inspecciona último byte, y computa además los valores-trampa (`NONBLANK_LINES`, `MEASURE_OBJECT_LINE`)
que demuestran la causa raíz de §A1. Los bloques siguientes los appendea el propio medidor:

#### EV-R42-01 · R4 original + prefijo 45056 (base de la RETRACTION §A1)

```
PATH=docs/engines/focus_v5/S5_R4_PASO7_PLAN_REVIEWED.md
BYTES=48448
SHA256=829537af8c79394d94813a04f654264acb4604a6b8963deb3d7e9f2580a778e4
LF_COUNT=532
CR_COUNT=0
LOGICAL_LINES=532
LAST_BYTE=0x0A
NONBLANK_LINES=449
MEASURE_OBJECT_LINE=449
PREFIX_LEN=45056
PREFIX_SHA256=5c1463a3f29581873b51c13ea9cab7ebfed913b7a2067b0367966e5a4bc0a15c
PREFIX_LF_COUNT=482
PREFIX_LAST_BYTE=0x75
```

#### EV-R42-02 · R4.1 (confirma 463; mi «464» era SPLIT con elemento vacío final)

```
PATH=docs/engines/focus_v5/S5_R4_1_PASO7_PLAN_REVIEWED.md
BYTES=36953
SHA256=b462948a91cba6a7dfc0a4f699db16ac0b33ca0e91f1d48cd27578c876cdae4a
LF_COUNT=463
CR_COUNT=0
LOGICAL_LINES=463
LAST_BYTE=0x0A
NONBLANK_LINES=375
MEASURE_OBJECT_LINE=375
```

#### EV-R42-03 · INPUTS_SHA256.txt worktree == bytes sellados (64 hex) + repo_key del lock

```
INPUTS_WORKTREE_SHA256=8f5f060988f8649fee01d3f4a48ae87d1829dbb220e257fd4eeb5b34dbc6c4a0
SEALED_MANIFEST_LINE=8f5f060988f8649fee01d3f4a48ae87d1829dbb220e257fd4eeb5b34dbc6c4a0      2220  CRLF   INPUTS_SHA256.txt
MATCH_64HEX=True
CANONICAL_REPO_PATH=c:\users\curro\androidstudioprojects\mindos
REPO_KEY=1c6b17edc619597d
```

#### EV-R42-04 · Referencia allowlist/denylist + git (2026-07-22, base de §3.2 del plan)

```
BRANCH=product/focus2-v5
HEAD=8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5
HEAD_TREE=1598aecdc990a5ecb7d082d4149f7b75188ecba7
STASH_COUNT=0
05_TECH  = cca93e829d0632b5... (30042 B)
09_DEC   = 5c9d2005a23fc760... (86018 B)
STATUS   = 502f8ff8b94069bb... (49557 B)
REGISTER = 8320d28f73a28813... (17584 B)
ADR_TQ   = d6941396ac941db2... (93870 B)
CLAUDE   = df385a054111b15a... (333296 B)
LEDGER   = 265e4b7eb1fffcd4... (353989 B)
LEDGER_SEAL_LINE=Canonical SHA-256 (self-excluding — see §8 convention): `8526b509c3ec086d393d895cb4ba742d6b8fc415d760c1f39fd757859ad60cf9` The **Canonical SHA-256** in §0 is computed over the entire file **with the single line matching `^Canonical SHA-256`** removed. To verify from the repo root: grep -v '^Canonical SHA-256' MINDBOOST_AUDIT_LEDGER.md | sha256sum
MASTER   = a0bada219d2bfdfe... (55628 B)
GATECAT  = 8c4adeecb98aa54c... (10485 B)
MANIFEST = e8ca17ae86ac5984... (2003 B)
INPUTS   = 8f5f060988f8649f... (2220 B)
FREEZE   = af198eeb1a64a035... (3817 B)
CREATE_TARGETS_STILL_ABSENT=True
```

