# Decisión de compliance — Compras in-app de la moneda virtual (T7.2)

**Fecha:** 2026-05-29
**Estado:** ⛔ BLOQUEADOR DE STORE — requiere decisión de producto/legal antes de publicar.
**Disparador:** Fase 7, tarea T7.2 (política de compras).
**Regla aplicada:** condición de ejecución #5 — riesgo/decisión → detener y documentar.

---

## Situación

La feature "moneda virtual" (wallet) acredita coins vía un **proveedor sandbox propio**
(`initiatePurchase`/`verifyPurchase` → Cloud Functions, server-authoritative). El cliente NO usa
hoy Google Play Billing ni Apple StoreKit.

El backend YA está preparado para proveedores reales: `functions/src/lib/paymentProviders.ts`,
`sandboxProvider.ts` y webhooks `webhookGooglePlay` / `webhookAppStore` / `webhookStripe`. El
**gap es el cliente**: no hay integración de billing nativo.

## Reglas de las tiendas (por qué bloquea)

- **Apple Guideline 3.1.1 (In-App Purchase):** si la app desbloquea features o vende moneda/contenido
  digital consumible dentro de la app, **debe** usar IAP de StoreKit. Un sistema de pago propio para
  bienes digitales → **rechazo seguro**.
- **Google Play (Payments policy):** los bienes/contenidos digitales dentro de la app deben usar
  **Google Play Billing**. Sandbox/pasarela propia para coins → **rechazo / suspensión**.
- **Stripe (webhook presente):** válido solo para bienes/servicios físicos o fuera del ámbito
  digital-in-app — NO sustituye a IAP para coins digitales.

## Decisiones REQUERIDAS antes de publicar (producto + legal)

| # | Decisión | Impacto |
|---|---|---|
| I1 | ¿La moneda se vende por **dinero real**? | Si sí → IAP obligatorio (Apple+Google). Si es solo recompensa no comprable → puede no requerir IAP |
| I2 | ¿Coins son **consumibles** o saldo? | Tipo de producto IAP (consumible vs no consumible) |
| I3 | Catálogo de packs y precios | Crear productos en App Store Connect + Play Console |
| I4 | ¿Hay usos fuera de la app (web)? | Afecta a si aplica "reader app" / excepciones |

## Si la respuesta a I1 es "sí" (lo más probable)

Trabajo de implementación (XL, NO en esta fase):
1. **Android:** Google Play Billing Library (cliente) → comprar producto → enviar `purchaseToken` a
   `verifyPurchase`/`webhookGooglePlay` (el backend ya valida). Reemplaza el sandbox en release.
2. **iOS:** StoreKit 2 → `Transaction` → `webhookAppStore` (gated por reentrada de iOS).
3. Mantener el sandbox SOLO para builds debug/test (nunca en release a store).
4. Tests + verificación contra los entornos sandbox de cada tienda.

## Acción
- **No se implementa IAP en Fase 7** (XL + dependiente de I1–I4 + requiere cuentas de las tiendas).
- Permanece como **bloqueador de publicación abierto**, con el backend ya preparado.
- Antes de invertir en el flujo de pago cliente, resolver I1–I4 con producto/legal.
