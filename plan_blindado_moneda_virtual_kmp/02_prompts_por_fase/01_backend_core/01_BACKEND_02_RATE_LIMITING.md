# Backend 02 — Rate limiting

## Objetivo

Añadir control de abuso por endpoint.

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

Implementa rate limiting por IP, userId y endpoint con configuración separada para wallet, purchase, admin y auth.

## Alcance permitido

Middleware, config, tests.

## Fuera de alcance

Antifraude avanzado.

## Requisitos de seguridad

Debe fallar cerrado para endpoints económicos si el limitador no está disponible, salvo configuración local dev.

## Tests mínimos

Tests de límites y headers de respuesta.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
