# 10 · Plan de Persistencia de Datos

> **Local-first y cifrado.** Los datos cognitivos son sensibles. Se guardan
> localmente cifrados; a Firestore solo van **resúmenes opt-in**, nunca trials
> crudos. Reutiliza el patrón de persistencia ya existente en el repo.

## 0. Principio: qué guardar y qué NO

| Dato | Dónde | Por qué |
|------|-------|---------|
| **Trials crudos** (cientos/sesión) | Local cifrado, retención corta (o solo en memoria + resumen) | Volumen alto; coste/privacidad. No a Firestore. |
| **BlockResult / SessionResult (resumen)** | Local cifrado (siempre) + Firestore (opt-in) | Historial y tendencias. |
| **BaselineSnapshot** | Local cifrado (+ opt-in cloud) | Comparación pre/post; clave para RCI. |
| **Resultados de probes** | Local cifrado | Evidencia de transferencia; **nunca** mezclar con training. |
| **UserCognitiveProfile / arquetipo** | Local cifrado (+ opt-in cloud) | Dato sensible (inferencias de salud). |
| **Métricas derivadas/tendencias** | Calculadas on-demand desde resúmenes | No duplicar verdad. |
| **PII en telemetría** | NUNCA | Solo nombres de ruta/eventos PII-free (patrón ObservabilityHub). |

## 1. Reutilizar el patrón de persistencia local del repo

El repo ya tiene el patrón exacto: `expect fun rememberPublisherLocalDraftStore(
namespace): PublisherLocalDraftStore` con interfaz `suspend load/save/clear`,
implementado en Android con **SharedPreferences + AES/GCM vía Android Keystore**.
**Replicar ese patrón** para datos de Enfoque (no inventar uno nuevo).

```kotlin
// presentation/data — patrón expect/actual idéntico al PublisherLocalDraftStore
@Composable
expect fun rememberFocusLocalStore(namespace: String): FocusLocalStore

interface FocusLocalStore {
    suspend fun loadProfile(userId: String): UserCognitiveProfile?
    suspend fun saveProfile(profile: UserCognitiveProfile)
    suspend fun loadBaseline(userId: String): BaselineSnapshot?
    suspend fun saveBaseline(userId: String, snapshot: BaselineSnapshot)
    suspend fun appendSession(summary: SessionResult)        // solo resumen, no trials crudos
    suspend fun recentSessions(userId: String, limit: Int): List<SessionResult>
    suspend fun saveProbeResult(result: SessionResult)
    suspend fun clearAll(userId: String)                     // borrado de cuenta / opt-out
}
```

- **Android:** SharedPreferences cifradas (mismo `AndroidPublisherLocalDraftStore`
  con Keystore AES/GCM). Reutilizar/factorizar el helper de cifrado existente.
- **Desktop/iOS:** stub funcional documentado al inicio (Fase 5), igual que el
  repo difiere actuals de plataforma. iOS real: Keychain-backed (diferido).

## 2. Serialización

Usar **kotlinx-serialization** (`1.9.0`, ya en `commonMain`). No añadir deps.

```kotlin
@Serializable data class SessionResultDto(...)
@Serializable data class BaselineSnapshotDto(...)
@Serializable data class UserCognitiveProfileDto(...)
// mappers DTO <-> dominio en data/mapper/, igual que features/dm y features/social
```
Versionar los DTO (`schemaVersion: Int`) para migraciones futuras sin perder
historial.

## 3. Capa de repositorio (contratos en doc 02)

```kotlin
// data/local/LocalFocusProfileRepository.kt  (implementa FocusProfileRepository)
class LocalFocusProfileRepository(private val store: FocusLocalStore) : FocusProfileRepository {
    override suspend fun load(userId: String) = store.loadProfile(userId)
    override suspend fun save(profile: UserCognitiveProfile) = store.saveProfile(profile)
}
// data/InMemoryFocusRepositories.kt  — para tests/preview (patrón InMemory del repo)
```

## 4. Sync opcional a Firestore (opt-in, solo resúmenes)

Solo si el usuario lo activa explícitamente. Reglas heredadas del repo:

```kotlin
// data/FirebaseFocusSummaryRepository.kt
class FirebaseFocusSummaryRepository(...) : RemoteFocusSummaryRepository {
    // SOLO SessionResult resumido + BaselineSnapshot. NUNCA trials crudos.
    override suspend fun pushSummary(s: SessionResult) =
        withFirestoreWriteTimeout { /* set en focus_sessions/{uid}/sessions */ }
    override suspend fun recent(uid: String, limit: Int) =
        withFirestoreReadTimeout { /* query .limit(limit) — OBLIGATORIO */ }
}
```

**Obligaciones (reglas documentadas del repo):**
- Toda query `list` con `.limit(N)` y regla Firestore con `request.query.limit`.
- Operaciones envueltas en `withFirestoreReadTimeout`/`WriteTimeout`.
- Reescribir `catch` para re-lanzar `CancellationException`.
- App Check enforced en cualquier path privado nuevo (regla 2026-05-26).
- Añadir reglas en `firestore.rules` + test espejo en
  `firestore.security.rules.test.mjs` para cualquier colección nueva
  (`focus_sessions/{uid}/...`, `focus_profiles/{uid}`).

## 5. Esquema Firestore recomendado (si se activa cloud)

```
focus_profiles/{uid}                       # 1 doc por usuario (perfil + baseline)
focus_sessions/{uid}/sessions/{sessionId}  # resumen por sesión (no trials)
focus_probes/{uid}/probes/{probeId}        # resultados de probe (evaluación)
```
Reglas: `read/write` solo `request.auth.uid == uid` + `isAppChecked()`;
`list` con `request.query.limit <= 100`.

## 6. Retención y borrado

- **Trials crudos:** retención corta (p. ej. mantener solo la sesión actual para
  re-cálculo; descartar tras agregar resumen) o desactivable.
- **Borrado de cuenta:** `clearAll(userId)` local + integrar con la Cloud Function
  de borrado de cuenta existente (`accountDeletion.ts`) si hay sync cloud.
- **Opt-out:** desactivar sync borra los docs remotos.

## 7. Privacidad (resumen; detalle doc 14)

- Datos cognitivos = **categoría sensible**. Por defecto local-only.
- Excluir directorios de Enfoque de backups del sistema (extender
  `backup_rules.xml`/`data_extraction_rules.xml` como se hizo con `dm_audio/`).
- Cero PII en Crashlytics/telemetría; solo eventos PII-free vía `ObservabilityHub`.
- Consentimiento explícito antes de cualquier sync.

## 8. Criterios de aceptación (persistencia)

- [ ] `FocusLocalStore` replica el patrón cifrado existente (no nuevo).
- [ ] Trials crudos nunca llegan a Firestore; solo resúmenes.
- [ ] DTO con `schemaVersion`; mappers testeados.
- [ ] Queries remotas con `.limit()` + regla + test espejo.
- [ ] `clearAll` + integración con borrado de cuenta.
- [ ] Backups del sistema excluyen datos cognitivos.
- [ ] Cero dependencias nuevas (kotlinx-serialization ya presente).
