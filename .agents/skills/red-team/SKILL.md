---
name: red-team
description: Red team — adopta el rol de un adversario inteligente y motivado para atacar un plan, diseño, argumento o sistema y encontrar sus puntos débiles antes de que lo haga la realidad (o un atacante real). Úsalo cuando el usuario quiera estresar un plan ya formado, auditar seguridad/robustez, encontrar agujeros en un razonamiento, o pida "/red-team".
trigger: El usuario quiere atacar/estresar un plan, diseño o argumento ya existente; busca vulnerabilidades de seguridad, agujeros lógicos o casos límite; o pide explícitamente red team.
---

# Red Team

## Qué es

Hacer red team es ponerte deliberadamente en el papel de un **adversario competente y motivado** cuyo único objetivo es que el plan/sistema/argumento falle. No buscas mejorarlo con cariño: buscas romperlo. Lo que sobrevive a un red team honesto es robusto; lo que no, revela dónde reforzar.

## Cuándo aplicarlo

- Estresar un plan o diseño ya formado antes de ejecutarlo.
- Auditoría de seguridad: ¿cómo abusaría de esto un atacante? (encaja con la cultura de seguridad de este repo).
- Encontrar casos límite y supuestos frágiles en lógica, reglas o flujos de estado.
- Antes de defender una postura en público/PR: ¿cuál es el mejor contraataque?

## Protocolo

1. **Define el objetivo del ataque.** ¿Qué significa exactamente "fallar" aquí? (datos filtrados, coste amplificado, flujo roto, conclusión falsa, abuso de usuario).
2. **Adopta personas adversarias.** Ataca desde varios ángulos, no solo uno:
   - **El atacante** — busca explotar (auth, límites, validación, escalada, enumeración).
   - **El usuario caótico** — hace lo inesperado (orden raro, inputs extremos, doble tap, offline).
   - **El escéptico lógico** — encuentra el supuesto no demostrado, el salto en el razonamiento, el dato inventado.
   - **La realidad operativa** — red lenta, dependencia caída, escala 100×, race condition.
3. **Lanza ataques concretos.** Para cada persona, enumera vectores específicos. Nada de "podría ser inseguro": di *cómo*, paso a paso.
4. **Clasifica cada hallazgo.** Severidad (crítica/alta/media/baja) × verosimilitud. Las críticas verosímiles van primero.
5. **Verifica antes de afirmar.** No inventes vulnerabilidades plausibles-pero-falsas. Si no estás seguro de que un ataque funcione, márcalo como "a verificar" en vez de afirmarlo.
6. **Defensa propuesta.** Para cada hallazgo que sobreviva, una contramedida concreta.

## Formato de salida

```
## Red team: <objetivo>

### Vectores de ataque (ordenados por severidad×verosimilitud)
1. [CRÍTICA] <persona> — <ataque concreto, paso a paso>
   Cómo: ...
   Defensa: ...
2. [ALTA] ...

### Qué sobrevive al ataque
- <partes que aguantan, para no tocarlas sin motivo>

### Acciones recomendadas
- Las 1–3 defensas a implementar primero
```

## Reglas

- Sé un adversario *honesto*, no uno teatral: ataques reales y específicos, no FUD genérico.
- Distingue lo que demostraste de lo que sospechas. Un hallazgo sin verificar etiquétalo como tal.
- Si el plan aguanta un red team serio, dilo — es información valiosa, no un fracaso del ejercicio.
- En seguridad de este repo: alinéate con las reglas ya aprendidas (App Check, `request.query.limit`, whitelists de campos, account enumeration). Un red team útil empuja MÁS ALLÁ de lo ya cubierto.
- Complementa a `[[steelman]]` (fortalecer una idea) y `[[pre-mortem]]` (asumir el fracaso desde dentro). Red team ataca desde fuera.
