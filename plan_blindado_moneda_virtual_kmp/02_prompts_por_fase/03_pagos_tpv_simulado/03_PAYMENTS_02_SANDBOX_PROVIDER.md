# Pagos 02 — TPV sandbox

## Objetivo

Permitir compra simulada sin cobro real.

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

Implementa SandboxPaymentProvider para packs `coins_1`, `coins_10`, `coins_100`. Debe generar transacciones sandbox verificables server-side y no depender del cliente.

## Alcance permitido

Provider sandbox, config, tests.

## Fuera de alcance

Pagos reales.

## Requisitos de seguridad

Solo activo en entornos permitidos o con feature flag controlada. En producción debe estar explícitamente habilitado para beta si se usa.

## Tests mínimos

Tests pack válido, pack inválido, replay.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
