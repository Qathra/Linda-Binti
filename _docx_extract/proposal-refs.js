const fs = require("fs");
const xml = fs.readFileSync("proposal/uz/word/document.xml", "utf8");
const paras = xml.split(/<w:p[ >]/).slice(1);
const text = (p) => [...p.matchAll(/<w:t[^>]*>([^<]*)<\/w:t>/g)].map((m) => m[1]).join("");
const lines = paras.map(text).map((s) => s.replace(/\s+/g, " ").trim()).filter(Boolean);
const i = lines.findIndex((s) => /references/i.test(s));
console.log("paras", lines.length, "refIdx", i, i >= 0 ? lines[i] : "");
fs.writeFileSync("proposal-tail.txt", lines.slice(i < 0 ? -40 : i).join("\n"), "utf8");
