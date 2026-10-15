const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
const i = xml.indexOf("server-managed education content");
console.log(JSON.stringify(xml.slice(i, i + 80)));
