const fs = require("fs");
const path = require("path");
const puppeteer = require("puppeteer-core");

const dir = __dirname;
const out = "C:\\Users\\hp\\Desktop\\Linda Binti Wireframe Images";
fs.mkdirSync(out, { recursive: true });

const files = [
  ["01-login-and-access.html", "01 Login and Access.png"],
  ["02-home-screen.html", "02 Home Screen.png"],
  ["03-education.html", "03 Education.png"],
  ["04-reporting.html", "04 Reporting.png"],
  ["05-risk-assessment.html", "05 Risk Assessment.png"],
  ["06-safety-compass.html", "06 Safety Compass.png"],
  ["07-emergency-assistance.html", "07 Emergency Assistance.png"],
  ["08-trusted-contacts.html", "08 Trusted Contacts.png"],
  ["09-survivor-support.html", "09 Survivor Support.png"],
  ["10-ai-assistant.html", "10 AI Assistant.png"],
  ["11-settings-and-privacy.html", "11 Settings and Privacy.png"],
  ["12-ngo-dashboard.html", "12 NGO Dashboard.png"],
  ["13-ussd-interface.html", "13 USSD Interface.png"],
  ["14-backend-security-authorisation.html", "14 Backend Security and Authorisation.png"]
];

(async () => {
  const browser = await puppeteer.launch({
    executablePath: "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
    headless: true,
    args: ["--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=2"]
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 900, deviceScaleFactor: 2 });
  for (const [file, name] of files) {
    const url = "file:///" + path.join(dir, file).replace(/\\/g, "/");
    await page.goto(url, { waitUntil: "networkidle0" });
    await page.addStyleTag({ content: ".sheet{width:max-content!important;padding:8px!important;}" });
    const sheet = await page.$("article.sheet");
    const target = path.join(out, name);
    await sheet.screenshot({ path: target });
    console.log(name);
  }
  await browser.close();
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
