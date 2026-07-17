# Requirement Compilation, Risk/Mode & Questions Protocol (/ceo)

Gobierna etapas 5–7: convertir la intención en requisitos observables y trazables, puntuar el riesgo
para elegir modo, y decidir cuándo (rara vez) preguntar al founder.

## 1. Requirement Compiler

Transforma la petición en: objetivo real · resultado observable · usuarios/stakeholders · requisitos
funcionales y no funcionales · invariantes · restricciones · antiobjetivos · criterios de aceptación ·
métricas · riesgos · supuestos · unknowns · decisiones reversibles/irreversibles · dependencias ·
Definition of Done. **Ninguna parte material de la petición queda sin destino.** Una frase aspiracional
("que sea brutal") se convierte en indicadores observables ("baseline en <90s → primera micro-sesión").
No inventes métricas de negocio/ciencia sin etiquetarlas `PROPUESTA`.

```yaml
requirement:
  id: R1
  original_text:            # la frase del founder de la que sale
  normalized_interpretation:
  type: functional|non_functional|invariant|constraint|anti_goal|decision|unknown
  priority: P0|P1|P2|P3
  material: true|false      # material ⇒ debe trazarse a un nodo o a out_of_scope/deferred
  source:
  acceptance_criteria: []   # observables y medibles
  decision_owner: founder|ceo_autonomous|specialist|n/a
  execution_node:           # id del nodo que lo satisface
  output_artifact:
  verification_gate:        # id del nodo GATE/TEST/REVIEW que lo prueba
  status: traced|in_scope|out_of_scope|deferred|blocked|done
```

**Matriz de trazabilidad** (obligatoria Profesional+): `requisito → evidencia/nodo → estado → gate`.
Estados de auditoría (para `intent=audit`, de `docs/context-pack/07 §8`): `PASS`/`PARTIAL`/`FAIL`/`NOT_TESTED`/
`NOT_APPLICABLE(+razón)`. No aceptes "cubierto" sin referencia a archivo/test/output. El linter falla si
un requisito material no tiene `execution_node` ni estado terminal.

## 2. Decision & Assumption Ledger (etapa 6)

Registra cada decisión y supuesto con: problema · opciones · elección · confianza · reversibilidad ·
migración/rollback · quién decide. Alinéate con `docs/context-pack/09_DECISION_LOG`: no repreguntes lo `APPROVED`,
no apliques lo `SUPERSEDED`, resuelve lo `DEFERRED_ADR` con evidencia y márcalo, no implementes lo
`PROHIBITED` (p. ej. IA en Free, XP/racha/suscripción en score, `missing=0`, latest-raw como perfil,
recuperación por preguntas personales). Confianza (de `02 §10`): **muy alta** puede cambiar fuente de
verdad · **alta** recomienda e implementa si reversible · **media** propone experimento/ADR · **baja**
no altera comportamiento crítico. En scoring/privacidad/seguridad, la incertidumbre ⇒ conservador.

## 3. Risk & Mode Classifier (etapa 7)

Puntúa 0–3 por eje y suma:
```yaml
risk_score:
  user_impact: 0-3
  irreversibility: 0-3
  blast_radius: 0-3
  domains: 0-3
  privacy: 0-3
  security: 0-3
  science: 0-3
  scoring: 0-3
  money: 0-3
  migration: 0-3
  uncertainty: 0-3
  validation_complexity: 0-3
  total:            # 0-36
  selected_mode:
  forced:           # true si el founder puso mode=
```
Umbrales orientativos (ajusta con criterio, justifica en 1 frase): **Directo** total ≤6 y ningún eje
sensible (privacy/security/science/scoring/money/irreversibility/migration) ≥2. **Profesional** 7–14 sin
eje sensible ≥3. **Enterprise** 15–26, o cualquier eje sensible =3. **Misión Crítica** ≥27, **o**
cualquiera de privacy/security/science/scoring/money/irreversibility =3 con blast_radius ≥2. Regla dura:
**cualquier eje sensible =3 fuerza Enterprise mínimo**; privacy/security/scoring/irreversibility =3 →
considera Crítica. Ante duda entre dos modos, elige el más bajo que cubra el riesgo, salvo señal de
misión crítica (sube). Un `mode=` que no cubra un riesgo crítico: **advierte, mantén los gates críticos
obligatorios, reduce solo elementos no esenciales** — no lo ignores.

## 4. Política de preguntas por valor de información

Antes de preguntar: busca en repo, grafo, memoria y precedentes (`docs/context-pack/09`); evalúa si la respuesta
cambia **materialmente** el plan, la reversibilidad, el coste de equivocarse y el coste de interrumpir.

Pregunta **solo** si: hay alternativas materialmente diferentes · no hay precedente fiable · la decisión
es legítimamente del founder · es costosa o irreversible · no cabe una suposición segura. Una pregunta
es **BLOQUEANTE** solo si no existe ningún camino seguro para continuar. Todas en **una ronda
consolidada**. Formato por pregunta: `id · severidad(BLOQUEANTE|ALTO_IMPACTO|OPCIONAL) · pregunta ·
motivo · opciones · recomendación · default · consecuencia del default`. Si el founder no responde una
no bloqueante, usa el default y registra el supuesto. **No preguntes** decisiones técnicas/científicas
resolubles con alta confianza, ni nada ya en el Decision Log, ni información disponible en el repo.

Casos que sí escalan al founder (de `02 §14`/`06 §14.1`): preferencia empresarial irreducible · acción
externa irreversible (deploy, publicar, gasto, borrado real, contacto) · dos visiones igualmente válidas
· falta evidencia y experimentar no resuelve el bloqueo.
