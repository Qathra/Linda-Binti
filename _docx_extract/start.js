const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
const paras = xml.split(/<w:p[ >]/).slice(1);
function textOf(p) {
  return [...p.matchAll(/<w:t[^>]*>([^<]*)<\/w:t>/g)].map(m => m[1]).join("").replace(/\s+/g, " ").trim();
}
let out = "";
for (let i = 0; i < 30; i++) out += i + " | " + textOf(paras[i]).slice(0, 160) + "\n";
fs.writeFileSync("start.txt", out);
console.log(out);
