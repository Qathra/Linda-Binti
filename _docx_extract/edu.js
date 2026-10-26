const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
const i = xml.indexOf("EducationContent");
console.log(xml.slice(i, i + 120));
console.log("---codes", [...xml.slice(i, i + 40)].map((c) => c.charCodeAt(0)).join(","));
