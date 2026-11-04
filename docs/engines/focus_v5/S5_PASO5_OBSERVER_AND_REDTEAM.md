# S5 · Paso 5 — Red-team técnico + presupuestos (observer-effect)

```
ESTADO = IMPLEMENTED_NOT_CLOSED (2026-08-18)
Fase ejecutada: la SIGUIENTE tras Paso 4 (`S5_REOPEN_STATUS.md §9`: «No se avanzó a Paso 5»).
Contenido normativo: ADR_FOCUS_TEMPORAL_QUALITY §R3-19 (observer-effect, FAIL, 6 bloqueantes B1–B6)
+ follow-ups F-R3-C · F-R3-D · F-R3-E · F-R3-L (§R3-16).
Sin montaje, sin wiring, sin flag: `focus2` sigue UNMOUNTED y `evaluate()` sin consumidores de producción.
R3 sigue CANDIDATA. Paso 5 NO la promueve y NO avanza a Paso 6.
```

## 1. Qué era esta fase

`§R3-19` es el único gate del plan marcado **BLOQUEANTE antes de montaje** y estaba en **FAIL** con seis
bloqueantes nombrados. El plan maestro (§5, `P5`) lo enuncia como «red-team técnico + presupuestos»; el
presupuesto en cuestión es el del propio instrumento: **un probe que induce el jank que luego clasifica no
mide nada**. Los cuatro follow-ups arrastrados (`F-R3-C/D/E/L`) son las piezas concretas de ese gate.

Fuera de alcance por decisión explícita: Paso 6 (protocolo físico), montaje (S6), fórmula de score
(`ADR-FOCUS-SCORING` = `BLOCKED_BY_DESIGN`), y `F-R3-A` (revocación subject-scoped) — ver §7.

## 2. Disposición bloqueante por bloqueante

| ID | Bloqueante (§R3-19) | Disposición | Evidencia |
|---|---|---|---|
| **B1** | Buffer no acotado; `ArrayList(1024)` es capacidad inicial, no cap; sin `BUFFER_OVERFLOW` | **CERRADO** | `FrameTimeRing` (ring `LongArray` con cap duro) + `TemporalWindowObservation.bufferOverflow` + `NoObservation` fail-closed. 8 tests |
| **B2** | Autoboxing por frame ⇒ «0 alloc/frame» FALSO | **CERRADO por construcción** | `record(Long)` escribe en `LongArray`; `FrameStatsCalculator.fromFrameTimes(LongArray)` nuevo — la ruta caliente no crea un solo `java.lang.Long`. La sobrecarga `List` delega en la primitiva (semántica idéntica, test de paridad) |
| **B3** | Sin `DisposableEffect` ⇒ callback Choreographer auto-reposteante nunca parado | **CERRADO** | `DisposableEffect(probe) { onDispose { probe.stop() } }` en los actuals Android e iOS. Desktop documenta por qué NO lo lleva (NoOp `object` singleton, no hay bucle que fugar) |
| **B4** | Report-loss confundido con drops | **CERRADO** | Cadencia = DOS hechos separados (`expectedCadenceHz` / `detectedCadenceHz`); desacuerdo ⇒ `NoObservation`, nunca «todo son drops» ni «impecable». 6 tests |
| **B5** | El probe induce GC/calor/batería que luego clasifica | **MITIGADO ESTRUCTURALMENTE — NO MEDIDO** | Cero allocations por frame por construcción (B1+B2). El **Microbenchmark end-to-end sigue `NOT_RUN`**: exige dispositivo (Paso 6). Honestamente: la causa dominante está eliminada, la magnitud residual no está medida |
| **B6** (=L1) | El marcador de render gatea vía `truncationSlackNanos` | **CERRADO** | `markerVerdict` BORRADO; `windowIntegrity` ya no acepta slack. Propiedad `theRenderMarkerNeverChangesAVerdict` (5 formas × 6 valores de marcador, incl. `null` y absurdos) |

**Veredicto honesto del gate:** 5 de 6 cerrados en código; **B5 parcial**. `§R3-19` pasa de `FAIL` a
**`FAIL_RESIDUAL_B5_UNMEASURED`**, no a PASS. El gate es «bloqueante antes de montaje» y el montaje es S6,
así que el residuo no bloquea nada de Paso 5 — pero tampoco se declara superado.

## 3. Follow-ups cerrados

- **F-R3-C** — marcador→proxy no-gateante + borrar enum fantasma. `ONSET_INTEGRITY` y `PRE_ONSET_RESPONSE`
  eliminados de `WindowExclusionReason` tras verificar por grep **cero productores**. AC#4 queda protegido por
  un escáner de fuentes (`everyExclusionReasonIsProducedSomewhereInProductionCode`) que recorre `commonMain` y
  falla si cualquier entrada del enum deja de tener productor. **AC#3 se satisface por PROPIEDAD, no por
  escáner** (§2, B6): se declara así en vez de afirmar el escáner que el ADR nombraba.
- **F-R3-D** — ring + overflow + `DisposableEffect` (§2). Microbenchmark `NOT_RUN`.
- **F-R3-E** — modelo de cadencia + `NotCaptured` sin expected. iOS declara `expectedCadenceHz = null` (§5).
- **F-R3-L** — `criticalJitterFactor` resuelto como **excepción no-versionada DECLARADA** (vive en el
  `FrameStatsCalculator` compartido con la lane de Velocidad; versionarlo ahí re-clasificaría las dos lanes) +
  **pinneado por comportamiento** (2.4× nominal = drop no crítico; 2.6× = crítico). Validador `init{}` escrito.

### 3.1 El validador `init{}` afirma solo lo que es necesario

`TemporalQualityThresholds` valida en construcción: versión no vacía, `minFramesWindow ≥ 2`,
`minFramesWindow < minFramesSession`, las cinco fracciones en `[0,1]` y finitas, y
`windowDropCleanMax < windowDropExcludeMin`.

**No** afirma `windowDropExcludeMin < sessionExcludedMax`: son poblaciones distintas (fracción de frames
dentro de una ventana vs fracción de ventanas dentro de una sesión) y el ADR §R3-18 lo excluye
explícitamente. Un test lo fija como la **mitad discriminante anti-exceso**: si alguien añade esa
comparación «por simetría», el test se pone rojo.

## 4. Dos defectos de honestidad encontrados y corregidos

Ninguno estaba registrado; ambos son de la familia «ausencia fabrica perfección» (learning 2026-07-15).

**(a) `FrameStatsCalculator` con `refreshHz` NaN devolvía un stream impecable.** Con NaN como periodo
nominal, toda comparación `delta > umbral` es falsa ⇒ cero drops, sin jitter crítico. Un dato de referencia
ABSENTE fabricaba la medición perfecta. Cero/negativo/infinito son la misma clase de no-respuesta.
Corregido: `FrameStats.UNAVAILABLE` fail-closed. Test con mitad discriminante (la misma traza a 60 Hz sigue
clasificando como jank). → `MB-FOCUS-0064`.

**(b) El adapter iOS auto-detectaba su propia cadencia y clasificaba contra ella.** Una ventana pacing a la
mitad del ritmo leía como **CLEAN**: el falso negativo que el charter prohíbe. La intención original era
correcta (no clasificar contra el máximo del panel ProMotion, que produciría el falso positivo opuesto) —
pero la salida honesta no es elegir el error conveniente, sino declarar que no hay expectativa defendible.
CADisplayLink observa la cadencia del SISTEMA, no lo que el bucle Skia/Metal de esta app iba a componer.
Corregido: `expectedCadenceHz = null` en iOS ⇒ `NoObservation` ⇒ `NotCaptured`. → `MB-FOCUS-0065`.

## 5. Tres decisiones de diseño, declaradas

1. **Buffer overflow ⇒ `NoObservation`, NO `Excluded`.** El tamaño del buffer del instrumento no es un hecho
   sobre el trial del usuario. `Excluded` retira accuracy; una muestra incompleta solo justifica no afirmar
   nada. Confundirlos convertiría una limitación nuestra en una penalización suya.
2. **El marcador se conserva como cantidad sombra descriptiva.** Se registra, no clasifica. Borrarlo perdería
   el dato que Paso 6 necesita para validar físicamente el bound; dejarlo gateando era B6.
3. **La cadencia se compara numéricamente contra una tolerancia versionada** (`cadenceMatchTolerance = 0.05`),
   no vía `HzBand`: dos rates distintos pueden caer ambos en `HzBand.OTHER` y darían un falso «coinciden».

## 6. Prompts ejecutados, en orden, con su gate

| # | Prompt | Resultado | Gate ejecutado |
|---|---|---|---|
| P5-1 | F-R3-L: validador `init{}` + `criticalJitterFactor` + `cadenceMatchTolerance` | HECHO | `desktopTest` focus2+timing: 6 clases / 36 tests / 0F |
| P5-2 | B1+B2: `FrameTimeRing` + sobrecarga `LongArray` + guard NaN | HECHO (+defecto (a)) | 7 clases / 47 tests / 0F |
| P5-3 | B6/F-R3-C: borrar `markerVerdict` + enum fantasma + AC#4 | HECHO | 35 clases / 274 tests / 0F |
| P5-4 | B3+B4+F-R3-E: `DisposableEffect` ×2, cadencia de 2 hechos, iOS `null` | HECHO (+defecto (b)) | `compileKotlinDesktop` + `compileAndroidMain` EJECUTADOS verdes |
| P5-5 | Suite completa | VERDE | 577 clases / 4082 tests / 0F/0E/0S |
| P5-6 | REDTEAM #31: presupuesto de permisos | HECHO | `Focus2PlatformPermissionTest` 2/2 |
| P5-7 | Gate final en serie | VERDE tras 3 fixes detekt | ver §8 |

## 7. Riesgo de seguridad: surfaceado, NO corregido (decisión declarada)

**REDTEAM #32 / `F-R3-A` — GAP-1/GAP-2: los stores de focus2 no están cableados a
`SubjectRevocationAuthority`.** Es un riesgo real y sigue vivo. **No se toca en Paso 5**, por tres razones:

1. Su owner declarado es `data-sync-dev + security-privacy`, no esta fase.
2. Su gate son **9 interleavings** de revocación (antes/durante/después de write, process-death entre
   tombstone y purga, reinicio completa la purga) — un slice propio, no un añadido.
3. Cablear a medias una autoridad de revocación es exactamente el fallo que el learning 2026-07-14 registra:
   un gate booleano fail-open expone, uno fail-closed fabrica ausencia.

Mitigación vigente por construcción: `focus2` está UNMOUNTED (`mindboost_focus2` vacío en runtime, sin
escritor productivo), y el borrado de cuenta cubre el namespace por `clearNamespace` blanket. El hueco es de
revocación **subject-scoped**, no de borrado de cuenta. Registrado en `MB-FOCUS-0048` (OPEN).

## 8. Gates (serie, una sola invocación Gradle)

```
gradlew :shared:desktopTest :shared:detekt :androidApp:compileDebugKotlin --console=plain
```

- **BUILD SUCCESSFUL** · 83 actionable: **26 executed** / 57 up-to-date.
- `:shared:desktopTest` **EJECUTADO** (sin sufijo `UP-TO-DATE`): **578 XML frescos · tests=4084 ·
  failures=0 · errors=0 · skipped=0** (agregado de los XML escritos en la ventana del run, no de
  `BUILD SUCCESSFUL` — learning 2026-07-19).
- `:shared:detekt` **EJECUTADO · 0 findings**, baseline INTACTO. Nota: `MODULE_DETEKT` que Paso 4 dejó en
  `FAIL_PREEXISTING` (4 findings del port iOS) está **PASS en este árbol** porque los fixes de
  `S5_REOPEN_STATUS §3.1` viven aquí como cambios sin commitear.
- `:androidApp:compileDebugKotlin` **EJECUTADO** verde.
- **iOS `NOT_EXECUTED`** (host Windows). El actual iOS de esta fase **no ha compilado nunca**; su
  verificación es el run de CI macOS del gate de salida (Paso 7).

Tres findings de detekt de código propio se corrigieron **de verdad**, sin tocar el baseline y sin
`@Suppress`: dos `ReturnCount` (guards fusionados / extensión `Double?.asCadence()`) y un `ComplexCondition`.

## 9. Lo que esta fase NO hizo

- No montó nada. No añadió flag. No cambió el camino oficial de ningún resultado.
- No corrió el Microbenchmark (B5) — exige dispositivo.
- No compiló iOS.
- No tocó `F-R3-A/B/F/G/H/I/J/K` ni avanzó a Paso 6.
- No promovió la R3: sigue **CANDIDATA**, pendiente de auditoría externa.
