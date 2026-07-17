# MindBoost Antigravity QA

Sistema cerrado de verificación y reparación para MindBoost. Expone una sola entrada operativa: `/qa`.

## Instalación

1. Extraer el contenido de este ZIP directamente en la raíz del repositorio de MindBoost.
2. Confirmar que existe `<raíz>/.agents/workflows/qa.md`.
3. Abrir esa raíz como workspace en Google Antigravity IDE o Antigravity.
4. Seleccionar un modelo Gemini con razonamiento alto. Usar `Gemini 3.5 Flash (High)` cuando esté disponible, o el Gemini de nivel High más reciente.
5. Abrir una conversación nueva del workspace y ejecutar exactamente:

```text
/qa
```

No instalar estos archivos como reglas globales. No mover el contenido de `.agents/qa-system/` a `.agents/skills/`: hacerlo crearía superficies de activación adicionales y rompería la garantía de que `/qa` sea la única entrada.

## Contrato de funcionamiento

- `/qa` descubre automáticamente el último cambio funcional entregado por Claude mediante el historial Git, el último informe QA y, si existe, un handoff reciente.
- Gemini compila, prueba, despliega, inspecciona, fuerza escenarios offline, revisa scoring, observa fallos y corrige únicamente regresiones no arquitectónicas.
- El resultado superior siempre es `PASS` o `FAIL`.
- La ausencia de evidencia, herramientas, credenciales, dispositivo o plataforma necesarios produce `FAIL`; nunca un aprobado por suposición.
- El sistema no llama a Claude, Codex ni a otro LLM. Los subagentes internos, si Antigravity los crea, deben usar Gemini y trabajar en modo de solo lectura. Solo el coordinador puede modificar el repositorio.
- El sistema no despliega a producción, no publica en tiendas, no cambia datos reales y no ejecuta operaciones destructivas.

## Salidas

Cada ejecución crea un directorio inmutable bajo:

```text
.qa/runs/<UTC>_<commit>/
```

El resultado principal queda en `QA_REPORT.md`, acompañado por un índice de evidencia, comandos, logs, resultados de pruebas, capturas, mediciones y el diff exacto de las correcciones.

## Alcance real

Este sistema reduce de forma agresiva el margen de error, pero ningún proceso de software puede demostrar ausencia absoluta de defectos. Su defensa contra la falsa confianza es deliberadamente estricta: solo concede `PASS` cuando todas las puertas aplicables se ejecutaron y dejaron evidencia verificable.
