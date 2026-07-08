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
    dragNodes: false,     // when on, dragging a node repositions it instead of panning the canvas
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

    multigraph: false,     // the loaded network allows parallel edges (recommendation tab disabled)
    // Node-colour legend + per-category colour overrides for the main display panel.
    colorLegend: null,     // { type:"none"|"numeric"|"categorical", title, ... } describing the current node colouring
    catColors: {},         // colour overrides for categorical colourings: colorKey -> { categoryValue -> hex }
    nodeBorder: { on: false, color: "#1b1c1f", width: 1.5 },   // optional node borders drawn on the overlay
    // Recommendation / link-prediction overlay (only one model is shown at a time).
    rec: {
        active: null,      // key of the model currently overlaid, or null
        models: {},        // key -> { label, mode, cutoff, edges:[{source,target,score}] }
        diff: true,        // differentiate recommended edges (dashed) in the visualization
        show: true,        // show the overlay (can be toggled off without removing the model)
        color: "#28c76f",  // colour of the recommended edges
        tableEdges: [],    // edges shown in the recommendation table (kept for study even after the overlay is cleared)
    },
    // Metrics recomputed over the augmented graph, keyed by family -> metric label -> rec key -> values.
    recMetricData: { vertex: {}, graph: {}, pair: {}, nodePair: {}, comm: {} },

    // Information-diffusion simulation.
    diffusion: {
        loaded: false,     // catalog loaded
        subview: "pieces", // active center subtab: "pieces" | "graph" | "metrics"
        pieces: [],        // uploaded/edited information pieces: [{ id, creator, timestamp, features:[{param,value,weight}] }]
        featureParams: [], // ordered feature-parameter names, one table column each
        pieceFilters: {},  // per-column filters for the pieces table: colKey -> { op, value }
        piecesHeaderSig: null,   // signature of the current column set (to rebuild the header only when it changes)
        applyFiltersToSim: false,// when true, only pieces passing the table filters are used in the simulation
        piecesPage: 0,     // current page of the (paginated) pieces table
        piecesPageSize: 200,
        piecesSaved: true, // whether the current pieces are persisted on the session
        result: null,      // latest run: { numIterations, iterations:[{propagating,newlyInformed}], metrics:[{id,label,values}] }
        runs: [],          // accumulated runs for overlaid metric plots: [{ label, numIterations, metrics:[{id,label,values}] }]
        iteration: 0,
        graph: null,       // graphology copy rendered in the diffusion canvas
        renderer: null,    // its sigma renderer
        selectedNode: null,
        playing: null,     // setInterval handle when playing
    },

    activeTab: "import",
    activeSubtab: "global",
    activeTableSubtab: "nodes",
    // Label rendering options (mirrors the Labels controls in the left panel).
    labelOpts: {
        nodeShow: true, nodeSize: 12, nodeProp: false, nodeColor: "#e6e6e6", nodeFont: "sans-serif",
        edgeShow: false, edgeSize: 9, edgeProp: false, edgeColor: "#e6e6e6", edgeFont: "sans-serif",
    },
    tables: {
        nodes: { sort: { col: "id", dir: 1 }, filters: {}, page: 0, pageSize: 100, headerSig: null },
        edges: { sort: { col: "source", dir: 1 }, filters: {}, page: 0, pageSize: 100, headerSig: null },
        rec: { sort: { col: "score", dir: -1 }, filters: {}, page: 0, pageSize: 100, headerSig: null },
    },
    catalog: null,
    defs: { vertex: {}, graph: {}, pair: {}, community: {}, communityIndividual: {}, recommendation: {} },
    attrSchema: { node: [], edge: [] },   // user-defined attributes: [{name,type,numeric}]
};

/* --------------------------- attribute helpers ---------------------- */

function nodeAttrDefs() { return (state.attrSchema && state.attrSchema.node) || []; }
function edgeAttrDefs() { return (state.attrSchema && state.attrSchema.edge) || []; }
function nodeAttrIsNumeric(name) { const d = nodeAttrDefs().find((x) => x.name === name); return !!(d && d.numeric); }

// Value of a node/edge attribute (attributes are nested under the graphology "attrs" attribute).
function nodeAttrVal(node, name) {
    const a = state.graph && state.graph.hasNode(node) ? state.graph.getNodeAttribute(node, "attrs") : null;
    return a ? a[name] : undefined;
}
function edgeAttrVal(edge, name) {
    const a = state.graph ? state.graph.getEdgeAttribute(edge, "attrs") : null;
    return a ? a[name] : undefined;
}
// Map node -> value for a node attribute (only nodes that have a value).
function nodeAttrValues(name) {
    const v = {};
    if (!state.graph) return v;
    state.graph.forEachNode((n) => { const x = nodeAttrVal(n, name); if (x !== undefined && x !== null) v[n] = x; });
    return v;
}

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

// Reads a numeric input by id, falling back to a default when blank or invalid.
function numInput(id, def) {
    const v = parseFloat($(id).value);
    return Number.isFinite(v) ? v : def;
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
        state.multigraph = $("opt-multigraph").checked;
        state.attrSchema = data.schema || { node: [], edge: [] };
        resetResults();
        resetRecommendation();
        resetDiffusion();
        // Pieces reference nodes of the old graph; start fresh on the new (empty) session.
        if (piecesSaveTimer) { clearTimeout(piecesSaveTimer); piecesSaveTimer = null; }
        state.diffusion.pieces = [];
        state.diffusion.featureParams = [];
        state.diffusion.pieceFilters = {};
        state.diffusion.piecesHeaderSig = null;
        state.diffusion.piecesPage = 0;
        state.diffusion.piecesSaved = true;
        renderPiecesTable();
        updateRecTabAvailability();
        resetTableViews();
        clearSelection();
        $("btn-global-comm").disabled = true;
        $("btn-indiv-comm").disabled = true;
        $("community-result").textContent = "";
        $("indiv-comm-partition").innerHTML = "";
        $("global-comm-partition").innerHTML = "";
        state.pathFocus = null;
        state.lastPaths = [];
        $("paths-table").innerHTML = "";
        $("path-source").value = "";
        $("path-target").value = "";
        $("path-summary").textContent = "Pick a source and a target, then find their shortest paths.";
        switchTab("network");          // ensure the (sized) network pane is visible before rendering
        renderGraph(data.graph);
        rebuildAppearanceOptions();    // surface any imported attributes in the appearance menus
        applyAppearance();
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
    state.recMetricData = { vertex: {}, graph: {}, pair: {}, nodePair: {}, comm: {} };
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
        renderLabels: state.labelOpts.nodeShow,
        renderEdgeLabels: state.labelOpts.edgeShow,
        labelRenderer: drawNodeLabel,
        edgeLabelRenderer: drawEdgeLabel,
        labelDensity: 0.5,
        labelRenderedSizeThreshold: 8,
        // The container can be momentarily hidden (zero-size) if a graph is loaded from another tab;
        // tolerate it and refresh once the Network tab becomes visible.
        allowInvalidContainer: true,
    });
    syncLabelOpts();

    // Repaint the recommended-edge overlay after sigma renders. Debounced (and cleared during the gesture) so
    // panning/zooming a large overlay stays smooth and the dashed lines never lag behind the moving graph.
    state.renderer.on("afterRender", scheduleRecOverlay);

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

    // Node dragging (only when the "Drag nodes" control is on): reposition a node instead of panning the canvas.
    let draggedNode = null;
    state.renderer.on("downNode", ({ node }) => {
        if (!state.dragNodes) return;
        draggedNode = node;
        graph.setNodeAttribute(node, "highlighted", true);
        // Freeze the auto-fit bounding box so moving a node out of bounds doesn't make the camera jump.
        if (!state.renderer.getCustomBBox()) state.renderer.setCustomBBox(state.renderer.getBBox());
    });
    const captor = state.renderer.getMouseCaptor();
    captor.on("mousemovebody", (e) => {
        if (!draggedNode) return;
        const pos = state.renderer.viewportToGraph(e);
        graph.setNodeAttribute(draggedNode, "x", pos.x);
        graph.setNodeAttribute(draggedNode, "y", pos.y);
        // Stop sigma (and the browser) from also panning/selecting while dragging.
        e.preventSigmaDefault();
        e.original.preventDefault();
        e.original.stopPropagation();
    });
    const endDrag = () => {
        if (draggedNode) graph.removeNodeAttribute(draggedNode, "highlighted");
        draggedNode = null;
    };
    captor.on("mouseup", endDrag);
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
    // Numeric node attributes can also drive node size.
    for (const d of nodeAttrDefs()) if (d.numeric) sizeSelect.appendChild(option("attr:" + d.name, "attr: " + d.name));
    restoreSelect(sizeSelect, sizeCurrent);

    const colorSelect = $("color-by");
    const colorCurrent = colorSelect.value;
    colorSelect.innerHTML = '<option value="">— none —</option>';
    for (const name of metricNames) colorSelect.appendChild(option("metric:" + name, name));
    for (const algo of communityNames) colorSelect.appendChild(option("community:" + algo, "community: " + algo));
    // Node attributes: numeric ones use the colour ramp, the rest are coloured categorically.
    for (const d of nodeAttrDefs()) colorSelect.appendChild(option((d.numeric ? "attr:" : "attrcat:") + d.name, "attr: " + d.name));
    restoreSelect(colorSelect, colorCurrent);

    // Edge thickness: uniform + each computed edge (link) metric + numeric edge attributes.
    const edgeSizeSelect = $("edge-size-by");
    const edgeCurrent = edgeSizeSelect.value;
    edgeSizeSelect.innerHTML = '<option value="">— uniform —</option>';
    for (const name of state.pairOrder) edgeSizeSelect.appendChild(option(name, name));
    for (const d of edgeAttrDefs()) if (d.numeric) edgeSizeSelect.appendChild(option("eattr:" + d.name, "attr: " + d.name));
    restoreSelect(edgeSizeSelect, edgeCurrent);

    rebuildLayoutGroupOptions();   // keep the circle-packing "Group by" options in sync with communities/attributes
}

// Restores a select's value if the option still exists, otherwise falls back to the first (default) option.
function restoreSelect(select, value) {
    const exists = Array.from(select.options).some((o) => o.value === value);
    select.value = exists ? value : "";
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
    let sizeValues;
    if (!sizeBy) sizeValues = degreeValues();
    else if (sizeBy.startsWith("attr:")) sizeValues = numericMap(nodeAttrValues(sizeBy.slice(5)));
    else sizeValues = state.metricData[sizeBy] || {};
    let min = Infinity, max = -Infinity;
    for (const n of graph.nodes()) {
        const val = sizeValues[n] ?? 0;
        if (val < min) min = val;
        if (val > max) max = val;
    }
    const span = (max - min) || 1;
    const minSize = numInput("node-size-min", 2), maxSize = numInput("node-size-max", 14);
    const sizeRange = Math.max(0, maxSize - minSize);
    graph.forEachNode((n) => graph.setNodeAttribute(n, "size", minSize + sizeRange * (((sizeValues[n] ?? 0) - min) / span)));

    const colorSel = $("color-by").value;
    if (!colorSel) { graph.forEachNode((n) => graph.setNodeAttribute(n, "color", "#4f9dff")); state.colorLegend = { type: "none" }; }
    else if (colorSel.startsWith("community:")) colorByCommunity(colorSel.slice("community:".length));
    else if (colorSel.startsWith("metric:")) colorByMetric(colorSel.slice("metric:".length));
    else if (colorSel.startsWith("attrcat:")) colorByNodeCategorical(colorSel.slice("attrcat:".length));
    else if (colorSel.startsWith("attr:")) colorByNodeAttr(colorSel.slice("attr:".length));

    applyEdgeAppearance();
    applyLabels();
    buildNodeLegend();

    if (state.renderer) { state.renderer.refresh(); drawRecOverlay(); }
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
    state.colorLegend = { type: "numeric", title: metricName, low, high, min, max };
}

function colorByCommunity(algo) {
    applyCategorical("community:" + algo, state.communityData[algo] || {}, "community: " + algo);
}

// Colours nodes by a numeric attribute, using the configured low→high colour ramp.
function colorByNodeAttr(name) {
    const graph = state.graph;
    const values = nodeAttrValues(name);
    const low = $("node-color-low").value, high = $("node-color-high").value;
    let min = Infinity, max = -Infinity;
    for (const n of graph.nodes()) {
        const v = Number(values[n]);
        if (!Number.isNaN(v)) { if (v < min) min = v; if (v > max) max = v; }
    }
    if (min === Infinity) { min = 0; max = 1; }
    const span = (max - min) || 1;
    graph.forEachNode((n) => {
        const v = Number(values[n]);
        graph.setNodeAttribute(n, "color", Number.isNaN(v) ? "#888888" : lerpHex(low, high, (v - min) / span));
    });
    state.colorLegend = { type: "numeric", title: "attr: " + name, low, high, min, max };
}

// Colours nodes by a categorical/textual attribute: one distinct colour per value.
function colorByNodeCategorical(name) {
    applyCategorical("attrcat:" + name, nodeAttrValues(name), "attr: " + name);
}

// Shared categorical colouring: assigns one colour per distinct value (honouring per-category user overrides in
// state.catColors[colorKey]) and records the value→colour mapping in state.colorLegend for the legend.
function applyCategorical(colorKey, values, title) {
    const graph = state.graph;
    const order = [];
    const seen = new Set();
    graph.forEachNode((n) => {
        const key = values[n];
        if (key === undefined || key === null) return;
        const s = String(key);
        if (!seen.has(s)) { seen.add(s); order.push(s); }
    });
    const overrides = state.catColors[colorKey] || {};
    const colorOf = {};
    order.forEach((cat, i) => { colorOf[cat] = overrides[cat] || categoricalHex(i); });

    let hasMissing = false;
    graph.forEachNode((n) => {
        const key = values[n];
        if (key === undefined || key === null) { hasMissing = true; graph.setNodeAttribute(n, "color", "#888888"); }
        else graph.setNodeAttribute(n, "color", colorOf[String(key)]);
    });

    state.colorLegend = {
        type: "categorical", colorKey, title,
        entries: order.map((cat) => ({ value: cat, color: colorOf[cat] })),
        hasMissing,
    };
}

// Edge thickness (by a computed link metric) and colour (uniform default / single / average of endpoints).
function applyEdgeAppearance() {
    const graph = state.graph;
    const sizeBy = $("edge-size-by").value;
    // Thickness can come from a computed link metric or a numeric edge attribute.
    const edgeAttrName = sizeBy.startsWith("eattr:") ? sizeBy.slice("eattr:".length) : null;
    const sizeData = (sizeBy && !edgeAttrName) ? (state.pairData[sizeBy] || {}) : null;
    const sizing = sizeBy !== "";
    const edgeVal = (edge, s, t) => {
        if (edgeAttrName) { const v = Number(edgeAttrVal(edge, edgeAttrName)); return Number.isNaN(v) ? undefined : v; }
        if (sizeData) return sizeData[pairKey(s, t)];
        return undefined;
    };

    let min = Infinity, max = -Infinity;
    if (sizing) {
        graph.forEachEdge((e, a, s, t) => {
            const v = edgeVal(e, s, t);
            if (v === undefined) return;
            if (v < min) min = v;
            if (v > max) max = v;
        });
    }
    const span = (max - min) || 1;

    const mode = $("edge-color-mode").value;
    const single = $("edge-color-single").value;
    const defColor = "#888888";
    const minW = numInput("edge-size-min", 0.5), maxW = numInput("edge-size-max", 6);
    const wRange = Math.max(0, maxW - minW);

    graph.forEachEdge((edge, attr, s, t) => {
        if (sizing) {
            const v = edgeVal(edge, s, t);
            graph.setEdgeAttribute(edge, "size", v === undefined ? minW : minW + wRange * ((v - min) / span));
        } else {
            graph.setEdgeAttribute(edge, "size", minW > 0 ? minW : 1);
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

// Same distinct-colour sequence as categorical(), but as a #rrggbb string so it can seed <input type="color">.
function categoricalHex(i) {
    const { r, g, b } = hslToRgb((i * 137.508) % 360, 0.65, 0.55);
    const h = (v) => v.toString(16).padStart(2, "0");
    return "#" + h(r) + h(g) + h(b);
}

// Coerces any colour string (hex / rgb() / named) to #rrggbb for <input type="color">.
function toHexColor(c) {
    if (typeof c === "string" && /^#[0-9a-fA-F]{6}$/.test(c)) return c;
    const rgb = colorToRgb(c);
    if (!rgb) return "#888888";
    const h = (v) => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, "0");
    return "#" + h(rgb.r) + h(rgb.g) + h(rgb.b);
}

// Renders the node-colour legend overlaid on the display panel from state.colorLegend. Categorical legends expose a
// colour picker per category (writing to state.catColors and re-applying); numeric legends show a gradient + range.
function buildNodeLegend() {
    const el = $("node-legend");
    if (!el) return;
    const lg = state.colorLegend;
    if (!lg || lg.type === "none") { el.hidden = true; el.innerHTML = ""; return; }
    el.innerHTML = "";

    const title = document.createElement("div");
    title.className = "legend-title";
    title.textContent = lg.title || "Node colour";
    el.appendChild(title);

    if (lg.type === "numeric") {
        const bar = document.createElement("div");
        bar.className = "legend-gradient";
        bar.style.background = "linear-gradient(to right, " + lg.low + ", " + lg.high + ")";
        el.appendChild(bar);
        const scale = document.createElement("div");
        scale.className = "legend-scale";
        const lo = document.createElement("span"); lo.textContent = fmt(lg.min);
        const hi = document.createElement("span"); hi.textContent = fmt(lg.max);
        scale.appendChild(lo); scale.appendChild(hi);
        el.appendChild(scale);
    } else {
        const list = document.createElement("div");
        list.className = "legend-list";
        for (const entry of lg.entries) {
            list.appendChild(legendCategoryRow(lg.colorKey, entry.value, entry.color, true));
        }
        if (lg.hasMissing) list.appendChild(legendCategoryRow(null, "(none)", "#888888", false));
        el.appendChild(list);
    }
    el.hidden = false;
}

// One legend row: an editable colour swatch (for real categories) + the category label.
function legendCategoryRow(colorKey, value, color, editable) {
    const row = document.createElement("div");
    row.className = "legend-row";
    if (editable) {
        const sw = document.createElement("input");
        sw.type = "color";
        sw.className = "legend-swatch-input";
        sw.value = toHexColor(color);
        sw.title = "Colour for " + value;
        sw.addEventListener("input", () => {
            if (!state.catColors[colorKey]) state.catColors[colorKey] = {};
            state.catColors[colorKey][value] = sw.value;
            applyAppearance();
        });
        row.appendChild(sw);
    } else {
        const sw = document.createElement("span");
        sw.className = "legend-swatch";
        sw.style.background = color;
        row.appendChild(sw);
    }
    const lab = document.createElement("span");
    lab.className = "legend-label";
    lab.textContent = value;
    row.appendChild(lab);
    return row;
}

/* ------------------------------ labels ------------------------------ */

// Sets the graphology label of every node/edge from a "label" attribute when present (node ids are the fallback;
// edges have no label unless a "label" attribute provides one). Sigma renders these via the custom drawers below.
function applyLabels() {
    const g = state.graph;
    if (!g) return;
    const nodeHasLabel = nodeAttrDefs().some((d) => d.name === "label");
    g.forEachNode((n) => {
        const lbl = nodeHasLabel ? nodeAttrVal(n, "label") : null;
        g.setNodeAttribute(n, "label", (lbl !== undefined && lbl !== null && lbl !== "") ? String(lbl) : n);
    });
    const edgeHasLabel = edgeAttrDefs().some((d) => d.name === "label");
    g.forEachEdge((e) => {
        const lbl = edgeHasLabel ? edgeAttrVal(e, "label") : null;
        g.setEdgeAttribute(e, "label", (lbl !== undefined && lbl !== null && lbl !== "") ? String(lbl) : "");
    });
}

// Reads the label controls into state and pushes the show/hide flags to the renderer.
function syncLabelOpts() {
    const o = state.labelOpts;
    o.nodeShow = $("node-label-show").checked;
    o.nodeSize = numInput("node-label-size", 12);
    o.nodeProp = $("node-label-prop").checked;
    o.nodeColor = $("node-label-color").value;
    o.nodeFont = $("node-label-font").value;
    o.edgeShow = $("edge-label-show").checked;
    o.edgeSize = numInput("edge-label-size", 9);
    o.edgeProp = $("edge-label-prop").checked;
    o.edgeColor = $("edge-label-color").value;
    o.edgeFont = $("edge-label-font").value;
    if (state.renderer) {
        state.renderer.setSetting("renderLabels", o.nodeShow);
        state.renderer.setSetting("renderEdgeLabels", o.edgeShow);
        // Mirror to the built-in settings too, as a fallback for the fixed-size case.
        state.renderer.setSetting("labelSize", o.nodeSize);
        state.renderer.setSetting("edgeLabelSize", o.edgeSize);
        state.renderer.setSetting("labelColor", { color: o.nodeColor });
        state.renderer.setSetting("edgeLabelColor", { color: o.edgeColor });
        state.renderer.setSetting("labelFont", o.nodeFont);
        state.renderer.setSetting("edgeLabelFont", o.edgeFont);
        state.renderer.refresh();
    }
}

function labelTextColor() {
    return document.body.classList.contains("light") ? "#1c1d20" : "#e6e6e6";
}

// Custom node label drawer: font size is either fixed or proportional to the node's rendered size.
// The colour is a parameter so the diffusion canvas can reuse the same geometry with its own (fixed) label colour.
function drawNodeLabelWith(context, data, color) {
    if (!data.label) return;
    const o = state.labelOpts;
    const fontSize = o.nodeProp ? Math.max(6, data.size * (o.nodeSize / 8)) : o.nodeSize;
    context.fillStyle = color;
    context.font = `${fontSize}px ${o.nodeFont || "sans-serif"}`;
    context.fillText(data.label, data.x + data.size + 3, data.y + fontSize / 3);
}
function drawNodeLabel(context, data) { drawNodeLabelWith(context, data, state.labelOpts.nodeColor || labelTextColor()); }
// Diffusion canvas: same label geometry as the main plot, but its own (theme-default) colour — label colour is
// deliberately not mirrored from the main plot.
function drawDiffNodeLabel(context, data) { drawNodeLabelWith(context, data, labelTextColor()); }

// Custom edge label drawer: drawn at the edge midpoint, size fixed or proportional to the edge thickness.
function drawEdgeLabelWith(context, data, sourceData, targetData, color) {
    if (!data.label) return;
    const o = state.labelOpts;
    const fontSize = o.edgeProp ? Math.max(5, (data.size || 1) * (o.edgeSize / 2)) : o.edgeSize;
    const x = (sourceData.x + targetData.x) / 2;
    const y = (sourceData.y + targetData.y) / 2;
    context.fillStyle = color;
    context.font = `${fontSize}px ${o.edgeFont || "sans-serif"}`;
    context.textAlign = "center";
    context.fillText(data.label, x, y);
    context.textAlign = "left";
}
function drawEdgeLabel(context, data, sourceData, targetData) { drawEdgeLabelWith(context, data, sourceData, targetData, state.labelOpts.edgeColor || labelTextColor()); }
function drawDiffEdgeLabel(context, data, sourceData, targetData) { drawEdgeLabelWith(context, data, sourceData, targetData, labelTextColor()); }

/* --------------------------- canvas tools --------------------------- */

function zoomIn() { if (state.renderer) state.renderer.getCamera().animatedZoom(); }
function zoomOut() { if (state.renderer) state.renderer.getCamera().animatedUnzoom(); }
function zoomFit() { if (state.renderer) state.renderer.getCamera().animatedReset(); }

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
    for (const d of nodeAttrDefs()) {
        const v = nodeAttrVal(node, d.name);
        if (v !== undefined && v !== null) rows.push(["attr · " + d.name, fmt(v)]);
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

    // The dropdown is positioned with fixed coordinates (from the input's viewport rect) so it is never clipped when
    // the input lives inside a scrollable container (e.g. the information-pieces table).
    function position() {
        const r = input.getBoundingClientRect();
        list.style.position = "fixed";
        list.style.top = (r.bottom + 2) + "px";
        list.style.left = r.left + "px";
        list.style.minWidth = r.width + "px";
    }
    function onAncestorScroll(e) { if (e.target === list || list.contains(e.target)) return; close(); }

    function close() {
        list.hidden = true;
        active = -1;
        window.removeEventListener("scroll", onAncestorScroll, true);
        window.removeEventListener("resize", close);
    }

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
        position();
        window.addEventListener("scroll", onAncestorScroll, true);
        window.addEventListener("resize", close);
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
    ["select-node-input", "path-source", "path-target", "add-edge-source", "add-edge-target", "diff-node-input"]
        .forEach((id) => attachNodeCombo($(id)));
}

/* ------------------------------ layout ------------------------------ */

// Layouts that run continuously (animated) vs. one-shot layouts that are applied once.
const ITERATIVE_LAYOUTS = new Set(["forceatlas2", "force"]);

function currentLayoutType() {
    const sel = $("layout-type");
    return sel ? sel.value : "forceatlas2";
}

// Keeps the layout button label in sync with the selected algorithm / running state.
function updateLayoutButton() {
    if (state.fa2Running) { $("btn-layout").textContent = "Stop layout"; return; }
    $("btn-layout").textContent = ITERATIVE_LAYOUTS.has(currentLayoutType()) ? "Start layout" : "Apply layout";
}

// Button handler: toggles animated layouts, or applies a static layout once.
function onLayoutButton() {
    if (!state.graph) { setStatus("Load a network first.", "error"); return; }
    const type = currentLayoutType();
    if (ITERATIVE_LAYOUTS.has(type)) {
        if (state.fa2Running) { stopLayout(); return; }
        startIterativeLayout(type);
    } else {
        applyStaticLayout(type);
    }
}

// Starts an animated force layout (ForceAtlas2 or graphology's force layout), falling back to the built-in stepper.
function startIterativeLayout(type) {
    let stepFn;
    if (type === "force" && lib.layoutForce && typeof lib.layoutForce.assign === "function") {
        stepFn = () => lib.layoutForce.assign(state.graph, { maxIterations: 1 });
    } else if (FA2 && typeof FA2.assign === "function") {
        let settings;
        try { settings = FA2.inferSettings ? FA2.inferSettings(state.graph) : {}; }
        catch (e) { console.warn("inferSettings failed, using defaults", e); settings = {}; }
        const lp = layoutParams();
        settings.scalingRatio = (settings.scalingRatio || 1) * lp.scaling;
        settings.gravity = lp.gravity;
        settings.slowDown = 1 / Math.max(0.1, lp.speed);
        stepFn = () => FA2.assign(state.graph, { iterations: 1, settings });
    } else {
        stepFn = builtinForceStep;
    }

    state.fa2Running = true;
    state.layoutTemp = 50;
    updateLayoutButton();

    const step = () => {
        try {
            stepFn();
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

// Applies a one-shot geometric layout (circular / circle packing / random). These are computed natively (they are
// trivial and dependency-free), so they work regardless of what the graphology UMD bundle exposes. Circle packing can
// group nodes by a community partition or a node attribute.
function applyStaticLayout(type) {
    stopLayout();
    const g = state.graph;
    try {
        if (type === "circular") nativeCircular(g);
        else if (type === "random") nativeRandom(g);
        else if (type === "circlepack") nativeCirclepack(g, $("circlepack-group") ? $("circlepack-group").value : "");
        else { setStatus("Unknown layout: " + type, "error"); return; }
    } catch (e) {
        setStatus("Layout failed: " + e.message, "error");
        return;
    }
    if (state.renderer) state.renderer.refresh();
    drawRecOverlay();
    setStatus(type + " layout applied.");
}

// Places nodes evenly on a circle whose radius grows with the node count.
function nativeCircular(g) {
    const nodes = g.nodes(), n = nodes.length;
    const R = Math.max(50, n * 8);
    nodes.forEach((nd, i) => {
        const a = (2 * Math.PI * i) / Math.max(1, n);
        g.setNodeAttribute(nd, "x", Math.cos(a) * R);
        g.setNodeAttribute(nd, "y", Math.sin(a) * R);
    });
}

// Scatters nodes uniformly in a square sized to the node count.
function nativeRandom(g) {
    const nodes = g.nodes();
    const S = Math.max(100, Math.sqrt(nodes.length) * 60);
    nodes.forEach((nd) => {
        g.setNodeAttribute(nd, "x", (Math.random() - 0.5) * S);
        g.setNodeAttribute(nd, "y", (Math.random() - 0.5) * S);
    });
}

// Lays out a set of nodes around (cx, cy) in a sunflower/phyllotaxis spiral (a compact, even disc).
function placeCluster(nodes, cx, cy, g) {
    const golden = Math.PI * (3 - Math.sqrt(5)), spacing = 12;
    nodes.forEach((nd, i) => {
        const r = spacing * Math.sqrt(i + 0.5), a = i * golden;
        g.setNodeAttribute(nd, "x", cx + r * Math.cos(a));
        g.setNodeAttribute(nd, "y", cy + r * Math.sin(a));
    });
}

// Packs nodes into a disc; when a grouping is chosen, each group becomes its own disc arranged on a ring.
function nativeCirclepack(g, groupBy) {
    const nodes = g.nodes();
    if (!groupBy) { placeCluster(nodes, 0, 0, g); return; }
    const groups = new Map();
    nodes.forEach((nd) => {
        const k = String(nodeGroupKey(groupBy, nd) ?? "?");
        if (!groups.has(k)) groups.set(k, []);
        groups.get(k).push(nd);
    });
    const keys = [...groups.keys()], m = keys.length;
    const ringR = Math.max(150, 40 * m, 14 * Math.sqrt(nodes.length));
    keys.forEach((k, gi) => {
        const a = (2 * Math.PI * gi) / Math.max(1, m);
        placeCluster(groups.get(k), Math.cos(a) * ringR, Math.sin(a) * ringR, g);
    });
}

// Spreads overlapping nodes apart: nudges pairs closer than (size-based) min distance until they no longer overlap.
function nativeNoverlap(g) {
    const nodes = g.nodes(), n = nodes.length;
    if (!n) return;
    const xs = [], ys = [], rs = [];
    let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
    nodes.forEach((nd, i) => {
        const x = g.getNodeAttribute(nd, "x") || 0, y = g.getNodeAttribute(nd, "y") || 0;
        xs[i] = x; ys[i] = y; rs[i] = g.getNodeAttribute(nd, "size") || 3;
        if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y;
    });
    // Map node "size" units into position units so collisions are meaningful at the current layout scale.
    const w = Math.max(1, maxX - minX), h = Math.max(1, maxY - minY);
    const avgSize = rs.reduce((a, b) => a + b, 0) / n || 1;
    const cell = Math.sqrt((w * h) / n);
    const scale = (cell * 0.6) / avgSize;
    const margin = cell * 0.1;
    const iterations = n > 1500 ? 20 : 60;
    for (let it = 0; it < iterations; it++) {
        let moved = false;
        for (let i = 0; i < n; i++) {
            for (let j = i + 1; j < n; j++) {
                let dx = xs[j] - xs[i], dy = ys[j] - ys[i], d = Math.hypot(dx, dy);
                const need = (rs[i] + rs[j]) * scale + margin;
                if (d < need) {
                    if (d < 1e-6) { dx = Math.random() - 0.5; dy = Math.random() - 0.5; d = Math.hypot(dx, dy) || 1; }
                    const push = (need - d) / 2, ux = dx / d, uy = dy / d;
                    xs[i] -= ux * push; ys[i] -= uy * push; xs[j] += ux * push; ys[j] += uy * push;
                    moved = true;
                }
            }
        }
        if (!moved) break;
    }
    nodes.forEach((nd, i) => { g.setNodeAttribute(nd, "x", xs[i]); g.setNodeAttribute(nd, "y", ys[i]); });
}

// Resolves the grouping key of a node for circle packing (a community id or a node-attribute value).
function nodeGroupKey(spec, node) {
    if (spec.startsWith("community:")) { const a = state.communityData[spec.slice("community:".length)]; return a ? a[node] : undefined; }
    if (spec.startsWith("attr:")) return nodeAttrVal(node, spec.slice("attr:".length));
    return undefined;
}

// Populates the circle-packing "Group by" selector with the detected partitions and node attributes.
function rebuildLayoutGroupOptions() {
    const sel = $("circlepack-group");
    if (!sel) return;
    const current = sel.value;
    sel.innerHTML = '<option value="">— none —</option>';
    Object.keys(state.communityData).forEach((a) => sel.appendChild(option("community:" + a, "community: " + a)));
    nodeAttrDefs().forEach((d) => sel.appendChild(option("attr:" + d.name, "attr: " + d.name)));
    restoreSelect(sel, current);
}

// Shows only the controls relevant to the selected layout: the FA2 tuning parameters and the circle-packing grouping.
function updateLayoutTypeUI() {
    const type = currentLayoutType();
    toggleHidden("layout-params", type !== "forceatlas2");
    toggleHidden("layout-params-hint", type !== "forceatlas2");
    toggleHidden("circlepack-group-field", type !== "circlepack");
}

// Spreads overlapping nodes apart. Uses graphology-layout-noverlap when the bundle exposes it, otherwise a native pass.
function removeOverlaps() {
    if (!state.graph) { setStatus("Load a network first.", "error"); return; }
    const NOV = lib.layoutNoverlap || window.graphologyLayoutNoverlap;
    let ok = false;
    try {
        if (NOV && typeof NOV.assign === "function") {
            NOV.assign(state.graph, { maxIterations: 60, settings: { margin: 5, ratio: 1 } });
            ok = true;
        }
    } catch (e) {
        console.warn("Bundled noverlap failed, using native fallback.", e);
    }
    if (!ok) nativeNoverlap(state.graph);
    if (state.renderer) state.renderer.refresh();
    drawRecOverlay();
    setStatus("Removed node overlaps.");
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
    updateLayoutButton();
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
            communityGlobal: indexById(cat.communityGlobal),
        };
        fillSelect("vertex-metric", cat.vertex);
        fillSelect("graph-metric", cat.graph);
        fillSelect("pair-metric", cat.pair);
        fillCommunitySelect("community-algo", cat.community);
        fillSelect("indiv-comm-metric", cat.communityIndividual);
        fillSelect("global-comm-metric", cat.communityGlobal);
        renderParams("vertex-params", "vertex", $("vertex-metric").value);
        renderParams("graph-params", "graph", $("graph-metric").value);
        renderParams("pair-params", "pair", $("pair-metric").value);
        renderParams("community-params", "community", $("community-algo").value);
        renderParams("indiv-comm-params", "communityIndividual", $("indiv-comm-metric").value);
        renderParams("global-comm-params", "communityGlobal", $("global-comm-metric").value);
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
        } else if (p.type === "string") {
            input = document.createElement("input");
            input.type = "text";
            input.value = p.default == null ? "" : p.default;
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
        if (state.rec.active && $("vertex-rec").checked) {
            try {
                const r2 = await api("/api/metrics/vertex", jsonBody(
                    { graphId: state.graphId, metric, params: collectParams("vertex-params"), withRecommendation: true }));
                storeRecMetric("vertex", res.label, numericMap(r2.values));
            } catch (e) { /* recommendation metric is optional */ }
        }
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
        if (state.rec.active && $("graph-rec").checked) {
            try {
                const r2 = await api("/api/metrics/graph", jsonBody(
                    { graphId: state.graphId, metric, params: collectParams("graph-params"), withRecommendation: true }));
                storeRecMetric("graph", res.label, r2.value);
            } catch (e) { /* recommendation metric is optional */ }
        }
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
        if (state.rec.active && $("pair-rec").checked) {
            try {
                const r2 = await api("/api/metrics/pair", jsonBody(
                    { graphId: state.graphId, metric, onlyLinks, params: collectParams("pair-params"), withRecommendation: true }));
                if (allPairs) {
                    storeRecMetric("nodePair", res.label, r2);
                } else {
                    const map = {};
                    r2.values.forEach((e) => {
                        map[pairKey(e.source, e.target)] = Number(e.value);
                        if (!state.directed) map[pairKey(e.target, e.source)] = Number(e.value);
                    });
                    storeRecMetric("pair", res.label, map);
                }
            } catch (e) { /* recommendation metric is optional */ }
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
        $("btn-global-comm").disabled = false;
        $("btn-indiv-comm").disabled = false;
        const algos = Object.keys(state.communityData);
        setSelectOptions("indiv-comm-partition", algos, algos);
        $("indiv-comm-partition").value = res.algorithm;
        setSelectOptions("global-comm-partition", algos, algos);
        $("global-comm-partition").value = res.algorithm;
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

async function runGlobalCommMetric() {
    if (!requireGraph()) return;
    const algorithm = $("global-comm-partition").value;
    if (!algorithm || !state.communityData[algorithm]) { setStatus("Detect a partition first.", "error"); return; }
    const metric = $("global-comm-metric").value;
    setStatus("Computing global " + metric + "…", "busy");
    $("btn-global-comm").disabled = true;
    try {
        const res = await api("/api/communities/global",
            jsonBody({ graphId: state.graphId, algorithm, metric, params: collectParams("global-comm-params") }));
        const label = res.label + " (" + algorithm + ")";
        state.graphMetrics[label] = res.value;
        if (state.rec.active && $("global-comm-rec").checked) {
            try {
                const r2 = await api("/api/communities/global", jsonBody(
                    { graphId: state.graphId, algorithm, metric, params: collectParams("global-comm-params"), withRecommendation: true }));
                storeRecMetric("graph", label, r2.value);
            } catch (e) { /* recommendation metric is optional */ }
        }
        refreshAfterCompute();
        setStatus("Computed " + label + " = " + fmt(res.value));
    } catch (e) {
        setStatus(e.message, "error");
    } finally {
        $("btn-global-comm").disabled = false;
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
        if (state.rec.active && $("comm-rec").checked) {
            try {
                const r2 = await api("/api/communities/individual", jsonBody(
                    { graphId: state.graphId, algorithm, metric, params: collectParams("indiv-comm-params"), withRecommendation: true }));
                storeRecMetric("comm", label, numericMap(r2.values));
            } catch (e) { /* recommendation metric is optional */ }
        }
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
    const withRecommendation = !!(state.rec.active && $("path-graph") && $("path-graph").value === "rec");
    setStatus("Finding shortest paths…", "busy");
    $("btn-find-paths").disabled = true;
    try {
        const res = await api("/api/paths", jsonBody({ graphId: state.graphId, source, target, withRecommendation }));
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
    if (!confirmClears("Adding a node")) return;
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
    if (!confirmClears("Adding an edge")) return;
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
        const res = await api("/api/graph/" + state.graphId + "/node/" + encodeURIComponent(node), { method: "DELETE" });
        if (state.graph.hasNode(node)) state.graph.dropNode(node);
        onGraphEdited(res.stats);
        setStatus("Removed node " + node + ".");
    } catch (e) { setStatus(e.message, "error"); }
}

// Removes a single edge (a specific parallel edge for multigraphs, identified by its graphology key).
async function editRemoveEdge(source, target, edgeId) {
    try {
        const body = { source, target };
        if (edgeId != null) body.edgeId = edgeId;
        const res = await api("/api/graph/" + state.graphId + "/edge",
            { method: "DELETE", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
        if (edgeId != null && state.graph.hasEdge(edgeId)) state.graph.dropEdge(edgeId);
        else if (state.graph.hasEdge(source, target)) state.graph.dropEdge(source, target);
        onGraphEdited(res.stats);
        setStatus("Removed edge " + source + " → " + target + ".");
    } catch (e) { setStatus(e.message, "error"); }
}

// Row remove handlers (with a confirmation, since removal is destructive).
function removeNodeFromTable(id) {
    if (!window.confirm('Remove node "' + id + '" and its edges?' + pendingClearsSuffix())) return;
    if (state.selectedNode === id) clearSelection();
    editDeleteNode(id);
}

function removeEdgeFromTable(edgeId) {
    const g = state.graph;
    const s = g.source(edgeId), t = g.target(edgeId);
    if (!window.confirm("Remove edge " + s + " → " + t + "?" + pendingClearsSuffix())) return;
    editRemoveEdge(s, t, edgeId);
}

// The computed state a graph edit (or reload) would discard, as a human-readable list.
function pendingClearsList() {
    const bits = [];
    if (state.metricOrder.length || state.pairOrder.length || state.commMetricOrder.length || Object.keys(state.graphMetrics).length) bits.push("computed metrics");
    if (Object.keys(state.communityData).length) bits.push("detected communities");
    if (Object.keys(state.rec.models).length || (state.rec.tableEdges && state.rec.tableEdges.length)) bits.push("recommendations");
    if (diffusionHasResults()) bits.push("diffusion results");
    return bits;
}

// A trailing sentence describing what an action will also clear, or "" if nothing.
function pendingClearsSuffix() {
    const bits = pendingClearsList();
    return bits.length ? " This will also clear the " + bits.join(", ") + "." : "";
}

// Confirms an action that will discard computed state; prompts only when there is something to clear.
function confirmClears(action) {
    const bits = pendingClearsList();
    if (!bits.length) return true;
    return window.confirm(action + " will clear the " + bits.join(", ") + ". Continue?");
}

function onGraphEdited(stats) {
    // Server cleared its caches, so all computed results (and any recommendation overlay) are stale.
    resetResults();
    resetRecommendation();
    resetDiffusion();
    rebuildAppearanceOptions();
    applyAppearance();
    if (stats) { $("ov-nodes").textContent = stats.nodes; $("ov-edges").textContent = stats.edges; }
    $("btn-global-comm").disabled = true;
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
    $("pane-import").classList.toggle("active", tab === "import");
    $("pane-network").classList.toggle("active", tab === "network");
    $("pane-tables").classList.toggle("active", tab === "tables");
    $("pane-metrics").classList.toggle("active", tab === "metrics");
    $("pane-paths").classList.toggle("active", tab === "paths");
    $("pane-recommend").classList.toggle("active", tab === "recommend");
    $("pane-diffusion").classList.toggle("active", tab === "diffusion");

    updatePanels(tab);

    if (tab === "network" && state.renderer) setTimeout(() => { state.renderer.refresh(); drawRecOverlay(); }, 0);
    if (tab === "tables") renderTable(state.activeTableSubtab);
    if (tab === "metrics") renderMetricsDashboard();
    if (tab === "paths" && state.selectedNode && !$("path-source").value) $("path-source").value = state.selectedNode;
    if (tab === "recommend") { loadRecCatalog(); renderTable("rec"); }
    if (tab === "diffusion") enterDiffusionTab();
}

// Switches between the Graph and Metrics subtabs in the Diffusion tab's center panel.
function switchDiffSubtab(sub) {
    state.diffusion.subview = sub;
    document.querySelectorAll(".diffsubtab").forEach((b) => b.classList.toggle("active", b.dataset.diffsubtab === sub));
    $("diff-view-graph").classList.toggle("active", sub === "graph");
    $("diff-view-metrics").classList.toggle("active", sub === "metrics");
    $("diff-view-pieces").classList.toggle("active", sub === "pieces");
    if (sub === "graph") {
        if (state.diffusion.renderer) setTimeout(() => { state.diffusion.renderer.refresh(); drawDiffOverlay(); }, 0);
    } else if (sub === "metrics") {
        renderDiffMetrics();
    } else if (sub === "pieces") {
        renderPiecesTable();
    }
}

// Shows/hides the side panels per tab and resizes the layout grid accordingly:
// import → no panels; network → left (layout/visualization) + right (selection); metrics → right (runners);
// tables/paths → no panels (full-width content).
function updatePanels(tab) {
    const showLeft = tab === "network";
    const showRight = tab === "network" || tab === "metrics";
    $("left").style.display = showLeft ? "" : "none";
    $("right").style.display = showRight ? "" : "none";
    const cols = [showLeft ? "270px" : null, "1fr", showRight ? "300px" : null].filter(Boolean).join(" ");
    $("layout").style.gridTemplateColumns = cols;

    $("selection-block").style.display = tab === "network" ? "" : "none";
    document.querySelectorAll(".right-metric").forEach((s) => { s.style.display = tab === "metrics" ? "" : "none"; });
}

// Disables the Recommendation tab for multigraphs (recommenders need a simple FastGraph); switches away if needed.
function updateRecTabAvailability() {
    const btn = $("tab-recommend");
    if (!btn) return;
    btn.disabled = state.multigraph;
    btn.classList.toggle("disabled", state.multigraph);
    btn.title = state.multigraph ? "Recommendation is not available for multigraphs." : "";
    if (state.multigraph && state.activeTab === "recommend") switchTab("network");
}

function switchSubtab(sub) {
    state.activeSubtab = sub;
    document.querySelectorAll(".subtab").forEach((b) => b.classList.toggle("active", b.dataset.subtab === sub));
    $("subpane-global").classList.toggle("active", sub === "global");
    $("subpane-nodes").classList.toggle("active", sub === "nodes");
    $("subpane-edges").classList.toggle("active", sub === "edges");
    $("subpane-pairs").classList.toggle("active", sub === "pairs");
    $("subpane-comm").classList.toggle("active", sub === "comm");
    if (sub === "nodes") { drawNodeChart(); drawNodeScatter(); }
    if (sub === "edges") { drawEdgeChart(); drawEdgeScatter(); }
    if (sub === "pairs") drawPairChart();
    if (sub === "comm") { drawCommChart(); drawCommScatter(); }
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
    // Degree / in-/out-degree are not computed by default; run the DEGREE vertex metric to add them as columns.
    const cols = [{ key: "id", label: "id" }];
    // Attribute columns come before the computed metric / community columns.
    nodeAttrDefs().forEach((d) => cols.push({ key: "a:" + d.name, label: d.name }));
    state.metricOrder.forEach((m) => cols.push({ key: "m:" + m, label: m }));
    Object.keys(state.communityData).forEach((a) => cols.push({ key: "c:" + a, label: "comm:" + a }));
    cols.push({ key: "_act", label: "" });   // trailing remove-row column
    return cols;
}

function nodeRowValue(node, key) {
    if (key === "id") return idValue(node);
    if (key.startsWith("m:")) return state.metricData[key.slice(2)]?.[node];
    if (key.startsWith("c:")) return state.communityData[key.slice(2)]?.[node];
    if (key.startsWith("a:")) return nodeAttrVal(node, key.slice(2));
    return undefined;
}

function edgeColumns() {
    const cols = [{ key: "source", label: "source" }, { key: "target", label: "target" }];
    // For multigraphs, expose each parallel edge's stable id so the rows can be told apart.
    if (state.graph && state.graph.multi) cols.push({ key: "edgeid", label: "edge id" });
    if (state.weighted) cols.push({ key: "weight", label: "weight" });
    // Attribute columns come before the computed pair-metric columns.
    edgeAttrDefs().forEach((d) => cols.push({ key: "ea:" + d.name, label: d.name }));
    state.pairOrder.forEach((m) => cols.push({ key: "p:" + m, label: m }));
    cols.push({ key: "_act", label: "" });   // trailing remove-row column
    return cols;
}

function edgeRowValue(edge, key) {
    const g = state.graph;
    const s = g.source(edge), t = g.target(edge);
    if (key === "source") return idValue(s);
    if (key === "target") return idValue(t);
    if (key === "edgeid") return idValue(edge);
    if (key === "weight") return g.getEdgeAttribute(edge, "weight");
    if (key.startsWith("p:")) return state.pairData[key.slice(2)]?.[pairKey(s, t)];
    if (key.startsWith("ea:")) return edgeAttrVal(edge, key.slice(3));
    return undefined;
}

// Node ids are arbitrary strings: sort them numerically when they look like numbers, lexicographically otherwise.
function idValue(id) {
    const n = Number(id);
    return (id !== "" && !Number.isNaN(n)) ? n : id;
}

const TABLE_IDS = { nodes: "nodes-table", edges: "edges-table", rec: "rec-edges-table" };

function tableModel(key) {
    if (key === "nodes") return { cols: nodeColumns(), ids: state.graph ? state.graph.nodes() : [], val: nodeRowValue };
    if (key === "rec") {
        const edges = state.rec.tableEdges || [];
        return {
            cols: [{ key: "source", label: "source" }, { key: "target", label: "target" }, { key: "score", label: "score" }, { key: "_act", label: "" }],
            ids: edges.map((_, i) => i),
            val: (i, k) => { const e = edges[i]; if (!e) return undefined; return k === "score" ? e.score : idValue(e[k]); },
        };
    }
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
    { value: "between", label: "between" },
    { value: "empty", label: "empty" },
    { value: "nonempty", label: "non-empty" },
];

// Operators that ignore the value box entirely (they test presence, not a value).
const UNARY_OPS = new Set(["empty", "nonempty"]);

function isValueEmpty(value) {
    return value === null || value === undefined || String(value).trim() === "";
}

// Whether a stored filter should actually be applied (unary ops are active with no value; "between" needs a bound).
function isFilterActive(f) {
    if (!f) return false;
    if (UNARY_OPS.has(f.op)) return true;
    if (f.op === "between") return !isValueEmpty(f.value) || !isValueEmpty(f.value2);
    return !isValueEmpty(f.value);
}

// Tests one cell value against a single column filter. fv2 is the upper bound for the "between" operator.
function matchFilter(value, op, fv, fv2) {
    if (op === "empty") return isValueEmpty(value);
    if (op === "nonempty") return !isValueEmpty(value);
    if (op === "between") {
        const numV = Number(value);
        if (isValueEmpty(value) || Number.isNaN(numV)) return false;
        const lo = isValueEmpty(fv) ? null : Number(fv);
        const hi = isValueEmpty(fv2) ? null : Number(fv2);
        if (lo !== null && !Number.isNaN(lo) && numV < lo) return false;
        if (hi !== null && !Number.isNaN(hi) && numV > hi) return false;
        return true;
    }
    if (isValueEmpty(fv)) return true; // inactive
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

// Builds a filter control (operator select + value input + a second input for "between"); calls apply(op, value,
// value2) on any change. Shared by the node/edge tables and the information-pieces table.
function makeColFilter(existing, apply) {
    const wrap = document.createElement("div");
    wrap.className = "colfilter";
    const sel = document.createElement("select");
    OPERATORS.forEach((o) => sel.appendChild(option(o.value, o.label)));
    const inp = document.createElement("input");
    inp.type = "text"; inp.placeholder = "…";
    const inp2 = document.createElement("input");
    inp2.type = "text"; inp2.placeholder = "…"; inp2.className = "filter-hi";
    if (existing) { sel.value = existing.op; inp.value = existing.value || ""; inp2.value = existing.value2 || ""; }
    const updateVis = () => {
        inp.style.display = UNARY_OPS.has(sel.value) ? "none" : "";
        inp2.style.display = sel.value === "between" ? "" : "none";
    };
    updateVis();
    const fire = () => apply(sel.value, inp.value, inp2.value);
    sel.addEventListener("change", () => { updateVis(); fire(); });
    inp.addEventListener("input", fire);
    inp2.addEventListener("input", fire);
    wrap.append(sel, inp, inp2);
    return wrap;
}

// Active filters for a table, restricted to columns that currently exist.
function activeFilters(key, cols) {
    const filters = state.tables[key].filters;
    const colKeys = new Set(cols.map((c) => c.key));
    return Object.keys(filters)
        .filter((col) => colKeys.has(col) && isFilterActive(filters[col]))
        .map((col) => ({ col, op: filters[col].op, value: filters[col].value, value2: filters[col].value2 }));
}

// True if a row passes every active filter (AND).
function rowPasses(valFn, id, filters) {
    return filters.every((f) => matchFilter(valFn(id, f.col), f.op, f.value, f.value2));
}

// Full filtered + sorted id list for a table (no pagination); used for rendering and export.
function tableRows(key) {
    const m = tableModel(key);
    const t = state.tables[key];
    const filters = activeFilters(key, m.cols);
    let ids = Array.from(m.ids);
    if (filters.length) ids = ids.filter((id) => rowPasses(m.val, id, filters));

    // "Show only" selection modes (ego-only / community-only) also restrict the node/edge tables (not the rec table).
    const focus = (key === "nodes" || key === "edges") ? selectionFocus() : null;
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
        if (c.key === "_act") { th.className = "act-th"; sortRow.appendChild(th); return; }
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
        if (c.key === "_act") { filterRow.appendChild(th); return; }   // no filter on the actions column
        const wrap = makeColFilter(state.tables[key].filters[c.key],
            (op, value, value2) => setColFilter(key, c.key, op, value, value2));
        // Attribute columns get a remove (✕) control in the header.
        const isAttr = (key === "nodes" && c.key.startsWith("a:")) || (key === "edges" && c.key.startsWith("ea:"));
        if (isAttr) {
            const rm = document.createElement("button");
            rm.textContent = "✕";
            rm.className = "col-remove";
            rm.title = "Remove attribute column";
            const name = c.key.slice(c.key.indexOf(":") + 1);
            rm.addEventListener("click", () => removeAttrColumn(key === "nodes" ? "node" : "edge", name));
            wrap.appendChild(rm);
        }
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
            if (c.key === "_act") {
                if (key === "rec") {
                    const add = document.createElement("button");
                    add.className = "row-add";
                    add.textContent = "+ Add";
                    add.title = "Add this link to the graph";
                    add.addEventListener("click", () => addRecLinkFromTable(id));
                    td.appendChild(add);
                } else {
                    const rm = document.createElement("button");
                    rm.className = "row-remove";
                    rm.textContent = "✕";
                    rm.title = key === "nodes" ? "Remove node" : "Remove edge";
                    rm.addEventListener("click", () => (key === "nodes" ? removeNodeFromTable(id) : removeEdgeFromTable(id)));
                    td.appendChild(rm);
                }
                tr.appendChild(td);
                continue;
            }
            const v = model.val(id, c.key);
            td.textContent = v === undefined || v === null ? "" : fmt(v);
            // The node id is renamable; attribute columns (a:/ea:) are editable; metric/intrinsic columns are not.
            if (key === "nodes" && c.key === "id") makeEditableCell(td, "Click to rename", (cell) => commitRename(id, cell));
            else if (key === "nodes" && c.key.startsWith("a:")) makeAttrEditable(td, "nodes", id, c.key.slice(2));
            else if (key === "edges" && c.key.startsWith("ea:")) makeAttrEditable(td, "edges", id, c.key.slice(3));
            tr.appendChild(td);
        }
        tbody.appendChild(tr);
    }
    table.appendChild(tbody);
    updatePager(key, total, pages, start, pageIds.length);
}

function setColFilter(key, col, op, value, value2) {
    const filters = state.tables[key].filters;
    const f = { op, value, value2 };
    if (isFilterActive(f)) filters[col] = f;
    else delete filters[col];
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

/* -------------------------- inline attribute editing -------------------- */

// Makes a table cell editable: commit on Enter/blur (via the callback), revert on Escape.
function makeEditableCell(td, title, commit) {
    td.contentEditable = "true";
    td.classList.add("editable");
    td.title = title;
    td.addEventListener("focus", () => { td.dataset.orig = td.textContent; });
    td.addEventListener("keydown", (e) => {
        if (e.key === "Enter") { e.preventDefault(); td.blur(); }
        else if (e.key === "Escape") { e.preventDefault(); td.textContent = td.dataset.orig || ""; td.blur(); }
    });
    td.addEventListener("blur", () => commit(td));
}

function makeAttrEditable(td, kind, id, name) {
    makeEditableCell(td, "Click to edit", () => commitAttrEdit(kind, id, name, td));
}

// Renames a node: persists on the server, then reloads the graph (preserving positions). Already-computed results
// are re-keyed (old id → new id) so the metric columns and appearance survive the rename.
async function commitRename(oldId, td) {
    const newId = td.textContent.trim();
    if (!newId || newId === oldId) { td.textContent = oldId; return; }
    if (state.graph.hasNode(newId)) { setStatus("Node " + newId + " already exists.", "error"); td.textContent = oldId; return; }
    try {
        const pos = {};
        state.graph.forEachNode((n, a) => { pos[n] = { x: a.x, y: a.y }; });
        pos[newId] = pos[oldId];
        delete pos[oldId];

        await api("/api/graph/" + state.graphId + "/node/rename", jsonBody({ old: oldId, new: newId }));
        rekeyComputedData(oldId, newId);
        const data = await api("/api/graph/" + state.graphId);          // reload with the new id
        state.attrSchema = data.schema || state.attrSchema;
        if (state.selectedNode === oldId) { state.selectedNode = newId; state.selection.node = newId; }

        renderGraph(data.graph);
        state.graph.forEachNode((n) => { if (pos[n]) { state.graph.setNodeAttribute(n, "x", pos[n].x); state.graph.setNodeAttribute(n, "y", pos[n].y); } });
        if (data.stats) { $("ov-nodes").textContent = data.stats.nodes; $("ov-edges").textContent = data.stats.edges; }
        state.pathFocus = null;

        rebuildAppearanceOptions();
        applyAppearance();
        refreshAfterCompute();
        applyReducers();
        if (state.renderer) state.renderer.refresh();
        if (state.selectedNode === newId) { $("select-node-input").value = newId; renderNodeInfo(newId); }
        setStatus("Renamed " + oldId + " → " + newId + ".");
    } catch (e) {
        setStatus(e.message, "error");
        td.textContent = oldId;
    }
}

// Rewrites computed-result keys after a node rename so previously-computed values are kept.
function rekeyComputedData(oldId, newId) {
    const renameNodeKey = (m) => {
        if (m && Object.prototype.hasOwnProperty.call(m, oldId)) { m[newId] = m[oldId]; delete m[oldId]; }
    };
    state.metricOrder.forEach((label) => renameNodeKey(state.metricData[label]));      // vertex metrics
    Object.keys(state.communityData).forEach((algo) => renameNodeKey(state.communityData[algo]));  // community colours
    // Pair/edge metric maps are keyed by "source|target".
    state.pairOrder.forEach((label) => {
        const m = state.pairData[label];
        if (!m) return;
        for (const k of Object.keys(m)) {
            const i = k.indexOf("|");
            const s = k.slice(0, i), t = k.slice(i + 1);
            if (s !== oldId && t !== oldId) continue;
            const nk = (s === oldId ? newId : s) + "|" + (t === oldId ? newId : t);
            if (nk !== k) { m[nk] = m[k]; delete m[k]; }
        }
    });
}

/* --------------------- add / remove attribute columns ------------------- */

async function addAttrColumn(target) {
    if (!requireGraph()) return;
    const nameId = target === "edge" ? "add-edge-attr-name" : "add-node-attr-name";
    const typeId = target === "edge" ? "add-edge-attr-type" : "add-node-attr-type";
    const name = $(nameId).value.trim();
    if (!name) { setStatus("Enter an attribute name.", "error"); return; }
    try {
        const res = await api("/api/graph/" + state.graphId + "/attributes/define",
            jsonBody({ target, name, type: $(typeId).value }));
        state.attrSchema = res.schema || state.attrSchema;
        $(nameId).value = "";
        state.tables.nodes.headerSig = null;
        state.tables.edges.headerSig = null;
        rebuildAppearanceOptions();
        renderTable(state.activeTableSubtab);
        setStatus("Added attribute " + name + ".");
    } catch (e) { setStatus(e.message, "error"); }
}

async function removeAttrColumn(target, name) {
    if (!window.confirm('Remove attribute "' + name + '"? This deletes its values.')) return;
    try {
        const res = await api("/api/graph/" + state.graphId + "/attributes/remove", jsonBody({ target, name }));
        state.attrSchema = res.schema || state.attrSchema;
        state.tables.nodes.headerSig = null;
        state.tables.edges.headerSig = null;
        rebuildAppearanceOptions();
        applyAppearance();
        renderTable(state.activeTableSubtab);
        setStatus("Removed attribute " + name + ".");
    } catch (e) { setStatus(e.message, "error"); }
}

// Parses an edited value to match the attribute's declared type and writes it into an "attrs" object
// (an empty value clears the attribute).
function applyLocalAttr(attrs, name, raw, defs) {
    if (raw === "") { delete attrs[name]; return; }
    const d = defs.find((x) => x.name === name);
    if (d && d.numeric) { const n = Number(raw); attrs[name] = Number.isNaN(n) ? raw : n; }
    else if (d && d.type === "bool") { attrs[name] = /^(true|1|yes|y|t)$/i.test(raw); }
    else attrs[name] = raw;
}

// Commits an edited attribute cell to the server and updates the live graph (and appearance).
async function commitAttrEdit(kind, id, name, td) {
    const raw = td.textContent.trim();
    if (td.dataset.orig !== undefined && raw === td.dataset.orig.trim()) return; // unchanged
    const g = state.graph;
    const value = raw === "" ? null : raw;
    try {
        if (kind === "nodes") {
            await api("/api/graph/" + state.graphId + "/attributes/node", jsonBody({ node: id, name, value }));
            const attrs = Object.assign({}, g.getNodeAttribute(id, "attrs") || {});
            applyLocalAttr(attrs, name, raw, nodeAttrDefs());
            g.setNodeAttribute(id, "attrs", attrs);
        } else {
            const s = g.source(id), t = g.target(id);
            await api("/api/graph/" + state.graphId + "/attributes/edge", jsonBody({ source: s, target: t, edgeId: id, name, value }));
            const attrs = Object.assign({}, g.getEdgeAttribute(id, "attrs") || {});
            applyLocalAttr(attrs, name, raw, edgeAttrDefs());
            g.setEdgeAttribute(id, "attrs", attrs);
        }
        applyAppearance();   // the edited attribute may drive size / colour / labels
        setStatus("Updated " + name + ".");
    } catch (e) {
        setStatus(e.message, "error");
        td.textContent = td.dataset.orig || "";   // revert on failure
    }
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
    renderAverages("node-averages-table", state.metricOrder, state.metricData, "vertex");
    renderAverages("edge-averages-table", state.pairOrder, state.pairData, "pair");
    renderPairAverages();
    renderCommAverages();
    syncChartSelectors();
    updateScatterBlocks();
    if (state.activeSubtab === "nodes") { drawNodeChart(); drawNodeScatter(); }
    if (state.activeSubtab === "edges") { drawEdgeChart(); drawEdgeScatter(); }
    if (state.activeSubtab === "pairs") drawPairChart();
    if (state.activeSubtab === "comm") { drawCommChart(); drawCommScatter(); }
}

// Per-community averages come from each stored metric's value map, plus one column per recommendation.
function renderCommAverages() {
    const recKeys = recColumnKeys("comm");
    const rows = state.commMetricOrder.map((label) => ({
        label,
        original: average(state.commMetricData[label].values),
        rec: recKeys.map((k) => { const m = state.recMetricData.comm?.[label]?.[k]; return m ? average(m) : undefined; }),
    }));
    buildAverageTable("comm-averages-table", rows, recKeys);
}

// Node-pair averages come from the streamed aggregate (one summary object per metric), not a per-pair map.
function renderPairAverages() {
    const recKeys = recColumnKeys("nodePair");
    const rows = state.nodePairOrder.map((label) => {
        const agg = state.nodePairAgg[label];
        return {
            label: label + (agg.estimated ? " (estimated)" : ""),
            original: agg.average,
            rec: recKeys.map((k) => state.recMetricData.nodePair?.[label]?.[k]?.average),
        };
    });
    buildAverageTable("pair-averages-table", rows, recKeys);
}

function renderGlobalTable() {
    const recKeys = recColumnKeys("graph");
    const rows = Object.keys(state.graphMetrics).map((k) => ({
        label: k,
        original: state.graphMetrics[k],
        rec: recKeys.map((rk) => state.recMetricData.graph?.[k]?.[rk]),
    }));
    buildAverageTable("global-metrics-table", rows, recKeys);
}

function average(values) {
    const arr = Object.values(values);
    if (!arr.length) return 0;
    return arr.reduce((a, b) => a + b, 0) / arr.length;
}

function renderAverages(tableId, order, data, family) {
    const recKeys = recColumnKeys(family);
    const rows = order.map((label) => ({
        label,
        original: average(data[label]),
        rec: recKeys.map((k) => { const m = state.recMetricData[family]?.[label]?.[k]; return m ? average(m) : undefined; }),
    }));
    buildAverageTable(tableId, rows, recKeys);
}

// Renders a metric/average table: a "metric" column, an "original" column, and one column per recommendation
// (headed by the model label) when any recommendation values are present. Falls back to a plain two-column table.
function buildAverageTable(tableId, rows, recKeys) {
    const table = $(tableId);
    table.innerHTML = "";
    if (recKeys.length) {
        const thead = document.createElement("thead");
        const tr = document.createElement("tr");
        tr.appendChild(thEl("metric"));
        tr.appendChild(thEl("original"));
        recKeys.forEach((k) => tr.appendChild(thEl(recLabel(k))));
        thead.appendChild(tr);
        table.appendChild(thead);
    }
    const tbody = document.createElement("tbody");
    for (const row of rows) {
        const tr = document.createElement("tr");
        tr.appendChild(tdText(row.label));
        tr.appendChild(tdText(fmt(row.original)));
        (row.rec || []).forEach((v) => tr.appendChild(tdText(v === undefined || v === null ? "" : fmt(v))));
        tbody.appendChild(tr);
    }
    table.appendChild(tbody);
}

function thEl(text) { const th = document.createElement("th"); th.textContent = text; return th; }
function tdText(text) { const td = document.createElement("td"); td.textContent = text; return td; }

// The recommendation columns currently populated for a metric family (union across that family's metrics).
function recColumnKeys(family) {
    const fam = state.recMetricData[family] || {};
    const set = new Set();
    for (const label of Object.keys(fam)) for (const k of Object.keys(fam[label])) set.add(k);
    return Array.from(set);
}

function recLabel(key) {
    return (state.rec.models[key] && state.rec.models[key].label) || key;
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

    // Original-vs-recommendation scatter selectors (metric + which recommendation column).
    syncScatterSelectors("node", "vertex", state.metricOrder);
    syncScatterSelectors("edge", "pair", state.pairOrder);
    syncScatterSelectors("comm", "comm", state.commMetricOrder);
}

// Populates a scatter subtab's metric + recommendation selectors from the metrics that have recommendation values.
function syncScatterSelectors(prefix, family, order) {
    const withRec = order.filter((label) => state.recMetricData[family] && state.recMetricData[family][label]);
    setSelectOptions(prefix + "-scatter-metric", withRec, withRec);
    const metric = $(prefix + "-scatter-metric").value;
    const keys = (state.recMetricData[family] && state.recMetricData[family][metric]) ? Object.keys(state.recMetricData[family][metric]) : [];
    setSelectOptions(prefix + "-scatter-rec", keys, keys.map(recLabel));
}

// Shows the scatter section on a subtab only when that family has at least one recommendation column.
function updateScatterBlocks() {
    toggleHidden("node-scatter-block", recColumnKeys("vertex").length === 0);
    toggleHidden("edge-scatter-block", recColumnKeys("pair").length === 0);
    toggleHidden("comm-scatter-block", recColumnKeys("comm").length === 0);
}

function toggleHidden(id, hidden) {
    const el = $(id);
    if (el) el.hidden = hidden;
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

// Shared chart typography: axis titles at 12px, tick labels at 10px (see drawBarChart / drawScatter / drawMultiLineChart).
const AXIS_TITLE_FONT = "12px sans-serif";
const AXIS_LABEL_FONT = "10px sans-serif";

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

    // Extra padding for the axis titles when present (room so the rotated y title clears the tick labels).
    const padL = 58 + (yLabel ? 18 : 0);
    const padR = 14;
    const padT = 18;
    const padB = 28 + (xLabel ? 18 : 0);
    const plotW = W - padL - padR, plotH = H - padT - padB;

    const values = items.map((d) => d.value);
    let max = Math.max(0, ...values), min = Math.min(0, ...values);
    if (max === min) max = min + 1;

    const colText = cssVar("--text", "#e6e6e6");
    const colMuted = cssVar("--muted", "#9aa0a6");
    const colBorder = cssVar("--border", "#333");
    const colBar = cssVar("--accent", "#4f9dff");

    // Axes.
    const yOf = (v) => padT + plotH - ((v - min) / (max - min)) * plotH;
    ctx.strokeStyle = colMuted;
    ctx.beginPath(); ctx.moveTo(padL, padT); ctx.lineTo(padL, padT + plotH); ctx.lineTo(padL + plotW, padT + plotH); ctx.stroke();
    ctx.font = AXIS_LABEL_FONT; ctx.textAlign = "right"; ctx.textBaseline = "middle";
    for (let g = 0; g <= 4; g++) {
        const val = min + ((max - min) * g) / 4;
        const y = yOf(val);
        ctx.fillStyle = colMuted; ctx.fillText(fmt(val), padL - 6, y);
        ctx.strokeStyle = colBorder; ctx.beginPath(); ctx.moveTo(padL, y); ctx.lineTo(padL + plotW, y); ctx.stroke();
    }
    ctx.textAlign = "start"; ctx.textBaseline = "alphabetic";
    ctx.fillStyle = colText; ctx.font = AXIS_TITLE_FONT; ctx.fillText(title + "  (n=" + items.length + ")", padL, 12);

    // Axis titles.
    if (xLabel) {
        ctx.textAlign = "center";
        ctx.fillText(xLabel, padL + plotW / 2, H - 5);
        ctx.textAlign = "start";
    }
    if (yLabel) {
        ctx.save();
        ctx.translate(14, padT + plotH / 2);
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
    const cols = model.cols.filter((c) => c.key !== "_act");   // drop the actions column from the export
    download(filename, buildCsv(cols, ids, model.val), "text/csv");
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

// Downloads a chart canvas as a PNG, compositing it over the panel background so the (transparent) plot is legible.
function downloadChartPng(canvas, filename) {
    if (typeof canvas === "string") canvas = $(canvas);
    if (!canvas || !canvas.width || !canvas.height) { setStatus("Nothing to download yet — draw the chart first.", "error"); return; }
    const out = document.createElement("canvas");
    out.width = canvas.width; out.height = canvas.height;
    const ctx = out.getContext("2d");
    ctx.fillStyle = cssVar("--panel-2", "#1e1f23");
    ctx.fillRect(0, 0, out.width, out.height);
    ctx.drawImage(canvas, 0, 0);
    out.toBlob((blob) => download(filename || "chart.png", blob, "image/png"), "image/png");
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
    if (!confirmClears("Adding a node")) return;
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

/* ---------------------------- attributes ---------------------------- */

// Uploads a node/edge attribute (TSV/CSV) file and merges the result in place (preserving the layout).
async function uploadAttributeFile(kind, file) {
    if (!requireGraph()) return;
    if (!file) { setStatus("Choose an attribute file first.", "error"); return; }
    const form = new FormData();
    form.append("file", file);
    setStatus("Importing " + kind + " attributes…", "busy");
    try {
        const res = await api("/api/graph/" + state.graphId + "/attributes/" + kind, { method: "POST", body: form });
        state.attrSchema = res.schema || state.attrSchema;
        mergeAttributes(res.graph);            // patch values in place so the layout is preserved
        state.tables.nodes.headerSig = null;
        state.tables.edges.headerSig = null;
        rebuildAppearanceOptions();
        applyAppearance();
        refreshAfterCompute();
        if (state.selectedNode != null) renderNodeInfo(state.selectedNode);
        const defs = kind === "edges" ? edgeAttrDefs() : nodeAttrDefs();
        setStatus("Imported " + defs.length + " " + kind.slice(0, -1) + " attribute(s).");
    } catch (e) {
        setStatus(e.message, "error");
    }
}

// Toolbar "Add …"/"Add column" toggles: reveal the second row with the relevant fields (or hide if re-clicked).
function toggleAddGroup(prefix, group) {
    const row = $(prefix + "-addrow");
    const next = (row.dataset.mode || "") === group ? "" : group;
    row.dataset.mode = next;
    row.hidden = next === "";
    $(prefix + "-add-main").hidden = next !== "main";
    $(prefix + "-add-col").hidden = next !== "col";
    const focusId = next === "col"
        ? (prefix === "edges" ? "add-edge-attr-name" : "add-node-attr-name")
        : (prefix === "edges" ? "add-edge-source" : "add-node-id");
    if (next) { const el = $(focusId); if (el) el.focus(); }
}

// Copies the attribute values from a freshly-serialized graph onto the live graph, keyed by node/edge identity,
// so imported attributes appear without re-importing the graph (which would reset node positions).
function mergeAttributes(serialized) {
    const g = state.graph;
    if (!g || !serialized) return;
    (serialized.nodes || []).forEach((nd) => {
        if (g.hasNode(nd.key)) g.setNodeAttribute(nd.key, "attrs", (nd.attributes && nd.attributes.attrs) || {});
    });
    (serialized.edges || []).forEach((ed) => {
        const attrs = (ed.attributes && ed.attributes.attrs) || {};
        // Multigraph edges carry a stable key, so each parallel edge is matched individually.
        if (ed.key !== undefined && g.hasEdge(ed.key)) { g.setEdgeAttribute(ed.key, "attrs", attrs); return; }
        if (!g.hasEdge(ed.source, ed.target)) return;
        let e;
        try { e = g.edge(ed.source, ed.target); }
        catch (_) { const es = g.edges(ed.source, ed.target); e = es && es[0]; }
        if (e !== undefined) g.setEdgeAttribute(e, "attrs", attrs);
    });
}

/* ------------------------------- theme ------------------------------ */

// Swaps the topbar + import logos to the dark-mode artwork when in dark theme, falling back to the light logo if
// the dark file is not present (so a missing asset never shows a broken image).
function applyLogo(dark) {
    const light = "img/relison-full-logo.png";
    const src = dark ? "img/relison-full-logo-dark.png" : light;
    document.querySelectorAll("#logo, .import-logo").forEach((img) => {
        img.onerror = () => { img.onerror = null; img.src = light; };
        img.src = src;
    });
}

function applyTheme(theme) {
    const light = theme === "light";
    document.body.classList.toggle("light", light);
    $("theme-toggle").textContent = light ? "☀️" : "🌙";
    applyLogo(!light);
    try { localStorage.setItem("relison-theme", theme); } catch (e) { /* ignore */ }
    // Charts are drawn imperatively, so re-render the visible one with the new palette.
    if (state.activeTab === "metrics") {
        if (state.activeSubtab === "nodes") { drawNodeChart(); drawNodeScatter(); }
        if (state.activeSubtab === "edges") { drawEdgeChart(); drawEdgeScatter(); }
        if (state.activeSubtab === "pairs") drawPairChart();
        if (state.activeSubtab === "comm") { drawCommChart(); drawCommScatter(); }
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

// Default label colours to a readable value for the current theme (white-ish on dark, dark on light).
(function initLabelColors() {
    const def = document.body.classList.contains("light") ? "#1c1d20" : "#e6e6e6";
    $("node-label-color").value = def;
    $("edge-label-color").value = def;
    state.labelOpts.nodeColor = def;
    state.labelOpts.edgeColor = def;
})();

/* ----------------------- recommendation / link prediction ----------------------- */

// Loads the algorithm catalog the first time the Recommendation tab is opened.
async function loadRecCatalog() {
    if (state.rec.catalogLoaded) return;
    try {
        const cat = await api("/api/recommendation/catalog");
        state.defs.recommendation = indexById(cat);
        fillCommunitySelect("rec-algo", cat);   // grouped like the community algorithms
        renderParams("rec-params", "recommendation", $("rec-algo").value);
        onRecModeChange();
        state.rec.catalogLoaded = true;
    } catch (e) {
        setStatus("Could not load recommendation catalog: " + e.message, "error");
    }
}

// The cutoff means different things per task: links per node (recommendation) vs. links overall (prediction).
function onRecModeChange() {
    $("rec-cutoff-label").textContent = $("rec-mode").value === "prediction" ? "Total links" : "Links per node";
}

// Trains the selected model (or loads it from the server cache), then overlays its links on the graph and tables.
async function applyRecommendation() {
    if (!requireGraph()) return;
    if (state.multigraph) { setStatus("Recommendation is not available for multigraphs.", "error"); return; }
    const algorithm = $("rec-algo").value;
    const mode = $("rec-mode").value;
    const cutoff = Math.max(1, parseInt($("rec-cutoff").value, 10) || 10);
    const reciprocal = $("rec-reciprocal").checked;
    setStatus("Applying " + algorithm + "…", "busy");
    $("btn-rec-apply").disabled = true;
    try {
        const res = await api("/api/recommendation/run", jsonBody(
            { graphId: state.graphId, algorithm, mode, cutoff, reciprocal, params: collectParams("rec-params") }));
        state.rec.models[res.key] = { label: res.label, mode: res.mode, cutoff: res.cutoff, edges: res.edges };
        state.rec.active = res.key;     // only one model is shown at a time
        state.rec.tableEdges = res.edges;
        state.rec.show = true;          // a freshly applied model is shown
        $("rec-edge-show").checked = true;
        drawRecOverlay();               // recommended links are drawn on the overlay, not added to the sigma graph
        renderTable("rec");
        updateRecUI();
        refreshAfterCompute();
        setStatus("Applied " + res.label + " — " + res.count + " recommended link(s).");
    } catch (e) {
        setStatus(e.message, "error");
    } finally {
        $("btn-rec-apply").disabled = false;
    }
}

// Fully removes the recommendation: overlay, model(s), metric columns/scatterplots and the recommendation table.
async function resetRecommendationResults() {
    if (state.graphId) {
        try { await api("/api/recommendation/clear", jsonBody({ graphId: state.graphId })); }
        catch (e) { /* clearing is best-effort */ }
    }
    resetRecommendation();
    refreshAfterCompute();
    setStatus("Recommendation results reset.");
}

// Commits a recommended link to the actual graph: persists it, removes it from the recommendation list, and clears the
// now-stale computed metrics (the graph changed) while keeping the rest of the recommendation list so more can be added.
async function addRecLinkFromTable(index) {
    const e = state.rec.tableEdges[index];
    if (!e || !state.graph) return;
    if (state.graph.hasEdge(e.source, e.target)) { setStatus("That edge already exists in the graph.", "error"); return; }
    try {
        const res = await api("/api/graph/" + state.graphId + "/edge", jsonBody({ source: e.source, target: e.target, weight: 1 }));
        ensureNode(e.source);
        ensureNode(e.target);
        if (!state.graph.hasEdge(e.source, e.target)) state.graph.addEdge(e.source, e.target, { weight: 1 });
        removeRecLink(e.source, e.target);   // it is now a real edge, no longer a recommendation
        resetResults();                      // computed metrics are stale now that the graph changed
        rebuildAppearanceOptions();
        applyAppearance();
        if (res.stats) { $("ov-nodes").textContent = res.stats.nodes; $("ov-edges").textContent = res.stats.edges; }
        renderTable("rec");
        drawRecOverlay();
        applyReducers();
        refreshAfterCompute();
        setStatus("Added link " + e.source + " → " + e.target + " to the graph.");
    } catch (err) {
        setStatus(err.message, "error");
    }
}

// Drops a link from the recommendation table and the active model's overlay edges.
function removeRecLink(s, t) {
    const keep = (e) => !(e.source === s && e.target === t);
    state.rec.tableEdges = state.rec.tableEdges.filter(keep);
    const m = state.rec.active && state.rec.models[state.rec.active];
    if (m) m.edges = m.edges.filter(keep);
}

// Fully drops all recommendation state and visuals; used when the graph itself changes (load / edit).
function resetRecommendation() {
    state.rec.active = null;
    state.rec.models = {};
    state.rec.tableEdges = [];
    state.recMetricData = { vertex: {}, graph: {}, pair: {}, nodePair: {}, comm: {} };
    state.tables.rec.filters = {};
    state.tables.rec.page = 0;
    state.tables.rec.headerSig = null;
    renderTable("rec");
    updateRecUI();
    drawRecOverlay();
}

// Shows/hides the recommendation-dependent controls and updates the status line.
function updateRecUI() {
    const active = !!state.rec.active;
    toggleHidden("rec-edge-block", !active);
    document.querySelectorAll(".rec-include-field").forEach((el) => { el.hidden = !active; });
    const status = $("rec-status");
    if (active) {
        const m = state.rec.models[state.rec.active];
        status.textContent = "Active: " + m.label + " (" + (m.mode === "prediction" ? "prediction" : "recommendation")
            + ", cutoff " + m.cutoff + ", " + m.edges.length + " links).";
    } else {
        status.textContent = "No recommendation applied.";
    }
}

// Debounced overlay repaint bound to sigma's afterRender: clears immediately (so dashed lines never lag behind the
// graph mid-gesture and rapid renders cost almost nothing) and does the full draw once rendering settles.
let recOverlayTimer = null;
function scheduleRecOverlay() {
    clearRecOverlay();
    if (recOverlayTimer) clearTimeout(recOverlayTimer);
    recOverlayTimer = setTimeout(() => { recOverlayTimer = null; drawRecOverlay(); }, 90);
}

// Sizes the overlay to the sigma container and clears it (no edge drawing — cheap enough to run every frame).
function clearRecOverlay() {
    const canvas = $("rec-overlay");
    if (!canvas) return;
    const cont = $("sigma-container");
    const dpr = window.devicePixelRatio || 1;
    const W = cont ? cont.clientWidth : 0, H = cont ? cont.clientHeight : 0;
    canvas.width = Math.max(1, Math.floor(W * dpr));
    canvas.height = Math.max(1, Math.floor(H * dpr));
    canvas.style.width = W + "px";
    canvas.style.height = H + "px";
    const ctx = canvas.getContext("2d");
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);
}

// The current emphasis context (table filters + selection focus / path focus), mirroring applyReducers, so the
// overlay can apply the same hide/dim decisions to its recommended edges and node copies.
function focusContext() {
    const g = state.graph;
    const nodeFilters = activeFilters("nodes", nodeColumns());
    let filterNodes = null;
    if (nodeFilters.length) {
        filterNodes = new Set();
        g.forEachNode((n) => { if (rowPasses(nodeRowValue, n, nodeFilters)) filterNodes.add(n); });
    }
    const path = state.pathFocus;
    const focus = path ? null : selectionFocus();
    return {
        filterNodes,
        path,
        focus,
        dim: !!(focus && !focus.only),
        isolate: !!(focus && focus.only),
        dimColor: cssVar("--border", "#3a3c41"),
    };
}

// "hidden" | "dim" | "normal" for a node, matching the nodeReducer in applyReducers.
function overlayNodeVerdict(node, c) {
    if (c.filterNodes && !c.filterNodes.has(node)) return "hidden";
    if (c.path) return c.path.nodes.has(node) ? "normal" : (c.path.only ? "hidden" : "dim");
    if (c.focus) {
        if (c.isolate) return c.focus.nodes.has(node) ? "normal" : "hidden";
        if (node === c.focus.selected) return "normal";
        return c.focus.nodes.has(node) ? "normal" : "dim";
    }
    return "normal";
}

// "hidden" | "dim" | "normal" for a recommended edge, matching the edgeReducer (a path may run over rec edges).
function overlayEdgeVerdict(s, t, c) {
    if (c.filterNodes && (!c.filterNodes.has(s) || !c.filterNodes.has(t))) return "hidden";
    if (c.path) {
        if (c.path.edges.has(pairKey(s, t)) || c.path.edges.has(pairKey(t, s))) return "normal";
        return c.path.only ? "hidden" : "dim";
    }
    if (c.isolate) return (c.focus.nodes.has(s) && c.focus.nodes.has(t)) ? "normal" : "hidden";
    if (c.dim) return (c.focus.nodes.has(s) && c.focus.nodes.has(t)) ? "normal" : "dim";
    return "normal";
}

// Draws the recommended edges (dashed) plus a copy of the nodes on top, aligned to sigma's camera and honouring the
// current filter / selection / path emphasis. Node copies keep the nodes visible above the recommended edges.
function drawRecOverlay() {
    clearRecOverlay();
    const canvas = $("rec-overlay");
    if (!canvas) return;
    const r = state.renderer, g = state.graph;
    if (!r || !g || state.activeTab !== "network") return;
    const model = state.rec.active && state.rec.models[state.rec.active];
    const showRec = model && state.rec.show;   // rec links are not part of the sigma graph, drawn here when enabled
    const showBorder = state.nodeBorder.on;
    if (!showRec && !showBorder) return;

    const ctx = canvas.getContext("2d");
    try {
        const c = focusContext();
        const cam = r.getCamera();
        const ratio = (cam && (cam.ratio || (cam.getState && cam.getState().ratio))) || 1;
        const pos = (id) => r.graphToViewport({ x: g.getNodeAttribute(id, "x"), y: g.getNodeAttribute(id, "y") });
        const nodeRadius = (node) => Math.max(1.5, (g.getNodeAttribute(node, "size") || 3) / Math.sqrt(ratio));

        if (showRec) {
            // Recommended edges (dashed) — same hide/dim rules as the base graph.
            ctx.lineWidth = 1.5;
            ctx.setLineDash(state.rec.diff ? [6, 4] : []);
            const edges = model.edges;
            const arrows = state.directed && edges.length <= 1500;   // arrowheads only when not too cluttered
            for (let i = 0; i < edges.length; i++) {
                const e = edges[i];
                if (!g.hasNode(e.source) || !g.hasNode(e.target)) continue;
                const verdict = overlayEdgeVerdict(e.source, e.target, c);
                if (verdict === "hidden") continue;
                const sxy = g.getNodeAttribute(e.source, "x");
                const txy = g.getNodeAttribute(e.target, "x");
                if (sxy == null || txy == null) continue;
                const ps = pos(e.source), pt = pos(e.target);
                const dimmed = verdict === "dim";
                ctx.strokeStyle = dimmed ? c.dimColor : state.rec.color;
                ctx.beginPath();
                ctx.moveTo(ps.x, ps.y);
                ctx.lineTo(pt.x, pt.y);
                ctx.stroke();
                if (arrows && !dimmed) { ctx.fillStyle = state.rec.color; drawRecArrow(ctx, ps, pt); }
            }
            ctx.setLineDash([]);

            // Node copies on top, so the nodes stay visible above the recommended edges.
            g.forEachNode((node) => {
                const verdict = overlayNodeVerdict(node, c);
                if (verdict === "hidden") return;
                const x = g.getNodeAttribute(node, "x");
                if (x == null) return;
                const p = pos(node);
                let radius = nodeRadius(node);
                let color = g.getNodeAttribute(node, "color") || "#4f9dff";
                if (verdict === "dim") { color = c.dimColor; radius = Math.max(1, radius * 0.6); }
                ctx.fillStyle = color;
                ctx.beginPath();
                ctx.arc(p.x, p.y, radius, 0, 2 * Math.PI);
                ctx.fill();
            });
        }

        if (showBorder) {
            // A stroked ring around each visible node, honouring the same hide/dim emphasis.
            ctx.setLineDash([]);
            ctx.lineWidth = state.nodeBorder.width;
            g.forEachNode((node) => {
                const verdict = overlayNodeVerdict(node, c);
                if (verdict === "hidden") return;
                const x = g.getNodeAttribute(node, "x");
                if (x == null) return;
                const p = pos(node);
                let radius = nodeRadius(node);
                if (verdict === "dim") radius = Math.max(1, radius * 0.6);
                ctx.strokeStyle = verdict === "dim" ? c.dimColor : state.nodeBorder.color;
                ctx.beginPath();
                ctx.arc(p.x, p.y, radius, 0, 2 * Math.PI);
                ctx.stroke();
            });
        }
    } catch (err) {
        console.error("Display overlay failed to draw", err);
    }
}

// Small filled arrowhead near the target endpoint, to convey direction on the dashed overlay.
function drawRecArrow(ctx, from, to) {
    const ang = Math.atan2(to.y - from.y, to.x - from.x);
    const len = 7, off = 10;   // pull the head slightly back from the node
    const tx = to.x - Math.cos(ang) * off, ty = to.y - Math.sin(ang) * off;
    ctx.save();
    ctx.setLineDash([]);
    ctx.beginPath();
    ctx.moveTo(tx, ty);
    ctx.lineTo(tx - len * Math.cos(ang - Math.PI / 7), ty - len * Math.sin(ang - Math.PI / 7));
    ctx.lineTo(tx - len * Math.cos(ang + Math.PI / 7), ty - len * Math.sin(ang + Math.PI / 7));
    ctx.closePath();
    ctx.fill();
    ctx.restore();
}

// Stores a metric's "with recommendation" result under the active model key, for its average column / scatter.
function storeRecMetric(family, label, value) {
    if (!state.rec.active) return;
    const fam = state.recMetricData[family];
    if (!fam[label]) fam[label] = {};
    fam[label][state.rec.active] = value;
}

/* ------------------------------ scatterplots ------------------------------ */

function drawNodeScatter() {
    const metric = $("node-scatter-metric").value, recKey = $("node-scatter-rec").value;
    const orig = state.metricData[metric], rmap = state.recMetricData.vertex?.[metric]?.[recKey];
    if (!metric || !recKey || !orig || !rmap) { clearChart("node-scatter"); return; }
    const points = Object.keys(orig).filter((n) => rmap[n] !== undefined).map((n) => ({ label: n, x: orig[n], y: rmap[n] }));
    drawScatter("node-scatter", "node-scatter-tip", points, metric + " — original vs " + recLabel(recKey), "original", "recommendation");
}

function drawEdgeScatter() {
    const metric = $("edge-scatter-metric").value, recKey = $("edge-scatter-rec").value;
    const orig = state.pairData[metric], rmap = state.recMetricData.pair?.[metric]?.[recKey];
    if (!metric || !recKey || !orig || !rmap) { clearChart("edge-scatter"); return; }
    const points = Object.keys(orig).filter((k) => rmap[k] !== undefined)
        .map((k) => ({ label: k.replace("|", "→"), x: orig[k], y: rmap[k] }));
    drawScatter("edge-scatter", "edge-scatter-tip", points, metric + " — original vs " + recLabel(recKey), "original", "recommendation");
}

function drawCommScatter() {
    const metric = $("comm-scatter-metric").value, recKey = $("comm-scatter-rec").value;
    const entry = state.commMetricData[metric], rmap = state.recMetricData.comm?.[metric]?.[recKey];
    if (!metric || !recKey || !entry || !rmap) { clearChart("comm-scatter"); return; }
    const orig = entry.values;
    const points = Object.keys(orig).filter((c) => rmap[c] !== undefined)
        .map((c) => ({ label: "community " + c, x: orig[c], y: rmap[c] }));
    drawScatter("comm-scatter", "comm-scatter-tip", points, metric + " — original vs " + recLabel(recKey), "original", "recommendation");
}

// Draws a scatter of (original, recommendation) values with a y=x reference line; shares the chart palette.
function drawScatter(canvasId, tipId, points, title, xLabel, yLabel) {
    const canvas = $(canvasId);
    if (!canvas) return;
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    const W = Math.max(1, Math.floor(rect.width)), H = Math.max(1, Math.floor(rect.height));
    canvas.width = W * dpr;
    canvas.height = H * dpr;
    const ctx = canvas.getContext("2d");
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);
    if (!points.length) return;

    const padL = 64, padR = 14, padT = 18, padB = 46;
    const plotW = W - padL - padR, plotH = H - padT - padB;
    let lo = Infinity, hi = -Infinity;
    for (const p of points) { lo = Math.min(lo, p.x, p.y); hi = Math.max(hi, p.x, p.y); }
    if (hi === lo) hi = lo + 1;

    const colText = cssVar("--text", "#e6e6e6"), colMuted = cssVar("--muted", "#9aa0a6");
    const colBorder = cssVar("--border", "#333"), colDot = cssVar("--accent", "#4f9dff");
    const xOf = (v) => padL + ((v - lo) / (hi - lo)) * plotW;
    const yOf = (v) => padT + plotH - ((v - lo) / (hi - lo)) * plotH;

    ctx.strokeStyle = colMuted;
    ctx.beginPath(); ctx.moveTo(padL, padT); ctx.lineTo(padL, padT + plotH); ctx.lineTo(padL + plotW, padT + plotH); ctx.stroke();
    ctx.font = AXIS_LABEL_FONT; ctx.fillStyle = colMuted;
    for (let i = 0; i <= 4; i++) {
        const val = lo + ((hi - lo) * i) / 4;
        const y = yOf(val);
        ctx.fillStyle = colMuted; ctx.textAlign = "right"; ctx.textBaseline = "middle";
        ctx.fillText(fmt(val), padL - 6, y);
        ctx.strokeStyle = colBorder; ctx.beginPath(); ctx.moveTo(padL, y); ctx.lineTo(padL + plotW, y); ctx.stroke();
        ctx.textAlign = "center"; ctx.textBaseline = "alphabetic";
        ctx.fillStyle = colMuted; ctx.fillText(fmt(val), xOf(val), padT + plotH + 14);
    }
    ctx.textAlign = "start"; ctx.textBaseline = "alphabetic";

    // y = x reference line.
    ctx.strokeStyle = colMuted; ctx.setLineDash([4, 4]);
    ctx.beginPath(); ctx.moveTo(xOf(lo), yOf(lo)); ctx.lineTo(xOf(hi), yOf(hi)); ctx.stroke();
    ctx.setLineDash([]);

    ctx.fillStyle = colText; ctx.font = AXIS_TITLE_FONT;
    ctx.fillText(title + "  (n=" + points.length + ")", padL, 12);
    ctx.textAlign = "center"; ctx.fillText(xLabel, padL + plotW / 2, H - 5);
    ctx.save(); ctx.translate(14, padT + plotH / 2); ctx.rotate(-Math.PI / 2); ctx.fillText(yLabel, 0, 0); ctx.restore();
    ctx.textAlign = "start";

    ctx.fillStyle = colDot;
    const dots = [];
    for (const p of points) {
        const x = xOf(p.x), y = yOf(p.y);
        ctx.beginPath(); ctx.arc(x, y, 2.5, 0, 2 * Math.PI); ctx.fill();
        dots.push({ x, y, label: p.label, px: p.x, py: p.y });
    }

    const tip = $(tipId);
    canvas.onmousemove = (ev) => {
        const r = canvas.getBoundingClientRect();
        const mx = ev.clientX - r.left, my = ev.clientY - r.top;
        let best = null, bd = 1e9;
        for (const d of dots) { const dd = (d.x - mx) ** 2 + (d.y - my) ** 2; if (dd < bd) { bd = dd; best = d; } }
        if (best && bd < 120) {
            tip.hidden = false;
            tip.style.left = best.x + "px";
            tip.style.top = best.y + "px";
            tip.textContent = best.label + ": " + fmt(best.px) + " → " + fmt(best.py);
        } else { tip.hidden = true; }
    };
    canvas.onmouseleave = () => { tip.hidden = true; };
}

/* ------------------------ information diffusion ------------------------ */

const DIFF_COL = { base: "#5a5d63", informed: "#4f9dff", newly: "#28c76f", prop: "#ff9f43" };

// Loads the diffusion catalog once and fills all the configuration selectors.
async function loadDiffusionCatalog() {
    if (state.diffusion.loaded) return;
    try {
        const cat = await api("/api/diffusion/catalog");
        state.defs.protocol = indexById(cat.protocol);
        state.defs.selection = indexById(cat.selection);
        state.defs.expiration = indexById(cat.expiration);
        state.defs.propagation = indexById(cat.propagation);
        state.defs.update = indexById(cat.update);
        state.defs.sight = indexById(cat.sight);
        state.defs.stop = indexById(cat.stop);
        state.defs.metric = indexById(cat.metric);

        fillSelect("diff-protocol", cat.protocol);
        fillSelect("diff-selection", cat.selection);
        fillSelect("diff-expiration", cat.expiration);
        fillSelect("diff-propagation", cat.propagation);
        fillSelect("diff-update", cat.update);
        fillSelect("diff-sight", cat.sight);
        fillSelect("diff-stop", cat.stop);
        fillCommunitySelect("diff-metrics", cat.metric);   // grouped multiselect of metrics to compute

        renderParams("diff-protocol-params", "protocol", $("diff-protocol").value);
        renderParams("diff-selection-params", "selection", $("diff-selection").value);
        renderParams("diff-expiration-params", "expiration", $("diff-expiration").value);
        renderParams("diff-propagation-params", "propagation", $("diff-propagation").value);
        renderParams("diff-update-params", "update", $("diff-update").value);
        renderParams("diff-sight-params", "sight", $("diff-sight").value);
        renderParams("diff-stop-params", "stop", $("diff-stop").value);
        state.diffusion.loaded = true;
    } catch (e) {
        setStatus("Could not load diffusion catalog: " + e.message, "error");
    }
}

function onDiffProtocolType() {
    const custom = $("diff-protocol-type").value === "custom";
    toggleHidden("diff-preset-wrap", custom);
    toggleHidden("diff-custom-wrap", !custom);
}

function enterDiffusionTab() {
    loadDiffusionCatalog();
    if (!state.graph) return;
    if (!state.diffusion.graph) initDiffGraph();
    else syncDiffAppearance();   // mirror any appearance changes made on the Network tab while we were away
    if (state.diffusion.renderer) setTimeout(() => { state.diffusion.renderer.refresh(); drawDiffOverlay(); }, 0);
    if (state.diffusion.result) setDiffIteration(state.diffusion.iteration);
    if (state.diffusion.subview === "metrics") renderDiffMetrics();
    if (state.diffusion.subview === "pieces") renderPiecesTable();
}

// Builds a sigma renderer over a copy of the loaded graph (sharing its node positions).
function initDiffGraph() {
    if (!state.graph || !SigmaClass) return;
    if (state.diffusion.renderer) { try { state.diffusion.renderer.kill(); } catch (e) { /* ignore */ } }
    const exported = state.graph.export();
    const g = new Graph({
        type: (exported.options && exported.options.type) || (state.directed ? "directed" : "undirected"),
        multi: !!(exported.options && exported.options.multi),
        allowSelfLoops: true,
    });
    g.import(exported);
    g.forEachNode((n) => g.setNodeAttribute(n, "color", DIFF_COL.base));
    state.diffusion.graph = g;
    $("diff-empty-hint").style.display = "none";
    const o = state.labelOpts;
    state.diffusion.renderer = new SigmaClass(g, $("diff-sigma-container"), {
        defaultEdgeType: state.directed ? "arrow" : "line",
        renderLabels: o.nodeShow,
        renderEdgeLabels: o.edgeShow,
        labelRenderer: drawDiffNodeLabel,
        edgeLabelRenderer: drawDiffEdgeLabel,
        labelDensity: 0.5,
        labelRenderedSizeThreshold: 8,
        allowInvalidContainer: true,
    });
    state.diffusion.renderer.on("clickNode", ({ node }) => selectDiffNode(node));
    state.diffusion.renderer.on("afterRender", drawDiffOverlay);
    syncDiffAppearance();
    buildDiffLegend();
}

// Mirrors the main display's drawing into the diffusion canvas — node sizes/positions, edge thickness/colour, labels
// (geometry + which are shown + size/font), and node borders — but NOT node or label colours, which the diffusion
// view drives itself (by diffusion state). Called when entering the tab and after appearance changes.
function syncDiffAppearance() {
    const g = state.diffusion.graph, main = state.graph;
    if (!g || !main) return;
    g.forEachNode((n) => {
        if (!main.hasNode(n)) return;
        g.setNodeAttribute(n, "size", main.getNodeAttribute(n, "size"));
        g.setNodeAttribute(n, "x", main.getNodeAttribute(n, "x"));
        g.setNodeAttribute(n, "y", main.getNodeAttribute(n, "y"));
        g.setNodeAttribute(n, "label", main.getNodeAttribute(n, "label"));
        // node "color" is intentionally left as the diffusion-state colour.
    });
    g.forEachEdge((e) => {
        if (!main.hasEdge(e)) return;
        g.setEdgeAttribute(e, "size", main.getEdgeAttribute(e, "size"));
        g.setEdgeAttribute(e, "color", main.getEdgeAttribute(e, "color"));
        g.setEdgeAttribute(e, "label", main.getEdgeAttribute(e, "label"));
    });
    const r = state.diffusion.renderer, o = state.labelOpts;
    if (r) {
        // Mirror which labels are shown and their size/font (but not their colour — see drawDiffNodeLabel).
        r.setSetting("renderLabels", o.nodeShow);
        r.setSetting("renderEdgeLabels", o.edgeShow);
        r.setSetting("labelSize", o.nodeSize);
        r.setSetting("edgeLabelSize", o.edgeSize);
        r.setSetting("labelFont", o.nodeFont);
        r.setSetting("edgeLabelFont", o.edgeFont);
        r.refresh();
    }
    drawDiffOverlay();
}

// Builds the node-colour legend shown over the diffusion canvas, sourced from DIFF_COL so it stays in sync.
function buildDiffLegend() {
    const el = $("diff-legend");
    if (!el) return;
    const items = [
        ["Not informed", DIFF_COL.base],
        ["Informed (received)", DIFF_COL.informed],
        ["Newly informed", DIFF_COL.newly],
        ["Propagating", DIFF_COL.prop],
    ];
    el.innerHTML = "";
    items.forEach(([label, color]) => {
        const row = document.createElement("div");
        row.className = "legend-row";
        const sw = document.createElement("span");
        sw.className = "legend-swatch";
        sw.style.background = color;
        const lab = document.createElement("span");
        lab.textContent = label;
        row.appendChild(sw);
        row.appendChild(lab);
        el.appendChild(row);
    });
    el.hidden = false;
}

function selectDiffNode(node) {
    state.diffusion.selectedNode = node;
    $("diff-node-input").value = node;
    $("diff-state-hint").textContent = "Node " + node;
    fetchDiffState();
}

// Distinct colours for overlaid metric series (one per accumulated run).
const SERIES_COLORS = ["#4f9dff", "#ff9f43", "#28c76f", "#e0576b", "#a66bff", "#22c3c3", "#f6c744", "#8892a0"];

// A human label for the currently-configured protocol (used to tag an accumulated run).
function protocolLabel(type) {
    if (type === "custom") return "Custom";
    const id = $("diff-protocol").value;
    const def = state.defs.protocol && state.defs.protocol[id];
    return def && def.label ? def.label : id;
}

// Ensures each accumulated run has a distinct label, appending " #k" on collision.
function uniqueRunLabel(base) {
    const used = new Set(state.diffusion.runs.map((r) => r.label));
    if (!used.has(base)) return base;
    let k = 2;
    while (used.has(base + " #" + k)) k++;
    return base + " #" + k;
}

// The feature parameters currently available: info-piece feature names (from the pieces) and user feature names —
// the graph's node attributes plus the detected community partitions, both exposed by the server as user features.
function knownFeatureParams() {
    const info = new Set();
    state.diffusion.pieces.forEach((p) => (p.features || []).forEach((f) => { if (f.param) info.add(f.param); }));
    const user = new Set();
    (state.attrSchema && state.attrSchema.node ? state.attrSchema.node : []).forEach((a) => { if (a.name) user.add(a.name); });
    Object.keys(state.communityData || {}).forEach((name) => user.add(name));   // communities as user features
    return { info: [...info], user: [...user] };
}

// Builds the metrics list for a run. Feature metrics (those with a "feature" parameter) are expanded into one
// instance per known feature parameter — over info-piece features and over user (node-attribute) features — so the
// user doesn't have to type feature names; plain metrics are sent as-is.
function buildDiffMetricsRequest() {
    const feats = knownFeatureParams();
    const out = [];
    for (const id of selectedOptions("diff-metrics")) {
        const def = state.defs.metric && state.defs.metric[id];
        const isFeatureMetric = def && def.params && def.params.some((p) => p.name === "feature");
        if (!isFeatureMetric) { out.push({ id }); continue; }
        feats.info.forEach((name) => out.push({ id, params: { feature: name, userFeature: false } }));
        feats.user.forEach((name) => out.push({ id, params: { feature: name, userFeature: true } }));
    }
    return out;
}

// Runs the simulation with the configured protocol / stop / metrics.
async function runDiffusion() {
    if (!requireGraph()) return;
    if (!state.diffusion.pieces.length) { setStatus("Add at least one information piece to run a simulation.", "error"); return; }
    // When "apply filters to simulation" is on, only the pieces passing the table filters are simulated.
    let simPieces = null;
    if (state.diffusion.applyFiltersToSim && activePieceFilters().length) {
        simPieces = filteredPieceIndices().map((i) => state.diffusion.pieces[i]);
        if (!simPieces.length) { setStatus("No information pieces pass the current filters.", "error"); return; }
    }
    if (!state.diffusion.graph) initDiffGraph();
    const type = $("diff-protocol-type").value;
    const protocol = type === "custom"
        ? {
            type: "custom",
            selection: { id: $("diff-selection").value, params: collectParams("diff-selection-params") },
            expiration: { id: $("diff-expiration").value, params: collectParams("diff-expiration-params") },
            propagation: { id: $("diff-propagation").value, params: collectParams("diff-propagation-params") },
            update: { id: $("diff-update").value, params: collectParams("diff-update-params") },
            sight: { id: $("diff-sight").value, params: collectParams("diff-sight-params") },
          }
        : { type: "preset", id: $("diff-protocol").value, params: collectParams("diff-protocol-params") };
    const stop = { id: $("diff-stop").value, params: collectParams("diff-stop-params") };
    const metrics = buildDiffMetricsRequest();
    const protoLabel = protocolLabel(type);

    setStatus("Running diffusion…", "busy");
    $("diff-status").textContent = "Running…";
    $("btn-diff-run").disabled = true;
    try {
        await persistPiecesNow();   // ensure the session has the latest pieces; the run reads them from there
        const body = { graphId: state.graphId, protocol, stop, metrics, filters: [] };
        if (simPieces) body.pieces = simPieces;   // filtered subset overrides the persisted (full) set for this run
        const res = await api("/api/diffusion/run", jsonBody(body));
        state.diffusion.result = res;
        state.diffusion.iteration = 0;
        // Accumulate this run's metric series (tagged with a unique protocol label) for overlaid plotting.
        if (res.metrics && res.metrics.length) {
            state.diffusion.runs.push({ label: uniqueRunLabel(protoLabel), numIterations: res.numIterations, metrics: res.metrics });
        }
        $("diff-empty-hint").style.display = "none";
        toggleHidden("diff-timebar", res.numIterations <= 0);
        const slider = $("diff-slider");
        slider.min = 0; slider.max = Math.max(0, res.numIterations - 1); slider.value = 0;
        setDiffIteration(0);
        populateDiffMetricSelect();
        renderDiffMetrics();
        $("diff-status").textContent = "Done — " + res.numIterations + " iteration(s).";
        setStatus("Diffusion finished (" + res.numIterations + " iterations).");
    } catch (e) {
        $("diff-status").textContent = e.message;
        setStatus(e.message, "error");
    } finally {
        $("btn-diff-run").disabled = false;
    }
}

// Colours the diffusion graph for an iteration and refreshes the slider, overlay and node panel.
function setDiffIteration(i) {
    const res = state.diffusion.result, g = state.diffusion.graph;
    if (!res || !g) return;
    i = Math.max(0, Math.min(i, res.numIterations - 1));
    state.diffusion.iteration = i;

    const informed = new Set(), newly = new Set(res.iterations[i].newlyInformed), prop = new Set(res.iterations[i].propagating);
    for (let j = 0; j <= i; j++) res.iterations[j].newlyInformed.forEach((n) => informed.add(n));
    g.forEachNode((n) => {
        let c = DIFF_COL.base;
        if (informed.has(n)) c = DIFF_COL.informed;
        if (newly.has(n)) c = DIFF_COL.newly;
        if (prop.has(n)) c = DIFF_COL.prop;
        g.setNodeAttribute(n, "color", c);
    });
    $("diff-slider").value = i;
    $("diff-iter-label").textContent = "iteration " + i + " / " + (res.numIterations - 1);
    if (state.diffusion.renderer) state.diffusion.renderer.refresh();
    drawDiffOverlay();
    if (state.diffusion.selectedNode) fetchDiffState();
}

// Draws, for the current iteration, dashed spread edges from each propagating user to its neighbours.
function drawDiffOverlay() {
    const canvas = $("diff-overlay");
    if (!canvas) return;
    const cont = $("diff-sigma-container"), r = state.diffusion.renderer, g = state.diffusion.graph, res = state.diffusion.result;
    const dpr = window.devicePixelRatio || 1;
    const W = cont ? cont.clientWidth : 0, H = cont ? cont.clientHeight : 0;
    canvas.width = Math.max(1, Math.floor(W * dpr));
    canvas.height = Math.max(1, Math.floor(H * dpr));
    canvas.style.width = W + "px"; canvas.style.height = H + "px";
    const ctx = canvas.getContext("2d");
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);
    if (!r || !g || state.activeTab !== "diffusion") return;

    try {
        const pos = (n) => r.graphToViewport({ x: g.getNodeAttribute(n, "x"), y: g.getNodeAttribute(n, "y") });

        if (res) {
            const prop = res.iterations[state.diffusion.iteration].propagating;
            ctx.strokeStyle = DIFF_COL.prop; ctx.lineWidth = 1.4; ctx.setLineDash([5, 4]);
            let drawn = 0;
            for (const u of prop) {
                if (!g.hasNode(u) || drawn > 3000) break;
                const pu = pos(u);
                const each = (v) => {
                    if (drawn++ > 3000) return;
                    const pv = pos(v);
                    ctx.beginPath(); ctx.moveTo(pu.x, pu.y); ctx.lineTo(pv.x, pv.y); ctx.stroke();
                };
                if (state.directed) g.forEachOutNeighbor(u, each); else g.forEachNeighbor(u, each);
            }
            ctx.setLineDash([]);
        }

        // Mirror the main display's node borders (a drawing attribute, not a node colour).
        if (state.nodeBorder.on) {
            const cam = r.getCamera();
            const ratio = (cam && (cam.ratio || (cam.getState && cam.getState().ratio))) || 1;
            ctx.lineWidth = state.nodeBorder.width;
            ctx.strokeStyle = state.nodeBorder.color;
            g.forEachNode((n) => {
                if (g.getNodeAttribute(n, "x") == null) return;
                const p = pos(n);
                const radius = Math.max(1.5, (g.getNodeAttribute(n, "size") || 3) / Math.sqrt(ratio));
                ctx.beginPath(); ctx.arc(p.x, p.y, radius, 0, 2 * Math.PI); ctx.stroke();
            });
        }
    } catch (err) {
        console.error("Diffusion overlay failed", err);
    }
}

// Fetches and renders the per-node, per-iteration information lists.
// Information categories shown for the selected node, each with a this-iteration and an overall list.
const DIFF_CATS = [
    ["own", "Own information"],
    ["propagated", "Propagated"],
    ["read", "Read"],
    ["received", "Received"],
    ["discarded", "Discarded"],
];

async function fetchDiffState() {
    const node = state.diffusion.selectedNode;
    if (!node || !state.diffusion.result) return;
    try {
        const res = await api("/api/diffusion/state", jsonBody(
            { graphId: state.graphId, iteration: state.diffusion.iteration, node }));
        renderDiffState(res);
    } catch (e) { /* ignore transient state errors */ }
}

function renderDiffState(res) {
    const host = $("diff-cats");
    if (!host) return;
    host.innerHTML = "";
    for (const [key, label] of DIFF_CATS) {
        const cat = res[key] || { iter: [], overall: [] };
        const block = document.createElement("div");
        block.className = "diff-cat";
        const h = document.createElement("span");
        h.className = "sublabel";
        h.textContent = label;
        block.appendChild(h);
        const cols = document.createElement("div");
        cols.className = "diff-cat-cols";
        cols.appendChild(diffCatColumn("This iteration", cat.iter));
        cols.appendChild(diffCatColumn("Overall", cat.overall));
        block.appendChild(cols);
        host.appendChild(block);
    }
}

function diffCatColumn(title, items) {
    const col = document.createElement("div");
    col.className = "diff-cat-col";
    const t = document.createElement("span");
    t.className = "diff-col-h";
    t.textContent = title + " (" + (items ? items.length : 0) + ")";
    col.appendChild(t);
    const tbl = document.createElement("div");
    tbl.className = "scroll-table";
    if (!items || !items.length) {
        tbl.innerHTML = '<div class="empty">none</div>';
    } else {
        for (const it of items) {
            const d = document.createElement("div");
            d.className = "row";
            d.textContent = it;
            tbl.appendChild(d);
        }
    }
    col.appendChild(tbl);
    return col;
}

// Exports every computed diffusion metric across all runs as a wide CSV: one row per iteration, one column
// per (metric, run) named "Metric (diffusion protocol)".
function downloadDiffMetricsCsv() {
    const runs = state.diffusion.runs;
    if (!runs.length) { setStatus("Run a simulation with metrics first.", "error"); return; }
    // One column per (run, metric); track the longest series for the row count.
    const cols = [];
    let maxIters = 0;
    for (const run of runs) {
        maxIters = Math.max(maxIters, run.numIterations || 0);
        for (const m of run.metrics) cols.push({ header: m.label + " (" + run.label + ")", values: m.values });
    }
    const lines = [["iteration", ...cols.map((c) => c.header)].map(csvCell).join(",")];
    for (let i = 0; i < maxIters; i++) {
        const row = [i, ...cols.map((c) => (c.values && Number.isFinite(c.values[i]) ? c.values[i] : ""))];
        lines.push(row.map(csvCell).join(","));
    }
    download("diffusion-metrics.csv", lines.join("\n"), "text/csv");
}

function selectedOptions(id) { return Array.from($(id).selectedOptions).map((o) => o.value); }

function diffPlay() {
    if (state.diffusion.playing) { diffStop(); return; }
    if (!state.diffusion.result) return;
    $("diff-play").textContent = "⏸";
    state.diffusion.playing = setInterval(() => {
        const res = state.diffusion.result;
        if (!res) { diffStop(); return; }
        const next = state.diffusion.iteration + 1;
        if (next >= res.numIterations) { diffStop(); return; }
        setDiffIteration(next);
    }, 700);
}

function diffStop() {
    if (state.diffusion.playing) { clearInterval(state.diffusion.playing); state.diffusion.playing = null; }
    const p = $("diff-play"); if (p) p.textContent = "▶";
}

async function clearDiffusion() {
    if (state.graphId) { try { await api("/api/diffusion/clear", jsonBody({ graphId: state.graphId })); } catch (e) { /* best effort */ } }
    resetDiffusion();
}

function resetDiffusion() {
    diffStop();
    if (state.diffusion.renderer) { try { state.diffusion.renderer.kill(); } catch (e) { /* ignore */ } state.diffusion.renderer = null; }
    state.diffusion.graph = null;
    state.diffusion.result = null;
    state.diffusion.runs = [];
    state.diffusion.iteration = 0;
    state.diffusion.selectedNode = null;
    toggleHidden("diff-timebar", true);
    toggleHidden("diff-legend", true);
    const eh = $("diff-empty-hint"); if (eh) eh.style.display = "";
    const cats = $("diff-cats"); if (cats) cats.innerHTML = "";
    const ds = $("diff-status"); if (ds) ds.textContent = "No simulation run yet.";
    const ch = $("diffmetrics-charts"); if (ch) ch.innerHTML = "";
}

/* ----------------------- diffusion information pieces ----------------------- */

// Whether there is any simulation output (canvas result or accumulated metric runs) that a change would invalidate.
function diffusionHasResults() {
    return !!state.diffusion.result || state.diffusion.runs.length > 0;
}

// Clears the simulation output (canvas result + metric plots) without touching the graph copy or the pieces. Used
// when the information pieces change, since the existing results describe a different set of pieces.
function invalidateDiffusionResults() {
    diffStop();
    state.diffusion.result = null;
    state.diffusion.runs = [];
    state.diffusion.iteration = 0;
    toggleHidden("diff-timebar", true);
    toggleHidden("diff-legend", true);
    const eh = $("diff-empty-hint"); if (eh) eh.style.display = "";
    const cats = $("diff-cats"); if (cats) cats.innerHTML = "";
    const ds = $("diff-status"); if (ds) ds.textContent = "No simulation run yet.";
    populateDiffMetricSelect();
    renderDiffMetrics();
    const g = state.diffusion.graph;
    if (g) {
        g.forEachNode((n) => g.setNodeAttribute(n, "color", DIFF_COL.base));
        if (state.diffusion.renderer) state.diffusion.renderer.refresh();
    }
    drawDiffOverlay();
}

// Confirms a change to the information pieces. If there are diffusion results, it warns that they will be cleared;
// on confirmation it clears them and returns true. Returns false if the user cancels (so the change is aborted).
function confirmPiecesChange() {
    if (!diffusionHasResults()) return true;
    if (!window.confirm("Changing the information pieces will clear the current diffusion results and metric plots. Continue?")) return false;
    invalidateDiffusionResults();
    return true;
}

function updatePiecesSummary() {
    const el = $("diff-pieces-summary");
    if (!el) return;
    const pieces = state.diffusion.pieces;
    const creators = new Set(pieces.map((p) => p.creator).filter(Boolean));
    let text = pieces.length === 0
        ? "No pieces defined — add or generate at least one piece to run a simulation."
        : pieces.length + " piece(s) from " + creators.size + " creator(s).";
    text += state.diffusion.piecesSaved ? "  ·  saved" : "  ·  saving…";
    el.textContent = text;
    updateDiffRunEnabled();
}

// The simulation can only run when at least one information piece is defined.
function updateDiffRunEnabled() {
    const btn = $("btn-diff-run");
    if (!btn) return;
    const has = state.diffusion.pieces.length > 0;
    btn.disabled = !has;
    btn.title = has ? "" : "Add at least one information piece (in the \"Information pieces\" subtab) to run.";
}

// Persistence: the pieces are stored on the session so they survive across runs and aren't resent on every run.
let piecesSaveTimer = null;

function setPiecesSaved(saved) {
    state.diffusion.piecesSaved = saved;
    updatePiecesSummary();
}

// Saves the pieces to the session immediately (cancelling any pending debounced save). Returns a promise.
function persistPiecesNow() {
    if (piecesSaveTimer) { clearTimeout(piecesSaveTimer); piecesSaveTimer = null; }
    if (!state.graphId) return Promise.resolve();
    return api("/api/diffusion/pieces", jsonBody({ graphId: state.graphId, pieces: state.diffusion.pieces }))
        .then(() => setPiecesSaved(true))
        .catch(() => { /* best effort; the pieces are also revalidated server-side at run time */ });
}

// Debounced save, used while the user is typing in the table.
function schedulePersistPieces() {
    setPiecesSaved(false);
    if (piecesSaveTimer) clearTimeout(piecesSaveTimer);
    piecesSaveTimer = setTimeout(persistPiecesNow, 600);
}

// The pieces-table columns, in order: fixed columns plus one per feature parameter (prefixed "feat:").
function pieceColumns() {
    return ["id", "creator", "timestamp"].concat(state.diffusion.featureParams.map((p) => "feat:" + p));
}

// Renders the pieces table. The header (column titles + filter row) is rebuilt only when the column set changes, so
// typing in a filter never recreates the input and never loses focus; the body (a single page of rows) is rebuilt
// on every filter / page / edit.
function renderPiecesTable() {
    const table = $("diff-pieces-table");
    if (!table) return;
    syncFeatureParams();
    ensurePiecesHeader();
    renderPiecesBody();
}

function ensurePiecesHeader() {
    const table = $("diff-pieces-table");
    const d = state.diffusion;
    const sig = pieceColumns().join("|");
    if (d.piecesHeaderSig === sig && table.querySelector("thead")) return;
    d.piecesHeaderSig = sig;
    const old = table.querySelector("thead");
    if (old) old.remove();

    const thead = document.createElement("thead");
    const htr = document.createElement("tr");
    ["Id", "Creator (node)", "Timestamp"].forEach((h) => { const th = document.createElement("th"); th.textContent = h; htr.appendChild(th); });
    d.featureParams.forEach((param) => htr.appendChild(featureColumnHeader(param)));
    htr.appendChild(document.createElement("th"));   // delete-button column
    thead.appendChild(htr);

    const ftr = document.createElement("tr");
    ftr.className = "filter-row";
    pieceColumns().forEach((col) => ftr.appendChild(pieceFilterTh(col)));
    ftr.appendChild(document.createElement("th"));   // delete-button column has no filter
    thead.appendChild(ftr);

    table.insertBefore(thead, table.firstChild);
}

// One filter cell (operator + value[s]), mirroring the node/edge tables.
function pieceFilterTh(col) {
    const th = document.createElement("th");
    th.appendChild(makeColFilter(state.diffusion.pieceFilters[col],
        (op, value, value2) => setPieceFilter(col, op, value, value2)));
    return th;
}

function renderPiecesBody() {
    const table = $("diff-pieces-table");
    const d = state.diffusion;
    updatePiecesSummary();
    const old = table.querySelector("tbody");
    if (old) old.remove();

    const idxs = filteredPieceIndices();
    const total = idxs.length;
    const pages = Math.max(1, Math.ceil(total / d.piecesPageSize));
    if (d.piecesPage >= pages) d.piecesPage = pages - 1;
    if (d.piecesPage < 0) d.piecesPage = 0;
    const start = d.piecesPage * d.piecesPageSize;
    const end = Math.min(total, start + d.piecesPageSize);

    const tbody = document.createElement("tbody");
    for (let k = start; k < end; k++) { const i = idxs[k]; tbody.appendChild(pieceRow(d.pieces[i], i)); }
    table.appendChild(tbody);

    updatePiecesPager(total, pages, start, end - start);
}

/* pieces-table filtering (mirrors the node/edge table filters) */

function pieceColValue(p, col) {
    if (col === "id") return p.id;
    if (col === "creator") return p.creator;
    if (col === "timestamp") return p.timestamp;
    if (col.startsWith("feat:")) return encodeParamValues(p.features, col.slice(5));
    return "";
}

function activePieceFilters() {
    const f = state.diffusion.pieceFilters || {};
    const cols = new Set(pieceColumns());
    return Object.keys(f)
        .filter((c) => cols.has(c) && isFilterActive(f[c]))
        .map((c) => ({ col: c, op: f[c].op, value: f[c].value, value2: f[c].value2 }));
}

function pieceRowPasses(p, filters) {
    return filters.every((f) => matchFilter(pieceColValue(p, f.col), f.op, f.value, f.value2));
}

// Indices (into state.diffusion.pieces) of the pieces that pass the active filters, in order.
function filteredPieceIndices() {
    const filters = activePieceFilters();
    const out = [];
    state.diffusion.pieces.forEach((p, i) => { if (!filters.length || pieceRowPasses(p, filters)) out.push(i); });
    return out;
}

function setPieceFilter(col, op, value, value2) {
    const f = { op, value, value2 };
    if (isFilterActive(f)) state.diffusion.pieceFilters[col] = f;
    else delete state.diffusion.pieceFilters[col];
    state.diffusion.piecesPage = 0;
    renderPiecesBody();   // body only, so the filter input the user is typing in keeps focus
}

function clearPieceFilters() {
    state.diffusion.pieceFilters = {};
    state.diffusion.piecesHeaderSig = null;   // force a header rebuild to clear the filter inputs
    state.diffusion.piecesPage = 0;
    renderPiecesTable();
}

function updatePiecesPager(total, pages, start, shown) {
    const container = $("diff-pieces-pager");
    if (!container) return;
    container.innerHTML = "";
    const d = state.diffusion;

    const sizeSel = document.createElement("select");
    [100, 200, 500, 1000].forEach((n) => sizeSel.appendChild(option(String(n), n + " / page")));
    sizeSel.value = String(d.piecesPageSize);
    sizeSel.addEventListener("change", () => { d.piecesPageSize = parseInt(sizeSel.value, 10); d.piecesPage = 0; renderPiecesTable(); });

    const prev = document.createElement("button");
    prev.textContent = "‹ Prev";
    prev.disabled = d.piecesPage <= 0;
    prev.addEventListener("click", () => { d.piecesPage--; renderPiecesTable(); });

    const next = document.createElement("button");
    next.textContent = "Next ›";
    next.disabled = d.piecesPage >= pages - 1;
    next.addEventListener("click", () => { d.piecesPage++; renderPiecesTable(); });

    // Direct page jump.
    const jump = document.createElement("span");
    jump.className = "page-jump";
    const jumpLabel = document.createElement("span");
    jumpLabel.textContent = "Page";
    const pageInput = document.createElement("input");
    pageInput.type = "number";
    pageInput.min = "1";
    pageInput.max = String(pages);
    pageInput.value = String(d.piecesPage + 1);
    pageInput.className = "page-input";
    const goTo = () => {
        let v = parseInt(pageInput.value, 10);
        if (isNaN(v)) v = d.piecesPage + 1;
        v = Math.max(1, Math.min(pages, v));
        d.piecesPage = v - 1;
        renderPiecesTable();
    };
    pageInput.addEventListener("change", goTo);
    pageInput.addEventListener("keydown", (e) => { if (e.key === "Enter") goTo(); });
    const ofLabel = document.createElement("span");
    ofLabel.textContent = "of " + pages;
    jump.append(jumpLabel, pageInput, ofLabel);

    const info = document.createElement("span");
    info.className = "pageinfo";
    info.textContent = (total ? start + 1 : 0) + "–" + (start + shown) + " of " + total;

    container.append(sizeSel, prev, next, jump, info);
}

function pieceRow(p, i) {
    const tr = document.createElement("tr");
    tr.appendChild(pieceCell(i, "id", "text", p.id));
    tr.appendChild(pieceCell(i, "creator", "text", p.creator));
    tr.appendChild(pieceCell(i, "timestamp", "number", p.timestamp));
    state.diffusion.featureParams.forEach((param) => tr.appendChild(featureValueCell(i, param, p)));
    const td = document.createElement("td");
    const del = document.createElement("button");
    del.className = "piece-del";
    del.textContent = "✕";
    del.title = "Delete piece";
    del.addEventListener("click", () => {
        if (!confirmPiecesChange()) return;
        state.diffusion.pieces.splice(i, 1);
        renderPiecesTable();
        persistPiecesNow();
    });
    td.appendChild(del);
    tr.appendChild(td);
    return tr;
}

function pieceCell(i, field, type, value) {
    const td = document.createElement("td");
    const inp = document.createElement("input");
    inp.type = type;
    if (type === "number") inp.step = "1";
    inp.value = value == null ? "" : value;
    inp.addEventListener("input", () => {
        if (!confirmPiecesChange()) { inp.value = state.diffusion.pieces[i][field] ?? ""; return; }   // revert on cancel
        state.diffusion.pieces[i][field] = (type === "number") ? (parseInt(inp.value, 10) || 0) : inp.value;
        schedulePersistPieces();
    });
    td.appendChild(inp);
    // The creator is a node of the graph: turn its cell into a searchable node selector (picking fires "change").
    if (field === "creator") {
        attachNodeCombo(inp);
        inp.addEventListener("change", () => {
            if (!confirmPiecesChange()) { inp.value = state.diffusion.pieces[i][field] ?? ""; return; }
            state.diffusion.pieces[i][field] = inp.value;
            schedulePersistPieces();
        });
    }
    return td;
}

// Ensures every feature parameter present in the pieces has a column (columns are additive: explicitly-added or
// previously-seen parameters remain even when no piece currently carries a value).
function syncFeatureParams() {
    const d = state.diffusion;
    if (!d.featureParams) d.featureParams = [];
    const have = new Set(d.featureParams);
    d.pieces.forEach((p) => (p.features || []).forEach((f) => {
        if (f.param && !have.has(f.param)) { have.add(f.param); d.featureParams.push(f.param); }
    }));
}

// A feature-column header: the parameter name plus a control to drop the whole column.
function featureColumnHeader(param) {
    const th = document.createElement("th");
    th.className = "feature-col-th";
    const label = document.createElement("span");
    label.textContent = param;
    const del = document.createElement("button");
    del.className = "feature-col-del";
    del.textContent = "✕";
    del.title = 'Remove the "' + param + '" feature column';
    del.addEventListener("click", () => removeFeatureColumn(param));
    th.append(label, del);
    return th;
}

// One cell for (piece i, feature parameter): the piece's value(s) for that parameter, editable.
function featureValueCell(i, param, p) {
    const td = document.createElement("td");
    td.className = "piece-feature-cell";
    const inp = document.createElement("input");
    inp.type = "text";
    inp.placeholder = "value:weight; …";
    inp.value = encodeParamValues(p.features, param);
    inp.addEventListener("input", () => {
        if (!confirmPiecesChange()) { inp.value = encodeParamValues(state.diffusion.pieces[i].features, param); return; }
        setPieceParamValues(i, param, inp.value);
        schedulePersistPieces();
    });
    td.appendChild(inp);
    return td;
}

// Prompts for a new feature-parameter name and adds an (initially empty) column for it.
function addFeatureColumn() {
    const name = (window.prompt("New feature name:") || "").trim();
    if (!name) return;
    if (!state.diffusion.featureParams.includes(name)) state.diffusion.featureParams.push(name);
    renderPiecesTable();
}

// Removes a feature column: drops that parameter's values from every piece and the column itself.
function removeFeatureColumn(param) {
    if (!confirmPiecesChange()) return;
    state.diffusion.pieces.forEach((p) => { if (p.features) p.features = p.features.filter((f) => f.param !== param); });
    state.diffusion.featureParams = state.diffusion.featureParams.filter((x) => x !== param);
    delete state.diffusion.pieceFilters["feat:" + param];   // drop its filter, if any
    renderPiecesTable();
    persistPiecesNow();
}

// Encodes one parameter's values for a piece as "value:weight" entries (":weight" omitted when 1), separated by "; ".
function encodeParamValues(features, param) {
    const vals = (features || []).filter((f) => f.param === param);
    if (!vals.length) return "";
    return vals.map((f) => f.value + (f.weight != null && Number(f.weight) !== 1 ? ":" + f.weight : "")).join("; ");
}

// Replaces the values of one parameter on a piece with those parsed from a cell ("value:weight; value2; …").
function setPieceParamValues(i, param, text) {
    const p = state.diffusion.pieces[i];
    p.features = (p.features || []).filter((f) => f.param !== param);
    (text || "").split(/[;,\n]+/).forEach((tokRaw) => {
        const tok = tokRaw.trim();
        if (!tok) return;
        let value = tok, weight = 1;
        const colon = tok.lastIndexOf(":");
        if (colon >= 0) {
            const w = parseFloat(tok.slice(colon + 1));
            if (!isNaN(w)) { weight = w; value = tok.slice(0, colon).trim(); }
        }
        if (value) p.features.push({ param, value, weight });
    });
}

// Encodes a piece's feature list as "param=value" entries (with ":weight" when not 1), separated by "; ".
function encodePieceFeatures(features) {
    if (!features || !features.length) return "";
    return features.map((f) => f.param + "=" + f.value + (f.weight != null && Number(f.weight) !== 1 ? ":" + f.weight : "")).join("; ");
}

// Parses "param=value:weight; …" into a list of {param, value, weight} (weight defaults to 1).
function parsePieceFeatures(text) {
    const out = [];
    (text || "").split(/[;\n]+/).forEach((tokRaw) => {
        const tok = tokRaw.trim();
        if (!tok) return;
        const eq = tok.indexOf("=");
        if (eq < 0) return;
        const param = tok.slice(0, eq).trim();
        let rest = tok.slice(eq + 1).trim();
        let weight = 1;
        const colon = rest.lastIndexOf(":");
        if (colon >= 0) {
            const w = parseFloat(rest.slice(colon + 1));
            if (!isNaN(w)) { weight = w; rest = rest.slice(0, colon).trim(); }
        }
        if (!param || !rest) return;
        out.push({ param, value: rest, weight });
    });
    return out;
}

function addDiffPiece() {
    if (!confirmPiecesChange()) return;
    const d = state.diffusion;
    let n = d.pieces.length + 1;
    const existing = new Set(d.pieces.map((p) => p.id));
    while (existing.has("piece" + n)) n++;
    d.pieces.push({ id: "piece" + n, creator: "", timestamp: 0, features: [] });
    d.piecesPage = Math.floor((d.pieces.length - 1) / d.piecesPageSize);   // jump to the page holding the new row
    renderPiecesTable();
    schedulePersistPieces();
}

// Prefills the pieces from the graph: every node creates the chosen number of pieces (owned by it), added to the list.
function generatePiecesFromGraph() {
    if (!requireGraph()) return;
    if (!confirmPiecesChange()) return;
    const k = Math.max(1, numInput("diff-piece-seed", 1));
    const pieces = [];
    state.graph.forEachNode((node) => {
        for (let j = 0; j < k; j++) pieces.push({ id: node + "#" + j, creator: node, timestamp: 0, features: [] });
    });
    state.diffusion.pieces = pieces;
    state.diffusion.featureParams = [];
    state.diffusion.pieceFilters = {};
    state.diffusion.piecesHeaderSig = null;
    state.diffusion.piecesPage = 0;
    renderPiecesTable();
    persistPiecesNow();
    setStatus("Generated " + pieces.length + " information piece(s) from the graph.");
}

function clearDiffPieces() {
    if (!state.diffusion.pieces.length) return;
    if (!confirmPiecesChange()) return;
    state.diffusion.pieces = [];
    state.diffusion.featureParams = [];
    state.diffusion.pieceFilters = {};
    state.diffusion.piecesHeaderSig = null;
    state.diffusion.piecesPage = 0;
    renderPiecesTable();
    persistPiecesNow();
}

function uploadPiecesCsv(file) {
    if (!file) return;
    if (!confirmPiecesChange()) return;
    const reader = new FileReader();
    reader.onload = () => {
        state.diffusion.pieces = parsePiecesText(String(reader.result));
        state.diffusion.featureParams = [];   // rebuilt from the loaded pieces
        state.diffusion.pieceFilters = {};
        state.diffusion.piecesHeaderSig = null;
        state.diffusion.piecesPage = 0;
        renderPiecesTable();
        persistPiecesNow();
        setStatus("Loaded " + state.diffusion.pieces.length + " information piece(s) from " + file.name + ".");
    };
    reader.onerror = () => setStatus("Could not read " + file.name + ".", "error");
    reader.readAsText(file);
}

// Uploads a RELISON-style info-features file: lines of "infoId \t featureId" (optional 3rd column = weight). The
// feature parameter name comes from a header row ("infoId \t <name>") when present, otherwise from the name box.
function uploadInfoFeaturesFile(file) {
    if (!file) return;
    if (!state.diffusion.pieces.length) { setStatus("Add or generate information pieces before uploading their features.", "error"); return; }
    if (!confirmPiecesChange()) return;
    const reader = new FileReader();
    reader.onload = () => applyInfoFeaturesText(String(reader.result), file.name);
    reader.onerror = () => setStatus("Could not read " + file.name + ".", "error");
    reader.readAsText(file);
}

function applyInfoFeaturesText(text, filename) {
    const lines = text.split(/\r?\n/).filter((l) => l.trim().length);
    if (!lines.length) { setStatus("The features file is empty.", "error"); return; }

    // Split on tab (fall back to any whitespace if a line has no tab).
    const cells = (line) => (line.includes("\t") ? line.split("\t") : line.split(/\s+/)).map((c) => c.trim());
    let param = ($("diff-feature-param").value || "").trim() || "feature";
    let start = 0;
    const first = cells(lines[0]);
    if (["infoid", "id", "info", "piece"].includes((first[0] || "").toLowerCase())) {
        if (first[1]) param = first[1];   // header names the feature parameter
        start = 1;
    }

    // Collect featureId(s) per infoId.
    const byInfo = new Map();
    for (let i = start; i < lines.length; i++) {
        const c = cells(lines[i]);
        const info = c[0], value = c[1];
        if (!info || !value) continue;
        const weight = (c[2] !== undefined && c[2] !== "") ? (parseFloat(c[2]) || 1) : 1;
        if (!byInfo.has(info)) byInfo.set(info, []);
        byInfo.get(info).push({ value, weight });
    }

    // Replace any existing values of this parameter, then apply the uploaded ones to the matching pieces.
    const pieceById = new Map(state.diffusion.pieces.map((p) => [p.id, p]));
    state.diffusion.pieces.forEach((p) => { p.features = (p.features || []).filter((f) => f.param !== param); });
    let applied = 0, unmatched = 0;
    byInfo.forEach((entries, info) => {
        const p = pieceById.get(info);
        if (!p) { unmatched++; return; }
        entries.forEach((e) => p.features.push({ param, value: e.value, weight: e.weight }));
        applied++;
    });

    renderPiecesTable();
    persistPiecesNow();
    setStatus("Loaded \"" + param + "\" features for " + applied + " piece(s)" +
        (unmatched ? " (" + unmatched + " unknown info id(s) skipped)" : "") + " from " + filename + ".");
}

// Parses an information-pieces file. Columns: id, creator, timestamp, features. Tab-separated (so the features
// field can freely use ",", ";", "=", ":"); a comma-only line (no tab) is still accepted for legacy 3-column files.
// A leading header row (first cell "id") is skipped.
function parsePiecesText(text) {
    const lines = text.split(/\r?\n/).filter((l) => l.trim().length);
    const out = [];
    lines.forEach((line, idx) => {
        const cols = (line.includes("\t") ? line.split("\t") : line.split(",")).map((c) => c.trim());
        if (idx === 0 && cols[0].toLowerCase() === "id") return;   // header
        if (!cols[0]) return;
        out.push({
            id: cols[0],
            creator: cols[1] || "",
            timestamp: (cols[2] !== undefined && cols[2] !== "") ? (parseInt(cols[2], 10) || 0) : 0,
            features: parsePieceFeatures(cols[3] || ""),
        });
    });
    return out;
}

function downloadPiecesCsv() {
    const pieces = state.diffusion.pieces;
    if (!pieces.length) { setStatus("No information pieces to download.", "error"); return; }
    // Tab-separated so the features field can contain commas/semicolons/colons.
    const lines = [["id", "creator", "timestamp", "features"].join("\t")];
    for (const p of pieces) lines.push([p.id, p.creator, p.timestamp, encodePieceFeatures(p.features)].join("\t"));
    download("information-pieces.tsv", lines.join("\n"), "text/tab-separated-values");
}

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
    container.innerHTML = "";
    const runs = state.diffusion.runs;
    if (!runs.length) {
        $("diffmetrics-hint").textContent = "Run a simulation (with metrics selected) to see plots over iterations.";
        return;
    }
    $("diffmetrics-hint").textContent = "";
    const sel = $("diffmetric-select");
    const union = diffMetricUnion();
    const chosen = union.find((m) => m.id === sel.value) || union[0];
    if (!chosen) return;

    // One line per run that computed the chosen metric, coloured by run and labelled with its protocol.
    const series = [];
    runs.forEach((run, idx) => {
        const m = run.metrics.find((x) => x.id === chosen.id);
        if (m) series.push({ name: run.label, values: m.values, color: SERIES_COLORS[idx % SERIES_COLORS.length] });
    });
    if (!series.length) return;

    const title = document.createElement("h2");
    title.textContent = chosen.label;
    const area = document.createElement("div");
    area.className = "chart-area";
    area.style.height = "320px";
    const canvas = document.createElement("canvas");
    area.appendChild(canvas);
    container.appendChild(title);
    container.appendChild(area);
    drawMultiLineChart(canvas, series, chosen.label);
}

// Draws one or more metric series (each {name, values, color}) as lines over iteration index, with an
// "iteration" x axis (labelled ticks), the metric name as the y-axis title, and a legend of series names.
function drawMultiLineChart(canvas, series, label) {
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    const W = Math.max(1, Math.floor(rect.width)), H = Math.max(1, Math.floor(rect.height));
    canvas.width = W * dpr; canvas.height = H * dpr;
    const ctx = canvas.getContext("2d");
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);

    // Overall value range and iteration count across all series.
    let min = Infinity, max = -Infinity, n = 0;
    for (const s of series) {
        n = Math.max(n, (s.values || []).length);
        for (const v of s.values || []) if (Number.isFinite(v)) { if (v < min) min = v; if (v > max) max = v; }
    }
    if (!Number.isFinite(min) || !Number.isFinite(max)) return;
    if (max === min) { max = min + 1; min = min - 1; }

    const padL = 66, padR = 16, padT = 16, padB = 50;
    const plotW = W - padL - padR, plotH = H - padT - padB;
    const xOf = (i) => padL + (n <= 1 ? plotW / 2 : (i / (n - 1)) * plotW);
    const yOf = (v) => padT + plotH - ((v - min) / (max - min)) * plotH;

    const colText = cssVar("--text", "#e6e6e6"), colMuted = cssVar("--muted", "#9aa0a6");
    const colBorder = cssVar("--border", "#333");

    // Axis lines + y grid/labels.
    ctx.strokeStyle = colMuted;
    ctx.beginPath(); ctx.moveTo(padL, padT); ctx.lineTo(padL, padT + plotH); ctx.lineTo(padL + plotW, padT + plotH); ctx.stroke();
    ctx.font = AXIS_LABEL_FONT; ctx.fillStyle = colMuted; ctx.textAlign = "right"; ctx.textBaseline = "middle";
    for (let gIdx = 0; gIdx <= 4; gIdx++) {
        const val = min + ((max - min) * gIdx) / 4, y = yOf(val);
        ctx.fillStyle = colMuted; ctx.fillText(fmt(val), padL - 6, y);
        ctx.strokeStyle = colBorder; ctx.beginPath(); ctx.moveTo(padL, y); ctx.lineTo(padL + plotW, y); ctx.stroke();
    }
    // x tick labels (iteration indices, thinned so they don't overlap).
    ctx.textAlign = "center"; ctx.textBaseline = "alphabetic"; ctx.fillStyle = colMuted;
    const step = Math.max(1, Math.ceil(n / 8));
    for (let i = 0; i < n; i += step) ctx.fillText(String(i), xOf(i), padT + plotH + 15);
    if (n > 1 && (n - 1) % step !== 0) ctx.fillText(String(n - 1), xOf(n - 1), padT + plotH + 15);

    // Axis titles.
    ctx.fillStyle = colText; ctx.font = AXIS_TITLE_FONT; ctx.textAlign = "center";
    ctx.fillText("iteration", padL + plotW / 2, H - 6);
    ctx.save(); ctx.translate(14, padT + plotH / 2); ctx.rotate(-Math.PI / 2); ctx.fillText(label || "value", 0, 0); ctx.restore();
    ctx.textAlign = "start"; ctx.textBaseline = "alphabetic";

    // Series lines.
    for (const s of series) {
        ctx.strokeStyle = s.color || cssVar("--accent", "#4f9dff"); ctx.lineWidth = 1.8; ctx.beginPath();
        let started = false;
        const vals = s.values || [];
        for (let i = 0; i < vals.length; i++) {
            const v = vals[i];
            if (!Number.isFinite(v)) { started = false; continue; }
            const x = xOf(i), y = yOf(v);
            if (!started) { ctx.moveTo(x, y); started = true; } else ctx.lineTo(x, y);
        }
        ctx.stroke();
    }
    ctx.lineWidth = 1;

    // Legend (top-right): one swatch + protocol label per series.
    ctx.font = AXIS_LABEL_FONT; ctx.textBaseline = "middle";
    const lh = 15, sw = 12, gap = 5;
    let widest = 0;
    for (const s of series) widest = Math.max(widest, ctx.measureText(s.name).width);
    const boxW = sw + gap + widest + 12, boxH = series.length * lh + 8;
    const bx = padL + plotW - boxW, by = padT + 4;
    ctx.fillStyle = cssVar("--panel", "#26272b"); ctx.globalAlpha = 0.9;
    ctx.fillRect(bx, by, boxW, boxH);
    ctx.globalAlpha = 1; ctx.strokeStyle = colBorder; ctx.strokeRect(bx, by, boxW, boxH);
    series.forEach((s, i) => {
        const cy = by + 4 + i * lh + lh / 2;
        ctx.fillStyle = s.color; ctx.fillRect(bx + 6, cy - sw / 2, sw, sw);
        ctx.fillStyle = colText; ctx.textAlign = "start"; ctx.fillText(s.name, bx + 6 + sw + gap, cy);
    });
    ctx.textBaseline = "alphabetic";
}

/* ------------------------------- wiring ----------------------------- */

$("btn-load").addEventListener("click", loadGraph);
$("btn-layout").addEventListener("click", onLayoutButton);
$("btn-noverlap").addEventListener("click", removeOverlaps);
$("btn-reset-layout").addEventListener("click", resetLayout);
$("layout-type").addEventListener("change", () => { if (state.fa2Running) stopLayout(); updateLayoutButton(); updateLayoutTypeUI(); });
$("drag-nodes").addEventListener("change", (e) => { state.dragNodes = e.target.checked; });
updateLayoutButton();
updateLayoutTypeUI();

// Make the left/right panel blocks collapsible by clicking their headings.
document.querySelectorAll(".panel .block > h2").forEach((h) => {
    h.addEventListener("click", () => h.parentElement.classList.toggle("collapsed"));
});

// Diffusion tab controls.
$("btn-diff-run").addEventListener("click", runDiffusion);
$("btn-diff-clear").addEventListener("click", clearDiffusion);
$("diff-protocol-type").addEventListener("change", onDiffProtocolType);
$("diff-protocol").addEventListener("change", (e) => renderParams("diff-protocol-params", "protocol", e.target.value));
$("diff-selection").addEventListener("change", (e) => renderParams("diff-selection-params", "selection", e.target.value));
$("diff-expiration").addEventListener("change", (e) => renderParams("diff-expiration-params", "expiration", e.target.value));
$("diff-propagation").addEventListener("change", (e) => renderParams("diff-propagation-params", "propagation", e.target.value));
$("diff-update").addEventListener("change", (e) => renderParams("diff-update-params", "update", e.target.value));
$("diff-sight").addEventListener("change", (e) => renderParams("diff-sight-params", "sight", e.target.value));
$("diff-stop").addEventListener("change", (e) => renderParams("diff-stop-params", "stop", e.target.value));
$("diff-slider").addEventListener("input", (e) => { diffStop(); setDiffIteration(parseInt(e.target.value, 10) || 0); });
$("diff-step-back").addEventListener("click", () => { diffStop(); setDiffIteration(state.diffusion.iteration - 1); });
$("diff-step-fwd").addEventListener("click", () => { diffStop(); setDiffIteration(state.diffusion.iteration + 1); });
$("diff-play").addEventListener("click", diffPlay);
$("diff-node-input").addEventListener("change", () => {
    const v = $("diff-node-input").value.trim();
    if (v && state.diffusion.graph && state.diffusion.graph.hasNode(v)) selectDiffNode(v);
});
$("diffmetric-select").addEventListener("change", renderDiffMetrics);
// Information-pieces subtab controls.
$("btn-diff-piece-add").addEventListener("click", addDiffPiece);
$("btn-diff-piece-gen").addEventListener("click", generatePiecesFromGraph);
$("btn-diff-piece-clear").addEventListener("click", clearDiffPieces);
$("btn-diff-piece-download").addEventListener("click", downloadPiecesCsv);
$("btn-diff-piece-upload").addEventListener("click", () => $("diff-piece-file").click());
$("diff-piece-file").addEventListener("change", (e) => { uploadPiecesCsv(e.target.files[0]); e.target.value = ""; });
$("btn-diff-feature-upload").addEventListener("click", () => $("diff-feature-file").click());
$("diff-feature-file").addEventListener("change", (e) => { uploadInfoFeaturesFile(e.target.files[0]); e.target.value = ""; });
$("btn-diff-feature-add").addEventListener("click", addFeatureColumn);
$("btn-diff-piece-clear-filters").addEventListener("click", clearPieceFilters);
$("diff-apply-filters").addEventListener("change", (e) => { state.diffusion.applyFiltersToSim = e.target.checked; });
updateDiffRunEnabled();   // starts disabled until at least one piece exists
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
["node-size-min", "node-size-max", "edge-size-min", "edge-size-max"].forEach((id) => $(id).addEventListener("input", applyAppearance));

// Node borders (drawn on the display overlay).
$("node-border-on").addEventListener("change", (e) => {
    state.nodeBorder.on = e.target.checked;
    toggleHidden("node-border-params", !e.target.checked);
    drawRecOverlay();
});
$("node-border-color").addEventListener("input", (e) => { state.nodeBorder.color = e.target.value; drawRecOverlay(); });
$("node-border-width").addEventListener("input", () => { state.nodeBorder.width = numInput("node-border-width", 1.5); drawRecOverlay(); });
["node-label-show", "node-label-size", "node-label-prop", "node-label-color", "node-label-font",
 "edge-label-show", "edge-label-size", "edge-label-prop", "edge-label-color", "edge-label-font"]
    .forEach((id) => $(id).addEventListener("input", syncLabelOpts));
$("btn-zoom-in").addEventListener("click", zoomIn);
$("btn-zoom-out").addEventListener("click", zoomOut);
$("btn-zoom-fit").addEventListener("click", zoomFit);
$("btn-vertex").addEventListener("click", runVertexMetric);
$("btn-graph").addEventListener("click", runGraphMetric);
$("btn-pair").addEventListener("click", runPairMetric);
$("btn-community").addEventListener("click", detectCommunity);
$("btn-global-comm").addEventListener("click", runGlobalCommMetric);
$("btn-indiv-comm").addEventListener("click", runIndividualCommMetric);

$("vertex-metric").addEventListener("change", (e) => renderParams("vertex-params", "vertex", e.target.value));
$("graph-metric").addEventListener("change", (e) => renderParams("graph-params", "graph", e.target.value));
$("pair-metric").addEventListener("change", (e) => renderParams("pair-params", "pair", e.target.value));
$("community-algo").addEventListener("change", (e) => renderParams("community-params", "community", e.target.value));
$("indiv-comm-metric").addEventListener("change", (e) => renderParams("indiv-comm-params", "communityIndividual", e.target.value));
$("global-comm-metric").addEventListener("change", (e) => renderParams("global-comm-params", "communityGlobal", e.target.value));
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
document.querySelectorAll(".diffsubtab").forEach((b) => b.addEventListener("click", () => switchDiffSubtab(b.dataset.diffsubtab)));

// Structural metric chart download buttons (delegated by data-canvas / data-file).
document.querySelectorAll(".chart-dl[data-canvas]").forEach((b) =>
    b.addEventListener("click", () => downloadChartPng(b.dataset.canvas, b.dataset.file)));
// Diffusion metric chart: the canvas is created dynamically inside #diffmetrics-charts.
$("btn-diffmetric-png").addEventListener("click", () => {
    const canvas = $("diffmetrics-charts").querySelector("canvas");
    const sel = $("diffmetric-select");
    const name = (sel && sel.value ? sel.value : "diffusion-metric").replace(/[^\w.-]+/g, "_");
    downloadChartPng(canvas, name + ".png");
});
$("btn-diffmetric-csv").addEventListener("click", downloadDiffMetricsCsv);

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
$("btn-add-node-attr").addEventListener("click", () => addAttrColumn("node"));
$("btn-add-edge-attr").addEventListener("click", () => addAttrColumn("edge"));

// Toolbar "Add …" / "Add column" toggles (reveal the second row).
$("btn-node-add-toggle").addEventListener("click", () => toggleAddGroup("nodes", "main"));
$("btn-node-col-toggle").addEventListener("click", () => toggleAddGroup("nodes", "col"));
$("btn-edge-add-toggle").addEventListener("click", () => toggleAddGroup("edges", "main"));
$("btn-edge-col-toggle").addEventListener("click", () => toggleAddGroup("edges", "col"));

// Import CSV from the table toolbars (reuses the attribute upload).
$("btn-import-nodes-csv").addEventListener("click", () => $("node-csv-file").click());
$("btn-import-edges-csv").addEventListener("click", () => $("edge-csv-file").click());
$("node-csv-file").addEventListener("change", (e) => { uploadAttributeFile("nodes", e.target.files[0]); e.target.value = ""; });
$("edge-csv-file").addEventListener("change", (e) => { uploadAttributeFile("edges", e.target.files[0]); e.target.value = ""; });

$("btn-clear-nodes-filters").addEventListener("click", () => clearFilters("nodes"));
$("btn-clear-edges-filters").addEventListener("click", () => clearFilters("edges"));

["node-chart-metric", "node-chart-sort"].forEach((id) => $(id).addEventListener("change", drawNodeChart));
["edge-chart-metric", "edge-chart-sort"].forEach((id) => $(id).addEventListener("change", drawEdgeChart));
$("pair-chart-metric").addEventListener("change", drawPairChart);
["comm-chart-metric", "comm-chart-sort"].forEach((id) => $(id).addEventListener("change", drawCommChart));

// Recommendation tab + overlay controls.
$("btn-rec-apply").addEventListener("click", applyRecommendation);
$("btn-rec-reset").addEventListener("click", resetRecommendationResults);
$("rec-mode").addEventListener("change", onRecModeChange);
$("rec-algo").addEventListener("change", (e) => renderParams("rec-params", "recommendation", e.target.value));
$("btn-clear-rec-filters").addEventListener("click", () => clearFilters("rec"));
$("rec-edge-show").addEventListener("change", (e) => { state.rec.show = e.target.checked; drawRecOverlay(); });
$("rec-edge-diff").addEventListener("change", (e) => { state.rec.diff = e.target.checked; applyReducers(); drawRecOverlay(); });
$("rec-edge-color").addEventListener("input", (e) => { state.rec.color = e.target.value; applyAppearance(); drawRecOverlay(); });

// Scatter selectors: changing the metric reloads the recommendation list, both redraw.
$("node-scatter-metric").addEventListener("change", () => { syncScatterSelectors("node", "vertex", state.metricOrder); drawNodeScatter(); });
$("node-scatter-rec").addEventListener("change", drawNodeScatter);
$("edge-scatter-metric").addEventListener("change", () => { syncScatterSelectors("edge", "pair", state.pairOrder); drawEdgeScatter(); });
$("edge-scatter-rec").addEventListener("change", drawEdgeScatter);
$("comm-scatter-metric").addEventListener("change", () => { syncScatterSelectors("comm", "comm", state.commMetricOrder); drawCommScatter(); });
$("comm-scatter-rec").addEventListener("change", drawCommScatter);

window.addEventListener("resize", () => {
    if (state.activeTab === "network") { scheduleRecOverlay(); return; }
    if (state.activeTab === "diffusion") {
        if (state.diffusion.subview === "metrics") { renderDiffMetrics(); return; }
        if (state.diffusion.renderer) state.diffusion.renderer.refresh();
        drawDiffOverlay();
        return;
    }
    if (state.activeTab !== "metrics") return;
    if (state.activeSubtab === "nodes") { drawNodeChart(); drawNodeScatter(); }
    if (state.activeSubtab === "edges") { drawEdgeChart(); drawEdgeScatter(); }
    if (state.activeSubtab === "pairs") drawPairChart();
    if (state.activeSubtab === "comm") { drawCommChart(); drawCommScatter(); }
});

document.addEventListener("keydown", (e) => {
    if (!$("edit-mode").checked) return;
    if ((e.key === "Delete" || e.key === "Backspace") && state.selectedNode != null) {
        const node = state.selectedNode;
        if (!window.confirm('Delete node "' + node + '" and its edges?' + pendingClearsSuffix())) return;
        clearSelection();
        editDeleteNode(node);
    }
});

switchTab(state.activeTab); // start on the Import tab with the side panels hidden
loadCatalog();
