# KMP 01 — Modelos shared y API client

## Objetivo

Conectar KMP al backend wallet.

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

Añade modelos shared para balance, transactions y sandbox purchase. Implementa API client con manejo de errores y request IDs.

## Alcance permitido

Shared module, client, tests si existen.

## Fuera de alcance

Pantalla UI.

## Requisitos de seguridad

No persistir saldo como fuente de verdad. No guardar secrets.

## Tests mínimos

Tests de serialization/error mapping.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
