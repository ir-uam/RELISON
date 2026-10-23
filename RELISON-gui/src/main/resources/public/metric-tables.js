"use strict";

const METRIC_TABLE_SORTS = new Map();

// Separate the catalog metric name from the parameter suffix recorded with its result.
// Work from saved result labels, rather than the runner controls, which may have changed.
function metricTableDetails(label) {
    let text = String(label), estimated = "";
    if (text.endsWith(" (estimated)")) { text = text.slice(0, -12); estimated = " (estimated)"; }
    const divider = text.indexOf(" · ");
    const grouping = divider < 0 ? "" : text.slice(divider + 3);
    if (divider >= 0) text = text.slice(0, divider);
    const definitions = ["vertex", "graph", "pair", "communityIndividual", "communityGlobal",
        "edgePartitionGlobal", "edgePartitionIndividual", "intraEdgePartitionIndividual"]
        .flatMap(family => Object.values(state.defs?.[family] || {}));
    const definition = definitions.filter(def => text === def.label || text.startsWith(def.label + " ("))
        .sort((a, b) => b.label.length - a.label.length)[0];
    const boundary = text.indexOf(" (");
    const metric = definition?.label || (boundary < 0 ? text : text.slice(0, boundary));
    let suffix = text.slice(metric.length).trim();
    const parameters = [];
    // Read balanced groups so a partition's own parameter suffix remains intact.
    while (suffix.startsWith("(")) {
        let depth = 0, end = -1;
        for (let i = 0; i < suffix.length; i++) {
            if (suffix[i] === "(") depth++;
            else if (suffix[i] === ")" && --depth === 0) { end = i; break; }
        }
        if (end < 0) break;
        parameters.push(suffix.slice(1, end));
        suffix = suffix.slice(end + 1).trim();
    }
    if (suffix) parameters.push(suffix);
    const displayParameters = formatAuthorityMode(parameters.join("; "), metric);
    return { metric: metric + estimated, parameters: [displayParameters, grouping].filter(Boolean).join("; ") };
}

function compareMetricTableValues(a, b, direction) {
    const missing = value => value == null || (typeof value === "number" && Number.isNaN(value));
    if (missing(a) || missing(b)) return missing(a) === missing(b) ? 0 : missing(a) ? 1 : -1;
    if (typeof a === "number" && typeof b === "number") return direction * (a === b ? 0 : a < b ? -1 : 1);
    return direction * String(a).localeCompare(String(b), undefined, { numeric: true, sensitivity: "base" });
}

// All summary tables share the Top-k table's markup, styling, and three base columns.
// Recommendation values remain available as additional sortable comparison columns.
function buildAverageTable(tableId, rows, recKeys) {
    const table = $(tableId), sort = METRIC_TABLE_SORTS.get(tableId);
    const focusedKey = table.contains(document.activeElement) ? document.activeElement.dataset.sortKey : null;
    const entries = rows.map((row, index) => ({ ...row, ...metricTableDetails(row.label), index }));
    const columns = [
        { key: "metric", label: "Metric", value: row => row.metric },
        { key: "parameters", label: "Parameters", value: row => row.parameters },
        { key: "value", label: "Value", numeric: true, value: row => row.original },
        ...recKeys.map((key, i) => ({ key: "rec:" + key, label: recLabel(key), numeric: true, value: row => row.rec?.[i] })),
    ];
    const sortedColumn = columns.find(column => column.key === sort?.key);
    if (sortedColumn) entries.sort((a, b) => compareMetricTableValues(sortedColumn.value(a), sortedColumn.value(b), sort.direction) || a.index - b.index);
    const thead = document.createElement("thead"), header = document.createElement("tr");
    columns.forEach(column => {
        const th = document.createElement("th"), button = document.createElement("button");
        const active = sortedColumn === column;
        th.className = "sort-th";
        th.setAttribute("scope", "col");
        th.setAttribute("aria-sort", active ? (sort.direction === 1 ? "ascending" : "descending") : "none");
        button.type = "button";
        button.className = "metric-sort-button";
        button.dataset.sortKey = column.key;
        button.textContent = column.label + (active ? (sort.direction === 1 ? " ▲" : " ▼") : " ↕");
        const direction = active ? -sort.direction : column.numeric ? -1 : 1;
        button.setAttribute("aria-label", "Sort " + column.label + " " + (direction === 1 ? "ascending" : "descending"));
        button.addEventListener("click", () => {
            METRIC_TABLE_SORTS.set(tableId, { key: column.key, direction });
            buildAverageTable(tableId, rows, recKeys);
        });
        th.appendChild(button); header.appendChild(th);
    });
    thead.appendChild(header);
    const tbody = document.createElement("tbody");
    entries.forEach(row => {
        const tr = document.createElement("tr");
        columns.forEach(column => {
            const value = column.value(row);
            const td = tdText(column.numeric ? (value == null ? "" : fmt(value)) : value || "—");
            td.className = column.numeric ? "metric-value-cell" : "metric-text-cell";
            tr.appendChild(td);
        });
        tbody.appendChild(tr);
    });
    table.replaceChildren(thead, tbody);
    if (focusedKey) [...header.querySelectorAll("button")].find(button => button.dataset.sortKey === focusedKey)?.focus();
}
