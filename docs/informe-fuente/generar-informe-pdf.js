// Genera docs/entrega-academica/Informe-Actividad-11-Celular.pdf desde informe-actividad-11.md.
//
// Sin dependencias externas. Incrusta fuentes TrueType (Type0/Identity-H + ToUnicode), por lo
// que cualquier carácter UTF-8 presente en la fuente se representa y se puede copiar/buscar.
// Incluye portada, índice con números de página y enlaces, marcadores (outline) y paginación.
//
// Uso:  node docs/informe-fuente/generar-informe-pdf.js
// Fuentes por defecto: Arial, Arial Bold y Consolas de Windows. Para otras rutas defina
// PDF_FONT_REGULAR, PDF_FONT_BOLD y PDF_FONT_MONO. El script falla si falta algún glifo.
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const { execSync } = require('node:child_process');

const ROOT = path.resolve(__dirname, '..', '..');
const SOURCE = path.join(__dirname, 'informe-actividad-11.md');
const OUTPUT = path.join(ROOT, 'docs', 'entrega-academica', 'Informe-Actividad-11-Celular.pdf');

const git = (args) => {
  try { return execSync(`git ${args}`, { cwd: ROOT, encoding: 'utf8' }).trim(); } catch { return ''; }
};
// Last commit that touched application code or tests: the version this report describes.
const CODE_COMMIT = git('log -1 --format=%h -- src pom.xml');

const COVER = {
  kicker: 'Informe técnico',
  title: 'Actividad 11: CRUDL de Celular',
  subtitle: 'Java 17 · Spring Boot · DDD · Arquitectura Hexagonal · CQRS',
  // Datos institucionales: se muestran solo si se completan (no se inventan).
  institutional: [
    ['Institución', ''],
    ['Asignatura', ''],
    ['Docente', ''],
    ['Integrante(s)', ''],
  ],
  project: [
    ['Repositorio', 'github.com/A-F-G-A/users-management-microservice'],
    ['Rama', 'feature/actividad-11-celular-ddd-cqrs'],
    ['Versión del código', CODE_COMMIT ? `commit ${CODE_COMMIT}` : 'no disponible'],
    ['Fecha de generación', new Date().toISOString().slice(0, 10)],
  ],
};
const FOOTER = 'Informe técnico · Actividad 11: CRUDL de Celular';

// ── Geometría y estilos ─────────────────────────────────────────────────────
const PAGE_W = 595.28;
const PAGE_H = 841.89;
const MARGIN_X = 62;
const TOP = PAGE_H - 64;
const BOTTOM = 64;
const CONTENT_W = PAGE_W - 2 * MARGIN_X;
const INK = '0.13 0.13 0.13';
const ACCENT = '0.09 0.24 0.45';
const MUTED = '0.42 0.42 0.42';
const BODY = { size: 10.5, leading: 15 };
const TABLE = { size: 8.8, leading: 11.6, pad: 4.2 };
const HEADINGS = {
  1: { size: 16, before: 20, after: 8, color: ACCENT },
  2: { size: 12.5, before: 12, after: 5, color: ACCENT },
  3: { size: 11, before: 10, after: 4, color: INK },
};

// ── Fuentes TrueType ────────────────────────────────────────────────────────
const FONT_FILES = {
  F1: { file: process.env.PDF_FONT_REGULAR || 'C:/Windows/Fonts/arial.ttf', name: 'ArialMT' },
  F2: { file: process.env.PDF_FONT_BOLD || 'C:/Windows/Fonts/arialbd.ttf', name: 'Arial-BoldMT' },
  F3: { file: process.env.PDF_FONT_MONO || 'C:/Windows/Fonts/consola.ttf', name: 'Consolas' },
};
const FALLBACKS = { '→': '->', '←': '<-', '≤': '<=', '≥': '>=', '✓': 'OK' };

function parseTrueType(file) {
  const buf = fs.readFileSync(file);
  const tables = {};
  for (let i = 0; i < buf.readUInt16BE(4); i += 1) {
    const at = 12 + 16 * i;
    tables[buf.toString('latin1', at, at + 4)] = buf.readUInt32BE(at + 8);
  }
  for (const tag of ['head', 'hhea', 'hmtx', 'maxp', 'cmap']) {
    if (tables[tag] === undefined) throw new Error(`${file}: falta la tabla ${tag}`);
  }
  const os2 = tables['OS/2'];
  if (os2 !== undefined && (buf.readUInt16BE(os2 + 8) & 0x0002)) {
    throw new Error(`${file}: la licencia de la fuente no permite incrustarla`);
  }
  const head = tables.head;
  const unitsPerEm = buf.readUInt16BE(head + 18);
  const scale = (v) => Math.round((v * 1000) / unitsPerEm);
  const numberOfHMetrics = buf.readUInt16BE(tables.hhea + 34);
  const numGlyphs = buf.readUInt16BE(tables.maxp + 4);
  const advances = new Array(numGlyphs);
  for (let g = 0; g < numGlyphs; g += 1) {
    const idx = Math.min(g, numberOfHMetrics - 1);
    advances[g] = scale(buf.readUInt16BE(tables.hmtx + 4 * idx));
  }
  const cmap = readCmap(buf, tables.cmap);
  const capHeight = os2 !== undefined && buf.readUInt16BE(os2) >= 2 ? buf.readInt16BE(os2 + 88) : 700 * unitsPerEm / 1000;
  return {
    data: buf,
    cmap,
    advances,
    bbox: [36, 38, 40, 42].map((o) => scale(buf.readInt16BE(head + o))),
    ascent: scale(buf.readInt16BE(tables.hhea + 4)),
    descent: scale(buf.readInt16BE(tables.hhea + 6)),
    capHeight: scale(capHeight),
    used: new Map([[0, 0]]),
  };
}

function readCmap(buf, cmapAt) {
  const subtables = [];
  for (let i = 0; i < buf.readUInt16BE(cmapAt + 2); i += 1) {
    const at = cmapAt + 4 + 8 * i;
    subtables.push({ platform: buf.readUInt16BE(at), encoding: buf.readUInt16BE(at + 2), offset: cmapAt + buf.readUInt32BE(at + 4) });
  }
  const pick = subtables.find((s) => s.platform === 3 && s.encoding === 10 && buf.readUInt16BE(s.offset) === 12)
    || subtables.find((s) => s.platform === 3 && s.encoding === 1 && buf.readUInt16BE(s.offset) === 4)
    || subtables.find((s) => s.platform === 0 && buf.readUInt16BE(s.offset) === 4);
  if (!pick) throw new Error('No se encontró una tabla cmap Unicode compatible');
  const t = pick.offset;
  if (buf.readUInt16BE(t) === 12) {
    const groups = [];
    for (let i = 0; i < buf.readUInt32BE(t + 12); i += 1) {
      const at = t + 16 + 12 * i;
      groups.push([buf.readUInt32BE(at), buf.readUInt32BE(at + 4), buf.readUInt32BE(at + 8)]);
    }
    return (cp) => {
      for (const [start, end, gid] of groups) if (cp >= start && cp <= end) return gid + (cp - start);
      return 0;
    };
  }
  const segX2 = buf.readUInt16BE(t + 6);
  const ends = t + 14;
  const starts = ends + segX2 + 2;
  const deltas = starts + segX2;
  const ranges = deltas + segX2;
  return (cp) => {
    if (cp > 0xffff) return 0;
    for (let i = 0; i < segX2 / 2; i += 1) {
      if (cp > buf.readUInt16BE(ends + 2 * i)) continue;
      const start = buf.readUInt16BE(starts + 2 * i);
      if (cp < start) return 0;
      const delta = buf.readUInt16BE(deltas + 2 * i);
      const rangeOffset = buf.readUInt16BE(ranges + 2 * i);
      if (rangeOffset === 0) return (cp + delta) & 0xffff;
      const g = buf.readUInt16BE(ranges + 2 * i + rangeOffset + 2 * (cp - start));
      return g === 0 ? 0 : (g + delta) & 0xffff;
    }
    return 0;
  };
}

const FONTS = Object.fromEntries(Object.entries(FONT_FILES).map(([key, spec]) => [key, { ...spec, ...parseTrueType(spec.file) }]));
const missingGlyphs = new Set();

/** Applies fallbacks for characters absent from the font; records any that remain missing. */
function prepare(text, fontKey) {
  const font = FONTS[fontKey];
  let out = '';
  for (const ch of text) {
    if (font.cmap(ch.codePointAt(0))) out += ch;
    else if (FALLBACKS[ch]) out += FALLBACKS[ch];
    else { missingGlyphs.add(`${ch} (U+${ch.codePointAt(0).toString(16).toUpperCase()}) en ${font.name}`); out += '?'; }
  }
  return out;
}

function textWidth(text, fontKey, size) {
  const font = FONTS[fontKey];
  let units = 0;
  for (const ch of text) units += font.advances[font.cmap(ch.codePointAt(0))] ?? 0;
  return (units * size) / 1000;
}

function encodeText(text, fontKey) {
  const font = FONTS[fontKey];
  let hex = '';
  for (const ch of text) {
    const gid = font.cmap(ch.codePointAt(0));
    font.used.set(gid, ch.codePointAt(0));
    hex += gid.toString(16).padStart(4, '0');
  }
  return `<${hex}>`;
}

function utf16Hex(text) {
  const units = [0xfeff];
  for (let i = 0; i < text.length; i += 1) units.push(text.charCodeAt(i));
  return `<${units.map((u) => u.toString(16).padStart(4, '0')).join('')}>`;
}

// ── Markdown en línea: `código` y **negrita** ──────────────────────────────
function parseInline(text, baseFont = 'F1') {
  const runs = [];
  const pattern = /(`[^`]+`|\*\*[^*]+\*\*)/g;
  let last = 0;
  for (const match of text.matchAll(pattern)) {
    if (match.index > last) runs.push({ text: text.slice(last, match.index), font: baseFont });
    const token = match[0];
    runs.push(token.startsWith('`') ? { text: token.slice(1, -1), font: 'F3' } : { text: token.slice(2, -2), font: 'F2' });
    last = match.index + token.length;
  }
  if (last < text.length) runs.push({ text: text.slice(last), font: baseFont });
  return runs.map((run) => ({ ...run, text: prepare(run.text, run.font) }));
}

function wrapRuns(runs, width, size) {
  const words = [];
  for (const run of runs) {
    for (const part of run.text.split(/(\s+)/)) {
      if (!part) continue;
      words.push(/^\s+$/.test(part) ? { space: true } : { text: part, font: run.font });
    }
  }
  const lines = [];
  let line = [];
  let lineWidth = 0;
  let pendingSpace = false;
  const spaceWidth = textWidth(' ', 'F1', size);
  for (const word of words) {
    if (word.space) { pendingSpace = line.length > 0; continue; }
    for (const piece of splitLongWord(word, width, size)) {
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

// ── Bloques Markdown ────────────────────────────────────────────────────────
function parseMarkdown(source) {
  const lines = source.replace(/\r\n/g, '\n').split('\n');
  const blocks = [];
  let i = 0;
  const startsBlock = (l) => /^(#{1,3}\s|```|\s*\||\s*(?:[-*]|\d+\.)\s)/.test(l);
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
    if (heading) { blocks.push({ type: 'heading', level: heading[1].length, text: heading[2].trim() }); i += 1; continue; }
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
    const item = /^\s*(?:[-*]|(\d+)\.)\s+(.*)$/.exec(line);
    if (item) {
      let text = item[2];
      i += 1;
      while (i < lines.length && lines[i].trim() && !startsBlock(lines[i])) text += ` ${lines[i++].trim()}`;
      blocks.push({ type: 'item', marker: item[1] ? `${item[1]}.` : '•', text });
      continue;
    }
    let text = line.trim();
    i += 1;
    while (i < lines.length && lines[i].trim() && !startsBlock(lines[i])) text += ` ${lines[i++].trim()}`;
    blocks.push({ type: 'para', text });
  }
  return blocks;
}

// ── Composición ─────────────────────────────────────────────────────────────
let pages;
let page;
let y;
let headings;

function newPage() {
  page = { ops: [], annots: [] };
  pages.push(page);
  y = TOP;
}

function ensureSpace(height) {
  if (y - height < BOTTOM) newPage();
}

function drawRuns(runs, x, baseline, size, color = INK) {
  let cursor = x;
  for (const run of runs) {
    if (!run.text) continue;
    page.ops.push(`BT /${run.font} ${size} Tf ${color} rg 1 0 0 1 ${cursor.toFixed(2)} ${baseline.toFixed(2)} Tm ${encodeText(run.text, run.font)} Tj ET`);
    cursor += textWidth(run.text, run.font, size);
  }
  return cursor;
}

function plain(text, font = 'F1') {
  return [{ text: prepare(text, font), font }];
}

function renderCover() {
  newPage();
  page.ops.push(`${ACCENT} rg 0 ${PAGE_H - 250} ${PAGE_W} 250 re f`);
  drawRuns(plain(COVER.kicker.toUpperCase(), 'F2'), MARGIN_X, PAGE_H - 110, 11, '0.78 0.85 0.95');
  drawRuns(plain(COVER.title, 'F2'), MARGIN_X, PAGE_H - 150, 26, '1 1 1');
  drawRuns(plain(COVER.subtitle), MARGIN_X, PAGE_H - 180, 12.5, '0.88 0.92 0.98');
  y = PAGE_H - 300;
  const rows = [...COVER.institutional.filter(([, v]) => v), ...COVER.project];
  for (const [label, value] of rows) {
    drawRuns(plain(label, 'F2'), MARGIN_X, y, 10.5, MUTED);
    drawRuns(plain(value), MARGIN_X + 140, y, 10.5);
    page.ops.push(`0.85 0.85 0.85 RG 0.5 w ${MARGIN_X} ${(y - 7).toFixed(2)} m ${MARGIN_X + CONTENT_W} ${(y - 7).toFixed(2)} l S`);
    y -= 24;
  }
  drawRuns(plain('Contenido elaborado a partir del código y de las pruebas del repositorio; los resultados'), MARGIN_X, BOTTOM + 14, 8.5, MUTED);
  drawRuns(plain('no confirmados se identifican como pendientes en las secciones 9, 10 y 11.'), MARGIN_X, BOTTOM + 3, 8.5, MUTED);
}

function renderHeading(block) {
  const style = HEADINGS[block.level];
  const lines = wrapRuns(parseInline(block.text, 'F2'), CONTENT_W, style.size);
  ensureSpace(style.before + lines.length * style.size * 1.3 + style.after + 3 * BODY.leading);
  y -= style.before;
  const top = y;
  for (const line of lines) {
    y -= style.size;
    drawRuns(line, MARGIN_X, y, style.size, style.color);
    y -= style.size * 0.3;
  }
  if (block.level === 1) page.ops.push(`0.74 0.80 0.88 RG 0.8 w ${MARGIN_X} ${(y - 2).toFixed(2)} m ${MARGIN_X + CONTENT_W} ${(y - 2).toFixed(2)} l S`);
  y -= style.after;
  if (block.level <= 2) headings.push({ level: block.level, text: block.text, page: pages.length - 1, top: top + 4 });
}

function renderParagraph(text, indent = 0, marker = null) {
  const x = MARGIN_X + indent;
  const lines = wrapRuns(parseInline(text), CONTENT_W - indent, BODY.size);
  lines.forEach((line, index) => {
    ensureSpace(BODY.leading);
    y -= BODY.leading;
    if (index === 0 && marker) drawRuns(plain(marker), x - 14, y, BODY.size, marker === '•' ? ACCENT : INK);
    drawRuns(line, x, y, BODY.size);
  });
  y -= marker ? 3 : 7;
}

function renderCode(lines) {
  const size = 8.8;
  const leading = 11.5;
  for (const line of lines) {
    ensureSpace(leading + 4);
    page.ops.push(`0.95 0.96 0.97 rg ${MARGIN_X} ${(y - leading - 3).toFixed(2)} ${CONTENT_W} ${leading} re f`);
    y -= leading;
    drawRuns(plain(line, 'F3'), MARGIN_X + 6, y, size);
  }
  y -= 10;
}

/** Largest size (down to 7 pt) at which no word of the table must be split. */
function tableFontSize(rows) {
  for (const size of [TABLE.size, 8.2, 7.6, 7]) {
    const n = Math.max(...rows.map((r) => r.length));
    const longest = new Array(n).fill(0);
    rows.forEach((row, r) => row.forEach((cell, c) => {
      for (const run of parseInline(cell, r === 0 ? 'F2' : 'F1')) {
        for (const w of run.text.split(/s+/)) longest[c] = Math.max(longest[c], textWidth(w, run.font, size) + 2 * TABLE.pad);
      }
    }));
    if (longest.reduce((a, b) => a + b, 0) <= CONTENT_W) return size;
  }
  return 7;
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
    minimum[c] = Math.max(minimum[c], Math.min(longestWord, CONTENT_W / 2.2) + 2 * TABLE.pad);
  }));
  const sum = (a) => a.reduce((x, v) => x + v, 0);
  if (sum(natural) <= CONTENT_W) return natural.map((w) => (w * CONTENT_W) / sum(natural));
  if (sum(minimum) >= CONTENT_W) return minimum.map((w) => (w * CONTENT_W) / sum(minimum));
  const flexible = natural.map((w, c) => w - minimum[c]);
  return minimum.map((w, c) => w + (flexible[c] * (CONTENT_W - sum(minimum))) / sum(flexible));
}

function renderTable(rows) {
  const size = tableFontSize(rows);
  const leading = (TABLE.leading * size) / TABLE.size;
  const widths = columnWidths(rows, size);
  const layoutRow = (row, header) => {
    const cells = widths.map((w, c) => wrapRuns(parseInline(row[c] ?? '', header ? 'F2' : 'F1'), w - 2 * TABLE.pad, size));
    return { cells, height: Math.max(...cells.map((l) => l.length)) * leading + 2 * TABLE.pad, header };
  };
  const header = layoutRow(rows[0], true);
  const body = rows.slice(1).map((r) => layoutRow(r, false));
  const drawRow = (row, shaded) => {
    const top = y;
    if (row.header) page.ops.push(`0.87 0.91 0.96 rg ${MARGIN_X} ${(top - row.height).toFixed(2)} ${CONTENT_W} ${row.height.toFixed(2)} re f`);
    else if (shaded) page.ops.push(`0.975 0.98 0.985 rg ${MARGIN_X} ${(top - row.height).toFixed(2)} ${CONTENT_W} ${row.height.toFixed(2)} re f`);
    let x = MARGIN_X;
    row.cells.forEach((lines, c) => {
      lines.forEach((line, l) => drawRuns(line, x + TABLE.pad, top - TABLE.pad - size - l * leading + 1.6, size));
      page.ops.push(`0.72 0.74 0.78 RG 0.45 w ${x.toFixed(2)} ${(top - row.height).toFixed(2)} ${widths[c].toFixed(2)} ${row.height.toFixed(2)} re S`);
      x += widths[c];
    });
    y -= row.height;
  };
  // Keep the header with at least the first body row.
  ensureSpace(header.height + (body[0]?.height ?? 0) + 4);
  y -= 3;
  drawRow(header);
  body.forEach((row, index) => {
    if (y - row.height < BOTTOM) { newPage(); drawRow(header); }
    drawRow(row, index % 2 === 1);
  });
  y -= 11;
}

const TOC_LINE = 17;
const TOC_LINES_FIRST = Math.floor((TOP - 60 - BOTTOM) / TOC_LINE);
const TOC_LINES_NEXT = Math.floor((TOP - BOTTOM) / TOC_LINE);

function layout(blocks, tocPageCount) {
  pages = [];
  headings = [];
  renderCover();
  const tocStart = pages.length;
  for (let i = 0; i < tocPageCount; i += 1) newPage();
  newPage();
  for (const block of blocks) {
    if (block.type === 'heading') renderHeading(block);
    else if (block.type === 'para') renderParagraph(block.text);
    else if (block.type === 'item') renderParagraph(block.text, 18, block.marker);
    else if (block.type === 'table') renderTable(block.rows);
    else if (block.type === 'code') renderCode(block.lines);
  }
  return tocStart;
}

function tocPagesNeeded(entries) {
  if (entries <= TOC_LINES_FIRST) return 1;
  return 1 + Math.ceil((entries - TOC_LINES_FIRST) / TOC_LINES_NEXT);
}

function renderToc(tocStart) {
  let index = tocStart;
  page = pages[index];
  y = TOP;
  drawRuns(plain('Índice', 'F2'), MARGIN_X, y - 16, 18, ACCENT);
  page.ops.push(`0.74 0.80 0.88 RG 0.8 w ${MARGIN_X} ${(y - 26).toFixed(2)} m ${MARGIN_X + CONTENT_W} ${(y - 26).toFixed(2)} l S`);
  y -= 60;
  for (const entry of headings) {
    if (y - TOC_LINE < BOTTOM) { index += 1; page = pages[index]; y = TOP; }
    y -= TOC_LINE;
    const font = entry.level === 1 ? 'F2' : 'F1';
    const size = entry.level === 1 ? 10.5 : 10;
    const indent = entry.level === 1 ? 0 : 18;
    const number = String(entry.page + 1);
    const numberX = MARGIN_X + CONTENT_W - textWidth(number, 'F1', size);
    const titleEnd = drawRuns(plain(entry.text, font), MARGIN_X + indent, y, size, entry.level === 1 ? INK : '0.25 0.25 0.25');
    const dots = Math.max(0, Math.floor((numberX - titleEnd - 8) / textWidth('.', 'F1', size)));
    drawRuns(plain('.'.repeat(dots)), titleEnd + 4, y, size, '0.65 0.65 0.65');
    drawRuns(plain(number), numberX, y, size);
    page.annots.push({ rect: [MARGIN_X, y - 4, MARGIN_X + CONTENT_W, y + size], target: entry });
  }
}

function renderFooters() {
  pages.forEach((p, index) => {
    if (index === 0) return; // portada sin numeración
    page = p;
    const label = `Página ${index + 1} de ${pages.length}`;
    page.ops.push(`0.82 0.82 0.82 RG 0.5 w ${MARGIN_X} 46 m ${MARGIN_X + CONTENT_W} 46 l S`);
    drawRuns(plain(FOOTER), MARGIN_X, 33, 8, MUTED);
    drawRuns(plain(label), MARGIN_X + CONTENT_W - textWidth(label, 'F1', 8), 33, 8, MUTED);
  });
}

const blocks = parseMarkdown(fs.readFileSync(SOURCE, 'utf8'));
let tocPages = 1;
let tocStart = layout(blocks, tocPages);
const needed = tocPagesNeeded(headings.length);
if (needed !== tocPages) { tocPages = needed; tocStart = layout(blocks, tocPages); }
renderToc(tocStart);
renderFooters();

if (missingGlyphs.size) {
  console.error(`Glifos ausentes en las fuentes:\n  ${[...missingGlyphs].join('\n  ')}`);
  process.exit(1);
}

// ── Serialización PDF ───────────────────────────────────────────────────────
const objects = [];
const reserve = () => { objects.push(null); return objects.length; };
const set = (id, body) => { objects[id - 1] = body; return id; };
const add = (body) => set(reserve(), body);
const stream = (dict, data) => {
  const compressed = zlib.deflateSync(Buffer.isBuffer(data) ? data : Buffer.from(data, 'latin1'));
  return { dict: `${dict} /Filter /FlateDecode /Length ${compressed.length}`, data: compressed };
};

const catalogId = reserve();
const pagesId = reserve();
const outlinesId = reserve();

const fontRefs = {};
for (const [key, font] of Object.entries(FONTS)) {
  const fileId = add(stream(`<< /Length1 ${font.data.length}`, font.data));
  const descriptorId = add(`<< /Type /FontDescriptor /FontName /${font.name} /Flags 32 /FontBBox [${font.bbox.join(' ')}] /ItalicAngle 0 /Ascent ${font.ascent} /Descent ${font.descent} /CapHeight ${font.capHeight} /StemV 80 /FontFile2 ${fileId} 0 R >>`);
  const gids = [...font.used.keys()].sort((a, b) => a - b);
  const widths = gids.map((g) => `${g} [${font.advances[g]}]`).join(' ');
  const cidId = add(`<< /Type /Font /Subtype /CIDFontType2 /BaseFont /${font.name} /CIDSystemInfo << /Registry (Adobe) /Ordering (Identity) /Supplement 0 >> /FontDescriptor ${descriptorId} 0 R /CIDToGIDMap /Identity /DW 1000 /W [${widths}] >>`);
  const mappings = gids.filter((g) => g !== 0).map((g) => {
    const cp = font.used.get(g);
    const utf16 = String.fromCodePoint(cp).split('').map((u) => u.charCodeAt(0).toString(16).padStart(4, '0')).join('');
    return `<${g.toString(16).padStart(4, '0')}> <${utf16}>`;
  });
  const chunks = [];
  for (let i = 0; i < mappings.length; i += 100) chunks.push(`${Math.min(100, mappings.length - i)} beginbfchar\n${mappings.slice(i, i + 100).join('\n')}\nendbfchar`);
  const cmapText = `/CIDInit /ProcSet findresource begin\n12 dict begin\nbegincmap\n/CIDSystemInfo << /Registry (Adobe) /Ordering (UCS) /Supplement 0 >> def\n/CMapName /Adobe-Identity-UCS def\n/CMapType 2 def\n1 begincodespacerange\n<0000> <FFFF>\nendcodespacerange\n${chunks.join('\n')}\nendcmap\nCMapName currentdict /CMap defineresource pop\nend\nend`;
  const toUnicodeId = add(stream('<<', cmapText));
  fontRefs[key] = add(`<< /Type /Font /Subtype /Type0 /BaseFont /${font.name} /Encoding /Identity-H /DescendantFonts [${cidId} 0 R] /ToUnicode ${toUnicodeId} 0 R >>`);
}
const fontResources = Object.entries(fontRefs).map(([key, id]) => `/${key} ${id} 0 R`).join(' ');

const pageIds = pages.map(() => reserve());
const dest = (entry) => `[${pageIds[entry.page]} 0 R /XYZ ${MARGIN_X - 10} ${entry.top.toFixed(2)} 0]`;
pages.forEach((p, index) => {
  const contentId = add(stream('<<', p.ops.join('\n')));
  const annots = p.annots.map((a) => add(`<< /Type /Annot /Subtype /Link /Rect [${a.rect.map((v) => v.toFixed(2)).join(' ')}] /Border [0 0 0] /Dest ${dest(a.target)} >>`));
  const annotsEntry = annots.length ? ` /Annots [${annots.map((id) => `${id} 0 R`).join(' ')}]` : '';
  set(pageIds[index], `<< /Type /Page /Parent ${pagesId} 0 R /MediaBox [0 0 ${PAGE_W} ${PAGE_H}] /Resources << /Font << ${fontResources} >> >> /Contents ${contentId} 0 R${annotsEntry} >>`);
});

// Outline (marcadores): nivel 1 = secciones, nivel 2 = subsecciones.
const tops = [];
for (const h of headings) {
  if (h.level === 1) tops.push({ ...h, children: [] });
  else if (tops.length) tops[tops.length - 1].children.push(h);
}
tops.forEach((t) => { t.id = reserve(); t.children.forEach((c) => { c.id = reserve(); }); });
const link = (list, i) => `${i > 0 ? ` /Prev ${list[i - 1].id} 0 R` : ''}${i < list.length - 1 ? ` /Next ${list[i + 1].id} 0 R` : ''}`;
tops.forEach((t, i) => {
  const kids = t.children.length ? ` /First ${t.children[0].id} 0 R /Last ${t.children[t.children.length - 1].id} 0 R /Count -${t.children.length}` : '';
  set(t.id, `<< /Title ${utf16Hex(t.text)} /Parent ${outlinesId} 0 R /Dest ${dest(t)}${link(tops, i)}${kids} >>`);
  t.children.forEach((c, j) => set(c.id, `<< /Title ${utf16Hex(c.text)} /Parent ${t.id} 0 R /Dest ${dest(c)}${link(t.children, j)} >>`));
});
set(outlinesId, `<< /Type /Outlines /First ${tops[0].id} 0 R /Last ${tops[tops.length - 1].id} 0 R /Count ${tops.length} >>`);
set(pagesId, `<< /Type /Pages /Kids [${pageIds.map((id) => `${id} 0 R`).join(' ')}] /Count ${pageIds.length} >>`);
set(catalogId, `<< /Type /Catalog /Pages ${pagesId} 0 R /Outlines ${outlinesId} 0 R /PageMode /UseOutlines /Lang (es) >>`);
const infoId = add(`<< /Title ${utf16Hex(`${COVER.kicker}: ${COVER.title}`)} /Subject ${utf16Hex(COVER.subtitle)} /Producer (docs/informe-fuente/generar-informe-pdf.js) >>`);

const chunks = [Buffer.from('%PDF-1.7\n%\xE2\xE3\xCF\xD3\n', 'latin1')];
let offset = chunks[0].length;
const offsets = [];
objects.forEach((obj, index) => {
  offsets.push(offset);
  const parts = typeof obj === 'string'
    ? [Buffer.from(`${index + 1} 0 obj\n${obj}\nendobj\n`, 'latin1')]
    : [Buffer.from(`${index + 1} 0 obj\n${obj.dict} >>\nstream\n`, 'latin1'), obj.data, Buffer.from('\nendstream\nendobj\n', 'latin1')];
  for (const part of parts) { chunks.push(part); offset += part.length; }
});
let xref = `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
offsets.forEach((o) => { xref += `${String(o).padStart(10, '0')} 00000 n \n`; });
xref += `trailer\n<< /Size ${objects.length + 1} /Root ${catalogId} 0 R /Info ${infoId} 0 R >>\nstartxref\n${offset}\n%%EOF\n`;
chunks.push(Buffer.from(xref, 'latin1'));

fs.mkdirSync(path.dirname(OUTPUT), { recursive: true });
fs.writeFileSync(OUTPUT, Buffer.concat(chunks));
process.stdout.write(`PDF generado: ${path.relative(ROOT, OUTPUT)} (${pages.length} páginas, ${headings.length} entradas de índice)\n`);
