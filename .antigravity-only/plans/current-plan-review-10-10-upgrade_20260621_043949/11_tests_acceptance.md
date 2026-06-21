# Tests y Acceptance Criteria (10/10 Hard Gates)

**1. Cost Budget & Evaluator Cascade**
* Simulación de 10.000 sesiones sin superar coste máximo definido por usuario/sesión.
* El backend corta evaluación al alcanzar budget.
* Métrica de coste por sesión visible en dashboard.

**2. Metric Integrity Suite (200+ ataques adversarios)**
* 95%+ bloqueo/degradación contra diccionarios, palabras aleatorias, sinónimos repetidos, prompt injection, copypaste Wikipedia.
* <5% Falsos positivos en usuarios normales.

**3. Data Leakage & Privacy**
* Canarios PII nunca aparecen en logs.
* Test con emails/teléfonos/claves redactado al 100% antes del evaluator.
* Logs con IDs y hashes, sin raw content.

**4. Coaching Safety**
* 500 ejemplos evaluados: 0 mensajes humillantes, 0 diagnósticos.

**5. Friction Budget**
* A/B tests en thresholds de abandono D1/D7.
