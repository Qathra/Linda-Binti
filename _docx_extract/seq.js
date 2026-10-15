const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
const i = xml.indexOf("returns a response");
console.log("idx", i);
if (i > 0) {
  const start = xml.lastIndexOf("<w:p ", i);
  const end = xml.indexOf("</w:p>", i) + 6;
  const p = xml.slice(start, end);
  const texts = [...p.matchAll(/<w:t[^>]*>([^<]*)<\/w:t>/g)].map((m) => m[1]);
  console.log(texts.join(" | "));
}
