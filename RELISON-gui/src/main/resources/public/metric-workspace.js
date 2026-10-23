"use strict";

// A family shares one entity domain across its views. Group IDs are meaningful only
// within their partition/attribute; never join them across those scopes.
const METRIC_WORKSPACE_FAMILIES = {
    nodes: { prefix: "node", noun: "nodes", draw: () => drawNodeChart() },
    edges: { prefix: "edge", noun: "edges", draw: () => drawEdgeChart() },
    pairs: { prefix: "pair", noun: "node pairs" },
    comm: { prefix: "comm", noun: "communities", draw: () => drawCommChart() },
    attrcomm: { prefix: "attrcomm", noun: "attribute groups", draw: () => drawAttributeCommChart() },
    edgeattr: { prefix: "edgeattr", noun: "edge attribute groups", draw: () => drawEdgeAttributeChart() },
    edgepartitions: { prefix: "ep", noun: "edge groups", draw: () => drawEdgePartitionChart() },
};
const METRIC_WORKSPACE_VIEWS = new Map();
let metricWorkspaceComputeOpen = true;
let metricWorkspaceReady = false;

function metricAnalysisQuantile(sorted, probability) {
    if (!sorted.length) return NaN;
    const index = (sorted.length - 1) * probability, lo = Math.floor(index), fraction = index - lo;
    return sorted[lo] * (1 - fraction) + sorted[Math.ceil(index)] * fraction;
}

function metricAnalysisSummary(values) {
    const sorted = values.filter(Number.isFinite).sort((a, b) => a - b);
    let mean = 0;
    // Weighted updates avoid overflowing the sum for otherwise finite observations.
    sorted.forEach((value, i) => { mean = mean * (i / (i + 1)) + value / (i + 1); });
    return { sorted, count: sorted.length, excluded: values.length - sorted.length,
        mean: sorted.length ? mean : NaN, min: sorted[0], max: sorted.at(-1),
        q1: metricAnalysisQuantile(sorted, 0.25), median: metricAnalysisQuantile(sorted, 0.5),
        q3: metricAnalysisQuantile(sorted, 0.75) };
}

function metricAnalysisAggregateSummary(aggregate) {
    const counts = (aggregate?.valueCounts || []).filter(item => Number.isFinite(item.value) && Number.isFinite(item.count) && item.count > 0).slice().sort((a, b) => a.value - b.value);
    const total = counts.reduce((sum, item) => sum + item.count, 0);
    const valueAt = rank => {
        let cumulative = 0;
        for (const item of counts) { cumulative += item.count; if (cumulative > rank) return item.value; }
        return counts.at(-1)?.value ?? NaN;
    };
    const quantile = probability => {
        if (!total) return NaN;
        const index = (total - 1) * probability, lo = Math.floor(index), fraction = index - lo;
        return valueAt(lo) * (1 - fraction) + valueAt(Math.ceil(index)) * fraction;
    };
    return { median: quantile(0.5), min: counts[0]?.value ?? aggregate?.min ?? NaN, max: counts.at(-1)?.value ?? aggregate?.max ?? NaN,
        count: aggregate?.finite ?? total, excluded: aggregate?.infinite ?? 0 };
}

function metricAnalysisDomain(values, padding = 0) {
    let lo = Infinity, hi = -Infinity;
    for (const value of values) if (Number.isFinite(value)) { lo = Math.min(lo, value); hi = Math.max(hi, value); }
    if (!Number.isFinite(lo)) return [0, 1];
    if (lo === hi) { const pad = Math.max(Math.abs(lo) * 0.05, 0.5); return [Math.max(-Number.MAX_VALUE, lo - pad), Math.min(Number.MAX_VALUE, hi + pad)]; }
    const pad = (hi / 2 - lo / 2) * (2 * padding);
    return [Math.max(-Number.MAX_VALUE, lo - pad), Math.min(Number.MAX_VALUE, hi + pad)];
}

function metricAnalysisBinSize(series) {
    const values = series.flatMap(items => items.filter(Number.isFinite));
    const [lo, hi] = metricAnalysisDomain(values), target = (hi / 20 - lo / 20) || 1;
    if (values.every(Number.isInteger)) return Math.max(1, Math.ceil(target));
    const magnitude = 10 ** Math.floor(Math.log10(target));
    return Math.max(Number.MIN_VALUE, Math.ceil(target / magnitude) * magnitude);
}

function metricAnalysisHistogram(series, requestedSize) {
    const finite = series.flatMap(values => values.filter(Number.isFinite));
    const size = requestedSize == null ? metricAnalysisBinSize(series) : Number(requestedSize);
    if (!Number.isFinite(size) || size <= 0) throw new RangeError("Bin size must be a positive finite number.");
    let lo = Infinity, hi = -Infinity;
    for (const value of finite) { lo = Math.min(lo, value); hi = Math.max(hi, value); }
    if (!finite.length) lo = hi = 0;
    const scale = Math.max(Math.abs(lo), Math.abs(hi), size, 1);
    const span = (hi / scale - lo / scale) / (size / scale);
    let count = Math.max(1, Math.ceil(span));
    if (!Number.isFinite(count) || count > 10000) throw new RangeError("This bin size produces more than 10,000 bins. Choose a larger bin size.");
    const bound = i => {
        const direct = lo + i * size;
        return Number.isFinite(direct) ? direct : Math.max(-Number.MAX_VALUE, Math.min(Number.MAX_VALUE, (lo / scale + i * (size / scale)) * scale));
    };
    if (count > 1 && bound(count - 1) >= hi) count--;
    const bounds = Array.from({ length: count + 1 }, (_, i) => bound(i));
    const counts = series.map(values => {
        const bins = Array(count).fill(0);
        for (const value of values) if (Number.isFinite(value)) {
            let start = 0, end = count;
            while (start < end) {
                const middle = Math.floor((start + end + 1) / 2);
                if (bounds[middle] <= value) start = middle; else end = middle - 1;
            }
            bins[Math.min(count - 1, start)]++;
        }
        return bins;
    });
    return { bounds, counts, size };
}

function metricAnalysisRebin(bins, size) {
    if (!bins.length) return [];
    const base = bins[0].x1 - bins[0].x0, multiple = Math.round(size / base);
    if (!Number.isFinite(size) || multiple < 1 || Math.abs(size / base - multiple) > 1e-7)
        throw new RangeError("Stored pair results require a bin size that is a whole multiple of " + fmt(base) + ". Recompute raw values to use smaller bins.");
    if (bins.some(bin => Math.abs((bin.x1 - bin.x0) / base - 1) > 1e-7))
        throw new RangeError("These stored bins have unequal widths and cannot be rebinned accurately.");
    const result = [];
    for (let i = 0; i < bins.length; i += multiple) {
        const group = bins.slice(i, i + multiple);
        result.push({ x0: group[0].x0, x1: group.at(-1).x1, count: group.reduce((sum, bin) => sum + bin.count, 0) });
    }
    return result;
}

function metricAnalysisFrequency(values) {
    const counts = new Map();
    for (const value of values) if (Number.isFinite(value)) counts.set(value, (counts.get(value) || 0) + 1);
    return [...counts].sort((a, b) => a[0] - b[0]);
}

function metricAnalysisCumulative(values) {
    const sorted = values.filter(Number.isFinite).sort((a, b) => a - b), points = [];
    for (let i = 0; i < sorted.length; i++) {
        if (i + 1 === sorted.length || sorted[i] !== sorted[i + 1]) points.push([sorted[i], 100 * (i + 1) / sorted.length]);
    }
    if (points.length) points.unshift([points[0][0], 0]);
    return points;
}

function metricAnalysisMatch(x, y, labels = {}) {
    const ids = new Set([...Object.keys(x), ...Object.keys(y)]), points = [];
    for (const id of ids) if (Number.isFinite(x[id]) && Number.isFinite(y[id]))
        points.push({ id, label: metricAnalysisLabel(labels, id), x: x[id], y: y[id] });
    return { points, excluded: ids.size - points.length };
}

function metricAnalysisLabel(labels, id) {
    return Object.hasOwn(labels, id) ? labels[id] ?? id : id;
}

function metricAnalysisEntries(family) {
    const wrap = (key, values, scope, labels = {}, recFamily = null, extra = {}) =>
        ({ key, label: key, values, scope, labels, recFamily, ...extra });
    if (family === "nodes") return state.metricOrder.map(key => wrap(key,
        Object.fromEntries((state.graph?.nodes() || []).map(id => [id, state.metricData[key]?.[id]])), "nodes", {}, "vertex"));
    if (family === "edges") return state.pairOrder.map(key => {
        const values = {}, labels = {}, pairKeys = {};
        for (const id of state.graph?.edges() || []) {
            const source = state.graph.source(id), target = state.graph.target(id), pair = pairKey(source, target);
            values[id] = state.pairData[key]?.[pair]; pairKeys[id] = pair;
            labels[id] = source + "→" + target + (state.multigraph ? " (" + id + ")" : "");
        }
        return wrap(key, values, "edges", labels, "pair", { pairKeys });
    });
    if (family === "pairs") return state.nodePairOrder.map(key => wrap(key, {}, "pairs", {}, "nodePair", { aggregate: state.nodePairAgg[key] }));
    if (family === "comm") return state.commMetricOrder
        .filter(key => !isAttributeCommunityPartition(state.commMetricData[key]?.algorithm)).map(key => {
            const entry = state.commMetricData[key];
            const labels = Object.fromEntries(Object.keys(entry.values).map(id => [id, communityLabel(id, entry.algorithm)]));
            return wrap(key, entry.values, "community:" + entry.algorithm, labels, "comm");
        });
    if (family === "attrcomm" || family === "edgeattr") {
        const node = family === "attrcomm", data = node ? state.attributeMetricData : state.edgeAttributeMetricData;
        return (node ? state.attributeMetricOrder : state.edgeAttributeMetricOrder).map(key => {
            const entry = data[key];
            return wrap(key, entry.values, family + ":" + entry.attribute, entry.valueLabels || {}, node ? "comm" : "edgeattr",
                { axisLabel: entry.axisLabel || metricTableDetails(key).metric });
        });
    }
    if (family === "edgepartitions") return edgePartitionChartEntries().map(({ key, data, metric }) =>
        wrap(key, Object.fromEntries(data.groups.map(group => [group.id, metric.values[group.id]])),
            "edge-partition:" + data.partitionId, Object.fromEntries(data.groups.map(group => [group.id, group.label])), "edgePartition",
            { label: metric.label + " · " + data.label }));
    return [];
}

function metricAnalysisRecommendations(entry) {
    if (!entry?.recFamily) return {};
    const result = {};
    for (const [key, values] of Object.entries(state.recMetricData[entry.recFamily]?.[entry.key] || {})) {
        if (entry.aggregate) { result[key] = values; continue; }
        const mapped = Object.fromEntries(Object.keys(entry.values).map(id => [id, values[entry.pairKeys?.[id] ?? id]]));
        if (entry.pairKeys) {
            const existing = new Set(Object.values(entry.pairKeys));
            for (const edge of state.rec?.models?.[key]?.edges || []) {
                const pair = pairKey(edge.source, edge.target);
                if (existing.has(pair) || !state.directed && existing.has(pairKey(edge.target, edge.source))) continue;
                const endpoints = state.directed ? [edge.source, edge.target] : [edge.source, edge.target].sort();
                mapped["recommendation-edge:" + JSON.stringify(endpoints)] = values[pair];
            }
        } else for (const id of Object.keys(values)) if (id.startsWith("attribute-value:")) mapped[id] = values[id];
        result[key] = mapped;
    }
    return result;
}

function metricAnalysisAxis(name, colors) {
    return { type: "value", name, nameLocation: "middle", nameGap: 42,
        nameTextStyle: { color: colors.text, fontSize: colors.fontSize },
        axisLabel: { color: colors.muted, fontSize: colors.labelSize, formatter: value => fmt(value) },
        axisLine: { show: true, lineStyle: { color: colors.border } },
        splitLine: { lineStyle: { color: colors.border, opacity: colors.gridOpacity } } };
}

function metricAnalysisHistogramAxis(name, labels, colors) {
    return { ...metricAnalysisAxis(name, colors), type: "category", data: labels, boundaryGap: true,
        axisLabel: { color: colors.muted, fontSize: colors.labelSize, hideOverlap: true },
        axisTick: { show: true, alignWithLabel: false, interval: 0, length: 6, lineStyle: { color: colors.border } },
        splitLine: { show: true, interval: 0, lineStyle: { color: colors.border, opacity: Math.max(0.5, colors.gridOpacity || 0) } } };
}

function metricAnalysisDistribution(entry, type, binSize, comparison, marks = "lines", selectedSources = null) {
    if (selectedSources && !selectedSources.length) return { empty: true, rows: [], note: "Select at least one result to plot." };
    if (selectedSources?.some(source => source.aggregate)) return metricAnalysisAggregateDistribution(entry, type, binSize, selectedSources, marks);
    const colors = echartColors(), title = entry.label + " — " + ({ histogram: "histogram", cumulative: "cumulative distribution", frequency: "exact value frequency", boxplot: "box plot" })[type];
    const option = chartBase(colors, ""), aggregate = entry.aggregate;
    const series = selectedSources || [{ name: entry.seriesName || "Original", values: Object.values(entry.values), map: entry.values }];
    if (comparison && !selectedSources) series.push(...(Array.isArray(comparison) ? comparison : [comparison]));
    const summaries = series.map(item => metricAnalysisSummary(item.values));
    const notes = aggregate ? [aggregate.estimated
        ? "Estimated from " + aggregate.evaluated.toLocaleString() + " sampled pairs of " + aggregate.totalPairs.toLocaleString() + "."
        : "Exact over " + aggregate.totalPairs.toLocaleString() + " pairs."]
        : summaries.map((summary, i) => series[i].name + ": " + summary.count.toLocaleString() + " valid, " + summary.excluded.toLocaleString() + " excluded");
    if (aggregate?.infinite) notes.push(aggregate.infinite.toLocaleString() + " non-finite pairs excluded.");
    option.xAxis = metricAnalysisAxis(entry.label, colors);
    option.yAxis = metricAnalysisAxis("Count", colors);
    option.dataZoom = [{ type: "inside", xAxisIndex: 0, filterMode: "none" }];
    if (series.length > 1) option.legend = { data: series.map(item => item.name), selectedMode: false, textStyle: { color: colors.text } };
    let validBins = aggregate?.histogram?.filter(bin => Number.isFinite(bin.x0) && Number.isFinite(bin.x1) && Number.isFinite(bin.count) && bin.count >= 0) || [];
    if (aggregate && type === "histogram" && binSize != null) validBins = metricAnalysisRebin(validBins, binSize);
    const empty = aggregate ? !validBins.some(bin => bin.count > 0) : !summaries.some(summary => summary.count);
    let rows;
    if (aggregate) rows = [["from", "to", "count", "estimated"], ...validBins.map(bin => [bin.x0, bin.x1, bin.count, !!aggregate.estimated])];
    else rows = [["series", "entity", "value"], ...series.flatMap(item => Object.entries(item.map || {}).map(([id, value]) => [item.name, metricAnalysisLabel(item.labels || entry.labels, id), Number.isFinite(value) ? value : ""]))];
    if (type === "histogram") {
        const histogram = aggregate ? null : metricAnalysisHistogram(series.map(item => item.values), binSize);
        const bounds = aggregate ? validBins.map(bin => [bin.x0, bin.x1]) : histogram.bounds.slice(0, -1).map((lo, i) => [lo, histogram.bounds[i + 1]]);
        const labels = bounds.map(([lo, hi]) => fmt(lo) + "–" + fmt(hi));
        option.xAxis = metricAnalysisHistogramAxis(entry.label + " (value intervals)", labels, colors);
        option.series = (aggregate ? [{ name: "Original" }] : series).map((item, i) => ({ name: item.name, type: "bar", barCategoryGap: "12%", barGap: "15%",
            data: aggregate ? validBins.map(bin => bin.count) : histogram.counts[i],
            itemStyle: { color: colors.palette[(item.colorIndex ?? i) % colors.palette.length] }, emphasis: { focus: "series" } }));
        option.tooltip = { ...option.tooltip, trigger: "axis", formatter: params => {
            const items = Array.isArray(params) ? params : [params];
            return escapeChartHtml(labels[items[0]?.dataIndex] || "") + "<br>" + items.map(item => escapeChartHtml(item.seriesName) + ": " + escapeChartHtml(String(item.value))).join("<br>");
        } };
        if (series.length > 1) notes.push("Shared bin boundaries; each series uses its full population.");
        notes.push("Bin size " + fmt(aggregate ? binSize ?? validBins[0]?.x1 - validBins[0]?.x0 : histogram.size) + "; final bin includes its upper endpoint.");
    } else if (type === "cumulative" || type === "frequency") {
        if (type === "frequency" && aggregate) throw new RangeError("Exact value frequencies require individual values; stored pair results contain bins only.");
        option.yAxis = { ...option.yAxis, name: type === "frequency" ? "Number of values" : "Cumulative share (%)", min: 0, ...(type === "cumulative" ? { max: 100 } : {}) };
        let curves;
        if (aggregate) {
            let accumulated = 0;
            const total = validBins.reduce((sum, bin) => sum + bin.count, 0);
            curves = [[...(validBins.length ? [[validBins[0].x0, 0]] : []), ...validBins.map(bin => { accumulated += bin.count; return [bin.x1, total ? 100 * accumulated / total : 0]; })]];
            notes.push("Approximate cumulative curve from existing bins; values within each bin are unavailable.");
        } else curves = series.map(item => type === "frequency" ? metricAnalysisFrequency(item.values) : metricAnalysisCumulative(item.values).slice(1));
        option.series = curves.map((data, i) => ({ name: series[i]?.name || "Original", type: "line", ...(type === "cumulative" ? { step: "end" } : {}),
            showSymbol: marks !== "lines", showAllSymbol: marks !== "lines", symbolSize: 6,
            data, lineStyle: { width: marks === "points" ? 0 : 2, type: i ? "dashed" : "solid" },
            emphasis: { lineStyle: { width: marks === "points" ? 0 : 2 } }, itemStyle: { color: colors.palette[(series[i].colorIndex ?? i) % colors.palette.length] } }));
        option.tooltip = { ...option.tooltip, trigger: "axis", axisPointer: { type: "line" }, valueFormatter: value => fmt(value) + (type === "cumulative" ? "%" : "") };
        if (type === "frequency") {
            rows = [["series", "value", "count"], ...curves.flatMap((data, i) => data.map(([value, count]) => [series[i].name, value, count]))];
            notes.push("Exact counts for each distinct finite value; no binning.");
        }
    } else {
        option.xAxis = { type: "category", data: series.map(item => item.name), name: "Result", axisLabel: { color: colors.muted } };
        option.yAxis = metricAnalysisAxis(entry.label, colors);
        option.series = [{ type: "boxplot", data: summaries.map(summary => summary.count
            ? [summary.min, summary.q1, summary.median, summary.q3, summary.max] : [null, null, null, null, null]),
            itemStyle: { color: colors.accent + "33", borderColor: colors.accent }, boxWidth: [24, 70] }];
        if (selectedSources) option.series[0].data = option.series[0].data.map((value, i) => {
            const color = colors.palette[(series[i].colorIndex ?? i) % colors.palette.length];
            return { value, itemStyle: { color: color + "33", borderColor: color } };
        });
        option.tooltip = { ...option.tooltip, trigger: "item", formatter: item => {
            const summary = summaries[item.dataIndex];
            return escapeChartHtml(series[item.dataIndex]?.name || "") + "<br>" +
                [["Min", summary.min], ["Q1", summary.q1], ["Median", summary.median], ["Q3", summary.q3], ["Max", summary.max]]
                    .map(([label, value]) => label + ": " + escapeChartHtml(fmt(value))).join("<br>");
        } };
        delete option.legend;
        notes.push("Box: 25th–75th percentiles; centre: median; whiskers: minimum and maximum.");
    }
    if (!aggregate && summaries[0].count) notes.push(series[0].name + " median " + fmt(summaries[0].median) + "; range " + fmt(summaries[0].min) + "–" + fmt(summaries[0].max) + ".");
    return { option, title, note: notes.join(" · "), rows, empty };
}

function metricAnalysisComparison(entry, other, recommendation, type) {
    const colors = echartColors(), originalVsRec = !!recommendation;
    if (!originalVsRec && (!other || entry.scope !== other.scope))
        return { empty: true, note: "Choose two metrics from the same partition or attribute.", rows: [] };
    const matched = metricAnalysisMatch(entry.values, recommendation?.map || other.values, entry.labels);
    const xName = originalVsRec ? "Original: " + entry.label : entry.label;
    const yName = originalVsRec ? recommendation.name + ": " + entry.label : other.label;
    const title = originalVsRec ? entry.label + " — original vs " + recommendation.name : entry.label + " vs " + other.label;
    const rows = [["entity", xName, yName, ...(originalVsRec ? ["change (recommendation - original)"] : [])],
        ...matched.points.map(point => [point.label, point.x, point.y, ...(originalVsRec ? [point.y - point.x] : [])])];
    const note = matched.points.length.toLocaleString() + " matched; " + matched.excluded.toLocaleString() + " missing or non-finite matches excluded.";
    if (originalVsRec && type === "difference") {
        const changes = Object.fromEntries(matched.points.map(point => [point.id, point.y - point.x]));
        const distribution = metricAnalysisDistribution({ ...entry, label: "Change in " + entry.label, seriesName: "Change", values: changes }, "histogram", null);
        return { ...distribution, rows, title: entry.label + " — change distribution", note: note + " Change = recommendation − original; positive means an increase. " + distribution.note };
    }
    const option = chartBase(colors, "");
    option.xAxis = metricAnalysisAxis(xName, colors); option.yAxis = metricAnalysisAxis(yName, colors);
    const xDomain = metricAnalysisDomain(matched.points.map(point => point.x), 0.04);
    const yDomain = originalVsRec ? metricAnalysisDomain(matched.points.flatMap(point => [point.x, point.y]), 0.04) : metricAnalysisDomain(matched.points.map(point => point.y), 0.04);
    option.xAxis.min = originalVsRec ? yDomain[0] : xDomain[0]; option.xAxis.max = originalVsRec ? yDomain[1] : xDomain[1];
    option.yAxis.min = yDomain[0]; option.yAxis.max = yDomain[1];
    option.dataZoom = [{ type: "inside", xAxisIndex: 0, yAxisIndex: 0, filterMode: "none" }];
    option.series = [{ type: "scatter", symbolSize: 7, progressive: 2000,
        data: matched.points.map(point => [point.x, point.y, String(point.label)]), itemStyle: { color: colors.accent, opacity: 0.7 },
        ...(originalVsRec ? { markLine: { silent: true, symbol: "none", lineStyle: { color: colors.muted, type: "dashed" },
            data: [[{ coord: [yDomain[0], yDomain[0]] }, { coord: [yDomain[1], yDomain[1]] }]] } } : {}) }];
    option.tooltip = { ...option.tooltip, trigger: "item", formatter: item =>
        escapeChartHtml(item.data[2]) + "<br>" + escapeChartHtml(xName) + ": " + escapeChartHtml(fmt(item.data[0])) + "<br>" + escapeChartHtml(yName) + ": " + escapeChartHtml(fmt(item.data[1])) };
    return { option, title, rows, empty: !matched.points.length, note: note + (originalVsRec ? " Dashed line: no change; axes share the same scale." : " Each axis uses its own metric scale.") };
}

function metricWorkspaceElement(tag, className, text) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (text != null) element.textContent = text;
    return element;
}

function metricWorkspaceAction(button, icon, label) {
    button.textContent = "";
    button.setAttribute("aria-label", label); button.type = "button";
    const symbol = metricWorkspaceElement("span", "material-symbols-outlined", icon);
    symbol.setAttribute("aria-hidden", "true");
    button.append(symbol, metricWorkspaceElement("span", "", label));
    button.classList.add("metric-export-action");
    return button;
}

function metricAnalysisGlobalRows(family) {
    const metadata = state.globalMetricScopes || {}, results = state.graphMetrics || {};
    return Object.keys(results).filter(label => {
        if (metadata[label]) return metadata[label].family === family;
        if (family === "attrcomm") return /\((?:attribute:|Attribute: )[\s\S]*\)$/.test(label);
        if (family === "edgeattr") return /^Number of attribute values \([\s\S]*\)$/.test(label);
        return family === "comm" && Object.keys(state.communityData || {}).some(partition =>
            !isAttributeCommunityPartition(partition) && label.endsWith(" (" + partition + ")"));
    }).map(label => ({ label, original: results[label], recommendations: state.recMetricData.graph?.[label] || {} }));
}

function metricWorkspaceGlobalSummary(view) {
    if (!view.globalTable) return;
    const rows = metricAnalysisGlobalRows(view.family), keys = [...new Set(rows.flatMap(row => Object.keys(row.recommendations)))];
    buildAverageTable(view.globalTable.id, rows.map(row => ({ label: row.label, original: row.original, rec: keys.map(key => row.recommendations[key]) })), keys);
    view.globalRows = [["Metric", "Original", ...keys.map(recLabel)], ...rows.map(row => [row.label, row.original, ...keys.map(key => row.recommendations[key])])];
    view.globalExport.disabled = !rows.length;
    view.globalNote.textContent = rows.length ? "" : "Run a global metric for this family using Compute metrics to populate this table.";
}

function syncMetricComputePanel() {
    const family = state.activeSubtab;
    for (const section of $("left-metrics").querySelectorAll("[data-metric-family]")) {
        section.hidden = !section.dataset.metricFamily.split(" ").includes(family);
    }
    $("pair-allpairs").checked = family === "pairs";
    $("pair-metric-heading").textContent = family === "pairs" ? "Node-pair metrics" : "Edge metrics";
    const toggle = $("metric-compute-toggle"); if (toggle) toggle.hidden = family === "relationships";
}

function setupMetricWorkspace() {
    const pane = $("pane-metrics"), oldNavigation = $("subtabbar");
    if (!pane || metricWorkspaceReady) return;
    const header = metricWorkspaceElement("header", "metric-workspace-header");
    oldNavigation.setAttribute("role", "tablist"); oldNavigation.setAttribute("aria-label", "Metric type");
    oldNavigation.querySelectorAll(".subtab").forEach(button => {
        button.id = "metric-family-" + button.dataset.subtab;
        button.setAttribute("role", "tab"); button.setAttribute("aria-controls", "subpane-" + button.dataset.subtab);
        const panel = $("subpane-" + button.dataset.subtab);
        panel.setAttribute("role", "tabpanel"); panel.setAttribute("aria-labelledby", button.id);
    });
    oldNavigation.addEventListener("keydown", event => {
        const buttons = [...oldNavigation.querySelectorAll("button")], index = buttons.indexOf(document.activeElement);
        const offset = ({ ArrowRight: 1, ArrowLeft: -1 })[event.key];
        if (index < 0 || (!offset && !["Home", "End"].includes(event.key))) return;
        event.preventDefault(); const next = event.key === "Home" ? 0 : event.key === "End" ? buttons.length - 1 : (index + offset + buttons.length) % buttons.length;
        buttons[next].click(); buttons[next].focus();
    });
    header.appendChild(oldNavigation);
    const compute = metricWorkspaceElement("button", "", "Compute metrics");
    compute.type = "button"; compute.id = "metric-compute-toggle";
    compute.setAttribute("aria-expanded", String(metricWorkspaceComputeOpen)); compute.setAttribute("aria-controls", "left-metrics");
    compute.addEventListener("click", () => {
        metricWorkspaceComputeOpen = !metricWorkspaceComputeOpen;
        compute.setAttribute("aria-expanded", String(metricWorkspaceComputeOpen));
        updatePanels(state.activeTab);
    });
    header.appendChild(compute); pane.prepend(header); oldNavigation.hidden = false;
    if (typeof setupMetricRecommendationBatch === "function") setupMetricRecommendationBatch(header);
    for (const family of ["global", ...Object.keys(METRIC_WORKSPACE_FAMILIES)]) {
        const host = $("subpane-" + family), config = METRIC_WORKSPACE_FAMILIES[family];
        const view = { family, mode: "summary", metric: "", y: "", type: "histogram", binSize: null, marks: "lines",
            compare: "metric", compareType: "scatter", rows: [] };
        METRIC_WORKSPACE_VIEWS.set(family, view);
        const content = Array.from(host.children), distributionIndex = content.findIndex(child => child.tagName === "H2" && /distribution/i.test(child.textContent));
        const workspace = metricWorkspaceElement("div", "metric-analysis-workspace");
        const rail = metricWorkspaceElement("nav", "metric-analysis-rail"); rail.setAttribute("aria-label", "Analysis view"); rail.setAttribute("role", "tablist"); rail.setAttribute("aria-orientation", "vertical");
        const stage = metricWorkspaceElement("div", "metric-analysis-stage");
        view.panels = {};
        for (const [key, title] of [["summary", "Summary"], ["rankings", "Rankings"], ["distributions", "Distributions"], ["compare", "Compare"]]) {
            const button = metricWorkspaceElement("button", "", title); button.type = "button";
            button.id = "metric-" + family + "-" + key + "-tab"; button.dataset.analysisView = key;
            button.setAttribute("role", "tab"); button.setAttribute("aria-controls", "metric-" + family + "-" + key);
            button.setAttribute("aria-selected", String(key === "summary")); button.tabIndex = key === "summary" ? 0 : -1;
            button.disabled = (family === "global" && key !== "summary") || (family === "pairs" && ["rankings", "compare"].includes(key));
            if (button.disabled) button.title = family === "global" ? "Global metrics contain one value per result." : "This view needs individual pair values; current results contain aggregate bins.";
            button.addEventListener("click", () => { view.mode = key; refreshMetricWorkspace(); });
            rail.appendChild(button);
            const panel = metricWorkspaceElement("section", "metric-analysis-panel"); panel.id = "metric-" + family + "-" + key;
            panel.setAttribute("role", "tabpanel"); panel.setAttribute("aria-labelledby", button.id); panel.hidden = key !== "summary";
            view.panels[key] = panel; stage.appendChild(panel);
            const actions = metricWorkspaceElement("div", "metric-panel-actions"); actions.dataset.actions = key; panel.appendChild(actions);
        }
        rail.addEventListener("keydown", event => {
            const buttons = [...rail.querySelectorAll("button:not(:disabled)")], index = buttons.indexOf(document.activeElement);
            const offset = ({ ArrowDown: 1, ArrowRight: 1, ArrowUp: -1, ArrowLeft: -1 })[event.key];
            if (index < 0 || (!offset && !["Home", "End"].includes(event.key))) return;
            event.preventDefault(); const next = event.key === "Home" ? 0 : event.key === "End" ? buttons.length - 1 : (index + offset + buttons.length) % buttons.length;
            buttons[next].click(); buttons[next].focus();
        });
        workspace.append(rail, stage); host.appendChild(workspace);
        if (["comm", "attrcomm", "edgeattr"].includes(family)) {
            const section = metricWorkspaceElement("section", "metric-family-global");
            section.appendChild(metricWorkspaceElement("h2", "", "Global " + ({ comm: "community", attrcomm: "node attribute", edgeattr: "edge attribute" })[family] + " metrics"));
            const scroll = metricWorkspaceElement("div", "scroll-table"), table = metricWorkspaceElement("table", "data metric-summary-table");
            table.id = "metric-" + family + "-global-results"; table.setAttribute("aria-label", "Global " + family + " metrics"); scroll.appendChild(table);
            const note = metricWorkspaceElement("p", "muted"); section.append(scroll, note); view.panels.summary.appendChild(section);
            const csv = metricWorkspaceAction(metricWorkspaceElement("button"), "table_view", "Export global CSV");
            csv.addEventListener("click", () => download(family + "-global.csv", view.globalRows.map(row => row.map(csvCell).join(",")).join("\r\n"), "text/csv;charset=utf-8"));
            view.panels.summary.querySelector("[data-actions]").appendChild(csv);
            view.globalTable = table; view.globalExport = csv; view.globalNote = note;
        }
        content.slice(0, distributionIndex < 0 ? content.length : distributionIndex).forEach(child => view.panels.summary.appendChild(child));
        view.panels.summary.querySelectorAll(".toolbar").forEach(toolbar => {
            toolbar.querySelectorAll("button").forEach(button => {
                const label = family === "global" ? "Export global CSV" : family === "edgepartitions" ? button.textContent : "Export averages CSV";
                metricWorkspaceAction(button, "table_view", label);
                view.panels.summary.querySelector("[data-actions]").appendChild(button);
            }); toolbar.hidden = true;
        });
        const legacy = metricWorkspaceElement("div"); legacy.hidden = true;
        if (distributionIndex >= 0) for (const child of content.slice(distributionIndex)) {
            if (family === "pairs" || child.classList.contains("rec-scatter-block")) legacy.appendChild(child);
            else view.panels.rankings.appendChild(child);
        }
        if (config?.draw) {
            const rank = view.panels.rankings;
            rank.querySelector("h2").textContent = "Metric rankings";
            const controls = rank.querySelector(".chart-controls"), advanced = metricWorkspaceElement("details", "metric-display-options");
            rank.querySelectorAll(".chart-dl").forEach(button => {
                metricWorkspaceAction(button, "download", "Download PNG / SVG"); rank.querySelector("[data-actions]").appendChild(button);
            });
            advanced.appendChild(metricWorkspaceElement("summary", "", "Display options"));
            const advancedControls = metricWorkspaceElement("div", "chart-controls"); advanced.appendChild(advancedControls);
            for (const suffix of ["sort", "orientation", "order"]) advancedControls.appendChild($(config.prefix + "-chart-" + suffix).closest("label"));
            controls.after(advanced);
            $(config.prefix + "-chart-orientation").value = "horizontal";
            $(config.prefix + "-chart-show").value = "top";
            $(config.prefix + "-chart-sort").disabled = true;
            $(config.prefix + "-chart-k").closest("label").hidden = false;
            const topK = [...rank.querySelectorAll("h2")].find(child => child.textContent === "Top-k nodes");
            if (topK) {
                const table = metricWorkspaceElement("details", "metric-ranking-table");
                table.appendChild(metricWorkspaceElement("summary", "", "Ranking table"));
                let sibling = topK.nextSibling; topK.remove();
                while (sibling) { const next = sibling.nextSibling; table.appendChild(sibling); sibling = next; }
                rank.appendChild(table);
            }
            $(config.prefix + "-chart-metric").addEventListener("change", event => { view.metric = event.target.value; refreshMetricWorkspace(); });
            const sources = metricWorkspaceElement("div", "chart-controls"); sources.dataset.rankingSource = "true";
            sources.addEventListener("change", event => { if (event.target.dataset.analysisKey === "rankingSource") { view.rankingSource = event.target.value; refreshMetricWorkspace(); } });
            controls.after(sources); view.rankingSources = sources;
        }
        if (family === "pairs") view.panels.summary.appendChild(metricWorkspaceElement("p", "muted", "Pair results keep aggregate distributions without retaining an individual record for each pair."));
        if (family === "global") view.panels.summary.appendChild(metricWorkspaceElement("p", "muted", "Global metrics have one value per result. Use the original and recommendation columns to compare results."));
        if (config?.draw || family === "pairs") {
            const statistics = metricWorkspaceElement("details", "metric-summary-statistics");
            statistics.appendChild(metricWorkspaceElement("summary", "", "Distribution statistics"));
            const scroll = metricWorkspaceElement("div", "scroll-table"), table = metricWorkspaceElement("table", "data");
            table.setAttribute("aria-label", "Metric distribution statistics"); scroll.appendChild(table);
            const csv = metricWorkspaceAction(metricWorkspaceElement("button"), "table_view", "Export statistics CSV");
            csv.addEventListener("click", () => download(family + "-statistics.csv", view.statisticsRows.map(row => row.map(csvCell).join(",")).join("\r\n"), "text/csv;charset=utf-8"));
            const actions = metricWorkspaceElement("div", "metric-panel-actions"); actions.appendChild(csv);
            const sources = metricWorkspaceElement("div", "chart-controls");
            sources.addEventListener("change", event => { view.statisticsSource = event.target.value; refreshMetricWorkspace(); });
            const note = metricWorkspaceElement("p", "muted", "Statistics use all finite values for the selected result, independently of ranking limits. Undefined values are excluded; zero is a valid value.");
            statistics.append(actions, sources, scroll, note);
            view.statisticsSources = sources;
            view.statisticsNote = note;
            view.statistics = table; view.statisticsButton = csv; view.panels.summary.appendChild(statistics);
        }
        if (config) for (const mode of ["distributions", "compare"]) {
            const panel = view.panels[mode];
            const controls = metricWorkspaceElement("div", "chart-controls metric-analysis-controls"); controls.dataset.controls = mode;
            controls.addEventListener("change", event => {
                const key = event.target.dataset.analysisKey; if (!key) return;
                if (key === "binSize") {
                    if (!event.target.validity.valid || !event.target.value) return;
                    view.binSize = Number(event.target.value);
                } else view[key] = event.target.value;
                refreshMetricWorkspace();
            });
            const heading = metricWorkspaceElement("h2"); heading.dataset.heading = mode;
            const note = metricWorkspaceElement("p", "muted metric-analysis-note"); note.setAttribute("aria-live", "polite"); note.dataset.note = mode;
            const area = metricWorkspaceElement("div", "chart-area"), chart = metricWorkspaceElement("div", "echarts-chart");
            chart.id = config.prefix + "-analysis-" + mode; chart.setAttribute("role", "img"); area.appendChild(chart);
            const empty = metricWorkspaceElement("p", "metric-analysis-empty"); empty.dataset.empty = mode; empty.setAttribute("role", "status");
            panel.append(controls, heading, note, empty, area);
        }
        host.appendChild(legacy); view.rail = rail;
    }
    const relationships = $("subpane-relationships"), actions = metricWorkspaceElement("div", "metric-panel-actions");
    for (const [id, icon, label] of [["relationship-png", "download", "Download PNG / SVG"], ["relationship-csv", "table_view", "Export CSV"]])
        actions.appendChild(metricWorkspaceAction($(id), icon, label));
    relationships.prepend(actions);
    metricWorkspaceReady = true;
}

function metricWorkspaceSelect(controls, view, key, label, options) {
    const field = metricWorkspaceElement("label", "", label), select = metricWorkspaceElement("select");
    select.dataset.analysisKey = key;
    options.forEach(([value, text]) => select.appendChild(option(value, text)));
    if (!options.some(([value]) => value === view[key])) view[key] = options[0]?.[0] || "";
    select.value = view[key]; select.disabled = !options.length;
    field.appendChild(select); controls.appendChild(field);
    return select;
}

function metricWorkspaceExport(view, mode) {
    const controls = view.panels[mode].querySelector("[data-actions]"); controls.replaceChildren();
    const image = metricWorkspaceAction(metricWorkspaceElement("button", "chart-dl"), "download", "Download PNG / SVG");
    image.disabled = !view.rows.length;
    image.addEventListener("click", () => downloadChartPng(METRIC_WORKSPACE_FAMILIES[view.family].prefix + "-analysis-" + mode, view.family + "-" + mode + ".png"));
    const csv = metricWorkspaceAction(metricWorkspaceElement("button"), "table_view", "Export chart data CSV"); csv.disabled = view.rows.length < 2;
    csv.addEventListener("click", () => download(view.family + "-" + mode + ".csv", view.rows.map(row => row.map(csvCell).join(",")).join("\r\n"), "text/csv;charset=utf-8"));
    controls.append(image, csv);
    return image;
}

function refreshMetricWorkspace() {
    if (!metricWorkspaceReady) return;
    syncMetricComputePanel();
    const family = state.activeSubtab, view = METRIC_WORKSPACE_VIEWS.get(family);
    $("subtabbar").querySelectorAll(".subtab").forEach(button => {
        const active = button.dataset.subtab === family;
        button.setAttribute("aria-selected", String(active)); button.tabIndex = active ? 0 : -1;
    });
    if (!view) return; // Attribute relationships retain their specialised analysis.
    for (const [key, panel] of Object.entries(view.panels)) panel.hidden = key !== view.mode;
    view.rail.querySelectorAll("button").forEach(button => {
        const active = button.dataset.analysisView === view.mode;
        button.setAttribute("aria-selected", String(active)); button.tabIndex = active ? 0 : -1;
    });
    if (family === "global") return;
    const entries = metricAnalysisEntries(family), config = METRIC_WORKSPACE_FAMILIES[family];
    if (!entries.some(entry => entry.key === view.metric)) view.metric = entries[0]?.key || "";
    if (view.mode === "summary") {
        metricWorkspaceGlobalSummary(view);
        if (view.statistics) {
            const available = new Map([["original", "Original"]]);
            entries.forEach(entry => metricResultSources(entry).forEach(source => available.set(source.id, source.name)));
            view.statisticsSources.replaceChildren();
            metricWorkspaceSelect(view.statisticsSources, view, "statisticsSource", "Result", [...available]);
            view.statisticsRows = [["Result", "Metric", "Median", "Minimum", "Maximum", "Valid", "Excluded"], ...entries.map(entry => {
                const source = metricResultSources(entry).find(item => item.id === view.statisticsSource);
                const stats = source?.aggregate ? metricAnalysisAggregateSummary(source.aggregate)
                    : metricAnalysisSummary(source ? source.values : Object.keys(entry.values).map(() => NaN));
                return [available.get(view.statisticsSource), entry.label, stats.median, stats.min, stats.max, stats.count, stats.excluded];
            })];
            if (family === "pairs") {
                const estimated = entries.filter(entry => metricResultSources(entry).find(item => item.id === view.statisticsSource)?.aggregate?.estimated).map(entry => entry.label);
                view.statisticsNote.textContent = estimated.length
                    ? "Statistics use the selected result's stored value counts. Estimates are sampled for: " + estimated.join(", ") + ". Zero is a valid value."
                    : "Statistics use the selected result's stored value counts. Pair results listed here are exact. Zero is a valid value.";
            }
            const head = document.createElement("thead"), header = document.createElement("tr"), body = document.createElement("tbody");
            view.statisticsRows[0].forEach(text => header.appendChild(thEl(text))); head.appendChild(header);
            view.statisticsRows.slice(1).forEach(row => {
                const tr = document.createElement("tr"); row.forEach((value, i) => tr.appendChild(tdText(i < 2 ? value : fmt(value)))); body.appendChild(tr);
            });
            view.statistics.replaceChildren(head, body); view.statisticsButton.disabled = !entries.length;
        }
        return;
    }
    if (view.mode === "rankings") {
        $(config.prefix + "-chart-metric").value = view.metric;
        view.rankingSources.replaceChildren();
        metricWorkspaceSelect(view.rankingSources, view, "rankingSource", "Result", metricResultSources(entries.find(entry => entry.key === view.metric)).map(source => [source.id, source.name]));
        if (family === "edgepartitions") syncEdgePartitionChartSort();
        else {
            const sort = $(config.prefix + "-chart-sort"), original = sort.options[0];
            const compatible = entries.filter(entry => entry.scope === entries.find(item => item.key === view.metric)?.scope);
            setSelectOptions(sort.id, [original.value, ...compatible.map(entry => entry.key)], [original.textContent, ...compatible.map(entry => entry.label)]);
        }
        config.draw();
        if (family === "nodes") {
            $("node-topk-metric").value = view.metric; $("node-topk-k").value = $("node-chart-k").value; renderNodeTopK();
        }
        const note = $(config.prefix + "-chart-view-note");
        if (!entries.length) note.textContent = "Run a metric using Compute metrics to populate this view.";
        return;
    }
    const mode = view.mode, panel = view.panels[mode], controls = panel.querySelector("[data-controls]");
    // Keep the controls (and focused element) alive while a user changes a value.
    const focusedKey = controls.contains(document.activeElement) ? document.activeElement.dataset.analysisKey : null;
    const focusedSeries = controls.contains(document.activeElement) ? document.activeElement.dataset.seriesFocus : null;
    controls.replaceChildren();
    metricWorkspaceSelect(controls, view, "metric", mode === "compare" ? "X metric" : "Metric", entries.map(entry => [entry.key, entry.label]));
    const entry = entries.find(item => item.key === view.metric), recs = metricAnalysisRecommendations(entry);
    let result;
    if (mode === "distributions") {
        metricWorkspaceSelect(controls, view, "type", "Chart", [["histogram", "Histogram"], ["cumulative", "Cumulative distribution"], ...(!entry?.aggregate || Array.isArray(entry.aggregate.valueCounts) ? [["frequency", "Exact value frequency"]] : []), ...(!entry?.aggregate ? [["boxplot", "Box plot"]] : [])]);
        const sources = metricWorkspaceSourcePicker(controls, view, "distributionSources", "Results", metricResultSources(entry));
        if (view.type === "histogram") {
            const exactAggregate = sources.some(item => item.aggregate && Array.isArray(item.aggregate.valueCounts));
            const field = metricWorkspaceElement("label", "", exactAggregate ? "Minimum bin size" : "Bin size"), input = metricWorkspaceElement("input", "chart-k-input");
            const source = sources[0]?.aggregate?.histogram?.[0];
            const automatic = source ? source.x1 - source.x0 : metricAnalysisBinSize(sources.map(item => item.values));
            input.type = "number"; input.min = String(Number.MIN_VALUE); input.step = "any"; input.required = true;
            input.value = view.binSize ?? automatic; input.dataset.analysisKey = "binSize";
            field.appendChild(input); controls.appendChild(field);
        }
        if (["cumulative", "frequency"].includes(view.type)) metricWorkspaceSelect(controls, view, "marks", "Display", [["points", "Points only"], ["lines", "Lines only"], ["both", "Lines and points"]]);
        if (["cumulative", "frequency"].includes(view.type)) {
            metricWorkspaceSelect(controls, view, "distributionXScale", "X axis", [["linear", "Linear"], ["log", "Logarithmic"]]);
            metricWorkspaceSelect(controls, view, "distributionYScale", "Y axis", [["linear", "Linear"], ["log", "Logarithmic"]]);
        }
        if (entry) try {
            result = metricAnalysisDistribution(entry, view.type, view.binSize, null, view.marks, sources);
        } catch (error) {
            if (!(error instanceof RangeError)) throw error;
            result = { empty: true, error: true, note: error.message, rows: [] };
        }
    } else {
        metricWorkspaceSelect(controls, view, "compare", "Compare", [["metric", "Metric vs metric"], ["recommendation", "Original vs recommendations"], ["rec-rec", "Recommendation vs recommendations"]]);
        let other, reference = { id: "original", name: "Original", map: entry?.values || {} }, targets = [];
        if (view.compare === "metric") {
            const compatible = entries.filter(item => item.scope === entry?.scope);
            if (compatible.length > 1 && !view.y) view.y = compatible.find(item => item.key !== view.metric).key;
            metricWorkspaceSelect(controls, view, "y", "Y metric", compatible.map(item => [item.key, item.label]));
            other = compatible.find(item => item.key === view.y);
            const ySources = metricResultSources(other), choices = metricResultSources(entry).map(source => ({ ...source, disabled: !ySources.some(item => item.id === source.id) }));
            const chosen = metricWorkspaceSourcePicker(controls, view, "metricComparisonSources", "Results", choices);
            targets = chosen.map(source => ({ ...source, x: source.map, y: ySources.find(item => item.id === source.id).map }));
        } else {
            const sources = metricResultSources(entry).filter(source => source.id !== "original");
            if (view.compare === "rec-rec") {
                metricWorkspaceSelect(controls, view, "reference", "Reference recommendation (X)", sources.map(source => [source.id, source.name]));
                reference = sources.find(source => source.id === view.reference);
            }
            const choices = sources.filter(source => source.id !== reference?.id);
            const chosen = metricWorkspaceSourcePicker(controls, view, view.compare === "rec-rec" ? "recComparisonTargets" : "recommendationTargets", "Compare against (Y)", choices, choices.length ? [choices[0].id] : []);
            metricWorkspaceSelect(controls, view, "compareType", "Chart", [["scatter", "Paired scatter"], ["difference", "Change distribution"]]);
            targets = chosen.map(source => ({ ...source, x: reference?.map || {}, y: source.map }));
        }
        if (view.compare === "metric" || view.compareType === "scatter") {
            metricWorkspaceSelect(controls, view, "compareXScale", "X axis", [["linear", "Linear"], ["log", "Logarithmic"]]);
            metricWorkspaceSelect(controls, view, "compareYScale", "Y axis", [["linear", "Linear"], ["log", "Logarithmic"]]);
        }
        if (entry) {
            const recCount = metricResultSources(entry).length - 1;
            if (view.compare === "recommendation" && !recCount) result = { empty: true, rows: [], note: "Recompute this metric for recommendations using Compute for recommendations." };
            else if (view.compare === "rec-rec" && recCount < 2) result = { empty: true, rows: [], note: "Compute this metric for at least two recommendations to compare them." };
            else result = metricAnalysisComparisonSeries(entry, other, view.compare, reference || { name: "Recommendation" }, targets, view.compareType);
        }
    }
    if (mode === "distributions" && ["cumulative", "frequency"].includes(view.type))
        result = metricAnalysisApplyLogAxes(result, view.distributionXScale, view.distributionYScale);
    else if (mode === "compare" && (view.compare === "metric" || view.compareType === "scatter"))
        result = metricAnalysisApplyLogAxes(result, view.compareXScale, view.compareYScale);
    const empty = !result || result.empty, chartId = config.prefix + "-analysis-" + mode;
    panel.querySelector("[data-heading]").textContent = result?.title || (mode === "distributions" ? "Metric distribution" : "Metric comparison");
    panel.querySelector("[data-note]").textContent = result?.note || "";
    const message = panel.querySelector("[data-empty]"); message.hidden = !empty;
    message.textContent = !entry ? "Run a metric using Compute metrics to populate this view." : result?.empty && !result.option ? result.note
        : mode === "compare" && view.compare === "recommendation" && !Object.keys(recs).length ? "Recompute this metric with a recommendation to compare its original and recommendation results."
        : "No finite values are available for this selection.";
    $(chartId).parentElement.hidden = empty;
    $(chartId).setAttribute("aria-label", (result?.title || "Metric chart") + ". " + (result?.note || "No data."));
    view.rows = result?.rows || [];
    const image = metricWorkspaceExport(view, mode); image.disabled = empty;
    if (empty) clearChart(chartId); else showEChart(chartId, result.option, result.title);
    if (focusedKey) controls.querySelector('[data-analysis-key="' + focusedKey + '"]')?.focus();
    if (focusedSeries) controls.querySelector('[data-series-focus="' + focusedSeries + '"]')?.focus();
}
