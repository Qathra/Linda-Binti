const fs = require("fs");
const { execFileSync } = require("child_process");
const stylesPath = "C:\\Users\\hp\\Desktop\\LINDA BINTI FOLDER\\_abbr_scan\\styles.xml";
if (!fs.existsSync(stylesPath)) {
  execFileSync("powershell", ["-NoProfile", "-Command",
    "Add-Type -AssemblyName System.IO.Compression.FileSystem; $z=[IO.Compression.ZipFile]::OpenRead('C:\\Users\\hp\\Desktop\\LINDA BINTI FINAL DOCUMENTATION.docx'); $e=$z.GetEntry('word/styles.xml'); $s=New-Object IO.FileStream('" + stylesPath.replace(/\\/g,"\\\\") + "', 'Create'); $e.Open().CopyTo($s); $s.Close(); $z.Dispose()"
  ], { stdio: "inherit" });
}
const xml = fs.readFileSync(stylesPath, "utf8");
for (const name of ["Normal", "Heading1"]) {
  const i = xml.indexOf('w:styleId="' + name + '"');
  console.log("\n==== " + name + " ====\n" + xml.slice(i, i + 900));
}
