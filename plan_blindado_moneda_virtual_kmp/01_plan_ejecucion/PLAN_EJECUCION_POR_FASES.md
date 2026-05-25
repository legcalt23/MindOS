# Plan de ejecución blindado por fases

## Fase 0 — Preparación y auditoría del repo

Objetivo: entender el estado real del proyecto antes de modificarlo.

Orden:

1. Auditar estructura del repositorio.
2. Detectar stack backend/KMP/DB.
3. Crear documento técnico de decisiones.
4. Definir convenciones de seguridad.

Entregables:

- Mapa del repo.
- Decisiones de stack.
- Lista de riesgos iniciales.
- Plan adaptado al código existente.

---

## Fase 1 — Base backend y seguridad transversal

Objetivo: preparar el backend para operaciones económicas seguras.

Orden:

1. Configurar módulos de seguridad.
2. Añadir correlation/request IDs.
3. Añadir logging estructurado.
4. Añadir rate limiting.
5. Añadir validación estricta de inputs.
6. Añadir error model unificado.
7. Preparar transacciones DB.

Entregables:

- Middleware/API filters.
- Error contract.
- Logs estructurados.
- Rate limits iniciales.
- Tests base.

---

## Fase 2 — Modelo contable: wallet + ledger

Objetivo: implementar el núcleo económico.

Orden:

1. Migraciones DB.
2. Repositorios.
3. Ledger append-only.
4. Balance snapshot.
5. Idempotency keys.
6. Servicio de wallet.
7. Endpoints balance/spend/transactions.
8. Tests de concurrencia.

Entregables:

- Tablas wallet/ledger.
- Servicio transaccional.
- API wallet.
- Tests de double spending.
- Reconciliación ledger/snapshot.

---

## Fase 3 — TPV simulado y preparación pagos reales

Objetivo: dejar todo listo para pagos reales sin cobrar todavía.

Orden:

1. Crear interfaz `PaymentProvider`.
2. Crear `SandboxPaymentProvider`.
3. Crear tablas purchases/provider_events.
4. Implementar compra simulada 1/10/100.
5. Implementar verify purchase vía backend.
6. Preparar adaptadores Google/Apple/Stripe como stubs seguros.
7. Añadir webhook endpoints protegidos, no operativos o sandbox.

Entregables:

- Compra simulada.
- Purchase ledger credit.
- No pago real.
- Preparado para providers reales.

---

## Fase 4 — KMP UI: pantalla Moneda

Objetivo: integrar UX sin comprometer seguridad.

Orden:

1. Añadir modelo shared.
2. Añadir API client.
3. Añadir estado UI.
4. Añadir pantalla “Moneda”.
5. Añadir entrada en Home.
6. Añadir botones comprar 1/10/100.
7. Añadir refresh balance.
8. Manejar errores/desincronización.

Entregables:

- Pantalla Moneda.
- Balance backend.
- Compra sandbox.
- Sin saldo local autoritativo.

---

## Fase 5 — Antifraude inicial

Objetivo: bloquear abuso básico y preparar scoring.

Orden:

1. Fraud signals.
2. Risk scoring.
3. Velocity limits.
4. Device/IP/session metadata.
5. Reglas para promociones y compras sandbox.
6. Wallet freeze automático.
7. Alertas iniciales.

Entregables:

- FraudService.
- Reglas iniciales.
- Audit trail.
- Tests.

---

## Fase 6 — Admin y auditoría

Objetivo: permitir soporte sin abrir agujeros críticos.

Orden:

1. Admin RBAC.
2. Audit logs inmutables.
3. Admin action workflow.
4. Ajustes con justificación.
5. Doble aprobación para importes altos.
6. Vista ledger/user/purchase.
7. Alertas admin.

Entregables:

- Admin endpoints seguros.
- Maker/checker.
- Auditoría completa.

---

## Fase 7 — Infraestructura y DevSecOps

Objetivo: producción segura.

Orden:

1. Docker hardening.
2. Compose local seguro.
3. Terraform base.
4. Secrets management.
5. CI con tests.
6. SAST/dependency scanning.
7. DB migrations pipeline.
8. Backups/recovery docs.

Entregables:

- IaC base.
- Pipeline seguro.
- Hardening.
- Documentación.

---

## Fase 8 — Testing ofensivo/defensivo

Objetivo: probar que el sistema resiste ataques comunes.

Orden:

1. Unit tests.
2. Integration tests.
3. Concurrency tests.
4. Replay tests.
5. Idempotency tests.
6. Webhook spoofing tests.
7. Admin abuse tests.
8. Mobile tamper tests documentados.

Entregables:

- Suite de seguridad.
- Reporte de cobertura.
- Fallos corregidos.

---

## Fase 9 — Auditoría final Red Team

Objetivo: atacar la implementación y remediar.

Orden:

1. Auditoría código.
2. Auditoría DB.
3. Auditoría API.
4. Auditoría KMP.
5. Auditoría pagos sandbox.
6. Auditoría concurrencia.
7. Auditoría admin.
8. Plan de remediación.
9. Re-test.

Entregables:

- Informe final.
- Hallazgos por severidad.
- Parches.
- Checklist de lanzamiento.
