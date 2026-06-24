"use strict";

/* ------------------------------------------------------------------ *
 * RELISON GUI frontend.
 *
 * Resolves the UMD globals defensively (their exact names vary between
 * builds), then wires up loading, rendering, layout, interaction, the
 * node/edge tables and the computed-metrics dashboards.
 * ------------------------------------------------------------------ */

const Graph = (window.graphology && window.graphology.Graph) || window.graphology;
const lib = window.graphologyLibrary || {};
const FA2 = lib.layoutForceAtlas2 || window.graphologyLayoutForceAtlas2;
const SigmaClass = window.Sigma || (window.sigma && window.sigma.Sigma) || window.sigma;

/* ------------------------------- state ------------------------------ */

const state = {
    graphId: null,
    graph: null,          // graphology Graph instance
    renderer: null,       // Sigma renderer
    directed: true,
    weighted: false,
    fa2Running: false,
    fa2Raf: null,
    layoutTemp: 50,
    selectedNode: null,
    editFirstNode: null,
    selection: { node: null, mode: "highlight", partition: null },
    pathFocus: null,   // { nodes: Set, edges: Set("s|t") } highlighted from the Paths tab
    lastPaths: [],     // most recent shortest-path result (array of node-id arrays)

    // Computed results.
    metricData: {},        // vertex: label -> { node(string): value }
    metricOrder: [],       // vertex metric labels, in computation order
    pairData: {},          // edge: label -> { "s|t": value }  (computed over links)
    pairOrder: [],         // edge/pair metric labels, in computation order
    nodePairAgg: {},       // node-pair: label -> { average, min, max, estimated, totalPairs, evaluated, infinite, histogram[] }
    nodePairOrder: [],     // node-pair metric labels, in computation order
    graphMetrics: {},      // global: label -> value (graph + community metrics)
    communityData: {},     // algo -> { node(string): communityId }
    commMetricData: {},    // per-community: label -> { algorithm, values: { comm(string): value } }
    commMetricOrder: [],   // per-community metric labels, in computation order

    activeTab: "network",
    activeSubtab: "global",
    activeTableSubtab: "nodes",
    tables: {
        nodes: { sort: { col: "id", dir: 1 }, filters: {}, page: 0, pageSize: 100, headerSig: null },
        edges: { sort: { col: "source", dir: 1 }, filters: {}, page: 0, pageSize: 100, headerSig: null },
    },
    catalog: null,
    defs: { vertex: {}, graph: {}, pair: {}, community: {}, communityIndividual: {} },
};

/* ------------------------------ helpers ----------------------------- */

const $ = (id) => document.getElementById(id);

function setStatus(text, kind) {
    const el = $("status");
    el.textContent = text || "";
    el.className = "status" + (kind ? " " + kind : "");
}

async function api(url, options) {
    const res = await fetch(url, options);
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || ("Request failed: " + res.status));
    return data;
}

function fmt(v) {
    if (typeof v !== "number") return v;
    return Number.isInteger(v) ? v : v.toFixed(4);
}

function option(value, text) {
    const opt = document.createElement("option");
    opt.value = value;
    opt.textContent = text;
    return opt;
}

function jsonBody(obj) {
    return { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(obj) };
}

function pairKey(s, t) {
    return s + "|" + t;
}

// Converts a {key: value} object's values to numbers.
function numericMap(values) {
    const out = {};
    for (const k of Object.keys(values)) out[k] = Number(values[k]);
    return out;
}

function requireGraph() {
    if (!state.graphId) {
        setStatus("Load a network first.", "error");
        return false;
    }
    return true;
}

/* ------------------------------ loading ----------------------------- */

async function loadGraph() {
    const fileInput = $("file-input");
    if (!fileInput.files || fileInput.files.length === 0) {
        setStatus("Choose an edge-list file first.", "error");
        return;
    }

    const form = new FormData();
    form.append("file", fileInput.files[0]);
    form.append("directed", $("opt-directed").checked);
    form.append("weighted", $("opt-weighted").checked);
    form.append("multigraph", $("opt-multigraph").checked);
    form.append("selfloops", $("opt-selfloops").checked);

    setStatus("Loading network…", "busy");
    $("btn-load").disabled = true;
    try {
        const data = await api("/api/graph/load", { method: "POST", body: form });
        state.graphId = data.graphId;
        state.directed = data.stats.directed;
        state.weighted = data.stats.weighted;
        resetResults();
        resetTableViews();
        clearSelection();
        $("btn-community-metrics").disabled = true;
        $("btn-indiv-comm").disabled = true;
        $("community-result").textContent = "";
        $("indiv-comm-partition").innerHTML = "";
        state.pathFocus = null;
        state.lastPaths = [];
        $("paths-table").innerHTML = "";
        $("path-source").value = "";
        $("path-target").value = "";
        $("path-summary").textContent = "Pick a source and a target, then find their shortest paths.";
        switchTab("network");          // ensure the (sized) network pane is visible before rendering
        renderGraph(data.graph);
        updateOverview(data.stats);
        if (state.renderer) state.renderer.refresh();
        refreshAfterCompute();
        setStatus("Loaded " + data.stats.nodes + " nodes, " + data.stats.edges + " edges.");
    } catch (e) {
        setStatus(e.message, "error");
    } finally {
        $("btn-load").disabled = false;
    }
}

function resetResults() {
    state.metricData = {};
    state.metricOrder = [];
    state.pairData = {};
    state.pairOrder = [];
    state.nodePairAgg = {};
    state.nodePairOrder = [];
    state.graphMetrics = {};
    state.communityData = {};
    state.commMetricData = {};
    state.commMetricOrder = [];
}

// Clears table filters/paging on a fresh load (sort defaults kept).
function resetTableViews() {
    for (const k of ["nodes", "edges"]) {
        const t = state.tables[k];
        t.filters = {};
        t.page = 0;
        t.headerSig = null;
    }
}

/* ----------------------------- rendering ---------------------------- */

function renderGraph(serialized) {
    stopLayout();
    if (state.renderer) { state.renderer.kill(); state.renderer = null; }

    const graph = new Graph({
        type: serialized.options ? serialized.options.type : (state.directed ? "directed" : "undirected"),
        multi: serialized.options ? serialized.options.multi : false,
        allowSelfLoops: serialized.options ? serialized.options.allowSelfLoops : true,
    });
    graph.import(serialized);
    state.graph = graph;

    graph.forEachNode((node) => graph.setNodeAttribute(node, "color", "#4f9dff"));
    applyAppearance();

    $("empty-hint").style.display = "none";
    state.renderer = new SigmaClass(graph, $("sigma-container"), {
        defaultEdgeType: state.directed ? "arrow" : "line",
        renderEdgeLabels: false,
        labelDensity: 0.5,
        labelRenderedSizeThreshold: 8,
        // The container can be momentarily hidden (zero-size) if a graph is loaded from another tab;
        // tolerate it and refresh once the Network tab becomes visible.
        allowInvalidContainer: true,
    });

    state.renderer.on("clickNode", ({ node }) => {
        if ($("edit-mode").checked) handleEditNodeClick(node);
        else selectNode(node);
    });
    state.renderer.on("clickStage", (e) => {
        if ($("edit-mode").checked) {
            const coords = state.renderer.viewportToGraph({ x: e.event.x, y: e.event.y });
            editAddNodeAt(coords);
        } else {
            clearSelection();
        }
    });
}

function handleEditNodeClick(node) {
    if (state.editFirstNode == null) {
        state.editFirstNode = node;
        selectNode(node);
        setStatus("Edge source: " + node + ". Click a target node (or Delete to remove).");
    } else if (state.editFirstNode === node) {
        state.editFirstNode = null;
        setStatus("Edge cancelled.");
    } else {
        editAddEdge(state.editFirstNode, node);
        state.editFirstNode = null;
    }
}

function updateOverview(stats) {
    $("ov-nodes").textContent = stats.nodes;
    $("ov-edges").textContent = stats.edges;
    $("ov-type").textContent =
        (stats.directed ? "directed" : "undirected") + (stats.weighted ? ", weighted" : "");
}

/* ---------------------------- appearance ---------------------------- */

function rebuildAppearanceOptions() {
    const metricNames = state.metricOrder;
    const communityNames = Object.keys(state.communityData);

    const sizeSelect = $("size-by");
    const sizeCurrent = sizeSelect.value;
    sizeSelect.innerHTML = '<option value="">— degree (default) —</option>';
    for (const name of metricNames) sizeSelect.appendChild(option(name, name));
    sizeSelect.value = metricNames.includes(sizeCurrent) ? sizeCurrent : "";

    const colorSelect = $("color-by");
    const colorCurrent = colorSelect.value;
    colorSelect.innerHTML = '<option value="">— none —</option>';
    for (const name of metricNames) colorSelect.appendChild(option("metric:" + name, name));
    for (const algo of communityNames) colorSelect.appendChild(option("community:" + algo, "community: " + algo));
    const values = Array.from(colorSelect.options).map((o) => o.value);
    colorSelect.value = values.includes(colorCurrent) ? colorCurrent : "";

    // Edge thickness: uniform + each computed edge (link) metric.
    const edgeSizeSelect = $("edge-size-by");
    const edgeCurrent = edgeSizeSelect.value;
    edgeSizeSelect.innerHTML = '<option value="">— uniform —</option>';
    for (const name of state.pairOrder) edgeSizeSelect.appendChild(option(name, name));
    edgeSizeSelect.value = state.pairOrder.includes(edgeCurrent) ? edgeCurrent : "";
}

function degreeValues() {
    const g = state.graph, v = {};
    g.forEachNode((n) => (v[n] = g.degree(n)));
    return v;
}

function applyAppearance() {
    const graph = state.graph;
    if (!graph) return;

    const sizeBy = $("size-by").value;
    const sizeValues = sizeBy ? (state.metricData[sizeBy] || {}) : degreeValues();
    let min = Infinity, max = -Infinity;
    for (const n of graph.nodes()) {
        const val = sizeValues[n] ?? 0;
        if (val < min) min = val;
        if (val > max) max = val;
    }
    const span = (max - min) || 1;
    graph.forEachNode((n) => graph.setNodeAttribute(n, "size", 2 + 12 * (((sizeValues[n] ?? 0) - min) / span)));

    const colorSel = $("color-by").value;
    if (!colorSel) graph.forEachNode((n) => graph.setNodeAttribute(n, "color", "#4f9dff"));
    else if (colorSel.startsWith("community:")) colorByCommunity(colorSel.slice("community:".length));
    else if (colorSel.startsWith("metric:")) colorByMetric(colorSel.slice("metric:".length));

    applyEdgeAppearance();

    if (state.renderer) state.renderer.refresh();
}

function colorByMetric(metricName) {
    const graph = state.graph;
    const values = state.metricData[metricName] || {};
    const low = $("node-color-low").value, high = $("node-color-high").value;
    let min = Infinity, max = -Infinity;
    for (const n of graph.nodes()) {
        const v = values[n] ?? 0;
        if (v < min) min = v;
        if (v > max) max = v;
    }
    const span = (max - min) || 1;
    graph.forEachNode((n) => graph.setNodeAttribute(n, "color", lerpHex(low, high, ((values[n] ?? 0) - min) / span)));
}

function colorByCommunity(algo) {
    const graph = state.graph;
    const data = state.communityData[algo] || {};
    graph.forEachNode((n) => graph.setNodeAttribute(n, "color", categorical(data[n] ?? 0)));
}

// Edge thickness (by a computed link metric) and colour (uniform default / single / average of endpoints).
function applyEdgeAppearance() {
    const graph = state.graph;
    const sizeBy = $("edge-size-by").value;
    const sizeData = sizeBy ? (state.pairData[sizeBy] || {}) : null;
    let min = Infinity, max = -Infinity;
    if (sizeData) {
        graph.forEachEdge((e, a, s, t) => {
            const v = sizeData[pairKey(s, t)];
            if (v === undefined) return;
            if (v < min) min = v;
            if (v > max) max = v;
        });
    }
    const span = (max - min) || 1;

    const mode = $("edge-color-mode").value;
    const single = $("edge-color-single").value;
    const defColor = "#888888";

    graph.forEachEdge((edge, attr, s, t) => {
        if (sizeData) {
            const v = sizeData[pairKey(s, t)];
            graph.setEdgeAttribute(edge, "size", v === undefined ? 0.5 : 0.5 + 5.5 * ((v - min) / span));
        } else {
            graph.setEdgeAttribute(edge, "size", 1);
        }

        if (mode === "single") graph.setEdgeAttribute(edge, "color", single);
        else if (mode === "endpoints") graph.setEdgeAttribute(edge, "color", averageColor(graph.getNodeAttribute(s, "color"), graph.getNodeAttribute(t, "color")));
        else graph.setEdgeAttribute(edge, "color", defColor);
    });
}

// Linear interpolation between two hex colours, returning rgb().
function lerpHex(c1, c2, t) {
    t = Math.max(0, Math.min(1, t));
    const a = colorToRgb(c1) || { r: 79, g: 157, b: 255 };
    const b = colorToRgb(c2) || { r: 255, g: 91, b: 91 };
    const ch = (k) => Math.round(a[k] + (b[k] - a[k]) * t);
    return `rgb(${ch("r")},${ch("g")},${ch("b")})`;
}

function averageColor(c1, c2) {
    const a = colorToRgb(c1) || { r: 128, g: 128, b: 128 };
    const b = colorToRgb(c2) || { r: 128, g: 128, b: 128 };
    return `rgb(${Math.round((a.r + b.r) / 2)},${Math.round((a.g + b.g) / 2)},${Math.round((a.b + b.b) / 2)})`;
}

// Distinct colour per community. Returns rgb() (not hsl()) because sigma's WebGL renderer
// only parses hex/rgb/rgba — an hsl() string renders as black.
function categorical(i) {
    const { r, g, b } = hslToRgb((i * 137.508) % 360, 0.65, 0.55);
    return `rgb(${r},${g},${b})`;
}

/* ----------------------------- selection ---------------------------- */

// Selects a node (from a click or the UI) and refreshes the info panel + visualization emphasis.
function selectNode(node) {
    if (!state.graph || !state.graph.hasNode(node)) return;
    state.selectedNode = node;
    state.selection.node = node;
    $("select-node-input").value = node;
    renderNodeInfo(node);
    updatePartitionField();
    applyReducers();
    if (state.selection.mode === "ego-only" || state.selection.mode === "community-only") {
        renderTable(state.activeTableSubtab);
    }
}

function renderNodeInfo(node) {
    const graph = state.graph;
    const table = $("node-info-table");
    table.innerHTML = "";
    if (node == null || !graph || !graph.hasNode(node)) {
        table.innerHTML = '<tr><td class="muted" colspan="2">No node selected.</td></tr>';
        return;
    }
    const rows = [["id", node], ["degree", graph.degree(node)]];
    if (state.directed) {
        rows.push(["in-degree", graph.inDegree(node)]);
        rows.push(["out-degree", graph.outDegree(node)]);
    }
    for (const name of state.metricOrder) {
        const v = state.metricData[name][node];
        if (v !== undefined) rows.push([name, fmt(v)]);
    }
    for (const algo of Object.keys(state.communityData)) {
        const c = state.communityData[algo][node];
        if (c !== undefined) rows.push(["community · " + algo, c]);
    }
    for (const [k, v] of rows) {
        const tr = document.createElement("tr");
        tr.innerHTML = `<td>${k}</td><td>${v}</td>`;
        table.appendChild(tr);
    }
}

function clearSelection() {
    state.selectedNode = null;
    state.selection.node = null;
    $("select-node-input").value = "";
    renderNodeInfo(null);
    applyReducers();
    renderTable(state.activeTableSubtab);
}

// Shows the community-partition picker only for community view modes, and keeps its options in sync.
function updatePartitionField() {
    const mode = state.selection.mode;
    const isCommunity = mode === "community-highlight" || mode === "community-only";
    $("select-partition-field").hidden = !isCommunity;
    const algos = Object.keys(state.communityData);
    setSelectOptions("select-partition", algos, algos);
    if (!state.selection.partition || !algos.includes(state.selection.partition)) {
        state.selection.partition = algos[0] || null;
        if (state.selection.partition) $("select-partition").value = state.selection.partition;
    }
}

// Computes the focus set of nodes for the current selection + mode, and whether other nodes are hidden.
function selectionFocus() {
    const sel = state.selection;
    const g = state.graph;
    if (!g || !sel.node || sel.mode === "none" || !g.hasNode(sel.node)) return null;

    let nodes;
    let only = false;
    if (sel.mode === "highlight") {
        nodes = new Set([sel.node]);
    } else if (sel.mode === "ego-highlight" || sel.mode === "ego-only") {
        nodes = new Set([sel.node]);
        g.neighbors(sel.node).forEach((n) => nodes.add(n));
        only = sel.mode === "ego-only";
    } else { // community-highlight / community-only
        const data = state.communityData[sel.partition];
        if (!data) return null;
        const comm = data[sel.node];
        nodes = new Set();
        g.forEachNode((n) => { if (data[n] === comm) nodes.add(n); });
        only = sel.mode === "community-only";
    }
    return { nodes, only, selected: sel.node };
}

function setSelectionMode(mode) {
    state.selection.mode = mode;
    updatePartitionField();
    applyReducers();
    renderTable(state.activeTableSubtab); // "show only" modes restrict the tables
}

function setSelectionPartition(partition) {
    state.selection.partition = partition;
    applyReducers();
    renderTable(state.activeTableSubtab);
}

function selectFromInput() {
    const v = $("select-node-input").value.trim();
    if (!v) { clearSelection(); return; }
    if (!state.graph || !state.graph.hasNode(v)) { setStatus("No node with id " + v + ".", "error"); return; }
    selectNode(v);
}

// Turns a plain text input into a type-to-filter node selector. Unlike a <datalist> (which becomes sluggish past a
// few thousand entries), this queries the live graph on each keystroke and shows only the top matches, so it scales
// to networks with tens of thousands of nodes. Picking an option sets the value and fires a "change" event, so any
// existing change handler on the input keeps working.
const NODE_COMBO_MAX = 50;
function attachNodeCombo(input) {
    if (!input || input.dataset.combo === "1") return;
    input.dataset.combo = "1";
    input.setAttribute("autocomplete", "off");
    input.removeAttribute("list");

    const wrap = document.createElement("span");
    wrap.className = "combo";
    input.parentNode.insertBefore(wrap, input);
    wrap.appendChild(input);
    const list = document.createElement("ul");
    list.className = "combo-list";
    list.hidden = true;
    wrap.appendChild(list);

    let items = [];
    let active = -1;

    function close() { list.hidden = true; active = -1; }

    function compute() {
        const q = input.value.trim().toLowerCase();
        const pref = [], sub = [];
        if (state.graph) {
            const all = state.graph.nodes();
            for (let i = 0; i < all.length; i++) {
                const node = all[i];
                const lc = String(node).toLowerCase();
                if (!q) { pref.push(node); if (pref.length >= NODE_COMBO_MAX) break; continue; }
                const idx = lc.indexOf(q);
                if (idx === 0) { pref.push(node); if (pref.length >= NODE_COMBO_MAX) break; }
                else if (idx > 0 && sub.length < NODE_COMBO_MAX) sub.push(node);
            }
        }
        items = pref.concat(sub).slice(0, NODE_COMBO_MAX);
    }

    function render() {
        list.innerHTML = "";
        if (!items.length) { close(); return; }
        items.forEach((node, i) => {
            const li = document.createElement("li");
            li.textContent = node;
            if (i === active) li.className = "active";
            li.addEventListener("mousedown", (e) => { e.preventDefault(); pick(node); });
            list.appendChild(li);
        });
        list.hidden = false;
        if (active >= 0 && list.children[active]) list.children[active].scrollIntoView({ block: "nearest" });
    }

    function open() { compute(); render(); }

    function pick(node) {
        input.value = node;
        close();
        input.dispatchEvent(new Event("change"));
    }

    input.addEventListener("input", open);
    input.addEventListener("focus", open);
    input.addEventListener("blur", () => setTimeout(close, 150));
    input.addEventListener("keydown", (e) => {
        if (list.hidden) return;
        if (e.key === "ArrowDown") { e.preventDefault(); active = Math.min(active + 1, items.length - 1); render(); }
        else if (e.key === "ArrowUp") { e.preventDefault(); active = Math.max(active - 1, 0); render(); }
        else if (e.key === "Enter") {
            if (active >= 0 && active < items.length) { e.preventDefault(); pick(items[active]); }
            else { close(); input.dispatchEvent(new Event("change")); }
        }
        else if (e.key === "Escape") { close(); }
    });
}

// Upgrades every node-id input into a searchable selector. Safe to call repeatedly (each input is wired once).
function initNodeCombos() {
    ["select-node-input", "path-source", "path-target", "add-edge-source", "add-edge-target"]
        .forEach((id) => attachNodeCombo($(id)));
}

/* ------------------------------ layout ------------------------------ */

function startLayout() {
    if (!state.graph) { setStatus("Load a network first.", "error"); return; }
    if (state.fa2Running) { stopLayout(); return; }

    const useFA2 = FA2 && typeof FA2.assign === "function";
    let settings = null;
    if (useFA2) {
        try { settings = FA2.inferSettings ? FA2.inferSettings(state.graph) : {}; }
        catch (e) { console.warn("inferSettings failed, using defaults", e); settings = {}; }
        const lp = layoutParams();
        settings.scalingRatio = (settings.scalingRatio || 1) * lp.scaling;
        settings.gravity = lp.gravity;
        settings.slowDown = 1 / Math.max(0.1, lp.speed);
    }
    console.log("Layout engine:", useFA2 ? "graphology ForceAtlas2" : "built-in force-directed");

    state.fa2Running = true;
    state.layoutTemp = 50;
    $("btn-layout").textContent = "Stop ForceAtlas2";

    const step = () => {
        try {
            if (useFA2) FA2.assign(state.graph, { iterations: 1, settings });
            else builtinForceStep();
        } catch (e) {
            console.error("Layout error", e);
            setStatus("Layout error: " + e.message, "error");
            stopLayout();
            return;
        }
        if (state.renderer) state.renderer.refresh();
        if (state.fa2Running) state.fa2Raf = requestAnimationFrame(step);
    };
    step();
}

function layoutParams() {
    return {
        scaling: parseFloat($("layout-scaling").value) || 1,
        gravity: parseFloat($("layout-gravity").value) || 0,
        speed: parseFloat($("layout-speed").value) || 1,
    };
}

function builtinForceStep() {
    const g = state.graph;
    const nodes = g.nodes();
    const n = nodes.length || 1;
    const lp = layoutParams();
    const k = Math.max(10, Math.sqrt(250000 / n)) * lp.scaling;
    const disp = new Map(), x = new Map(), y = new Map();
    for (const v of nodes) {
        disp.set(v, { x: 0, y: 0 });
        x.set(v, g.getNodeAttribute(v, "x") || 0);
        y.set(v, g.getNodeAttribute(v, "y") || 0);
    }
    for (let i = 0; i < nodes.length; i++) {
        const v = nodes[i];
        for (let j = i + 1; j < nodes.length; j++) {
            const u = nodes[j];
            let dx = x.get(v) - x.get(u), dy = y.get(v) - y.get(u);
            let dist = Math.hypot(dx, dy) || 0.01;
            const rep = (k * k) / dist;
            const fx = (dx / dist) * rep, fy = (dy / dist) * rep;
            const dv = disp.get(v), du = disp.get(u);
            dv.x += fx; dv.y += fy; du.x -= fx; du.y -= fy;
        }
    }
    g.forEachEdge((edge, attr, s, t) => {
        if (s === t) return;
        let dx = x.get(s) - x.get(t), dy = y.get(s) - y.get(t);
        let dist = Math.hypot(dx, dy) || 0.01;
        const att = (dist * dist) / k;
        const fx = (dx / dist) * att, fy = (dy / dist) * att;
        const ds = disp.get(s), dt = disp.get(t);
        ds.x -= fx; ds.y -= fy; dt.x += fx; dt.y += fy;
    });
    // Gravity: pull every node toward the centre, proportional to its distance.
    const gpull = 0.01 * lp.gravity;
    if (gpull > 0) {
        for (const v of nodes) {
            const d = disp.get(v);
            d.x -= x.get(v) * gpull;
            d.y -= y.get(v) * gpull;
        }
    }

    const temp = state.layoutTemp;
    for (const v of nodes) {
        const d = disp.get(v);
        const len = Math.hypot(d.x, d.y) || 0.01;
        const lim = Math.min(len, temp) * lp.speed;
        g.setNodeAttribute(v, "x", x.get(v) + (d.x / len) * lim);
        g.setNodeAttribute(v, "y", y.get(v) + (d.y / len) * lim);
    }
    state.layoutTemp = Math.max(1, temp * 0.98);
}

function stopLayout() {
    state.fa2Running = false;
    if (state.fa2Raf) cancelAnimationFrame(state.fa2Raf);
    state.fa2Raf = null;
    $("btn-layout").textContent = "Start ForceAtlas2";
}

function resetLayout() {
    const graph = state.graph;
    if (!graph) return;
    stopLayout();
    const nodes = graph.nodes();
    const n = nodes.length;
    nodes.forEach((node, i) => {
        const angle = (2 * Math.PI * i) / Math.max(1, n);
        graph.setNodeAttribute(node, "x", Math.cos(angle) * 100);
        graph.setNodeAttribute(node, "y", Math.sin(angle) * 100);
    });
    if (state.renderer) state.renderer.refresh();
}

/* ------------------------------ catalog ----------------------------- */

async function loadCatalog() {
    try {
        const cat = await api("/api/metrics");
        state.catalog = cat;
        state.defs = {
            vertex: indexById(cat.vertex), graph: indexById(cat.graph), pair: indexById(cat.pair),
            community: indexById(cat.community), communityIndividual: indexById(cat.communityIndividual),
        };
        fillSelect("vertex-metric", cat.vertex);
        fillSelect("graph-metric", cat.graph);
        fillSelect("pair-metric", cat.pair);
        fillCommunitySelect("community-algo", cat.community);
        fillSelect("indiv-comm-metric", cat.communityIndividual);
        renderParams("vertex-params", "vertex", $("vertex-metric").value);
        renderParams("graph-params", "graph", $("graph-metric").value);
        renderParams("pair-params", "pair", $("pair-metric").value);
        renderParams("community-params", "community", $("community-algo").value);
        renderParams("indiv-comm-params", "communityIndividual", $("indiv-comm-metric").value);
    } catch (e) {
        setStatus("Could not load metric catalog: " + e.message, "error");
    }
}

function indexById(items) {
    const map = {};
    (items || []).forEach((it) => (map[it.id] = it));
    return map;
}

function fillSelect(id, items) {
    const select = $(id);
    select.innerHTML = "";
    (items || []).forEach((m) => select.appendChild(option(m.id, m.label)));
}

function fillCommunitySelect(id, items) {
    const select = $(id);
    select.innerHTML = "";
    const groups = {};
    (items || []).forEach((a) => (groups[a.group] = groups[a.group] || []).push(a));
    for (const group of Object.keys(groups)) {
        const og = document.createElement("optgroup");
        og.label = group;
        groups[group].forEach((a) => og.appendChild(option(a.id, a.label)));
        select.appendChild(og);
    }
}

// Renders the parameter controls for the currently-selected metric/algorithm into a container.
function renderParams(containerId, family, metricId) {
    const container = $(containerId);
    container.innerHTML = "";
    const def = state.defs?.[family]?.[metricId];
    if (!def || !def.params) return;
    for (const p of def.params) {
        const label = document.createElement("label");
        const span = document.createElement("span");
        span.textContent = p.label;
        label.appendChild(span);

        let input;
        if (p.options) { // orientation or choice → dropdown
            input = document.createElement("select");
            p.options.forEach((o) => input.appendChild(option(o, o)));
            input.value = p.default;
        } else if (p.type === "bool") {
            input = document.createElement("input");
            input.type = "checkbox";
            input.checked = !!p.default;
        } else {
            input = document.createElement("input");
            input.type = "number";
            if (p.type === "double") input.step = "any";
            input.value = p.default;
        }
        input.dataset.param = p.name;
        input.dataset.ptype = p.type;
        label.appendChild(input);
        container.appendChild(label);
    }
}

// Collects the current parameter values from a container into a plain object.
function collectParams(containerId) {
    const container = $(containerId);
    const params = {};
    container.querySelectorAll("[data-param]").forEach((input) => {
        const name = input.dataset.param;
        const type = input.dataset.ptype;
        if (type === "bool") params[name] = input.checked;
        else if (type === "double") params[name] = parseFloat(input.value);
        else if (type === "int") params[name] = parseInt(input.value, 10);
        else params[name] = input.value;
    });
    return params;
}

/* --------------------------- metric runners ------------------------- */

async function runVertexMetric() {
    if (!requireGraph()) return;
    const metric = $("vertex-metric").value;
    setStatus("Computing " + metric + "…", "busy");
    $("btn-vertex").disabled = true;
    try {
        const res = await api("/api/metrics/vertex",
            jsonBody({ graphId: state.graphId, metric, params: collectParams("vertex-params") }));
        const values = {};
        for (const k of Object.keys(res.values)) values[k] = Number(res.values[k]);
        if (!state.metricOrder.includes(res.label)) state.metricOrder.push(res.label);
        state.metricData[res.label] = values;
        rebuildAppearanceOptions();
        $("size-by").value = res.label;
        $("color-by").value = "metric:" + res.label;
        applyAppearance();
        $("vertex-result").textContent = res.label + " — avg " + fmt(res.average);
        refreshAfterCompute();
        setStatus("Computed " + res.label + ".");
    } catch (e) {
        setStatus(e.message, "error");
    } finally {
        $("btn-vertex").disabled = false;
    }
}

async function runGraphMetric() {
    if (!requireGraph()) return;
    const metric = $("graph-metric").value;
    setStatus("Computing " + metric + "…", "busy");
    $("btn-graph").disabled = true;
    try {
        const res = await api("/api/metrics/graph",
            jsonBody({ graphId: state.graphId, metric, params: collectParams("graph-params") }));
        state.graphMetrics[res.label] = res.value;
        refreshAfterCompute();
        setStatus("Computed " + res.label + " = " + fmt(res.value));
    } catch (e) {
        setStatus(e.message, "error");
    } finally {
        $("btn-graph").disabled = false;
    }
}

async function runPairMetric() {
    if (!requireGraph()) return;
    const metric = $("pair-metric").value;
    const allPairs = $("pair-allpairs").checked;
    const onlyLinks = !allPairs;
    setStatus("Computing " + metric + (allPairs ? " over all node pairs" : "") + "…", "busy");
    $("btn-pair").disabled = true;
    try {
        const res = await api("/api/metrics/pair",
            jsonBody({ graphId: state.graphId, metric, onlyLinks, params: collectParams("pair-params") }));
        if (allPairs) {
            if (!state.nodePairOrder.includes(res.label)) state.nodePairOrder.push(res.label);
            state.nodePairAgg[res.label] = res;
            const how = res.estimated ? ("estimated from " + res.evaluated.toLocaleString() + " sampled pairs") :
                ("exact over " + res.totalPairs.toLocaleString() + " pairs");
            $("pair-result").textContent = res.label + " — avg " + fmt(res.average) + " (" + how + ")";
        } else {
            const map = {};
            res.values.forEach((e) => {
                map[pairKey(e.source, e.target)] = Number(e.value);
                if (!state.directed) map[pairKey(e.target, e.source)] = Number(e.value);
            });
            if (!state.pairOrder.includes(res.label)) state.pairOrder.push(res.label);
            state.pairData[res.label] = map;
            rebuildAppearanceOptions(); // expose the new metric in "Thicken edges by"
            $("pair-result").textContent = res.label + " — " + res.count + " links, avg " + fmt(res.average);
        }
        refreshAfterCompute();
        setStatus("Computed " + res.label + ".");
    } catch (e) {
        setStatus(e.message, "error");
    } finally {
        $("btn-pair").disabled = false;
    }
}

/* ----------------------------- communities -------------------------- */

async function detectCommunity() {
    if (!requireGraph()) return;
    const algorithm = $("community-algo").value;
    setStatus("Detecting communities (" + algorithm + ")…", "busy");
    $("btn-community").disabled = true;
    try {
        const res = await api("/api/communities",
            jsonBody({ graphId: state.graphId, algorithm, params: collectParams("community-params") }));
        state.communityData[res.algorithm] = res.values;
        rebuildAppearanceOptions();
        $("color-by").value = "community:" + res.algorithm;
        applyAppearance();
        $("community-result").textContent = res.label + " — " + res.numCommunities + " communities";
        $("btn-community-metrics").disabled = false;
        $("btn-community-metrics").dataset.algo = res.algorithm;
        $("btn-indiv-comm").disabled = false;
        const algos = Object.keys(state.communityData);
        setSelectOptions("indiv-comm-partition", algos, algos);
        $("indiv-comm-partition").value = res.algorithm;
        updatePartitionField();
        applyReducers();
        refreshAfterCompute();
        setStatus("Detected " + res.numCommunities + " communities.");
    } catch (e) {
        setStatus(e.message, "error");
    } finally {
        $("btn-community").disabled = false;
    }
}

async function runCommunityMetrics() {
    if (!requireGraph()) return;
    const algorithm = $("btn-community-metrics").dataset.algo;
    if (!algorithm) return;
    setStatus("Computing community metrics…", "busy");
    try {
        const res = await api("/api/communities/metrics", jsonBody({ graphId: state.graphId, algorithm }));
        res.metrics.forEach((m) => {
            if (!m.error) state.graphMetrics[m.label + " (" + algorithm + ")"] = m.value;
        });
        refreshAfterCompute();
        setStatus("Community metrics done.");
    } catch (e) {
        setStatus(e.message, "error");
    }
}

async function runIndividualCommMetric() {
    if (!requireGraph()) return;
    const algorithm = $("indiv-comm-partition").value;
    if (!algorithm || !state.communityData[algorithm]) { setStatus("Detect a partition first.", "error"); return; }
    const metric = $("indiv-comm-metric").value;
    setStatus("Computing per-community " + metric + "…", "busy");
    $("btn-indiv-comm").disabled = true;
    try {
        const res = await api("/api/communities/individual",
            jsonBody({ graphId: state.graphId, algorithm, metric, params: collectParams("indiv-comm-params") }));
        const label = res.label + " · " + algorithm;
        if (!state.commMetricOrder.includes(label)) state.commMetricOrder.push(label);
        state.commMetricData[label] = { algorithm, values: numericMap(res.values) };
        refreshAfterCompute();
        setStatus("Computed " + res.label + " over " + algorithm + " — avg " + fmt(res.average) + ".");
    } catch (e) {
        setStatus(e.message, "error");
    } finally {
        $("btn-indiv-comm").disabled = false;
    }
}

/* ------------------------------- paths ------------------------------ */

async function findPaths() {
    if (!requireGraph()) return;
    const source = $("path-source").value.trim();
    const target = $("path-target").value.trim();
    if (!source || !target) { setStatus("Enter both source and target.", "error"); return; }
    setStatus("Finding shortest paths…", "busy");
    $("btn-find-paths").disabled = true;
    try {
        const res = await api("/api/paths", jsonBody({ graphId: state.graphId, source, target }));
        state.lastPaths = res.paths || [];
        if (res.length < 0) {
            $("path-summary").textContent = "No path from " + source + " to " + target + ".";
        } else {
            $("path-summary").textContent =
                res.count + (res.truncated ? "+" : "") + " shortest path(s) of length " + res.length +
                " from " + source + " to " + target + (res.truncated ? " (showing first " + res.count + ")" : "") + ".";
        }
        renderPathsTable(state.lastPaths);
        setStatus("Found " + res.count + " path(s).");
    } catch (e) {
        setStatus(e.message, "error");
    } finally {
        $("btn-find-paths").disabled = false;
    }
}

function renderPathsTable(paths) {
    const table = $("paths-table");
    table.innerHTML = "";
    const thead = document.createElement("thead");
    thead.innerHTML = "<tr><th>#</th><th>path</th><th>action</th></tr>";
    table.appendChild(thead);
    const tbody = document.createElement("tbody");
    paths.forEach((p, i) => {
        const tr = document.createElement("tr");
        const idx = document.createElement("td"); idx.textContent = i + 1;
        const path = document.createElement("td"); path.textContent = p.join(" → "); path.style.textAlign = "left";
        const act = document.createElement("td");
        const btn = document.createElement("button");
        btn.textContent = "Highlight";
        btn.style.width = "auto"; btn.style.margin = "0"; btn.style.padding = "2px 8px";
        btn.addEventListener("click", () => highlightPaths([p]));
        act.appendChild(btn);
        tr.append(idx, path, act);
        tbody.appendChild(tr);
    });
    table.appendChild(tbody);
}

// Highlights one or more paths on the network (dims everything else) and switches to the Network tab.
function highlightPaths(paths) {
    if (!paths.length) return;
    const nodes = new Set();
    const edges = new Set();
    for (const p of paths) {
        for (let i = 0; i < p.length; i++) {
            nodes.add(p[i]);
            if (i > 0) {
                edges.add(pairKey(p[i - 1], p[i]));
                if (!state.directed) edges.add(pairKey(p[i], p[i - 1]));
            }
        }
    }
    state.pathFocus = { nodes, edges, only: $("path-display").value === "only" };
    switchTab("network");
    applyReducers();
}

function clearPathHighlight() {
    state.pathFocus = null;
    applyReducers();
}

/* ------------------------------ editing ----------------------------- */

function setEditMode(on) {
    $("edit-help").hidden = !on;
    state.editFirstNode = null;
}

async function editAddNodeAt(coords) {
    if (!requireGraph() || !state.graph) return;
    let id = state.graph.order;
    while (state.graph.hasNode(String(id))) id++;
    try {
        const res = await api("/api/graph/" + state.graphId + "/node", jsonBody({ node: id }));
        state.graph.addNode(String(id), { label: String(id), x: coords.x, y: coords.y, size: 4, color: "#4f9dff" });
        onGraphEdited(res.stats);
        setStatus("Added node " + id + ".");
    } catch (e) { setStatus(e.message, "error"); }
}

async function editAddEdge(source, target, weight) {
    const w = weight == null ? 1.0 : weight;
    try {
        const res = await api("/api/graph/" + state.graphId + "/edge", jsonBody({ source, target, weight: w }));
        ensureNode(source);
        ensureNode(target);
        if (!state.graph.hasEdge(source, target)) state.graph.addEdge(source, target, { weight: w });
        onGraphEdited(res.stats);
        setStatus("Added edge " + source + " → " + target + ".");
    } catch (e) { setStatus(e.message, "error"); }
}

// Adds a node to the client graph if it is not present yet (the server creates endpoints automatically).
function ensureNode(id) {
    if (!state.graph.hasNode(id)) {
        state.graph.addNode(id, { label: id, x: (Math.random() - 0.5) * 50, y: (Math.random() - 0.5) * 50, size: 4, color: "#4f9dff" });
    }
}

async function editDeleteNode(node) {
    try {
        const res = await api("/api/graph/" + state.graphId + "/node/" + node, { method: "DELETE" });
        if (state.graph.hasNode(node)) state.graph.dropNode(node);
        onGraphEdited(res.stats);
        setStatus("Removed node " + node + ".");
    } catch (e) { setStatus(e.message, "error"); }
}

function onGraphEdited(stats) {
    // Server cleared its caches, so all computed results are stale.
    resetResults();
    rebuildAppearanceOptions();
    applyAppearance();
    if (stats) { $("ov-nodes").textContent = stats.nodes; $("ov-edges").textContent = stats.edges; }
    $("btn-community-metrics").disabled = true;
    $("btn-indiv-comm").disabled = true;
    state.pathFocus = null;
    if (state.selectedNode != null && !state.graph.hasNode(state.selectedNode)) clearSelection();
    refreshAfterCompute();
    applyReducers();
    if (state.renderer) state.renderer.refresh();
}

/* ------------------------------ tabs -------------------------------- */

function switchTab(tab) {
    state.activeTab = tab;
    document.querySelectorAll(".tab").forEach((b) => b.classList.toggle("active", b.dataset.tab === tab));
    $("pane-network").classList.toggle("active", tab === "network");
    $("pane-tables").classList.toggle("active", tab === "tables");
    $("pane-metrics").classList.toggle("active", tab === "metrics");
    $("pane-paths").classList.toggle("active", tab === "paths");

    // Right panel content depends on the tab: Selection on Network, metric runners on Metrics, nothing on Tables.
    $("selection-block").style.display = tab === "network" ? "" : "none";
    document.querySelectorAll(".right-metric").forEach((s) => { s.style.display = tab === "metrics" ? "" : "none"; });

    if (tab === "network" && state.renderer) setTimeout(() => state.renderer.refresh(), 0);
    if (tab === "tables") renderTable(state.activeTableSubtab);
    if (tab === "metrics") renderMetricsDashboard();
    if (tab === "paths" && state.selectedNode && !$("path-source").value) $("path-source").value = state.selectedNode;
}

function switchSubtab(sub) {
    state.activeSubtab = sub;
    document.querySelectorAll(".subtab").forEach((b) => b.classList.toggle("active", b.dataset.subtab === sub));
    $("subpane-global").classList.toggle("active", sub === "global");
    $("subpane-nodes").classList.toggle("active", sub === "nodes");
    $("subpane-edges").classList.toggle("active", sub === "edges");
    $("subpane-pairs").classList.toggle("active", sub === "pairs");
    $("subpane-comm").classList.toggle("active", sub === "comm");
    if (sub === "nodes") drawNodeChart();
    if (sub === "edges") drawEdgeChart();
    if (sub === "pairs") drawPairChart();
    if (sub === "comm") drawCommChart();
}

function switchTableSubtab(sub) {
    state.activeTableSubtab = sub;
    document.querySelectorAll(".tablesubtab").forEach((b) => b.classList.toggle("active", b.dataset.tablesubtab === sub));
    $("tablesubpane-nodes").classList.toggle("active", sub === "nodes");
    $("tablesubpane-edges").classList.toggle("active", sub === "edges");
    renderTable(sub);
}

// Refresh whichever data-driven views are currently visible after a computation.
function refreshAfterCompute() {
    if (state.activeTab === "tables") renderTable(state.activeTableSubtab);
    if (state.activeTab === "metrics") renderMetricsDashboard();
}

/* ---------------------------- table models -------------------------- */

function nodeColumns() {
    const cols = [{ key: "id", label: "id" }, { key: "degree", label: "degree" }];
    if (state.directed) {
        cols.push({ key: "indeg", label: "in-degree" });
        cols.push({ key: "outdeg", label: "out-degree" });
    }
    state.metricOrder.forEach((m) => cols.push({ key: "m:" + m, label: m }));
    Object.keys(state.communityData).forEach((a) => cols.push({ key: "c:" + a, label: "comm:" + a }));
    return cols;
}

function nodeRowValue(node, key) {
    const g = state.graph;
    if (key === "id") return Number(node);
    if (key === "degree") return g.degree(node);
    if (key === "indeg") return g.inDegree(node);
    if (key === "outdeg") return g.outDegree(node);
    if (key.startsWith("m:")) return state.metricData[key.slice(2)]?.[node];
    if (key.startsWith("c:")) return state.communityData[key.slice(2)]?.[node];
    return undefined;
}

function edgeColumns() {
    const cols = [{ key: "source", label: "source" }, { key: "target", label: "target" }];
    if (state.weighted) cols.push({ key: "weight", label: "weight" });
    state.pairOrder.forEach((m) => cols.push({ key: "p:" + m, label: m }));
    return cols;
}

function edgeRowValue(edge, key) {
    const g = state.graph;
    const s = g.source(edge), t = g.target(edge);
    if (key === "source") return Number(s);
    if (key === "target") return Number(t);
    if (key === "weight") return g.getEdgeAttribute(edge, "weight");
    if (key.startsWith("p:")) return state.pairData[key.slice(2)]?.[pairKey(s, t)];
    return undefined;
}

const TABLE_IDS = { nodes: "nodes-table", edges: "edges-table" };

function tableModel(key) {
    if (key === "nodes") return { cols: nodeColumns(), ids: state.graph ? state.graph.nodes() : [], val: nodeRowValue };
    return { cols: edgeColumns(), ids: state.graph ? state.graph.edges() : [], val: edgeRowValue };
}

function compareValues(a, b) {
    if (a === undefined || a === null) return 1;
    if (b === undefined || b === null) return -1;
    if (typeof a === "number" && typeof b === "number") return a - b;
    return String(a).localeCompare(String(b));
}

/* ----------------------------- filtering ---------------------------- */

const OPERATORS = [
    { value: "contains", label: "contains" },
    { value: "notcontains", label: "not contains" },
    { value: "eq", label: "=" },
    { value: "neq", label: "≠" },
    { value: "gt", label: ">" },
    { value: "gte", label: "≥" },
    { value: "lt", label: "<" },
    { value: "lte", label: "≤" },
];

// Tests one cell value against a single column filter.
function matchFilter(value, op, fv) {
    if (fv === "" || fv == null) return true; // inactive
    const s = value == null ? "" : String(value);
    const sl = s.toLowerCase(), fl = String(fv).toLowerCase();
    const numV = Number(value), numF = Number(fv);
    const bothNum = value !== null && value !== undefined && value !== "" && !Number.isNaN(numV) && !Number.isNaN(numF);
    switch (op) {
        case "contains": return sl.includes(fl);
        case "notcontains": return !sl.includes(fl);
        case "eq": return bothNum ? numV === numF : sl === fl;
        case "neq": return bothNum ? numV !== numF : sl !== fl;
        case "gt": return bothNum && numV > numF;
        case "gte": return bothNum && numV >= numF;
        case "lt": return bothNum && numV < numF;
        case "lte": return bothNum && numV <= numF;
        default: return true;
    }
}

// Active filters for a table, restricted to columns that currently exist.
function activeFilters(key, cols) {
    const filters = state.tables[key].filters;
    const colKeys = new Set(cols.map((c) => c.key));
    return Object.keys(filters)
        .filter((col) => colKeys.has(col) && filters[col].value !== "")
        .map((col) => ({ col, op: filters[col].op, value: filters[col].value }));
}

// True if a row passes every active filter (AND).
function rowPasses(valFn, id, filters) {
    return filters.every((f) => matchFilter(valFn(id, f.col), f.op, f.value));
}

// Full filtered + sorted id list for a table (no pagination); used for rendering and export.
function tableRows(key) {
    const m = tableModel(key);
    const t = state.tables[key];
    const filters = activeFilters(key, m.cols);
    let ids = Array.from(m.ids);
    if (filters.length) ids = ids.filter((id) => rowPasses(m.val, id, filters));

    // "Show only" selection modes (ego-only / community-only) also restrict the tables.
    const focus = selectionFocus();
    if (focus && focus.only) {
        const g = state.graph;
        ids = key === "nodes"
            ? ids.filter((id) => focus.nodes.has(id))
            : ids.filter((e) => focus.nodes.has(g.source(e)) && focus.nodes.has(g.target(e)));
    }

    ids.sort((a, b) => compareValues(m.val(a, t.sort.col), m.val(b, t.sort.col)) * t.sort.dir);
    return { model: m, ids };
}

function renderTable(key) {
    ensureHeader(key);
    renderBody(key);
}

// (Re)builds the header (sort row + per-column filter row) only when the column set changes,
// so typing in a filter input never recreates the input and never loses focus.
function ensureHeader(key) {
    const m = tableModel(key);
    const sig = m.cols.map((c) => c.key).join("|");
    if (state.tables[key].headerSig === sig) { updateSortIndicators(key, m.cols); return; }
    state.tables[key].headerSig = sig;
    buildHeader(key, m.cols);
}

function buildHeader(key, cols) {
    const table = $(TABLE_IDS[key]);
    const old = table.querySelector("thead");
    if (old) old.remove();
    const thead = document.createElement("thead");

    const sortRow = document.createElement("tr");
    cols.forEach((c) => {
        const th = document.createElement("th");
        th.className = "sort-th";
        th.dataset.col = c.key;
        th.dataset.label = c.label;
        th.addEventListener("click", () => {
            const s = state.tables[key].sort;
            if (s.col === c.key) s.dir = -s.dir;
            else { s.col = c.key; s.dir = 1; }
            state.tables[key].page = 0;
            renderTable(key);
        });
        sortRow.appendChild(th);
    });
    thead.appendChild(sortRow);

    const filterRow = document.createElement("tr");
    filterRow.className = "filter-row";
    cols.forEach((c) => {
        const th = document.createElement("th");
        const wrap = document.createElement("div");
        wrap.className = "colfilter";
        const sel = document.createElement("select");
        OPERATORS.forEach((o) => sel.appendChild(option(o.value, o.label)));
        const inp = document.createElement("input");
        inp.type = "text";
        inp.placeholder = "…";
        const existing = state.tables[key].filters[c.key];
        if (existing) { sel.value = existing.op; inp.value = existing.value; }
        const apply = () => setColFilter(key, c.key, sel.value, inp.value);
        sel.addEventListener("change", apply);
        inp.addEventListener("input", apply);
        wrap.append(sel, inp);
        th.appendChild(wrap);
        filterRow.appendChild(th);
    });
    thead.appendChild(filterRow);

    table.insertBefore(thead, table.firstChild);
    updateSortIndicators(key, cols);
}

function updateSortIndicators(key) {
    const s = state.tables[key].sort;
    $(TABLE_IDS[key]).querySelectorAll("thead .sort-th").forEach((th) => {
        const c = th.dataset.col;
        th.textContent = th.dataset.label + (s.col === c ? (s.dir === 1 ? " ▲" : " ▼") : "");
    });
}

// Renders just the body + pager (leaves the header — and any focused filter input — untouched).
function renderBody(key) {
    const { model, ids } = tableRows(key);
    const t = state.tables[key];
    const total = ids.length;
    const pages = Math.max(1, Math.ceil(total / t.pageSize));
    t.page = Math.min(Math.max(0, t.page), pages - 1);
    const start = t.page * t.pageSize;
    const pageIds = ids.slice(start, start + t.pageSize);

    const table = $(TABLE_IDS[key]);
    const oldBody = table.querySelector("tbody");
    if (oldBody) oldBody.remove();
    const tbody = document.createElement("tbody");
    for (const id of pageIds) {
        const tr = document.createElement("tr");
        for (const c of model.cols) {
            const td = document.createElement("td");
            const v = model.val(id, c.key);
            td.textContent = v === undefined || v === null ? "" : fmt(v);
            tr.appendChild(td);
        }
        tbody.appendChild(tr);
    }
    table.appendChild(tbody);
    updatePager(key, total, pages, start, pageIds.length);
}

function setColFilter(key, col, op, value) {
    const filters = state.tables[key].filters;
    if (value === "") delete filters[col];
    else filters[col] = { op, value };
    state.tables[key].page = 0;
    renderBody(key);     // body only → keeps the filter input focused
    applyReducers();   // hide filtered-out nodes/edges in the visualization
}

function clearFilters(key) {
    state.tables[key].filters = {};
    state.tables[key].headerSig = null; // force header rebuild so inputs reset
    state.tables[key].page = 0;
    renderTable(key);
    applyReducers();
}

/* ----------------------------- pagination -------------------------- */

function updatePager(key, total, pages, start, shown) {
    const t = state.tables[key];
    const container = $("pager-" + key);
    container.innerHTML = "";

    const sizeSel = document.createElement("select");
    [50, 100, 500, 1000].forEach((n) => sizeSel.appendChild(option(String(n), n + " / page")));
    sizeSel.value = String(t.pageSize);
    sizeSel.addEventListener("change", () => { t.pageSize = parseInt(sizeSel.value, 10); t.page = 0; renderTable(key); });

    const prev = document.createElement("button");
    prev.textContent = "‹ Prev";
    prev.disabled = t.page <= 0;
    prev.addEventListener("click", () => { t.page--; renderTable(key); });

    const next = document.createElement("button");
    next.textContent = "Next ›";
    next.disabled = t.page >= pages - 1;
    next.addEventListener("click", () => { t.page++; renderTable(key); });

    const info = document.createElement("span");
    info.className = "pageinfo";
    info.textContent = (total ? start + 1 : 0) + "–" + (start + shown) + " of " + total + "  (page " + (t.page + 1) + "/" + pages + ")";

    container.append(sizeSel, prev, info, next);
}

/* ------------------- visualization reducers (filter + selection) ------------------- */

// Combines two view effects into sigma's reducers:
//  - table filters: filtered-out nodes/edges are hidden;
//  - node selection: depending on the mode, the focus set is highlighted (others dimmed) or isolated (others hidden).
// Neither affects metric computation (always done over the whole graph on the server).
function applyReducers() {
    if (!state.renderer || !state.graph) return;
    const g = state.graph;

    const nodeFilters = activeFilters("nodes", nodeColumns());
    const edgeFilters = activeFilters("edges", edgeColumns());
    const hasEdgeFilter = edgeFilters.length > 0;

    let filterNodes = null;
    if (nodeFilters.length) {
        filterNodes = new Set();
        g.forEachNode((node) => { if (rowPasses(nodeRowValue, node, nodeFilters)) filterNodes.add(node); });
    }

    const path = state.pathFocus;           // highlighting shortest paths takes priority over selection
    const focus = path ? null : selectionFocus();
    const dim = focus && !focus.only;       // highlight modes: dim non-focus
    const isolate = focus && focus.only;    // "show only" modes: hide non-focus
    const dimColor = cssVar("--border", "#3a3c41");

    const nodeActive = filterNodes || focus || path;
    state.renderer.setSetting("nodeReducer", nodeActive ? (node, data) => {
        if (filterNodes && !filterNodes.has(node)) return { ...data, hidden: true };
        if (path) {
            if (path.nodes.has(node)) return { ...data, zIndex: 2 };
            return path.only
                ? { ...data, hidden: true }
                : { ...data, color: dimColor, label: "", size: Math.max(1, (data.size || 3) * 0.6), zIndex: 0 };
        }
        if (isolate && !focus.nodes.has(node)) return { ...data, hidden: true };
        if (focus && node === focus.selected) return { ...data, highlighted: true, zIndex: 2 };
        if (dim && !focus.nodes.has(node)) return { ...data, color: dimColor, label: "", size: Math.max(1, (data.size || 3) * 0.6), zIndex: 0 };
        if (focus && focus.nodes.has(node)) return { ...data, zIndex: 1 };
        return data;
    } : null);

    const edgeActive = filterNodes || hasEdgeFilter || focus || path;
    state.renderer.setSetting("edgeReducer", edgeActive ? (edge, data) => {
        const s = g.source(edge), t = g.target(edge);
        if (filterNodes && (!filterNodes.has(s) || !filterNodes.has(t))) return { ...data, hidden: true };
        if (hasEdgeFilter && !rowPasses(edgeRowValue, edge, edgeFilters)) return { ...data, hidden: true };
        if (path) {
            if (path.edges.has(pairKey(s, t))) return { ...data, zIndex: 2 };
            return path.only ? { ...data, hidden: true } : { ...data, color: dimColor, zIndex: 0 };
        }
        if (isolate && (!focus.nodes.has(s) || !focus.nodes.has(t))) return { ...data, hidden: true };
        if (dim && (!focus.nodes.has(s) || !focus.nodes.has(t))) return { ...data, color: dimColor };
        return data;
    } : null);

    state.renderer.refresh();
}

/* ------------------------- metrics dashboard ------------------------ */

function renderMetricsDashboard() {
    renderGlobalTable();
    renderAverages("node-averages-table", state.metricOrder, state.metricData);
    renderAverages("edge-averages-table", state.pairOrder, state.pairData);
    renderPairAverages();
    renderCommAverages();
    syncChartSelectors();
    if (state.activeSubtab === "nodes") drawNodeChart();
    if (state.activeSubtab === "edges") drawEdgeChart();
    if (state.activeSubtab === "pairs") drawPairChart();
    if (state.activeSubtab === "comm") drawCommChart();
}

// Per-community averages come from each stored metric's value map.
function renderCommAverages() {
    const table = $("comm-averages-table");
    table.innerHTML = "";
    for (const label of state.commMetricOrder) {
        const tr = document.createElement("tr");
        tr.innerHTML = `<td>${label}</td><td>${fmt(average(state.commMetricData[label].values))}</td>`;
        table.appendChild(tr);
    }
}

// Node-pair averages come from the streamed aggregate (one summary object per metric), not a per-pair map.
function renderPairAverages() {
    const table = $("pair-averages-table");
    table.innerHTML = "";
    for (const label of state.nodePairOrder) {
        const agg = state.nodePairAgg[label];
        const note = agg.estimated ? " (estimated)" : "";
        const tr = document.createElement("tr");
        tr.innerHTML = `<td>${label}${note}</td><td>${fmt(agg.average)}</td>`;
        table.appendChild(tr);
    }
}

function renderGlobalTable() {
    const table = $("global-metrics-table");
    table.innerHTML = "";
    const keys = Object.keys(state.graphMetrics);
    for (const k of keys) {
        const tr = document.createElement("tr");
        tr.innerHTML = `<td>${k}</td><td>${fmt(state.graphMetrics[k])}</td>`;
        table.appendChild(tr);
    }
}

function average(values) {
    const arr = Object.values(values);
    if (!arr.length) return 0;
    return arr.reduce((a, b) => a + b, 0) / arr.length;
}

function renderAverages(tableId, order, data) {
    const table = $(tableId);
    table.innerHTML = "";
    for (const label of order) {
        const tr = document.createElement("tr");
        tr.innerHTML = `<td>${label}</td><td>${fmt(average(data[label]))}</td>`;
        table.appendChild(tr);
    }
}

function syncChartSelectors() {
    // Node chart.
    setSelectOptions("node-chart-metric", state.metricOrder, state.metricOrder);
    setSelectOptions("node-chart-sort", ["id", ...state.metricOrder], ["id", ...state.metricOrder]);
    // Edge chart.
    setSelectOptions("edge-chart-metric", state.pairOrder, state.pairOrder);
    setSelectOptions("edge-chart-sort", ["pair", ...state.pairOrder], ["source→target", ...state.pairOrder]);
    // Node-pair distribution chart (histogram per metric).
    setSelectOptions("pair-chart-metric", state.nodePairOrder, state.nodePairOrder);
    // Per-community chart.
    setSelectOptions("comm-chart-metric", state.commMetricOrder, state.commMetricOrder);
    setSelectOptions("comm-chart-sort", ["community", ...state.commMetricOrder], ["community", ...state.commMetricOrder]);
}

function setSelectOptions(id, values, labels) {
    const select = $(id);
    const current = select.value;
    select.innerHTML = "";
    values.forEach((v, i) => select.appendChild(option(v, labels[i])));
    if (values.includes(current)) select.value = current;
}

/* ------------------------------- charts ----------------------------- */

function drawNodeChart() {
    const metric = $("node-chart-metric").value;
    if (!state.graph || !metric || !state.metricData[metric]) { clearChart("node-chart"); return; }
    const sortBy = $("node-chart-sort").value;
    const values = state.metricData[metric];
    let nodes = state.graph.nodes().slice();
    if (sortBy === "id") nodes.sort((a, b) => Number(a) - Number(b));
    else {
        const sv = state.metricData[sortBy] || values;
        nodes.sort((a, b) => (sv[b] ?? 0) - (sv[a] ?? 0));
    }
    const items = nodes.map((n) => ({ label: n, value: values[n] ?? 0 }));
    drawBarChart("node-chart", "node-chart-tip", items, metric, "nodes", metric);
}

function drawEdgeChart() {
    const metric = $("edge-chart-metric").value;
    if (!state.graph || !metric || !state.pairData[metric]) { clearChart("edge-chart"); return; }
    const sortBy = $("edge-chart-sort").value;
    const data = state.pairData[metric];
    const g = state.graph;
    let edges = g.edges().slice().filter((e) => data[pairKey(g.source(e), g.target(e))] !== undefined);
    const valOf = (e) => data[pairKey(g.source(e), g.target(e))] ?? 0;
    if (sortBy === "pair") {
        edges.sort((a, b) => Number(g.source(a)) - Number(g.source(b)) || Number(g.target(a)) - Number(g.target(b)));
    } else {
        const sd = state.pairData[sortBy] || data;
        const sv = (e) => sd[pairKey(g.source(e), g.target(e))] ?? 0;
        edges.sort((a, b) => sv(b) - sv(a));
    }
    const items = edges.map((e) => ({ label: g.source(e) + "→" + g.target(e), value: valOf(e) }));
    drawBarChart("edge-chart", "edge-chart-tip", items, metric, "edges (source→target)", metric);
}

// The node-pair distribution is a histogram of the streamed aggregate (exact or sampled), so it scales to any N.
function drawPairChart() {
    const metric = $("pair-chart-metric").value;
    const agg = state.nodePairAgg[metric];
    if (!metric || !agg || !agg.histogram) { clearChart("pair-chart"); $("pair-chart-note").textContent = ""; return; }
    const items = agg.histogram.map((b) => ({ label: fmt(b.x0) + "–" + fmt(b.x1), value: b.count }));
    drawBarChart("pair-chart", "pair-chart-tip", items, metric + " — value distribution", "value", "count");
    const parts = [];
    parts.push(agg.estimated
        ? "Estimated from " + agg.evaluated.toLocaleString() + " sampled pairs of " + agg.totalPairs.toLocaleString() + " total."
        : "Exact over " + agg.totalPairs.toLocaleString() + " pairs.");
    if (agg.infinite) parts.push(agg.infinite.toLocaleString() + " pairs had no finite value (excluded).");
    parts.push("min " + fmt(agg.min) + ", max " + fmt(agg.max) + ".");
    $("pair-chart-note").textContent = parts.join(" ");
}

function drawCommChart() {
    const metric = $("comm-chart-metric").value;
    const entry = state.commMetricData[metric];
    if (!metric || !entry) { clearChart("comm-chart"); return; }
    const values = entry.values;
    const sortBy = $("comm-chart-sort").value;
    let comms = Object.keys(values);
    if (sortBy === "community") {
        comms.sort((a, b) => Number(a) - Number(b));
    } else {
        const sv = state.commMetricData[sortBy]?.values || values;
        comms.sort((a, b) => (sv[b] ?? 0) - (sv[a] ?? 0));
    }
    const items = comms.map((c) => ({ label: "community " + c, value: values[c] ?? 0 }));
    drawBarChart("comm-chart", "comm-chart-tip", items, metric, "communities", metric);
}

function clearChart(canvasId) {
    const canvas = $(canvasId);
    const ctx = canvas.getContext("2d");
    ctx.clearRect(0, 0, canvas.width, canvas.height);
}

function drawBarChart(canvasId, tipId, items, title, xLabel, yLabel) {
    const canvas = $(canvasId);
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    const W = Math.max(1, Math.floor(rect.width)), H = Math.max(1, Math.floor(rect.height));
    canvas.width = W * dpr;
    canvas.height = H * dpr;
    const ctx = canvas.getContext("2d");
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);

    // Extra padding for the axis labels when present.
    const padL = 56 + (yLabel ? 14 : 0);
    const padR = 14;
    const padT = 18;
    const padB = 28 + (xLabel ? 16 : 0);
    const plotW = W - padL - padR, plotH = H - padT - padB;

    const values = items.map((d) => d.value);
    let max = Math.max(0, ...values), min = Math.min(0, ...values);
    if (max === min) max = min + 1;

    const colText = cssVar("--text", "#e6e6e6");
    const colMuted = cssVar("--muted", "#9aa0a6");
    const colBorder = cssVar("--border", "#333");
    const colBar = cssVar("--accent", "#4f9dff");

    // Axes.
    ctx.strokeStyle = colMuted; ctx.fillStyle = colMuted; ctx.font = "11px sans-serif";
    const yOf = (v) => padT + plotH - ((v - min) / (max - min)) * plotH;
    ctx.beginPath(); ctx.moveTo(padL, padT); ctx.lineTo(padL, padT + plotH); ctx.lineTo(padL + plotW, padT + plotH); ctx.stroke();
    for (let g = 0; g <= 4; g++) {
        const val = min + ((max - min) * g) / 4;
        const y = yOf(val);
        ctx.fillStyle = colMuted; ctx.fillText(fmt(val), 4, y + 3);
        ctx.strokeStyle = colBorder; ctx.beginPath(); ctx.moveTo(padL, y); ctx.lineTo(padL + plotW, y); ctx.stroke();
    }
    ctx.fillStyle = colText; ctx.fillText(title + "  (n=" + items.length + ")", padL, 12);

    // Axis labels.
    ctx.fillStyle = colText;
    if (xLabel) {
        ctx.textAlign = "center";
        ctx.fillText(xLabel, padL + plotW / 2, H - 4);
        ctx.textAlign = "start";
    }
    if (yLabel) {
        ctx.save();
        ctx.translate(12, padT + plotH / 2);
        ctx.rotate(-Math.PI / 2);
        ctx.textAlign = "center";
        ctx.fillText(yLabel, 0, 0);
        ctx.restore();
    }

    // Bars.
    const n = items.length || 1;
    const bw = plotW / n;
    const zeroY = yOf(0);
    const bars = [];
    for (let i = 0; i < items.length; i++) {
        const v = items[i].value;
        const x = padL + i * bw;
        const y = yOf(v);
        const h = Math.abs(zeroY - y);
        ctx.fillStyle = colBar;
        ctx.fillRect(x, Math.min(y, zeroY), Math.max(1, bw - (bw > 3 ? 1 : 0)), Math.max(1, h));
        bars.push({ x, w: bw, label: items[i].label, value: v });
    }

    // Hover tooltip.
    const tip = $(tipId);
    canvas.onmousemove = (ev) => {
        const r = canvas.getBoundingClientRect();
        const mx = ev.clientX - r.left;
        const idx = Math.floor((mx - padL) / bw);
        if (idx >= 0 && idx < bars.length) {
            const b = bars[idx];
            tip.hidden = false;
            tip.style.left = (b.x + bw / 2) + "px";
            tip.style.top = (ev.clientY - r.top) + "px";
            tip.textContent = b.label + ": " + fmt(b.value);
        } else {
            tip.hidden = true;
        }
    };
    canvas.onmouseleave = () => { tip.hidden = true; };
}

/* ------------------------------ exports ----------------------------- */

function csvCell(v) {
    if (v === undefined || v === null) return "";
    const s = String(v);
    return /[",\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
}

function buildCsv(cols, rowIds, valueFn) {
    const lines = [cols.map((c) => csvCell(c.label)).join(",")];
    for (const id of rowIds) {
        lines.push(cols.map((c) => csvCell(valueFn(id, c.key))).join(","));
    }
    return lines.join("\n");
}

function download(filename, content, mime) {
    const blob = content instanceof Blob ? content : new Blob([content], { type: mime || "text/plain" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
}

// Exports a table honouring its current filter and sort (all rows, not just the visible page).
function exportTableCsv(key, filename) {
    if (!state.graph) return;
    const { model, ids } = tableRows(key);
    download(filename, buildCsv(model.cols, ids, model.val), "text/csv");
}

function exportNodesCsv() { exportTableCsv("nodes", "nodes.csv"); }
function exportEdgesCsv() { exportTableCsv("edges", "edges.csv"); }

function exportPairAveragesCsv() {
    const rows = state.nodePairOrder.map((label) => {
        const a = state.nodePairAgg[label];
        return [label, a.average, a.min, a.max, a.estimated ? "estimated" : "exact", a.totalPairs, a.evaluated];
    });
    const header = ["metric", "average", "min", "max", "mode", "totalPairs", "evaluated"].map(csvCell).join(",");
    const lines = [header];
    for (const r of rows) lines.push(r.map(csvCell).join(","));
    download("node-pair-averages.csv", lines.join("\n"), "text/csv");
}

// Composite sigma's layered canvases into a single PNG of the current view.
// Nodes/edges are drawn with WebGL, whose buffer reads blank outside a render pass, so we capture inside
// sigma's own "afterRender" event (synchronously, before the buffer is cleared) to match the on-screen plot.
function exportPng() {
    if (!state.renderer) { setStatus("Load a network first.", "error"); return; }
    const r = state.renderer;
    const capture = () => {
        if (typeof r.off === "function") r.off("afterRender", capture);
        else if (typeof r.removeListener === "function") r.removeListener("afterRender", capture);
        try { compositePng(); }
        catch (e) { console.error(e); setStatus("PNG export failed: " + e.message, "error"); }
    };
    r.on("afterRender", capture);
    r.refresh(); // schedules a render; afterRender fires at the end of it
}

function compositePng() {
    const canvases = $("sigma-container").querySelectorAll("canvas");
    if (!canvases.length) return;
    const w = canvases[0].width, h = canvases[0].height;
    const out = document.createElement("canvas");
    out.width = w; out.height = h;
    const ctx = out.getContext("2d");
    ctx.fillStyle = cssVar("--canvas-bg", "#18191c");
    ctx.fillRect(0, 0, w, h);
    canvases.forEach((c) => ctx.drawImage(c, 0, 0, w, h));
    out.toBlob((blob) => download("network.png", blob, "image/png"), "image/png");
}

function cssVar(name, fallback) {
    const v = getComputedStyle(document.body).getPropertyValue(name).trim();
    return v || fallback;
}

function exportGlobalCsv() {
    const rows = Object.keys(state.graphMetrics).map((k) => [k, state.graphMetrics[k]]);
    download("global-metrics.csv", kvCsv("metric", "value", rows), "text/csv");
}

function exportAveragesCsv(name, order, data) {
    const rows = order.map((label) => [label, average(data[label])]);
    download(name, kvCsv("metric", "average", rows), "text/csv");
}

function exportCommAveragesCsv() {
    const rows = state.commMetricOrder.map((label) => [label, average(state.commMetricData[label].values)]);
    download("community-metric-averages.csv", kvCsv("metric", "average", rows), "text/csv");
}

function kvCsv(keyHeader, valueHeader, rows) {
    const lines = [csvCell(keyHeader) + "," + csvCell(valueHeader)];
    for (const [k, v] of rows) lines.push(csvCell(k) + "," + csvCell(v));
    return lines.join("\n");
}

// Serializes the current graph (with computed node/edge attributes) to GEXF for Gephi/other tools.
function exportGexf() {
    if (!state.graph) { setStatus("Load a network first.", "error"); return; }
    const g = state.graph;
    const esc = (s) => String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");

    const nodeAttrs = [...state.metricOrder, ...Object.keys(state.communityData).map((a) => "community:" + a)];
    const edgeAttrs = [...state.pairOrder];
    const nodeAttrId = (i) => "n" + i;
    const edgeAttrId = (i) => "e" + i;

    const lines = [];
    lines.push('<?xml version="1.0" encoding="UTF-8"?>');
    lines.push('<gexf xmlns="http://www.gexf.net/1.2draft" xmlns:viz="http://www.gexf.net/1.2draft/viz" version="1.2">');
    lines.push('<graph defaultedgetype="' + (state.directed ? "directed" : "undirected") + '">');

    if (nodeAttrs.length) {
        lines.push('<attributes class="node">');
        nodeAttrs.forEach((a, i) => lines.push('<attribute id="' + nodeAttrId(i) + '" title="' + esc(a) + '" type="double"/>'));
        lines.push('</attributes>');
    }
    if (edgeAttrs.length) {
        lines.push('<attributes class="edge">');
        edgeAttrs.forEach((a, i) => lines.push('<attribute id="' + edgeAttrId(i) + '" title="' + esc(a) + '" type="double"/>'));
        lines.push('</attributes>');
    }

    lines.push('<nodes>');
    g.forEachNode((node, attr) => {
        lines.push('<node id="' + esc(node) + '" label="' + esc(attr.label ?? node) + '">');
        if (attr.x !== undefined && attr.y !== undefined)
            lines.push('<viz:position x="' + attr.x + '" y="' + attr.y + '" z="0"/>');
        if (attr.size !== undefined) lines.push('<viz:size value="' + attr.size + '"/>');
        const rgb = colorToRgb(attr.color);
        if (rgb) lines.push('<viz:color r="' + rgb.r + '" g="' + rgb.g + '" b="' + rgb.b + '"/>');
        if (nodeAttrs.length) {
            lines.push('<attvalues>');
            nodeAttrs.forEach((a, i) => {
                const v = a.startsWith("community:") ? state.communityData[a.slice(10)]?.[node] : state.metricData[a]?.[node];
                if (v !== undefined && v !== null) lines.push('<attvalue for="' + nodeAttrId(i) + '" value="' + v + '"/>');
            });
            lines.push('</attvalues>');
        }
        lines.push('</node>');
    });
    lines.push('</nodes>');

    lines.push('<edges>');
    let ei = 0;
    g.forEachEdge((edge, attr, s, t) => {
        const weight = attr.weight !== undefined ? ' weight="' + attr.weight + '"' : "";
        lines.push('<edge id="' + (ei++) + '" source="' + esc(s) + '" target="' + esc(t) + '"' + weight + '>');
        if (edgeAttrs.length) {
            lines.push('<attvalues>');
            edgeAttrs.forEach((a, i) => {
                const v = state.pairData[a]?.[pairKey(s, t)];
                if (v !== undefined && v !== null) lines.push('<attvalue for="' + edgeAttrId(i) + '" value="' + v + '"/>');
            });
            lines.push('</attvalues>');
        }
        lines.push('</edge>');
    });
    lines.push('</edges>');

    lines.push('</graph>');
    lines.push('</gexf>');
    download("network.gexf", lines.join("\n"), "application/gexf+xml");
}

// Parses a CSS colour (#hex, rgb(), hsl()) into {r,g,b}, or null if it can't.
function colorToRgb(color) {
    if (!color) return null;
    if (color[0] === "#") {
        const h = color.slice(1);
        const n = h.length === 3 ? h.split("").map((c) => c + c).join("") : h;
        return { r: parseInt(n.slice(0, 2), 16), g: parseInt(n.slice(2, 4), 16), b: parseInt(n.slice(4, 6), 16) };
    }
    let m = color.match(/rgb\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/i);
    if (m) return { r: +m[1], g: +m[2], b: +m[3] };
    m = color.match(/hsl\(\s*([\d.]+)\s*,\s*([\d.]+)%\s*,\s*([\d.]+)%/i);
    if (m) return hslToRgb(+m[1], +m[2] / 100, +m[3] / 100);
    return null;
}

function hslToRgb(h, s, l) {
    const c = (1 - Math.abs(2 * l - 1)) * s;
    const x = c * (1 - Math.abs(((h / 60) % 2) - 1));
    const m = l - c / 2;
    let r = 0, g = 0, b = 0;
    if (h < 60) [r, g, b] = [c, x, 0];
    else if (h < 120) [r, g, b] = [x, c, 0];
    else if (h < 180) [r, g, b] = [0, c, x];
    else if (h < 240) [r, g, b] = [0, x, c];
    else if (h < 300) [r, g, b] = [x, 0, c];
    else [r, g, b] = [c, 0, x];
    return { r: Math.round((r + m) * 255), g: Math.round((g + m) * 255), b: Math.round((b + m) * 255) };
}

/* -------------------------- add from table -------------------------- */

async function addNodeFromTable() {
    if (!requireGraph()) return;
    const raw = $("add-node-id").value.trim();
    let id = raw;
    if (!id) { id = String(state.graph.order); while (state.graph.hasNode(id)) id = String(Number(id) + 1); }
    if (state.graph.hasNode(id)) { setStatus("Node " + id + " already exists.", "error"); return; }
    try {
        const res = await api("/api/graph/" + state.graphId + "/node", jsonBody({ node: id }));
        state.graph.addNode(id, { label: id, x: (Math.random() - 0.5) * 50, y: (Math.random() - 0.5) * 50, size: 4, color: "#4f9dff" });
        $("add-node-id").value = "";
        onGraphEdited(res.stats);
        setStatus("Added node " + id + ".");
    } catch (e) { setStatus(e.message, "error"); }
}

async function addEdgeFromTable() {
    if (!requireGraph()) return;
    const source = $("add-edge-source").value.trim();
    const target = $("add-edge-target").value.trim();
    const weight = parseFloat($("add-edge-weight").value) || 1.0;
    if (!source || !target) { setStatus("Enter both source and target.", "error"); return; }
    editAddEdge(source, target, weight);
}

/* ------------------------------- theme ------------------------------ */

function applyTheme(theme) {
    const light = theme === "light";
    document.body.classList.toggle("light", light);
    $("theme-toggle").textContent = light ? "☀️" : "🌙";
    try { localStorage.setItem("relison-theme", theme); } catch (e) { /* ignore */ }
    // Charts are drawn imperatively, so re-render the visible one with the new palette.
    if (state.activeTab === "metrics") {
        if (state.activeSubtab === "nodes") drawNodeChart();
        if (state.activeSubtab === "edges") drawEdgeChart();
        if (state.activeSubtab === "pairs") drawPairChart();
        if (state.activeSubtab === "comm") drawCommChart();
    }
}

function toggleTheme() {
    applyTheme(document.body.classList.contains("light") ? "dark" : "light");
}

(function initTheme() {
    let saved = "dark";
    try { saved = localStorage.getItem("relison-theme") || "dark"; } catch (e) { /* ignore */ }
    applyTheme(saved);
})();

/* ------------------------------- wiring ----------------------------- */

$("btn-load").addEventListener("click", loadGraph);
$("btn-layout").addEventListener("click", startLayout);
$("btn-reset-layout").addEventListener("click", resetLayout);
$("size-by").addEventListener("change", applyAppearance);
$("color-by").addEventListener("change", applyAppearance);
$("node-color-low").addEventListener("input", applyAppearance);
$("node-color-high").addEventListener("input", applyAppearance);
$("edge-size-by").addEventListener("change", applyAppearance);
$("edge-color-mode").addEventListener("change", (e) => {
    $("edge-color-single-field").hidden = e.target.value !== "single";
    applyAppearance();
});
$("edge-color-single").addEventListener("input", applyAppearance);
$("btn-vertex").addEventListener("click", runVertexMetric);
$("btn-graph").addEventListener("click", runGraphMetric);
$("btn-pair").addEventListener("click", runPairMetric);
$("btn-community").addEventListener("click", detectCommunity);
$("btn-community-metrics").addEventListener("click", runCommunityMetrics);
$("btn-indiv-comm").addEventListener("click", runIndividualCommMetric);

$("vertex-metric").addEventListener("change", (e) => renderParams("vertex-params", "vertex", e.target.value));
$("graph-metric").addEventListener("change", (e) => renderParams("graph-params", "graph", e.target.value));
$("pair-metric").addEventListener("change", (e) => renderParams("pair-params", "pair", e.target.value));
$("community-algo").addEventListener("change", (e) => renderParams("community-params", "community", e.target.value));
$("indiv-comm-metric").addEventListener("change", (e) => renderParams("indiv-comm-params", "communityIndividual", e.target.value));
$("edit-mode").addEventListener("change", (e) => setEditMode(e.target.checked));
$("theme-toggle").addEventListener("click", toggleTheme);

$("select-node-input").addEventListener("change", selectFromInput);
initNodeCombos();
$("select-mode").addEventListener("change", (e) => setSelectionMode(e.target.value));
$("select-partition").addEventListener("change", (e) => setSelectionPartition(e.target.value));
$("btn-clear-selection").addEventListener("click", clearSelection);
$("select-mode").value = state.selection.mode;

$("btn-find-paths").addEventListener("click", findPaths);
$("btn-highlight-all-paths").addEventListener("click", () => highlightPaths(state.lastPaths));
$("btn-clear-path-hl").addEventListener("click", clearPathHighlight);
$("path-display").addEventListener("change", () => {
    if (state.pathFocus) { state.pathFocus.only = $("path-display").value === "only"; applyReducers(); }
});

document.querySelectorAll(".tab").forEach((b) => b.addEventListener("click", () => switchTab(b.dataset.tab)));
document.querySelectorAll(".subtab").forEach((b) => b.addEventListener("click", () => switchSubtab(b.dataset.subtab)));
document.querySelectorAll(".tablesubtab").forEach((b) => b.addEventListener("click", () => switchTableSubtab(b.dataset.tablesubtab)));

$("btn-export-png").addEventListener("click", exportPng);
$("btn-export-gexf").addEventListener("click", exportGexf);
$("btn-export-nodes").addEventListener("click", exportNodesCsv);
$("btn-export-edges").addEventListener("click", exportEdgesCsv);
$("btn-export-global").addEventListener("click", exportGlobalCsv);
$("btn-export-node-averages").addEventListener("click", () => exportAveragesCsv("node-averages.csv", state.metricOrder, state.metricData));
$("btn-export-edge-averages").addEventListener("click", () => exportAveragesCsv("edge-averages.csv", state.pairOrder, state.pairData));
$("btn-export-pair-averages").addEventListener("click", exportPairAveragesCsv);
$("btn-export-comm-averages").addEventListener("click", () => exportCommAveragesCsv());
$("btn-add-node").addEventListener("click", addNodeFromTable);
$("btn-add-edge").addEventListener("click", addEdgeFromTable);

$("btn-clear-nodes-filters").addEventListener("click", () => clearFilters("nodes"));
$("btn-clear-edges-filters").addEventListener("click", () => clearFilters("edges"));

["node-chart-metric", "node-chart-sort"].forEach((id) => $(id).addEventListener("change", drawNodeChart));
["edge-chart-metric", "edge-chart-sort"].forEach((id) => $(id).addEventListener("change", drawEdgeChart));
$("pair-chart-metric").addEventListener("change", drawPairChart);
["comm-chart-metric", "comm-chart-sort"].forEach((id) => $(id).addEventListener("change", drawCommChart));

window.addEventListener("resize", () => {
    if (state.activeTab !== "metrics") return;
    if (state.activeSubtab === "nodes") drawNodeChart();
    if (state.activeSubtab === "edges") drawEdgeChart();
    if (state.activeSubtab === "pairs") drawPairChart();
    if (state.activeSubtab === "comm") drawCommChart();
});

document.addEventListener("keydown", (e) => {
    if (!$("edit-mode").checked) return;
    if ((e.key === "Delete" || e.key === "Backspace") && state.selectedNode != null) {
        const node = state.selectedNode;
        clearSelection();
        editDeleteNode(node);
    }
});

switchTab(state.activeTab); // set initial right-panel visibility for the default (Network) tab
loadCatalog();
