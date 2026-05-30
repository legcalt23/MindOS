# Plan de cierre de bloqueadores de publicación (pre-Fase 8)

**Fecha:** 2026-05-29
**Objetivo:** cerrar los 3 bloqueadores rojos que impiden subir a las tiendas. Fase 8 (release
controlado) NO debe empezar hasta completar B1 y B2 (y B0).

Orden de ejecución: **B0 → decisiones → B1 → B2**. B1 y B2 dependen de decisiones de
producto/legal que NO me corresponden; el código va detrás de esas respuestas.

---

## B0 — Política de privacidad publicada (no código) · esfuerzo S
- **Objetivo:** URL pública de política de privacidad coherente con `DATA_SAFETY_AND_PRIVACY_LABELS.md`.
- **Pasos:** redactar (legal) → publicar (web) → enlazar en ambas fichas.
- **Criterio de aceptación:** URL accesible y enlazada; coincide con los datos declarados.
- **Estado:** pendiente (producto/legal).

---

## Decisiones requeridas ANTES de tocar código

### Borrado de cuenta (de `ACCOUNT_DELETION_DECISION.md`)
- **D1** ¿Qué pasa con el saldo de coins al borrar? (se pierde + T&C / debe gastarse antes)
- **D2** Periodo de retención legal del ledger anonimizado (p. ej. 5–10 años)
- **D3** ¿Borrado inmediato o grace period (14–30 días reversible)?
- **D4** DMs: ¿purgar solo el lado del usuario o toda la conversación?
- **D5** ¿La moneda se vende por dinero real? (afecta retención fiscal)

### IAP (de `IAP_COMPLIANCE_DECISION.md`)
- **I1** ¿La moneda se vende por dinero real? (si no → quizá no requiere IAP)
- **I2** ¿Coins consumibles o saldo no consumible?
- **I3** Catálogo de packs + precios
- **I4** ¿Usos fuera de la app (web)?

> D5 e I1 son la misma pregunta y desbloquean ambos frentes — resolverla primero.

---

## B1 — Borrado de cuenta in-app · esfuerzo L · bloquea ambas tiendas

**Diseño aprobado:** anonimización + retención (NO borrado físico del ledger), ver decisión.

| ID | Tarea | Capa | Esfuerzo |
|---|---|---|---|
| B1.1 | CF `deleteAccount` (envelope appCheck→auth→validate→rateLimit), idempotente | functions/src/accountDeletion.ts (NUEVO) | M |
| B1.2 | Purga de PII: profiles, profile_visuals, posts (reusar deletePost/onPostDeleted), DMs+dm_media, follows/observations/requests/blocks (bidireccional) + contadores, profile_visits, Storage | functions + reglas existentes | L |
| B1.3 | Ledger/audit/purchases: **anonimizar** (uid→`deleted_<hash>`, quitar email/displayName), NO borrar | functions/src/lib/ledgerService, walletRepository | M |
| B1.4 | `admin.auth().deleteUser(uid)` al final | functions | XS |
| B1.5 | `SessionRepository.deleteAccount()` + impls (Firebase/Preview) | shared/core/session | S |
| B1.6 | UI: entrada en Settings + confirmación doble + **reautenticación reciente** (Firebase la exige) | shared/features/privacy/SettingsScreen | M |

### Mapa de borrado por colección (derivado de DATA_INVENTORY.md) — para la CF `deleteAccount`

> ⚠️ La CF debe ser **completa** antes de borrar el usuario de Auth. Un borrado que elimine la
> cuenta dejando posts/follows/DMs deja PII pública → trampa de compliance. No shippear parcial.

| Colección | Estrategia | Notas |
|---|---|---|
| `profiles/{uid}`, `profile_visuals/{uid}`, `privacy/{uid}`, `counters/{uid}` | delete por ID directo | docs únicos del usuario |
| `users/{uid}` | update `status="Deleted"` | soft-delete del registro de cuenta (schema: nadie borra) |
| `usernames/{username}` | query `where uid == uid` → delete | libera el handle |
| `profile_visits/{id}` | query `where viewerUserId == uid` **y** `where profileOwnerId == uid` → delete | dos queries (Firestore no hace OR) |
| `follows/{id}` | query `where followerId == uid` **y** `where followeeId == uid` → delete + decrementar `counters` de los afectados | bidireccional |
| `observations/{id}` | query `where observerUserId == uid` **y** `where observedUserId == uid` → delete | bidireccional |
| `notifications/{id}` | query `where actorUserId == uid` **y** `where recipientUserId == uid` → delete | bidireccional |
| `followRequests/{id}`, `blocks/{id}` | query por requester/target / blocker/blocked → delete | bidireccional |
| `posts/{id}` (+ `user_posts/{uid}/posts/*`, `feeds/{uid}/posts/*`) | query `where userId == uid` → delete (dispara `onPostDeleted` → purga attachments+feed) | reutilizar trigger existente |
| `dm_chats` (array `participants` contains uid) + `messages` | **anonimizar** sender del usuario (D4); NO borrar mensajes del otro | requiere update por mensaje |
| Storage `profile_visuals/{uid}/**`, `post_attachments/{uid}/**` | borrar prefijos del usuario | `dm_media` compartido → no borrar (D4) |
| `wallets`, `ledger_*`, `purchases`, `*_audit*` | **RETENER + anonimizar** uid→`deleted_<hash>` (NO borrar) | obligación legal (D5=sí); requiere revisar `ledgerService`/`walletRepository` |
| Auth user | `admin.auth().deleteUser(uid)` **al final** | irreversible; tras purgar todo |

Implementación: callable v1 con envelope `requireAppCheck→requireAuth→checkRateLimit('deleteAccount')`
(añadir entrada a `RATE_LIMITS`), helper `deleteByQuery(query)` con batches de ≤500, idempotente
(re-ejecutable si falla a mitad). Tests jest con mock de `.where().get()` multi-doc + `admin.auth().deleteUser`.

- **Dependencias:** D1–D4 (✅ defaults adoptados); emulador de reglas (Fase 5, ya en CI) para validar.
- **Riesgos:** borrado parcial si falla a mitad → diseñar idempotente y re-ejecutable; no romper
  invariantes de reconciliación del ledger (de ahí la anonimización).
- **Cómo probarlo:** tests Functions (jest) del flujo de borrado+anonimización; test de reglas
  (que el usuario borrado pierde acceso); contract test de `SessionRepository.deleteAccount`;
  manual: crear cuenta → borrar → verificar Auth eliminado + PII purgada + ledger anonimizado.
- **Criterio de aceptación:** tras borrar, no queda PII; Auth eliminado; ledger cuadra
  (reconciliación verde); se puede declarar "el usuario puede solicitar borrado" en Data Safety.

---

## B2 — IAP de la moneda virtual · esfuerzo L/XL · bloquea (si I1=sí)

**Ventaja:** el backend ya tiene `paymentProviders.ts` + `webhookGooglePlay`/`webhookAppStore`.
El gap es el **cliente**.

Decisiones: **I1**=dinero real (100 coins=1 €) · **I2**=consumible · **I3**=catálogo escalonado ·
**I4**=solo app móvil (v1). Ver `IAP_COMPLIANCE_DECISION.md`.

| ID | Tarea | Capa | Estado |
|---|---|---|---|
| B2.0 | Catálogo IAP real (`CoinPackCatalog`: coins_100/550/1200/3000) server-authoritative + tests | shared (commonMain) | ✅ HECHO (4 tests) |
| B2.1 | Google Play Billing Library (cliente): conexión, query de productos, launchBillingFlow | androidApp/androidMain | ⛔ GATED (productos en Play Console + sandbox) |
| B2.2 | Enviar `purchaseToken`+`productId` a verify/`webhookGooglePlay` (server acredita por catálogo) | shared/features/wallet/data + functions | ⛔ GATED (validar contra sandbox) |
| B2.3 | Gating por build: sandbox SOLO en debug; Play Billing en release. Reusar kill switch `wallet_enabled` | shared + androidApp | ⛔ GATED |
| B2.4 | (iOS) StoreKit 2 → `webhookAppStore` | iosApp/iosMain | ⛔ GATED (iOS aplazado) |

> **B2.1–B2.4 NO son verificables sin la cuenta de Play Console + cuenta sandbox de tester**
> (no disponibles en este entorno). La ruta de dinero es de máximo riesgo (condición #5): no se
> implementa a ciegas sin poder validar contra el sandbox. B2.0 (catálogo) sí se hizo: pin de I3
> en código, sin tocar el flujo sandbox actual (no rompedor).

- **Dependencias:** I1–I4 (✅ decididas); cuentas/productos en Play Console (+ App Store Connect para iOS);
  el contrato server-authoritative del wallet (ya implementado).
- **Riesgos:** consumir/acknowledge correctamente la compra (Play exige acknowledge < 3 días);
  reconciliación con el ledger; pruebas en sandbox de cada tienda.
- **Cómo probarlo:** test de Play Billing en sandbox (cuenta de prueba) → coins acreditados vía
  webhook; verificar idempotencia (no doble crédito); kill switch desactiva la compra.
- **Criterio de aceptación:** compra real (sandbox de la tienda) acredita coins server-side; sin
  pago propio para bienes digitales; cumple Apple 3.1.1 / Play Payments.

---

## Resumen de ruta a Fase 8
1. **B0** (privacidad publicada) — paralelo, no bloquea código.
2. Resolver **D5/I1** primero (¿dinero real?), luego D1–D4 e I2–I4.
3. **B1** (borrado de cuenta) — validable con emulador de reglas + jest.
4. **B2** (IAP) — solo si I1=sí; backend listo, falta cliente.
5. Rellenar consolas (Data Safety/Privacy Labels, content rating) y **entonces** Fase 8
   (rollout escalonado + monitoreo + rollback).
