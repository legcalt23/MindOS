# BASELINE_EVIDENCE_V5 — Freeze de autoridad y estado real del repositorio (R0)

```
DOCUMENT_STATUS = DRAFT (candidato V5; no promovido)
EXECUTION_AUTHORIZATION = NOT_GRANTED
FECHA = 2026-07-19
BASE_SNAPSHOT = 27849450345ef57f96b02f1900afe1251b456292 (branch product/mindboost-wip-consolidation-20260717)
METODO = solo inspección read-only; CERO mutaciones del repo; Gradle NOT_RUN (directiva vigente)
HASHES = ver INPUTS_SHA256.txt (SHA-256 completo de los 34 inputs; rutas relativas)
```

## 1. Worktree (antes — se re-verificará idéntico al cierre)

```
# branch.oid 27849450345ef57f96b02f1900afe1251b456292
# branch.head product/mindboost-wip-consolidation-20260717
? .claude/skills/mindboost-exercise-gate/
? .claude/skills/molvex-design-language/
? .claude/skills/ui-ux-pro-max/
```

Sin submódulos (`git submodule status` vacío). Los 3 untracked son preexistentes a esta tarea y no se tocan.

## 2. Adjudicación de canon: 2.0.0 vs 2.0.2

**VEREDICTO: el canon que existe realmente es 2.0.2**, en `docs/context_pack/` (11 archivos), commiteado en
`81e263c9` ("docs(repo): establish canonical context pack v2.0.2") y contenido en HEAD. VERIFIED (git + lectura
directa de `00_CONTEXT_INDEX.md` que declara `packRelease: 2.0.2`, y `09_DECISION_LOG.md` §21).

**2.0.0 NO es byte-recuperable desde el repo**: `git log --all -- docs/context-pack` (hyphen) devuelve 0 commits
y `-- docs/context_pack` (underscore) devuelve solo `81e263c9`. El snapshot 2.0.0 quedó
`SUPERSEDED_SNAPSHOT_DO_NOT_USE` FUERA de HEAD (quarantine) por adjudicación D-H1 (ChatGPT), registrada en
`00 §8.1` y `09 §21`, con el linaje completo en el ledger `MB-ENG-0006` (CLOSED, evidencia VERIFIED).

**Diff de autoridad 2.0.0 → 2.0.2** (clasificación honesta):
- Existencia y adjudicación: VERIFIED (git + ledger).
- Contenido del delta: REPORTED — declarado en `00 §8.1`/`09 §21`: reconciliación de canon; correcciones ya
  presentes en `03`, `05` y `09`; corrección de atribución PMID S6R3-006 (35953659 = Nicosia — latencia BYOD;
  33954913 = Passell — variación por dispositivo; "verificado en PubMed 2026-07-17" según ledger). El detalle
  línea a línea del delta NO es reproducible aquí porque 2.0.0 no está en git → NOT_VERIFIED byte a byte.
- Consecuencia para el research pack: el pack se ancló a 2.0.0 ⇒ toda cita del pack al canon es REPORTED contra
  un snapshot no verificable. Dirección del sesgo conservadora (las correcciones 2.0.2 ENDURECEN atribuciones),
  pero el caveat se declara en cada gate científico (era C-02 en V4; se conserva como CAVEAT-CANON en V5).
- **NO se presupone que 2.0.2 sustituye a 2.0.0 por costumbre: se verifica** — la sustitución está registrada
  explícitamente como decisión (`00 §8.1` estado del hyphen = SUPERSEDED_SNAPSHOT_DO_NOT_USE; regla `00 §3`:
  "una decisión posterior solo sustituye a otra si queda registrada explícitamente"). Cumplida.

## 3. Fuentes: existencia, versión y hash

| Fuente | ¿Existe? | Versión | SHA-256 (completo en INPUTS_SHA256.txt) |
|---|---|---|---|
| `00_CONTEXT_INDEX.md` … `09_DECISION_LOG.md` | SÍ — `docs/context_pack/` | packRelease 2.0.2 | 11 hashes registrados |
| Research pack (18) | SÍ — zip `FOCUS_ENGINE_RESEARCH_PACK.zip` | anclado a 2.0.0 | `5fc8e617ef455ea1…0cdb` (= PACK_SHA256 de V4) |
| `11_REQUIREMENTS_TRACEABILITY_MATRIX.csv` (18) | SÍ — dentro del pack (16 archivos extraídos) | — | registrado |
| Bundle V4 (11–17) | SÍ — 7/7 SHA-256 **idénticos a los del mandato CEO** | H3 | los 7 del mandato, re-computados y coincidentes |

Reconciliación mecánica exigida por el mandato: **hecha**. Los 7 SHA-256 del bundle recibido por el CEO
coinciden byte a byte con las copias de `Downloads/FOCUS_ENGINE_MASTER_PLAN_V4_H3/`. Confirmado también lo
denunciado: el plan V4 conserva la referencia interna obsoleta `91D776BC` para A2 (P3, se corrige en V5),
`hash_h3.cjs` usa ruta absoluta Windows (P1-3, sustituido por manifiesto SHA-256 portable en V5), y CRC32 no
sustituye a SHA-256 (V5 usa SHA-256 en todo).

## 4. Fingerprint de toolchain (VERIFIED, `gradle/libs.versions.toml` + `iosApp/Podfile`)

| Eje | Valor |
|---|---|
| Kotlin | 2.3.10 |
| AGP | 9.1.0 |
| Compose Multiplatform | 1.10.3 |
| compileSdk / targetSdk | 36 / 36 |
| **minSdk** | **29** |
| iOS deployment target | **15.0** (`iosApp/Podfile: platform :ios, '15.0'`) |
| Tests JVM de :shared | `desktopTest` (fuente: CLAUDE.md §3, tabla de tareas reales) |

**CONTRADICTED (hallazgo de baseline):** `09_DECISION_LOG.md` **TEC-002 "Android 8/API26 e iOS15 como
mínimos" = APPROVED**, pero el repo compila con `minSdk = 29`. Por precedencia (canon > código; "el código no
se convierte en requisito solo porque ya exista", `00 §3`), la decisión vigente exige API 26 y el repo la
incumple. Disposición en V5: ítem de S5 (paridad/plataforma) con dos salidas posibles — (a) bajar `minSdk` a 26
(slice técnico con matriz de compatibilidad y coste de QA en dispositivos antiguos) o (b) el CEO registra
`SUPERSEDED` de TEC-002 a API 29. Hasta esa decisión, la estrategia de timing de V5 se diseña para API 26+
(TEC-002 vigente) y se declara el gap. iOS 15 SÍ está alineado (Podfile = 15.0).

## 5. focus2 y candidato rechazado

- `focus2` **NO existe en HEAD** (ls del árbol focus2 = ausente). VERIFIED.
- Existe SOLO en el commit rechazado `89325271` ("feat(focus2): add foundational authority contracts" —
  FocusExerciseIdentity, FocusDegradation, FocusEvidenceCap, FocusExerciseMaturity, FocusMeasurementPurpose).
  `git show --stat` VERIFIED. Sigue siendo evidencia intacta de lo rechazado: NO base, NO autoridad, NO fuente
  reutilizable por defecto; solo lectura conceptual citada como tal.

## 6. Inspección P0: score Focus v1 manipulable — VEREDICTO

**P0 OPERATIVO CONFIRMADO** (inspección fresca en esta sesión, HEAD 27849450):

1. **Visible al usuario: SÍ.**
   - El 0–100 de `PersonalizedSessionScorer` (consume `MetricKey.LAPSES` con umbral PVT `lapseRtMs=500` sobre
     flanker + `RT_CV`) se renderiza en `FocusSummaryScreen.kt:94-96` — file:line VERIFIED por el ledger
     `MB-ENG-0007` (P0, OPEN), verificado ahí directamente por la sesión primaria en su día.
   - El `FocusScore` compuesto se renderiza en `FocusProgressScreen` (grep fresco: montada en
     `FocusRootScreen.kt:107-110` vía `rememberFocusProgressStateHolder`). VERIFIED hoy.
2. **Alimenta perfil: NO (fail-closed).** `FocusProfileSource.kt:55-61` (leído hoy): gate D1 deniega SIEMPRE
   (`SessionMeasurementPurpose.UNKNOWN, lineageComplete=false` → `measurementEligible=false` → `return null`)
   ⇒ ATTENTION permanece CALIBRATING. Además S6R3-008: pin `preliminary=true` hardcodeado + test invariante.
3. **Alimenta claims: NO** hay claims públicos derivados (no hay release; RELEASE_EVIDENCE=NO_GO).
4. **Segunda puerta activa (MB-ENG-0008, P0 OPEN):** la ruta arquetipo (`ArchetypeInference` consume RT_CV/LAPSES,
   `faScore` doble-ponderado) fija la dificultad inicial de la SIGUIENTE sesión (`FocusSessionStateHolder.kt:198`)
   y gatea input (`MicroPauseGate`), puenteando la política `FIXED`. Ledger VERIFIED.

**Clasificación: P0 operativo** (el mandato lo condicionaba a "sigue visible" — lo está). La especificación de
ocultación reversible por el mecanismo existente (FeatureFlags) vive en la slice S0 del plan V5.
**No se implementó nada en esta tarea.**

## 7. Gates baseline: comando → estado

Directiva de entorno vigente (CLAUDE.md §4.1 + memoria persistida): Android Studio posee el daemon Gradle de
este worktree ⇒ **prohibido invocar Gradle CLI desde la sesión**; se verifica estáticamente y los comandos se
entregan al CEO. Cumpliendo el mandato ("si un comando no puede ejecutarse, registra comando, error, impacto y
gate bloqueado; no inventes resultados"):

| Comando | Estado | Impacto |
|---|---|---|
| `.\gradlew.bat :shared:desktopTest` | NOT_RUN (daemon del IDE) | Baseline de tests JVM no reproducido en esta sesión |
| `.\gradlew.bat :shared:detekt` | NOT_RUN (ídem) | Baseline estático no reproducido |
| `.\gradlew.bat :shared:compileKotlinDesktop` | NOT_RUN (ídem) | Compilación desktop no verificada hoy |
| `.\gradlew.bat :shared:compileAndroidMain` / `:androidApp:compileDebugKotlin` | NOT_RUN (ídem) | Compilación Android no verificada hoy |
| `:shared:compileKotlinIosSimulatorArm64` / `IosArm64` | NOT_RUN (imposible en Windows: targets `if (isMacOs)`) | iOS = BLOCKED_IOS |

Nota de honestidad: el ledger (fila `integration-baseline`) registra que en un HEAD ANTERIOR (`71e17038`) la
suite completa `desktopTest` tenía 17 fallos y `detekt` 6 findings, ATRIBUIDOS a WIP ajeno concurrente pero NO
probados independientemente (`UNATTRIBUTED_INTEGRATION_FAILURE`). El HEAD actual (27849450, consolidación
posterior) NO tiene baseline verde demostrado. **R0 del plan V5 exige que el CEO ejecute estos comandos y aporte
outputs antes de S1** — es el primer exit-gate y no puede darse por bueno por documentación.

## 8. Hardware/CI existente para iOS y Android (sin contratar nada)

- `.github/workflows/ios-build.yml` (VERIFIED, leído): job `ios-compile` en `macos-14` que compila
  `:shared:compileKotlinIosSimulatorArm64` + `:shared:compileKotlinIosArm64` (drift-detection; "iOS targets solo
  declarados `if (isMacOs)`"). Triggers: push/PR a main con paths de shared/gradle + `workflow_dispatch`.
  **Evidencia de ejecución: NINGUNA verificada** (el flujo de trabajo actual del CEO no usa GitHub como remoto
  activo). Es la vía 0 € para pasar iOS de BLOCKED_IOS a verde COMPILE-level; NO cubre tests iOS, UI ni timing
  de dispositivo (eso exige Mac físico o simulador interactivo).
- Otras lanes: `kotlin-ci.yml`, `android-instrumented.yml`, `backend-ci.yml`, `ci.yml`, `security.yml`,
  `release.yml`, `command-router-voice.yml` (existencia VERIFIED; ejecución NOT_VERIFIED).
- Android local: SDK en esta máquina (builds vía IDE del CEO). iOS local: NO hay Mac en este host (Windows).

## 9. Protocolo `/mb` y restricciones de agentes (VERIFIED)

`.claude/commands/mb.md` leído: la sesión principal ES el orquestador (los subagentes no lanzan subagentes —
GOV-004/GOV-005 del Decision Log). El routing del mandato CEO (7 agentes exactos) es un subconjunto compatible
de la tabla de `/mb`; los agentes son read-only en esta tarea y no escriben el plan final.

## 10. Ledger de auditoría (obligación protocolaria)

`MINDBOOST_AUDIT_LEDGER.md` LEÍDO al inicio (filas MB-ENG-0002…0008, 6R3-EVID-P2-01, stale-exit-reviews,
integration-baseline). Su ACTUALIZACIÓN es una mutación del repo ⇒ prohibida por este mandato ("worktree
idéntico") ⇒ queda **PENDING_BY_OWNER_DIRECTIVE**, registrada aquí y en el cierre. El plan V5 incluye la
actualización del ledger como primera acción de la fase autorizada que sí pueda escribir.

## 11. Estado de lectura de las 18 fuentes (límites de evidencia honestos)

| Fuente | Estado de lectura en esta sesión |
|---|---|
| 00 índice | LEÍDO ÍNTEGRO (sesión principal) |
| 09 Decision Log | LEÍDO ÍNTEGRO (sesión principal) |
| 01 PRD, 03 Charter, 04 Standard, 05 Technical, 06 AI-OS, 08 Alpha/Beta | LEÍDOS por agentes de orden 1 en su carril (delegación con cita obligatoria doc§); sesión principal: lectura dirigida |
| 02 CEO Copilot Charter, 07 Phase Review | Lectura dirigida (gobiernan a ChatGPT/cierre de fase; citados como REPORTED donde se usen) |
| 11–17 (bundle V4 propio) | CONOCIDOS ÍNTEGROS (autoría propia, re-hashados hoy) |
| 18 research pack | Extraído y verificado; docs leídos por agentes por carril (02/03/04/05/06/08/09 del pack) + lecturas previas de la sesión V4 |

Ninguna afirmación del plan V5 se apoya en una fuente no leída por la sesión o por su agente citado.
