"use strict";

function metricResultSources(entry) {
    if (!entry) return [];
    const original = { id: "original", name: "Original", map: entry.values, labels: entry.labels, values: Object.values(entry.values), aggregate: entry.aggregate, colorIndex: 0 };
    const models = Object.keys(state.rec?.models || {}), recommendations = metricAnalysisRecommendations(entry);
    return [original, ...Object.entries(recommendations).map(([key, value], i) => {
        const labels = { ...entry.labels };
        if (!entry.aggregate) for (const id of Object.keys(value)) {
            if (!Object.hasOwn(entry.values, id) && entry.pairKeys && id.startsWith("recommendation-edge:")) labels[id] = JSON.parse(id.slice("recommendation-edge:".length)).join("→");
            else if (!Object.hasOwn(entry.values, id) && id.startsWith("attribute-value:")) labels[id] = JSON.parse(id.slice("attribute-value:".length));
        }
        return { id: "rec:" + key, key, name: recLabel(key), labels,
            map: entry.aggregate ? {} : value, values: entry.aggregate ? [] : Object.values(value), aggregate: entry.aggregate ? value : null,
            colorIndex: 1 + (models.includes(key) ? models.indexOf(key) : models.length + i) };
    })];
}

function metricWorkspaceSourcePicker(container, view, key, label, sources, initial = ["original"]) {
    if (!Array.isArray(view[key])) view[key] = initial.slice();
    const fieldset = metricWorkspaceElement("fieldset", "metric-series-picker");
    fieldset.appendChild(metricWorkspaceElement("legend", "", label));
    sources.forEach((source, i) => {
        const choice = metricWorkspaceElement("label", "metric-series-choice"), input = metricWorkspaceElement("input");
        input.type = "checkbox"; input.checked = view[key].includes(source.id); input.disabled = !!source.disabled;
        input.dataset.seriesFocus = key + "-" + i;
        input.addEventListener("change", () => {
            const selected = new Set(view[key]);
            if (input.checked) selected.add(source.id); else selected.delete(source.id);
            view[key] = [...selected]; refreshMetricWorkspace();
        });
        choice.append(input, metricWorkspaceElement("span", "", source.name + (source.disabled ? " (compute both metrics first)" : "")));
        fieldset.appendChild(choice);
    });
    container.appendChild(fieldset);
    return sources.filter(source => !source.disabled && view[key].includes(source.id));
}

function metricAnalysisPairValueBins(sources, minimumSize) {
    const all = sources.flatMap(source => source.aggregate.valueCounts || []).filter(item => Number.isFinite(item.value) && Number.isFinite(item.count) && item.count > 0);
    if (!all.length) return { bins: sources.map(() => []), width: null, minimum: minimumSize };
    let lo = Infinity, hi = -Infinity;
    for (const item of all) { lo = Math.min(lo, item.value); hi = Math.max(hi, item.value); }
    const scale = Math.max(Math.abs(lo), Math.abs(hi), Number(minimumSize) || 1, 1);
    const spanScaled = hi / scale - lo / scale;
    let width = Number(minimumSize);
    if (!Number.isFinite(width) || width <= 0) width = metricAnalysisBinSize([all.map(item => item.value)]);
    width = Math.max(width, scale / 10000 * spanScaled);
    const bounds = metricAnalysisHistogram([[lo, hi]], width).bounds;
    const bins = sources.map(source => Array.from({ length: bounds.length - 1 }, (_, i) => ({ x0: bounds[i], x1: bounds[i + 1], count: 0 })));
    sources.forEach((source, i) => (source.aggregate.valueCounts || []).forEach(item => {
        if (!Number.isFinite(item.value) || !Number.isFinite(item.count) || item.count <= 0) return;
        let start = 0, end = bounds.length - 1;
        while (start < end) { const middle = Math.floor((start + end + 1) / 2); if (bounds[middle] <= item.value) start = middle; else end = middle - 1; }
        bins[i][Math.min(bins[i].length - 1, start)].count += item.count;
    }));
    return { bins, width: bounds.length > 1 ? bounds[1] - bounds[0] : width, minimum: Number(minimumSize) };
}

function metricAnalysisAggregateDistribution(entry, type, binSize, sources, marks) {
    if (!["histogram", "cumulative", "frequency"].includes(type)) throw new RangeError("Stored pair results only support histograms and distribution line charts.");
    const colors = echartColors(), option = chartBase(colors, ""), title = entry.label + " — " + ({ histogram: "histogram", cumulative: "cumulative distribution", frequency: "exact value frequency" })[type];
    const notes = [], rows = [["series", "from", "to", "count", "estimated"]];
    const exactCounts = sources.length > 0 && sources.every(source => Array.isArray(source.aggregate?.valueCounts));
    const firstBin = sources[0]?.aggregate?.histogram?.[0];
    const requestedMinimum = binSize ?? (firstBin ? firstBin.x1 - firstBin.x0 : undefined);
    if (type === "frequency" && !exactCounts) throw new RangeError("Exact value counts require recomputing this older pair result.");
    const exactBins = exactCounts && type === "histogram" ? metricAnalysisPairValueBins(sources, requestedMinimum) : null;
    const prepared = sources.map(source => {
        const aggregate = source.aggregate;
        let bins = (aggregate?.histogram || []).filter(bin => Number.isFinite(bin.x0) && Number.isFinite(bin.x1) && Number.isFinite(bin.count) && bin.count >= 0);
        if (type === "histogram" && exactBins) bins = exactBins.bins[sources.indexOf(source)];
        else if (type === "histogram" && binSize != null) bins = metricAnalysisRebin(bins, binSize);
        notes.push(source.name + ": " + (aggregate?.estimated ? "estimated from " + aggregate.evaluated.toLocaleString() + " sampled pairs" : "exact over " + (aggregate?.totalPairs || 0).toLocaleString() + " pairs")
            + "; " + (aggregate?.infinite || 0).toLocaleString() + " non-finite excluded");
        bins.forEach(bin => rows.push([source.name, bin.x0, bin.x1, bin.count, !!aggregate?.estimated]));
        const valueCounts = (aggregate?.valueCounts || []).filter(item => Number.isFinite(item.value) && Number.isFinite(item.count) && item.count > 0).sort((a, b) => a.value - b.value);
        return { ...source, bins, valueCounts };
    });
    option.xAxis = metricAnalysisAxis(entry.label, colors); option.yAxis = metricAnalysisAxis(type === "histogram" || type === "frequency" ? "Number of values" : "Cumulative share (%)", colors);
    option.legend = { data: sources.map(source => source.name), selectedMode: false, textStyle: { color: colors.text } };
    option.dataZoom = [{ type: "inside", xAxisIndex: 0, filterMode: "none" }];
    if (type === "histogram") {
        const intervalKey = bin => JSON.stringify([bin.x0, bin.x1]);
        const intervals = [...new Map(prepared.flatMap(source => source.bins.map(bin => [intervalKey(bin), bin]))).values()]
            .sort((a, b) => a.x0 - b.x0 || a.x1 - b.x1);
        const labels = intervals.map(bin => fmt(bin.x0) + "–" + fmt(bin.x1));
        option.xAxis = metricAnalysisHistogramAxis(entry.label + " (value intervals)", labels, colors);
        option.yAxis.min = 0;
        option.series = prepared.map(source => {
            const counts = new Map(source.bins.map(bin => [intervalKey(bin), bin.count]));
            return { name: source.name, type: "bar", barCategoryGap: "12%", barGap: "15%",
                itemStyle: { color: colors.palette[source.colorIndex % colors.palette.length] }, emphasis: { focus: "series" },
                data: intervals.map(bin => counts.get(intervalKey(bin)) ?? null) };
        });
        option.tooltip = { ...option.tooltip, trigger: "axis", formatter: params => {
            const items = Array.isArray(params) ? params : [params];
            return escapeChartHtml(labels[items[0]?.dataIndex] || "") + "<br>" + items.filter(item => Number.isFinite(item.value))
                .map(item => escapeChartHtml(item.seriesName) + ": " + escapeChartHtml(String(item.value))).join("<br>");
        } };
        notes.push(exactCounts ? "Minimum bin size " + fmt(exactBins.minimum) + "; actual bin width " + fmt(exactBins.width) + "."
            : "Stored bin boundaries are retained; intervals with different boundaries remain separate groups.");
        notes.push("Bars are grouped by result within each interval; vertical lines and ticks mark interval boundaries.");
    } else {
        option.yAxis.min = 0; if (type === "cumulative") option.yAxis.max = 100;
        option.series = prepared.map((source, i) => {
            const total = source.bins.reduce((sum, bin) => sum + bin.count, 0); let accumulated = 0;
            const data = exactCounts
                ? (type === "frequency" ? source.valueCounts.map(item => [item.value, item.count])
                    : [...(source.valueCounts.length ? [[source.valueCounts[0].value, 0]] : []), ...source.valueCounts.map(item => { accumulated += item.count; return [item.value, total ? accumulated / total * 100 : 0]; })])
                : source.bins.map(bin => { accumulated += bin.count; return [bin.x1, total ? accumulated / total * 100 : 0]; });
            return { name: source.name, type: "line", step: "end", data, showSymbol: marks !== "lines", showAllSymbol: marks !== "lines", symbolSize: 6,
                lineStyle: { width: marks === "points" ? 0 : 2, type: i % 2 ? "dashed" : "solid" }, emphasis: { lineStyle: { width: marks === "points" ? 0 : 2 } },
                itemStyle: { color: colors.palette[source.colorIndex % colors.palette.length] } };
        });
        option.tooltip = { ...option.tooltip, trigger: "axis", axisPointer: { type: "line" }, valueFormatter: value => fmt(value) + (type === "cumulative" ? "%" : "") };
        if (type === "frequency" && exactCounts) {
            rows.length = 0; rows.push(["series", "value", "count", "estimated"]);
            prepared.forEach(source => source.valueCounts.forEach(item => rows.push([source.name, item.value, item.count, !!source.aggregate.estimated])));
            notes.push("Exact counts for each distinct finite value; no binning.");
        } else if (type === "cumulative") {
            if (exactCounts) {
                rows.length = 0; rows.push(["series", "value", "cumulative share (%)", "estimated"]);
                prepared.forEach(source => {
                    const total = source.valueCounts.reduce((sum, item) => sum + item.count, 0); let accumulated = 0;
                    source.valueCounts.forEach(item => { accumulated += item.count; rows.push([source.name, item.value, total ? 100 * accumulated / total : 0, !!source.aggregate.estimated]); });
                });
            }
            notes.push(exactCounts ? "Exact cumulative curve from distinct-value counts." : "Approximate cumulative curves from stored bins; within-bin values are unavailable.");
        }
    }
    return { option, title, rows, empty: !prepared.some(source => source.bins.some(bin => bin.count > 0)), note: notes.join(" · ") };
}

function metricAnalysisComparisonSeries(entry, other, mode, reference, targets, type = "scatter") {
    if (!targets.length) return { empty: true, note: "Select at least one comparison result.", rows: [] };
    const colors = echartColors(), metricVsMetric = mode === "metric";
    if (metricVsMetric && (!other || entry.scope !== other.scope)) return { empty: true, note: "Choose metrics from the same partition or attribute.", rows: [] };
    const comparisons = targets.map(target => ({ ...target, ...metricAnalysisMatch(target.x, target.y, { ...entry.labels, ...target.labels }) }));
    const rows = [["series", "entity", "X: " + (metricVsMetric ? entry.label : reference.name + " · " + entry.label),
        "Y: " + (metricVsMetric ? other.label : entry.label), ...(!metricVsMetric ? ["change (target - reference)"] : [])],
        ...comparisons.flatMap(target => target.points.map(point => [target.name, point.label, point.x, point.y, ...(!metricVsMetric ? [point.y - point.x] : [])]))];
    const counts = comparisons.map(target => target.name + ": " + target.points.length.toLocaleString() + " matched; " + target.excluded.toLocaleString() + " excluded").join(" · ");
    const title = metricVsMetric ? entry.label + " vs " + other.label : entry.label + " — " + reference.name + " vs recommendations";
    if (!metricVsMetric && type === "difference") {
        const sources = comparisons.map(target => ({ ...target, map: Object.fromEntries(target.points.map(point => [point.id, point.y - point.x])),
            values: target.points.map(point => point.y - point.x) }));
        const plot = metricAnalysisDistribution({ ...entry, label: "Change in " + entry.label }, "histogram", null, null, "lines", sources);
        return { ...plot, rows, title: entry.label + " — change from " + reference.name, note: counts + ". Change = target − " + reference.name + ". " + plot.note };
    }
    const option = chartBase(colors, ""), points = comparisons.flatMap(target => target.points);
    const xName = metricVsMetric ? entry.label : reference.name + ": " + entry.label;
    const yName = metricVsMetric ? other.label : "Comparison: " + entry.label;
    const xDomain = metricAnalysisDomain(points.map(point => point.x), 0.04);
    const commonDomain = metricAnalysisDomain(points.flatMap(point => [point.x, point.y]), 0.04);
    const yDomain = metricVsMetric ? metricAnalysisDomain(points.map(point => point.y), 0.04) : commonDomain;
    option.xAxis = { ...metricAnalysisAxis(xName, colors), min: metricVsMetric ? xDomain[0] : commonDomain[0], max: metricVsMetric ? xDomain[1] : commonDomain[1] };
    option.yAxis = { ...metricAnalysisAxis(yName, colors), min: yDomain[0], max: yDomain[1] };
    option.legend = { data: targets.map(target => target.name), selectedMode: false, textStyle: { color: colors.text } };
    option.dataZoom = [{ type: "inside", xAxisIndex: 0, yAxisIndex: 0, filterMode: "none" }];
    option.series = comparisons.map((target, i) => ({ name: target.name, type: "scatter", symbol: ["circle", "triangle", "rect", "diamond"][i % 4], symbolSize: 7, progressive: 2000,
        data: target.points.map(point => [point.x, point.y, String(point.label)]), itemStyle: { color: colors.palette[target.colorIndex % colors.palette.length], opacity: 0.72 },
        ...(!metricVsMetric && i === 0 ? { markLine: { silent: true, symbol: "none", lineStyle: { color: colors.muted, type: "dashed" },
            data: [[{ coord: [commonDomain[0], commonDomain[0]] }, { coord: [commonDomain[1], commonDomain[1]] }]] } } : {}) }));
    option.tooltip = { ...option.tooltip, trigger: "item", formatter: item => escapeChartHtml(item.seriesName) + "<br>" + escapeChartHtml(item.data[2])
        + "<br>" + escapeChartHtml(xName) + ": " + escapeChartHtml(fmt(item.data[0])) + "<br>" + escapeChartHtml(yName) + ": " + escapeChartHtml(fmt(item.data[1])) };
    return { option, title, rows, empty: !points.length, note: counts + (metricVsMetric ? ". Each axis uses its metric scale." : ". Reference: " + reference.name + "; equal axis scales; dashed line = no change.") };
}

// Log axes change only the chart option. Export rows and source metric maps stay raw.
function metricAnalysisApplyLogAxes(result, xScale, yScale) {
    if (!result?.option || (xScale !== "log" && yScale !== "log")) return result;
    const option = { ...result.option, xAxis: { ...result.option.xAxis }, yAxis: { ...result.option.yAxis },
        series: (result.option.series || []).map(series => ({ ...series, data: Array.isArray(series.data) ? series.data.slice() : series.data,
            markLine: series.markLine ? JSON.parse(JSON.stringify(series.markLine)) : series.markLine })) };
    const axes = [option.xAxis, option.yAxis];
    if (xScale === "log") { axes[0].type = "log"; axes[0].logBase = 10; }
    if (yScale === "log") { axes[1].type = "log"; axes[1].logBase = 10; }
    let omitted = 0, plotted = 0, hadEqualityLine = false;
    for (const series of option.series || []) {
        if (!["line", "scatter"].includes(series.type) || !Array.isArray(series.data)) continue;
        const before = series.data.length;
        series.data = series.data.filter(point => Array.isArray(point)
            && (xScale !== "log" || Number.isFinite(point[0]) && point[0] > 0)
            && (yScale !== "log" || Number.isFinite(point[1]) && point[1] > 0));
        omitted += before - series.data.length; plotted += series.data.length;
        if (series.markLine) { hadEqualityLine = true; if (xScale !== yScale) delete series.markLine; }
    }
    const notes = [];
    if (omitted) notes.push(omitted.toLocaleString() + " point" + (omitted === 1 ? "" : "s") + " hidden because log axes require positive values");
    if (plotted === 0) notes.push("No points have positive values on every logarithmic axis");
    if (hadEqualityLine && xScale !== yScale) {
        notes.push("The equality reference line is hidden when only one axis is logarithmic");
    }
    const xValues = [], yValues = [];
    for (const series of option.series || []) if (["line", "scatter"].includes(series.type)) for (const point of series.data || []) {
        if (point[0] > 0) xValues.push(point[0]); if (point[1] > 0) yValues.push(point[1]);
    }
    const setLogDomain = (axis, values) => {
        if (!values.length) return;
        let min = Infinity, max = -Infinity;
        for (const value of values) { min = Math.min(min, value); max = Math.max(max, value); }
        axis.min = min === max ? min / Math.sqrt(10) : min / 1.1;
        axis.max = min === max ? max * Math.sqrt(10) : max * 1.1;
    };
    if (xScale === "log") setLogDomain(option.xAxis, xValues);
    if (yScale === "log") setLogDomain(option.yAxis, yValues);
    if (xScale === "log" && yScale === "log" && option.series?.some(series => series.type === "scatter")) {
        const values = [...xValues, ...yValues]; setLogDomain(option.xAxis, values); setLogDomain(option.yAxis, values);
        const low = option.xAxis.min, high = option.xAxis.max;
        for (const series of option.series || []) if (series.markLine) series.markLine.data = [[{ coord: [low, low] }, { coord: [high, high] }]];
    }
    return { ...result, option, empty: result.empty || plotted === 0, note: [result.note, ...notes].filter(Boolean).join(" · ") };
}

function drawMetricWorkspaceRanking(family) {
    const view = METRIC_WORKSPACE_VIEWS.get(family), config = METRIC_WORKSPACE_FAMILIES[family];
    if (!view || !config?.draw) return;
    const entries = metricAnalysisEntries(family), selected = $(config.prefix + "-chart-metric").value;
    const entry = entries.find(item => item.key === selected), id = config.prefix + "-chart";
    view.metric = selected;
    const source = metricResultSources(entry).find(item => item.id === (view.rankingSource || "original"));
    if (!source) { clearChart(id); $(id + "-view-note").textContent = "Compute this metric for the selected recommendation first."; return; }
    const sortKey = $(id + "-sort").value, sortEntry = entries.find(item => item.key === sortKey && item.scope === entry.scope);
    const sortedSource = sortEntry ? metricResultSources(sortEntry).find(item => item.id === source.id) : null;
    let ids = Object.keys(source.map);
    if (sortEntry) ids.sort((a, b) => {
        const x = sortedSource?.map[a], y = sortedSource?.map[b];
        return (Number.isFinite(y) ? y : -Infinity) - (Number.isFinite(x) ? x : -Infinity) || String(a).localeCompare(String(b), undefined, { numeric: true });
    });
    else ids.sort((a, b) => String(source.labels[a] ?? a).localeCompare(String(source.labels[b] ?? b), undefined, { numeric: true }));
    const items = ids.map(key => ({ label: metricAnalysisLabel(source.labels, key), value: source.map[key] }));
    drawBarChart(id, "", items, entry.label + " — " + source.name, config.noun, entry.label);
    $(id + "-view-note").textContent += " Result: " + source.name + ".";
}
