/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const source = fs.readFileSync(path.join(__dirname, "../../main/resources/public/diffusion-cosmograph.js"), "utf8")
    .replace(/^import .*;\s*/m, "");

function setup(options = {}) {
    const configs = [], positionUpdates = [], fits = [], renders = [], destroyed = [];
    class Cosmograph {
        constructor(_container, config) {
            configs.push(config);
            this.config = config;
            this._graph = {
                setPointColors() {}, setLinkColors() {}, setPointSizes() {}, setLinkWidths() {}, setLinkStyles() {},
                setPointPositions: (positions, options) => positionUpdates.push({ positions: Array.from(positions), options }),
                render: (...args) => renders.push(args),
            };
        }
        async dataUploaded() { if (options.uploadError) throw new Error("Upload failed"); }
        stop() {}
        destroy() { destroyed.push(this); }
        fitView(...args) { fits.push(args); }
    }
    const graph = {
        type: "directed", nodes: () => ["a", "b"], edges: () => ["ab"],
        getNodeAttributes: node => ({ x: node === "a" ? 1 : 4, y: node === "a" ? 2 : 5, label: node, size: 3, color: "#fff" }),
        getNodeAttribute: (node, key) => ({ x: node === "a" ? 1 : 4, y: node === "a" ? 2 : 5, size: 3 }[key]),
        source: () => "a", target: () => "b", getEdgeAttribute: () => 1,
    };
    const window = { relisonCosmographLabelOpts: {}, dispatchEvent() {} };
    const context = vm.createContext({ Cosmograph, window, graph,
        document: { getElementById: id => id === "cosmograph-sphere-shading" ? { checked: true } : null,
            createElement: () => ({ getContext: () => ({ fillRect() {}, drawImage() {} }) }) },
        CustomEvent: function () {}, Float32Array, Map, Set, Object, Number, String, Math,
        requestAnimationFrame: callback => callback(), getComputedStyle: () => ({ getPropertyValue: () => "#000" }),
    });
    vm.runInContext(source, context);
    return { api: window.relisonDiffusionCosmograph, configs, positionUpdates, graph, fits, renders, destroyed };
}

for (const dimensions of [2, 3]) test(`diffusion Cosmograph uses network coordinates in ${dimensions}D`, async () => {
    const h = setup();
    const positions = { a: { x: 10, y: 20, z: 30 }, b: { x: 40, y: 50, z: 60 } };
    const view = await h.api.create(h.graph, {}, null, { dimensions, positions });
    const config = h.configs[0];
    assert.equal(config.spaceDimensions, dimensions);
    assert.equal(config.pointZBy, dimensions === 3 ? "z" : undefined);
    assert.equal(config.pointSphereShading, dimensions === 3);
    assert.deepEqual(config.points.map(point => [point.x, point.y, point.z]), dimensions === 3
        ? [[10, 20, 30], [40, 50, 60]] : [[10, 20, 0], [40, 50, 0]]);
    assert.equal(view.getDimensions(), dimensions);

    await view.setIteration(0, { iterations: [{ newlyInformed: ["a"], propagating: [] }] },
        { base: "#000000", informed: "#0000ff", newly: "#00ff00", prop: "#ff0000" });
    assert.equal(h.positionUpdates[0].options.dimensions, dimensions);
    assert.equal(h.positionUpdates[0].positions.length, 2 * dimensions);
    if (dimensions === 3) {
        await view.syncAppearance(h.graph, null, { a: { x: 11, y: 21, z: 31 }, b: { x: 41, y: 51, z: 61 } });
        assert.deepEqual(h.positionUpdates.at(-1).positions, [11, 21, 31, 41, 51, 61]);
    }
});

test("diffusion fits only after its viewport is visible and redraws through the supported Cosmos API", async () => {
    const h = setup(), container = { clientWidth: 0, clientHeight: 0 };
    const view = await h.api.create(h.graph, container);
    assert.equal(h.configs[0].fitViewOnInit, false); assert.equal(h.fits.length, 0);
    container.clientWidth = 640; container.clientHeight = 480;
    view.resize(); assert.equal(h.renders.length, 1); assert.equal(h.fits.length, 1);
    view.resize(); assert.equal(h.renders.length, 2); assert.equal(h.fits.length, 1);
    assert.deepEqual(h.fits[0], [0, 0.1]);
});

test("failed uploads release the partially created WebGL renderer", async () => {
    const h = setup({ uploadError: true });
    await assert.rejects(h.api.create(h.graph, {}), /Upload failed/);
    assert.equal(h.destroyed.length, 1);
});

function lifecycle() {
    const app = fs.readFileSync(path.join(__dirname, "../../main/resources/public/app.js"), "utf8");
    const start = app.indexOf("function restoreDiffCosmograph() {"), end = app.indexOf("\nfunction usingDiffGeographic", start);
    const controls = new Map(), frames = [], uploads = [], statuses = [];
    const $ = id => {
        if (!controls.has(id)) controls.set(id, { value: id === "network-renderer" ? "cosmograph" : "", classList: { toggle() {} } });
        return controls.get(id);
    };
    const state = { activeTab: "diffusion", directed: true, graph: {}, rec: { models: {}, color: "#fff", diff: false },
        diffusion: { graph: {}, result: null, recKey: null, rendererType: "cosmograph", subview: "pieces" } };
    const window = { relisonDiffusionCosmograph: { create: () => new Promise((resolve, reject) => {
        const entry = { resolve, reject, destroyed: 0, resized: 0, iterations: [] };
        entry.instance = { destroy() { entry.destroyed++; }, resize() { entry.resized++; }, async setIteration(...args) { entry.iterations.push(args); } };
        uploads.push(entry);
    }) } };
    const context = vm.createContext({ state, $, window, DIFF_COL: {}, usingGeographic: () => false,
        requestAnimationFrame: callback => frames.push(callback), selectDiffNode() {}, initDiffGeographic() {}, drawDiffOverlay() {},
        setStatus: message => statuses.push(message), console: { error() {} } });
    vm.runInContext(app.slice(start, end), context);
    const flush = () => { while (frames.length) frames.shift()(); };
    return { context, state, uploads, frames, flush, statuses };
}

test("tab restoration defers hidden uploads and coalesces duplicate initialization", async () => {
    const h = lifecycle();
    h.context.setDiffRenderer("network"); h.flush();
    assert.equal(h.uploads.length, 0);
    h.state.diffusion.subview = "graph";
    h.context.restoreDiffCosmograph(); h.context.restoreDiffCosmograph(); h.flush();
    assert.equal(h.uploads.length, 1);
    const pending = h.context.initDiffCosmograph(); assert.strictEqual(h.context.initDiffCosmograph(), pending);
    h.uploads[0].resolve(h.uploads[0].instance); await pending;
    assert.strictEqual(h.state.diffusion.cosmograph, h.uploads[0].instance);
    h.state.diffusion.subview = "stats"; h.context.restoreDiffCosmograph(); h.flush();
    h.state.diffusion.subview = "graph"; h.context.restoreDiffCosmograph(); h.flush();
    assert.equal(h.uploads.length, 1); assert.equal(h.uploads[0].destroyed, 0); assert.ok(h.uploads[0].resized >= 2);
});

test("a new run waits for stale uploads to clean up before creating its renderer", async () => {
    const h = lifecycle(); h.state.diffusion.subview = "graph";
    const first = h.context.initDiffCosmograph();
    const result = h.state.diffusion.result = { iterations: [] };
    const second = h.context.initDiffCosmograph(); assert.equal(h.uploads.length, 1);
    h.uploads[0].resolve(h.uploads[0].instance); await first; await Promise.resolve();
    assert.equal(h.uploads[0].destroyed, 1); assert.equal(h.uploads.length, 2);
    h.uploads[1].resolve(h.uploads[1].instance); await second;
    assert.strictEqual(h.state.diffusion.cosmograph, h.uploads[1].instance);
    assert.strictEqual(h.uploads[1].iterations[0][1], result); assert.equal(h.statuses.length, 0);
});

test("switching renderer during upload discards the obsolete instance", async () => {
    const h = lifecycle(); h.state.diffusion.subview = "graph";
    const pending = h.context.initDiffCosmograph(); h.context.setDiffRenderer("sigma");
    h.uploads[0].resolve(h.uploads[0].instance); await pending;
    assert.equal(h.uploads[0].destroyed, 1); assert.equal(h.state.diffusion.cosmograph, null);
    assert.equal(h.state.diffusion.rendererType, "sigma"); assert.equal(h.statuses.length, 0);
});
