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
        "layout-width": "600", "layout-height": "600", "layout-seed": "42", "layout-score": "degree", "circlepack-group": "",
        "layout-iterations": "10", "layout-theta": "0.8", "layout-tolerance": "0.001",
        "layout-ideal-length": "50", "layout-cooling": "0.95", "layout-fa-scaling": "100", "layout-fa-gravity": "1",
        "layout-jitter": "1", "layout-weight-influence": "0", "layout-node-radius": "5", "layout-overlap-gap": "5",
        "layout-root": "", "layout-direction": "UND", "layout-level-spacing": "75", "layout-column-spacing": "150",
        "layout-sweeps": "4", "layout-packing-gap": "50" };
    const $ = id => {
        if (!inputs.has(id)) inputs.set(id, { value: defaults[id] ?? "", textContent: "", disabled: false, hidden: false,
            checked: false, selectedOptions: [{ textContent: id === "layout-type" ? "Grid" : id }],
            closest: () => ({ querySelector: () => ({ textContent: id }) }) });
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
    const handlers = vm.runInContext(section + "\n({applyStaticLayout, startRelisonAnimation, onLayoutButton, updateLayoutButton, staticLayoutParams, saveLayoutPositions, updateLayoutTypeUI, rebuildLayoutGroupOptions, stopLayout, resetLayout})", context);
    return { handlers, context, state, graph, data, $, requests, statuses };
}

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
        h.state.renderer.refresh = () => displayed.push(h.data.get("a").x);
        let iterations = 0;
        h.context.api = async (url, options) => {
            calls.push({ url, options });
            if (options.method === "DELETE") return { cancelled: true };
            if (url === "/api/layout/session") {
                assert.equal(h.$("btn-layout").textContent, "Stop layout");
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
        h.state.renderer.refresh = () => displayed.push(h.data.get("a").x);
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
