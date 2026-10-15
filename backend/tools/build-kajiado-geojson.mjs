/**
 * Build dashboard/kajiado.geojson from official geoBoundaries (HDX / IEBC / RCMRD).
 * Source: https://www.geoboundaries.org — Kenya ADM1 (counties) + ADM2 (sub-counties).
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "..");
const toolsDir = path.join(root, "tools");
const outPath = path.join(root, "src/main/resources/dashboard/kajiado.geojson");
const existingPath = outPath;

const SUB_META = {
  "Kajiado North": { code: "kajiado_north", hq: "Ngong / Ongata Rongai" },
  "Kajiado East": { code: "kajiado_east", hq: "Kitengela / Isinya" },
  "Kajiado Central": { code: "kajiado_central", hq: "Kajiado Town" },
  "Kajiado West": { code: "kajiado_west", hq: "Magadi" },
  "Kajiado South": { code: "kajiado_south", hq: "Loitokitok" }
};

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

function pickKajiado(features) {
  return features.filter((f) => /kajiado/i.test(f.properties?.shapeName || ""));
}

const adm1 = readJson(path.join(toolsDir, "ken-adm1.geojson"));
const adm2 = readJson(path.join(toolsDir, "ken-adm2.geojson"));
const existing = readJson(existingPath);
const towns = existing.features.filter((f) => f.properties?.kind === "town");

const countySrc = pickKajiado(adm1.features);
if (countySrc.length !== 1) {
  throw new Error(`Expected 1 Kajiado county in ADM1, found ${countySrc.length}`);
}

const subSrc = pickKajiado(adm2.features);
if (subSrc.length !== 5) {
  throw new Error(`Expected 5 Kajiado sub-counties in ADM2, found ${subSrc.length}`);
}

const countyFeature = {
  type: "Feature",
  properties: {
    kind: "county",
    name: "Kajiado County",
    code: "kajiado",
    shapeISO: countySrc[0].properties.shapeISO,
    source: "geoBoundaries KEN ADM1 (RCMRD GeoPortal / HDX)",
    license: "Public Domain"
  },
  geometry: countySrc[0].geometry
};

const subFeatures = subSrc.map((f) => {
  const name = f.properties.shapeName;
  const meta = SUB_META[name];
  if (!meta) throw new Error(`Unknown sub-county name: ${name}`);
  return {
    type: "Feature",
    properties: {
      kind: "subcounty",
      name,
      code: meta.code,
      hq: meta.hq,
      source: "geoBoundaries KEN ADM2 (IEBC / OCHA ROSEA / HDX)",
      license: "CC BY 3.0 IGO"
    },
    geometry: f.geometry
  };
});

const collection = {
  type: "FeatureCollection",
  name: "Kajiado County",
  crs: { type: "name", properties: { name: "urn:ogc:def:crs:OGC:1.3:CRS84" } },
  metadata: {
    boundarySource: "geoBoundaries.org — Kenya administrative boundaries (HDX)",
    countyLayer: "ADM1 — RCMRD GeoPortal, Africa GeoPortal (2020)",
    subCountyLayer: "ADM2 — IEBC, OCHA ROSEA (2020)",
    generatedAt: new Date().toISOString()
  },
  features: [countyFeature, ...subFeatures, ...towns]
};

fs.writeFileSync(outPath, JSON.stringify(collection, null, 2) + "\n", "utf8");
console.log(`Wrote ${outPath}`);
console.log(`  county vertices: ${countyFeature.geometry.coordinates[0].length}`);
console.log(`  sub-counties: ${subFeatures.length}, towns: ${towns.length}`);
