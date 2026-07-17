# Dispositivos, recorridos visuales y accesibilidad

## Índice

1. Identidad del binario
2. Matriz de dispositivos
3. Construcción del recorrido
4. Ejecución
5. Contrato visual MindBoost
6. Accesibilidad e interacción
7. Evidencia y puerta

## 1. Identidad del binario

Antes de instalar:

- Registrar variante, application/bundle ID, version name/code, commit y hash del artefacto.
- Confirmar que el binario incluye el diff final y configuración de test, no producción.
- Desinstalar/reinstalar o limpiar datos solo cuando el escenario exija estado frío; registrar la acción.
- No mezclar evidencia de APK/app/frameworks distintos.

## 2. Matriz de dispositivos

Seleccionar por alcance y riesgo, no por disponibilidad cómoda.

### Android mínimo para HIGH/CRITICAL

- API 26, tamaño de teléfono compacto o representativo.
- Una API estable reciente, tamaño/densidad representativos.
- Al menos una configuración con navegación gestual o equivalente actual.

Añadir tablet/foldable solo si el producto o cambio lo soporta/afecta. No exigir soporte no declarado.

### iOS cuando sea aplicable

- iOS 15.
- Una versión estable reciente soportada.

Requiere macOS/Xcode o infraestructura real equivalente. No declarar iOS verificado con una compilación Android o lectura de `commonMain`.

### Variaciones visuales aplicables

- Dark mode obligatorio.
- Tamaño de fuente normal y grande; usar al menos 1.3× y un tamaño de accesibilidad alto cuando la plataforma lo permita.
- Idioma/locale principal y un locale que cambie longitud/formato si está soportado.
- Orientación configurada por producto; comprobar que una rotación no corrompe sesión aunque la UI esté bloqueada.

## 3. Construcción del recorrido

Derivar el journey de:

1. Criterios de aceptación del handoff.
2. Flujo anterior y posterior alcanzado.
3. Estado vacío, carga, éxito y error.
4. Reanudación/background/process death.
5. Efecto persistido final.

Usar Journeys de Android Studio/Antigravity cuando esté disponible y sea compatible. Si no lo está, usar herramientas nativas de dispositivo, ADB/XCTest/Compose UI y navegación guiada por visión. Documentar el método. La indisponibilidad de Journeys no autoriza omitir el recorrido.

Las aserciones deben describir significado, no coordenadas frágiles: texto/estado visible, elemento habilitado, progreso persistido, ausencia de superposición, resultado correcto.

## 4. Ejecución

Por recorrido:

1. Preparar fixture conocido y registrar sus valores.
2. Iniciar grabación/captura y logs.
3. Ejecutar desde entrada real, no saltar directamente a una pantalla interna salvo test focal adicional.
4. Interactuar a velocidad humana y después repetir acciones sensibles rápidamente.
5. Confirmar cada transición visual y su efecto de datos.
6. Enviar app a background y volver en puntos de riesgo.
7. Ejecutar cold start posterior y verificar persistencia.
8. Repetir según riesgo y matriz.

No usar solo inspección estática de Preview como prueba de runtime.

## 5. Contrato visual MindBoost

Evaluar contra el PRD, sin rediseñar:

- Apariencia científica, premium, calmada y cósmica sutil.
- Dark-first con contraste legible.
- Sin estética infantil, saturación, ruido o recompensa superficial.
- Animaciones suaves que no bloquean interacción ni ocultan estado.
- Tipografía moderna, jerarquía clara y sin truncados críticos.
- Tarjetas/gráficas comprensibles, no decorativas.

### Dashboard: prueba de comprensión de cinco segundos

Una captura inicial debe permitir identificar sin scroll ni exploración innecesaria:

1. Estado/Global Score actual.
2. Qué entrenar ahora.
3. Progreso relevante.

Comprobar además rango/estado provisional, ocho bloques, CTA principal y acceso Elite sin que este domine el entrenamiento.

### Pantalla de ejercicio

- Bloque/habilidad/duración/dificultad claros.
- CTA de inicio inequívoco.
- Inputs responden una vez por acción.
- No hay feedback que revele una respuesta incorrectamente o altere métricas.
- Pérdida de red no tapa ni detiene el ejercicio.

### Resultado

- Se siente como informe de rendimiento, no premio vacío.
- Score/delta/precisión/velocidad/dificultad/consistencia coinciden con datos.
- Errores y siguiente recomendación son legibles.
- Estado de guardado/sync no induce a pensar que ya está cloud si solo está local.

## 6. Accesibilidad e interacción

Comprobar al menos:

- Semantics/labels/roles/estados para controles.
- Orden de foco lógico.
- Touch targets conformes a guías de plataforma o especificación del proyecto.
- Contraste y legibilidad en estados normal/disabled/error.
- Fuente grande sin solapamientos, clipping ni CTA inaccesible.
- Lectura de score/rango no dependiente solo del color.
- Teclado/IME no tapa acciones en Auth/chat.
- Back navigation no pierde sesión ni duplica acciones.
- Reduced motion si está soportado; ninguna animación esencial sin alternativa.
- Mensajes de error accionables y no humillantes.

Usar herramientas de accesibilidad disponibles, pero confirmar manualmente los hallazgos críticos en runtime.

## 7. Evidencia y puerta

Por dispositivo/configuración conservar:

- Modelo, OS/API, tamaño, densidad, font scale, locale y orientación.
- Hash del binario.
- Journey/instrucciones exactas.
- Grabación o secuencia de capturas con timestamps.
- Resultado de cada aserción.
- Capturas antes/después de reparaciones.
- Log correlacionado.

La puerta no pasa con capturas aisladas sin recorrido, ni con un recorrido que no verifica el efecto persistido. Cualquier pantalla inutilizable, dato visualmente falso, acción duplicada o bloqueo del entrenamiento produce `FAIL`.
