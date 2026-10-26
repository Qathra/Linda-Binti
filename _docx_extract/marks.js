const fs = require("fs");
const xml = fs.readFileSync("unzipped/word/document.xml", "utf8");
const markers = [
  "Where permitted and available, a location fix may be included in the emergency message.",
  "does not present itself as a counselling, diagnostic or therapeutic service.",
  "the application uses its local rules-based fallback.",
  "allows the user to manage privacy-related settings.",
  "while protected endpoints require appropriate authentication and role authorisation.",
  "Case statuses include NEW, REVIEWING, REFERRED, RESPONDING and RESOLVED.",
  "were outside the delivered project scope.",
  "reduce unnecessary exposure of sensitive survivor information."
];
for (const m of markers) console.log(xml.includes(m) ? "OK" : "NO", m.slice(0, 70));
