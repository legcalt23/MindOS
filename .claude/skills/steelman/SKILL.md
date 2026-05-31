---
name: steelman
description: Steelman — construye la versión MÁS fuerte y caritativa de una postura, idea o argumento (lo contrario de un hombre de paja) antes de evaluarla o rebatirla. Úsalo cuando el usuario quiera evaluar con justicia una opción que le disgusta, entender el lado contrario, decidir entre alternativas, o pida "/steelman".
trigger: El usuario quiere evaluar de forma justa una idea/postura (especialmente una con la que no está de acuerdo), comparar alternativas, o pide explícitamente un steelman.
---

# Steelman

## Qué es

Hacer steelman de un argumento es reconstruirlo en su forma **más fuerte, más coherente y más caritativa** posible — incluso más fuerte de lo que lo planteó su defensor original — *antes* de juzgarlo o criticarlo. Es el antídoto contra el hombre de paja (atacar una versión débil y deformada). Solo derrotar al steelman significa algo.

## Cuándo aplicarlo

- Antes de descartar una opción técnica o de producto que "obviamente" no te gusta.
- Cuando hay que elegir entre 2+ enfoques y quieres una comparación honesta, no sesgada hacia tu favorito.
- Para entender una decisión heredada del proyecto antes de cambiarla (¿qué problema real resolvía?).
- Cuando el usuario tiene una opinión fuerte y quiere ponerla a prueba de verdad.

## Protocolo

1. **Enuncia la postura tal cual.** Resume en 1–2 frases la idea/argumento a fortalecer, sin caricatura.
2. **Asume buena fe y competencia.** Pregúntate: "¿Qué sabría, valoraría o priorizaría alguien inteligente y bien informado para defender esto?"
3. **Construye el mejor caso.** Reúne:
   - Las premisas más sólidas que lo sostienen.
   - Los contextos donde es claramente correcto.
   - La evidencia o principios a su favor.
   - El coste real de NO hacerlo.
4. **Repara debilidades obvias.** Si el argumento original tiene un agujero fácil pero el núcleo es salvable, parchéalo y presenta la versión reparada.
5. **Solo entonces evalúa.** Con el steelman en pie, da tu juicio: ¿se sostiene? ¿bajo qué condiciones? ¿qué lo derribaría?

## Formato de salida

```
## Steelman: <postura>

### El caso más fuerte a favor
- Premisa sólida 1: ...
- Premisa sólida 2: ...
- Funciona especialmente cuando: <condiciones>
- Coste de no hacerlo: ...

### Evaluación (tras fortalecerlo)
- Qué sobrevive al escrutinio: ...
- Bajo qué condiciones es la opción correcta: ...
- Qué lo derribaría: ...
- Veredicto: ...
```

## Reglas

- Prohibido el hombre de paja. Si te sorprendes eligiendo la versión más fácil de atacar, vuelve atrás.
- El steelman debe ser reconocible y aprobado por un defensor real de la postura ("sí, eso es exactamente lo que quiero decir, mejor dicho").
- Steelman ≠ estar de acuerdo. Puedes construir el caso más fuerte y aun así rechazarlo razonadamente — pero el rechazo ahora vale algo.
- Útil emparejado con `[[red-team]]`: fortalece una postura (steelman) y luego atácala (red team) para ver si aguanta. Si la decisión es delicada, pasa después por `[[pre-mortem]]`.
