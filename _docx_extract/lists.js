const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
const paras = xml.split(/<w:p[ >]/).slice(1);
const text = (p) => [...p.matchAll(/<w:t[^>]*>([^<]*)<\/w:t>/g)].map((m) => m[1]).join("").replace(/\s+/g, " ").trim();
for (let i = 227; i < 265; i++) console.log(i, text(paras[i]).slice(0, 140));
const idx = xml.indexOf("Summary of System Test Results");
console.log("\nTABLE SNIP\n", xml.slice(idx - 400, idx + 200).replace(/></g, ">\n<").slice(0, 1500));
