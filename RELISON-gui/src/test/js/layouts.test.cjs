/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

// Exercise the actual frontend handlers with a small DOM/graph boundary.
const source = fs.readFileSync(path.join(__dirname, "../../main/resources/public/app.js"), "utf8");
const section = source.slice(source.indexOf("const ITERATIVE_LAYOUTS"), source.indexOf("/* ------------------------------ catalog"));

function setup() {
    const inputs = new Map();
    const defaults = { "layout-type": "grid", "layout-radius": "100", "layout-spacing": "50", "layout-columns": "0",
        "layout-width": "600", "layout-height": "600", "layout-seed": "42", "layout-score": "degree", "circlepack-group": "", "layout-z-scale": "50",
        "layout-iterations": "10", "layout-theta": "0.8", "layout-tolerance": "0.001",
        "layout-ideal-length": "50", "layout-cooling": "0.95", "layout-fa-scaling": "100", "layout-fa-gravity": "1",
        "layout-jitter": "1", "layout-weight-influence": "0", "layout-node-radius": "5", "layout-overlap-gap": "5",
        "layout-root": "", "layout-direction": "UND", "layout-level-spacing": "75", "layout-column-spacing": "150",
        "layout-sweeps": "4", "layout-packing-gap": "50" };
    const $ = id => {
        if (!inputs.has(id)) inputs.set(id, { value: defaults[id] ?? "", textContent: "", disabled: false, hidden: false,
            dataset: {}, classList: { add() {}, remove() {}, toggle() {}, contains() { return false; } },
            checked: false, selectedOptions: [{ textContent: id === "layout-type" ? "Grid" : id }],
            replaceChildren(...children) { this.options = children; },
            appendChild(child) { (this.options ||= []).push(child); },
            closest: () => ({ dataset: {}, querySelector: () => ({ textContent: id }) }) });
        return inputs.get(id);
    };
    const data = new Map([["b", { x: 1, y: 2 }], ["a", { x: 3, y: 4 }]]);
    const graph = { nodes: () => [...data.keys()], hasNode: id => data.has(id), get order() { return data.size; },
        getNodeAttribute: (id, name) => data.get(id)[name], setNodeAttribute: (id, name, value) => { data.get(id)[name] = value; } };
    const state = { graph, graphId: "network", layoutRequest: null, savedLayoutPositions: null,
        iterativeLayoutRunning: false, cosmographLayoutRunning: false, communityData: {}, metricData: {},
        renderer: { refresh() {}, setCustomBBox() {} } };
    const requests = [], statuses = [];
    const context = vm.createContext({ state, $, AbortController, console, window: {},
        requestAnimationFrame: callback => setImmediate(callback), cancelAnimationFrame: id => clearImmediate(id), drawRecOverlay() {}, queueCosmographAppearanceRefresh() {},
        setStatus: (...args) => statuses.push(args), isAbort: error => error?.name === "AbortError",
        jsonBody: body => ({ method: "POST", body: JSON.stringify(body) }),
        api: async (url, options) => {
            requests.push({ url, options, body: JSON.parse(options.body) });
            return { positions: { a: { x: 10, y: 20 }, b: { x: 30, y: 40 } } };
        }, toggleHidden: (id, hidden) => { $(id).hidden = hidden; },
        nodeAttrVal: (node, name) => data.get(node).attrs?.[name],
    });
    const handlers = vm.runInContext(section + "\n({applyStaticLayout, startRelisonAnimation, onLayoutButton, updateLayoutButton, staticLayoutParams, saveLayoutPositions, updateLayoutTypeUI, rebuildLayoutGroupOptions, stopLayout, resetLayout, layeredLayoutInputs})", context);
    return { handlers, context, state, graph, data, $, requests, statuses };
}

test("grid metric ordering reaches layout requests and leaves default behaviour intact", async () => {
    const h = setup(); h.state.metricData.score = { a: 8, b: 2 };
    h.$("layout-grid-order").value = "metric:score";
    await h.handlers.applyStaticLayout("grid");
    assert.deepEqual(h.requests.at(-1).body.nodeOrder, ["b", "a"]);
    h.$("layout-grid-order-descending").checked = true;
    await h.handlers.applyStaticLayout("grid");
    assert.deepEqual(h.requests.at(-1).body.nodeOrder, ["a", "b"]);
    h.$("layout-grid-order").value = "";
    await h.handlers.applyStaticLayout("grid");
    assert.deepEqual(h.requests.at(-1).body.nodeOrder, ["a", "b"]);
});

test("grid metric feature ordering overrides sweeps only while selected", async () => {
    const h = setup(); h.state.metricData.score = { a: 8, b: 2 };
    h.data.get("a").attrs = { group: "same" }; h.data.get("b").attrs = { group: "same" };
    h.$("circlepack-group").value = "attr:group"; h.$("layout-grid-order").value = "metric:score";
    await h.handlers.applyStaticLayout("feature-grid");
    assert.deepEqual(h.requests.at(-1).body.nodeOrder, ["b", "a"]);
    assert.deepEqual(h.requests.at(-1).body.partitions, [["b", "a"]]);
    assert.equal(h.requests.at(-1).body.params.sweeps, 0);
    h.$("layout-grid-order").value = "";
    assert.equal(h.handlers.staticLayoutParams("feature-grid").sweeps, 4);
});

test("grid metric ego order preserves the automatic root", () => {
    const h = setup(); h.$("layout-grid-order").value = "metric:score";
    assert.equal(h.handlers.staticLayoutParams("ego-grid").root, "a");
    h.$("layout-root").value = "b";
    assert.equal(h.handlers.staticLayoutParams("ego-grid").root, "b");
    h.$("layout-root").value = "";
    assert.equal(h.handlers.staticLayoutParams("radial-pyramid-3d").root, "a");
    h.$("layout-type").value = "radial-pyramid-3d";
    h.handlers.updateLayoutTypeUI();
    assert.equal(h.$("layout-grid-order-field").hidden, false);
});

test("paper-defined 3D controls send distinct model settings", () => {
    const h = setup();
    h.$("layout-smds-rate").value = "0.1"; h.$("layout-smds-tolerance").value = "0.0000001";
    h.$("layout-h3-leaf").value = "0.1"; h.$("layout-h3-area").value = "7.2";
    const spherical = h.handlers.staticLayoutParams("spherical-mds-3d");
    assert.equal(spherical.radius,100);assert.equal(spherical.learningRate,.1);assert.equal(spherical.tolerance,1e-7);
    const upward = h.handlers.staticLayoutParams("upward-grid-3d");assert.equal(upward.spacing,50);
    const h3 = h.handlers.staticLayoutParams("h3-3d");assert.equal(h3.areaScale,7.2);assert.equal(h3.leafRadius,.1);assert.equal(h3.direction,"UND");
    h.$("layout-type").value = "spherical-mds-3d";h.handlers.updateLayoutTypeUI();
    assert.equal(h.$("layout-smds-controls").hidden,false);assert.equal(h.$("layout-h3-controls").hidden,true);assert.equal(h.$("layout-radius-field").hidden,false);
});
test("spectral controls send the same eigenproblem in two or three dimensions", () => {
    const h=setup();h.$("layout-spectral-scale").value="75";h.$("layout-spectral-iterations").value="1000";
    h.$("layout-spectral-tolerance").value="1e-8";h.$("layout-spectral-model").value="normalized";
    const two=h.handlers.staticLayoutParams("spectral"),three=h.handlers.staticLayoutParams("spectral-3d");
    assert.deepEqual(two,three);assert.equal(two.normalized,true);assert.equal(two.scale,75);assert.equal(two.iterations,1000);
    h.$("layout-spectral-model").value="classical";h.$("layout-spectral-weighted").checked=true;
    assert.equal(h.handlers.staticLayoutParams("spectral").normalized,false);assert.equal(h.handlers.staticLayoutParams("spectral").weighted,true);
    h.$("layout-type").value="spectral-3d";h.handlers.updateLayoutTypeUI();assert.equal(h.$("layout-spectral-controls").hidden,false);
});
test("sphere packing accepts no grouping while other grouped 3D layouts still require one", () => {
    const h=setup(),body={nodeOrder:["a","b"]};
    h.handlers.layeredLayoutInputs(body,"sphere-packing-3d");assert.equal(JSON.stringify(body.partitions),JSON.stringify([["a","b"]]));
    assert.throws(()=>h.handlers.layeredLayoutInputs({nodeOrder:["a","b"]},"feature-grid-3d"),/Choose a community/);
    h.$("circlepack-group").value="attr:group";h.data.get("a").attrs={group:"A"};h.data.get("b").attrs={group:"B"};
    const grouped={nodeOrder:["a","b"]};h.handlers.layeredLayoutInputs(grouped,"sphere-packing-3d");assert.equal(grouped.partitions.length,2);
});
test("Grid 3D exposes column and depth counts with automatic defaults", () => {
    const h=setup();h.$("layout-grid-depth").value="0";
    let p=h.handlers.staticLayoutParams("grid-3d");assert.equal(p.columns,0);assert.equal(p.depth,0);
    h.$("layout-columns").value="4";h.$("layout-grid-depth").value="2";
    p=h.handlers.staticLayoutParams("grid-3d");assert.equal(p.columns,4);assert.equal(p.depth,2);assert.equal(p.spacing,50);
    h.$("layout-type").value="grid-3d";h.handlers.updateLayoutTypeUI();assert.equal(h.$("layout-columns-field").hidden,false);assert.equal(h.$("layout-grid-depth-field").hidden,false);
    h.$("layout-type").value="grid";h.handlers.updateLayoutTypeUI();assert.equal(h.$("layout-grid-depth-field").hidden,true);
    h.$("layout-grid-depth").value="1.5";assert.throws(()=>h.handlers.staticLayoutParams("grid-3d"),/invalid/);
});

test("all ten selector layouts call the RELISON endpoint and apply returned coordinates", async () => {
    for (const type of ["circular", "random", "grid", "shell", "concentric", "preset", "radial", "bipartite", "multipartite", "tree"]) {
        const h = setup();
        if (type === "preset") h.handlers.saveLayoutPositions();
        if (type === "multipartite") {
            h.$("circlepack-group").value = "community:groups";
            h.state.communityData.groups = { a: 0, b: 1 };
        }
        await h.handlers.applyStaticLayout(type);
        assert.equal(h.requests.length, 1, type);
        assert.equal(h.requests[0].url, "/api/layout");
        assert.equal(h.requests[0].body.algorithm, type);
        assert.deepEqual(h.requests[0].body.nodeOrder, ["a", "b"]);
        assert.equal(h.data.get("a").x, 10);
        assert.equal(h.state.layoutRequest, null);
        assert.equal(h.$("btn-layout").disabled, false);
    }
});

test("flat 2D layout variants stay in their existing categories and are labelled with a 2D suffix", () => {
    const groups = new Map();
    const options = [];
    const makeOption = (value, textContent, parentElement = null) => ({ value, textContent, parentElement });
    for (const base of ["fruchterman-reingold", "stress-majorization", "circular", "shell", "grid", "feature-grid", "tree", "community"]) {
        const group = groups.get(base) || { options: [], append(option) { option.parentElement = this; this.options.push(option); options.push(option); } };
        groups.set(base, group);
        const option = makeOption(base, base, group); group.options.push(option); options.push(option);
    }
    const select = { querySelector(selector) {
        const match = selector.match(/option\[value="([^"]+)"\]/);
        return match ? options.find(option => option.value === match[1]) || null : null;
    } };
    const context = vm.createContext({ $, document: { createElement: () => ({}) } });
    function $(id) { assert.equal(id, "layout-type"); return select; }
    const handler = vm.runInContext(section + "\nensureFlat3DLayoutOptions", context);
    handler();
    for (const base of ["fruchterman-reingold", "stress-majorization", "circular", "shell", "grid", "feature-grid", "tree", "community"]) {
        const sourceOption = options.find(option => option.value === base);
        const variant = options.find(option => option.value === base + "-flat-3d");
        assert.ok(variant, base);
        assert.equal(variant.parentElement, sourceOption.parentElement, base);
        assert.equal(variant.textContent, sourceOption.textContent + " 2D", base);
    }
});

test("3D static layouts update Cosmograph without replacing Sigma coordinates", async () => {
    const h = setup(), frames = [];
    h.$("network-renderer").value = "cosmograph";
    h.$("layout-depth").value = "400";
    h.$("layout-pack-components").checked = true;
    h.context.window.relisonCosmograph = {
        getDimensions: () => 3, setNodePositions: async positions => { frames.push(positions); return true; }, fitView() {},
    };
    h.context.api = async (url, options) => {
        h.requests.push({ url, body: JSON.parse(options.body) });
        return { dimensions: 3, positions: { a: { x: 100, y: 200, z: 300 }, b: { x: 400, y: 500, z: 600 } } };
    };
    await h.handlers.applyStaticLayout("random-3d");
    assert.equal(h.requests[0].body.params.depth, 400);
    assert.equal(h.requests[0].body.params.packComponents, false);
    assert.equal(frames[0].a.z, 300);
    assert.deepEqual(h.data.get("a"), { x: 3, y: 4 });
    assert.equal(h.state.layoutRequest, null);
});

test("spherical and cone layouts reuse shell grouping, score and radial traversal inputs", async () => {
    for (const family of ["shell", "concentric", "radial"]) for (const variant of ["sphere", "cone"]) {
        const h = setup(), type = family + "-" + variant + "-3d";
        h.$("network-renderer").value = "cosmograph";
        h.$("layout-type").value = type;
        h.$("circlepack-group").value = "community:groups";
        h.state.communityData.groups = { a: "z", b: "a" };
        h.$("layout-score").value = "metric:score";
        h.$("layout-score-reverse").checked = true;
        h.state.metricData.score = { a: -3, b: 9 };
        h.$("layout-root").value = "b";
        h.$("layout-direction").value = "IN";
        h.context.window.relisonCosmograph = { getDimensions: () => 3, setNodePositions: async () => true, fitView() {} };
        h.context.api = async (url, options) => {
            h.requests.push({ url, body: JSON.parse(options.body) });
            return { dimensions: 3, positions: { a: { x: 1, y: 2, z: -3 }, b: { x: 3, y: 4, z: 9 } } };
        };
        h.handlers.updateLayoutTypeUI();
        assert.equal(h.$("layout-spacing-field").hidden, false);
        await h.handlers.applyStaticLayout(type);
        assert.equal(h.requests.length, 1, type);
        const body = h.requests[0].body;
        assert.equal(body.algorithm, type);
        assert.equal(body.params.zScale, variant === "cone" ? 50 : undefined);
        assert.equal(h.$("layout-z-scale-field").hidden, variant !== "cone");
        if (family === "shell") {
            assert.equal(h.$("circlepack-group-field").hidden, false);
            assert.deepEqual(body.shells, [["b"], ["a"]]);
        } else if (family === "concentric") {
            assert.equal(h.$("layout-score-field").hidden, false);
            assert.deepEqual(body.scores, { a: -3, b: 9 });
            assert.equal(body.params.reverse, true);
        } else {
            assert.equal(h.$("layout-root-field").hidden, false);
            assert.equal(body.params.root, "b");assert.equal(body.params.direction, "IN");
        }
        assert.deepEqual(h.data.get("a"), { x: 3, y: 4 });
    }
});

test("concentric 3D rejects missing scores before sending a request", async () => {
    const h = setup();
    h.$("network-renderer").value = "cosmograph";
    h.$("layout-score").value = "metric:incomplete";
    h.state.metricData.incomplete = { a: 1 };
    h.context.window.relisonCosmograph = { getDimensions: () => 3 };
    await h.handlers.applyStaticLayout("concentric-cone-3d");
    assert.equal(h.requests.length, 0);
    assert.match(h.statuses.at(-1)[0], /Score is missing/);
});

test("cone layouts reject invalid Z-axis multipliers before requesting a layout", async () => {
    for (const value of ["", "0", "-2", "Infinity"]) {
        const h = setup();
        h.$("network-renderer").value = "cosmograph";
        h.$("layout-z-scale").value = value;
        h.context.window.relisonCosmograph = { getDimensions: () => 3 };
        await h.handlers.applyStaticLayout("shell-cone-3d");
        assert.equal(h.requests.length, 0);
        assert.match(h.statuses.at(-1)[0], /layout-z-scale.*invalid/);
    }
});

test("3D animation uses exact forces and preserves 2D coordinates through every frame", async () => {
    const h = setup(), frames = [], requests = [];
    h.$("network-renderer").value = "cosmograph";
    h.context.window.relisonCosmograph = {
        getDimensions: () => 3, setNodePositions: async positions => { frames.push(positions); return true; }, fitView() {},
    };
    h.context.api = async (url, options) => {
        if (options.method === "DELETE") return {};
        requests.push(JSON.parse(options.body));
        const last = url.endsWith("/step");
        return { sessionId: "spatial", dimensions: 3, iterations: last ? 1 : 0, finished: last, termination: "LIMIT_REACHED",
            positions: { a: { x: 100, y: 200, z: last ? 301 : 300 }, b: { x: 400, y: 500, z: 600 } } };
    };
    await h.handlers.startRelisonAnimation("fruchterman-reingold-3d");
    assert.equal(requests[0].params.theta, 0);
    assert.equal(frames.length, 2);
    assert.equal(frames[1].a.z, 301);
    assert.deepEqual(h.data.get("a"), { x: 3, y: 4 });
    assert.equal(h.state.relisonLayout, null);
});

test("animated flat-3D variants run the 2D force session and stream z=0 frames", async () => {
    const h = setup(), frames = [];
    h.$("network-renderer").value = "cosmograph";
    h.$("layout-type").value = "fruchterman-reingold-flat-3d";
    h.context.window.relisonCosmograph = {
        getDimensions: () => 3,
        setNodePositions: async positions => { frames.push(positions); return true; },
        fitView() {},
    };
    let iterations = 0;
    h.context.api = async (url, options) => {
        if (options.method === "DELETE") return {};
        if (url === "/api/layout/session") {
            const body = JSON.parse(options.body);
            assert.equal(body.algorithm, "fruchterman-reingold");
            return animationFrame(0);
        }
        return animationFrame(++iterations, iterations === 1);
    };
    h.handlers.updateLayoutButton();
    assert.equal(h.$("btn-layout").textContent, "Start layout");
    await h.handlers.onLayoutButton();
    assert.equal(frames.length, 2);
    assert.equal(frames[1].a.z, 0);
    assert.equal(frames[1].b.z, 0);
    assert.deepEqual(h.data.get("a"), { x: 3, y: 4 });
});

test("shell passes community groups in stable inner-to-outer order", async () => {
    const h = setup();
    h.$("circlepack-group").value = "community:groups";
    h.state.communityData.groups = { a: "z", b: "a" };
    await h.handlers.applyStaticLayout("shell");
    assert.deepEqual(h.requests[0].body.shells, [["b"], ["a"]]);
});

test("radial and tree pass root and spacing controls, with optional component packing", async () => {
    const h = setup();
    h.$("layout-root").value = "b";
    h.$("layout-direction").value = "IN";
    h.$("layout-pack-components").checked = true;
    h.$("layout-packing-gap").value = "25";
    await h.handlers.applyStaticLayout("radial");
    assert.deepEqual(h.requests[0].body.params, { root: "b", direction: "IN", spacing: 50, packComponents: true, packingGap: 25 });
    await h.handlers.applyStaticLayout("tree");
    assert.equal(h.requests[1].body.params.root, "b");
    assert.equal(h.requests[1].body.params.levelSpacing, 75);
    h.$("layout-packing-gap").value = "0";
    await h.handlers.applyStaticLayout("tree");
    assert.equal(h.requests.length, 2);
    assert.match(h.statuses.at(-1)[0], /Component gap/);
});

test("bipartite uses automatic colouring or explicit two-group partitions", async () => {
    const h = setup();
    await h.handlers.applyStaticLayout("bipartite");
    assert.equal(h.requests[0].body.partitions, undefined);
    h.$("circlepack-group").value = "community:groups";
    h.state.communityData.groups = { a: "right", b: "left" };
    await h.handlers.applyStaticLayout("bipartite");
    assert.deepEqual(h.requests[1].body.partitions, [["b"], ["a"]]);
    h.state.communityData.groups = { a: "same", b: "same" };
    await h.handlers.applyStaticLayout("bipartite");
    assert.equal(h.requests.length, 2);
    assert.match(h.statuses.at(-1)[0], /exactly two groups/);
});

test("multipartite requires at least two explicit groups", async () => {
    const h = setup();
    await h.handlers.applyStaticLayout("multipartite");
    assert.equal(h.requests.length, 0);
    assert.match(h.statuses.at(-1)[0], /Choose a grouping/);
    h.$("circlepack-group").value = "attr:kind";
    h.data.get("a").attrs = { kind: "user" };
    h.data.get("b").attrs = { kind: "item" };
    await h.handlers.applyStaticLayout("multipartite");
    assert.deepEqual(h.requests[0].body.partitions, [["b"], ["a"]]);
});

test("concentric sends metric or numeric attribute scores", async () => {
    for (const spec of ["metric:centrality", "attr:weight"]) {
        const h = setup();
        h.$("layout-score").value = spec;
        h.state.metricData.centrality = { a: 8, b: 2 };
        h.data.get("a").attrs = { weight: 8 };
        h.data.get("b").attrs = { weight: 2 };
        await h.handlers.applyStaticLayout("concentric");
        assert.deepEqual(h.requests[0].body.scores, { a: 8, b: 2 });
    }
});

test("missing scores and invalid parameters fail before sending a request", async () => {
    const h = setup();
    h.$("layout-score").value = "metric:missing";
    await h.handlers.applyStaticLayout("concentric");
    assert.equal(h.requests.length, 0);
    assert.match(h.statuses.at(-1)[0], /Score is missing/);
    h.$("layout-columns").value = "1.5";
    await h.handlers.applyStaticLayout("grid");
    assert.equal(h.requests.length, 0);
    assert.match(h.statuses.at(-1)[0], /invalid/);
});

test("saved positions are copied and invalidated by node changes", async () => {
    const h = setup();
    h.handlers.saveLayoutPositions();
    h.data.get("a").x = 99;
    assert.equal(h.state.savedLayoutPositions.positions.a.x, 3);
    h.data.set("c", { x: 0, y: 0 });
    await h.handlers.applyStaticLayout("preset");
    assert.equal(h.requests.length, 0);
    assert.match(h.statuses.at(-1)[0], /nodes changed/);
});

test("preset maps numeric attributes to X/Y without a saved arrangement", async () => {
    const h = setup();
    h.$("layout-preset-x").value = "attr:longitude";
    h.$("layout-preset-y").value = "attr:latitude";
    h.data.get("a").attrs = { longitude: -123.4, latitude: 0 };
    h.data.get("b").attrs = { longitude: "0", latitude: 8.5 };
    await h.handlers.applyStaticLayout("preset");
    assert.equal(h.requests.length, 1);
    assert.deepEqual(h.requests[0].body.positions, { a: { x: -123.4, y: 0 }, b: { x: 0, y: 8.5 } });
});

test("preset rejects computed metrics on either coordinate axis", async () => {
    for (const axis of ["x", "y"]) {
        const h = setup();
        h.$("layout-preset-x").value = "attr:feature";
        h.$("layout-preset-y").value = "attr:feature";
        h.$("layout-preset-" + axis).value = "metric:centrality";
        h.data.get("a").attrs = { feature: 1 };
        h.data.get("b").attrs = { feature: 2 };
        h.state.metricData.centrality = { a: 3, b: 4 };
        await h.handlers.applyStaticLayout("preset");
        assert.equal(h.requests.length, 0);
        assert.match(h.statuses.at(-1)[0], /Choose a numeric node attribute or saved coordinate/);
    }
});

test("preset combines a feature axis with a saved axis in either direction", async () => {
    for (const axis of ["x", "y"]) {
        const h = setup();
        h.handlers.saveLayoutPositions();
        h.$("layout-preset-" + axis).value = "attr:feature";
        h.data.get("a").attrs = { feature: 10 };
        h.data.get("b").attrs = { feature: 20 };
        await h.handlers.applyStaticLayout("preset");
        assert.deepEqual(h.requests[0].body.positions, axis === "x"
            ? { a: { x: 10, y: 4 }, b: { x: 20, y: 2 } }
            : { a: { x: 3, y: 10 }, b: { x: 1, y: 20 } });
    }
});

test("preset rejects missing, nonnumeric, and non-finite feature coordinates", async () => {
    for (const value of [undefined, null, "", "  ", "not numeric", true, Infinity, NaN]) {
        const h = setup();
        h.$("layout-preset-x").value = "attr:feature";
        h.$("layout-preset-y").value = "attr:score";
        h.data.get("a").attrs = { feature: value, score: 3 };
        h.data.get("b").attrs = { feature: 2, score: 4 };
        await h.handlers.applyStaticLayout("preset");
        assert.equal(h.requests.length, 0);
        assert.equal(h.data.get("a").x, 3);
        assert.match(h.statuses.at(-1)[0], /X feature is missing or non-finite for node a/);
    }
});

test("preset still requires saved positions when either axis uses saved coordinates", async () => {
    const h = setup();
    h.$("layout-preset-x").value = "attr:feature";
    await h.handlers.applyStaticLayout("preset");
    assert.equal(h.requests.length, 0);
    assert.match(h.statuses.at(-1)[0], /Save positions/);
});

test("server failures and incomplete coordinates leave existing positions untouched", async () => {
    for (const failure of ["server", "incomplete"]) {
        const h = setup();
        h.context.api = async () => {
            if (failure === "server") throw new Error("Server unavailable");
            return { positions: { a: { x: 10, y: 20 } } };
        };
        await h.handlers.applyStaticLayout("grid");
        assert.equal(h.data.get("a").x, 3);
        assert.equal(h.$("btn-layout").disabled, false);
        assert.equal(h.statuses.at(-1)[1], "error");
    }
});

test("cancelled or replaced-graph requests cannot apply stale coordinates", async () => {
    for (const action of ["cancel", "replace"]) {
        const h = setup();
        let resolve;
        h.context.api = () => new Promise(done => { resolve = done; });
        const pending = h.handlers.applyStaticLayout("grid");
        assert.equal(h.$("btn-layout").disabled, true);
        if (action === "cancel") h.handlers.stopLayout();
        else h.state.graph = {};
        resolve({ positions: { a: { x: 100, y: 200 }, b: { x: 300, y: 400 } } });
        await pending;
        assert.equal(h.data.get("a").x, 3);
        assert.equal(h.$("btn-layout").disabled, false);
    }
});

test("layout controls only expose the selected algorithm parameters", () => {
    const h = setup();
    h.$("layout-type").value = "shell";
    h.handlers.updateLayoutTypeUI();
    assert.equal(h.$("circlepack-group-field").hidden, false);
    assert.equal(h.$("layout-spacing-field").hidden, false);
    assert.equal(h.$("layout-radius-field").hidden, true);
    h.$("layout-type").value = "concentric";
    h.handlers.updateLayoutTypeUI();
    assert.equal(h.$("layout-score-field").hidden, false);
    assert.equal(h.$("circlepack-group-field").hidden, true);
    h.$("layout-type").value = "preset";
    h.handlers.updateLayoutTypeUI();
    assert.equal(h.$("layout-preset-x-field").hidden, false);
    assert.equal(h.$("layout-preset-y-field").hidden, false);
    h.$("layout-type").value = "grid";
    h.handlers.updateLayoutTypeUI();
    assert.equal(h.$("layout-preset-x-field").hidden, true);
    h.$("layout-type").value = "radial";
    h.handlers.updateLayoutTypeUI();
    assert.equal(h.$("layout-root-field").hidden, false);
    assert.equal(h.$("layout-direction-field").hidden, false);
    assert.equal(h.$("layout-level-spacing-field").hidden, true);
    h.$("layout-type").value = "tree";
    h.handlers.updateLayoutTypeUI();
    assert.equal(h.$("layout-direction-field").hidden, true);
    assert.equal(h.$("layout-level-spacing-field").hidden, false);
});

test("coordinate selectors offer only numeric features and retain valid selections", () => {
    const h = setup();
    h.state.metricOrder = ["centrality"];
    h.context.nodeAttrDefs = () => [{ name: "weight", numeric: true }, { name: "name", numeric: false }];
    h.context.option = (value, textContent) => ({ value, textContent });
    h.context.restoreSelect = (select, value) => { select.value = select.options.some(item => item.value === value) ? value : ""; };
    for (const [id, defaultValue] of [["circlepack-group", ""], ["layout-score", "degree"], ["layout-preset-x", ""], ["layout-preset-y", ""]]) {
        const select = h.$(id);
        select.options = [];
        select.appendChild = item => select.options.push(item);
        Object.defineProperty(select, "innerHTML", { set() { select.options = [{ value: defaultValue }]; } });
    }
    h.$("layout-preset-x").value = "attr:weight";
    h.$("layout-preset-y").value = "metric:removed";
    h.handlers.rebuildLayoutGroupOptions();
    assert.deepEqual(h.$("layout-preset-x").options.map(item => item.value), ["", "attr:weight"]);
    assert.equal(h.$("layout-preset-x").value, "attr:weight");
    assert.equal(h.$("layout-preset-y").value, "");
});

test("removed scoring options fall back to degree rather than an empty score", () => {
    const h = setup();
    h.context.nodeAttrDefs = () => [];
    h.context.option = (value, textContent) => ({ value, textContent });
    h.context.restoreSelect = () => {};
    h.state.metricOrder = [];
    const select = h.$("layout-score");
    select.value = "metric:removed";
    select.options = [];
    select.appendChild = item => select.options.push(item);
    Object.defineProperty(select, "innerHTML", { set() { select.options = [{ value: "degree" }]; } });
    h.handlers.rebuildLayoutGroupOptions();
    assert.equal(select.value, "degree");
});

test("feature grid sends grouped columns and accepts a single feature value", async () => {
    const h = setup();
    h.$("layout-type").value = "feature-grid";
    h.$("circlepack-group").value = "attr:category";
    h.data.get("a").attrs = { category: "same" };
    h.data.get("b").attrs = { category: "same" };
    h.handlers.updateLayoutTypeUI();
    assert.equal(h.$("circlepack-group-field").hidden, false);
    assert.equal(h.$("layout-column-spacing-field").hidden, false);
    await h.handlers.applyStaticLayout("feature-grid");
    assert.deepEqual(h.requests[0].body.partitions, [["a", "b"]]);
    assert.equal(h.requests[0].body.params.columnSpacing, 150);
    h.$("circlepack-group").value = "";
    await h.handlers.applyStaticLayout("feature-grid");
    assert.equal(h.requests.length, 1);
});

test("ego grid exposes the searchable root and sends traversal and column spacing", async () => {
    const h = setup();
    h.$("layout-type").value = "ego-grid";
    h.$("layout-root").value = "b";
    h.$("layout-direction").value = "MUTUAL";
    h.handlers.updateLayoutTypeUI();
    assert.equal(h.$("layout-root-field").hidden, false);
    assert.equal(h.$("layout-direction-field").hidden, false);
    assert.equal(h.$("layout-column-spacing-field").hidden, false);
    assert.equal(h.$("layout-sweeps-field").hidden, true);
    await h.handlers.applyStaticLayout("ego-grid");
    assert.equal(h.requests[0].body.params.root, "b");
    assert.equal(h.requests[0].body.params.direction, "MUTUAL");
    assert.equal(h.requests[0].body.params.columnSpacing, 150);
    assert.equal(h.requests[0].body.params.spacing, 50);
});

test("RELISON force layouts send validated controls and optional warm starts and glyph radii", async () => {
    for (const type of ["fruchterman-reingold", "relison-forceatlas2"]) {
        const h = setup();
        h.$("layout-type").value = type;
        h.$("layout-warm-start").checked = true;
        h.$("layout-remove-overlap").checked = true;
        h.handlers.updateLayoutTypeUI();
        assert.equal(h.$("layout-force-controls").hidden, false);
        assert.equal(h.$("layout-seed-field").hidden, false);
        await h.handlers.applyStaticLayout(type);
        assert.equal(h.requests[0].body.params.iterations, 10);
        assert.equal(h.requests[0].body.params.theta, 0.8);
        assert.deepEqual(h.requests[0].body.positions.a, { x: 3, y: 4 });
        assert.deepEqual(h.requests[0].body.nodeSizes, { a: 5, b: 5 });
        h.$("layout-theta").value = "3";
        await h.handlers.applyStaticLayout(type);
        assert.equal(h.requests.length, 1);
    }
});

function animationFrame(iterations, finished = false) {
    return { sessionId: "session-one", positions: { a: { x: 10 + iterations, y: 20 }, b: { x: 30 + iterations, y: 40 } },
        iterations, termination: finished ? "LIMIT_REACHED" : "RUNNING", finished };
}

test("both RELISON methods display consecutive frames and release the server session", async () => {
    for (const type of ["fruchterman-reingold", "relison-forceatlas2"]) {
        const h = setup(), calls = [], displayed = [];
        h.$("layout-type").value = type;
        h.context.drawRecOverlay = () => displayed.push(h.data.get("a").x);
        let iterations = 0;
        h.context.api = async (url, options) => {
            calls.push({ url, options });
            if (options.method === "DELETE") return { cancelled: true };
            if (url === "/api/layout/session") {
                assert.match(h.$("btn-layout").textContent, /Stop layout/);
                assert.equal(h.$("btn-layout").disabled, false);
                return animationFrame(0);
            }
            return animationFrame(++iterations, iterations === 3);
        };
        await h.handlers.startRelisonAnimation(type);
        assert.deepEqual(displayed, [10, 11, 12, 13]);
        assert.equal(calls.filter(call => call.url.endsWith("/step")).length, 3);
        assert.equal(calls.at(-1).options.method, "DELETE");
        assert.equal(h.state.relisonLayout, null);
        assert.equal(h.$("btn-layout").textContent, "Start layout");
    }
});

test("Stop during startup cancels the session once allocated without applying its frame", async () => {
    const h = setup(), deleted = [];
    let release;
    h.context.api = async (url, options) => {
        if (options.method === "DELETE") { deleted.push(url); return {}; }
        return new Promise(resolve => { release = resolve; });
    };
    const running = h.handlers.startRelisonAnimation("fruchterman-reingold");
    h.handlers.stopLayout();
    release(animationFrame(0));
    await running;
    assert.equal(h.data.get("a").x, 3);
    assert.deepEqual(deleted, ["/api/layout/session/session-one"]);
    assert.equal(h.state.relisonLayout, null);
});

test("Stop cancels an in-flight step and ignores its late coordinates", async () => {
    const h = setup(), deleted = [];
    let release, notify;
    const stepping = new Promise(resolve => { notify = resolve; });
    h.context.api = async (url, options) => {
        if (options.method === "DELETE") { deleted.push(url); return {}; }
        if (url === "/api/layout/session") return animationFrame(0);
        notify();
        return new Promise(resolve => { release = resolve; });
    };
    const running = h.handlers.startRelisonAnimation("relison-forceatlas2");
    await stepping;
    assert.equal(h.data.get("a").x, 10);
    h.handlers.stopLayout();
    release(animationFrame(99));
    await running;
    assert.equal(h.data.get("a").x, 10);
    assert.equal(deleted.length, 1);
});

test("graph replacement and malformed animation frames cannot overwrite coordinates", async () => {
    for (const replaced of [false, true]) {
        const h = setup(), deleted = [];
        let release;
        h.context.api = async (url, options) => {
            if (options.method === "DELETE") { deleted.push(url); return {}; }
            return new Promise(resolve => { release = resolve; });
        };
        const running = h.handlers.startRelisonAnimation("fruchterman-reingold");
        if (replaced) h.state.graph = {};
        const frame = animationFrame(0);
        if (!replaced) delete frame.positions.b;
        release(frame);
        await running;
        assert.equal(h.data.get("a").x, 3);
        assert.equal(deleted.length, 1);
    }
});

test("Cosmograph renders every RELISON frame without relying on the debounced refresh", async () => {
    const h = setup(), displayed = [];
    h.$("network-renderer").value = "cosmograph";
    h.$("pane-network").classList = { contains: () => true };
    h.context.cosmographAppearanceRefreshTimer = null;
    h.context.clearTimeout = () => {};
    h.context.cosmographInteractionCallbacks = () => ({});
    h.context.window.relisonCosmograph = {
        setNodePositions: async () => false,
        render: async () => { displayed.push(h.data.get("a").x); },
    };
    let iterations = 0;
    h.context.api = async (url, options) => {
        if (options.method === "DELETE") return {};
        if (url === "/api/layout/session") return animationFrame(0);
        return animationFrame(++iterations, iterations === 2);
    };
    await h.handlers.startRelisonAnimation("relison-forceatlas2");
    assert.deepEqual(displayed, [10, 11, 12]);
});

test("the layout button starts RELISON animation instead of the batch endpoint", async () => {
    for (const type of ["fruchterman-reingold", "relison-forceatlas2"]) {
        const h = setup(), calls = [], displayed = [];
        h.$("layout-type").value = type;
        h.context.drawRecOverlay = () => displayed.push(h.data.get("a").x);
        let iterations = 0;
        h.context.api = async (url, options) => {
            calls.push(url);
            if (options.method === "DELETE") return {};
            if (url === "/api/layout/session") return animationFrame(0);
            assert.equal(url, "/api/layout/session/session-one/step");
            return animationFrame(++iterations, iterations === 2);
        };
        await h.handlers.onLayoutButton();
        assert.equal(calls[0], "/api/layout/session");
        assert.equal(calls.includes("/api/layout"), false);
        assert.deepEqual(displayed, [10, 11, 12]);
    }
});

test("the layout button stops a running RELISON session and preserves its last frame", async () => {
    const h = setup();
    h.$("layout-type").value = "relison-forceatlas2";
    let release, notify, deleted = 0;
    const stepping = new Promise(resolve => { notify = resolve; });
    h.context.api = async (url, options) => {
        if (options.method === "DELETE") { deleted++; return {}; }
        if (url === "/api/layout/session") return animationFrame(0);
        notify();
        return new Promise(resolve => { release = resolve; });
    };
    const running = h.handlers.onLayoutButton();
    await stepping;
    h.handlers.onLayoutButton();
    release(animationFrame(99));
    await running;
    assert.equal(deleted, 1);
    assert.equal(h.data.get("a").x, 10);
    assert.equal(h.state.relisonLayout, null);
});

test("ForceAtlas2 controls send reference options and radii without final overlap removal", async () => {
    const h = setup();
    for (const id of ["layout-outbound", "layout-adjust-sizes", "layout-normalize-weights", "layout-invert-weights"])
        h.$(id).checked = true;
    await h.handlers.applyStaticLayout("relison-forceatlas2");
    const body = h.requests[0].body;
    for (const key of ["outboundAttractionDistribution", "adjustSizes", "normalizeWeights", "invertWeights"])
        assert.equal(body.params[key], true);
    assert.equal(body.params.removeOverlap, false);
    assert.deepEqual(body.nodeSizes, {a: 5, b: 5});
});

test("FR reference mode sends frame and temperature instead of custom edge scale", async () => {
    const h = setup();
    h.$("layout-fr-bounded").checked = true;
    h.$("layout-fr-width").value = "100";
    h.$("layout-fr-height").value = "50";
    h.$("layout-fr-temperature").value = "10";
    await h.handlers.applyStaticLayout("fruchterman-reingold");
    const params = h.requests[0].body.params;
    assert.equal(params.bounded, true);
    assert.equal(params.width, 100);
    assert.equal(params.height, 50);
    assert.equal(params.initialTemperature, 10);
    assert.equal(params.idealLength, undefined);
    assert.equal(params.cooling, undefined);
});
