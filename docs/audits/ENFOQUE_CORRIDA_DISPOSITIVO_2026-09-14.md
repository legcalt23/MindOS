# ENFOQUE · Corrida observada en dispositivo Android — runbook + dossier

**Estado:** corridas 1 y 2 **EJECUTADAS EN EMULADOR con el flag ON** el 2026-09-14 (§7.2: las dos frases esperadas
observadas; **evidencia parcial, no es GO** — §0: emulador con drivers guionizados, no dispositivo con un humano);
ensayo previo con el flag OFF en §7.1. **Sigue pendiente** la corrida en dispositivo físico por el fundador
(dispositivo y credenciales son suyos) y la corrida iOS (no ejecutable en el host Windows de la sesión).
**Escribe el resultado:** el fundador, en §7 de este fichero y en `PORTFOLIO_RUNTIME_EVIDENCE.json` (§5).
**Ledger:** `MB-FOCUS-0165` (sink), `MB-FOCUS-0101` (gate de la sonda), `MB-FOCUS-0116` (decisión de encendido),
`MB-PORTFOLIO-0024` (gate b), `MB-FOCUS-0091`, `MB-FOCUS-0135`. Informe: `AUDITORIA_ADVERSARIAL_ENFOQUE_2026-08-29.md` §28 (Q10 PROVISIONAL).

## 0. Alcance y techos (leer antes de tocar nada)

- **Carril:** Enfoque v1, el que monta el hub (`MindBoostHubScreen.kt:117` → `FocusRootScreen`), ejercicio `E1_VIGIA`
  («Vigía»). **No es** `S5_PASO6_PHYSICAL_PROTOCOL.md` (campaña de timing del carril `focus2`, que el hub no monta)
  y `FOC2-E01` **no es** `E1_VIGIA`. Esta corrida no produce evidencia de tiempos de reacción ni de precisión
  oficial: sólo observa que el sensor de primer plano funciona en un dispositivo real y que el gate de admisión
  produce `Admitted` e `INTERRUPTED` cuando debe.
- **Qué cierra:** una sesión `Admitted` y una `NotMeasurable(INTERRUPTED)` observadas en Android con el sensor
  encendido satisfacen el gate de `MB-FOCUS-0101`, la segunda mitad de `MB-FOCUS-0165` y el gate (b) de
  `MB-PORTFOLIO-0024`, y levantan el gatillo PROVISIONAL de la pregunta 10 (§28).
- **Emulador:** evidencia parcial, nunca un GO (misma regla que `S5_PASO6` §8 y `PHYSICAL_VALIDATION_GATE.md`
  «ruta de precisión»). Si sólo hay emulador, registrar la corrida igual y decirlo en `provenance`.
- **Fuera de alcance:** `MB-FOCUS-0155` (declaración del CEO) no cambia con esta corrida.

## 1. Precondiciones

1. **Dispositivo Android físico** y build **debug** (`com.mindos.app.debug`, configuración Firebase real en
   `androidApp/src/debug/google-services.json`; token de App Check debug registrado en el proyecto `mindos-8452f`
   — pasos en `PHYSICAL_VALIDATION_GATE.md` §2.1; borrar datos de la app genera un token nuevo).
2. **Procedencia del árbol:** anotar `git rev-parse HEAD` y la huella de `git status --porcelain` antes de compilar
   (`.\gradlew.bat :androidApp:assembleDebug`); calcular el SHA-256 del APK instalado
   (`androidApp/build/outputs/apk/debug/androidApp-debug.apk`).
3. **Sensor encendido sin tocar código:** en Firebase Remote Config del proyecto `mindos-8452f` crear/publicar el
   parámetro booleano **`focus_lifecycle_probe_enabled` = `true`**. Mecanismo (`MindOSApplication.kt`):
   `RemoteConfigFeatureFlags.focusLifecycleProbeEnabled` lee `boolOrDefault(KEY_FOCUS_LIFECYCLE_PROBE, false)`
   (`:151-155`, `:163-166`): sin valor remoto la fuente es `STATIC` y cae al `false` compilado
   (`FeatureFlags.kt:132`); con valor remoto activado se honra. La clave **no** está en `setDefaultsAsync`
   (`:34-46`) — es inocuo: `boolOrDefault` trata DEFAULT y REMOTE igual. Desktop e iOS quedan inertes aunque el
   flag sea `true` (`NoOpAppLifecycleProbe` → `NO_PLATFORM_SOURCE`): sólo Android puede llegar a `Admitted`.
   - **Techo declarado (va a `MB-FOCUS-0116`):** nada en el código exige la «verificación en dispositivo» que
     `FeatureFlags.kt:94` y `MB-FOCUS-0101` ponen como precondición del encendido. Un flip de consola es
     **equivalente a un release** para todos los usuarios Android; la reversión es sólo por consola (borrar el
     parámetro devuelve `false` en el siguiente fetch). Si no se quiere encender para todos, usar una **condición**
     de Remote Config (p. ej. porcentaje 0 % + inclusión manual, o versión de app) — decisión de `0116`, no de este
     runbook.
4. **Secuencia de activación (obligatoria):** `fetchAndActivate()` se dispara sin esperar en el arranque
   (`MindOSApplication.kt:48`) y el flag se lee **en vivo en cada composición**
   (`RememberFocusStateHolders.kt:89`), con el resultado como clave de `remember` de los holders
   (`:52`, `:71`, `:113`). Por tanto: instalar → abrir la app → esperar ≥ 30 s en la Home → **cerrar del todo**
   (matar la app) → volver a abrir → **sólo entonces** entrar en MindBoost → Enfoque. Si la activación aterriza a
   mitad de una sesión, el holder se recrea y esa sesión se pierde en silencio. Si el parámetro se publicó
   después del primer arranque y el fetch no lo trae (intervalo mínimo por defecto del SDK), borrar los datos de
   la app y repetir la secuencia. **Medido en §7.2:** el intervalo mínimo es de **12 h**, así que un parámetro publicado
   después del primer arranque no llega con relanzar; borrar datos obliga a repetir la línea base (punto 5). En un
   build **debug** hay una vía sin borrar nada: force-stop + poner `last_fetch_time_in_millis` a `-1` en
   `shared_prefs/frc_<app-id>_firebase_settings.xml` con `adb shell run-as com.mindos.app.debug` y relanzar; el
   fichero `files/frc_<app-id>_firebase_activate.json` confirma la activación (valor y `template_version_number_key`).
5. **Línea base existente (premisa que §1 omitía; medida en el ensayo de §7):** la sesión de entrenamiento de §2
   **no es alcanzable** mientras `profile.baseline == null`: la Home de Enfoque muestra sólo «Empezar mi base»
   (`FocusHomeStateHolder.kt:88` `needsBaseline = profile.baseline == null`; `FocusHomeScreen.kt:158-175`
   sustituye duración, «Empezar» y «Evaluación →» por ese CTA). Un perfil nuevo (o uno con datos borrados para
   renovar el token de App Check, punto 1) tiene que completar primero **«Empezar mi base»**: cuatro bloques
   seguidos sin pausa (`BaselineProtocol.kt:85-116`: Vigía 75, Filtro 60, Gatillo 60 y la sonda `VIGILANCE_PROBE`
   55 ensayos, que se juega como Vigía), unos 16 minutos. **El bloque de sonda de la propia línea base pasa por el
   mismo gate de admisión que las corridas 1 y 2**, así que el flag tiene que estar ya activado ANTES de la base:
   con el sensor apagado la admisión es `LIFECYCLE_NOT_OBSERVED`, `persist()` graba igualmente la forma reservada
   como administrada (`FocusBaselineStateHolder.kt:213` `recordProbe`, comentario «Recorded either way») y guarda el
   perfil con `baseline = null` (`:221`), es decir: **16 minutos, una de las cuatro formas reservadas gastada y la
   Home igual que antes**. Orden correcto: punto 3 (publicar) → punto 4 (secuencia de activación) → «Empezar mi
   base» → §2 → §3. Una base con el flag encendido dibuja la siguiente forma no gastada del cursor.

## 2. Corrida 1 — sesión `Admitted`

1. Home → MindBoost → tarjeta **«Enfoque»** → **«Vigía»** → duración **«Estándar»** (10 min, ≈ 94 ensayos;
   «Mantenimiento» son ≈ 55 y también supera el mínimo de 20, pero da menos margen para métricas confiables).
2. Jugar la tarea de forma honesta hasta el final: responder a las señales, retener en el resto; **no** salir de
   la app, **no** bloquear la pantalla, **no** atender notificaciones. Recomendado: modo No molestar.
3. **Esperado en la pantalla de resumen**, en la nota final (`FocusSummaryScreen.kt:113` renderiza
   `FocusReport.honestNote`; el texto lo produce `FocusSessionAssembler.kt:222-234`):
   - `Admitted` con métricas confiables → la frase empieza por **«Esto describe tu práctica de hoy en este
     ejercicio»**.
   - `Admitted` sin métricas confiables (< 20 ensayos o ninguna métrica `confident`) → **«Esta sesión es demasiado
     corta para sacar conclusiones.»** Cuenta como `Admitted` (el gate pasó), pero repetir con «Desarrollo» (20 min)
     para una evidencia limpia.
   - **«Esta sesión no cuenta para tu progreso: no pudimos comprobar que la app siguiera en primer plano.»** →
     el flag **no** estaba activo en esa lectura (`LIFECYCLE_NOT_OBSERVED`); volver a §1.4. Con el flag apagado
     esta frase es lo que ve TODO Android hoy.
   - «…: no hubo ninguna respuesta.» → no se respondió a nada; la precedencia es
     `INTERRUPTED` > `NO_RESPONSES` > `RESPONSE_PATTERN_NOT_INTERPRETABLE` > `LIFECYCLE_NOT_OBSERVED`
     (`SessionQualityGate.kt:55-64`).

## 3. Corrida 2 — `NotMeasurable(INTERRUPTED)`

1. Misma ruta y misma duración.
2. A mitad del ejercicio, sacar la app de primer plano durante ≥ 5 s (recibir una llamada, o pulsar Home) y
   **volver a la app y completar la sesión hasta el resumen**. **No abandonar:** el botón de atrás/abandonar
   produce `Aborted("Sesión interrumpida")` sin fila de historial ni admisión (`FocusSessionStateHolder.kt:199-215`),
   que no es evidencia de nada. La ventana observada es la sesión entera (`start` en `:252`, `stop` en `:392`).
3. **Esperado:** **«Esta sesión no cuenta para tu progreso: la app salió de primer plano durante el ejercicio.»**
   `INTERRUPTED` gana a las demás razones: aunque no se responda a nada durante la llamada, la frase sigue siendo
   esta.

## 4. Qué transcribir (y qué no)

**Transcribir a mano** en §7 y en la entrada de `runs` (§5): fecha y hora UTC de fin de cada sesión; modelo del
dispositivo y nivel de API; variante (`com.mindos.app.debug`); `HEAD`; SHA-256 del APK; ejercicio y duración
elegida; el `honestNote` **verbatim**; valor del parámetro en la consola (`true`) y hora de publicación.

**No transcribir ni subir a git:** correo de la cuenta, capturas de pantalla, exportaciones crudas
(`.gitignore:97-100`, `MB-FOCUS-0068`), el token de App Check ni ningún valor cuyo nombre contenga `token`,
`secret` o `credential` (el escáner de secretos los rechaza).

**No usar como evidencia:** el contador de sesiones ni la racha de la Home de Enfoque — suben aunque la sesión
sea `NotMeasurable` (`FocusSessionStateHolder.kt:424-428`). Enfoque no tiene DebugExport ni pantalla que muestre la
admisión: la admisión se persiste cifrada (`FocusDtos.kt:78-80`) pero el **único** discriminador visible para el
fundador es la frase final del resumen.

## 5. Registro en la autoridad `PORTFOLIO_RUNTIME_EVIDENCE.json`

Añadir a `runs` **una entrada por corrida**. Campos obligatorios: `slotId`, `codeExerciseId`, `platform`,
`snapshotSha256`, `testOrDossier`, `result`, `provenance`, `executedAtUtc` (`validate-portfolio.mjs:205`), más
`evidencePath` cuando `result` es `VERIFIED` (`:209`); `slotId` debe existir en `MINDBOOST_EXERCISE_PORTFOLIO.csv`
(`:210`) y `codeExerciseId` debe coincidir con el del slot (`portfolio-state-model.mjs:216`). `result` pertenece al
enum cerrado `NOT_RUN | VERIFIED | FAILED | BLOCKED_PLATFORM` (`portfolio-state-model.mjs:22`). `snapshotSha256` es
sólo procedencia: ningún validador comprueba qué hashea; aquí es el SHA-256 del APK instalado.

```json
{
  "slotId": "FOCUS-01",
  "codeExerciseId": "E1_VIGIA",
  "platform": "android",
  "snapshotSha256": "<sha256 del APK instalado>",
  "testOrDossier": "docs/audits/ENFOQUE_CORRIDA_DISPOSITIVO_2026-09-14.md",
  "evidencePath": "docs/audits/ENFOQUE_CORRIDA_DISPOSITIVO_2026-09-14.md#7-dossier",
  "result": "VERIFIED",
  "provenance": "com.mindos.app.debug; HEAD <sha>; <modelo> API <n>; Remote Config focus_lifecycle_probe_enabled=true; corrida 1 Admitted + corrida 2 INTERRUPTED",
  "executedAtUtc": "<AAAA-MM-DDThh:mm:ssZ de la segunda sesión>"
}
```

- `VERIFIED` = las dos frases esperadas se observaron. `FAILED` = alguna no coincidió (registrar igual, con la
  frase real en `provenance`). No registrar `NOT_RUN`: la ausencia de entradas ya lo dice.
- Validar: `node scripts/portfolio/validate-portfolio.mjs --mode=repo --json` → `RUNTIME_PLAYABILITY_GATE` debe
  ser `PASS` («N run(s) recorded»). Los demás gates en rojo son ajenos a esta corrida (conjunto de salidas V2d
  ausente, `MB-PORTFOLIO-0007`). Comprobado el 2026-09-14 con esta plantilla exacta: `PASS`; sin `evidencePath`,
  con un `slotId` desconocido o sin `executedAtUtc`: `FAIL` con el mensaje que nombra el campo.

## 6. Cierre en el ledger (sesión principal, después de la corrida)

Con la entrada registrada y este §7 relleno: `MB-FOCUS-0101` → `CLOSED` (evidencia de dispositivo + flag +
sesión `Admitted`); `MB-FOCUS-0165` → `CLOSED`; `MB-PORTFOLIO-0024` gate (b) satisfecho (el resto sigue `OPEN`);
`MB-FOCUS-0091` y `MB-FOCUS-0135` se reevalúan con la corrida como evidencia; §28 del informe retira el
PROVISIONAL de Q10 si procede. **Con evidencia de EMULADOR (§7.2) ninguno de esos cierres procede:** `0101` sigue
`OPEN`, `0165` sigue `MITIGATED` y la corrida queda como metadato en sus filas; la corrida en dispositivo es la que
los cierra. **Riesgo residual de `0165` resuelto el 2026-09-14:** `evidence-mutation-tests.mjs` (P3-11) ya no exige
`runs.length === 0`; mide sobre la copia sandbox que las corridas reales por sí solas NO disparan G10 y sólo entonces
muta (una corrida real deja de poner el suite en ROJO).

## 7. Dossier (EMULADOR, 2026-09-14 — evidencia parcial; la fila de dispositivo físico sigue pendiente)

| Corrida | Fin (UTC) | Dispositivo / API | Variante · HEAD · SHA-256 APK | Duración | `honestNote` verbatim | Resultado |
|---|---|---|---|---|---|---|
| 1, intento A (se esperaba `Admitted`) | 2026-09-14 13:19:25 (resumen observado; última respuesta 13:16:35) | EMULADOR AVD `Medium_Phone_API_36.1`, API 36.1 (`emulator-5554`, 1080x2400) | `com.mindos.app.debug` · `bdc252f` · `afbdec73f26ef578d59daccb9d9edee2339d205f6db097aa8cfe037e870b5285` | Vigía · Estándar («10 minutos de estándar»; ≈6 min de reloj) | «Esta sesión no cuenta para tu progreso: el patrón de respuestas no nos deja leerla como una medida. Es un registro de hoy, no una nota ni una conclusión sobre ti.» | `DENEGADA` (`RESPONSE_PATTERN_NOT_INTERPRETABLE`): driver `drv_all.sh` a ≈1 s por vuelta frente a una ventana de respuesta de 950-1400 ms; 10 respuestas, HUD 3 aciertos. No dice nada del sensor: ese veredicto precede a `LIFECYCLE_NOT_OBSERVED` (`SessionQualityGate.kt:55-62`). |
| 1, intento B (`Admitted`) | 2026-09-14 13:34:42 (resumen observado; última respuesta 13:34:07) | ídem | ídem | ídem | «Esto describe tu práctica de hoy en este ejercicio — influida por sueño, hora y ánimo. Por ahora se registra el entrenamiento; no se concluye nada sobre ti.» | `OBSERVADA` — frase de `Admitted`; driver `drv_fast.sh` (retrasos 139-298 ms), 16 respuestas, HUD 16 aciertos, 0 fallos, 0 falsas alarmas |
| 2 (`INTERRUPTED`) | 2026-09-14 13:42:55 (resumen observado) | ídem | ídem | ídem; `KEYCODE_HOME` 13:41:52 → launcher en primer plano 13:41:54 (`dumpsys activity`) → `am start` 13:42:04 (12 s en segundo plano) → `MainActivity` en primer plano 13:42:09 | «Esta sesión no cuenta para tu progreso: la app salió de primer plano durante el ejercicio. Es un registro de hoy, no una nota ni una conclusión sobre ti.» | `OBSERVADA` — frase de `INTERRUPTED`; 18 respuestas (17 antes de HOME, 1 después), HUD 18 aciertos, 0 fallos, 0 falsas alarmas; la sesión siguió tras volver y el resumen la retuvo |

Parámetro `focus_lifecycle_probe_enabled` publicado: **2026-09-14 12:54:19 UTC** (plantilla de Remote Config versión 1,
`firebase deploy --only remoteconfig --project mindos-8452f`, origen `REST_API`, `INCREMENTAL_UPDATE`) · condición:
`android_debug_app` (el `app.id` del build `com.mindos.app.debug`) → `true`; valor por defecto `useInAppDefault`, así que
release conserva el `false` compilado (`MB-FOCUS-0116` intacta) · observaciones: el SDK no refresca antes de 12 h, así
que la activación en el emulador exigió force-stop + `last_fetch_time_in_millis = -1` en
`shared_prefs/frc_..._firebase_settings.xml` (vía `run-as`) y relanzar (12:56:23); `files/frc_..._firebase_activate.json`
registra `focus_lifecycle_probe_enabled = "true"`, `template_version_number_key = 1`, fetch 12:55:25 UTC.

### 7.2 Corrida en EMULADOR con el flag ON (2026-09-14, evidencia parcial, no es GO)

- **Qué es:** ejecución completa de §1-§5 por la sesión principal en el emulador, con la publicación de Remote Config
  autorizada por el fundador («Sí, publica (solo debug)»). Prueba que la ruta corre con el sensor encendido y produce
  las frases esperadas; **no** prueba lo que §0 reserva al dispositivo (un humano jugando, timing real, App Check de
  producción). Capturas de pantalla y registros del driver quedan en el scratchpad de la sesión, fuera del repo (§4).
- **Entorno:** el de §7.1 (mismo AVD, mismo APK, misma sesión Firebase persistida, app no reinstalada) más el
  parámetro publicado y activado como se describe arriba. Reloj del dispositivo en UTC; el host va ≈3 s adelantado.
- **Línea base con el flag encendido (precondición de §1, punto 5):** «Empezar mi base» con `drv_all.sh` (una sola
  pasada honesta para los cuatro bloques: disco → centro 150-450 ms después; flecha → mitad por asimetría de tinta
  100-350 ms después) 12:58:46-13:12:10, 120 respuestas; «Base registrada» observada 13:12:31 con Umbral de lapso
  1270 ms, RT-CV 13 %, Ejercicios calibrados 3, Métricas de la prueba 14. Con el sensor encendido el bloque de sonda
  pasó la admisión (con el flag apagado, §7.1, no la había pasado) y dibujó la forma 1 del cursor, como predecía §1.
- **Corrida 1, intento A (denegada):** `drv_all.sh` lee una banda de 91 filas por vuelta (captura 357 ms + lectura
  532 ms + retraso 150-450 ms ≈ 1-1,5 s por respuesta) mientras E1 estrecha la ventana de respuesta de 1400 a 950 ms,
  baja el contraste de 1,0 a 0,4 y la rareza de 0,30 a 0,06 al subir la carga perceptual (`E1VigiaGenerator.kt`): a
  partir del primer aumento de dificultad el guion llega tarde, la precisión cae al techo ciego y el gate la retiene
  (`RESPONSE_PATTERN_NOT_INTERPRETABLE`, familia `MB-FOCUS-0146`/`0147`). Es el comportamiento diseñado, no un defecto;
  y como ese veredicto precede a `LIFECYCLE_NOT_OBSERVED`, el intento A **no atestigua el sensor** en ningún sentido.
- **Corrida 1, intento B (`Admitted`):** `drv_fast.sh` (banda de 5 filas, umbral de tinta más bajo, retraso 100-300
  ms; ≈0,4 s por vuelta) 13:28:46-13:34:42; 16 respuestas, 16 aciertos; frase de `Admitted` verbatim en la tabla.
- **Corrida 2 (`INTERRUPTED`):** misma app viva (no se mató el proceso), `KEYCODE_HOME` a los 295 s de sesión;
  `dumpsys activity activities` muestra el launcher como `topResumedActivity` durante 12 s y después `MainActivity`;
  la sesión continuó (HUD 17 → 18) y terminó a los 5 min 58 s con la frase de `INTERRUPTED`. Como `INTERRUPTED`
  precede a todo en la jerarquía del gate, este veredicto es decidible con independencia de la calidad del driver.
- **Registro:** dos entradas en `PORTFOLIO_RUNTIME_EVIDENCE.json` (corrida 1 y corrida 2, ambas `VERIFIED` por la
  frase, con `provenance` que dice EMULADOR y drivers guionizados); `RUNTIME_PLAYABILITY_GATE` `PASS`;
  `evidence-mutation-tests.mjs` verde con P3-11 reescrito (§6).
- **Qué vale y qué no:** vale como prueba de que la cadena publicación → activación → sensor → gate → frase funciona
  de punta a punta en Android, y como ensayo del propio runbook (dos hallazgos de procedimiento: el intervalo de fetch
  de 12 h y la latencia del driver frente a la ventana de respuesta). **No vale como GO** de `MB-FOCUS-0101` ni
  cierra `MB-FOCUS-0165`: emulador ≠ dispositivo y guion ≠ humano. iOS: sin macOS ni Xcode en el host, no ejecutable.

### 7.1 Ensayo previo — EMULADOR, flag OFF (2026-09-14, evidencia parcial, no es corrida)

- **Qué era:** ensayo de los drivers con el flag apagado para validarlos antes de publicar; el plan era abortar en el
  bloque 3 para no gastar una forma reservada. **No se abortó**: la sesión del operador quedó pausada por límite de uso
  entre el bloque 2 y el 3 y los drivers siguieron solos hasta el final de la base.
- **Entorno:** AVD `Medium_Phone_API_36.1` (`emulator-5554`, 1080x2400, reloj del dispositivo en UTC), build debug
  `com.mindos.app.debug`, HEAD `bdc252f` con árbol sucio (huella `git status --porcelain` `57bb47c7001e`), APK
  `androidApp-debug.apk` SHA-256 `afbdec73f26ef578d59daccb9d9edee2339d205f6db097aa8cfe037e870b5285` (md5 del APK
  instalado idéntico al del host). Sesión Firebase persistida del fundador; Remote Config sin publicar
  (`frc_..._firebase_activate.json` de 166 bytes, sin cambios) → `focusLifecycleProbeEnabled = false` compilado.
- **Drivers (scratchpad, no van al repo):** `drv_tap.sh` toca el centro del campo sólo cuando es el disco verde
  `FocusSignal`, 150-450 ms después de verlo, y espera a que deje de serlo (bloques Vigía / Gatillo / sonda);
  `drv_arrow.sh` lee la flecha central por asimetría de tinta en una banda de 91 filas y toca la mitad izquierda o
  derecha 100-350 ms después (bloque Filtro). Ambos son honestos por construcción (nunca responden sin estímulo) y
  ambos son un guion: lo que prueban es que la ruta corre, **no** que un humano la jugó.
- **Cronología (UTC del dispositivo):** app arrancada 10:38:24 (pid 10094, no muerta ni reinstalada en todo el
  ensayo); bloque 1 Vigía con `drv_tap.sh` 10:48:53-10:53:11 (14 respuestas, retrasos 162-436 ms); bloque 2 Filtro
  con `drv_arrow.sh` 10:54:14-10:55:56 (53 respuestas); bloques 3 Gatillo y 4 sonda con el mismo `drv_arrow.sh`
  (el disco entero cae en su banda) 10:56:00-11:01:55 (51 respuestas); `neu_secure_focus.xml` escrito a las 11:02.
- **Resultado:** la base terminó y **quedó sin línea base**: la Home vuelve a mostrar «EMPIEZA POR TU BASE» /
  «Empezar mi base» (`needsBaseline = true`, comprobado con `uiautomator` a las 12:49). Con el sensor apagado la
  admisión esperada es `LIFECYCLE_NOT_OBSERVED`; **no se capturó** la pantalla de aviso (la pausa del operador
  cubrió ese tramo) y la vuelta a la Home es una interacción externa no atribuida (ni el registro del driver tiene
  toques después de 11:01:55 ni el buffer `events` registra toques por pulsación). Consecuencia acotada al perfil
  del emulador: **forma reservada índice 0 de `VIGILANCE_PROBE` gastada**, quedan 3 de 4; una base con el flag
  encendido dibujará la forma 1.
- **Qué vale:** valida los dos drivers de punta a punta y refuta la premisa de §1 (punto 5). No satisface ningún
  gate de §0: sin flag no hay sensor y en emulador no hay GO.

