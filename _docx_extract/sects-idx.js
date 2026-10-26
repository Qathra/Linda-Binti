const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
const paras = xml.split(/<w:p[ >]/).slice(1);
function textOf(p) {
  return [...p.matchAll(/<w:t[^>]*>([^<]*)<\/w:t>/g)].map(m => m[1]).join("").replace(/\s+/g, " ").trim().slice(0, 80);
}
paras.forEach((p, i) => {
  if (p.includes("<w:sectPr")) console.log(i, textOf(p), "SECT");
});
// sample normal para pPr
const sample = paras.find(p => textOf(p).startsWith("The study was guided"));
const pPr = sample.match(/<w:pPr>[\s\S]*?<\/w:pPr>/);
fs.writeFileSync("ppr-body.txt", pPr ? pPr[0] : "none");
const h2 = paras.find(p => p.includes('w:val="Heading2"') && textOf(p).includes("1.5"));
const hPr = h2.match(/<w:pPr>[\s\S]*?<\/w:pPr>/);
fs.writeFileSync("ppr-h2.txt", hPr ? hPr[0] : "none");
const list = paras.find(p => p.includes("ListParagraph") && textOf(p).startsWith("To investigate"));
fs.writeFileSync("ppr-list.txt", list.match(/<w:pPr>[\s\S]*?<\/w:pPr>/)[0]);
console.log("body len", pPr[0].length, "h2", hPr[0].length);
