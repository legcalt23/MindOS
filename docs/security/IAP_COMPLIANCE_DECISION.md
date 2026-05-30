# Decisión de compliance — Compras in-app de la moneda virtual (T7.2)

**Fecha:** 2026-05-29 · **Actualizado:** 2026-05-30
**Estado:** ⛔ BLOQUEADOR DE STORE CONFIRMADO — IAP obligatorio; el sandbox NO puede ir a producción.
**Disparador:** Fase 7, tarea T7.2 (política de compras).
**Regla aplicada:** condición de ejecución #5 — riesgo/decisión → detener y documentar.

---

## DECISIÓN REGISTRADA (2026-05-30)

> **I1 = SÍ. La moneda SE VENDE POR DINERO REAL. Tasa: 100 coins = 1 €.**

Consecuencias directas:
1. **IAP obligatorio** — Google Play Billing (Android) y Apple StoreKit (iOS). El proveedor
   sandbox propio para release sería **rechazo seguro** en ambas tiendas.
2. **El catálogo actual NO es viable** como producto IAP real:
   - `coins_1` → 0,01 € y `coins_10` → 0,10 € quedan **por debajo del precio mínimo** de las
     tiendas (~0,29–0,49 € por producto). **Deben eliminarse** o reagruparse.
   - `coins_100` → 1,00 € sí es viable. Hace falta un **catálogo nuevo** de packs con tiers
     válidos (p. ej. 100/1 €, 500/5 €, 1200/10 €…) — pendiente (I3).
3. **Obligaciones fiscales/consumo** (porque hay dinero real):
   - **IVA/impuestos** sobre las ventas en € (las tiendas gestionan IVA en muchos países como
     "merchant of record", pero hay que declararlo y configurarlo por territorio).
   - **Derecho de desistimiento (UE)**: el contenido digital consumido inmediatamente requiere
     consentimiento expreso de renuncia al desistimiento de 14 días → añadir ese checkbox/flujo.
   - **Comisión de tienda** (~15–30 %) afecta a la economía de 100 coins = 1 €.
4. **Retención del ledger** pasa a ser **obligación legal** (registros financieros) → confirma el
   diseño de anonimización-sin-borrado del borrado de cuenta (D5 = sí).

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

| # | Decisión | Estado | Impacto |
|---|---|---|---|
| I1 | ¿La moneda se vende por **dinero real**? | ✅ **SÍ — 100 coins = 1 €** | IAP obligatorio (Apple+Google) |
| I2 | ¿Coins son **consumibles** o saldo? | ✅ **CONSUMIBLE** | Productos tipo consumible; el cliente consume/acknowledge tras el crédito del servidor |
| I3 | Catálogo de packs y precios | ✅ **Escalonado con bonus** (ver abajo) | Crear estos productos en Play Console / App Store Connect |
| I4 | ¿Hay usos fuera de la app (web)? | ✅ **Solo app móvil (v1)** | IAP puro; sin pasarela externa ni promoción de compra externa in-app |

### Catálogo definitivo (I3) — productos IAP consumibles

| Product ID | Coins | Precio | Bonus |
|---|---|---|---|
| `coins_100`   | 100   | 0,99 € | — |
| `coins_550`   | 550   | 4,99 € | +10% (vs 500) |
| `coins_1200`  | 1200  | 9,99 € | +20% (vs 1000) |
| `coins_3000`  | 3000  | 24,99 € | +20% |

> Reemplaza el catálogo sandbox actual (`coins_1`/`coins_10`/`coins_100` a 1¢/10¢/1 €, no publicable).
> El sandbox se mantiene SOLO en builds debug; release usa estos productos vía Play Billing/StoreKit.
> El servidor sigue siendo autoritativo (el cliente nunca calcula el saldo); el `coinsAmount` lo
> determina el catálogo de servidor por `productId`.

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
