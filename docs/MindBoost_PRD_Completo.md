# MindBoost — Product Requirements Document (PRD)

**Versión:** 1.0  
**Fecha:** 1 de julio de 2026  
**Producto:** MindBoost  
**Tagline:** Tu mente, tu universo.  
**Tipo de producto:** Entrenador cognitivo serio, adaptativo, medible y personalizado.  
**Plataformas MVP:** Android + iOS  
**Stack actual:** Kotlin Multiplatform, Compose Multiplatform, Firebase / Firestore  
**Modelo de negocio:** Freemium + MindBoost Elite a 2 €/mes  
**Fecha objetivo MVP:** 25 de julio de 2026  
**Owner:** New Era UNIVERSE  

---

## Índice

1. [Resumen ejecutivo](#1-resumen-ejecutivo)
2. [Fuente de verdad CEO](#2-fuente-de-verdad-ceo)
3. [Identidad estratégica](#3-identidad-estratégica)
4. [Problema y oportunidad](#4-problema-y-oportunidad)
5. [Usuario objetivo](#5-usuario-objetivo)
6. [Posicionamiento y diferenciación](#6-posicionamiento-y-diferenciación)
7. [Principios de producto](#7-principios-de-producto)
8. [Alcance MVP](#8-alcance-mvp)
9. [Fuera de alcance MVP](#9-fuera-de-alcance-mvp)
10. [Los 8 motores cognitivos](#10-los-8-motores-cognitivos)
11. [Sistema de ejercicios](#11-sistema-de-ejercicios)
12. [Onboarding y evaluación inicial](#12-onboarding-y-evaluación-inicial)
13. [Sistema de entrenamiento](#13-sistema-de-entrenamiento)
14. [Scoring, calibración y rangos](#14-scoring-calibración-y-rangos)
15. [Experiencia de usuario](#15-experiencia-de-usuario)
16. [MindBoost Elite](#16-mindboost-elite)
17. [IA Coach Elite](#17-ia-coach-elite)
18. [Competición, ranking y comunidad](#18-competición-ranking-y-comunidad)
19. [Privacidad, datos y cumplimiento](#19-privacidad-datos-y-cumplimiento)
20. [Arquitectura técnica](#20-arquitectura-técnica)
21. [Analytics y métricas internas](#21-analytics-y-métricas-internas)
22. [Requisitos no funcionales](#22-requisitos-no-funcionales)
23. [Criterios de aceptación MVP](#23-criterios-de-aceptación-mvp)
24. [Bugs bloqueantes](#24-bugs-bloqueantes)
25. [Roadmap](#25-roadmap)
26. [Riesgos y mitigaciones](#26-riesgos-y-mitigaciones)
27. [Claims, ciencia y comunicación](#27-claims-ciencia-y-comunicación)
28. [AGENTS.md — Base científica y operativa](#28-agentsmd--base-científica-y-operativa)
29. [Backlog inicial por épicas](#29-backlog-inicial-por-épicas)
30. [Glosario](#30-glosario)
31. [Referencias](#31-referencias)

---

# 1. Resumen ejecutivo

MindBoost es una app de desarrollo cognitivo serio diseñada para entrenar, medir y maximizar el progreso cognitivo del usuario promedio mediante sesiones adaptativas, métricas detalladas, rangos, planes personalizados y una capa Elite con IA Coach.

MindBoost no debe percibirse como una app de juegos cerebrales infantiles. Su categoría deseada es: **entrenador cognitivo de alto rendimiento**.

La app se organiza en ocho motores cognitivos principales:

1. Enfoque.
2. Decisiones.
3. Lógica.
4. Creatividad.
5. Lenguaje.
6. Emociones.
7. Velocidad mental.
8. Memoria.

Cada motor tendrá ejercicios propios, score 0–100, progresión, rango independiente y contribución al score global del usuario.

El MVP debe lanzar con una experiencia que demuestre la esencia del producto:

> Esto no es un juego. Esto es mi entrenamiento mental.

---

# 2. Fuente de verdad CEO

Esta sección gobierna todas las decisiones de producto, diseño, desarrollo, IA, scoring, monetización, privacidad y roadmap.

## 2.1 Regla número uno

**Maximizar el progreso cognitivo medible del usuario con seriedad, precisión y honestidad.**

MindBoost no sacrifica:

- Precisión por espectáculo.
- Privacidad por datos innecesarios.
- Ciencia por marketing.
- Entrenamiento real por gamificación vacía.
- Confianza por conversión.
- Calidad por cantidad.
- Seriedad por viralidad.

MindBoost debe convertirse en el entrenador cognitivo que dice la verdad, mide bien, entrena fuerte y ayuda al usuario a construir una mente más capaz.

## 2.2 Decisión ejecutiva final

MindBoost no compite por ser una app bonita de ejercicios mentales. Compite por convertirse en el sistema de referencia para entrenar capacidades cognitivas de forma personalizada, seria y medible.

La prioridad absoluta del producto es:

1. Entrenamiento real.
2. Medición clara.
3. Adaptación inteligente.
4. Progreso demostrable.
5. Confianza.
6. Elite como capa de optimización avanzada.

## 2.3 Principio de secuencia

**Primero construir el motor cognitivo. Después construir el estadio competitivo.**

PvP, comunidad, torneos y competición avanzada son importantes, pero no pueden comprometer el núcleo: entrenamiento, scoring, calibración, privacidad y estabilidad.

## 2.4 Principio de scoring

Competir con métricas malas destruye confianza. Primero medir bien, después competir.

## 2.5 Principio de IA

La IA optimiza entrenamiento; no diagnostica, no exagera y no sustituye criterio profesional.

## 2.6 Principio de privacidad

MindBoost debe conocer el rendimiento cognitivo del usuario, no invadir su identidad personal.

---

# 3. Identidad estratégica

## 3.1 Misión

MindBoost entrena lo máximo posible la capacidad cognitiva del usuario promedio y demuestra su progreso de la manera más precisa, clara y motivadora posible.

## 3.2 Visión

Convertir MindBoost en el entrenador cognitivo serio de referencia mundial para usuarios que quieren desarrollar su mente con estructura, medición y personalización.

## 3.3 Frase de posicionamiento

MindBoost es el entrenador cognitivo serio para personas que quieren desarrollar su mente con estructura, métricas y progresión real.

## 3.4 Promesa principal

Ayudar al usuario a entrenar sus capacidades cognitivas mediante ejercicios adaptativos, medición continua y planes personalizados que reflejen su progreso de forma clara.

## 3.5 Transformación buscada

Después de usar MindBoost, el usuario debe sentir:

- Que entiende mejor su perfil cognitivo.
- Que sabe qué entrenar.
- Que puede medir su progreso.
- Que tiene una rutina mental seria.
- Que su mente no es algo abstracto, sino un sistema entrenable.

## 3.6 Personalidad de marca

MindBoost debe sentirse:

- Científico.
- Motivador.
- Calmado.
- De alto rendimiento.
- Premium.
- Cósmico.
- Preciso.

MindBoost no debe sentirse:

- Infantil.
- Casual sin profundidad.
- Ruidoso.
- Manipulador.
- Pseudocientífico.
- Como un videojuego superficial.

---

# 4. Problema y oportunidad

## 4.1 Problema principal

La mayoría de personas desaprovecha gran parte de su capacidad mental porque no entrena sus habilidades cognitivas con estructura, constancia ni métricas fiables.

## 4.2 Problemas secundarios

- Falta de conciencia sobre el propio perfil cognitivo.
- Falta de estructura para entrenar habilidades mentales.
- Apps existentes percibidas como juegos infantiles.
- Métricas poco claras o poco accionables.
- Poca personalización real.
- Falta de sistemas competitivos serios basados en capacidad cognitiva.
- Claims de mercado que pueden sonar exagerados o poco creíbles.

## 4.3 Oportunidad

Crear una plataforma que combine:

- Entrenamiento cognitivo estructurado.
- Sistema adaptativo.
- Métricas comprensibles.
- Rangos competitivos.
- IA Coach científica.
- Experiencia visual premium.
- Comunicación honesta.

---

# 5. Usuario objetivo

## 5.1 Usuario principal

Personas de 18 a 35 años que sienten que tienen potencial mental no aprovechado y quieren desarrollar inteligencia práctica, concentración, memoria, velocidad mental, lógica, toma de decisiones o productividad.

## 5.2 Usuario ideal de lanzamiento

Usuarios inteligentes, ambiciosos y motivados que quieren aprovechar al máximo su mente.

## 5.3 Segmentos prioritarios

| Segmento | Necesidad principal |
|---|---|
| Estudiantes exigentes | Mejorar enfoque, memoria, velocidad y lógica. |
| Jóvenes profesionales | Optimizar rendimiento diario y toma de decisiones. |
| Emprendedores | Mantener claridad, velocidad mental y disciplina. |
| Programadores/creadores | Mejorar lógica, creatividad, enfoque y memoria de trabajo. |
| Usuarios de apps cognitivas | Encontrar una alternativa más seria que Peak, Lumosity o similares. |
| Competitivos mentales | Medirse por rango, score y ranking. |

## 5.4 Jobs to be Done

1. Cuando siento que estoy desaprovechando mi mente, quiero un sistema que me diga qué entrenar y cómo mejorar.
2. Cuando entreno mi mente, quiero ver progreso real, no solo puntos vacíos.
3. Cuando compito, quiero que el ranking represente habilidad cognitiva aproximada, no solo tiempo disponible.
4. Cuando pago Elite, quiero análisis avanzado y dirección personalizada.
5. Cuando termino una sesión, quiero entender qué hice bien, qué hice mal y qué entrenar después.

---

# 6. Posicionamiento y diferenciación

## 6.1 Categoría

MindBoost pertenece a la categoría de **desarrollo cognitivo serio / cognitive performance training**.

No es:

- Una app médica.
- Una app terapéutica.
- Una app de diagnóstico.
- Un juego casual.
- Un clon de Lumosity.

## 6.2 Competidores de referencia

- Peak.
- BrainHQ.
- Lumosity.
- Elevate.
- Apps de productividad.
- Apps de gamificación de hábitos.

## 6.3 Diferenciadores principales

| Diferenciador | Descripción |
|---|---|
| Seriedad | Experiencia de alto rendimiento, no infantil. |
| 8 motores cognitivos | Modelo claro de capacidades entrenables. |
| Métricas | Scores, rangos, progreso, calibración y análisis. |
| Personalización | Plan adaptativo por usuario. |
| IA Coach | Mentor científico para usuarios Elite. |
| Competición futura | PvP, rankings, comunidad y anti-trampa. |
| Privacidad | Datos mínimos, sin DNI en MVP. |
| Honestidad | Claims prudentes, no promesas médicas ni garantizadas. |

## 6.4 Frase comercial permitida

> Entrena tus capacidades cognitivas con un sistema serio, adaptativo y medible.

## 6.5 Frases a evitar

- “Aumenta tu IQ garantizado.”
- “Cura TDAH.”
- “Previene Alzheimer.”
- “Mejora tu inteligencia de forma demostrada clínicamente” si no existe estudio propio.
- “El mejor científicamente probado” sin validación externa.

---

# 7. Principios de producto

## 7.1 Seriedad antes que entretenimiento

La app puede ser motivadora, pero nunca debe parecer una colección de juegos infantiles.

## 7.2 Progreso medible

Toda experiencia importante debe producir datos útiles: precisión, velocidad, consistencia, dificultad, errores, tiempo de reacción, evolución y estabilidad.

## 7.3 Personalización real

La app debe acercarse progresivamente al perfil cognitivo del usuario. Más datos deben producir mejor adaptación.

## 7.4 Prudencia científica

MindBoost debe evitar claims médicos, diagnósticos o promesas absolutas. Debe diferenciar entre:

- Mejora dentro de la app.
- Mejora en tareas similares.
- Posible transferencia a la vida diaria.
- Evidencia todavía en validación.

## 7.5 Privacidad máxima

Los datos personales deben limitarse a lo necesario para entrenamiento, personalización, seguridad, suscripción y cumplimiento.

## 7.6 Calidad sobre cantidad

38 ejercicios excelentes son mejores que 120 ejercicios mediocres.

## 7.7 Claridad sobre complejidad

El usuario debe entender qué está entrenando, cómo se mide y qué debe hacer después.

---

# 8. Alcance MVP

## 8.1 Objetivo del MVP

El MVP del 25 de julio de 2026 debe demostrar que MindBoost ya es un entrenador cognitivo serio, estable y útil.

## 8.2 Obligatorio para MVP

| Área | Requisito |
|---|---|
| Cuenta | Registro obligatorio. No se puede usar sin cuenta. |
| Plataformas | Android + iOS. |
| Onboarding | Personalizado, serio y completo. |
| Evaluación inicial | 6–10 minutos máximo, cubriendo los 8 bloques. |
| Bloques | Los 8 motores cognitivos entrenables. |
| Ejercicios | Mínimo aproximado de 38 ejercicios sólidos. |
| Sesiones | 120s, 5min, 10min, 20min, 30min. |
| Modo libre | El usuario puede elegir bloque y ejercicios. |
| Plan adaptativo | Recomendación básica de entrenamiento. |
| Score | 0–100 por bloque. |
| Score global | Media simple de los 8 bloques. |
| Rangos | Rango por bloque y global. |
| Calibración | 5 sesiones válidas por bloque para rango oficial. |
| Métricas | Post-sesión claras y accionables. |
| Perfil | Perfil cognitivo del usuario. |
| Historial | Progreso básico por bloque. |
| Elite | Suscripción de 2 €/mes. |
| IA Coach | Funcional para Elite. |
| Ads | Ningún anuncio. |
| Firestore | Persistencia cloud. |
| Local | Persistencia/caché en dispositivo. |
| Privacidad | Sin DNI, compatible con GDPR desde el inicio. |
| Performance | Rápida, estable y sin fallos críticos. |

## 8.3 Criterio de éxito del MVP

MindBoost funciona si el usuario puede:

1. Registrarse.
2. Completar onboarding.
3. Completar evaluación inicial.
4. Entrenar cualquiera de los 8 bloques.
5. Recibir score y feedback.
6. Entender su progreso.
7. Desbloquear rangos tras calibración.
8. Usar IA Coach si es Elite.
9. Volver a entrenar sin fricción.

---

# 9. Fuera de alcance MVP

Estas funciones quedan fuera del MVP salvo que no comprometan calidad, estabilidad ni scoring:

1. PvP tiempo real completo.
2. 2v2 y 3v3.
3. Comunidad completa.
4. Torneos.
5. Temporadas competitivas.
6. Anti-trampa avanzado.
7. Rankings complejos.
8. Clanes/equipos.
9. Informes científicos largos.
10. Programas Elite extremadamente avanzados.
11. Ejercicios generados por IA.
12. Comparativas por edad o país.
13. Dashboard interno complejo.
14. Exportación avanzada de datos.
15. Sistema de referidos.
16. Claims científicos fuertes.
17. Diagnóstico médico o psicológico.

---

# 10. Los 8 motores cognitivos

## 10.1 Enfoque

Entrena atención sostenida, atención selectiva, resistencia a distracciones y control atencional.

**Subhabilidades:**

- Atención sostenida.
- Atención selectiva.
- Control distractor.
- Concentración profunda.
- Estabilidad bajo fatiga.

**Métricas potenciales:**

- Precisión.
- Tiempo de reacción.
- Errores por distracción.
- Estabilidad durante la sesión.
- Caída de rendimiento bajo fatiga.
- Consistencia entre sesiones.

## 10.2 Decisiones

Entrena juicio rápido, control de impulsos, evaluación de opciones y toma de decisiones bajo presión.

**Subhabilidades:**

- Calidad de decisión.
- Velocidad controlada.
- Riesgo/recompensa.
- Inhibición de impulsos.
- Priorización.

**Métricas potenciales:**

- Tiempo hasta decisión.
- Precisión de decisión.
- Errores por impulsividad.
- Calidad de elección bajo tiempo limitado.
- Mejora en patrones de decisión.

## 10.3 Lógica

Entrena razonamiento, inferencia, patrones, secuencias, deducción y resolución de problemas.

**Subhabilidades:**

- Patrones.
- Series.
- Inferencia.
- Deducción.
- Razonamiento abstracto.
- Resolución de problemas.

**Métricas potenciales:**

- Aciertos.
- Complejidad alcanzada.
- Tiempo por problema.
- Errores conceptuales.
- Razonamiento bajo presión.

## 10.4 Creatividad

Entrena fluidez, asociación, generación de alternativas, flexibilidad cognitiva y pensamiento divergente.

**Subhabilidades:**

- Fluidez ideacional.
- Asociación remota.
- Flexibilidad cognitiva.
- Originalidad aproximada.
- Pensamiento divergente.

**Métricas potenciales:**

- Número de respuestas válidas.
- Originalidad aproximada.
- Variedad semántica.
- Flexibilidad entre categorías.
- Velocidad de generación.

## 10.5 Lenguaje

Entrena comprensión, vocabulario, asociación verbal, velocidad verbal y precisión lingüística.

**Subhabilidades:**

- Comprensión.
- Vocabulario.
- Rapidez verbal.
- Memoria semántica.
- Asociación verbal.

**Métricas potenciales:**

- Precisión semántica.
- Tiempo de respuesta.
- Vocabulario activo.
- Comprensión contextual.
- Errores lingüísticos.

## 10.6 Emociones

Entrena autoconciencia, regulación, interpretación emocional, control de reactividad y toma de perspectiva.

**Subhabilidades:**

- Reconocimiento emocional.
- Regulación no clínica.
- Autoconciencia.
- Perspectiva.
- Control de reactividad.

**Métricas potenciales:**

- Reconocimiento emocional.
- Consistencia en respuestas.
- Capacidad de regulación declarada.
- Identificación de patrones.
- Decisión emocional bajo contexto.

**Límite:** este bloque no debe parecer terapia ni diagnóstico psicológico.

## 10.7 Velocidad mental

Entrena procesamiento rápido, agilidad cognitiva, reacción y cambio veloz entre estímulos.

**Subhabilidades:**

- Procesamiento rápido.
- Cambio de regla.
- Reacción cognitiva.
- Velocidad con precisión.
- Agilidad mental.

**Métricas potenciales:**

- Tiempo de reacción.
- Respuestas por minuto.
- Precisión bajo velocidad.
- Penalización por errores rápidos.
- Estabilidad a alta intensidad.

**Regla:** velocidad sin precisión no debe ser premiada.

## 10.8 Memoria

Entrena memoria de trabajo, retención, recuperación, memoria secuencial y manipulación activa de información.

**Subhabilidades:**

- Memoria de trabajo.
- Memoria visual.
- Memoria verbal.
- Memoria secuencial.
- Recuerdo diferido.
- Manipulación activa.

**Métricas potenciales:**

- Longitud máxima retenida.
- Precisión de recuperación.
- Tiempo de recuperación.
- Errores de orden.
- Retención tras interferencia.

---

# 11. Sistema de ejercicios

## 11.1 Meta de ejercicios

Objetivo futuro: aproximadamente 15 ejercicios por bloque.  
Objetivo MVP: mínimo aproximado de 38 ejercicios excelentes.

| Bloque | Mínimo MVP recomendado |
|---|---:|
| Enfoque | 5 ejercicios sólidos |
| Decisiones | 5 ejercicios sólidos |
| Lógica | 5 ejercicios sólidos |
| Creatividad | 4 ejercicios sólidos |
| Lenguaje | 5 ejercicios sólidos |
| Emociones | 4 ejercicios sólidos |
| Velocidad mental | 5 ejercicios sólidos |
| Memoria | 5 ejercicios sólidos |

## 11.2 Criterios de aceptación de un ejercicio

Un ejercicio solo entra en MVP si:

1. Entrena una habilidad concreta.
2. Tiene objetivo cognitivo definido.
3. Registra métricas útiles.
4. Tiene dificultad escalable.
5. Tiene feedback claro.
6. Impacta correctamente el score.
7. Puede completarse sin errores críticos.
8. No parece infantil ni arbitrario.
9. Puede versionarse.
10. Puede analizarse históricamente.

## 11.3 Plantilla estándar de ejercicio

Cada ejercicio debe documentarse así:

| Campo | Descripción |
|---|---|
| Nombre | Nombre claro y serio del ejercicio. |
| Bloque cognitivo | Uno de los 8 motores. |
| Habilidad concreta | Subcapacidad entrenada. |
| Objetivo científico | Qué proceso cognitivo pretende entrenar. |
| Duración | 120s, 5min, 10min, 20min o 30min. |
| Dificultad mínima | Nivel de entrada. |
| Dificultad máxima | Nivel máximo alcanzable. |
| Métricas capturadas | Datos registrados. |
| Fórmula de score | Cómo impacta en el bloque. |
| Criterio de progresión | Cuándo sube la dificultad. |
| Criterio de fallo | Cuándo baja dificultad o cambia ejercicio. |
| Feedback inmediato | Qué ve el usuario tras terminar. |
| Feedback histórico | Cómo se refleja en evolución. |
| Versión | Versión del ejercicio. |
| Estado | Construido, parcial, diseñado, faltante o rehacer. |

## 11.4 Inventario requerido

Cada ejercicio existente debe clasificarse:

| Bloque | Ejercicio | Estado | Calidad | Métricas listas | Apto MVP |
|---|---|---|---|---|---|
| TBD | TBD | Construido/Parcial/Diseñado/Faltante/Rehacer | Alta/Media/Baja | Sí/No | Sí/No |

---

# 12. Onboarding y evaluación inicial

## 12.1 Objetivo del onboarding

Crear una experiencia inicial seria, personalizada y memorable que prepare al usuario para entrenar su mente.

## 12.2 Flujo recomendado

1. Bienvenida: “Tu mente, tu universo.”
2. Explicación breve de MindBoost.
3. Selección de objetivo principal.
4. Selección de bloques prioritarios.
5. Preguntas sobre hábitos básicos.
6. Preguntas sobre tiempo disponible diario.
7. Evaluación inicial.
8. Resultado provisional.
9. Creación del primer plan.
10. Primera sesión recomendada.

## 12.3 Objetivos disponibles

- Mejorar enfoque.
- Mejorar memoria.
- Pensar más rápido.
- Mejorar lógica.
- Mejorar lenguaje.
- Mejorar creatividad.
- Mejorar toma de decisiones.
- Mejorar regulación emocional no clínica.
- Desarrollo cognitivo general.

## 12.4 Evaluación inicial

La evaluación inicial crea una primera estimación del perfil cognitivo del usuario en los 8 bloques.

**Duración:** 6–10 minutos máximo.  
**Cobertura:** los 8 motores cognitivos.  
**Estado generado:** perfil provisional.  
**Rango oficial:** se desbloquea tras 5 sesiones válidas por bloque.

## 12.5 Reglas de evaluación inicial

1. Debe ser obligatoria para una experiencia completa.
2. Debe ser breve, intensa y seria.
3. Debe generar una línea base inicial.
4. No debe asignar rango definitivo.
5. Debe configurar dificultad inicial.
6. Debe explicar que el sistema necesita más datos.

## 12.6 Texto recomendado

> Esta evaluación crea una primera estimación de tu perfil cognitivo. Tu rango oficial en cada bloque se desbloqueará cuando completes 5 sesiones en ese bloque. A partir de ahí, MindBoost podrá medir tu progreso con mayor precisión.

---

# 13. Sistema de entrenamiento

## 13.1 Duraciones disponibles

| Duración | Uso recomendado |
|---|---|
| 120 segundos | Sesión ideal rápida diaria. |
| 5 minutos | Entrenamiento corto. |
| 10 minutos | Entrenamiento estándar. |
| 20 minutos | Entrenamiento profundo. |
| 30 minutos | Entrenamiento intensivo. |

## 13.2 Regla de sesión

Las sesiones son individuales por bloque cognitivo. El usuario elige qué bloque entrenar y la duración.

## 13.3 Modo libre

El usuario puede entrenar libremente cualquier bloque y ejercicio disponible.

## 13.4 Plan adaptativo básico

La app recomienda el bloque más importante para entrenar según:

- Debilidad relativa.
- Estancamiento.
- Objetivo elegido.
- Tiempo sin entrenar.
- Consistencia reciente.
- Rango actual.
- Rendimiento histórico.

## 13.5 Si el usuario falla mucho

El sistema debe:

1. Cambiar de ejercicio.
2. Reducir presión o dificultad.
3. Penalizar moderadamente el score del bloque.
4. Recomendar entrenamiento base si el patrón se repite.

## 13.6 Tono del sistema

La app no debe humillar al usuario. Debe ser exigente, precisa y constructiva.

---

# 14. Scoring, calibración y rangos

## 14.1 Score por bloque

Cada bloque cognitivo tiene una puntuación de 0 a 100.

El score representa una aproximación dinámica del nivel actual del usuario en ese bloque, basada en rendimiento reciente, progreso personal, dificultad alcanzada y estabilidad.

## 14.2 Fórmula inicial de score por sesión

```text
BlockSessionScore =
  0.30 * AccuracyScore +
  0.20 * SpeedScore +
  0.20 * DifficultyScore +
  0.15 * ConsistencyScore +
  0.10 * ProgressScore +
  0.05 * ErrorControlScore
```

## 14.3 Actualización histórica del bloque

```text
NewBlockScore =
  0.85 * PreviousBlockScore +
  0.15 * BlockSessionScore
```

## 14.4 Score durante calibración

```text
CalibrationBlockScore = average(last 5 valid session scores)
```

## 14.5 Pesos iniciales

| Señal | Peso inicial |
|---|---:|
| Precisión | 30% |
| Velocidad | 20% |
| Dificultad | 20% |
| Consistencia | 15% |
| Progreso reciente | 10% |
| Control de errores | 5% |

## 14.6 Regla crítica de velocidad

La velocidad nunca debe premiarse si destruye la precisión.

## 14.7 Variación futura por bloque

| Bloque | Señal dominante futura |
|---|---|
| Enfoque | Consistencia + precisión |
| Decisiones | Calidad + velocidad controlada |
| Lógica | Precisión + dificultad |
| Creatividad | Fluidez + originalidad aproximada |
| Lenguaje | Precisión + velocidad verbal |
| Emociones | Consistencia + interpretación |
| Velocidad mental | Velocidad + precisión |
| Memoria | Precisión + dificultad |

## 14.8 Versionado de fórmulas

Toda fórmula debe estar versionada. Cada cambio debe registrar:

- Fecha.
- Versión.
- Motivo.
- Impacto esperado.
- Compatibilidad con scores anteriores.
- Método de migración si aplica.

## 14.9 Calibración de bloques

Un bloque se considera calibrado cuando MindBoost tiene datos mínimos suficientes para estimar el nivel real del usuario en ese motor cognitivo.

### Requisitos de calibración

1. El usuario completa 5 sesiones válidas en ese bloque.
2. Las sesiones no son abandonadas.
3. Cada sesión dura mínimo 120 segundos.
4. El rendimiento no presenta anomalías extremas.
5. El sistema puede calcular score, rango y nivel de confianza.

Antes de calibrarse, el bloque mostrará:

> Perfil provisional

Después de calibrarse:

> Rango oficial desbloqueado

## 14.10 Fiabilidad según duración

| Duración | Validez para calibración |
|---|---|
| 120s | Válida |
| 5min | Válida + más fiable |
| 10min | Alta fiabilidad |
| 20min | Muy alta fiabilidad |
| 30min | Máxima fiabilidad |

## 14.11 Global Score

El Global Score del MVP será la media simple de los 8 bloques:

```text
GlobalScore =
  (Enfoque + Decisiones + Lógica + Creatividad + Lenguaje + Emociones + VelocidadMental + Memoria) / 8
```

Para tener rango global oficial, el usuario debe tener al menos 5 de los 8 bloques calibrados.

Si no cumple el requisito:

> Perfil global provisional

## 14.12 Sistema de rangos

La progresión es:

1. Bronce III
2. Bronce II
3. Bronce I
4. Plata III
5. Plata II
6. Plata I
7. Oro III
8. Oro II
9. Oro I
10. Diamante III
11. Diamante II
12. Diamante I
13. Zafiro III
14. Zafiro II
15. Zafiro I
16. Rubí III
17. Rubí II
18. Rubí I
19. Maestro III
20. Maestro II
21. Maestro I
22. Gran Maestro III
23. Gran Maestro II
24. Gran Maestro I
25. Prodigio III
26. Prodigio II
27. Prodigio I
28. Leyenda — World Class

III es la división de entrada. I es la división superior.

## 14.13 Leyenda — World Class

En MVP, Leyenda — World Class será rango único, sin divisiones.

En V2 podrá existir sistema interno:

- Leyenda — World Class.
- Top 100.
- Top 10.
- Rank #1 global.

## 14.14 Penalización por inactividad

Si el usuario deja de entrenar durante 1 mes, baja un rango.

Texto recomendado:

> Tu rango ha bajado por inactividad prolongada. No significa que hayas perdido todo tu progreso, sino que MindBoost necesita recalibrar tu rendimiento actual con nuevas sesiones.

## 14.15 Protección contra volatilidad

El sistema debe evitar cambios bruscos por una sola mala sesión. El score debe reflejar tendencia, no ruido.

---

# 15. Experiencia de usuario

## 15.1 Estilo visual

MindBoost debe sentirse:

- Científico.
- Cósmico.
- Minimalista.
- Premium.
- Calmado.
- De alto rendimiento.
- Futurista sin parecer videojuego infantil.

Dirección visual:

- Dark mode primero.
- Contraste elegante.
- Elementos cósmicos sutiles.
- Gráficas limpias.
- Tarjetas serias.
- Animaciones suaves.
- Tipografía moderna.
- Nada caricaturesco.
- Nada infantil.
- Nada saturado.

MindBoost debe sentirse como:

> Laboratorio cognitivo + universo personal + entrenador de élite.

No debe sentirse como:

> Candy Crush para el cerebro.

## 15.2 Dashboard Cognitivo

La pantalla central será el Dashboard Cognitivo.

Debe mostrar:

1. Saludo corto.
2. MindBoost Global Score.
3. Rango global.
4. Botón “Entrenar ahora”.
5. Bloque recomendado.
6. Progreso hacia siguiente rango.
7. Tarjetas de los 8 bloques.
8. Score de cada bloque.
9. Estado de calibración.
10. Racha o consistencia.
11. Acceso a IA Coach Elite.
12. Acceso a ranking cuando esté activo.
13. Último resultado relevante.

Jerarquía visual:

1. Qué soy ahora.
2. Qué debo entrenar ahora.
3. Cómo estoy progresando.
4. Qué puedo desbloquear.

El usuario debe entender en menos de 5 segundos:

> Este es mi estado mental actual. Esto debo entrenar hoy. Este es mi progreso.

## 15.3 Pantalla de bloque

Cada pantalla de bloque debe mostrar:

1. Nombre del bloque.
2. Descripción seria.
3. Score 0–100.
4. Rango del bloque.
5. Estado de calibración.
6. Progreso histórico.
7. Métricas principales del bloque.
8. Ejercicios disponibles.
9. Recomendación del sistema.
10. Botón de sesión rápida 120s.
11. Duraciones disponibles.
12. Fortalezas/debilidades si es Elite.
13. Explicación de qué entrena ese bloque.
14. Progreso hacia el siguiente rango.

Ejemplo:

> Memoria  
> Entrena retención, recuperación, memoria de trabajo y manipulación activa de información.  
> Score: 42.8  
> Rango: Diamante II  
> Calibración: completa  
> Recomendación: trabajar secuencias inversas para mejorar estabilidad.

## 15.4 Pantalla de ejercicio

Debe mostrar:

1. Nombre del ejercicio.
2. Bloque cognitivo.
3. Habilidad entrenada.
4. Duración seleccionada.
5. Explicación breve.
6. Indicador de dificultad.
7. Botón de inicio.
8. Estado de conexión si afecta guardado.
9. Advertencia si modo práctica no impacta score.

## 15.5 Resultado post-sesión

Debe mostrar:

1. Sesión completada.
2. Bloque entrenado.
3. Score de sesión.
4. Cambio de score.
5. Precisión.
6. Velocidad.
7. Dificultad.
8. Consistencia.
9. Errores clave.
10. XP.
11. Progreso hacia rango.
12. Feedback.
13. Siguiente recomendación.
14. Botón “Entrenar otra vez”.
15. Botón “Volver al dashboard”.
16. CTA Elite si aplica.

Regla:

> El resultado no debe sentirse como una recompensa superficial. Debe sentirse como un informe de rendimiento cognitivo.

---

# 16. MindBoost Elite

## 16.1 Precio

2 €/mes.

## 16.2 Posicionamiento

Elite no desbloquea el derecho a entrenar. El entrenamiento base es gratuito e ilimitado.

Elite desbloquea:

> Comprensión, optimización y dirección avanzada.

## 16.3 Funciones Elite

1. IA Coach.
2. Análisis profundo por sesión.
3. Análisis semanal.
4. Recomendaciones personalizadas.
5. Programas inteligentes de 30 días.
6. Predicción de progreso.
7. Explicaciones detalladas por ejercicio.
8. Comparativa avanzada.
9. Detección de fortalezas.
10. Detección de debilidades.
11. Metacognición Elite.
12. Análisis de consistencia.
13. Análisis de estancamiento.
14. Botón para aplicar recomendaciones al plan.
15. Resúmenes personalizados guardados.
16. Consejos generales de productividad, sueño y estilo de vida.

## 16.4 Gratis

| Función | Estado |
|---|---|
| 8 bloques entrenables | Gratis |
| Entrenamiento ilimitado | Gratis |
| Sesiones de todas las duraciones | Gratis |
| Score básico | Gratis |
| Rango básico | Gratis |
| Progreso básico | Gratis |
| Modo libre | Gratis |
| Evaluación inicial | Gratis |

## 16.5 Sin anuncios

MindBoost no tendrá anuncios. Los anuncios dañarían la percepción seria, científica y premium del producto.

---

# 17. IA Coach Elite

## 17.1 Personalidad

La IA Coach será un mentor científico especializado en la mente humana. Tono:

- Científico.
- Inspirador.
- Preciso.
- Motivador.
- Directo.
- No infantil.

## 17.2 Funciones

| Función | Descripción |
|---|---|
| Análisis profundo | Explica fortalezas, debilidades y patrones. |
| Recomendaciones personalizadas | Sugiere bloques, ejercicios y duración. |
| Informes avanzados | Genera reportes detallados de progreso. |
| Programas inteligentes | Crea programas de 30 días. |
| Predicción de progreso | Estima evolución probable según hábitos. |
| Explicaciones por ejercicio | Explica qué entrena cada ejercicio. |
| Metacognición Elite | Ayuda al usuario a entender cómo piensa. |
| Consejos generales | Productividad, sueño y estilo de vida. |

## 17.3 Acceso a datos

La IA debe trabajar principalmente con resúmenes estructurados, no con datos crudos ilimitados.

## 17.4 Memoria IA

Los resúmenes de IA se guardarán en Firestore como documentos estructurados en formato markdown o markdown-like, con posible caché local.

Estructura recomendada:

```text
AICoachSummary
  userId
  summaryMd
  strengthsMd
  weaknessesMd
  currentPlanMd
  recommendationsMd
  lastUpdatedAt
  version
```

El usuario Elite debe poder ver una versión legible de estos resúmenes.

En V1.1 o V2, el usuario debe poder borrar la memoria IA.

## 17.5 Modificación de plan

La IA no modifica automáticamente el plan sin confirmación.

Flujo:

1. IA detecta oportunidad de mejora.
2. IA recomienda cambio.
3. Usuario ve explicación.
4. Usuario pulsa “Aplicar al plan”.
5. Sistema actualiza plan.

## 17.6 Límite diario de IA

| Tipo | Límite diario inicial |
|---|---:|
| Mensajes del usuario | 20 |
| Respuestas IA | 20 |
| Input aproximado | 20.000 tokens/día |
| Output aproximado | 10.000 tokens/día |

Mensaje al agotar límite:

> Has alcanzado tu límite diario de IA Coach. Tu entrenamiento sigue disponible sin límites. El análisis avanzado se restablecerá mañana.

## 17.7 Límites de la IA

La IA nunca puede:

1. Diagnosticar enfermedades.
2. Decir que el usuario tiene TDAH, depresión, ansiedad clínica, deterioro cognitivo o cualquier trastorno.
3. Sustituir a un médico, psicólogo o neuropsicólogo.
4. Prometer mejora garantizada.
5. Inventar datos del usuario.
6. Modificar el plan sin confirmación.
7. Manipular emocionalmente al usuario para pagar.
8. Usar miedo como venta.
9. Dar consejos médicos.
10. Recomendar medicación.
11. Interpretar resultados como diagnóstico clínico.
12. Decir que MindBoost aumenta el IQ garantizado.
13. Comparar al usuario de forma humillante.
14. Exponer datos privados.
15. Dar respuestas fuera del propósito de MindBoost cuando pongan en riesgo la confianza del producto.

---

# 18. Competición, ranking y comunidad

## 18.1 MVP

En MVP puede existir ranking global básico solo si no compromete:

1. Entrenamiento.
2. Scoring.
3. Estabilidad.
4. Privacidad.
5. Velocidad de lanzamiento.

Si compromete cualquiera de esos puntos, pasa a V1.1.

## 18.2 V2

| Función | Descripción |
|---|---|
| PvP 1v1 | Duelos en tiempo real. |
| PvP 2v2 | Duelos por equipos. |
| PvP 3v3 | Duelos por equipos ampliados. |
| Modalidades por bloque | Competición en memoria, lógica, enfoque, etc. |
| Modalidades globales | Competición por score general. |
| Torneos | Eventos competitivos periódicos. |
| Comunidad | Perfiles, interacción, retos y grupos. |
| Anti-trampa avanzado | Validación de patrones anómalos. |

## 18.3 Anti-trampa futuro

Señales a detectar:

- Tiempos de reacción imposibles.
- Patrones repetitivos artificiales.
- Cambios de rendimiento extremos.
- Uso de automatización.
- Inconsistencias entre bloques.
- Sesiones anormalmente perfectas.
- Manipulación local.

---

# 19. Privacidad, datos y cumplimiento

## 19.1 Datos obligatorios para MVP

1. Email.
2. Nombre visible o alias.
3. Contraseña/autenticación.
4. País.
5. Rango de edad.
6. Objetivo cognitivo principal.
7. Aceptación de términos.
8. Aceptación de privacidad.

## 19.2 Datos no obligatorios

1. Nombre legal completo.
2. DNI.
3. Fecha exacta de nacimiento.
4. Profesión.
5. Nivel educativo.
6. Datos médicos.
7. Diagnósticos.
8. Dirección.
9. Teléfono.

## 19.3 Decisión definitiva sobre DNI

El DNI queda eliminado del MVP.

No se pedirá DNI para usar MindBoost ni para pagar Elite.

Los pagos deben delegarse en App Store, Google Play, RevenueCat u otro sistema de suscripción.

MindBoost no debe almacenar documentos de identidad salvo obligación legal futura muy específica.

## 19.4 Datos de sesión

Cada sesión debe guardar:

1. User ID.
2. Session ID.
3. Fecha y hora.
4. Bloque cognitivo.
5. Ejercicio.
6. Duración seleccionada.
7. Duración real.
8. Dificultad inicial.
9. Dificultad final.
10. Nivel máximo alcanzado.
11. Respuestas totales.
12. Respuestas correctas.
13. Respuestas incorrectas.
14. Omisiones.
15. Precisión.
16. Tiempo medio de respuesta.
17. Mejor tiempo de respuesta.
18. Peor tiempo de respuesta.
19. Variabilidad del tiempo de respuesta.
20. Score bruto de sesión.
21. Score normalizado.
22. Cambio en score del bloque.
23. XP ganada.
24. Estado de calibración.
25. Señales de anomalía.
26. Estado completada/abandonada.
27. Versión del ejercicio.
28. Versión de la fórmula de scoring.
29. Plataforma/dispositivo de forma no invasiva.
30. Estado de conexión/sincronización.

## 19.5 Requisitos de privacidad

1. Consentimiento claro para personalización.
2. Consentimiento separado para IA Coach si usa datos de progreso.
3. Política de privacidad desde lanzamiento.
4. Eliminación de cuenta.
5. Separación entre datos de entrenamiento y datos de facturación.
6. Logs mínimos necesarios.
7. Reglas de Firestore estrictas.
8. No acceso cruzado entre usuarios.
9. No venta de datos.
10. No recogida de datos innecesarios.

## 19.6 Mercados

Lanzamiento inicial: España → Europa → mundial.

Por operar primero en Europa, el diseño debe contemplar GDPR desde el inicio.

---

# 20. Arquitectura técnica

## 20.1 Stack

| Capa | Tecnología |
|---|---|
| App | Kotlin Multiplatform |
| UI | Compose Multiplatform |
| Backend/database | Firebase Firestore |
| Auth | Firebase Auth recomendado |
| Suscripción | RevenueCat o stores nativas |
| IA | API externa mediante backend intermedio recomendado |
| Storage local | Persistencia local multiplataforma |

## 20.2 Principios técnicos

1. La app debe ser rápida.
2. La experiencia de entrenamiento no debe depender siempre de red.
3. Los scores deben poder calcularse localmente y sincronizarse.
4. Firestore debe ser fuente cloud, no cuello de botella.
5. La IA no debe recibir más datos de los necesarios.
6. Las fórmulas de scoring deben versionarse.
7. Los ejercicios deben ser configurables y extensibles.
8. El sistema debe ser robusto ante fallos de conexión.
9. La seguridad de Firestore es crítica.
10. La manipulación local del score debe mitigarse progresivamente.

## 20.3 Estructura de datos recomendada

```text
User
  id
  email
  displayName
  country
  ageRange
  createdAt
  subscriptionStatus
  goals
  onboardingCompleted

CognitiveProfile
  userId
  globalScore
  globalRank
  blockScores[]
  calibrationStatus
  lastUpdatedAt

BlockScore
  blockId
  score
  rank
  sessionsCompleted
  calibrated
  lastSessionAt
  confidenceLevel

Session
  id
  userId
  blockId
  exerciseId
  durationSelected
  durationActual
  difficultyStart
  difficultyEnd
  maxLevelReached
  totalAnswers
  correctAnswers
  incorrectAnswers
  omissions
  accuracy
  averageReactionTime
  bestReactionTime
  worstReactionTime
  reactionTimeVariability
  rawScore
  normalizedScore
  scoreDelta
  xpEarned
  anomalyFlags
  completed
  exerciseVersion
  scoringVersion
  platform
  syncStatus
  createdAt

Exercise
  id
  blockId
  name
  skill
  scientificObjective
  minDifficulty
  maxDifficulty
  scoringVersion
  status

AICoachSummary
  userId
  summaryMd
  strengthsMd
  weaknessesMd
  currentPlanMd
  recommendationsMd
  lastUpdatedAt
  version
```

## 20.4 Versiones mínimas soportadas

| Plataforma | Versión mínima |
|---|---|
| Android | Android 8.0 Oreo / API 26 |
| iOS | iOS 15 |

Si hay conflicto entre compatibilidad amplia y estabilidad, MindBoost prioriza estabilidad.

---

# 21. Analytics y métricas internas

## 21.1 Eventos obligatorios desde día 1

1. App opened.
2. Sign up started.
3. Sign up completed.
4. Onboarding started.
5. Onboarding completed.
6. Initial assessment started.
7. Initial assessment completed.
8. Block selected.
9. Exercise started.
10. Exercise completed.
11. Exercise abandoned.
12. Session duration selected.
13. Session completed.
14. Score updated.
15. Rank unlocked.
16. Calibration completed.
17. Elite paywall viewed.
18. Elite subscription started.
19. Elite subscription cancelled.
20. IA Coach opened.
21. IA message sent.
22. IA limit reached.
23. Recommendation applied.
24. App crash.
25. Sync failed.
26. Login failed.
27. Session save failed.

## 21.2 Dashboard CEO mínimo

1. Usuarios registrados.
2. Completion rate del onboarding.
3. Evaluación completada.
4. Primera sesión completada.
5. Retención D1/D7/D30.
6. Sesiones por usuario.
7. Mejora media por bloque.
8. Conversión Elite.
9. Uso IA.
10. Crash-free users.

## 21.3 Métrica principal de éxito a 30 días

**Mejora media validada por bloque después de 30 días.**

## 21.4 Métricas acompañantes

1. Retención D7.
2. Retención D30.
3. Sesiones por usuario por semana.
4. Porcentaje de usuarios con al menos 3 bloques calibrados.
5. Conversión a Elite.
6. Uso del IA Coach.
7. Porcentaje de usuarios que perciben progreso.
8. Crash-free users.
9. Tiempo medio hasta primera sesión.
10. Completion rate de evaluación inicial.

---

# 22. Requisitos no funcionales

## 22.1 Performance

| Requisito | Objetivo |
|---|---|
| Arranque de app | Rápido y sin bloqueos. |
| Inicio de sesión | Fluido. |
| Inicio de ejercicio | Casi inmediato. |
| Guardado de sesión | Seguro, sin pérdida de datos. |
| Resultado post-sesión | Rápido y claro. |
| Animaciones | Suaves. |
| Offline parcial | Deseable. |

## 22.2 Calidad

1. Sin crashes críticos.
2. Sin pérdida de progreso.
3. Sin scores incoherentes.
4. Sin esperas largas al terminar sesión.
5. Sin bloqueo por fallo de IA.
6. Sin errores de suscripción.
7. Sin exposición de datos de otros usuarios.
8. Sin asignación de rango antes de calibración.

## 22.3 Robustez offline

Si el usuario pierde conexión durante una sesión:

1. La sesión debe continuar si ya empezó.
2. El resultado debe guardarse localmente.
3. La sincronización debe ocurrir al recuperar conexión.
4. El usuario debe recibir estado claro si algo queda pendiente.

---

# 23. Criterios de aceptación MVP

## 23.1 Cuenta

- El usuario puede registrarse.
- El usuario puede iniciar sesión.
- El usuario no puede usar la app sin cuenta.
- Los datos se sincronizan correctamente.
- No hay acceso cruzado entre usuarios.

## 23.2 Onboarding

- El usuario completa objetivos.
- El usuario completa evaluación inicial.
- El sistema genera perfil provisional.
- El usuario entiende que el rango requiere 5 sesiones por bloque.

## 23.3 Entrenamiento

- El usuario puede seleccionar cualquiera de los 8 bloques.
- El usuario puede elegir duración.
- El usuario puede completar ejercicios.
- La app registra métricas.
- La app actualiza score.
- La app muestra feedback claro.

## 23.4 Scoring

- Cada bloque tiene score 0–100.
- El global se calcula correctamente.
- El sistema distingue bloque calibrado/no calibrado.
- El rango aparece tras 5 sesiones válidas.
- La inactividad de 1 mes puede reducir rango.
- La fórmula de scoring está versionada.

## 23.5 Elite

- El usuario puede suscribirse por 2 €/mes.
- Elite desbloquea IA Coach.
- IA Coach analiza progreso mediante resúmenes.
- IA Coach recomienda cambios.
- El usuario puede aplicar recomendaciones con botón.
- Hay límite diario de uso IA.

## 23.6 Privacidad

- No se solicita DNI en MVP.
- El usuario puede consultar política de privacidad.
- Los datos están separados por usuario.
- Firestore impide acceso cruzado.
- La IA recibe solo datos necesarios.

## 23.7 Performance

- La app no debe sentirse lenta.
- No debe perder sesiones completadas.
- No debe fallar al calcular score.
- No debe depender de IA para funcionar.

---

# 24. Bugs bloqueantes

Bloquean lanzamiento:

1. Pérdida de progreso.
2. Crash durante sesión.
3. Score incorrecto o incoherente.
4. Usuario no puede registrarse.
5. Usuario no puede iniciar sesión.
6. Datos de un usuario visibles para otro.
7. Suscripción no desbloquea Elite.
8. Elite se desbloquea sin pagar por error.
9. IA accede a datos incorrectos.
10. Firestore guarda sesiones duplicadas graves.
11. App tarda demasiado en abrir.
12. Resultados no se guardan.
13. Evaluación inicial no termina.
14. Bloques no calculan score.
15. Rango se asigna antes de calibración.
16. Pago falla sin explicación.
17. App se bloquea sin conexión.
18. UI principal inutilizable en iOS o Android.
19. Errores graves de privacidad.
20. Errores que impidan completar entrenamiento.

No bloquean lanzamiento:

1. Animaciones no perfectas.
2. Algún texto mejorable.
3. Algunos ejercicios fuera del MVP.
4. Ranking no disponible.
5. PvP no disponible.
6. Comunidad no disponible.

---

# 25. Roadmap

## 25.1 MVP — 25 julio 2026

- Android + iOS.
- Cuenta obligatoria.
- Onboarding completo.
- Evaluación inicial.
- 8 bloques.
- Sesiones por bloque.
- Modo libre.
- Plan adaptativo básico.
- Scores 0–100.
- Rangos.
- Firestore.
- Elite 2 €/mes.
- IA Coach básica/funcional.
- Métricas principales.
- Privacidad base.

## 25.2 V1.1

- Informes semanales.
- Mejor calibración de scores.
- Mejor visualización de progreso.
- Mejor memoria IA.
- Programas de 30 días más avanzados.
- Comparativas por bloque.
- Mejoras de onboarding.
- Mejoras de retención.
- Ranking global si no entra en MVP.
- Borrado de memoria IA.

## 25.3 V2

- PvP 1v1 tiempo real.
- PvP 2v2 y 3v3.
- Comunidad.
- Torneos.
- Ligas.
- Anti-trampa avanzado.
- Ranking por modalidades.
- Perfil competitivo avanzado.
- Top Leyenda / Top 100 / Top 10 / Rank #1.

## 25.4 V3

- Validación científica propia.
- Estudios internos longitudinales.
- Benchmarking cognitivo más robusto.
- Modelos predictivos avanzados.
- Personalización profunda por clusters de usuario.
- Programas avanzados de metacognición.
- Motor de ejercicios personalizado por IA, si se valida seguridad y calidad.

---

# 26. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Claims exagerados | Legal/reputacional | Lenguaje prudente y medible. |
| Scores poco fiables | Pérdida de confianza | Calibración tras 5 sesiones y confidence level. |
| MVP demasiado grande | Retraso/calidad baja | PvP/comunidad fuera del MVP. |
| Coste IA | Margen negativo | Límites diarios de tokens. |
| Privacidad | Riesgo legal | Minimización de datos y resúmenes IA. |
| Usuario no entiende progreso | Baja retención | Visualizaciones simples y feedback claro. |
| Competición injusta | Desconfianza | Anti-trampa en roadmap. |
| App percibida como juego | Posicionamiento débil | Diseño serio, tono científico y entrenamiento guiado. |
| Fórmula de score manipulable | Ranking inútil | Versionado, anomalías y validación server-side futura. |
| Dependencia de Firestore | Latencia/fallos | Caché local y sincronización robusta. |

---

# 27. Claims, ciencia y comunicación

## 27.1 Claims permitidos

- “Entrena tus capacidades cognitivas.”
- “Mide tu progreso.”
- “Desarrolla memoria, enfoque, lógica, lenguaje y velocidad mental.”
- “Sigue un plan adaptativo.”
- “Descubre tus fortalezas y debilidades cognitivas.”
- “Mejora tu rendimiento dentro de MindBoost.”
- “Entrena habilidades cognitivas mediante ejercicios adaptativos.”

## 27.2 Claims prohibidos

- “Aumenta tu IQ garantizado.”
- “Cura TDAH.”
- “Previene Alzheimer.”
- “Mejora tu vida laboral garantizado.”
- “Demostrado clínicamente” sin estudio propio.
- “El mejor científicamente probado” sin validación externa.
- “Diagnostica tu mente.”

## 27.3 Criterio científico

MindBoost puede ser ambicioso en producto, pero debe ser prudente en comunicación.

La evidencia sobre entrenamiento cognitivo computarizado es heterogénea. Existen resultados prometedores para tareas específicas y ciertos dominios, pero la transferencia amplia a la vida diaria debe tratarse con cautela y validarse con metodología propia.

## 27.4 Razón regulatoria

El sector de brain training ha tenido precedentes regulatorios por claims no sustentados. MindBoost debe evitar afirmaciones clínicas o garantizadas, especialmente en salud, rendimiento académico/laboral o deterioro cognitivo.

---

# 28. AGENTS.md — Base científica y operativa

## 28.1 Objetivo

Mantener una base viva de investigación científica y decisiones de producto para que MindBoost evolucione hacia el mejor entrenador cognitivo posible.

## 28.2 Estructura recomendada

```markdown
# MindBoost Scientific Research Base

## 1. Objetivo
Recolectar, analizar y sintetizar investigación científica relevante para mejorar MindBoost.

## 2. Dominios cognitivos
- Atención
- Memoria de trabajo
- Velocidad de procesamiento
- Razonamiento
- Lenguaje
- Creatividad
- Regulación emocional no clínica
- Toma de decisiones
- Metacognición

## 3. Evidencia por dominio
Para cada paper:
- Título
- Autores
- Año
- Tipo de estudio
- Población
- Tamaño de muestra
- Dominio cognitivo
- Intervención
- Resultados
- Limitaciones
- Aplicabilidad a MindBoost
- Nivel de confianza

## 4. Claims permitidos
Lista de claims seguros, prudentes y sustentables.

## 5. Claims prohibidos
Claims médicos, terapéuticos o no demostrados.

## 6. Ideas de ejercicios
Cada idea debe incluir:
- Dominio cognitivo
- Hipótesis
- Métrica
- Riesgo de medición
- Criterio de éxito

## 7. Sistema de scoring
Registrar cambios de fórmula, razones y fecha.

## 8. Validación futura
Diseño de estudios internos para medir mejora tras 7, 30 y 90 días.
```

## 28.3 Regla para agentes

Todo agente que proponga un ejercicio, claim, cambio de scoring o recomendación científica debe justificarlo con:

1. Hipótesis.
2. Métrica.
3. Riesgo.
4. Criterio de éxito.
5. Nivel de confianza.
6. Fuente si aplica.

---

# 29. Backlog inicial por épicas

## Épica 1 — Cuenta y perfil

- Registro.
- Login.
- Perfil básico.
- País.
- Rango de edad.
- Objetivo cognitivo.
- Estado de suscripción.

## Épica 2 — Onboarding

- Flujo de bienvenida.
- Selección de objetivos.
- Selección de bloques prioritarios.
- Preguntas de hábitos básicos.
- Inicio de evaluación.

## Épica 3 — Evaluación inicial

- Mini-tests para 8 bloques.
- Cálculo de perfil provisional.
- Configuración de dificultad inicial.
- Explicación de calibración.

## Épica 4 — Motor de ejercicios

- Modelo Exercise.
- Configuración por bloque.
- Duraciones.
- Dificultad.
- Métricas.
- Feedback.

## Épica 5 — Scoring

- Fórmula inicial.
- Score por sesión.
- Score histórico.
- Score global.
- Versionado.
- Calibración.
- Rangos.

## Épica 6 — Dashboard Cognitivo

- Global Score.
- Rango global.
- Tarjetas de bloques.
- Recomendación diaria.
- Estado de calibración.
- Acceso a entrenamiento.

## Épica 7 — Pantalla de bloque

- Score por bloque.
- Rango.
- Métricas.
- Ejercicios.
- Recomendación.
- Historial básico.

## Épica 8 — Resultado post-sesión

- Score de sesión.
- Delta.
- Precisión.
- Velocidad.
- Dificultad.
- XP.
- Feedback.
- CTA Elite.

## Épica 9 — MindBoost Elite

- Paywall.
- Suscripción.
- Estado Elite.
- Acceso IA.
- Bloqueo/desbloqueo correcto.

## Épica 10 — IA Coach

- Resumen de usuario.
- Chat.
- Análisis.
- Recomendaciones.
- Botón aplicar al plan.
- Límite diario.
- Guardrails.

## Épica 11 — Firestore y sincronización

- Modelos.
- Escritura de sesiones.
- Lectura de progreso.
- Reglas de seguridad.
- Caché local.
- Sincronización.

## Épica 12 — Analytics

- Eventos obligatorios.
- Dashboard mínimo.
- Errores.
- Conversión.
- Retención.

## Épica 13 — Calidad y lanzamiento

- Pruebas críticas.
- Bugs bloqueantes.
- Performance.
- Privacidad.
- Revisión App Store / Google Play.

---

# 30. Glosario

| Término | Definición |
|---|---|
| Motor cognitivo | Bloque principal de capacidad cognitiva entrenable. |
| Bloque | Sinónimo operativo de motor cognitivo. |
| Score | Puntuación 0–100 de rendimiento aproximado. |
| Global Score | Media de los 8 scores de bloque. |
| Calibración | Proceso de reunir datos suficientes para asignar rango oficial. |
| Perfil provisional | Estado antes de tener suficientes sesiones. |
| Rango | Nivel competitivo asociado al score. |
| Elite | Suscripción de pago con IA y análisis avanzado. |
| IA Coach | Mentor científico conversacional de MindBoost. |
| Sesión válida | Sesión completada, no abandonada, mínimo 120s, sin anomalías extremas. |
| Confidence level | Nivel de confianza del sistema sobre la precisión del score. |
| PvP | Competición jugador contra jugador. |
| Anti-trampa | Sistema de detección de uso abusivo o manipulación. |

---

# 31. Referencias

Estas referencias informan las decisiones de prudencia científica, privacidad y regulación. No convierten automáticamente a MindBoost en producto clínicamente validado; sirven como base de diseño responsable.

1. Reglamento General de Protección de Datos, Reglamento (UE) 2016/679 — EUR-Lex: https://eur-lex.europa.eu/eli/reg/2016/679/oj/eng
2. Reglamento Europeo de Inteligencia Artificial, Reglamento (UE) 2024/1689 — EUR-Lex: https://eur-lex.europa.eu/eli/reg/2024/1689/oj/eng
3. FTC — Lumosity to Pay $2 Million to Settle FTC Deceptive Advertising Charges: https://www.ftc.gov/news-events/news/press-releases/2016/01/lumosity-pay-2-million-settle-ftc-deceptive-advertising-charges-its-brain-training-program
4. AEPD — GDPR compliance of processing that embed Artificial Intelligence: https://www.aepd.es/guides/gdpr-compliance-processings-that-embed-ia.pdf
5. Harris et al. — Can cognitive training capitalise on near transfer effects? PLOS ONE, 2023: https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0293657
6. Sala et al. — Near and Far Transfer in Cognitive Training: A Second-Order Meta-Analysis: https://osf.io/download/c5kxn/
7. Chan et al. — Computerized cognitive training for memory functions in mild cognitive impairment or dementia, 2024: https://pmc.ncbi.nlm.nih.gov/articles/PMC10764827/
8. Zhu et al. — Cognitive Training During Midlife: A Systematic Review and Meta-analysis, 2024: https://pmc.ncbi.nlm.nih.gov/articles/PMC12602558/

---

# Cierre

MindBoost debe construirse con una obsesión central:

> Maximizar el progreso cognitivo medible del usuario, sin infantilizar la experiencia, sin exagerar claims y sin sacrificar privacidad.

La experiencia debe hacer que el usuario sienta:

> Estoy entrenando mi mente de verdad. Entiendo mi progreso. Sé qué mejorar. Y cada sesión me acerca a una versión mentalmente más fuerte de mí mismo.
