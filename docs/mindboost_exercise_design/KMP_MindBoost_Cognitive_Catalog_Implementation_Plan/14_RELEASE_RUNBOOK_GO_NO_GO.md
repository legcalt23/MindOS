# 14 — Release runbook GO / NO-GO (versión ejecutable detallada)

> **Estado de partida (hecho, anclado a `AUDIT_V2_PRODUCTO.md` §1 y `AUDIT_V2_GROUND_TRUTH.md`): NO-GO como perfil.**
> De ~71 ejercicios, solo ~9 pueden tocar el perfil hoy y casi todos solo en `calibrating`. El bloqueo
> NO es de contenido: es **infraestructura de medición ausente, replicada 71 veces**. Tres bloqueadores
> estructurales P0:
> 1. **Jank-gate muerto en TODO MindBoost** — `frameDropFraction=0.0` / `criticalFrameJitter=false`
>    HARDCODEADOS en `SpeedSessionStateHolder.kt:259-260` (hecho, verificado en repo). El gate
>    `device_unreliable` existe como modelo (`TimebasePolicy.deviceTimingRisk` + `SessionQualityEvaluator`)
>    pero **nunca dispara por causa física**.
> 2. **Anti-gaming diseñado, no implementado** — ~4/8 capacidades reales (anticipación, RT<400/AGRS,
>    SSRT-trust, fast-commission + quality-gate + abandono); cadencia de tap solo en Arena sintético;
>    sesgo posicional inexistente (`spatialEntropy=1.0` fijo); keyword-stuffing BLOCKED por diseño.
> 3. **Bancos de juicio sin inter-rater ni holdout** — Logic(6 fijos), Language(36 fijos), Emotions
>    SEPARATOR(40, holdout fantasma), Creatividad(14). Memorizables en 1-2 sesiones.
>
> **GO condicional** para un v1 honesto de ~9 ejercicios etiquetados "práctica/calibrando" **si y solo si**
> caen los 3 bloqueadores (INFRA_01..06 mergeadas+testeadas) y cada ejercicio v1 pasa su criterio EXACTO.
>
> **Invariantes no negociables (se repiten en cada sección):** `null != 0`; Enfoque/Velocidad **sin
> profile-update** hasta timebase autoritativa + jank real; **cero claims de far-transfer NI para negarlos**
> (no añadir "fake news"/"IQ"/"regulación" ni siquiera para negarlos — el scanner cruza guiones y
> negaciones); anti-gaming **solo si implementado+testeado con adversarios**; banco pequeño/no-validado
> **bloquea perfil** (build fail).

Leyenda de estado: **(hecho)** verificado en repo · **(inferido)** deducido del diseño · **(POR_CONFIRMAR_EN_REPO)**
ruta/símbolo no verificado, marcar antes de marcar la casilla.

---

## 0. Cómo se usa este runbook

- Cada casilla `[ ]` lleva un **comando de validación** concreto (Gradle, scanner, test) donde aplica.
- Una casilla solo se marca cuando su comando devuelve **verde**; pegar el output relevante en la evidencia de release.
- **Cualquier casilla P0 sin marcar ⇒ NO-GO del perfil.** El catálogo puede lanzarse como **minijuegos sin
  profile-update** aunque el perfil sea NO-GO.
- Comandos Gradle en Windows/PowerShell: citar propiedades con punto, p.ej. `"-Pkotlin.incremental=false"`
  (aprendizaje 2026-06-19). Para Android del módulo compartido usar `:shared:compileAndroidMain`
  (NO `compileDebugKotlinAndroid`, que no existe — aprendizaje 2026-06-12).
- No correr en paralelo tareas Gradle que compartan `shared/build` (aprendizaje 2026-06-19): serializar
  `detekt`, compilación y `desktopTest`.

---

## 1. Pre-merge (por PR) — gate por cada pull request

| # | Item | Comando de validación | Bloquea merge si |
|---|---|---|---|
| 1.1 | Compila Android del módulo compartido | `./gradlew :shared:compileAndroidMain "-Pkotlin.incremental=false"` | no compila |
| 1.2 | Compila la app end-to-end | `./gradlew :androidApp:compileDebugKotlin` | no compila (detecta impls incompletas fuera de `:shared`) |
| 1.3 | Compila Desktop | `./gradlew :shared:compileKotlinDesktop` | no compila |
| 1.4 | Tests JVM del scope (unit + adversarial + integración) | `./gradlew :shared:desktopTest` | rojo |
| 1.5 | Detekt sin regenerar baseline | `./gradlew :shared:detekt` | nuevos findings fuera de baseline |
| 1.6 | Copy-scanner MindBoost (INFRA_05) sobre commonMain/commonTest/desktopTest | `./gradlew :shared:mindboostCopySafetyScanner` *(POR_CONFIRMAR_EN_REPO: task aún no existe; espejo de `arenaTerminologyScanner` en `shared/build.gradle.kts:198-272`)* | lexema prohibido (incl. negación/guion) |
| 1.7 | `registry ⟺ implemented` (consistencia de catálogo) | `./gradlew :shared:desktopTest --tests "*ExerciseCatalogConsistencyTest"` (existe, focus) | generador render-compatible sin exponer, o id sin generador/renderer |
| 1.8 | Property: ningún `ScoreResult` no-VALID se serializa como número | `./gradlew :shared:desktopTest --tests "*ScoringStateMachineTest"` (INFRA_01, NUEVO) | viola `null != 0` |

**Checklist:**
- [ ] 1.1 `:shared:compileAndroidMain` verde.
- [ ] 1.2 `:androidApp:compileDebugKotlin` verde.
- [ ] 1.3 `:shared:compileKotlinDesktop` verde.
- [ ] 1.4 `:shared:desktopTest` verde (incluye adversariales del scope).
- [ ] 1.5 `:shared:detekt` verde sin tocar `shared/detekt-baseline.xml`.
- [ ] 1.6 Copy-scanner verde sobre los 3 source-sets.
- [ ] 1.7 `ExerciseCatalogConsistencyTest` verde.
- [ ] 1.8 Property `no-VALID nunca produce número` verde.

---

## 2. Pre-release (gate de catálogo) — una vez por release

> **Toda la sección es P0.** Cualquier casilla sin marcar ⇒ NO-GO de perfil.

- [ ] 2.1 **INFRA_01..06 mergeadas y testeadas.** Validar cada una:
  - `./gradlew :shared:desktopTest --tests "*ScoringStateMachineTest"` (INFRA_01 — sealed `ScoreResult` + state machine).
  - `./gradlew :shared:desktopTest --tests "*TimingPolicyClassifierTest"` (INFRA_02 — TimingPolicy con signo + jank real). *(POR_CONFIRMAR_EN_REPO)*
  - `./gradlew :shared:desktopTest --tests "*AntiGaming*Test"` (INFRA_03 — framework + adversarios). *(POR_CONFIRMAR_EN_REPO)*
  - `./gradlew :shared:desktopTest --tests "*BankValidatorTest"` (INFRA_04 — banco/holdout). *(POR_CONFIRMAR_EN_REPO)*
  - `./gradlew :shared:mindboostCopySafetyScanner` (INFRA_05). *(POR_CONFIRMAR_EN_REPO)*
  - `./gradlew :shared:desktopTest --tests "*MinSampleGateTest"` (INFRA_06 — n_min/null/calibrating). *(POR_CONFIRMAR_EN_REPO)*
- [ ] 2.2 **Ningún ejercicio BLOCK/RETIRE aparece como `canUpdateProfile`.** Comando:
  `./gradlew :shared:desktopTest --tests "*CatalogProfileEligibilityTest"` — test que cruza el veredicto
  (`AUDIT_V2_PRODUCTO.md §2`) contra `canUpdateProfile()`; FALLA si un BLOCK/RETIRE puede tocar perfil.
  *(POR_CONFIRMAR_EN_REPO: test a crear)*
- [ ] 2.3 **Enfoque y Velocidad marcados "minijuego sin perfil".** No contribuyen al brain-profile hasta
  timebase autoritativa + jank real. Validar: `*CatalogProfileEligibilityTest` afirma `FOCUS.* ` y
  `SPEED.*` ⇒ `updatesProfile==false`. (Razón: RT-crítico sin timebase frame-accurate; `AUDIT_V2_PRODUCTO.md`
  §"riesgos por dispositivo".)
- [ ] 2.4 **Cada ejercicio v1 con `canUpdateProfile` pasó su criterio EXACTO** (ver tabla §3 de este doc y
  `AUDIT_V2_PRODUCTO.md §12`). Un test por ejercicio:
  `./gradlew :shared:desktopTest --tests "*<Exercise>GateTest"`.
- [ ] 2.5 **`EX_TRANSFER_PROBE` cableado** O claim de "mejora de capacidad" deshabilitado en UI. Validar:
  `./gradlew :shared:desktopTest --tests "*TransferProbe*Test"` (RCI `NotEnoughData` por defecto, sonda
  UNTRAINED servida, `|RCI|>1.96` gated). *(POR_CONFIRMAR_EN_REPO: hoy `RealImprovementCriterion`/`ReliableChangeIndex`
  existen pero sin caller — hecho.)*

---

## 3. Data QA — psicometría y muestreo

> Ancla: `AUDIT_V2_GROUND_TRUTH.md` (min-n por métrica) + `AUDIT_V2_TECNICO.md §"Thresholds (MinSampleGate)"`.

- [ ] 3.1 **`nEffective`/`nMin` enforced por métrica; n<nMin ⇒ CALIBRATING (no 0).** Umbrales objetivo
  (subir de los actuales `MIN_N_*=3..5` que son demasiado bajos):
  - Comparativas (CONGRUENCY_EFFECT, SWITCH_COST, PRESSURE_DELTA, DUAL_TASK_COST, POST_ERROR): `nMin ≥ 20`/celda + IC.
  - SSRT: `nStops ≥ 30` + `p(respond|signal) ∈ [0.25,0.75]`.
  - Brier / calibration error: `≥ 40-60` juicios **agregados inter-sesión**, null en sesión única.
  - RCI: `|RCI|>1.96` reliable, gated por `≥2-3` administraciones equated.
  - BUR (Bayes): excluir trials `priori=posterior`, `min-n=12`.

  Comando: `./gradlew :shared:desktopTest --tests "*MinSampleGateTest"` (golden de umbrales) +
  `./gradlew :shared:desktopTest --tests "*DefaultMetricsEngineTest"` (regresión existente, focus).
- [ ] 3.2 **Agregación inter-sesión correcta (golden).** Verifica que `weightedAverage` solo agrega métricas
  `confident` y que el composite del perfil exige `≥3 ESTABLISHED`.
  Comando: `./gradlew :shared:desktopTest --tests "*MetricsEngineRegressionTest"` (golden existente) +
  `./gradlew :shared:desktopTest --tests "*MindBoostProfileAggregatorTest"`.
- [ ] 3.3 **Métricas comparativas null si falta una celda.** CONGRUENCY/SWITCH/POST_ERROR ⇒ `null` (no 0)
  cuando una condición (congruente/incongruente, switch/repeat, post-error/post-correct) está vacía.
  Comando: `./gradlew :shared:desktopTest --tests "*ComparativeMetricNullTest"`. *(POR_CONFIRMAR_EN_REPO)*

---

## 4. Device QA — timebase y jank

> **El bloqueador #1 (`AUDIT_V2_PRODUCTO.md §1`).** Hecho: el jank-gate está muerto en producción.

- [ ] 4.1 **`frameDropFraction` proviene de medición real (no 0.0 hardcoded).** Sustituido el literal de
  `SpeedSessionStateHolder.kt:259-260` por `FrameTimingProbe.stop().frameDropFraction` (INFRA_02, A.2 Android
  Choreographer real). Comando: `./gradlew :shared:desktopTest --tests "*TimingPolicyClassifierTest"`
  (jank simulado > umbral ⇒ `DeviceUnreliable`) + grep manual de que el literal `0.0` ya no alimenta el gate.
- [ ] 4.2 **RT-crítico fuera de Android autoritativo ⇒ `DEVICE_UNRELIABLE`, no perfil.** En Desktop/iOS de
  Speed (fallback monotónico) y en Focus (`MonotonicClock`, no frame), todo RT-crítico degrada y NO actualiza
  perfil. Comando: `./gradlew :shared:desktopTest --tests "*TimingPolicyClassifierTest"`
  (`platformSource != CHOREOGRAPHER_FRAME ⇒ NonAuthoritative`) + `*ScoringStateMachineTest`
  (`RT_CRITICAL && !timebaseTrusted ⇒ DEVICE_UNRELIABLE`).
- [ ] 4.3 **Device-swap: el mismo usuario en 2 dispositivos no produce scores de perfil divergentes.** Test
  con dos perfiles de timebase (autoritativo vs fallback): el fallback no escribe perfil, así que no puede
  divergir. Comando: `./gradlew :shared:desktopTest --tests "*DeviceSwapInvarianceTest"`. *(POR_CONFIRMAR_EN_REPO)*
- [ ] 4.4 **iOS RT-crítico = `DEVICE_UNRELIABLE` hasta portar CADisplayLink.** Hecho: `SpeedTimebase.ios.kt:14`
  delega a fallback. Comando: test que afirma `iosMain` ⇒ `NonAuthoritative(NO_DISPLAY_LINK)`. *(POR_CONFIRMAR_EN_REPO)*

---

## 5. Copy QA — claims y scanner

> Ancla: `AUDIT_V2_TECNICO.md §11` (copy permitido/prohibido) + `00_CHARTER.md §1`.
> **Invariante: cero claims de far-transfer NI para negarlos.** El scanner es whole-word case-insensitive y
> **cruza guiones/negaciones** (aprendizaje 2026-06-23): "non-clinical", "anti-fake-news", "no es IQ" ⇒ rojo.

- [ ] 5.1 **Scanner en `check` bloquea far-transfer/clínico/IQ/apuesta/fake-news incl. negaciones.** Lista
  exacta en `AUDIT_V2_TECNICO.md §11.2` (far-transfer: `inteligencia/IQ/Gf/mejores decisiones/multitarea/
  regulacion emocional/autocontrol/...`; clínico: `TDAH/depresion/diagnostico/trastorno/...`; apuesta:
  `apuesta/fichas/casino/odds/bankroll/...`; misinformación: `fake news/desinformacion/fuentes fiables/...`).
  Comando: `./gradlew :shared:mindboostCopySafetyScanner` (enganchado a `check`).
- [ ] 5.2 **Ninguna pantalla muestra constructo absoluto sin `VALID`.** "velocidad de procesamiento",
  "memoria de trabajo", "control inhibitorio", "flexibilidad cognitiva" como número/percentil exigen
  `ScoreResult.state == VALID`. Lo enforca INFRA_01 en UI (`claimLevel` + `copySafetyFlags`).
  Comando: `./gradlew :shared:desktopTest --tests "*AbsoluteConstructGatingTest"`. *(POR_CONFIRMAR_EN_REPO)*
- [ ] 5.3 **Pantallas de calibración/invalidación/insuficiencia con copy permitido** (`AUDIT_V2_TECNICO.md §11.1`):
  "Señal de práctica", "Estimación preliminar", "Datos insuficientes para estimar", "Calibrando tu línea base",
  "No actualizado por calidad de medición", "Medición contaminada — no contabilizada", "Proxy interno, no
  diagnóstico". Verificación: el scanner pasa Y la lista permitida aparece en `AppStrings`/pantallas de estado.

---

## 6. Profile QA — el perfil cognitivo global

> Ancla: `MindBoostProfileAggregator`, `DimensionState.kt:35` `require((CALIBRATING)==(value==null))` (hecho),
> Memory `RealImprovementCriterion`/`ReliableChangeIndex`.

- [ ] 6.1 **Brain-profile: dimensiones CALIBRATING renderizan "—", jamás 0.** Comando:
  `./gradlew :shared:desktopTest --tests "*MindBoostBrainProfile*Test"` (UI-model builder mapea CALIBRATING→"—").
- [ ] 6.2 **Señal inválida nunca reduce perfil.** Un estado no-VALID NO llama al aggregator (no hay
  "actualización a la baja"). Comando: `./gradlew :shared:desktopTest --tests "*ScoringStateMachineTest"`
  (`R-NO-REDUCE`: ningún path no-VALID pone `updatesProfile=true`).
- [ ] 6.3 **"Mejora confirmada" solo con `isGenuine && reliable` (RCI |>1.96|).** Comando:
  `./gradlew :shared:desktopTest --tests "*RealImprovementCriterionTest"` +
  `./gradlew :shared:desktopTest --tests "*ReliableChangeIndexTest"` (NotEnoughData con <2 admins).

---

## 7. Criterios de rollback (post-release)

| Disparador | Detección | Acción inmediata |
|---|---|---|
| Aparece un claim prohibido en producción (screenshot/regulador) | screenshot, ticket, scanner que no cubría una pantalla | rollback inmediato del copy + hotfix del scanner (añadir el lexema + su negación/compuesto) + re-run `mindboostCopySafetyScanner` |
| Un ejercicio BLOCK/RETIRE actualiza perfil | telemetría: dimensión sube por un id que debía estar gated | congelar la contribución de ese ejercicio (`updatesProfile=false`) + test `*CatalogProfileEligibilityTest` que reproduzca |
| Score de perfil cambia por device-swap | correlación score↔modelo de dispositivo | desactivar profile-update de TODAS las métricas RT (degradar a `DEVICE_UNRELIABLE`) hasta auditar timebase |
| Distribución de scores con masa en el mínimo | dashboard de distribución | sospechar `null→0` filtrado; auditar el adaptador del módulo y el property test `no-VALID nunca produce número` |
| Curva de "progreso" = curva de exposición de banco | progreso correlaciona con nº de sesiones, no con UNTRAINED | congelar claim de mejora; exigir holdout servido (INFRA_04) antes de re-habilitar |

---

## 8. Señales de alerta post-release (monitoreo continuo)

- **Masa de scores en el mínimo** ⇒ posible `null→0` filtrado (viola `null != 0`). Revisar adaptadores.
- **Correlación score ↔ modelo de dispositivo** ⇒ timebase no controlado; jank-gate no está midiendo.
- **Curva de "progreso" = exposición de banco** ⇒ memorización, no mejora; falta holdout/rotación.
- **Reportes de usuarios citando beneficios de vida real** ⇒ copy filtrando far-transfer; auditar cadena de
  strings que no pasó por el scanner.
- **Abandono que reduce score en ejercicios emocionales** ⇒ viola regla §8 (abandono = `ABANDONED_SAFE`,
  señal de seguridad, nunca culpa).

---

## 9. Matriz de decisión final

| Sección | Estado | Efecto si rojo |
|---|---|---|
| §1 Pre-merge | P0 por PR | merge bloqueado |
| §2 Pre-release (catálogo) | P0 | NO-GO perfil |
| §3 Data QA | P0 | NO-GO perfil |
| §4 Device QA | P0 | NO-GO perfil (Enfoque/Velocidad ya minijuego) |
| §5 Copy QA | P0 | NO-GO total (riesgo regulatorio) |
| §6 Profile QA | P0 | NO-GO perfil |

**GO de perfil ⟺ §1–§6 en verde con sus comandos.** Cualquier casilla P0 sin marcar ⇒ **NO-GO del perfil**;
el catálogo puede lanzarse como **minijuegos sin profile-update**. El copy-scanner (§5) es el único gate cuyo
fallo bloquea **incluso el lanzamiento como minijuego** (riesgo regulatorio existencial).

---

## 10. Evidencia de release (a adjuntar)

Para declarar GO, adjuntar el output de: `:shared:compileAndroidMain`, `:androidApp:compileDebugKotlin`,
`:shared:compileKotlinDesktop`, `:shared:desktopTest`, `:shared:detekt`, `mindboostCopySafetyScanner`, y los
`--tests` nombrados en §2–§6. Marcar cada ejercicio v1 con su `*GateTest` verde y su veredicto de
`AUDIT_V2_PRODUCTO.md §2`. Distinguir en la evidencia `hecho` / `inferido` / `POR_CONFIRMAR_EN_REPO`.
