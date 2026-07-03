# Platform Matrix — feasibilidad por plataforma y agente

Entorno del auditor: **Windows 11**. Dispositivo/emulador Android: **disponible**. macOS/Xcode: **no**.

## Targets KMP declarados

| Target  | Módulo app    | Source set      | Build local Windows | Ejecución/UI testable |
|---------|---------------|-----------------|---------------------|------------------------|
| Android | `:androidApp` | `androidMain`   | Sí (`assembleDebug`)| Sí (emulador/device ✓) |
| Desktop | `:desktopApp` | `desktopMain`   | Sí (`run`/`desktopTest`) | Sí (JVM) |
| iOS     | `iosApp/` (shell, sin .xcodeproj) | `iosMain` | **No** (necesita macOS) | **No** (solo estático) |

> Nota: `:shared` usa el plugin nuevo `com.android.kotlin.multiplatform.library` (AGP 9):
> compilar Android del módulo es `:shared:compileAndroidMain` (no `compileDebugKotlinAndroid`).
> Desktop: `:shared:compileKotlinDesktop`, tests JVM: `:shared:desktopTest`, estilo: `:shared:detekt`.

## Agente × estado en este entorno

| Agente               | Estado        | Por qué / cómo |
|----------------------|---------------|----------------|
| architecture-agent   | ejecutable    | Estático sobre el repo |
| build-agent          | ejecutable    | `:shared:desktopTest`, `:shared:detekt`, `:shared:compileAndroidMain`, `:androidApp:assembleDebug` |
| navigation-agent     | ejecutable    | Estático: AppRoute/AppRouteStack + *Screen.kt |
| api-agent            | ejecutable    | Estático: repos gitlive Firebase + WalletApiClient (callables) |
| state-agent          | ejecutable    | Estático: StateHolders, Flows, SecureKeyValueStore |
| security-agent       | ejecutable    | Estático: secretos, reglas, tokens, App Check (mayoría ya endurecido — ver CLAUDE.md) |
| ui-agent             | ejecutable    | Estático: estados vacío/loading/error; dark mode forzado |
| performance-agent    | ejecutable    | Estático: recomposición, trabajo en main thread |
| test-automation-agent| ejecutable    | Estático: cobertura existente (commonTest/desktopTest) + propuesta |
| triage-agent         | ejecutable    | Consolidación de findings |
| android-agent        | **needs re-gate** | Device disponible → `installDebug` + `adb logcat`. Requiere presupuesto. |
| auth-agent (live)    | **BLOCKED**   | Faltan credenciales de prueba. Estático sí. |
| ios-agent (live)     | **BLOCKED**   | Sin macOS/Xcode. Solo revisión estática de `iosMain`. |
