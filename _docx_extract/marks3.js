const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
let i = 0, n = 0;
while ((i = xml.indexOf("emergency message", i)) !== -1 && n < 8) {
  console.log(JSON.stringify(xml.slice(i - 80, i + 40)));
  console.log("---");
  i += 10; n++;
}
const j = xml.indexOf("authorisation.");
console.log("AUTH", JSON.stringify(xml.slice(j - 40, j + 30)));
