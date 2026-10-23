"use strict";

/* ------------------------------- charts ----------------------------- */

const ECHARTS = new Map();
const ECHART_RESIZE_OBSERVERS = new Map();
const ECHART_PENDING = new Map();

// Recalculate only layout properties so resizing preserves zoom and legend selections.
function responsiveChartLayout(option, width, height) {
    const compact = width < 520;
    const x = option.xAxis || {}, y = option.yAxis || {};
    const xFont = x.axisLabel?.fontSize || 12, yFont = y.axisLabel?.fontSize || 12;
    const textWidth = (text, size) => String(text).length * size * 0.65;
    const categoryWidth = (axis, size) => (axis.data || []).reduce((max, label) => Math.max(max, textWidth(label, size)), 0);
    const numericWidth = (axis, size, coordinate) => {
        let max = textWidth("0", size);
        const measure = value => {
            if (value != null && Number.isFinite(Number(value))) max = Math.max(max, textWidth(fmt(Number(value)), size));
        };
        measure(axis.min); measure(axis.max);
        (option.series || []).forEach(series => (series.data || []).forEach(item => {
            const value = item && typeof item === "object" && !Array.isArray(item) ? item.value : item;
            measure(Array.isArray(value) ? value[coordinate] : value);
        }));
        return max;
    };
    const yWidth = Math.min(Math.max(32, y.type === "category" ? categoryWidth(y, yFont) : numericWidth(y, yFont, 1)),
        Math.max(40, Math.min(160, width * 0.24)));
    const hasLegend = !!option.legend;
    const hasTitle = !!option.title?.text;
    const sliders = (option.dataZoom || []).filter(zoom => zoom.type === "slider");
    const verticalSlider = sliders.some(zoom => zoom.orient === "vertical");
    const horizontalSlider = sliders.some(zoom => zoom.orient !== "vertical");
    const heatmap = !!option.visualMap;
    const bottomScale = heatmap && compact;
    const endLabels = (option.series || []).some(series => series.endLabel?.show);
    const left = Math.ceil(yWidth + (y.name ? 40 : 16));
    const right = heatmap && !bottomScale ? 64 : verticalSlider ? 40 : endLabels && !compact ? Math.min(116, width * 0.2) : 16;
    const plotWidth = Math.max(40, width - left - right);
    const xWidth = x.type === "category" ? categoryWidth(x, xFont) : numericWidth(x, xFont, 0);
    const crowded = x.type === "category" && (x.data || []).length * Math.min(xWidth + 10, 100) > plotWidth;
    const rotate = crowded ? (compact ? 40 : 28) : 0;
    const labelWidth = Math.max(32, Math.min(xWidth || 40, compact ? 88 : 140, plotWidth * 0.45));
    const radians = rotate * Math.PI / 180;
    const xLabelHeight = Math.ceil(Math.sin(radians) * labelWidth + Math.cos(radians) * xFont);
    const bottom = xLabelHeight + 16 + (x.name ? 30 : 0) + (horizontalSlider ? 28 : 0) + (bottomScale ? 52 : 0);
    const top = (hasTitle ? 30 : 12) + (hasLegend ? 30 : 0) + (option.toolbox && !hasTitle && !hasLegend ? 18 : 0);
    const plotHeight = Math.max(40, height - top - bottom);
    const axisNameStyle = (axis, limit) => ({ ...axis.nameTextStyle, width: limit, overflow: "truncate", ellipsis: "…" });
    const patch = {
        grid: { ...option.grid, left, right, top, bottom, containLabel: false },
        xAxis: { ...x, nameLocation: "middle", nameGap: xLabelHeight + 20,
            nameTextStyle: axisNameStyle(x, plotWidth),
            axisLabel: { ...x.axisLabel, rotate, hideOverlap: true,
                ...(x.type === "category" ? { width: labelWidth, overflow: "truncate" } : {}) } },
        yAxis: { ...y, nameLocation: "middle", nameGap: yWidth + 14,
            nameTextStyle: axisNameStyle(y, plotHeight),
            axisLabel: { ...y.axisLabel, width: yWidth, overflow: "truncate", hideOverlap: true } },
    };
    if (option.title) patch.title = { ...option.title, left, right: 40,
        textStyle: { ...option.title.textStyle, width: Math.max(40, width - left - 40), overflow: "truncate" } };
    if (option.toolbox) patch.toolbox = { ...option.toolbox, right: 8 };
    if (hasLegend) patch.legend = { type: compact || (option.series || []).length > 8 ? "scroll" : (option.legend.type || "plain"),
        left, right: 40, top: hasTitle ? 30 : 4 };
    if (heatmap) patch.visualMap = { orient: bottomScale ? "horizontal" : "vertical",
        left: bottomScale ? "center" : "auto", right: bottomScale ? "auto" : 4,
        top: bottomScale ? "auto" : "middle", bottom: bottomScale ? 8 : "auto",
        itemWidth: 12, itemHeight: Math.min(140, bottomScale ? plotWidth * 0.65 : plotHeight * 0.6) };
    if (option.dataZoom) patch.dataZoom = option.dataZoom.map(zoom => zoom.type !== "slider" ? {} : {
        ...(zoom.orient === "vertical" ? { top, bottom, right: 8, width: 16 }
            : { left, right, bottom: 8 + (bottomScale ? 52 : 0), height: 16 }),
    });
    if (endLabels) patch.series = option.series.map(series => ({ endLabel: { ...series.endLabel,
        show: !compact && !!series.endLabel?.show, width: Math.max(32, right - 16), overflow: "truncate" } }));
    return patch;
}

function observeEChartContainer(el) {
    if (!window.ResizeObserver || ECHART_RESIZE_OBSERVERS.has(el.id)) return;
    const observer = new ResizeObserver(([entry]) => {
        if (entry.contentRect.width <= 0 || entry.contentRect.height <= 0) return;
        const pending = ECHART_PENDING.get(el.id);
        if (pending) {
            ECHART_PENDING.delete(el.id);
            observer.disconnect(); ECHART_RESIZE_OBSERVERS.delete(el.id);
            showEChart(el, pending.option, pending.title);
            return;
        }
        const chart = ECHARTS.get(el.id);
        if (chart && !chart.isDisposed()) {
            resizeEChartLayout(chart);
        }
    });
    observer.observe(el);
    const area = el.closest(".chart-area");
    if (area) observer.observe(area);
    ECHART_RESIZE_OBSERVERS.set(el.id, observer);
}

function getEChart(id) {
    const el = typeof id === "string" ? $(id) : id;
    if (!el || !window.echarts) return null;
    let chart = ECHARTS.get(el.id);
    if (chart && chart.isDisposed()) { ECHARTS.delete(el.id); chart = null; }
    if (!chart) {
        const rect = el.getBoundingClientRect();
        if (rect.width <= 0 || rect.height <= 0) return null;
        // Full-canvas redraw avoids hover emphasis artifacts across all ECharts series types.
        chart = echarts.init(el, null, { renderer: "canvas", useDirtyRect: false });
        ECHARTS.set(el.id, chart);
        observeEChartContainer(el);
        chart.on("finished", () => {
            if (chart.__relisonTitle && !chart.isDisposed()) capturePlot(chart, chart.__relisonTitle);
        });
        chart.on("restore", () => {
            if (chart.__categoryLabelMode === "all") requestAnimationFrame(() => {
                if (!chart.isDisposed()) resizeEChartLayout(chart);
            });
        });
    }
    return chart;
}

function disposeEChart(id) {
    const key = typeof id === "string" ? id : id?.id;
    const chart = key && ECHARTS.get(key);
    if (chart) {
        const observer = ECHART_RESIZE_OBSERVERS.get(key);
        if (observer) observer.disconnect();
        ECHART_RESIZE_OBSERVERS.delete(key);
        chart.dispose(); ECHARTS.delete(key);
    }
    ECHART_PENDING.delete(key);
    const observer = ECHART_RESIZE_OBSERVERS.get(key);
    if (observer) observer.disconnect();
    ECHART_RESIZE_OBSERVERS.delete(key);
}

function echartPaletteColors(id, light) {
    switch (id) {
    case "vibrant": return ["#00c2ff", "#a855f7", "#ff4d8d", "#ffb020", "#22c55e", "#ff6b35", "#6366f1", "#14b8a6", "#e879f9", "#84cc16"];
    case "editorial": return ["#264653", "#2a9d8f", "#e9c46a", "#f4a261", "#e76f51", "#457b9d"];
    case "glass": return ["#8c7bff", "#52d6c7", "#ff92bd", "#ffcb6b", "#83b8ff", "#c8a7ff", "#f19e7e"];
    case "monochrome": return light
        ? ["#b7472a", "#38424d", "#58636e", "#76818c", "#929ca6", "#4e687d", "#aeb6be", "#687783"]
        : ["#ff9777", "#e0e5ea", "#b8c1ca", "#909ca8", "#727f8b", "#9fc4d7", "#606d79", "#c8d0d7"];
    default: return ["#4f9dff", "#ff9f43", "#28c76f", "#e0576b", "#a66bff", "#22c3c3", "#f6c744", "#8892a0"];
    }
}

function echartColors() {
    const style = document.body.dataset.chartStyle || "quiet-scientific";
    const paletteId = document.body.dataset.chartPalette || "balanced";
    const vibrant = style === "vibrant-dashboard";
    const editorial = style === "editorial";
    const glass = style === "glass-panel";
    const monochrome = style === "monochrome-accent";
    const light = document.body.classList.contains("light");
    const palette = echartPaletteColors(paletteId, light);
    const accent = palette[0];
    return {
        text: cssVar("--text", "#e6e6e6"), muted: cssVar("--muted", "#9aa0a6"),
        border: cssVar("--border", "#333"), accent,
        panel: cssVar("--panel", "#26272b"), background: cssVar("--panel-2", "#1e1f23"),
        palette, chartStyle: style, gridOpacity: editorial ? 0.24 : vibrant ? 0.58 : glass ? 0.48 : monochrome ? 0.32 : 0.38,
        lineWidth: vibrant ? 2.6 : editorial ? 1.7 : glass ? 2.2 : monochrome ? 2 : 1.8,
        barRadius: vibrant ? [7, 7, 0, 0] : glass ? [5, 5, 2, 2] : editorial || monochrome ? [0, 0, 0, 0] : [2, 2, 0, 0],
        fontSize: editorial ? 14 : 13, labelSize: editorial ? 13 : 12,
        titleSize: editorial ? 17 : 14,
    };
}

function escapeChartHtml(value) {
    return String(value).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/\"/g, "&quot;").replace(/'/g, "&#39;");
}

function chartFeatureValueColor(value) {
    let h = 0; const s = String(value);
    for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0;
    const palette = echartColors().palette;
    return palette[Math.abs(h) % palette.length];
}

function chartBase(colors, title) {
    return {
        animation: false,
        backgroundColor: "transparent",
        color: colors.palette,
        textStyle: { color: colors.text, fontFamily: "sans-serif", fontSize: colors.fontSize },
        title: { text: title || "", left: 72, top: 3, textStyle: { color: colors.text, fontSize: colors.titleSize, fontWeight: colors.chartStyle === "editorial" ? 600 : 500 },
            subtextStyle: { color: colors.muted, fontSize: 10 } },
        tooltip: { trigger: "axis", confine: true,
            backgroundColor: colors.chartStyle === "glass-panel" ? (document.body.classList.contains("light") ? "rgba(255,255,255,0.82)" : "rgba(25,31,44,0.82)") : colors.panel,
            borderColor: colors.chartStyle === "glass-panel" ? "rgba(255,255,255,0.28)" : colors.border,
            extraCssText: colors.chartStyle === "glass-panel" ? "backdrop-filter:blur(12px);box-shadow:0 10px 32px rgba(0,0,0,.2);border-radius:12px;" : "",
            textStyle: { color: colors.text, fontSize: 12 }, axisPointer: { type: "shadow", shadowStyle: { opacity: 0.08 } } },
        toolbox: { right: 12, top: 2, feature: { restore: { title: "Reset zoom" } }, iconStyle: { borderColor: colors.muted } },
        grid: { left: 72, right: 22, top: 44, bottom: 58, containLabel: false },
    };
}

function showsMetricChartTitle(canvasId) {
    return !["node-chart", "edge-chart", "comm-chart", "attrcomm-chart", "edgeattr-chart", "ep-chart",
        "node-scatter", "edge-scatter", "comm-scatter"].includes(canvasId);
}

function showEChart(id, option, title) {
    const el = typeof id === "string" ? $(id) : id;
    if (!el) return null;
    const chart = getEChart(el);
    if (!chart) {
        ECHART_PENDING.set(el.id, { option, title });
        observeEChartContainer(el);
        return null;
    }
    chart.__relisonTitle = title || "chart";
    chart.__relisonOption = option;
    const layout = prepareCategoryChartLayout(el, option, null);
    const replaceZoom = layout.__replaceZoom;
    delete layout.__replaceZoom;
    chart.__categoryLabelMode = replaceZoom ? "all" : "automatic";
    chart.__categoryLabelZoom = replaceZoom ? (option.dataZoom || []).map(categoryZoomRange) : null;
    chart.resize();
    const series = layout.series ? option.series.map((series, i) => ({ ...series, ...layout.series[i] })) : option.series;
    const dataZoom = replaceZoom ? layout.dataZoom : layout.dataZoom ? option.dataZoom.map((zoom, i) => ({ ...zoom, ...layout.dataZoom[i] })) : option.dataZoom;
    const legend = layout.legend ? { ...option.legend, ...layout.legend } : option.legend;
    const visualMap = layout.visualMap ? { ...option.visualMap, ...layout.visualMap } : option.visualMap;
    chart.setOption({ ...option, ...layout, series, dataZoom, legend, visualMap }, { notMerge: true, lazyUpdate: false });
    return chart;
}

function drawNodeChart() {
    if (typeof metricWorkspaceReady !== "undefined" && metricWorkspaceReady) return drawMetricWorkspaceRanking("nodes");
    const metric = $("node-chart-metric").value;
    if (!state.graph || !metric || !state.metricData[metric]) { clearChart("node-chart"); return; }
    const sortBy = $("node-chart-sort").value;
    const values = state.metricData[metric];
    let nodes = state.graph.nodes().slice();
    if (sortBy === "id") nodes.sort((a, b) => Number(a) - Number(b));
    else {
        const sv = state.metricData[sortBy] || values;
        nodes.sort((a, b) => (sv[b] ?? 0) - (sv[a] ?? 0));
    }
    const items = nodes.map((n) => ({ label: n, value: values[n] ?? NaN }));
    drawBarChart("node-chart", "node-chart-tip", items, metric, "nodes", metric);
}

function drawEdgeChart() {
    if (typeof metricWorkspaceReady !== "undefined" && metricWorkspaceReady) return drawMetricWorkspaceRanking("edges");
    const metric = $("edge-chart-metric").value;
    if (!state.graph || !metric || !state.pairData[metric]) { clearChart("edge-chart"); return; }
    const sortBy = $("edge-chart-sort").value;
    const data = state.pairData[metric];
    const g = state.graph;
    let edges = g.edges().slice().filter((e) => data[pairKey(g.source(e), g.target(e))] !== undefined);
    const valOf = (e) => data[pairKey(g.source(e), g.target(e))] ?? NaN;
    if (sortBy === "pair") {
        edges.sort((a, b) => Number(g.source(a)) - Number(g.source(b)) || Number(g.target(a)) - Number(g.target(b)));
    } else {
        const sd = state.pairData[sortBy] || data;
        const sv = (e) => sd[pairKey(g.source(e), g.target(e))] ?? 0;
        edges.sort((a, b) => sv(b) - sv(a));
    }
    const items = edges.map((e) => ({ label: g.source(e) + "→" + g.target(e), value: valOf(e) }));
    drawBarChart("edge-chart", "edge-chart-tip", items, metric, "edges (source→target)", metric);
}

// The node-pair distribution is a histogram of the streamed aggregate (exact or sampled), so it scales to any N.
function drawPairChart() {
    const metric = $("pair-chart-metric").value;
    const agg = state.nodePairAgg[metric];
    if (!metric || !agg || !agg.histogram) { clearChart("pair-chart"); $("pair-chart-note").textContent = ""; return; }
    const items = agg.histogram.map((b) => ({ label: fmt(b.x0) + "–" + fmt(b.x1), value: b.count }));
    drawBarChart("pair-chart", "pair-chart-tip", items, metric + " — value distribution", "value", "count");
    const parts = [];
    parts.push(agg.estimated
        ? "Estimated from " + agg.evaluated.toLocaleString() + " sampled pairs of " + agg.totalPairs.toLocaleString() + " total."
        : "Exact over " + agg.totalPairs.toLocaleString() + " pairs.");
    if (agg.infinite) parts.push(agg.infinite.toLocaleString() + " pairs had no finite value (excluded).");
    parts.push("min " + fmt(agg.min) + ", max " + fmt(agg.max) + ".");
    $("pair-chart-note").textContent = parts.join(" ");
}

function drawCommChart() {
    if (typeof metricWorkspaceReady !== "undefined" && metricWorkspaceReady) return drawMetricWorkspaceRanking("comm");
    const metric = $("comm-chart-metric").value;
    const entry = state.commMetricData[metric];
    if (!metric || !entry) { clearChart("comm-chart"); return; }
    const values = entry.values;
    const sortBy = $("comm-chart-sort").value;
    let comms = Object.keys(values);
    if (sortBy === "community") {
        comms.sort((a, b) => Number(a) - Number(b));
    } else {
        const sv = state.commMetricData[sortBy]?.values || values;
        comms.sort((a, b) => (sv[b] ?? 0) - (sv[a] ?? 0));
    }
    const items = comms.map((c) => ({ label: communityLabel(c, entry.algorithm), value: values[c] ?? NaN }));
    drawBarChart("comm-chart", "comm-chart-tip", items, metric, "", communityMetricAxisTitle(entry, metric));
}

function drawAttributeCommChart() {
    if (typeof metricWorkspaceReady !== "undefined" && metricWorkspaceReady) return drawMetricWorkspaceRanking("attrcomm");
    const metric = $("attrcomm-chart-metric").value;
    const entry = state.attributeMetricData[metric];
    if (!metric || !entry) { clearChart("attrcomm-chart"); return; }
    const values = entry.values;
    const labels = entry.valueLabels || {};
    const sortBy = $("attrcomm-chart-sort").value;
    let ids = Object.keys(values);
    if (sortBy === "attribute") {
        ids.sort((a, b) => String(labels[a] ?? a).localeCompare(String(labels[b] ?? b), undefined, { numeric: true }));
    } else {
        const compared = state.attributeMetricData[sortBy]?.values || values;
        ids.sort((a, b) => (compared[b] ?? 0) - (compared[a] ?? 0));
    }
    const items = ids.map((id) => ({ label: String(labels[id] ?? id), value: values[id] ?? NaN }));
    drawBarChart("attrcomm-chart", "attrcomm-chart-tip", items, metric, "", communityMetricAxisTitle(entry, metric));
}

function communityMetricAxisTitle(entry, fullLabel) {
    if (entry.axisLabel) return entry.axisLabel;
    const families = ["communityIndividual", "intraCommunityIndividual", "intraAttributeIndividual"];
    for (const family of families) {
        const label = state.defs[family]?.[entry.metric]?.label;
        if (label) return label;
    }
    const metricLabel = String(fullLabel).split(" · ")[0];
    const catalogLabels = families.flatMap(family => Object.values(state.defs[family] || {}).map(def => def.label))
        .filter(Boolean).sort((a, b) => b.length - a.length);
    return catalogLabels.find(label => metricLabel === label || metricLabel.startsWith(label + " ")) || metricLabel;
}
function drawEdgeAttributeChart() {
    if (typeof metricWorkspaceReady !== "undefined" && metricWorkspaceReady) return drawMetricWorkspaceRanking("edgeattr");
    const metric = $("edgeattr-chart-metric").value;
    const entry = state.edgeAttributeMetricData[metric];
    if (!metric || !entry) { clearChart("edgeattr-chart"); return; }
    const values = entry.values, labels = entry.valueLabels || {};
    const sortBy = $("edgeattr-chart-sort").value;
    const ids = Object.keys(values);
    if (sortBy === "attribute") ids.sort((a, b) => String(labels[a] ?? a).localeCompare(String(labels[b] ?? b), undefined, { numeric: true }));
    else { const compared = state.edgeAttributeMetricData[sortBy]?.values || values; ids.sort((a, b) => (compared[b] ?? 0) - (compared[a] ?? 0)); }
    drawBarChart("edgeattr-chart", "edgeattr-chart-tip", ids.map((id) => ({ label: String(labels[id] ?? id), value: values[id] ?? NaN })), metric, "edge attribute values", metric);
}
function clearChart(canvasId) {
    const note = $(canvasId + "-view-note");
    if (note) note.textContent = "";
    const chart = ECHARTS.get(canvasId);
    if (!chart) {
        ECHART_PENDING.delete(canvasId);
        const observer = ECHART_RESIZE_OBSERVERS.get(canvasId);
        if (observer) observer.disconnect();
        ECHART_RESIZE_OBSERVERS.delete(canvasId);
    }
    if (chart && !chart.isDisposed()) {
        chart.__relisonOption = null;
        chart.__categoryLabelMode = null; chart.__categoryLabelZoom = null;
        chart.clear();
    }
}

function metricBarView(canvasId, items) {
    const horizontal = $(canvasId + "-orientation")?.value === "horizontal";
    const mode = $(canvasId + "-show")?.value;
    const ranked = mode === "top" || mode === "bottom";
    const reversed = $(canvasId + "-order")?.value === "reverse";
    const k = Math.max(1, Math.floor(Number($(canvasId + "-k")?.value) || 10));
    let displayed = ranked ? items.filter((item) => Number.isFinite(item.value))
        .slice().sort((a, b) => mode === "bottom" ? Number(a.value) - Number(b.value) : Number(b.value) - Number(a.value)).slice(0, k) : items;
    if (reversed) displayed = displayed.slice().reverse();
    const note = $(canvasId + "-view-note");
    if (note) note.textContent = (ranked
        ? "Showing " + mode + " " + displayed.length + " of " + items.length + " by the selected metric ("
            + (mode === "bottom" ? "lowest" : "highest") + " finite values)."
        : "Showing all " + items.length + ".") + (reversed ? " Display order reversed." : "");
    return { horizontal, items: displayed };
}

function setupMetricBarControls() {
    const charts = [["node-chart", drawNodeChart], ["edge-chart", drawEdgeChart],
        ["comm-chart", drawCommChart], ["attrcomm-chart", drawAttributeCommChart], ["edgeattr-chart", drawEdgeAttributeChart],
        ["ep-chart", drawEdgePartitionChart]];
    charts.forEach(([id, redraw]) => {
        const orientation = $(id + "-orientation"), show = $(id + "-show"), k = $(id + "-k");
        const sync = () => {
            const ranked = show.value === "top" || show.value === "bottom";
            k.closest("label").hidden = !ranked;
            $(id + "-sort").disabled = ranked;
        };
        orientation.addEventListener("change", redraw);
        $(id + "-order").addEventListener("change", redraw);
        show.addEventListener("change", () => { sync(); redraw(); });
        k.addEventListener("input", () => { if (k.validity.valid && k.value) redraw(); });
        k.addEventListener("change", () => {
            k.value = String(Math.max(1, Math.floor(Number(k.value) || 10)));
            redraw();
        });
        sync();
    });
}

function drawBarChart(canvasId, tipId, items, title, xLabel, yLabel, showAxisNames = true) {
    const view = metricBarView(canvasId, items);
    items = view.items;
    const horizontal = view.horizontal;
    const colors = echartColors();
    const option = chartBase(colors, showsMetricChartTitle(canvasId) ? title + "  (n=" + items.length + ")" : "");
    const categoryLabelWidth = horizontal ? Math.min(160, items.reduce((width, item) =>
        Math.max(width, String(item.label).length * colors.labelSize * 0.65), 40)) : undefined;
    option.grid = { left: horizontal ? 84 : yLabel ? 82 : 58, right: horizontal && items.length > 40 ? 56 : colors.chartStyle === "editorial" ? 30 : 18,
        top: colors.chartStyle === "editorial" ? 54 : 44, bottom: horizontal ? 64 : xLabel ? 82 : 48, containLabel: true };
    option.tooltip = { ...option.tooltip, trigger: "axis", formatter: (params) => {
        const p = Array.isArray(params) ? params[0] : params;
        const item = p && items[p.dataIndex];
        if (horizontal) return item ? escapeChartHtml(yLabel || "value") + ": <b>" + escapeChartHtml(fmt(item.value))
            + "</b><br>" + escapeChartHtml(xLabel || "category") + ": " + escapeChartHtml(item.label) : "";
        return item ? escapeChartHtml(xLabel || "x") + ": " + escapeChartHtml(item.label)
            + "<br>" + escapeChartHtml(yLabel || "value") + ": <b>" + escapeChartHtml(fmt(item.value)) + "</b>" : "";
    } };
    const categoryAxis = { type: "category", name: showAxisNames ? xLabel : "", nameLocation: "middle", nameGap: horizontal ? categoryLabelWidth + 28 : 58,
        inverse: horizontal,
        nameTextStyle: { color: colors.text, fontSize: colors.fontSize + 1, fontWeight: 600 }, data: items.map((d) => String(d.label)),
        axisLabel: { color: colors.muted, fontSize: colors.labelSize, hideOverlap: true, interval: items.length > 40 ? "auto" : 0,
            rotate: !horizontal && items.length > 8 ? 28 : 0, width: categoryLabelWidth, overflow: horizontal ? "truncate" : undefined },
        axisLine: { lineStyle: { color: colors.border } }, axisTick: { show: false },
        splitLine: { show: false } };
    const valueAxis = { type: "value", name: showAxisNames ? yLabel : "", nameLocation: "middle", nameGap: horizontal ? 40 : yLabel ? 58 : 44,
        nameTextStyle: { color: colors.text, fontSize: colors.fontSize + 1, fontWeight: 600 },
        axisLabel: { color: colors.muted, fontSize: colors.labelSize, formatter: (v) => fmt(v) },
        axisLine: { show: true, lineStyle: { color: colors.border } }, axisTick: { show: true, inside: false, length: 6, lineStyle: { color: colors.border } },
        splitLine: { lineStyle: { color: colors.border, opacity: colors.gridOpacity } }, scale: false };
    if (horizontal) categoryAxis.axisTick = { show: true, inside: false, length: 6, lineStyle: { color: colors.border } };
    option.xAxis = horizontal ? valueAxis : categoryAxis;
    option.yAxis = horizontal ? categoryAxis : valueAxis;
    const zoomAxis = horizontal ? { yAxisIndex: 0 } : { xAxisIndex: 0 };
    option.dataZoom = [
        { type: "inside", ...zoomAxis, filterMode: "none" },
        ...(items.length > 40 ? [
        { type: "slider", ...zoomAxis, ...(horizontal ? { orient: "vertical", width: 16, right: 8, top: 44, bottom: 64 }
            : { height: 16, bottom: 8 }), borderColor: colors.border,
            textStyle: { color: colors.muted }, fillerColor: colors.accent + "33", handleStyle: { color: colors.accent } },
        ] : []),
    ];
    const histogram = canvasId === "pair-chart";
    const barColor = colors.chartStyle === "glass-panel" || histogram
        ? new echarts.graphic.LinearGradient(0, 0, horizontal ? 1 : 0, horizontal ? 0 : 1, [{ offset: 0, color: colors.accent }, { offset: 1, color: colors.accent + "55" }])
        : colors.accent;
    option.series = [{ type: "bar", data: items.map(d => d.color ? { value: d.value, itemStyle: { color: d.color } } : d.value), barMaxWidth: histogram ? 48 : colors.chartStyle === "editorial" ? 30 : 38,
        barMinHeight: histogram ? 3 : 0, barCategoryGap: histogram ? "12%" : "20%",
        itemStyle: { color: barColor, borderRadius: horizontal
            ? [colors.barRadius[3], colors.barRadius[0], colors.barRadius[1], colors.barRadius[2]] : colors.barRadius,
            opacity: histogram ? 0.96 : 1, borderColor: histogram ? colors.text : undefined, borderWidth: histogram ? 0.5 : 0,
            shadowBlur: colors.chartStyle === "glass-panel" ? 14 : 0, shadowColor: colors.accent + "55" },
        emphasis: { focus: "none", itemStyle: { color: colors.accent, opacity: 1 } } }];
    showEChart(canvasId, option, title);
}


/* ------------------------------ scatterplots ------------------------------ */

function drawNodeScatter() {
    const metric = $("node-scatter-metric").value, recKey = $("node-scatter-rec").value;
    const orig = state.metricData[metric], rmap = state.recMetricData.vertex?.[metric]?.[recKey];
    if (!metric || !recKey || !orig || !rmap) { clearChart("node-scatter"); return; }
    const points = Object.keys(orig).filter((n) => rmap[n] !== undefined).map((n) => ({ label: n, x: orig[n], y: rmap[n] }));
    drawScatter("node-scatter", "node-scatter-tip", points, metric + " — original vs " + recLabel(recKey), "original", "recommendation");
}

function drawEdgeScatter() {
    const metric = $("edge-scatter-metric").value, recKey = $("edge-scatter-rec").value;
    const orig = state.pairData[metric], rmap = state.recMetricData.pair?.[metric]?.[recKey];
    if (!metric || !recKey || !orig || !rmap) { clearChart("edge-scatter"); return; }
    const points = Object.keys(orig).filter((k) => rmap[k] !== undefined)
        .map((k) => ({ label: k.replace("|", "→"), x: orig[k], y: rmap[k] }));
    drawScatter("edge-scatter", "edge-scatter-tip", points, metric + " — original vs " + recLabel(recKey), "original", "recommendation");
}

function drawCommScatter() {
    const metric = $("comm-scatter-metric").value, recKey = $("comm-scatter-rec").value;
    const entry = state.commMetricData[metric], rmap = state.recMetricData.comm?.[metric]?.[recKey];
    if (!metric || !recKey || !entry || !rmap) { clearChart("comm-scatter"); return; }
    const orig = entry.values;
    const points = Object.keys(orig).filter((c) => rmap[c] !== undefined)
        .map((c) => ({ label: "community " + c, x: orig[c], y: rmap[c] }));
    drawScatter("comm-scatter", "comm-scatter-tip", points, metric + " — original vs " + recLabel(recKey), "original", "recommendation");
}

// Draws a scatter of (original, recommendation) values with a y=x reference line; shares the chart palette.
function drawScatter(canvasId, tipId, points, title, xLabel, yLabel) {
    const colors = echartColors();
    let lo = Infinity, hi = -Infinity;
    for (const p of points) { lo = Math.min(lo, p.x, p.y); hi = Math.max(hi, p.x, p.y); }
    if (!Number.isFinite(lo) || !Number.isFinite(hi)) { clearChart(canvasId); return; }
    if (hi === lo) { hi += 0.5; lo -= 0.5; }
    const option = chartBase(colors, showsMetricChartTitle(canvasId) ? title + "  (n=" + points.length + ")" : "");
    option.tooltip = { ...option.tooltip, trigger: "item", formatter: (p) => {
        const d = p.data;
        return escapeChartHtml(d[2]) + "<br>" + escapeChartHtml(xLabel) + ": " + escapeChartHtml(fmt(d[0]))
            + "<br>" + escapeChartHtml(yLabel) + ": " + escapeChartHtml(fmt(d[1]));
    } };
    option.grid = { left: 82, right: 24, top: colors.chartStyle === "editorial" ? 54 : 44, bottom: 66, containLabel: true };
    const axis = (name) => ({ type: "value", name, min: lo, max: hi, scale: false,
        nameTextStyle: { color: colors.text, fontSize: colors.fontSize + 1, fontWeight: 600 },
        axisLabel: { color: colors.muted, fontSize: colors.labelSize, formatter: (v) => fmt(v) }, axisLine: { lineStyle: { color: colors.border } },
        axisTick: { show: true, inside: false, length: 6, lineStyle: { color: colors.border } },
        splitLine: { lineStyle: { color: colors.border, opacity: colors.gridOpacity } } });
    option.xAxis = axis(xLabel);
    option.yAxis = axis(yLabel);
    option.dataZoom = [{ type: "inside", xAxisIndex: 0, yAxisIndex: 0, filterMode: "none" }];
    const scatterSize = colors.chartStyle === "vibrant-dashboard" || colors.chartStyle === "glass-panel" ? 9 : colors.chartStyle === "editorial" ? 6 : 7;
    option.series = [{ type: "scatter", symbolSize: scatterSize, data: points.map((p) => [p.x, p.y, String(p.label)]),
        itemStyle: { color: colors.accent, opacity: 0.74,
            shadowBlur: colors.chartStyle === "glass-panel" ? 8 : 0, shadowColor: colors.accent + "88" }, emphasis: { focus: "self", scale: 1.5,
            itemStyle: { opacity: 1, borderColor: colors.text, borderWidth: 1 } },
        markLine: { silent: true, symbol: "none", lineStyle: { color: colors.muted, type: "dashed", opacity: 0.8 },
            data: [[{ coord: [lo, lo] }, { coord: [hi, hi] }]] } }];
    showEChart(canvasId, option, title);
}

