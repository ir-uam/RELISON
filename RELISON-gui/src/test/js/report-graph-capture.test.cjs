const test = require("node:test"), assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), vm = require("node:vm");
const source = fs.readFileSync(path.join(__dirname, "../../main/resources/public/report-graph-capture.js"), "utf8");
const app = fs.readFileSync(path.join(__dirname, "../../main/resources/public/app.js"), "utf8");

function harness() {
    const control = () => ({ hidden: false, disabled: false, textContent: "", events: {},
        addEventListener(name, handler) { this.events[name] = handler; }, focus() {},
        click() { return this.events.click(); } });
    const bar = control(), captureButton = control(), cancelButton = control(), message = control();
    const state = { graph: {}, view: "sigma", network: false, captured: [], opened: [], frame: () => Promise.resolve({ image: 1 }) };
    const context = vm.createContext({ setTimeout, clearTimeout });
    const api = vm.runInContext(source + "\nReportGraphCapture", context);
    const controller = api.create({ bar, captureButton, cancelButton, message,
        getGraph: () => state.graph, getViewKey: () => state.view, isNetworkActive: () => state.network,
        openNetwork: () => { state.network = true; }, openReport: target => { state.network = false; state.opened.push(target); },
        captureView: () => state.frame(), complete: (target, snapshot) => { state.captured.push({ target, snapshot }); return true; } });
    return { bar, captureButton, cancelButton, message, controller, state, api };
}

test("capture returns a snapshot to the requested cell; cancel leaves it untouched", async () => {
    const h = harness(), target = { reportId: 2, cellId: 5, reportTitle: "Example" };
    assert.equal(h.bar.hidden, true);
    h.controller.start(target);
    assert.equal(h.bar.hidden, false); assert.match(h.message.textContent, /Example/);
    await h.captureButton.click();
    assert.deepEqual(h.state.captured, [{ target, snapshot: { image: 1 } }]);
    assert.deepEqual(h.state.opened, [target]); assert.equal(h.bar.hidden, true);
    h.controller.start(target); h.cancelButton.click();
    assert.equal(h.state.captured.length, 1); assert.equal(h.state.opened.length, 2);
});

test("capture rejects stale and failed results and allows retry", async () => {
    const h = harness(), target = { reportId: 3, cellId: 1 };
    h.state.graph = null; h.controller.start(target);
    assert.equal(h.captureButton.disabled, true);
    assert.match(h.message.textContent, /Load a network first/);
    h.state.graph = {}; h.controller.sync(); assert.equal(h.captureButton.disabled, false);
    assert.match(h.message.textContent, /Adjust the network view/);
    let resolve;
    h.state.frame = () => new Promise(done => { resolve = done; });
    const pending = h.captureButton.click(); assert.equal(h.captureButton.disabled, true);
    h.state.view = "geographic"; resolve({ image: 1 }); await pending;
    assert.equal(h.state.captured.length, 0); assert.match(h.message.textContent, /changed during capture/);
    h.state.frame = () => Promise.reject(new Error("renderer unavailable")); await h.captureButton.click();
    assert.match(h.message.textContent, /renderer unavailable/); assert.equal(h.captureButton.disabled, false);
    h.state.frame = () => Promise.resolve({ image: 2 }); await h.captureButton.click();
    assert.equal(h.state.captured[0].snapshot.image, 2);
});

test("cancelling or starting a different request ignores an in-flight capture", async () => {
    const h = harness(); let resolve;
    h.state.frame = () => new Promise(done => { resolve = done; });
    h.controller.start({ reportId: 1, cellId: 1 });
    const old = h.captureButton.click(); h.cancelButton.click();
    h.controller.start({ reportId: 2, cellId: 4 });
    resolve({ image: 1 }); await old;
    assert.equal(h.state.captured.length, 0); assert.equal(h.bar.hidden, false);
    h.state.frame = () => Promise.resolve({ image: 2 }); await h.captureButton.click();
    assert.equal(h.state.captured[0].target.reportId, 2);
});

test("sigma captures its layers and overlay during afterRender and removes its listener", async () => {
    const layers = [{ width: 200, height: 100, id: "edges" }, { width: 200, height: 100, id: "nodes" }];
    const overlay = { width: 200, height: 100, id: "overlay" }, drawn = [];
    const context = vm.createContext({ setTimeout, clearTimeout, document: { createElement: () => ({
        getContext: () => ({ fillRect: () => drawn.push("background"), drawImage: layer => drawn.push(layer.id) }) }) } });
    const api = vm.runInContext(source + "\nReportGraphCapture", context);
    let callback, detached = false;
    const renderer = { on: (_, fn) => { callback = fn; }, off: (_, fn) => { detached = fn === callback; }, refresh: () => callback() };
    const canvas = await api.sigmaFrame({ renderer, container: { querySelectorAll: () => layers }, overlay,
        drawOverlay: () => drawn.push("refresh overlay"), background: "white" });
    assert.equal(canvas.width, 200); assert.equal(canvas.height, 100);
    assert.deepEqual(drawn, ["background", "edges", "nodes", "refresh overlay", "overlay"]); assert.equal(detached, true);
});

test("the network capture uses the active renderer and passes the visible-legend setting", async () => {
    const calls = [], canvas = { width: 640, height: 480, toDataURL: type => `data:${type};base64,AAAA` };
    const controls = { "network-dimensions": { value: "3" }, "export-include-legend": { checked: true },
        "sigma-container": { clientWidth: 320 }, "rec-overlay": {} };
    const state = { graph: {}, activeTab: "network", renderer: {} };
    let renderer = "sigma";
    const context = vm.createContext({ state, $: id => controls[id], geographicActiveProjection: "globe",
        usingGeographic: () => renderer === "geographic", usingCosmograph: () => renderer === "cosmograph",
        networkLegendData: () => ({ type: "group" }), cssVar: () => "#111111", drawRecOverlay() {},
        ReportGraphCapture: { sigmaFrame: async args => { calls.push(["sigma", args]); return canvas; } },
        window: { relisonGeographic: { captureFrame: async settings => { calls.push(["geographic", settings]); return canvas; } },
            relisonCosmograph: { captureRotationFrame: async legend => { calls.push(["cosmograph", legend]); return canvas; } },
            relisonGeographicExport: { canvasLegend: async (...args) => calls.push(["legend", ...args]) } } });
    const fn = vm.runInContext(app.slice(app.indexOf("async function captureReportNetworkView()"), app.indexOf("\nReportNotebook.init(", app.indexOf("async function captureReportNetworkView()"))) + "\ncaptureReportNetworkView", context);
    const sigma = await fn(); assert.equal(sigma.renderer, "sigma"); assert.equal(sigma.width, 640); assert.equal(sigma.dataUrl, "data:image/png;base64,AAAA");
    assert.deepEqual(calls.map(item => item[0]), ["sigma", "legend"]); assert.equal(calls[1][3], 2);
    calls.length = 0; renderer = "cosmograph"; const cosmo = await fn();
    assert.equal(cosmo.renderer, "cosmograph:3"); assert.equal(calls[0][1].type, "group");
    calls.length = 0; renderer = "geographic"; controls["export-include-legend"].checked = false;
    const map = await fn(); assert.equal(map.renderer, "geographic:globe"); assert.equal(calls[0][1].includeLegend, false);
    state.graph = null; await assert.rejects(fn(), /Load and show a network/);
});
