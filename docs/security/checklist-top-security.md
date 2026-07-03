# Checklist de cierre de seguridad agentic

## Configuracion y alcance

- [ ] `.codex/config.toml` usa solo Multi-Agent V2 y limita concurrencia.
- [ ] No existen claves de esquema inventadas activas.
- [ ] La configuracion global del operador no fue modificada automaticamente.
- [ ] El estado diferencia `package validated` de `runtime hardened`.
- [ ] No hubo red, deploy, cloud, secretos ni dependencias nuevas.

## Agentes y RBAC

- [ ] Los 17 agentes nuevos parsean como TOML.
- [ ] Solo Remediator tiene escritura logica y requiere A2.
- [ ] Verifier es independiente y de solo lectura.
- [ ] A3 exige dos aprobaciones y RoE verificadas.
- [ ] No hay delegacion recursiva ni autoactivacion legacy.
- [ ] Los 51 agentes legacy estan inventariados y denegados por defecto.

## Controles

- [ ] Writer lock rechaza colisiones y conserva historial append-only.
- [ ] Secret-path guard bloquea por nombre sin abrir el objetivo.
- [ ] Egress checker deniega por defecto y nunca conecta.
- [ ] DLP detecta canarios sin reproducirlos.
- [ ] Ledger solo acepta metadatos saneados y hashes.
- [ ] Receipt validator aplica segregacion y campos obligatorios.

## Evidencia y salida

- [ ] Findings separan evidencia, inferencia y proof gaps.
- [ ] Rutas compartidas son relativas y no sensibles.
- [ ] `AGENTS.md` fue fusionado, no reemplazado.
- [ ] `git diff --check` pasa.
- [ ] Evals locales pasan sin writes fuera del area temporal.
- [ ] Riesgos residuales y gates pendientes quedan explicitos.
