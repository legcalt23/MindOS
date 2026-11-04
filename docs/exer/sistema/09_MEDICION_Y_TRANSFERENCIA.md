# Medición, cambio y transferencia

## 1. Dos carriles y varias inferencias

Entrenamiento puede adaptar, enseñar y variar. Medición necesita condiciones controladas, exposición, formas/versiones, integridad, comparabilidad e incertidumbre. No todos los ejercicios deben medir oficialmente. Una familia de entrenamiento puede necesitar otra familia para evaluar su resultado.

| Resultado observado | Qué puede describir | Qué no demuestra por sí solo |
|---|---|---|
| Mejor ejecución en ítems repetidos | Aprendizaje específico bajo condiciones conocidas | Mejora de capacidad general |
| Mejor ejecución con estímulos nuevos en la misma regla | Generalización a nuevos ítems | Transferencia a tareas distintas |
| Mejora en tareas diferentes del constructo | Evidencia candidata de transferencia cercana | Causalidad o efecto cotidiano sin diseño adecuado |
| Mejora funcional independiente fuera de la tarea | Cambio cotidiano bajo condiciones del estudio | Atribución a Molvex sin comparador y control de sesgos |
| Retención posterior | Persistencia del resultado medido | Durabilidad universal o en otras poblaciones |

Separar estimandos poblacionales de decisiones individuales. Una media de grupo no identifica qué persona respondió causalmente, y una buena predicción individual no demuestra efecto de intervención.

## 2. Contrato de métrica

Para cada métrica: fenómeno observado, unidad, entradas, denominador, tipo de ensayo elegible, dirección, filtros predefinidos, tratamiento de errores/omisiones/timeout/anticipaciones, datos faltantes, agregación, versión, incertidumbre, efecto de dificultad, dispositivo, modalidad, exposición y usos permitidos.

En tareas con objetivos raros, descomponer aciertos, omisiones, falsas alarmas y rechazos correctos. Si procede teoría de detección de señales, definir supuestos y tratamiento de tasas extremas; no imponerla por prestigio. RT requiere precisar inicio y fin observados, no asumir instante de presentación físico.

Mantener precisión y tiempo separados cuando no exista una combinación validada para la finalidad. Probar perfiles rápido/preciso, rápido/impreciso, lento/preciso, lento/impreciso, conservador, impulsivo, aleatorio y anticipador. No forzar ordenaciones totales si el objetivo no define el intercambio. Evitar `accuracy × speed` y ratios sin justificación.

Para sostenimiento, considerar distribución y estructura temporal cuando aporten señal: mediana/dispersiones, colas, lapsos, clusters, pendiente por tiempo o recuperación. No convertir cada feature correlacionada en evidencia independiente ni usar coeficiente de variación como confianza estadística del perfil.

## 3. Aprendizaje, estado y error

Analizar: señal objetivo, demandas secundarias, percepción, motor, dispositivo, estrategia, práctica de regla/ítem, estado temporal y ruido. No se exige estimar cada componente sin datos; sí identificar qué puede explicar el cambio y cómo se comprobaría.

Diseñar ataques de falso progreso: baseline accidentalmente bajo, regresión a la media, forma más fácil, cambio de dispositivo, mezcla de ejercicios, familiaridad, descanso, sesión excepcional, duplicación, exposición ilimitada y cambio de versión. Un valor 0–100 es presentación, no una medida primaria de capacidad.

## 4. Estimación y cambio

Performance Score representa nivel según un estimador/versiones defendibles; Growth Index representa evolución con error, comparabilidad y control de práctica. No fijar una fórmula porque sea intuitiva. Mantener bloqueados los ADR abiertos que afectan el uso.

No usar última sesión como perfil, first–last como progreso fiable, CV como confianza final, missing como cero, inactividad como pérdida artificial o máximo histórico para ocultar descensos. XP, racha, pago y volumen no son capacidad. La actividad puede tener su propio registro sin entrar en inferencia cognitiva.

Evaluar fiabilidad apropiada al uso: consistencia entre ensayos/formas, test–retest, error condicional, SEM y sensibilidad al cambio. La fiabilidad entre personas no resuelve sola la precisión intraindividual. No trasladar un cutoff universal o estimar cambio fiable sin modelo y condiciones que lo permitan.

## 5. Calibración y elegibilidad

El mínimo documental es **5 sesiones oficiales válidas, 3 días distintos y ≥48 h entre primera y última**, además de cobertura del blueprint, integridad y comparabilidad, sin dependencia crítica de una única tarea. Es una política prudencial, no suficiencia empírica automática. Datos adicionales pueden ser necesarios. No especificar UTC o política temporal alternativa sin resolución vigente.

Mantener separados lifecycle de la versión, finalidad/estado de sesión, elegibilidad de perfil y ranking. Una tarea válida puede producir una sesión inválida. Una sesión completa `TRAINING_FREE` se describe como `TRAINING_ONLY` en el contrato facts-only reportado; verificar la política viva antes de traducir tipos.

Probes embebidos requieren transparencia y control de selección/exposición. No evaluar solo en días favorables ni cuando la persona consigue una buena práctica. Condiciones alteradas por ayuda, feedback, accesibilidad o interrupción se etiquetan; no heredan comparabilidad.

## 6. Evaluación independiente y contaminación

Separar tutorial, entrenamiento, evaluación de desarrollo y evaluación confirmatoria reservada. Registrar exposición a estímulos, familia, regla y estrategia. Evitar usar la prueba primaria para ajustar continuamente el entrenamiento y después presentarla como evaluación independiente. Nuevas versiones del diseño necesitan un plan de control de filtración de evaluación.

La «distancia» de transferencia debe justificarse por operaciones, materiales, respuesta, reglas y contexto, no por un nombre nuevo. Un entorno laboral decorativo sigue siendo la misma tarea si exige el mismo atajo.

## 7. Claims y UX

Con datos de práctica, informar desempeño específico y condiciones. Con evidencia de cambio fiable, delimitar medida, periodo y población. Con evidencia causal, citar protocolo/comparador/outcome; no extrapolar a CI, diagnóstico o rendimiento laboral universal. VEPR-30 es un objetivo/protocolo por concretar, no un sello de eficacia.

No activar global oficial, ranking o World Class desde /exer. El catálogo de diseño puede proponer cómo obtener evidencia futura, sin levantar gates de los documentos científicos o del motor.
