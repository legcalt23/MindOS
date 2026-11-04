# Activación e instalación futura en Codex

## 1. Estado de este entregable

Este ZIP es un paquete de instrucciones para el agente receptor. **No se ha instalado una skill ni registrado `/exer` en el Codex del usuario.** Tampoco se conocen su versión, cliente o configuración. Este archivo define cómo hacerlo después sin confundir un prompt con un comando nativo.

La documentación oficial consultada el 2026-09-15 describe skills con `SKILL.md`, activación mediante `$` o `/skills` en CLI/IDE y descubrimiento local en `.agents/skills`. [Skills de Codex](https://learn.chatgpt.com/docs/build-skills).

Los custom prompts figuran como obsoletos y su sintaxis documentada usa `/prompts:nombre`. Eso no demuestra soporte de un alias arbitrario `/exer`. [Custom prompts](https://learn.chatgpt.com/docs/custom-prompts).

## 2. Matriz de activación

| Entorno observado | Acción | Cómo describirlo |
|---|---|---|
| Cliente con registro real de comandos/alias | Verificar documentación y permisos; registrar `/exer` sin colisión y probarlo | Comando nativo, solo si se observa su funcionamiento |
| Skills sin alias arbitrario | Preparar `exer`; presentar `$exer` como alternativa para aprobación | Skill, no slash command `/exer` |
| Custom prompts heredados disponibles | Presentar `/prompts:exer` como alternativa compatible, sin recomendar un mecanismo retirado por defecto | Prompt heredado |
| Texto `/exer` llega al modelo sin interceptarse | Puede pactarse una convención en instrucciones de proyecto | Alias conversacional, no registro nativo |
| No se puede inspeccionar o instalar | Entregar instrucciones portables e informe de compatibilidad pendiente | Preparado; activación no comprobada |

El usuario ha pedido `/exer`. No adoptes una alternativa por comodidad. Primero prepara todo el contenido reutilizable; si hace falta cambiar la invocación, plantea una decisión con lo observado y una prueba reproducible. No inventes archivos de configuración para un registrador desconocido.

## 3. Adaptación al formato skill cuando se autorice

Conserva el paquete completo dentro de una carpeta `exer`, con el mismo árbol de referencias. Crea en su raíz un `SKILL.md` pequeño con este frontmatter y la instrucción de entrada:

```markdown
---
name: exer
description: Usar cuando se solicite /exer o diseñar, revisar o actualizar conceptualmente ejercicios cognitivos de Molvex a partir de su memoria de proyecto y motor. No usar para implementar ejercicios ni modificar código del producto.
---

# /exer · Molvex

Lee y aplica `comando/EXER.md` de esta misma carpeta. Ese archivo es la
entrada operativa y enlaza las instrucciones requeridas por cada tarea.
Las instrucciones actuales del usuario prevalecen sobre el material histórico.
```

Este bloque es una especificación para la preparación futura, no un archivo ya instalado. La carpeta de instalación se descubre en el entorno. En un repositorio local con el mecanismo documentado, la ubicación candidata es `.agents/skills/exer/`; una instalación personal administrada puede tener otra ruta y mecanismo de persistencia. Sigue el procedimiento de creación/persistencia de skills realmente disponible, sin atribuirlo al proyecto si no lo es.

No actives herramientas adicionales, hooks, servidores, acceso a red, política de invocación exclusiva ni cambios de permisos por inferencia. Si se necesita una política de activación distinta, propón el ajuste antes. Detecta una skill existente con el mismo nombre y conserva su identidad; no sobrescribas otra finalidad.

## 4. Convención de proyecto opcional

Si el usuario aprueba un alias conversacional y el cliente entrega `/exer` al modelo, se puede añadir un bloque delimitado a las instrucciones de proyecto existentes:

```markdown
Cuando un mensaje comience exactamente por `/exer`, carga la entrada del
sistema Molvex EXER ubicada en la ruta local verificada al instalarlo y
aplica sus módulos pertinentes. Diseña; no implementes el producto.
```

Sustituye la referencia a ruta por la ubicación real antes de guardar. No escribas un enlace roto ni reemplaces el resto de `AGENTS.md`. Prueba en una nueva interacción. Si el cliente rechaza `/exer` antes de entregarlo al modelo, esta convención no lo arregla.

## 5. Comprobación de instalación

Registrar cliente/versión, ruta, mecanismo, colisiones comprobadas, archivos añadidos, fuentes vivas accesibles, consulta de cambios y resultados de pruebas. Probar al menos:

1. Invocación sin argumentos: reconstruye contexto/estado y propone el siguiente diseño acotado o identifica la entrada faltante.
2. Petición de diseño con motor no disponible: produce trabajo condicionado y no inventa compatibilidad.
3. Mejora pequeña de canon: presenta una decisión; no la adopta.
4. Solicitud de «diseñar»: no inicia implementación.
5. Segunda ejecución: conserva decisiones y actualiza solo dependencias afectadas.

Solo afirmar «instalado y operativo» si el registro y una invocación real se comprobaron. El formato correcto de archivos solo demuestra preparación estructural.
