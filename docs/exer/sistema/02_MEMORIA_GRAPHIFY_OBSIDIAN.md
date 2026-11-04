# Memoria de proyecto: Graphify y Obsidian

## Propósito

Basar cada diseño en el conocimiento vigente de Molvex, detectar contexto obsoleto y mantener trazabilidad sin crear una segunda fuente de verdad. Este protocolo no supone que el grafo, vault, código o Graphify sean accesibles. En este paquete no se incluyeron.

## 1. Descubrir la configuración real

Dentro del proyecto y fuentes autorizadas, localizar instrucciones de memoria, vault, índices, archivos de configuración pertinentes, rutas canónicas, manifests, registros de decisiones y documentación de la instalación Graphify. Leer archivos de instrucciones antes de actuar. Identificar versión y capacidades mediante documentación o ayuda del binario realmente instalado; no inventar comandos `graphify update`, schemas, bases o endpoints.

Registrar: raíces accesibles y excluidas; repositorio/commit y cambios sin confirmar; vault y su rol; Graphify/proveedor/versión si son identificables; índice y fuente de verdad que representa; último snapshot verificable; operaciones de lectura y actualización disponibles; si el procesamiento sale del dispositivo o genera coste. No mostrar secretos de configuración.

No instalar otro Graphify, crear un grafo paralelo, enviar el vault a un proveedor externo o cambiar su arquitectura para completar una consulta. Esas son propuestas nuevas. Si solo existen notas exportadas, tratarlas como snapshot y no como grafo vivo.

## 2. Aprovechar cada componente sin atribuirle autoridad indebida

Obsidian permite navegar notas y enlaces; su grafo muestra relaciones entre notas, no veracidad ni aprobación. [Documentación de Graph view](https://obsidian.md/help/plugins/graph).

Para Graphify, usar las consultas, relaciones con código, extracción o índices que ofrezca **esa instalación**, con su procedencia. Si una relación fue inferida por un modelo, conservar ese origen y contrastarla. Un nodo muy conectado o una arista extraída no demuestra causalidad, implementación ni vigencia.

Obsidian puede mantener decisiones y explicaciones curadas; Graphify puede facilitar recuperación e impacto según capacidades reales. Mantener un único identificador conceptual estable y referencias a fuentes, no dos políticas editables independientes.

## 3. Recuperación orientada a la tarea

Partir del objetivo y buscar: Molvex/MindBoost; bloque; constructos; ejercicios/familias; auditorías; decisiones; métricas; adaptación; exposición; accesibilidad; versiones y límites del motor. Seguir dependencias pertinentes y volver al texto/código original.

Crear una tabla de afirmaciones críticas con fuente directa y relaciones. Revisar especialmente entradas negativas: decisiones rechazadas, superseded, bloqueos y resultados que contradicen el diseño. No usar únicamente búsqueda semántica: comprobar IDs, enlaces y referencias literales con búsqueda exacta.

No cargar todo el vault por defecto. Ampliar una búsqueda cuando una pregunta decisiva siga sin resolver. Registrar consultas/rutas cubiertas y límites. Datos de usuarios, credenciales o archivos de otros proyectos no son contexto necesario.

## 4. Criterios de actualidad

Comparar objetos, no solo timestamps:

- Hash/versión de la fuente y snapshot contra la versión referenciada por el índice.
- ADR nuevos o reemplazados, su aprobación y fecha efectiva.
- Código de las capacidades realmente usadas y cambios locales aún no incluidos en HEAD.
- Enlaces rotos, IDs duplicados, documentos canónicos incompatibles y artefactos derivados huérfanos.
- Cambios de constructo, ejercicio, generador, banco, métrica, scoring, adaptación, blueprint y elegibilidad.

Clasificar cada hallazgo: `CURRENT`, `STALE_FACTUAL`, `CONFLICTED`, `UNVERIFIED`, `MISSING`, `IMMUTABLE_SNAPSHOT`. Un archivo antiguo puede seguir vigente. Un archivo subido hoy puede contener decisiones obsoletas. Conservar el canon histórico `docs/context_pack` y el estado de `docs/context-pack`/`PHASE0_FOCUS_FINAL` documentado en el paquete hasta verificar decisiones posteriores.

## 5. Actualización ya autorizada frente a propuesta

**Actualización factual:** incorporar un hecho nuevo verificado, corregir un enlace cuya identidad sea inequívoca, enlazar una decisión ya aprobada, registrar que un claim carece de verificación, refrescar una vista derivada de esas fuentes. Conservar la historia y la distinción entre hecho documental y código observado.

**Cambio pendiente del CEO:** nueva política, umbral, taxonomía, significado, fusión/eliminación de conceptos, cambio de canon, reestructuración del vault, eliminación histórica, nuevas fuentes externas o promoción de recomendaciones. No lo apliques aunque sea P3, reversible o tenga gran confianza. Preparar un delta conceptual y pregunta.

Puede registrarse una propuesta como `PROPOSED` en el espacio de trabajo de diseño; eso no es incorporarla como canon. Si no existe una ubicación autorizada para propuestas, entregar un Markdown aparte.

## 6. Transacción de actualización

1. Congelar lista de objetos, hashes/versiones y evidencia que justifica cada cambio.
2. Preparar copia recuperable o mecanismo de versiones ya existente para las fuentes que se tocarán. No duplicar datos privados innecesarios.
3. Construir delta mínimo; escribir fuentes canónicas solo cuando el cambio sea factual autorizado. No editar índices derivados a mano si su formato depende del generador.
4. Justo antes de aplicar, comparar hashes/versiones. Si otro actor cambió el archivo, detener ese delta, leer lo nuevo y reconciliar; no sobrescribirlo.
5. Actualizar notas/decisiones mediante la vía autorizada y regenerar únicamente los derivados afectados con el mecanismo real. No ejecutar operaciones destructivas o de coste no autorizado.
6. Verificar fuente→nodo→relación→dossier con consultas de ejemplo; detectar enlaces rotos y afirmaciones que cambiaron de estado.
7. Registrar lo aplicado, lo no aplicado, verificación, riesgos y reversión. En fallo parcial, mantener estado `PARTIAL`, restaurar solo modificaciones propias cuando sea seguro y dejar claro qué índice no quedó actualizado.

El éxito de una herramienta no prueba por sí solo la fidelidad de todas las relaciones. Comprobar al menos una consulta de decisión vigente, una relación histórica superseded y una dependencia del diseño afectada.

## 7. Esquema semántico propuesto, no migración automática

Objetos útiles: decisión, constructo, familia, ejercicio/versión, requisito del motor, estudio, métrica, riesgo, hallazgo y ejecución de diseño. Relaciones útiles: `supported_by`, `contradicts`, `supersedes`, `depends_on`, `trains_hypothesis`, `observes`, `requires`, `validated_in`, `proposed_change_to`.

Reutilizar tipos existentes cuando cubran el significado. Si no, proponer el cambio antes de crear una ontología nueva. Cada relación causal o de entrenamiento mantiene su estado de hipótesis; `trains_hypothesis` no se transforma en «mejora demostrada» por repetición.

## 8. Salida y bloqueo

Usar [memoria y reanudación](../plantillas/07_MEMORIA_Y_REANUDACION.md). Si el grafo no es accesible, entregar `MEMORY_ACCESS_MISSING`, fuentes locales usadas, reconciliación propuesta, consultas pendientes y la mínima referencia que necesita el usuario aportar. Continuar diseño conceptual que no dependa del dato faltante. Nunca declarar «grafo revisado/actualizado» cuando solo se han leído adjuntos.
