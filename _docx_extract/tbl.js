const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
const i = xml.indexOf("<w:tbl>");
const j = xml.indexOf("</w:tbl>", i);
fs.writeFileSync("table-sample.xml", xml.slice(i, j + 8).replace(/></g, ">\n<"));
console.log("len", j - i);
