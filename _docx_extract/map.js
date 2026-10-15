const fs = require("fs");
const rels = fs.readFileSync("unzipped/word/_rels/document.xml.rels", "utf8");
const pairs = [...rels.matchAll(/Id="(rId\d+)"[^>]*Target="media\/([^"]+)"/g)];
const map = Object.fromEntries(pairs.map(m => [m[1], m[2]]));
const ids = [];
for (let i = 26; i <= 59; i++) ids.push("rId" + i);
const lines = ids.map(id => `${id}\t${map[id] || "MISSING"}`);
fs.writeFileSync("media-map.txt", lines.join("\n"));
console.log(lines.join("\n"));
