const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const publicDir = path.join(__dirname, "../../main/resources/public");
const source = fs.readFileSync(path.join(publicDir, "nodi-tutorials.js"), "utf8");

function memoryStorage(initial = null) {
    const values = new Map([["relison-nodi-tutorials-v1", initial]]);
    return { getItem: key => values.get(key) ?? null, setItem: (key, next) => { values.set(key, next); } };
}

function setup(storage = memoryStorage(), options = {}) {
    const nodes = new Map(), frames = [], navigations = [], listeners = {};
    const context = { tab: "import", hasGraph: false, multigraph: false };
    const reducedMotion = { matches: !!options.reducedMotion, addEventListener(_name, handler) { listeners.motionChange = handler; } };
    const heading = { focus() {} };
    function node(tag = "div") {
        return { tag, innerHTML: "", hidden: false, style: {}, attrs: {}, listeners: {}, dataset: {},
            offsetWidth: 360, offsetHeight: 330, isConnected: true,
            setAttribute(key, value) { this.attrs[key] = value; },
            addEventListener(key, handler) { this.listeners[key] = handler; },
            focus() { document.activeElement = this; },
            querySelector(selector) {
                if (selector !== '[data-nodi="motion"]') return heading;
                if (!this.innerHTML.includes('data-nodi="motion"')) return null;
                this.motionButton ||= node("button");
                this.motionButton.dataset.nodi = "motion";
                this.motionButton.dataset.compact = String(this.innerHTML.includes('data-compact="true"'));
                return this.motionButton;
            },
            querySelectorAll() { return []; },
            getClientRects() { return this.hidden ? [] : [this.getBoundingClientRect()]; },
            getBoundingClientRect() { return { left: 10, top: 120, right: 200, bottom: 150 }; },
            scrollIntoView() {},
            showModal() { this.open = true; }, close() { this.open = false; },
        };
    }
    const document = { activeElement: null,
        body: { append(...items) { for (const item of items) nodes.set(item.id || item.className, item); } },
        createElement: node,
        getElementById(id) { if (!nodes.has(id)) nodes.set(id, node()); return nodes.get(id); },
        querySelector(selector) {
            if (selector === "dialog[open]") return nodes.get("nodi-library")?.open ? nodes.get("nodi-library") : null;
            return this.getElementById(selector.slice(1));
        },
        addEventListener(key, handler) { listeners[key] = handler; },
    };
    const sandbox = vm.createContext({ document, console,
        window: { innerWidth: 1200, innerHeight: 800, addEventListener() {}, matchMedia: () => reducedMotion },
        requestAnimationFrame(callback) { frames.push(callback); return frames.length; },
        getComputedStyle: () => ({ visibility: "visible" }),
    });
    const api = vm.runInContext(source + "\nNodiTutorials", sandbox);
    const ui = api.create({ storage, getContext: () => context, navigate(step) {
        navigations.push(step);
        if (step.tab) context.tab = step.tab;
    } });
    return { api, ui, context, nodes, navigations, listeners, storage, reducedMotion,
        frame() { while (frames.length) frames.shift()(); },
        guide: () => nodes.get("nodi-guide"), library: () => nodes.get("nodi-library"),
    };
}

function clickMotion(root) {
    const button = root.querySelector('[data-nodi="motion"]');
    root.listeners.click({ target: { closest: () => button } });
}

function complete(h, type, details = {}) {
    h.ui.actionCompleted({ type, graphKey: h.context.graphKey ?? (h.context.hasGraph ? true : null),
        graphRevision: h.context.graphRevision || 0, ...details });
}

test("loading tutorial waits for a real load/generation event, including with an existing network, and cannot be skipped", () => {
    const h = setup(); h.context.hasGraph = true; h.context.graphKey = "existing";
    h.ui.start("import"); h.ui.move(99);
    assert.equal(h.api.createProgress(h.storage).index("import"), 3);
    assert.match(h.guide().innerHTML, /data-nodi="next" disabled/);
    assert.match(h.guide().innerHTML, /Load a network, open a saved session, or generate/);
    h.ui.move(1);
    assert.equal(h.api.createProgress(h.storage).completed("import"), false);
    h.context.graphKey = "generated";
    complete(h, "network", { graphKey: "old request" });
    assert.equal(h.api.createProgress(h.storage).index("import"), 3);
    complete(h, "network");
    assert.equal(h.navigations.at(-1).title, "Your network is ready");
    assert.equal(h.context.tab, "network");
    h.context.graphRevision = 1; h.ui.contextChanged();
    h.ui.move(1); assert.equal(h.api.createProgress(h.storage).completed("import"), true);
    h.ui.start("import"); h.ui.move(99);
    assert.equal(h.api.createProgress(h.storage).completed("import"), false, "replay starts a fresh attempt");
    assert.equal(h.api.createProgress(h.storage).index("import"), 3);
});

test("loading a network early in the import tour skips the alternative generator instructions", () => {
    const h = setup(); h.ui.start("import");
    h.context.hasGraph = true; h.context.graphKey = "loaded";
    complete(h, "network");
    assert.equal(h.navigations.at(-1).title, "Your network is ready");
    assert.equal(h.api.createProgress(h.storage).index("import"), 4);
});

test("metric tour requires a new successful computation and hands the exact result to rankings and distributions", () => {
    const h = setup(); h.context.hasGraph = true; h.context.graphKey = "graph";
    h.context.nodeMetrics = { Old: { a: 1 } };
    h.ui.start("metrics"); h.ui.move(1); h.ui.move(1);
    assert.equal(h.api.createProgress(h.storage).index("metrics"), 1);
    assert.match(h.guide().innerHTML, /data-nodi="next" disabled/);
    h.guide().listeners.keydown({ key: "ArrowRight", target: { closest: () => null }, preventDefault() {} });
    assert.equal(h.api.createProgress(h.storage).index("metrics"), 1, "keyboard navigation obeys the gate");
    complete(h, "node-metric", { label: "New", values: {} });
    assert.equal(h.api.createProgress(h.storage).index("metrics"), 1, "an event without a stored result is insufficient");
    const values = h.context.nodeMetrics["Degree <OUT>"] = { a: 2, b: 0 };
    complete(h, "node-metric", { label: "Degree <OUT>", values });
    assert.equal(h.navigations.at(-1).view, "node-summary");
    assert.equal(h.navigations.at(-1).result.values, values);
    assert.match(h.guide().innerHTML, /Degree &lt;OUT&gt;/);
    h.ui.move(1);
    assert.equal(h.navigations.at(-1).view, "node-rankings");
    h.ui.move(1);
    assert.equal(h.navigations.at(-1).view, "node-ranking-table");
    h.ui.move(1);
    assert.equal(h.navigations.at(-1).view, "node-distributions");
    assert.equal(h.navigations.at(-1).result.label, "Degree <OUT>");
    h.ui.move(1); assert.equal(h.api.createProgress(h.storage).completed("metrics"), true);
});

test("paused action steps record success without navigating, and new sessions or graph edits require the action again", () => {
    const h = setup(); h.context.hasGraph = true; h.context.graphKey = "graph";
    h.context.nodeMetrics = {};
    h.ui.start("metrics"); h.ui.move(1); h.ui.closeGuide();
    const count = h.navigations.length, values = h.context.nodeMetrics.Degree = { a: 1 };
    complete(h, "node-metric", { label: "Degree", values });
    assert.equal(h.navigations.length, count);
    h.ui.start("metrics"); assert.equal(h.navigations.at(-1).view, "node-summary");
    h.context.graphRevision = 1; h.context.nodeMetrics = {}; h.ui.contextChanged();
    assert.equal(h.api.createProgress(h.storage).index("metrics"), 1);
    complete(h, "node-metric", { graphRevision: 0, label: "Degree", values });
    assert.equal(h.api.createProgress(h.storage).index("metrics"), 1);
    const reloaded = setup(memoryStorage('{"progress":{"metrics":3},"completed":[]}'));
    reloaded.context.hasGraph = true; reloaded.context.graphKey = "restored";
    reloaded.context.nodeMetrics = { Degree: { a: 1 } };
    reloaded.ui.start("metrics");
    assert.equal(reloaded.navigations.at(-1).view, "compute-nodes", "saved step numbers and old results cannot bypass an action");
});

test("community, edge-group, recommendation and diffusion tours wait at their computation steps", () => {
    for (const [id, type, index] of [["communities", "community", 1], ["edge-partitions", "edge-partition", 1],
        ["recommend", "recommendation", 1], ["diffusion", "diffusion", 2]]) {
        const h = setup(); h.context.hasGraph = true; h.context.graphKey = "graph";
        h.ui.start(id); h.ui.move(index); h.ui.move(1);
        assert.equal(h.api.createProgress(h.storage).index(id), index, id);
        complete(h, type, { label: "Result" });
        assert.equal(h.api.createProgress(h.storage).index(id), index + 1, id);
        if (id === "diffusion") assert.equal(h.navigations.at(-1).view, "graph");
    }
});

function metricRunner(h, response) {
    const graph = {}, values = { a: 1 }, elements = new Map(), ended = [];
    h.context.hasGraph = true; h.context.graphKey = graph; h.context.graphRevision = 0;
    h.context.nodeMetrics = { Degree: values };
    const state = { graph, graphId: "g", networkRevision: 0, metricData: h.context.nodeMetrics, metricOrder: ["Degree"], rec: { active: null } };
    const signal = { aborted: false };
    const context = vm.createContext({ state, NodiTutorials: { actionCompleted: action => h.ui.actionCompleted(action) },
        $(id) { if (!elements.has(id)) elements.set(id, { value: "Degree", checked: false }); return elements.get(id); },
        jobBusy: () => false, requireGraph: () => true, setStatus() {}, beginJob: () => signal,
        endJob: (...args) => ended.push(args), api: () => response, jsonBody: body => ({ body }),
        collectParams: () => ({}), numericMap: data => ({ ...data }), rebuildAppearanceOptions() {},
        syncNodeColorControls() {}, applyAppearance() {}, fmt: String, isAbort: error => error.name === "AbortError",
        refreshAfterCompute: () => h.ui.contextChanged() });
    const app = fs.readFileSync(path.join(publicDir, "app.js"), "utf8").replace(/\r\n/g, "\n");
    const start = app.indexOf("async function runVertexMetric(");
    vm.runInContext(app.slice(start, app.indexOf("\n}", start) + 2), context);
    h.ui.start("metrics"); h.ui.move(1);
    return { state, signal, ended, run: () => context.runVertexMetric() };
}

test("the real node-metric runner unlocks the tour after storage, even when recomputing the same result label", async () => {
    const h = setup();
    let resolve;
    const runner = metricRunner(h, new Promise(done => { resolve = done; }));
    const before = runner.state.metricData.Degree, request = runner.run();
    assert.equal(h.api.createProgress(h.storage).index("metrics"), 1);
    resolve({ label: "Degree", values: { a: 3, b: 0 }, average: 1.5 }); await request;
    assert.notEqual(runner.state.metricData.Degree, before);
    assert.equal(h.navigations.at(-1).view, "node-summary");
    assert.equal(h.navigations.at(-1).result.values, runner.state.metricData.Degree);
    assert.equal(runner.ended.length, 1);
});

test("failed, cancelled, aborted and stale real metric requests never satisfy a tutorial action", async () => {
    for (const reason of ["failure", "cancelled", "abort", "graph replaced", "graph edited"]) {
        const h = setup(); let resolve, reject;
        const runner = metricRunner(h, new Promise((done, fail) => { resolve = done; reject = fail; }));
        const before = runner.state.metricData.Degree, request = runner.run();
        if (reason === "abort") runner.signal.aborted = true;
        if (reason === "graph replaced") { runner.state.graph = {}; h.context.graphKey = runner.state.graph; }
        if (reason === "graph edited") { runner.state.networkRevision++; h.context.graphRevision++; }
        if (reason === "failure") reject(new Error("Server failed"));
        else resolve(reason === "cancelled" ? { cancelled: true } : { label: "Degree", values: { a: 9 }, average: 9 });
        await request; h.ui.move(1);
        assert.equal(h.api.createProgress(h.storage).index("metrics"), 1, reason);
        assert.equal(runner.state.metricData.Degree, before, reason);
        assert.equal(runner.ended.length, 1, reason);
    }
});

test("network tutorials require a graph and recommendation requires a simple graph", () => {
    const h = setup();
    for (const course of h.api.courses.filter(course => course.needsGraph)) assert.equal(h.ui.start(course.id), false);
    assert.equal(h.navigations.length, 0);
    h.ui.openLibrary();
    assert.match(h.library().innerHTML, /Load a network first/);
    h.context.hasGraph = true; h.context.multigraph = true;
    assert.equal(h.ui.start("recommend"), false);
    assert.equal(h.ui.start("network"), true);
    assert.equal(h.navigations.at(-1).tab, "network");
});

test("a paused tour resumes after reload, changes Nodi's expression, and finishes explicitly", () => {
    const h = setup();
    assert.equal(h.ui.start("welcome"), true);
    assert.match(h.guide().innerHTML, /nodi-welcome.png/);
    h.ui.move(-1); assert.equal(h.navigations.at(-1).title, "Hi, I’m Nodi!");
    h.ui.move(1); assert.match(h.guide().innerHTML, /nodi-reassure.png/);
    h.ui.closeGuide(); assert.equal(h.guide().hidden, true);
    const reloaded = setup(h.storage);
    reloaded.ui.start("welcome");
    assert.equal(reloaded.navigations.at(-1).target, "#btn-session");
    reloaded.ui.move(1); assert.match(reloaded.guide().innerHTML, /nodi-compute.png/);
    reloaded.ui.move(1);
    assert.equal(reloaded.api.createProgress(h.storage).completed("welcome"), false);
    reloaded.ui.move(1);
    assert.equal(reloaded.api.createProgress(h.storage).completed("welcome"), true);
    assert.match(reloaded.guide().innerHTML, /nodi-celebrate.png/);
    reloaded.ui.start("welcome");
    assert.equal(reloaded.navigations.at(-1).title, "Hi, I’m Nodi!");
});

test("motion can be paused and resumed without advancing or restarting a tutorial", () => {
    const h = setup(); h.ui.start("welcome"); h.ui.move(1);
    const before = h.navigations.length;
    assert.equal(h.guide().dataset.nodiMotion, "on");
    assert.match(h.guide().innerHTML, /data-nodi-face="reassure"/);
    clickMotion(h.guide());
    assert.equal(h.guide().dataset.nodiMotion, "off");
    assert.equal(h.nodes.get("nodi-welcome").dataset.nodiMotion, "off");
    assert.equal(h.guide().motionButton.attrs["aria-label"], "Animate Nodi");
    assert.equal(h.navigations.length, before);
    assert.equal(h.api.createProgress(h.storage).index("welcome"), 1);
    const reloaded = setup(h.storage);
    reloaded.ui.start("welcome");
    assert.equal(reloaded.guide().dataset.nodiMotion, "off");
    assert.equal(reloaded.navigations.at(-1).target, "#btn-session");
    clickMotion(reloaded.guide());
    assert.equal(reloaded.guide().dataset.nodiMotion, "on");
    assert.equal(reloaded.guide().motionButton.attrs["aria-label"], "Pause Nodi");
    reloaded.ui.openLibrary();
    clickMotion(reloaded.library());
    assert.equal(reloaded.library().dataset.nodiMotion, "off");
});

test("system reduced motion takes precedence and updates an open guide immediately", () => {
    const h = setup(memoryStorage(), { reducedMotion: true });
    h.ui.start("welcome");
    assert.equal(h.guide().dataset.nodiMotion, "off");
    assert.equal(h.guide().motionButton.disabled, true);
    assert.equal(h.guide().motionButton.attrs["aria-label"], "Motion reduced");
    clickMotion(h.guide()); // Even a synthetic event cannot override the system setting.
    assert.equal(h.storage.getItem("relison-nodi-motion-v1"), null);
    h.reducedMotion.matches = false; h.listeners.motionChange();
    assert.equal(h.guide().dataset.nodiMotion, "on");
    assert.equal(h.guide().motionButton.disabled, false);
    clickMotion(h.guide());
    h.reducedMotion.matches = true; h.listeners.motionChange();
    h.reducedMotion.matches = false; h.listeners.motionChange();
    assert.equal(h.guide().dataset.nodiMotion, "off", "manual pause survives system preference changes");
});

test("motion controls still work when saving preferences is blocked", () => {
    const h = setup({ getItem() { throw Error("blocked"); }, setItem() { throw Error("blocked"); } });
    h.ui.start("welcome"); clickMotion(h.guide());
    assert.equal(h.guide().dataset.nodiMotion, "off");
    h.ui.move(1);
    assert.equal(h.guide().dataset.nodiMotion, "off");
    clickMotion(h.guide());
    assert.equal(h.guide().dataset.nodiMotion, "on");
});

test("manual workspace navigation hides the highlight and offers a return to the current step", () => {
    const h = setup(); h.context.hasGraph = true;
    h.ui.start("network"); h.frame();
    assert.equal(h.nodes.get("nodi-highlight").hidden, false);
    h.context.tab = "tables"; h.ui.contextChanged(); h.frame();
    assert.equal(h.nodes.get("nodi-highlight").hidden, true);
    assert.match(h.guide().innerHTML, /Show this step/);
    h.ui.move(1); assert.equal(h.context.tab, "network");
    assert.equal(h.navigations.at(-1).target, "#layout-type");
});

test("unavailable views are gated again before advancing a running tour", () => {
    const h = setup(); h.context.hasGraph = true;
    h.ui.start("recommend");
    h.context.multigraph = true;
    const before = h.navigations.length;
    h.ui.move(1);
    assert.equal(h.navigations.length, before);
    assert.equal(h.library().open, true);
    assert.match(h.library().innerHTML, /Requires a simple graph/);
    assert.equal(h.guide().hidden, true);
});

test("Escape closes a tour, but leaves another open dialog in control", () => {
    const h = setup(); h.ui.start("welcome");
    h.library().open = true;
    h.listeners.keydown({ key: "Escape", preventDefault() {} });
    assert.equal(h.guide().hidden, false);
    h.library().open = false;
    h.listeners.keydown({ key: "Escape", preventDefault() {} });
    assert.equal(h.guide().hidden, true);
});

test("malformed, stale, out-of-range and blocked storage do not break tutorials", () => {
    const { api } = setup();
    for (const value of ["bad json", "null", "[]", '{"progress":{"welcome":99},"completed":"welcome"}',
        '{"progress":{"welcome":-1,"network":"2","unknown":0},"completed":["unknown"]}']) {
        const progress = api.createProgress(memoryStorage(value));
        assert.equal(progress.index("welcome"), 0);
        assert.equal(progress.completed("welcome"), false);
    }
    const storage = { getItem() { throw Error("blocked"); }, setItem() { throw Error("blocked"); } };
    const progress = api.createProgress(storage);
    progress.save("welcome", 2); assert.equal(progress.index("welcome"), 2);
    progress.save("welcome", 99); assert.equal(progress.index("welcome"), 2);
    progress.finish("welcome"); assert.equal(progress.completed("welcome"), true);
});

test("all tutorial targets exist in the UI and every referenced Nodi asset has alpha", () => {
    const { api } = setup();
    const html = fs.readFileSync(path.join(publicDir, "index.html"), "utf8");
    const dynamicIds = ["vertex-metric", "node-analysis-distributions"];
    const faces = new Set(["welcome", "celebrate"]), ids = new Set();
    for (const course of api.courses) {
        assert.ok(!ids.has(course.id), "unique tutorial ID"); ids.add(course.id);
        for (const step of course.steps) {
            faces.add(step.face);
            if (step.target.startsWith("#")) {
                const id = step.target.slice(1);
                assert.ok(dynamicIds.includes(id) || html.includes(`id="${id}"`), `${course.id}: ${step.target}`);
            } else assert.ok(html.includes(step.target.slice(1)), step.target);
        }
    }
    assert.equal(ids.size, 10);
    for (const face of faces) {
        const bytes = fs.readFileSync(path.join(publicDir, `img/nodi-2d/nodi-${face}.png`));
        assert.equal(bytes.subarray(1, 4).toString(), "PNG");
        assert.equal(bytes[25], 6, "RGBA PNG with transparency");
    }
    const script = html.indexOf('src="nodi-tutorials.js"');
    assert.ok(script > 0 && script < html.indexOf('src="app.js"'));
});

test("the app adapter opens the intended views without changing the loaded graph", () => {
    const app = fs.readFileSync(path.join(publicDir, "app.js"), "utf8");
    const start = app.lastIndexOf("let nodiStorage;");
    assert.ok(start > 0);
    const graph = { original: true }, calls = [], values = { a: 3 }, view = { mode: "summary", metric: "Old", distributionSources: ["rec:old"] }, rankingDetails = { open: false };
    const context = { state: { activeTab: "import", graph, multigraph: false, metricData: { Degree: values } }, metricWorkspaceComputeOpen: false,
        debugConsole: { on: true }, toggleDebugView() { calls.push("hide-debug"); context.debugConsole.on = false; },
        switchTab(tab) { calls.push(["tab", tab]); context.state.activeTab = tab; },
        switchTableSubtab(view) { calls.push(["table", view]); },
        switchSubtab(view) { calls.push(["metric", view]); },
        switchDiffSubtab(view) { calls.push(["diffusion", view]); },
        switchRecSubtab(view) { calls.push(["recommendation", view]); },
        METRIC_WORKSPACE_VIEWS: new Map([["nodes", view]]),
        refreshMetricWorkspace() { calls.push("metric-results"); },
        $(id) { return { closest: () => rankingDetails, click() { calls.push(["click", id]); if (id === "metric-compute-toggle") context.metricWorkspaceComputeOpen = true; } }; },
        NodiTutorials: { init(options) { context.options = options; } },
        // Reading localStorage itself may be blocked, before calling getItem.
        get localStorage() { throw Error("disabled"); },
    };
    vm.runInNewContext(app.slice(start), context);
    assert.equal(context.options.storage, undefined);
    assert.equal(context.options.getContext().hasGraph, true);
    context.options.navigate({ tab: "metrics", view: "compute-nodes" });
    assert.ok(calls.some(call => call[0] === "click" && call[1] === "metric-compute-toggle"));
    const before = calls.filter(call => call[0] === "click").length;
    context.options.navigate({ tab: "metrics", view: "compute-nodes" });
    assert.equal(calls.filter(call => call[0] === "click").length, before, "already-open panel stays open");
    context.options.navigate({ tab: "tables", view: "nodes" });
    context.options.navigate({ tab: "diffusion", view: "pieces" });
    context.options.navigate({ tab: "reports" });
    context.options.navigate({ tab: "metrics", view: "node-summary", result: { label: "Degree", values } });
    assert.equal(view.mode, "summary"); assert.equal(view.metric, "Degree");
    assert.equal(view.statisticsSource, "original");
    context.options.navigate({ tab: "metrics", view: "node-rankings", result: { label: "Degree", values } });
    assert.equal(view.mode, "rankings"); assert.equal(view.metric, "Degree");
    assert.equal(view.rankingSource, "original");
    context.options.navigate({ tab: "metrics", view: "node-ranking-table", result: { label: "Degree", values } });
    assert.equal(view.mode, "rankings"); assert.equal(rankingDetails.open, true);
    context.options.navigate({ tab: "metrics", view: "node-distributions", result: { label: "Degree", values } });
    assert.equal(view.mode, "distributions"); assert.equal(view.metric, "Degree");
    assert.deepEqual(Array.from(view.distributionSources), ["original"]);
    assert.equal(view.type, "histogram");
    assert.equal(context.state.metricData.Degree, values);
    assert.ok(calls.some(call => call[0] === "table" && call[1] === "nodes"));
    assert.ok(calls.some(call => call[0] === "diffusion" && call[1] === "pieces"));
    assert.ok(calls.some(call => call[0] === "click" && call[1] === "btn-report"));
    assert.equal(context.state.graph, graph);
});
