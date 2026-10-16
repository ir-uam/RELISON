const test = require("node:test"), assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), vm = require("node:vm");
const read = name => fs.readFileSync(path.join(__dirname, "../../main/resources/public", name), "utf8");
const plain = value => JSON.parse(JSON.stringify(value));

function setup(horizontal = false, failRender = false) {
    const data = Array.from({ length: 1000 }, (_, i) => i);
    const category = { type: "category", data: data.map(String), boundaryGap: true };
    const option = { grid: [{ left: 50, right: 50, top: 50, bottom: 50 }],
        xAxis: [horizontal ? { type: "value" } : category], yAxis: [horizontal ? category : { type: "value" }],
        dataZoom: [{ type: "slider", [horizontal ? "yAxisIndex" : "xAxisIndex"]: 0, startValue: 10, endValue: 14, show: true }],
        toolbox: [{ feature: { restore: {} } }], legend: [{ selected: { base: true, comparison: false } }],
        series: [{ name: "base", type: "bar", data: data.slice(10, 15), markLine: { data: [{ yAxis: 12 }] } }],
    };
    const elements = new Map(), downloads = [], renders = [], statuses = [];
    let disposed = 0, pngCalls = 0;
    const $ = id => {
        if (!elements.has(id)) {
            const element = { value: id === "chart-export-format" ? "png" : "", handlers: {},
                addEventListener(event, fn) { this.handlers[event] = fn; },
                showModal() { this.open = true; }, close() { this.open = false; this.handlers.close?.(); } };
            elements.set(id, element);
        }
        return elements.get(id);
    };
    const chart = { getWidth: () => 1000, getHeight: () => 500, isDisposed: () => false,
        getOption: () => structuredClone(option), __relisonOption: { series: [{ data }] },
        getDataURL: settings => { pngCalls++; renders.push({ png: settings }); return "data:image/png;base64,test"; } };
    const context = vm.createContext({ $, ECHARTS: new Map([["metric", chart]]),
        echartColors: () => ({ background: "#112233" }),
        echarts: { init: (host, theme, settings) => {
            const record = { host, settings }; renders.push(record);
            return { setOption: value => { record.option = value; },
                renderToSVGString: () => { if (failRender) throw new Error("render failed"); return '<svg xmlns="http://www.w3.org/2000/svg"><path d="M0 0L1 1"/><text>metric</text></svg>'; },
                dispose: () => disposed++ };
        } },
        document: { createElement: () => { throw new Error("SVG must not allocate a canvas or host"); } },
        download: (...args) => downloads.push(args), setStatus: (...args) => statuses.push(args),
        fetch: async () => ({ blob: async () => "png-blob" }), requestAnimationFrame: fn => fn(),
    });
    const labels = read("chart-labels.js");
    vm.runInContext(labels.slice(labels.indexOf("function categoryZoomTargets"), labels.indexOf("function categoryZoomRange")), context);
    vm.runInContext(read("chart-export.js"), context);
    context.setupChartPngExport();
    context.downloadChartPng("metric", "metric.png");
    return { context, $, option, downloads, renders, statuses, data,
        get disposed() { return disposed; }, get pngCalls() { return pngCalls; },
        format(value) { $("chart-export-format").value = value; $("chart-export-format").handlers.change(); } };
}

test("SVG enables complete exports beyond the PNG pixel cap in both chart orientations", async () => {
    for (const horizontal of [false, true]) {
        const h = setup(horizontal), original = structuredClone(h.option);
        assert.equal(h.$("chart-png-full").disabled, true);
        assert.equal(h.$("chart-png-limit").hidden, false);
        h.format("svg");
        assert.equal(h.$("chart-png-full").disabled, false);
        assert.equal(h.$("chart-png-limit").hidden, true);
        await h.context.exportChartPng("full");
        const rendered = h.renders[0];
        assert.equal(rendered.host, null); assert.equal(rendered.settings.renderer, "svg");
        assert.equal(rendered.settings.ssr, true);
        assert.ok((horizontal ? rendered.settings.height : rendered.settings.width) > 32767);
        assert.equal(rendered.option.series[0].data.length, 1000);
        assert.deepEqual(plain(rendered.option.dataZoom), []);
        assert.deepEqual(plain(rendered.option.legend), h.option.legend);
        assert.deepEqual(plain(rendered.option.series[0].markLine), h.option.series[0].markLine);
        assert.deepEqual(h.option, original);
        assert.equal(h.downloads[0][0], "metric-full.svg");
        assert.match(h.downloads[0][1], /<path/); assert.match(h.downloads[0][2], /image\/svg\+xml/);
        assert.equal(h.pngCalls, 0); assert.equal(h.disposed, 1);
    }
});

test("current SVG preserves zoom filtering, legend selections and viewport dimensions while hiding zoom controls", async () => {
    const h = setup(); h.format("svg"); await h.context.exportChartPng("current");
    const rendered = h.renders[0];
    assert.equal(rendered.settings.width, 1000); assert.equal(rendered.settings.height, 500);
    assert.equal(rendered.option.dataZoom[0].startValue, 10); assert.equal(rendered.option.dataZoom[0].endValue, 14);
    assert.equal(rendered.option.dataZoom[0].show, false); assert.equal(h.option.dataZoom[0].show, true);
    assert.deepEqual(plain(rendered.option.toolbox), []);
    assert.equal(rendered.option.legend[0].selected.comparison, false);
    assert.equal(rendered.option.backgroundColor, "#112233");
    assert.equal(rendered.option.series[0].progressive, 0);
    assert.equal(h.downloads[0][0], "metric.svg"); assert.equal(h.disposed, 1);
});

test("switching back to PNG retains its size guard and current-view PNG export", async () => {
    const h = setup(); h.format("svg"); h.format("png");
    assert.equal(h.$("chart-png-full").disabled, true);
    await h.context.exportChartPng("full");
    assert.equal(h.downloads.length, 0); assert.equal(h.renders.length, 0);
    assert.match(h.statuses.at(-1)[0], /too large for PNG/);
    h.context.downloadChartPng("metric", "metric.png");
    await h.context.exportChartPng("current");
    assert.equal(h.downloads[0][0], "metric.png"); assert.equal(h.pngCalls, 1);
    assert.equal(h.renders[0].png.pixelRatio, 2);
});

test("SVG rendering errors dispose the temporary renderer and leave the live chart intact", async () => {
    const h = setup(false, true), original = structuredClone(h.option);
    h.format("svg"); await h.context.exportChartPng("full");
    assert.equal(h.disposed, 1); assert.equal(h.downloads.length, 0);
    assert.match(h.statuses.at(-1)[0], /render failed/); assert.deepEqual(h.option, original);
});
