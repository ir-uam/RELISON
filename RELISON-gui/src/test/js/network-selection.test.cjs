/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
function loadBrowserHelper(file) {
    const source = fs.readFileSync(path.join(__dirname, "../../main/resources/public", file), "utf8");
    return vm.runInThisContext("(function(module) {\n" + source + "\nreturn module.exports;\n})")({ exports: {} });
}
const viewModel = loadBrowserHelper("network-view.js");
const filterBuilder = loadBrowserHelper("filter-builder.js");
const appSource = fs.readFileSync(path.join(__dirname, "../../main/resources/public/app.js"), "utf8");
const cosmographSource = fs.readFileSync(path.join(__dirname, "../../main/resources/public/cosmograph-spike.js"), "utf8");
function extract(source, name) {
    const start = source.indexOf("function " + name + "("), lineEnd = source.indexOf("\n", start);
    assert.ok(start >= 0, name);
    const end = source.slice(start, lineEnd).trimEnd().endsWith("}") ? lineEnd : source.indexOf("\n}", start) + 2;
    return (source.slice(start - 6, start) === "async " ? "async " : "") + source.slice(start, end);
}

function setup() {
    let document;
    const element = (tag = "div") => ({ tag, ownerDocument: document, value: "", textContent: "", hidden: false, open: false, disabled: false, children: [], listeners: {}, attributes: {},
        classList: { add() {} },
        focus() { document.activeElement = this; },
        showModal() { assert.equal(this.open, false); this.open = true; },
        close() { this.open = false; this.listeners.close?.(); },
        getBoundingClientRect: () => ({ left: 100, right: 900, top: 100, bottom: 700 }),
        querySelector() { return descendants(this).find(node => node.className === "filter-builder-field" || node.tag === "button" && node.textContent === "+ Condition") || null; },
        querySelectorAll: () => [], appendChild(child) { this.children.push(child); },
        append(...children) { this.children.push(...children); },
        setAttribute(name, value) { this.attributes[name] = value; }, addEventListener(name, callback) { this.listeners[name] = callback; },
        replaceChildren(...children) { this.children = children; this.textContent = ""; } });
    document = { createElement: element };
    const nodes = ["A", "B", "C", "D", "isolated", "01", "1", "a"];
    const links = new Map([
        ["ab", ["A", "B", "blue"]], ["ab2", ["A", "B", "red"]], ["ba", ["B", "A", "red"]],
        ["bc", ["B", "C", "blue"]], ["ac", ["A", "C", "red"]], ["cd", ["C", "D", "blue"]],
    ]);
    const graph = { type: "directed", multi: true, nodes: () => nodes, edges: () => [...links.keys()],
        source: edge => links.get(edge)[0], target: edge => links.get(edge)[1], hasNode: node => nodes.includes(node),
        hasEdge: edge => links.has(edge), forEachNode: cb => nodes.forEach(cb), forEachEdge: cb => [...links.keys()].forEach(cb),
        getNodeAttributes: () => ({ color: "node", size: 4 }), getEdgeAttributes: () => ({ color: "edge", size: 1 }),
        getNodeAttribute: (node, name) => name === "attrs" ? { group: ["A", "B"].includes(node) ? "first" : "second" } : undefined,
        getEdgeAttribute: (edge, name) => name === "attrs" ? { color: links.get(edge)[2] } : undefined,
        order: nodes.length, size: links.size };
    const inputs = new Map();
    const $ = id => {
        if (!inputs.has(id)) inputs.set(id, { ...element(), value: id === "select-path-scope" ? "filtered" : "" });
        return inputs.get(id);
    };
    const state = { graph, graphId: "graph", networkRevision: 0, directed: true, weighted: false,
        selection: { type: "node", node: null, mode: "highlight", partition: null, edgeAttribute: null },
        selectionModes: { node: "highlight", edge: "edge-highlight", path: "path-highlight" },
        selectedNode: null, selectedEdge: null, activePaths: [], lastPaths: [], pathRequestVersion: 0, pathEndpointFocusActive: false,
        pathFocus: null, pathEdgeStyles: new Map(), pathEdgeStyleSequence: 0, multiSelectedNodes: new Set(), timeline: {},
        pathAppearance: { enlargeNodes: false, nodeScale: 1.5, edgeScale: 1 }, pathEdgeHighlight: { mode: "default", color: "orange" },
        attrSchema: { node: [{ name: "group", type: "categorical" }], edge: [{ name: "color", type: "categorical" }] },
        metricOrder: [], pairOrder: [], communityData: {}, metricData: {}, pairData: {},
        tables: { nodes: { filters: {}, sort: { col: "id", dir: 1 }, page: 0 }, edges: { filters: {}, sort: { col: "source", dir: 1 }, page: 0 } } };
    const settings = {}, renders = [], requests = [], statuses = [], downloads = [];
    state.renderer = { setSetting: (name, value) => { settings[name] = value; }, refresh() {} };
    const window = { relisonNetworkView: viewModel, relisonFilterBuilder: filterBuilder };
    const operators = vm.runInNewContext(appSource.slice(appSource.indexOf("const OPERATORS"), appSource.indexOf("const UNARY_OPS")) + "\nOPERATORS");
    const context = vm.createContext({ state, window, $, console, document, OPERATORS: operators, TABLE_IDS: { nodes: "nodes-table", edges: "edges-table" },
        UNARY_OPS: new Set(["empty", "nonempty"]),
        updateSelectionTargetUI() {}, renderNodeInfo() {}, renderEdgeInfo() {}, clearEdgeSelection() { state.selectedEdge = null; state.edgeSelectionPair = null; },
        renderPathsTable() {}, renderTable: key => renders.push(key), drawRecOverlay() {},
        cssVar: () => "dim", usingGeographic: () => false, queueCosmographAppearanceRefresh() {}, syncCosmographSelection() {},
        download: (filename, content) => downloads.push({ filename, content }),
        buildCsv: (cols, ids, value) => JSON.stringify(Array.from(ids, id => Object.fromEntries(cols.map(col => [col.key, value(id, col.key)])))),
        requireGraph: () => true, jsonBody: body => ({ body }), setStatus: (...args) => statuses.push(args),
        api: (url, request) => new Promise((resolve, reject) => requests.push({ url, body: request.body, resolve, reject })) });
    const names = ["nodeAttrDefs", "edgeAttrDefs", "nodeAttrVal", "edgeAttrVal", "nodeColumns", "edgeColumns", "nodeRowValue", "edgeRowValue", "tableModel",
        "isValueEmpty", "isFilterActive", "activeFilters", "rowPasses", "matchFilter", "idValue", "compareValues", "pairKey", "groupingValue", "groupingKey",
        "networkColumnType", "networkWorkingSet", "networkView", "selectionFocus", "rememberSelectionType", "invalidatePathResults", "refreshNetworkView",
        "networkFilterColumns", "effectiveNetworkExpression", "networkExpressionPredicate", "networkExpressionText", "openNetworkFilterBuilder", "cancelNetworkFilterBuilder",
        "networkBuilderErrors", "updateFilterBuilderPreview", "refreshFilterBuilder", "applyNetworkFilterBuilder", "removeNetworkCustomFilter",
        "reconcileNetworkFilters", "setSelectionType", "selectNode", "selectEdge", "clearSelection", "setSelectionMode", "setColFilter", "clearFilters", "setNetworkFilterMode", "resetTableViews", "tableRows",
        "findPaths", "highlightPaths", "togglePathHighlight", "updateSelectionSummary", "applyReducers", "getPathEdgeStyle", "buildPathEdgeColors", "focusContext",
        "overlayNodeVerdict", "overlayEdgeVerdict", "exportTableCsv", "renderNetworkFilterSummary"];
    vm.runInContext(names.map(name => extract(appSource, name)).join("\n"), context);
    const handlers = vm.runInContext("({" + names.join(",") + "})", context);
    for (const key of ["nodes", "edges"]) $("filter-builder-" + key).appendChild($("filter-builder-editor-" + key));
    const bindingsStart = appSource.indexOf('for (const key of ["nodes", "edges"]) {\n    $("btn-filter-builder-"');
    assert.ok(bindingsStart >= 0);
    vm.runInContext(appSource.slice(bindingsStart, appSource.indexOf("\n}", bindingsStart) + 2), context);
    function project(dimensions = 2) {
        const spatial = new Map(nodes.map((node, index) => [node, { x: index + 10, y: index + 20, z: index + 30 }]));
        const cosmoContext = vm.createContext({ window, graph, spaceDimensions: dimensions, spatialPositions: () => spatial, linkStyleColumnRevision: 0 });
        return vm.runInContext(extract(cosmographSource, "projectGraph") + "\nprojectGraph(graph)", cosmoContext);
    }
    return { state, handlers, $, window, graph, settings, renders, requests, statuses, downloads, project, context };
}
const sorted = set => [...set].sort();

test("Cosmograph network filtering and path selection project consistently in 2D and 3D", () => {
    for (const dimensions of [2, 3]) {
        const h = setup();
        h.handlers.selectEdge("ab"); h.state.selection.edgeAttribute = "color";
        h.handlers.setSelectionMode("edge-attribute-only");
        const attributeView = h.project(dimensions);
        assert.deepEqual(sorted(attributeView.links.map(edge => edge.id)), ["ab", "bc", "cd"]);
        assert.equal(attributeView.points.find(point => point.id === "A").z, dimensions === 3 ? 30 : 0);

        h.handlers.setSelectionType("path"); h.handlers.highlightPaths([["A", "B"]]);
        h.handlers.setSelectionMode("path-only");
        assert.deepEqual(sorted(h.project(dimensions).links.map(edge => edge.id)), ["ab", "ab2"]);

    }
});

test("linked working sets combine node and edge filters and retain isolates without edge filters", () => {
    const h = setup();
    assert.ok(h.handlers.networkWorkingSet().nodes.has("isolated"));
    h.handlers.setColFilter("edges", "ea:color", "eq", "blue");
    assert.deepEqual(sorted(h.handlers.networkWorkingSet().edges), ["ab", "bc", "cd"]);
    assert.deepEqual(sorted(h.handlers.networkWorkingSet().nodes), ["A", "B", "C", "D"]);
    h.handlers.setColFilter("nodes", "id", "eq", "A");
    assert.equal(h.handlers.tableRows("nodes").ids.length, 0);
    assert.equal(h.handlers.tableRows("edges").ids.length, 0);
});

test("filter and selection order agree; None preserves filters and clear preserves type and mode", () => {
    const first = setup(), second = setup();
    first.handlers.setColFilter("edges", "ea:color", "eq", "blue"); first.handlers.selectNode("B");
    second.handlers.selectNode("B"); second.handlers.setColFilter("edges", "ea:color", "eq", "blue");
    assert.equal(second.state.selectedNode, "B");
    assert.deepEqual(sorted(first.handlers.networkView().focus.nodes), sorted(second.handlers.networkView().focus.nodes));
    second.handlers.setSelectionMode("none");
    assert.equal(second.settings.edgeReducer("ba", { color: "edge" }).color, "dim");
    second.handlers.selectEdge("ab"); second.handlers.setSelectionMode("edge-attribute-only");
    second.handlers.clearSelection();
    assert.equal(second.state.selection.type, "edge"); assert.equal(second.state.selection.mode, "edge-attribute-only");
    assert.equal(second.handlers.tableRows("edges").ids.length, 3);
});

test("attribute-only retains exact edge identity across tables, renderers and clearing filters", () => {
    const h = setup(); h.handlers.selectEdge("ab"); h.state.selection.edgeAttribute = "color";
    h.handlers.setSelectionMode("edge-attribute-only");
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab", "bc", "cd"]);
    assert.equal(h.settings.edgeReducer("ab2", { color: "edge" }).hidden, true);
    assert.deepEqual(sorted(h.project().links.map(edge => edge.id)), ["ab", "bc", "cd"]);
    h.handlers.clearFilters("nodes"); assert.equal(h.state.selectedEdge, "ab");
});

test("excluded targets retain inspection, empty isolation stays empty and highlight falls back to filters", () => {
    const h = setup(); h.handlers.selectNode("A"); h.handlers.setSelectionMode("ego-only");
    h.handlers.setColFilter("nodes", "id", "eq", "D");
    assert.equal(h.state.selectedNode, "A"); assert.equal(h.handlers.tableRows("nodes").ids.length, 0);
    assert.match(h.$("selection-notice").textContent, /Outside current filters/);
    h.handlers.setSelectionMode("ego-highlight");
    assert.deepEqual(Array.from(h.handlers.tableRows("nodes").ids), ["D"]);
    assert.equal(h.settings.nodeReducer("D", { color: "node", size: 4 }).color, "node");
});

test("mode preferences return per type and every selection refreshes both tables", () => {
    const h = setup(); h.handlers.selectNode("A"); h.handlers.setSelectionMode("ego-only");
    h.handlers.selectEdge("ab"); h.handlers.setSelectionMode("none"); h.handlers.selectNode("B");
    assert.equal(h.state.selection.mode, "ego-only"); h.handlers.selectEdge("bc"); assert.equal(h.state.selection.mode, "none");
    assert.deepEqual(h.renders.slice(-2), ["nodes", "edges"]);
});

test("path-only membership respects direction and eligible parallel edges, and None retains active paths", () => {
    const h = setup(); h.handlers.setSelectionType("path"); h.handlers.highlightPaths([["A", "B"]]); h.handlers.setSelectionMode("path-only");
    assert.deepEqual(sorted(h.handlers.tableRows("nodes").ids), ["A", "B"]);
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab", "ab2"]);
    assert.equal(h.settings.edgeReducer("ba", { color: "edge" }).hidden, true);
    assert.deepEqual(sorted(h.project().links.map(edge => edge.id)), ["ab", "ab2"]);
    h.handlers.setSelectionMode("none"); assert.equal(h.state.activePaths.length, 1);
    h.handlers.setSelectionMode("path-only"); assert.equal(h.handlers.tableRows("edges").ids.length, 2);
    h.handlers.togglePathHighlight(["A", "B"]); assert.equal(h.handlers.tableRows("edges").ids.length, 6);
});

test("whole-graph paths are clipped by filters with a partial-path explanation", () => {
    const h = setup(); h.$("select-path-scope").value = "whole";
    h.handlers.setColFilter("edges", "ea:color", "eq", "blue");
    h.handlers.setSelectionType("path"); h.handlers.highlightPaths([["B", "A", "C"]]); h.handlers.setSelectionMode("path-only");
    assert.equal(h.handlers.tableRows("edges").ids.length, 0);
    assert.match(h.$("selection-notice").textContent, /Showing a partial path/);
});

test("filtered requests include allowed links, while whole graph omits constraints", async () => {
    const h = setup(); h.handlers.setColFilter("edges", "ea:color", "eq", "blue"); h.handlers.setSelectionType("path");
    h.$("select-path-source").value = "A"; h.$("select-path-target").value = "D";
    let pending = h.handlers.findPaths();
    assert.deepEqual(JSON.parse(JSON.stringify(h.requests[0].body.allowedLinks)), [["A", "B"], ["B", "C"], ["C", "D"]]);
    h.requests[0].resolve({ paths: [["A", "B", "C", "D"]], count: 1, length: 3 }); await pending;
    assert.equal(h.state.activePaths.length, 1);
    h.$("select-path-scope").value = "whole"; pending = h.handlers.findPaths();
    assert.equal(h.requests[1].body.allowedLinks, undefined); h.requests[1].resolve({ paths: [], count: 0, length: -1 }); await pending;
});

test("late responses cannot resurrect cleared, filtered or changed endpoint searches", async () => {
    for (const change of [h => h.handlers.clearSelection(), h => h.handlers.setColFilter("nodes", "id", "eq", "B"),
        h => { h.$("select-path-target").value = "C"; h.handlers.invalidatePathResults(); }, h => h.handlers.setSelectionType("node")]) {
        const h = setup(); h.handlers.setSelectionType("path"); h.$("select-path-source").value = "A"; h.$("select-path-target").value = "D";
        const pending = h.handlers.findPaths(); change(h);
        h.requests[0].resolve({ paths: [["A", "B", "C", "D"]], count: 1, length: 3 }); await pending;
        assert.equal(h.state.lastPaths.length, 0); assert.equal(h.state.activePaths.length, 0); assert.equal(h.$("btn-select-paths").disabled, false);
    }
});

test("endpoint previews agree between Sigma and Cosmograph and overlays obey isolation", () => {
    const h = setup(); h.handlers.setSelectionType("path"); h.$("select-path-source").value = "A"; h.$("select-path-target").value = "B";
    h.state.pathEndpointFocusActive = true; h.handlers.applyReducers();
    assert.equal(h.settings.nodeReducer("C", { size: 4, color: "node" }).color, "dim");
    assert.equal(h.project().points.find(point => point.id === "C").color, "dim");
    h.handlers.highlightPaths([["A", "B"]]); h.handlers.setSelectionMode("path-only");
    assert.equal(h.handlers.overlayEdgeVerdict("A", "B", h.handlers.focusContext()), "hidden");
    assert.equal(h.window.relisonNetworkViewSnapshot.recommendationVerdict("A", "B"), "hidden");
});

test("typed equality preserves exact IDs and categorical strings and rejects invalid numeric ranges", () => {
    const h = setup(); h.handlers.setColFilter("nodes", "id", "eq", "1");
    assert.deepEqual(Array.from(h.handlers.tableRows("nodes").ids), ["1"]);
    h.handlers.setColFilter("nodes", "id", "eq", "a"); assert.deepEqual(Array.from(h.handlers.tableRows("nodes").ids), ["a"]);
    assert.equal(viewModel.match("01", { op: "eq", value: "1" }, "categorical"), false);
    assert.equal(viewModel.match(1, { op: "eq", value: "01" }, "number"), true);
    assert.equal(viewModel.match(false, { op: "eq", value: "FALSE" }, "bool"), true);
    assert.match(viewModel.filterError({ op: "between", value: "ten", value2: "20" }, "number"), /finite number/);
    assert.match(viewModel.filterError({ op: "between", value: "20", value2: "10" }, "number"), /lower bound/);
});

test("path membership cannot confuse IDs containing separators", () => {
    const edges = { first: ["a|b", "c"], second: ["a", "b|c"] };
    const graph = { nodes: () => ["a|b", "c", "a", "b|c"], source: e => edges[e][0], target: e => edges[e][1], forEachEdge: cb => Object.keys(edges).forEach(cb) };
    const working = viewModel.workingSet(graph, () => true, () => true, false, false);
    const view = viewModel.calculate(graph, working, { type: "path", mode: "path-only", paths: [["a|b", "c"]] }, { directed: true });
    assert.deepEqual([...view.tableEdges], ["first"]);
});

test("ego and group focus operate within eligible edges and missing group values never fall back", () => {
    const h = setup(); h.handlers.setColFilter("edges", "ea:color", "eq", "blue"); h.handlers.selectNode("A");
    h.handlers.setSelectionMode("ego-only");
    assert.deepEqual(sorted(h.handlers.tableRows("nodes").ids), ["A", "B"]);
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab"]);
    h.state.selection.partition = "attribute:group"; h.handlers.setSelectionMode("community-only");
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab"]);
    h.state.selection.partition = null; h.handlers.refreshNetworkView();
    assert.equal(h.handlers.tableRows("nodes").ids.length, 0);
    assert.match(h.$("selection-notice").textContent, /Choose a community partition/);
});

test("CSV exports the full effective table result regardless of pagination", () => {
    const h = setup(); h.handlers.selectEdge("ab"); h.state.selection.edgeAttribute = "color";
    h.handlers.setSelectionMode("edge-attribute-only"); h.state.tables.edges.page = 2; h.state.tables.edges.pageSize = 1;
    h.handlers.exportTableCsv("edges", "edges.csv");
    assert.deepEqual(h.downloads[0].content && JSON.parse(h.downloads[0].content).map(row => row.edgeid).sort(), ["ab", "bc", "cd"]);
    h.handlers.setSelectionType("path"); h.handlers.highlightPaths([["A", "B"]]); h.handlers.setSelectionMode("path-only");
    h.handlers.exportTableCsv("nodes", "nodes.csv");
    assert.deepEqual(JSON.parse(h.downloads[1].content).map(row => row.id).sort(), ["A", "B"]);
});

test("transient recommendations use the same dim and hide rules in Cosmograph and overlays", () => {
    const h = setup(); h.window.relisonCosmographRecommendation = { show: true, directed: true, color: "rec", edges: [{ source: "A", target: "D" }] };
    h.handlers.setColFilter("edges", "ea:color", "eq", "blue");
    assert.equal(h.project().links.find(edge => edge.recommendation).color, "dim");
    assert.equal(h.handlers.overlayEdgeVerdict("A", "D", h.handlers.focusContext()), "dim");
    h.handlers.selectEdge("ab"); h.state.selection.edgeAttribute = "color"; h.handlers.setSelectionMode("edge-attribute-only");
    assert.equal(h.project().links.some(edge => edge.recommendation), false);
    assert.equal(h.handlers.overlayEdgeVerdict("A", "D", h.handlers.focusContext()), "hidden");
});

test("recommended-only mode hides base edges in Sigma and both Cosmograph dimensions without filtering the network", () => {
    const h = setup(), model = { edges: [{ source: "A", target: "D" }] };
    h.state.rec = { active: "model", models: { model }, show: true, only: true };
    h.window.relisonCosmographRecommendation = { show: true, directed: true, color: "rec", edges: model.edges };
    h.handlers.applyReducers();
    assert.equal(h.settings.edgeReducer("ab", { color: "edge", size: 1 }).hidden, true);
    assert.ok([...h.window.relisonNetworkViewSnapshot.edgeVerdicts.values()].every(verdict => verdict === "hidden"));
    assert.equal(h.handlers.networkView().tableEdges.size, h.graph.edges().length);
    for (const dimensions of [2, 3]) {
        const projected = h.project(dimensions);
        assert.equal(projected.links.length, 1); assert.equal(projected.links[0].recommendation, true);
        assert.equal(projected.points.length, h.graph.nodes().length);
    }
    h.state.rec.only = false; h.handlers.applyReducers();
    assert.equal(!!h.settings.edgeReducer("ab", { color: "edge", size: 1 }).hidden, false);
    assert.equal(h.project().links.length, h.graph.edges().length + 1);
    h.state.rec.only = true; h.state.rec.show = false; h.window.relisonCosmographRecommendation.show = false; h.handlers.applyReducers();
    assert.equal(h.project().links.length, h.graph.edges().length);
});

test("removed fields drop their filters and missing or invalid numeric values never pass ranges", () => {
    const h = setup(); h.state.tables.nodes.filters["a:removed"] = { op: "eq", value: "x" };
    h.handlers.refreshNetworkView(); assert.equal(h.state.tables.nodes.filters["a:removed"], undefined);
    for (const value of [null, undefined, "", "text", NaN, Infinity]) assert.equal(viewModel.match(value, { op: "between", value: "0", value2: "10" }, "number"), false);
    assert.equal(viewModel.match(5, { op: "between", value: "text", value2: "10" }, "number"), false);
    assert.equal(viewModel.match(5, { op: "between", value: "", value2: "10" }, "number"), true);
});

test("hidden-column filter chips are visible from either table and can clear a constraint without deselecting", () => {
    const h = setup(); h.handlers.selectNode("A"); h.handlers.setColFilter("edges", "ea:color", "eq", "blue");
    h.state.tables.edges.hiddenColumns = ["ea:color"];
    h.handlers.renderNetworkFilterSummary("nodes", h.handlers.networkView());
    const chip = h.$("filters-nodes").children.find(child => child.className === "network-filter-chip");
    assert.match(chip.textContent, /Edges: color eq blue.*hidden column/);
    chip.listeners.click();
    assert.equal(h.state.tables.edges.filters["ea:color"], undefined); assert.equal(h.state.selectedNode, "A");
});

test("None clears native Cosmograph focus and excluded targets do not receive a second native highlight", async () => {
    const h = setup(), focused = [];
    h.window.relisonCosmograph = { setFocusedSelection: async (...args) => focused.push(args) };
    vm.runInContext(extract(appSource, "syncCosmographSelection"), h.context);
    h.handlers.selectNode("A"); assert.deepEqual(focused.at(-1), ["A", null]);
    h.handlers.setSelectionMode("none"); assert.deepEqual(focused.at(-1), [null, null]);
    h.handlers.setSelectionMode("highlight"); h.handlers.setColFilter("nodes", "id", "eq", "D");
    assert.deepEqual(focused.at(-1), [null, null]);
});

test("path colours preserve direction and unambiguous endpoint identity", () => {
    const h = setup(); h.state.pathEdgeHighlight.mode = "per-path";
    const paths = [["a|b", "c"], ["a", "b|c"], ["c", "a|b"]];
    paths.forEach((path, index) => h.state.pathEdgeStyles.set(JSON.stringify(path), { color: "color" + index, priority: index }));
    const colors = h.handlers.buildPathEdgeColors({ paths });
    assert.equal(colors.size, 3);
    assert.equal(colors.get(viewModel.pair("a|b", "c")), "color0");
    assert.equal(colors.get(viewModel.pair("a", "b|c")), "color1");
    assert.equal(colors.get(viewModel.pair("c", "a|b")), "color2");
});

test("edge OR matches either endpoint consistently in tables, Cosmograph, Sigma and CSV", () => {
    const h = setup();
    h.handlers.setColFilter("edges", "source", "eq", "A");
    h.handlers.setColFilter("edges", "target", "eq", "A");
    assert.equal(h.handlers.tableRows("edges").ids.length, 0);
    h.handlers.setNetworkFilterMode("edges", "or");
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab", "ab2", "ac", "ba"]);
    assert.deepEqual(sorted(h.handlers.tableRows("nodes").ids), ["A", "B", "C"]);
    assert.equal(h.project().links.find(edge => edge.id === "ba").color, "edge");
    assert.equal(h.project().links.find(edge => edge.id === "bc").color, "dim");
    assert.equal(h.settings.edgeReducer("ba", { color: "edge" }).color, "edge");
    assert.equal(h.settings.edgeReducer("bc", { color: "edge" }).color, "dim");
    h.handlers.exportTableCsv("edges", "edges.csv");
    assert.deepEqual(JSON.parse(h.downloads[0].content).map(row => row.edgeid).sort(), ["ab", "ab2", "ac", "ba"]);
});

test("node and edge groups choose OR independently while edges still require two eligible endpoints", () => {
    const h = setup();
    h.handlers.setColFilter("nodes", "id", "eq", "D");
    h.handlers.setColFilter("nodes", "a:group", "eq", "first");
    assert.equal(h.handlers.tableRows("nodes").ids.length, 0);
    h.handlers.setNetworkFilterMode("nodes", "or");
    assert.deepEqual(sorted(h.handlers.tableRows("nodes").ids), ["A", "B", "D"]);
    h.handlers.setColFilter("edges", "source", "eq", "A");
    h.handlers.setColFilter("edges", "target", "eq", "A");
    assert.equal(h.handlers.tableRows("edges").ids.length, 0);
    h.handlers.setNetworkFilterMode("edges", "or");
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab", "ab2", "ba"]);
    assert.deepEqual(sorted(h.handlers.tableRows("nodes").ids), ["A", "B"]);
    assert.equal(h.handlers.networkWorkingSet().edges.has("ac"), false);
});

test("changing combination preserves selection, resets paging and explains OR in the filter summary", () => {
    const h = setup(); h.handlers.selectEdge("ab");
    h.handlers.setColFilter("edges", "source", "eq", "A"); h.handlers.setColFilter("edges", "target", "eq", "A");
    h.state.tables.nodes.page = h.state.tables.edges.page = 2;
    h.handlers.setNetworkFilterMode("edges", "or");
    assert.equal(h.state.selectedEdge, "ab"); assert.equal(h.state.selection.mode, "edge-highlight");
    assert.equal(h.state.tables.nodes.page, 0); assert.equal(h.state.tables.edges.page, 0);
    assert.equal(h.$("filter-mode-edges").value, "or");
    h.handlers.renderNetworkFilterSummary("nodes", h.handlers.networkView());
    assert.match(h.$("filters-nodes").children.find(child => child.className === "network-filter-group").textContent, /Edge filters: match any \(OR\)/);
    h.handlers.setNetworkFilterMode("edges", "and");
    assert.equal(h.state.selectedEdge, "ab"); assert.equal(h.handlers.tableRows("edges").ids.length, 0);
});

test("OR with no or one active filter behaves predictably and clear preserves the mode", () => {
    const h = setup(); h.handlers.setNetworkFilterMode("nodes", "or"); h.handlers.setNetworkFilterMode("edges", "or");
    assert.equal(h.handlers.tableRows("nodes").ids.length, h.graph.order);
    assert.equal(h.handlers.tableRows("edges").ids.length, h.graph.size);
    h.handlers.setColFilter("edges", "ea:color", "eq", "blue");
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab", "bc", "cd"]);
    h.handlers.clearFilters("edges");
    assert.equal(h.state.tables.edges.filterMode, "or"); assert.equal(h.handlers.tableRows("edges").ids.length, h.graph.size);
    h.handlers.resetTableViews(); h.handlers.refreshNetworkView();
    assert.equal(h.state.tables.nodes.filterMode, "and"); assert.equal(h.state.tables.edges.filterMode, "and");
    assert.equal(h.$("filter-mode-nodes").value, "and"); assert.equal(h.$("filter-mode-edges").value, "and");
});

test("changing AND to OR invalidates pending filtered paths and new requests use the expanded graph", async () => {
    const h = setup();
    h.handlers.setColFilter("edges", "source", "eq", "A"); h.handlers.setColFilter("edges", "ea:color", "eq", "blue");
    h.handlers.setSelectionType("path"); h.$("select-path-source").value = "A"; h.$("select-path-target").value = "B";
    const first = h.handlers.findPaths();
    assert.deepEqual(JSON.parse(JSON.stringify(h.requests[0].body.allowedLinks)), [["A", "B"]]);
    h.handlers.setNetworkFilterMode("edges", "or");
    h.requests[0].resolve({ paths: [["A", "B"]], count: 1, length: 1 }); await first;
    assert.equal(h.state.lastPaths.length, 0); assert.equal(h.state.activePaths.length, 0);
    const second = h.handlers.findPaths();
    assert.deepEqual(JSON.parse(JSON.stringify(h.requests[1].body.allowedLinks)), [["A", "B"], ["A", "B"], ["B", "C"], ["A", "C"], ["C", "D"]]);
    h.requests[1].resolve({ paths: [["A", "B"]], count: 1, length: 1 }); await second;
    assert.equal(h.state.activePaths.length, 1);
});

const condition = (col, value, op = "eq", value2 = "") => ({ kind: "condition", col, op, value, value2 });
const group = (children, joins = []) => ({ kind: "group", children, joins });
const descendants = node => [node, ...(node.children || []).flatMap(descendants)];

test("mixed connectors use AND precedence and nested groups change the result explicitly", () => {
    const h = setup();
    h.handlers.openNetworkFilterBuilder("edges");
    h.state.tables.edges.filterDraft = group([condition("source", "A"), condition("target", "A"), condition("ea:color", "blue")], ["or", "and"]);
    h.handlers.applyNetworkFilterBuilder("edges");
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab", "ab2", "ac"]);
    h.state.tables.edges.filterDraft = group([group([condition("source", "A"), condition("target", "A")], ["or"]), condition("ea:color", "blue")], ["and"]);
    h.handlers.applyNetworkFilterBuilder("edges");
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab"]);
    assert.match(h.handlers.networkExpressionText("edges", h.state.tables.edges.filterExpression), /^\(\(source = "A" OR target = "A"\) AND color = "blue"\)$/);
});

test("builder supports repeated fields, previewed drafts, Apply and closing unapplied edits", () => {
    const h = setup(); h.handlers.selectNode("A");
    h.handlers.openNetworkFilterBuilder("nodes");
    const editor = h.$("filter-builder-editor-nodes");
    descendants(editor).find(node => node.textContent === "+ Condition").listeners.click();
    let input = descendants(editor).find(node => node.className === "filter-builder-value" && !node.hidden);
    input.value = "A"; const firstRow = input; input.listeners.input();
    assert.equal(descendants(editor).includes(firstRow), true, "typing preserves the focused control");
    descendants(editor).find(node => node.textContent === "+ Condition").listeners.click();
    input = descendants(editor).filter(node => node.className === "filter-builder-value" && !node.hidden)[1];
    input.value = "B"; input.listeners.input();
    const join = descendants(editor).find(node => node.className === "filter-builder-join");
    join.value = "or"; join.listeners.change();
    assert.match(h.$("filter-builder-preview-nodes").textContent, /2 nodes and 3 edges/);
    assert.equal(h.handlers.tableRows("nodes").ids.length, h.graph.order, "draft is not applied implicitly");
    h.handlers.applyNetworkFilterBuilder("nodes");
    assert.deepEqual(sorted(h.handlers.tableRows("nodes").ids), ["A", "B"]);
    assert.equal(h.state.selectedNode, "A");
    h.state.tables.nodes.filterDraft.children[0].value = "D";
    h.handlers.cancelNetworkFilterBuilder("nodes");
    assert.deepEqual(sorted(h.handlers.tableRows("nodes").ids), ["A", "B"], "applied expression is a separate copy");
    assert.equal(h.$("filter-builder-nodes").open, false);
});

test("builder imports column filters, consumes them on Apply and can regroup later column constraints", () => {
    const h = setup();
    h.handlers.setColFilter("edges", "source", "eq", "A"); h.handlers.setColFilter("edges", "target", "eq", "A");
    h.handlers.setNetworkFilterMode("edges", "or"); h.handlers.openNetworkFilterBuilder("edges");
    assert.match(h.$("filter-builder-expression-edges").textContent, /source = "A" OR target = "A"/);
    h.handlers.applyNetworkFilterBuilder("edges");
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab", "ab2", "ac", "ba"]);
    assert.equal(Object.keys(h.state.tables.edges.filters).length, 0);
    h.handlers.cancelNetworkFilterBuilder("edges");
    h.handlers.setColFilter("edges", "ea:color", "eq", "blue");
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab"]);
    h.handlers.openNetworkFilterBuilder("edges");
    assert.match(h.$("filter-builder-expression-edges").textContent, /\(source = "A" OR target = "A"\) AND \(color = "blue"\)/);
    h.handlers.applyNetworkFilterBuilder("edges");
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab"]);
    const appliedText = h.handlers.networkExpressionText("edges", h.state.tables.edges.filterExpression);
    h.handlers.openNetworkFilterBuilder("edges");
    assert.equal(h.$("filter-builder-expression-edges").textContent, appliedText, "reopening does not add redundant groups");
});

test("invalid and incomplete builder conditions block Apply and unsupported operators are excluded", () => {
    const h = setup(); h.state.attrSchema.node.push({ name: "age", type: "int", numeric: true });
    h.handlers.openNetworkFilterBuilder("nodes");
    for (const leaf of [condition("id", ""), condition("missing", "x"), condition("a:age", "not a number", "gt"), condition("a:age", "20", "between", "10"), condition("id", "2", "gt")]) {
        h.state.tables.nodes.filterDraft = group([leaf]); h.handlers.updateFilterBuilderPreview("nodes");
        assert.equal(h.$("filter-builder-apply-nodes").disabled, true);
        h.handlers.applyNetworkFilterBuilder("nodes");
        assert.equal(h.state.tables.nodes.filterExpression, undefined);
        assert.equal(h.handlers.tableRows("nodes").ids.length, h.graph.order);
    }
    h.state.tables.nodes.filterDraft = group([condition("a:age", "", "between", "30")]);
    assert.equal(h.handlers.networkBuilderErrors("nodes", h.state.tables.nodes.filterDraft).length, 0);
    h.state.tables.nodes.filterDraft = group([condition("a:age", "", "empty")]);
    assert.equal(h.handlers.networkBuilderErrors("nodes", h.state.tables.nodes.filterDraft).length, 0);
});

test("nested filters keep graph, CSV and endpoint constraints aligned", () => {
    const h = setup(); h.handlers.openNetworkFilterBuilder("edges");
    h.state.tables.edges.filterDraft = group([group([condition("source", "A"), condition("source", "B")], ["or"]), condition("ea:color", "blue")], ["and"]);
    h.handlers.applyNetworkFilterBuilder("edges");
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab", "bc"]);
    assert.equal(h.settings.edgeReducer("ac", { color: "edge" }).color, "dim");
    assert.equal(h.project().links.find(edge => edge.id === "bc").color, "edge");
    h.handlers.exportTableCsv("edges", "edges.csv");
    assert.deepEqual(JSON.parse(h.downloads[0].content).map(row => row.edgeid).sort(), ["ab", "bc"]);
    h.handlers.setColFilter("nodes", "a:group", "eq", "first");
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab"]);
});

test("applying a builder invalidates old filtered searches and subsequent requests use its expression", async () => {
    const h = setup(); h.handlers.setSelectionType("path"); h.$("select-path-source").value = "A"; h.$("select-path-target").value = "B";
    const first = h.handlers.findPaths();
    h.handlers.openNetworkFilterBuilder("edges");
    h.state.tables.edges.filterDraft = group([condition("ea:color", "blue"), condition("source", "B")], ["or"]);
    h.handlers.applyNetworkFilterBuilder("edges");
    h.requests[0].resolve({ paths: [["A", "B"]], count: 1, length: 1 }); await first;
    assert.equal(h.state.lastPaths.length, 0);
    const second = h.handlers.findPaths();
    assert.deepEqual(JSON.parse(JSON.stringify(h.requests[1].body.allowedLinks)), [["A", "B"], ["B", "A"], ["B", "C"], ["C", "D"]]);
    h.requests[1].resolve({ paths: [["A", "B"]], count: 1, length: 1 }); await second;
    assert.equal(h.state.activePaths.length, 1);
});

test("removing conditions and groups through the builder preserves remaining connectors", () => {
    const h = setup(); h.handlers.openNetworkFilterBuilder("edges");
    h.state.tables.edges.filterDraft = group([condition("source", "A"), condition("ea:color", "red"), condition("target", "B")], ["or", "and"]);
    h.state.tables.edges.builderSchemaSignature = null; h.handlers.refreshFilterBuilder("edges");
    descendants(h.$("filter-builder-editor-edges")).filter(node => node.attributes?.["aria-label"] === "Remove filter condition")[1].listeners.click();
    assert.deepEqual(Array.from(h.state.tables.edges.filterDraft.joins), ["and"]);
    h.handlers.applyNetworkFilterBuilder("edges");
    assert.deepEqual(sorted(h.handlers.tableRows("edges").ids), ["ab", "ab2"]);
    const addGroup = descendants(h.$("filter-builder-editor-edges")).find(node => node.textContent === "+ Group");
    addGroup.listeners.click();
    assert.equal(h.state.tables.edges.filterDraft.children.at(-1).kind, "group");
    descendants(h.$("filter-builder-editor-edges")).find(node => node.attributes?.["aria-label"] === "Remove filter group").listeners.click();
    assert.equal(h.state.tables.edges.filterDraft.children.length, 2);
});

test("removed fields prune applied expression leaves and Clear removes expressions and drafts", () => {
    const h = setup(); h.handlers.openNetworkFilterBuilder("edges");
    h.state.tables.edges.filterDraft = group([condition("source", "A"), condition("ea:color", "blue"), condition("target", "B")], ["or", "and"]);
    h.handlers.applyNetworkFilterBuilder("edges");
    h.state.attrSchema.edge = []; h.handlers.refreshNetworkView();
    assert.equal(viewModel.expressionConditions(h.state.tables.edges.filterExpression).length, 2);
    assert.match(h.handlers.networkExpressionText("edges", h.state.tables.edges.filterExpression), /source = "A" AND target = "B"/);
    assert.equal(h.$("filter-builder-apply-edges").disabled, true, "draft still explains its missing field");
    h.handlers.clearFilters("edges");
    assert.equal(h.state.tables.edges.filterExpression, null); assert.equal(h.state.tables.edges.filterDraft, null);
    assert.equal(h.handlers.tableRows("edges").ids.length, h.graph.size);
});

test("custom filters can be removed from either summary without clearing other filters or selection", () => {
    for (const key of ["nodes", "edges"]) {
        const h = setup(), other = key === "nodes" ? "edges" : "nodes";
        h.state.tables.nodes.filterExpression = group([condition("a:group", "first")]);
        h.state.tables.edges.filterExpression = group([condition("source", "C")]);
        h.handlers.setColFilter("edges", "ea:color", "eq", "blue");
        h.handlers.setNetworkFilterMode("edges", "or");
        h.handlers.selectNode("A");
        h.handlers.openNetworkFilterBuilder(key);
        const columns = h.state.tables.edges.filters, otherExpression = h.state.tables[other].filterExpression;
        h.state.tables.nodes.page = h.state.tables.edges.page = 5;
        h.handlers.renderNetworkFilterSummary(other, h.handlers.networkView());
        const button = descendants(h.$("filters-" + other)).find(node => node.attributes?.["aria-label"] ===
            "Remove " + (key === "nodes" ? "node" : "edge") + " custom filter");
        assert.equal(button.textContent, "Remove custom filter");
        button.listeners.click();
        assert.equal(h.state.tables[key].filterExpression, null);
        assert.deepEqual(h.state.tables[other].filterExpression, otherExpression);
        assert.equal(h.state.tables.edges.filters, columns);
        assert.equal(h.state.tables.edges.filterMode, "or");
        assert.equal(h.state.selectedNode, "A"); assert.equal(h.state.selection.mode, "highlight");
        assert.equal(h.state.tables.nodes.page, 0); assert.equal(h.state.tables.edges.page, 0);
        assert.equal(h.state.tables[key].filterDraft, null); assert.equal(h.$("filter-builder-" + key).open, false);
        h.handlers.applyNetworkFilterBuilder(key);
        assert.equal(h.state.tables[key].filterExpression, null, "discarded drafts cannot restore the removed expression");
        assert.deepEqual(sorted(h.handlers.networkWorkingSet().edges), key === "nodes" ? ["cd"] : ["ab"]);
        h.handlers.renderNetworkFilterSummary(other, h.handlers.networkView());
        assert.equal(descendants(h.$("filters-" + other)).filter(node => node.textContent === "Remove custom filter").length, 1);
        assert.deepEqual(h.renders.slice(-2), ["nodes", "edges"]);
    }
});

test("removing a custom filter invalidates filtered paths but preserves whole-graph paths", async () => {
    for (const scope of ["filtered", "whole"]) {
        const h = setup(); h.$("select-path-scope").value = scope;
        h.state.tables.edges.filterExpression = group([condition("source", "A")]);
        h.handlers.setSelectionType("path"); h.$("select-path-source").value = "A"; h.$("select-path-target").value = "B";
        const pending = h.handlers.findPaths();
        h.handlers.removeNetworkCustomFilter("edges");
        h.requests[0].resolve({ paths: [["A", "B"]], count: 1, length: 1 }); await pending;
        assert.equal(h.state.activePaths.length, scope === "filtered" ? 0 : 1);
        assert.equal(h.state.lastPaths.length, scope === "filtered" ? 0 : 1);
        if (scope === "filtered") {
            const next = h.handlers.findPaths();
            assert.equal(h.requests[1].body.allowedLinks.length, h.graph.size);
            h.requests[1].resolve({ paths: [["A", "B"]], count: 1, length: 1 }); await next;
            assert.equal(h.state.activePaths.length, 1);
        }
    }
});

test("empty groups are neutral and field renames preserve nested connectors", () => {
    const expression = group([group([]), condition("id", "A"), group([condition("a:old", "first"), condition("id", "B")], ["or"])], ["or", "and"]);
    const renamed = viewModel.mapExpression(expression, leaf => leaf.col === "a:old" ? { ...leaf, col: "a:new" } : leaf);
    assert.deepEqual(renamed.joins, ["and"]);
    assert.deepEqual(renamed.children[1].joins, ["or"]);
    assert.equal(renamed.children[1].children[0].col, "a:new");
    assert.equal(viewModel.expressionMatch(group([group([])], []), () => null, () => "id", () => true), true);
    assert.equal(viewModel.expressionMatch(renamed, col => col === "id" ? "A" : "first", () => "string", () => true), true);
});

test("overlay opening focuses the editor, permits one builder and Escape discards only the draft", () => {
    const h = setup(); h.handlers.setColFilter("nodes", "id", "eq", "A");
    h.$("btn-filter-builder-nodes").listeners.click();
    assert.equal(h.$("filter-builder-nodes").open, true);
    assert.equal(h.context.document.activeElement.className, "filter-builder-field");
    h.state.tables.nodes.filterDraft.children[0].value = "B";
    h.handlers.openNetworkFilterBuilder("edges");
    assert.equal(h.$("filter-builder-nodes").open, false); assert.equal(h.$("filter-builder-edges").open, true);
    assert.equal(h.state.tables.nodes.filterDraft, null);
    assert.deepEqual(sorted(h.handlers.tableRows("nodes").ids), ["A"]);
    let prevented = false;
    h.$("filter-builder-edges").listeners.cancel({ preventDefault() { prevented = true; } });
    assert.equal(prevented, true); assert.equal(h.$("filter-builder-edges").open, false);
    assert.equal(h.state.tables.edges.filterDraft, null);
    assert.equal(h.context.document.activeElement, h.$("btn-filter-builder-edges"));
    assert.equal(h.$("btn-filter-builder-edges").attributes["aria-expanded"], "false");
});

test("overlay backdrop dismisses drafts while interior clicks and applied filters are preserved", () => {
    const h = setup(); h.handlers.openNetworkFilterBuilder("edges");
    const dialog = h.$("filter-builder-edges");
    h.state.tables.edges.filterDraft = group([condition("source", "A"), condition("target", "A")], ["or"]);
    dialog.listeners.click({ target: dialog, clientX: 150, clientY: 150 });
    assert.equal(dialog.open, true);
    h.$("filter-builder-apply-edges").listeners.click();
    assert.equal(dialog.open, true); assert.equal(h.handlers.tableRows("edges").ids.length, 4);
    h.state.tables.edges.filterDraft.children[0].value = "D";
    dialog.listeners.click({ target: dialog, clientX: 50, clientY: 50 });
    assert.equal(dialog.open, false); assert.equal(h.handlers.tableRows("edges").ids.length, 4);
    h.handlers.openNetworkFilterBuilder("edges");
    dialog.listeners.close();
    assert.equal(dialog.open, true, "a delayed close event cannot close a reopened overlay");
    h.$("filter-builder-dismiss-edges").listeners.click();
    assert.equal(dialog.open, false); assert.equal(h.handlers.tableRows("edges").ids.length, 4);
});
