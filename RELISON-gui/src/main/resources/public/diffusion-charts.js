"use strict";

/* --------------------------- diffusion metrics --------------------------- */

// The union of metrics computed across all accumulated runs, in first-seen order.
function diffMetricUnion() {
    const seen = new Map();
    for (const run of state.diffusion.runs) {
        for (const m of run.metrics) if (!seen.has(m.id)) seen.set(m.id, m.label);
    }
    return Array.from(seen, ([id, label]) => ({ id, label }));
}

function populateDiffMetricSelect() {
    const items = diffMetricUnion();
    const sel = $("diffmetric-select");
    const prev = sel.value;
    sel.innerHTML = "";
    items.forEach((m) => sel.appendChild(option(m.id, m.label)));
    if (prev && items.some((m) => m.id === prev)) sel.value = prev;
}

function renderDiffMetrics() {
    const container = $("diffmetrics-charts");
    if (!container) return;
    disposeEChart("diff-metrics-chart");
    container.innerHTML = "";
    const runs = state.diffusion.runs;
    if (!runs.length) {
        $("diffmetrics-hint").textContent = "Run a simulation (with metrics selected) to see plots over iterations.";
        renderDiffRuns();
        return;
    }
    $("diffmetrics-hint").textContent = "";
    renderDiffRuns();
    const sel = $("diffmetric-select");
    const union = diffMetricUnion();
    const chosen = union.find((m) => m.id === sel.value) || union[0];
    if (!chosen) return;

    // One line per run that computed the chosen metric, coloured by run and labelled with its protocol.
    const series = [];
    runs.forEach((run, idx) => {
        const m = run.metrics.find((x) => x.id === chosen.id);
        if (m) series.push({ name: run.label, values: m.values, color: echartColors().palette[idx % echartColors().palette.length] });
    });
    if (!series.length) return;

    const title = document.createElement("h2");
    title.textContent = chosen.label;
    const area = document.createElement("div");
    area.className = "chart-area";
    area.style.height = "320px";
    const chartEl = document.createElement("div");
    chartEl.id = "diff-metrics-chart";   // stable id so the report groups it under Diffusion (keyed by the metric label)
    chartEl.className = "echarts-chart";
    area.appendChild(chartEl);
    container.appendChild(title);
    container.appendChild(area);
    drawMultiLineChart(chartEl, series, chosen.label, false);   // legend omitted — see the runs list below
}

// The accumulated runs, each with its plot colour, label and (on hover) full configuration; ✕ removes a run from
// the overlaid plots. Colours match drawMultiLineChart's per-run assignment (run index into SERIES_COLORS).
function renderDiffRuns() {
    const host = $("diffmetrics-runs");
    if (!host) return;
    host.innerHTML = "";
    const runs = state.diffusion.runs;
    if (!runs.length) return;
    const title = document.createElement("div");
    title.className = "diff-runs-title sublabel";
    title.textContent = "Runs (hover for the full configuration)";
    host.appendChild(title);
    runs.forEach((run, idx) => {
        const row = document.createElement("div");
        row.className = "diff-run-row";
        if (run.detail) row.title = run.detail;
        const sw = document.createElement("span");
        sw.className = "legend-swatch";
        const palette = echartColors().palette;
        sw.style.background = palette[idx % palette.length];
        const lab = document.createElement("span");
        lab.className = "diff-run-label";
        lab.textContent = run.label;
        const del = document.createElement("button");
        del.className = "diff-run-del";
        del.textContent = "✕";
        del.title = "Remove this run from the plots";
        del.addEventListener("click", () => { state.diffusion.runs.splice(idx, 1); renderDiffMetrics(); });
        row.appendChild(sw);
        row.appendChild(lab);
        row.appendChild(del);
        host.appendChild(row);
    });
}

// Draws one or more metric series (each {name, values, color}) as lines over iteration index, with an
// "iteration" x axis (labelled ticks) and the metric name as the y-axis title. The in-chart legend is optional
// (off for the diffusion metric plot, whose series are listed in the runs list below the chart).
function drawMultiLineChart(container, series, label, showLegend = true, stacked = false) {
    const el = typeof container === "string" ? $(container) : container;
    if (!el) return;
    let n = 0;
    for (const s of series) n = Math.max(n, (s.values || []).length);
    if (!n) { clearChart(el.id); return; }
    const colors = echartColors();
    const option = chartBase(colors, "");
    option.grid = { left: colors.chartStyle === "editorial" ? 88 : 76,
        right: colors.chartStyle === "editorial" && !stacked && series.length <= 6 ? 116 : 24,
        top: showLegend ? 46 : 24, bottom: 80, containLabel: true };
    if (showLegend) option.legend = { type: series.length > 8 ? "scroll" : "plain", top: 4, left: 76, right: 92,
        textStyle: { color: colors.muted, fontSize: colors.chartStyle === "editorial" ? 14 : 12 }, pageTextStyle: { color: colors.muted }, itemWidth: 12, itemHeight: 8 };
    option.tooltip = { ...option.tooltip, trigger: "axis", axisPointer: { type: "line" }, formatter: (params) => {
        const list = Array.isArray(params) ? params : [params];
        if (!list.length) return "";
        return "Iteration " + escapeChartHtml(list[0].axisValue) + "<br>" + list.filter((p) => p.value != null && Number.isFinite(Number(p.value)))
            .map((p) => p.marker + escapeChartHtml(p.seriesName) + ": <b>" + escapeChartHtml(fmt(Number(p.value))) + "</b>").join("<br>");
    } };
    option.xAxis = { type: "category", boundaryGap: false, name: "iteration", nameLocation: "middle", nameGap: 40,
        nameTextStyle: { color: colors.text, fontSize: colors.fontSize + 1, fontWeight: 600 },
        data: Array.from({ length: n }, (_, i) => String(i)), axisLabel: { color: colors.muted, fontSize: colors.labelSize, hideOverlap: true },
        axisLine: { lineStyle: { color: colors.border } }, axisTick: { show: false }, splitLine: { show: false } };
    option.yAxis = { type: "value", name: label || "value", nameLocation: "middle", nameGap: 60,
        nameTextStyle: { color: colors.text, fontSize: colors.fontSize + 1, fontWeight: 600 },
        axisLabel: { color: colors.muted, fontSize: colors.labelSize, formatter: (v) => fmt(v) },
        axisLine: { show: true, lineStyle: { color: colors.border } }, axisTick: { show: true, inside: false, length: 6, lineStyle: { color: colors.border } },
        splitLine: { lineStyle: { color: colors.border, opacity: colors.gridOpacity } } };
    if (stacked) option.yAxis.min = 0;
    option.dataZoom = [
        { type: "inside", xAxisIndex: 0, filterMode: "none" },
        { type: "slider", xAxisIndex: 0, height: 16, bottom: 8, borderColor: colors.border,
            textStyle: { color: colors.muted }, fillerColor: colors.accent + "33", handleStyle: { color: colors.accent } },
    ];
    option.series = series.map((s) => ({ name: s.name, type: "line", data: (s.values || []).map((v) =>
        Number.isFinite(v) ? v : (stacked ? 0 : null)), showSymbol: false, connectNulls: false, smooth: false,
        stack: stacked ? "total" : undefined, areaStyle: stacked ? { opacity: colors.chartStyle === "glass-panel" ? 0.62 : 0.78 } : undefined,
        endLabel: colors.chartStyle === "editorial" && !stacked && series.length <= 6
            ? { show: true, formatter: "{a}", color: colors.text, fontSize: 11, distance: 8 } : undefined,
        lineStyle: { width: stacked ? (colors.lineWidth > 2 ? 1.6 : 1) : colors.lineWidth, color: s.color || colors.accent }, itemStyle: { color: s.color || colors.accent },
    }));
    showEChart(el, option, label);
}
/* ------------------------- node timeline (subtab) ------------------- */

// Keeps the Node-timeline feature selector in sync. The first option, "Information pieces" (value "pieces"), is the
// base view (piece counts per category, no feature breakdown); the rest are info-piece features and user features
// (node attrs + communities), tagged so the request knows which kind. Value form: "info:<name>" | "user:<name>".
function syncTrajFeatureSelect() {
    const sel = $("diff-traj-feature");
    const { info, user } = knownFeatureParams();
    const sig = "base|i:" + info.join("") + "|u:" + user.join("");
    if (sel.dataset.sig === sig) return;
    const prev = sel.value;
    sel.innerHTML = "";
    sel.appendChild(option("pieces", "Information pieces"));
    info.forEach((n) => sel.appendChild(option("info:" + n, n)));
    user.forEach((n) => sel.appendChild(option("user:" + n, n + " (user)")));
    sel.dataset.sig = sig;
    sel.value = [...sel.options].some((o) => o.value === prev) ? prev : "pieces";
}

// Enables/disables the controls that only apply to the feature views (category, aggregation) — the base
// "Information pieces" view always shows all four categories as counts.
function updateTrajControls() {
    const base = $("diff-traj-feature").value === "pieces";
    $("diff-traj-category").disabled = base;
    $("diff-traj-agg").disabled = base;
}

// Entry point for the Node subtab: validates prerequisites, otherwise fetches and draws the timeline.
function renderNodeTimeline() {
    const hint = $("diff-traj-hint");
    if (!state.diffusion.result) {
        hint.textContent = "Run a simulation and select a node to see how its information exposure evolves.";
        hint.hidden = false; clearTrajCanvas(); return;
    }
    syncTrajFeatureSelect();
    updateTrajControls();
    const node = state.diffusion.selectedNode;
    if (node) $("diff-traj-node").value = node;
    if (!node) {
        hint.textContent = "Select a node (click it on the Graph subtab or type it above).";
        hint.hidden = false; clearTrajCanvas(); return;
    }
    hint.hidden = true;
    fetchTrajectory();
}

async function fetchTrajectory() {
    const node = state.diffusion.selectedNode;
    if (!node || !state.diffusion.result) return;
    const fv = $("diff-traj-feature").value;
    if (!fv) return;
    const body = { graphId: state.graphId, node, mode: $("diff-traj-mode").value };
    if (fv === "pieces") {
        body.pieces = true;   // base view: piece counts per category, no feature breakdown
    } else {
        body.userFeature = fv.startsWith("user:");
        body.feature = fv.slice(fv.indexOf(":") + 1);
        body.category = $("diff-traj-category").value;
        body.aggregation = $("diff-traj-agg").value;
    }
    setChartLoading("diff-traj-area", true);
    try {
        const res = await api("/api/diffusion/trajectory", jsonBody(body));
        state.diffusion.traj = res;
        drawTrajectoryChart();
    } catch (e) { setStatus("Node timeline failed: " + e.message, "error"); }
    finally { setChartLoading("diff-traj-area", false); }
}

function clearTrajCanvas() {
    clearChart("diff-traj-canvas");
}

// Shared renderer for the Node and Piece timeline charts: turns a {values, series, iterations} response into coloured
// series and draws them (stacked or lines) with the slider playhead.
function drawTrajectoryInto(canvasId, res, stacked, label) {
    const container = $(canvasId);
    if (!container || !res) return;
    const values = res.values || [];
    if (!values.length) { clearChart(canvasId); return; }
    const series = values.map((v) => ({
        name: v,
        values: (res.series && res.series[v]) || [],
        color: v === "other" ? cssVar("--muted", "#888") : chartFeatureValueColor(v),
    }));
    if (stacked) drawStackedAreaChart(container, series, label);
    else drawMultiLineChart(container, series, label);
    drawTrajPlayhead(canvasId, res.iterations);
}

function drawTrajectoryChart() {
    const res = state.diffusion.traj;
    if (!res) return;
    const label = res.base
        ? "Information pieces — count" + (res.mode === "cumulative" ? " (cumulative)" : " (per iteration)")
        : (res.category + " · " + res.feature + (res.userFeature ? " (user)" : "")
            + " — " + (res.aggregation === "weight" ? "weight" : "count"));
    drawTrajectoryInto("diff-traj-canvas", res, $("diff-traj-stack").value === "stacked", label);
}

// A dashed vertical marker at the slider's current iteration, keeping the timeline coupled to the Graph subtab.
function drawTrajPlayhead(chartId, n) {
    const chart = ECHARTS.get(chartId);
    if (!chart || !n || n <= 1) return;
    const iter = Math.max(0, Math.min(state.diffusion.iteration || 0, n - 1));
    chart.setOption({ series: [{ markLine: { silent: true, symbol: "none", animation: false,
        lineStyle: { color: echartColors().text, type: "dashed", opacity: 0.6, width: 1 },
        label: { show: true, formatter: "{b}", color: echartColors().text },
        data: [{ name: "Iteration " + iter, xAxis: String(iter) }] } }] });
}

/* ------------------------- piece timeline (subtab) ------------------ */

// The Piece-timeline break-down selector: "Users" (base — user counts per category) plus the user features
// (node attributes + communities) to break the users of one category down by.
function syncPtrajFeatureSelect() {
    const sel = $("diff-ptraj-feature");
    const { user } = knownFeatureParams();
    const sig = "base|u:" + user.join("");
    if (sel.dataset.sig !== sig) {
        const prev = sel.value;
        sel.innerHTML = "";
        sel.appendChild(option("users", "Users (all categories)"));
        user.forEach((n) => sel.appendChild(option(n, n)));
        sel.dataset.sig = sig;
        sel.value = [...sel.options].some((o) => o.value === prev) ? prev : "users";
    }
    // The category selector only applies when breaking down by a feature (the base view shows every category).
    $("diff-ptraj-category").disabled = sel.value === "users";
}

// Keeps the piece autocomplete list in sync with the current pieces.
function syncPieceDatalist() {
    const dl = $("diff-piece-datalist");
    if (!dl) return;
    dl.innerHTML = "";
    (state.diffusion.pieces || []).forEach((p) => { if (p.id != null && p.id !== "") dl.appendChild(option(String(p.id), "")); });
}

function renderPieceTimeline() {
    const hint = $("diff-ptraj-hint");
    if (!state.diffusion.result) {
        hint.textContent = "Run a simulation and choose an information piece to see how many users receive / read / propagate / discard it over the iterations.";
        hint.hidden = false; clearCanvasById("diff-ptraj-canvas"); return;
    }
    syncPtrajFeatureSelect();
    syncPieceDatalist();
    if (state.diffusion.selectedPiece) $("diff-ptraj-piece").value = state.diffusion.selectedPiece;
    if (!state.diffusion.selectedPiece) {
        hint.textContent = "Type or pick an information piece id above.";
        hint.hidden = false; clearCanvasById("diff-ptraj-canvas"); return;
    }
    hint.hidden = true;
    fetchPieceTrajectory();
}

async function fetchPieceTrajectory() {
    const piece = state.diffusion.selectedPiece;
    if (!piece || !state.diffusion.result) return;
    const fv = $("diff-ptraj-feature").value;
    const body = { graphId: state.graphId, piece, mode: $("diff-ptraj-mode").value };
    if (fv && fv !== "users") { body.feature = fv; body.category = $("diff-ptraj-category").value; }
    setChartLoading("diff-ptraj-area", true);
    try {
        const res = await api("/api/diffusion/piece-trajectory", jsonBody(body));
        state.diffusion.ptraj = res;
        drawPieceTimeline();
    } catch (e) { setStatus("Piece timeline failed: " + e.message, "error"); }
    finally { setChartLoading("diff-ptraj-area", false); }
}

function drawPieceTimeline() {
    const res = state.diffusion.ptraj;
    if (!res) return;
    const label = res.base
        ? "Users — count" + (res.mode === "cumulative" ? " (cumulative)" : " (per iteration)")
        : (res.category + " users · " + res.feature + " — count" + (res.mode === "cumulative" ? " (cumulative)" : " (per iteration)"));
    drawTrajectoryInto("diff-ptraj-canvas", res, $("diff-ptraj-stack").value === "stacked", label);
}

/* ------------------------ feature timeline (subtab) ----------------- */

// The Feature-timeline selector: which feature's values to track over time (info-piece features + user features).
function syncFtrajFeatureSelect() {
    const sel = $("diff-ftraj-feature");
    const { info, user } = knownFeatureParams();
    const sig = "i:" + info.join("") + "|u:" + user.join("");
    if (sel.dataset.sig === sig) return;
    const prev = sel.value;
    sel.innerHTML = "";
    info.forEach((n) => sel.appendChild(option("info:" + n, n)));
    user.forEach((n) => sel.appendChild(option("user:" + n, n + " (user)")));
    sel.dataset.sig = sig;
    if ([...sel.options].some((o) => o.value === prev)) sel.value = prev;
}

// The distinct values of a tagged feature ("info:<name>" | "user:<name>"), derived client-side from the pieces
// (info features) or node attributes / communities (user features).
function featureValueOptions(fv) {
    if (!fv) return [];
    const userFeature = fv.startsWith("user:");
    const name = fv.slice(fv.indexOf(":") + 1);
    const set = new Set();
    if (!userFeature) {
        (state.diffusion.pieces || []).forEach((p) => (p.features || []).forEach((f) => {
            if (f.param === name && f.value != null && f.value !== "") set.add(String(f.value));
        }));
    } else if (state.communityData && state.communityData[name]) {
        Object.values(state.communityData[name]).forEach((v) => { if (v != null) set.add(String(v)); });
    } else if (state.graph) {
        state.graph.forEachNode((n) => { const v = nodeAttrVal(n, name); if (v != null && v !== "") set.add(String(v)); });
    }
    return [...set].sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));
}

// Repopulates the value selector for the current feature ("All values" + each distinct value).
function syncFtrajValueSelect() {
    const sel = $("diff-ftraj-value");
    const prev = sel.value;
    sel.innerHTML = "";
    sel.appendChild(option("__all__", "All values"));
    featureValueOptions($("diff-ftraj-feature").value).forEach((v) => sel.appendChild(option(v, v)));
    sel.value = [...sel.options].some((o) => o.value === prev) ? prev : "__all__";
    updateFtrajControls();
}

// The category selector only applies to the "all values" view (a specific value shows all four categories).
function updateFtrajControls() {
    $("diff-ftraj-category").disabled = $("diff-ftraj-value").value !== "__all__";
}

function renderFeatureTimeline() {
    const hint = $("diff-ftraj-hint");
    if (!state.diffusion.result) {
        hint.textContent = "Run a simulation and choose a feature to see how its values spread across the network over the iterations.";
        hint.hidden = false; clearCanvasById("diff-ftraj-canvas"); return;
    }
    syncFtrajFeatureSelect();
    syncFtrajValueSelect();
    if (!$("diff-ftraj-feature").value) {
        hint.textContent = "No features available. Add info-piece features, node attributes or communities.";
        hint.hidden = false; clearCanvasById("diff-ftraj-canvas"); return;
    }
    hint.hidden = true;
    fetchFeatureTrajectory();
}

async function fetchFeatureTrajectory() {
    if (!state.diffusion.result) return;
    const fv = $("diff-ftraj-feature").value;
    if (!fv) return;
    const body = {
        graphId: state.graphId,
        feature: fv.slice(fv.indexOf(":") + 1),
        userFeature: fv.startsWith("user:"),
        value: $("diff-ftraj-value").value,
        category: $("diff-ftraj-category").value,
        mode: $("diff-ftraj-mode").value,
        entity: $("diff-ftraj-entity").value,
    };
    setChartLoading("diff-ftraj-area", true);
    try {
        const res = await api("/api/diffusion/feature-trajectory", jsonBody(body));
        state.diffusion.ftraj = res;
        drawFeatureTimeline();
    } catch (e) { setStatus("Feature timeline failed: " + e.message, "error"); }
    finally { setChartLoading("diff-ftraj-area", false); }
}

function drawFeatureTimeline() {
    const res = state.diffusion.ftraj;
    if (!res) return;
    const entity = res.entity === "users" ? "users" : "pieces";
    const modeSuffix = res.mode === "cumulative" ? " (cumulative)" : " (per iteration)";
    const label = res.value
        ? res.feature + (res.userFeature ? " (user)" : "") + "=" + res.value + " — " + entity + modeSuffix
        : res.category + " · " + res.feature + (res.userFeature ? " (user)" : "") + " — " + entity + modeSuffix;
    drawTrajectoryInto("diff-ftraj-canvas", res, $("diff-ftraj-stack").value === "stacked", label);
}

function clearCanvasById(id) {
    clearChart(id);
}

// Shows/hides a spinner overlay on a chart area while its data is being fetched (the diffusion plots stream the
// simulation back from disk, which can take a moment on large runs).
function setChartLoading(areaId, on) {
    const area = $(areaId);
    if (!area) return;
    let el = area.querySelector(".chart-loading");
    if (on) {
        if (!el) {
            el = document.createElement("div");
            el.className = "chart-loading";
            el.innerHTML = '<span class="spinner"></span>Loading…';
            area.appendChild(el);
        }
        el.hidden = false;
    } else if (el) {
        el.hidden = true;
    }
}

/* ------------------- feature distributions (subtab) ----------------- */
// Uses RELISON's diffusion distribution classes (InformationFeatureDistribution / UserFeatureDistribution /
// MixedFeatureDistribution) computed server-side over the whole simulation.

// Shows the right feature selector(s) for the chosen distribution type and fills them from the schema.
function updateDistFields() {
    const type = $("diff-dist-type").value;
    const { info, user } = knownFeatureParams();
    toggleHidden("diff-dist-feat-field", type === "mixed");
    toggleHidden("diff-dist-info-field", type !== "mixed");
    toggleHidden("diff-dist-user-field", type !== "mixed");
    if (type === "info") fillSelectKeep("diff-dist-feature", info);
    else if (type === "user") fillSelectKeep("diff-dist-feature", user);
    else { fillSelectKeep("diff-dist-info", info); fillSelectKeep("diff-dist-user", user); }
}

function fillSelectKeep(id, names) {
    const sel = $(id);
    const prev = sel.value;
    sel.innerHTML = "";
    names.forEach((n) => sel.appendChild(option(n, n)));
    if (names.includes(prev)) sel.value = prev;
}

function renderDiffDistribution() {
    const hint = $("diff-dist-hint");
    if (!state.diffusion.result) {
        hint.textContent = "Run a simulation to see how received information distributes across feature values.";
        hint.hidden = false; clearCanvasById("diff-dist-canvas"); return;
    }
    updateDistFields();
    const type = $("diff-dist-type").value;
    const ok = type === "mixed" ? ($("diff-dist-info").value && $("diff-dist-user").value) : $("diff-dist-feature").value;
    if (!ok) {
        hint.textContent = "No suitable features available. Add info-piece features, node attributes or communities.";
        hint.hidden = false; clearCanvasById("diff-dist-canvas"); return;
    }
    hint.hidden = true;
    fetchDistribution();
}

async function fetchDistribution() {
    if (!state.diffusion.result) return;
    const type = $("diff-dist-type").value;
    const body = { graphId: state.graphId, type };
    if (type === "mixed") { body.infoFeature = $("diff-dist-info").value; body.userFeature = $("diff-dist-user").value; }
    else body.feature = $("diff-dist-feature").value;
    setChartLoading("diff-dist-area", true);
    try {
        const res = await api("/api/diffusion/distribution", jsonBody(body));
        state.diffusion.dist = res;
        drawDistribution();
        if (res.error) { $("diff-dist-hint").textContent = res.error; $("diff-dist-hint").hidden = false; }
    } catch (e) { setStatus("Distribution failed: " + e.message, "error"); }
    finally { setChartLoading("diff-dist-area", false); }
}

function drawDistribution() {
    const res = state.diffusion.dist;
    if (!res) return;
    if (res.type === "mixed") { drawHeatmap("diff-dist-canvas", res); return; }
    const items = (res.values || []).map((v) => ({ label: v.value, value: v.count }));
    if (!items.length) { clearCanvasById("diff-dist-canvas"); return; }
    drawBarChart("diff-dist-canvas", "diff-dist-tip", items, res.feature + " — received pieces", res.feature, "count");
}

// A cross-tab heatmap: info-feature values down the rows, user-feature values across the columns, cell = joint count.
function drawHeatmap(chartId, res) {
    const infoVals = res.infoValues || [], userVals = res.userValues || [], matrix = res.matrix || [];
    if (!infoVals.length || !userVals.length) { clearChart(chartId); return; }
    const colors = echartColors();
    const points = [];
    let max = 0;
    for (let r = 0; r < infoVals.length; r++) for (let c = 0; c < userVals.length; c++) {
        const value = Number((matrix[r] && matrix[r][c]) || 0);
        points.push([c, r, value]);
        if (value > max) max = value;
    }
    const option = chartBase(colors, "");
    option.grid = { left: 112, right: 72, top: 18, bottom: 94, containLabel: true };
    option.tooltip = { ...option.tooltip, trigger: "item", formatter: (p) =>
        escapeChartHtml(res.userFeature || "user feature") + ": <b>" + escapeChartHtml(userVals[p.data[0]]) + "</b><br>"
        + escapeChartHtml(res.infoFeature || "info feature") + ": <b>" + escapeChartHtml(infoVals[p.data[1]]) + "</b><br>Count: <b>" + escapeChartHtml(fmt(p.data[2])) + "</b>" };
    option.xAxis = { type: "category", name: res.userFeature || "user feature", nameLocation: "middle", nameGap: 74,
        nameTextStyle: { color: colors.text, fontSize: colors.fontSize + 1, fontWeight: 600 },
        data: userVals.map(String), axisLabel: { color: colors.muted, fontSize: colors.labelSize, rotate: userVals.length > 12 ? 40 : 0, hideOverlap: true },
        axisLine: { lineStyle: { color: colors.border } }, axisTick: { show: false }, splitArea: { show: true, areaStyle: { color: ["transparent", colors.background + "33"] } } };
    option.yAxis = { type: "category", name: res.infoFeature || "info feature", nameLocation: "middle", nameGap: 96,
        nameTextStyle: { color: colors.text, fontSize: colors.fontSize + 1, fontWeight: 600 },
        data: infoVals.map(String), axisLabel: { color: colors.muted, fontSize: colors.labelSize, hideOverlap: true }, inverse: true,
        axisLine: { lineStyle: { color: colors.border } }, axisTick: { show: true, inside: false, length: 6, lineStyle: { color: colors.border } }, splitArea: { show: true, areaStyle: { color: ["transparent", colors.background + "33"] } } };
    option.visualMap = { min: 0, max: Math.max(1, max), calculable: true, orient: "vertical", right: 4, top: "middle",
        inRange: { color: [colors.background, colors.accent] }, textStyle: { color: colors.muted } };
    option.series = [{ type: "heatmap", data: points, progressive: 4000,
        itemStyle: { borderWidth: 1, borderColor: colors.background }, emphasis: { itemStyle: { shadowBlur: 8, shadowColor: "rgba(0,0,0,0.35)" } } }];
    showEChart(chartId, option, (res.infoFeature || "info") + " × " + (res.userFeature || "user") + " distribution");
}
// Stacked-area variant of drawMultiLineChart: each series is a band, stacked to show composition over iterations.
function drawStackedAreaChart(container, series, label) {
    drawMultiLineChart(container, series, label, true, true);
}
