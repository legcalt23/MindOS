# MindBoost — Scientific Measurement Charter

**Versión:** 1.0.0  
**Fecha:** 2026-07-10  
**Owner científico:** MindBoost  
**Estado:** APPROVED como política de producto; las fórmulas concretas siguen el ciclo de hipótesis → validación  
**Ámbito:** constructos, medición, scoring, progreso, transferencia, adaptación y claims

---

## 1. Propósito

Este documento impide que MindBoost confunda una experiencia convincente con una medición válida.

Define el contrato científico que deben respetar producto, ejercicios, analítica, personalización, AI Coach, rankings y comunicación. Su objetivo no es aparentar precisión, sino producir estimaciones tan útiles, comparables y honestas como permita la evidencia disponible.

Principio rector:

> Si el sistema no puede distinguir una mejora cognitiva de práctica, dificultad, dispositivo, azar, fatiga o error de medida, no debe presentar esa diferencia como progreso demostrado.

---

## 2. Alcance y límites

Este charter cubre:

- Qué se pretende medir.
- Qué observaciones pueden convertirse en evidencia.
- Cómo separar entrenamiento, evaluación y transferencia.
- Cómo estimar rendimiento y cambio.
- Cuándo una cifra es provisional, oficial, obsoleta o no comparable.
- Cómo controlar práctica, dificultad, latencia, accesibilidad y contexto.
- Qué claims puede hacer MindBoost.
- Qué evidencia se exige antes de endurecer un claim.

No convierte MindBoost en:

- Una prueba de cociente intelectual.
- Un instrumento diagnóstico o clínico.
- Una evaluación neuropsicológica profesional.
- Una certificación laboral o académica.
- Una herramienta para tomar decisiones de empleo, educación, crédito, seguro o salud.

Toda pantalla, exportación y texto comercial debe respetar esos límites.

---

## 3. Reglas epistémicas no negociables

1. **Medir antes de afirmar.** La misión no sustituye a la evidencia.
2. **Separar constructo y métrica.** Tiempo de reacción no equivale por sí solo a velocidad cognitiva.
3. **Separar señal y contexto.** Dispositivo, práctica, sueño, fatiga y exposición pueden alterar el resultado.
4. **No rellenar ausencias.** Un dato faltante nunca vale cero ni “promedio”.
5. **No premiar el entrenamiento con el propio score.** XP, racha y volumen no elevan capacidad estimada.
6. **No usar la última partida como estado cognitivo.** La estimación debe agregar evidencia suficiente y ponderada.
7. **No fingir invariancia.** Dos versiones, dispositivos o dificultades solo son comparables si existe un puente defendible.
8. **Comunicar incertidumbre.** Una cifra sin calidad de evidencia es incompleta.
9. **Controlar multiplicidad.** Buscar muchas métricas hasta encontrar una positiva no valida un efecto.
10. **Versionar todo lo que cambia significado.** Ítems, reglas, escalas, algoritmos y normas necesitan identidad y linaje.
11. **Tratar cada fórmula inicial como una hipótesis.** El código correcto no demuestra validez psicométrica.
12. **Favorecer conclusiones conservadoras.** Ante evidencia ambigua, usar el estado menos afirmativo.

---

## 4. Modelo de constructos

### 4.1 Taxonomía visible

MindBoost conserva ocho motores visibles por continuidad de producto:

1. Enfoque.
2. Decisiones.
3. Lógica.
4. Creatividad.
5. Lenguaje.
6. Emociones.
7. Velocidad mental.
8. Memoria.

Estos nombres son interfaces de navegación y agrupaciones funcionales. No implican que:

- Sean inteligencias independientes.
- Cubran toda la cognición humana.
- Formen una teoría psicométrica validada.
- Cada bloque sea unidimensional.
- Los ocho tengan la misma fiabilidad o sensibilidad al cambio.

### 4.2 Modelo interno

Cada ejercicio debe apuntar a una o más **subhabilidades operativas**. Ejemplos:

- Atención sostenida, selectiva y control de distractores.
- Control inhibitorio, flexibilidad y cambio de regla.
- Decisión, priorización y evaluación de riesgo bajo condiciones delimitadas.
- Memoria de trabajo verbal o visuoespacial, recuperación y manipulación.
- Velocidad de procesamiento bajo condiciones definidas.
- Razonamiento relacional, cuantitativo, abstracto o secuencial.
- Fluidez, flexibilidad y asociación creativa con control cultural.
- Comprensión, acceso léxico, semántica y precisión contextual.
- Reconocimiento emocional, perspectiva y autoconciencia funcional no clínica.
- Metacognición y calibración de confianza.

Cada subhabilidad necesita definición operacional, indicadores observables, amenazas a la validez y límites de interpretación.

### 4.3 Jerarquía de inferencia

La inferencia debe ascender de manera explícita:

```text
evento válido
  → respuesta/ensayo
    → métrica de sesión
      → estimación por ejercicio
        → estimación de subhabilidad
          → resumen de bloque
            → perfil global, si existe cobertura suficiente
```

No se puede saltar un nivel sin justificar la agregación.

### 4.4 Registro de constructos

Cada subhabilidad tendrá un registro versionado con:

- `constructId` estable.
- Nombre visible e interno.
- Definición inclusiva y exclusiva.
- Evidencia teórica.
- Tareas que la observan.
- Métricas primarias y secundarias.
- Variables de confusión.
- Población y rango de uso.
- Evidencia de fiabilidad, validez y sensibilidad.
- Versión y fecha de revisión.
- Claims permitidos.

---

## 5. Cinco objetos que nunca deben confundirse

| Objeto | Pregunta | Puede alimentar score oficial |
|---|---|---|
| Telemetría | ¿Qué ocurrió en el dispositivo? | Solo después de validación e integridad. |
| Rendimiento entrenado | ¿Cómo ejecutó la tarea practicada? | Sí, pero identificado como rendimiento específico. |
| Medición del constructo | ¿Qué sugiere una tarea controlada sobre una capacidad? | Sí, con calibración, comparabilidad e incertidumbre. |
| Progreso longitudinal | ¿Existe cambio fiable frente al propio baseline? | Solo con suficiente evidencia y control de práctica. |
| Transferencia | ¿El cambio aparece en tareas nuevas o vida cotidiana? | Es un resultado distinto; nunca se infiere automáticamente del entrenamiento. |

La interfaz debe nombrar correctamente cada objeto.

---

## 6. Dos carriles: entrenamiento y medición

### 6.1 Carril de entrenamiento

Prioriza aprendizaje, reto óptimo, variedad, feedback y adherencia.

- Puede adaptar dificultad con rapidez.
- Puede generar estímulos procedimentalmente.
- Puede repetir una habilidad con formatos variados.
- Ofrece feedback detallado, pero no invasivo.
- Permite practicar tanto como quiera el usuario.
- No tiene que maximizar precisión psicométrica en cada partida.

### 6.2 Carril de medición

Prioriza comparabilidad, cobertura e integridad.

- Usa blueprints y condiciones controladas.
- Emplea bancos versionados o generación calibrada.
- Limita exposición y repetición.
- Registra dispositivo, versión, latencia y condiciones relevantes.
- Agrega evidencia; no depende de una única sesión.
- Devuelve feedback agregado para reducir enseñanza de ítems.

### 6.3 Probes embebidos

Se permiten microevaluaciones dentro del entrenamiento si:

- El usuario sabe que MindBoost usa parte de la sesión para actualizar su perfil.
- Los probes se identifican internamente y no revelan respuestas reutilizables.
- Cumplen el mismo contrato de integridad que una evaluación.
- No se seleccionan de forma que favorezca artificialmente la estimación.
- Su exposición está controlada.

### 6.4 Evaluaciones explícitas

Habrá:

- Evaluación inicial rápida para un perfil provisional.
- Calibración progresiva durante varios días.
- Reevaluación estructurada mensual.
- Probes continuos entre reevaluaciones.

El plan puede comenzar inmediatamente, pero debe declarar cuándo aún es provisional.

---

## 7. Estados de evidencia de sesión

Cada sesión o probe debe tener un estado calculado, auditable y reproducible:

### 7.1 `TRAINING_ONLY`

Sirve para practicar, feedback y adaptación local. No actualiza una estimación oficial.

Casos típicos:

- Repetición excesiva.
- Modo práctica.
- Versión no calibrada.
- Accesibilidad o dispositivo sin puente de comparabilidad.
- Condición experimental no aprobada.

### 7.2 `PROVISIONAL`

Puede actualizar un perfil preliminar, pero no un score oficial comparable.

Casos típicos:

- Usuario aún sin calibrar.
- Sesión offline pendiente de sincronización e integridad.
- Nueva versión con evidencia insuficiente.
- Baja cobertura o calidad técnica.

### 7.3 `OFFICIAL_ELIGIBLE`

Cumple blueprint, integridad, comparabilidad y exposición. Puede incorporarse a la estimación oficial.

### 7.4 `INVALIDATED`

Se conserva el evento necesario para auditoría, pero se excluye de estimaciones.

Motivos codificados:

- Interrupción severa.
- Latencia fuera de rango.
- Clock inconsistente.
- Duplicado.
- Versión corrupta.
- Patrón imposible o automatizado.
- No cumple cobertura mínima.
- Condición no comparable.

La invalidación no debe castigar al usuario. Se explica en lenguaje neutral y ofrece repetir si procede.

---

## 8. Contrato de métricas

Toda métrica debe declarar:

1. Nombre y propósito.
2. Unidad original.
3. Dirección: mayor es mejor, menor es mejor o forma no monótona.
4. Eventos de entrada.
5. Filtros de validez.
6. Tratamiento de omisiones, anticipaciones y outliers.
7. Corrección velocidad–precisión.
8. Dependencia de dificultad.
9. Dependencia de dispositivo o modalidad.
10. Agregador de ensayo a sesión.
11. Agregador longitudinal.
12. Error o estabilidad estimada.
13. Versión de fórmula.
14. Tests con casos input → output.
15. Evidencia que permite interpretarla.

### 8.1 Precisión

No basta con `correctas / total` cuando:

- Hay dificultad desigual.
- Existe opción de omitir.
- El azar puede producir aciertos.
- Las respuestas tienen crédito parcial.
- La velocidad modifica la estrategia.

La transformación debe ser específica del ejercicio y estar documentada.

### 8.2 Tiempo de respuesta

Política base:

- Preferir reloj monotónico del dispositivo.
- Definir el instante real de inicio y final de cada respuesta.
- Separar tiempo perceptivo, decisión y animación cuando sea posible.
- Excluir anticipaciones y timeouts por reglas predefinidas.
- Usar resúmenes robustos, normalmente mediana o modelos distribucionales.
- No recompensar rapidez a costa de errores mediante una bonificación arbitraria.
- Registrar clase de dispositivo, refresh rate y modalidad de entrada cuando importe.

### 8.3 Trade-off velocidad–precisión

Cada ejercicio debe seleccionar un enfoque justificable, por ejemplo:

- Modelar precisión y RT por separado.
- Efficiency score validado para esa tarea.
- Balanced integration score con simulaciones previas.
- Modelo jerárquico que estime habilidad y velocidad.

Queda prohibida una fórmula universal improvisada para los 120 ejercicios.

### 8.4 Dificultad

Subir de nivel no prueba mejora. Para comparar actuaciones:

- Los ítems deben tener parámetros o categorías de dificultad versionadas.
- El estimador debe condicionar por dificultad.
- La adaptación debe incluir exploración suficiente y evitar quedar atrapada.
- La UI debe distinguir “nivel de reto alcanzado” de “capacidad estimada”.

### 8.5 Métricas auxiliares

Confianza, consistencia, perseveración, errores por tipo, secuencia y estrategia pueden enriquecer el feedback. Solo pasan a score si muestran valor incremental y estabilidad.

---

## 9. Scoring por ejercicio y bloque

### 9.1 Capa de estimación

El score visible en escala 0–100 es una presentación, no el dato primario. Debajo debe existir una estimación en una escala versionada y defendible.

Pipeline mínimo:

```text
eventos brutos
→ validación técnica
→ features robustas
→ ajuste por dificultad/forma/dispositivo aprobado
→ estimación por subhabilidad
→ agregación de bloque con cobertura
→ transformación visible 0–100
→ incertidumbre + frescura + versión
```

### 9.2 Fórmulas específicas

No se exige el mismo algoritmo a todos los bloques. Se exige el mismo estándar documental.

Cada bloque debe definir:

- Subhabilidades incluidas.
- Peso o modelo de agregación.
- Cobertura mínima.
- Evidencia mínima por subhabilidad.
- Manejo de correlaciones.
- Escala y transformación.
- Error de estimación.
- Invariancia por forma, idioma, accesibilidad y dispositivo.

### 9.3 Datos ausentes

- No imputar como cero.
- No penalizar un bloque no evaluado.
- Mostrar “sin evidencia suficiente”.
- Recalcular cobertura explícitamente.
- No generar un global oficial si falta uno de los ocho bloques.

### 9.4 Evidencia reciente y anterior

El estimador puede ponderar evidencia por relevancia temporal sin borrar historia. La ponderación debe:

- Evitar saltos causados por una sesión.
- No producir caída arbitraria solo por inactividad.
- Marcar la estimación como `STALE` cuando pierda actualidad.
- Permitir reactivar comparabilidad con probes controlados.

### 9.5 Regresión a la media

El sistema debe evitar vender como progreso la reversión de un baseline excepcionalmente bajo. Se usarán baselines de varias observaciones, estimadores con regularización o modelos equivalentes.

---

## 10. Performance Score y Growth Index

### 10.1 Performance Score

Responde:

> ¿Cuál es la mejor estimación actual del rendimiento del usuario en las tareas y constructos cubiertos, bajo la versión y condiciones declaradas?

Características:

- Escala visible 0–100 por bloque.
- Puede subir o bajar.
- No incorpora XP, racha, tiempo entrenado, suscripción ni estado emocional.
- Incluye estado de calibración, cobertura, frescura y calidad de evidencia.
- No se presenta con más decimales de los que soporta su error.

### 10.2 Growth Index

Responde:

> ¿Qué cambio fiable existe frente al baseline personal, considerando incertidumbre, exposición y práctica?

Características:

- Está separado del Performance Score.
- Puede ser positivo, estable, negativo o inconcluso.
- Usa intervalos o categorías de cambio fiable.
- Distingue mejora específica, near transfer y transferencia cotidiana.
- No suma puntos solo por completar sesiones.

### 10.3 Estados recomendados

```text
INSUFFICIENT_DATA
CALIBRATING
NO_RELIABLE_CHANGE
LIKELY_IMPROVEMENT
RELIABLE_IMPROVEMENT
LIKELY_DECLINE
RELIABLE_DECLINE
STALE
NON_COMPARABLE_VERSION
```

Los umbrales exactos se fijan mediante ADR científico y validación empírica.

### 10.4 Baseline, actual y mejor

La UI puede mostrar:

- Baseline calibrado.
- Estimación actual.
- Mejor estimación histórica comparable.
- Cambio fiable.

“Mejor” no reemplaza a “actual”. Se prohíbe ocultar una bajada real mostrando solo máximos históricos.

---

## 11. Calibración

### 11.1 Política mínima aprobada

Para que un bloque pueda pasar de provisional a calibrado se exige, como punto de partida:

- Al menos **5 sesiones oficiales válidas**.
- Distribuidas en al menos **3 días distintos**.
- Con al menos **48 horas** entre la primera y la última evidencia.
- Blueprint y subhabilidades requeridas cubiertas.
- Integridad y comparabilidad aprobadas.
- Ninguna dependencia crítica de una única tarea o forma.

Esta es una política de prudencia del producto, no una afirmación de suficiencia psicométrica universal.

### 11.2 Evidencia adicional adaptativa

El sistema debe pedir más evidencia cuando:

- La variabilidad es alta.
- Las tareas discrepan.
- La cobertura es desigual.
- Hay cambio de dispositivo/modalidad.
- La estimación está cerca de un umbral relevante.
- Se detectan muchos ítems conocidos.
- La calidad técnica es baja.

### 11.3 Límite de evidencia oficial diaria

El usuario puede entrenar sin límite artificial, pero solo una dosis predefinida de evidencia independiente puede actualizar el perfil oficial por día. Las repeticiones restantes son `TRAINING_ONLY`.

La dosis exacta se decide por ejercicio/bloque mediante ADR; no se fija una regla universal sin datos.

### 11.4 Estados de calibración

```text
UNASSESSED
QUICK_PROVISIONAL
CALIBRATING
CALIBRATED
STALE
RECALIBRATING
NON_COMPARABLE
```

### 11.5 Cambio de versión

Una nueva fórmula o banco no hereda automáticamente calibración. Requiere:

- Compatibilidad demostrada y transformación puente, o
- Periodo de doble scoring, o
- Nuevo baseline claramente comunicado.

---

## 12. Incertidumbre, fiabilidad y estabilidad

### 12.1 Qué debe acompañar a un score

Internamente:

- Error estándar o intervalo posterior equivalente.
- Cobertura.
- Número y diversidad de observaciones.
- Frescura.
- Calidad técnica.
- Comparabilidad de versiones.

Externamente, la UI puede simplificar a:

- “Perfil en calibración”.
- “Confianza baja/media/alta”.
- Banda o rango plausible.
- “Necesitamos una breve reevaluación”.

### 12.2 Confianza no es consistencia

El coeficiente de variación, la dispersión o una racha estable no son por sí solos “confianza estadística”. La etiqueta visible debe derivar de un modelo que considere error, cobertura y calidad.

### 12.3 Fiabilidad

Según la tarea, evaluar:

- Consistencia interna cuando tenga sentido.
- Split-half corregida.
- Test–retest.
- ICC con especificación del modelo.
- SEM y cambio mínimo detectable.
- Información por nivel de habilidad si se usa IRT/MIRT.

No existe un único corte mágico. La suficiencia depende del uso: feedback de entrenamiento requiere menos evidencia que ranking o claim de transferencia.

### 12.4 Tendencia

Una línea suavizada no equivale a tendencia fiable. El algoritmo debe:

- Incorporar error de medida.
- Evitar cherry-picking de ventanas.
- Registrar el periodo elegido.
- Detectar outliers sin borrar datos silenciosamente.
- Distinguir cambio de nivel, curva de aprendizaje y oscilación normal.

---

## 13. Práctica, exposición y bancos de ítems

### 13.1 Amenaza de práctica

Mejorar por recordar ítems, reglas o trucos es progreso en la tarea, no necesariamente en el constructo.

Controles:

- Estímulos nuevos.
- Formas paralelas.
- Bancos separados para training y measurement.
- Límites de exposición.
- Registro de historial por ítem/familia.
- Contrabalanceo cuando sea viable.
- Modelado de número de exposiciones.
- Washout o reevaluación con forma distinta.

### 13.2 Generación procedural

La generación determinista es preferible para el entrenamiento gratuito porque permite variedad sin IA. Debe registrar:

- `generatorVersion`.
- Semilla.
- Parámetros.
- Dificultad prevista.
- Solución verificable.
- Restricciones y validadores.

Una semilla reproducible facilita depuración, pero el cliente no debe poder predecir formas oficiales sensibles.

### 13.3 Medición controlada

Los ítems de medición requieren:

- Identidad o familia versionada.
- Exposición.
- Estadísticos de ítem.
- Reglas de retirada.
- Seguridad suficiente.
- Equivalencia de formas antes de mezclarlas.

---

## 14. Dispositivo, modalidad y latencia

### 14.1 Regla de comparabilidad

No asumir que una respuesta táctil en Android, ratón, teclado, audio Bluetooth o un iPhone a 120 Hz son equivalentes.

Registrar cuando sea pertinente:

- Plataforma y versión.
- Modelo/clase de dispositivo.
- Refresh rate.
- Modalidad de entrada.
- Audio route.
- Estado de rendimiento.
- Frame drops y latencia observada.
- Versión de la app y motor.

### 14.2 Estrategias permitidas

- Excluir la métrica RT de score oficial en hardware no validado.
- Crear normas o ajustes por clase de dispositivo.
- Usar métricas menos sensibles al hardware.
- Calibrar latencia local con pruebas técnicas.
- Declarar cambio de modalidad y reiniciar la comparabilidad necesaria.

### 14.3 Política fail-closed

Si la calidad temporal no puede garantizarse, la sesión puede entrenar pero no alimentar un score de velocidad oficial.

---

## 15. Accesibilidad, equidad y sesgo

Accesibilidad es parte de la validez, no un añadido visual.

Cada medición debe documentar:

- Dependencia de visión, audición, color, lectura, motricidad o idioma.
- Ajustes disponibles.
- Qué ajustes preservan comparabilidad.
- Qué ajustes crean una modalidad separada.
- Posibles sesgos culturales, educativos o socioeconómicos.
- Análisis por dispositivo y grupo cuando el tamaño lo permita.

Requisitos:

- No depender solo del color.
- Targets táctiles y contraste adecuados.
- Alternativas a audio o gestos cuando el constructo no los requiera.
- Texto comprensible para adultos con distinta alfabetización.
- Reduced motion.
- Soporte de tecnologías de asistencia cuando no invalide la tarea.
- Declaración honesta cuando una condición sensorial o motora limita la inferencia.

Se analizará DIF o método equivalente antes de afirmar comparabilidad entre grupos. Un resultado diferencial no autoriza inferir una causa.

---

## 16. Contexto: sueño, estrés, energía y fatiga

El usuario puede declarar:

- Sueño.
- Estrés.
- Energía.
- Fatiga pre-sesión.

Política aprobada:

- Permanecen locales por defecto.
- Pueden modificar la selección, duración, dificultad o confianza interpretativa.
- No suben ni bajan el Performance Score oficial.
- No se envían a analítica, backend o IA sin finalidad y consentimiento separados.
- El usuario puede ignorarlos, corregirlos y borrarlos.

Una interpretación contextual ajustada puede investigarse en privado. Solo se publica si demuestra mejora predictiva, estabilidad y ausencia de incentivos perversos.

---

## 17. Personalización y profesión

La personalización ocupacional se basa en demandas cognitivas de tareas concretas, no en estereotipos de profesión.

Flujo:

1. Recoger profesión/estudios y tareas habituales.
2. Descomponer tareas en demandas observables.
3. Mapear demandas a subhabilidades entrenables.
4. Seleccionar ejercicios validados para esas subhabilidades.
5. Ajustar dosis por objetivo, tiempo, perfil, preferencias y fatiga.
6. Medir progreso específico y transferencia por separado.

La etiqueta profesional no modifica el score ni una norma. El plan general es fallback cuando la taxonomía no cubre una ocupación.

El plan se revisa semanalmente; la dificultad micro se ajusta dentro de sesión. Rechazar un ejercicio debe activar una alternativa equivalente, no castigar adherencia.

---

## 18. Transferencia

### 18.1 Jerarquía oficial

1. Transferencia a rendimiento cotidiano: estudiar, trabajar, decidir o concentrarse.
2. Transferencia a ejercicios distintos que entrenan la misma capacidad.
3. Mayor conocimiento del propio perfil cognitivo.
4. Motivación, sensación de progreso y adherencia.
5. Creación de un hábito estable.
6. Mejora en ejercicios entrenados con estímulos nuevos.

La prioridad estratégica no equivale a facilidad de demostración. La transferencia cotidiana es el resultado más importante y uno de los más difíciles de atribuir.

### 18.2 Taxonomía de resultados

- **Task learning:** mejora en el mismo ejercicio.
- **Novel-stimulus performance:** misma mecánica, estímulos nuevos.
- **Near transfer:** tarea distinta con mecanismo cercano.
- **Far transfer:** tarea o dominio distante.
- **Everyday transfer:** cambio funcional en estudio, trabajo o vida cotidiana.

Cada resultado se reporta por separado.

### 18.3 Objetivo de 30 días

MindBoost aspira a que el usuario perciba al menos una mejora leve en una tarea cotidiana tras 30 días. Esta es una **hipótesis de producto**, no un claim validado.

Debe evaluarse mediante:

- Objetivo cotidiano definido al inicio.
- Medidas repetidas y breves.
- Tarea funcional o indicador conductual cuando sea posible.
- Auto-reporte con wording neutral, no solo satisfacción.
- Control de exposición y expectativas.
- Comparador apropiado en estudios posteriores.

### 18.4 Atribución

No atribuir causalidad a MindBoost solo por una mejora pre–post. La evidencia causal requiere un diseño comparativo adecuado, potencia, análisis preespecificado y control de pérdida de participantes.

---

## 19. Feedback y comunicación

### 19.1 Entrenamiento

El plan Free ofrece feedback determinista suficientemente detallado:

- Qué ocurrió.
- Qué patrón se observa.
- Una acción concreta para el siguiente intento.
- Qué no puede concluirse todavía.

No usa IA generativa.

### 19.2 Evaluación

El feedback se agrega para evitar coaching del ítem. Debe mostrar:

- Estimación.
- Cambio fiable o no concluyente.
- Cobertura/confianza.
- Próxima acción.
- Límites.

### 19.3 Lenguaje prohibido sin evidencia

- “Has aumentado tu inteligencia”.
- “Tu cerebro es X % más potente”.
- “Diagnóstico”.
- “Previene” o “trata” una condición.
- “Garantizado”.
- “Demostrado científicamente” sin estudio específico aplicable.
- “Mejor que el X % del mundo” sin normas representativas y comparables.

---

## 20. Score global, perfiles y rangos

### 20.1 Global provisional

Puede mostrarse un resumen provisional si:

- Expone la cobertura.
- Identifica los bloques no calibrados.
- No se usa para ranking.
- No aparenta ser equivalente al global oficial.

### 20.2 Global oficial

Solo existe cuando:

- Los ocho bloques están calibrados.
- Son comparables bajo versiones vigentes.
- Existe cobertura mínima por subhabilidad.
- La agregación ha sido aprobada científicamente.
- Se comunica la incertidumbre.

### 20.3 Rangos

- Durante alpha y beta son provisionales.
- No hay ranking público oficial durante beta.
- Solo pueden usar rendimiento calibrado e íntegro.
- No incorporan gasto, frecuencia, XP o suscripción.
- El rango `World Class` queda bloqueado hasta disponer de normas representativas, tamaño suficiente, calidad, invariancia y prevención de manipulación.

### 20.4 Privacidad del perfil

El perfil cognitivo es privado por defecto. Compartir requiere acción explícita, granular y revocable.

---

## 21. Versionado y linaje

Todo resultado oficial debe poder reconstruirse con:

- `exerciseId` y `exerciseVersion`.
- `constructModelVersion`.
- `metricFormulaVersion`.
- `scoringModelVersion`.
- `itemBankVersion` o `generatorVersion`.
- `normVersion`, si existe.
- `appVersion` y `engineVersion`.
- Flags experimentales.
- Estado de integridad.

### 21.1 Tipos de cambio

- **Patch:** corrige un bug sin alterar significado esperado; verificar equivalencia.
- **Minor:** mejora manteniendo puente de comparabilidad.
- **Major:** cambia constructo, escala o interpretación; requiere nueva serie o transformación validada.

### 21.2 Migraciones

No reescribir silenciosamente el historial. Conservar:

- Valor calculado originalmente.
- Versión original.
- Valor recalculado, si se produce.
- Motivo y fecha.
- Compatibilidad declarada.

---

## 22. Ciclo de evidencia

Cada mecanismo, ejercicio, fórmula y claim pasa por:

```text
RATIONALE
→ IMPLEMENTED
→ TECHNICALLY_VERIFIED
→ INTERNALLY_PLAUSIBLE
→ HUMAN_PILOTED
→ RELIABILITY_SUPPORTED
→ VALIDITY_SUPPORTED
→ TRANSFER_SUPPORTED
→ REPLICATED / MONITORED
```

No todos alcanzarán el último estado. La UI y comunicación solo usan el claim permitido por el estado actual.

### 22.1 Etiquetas de evidencia

- `KNOWN`: hecho directamente verificado.
- `SUPPORTED`: respaldado por evidencia aplicable.
- `INFERRED`: deducción razonable, no observación directa.
- `HYPOTHESIS`: propuesta por validar.
- `UNKNOWN`: falta evidencia.
- `CONFLICTED`: evidencia relevante contradictoria.

---

## 23. Jerarquía de fuentes

Orden preferido:

1. Revisiones sistemáticas y metaanálisis aplicables y de calidad.
2. Estudios preregistrados, con potencia suficiente y resultados relevantes.
3. Estudios psicométricos del paradigma o instrumento.
4. Guías técnicas y estándares oficiales.
5. Estudios observacionales y pilotos.
6. Razonamiento mecanístico.
7. Opinión experta.

Evaluar siempre:

- Población.
- Comparador.
- Outcome.
- Dosis.
- Riesgo de sesgo.
- Replicación.
- Generalización a móvil y a MindBoost.
- Magnitud e incertidumbre, no solo significación.

La cita de un estudio sobre un paradigma no valida automáticamente la implementación de MindBoost.

---

## 24. ADR científicos obligatorios

Antes de producción deben existir decisiones versionadas para:

1. Modelo de cada uno de los ocho bloques.
2. Definición de subhabilidades.
3. Fórmulas velocidad–precisión por familia.
4. Tratamiento de dificultad.
5. Regla de exposición y evidencia diaria.
6. Modelo de calibración e incertidumbre.
7. Criterio de cambio fiable.
8. Construcción del global.
9. Normas y rangos.
10. Puentes entre dispositivo/modalidad.
11. Versionado/migraciones.
12. Medición de transferencia a 30 y 90 días.

Un ADR debe contener problema, opciones, evidencia, decisión, supuestos, simulaciones, riesgos, reversibilidad y plan de validación.

---

## 25. Gates científicos

### 25.1 Gate de ejercicio para entrenamiento

- Constructo y mecanismo definidos.
- Solución correcta y determinista.
- Dificultad controlable.
- Métricas técnicamente válidas.
- Feedback no engañoso.
- Accesibilidad mínima.
- Tests y telemetría.

### 25.2 Gate para evidencia provisional

- Blueprint definido.
- Integridad implementada.
- Versionado y exposición.
- Datos humanos iniciales o justificación conservadora.
- UI marcada como provisional.

### 25.3 Gate para score oficial

- Fiabilidad suficiente para el uso.
- Error cuantificado.
- Comparabilidad demostrada.
- Práctica y dificultad controladas.
- Sesgo/device effects analizados.
- Algoritmo congelado/versionado.

### 25.4 Gate para claim de transferencia

- Outcome independiente y preespecificado.
- Comparador adecuado.
- Potencia suficiente.
- Manejo transparente de attrition y multiplicidad.
- Magnitud útil e incertidumbre.
- Replicación o wording limitado al estudio exacto.

---

## 26. Datos que alpha y beta deben producir

Alpha privada:

- Comprensión de reglas.
- Bugs y datos imposibles.
- Distribuciones de métricas.
- Techo/suelo.
- Duraciones y abandono.
- Latencia y device effects gruesos.
- Accesibilidad y wording.

Beta abierta:

- Parámetros de ítem.
- Fiabilidad y estabilidad.
- Curvas de práctica.
- Equivalencia de formas.
- Sensibilidad al cambio.
- Sesgo por dispositivo y subgrupos con tamaño adecuado.
- Relación con resultados funcionales.
- Retención sin incentivos engañosos.

Alpha con unas 20 personas no puede validar toda la psicometría ni transferencia causal.

---

## 27. Implementación segura de una fórmula nueva

Antes de sustituir una fórmula:

1. Redactar hipótesis y ADR.
2. Crear dataset sintético con casos límite.
3. Simular estrategias: azar, velocidad imprudente, lentitud perfecta, abandono, repetición.
4. Definir propiedades invariantes.
5. Añadir tests input → output y property-based.
6. Ejecutar doble scoring sin afectar al usuario.
7. Analizar estabilidad, sesgo y comportamiento longitudinal.
8. Aprobar gate científico y técnico.
9. Versionar y migrar sin borrar historia.
10. Monitorizar después del despliegue.

---

## 28. Definition of Done científica

Una función de medición está terminada solo si:

- Declara constructo, uso y no-uso.
- Tiene eventos, filtros y fórmula versionados.
- Controla dificultad, práctica y dispositivo en proporción al claim.
- Cuantifica cobertura e incertidumbre.
- Se prueba con casos adversarios y datos reales suficientes para su estado.
- La UI no exagera la interpretación.
- La documentación y el Decision Log están actualizados.
- No quedan P0/P1.
- Toda mejora segura, relevante y dentro del alcance de la fase está implementada; las demás quedan registradas con owner/gate.

---

## 29. Referencias normativas y científicas de partida

Estas fuentes orientan el trabajo; no constituyen validación de MindBoost:

- NIST, *Digital Identity Guidelines — Authentication and Lifecycle Management*: https://pages.nist.gov/800-63-4/sp800-63b.html
- OWASP, *Authentication Cheat Sheet*: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html
- Reglamento General de Protección de Datos, texto oficial: https://eur-lex.europa.eu/eli/reg/2016/679/oj
- EDPB, guidelines y recomendaciones: https://www.edpb.europa.eu/our-work-tools/general-guidance/guidelines-recommendations-best-practices_en
- Goldberg et al., práctica en evaluación cognitiva repetida: https://pubmed.ncbi.nlm.nih.gov/27239497/
- Nicosia et al., fiabilidad de tareas cognitivas digitales (reliability): https://pubmed.ncbi.nlm.nih.gov/36062528/
- Nicosia et al., latencia en dispositivos BYOD en evaluación cognitiva digital (2022): https://pubmed.ncbi.nlm.nih.gov/35953659/
- Passell et al., elección de dispositivo y factores técnicos en evaluación cognitiva digital: https://pubmed.ncbi.nlm.nih.gov/33954913/
- Gavett et al., cambio fiable y efectos de práctica: https://pubmed.ncbi.nlm.nih.gov/26234918/

> **Corrección 2026-07-11 (6R3-P2-05).** Antes, esta lista atribuía el PMID **35953659 a "Passell et al."**. Es
> incorrecto (verificado en PubMed): **35953659 = Nicosia et al. (latencia BYOD, 2022)**; **36062528 = Nicosia
> et al. (fiabilidad)**; **33954913 = Passell et al. (elección de dispositivo)**. Se corrige la atribución y se
> añade la referencia Passell (33954913) que faltaba. Nota: el KDoc de `MeasurementLineage.kt` (código) ya usa
> los PMIDs correctos (Passell 33954913, Nicosia 35953659) — único matiz: 35953659 es de **2022**, no 2023.
> Registrado en `09_DECISION_LOG.md` (S6R3-006) y en
> `docs/mindboost_velocidad_implementation_plan/SYSTEM1_SLICE_6R3_REOPEN.md` §5/§7.

Toda fuente nueva debe registrarse con alcance, calidad, población, limitaciones y decisión afectada.

---

## 30. Regla final

MindBoost puede ser ambicioso y prudente a la vez.

La victoria no consiste en que todos los números suban. Consiste en que el usuario pueda confiar en que:

- Los resultados significan lo que dicen.
- La incertidumbre no está escondida.
- La práctica no se vende como transferencia.
- Las recomendaciones responden a evidencia real.
- Los claims mejoran a medida que mejora la ciencia.
