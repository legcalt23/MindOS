#!/usr/bin/env node
// freeze_v5.cjs — manifiesto SHA-256 PORTABLE del bundle V5 + checks mecánicos (P1-3, P3-25).
// Uso: node tools/freeze_v5.cjs   (ejecutado DESDE la raíz del bundle; solo rutas RELATIVAS)
// Política de line endings: no se convierte nada; el manifiesto DECLARA el EOL real de cada archivo
// (LF/CRLF/MIXED/NONE) y el hash es de los bytes exactos. Reproducible en cualquier SO con Node.
const fs = require("fs"), path = require("path"), crypto = require("crypto");
const ROOT = process.cwd();
const SKIP = new Set(["SHA256_MANIFEST_V5.txt"]); // el manifiesto no se auto-incluye

function walk(dir) {
  let out = [];
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) out = out.concat(walk(p));
    else out.push(p);
  }
  return out;
}
function eolOf(buf) {
  const s = buf.toString("latin1");
  const crlf = (s.match(/\r\n/g) || []).length;
  const lf = (s.match(/(?<!\r)\n/g) || []).length;
  if (crlf && lf) return "MIXED";
  if (crlf) return "CRLF";
  if (lf) return "LF";
  return "NONE";
}
const files = walk(ROOT)
  .map(p => path.relative(ROOT, p).split(path.sep).join("/"))
  .filter(p => !SKIP.has(p))
  .sort();

// ---- checks mecánicos ----
let failures = [];
const mdFiles = files.filter(f => f.endsWith(".md"));
// tokens de gate: T- precedido de no-alfanumérico (evita CAT-003/OUT-002/RT-CV) y ≥2 chars tras el guión;
// "T-Sn"/"T-SCI-n" genéricos y agent_outputs/ (registros verbatim, no normativos) quedan fuera del check.
const GATE_RX = /(?<![A-Za-z0-9])T-[A-Z0-9]{2,}(?:-[A-Z0-9]+)?/g;
const GATE_ALLOW = new Set(["T-Sn", "T-SCI-n"]);
const gateCat = fs.readFileSync(path.join(ROOT, "GATE_CATALOG_V5.md"), "utf8");
const definedT = new Set((gateCat.match(GATE_RX) || []));
for (const f of mdFiles) {
  const txt = fs.readFileSync(path.join(ROOT, f), "utf8");
  // 1) paridad de fences
  const fences = (txt.match(/^```/gm) || []).length;
  if (fences % 2 !== 0) failures.push(`FENCE_IMPAR: ${f} (${fences} fences)`);
  // 2) tokens de gate T-* usados pero no definidos en el catálogo (solo artefactos normativos)
  if (!f.startsWith("agent_outputs/")) {
    for (const tok of new Set(txt.match(GATE_RX) || [])) {
      if (!definedT.has(tok) && !GATE_ALLOW.has(tok)) failures.push(`GATE_HUERFANO: ${tok} en ${f}`);
    }
  }
  // 3) referencias a archivos del bundle que no existen
  for (const ref of new Set(txt.match(/[A-Za-z0-9_]+_V5\.(md|csv|txt)/g) || [])) {
    if (ref === "SHA256_MANIFEST_V5.txt") continue; // se genera en este mismo freeze
    if (!files.includes(ref) && !files.includes("tools/" + ref)) failures.push(`REF_AUSENTE: ${ref} citado en ${f}`);
  }
}
// 4) CSV: tokens G-* legacy residuales
const csv = fs.readFileSync(path.join(ROOT, "REQUIREMENTS_MATRIX_V5.csv"), "utf8");
if (/G-(A\b|DR|S0|S2|V[123]|SCI)/.test(csv)) failures.push("CSV: tokens G-* legacy residuales");

// ---- manifiesto ----
const lines = ["# SHA256_MANIFEST_V5 — bundle FOCUS_ENGINE_MASTER_PLAN_V5 (rutas relativas; hash de bytes exactos)",
  "# Regenerar: node tools/freeze_v5.cjs (desde la raíz del bundle). EOL declarado, no convertido.",
  "# formato: sha256  bytes  eol  ruta"];
for (const f of files) {
  const buf = fs.readFileSync(path.join(ROOT, f));
  const h = crypto.createHash("sha256").update(buf).digest("hex");
  lines.push(`${h}  ${String(buf.length).padStart(8)}  ${eolOf(buf).padEnd(5)}  ${f}`);
}
fs.writeFileSync(path.join(ROOT, "SHA256_MANIFEST_V5.txt"), lines.join("\n") + "\n");

console.log(`Manifiesto: ${files.length} archivos.`);
if (failures.length) { console.log("CHECKS FALLIDOS:"); failures.forEach(f => console.log("  - " + f)); process.exit(1); }
console.log("Checks mecánicos: OK (fences pares, gates T-* definidos, refs presentes, CSV sin G-*)");
