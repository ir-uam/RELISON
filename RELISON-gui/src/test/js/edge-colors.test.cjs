/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const publicPath = path.join(__dirname, "../../main/resources/public");
const source = fs.readFileSync(path.join(publicPath, "app.js"), "utf8");
const cosmograph = fs.readFileSync(path.join(publicPath, "cosmograph-spike.js"), "utf8");
function extract(source, name) {
    const start = source.indexOf("function " + name + "(");
    assert.ok(start >= 0, name);
    return (source.slice(start - 6, start) === "async " ? "async " : "") + source.slice(start, source.indexOf("\n}", start) + 2);
}
function setup(dimensions = 2) {
    const nodes = new Map([["a", { color: "#ff0000", size: 4 }], ["b", { color: "#0000ff", size: 4 }]]);
    const edges = new Map([["ab", { source: "a", target: "b", color: "#888888" }],
        ["parallel", { source: "a", target: "b", color: "#888888" }], ["loop", { source: "a", target: "a", color: "#888888" }]]);
    const graph = { type: "directed", nodes: () => [...nodes.keys()], edges: () => [...edges.keys()],
        source: id => edges.get(id).source, target: id => edges.get(id).target,
        getNodeAttribute: (id, key) => nodes.get(id)[key], getNodeAttributes: id => nodes.get(id),
        getEdgeAttributes: id => edges.get(id), setEdgeAttribute: (id, key, value) => { edges.get(id)[key] = value; } };
    const controls = { "edge-size-by": { value: "" }, "edge-color-mode": { value: "endpoints" },
        "edge-color-single": { value: "#123456" }, "edge-color-by": { value: "" } };
    const uploads = [];
    const window = { relisonNetworkViewSnapshot: { dimColor: "#444444", nodeVerdicts: new Map([["a", "normal"], ["b", "normal"]]),
        edgeVerdicts: new Map([...edges.keys()].map(id => [id, "normal"])) } };
    const pointPositions = new Map([...nodes.keys()].map((id, index) => [id, { x: index + 10, y: index + 20, z: index + 30 }]));
    const context = vm.createContext({ state: { graph, pairData: {} }, graph, window, spaceDimensions: dimensions,
        spatialPositions: () => pointPositions,
        $: id => controls[id], numInput: () => 1, edgeAttrDefs: () => [], edgeAttrVal: () => undefined,
        linkStyleColumnRevision: 0,
        prepareCosmographData: async (config, points, links) => {
            uploads.push({ config, points, links });
            return { points, links, cosmographConfig: { linkColorBy: config.links.linkColorBy } };
        } });
    vm.runInContext(["applyEdgeAppearance", "averageColor", "colorToRgb", "hslToRgb"].map(name => extract(source, name)).join("\n") +
        "\n" + ["projectGraph", "prepareGraph"].map(name => extract(cosmograph, name)).join("\n"), context);
    return { nodes, edges, controls, uploads, window, context,
        handlers: vm.runInContext("({applyEdgeAppearance,averageColor,colorToRgb,prepareGraph})", context) };
}

test("endpoint averages accept imported RGBA and alpha hex colours without a grey fallback", () => {
    const { handlers } = setup();
    assert.equal(handlers.averageColor("rgba(255,0,0,0.5)", "rgba(0,0,255,1)"), "rgb(128,0,128)");
    assert.equal(handlers.averageColor("#f008", "#0000ff80"), "rgb(128,0,128)");
    assert.equal(handlers.averageColor("rgb(100% 0% 0% / 50%)", "rgb(0%,0%,100%)"), "rgb(128,0,128)");
    assert.equal(handlers.averageColor(" rgb(255.0, 0.0, 0.0) ", "#00f"), "rgb(128,0,128)");
    assert.equal(handlers.colorToRgb("#invalid"), null);
});

test("changing endpoint colours recalculates parallel edges and self-loops", () => {
    const h = setup(); h.handlers.applyEdgeAppearance();
    assert.equal(h.edges.get("ab").color, "rgb(128,0,128)");
    assert.equal(h.edges.get("parallel").color, "rgb(128,0,128)");
    assert.equal(h.edges.get("loop").color, "rgb(255,0,0)");
    h.nodes.get("a").color = "rgba(0,255,0,0.5)"; h.handlers.applyEdgeAppearance();
    assert.equal(h.edges.get("ab").color, "rgb(0,128,128)");
    assert.equal(h.edges.get("loop").color, "rgb(0,255,0)");
});

for (const dimensions of [2, 3]) test(`Cosmograph uploads current endpoint averages in ${dimensions}D`, async () => {
    const h = setup(dimensions);
    h.controls["edge-color-mode"].value = "single"; h.handlers.applyEdgeAppearance();
    await h.handlers.prepareGraph(h.context.graph);
    h.controls["edge-color-mode"].value = "endpoints"; h.handlers.applyEdgeAppearance();
    await h.handlers.prepareGraph(h.context.graph);
    h.nodes.get("a").color = "#00ff00"; h.handlers.applyEdgeAppearance();
    await h.handlers.prepareGraph(h.context.graph);
    assert.deepEqual(h.uploads.map(upload => upload.links.find(link => link.id === "ab")[upload.config.links.linkColorBy]),
        ["#123456", "rgb(128,0,128)", "rgb(0,128,128)"]);
    assert.equal(h.uploads[0].points[0].z, dimensions === 3 ? 30 : 0);
    assert.equal(h.uploads[0].points[0].x, dimensions === 3 ? 10 : 0);
    // Reusing the mapping column can leave the native renderer's cached colour buffer unchanged.
    assert.equal(new Set(h.uploads.map(upload => upload.config.links.linkColorBy)).size, 3);
});

for (const dimensions of [2, 3]) test(`Cosmograph colour uploads preserve filter dimming and path overrides in ${dimensions}D`, async () => {
    const h = setup(dimensions); h.handlers.applyEdgeAppearance();
    h.window.relisonNetworkViewSnapshot.edgeVerdicts.set("parallel", "dim");
    h.window.relisonCosmographTableFocus = { dimColor: "#444444" };
    h.window.relisonCosmographPath = { nodes: new Set(["a", "b"]), edgeIds: new Set(["ab"]), edges: new Set(), edgeColor: "#ffaa00" };
    await h.handlers.prepareGraph(h.context.graph);
    const upload = h.uploads[0];
    assert.equal(upload.links.find(link => link.id === "ab")[upload.config.links.linkColorBy], "#ffaa00");
    assert.equal(upload.links.find(link => link.id === "parallel")[upload.config.links.linkColorBy], "#444444");
    assert.equal(h.edges.get("ab").color, "rgb(128,0,128)");
});
