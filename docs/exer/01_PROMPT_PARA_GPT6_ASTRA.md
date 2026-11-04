# Encargo a GPT-6 Astra: preparar el sistema /exer de Molvex

Actúa como responsable de preparar un sistema profesional de **diseño conceptual de ejercicios cognitivos para Molvex**, conforme a este paquete. El usuario quiere invocarlo escribiendo `/exer` en Codex. Mantén GPT-6 Astra como modelo destinatario; comprueba qué modelo y herramientas tienes realmente. No afirmes haber cambiado el modelo por una instrucción textual ni lo sustituyas sin informar al usuario.

## Resultado del encargo de preparación

Deja un conjunto de instrucciones reutilizable, íntegro y comprobado, con la activación que el cliente soporte y el usuario apruebe. No ejecutes todavía un catálogo de ejercicios, no escribas código productivo y no implementes mejoras del motor. Las referencias a implementación de los adjuntos describen otras fases.

Esta orden autoriza preparar las instrucciones del comando en el proyecto autorizado y comprobarlas. Conserva cambios ajenos, no reemplaces `AGENTS.md` ni un comando existente. No publiques, no despliegues, no envíes mensajes y no instales herramientas de pago. Si la instalación requiere una mutación fuera de estos permisos, termina primero el resultado revisable y pide únicamente esa autorización.

## Lectura y contraste antes de preparar archivos del comando

1. Lee [02_DISENO_CONCEPTUAL.md](02_DISENO_CONCEPTUAL.md), [03_INSTALACION_Y_ACTIVACION.md](03_INSTALACION_Y_ACTIVACION.md), [contexto/00_CONTEXTO_VIGENTE.md](contexto/00_CONTEXTO_VIGENTE.md) y [contexto/01_RECONCILIACION.md](contexto/01_RECONCILIACION.md).
2. Lee [comando/EXER.md](comando/EXER.md) y todos los módulos de `sistema/` y `plantillas/` una vez durante la preparación. Conserva un índice de lectura y hashes para futuras ejecuciones incrementales. En ejecución normal usa carga selectiva, sin omitir dependencias obligatorias.
3. Inspecciona las once fuentes originales de `contexto/fuentes/`. Puedes distribuir lecturas entre agentes reales si están disponibles; identifica quién leyó cada archivo, comparte hallazgos y vuelve a la fuente de cualquier punto que influya en una decisión. No afirmes lectura íntegra de resultados truncados.
4. Revisa el grafo de memoria y las fuentes vivas **solo dentro del acceso autorizado** siguiendo `sistema/02_MEMORIA_GRAPHIFY_OBSIDIAN.md`. Verifica actualidad por contenido, versiones, decisiones y referencias; una fecha antigua no basta para declarar obsolescencia.
5. Si hay hechos obsoletos, actualiza la memoria de forma trazable conforme a decisiones ya autorizadas. Si un cambio mejora el proyecto pero cambia su significado, política o diseño, presenta la propuesta al usuario antes de adoptarla, por pequeña que sea. Elabora alternativas mientras esperas y termina las partes independientes.

No inventes acceso a un grafo, repositorio, auditoría, comandos, especialistas ni datos humanos. La ausencia de una herramienta no autoriza construir o instalar otra sin comprobar permisos y alcance. Si faltan fuentes vivas, termina la preparación portable y registra que su integración queda pendiente.

## Materialización futura

Usa `comando/EXER.md` como cuerpo de la entrada. La convención de skill y el empaquetado concreto se resuelven con la documentación vigente del cliente. No modifiques el contenido del encargo para adaptarlo silenciosamente a limitaciones de la plataforma.

Mantén los módulos y sus enlaces relativos juntos. Si el cliente usa skills de Codex, crea una única skill `exer` con descripción específica y referencias locales; no conviertas cada especialidad en una skill global ni cargues todas las fuentes históricas en cada invocación. La especificación exacta de la adaptación está en `03_INSTALACION_Y_ACTIVACION.md`.

Comprueba que escribir `/exer` produce la entrada esperada en **ese cliente**. Si el cliente no permite registrarlo o intercepta el texto, documenta la limitación y pregunta qué alternativa acepta el usuario. No presentes una convención conversacional como comando nativo. No afirmes instalación por el mero hecho de haber escrito Markdown.

## Prueba y entrega de la preparación

Revisa enlaces, integridad de fuentes, prioridades, permisos, carga de módulos y escenarios de `revision/02_ESCENARIOS_DE_ACEPTACION.md`. Con agentes disponibles, realiza una prueba independiente de uso; si no, declara que es una revisión secuencial. No simules ejecución del motor ni eficacia humana.

Entrega:

- Ubicación de las instrucciones preparadas y mecanismo de activación observado.
- Qué fuentes/grafo pudiste verificar y actualizar realmente.
- Qué cambios propones al CEO, con su decisión pendiente.
- Resultado de las pruebas del comando, no de la app.
- Ejemplo concreto de primera invocación útil.

El sistema operativo para sus ejecuciones futuras es `comando/EXER.md`. Este archivo prepara el sistema; no sustituye sus reglas ni inicia automáticamente desarrollo de producto.
