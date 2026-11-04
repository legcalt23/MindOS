# S5_R4_2_PASO7_EXECUTION_PLAN — Paso 7 Focus V5 (plan normativo autocontenido, NO ejecutado)

> Miembro NORMATIVO del bundle R4.2. La autorización liga `PLAN_BUNDLE_ROOT` = sha256 de los bytes de
> `S5_R4_2_PLAN_BUNDLE_MANIFEST.json`, NUNCA este archivo aislado. `EXECUTE_STEP_7 = DENIED` hasta el
> mandato literal de §1. Ejecutar este plan NO requiere leer R3/R4/R4.1: son evidencia histórica NO
> normativa (annex §A4). El annex (`S5_R4_2_AUDIT_ANNEX.md`) es evidencia/adjudicación, no instrucciones.

---

## 1. Mandato, binding del bundle y prohibiciones

**Literal único de autorización futura (emitido por el CEO, todos los parámetros dentro del mandato):**

```
EJECUTAR_PASO7_R4_2 PLAN_BUNDLE_ROOT=<64hex> CUSTODY=D DISCOVERABILITY=ADR_REGISTER+CLAUDE_MD+SCANNER REAUDIT=FULL_SCOPED RUN_ID=<uuid-v4-no-usado>
```

Verificación en R42-0 (todas obligatorias; cualquier fallo ⇒ `BLOCKED_DECISION`, cero writes):

1. Releer `docs/engines/focus_v5/S5_R4_2_PLAN_BUNDLE_MANIFEST.json` del repo; `sha256(bytes crudos)` ==
   `PLAN_BUNDLE_ROOT` del mandato.
2. `sha256` de CADA miembro listado en el manifest == el valor listado (bundle íntegro; sin esto, ningún
   miembro es normativo).
3. `CUSTODY=D`, `DISCOVERABILITY=ADR_REGISTER+CLAUDE_MD+SCANNER`, `REAUDIT=FULL_SCOPED` — son los ÚNICOS
   valores que este plan implementa; cualquier otro valor ⇒ `BLOCKED_DECISION` (el plan habría de
   reescribirse, no improvisarse).
4. `RUN_ID` ∉ `${OPS_ROOT}/registry/run_ids.jsonl` (§2.4).

**Prohibiciones permanentes de toda la ejecución:** editar código/build/tests (`.kt/.kts/*.gradle*`);
invocar Gradle; ejecutar `docs/engines/focus_v5/tools/freeze_v5.cjs` en el directorio real (ESCRIBE el
manifest); mutar git (`add/commit/push/stash/checkout/reset` — git es SOLO lectura); montar focus2;
auto-congelar (`FROZEN_FOR_S5_*` = solo la auditoría externa); escribir cualquier path fuera de la
allowlist §4; declarar un STOP como DONE; ofrecer commit/PR.

---

## 2. Entorno operacional (Clase O — durable, fuera del worktree)

### 2.1 Layout

`${OPS_ROOT}` = `C:\Users\curro\MindOS_ops` (perfil de usuario: durable; NUNCA `%TEMP%`, NUNCA dentro del
repo, jamás bajo git, jamás exportado completo a ningún paquete).

```
${OPS_ROOT}/
  locks/<repo_key>.lock                  # lock COMPARTIDO por repo (§2.3)
  registry/run_ids.jsonl                 # registro append-only de run-ids (§2.4)
  runs/p7_<RUN_ID>/
    pre_state/                           # PRE_STATE_MANIFEST.json + status crudo (§3)
    backup/                              # bytes pre-state de los targets REPLACE/HUNK
    staging/repo/<paths espejo>          # TODOS los deliverables en construcción
    staging/shadow/                      # shadow-root para validar el ledger staged (§5.13)
    journal/TRANSACTION_JOURNAL.json     # journal durable de la transacción
    evidence/                            # INTERNAL_EVIDENCE_BUNDLE (§7.2)
    packages/{fingerprint,reproduction,external}/
    closure/RUN_CLOSURE_ATTESTATION.json # attestation final (§11)
```

### 2.2 Retención por clase (no uniforme)

| Clase | Retención |
|---|---|
| `backup/` | borrable SOLO tras `COMPLETED` + aceptación externa registrada |
| `evidence/` + `packages/` | retener hasta aceptación externa; root hash sellado en capsule y attestation |
| `journal/` + `closure/` + `registry/` | retención de gobernanza (indefinida) |

Acceso: solo el usuario local; sin sincronización cloud; sin PII ni secretos (charter del ledger aplica).

### 2.3 Lock compartido por repo (excluye TODOS los run-ids)

- Path: `${OPS_ROOT}/locks/<repo_key>.lock` con `repo_key` = primeros 16 hex de
  `sha256(UTF-8(ruta canónica del repo en minúsculas))`. Para este repo
  (`c:\users\curro\androidstudioprojects\mindos`): `repo_key = 1c6b17edc619597d`.
- Adquisición: creación EXCLUSIVA (`CreateNew`; falla si existe). Contenido JSON:
  `{run_id, host, pid, pid_start_utc, acquired_utc, repo_path}`. Dos `RUN_ID` distintos comparten ESTE
  lock: no pueden ejecutar a la vez sobre el mismo repo.
- Stale: el lock es stale sii el PID no existe O `(pid, pid_start_utc)` no coincide con un proceso vivo.
  Romperlo SOLO con decisión CEO explícita `BREAK_STALE_LOCK=YES` registrada en journal + registry.
  Nunca por TTL solo.
- Liberación: tras `CLOSURE_ATTESTED`→`COMPLETED` (§11) o tras rollback verificado (§9).

### 2.4 Registro de run-ids

`${OPS_ROOT}/registry/run_ids.jsonl`, append-only, escrito SOLO bajo el lock (el lock resuelve la
concurrencia del registro). Eventos: `{run_id, ts_utc, event: STARTED | TERMINAL:<estado>}`. La
virginidad de un `RUN_ID` se verifica contra ESTE registro (no contra «existe journal»: una caída pudo
preceder al journal). Recovery: journal del run y registry se cross-verifican; discrepancia ⇒
`PARTIAL_RECOVERY_REQUIRED`.

---

## 3. PRE_STATE_MANIFEST, drift y post-validation

### 3.1 Captura (R42-1, bajo lock, ANTES de todo write)

1. `head_commit`, `head_tree` (`git rev-parse HEAD`, `git log -1 --format=%T`), rama, upstream ±, stash.
2. `git status --porcelain=v2 -z` COMPLETO (bytes crudos preservados en `pre_state/`).
3. **Dirty overlay:** `sha256` + bytes de CADA archivo M/A/R/?? del status — incluidos los ajenos
   (fingerprint de concurrencia; nunca exportado).
4. **Target/denylist map:** para cada path de la allowlist §4: `sha256`+bytes o `ABSENT`; para cada
   entrada de la denylist explícita §4: `sha256`+bytes.

### 3.2 Drift contra la referencia del plan

Los hashes de §4 son la REFERENCIA medida el 2026-07-22 (EV-R42-04, annex). Si en R42-1 algún target
difiere de su referencia ⇒ `STOPPED_NEEDS_REVIEW` con el delta reportado al CEO — salvo que el mandato
incluya explícitamente `ACCEPT_TARGET_DRIFT=YES` (en cuyo caso el pre-state capturado pasa a ser la base
y así se registra). El árbol es compartido con otros agentes: el drift NO se «arregla», se decide.

### 3.3 Post-validation (R42-7, algoritmo)

Recomputar §3.1 (1)–(4) y verificar: (a) `head/tree/rama` idénticos; (b) cada target promovido:
`sha256(vivo) == staged_sha` del `PROMOTION_PLAN`; (c) TODO lo demás — dirty overlay y denylist —
byte-idéntico al pre-state. Resultado: (b) falla ⇒ ruta rollback §9. Solo (c) falla (drift en NO-target,
árbol compartido) ⇒ targets intactos: continuar a closure con `CONFLICT_NOTED(paths)` en la attestation;
jamás «arreglar» el drift ajeno.

---

## 4. Allowlist (13 targets literales) · Denylist · Membership

### 4.1 ALLOWLIST — único conjunto que la ejecución puede escribir en el repo

Acciones: CREATE (pre=ABSENT) · REPLACE (archivo completo) · HUNK_ONLY (solo la sección nombrada; resto
byte-idéntico). Referencia = sha256 (16 hex) + bytes medidos 2026-07-22 (EV-R42-04). Orden de promoción
en §8.2 (ledger ÚLTIMO).

| # | Path | Acción | Pre-state referencia |
|---|---|---|---|
| 1 | `docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md` | CREATE | ABSENT |
| 2 | `docs/engines/focus_v5/PROVENANCE_INCIDENT_V5_BUNDLE.md` | CREATE | ABSENT |
| 3 | `docs/engines/focus_v5/S5_R3_EVIDENCE_CAPSULE.md` | CREATE | ABSENT |
| 4 | `docs/engines/focus_v5/S5_R3_EVIDENCE_CAPSULE.json` | CREATE | ABSENT |
| 5 | `scripts/validate-authority-entrypoints.mjs` | CREATE | ABSENT |
| 6 | `docs/context_pack/05_TECHNICAL_CURRENT_STATE.md` | HUNK_ONLY (sección focus2/temporal) | `cca93e829d0632b5…` (30042 B) |
| 7 | `docs/context_pack/09_DECISION_LOG.md` | HUNK_ONLY (append nueva sección de decisiones P7) | `5c9d2005a23fc760…` (86018 B) |
| 8 | `docs/engines/focus_v5/S5_REOPEN_STATUS.md` | HUNK_ONLY (cabecera + §estado + §cierre P7) | `502f8ff8b94069bb…` (49557 B) |
| 9 | `docs/engines/focus_v5/ADR_REGISTER_V5.md` | HUNK_ONLY (marcador índice + fila ADR + nota custodia D) | `8320d28f73a28813…` (17584 B) |
| 10 | `docs/engines/focus_v5/ADR_FOCUS_TEMPORAL_QUALITY.md` | HUNK_ONLY, SOLO si el reaudit §6 lo exige | `d6941396ac941db2…` (93870 B) |
| 11 | `CLAUDE.md` | HUNK_ONLY (bloque discoverability §5.7) | `df385a054111b15a…` (333296 B) |
| 12 | `docs/engines/focus_v5/S5_R3_P7_HANDOFF.md` | CREATE | ABSENT |
| 13 | `MINDBOOST_AUDIT_LEDGER.md` | REPLACE (append filas + resello §0; ÚLTIMO) | `265e4b7eb1fffcd4…` (353989 B); sello §0 = `8526b509c3ec086d393d895cb4ba742d6b8fc415d760c1f39fd757859ad60cf9` |

Sin placeholders: `DISCOVERABILITY=ADR_REGISTER+CLAUDE_MD+SCANNER` está DECIDIDO (recomendación CEO
aceptada en el mandato); los entrypoints son los targets 9 y 11 y el scanner es el target 5.
`FOCUS_ENGINE_MASTER_PLAN_V5.md` NO es entrypoint y permanece en denylist.

### 4.2 DENYLIST explícita (verificada byte-idéntica en post-validation) + default-deny

`docs/engines/focus_v5/`: `FOCUS_ENGINE_MASTER_PLAN_V5.md` (`a0bada219d2bfdfe…`) ·
`GATE_CATALOG_V5.md` (`8c4adeecb98aa54c…`) · `SHA256_MANIFEST_V5.txt` (`e8ca17ae86ac5984…`) ·
`INPUTS_SHA256.txt` (`8f5f060988f8649f…`) · `EXERCISE_PORTFOLIO_V5.md` · `BASELINE_EVIDENCE_V5.md` ·
`REVIEWS_V5.md` · `REDTEAM_TEST_MATRIX_V5.md` · `REQUIREMENTS_MATRIX_V5.csv` · `S4_COUPLING_REPORT.md` ·
`S5_EXECUTION_PROPOSAL.md` · `S5_R3_PASO7_PLAN.md` · `S5_R4_PASO7_PLAN_REVIEWED.md` (`829537af…`) ·
`S5_R4_1_PASO7_PLAN_REVIEWED.md` (`b462948a…`) · el bundle R4.2 (3 archivos) · `dossiers/**` ·
`agent_outputs/**` · `tools/freeze_v5.cjs` (`af198eeb1a64a035…`; JAMÁS ejecutar en el dir real).
Resto del repo: `scripts/validate-audit-ledger.mjs` (solo ejecutar, no editar) · TODO
`**/*.kt|*.kts|*.gradle*` · los 2 iOS AJENOS (`shared/src/iosMain/.../dm/presentation/DMListScreenIcon.ios.kt`,
`shared/src/iosMain/.../social/presentation/media/AppMediaViewer.ios.kt`) = FOREIGN: nunca tocar, nunca
exportar · `.git/**` · `graphify-out/**` · `MEMORY.md`/memoria. **Default-deny: cualquier path no listado
en §4.1 es DENY.**

### 4.3 MEMBERSHIP para paquetes (congelada en R42-1 desde el pre-state)

Targets §4.1 ∪ `docs/engines/focus_v5/**` ∪ `docs/context_pack/{05,09}_*.md` ∪
`shared/src/**/mindboost/core/timing/**` ∪ `shared/src/**/mindboost/focus2/**` ∪ sus tests de
`desktopTest` ∪ `MINDBOOST_AUDIT_LEDGER.md` ∪ `scripts/validate-audit-ledger.mjs`. EXCLUYE SIEMPRE: los
2 iOS ajenos, `${OPS_ROOT}`, scratchpad, `.git`, `graphify-out`, cualquier manifest autorreferente.
La lista materializada (paths exactos) se congela ANTES de crear cualquier paquete.

---

## 5. Especificación de contenido staged (por target)

Todo se construye en `${OPS_OTS := ${OPS_ROOT}/runs/p7_<RUN_ID>}/staging/repo/<path espejo>`; los bytes
staged SON los bytes finales (EOL incluido, §8.3).

- **5.1 `CURRENT_AUTHORITY_INDEX.md`** — contiene `AUTHORITY_INDEX_ROLE: CURRENT` (única en
  `docs/engines/focus_v5/*.md`). Tabla de autoridad por artefacto: `{path, rol, estado, sha256_HEAD,
  sha256_worktree, disposición}`. **Disposición fila-a-fila del `GATE_CATALOG_V5.md`** (ligada a su hash
  `8c4adeec…`): cada gate `T-*` = `VIGENTE` | `SUPERSEDED_BY=<ref>` | `VOID_PROVENANCE`; el eje RELEASE
  (§10) solo puede leer filas `VIGENTE`. Producida por el scope integrador del reaudit (§6).
- **5.2 `PROVENANCE_INCIDENT_V5_BUNDLE.md`** — timeline (`15cb12a` → bundle roto de nacimiento 3/16 →
  register mutado en Paso 2); tabla 16 entradas OK/MISMATCH; forense:
  `NOT_FOUND_IN_LOCAL_GIT_OBJECT_DATABASE` ×2 (GATE_CATALOG sellado 9249 B, PORTFOLIO sellado 16693 B) —
  CI/remotos/backups = `NOT_CHECKED`; **hallazgo EV-R42-05: los bytes sellados de `INPUTS_SHA256.txt`
  EXISTEN en el worktree** (sha256 completo `8f5f060988f8649fee01d3f4a48ae87d1829dbb220e257fd4eeb5b34dbc6c4a0`,
  2220 B, == línea del manifest sellado; la versión commiteada en HEAD difiere) ⇒ 1/3 recuperable
  byte-a-byte; causa raíz `UNKNOWN` + `HYPOTHESIS` (edición post-generación pre-commit) SEPARADAS;
  límites (13/16 coinciden con HEAD). Los outputs forenses se re-ejecutan y CAPTURAN a `evidence/`.
- **5.3/5.4 `S5_R3_EVIDENCE_CAPSULE.md` + `.json`** — claim registry; inventario clasificado;
  CORRECTION_MANIFEST (12 filas, oráculo por fila); gate matrix; matriz finding→ledger con dedupe probado
  por fila (invariante+owner+fix+gate+evidencia; en duda NO fusionar; sin conteo esperado); **IDs de
  ledger CONGELADOS** (siguientes libres ≥ `MB-FOCUS-0047` verificados contra el ledger vivo);
  contraejemplos machine-readable con DOS cálculos independientes por EVIDENCE_ID; incident; prestate_sha;
  restore attestation (no sensible). Tras congelar, NADA re-mapea IDs sin invalidar el capsule.
- **5.5 `scripts/validate-authority-entrypoints.mjs`** (scanner; Node sin deps; NO se cablea a
  Gradle/CI en este paso — wiring = follow-up con autorización separada):
  - `ENTRYPOINTS = ['CLAUDE.md', 'docs/engines/focus_v5/ADR_REGISTER_V5.md']` (literales en el script).
  - `MARKER` (línea exacta): `FOCUS_V5_CURRENT_AUTHORITY_INDEX: docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md`.
  - Checks: (1) cada entrypoint contiene `MARKER` ≥1 vez; (2) exactamente UN archivo en
    `docs/engines/focus_v5/*.md` contiene `AUTHORITY_INDEX_ROLE: CURRENT` y es el índice; (3) los legacy
    `FOCUS_ENGINE_MASTER_PLAN_V5.md` y `GATE_CATALOG_V5.md` NO contienen ni `MARKER` ni el rol CURRENT.
    **Clasificador de citas históricas:** el scanner matchea SOLO los marcadores máquina exactos; la
    prosa histórica que nombra archivos sin marcador NO dispara (eso resuelve falsos positivos).
  - **Oráculo:** modo `--self-test` con fixtures sintéticos embebidos (in-memory): caso PASS; marcador
    ausente ⇒ FAIL; rol CURRENT duplicado ⇒ FAIL; legacy con marcador ⇒ FAIL. Exit 0/1. El self-test se
    ejecuta en staging y su output se captura a `evidence/`.
- **5.6 `05_TECHNICAL_CURRENT_STATE.md` / 5.8 `S5_REOPEN_STATUS.md` / 5.9 `ADR_REGISTER_V5.md`** —
  hunks derivados del capsule (proyección; gate «proyección-no-excede-fuente»). El register recibe:
  línea `MARKER` bajo el título + fila del ADR temporal-quality (`DRAFT_PENDING_REAUDIT` →
  `CANDIDATE_FOR_EXTERNAL_REVIEW` sii el reaudit §6 = PASS) + nota custodia D.
- **5.7 `09_DECISION_LOG.md` + `CLAUDE.md`** — 09: sección nueva con D-R3-01..04 + D-P7-A=D +
  D-P7-B=`ADR_REGISTER+CLAUDE_MD+SCANNER` + `SUPERSEDES=<id>` donde aplique (recency NUNCA supersede).
  CLAUDE.md: insertar INMEDIATAMENTE DESPUÉS de la línea de §6 que comienza por `- Docs clave:` el bloque:

  ```
  - **Focus V5 — autoridad vigente:** `docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md` (índice de
    procedencia y disposición; los artefactos legacy del bundle V5 se leen SOLO vía el índice).
    FOCUS_V5_CURRENT_AUTHORITY_INDEX: docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md
  ```

- **5.10 `ADR_FOCUS_TEMPORAL_QUALITY.md`** — SOLO si una lente del reaudit exige corrección: aplicar en
  staging ⇒ repetir el reaudit COMPLETO sobre el hash nuevo (§6); cambio normativo no autorizado ⇒
  `STOPPED_NEEDS_REVIEW`.
- **5.12 `S5_R3_P7_HANDOFF.md`** — staged ANTES de la promoción y promovido CON ella (target 12).
  Contiene: run_id, parámetros del mandato, `staged_sha` de los otros 12 targets, referencia al
  `PROMOTION_PLAN` por path+run_id (su hash queda en attestation y paquetes — acíclico), criterios de
  terminal y punteros a paquetes («root en RUN_CLOSURE_ATTESTATION»). SIN su propio hash; SIN hash del
  journal (el journal es privado; la referencia compartible es el `PROMOTION_PLAN`, §7.3).
- **5.13 `MINDBOOST_AUDIT_LEDGER.md`** — protocolo intacto: append-only (filas §2 con IDs congelados del
  capsule; eventos §3; checkpoint §4; task `TASK-<fecha>-FOCUS-V5-S5-R3-PASO7`); única línea reescrita =
  el sello §0. **Algoritmo del sello (self-excluding, replicado del validador):** cuerpo = todas las
  líneas del archivo EXCEPTO las que empiezan por `Canonical SHA-256`, unidas con `\n`, UTF-8 → sha256;
  ese hash se estampa en la línea `Canonical SHA-256`. Sin `|` literal en celdas (usar `abs()`); sin
  PII/secretos; EOL LF (`.gitattributes`). **Validación en staging (shadow-root):** copiar el ledger
  staged a `staging/shadow/MINDBOOST_AUDIT_LEDGER.md` y el validador (sin editar) a
  `staging/shadow/scripts/validate-audit-ledger.mjs`; ejecutar `node staging/shadow/scripts/...` — el
  validador resuelve la raíz relativa a su propia ubicación, luego valida la copia staged completa.
  Verde OBLIGATORIO pre-promoción; re-run sobre el vivo en post-validation.

Gates de staging (todos, outputs a `evidence/`): validador ledger shadow · comparador semántico
(artefacto hasheado con self-test rojo probado) · proyección-no-excede-fuente · secret/PII scan sobre
TODO lo nuevo · scanner self-test + run real sobre los entrypoints staged.

---

## 6. Reaudit `FULL_SCOPED` (7 lentes, ligado a hashes staged)

Lentes read-only, cada una con scope/criterios/veredicto SEPARADOS, sobre los hashes staged; prompts +
input-hashes + outputs CAPTURADOS a `evidence/`:

1. psicometría · 2. eficacia/claims · 3. timing/KMP · 4. seguridad · 5. privacidad · 6. a11y/fairness ·
7. **integrador** (consistencia doc↔impl: 12 filas del CORRECTION_MANIFEST vs ADR staged; truth-table
14/14 vs source+build config+wiring+flags — no solo bytes fuente; disposición fila-a-fila del
GATE_CATALOG para §5.1).

Reglas duras: cualquier finding P0/MAJOR ⇒ `STOPPED_NEEDS_REVIEW` (cero promoción). Cambio exigido ⇒
aplicar en staging ⇒ REPETIR el reaudit completo sobre los hashes nuevos; nada queda ligado a un hash
viejo. Los subagentes NO invocan Gradle (paso documental; regla 2026-07-19); un gate de build que una
lente crea necesario se declara `NOT_RUN`, no se improvisa. Resultado = `INTERNAL` (independencia de
contexto, no material); la aprobación material es de la auditoría externa por scopes.

---

## 7. Paquetes (4) y gates negativos

### 7.1 `INTERNAL_FINGERPRINT` — pre-state completo (incluye ajenos). NUNCA se exporta.

### 7.2 `INTERNAL_EVIDENCE_BUNDLE` (durable; corrige que la evidencia viva solo en staging efímero)

`evidence/` con TODOS los outputs primarios (forense, gates, reaudit, self-tests, mediciones) + **copias
hasheadas de los comparadores/scanners usados** (validador copiado, scanner, script de medición — si son
efímeros no son reproducibles). Root hash = sha256 del manifest ordenado de miembros; sellado en capsule
y en attestation. Retención §2.2; nunca se exporta completo — el packet externo recibe extractos
minimizados por allowlist-extraction.

### 7.3 `INTERNAL_REPRODUCTION_BUNDLE`

`head.txt` + `tracked.patch` = `git diff --binary --no-ext-diff HEAD -- <miembros tracked de la
membership §4.3>` (**path-limited: el pathspec se construye DESDE la membership congelada**) +
`untracked/` SOLO miembros untracked de la membership + `members.sha256` + `reconstruct.(ps1|sh)` +
`PROMOTION_PLAN` (§8.1; así el handoff referencia un artefacto resoluble). **Gate negativo obligatorio:**
parsear los headers del patch y verificar (a) cada path ∈ membership; (b) los 2 iOS ajenos ∉ patch;
(c) untracked exportado ⊆ membership. Cero paths fuera ⇒ PASS; si no ⇒ construir de nuevo, nunca «pasa
igual». **Prueba de reconstrucción:** en dir temporal limpio, HEAD + overlay ⇒ re-hash == staged set.

### 7.4 `EXTERNAL_AUDIT_PACKET` (minimizado)

Por scope de auditoría externa; allowlist-extraction de bloques mínimos + secret/PII scan + marca de
revisión humana pendiente; incluye capsule + handoff + plan + preguntas de falsación +
`MISSING_PRIMARY_EVIDENCE`/`NOT_CHECKED` declarados. Mismo gate negativo de §7.3 + `∩ FOREIGN = ∅`.

Los 4 se construyen y verifican DESDE staging ANTES de la promoción (§12, orden).

---

## 8. `PROMOTION_PLAN` + promoción crash-consistent

### 8.1 `PROMOTION_PLAN` (inmutable, pre-promoción)

`{run_id, targets:[{path, action, pre_sha|ABSENT, staged_sha}], orden}` — construido al cerrar staging
(incluye el `staged_sha` del handoff), hasheado, copiado a `packages/reproduction/`; su hash va a la
attestation. Inmutable: cualquier cambio posterior de staging invalida el plan y reinicia §12 desde
R42-4.

### 8.2 Promoción (por archivo, orden fijo)

Orden: targets 1→12 (§4.1), `MINDBOOST_AUDIT_LEDGER.md` ÚLTIMO (su §0 sella el estado final; el
validador re-corre sobre el vivo en post-validation). Por archivo:

1. **Recheck inmediato:** `sha256(vivo) == pre_sha` del plan (o `ABSENT` para CREATE) — inmediatamente
   antes del replace de ESE archivo, no solo una vez global. Falla ⇒ `CONFLICT_DETECTED` ⇒ rollback §9.
2. Escribir staged a temp `<target>.p7tmp_<RUN_ID>` en el MISMO directorio (mismo filesystem).
3. `Flush(true)` del stream (fsync del archivo).
4. Reemplazo: existentes → `[System.IO.File]::Replace(temp, target, null)` (Windows: semántica
   `MOVEFILE_REPLACE_EXISTING`+write-through; la durabilidad del rename la da el journaling NTFS —
   limitación declarada: Windows no expone fsync de directorio; en POSIX: `rename(2)` + `fsync` del
   directorio padre). CREATE → `File.Move` (falla si existe: ratifica ABSENT).
5. Re-hash `promoted_sha` == `staged_sha`; journal por archivo `{path, pre_sha|ABSENT, staged_sha,
   promoted_sha, ts}`.

### 8.3 Modo / symlinks / case / Unicode / EOL

Modo git 100644 todos (sin exec bits); sin symlinks; paths POSIX `/` + Unicode NFC + case-sensitive en
todos los manifests; `sha256` siempre de bytes crudos (nunca git-blob-id). EOL: los bytes staged son
finales — ledger = LF (`.gitattributes`); REPLACE/HUNK conservan el EOL del pre-state; CREATE = LF,
declarado por miembro en los manifests.

---

## 9. Rollback + crash recovery

**Rollback (orden INVERSO de promoción; protege CREATE y REPLACE frente a writers concurrentes):** por
target ya promovido: si `sha256(vivo) != promoted_sha` de ESTE run ⇒ **NO tocar** (otro writer), anotar
path ⇒ terminal `PARTIAL_RECOVERY_REQUIRED` con lista exacta `{path, esperado, encontrado, pasos}`. Si
coincide: REPLACE/HUNK → restaurar backup byte-a-byte (vía temp+replace §8.2); CREATE → borrar y
re-verificar `ABSENT`. Todo restaurado ⇒ verificar conjunto == pre-state ⇒ `ABORTED_ROLLED_BACK`.

**Crash recovery (journal-driven, reanudable por RUN_ID):**

| Journal dice | Acción |
|---|---|
| `PREPARED` sin ningún `PROMOTED` | nada vivo cambió: limpiar staging, liberar lock, `ABORTED_ROLLED_BACK` |
| promoción parcial | ruta rollback de arriba |
| todos `PROMOTED`, sin closure | estado `PROMOTED_PENDING_CLOSURE` (recuperable): reanudar §11 idempotente — post-validation → attestation → `COMPLETED`. JAMÁS `COMPLETED` sin post-validation registrada |
| closure completo | ya cerrado; solo re-verificar registry |

---

## 10. State machine

**RUN_STATE (persistido en journal; checkpoints en orden):** `INIT → LOCKED → PRE_STATE_CAPTURED →
STAGED_SUBSTANCE → REAUDIT_PASSED → CAPSULE_FROZEN → PROJECTIONS_STAGED → PACKAGES_VERIFIED →
PROMOTION_PLANNED → PROMOTING → PROMOTED_PENDING_CLOSURE → POST_VALIDATED → CLOSURE_ATTESTED →
COMPLETED → DELIVERED`. **Terminales de fallo (excluyentes):** `BLOCKED_DECISION` ·
`STOPPED_NEEDS_REVIEW` · `CONFLICT_DETECTED` · `ABORTED_ROLLED_BACK` · `PARTIAL_RECOVERY_REQUIRED`.
`COMPLETED` existe SOLO después de `CLOSURE_ATTESTED`; `DELIVERED` solo con handshake PASS (§11).
STOP ≠ DONE.

**Ejes documentales (autoridad / invalidador):**

| Eje | Valores | Regla |
|---|---|---|
| `ADR_STATUS` | `DRAFT_PENDING_REAUDIT` · `CANDIDATE_FOR_EXTERNAL_REVIEW` · `FROZEN_FOR_S5_R3` · `REOPENED_AFTER_EXTERNAL_AUDIT` · `SUPERSEDED` | **hoy = `DRAFT_PENDING_REAUDIT`** (entry criteria de CANDIDATE exigen reaudit ligado al hash final — §6; FROZEN solo por auditoría externa); edición ⇒ vuelve a DRAFT del delta |
| `EXTERNAL_AUDIT` (por scope) | `PENDING` · `APPROVED` · `REJECTED_WITH_FINDINGS` ×{ciencia, timing, seguridad, privacidad, a11y} | edición de un miembro expira ese scope |
| `INTERNAL_AUDIT` | `NOT_REVIEWED` · `HISTORICAL_INTERNAL_AUDIT_UNBOUND` (fijo) · `REAUDIT_FULL_SCOPED_PASS{scope}` · `NEEDS_REVIEW` · `FALSIFIED` | PASS solo con run+hash capturados |
| `IMPLEMENTATION` (por claim) | `NOT_STARTED` · `DESIGNED_NOT_RUN` · `PENDING` · `PARTIAL` · `CONFORME_UNMOUNTED` · `CONTRADICTED` · `MOUNTED` | fuente = source+build config+DI/wiring+flags+runtime; docs solo proyectan |
| `VALIDATION` (matriz) | filas `{domain, platform, environment, status ∈ NOT_RUN/STATIC/UNIT_SIM/DEVICE_MANUAL/PHYSICAL_BENCH}` | cambio de código/OS/device/umbral ⇒ impact assessment |
| `EVIDENCE_STATE` (runtime por-sesión) | `TRAINING_ONLY` · `PROVISIONAL` · `OFFICIAL_ELIGIBLE` (inconstructible) · `INVALIDATED` | recomputado por sesión por el motor; jamás editorial |
| `MOUNT` | `BLOCKED` · `SHADOW_ONLY` · `ALLOWED` | hoy `BLOCKED`; cualquier P0 nuevo ⇒ BLOCKED |
| `RELEASE` | solo gates marcados `VIGENTE` en `CURRENT_AUTHORITY_INDEX` (§5.1) | nunca el GATE_CATALOG legacy en bruto (procedencia inválida) |

---

## 11. Closure, `COMPLETED` y handshake de entrega

1. **`RUN_CLOSURE_ATTESTATION.json`** (en `closure/`, durable): `{run_id, mandato, promotion_plan_sha,
   journal_final_sha, promoted:[{path, sha}], package_roots:{evidence, reproduction, external},
   post_validation, conflict_noted:[], terminal, ts_utc}`. Se escribe DESPUÉS de post-validation y ANTES
   de declarar `COMPLETED`.
2. `COMPLETED` (journal + registry `TERMINAL:COMPLETED`) → liberar lock.
3. **Handshake de entrega (obligatorio para `DELIVERED`; corrige que validar solo el origen no evita
   otra truncación):** por cada artefacto entregado, el productor reporta `{sha256, bytes, lf_count,
   last_byte}`; el receptor MATERIALIZA su copia y reporta los mismos 4 valores; igualdad de los 4 ⇒
   `DELIVERY_INTEGRITY=PASS` ⇒ `DELIVERED`; cualquier diferencia ⇒ `DELIVERY_INTEGRITY=FAIL` aunque el
   origen esté completo (se re-entrega, no se re-escribe el origen).

---

## 12. Fases R42-0..R42-8 (orden transaccional OBLIGATORIO)

| Fase | Contenido | Estado al salir |
|---|---|---|
| R42-0 | Verificación del mandato + bundle binding (§1); git preflight (rama≠main, HEAD, sin fetch) | `INIT` |
| R42-1 | Lock compartido (§2.3) → registry `STARTED` (§2.4) → `PRE_STATE_MANIFEST` (§3.1) → chequeo drift (§3.2) → backups de targets 6–11,13 + restore PROBADO → membership congelada (§4.3). Cero bytes del repo cambiados | `PRE_STATE_CAPTURED` |
| R42-2 | Staging de SUSTANCIA: forense re-ejecutada y capturada, provenance (§5.2), correcciones ADR si aplican (§5.10), archivo de evidencia primaria | `STAGED_SUBSTANCE` |
| R42-3 | Reaudit `FULL_SCOPED` (§6) sobre hashes staged; si exige cambios: aplicar en staging y REPETIR completo → capsule congelado (§5.3/5.4, IDs ledger congelados) | `REAUDIT_PASSED` → `CAPSULE_FROZEN` |
| R42-4 | Proyecciones staged: 05/09/status/register/índice/CLAUDE-hunk/scanner(+self-test)/handoff/ledger resellado (§5) + gates de staging (validador shadow, comparador, proyección-no-excede, secret/PII, scanner run real) | `PROJECTIONS_STAGED` |
| R42-5 | Paquetes 4 (§7) construidos y verificados desde staging: gates negativos + prueba de reconstrucción + root hashes | `PACKAGES_VERIFIED` |
| R42-6 | `PROMOTION_PLAN` inmutable (§8.1) → recheck → promoción crash-consistent de los 13 targets, ledger último (§8.2) | `PROMOTING` → `PROMOTED_PENDING_CLOSURE` |
| R42-7 | Post-validation (§3.3) → `RUN_CLOSURE_ATTESTATION` → `COMPLETED` → registry → liberar lock | `POST_VALIDATED` → `CLOSURE_ATTESTED` → `COMPLETED` |
| R42-8 | Handshake de entrega productor↔receptor (§11.3) del handoff/paquetes que se entreguen | `DELIVERED` |

Fallo en cualquier fase ⇒ el terminal de fallo que corresponda (§10); nunca continuar con un gate en
rojo; nunca declarar un STOP como DONE.

---

## 13. Convención de conteo, completeness y reporte out-of-band

- **Convención normativa de conteo (corrige la retracción del annex §A1):** `LF_COUNT` = nº de bytes
  `0x0A`; `LOGICAL_LINES` = `LF_COUNT` si el último byte es `0x0A`, si no `LF_COUNT + 1`. PROHIBIDO usar
  `Measure-Object -Line` para contar líneas (ignora líneas en blanco — causa raíz del falso «449») y
  `Get-Content -Tail` para inspeccionar colas (PS 5.1 decodifica ANSI ⇒ mojibake). Medición = bytes
  crudos + decode UTF-8 explícito.
- **Completeness gate del bundle:** tras escribir cada miembro `.md`: releer del MISMO path; sha256 +
  bytes + `LF_COUNT` + último byte == `0x0A`; headings esperados presentes; cola sin frase cortada;
  outputs preservados como evidencia (annex §A6) y citados por EVIDENCE_ID — nunca «ver §X» sin el
  output real.
- **Reporte out-of-band (productor):** por cada miembro y por el manifest: `{sha256, bytes, LF_COUNT,
  last_byte}` + `PLAN_BUNDLE_ROOT = sha256(bytes del manifest)`. El manifest NO contiene su propio hash.
  El receptor verifica por handshake §11.3.
