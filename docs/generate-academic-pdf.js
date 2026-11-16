// Regenerates the shareable academic PDF from the reviewed Markdown sources.
const fs = require('node:fs');
const path = require('node:path');

const docsDir = __dirname;
const sources = [
  ['ACTIVIDAD 11 - CELULAR', 'Arquitectura DDD, Hexagonal y CQRS', 'Java 17 | Spring Boot 3.3.5 | MySQL | Spring JDBC'],
  fs.readFileSync(path.join(docsDir, 'actividad-11-celular-ddd-cqrs.md'), 'utf8'),
  fs.readFileSync(path.join(docsDir, 'guion-sustentacion.md'), 'utf8'),
].join('\n\n');

function cleanLine(line) {
  return line
    .replace(/^\s*#+\s*/, '')
    .replace(/^\s*[-*]\s+/, '  - ')
    .replace(/[`*_]/g, '')
    .replace(/\|/g, '   ')
    .replace(/-{3,}/g, '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .replace(/[^\x20-\x7E]/g, '');
}

function wrap(line, max = 108) {
  if (!line.trim()) return [''];
  const words = cleanLine(line).split(/\s+/);
  const lines = [];
  let current = '';
  for (const word of words) {
    if (current && `${current} ${word}`.length > max) {
      lines.push(current);
      current = word;
    } else {
      current = current ? `${current} ${word}` : word;
    }
  }
  if (current) lines.push(current);
  return lines;
}

const lines = sources.split(/\r?\n/).flatMap((line) => wrap(line));
const pages = [];
for (let i = 0; i < lines.length; i += 56) pages.push(lines.slice(i, i + 56));

const objects = new Map();
objects.set(1, '<< /Type /Catalog /Pages 2 0 R >>');
objects.set(3, '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>');
const pageRefs = pages.map((_, index) => `${4 + index * 2} 0 R`).join(' ');
objects.set(2, `<< /Type /Pages /Kids [${pageRefs}] /Count ${pages.length} >>`);

pages.forEach((page, index) => {
  const pageId = 4 + index * 2;
  const contentId = pageId + 1;
  const content = [
    'BT',
    '/F1 9 Tf',
    '48 790 Td',
    '12 TL',
    ...page.map((line) => `(${line.replace(/[\\()]/g, '\\$&')}) Tj T*`),
    'ET',
  ].join('\n');
  objects.set(pageId, `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents ${contentId} 0 R >>`);
  objects.set(contentId, `<< /Length ${Buffer.byteLength(content, 'ascii')} >>\nstream\n${content}\nendstream`);
});

let pdf = '%PDF-1.4\n';
const offsets = [0];
for (let id = 1; id <= objects.size; id += 1) {
  offsets[id] = Buffer.byteLength(pdf, 'ascii');
  pdf += `${id} 0 obj\n${objects.get(id)}\nendobj\n`;
}
const xrefOffset = Buffer.byteLength(pdf, 'ascii');
pdf += `xref\n0 ${objects.size + 1}\n0000000000 65535 f \n`;
for (let id = 1; id <= objects.size; id += 1) {
  pdf += `${String(offsets[id]).padStart(10, '0')} 00000 n \n`;
}
pdf += `trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n${xrefOffset}\n%%EOF\n`;

const output = path.join(docsDir, 'Actividad-11-Celular-DDD-CQRS.pdf');
fs.writeFileSync(output, Buffer.from(pdf, 'ascii'));
process.stdout.write(`PDF generado: ${output} (${pages.length} paginas)\n`);
