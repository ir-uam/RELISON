const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const panelSource = fs.readFileSync(path.join(__dirname, "../../main/resources/public/process-panel.js"), "utf8");
const appSource = fs.readFileSync(path.join(__dirname, "../../main/resources/public/app.js"), "utf8");

function setup() {
    const nodes = new Map(), requests = [], ready = [], intervals = [];
    let now = 1000;
    function node(tag = "div") {
        return { tag, textContent: "", children: [], dataset: {}, attrs: {}, listeners: {}, hidden: true,
            classList: { toggle() {}, contains() { return false; } },
            addEventListener(event, handler) { this.listeners[event] = handler; },
            setAttribute(name, value) { this.attrs[name] = value; }, append(...children) { this.children.push(...children); },
            replaceChildren(...children) { this.children = children; }, focus() { this.focused = true; },
            querySelector() { return node("span"); } };
    }
    const $ = id => { if (!nodes.has(id)) nodes.set(id, node()); return nodes.get(id); };
    function descendants(item) { return [item, ...item.children.flatMap(descendants)]; }
    const context = vm.createContext({ console, performance: { now: () => now },
        document: { createElement: node, getElementById: $, addEventListener: (_event, callback) => ready.push(callback),
            querySelectorAll: selector => [...nodes.values()].flatMap(descendants).filter(item => item.className === selector.slice(1)) },
        localStorage: { getItem() { return null; }, setItem() {} },
        fetch: async (url, options) => { requests.push({ url, options }); return { ok: true, json: async () => url === "/api/jobs" ? { jobs: context.serverJobs || [] } : { cancelled: true } }; },
        setTimeout() { return 1; }, clearTimeout() {}, setInterval(callback) { intervals.push(callback); },
        crypto: { randomUUID: () => "11111111-1111-4111-8111-111111111111" }, AbortController,
        state: { graphId: "original-graph", jobs: {} }, setStatus() {}, $,
        jsonBody: body => ({ method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) }) });
    const panel = vm.runInContext(panelSource + "\nProcessPanel", context);
    return { panel, context, $, requests, ready, intervals, advance(ms) { now += ms; } };
}
function appFunction(name) {
    const start = appSource.indexOf("function " + name + "("), end = appSource.indexOf("\n}", start) + 2;
    assert.ok(start >= 0 && end > start);
    return "(" + appSource.slice(appSource.slice(start - 6, start) === "async " ? start - 6 : start, end) + ")";
}

test("progress text distinguishes known totals, turn counts and unavailable progress", () => {
    const { panel } = setup();
    assert.match(panel.progressText({ completed: 4, total: 10, unit: "nodes" }), /4 \/ 10 nodes/);
    assert.equal(panel.progressText({ completed: 17, total: -1, unit: "turns" }), "17 turns completed");
    assert.match(panel.progressText({ completed: 0, total: -1, unit: "" }), /unavailable/);
    assert.equal(panel.duration(61234), "1m 1s");
});

test("server jobs reappear on reload and elapsed time advances locally", async () => {
    const h = setup();
    h.context.serverJobs = [{ id: "job", label: "Degree <unsafe>", graphId: "original-graph", kind: "vertex", state: "running",
        phase: "Computing nodes", completed: 4, total: 10, unit: "nodes", elapsedMillis: 1000, queueMillis: 0,
        startedAt: 1, endedAt: 0, cancellable: true }];
    h.ready[0](); await h.panel.refresh(); await new Promise(resolve => setImmediate(resolve));
    assert.equal(h.$("processes-count").textContent, "1");
    const card = h.$("processes-list").children[0];
    assert.equal(card.children[0].children[0].textContent, "Degree <unsafe>");
    assert.equal(card.children[0].children[1].children[0].textContent, "Metric");
    assert.match(card.children[0].children[1].children[0].className, /type-metric/);
    assert.equal(card.children[0].children[1].children[0].children.length, 0);
    const footer = card.children.find(child => child.className === "process-footer");
    assert.equal(footer.children[1].className, "process-cancel icon-btn");
    assert.equal(footer.children[1].children[0].textContent, "cancel");
    h.advance(3000); h.intervals[0](); assert.equal(footer.children[0].textContent, "4s running");
    await footer.children[1].listeners.click();
    const request = h.requests.find(request => request.url === "/api/jobs/cancel");
    assert.deepEqual(JSON.parse(request.options.body), { jobId: "job", graphId: "original-graph" });
});

test("processes show the current graph by default and include other graphs on request", async () => {
    const h = setup();
    const job = (id, graphId) => ({ id, label: id, graphId,
        kind: id === "other" ? "recommendation" : "vertex",
        parameters: id === "other" ? { mode: "prediction" } : {}, state: "running",
        phase: "Computing", completed: 1, total: 10, unit: "nodes", elapsedMillis: 0, queueMillis: 0,
        startedAt: 1, endedAt: 0, cancellable: true });
    h.context.serverJobs = [job("current", "original-graph"), job("other", "another-graph")];
    h.ready[0](); await h.panel.refresh(); await new Promise(resolve => setImmediate(resolve));
    assert.equal(h.$("processes-count").textContent, "1");
    assert.equal(h.$("processes-list").children.length, 1);
    const allGraphs = h.$("processes-all-graphs"); allGraphs.checked = true;
    allGraphs.listeners.change();
    assert.equal(h.$("processes-count").textContent, "2");
    assert.equal(h.$("processes-list").children.length, 2);
    const otherCard = h.$("processes-list").children.find(card => card.dataset.jobId === "other");
    assert.equal(otherCard.children[0].children[1].children[0].textContent, "Prediction");
    assert.match(otherCard.children[0].children[1].children[0].className, /type-prediction/);
    const typeFilter = h.$("process-filter-type"); typeFilter.value = "Prediction"; typeFilter.listeners.change();
    assert.equal(h.$("processes-count").textContent, "1");
    assert.equal(h.$("processes-list").children[0].dataset.jobId, "other");
    const statusFilter = h.$("process-filter-status"); statusFilter.value = "completed"; statusFilter.listeners.change();
    assert.equal(h.$("processes-count").textContent, "0");
    assert.match(h.$("processes-list").children[0].textContent, /No processes match these filters/);
});

test("process type labels cover layout, recommendation, prediction, method and diffusion", () => {
    const { panel } = setup();
    assert.equal(panel.processType({ kind: "layout" }), "Layout");
    assert.equal(panel.processType({ kind: "recommendation", parameters: { mode: "recommendation" } }), "Recommendation");
    assert.equal(panel.processType({ kind: "recommendation", parameters: { mode: "prediction" } }), "Prediction");
    assert.equal(panel.processType({ kind: "community" }), "Method");
    assert.equal(panel.processType({ kind: "diffusion" }), "Diffusion");
});

test("tracked requests get unique identity and Stop keeps the request alive through cleanup", async () => {
    const h = setup();
    const api = vm.runInContext(appFunction("api"), h.context);
    const cancel = vm.runInContext(appFunction("cancelJob"), h.context);
    const controller = new AbortController();
    const job = { buttonId: "button", signature: "test", controller, kind: "vertex", graphId: "original-graph" };
    controller.signal.jobKey = "button:job";
    h.context.state.jobs[controller.signal.jobKey] = job;
    h.context.jobConfiguration = () => "test";
    h.context.selectedJob = btnId => Object.values(h.context.state.jobs)
        .find(candidate => candidate.buttonId === btnId && candidate.signature === h.context.jobConfiguration(btnId));
    await api("/api/metrics/vertex", { ...h.context.jsonBody({ graphId: "original-graph", metric: "Degree" }), signal: controller.signal });
    const request = h.requests.find(request => request.url === "/api/metrics/vertex");
    assert.equal(request.options.headers["X-Relison-Job-ID"], job.id);
    h.context.state.graphId = "different-graph";
    await cancel("button");
    assert.equal(controller.signal.aborted, false);
    const cancellation = h.requests.find(request => request.url === "/api/jobs/cancel");
    assert.equal(JSON.parse(cancellation.options.body).graphId, "original-graph");
});

test("failed cancellation is reported instead of claiming the worker stopped", async () => {
    const h = setup(); h.context.fetch = async () => ({ ok: false });
    await assert.rejects(h.panel.cancelJob("job", "graph"), /Cancellation request failed/);
});

test("results from a previous graph cannot be applied after switching graphs", async () => {
    const h = setup();
    const api = vm.runInContext(appFunction("api"), h.context);
    const originalFetch = h.context.fetch;
    h.context.fetch = async (url, options) => {
        const response = await originalFetch(url, options);
        if (url === "/api/metrics/vertex") h.context.state.graphId = "different-graph";
        return response;
    };
    await assert.rejects(api("/api/metrics/vertex", h.context.jsonBody({ graphId: "original-graph" })), { name: "AbortError" });
});
