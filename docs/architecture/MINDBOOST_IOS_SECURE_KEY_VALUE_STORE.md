# MindBoost — iOS durable `SecureKeyValueStore` (Phase 1.2)

**Fecha:** 2026-07-04 (rev. 2026-07-05) · **Autor:** Staff Mobile Engineer
**Estado oficial (CEO 2026-07-05):** *Implementada, endurecida y revisada; **iOS verification DEFERRED until
macOS/Xcode is available**.* NO cerrada al 100%; iOS NO marcado verde; Keychain NO verificado en simulador/dispositivo.

> **iOS Keychain implementation compile-pending until macOS/Xcode gate.**
> **Do not activate real sync on iOS until iOS gates are green.**
> Gates iOS pospuestos (sin Mac/Xcode ahora): `:shared:compileKotlinIosSimulatorArm64`,
> `:shared:compileKotlinIosArm64`, `:shared:iosSimulatorArm64Test`. Son **blockers explícitos**, no completados.

> **Alcance:** solo storage local durable iOS para que la cola de sync sea viable en iOS. **NO** activa sync,
> **NO** monta coordinador, **NO** toca scoring/calibración/UI/otros motores. Verificación estática — los gates
> Gradle (y la compilación iOS) los ejecuta el dueño.

---

## 1. Auditoría de source sets (verificada contra el repo)

| Source set | File | Implementation | Durable | Encrypted | Notes |
|---|---|---|---|---|---|
| commonMain | `SecureKeyValueStore.kt` | interfaz + `InMemorySecureKeyValueStore` + helpers + `expect rememberSecureKeyValueStore` | — | — | contrato |
| androidMain | `SecureKeyValueStore.android.kt` | `AndroidSecureKeyValueStore` (AES-256-GCM + Keystore + private SharedPreferences) | **Sí** | **Sí** | sin cambios |
| desktopMain | `SecureKeyValueStore.desktop.kt` | `InMemorySecureKeyValueStore` | No | No | dev/test, sin cambios |
| iosMain | `SecureKeyValueStore.ios.kt` | **antes** `InMemorySecureKeyValueStore` → **ahora** `IndexedSecureKeyValueStore(IosKeychainBackend(ns))` | **Sí (diseño)** | **Sí (Keychain)** | binding nativo pendiente de compilar |

## 2. Diseño

Se separa la lógica (pura, testeable) de la I/O nativa (no testeable aquí), igual que el patrón del gateway
Firestore de Phase 1.1:

- **`IndexedSecureKeyValueStore`** (commonMain, **PURO + unit-tested**): implementa el contrato
  `SecureKeyValueStore` sobre un `KeyValueBackend` plano, añadiendo un **índice de keys por namespace** para que
  `keys(prefix)` y `clearNamespace()` funcionen aunque el backend no enumere (Keychain no lista por prefijo).
- **`KeyValueBackend`** (commonMain): seam mínimo `read/write/delete` de un `String`, namespace-scoped.
- **`IosKeychainBackend`** (iosMain, **binding nativo, compile-unverified aquí**): implementa `KeyValueBackend`
  sobre Keychain (generic-password).

### Índice de keys
La key reservada `__index__` guarda el set de keys vivas (separadas por `\n`; las keys son prefijo+id, sin
saltos de línea). `put` añade al índice; `remove` la quita; `keys(prefix)` filtra el índice; `clearNamespace()`
borra cada item indexado y luego el índice. Una key en el índice sin item real **se lee como `Missing`**
(tolerado, nunca crash; el lector la salta). Un índice corrupto degrada a vacío (fail-safe). `__index__` es
reservada (ningún caller la usa como key de datos: `entry:…`, `profile:…`, etc.).

### Consistencia / endurecimiento (Phase 1.2)
El contrato del seam cambió: **`KeyValueBackend.write` DEBE lanzar** (no retornar normalmente) si la escritura
durable no ocurrió — así el store de arriba **nunca indexa una key que no se persistió**. Sobre esa base:

- **`put` atómico:** escribe el valor primero; solo si persiste, indexa. Si el índice falla, hace **rollback
  best-effort del valor** (`delete`) y **propaga** el error. Nunca queda una key medio-escrita ni una key no
  persistida indexada. `put` **rechaza** la key reservada `__index__` (`require`).
- **`remove`:** borra el valor primero (un fallo real de borrado se **propaga**); si la limpieza del índice
  falla, solo queda una entrada stale que se lee como `Missing`.
- **`clearNamespace` (erasure GDPR):** best-effort **TOTAL** — intenta borrar todos los items aunque alguno
  falle; **conserva el índice** si un data-delete falló (para que un reintento encuentre los supervivientes) y
  **propaga el primer fallo** para que el `AccountDeletionCoordinator` reintente. El eraser no captura por-store,
  pero el coordinator envuelve `eraseUserData` → un throw da `LocalEraseBlocked` (retryable, fail-closed: NO
  borra la cuenta Auth mientras quede dato local).
- **`read`/`keys`:** toleran índice stale/degenerado (degenerado → vacío), PERO un fallo REAL de lectura del
  backend **propaga** (fail-closed) en vez de disfrazarse de vacío/ausente (si no, `clearNamespace` saltaría
  datos y el erasure "tendría éxito" dejando datos huérfanos — hallazgo P1 de la review, corregido).

`record(PENDING)=false` (cola total/observable) se preserva end-to-end: si el Keychain lanza en `write`, `put`
propaga → `SecureSyncQueueRepository.record` (envuelto en `queueSafe`) devuelve `false` — nunca fabrica una fila
SYNCED. Cubierto por `secureSyncQueueRecord_returnsFalse_whenBackendWriteFails_keepingTheQueueTotal`.

## 3. Keychain (iOS)

- **API:** `SecItemAdd` / `SecItemUpdate` / `SecItemCopyMatching` / `SecItemDelete` con `kSecClassGenericPassword`.
- **Upsert atómico:** `write` hace `SecItemAdd`; ante `errSecDuplicateItem` actualiza en sitio con
  `SecItemUpdate` (NO delete+add), así **no hay ventana** en la que la key quede momentáneamente ausente.
- **Fail-closed (lectura y escritura):** se comprueba **cada `OSStatus`**; un add/update/delete
  no-`errSecSuccess` lanza `KeyValueBackendException` (solo el código de estado — nunca key/valor/identificador).
  `read` devuelve `null` SOLO ante `errSecItemNotFound` (ausencia real) o datos no-UTF-8; **cualquier otro fallo
  real de lectura LANZA** (simétrico con `delete`), para que una lectura fallida del índice no se disfrace de
  "vacío" y `clearNamespace` no salte datos en silencio durante el erasure. `delete` trata `errSecItemNotFound`
  como no-op (no es fallo).
- **Recursos:** cada método usa `try/finally`; `CFRelease` va guardado contra `null`; `CFBridgingRelease`
  libera los refs retenidos — sin double-release ni leaks.
- **Namespacing:** `kSecAttrService` = namespace MindBoost (`mindboost_sync_queue`, …); `kSecAttrAccount` = key.
  Namespaces distintos → services distintos → **sin colisión**.
- **Accessibility:** `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` — disponible tras el primer desbloqueo,
  **sin** iCloud Keychain sync, **sin** restaurar a otro dispositivo (prioriza privacidad; disponibilidad
  razonable en background).
- **Encriptación:** el Keychain cifra en reposo (respaldado por hardware).
- **Durabilidad:** sobrevive a reinicio de app y reboot de dispositivo.
- **Privacidad:** valores UTF-8; **sin logs** de keys ni valores; sin raw trials; sin email/displayName en la
  sync queue (el DTO ya los excluye).

## 4. Erasure

- `mindboost_sync_queue` sigue en `MindBoostStorageNamespaces.AccountBound` + el eraser derivado de `AccountBound`
  (`accountBoundEraserStores`). El eraser llama `clearNamespace()`, que en iOS borra los items indexados + el
  índice.
- `SecureSyncQueueRepository.deleteByUser(userId)` usa `keys(prefix)` → índice → funciona sobre el store iOS.
- Filas corruptas no atribuibles: `deleteByUser` las deja; las reclama `clearNamespace()` en borrado de cuenta.
- **Sin cambios** en la semántica del eraser.

## 5. Limitaciones / riesgos

- **`IosKeychainBackend` NO se compila en este entorno** (sin macOS/Xcode). El interop Kotlin/Native de Security
  (CFDictionary, CFBridging, `SecItem*`) es **best-effort** y **debe** verificarse con
  `:shared:compileKotlinIosSimulatorArm64` (+ tests de simulador) antes de dev rollout iOS. Si no compila, es un
  arreglo localizado en ESTE archivo; el resto (lógica + tests) es válido. Como fallback temporal, el actual iOS
  puede volver a `InMemorySecureKeyValueStore()` en una línea mientras se arregla el binding.
- Keychain tiene límites de tamaño de item; los valores de la sync queue (resúmenes) son pequeños.
- Accessibility elegida = `AfterFirstUnlockThisDeviceOnly`; si producto exige otra, revisar.
- **Contrato `write` = throw en fallo:** en iOS un fallo real de Keychain ahora **propaga** desde `put`
  (antes, con in-memory, `put` no fallaba nunca). El único consumidor que necesita totalidad
  (`SecureSyncQueueRepository`) ya envuelve `put` (→ `record=false`). Otros repos iOS que llaman `put`
  directamente verían el fallo genuino (comportamiento honesto, no silencioso) — a validar en dispositivo antes
  del rollout iOS (iOS aún es pre-rollout, riesgo acotado).
- **Tests iOS de Keychain real:** un binario `iosSimulatorArm64Test` "pelado" puede carecer de
  keychain-access-group entitlement y devolver `errSecMissingEntitlement`; por eso el test sondea disponibilidad
  y **SKIP** (verde) si el host no puede usar Keychain (ver §6).

## 6. Tests

- **`IndexedSecureKeyValueStoreTest`** (desktopTest, JVM): **20** casos de contrato con un fake backend
  (inyectable con fallos por-key en write/delete/read). 12 base — put/read/overwrite/remove(+índice)/keys(prefix)/
  clearNamespace(+índice)/aislamiento de namespaces/quarantine/special-chars/large-value/stale-index-tolerado +
  integración con `SecureSyncQueueRepository`. **+8 de endurecimiento:** write-item-falla→no-indexa+throw;
  write-índice-falla→rollback-valor+throw; put-rechaza-`__index__`; remove-delete-falla→propaga;
  clearNamespace-borra-todo-aunque-uno-falle-y-conserva-índice; record→`false` cuando el backend lanza (cola
  total); **clearNamespace-con-fallo-de-lectura-de-índice→throw (no falso-éxito, regresión P1)**;
  **read-fallo-real→propaga**. La lógica es platform-agnostic → valida exactamente lo que usará iOS.
- **`IosSecureKeyValueStoreTest`** (`iosTest`, gate `iosSimulatorArm64Test` — **pendiente de macOS**): Keychain
  REAL vía `IosKeychainBackend`. Casos: roundtrip; persistencia entre dos instancias con mismo namespace;
  aislamiento de namespaces; `keys(prefix)`+`remove` actualiza índice; overwrite no duplica índice;
  `clearNamespace` borra todo; special-chars + JSON queue-like; erasure de `mindboost_sync_queue` vía
  `deleteByUser`. Cada test **SKIP-verde** si el host no puede usar Keychain (entitlement). `iosTest` es INERTE
  en Windows/JVM (no se compila ni escanea). Compile-unverified aquí, como el backend.

## 7. Gates

**NO corridos aquí** (el IDE posee el daemon Gradle; iOS exige macOS/Xcode). Comandos:
```bash
:shared:desktopTest --tests "*IndexedSecureKeyValueStore*"   ·   "*SecureKeyValueStore*"   ·   "*SecureSyncQueue*"
:shared:desktopTest --tests "*AccountBoundErasureCoverageTest*"
:shared:detekt   ·   :shared:mindboostCopySafetyScannerDesktopTest   ·   :androidApp:compileDebugKotlin
# iOS (macOS/Xcode — gate manual de este turno):
:shared:compileKotlinIosSimulatorArm64   ·   :shared:compileKotlinIosArm64   ·   :shared:iosSimulatorArm64Test
```
`iosMain` es analizado por `:shared:detekt` (estilo, sin compilar); la compilación real es el gate iOS.

## 8. Estado del P1

- **Antes (pre-1.2):** `iOS in-memory — P1 blocker` (no implementado).
- **Ahora (1.2):** `iOS Keychain implementado y ENDURECIDO (upsert atómico, fail-closed por OSStatus, put atómico
  con rollback, clearNamespace best-effort para erasure; lógica pura con 18 tests JVM verdes en verificación
  estática); binding nativo + tests de Keychain real **pendientes** de
  :shared:compileKotlinIosSimulatorArm64 + :shared:iosSimulatorArm64Test en macOS`. El P1 pasa de "no
  implementado" a "implementado y endurecido, verificación de compilación/tests iOS pendiente". No baja a
  resuelto hasta que esos dos gates estén verdes en macOS.
