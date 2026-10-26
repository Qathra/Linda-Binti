const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
for (const n of ["synchronisation status", "synchronization status", "local synchron"]) {
  const i = xml.indexOf(n);
  console.log("\n", n, i);
  if (i >= 0) console.log(xml.slice(i - 60, i + 80));
}
