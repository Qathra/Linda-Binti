const fs = require("fs");
const xml = fs.readFileSync("c:/Users/hp/Desktop/LINDA BINTI FOLDER/docs/_document.xml", "utf8");
const paras = xml.split(/<w:p[ >]/).slice(1);
let n = 0;
paras.forEach((p, idx) => {
  const texts = [];
  const reT = /<w:t[^>]*>([^<]*)<\/w:t>/g;
  let m;
  while ((m = reT.exec(p))) texts.push(m[1]);
  let text = texts.join("").replace(/&amp;/g, "&").replace(/\s+/g, " ").trim();
  if (!/Figure/i.test(text)) return;
  n++;
  const start = /^(Figure|Table)\s+\d/.test(text);
  if (!start) console.log(String(idx + 1).padStart(4), text.slice(0, 240));
});
console.log("totalFigureParas", n);
