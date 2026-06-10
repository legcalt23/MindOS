# Fase 3 — Notas de ejecución (T3.1 y T3.4 con dependencias externas)

**Fecha:** 2026-05-29

T3.2 (Splash API + predictive back) y T3.3 (auditoría de paginación + fix de límites DM) se
ejecutaron y verificaron. T3.1 y T3.4 dependen de recursos externos al entorno actual (Windows,
sin dispositivo; proyecto Firebase de producción) y se documentan aquí.

---

## T3.1 — Profiling de cold start / memoria (MANUAL — requiere dispositivo)

**Estado:** no ejecutable en este entorno (Windows sin emulador/dispositivo). Es una tarea de
**medición**, cuyo entregable son métricas reales de un dispositivo — no hay código que escribir.

**Contexto existente:** ya hay baseline profiles en `androidApp/release/baselineProfiles/{0,1}`
(mejoran el cold start al precompilar rutas calientes con AOT).

**Runbook (ejecutar en macOS/Linux/Windows CON dispositivo o emulador):**

1. **Cold start (manual rápido):**
   `adb shell am force-stop com.mindos.app`
   `adb shell am start -W -n com.mindos.app/com.mindos.app.MainActivity`
   → leer `TotalTime` / `WaitTime` (objetivo: TTID < 1000 ms en gama media).

2. **Macrobenchmark (recomendado, repetible):** añadir un módulo `:macrobenchmark` con
   `androidx.benchmark:benchmark-macro-junit4`, escribir `StartupBenchmark` (COLD/WARM) y
   `BaselineProfileGenerator`. Requiere un dispositivo físico (los benchmarks no corren fiables en
   emulador). Esto es **infraestructura de test → pertenece a Fase 5**; aquí solo se referencia.

3. **Memoria / leaks:** Android Studio Profiler (Memory) en sesiones de navegación profunda
   (DMChat → MediaViewer → back x N); confirmar que el back stack libera. Considerar LeakCanary
   solo en `debug`.

4. **Recomposición:** Layout Inspector → "Recomposition counts" en las god-screens
   (`ProfileScreen`, `GlobalFeedScreen`) — cruzar con T2.5.

**Acción:** ejecutar el runbook en un dispositivo antes del beta. No bloquea código de Fase 3.

---

## T3.4 — applicationIdSuffix ".debug" (GATED — requiere app Firebase debug)

**Estado:** NO aplicado, para no romper el build. Documentado con la dependencia exacta.

**Por qué no se aplica directamente:** `google-services.json` contiene un único cliente con
`package_name = com.mindos.app`. Si se añade `applicationIdSuffix = ".debug"` al build type
`debug`, el applicationId pasa a `com.mindos.app.debug` y el plugin `com.google.gms.google-services`
**falla el build** con *"No matching client found for package name 'com.mindos.app.debug'"*.
Romper `assembleDebug` violaría la condición de ejecución #4.

**Prerrequisito (acción de configuración, afecta al proyecto Firebase de producción `mindos-8452f`):**
1. Registrar una app Android `com.mindos.app.debug` en el proyecto Firebase.
2. Añadir su cliente al `google-services.json` (o colocar un `androidApp/src/debug/google-services.json`).
3. Verificar App Check debug (ya hay `firebase-appcheck-debug` en debug) para el nuevo id.

**Cambio de código pendiente (trivial, tras el prerrequisito):**
```kotlin
buildTypes {
    getByName("debug") {
        applicationIdSuffix = ".debug"
        versionNameSuffix = "-debug"
    }
}
```

**Decisión requerida del usuario:** ¿registro la app debug vía el Firebase MCP conectado
(acción aditiva y reversible sobre el proyecto de producción), o lo gestionas tú en la consola?
No tomo acciones que modifiquen el proyecto Firebase de producción sin confirmación explícita.

**Beneficio cuando se desbloquee:** instalar debug + release simultáneamente; la build de
desarrollo deja de contaminar los datos/analytics del proyecto del id de producción.
