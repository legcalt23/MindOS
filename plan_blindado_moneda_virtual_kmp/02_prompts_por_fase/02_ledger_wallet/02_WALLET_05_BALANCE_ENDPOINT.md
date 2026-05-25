# Wallet 05 — Consulta de saldo

## Objetivo

Exponer saldo seguro.

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

Implementa `GET /wallet/balance` autenticado usando snapshot. Si falta wallet, créala de forma segura o mediante servicio dedicado según decisión ADR.

## Alcance permitido

Endpoint, DTOs, tests.

## Fuera de alcance

Spend, purchases, UI.

## Requisitos de seguridad

No aceptar userId externo. No devolver metadata sensible.

## Tests mínimos

Tests auth, wallet inexistente, respuesta correcta.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
