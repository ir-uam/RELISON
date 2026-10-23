"use strict";

// Attribute comparisons use the loaded graph directly; no metric computation is needed.
const ATTRIBUTE_RELATIONSHIPS = { swapped: false, result: null };

function relationshipAxisTypes() {
    const types = $("relationship-mode").value.split("-");
    return ATTRIBUTE_RELATIONSHIPS.swapped ? types.reverse() : types;
}

function relationshipDefinitions(type) {
    if (type === "community") return Object.keys(state.communityData)
        .filter(name => !isAttributeCommunityPartition(name)).map(name => ({ name, numeric: false }));
    return type === "edge" ? edgeAttrDefs() : nodeAttrDefs();
}

function syncRelationshipSelectors(preferred = []) {
    relationshipAxisTypes().forEach((type, index) => {
        const axis = index ? "y" : "x", select = $("relationship-" + axis);
        const previous = preferred[index] ?? select.value;
        const definitions = relationshipDefinitions(type);
        select.replaceChildren(...definitions.map(def => option(def.name, def.name)));
        select.disabled = !definitions.length;
        if (definitions.some(def => def.name === previous)) select.value = previous;
        else if (index && definitions.length > 1 && type === relationshipAxisTypes()[0]) select.selectedIndex = 1;
        $("relationship-" + axis + "-label").textContent = axis.toUpperCase() + ": "
            + ({ node: "Node attribute", edge: "Edge attribute", community: "Community partition" })[type];
    });
}

function relationshipMissing(value, numeric) {
    return value == null || (typeof value === "string" && !value.trim())
        || (numeric && !Number.isFinite(Number(value)));
}

// Keys distinguish unset values from a literal category named "Missing".
function relationshipCategories(values, numeric, binning, bins, labelForValue = null) {
    let min = Infinity, max = -Infinity;
    if (numeric) for (const value of values) {
        if (!relationshipMissing(value, true)) { min = Math.min(min, Number(value)); max = Math.max(max, Number(value)); }
    }
    const useBins = numeric && binning === "bins" && max > min;
    const width = useBins ? (max - min) / bins : 0;
    const precision = useBins ? Math.min(17, Math.max(6,
        Math.ceil(Math.log10(Math.max(Math.abs(min), Math.abs(max), 1) / Math.max(width, Number.MIN_VALUE))) + 3)) : 12;
    const boundary = value => Number(value.toPrecision(precision)).toString();
    const categories = new Map();
    if (useBins) for (let index = 0; index < bins; index++) {
        const key = "bin:" + index;
        categories.set(key, { key, order: index,
            label: boundary(min + index * width) + " ≤ value " + (index === bins - 1 ? "≤ " : "< ")
                + boundary(index === bins - 1 ? max : min + (index + 1) * width) });
    }
    const entries = values.map(value => {
        let key, label, order;
        if (relationshipMissing(value, numeric)) { key = "missing"; label = "Missing (unset)"; order = Infinity; }
        else if (useBins) {
            const index = Math.min(bins - 1, Math.max(0, Math.floor((Number(value) - min) / width)));
            key = "bin:" + index;
            label = categories.get(key).label;
            order = index;
        } else {
            label = labelForValue ? labelForValue(value)
                : numeric ? String(Number(value)) : (typeof value === "object" ? JSON.stringify(value) : String(value));
            // Keep community IDs as category keys even when two custom labels happen to match.
            key = "value:" + (labelForValue ? String(value) : label); order = numeric ? Number(value) : 0;
        }
        if (!categories.has(key)) categories.set(key, { key, label, order });
        return key;
    });
    const sorted = [...categories.values()].sort((a, b) => {
        if (a.key === "missing" || b.key === "missing") return a.key === b.key ? 0 : a.key === "missing" ? 1 : -1;
        return numeric ? a.order - b.order : a.label.localeCompare(b.label, undefined, { numeric: true });
    });
    const indices = new Map(sorted.map((category, index) => [category.key, index]));
    return { labels: sorted.map(category => category.label), indices: entries.map(key => indices.get(key)) };
}

function buildRelationshipMatrix(pairs, xNumeric, yNumeric, xBinning, yBinning, bins, includeMissing, labelers = []) {
    const included = includeMissing ? pairs : pairs.filter(pair => !relationshipMissing(pair[0], xNumeric) && !relationshipMissing(pair[1], yNumeric));
    const x = relationshipCategories(included.map(pair => pair[0]), xNumeric, xBinning, bins, labelers[0]);
    const y = relationshipCategories(included.map(pair => pair[1]), yNumeric, yBinning, bins, labelers[1]);
    const counts = new Map(), rows = Array(y.labels.length).fill(0), columns = Array(x.labels.length).fill(0);
    included.forEach((pair, i) => {
        const column = x.indices[i], row = y.indices[i], key = row + ":" + column;
        counts.set(key, (counts.get(key) || 0) + 1);
        rows[row]++; columns[column]++;
    });
    return { x: x.labels, y: y.labels, counts, rows, columns, total: included.length, excluded: pairs.length - included.length };
}

function relationshipCell(result, column, row) {
    const count = result.counts.get(row + ":" + column) || 0;
    const rowPercent = result.rows[row] ? 100 * count / result.rows[row] : 0;
    const columnPercent = result.columns[column] ? 100 * count / result.columns[column] : 0;
    return { count, rowPercent, columnPercent,
        value: result.display === "row" ? rowPercent : result.display === "column" ? columnPercent : count };
}

function renderAttributeRelationships() {
    syncRelationshipSelectors();
    ATTRIBUTE_RELATIONSHIPS.result = null;
    $("relationship-png").disabled = true;
    $("relationship-csv").disabled = true;
    const types = relationshipAxisTypes(), names = [$("relationship-x").value, $("relationship-y").value];
    const definitions = types.map((type, i) => relationshipDefinitions(type).find(def => def.name === names[i]));
    definitions.forEach((def, i) => { $("relationship-" + (i ? "y" : "x") + "-binning-field").hidden = !def?.numeric; });
    const binnings = [$("relationship-x-binning").value, $("relationship-y-binning").value];
    const binned = definitions.some((def, i) => def?.numeric && binnings[i] === "bins");
    $("relationship-bins-field").hidden = !binned;
    const empty = message => {
        clearChart("relationship-chart");
        $("relationship-area").hidden = true;
        $("relationship-note").textContent = message;
    };
    if (!state.graph) { empty("Load a graph to compare its attributes."); return; }
    if (definitions.some(def => !def)) {
        empty("Load the required attributes or detect community partitions to use this comparison."); return;
    }
    if (binned && !$("relationship-bins").validity.valid) { empty("Choose a whole number of bins between 2 and 100."); return; }
    const bins = Math.max(2, Math.min(100, Number($("relationship-bins").value) || 10));
    const pairs = [];
    const value = (id, i) => types[i] === "community" ? state.communityData[names[i]]?.[id]
        : types[i] === "edge" ? edgeAttrVal(id, names[i]) : nodeAttrVal(id, names[i]);
    const collect = id => pairs.push([value(id, 0), value(id, 1)]);
    if (types[0] === "edge") state.graph.forEachEdge(collect);
    else state.graph.forEachNode(collect);
    const communityLabelers = types.map((type, index) => type === "community"
        ? value => communityLabel(value, names[index]) : null);
    const result = buildRelationshipMatrix(pairs, !!definitions[0].numeric, !!definitions[1].numeric,
        binnings[0], binnings[1], bins, $("relationship-missing").value === "include", communityLabelers);
    result.names = names; result.display = $("relationship-display").value;
    result.entity = types[0] === "edge" ? "edges" : "nodes";
    const displayLabel = { count: "Counts", row: "Row percentages", column: "Column percentages" }[result.display];
    if (!result.total) { empty("No " + result.entity + " have values for both selections. Try including missing values."); return; }
    ATTRIBUTE_RELATIONSHIPS.result = result;
    $("relationship-csv").disabled = false;
    const description = "Counting " + result.total.toLocaleString() + " " + result.entity + "; "
        + result.excluded.toLocaleString() + " excluded for missing values. " + displayLabel + "."
        + (result.display === "row" ? " Each nonempty row (Y value) sums to 100%." : result.display === "column" ? " Each nonempty column (X value) sums to 100%." : "");
    if (result.x.length * result.y.length > 40000) {
        empty(description + " Too many cells to display (limit: 40,000). Use numeric bins or select attributes with fewer values. CSV export remains available for occupied cells.");
        return;
    }
    $("relationship-area").hidden = false;
    $("relationship-note").textContent = description;
    const colors = echartColors(), chartOption = chartBase(colors, "");
    const axis = (labels, name) => ({ type: "category", data: labels, name, nameLocation: "middle",
        nameTextStyle: { color: colors.text, fontSize: colors.fontSize + 1, fontWeight: 600 },
        axisLabel: { color: colors.muted, fontSize: colors.labelSize, hideOverlap: true },
        axisLine: { show: true, lineStyle: { color: colors.border } },
        axisTick: { show: true, inside: false, length: 6, lineStyle: { color: colors.border } }, splitLine: { show: false } });
    // The selected attribute/partition names remain available in tooltips and
    // selectors; keep the chart axes focused on their category tick labels.
    chartOption.xAxis = axis(result.x, "");
    chartOption.yAxis = { ...axis(result.y, ""), inverse: true };
    const points = []; let max = 0;
    result.y.forEach((label, row) => result.x.forEach((label, column) => {
        const cell = relationshipCell(result, column, row);
        points.push([column, row, cell.value]); max = Math.max(max, cell.value);
    }));
    chartOption.tooltip = { ...chartOption.tooltip, trigger: "item", formatter: params => {
        const [column, row] = params.data, cell = relationshipCell(result, column, row);
        return "X · " + escapeChartHtml(names[0]) + ": <b>" + escapeChartHtml(result.x[column]) + "</b><br>"
            + "Y · " + escapeChartHtml(names[1]) + ": <b>" + escapeChartHtml(result.y[row]) + "</b><br>"
            + "Count: <b>" + cell.count.toLocaleString() + "</b> " + result.entity + "<br>"
            + "Row: <b>" + cell.rowPercent.toFixed(2) + "%</b><br>Column: <b>" + cell.columnPercent.toFixed(2) + "%</b>";
    } };
    chartOption.visualMap = { min: 0, max: result.display === "count" ? Math.max(1, max) : 100, calculable: true,
        text: [result.display === "count" ? "Count" : "%", ""], inRange: { color: [colors.background, colors.accent] },
        textStyle: { color: colors.muted }, formatter: value => result.display === "count" ? fmt(value) : value.toFixed(1) + "%" };
    chartOption.dataZoom = [{ type: "inside", xAxisIndex: 0, yAxisIndex: 0, filterMode: "none" },
        { type: "slider", xAxisIndex: 0, filterMode: "none", borderColor: colors.border,
            textStyle: { color: colors.muted }, fillerColor: colors.accent + "33", handleStyle: { color: colors.accent } }];
    chartOption.series = [{ type: "heatmap", data: points, progressive: 4000,
        itemStyle: { borderWidth: 1, borderColor: colors.background },
        emphasis: { itemStyle: { borderColor: colors.text, borderWidth: 2 } } }];
    showEChart("relationship-chart", chartOption, types.join(" × ") + " · " + names[0] + " × " + names[1] + " — " + displayLabel);
    $("relationship-png").disabled = false;
}

function exportAttributeRelationshipsCsv() {
    const result = ATTRIBUTE_RELATIONSHIPS.result;
    if (!result) return;
    const lines = [["x_attribute", "x_value", "y_attribute", "y_value", "entity", "count", "row_percent", "column_percent", "display", "display_value"].map(csvCell).join(",")];
    const append = (row, column) => {
        const cell = relationshipCell(result, column, row);
        lines.push([result.names[0], result.x[column], result.names[1], result.y[row], result.entity,
            cell.count, cell.rowPercent, cell.columnPercent, result.display, cell.value].map(csvCell).join(","));
    };
    if (result.x.length * result.y.length <= 40000) {
        result.y.forEach((label, row) => result.x.forEach((label, column) => append(row, column)));
    } else {
        // Large grids remain exportable without generating millions of empty cells.
        for (const key of result.counts.keys()) { const [row, column] = key.split(":").map(Number); append(row, column); }
    }
    download("attribute-relationships.csv", lines.join("\n"), "text/csv;charset=utf-8");
}

function setupAttributeRelationships() {
    $("relationship-mode").addEventListener("change", () => { ATTRIBUTE_RELATIONSHIPS.swapped = false; renderAttributeRelationships(); });
    for (const control of ["x", "y", "display", "missing", "x-binning", "y-binning", "bins"]) {
        $("relationship-" + control).addEventListener("change", renderAttributeRelationships);
    }
    $("relationship-swap").addEventListener("click", () => {
        const preferred = [$("relationship-y").value, $("relationship-x").value];
        const xBinning = $("relationship-x-binning").value;
        $("relationship-x-binning").value = $("relationship-y-binning").value;
        $("relationship-y-binning").value = xBinning;
        ATTRIBUTE_RELATIONSHIPS.swapped = !ATTRIBUTE_RELATIONSHIPS.swapped;
        syncRelationshipSelectors(preferred); renderAttributeRelationships();
    });
    $("relationship-csv").addEventListener("click", exportAttributeRelationshipsCsv);
}
