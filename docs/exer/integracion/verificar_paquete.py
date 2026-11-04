"""Verificación local de integridad, entrada y enlaces; no evalúa la conducta del modelo."""
import argparse
import hashlib
import json
import re
from pathlib import Path
from urllib.parse import unquote


def verify(root):
    manifest = json.loads((root / "integracion/MANIFIESTO_LECTURA.json").read_text(encoding="utf-8"))
    errors = []
    files = manifest["files"]
    if len(files) != 45 or len({f["path"] for f in files}) != 45:
        errors.append("El manifiesto debe identificar 45 miembros únicos del ZIP")
    for entry in files:
        path = root / entry["path"]
        if not path.is_file():
            errors.append(f"Falta original: {entry['path']}")
            continue
        data = path.read_bytes()
        if len(data) != entry["bytes"] or hashlib.sha256(data).hexdigest() != entry["sha256"]:
            errors.append(f"Original alterado: {entry['path']}")
    skill = (root / "SKILL.md").read_text(encoding="utf-8")
    if not re.match(r"\A---\r?\nname: exer\r?\ndescription: .+?\r?\n---", skill):
        errors.append("Frontmatter de la entrada inválido")
    checked_links = 0
    historical = []
    for path in sorted(root.rglob("*.md")):
        rel = path.relative_to(root).as_posix()
        text = path.read_text(encoding="utf-8")
        if rel.startswith("contexto/fuentes/"):
            historical.append(rel)
            continue  # Originales históricos pueden citar materiales no incluidos; no reescribirlos.
        # No interpretar ejemplos de código como enlaces operativos.
        fences = re.findall(r"^\s*(`{3,}|~{3,})", text, re.MULTILINE)
        if len(fences) % 2:
            errors.append(f"Fence sin pareja: {rel}")
        text = re.sub(r"^\s*(`{3,}|~{3,}).*?^\s*\1\s*$", "", text, flags=re.MULTILINE | re.DOTALL)
        for match in re.finditer(r"\[[^\]\n]+\]\(([^)\n]+)\)", text):
            dest = match.group(1).strip().strip("<>")
            if re.match(r"^[a-zA-Z][a-zA-Z0-9+.-]*:", dest) or dest.startswith("#"):
                continue
            dest = unquote(dest.split("#", 1)[0])
            if dest:
                checked_links += 1
                if not (path.parent / dest).exists():
                    errors.append(f"Enlace roto: {rel} -> {dest}")
    return {
        "result": "FAIL" if errors else "PASS",
        "original_files_checked": len(files),
        "relative_links_checked": checked_links,
        "historical_sources_preserved_not_link_validated": historical,
        "errors": errors,
        "limits": ["No prueba invocación del cliente", "No prueba conducta del modelo", "No ejecuta la app", "No valida URLs externas ni anchors Markdown"],
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    result = verify(args.root.resolve())
    print(json.dumps(result, ensure_ascii=False, indent=2))
    raise SystemExit(bool(result["errors"]))
