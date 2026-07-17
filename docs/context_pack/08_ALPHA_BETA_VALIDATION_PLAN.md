# MindBoost — Alpha, Beta & Validation Plan

**Versión:** 1.0.0  
**Fecha:** 2026-07-10  
**Owner:** CEO + Product + Science + QA  
**Estado:** APPROVED como estrategia; protocolos estadísticos concretos deben preregistrarse antes de recoger datos confirmatorios  
**Mercado inicial:** España, español, adultos 18+

---

## 1. Propósito

Este plan transforma una app técnicamente sólida en un producto confiable mediante evidencia humana progresiva.

Separa cuatro preguntas:

1. ¿Funciona sin romperse?
2. ¿Las personas entienden y pueden usarlo?
3. ¿Las métricas son fiables y válidas para el uso declarado?
4. ¿El entrenamiento produce mejoras útiles y transferibles?

Una respuesta positiva a una pregunta no responde automáticamente a la siguiente.

---

## 2. Principios

1. **Alpha no valida eficacia.** Con ~20 personas detecta problemas graves, no prueba transferencia general.
2. **Beta no autoriza claims automáticos.** Los análisis exploratorios generan hipótesis; los confirmatorios requieren protocolo.
3. **El score también se valida.** No basta con que el juego sea estable.
4. **La transferencia se mide fuera del ejercicio entrenado.**
5. **La muestra responde a potencia/precisión.** No se fija por intuición o por marketing.
6. **Versiones congeladas para comparar.** Cambios de fórmula/ítems crean cohortes o puentes.
7. **Adultos y consentimiento real.** Las vías de reclutamiento educativo no incluyen menores.
8. **Privacidad separada por finalidad.** Servicio, analytics e investigación no comparten consentimiento genérico.
9. **No dañar por optimizar retención.** Nada de culpa, engaño, score inflado o notificaciones coercitivas.
10. **Publicar incertidumbre.** Resultados nulos o negativos también mejoran el producto.

---

## 3. Fases

```text
PRE-ALPHA TÉCNICA
→ ALPHA PRIVADA (~20 adultos)
→ HARDENING
→ OPEN BETA EN STORES
→ CALIBRACIÓN / VALIDACIÓN
→ ESTUDIOS DE TRANSFERENCIA
→ RELEASE ESTABLE POR GATES
→ MONITORIZACIÓN CONTINUA
```

---

## 4. Población inicial

### 4.1 Inclusión

- 18 años o más.
- Residente/usuario del mercado español inicial.
- Comprensión funcional de español.
- Interés en entrenar su mente.
- Dispositivo compatible Android o iOS.
- Acepta consentimiento correspondiente.

### 4.2 Prioridad de segmentos

- Estudiantes adultos ambiciosos.
- Emprendedores.
- Personas adultas que desean mejorar concentración, estudio, trabajo o decisiones.

La mayoría puede partir de experiencia casi nula en entrenamiento cognitivo.

### 4.3 Exclusión/limitaciones

- Menores de 18 en alpha/beta inicial.
- Uso para diagnóstico o tratamiento.
- Personas que esperan una evaluación clínica.
- Participantes sin capacidad para consentir de acuerdo con el protocolo.

No recoger fecha exacta de nacimiento si basta una declaración/age gate, salvo obligación justificada.

### 4.4 Diversidad deliberada

Reclutar variedad de:

- Android/iOS y gama de dispositivo.
- Edad adulta.
- Género.
- Nivel educativo.
- Profesión/tipo de estudios.
- Habilidad digital.
- Disponibilidad diaria.
- Necesidades de accesibilidad relevantes.

No usar datos demográficos para estereotipar capacidad; se usan para detectar cobertura y sesgo.

---

## 5. Reclutamiento

Canales aprobados:

- Servidor de Discord de aproximadamente 240 personas.
- Universidades.
- Institutos u otros centros educativos, solo con participantes adultos.
- Centros de enseñanza y comunidades profesionales.

### 5.1 Mensaje de reclutamiento

Debe decir:

- Es una versión alpha/beta.
- Puede contener bugs.
- No es diagnóstico ni tratamiento.
- Qué datos son necesarios y cuáles opcionales.
- Tiempo esperado.
- Cómo abandonar y borrar datos.
- Qué beneficio/compensación existe; no prometer mejora.

### 5.2 Evitar sesgo de expectativas

No reclutar con “demuestra que tu cerebro mejorará”. Usar wording neutral:

> “Ayúdanos a evaluar la usabilidad, medición y utilidad de un sistema de entrenamiento cognitivo en desarrollo.”

---

## 6. Pre-alpha técnica

Antes de exponer la app a personas:

- P0/P1 técnicos cerrados.
- Secret scan y reglas default deny.
- Build Android/iOS instalable.
- Auth y borrado apropiados al entorno.
- Crash reporting sin datos sensibles.
- Catálogo navegable.
- 120 ejercicios superan `TRAINING_READY` o los no listos están ocultos.
- Every exercise automated smoke/golden.
- Ruta sin permisos sensibles para 8 bloques.
- Offline/reconnect scriptado.
- Consentimientos y contacto de soporte.
- Rollback/kill-switch.

No usar testers humanos como sustitutos de tests que la máquina puede ejecutar.

---

## 7. Alpha privada

### 7.1 Tamaño y propósito

Alrededor de 20 adultos.

Objetivos:

- Detectar bloqueos y pérdida de datos.
- Verificar comprensión y primera experiencia.
- Cubrir dispositivos/modalidades.
- Comprobar funcionamiento real de ejercicios.
- Detectar datos imposibles, techos/suelos gruesos y dificultad rota.
- Refinar feedback, accesibilidad y carga.
- Verificar soporte, export y borrado.

No objetivos:

- Validar los ocho constructos.
- Construir normas representativas.
- Probar causalmente transferencia.
- Publicar porcentiles o World Class.
- Optimizar agresivamente conversión.

### 7.2 Diseño recomendado

- Duración: 14–30 días según disponibilidad.
- Onboarding observado para una submuestra.
- Diario de bugs/fricción breve.
- Check-ins días 1, 7, 14 y cierre.
- Objetivo cotidiano definido en baseline.
- Uso natural + tareas de cobertura asignadas.
- Entrevista final semiestructurada.

### 7.3 Cobertura del catálogo

No exigir que cada persona juegue 120 ejercicios. Crear una matriz de asignación:

- Todos prueban onboarding, dashboard, plan y core flows.
- Cada ejercicio recibe smoke humano en más de una persona y, cuando sea posible, más de una clase de dispositivo.
- Ejercicios temporales, auditivos, motores o con sensores reciben cobertura reforzada.
- Las familias científicamente equivalentes se distribuyen para comparar comprensión y dificultad.

Los números exactos de exposiciones se deciden tras inventariar duración y riesgo; ninguna celda crítica queda en cero.

### 7.4 Métricas alpha

**Calidad técnica**

- Crash/hang/ANR.
- Sesión perdida o duplicada.
- Sync y recuperación offline.
- Jank/latencia en tareas sensibles.
- Fallo de recursos/audio/permisos.
- Consumo extremo de batería/datos.

**Usabilidad**

- Completa onboarding sin ayuda.
- Comprende tutorial a primer/reintento.
- Tiempo hasta primer valor.
- Abandono por ejercicio y motivo.
- Interpretación de score/confianza.
- Éxito en export/delete.

**Señal temprana**

- Distribuciones.
- Missingness.
- Techo/suelo.
- RT imposible.
- Discontinuidad por nivel/dispositivo.
- Estrategias superficiales.

**Percepción**

- Utilidad.
- Carga/fatiga.
- Confianza.
- Expectativas y malinterpretaciones.
- Mejora cotidiana percibida, etiquetada exploratoria.

### 7.5 Gates de salida alpha

Todos obligatorios:

- Cero P0 abiertos.
- Cero P1 abiertos para open beta.
- Ninguna pérdida/duplicación no explicada de sesión en casos canónicos.
- Auth temporal aislada y plan de migración listo; open beta bloqueada si sigue activa.
- Export/delete y consentimientos funcionan end-to-end.
- Cada ruta crítica puede completarse sin facilitador.
- Los ocho bloques tienen alternativa sin permisos sensibles.
- Catálogo tiene cobertura humana registrada por riesgo.
- Problemas de comprensión críticos corregidos y reprobados.
- Android/iOS y dispositivos mínimos verificados.
- Score/rangos siguen provisionales y World Class bloqueado.
- Privacy/security/qa-safety gate aprobado.

Las tasas numéricas se reportan con denominador e intervalo; una muestra pequeña no permite falsa precisión.

---

## 8. Hardening entre alpha y beta

- Priorizar causas, no solo síntomas.
- Congelar schemas/IDs necesarios para longitudinal.
- Migrar/eliminar auth DEV.
- Corregir catálogos y tutorials.
- Validar idempotencia y recovery.
- Clasificar cada ejercicio por estado de evidencia.
- Preparar dashboards de calidad.
- Definir protocolo, outcomes y análisis beta.
- Preregistrar estudios confirmatorios.
- Actualizar store copy sin claims no respaldados.

Todo cambio que altere métrica crea versión y decide comparabilidad.

---

## 9. Open beta en App Store y Google Play

### 9.1 Objetivos

- Probar operación a escala.
- Estimar fiabilidad y práctica.
- Calibrar ítems/familias y dificultad.
- Comprobar invariancia por dispositivo y subgrupos.
- Mejorar personalización y adherence ética.
- Medir outcomes cotidianos a 30 y 90 días.
- Estimar coste y valor de Elite.

### 9.2 Estados públicos

- App claramente identificada como beta.
- Scores/rangos provisionales cuando no cumplen charter.
- No ranking público oficial.
- `World Class` bloqueado.
- Claims limitados a entrenamiento y medición en desarrollo.
- Canal visible para reportar/borrar/exportar.

### 9.3 Cohortes

Separar por:

- App/scoring version.
- Fecha de onboarding.
- Plataforma/device class.
- Plan general/estudiante/emprendedor.
- Free/Elite para negocio, nunca para score.
- Consentimiento de investigación.
- Exposición a experimento.

No comparar cohortes incompatibles como si fueran aleatorias.

---

## 10. Tamaño muestral

### 10.1 Regla aprobada

El tamaño de beta y de cada análisis se determina por potencia o precisión antes de mirar el efecto.

### 10.2 Inputs del cálculo

- Outcome primario.
- Diferencia mínima relevante.
- Variabilidad esperada.
- Fiabilidad.
- Alpha/intervalo.
- Potencia objetivo.
- Número de brazos.
- Medidas repetidas.
- Attrition esperado.
- Multiplicidad.
- Clustering si existe.
- Subgrupos/DIF previstos.

### 10.3 Estrategia

1. Usar alpha/piloto para estimar varianza y factibilidad, sin inflar efecto.
2. Simular modelos de scoring/calibración.
3. Fijar tamaño y regla de parada.
4. Preregistrar.
5. Reclutar con margen por attrition.
6. No detener al primer resultado positivo.

### 10.4 Normas y World Class

Requieren una muestra mucho mayor y representativa por edad/educación/dispositivo relevantes, con invariancia y calidad. No reutilizar por comodidad una beta autoseleccionada como “el mundo”.

---

## 11. Programa psicométrico

### 11.1 Respuesta y comprensión

- Entrevistas cognitivas.
- Pensamiento en voz alta en submuestra.
- Análisis de errores/estrategias.
- Verificar que instrucciones se interpretan igual.

### 11.2 Análisis de ítems/familias

- Dificultad.
- Discriminación/información.
- Techo/suelo.
- Distractores.
- Tiempo y omisión.
- Exposición.
- Local dependence.
- Comportamiento por versión/dispositivo.

### 11.3 Fiabilidad

Según el uso:

- Split-half/consistencia cuando proceda.
- Test–retest con intervalo definido.
- ICC especificando modelo y unidad.
- SEM y cambio mínimo detectable.
- Información condicional si se usa IRT/MIRT.

La fiabilidad se evalúa por ejercicio, subhabilidad, bloque y uso; no existe un único número global que sanee todo.

### 11.4 Validez

- Evidencia de contenido.
- Proceso de respuesta.
- Estructura interna.
- Convergente con tareas externas apropiadas.
- Discriminante frente a constructos distintos.
- Criterio/funcional cuando exista.
- Consecuencias de interpretación.

### 11.5 Invariancia y equidad

- Android vs iOS.
- Clases de dispositivo.
- Modalidad/accesibilidad.
- Edad adulta y educación.
- Género y otros grupos solo con tamaño/ética adecuados.

Usar DIF/modelos equivalentes y revisar contenido. Diferencia observada no demuestra sesgo ni su causa; requiere investigación.

### 11.6 Práctica y formas

- Curvas por número de exposición.
- Formas paralelas.
- Equivalencia entre generadores/bancos.
- Estabilidad de dificultad.
- Separación de task learning y habilidad.

---

## 12. Validación de scoring y calibración

### 12.1 Requisitos

- No missing=0.
- No latest raw como perfil.
- Difficulty-aware.
- Error/cobertura/frescura.
- 5 sesiones válidas, 3 días y ≥48 h como mínimo de calibración inicial.
- Más evidencia si hay variabilidad.
- Score, Growth Index y XP separados.
- Global oficial solo con ocho bloques comparables.

### 12.2 Shadow scoring

Antes de activar fórmula nueva:

- Calcular versión antigua y candidata en paralelo.
- No mostrar candidata.
- Analizar saltos, rank flips, subgrupos y casos adversarios.
- Documentar puente o iniciar serie nueva.
- Aprobar ADR.

### 12.3 Sensibilidad

Simular:

- Usuario perfecto/lento.
- Usuario rápido/azar.
- Práctica intensiva.
- Ausencia prolongada.
- Cambio de dispositivo.
- Una sesión extrema.
- Datos duplicados/reordenados.
- Cambio de versión.

---

## 13. North Star de evidencia

### 13.1 Resultado estratégico

La North Star de producto debe reflejar transferencia útil, no minutos consumidos.

Nombre operativo recomendado:

> **30-day Valid Everyday Progress Rate (VEPR-30)**

Definición conceptual:

> Proporción de usuarios elegibles que, a 30 días, muestran mejora predefinida y suficientemente fiable en al menos una tarea cotidiana objetivo, con datos completos y sin deterioro material en guardrails.

### 13.2 Componentes

- Tarea cotidiana elegida al inicio.
- Indicador conductual o funcional repetible cuando sea viable.
- Auto-reporte neutral como complemento, no único dato si existe alternativa.
- Umbral mínimo relevante predefinido.
- Calidad/adherencia suficiente para interpretar.
- Resultado a 30 días y persistencia a 90.

### 13.3 Interpretación

- Es métrica de producto/resultado.
- No prueba causalidad sin comparador aleatorizado.
- No se optimiza inflando la pregunta de auto-reporte.
- Debe mostrarse junto con attrition y missingness.
- No sustituye análisis por persona, objetivo y plan.

### 13.4 Guardrails

- Bienestar/carga.
- Score integrity.
- Crash/sync.
- Privacidad/quejas.
- Uso compulsivo.
- Abandono.
- Brecha por dispositivo/grupo.
- Coste por usuario.

---

## 14. Estudio de 30 días

### 14.1 Pregunta

¿Los usuarios de MindBoost mejoran en una tarea cotidiana objetivo y en tareas nuevas relacionadas tras 30 días, más allá de práctica/expectativa?

### 14.2 Diseño escalonado

**Piloto exploratorio**

- Factibilidad de outcomes.
- Varianza y attrition.
- Comprensión del objetivo cotidiano.
- Sin claim causal.

**Estudio confirmatorio posterior**

- Aleatorización.
- Control activo creíble y equivalente en expectativa/tiempo cuando sea posible.
- Outcome primario preregistrado.
- Evaluador/algoritmo ciego a grupo cuando sea viable.
- Intention-to-treat + análisis de sensibilidad.
- Manejo predefinido de missingness.
- Corrección de multiplicidad.

### 14.3 Outcomes

Un primario, pocos secundarios:

- Primario cotidiano/funcional relevante.
- Near-transfer en tarea no entrenada.
- Rendimiento en estímulos nuevos.
- Autoconocimiento/calibración.
- Adherencia y hábito.

No convertir todos los scores en co-primarios.

### 14.4 Dosis

Registrar plan asignado, minutos, calidad y exposición. No definir adherencia después de ver quién mejoró.

---

## 15. Seguimiento a 90 días

Objetivos:

- Durabilidad del cambio.
- Hábito estable.
- Cambio de profesión/objetivo.
- Retención sin presión.
- Necesidad de mantenimiento.
- Pérdida de efecto tras inactividad.

No interpretar abandono como ausencia de efecto sin análisis de attrition.

---

## 16. Validación de planes ocupacionales

Planes iniciales:

- General.
- Estudiante.
- Emprendedor.

Validación:

1. Análisis de tareas reales.
2. Mapeo a subhabilidades.
3. Revisión experta.
4. Comprensión del usuario.
5. Adherencia y carga.
6. Near transfer.
7. Outcome cotidiano específico.

No validar “plan de emprendedor” solo porque emprendedores lo prefieren. Debe mejorar demandas definidas o aportar utilidad medible.

Ocupación desconocida usa plan general y tareas declaradas; no inventar perfil por título.

---

## 17. Experimentos de producto

### 17.1 Criterios

Cada experimento tiene:

- Hipótesis.
- Unidad de aleatorización.
- Métrica primaria.
- Guardrails.
- Tamaño/potencia.
- Regla de parada.
- Segmentos previstos.
- Versión.
- Decisión posterior.

### 17.2 Experimentos permitidos

- Tutorial y comprensión.
- Orden de onboarding.
- Presentación de feedback.
- Recordatorio opt-in.
- Dosis/plan determinista dentro de componentes validados.
- Visualización de incertidumbre.

### 17.3 Experimentos prohibidos o restringidos

- Inflar score para retener.
- Amenazar racha.
- Ocultar cancelación/borrado.
- Paywall que cambie capacidad estimada.
- Claims distintos por variante sin evidencia.
- Exponer medición oficial a contenido no calibrado.
- Enviar datos a IA sin consentimiento.

---

## 18. Elite en beta

### 18.1 Oferta

- Disponible en beta abierta.
- Precio inicial aprobado: 2 €/mes, ajustable.
- Solo suscriptores; no trial gratuito de IA.
- Free sigue siendo plenamente efectivo con plan determinista.

### 18.2 Métricas

- Activación real.
- Utilidad percibida.
- Planes aceptados/rechazados.
- Safety/schema failures.
- Latencia y disponibilidad.
- Tokens/coste por usuario.
- Margen unitario.
- Borrado/edición de memoria.
- Diferencia de adherence sin confundir con capacidad.

### 18.3 Gates de coste

- Entitlement server-side.
- Cuota dinámica.
- Circuit breaker.
- Fallback Free.
- Alerta antes de margen negativo.
- Ninguna clave en cliente.

---

## 19. Datos, consentimiento y derechos

### 19.1 Capas

1. Cuenta y servicio.
2. Seguridad/operación.
3. Analytics opcional.
4. Investigación opcional.
5. Elite AI.
6. Permisos de dispositivo.

### 19.2 Información al participante

- Finalidad.
- Datos.
- Retención.
- Destinatarios/proveedores.
- Riesgos y límites.
- Voluntariedad.
- Retirada.
- Export, corrección y borrado.
- Contacto.

### 19.3 Borrado

El protocolo y producto deben cubrir:

- Dispositivo/offline.
- Cloud principal.
- Cola de sync.
- Analytics.
- IA/memoria/prompts según política.
- Backups con plazo.
- Datos ya anonimizados de forma irreversible, explicando que ya no se pueden vincular.

Los plazos legales/operativos exactos se fijan en ADR y política antes de alpha; no usar “anonimizado” para datos solo pseudonimizados.

### 19.4 Investigación

- Consentimiento específico y revocable para uso futuro identificable.
- Dataset minimizado/pseudonimizado.
- Acceso restringido.
- Plan de análisis y retención.
- Publicación agregada.

---

## 20. Monitorización y dashboards

### 20.1 Calidad técnica

- Crash/hang/ANR.
- Sync fail/duplicate/loss.
- Latencia/jank.
- Auth/recovery failures.
- Export/delete failures.
- API Elite y coste.

### 20.2 Calidad de medición

- Elegibilidad/invalidation.
- Cobertura/calibración.
- Missingness.
- Techo/suelo.
- Variabilidad.
- Device effects.
- Exposición.
- Version mismatch.

### 20.3 Producto

- Time to first value.
- Plan started/completed.
- Adherencia elegida vs real.
- Rechazos de ejercicios.
- VEPR-30 y attrition.
- Retención D7/D30/D90 como guardrail, no objetivo único.

Los dashboards separan métricas necesarias de analytics opcional y aplican umbrales de privacidad.

---

## 21. Incidentes y reglas de parada

Pausar reclutamiento/feature si:

- Fuga o acceso cruzado.
- Pérdida/corrupción de historial.
- Score/rank falso generalizado.
- Crash crítico repetido.
- Consentimiento o borrado no funcionan.
- Claim engañoso expuesto.
- Daño o distress no anticipado.
- Coste Elite fuera de control.

Proceso:

1. Kill-switch.
2. Preservar evidencia de forma segura.
3. Evaluar alcance.
4. Corregir y migrar.
5. Comunicar cuando corresponda.
6. Revalidar antes de reabrir.

---

## 22. Gates de open beta

Antes de publicar en stores:

### Producto

- Onboarding, plan, sesión, dashboard, settings y soporte completos.
- 120 ejercicios `TRAINING_READY` o alcance público coherentemente reducido; el objetivo aprobado es 120.
- Free offline y sin IA.

### Tecnología

- Android 8/iOS15.
- Auth de release aprobada; ningún email sintético.
- Sync idempotente.
- Crash/performance gates fijados y superados.
- Rollback/kill-switch.

### Ciencia

- Estados de evidencia visibles.
- Provisional/oficial correctos.
- Calibración y score versionados.
- No ranking público oficial.
- No claims de transferencia sin estudio.

### Privacidad/seguridad

- Consentimiento por finalidad.
- Export/correct/delete.
- Reglas y acceso cruzado probados.
- Retención/backups/proveedores documentados.
- Store privacy disclosures coherentes.

### Operación

- Canal de soporte/incidentes.
- Dashboard mínimo.
- Capacidad de detener feature/version.
- Plan de migración.

Todos requieren evidencia y veredicto `APTO`.

---

## 23. Gates de release estable

No se fija una fecha; se exige:

- P0/P1 sostenidamente cerrados.
- Operación móvil estable en cohortes representativas.
- Auth/recuperación/privacidad probadas.
- Catálogo completo y paritario.
- Instrumentos con evidencia suficiente para sus labels.
- Fórmulas congeladas/versionadas y monitorizadas.
- Transferencia comunicada solo al nivel demostrado.
- North Star medida de forma válida.
- Sin desigualdad crítica no resuelta por dispositivo/accesibilidad.
- Elite con safety y unit economics positivos.
- Support/incident/release sostenible para un founder solo.
- Documentación y Decision Log al día.

El release estable puede mantener scores provisionales en bloques concretos si la UI lo declara y la función oficial queda bloqueada; no puede fingir validación global.

---

## 24. Comunicación de resultados

Todo informe interno/externo incluye:

- Versión.
- Población.
- Reclutamiento.
- N inicial/final y attrition.
- Outcome y análisis preespecificados.
- Efecto e incertidumbre.
- Missingness.
- Limitaciones.
- Cambios de protocolo.
- Resultados negativos.
- Conflictos/intereses.

No usar solo porcentajes relativos, p-values o testimonios.

### 24.1 Claims por etapa

**Alpha:** “Estamos probando funcionamiento y experiencia.”

**Beta sin estudio:** “Entrena habilidades mediante ejercicios adaptativos; medición en calibración.”

**Con evidencia psicométrica:** describir fiabilidad/uso exactos.

**Con estudio de transferencia:** describir población, dosis, outcome y magnitud exactos; no universalizar.

---

## 25. Artefactos obligatorios

- Alpha protocol y script.
- Consentimientos/versiones.
- Participant matrix anonimizada.
- Issue log P0–P3.
- Exercise coverage matrix.
- Device matrix.
- Statistical Analysis Plan.
- Preregistration IDs.
- Data dictionary.
- Scoring/model version registry.
- Experiment registry.
- Incident log.
- Alpha/beta close-out reports.
- Claim register.

---

## 26. Agent routing

Para diseñar/ejecutar esta etapa, además de `/mb`, nombrar según necesidad:

- `mindboost-experimentation`: diseño, potencia, preregistro y análisis.
- `mindboost-assessment-psychometrics`: fiabilidad, IRT/MIRT, error y DIF.
- `mindboost-cognitive-transfer-agent`: outcomes 30/90 días y control activo.
- `mindboost-data-analytics`: eventos, calidad y dashboards.
- `mindboost-qa-safety`: veredicto de release.
- `mindboost-security-privacy`: consentimiento, minimización y borrado.
- `mindboost-product-ux`: research y corrección de flujos.
- `mindboost-mobile-performance`: device matrix y timing.
- `mindboost-devops-release`: stores, rollback y observabilidad.
- `mindverse-legal-compliance`: revisión de textos/procesamiento cuando aplique.
- `mindverse-north-star-metrics`: validación de VEPR-30 y guardrails.

Cada agente recibe pregunta acotada; el lead integra.

---

## 27. Definition of Done de validación

Una etapa termina cuando:

- Respondió a sus preguntas predefinidas.
- Cumplió tamaño/regla de parada.
- Reportó attrition y missingness.
- No cambió outcomes después de ver resultados sin etiquetarlos exploratorios.
- Cerró P0/P1 o detuvo el avance.
- Versionó cambios derivados.
- Actualizó claims y estados de ejercicio.
- Protegió derechos y borrado.
- Publicó informe interno honesto.
- Definió siguiente gate.

---

## 28. Regla final

El propósito de la beta no es acumular usuarios para aparentar tracción. Es convertir incertidumbre en decisiones y demostrar, paso a paso, qué parte de MindBoost funciona, mide y transfiere.

