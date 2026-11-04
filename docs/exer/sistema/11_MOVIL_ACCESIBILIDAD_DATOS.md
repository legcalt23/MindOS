# Restricciones móviles, accesibilidad y datos

## 1. Contexto de plataforma

El canon documenta Kotlin Multiplatform/Compose Multiplatform, lógica común, offline y mínimos Android API 26/iOS 15. Son requisitos heredados, no versiones comprobadas del entorno. `/exer` especifica comportamiento y evidencia necesaria; no programa adaptadores ni promete paridad de ejecución.

Definir invariantes compartidos de reglas, contenido, resultados y eventos. Separar diferencias de plataforma que pueden cambiar la demanda: entrada táctil, pantalla, renderizado, audio, accesibilidad, interrupciones y recuperación. No usar un test desktop como prueba de temporización física móvil.

## 2. Contrato temporal

Describir instantes: preparación, solicitud de presentación, frame presentado si es observable, señal realmente disponible según medida física, evento de entrada y recepción por el motor. Identificar reloj monotónico, unidades, origen y orden. La diferencia entre eventos software no equivale automáticamente al tiempo de decisión humana.

Registrar cuando sea necesario y autorizado frecuencia de refresco, calidad/frame pacing, modalidad de entrada, cambio de dispositivo, ruta de audio y eventos de interrupción. No estimar latencia exacta por marca/OS ni restar constantes publicadas a todos los dispositivos.

Especificar qué tolerancias exige el diseño y cómo se medirán; los valores desconocidos quedan como requisito empírico. Determinar si el timing es crítico para entrenamiento, comparabilidad intra-dispositivo, entre dispositivos o interpretación oficial. Si solo falta comparabilidad oficial, preservar entrenamiento con etiqueta adecuada.

## 3. Superficies y entrada

Definir orientación permitida, escalado de estímulos, áreas táctiles, densidad, contraste, lectura, posiciones y respuestas. Controlar que una pantalla pequeña, un pulgar o una velocidad motora no dominen un constructo distinto. Animaciones, efectos cósmicos o transiciones no pueden contaminar el intervalo cognitivo relevante.

No convertir la presión temporal en dificultad universal. Si respuesta motora es irrelevante, usar una interacción que la minimice; si es parte del constructo, declararlo y revisar equidad/interpretación.

## 4. Accesibilidad como condición de validez

Evaluar visión/color/contraste, texto/lectura, audición, motricidad, reducción de movimiento, lector de pantalla y comprensión de instrucciones. Para cada ajuste señalar:

| Clase | Tratamiento |
|---|---|
| Barrera irrelevante eliminada con equivalencia apoyada | Conservar comparabilidad bajo condiciones evidenciadas |
| Equivalencia plausible sin validar | Condición etiquetada; puente/estudio pendiente |
| Cambia el constructo/demanda | Variante distinta, interpretación propia |
| Permite práctica pero no medición comparable | Training-only |
| No preserva la tarea | Ofrecer familia alternativa; no fingir accesibilidad equivalente |

No declarar «compatible con lector de pantalla» en una tarea visual rápida sin describir cómo y qué se entrena en esa modalidad. No penalizar una discapacidad irrelevante para la capacidad que se pretende estimar. La misma UI accesible puede necesitar otra norma/forma; eso no implica menor valor de entrenamiento.

## 5. Pausa, interrupción y offline

Definir respuesta a llamada, background, bloqueo, audio cambiado, pérdida de foco, batería/carga, process death y reconexión. Qué ensayo continúa, qué se invalida, qué queda sellado y qué puede recuperar el usuario. No reconstruir evidencia oficial desde eventos incompletos ambiguos.

Los ocho motores deben tener ruta sin permisos sensibles. Núcleo offline y determinista; no introducir cámara/micrófono/sensores como dependencia por atractivo. El entrenamiento puede seguir sin red, pero integridad/elegibilidad de evidencia offline se somete a la política vigente. No usar el timestamp del servidor como reloj de ensayo.

## 6. Datos necesarios

Cada campo propuesto declara finalidad, decisión que soporta, mínimo necesario, ubicación, retención, acceso, consentimiento aplicable y alternativa menos invasiva. Separar datos del servicio, analytics opt-in, investigación y contexto subjetivo local. Pseudonimización no equivale a anonimato.

Contrato conceptual típico, solo si es pertinente: ID de sesión/subject, ejercicio/familia/versiones, seed o ID de ítem, índice de ensayo, condición, parámetros de dificultad, acción/respuesta, corrección/tipo de error, tiempo e integridad, cambios adaptativos, exposición, fin/cancelación y razón de elegibilidad. No recopilar respuestas sensibles completas cuando basten hechos derivados para el uso autorizado.

Conservar linaje suficiente para reconstrucción y pruebas, sin crear un almacén ilimitado de datos cognitivos. La política de retención real pendiente se deja como decisión, no como «para siempre» por defecto. No incorporar datos identificables de usuarios al grafo de proyecto.

## 7. Integridad semántica

Diseñar IDs/idempotencia, versión y subject en los contratos para evitar duplicación o mezcla entre cuentas. Un ejemplo de schema no autoriza migrarlo. No reproducir bugs históricos de borrado general ni proponer purgas como parte del diseño de ejercicios.

Enumerar qué decisiones dependen de datos que el motor facts-only no entrega. No reconstruir perfiles prohibidos calculando ratios de hechos ni introducir proxies de madurez/confianza bajo otros nombres. La minimización del contrato vigente es una frontera a respetar o proponer modificar al CEO.

## 8. Gate móvil

Un diseño debe tener una matriz de verificación futura por iOS/Android, gama relevante, modalidad y accesibilidad, con casos de interrupción y timing. `NOT_VERIFIED` es el estado cuando no se ejecutó. No exigir builds completos para una corrección meramente documental, ni declarar entrenamiento listo porque una simulación abstracta funciona.
