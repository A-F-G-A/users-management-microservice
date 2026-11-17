// Regenerates docs/Actividad-11-Celular-DDD-CQRS.pdf from the reviewed Markdown sources.
// Dependency-free: a small Markdown subset (headings, paragraphs, lists, pipe tables, code
// blocks, `code`, **bold**) rendered with the standard PDF fonts in WinAnsiEncoding, so
// accents, ñ, underscores and technical names are preserved. Run: node docs/generate-academic-pdf.js
const fs = require('node:fs');
const path = require('node:path');

const docsDir = __dirname;
const SOURCES = ['actividad-11-celular-ddd-cqrs.md', 'guion-sustentacion.md'];
const OUTPUT = path.join(docsDir, 'Actividad-11-Celular-DDD-CQRS.pdf');
const COVER = {
  title: 'Actividad 11 — Celular',
  subtitle: 'Arquitectura DDD, Hexagonal y CQRS',
  stack: 'Java 17 · Spring Boot 3.3.5 · MySQL 8.4 · Spring JDBC · Flyway',
};
const FOOTER = 'Actividad 11 — Celular · DDD, Hexagonal y CQRS';

// ── Page geometry and styles ───────────────────────────────────────────────
const PAGE_W = 595;
const PAGE_H = 842;
const MARGIN_X = 56;
const TOP = PAGE_H - 60;
const BOTTOM = 60;
const CONTENT_W = PAGE_W - 2 * MARGIN_X;
const BODY = { size: 10, leading: 14 };
const TABLE = { size: 8.5, leading: 11, pad: 4 };
const CODE = { size: 8.5, leading: 11 };
const HEADINGS = {
  1: { size: 17, before: 18, after: 8, color: '0.10 0.22 0.42' },
  2: { size: 13, before: 14, after: 5, color: '0.10 0.22 0.42' },
  3: { size: 11, before: 10, after: 4, color: '0.15 0.15 0.15' },
};

// ── Font metrics (Adobe Helvetica / Helvetica-Bold AFM widths, chars 32..126) ──
const HELVETICA = [278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
  556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556, 1015, 667, 667,
  722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778, 667, 778, 722, 667, 611, 722,
  667, 944, 667, 667, 611, 278, 278, 278, 469, 556, 333, 556, 556, 500, 556, 556, 278, 556, 556,
  222, 222, 500, 222, 833, 556, 556, 556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334,
  260, 334, 584];
const HELVETICA_BOLD = [278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278,
  278, 556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611, 975, 722,
  722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778, 667, 778, 722, 667, 611,
  722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556, 333, 556, 611, 556, 611, 556, 333, 611,
  611, 278, 278, 556, 278, 889, 611, 611, 611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500,
  389, 280, 389, 584];
const SPECIAL_WIDTHS = { '—': 1000, '–': 556, '·': 278, '•': 350, '“': 333, '”': 333, '‘': 222, '’': 222, '…': 1000 };
const FONTS = { F1: 'Helvetica', F2: 'Helvetica-Bold', F3: 'Courier' };

// Characters outside WinAnsiEncoding are replaced by readable ASCII equivalents.
const REPLACEMENTS = { '→': '->', '←': '<-', '≤': '<=', '≥': '>=', '≠': '!=', '✓': 'OK', '×': 'x' };
const WIN_ANSI_EXTRA = { '€': 0x80, '…': 0x85, '‘': 0x91, '’': 0x92, '“': 0x93, '”': 0x94, '•': 0x95, '–': 0x96, '—': 0x97 };

function normalizeText(text) {
  return [...text].map((ch) => REPLACEMENTS[ch] ?? ch).join('');
}

function charWidth(ch, font) {
  if (font === 'F3') return 600;
  if (SPECIAL_WIDTHS[ch]) return SPECIAL_WIDTHS[ch];
  let code = ch.charCodeAt(0);
  if (code < 32 || code > 126) code = ch.normalize('NFD').charCodeAt(0); // á -> a, ñ -> n
  const table = font === 'F2' ? HELVETICA_BOLD : HELVETICA;
  return code >= 32 && code <= 126 ? table[code - 32] : 556;
}

function textWidth(text, font, size) {
  let units = 0;
  for (const ch of text) units += charWidth(ch, font);
  return (units * size) / 1000;
}

/** PDF literal string in WinAnsiEncoding; bytes >= 128 as octal escapes keep the file ASCII. */
function pdfString(text) {
  let out = '';
  for (const ch of text) {
    let code = WIN_ANSI_EXTRA[ch] ?? ch.charCodeAt(0);
    if (code > 255) code = 63; // '?'
    if (ch === '\\' || ch === '(' || ch === ')') out += `\\${ch}`;
    else if (code < 32 || code > 126) out += `\\${code.toString(8).padStart(3, '0')}`;
    else out += ch;
  }
  return `(${out})`;
}

// ── Inline Markdown: `code` and **bold** ───────────────────────────────────
function parseInline(text, baseFont = 'F1') {
  const runs = [];
  const pattern = /(`[^`]+`|\*\*[^*]+\*\*)/g;
  let last = 0;
  for (const match of normalizeText(text).matchAll(pattern)) {
    if (match.index > last) runs.push({ text: match.input.slice(last, match.index), font: baseFont });
    const token = match[0];
    runs.push(token.startsWith('`')
      ? { text: token.slice(1, -1), font: 'F3' }
      : { text: token.slice(2, -2), font: 'F2' });
    last = match.index + token.length;
  }
  const rest = normalizeText(text).slice(last);
  if (rest) runs.push({ text: rest, font: baseFont });
  return runs;
}

/** Splits runs into words (keeping fonts) and wraps them to `width`; returns lines of runs. */
function wrapRuns(runs, width, size) {
  const words = [];
  for (const run of runs) {
    const parts = run.text.split(/(\s+)/);
    for (const part of parts) {
      if (!part) continue;
      if (/^\s+$/.test(part)) words.push({ space: true });
      else words.push({ text: part, font: run.font });
    }
  }
  const lines = [];
  let line = [];
  let lineWidth = 0;
  let pendingSpace = false;
  const spaceWidth = textWidth(' ', 'F1', size);
  for (const word of words) {
    if (word.space) { pendingSpace = line.length > 0; continue; }
    const pieces = splitLongWord(word, width, size);
    for (const piece of pieces) {
      const w = textWidth(piece.text, piece.font, size);
      const extra = pendingSpace ? spaceWidth : 0;
      if (line.length && lineWidth + extra + w > width) {
        lines.push(line);
        line = [];
        lineWidth = 0;
        pendingSpace = false;
      }
      if (pendingSpace && line.length) { line.push({ text: ' ', font: 'F1' }); lineWidth += spaceWidth; }
      line.push(piece);
      lineWidth += w;
      pendingSpace = false;
    }
  }
  if (line.length) lines.push(line);
  return lines.length ? lines : [[]];
}

function splitLongWord(word, width, size) {
  if (textWidth(word.text, word.font, size) <= width) return [word];
  const pieces = [];
  let current = '';
  for (const ch of word.text) {
    if (current && textWidth(current + ch, word.font, size) > width) { pieces.push({ text: current, font: word.font }); current = ''; }
    current += ch;
  }
  if (current) pieces.push({ text: current, font: word.font });
  return pieces;
}

// ── Markdown blocks ────────────────────────────────────────────────────────
function parseMarkdown(source) {
  const lines = source.replace(/\r\n/g, '\n').split('\n');
  const blocks = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    if (!line.trim()) { i += 1; continue; }
    if (line.startsWith('```')) {
      const code = [];
      i += 1;
      while (i < lines.length && !lines[i].startsWith('```')) code.push(lines[i++]);
      i += 1;
      blocks.push({ type: 'code', lines: code });
      continue;
    }
    const heading = /^(#{1,3})\s+(.*)$/.exec(line);
    if (heading) { blocks.push({ type: 'heading', level: heading[1].length, text: heading[2] }); i += 1; continue; }
    if (line.trim().startsWith('|')) {
      const rows = [];
      while (i < lines.length && lines[i].trim().startsWith('|')) {
        const cells = lines[i].trim().replace(/^\||\|$/g, '').split('|').map((c) => c.trim());
        if (!cells.every((c) => /^:?-{3,}:?$/.test(c))) rows.push(cells);
        i += 1;
      }
      blocks.push({ type: 'table', rows });
      continue;
    }
    const item = /^(\s*)(?:[-*]|(\d+)\.)\s+(.*)$/.exec(line);
    if (item) {
      let text = item[3];
      i += 1;
      while (i < lines.length && /^\s{2,}\S/.test(lines[i]) && !/^\s*(?:[-*]|\d+\.)\s/.test(lines[i])) text += ` ${lines[i++].trim()}`;
      blocks.push({ type: 'item', marker: item[2] ? `${item[2]}.` : '•', text });
      continue;
    }
    let text = line.trim();
    i += 1;
    while (i < lines.length && lines[i].trim() && !/^(#{1,3}\s|```|\s*\||\s*(?:[-*]|\d+\.)\s)/.test(lines[i])) text += ` ${lines[i++].trim()}`;
    blocks.push({ type: 'para', text });
  }
  return blocks;
}

// ── Layout ─────────────────────────────────────────────────────────────────
const pages = [];
let ops = null;
let y = 0;

function newPage() {
  ops = [];
  pages.push(ops);
  y = TOP;
}

function ensureSpace(height) {
  if (!ops || y - height < BOTTOM) newPage();
}

function drawRuns(runs, x, baseline, size, color = '0.12 0.12 0.12') {
  let cursor = x;
  for (const run of runs) {
    if (!run.text) continue;
    ops.push(`BT /${run.font} ${size} Tf ${color} rg 1 0 0 1 ${cursor.toFixed(2)} ${baseline.toFixed(2)} Tm ${pdfString(run.text)} Tj ET`);
    cursor += textWidth(run.text, run.font, size);
  }
}

function renderCover() {
  newPage();
  ops.push(`0.10 0.22 0.42 rg ${MARGIN_X} ${TOP - 92} ${CONTENT_W} 4 re f`);
  drawRuns([{ text: normalizeText(COVER.title), font: 'F2' }], MARGIN_X, TOP - 30, 24, '0.10 0.22 0.42');
  drawRuns([{ text: normalizeText(COVER.subtitle), font: 'F1' }], MARGIN_X, TOP - 56, 14);
  drawRuns([{ text: normalizeText(COVER.stack), font: 'F1' }], MARGIN_X, TOP - 76, 10, '0.35 0.35 0.35');
  y = TOP - 120;
}

function renderHeading(block, first) {
  const style = HEADINGS[block.level];
  if (block.level === 1 && !first) newPage();
  const lines = wrapRuns(parseInline(block.text, 'F2'), CONTENT_W, style.size);
  const height = style.before + lines.length * style.size * 1.25 + style.after;
  ensureSpace(height + 3 * BODY.leading); // keep heading with following text
  y -= style.before;
  for (const line of lines) {
    y -= style.size;
    drawRuns(line, MARGIN_X, y, style.size, style.color);
    y -= style.size * 0.25;
  }
  if (block.level === 1) ops.push(`0.75 0.80 0.88 RG 0.8 w ${MARGIN_X} ${(y - 2).toFixed(2)} m ${MARGIN_X + CONTENT_W} ${(y - 2).toFixed(2)} l S`);
  y -= style.after;
}

function renderParagraph(text, indent = 0, marker = null) {
  const x = MARGIN_X + indent;
  const lines = wrapRuns(parseInline(text), CONTENT_W - indent, BODY.size);
  lines.forEach((line, index) => {
    ensureSpace(BODY.leading);
    y -= BODY.leading;
    if (index === 0 && marker) drawRuns([{ text: marker, font: 'F1' }], x - 12, y, BODY.size);
    drawRuns(line, x, y, BODY.size);
  });
  y -= marker ? 2 : 6;
}

function renderCode(lines) {
  const charsPerLine = Math.floor((CONTENT_W - 12) / (0.6 * CODE.size));
  const wrapped = lines.flatMap((l) => (l.length ? l.match(new RegExp(`.{1,${charsPerLine}}`, 'g')) : ['']));
  for (const line of wrapped) {
    ensureSpace(CODE.leading + 4);
    ops.push(`0.95 0.95 0.95 rg ${MARGIN_X} ${(y - CODE.leading - 3).toFixed(2)} ${CONTENT_W} ${CODE.leading} re f`);
    y -= CODE.leading;
    drawRuns([{ text: normalizeText(line), font: 'F3' }], MARGIN_X + 6, y, CODE.size);
  }
  y -= 10;
}

function columnWidths(rows, size) {
  const n = Math.max(...rows.map((r) => r.length));
  const natural = new Array(n).fill(0);
  const minimum = new Array(n).fill(0);
  rows.forEach((row, r) => row.forEach((cell, c) => {
    const runs = parseInline(cell, r === 0 ? 'F2' : 'F1');
    const full = runs.reduce((sum, run) => sum + textWidth(run.text, run.font, size), 0);
    const longestWord = Math.max(0, ...runs.flatMap((run) => run.text.split(/\s+/).map((w) => textWidth(w, run.font, size))));
    natural[c] = Math.max(natural[c], full + 2 * TABLE.pad);
    minimum[c] = Math.max(minimum[c], Math.min(longestWord, CONTENT_W / 2) + 2 * TABLE.pad);
  }));
  const sumNatural = natural.reduce((a, b) => a + b, 0);
  if (sumNatural <= CONTENT_W) return natural.map((w) => (w * CONTENT_W) / sumNatural);
  const sumMin = minimum.reduce((a, b) => a + b, 0);
  if (sumMin >= CONTENT_W) return minimum.map((w) => (w * CONTENT_W) / sumMin);
  const flexible = natural.map((w, c) => w - minimum[c]);
  const sumFlex = flexible.reduce((a, b) => a + b, 0);
  return minimum.map((w, c) => w + (flexible[c] * (CONTENT_W - sumMin)) / sumFlex);
}

function renderTable(rows) {
  const size = TABLE.size;
  const widths = columnWidths(rows, size);
  const layoutRow = (row, header) => {
    const cells = widths.map((w, c) => wrapRuns(parseInline(row[c] ?? '', header ? 'F2' : 'F1'), w - 2 * TABLE.pad, size));
    return { cells, height: Math.max(...cells.map((l) => l.length)) * TABLE.leading + 2 * TABLE.pad, header };
  };
  const header = layoutRow(rows[0], true);
  const drawRow = (row) => {
    const top = y;
    if (row.header) ops.push(`0.89 0.92 0.96 rg ${MARGIN_X} ${(top - row.height).toFixed(2)} ${CONTENT_W} ${row.height.toFixed(2)} re f`);
    let x = MARGIN_X;
    row.cells.forEach((lines, c) => {
      lines.forEach((line, l) => drawRuns(line, x + TABLE.pad, top - TABLE.pad - size - l * TABLE.leading + 1.5, size));
      ops.push(`0.70 0.70 0.70 RG 0.5 w ${x.toFixed(2)} ${(top - row.height).toFixed(2)} ${widths[c].toFixed(2)} ${row.height.toFixed(2)} re S`);
      x += widths[c];
    });
    y -= row.height;
  };
  ensureSpace(header.height + 2 * TABLE.leading + 2 * TABLE.pad);
  y -= 2;
  drawRow(header);
  for (const row of rows.slice(1).map((r) => layoutRow(r, false))) {
    if (y - row.height < BOTTOM) { newPage(); drawRow(header); }
    drawRow(row);
  }
  y -= 10;
}

function render(blocks, { first }) {
  blocks.forEach((block, index) => {
    if (block.type === 'heading') renderHeading(block, first && index === 0);
    else if (block.type === 'para') renderParagraph(block.text);
    else if (block.type === 'item') renderParagraph(block.text, 16, block.marker);
    else if (block.type === 'table') renderTable(block.rows);
    else if (block.type === 'code') renderCode(block.lines);
  });
}

renderCover();
SOURCES.forEach((file, index) => {
  const blocks = parseMarkdown(fs.readFileSync(path.join(docsDir, file), 'utf8'));
  render(blocks, { first: index === 0 });
});

pages.forEach((pageOps, index) => {
  const label = `Página ${index + 1} de ${pages.length}`;
  ops = pageOps;
  ops.push(`0.80 0.80 0.80 RG 0.5 w ${MARGIN_X} 44 m ${MARGIN_X + CONTENT_W} 44 l S`);
  drawRuns([{ text: normalizeText(FOOTER), font: 'F1' }], MARGIN_X, 32, 8, '0.45 0.45 0.45');
  drawRuns([{ text: label, font: 'F1' }], MARGIN_X + CONTENT_W - textWidth(label, 'F1', 8), 32, 8, '0.45 0.45 0.45');
});

// ── PDF serialization ──────────────────────────────────────────────────────
const objects = [];
const add = (body) => { objects.push(body); return objects.length; };
const catalogId = add(null);
const pagesId = add(null);
const fontIds = Object.entries(FONTS).map(([key, base]) => [key, add(`<< /Type /Font /Subtype /Type1 /BaseFont /${base} /Encoding /WinAnsiEncoding >>`)]);
const fontResources = fontIds.map(([key, id]) => `/${key} ${id} 0 R`).join(' ');
const pageIds = pages.map((pageOps) => {
  const content = pageOps.join('\n');
  const contentId = add(`<< /Length ${Buffer.byteLength(content, 'latin1')} >>\nstream\n${content}\nendstream`);
  return add(`<< /Type /Page /Parent ${pagesId} 0 R /MediaBox [0 0 ${PAGE_W} ${PAGE_H}] /Resources << /Font << ${fontResources} >> >> /Contents ${contentId} 0 R >>`);
});
objects[catalogId - 1] = `<< /Type /Catalog /Pages ${pagesId} 0 R /Lang (es-CO) >>`;
objects[pagesId - 1] = `<< /Type /Pages /Kids [${pageIds.map((id) => `${id} 0 R`).join(' ')}] /Count ${pageIds.length} >>`;
const infoId = add(`<< /Title ${pdfString(`${COVER.title}: ${COVER.subtitle}`)} /Subject ${pdfString(COVER.stack)} /Producer (docs/generate-academic-pdf.js) >>`);

let pdf = '%PDF-1.4\n%\xE2\xE3\xCF\xD3\n';
const offsets = [];
objects.forEach((body, index) => {
  offsets.push(Buffer.byteLength(pdf, 'latin1'));
  pdf += `${index + 1} 0 obj\n${body}\nendobj\n`;
});
const xrefOffset = Buffer.byteLength(pdf, 'latin1');
pdf += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
offsets.forEach((offset) => { pdf += `${String(offset).padStart(10, '0')} 00000 n \n`; });
pdf += `trailer\n<< /Size ${objects.length + 1} /Root ${catalogId} 0 R /Info ${infoId} 0 R >>\nstartxref\n${xrefOffset}\n%%EOF\n`;

fs.writeFileSync(OUTPUT, Buffer.from(pdf, 'latin1'));
process.stdout.write(`PDF generado: ${OUTPUT} (${pages.length} páginas)\n`);
