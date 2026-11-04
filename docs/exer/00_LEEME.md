# Molvex · Sistema de diseño cognitivo /exer

**Paquete de instrucciones 1.0 · 15 de septiembre de 2026 · Destinatario: GPT-6 Astra en Codex.**

Este ZIP contiene el diseño conceptual y las instrucciones completas para preparar `/exer`, un sistema dedicado a **diseñar ejercicios**, sus variantes, medición y validación. No contiene una implementación de Molvex ni un catálogo inventado antes de revisar el motor.

## Cómo usarlo

1. Adjunta el ZIP a una conversación de Codex con GPT-6 Astra y acceso autorizado al proyecto Molvex.
2. Envía: **«Lee `00_LEEME.md` y ejecuta el encargo de `01_PROMPT_PARA_GPT6_ASTRA.md`. Prepara el sistema `/exer` conforme al paquete. No implementes ejercicios ni modifiques el motor.»**
3. El agente debe leer primero el diseño conceptual, contrastarlo con tu memoria real y comprobar el mecanismo de activación de tu cliente. Si necesita cambiar una decisión, debe presentarte la propuesta concreta.
4. Cuando tengas la auditoría del motor, adjunta su informe y handoff; usa **`/exer incorporar-auditoria`**. Después puedes pedir **`/exer disenar enfoque`**, acotando el bloque, familia o ejercicio cuando quieras.

Los sufijos de estos ejemplos son instrucciones para el modelo, no comandos de terminal. El identificador `/exer` es el deseado; su registro real depende del cliente. No se debe sustituir silenciosamente por `$exer` o `/prompts:exer`. La comprobación y las alternativas están en [03_INSTALACION_Y_ACTIVACION.md](03_INSTALACION_Y_ACTIVACION.md).

## Qué incluye

| Carpeta/archivo | Uso |
|---|---|
| [01_PROMPT_PARA_GPT6_ASTRA.md](01_PROMPT_PARA_GPT6_ASTRA.md) | Encargo listo para el agente que preparará el comando |
| [02_DISENO_CONCEPTUAL.md](02_DISENO_CONCEPTUAL.md) | Arquitectura completa, componentes, decisiones y fronteras |
| [comando/EXER.md](comando/EXER.md) | Entrada operativa del diseñador |
| `sistema/` | Procedimientos especializados y criterios de calidad |
| `plantillas/` | Contratos y dossiers para resultados completos y revisables |
| `contexto/` | Contexto reconciliado, fuentes científicas y once adjuntos íntegros |
| `revision/` | Trazabilidad, escenarios de aceptación y revisión del paquete |

## Alcance y límites reales

- La conversación aportada contiene decisiones y un **prompt de auditoría**; no contiene los resultados de esa auditoría.
- El repositorio, vault de Obsidian e instalación/índice de Graphify reales no se han aportado. El paquete especifica cómo revisarlos y actualizarlos cuando sean accesibles.
- Las capacidades del motor descritas en los adjuntos son evidencia documental, no comprobaciones nuevas de código, iOS, Android o usuarios.
- Las modalidades profesional y sencilla siguen siendo entrenamiento serio. La menor carga de la segunda no demuestra por sí misma un menor efecto cognitivo.
- El sistema busca la mejor calidad alcanzable con evidencia y alcance definidos. No garantiza perfección científica, eficacia máxima ni equivalencia literal con una cantidad de trabajadores humanos.

El paquete conserva tus fuentes completas con nombres estables y hashes. Los textos históricos mantienen su contenido original, incluido el nombre MindBoost. El contexto reconciliado explica qué decisiones posteriores prevalecen.
