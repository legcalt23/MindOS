# S5 · Paso 4 — Shadow eligibility: formal property, truth table, mutation adequacy

> **Veredicto:** `S5_PASO_4 = IMPLEMENTED_BUT_NOT_CLOSED`. El trabajo PROPIO de Paso 4 (propiedad formal,
> adecuación de mutación, tabla de verdad, mitades discriminantes, barrera no-wiring, suites verdes) está
> COMPLETO y verificado por bytes. NO se cierra por dos bloqueos **fuera del alcance de Paso 4**: (1)
> `MODULE_DETEKT = FAIL_PREEXISTING` — 4 findings en 2 archivos iosMain committeados en HEAD, fuera del
> baseline, del workstream del port iOS, que NO debo tocar; (2) `CROSS_PLATFORM_PROPERTY_PARITY = NOT_RUN` —
> el núcleo K/N no se puede compilar/ejecutar en Windows. NO promueve R3 (sigue `CANDIDATE / UNMOUNTED`). NO
> commit / wiring / mount. Todo Gradle corrió en un worktree aislado (`--project-cache-dir`, daemon dedicado),
> nunca en el árbol del IDE; ambos SUTs verificados `IDENTICAL` tras cada mutante.

SHA-256 de los dos tests de Paso 4 (post-fix detekt):
- `commonTest/.../FocusTemporalEligibilityProperty.kt` — `778fb94fba3a064205d4a283d64c1a281e0b9bbec3356bced9602ae91c11b138`
- `desktopTest/.../FocusTemporalEligibilityAdversarialOracleTest.kt` — `f8936632a5217287013de0168c4cb29329f56f7d3d1cb67e891e2393b491ac90`

SUTs (producción, sin cambios — hashes congelados en Fase A):
- `FocusTemporalEligibility.kt` — `63e8a49476f2331ab8b869859ead2227a161a9889c8f6c208318b64a862328fd`
- `FocusEvidenceStatePolicy.kt` — `33bfcf9d98603a7122f485c96db8549db3c35764e34c70e193581913621b53c1`

Tests:
- `commonTest/.../focus2/simulation/FocusTemporalEligibilityProperty.kt` (NUEVO — núcleo puro, K/N-portable)
- `desktopTest/.../focus2/simulation/FocusTemporalEligibilityAdversarialOracleTest.kt` (REESCRITO — 8 → 15 tests)

---

## Fase B — especificación de la propiedad (aislada a un solo eje)

**Proyección no-exclusión** (todo lo que un evaluación tiene EXCEPTO cuánto excluyó):

```
nonExclusionState(e) = (
  e.sessionClass,          // autoridad temporal (T0/T1)
  e.coverageComplete,      // completitud de cobertura
  e.windowsClassified,     // denominador de ventanas observadas
  thresholds.sessionExcludedMax,   // configuración de umbral
  thresholds.version               // versión de configuración
)
```

**Relación de comparabilidad** (el orden sobre el que se define la monotonía):

```
comparableByExclusion(a, b) ⟺
    nonExclusionState(a) == nonExclusionState(b)          // mismo estado en TODO lo no-exclusión
  ∧ exclusionRank(a) ≤ exclusionRank(b)                   // a excluyó ≤ que b
exclusionRank(e) = e.excludedFraction ?: 1.0              // una fracción no derivable = máximo fail-closed
```

**Propiedad mínima (monotonía):**

```
comparableByExclusion(a, b) ∧ isEligible(b)  ⟹  isEligible(a)
```

Equivalente operacional: una vez que la elegibilidad deja de ser `true` por AUMENTAR exclusivamente la
exclusión, no puede volver a `true`. **No se combina** un aumento de exclusión con una mejora de cobertura:
`nonExclusionState` se fija constante dentro de cada cadena (aserción `states.size == 1`), así que cobertura,
autoridad y denominador no pueden moverse detrás de la invariante. Cobertura y precedencia de causas se
prueban por separado (Fase D).

Dominio generado: familias de cadenas `chain(n) = { evaluate(List(n){ i<k ? excluded : clean }) : k = 0..n }`
para `n = 2..6`. Dentro de una cadena, cada ventana RESUELVE (entra en `tallies` y en su celda temporal), de
modo que `windowsClassified = n` y `coverageComplete = true` son constantes; sólo cambia la partición
clean/excluded, luego `excludedFraction = k/n` es el ÚNICO eje que varía. `sessionClass = T1` constante
(`n·60 ≥ 120` frames, capacidad/reloj ANDROID). El PRNG no interviene (no hay aleatoriedad; dominio
enumerado exhaustivamente).

### Reporte de dominio / cardinalidad (enumeración exhaustiva, sin semillas)

| Métrica | Valor |
|---|---|
| Cadenas (`n = 2..6`) | 5 |
| Evaluaciones en la propiedad de monotonía | 25 |
| Observaciones `Eligible` | 14 |
| Observaciones `Excluded` | 11 |
| Pares comparables ejecutados (`Σ C(n+2,2)`) | 80 |
| Pares descartados | 0 (todo par `i≤j` dentro de una cadena es comparable por construcción) |
| Re-aperturas detectadas | 0 (invariante) |
| Codominio+causas: evaluaciones | 53 (2 caps × Σ(n+1) + 3 explícitas) |
| Mitad honesta (Fase E) | 10 (5 `n` × 2 (respuesta,RT)) — todas `Eligible` |
| Mitad adversaria (Fase E) | 6 ataques etiquetados — todos NO `Eligible` |
| Barrido de evidencia (`OFFICIAL_ELIGIBLE` inconstruible) | 32 (2×2×2×4) |
| No-interferencia: pares | 25 |

Counts por causa se ejercitan todos en el test de codominio (`{NON_AUTHORITATIVE_CLASS, COVERAGE_INCOMPLETE,
SESSION_BACKSTOP_EXCEEDED}`) y exactamente en la tabla de verdad (Fase D).

---

## Fase C — umbral 0.50 realmente pinneado

`TemporalQualityThresholds.DEFAULT.sessionExcludedMax` se pinnea DIRECTAMENTE a `0.50`
(`assertEquals(0.50, …, 0.0)`), no sólo por su efecto.

Resolución de brackets: los conteos de ventana son enteros ⇒ las fracciones son racionales `k/den`. El límite
exacto `0.50` es representable (`2/4`); los vecinos alcanzables lo bracketean.

| Caso | Fracción (racional) | Umbral | Resultado esperado | Test |
|---|---|---|---|---|
| Constante por defecto | — | — | `== 0.50` exacto | `defaultBackstopMax_isPinnedToExactlyHalf` |
| Exactamente 0.50 | `2/4` | 0.50 | `Eligible` (comparador estricto `>`) | `backstopBoundary_isStrict…` |
| Inmediatamente inferior | `3/7 ≈ 0.4286` | 0.50 | `Eligible` | `backstopBoundary_isStrict…` |
| Inmediatamente superior | `3/5 = 0.60` | 0.50 | `Excluded{BACKSTOP}` | `backstopBoundary_isStrict…` |
| In-band | `4/7 ≈ 0.571` | 0.50 | `Excluded` (pincha el default dentro de (0.50,0.60)) | `inBandFraction_tripsTheDefaultBackstop…` |
| Umbral inyectado 0.66, `3/5=0.60` | 0.60 | 0.66 | `Eligible` (lee el max configurado) | `evaluateReadsTheConfiguredBackstopMax…` |
| Umbral inyectado 0.66, `2/3=0.667` | 0.667 | 0.66 | `Excluded` | `evaluateReadsTheConfiguredBackstopMax…` |

**NaN / ±Inf / −0 / denominador 0 — barrera de construcción:** el numerador y el denominador son enteros
no-negativos y la SUT guarda `denominator > 0`, de modo que toda fracción derivable es finita en `[0,1]`
(`f == f`, `f in 0.0..1.0`). El único `null` es el caso de celdas vacías (`NotCaptured`), nunca un `0.0`
fabricado; una fracción `null` en la rama post-`NotCaptured` (denominador 0 con celdas no vacías) es
**inalcanzable por el reducer** (todo evento de ventana añade ≥1 a `windowsClassified` o a `discards`), y el
test `excludedFraction_isAlwaysFiniteInUnitInterval_nullOnlyWhenNoWindowsObserved` lo demuestra sobre todo el
dominio (`null` sólo con `n==0`).

---

## Fase D — tabla de verdad (8 combinaciones de causa) — ambos SUTs

Contrato de precedencia: el veredicto reporta la **UNIÓN** de causas disparadas (un `Set` vía `buildSet`), NO
una escalera de prioridad — reordenar los `if` no puede cambiar un `Set` (ver mutante M12, equivalente). La
tabla pincha verdict, el `Set` EXACTO de causas, `rtShadowAdmissible` y el estado de evidencia.

`evidenceOf` demuestra que el eje temporal degrada SÓLO por el backstop (fracción), nunca por
`NON_AUTH`/`INCOMPLETE`: una sesión no-autoritativa o de cobertura incompleta pero bajo el backstop sigue
siendo `PROVISIONAL`.

| # | Caso | verdicts / cap | sessionClass | cobertura | fracción | verdict causas | rtShadowAdm. | evidence |
|---|---|---|---|---|---|---|---|---|
| 1 | none | `[C,C]` / AUTH | T1 | completa | 0.0 | `Eligible` | true | PROVISIONAL |
| 2 | nonAuth | `[C,C]` / NONE | T0 | completa | 0.0 | `{NON_AUTH}` | false | PROVISIONAL |
| 3 | incomplete | `[C,C,C,N]` / AUTH | T1 | incompleta | 0.0 | `{COVERAGE_INCOMPLETE}` | false | PROVISIONAL |
| 4 | backstop | `[C,X,X,X]` / AUTH | T1 | completa | 0.75 | `{BACKSTOP}` | false | TRAINING_ONLY |
| 5 | nonAuth+incomplete | `[C,C,C,N]` / NONE | T0 | incompleta | 0.0 | `{NON_AUTH,COVERAGE}` | false | PROVISIONAL |
| 6 | nonAuth+backstop | `[C,X,X,X]` / NONE | T0 | completa | 0.75 | `{NON_AUTH,BACKSTOP}` | false | TRAINING_ONLY |
| 7 | incomplete+backstop | `[X,X,X,C,N]` / AUTH | T1 | incompleta | 0.75 | `{COVERAGE,BACKSTOP}` | false | TRAINING_ONLY |
| 8 | all-three | `[X,X,X,C,N]` / NONE | T0 | incompleta | 0.75 | `{NON_AUTH,COVERAGE,BACKSTOP}` | false | TRAINING_ONLY |

(`C`=Clean, `X`=Excluded, `N`=null/probe-off. AUTH=`ANDROID_CHOREOGRAPHER`+uptime; NONE=`NONE_MONOTONIC_FALLBACK`.)

Test: `truthTable_ofAllEightCauseCombinations_pinsVerdictCauseAdmissibilityAndEvidence` (las 8 filas, `Set`
exacto por fila, `rtShadowAdmissible` y `evidence` por fila, y `!= OFFICIAL_ELIGIBLE`).

---

## Fase G — policy ejercitada + barrera mecánica (UNMOUNTED)

Ambos SUTs se ejercitan por el oráculo: `FocusTemporalEligibility.evaluate` (todos los tests) y
`FocusEvidenceStatePolicy.evidenceOf` + `rtShadowAdmissible` (tabla de verdad, barrido de evidencia,
`rtShadowAdmissible_requires…`). `OFFICIAL_ELIGIBLE` inconstruible probado sobre 32 combinaciones
booleanas × 4 muestras temporales + las 8 filas de la tabla.

**Barrido de consumidores en producción (`commonMain`), mecánico y versionado (grep sobre `HEAD 8a06b9a`):**

| Símbolo | Call-sites / consumidores en producción |
|---|---|
| `FocusTemporalEligibility.evaluate(` | **0** |
| `FocusEvidenceStatePolicy.evidenceOf(` / `.rtShadowAdmissible(` | **0** |
| `FocusEvidenceStatePolicy` (cualquier referencia) | **0** |
| `FocusTemporalEligibility` (cualquier referencia) | 3 — todas el TIPO `TemporalEvaluation`/`Verdict.Eligible` en la FIRMA de `FocusEvidenceStatePolicy` (shadow→shadow, él mismo sin consumidores) |

⇒ Toda la cadena shadow está **UNMOUNTED**: ningún código de producción llama a `evaluate()` ni a la policy.
(Los dos `.evaluate(` que aparecen fuera de `focus2` son símbolos distintos: `LanguageSessionStateHolder`'s
`evidence.evaluate` y `SessionEligibilityPolicy.evaluate`.)

**Barrera estructural (constant-pool, transitiva):** `Focus2DependencyBoundaryTest` prueba mecánicamente que
NINGUNA clase de `focus2` alcanza los tipos de perfil/agregador (`profile/`, `ModuleContribution`,
`MindBoostProfileAggregator`) directa o transitivamente — la dirección "no wiring a perfil/rank". El mutante
M15 (Fase F) inyecta una referencia a perfil y confirma que esta barrera se pone en ROJO (carga real).

---

## Fase H — paridad cross-platform

El oráculo NO usa API JVM (grep de `java.`/`javax.`/`System.`/`File`/`Thread` = 0 hits). El NÚCLEO puro de la
propiedad (`nonExclusionState` / `comparableByExclusion` / `reopeningsOverAscendingExclusion` / `isEligible`)
se EXTRAJO a `commonTest` (`FocusTemporalEligibilityProperty.kt`), que compila para JVM **y** Kotlin/Native,
usando sólo stdlib + tipos `commonMain` de `focus2`. El oráculo pesado permanece en `desktopTest` (consistente
con los dos tests hermanos de los SUTs y con `Focus2DependencyBoundaryTest`, que SÍ es JVM-bound por leer
class-files), y consume el núcleo.

`CROSS_PLATFORM_PROPERTY_PARITY = NOT_RUN`: el núcleo se verifica bajo `desktopTest` en este host; su ejecución
bajo `iosSimulatorArm64Test` requiere macOS/CI y no ha corrido (Windows no puede compilar/ejecutar K/N).

---

## Fase F — matriz de mutación (real, aislada, reversible)

Metodología: por cada mutante, aplicar desde los bytes pristine (los mutantes NO se apilan) → correr el test
que lo mata → registrar KILL/SURVIVE del **exit code REAL de Gradle + `AssertionError` en el XML/log** →
restaurar → verificar `sha256 == pristine` (byte-idéntico). Serial, worktree aislado. Campaña
`2026-07-23T17:52:24Z → 18:15:28Z`. Verificación final pristine: `elig IDENTICAL`, `pol IDENTICAL`.
Reportes: `.mutation-pristine/mutation_report.jsonl`, `run_M*.log` (conservados como evidencia).

| # | Mutante (cambio en el SUT) | SUT | esperado | veredicto | test(s) que lo mata | `AssertionError`s | restaurado |
|---|---|---|---|---|---|---|---|
| M1 | backstop `>` → `>=` | elig | KILL | **KILL** | `backstopBoundary_isStrict…` (L181: 0.50 debe seguir Eligible) | 1 | ✓ byte-idéntico |
| M2 | backstop `>` → `<` | elig | KILL | **KILL** | `backstopBoundary_isStrict…` + 8 | 9 | ✓ |
| M3 | verdict siempre `Eligible` | elig | KILL | **KILL** | `backstopBoundary…`, monotonía, truthTable, adversaryHalf, codominio… | 9 | ✓ |
| M4 | verdict siempre `Excluded(causes)` | elig | KILL | **KILL** | `backstopBoundary…` + honest/none | 8 | ✓ |
| M5 | `excludedFraction` siempre `0.0` | elig | KILL | **KILL** | backstop + inBand + monotonía… | 10 | ✓ |
| M6 | usa `1 − excludedFraction` (accepted) | elig | KILL | **KILL** | backstop + inBand + monotonía… | 10 | ✓ |
| M7 | umbral hardcodeado `0.50` (ignora inyectado) | elig | KILL | **KILL** | `evaluateReadsTheConfiguredBackstopMax…` (mata SÓLO por el umbral inyectado) | 1 | ✓ |
| M8 | umbral hardcodeado `0.99` | elig | KILL | **KILL** | `backstopBoundary…` + inBand + … | 6 | ✓ |
| M9 | omite causa `NON_AUTHORITATIVE_CLASS` | elig | KILL | **KILL** | `truthTable…` + codominio + 1 | 3 | ✓ |
| M10 | omite causa `COVERAGE_INCOMPLETE` | elig | KILL | **KILL** | `truthTable…` + codominio + 1 | 3 | ✓ |
| M11 | omite causa `SESSION_BACKSTOP_EXCEEDED` | elig | KILL | **KILL** | `backstopBoundary…` + 8 | 9 | ✓ |
| M12 | **reordena** las 3 causas (buildSet) | elig | **SURVIVE** | **SURVIVE (equivalente)** | — (ver prueba formal) | 0 | ✓ |
| M13 | `rtShadowAdmissible` ignora el verdict temporal | pol | KILL | **KILL** | `truthTable…` + `rtShadowAdmissible_requires…` | 2 | ✓ |
| M14 | `evidenceOf` permite `OFFICIAL_ELIGIBLE` | pol | KILL | **KILL** | `officialEligible_isInconstructible…` + truthTable | 2 | ✓ |
| M15 | inyecta una referencia a `profile/ModuleContribution` (rompe la barrera) | elig | KILL | **KILL** | `Focus2DependencyBoundaryTest.no_focus2_class_reaches_the_profile_contribution_types` (dirty exit=1; clean exit=0, boundary 3/0) | 1 | ✓ IDENTICAL |

**Resultado (M1–M15): 14 KILL, 1 SURVIVE (equivalente demostrado), 0 inesperados, 0 fallos de restauración.**
Todos los mutantes load-bearing (backstop `>`/`>=`/`<`, umbral hardcodeado, umbral inyectado ignorado,
omisión de cada causa, `OFFICIAL_ELIGIBLE` construible, barrera de perfil) mueren. El único superviviente es
equivalente por construcción (Set order-independiente).

**M12 — prueba formal de equivalencia (el único superviviente):** el mutante intercambia el ORDEN de dos
llamadas `add(...)` dentro de `buildSet { … }`. Un `Set` es no-ordenado por definición; `buildSet` produce el
MISMO conjunto sin importar el orden de inserción, luego `evaluate()` devuelve un output idéntico bit a bit y
el oráculo pasa (exit 0, ejecución real confirmada). Esto NO es un survivor por falta de cobertura: la tabla de
verdad afirma el `Set` EXACTO de causas por fila (order-independiente), así que un cambio de precedencia REAL
(que produjera un conjunto distinto) moriría. El contrato de precedencia es "unión de causas disparadas", no una
escalera — M12 lo confirma en vez de refutarlo. Superviviente aceptado como **equivalente demostrado**.

## Fase J — regresiones (serie, worktree aislado, evidencia por bytes)

`final-gates.sh` (`2026-07-23T18:19:23Z → 18:24:10Z`), serie:

| # | Gate | Comando | Resultado |
|---|---|---|---|
| 1 | Oráculo (baseline + survivor M12) | `:shared:desktopTest --tests '*AdversarialOracleTest'` | **15 tests / 0 fail / 0 err** (XML) |
| 2 | M15 barrera (dirty) | `:shared:desktopTest --tests '*Focus2DependencyBoundaryTest'` (con ref a perfil) | exit **1** — `no_focus2_class_reaches_the_profile_contribution_types` FALLA (barrera con dientes) |
| 3 | M15 barrera (clean) | idem sobre pristine | exit **0** — boundary **3 tests / 0 fail**; `elig IDENTICAL` |
| 4 | **Full `:shared:desktopTest`** | `--rerun-tasks` (ejecución REAL, no cache) | **570 clases / 4032 tests / 0 failures / 0 errors / 0 skipped** |
| 4b | focus2 subset (del run 4) | — | **28 clases / 229 tests / 0 failures+errors** |
| 5 | Módulo detekt | `:shared:detekt` | exit 1 — ver Fase I (split) |
| 6 | `Focus2DependencyBoundaryTest` (barrera perfil/rank/v1) | incluido en runs 2/3/4 | verde en pristine |
| — | Verificación final pristine | `sha256` | `elig IDENTICAL`, `pol IDENTICAL` |

Nota `--rerun-tasks`: fuerza recompilación + ejecución real de TODAS las clases (no `up-to-date`/cache); el
conteo 4032/0 se AGREGA de los 570 XML `TEST-*.xml` frescos, no de "BUILD SUCCESSFUL".

### Fase I — estado detekt (clasificado)

- `PASO4_FILE_DETEKT = PASS` — mis 2 archivos nuevos: **0 findings** (verificado tras el fix del
  `MatchingDeclarationName` con `@file:Suppress` justificado; el oráculo sigue 15/0 con la anotación).
- `MODULE_DETEKT = FAIL_PREEXISTING` — **4 findings**, todos en 2 archivos iosMain, **UNCHANGED_FROM_HEAD**
  en el worktree (committeados), **fuera** de `detekt-baseline.xml` (2961 baselined; `ignoreFailures=false`,
  ratchet one-way). NO son de Paso 4 (Paso 4 sólo añade 2 archivos de test).
- `GLOBAL_DETEKT = FAIL_PREEXISTING` — igual.

Los 4 findings pre-existentes (owner = workstream del port iOS; criterio de cierre = `@file:Suppress("FunctionNaming")`
en los Composables + imports FQ, o baselinearlos — NO autorizado a Paso 4):

| Archivo (iosMain) | Línea | Regla |
|---|---|---|
| `DMListScreenIcon.ios.kt` | 11 | `FunctionNaming` (Composable PascalCase) |
| `AppMediaViewer.ios.kt` | 196 | `FunctionNaming` (Composable PascalCase) |
| `AppMediaViewer.ios.kt` | 23 | `WildcardImport` (`platform.CoreMedia.*`) |
| `AppMediaViewer.ios.kt` | 26 | `WildcardImport` (`platform.QuartzCore.*`) |

**Política del repo:** `detekt { buildUponDefaultConfig = true; ignoreFailures = false; baseline =
detekt-baseline.xml }`. El gate EXIGE módulo-verde ⇒ mientras estos 4 findings pre-existentes no se
resuelvan/baselinen (fuera del alcance de Paso 4), el gate de MÓDULO no puede declararse verde. El gate del
ARCHIVO de Paso 4 sí pasa.

---

## Criterios de cierre — estado

| Criterio | Estado |
|---|---|
| Propiedad formalizada | ✅ (Fase B) |
| Dominio y counts publicados | ✅ (5 cadenas / 25 evals / 14 elig / 11 excl / 80 pares) |
| Mitades no vacías y discriminantes (cardinalidad) | ✅ (honest 10, adversary 6, declaradas) |
| Threshold exacto pinned | ✅ (constante + boundary estricto + brackets + inyectado) |
| Truth table / precedencia completa | ✅ (8 filas, unión de causas) |
| Policy ejercitada (`evidenceOf` + `rtShadowAdmissible` + `OFFICIAL_ELIGIBLE` inconstruible) | ✅ |
| Barrera de no-wiring probada | ✅ (0 consumidores + M15 mata la barrera de perfil) |
| Mutantes críticos muertos | ✅ (14/15 KILL, 1 equivalente demostrado) |
| Oráculo reproducible | ✅ (reducer-driven, dominio enumerado) |
| Full focus2 verde | ✅ (229/0) |
| Full desktop verde | ✅ (4032/0) |
| Estado detekt correctamente clasificado | ✅ (PASO4 PASS / MODULE FAIL_PREEXISTING) |
| **MODULE_DETEKT verde** | ❌ **FAIL_PREEXISTING** (bloqueo fuera de alcance) |
| **CROSS_PLATFORM_PROPERTY_PARITY** | ❌ **NOT_RUN** (K/N no ejecutable en Windows) |
| Bytes transferidos / manifest / archive verificable | ✅ (Fase K — bundle Paso 4) |

⇒ **`IMPLEMENTED_BUT_NOT_CLOSED`**: el trabajo de Paso 4 está completo y verificado por bytes; el cierre queda
bloqueado por dos condiciones fuera de su alcance (detekt pre-existente del port iOS; paridad K/N en CI/macOS).

---

## Fase K — cadena de evidencia sellada (orden respetado)

1. Código/tests terminados → 2. gates corridos (Fase F/J) → 3. status actualizado (`S5_REOPEN_STATUS.md` §9) →
4. ledger actualizado (§4 checkpoint `TASK-2026-07-24-FOCUS-V5-S5-PASO4-HARDENING` + §0 bullet) →
5. **ledger resellado** (`node scripts/validate-audit-ledger.mjs` = `LEDGER OK — 130 findings, hash matches`) →
6. manifest reconstruido (per-member SHA en el bundle) → 7. validadores read-only → 8. **archive determinista** →
9. **extraído en carpeta nueva** → 10. **verificado por hash desde cero** → 11. adjuntado.

- **Bundle:** `docs/engines/focus_v5/reaudit/v2/transfer/S5_PASO4_EVIDENCE.tar.gz`
- **SHA-256:** `1b2821dc5ff55531cf78894b1817ea145fd82aa1526182e8ddccb8af36d71f06` · **55718 bytes** · **61 members**
- **Contenido:** los 2 tests + los 2 SUTs (bytes de producción sin cambios) + este doc + `mutation_report.jsonl`
  + 14 `run_M*.log` + 6 `gate_*.log` + 2 consolas + `detekt.xml` + 28 XML focus2 primarios + `mutate.mjs` +
  los 2 drivers de campaña + `TRANSFER_MANIFEST.json` + `README.md`. Sanitizado (host/user redactado);
  **staging scan fail-closed = CLEAN** antes de archivar.
- **Verificación desde cero (extract):** `EXTRACT_VERIFY: 61 members hash-match, 0 mismatched`; total focus2
  RE-DERIVADO de los XML extraídos = **28 suites / 229 tests / 0 failures / 0 errors** (coincide con el gate).

> Nota: R3 sigue `CANDIDATE`; el bundle NO ha sido transferido out-of-band a un receptor independiente
> (`TRANSFERRED_COMPLETE` fuera del alcance de Paso 4), y la procedencia sigue `FAIL_UNANCHORED` (sin commit
> firmado) — ambos son condiciones R3 de nivel superior, no de Paso 4.
