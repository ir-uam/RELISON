"use strict";

/* ------------------------------------------------------------------ *
 * Compatibility shim – bridges old UI expectations to the current
 * graphology‑library@0.7.0 and sigma@3.0.3 APIs.
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
    timeline: { nodeAttr: "", edgeAttr: "", t: null, min: null, max: null, events: [], playing: null },
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
    if (!bounds) {
        stopTimeline();
        tl.min = tl.max = tl.t = null;
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
    const slider = $("tl-slider"), num = $("tl-time");
    slider.min = tl.min; slider.max = tl.max; slider.step = 1; slider.value = tl.t;
    num.min = tl.min; num.max = tl.max; num.step = 1; num.value = tl.t;
    updateTimelineReadout();
    applyReducers();
}

function updateTimelineReadout() {
    const tl = state.timeline;
    $("tl-range").textContent = "t = " + tl.t + "   (range " + tl.min + " – " + tl.max + ")";
}

// Sets the current timestamp (clamped to [min, max]) and re-applies the graph visibility filter.
function setTimelineT(t) {
    const tl = state.timeline;
    if (tl.min == null) return;
    tl.t = Math.max(tl.min, Math.min(tl.max, Math.round(t)));
    $("tl-slider").value = tl.t;
    $("tl-time").value = tl.t;
    updateTimelineReadout();
    applyReducers();
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
        renderer: state.diffusion.renderer,
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
    if (!state.diffusion.result || !state.diffusion.renderer) { setStatus("Run a diffusion simulation first.", "error"); return false; }
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
    toggleHidden("import-session-hint", !isSession);
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

    setStatus("Loading network…", "busy");
    $("btn-load").disabled = true;
    try {
        const data = await api("/api/graph/load", { method: "POST", body: form });
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
    state.graphId = data.graphId;
    state.directed = data.stats.directed;
    state.weighted = data.stats.weighted;
    state.stats = data.stats;   // kept for the report export
    state.reportPlots.clear();  // the previous graph's captured plots no longer apply
    state.multigraph = multigraph;
    state.attrSchema = data.schema || { node: [], edge: [] };
    stopTimeline();
    state.timeline = { nodeAttr: "", edgeAttr: "", t: null, min: null, max: null, events: [], playing: null };
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
    // Sigma v3 uses `defaultDrawNodeLabel` and `defaultDrawEdgeLabel` instead of the older
    // `labelRenderer` / `edgeLabelRenderer` options. Update the renderer configuration accordingly.
    state.renderer = new Sigma(graph, $("sigma-container"), {
        defaultEdgeType: state.directed ? "arrow" : "line",
        renderLabels: state.labelOpts.nodeShow,
        renderEdgeLabels: state.labelOpts.edgeShow,
        defaultDrawNodeLabel: drawNodeLabel,
        defaultDrawEdgeLabel: drawEdgeLabel,
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
    sizeSelect.innerHTML = '<option value="">— uniform (default) —</option>';
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
    rebuildTimelineOptions();      // keep the timeline's time-attribute selectors in sync with the schema
}

// Restores a select's value if the option still exists, otherwise falls back to the first (default) option.
function restoreSelect(select, value) {
    const exists = Array.from(select.options).some((o) => o.value === value);
    select.value = exists ? value : "";
}

function applyAppearance() {
    const graph = state.graph;
    if (!graph) return;

    const sizeBy = $("size-by").value;
    const minSize = numInput("node-size-min", 2), maxSize = numInput("node-size-max", 14);
    if (!sizeBy) {
        // Default: every node the same size. Node size is not derived from degree (or any metric) unless the user
        // explicitly picks one in "Size by"; the "Max size" control sets the uniform node size.
        graph.forEachNode((n) => graph.setNodeAttribute(n, "size", maxSize));
    } else {
        const sizeValues = sizeBy.startsWith("attr:") ? numericMap(nodeAttrValues(sizeBy.slice(5))) : (state.metricData[sizeBy] || {});
        let min = Infinity, max = -Infinity;
        for (const n of graph.nodes()) {
            const val = sizeValues[n] ?? 0;
            if (val < min) min = val;
            if (val > max) max = val;
        }
        const span = (max - min) || 1;
        const sizeRange = Math.max(0, maxSize - minSize);
        graph.forEachNode((n) => graph.setNodeAttribute(n, "size", minSize + sizeRange * (((sizeValues[n] ?? 0) - min) / span)));
    }

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
    //if (document.body.classList.contains("focus-mode")) { document.body.classList.add("inspector-open"); $("inspector-toggle").classList.add("active"); $("inspector-toggle").setAttribute("aria-pressed", "true"); }
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
    ["select-node-input", "path-source", "path-target", "add-edge-source", "add-edge-target", "diff-node-input", "diff-traj-node"]
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
        $("color-by").value = "metric:" + res.label;
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
    if (state.selectedNode != null && !state.graph.hasNode(state.selectedNode)) clearSelection();
    refreshAfterCompute();
    applyReducers();
    if (state.renderer) state.renderer.refresh();
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
    const u = $("btn-undo"), r = $("btn-redo"), h = state.history;
    if (u) { u.disabled = h.applying || !h.undo.length; u.title = h.undo.length ? "Undo: " + h.undo[h.undo.length - 1].label + " (Ctrl+Z)" : "Nothing to undo"; }
    if (r) { r.disabled = h.applying || !h.redo.length; r.title = h.redo.length ? "Redo: " + h.redo[h.redo.length - 1].label + " (Ctrl+Y)" : "Nothing to redo"; }
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
        grip.title = "Drag to resize column";
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
    setupResizableColumns(table);
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

    // Timeline: at timestamp t, only nodes/edges whose time attribute covers t are shown (empty value → never shown).
    const tl = state.timeline;
    const tlNode = !!(tl.nodeAttr && tl.t != null && tl.min != null);
    const tlEdge = !!(tl.edgeAttr && tl.t != null && tl.min != null);
    let presentNodes = null;
    if (tlNode) {
        presentNodes = new Set();
        g.forEachNode((n) => { if (timeContains(nodeAttrVal(n, tl.nodeAttr), tl.t)) presentNodes.add(n); });
    }

    const path = state.pathFocus;           // highlighting shortest paths takes priority over selection
    const focus = path ? null : selectionFocus();
    const dim = focus && !focus.only;       // highlight modes: dim non-focus
    const isolate = focus && focus.only;    // "show only" modes: hide non-focus
    const dimColor = cssVar("--border", "#3a3c41");

    const nodeActive = filterNodes || focus || path || tlNode;
    state.renderer.setSetting("nodeReducer", nodeActive ? (node, data) => {
        if (presentNodes && !presentNodes.has(node)) return { ...data, hidden: true };
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

    const edgeActive = filterNodes || hasEdgeFilter || focus || path || tlNode || tlEdge;
    state.renderer.setSetting("edgeReducer", edgeActive ? (edge, data) => {
        const s = g.source(edge), t = g.target(edge);
        if (presentNodes && (!presentNodes.has(s) || !presentNodes.has(t))) return { ...data, hidden: true };
        if (tlEdge && !timeContains(edgeAttrVal(edge, tl.edgeAttr), tl.t)) return { ...data, hidden: true };
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
    capturePlot(canvas, title);
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
    "size-by", "color-by", "node-size-min", "node-size-max", "node-color-low", "node-color-high",
    "edge-size-by", "edge-size-min", "edge-size-max", "edge-color-mode", "edge-color-single",
    "node-border-on", "node-border-color", "node-border-width", "layout-type",
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
        controls,
        metrics: {
            metricData: state.metricData, metricOrder: state.metricOrder,
            pairData: state.pairData, pairOrder: state.pairOrder,
            nodePairAgg: state.nodePairAgg, nodePairOrder: state.nodePairOrder,
            graphMetrics: state.graphMetrics,
            communityData: state.communityData,
            commMetricData: state.commMetricData, commMetricOrder: state.commMetricOrder,
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

// A single chart canvas → PNG data URI, composited over the panel background (so light-on-dark text stays legible
// however the report page itself is themed).
function chartToDataUrl(canvas) {
    if (!canvas || !canvas.width || !canvas.height) return null;
    const out = document.createElement("canvas");
    out.width = canvas.width; out.height = canvas.height;
    const ctx = out.getContext("2d");
    ctx.fillStyle = cssVar("--panel-2", "#1e1f23");
    ctx.fillRect(0, 0, out.width, out.height);
    ctx.drawImage(canvas, 0, 0);
    return out.toDataURL("image/png");
}

// The report section a chart belongs to, inferred from its canvas id.
function plotSection(canvas) {
    const id = (canvas && canvas.id) || "";
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
function capturePlot(canvas, title) {
    if (!canvas || !state.graph || !state.reportPlots) return;
    const url = chartToDataUrl(canvas);
    if (!url) return;
    const key = (canvas.id || "chart") + "::" + (title || "");
    state.reportPlots.set(key, { section: plotSection(canvas), title: title || "chart", url });
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

/* ------------------------------ debug console ----------------------------- */
function applyVisualStyle(style) { const modern = style !== "classic"; document.body.classList.toggle("modern-slate", modern); document.body.classList.toggle("classic", !modern); $("style-selector").value = modern ? "modern-slate" : "classic"; try { localStorage.setItem("relison-visual-style", modern ? "modern-slate" : "classic"); } catch (e) { /* ignore */ } }
//function applyFocusMode(enabled) { document.body.classList.toggle("focus-mode", enabled); if (!enabled) document.body.classList.remove("inspector-open"); $("focus-toggle").classList.toggle("active", enabled); $("focus-toggle").setAttribute("aria-pressed", String(enabled)); $("focus-toggle").textContent = enabled ? "Focus on" : "Focus"; toggleHidden("inspector-toggle", !enabled); if (!enabled) { $("inspector-toggle").classList.remove("active"); $("inspector-toggle").setAttribute("aria-pressed", "false"); } try { localStorage.setItem("relison-focus-mode", enabled ? "on" : "off"); } catch (e) { /* ignore */ } setTimeout(() => { if (state.renderer) { state.renderer.refresh(); drawRecOverlay(); } if (state.diffusion.renderer) { state.diffusion.renderer.refresh(); drawDiffOverlay(); } }, 0); }
function applyExtendedVisualStyle(style) { const valid = ["classic", "modern-slate", "quiet-light", "graph-first-dark"]; const next = valid.includes(style) ? style : "modern-slate"; document.body.classList.remove(...valid); document.body.classList.add(next); $("style-selector").value = next; try { localStorage.setItem("relison-visual-style", next); } catch (e) { /* ignore */ } }
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
        if (m.needsFeatures) label.title = "Needs a feature attribute to be meaningful.";
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
        if (res.evaluatedUsers && row.users < res.evaluatedUsers) cov.title = "of " + res.evaluatedUsers + " evaluated users";
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
    const nodeFilters = activeFilters("nodes", nodeColumns());
    let filterNodes = null;
    if (nodeFilters.length) {
        filterNodes = new Set();
        g.forEachNode((n) => { if (rowPasses(nodeRowValue, n, nodeFilters)) filterNodes.add(n); });
    }
    const path = state.pathFocus;
    const focus = path ? null : selectionFocus();
    // Timeline presence: nodes absent at the current timestamp are hidden in the overlay too, so recommended links
    // between them disappear along with the base graph.
    const tl = state.timeline;
    let presentNodes = null;
    if (tl.nodeAttr && tl.t != null && tl.min != null) {
        presentNodes = new Set();
        g.forEachNode((n) => { if (timeContains(nodeAttrVal(n, tl.nodeAttr), tl.t)) presentNodes.add(n); });
    }
    return {
        presentNodes,
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
    if (c.presentNodes && !c.presentNodes.has(node)) return "hidden";
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
    if (c.presentNodes && (!c.presentNodes.has(s) || !c.presentNodes.has(t))) return "hidden";
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
    capturePlot(canvas, title);
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
    state.diffusion.renderer.on("clickNode", ({ node }) => selectDiffNode(node));
    state.diffusion.renderer.on("afterRender", drawDiffOverlay);
    // Track the hovered node so the overlay can redraw it (and its label) on top of the diffusion edges.
    state.diffusion.renderer.on("enterNode", ({ node }) => { state.diffusion.hoverNode = node; drawDiffOverlay(); });
    state.diffusion.renderer.on("leaveNode", () => { state.diffusion.hoverNode = null; drawDiffOverlay(); });
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
                const radius = Math.max(1.5, (g.getNodeAttribute(n, "size") || 3) / Math.sqrt(ratio));
                ctx.beginPath(); ctx.arc(p.x, p.y, radius, 0, 2 * Math.PI); ctx.stroke();
            });
        }

        // Redraw the hovered node (and its label) on top of the diffusion edges, so hovering stays legible.
        const hover = state.diffusion.hoverNode;
        if (hover && g.hasNode(hover) && g.getNodeAttribute(hover, "x") != null) {
            const cam = r.getCamera();
            const ratio = (cam && (cam.ratio || (cam.getState && cam.getState().ratio))) || 1;
            const p = pos(hover);
            const radius = Math.max(2, (g.getNodeAttribute(hover, "size") || 3) / Math.sqrt(ratio));
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
        seg.title = value + " — " + fmt(w) + " (" + Math.round(100 * w / total) + "%)";
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
    del.title = "Delete piece";
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
    del.title = 'Remove the "' + param + '" feature column';
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
        state.diffusion.pieces = parsePiecesText(String(reader.result));
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
        state.diffusion.realPropagated = parseRealPropagatedText(String(reader.result));
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

// Parses a real-propagated file. Columns: user, piece, timestamp. Tab-separated (falls back to any whitespace when a
// line has no tab). A leading header row (first cell "user"/"userid") is skipped; a missing timestamp defaults to 0.
function parseRealPropagatedText(text) {
    const lines = text.split(/\r?\n/).filter((l) => l.trim().length);
    const out = [];
    lines.forEach((line, idx) => {
        const c = (line.includes("\t") ? line.split("\t") : line.split(/\s+/)).map((x) => x.trim());
        if (idx === 0 && ["user", "userid"].includes((c[0] || "").toLowerCase())) return;   // header
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
    del.title = "Delete record";
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
    reader.onload = () => applyInfoFeaturesText(String(reader.result), file.name);
    reader.onerror = () => setStatus("Could not read " + file.name + ".", "error");
    reader.readAsText(file);
}

function applyInfoFeaturesText(text, filename) {
    const lines = text.split(/\r?\n/).filter((l) => l.trim().length);
    if (!lines.length) { setStatus("The features file is empty.", "error"); return; }

    // Split on tab (fall back to any whitespace if a line has no tab).
    const cells = (line) => (line.includes("\t") ? line.split("\t") : line.split(/\s+/)).map((c) => c.trim());
    let param = "";
    let start = 0;
    const first = cells(lines[0]);
    if (["infoid", "id", "info", "piece"].includes((first[0] || "").toLowerCase())) {
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

// Parses an information-pieces file. Columns: id, creator, timestamp, features. Tab-separated (so the features
// field can freely use ",", ";", "=", ":"); a comma-only line (no tab) is still accepted for legacy 3-column files.
// A leading header row (first cell "id") is skipped.
function parsePiecesText(text) {
    const lines = text.split(/\r?\n/).filter((l) => l.trim().length);
    if (!lines.length) return [];
    const cells = (line) => (line.includes("\t") ? line.split("\t") : line.split(",")).map((c) => c.trim());

    // A header row (first cell "id") decides how the columns from the 4th on are read. Two layouts are accepted:
    //   · a single packed "features" column ("param=value:weight; …") — what Download writes; and/or
    //   · one column per feature parameter, named "feat:<name>" or just "<name>" — mirroring the table's columns.
    // Without a header the 4th column is the packed one, as before.
    let start = 0, packedCol = 3;
    const featureCols = {};   // column index -> parameter name
    const first = cells(lines[0]);
    if ((first[0] || "").toLowerCase() === "id") {
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
        if (m) series.push({ name: run.label, values: m.values, color: SERIES_COLORS[idx % SERIES_COLORS.length] });
    });
    if (!series.length) return;

    const title = document.createElement("h2");
    title.textContent = chosen.label;
    const area = document.createElement("div");
    area.className = "chart-area";
    area.style.height = "320px";
    const canvas = document.createElement("canvas");
    canvas.id = "diff-metrics-chart";   // stable id so the report groups it under Diffusion (keyed by the metric label)
    area.appendChild(canvas);
    container.appendChild(title);
    container.appendChild(area);
    drawMultiLineChart(canvas, series, chosen.label, false);   // legend omitted — see the runs list below
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
        sw.style.background = SERIES_COLORS[idx % SERIES_COLORS.length];
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
function drawMultiLineChart(canvas, series, label, showLegend = true) {
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

    // Legend (top-right): one swatch + protocol label per series. Skipped for the diffusion metric plot, where the
    // runs list below the chart already provides the labels (and their full configuration on hover).
    if (showLegend) {
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
    capturePlot(canvas, label);
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
    const canvas = $("diff-traj-canvas");
    if (!canvas || !canvas.getContext) return;
    const ctx = canvas.getContext("2d");
    if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
}

// Shared renderer for the Node and Piece timeline charts: turns a {values, series, iterations} response into coloured
// series and draws them (stacked or lines) with the slider playhead.
function drawTrajectoryInto(canvasId, res, stacked, label) {
    const canvas = $(canvasId);
    if (!canvas || !res) return;
    const values = res.values || [];
    if (!values.length) { const c = canvas.getContext("2d"); if (c) c.clearRect(0, 0, canvas.width, canvas.height); return; }
    const series = values.map((v) => ({
        name: v,
        values: (res.series && res.series[v]) || [],
        color: v === "other" ? cssVar("--muted", "#888") : featureValueColor(v),
    }));
    if (stacked) drawStackedAreaChart(canvas, series, label);
    else drawMultiLineChart(canvas, series, label);
    drawTrajPlayhead(canvas, res.iterations);
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
function drawTrajPlayhead(canvas, n) {
    if (!n || n <= 1) return;
    const iter = Math.max(0, Math.min(state.diffusion.iteration || 0, n - 1));
    const rect = canvas.getBoundingClientRect();
    const W = Math.max(1, Math.floor(rect.width)), H = Math.max(1, Math.floor(rect.height));
    const ctx = canvas.getContext("2d");   // transform already set to dpr by the chart drawer
    const padL = 66, padR = 16, padT = 16, padB = 50;
    const plotW = W - padL - padR, plotH = H - padT - padB;
    const x = padL + (iter / (n - 1)) * plotW;
    ctx.save();
    ctx.strokeStyle = cssVar("--text", "#e6e6e6"); ctx.globalAlpha = 0.5;
    ctx.setLineDash([3, 3]); ctx.lineWidth = 1;
    ctx.beginPath(); ctx.moveTo(x, padT); ctx.lineTo(x, padT + plotH); ctx.stroke();
    ctx.restore();
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
    const canvas = $(id);
    if (!canvas || !canvas.getContext) return;
    const ctx = canvas.getContext("2d");
    if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
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
function drawHeatmap(canvasId, res) {
    const canvas = $(canvasId);
    if (!canvas) return;
    const infoVals = res.infoValues || [], userVals = res.userValues || [], matrix = res.matrix || [];
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    const W = Math.max(1, Math.floor(rect.width)), H = Math.max(1, Math.floor(rect.height));
    canvas.width = W * dpr; canvas.height = H * dpr;
    const ctx = canvas.getContext("2d");
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);
    if (!infoVals.length || !userVals.length) return;

    let max = 0;
    matrix.forEach((row) => row.forEach((v) => { if (v > max) max = v; }));
    if (max <= 0) max = 1;

    const padL = 96, padR = 14, padT = 12, padB = 78;
    const gw = (W - padL - padR) / userVals.length, gh = (H - padT - padB) / infoVals.length;
    const colText = cssVar("--text", "#e6e6e6"), colMuted = cssVar("--muted", "#9aa0a6");
    const rgb = hexToRgb(cssVar("--accent", "#4f9dff")) || { r: 79, g: 157, b: 255 };

    for (let r = 0; r < infoVals.length; r++) {
        for (let c = 0; c < userVals.length; c++) {
            const v = (matrix[r] && matrix[r][c]) || 0;
            const a = 0.08 + 0.92 * (v / max);
            ctx.fillStyle = "rgba(" + rgb.r + "," + rgb.g + "," + rgb.b + "," + a + ")";
            ctx.fillRect(padL + c * gw, padT + r * gh, Math.max(1, gw - 1), Math.max(1, gh - 1));
        }
    }

    ctx.font = AXIS_LABEL_FONT; ctx.fillStyle = colMuted; ctx.textAlign = "right"; ctx.textBaseline = "middle";
    for (let r = 0; r < infoVals.length; r++) ctx.fillText(clipText(infoVals[r], 14), padL - 6, padT + r * gh + gh / 2);
    ctx.textAlign = "right"; ctx.textBaseline = "middle";
    for (let c = 0; c < userVals.length; c++) {
        ctx.save();
        ctx.translate(padL + c * gw + gw / 2, padT + infoVals.length * gh + 6);
        ctx.rotate(-Math.PI / 4);
        ctx.fillText(clipText(userVals[c], 14), 0, 0);
        ctx.restore();
    }

    ctx.fillStyle = colText; ctx.font = AXIS_TITLE_FONT; ctx.textAlign = "center"; ctx.textBaseline = "alphabetic";
    ctx.save(); ctx.translate(12, padT + infoVals.length * gh / 2); ctx.rotate(-Math.PI / 2);
    ctx.fillText(res.infoFeature || "info feature", 0, 0); ctx.restore();
    ctx.fillText(res.userFeature || "user feature", padL + userVals.length * gw / 2, H - 4);
    capturePlot(canvas, (res.infoFeature || "info") + " × " + (res.userFeature || "user") + " distribution");
}

function hexToRgb(hex) {
    const m = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(String(hex).trim());
    return m ? { r: parseInt(m[1], 16), g: parseInt(m[2], 16), b: parseInt(m[3], 16) } : null;
}

function clipText(s, n) {
    s = String(s);
    return s.length > n ? s.slice(0, n - 1) + "…" : s;
}

// Stacked-area variant of drawMultiLineChart: each series is a band, stacked to show composition over iterations.
function drawStackedAreaChart(canvas, series, label) {
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    const W = Math.max(1, Math.floor(rect.width)), H = Math.max(1, Math.floor(rect.height));
    canvas.width = W * dpr; canvas.height = H * dpr;
    const ctx = canvas.getContext("2d");
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);

    let n = 0;
    for (const s of series) n = Math.max(n, (s.values || []).length);
    if (!n) return;
    let max = 0;
    for (let i = 0; i < n; i++) {
        let sum = 0;
        for (const s of series) { const v = (s.values || [])[i]; if (Number.isFinite(v)) sum += v; }
        if (sum > max) max = sum;
    }
    if (max <= 0) max = 1;
    const min = 0;

    const padL = 66, padR = 16, padT = 16, padB = 50;
    const plotW = W - padL - padR, plotH = H - padT - padB;
    const xOf = (i) => padL + (n <= 1 ? plotW / 2 : (i / (n - 1)) * plotW);
    const yOf = (v) => padT + plotH - ((v - min) / (max - min)) * plotH;

    const colText = cssVar("--text", "#e6e6e6"), colMuted = cssVar("--muted", "#9aa0a6");
    const colBorder = cssVar("--border", "#333");

    ctx.strokeStyle = colMuted;
    ctx.beginPath(); ctx.moveTo(padL, padT); ctx.lineTo(padL, padT + plotH); ctx.lineTo(padL + plotW, padT + plotH); ctx.stroke();
    ctx.font = AXIS_LABEL_FONT; ctx.textAlign = "right"; ctx.textBaseline = "middle";
    for (let g = 0; g <= 4; g++) {
        const val = min + ((max - min) * g) / 4, y = yOf(val);
        ctx.fillStyle = colMuted; ctx.fillText(fmt(val), padL - 6, y);
        ctx.strokeStyle = colBorder; ctx.beginPath(); ctx.moveTo(padL, y); ctx.lineTo(padL + plotW, y); ctx.stroke();
    }
    ctx.textAlign = "center"; ctx.textBaseline = "alphabetic"; ctx.fillStyle = colMuted;
    const step = Math.max(1, Math.ceil(n / 8));
    for (let i = 0; i < n; i += step) ctx.fillText(String(i), xOf(i), padT + plotH + 15);
    if (n > 1 && (n - 1) % step !== 0) ctx.fillText(String(n - 1), xOf(n - 1), padT + plotH + 15);

    ctx.fillStyle = colText; ctx.font = AXIS_TITLE_FONT; ctx.textAlign = "center";
    ctx.fillText("iteration", padL + plotW / 2, H - 6);
    ctx.save(); ctx.translate(14, padT + plotH / 2); ctx.rotate(-Math.PI / 2); ctx.fillText(label || "value", 0, 0); ctx.restore();

    // Bands, bottom-up: each series fills between the running baseline and baseline+value.
    const baseline = new Array(n).fill(0);
    for (const s of series) {
        ctx.fillStyle = s.color; ctx.globalAlpha = 0.85; ctx.beginPath();
        for (let i = 0; i < n; i++) {
            const v = (s.values || [])[i], top = baseline[i] + (Number.isFinite(v) ? v : 0);
            const x = xOf(i), y = yOf(top);
            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        }
        for (let i = n - 1; i >= 0; i--) ctx.lineTo(xOf(i), yOf(baseline[i]));
        ctx.closePath(); ctx.fill();
        for (let i = 0; i < n; i++) { const v = (s.values || [])[i]; baseline[i] += Number.isFinite(v) ? v : 0; }
    }
    ctx.globalAlpha = 1;

    // Legend (top-left, since the stack tends to be tallest on the right).
    ctx.font = AXIS_LABEL_FONT; ctx.textBaseline = "middle";
    const lh = 15, sw = 12, gap = 5;
    let widest = 0;
    for (const s of series) widest = Math.max(widest, ctx.measureText(s.name).width);
    const boxW = sw + gap + widest + 12, boxH = series.length * lh + 8;
    const bx = padL + 6, by = padT + 4;
    ctx.fillStyle = cssVar("--panel", "#26272b"); ctx.globalAlpha = 0.9; ctx.fillRect(bx, by, boxW, boxH);
    ctx.globalAlpha = 1; ctx.strokeStyle = colBorder; ctx.strokeRect(bx, by, boxW, boxH);
    series.forEach((s, i) => {
        const cy = by + 4 + i * lh + lh / 2;
        ctx.fillStyle = s.color; ctx.fillRect(bx + 6, cy - sw / 2, sw, sw);
        ctx.fillStyle = colText; ctx.textAlign = "start"; ctx.fillText(s.name, bx + 6 + sw + gap, cy);
    });
    ctx.textBaseline = "alphabetic";
    capturePlot(canvas, label);
}

/* ------------------------------- wiring ----------------------------- */

$("btn-load").addEventListener("click", loadGraph);
$("file-input").addEventListener("change", updateImportFormatHint);
$("opt-format").addEventListener("change", updateImportFormatHint);
$("btn-generate").addEventListener("click", generateGraph);
$("gen-type").addEventListener("change", onGenTypeChange);
// Graph timeline controls.
$("tl-node-attr").addEventListener("change", (e) => { state.timeline.nodeAttr = e.target.value; state.timeline.t = null; updateTimelineUI(); });
$("tl-edge-attr").addEventListener("change", (e) => { state.timeline.edgeAttr = e.target.value; state.timeline.t = null; updateTimelineUI(); });
$("tl-slider").addEventListener("input", (e) => { stopTimeline(); setTimelineT(parseInt(e.target.value, 10) || 0); });
$("tl-time").addEventListener("change", (e) => { stopTimeline(); setTimelineT(parseInt(e.target.value, 10) || 0); });
$("tl-play").addEventListener("click", playTimeline);
$("tl-step-back").addEventListener("click", () => stepTimeline(-1));
$("tl-step-fwd").addEventListener("click", () => stepTimeline(1));
$("btn-tl-gif").addEventListener("click", downloadTimelineGif);
$("btn-tl-webm").addEventListener("click", downloadTimelineWebm);
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
$("btn-diff-gif").addEventListener("click", downloadDiffusionGif);
$("btn-diff-webm").addEventListener("click", downloadDiffusionWebm);
$("diff-node-input").addEventListener("change", () => {
    const v = $("diff-node-input").value.trim();
    if (v && state.diffusion.graph && state.diffusion.graph.hasNode(v)) selectDiffNode(v);
});
$("diffmetric-select").addEventListener("change", renderDiffMetrics);
$("diff-feature-view").addEventListener("change", () => { if (state.diffusion.lastState) renderDiffState(state.diffusion.lastState); });
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
$("diff-piece-file").addEventListener("change", (e) => { uploadPiecesCsv(e.target.files[0]); e.target.value = ""; });
$("diff-feature-file").addEventListener("change", (e) => { uploadInfoFeaturesFile(e.target.files[0]); e.target.value = ""; });
$("btn-diff-feature-add").addEventListener("click", addFeatureColumn);
document.querySelectorAll(".diffpiecesmode").forEach((b) => b.addEventListener("click", () => switchPiecesMode(b.dataset.piecesmode)));
$("btn-diff-realprop-add").addEventListener("click", addRealPropRecord);
$("btn-diff-realprop-upload").addEventListener("click", () => $("diff-realprop-file").click());
$("diff-realprop-file").addEventListener("change", (e) => { uploadRealPropagatedFile(e.target.files[0]); e.target.value = ""; });
$("btn-diff-realprop-download").addEventListener("click", downloadRealPropagatedTsv);
$("btn-diff-realprop-clear-filters").addEventListener("click", clearRpFilters);
$("btn-diff-realprop-clear").addEventListener("click", clearRealPropagated);
updateRealPropSummary();
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
setupMenu("btn-report", "report-menu", (act) => (act === "pdf" ? exportReportPdf() : exportReportHtml()));
//$("focus-toggle").addEventListener("click", toggleFocusMode);
//$("inspector-toggle").addEventListener("click", toggleInspector);
//$("style-selector").addEventListener("change", (e) => applyVisualStyle(e.target.value));
$("style-selector").addEventListener("change", (e) => applyExtendedVisualStyle(e.target.value));
setupMenu("btn-session", "session-menu", (act) => (act === "save" ? saveSession() : $("session-file").click()));
$("session-file").addEventListener("change", (e) => {
    const file = e.target.files && e.target.files[0];
    e.target.value = "";   // so re-opening the same file fires again
    openSession(file);
});

$("btn-undo").addEventListener("click", historyUndo);
$("btn-redo").addEventListener("click", historyRedo);
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
document.querySelectorAll(".diffstatsview").forEach((b) => b.addEventListener("click", () => switchStatsView(b.dataset.statsview)));

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
initDebugConsole();
