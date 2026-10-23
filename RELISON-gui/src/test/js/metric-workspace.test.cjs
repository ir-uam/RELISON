const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), vm = require("node:vm");
const publicDir = path.join(__dirname, "../../main/resources/public");
const read = file => fs.readFileSync(path.join(publicDir, file), "utf8");
const plain = value => JSON.parse(JSON.stringify(value));

function harness(extra = {}) {
    const state = { activeTab: "metrics", activeSubtab: "nodes", metricOrder: ["Degree", "Centrality"],
        metricData: { Degree: { a: 0, b: 4, c: null }, Centrality: { c: 0.5, a: 0.1, b: 0.3 } },
        graph: { nodes: () => ["a", "b", "c"], edges: () => ["ab", "ab2"], source: () => "a", target: () => "b" },
        pairOrder: ["Distance"], pairData: { Distance: { "a|b": 2 } }, multigraph: true,
        nodePairOrder: [], nodePairAgg: {}, commMetricOrder: [], commMetricData: {},
        attributeMetricOrder: [], attributeMetricData: {}, edgeAttributeMetricOrder: [], edgeAttributeMetricData: {},
        rec: { models: { model: { label: "Model A" } } },
        recMetricData: { vertex: { Degree: { model: { a: 1, b: 6, c: null } } }, pair: { Distance: { model: { "a|b": 3 } } }, comm: {} },
    };
    const colors = { text: "#e6e6e6", muted: "#9aa0a6", border: "#3a3c41", accent: "#4f9dff", panel: "#2b2d31",
        fontSize: 13, labelSize: 12, gridOpacity: 0.4, palette: ["#4f9dff", "#dd9944"] };
    const context = vm.createContext({ state, echartColors: () => colors,
        chartBase: () => ({ tooltip: { confine: true }, animation: false, grid: { containLabel: true, left: 90, right: 30, top: 50, bottom: 80 } }),
        fmt: value => value == null || !Number.isFinite(value) ? "—" : Number(value.toPrecision(4)).toString(),
        escapeChartHtml: value => String(value).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;"),
        pairKey: (a, b) => a + "|" + b, isAttributeCommunityPartition: algorithm => algorithm?.startsWith("attribute:"),
        recLabel: key => key === "model" ? "Model A" : key,
        numericMap: values => Object.fromEntries(Object.entries(values).map(([key, value]) => [key, value == null ? NaN : Number(value)])),
        communityLabel: (id, algorithm) => algorithm + ":" + id, edgePartitionChartEntries: () => [], ...extra });
    vm.runInContext(read("metric-series.js"), context);
    vm.runInContext(read("metric-workspace.js"), context);
    vm.runInContext(read("metric-recommendations.js"), context);
    return { context, state, entry: () => context.metricAnalysisEntries("nodes")[0] };
}

test("statistics exclude missing and non-numeric values, retain zero, and interpolate quartiles without mutating data", () => {
    const { context: c } = harness();
    const values = [9, 0, -3, 6, null, undefined, NaN, Infinity, "4"];
    const result = c.metricAnalysisSummary(values);
    assert.equal(result.count, 4); assert.equal(result.excluded, 5);
    assert.equal(result.mean, 3); assert.equal(result.median, 3); assert.equal(result.q1, -0.75); assert.equal(result.q3, 6.75);
    assert.deepEqual(values.slice(0, 4), [9, 0, -3, 6]);
    assert.ok(Number.isNaN(c.metricAnalysisSummary([null]).median));
    assert.equal(c.metricAnalysisSummary([Number.MAX_VALUE, Number.MAX_VALUE]).mean, Number.MAX_VALUE);
});

test("histograms share boundaries across populations, include both endpoints, and handle constant and negative values", () => {
    const { context: c } = harness();
    const histogram = c.metricAnalysisHistogram([[-4, 0, 4, null], [-2, 2, 10, Infinity]], 7);
    assert.deepEqual(plain(histogram.bounds), [-4, 3, 10]);
    assert.deepEqual(plain(histogram.counts), [[2, 1], [2, 1]]);
    assert.deepEqual(plain(c.metricAnalysisHistogram([[0, 0, 0]], 2).counts), [[3]]);
    const extremes = c.metricAnalysisHistogram([[-Number.MAX_VALUE, Number.MAX_VALUE]], Number.MAX_VALUE);
    assert.deepEqual(plain(extremes.counts), [[1, 1]]);
    assert.ok(c.metricAnalysisDomain([Number.MAX_VALUE]).every(Number.isFinite));
    assert.throws(() => c.metricAnalysisHistogram([[0, 1]], 0.000001), /larger bin size/);
    assert.deepEqual(plain(c.metricAnalysisHistogram([[0, 0.1, 0.2, 0.3]], 0.1).counts), [[1, 1, 2]]);
    assert.deepEqual(plain(c.metricAnalysisHistogram([[0, 0.09999999999999999, 0.1]], 0.1).counts), [[3]]);
    assert.deepEqual(plain(c.metricAnalysisHistogram([[0, 0.09999999999999999, 0.1, 0.2]], 0.1).counts), [[2, 2]]);
    assert.throws(() => c.metricAnalysisHistogram([[1]], 0), /positive/);
});

test("cumulative curves retain tied observations and end at 100 percent", () => {
    const { context: c } = harness();
    assert.deepEqual(plain(c.metricAnalysisCumulative([2, 0, 2, 2, null])), [[0, 0], [0, 25], [2, 100]]);
    assert.deepEqual(plain(c.metricAnalysisCumulative([null, NaN])), []);
});

test("log axes filter only chart points, preserve raw rows, and report values that cannot be plotted", () => {
    const { context: c } = harness();
    const rawRows = [["series", "entity", "x", "y"], ["A", "zero", 0, 2], ["A", "negative", -1, 3], ["A", "positive", 4, 5]];
    const result = { option: { xAxis: {}, yAxis: {}, series: [{ type: "scatter", data: [[0, 2, "zero"], [-1, 3, "negative"], [4, 5, "positive"]] }] }, rows: rawRows, note: "3 matched", empty: false };
    const xLog = c.metricAnalysisApplyLogAxes(result, "log", "linear");
    assert.equal(xLog.option.xAxis.type, "log"); assert.equal(xLog.option.yAxis.type, undefined);
    assert.deepEqual(plain(xLog.option.series[0].data), [[4, 5, "positive"]]);
    assert.deepEqual(plain(xLog.rows), rawRows); assert.match(xLog.note, /2 points hidden/);
    assert.equal(result.option.series[0].data.length, 3);
    const both = c.metricAnalysisApplyLogAxes({ option: { xAxis: {}, yAxis: {}, series: [{ type: "line", data: [[2, 0], [2, 8]] }] }, rows: rawRows }, "log", "log");
    assert.deepEqual(plain(both.option.series[0].data), [[2, 8]]); assert.ok(both.option.xAxis.min > 0 && both.option.yAxis.min > 0);
    const none = c.metricAnalysisApplyLogAxes({ option: { xAxis: {}, yAxis: {}, series: [{ type: "line", data: [[0, 0], [-2, 1]] }] }, rows: [] }, "log", "linear");
    assert.equal(none.empty, true); assert.match(none.note, /No points have positive/);
});

test("comparisons align by entity ID rather than iteration order, reject missing values, and preserve zeros", () => {
    const { context: c } = harness();
    const result = c.metricAnalysisMatch({ a: 0, b: 3, c: null, d: 1 }, { b: 7, c: 0, a: 0, e: 5 });
    assert.deepEqual(plain(result.points), [{ id: "a", label: "a", x: 0, y: 0 }, { id: "b", label: "b", x: 3, y: 7 }]);
    assert.equal(result.excluded, 3);
    assert.equal(c.metricAnalysisMatch({ toString: 1 }, { toString: 2 }).points[0].label, "toString");
});

test("entity adapters restrict nodes to the graph, preserve parallel edges, and translate recommendation pair keys", () => {
    const { context: c, state } = harness();
    state.metricData.Degree.stale = 100;
    const node = c.metricAnalysisEntries("nodes")[0];
    assert.deepEqual(Object.keys(node.values), ["a", "b", "c"]);
    const edge = c.metricAnalysisEntries("edges")[0];
    assert.deepEqual(plain(edge.values), { ab: 2, ab2: 2 });
    assert.deepEqual(plain(c.metricAnalysisRecommendations(edge).model), { ab: 3, ab2: 3 });
    assert.equal(edge.labels.ab2, "a→b (ab2)");
});

test("group comparison scopes distinguish community partitions, attributes and edge partitions", () => {
    const { context: c, state, entry } = harness();
    state.commMetricOrder = ["Degree A", "Degree B", "Attribute result"];
    state.commMetricData = { "Degree A": { algorithm: "A", values: { 0: 5 } }, "Degree B": { algorithm: "B", values: { 0: 8 } },
        "Attribute result": { algorithm: "attribute:country", values: { 0: 1 } } };
    const communities = c.metricAnalysisEntries("comm");
    assert.equal(communities.length, 2);
    assert.equal(c.metricAnalysisComparison(communities[0], communities[1], null, "scatter").empty, true);
    state.attributeMetricOrder = ["Country", "Role"];
    state.attributeMetricData = { Country: { attribute: "country", values: { 0: 1 } }, Role: { attribute: "role", values: { 0: 2 } } };
    const attributes = c.metricAnalysisEntries("attrcomm");
    assert.notEqual(attributes[0].scope, attributes[1].scope);
    const points = c.metricAnalysisComparison(entry(), c.metricAnalysisEntries("nodes")[1], null, "scatter");
    assert.equal(points.empty, false);
    assert.notEqual(points.option.xAxis.max, points.option.yAxis.max);
    assert.equal(points.option.series[0].markLine, undefined);
});

test("recommendation scatter uses equal padded domains and a no-change line; differences use every matched observation", () => {
    const { context: c, entry } = harness();
    const rec = { name: "Model A", map: { a: 1, b: 6, c: null } };
    const scatter = c.metricAnalysisComparison(entry(), null, rec, "scatter");
    assert.equal(scatter.option.xAxis.min, scatter.option.yAxis.min);
    assert.equal(scatter.option.xAxis.max, scatter.option.yAxis.max);
    assert.equal(scatter.option.series[0].markLine.data.length, 1);
    assert.equal(scatter.rows.length, 3); assert.equal(scatter.rows[1][3], 1); assert.equal(scatter.rows[2][3], 2);
    const difference = c.metricAnalysisComparison(entry(), null, rec, "difference");
    assert.equal(difference.option.series[0].data.reduce((a, b) => a + b, 0), 2);
    assert.match(difference.note, /positive means an increase/);
    assert.match(difference.note, /Change median/); assert.doesNotMatch(difference.note, /Original median/);
});

test("distribution overlays use common bins, explicit min/max box whiskers, escaped tooltips, and raw CSV values", () => {
    const { context: c, entry } = harness();
    const comparison = { name: "<Model>", map: { a: 20, b: 30, c: null }, values: [20, 30, null] };
    const histogram = c.metricAnalysisDistribution(entry(), "histogram", 8, comparison);
    assert.equal(histogram.option.series.length, 2);
    assert.equal(histogram.option.series[0].data.length, 4);
    assert.equal(histogram.option.series[0].data.reduce((a, b) => a + b, 0), 2);
    assert.equal(histogram.option.series[1].data.reduce((a, b) => a + b, 0), 2);
    assert.match(histogram.option.tooltip.formatter([{ dataIndex: 0, seriesName: "<Model>", value: 2 }]), /&lt;Model&gt;/);
    assert.equal(histogram.rows.length, 7);
    const box = c.metricAnalysisDistribution(entry(), "boxplot", 8, comparison);
    assert.deepEqual(plain(box.option.series[0].data), [[0, 1, 2, 3, 4], [20, 22.5, 25, 27.5, 30]]);
    assert.match(box.note, /whiskers: minimum and maximum/);
    assert.equal(c.metricAnalysisDistribution({ ...entry(), values: { a: null } }, "histogram", 8).empty, true);
});

test("all-pairs distributions preserve source bins, sampled counts, exclusions and the cumulative approximation label", () => {
    const { context: c } = harness();
    const entry = { label: "Distance", values: {}, labels: {}, aggregate: { estimated: true, evaluated: 10, totalPairs: 100,
        infinite: 2, histogram: [{ x0: 0, x1: 2, count: 3 }, { x0: 2, x1: 4, count: 5 }] } };
    const histogram = c.metricAnalysisDistribution(entry, "histogram", 2);
    assert.deepEqual(plain(histogram.option.series[0].data), [3, 5]);
    assert.match(histogram.note, /Estimated from 10 sampled pairs of 100/);
    assert.match(histogram.note, /2 non-finite/);
    const cumulative = c.metricAnalysisDistribution(entry, "cumulative", 20);
    assert.deepEqual(plain(cumulative.option.series[0].data), [[0, 0], [2, 37.5], [4, 100]]);
    assert.match(cumulative.note, /Approximate cumulative curve/);
    assert.equal(histogram.rows[1][3], true);
    entry.aggregate.estimated = false;
    assert.match(c.metricAnalysisDistribution(entry, "histogram", 2).note, /Exact over 100 pairs/);
});

test("the packaged ECharts renderer exports every new chart type as SVG at desktop and compact widths", () => {
    const renderer = vm.createContext({ setTimeout, clearTimeout, console });
    vm.runInContext(read("vendor/echarts.min.js"), renderer);
    const { context: c, entry } = harness();
    const plots = ["histogram", "cumulative", "boxplot"].map(type => c.metricAnalysisDistribution(entry(), type, 20));
    for (const type of ["cumulative", "frequency"]) for (const marks of ["points", "lines", "both"])
        plots.push(c.metricAnalysisDistribution(entry(), type, null, null, marks));
    plots.push(c.metricAnalysisComparison(entry(), c.metricAnalysisEntries("nodes")[1], null, "scatter"));
    plots.push(c.metricAnalysisComparison(entry(), null, { name: "Model A", map: { a: 1, b: 6 } }, "scatter"));
    for (const width of [360, 800]) for (const plot of plots) {
        const chart = renderer.echarts.init(null, null, { renderer: "svg", ssr: true, width, height: 400 });
        try {
            chart.setOption(plot.option);
            const svg = chart.renderToSVGString();
            assert.match(svg, /<svg/); assert.match(svg, /<path/); assert.doesNotMatch(svg, /NaN|Infinity/);
        } finally { chart.dispose(); }
    }
});

test("exact frequency charts count distinct finite values without bins and support all three mark styles", () => {
    const { context: c, entry } = harness();
    const data = { ...entry(), values: { a: 0, b: 2, c: 2, d: 2.01, e: null, f: Infinity } };
    assert.deepEqual(plain(c.metricAnalysisFrequency(Object.values(data.values))), [[0, 1], [2, 2], [2.01, 1]]);
    for (const type of ["frequency", "cumulative"]) for (const marks of ["points", "lines", "both"]) {
        const plot = c.metricAnalysisDistribution(data, type, null, null, marks), series = plot.option.series[0];
        assert.equal(series.showSymbol, marks !== "lines"); assert.equal(series.lineStyle.width, marks === "points" ? 0 : 2);
        assert.equal(series.emphasis.lineStyle.width, marks === "points" ? 0 : 2);
        if (type === "frequency") {
            assert.deepEqual(plain(series.data), [[0, 1], [2, 2], [2.01, 1]]);
            assert.deepEqual(plain(plot.rows[2]), ["Original", 2, 2]);
        } else assert.deepEqual(plain(series.data), [[0, 25], [2, 75], [2.01, 100]]);
    }
});

test("stored pair bins can be merged exactly but are never split or presented as exact frequencies", () => {
    const { context: c } = harness();
    const bins = [{ x0: 0, x1: 2, count: 3 }, { x0: 2, x1: 4, count: 5 }, { x0: 4, x1: 6, count: 1 }];
    assert.deepEqual(plain(c.metricAnalysisRebin(bins, 4)), [{ x0: 0, x1: 4, count: 8 }, { x0: 4, x1: 6, count: 1 }]);
    assert.throws(() => c.metricAnalysisRebin(bins, 3), /whole multiple/);
    assert.throws(() => c.metricAnalysisRebin(bins, 1), /whole multiple/);
    const entry = { label: "Distance", values: {}, labels: {}, aggregate: { estimated: false, totalPairs: 9, histogram: bins } };
    assert.throws(() => c.metricAnalysisDistribution(entry, "frequency", null), /individual values/);
    const histogram = c.metricAnalysisDistribution(entry, "histogram", 4);
    assert.deepEqual(plain(histogram.option.series[0].data), [8, 1]); assert.equal(histogram.rows[1][2], 8);
});

test("global summaries use explicit family metadata, retain recommendation values, and recognise legacy grouping labels", () => {
    const { context: c, state } = harness();
    state.graphMetrics = { Density: 0.1, "Modularity (Louvain)": 0.4, "Gini (attribute:country)": 0.2,
        "Number of attribute values (type)": 3, "Scoped metric": 4 };
    state.communityData = { Louvain: { a: 0 } };
    state.globalMetricScopes = { Density: { family: "global", scope: "graph" }, "Scoped metric": { family: "comm", scope: "partition-id" } };
    state.recMetricData.graph = { "Modularity (Louvain)": { model: 0.6 } };
    const communities = c.metricAnalysisGlobalRows("comm");
    assert.deepEqual(plain(communities.map(row => row.label)), ["Modularity (Louvain)", "Scoped metric"]);
    assert.equal(communities[0].recommendations.model, 0.6);
    assert.deepEqual(plain(c.metricAnalysisGlobalRows("attrcomm").map(row => row.label)), ["Gini (attribute:country)"]);
    assert.deepEqual(plain(c.metricAnalysisGlobalRows("edgeattr").map(row => row.label)), ["Number of attribute values (type)"]);
});

test("session save/restore retains global metric families while older sessions still use label-based grouping", () => {
    const stubs = Object.fromEntries(["restoreCommunityExplorer", "rebuildAppearanceOptions", "applyAppearance", "syncLayoutOptionsForRenderer",
        "updateRecUI", "drawRecOverlay", "renderTable", "renderPiecesTable", "updateRecTabAvailability", "applyReducers", "refreshAfterCompute"].map(name => [name, () => {}]));
    const { context: c, state } = harness({ SESSION_CONTROLS: [], collectCommunityExplorer: () => ({}), ...stubs });
    state.graph = null; state.graphId = "test-graph"; state.communityData = {}; state.rec = {}; state.recEval = {}; state.diffusion = {};
    state.graphMetrics = { "Grouped global": 3 };
    state.globalMetricScopes = { "Grouped global": { family: "attrcomm", scope: "country" } };
    const app = read("app.js");
    for (const name of ["collectSessionClientState", "applySessionClientState"]) {
        const start = app.indexOf("function " + name + "(");
        vm.runInContext(app.slice(start, app.indexOf("\n}\n", start) + 3), c);
    }
    const saved = plain(c.collectSessionClientState());
    state.globalMetricScopes = {}; c.applySessionClientState(saved);
    assert.equal(c.metricAnalysisGlobalRows("attrcomm")[0].label, "Grouped global");
    assert.equal(state.globalMetricScopes["Grouped global"].scope, "country");
    delete saved.metrics.globalMetricScopes;
    saved.metrics.graphMetrics = { "Number of attribute values (type)": 3 };
    c.applySessionClientState(saved);
    assert.deepEqual(plain(state.globalMetricScopes), {});
    assert.equal(c.metricAnalysisGlobalRows("edgeattr")[0].label, "Number of attribute values (type)");
});

// Minimal DOM fixture for exercising the real pane reorganisation and controls.
// It parses the shipped metric section, so missing IDs/sections break the test.
function workspaceDom() {
    let document;
    class Element {
        constructor(tag) { this.tagName = tag.toUpperCase(); this.children = []; this.attributes = {}; this.dataset = {}; this.handlers = {}; this._text = ""; this.className = ""; this._value = ""; this.style = {}; }
        get classList() { return { contains: name => this.className.split(/\s+/).includes(name), add: name => { this.className += " " + name; }, toggle: (name, active) => { const names = new Set(this.className.split(/\s+/).filter(Boolean)); if (active) names.add(name); else names.delete(name); this.className = [...names].join(" "); } }; }
        get parentElement() { return this.parentNode; }
        get nextSibling() { return this.parentNode?.children[this.parentNode.children.indexOf(this) + 1] || null; }
        get textContent() { return this._text + this.children.map(child => child.textContent).join(""); }
        set textContent(value) { this.replaceChildren(); this._text = String(value); }
        get options() { return this.children.filter(child => child.tagName === "OPTION"); }
        get value() { return this.tagName === "SELECT" ? (this.options.find(item => item.value === this._value) || this.options[0])?.value || "" : this._value; }
        set value(value) { this._value = String(value); }
        set innerHTML(value) { assert.equal(value, ""); this.replaceChildren(); }
        get validity() { const n = Number(this.value); return { valid: this.type !== "number" || this.value !== "" && Number.isFinite(n) && (this.step === "any" || Number.isInteger(n)) && n >= Number(this.min || -Infinity) && n <= Number(this.max || Infinity) }; }
        setAttribute(name, value) { this.attributes[name] = String(value); if (name === "class") this.className = value; else if (name.startsWith("data-")) this.dataset[name.slice(5).replace(/-([a-z])/g, (_, ch) => ch.toUpperCase())] = value; else if (name === "id") this.id = value; }
        getAttribute(name) { return this.attributes[name]; }
        appendChild(child) { child.remove(); child.parentNode = this; this.children.push(child); return child; }
        append(...children) { children.forEach(child => this.appendChild(child)); }
        prepend(child) { child.remove(); child.parentNode = this; this.children.unshift(child); }
        after(child) { child.remove(); child.parentNode = this.parentNode; this.parentNode.children.splice(this.parentNode.children.indexOf(this) + 1, 0, child); }
        remove() { if (this.parentNode) { const siblings = this.parentNode.children; siblings.splice(siblings.indexOf(this), 1); this.parentNode = null; } }
        replaceChildren(...children) { this.children.forEach(child => { child.parentNode = null; }); this.children = []; this._text = ""; this.append(...children); }
        matches(selector) {
            if (selector === "button:not(:disabled)") return this.tagName === "BUTTON" && !this.disabled;
            if (selector[0] === ".") return this.classList.contains(selector.slice(1));
            const attr = selector.match(/^\[([^=\]]+)(?:="([^"]*)")?\]$/);
            if (attr) { const value = attr[1].startsWith("data-") ? this.dataset[attr[1].slice(5).replace(/-([a-z])/g, (_, ch) => ch.toUpperCase())] : this.getAttribute(attr[1]); return value != null && (attr[2] == null || value === attr[2]); }
            return this.tagName.toLowerCase() === selector;
        }
        querySelectorAll(selector) { return this.children.flatMap(child => [...(child.matches(selector) ? [child] : []), ...child.querySelectorAll(selector)]); }
        querySelector(selector) { return this.querySelectorAll(selector)[0] || null; }
        closest(selector) { return this.matches(selector) ? this : this.parentNode?.closest(selector); }
        contains(child) { return this === child || this.children.some(item => item.contains(child)); }
        addEventListener(name, fn) { (this.handlers[name] ||= []).push(fn); }
        click() { if (!this.disabled) { (this.handlers.click || []).forEach(fn => fn({ target: this })); this.onclick?.({ target: this }); } }
        showModal() { this.open = true; }
        close() { this.open = false; }
        focus() { document.activeElement = this; }
        change(value) { this.value = value; let node = this; while (node) { (node.handlers.change || []).forEach(fn => fn({ target: this })); node.onchange?.({ target: this }); node = node.parentNode; } }
    }
    const root = new Element("main"), html = read("index.html"), start = html.indexOf('<div class="tabpane" id="pane-metrics">');
    const sidebarStart = html.indexOf('<div id="left-metrics"');
    const section = html.slice(sidebarStart, html.indexOf("</aside>", sidebarStart)) + html.slice(start, html.indexOf("<!-- TAB 5:", start)), stack = [root];
    for (const token of section.match(/<!--[\s\S]*?-->|<[^>]+>|[^<]+/g)) {
        if (token.startsWith("<!--")) continue;
        if (token.startsWith("</")) { stack.pop(); continue; }
        if (token.startsWith("<")) {
            const tag = token.match(/^<([\w-]+)/)[1], node = new Element(tag);
            for (const [, name, value] of token.matchAll(/([\w-]+)="([^"]*)"/g)) { node.setAttribute(name, value); if (["value", "type", "min", "max"].includes(name)) node[name] = value; }
            if (/\shidden(?:\s|>)/.test(token)) node.hidden = true;
            stack.at(-1).appendChild(node);
            if (!/\/>$/.test(token) && !["input", "br", "hr"].includes(tag)) stack.push(node);
        } else stack.at(-1)._text += token.trim();
    }
    for (const id of ["layout", "left", "right", "left-layout", "left-communities", "left-edge-partitions"]) { const element = new Element("div"); element.id = id; root.appendChild(element); }
    document = { body: root, activeElement: null, createElement: tag => new Element(tag), getElementById: id => [root, ...all(root)].find(node => node.id === id), querySelectorAll: selector => root.querySelectorAll(selector) };
    function all(node) { return node.children.flatMap(child => [child, ...all(child)]); }
    const $ = document.getElementById, renders = [], downloads = [], panelUpdates = [];
    const h = harness({ document, $, option: (value, text) => { const o = new Element("option"); o.value = value; o.textContent = text; return o; },
        thEl: text => { const e = new Element("th"); e.textContent = text; return e; }, tdText: text => { const e = new Element("td"); e.textContent = text; return e; },
        clearChart: id => renders.push({ cleared: id }), showEChart: (id, option, title) => renders.push({ id, option, title }),
        drawNodeChart() {}, drawEdgeChart() {}, drawCommChart() {}, drawAttributeCommChart() {}, drawEdgeAttributeChart() {}, drawEdgePartitionChart() {}, renderNodeTopK() {},
        syncEdgePartitionChartSort() {}, recLabel: key => key === "model" ? "Model A" : key,
        buildAverageTable(id, rows, keys) { $(id).textContent = rows.map(row => row.label + " " + row.original + " " + row.rec.join(" ")).join(" | "); },
        csvCell: value => value == null || typeof value === "number" && !Number.isFinite(value) ? "" : '"' + String(value).replace(/"/g, '""') + '"',
        download: (...args) => downloads.push(args), downloadChartPng: (...args) => downloads.push(args), updatePanels: tab => panelUpdates.push(tab),
        setSelectOptions(id, values, labels) { const select = $(id), previous = select.value; select.replaceChildren(); values.forEach((value, i) => { const o = new Element("option"); o.value = value; o.textContent = labels[i]; select.appendChild(o); }); if (values.includes(previous)) select.value = previous; },
        switchSubtab(family) { h.state.activeSubtab = family; h.context.refreshMetricWorkspace(); },
    });
    $("subtabbar").querySelectorAll("button").forEach(button => button.addEventListener("click", () => h.context.switchSubtab(button.dataset.subtab)));
    h.context.setupMetricWorkspace(); h.context.refreshMetricWorkspace();
    return { ...h, $, root, renders, downloads, panelUpdates, view: family => vm.runInContext(`METRIC_WORKSPACE_VIEWS.get(${JSON.stringify(family)})`, h.context) };
}

test("real metric panes reorganise into an accessible rail while retaining every legacy result/control ID", () => {
    const h = workspaceDom();
    assert.equal(h.$("subtabbar").hidden, false);
    assert.equal(h.$("metric-family-picker"), undefined);
    assert.equal(h.$("metric-family-nodes").getAttribute("aria-selected"), "true");
    assert.ok(h.$("node-averages-table")); assert.ok(h.$("node-topk-table")); assert.ok(h.$("node-scatter-metric"));
    assert.equal(h.$("node-chart-orientation").value, "horizontal");
    assert.equal(h.$("node-chart-show").value, "top");
    assert.equal(h.$("metric-pairs-compare-tab").disabled, true);
    assert.equal(h.$("metric-global-distributions-tab").disabled, true);
    h.$("metric-nodes-distributions-tab").click();
    assert.equal(h.view("nodes").panels.distributions.hidden, false);
    assert.equal(h.view("nodes").panels.summary.hidden, true);
    assert.equal(h.renders.at(-1).id, "node-analysis-distributions");
    const select = h.view("nodes").panels.distributions.querySelector('[data-analysis-key="type"]');
    select.focus(); select.change("boxplot");
    assert.equal(h.renders.at(-1).option.series[0].type, "boxplot");
    assert.equal(h.context.document.activeElement.dataset.analysisKey, "type");
    h.$("metric-family-edges").click(); h.$("metric-family-nodes").click();
    assert.equal(h.view("nodes").mode, "distributions"); assert.equal(h.view("nodes").type, "boxplot");
    h.$("metric-compute-toggle").click();
    assert.equal(h.$("metric-compute-toggle").getAttribute("aria-expanded"), "false"); assert.deepEqual(h.panelUpdates, ["metrics"]);
});

test("panel exports use compact icon action rows, distribution controls use bin size and mark options, and related globals export", () => {
    const h = workspaceDom();
    assert.equal(h.$("metric-nodes-summary").children[0].className, "metric-panel-actions");
    assert.equal(h.view("nodes").statisticsButton.classList.contains("metric-export-action"), true);
    assert.ok(h.view("nodes").statisticsButton.querySelector(".material-symbols-outlined"));
    assert.equal(h.$("node-chart").parentElement.parentElement.querySelector("[data-actions]").children[0].getAttribute("aria-label"), "Download PNG / SVG");
    h.$("metric-nodes-distributions-tab").click();
    const panel = h.view("nodes").panels.distributions;
    assert.equal(panel.querySelector('[data-analysis-key="bins"]'), null);
    panel.querySelector('[data-analysis-key="binSize"]').change("0.5");
    assert.equal(h.renders.at(-1).option.series[0].data.length, 8);
    panel.querySelector('[data-analysis-key="type"]').change("frequency");
    panel.querySelector('[data-analysis-key="marks"]').change("points");
    assert.equal(h.renders.at(-1).option.series[0].lineStyle.width, 0);
    panel.querySelector('[data-analysis-key="distributionXScale"]').change("log");
    assert.equal(h.renders.at(-1).option.xAxis.type, "log");
    assert.equal(panel.querySelector('[data-analysis-key="distributionYScale"]').value, "linear");
    assert.equal(panel.querySelector('[data-analysis-key="binSize"]'), null);
    h.state.graphMetrics = { "Grouped global": 3 }; h.state.globalMetricScopes = { "Grouped global": { family: "attrcomm", scope: "country" } };
    h.$("metric-family-attrcomm").click();
    assert.match(h.view("attrcomm").globalTable.textContent, /Grouped global/);
    h.view("attrcomm").globalExport.click(); assert.match(h.downloads.at(-1)[1], /Grouped global/);
    assert.ok(h.$("ep-global-results"));
});

test("comparison controls redraw, CSV/image actions export the selected view, and empty results clear stale charts", () => {
    const h = workspaceDom();
    h.$("metric-nodes-compare-tab").click();
    const panel = h.view("nodes").panels.compare;
    assert.equal(h.renders.at(-1).option.series[0].data.length, 2);
    panel.querySelector('[data-analysis-key="compareXScale"]').change("log");
    assert.equal(h.renders.at(-1).option.xAxis.type, "log");
    panel.querySelector('[data-analysis-key="compareXScale"]').change("linear");
    panel.querySelector('[data-analysis-key="compare"]').change("recommendation");
    assert.ok(h.renders.at(-1).option.series[0].markLine);
    const buttons = panel.querySelectorAll("button"); buttons.find(b => b.getAttribute("aria-label") === "Export chart data CSV").click();
    assert.match(h.downloads.at(-1)[1], /change \(target - reference\)/);
    buttons.find(b => b.getAttribute("aria-label") === "Download PNG / SVG").click(); assert.equal(h.downloads.at(-1)[0], "node-analysis-compare");
    h.state.recMetricData.vertex.Degree = {}; h.context.refreshMetricWorkspace();
    assert.equal(h.renders.at(-1).cleared, "node-analysis-compare");
    assert.match(panel.querySelector("[data-empty]").textContent, /Recompute this metric/);
    assert.equal(panel.querySelector(".chart-dl").disabled, true);
    h.$("metric-nodes-summary-tab").click();
    assert.match(h.view("nodes").statistics.textContent, /Excluded/);
});

test("real app navigation, dashboard refresh, theme redraw and computation-panel layout use the new workspace", () => {
    const h = workspaceDom(), app = read("app.js");
    for (const name of ["renderGlobalTable", "renderAverages", "renderPairAverages", "renderCommAverages", "renderAttributeCommAverages", "renderEdgeAttributeAverages", "renderEdgePartitionMetricResults", "renderAttributeRelationships"])
        h.context[name] = () => {};
    for (const name of ["recColumnKeys", "syncChartSelectors", "syncScatterSelectors", "updateScatterBlocks", "toggleHidden", "updatePanels", "switchSubtab", "renderMetricsDashboard", "redrawVisibleCharts"]) {
        const start = app.indexOf("function " + name + "("); assert.ok(start >= 0, name);
        vm.runInContext(app.slice(start, app.indexOf("\n}\n", start) + 3), h.context);
    }
    h.context.renderMetricsDashboard();
    h.$("metric-nodes-rankings-tab").click();
    h.context.switchSubtab("edges"); h.context.switchSubtab("nodes");
    assert.equal(h.view("nodes").mode, "rankings");
    h.$("metric-nodes-distributions-tab").click();
    h.context.redrawVisibleCharts();
    assert.equal(h.renders.at(-1).id, "node-analysis-distributions");
    h.context.updatePanels("metrics");
    assert.equal(h.$("left").style.display, ""); assert.equal(h.$("layout").style.gridTemplateColumns, "300px 1fr");
    assert.equal(h.$("metric-compute-toggle").getAttribute("aria-expanded"), "true");
    h.$("metric-compute-toggle").click();
    assert.equal(h.$("left").style.display, "none"); assert.equal(h.$("layout").style.gridTemplateColumns, "1fr");
    h.context.updatePanels("network"); assert.equal(h.$("layout").style.gridTemplateColumns, "270px 1fr 300px");
    h.context.switchSubtab("relationships"); assert.equal(h.$("metric-family-relationships").getAttribute("aria-selected"), "true");
});

test("computation controls show only the active metric family and fix the pair scope to its panel", () => {
    const h = workspaceDom(), sections = h.$("left-metrics").querySelectorAll("[data-metric-family]");
    assert.equal(sections.length, 7);
    for (const family of ["global", "nodes", "edges", "pairs", "comm", "attrcomm", "edgeattr", "edgepartitions", "relationships"]) {
        h.state.activeSubtab = family; h.context.refreshMetricWorkspace();
        const visible = sections.filter(section => !section.hidden);
        assert.equal(visible.length, family === "relationships" ? 0 : 1, family);
        if (visible.length) assert.ok(visible[0].dataset.metricFamily.split(" ").includes(family), family);
        assert.equal(h.$("pair-allpairs").checked, family === "pairs");
        assert.equal(h.$("pair-allpairs").closest("label").hidden, true);
        assert.equal(h.$("metric-compute-toggle").hidden, family === "relationships");
    }
    h.$("pair-metric").value = "Distance";
    h.state.activeSubtab = "pairs"; h.context.refreshMetricWorkspace(); assert.equal(h.$("pair-metric-heading").textContent, "Node-pair metrics");
    h.state.activeSubtab = "edges"; h.context.refreshMetricWorkspace(); assert.equal(h.$("pair-metric-heading").textContent, "Edge metrics");
});

test("pair requests use the active result panel even if the legacy checkbox has a stale value", async () => {
    const app = read("app.js"), requests = [], state = { graphId: "g", activeSubtab: "edges" };
    const controls = { "pair-metric": { value: "Distance" }, "pair-allpairs": { checked: true } };
    const c = vm.createContext({ state, $: id => controls[id], jobBusy: () => false, requireGraph: () => true,
        setStatus() {}, beginJob: () => undefined, endJob() {}, collectParams: () => ({}),
        jsonBody: body => ({ body: JSON.stringify(body) }), api: async (_url, options) => { requests.push(JSON.parse(options.body)); return { cancelled: true }; },
    });
    const start = app.indexOf("async function runPairMetric("); vm.runInContext(app.slice(start, app.indexOf("\n}\n", start) + 3), c);
    await c.runPairMetric();
    state.activeSubtab = "pairs"; controls["pair-allpairs"].checked = false; await c.runPairMetric();
    assert.equal(requests[0].onlyLinks, true); assert.equal(requests[1].onlyLinks, false);
});

test("undirected metric orientation parameters submit UND and stay disabled across all metric families", () => {
    const h = workspaceDom(), app = read("app.js"), c = h.context;
    for (const name of ["syncMetricOrientations", "renderParams", "collectParams"]) {
        const start = app.indexOf("function " + name + "("); vm.runInContext(app.slice(start, app.indexOf("\n}\n", start) + 3), c);
    }
    h.state.directed = false;
    const params = [
        { name: "uSel", label: "Source neighbours", type: "orientation", options: ["OUT", "IN", "UND", "MUTUAL"], default: "OUT" },
        { name: "vSel", label: "Target neighbours", type: "orientation", options: ["OUT", "IN", "UND", "MUTUAL"], default: "IN" },
        { name: "orientation", label: "Boundary direction", type: "string", options: ["UND", "OUT", "IN"], default: "OUT" },
        { name: "choice", label: "Other choice", type: "string", options: ["A", "B"], default: "B" },
        { name: "alpha", label: "Alpha", type: "double", default: 0.25 },
    ];
    for (const family of ["vertex", "graph", "pair", "communityGlobal", "communityIndividual", "intraCommunityIndividual", "intraAttributeIndividual", "edgePartitionGlobal", "edgePartitionIndividual", "intraEdgePartitionIndividual"]) {
        h.state.defs = { [family]: { metric: { params } } }; c.renderParams("vertex-params", family, "metric");
        const inputs = h.$("vertex-params").querySelectorAll("[data-param]");
        for (const input of inputs.slice(0, 3)) { assert.equal(input.value, "UND", family); assert.equal(input.disabled, true, family); }
        assert.equal(inputs[3].value, "B"); assert.equal(!!inputs[3].disabled, false);
        inputs[0].value = "IN";
        assert.deepEqual(plain(c.collectParams("vertex-params")), { uSel: "UND", vSel: "UND", orientation: "UND", choice: "B", alpha: 0.25 });
    }
});

test("directed metric orientations remain selectable and recover after switching graph direction", () => {
    const h = workspaceDom(), app = read("app.js"), c = h.context;
    for (const name of ["syncMetricOrientations", "renderParams", "collectParams"]) {
        const start = app.indexOf("function " + name + "("); vm.runInContext(app.slice(start, app.indexOf("\n}\n", start) + 3), c);
    }
    const params = [{ name: "orientation", label: "Orientation", type: "orientation", options: ["OUT", "IN", "UND"], default: "OUT" }];
    h.state.defs = { pair: { metric: { params } }, propagation: { algorithm: { params } } }; h.state.directed = true;
    c.renderParams("pair-params", "pair", "metric");
    const input = h.$("pair-params").querySelector("[data-param]"); assert.equal(input.value, "OUT"); assert.equal(!!input.disabled, false);
    input.value = "IN"; assert.equal(c.collectParams("pair-params").orientation, "IN");
    h.state.directed = false; c.syncMetricOrientations(); assert.equal(input.value, "UND"); assert.equal(input.disabled, true);
    c.renderParams("vertex-params", "propagation", "algorithm");
    const protocolInput = h.$("vertex-params").querySelector("[data-param]"); assert.equal(protocolInput.value, "OUT"); assert.equal(!!protocolInput.disabled, false);
    h.state.directed = true; c.syncMetricOrientations(); assert.equal(input.value, "IN"); assert.equal(input.disabled, false);
    assert.equal(c.collectParams("pair-params").orientation, "IN");
});

test("HITS mode uses Hubs and Authorities labels while submitting boolean parameters", () => {
    const h = workspaceDom(), app = read("app.js"), c = h.context;
    for (const name of ["syncMetricOrientations", "renderParams", "collectParams"]) {
        const start = app.indexOf("function " + name + "("); vm.runInContext(app.slice(start, app.indexOf("\n}\n", start) + 3), c);
    }
    h.state.defs = { vertex: { HITS: { params: [{ name: "mode", label: "Authority (off = hub)", type: "bool", default: true }] } } };
    c.renderParams("vertex-params", "vertex", "HITS");
    const input = h.$("vertex-params").querySelector("[data-param]");
    assert.deepEqual(input.options.map(item => [item.value, item.textContent]), [["false", "Hubs"], ["true", "Authorities"]]);
    assert.equal(c.collectParams("vertex-params").mode, true);
    input.value = "false"; assert.equal(c.collectParams("vertex-params").mode, false);
});

test("authority-based recommendation models show Hubs and Authorities and retain other boolean controls", () => {
    const h = workspaceDom(), app = read("app.js"), c = h.context;
    for (const name of ["syncMetricOrientations", "renderParams", "collectParams"]) {
        const start = app.indexOf("function " + name + "("); vm.runInContext(app.slice(start, app.indexOf("\n}\n", start) + 3), c);
    }
    for (const model of ["HITS", "SALSA", "Personalized HITS", "Personalized SALSA", "Money", "Love"]) {
        const label = ["Money", "Love"].includes(model) ? "Use authorities" : "Authorities (vs. hubs)";
        h.state.defs = { recommendation: { [model]: { params: [
            { name: "mode", label, type: "bool", default: true },
            { name: "weighted", label: "Weighted", type: "bool", default: false },
        ] } } };
        c.renderParams("vertex-params", "recommendation", model);
        const inputs = h.$("vertex-params").querySelectorAll("[data-param]");
        assert.deepEqual(inputs[0].options.map(item => [item.value, item.textContent]), [["false", "Hubs"], ["true", "Authorities"]], model);
        assert.equal(c.collectParams("vertex-params").mode, true, model);
        inputs[0].value = "false"; inputs[1].checked = true;
        assert.deepEqual(plain(c.collectParams("vertex-params")), { mode: false, weighted: true }, model);
    }
});

test("all distribution types support several recommendations with Original deselected and export only visible sources", () => {
    const { context: c, state, entry } = harness();
    state.rec.models.other = { label: "Other" };
    state.recMetricData.vertex.Degree.other = { a: 8, b: 9, c: 9 };
    const sources = c.metricResultSources(entry()).filter(source => source.id !== "original");
    for (const type of ["histogram", "cumulative", "frequency", "boxplot"]) {
        const result = c.metricAnalysisDistribution(entry(), type, null, null, "both", sources);
        assert.equal(result.empty, false); assert.doesNotMatch(result.note, /Original/);
        assert.equal(result.option.series.length, type === "boxplot" ? 1 : 2);
        if (type === "boxplot") assert.deepEqual(plain(result.option.xAxis.data), ["Model A", "other"]);
        assert.ok(result.rows.slice(1).every(row => row[0] !== "Original"));
    }
    assert.equal(c.metricAnalysisDistribution(entry(), "histogram", null, null, "lines", []).empty, true);
});

test("multi-series comparisons match each recommendation independently, including recommendation versus recommendation", () => {
    const { context: c, entry } = harness(), original = entry();
    const one = { a: 1, b: 6, c: null }, two = { a: 4, b: 20, c: 2 };
    const targets = [{ name: "A", colorIndex: 1, x: original.values, y: one }, { name: "B", colorIndex: 2, x: original.values, y: two }];
    const plot = c.metricAnalysisComparisonSeries(original, null, "recommendation", { name: "Original" }, targets);
    assert.equal(plot.option.series.length, 2);
    assert.equal(plot.option.xAxis.min, plot.option.yAxis.min); assert.equal(plot.option.xAxis.max, plot.option.yAxis.max);
    assert.equal(plot.rows.length, 5); assert.equal(plot.option.series[1].data[1][1], 20);
    const versus = c.metricAnalysisComparisonSeries(original, null, "rec-rec", { name: "A" }, [{ name: "B", colorIndex: 2, x: one, y: two }]);
    assert.deepEqual(plain(versus.option.series[0].data), [[1, 4, "a"], [6, 20, "b"]]);
    assert.match(versus.note, /Reference: A/);
    const difference = c.metricAnalysisComparisonSeries(original, null, "rec-rec", { name: "A" }, [{ name: "B", colorIndex: 2, x: one, y: two }], "difference");
    assert.equal(difference.rows[1].at(-1), 3); assert.equal(difference.rows[2].at(-1), 14);
    assert.match(difference.note, /target − A/);
    const metric = c.metricAnalysisComparisonSeries(original, { ...original, label: "Other metric" }, "metric", null,
        [{ name: "A", colorIndex: 1, x: one, y: { b: 0.2, a: 0.1 } }]);
    assert.equal(metric.option.series[0].markLine, undefined); assert.deepEqual(plain(metric.option.legend.data), ["A"]);
    assert.equal(c.metricAnalysisComparisonSeries(original, original, "metric", null, []).empty, true);
});

test("stored pair distributions retain each recommendation's actual bins and can omit Original", () => {
    const { context: c } = harness();
    const sources = [{ name: "A", colorIndex: 1, aggregate: { totalPairs: 5, histogram: [{ x0: 0, x1: 2, count: 5 }] } },
        { name: "B", colorIndex: 2, aggregate: { estimated: true, evaluated: 4, totalPairs: 10, histogram: [{ x0: 1, x1: 3, count: 4 }] } }];
    const entry = { label: "Distance", values: {}, labels: {}, aggregate: {} };
    const histogram = c.metricAnalysisDistribution(entry, "histogram", null, null, "both", sources);
    assert.equal(histogram.option.series.length, 2); assert.equal(histogram.option.series[0].type, "bar");
    assert.deepEqual(plain(histogram.option.xAxis.data), ["0–2", "1–3"]);
    assert.deepEqual(plain(histogram.option.series[0].data), [5, null]);
    assert.deepEqual(plain(histogram.option.series[1].data), [null, 4]);
    assert.deepEqual(plain(histogram.rows.slice(1).map(row => row.slice(0, 4))), [["A", 0, 2, 5], ["B", 1, 3, 4]]);
    assert.ok(histogram.rows.slice(1).every(row => row[0] !== "Original"));
    const cdf = c.metricAnalysisDistribution(entry, "cumulative", null, null, "points", sources);
    assert.equal(cdf.option.series[1].data[0][1], 100); assert.match(cdf.note, /Approximate/);
    const renderer = vm.createContext({ setTimeout, clearTimeout, console }); vm.runInContext(read("vendor/echarts.min.js"), renderer);
    for (const result of [histogram, cdf]) {
        const chart = renderer.echarts.init(null, null, { renderer: "svg", ssr: true, width: 700, height: 400 });
        try { chart.setOption(result.option); const svg = chart.renderToSVGString(); assert.match(svg, /<svg/); assert.doesNotMatch(svg, /NaN|Infinity/); }
        finally { chart.dispose(); }
    }
});

test("pair histograms group bars without overlap and put separator lines and ticks at bin boundaries", () => {
    const { context: c } = harness();
    const sources = [0, 1, 2].map(i => ({ name: "Result " + i, colorIndex: i, aggregate: {
        totalPairs: 3 * (i + 1), finite: 3 * (i + 1),
        valueCounts: [0, 2, 4].map(value => ({ value, count: i + 1 })),
    } }));
    const result = c.metricAnalysisDistribution({ label: "Distance", values: {}, labels: {}, aggregate: {} }, "histogram", 2, null, "lines", sources);
    assert.deepEqual(plain(result.option.xAxis.data), ["0–2", "2–4"]);
    assert.equal(result.option.xAxis.axisTick.show, true); assert.equal(result.option.xAxis.axisTick.alignWithLabel, false);
    assert.equal(result.option.xAxis.axisTick.interval, 0); assert.equal(result.option.xAxis.splitLine.show, true); assert.equal(result.option.xAxis.splitLine.interval, 0);
    assert.equal(result.option.xAxis.axisLabel.formatter, undefined);
    result.option.series.forEach((series, i) => assert.deepEqual(plain(series.data), [i + 1, 2 * (i + 1)]));
    assert.deepEqual(plain(result.rows.slice(1).map(row => row.slice(1, 4))), [[0, 2, 1], [2, 4, 2], [0, 2, 2], [2, 4, 4], [0, 2, 3], [2, 4, 6]]);
    const renderer = vm.createContext({ setTimeout, clearTimeout, console }); vm.runInContext(read("vendor/echarts.min.js"), renderer);
    for (const width of [360, 1000]) {
        const chart = renderer.echarts.init(null, null, { renderer: "svg", ssr: true, width, height: 420 });
        try {
            chart.setOption(result.option);
            const model = chart.getModel(), rectangles = [];
            for (let bin = 0; bin < 2; bin++) for (let source = 0; source < 3; source++) rectangles.push(model.getSeriesByIndex(source).getData().getItemLayout(bin));
            rectangles.sort((a, b) => a.x - b.x);
            for (let i = 1; i < rectangles.length; i++) assert.ok(rectangles[i - 1].x + rectangles[i - 1].width <= rectangles[i].x + 1e-8, "bars must not overlap at width " + width);
            assert.equal(model.getComponent("xAxis").axis.getTicksCoords().length, 3);
            const svg = chart.renderToSVGString(); assert.match(svg, /<svg/); assert.doesNotMatch(svg, /NaN|Infinity/);
        } finally { chart.dispose(); }
    }
});

test("source checkboxes can remove Original, add multiple recommendations and clear every series without restoring defaults", () => {
    const h = workspaceDom();
    h.state.rec.models.other = { label: "Other" };
    h.state.recMetricData.vertex.Degree.other = { a: 8, b: 9, c: 9 };
    h.state.recMetricData.vertex.Centrality = { model: { a: 0.2, b: 0.4 }, other: { a: 0.3, b: 0.9 } };
    const change = (panel, index, checked) => {
        const input = panel.querySelector(".metric-series-picker").querySelectorAll("input")[index];
        input.checked = checked; input.focus(); input.change(input.value);
    };
    h.$("metric-nodes-distributions-tab").click(); const panel = h.view("nodes").panels.distributions;
    change(panel, 1, true); change(panel, 2, true); change(panel, 0, false);
    assert.equal(h.renders.at(-1).option.series.length, 2); assert.deepEqual(plain(h.renders.at(-1).option.legend.data), ["Model A", "other"]);
    assert.ok(h.view("nodes").rows.slice(1).every(row => row[0] !== "Original"));
    change(panel, 1, false); change(panel, 2, false);
    assert.equal(h.renders.at(-1).cleared, "node-analysis-distributions"); assert.match(panel.querySelector("[data-empty]").textContent, /Select at least one/);
    h.$("metric-nodes-compare-tab").click(); const compare = h.view("nodes").panels.compare;
    change(compare, 1, true); change(compare, 2, true); change(compare, 0, false);
    assert.equal(h.renders.at(-1).option.series.length, 2);
    compare.querySelector('[data-analysis-key="compare"]').change("recommendation"); change(compare, 1, true);
    assert.equal(h.renders.at(-1).option.series.length, 2);
    compare.querySelector('[data-analysis-key="compare"]').change("rec-rec");
    assert.equal(h.renders.at(-1).option.series.length, 1); assert.match(h.renders.at(-1).title, /Model A vs/);
});

test("statistics and real ranking renderer select recommendation data without changing original metrics", () => {
    const h = workspaceDom();
    h.view("nodes").statisticsSources.querySelector("select").change("rec:model");
    const degree = h.view("nodes").statisticsRows.find(row => row[1] === "Degree");
    assert.equal(degree[0], "Model A"); assert.equal(degree[2], 3.5);
    h.context.setSelectOptions("node-chart-sort", ["id", "Degree", "Centrality"], ["ID", "Degree", "Centrality"]);
    h.context.setSelectOptions("node-chart-metric", ["Degree", "Centrality"], ["Degree", "Centrality"]);
    h.$("metric-nodes-rankings-tab").click();
    h.view("nodes").rankingSources.querySelector("select").change("rec:model");
    const source = read("metric-charts.js").replace(/\r\n/g, "\n"), start = source.indexOf("function drawNodeChart(");
    vm.runInContext(source.slice(start, source.indexOf("\n}\n", start) + 3), h.context);
    let items, title;
    h.context.drawBarChart = (_id, _tip, data, name) => { items = data; title = name; };
    h.context.drawNodeChart();
    assert.ok(items, JSON.stringify({ metric: h.view("nodes").metric, source: h.view("nodes").rankingSource, selected: h.$("node-chart-metric").value, last: h.renders.at(-1) }));
    assert.deepEqual(plain(items.map(item => item.value)), [1, 6, null]); assert.match(title, /Model A/);
    assert.equal(h.state.metricData.Degree.b, 4);
});

test("metric requests retain exact submitted parameters and recover supported older result configurations", () => {
    const { context: c, state } = harness();
    c.rememberMetricComputation("/api/metrics/vertex", JSON.stringify({ graphId: "g", metric: "DEGREE", params: { orientation: "IN" } }), { label: "Degree (orientation=IN)" });
    const recipe = Object.values(state.metricComputations)[0]; assert.equal(recipe.body.params.orientation, "IN");
    c.rememberMetricComputation("/api/metrics/vertex", JSON.stringify({ withRecommendation: true, metric: "other" }), { label: "Degree (orientation=IN)" });
    assert.equal(Object.values(state.metricComputations).length, 1);
    state.metricOrder = ["Degree (orientation=IN)"]; state.pairOrder = []; state.graphMetrics = {};
    assert.equal(c.metricComputationInventory().recipes[0].body.metric, "DEGREE");
    state.metricComputations = {}; state.defs = { vertex: { DEGREE: { label: "Degree", params: [{ name: "orientation", type: "orientation" }] } } };
    const recovered = c.metricComputationInventory(); assert.equal(recovered.skipped.length, 0); assert.equal(recovered.recipes[0].body.params.orientation, "IN");
    assert.equal(c.metricRecipeParameters({ params: [{ name: "n", type: "int" }, { name: "enabled", type: "bool" }] }, " (n=3, enabled=false)").enabled, false);
});

test("batch computation explicitly targets selected recommendations, keeps the visible overlay and stores each result separately", async () => {
    const requests = [];
    const { context: c, state } = harness({ jsonBody: value => ({ body: JSON.stringify(value) }), isAbort: error => error.name === "AbortError",
        api: async (url, options) => { const body = JSON.parse(options.body); requests.push(body); return { recommendation: body.recommendationKey, values: { a: 0, b: body.recommendationKey === "model" ? 7 : 9 } }; } });
    state.graphId = "g"; state.rec.active = "model"; state.rec.models.other = { label: "Other" };
    const recipe = { label: "Degree", recFamily: "vertex", family: "nodes", shape: "map", url: "/api/metrics/vertex", body: { metric: "DEGREE", params: { orientation: "IN" } } };
    const result = await c.runMetricRecommendationBatch(["model", "other"], [recipe]);
    assert.equal(result.completed, 1); assert.equal(result.skipped, 1); assert.equal(result.errors.length, 0);
    assert.deepEqual(requests.map(body => body.recommendationKey), ["other"]); assert.equal(requests[0].params.orientation, "IN");
    assert.equal(state.rec.active, "model"); assert.equal(state.metricData.Degree.b, 4);
    assert.equal(state.recMetricData.vertex.Degree.model.b, 6); assert.equal(state.recMetricData.vertex.Degree.other.b, 9);
});

test("batch failures stay separate, cancellation stops scheduling, and late responses cannot populate a changed graph", async () => {
    const recipe = { label: "Degree", recFamily: "vertex", family: "nodes", shape: "map", url: "/api/metrics/vertex", body: { metric: "DEGREE" } };
    const h = harness({ jsonBody: body => ({ body: JSON.stringify(body) }), isAbort: error => error.name === "AbortError" });
    delete h.state.recMetricData.vertex.Degree.model;
    let requests = 0; h.context.api = async () => { requests++; throw new Error("failed metric"); };
    let result = await h.context.runMetricRecommendationBatch(["model"], [recipe]);
    assert.equal(result.errors.length, 1); assert.equal(result.completed, 0);
    h.context.api = async () => ({ cancelled: true });
    result = await h.context.runMetricRecommendationBatch(["model"], [recipe, recipe]); assert.equal(result.stopped, true);
    h.state.graphId = "first";
    h.context.api = async () => { h.state.graphId = "second"; return { values: { a: 55 } }; };
    result = await h.context.runMetricRecommendationBatch(["model"], [recipe]);
    assert.equal(result.stopped, true); assert.equal(h.state.recMetricData.vertex.Degree.model, undefined);
    result = await h.context.runMetricRecommendationBatch(["model"], [recipe], undefined, () => {}, () => true);
    assert.equal(result.stopped, true); assert.equal(requests, 1);
});

test("recommendation attribute values align by category label when server-generated IDs change", () => {
    const { context: c, state } = harness();
    state.attributeMetricData.Count = { values: { 0: 3, 1: 4 }, valueLabels: { 0: "US", 1: "GB" } };
    const result = c.metricRecommendationValue({ family: "attrcomm", label: "Count", shape: "map" },
        { values: { 10: 5, 11: 6 }, valueLabels: { 10: "GB", 11: "US" } });
    assert.deepEqual(plain(result), { 0: 6, 1: 5 });
});

test("the batch dialog runs the checked recommendations with the submitted metric configuration", async () => {
    const h = workspaceDom(), requests = [];
    h.context.requireGraph = () => true;
    h.context.jsonBody = body => ({ body: JSON.stringify(body) });
    h.context.isAbort = error => error.name === "AbortError";
    h.context.beginJob = () => ({}); h.context.endJob = () => {};
    h.context.renderMetricsDashboard = () => h.context.refreshMetricWorkspace();
    h.state.graphId = "g"; h.state.rec.active = "model"; h.state.rec.models.other = { label: "Other" };
    h.context.rememberMetricComputation("/api/metrics/vertex", JSON.stringify({ graphId: "g", metric: "DEGREE", params: { orientation: "IN" } }), { label: "Degree" });
    h.context.api = async (_url, options) => { const body = JSON.parse(options.body); requests.push(body); return { recommendation: body.recommendationKey, values: { a: 2, b: 8 } }; };
    h.context.openMetricRecommendationBatch(); assert.equal(h.$("metric-recompute-dialog").open, true);
    const choices = h.$("metric-recompute-choices").querySelectorAll("input");
    choices.forEach(choice => { choice.checked = true; }); choices[0].change(choices[0].value);
    assert.equal(h.$("metric-recompute-run").disabled, false);
    await h.$("metric-recompute-run").onclick();
    assert.equal(requests.length, 1); assert.equal(requests[0].params.orientation, "IN");
    assert.equal(h.state.rec.active, "model"); assert.match(h.$("metric-recompute-note").textContent, /Finished. 1/);
    assert.match(h.$("metric-recompute-note").textContent, /1 already computed and skipped/);
    assert.equal(h.$("metric-recompute-run").disabled, true);
    assert.equal(h.state.recMetricData.vertex.Degree.other.b, 8);
});

test("recommendation distributions include newly added links and newly present attribute categories", () => {
    const { context: c, state } = harness();
    state.directed = true;
    state.rec.models.model.edges = [{ source: "b", target: "c" }];
    state.recMetricData.pair.Distance.model["b|c"] = 7;
    const edge = c.metricAnalysisEntries("edges")[0], source = c.metricResultSources(edge)[1];
    assert.equal(source.values.length, 3);
    const extra = Object.keys(source.map).find(key => key.startsWith("recommendation-edge:"));
    assert.equal(source.map[extra], 7); assert.equal(source.labels[extra], "b→c");
    const plot = c.metricAnalysisDistribution(edge, "histogram", 1, null, "lines", [source]);
    assert.equal(plot.option.series[0].data.reduce((sum, value) => sum + value, 0), 3);
    assert.ok(plot.rows.some(row => row[1] === "b→c"));
    state.edgeAttributeMetricData.Count = { attribute: "kind", values: { 0: 2 }, valueLabels: { 0: "friend" } };
    state.edgeAttributeMetricOrder = ["Count"];
    const map = c.metricRecommendationValue({ family: "edgeattr", label: "Count", shape: "map" }, { values: { 10: 2, 11: 1 }, valueLabels: { 10: "friend", 11: "(missing)" } });
    state.recMetricData.edgeattr = { Count: { model: map } };
    const attribute = c.metricResultSources(c.metricAnalysisEntries("edgeattr")[0])[1];
    assert.equal(attribute.values.length, 2); assert.equal(attribute.map['attribute-value:"(missing)"'], 1);
});

test("recommendations with the same model label have distinct legend and selector labels", () => {
    const { context: c, state } = harness();
    state.rec.models = { one: { label: "Neighbors", mode: "recommendation", cutoff: 5 }, two: { label: "Neighbors", mode: "recommendation", cutoff: 10 } };
    const source = read("app.js").replace(/\r\n/g, "\n"), start = source.indexOf("function recLabel(");
    vm.runInContext(source.slice(start, source.indexOf("\n}\n", start) + 3), c);
    assert.notEqual(c.recLabel("one"), c.recLabel("two")); assert.match(c.recLabel("one"), /k=5/);
    assert.equal(c.recLabel("missing"), "missing");
});
