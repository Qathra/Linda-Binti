const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
const paras = xml.split(/<w:p[ >]/).slice(1);
function textOf(p) {
  const parts = [];
  const re = /<w:t[^>]*>([^<]*)<\/w:t>/g;
  let m;
  while ((m = re.exec(p))) parts.push(m[1]);
  return parts.join("").replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">");
}
function styleOf(p) {
  const m = p.match(/<w:pStyle w:val="([^"]+)"/);
  return m ? m[1] : "";
}
const lines = [];
paras.forEach((p, i) => {
  const style = styleOf(p);
  const text = textOf(p).replace(/\s+/g, " ").trim();
  const hasDrawing = p.includes("<w:drawing") || p.includes("<w:pict");
  if (!text && !hasDrawing) return;
  const interesting =
    /Heading|Title|Caption|TOC/i.test(style) ||
    /^(CHAPTER|Chapter|Figure|Table|ABSTRACT|Abstract|REFERENCES|References)/.test(text) ||
    /wireframe|Wireframe|Flutter|Firebase|Google Maps|Leaflet|research question|Class Diagram|functional test|questionnaire|focus group|USSD|Discussion|Recommendation|Conclusion/i.test(text);
  if (interesting || /Heading|Caption/i.test(style)) {
    lines.push(`${i}\t${style}\t${hasDrawing ? "[IMG] " : ""}${text.slice(0, 220)}`);
  }
});
fs.writeFileSync("outline.txt", lines.join("\n"), "utf8");
console.log("paras", paras.length, "lines", lines.length);
