const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
// Exercise our adapter against the existing Cosmograph API boundary without loading a CDN or WebGL.
const source = fs.readFileSync(path.join(__dirname, "../../main/resources/public/community-cosmograph.js"), "utf8")
    .replace(/^import .*?;\s*/s, "").replace("export async function create", "async function create");
const tick = () => new Promise(resolve => setImmediate(resolve));
const plain = value => JSON.parse(JSON.stringify(value));

function setup(options = {}) {
    const instances = [], hosts = [], frames = [];
    class Cosmograph {
        constructor(host, config) { this.host = host; this.config = config; this.camera = { azimuth: 1, polar: 2, distance: 10 }; this.destroyed = false; this.events = []; this.fitCount = 0; instances.push(this); }
        async dataUploaded() { return options.upload?.(); }
        stop() {}
        destroy() { this.destroyed = true; }
        async setConfig(config) { this.config = config; this.camera = {}; this.events.push("config"); }
        async setConfigPartial(config) { Object.assign(this.config, config); }
        getCameraState() { return this.camera; }
        setCameraState(camera) { this.camera = camera; this.events.push("camera"); }
        getZoomLevel() { return this.zoomLevel || 1; }
        setZoomLevel(level) { this.zoomLevel = level; }
        fitView() { this.fitted = true; this.fitCount++; this.events.push("fit"); }
        resize() {}
        spaceToScreenPosition(point, options) { this.projection = { point, options }; return [point[0] * 10 + 100, point[1] * 10 + 100]; }
        getPointScreenRadiusByIndex(index) { return this.config.points[index].size; }
    }
    const points = new Map([["0", { x: 1, y: 2, z: 3, size: 8, label: "First", color: "#ff0000" }], ["1", { x: 4, y: 5, z: 6, size: 4, label: "Second", color: "#00ff00" }]]);
    const edge = { color: "#888888", size: 2, hidden: false };
    const graph = { type: "directed", nodes: () => [...points.keys()], edges: () => ["link"],
        getNodeAttributes: id => points.get(id), getNodeAttribute: (id, key) => points.get(id)[key],
        source: () => "0", target: () => "1", getEdgeAttribute: (_id, key) => edge[key] };
    const context = vm.createContext({ Cosmograph, console, Map, JSON,
        prepareCosmographData: async (_schema, points, links) => { await options.prepare?.(); return { points, links, cosmographConfig: {} }; },
        document: { body: {}, createElement: () => { const host = { labels: [], querySelectorAll: () => host.labels, remove() { this.removed = true; } }; hosts.push(host); return host; } },
        getComputedStyle: element => element?.computedStyle || ({ getPropertyValue: () => "#ffffff" }),
        requestAnimationFrame: callback => { frames.push(callback); return frames.length; }, cancelAnimationFrame() {},
    });
    return { create: vm.runInContext(source + "\ncreate", context), graph, points, edge, instances, hosts, frames, container: { appendChild() {} } };
}

function nativeLabel(index, overrides = {}) {
    return { computedStyle: { display: "block", visibility: "visible", opacity: "1",
        getPropertyValue: key => key === "--relison-community-node-index" ? String(index) : "", ...overrides },
        getBoundingClientRect: () => ({ width: 60, height: 20 }), closest: () => null };
}

test("community label selection uses Network's native dynamic settings and updates visibility after zoom", async () => {
    const h = setup(), labels = { labelShow: true, labelSize: 20, labelFont: "Georgia, serif", labelProp: true };
    const renderer = await h.create(h.graph, h.container, { labelOptions: () => labels });
    const instance = h.instances[0];
    assert.equal(instance.config.showLabels, true); assert.equal(instance.config.showTopLabels, false);
    assert.equal(instance.config.showDynamicLabels, true); assert.equal(instance.config.showDynamicLabelsLimit, 200);
    assert.equal(instance.config.showHoveredPointLabel, true);
    assert.match(instance.config.pointLabelClassName("First", 0), /--relison-community-node-index: 0; font-family: Georgia, serif; font-size: 20px/);
    assert.match(instance.config.pointLabelClassName("Second", 1), /font-size: 10px/);
    h.hosts[0].labels = [nativeLabel(1), nativeLabel(0, { opacity: "0" }), nativeLabel(0, { display: "none" }),
        { ...nativeLabel(0), closest: () => ({}) }, { ...nativeLabel(0), getBoundingClientRect: () => ({ width: 0, height: 0 }) }];
    assert.deepEqual([...renderer.getVisibleLabelNodes()], ["1"]);
    let redraws = 0; renderer.on("afterRender", () => redraws++);
    h.frames.shift()(); const previous = redraws;
    h.hosts[0].labels = [nativeLabel(0)]; h.frames.shift()();
    assert.ok(redraws > previous, "native label selection changes must redraw the foreground even without a camera change");
    assert.deepEqual([...renderer.getVisibleLabelNodes()], ["0"]);
    labels.labelShow = false; labels.labelSize = 16; labels.labelFont = "monospace";
    await renderer.refresh();
    assert.equal(instance.config.showLabels, false); assert.equal(instance.config.showHoveredPointLabel, true);
    assert.match(instance.config.pointLabelClassName("Second", 1), /font-family: monospace; font-size: 8px/);
    h.points.get("0").color = "#0000ff"; await renderer.refresh();
    assert.equal(instance.config.showLabels, false); assert.equal(instance.config.showDynamicLabelsLimit, 200);
    renderer.kill(); assert.deepEqual([...renderer.getVisibleLabelNodes()], []);
});

test("Cosmograph emits node enter/leave events independently of persistent labels and stops after disposal", async () => {
    const h = setup(), events = [];
    const renderer = await h.create(h.graph, h.container, { labelOptions: () => ({ labelShow: false }) });
    renderer.on("enterNode", ({ node }) => events.push("enter " + node));
    renderer.on("leaveNode", ({ node }) => events.push("leave " + node));
    const config = h.instances[0].config;
    config.onPointMouseOver(0); config.onPointMouseOver(0); config.onPointMouseOver(1); config.onPointMouseOut();
    assert.deepEqual(events, ["enter 0", "leave 0", "enter 1", "leave 1"]);
    renderer.kill(); config.onPointMouseOver(0);
    assert.equal(events.length, 4);
});

test("Cosmograph community instance has independent coordinates, sizes, links and click events", async () => {
    const h = setup(), clicks = [];
    const renderer = await h.create(h.graph, h.container, { dimensions: 3, selected: "1" });
    const instance = h.instances[0];
    assert.equal(instance.config.spaceDimensions, 3); assert.equal(instance.config.enableSimulation, false);
    assert.equal(instance.config.points[0].size, 16); assert.equal(instance.config.points[0].z, 3);
    assert.equal(instance.config.linkDefaultArrows, true);
    assert.deepEqual(plain(instance.config.outlinedPointIndices), [1]);
    renderer.on("clickNode", value => clicks.push(value)); renderer.on("clickEdge", value => clicks.push(value));
    instance.config.onPointClick(1); instance.config.onLinkClick(0);
    assert.deepEqual(plain(clicks), [{ node: "1" }, { edge: "link" }]);
    assert.deepEqual(plain(renderer.graphToViewport(h.points.get("0"))), { x: 110, y: 120 });
    assert.deepEqual(plain(instance.projection), { point: [1, 2, 3], options: { dimensions: 3 } });
    assert.equal(renderer.getNodeDisplayData("0").size, 8);
    renderer.kill(); assert.equal(instance.destroyed, true); assert.equal(h.hosts[0].removed, true);
});

test("cancelling a stalled upload immediately destroys its instance and allows a replacement", async () => {
    let rejectUpload, stalled = true;
    const h = setup({ upload: () => stalled ? new Promise((_resolve, reject) => { rejectUpload = reject; }) : Promise.resolve() });
    const controller = new AbortController();
    const pending = h.create(h.graph, h.container, { signal: controller.signal });
    const rejected = assert.rejects(pending, { name: "AbortError" });
    await tick();
    controller.abort();
    assert.equal(h.instances[0].destroyed, true); assert.equal(h.hosts[0].removed, true);
    await rejected;
    // Even a late library failure must be consumed after cancellation.
    rejectUpload(new Error("late upload failure")); await tick();
    stalled = false;
    const renderer = await h.create(h.graph, h.container);
    assert.equal(h.instances[1].destroyed, false); assert.equal(h.hosts[1].removed, undefined);
    assert.equal(h.instances[0].fitCount, 0); renderer.kill();
});

test("cancelling preparation cannot create a late WebGL instance, and preserves the timeout reason", async () => {
    let finish;
    const h = setup({ prepare: () => new Promise(resolve => { finish = resolve; }) });
    const controller = new AbortController(), reason = new Error("Cosmograph loading timed out");
    const pending = h.create(h.graph, h.container, { signal: controller.signal });
    const rejected = assert.rejects(pending, error => error === reason);
    controller.abort(reason); await rejected;
    assert.equal(h.hosts[0].removed, true);
    finish(); await tick();
    assert.equal(h.instances.length, 0);
    const aborted = new AbortController(); aborted.abort();
    await assert.rejects(h.create(h.graph, h.container, { signal: aborted.signal }), { name: "AbortError" });
    assert.equal(h.hosts.length, 1);
});

test("label, colour and selection changes preserve the Cosmograph camera", async () => {
    const h = setup(), camera = { azimuth: 0.4, polar: 1.5, distance: 40 };
    const renderer = await h.create(h.graph, h.container, { dimensions: 2, camera });
    h.points.get("0").label = "Research"; h.points.get("0").color = "#0000ff";
    renderer.setSetting("selectedNode", "0"); renderer.setSetting("labelColor", { color: "#111111" });
    renderer.refresh(); await tick();
    const instance = h.instances[0];
    assert.equal(instance.config.points[0].label, "Research");
    assert.equal(instance.config.points[0].color, "#0000ff");
    assert.deepEqual(plain(instance.config.outlinedPointIndices), [0]);
    assert.deepEqual(plain(renderer.getCamera().getState()), camera);
    renderer.graphToViewport(h.points.get("0"));
    assert.deepEqual(plain(instance.projection.point), [1, 2]);
    renderer.kill();
});

test("straight and curved edges work on initial load and after partial or full updates in 2D and 3D", async () => {
    for (const dimensions of [2, 3]) {
        const h = setup(), renderer = await h.create(h.graph, h.container, { dimensions });
        const instance = h.instances[0];
        assert.equal(instance.config.curvedLinks, true);
        renderer.setSetting("edgeShape", "straight");
        await renderer.refresh({ fit: true });
        assert.equal(instance.config.curvedLinks, false); assert.equal(instance.fitCount, 2);
        h.points.get("0").size = 20;
        await renderer.refresh({ fit: true });
        assert.equal(instance.config.curvedLinks, false);
        assert.equal(instance.config.points[0].size, 40);
        renderer.setSetting("edgeShape", "curved");
        await renderer.refresh();
        assert.equal(instance.config.curvedLinks, true);
        const straight = await h.create(h.graph, h.container, { dimensions, edgeShape: "straight" });
        assert.equal(h.instances[1].config.curvedLinks, false);
        renderer.kill(); straight.kill();
    }
});

test("destroying an instance only removes its own host when renderer creation overlaps", async () => {
    const h = setup();
    const first = await h.create(h.graph, h.container);
    const second = await h.create(h.graph, h.container);
    first.kill(); first.kill();
    assert.equal(h.hosts[0].removed, true); assert.equal(h.hosts[1].removed, undefined);
    assert.equal(h.instances[1].destroyed, false);
    second.kill();
});

test("Cosmograph updates per-node label colours, radii and edge appearance without moving its camera", async () => {
    const h = setup(), camera = { azimuth: 0.7, distance: 25, polar: 1 };
    let mode = "node";
    const renderer = await h.create(h.graph, h.container, { dimensions: 3, camera,
        labelColorByNode: point => mode === "node" ? point.color : mode === "single" ? "#112233" : "#ffffff" });
    const instance = h.instances[0];
    assert.match(instance.config.pointLabelClassName("First", 0), /color: #ff0000;/);
    assert.match(instance.config.pointLabelClassName("Second", 1), /color: #00ff00;/);
    mode = "single"; renderer.refresh(); await tick();
    assert.match(instance.config.pointLabelClassName("First", 0), /color: #112233;/);
    h.points.get("0").size = 17; h.edge.color = "#001122"; h.edge.size = 7;
    renderer.refresh(); await tick();
    assert.equal(instance.config.points[0].size, 34);
    assert.equal(instance.config.links[0].color, "#001122"); assert.equal(instance.config.links[0].width, 7);
    assert.deepEqual(plain(renderer.getCamera().getState()), camera);
    mode = "default"; renderer.refresh(); await tick();
    assert.match(instance.config.pointLabelClassName("First", 0), /color: #ffffff;/);
    h.edge.hidden = true; renderer.refresh(); await tick();
    assert.equal(instance.config.links.length, 0);
    renderer.kill();
});

test("appearance Fit runs after the final serialized upload and never restores the obsolete camera afterward", async () => {
    const h = setup();
    const renderer = await h.create(h.graph, h.container);
    const instance = h.instances[0], uploads = [], initialFits = instance.fitCount;
    instance.dataUploaded = () => new Promise(resolve => { instance.events.push("upload"); uploads.push(() => { instance.events.push("uploaded"); resolve(); }); });
    h.points.get("0").size = 12;
    const first = renderer.refresh({ fit: true }); await tick();
    assert.equal(uploads.length, 1);
    h.points.get("0").size = 25;
    const latest = renderer.refresh({ fit: true }); await tick();
    assert.equal(uploads.length, 1, "uploads must not overlap");
    uploads[0](); await tick();
    assert.equal(uploads.length, 2); assert.equal(instance.fitCount, initialFits);
    uploads[1](); await Promise.all([first, latest]);
    assert.equal(instance.config.points[0].size, 50);
    assert.equal(instance.fitCount, initialFits + 1);
    assert.deepEqual(instance.events.slice(-2), ["uploaded", "fit"]);
    assert.equal(instance.config.showLabels, true); assert.equal(instance.config.showHoveredPointLabel, true);
    renderer.kill();
});

test("a queued non-fitting refresh cannot lose requested Fit, and destroyed renderers cannot fit later", async () => {
    const h = setup(), renderer = await h.create(h.graph, h.container), instance = h.instances[0];
    h.points.get("0").color = "#aabbcc";
    await Promise.all([renderer.refresh({ fit: true }), renderer.refresh()]);
    assert.equal(instance.fitCount, 2);
    await renderer.getCamera().animatedReset();
    assert.equal(instance.fitCount, 3);
    let finish;
    instance.dataUploaded = () => new Promise(resolve => { finish = resolve; });
    h.points.get("0").size = 30;
    const pending = renderer.refresh({ fit: true }); await tick();
    renderer.kill(); finish(); await pending;
    assert.equal(instance.fitCount, 3);
});

test("community zoom buttons use the same 2D zoom and 3D distance changes as Network", async () => {
    const h = setup(), planar = await h.create(h.graph, h.container, { dimensions: 2 });
    await planar.getCamera().animatedZoom({ factor: 1.25 });
    assert.equal(h.instances[0].zoomLevel, 1.25);
    await planar.getCamera().animatedZoom({ factor: 0.8 });
    assert.equal(h.instances[0].zoomLevel, 1);
    const spatial = await h.create(h.graph, h.container, { dimensions: 3 });
    await spatial.getCamera().animatedZoom({ factor: 1.25 });
    assert.equal(h.instances[1].camera.distance, 8);
    await spatial.getCamera().animatedZoom({ factor: 0.8 });
    assert.equal(h.instances[1].camera.distance, 10);
    assert.equal(await spatial.getCamera().animatedZoom({ factor: -1 }), false);
    planar.kill(); spatial.kill();
});
