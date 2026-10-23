const test = require("node:test"), assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), vm = require("node:vm");
const read = name => fs.readFileSync(path.join(__dirname, "../../main/resources/public", name), "utf8");
const plain = value => JSON.parse(JSON.stringify(value));

// Small DOM fixture: exercise the real diffusion markup and event wiring.
class Element {
    constructor(tag) { this.tagName = tag; this.children = []; this.attrs = {}; this.dataset = {}; this.handlers = {}; this.style = {}; this.className = ""; this._value = ""; this._text = ""; }
    get parentElement() { return this.parentNode; }
    get options() { return this.children.filter(child => child.tagName === "option"); }
    get classList() { return { add: name => this.className += " " + name, contains: name => this.className.split(/\s+/).includes(name), toggle: (name, on) => { this.className = this.className.split(/\s+/).filter(x => x !== name).join(" ") + (on ? " " + name : ""); } }; }
    get value() { return this.tagName === "select" ? (this.children.find(x => x.value === this._value) || this.children[0])?.value || "" : this._value; }
    set value(value) { this._value = String(value); }
    get textContent() { return this._text + this.children.map(x => x.textContent).join(""); }
    set textContent(value) { this.replaceChildren(); this._text = String(value); }
    set innerHTML(value) { assert.equal(value, ""); this.replaceChildren(); }
    setAttribute(name, value) { this.attrs[name] = value; if (name === "class") this.className = value; if (name === "id") this.id = value; if (name.startsWith("data-")) this.dataset[name.slice(5).replace(/-([a-z])/g, (_, x) => x.toUpperCase())] = value; }
    getAttribute(name) { return this.attrs[name]; }
    remove() { if (this.parentNode) this.parentNode.children.splice(this.parentNode.children.indexOf(this), 1); this.parentNode = null; }
    appendChild(child) { child.remove(); child.parentNode = this; this.children.push(child); return child; }
    append(...children) { children.forEach(child => this.appendChild(child)); }
    prepend(child) { child.remove(); child.parentNode = this; this.children.unshift(child); }
    after(child) { child.remove(); child.parentNode = this.parentNode; this.parentNode.children.splice(this.parentNode.children.indexOf(this) + 1, 0, child); }
    before(child) { child.remove(); child.parentNode = this.parentNode; this.parentNode.children.splice(this.parentNode.children.indexOf(this), 0, child); }
    replaceChildren(...children) { this.children.forEach(x => x.parentNode = null); this.children = []; this._text = ""; this.append(...children); }
    matches(selector) {
        if (selector.includes(",")) return selector.split(",").some(s => this.matches(s.trim()));
        if (selector.includes(" ")) { const parts = selector.split(/\s+/), last = parts.pop(); return this.matches(last) && !!this.parentNode?.closest(parts.join(" ")); }
        if (selector.startsWith(".")) return this.classList.contains(selector.slice(1));
        const attr = selector.match(/^\[([^=]+)="([^"]*)"\]$/);
        return attr ? this.getAttribute(attr[1]) === attr[2] : this.tagName === selector;
    }
    closest(selector) { return this.matches(selector) ? this : this.parentNode?.closest(selector); }
    querySelectorAll(selector) { return this.children.flatMap(child => [...(child.matches(selector) ? [child] : []), ...child.querySelectorAll(selector)]); }
    querySelector(selector) { return this.querySelectorAll(selector)[0] || null; }
    addEventListener(event, fn) { (this.handlers[event] ||= []).push(fn); }
    async change(value) { this.value = value; await Promise.all((this.handlers.change || []).map(fn => fn({ target: this }))); }
    async click() { if (!this.disabled) await Promise.all((this.handlers.click || []).map(fn => fn({ target: this }))); }
}

function harness(api) {
    const root = new Element("main"), stack = [root], html = read("index.html");
    const section = html.slice(html.indexOf('<div class="tabpane" id="pane-diffusion">'), html.indexOf('<script src="pdf.js">'));
    for (const token of section.match(/<!--[\s\S]*?-->|<[^>]+>|[^<]+/g)) {
        if (token.startsWith("<!--")) continue;
        if (token.startsWith("</")) { if (stack.length > 1) stack.pop(); continue; }
        if (!token.startsWith("<")) { stack.at(-1)._text += token.trim(); continue; }
        const tag = token.match(/^<([\w-]+)/)?.[1]; if (!tag) continue;
        const node = new Element(tag);
        for (const [, name, value] of token.matchAll(/([\w-]+)="([^"]*)"/g)) { node.setAttribute(name, value); if (["value", "type", "min", "max"].includes(name)) node[name] = value; }
        if (/\shidden(?:\s|>)/.test(token)) node.hidden = true;
        stack.at(-1).append(node);
        if (!/\/>$/.test(token) && !["input", "br", "hr"].includes(tag)) stack.push(node);
    }
    const all = node => [node, ...node.children.flatMap(all)], $ = id => all(root).find(x => x.id === id);
    const document = { createElement: tag => new Element(tag), querySelector: selector => root.querySelector(selector), querySelectorAll: selector => root.querySelectorAll(selector) };
    const state = { graphId: "g", attrSchema: { node: [{ name: "group", type: "string", numeric: false }] }, diffusion: { runs: [], result: { runId: "b" }, iteration: 0, selectedNode: "n", selectedPiece: "p", pieces: [{ features: [{ param: "score", value: "1" }, { param: "topic", value: "news" }] }] } };
    const renders = [], downloads = [], requests = [];
    const context = vm.createContext({ document, $, state, crypto: require("node:crypto").webcrypto,
        echartColors: () => ({ palette: ["#3778bf", "#cc7733", "#44aa88"], border: "#888", text: "#222", muted: "#666", lineWidth: 2, gridOpacity: 0.4 }),
        chartBase: () => ({}), fmt: value => Number.isFinite(value) ? String(value) : "—", escapeChartHtml: value => String(value),
        option: (value, label) => { const o = new Element("option"); o.value = value; o.textContent = label; return o; },
        toggleHidden: (id, hide) => $(id).hidden = hide, knownFeatureParams: () => ({ info: [...new Set(state.diffusion.pieces.flatMap(piece => (piece.features || []).map(feature => feature.param)))], user: [...new Set([...state.attrSchema.node.map(item => item.name), ...Object.keys(state.communityData || {})])] }),
        showEChart: (id, config) => renders.push({ id, config }), clearChart: id => renders.push({ id, cleared: true }), ECHARTS: new Map(),
        download: (...args) => downloads.push(args), csvCell: value => JSON.stringify(value ?? ""), downloadChartPng() {}, setStatus() {},
        activateDiffusionAnalysisRun: result => { state.diffusion.result = result; },
        jsonBody: body => ({ body: JSON.stringify(body) }),
        api: async (url, options) => { const body = JSON.parse(options.body); requests.push({ url, body }); return api ? api(url, body) : {}; },
    });
    for (const file of ["metric-workspace.js", "diffusion-charts.js", "diffusion-workspace.js"]) vm.runInContext(read(file), context);
    for (const name of ["switchStatsView", "renderActiveStatsView"]) {
        const source = read("app.js"), start = source.indexOf("function " + name + "(");
        vm.runInContext(source.slice(start, source.indexOf("\n}", start) + 2), context);
    }
    const workspace = vm.runInContext("DiffusionWorkspace", context); workspace.setup();
    function run(id, values = [0, 2, 4]) {
        const result = { id, label: "Run " + id, network: "Original", numIterations: values.length, metrics: [{ id: "m", label: "Metric", values }, { id: "x", label: "Other", values: [3, null, 5] }] };
        state.diffusion.runs.push(result); workspace.addRun(id); return result;
    }
    const host = key => $("diff-stats-view-" + key);
    const control = (key, label) => host(key).querySelectorAll("label").find(x => x.children[0]?.textContent === label)?.querySelector("select");
    return { $, context, state, workspace, renders, downloads, requests, run, host, control };
}

test("real diffusion markup becomes one analysis workspace with selectable runs and compact exports", async () => {
    const h = harness(); h.run("a"); h.run("b"); h.context.switchStatsView("metrics");
    assert.equal(h.$("diff-view-metrics").hidden, true);
    assert.equal(h.host("metrics").hidden, false);
    assert.equal(h.$("diff-analysis-runs").querySelectorAll("input").length, 2);
    assert.equal(h.renders.at(-1).config.series.length, 2);
    assert.equal(h.$("btn-diffmetric-png").parentElement.className, "diff-analysis-actions");
    await h.$("diff-analysis-runs").querySelectorAll("button").find(x => x.textContent === "Deselect all").click();
    assert.equal(h.workspace.selectedRuns().length, 0); assert.equal(h.renders.at(-1).cleared, true);
});

test("comparison aligns iterations, preserves missing values, and excludes undefined percentage changes", async () => {
    const h = harness(); h.run("a", [0, 2, 4]); h.run("b", [1, 3]); h.context.switchStatsView("compare");
    await h.control("compare", "Y metric").change("m"); await h.control("compare", "X metric").change("x");
    assert.deepEqual(plain(h.renders.at(-1).config.series[0].data), [[3, 0], [null, null], [5, 4]]);
    await h.control("compare", "Compare").change("delta"); await h.control("compare", "Change").change("percent");
    assert.deepEqual(plain(h.renders.at(-1).config.series[0].data), [[0, null], [1, 50]]);
    await h.control("compare", "X axis").change("log");
    assert.deepEqual(plain(h.renders.at(-1).config.series[0].data), [[null, null], [1, 50]]);
    assert.match(h.host("compare").textContent, /baseline is zero/);
});

test("multi-run analysis caches completed requests, ignores stale responses, and retries cancelled work", async () => {
    let release, cancelled = true;
    const h = harness((url, body) => body.node === "slow" ? new Promise(resolve => release = resolve) : body.node === "cancel" && cancelled ? { cancelled: true } : { values: ["received"], series: { received: [0, 4] }, iterations: 2 });
    h.run("a"); const body = { node: "n", pieces: true };
    await h.workspace.load("node", "/api/diffusion/trajectory", body);
    await h.workspace.load("node", "/api/diffusion/trajectory", body);
    assert.equal(h.requests.length, 1);
    const slow = h.workspace.load("node", "/api/diffusion/trajectory", { node: "slow" });
    await h.workspace.load("node", "/api/diffusion/trajectory", body);
    release({ values: ["old"], series: { old: [99] }, iterations: 1 }); await slow;
    assert.equal(h.renders.at(-1).config.series[0].name, "received");
    await h.workspace.load("node", "/api/diffusion/trajectory", { node: "cancel" });
    assert.match(h.host("node").textContent, /cancelled/); cancelled = false;
    await h.workspace.load("node", "/api/diffusion/trajectory", { node: "cancel" });
    assert.equal(h.requests.filter(x => x.body.node === "cancel").length, 2);
});

test("weighted numeric distributions preserve totals with exact counts, requested bin widths, and cumulative shares", async () => {
    const h = harness(() => ({ type: "info", feature: "score", values: [{ value: "0", count: 2 }, { value: "1", count: 3 }, { value: "2", count: 1 }] }));
    h.run("a"); await h.workspace.load("dist", "/api/diffusion/distribution", { type: "info", feature: "score" });
    await h.control("dist", "Chart").change("exact");
    assert.deepEqual(plain(h.renders.at(-1).config.series[0].data), [[0, 2], [1, 3], [2, 1]]);
    await h.control("dist", "Chart").change("cdf");
    assert.deepEqual(plain(h.renders.at(-1).config.series[0].data), [[0, 2 / 6], [1, 5 / 6], [2, 1]]);
    const bin = h.host("dist").querySelectorAll("input").find(x => x.type === "number"); bin.value = "2";
    await h.control("dist", "Chart").change("histogram");
    assert.deepEqual(plain(h.renders.at(-1).config.series[0].data), [[1, 5], [3, 1]]);
    assert.equal(h.requests.length, 1);
});

test("distribution bars rotate counts and shares onto X without changing exports or recomputing", async () => {
    const h = harness(() => ({ type: "info", values: [{ value: "0", count: 2 }, { value: "1", count: 3 }, { value: "2", count: 1 }] }));
    h.run("a"); h.$("diff-dist-type").value = "user"; await h.workspace.renderDistribution();
    await h.control("dist", "Bars").change("horizontal");
    let config = h.renders.at(-1).config;
    assert.equal(config.xAxis.name, "Count / weight"); assert.equal(config.yAxis.type, "category");
    assert.deepEqual(plain(config.series[0].data), [[2, "0"], [3, "1"], [1, "2"]]);
    assert.equal(config.dataZoom[0].yAxisIndex, 0);
    await h.control("dist", "Chart").change("shares"); config = h.renders.at(-1).config;
    assert.equal(config.xAxis.name, "Share (%)"); assert.equal(config.series[0].data[1][0], 50);
    h.host("dist").querySelectorAll("input").find(x => x.type === "number").value = "2";
    h.$("diff-dist-type").value = "info";
    await h.control("dist", "Chart").change("histogram"); config = h.renders.at(-1).config;
    assert.deepEqual(plain(config.series[0].data), [[5, "[0, 2)"], [1, "[2, 4)"]]);
    await h.host("dist").querySelectorAll("button").find(x => x.getAttribute("aria-label") === "Export CSV").click();
    assert.match(h.downloads[0][1], /"Run a",0,2,5/);
    const renderer = vm.createContext({ setTimeout, clearTimeout, console }); vm.runInContext(read("vendor/echarts.min.js"), renderer);
    const chart = renderer.echarts.init(null, null, { renderer: "svg", ssr: true, width: 800, height: 420 });
    try { chart.setOption(config); const svg = chart.renderToSVGString(); assert.match(svg, /<svg/); assert.doesNotMatch(svg, /NaN|Infinity/); } finally { chart.dispose(); }
    await h.control("dist", "X axis").change("log"); assert.equal(h.renders.at(-1).config.xAxis.type, "log");
    await h.control("dist", "Chart").change("cdf"); assert.equal(h.control("dist", "Bars").parentElement.hidden, true);
    assert.equal(h.requests.length, 2);
});

test("heatmap is directly selectable and owns the mixed-feature selectors", async () => {
    const h = harness((_url, body) => body.type === "mixed"
        ? { type: "mixed", infoFeature: body.infoFeature, userFeature: body.userFeature, infoValues: ["news"], userValues: ["A"], points: [[0, 0, 3]] }
        : { type: body.type, feature: body.feature, values: [{ value: body.feature === "score" ? "1" : "news", count: 3 }] });
    h.run("a"); await h.workspace.renderDistribution();
    const rail = h.host("dist").querySelector(".metric-analysis-rail"), heatmap = rail.children.find(button => button.dataset.chartType === "heatmap");
    assert.equal(heatmap.disabled, false); assert.equal(h.$("diff-dist-type").options.some(option => option.value === "mixed"), false);
    assert.equal(h.$("diff-dist-info-field").hidden, true); assert.equal(h.$("diff-dist-user-field").hidden, true);
    await heatmap.click();
    assert.equal(h.control("dist", "Chart").value, "heatmap"); assert.equal(h.renders.at(-1).config.series[0].type, "heatmap");
    assert.equal(h.$("diff-dist-type").parentElement.hidden, true); assert.equal(h.$("diff-dist-feat-field").hidden, true);
    assert.equal(h.$("diff-dist-info-field").hidden, false); assert.equal(h.$("diff-dist-user-field").hidden, false);
    assert.equal(h.requests.at(-1).body.type, "mixed"); assert.equal(h.requests.at(-1).body.userFeature, "group");
    assert.deepEqual(h.$("diff-dist-info").options.map(option => option.value), ["score", "topic"]);
    for (const button of rail.children) assert.equal(button.disabled, false);
    await rail.children.find(button => button.dataset.chartType === "histogram").click();
    assert.equal(h.$("diff-dist-type").parentElement.hidden, false); assert.equal(h.$("diff-dist-info-field").hidden, true);
    assert.equal(h.$("diff-dist-user-field").hidden, true); assert.equal(h.requests.at(-1).body.type, "info");
    assert.equal(h.requests.at(-1).body.feature, "score"); assert.equal(h.renders.at(-1).config.series[0].type, "bar");
    await rail.children.find(button => button.dataset.chartType === "categories").click();
    assert.equal(h.$("diff-dist-feature").value, "topic"); assert.equal(h.requests.length, 3);
});

test("distribution feature choices follow numeric types while numeric-looking categories remain categorical", async () => {
    const h = harness(() => ({ type: "info", values: [{ value: "1", count: 2 }] })); h.run("a");
    h.state.attrSchema.node.push({ name: "weight", type: "double", numeric: true }, { name: "code", type: "string", numeric: false });
    h.state.communityData = { community: { n: 1 } };
    h.state.diffusion.pieces.push({ features: [{ param: "mixed", value: "word" }, { param: "mixed", value: "1" }, { param: "number", value: "0.25" }] });
    const choices = () => h.$("diff-dist-feature").options.map(option => option.value);
    for (const chart of ["categories", "shares"]) {
        await h.control("dist", "Chart").change(chart); assert.deepEqual(choices(), ["topic", "mixed"]);
    }
    for (const chart of ["histogram", "cdf", "exact"]) {
        await h.control("dist", "Chart").change(chart); assert.deepEqual(choices(), ["score", "number"]);
        assert.equal(h.requests.at(-1).body.feature, "score");
    }
    h.$("diff-dist-type").value = "user"; await h.workspace.renderDistribution(); assert.deepEqual(choices(), ["weight"]);
    await h.control("dist", "Chart").change("categories"); assert.deepEqual(choices(), ["group", "code", "community"]);
    h.state.attrSchema.node = h.state.attrSchema.node.filter(item => item.name !== "weight");
    const count = h.requests.length; await h.control("dist", "Chart").change("histogram");
    assert.deepEqual(choices(), []); assert.equal(h.$("diff-dist-feature").disabled, true); assert.equal(h.requests.length, count);
    assert.match(h.host("dist").textContent, /No numerical features/); assert.equal(h.renders.at(-1).cleared, true);
});

test("point/line styles and log axes render valid SVG while raw exports retain zero values", async () => {
    const h = harness(); h.run("a"); h.context.switchStatsView("metrics");
    await h.control("metrics", "Display").change("both"); await h.control("metrics", "Y axis").change("log");
    const config = h.renders.at(-1).config;
    assert.equal(config.series[0].showSymbol, true); assert.equal(config.yAxis.type, "log");
    assert.deepEqual(plain(config.series[0].data), [[0, null], [1, 2], [2, 4]]);
    await h.host("metrics").querySelectorAll("button").find(x => x.getAttribute("aria-label") === "Export CSV").click();
    assert.match(h.downloads[0][1], /"Run a","Metric",0,0/);
    const renderer = vm.createContext({ setTimeout, clearTimeout, console }); vm.runInContext(read("vendor/echarts.min.js"), renderer);
    for (const width of [360, 1000]) {
        const chart = renderer.echarts.init(null, null, { renderer: "svg", ssr: true, width, height: 420 });
        try { chart.setOption(config); const svg = chart.renderToSVGString(); assert.match(svg, /<svg/); assert.doesNotMatch(svg, /NaN|Infinity/); } finally { chart.dispose(); }
    }
});

test("rankings retain all entities in CSV and mixed heatmaps export all selected runs", async () => {
    const h = harness(url => url.endsWith("rankings") ? { values: { n: 3, other: 0, third: 2 } } : { type: "mixed", infoFeature: "topic", userFeature: "group", infoValues: ["A", "B"], userValues: ["X", "Y"], matrix: [[1, 2], [3, 4]] });
    h.run("a"); h.run("b"); h.control("node", "View").value = "rankings";
    await h.workspace.rankings("node");
    assert.equal(h.renders.at(-1).config.series.length, 2); assert.equal(h.renders.at(-1).config.series[0].type, "bar");
    await h.host("node").querySelectorAll("button").find(x => x.getAttribute("aria-label") === "Export CSV").click();
    assert.match(h.downloads[0][1], /"other",3,0/);
    // drawHeatmap uses the same shared renderer as the existing diffusion view.
    h.context.clearChart = id => h.renders.push({ id, cleared: true });
    h.control("dist", "Chart").value = "heatmap";
    await h.workspace.load("dist", "/api/diffusion/distribution", { type: "mixed" });
    assert.equal(h.renders.at(-1).config.series[0].type, "heatmap");
    await h.host("dist").querySelectorAll("button").find(x => x.getAttribute("aria-label") === "Export CSV").click();
    assert.equal(h.downloads[1][1].split("\n").length, 9);
});

test("session imports keep metric curves selectable and identify unavailable simulation detail", async () => {
    const h = harness(); h.state.diffusion.runs = [{ label: "Saved run", metrics: [{ id: "m", label: "Metric", values: [1, 2] }], numIterations: 2 }];
    h.workspace.restoreRuns(); h.context.switchStatsView("metrics");
    assert.equal(h.workspace.selectedRuns().length, 1); assert.match(h.$("diff-analysis-runs").textContent, /Metrics only/);
    assert.equal(h.renders.at(-1).config.series[0].data.length, 2);
    await h.workspace.load("node", "/api/diffusion/trajectory", { node: "n" });
    assert.equal(h.requests.length, 0); assert.match(h.host("node").textContent, /Only metric curves/);
    h.state.diffusion.runs = []; h.workspace.reset(); assert.equal(h.workspace.selectedRuns().length, 0);
});

test("sparse heatmaps keep all categories and observed counts while providing axis zoom", async () => {
    const labels = Array.from({ length: 20 }, (_, i) => "v" + i);
    const h = harness(() => ({ type: "mixed", infoValues: labels, userValues: labels, points: [[0, 0, 2], [19, 19, 3]] }));
    h.run("a"); h.control("dist", "Chart").value = "heatmap"; await h.workspace.load("dist", "/api/diffusion/distribution", { type: "mixed" });
    const config = h.renders.at(-1).config;
    assert.equal(config.xAxis.data.length, 20); assert.equal(config.yAxis.data.length, 20);
    assert.equal(config.series[0].data.length, 2); assert.equal(config.dataZoom.length, 2);
    await h.host("dist").querySelectorAll("button").find(x => x.getAttribute("aria-label") === "Export CSV").click();
    assert.equal(h.downloads[0][1].split("\n").length, 3); assert.match(h.downloads[0][1], /"v19","v19",3/);
});

test("overview keeps every run in a nodes-style table with protocol/mechanism configuration and selection", async () => {
    const h = harness(), preset = h.run("a"), custom = h.run("b");
    preset.config = { type: "preset", protocol: { id: "Simple", params: { probability: 0.25 } }, stop: { id: "Turns", params: { count: 3 } }, metrics: [{ id: "m", params: { feature: "topic" } }] };
    custom.recKey = "rec"; custom.network = "Recommendation A";
    custom.config = { type: "custom", protocol: { type: "custom", ...Object.fromEntries(["selection", "expiration", "propagation", "update", "sight"].map(key => [key, { id: key + " mechanism", params: { value: 2 } }])) }, stop: { id: "No new pieces" } };
    h.context.switchStatsView("overview");
    const tbl = h.$("diff-analysis-run-table"), rows = tbl.querySelector("tbody").children;
    assert.equal(tbl.classList.contains("data"), true); assert.equal(rows.length, 2);
    assert.equal(rows[0].children[3].textContent, "Simple [probability=0.25]");
    assert.equal(rows[0].children[4].textContent, ""); assert.equal(rows[1].children[3].textContent, "");
    assert.equal(rows[1].children[4].children.length, 5); assert.match(rows[1].children[4].textContent, /Selectionselection mechanism \[value=2\]/);
    assert.equal(rows[0].children[5].textContent, "Turns [count=3]"); assert.equal(rows[0].children[6].textContent, "3");
    assert.equal(rows[0].children[7].textContent, "Original"); assert.equal(rows[1].children[7].textContent, "Recommendation A");
    const checkbox = rows[1].children[0].querySelector("input"); checkbox.checked = false; await checkbox.change("on");
    assert.equal(h.$("diff-analysis-run-table").querySelector("tbody").children.length, 2);
    assert.equal(h.workspace.selectedRuns().length, 1); assert.equal(h.$("diff-analysis-run-table").querySelector("thead").querySelector("input").indeterminate, true);
    await h.host("overview").querySelectorAll("button").find(x => x.getAttribute("aria-label") === "Export CSV").click();
    assert.match(h.downloads[0][1], /false,.*"Run b"/);
    assert.equal(h.$("diff-analysis-runs").hidden, true);
});

test("overview run names update selectors, legends and exports without changing run identity", async () => {
    const h = harness(); h.run("a"); const run = h.run("b"); run.color = "#12ab34";
    const playbackLabels = []; h.context.syncDiffPlaybackRuns = () => playbackLabels.push(h.state.diffusion.runs.map(run => run.label));
    h.context.switchStatsView("overview");
    const nameInput = () => h.$("diff-analysis-run-table").querySelector("tbody").children[1].querySelectorAll("input").find(input => input.type === "text");
    await nameInput().change("  Revised run  ");
    assert.equal(run.label, "Revised run"); assert.equal(run.id, "b"); assert.equal(run.color, "#12ab34");
    assert.equal(playbackLabels.at(-1)[1], "Revised run");
    assert.equal(h.control("compare", "Baseline run").options[1].textContent, "Revised run");
    assert.ok(h.host("overview").querySelectorAll("button").some(button => button.getAttribute("aria-label") === "Delete Revised run"));
    await h.host("overview").querySelectorAll("button").find(button => button.getAttribute("aria-label") === "Export CSV").click();
    assert.match(h.downloads[0][1], /Revised run/);
    h.context.switchStatsView("metrics"); assert.equal(h.renders.at(-1).config.series[1].name, "Revised run");
    h.context.switchStatsView("overview"); await nameInput().change(" "); assert.equal(run.label, "Revised run");
    await nameInput().change("Run a"); assert.equal(run.label, "Run a (2)");
    assert.equal(h.requests.length, 0);
});

test("run colour edits apply to metrics, multi-protocol timelines, distributions and heatmaps", async () => {
    const h = harness(url => url.endsWith("trajectory") ? { values: ["received", "read"], series: { received: [1, 2], read: [0, 1] }, iterations: 2 }
        : { type: "mixed", infoValues: ["A"], userValues: ["B"], points: [[0, 0, 2]] });
    const run = h.run("a"); h.context.switchStatsView("overview");
    const input = h.$("diff-analysis-run-table").querySelector("tbody").querySelectorAll("input").find(input => input.type === "color");
    await input.change("#12ab34"); assert.equal(run.color, "#12ab34");
    assert.equal(h.$("diff-analysis-run-table").querySelector("tbody").querySelectorAll("input").find(x => x.type === "color"), input);
    h.context.switchStatsView("metrics"); assert.equal(h.renders.at(-1).config.series[0].itemStyle.color, "#12ab34");
    h.control("node", "View").value = "multi";
    await h.workspace.load("node", "/api/diffusion/trajectory", { node: "n" });
    h.$("diff-traj-stack").value = "lines"; h.workspace.draw("node");
    assert.equal(h.renders.at(-1).config.series.every(series => series.itemStyle.color === "#12ab34"), true);
    h.control("dist", "Chart").value = "heatmap";
    await h.workspace.load("dist", "/api/diffusion/distribution", { type: "mixed" });
    assert.equal(h.renders.at(-1).config.visualMap.inRange.color.at(-1), "#12ab34");
    h.workspace.restoreRuns(); h.context.switchStatsView("metrics"); assert.equal(h.renders.at(-1).config.series[0].itemStyle.color, "#12ab34");
});

test("analysis titles stay above per-tab chart-type rails and offer no reset zoom or chart-data sections", () => {
    const h = harness();
    const rail = h.$("diff-view-stats").querySelector(".diff-analysis-tabs");
    assert.equal(rail.classList.contains("metric-analysis-rail"), false); assert.equal(rail.getAttribute("aria-orientation"), "horizontal");
    assert.equal(h.host("overview").querySelector(".metric-analysis-rail"), null);
    for (const key of ["metrics", "node", "piece", "feat", "dist", "compare"]) {
        assert.ok(h.host(key).querySelector(".metric-display-options"));
        assert.equal(!!h.host(key).querySelector(".metric-analysis-rail"), key !== "metrics");
        assert.equal(h.host(key).querySelectorAll("button").some(button => /Reset zoom/i.test(button.getAttribute("aria-label") || button.textContent)), false);
        assert.equal(h.host(key).querySelector(".diff-analysis-data"), null);
    }
});

test("deleting the playback run activates the remaining result and permits deleting the last run", async () => {
    const h = harness((_url, body) => ({ ok: true, result: body.runId === "b" ? { runId: "a", numIterations: 3 } : null }));
    h.run("a"); h.run("b"); h.context.switchStatsView("overview");
    await h.host("overview").querySelectorAll("button").find(button => button.getAttribute("aria-label") === "Delete Run b").click();
    assert.equal(h.state.diffusion.runs.length, 1); assert.equal(h.state.diffusion.result.runId, "a");
    assert.equal(h.requests[0].body.runId, "b");
    await h.host("overview").querySelectorAll("button").find(button => button.getAttribute("aria-label") === "Delete Run a").click();
    assert.equal(h.state.diffusion.runs.length, 0); assert.equal(h.state.diffusion.result, null);
});

for (const key of ["node", "piece"]) test(`${key} timelines adapt categories, feature values and legends to one or several runs`, async () => {
    const categories = ["received", "read", "propagated", "discarded"], prefix = key === "node" ? "diff-traj" : "diff-ptraj";
    const h = harness((_url, body) => {
        const offset = body.runId === "a" ? 0 : 100;
        if (!body.feature) return { base: true, values: categories, series: Object.fromEntries(categories.map((value, i) => [value, [offset + i, offset + i + 1]])), iterations: 2 };
        const selected = body.singleValue ? body.value || (body.runId === "a" ? "B" : "A") : null;
        const values = selected ? [selected] : ["A", "B"];
        return { base: false, feature: body.feature, selectedValue: selected, availableValues: ["A", "B"], values,
            series: Object.fromEntries(values.map(value => [value, [offset + (value === "A" ? 5 : 11), offset + 20]])), iterations: 2 };
    });
    const fetch = () => key === "node" ? h.context.fetchTrajectory() : h.context.fetchPieceTrajectory();
    h.run("a");
    if (key === "node") h.context.syncTrajFeatureSelect(); else h.context.syncPtrajFeatureSelect();
    await fetch();
    assert.deepEqual(plain(h.renders.at(-1).config.series.map(series => series.name)), categories);
    assert.equal(h.$(prefix + "-category").disabled, true); assert.equal(h.$(prefix + "-category").parentElement.hidden, true);
    h.$(prefix + "-feature").value = key === "node" ? "info:score" : "group";
    h.$(prefix + "-category").value = "propagated"; await fetch();
    assert.deepEqual(plain(h.renders.at(-1).config.series.map(series => series.name)), ["A", "B"]);
    assert.equal(h.$(prefix + "-category").disabled, false); assert.equal(h.control(key, "Feature value").parentElement.hidden, true);
    h.run("b"); h.$(prefix + "-feature").value = key === "node" ? "pieces" : "users";
    await fetch();
    assert.deepEqual(plain(h.renders.at(-1).config.series.map(series => series.name)), categories);
    h.control(key, "Protocol").value = "b"; await fetch();
    assert.equal(h.renders.at(-1).config.series[0].data[0][1], 100);
    h.control(key, "View").value = "multi";
    h.$(prefix + "-category").value = "read"; await fetch();
    assert.deepEqual(plain(h.renders.at(-1).config.series.map(series => series.name)), ["Run a", "Run b"]);
    assert.deepEqual(plain(h.renders.at(-1).config.series.map(series => series.data[0][1])), [1, 101]);
    assert.equal(h.$(prefix + "-category").disabled, false);
    h.$(prefix + "-feature").value = key === "node" ? "user:group" : "group"; await fetch();
    assert.equal(h.control(key, "Feature value").parentElement.hidden, false);
    assert.deepEqual(plain(h.renders.at(-1).config.series.map(series => series.name)), ["Run a", "Run b"]);
    assert.deepEqual(plain(h.renders.at(-1).config.series.map(series => series.data[0][1])), [5, 105]);
    await h.control(key, "Feature value").change("B");
    assert.deepEqual(plain(h.renders.at(-1).config.series.map(series => series.data[0][1])), [11, 111]);
    assert.equal(h.requests.slice(-2).every(request => request.body.singleValue && request.body.value === "B" && request.body.category === "read"), true);
});

test("feature views select one protocol or compare one value and category across protocols", async () => {
    const categories = ["received", "read", "propagated", "discarded"];
    const h = harness((_url, body) => {
        const values = body.value === "__all__" ? ["A", "B"] : categories;
        const offset = body.runId === "b" ? 100 : 0;
        return { values, iterations: 2, series: Object.fromEntries(values.map((value, i) => [value, [offset + i, offset + i + 1]])) };
    });
    h.state.diffusion.pieces = [{ features: [{ param: "score", value: "A" }, { param: "score", value: "B" }] }];
    h.run("a"); h.run("b"); h.context.syncFtrajFeatureSelect(); h.context.syncFtrajValueSelect();
    const fetch = () => h.context.fetchFeatureTrajectory();
    await fetch();
    assert.equal(h.requests.length, 1); assert.equal(h.requests[0].body.runId, "a"); assert.equal(h.requests[0].body.allValues, true);
    assert.deepEqual(plain(h.renders.at(-1).config.series.map(x => x.name)), ["A", "B"]);
    assert.equal(h.$("diff-ftraj-value").parentElement.hidden, true);
    h.control("feat", "View").value = "single"; h.control("feat", "Protocol").value = "b"; await fetch();
    assert.equal(h.requests.at(-1).body.runId, "b"); assert.equal(h.requests.at(-1).body.value, "A");
    assert.deepEqual(plain(h.renders.at(-1).config.series.map(x => x.name)), categories);
    assert.ok(new Set(h.renders.at(-1).config.series.map(x => x.itemStyle.color)).size > 1);
    assert.equal(h.$("diff-ftraj-category").parentElement.hidden, true);
    h.control("feat", "View").value = "multi"; h.$("diff-ftraj-value").value = "B"; h.$("diff-ftraj-category").value = "read"; await fetch();
    assert.deepEqual(plain(h.renders.at(-1).config.series.map(x => x.name)), ["Run a", "Run b"]);
    assert.deepEqual(plain(h.renders.at(-1).config.series.map(x => x.data[0][1])), [1, 101]);
    assert.equal(h.requests.slice(-2).every(x => x.body.value === "B" && x.body.category === "read"), true);
    assert.equal(h.control("feat", "Protocol").parentElement.hidden, true);
    assert.equal(h.$("diff-ftraj-category").disabled, false);
});

for (const key of ["node", "piece", "feat"]) test(`${key} single-protocol charts keep distinct series colours across all display styles`, async () => {
    const h = harness(() => ({ values: ["received", "read", "propagated"], iterations: 2, series: { received: [1, 2], read: [2, 3], propagated: [1, 1] } }));
    h.run("a");
    await h.workspace.load(key, "/api/diffusion/trajectory", {});
    const prefix = key === "node" ? "diff-traj" : key === "piece" ? "diff-ptraj" : "diff-ftraj";
    let colours;
    for (const style of ["stacked", "percent", "lines", "points", "both"]) {
        h.$(prefix + "-stack").value = style; h.workspace.draw(key);
        const series = h.renders.at(-1).config.series;
        assert.equal(new Set(series.map(item => item.itemStyle.color)).size, 3);
        const currentColours = series.map(item => item.itemStyle.color);
        if (colours) assert.deepEqual(currentColours, colours); else colours = currentColours;
        if (["stacked", "percent"].includes(style)) assert.equal(series.every(item => item.stack === "a" && item.areaStyle), true);
        else assert.equal(series.every(item => !item.stack && !item.areaStyle), true);
    }
    assert.equal(h.requests.length, 1);
});

for (const key of ["node", "piece"]) test(`${key} rankings switch range and orientation without styles or recomputing`, async () => {
    const h = harness(() => ({ values: { low: 0, middle: 2, high: 9 } })); h.run("a");
    h.control(key, "View").value = "rankings";
    const limit = h.host(key).querySelectorAll("input").find(input => input.type === "number"); limit.value = "1";
    await h.workspace.rankings(key);
    assert.deepEqual(plain(h.renders.at(-1).config.xAxis.data), ["high"]);
    await h.control(key, "Show").change("bottom");
    assert.deepEqual(plain(h.renders.at(-1).config.xAxis.data), ["low"]);
    assert.equal(h.renders.at(-1).config.series[0].data[0][1], 0);
    await h.control(key, "Show").change("all");
    assert.deepEqual(plain(h.renders.at(-1).config.xAxis.data), ["high", "middle", "low"]);
    assert.equal(limit.parentElement.hidden, true); assert.equal(h.renders.at(-1).config.legend.show, false);
    const style = h.$(key === "node" ? "diff-traj-stack" : "diff-ptraj-stack");
    assert.equal(style.parentElement.hidden, true);
    assert.equal(h.control(key, "Display").parentElement.hidden, true);
    assert.equal(h.control(key, "Bars").parentElement.hidden, false);
    await h.control(key, "Bars").change("horizontal");
    assert.equal(h.renders.at(-1).config.xAxis.name, "Count");
    assert.equal(h.renders.at(-1).config.yAxis.type, "category");
    assert.deepEqual(plain(h.renders.at(-1).config.series[0].data), [[9, "high"], [2, "middle"], [0, "low"]]);
    await h.control(key, "Bars").change("vertical");
    assert.deepEqual(plain(h.renders.at(-1).config.xAxis.data), ["high", "middle", "low"]);
    assert.equal(h.requests.length, 1);
    h.control(key, "View").value = "timeline"; h.workspace.timelineControls(key);
    assert.equal(style.parentElement.hidden, false);
});
