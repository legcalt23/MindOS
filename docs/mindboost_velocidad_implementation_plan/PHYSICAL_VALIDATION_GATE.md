# MindBoost · Velocidad Mental — Gate de Validación Física (§15)

> **Estado:** GATE ABIERTO (2/4 perfiles). **P1 y P2 VALIDADOS en emulador**; faltan P3, P4 (hardware).
> Bloquea que el score/perfil de MVP-3 pasen de **preliminar** a **autoritativo**.

## ✅ Resultados registrados

| Perfil | Entorno | Fecha | Veredicto | Evidencia |
|--------|---------|-------|-----------|-----------|
| **P1** (API 34+) | Emulador AVD `Medium_Phone_API_36.1` (API 36 / Android 16) | 2026-06-14 | **PASS (ruta de precisión)** | DebugExport E1, 12 trials → `NANOS_NATIVE`/`LOW` |
| **P1-humano** (RT reales) | Emulador AVD `Medium_Phone_API_36.1` (API 36 / Android 16), **taps humanos en ventana** | 2026-06-15 | **PASS (RT plausibles)** | DebugExport E1, 12 trials → `rtMedian=176.7 ms`, `sessionQuality=HIGH`, 0 anticipaciones |
| **P2** (API <34) | Emulador AVD `speed_p2_api30` (API 30 / Android 11, 320×640) | 2026-06-15 | **PASS (ruta de precisión)** | DebugExport E1, 12 trials → `MILLIS_DERIVED`/`MODERATE` |
| P3 (gama media/baja) | — | — | pendiente | requiere hardware real (jitter no emulable) |
| P4 (90/120 Hz) | — | — | pendiente | requiere hardware real (alto Hz no emulable) |

**P2 — DebugExport verificado** (corrida E1, API 30, taps scriptados): `timebase=ANDROID_UI_UPTIME_NANOS` ·
`inputTimestampPrecision=MILLIS_DERIVED` · `deviceTimingRisk=MODERATE` · 12/12 `rtNanos>0` con
`rtNanos = responseUptimeNanos − stimulusUptimeNanos` (verificado, p. ej. t0:
`349373000000 − 347303868290 = 2069131710`) · `timestampSource=CHOREOGRAPHER_FRAME`. Los
`responseUptimeNanos` terminan en `000000` porque la ruta API<34 es `eventTime × 1_000_000` **por
diseño** (§4.1 regla 3) — no es un artefacto. Confirma la rama de fallback en runtime real <34.

**P1 — DebugExport verificado** (corrida E1, taps scriptados vía `adb input tap`):
`timebase=ANDROID_UI_UPTIME_NANOS` · `inputTimestampPrecision=NANOS_NATIVE` · `deviceTimingRisk=LOW` ·
12/12 `rtNanos>0` con `rtNanos = responseUptimeNanos − stimulusUptimeNanos` (aritmética verificada) ·
`timestampSource=CHOREOGRAPHER_FRAME`. `sessionQuality=LOW` correcto (RT mediano 1518 ms por la
latencia de polling de los taps automatizados → alta variabilidad; el evaluador de calidad lo marca
bien — no es un fallo). Esto valida la **ruta de precisión NANOS_NATIVE en runtime real**, no solo en
`commonTest`. **Caveat:** los timestamps de respuesta salen alineados a ms porque `adb input tap`
inyecta MotionEvents sintéticos; un dedo real da nanos sub-ms. P3 (jitter) y P4 (alto Hz) **no** son
representables en emulador → siguen requiriendo hardware. Capturas locales en `validation/` (gitignored).

**P1-humano — DebugExport verificado** (corrida E1, **taps físicos del usuario** en la ventana del
emulador, no `adb input tap`): cierra el caveat de la corrida P1 scriptada. `sessionId`
`…:speed:1781509781949`. `timebase=ANDROID_UI_UPTIME_NANOS` · `inputTimestampPrecision=NANOS_NATIVE` ·
`deviceTimingRisk=LOW` · `sessionQuality=HIGH` (no `LOW`). 12/12 trials `isCorrect`, **0
anticipaciones**, 0 lapsos. `rtNanos = responseUptimeNanos − stimulusUptimeNanos` (aritmética
verificada, p. ej. t0: `449243279000 − 449041951590 = 201327410`; t1:
`451501414000 − 451325284832 = 176129168`). **`rtMedian = 176.7 ms`** (`cv = 0.077`, RT por trial
156–201 ms) — son RT humanos reales y consistentes, a diferencia de los **1518 ms** de los taps
scriptados de la corrida P1 anterior. **Caveat honesto:** 176.7 ms está por **debajo** de la guía
aproximada ~250–450 ms de §5.2, pero **no** dispara el heurístico de fallo (mediana < 120 ms **con**
casi todo `isAnticipation`): aquí la mediana es > 120 ms y hay **cero** anticipaciones, con el onset
bien capturado (CV 7,7 %) → es un reactor rápido genuino (clic de ratón sobre E1, que espera el
toque y permite estar preparado), no un artefacto de reloj. Los `responseUptimeNanos` terminan en
`000` (alineados a µs) porque la inyección de input del emulador cuantiza a microsegundos aunque la
ruta reporte `NANOS_NATIVE`; un dedo real sobre vidrio da nanos sub-µs (sigue pendiente de hardware,
P3/P4). El flag `preliminary` **no** se retira con esto (gate §15 sigue abierto: faltan P3/P4 y la
calibración).

> Prerequisito que desbloqueó P1: el build debug usaba un `src/debug/google-services.json` placeholder
> (no autenticaba). Se resolvió registrando `com.mindos.app.debug` en `mindos-8452f` con config real,
> SHA-1 de debug y token App Check debug (ver §2.1).

---

> **Estado:** GATE ABIERTO. Bloquea que el score/perfil de MVP-3 pasen de **preliminar** a
> **autoritativo**. Este documento es el paquete ejecutable para cerrarlo.
>
> **Por qué existe:** el código (MVP-0 → MVP-3) está completo y verde en simulación, pero el
> plan (§3, §15, acceptance #18) exige **validación física multi-dispositivo** antes de retirar
> el rótulo "preliminar". La medición de tiempo de reacción depende del hardware real
> (Choreographer, MotionEvent, tasa de refresco, jitter) que **ningún simulador ni esta sesión de
> IA pueden reproducir**. Lo ejecuta una persona con dispositivos físicos.

---

## 0. Reparto de responsabilidades

| Parte | Quién | Estado |
|---|---|---|
| Código de medición, métricas, score, DebugExport | IA / repo | ✅ hecho, verde |
| Validación sintética (simulador §12) | IA / repo | ✅ 131 tests speed, 970 `:shared:desktopTest` |
| **Ejecutar las corridas en 4 dispositivos físicos** | **Tú (humano)** | ⛔ pendiente — este doc |
| **Recoger y leer los DebugExport JSON/CSV** | **Tú (humano)** | ⛔ pendiente — §4–§5 |
| Calibrar referencias + quitar flag `preliminary` con el PASS | IA / repo | ⏳ tras tu PASS — §7 |

La IA **no puede** completar el gate por ti: requiere pulsaciones táctiles humanas sobre vidrio
real. Este documento te da el guion exacto, los comandos `adb`, los campos a verificar y el
criterio Go/No-Go para que el resultado sea inequívoco.

---

## 1. Matriz de los 4 perfiles técnicos obligatorios (§15)

Necesitas **al menos** un dispositivo por fila. Uno solo puede cubrir varias filas si cumple
varias condiciones (p. ej. un gama media API 35 a 120 Hz cubre P1, P3 y P4; aun así te falta P2).

| ID | Perfil requerido | Condición técnica | Qué valida | `InputTimestampPrecision` esperada | `DeviceTimingRisk` esperado |
|----|------------------|-------------------|------------|-----------------------------------|-----------------------------|
| **P1** | Android **API 34+** | `Build.VERSION.SDK_INT >= 34` | Ruta de nanos nativos | `NANOS_NATIVE` | `LOW` |
| **P2** | Android **API < 34** | `SDK_INT in 29..33` | Fallback millis derivados | `MILLIS_DERIVED` | `MODERATE` |
| **P3** | **Gama media/baja** | RAM ≤ ~4 GB / SoC económico | Descartar jitter extremo | la que corresponda a su API | `LOW`/`MODERATE` salvo jitter ⇒ `HIGH` |
| **P4** | **Alta tasa de refresco** | Pantalla 90 Hz o 120 Hz | Estabilidad del Choreographer a alto Hz | la que corresponda a su API | `LOW` esperado (sin `FRAME_JITTER_DETECTED`) |

> P4 es "si está disponible en el laboratorio". Si **no** tienes un 90/120 Hz, documenta su
> ausencia como **desviación explícita** (§6) — no la inventes ni la des por aprobada.

`minSdk = 29`, así que P2 puede ser cualquier dispositivo/emulador API 29–33.

---

## 2. Preparación (una vez)

### 2.1 Build debug (DebugExport SOLO existe en debug)

`DebugExport` se activa por `FLAG_DEBUGGABLE`. En **release está deshabilitado** y no escribe
nada (acceptance #14/#19). Por tanto valida con un **APK debug**:

```bash
./gradlew :androidApp:assembleDebug
# APK en androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

El `applicationId` debug es **`com.mindos.app.debug`** (`applicationIdSuffix = ".debug"` en
`androidApp/build.gradle.kts`).

> [!NOTE]
> El build debug autentica contra la app Firebase separada `com.mindos.app.debug` del proyecto
> `mindos-8452f`. `androidApp/src/debug/google-services.json` contiene su configuración real y la
> API key está restringida al paquete y SHA-1 debug. En un emulador o dispositivo nuevo, registra
> el token emitido por `DebugAppCheckProvider`; borrar los datos de la app genera un token nuevo.

### 2.2 Confirma el API level de cada dispositivo (clasifica P1 vs P2)

```bash
adb shell getprop ro.build.version.sdk      # 34+ ⇒ P1 ; 29–33 ⇒ P2
adb shell getprop ro.product.model
adb shell dumpsys display | grep -i "fps\|refresh"   # confirma 90/120 Hz para P4
```

---

## 3. Protocolo de corrida por dispositivo

Repite **todo** este bloque en cada uno de los 4 dispositivos. Misma persona o varias, no
importa: lo que se valida es el **hardware/timing**, no la habilidad cognitiva.

1. Abre la app → **MindBoost** → pista **Velocidad Mental**.
2. Ejecuta, **en orden**, una corrida completa de cada ejercicio disponible:
   - **E1 · Destello** (RT simple) — al menos **1 bloque completo**.
   - **E6 · Multivía** (2 vías, adaptativo) — al menos **1 corrida multi-bloque** (deja que el
     staircase corra varios bloques; no abandones al primero).
   - **E3 · Cifrado** y **E7 · Compuerta** — 1 corrida cada uno.
3. Responde **con normalidad** (no intentes "romperlo" con toques al azar): el objetivo es que
   `sessionQuality` salga `HIGH`/`ACCEPTABLE` en condiciones normales. Habrá una corrida extra
   "estrés" en §3.1.
4. Al terminar cada ejercicio verás el resumen descriptivo (precisión, RT mediano). En build
   debug, usa el **panel de depuración** (toggle de desarrollador) → **"Exportar datos brutos
   (JSON/CSV)"**. Anota el `sessionId` o la ruta que muestre.
5. Repite para todos los ejercicios. Cada corrida genera su par JSON+CSV.

### 3.1 Corrida de control negativo (1 por dispositivo)

Haz **una** corrida E6 deliberadamente "mala": responde antes de la señal (anticipaciones) y/o
deja pasar deadlines (omisiones). **Debe** clasificarse `LOW` o `CONTAMINATED` con razones en el
`QualityDecisionLog`. Esto prueba que el detector de calidad funciona en hardware real, no solo
en simulación.

---

## 4. Captura de los DebugExport (adb)

Los archivos viven en almacenamiento **privado** de la app:
`filesDir/speed_debug_exports/` → `/data/data/com.mindos.app.debug/files/speed_debug_exports/`.
Nombres: `speed_run_<sessionId>.json` y `speed_trials_<sessionId>.csv`.

Como el APK es debuggable, usa `run-as` (no requiere root):

```bash
# Listar lo exportado
adb exec-out run-as com.mindos.app.debug ls -l files/speed_debug_exports/

# Sacar TODO a tu PC, a una carpeta por dispositivo (¡usa un nombre claro!)
mkdir -p validation/P1_pixel8_api35
adb exec-out run-as com.mindos.app.debug tar -c -C files speed_debug_exports \
  | tar -x -C validation/P1_pixel8_api35
```

> Si tu `tar` de Windows da problemas, alternativa archivo a archivo:
> ```bash
> adb exec-out run-as com.mindos.app.debug cat files/speed_debug_exports/speed_run_<id>.json > validation/P1.../speed_run_<id>.json
> ```

**Privacidad:** estos JSON/CSV contienen marcas temporales y conducta sensibles (§16). Guárdalos
solo localmente, no los subas a repos públicos ni los pegues en chats. Bórralos al cerrar el gate.

---

## 5. Cómo leer los datos (qué verificar en cada export)

Cada `speed_run_*.json` es un `DebugExportEnvelope`: `metadata` + `run`.

### 5.1 Campos clave de `metadata`

| Campo | Qué mirar | Criterio |
|---|---|---|
| `timebase` | base temporal | **siempre** `ANDROID_UI_UPTIME_NANOS` (acceptance #1/#16) |
| `inputTimestampPrecision` | nanos nativos vs derivados | P1 ⇒ `NANOS_NATIVE`; P2 ⇒ `MILLIS_DERIVED` (acceptance #2) |
| `deviceTimingRisk` | riesgo de **hardware** | P1 `LOW`, P2 `MODERATE`; `HIGH` solo si hubo jitter crítico |
| `sessionQuality` | calidad **combinada** | corridas normales ⇒ `HIGH`/`ACCEPTABLE`; la de control ⇒ `LOW`/`CONTAMINATED` |
| `metricsVersion` / `debugExportVersion` | versionado | `1.0.0` / `v0` |
| `thresholds` | umbrales activos | anticipación `100_000_000` ns, lapse `2.5`, cv `0.30`, etc. |

### 5.2 Sanidad de los trials (en el `.csv`, columna `rtNanos`)

El CSV tiene una fila por trial. Columnas relevantes: `rtNanos`, `isCorrect`, `isAnticipation`,
`isLapse`, `inputTimestampPrecision`, `timestampSource`, `timebase`.

- **`rtNanos > 0`** en todos los aciertos (nunca negativo ni 0). Un negativo significaría mezcla de
  bases temporales → **FALLO duro**.
- **RT plausibles para RT simple (E1):** la mediana humana ronda **~250–450 ms** (`250_000_000`–
  `450_000_000` ns). Si la mediana sale < 120 ms de forma sistemática ⇒ casi todo `isAnticipation`
  ⇒ sospecha de reloj/onset mal capturado, **no** de un humano sobrehumano.
- **`timebase` = `ANDROID_UI_UPTIME_NANOS`** y `timestampSource` coherente en todas las filas.
- En **P1** las filas deben decir `NANOS_NATIVE`; en **P2**, `MILLIS_DERIVED`. Un dispositivo que
  reporte la precisión equivocada para su API ⇒ **FALLO**.

### 5.3 Lectura rápida con `jq` (opcional)

```bash
jq '.metadata | {timebase, inputTimestampPrecision, deviceTimingRisk, sessionQuality}' speed_run_*.json
```

---

## 6. Criterio Go / No-Go (§15)

El gate **PASA** solo si **todo** lo siguiente es cierto:

- [ ] **P1 (API 34+)**: ≥1 corrida con `inputTimestampPrecision = NANOS_NATIVE`,
      `deviceTimingRisk = LOW`, `sessionQuality ∈ {HIGH, ACCEPTABLE}`, `rtNanos` plausibles.
- [ ] **P2 (API < 34)**: ≥1 corrida con `inputTimestampPrecision = MILLIS_DERIVED`,
      `deviceTimingRisk = MODERATE`, calidad `ACCEPTABLE` o mejor, `rtNanos` plausibles.
- [ ] **P3 (gama media/baja)**: corridas sin `FRAME_JITTER_DETECTED` sistemático; si aparece
      `HIGH`/`CONTAMINATED` por jitter de forma **reproducible**, es un **hallazgo a corregir**,
      no un PASS.
- [ ] **P4 (90/120 Hz)**: el Choreographer se mantiene estable a alto Hz (sin jitter crítico).
      *O bien* documentado como **no disponible** en el laboratorio (desviación explícita).
- [ ] **Control negativo**: la corrida "mala" de §3.1 salió `LOW`/`CONTAMINATED` con razones
      pobladas en `QualityDecisionLog` en **todos** los dispositivos.
- [ ] **Ningún** `rtNanos` negativo o cero en aciertos, en ningún dispositivo.

**No-Go** si cualquier casilla falla. Anota el síntoma exacto (dispositivo, ejercicio,
`sessionId`, campo) y trátalo como bug antes de reintentar — no relajes el criterio.

### Plantilla de registro

Rellena una fila por corrida en
[`PHYSICAL_VALIDATION_RECORD.csv`](PHYSICAL_VALIDATION_RECORD.csv) (misma carpeta).

---

## 7. Qué pasa al cerrar el gate (lo hace la IA, con tu PASS)

Cuando entregues un PASS con los datos de §6, el repo hará **exactamente** esto (cambios
pequeños y revisables, con tests):

1. **Calibrar la referencia de velocidad útil** — hoy `usefulSpeedReferenceVce = 5.0` es un
   placeholder no calibrado en
   [`SpeedScoreEngine.kt`](../../shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/score/SpeedScoreEngine.kt)
   (`SpeedScoreConfig`). Se fijará a partir de la **VCE observada** en las corridas reales
   (p. ej. percentil propio de las corridas válidas), y se revisarán los umbrales de
   macro-clasificación (`fastControlMax`, `preciseControlMin`, etc.) contra los patrones reales.
2. **Retirar el rótulo "preliminar"** — `CognitiveProfileScore.preliminary` pasa de `true` fijo a
   reflejar el estado real; el banner `DASHBOARD_DISCLAIMER` y `MVP_NOTE` se reescriben para
   lenguaje **funcional no clínico** (siguen prohibidos claims diagnósticos/percentil poblacional,
   §9 / acceptance #20).
3. **Versionar** — `SpeedVersions.SCORE_VERSION` pasa de `1.0.0-mvp3-preliminary` a una versión
   calibrada (p. ej. `1.0.1-mvp3`) en
   [`MetricThresholds.kt`](../../shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/MetricThresholds.kt).
   (Pendiente menor relacionado: `STAIRCASE_VERSION` no se bumpeó para el multi-señal de MVP-2.)
4. **Actualizar** este doc a "GATE CERRADO" con fecha y resumen de evidencia, y la memoria de
   proyecto `velocidad-mental-mvp-status`.

> **Límite de producto:** aun calibrado, el perfil sigue siendo un **perfil cognitivo funcional
> no clínico**. Cerrar §15 levanta el "preliminar/no-validado-en-dispositivos", **no** habilita
> diagnósticos, percentiles poblacionales ni los 7 perfiles avanzados (eso es Post-MVP, §13/§11/§14,
> y requiere tu OK explícito).

---

## 8. Si NO tienes los dispositivos ahora

El gate es intrínsecamente hardware-dependiente; no hay atajo válido. Opciones legítimas:

- **Emuladores** para P1 y P2 (un AVD API 35 y un AVD API 30) cubren la verificación de la **ruta
  de precisión** (`NANOS_NATIVE` vs `MILLIS_DERIVED`) y la sanidad de `rtNanos`, pero **no** el
  jitter real de P3 ni el alto Hz de P4 (un emulador no representa el panel físico). Sirve como
  validación **parcial**: documenta qué quedó cubierto por emulador y qué sigue pendiente de
  hardware real.
- **Diferir** el cierre y mantener el rótulo "preliminar" (estado actual, ya seguro y enviable).

Lo que **no** es válido: marcar el gate como cerrado sin evidencia de los 4 perfiles (o su
desviación documentada). El rótulo "preliminar" existe precisamente para no afirmar lo que no se
ha medido.
