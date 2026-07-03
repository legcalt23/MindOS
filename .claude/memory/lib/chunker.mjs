// @ts-check
/**
 * chunker.mjs — split a Markdown document into retrievable chunks.
 *
 * Strategy: cut on heading boundaries first (a CLAUDE.md "### Aprendizaje" entry,
 * an ADR section, a HANDOFF heading each become a chunk). Oversized sections are
 * sub-split by paragraph to a soft token target with small overlap. Each chunk
 * carries the nearest heading as `anchor` and its start line for citation.
 */

/** Rough token estimate: ~4 chars/token. Good enough for budgeting. */
export function estimateTokens(text) {
  return Math.ceil(String(text).length / 4);
}

const HEADING_RE = /^(#{1,6})\s+(.*)$/;

/**
 * @param {string} markdown
 * @param {{ targetTokens?: number, maxTokens?: number, overlapTokens?: number, splitOnHeading?: string }} [opts]
 * @returns {Array<{ text: string, anchor: string, startLine: number, ord: number }>}
 */
export function chunkMarkdown(markdown, opts = {}) {
  const targetTokens = opts.targetTokens || 400;
  const maxTokens = opts.maxTokens || 512;
  const overlapTokens = opts.overlapTokens || 40;
  const text = String(markdown).replace(/\r\n/g, '\n');
  const lines = text.split('\n');

  // Group lines into heading-delimited sections.
  const sections = [];
  let current = { heading: '', startLine: 1, lines: [] };
  lines.forEach((line, idx) => {
    const m = line.match(HEADING_RE);
    if (m) {
      if (current.lines.length) sections.push(current);
      current = { heading: m[2].trim(), startLine: idx + 1, lines: [line] };
    } else {
      current.lines.push(line);
    }
  });
  if (current.lines.length) sections.push(current);
  if (!sections.length) sections.push({ heading: '', startLine: 1, lines });

  /** @type {Array<{ text: string, anchor: string, startLine: number, ord: number }>} */
  const chunks = [];
  let ord = 0;
  for (const section of sections) {
    const body = section.lines.join('\n').trim();
    if (!body) continue;
    if (estimateTokens(body) <= maxTokens) {
      chunks.push({ text: body, anchor: section.heading || `L${section.startLine}`, startLine: section.startLine, ord: ord++ });
      continue;
    }
    // Sub-split oversized section by paragraphs to the target size with overlap.
    const paragraphs = body.split(/\n{2,}/);
    let buf = [];
    let bufTokens = 0;
    let tail = '';
    for (const p of paragraphs) {
      const pTokens = estimateTokens(p);
      if (bufTokens + pTokens > targetTokens && buf.length) {
        const chunkText = (tail ? tail + '\n\n' : '') + buf.join('\n\n');
        chunks.push({ text: chunkText.trim(), anchor: section.heading || `L${section.startLine}`, startLine: section.startLine, ord: ord++ });
        tail = overlapTokens > 0 ? lastTokens(buf.join('\n\n'), overlapTokens) : '';
        buf = [];
        bufTokens = 0;
      }
      buf.push(p);
      bufTokens += pTokens;
    }
    if (buf.length) {
      const chunkText = (tail ? tail + '\n\n' : '') + buf.join('\n\n');
      chunks.push({ text: chunkText.trim(), anchor: section.heading || `L${section.startLine}`, startLine: section.startLine, ord: ord++ });
    }
  }
  return chunks;
}

/** Take roughly the last N tokens of text (for chunk overlap). */
function lastTokens(text, tokens) {
  const chars = tokens * 4;
  return text.length <= chars ? text : text.slice(text.length - chars);
}
