const fs = require("fs");
const path = require("path");

const root = "c:/Users/hp/Desktop/LINDA BINTI FOLDER/_docx_extract/unzipped";
const imgDir = "c:/Users/hp/Desktop/Linda Binti Wireframe Images";
let xml = fs.readFileSync(path.join(root, "word/document.xml"), "utf8");
let rels = fs.readFileSync(path.join(root, "word/_rels/document.xml.rels"), "utf8");
let types = fs.readFileSync(path.join(root, "[Content_Types].xml"), "utf8");

function esc(s) {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
function count(hay, needle) {
  let n = 0, i = 0;
  while ((i = hay.indexOf(needle, i)) !== -1) { n++; i += needle.length; }
  return n;
}
function mustOne(needle) {
  const n = count(xml, needle);
  if (n !== 1) throw new Error(`Expected 1 of [${needle}] found ${n}`);
}
function insertAfter(marker, insertion) {
  mustOne(marker);
  const at = xml.indexOf(marker);
  const end = xml.indexOf("</w:p>", at) + 6;
  xml = xml.slice(0, end) + insertion + xml.slice(end);
}
function nthIndex(hay, needle, n) {
  let i = -1;
  for (let k = 0; k < n; k++) {
    i = hay.indexOf(needle, i + 1);
    if (i < 0) throw new Error(`Missing occurrence ${n} of [${needle}]`);
  }
  return i;
}
function insertBeforeParagraph(marker, insertion, occurrence) {
  const at = occurrence ? nthIndex(xml, marker, occurrence) : (mustOne(marker), xml.indexOf(marker));
  const start = xml.lastIndexOf("<w:p ", at);
  xml = xml.slice(0, start) + insertion + xml.slice(start);
}
function replaceOnce(oldS, newS) {
  mustOne(oldS);
  xml = xml.replace(oldS, newS);
}
function paraStart(marker) {
  mustOne(marker);
  const at = xml.indexOf(marker);
  const a = xml.lastIndexOf("<w:p ", at);
  const b = xml.lastIndexOf("<w:p>", at);
  return Math.max(a, b);
}
function replaceParagraph(marker, replacement) {
  const start = paraStart(marker);
  const at = xml.indexOf(marker);
  const end = xml.indexOf("</w:p>", at) + 6;
  xml = xml.slice(0, start) + replacement + xml.slice(end);
}
function deleteParagraph(marker) {
  replaceParagraph(marker, "");
}

const RPR = `<w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/></w:rPr>`;
function pBody(text) {
  return `<w:p><w:pPr><w:spacing w:line="360" w:lineRule="auto"/><w:jc w:val="both"/>${RPR}</w:pPr><w:r>${RPR}<w:t xml:space="preserve">${esc(text)}</w:t></w:r></w:p>`;
}
function pHeading(style, text) {
  return `<w:p><w:pPr><w:pStyle w:val="${style}"/><w:spacing w:line="360" w:lineRule="auto"/><w:jc w:val="both"/>${RPR}<w:b/><w:bCs/></w:pPr><w:r>${RPR}<w:b/><w:bCs/><w:t xml:space="preserve">${esc(text)}</w:t></w:r></w:p>`;
}

let picId = 8000;
const relIds = [...rels.matchAll(/Id="rId(\d+)"/g)].map((m) => Number(m[1]));
let nextRid = Math.max(...relIds) + 1;
function addImageRel(fileName) {
  const id = "rId" + nextRid++;
  const rel = `<Relationship Id="${id}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/${fileName}"/>`;
  rels = rels.replace("</Relationships>", rel + "</Relationships>");
  return id;
}
function pngSize(file) {
  const buf = fs.readFileSync(file);
  return { w: buf.readUInt32BE(16), h: buf.readUInt32BE(20) };
}
function extentFor(file) {
  const { w, h } = pngSize(file);
  const maxW = Math.round(6.3 * 914400);
  const maxH = Math.round(8.6 * 914400);
  let cx = maxW;
  let cy = Math.round(cx * (h / w));
  if (cy > maxH) {
    cy = maxH;
    cx = Math.round(cy * (w / h));
  }
  return { cx, cy };
}
function imageParagraph(fileName, relId, filePath) {
  const { cx, cy } = extentFor(filePath);
  const id = picId++;
  return `<w:p><w:pPr><w:jc w:val="center"/>${RPR}</w:pPr><w:r>${RPR}<w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="${cx}" cy="${cy}"/><wp:effectExtent l="0" t="0" r="0" b="0"/><wp:docPr id="${id}" name="Wireframe ${id}"/><wp:cNvGraphicFramePr><a:graphicFrameLocks xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" noChangeAspect="1"/></wp:cNvGraphicFramePr><a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:nvPicPr><pic:cNvPr id="${id}" name="${fileName}"/><pic:cNvPicPr><a:picLocks noChangeAspect="1"/></pic:cNvPicPr></pic:nvPicPr><pic:blipFill><a:blip r:embed="${relId}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="${cx}" cy="${cy}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>`;
}
function useWire(srcName, mediaName) {
  const src = path.join(imgDir, srcName);
  if (!fs.existsSync(src)) throw new Error("missing image " + src);
  fs.copyFileSync(src, path.join(root, "word/media", mediaName));
  const id = addImageRel(mediaName);
  return imageParagraph(mediaName, id, src);
}

const questions = [
  pHeading("Heading2", "1.4 Research Questions"),
  pBody("The study was guided by the following research questions. Each question corresponds directly to one of the research objectives stated above."),
  pBody("1. What challenges affect the reporting of and response to gender-based violence and female genital mutilation in Kajiado County?"),
  pBody("2. How do existing digital solutions support the prevention of and response to gender-based violence and female genital mutilation?"),
  pBody("3. How can the Linda Binti application be developed as an AI-powered system for the prevention of and response to gender-based violence and female genital mutilation in Kajiado County?"),
  pBody("4. Does the developed Linda Binti application meet the identified needs and the intended functionality?")
].join("");
insertBeforeParagraph("1.5 Justification of the Study", questions, 2);

replaceOnce(" into six chapters.", " into seven chapters.");
replaceOnce(
  "Chapter Six presented the development and testing of the Linda Binti system and discussed the implemented system and its performance.",
  "Chapter Six presented the development of the Linda Binti system. Chapter Seven presented the testing and evaluation of the system, together with the discussion, recommendations and conclusion."
);
replaceOnce(
  "four research objectives, justification of the study",
  "four research objectives, the corresponding research questions, justification of the study"
);

insertAfter(
  "dashboard information.",
  pBody("A class diagram is a structural UML diagram that shows the main classes in a system and the relationships between them (Sommerville, 2016). It is important in this design because it sets out, before coding, how Linda Binti organises users, reports, trusted contacts, risk assessments, education progress and SOS events in the mobile application and in the backend. Figure 5.1 presents the class diagram. The diagram reduces inconsistency between the local Room records and the server-side records.")
);
insertAfter(
  "local rules-based processing.",
  pBody("A component diagram is a structural UML diagram that shows the major software parts of a system and how those parts communicate (Pressman & Maxim, 2020). It is used here to show that the Android application, local storage, Ktor backend, SQLite database, AI layer and NGO dashboard are separate components. Figure 5.2 presents the component diagram.")
);
insertAfter(
  "Africa’s Talking gateway for the gateway-ready USSD pathway.",
  pBody("A deployment diagram is a structural UML diagram that shows where the software is placed when the system runs (Sommerville, 2016). It matters for Linda Binti because the Android device, the Ktor server and the browser dashboard are different nodes. Figure 5.3 presents the deployment diagram.")
);
insertAfter(
  "staff management and audit access.",
  pBody("A use case diagram is a behavioural UML diagram that shows who uses the system and which functions each actor can perform (Pressman & Maxim, 2020). Figure 5.4 presents the use case diagram for the application user, NGO staff and the NGO administrator.")
);
insertAfter(
  "synchronised with the server.",
  pBody("An activity diagram is a behavioural UML diagram that shows the order of actions in a process, including the points at which the flow can take a different path (Sommerville, 2016). Figure 5.5 presents the activity diagram for reporting.")
);
insertAfter(
  "returns a response, after which the mobile application updates the local ",
  pBody("A sequence diagram is a behavioural UML diagram that shows the order of messages between parts of the system during one interaction (Pressman & Maxim, 2020). Figure 5.6 presents the sequence diagram for report synchronisation.")
);
replaceOnce("The ERD will distinguish", "The ERD distinguishes");
insertAfter(
  "server-managed education content.",
  pBody("An entity relationship diagram shows the data items that must be stored and how those items are related before they are implemented as tables (Sommerville, 2016). Figure 5.7 presents the entity relationship diagram, and Figure 5.8 shows how those entities map to relational tables.")
);

replaceOnce(
  "The interface was designed using Jetpack Compose and Material 3.",
  "The interface was designed using Jetpack Compose and Material 3. The screen figures in this section are low-fidelity design-stage wireframes of the Linda Binti layout. They are not screenshots of the completed application."
);
replaceOnce("Figure 5.9 login and access wireframe", "Figure 5.9: Login and Access Wireframe");

const figureNotes = [
  ["while a separate SOS Code initiates the emergency process.", " Figure 5.9 shows this design-stage wireframe."],
  ["A Hide function allows the user to return to the calculator-style launcher.", " Figure 5.10 shows this design-stage wireframe."],
  ["The design includes topic selection, educational slides, quizzes and progress information.", " Figure 5.11 shows this design-stage wireframe."],
  ["Selected report information may be synchronised with the backend when the required authentication, network and consent conditions are satisfied.", " Figure 5.12 shows this design-stage wireframe."],
  ["The Safety Compass presents the resulting safety summary together with indicators, concerns, protective factors and suggested next steps.", " Figure 5.13 shows these design-stage wireframes."],
  ["a location fix may be included in the emergency message.", " Figure 5.14 shows these design-stage wireframes."],
  ["The module is designed as an information and referral feature and does not present itself as a counselling, diagnostic or therapeutic service.", " Figure 5.15 shows this design-stage wireframe."],
  ["If the online service is unavailable, the application uses its local rules-based fallback.", " Figure 5.16 shows this design-stage wireframe."],
  ["allows the user to manage privacy-related settings.", " Figure 5.17 shows this design-stage wireframe."],
  ["Case statuses include NEW, REVIEWING, REFERRED, RESPONDING and RESOLVED.", " Figure 5.19 shows the design-stage wireframe of the dashboard."],
  ["were outside the delivered project scope.", " Figure 5.20 shows the design-stage wireframe of the USSD menu. The project did not implement a live public shortcode."],
  ["This separation was designed to reduce unnecessary exposure of sensitive survivor information.", " Figure 5.21 shows this authorisation design. It is a design diagram, not a screenshot of the running system."]
];
for (const [a, b] of figureNotes) replaceOnce(a, a + b);
insertAfter(
  "while protected endpoints require appropriate authentication and role ",
  pBody("Figure 5.18 shows this service design. It is a design diagram, not a screenshot of the running system.")
);

function cell(text, width, bold) {
  const b = bold ? "<w:b/><w:bCs/>" : "";
  return `<w:tc><w:tcPr><w:tcW w:w="${width}" w:type="dxa"/></w:tcPr><w:p><w:pPr><w:spacing w:line="240" w:lineRule="auto"/>${RPR}</w:pPr><w:r>${RPR}${b}<w:t xml:space="preserve">${esc(text)}</w:t></w:r></w:p></w:tc>`;
}
function table(widths, rows) {
  const total = widths.reduce((a, b) => a + b, 0);
  const grid = widths.map((w) => `<w:gridCol w:w="${w}"/>`).join("");
  const trs = rows.map((row, ri) => {
    const cells = row.map((c, i) => cell(c, widths[i], ri === 0)).join("");
    return `<w:tr>${cells}</w:tr>`;
  }).join("");
  return `<w:tbl><w:tblPr><w:tblStyle w:val="TableGrid"/><w:tblW w:w="${total}" w:type="dxa"/><w:tblLook w:val="04A0" w:firstRow="1" w:lastRow="0" w:firstColumn="1" w:lastColumn="0" w:noHBand="0" w:noVBand="1"/></w:tblPr><w:tblGrid>${grid}</w:tblGrid>${trs}</w:tbl>`;
}

const devRows = [
  ["Component", "Implementation recorded in this project"],
  ["Mobile application", "Kotlin 2.0.21, Jetpack Compose, Material 3 and Navigation Compose"],
  ["Android build", "Android Studio, Android Gradle Plugin 8.13.2, minimum SDK 24, compile and target SDK 36, and Java 11"],
  ["Local storage", "Room 2.6.1 on SQLite, with AndroidX Security Crypto for sensitive preferences"],
  ["Mobile networking and location", "Retrofit, OkHttp, Gson, Coil, Google Play Services Location and Android LocationManager"],
  ["Backend", "Kotlin/JVM, Ktor 2.3.12 on Netty, and SQLite JDBC 3.46.1.0"],
  ["Backend security", "JWT authentication, bcrypt password hashing and Logback"],
  ["Dashboard", "HTML, CSS and JavaScript, with Leaflet 1.9.4 and the Kajiado GeoJSON file"],
  ["Deployment files", "Dockerfile and a Render configuration for a hostable web service"]
];
const devTable = pBody("Table 6.1 summarises the development environment used to implement Linda Binti. The entries are limited to the tools and versions stated in this chapter.") +
  pBody("Table 6.1: Development Environment") +
  table([3200, 6160], devRows);
insertAfter("Render configuration for a hostable web service.", devTable);

replaceOnce(
  "including offline operation, missing permissions, invalid access information and unavailable backend services.",
  "including offline operation, missing permissions, invalid access information and unavailable backend services. Section 7.4.1 presents the functional tests of the mobile application. Those tests are part of functional testing and are not repeated under a second heading. Section 7.4.2 presents the backend tests. Table 7.2 lists the test cases. A separate actual result was not recorded for each case in the project evidence, so that field is marked as not separately recorded. The aggregate result remains the one already reported in Table 7.1."
);
replaceOnce(
  "Where final questionnaire statistics or user-acceptance scores were not available, no numerical usability score was assigned.",
  "Where final questionnaire statistics or user-acceptance scores were not available, no numerical usability score was assigned. Focus-group testing was not reported in the project evidence, so no focus-group findings are presented. The questionnaire in Chapter Four was used to gather requirements. It is not treated here as a scored post-implementation test."
);

const caseRows = [
  ["Test Case ID", "Test/Feature", "Test description", "Expected result", "Actual result", "Status"],
  ["TC-01", "Access", "Calculator launcher, Access Code and SOS code, as described in Section 7.4.1.", "Ordinary calculation remains available. The Access Code opens the protected space. The SOS code starts the emergency path. The two codes cannot be the same.", "Not separately recorded. Included in the mobile suite in Table 7.1.", "Not separately recorded"],
  ["TC-02", "Home and education", "Navigation and the eight education topics, as described in Section 7.4.1.", "Modules open from Home. Lessons, quizzes and local progress remain available on the device.", "Not separately recorded. Included in the mobile suite in Table 7.1.", "Not separately recorded"],
  ["TC-03", "Reporting", "Local report storage and synchronisation status, as described in Sections 7.4.1 and 7.5.", "A report is stored locally first and can be synchronised only when authentication, network and consent allow it.", "Not separately recorded. Included in the suites in Table 7.1.", "Not separately recorded"],
  ["TC-04", "Risk and Safety Compass", "Five safety questions and the compass summary, as described in Section 7.4.1.", "Responses are stored locally and a risk band is produced. The compass uses local information already on the phone.", "Not separately recorded. Included in the mobile suite in Table 7.1.", "Not separately recorded"],
  ["TC-05", "Contacts and SOS", "Trusted contacts and GSM SMS, as described in Sections 7.4.1 and 7.6.4.", "Between one and five contacts can be saved. SOS uses SMS and does not include the Access Code, SOS code or report narrative.", "Not separately recorded. Included in the mobile suite in Table 7.1.", "Not separately recorded"],
  ["TC-06", "Support, AI and settings", "Survivor support, offline and configured online AI, and settings, as described in Section 7.4.1.", "Support information is available. The assistant falls back to local rules when online AI is unavailable. Settings include consent and local wipe.", "Not separately recorded. Included in the mobile suite in Table 7.1.", "Not separately recorded"],
  ["TC-07", "Backend authentication and roles", "Registration, login, tokens and USER, NGO_STAFF and NGO_ADMIN access, as described in Section 7.4.2.", "Protected services require authentication. Each role receives only its permitted access.", "Not separately recorded. Included in the backend suite in Table 7.1.", "Not separately recorded"],
  ["TC-08", "Backend reports, AI and dashboard", "Report submission, AI fallback, education, analytics and dashboard endpoints, as described in Section 7.4.2.", "A report can still be saved if the AI provider is unavailable. Dashboard views are aggregate and role-restricted.", "Not separately recorded. Included in the backend suite in Table 7.1.", "Not separately recorded"],
  ["TC-09", "USSD pathway", "Gateway-ready USSD menu, as described in Section 7.4.2.", "The menu supports GBV and FGM reporting and emergency numbers. A live public shortcode is not part of the delivered system.", "Not separately recorded. The document states that live operator USSD was not tested.", "Not separately recorded"]
];
const caseTable = pBody("Table 7.2: Functional and Backend Test Cases") + table([900, 1300, 1900, 2000, 1960, 1300], caseRows);
mustOne("Table 7.1: Summary of System Test Results");
{
  const at = xml.indexOf("Table 7.1: Summary of System Test Results");
  const endTbl = xml.indexOf("</w:tbl>", at) + 8;
  xml = xml.slice(0, endTbl) + caseTable + xml.slice(endTbl);
}

const closing = [
  pHeading("Heading2", "7.11 Discussion"),
  pBody("The discussion is limited to the findings and implementation already presented in this report. The requirement-gathering questionnaire and the literature review identified barriers to reporting, the need for confidentiality, and the value of accessible information on GBV and FGM in Kajiado County. The implemented system responds to those requirements through a discreet Android application, local reporting, education, risk assessment, the Safety Compass, trusted-contact SOS by GSM SMS, survivor support information and an optional hybrid AI assistant."),
  pBody("Chapter Six showed that the system was built with Kotlin and Jetpack Compose on Android, Room for local SQLite storage, and a Ktor backend with its own SQLite database. The NGO dashboard uses HTML, CSS, JavaScript and Leaflet. It does not use Flutter, Firebase or Google Maps for the dashboard. Emergency calls to 999, 112, 1195 and 116 open the phone dialler. The application does not dispatch the police and does not replace counselling, healthcare or legal services."),
  pBody("The testing reported in this chapter recorded 255 mobile tests and 140 backend tests, with no recorded failures in those suites. That result applies only to the tests that were run. Section 7.9 already states that the system was a pilot-scale prototype, that SOS delivery depends on the device and the mobile network, that online AI depends on configuration and consent, and that USSD was gateway-ready rather than a live public shortcode. The discussion therefore does not treat the zero-failure result as evidence of county-wide deployment or of a completed security certification."),
  pHeading("Heading2", "7.12 Recommendations"),
  pBody("The recommendations below follow from the limitations already stated in Sections 1.6 and 7.9 and from the functions that were actually implemented."),
  pBody("1. Any later use beyond this prototype should remain within Kajiado County and should be arranged with the relevant health, community and NGO partners. The present project did not deploy the system county-wide."),
  pBody("2. A live USSD service should be introduced only after a licensed gateway and an operator shortcode are in place. The current menu should continue to be described as gateway-ready."),
  pBody("3. The Somali language option should not be described as complete until a full content pack is added. English, Kiswahili and Maa are the implemented content languages."),
  pBody("4. An independent security review would be appropriate before any wider operational use. The project tested the implemented controls, but it did not include a full penetration test or a security certification."),
  pBody("5. Linda Binti should continue to complement 999, 112, 1195 and 116, and it should not be presented as a replacement for police, clinical, legal or counselling services."),
  pHeading("Heading2", "7.13 Conclusion"),
  pBody("The study set out to investigate challenges affecting the reporting of and response to GBV and FGM in Kajiado County, to examine existing digital approaches, to develop Linda Binti, and to test the developed system. The literature review and the requirement-gathering questionnaire addressed the first two objectives within the limits of the sources and respondents used. The third objective was met by the Android application and Ktor backend described in Chapter Six, including offline-first local functions, GSM SMS to trusted contacts, hybrid AI support and the NGO dashboard."),
  pBody("The fourth objective was addressed through the functional, backend, integration and security checks in this chapter. The recorded automated suites contained 395 tests and no recorded failures. Those results support the claim that the tested functions behaved as implemented. They do not support a claim of live county-wide operation, live public USSD, automatic police integration, or a substitute for professional response services. Within that scope, Linda Binti provides a discreet, locally contextualised prototype for GBV and FGM prevention and response in Kajiado County."),
  pHeading("Heading1", "REFERENCES"),
  pBody("Complete bibliographic details were available in the existing project documents only for the sources listed here. Other in-text citations have been left as they appeared in the report. Their full references were not reconstructed."),
  pBody("Davis, F. D. (1989). Perceived usefulness, perceived ease of use, and user acceptance of information technology. MIS Quarterly, 13(3), 319–340."),
  pBody("Rogers, R. W. (1975). A protection motivation theory of fear appeals and attitude change. The Journal of Psychology, 91(1), 93–114.")
].join("");
insertBeforeParagraph("This chapter presented the testing and evaluation of the Linda Binti application.", closing);
replaceOnce(
  "within the defined project scope.",
  "within the defined project scope. Sections 7.11 to 7.13 discuss those results, state the recommendations that follow from the stated limitations, and conclude against the study objectives."
);

deleteParagraph('r:embed="rId39"');
deleteParagraph('r:embed="rId58"');
replaceParagraph('r:embed="rId35"', useWire("01 Login and Access.png", "wf-login.png"));
replaceParagraph('r:embed="rId41"', useWire("02 Home Screen.png", "wf-home.png"));
replaceParagraph('r:embed="rId43"', useWire("03 Education.png", "wf-education.png"));
replaceParagraph('r:embed="rId44"', useWire("04 Reporting.png", "wf-reporting.png"));
replaceParagraph('r:embed="rId45"', useWire("05 Risk Assessment.png", "wf-risk.png") + useWire("06 Safety Compass.png", "wf-compass.png"));
replaceParagraph('r:embed="rId47"', useWire("07 Emergency Assistance.png", "wf-emergency.png") + useWire("08 Trusted Contacts.png", "wf-contacts.png"));
replaceParagraph('r:embed="rId49"', useWire("09 Survivor Support.png", "wf-support.png"));
replaceParagraph('r:embed="rId51"', useWire("10 AI Assistant.png", "wf-ai.png"));
replaceParagraph('r:embed="rId52"', useWire("11 Settings and Privacy.png", "wf-settings.png"));
replaceParagraph('r:embed="rId55"', useWire("12 NGO Dashboard.png", "wf-ngo.png"));
replaceParagraph('r:embed="rId56"', useWire("13 USSD Interface.png", "wf-ussd.png"));

const footerRid = "rId" + nextRid++;
rels = rels.replace(
  "</Relationships>",
  `<Relationship Id="${footerRid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer3.xml"/></Relationships>`
);
const footer1 = fs.readFileSync(path.join(root, "word/footer1.xml"), "utf8");
const open = footer1.slice(0, footer1.indexOf(">") + 1);
const emptyFooter = open + `<w:p><w:pPr><w:pStyle w:val="Footer"/><w:jc w:val="center"/></w:pPr></w:p></w:ftr>`;
fs.writeFileSync(path.join(root, "word/footer3.xml"), emptyFooter);
if (!types.includes("/word/footer3.xml")) {
  types = types.replace(
    "</Types>",
    `<Override PartName="/word/footer3.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/></Types>`
  );
}
const firstSect = xml.indexOf("<w:sectPr");
const footerRef = `<w:footerReference w:type="first" r:id="${footerRid}"/>`;
if (!xml.slice(firstSect, xml.indexOf("</w:sectPr>", firstSect)).includes('w:type="first"')) {
  xml = xml.slice(0, firstSect) + xml.slice(firstSect).replace("<w:sectPr", "<w:sectPr").replace(
    /<w:sectPr([^>]*)>/,
    `<w:sectPr$1>${footerRef}`
  );
}

fs.writeFileSync(path.join(root, "word/document.xml"), xml);
fs.writeFileSync(path.join(root, "word/_rels/document.xml.rels"), rels);
fs.writeFileSync(path.join(root, "[Content_Types].xml"), types);
console.log("xml bytes", xml.length, "nextRid", nextRid);
