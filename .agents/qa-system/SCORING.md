# Contrato QA de scoring, calibración y rangos

## Índice

1. Autoridad
2. Fórmulas MVP
3. Invariantes
4. Vectores de referencia
5. Propiedades y bordes
6. Calibración/rangos
7. Versionado/migración
8. Puerta de scoring

## 1. Autoridad

QA verifica el scoring aprobado; no lo diseña. Antes de probar, localizar implementación, documentación versionada y tests existentes. Si faltan decisiones necesarias —por ejemplo, umbral exacto del guard de velocidad o mapping score→rango— no inventarlas: `FAIL — SCORING_SPEC_INCOMPLETE`.

Mantener separados:

- Valor crudo por ejercicio.
- Componentes normalizados.
- Score de sesión antes/después de guards.
- Score histórico de bloque.
- Score de calibración.
- Score global.
- Representación redondeada de UI.

## 2. Fórmulas MVP

```text
BlockSessionScore =
  0.30 * AccuracyScore +
  0.20 * SpeedScore +
  0.20 * DifficultyScore +
  0.15 * ConsistencyScore +
  0.10 * ProgressScore +
  0.05 * ErrorControlScore

NewBlockScore =
  0.85 * PreviousBlockScore +
  0.15 * BlockSessionScore

CalibrationBlockScore = average(last 5 valid session scores)

GlobalScore = average(scores of the 8 cognitive blocks)
```

El guard aprobado de velocidad/precisión se aplica en el punto definido por la arquitectura. La suma ponderada por sí sola no satisface la regla “velocidad sin precisión no se premia”. No crear un umbral dentro de QA.

## 3. Invariantes

Para toda entrada aceptada:

1. Todos los componentes y resultados son finitos: nunca `NaN`, `Infinity` o overflow.
2. Scores persistidos permanecen en `[0, 100]`.
3. Mismas entradas + misma versión + mismo reloj/seed producen mismo resultado.
4. El redondeo de UI no altera el valor persistido.
5. Una respuesta omitida no cuenta simultáneamente como correcta.
6. `correct + incorrect + omissions` respeta el contrato de `totalAnswers`.
7. Duración real, tiempos de reacción y variabilidad no pueden ser negativos.
8. Una sesión abandonada no calibra ni actualiza score como completada.
9. Una sesión menor de 120 s no cuenta para calibración.
10. Una sesión con anomalía extrema no cuenta si el contrato la invalida.
11. Reprocesar la misma `sessionId` no aplica dos veces el delta.
12. Orden de sincronización no cambia el resultado final cuando el conjunto/versiones es el mismo.
13. Aumentar precisión manteniendo señales válidas iguales no reduce el score crudo.
14. Aumentar velocidad con precisión dentro del rango permitido no reduce el score crudo.
15. Empeorar precisión hasta violar el guard no puede mejorar el score final por velocidad.
16. Una única sesión puede mover `NewBlockScore` como máximo 15 puntos por la fórmula MVP antes de otros guards aprobados.
17. El Global Score usa los ocho bloques; la elegibilidad del rango global es una decisión separada.
18. Ninguna UI muestra rango oficial antes de cumplir calibración.

## 4. Vectores de referencia

Probar al menos estos vectores sobre la suma ponderada **antes** de guards adicionales, con tolerancia numérica explícita compatible con el tipo usado:

| Accuracy | Speed | Difficulty | Consistency | Progress | ErrorControl | Resultado |
|---:|---:|---:|---:|---:|---:|---:|
| 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 100 | 100 | 100 | 100 | 100 | 100 | 100 |
| 100 | 0 | 50 | 100 | 0 | 100 | 60 |
| 50 | 50 | 50 | 50 | 50 | 50 | 50 |

Otros vectores:

- `Previous=40`, `Session=60` → `NewBlock=43`.
- `Previous=100`, `Session=0` → `NewBlock=85`.
- Sesiones válidas `[20,40,60,80,100]` → calibración `60`.
- Ocho bloques `[10,20,30,40,50,60,70,80]` → global `45`.

Probar aparte entradas como Accuracy `0`, Speed `100`: el resultado final debe cumplir el guard aprobado, no simplemente aceptar el `70` que producirían otros componentes a 100. Si el comportamiento esperado no está cuantificado, abrir decisión; no codificar una opinión.

## 5. Propiedades y bordes

Generar casos deterministas y property-based cuando la librería del proyecto lo permita:

- Componentes en `0`, `100`, justo dentro/fuera del rango, fracciones y valores inválidos.
- Cero respuestas, una respuesta y grandes cantidades.
- Duración cero, 119.999 s, 120 s y límites de cada duración.
- Tiempos iguales, variabilidad cero, outliers y orden alterado.
- Todos correctos, todos incorrectos, todas omisiones y combinaciones.
- Locale con coma/punto decimal; UI y persistencia no dependen del locale.
- Cambio de zona horaria, DST y reloj alrededor de medianoche.
- Sesiones fuera de orden, duplicadas y concurrentes.
- Reapertura/reproceso después de crash entre cálculo local y escritura cloud.
- Dificultad mínima/máxima y transición exacta de nivel.

Comprobar precisión de coma flotante y política de rounding. No comparar strings formateados para validar lógica.

## 6. Calibración y rangos

Validar por bloque:

- 0–4 sesiones válidas → provisional, sin rango oficial.
- Quinta válida → calibración exactamente una vez.
- Sesión abandonada, corta o anómala no incrementa contador válido.
- Sexta y posteriores actualizan historial sin recalibraciones duplicadas.
- Cinco sesiones válidas mezcladas con inválidas usan las últimas cinco válidas según contrato.
- UI, perfil local y Firestore coinciden.

Global:

- Score global = media simple de ocho scores.
- Menos de cinco bloques calibrados → perfil global provisional.
- Quinto bloque calibrado → elegible para rango global, según mapping aprobado.

Rangos:

- Probar cada frontera exacta `justo debajo / exacta / justo encima` a partir de la tabla versionada real.
- Probar divisiones III→II→I y transición al rango siguiente.
- `Leyenda — World Class` es único en MVP.
- Si no existe tabla score→rango aprobada, no aceptar una inventada en código.

Inactividad:

- Usar fake clock.
- Justo antes de un mes no baja.
- En el instante contractual baja un rango, no más.
- Entrenar/recalibrar sigue la política aprobada.
- El texto no afirma pérdida total de progreso.

## 7. Versionado y migración

Cada sesión conserva `exerciseVersion` y `scoringVersion`. Verificar:

- Una versión ausente no se interpreta silenciosamente como la más reciente.
- Recalcular históricos requiere una migración explícita y trazable.
- Datos antiguos siguen legibles o fallan de forma controlada.
- Local/cloud resuelven conflictos sin mezclar semánticas.
- Cambiar fórmula exige fecha, motivo, impacto, compatibilidad y migración aprobados.

QA no crea una migración de scoring sin decisión de Claude/owner.

## 8. Puerta de scoring

`PASS` requiere:

- Fórmula y guards coincidentes con fuente aprobada.
- Vectores y propiedades ejecutados.
- Cero valores no finitos/fuera de rango.
- Calibración y rango correctos en persistencia y UI.
- Idempotencia ante duplicado/reintento.
- Versionado preservado.
- Evidencia automatizada más al menos un recorrido real completo de sesión→resultado→reapertura.
