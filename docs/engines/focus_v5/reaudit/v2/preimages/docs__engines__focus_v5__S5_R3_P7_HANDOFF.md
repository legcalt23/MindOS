# S5_R3_P7_HANDOFF — Focus V5 · S5 · R3 · entrega a revisión (Paso 7 + REAUDIT 2026-07-22)

```
ROLE: HANDOFF de la R3 CANDIDATA a la siguiente revisión (CEO / revisor externo humano si lo hay)
GENERATED_BY: TASK-2026-07-22-FOCUS-V5-S5-R3-REAUDIT · describe el árbol en HEAD 8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5
REVIEW_CLASS: AI_ASSISTED_INDEPENDENT_REVIEW  (NO es EXTERNAL_AUDIT humano/institucional — ninguno ha ocurrido)
CEILING: R3 CANDIDATA (NO FROZEN_FOR_S5_R3) · MOUNT BLOCKED · code UNTOUCHED IN THIS TASK · NO congela/monta
PACKAGE PROVENANCE: los artefactos R3 están UNTRACKED (working-tree), NO en el commit 8a06b9a — ver §0.
```

> **Corrección de esta reaudit (2026-07-22).** Este handoff reemplaza la versión de Paso 7, que contenía cinco
> defectos de honestidad de empaquetado que una reaudit adversarial de 7 lentes + reproducción independiente
> confirmó: (1) clase de revisión mal etiquetada como `EXTERNAL_AUDIT`; (2) el paquete R3 es UNTRACKED y no
> reproducible desde el HEAD que citaba; (3) hashes truncados a 16-hex sin manifest de cierre; (4) sello del
> ledger fuera de banda; (5) «security APTO» contradicho por su propia lente de privacidad. Todos corregidos
> abajo. El redline vive en `reaudit/S5_R3_REAUDIT_REDLINE.md`; el informe maestro en
> `reaudit/S5_R3_REAUDIT_MASTER_REPORT.md`; los veredictos multidimensionales en
> `reaudit/S5_R3_MULTIDIMENSIONAL_VERDICTS.md`.

---

## 0. Provenance del paquete (leer primero) — UNTRACKED, reproducible por MANIFEST, no por checkout

Todos los artefactos R3 (este handoff, índice, cápsula, provenance, ADR, scanner, evidencia de reaudit) están
**UNTRACKED** en el working-tree: NO forman parte del commit `8a06b9a`. Un `git checkout 8a06b9a` limpio NO los
contiene. Por tanto el paquete **no es reproducible por checkout**; su ancla de integridad es el manifest de
bytes del working-tree: **`reaudit/S5_R3_EXTERNAL_MANIFEST.{json,md}`**, que lista para CADA miembro (incluido
este handoff y el ledger) su **SHA-256 completo (64 hex)**, bytes, tipo de EOL y último byte, y un
`package_root_sha256`. Regenerar/verificar: `node docs/engines/focus_v5/reaudit/build_external_manifest.mjs`.
El hash `§0` self-excluding del ledger ya NO se entrega fuera de banda: el manifest hashea el fichero completo
`MINDBOOST_AUDIT_LEDGER.md`. **Decisión CEO pendiente:** commitear el paquete R3 (hoy prohibido por el mandato)
para darle provenance de git real.

## 1. Lo que domina: la falsación de la R2 (REPRODUCED + CORROBORATED_INDEPENDENTLY)

El gate de elegibilidad temporal R2 §6.1 (`S_full`-o-`Absent` con `monotonicGuard mg-1`) es **no-monótono** bajo
el scorer real. Contraejemplo (30H/10M/10FA/30CR; loglinear Hautus +0.5/+1.0; noise F=0.256098):

| Escenario | d′ (scorer real, 6 dp) | Gate R2 |
|---|---|---|
| `S_full` (todas las ventanas) | 1.310847 | referencia |
| `D1` (excluir 5 misses) | 1.680016 | **Absent** (1.680016 > 1.310847) |
| `D2` (excluir 5 misses + 15 hits) | 1.292908 | **Present** (1.292908 ≤ 1.310847; margen 0.017939) |

**Más exclusión técnica (D1→D2) ABRE el gate que menos exclusión cerraba.** El harness reproducible
`reaudit/r2_falsification_harness.mjs` (digest `2df300f3e80f361e62eabfb97176bc04dd1e96acb44978a4052a6c839df27452`)
lo verifica con **3 probits de 2 familias conceptuales** (Acklam=repo, Wichura-AS241, bisección sobre erf) que
coinciden a **1.3e-9**, en **2 bases** (30/10/10/30 y 60/20/20/60), y demuestra que el flip es **GENÉRICO: 100%
de 1363 bases factibles**, no dos casos a mano. `CI-lower(S_full)>0` hace la elegibilidad dependiente del
rendimiento (sesgo de selección). **Mecanismo retirado como gate** (D-R3-01: elegibilidad OUTCOME-INDEPENDENT).

**Alcanzabilidad EN CÓDIGO (corrección de la reaudit):** el gate R2 (`monotonicGuard`/`S_full`/`mg-1`,
recomputar d′ bajo exclusión anidada) existe **SOLO en docs**: grep de `*.kt` = **0 hits**, y no hay
recomputación-de-d′-bajo-exclusión en el código focus2. El código VIVO (`FocusTemporalEligibility.kt`) ya
implementa la forma R3 (`E=g(excludedFraction)`, techo one-sided, monótona por construcción). Por tanto
`R2_COUNTEREXAMPLE_ARITHMETIC=CONFIRMED` y `R2_REACHABILITY_IN_CODE=NOT_APPLICABLE`: la falsación refuta un
**diseño**, nunca un camino de código vivo. Esto corrige el marco previo de `MB-FOCUS-0047` («retiro en código
PENDING»): no hay gate no-monótono en código que retirar (§4).

## 2. Honestidad (lo que NO se afirma)

- **Clase de revisión = `AI_ASSISTED_INDEPENDENT_REVIEW`.** La falsación R2 y esta reaudit las hizo Claude, no
  un auditor humano/institucional. Ningún `EXTERNAL_AUDIT` humano ha ocurrido (`NOT_OBTAINED`). El token de
  estado `REOPENED_AFTER_EXTERNAL_AUDIT` se conserva como historia; léase como `..._INDEPENDENT_REVIEW`.
- **«Gate 6/6 + 7/7 APTO» = `MISSING_PRIMARY_EVIDENCE`.** El tracker de lentes de Paso 7 fue efímero. La reaudit
  SÍ persiste sus 7 lentes en `reaudit/S5_R3_LENS_OUTPUTS.json`, y **5 de 7 devolvieron FAIL** (psicometría,
  arquitectura, privacidad, provenance, a11y) — ver §5. La R3 se entrega CANDIDATA, no APTA.
- **«security APTO» RETIRADO.** Se degrada a `DOCUMENTATION_SECURITY_REVIEW_ONLY`: la lente de privacidad halló
  GAP-1 (revocación no cableada en el write-ahead de focus2, **sin test de regresión**), GAP-2 (borrado
  cross-account colateral reintroducido), y un consent-gate del journal sin call-site. No es un veredicto de
  seguridad defendible (§5, findings L5-01/02/03).
- **iOS = `BLOCKED_IOS` · Físico/device-matrix = `NOT_RUN` · builds/tests = `NOT_RUN`** (reaudit doc-only).
- **Techo = R3 CANDIDATA.** NO montado (`focus2Enabled` no existe; `CONTRACT_INERT_NO_WIRING`, verificado: 0
  refs externas a `focus2.*`). V0–V8 `DESIGNED_NOT_RUN`; ningún claim > C0.

## 3. Deliverables — hashes completos en el MANIFEST (no truncar aquí)

El cierre de integridad es `reaudit/S5_R3_EXTERNAL_MANIFEST.{json,md}` (SHA-256 de 64 hex + bytes + EOL + último
byte por miembro, incluido este handoff y `MINDBOOST_AUDIT_LEDGER.md`, más `package_root_sha256`). Miembros del
paquete R3: los docs de autoridad (índice, provenance, cápsula .md/.json, ADR contrato, ADR register, status),
este handoff, `scripts/validate-authority-entrypoints.mjs`, el ledger, y la evidencia de reaudit
(`reaudit/*`: harness + result + summary, informe maestro, findings JSON, veredictos, spec de elegibilidad,
data-flow/threat-model, blockers, command-log, lens-outputs, redline). Los planes de PROCESO `S5_R4_*` quedan
**EXCLUIDOS** (rutas absolutas con username; higiene de host).

## 4. Blockers vivos (ninguno cerrado por esta reaudit — solo docs/tooling/ledger)

- **R3 pendiente de aprobación** por la autoridad que promueve CANDIDATA→aprobada (CEO; revisor externo humano
  si el CEO lo dispone). Esta reaudit NO es esa autoridad.
- **`MB-FOCUS-0047` OPEN (reencuadrado):** el gate no-monótono es **diseño-only** (0 hits en `.kt`); el código
  ya es R3-monótono. Pendiente = sellar R2-SUPERSEDED en ADR versionado + garantizar que el wiring (Paso 4) no
  reintroduce `S_adm≤S_full`. NO es «retirar código».
- **`MB-FOCUS-0048` OPEN:** inflación de estado R2 + wiring PENDING (marker→proxy, revocación GAP-1/GAP-2,
  ONSET_INTEGRITY no persistido, `seal()` con 5 params temporales opcionales, `validate()` de tallies ausente,
  thresholds/versiones/tertiles/retención).
- **Nuevos de la reaudit (OPEN, owner=fase futura):** `MB-FOCUS-0049` (revisión mal-etiquetada EXTERNAL_AUDIT),
  `MB-FOCUS-0050` (cadena de autoridad UNTRACKED), `MB-FOCUS-0051` (a11y: el lector de pantalla anuncia la
  respuesta correcta → accuracy NO es modality-fair, `FocusTracerScreen.kt:137`), `MB-FOCUS-0052` (contrato R3
  sin política de missingness/MNAR para `excludedFraction`), `MB-FOCUS-0053` (`differentialExclusionDelta` =
  constante huérfana, detector de exclusión-diferencial de R2 dejado muerto).
- **MOUNT BLOCKED:** `MB-FOCUS-0026/0028/0035/0042` (0035 single-instance: violarlo corrompe el write-ahead).
- **iOS `BLOCKED_IOS`; físico `NOT_RUN`. Precondiciones de wiring W1–W5** (sellar `SessionEligibility`, purga
  sign-out, decisión `TRAINING_FREE`, registro ejercicio∈bloque, `CalibrationBlueprint`+equating).

## 5. Reaudit de 7 lentes (persistida) — resultado honesto

`reaudit/S5_R3_LENS_OUTPUTS.json` (raw) + `S5_R3_MULTIDIMENSIONAL_VERDICTS.md`. Verdicts por lente: ciencia/
eficacia **PASS_WITH_LIMITATIONS**; timing/KMP **PASS_WITH_LIMITATIONS**; psicometría/missingness **FAIL** (sin
política MNAR; constante huérfana); arquitectura/recovery **FAIL** (single-instance no forzado corrompe
write-ahead; drain sin consumir el outcome tipado); privacidad **FAIL** (GAP-1 sin test; security-APTO
contradicho); provenance **FAIL** (chain UNTRACKED; review-class; closure); a11y **FAIL** (el lector anuncia la
respuesta; sin Dynamic-Type). **Nota de integración honesta:** el integrador del workflow recibió por error solo
2 de 7 lentes (bug de truncado del orquestador, 24 KB); la integración cross-lens se hizo a mano en el informe
maestro sobre las 7 salidas. Ninguna lente ejecutó Gradle/tests (read-only): todo claim de comportamiento es
`STATICALLY_VERIFIED`/`INFERENCE`, nunca ejecución.

## 6. Qué debe verificar la revisión

(1) `node reaudit/r2_falsification_harness.mjs` y comparar con §1 (su propio probit). (2)
`node reaudit/build_external_manifest.mjs` y comparar `package_root_sha256` + cada miembro (bytes+EOL). (3)
`node scripts/validate-audit-ledger.mjs` (VERDE) y `node scripts/validate-authority-entrypoints.mjs --self-test`
+ real. (4) Que ningún doc afirme `FROZEN_FOR_S5_R3`, «montado», «7/7 APTO» ni `EXTERNAL_AUDIT` como hecho. (5)
Que `MB-FOCUS-0047..0053` están OPEN y la capa es `CONTRACT_INERT_NO_WIRING`. (6) Adjudicar si la R3
(OUTCOME-INDEPENDENT, D-R3-01..04) es aceptable, **sabiendo que 5/7 lentes = FAIL con blockers de wiring
abiertos**. Solo entonces el cierre real de S5 (versionar ADR/register, re-sellar bundle) es una invocación
separada con su propio `T-DR`.
