const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const source = fs.readFileSync(path.join(__dirname, "../../main/resources/public/community-explorer.js"), "utf8");
const appSource = fs.readFileSync(path.join(__dirname, "../../main/resources/public/app.js"), "utf8");
const legendExportSource = fs.readFileSync(path.join(__dirname, "../../main/resources/public/geographic-export.js"), "utf8");
function appFunction(name) {
    const start = appSource.indexOf("function " + name + "("), end = appSource.indexOf("\n}", start) + 2;
    assert.ok(start >= 0 && end > start, name);
    return appSource.slice(start, end);
}

function setup(options = {}) {
    const inputs = new Map(), ready = [], requests = [], downloads = [];
    function element() {
        return { value: "", innerHTML: "", textContent: "", hidden: false, disabled: false, checked: false, children: [], options: [{ textContent: "Circular" }],
            clientWidth: 800, clientHeight: 500, attributes: {}, style: {}, listeners: {}, classList: { toggle() {} },
            appendChild(child) { this.children.push(child); }, addEventListener(type, handler) { this.listeners[type] = handler; },
            replaceChildren(...children) { this.children = children; }, append(...children) { this.children.push(...children); },
            setAttribute(name, value) { this.attributes[name] = value; }, querySelectorAll() { return []; } };
    }
    const $ = id => { if (!inputs.has(id)) inputs.set(id, element()); return inputs.get(id); };
    const nodes = new Map([['a', { country: 'Spain' }], ['b', { country: 'Portugal' }], ['c', { country: 'Spain' }], ['isolated', {}]]);
    const graph = { order: nodes.size, hasNode: node => nodes.has(node), nodes: () => [...nodes.keys()],
        setNodeAttribute: (node, key, value) => { nodes.get(node)[key] = value; },
        forEachNode: callback => nodes.forEach((attrs, node) => callback(node, attrs)) };
    const state = { graph, graphId: "graph", activeTab: "communities", directed: true, catColors: {},
        communityData: { first: { a: 0, b: 0, c: 1, isolated: 2 }, second: { a: 1, b: 0, c: 1, isolated: 2 } },
        commMetricOrder: ["Degree_OUT · first", "Degree_OUT · second"],
        commMetricData: { "Degree_OUT · first": { algorithm: "first", values: { 0: 3, 1: 7, 2: 0 } }, "Degree_OUT · second": { algorithm: "second", values: { 0: 9, 1: 1, 2: 0 } } },
        metricData: { PageRank: { a: 0.5, b: 0.3, c: 0.2 } },
        recMetricData: { comm: { "Degree_OUT · first": { rec: { 0: 8 } } }, graph: { "Modularity (first)": { rec: 0.2 } } },
        graphMetrics: { "Modularity (first)": 0.1, Density: 0.4 }, selection: {},
    };
    const columns = [{ key: "id", label: "ID" }, { key: "a:country", label: "Country" }, { key: "m:PageRank", label: "PageRank" }];
    const rowValue = (id, key) => key === "id" ? id : key.startsWith("a:") ? nodes.get(id)?.[key.slice(2)] : state.metricData[key.slice(2)]?.[id];
    const context = vm.createContext({ state, $, console, AbortController, Date,
        setTimeout: options.setTimeout || setTimeout, clearTimeout: options.clearTimeout || clearTimeout,
        loadCommunityAdapter: options.loadCommunityAdapter,
        document: { getElementById: $, createElement: element, createElementNS: (_ns, tag) => ({ ...element(), tag }),
            addEventListener: (type, handler) => { if (type === "DOMContentLoaded") ready.push(handler); } },
        window: { addEventListener() {} }, getComputedStyle: () => ({ getPropertyValue: () => "#ffffff" }),
        option: (value, textContent) => ({ value, textContent }), esc: value => String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll('"', '&quot;'),
        fmt: value => String(value), categoricalHex: index => ["#123456", "#abcdef", "#778899"][index % 3],
        toHexColor: color => color, setTooltip() {}, applyAppearance() {},
        nodeColumns: () => columns, nodeRowValue: rowValue,
        requestAnimationFrame: callback => { queueMicrotask(callback); return 1; },
        jsonBody: body => ({ method: "POST", body: JSON.stringify(body) }),
        api: (url, options) => new Promise(resolve => requests.push({ url, options, body: JSON.parse(options.body), resolve })),
        download: (...args) => downloads.push(args),
        buildCsv: (cols, rows, value) => [cols.map(col => col.label).join(","), ...rows.map(row => cols.map(col => value(row, col.key) ?? "").join(","))].join("\n"),
        clearSelection() { state.selection = {}; }, selectNode: id => { state.selection.node = id; },
        setSelectionMode: mode => { state.selection.mode = mode; }, setSelectionPartition: partition => { state.selection.partition = partition; },
        switchTab: tab => { state.activeTab = tab; }, switchSubtab: tab => { state.subtab = tab; }, setStatus() {},
    });
    const testSource = source.replace('import("./community-cosmograph.js")', 'loadCommunityAdapter()');
    const h = vm.runInContext(testSource + "\n({e:COMMUNITY_EXPLORER, communityRows, communitySortedRows, communityColors, communityMembers, communityMetricColumns, communityDetected, renderCommunityTable, renderCommunityInspector, selectCommunity, communityToNetwork, communityToMetrics, collectCommunityExplorer, restoreCommunityExplorer, resetCommunityExplorer, refreshCommunityExplorer, loadCommunityGraph, renderCommunityGraph, communityGraphKey, communityLayoutId, leaveCommunityExplorer})", context);
    Object.assign(h, vm.runInContext(["colorByCommunity", "applyCategorical", "buildNodeLegend", "legendCategoryRow", "scaleVisualValues"].map(appFunction).join("\n")
        + "\n({colorByCommunity, applyCategorical, buildNodeLegend})", context));
    Object.assign(h, vm.runInContext("({communityGraphStyles, communityNodeLabelColor, communityForegroundMarkup, drawCommunityLoops, applyCommunityAppearanceControls, normalizeCommunityAppearance, updateCommunityGraphAppearance, communityLoopMarkup, exportCommunityGraph, layouts: COMMUNITY_LAYOUTS})", context));
    Object.assign(h, vm.runInContext("async " + appFunction("detectCommunity")
        + "\n({detectCommunity, findCommunityPartition, selectCommunityPartition})", context));
    vm.runInContext(legendExportSource, context);
    h.e.partition = "first"; h.e.view = "table";
    ready.forEach(handler => handler());
    return { ...h, state, $, nodes, requests, downloads, context };
}
const plain = value => JSON.parse(JSON.stringify(value));
const response = tag => ({ tag, directed: true, nodes: [], edges: [], positions: {} });

const tick = () => new Promise(resolve => setImmediate(resolve));
function pendingRenderHarness(loadCommunityAdapter) {
    const timers = new Map(); let nextTimer = 0;
    const h = setup({ loadCommunityAdapter,
        setTimeout: (callback, delay) => { assert.equal(delay, 30000); timers.set(++nextTimer, callback); return nextTimer; },
        clearTimeout: id => timers.delete(id) });
    const sigma = [];
    class Renderer {
        constructor() { this.killed = false; this.handlers = {}; sigma.push(this); }
        on(type, handler) { this.handlers[type] = handler; } kill() { this.killed = true; } refresh() {}
        getCamera() { return { getState: () => ({ ratio: 1 }) }; }
    }
    h.context.Graph = class { addNode() {} addEdgeWithKey() {} };
    h.context.Sigma = Renderer;
    // These tests exercise initialization ownership rather than appearance rendering.
    vm.runInContext("updateCommunityGraphAppearance = () => Promise.resolve()", h.context);
    h.e.view = "graph";
    h.e.data = { directed: true, nodes: [{ id: "0", size: 1 }], edges: [], positions: { 0: { x: 0, y: 0 } } };
    h.e.dataKey = h.communityGraphKey();
    return { ...h, timers, sigma, Renderer };
}

test("switching to Sigma escapes a stalled Cosmograph import without waiting for it", async () => {
    let finishImport, creations = 0;
    const h = pendingRenderHarness(() => new Promise(resolve => { finishImport = resolve; }));
    const pending = h.renderCommunityGraph();
    assert.equal(h.e.renderPending, true);
    h.$("explorer-renderer").value = "sigma"; h.$("explorer-dimensions").value = "2";
    h.$("explorer-renderer").onchange(); await pending;
    assert.equal(h.e.renderer, h.sigma[0]); assert.equal(h.e.renderPending, false);
    assert.equal(h.$("explorer-graph-status").textContent, "");
    finishImport({ create: () => { creations++; } }); await tick();
    assert.equal(creations, 0); assert.equal(h.e.renderer, h.sigma[0]); assert.equal(h.timers.size, 0);
});

test("an obsolete initialization cannot clear the pending state of its replacement", async () => {
    const attempts = [];
    const h = pendingRenderHarness(async () => ({ create: (_graph, _container, options) => new Promise((resolve, reject) => {
        attempts.push({ resolve, signal: options.signal });
        options.signal.addEventListener("abort", () => reject(options.signal.reason), { once: true });
    }) }));
    const first = h.renderCommunityGraph(); await tick();
    h.leaveCommunityExplorer(); h.e.partition = "second"; h.e.dataKey = h.communityGraphKey();
    const second = h.renderCommunityGraph(); await first; await tick();
    assert.equal(attempts[0].signal.aborted, true); assert.equal(attempts.length, 2);
    assert.equal(h.e.renderPending, true); assert.equal(h.e.renderController.signal, attempts[1].signal);
    assert.match(h.$("explorer-graph-status").textContent, /Loading Cosmograph/);
    const renderer = new h.Renderer(); attempts[1].resolve(renderer); await second;
    assert.equal(h.e.renderer, renderer); assert.equal(h.e.renderPending, false); assert.equal(h.timers.size, 0);
});

test("Cosmograph initialization times out with a recovery message and Sigma can then render", async () => {
    const h = pendingRenderHarness(() => new Promise(() => {}));
    const pending = h.renderCommunityGraph();
    [...h.timers.values()][0](); await pending;
    assert.equal(h.e.renderPending, false); assert.equal(h.e.renderController, null);
    assert.match(h.$("explorer-graph-status").textContent, /timed out.*Apply layout/);
    h.$("explorer-renderer").value = "sigma"; h.$("explorer-dimensions").value = "2";
    h.$("explorer-renderer").onchange();
    assert.equal(h.e.renderer, h.sigma[0]); assert.equal(h.$("explorer-graph-status").textContent, "");
    assert.equal(h.timers.size, 0);
});

test("switching to Sigma cancels a stalled upload and ignores its later failure", async () => {
    let signal, fail;
    const h = pendingRenderHarness(async () => ({ create: (_graph, _container, options) => {
        signal = options.signal;
        // Simulate a late library failure even after the cancellation notification.
        return new Promise((_resolve, reject) => { fail = reject; });
    } }));
    const pending = h.renderCommunityGraph(); await tick();
    h.$("explorer-renderer").value = "sigma"; h.$("explorer-dimensions").value = "2";
    h.$("explorer-renderer").onchange();
    assert.equal(signal.aborted, true); assert.equal(h.e.renderer, h.sigma[0]);
    fail(new Error("obsolete upload failed")); await pending;
    assert.equal(h.e.renderer, h.sigma[0]); assert.equal(h.e.renderPending, false);
    assert.equal(h.$("explorer-graph-status").textContent, ""); assert.equal(h.timers.size, 0);
});

test("leaving the graph or resetting the network cancels a pending initialization", async () => {
    for (const action of [h => h.$("explorer-view-table").onclick(), h => { h.state.communityData = {}; h.resetCommunityExplorer(); }]) {
        const h = pendingRenderHarness(() => new Promise(() => {}));
        const pending = h.renderCommunityGraph(), signal = h.e.renderController.signal;
        action(h); await pending;
        assert.equal(signal.aborted, true); assert.equal(h.e.renderPending, false);
        assert.equal(h.e.renderer, null); assert.equal(h.timers.size, 0);
    }
});

test("community hover events redraw labels without an appearance refresh and ignore obsolete renderers", async () => {
    const h = pendingRenderHarness(() => new Promise(() => {}));
    h.context.hoverRedraws = 0;
    vm.runInContext("drawCommunityLoops = () => { hoverRedraws++; }", h.context);
    h.e.rendererType = "sigma";
    await h.renderCommunityGraph();
    const old = h.e.renderer;
    old.handlers.enterNode({ node: "0" }); assert.equal(h.e.hoveredNode, "0");
    old.handlers.leaveNode({ node: "1" }); assert.equal(h.e.hoveredNode, "0");
    old.handlers.leaveNode({ node: "0" }); assert.equal(h.e.hoveredNode, null);
    assert.equal(h.context.hoverRedraws, 2);
    h.leaveCommunityExplorer(); await h.renderCommunityGraph();
    h.e.renderer.handlers.enterNode({ node: "1" });
    old.handlers.enterNode({ node: "0" }); old.handlers.leaveNode({ node: "1" });
    assert.equal(h.e.hoveredNode, "1"); assert.equal(h.context.hoverRedraws, 3);
    assert.equal(h.requests.length, 0);
});

function renderedGraph(h) {
    h.e.rendererType = "sigma";
    h.e.data = { directed: true, nodes: [{ id: "0", size: 1 }, { id: "1", size: 5 }, { id: "2", size: 9 }],
        edges: [{ source: "0", target: "1", weight: -2 }, { source: "1", target: "2", weight: 6 }, { source: "2", target: "2", weight: 10 }] };
    const nodes = new Map(h.e.data.nodes.map((node, index) => [node.id, { x: index * 100 + 10, y: 100, size: 4, color: "#123456" }]));
    const edges = new Map([["0", { size: 1, color: "#888888" }], ["1", { size: 1, color: "#888888" }]]);
    h.e.graph = { nodes: () => [...nodes.keys()], getNodeAttributes: id => nodes.get(id),
        mergeNodeAttributes: (id, attrs) => Object.assign(nodes.get(id), attrs), mergeEdgeAttributes: (id, attrs) => Object.assign(edges.get(id), attrs),
        forEachNode: fn => nodes.forEach((attrs, id) => fn(id, attrs)),
        forEachEdge: fn => edges.forEach((attrs, id) => { const edge = h.e.data.edges[Number(id)]; fn(id, attrs, edge.source, edge.target); }) };
    const settings = {}, camera = { x: 7, y: 8, ratio: 2 };
    let refreshes = 0, fits = 0;
    const zooms = [];
    h.e.renderer = { setSetting: (key, value) => { settings[key] = value; }, refresh: () => refreshes++,
        getVisibleLabelNodes: () => new Set(nodes.keys()),
        graphToViewport: attrs => ({ x: attrs.x, y: attrs.y }), getNodeDisplayData: id => ({ size: nodes.get(id).size }),
        getCamera: () => ({ getState: () => camera, animatedReset: () => fits++, animatedZoom: ({ factor }) => zooms.push(factor) }) };
    return { nodes, edges, settings, camera, zooms, refreshes: () => refreshes, fits: () => fits };
}

test("node size bounds, label modes and edge styles update rendered data without requesting a layout", () => {
    const h = setup(), view = renderedGraph(h);
    h.$("community-node-min").value = "6"; h.$("community-node-max").value = "18";
    h.$("community-label-mode").value = "single"; h.$("community-label-color").value = "#112233";
    h.$("community-edge-size-mode").value = "uniform"; h.$("community-edge-width").value = "3";
    h.$("community-edge-color-mode").value = "single"; h.$("community-edge-color").value = "#ff0000";
    h.$("community-edge-width").oninput();
    assert.equal(view.nodes.get("0").size, 6); assert.equal(view.nodes.get("2").size, 18);
    assert.equal(view.nodes.get("1").size, 12);
    assert.equal(view.nodes.get("1").x, 110);
    assert.equal(view.edges.get("0").size, 3); assert.equal(view.edges.get("0").color, "#ff0000");
    assert.deepEqual(plain(view.settings.labelColor), { color: "#112233" });
    assert.equal(h.communityNodeLabelColor({ color: "#abcabc" }), "#112233");
    assert.equal(h.requests.length, 0); assert.ok(view.refreshes() > 0);
    h.$("community-label-mode").value = "node"; h.$("community-label-mode").oninput();
    assert.deepEqual(plain(view.settings.labelColor), { attribute: "color" });
    assert.equal(h.communityNodeLabelColor({ color: "#abcabc" }), "#abcabc");
    h.$("community-label-mode").value = "default"; h.$("community-label-mode").oninput();
    assert.equal(h.communityNodeLabelColor({ color: "#abcabc" }), "#000000");
    assert.equal(h.$("community-label-color-field").hidden, true);
});

test("weighted thickness uses absolute values, honours both bounds and colours endpoints consistently", () => {
    const h = setup(); renderedGraph(h);
    Object.assign(h.e.appearance, { nodeMin: 10, nodeMax: 10, edgeMin: 1, edgeMax: 9, edgeColorMode: "source" });
    const styles = h.communityGraphStyles();
    assert.equal(styles.edge(h.e.data.edges[0]).size, 1); assert.equal(styles.edge(h.e.data.edges[2]).size, 9);
    assert.equal(styles.edge(h.e.data.edges[0]).color, h.communityColors()["0"]);
    assert.equal(styles.node(h.e.data.nodes[0]).size, 10); assert.equal(styles.node(h.e.data.nodes[2]).size, 10);
    h.e.appearance.edgeColorMode = "target";
    assert.equal(h.communityGraphStyles().edge(h.e.data.edges[0]).color, h.communityColors()["1"]);
    h.e.appearance.edgeMin = h.e.appearance.edgeMax = 0;
    assert.equal(h.communityGraphStyles().edge(h.e.data.edges[0]).hidden, true);
});

test("edge shape switches Sigma programs, fits the graph and persists in sessions", async () => {
    const h = setup(), view = renderedGraph(h);
    h.context.window.relisonSigmaEdgePrograms = {};
    assert.equal(h.e.appearance.edgeShape, "curved");
    await h.updateCommunityGraphAppearance();
    assert.equal(view.settings.defaultEdgeType, "curvedArrow");
    h.$("community-edge-shape").value = "straight";
    await h.$("community-edge-shape").oninput();
    assert.equal(view.settings.defaultEdgeType, "arrow"); assert.equal(view.fits(), 1);
    h.e.data.directed = false;
    await h.updateCommunityGraphAppearance();
    assert.equal(view.settings.defaultEdgeType, "line");
    const saved = plain(h.collectCommunityExplorer());
    const restored = setup(); restored.restoreCommunityExplorer(saved); restored.refreshCommunityExplorer();
    assert.equal(restored.e.appearance.edgeShape, "straight");
    assert.equal(restored.$("community-edge-shape").value, "straight");
    h.$("community-edge-shape").value = "curved";
    await h.$("community-edge-shape").oninput();
    assert.equal(view.settings.defaultEdgeType, "curve");
    delete h.context.window.relisonSigmaEdgePrograms;
    await h.updateCommunityGraphAppearance();
    assert.equal(view.settings.defaultEdgeType, "line");
    assert.equal(h.normalizeCommunityAppearance({ edgeShape: "invalid" }).edgeShape, "curved");
    assert.equal(h.requests.length, 0);
});

test("edge labels and vector exports follow the selected shape in both renderers while internal links stay loops", async () => {
    const h = setup(); renderedGraph(h); h.e.loops = true;
    h.context.window.relisonSigmaEdgePrograms = {};
    h.e.appearance.edgeLabelShow = true;
    for (const renderer of ["sigma", "cosmograph", "cosmograph-3d"]) {
        h.e.rendererType = renderer;
        for (const shape of ["curved", "straight"]) {
            h.e.appearance.edgeShape = shape;
            await h.updateCommunityGraphAppearance();
            const y = shape === "straight" ? 96 : renderer === "sigma" ? 86 : 106;
            assert.match(h.communityForegroundMarkup(), new RegExp('<text x="60" y="' + y + '"'));
            await h.exportCommunityGraph(false);
            const svg = h.downloads.at(-1)[1];
            assert.match(svg, shape === "straight" ? /<path d="M10 100 L / : /<path d="M10 100 Q60 /);
            assert.match(svg, / C /);
            assert.match(svg, /marker-end="url\(#community-export-arrow-0\)"/);
        }
    }
});

test("average edge colours are symmetric and internal links retain their community colour", () => {
    const h = setup(); renderedGraph(h); h.e.appearance.edgeColorMode = "average";
    const styles = h.communityGraphStyles();
    assert.equal(styles.edge(h.e.data.edges[0]).color, "#5f81a3");
    assert.equal(styles.edge({ source: "1", target: "0", weight: 2 }).color, "#5f81a3");
    assert.equal(styles.edge(h.e.data.edges[2]).color, h.communityColors()["2"]);
});

test("proportional and percentile sizes match Network scaling including ties and uniform values", () => {
    const h = setup(); renderedGraph(h); h.e.appearance.nodeMin = 4; h.e.appearance.nodeMax = 24;
    h.e.data.nodes.forEach((node, index) => { node.size = [1, 2, 101][index]; });
    assert.ok(Math.abs(h.communityGraphStyles().node(h.e.data.nodes[1]).size - 4.2) < 1e-10);
    h.e.appearance.nodeScale = "percentile";
    assert.equal(h.communityGraphStyles().node(h.e.data.nodes[1]).size, 14);
    h.e.data.nodes[1].size = 1;
    assert.equal(h.communityGraphStyles().node(h.e.data.nodes[0]).size, h.communityGraphStyles().node(h.e.data.nodes[1]).size);
    h.e.data.nodes.forEach(node => { node.size = 7; });
    assert.equal(h.communityGraphStyles().node(h.e.data.nodes[2]).size, 4);
});

test("node borders and both kinds of label render in the foreground with chosen typography and colours", async () => {
    const h = setup(); renderedGraph(h); h.e.loops = true;
    Object.assign(h.e.appearance, { nodeBorderOn: true, nodeBorderWidth: 3, nodeBorderColor: "#abcdef",
        labelSize: 20, labelFont: "Georgia, serif", edgeLabelShow: true, edgeLabelSize: 13, edgeLabelFont: "monospace",
        edgeLabelMode: "edge", edgeColorMode: "average" });
    await h.updateCommunityGraphAppearance();
    const markup = h.$("community-foreground").innerHTML;
    assert.match(markup, /stroke="#abcdef" stroke-width="3"/);
    assert.match(markup, /fill="#000000" font-size="20" font-family="Georgia, serif"/);
    assert.match(markup, /fill="#5f81a3" font-size="13" font-family="monospace"/);
    assert.ok(markup.indexOf('class="community-node-labels"') > markup.indexOf('class="community-edge-labels"'));
    h.e.appearance.labelProp = true; h.e.appearance.edgeLabelProp = true;
    assert.match(h.communityForegroundMarkup(), /font-size="62.5"/);
    await h.exportCommunityGraph(false);
    const svg = h.downloads[0][1];
    assert.ok(svg.indexOf('class="community-node-labels"') > svg.lastIndexOf('fill="#778899"'));
    h.e.appearance.labelShow = h.e.appearance.edgeLabelShow = false;
    assert.doesNotMatch(h.communityForegroundMarkup(), /<text/);
    h.e.rendererType = "cosmograph";
    assert.equal(h.communityNodeLabelColor({ color: "#abcdef" }), "#ffffff");
    h.e.rendererType = "sigma";
    assert.equal(h.communityNodeLabelColor({ color: "#abcdef" }), "#000000");
});

test("Cosmograph foreground and exported node/edge labels retain measured background boxes in 2D and 3D", async () => {
    const h = setup(); renderedGraph(h);
    const createElement = h.context.document.createElement;
    const measureContext = { font: "", measureText: text => ({ width: text.length * 8, actualBoundingBoxAscent: 10, actualBoundingBoxDescent: 3 }) };
    h.context.document.createElement = tag => tag === "canvas" ? { getContext: () => measureContext } : createElement(tag);
    h.e.appearance.edgeLabelShow = true;
    for (const renderer of ["cosmograph", "cosmograph-3d"]) {
        h.e.rendererType = renderer;
        await h.updateCommunityGraphAppearance();
        const markup = h.$("community-foreground").innerHTML;
        assert.equal((markup.match(/class="community-label-background"/g) || []).length, 5);
        assert.match(markup, /width="98" height="19" rx="3" fill="#232428" fill-opacity="0.92"/);
        assert.ok(markup.indexOf('class="community-label-background"') < markup.indexOf("<text"));
        await h.exportCommunityGraph(false);
        assert.match(h.downloads.at(-1)[1], /class="community-label-background"/);
    }
    h.e.rendererType = "sigma";
    assert.doesNotMatch(h.communityForegroundMarkup(), /class="community-label-background"/);
});

test("Cosmograph foreground and exports show only native dynamic labels, with hover always visible and painted last", async () => {
    const h = setup(), view = renderedGraph(h);
    for (const type of ["cosmograph", "cosmograph-3d"]) {
        h.e.rendererType = type;
        h.e.renderer.getVisibleLabelNodes = () => new Set(["1"]);
        h.e.appearance.labelShow = true; h.e.hoveredNode = null;
        await h.updateCommunityGraphAppearance();
        let markup = h.communityForegroundMarkup();
        assert.match(markup, />Community 1<\/text>/); assert.doesNotMatch(markup, />Community [02]<\/text>/);
        h.e.hoveredNode = "0";
        markup = h.communityForegroundMarkup();
        assert.ok(markup.indexOf(">Community 0</text>") > markup.indexOf(">Community 1</text>"));
        h.e.appearance.labelShow = false;
        markup = h.communityForegroundMarkup();
        assert.match(markup, />Community 0<\/text>/); assert.doesNotMatch(markup, />Community [12]<\/text>/);
        assert.match(markup, /class="community-label-background"/);
        await h.exportCommunityGraph(false);
        assert.match(h.downloads.at(-1)[1], />Community 0<\/text>/);
        assert.doesNotMatch(h.downloads.at(-1)[1], />Community [12]<\/text>/);
        h.e.hoveredNode = null;
        assert.doesNotMatch(h.communityForegroundMarkup(), /<text/);
    }
    h.e.rendererType = "sigma"; h.e.hoveredNode = "2";
    assert.match(h.communityForegroundMarkup(), />Community 2<\/text>/);
    h.e.renderer.kill = () => {}; h.leaveCommunityExplorer();
    assert.equal(h.e.hoveredNode, null); assert.equal(view.fits(), 0);
});

test("valid appearance edits fit Sigma and queue Cosmograph Fit while selection alone preserves the camera", async () => {
    const h = setup(), view = renderedGraph(h);
    h.$("community-node-border-on").checked = true;
    await h.$("community-node-border-on").oninput();
    assert.equal(view.fits(), 1);
    await h.updateCommunityGraphAppearance();
    assert.equal(view.fits(), 1);
    h.$("community-node-min").value = "100"; h.$("community-node-max").value = "3";
    await h.$("community-node-min").oninput();
    assert.equal(view.fits(), 1);
    h.e.rendererType = "cosmograph";
    const requests = [];
    h.e.renderer.refresh = options => { requests.push(options); return Promise.resolve(); };
    h.$("community-node-min").value = "4"; h.$("community-node-max").value = "25";
    await h.$("community-node-min").oninput();
    assert.deepEqual(plain(requests), [{ fit: true }]);
    const saved = plain(h.collectCommunityExplorer());
    h.restoreCommunityExplorer(saved);
    assert.equal(h.e.appearance.nodeBorderOn, true);
    assert.equal(h.requests.length, 0);
});

test("invalid appearance ranges leave active settings unchanged, and sessions restore valid settings", () => {
    const h = setup(), original = plain(h.e.appearance);
    h.$("community-node-min").value = "100"; h.$("community-node-max").value = "3";
    h.applyCommunityAppearanceControls();
    assert.match(h.$("community-appearance-status").textContent, /must not exceed/);
    assert.deepEqual(plain(h.e.appearance), original);
    h.$("community-node-min").value = ""; h.applyCommunityAppearanceControls();
    assert.match(h.$("community-appearance-status").textContent, /Enter valid/);
    h.e.appearance.nodeMax = 32; h.e.appearance.labelMode = "node";
    const saved = plain(h.collectCommunityExplorer());
    h.resetCommunityExplorer(); h.restoreCommunityExplorer(saved);
    assert.equal(h.e.appearance.nodeMax, 32); assert.equal(h.e.appearance.labelMode, "node");
    h.restoreCommunityExplorer({ appearance: { nodeMin: -20, nodeMax: Infinity, labelColor: "bad", edgeSizeMode: "unknown" } });
    assert.deepEqual(plain(h.e.appearance), original);
});

test("loops and vector exports share the current edge and label appearance", async () => {
    const h = setup(), view = renderedGraph(h); h.e.loops = true;
    Object.assign(h.e.appearance, { labelMode: "single", labelColor: "#223344", edgeColorMode: "single", edgeColor: "#ff0000", edgeSizeMode: "uniform", edgeWidth: 4 });
    h.updateCommunityGraphAppearance();
    assert.match(h.communityLoopMarkup(), /stroke="#ff0000" stroke-width="4"/);
    await h.exportCommunityGraph(false);
    const svg = h.downloads[0][1];
    assert.match(svg, /stroke="#ff0000" stroke-width="4"/);
    assert.match(svg, /fill="#223344" font-size="12"/);
    assert.match(svg, new RegExp('r="' + view.nodes.get("2").size + '"'));
});

test("every expanded layout maps to a permitted backend layout in 2D and 3D", () => {
    const h = setup();
    const controller = fs.readFileSync(path.join(__dirname, "../../main/java/es/uam/eps/ir/relison/gui/CommunityController.java"), "utf8");
    const allowed = [...controller.match(/GRAPH_LAYOUTS = List\.of\(([\s\S]*?)\);/)[1].matchAll(/"([^"]+)"/g)].map(match => match[1]);
    assert.ok(h.layouts.length >= 10);
    for (const layout of h.layouts) {
        h.e.layout = layout.id; h.e.rendererType = "sigma";
        assert.equal(h.communityLayoutId(), layout.id); assert.ok(allowed.includes(h.communityLayoutId()));
        h.e.rendererType = "cosmograph-3d";
        assert.equal(h.communityLayoutId(), layout.spatial); assert.ok(allowed.includes(h.communityLayoutId()));
        if (layout.spatial.endsWith("-flat-3d")) assert.match(layout.spatialLabel, /planar in 3D/);
        const saved = plain(h.collectCommunityExplorer()); h.restoreCommunityExplorer(saved);
        assert.equal(h.e.layout, layout.id);
    }
});

test("community table and inspector resolve membership and metrics within the selected partition", () => {
    const h = setup();
    assert.deepEqual(plain(h.communityRows().map(({ id, size, share }) => ({ id, size, share }))),
        [{ id: "0", size: 2, share: 50 }, { id: "1", size: 1, share: 25 }, { id: "2", size: 1, share: 25 }]);
    assert.deepEqual(plain(h.communityMetricColumns()), [{ key: "metric:Degree_OUT · first", label: "Degree_OUT" }]);
    h.e.sort = "metric:Degree_OUT · first";
    assert.deepEqual(plain(h.communitySortedRows().map(row => row.id)), ["1", "0", "2"]);
    h.selectCommunity("2");
    assert.match(h.$("community-details").innerHTML, /Degree_OUT<\/th><td>0<\/td>/);
    assert.match(h.$("community-members").innerHTML, /isolated/);
    h.e.partition = "second";
    assert.deepEqual(plain(h.communityRows().find(row => row.id === "1").members), ["a", "c"]);
});

test("member search includes attributes and the selected computed node metric", () => {
    const h = setup(); h.selectCommunity("0");
    h.e.memberSearch = "portugal";
    assert.deepEqual(plain(h.communityMembers()), ["b"]);
    h.e.memberColumn = "m:PageRank"; h.e.memberSearch = "0.5";
    assert.deepEqual(plain(h.communityMembers()), ["a"]);
});

test("labels are escaped, editable without changing IDs, and saved with selection and renderer", () => {
    const h = setup(); h.selectCommunity("0");
    h.$("community-label").value = 'Research <group> "A"'; h.$("community-save-label").onclick();
    assert.match(h.$("community-table").innerHTML, /Research &lt;group>/);
    assert.equal(h.state.communityData.first.a, 0);
    h.e.rendererType = "cosmograph-3d";
    const saved = plain(h.collectCommunityExplorer());
    h.resetCommunityExplorer(); h.restoreCommunityExplorer(saved);
    assert.equal(h.e.labels.first["0"], 'Research <group> "A"');
    assert.equal(h.e.selected.first, "0"); assert.equal(h.e.view, "table");
    assert.equal(h.e.rendererType, "cosmograph-3d");
    h.restoreCommunityExplorer({ rendererType: "unknown", mode: "unknown" });
    assert.equal(h.e.rendererType, "cosmograph"); assert.equal(h.e.mode, "weighted");
});

test("colours match first-appearance order and honour Network legend overrides", () => {
    const h = setup();
    h.state.communityData.first = { a: 2, b: 0, c: 1, isolated: 2 };
    h.state.catColors["community:first"] = { 0: "#ff0000" };
    assert.deepEqual(plain(h.communityColors()), { 0: "#ff0000", 1: "#778899", 2: "#123456" });
});

test("Network legend resolves labels for its colouring partition independently of the explorer partition", () => {
    const h = setup();
    h.e.labels = { first: { 0: "Research", 1: "Research" }, second: { 0: "Other partition" } };
    h.e.partition = "second";
    h.colorByCommunity("first"); h.buildNodeLegend();
    assert.deepEqual(plain(h.state.colorLegend.entries.map(({ value, label }) => ({ value, label }))),
        [{ value: "0", label: "Research" }, { value: "1", label: "Research" }, { value: "2", label: "Community 2" }]);
    const rows = h.$("node-legend").children.at(-1).children;
    assert.deepEqual(rows.map(row => row.children.at(-1).textContent), ["Research", "Research", "Community 2"]);
    rows[1].children[0].value = "#ff0000"; rows[1].children[0].listeners.input();
    assert.deepEqual(plain(h.state.catColors["community:first"]), { 1: "#ff0000" });
    h.colorByCommunity("first");
    assert.equal(h.nodes.get("a").color, "#123456"); assert.equal(h.nodes.get("c").color, "#ff0000");
});

test("saving a community label immediately refreshes the main legend and survives session restoration", () => {
    const h = setup(); h.colorByCommunity("first"); h.selectCommunity("0");
    h.$("community-label").value = "Research <group>"; h.$("community-save-label").onclick();
    assert.equal(h.state.colorLegend.entries[0].label, "Research <group>");
    assert.equal(h.$("node-legend").children.at(-1).children[0].children.at(-1).textContent, "Research <group>");
    const saved = plain(h.collectCommunityExplorer());
    h.resetCommunityExplorer(); h.restoreCommunityExplorer(saved); h.colorByCommunity("first");
    assert.equal(h.state.colorLegend.entries[0].label, "Research <group>");
    h.e.partition = "second"; h.selectCommunity("0");
    h.$("community-label").value = "Different"; h.$("community-save-label").onclick();
    assert.equal(h.state.colorLegend.entries[0].label, "Research <group>");
});

test("exported legends use community display labels while ordinary categorical legends keep their values", () => {
    const h = setup(); h.e.labels.first = { 0: "Research" }; h.colorByCommunity("first");
    const svg = { children: [], appendChild(child) { this.children.push(child); } };
    h.context.window.relisonGeographicExport.legend(svg, 800, 600, h.state.colorLegend);
    assert.equal(svg.children[0].children.find(child => child.tag === "text" && child.textContent === "Research")?.textContent, "Research");
    h.applyCategorical("attrcat:country", { a: "Spain", b: "Portugal", c: "Spain" }, "Country");
    assert.deepEqual(plain(h.state.colorLegend.entries.map(entry => entry.value)), ["Spain", "Portugal"]);
    const categories = { children: [], appendChild(child) { this.children.push(child); } };
    h.context.window.relisonGeographicExport.legend(categories, 800, 600, h.state.colorLegend);
    assert.ok(categories.children[0].children.some(child => child.tag === "text" && child.textContent === "Spain"));
});

test("rerunning detection drops that partition's labels and metrics, preserving other partitions", () => {
    const h = setup(); h.e.labels.first = { 0: "Old label" };
    h.communityDetected("first", "Louvain", { threshold: 0.001 }, 3);
    assert.equal(h.e.labels.first, undefined);
    assert.deepEqual(plain(h.state.commMetricOrder), ["Degree_OUT · second"]);
    assert.equal(h.state.recMetricData.comm["Degree_OUT · first"], undefined);
    assert.equal(h.state.recMetricData.graph["Modularity (first)"], undefined);
    assert.deepEqual(plain(h.state.graphMetrics), { Density: 0.4 });
    assert.equal(h.e.metadata.first.count, 3);
});

function detectionHarness(params = {}) {
    const h = setup(), calls = { starts: 0, ends: 0, cancels: 0, statuses: [] };
    h.$("community-algo").value = "Louvain";
    Object.assign(h.context, {
        collectParams: () => params, requireGraph: () => true, jobBusy: () => calls.starts > calls.ends,
        cancelJob: () => calls.cancels++, beginJob: () => { calls.starts++; return new AbortController().signal; },
        endJob: () => calls.ends++, isAbort: error => error?.name === "AbortError",
        setStatus: (...value) => calls.statuses.push(value), rebuildAppearanceOptions() {}, syncNodeColorControls() {},
        setSelectOptions: (id, values) => h.$(id).replaceChildren(...values.map(value => ({ value, textContent: value }))),
        updatePartitionField() {}, applyReducers() {}, refreshAfterCompute: h.refreshCommunityExplorer,
    });
    return { ...h, calls };
}

test("Detect reuses matching typed parameters regardless of key order and preserves existing results", async () => {
    const h = detectionHarness({ iterations: 10, threshold: 0.001, weighted: false });
    h.e.metadata.first = { algorithm: "Louvain", params: { weighted: false, threshold: 0.001, iterations: 10 }, count: 3 };
    h.e.partition = "second"; h.e.labels.first = { 0: "Research" }; h.e.selected.first = "1";
    const metrics = plain(h.state.commMetricData), assignment = h.state.communityData.first;
    await h.detectCommunity();
    assert.equal(h.e.partition, "first"); assert.equal(h.$("explorer-partition").value, "first");
    assert.equal(h.e.labels.first["0"], "Research"); assert.equal(h.e.selected.first, "1");
    assert.deepEqual(plain(h.state.commMetricData), metrics); assert.equal(h.state.communityData.first, assignment);
    assert.equal(h.requests.filter(request => request.url === "/api/communities").length, 0);
    assert.equal(h.calls.starts, 0); assert.equal(h.calls.ends, 0);
    assert.match(h.$("community-result").textContent, /first.*3 communities/);
    assert.match(h.calls.statuses.at(-1)[0], /Selected existing partition/);
    const saved = plain(h.collectCommunityExplorer()), restored = setup();
    restored.restoreCommunityExplorer(saved);
    assert.equal(restored.findCommunityPartition("Louvain", { threshold: 0.001, iterations: 10, weighted: false }), "first");
});

test("selecting the already active configuration preserves its camera and inspector", async () => {
    const h = detectionHarness({}), view = renderedGraph(h);
    h.e.metadata.first = { algorithm: "Louvain", params: {}, count: 3 };
    h.e.dataKey = h.communityGraphKey(); h.e.selected.first = "0";
    const renderer = h.e.renderer;
    await h.detectCommunity();
    assert.equal(h.e.renderer, renderer); assert.equal(h.e.selected.first, "0");
    assert.equal(view.fits(), 0); assert.equal(h.requests.length, 0); assert.equal(h.calls.starts, 0);
});

test("new parameter configurations run detection once, and subsequent Detect selects the stored result", async () => {
    const params = { threshold: 0.02 }, h = detectionHarness(params);
    h.e.metadata.first = { algorithm: "Louvain", params: { threshold: 0.001 }, count: 3 };
    const pending = h.detectCommunity();
    assert.equal(h.calls.starts, 1); assert.equal(h.requests[0].url, "/api/communities");
    assert.deepEqual(h.requests[0].body.params, params);
    h.requests[0].resolve({ algorithm: "Louvain threshold=0.02", label: "Louvain threshold=0.02", values: { a: 0, b: 1, c: 1, isolated: 2 }, numCommunities: 3 });
    await pending;
    assert.equal(h.calls.ends, 1);
    assert.equal(h.e.partition, "Louvain threshold=0.02");
    await h.detectCommunity();
    assert.equal(h.requests.filter(request => request.url === "/api/communities").length, 1);
    assert.equal(h.calls.starts, 1);
});

test("configuration reuse excludes different methods, parameter names/types/values and removed partitions", () => {
    const h = setup();
    h.e.metadata.first = { algorithm: "Louvain", params: { iterations: 10, weighted: false } };
    assert.equal(h.findCommunityPartition("Leiden", { iterations: 10, weighted: false }), undefined);
    for (const params of [{ iterations: 11, weighted: false }, { iterations: "10", weighted: false },
        { iterations: 10, weighted: true }, { iterations: 10 }, { iterations: 10, other: false }])
        assert.equal(h.findCommunityPartition("Louvain", params), undefined);
    delete h.state.communityData.first;
    assert.equal(h.findCommunityPartition("Louvain", { iterations: 10, weighted: false }), undefined);
    assert.equal(h.findCommunityPartition("Louvain", {}), undefined, "legacy results without configuration metadata must not match");
});

test("Detect still stops a running job instead of selecting a cached configuration", async () => {
    const h = detectionHarness({});
    h.e.metadata.first = { algorithm: "Louvain", params: {} };
    h.e.partition = "second"; h.calls.starts = 1;
    await h.detectCommunity();
    assert.equal(h.calls.cancels, 1); assert.equal(h.e.partition, "second"); assert.equal(h.requests.length, 0);
});

test("network and metric navigation retain partition context and the inspector selection", () => {
    const h = setup(); h.selectCommunity("0");
    h.communityToNetwork(true);
    assert.equal(h.state.activeTab, "network");
    assert.equal(h.state.selection.node, "a"); assert.equal(h.state.selection.partition, "community:first");
    assert.equal(h.state.selection.mode, "community-only"); assert.equal(h.e.selected.first, "0");
    h.communityToMetrics();
    assert.equal(h.state.activeTab, "metrics"); assert.equal(h.state.subtab, "comm");
    assert.equal(h.$("indiv-comm-partition").value, "first"); assert.equal(h.$("global-comm-partition").value, "first");
});

test("table CSV exports all matching rows in sort order beyond the visible page", () => {
    const h = setup(); h.state.communityData.first = {};
    for (let index = 0; index < 123; index++) { const id = String(index); h.nodes.set(id, {}); h.state.communityData.first[id] = index; }
    h.e.sort = "id"; h.e.descending = false; h.e.page = 1;
    h.renderCommunityTable(); h.$("explorer-export-table").onclick();
    assert.equal(h.downloads[0][1].split("\n").length, 124);
    assert.match(h.downloads[0][1].split("\n")[123], /^122,Community 122,/);
    h.e.search = "Community 12"; h.$("explorer-export-table").onclick();
    assert.equal(h.downloads[1][1].split("\n").length, 5);
});

test("late community graph responses cannot overwrite a switched partition or reset network", async () => {
    const h = setup();
    const first = h.loadCommunityGraph();
    h.e.partition = "second"; const second = h.loadCommunityGraph();
    h.requests[1].resolve(response("second")); await second;
    h.requests[0].resolve(response("first")); await first;
    assert.equal(h.e.data.tag, "second"); assert.equal(h.requests[0].options.signal.aborted, true);
    const next = h.loadCommunityGraph(true);
    h.state.communityData = {}; h.resetCommunityExplorer();
    h.requests[2].resolve(response("stale")); await next;
    assert.equal(h.e.data, null);
});

test("Cosmograph 3D requests a spatial layout and renderer switches preserve selected community", async () => {
    const h = setup(); h.selectCommunity("1");
    h.$("explorer-renderer").value = "cosmograph"; h.$("explorer-dimensions").value = "3"; h.$("explorer-dimensions").onchange();
    assert.equal(h.requests[0].body.layout, "spherical-3d");
    assert.equal(h.e.selected.first, "1");
    h.e.layout = "fruchterman-reingold";
    assert.equal(h.communityLayoutId(), "fruchterman-reingold-3d");
    h.$("explorer-renderer").value = "sigma"; h.$("explorer-renderer").onchange();
    assert.equal(h.requests[1].body.layout, "fruchterman-reingold");
    assert.equal(h.e.selected.first, "1");
});

test("Network-style renderer and dimensions controls retain the Cosmograph preference through Sigma and sessions", () => {
    const h = setup();
    h.$("explorer-renderer").value = "cosmograph"; h.$("explorer-dimensions").value = "3";
    h.$("explorer-dimensions").onchange();
    assert.equal(h.e.rendererType, "cosmograph-3d"); assert.equal(h.$("explorer-dimensions-field").hidden, false);
    h.$("explorer-renderer").value = "sigma"; h.$("explorer-renderer").onchange();
    assert.equal(h.$("explorer-dimensions-field").hidden, true); assert.equal(h.e.cosmographDimensions, 3);
    const saved = plain(h.collectCommunityExplorer());
    h.resetCommunityExplorer(); h.restoreCommunityExplorer(saved); h.refreshCommunityExplorer();
    assert.equal(h.$("explorer-renderer").value, "sigma"); assert.equal(h.$("explorer-dimensions").value, "3");
    h.$("explorer-renderer").value = "cosmograph"; h.$("explorer-renderer").onchange();
    assert.equal(h.e.rendererType, "cosmograph-3d");
});

test("Network-style camera buttons control the independent community camera", () => {
    const h = setup(), view = renderedGraph(h);
    h.$("explorer-zoom-in").onclick(); h.$("explorer-zoom-out").onclick(); h.$("explorer-fit").onclick();
    assert.deepEqual(view.zooms, [1.25, 0.8]); assert.equal(view.fits(), 1);
    assert.equal(h.requests.length, 0);
});
