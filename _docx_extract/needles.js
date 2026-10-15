const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
const needles = [
  "organised into six chapters",
  "organized into six chapters",
  "Figure 5.9 login and access wireframe",
  "The ERD will distinguish",
  "four research objectives, justification",
  "The interface was designed using Jetpack Compose and Material 3.",
  "1.5 Justification of the Study"
];
for (const n of needles) console.log(n, xml.includes(n));
