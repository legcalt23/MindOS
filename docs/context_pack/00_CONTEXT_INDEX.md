# MindBoost Context Pack — Índice maestro y reglas de uso

**Versión del paquete (packRelease):** 2.0.2  
**Fecha:** 2026-07-10  
**Owner:** New Era UNIVERSE  
**Producto:** MindBoost  
**Estado:** Fuente de verdad aprobada por CEO  
**Idioma operativo inicial:** Español (España)

---

## 1. Propósito

Este paquete proporciona a cualquier IA el contexto necesario para diseñar, implementar, auditar y mejorar MindBoost sin depender de supuestos genéricos.

MindBoost aspira a convertirse en el entrenador cognitivo de referencia mundial para maximizar el progreso cognitivo **medible** del usuario promedio. La ambición del producto no autoriza exageraciones: cada score, claim, recomendación y rango debe reflejar el nivel real de evidencia disponible.

El paquete separa deliberadamente:

- Qué producto se construye.
- Cómo se decide.
- Qué significa medir honestamente.
- Cómo se aceptan ejercicios.
- Cuál es el estado técnico real.
- Cómo trabajan Claude, `/mb`, los agentes y ChatGPT.
- Cómo se revisa una fase.
- Cómo se valida con personas reales.
- Qué decisiones ejecutivas ya están cerradas.

No debe comprimirse todo en un único PRD.

---

## 2. Manifiesto de documentos

| Orden | Documento | Autoridad y propósito |
|---:|---|---|
| 00 | `00_CONTEXT_INDEX.md` | Índice, precedencia, rutas de lectura y contrato de contexto. |
| 01 | `01_MINDBOOST_PRODUCT_PRD.md` | Producto, usuarios, experiencia, alcance, negocio y aceptación. |
| 02 | `02_CEO_COPILOT_CHARTER.md` | Cómo debe actuar ChatGPT como segundo cerebro ejecutivo del CEO. |
| 03 | `03_SCIENTIFIC_MEASUREMENT_CHARTER.md` | Validez, scoring, calibración, transferencia, confianza y claims. |
| 04 | `04_EXERCISE_QUALITY_STANDARD.md` | Diseño, implementación, métricas y gates de los ejercicios. |
| 05 | `05_TECHNICAL_CURRENT_STATE.md` | Realidad técnica conocida, límites, arquitectura objetivo y unknowns. |
| 06 | `06_AI_DEVELOPMENT_OPERATING_SYSTEM.md` | `/mb`, equipos de agentes, orden de ejecución y gobierno del desarrollo. |
| 07 | `07_PHASE_REVIEW_PROTOCOL.md` | Auditoría post-fase de ChatGPT y generación del prompt para Claude. |
| 08 | `08_ALPHA_BETA_VALIDATION_PLAN.md` | Alpha, beta, evidencia humana, análisis y gates de publicación. |
| 09 | `09_DECISION_LOG.md` | Decisiones aprobadas, cambios respecto al PRD 1.0 y ADR pendientes. |

Todos los documentos forman una única fuente de verdad distribuida. Ningún archivo debe interpretarse aislado cuando la tarea afecte a más de un dominio.

---

## 3. Orden de precedencia

Cuando dos instrucciones parezcan incompatibles, aplicar este orden:

1. Ley aplicable, seguridad, privacidad y protección del usuario.
2. Fuente de verdad y objetivo supremo del CEO.
3. `03_SCIENTIFIC_MEASUREMENT_CHARTER.md` para afirmaciones de validez, scoring y eficacia.
4. `01_MINDBOOST_PRODUCT_PRD.md` para requisitos de producto.
5. ADR y decisiones vigentes de `09_DECISION_LOG.md`.
6. Plan maestro y contrato de la slice activa.
7. Implementación actual.

Reglas adicionales:

- `06_AI_DEVELOPMENT_OPERATING_SYSTEM.md` gobierna **cómo** se trabaja; no puede reducir la validez exigida al producto.
- `04_EXERCISE_QUALITY_STANDARD.md` gobierna la admisión de ejercicios.
- `07_PHASE_REVIEW_PROTOCOL.md` gobierna el cierre de fases.
- El código no se convierte en requisito solo porque ya exista.
- Una decisión posterior solo sustituye a otra si queda registrada explícitamente como `SUPERSEDED`.

---

## 4. Rutas mínimas de lectura por tarea

### 4.1 Implementar o modificar una funcionalidad

Leer, en este orden:

1. Este índice.
2. PRD.
3. Technical Current State.
4. AI Development Operating System.
5. Sección de producto afectada.
6. ADR/Decision Log relevante.
7. Charter científico si genera o consume métricas.

### 4.2 Crear, modificar o validar un ejercicio

Leer:

1. PRD.
2. Scientific Measurement Charter.
3. Exercise Quality Standard.
4. Alpha/Beta Validation Plan.
5. Technical Current State.

### 4.3 Cambiar scoring, calibración, perfil o ranking

Leer obligatoriamente:

1. CEO Copilot Charter.
2. Scientific Measurement Charter completo.
3. PRD §§ scoring, experiencia y claims.
4. Phase Review Protocol.
5. Decision Log y ADR del estimador vigente.

### 4.4 Trabajar con IA Coach o Elite

Leer:

1. PRD §§ Gratis, Elite e IA Coach.
2. Scientific Measurement Charter.
3. Technical Current State.
4. Privacy y unit economics del Decision Log.

### 4.5 Auditar una actualización de Claude

ChatGPT debe leer:

1. CEO Copilot Charter.
2. Phase Review Protocol.
3. AI Development Operating System.
4. Documentos de dominio afectados.
5. Resumen final de Claude y todos los archivos adjuntos.

---

## 5. Contrato de arranque para cualquier IA

Antes de responder o modificar código:

1. Identifica el objetivo de la tarea y los documentos relevantes.
2. Distingue requisitos cerrados de hipótesis y ADR pendientes.
3. No asumas que un build verde valida una fórmula o un ejercicio.
4. No confundas rendimiento en una tarea con transferencia cognitiva.
5. No conviertas ausencia de datos en score cero.
6. No mezcles puntuaciones de escalas/versiones no comparables.
7. No uses claims médicos, diagnósticos ni promesas garantizadas.
8. No introduzcas servicios de pago durante la etapa de presupuesto 0 €, salvo que estén cubiertos directamente por ingresos de una función pagada.
9. Conserva los cambios ajenos y exige migración segura para reescrituras.
10. Si falta evidencia, declara la incertidumbre y falla de forma conservadora.

---

## 6. Resumen ejecutivo de decisiones innegociables

- Producto independiente para iOS y Android.
- Lanzamiento inicial en España, en español, solo para mayores de 18 años.
- Perfiles primarios: estudiantes ambiciosos y emprendedores.
- Núcleo gratuito, sin anuncios y entrenable offline.
- Presupuesto adicional de desarrollo: 0 €.
- Calidad por encima de fecha.
- Ocho motores visibles: Enfoque, Decisiones, Lógica, Creatividad, Lenguaje, Emociones, Velocidad mental y Memoria.
- Al menos 15 ejercicios excelentes por cada motor: mínimo 120.
- Entrenamiento y medición son vías distintas.
- Cada bloque muestra score compuesto y subhabilidades.
- `Performance Score` y `Growth Index` son indicadores separados.
- El score puede bajar; no se maquilla el rendimiento.
- Calibración inicial: al menos 5 evidencias oficiales, 3 días y 48 horas entre primera y última; más si la señal lo exige.
- Rango oficial solo con validez suficiente; `World Class` permanece bloqueado sin normas representativas.
- Ranking público desactivado hasta validar scoring y anti-trampa.
- Plan gratuito determinista y personalizado; sin IA generativa.
- IA Coach solo para Elite, mediante API externa y con coste cubierto por suscripción.
- Precio inicial de Elite: 2 €/mes, ajustable según unit economics.
- Datos de sueño, estrés, energía y fatiga permanecen localmente y no alteran el score oficial.
- Cuenta final mediante usuario/contraseña en VPS propio; email/teléfono opcionales.
- Durante desarrollo/alpha puede existir login Firebase con correo sintético, bloqueado en release.
- Alpha privada aproximada de 20 personas; beta abierta en App Store y Google Play.
- ChatGPT audita después de cada fase y puede cambiar decisiones con confianza muy alta.
- Claude posee acceso completo al repositorio y ejecuta mediante plan maestro y `/mb`.

---

## 7. Semántica de estados documentales

| Estado | Significado |
|---|---|
| `APPROVED` | Decisión ratificada y aplicable. |
| `HYPOTHESIS` | Supuesto que debe medirse; no es claim. |
| `ADR_REQUIRED` | La IA puede decidir, pero debe documentar alternativas y migración. |
| `UNVERIFIED` | Existe una afirmación, no evidencia independiente suficiente. |
| `BLOCKED` | No puede cablearse o publicarse. |
| `SUPERSEDED` | Sustituido por una decisión posterior identificada. |

---

## 8. Control de cambios

Cada actualización del paquete debe:

1. Incrementar versión del documento afectado.
2. Registrar fecha y razón.
3. Añadir o modificar entrada del Decision Log.
4. Identificar requisitos y tests afectados.
5. Explicar migración de datos o scores si aplica.
6. Mantener enlaces y terminología consistentes.

No se permite modificar silenciosamente un umbral científico o una promesa al usuario.

### 8.1 Registro de releases del paquete

`packRelease` (versión del paquete) es distinto de `documentVersion` (versión propia de cada documento). Elevar `packRelease` no reescribe la versión individual de un documento que no cambió.

- **2.0.0 (2026-07-10):** release inicial aprobado por CEO.
- **2.0.2 (2026-07-17):** reconciliación de canon (adjudicación ChatGPT, D-H1). `docs/context_pack` queda como único canon vivo y trazable en git; `docs/context-pack` pasa a `SUPERSEDED_SNAPSHOT_DO_NOT_USE` (permanece fuera de HEAD, en quarantine, como snapshot histórico). Incorpora las correcciones ya presentes en `03`, `05` y `09`, incluida la corrección de atribución PMID 6R3-P2-05 (35953659 = Nicosia — latencia BYOD; 33954913 = Passell — variación de puntuaciones por dispositivo). Detalle en `09_DECISION_LOG.md` y `README.md`.

---

## 9. Terminología canónica

- **Bloque / motor:** categoría visible de entrenamiento.
- **Subhabilidad:** componente medible dentro de un bloque.
- **Entrenamiento:** experiencia optimizada para practicar y aprender.
- **Medición:** protocolo controlado para estimar rendimiento.
- **Performance Score:** estimación del rendimiento actual dentro de MindBoost.
- **Growth Index:** evolución personal respecto a baseline y periodo reciente.
- **Evidencia oficial:** sesión/probe apto para perfil/rango.
- **Cobertura:** cantidad y variedad de evidencia disponible.
- **Confianza/uncertainty:** calidad de la estimación; no sinónimo de estabilidad.
- **Transferencia cercana:** mejora en tareas diferentes del mismo constructo.
- **Transferencia cotidiana:** posible cambio funcional fuera de MindBoost.
- **Plan gratuito:** reglas y programas preconstruidos, sin IA generativa.
- **Elite:** capa de comprensión y optimización mediante IA Coach.

---

## 10. Instrucción breve para adjuntar este paquete

> Lee `00_CONTEXT_INDEX.md` y sigue la ruta de documentos correspondiente a la tarea. Trata el paquete como fuente de verdad distribuida. No rebajes honestidad científica, privacidad o seguridad por velocidad. Distingue hechos, hipótesis y afirmaciones no verificadas. Si trabajas en código, usa el plan maestro y el sistema `/mb`. Si revisas una fase de Claude, aplica íntegramente `07_PHASE_REVIEW_PROTOCOL.md` y devuelve un prompt ejecutable con routing explícito de agentes.
