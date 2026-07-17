// Minimal RFC-4180 quoted-CSV parser → array of row objects keyed by header.
export function parseCsvRaw(text) {
  const rows = []; let row = []; let field = ''; let inQ = false;
  for (let i = 0; i < text.length; i++) {
    const ch = text[i];
    if (inQ) {
      if (ch === '"') { if (text[i + 1] === '"') { field += '"'; i++; } else inQ = false; }
      else field += ch;
    } else if (ch === '"') inQ = true;
    else if (ch === ',') { row.push(field); field = ''; }
    else if (ch === '\n') { row.push(field); rows.push(row); row = []; field = ''; }
    else if (ch === '\r') { /* skip */ }
    else field += ch;
  }
  if (field.length || row.length) { row.push(field); rows.push(row); }
  return rows.filter((r) => r.length > 1 || (r.length === 1 && r[0] !== ''));
}
export function parseCsv(text) {
  const t = parseCsvRaw(text);
  const header = t[0];
  return t.slice(1).map((r) => Object.fromEntries(header.map((h, i) => [h, r[i] ?? ''])));
}
export function headerOf(text) { return parseCsvRaw(text)[0]; }
const q = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`;
// Serialize objects back to CSV using the given column order (RFC-4180 quoting; shared with the seed).
export function toCsv(objs, columns) {
  return [columns.join(',')].concat(objs.map((o) => columns.map((c) => q(o[c])).join(','))).join('\n') + '\n';
}
