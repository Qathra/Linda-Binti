const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
for (const n of ["location fix", "protected endpoints", "role authorisation", "role authorization"]) {
  const i = xml.indexOf(n);
  console.log("\n", n, i);
  if (i >= 0) console.log(JSON.stringify(xml.slice(i - 30, i + 90)));
}
