const fs = require("fs");
const xml = fs.readFileSync("proposal/uz/word/document.xml", "utf8");
const paras = xml.split(/<w:p[ >]/).slice(1);
const text = (p) => [...p.matchAll(/<w:t[^>]*>([^<]*)<\/w:t>/g)].map((m) => m[1]).join("").replace(/\s+/g, " ").trim();
const lines = paras.map(text).filter(Boolean);
const keys = ["Sommerville", "Pressman", "Beck", "WHO", "UNICEF", "Davis", "Rogers", "REFERENCES", "References"];
lines.forEach((s, i) => {
  if (keys.some((k) => s.includes(k))) console.log(i, s.slice(0, 180));
});
