# Backend 03 — Validación estricta de inputs

## Objetivo

Evitar payloads ambiguos o abusivos.

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

Añade validación estricta de DTOs: longitudes, enums, cantidades, UUIDs, body size, campos desconocidos y mensajes de error seguros.

## Alcance permitido

DTO validation y tests.

## Fuera de alcance

Cambiar lógica económica.

## Requisitos de seguridad

Rechaza campos inesperados en endpoints económicos. No aceptar `amount` desde cliente en compras sandbox salvo pack ID permitido.

## Tests mínimos

Tests de payload inválido, campos extra y límites.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
