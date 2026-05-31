---
name: primeros-principios
description: Primeros principios — descompón un problema hasta sus verdades fundamentales y razona hacia arriba desde ahí, descartando supuestos heredados, convenciones y razonamiento por analogía. Úsalo cuando el usuario esté atascado, copiando un patrón "porque así se hace", cuestionando si el enfoque es el correcto de raíz, o pida "/primeros-principios".
trigger: El usuario quiere replantear un problema desde cero, sospecha que un supuesto heredado es el cuello de botella, está copiando una solución por analogía sin entender el porqué, o pide razonamiento desde primeros principios.
---

# Primeros principios

## Qué es

Razonar desde primeros principios significa **descomponer un problema hasta los hechos que sabes que son verdad** (no porque alguien lo dijo o porque "siempre se ha hecho así") y reconstruir la solución desde esa base. Es lo contrario de razonar por analogía ("hagámoslo como X porque a X le funcionó"). Disuelve supuestos que parecen restricciones pero no lo son.

## Cuándo aplicarlo

- Cuando un problema parece sin salida bajo el enfoque actual.
- Cuando la solución propuesta es "copia de cómo lo hace otro" sin entender por qué encaja aquí.
- Cuando una "restricción" se da por sentada y nunca se ha cuestionado ("tiene que ser así porque...").
- Para decisiones de arquitectura fundacionales donde la inercia es cara.

## Protocolo

1. **Enuncia el problema y la solución por defecto.** ¿Qué se quiere lograr y cuál es el enfoque que "todo el mundo" daría por hecho?
2. **Lista los supuestos.** Saca a la superficie TODO lo que se está dando por cierto: restricciones, requisitos, "tiene que", "no se puede", convenciones del stack.
3. **Interroga cada supuesto.** Para cada uno: "¿Esto es una ley física/lógica/de negocio real, o es una convención heredada?" Distingue:
   - **Verdades fundamentales** — irreducibles, demostrables (límites de la plataforma, leyes de coste, requisitos legales reales, matemática).
   - **Supuestos disfrazados de verdades** — se pueden eliminar o renegociar.
4. **Reconstruye desde el fondo.** Con solo las verdades fundamentales sobre la mesa, ¿cuál es la solución que emerge? No mires la solución por defecto hasta tener la tuya.
5. **Compara.** Pon la solución desde primeros principios junto a la por defecto. Si coinciden, ahora *sabes por qué* la convención es correcta. Si difieren, puede que hayas encontrado algo mejor.

## Formato de salida

```
## Primeros principios: <problema>

### Solución por defecto (la heredada)
...

### Supuestos en juego
- <supuesto> → ¿verdad fundamental o convención? → veredicto
- ...

### Verdades fundamentales (lo que de verdad restringe)
- ...

### Solución reconstruida desde la base
...

### Veredicto
- Coincide con la convención (ahora justificada) / propone algo distinto + por qué
```

## Reglas

- El paso clave es el 3: la mayoría de los "no se puede" resultan ser "nadie lo ha intentado distinto". Sé escéptico con cada restricción.
- No confundas "es difícil" o "es incómodo" con "es imposible". Solo lo segundo es una verdad fundamental.
- No tires convenciones por deporte. El objetivo es *entender* la base, lo que a veces confirma la convención y otras la derriba.
- Mantén los pies en la tierra: las verdades fundamentales incluyen restricciones reales del proyecto (KMP, Firebase, coste, plazos). Replantear ≠ ignorar la realidad.
- Complementa a `[[steelman]]` (fortalecer la idea existente) y `[[red-team]]` (atacarla). Primeros principios pregunta si la idea es siquiera la correcta de raíz.
