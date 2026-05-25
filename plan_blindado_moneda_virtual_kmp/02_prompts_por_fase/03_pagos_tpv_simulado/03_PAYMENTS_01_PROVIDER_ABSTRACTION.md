# Pagos 01 — Abstracción de proveedor

## Objetivo

Preparar pagos reales sin implementarlos.

## Contexto obligatorio

Estás implementando un sistema de moneda virtual blindado para una app Kotlin Multiplatform con backend autoritativo.

Reglas absolutas:

- El cliente nunca es fuente de verdad.
- No aceptes saldo final desde cliente.
- No implementes pagos reales todavía.
- Usa TPV sandbox/simulado.
- Toda operación económica debe ser idempotente.
- Toda operación económica debe ser auditada.
- Ledger append-only.
- Tests obligatorios.

## Tarea única

Crea interfaz PaymentProvider con métodos validate/verify/refundStatus y modelos normalizados. Añade stubs seguros para GooglePlayPaymentProvider, ApplePaymentProvider y StripePaymentProvider que no procesen en producción hasta configurar credenciales.

## Alcance permitido

Interfaces, modelos, tests.

## Fuera de alcance

Llamadas reales a proveedores.

## Requisitos de seguridad

Fail closed si provider real no configurado. No secretos hardcodeados.

## Tests mínimos

Tests de provider no configurado.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
