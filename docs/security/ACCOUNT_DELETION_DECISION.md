# Decisión de seguridad — Borrado de cuenta (T1.1)

**Fecha:** 2026-05-29
**Estado:** ⛔ BLOQUEADA — requiere decisiones de producto/legales antes de implementar.
**Disparador:** Fase 1, tarea T1.1 (bloqueador de Google Play / App Store).
**Regla aplicada:** CLAUDE.md / condición de ejecución #5 — "Si aparece un riesgo de seguridad, detente y documenta la decisión."

---

## Resumen

Google Play (política de borrado de datos, obligatoria para apps con creación de cuenta) y
Apple (Guideline 5.1.1(v)) exigen **borrado de cuenta in-app**. Es un bloqueador real de
publicación. Sin embargo, **no se implementa en esta fase** porque un borrado físico ingenuo
introduce una regresión de seguridad y compliance grave en el subsistema de moneda virtual.

Se detiene la implementación y se documentan las decisiones pendientes.

---

## Por qué un borrado físico ingenuo es un riesgo (no un simple delete)

El backend (`functions/src/lib/`) implementa un **ledger financiero de doble entrada** con
servicios dependientes:

- `ledgerService.ts` — asientos contables (la suma global debe cuadrar).
- `reconciliationService.ts` — reconciliación que valida invariantes sobre TODOS los asientos.
- `auditLogService.ts` — rastro de auditoría inmutable.
- `walletRepository.ts` — saldos derivados del ledger.

Borrar los documentos de wallet/ledger de un usuario al eliminar su cuenta provocaría:

1. **Rotura de invariantes de reconciliación**: la suma de asientos deja de cuadrar; el
   `reconciliationService` reportaría falsos positivos permanentes.
2. **Destrucción del rastro de auditoría**: regresión directa de seguridad/compliance.
3. **Vector de abuso/anti-fraude**: un atacante podría comprar/gastar/recibir fondos y luego
   "borrar la cuenta" para eliminar la evidencia (lavado de evidencia de fraude).
4. **Conflicto con retención legal**: los registros financieros suelen requerir retención
   (fiscal, contable, antifraude). El GDPR Art. 17(3)(b)/(e) permite —y a veces obliga a—
   retener datos para obligaciones legales y defensa de reclamaciones.

**Conclusión técnica:** el patrón correcto es **anonimización + retención**, NO borrado físico
del ledger.

---

## Diseño recomendado (pendiente de aprobación)

Función callable `deleteAccount` (envelope estándar: `requireAppCheck → requireAuth →
validateInput → checkRateLimit → lógica`), idempotente, que realiza:

### A. PII y contenido del usuario → BORRADO/PURGA
- Firebase Auth user (`admin.auth().deleteUser(uid)`) — al final, tras purgar datos.
- Perfil + profile visuals + portadas (Firestore + Storage).
- Posts del usuario (reutilizar `deletePost`/trigger `onPostDeleted` para purgar attachments y feed).
- DM chats, mensajes y `dm_media` donde el usuario es participante.
- Grafo social **bidireccional**: follows, followers, follow requests (entrantes y salientes),
  blocks (en ambos sentidos), y decremento de contadores en los perfiles afectados.
- Profile visits, quotas de Storage, tokens/FCM, draft local.

### B. Registros financieros → RETENCIÓN ANONIMIZADA
- Ledger, transacciones, audit log y purchases: **se conservan**, pero se **desvincula la PII**:
  reemplazar `uid` por un pseudónimo estable (`deleted_<hash(uid)>`), eliminar email/displayName
  embebidos. Así reconciliación y auditoría siguen cuadrando sin retener identidad.

### C. Orquestación
- Soft-delete + grace period (recomendado) o borrado inmediato (decisión de producto).
- Reautenticación reciente obligatoria del lado cliente antes de invocar (Firebase exige
  credenciales frescas para `deleteUser`).
- Operación por lotes/transaccional resiliente a reintentos (idempotente).

---

## Decisiones REQUERIDAS antes de implementar (producto + legal)

| # | Decisión | Opciones | Impacto |
|---|---|---|---|
| D1 | Saldo pendiente al borrar | (a) Se pierde y se documenta en T&C; (b) Debe gastarse/reembolsarse antes de permitir borrado | Diseño del flujo + T&C |
| D2 | Retención de registros financieros | Periodo legal (p. ej. 5–10 años según jurisdicción) | Política de purga del ledger anonimizado |
| D3 | Borrado inmediato vs. grace period | Inmediato / 14–30 días reversible | UX + complejidad backend |
| D4 | Borrado de DMs del *otro* participante | Purgar solo lado del usuario / purgar conversación completa | Privacidad del otro usuario |
| D5 | ¿Moneda virtual comprable con dinero real? | Si sí → afecta retención fiscal y Play Billing/IAP | Compliance financiero |

---

## Acción

- **NO se implementa código de borrado en Fase 1.** Implementarlo sin D1–D5 crearía una
  regresión de seguridad (rotura de auditoría/reconciliación) y posible incumplimiento legal.
- T1.1 permanece como **bloqueador de publicación abierto**, ahora con diseño y decisiones
  acotadas listas para ejecutar en cuanto se respondan D1–D5.
- Próximo paso sugerido: resolver D1–D5 con producto/legal y entonces implementar `deleteAccount`
  (backend) + `SessionRepository.deleteAccount()` + UI de confirmación con reautenticación.

---

## Archivos que se tocarían al desbloquear (referencia, NO modificados ahora)
- `functions/src/index.ts` (export nuevo callable)
- `functions/src/accountDeletion.ts` (NUEVO)
- `functions/src/lib/ledgerService.ts` / `walletRepository.ts` (anonimización, no borrado)
- `shared/.../core/session/SessionRepository.kt` + `FirebaseSessionRepository.kt`
- `shared/.../features/privacy/SettingsScreen.kt` (entrada UI + confirmación)
