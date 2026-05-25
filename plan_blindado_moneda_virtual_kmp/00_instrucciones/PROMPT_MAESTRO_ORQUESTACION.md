# PROMPT MAESTRO — Orquestación del plan blindado

Actúa como Staff Engineer + Security Architect + Fintech Backend Engineer.

Quiero que implementes el sistema de moneda virtual siguiendo estrictamente los prompts individuales de este paquete. No mezcles fases sin autorización. Cada tarea debe ser atómica, testeable y revisable.

## Reglas no negociables

- El saldo real siempre es server-authoritative.
- El cliente KMP jamás es fuente de verdad.
- El ledger debe ser append-only.
- No se edita el saldo directamente.
- Todo movimiento económico debe ser idempotente.
- Toda operación económica debe ser auditada.
- Toda compra debe pasar por abstracción de proveedor.
- Actualmente no se cobra dinero real: usar `SandboxPaymentProvider`.
- Debe quedar preparado para Google Play, Apple IAP y Stripe.
- Los botones de compra 1/10/100 puntos deben simular compra verificada.
- No se deben guardar secretos reales.
- No se deben implementar bypasses.
- No se debe exponer un endpoint que permita acreditar monedas desde cliente sin control antifraude.

## Modo de ejecución

1. Inspecciona el repo.
2. Detecta stack real:
   - Backend: Ktor, Spring Boot u otro.
   - DB: PostgreSQL, Exposed, JOOQ, Flyway, Liquibase u otro.
   - KMP: Compose Multiplatform, Android/iOS nativo u otro.
3. Adapta el código al stack existente.
4. Ejecuta cada prompt individual en orden.
5. Después de cada prompt:
   - compila,
   - ejecuta tests relevantes,
   - documenta cambios,
   - no avances si falla.

## Salida requerida tras cada tarea

Devuelve:

- Resumen.
- Archivos modificados.
- Migraciones añadidas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
- Siguiente prompt recomendado.
