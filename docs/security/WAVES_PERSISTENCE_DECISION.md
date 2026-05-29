# Decisión de seguridad — Persistencia de Wave Requests (T2.2)

**Fecha:** 2026-05-29
**Estado:** ⛔ GATED por seguridad — implementación diferida hasta Fase 5 (T5.1).
**Disparador:** Fase 2, tarea T2.2.
**Regla aplicada:** condición de ejecución #5 — "Si aparece un riesgo de seguridad, detente y documenta la decisión."

---

## Problema

`AppGraph` usa `InMemoryWaveRequestRepository` en producción: las solicitudes de wave viven
en un `MutableStateFlow` en memoria y se pierden al matar el proceso (pérdida de datos funcional).

## Por qué no se implementa la versión Firestore en esta fase

Persistir waves significa crear una **colección Firestore nueva** (`wave_requests`) con su
**superficie de seguridad propia**:

- Requiere reglas nuevas en `firestore.rules` (acceso por participante, `request.query.limit`,
  `isAppChecked()` — exactamente los controles que los aprendizajes previos de CLAUDE.md
  identifican como obligatorios).
- La máquina de estados (`sendRequest`/`accept`/`reject`/`cancel` + `getRelationshipStatus`
  + check de bloqueo) debe reimplementarse sobre transacciones Firestore.

**Bloqueador real:** el pipeline de CI **no ejecuta el emulador de reglas** todavía. Los tests
`firestore.security.rules.test.mjs` existen pero no corren en CI (gap documentado, Fase 5 T5.1).
Enviar reglas nuevas sin validación automatizada es la causa raíz de varios hallazgos de
seguridad ya corregidos. No se repetirá el patrón.

## Dependencia

`T2.2 → T5.1` (job CI que ejecute el emulador + tests de reglas). Hasta que exista, no se debe
introducir una colección Firestore nueva.

## Diseño aprobado para cuando se desbloquee

### Colección `wave_requests/{requestId}`
Campos: `fromUserId`, `toUserId`, `participants: [a,b]` (ordenado), `status`
(`pending|accepted|rejected|cancelled`), `createdAt`, `updatedAt`.

### Reglas (plantilla, NO desplegada)
```
match /wave_requests/{requestId} {
  allow get: if isAppChecked() && request.auth.uid in resource.data.participants;
  allow list: if isAppChecked()
              && request.query.limit != null && request.query.limit <= 50
              && request.auth.uid in resource.data.participants;
  // Las escrituras (crear/aceptar/rechazar/cancelar) pasan por Cloud Functions
  // (Admin SDK + envelope requireAppCheck→requireAuth→validate→rateLimit), igual
  // que socialGraph/posts. allow write: if false desde el cliente.
  allow write: if false;
}
```

### Implementación
- `FirebaseWaveRequestRepository` en `commonMain` (gitlive Firestore), con timeouts
  `withFirestoreReadTimeout`/`withFirestoreWriteTimeout` y transacciones para el cambio de estado.
- Callables backend para mutaciones (consistente con el resto del backend: las escrituras
  sensibles no se hacen desde el cliente).
- Tests: `wave_requests` en `firestore.security.rules.test.mjs` + contrato del repo.

## Alternativa de producto (si waves NO entra en v1)
Cortar el feature del `AppGraph`/UI y eliminar `InMemoryWaveRequestRepository` del path de
producción. Decisión de producto pendiente.

## Acción
- **No se implementa repo/reglas Firestore en Fase 2.** El `InMemoryWaveRequestRepository`
  permanece como está (sin regresión respecto al estado actual).
- T2.2 queda como tarea con diseño listo, gated por T5.1.
