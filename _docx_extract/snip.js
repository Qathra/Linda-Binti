const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
const i = xml.toLowerCase().indexOf("six chapter");
console.log("idx", i);
console.log(xml.slice(i - 80, i + 120));
const j = xml.indexOf("Figure 5.9");
console.log("\nFIG", xml.slice(j - 30, j + 80));
