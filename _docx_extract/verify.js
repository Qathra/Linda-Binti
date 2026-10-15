const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
const rels = fs.readFileSync("unzipped/word/_rels/document.xml.rels", "utf8");
const checks = [
  "1.4 Research Questions",
  "A class diagram is a structural UML diagram",
  "Figure 5.9: Login and Access Wireframe",
  "Figure 5.9 shows this design-stage wireframe",
  "Table 6.1: Development Environment",
  "Table 7.2: Functional and Backend Test Cases",
  "Not separately recorded",
  "7.11 Discussion",
  "7.12 Recommendations",
  "7.13 Conclusion",
  "REFERENCES",
  "Davis, F. D. (1989)",
  "into seven chapters",
  "low-fidelity design-stage wireframes",
  "Focus-group testing was not reported",
  "w:type=\"first\""
];
for (const c of checks) console.log(xml.includes(c) ? "OK" : "MISSING", c);
console.log("old login embed", xml.includes('r:embed="rId35"'));
console.log("wf-login rel", rels.includes("wf-login.png"));
console.log("ampamp", (xml.match(/&amp;amp;/g) || []).length);
console.log("unclosed", (xml.match(/<w:p[ >]/g) || []).length, (xml.match(/<\/w:p>/g) || []).length);
console.log("tbl", (xml.match(/<w:tbl>/g) || []).length, (xml.match(/<\/w:tbl>/g) || []).length);
