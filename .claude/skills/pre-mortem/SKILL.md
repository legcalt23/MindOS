---
name: pre-mortem
description: Pre-mortem — antes de comprometerte con una decisión, plan o cambio de arquitectura, imagina que YA fracasó por completo y razona hacia atrás para descubrir por qué. Úsalo cuando el usuario vaya a tomar una decisión importante, fijar una arquitectura, lanzar una feature, o pida "/pre-mortem". También cuando una decisión sea difícil de revertir.
trigger: El usuario pide un pre-mortem, va a comprometerse con una decisión costosa/irreversible, o quiere anticipar modos de fallo antes de ejecutar.
---

# Pre-mortem

## Qué es

Un pre-mortem invierte la pregunta del análisis de riesgos. En vez de "¿qué podría salir mal?", asumes que **ya estamos en el futuro y la decisión fracasó de forma rotunda**, y trabajas hacia atrás para explicar por qué. Esta inversión rompe el optimismo por defecto y saca a la luz fallos que un análisis "hacia delante" pasa por alto.

## Cuándo aplicarlo

- Antes de fijar una decisión de arquitectura difícil de revertir (esquema de datos, contrato de API, modelo de navegación).
- Antes de lanzar una feature de cara al usuario o un cambio de seguridad.
- Cuando el coste de equivocarse es alto y el camino parece "obviamente correcto" (señal de exceso de confianza).

## Protocolo

1. **Fija el escenario.** Enuncia en una frase la decisión/plan concreto que se va a evaluar. Si es ambiguo, pídele al usuario que lo concrete.
2. **Salta al futuro.** "Han pasado N meses. Esto ha sido un fracaso claro." Declara el horizonte temporal explícito.
3. **Genera causas de muerte.** Lista 5–10 razones distintas por las que fracasó. Cúbrelas en categorías:
   - Técnicas (arquitectura, rendimiento, deuda, integración).
   - De producto/usuario (nadie lo usa, fricción, malentendido).
   - Operativas (coste, mantenimiento, on-call, escalado).
   - Externas (dependencias, plataforma, legal/compliance).
   - Humanas (suposición incorrecta del equipo, falta de skill, comunicación).
4. **Para cada causa:** estima probabilidad (alta/media/baja) e impacto (alto/medio/bajo). No pierdas tiempo en las baja/baja.
5. **Señal más fuerte primero.** Ordena por probabilidad×impacto y resalta las 2–3 que más deberían preocupar.
6. **Mitigación o kill.** Para cada causa top: o una mitigación concreta y barata que se pueda meter AHORA, o la recomendación de no proceder / cambiar de enfoque.

## Formato de salida

```
## Pre-mortem: <decisión> — horizonte <N meses>

### Causas de fallo (ordenadas por riesgo)
1. **<causa>** — prob: alta · impacto: alto
   Por qué pasaría: ...
   Mitigación: ... (o "sin mitigación barata → reconsiderar")
2. ...

### Veredicto
- Proceder / proceder con mitigaciones / reconsiderar
- Las 1–3 acciones concretas a meter antes de ejecutar
```

## Reglas

- Sé concreto y técnico; nada de riesgos genéricos ("podría haber bugs"). Ata cada causa a algo específico de ESTE plan.
- No endulces. El valor del pre-mortem es decir lo incómodo antes de que sea caro.
- Si tras el análisis la decisión sigue siendo sólida, dilo claramente — un pre-mortem también puede aumentar la confianza.
- Relacionado: `[[red-team]]` ataca un plan ya hecho desde fuera; el pre-mortem asume el fracaso desde dentro. `[[primeros-principios]]` cuestiona si el plan es siquiera el correcto.
