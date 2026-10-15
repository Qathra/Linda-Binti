const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
const paras = xml.split(/<w:p[ >]/).slice(1);
const text = (p) => [...p.matchAll(/<w:t[^>]*>([^<]*)<\/w:t>/g)].map((m) => m[1]).join(" ").replace(/\s+/g, " ").trim();
const want = /Research Questions|class diagram is a structural|Table 6.1|7.11 Discussion|REFERENCES|Figure 5.9:|Not separately recorded|seven chapters/;
paras.forEach((p, i) => {
  const t = text(p);
  if (want.test(t)) console.log(i, t.slice(0, 160));
});
const s = xml.indexOf("<w:sectPr");
console.log(xml.slice(s, s + 280));
