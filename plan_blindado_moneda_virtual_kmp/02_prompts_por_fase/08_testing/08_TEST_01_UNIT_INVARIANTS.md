# Testing 01 — Unit tests de invariantes

## Objetivo

Probar reglas económicas.

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

Añade unit tests para invariantes: saldo derivado, snapshot, no amount cero, reversos, idempotencia request hash, pack permitido.

## Alcance permitido

Tests.

## Fuera de alcance

Nuevas features.

## Requisitos de seguridad

Los tests deben fallar si se rompe append-only o server-authoritative.

## Tests mínimos

Ejecutar suite unitaria.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
