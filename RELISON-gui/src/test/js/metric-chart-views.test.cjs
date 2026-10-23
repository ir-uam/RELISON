const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

const source = fs.readFileSync(path.join(__dirname, "../../main/resources/public/metric-charts.js"), "utf8");
const section = source.slice(source.indexOf("function clearChart("), source.indexOf("/* ------------------------------ scatterplots"));
const ids = ["node-chart", "edge-chart", "comm-chart", "attrcomm-chart", "edgeattr-chart", "ep-chart"];
const plain = value => JSON.parse(JSON.stringify(value));

function setup() {
    const controls = new Map();
    let option, redraws = 0;
    ids.forEach(id => {
        for (const [suffix, value] of [["orientation", "vertical"], ["show", "all"], ["order", "normal"], ["k", "10"], ["sort", "id"], ["view-note", ""]]) {
            const label = { hidden: true };
            controls.set(id + "-" + suffix, { value, textContent: "", disabled: false, validity: { valid: true },
                handlers: {}, addEventListener(name, fn) { this.handlers[name] = fn; }, closest: () => label });
        }
    });
    const colors = { text: "#ffffff", muted: "#aaaaaa", border: "#333333", accent: "#4f9dff", chartStyle: "quiet-scientific",
        fontSize: 13, labelSize: 12, gridOpacity: 0.38, barRadius: [7, 7, 0, 0] };
    const context = vm.createContext({ $: id => controls.get(id), fmt: String,
        escapeChartHtml: value => String(value).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;"),
        echartColors: () => colors, chartBase: () => ({ tooltip: {} }), showsMetricChartTitle: () => false,
        showEChart: (id, value) => { option = value; },
        echarts: { graphic: { LinearGradient: class { constructor(...args) { this.args = args; } } } },
        ECHARTS: new Map(), ECHART_PENDING: new Map(), ECHART_RESIZE_OBSERVERS: new Map(),
        drawNodeChart() { redraws++; }, drawEdgeChart() { redraws++; }, drawCommChart() { redraws++; },
        drawAttributeCommChart() { redraws++; }, drawEdgeAttributeChart() { redraws++; }, drawEdgePartitionChart() { redraws++; },
    });
    vm.runInContext(section, context);
    return { context, controls, colors, draw(id, items) {
        context.drawBarChart(id, "", items, "degree", "nodes", "degree");
        return option;
    }, get redraws() { return redraws; } };
}

test("All preserves the existing category sort order and values", () => {
    const h = setup();
    const items = [{ label: "a", value: -5 }, { label: "b", value: 20 }, { label: "c", value: 0 }];
    const option = h.draw("node-chart", items);
    assert.equal(option.xAxis.type, "category");
    assert.equal(option.yAxis.type, "value");
    assert.deepEqual(plain(option.xAxis.data), ["a", "b", "c"]);
    assert.deepEqual(plain(option.series[0].data), [-5, 20, 0]);
    assert.equal(h.controls.get("node-chart-view-note").textContent, "Showing all 3.");
});

test("Top k ranks the selected metric across each category chart without mutating source data", () => {
    for (const id of ids) {
        const h = setup();
        h.controls.get(id + "-show").value = "top";
        h.controls.get(id + "-k").value = "2";
        const items = [{ label: "a", value: -5 }, { label: "b", value: 20 }, { label: "c", value: 8 }, { label: "d", value: NaN }];
        const option = h.draw(id, items);
        assert.deepEqual(plain(option.xAxis.data), ["b", "c"]);
        assert.deepEqual(plain(option.series[0].data), [20, 8]);
        assert.deepEqual(items.map(item => item.label), ["a", "b", "c", "d"]);
        assert.match(h.controls.get(id + "-view-note").textContent, /top 2 of 4/);
        h.controls.get(id + "-k").value = "100";
        assert.equal(h.draw(id, items).series[0].data.length, 3);
    }
});

test("horizontal Top k puts the highest category at the top and keeps full escaped labels in tooltips", () => {
    const h = setup();
    h.controls.get("node-chart-orientation").value = "horizontal";
    h.controls.get("node-chart-show").value = "top";
    h.controls.get("node-chart-k").value = "1";
    const option = h.draw("node-chart", [{ label: "low", value: 1 }, { label: "<a very long category name>", value: 8 }]);
    assert.equal(option.xAxis.type, "value");
    assert.equal(option.xAxis.name, "degree");
    assert.equal(option.yAxis.type, "category");
    assert.equal(option.yAxis.name, "nodes");
    assert.equal(option.yAxis.inverse, true);
    assert.equal(option.yAxis.axisLabel.rotate, 0);
    assert.equal(option.yAxis.axisLabel.overflow, "truncate");
    assert.equal(option.tooltip.formatter([{ dataIndex: 0 }]), "degree: <b>8</b><br>nodes: &lt;a very long category name&gt;");
    assert.deepEqual(plain(option.series[0].itemStyle.borderRadius), [0, 7, 7, 0]);
});

test("Bottom k selects the lowest finite values, including negatives and zero", () => {
    for (const id of ids) {
        const h = setup();
        h.controls.get(id + "-show").value = "bottom";
        h.controls.get(id + "-k").value = "2";
        const option = h.draw(id, [{ label: "high", value: 20 }, { label: "zero", value: 0 },
            { label: "negative", value: -5 }, { label: "missing", value: NaN }, { label: "infinite", value: Infinity }]);
        assert.deepEqual(plain(option.xAxis.data), ["negative", "zero"]);
        assert.deepEqual(plain(option.series[0].data), [-5, 0]);
        assert.match(h.controls.get(id + "-view-note").textContent, /bottom 2 of 5.*lowest finite values/);
        h.controls.get(id + "-k").value = "100";
        assert.equal(h.draw(id, [{ label: "only", value: 4 }]).series[0].data.length, 1);
    }
});

test("reverse changes display order after All, Top k, or Bottom k selection in both orientations", () => {
    const items = [{ label: "a", value: -5 }, { label: "b", value: 20 }, { label: "c", value: 0 }, { label: "d", value: 8 }];
    for (const id of ids) for (const orientation of ["vertical", "horizontal"]) {
        for (const [mode, expected] of [["all", ["d", "c", "b", "a"]], ["top", ["d", "b"]], ["bottom", ["c", "a"]]]) {
            const h = setup();
            h.controls.get(id + "-orientation").value = orientation;
            h.controls.get(id + "-show").value = mode;
            h.controls.get(id + "-k").value = "2";
            h.controls.get(id + "-order").value = "reverse";
            const option = h.draw(id, items);
            const categoryAxis = orientation === "horizontal" ? option.yAxis : option.xAxis;
            assert.deepEqual(plain(categoryAxis.data), expected);
            assert.deepEqual(plain(option.series[0].data), expected.map(label => items.find(item => item.label === label).value));
            assert.match(option.tooltip.formatter([{ dataIndex: 0 }]), new RegExp("nodes: " + expected[0]));
            assert.match(h.controls.get(id + "-view-note").textContent, /Display order reversed/);
            h.controls.get(id + "-order").value = "normal";
            const restored = h.draw(id, items);
            assert.deepEqual(plain((orientation === "horizontal" ? restored.yAxis : restored.xAxis).data), [...expected].reverse());
        }
    }
    assert.deepEqual(items.map(item => item.label), ["a", "b", "c", "d"]);
});

test("dense charts zoom the category axis in both orientations", () => {
    const h = setup();
    const items = Array.from({ length: 50 }, (_, i) => ({ label: String(i), value: i }));
    let option = h.draw("edge-chart", items);
    assert.equal(option.dataZoom[0].xAxisIndex, 0);
    assert.equal(option.dataZoom[1].height, 16);
    h.controls.get("edge-chart-orientation").value = "horizontal";
    option = h.draw("edge-chart", items);
    assert.equal(option.dataZoom[0].yAxisIndex, 0);
    assert.equal(option.dataZoom[0].xAxisIndex, undefined);
    assert.equal(option.dataZoom[1].orient, "vertical");
    assert.equal(option.dataZoom[1].width, 16);
});

test("controls toggle k and sorting, redraw, and normalize invalid k", () => {
    const h = setup();
    h.context.setupMetricBarControls();
    const show = h.controls.get("comm-chart-show"), k = h.controls.get("comm-chart-k"), sort = h.controls.get("comm-chart-sort");
    assert.equal(k.closest().hidden, true);
    show.value = "top";
    show.handlers.change();
    assert.equal(k.closest().hidden, false);
    assert.equal(sort.disabled, true);
    k.value = "";
    k.handlers.change();
    assert.equal(k.value, "10");
    show.value = "bottom";
    show.handlers.change();
    assert.equal(k.closest().hidden, false);
    assert.equal(sort.disabled, true);
    h.controls.get("comm-chart-order").handlers.change();
    show.value = "all";
    show.handlers.change();
    assert.equal(sort.disabled, false);
    assert.equal(sort.value, "id");
    assert.equal(k.closest().hidden, true);
    assert.equal(h.redraws, 5);
});

test("histograms without category controls retain every bin and their vertical axes", () => {
    const h = setup();
    const option = h.draw("pair-chart", [{ label: "0–1", value: 7 }, { label: "1–2", value: 4 }]);
    assert.equal(option.xAxis.type, "category");
    assert.deepEqual(plain(option.xAxis.data), ["0–1", "1–2"]);
    assert.deepEqual(plain(option.series[0].data), [7, 4]);
    assert.equal(option.series[0].barMinHeight, 3);
});
