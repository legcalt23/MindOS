# Handoff — Opción A: validar RT humanos en emulador con ventana (Velocidad Mental §15)

> Self-contained. La sesión nueva NO tiene memoria de la conversación previa. Ejecuta esto tal cual.

## Objetivo
Validar el pipeline **end-to-end con cognición humana real** del ejercicio **E1 · Destello** de MindBoost
Velocidad Mental: lanzar el emulador **con ventana visible**, dejar que **el USUARIO toque** los destellos
(no taps scriptados), capturar el `DebugExport` y verificar que los **RT son humanos plausibles
(~250–450 ms)** y la `sessionQuality` sube a `ACCEPTABLE`/`HIGH`. Las corridas previas (P1/P2) usaron
`adb input tap` → RT ~1.5 s (basura); esto cierra ese hueco.

**NO** es una validación de jitter/alto-refresco (el build MVP-0 no los mide: `criticalFrameJitter`/
`frameDropFraction` están hardcoded a `false`/`0`, y `deviceTimingRisk` sale solo de la precisión por API).

## Entorno (ground truth)
- Repo: `C:\Users\curro\AndroidStudioProjects\MindOS`
- SDK: `C:\Users\curro\AppData\Local\Android\Sdk` · adb: `platform-tools\adb.exe` · emulator: `emulator\emulator.exe`
- App debug: paquete **`com.mindos.app.debug`** (`applicationIdSuffix=.debug`). Autentica contra Firebase
  real: `androidApp/src/debug/google-services.json` ya tiene config real de `com.mindos.app.debug` en
  proyecto `mindos-8452f`, con SHA-1 debug `9B:A5:F9:33:82:D7:F4:EC:2C:55:6D:B7:55:85:35:B9:6B:81:A7:B0`
  registrado + token App Check debug autorizado.
- APK ya construido: `androidApp/build/outputs/apk/debug/androidApp-debug.apk` (si falta: `./gradlew :androidApp:assembleDebug`).
- AVDs existentes: `Medium_Phone_API_36.1` (API 36 → `NANOS_NATIVE`/`LOW`), `speed_p2_api30` (API 30 → `MILLIS_DERIVED`/`MODERATE`).
- Cuenta de test: **mindverse@mv.com** / **123456789** (funciona). Puede haber sesión persistida.

## Pasos
1. **Lanza el emulador CON ventana** (clave: sin `-no-window`, para que el usuario vea y toque):
   `emulator -avd Medium_Phone_API_36.1 -gpu swiftshader_indirect -no-snapshot` (vía `Start-Process`, detached).
   Espera boot: `adb wait-for-device` + `getprop sys.boot_completed == 1`.
2. Instala: `adb install -r -d androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
3. Lanza app: `adb shell monkey -p com.mindos.app.debug -c android.intent.category.LAUNCHER 1`.
4. Si cae en pantalla de Auth (MindOS · Acceso seguro): rellena email/contraseña y pulsa **Entrar**.
   (Coordenadas vía `adb shell uiautomator dump` + parse de `bounds`; resolución varía por AVD.)
   Login OK en logcat = `FirebaseAuth: ... login_success`.
5. Navega: Home → toca card **"MINDBOOST · ENFOQUE"** → hub → **"Velocidad Mental"** → **"E1 · Destello"** → **"Iniciar bloque"**.
   (Usa `uiautomator dump` para localizar cada texto; tap al centro de sus `bounds`.)
6. **PIDE AL USUARIO QUE TOQUE**: "Toca el centro de la pantalla en cuanto veas el destello verde
   («¡Ahora!»), reacciona natural. Son 12 destellos (1 bloque). E1 no tiene límite: el destello espera tu toque."
   NO toques tú por adb — el objetivo son RT humanos. Espera a que el usuario complete el bloque
   (aparece "Bloque completado.").
7. En la pantalla Finished, pulsa **"Exportar datos brutos (JSON/CSV)"** (vía `uiautomator dump` → tap).
8. Saca los ficheros (privados; `run-as`):
   `adb exec-out run-as com.mindos.app.debug ls files/speed_debug_exports/`
   `adb exec-out run-as com.mindos.app.debug cat files/speed_debug_exports/speed_run_<id>.json`
   `adb exec-out run-as com.mindos.app.debug cat files/speed_debug_exports/speed_trials_<id>.csv`
   Guárdalos en `validation/` (gitignored).

## Verificación (criterio PASS)
- `metadata.timebase = ANDROID_UI_UPTIME_NANOS`
- `metadata.inputTimestampPrecision = NANOS_NATIVE` (API 36) — o `MILLIS_DERIVED` si usas el AVD API 30
- `metadata.deviceTimingRisk = LOW` (o `MODERATE` en API 30)
- **Todos los `rtNanos > 0`** y `rtNanos = responseUptimeNanos − stimulusUptimeNanos` (comprueba la resta en 1–2 trials)
- **CLAVE: `rtMedian` humano-plausible (~250–450 ms)** y **`sessionQuality = HIGH`/`ACCEPTABLE`** (no `LOW`).
  Si sale `LOW` por CV alta, es que los toques fueron erráticos — repetir, no es fallo del código.

## Registrar resultado
Añade el resultado (PASS + valores) a la tabla "Resultados registrados" de
`docs/mindboost_velocidad_implementation_plan/PHYSICAL_VALIDATION_GATE.md` (fila nueva tipo
"P1-humano / RT plausibles"). Actualiza también la memoria de proyecto `velocidad-mental-mvp-status`.

## Guardrails (NO violar)
- **NO** quitar `CognitiveProfileScore.preliminary = true` ni calibrar el score (gate §15 sigue abierto; calibración es posterior).
- **NO** commitear `validation/` (gitignored; contiene uids/emails reales).
- **NO** tocar código de producto para esto (es validación, no feature).
- PowerShell: para capturas, usar `adb shell screencap -p /sdcard/x.png` + `adb pull` (NO `adb exec-out screencap > file`: PowerShell corrompe el binario a UTF-16).
- Evitar en una misma línea PowerShell el token `rm` junto a regex `\d+` (un hook lo bloquea); usa `[0-9]` y no borres `/sdcard/ui.xml`.
