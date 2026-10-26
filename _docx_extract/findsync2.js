const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
let i = 0, n = 0;
while ((i = xml.indexOf("synchron", i)) !== -1 && n < 15) {
  console.log(xml.slice(i - 40, i + 50).replace(/\n/g, " "));
  console.log("---");
  i += 8; n++;
}
console.log("count shown", n);
