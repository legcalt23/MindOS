#!/usr/bin/env node
// Conservative Kotlin CODE-POSITION scanner (V2d Phase 2).
//
// WHY: the V2c extractor substring-probed raw file text, so an id mentioned in a KDoc, a comment, a
// string literal or a doc block counted exactly like a real declaration (MB-PORTFOLIO-0013). This strips
// non-code regions first, so a token only counts when it appears in CODE position.
//
// WHAT THIS IS NOT: this is NOT a Kotlin parser and NOT a symbol resolver. It cannot tell a declaration
// from a reference, cannot resolve imports, and cannot follow indirection. It answers exactly one narrow
// question — "does this identifier appear as a code token in this text?" — and every caller that needs
// more than that MUST degrade to UNRESOLVED rather than invent precision. See PORTFOLIO_AUTHORITY_MAP.md.
export const STRATEGY_VERSION = '2.0.0';

// Replaces comments/strings/chars with equal-length blanks (offsets preserved for future line mapping).
// Handles: nested /* */, //, raw """ """ (incl. embedded quotes), normal "..." with escapes, '...' chars.
export function stripNonCode(src) {
  const n = src.length;
  const out = new Array(n);
  for (let i = 0; i < n; i++) out[i] = src[i];
  const blank = (a, b) => { for (let k = a; k < b && k < n; k++) if (out[k] !== '\n') out[k] = ' '; };
  let i = 0;
  while (i < n) {
    const c = src[i], d = src[i + 1];
    if (c === '/' && d === '*') {                       // block comment, nestable in Kotlin
      let depth = 1, j = i + 2;
      while (j < n && depth > 0) {
        if (src[j] === '/' && src[j + 1] === '*') { depth++; j += 2; continue; }
        if (src[j] === '*' && src[j + 1] === '/') { depth--; j += 2; continue; }
        j++;
      }
      blank(i, j); i = j; continue;
    }
    if (c === '/' && d === '/') {                       // line comment
      let j = i; while (j < n && src[j] !== '\n') j++;
      blank(i, j); i = j; continue;
    }
    if (c === '"' && d === '"' && src[i + 2] === '"') { // raw string
      let j = i + 3;
      while (j < n && !(src[j] === '"' && src[j + 1] === '"' && src[j + 2] === '"')) j++;
      j = Math.min(j + 3, n);
      blank(i, j); i = j; continue;
    }
    if (c === '"') {                                    // normal string
      let j = i + 1;
      while (j < n && src[j] !== '"') { if (src[j] === '\\') j++; j++; }
      j = Math.min(j + 1, n);
      blank(i, j); i = j; continue;
    }
    if (c === "'") {                                    // char literal
      let j = i + 1;
      while (j < n && src[j] !== "'") { if (src[j] === '\\') j++; j++; }
      j = Math.min(j + 1, n);
      blank(i, j); i = j; continue;
    }
    i++;
  }
  return out.join('');
}

const WORD = (id) => new RegExp(`(^|[^A-Za-z0-9_$])${id.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}([^A-Za-z0-9_$]|$)`);

/** True iff `id` appears as a whole code token (comments/strings excluded). */
export function hasCodeToken(src, id) {
  if (src == null) return false;
  return WORD(id).test(stripNonCode(src));
}

/** True iff `id` appears ONLY outside code (comment/string) — used by the discriminating tests. */
export function appearsOnlyInNonCode(src, id) {
  if (src == null) return false;
  return WORD(id).test(src) && !WORD(id).test(stripNonCode(src));
}

/** All whole-word matches of `re` in CODE position only. */
export function codeTokens(src, re) {
  if (src == null) return [];
  return [...new Set(stripNonCode(src).match(re) || [])];
}

// ---------------------------------------------------------------------------------------------------
// SEMANTIC BINDING (V2d Phase 3, correction C5).
//
// "The identifier appears in code position" is NOT enough to accredit an axis: an incidental reference
// (a parameter name, an import, a log line, an unrelated mention) would count exactly like a registry
// entry. This classifies WHERE the token sits, so an axis can require a construct that actually means
// something for that axis. Anything it cannot classify returns null → the caller MUST emit
// UNRESOLVED / CODE_TOKEN_ONLY_NO_SEMANTIC_BINDING rather than invent a binding.
//
// Still not a parser: these are conservative structural patterns over comment/string-stripped code. They
// can miss real bindings (→ UNRESOLVED, safe) but must not classify an incidental mention as a binding.
const spanOf = (code, openRe) => {           // balanced-paren span starting at the first openRe match
  const m = openRe.exec(code); if (!m) return null;
  let depth = 0, i = m.index + m[0].length - 1, start = i;
  for (; i < code.length; i++) { const c = code[i]; if (c === '(') depth++; else if (c === ')') { depth--; if (depth === 0) return [start, i]; } }
  return null;
};

export function semanticBinding(src, id) {
  if (src == null) return null;
  const code = stripNonCode(src);
  const E = id.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const at = (re) => { const m = re.exec(code); return m ? { index: m.index, length: m[0].length } : null; };

  // type declaration: `class Foo`, `object Foo`, `interface Foo`
  let m = at(new RegExp(`\\b(?:class|object|interface)\\s+${E}\\b`));
  if (m) return { construct: 'type-declaration', matchRange: [m.index, m.index + m.length] };
  // property declaration: `val FOO =`, `const val FOO =`, `var FOO =`
  m = at(new RegExp(`\\b(?:const\\s+)?(?:val|var)\\s+${E}\\b`));
  if (m) return { construct: 'property-declaration', matchRange: [m.index, m.index + m.length] };
  // when branch / lambda arrow: `FOO ->`
  m = at(new RegExp(`(?:^|[^A-Za-z0-9_$.])${E}\\s*->`, 'm'));
  if (m) return { construct: 'when-branch-or-lambda', matchRange: [m.index, m.index + m.length] };
  // map entry: `FOO to X`  /  `X to FOO`
  m = at(new RegExp(`(?:^|[^A-Za-z0-9_$.])${E}\\s+to\\s+`, 'm')) || at(new RegExp(`\\bto\\s+${E}(?:[^A-Za-z0-9_$]|$)`, 'm'));
  if (m) return { construct: 'map-entry', matchRange: [m.index, m.index + m.length] };
  // enum member: inside ANY `enum class X { ... }` body. A file may declare several enums, so every body is
  // scanned — checking only the first made real members look unresolvable, which is under-detection dressed
  // up as an epistemic limit. UNRESOLVED must mean "cannot resolve", not "the regex was lazy".
  {
    const re = /enum\s+class\s+[A-Za-z0-9_]+\s*(?:\([^)]*\))?\s*(?::[^{]*)?\{/g;
    for (let em; (em = re.exec(code)) !== null;) {
      let depth = 0, i = em.index + em[0].length - 1;
      const start = i;
      for (; i < code.length; i++) { const c = code[i]; if (c === '{') depth++; else if (c === '}') { depth--; if (depth === 0) break; } }
      const body = code.slice(start, i);
      if (WORD(id).test(body)) return { construct: 'enum-member', matchRange: [start, i] };
      re.lastIndex = i;
    }
  }
  // collection entry: token inside a listOf(...) / setOf(...) / mapOf(...) span
  for (const kind of ['listOf', 'setOf', 'mapOf']) {
    let from = 0;
    for (;;) {
      const re = new RegExp(`\\b${kind}\\s*\\(`, 'g'); re.lastIndex = from;
      const s = spanOf(code.slice(from), re);
      if (!s) break;
      const [a, b] = [from + s[0], from + s[1]];
      if (WORD(id).test(code.slice(a, b))) return { construct: `collection-entry:${kind}`, matchRange: [a, b] };
      from = b + 1;
      if (from >= code.length) break;
    }
  }
  return WORD(id).test(code) ? { construct: null, matchRange: null, tokenOnly: true } : null;
}

/** Members of `enum class <name> { ... }`, code-position only. */
export function enumMembers(src, name) {
  if (src == null) return [];
  const code = stripNonCode(src);
  const m = code.match(new RegExp(`enum\\s+class\\s+${name}\\s*(?:\\([^)]*\\))?\\s*(?::[^{]*)?\\{([\\s\\S]*?)\\}`));
  if (!m) return [];
  return [...new Set(m[1].split(/[,\n;]/).map((s) => s.trim().replace(/\(.*$/, '').trim())
    .filter((s) => /^[A-Za-z][A-Za-z0-9_]*$/.test(s)))];
}
