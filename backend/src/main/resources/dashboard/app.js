/**
 * LINDA BINTI — NGO case-volume map for Kajiado.
 * Hotspots are counts of synced reports and SOS flags, not live police dispatch.
 */

const state = {
  token: sessionStorage.getItem("lb_token") || "",
  refreshToken: sessionStorage.getItem("lb_refresh") || "",
  expiresAt: Number(sessionStorage.getItem("lb_expires") || "0"),
  role: sessionStorage.getItem("lb_role") || "",
  email: sessionStorage.getItem("lb_email") || "",
  mustChangePassword: sessionStorage.getItem("lb_must_change") === "1",
  map: null,
  countyLayer: null,
  maskLayer: null,
  baseLayer: null,
  subLayer: null,
  townLayer: null,
  countyBounds: null,
  refreshTimer: null,
  riskByCode: {},
  geojson: null,
  intel: null
};

const $ = (id) => document.getElementById(id);

const KAJIADO_CENTER = [-2.05, 36.85];
const MASK_FILL = "#0b1220";
const SUB_CODES = [
  "kajiado_north", "kajiado_east", "kajiado_central", "kajiado_west", "kajiado_south"
];

function ringToLatLng(ring) {
  return ring.map(([lng, lat]) => [lat, lng]);
}

function getCountyFeature(geojson) {
  return (geojson.features || []).find((f) => f.properties && f.properties.kind === "county");
}

function boundsFromCountyFeature(feature) {
  const ring = feature.geometry.coordinates[0];
  let minLat = 90;
  let maxLat = -90;
  let minLng = 180;
  let maxLng = -180;
  ring.forEach(([lng, lat]) => {
    if (lat < minLat) minLat = lat;
    if (lat > maxLat) maxLat = lat;
    if (lng < minLng) minLng = lng;
    if (lng > maxLng) maxLng = lng;
  });
  return L.latLngBounds([[minLat, minLng], [maxLat, maxLng]]);
}

function applyCountyViewport(countyFeature) {
  if (!state.map || !countyFeature) return;

  const hole = ringToLatLng(countyFeature.geometry.coordinates[0]);
  const world = [
    [-90, -180], [-90, 180], [90, 180], [90, -180], [-90, -180]
  ];

  if (!state.map.getPane("countyMaskPane")) {
    state.map.createPane("countyMaskPane");
    state.map.getPane("countyMaskPane").style.zIndex = 350;
  }

  if (!state.maskLayer) {
    state.maskLayer = L.polygon([world, hole.slice().reverse()], {
      pane: "countyMaskPane",
      stroke: false,
      fillColor: MASK_FILL,
      fillOpacity: 1,
      interactive: false,
      className: "county-outside-mask"
    }).addTo(state.map);
  } else {
    state.maskLayer.setLatLngs([world, hole.slice().reverse()]);
  }

  state.countyBounds = boundsFromCountyFeature(countyFeature);
  state.map.setMaxBounds(state.countyBounds.pad(0.08));
  state.map.fitBounds(state.countyBounds, { padding: [12, 12], maxZoom: 12 });
}

function setError(id, message) {
  const el = $(id);
  if (el) el.textContent = message || "";
}

function escapeHtml(value) {
  return String(value || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function isNgoRole(role) {
  return role === "NGO_STAFF" || role === "NGO_ADMIN";
}

function normalizeAreaCode(label) {
  return String(label || "").trim().toLowerCase().replaceAll(" ", "_");
}

function severityRank(sev) {
  const s = String(sev || "").toUpperCase();
  if (s === "CRITICAL") return 4;
  if (s === "HIGH") return 3;
  if (s === "MEDIUM") return 2;
  if (s === "LOW") return 1;
  return 0;
}

async function fetchJson(path, options = {}) {
  const headers = Object.assign({ "Content-Type": "application/json" }, options.headers || {});
  if (state.token && !options.skipAuth) headers.Authorization = "Bearer " + state.token;
  const res = await fetch(path, Object.assign({}, options, { headers }));
  const text = await res.text();
  let body = {};
  try { body = text ? JSON.parse(text) : {}; } catch (_) { body = { error: text }; }
  return { res, body };
}

async function tryRefreshSession() {
  if (!state.refreshToken) return false;
  try {
    const { res, body } = await fetchJson("/v1/auth/refresh", {
      method: "POST",
      skipAuth: true,
      body: JSON.stringify({ refreshToken: state.refreshToken })
    });
    if (!res.ok || !isNgoRole(body.role)) {
      clearSession(false);
      return false;
    }
    saveSession(body);
    return true;
  } catch (_) {
    return false;
  }
}

async function api(path, options = {}) {
  const authOptional = !!options.authOptional;
  let { res, body } = await fetchJson(path, options);

  if (res.status === 401 && !authOptional && state.refreshToken) {
    if (await tryRefreshSession()) ({ res, body } = await fetchJson(path, options));
  }

  if (res.status === 401) {
    if (authOptional) throw new Error("Invalid email or password.");
    clearSession(false);
    throw new Error("Session expired. Sign in again.");
  }
  if (res.status === 403) {
    if (body.error === "password_change_required") {
      state.mustChangePassword = true;
      sessionStorage.setItem("lb_must_change", "1");
      showChangePasswordRequired();
      throw new Error("password_change_required");
    }
    clearSession(false);
    throw new Error("Access denied. NGO staff only.");
  }
  if (!res.ok) throw new Error(body.error || ("Request failed (" + res.status + ")"));
  return body;
}

function clearSession(clearMessage) {
  if (state.refreshTimer) {
    clearInterval(state.refreshTimer);
    state.refreshTimer = null;
  }
  state.token = "";
  state.refreshToken = "";
  state.expiresAt = 0;
  state.role = "";
  state.email = "";
  state.mustChangePassword = false;
  state.intel = null;
  sessionStorage.clear();
  $("app-view").classList.add("hidden");
  $("change-password-view").classList.add("hidden");
  $("login-view").classList.remove("hidden");
  if (clearMessage !== false) setError("login-error", "");
}

function saveSession(auth) {
  state.token = auth.accessToken;
  state.refreshToken = auth.refreshToken || state.refreshToken;
  state.expiresAt = auth.expiresAtEpochMs || 0;
  state.role = auth.role;
  state.email = auth.email;
  state.mustChangePassword = !!auth.mustChangePassword;
  sessionStorage.setItem("lb_token", state.token);
  sessionStorage.setItem("lb_refresh", state.refreshToken);
  sessionStorage.setItem("lb_expires", String(state.expiresAt));
  sessionStorage.setItem("lb_role", state.role);
  sessionStorage.setItem("lb_email", state.email);
  sessionStorage.setItem("lb_must_change", state.mustChangePassword ? "1" : "0");
}

function showChangePasswordRequired() {
  $("login-view").classList.add("hidden");
  $("app-view").classList.add("hidden");
  $("change-password-view").classList.remove("hidden");
  setError("change-password-error", "");
}

function showMap() {
  $("login-view").classList.add("hidden");
  $("change-password-view").classList.add("hidden");
  $("app-view").classList.remove("hidden");
  const sessionMeta = $("session-meta");
  if (sessionMeta) sessionMeta.textContent = state.email;
  ensureMap();
  setTimeout(() => state.map && state.map.invalidateSize(), 100);
}

function ensureMap() {
  if (typeof L === "undefined") {
    setError("app-error", "Map failed to load.");
    return;
  }
  if (state.map) return;

  state.map = L.map("kajiado-map", {
    zoomControl: true,
    attributionControl: true,
    scrollWheelZoom: true,
    maxBoundsViscosity: 1
  }).setView(KAJIADO_CENTER, 9);

  state.map.attributionControl.setPrefix("");
  state.map.attributionControl.addAttribution(
    "Kajiado © geoBoundaries/IEBC · LINDA BINTI AI"
  );

  state.baseLayer = L.layerGroup().addTo(state.map);
  state.countyLayer = L.layerGroup().addTo(state.map);
  state.subLayer = L.layerGroup().addTo(state.map);
  state.townLayer = L.layerGroup().addTo(state.map);
}

/** Area hotspot scoring from case counts per Kajiado sub-county. */
function buildRiskIndex(geoAreas, caseFlags) {
  const index = {};
  const visible = (geoAreas || []).filter((a) => !a.suppressed && a.areaCode !== "suppressed");
  const totals = visible.map((a) => a.totalCount || 0);
  const max = Math.max(0, ...totals);

  const severityByCode = {};
  (caseFlags || []).forEach((flag) => {
    const code = (flag.areaCode && String(flag.areaCode).toLowerCase()) ||
      normalizeAreaCode(flag.areaLabel);
    if (!SUB_CODES.includes(code)) return;
    if (!severityByCode[code]) {
      severityByCode[code] = { critical: 0, high: 0, total: 0, topSeverity: "LOW", sos: 0 };
    }
    const bucket = severityByCode[code];
    bucket.total += 1;
    if (String(flag.categoryId || "").toLowerCase() === "sos") bucket.sos += 1;
    const sev = String(flag.severity || flag.urgency || "MEDIUM").toUpperCase();
    if (severityRank(sev) > severityRank(bucket.topSeverity)) bucket.topSeverity = sev;
    if (sev === "CRITICAL") bucket.critical += 1;
    if (sev === "HIGH") bucket.high += 1;
  });

  SUB_CODES.forEach((code) => {
    index[code] = {
      level: "low",
      total: 0,
      gbv: 0,
      fgm: 0,
      sos: 0,
      score: 0.14,
      topSeverity: "LOW",
      hotspotLevel: "LOWER",
      aiNote: "No cases reported in this sub-county"
    };
  });

  visible.forEach((area) => {
    const code = area.areaCode || normalizeAreaCode(area.areaLabel);
    if (!index[code]) return;
    const total = area.totalCount || 0;
    const ratio = max > 0 ? total / max : 0;
    const sev = severityByCode[code] || { topSeverity: "LOW", critical: 0, high: 0, sos: 0 };
    const hotspot = String(area.hotspotLevel || "").toUpperCase();
    // Primary rule: volume of cases in the area (backend hotspotLevel).
    const highRisk = hotspot === "HIGH";
    const elevated = hotspot === "ELEVATED";
    index[code] = {
      level: highRisk ? "high" : elevated ? "elevated" : "low",
      total,
      gbv: area.gbvCount || 0,
      fgm: area.fgmCount || 0,
      sos: area.sosCount || sev.sos || 0,
      topSeverity: sev.topSeverity || "LOW",
      hotspotLevel: hotspot || "LOWER",
      score: highRisk
        ? Math.min(0.88, 0.55 + ratio * 0.33)
        : elevated
          ? Math.min(0.45, 0.22 + ratio * 0.2)
          : 0.12 + ratio * 0.05,
      aiNote: area.hotspotReason ||
        (highRisk
          ? total + " case(s) — high-volume hotspot in Kajiado"
          : elevated
            ? total + " case(s) reported in this sub-county"
            : "Few or no cases in this sub-county")
    };
  });

  state.riskByCode = index;
  return index;
}

function isHighRisk(risk) {
  return risk && (risk.level === "high" || risk.hotspotLevel === "HIGH");
}

function subStyle(feature) {
  const code = feature.properties.code;
  const risk = state.riskByCode[code] || { level: "low", score: 0.14 };
  const high = isHighRisk(risk);
  const elevated = risk.level === "elevated";
  return {
    color: high ? "#b91c1c" : elevated ? "#b45309" : "#64748b",
    weight: high ? 2.2 : elevated ? 1.6 : 1.1,
    opacity: high ? 0.95 : elevated ? 0.8 : 0.5,
    fillColor: high ? "#ef4444" : elevated ? "#fbbf24" : "#bbf7d0",
    fillOpacity: high ? Math.max(0.55, risk.score) : elevated ? Math.max(0.35, risk.score) : Math.max(0.22, risk.score),
    className: high ? "sub-high" : elevated ? "sub-elevated" : "sub-low"
  };
}

function countyBaseStyle() {
  return {
    color: "transparent",
    weight: 0,
    fillColor: "#dbe4ef",
    fillOpacity: 1
  };
}

function countyStyle() {
  return {
    color: "#1e293b",
    weight: 2.4,
    opacity: 0.9,
    fillColor: "#dbe4ef",
    fillOpacity: 0,
    dashArray: null
  };
}

function onEachSub(feature, layer) {
  const code = feature.properties.code;
  const name = feature.properties.name;
  const hq = feature.properties.hq || "";
  const risk = state.riskByCode[code] || { level: "low", total: 0, gbv: 0, fgm: 0, sos: 0, aiNote: "" };
  const high = isHighRisk(risk);
  const elevated = risk.level === "elevated";
  const signal = high
    ? "High-risk area · " + (risk.total || 0) + " case(s) — highest volume hotspot"
    : elevated
      ? "Elevated area · " + (risk.total || 0) + " case(s) reported"
      : "Lower area risk · " + (risk.total || 0) + " case(s)";

  layer.bindTooltip(name, {
    permanent: true,
    direction: "center",
    className: "subcounty-label" + (high ? " high" : elevated ? " elevated" : "")
  });

  layer.bindPopup(
    "<div class='popup-card'>" +
      "<strong>" + escapeHtml(name) + "</strong>" +
      (hq ? "<div class='popup-meta'>" + escapeHtml(hq) + "</div>" : "") +
      "<div class='popup-signal " + (high ? "high" : elevated ? "elevated" : "low") + "'>" +
        escapeHtml(signal) + "</div>" +
      "<div class='popup-meta'>Total " + (risk.total || 0) +
        " · GBV " + risk.gbv + " · FGM " + risk.fgm +
        " · SOS " + (risk.sos || 0) + "</div>" +
      "<div class='popup-note'>" + escapeHtml(risk.aiNote || "Case-count hotspot · privacy protected") + "</div>" +
    "</div>"
  );

  layer.on({
    mouseover: (e) => {
      e.target.setStyle({ weight: 2.6, fillOpacity: Math.min(0.85, (risk.score || 0.2) + 0.2) });
      e.target.bringToFront();
    },
    mouseout: (e) => {
      if (state.subLayer) e.target.setStyle(subStyle(feature));
    }
  });
}

function renderTowns(geojson) {
  state.townLayer.clearLayers();
  const towns = geojson.features.filter((f) => f.properties.kind === "town");

  towns.forEach((f) => {
    const [lng, lat] = f.geometry.coordinates;
    const name = f.properties.name;
    const sub = f.properties.subcounty;
    const risk = state.riskByCode[sub] || { level: "low" };
    const high = isHighRisk(risk);
    const capital = !!f.properties.capital;

    const marker = L.circleMarker([lat, lng], {
      radius: capital ? 6 : 4.5,
      color: capital ? "#0f172a" : "#475569",
      weight: capital ? 2 : 1.2,
      fillColor: high ? "#fca5a5" : "#d1fae5",
      fillOpacity: 0.95
    });

    marker.bindTooltip(name, {
      permanent: true,
      direction: "right",
      offset: [8, 0],
      className: "town-label" + (capital ? " capital" : "")
    });

    marker.bindPopup(
      "<strong>" + escapeHtml(name) + "</strong>" +
      (capital ? "<div class='popup-meta'>County capital</div>" : "") +
      "<div class='popup-meta'>" + escapeHtml((sub || "").replace(/_/g, " ")) + "</div>"
    );

    marker.addTo(state.townLayer);
  });
}

function renderAiChrome(intel) {
  const summary = (intel && intel.commandSummary) || {};
  const engine = (intel && intel.aiEngine) || {};
  const highZones = Object.values(state.riskByCode).filter((r) => isHighRisk(r)).length;
  const lowZones = Object.values(state.riskByCode).filter((r) => !isHighRisk(r)).length;

  const engineEl = $("ai-engine-status");
  if (engineEl) {
    const label = engine.statusLabel || engine.label || engine.mode || "ACTIVE";
    const source = engine.activeSourceLabel ? " · " + engine.activeSourceLabel : "";
    engineEl.textContent = label + source;
  }

  const dot = $("ai-live-dot");
  if (dot) {
    dot.classList.remove("warn", "off");
    const status = String(engine.statusLabel || "").toUpperCase();
    if (status.includes("NOT CONFIGURED") || status.includes("FAILED")) dot.classList.add("warn");
    else if (status.includes("FALLBACK")) dot.classList.add("warn");
  }

  const insightEl = $("ai-insight");
  if (insightEl) {
    const insight = (intel && intel.insightOfPeriod && intel.insightOfPeriod.text) ||
      (intel && intel.interventionInsights && intel.interventionInsights[0] && intel.interventionInsights[0].text) ||
      (intel && intel.trendInsights && intel.trendInsights[0] && intel.trendInsights[0].text) ||
      (highZones > 0
        ? "AI detected elevated spatial risk in " + highZones + " Kajiado sub-county zone(s)."
        : "AI monitoring active · all sub-counties currently show low spatial risk.");
    insightEl.textContent = insight;
  }

  const setMetric = (id, val) => {
    const el = $(id);
    if (el) el.textContent = val != null ? String(val) : "—";
  };
  setMetric("metric-cases", summary.totalReports);
  setMetric("metric-critical", summary.criticalCases);
  setMetric("metric-flagged", summary.aiFlaggedCases);
  setMetric("metric-high-zones", highZones);

  const list = $("ai-priority-list");
  if (list) {
    const queue = ((intel && intel.priorityQueue) || []).slice(0, 4);
    if (!queue.length) {
      list.innerHTML = "<li class='priority-empty'>No active AI priority signals</li>";
    } else {
      list.innerHTML = queue.map((item) => {
        const sev = escapeHtml(String(item.severity || item.urgency || "MEDIUM").toUpperCase());
        return "<li>" +
          "<span class='prio-ref'>" + escapeHtml(item.referenceId || "—") + "</span> " +
          escapeHtml(item.incidentType || item.category || "Case") +
          "<span class='prio-area'>" + escapeHtml(item.areaLabel || "Kajiado County") + "</span>" +
          "<span class='prio-sev " + sev + "'>" + sev + "</span>" +
        "</li>";
      }).join("");
    }
  }

  const status = $("map-status");
  if (status) {
    status.textContent = "Case map · " + highZones + " high · " + lowZones + " low · " +
      new Date().toLocaleTimeString();
  }

  const subtitle = $("topbar-subtitle");
  if (subtitle) {
    subtitle.textContent = highZones > 0
      ? highZones + " high-risk zone(s) · Kajiado County"
      : "All zones low risk · Kajiado County";
  }
}

function paintMap() {
  ensureMap();
  if (!state.map || !state.geojson) return;

  state.baseLayer.clearLayers();
  state.countyLayer.clearLayers();
  state.subLayer.clearLayers();
  state.townLayer.clearLayers();

  const county = state.geojson.features.filter((f) => f.properties.kind === "county");
  const subs = state.geojson.features.filter((f) => f.properties.kind === "subcounty");
  const countyFeature = getCountyFeature(state.geojson);

  applyCountyViewport(countyFeature);

  L.geoJSON({ type: "FeatureCollection", features: county }, { style: countyBaseStyle }).addTo(state.baseLayer);
  L.geoJSON({ type: "FeatureCollection", features: county }, { style: countyStyle }).addTo(state.countyLayer);
  L.geoJSON({ type: "FeatureCollection", features: subs }, {
    style: subStyle,
    onEachFeature: onEachSub
  }).addTo(state.subLayer);

  renderTowns(state.geojson);
  renderAiChrome(state.intel);
  setTimeout(() => state.map.invalidateSize(), 80);
}

async function loadGeo() {
  if (state.geojson) return state.geojson;
  const res = await fetch("/dashboard/kajiado.geojson", { cache: "no-store" });
  if (!res.ok) throw new Error("Could not load Kajiado map data.");
  state.geojson = await res.json();
  return state.geojson;
}

async function loadMap() {
  setError("app-error", "");
  const refreshBtn = $("refresh-btn");
  if (refreshBtn) refreshBtn.disabled = true;
  try {
    const session = await api("/v1/dashboard/session");
    state.role = session.role;
    state.email = session.email;
    state.mustChangePassword = !!session.mustChangePassword;
    sessionStorage.setItem("lb_must_change", state.mustChangePassword ? "1" : "0");
    if (state.mustChangePassword) {
      showChangePasswordRequired();
      return;
    }
    showMap();
    await loadGeo();

    const intel = await api("/v1/dashboard/intelligence?periodDays=30");
    state.intel = intel;
    const geoAreas = intel.mapGeoAreas || (intel.trends && intel.trends.geoAreas) || [];
    buildRiskIndex(geoAreas, intel.caseFlags || []);
    paintMap();
    scheduleRefresh();
  } catch (err) {
    if (err.message === "password_change_required") return;
    if (String(err.message).includes("Access denied") || String(err.message).includes("Invalid email")) {
      clearSession(false);
      setError("login-error", err.message);
      return;
    }
    setError("app-error", err.message);
    if (!state.token) clearSession(false);
  } finally {
    if (refreshBtn) refreshBtn.disabled = false;
  }
}

function scheduleRefresh() {
  if (state.refreshTimer) clearInterval(state.refreshTimer);
  state.refreshTimer = setInterval(() => {
    if (!state.token || state.mustChangePassword || document.hidden) return;
    loadMap();
  }, 8000);
}

async function submitPasswordChange(currentPassword, newPassword, confirmPassword) {
  setError("change-password-error", "");
  if (newPassword !== confirmPassword) {
    setError("change-password-error", "New passwords do not match.");
    return;
  }
  try {
    const auth = await api("/v1/auth/change-password", {
      method: "POST",
      allowUnauthorized: true,
      body: JSON.stringify({ currentPassword, newPassword })
    });
    saveSession(auth);
    await loadMap();
  } catch (err) {
    if (err.message === "password_change_required") return;
    setError(
      "change-password-error",
      err.message === "invalid_current_password" ? "Current password is incorrect." : err.message
    );
  }
}

$("login-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  setError("login-error", "");
  try {
    const auth = await api("/v1/auth/login", {
      method: "POST",
      authOptional: true,
      body: JSON.stringify({
        email: $("email").value.trim(),
        password: $("password").value
      })
    });
    if (!isNgoRole(auth.role)) {
      clearSession(false);
      setError("login-error", "Access denied. NGO staff only.");
      return;
    }
    saveSession(auth);
    if (auth.mustChangePassword) {
      showChangePasswordRequired();
      return;
    }
    await loadMap();
  } catch (err) {
    setError("login-error", err.message);
  }
});

$("change-password-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  await submitPasswordChange(
    $("current-password").value,
    $("new-password").value,
    $("confirm-password").value
  );
});

$("logout-btn").addEventListener("click", () => clearSession(true));
$("refresh-btn").addEventListener("click", () => loadMap());

async function apiJson(path, options = {}) {
  const headers = Object.assign(
    { "Content-Type": "application/json" },
    options.headers || {},
    state.token ? { Authorization: `Bearer ${state.token}` } : {}
  );
  const res = await fetch(path, Object.assign({}, options, { headers }));
  const text = await res.text();
  let body = {};
  try { body = text ? JSON.parse(text) : {}; } catch (_) { body = { error: text }; }
  if (!res.ok) throw new Error(body.error || `Request failed (${res.status})`);
  return body;
}

function showEducation(show) {
  const edu = $("education-view");
  if (!edu) return;
  edu.classList.toggle("hidden", !show);
  if (show) showAudit(false);
}

function fillEduForm(item) {
  $("edu-id").value = item?.id || "";
  $("edu-type").value = item?.contentType || "video";
  $("edu-title").value = item?.title || "";
  $("edu-desc").value = item?.description || "";
  $("edu-lang").value = item?.language || "en";
  $("edu-category").value = item?.category || "what_is_gbv";
  $("edu-url").value = item?.urlOrPath || "";
  $("edu-thumb").value = item?.thumbnailUrl || "";
  $("edu-duration").value = item?.durationSeconds ?? 0;
  $("edu-sort").value = item?.sortOrder ?? 0;
  $("edu-active").checked = item ? !!item.active : true;
}

async function loadEducationList() {
  setError("edu-error", "");
  const data = await apiJson("/v1/admin/education/content");
  const list = $("edu-list");
  list.innerHTML = "";
  (data.items || []).forEach((item) => {
    const li = document.createElement("li");
    li.className = "edu-item" + (item.active ? "" : " inactive");
    li.innerHTML = `
      <div>
        <strong>${escapeHtml(item.title)}</strong>
        <span class="edu-meta">${escapeHtml(item.contentType)} · ${escapeHtml(item.language)} · ${escapeHtml(item.category)} · ${item.active ? "active" : "inactive"}</span>
        <p>${escapeHtml(item.description || "")}</p>
      </div>
      <div class="edu-item-actions">
        <button type="button" data-edit="${escapeHtml(item.id)}" class="btn-ghost">Edit</button>
        <button type="button" data-toggle="${escapeHtml(item.id)}" data-active="${item.active ? "0" : "1"}" class="btn-ghost">${item.active ? "Deactivate" : "Activate"}</button>
      </div>`;
    list.appendChild(li);
  });
  list.querySelectorAll("[data-edit]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const found = (data.items || []).find((x) => x.id === btn.getAttribute("data-edit"));
      if (found) fillEduForm(found);
    });
  });
  list.querySelectorAll("[data-toggle]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      try {
        const id = btn.getAttribute("data-toggle");
        const active = btn.getAttribute("data-active") === "1";
        await apiJson(`/v1/admin/education/content/${encodeURIComponent(id)}/active`, {
          method: "POST",
          body: JSON.stringify({ active })
        });
        await loadEducationList();
      } catch (err) {
        setError("edu-error", err.message);
      }
    });
  });
}

function escapeHtml(value) {
  return String(value || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

if ($("edu-btn")) {
  $("edu-btn").addEventListener("click", async () => {
    try {
      showEducation(true);
      fillEduForm(null);
      await loadEducationList();
    } catch (err) {
      setError("edu-error", err.message);
    }
  });
}
if ($("edu-close-btn")) {
  $("edu-close-btn").addEventListener("click", () => showEducation(false));
}
if ($("edu-reset-btn")) {
  $("edu-reset-btn").addEventListener("click", () => fillEduForm(null));
}
if ($("edu-form")) {
  $("edu-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    setError("edu-error", "");
    const payload = {
      contentType: $("edu-type").value,
      title: $("edu-title").value.trim(),
      description: $("edu-desc").value.trim(),
      language: $("edu-lang").value,
      category: $("edu-category").value,
      urlOrPath: $("edu-url").value.trim(),
      thumbnailUrl: $("edu-thumb").value.trim(),
      durationSeconds: Number($("edu-duration").value || 0),
      active: $("edu-active").checked,
      sortOrder: Number($("edu-sort").value || 0)
    };
    try {
      const id = $("edu-id").value.trim();
      if (id) {
        await apiJson(`/v1/admin/education/content/${encodeURIComponent(id)}`, {
          method: "PUT",
          body: JSON.stringify(payload)
        });
      } else {
        await apiJson("/v1/admin/education/content", {
          method: "POST",
          body: JSON.stringify(payload)
        });
      }
      fillEduForm(null);
      await loadEducationList();
    } catch (err) {
      setError("edu-error", err.message);
    }
  });
}

function showAudit(show) {
  const view = $("audit-view");
  if (!view) return;
  view.classList.toggle("hidden", !show);
  if (show) {
    const edu = $("education-view");
    if (edu) edu.classList.add("hidden");
  }
}

async function loadAuditList() {
  setError("audit-error", "");
  const rows = await apiJson("/v1/admin/audit");
  const list = $("audit-list");
  list.innerHTML = "";
  (Array.isArray(rows) ? rows : []).forEach((row) => {
    const li = document.createElement("li");
    li.className = "edu-item";
    const when = row.createdAt ? new Date(row.createdAt).toLocaleString() : "";
    li.innerHTML = `
      <div>
        <strong>${escapeHtml(row.action || "")}</strong>
        <span class="edu-meta">${escapeHtml(when)} · actor ${escapeHtml(row.actorUserId || "")}</span>
        <p>${escapeHtml(row.detail || "")}</p>
      </div>`;
    list.appendChild(li);
  });
}

if ($("audit-btn")) {
  $("audit-btn").addEventListener("click", async () => {
    try {
      showEducation(false);
      showAudit(true);
      await loadAuditList();
    } catch (err) {
      setError("audit-error", err.message);
    }
  });
}
if ($("audit-close-btn")) {
  $("audit-close-btn").addEventListener("click", () => showAudit(false));
}
if ($("audit-refresh-btn")) {
  $("audit-refresh-btn").addEventListener("click", () => loadAuditList().catch((err) => setError("audit-error", err.message)));
}

window.addEventListener("resize", () => {
  if (state.map) state.map.invalidateSize();
});

(async function boot() {
  if (state.token && isNgoRole(state.role)) {
    if (state.mustChangePassword) showChangePasswordRequired();
    else await loadMap();
  }
})();
