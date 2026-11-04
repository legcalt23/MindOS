# S5 R4.1 · PASO 7 — R4 recuperada + reparación de aceptación (borrador candidato, NO ejecutado)

> Mandato: «RECUPERAR LA R4 TRUNCADA Y PRODUCIR R4.1 COMPLETA» (CEO, 2026-07-22). Invocación
> READ-ONLY sobre el repo; único archivo escrito = éste. `EXECUTE_STEP_7 = DENIED`. No sobrescribe la
> R4 (`S5_R4_PASO7_PLAN_REVIEWED.md`), que está COMPLETA y se conserva como evidencia.
> El hash autorizable de ESTE archivo se reporta FUERA de él (post-relectura, §16). Ejecución futura
> SOLO con: `EJECUTAR_PASO7_R4 PLAN_SHA256=<64hex-de-este-archivo> CUSTODY_DECISION=<A|B|C|D> RUN_ID=<uuid-no-usado>`.
>
> Etiquetas: `VERIFIED_PRIMARY` · `VERIFIED_PRIMARY_NOT_DURABLY_CAPTURED` (ejecutado esta sesión, output
> vive en el transcript, NO en el repo) · `VERIFIED_DERIVED` · `STATIC_SEARCH_ONLY` · `INFERENCE` ·
> `HYPOTHESIS` · `PROPOSAL` · `UNKNOWN` · `MISSING_PRIMARY_EVIDENCE` · `NOT_CHECKED` · `CONTRADICTED`.

---

## 1. VEREDICTO SOBRE LA TRUNCACIÓN + HASH DEL ORIGINAL

**`ATTACHMENT_TRUNCATED / ORIGINAL_COMPLETE` — probado criptográficamente.** `VERIFIED_PRIMARY`:
`sha256(bytes[0..45055])` de mi original `S5_R4_PASO7_PLAN_REVIEWED.md` =
`5c1463a3f29581873b51c13ea9cab7ebfed913b7a2067b0367966e5a4bc0a15c` = el hash del adjunto. La copia del
auditor es un **prefijo estricto**; mi original es **completo**:

| Propiedad | Original (in-repo) | Adjunto del auditor |
|---|---|---|
| SHA-256 | `829537af8c79394d94813a04f654264acb4604a6b8963deb3d7e9f2580a778e4` | `5c1463a3f29581873b51c13ea9cab7ebfed913b7a2067b0367966e5a4bc0a15c` |
| Bytes | 48 448 | 45 056 (= 44 KiB exactos) |
| Líneas | 449 | — |
| Newline final | SÍ (LF, byte 10) | NO (corte a mitad de `no\n   fu…`) |
| Self-red-team | 15/15 | 7/15 (cortado) |
| §14 CEO / §15 resumible | presentes | ausentes |

Conclusión: **la R4 no se re-escribe por estar rota** (no lo está); R4.1 existe porque la R4 conserva
defectos P0 REALES en su PROTOCOLO (los que el auditor R4A encontró), no por truncación. La R4 queda
como evidencia; R4.1 la SUPERSEDE en contenido de plan (`SUPERSEDES=S5_R4_PASO7_PLAN_REVIEWED.md@829537af`).

---

## 2. ADJUDICACIÓN DE LOS FINDINGS R4A (14 P0 + 15 P1)

`CONFIRMED` = defecto real de mi R4, corregido aquí. `PARTIAL` = cierto con matiz. `REFUTED` = rebatido
con evidencia. No acepto por venir del CEO; donde matizo, lo pruebo.

### P0 (R4A-P0-01..14)

| ID | Veredicto | Adjudicación y dónde se corrige |
|---|---|---|
| 01 truncado | **PARTIAL** | El *adjunto* está truncado (CONFIRMED); el *original* está COMPLETO (§1, prueba de prefijo). No hay «append ciego»; R4.1 es archivo nuevo completo. |
| 02 operational-state en repo | **CONFIRMED** | Mi R4 ponía `evidence/…lock/prestate/backup` DENTRO del worktree y afirmaba «cero bytes cambiaron» — contradicción. R4.1: lock/pre-state/backups/journal/staging/overlay = **fuera del worktree** (§4). Pre-state se captura ANTES de todo write al repo. |
| 03 sin allowlist completa | **CONFIRMED** | Solo enumeraba 6 respaldados pero creaba ≥10 artefactos. R4.1 §5: tabla path-by-path exhaustiva (CREATE/REPLACE/HUNK_ONLY, pre-state incl. `ABSENT`, fase, owner, rollback) + denylist. |
| 04 staging sin frontera | **CONFIRMED** | «staging» usaba paths `evidence/` in-repo. R4.1: staging externo con manifest propio; nada toca el repo hasta la fase de promoción (§4/§6). |
| 05 backups en repo | **CONFIRMED** | Backups de 05/09/register/ADR/ledger fuera del repo, permisos restrictivos, retención definida, NO empaquetados; en repo solo una attestation no sensible de restore-probado (§4). |
| 06 overlay recursivo | **CONFIRMED** | Overlay se construía in-repo copiando «todos los untracked» → podía incluirse a sí mismo + lock/backups. R4.1: overlay fuera del worktree desde **membership list congelada** que excluye operacionales (§7). |
| 07 packet incluye dirty-tree ajeno | **CONFIRMED** | «Todos los untracked» exportaría los 2 iOS ajenos al auditor. R4.1: 3 paquetes separados — fingerprint interno / reproduction bundle (dependency closure) / **external packet minimizado** sin ajenos (§7). |
| 08 un solo diff-auditor | **CONFIRMED** | R4.1: el diff-auditor solo emite `INTERNAL_DOCUMENT_AND_IMPLEMENTATION_CONSISTENCY_PASS`; el `INTERNAL_ADR_REAUDIT` amplio se hace por 4 scopes frescos y preservados (§8). |
| 09 forense resumida | **PARTIAL** | Los comandos forenses SÍ se ejecutaron (primarios), pero su output NO está capturado durablemente in-repo — vive en el transcript. Reetiqueto de `VERIFIED_PRIMARY` a **`VERIFIED_PRIMARY_NOT_DURABLY_CAPTURED`** (§3/§9). En ejecución se repiten y se capturan al archive. |
| 10 «atómica» multiarchivo | **CONFIRMED** | No hay atomicidad multiarchivo. R4.1: **`crash-consistent two-phase promotion`** (temp mismo FS + fsync + atomic-replace por archivo + precondition inmediata + journal durable + rollback global) (§6). |
| 11 sin rollback de archivos nuevos | **CONFIRMED** | Pre-state registra `ABSENT` para paths nuevos; rollback borra SOLO los paths creados por ese run-id y re-verifica `ABSENT`; nunca toca un path que otro writer cambió (§6). |
| 12 grafo final stale | **CONFIRMED** | Terminal + journal se congelan ANTES del handoff; handoff después; package manifest después; ningún miembro cambia luego (§6/§12 orden). |
| 13 authority index no obligatorio | **CONFIRMED (parcial en fuerza)** | Un archivo nuevo no cambia comportamiento. R4.1: mecanismo de descubrimiento ejecutable — modificar UN entrypoint realmente obligatorio (bajo hunk-allowlist + decisión CEO) + scanner-gate; hasta que ese entrypoint lo referencie, discoverability = **PARTIAL declarado** (§11). |
| 14 sin hash autorizable completo | **PARTIAL** | El *adjunto* no era autorizable (CONFIRMED); mi original SÍ tenía hash completo (`829537af…`). R4.1 adopta el protocolo: re-leer tras escribir, contar headings, verificar 15/15 + newline, y reportar hash/bytes/líneas/último-heading FUERA del archivo (§16). |

### P1 (1..15) — todos CONFIRMED salvo nota

| # | Corrección adoptada |
|---|---|
| 1 | `NOT_FOUND` → **`NOT_FOUND_IN_LOCAL_GIT_OBJECT_DATABASE`**; CI / remotos no-fetched / backups / otras máquinas / archivos del usuario = **`NOT_CHECKED`** (no «irrecuperable») (§3). |
| 2 | `causa raíz UNKNOWN` ya NO incluye causa: la «edición post-generación pre-commit» se marca **`HYPOTHESIS`**, separada del `UNKNOWN` (§3). |
| 3 | Recency NO supersede: solo una decisión con `SUPERSEDES=<id>` + autoridad válida reemplaza otra (§10). |
| 4 | Implementación real = **source + build config + DI/wiring + feature flags + artefacto/runtime pertinente**, no bytes fuente solos (§9/§10). |
| 5 | `VALIDATION` = **matriz {domain, platform, environment, status}**, no escalera escalar (§9). |
| 6 | **`EXTERNAL_AUDIT` es un eje propio**, separado de `ADR_STATUS` (§9). |
| 7 | `EVIDENCE_STATE` = estado **runtime por-sesión**, no editorial global; con invalidadores (§9). |
| 8 | Stale-lock: takeover exige verificar **owner/proceso/run**, no solo TTL (§4/§6). |
| 9 | Run-id no virgen por «no hay journal»: **registro/lock externo durable** + UUID no reutilizable; una caída pudo preceder al journal (§4). |
| 10 | Secret/PII scan defense-in-depth: **allowlist-extraction de bloques mínimos + revisión humana** antes de exportar a un auditor externo (§7/§8). |
| 11 | Root digest en chat = **`out-of-band record`**, distinto de **`cryptographic attestation`** (tag firmado, futuro, con autorización) (§16). |
| 12 | El capsule asigna **IDs ledger / issue-keys estables ANTES de congelarse**; R4-5 no puede cambiar mappings sin invalidar el capsule (§6/§13). |
| 13 | `CANDIDATE_FOR_EXTERNAL_REVIEW` con **entry criteria explícitos** (§9); no implica quality-pass. |
| 14 | Reconstrucción especifica **path encoding, case-sensitivity, Unicode NFC, symlinks, exec bits, EOL** (§13). |
| 15 | **Completeness gate anti-truncación**: headings obligatorios, 15/15 self-red-team, §CEO, §resumible, newline final, hash post-relectura (§16). |

---

## 3. CONSERVADO DE LA R4 (sin degradar, con fuerza epistémica corregida)

R4 (`docs/engines/focus_v5/S5_R4_PASO7_PLAN_REVIEWED.md`, `829537af…`, in-repo durable) contiene la
adjudicación íntegra de las **64 hipótesis** del mandato anterior (61 CONFIRMED / 2 PARTIAL — 29 y 58 —
/ 1 UNKNOWN — S-05). Se conservan sin cambio y no se re-pegan (bloat que el auditor ya aceptó); se citan
por hash. Se CONSERVAN además, con fuerza corregida:

| Ref | Conservado | Corrección de fuerza epistémica |
|---|---|---|
| gate histórico | `HISTORICAL_INTERNAL_AUDIT_UNBOUND` permanente | intacto |
| reaudit | nuevo, sobre hash ADR final, ADR read-only después | ahora **por scopes** (§8), no un diff-auditor |
| capsule/handoff | separados, acíclicos | intacto + orden de congelado (§6) |
| delta vs pre-state | reemplaza worktree-absoluto | intacto |
| Opción D | custodia recomendada | intacto + discoverability real (§11) |
| S-01 | brecha bundle **3/16** (catálogo, portfolio, inputs), un solo commit `15cb12a`, bytes sellados no en git | `NOT_FOUND` → **`NOT_FOUND_IN_LOCAL_GIT_OBJECT_DATABASE`**; remoto/CI/backups = `NOT_CHECKED` |
| S-02 | scratchpad sobrevivió al reinicio (R3 byte-idéntica) | intacto (`VERIFIED_PRIMARY`) |
| S-03 | `INPUTS_SHA256.txt` roto ⇒ ancla de inputs también inválida | intacto |
| S-04 | focus2 sin feature-flag ⇒ flag/kill-switch = precondición NUEVA de S6 | intacto |
| S-05 | mandato duplicado del chat sin rastro en repo | `UNKNOWN` (aceptado como reporte CEO) |
| provenance | causa raíz | `UNKNOWN` + **`HYPOTHESIS`** (edición post-generación pre-commit), separadas |
| CEO assurance | matriz 18 dominios docs≠impl≠valid≠humana | intacto (§14) |

Los outputs forenses de S-01/S-03 son `VERIFIED_PRIMARY_NOT_DURABLY_CAPTURED` hasta que R41-1 los capture
al archive.

---

## 4. ARQUITECTURA OPERACIONAL — DOS CLASES, OPERACIONAL FUERA DEL REPO

El defecto raíz de mi R4 fue mezclar estado OPERACIONAL con DELIVERABLES en `evidence/` in-repo. R4.1
separa dos clases y una frontera dura.

**Clase O — OPERACIONAL, SIEMPRE fuera del worktree** (dir externo `${OPS}` = p. ej.
`%LOCALAPPDATA%\claude\…\p7_run_<RUN_ID>\`, permisos restrictivos, jamás git, jamás en ningún paquete):
`lock` · `PRE_STATE_MANIFEST.json` · `backup/` (bytes de los canónicos existentes) · `staging/` (TODAS
las copias en construcción) · `journal.json` · `overlay/` · `reproduction_bundle/` ·
`external_packet/` · `stop_report/`. Backups: retención definida, borrado tras `COMPLETED` verificado;
en repo solo una **attestation no sensible** («restore probado, sha del backup = X») dentro del capsule.

**Clase C — CANÓNICO DURABLE, se PROMUEVE al repo en la transacción** (Fase A): docs `05` · `09` ·
`S5_REOPEN_STATUS` · `ADR_REGISTER` (solo si custodia lo permite) · `MINDBOOST_AUDIT_LEDGER` ·
`S5_R3_EVIDENCE_CAPSULE.md` + `.json` · `PROVENANCE_INCIDENT.md` · `CURRENT_AUTHORITY_INDEX.md`. Fase B
(tras congelar journal+terminal): `S5_R3_P7_HANDOFF.md`. El ADR se corrige en staging y se relee-liga;
su promoción es parte de Fase A si cambió.

**Frontera dura:** todo se construye/valida en `${OPS}/staging`; el repo no cambia hasta la promoción
crash-consistent (§6). El overlay/reproduction/external-packet se construyen en `${OPS}` desde una
membership list congelada → **no pueden auto-incluirse** ni exportar operacionales.

Lock: creación exclusiva de `${OPS}/lock` con `{owner, pid, run_id, ts}`. Stale-takeover: SOLO si el
proceso owner no vive Y decisión CEO explícita registrada; nunca por TTL solo (P1-8). Run-id: verificado
contra un registro durable externo de run-ids usados, no contra «existe journal» (P1-9).

---

## 5. ALLOWLIST / DENYLIST EXHAUSTIVAS (path-by-path)

Fase: A=promoción principal, B=handoff post-congelado, OPS=externo (nunca repo). Acción: CREATE (pre=ABSENT),
REPLACE (pre=hash conocido), HUNK_ONLY (solo secciones nombradas). Rollback de CREATE = borrar si el path
sigue siendo el que creó este run-id, re-verificar `ABSENT`; de REPLACE/HUNK = restaurar backup byte-a-byte.

### ALLOWLIST (único conjunto que P7 puede escribir)

| Path | Acción | Pre-state esperado | Fase | Owner | Rollback |
|---|---|---|---|---|---|
| `docs/context_pack/05_TECHNICAL_CURRENT_STATE.md` | HUNK_ONLY (sección focus2/temporal) | `cca93e82…` (M) | A | sesión | backup |
| `docs/context_pack/09_DECISION_LOG.md` | HUNK_ONLY (nueva §20.6, append) | `5c9d2005…` (M) | A | sesión | backup |
| `docs/engines/focus_v5/S5_REOPEN_STATUS.md` | HUNK_ONLY (cabecera, §0.4, §0.5, §7) | `502f8ff8…` (U) | A | sesión | backup |
| `docs/engines/focus_v5/ADR_REGISTER_V5.md` | HUNK_ONLY (fila 24 + nota custodia) **solo si CUSTODY∈{A,D}** | `8320d28f…` (M) | A | sesión + CEO | backup |
| `docs/engines/focus_v5/ADR_FOCUS_TEMPORAL_QUALITY.md` | HUNK_ONLY (§R3-* de correcciones) solo si reaudit pide edición | `d6941396…` (U) | A | sesión | backup |
| `MINDBOOST_AUDIT_LEDGER.md` | REPLACE (append filas §2/§3/§4 + resello §0) | `265e4b7e…` + sello `8526b509…` (T-clean) | A | sesión | backup |
| `docs/engines/focus_v5/S5_R3_EVIDENCE_CAPSULE.md` | CREATE | ABSENT | A | sesión | delete-if-ours |
| `docs/engines/focus_v5/S5_R3_EVIDENCE_CAPSULE.json` | CREATE | ABSENT | A | sesión | delete-if-ours |
| `docs/engines/focus_v5/PROVENANCE_INCIDENT_V5_BUNDLE.md` | CREATE | ABSENT | A | sesión + CEO | delete-if-ours |
| `docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md` | CREATE | ABSENT | A | sesión + CEO | delete-if-ours |
| `docs/engines/focus_v5/S5_R3_P7_HANDOFF.md` | CREATE | ABSENT | B | sesión | delete-if-ours |
| `<entrypoint obligatorio>` (§11, p. ej. `ADR_REGISTER` cabecera o `FOCUS_ENGINE_MASTER_PLAN` cabecera) | HUNK_ONLY (1 línea → índice) **solo bajo decisión CEO D-P7-B** | hash conocido | A | CEO | backup |

### DENYLIST (jamás tocado por P7)

`FOCUS_ENGINE_MASTER_PLAN_V5.md` · `GATE_CATALOG_V5.md` · `SHA256_MANIFEST_V5.txt` · `EXERCISE_PORTFOLIO_V5.md`
· `INPUTS_SHA256.txt` · `tools/freeze_v5.cjs` · `agent_outputs/**` · TODO `.kt/.kts/*.gradle*` (13 M + ~20 U)
· los 2 iOS ajenos (`DMListScreenIcon.ios.kt`, `AppMediaViewer.ios.kt`) · `S5_R4_PASO7_PLAN_REVIEWED.md` ·
`S5_R3_PASO7_PLAN.md` · `S5_EXECUTION_PROPOSAL.md` · este archivo · **todo `${OPS}/**` (externo)**.
Regla: cualquier path no listado en ALLOWLIST es DENY por defecto; el gate delta (§6) lo verifica byte-idéntico.

---

## 6. TRANSACCIÓN CRASH-CONSISTENT + RECOVERY (existentes y nuevos)

No es atómica multiarchivo; es **crash-consistent two-phase promotion**.

**Preparación (todo en `${OPS}`):** para cada target de la allowlist, `staged` construido y validado
(validador ledger sobre la copia staged; comparador semántico; proyección-no-excede-fuente; secret/PII
scan). Journal fase-1 `PREPARED`: `[{path, pre_sha|ABSENT, staged_sha, action}]`.

**Recheck optimista:** inmediatamente antes de promover, re-hash de CADA `pre_sha` contra el árbol VIVO.
Cualquier diferencia (otro writer, HEAD movido) ⇒ `CONFLICT_DETECTED`, cero promoción.

**Promoción (Fase A, por archivo):** para cada target: escribir `staged` a un temp EN EL MISMO
filesystem del destino → `fsync` → **atomic replace** (rename) → re-hash `promoted_sha`. La precondition
se re-verifica INMEDIATAMENTE antes de cada replace individual (no solo una vez global). Journal registra
cada replace. El ledger (`§0` resello) va ÚLTIMO dentro de Fase A y su validador corre sobre el VIVO.

**Rollback global:** fallo en cualquier punto ⇒ para cada target ya promovido, restaurar: REPLACE/HUNK →
backup byte-a-byte; CREATE → borrar SOLO si el path sigue siendo el creado por este run-id (si otro
writer lo cambió, NO borrar → `PARTIAL_RECOVERY_REQUIRED`), re-verificar `ABSENT`. Verificar byte-a-byte
que el conjunto == pre-state. Journal `ROLLED_BACK`, terminal `ABORTED_ROLLED_BACK`.

**Congelado + Fase B:** solo si Fase A promovió TODO y validó: terminal `COMPLETED` se escribe al journal,
journal se **congela** (hash final). SOLO ENTONCES se crea el `HANDOFF` (Fase B) que referencia los
hashes ya congelados (incluido el del journal). El package manifest del reproduction/external se crea
DESPUÉS del handoff. Ningún miembro se re-edita (P0-12: sin grafo stale).

**Crash recovery (reanudar por RUN_ID):** leer journal. `PREPARED` sin ningún `PROMOTED` ⇒ nada vivo
cambió, borrar staging, liberar lock. Promoción parcial ⇒ restore total desde backup + verificación
byte-a-byte ⇒ `ABORTED_ROLLED_BACK`; si el restore no se CONFIRMA byte-a-byte ⇒ `PARTIAL_RECOVERY_REQUIRED`
con lista exacta de paths+hashes esperados+pasos. `PROMOTED`+`COMPLETED`+handoff ⇒ ya cerrado.

IDs de ledger / issue-keys se asignan y CONGELAN en el capsule ANTES de la promoción (P1-12): R41-5 no
puede re-mapear sin invalidar el capsule y reiniciar downstream.

---

## 7. LOS TRES PAQUETES (fingerprint / reproduction / external)

Construidos en `${OPS}` desde membership lists CONGELADAS antes de crear cada output (no recursivos).

1. **`INTERNAL_FINGERPRINT`** (concurrencia/forense): el `PRE_STATE_MANIFEST` COMPLETO — todo el
   dirty-tree, incluidos los 2 iOS ajenos y todo untracked. Uso: detección de writers y delta. **Nunca
   se exporta.**
2. **`INTERNAL_REPRODUCTION_BUNDLE`** (equipo): `base HEAD + overlay determinista` de la **dependency
   closure auditada** = {docs canónicos, ledger, capsule, ADR, código focus2/core.timing citado por las
   lentes}. Excluye explícitamente: overlay-output, staging, lock, backups, journal, external_packet,
   los 2 iOS ajenos, cualquier manifest autorreferente. Incluye `reconstruct.(ps1|sh)` + `members.sha256`
   derivado de la membership congelada.
3. **`EXTERNAL_AUDIT_PACKET`** (auditor externo, minimizado): SOLO lo que cada scope necesita (§8) +
   capsule + handoff + este plan + preguntas de falsación + `MISSING_PRIMARY_EVIDENCE`/`NOT_CHECKED`
   declarados + política de invalidación. **No exporta los iOS ajenos ni ningún path fuera de la
   dependency closure sin justificación + autorización CEO.** Transcripts/outputs de lente: **allowlist-
   extraction de bloques mínimos + secret/PII scan + revisión humana** antes de incluir (P1-10). Cada
   miembro con path-encoding/case/NFC/EOL declarados (§13).

Prueba de reconstrucción (obligatoria antes de declarar «listo»): en dir temporal limpio, HEAD + overlay
⇒ re-hash ⇒ root manifest idéntico. Root digest reportado **out-of-band** (`out-of-band record`; la
`cryptographic attestation` — tag firmado — es futura, con autorización) (P1-11).

---

## 8. REAUDIT — SCOPE HONESTO Y REVISORES ADECUADOS

Se separan DOS resultados, ninguno colapsa scopes:

- **`INTERNAL_DOCUMENT_AND_IMPLEMENTATION_CONSISTENCY_PASS`** — 1 diff-auditor read-only: (a) las 12
  filas del CORRECTION_MANIFEST vs el ADR vivo, (b) truth-table 14/14 de §R3-15 vs código (source +
  build config + wiring + flags, no solo bytes; P1-4). Emite SOLO consistencia doc↔impl; **no** afirma
  validez científica. Prompt+input-hash+output CAPTURADOS al archive.
- **`INTERNAL_ADR_REAUDIT`** (amplio, opcional pero requerido si se quiere afirmar más que consistencia):
  4 lentes frescas y preservadas — psicometría/eficacia · timing/KMP · seguridad/privacidad · integrador
  diff — cada una con scope, criterios, blockers y veredicto SEPARADOS, sobre el hash ADR final. Sin
  ellas, el gate se queda en el nombre limitado de arriba. Ambos resultados son `INTERNAL` (independencia
  de contexto, NO material; la material la aporta el auditor externo §7/§11-R4).

Regla dura: si el reaudit pide un cambio del ADR, se aplica en staging y el reaudit COMPLETO se repite
sobre el hash nuevo (nada queda ligado al hash viejo); tras el PASS, ADR read-only lógico.

---

## 9. STATE MACHINE NAMESPACED (corregida)

Ejes con autoridad de transición e invalidador. Correcciones R4A: `VALIDATION` = matriz;
`EXTERNAL_AUDIT` = eje propio; `EVIDENCE_STATE` = runtime por-sesión con invalidadores; `IMPLEMENTATION`
= fuente compuesta; `CANDIDATE` con entry-criteria.

| Eje | Valores literales | Autoridad | Invalidador |
|---|---|---|---|
| `ADR_STATUS` | `DRAFT` · `CANDIDATE_FOR_EXTERNAL_REVIEW` · `FROZEN_FOR_S5_R4` · `REOPENED_AFTER_EXTERNAL_AUDIT` · `SUPERSEDED` | CEO (FROZEN); nunca la sesión sola | edición del ADR ⇒ vuelve a DRAFT del delta |
| `CANDIDATE entry-criteria` | requiere: reaudit interno con scope declarado + newline/completeness gate + hash re-leído | sesión | falla cualquiera ⇒ no es CANDIDATE |
| `EXTERNAL_AUDIT` (eje propio, por scope) | `PENDING` · `APPROVED` · `REJECTED_WITH_FINDINGS` — uno por {ciencia, timing, seguridad, privacidad, a11y} | auditor externo | edición de cualquier miembro ⇒ expira ese scope |
| `INTERNAL_AUDIT` | `NOT_REVIEWED` · `HISTORICAL_INTERNAL_AUDIT_UNBOUND` (fijo) · `INTERNAL_DOC_IMPL_CONSISTENCY_PASS` · `INTERNAL_ADR_REAUDIT_PASS{scope}` · `NEEDS_REVIEW` · `FALSIFIED` | sesión; PASS solo con run+hash capturados | edición del artefacto ⇒ expira |
| `IMPLEMENTATION` (por claim) | `NOT_STARTED` · `DESIGNED_NOT_RUN` · `PENDING` · `PARTIAL` · `CONFORME_UNMOUNTED` · `CONTRADICTED` · `MOUNTED` | **source + build config + DI/wiring + flags + artefacto/runtime** (P1-4); docs proyectan | diff en cualquiera de esas capas ⇒ re-derivar |
| `VALIDATION` (matriz, no escalera) | fila `{domain, platform, environment, status∈NOT_RUN/STATIC/UNIT_SIM/DEVICE_MANUAL/PHYSICAL_BENCH}` | el run que la produce (con captura) | cambio de código/OS/device/thresholds ⇒ impact assessment |
| `EVIDENCE_STATE` (runtime, POR-SESIÓN — no editorial) | `TRAINING_ONLY` · `PROVISIONAL` · `OFFICIAL_ELIGIBLE`(inconstructible) · `INVALIDATED` (+ `NotCaptured`) | el motor en runtime, por sesión | recomputo por sesión; renombrado de `PROVISIONAL` = decisión de código PENDIENTE (C-R3-002) |
| `MOUNT` | `BLOCKED` · `SHADOW_ONLY` · `ALLOWED` | gate S6 (F-R3-A/C/D/E + flag S-04) | cualquier P0 nuevo ⇒ BLOCKED |
| `RELEASE` | escalera GATE_CATALOG vigente vía índice | gates T-* vigentes | T-S5 no-PASS ⇒ NOT_READY |
| `EXECUTION` (terminales P7, excluyentes) | `COMPLETED` · `BLOCKED_DECISION` · `STOPPED_NEEDS_REVIEW` · `CONFLICT_DETECTED` · `ABORTED_ROLLED_BACK` · `PARTIAL_RECOVERY_REQUIRED` | el journal | **solo `COMPLETED` satisface DoD; STOP≠DONE** |

Estados hoy (2026-07-22): `ADR_STATUS=CANDIDATE_FOR_EXTERNAL_REVIEW` (entry-criteria aún incompletos:
reaudit no ligado) · `INTERNAL_AUDIT=HISTORICAL…UNBOUND` · `IMPLEMENTATION`=mixto · `VALIDATION`=matriz
mayormente NOT_RUN (físico) / STATIC (plataforma) / UNIT_SIM (dominio) · `MOUNT=BLOCKED` ·
`EXTERNAL_AUDIT`=PENDING×5 · `EXECUTION`=n/a.

---

## 10. AUTHORITY MATRIX (corregida)

| Tipo de claim | MANDA | Ante discrepancia |
|---|---|---|
| Intended use / claims | CEO + charter + ForbiddenLexicon | gana el más restrictivo; discrepancia ⇒ STOP |
| Ley / privacidad | normativa + counsel/DPO (humanos) | el modelo REGISTRA riesgo y remite; nunca concluye |
| Requisito de producto | PRD + decisiones CEO | supersesión SOLO con `SUPERSEDES=<id>` + autoridad (P1-3); recency NO basta |
| Decisión aprobada | `09_DECISION_LOG` (+ ledger para findings) | entrada con `SUPERSEDES` explícito gana; sin ella = no decidido |
| Contrato de diseño | ADR vigente vía `CURRENT_AUTHORITY_INDEX` | CANDIDATE ≠ APPROVED (eje `EXTERNAL_AUDIT`) |
| **Implementación real** | **source + build config + DI/wiring + feature flags + artefacto/runtime** (P1-4) | doc contra esa realidad = `CONTRADICTED` del doc |
| Validación | runs con `{domain, platform, environment}` capturados | grep/compile/unit ≠ físico; cada celda etiquetada |
| Release / mount | GATE_CATALOG vigente vía índice + ledger blockers | un P0 abierto domina cualquier agregado |

---

## 11. CUSTODIA D + DISCOVERABILITY REAL

Recomendación conservada: **Opción D** (procedencia versionada + cuarentena). Concreciones:

- `PROVENANCE_INCIDENT`: timeline (`15cb12a` → 3/16 rotos de nacimiento → register mutado en Paso 2),
  tabla 16 entradas OK/MISMATCH, forense `NOT_FOUND_IN_LOCAL_GIT_OBJECT_DATABASE` ×3 (CI/remoto/backups
  = `NOT_CHECKED`), causa `UNKNOWN` + `HYPOTHESIS` separada, límites (13 entradas SÍ coinciden con HEAD).
- `LEGACY_MANIFEST_PRESERVED=YES`, `LEGACY_MANIFEST_VALIDATES_TREE=NO` (medido).
- **Discoverability EJECUTABLE (corrige R4A-P0-13):** crear `CURRENT_AUTHORITY_INDEX.md` NO basta. Se
  añade (i) **decisión CEO D-P7-B**: qué entrypoint REALMENTE obligatorio referencia el índice — candidatos:
  la cabecera de `ADR_REGISTER_V5.md`, la cabecera de `FOCUS_ENGINE_MASTER_PLAN_V5.md`, o la sección
  Focus de `CLAUDE.md` (la más leída por cualquier sesión); (ii) editar ESE entrypoint (HUNK_ONLY, 1
  línea → «autoridad vigente: ver CURRENT_AUTHORITY_INDEX.md») bajo allowlist; (iii) un **scanner-gate**
  (`node`, read-only) que escanee mandatos/entrypoints y falle si alguno cita GATE_CATALOG/MASTER_PLAN
  como autoridad sin pasar por el índice. **Hasta que (ii) exista, discoverability = `PARTIAL` declarado.**
- El register worktree del Paso 2 se CONSERVA (historia real); register = documento VIVO indexado; el
  bundle V5-S5 futuro (cierre real, autorización separada) incluirá un SNAPSHOT del register, nunca el vivo.

---

## 12. PLAN R4.1 (fases R41-0..R41-8; requiere el mandato parametrizado)

Prohibido siempre: editar código/build; Gradle; `freeze_v5.cjs` en dir real; montar; congelar;
`git add/commit/push/stash/checkout/reset` (git solo lectura); tocar fuera de allowlist; STOP como DONE.

- **R41-0 · Preflight externo.** Verificar mandato (`PLAN_SHA256`==hash de este archivo re-computado;
  `CUSTODY_DECISION`; `RUN_ID` no en el registro durable externo). Rama≠main; HEAD; upstream; stash.
  Lock en `${OPS}` (exclusivo; stale-takeover solo con proceso-muerto+decisión CEO). `PRE_STATE_MANIFEST`
  a `${OPS}` (git status v2 -z COMPLETO). Backups de canónicos existentes a `${OPS}/backup` + restore
  PROBADO. **Cero bytes del repo cambiados.**
- **R41-1 · Procedencia + custodia (staging).** Repetir forense y CAPTURAR output primario a
  `${OPS}/staging`. Redactar `PROVENANCE_INCIDENT` + `CURRENT_AUTHORITY_INDEX` (staging). Aplicar D-P7-B
  si se autoriza.
- **R41-2 · Evidencia primaria (staging).** Archivar bytes de R3/R4/tracker + extractos verbatim de
  lente (allowlist-extraction + secret/PII scan + marca de revisión humana pendiente). Gate histórico =
  `HISTORICAL…UNBOUND`. Definir el reaudit (§8).
- **R41-3 · Correcciones ADR (staging) + reaudit + freeze lógico.** Editorial vs normativo (normativo ⇒
  `STOPPED_NEEDS_REVIEW`). Reaudit sobre hash final; repetir completo si cambia; ADR read-only después.
- **R41-4 · Capsule inmutable (una finalización).** Claim registry + inventario clasificado (motor con
  lookahead real O doble pasada) + CORRECTION_MANIFEST (12 filas, oráculo por fila) + gate matrix +
  matriz finding→ledger con dedupe PROBADO (sin conteo esperado) + IDs ledger CONGELADOS + fixtures
  machine-readable de contraejemplos + incident + prestate_hash. Finalizar capsule (`.md`+`.json`) → sha.
- **R41-5 · Docs+ledger como proyecciones (staging).** status/09/05/register/índice/ledger derivados del
  capsule; §0 del ledger recomputado replicando el algoritmo del validador DIRECTAMENTE.
- **R41-6 · Validación + recheck + promoción crash-consistent.** Validador ledger + comparador semántico
  (artefacto hasheado con self-test rojo) + proyección-no-excede + secret/PII sobre TODO lo nuevo.
  Recheck pre-state. Promoción Fase A (temp+fsync+atomic-replace por archivo). Rollback global si falla.
- **R41-7 · Congelar + handoff + paquetes.** Terminal+journal congelados → `HANDOFF` (Fase B) →
  reproduction bundle + external packet minimizado (§7) → prueba de reconstrucción → root digest
  out-of-band. Delta-only: todo no-allowlist byte-idéntico al pre-state.
- **R41-8 · Cierre.** UN terminal de §9. `FROZEN_FOR_S5_*`, montaje, steps 5–14: prohibidos.

---

## 13. ESQUEMAS DE ARTEFACTOS (paths/modes/symlinks/case/Unicode/EOL)

Orden acíclico de hash: `PRE_STATE` → `PRIMARY_EVIDENCE_ARCHIVE` → reaudit outputs → `CAPSULE`(+json) →
docs/ledger staging → promoción(journal) → `HANDOFF` → reproduction/external. Ninguno contiene su propio
hash; ninguno upstream contiene hashes downstream.

- **Codificación (todos los manifests):** `sha256` de **bytes crudos**; NO git-blob-id; se declara por
  miembro: `path` (rutas POSIX `/`, normalización Unicode **NFC**, case-sensitive), `bytes`, `mode`
  (100644/100755), `symlink_target|null`, `eol` (LF|CRLF|MIXED|NONE), `sha256`. `.gitattributes` fija LF
  del ledger — el resello lo respeta.
- **PRE_STATE_MANIFEST.json** (`${OPS}`): `{run_id, ts_utc, branch, head, upstream_ab, entries:[…], generator, hash_alg}`.
- **CAPSULE** (`.md` legible + `.json` companion): `{gate_matrix, correction_manifest[12], claim_registry,
  semantic_inventory[hit→clase], ledger_findings[dedupe_probado, frozen_ids], counterexamples{formula_id,
  constants, cases[10-dec], rounding, two_independent_calcs}, provenance_incident, prestate_sha,
  restore_attestation}`. Estado SIEMPRE `{status, artifact_sha256, audit_run_id, scope, ts, reviewer}`.
- **TRANSACTION_JOURNAL.json** (`${OPS}`): `{run_id, phases:[{name, files:[{path, pre_sha|ABSENT,
  staged_sha, promoted_sha}], status, ts}], terminal}`.
- **HANDOFF** (repo, Fase B): hashes finales de miembros + blockers supervivientes + estados §9; SIN su
  propio hash.
- **OVERLAY/reproduction/external** (`${OPS}`): `head.txt` + `tracked.patch` (`git diff --binary HEAD`) +
  `untracked/` (bytes+modos+rutas) + `members.sha256` (de la membership congelada) + `reconstruct.(ps1|sh)`.

---

## 14. CEO ASSURANCE MATRIX (conservada de R4, sin cambio)

18 dominios con clasificación `CB/PM/PMe/PC/PR/RR` + owner, separando docs/impl/validación/evidencia
humana (R4 §12, `829537af…`). Cambios de fuerza: #17 «observabilidad sin PII» = `STATIC_SEARCH_ONLY` →
runtime audit PRE_RELEASE; #2/#3/#5/#6 (validez, test-retest, responsiveness, invariancia) = `DOCUMENTED`
≠ `VALIDATED`. Dominio nuevo #14 (flag/kill-switch/rollback/observabilidad/incident/device-matrix) =
`PRE_MOUNT`, precondición NUEVA de S6 (S-04). Sin cambio material; se cita, no se re-pega.

---

## 15. SELF-RED-TEAM DE LA R4.1 (15/15 completo)

1. **Reaudit del mismo sistema** → etiquetado `INTERNAL` con reviewer/limitación; la externa (§7-R4/§11)
   es la única que aprueba el ADR (eje `EXTERNAL_AUDIT` por scope).
2. **Capsule como canon paralelo** → rol EVIDENCIA; transiciones solo en ledger; proyección-no-excede
   verificada.
3. **Truth-table copiada de §R3-15 sin re-verificar** → REAUDIT exige 14/14 vs código (source+build+wiring).
4. **Fixtures de contraejemplo mal trascritos** → companion machine-readable + dos cálculos
   independientes (base A / base B) por EVIDENCE_ID; discrepancia ⇒ STOP.
5. **Operacional filtrado al packet** → `${OPS}` externo + membership congelada + denylist de operacionales
   en overlay/external; prueba de reconstrucción no incluye `${OPS}`.
6. **Backups sensibles empaquetados** → backups solo en `${OPS}`, nunca en ningún paquete; en repo solo la
   attestation no sensible.
7. **Promoción parcial por crash** → journal dos-fases + recovery + `PARTIAL_RECOVERY_REQUIRED` si el
   restore no se confirma byte-a-byte.
8. **Rollback borra trabajo ajeno** → CREATE-rollback solo si el path sigue siendo el creado por este
   run-id; si otro writer lo cambió, NO borrar → recovery manual.
9. **Overlay se auto-incluye** → membership congelada ANTES de crear el output; exclusión explícita de
   overlay/staging/lock/backups/journal/manifests autorreferentes.
10. **Lock huérfano bloquea para siempre** → takeover exige proceso-owner-muerto + decisión CEO; nunca TTL solo.
11. **RUN_ID reutilizado tras crash pre-journal** → registro durable EXTERNO de run-ids usados; virginidad
    no se infiere de «no hay journal».
12. **Dedupe fusiona invariantes distintos** → prueba de equivalencia por fila (invariante+owner+fix+gate+
    evidencia); en duda no fusionar; sin conteo esperado.
13. **«Validador verde» leído como «ledger veraz»** → validador+comparador = sintaxis/estructura; la verdad
    la sostienen EVIDENCE_IDs; el packet lo declara.
14. **Authority index se desactualiza / no es obligatorio** → discoverability declarada `PARTIAL` hasta que
    un entrypoint obligatorio lo cite (D-P7-B) + scanner-gate; toda decisión nueva en 09 debe tocar el índice.
15. **Este documento tratado como ejecutado, auto-autorizado o truncado de nuevo** → banner
    `EXECUTE_STEP_7=DENIED`; terminales §9 sin cambiar; completeness gate §16 (headings, 15/15, §CEO,
    §resumible, newline, hash post-relectura) corrido y reportado fuera del archivo.

---

## 16. COMPLETENESS GATE (anti-truncación, auto-verificado)

Corrige R4A-P0-14/P1-15. Tras escribir este archivo, se ejecuta (§17 lo confirma con output real):
(a) re-leer el archivo del MISMO path; (b) `sha256` + bytes + líneas; (c) presencia de los headings
obligatorios `## 1..## 18`; (d) self-red-team con **15** ítems numerados; (e) presencia de §17 (decisiones
CEO) y §18 (estado resumible); (f) **newline final** verificado leyendo el último byte == `0x0A` con
decodificación UTF-8 explícita (NUNCA `Get-Content -Tail`, que en PS 5.1 usa ANSI y produce mojibake
engañoso); (g) ninguna frase cortada al final. El `PLAN_SHA256` autorizable se reporta FUERA de este
archivo (respuesta al CEO), como `out-of-band record` — la `cryptographic attestation` (tag firmado) es
futura y con autorización.

---

## 17. DECISIONES CEO REALMENTE BLOQUEANTES

- **D-P7-A · Custodia.** Recomendación: **Opción D** (procedencia versionada + cuarentena; forense ya
  ejecutada, `NOT_FOUND_IN_LOCAL_GIT_OBJECT_DATABASE` ×3; register conservado como vivo indexado).
  Trade-off: cuesta un incidente + un índice; a cambio, cero autoridad stale legible y auditoría externa
  con hechos medidos. Alternativas A/B/C en R4 §7 con sus costes.
- **D-P7-B · Entrypoint de discoverability (NUEVA, obliga a corregir R4A-P0-13).** Qué entrypoint
  obligatorio referencia el `CURRENT_AUTHORITY_INDEX`: **(1) cabecera de `ADR_REGISTER_V5.md`** (mínimo
  intrusivo, ya se toca en custodia D) · (2) cabecera de `FOCUS_ENGINE_MASTER_PLAN_V5.md` (pero está en
  DENYLIST/bundle — requeriría excepción) · (3) sección Focus de `CLAUDE.md` (la más leída por cualquier
  sesión/agente, máxima discoverability, pero toca gobernanza global). Recomendación: **(1) + scanner-gate**;
  (3) como refuerzo si el CEO quiere discoverability máxima. Sin esta decisión, discoverability = `PARTIAL`.

*(No son decisiones: formato de autorización — fijado; ubicación durable de esta R4.1 — defaulted a
`docs/engines/focus_v5/`; supersesión del criterio export de T-S5 — ya cerrada por contrato CEO, solo se
registra; el reaudit por scopes vs nombre limitado — es mandato del plan con default honesto.)*

---

## 18. ESTADO RESUMIBLE + SIGUIENTE ACCIÓN EXACTA

- **Qué pasó (2026-07-22):** adjudicada la truncación (PROBADA de transferencia; original completo, §1) +
  producida R4.1 con las 14 correcciones P0 y 15 P1 del auditor R4A (§2). Conservado sin degradar todo lo
  bueno de R4 (64 hipótesis, S-01..S-05, custodia D, CEO assurance), corrigiendo fuerza epistémica
  (`NOT_FOUND_IN_LOCAL_GIT_OBJECT_DATABASE`, `HYPOTHESIS`, `VERIFIED_PRIMARY_NOT_DURABLY_CAPTURED`).
- **Cambios estructurales R4→R4.1:** operacional 100% fuera del repo · allowlist/denylist path-by-path ·
  promoción crash-consistent two-phase (no «atómica») · rollback de paths nuevos (ABSENT) · 3 paquetes con
  minimización · reaudit por scopes · state machine con VALIDATION-matriz + EXTERNAL_AUDIT-eje +
  EVIDENCE_STATE-runtime · authority index con discoverability ejecutable (D-P7-B) · completeness gate.
- **Estado:** nada ejecutado; canon/ledger/código intactos (git status idéntico al pre-shutdown, R4 §2).
  Único archivo escrito = éste. R4 conservada (`829537af…`). `EXECUTION`=n/a.
- **Siguiente acción exacta (no puede ser ejecutar R3 ni R4):** el CEO (1) hace la última auditoría de
  aceptación de ESTA R4.1; (2) decide **D-P7-A** (rec. D) y **D-P7-B** (rec. 1+scanner); (3) fija el hash
  de esta R4.1 (reportado fuera de banda, §16); (4) si acepta, emite
  `EJECUTAR_PASO7_R4 PLAN_SHA256=<hash-de-este-archivo> CUSTODY_DECISION=<A|B|C|D> RUN_ID=<uuid-no-usado>`.
  La ejecución sigue R41-0→R41-8 y termina en UN terminal de §9; después, packet a auditoría externa POR
  SCOPES. `FROZEN_FOR_S5_*`, montaje y steps 5–14 siguen prohibidos.
