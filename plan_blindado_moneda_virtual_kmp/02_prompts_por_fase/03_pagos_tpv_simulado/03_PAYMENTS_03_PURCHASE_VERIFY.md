# Pagos 03 — Verify purchase sandbox

## Objetivo

Acreditar monedas usando ledger tras verificación sandbox.

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

Implementa endpoints `POST /purchases/sandbox/initiate` y `POST /purchases/sandbox/verify`. Verify debe crear purchase, deduplicar token/hash, acreditar ledger purchased y actualizar snapshot.

## Alcance permitido

Endpoints, purchase service, tests.

## Fuera de alcance

UI KMP.

## Requisitos de seguridad

Toda acreditación pasa por backend. Idempotency key obligatoria. Unique provider token.

## Tests mínimos

Tests doble verify, token cross-user, concurrencia.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
