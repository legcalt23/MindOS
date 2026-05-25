# Checklist auditoría final

## Ataques cliente

- [ ] Cambiar saldo local no afecta backend.
- [ ] Repetir request no duplica gasto.
- [ ] Repetir compra sandbox no duplica crédito.
- [ ] Cambiar pack inválido se rechaza.
- [ ] Cliente no puede enviar amount arbitrario.

## Ataques API

- [ ] Missing auth => 401.
- [ ] UserId tampering => 403/404 seguro.
- [ ] Idempotency key con body diferente => 409.
- [ ] Rate limit funciona.
- [ ] Campos extra rechazados.
- [ ] Webhook sin firma/config => rechazo.

## Concurrencia

- [ ] 100 spends simultáneos no superan saldo.
- [ ] 100 purchase verifies simultáneos acreditan una vez.
- [ ] Snapshot coincide con ledger.

## Admin

- [ ] Sin rol admin no accede.
- [ ] Admin no puede autoaprobar ajuste sensible.
- [ ] Ajuste crea ledger, no edita saldo.
- [ ] Audit log creado.

## Infra

- [ ] No secretos en repo.
- [ ] CI ejecuta tests.
- [ ] Docker no corre como root si aplica.
- [ ] `.env.example` sin secretos reales.
- [ ] Terraform no contiene credenciales reales.

## GO/NO-GO

- [ ] Sin hallazgos críticos.
- [ ] Sin hallazgos altos sin mitigación.
- [ ] Tests verdes.
- [ ] Migraciones probadas.
- [ ] Rollback documentado.
