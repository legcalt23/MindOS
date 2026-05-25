# Wallet 06 — Gasto de monedas

## Objetivo

Implementar gasto resistente a double spending.

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

Implementa `POST /wallet/spend` con precio server-side, idempotency key, lock de wallet/snapshot, ledger negativo, snapshot update y audit log.

## Alcance permitido

Endpoint, servicio, tests.

## Fuera de alcance

Pagos o pantalla KMP.

## Requisitos de seguridad

Cliente no envía saldo ni precio final. Bloquea wallet frozen. Usa transacción DB.

## Tests mínimos

Tests saldo insuficiente, idempotencia, double spending con concurrencia.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
