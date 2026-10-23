"use strict";

const COMMUNITY_APPEARANCE_DEFAULTS = Object.freeze({ nodeMin: 4, nodeMax: 25, labelMode: "default", labelColor: "#ffffff",
    edgeShape: "curved", edgeSizeMode: "weight", edgeWidth: 2, edgeMin: 0.5, edgeMax: 5.5, edgeColorMode: "default", edgeColor: "#8c96a5",
    nodeScale: "proportional", nodeBorderOn: false, nodeBorderColor: "#1b1c1f", nodeBorderWidth: 1,
    labelShow: true, labelSize: 12, labelFont: "sans-serif", labelProp: false,
    edgeLabelShow: false, edgeLabelSize: 9, edgeLabelFont: "sans-serif", edgeLabelProp: false, edgeLabelMode: "default", edgeLabelColor: "#ffffff" });
const COMMUNITY_APPEARANCE_CONTROLS = { nodeMin: "community-node-min", nodeMax: "community-node-max", labelMode: "community-label-mode",
    labelColor: "community-label-color", edgeSizeMode: "community-edge-size-mode", edgeWidth: "community-edge-width",
    edgeMin: "community-edge-min", edgeMax: "community-edge-max", edgeColorMode: "community-edge-color-mode", edgeColor: "community-edge-color", edgeShape: "community-edge-shape",
    nodeScale: "community-node-scale", nodeBorderOn: "community-node-border-on", nodeBorderColor: "community-node-border-color", nodeBorderWidth: "community-node-border-width",
    labelShow: "community-label-show", labelSize: "community-label-size", labelFont: "community-label-font", labelProp: "community-label-prop",
    edgeLabelShow: "community-edge-label-show", edgeLabelSize: "community-edge-label-size", edgeLabelFont: "community-edge-label-font",
    edgeLabelProp: "community-edge-label-prop", edgeLabelMode: "community-edge-label-mode", edgeLabelColor: "community-edge-label-color" };
const COMMUNITY_APPEARANCE_RANGES = { nodeMin: [0.1, 200], nodeMax: [0.1, 200], edgeWidth: [0, 50], edgeMin: [0, 50], edgeMax: [0, 50],
    nodeBorderWidth: [0, 20], labelSize: [1, 100], edgeLabelSize: [1, 100] };
const COMMUNITY_LABEL_FONTS = [{ value: "sans-serif", label: "Sans-serif" }, { value: "serif", label: "Serif" },
    { value: "monospace", label: "Monospace" }, { value: "'Segoe UI', sans-serif", label: "Segoe UI" }, { value: "Georgia, serif", label: "Georgia" }];
const COMMUNITY_LAYOUTS = [
    { id: "circular", label: "Circular", spatial: "spherical-3d", spatialLabel: "Spherical" },
    { id: "fruchterman-reingold", label: "Fruchterman–Reingold", spatial: "fruchterman-reingold-3d" },
    { id: "grid", label: "Grid", spatial: "grid-3d" },
    { id: "random", label: "Random", spatial: "random-3d" },
    { id: "spectral", label: "Spectral", spatial: "spectral-3d" },
    { id: "stress-majorization", label: "Stress majorization", spatial: "stress-majorization-3d" },
    { id: "kamada-kawai", label: "Kamada–Kawai", spatial: "kamada-kawai-flat-3d", spatialLabel: "Kamada–Kawai (planar in 3D)" },
    { id: "multilevel-force", label: "Multilevel force (Hu)", spatial: "multilevel-force-flat-3d", spatialLabel: "Multilevel force (Hu, planar in 3D)" },
    { id: "relison-forceatlas2", label: "ForceAtlas2", spatial: "relison-forceatlas2-flat-3d", spatialLabel: "ForceAtlas2 (planar in 3D)" },
    { id: "concentric", label: "Concentric (community graph degree)", spatial: "concentric-sphere-3d", spatialLabel: "Concentric spheres (community graph degree)" },
];

// The explorer reads existing partitions and metric values. Metric computation stays in app.js / Metrics.
const COMMUNITY_EXPLORER = {
    partition: "", view: "graph", selected: {}, labels: {}, notes: {}, metadata: {}, positions: {},
    mode: "weighted", layout: "circular", rendererType: "cosmograph", cosmographDimensions: 2, loops: false, search: "", sort: "size", descending: true,
    page: 0, memberPage: 0, memberSearch: "", memberColumn: "id", request: 0,
    data: null, dataKey: "", loadingKey: "", controller: null, graph: null, renderer: null, rendering: 0, renderController: null, renderPending: false, cameras: {},
    appearance: { ...COMMUNITY_APPEARANCE_DEFAULTS },
    appearanceFitPending: false, hoveredNode: null,
};
const COMMUNITY_PAGE_SIZE = 50;
const COMMUNITY_LABEL_METRICS = new Map();
let communityLabelMeasureContext;

function resetCommunityExplorer() {
    const e = COMMUNITY_EXPLORER;
    e.controller?.abort(); e.request++;
    cancelCommunityGraphRender();
    e.renderer?.kill(); e.renderer = e.graph = e.data = null;
    Object.assign(e, { partition: "", selected: {}, labels: {}, notes: {}, metadata: {}, positions: {},
        view: "graph", mode: "weighted", layout: "circular", rendererType: "cosmograph", cosmographDimensions: 2, cameras: {}, loops: false, search: "", sort: "size",
        descending: true, page: 0, memberPage: 0, memberSearch: "", memberColumn: "id", dataKey: "", loadingKey: "" });
    e.appearance = { ...COMMUNITY_APPEARANCE_DEFAULTS };
    e.appearanceFitPending = false;
    if (document.getElementById("explorer-search")) {
        $("explorer-search").value = "";
        $("community-loops").replaceChildren();
        $("community-foreground").replaceChildren();
        refreshCommunityExplorer();
    }
}

function communityDetected(partition, algorithm, params, count) {
    const e = COMMUNITY_EXPLORER;
    leaveCommunityExplorer();
    // A repeated configuration can produce a different partition; its old labels and metrics must not survive.
    delete e.labels[partition]; delete e.positions[partition]; delete e.selected[partition];
    for (const key of Object.keys(e.cameras)) if (JSON.parse(key)[0] === partition) delete e.cameras[key];
    e.metadata[partition] = { algorithm, params, count, detectedAt: new Date().toISOString() };
    for (const label of state.commMetricOrder.filter(label => state.commMetricData[label]?.algorithm === partition)) {
        delete state.commMetricData[label];
        delete state.recMetricData.comm[label];
    }
    state.commMetricOrder = state.commMetricOrder.filter(label => state.commMetricData[label]);
    for (const label of Object.keys(state.graphMetrics)) if (label.endsWith(" (" + partition + ")")) {
        delete state.graphMetrics[label]; delete state.recMetricData.graph[label];
    }
    e.partition = partition; e.dataKey = ""; e.page = e.memberPage = 0; e.memberSearch = "";
}

function findCommunityPartition(algorithm, params) {
    const keys = Object.keys(params);
    // collectParams supplies typed scalar values; key order must not affect a match.
    return Object.keys(state.communityData).find(partition => {
        const saved = COMMUNITY_EXPLORER.metadata[partition];
        return saved?.algorithm === algorithm && saved.params && Object.keys(saved.params).length === keys.length
            && keys.every(key => Object.hasOwn(saved.params, key) && saved.params[key] === params[key]);
    });
}

function selectCommunityPartition(partition) {
    const e = COMMUNITY_EXPLORER;
    if (isAttributeCommunityPartition(partition)) ensureAttributeCommunityPartition(partition);
    if (!Object.hasOwn(state.communityData, partition)) return;
    if (e.partition !== partition) {
        leaveCommunityExplorer();
        e.partition = partition; e.page = e.memberPage = 0; e.memberSearch = "";
    }
    refreshCommunityExplorer();
}

function isAttributeCommunityPartition(partition) {
    return typeof partition === "string" && partition.startsWith("attribute:");
}

function attributeCommunityPartitionKey(name) {
    return "attribute:" + encodeURIComponent(name);
}

function ensureAttributeCommunityPartition(partition) {
    if (!isAttributeCommunityPartition(partition)) return;
    let attribute;
    try { attribute = decodeURIComponent(partition.slice("attribute:".length)); }
    catch (_) { return; }
    const definition = nodeAttrDefs().find(def => def.name === attribute && !def.numeric);
    if (!definition || !state.graph) return;
    const e = COMMUNITY_EXPLORER, metadata = e.metadata[partition];
    if (Object.hasOwn(state.communityData, partition) && metadata?.revision === state.networkRevision
        && metadata?.graphId === state.graphId) return;

    const valueToCommunity = new Map(), valueLabels = {}, assignment = {};
    state.graph.forEachNode(node => {
        const rawValue = nodeAttrVal(node, attribute), value = rawValue == null ? null : rawValue;
        let id = valueToCommunity.get(value);
        if (id == null && !valueToCommunity.has(value)) {
            id = valueToCommunity.size;
            valueToCommunity.set(value, id);
            valueLabels[String(id)] = value == null ? "(missing)" : String(value);
        }
        assignment[node] = id;
    });
    state.communityData[partition] = assignment;
    delete e.labels[partition];
    e.metadata[partition] = { algorithm: partition, attribute, valueLabels,
        count: valueToCommunity.size, revision: state.networkRevision, graphId: state.graphId };
}

function communityLabel(id, partition = COMMUNITY_EXPLORER.partition) {
    return COMMUNITY_EXPLORER.labels[partition]?.[id]
        || COMMUNITY_EXPLORER.metadata[partition]?.valueLabels?.[id] || "Community " + id;
}

function refreshCommunityLegend(partition) {
    const legend = state.colorLegend;
    if (legend?.type !== "categorical" || legend.colorKey !== "community:" + partition) return;
    for (const entry of legend.entries) entry.label = communityLabel(entry.value, partition);
    buildNodeLegend();
}

function communityRows() {
    const e = COMMUNITY_EXPLORER, groups = new Map();
    const count = e.metadata[e.partition]?.count;
    if (Number.isInteger(count)) for (let id = 0; id < count; id++) groups.set(String(id), []);
    for (const [node, id] of Object.entries(state.communityData[e.partition] || {})) {
        if (id == null || Number(id) < 0 || !state.graph?.hasNode(node)) continue;
        const key = String(id);
        if (!groups.has(key)) groups.set(key, []);
        groups.get(key).push(node);
    }
    return [...groups].map(([id, members]) => ({ id, label: communityLabel(id), size: members.length,
        share: state.graph?.order ? 100 * members.length / state.graph.order : 0, members }));
}

function communityMetricColumns() {
    const partition = COMMUNITY_EXPLORER.partition, attribute = COMMUNITY_EXPLORER.metadata[partition]?.attribute;
    const communityMetrics = state.commMetricOrder.filter(label => state.commMetricData[label]?.algorithm === partition)
        .map(label => ({ key: "metric:" + label, label: label.endsWith(" · " + partition) ? label.slice(0, -(partition.length + 3)) : label }));
    if (!attribute) return communityMetrics;
    const attributeMetrics = state.attributeMetricOrder.filter(label => state.attributeMetricData[label]?.attribute === attribute)
        .map(label => ({ key: "attribute-metric:" + label,
            label: label.endsWith(" · Attribute: " + attribute) ? label.slice(0, -(" · Attribute: " + attribute).length) : label }));
    return [...communityMetrics, ...attributeMetrics];
}

function updateNodeAttributeMetricLabels(attribute, previousValue, value, merged) {
    const keys = attributeMetricResultKeys("node", attribute);
    if (merged) {
        for (const key of keys.individual) { delete state.attributeMetricData[key]; delete state.recMetricData.comm[key]; }
        state.attributeMetricOrder = state.attributeMetricOrder.filter(key => state.attributeMetricData[key]);
        for (const key of keys.community) { delete state.commMetricData[key]; delete state.recMetricData.comm[key]; }
        state.commMetricOrder = state.commMetricOrder.filter(key => state.commMetricData[key]);
        for (const key of keys.global) { delete state.graphMetrics[key]; delete state.globalMetricScopes?.[key]; delete state.recMetricData.graph[key]; }
        for (const [key, recipe] of Object.entries(state.metricComputations || {}))
            if (recipe.family === "attrcomm" && recipe.body.attribute === attribute) delete state.metricComputations[key];
        return;
    }
    const oldLabel = previousValue == null ? "(missing)" : String(previousValue), label = value == null ? "(missing)" : String(value);
    for (const key of keys.individual) {
        const entry = state.attributeMetricData[key];
        for (const id of Object.keys(entry.valueLabels || {})) if (entry.valueLabels[id] === oldLabel) entry.valueLabels[id] = label;
    }
    if (previousValue == null) {
        for (const key of [...keys.individual, ...keys.community]) delete state.recMetricData.comm[key];
        for (const key of keys.global) delete state.recMetricData.graph[key];
    }
}

function communityValue(row, key) {
    if (key.startsWith("metric:")) return state.commMetricData[key.slice(7)]?.values[row.id];
    if (key.startsWith("attribute-metric:")) {
        const entry = state.attributeMetricData[key.slice("attribute-metric:".length)];
        const valueId = Object.keys(entry?.valueLabels || {}).find(id => String(entry.valueLabels[id]) === String(row.label));
        return valueId == null ? undefined : entry.values[valueId];
    }
    if (key === "notes") return COMMUNITY_EXPLORER.notes[COMMUNITY_EXPLORER.partition]?.[row.id] || "";
    return row[key];
}

function communitySortedRows() {
    const e = COMMUNITY_EXPLORER, search = e.search.toLowerCase();
    return communityRows().filter(row => (row.id + " " + row.label).toLowerCase().includes(search)).sort((a, b) => {
        const av = communityValue(a, e.sort), bv = communityValue(b, e.sort);
        if (av == null || !Number.isFinite(av) && typeof av === "number") return bv == null ? 0 : 1;
        if (bv == null || !Number.isFinite(bv) && typeof bv === "number") return -1;
        const cmp = typeof av === "number" && typeof bv === "number" ? av - bv
            : String(av).localeCompare(String(bv), undefined, { numeric: true });
        return (e.descending ? -cmp : cmp) || Number(a.id) - Number(b.id);
    });
}

function communityColors() {
    const e = COMMUNITY_EXPLORER, order = [], seen = new Set();
    // Match applyCategorical's first-appearance order, including user legend overrides.
    state.graph?.forEachNode(node => {
        const value = state.communityData[e.partition]?.[node];
        if (value == null) return;
        const id = String(value);
        if (!seen.has(id)) { seen.add(id); order.push(id); }
    });
    for (const row of communityRows()) if (!seen.has(row.id)) { seen.add(row.id); order.push(row.id); }
    return Object.fromEntries(order.map((id, index) => [id, state.catColors["community:" + e.partition]?.[id] || categoricalHex(index)]));
}

function normalizeCommunityAppearance(saved = {}) {
    const result = { ...COMMUNITY_APPEARANCE_DEFAULTS };
    for (const [key, [min, max]] of Object.entries(COMMUNITY_APPEARANCE_RANGES))
        if (typeof saved[key] === "number" && Number.isFinite(saved[key]) && saved[key] >= min && saved[key] <= max) result[key] = saved[key];
    if (result.nodeMin > result.nodeMax) { result.nodeMin = 4; result.nodeMax = 25; }
    if (result.edgeMin > result.edgeMax) { result.edgeMin = 0.5; result.edgeMax = 5.5; }
    if (["default", "single", "node"].includes(saved.labelMode)) result.labelMode = saved.labelMode;
    if (["weight", "uniform"].includes(saved.edgeSizeMode)) result.edgeSizeMode = saved.edgeSizeMode;
    if (["curved", "straight"].includes(saved.edgeShape)) result.edgeShape = saved.edgeShape;
    if (["default", "single", "average", "source", "target"].includes(saved.edgeColorMode)) result.edgeColorMode = saved.edgeColorMode;
    if (["proportional", "percentile"].includes(saved.nodeScale)) result.nodeScale = saved.nodeScale;
    if (["default", "single", "edge"].includes(saved.edgeLabelMode)) result.edgeLabelMode = saved.edgeLabelMode;
    for (const key of ["labelColor", "edgeColor", "nodeBorderColor", "edgeLabelColor"]) if (/^#[\da-f]{6}$/i.test(saved[key])) result[key] = saved[key];
    for (const key of ["labelShow", "labelProp", "edgeLabelShow", "edgeLabelProp", "nodeBorderOn"]) if (typeof saved[key] === "boolean") result[key] = saved[key];
    for (const key of ["labelFont", "edgeLabelFont"]) if (COMMUNITY_LABEL_FONTS.some(font => font.value === saved[key])) result[key] = saved[key];
    return result;
}

function syncCommunityAppearanceControls() {
    for (const [key, id] of Object.entries(COMMUNITY_APPEARANCE_CONTROLS)) {
        if (typeof COMMUNITY_APPEARANCE_DEFAULTS[key] === "boolean") $(id).checked = COMMUNITY_EXPLORER.appearance[key];
        else $(id).value = String(COMMUNITY_EXPLORER.appearance[key]);
    }
    communityAppearanceVisibility();
}

function communityAppearanceVisibility() {
    $("community-label-color-field").hidden = $("community-label-mode").value !== "single";
    $("community-edge-color-field").hidden = $("community-edge-color-mode").value !== "single";
    $("community-edge-label-color-field").hidden = $("community-edge-label-mode").value !== "single";
    $("community-node-border-color-field").hidden = $("community-node-border-width-field").hidden = !$("community-node-border-on").checked;
    const uniform = $("community-edge-size-mode").value === "uniform";
    $("community-edge-width-field").hidden = !uniform;
    $("community-edge-min-field").hidden = $("community-edge-max-field").hidden = uniform;
}

function applyCommunityAppearanceControls() {
    communityAppearanceVisibility();
    const values = {};
    for (const [key, id] of Object.entries(COMMUNITY_APPEARANCE_CONTROLS)) {
        const uniform = $("community-edge-size-mode").value === "uniform";
        if (key === "edgeWidth" && !uniform || ["edgeMin", "edgeMax"].includes(key) && uniform
            || key === "nodeBorderWidth" && !$("community-node-border-on").checked) {
            values[key] = COMMUNITY_EXPLORER.appearance[key];
            continue;
        }
        values[key] = typeof COMMUNITY_APPEARANCE_DEFAULTS[key] === "boolean" ? $(id).checked
            : typeof COMMUNITY_APPEARANCE_DEFAULTS[key] === "number" ? Number($(id).value) : $(id).value;
        if (COMMUNITY_APPEARANCE_RANGES[key] && (!$(id).value.trim() || !Number.isFinite(values[key])
            || values[key] < COMMUNITY_APPEARANCE_RANGES[key][0] || values[key] > COMMUNITY_APPEARANCE_RANGES[key][1])) {
            $("community-appearance-status").textContent = "Enter valid sizes and widths within the displayed limits.";
            return;
        }
    }
    if (values.nodeMin > values.nodeMax || values.edgeMin > values.edgeMax) {
        $("community-appearance-status").textContent = "Minimum size or width must not exceed its maximum.";
        return;
    }
    COMMUNITY_EXPLORER.appearance = normalizeCommunityAppearance(values);
    $("community-appearance-status").textContent = "";
    updateCommunityGraphLegend(); return updateCommunityGraphAppearance({ fit: true });
}

function communityGraphStyles() {
    const e = COMMUNITY_EXPLORER, a = e.appearance, colors = communityColors(), nodes = e.data?.nodes || [], edges = e.data?.edges || [];
    const radii = scaleVisualValues(nodes, node => node.size, a.nodeMin, a.nodeMax, a.nodeScale);
    const nodeSizes = new Map(nodes.map((node, index) => [node.id, radii[index]]));
    const minWeight = edges.reduce((min, edge) => Math.min(min, Math.abs(edge.weight)), Infinity), maxWeight = edges.reduce((max, edge) => Math.max(max, Math.abs(edge.weight)), 0);
    const fraction = (value, min, max) => max > min ? Math.max(0, Math.min(1, (value - min) / (max - min))) : 0;
    return {
        node: node => ({ color: colors[node.id], label: communityLabel(node.id),
            size: nodeSizes.get(node.id) ?? a.nodeMin }),
        edge: edge => {
            const size = a.edgeSizeMode === "uniform" ? a.edgeWidth
                : a.edgeMin + (a.edgeMax - a.edgeMin) * Math.sqrt(fraction(Math.abs(edge.weight), minWeight, maxWeight));
            return { color: a.edgeColorMode === "single" ? a.edgeColor
                : a.edgeColorMode === "average" ? communityAverageColor(colors[edge.source], colors[edge.target])
                : a.edgeColorMode === "source" ? colors[edge.source] : a.edgeColorMode === "target" ? colors[edge.target] : "#8c96a5",
                size, hidden: size === 0 };
        },
    };
}

function communityNodeLabelColor(node) {
    const a = COMMUNITY_EXPLORER.appearance;
    return a.labelMode === "node" ? node.color : a.labelMode === "single" ? a.labelColor
        : COMMUNITY_EXPLORER.rendererType === "sigma" ? "#000000" : "#ffffff";
}

function communityAverageColor(source, target) {
    return "#" + [1, 3, 5].map(offset => Math.round((parseInt(source.slice(offset, offset + 2), 16)
        + parseInt(target.slice(offset, offset + 2), 16)) / 2).toString(16).padStart(2, "0")).join("");
}

function updateCommunityGraphLegend() {
    const e = COMMUNITY_EXPLORER;
    $("explorer-legend").textContent = "Node size uses " + (e.appearance.nodeScale === "percentile" ? "percentile ranks" : "proportional scaling")
        + " of member count. Node colour matches the partition in Network. "
        + (e.appearance.edgeSizeMode === "uniform" ? "Edges have uniform width. " : "Link width scales with "
            + (e.mode === "weighted" ? "absolute aggregate weight; values retain their sign. " : "RELISON generator multiplicity (not original parallel-edge counts). "))
        + (state.directed ? "Arrows show direction." : "");
}

function communityPager(id, total, page, change) {
    const el = $(id), pages = Math.max(1, Math.ceil(total / COMMUNITY_PAGE_SIZE));
    el.replaceChildren();
    const prev = document.createElement("button"), next = document.createElement("button"), note = document.createElement("span");
    prev.textContent = "Previous"; next.textContent = "Next";
    prev.disabled = page === 0; next.disabled = page >= pages - 1;
    note.textContent = total + " rows · Page " + (page + 1) + " of " + pages;
    prev.onclick = () => change(page - 1); next.onclick = () => change(page + 1);
    el.append(prev, note, next);
}

function renderCommunityTable() {
    const e = COMMUNITY_EXPLORER, rows = communitySortedRows(), colors = communityColors();
    const columns = [{ key: "id", label: "ID" }, { key: "label", label: "Label" }, { key: "size", label: "Members" },
        { key: "share", label: "Network share (%)" }, { key: "notes", label: "Notes" }, ...communityMetricColumns()];
    e.page = Math.min(e.page, Math.max(0, Math.ceil(rows.length / COMMUNITY_PAGE_SIZE) - 1));
    const table = $("community-table");
    table.innerHTML = "<thead><tr><th>Colour</th>" + columns.map(col => '<th aria-sort="' + (col.key === e.sort ? e.descending ? "descending" : "ascending" : "none")
        + '"><button type="button" data-community-sort="' + esc(col.key) + '">' + esc(col.label) + (col.key === e.sort ? e.descending ? " ▼" : " ▲" : "") + "</button></th>").join("") + "</tr></thead><tbody>"
        + rows.slice(e.page * COMMUNITY_PAGE_SIZE, (e.page + 1) * COMMUNITY_PAGE_SIZE).map(row => '<tr class="' + (e.selected[e.partition] === row.id ? "community-selected" : "")
            + '"><td><input type="color" class="partition-color-input" data-community-color="' + esc(row.id)
                + '" value="' + toHexColor(colors[row.id]) + '" aria-label="Colour for ' + esc(row.label) + '"></td>' + columns.map(col => {
                const value = communityValue(row, col.key);
                return "<td>" + (col.key === "id" ? '<button data-community-id="' + esc(row.id) + '">' + esc(row.id) + "</button>"
                    : col.key === "label" ? '<button data-community-id="' + esc(row.id) + '">' + esc(row.label) + "</button>"
                    : col.key === "notes" ? '<input class="community-note-input" type="text" maxlength="2000" data-community-note="' + esc(row.id)
                        + '" aria-label="Notes for ' + esc(row.label) + '" placeholder="Add note" value="' + esc(value) + '">'
                    : esc(value == null ? "—" : typeof value === "number" ? fmt(value) : String(value))) + "</td>";
            }).join("") + "</tr>").join("") + (rows.length ? "" : '<tr><td colspan="' + (columns.length + 1) + '">No matching communities.</td></tr>') + "</tbody>";
    table.querySelectorAll("[data-community-id]").forEach(button => button.onclick = () => selectCommunity(button.dataset.communityId));
    table.querySelectorAll("[data-community-color]").forEach(input => input.oninput = () =>
        setCategoricalColor("community:" + e.partition, input.dataset.communityColor, input.value));
    table.querySelectorAll("[data-community-sort]").forEach(button => button.onclick = () => {
        e.descending = e.sort === button.dataset.communitySort ? !e.descending : false;
        e.sort = button.dataset.communitySort; e.page = 0; renderCommunityTable();
    });
    table.querySelectorAll("[data-community-note]").forEach(input => input.oninput = () => {
        const id = input.dataset.communityNote, note = input.value.trim();
        if (!Object.hasOwn(e.notes, e.partition)) e.notes[e.partition] = {};
        if (note) e.notes[e.partition][id] = note;
        else delete e.notes[e.partition][id];
    });
    communityPager("community-table-pager", rows.length, e.page, page => { e.page = page; renderCommunityTable(); });
}

function selectCommunity(id) {
    const e = COMMUNITY_EXPLORER;
    e.selected[e.partition] = String(id); e.memberPage = 0; e.memberSearch = "";
    renderCommunityTable(); renderCommunityInspector(); updateCommunityGraphAppearance();
}

function communityMembers() {
    const e = COMMUNITY_EXPLORER, row = communityRows().find(row => row.id === e.selected[e.partition]);
    const query = e.memberSearch.toLowerCase();
    const columns = nodeColumns().filter(col => col.key === "id" || col.key.startsWith("a:"));
    return (row?.members || []).filter(node => columns.some(col => String(nodeRowValue(node, col.key) ?? "").toLowerCase().includes(query))
        || String(nodeRowValue(node, e.memberColumn) ?? "").toLowerCase().includes(query))
        .sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));
}

function renderCommunityMembers() {
    const e = COMMUNITY_EXPLORER, rows = communityMembers();
    e.memberPage = Math.min(e.memberPage, Math.max(0, Math.ceil(rows.length / COMMUNITY_PAGE_SIZE) - 1));
    const extra = nodeColumns().find(col => col.key === e.memberColumn && col.key !== "id");
    $("community-members").innerHTML = "<thead><tr><th>Node ID</th>" + (extra ? "<th>" + esc(extra.label) + "</th>" : "") + "</tr></thead><tbody>"
        + rows.slice(e.memberPage * COMMUNITY_PAGE_SIZE, (e.memberPage + 1) * COMMUNITY_PAGE_SIZE).map(node => '<tr><td><button data-member="' + esc(node) + '">' + esc(node) + "</button></td>"
            + (extra ? "<td>" + esc(String(nodeRowValue(node, extra.key) ?? "—")) + "</td>" : "") + "</tr>").join("")
        + (rows.length ? "" : '<tr><td colspan="2">No matching members.</td></tr>') + "</tbody>";
    $("community-members").querySelectorAll("[data-member]").forEach(button => button.onclick = () => {
        setSelectionMode("highlight"); selectNode(button.dataset.member); switchTab("network");
    });
    communityPager("community-members-pager", rows.length, e.memberPage, page => { e.memberPage = page; renderCommunityMembers(); });
}

function renderCommunityInspector() {
    const e = COMMUNITY_EXPLORER, row = communityRows().find(row => row.id === e.selected[e.partition]);
    $("community-inspector").hidden = !row;
    $("community-workspace").classList.toggle("has-inspector", !!row);
    if (!row) return;
    $("community-inspector-title").textContent = row.label;
    $("community-inspector-identity").textContent = "ID " + row.id + " · " + e.partition;
    $("community-label").value = row.label;
    const details = [["Members", row.size], ["Network share (%)", row.share], ...communityMetricColumns().map(col => [col.label, communityValue(row, col.key)])];
    $("community-details").innerHTML = "<tbody>" + details.map(([name, value]) => "<tr><th>" + esc(name) + "</th><td>" + esc(value == null ? "—" : fmt(value)) + "</td></tr>").join("") + "</tbody>";
    $("community-members-search").value = e.memberSearch;
    const columns = nodeColumns().filter(col => col.key === "id" || col.key.startsWith("a:") || col.key.startsWith("m:"));
    if (!columns.some(col => col.key === e.memberColumn)) e.memberColumn = "id";
    $("community-member-column").replaceChildren(...columns.map(col => option(col.key, col.key === "id" ? "None" : col.label)));
    $("community-member-column").value = e.memberColumn;
    renderCommunityMembers();
    const connections = (e.data?.edges || []).filter(edge => edge.source === row.id || edge.target === row.id);
    $("community-connections-note").textContent = e.data ? (e.mode === "weighted" ? "Aggregate weights" : "RELISON generator edge multiplicities") + " · includes internal connections."
        : "Connections will appear when the community graph is loaded.";
    $("community-connections").innerHTML = "<thead><tr><th>Community</th><th>Direction</th><th>Value</th></tr></thead><tbody>"
        + connections.map(edge => {
            const other = edge.source === row.id ? edge.target : edge.source;
            return '<tr><td><button data-neighbour="' + esc(other) + '">' + esc(communityLabel(other)) + "</button></td><td>"
                + (edge.source === edge.target ? "Internal" : !e.data.directed ? "Undirected" : edge.source === row.id ? "Outgoing" : "Incoming")
                + "</td><td>" + esc(fmt(edge.weight)) + "</td></tr>";
        }).join("") + (connections.length ? "" : '<tr><td colspan="3">No connections.</td></tr>') + "</tbody>";
    $("community-connections").querySelectorAll("[data-neighbour]").forEach(button => button.onclick = () => selectCommunity(button.dataset.neighbour));
}

function refreshCommunityExplorer() {
    const e = COMMUNITY_EXPLORER;
    const detectedPartitions = Object.keys(state.communityData).filter(partition => !isAttributeCommunityPartition(partition));
    const attributeDefinitions = nodeAttrDefs().filter(def => !def.numeric);
    const attributePartitions = attributeDefinitions.map(def => ({ key: attributeCommunityPartitionKey(def.name), label: def.name }));
    const partitions = [...detectedPartitions, ...attributePartitions.map(partition => partition.key)];
    if (!partitions.includes(e.partition)) e.partition = partitions[0] || "";
    if (isAttributeCommunityPartition(e.partition)) ensureAttributeCommunityPartition(e.partition);
    if ((e.sort.startsWith("metric:") || e.sort.startsWith("attribute-metric:"))
        && !communityMetricColumns().some(col => col.key === e.sort)) { e.sort = "size"; e.descending = true; }
    const partitionSelect = $("explorer-partition");
    const groups = [];
    if (detectedPartitions.length) {
        const group = document.createElement("optgroup"); group.label = "Detected partitions";
        detectedPartitions.forEach(name => group.appendChild(option(name, name)));
        groups.push(group);
    }
    if (attributePartitions.length) {
        const group = document.createElement("optgroup"); group.label = "Node attributes";
        attributePartitions.forEach(({ key, label }) => group.appendChild(option(key, "Attribute · " + label)));
        groups.push(group);
    }
    partitionSelect.replaceChildren(...groups);
    partitionSelect.value = e.partition;
    partitionSelect.disabled = $("explorer-metrics").disabled = !partitions.length;
    $("explorer-content").hidden = !!(!partitions.length);
    $("explorer-empty").hidden = !!partitions.length;
    const rows = communityRows(), assigned = rows.reduce((sum, row) => sum + row.size, 0);
    $("explorer-summary").textContent = partitions.length ? rows.length + " communities · " + assigned + " members · Whole network"
        + (assigned < (state.graph?.order || 0) ? " · " + (state.graph.order - assigned) + " unassigned nodes" : "") : "";
    const suffix = " (" + e.partition + ")";
    const globals = Object.entries(state.graphMetrics).filter(([label]) => e.partition && label.endsWith(suffix));
    $("explorer-partition-metrics").hidden = !globals.length;
    $("explorer-partition-metrics").textContent = globals.map(([label, value]) => label.slice(0, -suffix.length) + ": " + fmt(value)).join(" · ");
    $("explorer-mode").value = e.mode; $("explorer-layout").value = e.layout; $("explorer-loops").checked = e.loops;
    $("explorer-renderer").value = e.rendererType === "sigma" ? "sigma" : "cosmograph";
    if (e.rendererType !== "sigma") e.cosmographDimensions = e.rendererType === "cosmograph-3d" ? 3 : 2;
    $("explorer-dimensions").value = String(e.cosmographDimensions);
    $("explorer-dimensions-field").hidden = e.rendererType === "sigma";
    $("explorer-layout").replaceChildren(...COMMUNITY_LAYOUTS.map(layout => option(layout.id,
        e.rendererType === "cosmograph-3d" ? layout.spatialLabel || layout.label + " 3D" : layout.label)));
    $("explorer-layout").value = e.layout;
    syncCommunityAppearanceControls();
    for (const view of ["graph", "table"]) {
        $("explorer-" + view + "-view").hidden = e.view !== view;
        $("explorer-view-" + view).classList.toggle("active", e.view === view);
        $("explorer-view-" + view).setAttribute("aria-pressed", String(e.view === view));
    }
    updateCommunityGraphLegend();
    if (state.activeTab !== "communities" || !partitions.length) return;
    renderCommunityTable(); renderCommunityInspector();
    loadCommunityGraph();
}

function communityGraphKey() {
    const e = COMMUNITY_EXPLORER;
    return JSON.stringify([state.graphId, e.partition, e.mode, communityLayoutId()]);
}

function communityLayoutId() {
    const e = COMMUNITY_EXPLORER;
    const layout = COMMUNITY_LAYOUTS.find(layout => layout.id === e.layout) || COMMUNITY_LAYOUTS[0];
    return e.rendererType === "cosmograph-3d" ? layout.spatial : layout.id;
}

function setCommunityRendererFromControls() {
    const e = COMMUNITY_EXPLORER;
    leaveCommunityExplorer();
    e.cosmographDimensions = $("explorer-dimensions").value === "3" ? 3 : 2;
    e.rendererType = $("explorer-renderer").value === "sigma" ? "sigma" : e.cosmographDimensions === 3 ? "cosmograph-3d" : "cosmograph";
    refreshCommunityExplorer();
}

function leaveCommunityExplorer() {
    const e = COMMUNITY_EXPLORER;
    cancelCommunityGraphRender();
    if (e.renderer) {
        const camera = e.renderer.getCamera()?.getState?.();
        if (camera) e.cameras[JSON.stringify([e.partition, e.rendererType])] = camera;
        const renderer = e.renderer;
        e.renderer = null;
        renderer.kill();
    }
}

function cancelCommunityGraphRender() {
    const e = COMMUNITY_EXPLORER;
    e.rendering++;
    e.renderController?.abort();
    e.renderController = null;
    e.renderPending = false;
    e.hoveredNode = null;
}

// Dynamic imports cannot be cancelled, but an obsolete import must not hold the view open.
function communityRenderAwait(promise, signal) {
    return new Promise((resolve, reject) => {
        const aborted = () => reject(signal.reason);
        signal.addEventListener("abort", aborted, { once: true });
        if (signal.aborted) aborted();
        Promise.resolve(promise).then(value => {
            signal.removeEventListener("abort", aborted);
            if (signal.aborted) reject(signal.reason);
            else resolve(value);
        }, error => { signal.removeEventListener("abort", aborted); reject(error); });
    });
}

async function loadCommunityGraph(force = false) {
    const e = COMMUNITY_EXPLORER, key = communityGraphKey();
    if (!e.partition || !state.graph) return;
    if (!force && e.dataKey === key) {
        if (e.view === "graph") { renderCommunityGraph(); }
        return;
    }
    if (!force && e.loadingKey === key) return;
    e.controller?.abort(); e.controller = new AbortController();
    const request = ++e.request, graph = state.graph, assignment = state.communityData[e.partition];
    e.loadingKey = key; e.dataKey = ""; e.data = null;
    leaveCommunityExplorer(); e.graph = null;
    if (force) delete e.cameras[JSON.stringify([e.partition, e.rendererType])];
    $("community-loops").replaceChildren();
    $("community-foreground").replaceChildren();
    $("explorer-edge-info").textContent = "Select a community or connection to inspect it.";
    $("explorer-graph-status").textContent = "Loading community graph…";
    renderCommunityInspector();
    try {
        const attribute = e.metadata[e.partition]?.attribute;
        const data = await api("/api/communities/graph", { ...jsonBody({ graphId: state.graphId, algorithm: e.partition,
            ...(attribute ? { attribute } : {}), mode: e.mode, layout: communityLayoutId() }), signal: e.controller.signal });
        if (request !== e.request || key !== communityGraphKey() || graph !== state.graph || assignment !== state.communityData[e.partition]) return;
        e.data = data; e.dataKey = key;
        if (!Object.hasOwn(e.positions, e.partition)) e.positions[e.partition] = {};
        const savedPositions = e.positions[e.partition][communityLayoutId()];
        if (!savedPositions || data.nodes.some(node => !Number.isFinite(savedPositions[node.id]?.x) || !Number.isFinite(savedPositions[node.id]?.y)
            || e.rendererType === "cosmograph-3d" && !Number.isFinite(savedPositions[node.id]?.z)))
            e.positions[e.partition][communityLayoutId()] = data.positions;
        $("explorer-graph-status").textContent = "";
        if (state.activeTab === "communities") { renderCommunityInspector(); if (e.view === "graph") renderCommunityGraph(); }
    } catch (error) {
        if (request === e.request && error.name !== "AbortError") $("explorer-graph-status").textContent = error.message + " Use Apply layout to retry.";
    } finally {
        if (request === e.request) e.loadingKey = "";
    }
}

async function renderCommunityGraph() {
    const e = COMMUNITY_EXPLORER;
    if (!e.data || e.view !== "graph" || state.activeTab !== "communities") return;
    if (!e.data.nodes.length) { $("explorer-graph-status").textContent = "This partition contains no communities."; return; }
    // Retain the renderer/camera across inspector changes and metric refreshes.
    if (e.renderer) { updateCommunityGraphAppearance(); requestAnimationFrame(() => e.renderer?.refresh()); return; }
    if (e.renderPending) return;
    const rendering = ++e.rendering, dataKey = e.dataKey;
    const controller = new AbortController();
    e.renderController = controller;
    e.renderPending = true;
    const timeout = e.rendererType === "sigma" ? null : setTimeout(() => controller.abort(
        new Error("Cosmograph loading timed out after 30 seconds")), 30000);
    try {
        const styles = communityGraphStyles(), graph = new Graph({ type: e.data.directed ? "directed" : "undirected", multi: false, allowSelfLoops: true });
        const positions = e.positions[e.partition]?.[communityLayoutId()] || e.data.positions;
        for (const node of e.data.nodes) graph.addNode(node.id, { ...(positions[node.id] || e.data.positions[node.id]), ...styles.node(node) });
        for (const [index, edge] of e.data.edges.entries()) {
            if (edge.source === edge.target) continue; // Loops use an SVG overlay, including when the curve extension is unavailable.
            graph.addEdgeWithKey(String(index), edge.source, edge.target, { weight: edge.weight, label: String(edge.weight), ...styles.edge(edge), curvature: 0.2 });
        }
        const curves = window.relisonSigmaEdgePrograms;
        let renderer;
        if (e.rendererType !== "sigma") {
            $("explorer-graph-status").textContent = "Loading Cosmograph…";
            const adapter = await communityRenderAwait(import("./community-cosmograph.js"), controller.signal);
            if (rendering !== e.rendering || dataKey !== e.dataKey) return;
            renderer = await adapter.create(graph, $("community-sigma"), { dimensions: e.rendererType === "cosmograph-3d" ? 3 : 2,
                selected: e.selected[e.partition], labelColorByNode: communityNodeLabelColor, labelOptions: () => e.appearance, edgeShape: e.appearance.edgeShape,
                camera: e.cameras[JSON.stringify([e.partition, e.rendererType])], signal: controller.signal });
        } else renderer = new Sigma(graph, $("community-sigma"), {
            defaultEdgeType: communitySigmaEdgeType(),
            edgeProgramClasses: curves || {}, renderEdgeLabels: false, enableEdgeEvents: true,
            renderLabels: false, defaultDrawNodeHover: () => {},
            nodeReducer: (id, attrs) => ({ ...attrs, highlighted: id === e.selected[e.partition] }),
        });
        if (rendering !== e.rendering || dataKey !== e.dataKey || e.view !== "graph" || state.activeTab !== "communities") { renderer.kill(); return; }
        e.renderer = renderer;
        e.graph = graph;
        e.renderer.on("clickNode", ({ node }) => selectCommunity(node));
        e.renderer.on("clickEdge", ({ edge }) => {
            const link = e.data.edges[Number(edge)];
            $("explorer-edge-info").textContent = communityLabel(link.source) + (e.data.directed ? " → " : " ↔ ") + communityLabel(link.target)
                + " · " + (e.mode === "weighted" ? "Aggregate weight: " : "Generator multiplicity: ") + fmt(link.weight);
        });
        e.renderer.on("afterRender", drawCommunityLoops);
        e.renderer.on("enterNode", ({ node }) => {
            if (e.renderer !== renderer) return;
            e.hoveredNode = node; drawCommunityLoops();
        });
        e.renderer.on("leaveNode", ({ node }) => {
            if (e.renderer !== renderer || e.hoveredNode !== node) return;
            e.hoveredNode = null; drawCommunityLoops();
        });
        e.hoveredNode = renderer.getHoveredNode?.() ?? null;
        if (e.rendererType === "sigma") {
            const camera = e.cameras[JSON.stringify([e.partition, e.rendererType])];
            if (camera) e.renderer.getCamera().setState(camera);
        }
        updateCommunityGraphAppearance();
        $("explorer-graph-status").textContent = "";
    } catch (error) {
        if (rendering === e.rendering && error.name !== "AbortError") $("explorer-graph-status").textContent = "Could not display community graph: " + error.message + ". Use Apply layout to retry or choose Sigma in Renderer.";
    } finally {
        clearTimeout(timeout);
        // A cancelled task may settle after its replacement has already started.
        if (e.renderController === controller) {
            e.renderController = null;
            e.renderPending = false;
        }
    }
}

function updateCommunityGraphAppearance({ fit = false } = {}) {
    const e = COMMUNITY_EXPLORER;
    e.appearanceFitPending ||= fit;
    if (!e.graph || !e.renderer) return;
    fit ||= e.appearanceFitPending;
    e.appearanceFitPending = false;
    const styles = communityGraphStyles();
    for (const node of e.data.nodes) {
        const attrs = styles.node(node);
        e.graph.mergeNodeAttributes(node.id, attrs);
    }
    e.graph.forEachEdge(edge => e.graph.mergeEdgeAttributes(edge, styles.edge(e.data.edges[Number(edge)])));
    if (e.rendererType === "sigma") e.renderer.setSetting("labelColor", e.appearance.labelMode === "node" ? { attribute: "color" } : { color: communityNodeLabelColor({}) });
    if (e.rendererType === "sigma") e.renderer.setSetting("defaultEdgeType", communitySigmaEdgeType());
    else e.renderer.setSetting("edgeShape", e.appearance.edgeShape);
    if (e.rendererType !== "sigma") e.renderer.setSetting("selectedNode", e.selected[e.partition]);
    const renderer = e.renderer;
    const result = e.rendererType === "sigma" ? renderer.refresh() : renderer.refresh({ fit });
    return Promise.resolve(result).then(() => {
        if (renderer !== e.renderer) return;
        if (fit && e.rendererType === "sigma") renderer.getCamera().animatedReset();
        drawCommunityLoops();
    });
}

function communityEdgesCurved() {
    const e = COMMUNITY_EXPLORER;
    return e.appearance.edgeShape === "curved" && (e.rendererType !== "sigma" || Boolean(window.relisonSigmaEdgePrograms));
}

function communitySigmaEdgeType() {
    return communityEdgesCurved() ? COMMUNITY_EXPLORER.data.directed ? "curvedArrow" : "curve"
        : COMMUNITY_EXPLORER.data.directed ? "arrow" : "line";
}

function communityEdgeControlPoint(source, target) {
    if (!communityEdgesCurved()) return null;
    const direction = COMMUNITY_EXPLORER.rendererType === "sigma" ? -1 : 1;
    return { x: (source.x + target.x) / 2 - (target.y - source.y) * 0.2 * direction,
        y: (source.y + target.y) / 2 + (target.x - source.x) * 0.2 * direction };
}

function communityLoopMarkup() {
    const e = COMMUNITY_EXPLORER;
    if (!e.loops || !e.renderer || !e.data) return "";
    const styles = communityGraphStyles();
    return e.data.edges.filter(edge => edge.source === edge.target).map(edge => {
        const style = styles.edge(edge);
        if (!style.size) return "";
        const markerId = "community-loop-arrow-" + edge.source;
        const marker = '<defs><marker id="' + markerId + '" markerWidth="5" markerHeight="5" refX="4" refY="2.5" orient="auto"><path d="M0,0 L5,2.5 L0,5" fill="' + style.color + '"/></marker></defs>';
        const point = e.renderer.graphToViewport(e.graph.getNodeAttributes(edge.source));
        const radius = e.renderer.getNodeDisplayData(edge.source)?.size || 10, x = point.x, y = point.y;
        return marker + '<path d="M ' + (x - radius / 2) + " " + (y - radius / 2) + " C " + (x - radius * 4) + " " + (y - radius * 5) + " " + (x + radius * 4) + " " + (y - radius * 5) + " " + (x + radius / 2) + " " + (y - radius / 2)
            + '" fill="none" stroke="' + style.color + '" stroke-width="' + style.size + '"' + (e.data.directed ? ' marker-end="url(#' + markerId + ')"' : "") + "><title>" + esc(communityLabel(edge.source) + ": " + edge.weight) + "</title></path>";
    }).join("");
}

function drawCommunityLoops() {
    const svg = $("community-loops"), stage = $("community-graph-stage");
    svg.setAttribute("viewBox", "0 0 " + stage.clientWidth + " " + stage.clientHeight);
    svg.innerHTML = communityLoopMarkup();
    const foreground = $("community-foreground");
    foreground.setAttribute("viewBox", "0 0 " + stage.clientWidth + " " + stage.clientHeight);
    foreground.innerHTML = communityForegroundMarkup();
}

// Shared SVG foreground keeps borders and text above every WebGL node, link and loop.
function communityForegroundMarkup({ selection = true } = {}) {
    const e = COMMUNITY_EXPLORER, a = e.appearance;
    if (!e.graph || !e.renderer || !e.data) return "";
    const points = new Map(), borders = [], edgeLabels = [], nodeLabels = [], hoverLabels = [];
    const visibleLabels = e.rendererType === "sigma" ? null : e.renderer.getVisibleLabelNodes?.() || new Set();
    const width = $("community-graph-stage").clientWidth, height = $("community-graph-stage").clientHeight;
    e.graph.forEachNode((id, attrs) => {
        const point = e.renderer.graphToViewport(attrs), radius = e.renderer.getNodeDisplayData(id)?.size || attrs.size;
        if (!Number.isFinite(point.x) || !Number.isFinite(point.y)) return;
        points.set(id, { ...point, radius });
        if (point.x + radius < 0 || point.x - radius > width || point.y + radius < 0 || point.y - radius > height) return;
        const circle = (r, color, lineWidth) => '<circle cx="' + point.x + '" cy="' + point.y + '" r="' + r
            + '" fill="none" stroke="' + color + '" stroke-width="' + lineWidth + '"/>';
        if (a.nodeBorderOn && a.nodeBorderWidth) borders.push(circle(radius + a.nodeBorderWidth / 2, a.nodeBorderColor, a.nodeBorderWidth));
        if (selection && e.selected[e.partition] === id) borders.push(circle(radius + (a.nodeBorderOn ? a.nodeBorderWidth : 0) + 3,
            getComputedStyle(document.body).getPropertyValue("--accent").trim(), 2));
        const hovered = id === e.hoveredNode;
        if (hovered || a.labelShow && (visibleLabels === null || visibleLabels.has(id))) {
            const size = a.labelProp ? Math.max(6, radius * a.labelSize / 8) : a.labelSize;
            (hovered ? hoverLabels : nodeLabels).push(communityLabelMarkup(attrs.label, point.x, point.y - radius - (a.nodeBorderOn ? a.nodeBorderWidth : 0) - 4,
                size, a.labelFont, communityNodeLabelColor(attrs)));
        }
    });
    if (a.edgeLabelShow) {
        const styles = communityGraphStyles();
        for (const edge of e.data.edges) {
            const source = points.get(edge.source), target = points.get(edge.target), style = styles.edge(edge);
            if (!source || !target || style.hidden || edge.source === edge.target && !e.loops) continue;
            const control = communityEdgeControlPoint(source, target);
            const x = control ? (source.x + 2 * control.x + target.x) / 4 : (source.x + target.x) / 2;
            const y = edge.source === edge.target ? source.y - source.radius * 4
                : (control ? (source.y + 2 * control.y + target.y) / 4 : (source.y + target.y) / 2) - 4;
            const size = a.edgeLabelProp ? Math.max(5, style.size * a.edgeLabelSize / 2) : a.edgeLabelSize;
            const color = a.edgeLabelMode === "edge" ? style.color : a.edgeLabelMode === "single" ? a.edgeLabelColor
                : e.rendererType === "sigma" ? "#000000" : "#ffffff";
            edgeLabels.push(communityLabelMarkup(String(edge.weight), x, y, size, a.edgeLabelFont, color));
        }
    }
    return '<g class="community-node-borders">' + borders.join("") + '</g><g class="community-edge-labels">' + edgeLabels.join("")
        + '</g><g class="community-node-labels">' + nodeLabels.join("") + hoverLabels.join("") + '</g>';
}

function communityLabelMarkup(label, x, y, size, font, color) {
    const text = '<text x="' + x + '" y="' + y + '" text-anchor="middle" fill="' + color + '" font-size="' + size
        + '" font-family="' + esc(font) + '">' + esc(label) + '</text>';
    if (COMMUNITY_EXPLORER.rendererType === "sigma") return text;
    const bounds = communityLabelBounds(String(label), size, font), paddingX = 5, paddingY = 3;
    return '<g class="community-label"><rect class="community-label-background" x="' + (x - bounds.width / 2 - paddingX)
        + '" y="' + (y - bounds.ascent - paddingY) + '" width="' + (bounds.width + paddingX * 2)
        + '" height="' + (bounds.ascent + bounds.descent + paddingY * 2)
        + '" rx="3" fill="#232428" fill-opacity="0.92"/>' + text + '</g>';
}

function communityLabelBounds(label, size, font) {
    const key = JSON.stringify([label, size, font]);
    if (COMMUNITY_LABEL_METRICS.has(key)) return COMMUNITY_LABEL_METRICS.get(key);
    if (communityLabelMeasureContext === undefined) communityLabelMeasureContext = document.createElement("canvas").getContext?.("2d") || null;
    const context = communityLabelMeasureContext;
    if (context) context.font = size + "px " + font;
    const measured = context?.measureText(label);
    const bounds = { width: measured?.width ?? Array.from(label).length * size * 0.62,
        ascent: measured?.actualBoundingBoxAscent || size * 0.8, descent: measured?.actualBoundingBoxDescent || size * 0.2 };
    if (COMMUNITY_LABEL_METRICS.size >= 2048) COMMUNITY_LABEL_METRICS.clear();
    COMMUNITY_LABEL_METRICS.set(key, bounds);
    return bounds;
}

function communityToNetwork(only) {
    const e = COMMUNITY_EXPLORER, row = communityRows().find(row => row.id === e.selected[e.partition]);
    if (!row?.members.length) return;
    clearSelection({ refreshTable: false, refreshGraph: false });
    selectNode(row.members[0]);
    setSelectionPartition("community:" + e.partition);
    setSelectionMode(only ? "community-only" : "community-highlight");
    switchTab("network");
    setStatus("Community " + row.id + " selected. Existing network filters still apply.");
}

function communityToMetrics() {
    const partition = COMMUNITY_EXPLORER.partition;
    if (!partition) return;
    const attribute = COMMUNITY_EXPLORER.metadata[partition]?.attribute;
    if (attribute) {
        $("attribute-comm-attribute").value = attribute;
        syncAttributeCommunityMetricControls();
        $("attribute-comm-attribute").value = attribute;
        switchTab("metrics"); switchSubtab("attrcomm");
        return;
    }
    $("indiv-comm-partition").value = $("global-comm-partition").value = partition;
    switchTab("metrics"); switchSubtab("comm");
}

function collectCommunityExplorer() {
    const e = COMMUNITY_EXPLORER;
    const camera = e.renderer?.getCamera()?.getState?.();
    if (camera) e.cameras[JSON.stringify([e.partition, e.rendererType])] = camera;
    return { partition: e.partition, view: e.view, selected: e.selected, labels: e.labels, notes: e.notes, metadata: e.metadata,
        positions: e.positions, mode: e.mode, layout: e.layout, loops: e.loops, rendererType: e.rendererType, cameras: e.cameras,
        appearance: { ...e.appearance }, cosmographDimensions: e.cosmographDimensions };
}

function restoreCommunityExplorer(saved) {
    if (!saved) return;
    const e = COMMUNITY_EXPLORER;
    for (const key of ["selected", "labels", "notes", "metadata", "positions", "cameras"]) if (saved[key] && typeof saved[key] === "object") e[key] = saved[key];
    e.partition = typeof saved.partition === "string" ? saved.partition : "";
    e.view = saved.view === "table" ? "table" : "graph";
    e.mode = ["weighted", "complete", "inter"].includes(saved.mode) ? saved.mode : "weighted";
    e.layout = COMMUNITY_LAYOUTS.some(layout => layout.id === saved.layout) ? saved.layout : "circular";
    e.appearance = normalizeCommunityAppearance(saved.appearance || {});
    e.loops = !!saved.loops;
    e.rendererType = ["cosmograph", "cosmograph-3d", "sigma"].includes(saved.rendererType) ? saved.rendererType : "cosmograph";
    e.cosmographDimensions = saved.cosmographDimensions === 3 || e.rendererType === "cosmograph-3d" ? 3 : 2;
}

async function exportCommunityGraph(png) {
    const e = COMMUNITY_EXPLORER;
    if (!e.renderer || !e.data) return;
    const width = $("community-graph-stage").clientWidth, height = $("community-graph-stage").clientHeight;
    const textColor = getComputedStyle(document.body).getPropertyValue("--text").trim();
    const bg = getComputedStyle(document.body).getPropertyValue("--canvas-bg").trim();
    const points = new Map(e.graph.nodes().map(id => [id, e.renderer.graphToViewport(e.graph.getNodeAttributes(id))]));
    let body = '<defs><clipPath id="community-view-clip"><rect width="' + width + '" height="' + height + '"/></clipPath></defs>'
        + '<rect width="100%" height="100%" fill="' + bg + '"/><g clip-path="url(#community-view-clip)">';
    e.graph.forEachEdge((edge, attrs, source, target) => {
        if (attrs.hidden || !attrs.size) return;
        const markerId = "community-export-arrow-" + edge;
        body += '<defs><marker id="' + markerId + '" markerWidth="6" markerHeight="6" refX="6" refY="3" orient="auto"><path d="M0,0 L6,3 L0,6" fill="' + attrs.color + '"/></marker></defs>';
        const a = points.get(source), b = points.get(target), dx = b.x - a.x, dy = b.y - a.y;
        const length = Math.hypot(dx, dy) || 1, radius = e.renderer.getNodeDisplayData(target)?.size || 5;
        const control = communityEdgeControlPoint(a, b);
        body += '<path d="M' + a.x + " " + a.y + (control ? " Q" + control.x + " " + control.y : " L")
            + " " + (b.x - dx / length * radius) + " " + (b.y - dy / length * radius)
            + '" fill="none" stroke="' + attrs.color + '" stroke-width="' + attrs.size + '"' + (e.data.directed ? ' marker-end="url(#' + markerId + ')"' : "") + "/>";
    });
    body += communityLoopMarkup();
    e.graph.forEachNode((id, attrs) => {
        const p = points.get(id), radius = e.renderer.getNodeDisplayData(id)?.size || attrs.size;
        body += '<circle cx="' + p.x + '" cy="' + p.y + '" r="' + radius + '" fill="' + attrs.color + '"/>';
    });
    body += communityForegroundMarkup({ selection: false });
    body += '</g><text x="12" y="' + (height + 20) + '" fill="' + textColor + '" font-size="12" font-family="sans-serif">' + esc(e.partition + " · Whole network") + "</text>"
        + '<text x="12" y="' + (height + 40) + '" fill="' + textColor + '" font-size="11" font-family="sans-serif">'
        + esc("Node size: members · Link width: " + (e.appearance.edgeSizeMode === "uniform" ? "uniform" : e.mode === "weighted" ? "aggregate weights" : "generator multiplicities")) + "</text>";
    const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + width + '" height="' + (height + 55) + '" viewBox="0 0 ' + width + " " + (height + 55) + '">' + body + "</svg>";
    if (!png) { download("community-graph.svg", svg, "image/svg+xml"); return; }
    const url = URL.createObjectURL(new Blob([svg], { type: "image/svg+xml" }));
    try {
        const img = new Image();
        await new Promise((resolve, reject) => { img.onload = resolve; img.onerror = reject; img.src = url; });
        const canvas = document.createElement("canvas"); canvas.width = width * 2; canvas.height = (height + 55) * 2;
        const ctx = canvas.getContext("2d"); ctx.scale(2, 2); ctx.drawImage(img, 0, 0);
        const blob = await new Promise(resolve => canvas.toBlob(resolve, "image/png"));
        if (!blob) throw new Error("PNG encoding failed.");
        download("community-graph.png", blob, "image/png");
    } catch (error) { setStatus("Community export failed: " + (error.message || "Could not render PNG"), "error"); }
    finally { URL.revokeObjectURL(url); }
}

document.addEventListener("DOMContentLoaded", () => {
    const e = COMMUNITY_EXPLORER;
    for (const id of ["community-label-font", "community-edge-label-font"])
        $(id).replaceChildren(...COMMUNITY_LABEL_FONTS.map(font => option(font.value, font.label)));
    syncCommunityAppearanceControls();
    for (const id of Object.values(COMMUNITY_APPEARANCE_CONTROLS)) $(id).oninput = applyCommunityAppearanceControls;
    $("explorer-partition").onchange = () => selectCommunityPartition($("explorer-partition").value);
    for (const view of ["graph", "table"]) $("explorer-view-" + view).onclick = () => { if (view === "table") leaveCommunityExplorer(); e.view = view; refreshCommunityExplorer(); };
    $("explorer-renderer").onchange = $("explorer-dimensions").onchange = setCommunityRendererFromControls;
    $("explorer-mode").onchange = () => { e.mode = $("explorer-mode").value; refreshCommunityExplorer(); };
    $("explorer-apply-layout").onclick = () => {
        e.layout = $("explorer-layout").value;
        if (e.positions[e.partition]) delete e.positions[e.partition][communityLayoutId()];
        loadCommunityGraph(true);
    };
    $("explorer-loops").onchange = () => { e.loops = $("explorer-loops").checked; drawCommunityLoops(); };
    $("explorer-fit").onclick = () => e.renderer?.getCamera().animatedReset();
    $("explorer-zoom-in").onclick = () => e.renderer?.getCamera().animatedZoom({ factor: 1.25 });
    $("explorer-zoom-out").onclick = () => e.renderer?.getCamera().animatedZoom({ factor: 0.8 });
    $("explorer-search").oninput = () => { e.search = $("explorer-search").value; e.page = 0; renderCommunityTable(); };
    $("explorer-close-inspector").onclick = () => { delete e.selected[e.partition]; renderCommunityInspector(); renderCommunityTable(); updateCommunityGraphAppearance(); };
    $("community-save-label").onclick = async () => {
        const id = e.selected[e.partition]; if (id == null) return;
        const metadata = e.metadata[e.partition];
        if (metadata?.attribute) {
            const label = $("community-label").value.trim().slice(0, 120);
            if (!label) { setStatus("An attribute value cannot be empty.", "error"); return; }
            const row = communityRows().find(item => String(item.id) === String(id));
            if (!row) return;
            const previousValue = nodeAttrVal(row.members[0], metadata.attribute);
            const proposed = {};
            applyLocalAttr(proposed, metadata.attribute, label, nodeAttrDefs());
            const value = proposed[metadata.attribute];
            const members = new Set(row.members);
            const merged = state.graph.nodes().some(node => !members.has(node) && nodeAttrVal(node, metadata.attribute) === value);
            if (merged && !confirmAttributeMetricRemoval("node", metadata.attribute, "Merging these node attribute groups")) return;
            if (!merged && previousValue == null && !confirmAttributeMetricRemoval("node", metadata.attribute, "Renaming missing node attribute values", true)) return;
            const graphId = state.graphId, partition = e.partition;
            try {
                await api("/api/graph/" + graphId + "/attributes/nodes/value", jsonBody({
                    nodes: row.members, name: metadata.attribute, value: label,
                }));
                if (graphId !== state.graphId || partition !== e.partition) return;
                row.members.forEach(node => {
                    if (!state.graph?.hasNode(node)) return;
                    const attrs = Object.assign({}, state.graph.getNodeAttribute(node, "attrs") || {});
                    applyLocalAttr(attrs, metadata.attribute, label, nodeAttrDefs());
                    state.graph.setNodeAttribute(node, "attrs", attrs);
                });
                state.networkRevision++;
                updateNodeAttributeMetricLabels(metadata.attribute, previousValue, value, merged);
                delete e.labels[partition];
                delete state.communityData[partition];
                delete e.metadata[partition];
                ensureAttributeCommunityPartition(partition);
                const updatedId = state.communityData[partition]?.[row.members[0]];
                if (updatedId != null) e.selected[partition] = String(updatedId);
                e.page = e.memberPage = 0;
                rebuildAppearanceOptions();
                applyAppearance();
                refreshAfterCompute();
                if (state.activeTab === "tables") renderTable(state.activeTableSubtab);
                setStatus("Updated attribute " + metadata.attribute + " for " + row.members.length + " nodes.");
            } catch (error) { setStatus(error.message, "error"); }
            return;
        }
        if (!Object.hasOwn(e.labels, e.partition)) e.labels[e.partition] = {};
        e.labels[e.partition][id] = $("community-label").value.trim().slice(0, 120) || "Community " + id;
        refreshCommunityLegend(e.partition);
        renderCommunityTable(); renderCommunityInspector(); updateCommunityGraphAppearance({ fit: true });
        if (state.activeTab === "metrics" && state.activeSubtab === "comm") drawCommChart();
        if (state.activeTab === "metrics" && state.activeSubtab === "relationships") renderAttributeRelationships();
    };
    $("community-members-search").oninput = () => { e.memberSearch = $("community-members-search").value; e.memberPage = 0; renderCommunityMembers(); };
    $("community-member-column").onchange = () => { e.memberColumn = $("community-member-column").value; renderCommunityMembers(); };
    $("community-highlight").onclick = () => communityToNetwork(false);
    $("community-isolate").onclick = () => communityToNetwork(true);
    $("explorer-metrics").onclick = communityToMetrics;
    $("explorer-export-table").onclick = () => {
        const cols = [{ key: "id", label: "ID" }, { key: "label", label: "Label" }, { key: "size", label: "Members" },
            { key: "share", label: "Network share (%)" }, { key: "notes", label: "Notes" }, ...communityMetricColumns()];
        const rows = communitySortedRows();
        download("communities.csv", buildCsv(cols, rows, communityValue), "text/csv");
    };
    $("community-export-members").onclick = () => {
        const columns = nodeColumns().filter(col => col.key === "id" || col.key.startsWith("a:") || col.key.startsWith("m:"));
        download("community-" + e.selected[e.partition] + "-members.csv", buildCsv(columns, communityMembers(), nodeRowValue), "text/csv");
    };
    $("explorer-export-svg").onclick = () => exportCommunityGraph(false);
    $("explorer-export-png").onclick = () => exportCommunityGraph(true);
    window.addEventListener("resize", () => { if (state.activeTab === "communities" && e.view === "graph") e.renderer?.refresh(); });
    window.addEventListener("relison-sigma-edge-curve-ready", () => {
        if (!e.renderer || e.rendererType !== "sigma") return;
        // Register the new programs even in straight mode so a later switch to curved works immediately.
        leaveCommunityExplorer(); e.graph = null; renderCommunityGraph();
    });
});
