# Decisión: persistencia de datos cognitivos en iOS para el MVP

- **Fecha:** 2026-07-01
- **Estado:** ACEPTADA
- **Ámbito:** MindBoost — persistencia local cifrada de datos cognitivos sensibles
- **Tipo:** decisión de seguridad/privacidad + readiness de plataforma (MVP Fase 1, Prompt 1)

## Contexto

Los datos cognitivos de MindBoost (perfil, baseline, resultados de sesión, logs de
trials) son **sensibles** (pueden inferir rasgos funcionales del usuario) y se persisten
a través de la superficie `SecureKeyValueStore` (namespaces en
`MindBoostStorageNamespaces.AccountBound`: `mindboost_focus`, `mindboost_memory`,
`mindboost_speed`, `mindboost_logic`, `mindboost_creativity`, `mindboost_emotions`,
`mindboost_decisiones`, `mindboost_language`, `mindboost_profile`).

Estado real de la implementación por plataforma:

| Plataforma | `actual rememberSecureKeyValueStore` | Durabilidad | Cifrado en reposo |
|---|---|---|---|
| **Android** | AES-256-GCM, clave en Android Keystore (device-bound) | Durable | Sí (a nivel app) |
| **Desktop** | `InMemorySecureKeyValueStore` | Volátil (se pierde al cerrar) | N/A (no toca disco) |
| **iOS** | `InMemorySecureKeyValueStore` (stub) | Volátil (se pierde al cerrar) | N/A (no toca disco) |

Archivos:
- `shared/src/androidMain/.../focus/data/local/SecureKeyValueStore.android.kt` (real)
- `shared/src/iosMain/.../focus/data/local/SecureKeyValueStore.ios.kt` (in-memory stub)
- `shared/src/desktopMain/.../focus/data/local/SecureKeyValueStore.desktop.kt` (in-memory, dev/test)

**Consecuencia funcional en iOS:** todo el progreso cognitivo es *session-scoped* — se
pierde al reiniciar la app. El "perfil cognitivo" global nunca acumula los ≥20 trials/dimensión
necesarios para salir de `CALIBRATING`. Es decir: **en iOS hoy no hay producto de progreso**.

**Consecuencia de seguridad:** la degradación a memoria en iOS es **silenciosa** (sin
warning visible), lo que contradice la regla del repo "todo fallback de configuración crítica
para seguridad DEBE emitir un warning visible" (CLAUDE.md 2026-05-28).

## Decisión

1. **El MVP se lanza Android-first.** Android es el único target con persistencia cifrada
   durable y, por tanto, el único donde el perfil cognitivo acumula progreso real.

2. **iOS queda explícitamente marcado como BLOQUEANTE de lanzamiento iOS**, no como
   "listo con caveats". No se publica iOS hasta implementar y **verificar en macOS** un
   `SecureKeyValueStore.ios.kt` respaldado por **Keychain** (con protección
   `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`, sin backup a iCloud, device-bound —
   espejo del patrón AES/GCM+Keystore de Android).

3. **NO se implementa el store de Keychain en esta fase.** Razón: la toolchain actual es
   Windows sin Xcode; no se puede compilar el target iOS ni verificar la criptografía.
   **Publicar criptografía de datos sensibles sin verificar es un riesgo de seguridad
   inaceptable.** Implementarla a ciegas violaría además la regla del repo de no empezar
   cambios que no se puedan terminar/verificar.

4. **Desktop permanece in-memory** de forma permanente: es un target de dev/test, nunca una
   superficie de publicación. No debe persistir datos sensibles a un disco sin cifrar.

## Alternativas descartadas

- **Implementar Keychain ahora, a ciegas.** Descartada: sin `swift build`/Xcode local no hay
  verificación; shipping de cripto no verificada sobre datos sensibles = riesgo de seguridad.
- **Persistir en iOS a un almacén sin cifrar (UserDefaults/archivo plano).** Descartada:
  viola la minimización/cifrado en reposo de datos cognitivos sensibles.
- **Lanzar iOS con progreso volátil y no avisar.** Descartada: data-loss silenciosa +
  producto de progreso inexistente en esa plataforma = deshonesto con el usuario.

## Seguimiento (fuera de esta fase)

- **[iOS-KEYCHAIN]** Implementar `SecureKeyValueStore.ios.kt` con Keychain (device-bound,
  sin backup) y su verificación en el build de macOS. Bloqueante para publicar iOS.
- **[iOS-LOUD-FALLBACK]** Hasta entonces, cuando un build de iOS de *publicación* caiga al
  stub in-memory, emitir un warning visible/telemetría no-PII (hoy es silencioso). Debe
  hacerse en la fase de macOS junto con el store real (no se toca `iosMain` a ciegas desde
  Windows).

## Verificación de esta decisión

Cambio documental; no altera código compilable. No hay tests asociados (N/A). Los tests de
`SecureKeyValueStore` existentes (`SecureKeyValueStoreTest`, target desktop in-memory)
siguen siendo la referencia de contrato de la superficie.
