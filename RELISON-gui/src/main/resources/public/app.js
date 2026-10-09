"use strict";

/* ------------------------------------------------------------------ *
 * Compatibility shim – bridges old UI expectations to the current
 * graphology‑library@0.8.0 and sigma@3.0.3 APIs.
 * ------------------------------------------------------------------ */
(() => {
  // ----- graphology‑library -------------------------------------------------
  const lib = window.graphologyLibrary || {};

  // `blendFunc` used to be a function that returned a blending mode.
  // In sigma 3.x the renderer accepts a boolean `blendEdges` (default true).
  // Provide a no‑op that simply returns `true` so the UI never crashes.
  if (typeof lib.blendFunc !== "function") {
    lib.blendFunc = () => true;
  }

  // `getHashes` was moved to `graphology-utils` / `object‑hash`.
  // Fall back to the newer `objectHash` export if it exists,
  // otherwise return an empty array (the UI only iterates over it).
  if (typeof lib.getHashes !== "function") {
    lib.getHashes = (obj) => {
      if (window.graphologyLibrary && window.graphologyLibrary.objectHash) {
        return window.graphologyLibrary.objectHash(obj);
      }
      // Very simple fallback – returns an empty array so the UI does not
      // throw “undefined is not a function”.
      return [];
    };
  }

})();

/* Tooltip copy lives only in tooltips.json; markup and scripts use keys. */
let TOOLTIP_TEXT = null;

function applyTooltipKey(element) {
  const key = element && element.dataset ? element.dataset.tooltip : "";
  if (!key || !TOOLTIP_TEXT || typeof TOOLTIP_TEXT[key] !== "string") return;
  const template = TOOLTIP_TEXT[key];
  const text = template.replace(/\{([a-zA-Z0-9_]+)\}/g, (_, name) => {
    const value = element.dataset["tooltip" + name.charAt(0).toUpperCase() + name.slice(1)];
    return value == null ? "" : value;
  });
  element.title = text;
  element.setAttribute("aria-description", text);
}

function setTooltip(element, key, values) {
  if (!element) return;
  element.dataset.tooltip = key;
  Object.entries(values || {}).forEach(([name, value]) => {
    element.dataset["tooltip" + name.charAt(0).toUpperCase() + name.slice(1)] = String(value);
  });
  applyTooltipKey(element);
}

async function loadTooltipText() {
  try {
    const response = await fetch(new URL("tooltips.json", document.baseURI), { cache: "no-cache" });
    if (!response.ok) throw new Error("HTTP " + response.status);
    const config = await response.json();
    TOOLTIP_TEXT = config.tooltips || {};
    const applyTo = (root) => {
      if (!root || root.nodeType !== Node.ELEMENT_NODE) return;
      applyTooltipKey(root);
      root.querySelectorAll("[data-tooltip]").forEach(applyTooltipKey);
    };

    applyTo(document.body);
    new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.type === "childList") mutation.addedNodes.forEach(applyTo);
        else applyTooltipKey(mutation.target);
      });
    }).observe(document.body, {
      subtree: true,
      childList: true,
      attributes: true,
      attributeFilter: ["data-tooltip"]
    });
  } catch (error) {
    console.warn("Could not load tooltip text from tooltips.json.", error);
  }
}

/* ------------------------------------------------------------------ *
 * RELISON GUI frontend.
 *
 * Resolves the UMD globals defensively (their exact names vary between
 * builds), then wires up loading, rendering, layout, interaction, the
 * node/edge tables and the computed-metrics dashboards.
 * ------------------------------------------------------------------ */

const Graph = (window.graphology && window.graphology.Graph) || window.graphology;
const lib = window.graphologyLibrary || {};
const Sigma = window.Sigma;

/* ------------------------------- state ------------------------------ */

const state = {
    graphId: null,
    graph: null,          // graphology Graph instance
    renderer: null,       // Sigma renderer
    directed: true,
    weighted: false,
    stats: null,          // server-reported graph stats ({nodes, edges, directed, weighted}); used by the report export
    reportPlots: new Map(),  // every chart drawn this session, keyed for de-dup: key -> {section, title, url}; for the report
    iterativeLayoutRunning: false,
    iterativeLayoutRaf: null,
    layoutTemp: 50,
    layoutRequest: null,
    relisonLayout: null,
    savedLayoutPositions: null,
    dragNodes: false,     // when on, dragging a node repositions it instead of panning the canvas
    multiNodeSelect: false,
    areaSelectingNodes: false,
    multiSelectedNodes: new Set(),
    cosmographLayoutRunning: false,
    cosmographDirty: true,
    cosmographTimelineActive: false,
    cosmographFocusActive: false,
    cosmographPathActive: false,
    cosmographPathSignature: "",
    cosmographLabelDefaultsApplied: false,
    selectedNode: null,
    selectedEdge: null,
    edgeSelectionPair: null,
    cosmographEdgeStart: null,
    editFirstNode: null,
    selection: { type: "node", node: null, mode: "highlight", partition: null, edgeAttribute: null },
    pathFocus: null,   // { nodes: Set, edges: Set("s|t") } focused from the right-panel path selector
    pathEdgeHighlight: { mode: "default", color: "#ff9f43" },
    pathAppearance: { enlargeNodes: false, nodeScale: 1.5, edgeScale: 1 },
    pathEdgeStyles: new Map(),
    pathEdgeStyleSequence: 0,
    pathEndpointFocusActive: true,
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
    attributeMetricData: {}, // per-attribute-value: label -> { attribute, values, valueLabels }
    attributeMetricOrder: [], // per-attribute-value metric labels, in computation order
    edgeAttributeMetricData: {},
    edgeAttributeMetricOrder: [],

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
        subview: "pieces", // active center subtab: "pieces" | "graph" | "stats" | "metrics"
        statsView: "node", // active view inside the "Statistics" subtab: "node" | "piece" | "feat" | "dist"
        pieces: [],        // uploaded/edited information pieces: [{ id, creator, timestamp, features:[{param,value,weight}] }]
        featureParams: [], // ordered feature-parameter names, one table column each
        pieceFilters: {},  // per-column filters for the pieces table: colKey -> { op, value }
        piecesHeaderSig: null,   // signature of the current column set (to rebuild the header only when it changes)
        applyFiltersToSim: false,// when true, only pieces passing the table filters are used in the simulation
        piecesPage: 0,     // current page of the (paginated) pieces table
        piecesPageSize: 200,
        pieceSort: { col: "id", dir: 1 },   // current sort of the pieces table (col key + direction)
        piecesSaved: true, // whether the current pieces are persisted on the session
        piecesMode: "pieces",    // inner toggle of the "Information pieces" subtab: "pieces" | "realprop"
        realPropagated: [],// "real propagated" records: [{ user, piece, timestamp }] (which pieces each user repropagated in reality)
        rpFilters: {},     // per-column filters for the real-propagated table
        rpHeaderSig: null, // signature of the real-propagated column set
        rpPage: 0,
        rpPageSize: 200,
        rpSort: { col: "user", dir: 1 },    // current sort of the real-propagated table
        result: null,      // latest run: { numIterations, iterations:[{propagating,newlyInformed}], metrics:[{id,label,values}] }
        runs: [],          // accumulated runs for overlaid metric plots: [{ label, numIterations, metrics:[{id,label,values}] }]
        iteration: 0,
        graph: null,       // graphology copy rendered in the diffusion canvas
        renderer: null,    // its sigma renderer
        cosmograph: null,  // independent Cosmograph instance for diffusion
        rendererType: "sigma",
        rendererPreference: "network",
        selectedNode: null,
        hoverNode: null,   // node currently hovered on the diffusion canvas (redrawn on top of the spread edges)
        playing: null,     // setInterval handle when playing
        lastState: null,   // last /state response (re-rendered when the feature selector changes)
        traj: null,        // last /trajectory response for the Node timeline subtab
        ptraj: null,       // last /piece-trajectory response for the Piece timeline subtab
        selectedPiece: null,
        ftraj: null,       // last /feature-trajectory response for the Feature timeline subtab
        dist: null,        // last /distribution response for the Distributions subtab
        recKey: null,      // recommendation the current result was run over (null = base graph); its edges are overlaid
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
    // Graph timeline: scrub the network through time using a node/edge attribute of type "time". A node/edge is shown
    // at timestamp t only if its time value covers t (an empty/unset value never shows).
    timeline: { nodeAttr: "", edgeAttr: "", mode: "instant", t: null, rangeStart: null, min: null, max: null, events: [], playing: null },
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

/* ------------------------------ timeline ---------------------------- */

// Parses a canonical time value ("5,10-20,30") into a list of inclusive [lo, hi] ranges (empty/unset → []).
function parseTimeRanges(str) {
    if (str === undefined || str === null) return [];
    const out = [];
    String(str).split(",").forEach((tok) => {
        const t = tok.trim();
        if (!t) return;
        const dash = t.indexOf("-");
        if (dash < 0) { const v = Number(t); if (Number.isFinite(v)) out.push([v, v]); }
        else {
            const lo = Number(t.slice(0, dash).trim()), hi = Number(t.slice(dash + 1).trim());
            if (Number.isFinite(lo) && Number.isFinite(hi)) out.push([Math.min(lo, hi), Math.max(lo, hi)]);
        }
    });
    return out;
}

// Whether a time value covers timestamp t.
function timeContains(str, t) {
    for (const [lo, hi] of parseTimeRanges(str)) if (t >= lo && t <= hi) return true;
    return false;
}

// Whether a time value overlaps an inclusive exploration range.
function timeOverlaps(str, start, end) {
    const lo = Math.min(start, end), hi = Math.max(start, end);
    for (const [from, to] of parseTimeRanges(str)) if (from <= hi && to >= lo) return true;
    return false;
}

function timelineMatches(str) {
    const tl = state.timeline;
    return tl.mode === "range" ? timeOverlaps(str, tl.rangeStart ?? tl.t, tl.t) : timeContains(str, tl.t);
}

// Global [min, max] over every value of the selected node/edge time attributes, or null if there is no timestamp.
function timelineBounds() {
    const tl = state.timeline;
    let min = Infinity, max = -Infinity;
    const scan = (str) => parseTimeRanges(str).forEach(([lo, hi]) => { if (lo < min) min = lo; if (hi > max) max = hi; });
    if (state.graph && tl.nodeAttr) state.graph.forEachNode((n) => scan(nodeAttrVal(n, tl.nodeAttr)));
    if (state.graph && tl.edgeAttr) state.graph.forEachEdge((e) => scan(edgeAttrVal(e, tl.edgeAttr)));
    if (!Number.isFinite(min) || !Number.isFinite(max)) return null;
    return { min, max };
}

// Sorted distinct range endpoints of the selected attributes — the timestamps at which the graph changes; used to
// step / animate between meaningful instants rather than by an arbitrary delta over a possibly huge span.
function timelineEvents() {
    const tl = state.timeline;
    const set = new Set();
    const scan = (str) => parseTimeRanges(str).forEach(([lo, hi]) => { set.add(lo); set.add(hi); });
    if (state.graph && tl.nodeAttr) state.graph.forEachNode((n) => scan(nodeAttrVal(n, tl.nodeAttr)));
    if (state.graph && tl.edgeAttr) state.graph.forEachEdge((e) => scan(edgeAttrVal(e, tl.edgeAttr)));
    return [...set].sort((a, b) => a - b);
}

// Populates the node/edge time-attribute selectors from the schema's "time" attributes; called on every schema change.
function rebuildTimelineOptions() {
    const nodeSel = $("tl-node-attr"), edgeSel = $("tl-edge-attr");
    if (!nodeSel || !edgeSel) return;
    const nodeTime = nodeAttrDefs().filter((d) => d.type === "time").map((d) => d.name);
    const edgeTime = edgeAttrDefs().filter((d) => d.type === "time").map((d) => d.name);
    fillTimeSelect(nodeSel, nodeTime);
    fillTimeSelect(edgeSel, edgeTime);
    if (state.timeline.nodeAttr && !nodeTime.includes(state.timeline.nodeAttr)) state.timeline.nodeAttr = "";
    if (state.timeline.edgeAttr && !edgeTime.includes(state.timeline.edgeAttr)) state.timeline.edgeAttr = "";
    nodeSel.value = state.timeline.nodeAttr;
    edgeSel.value = state.timeline.edgeAttr;
    updateTimelineUI();
}

function fillTimeSelect(sel, names) {
    const prev = sel.value;
    sel.innerHTML = "";
    sel.appendChild(option("", "— none —"));
    names.forEach((n) => sel.appendChild(option(n, n)));
    if (names.includes(prev)) sel.value = prev;
}

// Recomputes the timeline range/events for the current attribute selection, updates the controls, and re-applies the
// visibility filter. Shows the controls only when a time attribute with actual timestamps is selected.
function updateTimelineUI() {
    const tl = state.timeline;
    const on = !!(state.graph && (tl.nodeAttr || tl.edgeAttr));
    const bounds = on ? timelineBounds() : null;
    toggleHidden("tl-controls", !bounds);
    toggleHidden("network-timeline", !bounds);
    if (!bounds) {
        stopTimeline();
        tl.min = tl.max = tl.t = tl.rangeStart = null;
        tl.events = [];
        $("tl-hint").textContent = on
            ? "The selected attribute has no timestamps."
            : "Pick a time attribute to scrub the graph through time. A node/edge shows only at timestamps its value covers; an empty value never shows.";
        applyReducers();
        return;
    }
    $("tl-hint").textContent = "";
    tl.min = bounds.min;
    tl.max = bounds.max;
    tl.events = timelineEvents();
    if (tl.t == null || tl.t < tl.min || tl.t > tl.max) tl.t = tl.min;
    if (tl.rangeStart == null || tl.rangeStart < tl.min || tl.rangeStart > tl.max) tl.rangeStart = tl.min;
    const slider = $("tl-slider"), num = $("tl-time"), rangeStart = $("tl-range-start");
    slider.min = tl.min; slider.max = tl.max; slider.step = 1; slider.value = tl.t;
    num.min = tl.min; num.max = tl.max; num.step = 1; num.value = tl.t;
    rangeStart.min = tl.min; rangeStart.max = tl.max; rangeStart.step = 1; rangeStart.value = tl.rangeStart;
    $("tl-mode").value = tl.mode;
    toggleHidden("tl-range-start-field", tl.mode !== "range");
    updateTimelineReadout();
    applyReducers();
}

function updateTimelineReadout() {
    const tl = state.timeline;
    $("tl-range").textContent = tl.mode === "range"
        ? "range = " + Math.min(tl.rangeStart, tl.t) + " – " + Math.max(tl.rangeStart, tl.t) + "   (domain " + tl.min + " – " + tl.max + ")"
        : "t = " + tl.t + "   (range " + tl.min + " – " + tl.max + ")";
}

// Sets the current timestamp (the range end in range-exploration mode) and re-applies the visibility filter.
function setTimelineT(t) {
    const tl = state.timeline;
    if (tl.min == null) return;
    tl.t = Math.max(tl.min, Math.min(tl.max, Math.round(t)));
    $("tl-slider").value = tl.t;
    $("tl-time").value = tl.t;
    updateTimelineReadout();
    applyReducers();
}

function setTimelineRangeStart(t) {
    const tl = state.timeline;
    if (tl.min == null) return;
    tl.rangeStart = Math.max(tl.min, Math.min(tl.max, Math.round(t)));
    $("tl-range-start").value = tl.rangeStart;
    updateTimelineReadout();
    applyReducers();
}

function setTimelineMode(mode) {
    stopTimeline();
    state.timeline.mode = mode === "range" ? "range" : "instant";
    if (state.timeline.rangeStart == null) state.timeline.rangeStart = state.timeline.t;
    updateTimelineUI();
}
function stopTimeline() {
    if (state.timeline.playing) { clearInterval(state.timeline.playing); state.timeline.playing = null; }
    const p = $("tl-play"); if (p) p.textContent = "▶";
}

// Animates the timeline by advancing through the event timestamps.
function playTimeline() {
    const tl = state.timeline;
    if (tl.playing) { stopTimeline(); return; }
    if (tl.min == null) return;
    if (tl.t >= tl.max) setTimelineT(tl.min);
    $("tl-play").textContent = "⏸";
    tl.playing = setInterval(() => {
        const next = tl.events.find((e) => e > tl.t);
        if (next == null) { stopTimeline(); return; }
        setTimelineT(next);
    }, 700);
}

function stepTimeline(dir) {
    const tl = state.timeline;
    if (tl.min == null) return;
    stopTimeline();
    if (dir > 0) {
        const n = tl.events.find((e) => e > tl.t);
        if (n != null) setTimelineT(n);
    } else {
        let prev = null;
        for (const e of tl.events) { if (e < tl.t) prev = e; else break; }
        if (prev != null) setTimelineT(prev);
    }
}

/* --------------------- canvas animation export (GIF / WebM) --------------------- */
// A generic recorder for the animated sigma canvases (the graph timeline and the diffusion playback). A "spec"
// describes one animation: its renderer, the container/overlay canvases to composite, an ordered list of frame args,
// a setFrame(arg) that advances the animation (and triggers a render), and a restore() to put things back afterwards.

const FRAME_DELAY_MS = 600;   // GIF per-frame delay / WebM per-frame hold

function sleep(ms) { return new Promise((r) => setTimeout(r, ms)); }

// gif.js runs its LZW encoder in a Web Worker; a cross-origin worker URL is blocked, so we fetch the CDN worker once
// and hand gif.js a same-origin blob URL for it.
let _gifWorkerUrl = null;
async function gifWorkerUrl() {
    if (_gifWorkerUrl) return _gifWorkerUrl;
    const resp = await fetch("https://cdn.jsdelivr.net/npm/gif.js@0.2.0/dist/gif.worker.js");
    if (!resp.ok) throw new Error("could not load the GIF worker");
    _gifWorkerUrl = URL.createObjectURL(await resp.blob());
    return _gifWorkerUrl;
}

// The best-supported WebM codec for MediaRecorder, or "" to let the browser choose.
function pickWebmMime() {
    const cands = ["video/webm;codecs=vp9", "video/webm;codecs=vp8", "video/webm"];
    for (const c of cands) if (window.MediaRecorder && MediaRecorder.isTypeSupported && MediaRecorder.isTypeSupported(c)) return c;
    return "";
}

// Composites a renderer's layered canvases (+ an optional overlay) into a single 2D canvas, downscaled to at most
// maxWidth px wide so the export stays a reasonable size. Dimensions are rounded to even numbers (VP8/VP9 require it).
// Must be called inside the renderer's afterRender (the WebGL buffer reads blank otherwise).
function compositeCanvasFrame(containerId, overlayId, maxWidth) {
    const canvases = $(containerId).querySelectorAll("canvas");
    if (!canvases.length) return null;
    const sw = canvases[0].width, sh = canvases[0].height;
    const scale = Math.min(1, (maxWidth || sw) / sw);
    let w = Math.max(2, Math.round(sw * scale)), h = Math.max(2, Math.round(sh * scale));
    w -= w % 2; h -= h % 2;
    const out = document.createElement("canvas");
    out.width = w; out.height = h;
    const ctx = out.getContext("2d");
    ctx.fillStyle = cssVar("--canvas-bg", "#18191c");
    ctx.fillRect(0, 0, w, h);
    canvases.forEach((c) => ctx.drawImage(c, 0, 0, w, h));
    const overlay = overlayId ? $(overlayId) : null;
    if (overlay && overlay.width) ctx.drawImage(overlay, 0, 0, w, h);
    return out;
}

// Advances an animation to one frame and resolves with its composited image, captured inside the renderer's afterRender.
function captureRendererFrame(spec, arg) {
    if (spec.captureFrame) {
        return Promise.resolve(spec.setFrame(arg)).then(() => spec.captureFrame());
    }
    return new Promise((resolve) => {
        const r = spec.renderer;
        const onRender = () => {
            if (typeof r.off === "function") r.off("afterRender", onRender);
            else if (typeof r.removeListener === "function") r.removeListener("afterRender", onRender);
            if (spec.drawOverlay) spec.drawOverlay();   // make the overlay current before compositing
            resolve(compositeCanvasFrame(spec.containerId, spec.overlayId, spec.maxWidth));
        };
        r.on("afterRender", onRender);
        spec.setFrame(arg);                             // must call renderer.refresh() → afterRender
    });
}

// Records an animation spec to an animated GIF (frames captured up front, then encoded off the main thread).
async function recordGif(spec) {
    if (typeof GIF === "undefined") { setStatus("GIF encoder not available (offline?).", "error"); return; }
    if (!spec.frameArgs.length) { setStatus("Nothing to record.", "error"); return; }
    spec.button.disabled = true;
    try {
        const workerScript = await gifWorkerUrl();
        const gif = new GIF({ workers: 2, quality: 10, workerScript, background: cssVar("--canvas-bg", "#18191c") });
        for (let i = 0; i < spec.frameArgs.length; i++) {
            setStatus("Recording " + spec.name + "… frame " + (i + 1) + "/" + spec.frameArgs.length, "busy");
            const frame = await captureRendererFrame(spec, spec.frameArgs[i]);
            if (frame) gif.addFrame(frame, { copy: true, delay: FRAME_DELAY_MS });
        }
        gif.on("progress", (p) => setStatus("Encoding GIF… " + Math.round(p * 100) + "%", "busy"));
        gif.on("finished", (blob) => { download(spec.name + ".gif", blob, "image/gif"); setStatus(spec.name + " GIF downloaded (" + spec.frameArgs.length + " frames)."); });
        gif.render();
    } catch (e) {
        setStatus("Could not record the " + spec.name + ": " + e.message, "error");
    } finally {
        spec.restore();
        spec.button.disabled = false;
    }
}

// Records an animation spec to a WebM video via MediaRecorder. MediaRecorder captures in real time, so each frame is
// held on a recording canvas for FRAME_DELAY_MS while a 30fps stream is captured off it.
async function recordWebm(spec) {
    if (typeof MediaRecorder === "undefined") { setStatus("WebM recording is not supported by this browser.", "error"); return; }
    if (!spec.frameArgs.length) { setStatus("Nothing to record.", "error"); return; }
    spec.button.disabled = true;
    try {
        const first = await captureRendererFrame(spec, spec.frameArgs[0]);
        if (!first) throw new Error("nothing to record");
        const rec = document.createElement("canvas");
        rec.width = first.width; rec.height = first.height;
        const rctx = rec.getContext("2d");
        rctx.drawImage(first, 0, 0);
        const stream = rec.captureStream(30);
        const mime = pickWebmMime();
        const recorder = new MediaRecorder(stream, mime ? { mimeType: mime } : undefined);
        const chunks = [];
        recorder.ondataavailable = (e) => { if (e.data && e.data.size) chunks.push(e.data); };
        const stopped = new Promise((res) => { recorder.onstop = res; });
        recorder.start();
        for (let i = 0; i < spec.frameArgs.length; i++) {
            setStatus("Recording " + spec.name + " (WebM)… frame " + (i + 1) + "/" + spec.frameArgs.length, "busy");
            if (i > 0) {
                const f = await captureRendererFrame(spec, spec.frameArgs[i]);
                if (f) rctx.drawImage(f, 0, 0, rec.width, rec.height);
            }
            await sleep(FRAME_DELAY_MS);
        }
        recorder.stop();
        await stopped;
        const blob = new Blob(chunks, { type: mime || "video/webm" });
        download(spec.name + ".webm", blob, mime || "video/webm");
        setStatus(spec.name + " WebM downloaded (" + spec.frameArgs.length + " frames).");
    } catch (e) {
        setStatus("Could not record the " + spec.name + ": " + e.message, "error");
    } finally {
        spec.restore();
        spec.button.disabled = false;
    }
}

// Caps a frame list to at most 80 entries, sampling evenly (keeps GIF/WebM size and recording time bounded).
function capFrames(pts) {
    const MAX = 80;
    if (pts.length <= MAX) return pts;
    const sampled = [];
    for (let i = 0; i < MAX; i++) sampled.push(pts[Math.round((i * (pts.length - 1)) / (MAX - 1))]);
    return [...new Set(sampled)];
}

// The WebM frame width from a resolution selector: a pixel cap, or null for full native resolution.
function webmMaxWidth(selectId) {
    const sel = $(selectId);
    return (sel && sel.value === "native") ? null : 1920;
}

// ---- graph timeline (Network tab) ----
// cap: whether to sample-cap the frames (GIF, where size/encode time matter) or keep them all (WebM).
// maxWidth: frame width cap in px (null = full native resolution).
function timelineRecordSpec(button, cap, maxWidth) {
    const tl = state.timeline;
    const savedT = tl.t;
    let pts = tl.events.slice();
    if (!pts.length || pts[0] > tl.min) pts.unshift(tl.min);
    if (pts[pts.length - 1] < tl.max) pts.push(tl.max);
    return {
        name: "timeline",
        button,
        renderer: state.renderer,
        containerId: "sigma-container",
        overlayId: "rec-overlay",
        drawOverlay: drawRecOverlay,
        maxWidth,
        frameArgs: cap ? capFrames(pts) : pts,
        setFrame: (t) => {
            tl.t = Math.max(tl.min, Math.min(tl.max, Math.round(t)));
            $("tl-slider").value = tl.t;
            $("tl-time").value = tl.t;
            updateTimelineReadout();
            applyReducers();
        },
        restore: () => setTimelineT(savedT),
    };
}

function downloadTimelineGif() {
    if (state.timeline.min == null) { setStatus("Pick a time attribute with timestamps first.", "error"); return; }
    if (state.activeTab !== "network") switchTab("network");
    stopTimeline();
    recordGif(timelineRecordSpec($("btn-tl-gif"), true, 900));
}

function downloadTimelineWebm() {
    if (state.timeline.min == null) { setStatus("Pick a time attribute with timestamps first.", "error"); return; }
    if (state.activeTab !== "network") switchTab("network");
    stopTimeline();
    recordWebm(timelineRecordSpec($("btn-tl-webm"), false, webmMaxWidth("tl-webm-res")));   // WebM: no frame cap
}

// ---- diffusion playback (Diffusion tab) ----
function diffusionRecordSpec(button, cap, maxWidth) {
    const res = state.diffusion.result;
    const savedIter = state.diffusion.iteration;
    const n = res.numIterations;
    const pts = [];
    for (let i = 0; i < n; i++) pts.push(i);
    return {
        name: "diffusion",
        button,
        renderer: state.diffusion.rendererType === "cosmograph" ? state.diffusion.cosmograph?.instance : state.diffusion.renderer,
        captureFrame: state.diffusion.rendererType === "cosmograph" ? () => state.diffusion.cosmograph?.captureFrame(maxWidth) : null,
        containerId: "diff-sigma-container",
        overlayId: "diff-overlay",
        drawOverlay: drawDiffOverlay,
        maxWidth,
        frameArgs: cap ? capFrames(pts) : pts,
        setFrame: (i) => setDiffIteration(i),
        restore: () => setDiffIteration(savedIter),
    };
}

async function prepareDiffusionRecording() {
    if (usingDiffGeographic()) { setStatus("Map video export is not available yet. Choose Sigma or Cosmograph to record diffusion.", "error"); return false; }
    if (!state.diffusion.result || !(state.diffusion.rendererType === "cosmograph" ? state.diffusion.cosmograph : state.diffusion.renderer)) { setStatus("Run a diffusion simulation first.", "error"); return false; }
    diffStop();
    if (state.activeTab !== "diffusion") switchTab("diffusion");
    if (state.diffusion.subview !== "graph") switchDiffSubtab("graph");
    await sleep(80);   // let the (now visible) diffusion canvas size itself before capturing
    return true;
}

async function downloadDiffusionGif() {
    if (!(await prepareDiffusionRecording())) return;
    recordGif(diffusionRecordSpec($("btn-diff-gif"), true, 900));
}

async function downloadDiffusionWebm() {
    if (!(await prepareDiffusionRecording())) return;
    recordWebm(diffusionRecordSpec($("btn-diff-webm"), false, webmMaxWidth("diff-webm-res")));   // WebM: no frame cap
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

/* ------------------------- cancellable jobs ------------------------- */
// Long computations (metrics, community detection, recommendation, diffusion) run on the server on a worker thread so
// they can be stopped mid-flight. Each is tied to a run button that turns into a "Stop" button while it is running:
// pressing it aborts the in-flight request (so the UI is freed at once) and posts /api/jobs/cancel with the job kind,
// which interrupts the server-side worker so cooperative computations also stop burning CPU.
state.jobs = {};   // btnId -> { controller, kind }

// Undo/redo history for structural edits (add/remove node, add/remove edge, add/remove information piece). Each
// entry carries an async `undo` and `redo` closure that re-applies the inverse / the action through the low-level
// apply* helpers (which never push history themselves). See the "edit history" section.
state.history = { undo: [], redo: [], applying: false };

function jobBusy(btnId) { return !!state.jobs[btnId]; }

function isAbort(e) { return e && (e.name === "AbortError"); }

// Marks a button as running: swaps it to a red "Stop" and returns the AbortSignal to pass to the request.
function beginJob(btnId, kind) {
    const btn = $(btnId);
    const controller = new AbortController();
    state.jobs[btnId] = { controller, kind };
    if (btn) {
        btn.dataset.runLabel = btn.innerHTML;
        btn.innerHTML = "⏹ Stop";
        btn.classList.add("btn-stop");
        btn.disabled = false;   // must stay clickable so it can be used to stop
    }
    return controller.signal;
}

// Restores a button to its idle (run) state.
function endJob(btnId) {
    if (!state.jobs[btnId]) return;
    delete state.jobs[btnId];
    const btn = $(btnId);
    if (btn && btn.dataset.runLabel != null) {
        btn.innerHTML = btn.dataset.runLabel;
        delete btn.dataset.runLabel;
        btn.classList.remove("btn-stop");
    }
}

// Stops the job attached to a button: abort the request client-side and ask the server to interrupt the worker.
async function cancelJob(btnId) {
    const job = state.jobs[btnId];
    if (!job) return;
    job.controller.abort();
    setStatus("Stopping…", "busy");
    try { await fetch("/api/jobs/cancel", jsonBody({ graphId: state.graphId, kind: job.kind })); }
    catch (e) { /* best-effort: the client-side abort already freed the UI */ }
    setStatus("Stopped.");
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

// Reflects a .relison selection on the import pane: the network type options do not apply to a saved session, so
// they are called out as ignored and the action reads "Open session".
function updateImportFormatHint() {
    const input = $("file-input");
    const file = input && input.files && input.files[0];
    const isSession = $("opt-format").value === "relison" || (!!file && /\.relison$/i.test(file.name));
    if (file && !isSession && $("opt-separator").dataset.manual !== "1")
        $("opt-separator").value = /\.csv$/i.test(file.name) ? "," : "tab";
    const isEdgeList = !["pajek", "gexf", "relison"].includes($("opt-format").value)
        && !(file && /\.(net|paj|gexf|relison)$/i.test(file.name));
    toggleHidden("edge-import-options", !isEdgeList);
    toggleHidden("edge-header-option", !isEdgeList);
    toggleHidden("import-session-hint", !isSession);
    toggleHidden("gexf-label-option", isSession || !($("opt-format").value === "gexf"
        || ($("opt-format").value === "" && file && /\.gexf$/i.test(file.name))));
    $("btn-load").textContent = isSession ? "Open session" : "Load network";
}

async function loadGraph() {
    const fileInput = $("file-input");
    if (!fileInput.files || fileInput.files.length === 0) {
        setStatus("Choose a network file first (edge list, Pajek, GEXF or a .relison session).", "error");
        return;
    }
    const file = fileInput.files[0];

    // A saved session carries its own network and type flags, so it bypasses the reader path entirely.
    if ($("opt-format").value === "relison" || /\.relison$/i.test(file.name)) {
        await openSession(file);
        return;
    }

    const form = new FormData();
    form.append("file", file);
    form.append("format", $("opt-format").value);   // blank = let the server infer it from the extension
    form.append("directed", $("opt-directed").checked);
    form.append("weighted", $("opt-weighted").checked);
    form.append("multigraph", $("opt-multigraph").checked);
    form.append("selfloops", $("opt-selfloops").checked);
    form.append("separator", selectedDelimiter("opt-separator"));
    form.append("header", $("opt-header").checked);
    form.append("labelsAsIds", $("opt-gexf-labels-as-ids").checked);

    setStatus("Loading network…", "busy");
    $("btn-load").disabled = true;
    try {
        const data = await api("/api/graph/load", { method: "POST", body: form });
        $("opt-directed").checked = data.stats.directed;
        applyLoadedGraph(data, $("opt-multigraph").checked);
        setStatus("Loaded " + data.stats.nodes + " nodes, " + data.stats.edges + " edges.");
    } catch (e) {
        setStatus(e.message, "error");
    } finally {
        $("btn-load").disabled = false;
    }
}

// Installs a freshly loaded/generated network session: resets every per-graph result, renders the graph and switches
// to the Network tab. Shared by the file-upload and the generator paths (both return the same payload).
function applyLoadedGraph(data, multigraph) {
    if (state.graphId !== data.graphId) state.savedLayoutPositions = null;
    state.graphId = data.graphId;
    state.directed = data.stats.directed;
    state.weighted = data.stats.weighted;
    state.stats = data.stats;   // kept for the report export
    state.reportPlots.clear();  // the previous graph's captured plots no longer apply
    state.multigraph = multigraph;
    state.attrSchema = data.schema || { node: [], edge: [] };
    stopTimeline();
    state.timeline = { nodeAttr: "", edgeAttr: "", mode: "instant", t: null, rangeStart: null, min: null, max: null, events: [], playing: null };
    resetResults();
    resetRecommendation();
    resetDiffusion();
    historyReset();   // a new network invalidates every pending undo/redo of the previous one
    resetRecEval();   // the test set and its results belong to the previous network
    populateRecEvalFeature();
    // Pieces reference nodes of the old graph; start fresh on the new (empty) session.
    if (piecesSaveTimer) { clearTimeout(piecesSaveTimer); piecesSaveTimer = null; }
    state.diffusion.pieces = [];
    state.diffusion.featureParams = [];
    state.diffusion.pieceFilters = {};
    state.diffusion.piecesHeaderSig = null;
    state.diffusion.piecesPage = 0;
    state.diffusion.piecesSaved = true;
    state.diffusion.realPropagated = [];
    state.diffusion.rpFilters = {};
    state.diffusion.rpHeaderSig = null;
    state.diffusion.rpPage = 0;
    switchPiecesMode("pieces");   // resets the inner toggle and renders the pieces table
    updateRecTabAvailability();
    resetTableViews();
    clearSelection();
    $("btn-global-comm").disabled = true;
    $("btn-indiv-comm").disabled = true;
    $("community-result").textContent = "";
    $("indiv-comm-partition").innerHTML = "";
    $("global-comm-partition").innerHTML = "";
    state.pathFocus = null;
    state.pathEdgeStyles.clear();
    state.pathEdgeStyleSequence = 0;
    state.pathEndpointFocusActive = false;
    state.lastPaths = [];
    $("select-path-source").value = "";
    $("select-path-target").value = "";
    $("select-paths-table").replaceChildren();
    $("select-path-summary").textContent = "Choose a source and target to find all shortest paths.";
    switchTab("network");          // ensure the (sized) network pane is visible before rendering
    renderGraph(data.graph);
    rebuildAppearanceOptions();    // surface any imported attributes in the appearance menus
    $("size-by").value = nodeAttrDefs().some(d => d.name === "viz:size") ? "imported" : "";
    $("node-color-mode").value = nodeAttrDefs().some(d => d.name === "viz:color") ? "imported" : "default";
    $("edge-size-by").value = edgeAttrDefs().some(d => d.name === "viz:thickness") ? "imported" : "";
    $("edge-color-mode").value = edgeAttrDefs().some(d => d.name === "viz:color") ? "imported" : "default";
    applyAppearance();
    updateOverview(data.stats);
    if (state.renderer) state.renderer.refresh();
    refreshAfterCompute();
}

/* --------------------------- graph generation ------------------------- */

// Model-specific parameters for the graph-generation request, read from the import card's inputs.
function collectGenParams(type) {
    switch (type) {
        case "erdos": return { nodes: numInput("gen-erdos-nodes", 100), prob: parseFloat($("gen-erdos-prob").value) || 0.05 };
        case "barabasi": return {
            initialNodes: numInput("gen-barabasi-initial", 5),
            numIter: numInput("gen-barabasi-iter", 95),
            numEdgesIter: numInput("gen-barabasi-edges", 2),
        };
        case "watts": return {
            nodes: numInput("gen-watts-nodes", 100),
            meanDegree: numInput("gen-watts-degree", 4),
            beta: parseFloat($("gen-watts-beta").value) || 0.1,
        };
        case "complete": return { nodes: numInput("gen-complete-nodes", 20) };
        case "empty": return { nodes: numInput("gen-empty-nodes", 50) };
        default: return {};
    }
}

const GEN_MODELS = ["erdos", "barabasi", "watts", "complete", "empty"];

function onGenTypeChange() {
    const type = $("gen-type").value;
    GEN_MODELS.forEach((m) => toggleHidden("gen-params-" + m, m !== type));
}

async function generateGraph() {
    const type = $("gen-type").value;
    // Client-side echo of the server-side Barabási constraint, for a friendlier message before the round-trip.
    if (type === "barabasi" && numInput("gen-barabasi-edges", 2) > numInput("gen-barabasi-initial", 5)) {
        setStatus("Barabási–Albert: edges per iteration must not exceed the initial nodes.", "error");
        return;
    }
    setStatus("Generating network…", "busy");
    $("btn-generate").disabled = true;
    try {
        const data = await api("/api/graph/generate", jsonBody({
            type, directed: $("gen-directed").checked, params: collectGenParams(type),
        }));
        applyLoadedGraph(data, false);   // generated graphs are simple unweighted graphs
        setStatus("Generated " + data.stats.nodes + " nodes, " + data.stats.edges + " edges.");
    } catch (e) {
        setStatus(e.message, "error");
    } finally {
        $("btn-generate").disabled = false;
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
    state.attributeMetricData = {};
    state.attributeMetricOrder = [];
    state.edgeAttributeMetricData = {};
    state.edgeAttributeMetricOrder = [];
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
    leaveGeographicView();
    state.cosmographDirty = true;
    stopLayout();
    if (state.renderer) { state.renderer.kill(); state.renderer = null; }

    const graph = new Graph({
        type: serialized.options ? serialized.options.type : (state.directed ? "directed" : "undirected"),
        multi: serialized.options ? serialized.options.multi : false,
        allowSelfLoops: serialized.options ? serialized.options.allowSelfLoops : true,
    });
    graph.import(serialized);
    state.graph = graph;
    window.relisonCosmographDirected = graph.type === "directed";

    graph.forEachNode((node) => graph.setNodeAttribute(node, "color", "#4f9dff"));
    applyAppearance();

    $("empty-hint").style.display = "none";
    // Sigma v3 uses `defaultDrawNodeLabel` and `defaultDrawEdgeLabel` instead of the older
    // `labelRenderer` / `edgeLabelRenderer` options. Update the renderer configuration accordingly.
    state.renderer = new Sigma(graph, $("sigma-container"), {
        defaultEdgeType: sigmaDefaultEdgeType(),
        edgeProgramClasses: window.relisonSigmaEdgePrograms || {},
        renderLabels: state.labelOpts.nodeShow,
        renderEdgeLabels: state.labelOpts.edgeShow,
        defaultDrawNodeLabel: drawNodeLabel,
        defaultDrawEdgeLabel: drawEdgeLabel,
        labelDensity: 0.5,
        labelRenderedSizeThreshold: 8,
        zIndex: true,
        // The container can be momentarily hidden (zero-size) if a graph is loaded from another tab;
        // tolerate it and refresh once the Network tab becomes visible.
        allowInvalidContainer: true,
    });
    // RELISON stores node sizes as diameters. Sigma interprets node `size` as
    // a radius, so normalize only its displayed data at the renderer boundary.
    state.renderer.setSetting("nodeReducer", (_node, data) => ({
        ...data,
        size: (Number.isFinite(data.size) ? data.size : 0) / 2,
    }));
    syncLabelOpts();

    // Repaint the recommended-edge overlay after sigma renders. Debounced (and cleared during the gesture) so
    // panning/zooming a large overlay stays smooth and the dashed lines never lag behind the moving graph.
    state.renderer.on("afterRender", scheduleRecOverlay);

    state.renderer.on("clickNode", ({ node, event }) => {
        if (state.dragNodes && dragDidMove) { dragDidMove = false; return; }
        if (state.multiNodeSelect && state.dragNodes && !$("edit-mode").checked) {
            toggleMultiSelectedNode(node);
            return;
        }
        handleNetworkPointClick(node, event);
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
    let draggedNodes = [];
    let dragAnchorNode = null;
    let dragStartPositions = new Map();
    let dragStartPointer = null;
    let dragDidMove = false;
    state.renderer.on("downNode", ({ node, event }) => {
        if (!state.dragNodes) return;
        dragDidMove = false;
        dragAnchorNode = node;
        draggedNodes = state.multiNodeSelect && state.multiSelectedNodes.has(node)
            ? [...state.multiSelectedNodes].filter((id) => graph.hasNode(id)) : [node];
        dragStartPositions = new Map(draggedNodes.map((id) => [id, { x: graph.getNodeAttribute(id, "x"), y: graph.getNodeAttribute(id, "y") }]));
        dragStartPointer = event ? state.renderer.viewportToGraph(event) : null;
        // Freeze the auto-fit bounding box so moving a node out of bounds doesn't make the camera jump.
        if (!state.renderer.getCustomBBox()) state.renderer.setCustomBBox(state.renderer.getBBox());
    });
    const captor = state.renderer.getMouseCaptor();
    captor.on("mousemovebody", (e) => {
        if (state.areaSelectingNodes) return;
        if (!draggedNodes.length) return;
        const pos = state.renderer.viewportToGraph(e);
        if (!dragStartPointer) dragStartPointer = pos;
        const dx = pos.x - dragStartPointer.x, dy = pos.y - dragStartPointer.y;
        if (Math.abs(dx) > 1e-7 || Math.abs(dy) > 1e-7) {
            dragDidMove = true;
            if (state.multiNodeSelect && dragAnchorNode != null && !state.multiSelectedNodes.has(dragAnchorNode)) {
                state.multiSelectedNodes = new Set([dragAnchorNode]);
                syncMultiNodeSelection();
            }
        }
        for (const id of draggedNodes) {
            const start = dragStartPositions.get(id);
            graph.setNodeAttribute(id, "x", start.x + dx);
            graph.setNodeAttribute(id, "y", start.y + dy);
        }
        // Stop sigma (and the browser) from also panning/selecting while dragging.
        e.preventSigmaDefault();
        e.original.preventDefault();
        e.original.stopPropagation();
    });
    const endDrag = () => {
        draggedNodes = [];
        dragAnchorNode = null;
        dragStartPositions.clear();
        dragStartPointer = null;
    };
    captor.on("mouseup", endDrag);
    captor.on("downStage", (event) => {
        if (!state.areaSelectingNodes || !state.multiNodeSelect || !state.dragNodes || $("edit-mode").checked) return;
        const original = event.original || event.originalEvent || event;
        const rect = $("sigma-container").getBoundingClientRect();
        const start = {
            x: Number.isFinite(original.clientX) ? original.clientX : rect.left + event.x,
            y: Number.isFinite(original.clientY) ? original.clientY : rect.top + event.y,
        };
        const selection = document.createElement("div");
        selection.className = "graph-area-selection";
        selection.style.cssText = `position:fixed;z-index:99999;left:${start.x}px;top:${start.y}px;width:0;height:0;border:2px solid var(--accent);background:rgba(79,157,255,.18)`;
        document.body.appendChild(selection);
        const move = (moveEvent) => {
            const x = moveEvent.clientX, y = moveEvent.clientY;
            selection.style.left = Math.min(start.x, x) + "px";
            selection.style.top = Math.min(start.y, y) + "px";
            selection.style.width = Math.abs(x - start.x) + "px";
            selection.style.height = Math.abs(y - start.y) + "px";
        };
        const finish = (upEvent) => {
            document.removeEventListener("pointermove", move, true);
            document.removeEventListener("pointerup", finish, true);
            document.removeEventListener("pointercancel", finish, true);
            const x = upEvent.clientX, y = upEvent.clientY;
            selection.remove();
            const bounds = {
                left: Math.min(start.x, x) - rect.left, right: Math.max(start.x, x) - rect.left,
                top: Math.min(start.y, y) - rect.top, bottom: Math.max(start.y, y) - rect.top,
            };
            const selected = new Set(upEvent.shiftKey || upEvent.ctrlKey || upEvent.metaKey ? state.multiSelectedNodes : []);
            graph.forEachNode((id) => {
                const data = state.renderer.getNodeDisplayData(id);
                if (!data || data.hidden) return;
                const point = state.renderer.graphToViewport({ x: data.x, y: data.y });
                if (point.x >= bounds.left && point.x <= bounds.right && point.y >= bounds.top && point.y <= bounds.bottom) selected.add(id);
            });
            state.multiSelectedNodes = selected;
            setAreaNodeSelection(false);
            syncMultiNodeSelection();
        };
        document.addEventListener("pointermove", move, true);
        document.addEventListener("pointerup", finish, { once: true, capture: true });
        document.addEventListener("pointercancel", finish, { once: true, capture: true });
        event.preventSigmaDefault?.();
        original.preventDefault?.();
    });
    if ($("network-renderer").value === "cosmograph") setTimeout(() => setNetworkRenderer("cosmograph"), 0);
}

function handleEditNodeClick(node, event) {
    const nativeEvent = event?.originalEvent || event?.original || event?.event || event;
    if (nativeEvent?.ctrlKey || nativeEvent?.metaKey) {
        if (state.editFirstNode == null) {
            state.editFirstNode = node;
            selectNode(node);
            setStatus("Edge source: " + node + ". Ctrl-click a target node to add the edge.");
        } else if (state.editFirstNode === node) {
            state.editFirstNode = null;
            setStatus("Edge creation cancelled.");
        } else {
            const source = state.editFirstNode;
            state.editFirstNode = null;
            editAddEdge(source, node);
        }
        return;
    }
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
    sizeSelect.innerHTML = '<option value="">— uniform (default) —</option>';
    if (nodeAttrDefs().some(d => d.name === "viz:size")) sizeSelect.appendChild(option("imported", "Imported size"));
    for (const name of metricNames) sizeSelect.appendChild(option(name, name));
    // Numeric node attributes can also drive node size.
    for (const d of nodeAttrDefs()) if (d.numeric) sizeSelect.appendChild(option("attr:" + d.name, "attr: " + d.name));
    restoreSelect(sizeSelect, sizeCurrent);

    const colorSelect = $("color-by");
    const colorCurrent = colorSelect.value;
    colorSelect.innerHTML = '<option value="">Choose an attribute</option>';
    for (const name of metricNames) colorSelect.appendChild(option("metric:" + name, name));
    for (const algo of communityNames) colorSelect.appendChild(option("community:" + algo, "community: " + algo));
    // Node attributes: numeric ones use the colour ramp, the rest are coloured categorically.
    for (const d of nodeAttrDefs()) colorSelect.appendChild(option((d.numeric ? "attr:" : "attrcat:") + d.name, "attr: " + d.name));
    restoreSelect(colorSelect, colorCurrent);

    const edgeColorSelect = $("edge-color-by");
    const edgeColorCurrent = edgeColorSelect.value;
    edgeColorSelect.innerHTML = '<option value="">Choose an attribute</option>';
    for (const d of edgeAttrDefs()) edgeColorSelect.appendChild(option(d.name, "attr: " + d.name));
    restoreSelect(edgeColorSelect, edgeColorCurrent);

    // Edge thickness: uniform + each computed edge (link) metric + numeric edge attributes.
    const edgeSizeSelect = $("edge-size-by");
    const edgeCurrent = edgeSizeSelect.value;
    edgeSizeSelect.innerHTML = '<option value="">— uniform —</option>';
    if (edgeAttrDefs().some(d => d.name === "viz:thickness")) edgeSizeSelect.appendChild(option("imported", "Imported thickness"));
    for (const name of state.pairOrder) edgeSizeSelect.appendChild(option(name, name));
    for (const d of edgeAttrDefs()) if (d.numeric) edgeSizeSelect.appendChild(option("eattr:" + d.name, "attr: " + d.name));
    restoreSelect(edgeSizeSelect, edgeCurrent);

    rebuildLayoutGroupOptions();   // keep the circle-packing "Group by" options in sync with communities/attributes
    rebuildTimelineOptions();      // keep the timeline's time-attribute selectors in sync with the schema
    syncAttributeCommunityMetricControls();
    syncEdgeAttributeMetricControls();
    syncNodeColorControls();
    syncEdgeColorControls();
    rebuildLabelAttributeOptions();
}

function setDefaultEdgeSeparator(inputId, selectId) {
    const input = $(inputId);
    const file = input && input.files && input.files[0];
    const select = $(selectId);
    if (file && select.dataset.manual !== "1") select.value = /\.csv$/i.test(file.name) ? "," : "tab";
}

function selectedDelimiter(selectId) {
    const value = $(selectId).value;
    return value === "tab" ? "\t" : value;
}

// Restores a select's value if the option still exists, otherwise falls back to the first (default) option.
function restoreSelect(select, value) {
    const exists = Array.from(select.options).some((o) => o.value === value);
    select.value = exists ? value : "";
}

function syncSizeControlVisibility() {
    const nodeUniform = !$("size-by").value;
    const edgeUniform = !$("edge-size-by").value;
    toggleHidden("node-size-uniform-params", !nodeUniform);
    toggleHidden("node-size-range-params", nodeUniform || $("size-by").value === "imported");
    toggleHidden("edge-size-uniform-params", !edgeUniform);
    toggleHidden("edge-size-range-params", edgeUniform || $("edge-size-by").value === "imported");
}

// Maps measured values into a visual range. Proportional mode uses the numeric
// extent; percentile mode uses rank, reducing the influence of outliers.
function scaleVisualValues(items, valueFor, minSize, maxSize, mode, reverse = false) {
    const values = items.map((item) => {
        const value = Number(valueFor(item));
        return Number.isFinite(value) ? value : 0;
    });
    const lo = Math.min(...values), hi = Math.max(...values);
    const span = (hi - lo) || 1;
    const range = Math.max(0, maxSize - minSize);
    if (hi === lo) return values.map(() => minSize);
    let percentile = null;
    if (mode === "percentile") {
        const ordered = [...values].sort((a, b) => a - b);
        percentile = new Map();
        ordered.forEach((value, index) => percentile.set(value, ordered.length > 1 ? index / (ordered.length - 1) : 0));
    }
    return values.map((value) => {
        const fraction = percentile ? percentile.get(value) : (value - lo) / span;
        return minSize + range * (reverse ? 1 - fraction : fraction);
    });
}
function applyAppearance() {
    const graph = state.graph;
    if (!graph) return;
    syncSizeControlVisibility();
    syncNodeColorControls();
    syncEdgeColorControls();

    const sizeBy = $("size-by").value;
    if (sizeBy === "imported") {
        const fallback = numInput("node-size-uniform", 14);
        graph.forEachNode(node => {
            const value = nodeAttrVal(node, "viz:size");
            graph.setNodeAttribute(node, "size", typeof value === "number" && Number.isFinite(value) && value >= 0 ? value : fallback);
        });
    } else if (!sizeBy) {
        const uniform = numInput("node-size-uniform", 14);
        graph.forEachNode((node) => graph.setNodeAttribute(node, "size", uniform));
    } else {
        const minSize = numInput("node-size-min", 2), maxSize = numInput("node-size-max", 14);
        const sizeValues = sizeBy.startsWith("attr:") ? numericMap(nodeAttrValues(sizeBy.slice(5))) : (state.metricData[sizeBy] || {});
        const nodes = graph.nodes();
        const sizes = scaleVisualValues(nodes, (node) => sizeValues[node], minSize, maxSize, $("node-size-scale").value, $("node-size-reverse").checked);
        nodes.forEach((node, index) => graph.setNodeAttribute(node, "size", sizes[index]));
    }
    const colorMode = $("node-color-mode").value;
    const colorSel = $("color-by").value;
    if (colorMode === "imported") {
        graph.forEachNode(node => graph.setNodeAttribute(node, "color", nodeAttrVal(node, "viz:color") || "#4f9dff"));
        state.colorLegend = { type: "none" };
    } else if (colorMode === "single") {
        const color = $("node-color-single").value || "#4f9dff";
        graph.forEachNode((node) => graph.setNodeAttribute(node, "color", color));
        state.colorLegend = { type: "none" };
    } else if (colorMode !== "attribute" || !colorSel) {
        graph.forEachNode((node) => graph.setNodeAttribute(node, "color", "#4f9dff"));
        state.colorLegend = { type: "none" };
    } else if (colorSel.startsWith("community:")) colorByCommunity(colorSel.slice("community:".length));
    else if (colorSel.startsWith("metric:")) colorByMetric(colorSel.slice("metric:".length));
    else if (colorSel.startsWith("attrcat:")) colorByNodeCategorical(colorSel.slice("attrcat:".length));
    else if (colorSel.startsWith("attr:")) colorByNodeAttr(colorSel.slice("attr:".length));

    applyEdgeAppearance();
    applyLabels();
    buildNodeLegend();

    if (state.renderer) { state.renderer.refresh(); drawRecOverlay(); }
    queueCosmographAppearanceRefresh();
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
    const edgeAttrName = sizeBy.startsWith("eattr:") ? sizeBy.slice("eattr:".length) : null;
    const sizeData = (sizeBy && !edgeAttrName) ? (state.pairData[sizeBy] || {}) : null;
    const edgeVal = (edge, source, target) => {
        if (edgeAttrName) { const value = Number(edgeAttrVal(edge, edgeAttrName)); return Number.isFinite(value) ? value : 0; }
        return sizeData ? (sizeData[pairKey(source, target)] ?? 0) : 0;
    };
    const edges = graph.edges();
    let widths = null;
    if (sizeBy && sizeBy !== "imported") {
        const minWidth = numInput("edge-size-min", 0.5), maxWidth = numInput("edge-size-max", 6);
        widths = scaleVisualValues(edges, (edge) => edgeVal(edge, graph.source(edge), graph.target(edge)), minWidth, maxWidth, $("edge-size-scale").value, $("edge-size-reverse").checked);
    }
    const uniform = numInput("edge-size-uniform", 0.5);
    const colorMode = $("edge-color-mode").value;
    const single = $("edge-color-single").value;
    const edgeColorAttribute = $("edge-color-by").value;
    const edgeColorDefinition = edgeAttrDefs().find((attribute) => attribute.name === edgeColorAttribute);
    const edgeColors = new Map();
    if (colorMode === "attribute" && edgeColorAttribute && edgeColorDefinition?.numeric) {
        const values = edges.map((edge) => {
            const value = edgeAttrVal(edge, edgeColorAttribute);
            return value == null ? NaN : Number(value);
        });
        const finiteValues = values.filter(Number.isFinite);
        const min = finiteValues.length ? Math.min(...finiteValues) : 0;
        const max = finiteValues.length ? Math.max(...finiteValues) : 1;
        const span = max - min || 1;
        values.forEach((value, index) => edgeColors.set(edges[index], Number.isFinite(value)
            ? lerpHex($("edge-color-low").value, $("edge-color-high").value, max === min ? 0.5 : (value - min) / span)
            : "#888888"));
    } else if (colorMode === "attribute" && edgeColorAttribute) {
        const categories = new Map();
        edges.forEach((edge) => {
            const value = edgeAttrVal(edge, edgeColorAttribute);
            if (value == null) { edgeColors.set(edge, "#888888"); return; }
            const key = String(value);
            if (!categories.has(key)) categories.set(key, categoricalHex(categories.size));
            edgeColors.set(edge, categories.get(key));
        });
    }

    edges.forEach((edge, index) => {
        const source = graph.source(edge), target = graph.target(edge);
        const importedThickness = edgeAttrVal(edge, "viz:thickness");
        const thickness = sizeBy === "imported" && typeof importedThickness === "number"
            && Number.isFinite(importedThickness) && importedThickness >= 0 ? importedThickness : null;
        graph.setEdgeAttribute(edge, "size", thickness ?? (widths ? widths[index] : uniform));
        if (colorMode === "imported") graph.setEdgeAttribute(edge, "color", edgeAttrVal(edge, "viz:color") || "#888888");
        else if (colorMode === "single") graph.setEdgeAttribute(edge, "color", single);
        else if (colorMode === "attribute") graph.setEdgeAttribute(edge, "color", edgeColors.get(edge) || "#888888");
        else if (colorMode === "endpoints") graph.setEdgeAttribute(edge, "color", averageColor(graph.getNodeAttribute(source, "color"), graph.getNodeAttribute(target, "color")));
        else graph.setEdgeAttribute(edge, "color", "#888888");
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
        setTooltip(sw, "js-colour-for", { value });
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

function rebuildLabelAttributeOptions() {
    for (const [kind, defs, defaultText] of [
        ["node", nodeAttrDefs(), "Default (label or node ID)"],
        ["edge", edgeAttrDefs(), "Default (label attribute)"],
    ]) {
        const select = $(kind + "-label-attribute");
        const current = select.value;
        select.replaceChildren(option("", defaultText), option("id", kind === "node" ? "Node ID" : "Edge ID"));
        defs.forEach((def) => select.appendChild(option("attr:" + def.name, def.name)));
        restoreSelect(select, current);
    }
}

function attributeLabelText(value, fallback) {
    if (value === undefined || value === null || value === "") return fallback;
    return typeof value === "object" ? JSON.stringify(value) : String(value);
}

// Map the chosen attributes into display labels shared by all graph renderers.
function applyLabels() {
    const g = state.graph;
    if (!g) return;
    const nodeSelection = $("node-label-attribute").value;
    const edgeSelection = $("edge-label-attribute").value;
    state.labelOpts.nodeAttribute = nodeSelection;
    state.labelOpts.edgeAttribute = edgeSelection;
    const nodeAttribute = nodeSelection.startsWith("attr:") ? nodeSelection.slice(5) : "label";
    const edgeAttribute = edgeSelection.startsWith("attr:") ? edgeSelection.slice(5) : "label";
    g.forEachNode((n) => {
        const lbl = nodeSelection === "id" ? n : nodeAttrVal(n, nodeAttribute);
        g.setNodeAttribute(n, "label", attributeLabelText(lbl, String(n)));
    });
    g.forEachEdge((e) => {
        const lbl = edgeSelection === "id" ? e : edgeAttrVal(e, edgeAttribute);
        g.setEdgeAttribute(e, "label", attributeLabelText(lbl, ""));
    });
}

// Reads the label controls into state and pushes the show/hide flags to the renderer.
function syncLabelOpts() {
    applyLabels();
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
    window.relisonCosmographLabelOpts = { ...o };
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
    queueCosmographAppearanceRefresh();
    if (usingGeographic()) window.relisonGeographic.refresh();
    if (state.diffusion.graph) syncDiffAppearance();
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

function zoomIn() {
    if (usingGeographic()) { window.relisonGeographic.zoomIn(); return; }
    if (usingCosmograph()) { window.relisonCosmograph?.zoomIn(); return; }
    if (state.renderer) state.renderer.getCamera().animatedZoom();
}
function zoomOut() {
    if (usingGeographic()) { window.relisonGeographic.zoomOut(); return; }
    if (usingCosmograph()) { window.relisonCosmograph?.zoomOut(); return; }
    if (state.renderer) state.renderer.getCamera().animatedUnzoom();
}
function zoomFit() {
    if (usingGeographic()) { window.relisonGeographic.fitView(); return; }
    if (usingCosmograph()) { window.relisonCosmograph?.fitView(); return; }
    if (state.renderer) state.renderer.getCamera().animatedReset();
}
function syncCosmographLabelColorControls() {
    // Cosmograph supports node-label colour directly. Keep both colour inputs
    // available so the shared label controls remain consistent between renderers.
    for (const id of ["node-label-color", "edge-label-color"]) {
        const control = $(id);
        control.disabled = false;
        control.removeAttribute("title");
        control.removeAttribute("aria-description");
    }
}


let cosmographAppearanceRefreshTimer = null;
function queueCosmographAppearanceRefresh() {
    if (usingGeographic()) window.relisonGeographic.refresh();
    if (!state.graph || !window.relisonCosmograph) return;
    syncCosmographRecommendation();
    // Avoid updating a hidden canvas. Its next visible render will consume the
    // latest shared graph state instead.
    if ($("network-renderer").value !== "cosmograph" || !$("pane-network").classList.contains("active")) {
        state.cosmographDirty = true;
        return;
    }
    clearTimeout(cosmographAppearanceRefreshTimer);
    cosmographAppearanceRefreshTimer = setTimeout(() => {
        if ($("network-renderer").value !== "cosmograph" || !$("pane-network").classList.contains("active") || !state.graph) {
            state.cosmographDirty = true;
            return;
        }
        window.relisonCosmograph.render(state.graph, {
            ...cosmographInteractionCallbacks(),
        }).then(() => { state.cosmographDirty = false; })
          .catch((e) => setStatus("Cosmograph could not update its visual mapping: " + e.message, "error"));
    }, 80);
}

let cosmographTimelineRefreshTimer = null;
function queueCosmographTimelineRefresh() {
    if (!state.graph || !window.relisonCosmograph) return;
    if ($("network-renderer").value !== "cosmograph" || !$("pane-network").classList.contains("active")) {
        state.cosmographDirty = true;
        return;
    }
    clearTimeout(cosmographTimelineRefreshTimer);
    cosmographTimelineRefreshTimer = setTimeout(() => {
        if ($("network-renderer").value !== "cosmograph" || !$("pane-network").classList.contains("active") || !state.graph) return;
        const update = window.relisonCosmograph.syncTimeline || window.relisonCosmograph.render;
        update(state.graph, {
            ...cosmographInteractionCallbacks(),
        }).then(() => { state.cosmographDirty = false; })
          .catch((e) => setStatus("Cosmograph could not update the timeline: " + e.message, "error"));
    }, 40);
}
async function setNetworkRenderer(name) {
    const pane = $("pane-network"), container = $("cosmograph-container"), select = $("network-renderer");
    if (name === "geographic") {
        $("layout-type").value = "geographic";
        $("layout-advanced").open = true;
        updateLayoutTypeUI(); updateLayoutButton();
        return applyGeographicLayout();
    }
    if (state.layoutRequest && currentLayoutType() === "geographic") stopLayout();
    if (pane.classList.contains("geographic-active")) leaveGeographicView(name);
    if (name !== "cosmograph") {
        pane.classList.remove("cosmograph-active");
        container.hidden = true;
        // Preserve the completed Cosmograph canvas. It can be shown again
        // without reconstructing it when the shared graph is unchanged.
        select.value = "sigma";
        return;
    }
    if (!state.graph) {
        setStatus("Load a network before selecting Cosmograph.", "error");
        return;
    }
    if (!window.relisonCosmograph) {
        setStatus("Cosmograph is still loading; try again in a moment.", "error");
        return;
    }
    try {
        const recommendationChanged = syncCosmographRecommendation();
        if (!state.cosmographLabelDefaultsApplied) {
            // Cosmograph labels begin white; afterwards the shared colour
            // pickers are the explicit source of truth for both renderers.
            $("node-label-color").value = "#ffffff";
            $("edge-label-color").value = "#ffffff";
            state.labelOpts.nodeColor = "#ffffff";
            state.labelOpts.edgeColor = "#ffffff";
            window.relisonCosmographLabelOpts = { ...state.labelOpts };
            state.cosmographLabelDefaultsApplied = true;
        }
        container.hidden = false;
        const needsRender = recommendationChanged || state.cosmographDirty || !window.relisonCosmograph.isRendered();
        if (needsRender) {
            await window.relisonCosmograph.render(state.graph, {
                ...cosmographInteractionCallbacks(),
            });
            state.cosmographDirty = false;
        }
        pane.classList.add("cosmograph-active");
        select.value = "cosmograph";
        setStatus("Cosmograph visualization ready.");
    } catch (e) {
        container.hidden = true;
        pane.classList.remove("cosmograph-active");
        select.value = "sigma";
        setStatus("Cosmograph could not render this graph: " + e.message, "error");
    }
}
window.addEventListener("relison-cosmograph-ready", () => { if ($("network-renderer").value === "cosmograph") setNetworkRenderer("cosmograph"); });
window.addEventListener("relison-diffusion-cosmograph-ready", () => {
    if (state.diffusion.graph && state.diffusion.rendererType === "cosmograph" && !state.diffusion.cosmograph) initDiffCosmograph();
});
/* ----------------------------- selection ---------------------------- */

function syncCosmographSelection() {
    if (!window.relisonCosmograph?.setFocusedSelection) return;
    window.relisonCosmograph.setFocusedSelection(state.selection.type === "node" ? state.selectedNode : null, state.selection.type === "edge" ? state.selectedEdge : null)
        .catch((error) => console.warn("Cosmograph selection sync failed.", error));
}

function syncNodeColorControls() {
    const mode = $("node-color-mode").value;
    const selection = $("color-by").value;
    toggleHidden("node-color-single-field", mode !== "single");
    toggleHidden("node-color-attribute-field", mode !== "attribute");
    const metric = selection.startsWith("metric:");
    toggleHidden("node-color-range-field", mode !== "attribute" || !metric);
}

function syncEdgeColorControls() {
    const mode = $("edge-color-mode").value;
    const name = $("edge-color-by").value;
    const definition = edgeAttrDefs().find((attribute) => attribute.name === name);
    toggleHidden("edge-color-single-field", mode !== "single");
    toggleHidden("edge-color-attribute-field", mode !== "attribute");
    toggleHidden("edge-color-range-field", mode !== "attribute" || !definition?.numeric);
}

function sigmaDefaultEdgeType() {
    const curved = $("edge-shape")?.value === "curved" && window.relisonSigmaEdgePrograms;
    return curved ? (state.directed ? "curvedArrow" : "curve") : (state.directed ? "arrow" : "line");
}

function applyEdgeShape() {
    if (usingCosmograph()) {
        queueCosmographAppearanceRefresh();
        return;
    }
    if (state.renderer) {
        state.renderer.setSetting("defaultEdgeType", sigmaDefaultEdgeType());
        state.renderer.refresh();
        drawRecOverlay();
        if ($("edge-shape").value === "curved" && !window.relisonSigmaEdgePrograms) {
            setStatus("Curved Sigma edges are still loading.", "error");
        }
    }
}

window.addEventListener("relison-sigma-edge-curve-ready", () => {
    if (state.renderer && !usingCosmograph() && $("edge-shape").value === "curved") {
        // Sigma creates edge programs with the renderer. Recreate only in the
        // rare case the user loaded a graph before the curve extension arrived.
        const graphData = state.graph?.export();
        if (graphData) renderGraph(graphData);
    }
});

function updateSelectionSummary() {
    const edge = state.selection.type === "edge", path = state.selection.type === "path", kind = $("selection-kind"), summary = $("selection-summary");
    if (!kind || !summary) return;
    kind.textContent = edge ? "Edge" : path ? "Path" : "Node";
    if (edge && state.selectedEdge && state.graph?.hasEdge(state.selectedEdge)) {
        const source = state.graph.source(state.selectedEdge), target = state.graph.target(state.selectedEdge);
        const focus = selectionFocus();
        summary.textContent = source + " → " + target + (focus ? " · " + focus.nodes.size + " nodes in focus" : "");
    } else if (path && $("select-path-source").value && $("select-path-target").value) {
        summary.textContent = $("select-path-source").value + " → " + $("select-path-target").value + (state.lastPaths.length ? " · " + state.lastPaths.length + " shortest path(s)" : "");
    } else if (!edge && !path && state.selectedNode) {
        const focus = selectionFocus();
        summary.textContent = state.selectedNode + (focus ? " · " + focus.nodes.size + " nodes in focus" : "");
    } else {
        summary.textContent = edge ? "Click a link to inspect both endpoints." : path ? "Choose path endpoints to explore connections." : "Select a node to inspect it.";
    }
}
function populateEdgeSelector() {
    const select = $("select-edge-input"), graph = state.graph;
    if (!select) return;
    const current = state.selectedEdge || select.value;
    select.innerHTML = '<option value="">— choose an edge —</option>';
    if (!graph) return;
    const candidates = state.edgeSelectionPair
        ? edgeIdsBetween(state.edgeSelectionPair.source, state.edgeSelectionPair.target)
        : graph.edges();
    candidates.slice().sort((a, b) => String(a).localeCompare(String(b))).forEach((edge) => {
        const source = graph.source(edge), target = graph.target(edge);
        const suffix = graph.multi ? " · " + edge : "";
        select.appendChild(option(edge, source + " → " + target + suffix));
    });
    if (current && graph.hasEdge(current)) select.value = current;
}
function updateSelectionTargetUI() {
    const edge = state.selection.type === "edge";
    const path = state.selection.type === "path";
    $("select-type").value = path ? "path" : edge ? "edge" : "node";
    toggleHidden("select-node-field", edge || path);
    toggleHidden("select-edge-field", !edge);
    toggleHidden("select-path-fields", !path);
    toggleHidden("select-path-results", !path);
    toggleHidden("select-path-color-controls", !path);
    toggleHidden("path-edge-highlight-color-field", state.pathEdgeHighlight.mode !== "single");
    toggleHidden("path-node-scale-field", !state.pathAppearance.enlargeNodes);
    toggleHidden("select-view-mode-field", false);
    if (edge) populateEdgeSelector();
    $("node-info-table").hidden = edge || path;
    const options = Array.from($("select-mode").options);
    options.forEach((option) => {
        const value = option.value;
        const isPathMode = value === "path-highlight" || value === "path-only";
        const isEdgeMode = value === "edge-highlight" || value.startsWith("edge-attribute-");
        const isNodeEdgeMode = value.startsWith("ego-") || value.startsWith("community-");
        const visible = value === "none" || (path ? isPathMode : edge ? isEdgeMode || isNodeEdgeMode : value === "highlight" || isNodeEdgeMode);
        option.hidden = !visible;
        option.disabled = !visible;
    });
    const highlight = $("select-mode").querySelector('option[value="highlight"]');
    if (highlight) highlight.textContent = edge ? "Highlight endpoints" : "Highlight node";
    const edgeHighlight = $("select-mode").querySelector('option[value="edge-highlight"]');
    if (edgeHighlight) { edgeHighlight.hidden = !edge; edgeHighlight.disabled = !edge; }
    ["edge-attribute-highlight", "edge-attribute-only"].forEach((value) => {
        const option = $("select-mode").querySelector('option[value="' + value + '"]');
        if (option) { option.hidden = !edge; option.disabled = !edge; }
    });
    const edgeAttributeMode = edge && ["edge-attribute-highlight", "edge-attribute-only"].includes(state.selection.mode);
    $("select-edge-attribute-field").hidden = !edgeAttributeMode;
    const edgeAttributes = edgeAttrDefs().map((def) => def.name);
    setSelectOptions("select-edge-attribute", edgeAttributes, edgeAttributes);
    if (!state.selection.edgeAttribute || !edgeAttributes.includes(state.selection.edgeAttribute)) state.selection.edgeAttribute = edgeAttributes[0] || null;
    if (state.selection.edgeAttribute) $("select-edge-attribute").value = state.selection.edgeAttribute;
    if (path && !["path-highlight", "path-only", "none"].includes(state.selection.mode)) state.selection.mode = "path-highlight";
    if (edge && !["edge-highlight", "edge-attribute-highlight", "edge-attribute-only", "ego-highlight", "ego-only", "community-highlight", "community-only", "none"].includes(state.selection.mode)) state.selection.mode = "edge-highlight";
    if (!edge && !path && ["edge-highlight", "edge-attribute-highlight", "edge-attribute-only", "path-highlight", "path-only"].includes(state.selection.mode)) state.selection.mode = "highlight";
    $("select-mode").value = state.selection.mode;
    const labels = edge ? { "ego-highlight": "Highlight both ego-networks", "ego-only": "Show only both ego-networks", "community-highlight": "Highlight both communities", "community-only": "Show only both communities" } : { "ego-highlight": "Highlight ego-network", "ego-only": "Show only ego-network", "community-highlight": "Highlight community", "community-only": "Show only community" };
    Object.entries(labels).forEach(([value, text]) => { const option = $("select-mode").querySelector('option[value="' + value + '"]'); if (option) option.textContent = text; });
    const details = $("edge-selection-details");
    if (details) details.hidden = !edge;
    if (edge && !state.selectedEdge) {
        const prompt = state.edgeSelectionPair
            ? "Choose the desired link between " + state.edgeSelectionPair.source + " and " + state.edgeSelectionPair.target + " from the selector above."
            : "Click a link, or Ctrl/Cmd-click its two endpoints.";
        $("edge-info-table").innerHTML = '<tr><td class="muted" colspan="2">' + prompt + '</td></tr>';
    }
    updateSelectionSummary();
    updatePartitionField();
}

function setSelectionType(type) {
    const previousType = state.selection.type;
    state.selection.type = type === "edge" ? "edge" : type === "path" ? "path" : "node";
    if (previousType === "path" && state.selection.type !== "path") {
        state.pathFocus = null;
        state.pathEndpointFocusActive = false;
    }
    if (state.selection.type === "path") {
        const previousNode = state.selectedNode;
        state.selectedNode = null;
        state.selection.node = null;
        state.selectedEdge = null;
        state.edgeSelectionPair = null;
        state.cosmographEdgeStart = null;
        state.pathFocus = null;
        state.pathEndpointFocusActive = false;
        state.selection.mode = "path-highlight";
        $("select-node-input").value = "";
        if (!$("select-path-source").value) $("select-path-source").value = previousNode || "";
        renderNodeInfo(null);
        clearEdgeSelection();
        state.lastPaths = [];
        $("select-paths-table").replaceChildren();
        $("select-path-summary").textContent = "Choose a source and target to find all shortest paths.";
    } else if (state.selection.type === "node") clearEdgeSelection();
    else {
        state.selectedNode = null;
        state.selection.node = null;
        $("select-node-input").value = "";
        renderNodeInfo(null);
    }
    updateSelectionTargetUI();
    applyReducers();
    syncCosmographSelection();
    renderTable(state.activeTableSubtab);
}
function clearEdgeSelection() {
    state.selectedEdge = null;
    state.edgeSelectionPair = null;
    const details = $("edge-selection-details");
    if (details) details.hidden = true;
    const table = $("edge-info-table");
    if (table) table.innerHTML = '<tr><td class="muted" colspan="2">No edge selected.</td></tr>';
}

function renderEdgeInfo(edge) {
    const graph = state.graph, table = $("edge-info-table"), details = $("edge-selection-details");
    if (!table || !details) return;
    table.innerHTML = "";
    if (edge == null || !graph || !graph.hasEdge(edge)) { clearEdgeSelection(); return; }
    details.hidden = false;
    const source = graph.source(edge), target = graph.target(edge), attrs = graph.getEdgeAttributes(edge);
    $("edge-source-preview").textContent = source;
    $("edge-target-preview").textContent = target;
    const rows = [["id", edge], ["source", source], ["target", target]];
    if (attrs.weight != null) rows.push(["weight", fmt(attrs.weight)]);
    if (attrs.label) rows.push(["label", attrs.label]);
    for (const d of edgeAttrDefs()) {
        const value = edgeAttrVal(edge, d.name);
        if (value !== undefined && value !== null && d.name !== "label") rows.push(["attr · " + d.name, fmt(value)]);
    }
    for (const [key, value] of rows) {
        const tr = document.createElement("tr");
        tr.innerHTML = `<td>${key}</td><td>${value}</td>`;
        table.appendChild(tr);
    }
}

function selectEdge(edge) {
    if (!state.graph || !state.graph.hasEdge(edge)) return;
    state.pathFocus = null;
    state.pathEndpointFocusActive = false;
    // Selection is exclusive: an edge replaces any previously selected node.
    state.selectedNode = null;
    state.selection.node = null;
    $("select-node-input").value = "";
    renderNodeInfo(null);
    state.selectedEdge = edge;
    state.edgeSelectionPair = null;
    state.selection.type = "edge";
    state.cosmographEdgeStart = null;
    updateSelectionTargetUI();
    renderEdgeInfo(edge);
    $("select-edge-input").value = edge;
    applyReducers();
    syncCosmographSelection();
}
function edgeIdsBetween(source, target) {
    const graph = state.graph;
    if (!graph || !graph.hasNode(source) || !graph.hasNode(target)) return [];
    const ids = new Set(graph.edges(source, target));
    // Endpoint-pair selection is direction-agnostic; direct link clicks retain
    // the exact edge id, including direction and parallel-edge identity.
    graph.edges(target, source).forEach((edge) => ids.add(edge));
    return [...ids];
}

function handleCosmographPointClick(node, event) {
    if (state.multiNodeSelect && state.dragNodes && !$("edit-mode").checked) {
        toggleMultiSelectedNode(node);
        return;
    }
    handleNetworkPointClick(node, event);
}

function syncMultiNodeSelection() {
    if (state.graph) state.multiSelectedNodes = new Set([...state.multiSelectedNodes].filter((node) => state.graph.hasNode(node)));
    if (state.renderer && state.graph) applyReducers();
    window.relisonMultiSelectEnabled = state.multiNodeSelect;
    window.relisonMultiSelectedNodes = [...state.multiSelectedNodes];
    window.relisonCosmograph?.setMultiSelectedNodes?.([...state.multiSelectedNodes])
        ?.catch?.((error) => console.warn("Could not update Cosmograph multi-selection.", error));
}

function toggleMultiSelectedNode(node) {
    if (!state.graph?.hasNode(node)) return;
    if (state.multiSelectedNodes.has(node)) state.multiSelectedNodes.delete(node);
    else state.multiSelectedNodes.add(node);
    syncMultiNodeSelection();
}

function setAreaNodeSelection(enabled) {
    state.areaSelectingNodes = Boolean(enabled && state.multiNodeSelect && state.dragNodes && !$("edit-mode").checked);
    $("btn-area-select-nodes").classList.toggle("active", state.areaSelectingNodes);
    if (window.relisonCosmograph) {
        window.relisonCosmograph.setRectSelectionEnabled?.(state.areaSelectingNodes)
            ?.catch?.((error) => setStatus("Could not enable area selection: " + error.message, "error"));
    }
}

function chooseEdgeBetween(source, target) {
    state.pathFocus = null;
    state.pathEndpointFocusActive = false;
    state.selectedNode = null;
    state.selection.node = null;
    state.selectedEdge = null;
    state.edgeSelectionPair = { source, target };
    state.selection.type = "edge";
    state.cosmographEdgeStart = null;
    $("select-node-input").value = "";
    renderNodeInfo(null);
    updateSelectionTargetUI();
    applyReducers();
    syncCosmographSelection();
    setStatus("Choose the desired edge between " + source + " and " + target + " in the Edge selector.");
}

function handleNetworkPointClick(node, event) {
    if (!state.graph || !state.graph.hasNode(node)) return;
    if ($("edit-mode").checked) { handleEditNodeClick(node, event); return; }
    const nativeEvent = event?.originalEvent || event?.original || event?.event || event;
    const additive = Boolean(nativeEvent?.ctrlKey || nativeEvent?.metaKey);
    const source = state.selectedNode;
    if (additive && source && source !== node) {
        const edges = edgeIdsBetween(source, node);
        if (edges.length === 1) {
            selectNode(node);
            selectEdge(edges[0]);
            return;
        }
        if (edges.length > 1) { chooseEdgeBetween(source, node); return; }
        selectNode(node);
        showPathsBetween(source, node);
        return;
    }
    clearEdgeSelection();
    state.cosmographEdgeStart = node;
    selectNode(node);
}

function handleCosmographLinkClick(edge) {
    if (!$("edit-mode").checked) selectEdge(edge);
}

function handleCosmographStageClick(event) {
    if (!$("edit-mode").checked) { clearSelection(); return; }
    const container = $("cosmograph-container");
    const nativeEvent = event?.sourceEvent || event?.originalEvent || event?.original || event;
    if (!container || !nativeEvent || !window.relisonCosmograph?.screenToSpacePosition) return;
    const bounds = container.getBoundingClientRect();
    const position = window.relisonCosmograph.screenToSpacePosition([
        nativeEvent.clientX - bounds.left,
        nativeEvent.clientY - bounds.top,
    ]);
    if (position && Number.isFinite(position[0]) && Number.isFinite(position[1])) {
        editAddNodeAt({ x: position[0], y: position[1] });
    }
}

function cosmographInteractionCallbacks() {
    return {
        onPointClick: handleCosmographPointClick,
        onLinkClick: handleCosmographLinkClick,
        onStageClick: handleCosmographStageClick,
        onRectSelected: (indices, additive = false) => {
            const selected = new Set(additive ? state.multiSelectedNodes : []);
            indices.map((index) => window.relisonCosmograph?.getPointId?.(index)).filter((id) => id != null && state.graph?.hasNode(id)).forEach((id) => selected.add(id));
            state.multiSelectedNodes = selected;
            setAreaNodeSelection(false);
            syncMultiNodeSelection();
        },
        onDragStartNode: (node) => {
            if (node != null && !state.multiSelectedNodes.has(node)) {
                state.multiSelectedNodes = new Set([node]);
                syncMultiNodeSelection();
            }
        },
    };
}
// Selects a node (from a click or the UI) and refreshes the info panel + visualization emphasis.
function selectNode(node) {
    if (!state.graph || !state.graph.hasNode(node)) return;
    state.pathFocus = null;
    state.pathEndpointFocusActive = false;
    state.selectedNode = node;
    state.selection.type = "node";
    state.selection.node = node;
    clearEdgeSelection();
    updateSelectionTargetUI();
    $("select-node-input").value = node;
    renderNodeInfo(node);
    updatePartitionField();
    applyReducers();
    syncCosmographSelection();
    if (state.selection.mode === "ego-only" || state.selection.mode === "community-only") renderTable(state.activeTableSubtab);
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

function clearSelection({ refreshTable = true, refreshGraph = true } = {}) {
    const clearingPath = state.selection.type === "path";
    state.selectedNode = null;
    state.selection.type = "node";
    state.selection.node = null;
    state.cosmographEdgeStart = null;
    $("select-node-input").value = "";
    if (clearingPath) {
        state.pathFocus = null;
        state.pathEndpointFocusActive = false;
        state.lastPaths = [];
        $("select-path-source").value = $("select-path-target").value = "";
        $("select-path-summary").textContent = "Choose a source and target to find all shortest paths.";
        $("select-paths-table").replaceChildren();
    }
    renderNodeInfo(null);
    clearEdgeSelection();
    updateSelectionTargetUI();
    if (refreshGraph) {
        applyReducers();
        syncCosmographSelection();
    }
    if (refreshTable) renderTable(state.activeTableSubtab);
}
// Shows the community-partition picker only for community view modes, and keeps its options in sync.
function groupingOptions() {
    const partitions = Object.keys(state.communityData).map((name) => ({ value: "community:" + name, label: "Community · " + name }));
    const attributes = nodeAttrDefs().map((def) => ({ value: "attribute:" + encodeURIComponent(def.name), label: "Attribute · " + def.name }));
    return [...partitions, ...attributes];
}

function groupingValue(grouping, node) {
    if (grouping.startsWith("community:")) return state.communityData[grouping.slice("community:".length)]?.[node];
    if (grouping.startsWith("attribute:")) return nodeAttrVal(node, decodeURIComponent(grouping.slice("attribute:".length)));
    // Preserve existing saved selections that stored a bare community algorithm id.
    return state.communityData[grouping]?.[node];
}

function groupingKey(value) {
    return value === undefined || value === null || value === "" ? null : typeof value + ":" + String(value);
}
function updatePartitionField() {
    if (state.selection.type === "path") { $("select-partition-field").hidden = true; return; }
    const mode = state.selection.mode;
    const isCommunity = mode === "community-highlight" || mode === "community-only";
    $("select-partition-field").hidden = !isCommunity;
    const groups = groupingOptions();
    const values = groups.map((group) => group.value), labels = groups.map((group) => group.label);
    setSelectOptions("select-partition", values, labels);
    if (!state.selection.partition || !values.includes(state.selection.partition)) state.selection.partition = values[0] || null;
    if (state.selection.partition) $("select-partition").value = state.selection.partition;
}
function selectionFocus() {
    const sel = state.selection, g = state.graph;
    if (!g || sel.mode === "none") return null;
    const edgeSelection = sel.type === "edge";
    let seeds;
    if (edgeSelection) {
        if (!state.selectedEdge || !g.hasEdge(state.selectedEdge)) return null;
        seeds = [g.source(state.selectedEdge), g.target(state.selectedEdge)];
    } else {
        if (!sel.node || !g.hasNode(sel.node)) return null;
        seeds = [sel.node];
    }
    let nodes = new Set(seeds), only = false, matchingEdges = null;
    if (edgeSelection && (sel.mode === "edge-attribute-highlight" || sel.mode === "edge-attribute-only")) {
        const attribute = sel.edgeAttribute;
        if (!attribute) return null;
        const value = edgeAttrVal(state.selectedEdge, attribute);
        matchingEdges = new Set(g.edges().filter((edge) => Object.is(edgeAttrVal(edge, attribute), value)));
        matchingEdges.forEach((edge) => { nodes.add(g.source(edge)); nodes.add(g.target(edge)); });
        only = sel.mode === "edge-attribute-only";
    }
    if (sel.mode === "ego-highlight" || sel.mode === "ego-only") {
        seeds.forEach((seed) => g.neighbors(seed).forEach((node) => nodes.add(node)));
        only = sel.mode === "ego-only";
    } else if (sel.mode === "community-highlight" || sel.mode === "community-only") {
        if (!sel.partition) return null;
        const groups = new Set(seeds.map((seed) => groupingKey(groupingValue(sel.partition, seed))).filter(Boolean));
        if (!groups.size) return null;
        g.forEachNode((node) => { if (groups.has(groupingKey(groupingValue(sel.partition, node)))) nodes.add(node); });
        only = sel.mode === "community-only";
    }
    return { nodes, only, selected: edgeSelection ? null : sel.node, edgeOnly: sel.mode === "edge-highlight", edge: edgeSelection ? state.selectedEdge : null, matchingEdges };
}
function setSelectionMode(mode) {
    state.selection.mode = mode;
    if (state.selection.type === "path") {
        if (mode === "none") {
            state.pathFocus = null;
            state.pathEndpointFocusActive = false;
        } else if (state.pathFocus) {
            state.pathFocus.only = mode === "path-only";
        }
        updateSelectionTargetUI();
        applyReducers();
        renderPathsTable(state.lastPaths);
        renderTable(state.activeTableSubtab);
        return;
    }
    updatePartitionField();
    applyReducers();
    renderTable(state.activeTableSubtab); // "show only" modes restrict the tables
}

function setSelectionEdgeAttribute(attribute) { state.selection.edgeAttribute = attribute; applyReducers(); renderTable(state.activeTableSubtab); }

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
    ["select-node-input", "select-path-source", "select-path-target", "add-edge-source", "add-edge-target", "diff-node-input", "diff-traj-node", "layout-root"]
        .forEach((id) => attachNodeCombo($(id)));
}

/* ------------------------------ layout ------------------------------ */

// Layouts that run continuously (animated) vs. one-shot layouts that are applied once.
const ITERATIVE_LAYOUTS = new Set(["force"]);
const RELISON_FORCE_LAYOUTS = new Set(["fruchterman-reingold", "relison-forceatlas2", "stress-majorization", "kamada-kawai", "multilevel-force"]);
const RELISON_LAYOUTS = new Set([...RELISON_FORCE_LAYOUTS, "geographic", "community", "feature-grid", "ego-grid", "preset", "random", "grid", "circular", "shell", "concentric", "radial", "bipartite", "multipartite", "tree", "sugiyama"]);

function currentLayoutType() {
    const sel = $("layout-type");
    return sel ? sel.value : "force";
}

// Keeps the layout button label in sync with the selected algorithm / running state.
function updateLayoutButton() {
    const button = $("btn-layout");
    const running = Boolean(state.relisonLayout || state.iterativeLayoutRunning ||
        (currentLayoutType() === "cosmograph-force" && state.cosmographLayoutRunning));
    button.classList.add("primary");
    button.classList.toggle("btn-stop", running);
    button.disabled = Boolean(state.layoutRequest) && !running;
    if (running) { button.textContent = "⏹ Stop layout"; return; }
    if (state.layoutRequest) { button.textContent = "Applying layout…"; return; }
    if (currentLayoutType() === "cosmograph-force") {
        button.textContent = "Start layout";
        return;
    }
    button.textContent = (ITERATIVE_LAYOUTS.has(currentLayoutType()) || RELISON_FORCE_LAYOUTS.has(currentLayoutType())) ? "Start layout" : "Apply layout";
}

// Button handler: toggles animated layouts, or applies a static layout once.
function usingCosmograph() { return $("network-renderer").value === "cosmograph"; }
function usingGeographic() { return $("pane-network").classList.contains("geographic-active"); }
let geographicPreviousRenderer = "cosmograph";
let geographicActiveProjection = "mercator";
let geographicPreviousPositions = null;
function geographicCoordinates(graph = state.graph, latitude = $("layout-geographic-latitude").value, longitude = $("layout-geographic-longitude").value, projection = $("layout-geographic-projection").value) {
    if (!latitude || !longitude) throw new Error("Choose numeric latitude and longitude attributes in Advanced layout settings.");
    return window.relisonGeographic.readCoordinates(graph.nodes(),
        node => layoutCoordinateFeature(latitude, node, "Latitude"), node => layoutCoordinateFeature(longitude, node, "Longitude"), projection);
}
function leaveGeographicView(nextRenderer = geographicPreviousRenderer) {
    window.relisonGeographic?.destroy();
    const saved = geographicPreviousPositions;
    geographicPreviousPositions = null;
    if (saved?.graph === state.graph) {
        for (const [node, position] of saved.positions) {
            if (state.graph.hasNode(node)) state.graph.mergeNodeAttributes(node, position);
        }
        state.cosmographDirty = true;
        state.renderer?.refresh();
    }
    $("pane-network").classList.remove("geographic-active");
    if ($("network-renderer").value === "geographic") $("network-renderer").value = nextRenderer;
    for (const id of ["btn-noverlap", "btn-reset-layout", "drag-nodes", "edit-mode"]) $(id).disabled = false;
}
async function applyGeographicLayout() {
    const previous = usingGeographic() ? "geographic" : $("pane-network").classList.contains("cosmograph-active") ? "cosmograph" : "sigma";
    if (!state.graph) { $("network-renderer").value = previous === "geographic" ? geographicPreviousRenderer : previous; setStatus("Load a network first.", "error"); return; }
    stopLayout();
    const graph = state.graph, graphId = state.graphId, controller = new AbortController();
    state.layoutRequest = controller; updateLayoutButton();
    try {
        const latitude = $("layout-geographic-latitude").value, longitude = $("layout-geographic-longitude").value;
        const projection = $("layout-geographic-projection").value;
        const coordinates = geographicCoordinates();
        await window.relisonGeographic.ensureLoaded(projection);
        if (controller.signal.aborted || state.graph !== graph) return;
        const result = await api("/api/layout", { ...jsonBody({ graphId, algorithm: "geographic", nodeOrder: graph.nodes(),
            latitudes: coordinates.latitudes, longitudes: coordinates.longitudes, params: { centralLongitude: coordinates.center, projection } }), signal: controller.signal });
        if (controller.signal.aborted || state.graph !== graph) return;
        await window.relisonGeographic.render(graph, {
            projection,
            isCurrent: () => !controller.signal.aborted && state.graph === graph,
            overlays: () => geographicRecommendationEdges(),
            legend: () => state.colorLegend,
            coordinates: () => geographicCoordinates(graph, latitude, longitude, projection),
            nodeDisplay: (node, data) => state.renderer?.getSetting("nodeReducer")?.(node, { ...data }) || data,
            edgeDisplay: (edge, data) => state.renderer?.getSetting("edgeReducer")?.(edge, { ...data }) || data,
            selectedNode: () => state.selectedNode, labels: () => state.labelOpts,
            onNodeClick: selectNode, onEdgeClick: selectEdge, onClearSelection: clearSelection,
            onError: message => setStatus(message, "error"),
        });
        if (controller.signal.aborted || state.graph !== graph) { window.relisonGeographic.destroy(); return; }
        // Validate the complete result before replacing shared coordinates.
        for (const node of graph.nodes()) {
            const p = result.positions?.[node];
            if (!p || !Number.isFinite(p.x) || !Number.isFinite(p.y)) throw new Error("Incomplete geographic layout coordinates.");
        }
        // Retain the original layout across repeated map applications and
        // projection switches. Map renderers read latitude/longitude directly.
        if (geographicPreviousPositions?.graph !== graph) {
            geographicPreviousPositions = { graph, positions: new Map(graph.nodes().map(node => [node, {
                x: graph.getNodeAttribute(node, "x"), y: graph.getNodeAttribute(node, "y"),
            }])) };
        }
        for (const node of graph.nodes()) {
            const p = result.positions[node];
            graph.mergeNodeAttributes(node, { x: p.x, y: p.y });
        }
        if (previous !== "geographic") geographicPreviousRenderer = previous;
        $("pane-network").classList.remove("cosmograph-active");
        $("cosmograph-container").hidden = true;
        $("pane-network").classList.add("geographic-active");
        $("network-renderer").value = "geographic";
        geographicActiveProjection = projection;
        $("edit-mode").checked = false;
        setEditMode(false);
        for (const id of ["btn-noverlap", "btn-reset-layout", "drag-nodes", "edit-mode"]) $(id).disabled = true;
        toggleHidden("edit-help", true); toggleHidden("network-drag-hint", true);
        state.cosmographDirty = true;
        setStatus("Geographic network ready. Pan and zoom the background map; click nodes or edges to select.");
    } catch (error) {
        if (!isAbort(error)) setStatus("Geographic view failed: " + error.message, "error");
        $("network-renderer").value = previous === "geographic" ? "geographic" : previous;
        if (previous !== "geographic") window.relisonGeographic.destroy();
    } finally {
        if (state.layoutRequest === controller) { state.layoutRequest = null; updateLayoutButton(); }
    }
}

// Recommendations are transient results, rather than Graphology edges, so
// Cosmograph receives them as an additional projected link list. Returning
// whether it changed lets an already-created Cosmograph canvas stay intact
// when switching renderers without changing any recommendation setting.
function syncCosmographRecommendation() {
    const model = state.rec.active && state.rec.models[state.rec.active];
    const next = {
        show: Boolean(model && state.rec.show),
        edges: model ? model.edges : [],
        color: state.rec.color,
        diff: Boolean(state.rec.diff),
        directed: Boolean(state.directed),
    };
    const signature = JSON.stringify(next);
    const changed = !window.relisonCosmographRecommendation || window.relisonCosmographRecommendation.signature !== signature;
    window.relisonCosmographRecommendation = { ...next, signature };
    return changed;
}
function geographicRecommendationEdges(model = state.rec.active && state.rec.models[state.rec.active], show = state.rec.show) {
    return show && model ? model.edges.map(edge => ({ ...edge, color: state.rec.color, dashed: state.rec.diff,
        label: "Recommended: " + edge.source + " → " + edge.target })) : [];
}

function syncLayoutOptionsForRenderer() {
    const active = usingCosmograph();
    const select = $("layout-type");
    const cosmographForce = select.querySelector('option[value="cosmograph-force"]');
    cosmographForce.hidden = !active;
    cosmographForce.disabled = !active;
    for (const value of ITERATIVE_LAYOUTS) {
        const option = select.querySelector('option[value="' + value + '"]');
        if (option) { option.hidden = active; option.disabled = active; }
    }
    if (active && ITERATIVE_LAYOUTS.has(select.value)) select.value = "circular";
    if (!active && select.value === "cosmograph-force") select.value = "circular";
    updateLayoutButton();
    updateLayoutTypeUI();
}

async function toggleCosmographForceLayout() {
    if (!window.relisonCosmograph) return;
    if (state.cosmographLayoutRunning) {
        const saved = window.relisonCosmograph.stopForceLayout();
        state.cosmographLayoutRunning = false;
        if (state.renderer) state.renderer.refresh();
        drawRecOverlay();
        setStatus("Cosmograph force layout stopped; saved positions for " + saved + " nodes.");
    } else {
        state.cosmographLayoutRunning = true;
        updateLayoutButton();
        try {
            if (await window.relisonCosmograph.startForceLayout()) {
                setStatus("Cosmograph force layout running.");
            } else {
                state.cosmographLayoutRunning = false;
                updateLayoutButton();
            }
        } catch (error) {
            state.cosmographLayoutRunning = false;
            setStatus("Could not start Cosmograph force layout: " + error.message, "error");
        }
    }
    updateLayoutButton();
}

async function onLayoutButton() {
    if (!state.graph) { setStatus("Load a network first.", "error"); return; }
    const type = currentLayoutType();
    if (type === "geographic") return applyGeographicLayout();
    if (usingGeographic()) {
        leaveGeographicView();
        await setNetworkRenderer(geographicPreviousRenderer);
    }
    if (RELISON_FORCE_LAYOUTS.has(type)) {
        if (state.relisonLayout) stopLayout();
        else return startRelisonAnimation(type);
        return;
    }
    if (usingCosmograph()) {
        if (type === "cosmograph-force") { toggleCosmographForceLayout(); return; }
        if (ITERATIVE_LAYOUTS.has(type)) { setStatus("Use Cosmograph force layout or a static layout in this view.", "error"); return; }
    }
    if (ITERATIVE_LAYOUTS.has(type)) {
        if (state.iterativeLayoutRunning) { stopLayout(); return; }
        startIterativeLayout(type);
    } else {
        applyStaticLayout(type);
    }
}

// Starts the generic graphology force layout, falling back to the built-in stepper.
function startIterativeLayout(type) {
    let stepFn;
    if (type === "force" && lib.layoutForce && typeof lib.layoutForce.assign === "function") {
        stepFn = () => lib.layoutForce.assign(state.graph, { maxIterations: 1 });
    } else {
        stepFn = builtinForceStep;
    }

    state.iterativeLayoutRunning = true;
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
        if (state.iterativeLayoutRunning) state.iterativeLayoutRaf = requestAnimationFrame(step);
    };
    step();
}

// Advance one server session sequentially; each response becomes a displayed frame.
function relisonForceBody(type, g, graphId) {
    const nodes = g.nodes().sort();
    const body = { graphId, algorithm: type, nodeOrder: nodes, params: staticLayoutParams(type) };
    if ($("layout-warm-start").checked) {
        body.positions = Object.fromEntries(nodes.map(node => [node, {
            x: g.getNodeAttribute(node, "x"), y: g.getNodeAttribute(node, "y"),
        }]));
        if (nodes.some(node => !Number.isFinite(body.positions[node].x) || !Number.isFinite(body.positions[node].y)))
            throw new Error("Apply a layout before using current positions.");
    }
    body.params.removeOverlap = $("layout-remove-overlap").checked;
    if (body.params.removeOverlap || body.params.adjustSizes) {
        const radius = Number($("layout-node-radius").value), gap = Number($("layout-overlap-gap").value);
        if (!Number.isFinite(radius) || radius < 0 || !Number.isFinite(gap) || gap < 0)
            throw new Error("Node radius and overlap gap must be finite and non-negative.");
        body.nodeSizes = Object.fromEntries(nodes.map(node => [node, radius]));
        body.params.overlapGap = gap;
    }
    body.params.packComponents = $("layout-pack-components").checked;
    if (body.params.packComponents) {
        const gap = Number($("layout-packing-gap").value);
        if (!Number.isFinite(gap) || gap <= 0) throw new Error("Component gap must be positive and finite.");
        body.params.packingGap = gap;
    }
    return body;
}

function cancelRelisonSession(run) {
    if (!run.id || run.remoteCancelled) return Promise.resolve();
    run.remoteCancelled = true;
    return api("/api/layout/session/" + encodeURIComponent(run.id), { method: "DELETE" }).catch(() => {});
}

function waitRelisonFrame(run) {
    return new Promise(resolve => {
        run.resolveFrame = resolve;
        run.raf = requestAnimationFrame(() => { run.resolveFrame = null; run.raf = null; resolve(); });
    });
}

async function startRelisonAnimation(type) {
    stopLayout();
    const g = state.graph, graphId = state.graphId;
    const run = { controller: new AbortController(), id: null, raf: null, resolveFrame: null, remoteCancelled: false };
    const current = () => state.relisonLayout === run && !run.controller.signal.aborted && state.graph === g && state.graphId === graphId;
    try {
        const body = relisonForceBody(type, g, graphId), nodes = body.nodeOrder;
        state.relisonLayout = run;
        updateLayoutButton();
        setStatus("Starting " + type + " layout…");
        // Keep the start response readable after Stop, so its allocated session can be cancelled.
        let frame = await api("/api/layout/session", jsonBody(body));
        if (typeof frame.sessionId !== "string") throw new Error("Missing layout session identifier.");
        run.id = frame.sessionId;
        while (current()) {
            const positions = frame.positions;
            if (!positions || Object.keys(positions).length !== g.order || nodes.length !== g.order
                || nodes.some(node => !g.hasNode(node) || !Object.hasOwn(positions, node)
                    || !Number.isFinite(positions[node]?.x) || !Number.isFinite(positions[node]?.y)))
                throw new Error("The network changed or an animation frame has incomplete coordinates.");
            for (const node of nodes) {
                g.setNodeAttribute(node, "x", positions[node].x);
                g.setNodeAttribute(node, "y", positions[node].y);
            }
            const cosmographVisible = usingCosmograph() && window.relisonCosmograph
                && $("pane-network").classList.contains("active");
            if (cosmographVisible) {
                const updated = await window.relisonCosmograph.setNodePositions(positions);
                if (!current()) break;
                if (!updated) {
                    clearTimeout(cosmographAppearanceRefreshTimer);
                    await window.relisonCosmograph.render(g, { ...cosmographInteractionCallbacks() });
                }
            } else if (usingCosmograph()) state.cosmographDirty = true;
            // Graphology coordinate events schedule Sigma's render. No full refresh
            // or viewport rescale is needed for a frame that changes positions only.
            drawRecOverlay();
            if (!current()) break;
            setStatus(type + ": " + frame.iterations + " iterations" + (frame.finished
                ? " (" + String(frame.termination).toLowerCase().replaceAll("_", " ") + ")." : "…"));
            if (frame.finished) break;
            await waitRelisonFrame(run);
            if (!current()) break;
            frame = await api("/api/layout/session/" + encodeURIComponent(run.id) + "/step",
                { ...jsonBody({ iterations: 1 }), signal: run.controller.signal });
        }
    } catch (error) {
        if (current() && !isAbort(error)) setStatus("Layout failed: " + error.message, "error");
    } finally {
        if (run.raf) cancelAnimationFrame(run.raf);
        await cancelRelisonSession(run);
        if (state.relisonLayout === run) { state.relisonLayout = null; updateLayoutButton(); }
    }
}

// RELISON-viz computes Stage A layouts on the server; circle packing remains a client layout.
async function applyStaticLayout(type) {
    stopLayout();
    const g = state.graph;
    const graphId = state.graphId;
    let controller = null;
    let forceSummary = "";
    try {
        if (RELISON_LAYOUTS.has(type)) {
            const nodes = g.nodes().sort();
            const body = { graphId, algorithm: type, nodeOrder: nodes, params: staticLayoutParams(type) };
            if (RELISON_FORCE_LAYOUTS.has(type)) {
                if ($("layout-warm-start").checked) body.positions = Object.fromEntries(nodes.map(node => [node, {
                    x: g.getNodeAttribute(node, "x"), y: g.getNodeAttribute(node, "y"),
                }]));
                body.params.removeOverlap = $("layout-remove-overlap").checked;
                if (body.params.removeOverlap || body.params.adjustSizes) {
                    const radius = Number($("layout-node-radius").value);
                    const gap = Number($("layout-overlap-gap").value);
                    if (!Number.isFinite(radius) || radius < 0 || !Number.isFinite(gap) || gap < 0)
                        throw new Error("Node radius and overlap gap must be finite and non-negative.");
                    body.nodeSizes = Object.fromEntries(nodes.map(node => [node, radius]));
                    body.params.overlapGap = gap;
                }
            }
            body.params.packComponents = $("layout-pack-components").checked;
            if (body.params.packComponents) {
                const gap = Number($("layout-packing-gap").value);
                if (!Number.isFinite(gap) || gap <= 0) throw new Error("Component gap must be positive and finite.");
                body.params.packingGap = gap;
            }
            if (type === "preset") {
                const xFeature = $("layout-preset-x").value;
                const yFeature = $("layout-preset-y").value;
                const saved = state.savedLayoutPositions;
                if (!xFeature || !yFeature) {
                    if (!saved || saved.graphId !== graphId) throw new Error("Save positions for this network first, or choose a numeric feature for both axes.");
                    if (Object.keys(saved.positions).length !== nodes.length || nodes.some(node => !Object.hasOwn(saved.positions, node)))
                        throw new Error("The network's nodes changed. Save positions again before restoring them.");
                }
                body.positions = Object.fromEntries(nodes.map(node => [node, {
                    x: xFeature ? layoutCoordinateFeature(xFeature, node, "X") : saved.positions[node].x,
                    y: yFeature ? layoutCoordinateFeature(yFeature, node, "Y") : saved.positions[node].y,
                }]));
            }
            if (["shell", "bipartite", "multipartite", "feature-grid", "community"].includes(type)) {
                const groupBy = $("circlepack-group").value;
                if (type === "community" && !groupBy) throw new Error("Choose a community or node attribute under Group by.");
                if ((type === "multipartite" || type === "feature-grid") && !groupBy) throw new Error("Choose a grouping for this column layout.");
                const groups = new Map();
                for (const node of nodes) {
                    const key = groupBy ? String(nodeGroupKey(groupBy, node) ?? "?") : "all";
                    if (!groups.has(key)) groups.set(key, []);
                    groups.get(key).push(node);
                }
                const parts = [...groups.keys()].sort().map(key => groups.get(key));
                if (type === "shell") body.shells = parts;
                else if (type === "multipartite" || type === "feature-grid" || groupBy) {
                    if (nodes.length === 0 && type !== "community") parts.push([], []);
                    if (type === "bipartite" && parts.length !== 2) throw new Error("Bipartite grouping must have exactly two groups.");
                    if (type === "multipartite" && parts.length < 2) throw new Error("Multipartite grouping must have at least two groups.");
                    body.partitions = parts;
                }
            }
            if (type === "concentric" && $("layout-score").value !== "degree") {
                const spec = $("layout-score").value;
                body.scores = Object.fromEntries(nodes.map(node => {
                    const value = spec.startsWith("attr:") ? nodeAttrVal(node, spec.slice(5))
                        : state.metricData[spec.slice(7)]?.[node];
                    if (value === null || value === undefined || value === "" || !Number.isFinite(Number(value)))
                        throw new Error("Score is missing or non-finite for node " + node + ". Choose a score available for every node.");
                    return [node, Number(value)];
                }));
            }
            controller = new AbortController();
            state.layoutRequest = controller;
            updateLayoutButton();
            const response = await api("/api/layout", { ...jsonBody(body), signal: controller.signal });
            // A replaced graph or cancelled request must never receive stale coordinates.
            if (controller.signal.aborted || state.layoutRequest !== controller || state.graph !== g || state.graphId !== graphId) return;
            if (RELISON_FORCE_LAYOUTS.has(type) && Number.isInteger(response.iterations))
                forceSummary = " (" + response.iterations + " iterations, " + String(response.termination).toLowerCase().replaceAll("_", " ") + ")";
            const positions = response.positions;
            if (!positions || Object.keys(positions).length !== g.order || nodes.length !== g.order
                || nodes.some(node => !g.hasNode(node) || !Object.hasOwn(positions, node)
                    || !Number.isFinite(positions[node]?.x) || !Number.isFinite(positions[node]?.y)))
                throw new Error("The network changed or the layout returned incomplete coordinates. Apply it again.");
            for (const node of nodes) {
                g.setNodeAttribute(node, "x", positions[node].x);
                g.setNodeAttribute(node, "y", positions[node].y);
            }
        } else if (type === "circlepack") nativeCirclepack(g, $("circlepack-group") ? $("circlepack-group").value : "");
        else { setStatus("Unknown layout: " + type, "error"); return; }
        if (state.renderer) {
            state.renderer.setCustomBBox(null);
            state.renderer.refresh();
        }
        drawRecOverlay();
        queueCosmographAppearanceRefresh();
        const label = $("layout-type").selectedOptions[0]?.textContent || type;
        setStatus(label + " layout applied" + forceSummary + ".");
    } catch (e) {
        if (!isAbort(e)) setStatus("Layout failed: " + e.message, "error");
    } finally {
        if (controller && state.layoutRequest === controller) {
            state.layoutRequest = null;
            updateLayoutButton();
        }
    }
}

function staticLayoutParams(type) {
    const read = (id, integer = false, minimum = Number.MIN_VALUE) => {
        const input = $(id);
        const value = input.value === "" ? NaN : Number(input.value);
        if (!Number.isFinite(value) || value < minimum || (integer && !Number.isSafeInteger(value)))
            throw new Error(input.closest("label").querySelector("span").textContent + " is invalid.");
        return value;
    };
    if (RELISON_FORCE_LAYOUTS.has(type)) {
        const common = { iterations: read("layout-iterations", true, 0), seed: read("layout-seed", true, -Number.MAX_SAFE_INTEGER),
            theta: read("layout-theta", false, 0), tolerance: read("layout-tolerance", false, 0) };
        if (common.iterations > 5000 || common.theta > 2) throw new Error("Iterations must be at most 5000 and theta at most 2.");
        if (type === "stress-majorization") return { ...common, edgeLength: read("layout-distance-length"), weighted: $("layout-distance-weighted").checked };
        if (type === "kamada-kawai") return { ...common, drawingSize: read("layout-kk-size"), springConstant: read("layout-spring-constant"), weighted: $("layout-distance-weighted").checked };
        if (type === "multilevel-force") {
            const cooling = read("layout-hu-cooling");
            const levelIterations = read("layout-hu-level-iterations", true, 1);
            if (cooling >= 1 || levelIterations > 5000 || common.tolerance <= 0) throw new Error("Cooling must be below one, level iterations at most 5000, and tolerance positive.");
            return { ...common, initialScale: read("layout-hu-scale"), repulsion: read("layout-hu-repulsion"), cooling, levelIterations,
                weighted: $("layout-hu-weighted").checked };
        }
        if (type === "fruchterman-reingold") {
            if ($("layout-fr-bounded").checked) return { ...common, bounded: true,
                width: read("layout-fr-width"), height: read("layout-fr-height"),
                initialTemperature: read("layout-fr-temperature"), weighted: $("layout-weighted").checked };
            const cooling = read("layout-cooling");
            if (cooling >= 1) throw new Error("Cooling must be less than one.");
            return { ...common, bounded: false, idealLength: read("layout-ideal-length"), cooling, weighted: $("layout-weighted").checked };
        }
        return { ...common, scaling: read("layout-fa-scaling"), gravity: read("layout-fa-gravity", false, 0),
            jitterTolerance: read("layout-jitter"), weightInfluence: read("layout-weight-influence", false, 0),
            linLog: $("layout-linlog").checked, strongGravity: $("layout-strong-gravity").checked,
            outboundAttractionDistribution: $("layout-outbound").checked, adjustSizes: $("layout-adjust-sizes").checked,
            normalizeWeights: $("layout-normalize-weights").checked, invertWeights: $("layout-invert-weights").checked };
    }
    if (type === "community") return { iterations: read("layout-community-iterations", true, 0),
        seed: read("layout-seed", true, -Number.MAX_SAFE_INTEGER), communityGap: read("layout-community-gap", false, 0),
        innerLayout: $("layout-community-inner").value, outerLayout: $("layout-community-outer").value };
    if (type === "circular") return { radius: read("layout-radius") };
    if (type === "random") return { width: read("layout-width"), height: read("layout-height"), seed: read("layout-seed", true, -Number.MAX_SAFE_INTEGER) };
    if (type === "grid") return { columns: read("layout-columns", true, 0), spacing: read("layout-spacing") };
    if (type === "shell" || type === "concentric") return { spacing: read("layout-spacing"), reverse: type === "concentric" && $("layout-score-reverse").checked };
    if (type === "radial") return { root: $("layout-root").value || null, direction: $("layout-direction").value, spacing: read("layout-spacing") };
    if (type === "ego-grid") return { root: $("layout-root").value || null, direction: $("layout-direction").value, columnSpacing: read("layout-column-spacing"), spacing: read("layout-spacing") };
    if (type === "tree") return { root: $("layout-root").value || null, spacing: read("layout-spacing"), levelSpacing: read("layout-level-spacing") };
    if (type === "sugiyama") return { spacing: read("layout-spacing"), levelSpacing: read("layout-level-spacing"), sweeps: read("layout-sweeps", true, 0) };
    if (type === "bipartite" || type === "multipartite" || type === "feature-grid") return {
        columnSpacing: read("layout-column-spacing"), spacing: read("layout-spacing"), sweeps: read("layout-sweeps", true, 0),
    };
    return {};
}

// Coordinate features retain their original units and are never silently filled or normalized.
function layoutCoordinateFeature(spec, node, axis) {
    let value;
    if (spec.startsWith("attr:")) value = nodeAttrVal(node, spec.slice(5));
    else throw new Error("Choose a numeric node attribute or saved coordinate for the " + axis + " axis.");
    if ((typeof value !== "number" && typeof value !== "string")
        || (typeof value === "string" && value.trim() === "") || !Number.isFinite(Number(value)))
        throw new Error(axis + " feature is missing or non-finite for node " + node + ". Choose a feature available for every node.");
    return Number(value);
}

function saveLayoutPositions() {
    if (!state.graph) { setStatus("Load a network first.", "error"); return; }
    stopLayout();
    if (usingCosmograph()) window.relisonCosmograph?.savePointPositions();
    const entries = state.graph.nodes().map(node => [node, {
        x: state.graph.getNodeAttribute(node, "x"), y: state.graph.getNodeAttribute(node, "y"),
    }]);
    if (entries.some(([, point]) => !Number.isFinite(point.x) || !Number.isFinite(point.y))) {
        setStatus("Apply a layout before saving positions.", "error"); return;
    }
    state.savedLayoutPositions = { graphId: state.graphId, positions: Object.fromEntries(entries) };
    setStatus("Saved positions for " + entries.length + " nodes.");
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
        xs[i] = x; ys[i] = y; rs[i] = (g.getNodeAttribute(nd, "size") || 6) / 2;
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

// Populates layout grouping and scoring choices from the available analysis results.
function rebuildLayoutGroupOptions() {
    const sel = $("circlepack-group");
    if (!sel) return;
    const current = sel.value;
    sel.innerHTML = '<option value="">— none —</option>';
    Object.keys(state.communityData).forEach((a) => sel.appendChild(option("community:" + a, "community: " + a)));
    nodeAttrDefs().forEach((d) => sel.appendChild(option("attr:" + d.name, "attr: " + d.name)));
    restoreSelect(sel, current);
    const score = $("layout-score");
    const previousScore = score.value;
    score.innerHTML = '<option value="degree">Degree</option>';
    state.metricOrder.forEach(name => score.appendChild(option("metric:" + name, "metric: " + name)));
    nodeAttrDefs().filter(def => def.numeric).forEach(def => score.appendChild(option("attr:" + def.name, "attr: " + def.name)));
    score.value = Array.from(score.options).some(item => item.value === previousScore) ? previousScore : "degree";
    for (const axis of ["x", "y"]) {
        const coordinate = $("layout-preset-" + axis);
        const previous = coordinate.value;
        coordinate.innerHTML = '<option value="">Saved ' + axis.toUpperCase() + ' coordinate</option>';
        nodeAttrDefs().filter(def => def.numeric).forEach(def => coordinate.appendChild(option("attr:" + def.name, "attr: " + def.name)));
        restoreSelect(coordinate, previous);
    }
    for (const axis of ["latitude", "longitude"]) {
        const select = $("layout-geographic-" + axis), previous = select.value;
        select.replaceChildren(option("", "Choose " + axis));
        for (const def of nodeAttrDefs().filter(def => def.numeric)) select.appendChild(option("attr:" + def.name, def.name));
        restoreSelect(select, previous);
        if (!select.value) {
            const names = axis === "latitude" ? ["latitude", "lat"] : ["longitude", "lon", "lng", "long"];
            const def = nodeAttrDefs().find(def => def.numeric && names.includes(def.name.toLowerCase()));
            if (def) select.value = "attr:" + def.name;
        }
    }
}

// Shows only the controls relevant to the selected layout.
function updateLayoutTypeUI() {
    const type = currentLayoutType();
    toggleHidden("layout-geographic-controls", type !== "geographic");
    toggleHidden("layout-geographic-hint", type !== "geographic");
    const force = RELISON_FORCE_LAYOUTS.has(type);
    toggleHidden("layout-force-controls", !force);
    toggleHidden("layout-fr-controls", type !== "fruchterman-reingold");
    const framedFR = $("layout-fr-bounded").checked;
    for (const id of ["layout-fr-width", "layout-fr-height", "layout-fr-temperature"])
        $(id).closest("label").hidden = !framedFR;
    for (const id of ["layout-ideal-length", "layout-cooling"])
        $(id).closest("label").hidden = framedFR;
    toggleHidden("layout-fa-controls", type !== "relison-forceatlas2");
    const distance = type === "stress-majorization" || type === "kamada-kawai";
    toggleHidden("layout-distance-controls", !distance);
    $("layout-distance-length").closest("label").hidden = type !== "stress-majorization";
    for (const id of ["layout-kk-size", "layout-spring-constant"]) $(id).closest("label").hidden = type !== "kamada-kawai";
    $("layout-theta").closest("label").hidden = distance;
    toggleHidden("layout-hu-controls", type !== "multilevel-force");
    toggleHidden("layout-community-controls", type !== "community");
    $("layout-tolerance").dataset.tooltip = type === "stress-majorization" ? "ui-layout-stress-tolerance"
        : type === "kamada-kawai" ? "ui-layout-kk-tolerance" : type === "multilevel-force" ? "ui-layout-hu-tolerance" : "ui-layout-tolerance";
    $("layout-tolerance").closest("label").dataset.tooltip = $("layout-tolerance").dataset.tooltip;
    toggleHidden("layout-distance-hint", !distance);
    toggleHidden("layout-hu-hint", type !== "multilevel-force");
    toggleHidden("layout-community-hint", type !== "community");
    toggleHidden("layout-force-hint", !force);
    toggleHidden("layout-advanced", !RELISON_LAYOUTS.has(type) && type !== "circlepack");
    toggleHidden("circlepack-group-field", !["circlepack", "shell", "bipartite", "multipartite", "feature-grid", "community"].includes(type));
    toggleHidden("static-layout-params", !RELISON_LAYOUTS.has(type) || type === "preset");
    toggleHidden("layout-radius-field", type !== "circular");
    toggleHidden("layout-spacing-field", !["grid", "shell", "concentric", "radial", "bipartite", "multipartite", "tree", "feature-grid", "ego-grid", "sugiyama"].includes(type));
    toggleHidden("layout-root-field", type !== "radial" && type !== "ego-grid" && type !== "tree");
    toggleHidden("layout-direction-field", type !== "radial" && type !== "ego-grid");
    toggleHidden("layout-column-spacing-field", !["bipartite", "multipartite", "feature-grid", "ego-grid"].includes(type));
    toggleHidden("layout-sweeps-field", !["bipartite", "multipartite", "feature-grid", "sugiyama"].includes(type));
    toggleHidden("layout-level-spacing-field", type !== "tree" && type !== "sugiyama");
    toggleHidden("layout-columns-field", type !== "grid");
    for (const id of ["layout-width-field", "layout-height-field"]) toggleHidden(id, type !== "random");
    toggleHidden("layout-seed-field", type !== "random" && type !== "community" && !force);
    toggleHidden("layout-score-field", type !== "concentric");
    toggleHidden("layout-score-reverse-field", type !== "concentric");
    toggleHidden("layout-shell-hint", type !== "shell");
    toggleHidden("layout-preset-hint", type !== "preset");
    toggleHidden("layout-preset-x-field", type !== "preset");
    toggleHidden("layout-preset-y-field", type !== "preset");
    toggleHidden("layout-radial-hint", type !== "radial");
    toggleHidden("layout-ego-grid-hint", type !== "ego-grid");
    toggleHidden("layout-feature-grid-hint", type !== "feature-grid");
    toggleHidden("layout-partition-hint", type !== "bipartite" && type !== "multipartite");
    toggleHidden("layout-tree-hint", type !== "tree");
    toggleHidden("layout-sugiyama-hint", type !== "sugiyama");
    toggleHidden("layout-packing-controls", !RELISON_LAYOUTS.has(type) || type === "geographic");
    toggleHidden("layout-packing-gap-field", !$("layout-pack-components").checked);
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
    queueCosmographAppearanceRefresh();
    setStatus("Removed node overlaps.");
}

function builtinForceStep() {
    const g = state.graph;
    const nodes = g.nodes();
    const n = nodes.length || 1;
    const k = Math.max(10, Math.sqrt(250000 / n));
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
    const gpull = 0.01;
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
    if (state.relisonLayout) {
        const run = state.relisonLayout;
        state.relisonLayout = null;
        run.controller.abort();
        if (run.raf) cancelAnimationFrame(run.raf);
        if (run.resolveFrame) { run.resolveFrame(); run.resolveFrame = null; }
        cancelRelisonSession(run);
        setStatus("Layout stopped; keeping displayed positions.");
    }
    if (state.layoutRequest) {
        state.layoutRequest.abort();
        state.layoutRequest = null;
    }
    if (state.cosmographLayoutRunning) {
        window.relisonCosmograph?.stopForceLayout();
        state.cosmographLayoutRunning = false;
    }
    const wasRunning = state.iterativeLayoutRunning;
    state.iterativeLayoutRunning = false;
    if (state.iterativeLayoutRaf) cancelAnimationFrame(state.iterativeLayoutRaf);
    state.iterativeLayoutRaf = null;
    updateLayoutButton();
    if (wasRunning) queueCosmographAppearanceRefresh();
}

function resetLayout() {
    const graph = state.graph;
    if (!graph) return;
    $("layout-type").value = "circular";
    updateLayoutTypeUI();
    applyStaticLayout("circular");
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
        fillAttributeMetricSelect("attribute-indiv-comm-metric", cat.communityIndividual);
        fillAttributeMetricSelect("attribute-global-comm-metric", cat.communityGlobal);
        renderParams("vertex-params", "vertex", $("vertex-metric").value);
        renderParams("graph-params", "graph", $("graph-metric").value);
        renderParams("pair-params", "pair", $("pair-metric").value);
        renderParams("community-params", "community", $("community-algo").value);
        renderParams("indiv-comm-params", "communityIndividual", $("indiv-comm-metric").value);
        renderParams("global-comm-params", "communityGlobal", $("global-comm-metric").value);
        renderParams("attribute-indiv-comm-params", "communityIndividual", $("attribute-indiv-comm-metric").value);
        renderParams("attribute-global-comm-params", "communityGlobal", $("attribute-global-comm-metric").value);
        syncAttributeCommunityMetricControls();
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

// Uses the same metric identifiers as community metrics, but frames their labels for attribute-induced partitions.
function fillAttributeMetricSelect(id, items) {
    const select = $(id);
    select.innerHTML = "";
    (items || []).forEach((metric) => {
        const label = metric.label.replace(/\bcommunities\b/gi, "attributes").replace(/\bcommunity\b/gi, "attribute");
        select.appendChild(option(metric.id, label));
    });
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

// Keeps the attribute-partition metric controls in sync with the current node-attribute schema.
function syncAttributeCommunityMetricControls() {
    const select = $("attribute-comm-attribute");
    if (!select) return;
    const current = select.value;
    const attributes = nodeAttrDefs().map((def) => def.name);
    setSelectOptions("attribute-comm-attribute", attributes, attributes);
    if (attributes.includes(current)) select.value = current;
    const enabled = attributes.length > 0;
    $("btn-attribute-global-comm").disabled = !enabled;
    $("btn-attribute-indiv-comm").disabled = !enabled;
}
function syncEdgeAttributeMetricControls() {
    const select = $("edge-attribute-metric-attribute");
    if (!select) return;
    const current = select.value;
    const attributes = edgeAttrDefs().map((def) => def.name);
    setSelectOptions("edge-attribute-metric-attribute", attributes, attributes);
    if (attributes.includes(current)) select.value = current;
    $("btn-edge-attribute-global").disabled = attributes.length === 0;
    $("btn-edge-attribute-indiv").disabled = attributes.length === 0;
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
    if (jobBusy("btn-vertex")) return cancelJob("btn-vertex");
    if (!requireGraph()) return;
    const metric = $("vertex-metric").value;
    setStatus("Computing " + metric + "…", "busy");
    const signal = beginJob("btn-vertex", "vertex");
    try {
        const res = await api("/api/metrics/vertex",
            { ...jsonBody({ graphId: state.graphId, metric, params: collectParams("vertex-params") }), signal });
        if (res.cancelled) { setStatus("Stopped."); return; }
        const values = {};
        for (const k of Object.keys(res.values)) values[k] = Number(res.values[k]);
        if (!state.metricOrder.includes(res.label)) state.metricOrder.push(res.label);
        state.metricData[res.label] = values;
        rebuildAppearanceOptions();
        $("size-by").value = res.label;
        $("node-color-mode").value = "attribute";
        $("color-by").value = "metric:" + res.label;
        syncNodeColorControls();
        applyAppearance();
        $("vertex-result").textContent = res.label + " — avg " + fmt(res.average);
        if (state.rec.active && $("vertex-rec").checked) {
            try {
                const r2 = await api("/api/metrics/vertex", { ...jsonBody(
                    { graphId: state.graphId, metric, params: collectParams("vertex-params"), withRecommendation: true }), signal });
                if (!r2.cancelled) storeRecMetric("vertex", res.label, numericMap(r2.values));
            } catch (e) { /* recommendation metric is optional */ }
        }
        refreshAfterCompute();
        setStatus("Computed " + res.label + ".");
    } catch (e) {
        if (isAbort(e)) return;
        setStatus(e.message, "error");
    } finally {
        endJob("btn-vertex");
    }
}

async function runGraphMetric() {
    if (jobBusy("btn-graph")) return cancelJob("btn-graph");
    if (!requireGraph()) return;
    const metric = $("graph-metric").value;
    setStatus("Computing " + metric + "…", "busy");
    const signal = beginJob("btn-graph", "graph");
    try {
        const res = await api("/api/metrics/graph",
            { ...jsonBody({ graphId: state.graphId, metric, params: collectParams("graph-params") }), signal });
        if (res.cancelled) { setStatus("Stopped."); return; }
        state.graphMetrics[res.label] = res.value;
        if (state.rec.active && $("graph-rec").checked) {
            try {
                const r2 = await api("/api/metrics/graph", { ...jsonBody(
                    { graphId: state.graphId, metric, params: collectParams("graph-params"), withRecommendation: true }), signal });
                if (!r2.cancelled) storeRecMetric("graph", res.label, r2.value);
            } catch (e) { /* recommendation metric is optional */ }
        }
        refreshAfterCompute();
        setStatus("Computed " + res.label + " = " + fmt(res.value));
    } catch (e) {
        if (isAbort(e)) return;
        setStatus(e.message, "error");
    } finally {
        endJob("btn-graph");
    }
}

async function runPairMetric() {
    if (jobBusy("btn-pair")) return cancelJob("btn-pair");
    if (!requireGraph()) return;
    const metric = $("pair-metric").value;
    const allPairs = $("pair-allpairs").checked;
    const onlyLinks = !allPairs;
    setStatus("Computing " + metric + (allPairs ? " over all node pairs" : "") + "…", "busy");
    const signal = beginJob("btn-pair", "pair");
    try {
        const res = await api("/api/metrics/pair",
            { ...jsonBody({ graphId: state.graphId, metric, onlyLinks, params: collectParams("pair-params") }), signal });
        if (res.cancelled) { setStatus("Stopped."); return; }
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
                const r2 = await api("/api/metrics/pair", { ...jsonBody(
                    { graphId: state.graphId, metric, onlyLinks, params: collectParams("pair-params"), withRecommendation: true }), signal });
                if (r2.cancelled) { /* stopped */ }
                else if (allPairs) {
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
        if (isAbort(e)) return;
        setStatus(e.message, "error");
    } finally {
        endJob("btn-pair");
    }
}

/* ----------------------------- communities -------------------------- */

async function detectCommunity() {
    if (jobBusy("btn-community")) return cancelJob("btn-community");
    if (!requireGraph()) return;
    const algorithm = $("community-algo").value;
    setStatus("Detecting communities (" + algorithm + ")…", "busy");
    const signal = beginJob("btn-community", "community");
    try {
        const res = await api("/api/communities",
            { ...jsonBody({ graphId: state.graphId, algorithm, params: collectParams("community-params") }), signal });
        if (res.cancelled) { setStatus("Stopped."); return; }
        state.communityData[res.algorithm] = res.values;
        rebuildAppearanceOptions();
        $("node-color-mode").value = "attribute";
        $("color-by").value = "community:" + res.algorithm;
        syncNodeColorControls();
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
        if (isAbort(e)) return;
        setStatus(e.message, "error");
    } finally {
        endJob("btn-community");
    }
}

async function runGlobalCommMetric() {
    if (jobBusy("btn-global-comm")) return cancelJob("btn-global-comm");
    if (!requireGraph()) return;
    const algorithm = $("global-comm-partition").value;
    if (!algorithm || !state.communityData[algorithm]) { setStatus("Detect a partition first.", "error"); return; }
    const metric = $("global-comm-metric").value;
    setStatus("Computing global " + metric + "…", "busy");
    const signal = beginJob("btn-global-comm", "commGlobal");
    try {
        const res = await api("/api/communities/global",
            { ...jsonBody({ graphId: state.graphId, algorithm, metric, params: collectParams("global-comm-params") }), signal });
        if (res.cancelled) { setStatus("Stopped."); return; }
        const label = res.label + " (" + algorithm + ")";
        state.graphMetrics[label] = res.value;
        if (state.rec.active && $("global-comm-rec").checked) {
            try {
                const r2 = await api("/api/communities/global", { ...jsonBody(
                    { graphId: state.graphId, algorithm, metric, params: collectParams("global-comm-params"), withRecommendation: true }), signal });
                if (!r2.cancelled) storeRecMetric("graph", label, r2.value);
            } catch (e) { /* recommendation metric is optional */ }
        }
        refreshAfterCompute();
        setStatus("Computed " + label + " = " + fmt(res.value));
    } catch (e) {
        if (isAbort(e)) return;
        setStatus(e.message, "error");
    } finally {
        endJob("btn-global-comm");
    }
}

async function runIndividualCommMetric() {
    if (jobBusy("btn-indiv-comm")) return cancelJob("btn-indiv-comm");
    if (!requireGraph()) return;
    const algorithm = $("indiv-comm-partition").value;
    if (!algorithm || !state.communityData[algorithm]) { setStatus("Detect a partition first.", "error"); return; }
    const metric = $("indiv-comm-metric").value;
    setStatus("Computing per-community " + metric + "…", "busy");
    const signal = beginJob("btn-indiv-comm", "commIndividual");
    try {
        const res = await api("/api/communities/individual",
            { ...jsonBody({ graphId: state.graphId, algorithm, metric, params: collectParams("indiv-comm-params") }), signal });
        if (res.cancelled) { setStatus("Stopped."); return; }
        const label = res.label + " · " + algorithm;
        if (!state.commMetricOrder.includes(label)) state.commMetricOrder.push(label);
        state.commMetricData[label] = { algorithm, values: numericMap(res.values) };
        if (state.rec.active && $("comm-rec").checked) {
            try {
                const r2 = await api("/api/communities/individual", { ...jsonBody(
                    { graphId: state.graphId, algorithm, metric, params: collectParams("indiv-comm-params"), withRecommendation: true }), signal });
                if (!r2.cancelled) storeRecMetric("comm", label, numericMap(r2.values));
            } catch (e) { /* recommendation metric is optional */ }
        }
        refreshAfterCompute();
        setStatus("Computed " + res.label + " over " + algorithm + " — avg " + fmt(res.average) + ".");
    } catch (e) {
        if (isAbort(e)) return;
        setStatus(e.message, "error");
    } finally {
        endJob("btn-indiv-comm");
    }
}

async function runAttributeGlobalCommMetric() {
    if (jobBusy("btn-attribute-global-comm")) return cancelJob("btn-attribute-global-comm");
    if (!requireGraph()) return;
    const attribute = $("attribute-comm-attribute").value;
    if (!attribute) { setStatus("Add a node attribute with values first.", "error"); return; }
    const metric = $("attribute-global-comm-metric").value;
    setStatus("Computing global " + metric + " by " + attribute + "…", "busy");
    const signal = beginJob("btn-attribute-global-comm", "attributeCommGlobal");
    try {
        const body = { graphId: state.graphId, attribute, metric, params: collectParams("attribute-global-comm-params") };
        const res = await api("/api/communities/global", { ...jsonBody(body), signal });
        if (res.cancelled) { setStatus("Stopped."); return; }
        const label = res.label + " (" + res.algorithm + ")";
        state.graphMetrics[label] = res.value;
        if (state.rec.active && $("attribute-global-comm-rec").checked) {
            try {
                const r2 = await api("/api/communities/global", { ...jsonBody({ ...body, withRecommendation: true }), signal });
                if (!r2.cancelled) storeRecMetric("graph", label, r2.value);
            } catch (e) { /* recommendation metric is optional */ }
        }
        refreshAfterCompute();
        setStatus("Computed " + label + " = " + fmt(res.value));
    } catch (e) {
        if (isAbort(e)) return;
        setStatus(e.message, "error");
    } finally {
        endJob("btn-attribute-global-comm");
    }
}

async function runAttributeIndividualCommMetric() {
    if (jobBusy("btn-attribute-indiv-comm")) return cancelJob("btn-attribute-indiv-comm");
    if (!requireGraph()) return;
    const attribute = $("attribute-comm-attribute").value;
    if (!attribute) { setStatus("Add a node attribute with values first.", "error"); return; }
    const metric = $("attribute-indiv-comm-metric").value;
    setStatus("Computing per-value " + metric + " by " + attribute + "…", "busy");
    const signal = beginJob("btn-attribute-indiv-comm", "attributeCommIndividual");
    try {
        const body = { graphId: state.graphId, attribute, metric, params: collectParams("attribute-indiv-comm-params") };
        const res = await api("/api/communities/individual", { ...jsonBody(body), signal });
        if (res.cancelled) { setStatus("Stopped."); return; }
        const label = res.label + " · " + res.algorithm;
        if (!state.attributeMetricOrder.includes(label)) state.attributeMetricOrder.push(label);
        state.attributeMetricData[label] = { attribute, values: numericMap(res.values), valueLabels: res.valueLabels || {} };
        if (state.rec.active && $("attribute-comm-rec").checked) {
            try {
                const r2 = await api("/api/communities/individual", { ...jsonBody({ ...body, withRecommendation: true }), signal });
                if (!r2.cancelled) storeRecMetric("comm", label, numericMap(r2.values));
            } catch (e) { /* recommendation metric is optional */ }
        }
        refreshAfterCompute();
        setStatus("Computed " + res.label + " over " + res.algorithm + " — avg " + fmt(res.average) + ".");
    } catch (e) {
        if (isAbort(e)) return;
        setStatus(e.message, "error");
    } finally {
        endJob("btn-attribute-indiv-comm");
    }
}
async function runEdgeAttributeMetric(metric, global) {
    const button = global ? "btn-edge-attribute-global" : "btn-edge-attribute-indiv";
    if (jobBusy(button)) return cancelJob(button);
    if (!requireGraph()) return;
    const attribute = $("edge-attribute-metric-attribute").value;
    if (!attribute) { setStatus("Add an edge attribute first.", "error"); return; }
    setStatus("Computing edge attribute metric by " + attribute + "…", "busy");
    const signal = beginJob(button, global ? "edgeAttributeGlobal" : "edgeAttributeIndividual");
    try {
        const res = await api("/api/metrics/edge-attributes", { ...jsonBody({ graphId: state.graphId, attribute, metric }), signal });
        if (res.cancelled) { setStatus("Stopped."); return; }
        if (global) {
            state.graphMetrics[res.label] = res.value;
            setStatus("Computed " + res.label + " = " + fmt(res.value));
        } else {
            const label = res.label;
            if (!state.edgeAttributeMetricOrder.includes(label)) state.edgeAttributeMetricOrder.push(label);
            state.edgeAttributeMetricData[label] = { attribute, values: numericMap(res.values), valueLabels: res.valueLabels || {} };
            setStatus("Computed " + res.label + " — avg " + fmt(res.average) + ".");
        }
        refreshAfterCompute();
    } catch (e) {
        if (!isAbort(e)) setStatus(e.message, "error");
    } finally {
        endJob(button);
    }
}
/* ------------------------------- paths ------------------------------ */

async function findPaths(options = {}) {
    if (!requireGraph()) return;
    const source = String(options.source ?? $("select-path-source").value).trim();
    const target = String(options.target ?? $("select-path-target").value).trim();
    if (!source || !target) { setStatus("Enter both source and target.", "error"); return; }
    $("select-path-source").value = source;
    $("select-path-target").value = target;
    applyReducers();
    const withRecommendation = false;
    setStatus("Finding shortest paths…", "busy");
    $("btn-select-paths").disabled = true;
    try {
        const res = await api("/api/paths", jsonBody({ graphId: state.graphId, source, target, withRecommendation }));
        state.lastPaths = res.paths || [];
        if (res.length < 0) {
            $("select-path-summary").textContent = "No path from " + source + " to " + target + ".";
        } else {
            const resultText = res.count + (res.truncated ? "+" : "") + " shortest path(s) of length " + res.length +
                " from " + source + " to " + target + (res.truncated ? " (showing first " + res.count + ")" : "") + ".";
            $("select-path-summary").textContent = resultText;
        }
        renderPathsTable(state.lastPaths, "select-paths-table");
        if (state.selection.type === "path") {
            if (state.lastPaths.length) highlightPaths(state.lastPaths);
            else { state.pathFocus = null; state.pathEndpointFocusActive = false; applyReducers(); }
        }
        updateSelectionSummary();
        setStatus("Found " + res.count + " path(s).");
    } catch (e) {
        setStatus(e.message, "error");
    } finally {
        $("btn-select-paths").disabled = false;
    }
}

function getPathEdgeStyle(path, index = 0) {
    const key = JSON.stringify(path);
    if (!state.pathEdgeStyles.has(key)) {
        const colors = ["#ff9f43", "#4f9dff", "#28c76f", "#ea5caa", "#b08cff", "#f5d04c"];
        state.pathEdgeStyles.set(key, { color: colors[index % colors.length], priority: 0 });
    }
    return state.pathEdgeStyles.get(key);
}

function buildPathEdgeColors(path) {
    const colors = new Map();
    if (!path || state.pathEdgeHighlight.mode !== "per-path") return colors;
    (path.paths || []).map((nodes, index) => ({ nodes, style: getPathEdgeStyle(nodes, index) }))
        .sort((a, b) => a.style.priority - b.style.priority)
        .forEach(({ nodes, style }) => {
            for (let i = 1; i < nodes.length; i++) {
                colors.set(pairKey(nodes[i - 1], nodes[i]), style.color);
                if (!state.directed) colors.set(pairKey(nodes[i], nodes[i - 1]), style.color);
            }
        });
    return colors;
}

function updatePathEdgeStyle(path, color) {
    const style = getPathEdgeStyle(path);
    style.color = color;
    style.priority = ++state.pathEdgeStyleSequence;
    applyReducers();
    drawRecOverlay();
}

function renderPathsTable(paths, tableId = "select-paths-table") {
    const table = $(tableId);
    table.innerHTML = "";
    const thead = document.createElement("thead");
    const perPathColor = state.pathEdgeHighlight.mode === "per-path";
    thead.innerHTML = "<tr><th>#</th><th>path</th><th>action</th>" + (perPathColor ? "<th>Color</th>" : "") + "</tr>";
    table.appendChild(thead);
    const tbody = document.createElement("tbody");
    paths.forEach((p, i) => {
        const tr = document.createElement("tr");
        const idx = document.createElement("td"); idx.textContent = i + 1;
        const path = document.createElement("td"); path.textContent = p.join(" → "); path.style.textAlign = "left";
        const act = document.createElement("td");
        const btn = document.createElement("button");
        const highlighted = (state.pathFocus?.paths || []).some((activePath) => JSON.stringify(activePath) === JSON.stringify(p));
        btn.textContent = highlighted ? "De-highlight" : "Highlight";
        btn.style.width = "auto"; btn.style.margin = "0"; btn.style.padding = "2px 8px";
        btn.addEventListener("click", () => togglePathHighlight(p));
        act.appendChild(btn);
        const colorCell = document.createElement("td");
        const style = getPathEdgeStyle(p, i);
        const picker = document.createElement("input");
        picker.type = "color";
        picker.value = style.color;
        picker.style.cssText = "width:32px;min-width:32px;height:24px;padding:0";
        picker.setAttribute("aria-label", "Edge color for path " + (i + 1));
        setTooltip(picker, "path-specific-edge-color");
        picker.addEventListener("input", () => updatePathEdgeStyle(p, picker.value));
        colorCell.appendChild(picker);
        tr.append(idx, path, act);
        if (perPathColor) tr.appendChild(colorCell);
        tbody.appendChild(tr);
    });
    table.appendChild(tbody);
}

function togglePathHighlight(path) {
    const key = JSON.stringify(path);
    const activePaths = state.pathFocus?.paths || [];
    const remaining = activePaths.filter((activePath) => JSON.stringify(activePath) !== key);
    if (remaining.length !== activePaths.length) {
        if (remaining.length) highlightPaths(remaining);
        else {
            state.pathFocus = null;
            state.pathEndpointFocusActive = false;
            applyReducers();
        }
    } else {
        highlightPaths([...activePaths, path]);
    }
    renderPathsTable(state.lastPaths);
}

// Highlights one or more paths on the network (dims everything else).
function highlightPaths(paths) {
    if (!paths.length) return;
    const previousPaths = new Set((state.pathFocus?.paths || []).map((path) => JSON.stringify(path)));
    paths.forEach((path, index) => {
        const style = getPathEdgeStyle(path, index);
        if (!previousPaths.has(JSON.stringify(path))) style.priority = ++state.pathEdgeStyleSequence;
    });
    if (state.selection.type !== "path") {
        state.selection.type = "path";
        state.selection.mode = "path-highlight";
        state.selectedNode = null;
        state.selection.node = null;
        state.selectedEdge = null;
        state.edgeSelectionPair = null;
        $("select-node-input").value = "";
        renderNodeInfo(null);
        clearEdgeSelection();
    }
    if (state.selection.mode === "none") {
        state.pathFocus = null;
        state.pathEndpointFocusActive = false;
        updateSelectionTargetUI();
        syncCosmographSelection();
        applyReducers();
        return;
    }
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
    const only = state.selection.mode === "path-only";
    state.selection.mode = only ? "path-only" : "path-highlight";
    state.pathFocus = { nodes, edges, only, paths: paths.map((path) => [...path]) };
    state.pathEndpointFocusActive = false;
    updateSelectionTargetUI();
    syncCosmographSelection();
    applyReducers();
    renderPathsTable(state.lastPaths);
}

function showPathsBetween(source, target) {
    $("select-path-source").value = source;
    $("select-path-target").value = target;
    state.selection.type = "path";
    state.selection.mode = "path-highlight";
    state.selectedNode = null;
    state.selection.node = null;
    state.selectedEdge = null;
    state.edgeSelectionPair = null;
    $("select-node-input").value = "";
    renderNodeInfo(null);
    clearEdgeSelection();
    updateSelectionTargetUI();
    state.pathFocus = null;
    state.pathEndpointFocusActive = true;
    syncCosmographSelection();
    return findPaths({ source, target });
}

function findSelectedPaths() {
    state.selection.type = "path";
    state.pathEndpointFocusActive = true;
    updateSelectionTargetUI();
    return findPaths({ source: $("select-path-source").value, target: $("select-path-target").value });
}

/* ------------------------------ editing ----------------------------- */

function setEditMode(on) {
    $("edit-help").hidden = !on;
    state.editFirstNode = null;
    $("btn-area-select-nodes").disabled = on || !state.dragNodes || !state.multiNodeSelect;
    if (on) setAreaNodeSelection(false);
}

async function editAddNodeAt(coords) {
    if (!requireGraph() || !state.graph) return;
    if (!confirmClears("Adding a node")) return;
    let id = state.graph.order;
    while (state.graph.hasNode(String(id))) id++;
    id = String(id);
    const attrs = { label: id, x: coords.x, y: coords.y, size: 4, color: "#4f9dff" };
    try {
        await applyAddNode(id, attrs);
        historyPush("add node " + id, () => applyRemoveNode(id), () => applyAddNode(id, attrs));
        setStatus("Added node " + id + ".");
    } catch (e) { setStatus(e.message, "error"); }
}

async function editAddEdge(source, target, weight) {
    if (!confirmClears("Adding an edge")) return;
    const w = weight == null ? 1.0 : weight;
    // Remember which endpoints the edge creates, so undo can also drop nodes it brought into existence.
    const created = [source, target].filter((nd) => !state.graph.hasNode(nd));
    try {
        await applyAddEdge(source, target, w);
        historyPush("add edge " + source + " → " + target,
            async () => {
                let stats = await applyRemoveEdge(source, target, null, true);
                for (const nd of created) { if (state.graph.hasNode(nd) && state.graph.degree(nd) === 0) stats = await applyRemoveNode(nd, true); }
                onGraphEdited(stats);
            },
            () => applyAddEdge(source, target, w));
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
    node = String(node);
    const snap = snapshotNode(node);   // capture the node + its incident edges so undo can bring them all back
    try {
        await applyRemoveNode(node);
        if (snap) historyPush("remove node " + node, () => restoreNode(node, snap), () => applyRemoveNode(node));
        setStatus("Removed node " + node + ".");
    } catch (e) { setStatus(e.message, "error"); }
}

// Removes a single edge (a specific parallel edge for multigraphs, identified by its graphology key).
async function editRemoveEdge(source, target, edgeId) {
    // Snapshot the edge (weight + custom attributes) before it goes, so undo re-creates it faithfully.
    let weight = 1, attrs = null;
    if (edgeId != null && state.graph.hasEdge(edgeId)) { const a = state.graph.getEdgeAttributes(edgeId); weight = a && a.weight != null ? a.weight : 1; attrs = a && a.attrs ? JSON.parse(JSON.stringify(a.attrs)) : null; }
    else if (state.graph.hasEdge(source, target)) { const a = state.graph.getEdgeAttributes(source, target); weight = a && a.weight != null ? a.weight : 1; attrs = a && a.attrs ? JSON.parse(JSON.stringify(a.attrs)) : null; }
    try {
        await applyRemoveEdge(source, target, edgeId);
        // Redo removes by endpoints, not the original graphology key: an intervening undo re-creates the edge with a
        // fresh key, so the captured edgeId would be stale (harmless for simple graphs; removes one parallel edge for
        // a multigraph).
        const redo = () => applyRemoveEdge(source, target, null);
        const undo = async () => {
            const stats = await applyAddEdge(source, target, weight, true);
            if (attrs) {
                for (const name of Object.keys(attrs)) { try { await api("/api/graph/" + state.graphId + "/attributes/edge", jsonBody({ source, target, name, value: attrSendValue(attrs[name]) })); } catch (e) { /* best-effort */ } }
                if (state.graph.hasEdge(source, target)) { const key = state.graph.edge(source, target); if (key) state.graph.setEdgeAttribute(key, "attrs", JSON.parse(JSON.stringify(attrs))); }
            }
            onGraphEdited(stats);
        };
        historyPush("remove edge " + source + " → " + target, undo, redo);
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

// What resetResults() discards, as a human-readable list: metric values and community partitions. Committing
// recommended links clears exactly this much — it leaves the recommendation itself and any diffusion results alone.
function computedResultsList() {
    const bits = [];
    if (state.metricOrder.length || state.pairOrder.length || state.commMetricOrder.length || Object.keys(state.graphMetrics).length) bits.push("computed metrics");
    if (Object.keys(state.communityData).length) bits.push("detected communities");
    return bits;
}

// The computed state a structural graph edit (or reload) would discard, as a human-readable list. Broader than
// computedResultsList(): a graph edit also drops the recommendation overlay and the diffusion results.
function pendingClearsList() {
    const bits = computedResultsList();
    // Count remaining recommended *links*, not models: once every link has been committed to the graph ("Add all"),
    // the model entry survives with an empty edge list (undo restores links into it), but there is nothing left to
    // lose — warning about it then is a false alarm.
    if (recLinksPending()) bits.push("recommendations");
    if (diffusionHasResults()) bits.push("diffusion results");
    return bits;
}

// Whether any recommended link is still uncommitted (in the table or in any stored model's overlay edges).
function recLinksPending() {
    if (state.rec.tableEdges && state.rec.tableEdges.length) return true;
    return Object.values(state.rec.models).some((m) => m && m.edges && m.edges.length);
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
    state.cosmographDirty = true;
    if (state.selectedNode != null && !state.graph.hasNode(state.selectedNode)) clearSelection();
    refreshAfterCompute();
    applyReducers();
    if (state.renderer) state.renderer.refresh();
    if (usingCosmograph() && window.relisonCosmograph?.isRendered()) {
        window.relisonCosmograph.render(state.graph, cosmographInteractionCallbacks())
            .then(() => { state.cosmographDirty = false; })
            .catch((error) => setStatus("Cosmograph could not refresh after graph editing: " + error.message, "error"));
    }
}

/* --------------------------- edit history (undo/redo) --------------------------- */
// A single LIFO history for the reversible structural edits. The user-facing edit handlers apply their change
// through the low-level apply*/restore* helpers below and then push an {undo, redo} pair; undo/redo replay those
// helpers directly (guarded by `applying`, so the replay itself is never recorded). Bulk/irreversible changes
// (loading a network, generating or uploading the whole piece set) clear the history via historyReset().
//
// Note: like any graph edit, undo/redo clears computed results (metrics, communities, recommendations, diffusion) —
// it restores the network/pieces, not results computed from a previous topology.

function historyReset() {
    state.history.undo = [];
    state.history.redo = [];
    updateHistoryUI();
}

function historyPush(label, undo, redo) {
    if (state.history.applying) return;
    state.history.undo.push({ label, undo, redo });
    state.history.redo = [];   // a fresh action invalidates the redo branch
    updateHistoryUI();
}

async function historyUndo() {
    const h = state.history;
    if (h.applying || !h.undo.length) return;
    const entry = h.undo.pop();
    h.applying = true;
    try { await entry.undo(); h.redo.push(entry); setStatus("Undid: " + entry.label + "."); }
    catch (e) { h.undo.push(entry); setStatus("Could not undo (" + entry.label + "): " + e.message, "error"); }
    finally { h.applying = false; updateHistoryUI(); }
}

async function historyRedo() {
    const h = state.history;
    if (h.applying || !h.redo.length) return;
    const entry = h.redo.pop();
    h.applying = true;
    try { await entry.redo(); h.undo.push(entry); setStatus("Redid: " + entry.label + "."); }
    catch (e) { h.redo.push(entry); setStatus("Could not redo (" + entry.label + "): " + e.message, "error"); }
    finally { h.applying = false; updateHistoryUI(); }
}

function updateHistoryUI() {
    const h = state.history;
    ["btn-undo", "btn-network-undo"].forEach((id) => {
        const button = $(id);
        if (button) {
            button.disabled = h.applying || !h.undo.length;
            setTooltip(button, h.undo.length ? "js-undo" : "js-nothing-to-undo", h.undo.length ? { label: h.undo[h.undo.length - 1].label } : {});
        }
    });
    ["btn-redo", "btn-network-redo"].forEach((id) => {
        const button = $(id);
        if (button) {
            button.disabled = h.applying || !h.redo.length;
            setTooltip(button, h.redo.length ? "js-redo" : "js-nothing-to-redo", h.redo.length ? { label: h.redo[h.redo.length - 1].label } : {});
        }
    });
}

function defaultNodeAttrs(id) {
    return { label: String(id), x: (Math.random() - 0.5) * 50, y: (Math.random() - 0.5) * 50, size: 4, color: "#4f9dff" };
}

/* --- low-level apply helpers: mutate server + client + refresh, WITHOUT touching the history stack --- */
// When `quiet` is set the shared post-edit refresh (onGraphEdited) is skipped, so a compound operation (e.g. restoring
// a node together with all its incident edges) can refresh once at the end instead of after every sub-step.

async function applyAddNode(id, attrs, quiet) {
    id = String(id);
    const res = await api("/api/graph/" + state.graphId + "/node", jsonBody({ node: id }));
    if (!state.graph.hasNode(id)) state.graph.addNode(id, attrs || defaultNodeAttrs(id));
    else if (attrs) state.graph.mergeNodeAttributes(id, attrs);
    if (!quiet) onGraphEdited(res.stats);
    return res.stats;
}

async function applyRemoveNode(id, quiet) {
    id = String(id);
    const res = await api("/api/graph/" + state.graphId + "/node/" + encodeURIComponent(id), { method: "DELETE" });
    if (state.selectedNode === id) clearSelection();
    if (state.graph.hasNode(id)) state.graph.dropNode(id);
    if (!quiet) onGraphEdited(res.stats);
    return res.stats;
}

async function applyAddEdge(source, target, weight, quiet) {
    const w = weight == null ? 1.0 : weight;
    const res = await api("/api/graph/" + state.graphId + "/edge", jsonBody({ source, target, weight: w }));
    ensureNode(source); ensureNode(target);
    if (!state.graph.hasEdge(source, target)) state.graph.addEdge(source, target, { weight: w });
    if (!quiet) onGraphEdited(res.stats);
    return res.stats;
}

async function applyRemoveEdge(source, target, edgeId, quiet) {
    const body = { source, target };
    if (edgeId != null) body.edgeId = edgeId;
    const res = await api("/api/graph/" + state.graphId + "/edge",
        { method: "DELETE", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
    if (edgeId != null && state.graph.hasEdge(edgeId)) state.graph.dropEdge(edgeId);
    else if (state.graph.hasEdge(source, target)) state.graph.dropEdge(source, target);
    if (!quiet) onGraphEdited(res.stats);
    return res.stats;
}

/* --- node removal snapshot / restore (a removed node takes its incident edges and attributes with it) --- */

function snapshotNode(node) {
    const g = state.graph;
    if (!g.hasNode(node)) return null;
    const attrs = JSON.parse(JSON.stringify(g.getNodeAttributes(node)));   // label/x/y/size/color + custom "attrs"
    const edges = [];
    g.forEachEdge(node, (edge, ea, s, t) => {
        edges.push({ source: s, target: t, weight: ea && ea.weight != null ? ea.weight : 1, attrs: ea && ea.attrs ? JSON.parse(JSON.stringify(ea.attrs)) : null });
    });
    return { attrs, edges };
}

// Restores a removed node: the node (with its visual + custom attributes) and each incident edge (weight + custom
// attributes). Server-side custom attributes are re-applied through the attribute endpoints (best-effort).
async function restoreNode(node, snap) {
    let stats = await applyAddNode(node, snap.attrs, true);
    const nodeAttrs = (snap.attrs && snap.attrs.attrs) || {};
    for (const name of Object.keys(nodeAttrs)) {
        try { await api("/api/graph/" + state.graphId + "/attributes/node", jsonBody({ node, name, value: attrSendValue(nodeAttrs[name]) })); } catch (e) { /* best-effort */ }
    }
    for (const e of snap.edges) {
        stats = await applyAddEdge(e.source, e.target, e.weight, true);
        if (e.attrs) {
            for (const name of Object.keys(e.attrs)) {
                try { await api("/api/graph/" + state.graphId + "/attributes/edge", jsonBody({ source: e.source, target: e.target, name, value: attrSendValue(e.attrs[name]) })); } catch (ex) { /* best-effort */ }
            }
            if (state.graph.hasEdge(e.source, e.target)) {
                const key = state.graph.edge(e.source, e.target);
                if (key) state.graph.setEdgeAttribute(key, "attrs", JSON.parse(JSON.stringify(e.attrs)));
            }
        }
    }
    onGraphEdited(stats);   // single refresh after the whole node + its edges are back
}

// The attribute endpoints parse strings; send booleans/numbers as their string form so they round-trip.
function attrSendValue(v) { return v == null ? null : String(v); }

/* --- information-piece apply helpers (client state + persistence, no history) --- */

function applyPieceInsert(piece, index) {
    const d = state.diffusion;
    const i = Math.max(0, Math.min(index, d.pieces.length));
    d.pieces.splice(i, 0, JSON.parse(JSON.stringify(piece)));
    renderPiecesTable();
    schedulePersistPieces();
}

function applyPieceRemove(index) {
    const d = state.diffusion;
    if (index < 0 || index >= d.pieces.length) return;
    d.pieces.splice(index, 1);
    renderPiecesTable();
    schedulePersistPieces();
}

/* ------------------------------ tabs -------------------------------- */

function switchTab(tab) {
    state.activeTab = tab;
    document.querySelectorAll(".tab").forEach((b) => b.classList.toggle("active", b.dataset.tab === tab));
    $("pane-import").classList.toggle("active", tab === "import");
    $("pane-network").classList.toggle("active", tab === "network");
    $("pane-tables").classList.toggle("active", tab === "tables");
    $("pane-metrics").classList.toggle("active", tab === "metrics");
    $("pane-recommend").classList.toggle("active", tab === "recommend");
    $("pane-diffusion").classList.toggle("active", tab === "diffusion");

    updatePanels(tab);

    if (tab === "network") setTimeout(() => {
        if (state.renderer) { state.renderer.refresh(); drawRecOverlay(); }
        if ($("network-renderer").value === "cosmograph" && state.graph && window.relisonCosmograph &&
            (state.cosmographDirty || !window.relisonCosmograph.isRendered())) {
            window.relisonCosmograph.render(state.graph, {
                ...cosmographInteractionCallbacks(),
            }).then(() => { state.cosmographDirty = false; })
              .catch((e) => setStatus("Cosmograph could not restore after switching tabs: " + e.message, "error"));
        }
    }, 0);
    if (tab === "tables") renderTable(state.activeTableSubtab);
    if (tab === "metrics") renderMetricsDashboard();
    if (tab === "recommend") { loadRecCatalog(); renderTable("rec"); }
    if (tab === "diffusion") enterDiffusionTab();
}

// Switches between the subtabs in the Diffusion tab's center panel.
function switchDiffSubtab(sub) {
    state.diffusion.subview = sub;
    document.querySelectorAll(".diffsubtab").forEach((b) => b.classList.toggle("active", b.dataset.diffsubtab === sub));
    $("diff-view-graph").classList.toggle("active", sub === "graph");
    $("diff-view-metrics").classList.toggle("active", sub === "metrics");
    $("diff-view-pieces").classList.toggle("active", sub === "pieces");
    $("diff-view-stats").classList.toggle("active", sub === "stats");
    // The "Node at iteration" panel only makes sense next to the canvas; elsewhere it is hidden and its column collapses.
    const onGraph = sub === "graph";
    toggleHidden("diff-state-aside", !onGraph);
    document.querySelector(".diff-layout").classList.toggle("no-state", !onGraph);
    if (sub === "graph") {
        if (state.diffusion.renderer) setTimeout(() => { state.diffusion.renderer.refresh(); drawDiffOverlay(); }, 0);
        if (state.diffusion.rendererType === "cosmograph") restoreDiffCosmograph();
    } else if (sub === "metrics") {
        renderDiffMetrics();
    } else if (sub === "pieces") {
        renderActivePiecesMode();
    } else if (sub === "stats") {
        renderActiveStatsView();
    }
}

// Switches the inner Node / Piece / Feature / Distributions view of the "Statistics" subtab.
function switchStatsView(view) {
    state.diffusion.statsView = view;
    document.querySelectorAll(".diffstatsview").forEach((b) => b.classList.toggle("active", b.dataset.statsview === view));
    ["node", "piece", "feat", "dist"].forEach((v) => toggleHidden("diff-stats-view-" + v, v !== view));
    renderActiveStatsView();
}

// Renders whichever view of the Statistics subtab is currently active.
function renderActiveStatsView() {
    const view = state.diffusion.statsView;
    if (view === "piece") renderPieceTimeline();
    else if (view === "feat") renderFeatureTimeline();
    else if (view === "dist") renderDiffDistribution();
    else renderNodeTimeline();
}

// True when the Statistics subtab is showing the given inner view (node / piece / feat / dist).
function statsActive(view) {
    return state.diffusion.subview === "stats" && state.diffusion.statsView === view;
}

// Shows/hides the side panels per tab and resizes the layout grid accordingly:
// import → no panels; network → left (layout/visualization) + right (selection); metrics → right (runners);
// tables/paths → no panels (full-width content).
// The left panel carries the controls of whichever tab needs them — layout/appearance on Network, the metric and
// community runners on Metrics — so configuration always sits on the left. The right panel is now only the
// canvas-side Selection block, which is meaningful on the Network tab alone.
function updatePanels(tab) {
    const showLeft = tab === "network" || tab === "metrics";
    const showRight = tab === "network";
    $("left").style.display = showLeft ? "" : "none";
    $("right").style.display = showRight ? "" : "none";
    // The metric blocks were designed for the old 300px right column, so widen the left one on that tab.
    const leftWidth = tab === "metrics" ? "300px" : "270px";
    const cols = [showLeft ? leftWidth : null, "1fr", showRight ? "300px" : null].filter(Boolean).join(" ");
    $("layout").style.gridTemplateColumns = cols;

    toggleHidden("left-layout", tab !== "network");
    toggleHidden("left-metrics", tab !== "metrics");
}

// Disables the Recommendation tab for multigraphs (recommenders need a simple FastGraph); switches away if needed.
function updateRecTabAvailability() {
    const btn = $("tab-recommend");
    if (!btn) return;
    btn.disabled = state.multigraph;
    btn.classList.toggle("disabled", state.multigraph);
    if (state.multigraph) setTooltip(btn, "js-recommendation-multigraph");
    else { btn.removeAttribute("title"); btn.removeAttribute("aria-description"); }
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
    $("subpane-attrcomm").classList.toggle("active", sub === "attrcomm");
    $("subpane-edgeattr").classList.toggle("active", sub === "edgeattr");
    if (sub === "nodes") { drawNodeChart(); drawNodeScatter(); }
    if (sub === "edges") { drawEdgeChart(); drawEdgeScatter(); }
    if (sub === "pairs") drawPairChart();
    if (sub === "comm") { drawCommChart(); drawCommScatter(); }
    if (sub === "attrcomm") drawAttributeCommChart();
    if (sub === "edgeattr") drawEdgeAttributeChart();
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

/* ----------------------------- resizable columns ---------------------------- */
// Shared by every `table.data` (nodes, edges, recommendation, information pieces, real propagated). After a header is
// (re)built the table is marked "dirty"; the next body render freezes the current (natural) column widths into a
// <colgroup>, switches the table to fixed layout, and adds a drag grip on each column divider. Widths then persist
// across body-only re-renders (filtering / sorting / paging) so a user's manual resize is not lost.

// Called right after a header rebuild: reset to natural (auto) sizing so the next render can re-measure.
function markColumnsDirty(table) {
    if (!table) return;
    table.dataset.colsDirty = "1";
    const cg = table.querySelector("colgroup");
    if (cg) cg.remove();
    table.style.tableLayout = "";
    table.style.width = "";
}

// Called at the end of a body render: if dirty (and the table is laid out), freeze widths and attach resize grips.
function setupResizableColumns(table) {
    if (!table || table.dataset.colsDirty !== "1") return;
    const headRow = table.querySelector("thead tr");
    if (!headRow) return;
    const ths = Array.from(headRow.children);
    if (!ths.length) return;

    const widths = ths.map((th) => Math.round(th.getBoundingClientRect().width));
    if (widths.some((w) => !w)) return;   // not laid out yet (e.g. hidden tab); retry on the next render

    const colgroup = document.createElement("colgroup");
    widths.forEach((w) => { const col = document.createElement("col"); col.style.width = w + "px"; colgroup.appendChild(col); });
    table.insertBefore(colgroup, table.firstChild);
    table.style.tableLayout = "fixed";
    table.style.width = widths.reduce((a, b) => a + b, 0) + "px";

    ths.forEach((th, i) => {
        const grip = document.createElement("span");
        grip.className = "col-grip";
        if (i === ths.length - 1) grip.style.right = "0";   // last column: keep the grip inside the table's right edge
        setTooltip(grip, "js-resize-column");
        grip.addEventListener("mousedown", (e) => startColResize(e, table, colgroup, i));
        grip.addEventListener("click", (e) => e.stopPropagation());   // a click on the grip must not sort the column
        th.appendChild(grip);
    });

    table.dataset.colsDirty = "";
}

function startColResize(e, table, colgroup, i) {
    e.preventDefault();
    e.stopPropagation();
    const col = colgroup.children[i];
    const startX = e.clientX;
    const startW = col.getBoundingClientRect().width;
    const onMove = (ev) => {
        col.style.width = Math.max(36, startW + (ev.clientX - startX)) + "px";
        let sum = 0;
        for (const c of colgroup.children) sum += parseFloat(c.style.width) || 0;
        table.style.width = sum + "px";
    };
    const onUp = () => {
        document.removeEventListener("mousemove", onMove);
        document.removeEventListener("mouseup", onUp);
        document.body.classList.remove("col-resizing");
    };
    document.body.classList.add("col-resizing");
    document.addEventListener("mousemove", onMove);
    document.addEventListener("mouseup", onUp);
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
        if (c.key === "_act") { const th = document.createElement("th"); th.className = "act-th"; sortRow.appendChild(th); return; }
        const onSort = () => applySort(state.tables[key].sort, c.key, () => { state.tables[key].page = 0; }, () => renderTable(key));
        sortRow.appendChild(makeSortTh(c.key, c.label, onSort));
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
            setTooltip(rm, "js-remove-attribute-column");
            const name = c.key.slice(c.key.indexOf(":") + 1);
            rm.addEventListener("click", () => removeAttrColumn(key === "nodes" ? "node" : "edge", name));
            wrap.appendChild(rm);
        }
        th.appendChild(wrap);
        filterRow.appendChild(th);
    });
    thead.appendChild(filterRow);

    table.insertBefore(thead, table.firstChild);
    markColumnsDirty(table);
    updateSortIndicators(key, cols);
}

function updateSortIndicators(key) {
    updateSortIndicatorsFor(TABLE_IDS[key], state.tables[key].sort);
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
                    setTooltip(add, "js-add-link");
                    add.addEventListener("click", () => addRecLinkFromTable(id));
                    td.appendChild(add);
                } else {
                    const rm = document.createElement("button");
                    rm.className = "row-remove";
                    rm.textContent = "✕";
                    setTooltip(rm, key === "nodes" ? "js-remove-node" : "js-remove-edge");
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
    setupResizableColumns(table);
    updatePager(key, total, pages, start, pageIds.length);
}

function setColFilter(key, col, op, value, value2) {
    const networkFilter = key === "nodes" || key === "edges";
    if (networkFilter) clearSelection({ refreshTable: false, refreshGraph: false });
    const filters = state.tables[key].filters;
    const f = { op, value, value2 };
    if (isFilterActive(f)) filters[col] = f;
    else delete filters[col];
    state.tables[key].page = 0;
    renderBody(key);     // body only → keeps the filter input focused
    applyReducers();   // emphasize the rows remaining after filtering
    if (networkFilter) syncCosmographSelection();
}

function clearFilters(key) {
    const networkFilter = key === "nodes" || key === "edges";
    if (networkFilter) clearSelection({ refreshTable: false, refreshGraph: false });
    state.tables[key].filters = {};
    state.tables[key].headerSig = null; // force header rebuild so inputs reset
    state.tables[key].page = 0;
    renderTable(key);
    applyReducers();
    if (networkFilter) syncCosmographSelection();
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
        populateRecEvalFeature();   // a new node attribute can serve as evaluation feature data
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
        populateRecEvalFeature();
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

// The whole filtered table, independent of sorting and pagination, defines
// the graph emphasis. Edge filters also emphasize the remaining endpoints.
function tableFilterFocus(g) {
    // A manual selection takes over graph emphasis while table filters remain
    // available in the table. Editing a filter explicitly clears the selection.
    if (state.selectedNode != null || state.selectedEdge != null || state.edgeSelectionPair || state.pathFocus || state.pathEndpointFocusActive) return null;
    const nodeFilters = activeFilters("nodes", nodeColumns());
    const edgeFilters = activeFilters("edges", edgeColumns());
    if (!nodeFilters.length && !edgeFilters.length) return null;
    const matchingNodes = new Set();
    g.forEachNode((node) => { if (rowPasses(nodeRowValue, node, nodeFilters)) matchingNodes.add(node); });
    const nodes = edgeFilters.length ? new Set() : matchingNodes;
    const edges = new Set();
    g.forEachEdge((edge) => {
        const source = g.source(edge), target = g.target(edge);
        if (!matchingNodes.has(source) || !matchingNodes.has(target) || !rowPasses(edgeRowValue, edge, edgeFilters)) return;
        edges.add(edge);
        if (edgeFilters.length) { nodes.add(source); nodes.add(target); }
    });
    return { nodes, edges, edgeFiltered: edgeFilters.length > 0 };
}

// Combines two view effects into sigma's reducers:
//  - table filters: matching rows are emphasized; other nodes/edges are dimmed;
//  - node selection: depending on the mode, the focus set is highlighted (others dimmed) or isolated (others hidden).
// Neither affects metric computation (always done over the whole graph on the server).
function applyReducers() {
    if (!state.renderer || !state.graph) return;
    const g = state.graph;

    const tableFocus = tableFilterFocus(g);

    // Timeline: at timestamp t, only nodes/edges whose time attribute covers t are shown (empty value → never shown).
    const tl = state.timeline;
    const tlNode = !!(tl.nodeAttr && tl.t != null && tl.min != null);
    const tlEdge = !!(tl.edgeAttr && tl.t != null && tl.min != null);
    let presentNodes = null;
    if (tlNode) {
        presentNodes = new Set();
        g.forEachNode((n) => { if (timelineMatches(nodeAttrVal(n, tl.nodeAttr))) presentNodes.add(n); });
    }

    let presentEdges = null;
    if (tlEdge) {
        presentEdges = new Set();
        g.forEachEdge((e) => { if (timelineMatches(edgeAttrVal(e, tl.edgeAttr))) presentEdges.add(e); });
    }

    const path = state.pathFocus;           // highlighting shortest paths takes priority over selection
    const pathEdgeColor = state.pathEdgeHighlight.mode === "single" ? state.pathEdgeHighlight.color : null;
    const pathEdgeColors = buildPathEdgeColors(path);
    const nodeSizeScale = state.pathAppearance.enlargeNodes ? state.pathAppearance.nodeScale : 1;
    const edgeWidthScale = state.pathAppearance.edgeScale;
    const focus = path ? null : selectionFocus();
    const dim = focus && !focus.only;       // highlight modes: dim non-focus
    const isolate = focus && focus.only;    // "show only" modes: hide non-focus
    const dimColor = cssVar("--border", "#3a3c41");

    const nodeActive = tableFocus || focus || path || tlNode || state.multiSelectedNodes.size > 0;
    state.renderer.setSetting("nodeReducer", (node, sourceData) => {
        // Convert shared diameter values to Sigma's radius convention before
        // applying focus/path modifiers, so every Sigma view stays consistent.
        const data = { ...sourceData, size: (Number.isFinite(sourceData.size) ? sourceData.size : 0) / 2 };
        if (!nodeActive) return data;
        if (presentNodes && !presentNodes.has(node)) return { ...data, hidden: true };
        if (path?.only && !path.nodes.has(node)) return { ...data, hidden: true };
        if (isolate && !focus.nodes.has(node)) return { ...data, hidden: true };
        if (state.multiSelectedNodes.has(node)) return { ...data, highlighted: true, zIndex: 2 };
        if (tableFocus && !tableFocus.nodes.has(node)) return { ...data, color: dimColor, highlighted: false, zIndex: 0 };
        if (path) {
            if (path.nodes.has(node)) return { ...data, size: data.size * nodeSizeScale, zIndex: 2 };
            return path.only
                ? { ...data, hidden: true }
                : { ...data, color: dimColor, zIndex: 0 };
        }
        if (isolate && !focus.nodes.has(node)) return { ...data, hidden: true };
        if (focus && node === focus.selected) return { ...data, highlighted: true, zIndex: 2 };
        if (dim && !focus.nodes.has(node)) return { ...data, color: dimColor, zIndex: 0 };
        if ((focus && focus.nodes.has(node)) || tableFocus) return { ...data, zIndex: 1 };
        return data;
    });

    const edgeActive = tableFocus || focus || path || tlNode || tlEdge;
    state.renderer.setSetting("edgeReducer", edgeActive ? (edge, data) => {
        const s = g.source(edge), t = g.target(edge);
        if (presentNodes && (!presentNodes.has(s) || !presentNodes.has(t))) return { ...data, hidden: true };
        if (presentEdges && !presentEdges.has(edge)) return { ...data, hidden: true };
        if (path?.only && !path.edges.has(pairKey(s, t))) return { ...data, hidden: true };
        if (isolate && (!focus.nodes.has(s) || !focus.nodes.has(t) || (focus.matchingEdges && !focus.matchingEdges.has(edge)))) return { ...data, hidden: true };
        if (tableFocus && !tableFocus.edges.has(edge)) return { ...data, color: dimColor, zIndex: 0 };
        if (path) {
            if (path.edges.has(pairKey(s, t))) return { ...data, size: data.size * edgeWidthScale, color: pathEdgeColors.get(pairKey(s, t)) || pathEdgeColor || data.color, zIndex: 2 };
            return path.only ? { ...data, hidden: true } : { ...data, color: dimColor, zIndex: 0 };
        }
        if (isolate && (!focus.nodes.has(s) || !focus.nodes.has(t))) return { ...data, hidden: true };
        if (focus?.matchingEdges && !focus.matchingEdges.has(edge)) return focus.only ? { ...data, hidden: true } : { ...data, color: dimColor };
        if (focus?.edgeOnly && edge !== focus.edge) return { ...data, color: dimColor };
        if (dim && (!focus.nodes.has(s) || !focus.nodes.has(t))) return { ...data, color: dimColor };
        return data;
    } : null);

    state.renderer.refresh();
    const cosmographTimelineActive = tlNode || tlEdge;
    if (usingGeographic()) window.relisonGeographic.refresh();
    const cosmographFocusActive = Boolean(focus);
    window.relisonCosmographTableFocus = tableFocus ? {
        nodes: new Set([...tableFocus.nodes].map(String)),
        edges: new Set([...tableFocus.edges].map(String)),
        edgeFiltered: tableFocus.edgeFiltered,
        dimColor,
    } : null;
    const tableSignature = JSON.stringify(tableFocus ? {
        nodes: [...tableFocus.nodes].map(String).sort(),
        edges: [...tableFocus.edges].map(String).sort(),
        edgeFiltered: tableFocus.edgeFiltered,
    } : null);
    const cosmographTableChanged = tableSignature !== state.cosmographTableSignature;
    state.cosmographTableSignature = tableSignature;
    window.relisonCosmographTemporal = {
        nodes: presentNodes ? new Set([...presentNodes].map(String)) : null,
        edges: presentEdges ? new Set([...presentEdges].map(String)) : null,
    };
    // Cosmograph has its own data projection. Feed it the same selection set
    // used above by Sigma's reducers so highlight and show-only modes agree.
    window.relisonCosmographFocus = focus ? {
        nodes: new Set([...focus.nodes].map(String)),
        only: Boolean(focus.only),
        selected: focus.selected == null ? null : String(focus.selected),
        edgeOnly: Boolean(focus.edgeOnly),
        edge: focus.edge == null ? null : String(focus.edge),
        matchingEdges: focus.matchingEdges ? new Set([...focus.matchingEdges].map(String)) : null,
        dimColor,
    } : null;
    const pathSelectionNodes = new Set((state.pathEndpointFocusActive ? [$("select-path-source").value.trim(), $("select-path-target").value.trim()] : [])
        .filter((node) => node && g.hasNode(node)));
    window.relisonCosmographPath = path ? {
        nodes: new Set([...path.nodes].map(String)),
        edges: new Set([...path.edges].map(String)),
        only: Boolean(path.only),
        edgeColor: pathEdgeColor,
        edgeColors: pathEdgeColors,
        nodeSizeScale,
        edgeWidthScale,
        dimColor,
    } : null;
    window.relisonCosmographPathSelection = !path && pathSelectionNodes.size ? {
        nodes: pathSelectionNodes,
        dimColor,
    } : null;
    const pathSignature = JSON.stringify({
        path: path ? { nodes: [...path.nodes].map(String).sort(), edges: [...path.edges].map(String).sort(), only: Boolean(path.only), edgeColor: pathEdgeColor, edgeColors: [...pathEdgeColors].sort(), nodeSizeScale, edgeWidthScale } : null,
        endpoints: [...pathSelectionNodes].sort(),
    });
    const cosmographPathChanged = pathSignature !== state.cosmographPathSignature;
    state.cosmographPathSignature = pathSignature;
    state.cosmographPathActive = Boolean(path || pathSelectionNodes.size);
    // A focus change changes colours and, in show-only mode, the active data.
    // Use the regular projection refresh rather than a timeline-only delta so
    // retained points receive their new dimmed/visible mapping too.
    if (cosmographFocusActive || state.cosmographFocusActive || cosmographPathChanged || cosmographTableChanged) {
        queueCosmographAppearanceRefresh();
    } else if (cosmographTimelineActive || state.cosmographTimelineActive) {
        queueCosmographTimelineRefresh();
    }
    state.cosmographTimelineActive = cosmographTimelineActive;
    state.cosmographFocusActive = cosmographFocusActive;
}

/* ------------------------- metrics dashboard ------------------------ */

function renderMetricsDashboard() {
    renderGlobalTable();
    renderAverages("node-averages-table", state.metricOrder, state.metricData, "vertex");
    renderAverages("edge-averages-table", state.pairOrder, state.pairData, "pair");
    renderPairAverages();
    renderCommAverages();
    renderAttributeCommAverages();
    renderEdgeAttributeAverages();
    syncChartSelectors();
    renderNodeTopK();
    updateScatterBlocks();
    if (state.activeSubtab === "nodes") { drawNodeChart(); drawNodeScatter(); }
    if (state.activeSubtab === "edges") { drawEdgeChart(); drawEdgeScatter(); }
    if (state.activeSubtab === "pairs") drawPairChart();
    if (state.activeSubtab === "comm") { drawCommChart(); drawCommScatter(); }
    if (state.activeSubtab === "attrcomm") drawAttributeCommChart();
    if (state.activeSubtab === "edgeattr") drawEdgeAttributeChart();
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

// Attribute-based metrics reuse the community metric engines, but their categories are attribute values.
function renderAttributeCommAverages() {
    const recKeys = recColumnKeys("comm");
    const rows = state.attributeMetricOrder.map((label) => ({
        label,
        original: average(state.attributeMetricData[label].values),
        rec: recKeys.map((k) => { const m = state.recMetricData.comm?.[label]?.[k]; return m ? average(m) : undefined; }),
    }));
    buildAverageTable("attrcomm-averages-table", rows, recKeys);
}
function renderEdgeAttributeAverages() {
    const rows = state.edgeAttributeMetricOrder.map((label) => ({ label, original: average(state.edgeAttributeMetricData[label].values), rec: [] }));
    buildAverageTable("edgeattr-averages-table", rows, []);
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

function renderNodeTopK() {
    const metric = $("node-topk-metric").value;
    const values = state.metricData[metric];
    const table = $("node-topk-table");
    const note = $("node-topk-note");
    table.replaceChildren();
    $("node-topk-metric").disabled = !state.metricOrder.length;
    if (!state.graph || !values) {
        note.textContent = "Run a node metric to see the highest-scoring nodes.";
        return;
    }
    const k = Number($("node-topk-k").value);
    if (!Number.isSafeInteger(k) || k < 1) {
        note.textContent = "Enter a positive whole number for k.";
        return;
    }
    const nodes = state.graph.nodes().filter((node) => Number.isFinite(values[node]));
    nodes.sort((a, b) => values[b] - values[a]
        || String(a).localeCompare(String(b), undefined, { numeric: true }));
    const top = nodes.slice(0, k);
    const thead = document.createElement("thead");
    const header = document.createElement("tr");
    ["Rank", "Node", metric].forEach((label) => header.appendChild(thEl(label)));
    thead.appendChild(header);
    const tbody = document.createElement("tbody");
    top.forEach((node, index) => {
        const row = document.createElement("tr");
        [index + 1, node, fmt(values[node])].forEach((value) => row.appendChild(tdText(value)));
        tbody.appendChild(row);
    });
    table.append(thead, tbody);
    note.textContent = "Showing " + top.length + " of " + nodes.length
        + " nodes with finite metric values, highest first.";
}

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
    setSelectOptions("node-topk-metric", state.metricOrder, state.metricOrder);
    setSelectOptions("node-chart-sort", ["id", ...state.metricOrder], ["id", ...state.metricOrder]);
    // Edge chart.
    setSelectOptions("edge-chart-metric", state.pairOrder, state.pairOrder);
    setSelectOptions("edge-chart-sort", ["pair", ...state.pairOrder], ["source→target", ...state.pairOrder]);
    // Node-pair distribution chart (histogram per metric).
    setSelectOptions("pair-chart-metric", state.nodePairOrder, state.nodePairOrder);
    // Per-community chart.
    setSelectOptions("comm-chart-metric", state.commMetricOrder, state.commMetricOrder);
    setSelectOptions("comm-chart-sort", ["community", ...state.commMetricOrder], ["community", ...state.commMetricOrder]);
    // Per-attribute-value chart.
    setSelectOptions("attrcomm-chart-metric", state.attributeMetricOrder, state.attributeMetricOrder);
    setSelectOptions("attrcomm-chart-sort", ["attribute", ...state.attributeMetricOrder], ["attribute value", ...state.attributeMetricOrder]);
    setSelectOptions("edgeattr-chart-metric", state.edgeAttributeMetricOrder, state.edgeAttributeMetricOrder);
    setSelectOptions("edgeattr-chart-sort", ["attribute", ...state.edgeAttributeMetricOrder], ["attribute value", ...state.edgeAttributeMetricOrder]);

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

// Wires a small dropdown menu: `buttonId` toggles the `<ul class="menu">`, which closes on selection, on a click
// outside, or on Escape. `onSelect` receives the chosen item's data-act value.
function setupMenu(buttonId, menuId, onSelect) {
    const btn = $(buttonId), menu = $(menuId);
    if (!btn || !menu) return;

    const onOutside = (e) => { if (!menu.contains(e.target) && e.target !== btn) close(); };
    const onKey = (e) => { if (e.key === "Escape") close(); };
    function close() {
        toggleHidden(menuId, true);
        document.removeEventListener("mousedown", onOutside, true);
        document.removeEventListener("keydown", onKey, true);
    }

    btn.addEventListener("click", (e) => {
        e.stopPropagation();
        if (!menu.hidden) { close(); return; }
        toggleHidden(menuId, false);
        document.addEventListener("mousedown", onOutside, true);
        document.addEventListener("keydown", onKey, true);
    });
    menu.querySelectorAll("button[data-act]").forEach((item) =>
        item.addEventListener("click", () => { close(); onSelect(item.dataset.act); }));
}

function setupSettingsMenu() {
    const btn = $("settings-button"), menu = $("settings-menu");
    if (!btn || !menu) return;
    const onOutside = (e) => { if (!menu.contains(e.target) && !btn.contains(e.target)) close(); };
    const onKey = (e) => {
        if (e.key !== "Escape") return;
        close();
        btn.focus();
        e.preventDefault();
    };
    function close() {
        menu.hidden = true;
        btn.setAttribute("aria-expanded", "false");
        document.removeEventListener("mousedown", onOutside, true);
        document.removeEventListener("keydown", onKey, true);
    }
    btn.addEventListener("click", (e) => {
        e.stopPropagation();
        if (!menu.hidden) { close(); return; }
        menu.hidden = false;
        btn.setAttribute("aria-expanded", "true");
        document.addEventListener("mousedown", onOutside, true);
        document.addEventListener("keydown", onKey, true);
    });
}

function setSelectOptions(id, values, labels) {
    const select = $(id);
    const current = select.value;
    select.innerHTML = "";
    values.forEach((v, i) => select.appendChild(option(v, labels[i])));
    if (values.includes(current)) select.value = current;
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
async function exportGeographicImage(format, diffusion = false) {
    const map = diffusion ? state.diffusion.geographic : window.relisonGeographic;
    try {
        const filename = (diffusion ? "diffusion-map." : "network-map.") + format;
        const exported = await map?.[format === "png" ? "exportPng" : "exportSvg"](filename, {
            includeLegend: $(diffusion ? "diff-export-include-legend" : "export-include-legend").checked,
        });
        if (!exported) throw new Error("The geographic map is not ready to export.");
        setStatus("Geographic " + format.toUpperCase() + " downloaded.");
    } catch (error) { setStatus("Map export failed: " + error.message, "error"); }
}
// Nodes/edges are drawn with WebGL, whose buffer reads blank outside a render pass, so we capture inside
// sigma's own "afterRender" event (synchronously, before the buffer is cleared) to match the on-screen plot.
async function exportPng() {
    if (usingGeographic()) { await exportGeographicImage("png"); return; }
    if (usingCosmograph()) {
        try {
            const exported = await window.relisonCosmograph?.exportPng("network.png", { legend: $("export-include-legend").checked ? state.colorLegend : null });
            if (!exported) { setStatus("Cosmograph is not ready to export yet.", "error"); return; }
            setStatus("Cosmograph PNG downloaded.");
        } catch (error) {
            setStatus("PNG export failed: " + error.message, "error");
        }
        return;
    }
    if (!state.renderer) { setStatus("Load a network first.", "error"); return; }
    const r = state.renderer;
    const capture = () => {
        if (typeof r.off === "function") r.off("afterRender", capture);
        else if (typeof r.removeListener === "function") r.removeListener("afterRender", capture);
        try { compositePng().catch(e => setStatus("PNG export failed: " + e.message, "error")); }
        catch (e) { console.error(e); setStatus("PNG export failed: " + e.message, "error"); }
    };
    r.on("afterRender", capture);
    r.refresh(); // schedules a render; afterRender fires at the end of it
}

async function exportSvg() {
    if (usingGeographic()) { await exportGeographicImage("svg"); return; }
    if (usingCosmograph()) {
        const exported = await window.relisonCosmograph?.exportSvg("network.svg", { legend: $("export-include-legend").checked ? state.colorLegend : null });
        if (!exported) { setStatus("Cosmograph is not ready to export yet.", "error"); return; }
        setStatus("Cosmograph SVG downloaded.");
        return;
    }
    try {
        const svg = buildSigmaSvg();
        if (!svg) { setStatus("Sigma is not ready to export yet.", "error"); return; }
        download("network.svg", window.relisonGeographicExport.withLegend(svg, $("export-include-legend").checked ? state.colorLegend : null), "image/svg+xml;charset=utf-8");
        setStatus("Sigma SVG downloaded.");
    } catch (error) {
        console.error("Sigma SVG export failed", error);
        setStatus("SVG export failed: " + error.message, "error");
    }
}

// Reconstruct Sigma's current viewport as vector primitives. Display data is
// resolved after node/edge reducers, so hidden and dimmed selections match the
// live graph without rasterizing Sigma's WebGL canvas.
function buildSigmaSvg() {
    const renderer = state.renderer, graph = state.graph;
    if (!renderer || !graph || !renderer.getNodeDisplayData || !renderer.getEdgeDisplayData) return null;
    const container = $("sigma-container");
    const width = container.clientWidth, height = container.clientHeight;
    if (!width || !height) return null;
    const scaleSize = (size) => renderer.scaleSize ? renderer.scaleSize(size) : size / Math.sqrt(renderer.getCamera().getState().ratio || 1);
    const esc = (value) => String(value ?? "").replace(/[&<>\"']/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&apos;" })[ch]);
    const point = (data) => renderer.graphToViewport({ x: data.x, y: data.y });
    const positions = new Map();
    graph.forEachNode((node) => {
        const d = renderer.getNodeDisplayData(node);
        if (!d || d.hidden || !Number.isFinite(d.x) || !Number.isFinite(d.y)) return;
        positions.set(node, { ...point(d), radius: Math.max(0, scaleSize(d.size || 0)), data: d });
    });
    const background = cssVar("--canvas-bg", "#18191c");
    const edgeLabels = renderer.getEdgeDisplayedLabels?.() || new Set();
    const nodeLabels = renderer.getNodeDisplayedLabels?.() || new Set();
    const edgeMarkup = [];
    graph.forEachEdge((edge, attrs, source, target) => {
        const s = positions.get(source), t = positions.get(target);
        const d = renderer.getEdgeDisplayData(edge);
        if (!s || !t || !d || d.hidden) return;
        const dx = t.x - s.x, dy = t.y - s.y, distance = Math.hypot(dx, dy) || 1;
        const ux = dx / distance, uy = dy / distance;
        const strokeWidth = Math.max(0.25, scaleSize(d.size || 1));
        const color = d.color || attrs.color || "#999999";
        const curved = $("edge-shape").value === "curved" && window.relisonSigmaEdgePrograms;
        const controlX = (s.x + t.x) / 2 - uy * distance * 0.25;
        const controlY = (s.y + t.y) / 2 + ux * distance * 0.25;
        const tangentLength = curved ? (Math.hypot(t.x - controlX, t.y - controlY) || 1) : distance;
        const arrowUx = curved ? (t.x - controlX) / tangentLength : ux;
        const arrowUy = curved ? (t.y - controlY) / tangentLength : uy;
        const arrow = state.directed;
        const arrowLength = arrow ? Math.max(6, strokeWidth * 4) : 0;
        const lineEnd = Math.max(0, t.radius - (arrow ? arrowLength * 0.65 : 0));
        const x2 = t.x - arrowUx * lineEnd, y2 = t.y - arrowUy * lineEnd;
        edgeMarkup.push(curved
            ? '<path d="M ' + s.x + ' ' + s.y + ' Q ' + controlX + ' ' + controlY + ' ' + x2 + ' ' + y2
                + '" fill="none" stroke="' + esc(color) + '" stroke-width="' + strokeWidth + '"/>'
            : '<line x1="' + s.x + '" y1="' + s.y + '" x2="' + x2 + '" y2="' + y2
                + '" stroke="' + esc(color) + '" stroke-width="' + strokeWidth + '"/>');
        if (arrow && distance > 1) {
            const tipX = t.x - arrowUx * Math.max(1, t.radius * 0.45), tipY = t.y - arrowUy * Math.max(1, t.radius * 0.45);
            const baseX = tipX - arrowUx * arrowLength, baseY = tipY - arrowUy * arrowLength;
            const half = Math.max(2.5, arrowLength * 0.34);
            edgeMarkup.push('<polygon points="' + tipX + ',' + tipY + ' ' + (baseX - arrowUy * half) + ',' + (baseY + arrowUx * half)
                + ' ' + (baseX + arrowUy * half) + ',' + (baseY - arrowUx * half) + '" fill="' + esc(color) + '"/>');
        }
        const label = attrs.label;
        if (label != null && label !== "" && (edgeLabels.has(edge) || (edgeLabels.size === 0 && state.labelOpts.edgeShow))) {
            const fontSize = state.labelOpts.edgeProp ? Math.max(5, (d.size || 1) * (state.labelOpts.edgeSize / 2)) : state.labelOpts.edgeSize;
            const labelX = curved ? ((s.x + 2 * controlX + t.x) / 4) : ((s.x + t.x) / 2);
            const labelY = curved ? ((s.y + 2 * controlY + t.y) / 4) : ((s.y + t.y) / 2);
            edgeMarkup.push('<text x="' + labelX + '" y="' + labelY
                + '" text-anchor="middle" fill="' + esc(state.labelOpts.edgeColor || labelTextColor()) + '" font-family="' + esc(state.labelOpts.edgeFont || "sans-serif")
                + '" font-size="' + fontSize + '">' + esc(label) + '</text>');
        }
    });
    const nodeMarkup = [];
    for (const [node, p] of positions) {
        const d = p.data;
        nodeMarkup.push('<circle cx="' + p.x + '" cy="' + p.y + '" r="' + p.radius + '" fill="' + esc(d.color || "#4f9dff") + '"/>');
        if (state.nodeBorder.on) {
            const verdict = overlayNodeVerdict(node, focusContext());
            if (verdict !== "hidden") nodeMarkup.push('<circle cx="' + p.x + '" cy="' + p.y + '" r="' + p.radius
                + '" fill="none" stroke="' + esc(verdict === "dim" ? cssVar("--border", "#3a3c41") : state.nodeBorder.color)
                + '" stroke-width="' + state.nodeBorder.width + '"/>');
        }
        const label = graph.getNodeAttribute(node, "label");
        if (label != null && label !== "" && (nodeLabels.has(node) || (nodeLabels.size === 0 && state.labelOpts.nodeShow))) {
            const fontSize = state.labelOpts.nodeProp ? Math.max(6, d.size * (state.labelOpts.nodeSize / 8)) : state.labelOpts.nodeSize;
            nodeMarkup.push('<text x="' + (p.x + p.radius + 3) + '" y="' + (p.y + fontSize / 3)
                + '" fill="' + esc(state.labelOpts.nodeColor || labelTextColor()) + '" font-family="' + esc(state.labelOpts.nodeFont || "sans-serif")
                + '" font-size="' + fontSize + '">' + esc(label) + '</text>');
        }
    }
    const overlayMarkup = [];
    const model = state.rec.active && state.rec.models[state.rec.active];
    if (model && state.rec.show) {
        const context = focusContext();
        for (const link of model.edges) {
            const s = positions.get(link.source), t = positions.get(link.target);
            if (!s || !t) continue;
            const verdict = overlayEdgeVerdict(link.source, link.target, context);
            if (verdict === "hidden") continue;
            const color = verdict === "dim" ? context.dimColor : (context.pathEdgeColors.get(pairKey(link.source, link.target)) || (context.path && context.pathEdgeColor) || state.rec.color);
            const dash = state.rec.diff ? ' stroke-dasharray="6 4"' : "";
            const thickness = 1.5 * (context.path?.edges.has(pairKey(link.source, link.target)) ? state.pathAppearance.edgeScale : 1);
            if ($("edge-shape").value === "curved") {
                const dx = t.x - s.x, dy = t.y - s.y, length = Math.hypot(dx, dy) || 1;
                const cx = (s.x + t.x) / 2 - dy / length * length * 0.25;
                const cy = (s.y + t.y) / 2 + dx / length * length * 0.25;
                overlayMarkup.push('<path d="M ' + s.x + ' ' + s.y + ' Q ' + cx + ' ' + cy + ' ' + t.x + ' ' + t.y
                    + '" fill="none" stroke="' + esc(color) + '" stroke-width="' + thickness + '"' + dash + '/>');
            } else overlayMarkup.push('<line x1="' + s.x + '" y1="' + s.y + '" x2="' + t.x + '" y2="' + t.y
                + '" stroke="' + esc(color) + '" stroke-width="' + thickness + '"' + dash + '/>');
            if (state.directed && model.edges.length <= 1500 && verdict !== "dim") {
                const angle = Math.atan2(t.y - s.y, t.x - s.x), len = 7, off = 10;
                const tx = t.x - Math.cos(angle) * off, ty = t.y - Math.sin(angle) * off;
                overlayMarkup.push('<polygon points="' + tx + ',' + ty + ' ' + (tx - len * Math.cos(angle - Math.PI / 7)) + ',' + (ty - len * Math.sin(angle - Math.PI / 7))
                    + ' ' + (tx - len * Math.cos(angle + Math.PI / 7)) + ',' + (ty - len * Math.sin(angle + Math.PI / 7)) + '" fill="' + esc(color) + '"/>');
            }
        }
    }
    return '<?xml version="1.0" encoding="UTF-8"?>\n<svg xmlns="http://www.w3.org/2000/svg" width="' + width + '" height="' + height
        + '" viewBox="0 0 ' + width + ' ' + height + '"><rect width="100%" height="100%" fill="' + esc(background) + '"/>'
        + edgeMarkup.join("") + overlayMarkup.join("") + nodeMarkup.join("") + '</svg>';
}

async function compositePng() {
    const canvases = $("sigma-container").querySelectorAll("canvas");
    if (!canvases.length) return;
    const w = canvases[0].width, h = canvases[0].height;
    const out = document.createElement("canvas");
    out.width = w; out.height = h;
    const ctx = out.getContext("2d");
    ctx.fillStyle = cssVar("--canvas-bg", "#18191c");
    ctx.fillRect(0, 0, w, h);
    canvases.forEach((c) => ctx.drawImage(c, 0, 0, w, h));
    await window.relisonGeographicExport.canvasLegend(out, $("export-include-legend").checked ? state.colorLegend : null,
        w / Math.max(1, $("sigma-container").clientWidth));
    out.toBlob((blob) => download("network.png", blob, "image/png"), "image/png");
}

function cssVar(name, fallback) {
    const v = getComputedStyle(document.body).getPropertyValue(name).trim();
    return v || fallback;
}

// Downloads a chart canvas as a PNG, compositing it over the panel background so the (transparent) plot is legible.
function downloadChartPng(chartId, filename) {
    const chart = typeof chartId === "string" ? ECHARTS.get(chartId)
        : (chartId && typeof chartId.getDataURL === "function" ? chartId : ECHARTS.get(chartId?.id));
    if (!chart || chart.isDisposed() || !chart.getWidth() || !chart.getHeight()) {
        setStatus("Nothing to download yet — draw the chart first.", "error"); return;
    }
    const url = chart.getDataURL({ type: "png", pixelRatio: 2, backgroundColor: echartColors().background, excludeComponents: ["dataZoom"] });
    fetch(url).then((response) => response.blob()).then((blob) => download(filename || "chart.png", blob, "image/png"));
}

function exportGlobalCsv() {
    const rows = Object.keys(state.graphMetrics).map((k) => [k, state.graphMetrics[k]]);
    download("global-metrics.csv", kvCsv("metric", "value", rows), "text/csv");
}

function exportAveragesCsv(name, order, data) {
    const rows = order.map((label) => [label, average(data[label])]);
    download(name, kvCsv("metric", "average", rows), "text/csv");
}

/* --------------------------- session save / open --------------------------- */
// A ".relison" file is a JSON document holding the whole working session. The server owns and rebuilds the network,
// its attributes, the community partitions, the recommendations, the diffusion pieces and the evaluation test set;
// everything below is the client's half — node positions, appearance settings and the computed values it displays —
// which the server stores verbatim and hands straight back.
//
// The raw diffusion simulation is not included (it is streamed to a temp file because it does not fit comfortably in
// memory): each run keeps its metric series, so the plots come back, but scrubbing a run needs it to be run again.

// Appearance controls captured by value, so the visual state returns exactly as it was left.
const SESSION_CONTROLS = [
    "node-label-attribute", "edge-label-attribute",
    "edge-shape",
    "size-by", "node-color-mode", "node-color-single", "color-by", "node-size-uniform", "node-size-min", "node-size-max", "node-size-scale", "node-size-reverse", "node-color-low", "node-color-high",
    "edge-size-by", "edge-size-uniform", "edge-size-min", "edge-size-max", "edge-size-scale", "edge-size-reverse", "edge-color-mode", "edge-color-single", "edge-color-by", "edge-color-low", "edge-color-high",
    "node-border-on", "node-border-color", "node-border-width", "layout-type",
    "layout-radius", "layout-spacing", "layout-columns", "layout-width", "layout-height", "layout-seed", "layout-score", "layout-score-reverse", "circlepack-group",
    "layout-preset-x", "layout-preset-y",
    "layout-geographic-latitude", "layout-geographic-longitude", "layout-geographic-projection",
    "layout-iterations", "layout-theta", "layout-tolerance", "layout-fr-bounded", "layout-fr-width", "layout-fr-height", "layout-fr-temperature",
    "layout-ideal-length", "layout-cooling", "layout-weighted",
    "layout-fa-scaling", "layout-fa-gravity", "layout-jitter", "layout-weight-influence", "layout-linlog", "layout-strong-gravity",
    "layout-outbound", "layout-adjust-sizes", "layout-normalize-weights", "layout-invert-weights",
    "layout-warm-start", "layout-remove-overlap", "layout-node-radius", "layout-overlap-gap",
    "layout-root", "layout-direction", "layout-column-spacing", "layout-level-spacing", "layout-sweeps", "layout-pack-components", "layout-packing-gap",
];

function collectSessionClientState() {
    const positions = {};
    if (state.graph) state.graph.forEachNode((n, a) => { positions[n] = { x: a.x, y: a.y, size: a.size, color: a.color }; });

    const controls = {};
    for (const id of SESSION_CONTROLS) {
        const el = $(id);
        if (!el) continue;
        controls[id] = el.type === "checkbox" ? el.checked : el.value;
    }

    return {
        positions,
        savedLayoutPositions: state.savedLayoutPositions?.graphId === state.graphId ? state.savedLayoutPositions.positions : null,
        controls,
        metrics: {
            metricData: state.metricData, metricOrder: state.metricOrder,
            pairData: state.pairData, pairOrder: state.pairOrder,
            nodePairAgg: state.nodePairAgg, nodePairOrder: state.nodePairOrder,
            graphMetrics: state.graphMetrics,
            communityData: state.communityData,
            commMetricData: state.commMetricData, commMetricOrder: state.commMetricOrder,
            attributeMetricData: state.attributeMetricData, attributeMetricOrder: state.attributeMetricOrder,
            edgeAttributeMetricData: state.edgeAttributeMetricData, edgeAttributeMetricOrder: state.edgeAttributeMetricOrder,
        },
        rec: { models: state.rec.models, active: state.rec.active, tableEdges: state.rec.tableEdges,
               show: state.rec.show, diff: state.rec.diff, color: state.rec.color },
        recEval: { result: state.recEval.result, test: state.recEval.test },
        diffusion: { pieces: state.diffusion.pieces, realPropagated: state.diffusion.realPropagated,
                     featureParams: state.diffusion.featureParams, runs: state.diffusion.runs },
    };
}

// Applies the client half on top of a graph the server has already installed (applyLoadedGraph has run and reset
// everything, so this only ever adds state back).
function applySessionClientState(c) {
    if (!c) return;
    state.savedLayoutPositions = c.savedLayoutPositions ? { graphId: state.graphId, positions: c.savedLayoutPositions } : null;

    if (c.positions && state.graph) {
        state.graph.forEachNode((n) => {
            const p = c.positions[n];
            if (!p) return;
            if (p.x != null) state.graph.setNodeAttribute(n, "x", p.x);
            if (p.y != null) state.graph.setNodeAttribute(n, "y", p.y);
            if (p.size != null) state.graph.setNodeAttribute(n, "size", p.size);
            if (p.color != null) state.graph.setNodeAttribute(n, "color", p.color);
        });
    }

    const m = c.metrics || {};
    if (m.metricData) { state.metricData = m.metricData; state.metricOrder = m.metricOrder || []; }
    if (m.pairData) { state.pairData = m.pairData; state.pairOrder = m.pairOrder || []; }
    if (m.nodePairAgg) { state.nodePairAgg = m.nodePairAgg; state.nodePairOrder = m.nodePairOrder || []; }
    if (m.graphMetrics) state.graphMetrics = m.graphMetrics;
    if (m.communityData) state.communityData = m.communityData;
    if (m.commMetricData) { state.commMetricData = m.commMetricData; state.commMetricOrder = m.commMetricOrder || []; }
    if (m.attributeMetricData) { state.attributeMetricData = m.attributeMetricData; state.attributeMetricOrder = m.attributeMetricOrder || []; }
    if (m.edgeAttributeMetricData) { state.edgeAttributeMetricData = m.edgeAttributeMetricData; state.edgeAttributeMetricOrder = m.edgeAttributeMetricOrder || []; }

    if (c.rec) {
        state.rec.models = c.rec.models || {};
        state.rec.active = c.rec.active || null;
        state.rec.tableEdges = c.rec.tableEdges || [];
        if (c.rec.show != null) state.rec.show = c.rec.show;
        if (c.rec.diff != null) state.rec.diff = c.rec.diff;
        if (c.rec.color) state.rec.color = c.rec.color;
    }
    if (c.recEval) { state.recEval.result = c.recEval.result || null; state.recEval.test = c.recEval.test || null; }

    if (c.diffusion) {
        state.diffusion.pieces = c.diffusion.pieces || [];
        state.diffusion.realPropagated = c.diffusion.realPropagated || [];
        state.diffusion.featureParams = c.diffusion.featureParams || [];
        state.diffusion.runs = c.diffusion.runs || [];
        state.diffusion.piecesHeaderSig = null;
        state.diffusion.piecesPage = 0;
    }

    // The community-metric buttons are gated on having a partition.
    const algos = Object.keys(state.communityData);
    if (algos.length) {
        setSelectOptions("indiv-comm-partition", algos, algos);
        setSelectOptions("global-comm-partition", algos, algos);
        $("btn-global-comm").disabled = false;
        $("btn-indiv-comm").disabled = false;
        updatePartitionField();
    }

    // Options first (they are built from the restored metrics/communities), then the saved selections.
    rebuildAppearanceOptions();
    for (const id of Object.keys(c.controls || {})) {
        const el = $(id);
        if (!el) continue;
        if (el.type === "checkbox") el.checked = !!c.controls[id];
        else el.value = c.controls[id];
    }
    applyAppearance();
    syncLayoutOptionsForRenderer();

    if (state.recEval.result) renderRecEvalTable(state.recEval.result);
    if (state.recEval.test) {
        $("rec-test-status").textContent = state.recEval.test.edges + " test link(s) over "
            + state.recEval.test.nodes + " user(s); " + state.recEval.test.sharedUsers + " also present in the network.";
    }
    updateRecUI();
    drawRecOverlay();
    renderTable("rec");
    renderPiecesTable();
    updateRecTabAvailability();
    applyReducers();
    refreshAfterCompute();
    if (state.renderer) state.renderer.refresh();
}

async function saveSession() {
    if (!requireGraph()) return;
    setStatus("Saving session…", "busy");
    try {
        const doc = await api("/api/session/save",
            jsonBody({ graphId: state.graphId, client: collectSessionClientState() }));
        download("session.relison", JSON.stringify(doc), "application/json");
        setStatus("Session saved.");
    } catch (e) {
        setStatus("Could not save the session: " + e.message, "error");
    }
}

async function openSession(file) {
    if (!file) return;
    setStatus("Opening session…", "busy");
    try {
        // The file is already JSON, so it goes straight through as the request body.
        const text = await file.text();
        const data = await api("/api/session/load",
            { method: "POST", headers: { "Content-Type": "application/json" }, body: text });
        applyLoadedGraph(data, !!(data.stats && data.stats.multigraph));   // installs the graph and clears everything
        applySessionClientState(data.client);                             // then puts the saved state back
        setStatus("Session opened (" + data.stats.nodes + " nodes, " + data.stats.edges + " edges).");
    } catch (e) {
        setStatus("Could not open the session: " + e.message, "error");
    }
}

/* ------------------------------ report export ----------------------------- */
// Two report formats from the same content (overview, network snapshot, metric tables, community summary, diffusion
// charts). "HTML" builds a self-contained HTML file with the images embedded as data URIs. "PDF" builds a real PDF
// file directly — laid out to A4 pages by the MiniPDF writer in pdf.js (no browser print dialog, no external
// libraries), so the output is tailored to the page and contains only the report content.

function esc(s) {
    return String(s == null ? "" : s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
}

// A key/value table (metadata blocks).
function reportKvTable(rows) {
    if (!rows.length) return "";
    return "<table class='kv'>" + rows.map(([k, v]) => "<tr><th>" + esc(k) + "</th><td>" + esc(fmt(v)) + "</td></tr>").join("") + "</table>";
}

// A two-column data table (metric → value/average).
function reportTwoCol(headA, headB, rows) {
    if (!rows.length) return "";
    const body = rows.map(([k, v]) => "<tr><td>" + esc(k) + "</td><td class='num'>" + esc(fmt(v)) + "</td></tr>").join("");
    return "<table class='data'><thead><tr><th>" + esc(headA) + "</th><th>" + esc(headB) + "</th></tr></thead><tbody>" + body + "</tbody></table>";
}

// An N-column data table (the evaluation grid). The first column is a name, the rest are numeric/right-aligned.
function reportTable(headers, rows) {
    if (!rows.length) return "";
    const head = headers.map((h, i) => "<th" + (i ? " class='num'" : "") + ">" + esc(h) + "</th>").join("");
    const body = rows.map((r) =>
        "<tr>" + r.map((c, i) => "<td" + (i ? " class='num'" : "") + ">" + esc(c) + "</td>").join("") + "</tr>").join("");
    return "<table class='data'><thead><tr>" + head + "</tr></thead><tbody>" + body + "</tbody></table>";
}

// The recommendation evaluation: one row per executed algorithm, one column per metric.
function reportRecEvalHtml() {
    const { headers, rows, meta, note } = recEvalReportData();
    if (!rows) return "";
    let h = "<h2>Recommendation evaluation</h2>" + reportKvTable(meta) + reportTable(headers, rows);
    if (note) h += "<p class='meta'>" + esc(note) + "</p>";
    return h;
}

// Shared shape of the evaluation section, so the HTML and PDF reports stay in step.
function recEvalReportData() {
    const res = state.recEval && state.recEval.result;
    if (!res || !res.rows || !res.rows.length) return {};
    const headers = ["Algorithm", "Cutoff", "Users", ...res.metrics];
    const rows = res.rows.map((r) => [r.algorithm, r.cutoff, r.users,
        ...res.metrics.map((m) => (r.values[m] == null ? "—" : fmt(Number(r.values[m]))))]);
    const meta = [
        ["Population", res.population === "all" ? "all recommended users" : "users with test links"],
        ["Test links evaluated", fmt(res.evaluatedLinks)],
        ["Users evaluated", fmt(res.evaluatedUsers)],
    ];
    const note = res.undefined && res.undefined.length
        ? "Undefined for this population (division by an empty relevant set): " + res.undefined.join(", ") + "."
        : "";
    return { headers, rows, meta, note };
}

// Captures a high-resolution ECharts PNG over the current opaque panel color for HTML/PDF reports.
function chartToDataUrl(chart) {
    if (!chart || chart.isDisposed() || !chart.getWidth() || !chart.getHeight()) return null;
    return chart.getDataURL({ type: "png", pixelRatio: 2, backgroundColor: echartColors().background, excludeComponents: ["dataZoom"] });
}

// The report section a chart belongs to, inferred from its canvas id.
function plotSection(chart) {
    const id = (chart?.getDom() && chart.getDom().id) || "";
    if (id.startsWith("node-")) return "Vertex metrics";
    if (id.startsWith("edge-")) return "Link metrics";
    if (id.startsWith("pair-")) return "Node-pair metrics";
    if (id.startsWith("comm-")) return "Community metrics";
    if (id.startsWith("diff-")) return "Diffusion";
    return "Plots";
}

// Records a chart as it is drawn, so the report can include every plot generated during the session. Keyed by
// canvas + title so redraws of the same plot overwrite (latest kept) while different selections stay distinct.
// Blank / zero-size canvases (a chart that was never actually shown) are ignored.
function capturePlot(chart, title) {
    if (!chart || !state.graph || !state.reportPlots) return;
    const url = chartToDataUrl(chart);
    if (!url) return;
    const key = (chart.getDom().id || "chart") + "::" + (title || "");
    state.reportPlots.set(key, { section: plotSection(chart), title: title || "chart", url });
}

// Snapshots the network (sigma's WebGL layers must be read inside an afterRender pass, as in exportPng).
function captureNetworkDataUrl() {
    return new Promise((resolve) => {
        const r = state.renderer;
        if (!r) { resolve(null); return; }
        const capture = () => {
            if (typeof r.off === "function") r.off("afterRender", capture);
            else if (typeof r.removeListener === "function") r.removeListener("afterRender", capture);
            try {
                const canvases = $("sigma-container").querySelectorAll("canvas");
                if (!canvases.length || !canvases[0].width) { resolve(null); return; }
                const w = canvases[0].width, h = canvases[0].height;
                const out = document.createElement("canvas");
                out.width = w; out.height = h;
                const ctx = out.getContext("2d");
                ctx.fillStyle = cssVar("--canvas-bg", "#18191c");
                ctx.fillRect(0, 0, w, h);
                canvases.forEach((c) => ctx.drawImage(c, 0, 0, w, h));
                resolve(out.toDataURL("image/png"));
            } catch (e) { console.error(e); resolve(null); }
        };
        r.on("afterRender", capture);
        r.refresh();
    });
}

function reportMetricsHtml() {
    let h = "";
    const g = Object.keys(state.graphMetrics);
    if (g.length) h += "<h2>Global metrics</h2>" + reportTwoCol("Metric", "Value", g.map((k) => [k, state.graphMetrics[k]]));
    if (state.metricOrder.length) h += "<h2>Vertex metric averages</h2>" + reportTwoCol("Metric", "Average", state.metricOrder.map((l) => [l, average(state.metricData[l])]));
    if (state.pairOrder.length) h += "<h2>Link metric averages</h2>" + reportTwoCol("Metric", "Average", state.pairOrder.map((l) => [l, average(state.pairData[l])]));
    if (state.nodePairOrder.length) h += "<h2>Node-pair metric averages</h2>" + reportTwoCol("Metric", "Average", state.nodePairOrder.map((l) => [l, state.nodePairAgg[l].average]));
    return h;
}

function reportCommunityHtml() {
    const algos = Object.keys(state.communityData);
    if (!algos.length && !state.commMetricOrder.length) return "";
    let h = "<h2>Communities</h2>";
    for (const algo of algos) {
        const assign = state.communityData[algo];
        const sizes = {};
        for (const node in assign) sizes[assign[node]] = (sizes[assign[node]] || 0) + 1;
        const counts = Object.values(sizes);
        const total = counts.reduce((a, b) => a + b, 0);
        h += "<h3>" + esc(algo) + "</h3>" + reportKvTable([
            ["Communities", counts.length],
            ["Average size", counts.length ? total / counts.length : 0],
            ["Smallest", counts.length ? Math.min(...counts) : 0],
            ["Largest", counts.length ? Math.max(...counts) : 0],
        ]);
    }
    if (state.commMetricOrder.length) {
        h += "<h3>Per-community metric averages</h3>" +
            reportTwoCol("Metric", "Average", state.commMetricOrder.map((l) => [l, average(state.commMetricData[l].values)]));
    }
    return h;
}

function reportDiffusionHtml() {
    const runs = state.diffusion.runs;
    if (!runs.length) return "";
    const rows = runs.map((run) => [run.label, run.numIterations + " iterations, " + run.metrics.length + " metric(s)"]);
    return "<h2>Information diffusion</h2>" + reportTwoCol("Run", "Summary", rows);
}

// Every plot generated during the session, grouped by the section its chart belongs to (in first-drawn order).
function reportPlotsHtml() {
    if (!state.reportPlots || !state.reportPlots.size) return "";
    const bySection = new Map();
    for (const p of state.reportPlots.values()) {
        if (!bySection.has(p.section)) bySection.set(p.section, []);
        bySection.get(p.section).push(p);
    }
    let h = "<h2>Plots</h2>";
    for (const [section, plots] of bySection) {
        h += "<h3>" + esc(section) + "</h3>";
        for (const p of plots) {
            h += "<div class='chart'><div class='chart-title'>" + esc(p.title) + "</div><img src='" + p.url + "' alt='" + esc(p.title) + "'/></div>";
        }
    }
    return h;
}

// Wraps the assembled body in a standalone, print-friendly HTML document.
function reportDocument(body) {
    const style =
        "body{font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#1c1d20;background:#fff;margin:32px auto;max-width:900px;line-height:1.5;}" +
        "h1{font-size:24px;margin:0 0 4px;}h2{font-size:17px;margin:28px 0 10px;border-bottom:1px solid #d3d6db;padding-bottom:4px;}h3{font-size:14px;margin:18px 0 8px;color:#333;}" +
        ".meta{color:#5f6469;font-size:13px;margin:0 0 8px;}" +
        "table{border-collapse:collapse;margin:6px 0 14px;font-size:13px;}" +
        "table.kv th{text-align:left;color:#5f6469;font-weight:600;padding:3px 16px 3px 0;}table.kv td{padding:3px 0;}" +
        "table.data{width:100%;}table.data th,table.data td{border:1px solid #d3d6db;padding:5px 10px;text-align:left;}" +
        "table.data th{background:#eceef1;}table.data td.num{text-align:right;font-variant-numeric:tabular-nums;}" +
        ".snap img,.chart img{max-width:100%;height:auto;border:1px solid #d3d6db;border-radius:6px;}" +
        ".chart{margin:0 0 16px;}.chart-title{font-size:13px;color:#333;margin:0 0 4px;font-weight:600;}" +
        "@media print{body{margin:0;max-width:none;}h2{break-after:avoid;}table,.snap,.chart{break-inside:avoid;}}";
    return "<!DOCTYPE html><html lang='en'><head><meta charset='utf-8'><title>RELISON network report</title><style>" +
        style + "</style></head><body>" + body + "</body></html>";
}

async function buildReportHtml() {
    if (!state.graph) { setStatus("Load a network first.", "error"); return null; }

    // The network snapshot needs the sigma canvas laid out, so briefly switch to the Network tab if we're elsewhere.
    const prevTab = state.activeTab;
    if (state.renderer && prevTab !== "network") { switchTab("network"); await new Promise((r) => setTimeout(r, 80)); }
    const networkImg = await captureNetworkDataUrl();
    if (state.activeTab !== prevTab) switchTab(prevTab);

    // Refresh the diffusion metric chart so the currently-selected metric is captured into the plot registry too.
    if (state.diffusion.runs.length) { try { renderDiffMetrics(); } catch (e) { /* ignore */ } }

    const stats = state.stats || {};
    const parts = [];
    parts.push("<h1>RELISON network report</h1>");
    parts.push("<p class='meta'>Generated " + esc(new Date().toLocaleString()) + (state.graphId ? " &middot; graph " + esc(state.graphId) : "") + "</p>");

    parts.push("<h2>Overview</h2>");
    parts.push(reportKvTable([
        ["Nodes", stats.nodes != null ? stats.nodes : state.graph.order],
        ["Edges", stats.edges != null ? stats.edges : state.graph.size],
        ["Type", (state.directed ? "directed" : "undirected") + (state.weighted ? ", weighted" : ", unweighted") + (state.multigraph ? ", multigraph" : "")],
    ]));

    if (networkImg) parts.push("<h2>Network</h2><div class='snap'><img src='" + networkImg + "' alt='network snapshot'/></div>");

    const metricsHtml = reportMetricsHtml();
    if (metricsHtml) parts.push(metricsHtml);

    const commHtml = reportCommunityHtml();
    if (commHtml) parts.push(commHtml);

    const evalHtml = reportRecEvalHtml();
    if (evalHtml) parts.push(evalHtml);

    const diffHtml = reportDiffusionHtml();
    if (diffHtml) parts.push(diffHtml);

    const plotsHtml = reportPlotsHtml();
    if (plotsHtml) parts.push(plotsHtml);

    return reportDocument(parts.join("\n"));
}

async function exportReportHtml() {
    setStatus("Building report…", "busy");
    const html = await buildReportHtml();
    if (!html) return;
    download("relison-report.html", html, "text/html");
    setStatus("Report exported.");
}

// Assembles the report as a list of layout blocks (headings, key/value blocks, data tables, images), the structured
// form the direct-PDF renderer consumes. The same content the HTML report shows, but as data rather than markup.
function buildReportModel(networkImg) {
    const stats = state.stats || {};
    const blocks = [];
    blocks.push({ t: "h1", s: "RELISON network report" });
    blocks.push({ t: "meta", s: "Generated " + new Date().toLocaleString() + (state.graphId ? "  ·  graph " + state.graphId : "") });

    blocks.push({ t: "h2", s: "Overview" });
    blocks.push({ t: "kv", rows: [
        ["Nodes", fmt(stats.nodes != null ? stats.nodes : state.graph.order)],
        ["Edges", fmt(stats.edges != null ? stats.edges : state.graph.size)],
        ["Type", (state.directed ? "directed" : "undirected") + (state.weighted ? ", weighted" : ", unweighted") + (state.multigraph ? ", multigraph" : "")],
    ] });

    if (networkImg) { blocks.push({ t: "h2", s: "Network" }); blocks.push({ t: "image", url: networkImg }); }

    // Metric tables.
    const g = Object.keys(state.graphMetrics);
    if (g.length) { blocks.push({ t: "h2", s: "Global metrics" }); blocks.push({ t: "table", a: "Metric", b: "Value", rows: g.map((k) => [k, fmt(state.graphMetrics[k])]) }); }
    if (state.metricOrder.length) { blocks.push({ t: "h2", s: "Vertex metric averages" }); blocks.push({ t: "table", a: "Metric", b: "Average", rows: state.metricOrder.map((l) => [l, fmt(average(state.metricData[l]))]) }); }
    if (state.pairOrder.length) { blocks.push({ t: "h2", s: "Link metric averages" }); blocks.push({ t: "table", a: "Metric", b: "Average", rows: state.pairOrder.map((l) => [l, fmt(average(state.pairData[l]))]) }); }
    if (state.nodePairOrder.length) { blocks.push({ t: "h2", s: "Node-pair metric averages" }); blocks.push({ t: "table", a: "Metric", b: "Average", rows: state.nodePairOrder.map((l) => [l, fmt(state.nodePairAgg[l].average)]) }); }

    // Communities.
    const algos = Object.keys(state.communityData);
    if (algos.length || state.commMetricOrder.length) {
        blocks.push({ t: "h2", s: "Communities" });
        for (const algo of algos) {
            const assign = state.communityData[algo];
            const sizes = {};
            for (const node in assign) sizes[assign[node]] = (sizes[assign[node]] || 0) + 1;
            const counts = Object.values(sizes);
            const total = counts.reduce((a, b) => a + b, 0);
            blocks.push({ t: "h3", s: algo });
            blocks.push({ t: "kv", rows: [
                ["Communities", fmt(counts.length)],
                ["Average size", fmt(counts.length ? total / counts.length : 0)],
                ["Smallest", fmt(counts.length ? Math.min(...counts) : 0)],
                ["Largest", fmt(counts.length ? Math.max(...counts) : 0)],
            ] });
        }
        if (state.commMetricOrder.length) {
            blocks.push({ t: "h3", s: "Per-community metric averages" });
            blocks.push({ t: "table", a: "Metric", b: "Average", rows: state.commMetricOrder.map((l) => [l, fmt(average(state.commMetricData[l].values))]) });
        }
    }

    // Recommendation evaluation: one row per executed algorithm, one column per metric.
    const ev = recEvalReportData();
    if (ev.rows) {
        blocks.push({ t: "h2", s: "Recommendation evaluation" });
        blocks.push({ t: "kv", rows: ev.meta });
        blocks.push({ t: "grid", headers: ev.headers, rows: ev.rows });
        if (ev.note) blocks.push({ t: "meta", s: ev.note });
    }

    // Diffusion run summary.
    if (state.diffusion.runs.length) {
        blocks.push({ t: "h2", s: "Information diffusion" });
        blocks.push({ t: "table", a: "Run", b: "Summary", rows: state.diffusion.runs.map((run) => [run.label, run.numIterations + " iters, " + run.metrics.length + " metric(s)"]) });
    }

    // Every plot generated during the session, grouped by the section its chart belongs to.
    if (state.reportPlots && state.reportPlots.size) {
        const bySection = new Map();
        for (const p of state.reportPlots.values()) { if (!bySection.has(p.section)) bySection.set(p.section, []); bySection.get(p.section).push(p); }
        blocks.push({ t: "h2", s: "Plots" });
        for (const [section, plots] of bySection) {
            blocks.push({ t: "h3", s: section });
            for (const p of plots) blocks.push({ t: "image", url: p.url, title: p.title });
        }
    }

    return blocks;
}

// Renders a report model into a MiniPDF document (images must already be registered onto the blocks as `rec`).
function renderModelToPdf(pdf, blocks) {
    for (const b of blocks) {
        if (b.t === "h1") pdf.h1(b.s);
        else if (b.t === "meta") pdf.meta(b.s);
        else if (b.t === "h2") pdf.h2(b.s);
        else if (b.t === "h3") pdf.h3(b.s);
        else if (b.t === "kv") pdf.kvTable(b.rows);
        else if (b.t === "table") pdf.dataTable(b.a, b.b, b.rows);
        else if (b.t === "grid") pdf.gridTable(b.headers, b.rows);
        else if (b.t === "image" && b.rec) pdf.imageBlock(b.rec, b.title);
    }
}

// Builds a real PDF file directly (no browser print dialog): laid out to A4 pages, with only the report content.
async function exportReportPdf() {
    if (!state.graph) { setStatus("Load a network first.", "error"); return; }
    if (typeof MiniPDF === "undefined") { setStatus("PDF module failed to load.", "error"); return; }
    setStatus("Building PDF…", "busy");
    try {
        // Snapshot the network (its sigma canvas must be laid out, so briefly switch to the Network tab if elsewhere).
        const prevTab = state.activeTab;
        if (state.renderer && prevTab !== "network") { switchTab("network"); await new Promise((r) => setTimeout(r, 80)); }
        const networkImg = await captureNetworkDataUrl();
        if (state.activeTab !== prevTab) switchTab(prevTab);
        // Refresh the diffusion metric chart so the currently-selected metric is captured into the plot registry too.
        if (state.diffusion.runs.length) { try { renderDiffMetrics(); } catch (e) { /* ignore */ } }

        const blocks = buildReportModel(networkImg);
        const pdf = new MiniPDF();
        for (const b of blocks) { if (b.t === "image" && b.url) b.rec = await pdf.registerImage(b.url); }
        renderModelToPdf(pdf, blocks);

        const bytes = pdf.build();
        download("relison-report.pdf", new Blob([bytes], { type: "application/pdf" }), "application/pdf");
        setStatus("PDF report exported.");
    } catch (e) {
        console.error(e);
        setStatus("Could not build the PDF: " + e.message, "error");
    }
}

function exportCommAveragesCsv() {
    const rows = state.commMetricOrder.map((label) => [label, average(state.commMetricData[label].values)]);
    download("community-metric-averages.csv", kvCsv("metric", "average", rows), "text/csv");
}

function exportAttributeCommAveragesCsv() {
    const rows = state.attributeMetricOrder.map((label) => [label, average(state.attributeMetricData[label].values)]);
    download("attribute-metric-averages.csv", kvCsv("metric", "average", rows), "text/csv");
}
function exportEdgeAttributeAveragesCsv() {
    const rows = state.edgeAttributeMetricOrder.map((label) => [label, average(state.edgeAttributeMetricData[label].values)]);
    download("edge-attribute-metric-averages.csv", kvCsv("metric", "average", rows), "text/csv");
}
function kvCsv(keyHeader, valueHeader, rows) {
    const lines = [csvCell(keyHeader) + "," + csvCell(valueHeader)];
    for (const [k, v] of rows) lines.push(csvCell(k) + "," + csvCell(v));
    return lines.join("\n");
}

// Serializes the current graph (with computed node/edge attributes) to GEXF for Gephi/other tools.
function exportGexf() {
    if (!state.graph) { setStatus("Load a network first.", "error"); return; }
    // GEXF is graph data, rather than an image. When Cosmograph is active,
    // persist its current live coordinates first so the exported viz:position
    // entries reproduce the layout the user is looking at.
    if (usingCosmograph()) window.relisonCosmograph?.savePointPositions();
    const g = state.graph;
    const numericIds = $("export-gexf-numeric-ids").checked;
    const exportIds = new Map(g.nodes().map((node, index) => [node, numericIds ? String(index) : node]));
    const esc = (s) => String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");

    const gexfType = type => ({ int: "integer", integer: "integer", long: "long", double: "double",
        float: "float", bool: "boolean", boolean: "boolean" }[type] || "string");
    // Keep custom columns intact; disambiguate computed columns with the same title.
    const columns = (defs, computed) => {
        const used = new Set(defs.map(d => d.name));
        const result = defs.map(d => ({ name: d.name, type: gexfType(d.type), custom: true, key: d.name }));
        for (const key of computed) {
            let name = key;
            while (used.has(name)) name = "computed:" + name;
            used.add(name);
            result.push({ name, type: "double", custom: false, key });
        }
        return result;
    };
    const nodeAttrs = columns(nodeAttrDefs(), [...state.metricOrder, ...Object.keys(state.communityData).map(a => "community:" + a)]);
    const edgeAttrs = columns(edgeAttrDefs(), state.pairOrder);
    const nodeAttrId = (i) => "n" + i;
    const edgeAttrId = (i) => "e" + i;

    const lines = [];
    lines.push('<?xml version="1.0" encoding="UTF-8"?>');
    lines.push('<gexf xmlns="http://www.gexf.net/1.2draft" xmlns:viz="http://www.gexf.net/1.2draft/viz" version="1.2">');
    lines.push('<graph defaultedgetype="' + (state.directed ? "directed" : "undirected") + '">');

    if (nodeAttrs.length) {
        lines.push('<attributes class="node">');
        nodeAttrs.forEach((a, i) => lines.push('<attribute id="' + nodeAttrId(i) + '" title="' + esc(a.name) + '" type="' + a.type + '"/>'));
        lines.push('</attributes>');
    }
    if (edgeAttrs.length) {
        lines.push('<attributes class="edge">');
        edgeAttrs.forEach((a, i) => lines.push('<attribute id="' + edgeAttrId(i) + '" title="' + esc(a.name) + '" type="' + a.type + '"/>'));
        lines.push('</attributes>');
    }

    lines.push('<nodes>');
    g.forEachNode((node, attr) => {
        lines.push('<node id="' + esc(exportIds.get(node)) + '" label="' + esc(numericIds ? node : (attr.label ?? node)) + '">');
        if (attr.x !== undefined && attr.y !== undefined)
            lines.push('<viz:position x="' + attr.x + '" y="' + attr.y + '" z="0"/>');
        if (attr.size !== undefined) lines.push('<viz:size value="' + attr.size + '"/>');
        const rgb = colorToRgb(attr.color);
        if (rgb) lines.push('<viz:color r="' + rgb.r + '" g="' + rgb.g + '" b="' + rgb.b + '"/>');
        if (nodeAttrs.length) {
            lines.push('<attvalues>');
            nodeAttrs.forEach((a, i) => {
                const v = a.custom ? attr.attrs?.[a.key] : a.key.startsWith("community:")
                    ? state.communityData[a.key.slice(10)]?.[node] : state.metricData[a.key]?.[node];
                if (v !== undefined && v !== null) lines.push('<attvalue for="' + nodeAttrId(i) + '" value="' + esc(v) + '"/>');
            });
            lines.push('</attvalues>');
        }
        lines.push('</node>');
    });
    lines.push('</nodes>');

    lines.push('<edges>');
    let ei = 0;
    g.forEachEdge((edge, attr, s, t) => {
        const label = attr.attrs?.label ?? attr.label;
        const labelField = label != null ? ' label="' + esc(label) + '"' : "";
        const weight = attr.weight !== undefined ? ' weight="' + attr.weight + '"' : "";
        lines.push('<edge id="' + (ei++) + '" source="' + esc(exportIds.get(s)) + '" target="' + esc(exportIds.get(t)) + '"' + weight + labelField + '>');
        if (edgeAttrs.length) {
            lines.push('<attvalues>');
            edgeAttrs.forEach((a, i) => {
                const v = a.custom ? attr.attrs?.[a.key] : state.pairData[a.key]?.[pairKey(s, t)];
                if (v !== undefined && v !== null) lines.push('<attvalue for="' + edgeAttrId(i) + '" value="' + esc(v) + '"/>');
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
    const attrs = defaultNodeAttrs(id);
    try {
        await applyAddNode(id, attrs);
        $("add-node-id").value = "";
        historyPush("add node " + id, () => applyRemoveNode(id), () => applyAddNode(id, attrs));
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
    const prefix = kind === "nodes" ? "node" : "edge";
    form.append("separator", selectedDelimiter(prefix + "-attr-separator"));
    form.append("header", $(prefix + "-attr-header").checked);
    setStatus("Importing " + kind + " attributes…", "busy");
    try {
        const res = await api("/api/graph/" + state.graphId + "/attributes/" + kind, { method: "POST", body: form });
        state.attrSchema = res.schema || state.attrSchema;
        populateRecEvalFeature();
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
    const light = "img/RELISON-full-logo.png";
    const src = dark ? "img/RELISON-full-logo-dark.png" : light;
    document.querySelectorAll("#logo, .import-logo").forEach((img) => {
        img.onerror = () => { img.onerror = null; img.src = light; };
        img.src = src;
    });
}

function themeIconMarkup(light) {
    return '<span class="material-symbols-outlined" aria-hidden="true">' + (light ? "light_mode" : "dark_mode") + '</span>';
}

function applyTheme(theme) {
    const light = theme === "light";
    document.body.classList.toggle("light", light);
    $("theme-toggle").innerHTML = themeIconMarkup(light);
    applyLogo(!light);
    try { localStorage.setItem("relison-theme", theme); } catch (e) { /* ignore */ }
    redrawVisibleCharts();
}

function redrawVisibleCharts() {
    if (state.activeTab === "metrics") {
        if (state.activeSubtab === "nodes") { drawNodeChart(); drawNodeScatter(); }
        if (state.activeSubtab === "edges") { drawEdgeChart(); drawEdgeScatter(); }
        if (state.activeSubtab === "pairs") drawPairChart();
        if (state.activeSubtab === "comm") { drawCommChart(); drawCommScatter(); }
    if (state.activeSubtab === "attrcomm") drawAttributeCommChart();
    if (state.activeSubtab === "edgeattr") drawEdgeAttributeChart();
    }
    if (state.activeTab === "diffusion") {
        if (state.diffusion.subview === "metrics") renderDiffMetrics();
        else if (state.diffusion.subview === "stats") {
            if (state.diffusion.statsView === "node" && state.diffusion.traj) drawTrajectoryChart();
            if (state.diffusion.statsView === "piece" && state.diffusion.ptraj) drawPieceTimeline();
            if (state.diffusion.statsView === "feat" && state.diffusion.ftraj) drawFeatureTimeline();
            if (state.diffusion.statsView === "dist" && state.diffusion.dist) drawDistribution();
        }
    }
}

function toggleTheme() {
    applyTheme(document.body.classList.contains("light") ? "dark" : "light");
}

/* ------------------------------ debug console ----------------------------- */
function applyVisualStyle(style) { const modern = style !== "classic"; document.body.classList.toggle("modern-slate", modern); document.body.classList.toggle("classic", !modern); $("style-selector").value = modern ? "modern-slate" : "classic"; try { localStorage.setItem("relison-visual-style", modern ? "modern-slate" : "classic"); } catch (e) { /* ignore */ } }
//function applyFocusMode(enabled) { document.body.classList.toggle("focus-mode", enabled); if (!enabled) document.body.classList.remove("inspector-open"); $("focus-toggle").classList.toggle("active", enabled); $("focus-toggle").setAttribute("aria-pressed", String(enabled)); $("focus-toggle").textContent = enabled ? "Focus on" : "Focus"; toggleHidden("inspector-toggle", !enabled); if (!enabled) { $("inspector-toggle").classList.remove("active"); $("inspector-toggle").setAttribute("aria-pressed", "false"); } try { localStorage.setItem("relison-focus-mode", enabled ? "on" : "off"); } catch (e) { /* ignore */ } setTimeout(() => { if (state.renderer) { state.renderer.refresh(); drawRecOverlay(); } if (state.diffusion.renderer) { state.diffusion.renderer.refresh(); drawDiffOverlay(); } }, 0); }
function applyExtendedVisualStyle(style) { const valid = ["classic", "modern-slate", "quiet-light", "graph-first-dark"]; const next = valid.includes(style) ? style : "modern-slate"; document.body.classList.remove(...valid); document.body.classList.add(next); $("style-selector").value = next; try { localStorage.setItem("relison-visual-style", next); } catch (e) { /* ignore */ } setTimeout(() => applyTheme(document.body.classList.contains("light") ? "light" : "dark"), 0); }
// Available only when the server was launched with --debug. The button reveals a terminal-like overlay that mirrors
//function toggleFocusMode() { applyFocusMode(!document.body.classList.contains("focus-mode")); }
// the server's standard output / error (polled incrementally from /api/logs), replacing everything below the top bar.
//function toggleInspector() { if (!document.body.classList.contains("focus-mode")) return; const open = !document.body.classList.contains("inspector-open"); document.body.classList.toggle("inspector-open", open); $("inspector-toggle").classList.toggle("active", open); $("inspector-toggle").setAttribute("aria-pressed", String(open)); }
const debugConsole = { enabled: false, on: false, cursor: 0, timer: null };

async function initDebugConsole() {
    let cfg;
    try { cfg = await api("/api/config"); }
    catch (e) { return; }   // endpoint absent / older build → no debug console
    if (!cfg || !cfg.debug) return;
    debugConsole.enabled = true;
    toggleHidden("debug-terminal", false);
    $("debug-terminal").addEventListener("click", toggleDebugView);
    $("debug-clear").addEventListener("click", () => { $("debug-output").textContent = ""; });
}

function toggleDebugView() {
    debugConsole.on = !debugConsole.on;
    $("debug-view").classList.toggle("active", debugConsole.on);
    $("debug-terminal").classList.toggle("active", debugConsole.on);
    if (debugConsole.on) {
        pollDebugLogs();
        debugConsole.timer = setInterval(pollDebugLogs, 1200);
    } else if (debugConsole.timer) {
        clearInterval(debugConsole.timer);
        debugConsole.timer = null;
    }
}

async function pollDebugLogs() {
    let res;
    try { res = await api("/api/logs?since=" + debugConsole.cursor); }
    catch (e) { return; }
    if (!res) return;
    const entries = [];
    if (res.dropped) entries.push({ stream: "meta", text: "… (older output dropped) …" });
    if (res.lines && res.lines.length) entries.push(...res.lines);
    if (entries.length) appendDebugLines(entries);
    if (typeof res.cursor === "number") debugConsole.cursor = res.cursor;
}

// Appends captured lines, coloured by their source stream: stdout green, stderr red, meta (markers) muted.
function appendDebugLines(entries) {
    const pre = $("debug-output");
    const frag = document.createDocumentFragment();
    for (const e of entries) {
        const div = document.createElement("div");
        div.className = "log-line " + (
            e.stream === "err" ? "log-err" :
            e.stream === "http" ? "log-http" :
            e.stream === "meta" ? "log-meta" : "log-out");
        div.textContent = e.text || "";
        frag.appendChild(div);
    }
    pre.appendChild(frag);
    const MAX = 5000;
    while (pre.childNodes.length > MAX) pre.removeChild(pre.firstChild);
    if ($("debug-autoscroll").checked) pre.scrollTop = pre.scrollHeight;
}

(function initTheme() {
    let saved = "dark";
    try { saved = localStorage.getItem("relison-theme") || "dark"; } catch (e) { /* ignore */ }
    applyTheme(saved);
})();

(function initVisualStyle() { let saved = "modern-slate"; try { saved = localStorage.getItem("relison-visual-style") || "modern-slate"; } catch (e) { /* ignore */ } applyVisualStyle(saved); })();
// Default label colours to a readable value for the current theme (white-ish on dark, dark on light).
(function initAdditionalVisualStyles() { const selector = $("style-selector"); if (!selector.querySelector('option[value="quiet-light"]')) { selector.add(new Option("Quiet Light", "quiet-light"), 1); selector.add(new Option("Graph-first Dark", "graph-first-dark"), 2); } let saved = "modern-slate"; try { saved = localStorage.getItem("relison-visual-style") || "modern-slate"; } catch (e) { /* ignore */ } applyExtendedVisualStyle(saved); })();
//(function initFocusMode() { let saved = "off"; try { saved = localStorage.getItem("relison-focus-mode") || "off"; } catch (e) { /* ignore */ } applyFocusMode(saved === "on"); })();
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
    if (jobBusy("btn-rec-apply")) return cancelJob("btn-rec-apply");
    if (!requireGraph()) return;
    if (state.multigraph) { setStatus("Recommendation is not available for multigraphs.", "error"); return; }
    const algorithm = $("rec-algo").value;
    const mode = $("rec-mode").value;
    const cutoff = Math.max(1, parseInt($("rec-cutoff").value, 10) || 10);
    const reciprocal = $("rec-reciprocal").checked;
    setStatus("Applying " + algorithm + "…", "busy");
    const signal = beginJob("btn-rec-apply", "recommendation");
    try {
        const res = await api("/api/recommendation/run", { ...jsonBody(
            { graphId: state.graphId, algorithm, mode, cutoff, reciprocal, params: collectParams("rec-params") }), signal });
        if (res.cancelled) { setStatus("Stopped."); return; }
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
        if (isAbort(e)) return;
        setStatus(e.message, "error");
    } finally {
        endJob("btn-rec-apply");
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
    const snap = recLinkSnapshot(e);
    try {
        await addRecLinkBatch([snap]);
        historyPush("add recommended link " + e.source + " → " + e.target,
            () => removeRecLinkBatch([snap]),
            () => addRecLinkBatch([snap]));
        setStatus("Added link " + e.source + " → " + e.target + " to the graph.");
    } catch (err) {
        setStatus(err.message, "error");
    }
}

// Commits every recommended link currently listed in the table — honouring its column filters, so filtering by score
// (or endpoint) and pressing "Add all" commits exactly the visible subset — as a single undoable step.
async function addAllRecLinks() {
    if (!state.graph) { setStatus("Load a network first.", "error"); return; }
    if (!state.rec.tableEdges.length) { setStatus("No recommended links to add.", "error"); return; }
    const { ids } = tableRows("rec");   // filtered + sorted, exactly what the table shows
    // Snapshot everything up-front: each commit mutates tableEdges, which would invalidate the remaining indices.
    const snaps = [];
    for (const i of ids) {
        const e = state.rec.tableEdges[i];
        if (!e || state.graph.hasEdge(e.source, e.target)) continue;   // skip links already present in the graph
        snaps.push(recLinkSnapshot(e));
    }
    if (!snaps.length) { setStatus("No new links to add — they are all already in the graph.", "error"); return; }
    // Committing links only invalidates metrics/communities — the recommendation and any diffusion results survive.
    const cleared = computedResultsList();
    const extra = cleared.length ? " This will also clear the " + cleared.join(", ") + "." : "";
    if (!window.confirm("Add " + snaps.length + " recommended link(s) to the graph?" + extra)) return;
    setStatus("Adding " + snaps.length + " recommended link(s)…", "busy");
    try {
        await addRecLinkBatch(snaps);
        historyPush("add " + snaps.length + " recommended link(s)",
            () => removeRecLinkBatch(snaps),
            () => addRecLinkBatch(snaps));
        setStatus("Added " + snaps.length + " recommended link(s) to the graph.");
    } catch (err) {
        setStatus(err.message, "error");
    }
}

// Captures what undo needs to put a committed link back: the link itself and its position in the table and in the
// active model's overlay edges. Indices are read against the current (pre-commit) arrays.
function recLinkSnapshot(e) {
    const modelKey = state.rec.active || null;
    const m = modelKey && state.rec.models[modelKey];
    const same = (x) => x.source === e.source && x.target === e.target;
    return {
        edge: { source: e.source, target: e.target, score: e.score },
        tableIndex: state.rec.tableEdges.findIndex(same),
        modelIndex: m ? m.edges.findIndex(same) : -1,
        modelKey,
    };
}

/* --- rec-link batch helpers: mutate server + client, WITHOUT touching the history stack --- */
// Both directions go through the bulk edge endpoints, so committing a whole recommendation is one request rather than
// one per link, and the recommendation lists are rebuilt in a single pass (not once per link).

async function addRecLinkBatch(snaps) {
    const edges = snaps.map((s) => ({ source: s.edge.source, target: s.edge.target, weight: 1 }));
    const res = await api("/api/graph/" + state.graphId + "/edges", jsonBody({ edges }));
    for (const s of snaps) {
        ensureNode(s.edge.source);
        ensureNode(s.edge.target);
        if (!state.graph.hasEdge(s.edge.source, s.edge.target)) state.graph.addEdge(s.edge.source, s.edge.target, { weight: 1 });
    }
    removeRecLinks(new Set(snaps.map((s) => recKey(s.edge.source, s.edge.target))));   // they are real edges now
    afterRecLinkChange(res.stats);
    return res.stats;
}

async function removeRecLinkBatch(snaps) {
    const edges = snaps.map((s) => ({ source: s.edge.source, target: s.edge.target }));
    const res = await api("/api/graph/" + state.graphId + "/edges",
        { method: "DELETE", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ edges }) });
    for (const s of snaps) {
        if (state.graph.hasEdge(s.edge.source, s.edge.target)) state.graph.dropEdge(s.edge.source, s.edge.target);
    }
    restoreRecLinks(snaps);   // each link lands back in its original slot
    afterRecLinkChange(res.stats);
    return res.stats;
}

// NUL separator: node identifiers are arbitrary strings, so any printable delimiter could collide.
function recKey(s, t) { return s + "\u0000" + t; }

// Drops a set of links from the recommendation table and the active model's overlay edges, in one pass.
function removeRecLinks(keys) {
    const keep = (e) => !keys.has(recKey(e.source, e.target));
    state.rec.tableEdges = state.rec.tableEdges.filter(keep);
    const m = state.rec.active && state.rec.models[state.rec.active];
    if (m) m.edges = m.edges.filter(keep);
}

// Puts committed links back into the recommendation table and their model's overlay edges (the inverse of
// removeRecLinks). Skipped when that recommendation is no longer the active one — a later graph edit clears the
// recommendation state, and there is then no list to restore into; undoing the graph edges themselves is enough.
function restoreRecLinks(snaps) {
    const modelKey = snaps.length ? snaps[0].modelKey : null;
    if (!modelKey || state.rec.active !== modelKey) return;
    const mine = snaps.filter((s) => s.modelKey === modelKey);
    state.rec.tableEdges = mergeRecLinks(state.rec.tableEdges, mine, (s) => s.tableIndex);
    const m = state.rec.models[modelKey];
    if (m) m.edges = mergeRecLinks(m.edges, mine, (s) => s.modelIndex);
}

// Rebuilds a link list with the snapshot links re-inserted at their original positions, in a single pass (splicing
// them back one at a time would be quadratic, which matters when a whole recommendation is undone).
function mergeRecLinks(list, snaps, indexOf) {
    const items = snaps.filter((s) => indexOf(s) >= 0).sort((a, b) => indexOf(a) - indexOf(b));
    if (!items.length) return list;
    const existing = new Set(list.map((e) => recKey(e.source, e.target)));
    const out = [];
    let i = 0;
    for (const s of items) {
        if (existing.has(recKey(s.edge.source, s.edge.target))) continue;   // already there, nothing to restore
        while (out.length < indexOf(s) && i < list.length) out.push(list[i++]);
        out.push(Object.assign({}, s.edge));
    }
    while (i < list.length) out.push(list[i++]);
    return out;
}

// Shared refresh after committing/reverting recommended links: metrics are stale (the graph changed), but the rest of
// the recommendation list is kept so more links can be added.
function afterRecLinkChange(stats) {
    resetResults();
    rebuildAppearanceOptions();
    applyAppearance();
    if (stats) { $("ov-nodes").textContent = stats.nodes; $("ov-edges").textContent = stats.edges; }
    renderTable("rec");
    updateRecUI();   // the remaining-link count drives the status line and the "Add all" button
    drawRecOverlay();
    applyReducers();
    refreshAfterCompute();
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
    const addAll = $("btn-rec-add-all");
    if (addAll) addAll.disabled = !(state.rec.tableEdges && state.rec.tableEdges.length);
    const status = $("rec-status");
    if (active) {
        const m = state.rec.models[state.rec.active];
        // An exhausted model (every link committed to the graph) would otherwise read as a puzzling "0 links".
        status.textContent = m.edges.length === 0
            ? "Active: " + m.label + " — all recommended links have been added to the network."
            : "Active: " + m.label + " (" + (m.mode === "prediction" ? "prediction" : "recommendation")
                + ", cutoff " + m.cutoff + ", " + m.edges.length + " links).";
    } else {
        status.textContent = "No recommendation applied.";
    }
}

/* --------------------------- recommendation evaluation --------------------------- */
// Scores every algorithm run so far in this session against an uploaded held-out test set: one row per algorithm,
// one column per metric. The metric catalog (accuracy + novelty/diversity) comes from the backend, and the actual
// evaluation runs there through RELISON's RecommMetricGridSelector.

state.recEval = { catalog: [], test: null, result: null, subview: "links" };

// The recommendation tab's centre has two views: the recommended-link table and the evaluation results. The left
// column carries the controls for whichever one is showing, so the evaluation setup only appears on its own subtab.
function switchRecSubtab(sub) {
    state.recEval.subview = sub;
    document.querySelectorAll(".recsubtab").forEach((b) => b.classList.toggle("active", b.dataset.recsubtab === sub));
    $("rec-view-links").classList.toggle("active", sub === "links");
    $("rec-view-eval").classList.toggle("active", sub === "eval");
    toggleHidden("rec-eval-config", sub !== "eval");
}

async function loadRecEvalCatalog() {
    try {
        state.recEval.catalog = await api("/api/recommendation/eval-catalog");
        renderRecEvalMetrics();
    } catch (e) {
        setStatus("Could not load the evaluation metric catalog: " + e.message, "error");
    }
}

// Checkbox list of metrics, grouped by family; accuracy metrics start selected.
function renderRecEvalMetrics() {
    const box = $("rec-eval-metrics");
    if (!box) return;
    box.innerHTML = "";
    let group = null;
    for (const m of state.recEval.catalog) {
        if (m.group !== group) {
            group = m.group;
            const h = document.createElement("div");
            h.className = "checklist-group";
            h.textContent = group;
            box.appendChild(h);
        }
        const label = document.createElement("label");
        label.className = "inline-check";
        const cb = document.createElement("input");
        cb.type = "checkbox";
        cb.dataset.metric = m.id;
        cb.dataset.needsFeatures = m.needsFeatures ? "1" : "";
        cb.checked = m.group === "Accuracy";
        label.appendChild(cb);
        label.appendChild(document.createTextNode(" " + m.label));
        if (m.needsFeatures) setTooltip(label, "js-metric-needs-feature");
        box.appendChild(label);
    }
}

// Spells out what the user-population choice actually changes, since it is not symmetric across metric families.
function updateRecEvalPopulationHint() {
    const el = $("rec-eval-population-hint");
    if (!el) return;
    el.textContent = $("rec-eval-population").value === "all"
        ? "Novelty/diversity cover every ranked user. Precision and nDCG are unchanged, but Recall and MAP divide by an "
          + "empty relevant set for users without test links, so they are undefined and shown as “—”."
        : "All metrics share the same population. Recall and MAP are well-defined here.";
}

// The node attributes that can act as user features for ILD / Unexpectedness.
function populateRecEvalFeature() {
    const defs = nodeAttrDefs().map((d) => d.name);
    setSelectOptions("rec-eval-feature", ["", ...defs], ["— none —", ...defs]);
}

async function uploadRecTestSet() {
    if (!requireGraph()) return;
    const input = $("rec-test-file");
    if (!input.files || !input.files.length) { setStatus("Choose a test-set file first.", "error"); return; }
    const form = new FormData();
    form.append("file", input.files[0]);
    form.append("separator", selectedDelimiter("rec-test-separator"));
    form.append("header", $("rec-test-header").checked);
    setStatus("Uploading test set…", "busy");
    try {
        const res = await api("/api/graph/" + state.graphId + "/recommendation/test", { method: "POST", body: form });
        state.recEval.test = res;
        $("rec-test-status").textContent = res.edges + " test link(s) over " + res.nodes
            + " user(s); " + res.sharedUsers + " also present in the network.";
        setStatus("Test set loaded.");
    } catch (e) {
        setStatus(e.message, "error");
    }
}

async function runRecEvaluation() {
    if (jobBusy("btn-rec-evaluate")) return cancelJob("btn-rec-evaluate");
    if (!requireGraph()) return;
    if (!state.recEval.test) { setStatus("Upload a test set first.", "error"); return; }
    const cutoff = Math.max(1, parseInt($("rec-eval-cutoff").value, 10) || 10);
    const feature = $("rec-eval-feature").value || null;
    const metrics = [];
    let needsFeature = false;
    $("rec-eval-metrics").querySelectorAll("input[type=checkbox]").forEach((cb) => {
        if (!cb.checked) return;
        metrics.push({ id: cb.dataset.metric, params: { cutoff } });
        if (cb.dataset.needsFeatures) needsFeature = true;
    });
    if (!metrics.length) { setStatus("Select at least one evaluation metric.", "error"); return; }
    if (needsFeature && !feature) {
        if (!window.confirm("ILD / Unexpectedness need a feature attribute; without one they are degenerate. Evaluate anyway?")) return;
    }

    setStatus("Evaluating…", "busy");
    $("rec-eval-status").textContent = "Evaluating…";
    const signal = beginJob("btn-rec-evaluate", "evaluation");
    try {
        const res = await api("/api/recommendation/evaluate", { ...jsonBody(
            { graphId: state.graphId, metrics, feature, population: $("rec-eval-population").value,
              reciprocal: $("rec-eval-reciprocal").checked }), signal });
        if (res.cancelled) { $("rec-eval-status").textContent = "Stopped."; setStatus("Stopped."); return; }
        state.recEval.result = res;
        renderRecEvalTable(res);
        $("rec-eval-status").textContent = "";
        setStatus("Evaluated " + res.rows.length + " algorithm(s) over " + res.metrics.length + " metric(s).");
    } catch (e) {
        if (isAbort(e)) { $("rec-eval-status").textContent = "Stopped."; return; }
        $("rec-eval-status").textContent = e.message;
        setStatus(e.message, "error");
    } finally {
        endJob("btn-rec-evaluate");
    }
}

// Rows = executed algorithms, columns = metrics (named "<metric>@<cutoff>" by RELISON).
function renderRecEvalTable(res) {
    const table = $("rec-eval-table");
    table.innerHTML = "";
    toggleHidden("rec-eval-results", false);
    toggleHidden("rec-eval-empty", true);

    const thead = document.createElement("thead");
    const hr = document.createElement("tr");
    for (const h of ["Algorithm", "Run cutoff", "Users", ...res.metrics]) {
        const th = document.createElement("th");
        th.textContent = h;
        hr.appendChild(th);
    }
    thead.appendChild(hr);
    table.appendChild(thead);

    const tbody = document.createElement("tbody");
    for (const row of res.rows) {
        const tr = document.createElement("tr");
        const name = document.createElement("td");
        name.textContent = row.algorithm;
        tr.appendChild(name);
        const cut = document.createElement("td");
        cut.className = "num";
        cut.textContent = row.cutoff;
        tr.appendChild(cut);
        // Evaluated users covered by this algorithm's ranking; well below the total flags poor coverage (typically a
        // link-prediction run, whose global top-n only reaches a handful of users).
        const cov = document.createElement("td");
        cov.className = "num";
        cov.textContent = row.users;
        if (res.evaluatedUsers && row.users < res.evaluatedUsers) setTooltip(cov, "js-evaluated-users", { count: res.evaluatedUsers });
        tr.appendChild(cov);
        for (const m of res.metrics) {
            const td = document.createElement("td");
            td.className = "num";
            const v = row.values[m];
            td.textContent = v == null ? "—" : fmt(Number(v));
            tr.appendChild(td);
        }
        tbody.appendChild(tr);
    }
    table.appendChild(tbody);

    // A ranking is only stored up to the cutoff it was run with, so evaluating deeper than that silently truncates.
    const evalCutoff = Math.max(1, parseInt($("rec-eval-cutoff").value, 10) || 10);
    const shallow = res.rows.filter((r) => r.cutoff < evalCutoff).map((r) => r.algorithm);
    let summary = res.evaluatedLinks + " test link(s) over " + res.evaluatedUsers + " user(s) after filtering."
        + "  Population: " + (res.population === "all" ? "all recommended users" : "users with test links") + ".";
    if (res.undefined && res.undefined.length) {
        summary += "  ⚠ Undefined for this population (division by an empty relevant set): " + res.undefined.join(", ") + ".";
    }
    if (shallow.length) {
        summary += "  ⚠ Evaluated at " + evalCutoff + ", but " + shallow.length
            + " algorithm(s) were run with a smaller cutoff, so their rankings are truncated: " + shallow.join(", ") + ".";
    }
    if (res.skipped && res.skipped.length) summary += "  Skipped: " + res.skipped.join(", ") + ".";
    $("rec-eval-summary").textContent = summary;
}

function exportRecEvalCsv() {
    const res = state.recEval.result;
    if (!res) { setStatus("Run an evaluation first.", "error"); return; }
    const lines = [["algorithm", "cutoff", "users", ...res.metrics].map(csvCell).join(",")];
    for (const row of res.rows) {
        lines.push([row.algorithm, row.cutoff, row.users, ...res.metrics.map((m) => (row.values[m] == null ? "" : row.values[m]))]
            .map(csvCell).join(","));
    }
    download("recommendation-evaluation.csv", lines.join("\n"), "text/csv");
}

function resetRecEval() {
    state.recEval.test = null;
    state.recEval.result = null;
    const t = $("rec-test-status"); if (t) t.textContent = "No test set loaded.";
    const s = $("rec-eval-status"); if (s) s.textContent = "";
    toggleHidden("rec-eval-results", true);
    toggleHidden("rec-eval-empty", false);
    switchRecSubtab("links");   // a fresh network starts on the link table
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
    const tableFocus = tableFilterFocus(g);
    const path = state.pathFocus;
    const focus = path ? null : selectionFocus();
    // Timeline presence: nodes absent at the current timestamp are hidden in the overlay too, so recommended links
    // between them disappear along with the base graph.
    const tl = state.timeline;
    let presentNodes = null;
    if (tl.nodeAttr && tl.t != null && tl.min != null) {
        presentNodes = new Set();
        g.forEachNode((n) => { if (timelineMatches(nodeAttrVal(n, tl.nodeAttr))) presentNodes.add(n); });
    }
    return {
        presentNodes,
        tableFocus,
        pathEdgeColor: state.pathEdgeHighlight.mode === "single" ? state.pathEdgeHighlight.color : null,
        pathEdgeColors: buildPathEdgeColors(path),
        path,
        focus,
        dim: !!(focus && !focus.only),
        isolate: !!(focus && focus.only),
        dimColor: cssVar("--border", "#3a3c41"),
    };
}

// "hidden" | "dim" | "normal" for a node, matching the nodeReducer in applyReducers.
function overlayNodeVerdict(node, c) {
    if (c.presentNodes && !c.presentNodes.has(node)) return "hidden";
    if (c.path?.only && !c.path.nodes.has(node)) return "hidden";
    if (c.isolate && !c.focus.nodes.has(node)) return "hidden";
    if (c.tableFocus && !c.tableFocus.nodes.has(node)) return "dim";
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
    if (c.presentNodes && (!c.presentNodes.has(s) || !c.presentNodes.has(t))) return "hidden";
    if (c.path?.only && !c.path.edges.has(pairKey(s, t)) && !c.path.edges.has(pairKey(t, s))) return "hidden";
    if (c.isolate && (!c.focus.nodes.has(s) || !c.focus.nodes.has(t))) return "hidden";
    if (c.tableFocus && (c.tableFocus.edgeFiltered || !c.tableFocus.nodes.has(s) || !c.tableFocus.nodes.has(t))) return "dim";
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
    if (usingGeographic()) { window.relisonGeographic.refresh(); return; }
    // Sigma uses a canvas overlay for transient recommendation results. In
    // Cosmograph those links are part of its projected data, so refresh that
    // projection instead of trying to align Sigma's overlay canvas.
    if (usingCosmograph()) {
        const changed = syncCosmographRecommendation();
        if (changed) queueCosmographAppearanceRefresh();
        return;
    }
    const canvas = $("rec-overlay");
    if (!canvas) return;
    const r = state.renderer, g = state.graph;
    if (!r || !g || state.activeTab !== "network") return;
    const model = state.rec.active && state.rec.models[state.rec.active];
    const showRec = model && state.rec.show;   // rec links are not part of the sigma graph, drawn here when enabled
    const showBorder = state.nodeBorder.on;
    const path = state.pathFocus;
    if (!showRec && !showBorder && !path) return;

    const ctx = canvas.getContext("2d");
    try {
        const c = focusContext();
        const cam = r.getCamera();
        const ratio = (cam && (cam.ratio || (cam.getState && cam.getState().ratio))) || 1;
        const pos = (id) => r.graphToViewport({ x: g.getNodeAttribute(id, "x"), y: g.getNodeAttribute(id, "y") });
        const scaleSize = (size) => r.scaleSize ? r.scaleSize(size) : size / Math.sqrt(ratio);
        const nodeRadius = (node) => {
            const displayedSize = r.getNodeDisplayData(node)?.size;
            const storedDiameter = g.getNodeAttribute(node, "size");
            return Math.max(0, scaleSize(Number.isFinite(displayedSize) ? displayedSize : (Number.isFinite(storedDiameter) ? storedDiameter / 2 : 3)));
        };

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
                const color = dimmed ? c.dimColor : (c.pathEdgeColors.get(pairKey(e.source, e.target)) || (c.path && c.pathEdgeColor) || state.rec.color);
                ctx.strokeStyle = color;
                ctx.lineWidth = 1.5 * (c.path?.edges.has(pairKey(e.source, e.target)) ? state.pathAppearance.edgeScale : 1);
                ctx.beginPath();
                ctx.moveTo(ps.x, ps.y);
                if ($("edge-shape").value === "curved") {
                    const dx = pt.x - ps.x, dy = pt.y - ps.y, length = Math.hypot(dx, dy) || 1;
                    ctx.quadraticCurveTo((ps.x + pt.x) / 2 - dy / length * length * 0.25,
                        (ps.y + pt.y) / 2 + dx / length * length * 0.25, pt.x, pt.y);
                } else ctx.lineTo(pt.x, pt.y);
                ctx.stroke();
                if (arrows && !dimmed) { ctx.fillStyle = color; drawRecArrow(ctx, ps, pt, $("edge-shape").value === "curved"); }
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
                if (verdict === "dim") color = c.dimColor;
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
                ctx.strokeStyle = verdict === "dim" ? c.dimColor : state.nodeBorder.color;
                ctx.beginPath();
                ctx.arc(p.x, p.y, radius, 0, 2 * Math.PI);
                ctx.stroke();
            });
        }
        // Draw the active paths after the background node copies and borders,
        // so unrelated nodes cannot cover their links at crossings.
        if (path) {
            ctx.setLineDash([]);
            const curved = $("edge-shape").value === "curved";
            g.forEachEdge((edge) => {
                const source = g.source(edge), target = g.target(edge);
                if (!path.edges.has(pairKey(source, target))) return;
                const data = r.getEdgeDisplayData(edge);
                if (!data || data.hidden || overlayNodeVerdict(source, c) === "hidden" || overlayNodeVerdict(target, c) === "hidden") return;
                const from = pos(source), to = pos(target);
                ctx.strokeStyle = data.color;
                ctx.lineWidth = scaleSize(data.size);
                ctx.beginPath();
                ctx.moveTo(from.x, from.y);
                if (curved) ctx.quadraticCurveTo((from.x + to.x) / 2 - (to.y - from.y) * 0.25,
                    (from.y + to.y) / 2 + (to.x - from.x) * 0.25, to.x, to.y);
                else ctx.lineTo(to.x, to.y);
                ctx.stroke();
                if (!g.isUndirected(edge)) { ctx.fillStyle = data.color; drawRecArrow(ctx, from, to, curved); }
            });
            path.nodes.forEach((node) => {
                if (!g.hasNode(node) || overlayNodeVerdict(node, c) === "hidden") return;
                const data = r.getNodeDisplayData(node);
                if (!data || data.hidden) return;
                const p = pos(node);
                ctx.fillStyle = data.color;
                ctx.beginPath();
                ctx.arc(p.x, p.y, nodeRadius(node), 0, 2 * Math.PI);
                ctx.fill();
                if (showBorder) { ctx.strokeStyle = state.nodeBorder.color; ctx.lineWidth = state.nodeBorder.width; ctx.stroke(); }
            });
        }
    } catch (err) {
        console.error("Display overlay failed to draw", err);
    }
}

// Small filled arrowhead near the target endpoint, to convey direction on the dashed overlay.
function drawRecArrow(ctx, from, to, curved = false) {
    let dx = to.x - from.x, dy = to.y - from.y;
    if (curved) { const x = dx; dx = x * 0.5 + dy * 0.25; dy = dy * 0.5 - x * 0.25; }
    const ang = Math.atan2(dy, dx);
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
    if (state.diffusion.rendererPreference === "network" && state.diffusion.rendererType !== mainDiffRendererType())
        setDiffRenderer("network");
    if (state.diffusion.renderer) setTimeout(() => { state.diffusion.renderer.refresh(); drawDiffOverlay(); }, 0);
    if (state.diffusion.rendererType === "cosmograph" && state.diffusion.subview === "graph") restoreDiffCosmograph();
    if (state.diffusion.result && state.diffusion.rendererType !== "cosmograph") setDiffIteration(state.diffusion.iteration);
    if (state.diffusion.subview === "metrics") renderDiffMetrics();
    if (state.diffusion.subview === "pieces") renderActivePiecesMode();
}

// Builds a sigma renderer over a copy of the loaded graph (sharing its node positions).
function initDiffGraph() {
    if (!state.graph || !Sigma) return;
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
    state.diffusion.renderer = new Sigma(g, $("diff-sigma-container"), {
        defaultEdgeType: state.directed ? "arrow" : "line",
        renderLabels: o.nodeShow,
        renderEdgeLabels: o.edgeShow,
        defaultDrawNodeLabel: drawDiffNodeLabel,
        defaultDrawEdgeLabel: drawDiffEdgeLabel,
        labelDensity: 0.5,
        labelRenderedSizeThreshold: 8,
        allowInvalidContainer: true,
    });
    state.diffusion.renderer.setSetting("nodeReducer", (_node, data) => ({
        ...data,
        size: (Number.isFinite(data.size) ? data.size : 0) / 2,
    }));
    state.diffusion.renderer.on("clickNode", ({ node }) => selectDiffNode(node));
    state.diffusion.renderer.on("afterRender", drawDiffOverlay);
    // Track the hovered node so the overlay can redraw it (and its label) on top of the diffusion edges.
    state.diffusion.renderer.on("enterNode", ({ node }) => { state.diffusion.hoverNode = node; drawDiffOverlay(); });
    state.diffusion.renderer.on("leaveNode", () => { state.diffusion.hoverNode = null; drawDiffOverlay(); });
    syncDiffAppearance();
    buildDiffLegend();
    setDiffRenderer(state.diffusion.rendererPreference);
}

// Hidden app tabs can give the WebGL canvas a zero-sized viewport. Restore its
// dimensions only after the diffusion pane and graph subtab have become visible.
function restoreDiffCosmograph() {
    requestAnimationFrame(() => requestAnimationFrame(() => {
        if (state.activeTab !== "diffusion" || state.diffusion.subview !== "graph" || state.diffusion.rendererType !== "cosmograph") return;
        if (!state.diffusion.cosmograph) {
            initDiffCosmograph();
            return;
        }
        state.diffusion.cosmograph.resize();
    }));
}

async function initDiffCosmograph() {
    if (state.diffusion.rendererType !== "cosmograph" || !state.diffusion.graph || !window.relisonDiffusionCosmograph) return;
    try {
        if (state.diffusion.cosmograph) state.diffusion.cosmograph.destroy();
        state.diffusion.cosmograph = null;
        const graph = state.diffusion.graph;
        const recModel = state.diffusion.recKey && state.rec.models[state.diffusion.recKey];
        window.relisonDiffusionRecModel = recModel || null;
        window.relisonDiffusionRecColor = state.rec.color;
        window.relisonDiffusionRecDashed = state.rec.diff;
        window.relisonDiffusionDirected = state.directed;
        const cosmograph = await window.relisonDiffusionCosmograph.create(
            graph,
            $("diff-cosmograph-container"),
            (node) => selectDiffNode(node),
        );
        // The user may have switched renderers or loaded another graph while the
        // asynchronous Cosmograph upload was in flight. Never publish a stale instance.
        if (state.diffusion.rendererType !== "cosmograph" || state.diffusion.graph !== graph) {
            cosmograph.destroy();
            return;
        }
        state.diffusion.cosmograph = cosmograph;
        await cosmograph.setIteration(state.diffusion.iteration || 0, state.diffusion.result, DIFF_COL);
    } catch (error) {
        console.error("Could not initialize diffusion Cosmograph.", error);
        setStatus("Cosmograph could not render the diffusion graph: " + error.message, "error");
        $("diff-renderer").value = "sigma";
        setDiffRenderer("sigma");
    }
}

function mainDiffRendererType() {
    if (usingGeographic()) return geographicActiveProjection;
    return $("network-renderer").value === "cosmograph" ? "cosmograph" : "sigma";
}
function setDiffRenderer(type) {
    state.diffusion.rendererPreference = type;
    $("diff-renderer").value = type;
    if (type === "network") type = mainDiffRendererType();
    const view = $("diff-view-graph");
    const geographic = type === "mercator" || type === "equal-earth";
    state.diffusion.geographic?.destroy(); state.diffusion.geographic = null;
    state.diffusion.rendererType = geographic ? type : type === "cosmograph" ? "cosmograph" : "sigma";
    view.classList.toggle("geographic-active", geographic);
    $("diff-geographic-export").hidden = !geographic;
    $("diff-geographic-container").hidden = !geographic;
    view.classList.toggle("cosmograph-active", state.diffusion.rendererType === "cosmograph");
    $("diff-cosmograph-container").hidden = state.diffusion.rendererType !== "cosmograph";
    if (geographic) {
        initDiffGeographic(type);
    } else if (state.diffusion.rendererType === "cosmograph") {
        if (!state.diffusion.cosmograph) initDiffCosmograph();
        else state.diffusion.cosmograph.resize();
        if (state.diffusion.result) state.diffusion.cosmograph?.setIteration(state.diffusion.iteration, state.diffusion.result, DIFF_COL);
    } else {
        if (state.diffusion.renderer) state.diffusion.renderer.refresh();
        drawDiffOverlay();
    }
}
function usingDiffGeographic() { return ["mercator", "equal-earth"].includes(state.diffusion.rendererType); }
function geographicDiffusionEdges() {
    const g = state.diffusion.graph, result = state.diffusion.result;
    const model = state.diffusion.recKey && state.rec.models[state.diffusion.recKey];
    const edges = geographicRecommendationEdges(model, Boolean(model));
    const frame = result?.iterations[state.diffusion.iteration];
    if (g && frame) for (const source of frame.propagating) {
        if (!g.hasNode(source)) continue;
        const add = target => {
            if (edges.length < 3000) edges.push({ source, target, color: DIFF_COL.prop, dashed: true,
                label: "Propagating: " + source + " → " + target });
        };
        if (state.directed) g.forEachOutNeighbor(source, add); else g.forEachNeighbor(source, add);
    }
    return edges;
}
async function initDiffGeographic(projection) {
    if (!state.diffusion.graph) initDiffGraph();
    const graph = state.diffusion.graph;
    if (!graph) return;
    const map = window.createRelisonGeographic("diff-geographic-container");
    state.diffusion.geographic = map;
    // Freeze the coordinate selection for this view, as in the network map.
    const latitude = $("layout-geographic-latitude").value, longitude = $("layout-geographic-longitude").value;
    try {
        await map.render(graph, { projection,
            isCurrent: () => state.diffusion.geographic === map && state.diffusion.graph === graph && usingDiffGeographic(),
            coordinates: () => geographicCoordinates(graph, latitude, longitude, projection),
            nodeDisplay: (node, data) => state.diffusion.renderer?.getSetting("nodeReducer")?.(node, { ...data }) || data,
            edgeDisplay: (_edge, data) => data,
            selectedNode: () => state.diffusion.selectedNode, labels: () => state.labelOpts,
            overlays: geographicDiffusionEdges,
            legend: () => ({ type: "categorical", title: "Diffusion state", entries: [
                { value: "Not informed", color: DIFF_COL.base }, { value: "Informed", color: DIFF_COL.informed },
                { value: "Newly informed", color: DIFF_COL.newly }, { value: "Propagating", color: DIFF_COL.prop },
            ] }),
            onNodeClick: selectDiffNode, onEdgeClick: () => {}, onClearSelection: () => {},
            onError: message => setStatus(message, "error"),
        });
        if (state.diffusion.geographic !== map || state.diffusion.graph !== graph || !usingDiffGeographic()) {
            map.destroy(); return;
        }
        if (state.diffusion.result) setDiffIteration(state.diffusion.iteration);
    } catch (error) {
        map.destroy();
        if (state.diffusion.geographic === map) {
            setStatus("Diffusion map failed: " + error.message + " Choose latitude/longitude attributes in Network → Advanced layout settings.", "error");
            $("diff-renderer").value = "sigma"; setDiffRenderer("sigma");
        }
    }
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
    state.diffusion.cosmograph?.syncAppearance(g);
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
    // A dashed line swatch for the recommended-edge underlay, shown only when the run used a recommendation.
    const recModel = state.diffusion.recKey && state.rec.models[state.diffusion.recKey];
    if (recModel) {
        const row = document.createElement("div");
        row.className = "legend-row";
        const sw = document.createElement("span");
        sw.className = "legend-swatch legend-line";
        sw.style.background = "transparent";
        sw.style.borderTop = "2px " + (state.rec.diff ? "dashed " : "solid ") + state.rec.color;
        const lab = document.createElement("span");
        lab.textContent = "Recommended link (" + recModel.label + ")";
        row.appendChild(sw);
        row.appendChild(lab);
        el.appendChild(row);
    }
    el.hidden = false;
}

function selectDiffNode(node) {
    state.diffusion.selectedNode = node;
    state.diffusion.geographic?.refresh();
    $("diff-node-input").value = node;
    $("diff-traj-node").value = node;
    $("diff-state-hint").textContent = "Node " + node;
    fetchDiffState();
    if (statsActive("node")) renderNodeTimeline();
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

// A compact "k=v, k=v" rendering of a collected params object, skipping empty/NaN values. Used to tag a run's plot
// label with the protocol's configuration so overlaid runs of the same preset protocol are distinguishable.
function formatParams(params) {
    const parts = [];
    for (const [k, v] of Object.entries(params || {})) {
        if (v === undefined || v === null || v === "" || (typeof v === "number" && Number.isNaN(v))) continue;
        parts.push(k + "=" + (typeof v === "number" ? fmt(v) : v));
    }
    return parts.join(", ");
}

// The next free "Custom X" tag (A, B, … Z, A1, …), used to auto-label custom-protocol runs so overlaid ones stay
// distinguishable without spelling out the whole mechanism configuration in the legend.
function nextCustomTag() {
    const used = new Set(state.diffusion.runs.map((r) => r.label));
    for (let i = 0; ; i++) {
        const tag = "Custom " + String.fromCharCode(65 + (i % 26)) + (i >= 26 ? Math.floor(i / 26) : "");
        if (!used.has(tag)) return tag;
    }
}

// A human-readable, multi-line description of a run's full configuration — shown on demand as the runs-list tooltip,
// so the plot legend can stay short (a "Custom X" tag or a preset name) while the detail is a hover away.
function buildRunDetail(type, protocol, stop, withRec) {
    const mechLabel = (family, id) =>
        (state.defs[family] && state.defs[family][id] && state.defs[family][id].label) || id;
    const line = (name, family, m) => {
        const ps = formatParams(m.params);
        return name + ": " + mechLabel(family, m.id) + (ps ? " [" + ps + "]" : "");
    };
    const lines = [];
    if (type === "custom") {
        lines.push(line("Selection", "selection", protocol.selection));
        lines.push(line("Expiration", "expiration", protocol.expiration));
        lines.push(line("Propagation", "propagation", protocol.propagation));
        lines.push(line("Update", "update", protocol.update));
        lines.push(line("Sight", "sight", protocol.sight));
    } else {
        const ps = formatParams(protocol.params);
        lines.push("Protocol: " + mechLabel("protocol", protocol.id) + (ps ? " [" + ps + "]" : ""));
    }
    const sps = formatParams(stop.params);
    lines.push("Stop: " + mechLabel("stop", stop.id) + (sps ? " [" + sps + "]" : ""));
    if (withRec) lines.push("Recommendation: " + recLabel(state.rec.active));
    return lines.join("\n");
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
    if (jobBusy("btn-diff-run")) return cancelJob("btn-diff-run");
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
    // Optionally run over the graph augmented with the selected recommendation; when so, tag the run's plot label
    // with the recommendation model name: "Protocol (Model)".
    const withRec = !!(state.rec.active && $("diff-rec") && $("diff-rec").checked);
    const recSuffix = withRec ? " (" + recLabel(state.rec.active) + ")" : "";
    // Legend/CSV label for this run. A user-typed label wins; otherwise it is generated: preset protocols get
    // "Name [params]" (so runs of the same protocol with different settings are distinguishable), custom protocols a
    // short "Custom X" tag. The full configuration is always kept in runDetail and shown on demand in the runs list.
    const userLabel = ($("diff-run-label").value || "").trim();
    let baseLabel;
    if (userLabel) {
        baseLabel = userLabel;
    } else if (type === "custom") {
        baseLabel = nextCustomTag() + recSuffix;
    } else {
        baseLabel = protocolLabel(type);
        const ps = formatParams(protocol.params);
        if (ps) baseLabel += " [" + ps + "]";
        baseLabel += recSuffix;
    }
    const runDetail = buildRunDetail(type, protocol, stop, withRec);

    setStatus("Running diffusion…", "busy");
    $("diff-status").textContent = "Running…";
    const signal = beginJob("btn-diff-run", "diffusion");
    try {
        await persistPiecesNow();   // ensure the session has the latest pieces; the run reads them from there
        const body = { graphId: state.graphId, protocol, stop, metrics, filters: [] };
        if (withRec) body.withRecommendation = true;   // run over the recommendation-augmented graph
        if (simPieces) body.pieces = simPieces;   // filtered subset overrides the persisted (full) set for this run
        const res = await api("/api/diffusion/run", { ...jsonBody(body), signal });
        if (res.cancelled) { $("diff-status").textContent = "Stopped."; setStatus("Stopped."); return; }
        state.diffusion.result = res;
        state.diffusion.recKey = withRec ? state.rec.active : null;   // remember which recommendation to overlay
        buildDiffLegend();
        state.diffusion.iteration = 0;
        // Accumulate this run's metric series (tagged with a unique protocol label) for overlaid plotting.
        if (res.metrics && res.metrics.length) {
            state.diffusion.runs.push({ label: uniqueRunLabel(baseLabel), detail: runDetail, numIterations: res.numIterations, metrics: res.metrics });
        }
        $("diff-empty-hint").style.display = "none";
        if (state.diffusion.rendererType === "cosmograph") await initDiffCosmograph();
        toggleHidden("diff-timebar", res.numIterations <= 0);
        const slider = $("diff-slider");
        slider.min = 0; slider.max = Math.max(0, res.numIterations - 1); slider.value = 0;
        setDiffIteration(0);
        populateDiffMetricSelect();
        renderDiffMetrics();
        state.diffusion.traj = null;   // previous run's timelines are stale
        state.diffusion.ptraj = null;
        state.diffusion.ftraj = null;
        state.diffusion.dist = null;
        if (statsActive("node")) renderNodeTimeline();
        if (statsActive("piece")) renderPieceTimeline();
        if (statsActive("feat")) renderFeatureTimeline();
        if (statsActive("dist")) renderDiffDistribution();
        $("diff-status").textContent = "Done — " + res.numIterations + " iteration(s).";
        if (res.skippedMetrics && res.skippedMetrics.length) {
            const msg = "Skipped " + res.skippedMetrics.length + " metric(s) — out of memory: " + res.skippedMetrics.join(", ");
            $("diff-status").textContent += "  " + msg;
            setStatus(msg, "error");
        } else {
            setStatus("Diffusion finished (" + res.numIterations + " iterations).");
        }
    } catch (e) {
        if (isAbort(e)) { $("diff-status").textContent = "Stopped."; return; }
        $("diff-status").textContent = e.message;
        setStatus(e.message, "error");
    } finally {
        endJob("btn-diff-run");
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
    // Move the timeline playhead (no refetch — the whole series is already loaded).
    if (statsActive("node") && state.diffusion.traj) drawTrajectoryChart();
    if (statsActive("piece") && state.diffusion.ptraj) drawPieceTimeline();
    if (statsActive("feat") && state.diffusion.ftraj) drawFeatureTimeline();
    return state.diffusion.cosmograph?.setIteration(i, res, DIFF_COL);
}

// Draws, for the current iteration, dashed spread edges from each propagating user to its neighbours.
function drawDiffOverlay() {
    if (usingDiffGeographic()) { state.diffusion.geographic?.refresh(); return; }
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
    if (!r || !g || state.activeTab !== "diffusion" || state.diffusion.rendererType === "cosmograph") return;

    try {
        const pos = (n) => r.graphToViewport({ x: g.getNodeAttribute(n, "x"), y: g.getNodeAttribute(n, "y") });

        // When the simulation was run over a recommendation, draw its edges as a static underlay (they are part of the
        // graph the diffusion spread over, but not of the sigma graph) so the recommended links are visible.
        const recModel = state.diffusion.recKey && state.rec.models[state.diffusion.recKey];
        if (recModel) {
            // Same styling as the network tab's recommendation overlay (drawRecOverlay): all edges, dashed only when the
            // "differentiate recommended links" style is on, arrowheads only when directed and not too cluttered.
            ctx.strokeStyle = state.rec.color; ctx.lineWidth = 1.5; ctx.setLineDash(state.rec.diff ? [6, 4] : []);
            const edges = recModel.edges;
            const arrows = state.directed && edges.length <= 1500;
            for (let i = 0; i < edges.length; i++) {
                const e = edges[i];
                if (!g.hasNode(e.source) || !g.hasNode(e.target)) continue;
                const ps = pos(e.source), pt = pos(e.target);
                ctx.beginPath(); ctx.moveTo(ps.x, ps.y); ctx.lineTo(pt.x, pt.y); ctx.stroke();
                if (arrows) { ctx.fillStyle = state.rec.color; drawRecArrow(ctx, ps, pt); }
            }
            ctx.setLineDash([]);
        }

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
                const radius = Math.max(1.5, ((g.getNodeAttribute(n, "size") || 6) / 2) / Math.sqrt(ratio));
                ctx.beginPath(); ctx.arc(p.x, p.y, radius, 0, 2 * Math.PI); ctx.stroke();
            });
        }

        // Redraw the hovered node (and its label) on top of the diffusion edges, so hovering stays legible.
        const hover = state.diffusion.hoverNode;
        if (hover && g.hasNode(hover) && g.getNodeAttribute(hover, "x") != null) {
            const cam = r.getCamera();
            const ratio = (cam && (cam.ratio || (cam.getState && cam.getState().ratio))) || 1;
            const p = pos(hover);
            const radius = Math.max(2, ((g.getNodeAttribute(hover, "size") || 6) / 2) / Math.sqrt(ratio));
            ctx.setLineDash([]);
            ctx.fillStyle = g.getNodeAttribute(hover, "color") || "#4f9dff";
            ctx.beginPath(); ctx.arc(p.x, p.y, radius, 0, 2 * Math.PI); ctx.fill();
            ctx.lineWidth = 1.5;
            ctx.strokeStyle = state.nodeBorder.on ? state.nodeBorder.color : cssVar("--text", "#e6e6e6");
            ctx.beginPath(); ctx.arc(p.x, p.y, radius, 0, 2 * Math.PI); ctx.stroke();

            const label = String(g.getNodeAttribute(hover, "label") || hover);
            ctx.font = (state.labelOpts.nodeSize || 12) + "px " + (state.labelOpts.nodeFont || "sans-serif");
            ctx.textAlign = "start"; ctx.textBaseline = "middle";
            const tw = ctx.measureText(label).width;
            const lx = p.x + radius + 4, ly = p.y;
            ctx.fillStyle = cssVar("--panel", "#26272b"); ctx.globalAlpha = 0.9;
            ctx.fillRect(lx - 3, ly - 9, tw + 6, 18);
            ctx.globalAlpha = 1;
            ctx.fillStyle = cssVar("--text", "#e6e6e6");
            ctx.fillText(label, lx, ly);
            ctx.textBaseline = "alphabetic";
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

// Deterministic colour for a feature value, so the same value keeps the same colour across the piece chips,
// the distribution bars and the node timeline.
function featureValueColor(value) {
    let h = 0; const s = String(value);
    for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0;
    return SERIES_COLORS[Math.abs(h) % SERIES_COLORS.length];
}

// Index the (client-held) information pieces by id, for joining the state-endpoint id lists with their features.
function pieceIndexById() {
    const m = new Map();
    (state.diffusion.pieces || []).forEach((p) => m.set(String(p.id), p));
    return m;
}

// [{value, weight}] for one info-piece feature parameter on a given piece id (empty if unknown / synthetic).
function pieceFeatureValues(id, param, byId) {
    const p = byId.get(String(id));
    if (!p || !p.features) return [];
    return p.features
        .filter((f) => f.param === param && f.value !== "" && f.value != null)
        .map((f) => ({ value: String(f.value), weight: Number(f.weight) || 1 }));
}

// Aggregates a feature over a list of piece ids: [[value, weightSum], …] sorted by weight desc.
function categoryDistribution(ids, param, byId) {
    const agg = new Map();
    ids.forEach((id) => pieceFeatureVals(id, param, byId).forEach((fv) =>
        agg.set(fv.value, (agg.get(fv.value) || 0) + fv.weight)));
    return [...agg.entries()].sort((a, b) => b[1] - a[1]);
}

// A horizontal proportion bar for a feature distribution (top-8 values + an "other" segment).
function distBar(dist) {
    const total = dist.reduce((s, e) => s + e[1], 0) || 1;
    const K = 8;
    let items = dist;
    if (dist.length > K) {
        const otherW = dist.slice(K).reduce((s, e) => s + e[1], 0);
        items = dist.slice(0, K).concat([["other", otherW]]);
    }
    const bar = document.createElement("div");
    bar.className = "dist-bar";
    items.forEach(([value, w]) => {
        const seg = document.createElement("span");
        seg.className = "dist-seg";
        seg.style.flexBasis = (100 * w / total) + "%";
        seg.style.background = value === "other" ? cssVar("--muted", "#888") : featureValueColor(value);
        setTooltip(seg, "js-distribution-segment", { value, weight: fmt(w), percent: Math.round(100 * w / total) });
        bar.appendChild(seg);
    });
    return bar;
}

// The selected node's own user features (its community per partition + its node attributes), shown as chips.
function nodeFeatureHeader(node) {
    const wrap = document.createElement("div");
    wrap.className = "node-feats";
    const chips = [];
    for (const algo of Object.keys(state.communityData || {})) {
        const c = state.communityData[algo][node];
        if (c !== undefined && c !== null) chips.push(["comm:" + algo, c]);
    }
    for (const d of nodeAttrDefs()) {
        const v = nodeAttrVal(node, d.name);
        if (v !== undefined && v !== null && v !== "") chips.push([d.name, v]);
    }
    if (!chips.length) { wrap.hidden = true; return wrap; }
    const t = document.createElement("span");
    t.className = "sublabel";
    t.textContent = "User features";
    wrap.appendChild(t);
    const row = document.createElement("div");
    row.className = "piece-chips";
    chips.forEach(([k, v]) => {
        const c = document.createElement("span");
        c.className = "feat-chip";
        c.textContent = k + "=" + v;
        c.style.borderColor = featureValueColor(k + "=" + v);
        row.appendChild(c);
    });
    wrap.appendChild(row);
    return wrap;
}

// Keeps the right-panel feature selector in sync with the available features: info-piece features and user features
// (node attributes + communities, applied to each piece's creator). Value form: "info:<name>" | "user:<name>".
function syncFeatureViewSelect() {
    const sel = $("diff-feature-view");
    const { info, user } = knownFeatureParams();
    if (sel) {
        const sig = "i:" + info.join("") + "|u:" + user.join("");
        if (sel.dataset.sig !== sig) {
            const prev = sel.value;
            sel.innerHTML = "";
            info.forEach((p) => sel.appendChild(option("info:" + p, p)));
            user.forEach((p) => sel.appendChild(option("user:" + p, p + " (user)")));
            sel.dataset.sig = sig;
            if ([...sel.options].some((o) => o.value === prev)) sel.value = prev;
        }
    }
    toggleHidden("diff-feature-view-field", info.length + user.length === 0);
    return sel && sel.value ? sel.value : null;
}

// The user-feature value of a node: a community-partition id or a node-attribute value.
function userFeatureValue(node, name) {
    const comm = state.communityData && state.communityData[name];
    if (comm && comm[node] !== undefined && comm[node] !== null) return comm[node];
    return nodeAttrVal(node, name);
}

// [{value, weight}] for a tagged feature ("info:<name>" | "user:<name>") on a piece id. A user feature resolves to
// the piece creator's attribute / community value (weight 1); an info feature reads the piece's own feature values.
function pieceFeatureVals(id, tagged, byId) {
    if (!tagged) return [];
    const name = tagged.slice(tagged.indexOf(":") + 1);
    if (tagged.startsWith("user:")) {
        const p = byId.get(String(id));
        const creator = p ? p.creator : null;
        if (creator == null) return [];
        const v = userFeatureValue(creator, name);
        return (v === undefined || v === null || v === "") ? [] : [{ value: String(v), weight: 1 }];
    }
    return pieceFeatureValues(id, name, byId);
}

function renderDiffState(res) {
    state.diffusion.lastState = res;
    const host = $("diff-cats");
    if (!host) return;
    const param = syncFeatureViewSelect();
    const byId = pieceIndexById();

    host.innerHTML = "";
    host.appendChild(nodeFeatureHeader(res.node));
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
        cols.appendChild(diffCatColumn("This iteration", cat.iter, param, byId));
        cols.appendChild(diffCatColumn("Overall", cat.overall, param, byId));
        block.appendChild(cols);
        if (param) {
            const dist = categoryDistribution(cat.overall, param, byId);
            if (dist.length) block.appendChild(distBar(dist));
        }
        host.appendChild(block);
    }
}

function diffCatColumn(title, items, param, byId) {
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
            d.className = "row piece-row";
            const idSpan = document.createElement("span");
            idSpan.className = "piece-id";
            idSpan.textContent = it;
            d.appendChild(idSpan);
            const fvs = param ? pieceFeatureVals(it, param, byId) : [];
            if (fvs.length) {
                const chips = document.createElement("span");
                chips.className = "piece-chips";
                fvs.forEach((fv) => {
                    const c = document.createElement("span");
                    c.className = "feat-chip";
                    c.textContent = fv.value + (fv.weight !== 1 ? ":" + fmt(fv.weight) : "");
                    c.style.borderColor = featureValueColor(fv.value);
                    chips.appendChild(c);
                });
                d.appendChild(chips);
            }
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
    if (state.diffusion.cosmograph) { state.diffusion.cosmograph.destroy(); state.diffusion.cosmograph = null; }
    state.diffusion.rendererType = "sigma";
    state.diffusion.rendererPreference = "network";
    const rendererSelect = $("diff-renderer"); if (rendererSelect) rendererSelect.value = "network";
    state.diffusion.geographic?.destroy(); state.diffusion.geographic = null;
    const graphView = $("diff-view-graph"); if (graphView) graphView.classList.remove("cosmograph-active", "geographic-active");
    $("diff-geographic-export").hidden = true;
    const cosmographContainer = $("diff-cosmograph-container"); if (cosmographContainer) cosmographContainer.hidden = true;
    if (state.diffusion.renderer) { try { state.diffusion.renderer.kill(); } catch (e) { /* ignore */ } state.diffusion.renderer = null; }
    state.diffusion.graph = null;
    state.diffusion.result = null;
    state.diffusion.recKey = null;
    state.diffusion.runs = [];
    state.diffusion.iteration = 0;
    state.diffusion.selectedNode = null;
    state.diffusion.hoverNode = null;
    toggleHidden("diff-timebar", true);
    toggleHidden("diff-legend", true);
    const eh = $("diff-empty-hint"); if (eh) eh.style.display = "";
    const cats = $("diff-cats"); if (cats) cats.innerHTML = "";
    const ds = $("diff-status"); if (ds) ds.textContent = "No simulation run yet.";
    const ch = $("diffmetrics-charts"); if (ch) ch.innerHTML = "";
    state.diffusion.lastState = null;
    state.diffusion.traj = null;
    state.diffusion.ptraj = null;
    state.diffusion.ftraj = null;
    state.diffusion.dist = null;
    if (statsActive("node")) renderNodeTimeline();
    if (statsActive("piece")) renderPieceTimeline();
    if (statsActive("feat")) renderFeatureTimeline();
    if (statsActive("dist")) renderDiffDistribution();
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
    state.diffusion.recKey = null;
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
    state.diffusion.lastState = null;
    state.diffusion.traj = null;
    state.diffusion.ptraj = null;
    state.diffusion.ftraj = null;
    state.diffusion.dist = null;
    if (statsActive("node")) renderNodeTimeline();
    if (statsActive("piece")) renderPieceTimeline();
    if (statsActive("feat")) renderFeatureTimeline();
    if (statsActive("dist")) renderDiffDistribution();
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
    if (has) { btn.removeAttribute("title"); btn.removeAttribute("aria-description"); }
    else setTooltip(btn, "js-add-information-piece-to-run");
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

// A clickable, sortable column header (used by the pieces and real-propagated tables). The label lives in a
// ".sort-label" span so a per-column sort arrow can be updated in place without disturbing sibling controls
// (e.g. a feature column's remove button). `onSort` is fired on click.
function makeSortTh(colKey, labelText, onSort) {
    const th = document.createElement("th");
    th.className = "sort-th";
    th.dataset.col = colKey;
    const span = document.createElement("span");
    span.className = "sort-label";
    span.dataset.label = labelText;
    span.textContent = labelText;
    th.appendChild(span);
    th.addEventListener("click", onSort);
    return th;
}

// Refreshes the ▲/▼ arrow on each sortable header of a table to reflect the given sort state.
function updateSortIndicatorsFor(tableId, sort) {
    const table = $(tableId);
    if (!table) return;
    table.querySelectorAll("thead .sort-th").forEach((th) => {
        const span = th.querySelector(".sort-label");
        if (!span) return;
        span.textContent = span.dataset.label + (sort.col === th.dataset.col ? (sort.dir === 1 ? " ▲" : " ▼") : "");
    });
}

// Toggles/sets a table's sort (flip direction if the same column, else ascending on the new column) and re-renders.
function applySort(sort, colKey, resetPage, render) {
    if (sort.col === colKey) sort.dir = -sort.dir;
    else { sort.col = colKey; sort.dir = 1; }
    resetPage();
    render();
}

function ensurePiecesHeader() {
    const table = $("diff-pieces-table");
    const d = state.diffusion;
    const sig = pieceColumns().join("|");
    if (d.piecesHeaderSig === sig && table.querySelector("thead")) { updateSortIndicatorsFor("diff-pieces-table", d.pieceSort); return; }
    d.piecesHeaderSig = sig;
    const old = table.querySelector("thead");
    if (old) old.remove();

    const sortPiecesBy = (col) => applySort(d.pieceSort, col, () => { d.piecesPage = 0; }, renderPiecesTable);
    const thead = document.createElement("thead");
    const htr = document.createElement("tr");
    [["id", "Id"], ["creator", "Creator (node)"], ["timestamp", "Timestamp"]]
        .forEach(([col, label]) => htr.appendChild(makeSortTh(col, label, () => sortPiecesBy(col))));
    d.featureParams.forEach((param) => htr.appendChild(featureColumnHeader(param, sortPiecesBy)));
    htr.appendChild(document.createElement("th"));   // delete-button column
    thead.appendChild(htr);

    const ftr = document.createElement("tr");
    ftr.className = "filter-row";
    pieceColumns().forEach((col) => ftr.appendChild(pieceFilterTh(col)));
    ftr.appendChild(document.createElement("th"));   // delete-button column has no filter
    thead.appendChild(ftr);

    table.insertBefore(thead, table.firstChild);
    markColumnsDirty(table);
    updateSortIndicatorsFor("diff-pieces-table", state.diffusion.pieceSort);
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

    setupResizableColumns(table);
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

// Indices (into state.diffusion.pieces) of the pieces that pass the active filters, in the current sort order.
function filteredPieceIndices() {
    const filters = activePieceFilters();
    const out = [];
    state.diffusion.pieces.forEach((p, i) => { if (!filters.length || pieceRowPasses(p, filters)) out.push(i); });
    const s = state.diffusion.pieceSort;
    if (s && s.col) {
        const P = state.diffusion.pieces;
        out.sort((ia, ib) => compareValues(pieceSortValue(P[ia], s.col), pieceSortValue(P[ib], s.col)) * s.dir);
    }
    return out;
}

// The comparable value of a piece for a given column: timestamps numerically, id/creator numeric-aware
// (like the node table), feature columns by their encoded "value:weight; …" text.
function pieceSortValue(p, col) {
    if (col === "timestamp") return p.timestamp;
    if (col === "id" || col === "creator") return idValue(pieceColValue(p, col));
    return pieceColValue(p, col);
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
    setTooltip(del, "js-delete-piece");
    del.addEventListener("click", () => {
        if (!confirmPiecesChange()) return;
        const removed = JSON.parse(JSON.stringify(state.diffusion.pieces[i]));
        applyPieceRemove(i);
        historyPush("remove piece " + (removed.id || ""), () => applyPieceInsert(removed, i), () => applyPieceRemove(i));
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

// A feature-column header: the parameter name (clickable to sort by that column) plus a control to drop the column.
function featureColumnHeader(param, sortBy) {
    const col = "feat:" + param;
    const th = document.createElement("th");
    th.className = "feature-col-th sort-th";
    th.dataset.col = col;
    th.addEventListener("click", () => sortBy(col));
    const label = document.createElement("span");
    label.className = "sort-label";
    label.dataset.label = param;
    label.textContent = param;
    const del = document.createElement("button");
    del.className = "feature-col-del";
    del.textContent = "✕";
    setTooltip(del, "js-remove-feature-column", { column: param });
    del.addEventListener("click", (e) => { e.stopPropagation(); removeFeatureColumn(param); });   // don't trigger a sort
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

// Parses one parameter's values ("value:weight; value2; …") into {param, value, weight} entries. This is the form a
// single feature column holds — in the table's cells and in a "feat:<name>" column of an uploaded pieces file.
function parseParamValues(text, param) {
    const out = [];
    (text || "").split(/[;,\n]+/).forEach((tokRaw) => {
        const tok = tokRaw.trim();
        if (!tok) return;
        let value = tok, weight = 1;
        const colon = tok.lastIndexOf(":");
        if (colon >= 0) {
            const w = parseFloat(tok.slice(colon + 1));
            if (!isNaN(w)) { weight = w; value = tok.slice(0, colon).trim(); }
        }
        if (value) out.push({ param, value, weight });
    });
    return out;
}

// Replaces the values of one parameter on a piece with those parsed from a cell ("value:weight; value2; …").
function setPieceParamValues(i, param, text) {
    const p = state.diffusion.pieces[i];
    p.features = (p.features || []).filter((f) => f.param !== param).concat(parseParamValues(text, param));
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
    const piece = { id: "piece" + n, creator: "", timestamp: 0, features: [] };
    const index = d.pieces.length;
    applyPieceInsert(piece, index);
    d.piecesPage = Math.floor(index / d.piecesPageSize);   // jump to the page holding the new row
    renderPiecesTable();
    historyPush("add piece " + piece.id, () => applyPieceRemove(index), () => applyPieceInsert(piece, index));
}

/* --------------------------- seeder-driven piece generation --------------------------- */
// Drives the RELISON piece seeders (backend /api/diffusion/seed) along three orthogonal dimensions: how many pieces
// (count), which users (selection), and when (timestamp).

function toggleGenPanel() {
    const panel = $("diff-gen");
    const show = panel.hasAttribute("hidden");
    toggleHidden("diff-gen", !show);
    if (show) { populateGenSelects(); syncGenParams(); }
}

// Fills the metric dropdown (computed vertex metrics) and the community-partition dropdown from the current state.
function populateGenSelects() {
    const metricSel = $("gen-users-metric");
    const metrics = state.metricOrder || [];
    setSelectOptions("gen-users-metric", metrics, metrics);
    const partSel = $("gen-users-partition");
    const partitions = Object.keys(state.communityData || {});
    setSelectOptions("gen-users-partition", partitions, partitions);
    // Disable the user-selection modes that have no data yet, so the user isn't offered an empty selector.
    setGenOptionEnabled("gen-users-type", "values", metrics.length > 0);
    setGenOptionEnabled("gen-users-type", "community", partitions.length > 0);
}

function setGenOptionEnabled(selectId, value, enabled) {
    const opt = Array.from($(selectId).options).find((o) => o.value === value);
    if (!opt) return;
    opt.disabled = !enabled;
    opt.textContent = opt.textContent.replace(/ \(none.*\)$/, "") + (enabled ? "" : (value === "values" ? " (none computed)" : " (none detected)"));
    if (!enabled && $(selectId).value === value) $(selectId).value = "all";
}

// Shows only the parameter group matching each of the three type selects.
function syncGenParams() {
    const map = {
        "gen-count-type": { fixed: "gen-count-fixed", uniform: "gen-count-uniform", poisson: "gen-count-poisson" },
        "gen-users-type": { all: null, random: "gen-users-random", community: "gen-users-community", values: "gen-users-values" },
        "gen-ts-type": { fixed: "gen-ts-fixed", uniform: "gen-ts-uniform" },
    };
    for (const [sel, groups] of Object.entries(map)) {
        const chosen = $(sel).value;
        for (const [val, id] of Object.entries(groups)) {
            if (id) toggleHidden(id, val !== chosen);
        }
    }
}

// Gathers the generator configuration into the request body the /seed endpoint expects.
function genConfig() {
    const count = { type: $("gen-count-type").value };
    if (count.type === "fixed") count.n = numInput("gen-count-n", 1);
    else if (count.type === "uniform") { count.min = numInput("gen-count-min", 1); count.max = numInput("gen-count-max", 1); }
    else count.lambda = parseFloat($("gen-count-lambda").value) || 0;

    const users = { type: $("gen-users-type").value };
    if (users.type === "random") users.fraction = (numInput("gen-users-fraction", 0)) / 100;
    else if (users.type === "community") { users.partition = $("gen-users-partition").value; users.community = numInput("gen-users-commid", 0); }
    else if (users.type === "values") {
        const metric = $("gen-users-metric").value;
        users.values = state.metricData[metric] || {};
        users.mode = $("gen-users-mode").value;
        users.k = numInput("gen-users-k", 0);
    }

    const timestamp = { type: $("gen-ts-type").value };
    if (timestamp.type === "fixed") timestamp.value = numInput("gen-ts-value", 0);
    else { timestamp.min = numInput("gen-ts-min", 0); timestamp.max = numInput("gen-ts-max", 0); }

    const body = { graphId: state.graphId, count, users, timestamp };
    const seedStr = ($("gen-seed").value || "").trim();
    if (seedStr !== "") body.seed = parseInt(seedStr, 10);
    return body;
}

async function generatePiecesViaSeeder() {
    if (!requireGraph()) return;
    if (!confirmPiecesChange()) return;
    const body = genConfig();
    if (body.users.type === "values" && !Object.keys(body.users.values).length) {
        setStatus("Compute a vertex metric (Metrics tab) before selecting users by metric values.", "error"); return;
    }
    $("gen-hint").textContent = "Generating…";
    try {
        const res = await api("/api/diffusion/seed", jsonBody(body));
        state.diffusion.pieces = (res.pieces || []).map((p) => ({ id: p.id, creator: p.creator, timestamp: p.timestamp, features: [] }));
        historyReset();   // wholesale piece replacement invalidates piece-level undo history
        state.diffusion.featureParams = [];
        state.diffusion.pieceFilters = {};
        state.diffusion.piecesHeaderSig = null;
        state.diffusion.piecesPage = 0;
        renderPiecesTable();
        persistPiecesNow();
        $("gen-hint").textContent = "";
        setStatus("Generated " + state.diffusion.pieces.length + " information piece(s).");
    } catch (e) {
        $("gen-hint").textContent = "";
        setStatus("Generation failed: " + (e && e.message ? e.message : e), "error");
    }
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
        state.diffusion.pieces = parsePiecesText(String(reader.result), selectedDelimiter("diff-piece-separator"), $("diff-piece-header").checked);
        historyReset();   // wholesale piece replacement invalidates piece-level undo history
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

/* --------------------------- real-propagated information --------------------------- */
// "Real propagated" records tell the engine which pieces each user actually repropagated in the real world (not just
// the pieces they own). They power the real-propagation metrics (recall, precision, F1, …). Format: a RELISON-style
// file of "user \t piece \t timestamp" lines (an optional header whose first cell is "user"/"userId" is skipped).

// Switches the inner toggle of the "Information pieces" subtab between the pieces table and the real-propagated table.
function switchPiecesMode(mode) {
    state.diffusion.piecesMode = mode;
    document.querySelectorAll(".diffpiecesmode").forEach((b) => b.classList.toggle("active", b.dataset.piecesmode === mode));
    toggleHidden("diff-pieces-mode-pieces", mode !== "pieces");
    toggleHidden("diff-pieces-mode-realprop", mode !== "realprop");
    renderActivePiecesMode();
}

// Renders whichever inner mode (pieces / real propagated) is currently active.
function renderActivePiecesMode() {
    if (state.diffusion.piecesMode === "realprop") renderRealPropTable();
    else renderPiecesTable();
}

function uploadRealPropagatedFile(file) {
    if (!file) return;
    if (!confirmPiecesChange()) return;
    const reader = new FileReader();
    reader.onload = () => {
        state.diffusion.realPropagated = parseRealPropagatedText(String(reader.result), selectedDelimiter("diff-realprop-separator"), $("diff-realprop-header").checked);
        state.diffusion.rpFilters = {};
        state.diffusion.rpHeaderSig = null;
        state.diffusion.rpPage = 0;
        persistRealPropagatedNow();
        renderRealPropTable();
        setStatus("Loaded " + state.diffusion.realPropagated.length + " real-propagation record(s) from " + file.name + ".");
    };
    reader.onerror = () => setStatus("Could not read " + file.name + ".", "error");
    reader.readAsText(file);
}

// Parses a real-propagated file. Columns: user, piece, timestamp. The optional header is controlled by import settings;
// a missing timestamp defaults to 0.
function parseRealPropagatedText(text, delimiter = "\t", hasHeader = true) {
    const lines = text.split(/\r?\n/).filter((l) => l.trim().length);
    const out = [];
    lines.forEach((line, idx) => {
        const c = splitDelimitedRow(line, delimiter).map((x) => x.trim());
        if (idx === 0 && hasHeader && ["user", "userid"].includes((c[0] || "").toLowerCase())) return;
        if (!c[0] || !c[1]) return;
        out.push({ user: c[0], piece: c[1], timestamp: (c[2] !== undefined && c[2] !== "") ? (parseInt(c[2], 10) || 0) : 0 });
    });
    return out;
}

function persistRealPropagatedNow() {
    if (rpSaveTimer) { clearTimeout(rpSaveTimer); rpSaveTimer = null; }
    if (!state.graphId) return Promise.resolve();
    return api("/api/diffusion/real-propagated", jsonBody({ graphId: state.graphId, realPropagated: state.diffusion.realPropagated }))
        .catch(() => { /* best effort; also revalidated server-side at run time */ });
}

// Debounced save, used while the user is editing cells.
let rpSaveTimer = null;
function scheduleRpPersist() {
    if (rpSaveTimer) clearTimeout(rpSaveTimer);
    rpSaveTimer = setTimeout(persistRealPropagatedNow, 600);
}

function clearRealPropagated() {
    if (!state.diffusion.realPropagated.length) return;
    if (!confirmPiecesChange()) return;
    state.diffusion.realPropagated = [];
    state.diffusion.rpFilters = {};
    state.diffusion.rpHeaderSig = null;
    state.diffusion.rpPage = 0;
    persistRealPropagatedNow();
    renderRealPropTable();
}

function downloadRealPropagatedTsv() {
    const recs = state.diffusion.realPropagated;
    if (!recs.length) { setStatus("No real-propagation records to download.", "error"); return; }
    const lines = [["user", "piece", "timestamp"].join("\t")];
    for (const r of recs) lines.push([r.user, r.piece, r.timestamp].join("\t"));
    download("real-propagated.tsv", lines.join("\n"), "text/tab-separated-values");
}

function updateRealPropSummary() {
    const el = $("diff-realprop-summary");
    if (!el) return;
    const recs = state.diffusion.realPropagated;
    if (!recs.length) {
        el.textContent = "No real-propagation records — optional; needed only for real-propagation metrics.";
        return;
    }
    const users = new Set(recs.map((r) => r.user));
    const pieces = new Set(recs.map((r) => r.piece));
    el.textContent = recs.length + " record(s): " + users.size + " user(s) repropagating " + pieces.size + " distinct piece(s).";
}

/* --------------------------- real-propagated editable table --------------------------- */
// A full editable table mirroring the pieces table: three fixed columns (user, piece, timestamp), per-column filters,
// pagination and inline editing. The `user` cell is a node combo; the `piece` cell autocompletes over the defined
// piece ids. Records referencing an unknown user/piece are kept here but ignored at run time (matching the backend).

function rpColumns() { return ["user", "piece", "timestamp"]; }

function addRealPropRecord() {
    if (!confirmPiecesChange()) return;
    const d = state.diffusion;
    d.realPropagated.push({ user: "", piece: "", timestamp: 0 });
    d.rpPage = Math.floor((d.realPropagated.length - 1) / d.rpPageSize);   // jump to the page holding the new row
    renderRealPropTable();
    scheduleRpPersist();
}

function renderRealPropTable() {
    const table = $("diff-realprop-table");
    if (!table) return;
    refreshPieceIdDatalist();
    ensureRealPropHeader();
    renderRealPropBody();
}

function ensureRealPropHeader() {
    const table = $("diff-realprop-table");
    const d = state.diffusion;
    const sig = rpColumns().join("|");
    if (d.rpHeaderSig === sig && table.querySelector("thead")) { updateSortIndicatorsFor("diff-realprop-table", d.rpSort); return; }
    d.rpHeaderSig = sig;
    const old = table.querySelector("thead");
    if (old) old.remove();

    const sortRpBy = (col) => applySort(d.rpSort, col, () => { d.rpPage = 0; }, renderRealPropTable);
    const thead = document.createElement("thead");
    const htr = document.createElement("tr");
    [["user", "User (node)"], ["piece", "Piece"], ["timestamp", "Timestamp"]]
        .forEach(([col, label]) => htr.appendChild(makeSortTh(col, label, () => sortRpBy(col))));
    htr.appendChild(document.createElement("th"));   // delete-button column
    thead.appendChild(htr);

    const ftr = document.createElement("tr");
    ftr.className = "filter-row";
    rpColumns().forEach((col) => {
        const th = document.createElement("th");
        th.appendChild(makeColFilter(d.rpFilters[col], (op, value, value2) => setRpFilter(col, op, value, value2)));
        ftr.appendChild(th);
    });
    ftr.appendChild(document.createElement("th"));   // delete-button column has no filter
    thead.appendChild(ftr);

    table.insertBefore(thead, table.firstChild);
    markColumnsDirty(table);
    updateSortIndicatorsFor("diff-realprop-table", state.diffusion.rpSort);
}

function renderRealPropBody() {
    const table = $("diff-realprop-table");
    const d = state.diffusion;
    updateRealPropSummary();
    const old = table.querySelector("tbody");
    if (old) old.remove();

    const idxs = filteredRpIndices();
    const total = idxs.length;
    const pages = Math.max(1, Math.ceil(total / d.rpPageSize));
    if (d.rpPage >= pages) d.rpPage = pages - 1;
    if (d.rpPage < 0) d.rpPage = 0;
    const start = d.rpPage * d.rpPageSize;
    const end = Math.min(total, start + d.rpPageSize);

    const tbody = document.createElement("tbody");
    for (let k = start; k < end; k++) { const i = idxs[k]; tbody.appendChild(realPropRow(d.realPropagated[i], i)); }
    table.appendChild(tbody);

    setupResizableColumns(table);
    updateRpPager(total, pages, start, end - start);
}

function rpColValue(r, col) { return r[col]; }

// The comparable value of a record for a column: timestamp numerically, user/piece numeric-aware.
function rpSortValue(r, col) {
    return col === "timestamp" ? r.timestamp : idValue(r[col]);
}

function activeRpFilters() {
    const f = state.diffusion.rpFilters || {};
    const cols = new Set(rpColumns());
    return Object.keys(f)
        .filter((c) => cols.has(c) && isFilterActive(f[c]))
        .map((c) => ({ col: c, op: f[c].op, value: f[c].value, value2: f[c].value2 }));
}

function filteredRpIndices() {
    const filters = activeRpFilters();
    const out = [];
    state.diffusion.realPropagated.forEach((r, i) => {
        if (!filters.length || filters.every((f) => matchFilter(rpColValue(r, f.col), f.op, f.value, f.value2))) out.push(i);
    });
    const s = state.diffusion.rpSort;
    if (s && s.col) {
        const R = state.diffusion.realPropagated;
        out.sort((ia, ib) => compareValues(rpSortValue(R[ia], s.col), rpSortValue(R[ib], s.col)) * s.dir);
    }
    return out;
}

function setRpFilter(col, op, value, value2) {
    const f = { op, value, value2 };
    if (isFilterActive(f)) state.diffusion.rpFilters[col] = f;
    else delete state.diffusion.rpFilters[col];
    state.diffusion.rpPage = 0;
    renderRealPropBody();   // body only, so the filter input keeps focus
}

function clearRpFilters() {
    state.diffusion.rpFilters = {};
    state.diffusion.rpHeaderSig = null;   // force a header rebuild to clear the filter inputs
    state.diffusion.rpPage = 0;
    renderRealPropTable();
}

function updateRpPager(total, pages, start, shown) {
    const container = $("diff-realprop-pager");
    if (!container) return;
    container.innerHTML = "";
    const d = state.diffusion;

    const sizeSel = document.createElement("select");
    [100, 200, 500, 1000].forEach((n) => sizeSel.appendChild(option(String(n), n + " / page")));
    sizeSel.value = String(d.rpPageSize);
    sizeSel.addEventListener("change", () => { d.rpPageSize = parseInt(sizeSel.value, 10); d.rpPage = 0; renderRealPropTable(); });

    const prev = document.createElement("button");
    prev.textContent = "‹ Prev";
    prev.disabled = d.rpPage <= 0;
    prev.addEventListener("click", () => { d.rpPage--; renderRealPropTable(); });

    const next = document.createElement("button");
    next.textContent = "Next ›";
    next.disabled = d.rpPage >= pages - 1;
    next.addEventListener("click", () => { d.rpPage++; renderRealPropTable(); });

    const jump = document.createElement("span");
    jump.className = "page-jump";
    const jumpLabel = document.createElement("span");
    jumpLabel.textContent = "Page";
    const pageInput = document.createElement("input");
    pageInput.type = "number";
    pageInput.min = "1";
    pageInput.max = String(pages);
    pageInput.value = String(d.rpPage + 1);
    pageInput.className = "page-input";
    const goTo = () => {
        let v = parseInt(pageInput.value, 10);
        if (isNaN(v)) v = d.rpPage + 1;
        v = Math.max(1, Math.min(pages, v));
        d.rpPage = v - 1;
        renderRealPropTable();
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

function realPropRow(r, i) {
    const tr = document.createElement("tr");
    tr.appendChild(realPropCell(i, "user", "text", r.user));
    tr.appendChild(realPropCell(i, "piece", "text", r.piece));
    tr.appendChild(realPropCell(i, "timestamp", "number", r.timestamp));
    const td = document.createElement("td");
    const del = document.createElement("button");
    del.className = "piece-del";
    del.textContent = "✕";
    setTooltip(del, "js-delete-record");
    del.addEventListener("click", () => {
        if (!confirmPiecesChange()) return;
        state.diffusion.realPropagated.splice(i, 1);
        renderRealPropTable();
        persistRealPropagatedNow();
    });
    td.appendChild(del);
    tr.appendChild(td);
    return tr;
}

function realPropCell(i, field, type, value) {
    const td = document.createElement("td");
    const inp = document.createElement("input");
    inp.type = type;
    if (type === "number") inp.step = "1";
    inp.value = value == null ? "" : value;
    const commit = () => {
        if (!confirmPiecesChange()) { inp.value = state.diffusion.realPropagated[i][field] ?? ""; return; }   // revert on cancel
        state.diffusion.realPropagated[i][field] = (type === "number") ? (parseInt(inp.value, 10) || 0) : inp.value;
        scheduleRpPersist();
    };
    inp.addEventListener("input", commit);
    if (field === "piece") {
        inp.setAttribute("list", "diff-piece-ids");   // autocomplete over the defined piece ids
    }
    td.appendChild(inp);
    // The combo must be attached only after the input has a parent (it wraps the input in place).
    if (field === "user") {
        attachNodeCombo(inp);            // the user is a node of the graph
        inp.addEventListener("change", commit);
    }
    return td;
}

// Keeps a shared <datalist> of the currently-defined piece ids, used to autocomplete the real-propagated "piece" cells.
function refreshPieceIdDatalist() {
    let dl = $("diff-piece-ids");
    if (!dl) {
        dl = document.createElement("datalist");
        dl.id = "diff-piece-ids";
        document.body.appendChild(dl);
    }
    dl.innerHTML = "";
    for (const p of state.diffusion.pieces) {
        if (!p.id) continue;
        const opt = document.createElement("option");
        opt.value = p.id;
        dl.appendChild(opt);
    }
}

// Uploads a RELISON-style info-features file: lines of "infoId \t featureId" (optional 3rd column = weight). The
// feature parameter name comes from a header row ("infoId \t <name>") when present, otherwise from the name box.
function uploadInfoFeaturesFile(file) {
    if (!file) return;
    if (!state.diffusion.pieces.length) { setStatus("Add or generate information pieces before uploading their features.", "error"); return; }
    if (!confirmPiecesChange()) return;
    const reader = new FileReader();
    reader.onload = () => applyInfoFeaturesText(String(reader.result), file.name, selectedDelimiter("diff-feature-separator"), $("diff-feature-header").checked);
    reader.onerror = () => setStatus("Could not read " + file.name + ".", "error");
    reader.readAsText(file);
}

function applyInfoFeaturesText(text, filename, delimiter = "\t", hasHeader = true) {
    const lines = text.split(/\r?\n/).filter((l) => l.trim().length);
    if (!lines.length) { setStatus("The features file is empty.", "error"); return; }

    const cells = (line) => splitDelimitedRow(line, delimiter).map((c) => c.trim());
    let param = "";
    let start = 0;
    const first = cells(lines[0]);
    if (hasHeader && ["infoid", "id", "info", "piece"].includes((first[0] || "").toLowerCase())) {
        if (first[1]) param = first[1];   // header names the feature parameter
        start = 1;
    }
    // No header to name the parameter, so ask for it (this used to come from a permanent box in the toolbar).
    if (!param) {
        param = (window.prompt("Name of the feature in \"" + filename + "\":", "feature") || "").trim();
        if (!param) return;   // cancelled
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

// Splits one CSV-style row, honoring quoted separators and doubled quote escapes.
function splitDelimitedRow(line, delimiter) {
    if (!delimiter || delimiter.length !== 1) return line.split(delimiter || "\t");
    const fields = [];
    let field = "", quoted = false;
    for (let i = 0; i < line.length; i++) {
        const ch = line[i];
        if (ch === '"') {
            if (quoted && line[i + 1] === '"') { field += '"'; i++; }
            else quoted = !quoted;
        } else if (ch === delimiter && !quoted) { fields.push(field); field = ""; }
        else field += ch;
    }
    fields.push(field);
    if (fields[0] && fields[0].charCodeAt(0) === 0xFEFF) fields[0] = fields[0].slice(1);
    return fields;
}

// Parses an information-pieces file. Columns: id, creator, timestamp, features. The chosen separator and optional
// header are controlled by import settings.
function parsePiecesText(text, delimiter = "\t", hasHeader = true) {
    const lines = text.split(/\r?\n/).filter((l) => l.trim().length);
    if (!lines.length) return [];
    const cells = (line) => splitDelimitedRow(line, delimiter).map((c) => c.trim());

    // A header row (first cell "id") decides how the columns from the 4th on are read. Two layouts are accepted:
    //   · a single packed "features" column ("param=value:weight; …") — what Download writes; and/or
    //   · one column per feature parameter, named "feat:<name>" or just "<name>" — mirroring the table's columns.
    // Without a header the 4th column is the packed one, as before.
    let start = 0, packedCol = 3;
    const featureCols = {};   // column index -> parameter name
    const first = cells(lines[0]);
    if (hasHeader && (first[0] || "").toLowerCase() === "id") {
        start = 1;
        packedCol = null;
        for (let c = 3; c < first.length; c++) {
            const h = (first[c] || "").trim();
            if (!h) continue;
            if (h.toLowerCase() === "features") packedCol = c;
            else featureCols[c] = /^feat:/i.test(h) ? h.slice(5).trim() : h;
        }
        if (packedCol === null && !Object.keys(featureCols).length) packedCol = 3;   // header named no feature columns
    }

    const out = [];
    for (let i = start; i < lines.length; i++) {
        const cols = cells(lines[i]);
        if (!cols[0]) continue;
        let features = packedCol !== null ? parsePieceFeatures(cols[packedCol] || "") : [];
        for (const c of Object.keys(featureCols)) features = features.concat(parseParamValues(cols[Number(c)] || "", featureCols[c]));
        out.push({
            id: cols[0],
            creator: cols[1] || "",
            timestamp: (cols[2] !== undefined && cols[2] !== "") ? (parseInt(cols[2], 10) || 0) : 0,
            features,
        });
    }
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

/* ------------------------------- wiring ----------------------------- */

$("btn-load").addEventListener("click", loadGraph);
$("file-input").addEventListener("change", updateImportFormatHint);
$("opt-format").addEventListener("change", updateImportFormatHint);
[
    "opt-separator", "rec-test-separator", "node-attr-separator", "edge-attr-separator",
    "diff-piece-separator", "diff-feature-separator", "diff-realprop-separator",
].forEach((id) => $(id).addEventListener("change", () => { $(id).dataset.manual = "1"; }));
$("rec-test-file").addEventListener("change", () => setDefaultEdgeSeparator("rec-test-file", "rec-test-separator"));
$("btn-generate").addEventListener("click", generateGraph);
$("gen-type").addEventListener("change", onGenTypeChange);
// Graph timeline controls.
$("tl-node-attr").addEventListener("change", (e) => { state.timeline.nodeAttr = e.target.value; state.timeline.t = state.timeline.rangeStart = null; updateTimelineUI(); });
$("tl-edge-attr").addEventListener("change", (e) => { state.timeline.edgeAttr = e.target.value; state.timeline.t = state.timeline.rangeStart = null; updateTimelineUI(); });
$("tl-mode").addEventListener("change", (e) => setTimelineMode(e.target.value));
$("tl-slider").addEventListener("input", (e) => { stopTimeline(); setTimelineT(parseInt(e.target.value, 10) || 0); });
$("tl-time").addEventListener("change", (e) => { stopTimeline(); setTimelineT(parseInt(e.target.value, 10) || 0); });
$("tl-range-start").addEventListener("change", (e) => { stopTimeline(); setTimelineRangeStart(parseInt(e.target.value, 10) || 0); });
$("tl-play").addEventListener("click", playTimeline);
$("tl-step-back").addEventListener("click", () => stepTimeline(-1));
$("tl-step-fwd").addEventListener("click", () => stepTimeline(1));
$("btn-tl-gif").addEventListener("click", downloadTimelineGif);
$("btn-tl-webm").addEventListener("click", downloadTimelineWebm);
$("btn-layout").addEventListener("click", onLayoutButton);
$("btn-save-layout").addEventListener("click", saveLayoutPositions);
$("layout-pack-components").addEventListener("change", updateLayoutTypeUI);
$("layout-fr-bounded").addEventListener("change", updateLayoutTypeUI);
$("btn-noverlap").addEventListener("click", removeOverlaps);
$("btn-reset-layout").addEventListener("click", resetLayout);
$("layout-type").addEventListener("change", () => { stopLayout(); updateLayoutButton(); updateLayoutTypeUI(); });
$("drag-nodes").addEventListener("change", (e) => {
    state.dragNodes = e.target.checked;
    $("network-drag-hint").hidden = !state.dragNodes;
    $("multi-node-select").disabled = !state.dragNodes;
    $("btn-area-select-nodes").disabled = !state.dragNodes || !state.multiNodeSelect;
    if (!state.dragNodes) {
        $("multi-node-select").checked = false;
        state.multiNodeSelect = false;
        state.multiSelectedNodes.clear();
        setAreaNodeSelection(false);
        syncMultiNodeSelection();
    }
    if (usingCosmograph() && window.relisonCosmograph?.setDragEnabled) {
        window.relisonCosmograph.setDragEnabled(state.dragNodes)
            .catch((error) => setStatus("Could not update Cosmograph node dragging: " + error.message, "error"));
    }
});
$("multi-node-select").addEventListener("change", (event) => {
    state.multiNodeSelect = event.target.checked && state.dragNodes;
    $("btn-area-select-nodes").disabled = !state.multiNodeSelect;
    if (!state.multiNodeSelect) {
        state.multiSelectedNodes.clear();
        setAreaNodeSelection(false);
    }
    syncMultiNodeSelection();
});
$("btn-area-select-nodes").addEventListener("click", () => {
    if (!state.areaSelectingNodes) setAreaNodeSelection(true);
    else setAreaNodeSelection(false);
});
syncLayoutOptionsForRenderer();

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
$("btn-diff-gif").addEventListener("click", downloadDiffusionGif);
$("btn-diff-webm").addEventListener("click", downloadDiffusionWebm);
$("diff-node-input").addEventListener("change", () => {
    const v = $("diff-node-input").value.trim();
    if (v && state.diffusion.graph && state.diffusion.graph.hasNode(v)) selectDiffNode(v);
});
$("diffmetric-select").addEventListener("change", renderDiffMetrics);
$("diff-feature-view").addEventListener("change", () => { if (state.diffusion.lastState) renderDiffState(state.diffusion.lastState); });
$("diff-renderer").addEventListener("change", (event) => setDiffRenderer(event.target.value));
$("btn-diff-map-png").addEventListener("click", () => exportGeographicImage("png", true));
$("btn-diff-map-svg").addEventListener("click", () => exportGeographicImage("svg", true));
// Node timeline subtab controls.
$("diff-traj-node").addEventListener("change", () => {
    const v = $("diff-traj-node").value.trim();
    if (v && state.diffusion.graph && state.diffusion.graph.hasNode(v)) selectDiffNode(v);
});
$("diff-traj-feature").addEventListener("change", () => { updateTrajControls(); fetchTrajectory(); });
["diff-traj-category", "diff-traj-mode", "diff-traj-agg"]
    .forEach((id) => $(id).addEventListener("change", fetchTrajectory));
$("diff-traj-stack").addEventListener("change", drawTrajectoryChart);
$("btn-diff-traj-png").addEventListener("click", () => downloadChartPng($("diff-traj-canvas"), "node-timeline.png"));
// Piece timeline subtab controls.
$("diff-ptraj-piece").addEventListener("change", () => {
    const v = $("diff-ptraj-piece").value.trim();
    state.diffusion.selectedPiece = v || null;
    renderPieceTimeline();
});
$("diff-ptraj-feature").addEventListener("change", () => { syncPtrajFeatureSelect(); fetchPieceTrajectory(); });
["diff-ptraj-category", "diff-ptraj-mode"].forEach((id) => $(id).addEventListener("change", fetchPieceTrajectory));
$("diff-ptraj-stack").addEventListener("change", drawPieceTimeline);
$("btn-diff-ptraj-png").addEventListener("click", () => downloadChartPng($("diff-ptraj-canvas"), "piece-timeline.png"));
// Feature timeline subtab controls.
$("diff-ftraj-feature").addEventListener("change", () => { syncFtrajValueSelect(); fetchFeatureTrajectory(); });
$("diff-ftraj-value").addEventListener("change", () => { updateFtrajControls(); fetchFeatureTrajectory(); });
["diff-ftraj-category", "diff-ftraj-mode", "diff-ftraj-entity"]
    .forEach((id) => $(id).addEventListener("change", fetchFeatureTrajectory));
$("diff-ftraj-stack").addEventListener("change", drawFeatureTimeline);
$("btn-diff-ftraj-png").addEventListener("click", () => downloadChartPng($("diff-ftraj-canvas"), "feature-timeline.png"));
// Distributions subtab controls.
$("diff-dist-type").addEventListener("change", renderDiffDistribution);
["diff-dist-feature", "diff-dist-info", "diff-dist-user"].forEach((id) => $(id).addEventListener("change", fetchDistribution));
$("btn-diff-dist-png").addEventListener("click", () => downloadChartPng($("diff-dist-canvas"), "diffusion-distribution.png"));
// Information-pieces subtab controls.
$("btn-diff-piece-add").addEventListener("click", addDiffPiece);
$("btn-diff-gen-toggle").addEventListener("click", toggleGenPanel);
$("btn-diff-gen").addEventListener("click", generatePiecesViaSeeder);
["gen-count-type", "gen-users-type", "gen-ts-type"].forEach((id) => $(id).addEventListener("change", syncGenParams));
$("btn-diff-piece-clear").addEventListener("click", clearDiffPieces);
$("btn-diff-piece-download").addEventListener("click", downloadPiecesCsv);
// Both uploads live behind one "Upload ▾" menu; the feature name now comes from the file header or a prompt.
setupMenu("btn-diff-upload-menu", "diff-upload-menu", (act) => {
    $(act === "features" ? "diff-feature-file" : "diff-piece-file").click();
});
$("diff-piece-file").addEventListener("change", (e) => { setDefaultEdgeSeparator("diff-piece-file", "diff-piece-separator"); uploadPiecesCsv(e.target.files[0]); e.target.value = ""; });
$("diff-feature-file").addEventListener("change", (e) => { setDefaultEdgeSeparator("diff-feature-file", "diff-feature-separator"); uploadInfoFeaturesFile(e.target.files[0]); e.target.value = ""; });
$("btn-diff-feature-add").addEventListener("click", addFeatureColumn);
document.querySelectorAll(".diffpiecesmode").forEach((b) => b.addEventListener("click", () => switchPiecesMode(b.dataset.piecesmode)));
$("btn-diff-realprop-add").addEventListener("click", addRealPropRecord);
$("btn-diff-realprop-upload").addEventListener("click", () => $("diff-realprop-file").click());
$("diff-realprop-file").addEventListener("change", (e) => { setDefaultEdgeSeparator("diff-realprop-file", "diff-realprop-separator"); uploadRealPropagatedFile(e.target.files[0]); e.target.value = ""; });
$("btn-diff-realprop-download").addEventListener("click", downloadRealPropagatedTsv);
$("btn-diff-realprop-clear-filters").addEventListener("click", clearRpFilters);
$("btn-diff-realprop-clear").addEventListener("click", clearRealPropagated);
updateRealPropSummary();
$("btn-diff-piece-clear-filters").addEventListener("click", clearPieceFilters);
$("diff-apply-filters").addEventListener("change", (e) => { state.diffusion.applyFiltersToSim = e.target.checked; });
updateDiffRunEnabled();   // starts disabled until at least one piece exists
$("size-by").addEventListener("change", () => { syncSizeControlVisibility(); applyAppearance(); });
$("node-color-mode").addEventListener("change", () => { syncNodeColorControls(); applyAppearance(); });
$("node-color-single").addEventListener("input", applyAppearance);
$("color-by").addEventListener("change", () => {
    $("node-color-mode").value = "attribute";
    syncNodeColorControls();
    applyAppearance();
});
$("node-color-low").addEventListener("input", applyAppearance);
$("node-color-high").addEventListener("input", applyAppearance);
$("edge-size-by").addEventListener("change", () => { syncSizeControlVisibility(); applyAppearance(); });
$("edge-color-mode").addEventListener("change", (e) => {
    syncEdgeColorControls();
    applyAppearance();
});
$("edge-color-single").addEventListener("input", applyAppearance);
$("edge-color-by").addEventListener("change", () => { syncEdgeColorControls(); applyAppearance(); });
$("edge-color-low").addEventListener("input", applyAppearance);
$("edge-color-high").addEventListener("input", applyAppearance);
["node-size-uniform", "node-size-min", "node-size-max", "edge-size-uniform", "edge-size-min", "edge-size-max"].forEach((id) => $(id).addEventListener("input", applyAppearance));
["node-size-scale", "edge-size-scale", "node-size-reverse", "edge-size-reverse"].forEach((id) => $(id).addEventListener("change", applyAppearance));

// Sigma draws node borders on its display overlay. Cosmograph maps the same
// option to its native circular point-outline ring (its API has no ring-width
// setting), so refresh its configuration whenever the shared setting changes.
$("node-border-on").addEventListener("change", (e) => {
    state.nodeBorder.on = e.target.checked;
    toggleHidden("node-border-params", !e.target.checked);
    drawRecOverlay();
    queueCosmographAppearanceRefresh();
});
$("node-border-color").addEventListener("input", (e) => {
    state.nodeBorder.color = e.target.value;
    drawRecOverlay();
    queueCosmographAppearanceRefresh();
});
$("node-border-width").addEventListener("input", () => {
    state.nodeBorder.width = numInput("node-border-width", 1.5);
    drawRecOverlay();
});
["node-label-show", "node-label-size", "node-label-prop", "node-label-color", "node-label-font",
 "edge-label-show", "edge-label-size", "edge-label-prop", "edge-label-color", "edge-label-font",
 "node-label-attribute", "edge-label-attribute"]
    .forEach((id) => $(id).addEventListener("input", syncLabelOpts));
$("btn-zoom-in").addEventListener("click", zoomIn);
$("btn-zoom-out").addEventListener("click", zoomOut);
$("btn-zoom-fit").addEventListener("click", zoomFit);
$("network-renderer").addEventListener("change", (e) => setNetworkRenderer(e.target.value));
$("edge-shape").addEventListener("change", applyEdgeShape);
$("network-renderer").addEventListener("change", () => setTimeout(syncCosmographLabelColorControls, 0));
$("btn-vertex").addEventListener("click", runVertexMetric);
$("network-renderer").addEventListener("change", (e) => {
    // Renderer changes can implicitly replace the selected layout and should
    // not leave an animation from the previous renderer running in the background.
    stopLayout();
    if (e.target.value !== "cosmograph" && state.cosmographLayoutRunning) {
        window.relisonCosmograph?.stopForceLayout();
        state.cosmographLayoutRunning = false;
    }
    setTimeout(syncLayoutOptionsForRenderer, 0);
});
$("btn-graph").addEventListener("click", runGraphMetric);
$("btn-pair").addEventListener("click", runPairMetric);
$("btn-community").addEventListener("click", detectCommunity);
$("btn-global-comm").addEventListener("click", runGlobalCommMetric);
$("btn-indiv-comm").addEventListener("click", runIndividualCommMetric);
$("btn-attribute-global-comm").addEventListener("click", runAttributeGlobalCommMetric);
$("btn-attribute-indiv-comm").addEventListener("click", runAttributeIndividualCommMetric);
$("btn-edge-attribute-global").addEventListener("click", () => runEdgeAttributeMetric($("edge-attribute-global-metric").value, true));
$("btn-edge-attribute-indiv").addEventListener("click", () => runEdgeAttributeMetric($("edge-attribute-indiv-metric").value, false));

$("vertex-metric").addEventListener("change", (e) => renderParams("vertex-params", "vertex", e.target.value));
$("graph-metric").addEventListener("change", (e) => renderParams("graph-params", "graph", e.target.value));
$("pair-metric").addEventListener("change", (e) => renderParams("pair-params", "pair", e.target.value));
$("community-algo").addEventListener("change", (e) => renderParams("community-params", "community", e.target.value));
$("indiv-comm-metric").addEventListener("change", (e) => renderParams("indiv-comm-params", "communityIndividual", e.target.value));
$("global-comm-metric").addEventListener("change", (e) => renderParams("global-comm-params", "communityGlobal", e.target.value));
$("attribute-indiv-comm-metric").addEventListener("change", (e) => renderParams("attribute-indiv-comm-params", "communityIndividual", e.target.value));
$("attribute-global-comm-metric").addEventListener("change", (e) => renderParams("attribute-global-comm-params", "communityGlobal", e.target.value));
$("edit-mode").addEventListener("change", (e) => setEditMode(e.target.checked));
$("theme-toggle").addEventListener("click", toggleTheme);
setupMenu("btn-report", "report-menu", (act) => (act === "pdf" ? exportReportPdf() : exportReportHtml()));
//$("focus-toggle").addEventListener("click", toggleFocusMode);
//$("inspector-toggle").addEventListener("click", toggleInspector);
//$("style-selector").addEventListener("change", (e) => applyVisualStyle(e.target.value));
$("style-selector").addEventListener("change", (e) => applyExtendedVisualStyle(e.target.value));
setupSettingsMenu();
(function initChartStyle() {
    let saved = "quiet-scientific";
    try { saved = localStorage.getItem("relison-chart-style") || "quiet-scientific"; } catch (e) { /* ignore */ }
    applyChartStyle(saved);
})();
$("chart-style-selector").addEventListener("change", (e) => applyChartStyle(e.target.value));
(function initChartPalette() {
    let saved;
    try {
        saved = localStorage.getItem("relison-chart-palette");
        if (!saved) {
            const previousStyle = localStorage.getItem("relison-chart-style");
            saved = ({ "vibrant-dashboard": "vibrant", editorial: "editorial", "glass-panel": "glass",
                "monochrome-accent": "monochrome" })[previousStyle] || "balanced";
        }
    } catch (e) { saved = "balanced"; }
    applyChartPalette(saved);
})();
$("chart-palette-selector").addEventListener("change", (e) => applyChartPalette(e.target.value));
setupMenu("btn-session", "session-menu", (act) => (act === "save" ? saveSession() : $("session-file").click()));
$("session-file").addEventListener("change", (e) => {
    const file = e.target.files && e.target.files[0];
    e.target.value = "";   // so re-opening the same file fires again
    openSession(file);
});

$("btn-undo").addEventListener("click", historyUndo);
$("btn-redo").addEventListener("click", historyRedo);
$("btn-network-undo").addEventListener("click", historyUndo);
$("btn-network-redo").addEventListener("click", historyRedo);
// Keyboard: Ctrl/Cmd+Z undo, Ctrl/Cmd+Y or Ctrl/Cmd+Shift+Z redo. Ignored while typing so native field undo still works.
document.addEventListener("keydown", (e) => {
    if (!(e.ctrlKey || e.metaKey) || e.altKey) return;
    const t = e.target;
    if (t && (t.tagName === "INPUT" || t.tagName === "TEXTAREA" || t.tagName === "SELECT" || t.isContentEditable)) return;
    const k = e.key.toLowerCase();
    if (k === "z" && !e.shiftKey) { e.preventDefault(); historyUndo(); }
    else if (k === "y" || (k === "z" && e.shiftKey)) { e.preventDefault(); historyRedo(); }
});

$("select-node-input").addEventListener("change", selectFromInput);
$("select-type").addEventListener("change", (e) => setSelectionType(e.target.value));
$("select-edge-input").addEventListener("change", (e) => { if (e.target.value) selectEdge(e.target.value); else clearEdgeSelection(); });
initNodeCombos();
$("select-mode").addEventListener("change", (e) => setSelectionMode(e.target.value));
$("select-partition").addEventListener("change", (e) => setSelectionPartition(e.target.value));
$("select-edge-attribute").addEventListener("change", (e) => setSelectionEdgeAttribute(e.target.value));
$("btn-clear-selection").addEventListener("click", clearSelection);
$("select-mode").value = state.selection.mode;
updateSelectionTargetUI();

$("btn-select-paths").addEventListener("click", findSelectedPaths);
$("path-enlarge-nodes").addEventListener("change", (event) => {
    state.pathAppearance.enlargeNodes = event.target.checked;
    toggleHidden("path-node-scale-field", !event.target.checked);
    applyReducers();
    drawRecOverlay();
});
for (const [id, key] of [["path-node-scale", "nodeScale"], ["path-edge-scale", "edgeScale"]]) {
    $(id).addEventListener("input", (event) => {
        const value = Number(event.target.value);
        if (!Number.isFinite(value) || value < 0.1) return;
        state.pathAppearance[key] = value;
        applyReducers();
        drawRecOverlay();
    });
}
$("path-edge-color-mode").addEventListener("change", (event) => {
    state.pathEdgeHighlight.mode = event.target.value;
    toggleHidden("path-edge-highlight-color-field", event.target.value !== "single");
    renderPathsTable(state.lastPaths);
    applyReducers();
    drawRecOverlay();
});
$("path-edge-highlight-color").addEventListener("input", (event) => {
    state.pathEdgeHighlight.color = event.target.value;
    applyReducers();
    drawRecOverlay();
});
$("select-path-source").addEventListener("change", () => { state.pathFocus = null; state.pathEndpointFocusActive = true; updateSelectionSummary(); renderPathsTable(state.lastPaths); applyReducers(); });
$("select-path-target").addEventListener("change", () => { state.pathFocus = null; state.pathEndpointFocusActive = true; updateSelectionSummary(); renderPathsTable(state.lastPaths); applyReducers(); });

document.querySelectorAll(".tab").forEach((b) => b.addEventListener("click", () => switchTab(b.dataset.tab)));
document.querySelectorAll(".subtab").forEach((b) => b.addEventListener("click", () => switchSubtab(b.dataset.subtab)));
document.querySelectorAll(".tablesubtab").forEach((b) => b.addEventListener("click", () => switchTableSubtab(b.dataset.tablesubtab)));
document.querySelectorAll(".diffsubtab").forEach((b) => b.addEventListener("click", () => switchDiffSubtab(b.dataset.diffsubtab)));
document.querySelectorAll(".diffstatsview").forEach((b) => b.addEventListener("click", () => switchStatsView(b.dataset.statsview)));

// Structural metric chart download buttons (delegated by data-canvas / data-file).
document.querySelectorAll(".chart-dl[data-canvas]").forEach((b) =>
    b.addEventListener("click", () => downloadChartPng(b.dataset.canvas, b.dataset.file)));
// Diffusion metric chart: the chart container is created dynamically inside #diffmetrics-charts.
$("btn-diffmetric-png").addEventListener("click", () => {
    const chart = ECHARTS.get("diff-metrics-chart");
    const sel = $("diffmetric-select");
    const name = (sel && sel.value ? sel.value : "diffusion-metric").replace(/[^\w.-]+/g, "_");
    downloadChartPng(chart, name + ".png");
});
$("btn-diffmetric-csv").addEventListener("click", downloadDiffMetricsCsv);

$("btn-export-png").addEventListener("click", exportPng);
$("btn-export-svg").addEventListener("click", exportSvg);
$("btn-export-gexf").addEventListener("click", exportGexf);
$("btn-export-nodes").addEventListener("click", exportNodesCsv);
$("btn-export-edges").addEventListener("click", exportEdgesCsv);
$("btn-export-global").addEventListener("click", exportGlobalCsv);
$("btn-export-node-averages").addEventListener("click", () => exportAveragesCsv("node-averages.csv", state.metricOrder, state.metricData));
$("btn-export-edge-averages").addEventListener("click", () => exportAveragesCsv("edge-averages.csv", state.pairOrder, state.pairData));
$("btn-export-pair-averages").addEventListener("click", exportPairAveragesCsv);
$("btn-export-comm-averages").addEventListener("click", () => exportCommAveragesCsv());
$("btn-export-attrcomm-averages").addEventListener("click", exportAttributeCommAveragesCsv);
$("btn-export-edgeattr-averages").addEventListener("click", exportEdgeAttributeAveragesCsv);
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
$("node-csv-file").addEventListener("change", (e) => { setDefaultEdgeSeparator("node-csv-file", "node-attr-separator"); uploadAttributeFile("nodes", e.target.files[0]); e.target.value = ""; });
$("edge-csv-file").addEventListener("change", (e) => { setDefaultEdgeSeparator("edge-csv-file", "edge-attr-separator"); uploadAttributeFile("edges", e.target.files[0]); e.target.value = ""; });

$("btn-clear-nodes-filters").addEventListener("click", () => clearFilters("nodes"));
$("btn-clear-edges-filters").addEventListener("click", () => clearFilters("edges"));

["node-chart-metric", "node-chart-sort"].forEach((id) => $(id).addEventListener("change", drawNodeChart));
$("node-topk-metric").addEventListener("change", renderNodeTopK);
$("node-topk-k").addEventListener("input", renderNodeTopK);
["edge-chart-metric", "edge-chart-sort"].forEach((id) => $(id).addEventListener("change", drawEdgeChart));
$("pair-chart-metric").addEventListener("change", drawPairChart);
["comm-chart-metric", "comm-chart-sort"].forEach((id) => $(id).addEventListener("change", drawCommChart));
["attrcomm-chart-metric", "attrcomm-chart-sort"].forEach((id) => $(id).addEventListener("change", drawAttributeCommChart));
["edgeattr-chart-metric", "edgeattr-chart-sort"].forEach((id) => $(id).addEventListener("change", drawEdgeAttributeChart));

// Recommendation tab + overlay controls.
$("btn-rec-apply").addEventListener("click", applyRecommendation);
$("btn-rec-reset").addEventListener("click", resetRecommendationResults);
$("rec-mode").addEventListener("change", onRecModeChange);
$("rec-algo").addEventListener("change", (e) => renderParams("rec-params", "recommendation", e.target.value));
$("btn-rec-add-all").addEventListener("click", addAllRecLinks);
$("btn-clear-rec-filters").addEventListener("click", () => clearFilters("rec"));

// Recommendation evaluation.
document.querySelectorAll(".recsubtab").forEach((b) => b.addEventListener("click", () => switchRecSubtab(b.dataset.recsubtab)));
$("rec-eval-population").addEventListener("change", updateRecEvalPopulationHint);
updateRecEvalPopulationHint();
$("btn-rec-test-upload").addEventListener("click", uploadRecTestSet);
$("btn-rec-evaluate").addEventListener("click", runRecEvaluation);
$("btn-rec-eval-export").addEventListener("click", exportRecEvalCsv);
loadRecEvalCatalog();
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
    ECHARTS.forEach((chart) => {
        const el = chart.getDom();
        if (!chart.isDisposed() && el && el.getBoundingClientRect().width > 0) chart.resize();
    });
    if (state.activeTab === "diffusion") {
        if (state.diffusion.subview === "metrics") return;
        if (state.diffusion.renderer) state.diffusion.renderer.refresh();
        drawDiffOverlay();
        return;
    }
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

loadTooltipText();
switchTab(state.activeTab); // start on the Import tab with the side panels hidden
loadCatalog();
initDebugConsole();
