const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
const paras = xml.split(/<w:p[ >]/).slice(1);
function textOf(p) {
  const parts = [];
  const re = /<w:t[^>]*>([^<]*)<\/w:t>/g;
  let m;
  while ((m = re.exec(p))) parts.push(m[1]);
  return parts.join("").replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, '"');
}
function styleOf(p) {
  const m = p.match(/<w:pStyle w:val="([^"]+)"/);
  return m ? m[1] : "";
}
const ranges = [[280, 315], [700, 780], [850, 980], [980, 1060], [1145, 1246]];
let out = "";
paras.forEach((p, i) => {
  if (!ranges.some(([a, b]) => i >= a && i <= b)) return;
  const text = textOf(p).replace(/\s+/g, " ").trim();
  const style = styleOf(p);
  const img = (p.includes("<w:drawing") || p.includes("<w:pict")) ? " [IMG]" : "";
  const blip = (p.match(/r:embed="([^"]+)"/g) || []).join(",");
  if (!text && !img) return;
  out += `${i}\t${style}${img}${blip ? " " + blip : ""}\t${text}\n`;
});
fs.writeFileSync("focus.txt", out, "utf8");

const cites = new Set();
const all = paras.map(textOf).join("\n");
const re = /\(([A-Z][A-Za-z.&'\- ]{1,40}(?:,\s*\d{4}[a-z]?)(?:;\s*[A-Z][A-Za-z.&'\- ]{1,40},\s*\d{4}[a-z]?)*)\)/g;
let m;
while ((m = re.exec(all))) cites.add(m[1]);
fs.writeFileSync("cites.txt", [...cites].sort().join("\n"), "utf8");
console.log("cites", cites.size);

const sect = xml.match(/<w:sectPr[\s\S]*?<\/w:sectPr>/g) || [];
fs.writeFileSync("sects.txt", sect.join("\n\n====\n\n"), "utf8");
console.log("sects", sect.length);
