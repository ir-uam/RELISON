"use strict";

const EDGE_PARTITION_VIEW = { partitions: {}, data: null, selected: null, request: 0,
    page: 0, memberPage: 0, sort: "size", descending: true, memberColumn: "", attributeLoads: new Map(), choiceRequest: 0 };

function edgeAttributeChoice(name) { return "attribute:" + encodeURIComponent(name); }

function isDirectEdgeAttributePartition(data) {
    return data?.type === "edge-attribute" && data.configuration?.binWidth == null;
}

function isComputedEdgePartition(data) {
    return !!data && !isDirectEdgeAttributePartition(data);
}

function computedEdgePartitions() {
    return Object.values(EDGE_PARTITION_VIEW.partitions).filter(isComputedEdgePartition);
}

function edgePartitionChoice(data) {
    return isDirectEdgeAttributePartition(data) ? edgeAttributeChoice(data.configuration.attribute) : data.partitionId;
}

function edgePartitionDataForChoice(choice) {
    return EDGE_PARTITION_VIEW.partitions[choice] || Object.values(EDGE_PARTITION_VIEW.partitions)
        .find(data => isDirectEdgeAttributePartition(data) && edgePartitionChoice(data) === choice);
}

function fillEdgePartitionChoices(id, includeAttributes = false) {
    const select = $(id), current = select.value, groups = [];
    const detected = computedEdgePartitions();
    const attributes = includeAttributes ? edgeAttrDefs().filter(def => !def.numeric) : [];
    if (detected.length) {
        const group = document.createElement("optgroup"); group.label = "Detected partitions";
        detected.forEach(data => group.appendChild(option(data.partitionId, data.label))); groups.push(group);
    }
    if (attributes.length) {
        const group = document.createElement("optgroup"); group.label = "Edge attributes";
        attributes.forEach(def => group.appendChild(option(edgeAttributeChoice(def.name), "Attribute · " + def.name))); groups.push(group);
    }
    select.replaceChildren(...groups);
    if ([...select.options].some(item => item.value === current)) select.value = current;
    select.disabled = !select.options.length;
}

async function resolveEdgePartitionChoice(choice) {
    const existing = edgePartitionDataForChoice(choice);
    if (existing) return existing;
    if (!choice?.startsWith("attribute:") || !state.graphId) return null;
    const e = EDGE_PARTITION_VIEW;
    if (e.attributeLoads.has(choice)) return e.attributeLoads.get(choice);
    const attribute = decodeURIComponent(choice.slice("attribute:".length));
    if (!edgeAttrDefs().some(def => def.name === attribute && !def.numeric)) return null;
    const graphId = state.graphId, graph = state.graph, revision = state.networkRevision;
    const configuration = { graphId, type: "edge-attribute", attribute };
    const pending = (async () => {
        const result = await api("/api/edge-partitions", jsonBody(configuration));
        if (result.cancelled) { setEdgePartitionStatus("Cancelled."); return null; }
        if (graphId !== state.graphId || graph !== state.graph || revision !== state.networkRevision) return null;
        result.configuration = configuration; result.globalMetrics = {}; result.individualMetrics = {};
        e.partitions[result.partitionId] = result;
        syncEdgePartitionControls();
        return result;
    })();
    e.attributeLoads.set(choice, pending);
    try { return await pending; }
    finally { if (e.attributeLoads.get(choice) === pending) e.attributeLoads.delete(choice); }
}

async function selectEdgePartitionChoice(choice) {
    const request = ++EDGE_PARTITION_VIEW.choiceRequest;
    try {
        if (!edgePartitionDataForChoice(choice)) setEdgePartitionStatus("Loading edge attribute groups…", "busy");
        const data = await resolveEdgePartitionChoice(choice);
        if (data && request === EDGE_PARTITION_VIEW.choiceRequest) {
            selectEdgePartitionResult(data.partitionId);
            setEdgePartitionStatus("Selected " + data.label + ".");
        }
    } catch (error) { setEdgePartitionStatus(error.message, "error"); }
}

function refreshEdgePartitionExplorer() {
    syncEdgePartitionControls();
    if (!EDGE_PARTITION_VIEW.data && $("edge-partition-select").value) selectEdgePartitionChoice($("edge-partition-select").value);
    renderEdgePartitionGroups(); renderEdgePartitionMembers();
}

function invalidateEdgeAttributePartitions(attribute) {
    const e = EDGE_PARTITION_VIEW, colorChoice = $("edge-color-partition").value;
    let removedColorPartition = false;
    for (const data of Object.values(e.partitions)) {
        if (data.type !== "edge-attribute" || data.configuration?.attribute !== attribute) continue;
        if (data.partitionId === colorChoice) removedColorPartition = true;
        delete e.partitions[data.partitionId];
        if (e.data === data) e.data = null;
    }
    e.choiceRequest++;
    e.attributeLoads.delete(edgeAttributeChoice(attribute));
    if (removedColorPartition && $("edge-color-mode").value === "partition") $("edge-color-mode").value = "default";
    syncEdgePartitionControls();
    if (state.activeTab === "edge-partitions") refreshEdgePartitionExplorer();
}

function applyEdgePartitionColorSelection() {
    applyAppearance();
}

function syncEdgePartitionControls() {
    const fill = (id, values, labels) => {
        if (typeof setSelectOptions === "function") setSelectOptions(id, values, labels);
    };
    const edgeAttrs = edgeAttrDefs().filter(def => def.numeric).map(def => def.name);
    fill("edge-partition-edge-attribute", edgeAttrs, edgeAttrs);
    const nodeAttrs = nodeAttrDefs();
    fill("edge-partition-node-attribute", nodeAttrs.map(def => def.name), nodeAttrs.map(def => def.name));
    const numericAttrs = nodeAttrs.filter(def => def.numeric);
    fill("community-bin-attribute", numericAttrs.map(def => def.name), numericAttrs.map(def => def.name));
    const partitions = Object.keys(state.communityData || {});
    const attributePartitions = nodeAttrs.map(def => ({ value: "attribute:" + def.name, label: "Attribute · " + def.name }))
        .filter(item => !partitions.includes(item.value));
    const values = [...partitions, ...attributePartitions.map(item => item.value)];
    const labels = [...partitions, ...attributePartitions.map(item => item.label)];
    fill("edge-partition-community", values, labels);
    for (const id of ["edge-partition-select", "ep-global-partition", "ep-individual-partition", "edge-color-partition"])
        fillEdgePartitionChoices(id, id === "edge-partition-select");
    const partitionColorOption = $("edge-color-mode").querySelector('option[value="partition"]');
    partitionColorOption.disabled = !computedEdgePartitions().length;
    if (partitionColorOption.disabled && $("edge-color-mode").value === "partition") {
        $("edge-color-mode").value = "default";
        syncEdgeColorControls();
    }
    if (EDGE_PARTITION_VIEW.data) $("edge-partition-select").value = edgePartitionChoice(EDGE_PARTITION_VIEW.data);
    for (const id of ["btn-ep-global", "btn-ep-individual"])
        if (!jobBusy(id)) $(id).disabled = !$(id === "btn-ep-global" ? "ep-global-partition" : "ep-individual-partition").options.length;
    $("edge-partition-view-metrics").disabled = !EDGE_PARTITION_VIEW.data;
    const kindSelect = $("edge-partition-kind");
    for (const item of kindSelect.options) {
        item.disabled = item.value === "edge-attribute" ? !edgeAttrs.length
            : item.value === "node-attribute-relation" ? !nodeAttrs.length : !values.length;
        item.hidden = item.value === "edge-attribute" && !edgeAttrs.length;
    }
    if (!kindSelect.selectedOptions.length || kindSelect.selectedOptions[0].disabled)
        kindSelect.value = [...kindSelect.options].find(item => !item.disabled)?.value || "";
    updateEdgePartitionFields();
    $("btn-community-bins").disabled = numericAttrs.length === 0 || !state.graphId;
    $("btn-edge-partition").disabled = !state.graphId || !kindSelect.value;
}

function updateEdgePartitionFields() {
    const kind = $("edge-partition-kind").value;
    const nodeRelation = kind === "node-attribute-relation";
    const numeric = nodeAttrDefs().find(def => def.name === $("edge-partition-node-attribute").value)?.numeric;
    toggleHidden("edge-partition-edge-attribute-field", kind !== "edge-attribute");
    toggleHidden("edge-partition-node-attribute-field", !nodeRelation);
    toggleHidden("edge-partition-bin-width-field", kind !== "edge-attribute" && (!nodeRelation || !numeric));
    toggleHidden("edge-partition-community-field", kind !== "community-internal-external" && kind !== "community-relations");
}

function initEdgePartitionUI() {
    $("edge-partition-kind").addEventListener("change", updateEdgePartitionFields);
    $("edge-partition-node-attribute").addEventListener("change", updateEdgePartitionFields);
    $("btn-community-bins").addEventListener("click", detectNumericAttributeCommunities);
    $("btn-edge-partition").addEventListener("click", detectEdgePartition);
    $("edge-partition-export").addEventListener("click", exportSelectedEdgePartition);
    $("edge-partition-select").addEventListener("change", event => selectEdgePartitionChoice(event.target.value));
    $("edge-partition-view-metrics").addEventListener("click", () => {
        const data = EDGE_PARTITION_VIEW.data;
        if (!data) return;
        if (isDirectEdgeAttributePartition(data)) {
            $("edge-attribute-metric-attribute").value = data.configuration.attribute;
            switchTab("metrics"); switchSubtab("edgeattr"); return;
        }
        const id = edgePartitionChoice(data);
        $("ep-global-partition").value = $("ep-individual-partition").value = id;
        switchTab("metrics"); switchSubtab("edgepartitions");
    });
    $("ep-global-metric").addEventListener("change", () =>
        renderParams("ep-global-params", "edgePartitionGlobal", $("ep-global-metric").value));
    $("ep-individual-metric").addEventListener("change", renderEdgePartitionMetricParams);
    $("btn-ep-global").addEventListener("click", () => runEdgePartitionMetric(false));
    $("btn-ep-individual").addEventListener("click", () => runEdgePartitionMetric(true));
    $("ep-export-metrics").addEventListener("click", exportEdgePartitionMetrics);
    $("ep-export-averages").addEventListener("click", () => {
        const rows = [["partition", "metric", "average"]];
        for (const data of computedEdgePartitions())
            for (const metric of Object.values(data.individualMetrics || {})) rows.push([data.label, metric.label, metric.average]);
        download("edge-partition-metric-averages.csv", rows.map(row => row.map(csvCell).join(",")).join("\r\n"), "text/csv");
    });
    $("ep-chart-metric").addEventListener("change", () => { syncEdgePartitionChartSort(); drawEdgePartitionChart(); });
    $("ep-chart-sort").addEventListener("change", drawEdgePartitionChart);
    $("edge-partition-search").addEventListener("input", () => { EDGE_PARTITION_VIEW.page = 0; renderEdgePartitionGroups(); });
    $("edge-partition-members-search").addEventListener("input", () => { EDGE_PARTITION_VIEW.memberPage = 0; renderEdgePartitionEdges(); });
    $("edge-partition-member-column").addEventListener("change", event => {
        EDGE_PARTITION_VIEW.memberColumn = event.target.value;
        EDGE_PARTITION_VIEW.memberPage = 0; renderEdgePartitionEdges();
    });
    $("edge-partition-close-inspector").addEventListener("click", () => selectEdgePartitionGroup(null));
    $("edge-partition-save-label").addEventListener("click", saveEdgePartitionLabel);
    $("edge-partition-export-table").addEventListener("click", exportEdgePartitionTable);
    $("edge-partition-highlight").addEventListener("click", () => edgePartitionToNetwork(false));
    $("edge-partition-isolate").addEventListener("click", () => edgePartitionToNetwork(true));
    $("edge-partition-groups").addEventListener("click", event => {
        const sort = event.target.closest("[data-ep-sort]");
        if (sort) {
            const e = EDGE_PARTITION_VIEW;
            e.descending = e.sort === sort.dataset.epSort ? !e.descending : false;
            e.sort = sort.dataset.epSort; e.page = 0; renderEdgePartitionGroups(); return;
        }
        if (event.target.closest("input")) return;
        const row = event.target.closest("tr[data-partition]");
        if (row) selectEdgePartitionGroup(Number(row.dataset.partition));
    });
    $("edge-partition-groups").addEventListener("input", event => {
        const color = event.target.closest("[data-ep-color]");
        if (color) {
            const data = EDGE_PARTITION_VIEW.data, group = data?.groups.find(item => item.id === Number(color.dataset.epColor));
            if (!group || !/^#[\da-f]{6}$/i.test(color.value)) return;
            group.color = color.value;
            if ($("edge-color-mode").value === "partition" && edgePartitionDataForChoice($("edge-color-partition").value) === data) applyAppearance();
            drawEdgePartitionChart();
            return;
        }
        const input = event.target.closest("[data-ep-note]");
        const group = EDGE_PARTITION_VIEW.data?.groups.find(item => item.id === Number(input?.dataset.epNote));
        if (group) group.notes = input.value;
    });
    syncEdgePartitionControls();
}

async function detectNumericAttributeCommunities() {
    if (jobBusy("btn-community-bins")) return cancelJob("btn-community-bins");
    if (!requireGraph()) return;
    const attribute = $("community-bin-attribute").value;
    const binWidth = Number($("community-bin-width").value);
    if (!attribute || !Number.isFinite(binWidth) || binWidth <= 0) {
        setStatus("Choose a numeric node attribute and a positive bin width.", "error");
        return;
    }
    const graphId = state.graphId, graph = state.graph, revision = state.networkRevision;
    setStatus("Detecting numeric attribute communities…", "busy");
    const signal = beginJob("btn-community-bins", "community");
    try {
        const res = await api("/api/communities/attribute-bins", {
            ...jsonBody({ graphId, attribute, binWidth }), signal,
        });
        if (res.cancelled) { setStatus("Stopped."); return; }
        if (graphId !== state.graphId || graph !== state.graph || revision !== state.networkRevision) return;
        state.communityData[res.algorithm] = res.values;
        communityDetected(res.algorithm, "attribute-bins", { attribute, binWidth }, res.numCommunities);
        COMMUNITY_EXPLORER.metadata[res.algorithm].valueLabels = res.communityLabels || {};
        rebuildAppearanceOptions();
        $("node-color-mode").value = "attribute";
        $("color-by").value = "community:" + res.algorithm;
        syncNodeColorControls();
        applyAppearance();
        $("community-result").textContent = res.label + " — " + res.numCommunities + " communities";
        $("btn-global-comm").disabled = false;
        $("btn-indiv-comm").disabled = false;
        const partitions = Object.keys(state.communityData).filter(name => !name.startsWith("attribute:"));
        setSelectOptions("indiv-comm-partition", partitions, partitions);
        $("indiv-comm-partition").value = res.algorithm;
        setSelectOptions("global-comm-partition", partitions, partitions);
        $("global-comm-partition").value = res.algorithm;
        updatePartitionField();
        applyReducers();
        refreshAfterCompute();
        syncEdgePartitionControls();
        setStatus("Detected " + res.numCommunities + " numeric attribute communities.");
    } catch (error) {
        if (!isAbort(error)) setStatus(error.message, "error");
    } finally {
        endJob("btn-community-bins");
    }
}

async function detectEdgePartition() {
    if (!requireGraph()) return;
    EDGE_PARTITION_VIEW.choiceRequest++;
    const graphId = state.graphId, graph = state.graph, revision = state.networkRevision;
    const request = ++EDGE_PARTITION_VIEW.request;
    const type = $("edge-partition-kind").value;
    const body = { graphId, type };
    if (type === "edge-attribute") {
        body.attribute = $("edge-partition-edge-attribute").value;
        body.binWidth = Number($("edge-partition-bin-width").value);
        if (!edgeAttrDefs().some(def => def.name === body.attribute && def.numeric)
            || !Number.isFinite(body.binWidth) || body.binWidth <= 0) {
            setEdgePartitionStatus("Choose a numeric edge attribute and a positive bin width.", "error"); return;
        }
    }
    if (type === "node-attribute-relation") {
        body.attribute = $("edge-partition-node-attribute").value;
        const definition = nodeAttrDefs().find(def => def.name === body.attribute);
        if (definition?.numeric) {
            body.binWidth = Number($("edge-partition-bin-width").value);
            if (!Number.isFinite(body.binWidth) || body.binWidth <= 0) {
                setEdgePartitionStatus("Enter a positive numeric bin width.", "error"); return;
            }
        }
    }
    if (type === "community-internal-external" || type === "community-relations")
        body.partition = $("edge-partition-community").value;
    const existing = Object.values(EDGE_PARTITION_VIEW.partitions).find(data =>
        JSON.stringify(data.configuration) === JSON.stringify(body));
    if (existing) {
        selectEdgePartitionResult(existing.partitionId);
        setEdgePartitionStatus("Selected existing edge partition.");
        return;
    }
    setEdgePartitionStatus("Detecting edge groups…", "busy");
    $("btn-edge-partition").disabled = true;
    try {
        const result = await api("/api/edge-partitions", jsonBody(body));
        if (result.cancelled) { setEdgePartitionStatus("Cancelled."); return; }
        if (request !== EDGE_PARTITION_VIEW.request || graphId !== state.graphId || graph !== state.graph || revision !== state.networkRevision) return;
        result.configuration = body;
        result.globalMetrics = {};
        result.individualMetrics = {};
        EDGE_PARTITION_VIEW.partitions[result.partitionId] = result;
        selectEdgePartitionResult(result.partitionId);
        const edgeCount = result.edges.length;
        setEdgePartitionStatus("Found " + result.groups.length + " groups across " + edgeCount + " edges.");
        syncEdgePartitionControls();
    } catch (error) {
        setEdgePartitionStatus(error.message, "error");
    } finally {
        $("btn-edge-partition").disabled = false;
    }
}

function edgePartitionColumns() {
    const data = EDGE_PARTITION_VIEW.data;
    return [{ key: "id", label: "ID" }, { key: "label", label: "Label" }, { key: "size", label: "Edges" },
        { key: "share", label: "Network share (%)" }, { key: "notes", label: "Notes" },
        ...Object.entries(data?.individualMetrics || {}).map(([key, metric]) => ({ key: "metric:" + key, label: metric.label }))];
}

function edgePartitionColor(group, data = EDGE_PARTITION_VIEW.data) {
    return /^#[\da-f]{6}$/i.test(group.color) ? group.color : categoricalHex(Math.max(0, group.id));
}

function edgePartitionNetworkColors(partitionId) {
    const data = EDGE_PARTITION_VIEW.partitions[partitionId], result = new Map();
    if (!isComputedEdgePartition(data) || !state.graph) return result;
    if (!data.networkGroups) {
        data.networkGroups = new Map();
        for (const edge of data.edges) {
            const key = edgePartitionEdgeKey(edge);
            if (key != null) data.networkGroups.set(key, edge.partition);
        }
    }
    const colors = new Map(data.groups.map(group => [group.id, edgePartitionColor(group, data)]));
    for (const [edge, group] of data.networkGroups) result.set(edge, colors.get(group));
    return result;
}

function edgePartitionGroupValue(group, key) {
    if (key === "color") return edgePartitionColor(group);
    if (key === "share") return state.graph?.size ? 100 * group.size / state.graph.size : 0;
    if (key.startsWith("metric:")) return EDGE_PARTITION_VIEW.data.individualMetrics[key.slice(7)]?.values[group.id];
    return group[key];
}

function edgePartitionRows() {
    const e = EDGE_PARTITION_VIEW, query = $("edge-partition-search").value.trim().toLowerCase();
    return (e.data?.groups || []).filter(group => String(group.id).includes(query) || group.label.toLowerCase().includes(query))
        .slice().sort((a, b) => compareMetricTableValues(edgePartitionGroupValue(a, e.sort), edgePartitionGroupValue(b, e.sort), e.descending ? -1 : 1) || a.id - b.id);
}

function renderEdgePartitionGroups() {
    const e = EDGE_PARTITION_VIEW, data = e.data, table = $("edge-partition-groups"), esc = escapeEdgePartitionHtml;
    $("edge-partition-empty").hidden = !!data;
    $("edge-partition-content").hidden = !data;
    $("edge-partition-summary").textContent = data ? data.groups.length + " groups · " + data.edges.length + " edges" : "";
    renderEdgePartitionGlobalSummary();
    if (!data) { table.replaceChildren(); return; }
    const rows = edgePartitionRows(), columns = edgePartitionColumns();
    e.page = Math.min(e.page, Math.max(0, Math.ceil(rows.length / COMMUNITY_PAGE_SIZE) - 1));
    table.innerHTML = "<thead><tr><th>Colour</th>" + columns.map(col => '<th aria-sort="' + (col.key === e.sort ? e.descending ? "descending" : "ascending" : "none")
        + '"><button type="button" data-ep-sort="' + esc(col.key) + '">' + esc(col.label)
        + (col.key === e.sort ? e.descending ? " ▼" : " ▲" : "") + "</button></th>").join("") + "</tr></thead><tbody>"
        + rows.slice(e.page * COMMUNITY_PAGE_SIZE, (e.page + 1) * COMMUNITY_PAGE_SIZE).map(group => '<tr data-partition="' + group.id
            + '" class="' + (group.id === e.selected ? "community-selected" : "") + '"><td><input type="color" class="partition-color-input" data-ep-color="'
            + group.id + '" value="' + toHexColor(edgePartitionColor(group)) + '" aria-label="Colour for ' + esc(group.label) + '"></td>' + columns.map(col => {
                const value = edgePartitionGroupValue(group, col.key);
                return "<td>" + (col.key === "notes" ? '<input class="community-note-input" type="text" maxlength="2000" data-ep-note="' + group.id
                    + '" aria-label="Notes for ' + esc(group.label) + '" placeholder="Add note" value="' + esc(value) + '">'
                    : ["id", "label"].includes(col.key) ? '<button type="button">' + esc(value) + "</button>"
                    : typeof value === "number" || value == null ? edgePartitionMetricValue(value) : esc(value)) + "</td>";
            }).join("") + "</tr>").join("") + (rows.length ? "" : '<tr><td colspan="' + (columns.length + 1) + '">No matching groups.</td></tr>') + "</tbody>";
    communityPager("edge-partition-table-pager", rows.length, e.page, page => { e.page = page; renderEdgePartitionGroups(); });
}

function selectEdgePartitionGroup(group) {
    EDGE_PARTITION_VIEW.selected = group;
    if (EDGE_PARTITION_VIEW.data) EDGE_PARTITION_VIEW.data.selectedGroup = group;
    EDGE_PARTITION_VIEW.memberPage = 0;
    $("edge-partition-members-search").value = "";
    renderEdgePartitionGroups();
    renderEdgePartitionMembers();
}

function selectedEdgePartitionGroup() {
    return EDGE_PARTITION_VIEW.data?.groups.find(item => item.id === EDGE_PARTITION_VIEW.selected);
}

async function saveEdgePartitionLabel() {
    const data = EDGE_PARTITION_VIEW.data, group = selectedEdgePartitionGroup();
    const label = $("edge-partition-label").value.trim().slice(0, 120);
    const button = $("edge-partition-save-label");
    if (!data || !group || button.disabled) return;
    if (!label) { setStatus("The label cannot be empty.", "error"); return; }
    const attribute = data.configuration?.attribute;
    const definition = edgeAttrDefs().find(def => def.name === attribute);
    if (data.type !== "edge-attribute" || !["categorical", "string"].includes(definition?.type)) {
        group.label = label;
        renderEdgePartitionGroups(); renderEdgePartitionMembers(); drawEdgePartitionChart();
        return;
    }
    const graphId = state.graphId, graph = state.graph;
    const keys = edgePartitionGroupEdges(group.id).map(edgePartitionEdgeKey);
    if (keys.some(key => key == null)) {
        setStatus("Some selected edges no longer exist. Detect the partition again before renaming it.", "error"); return;
    }
    button.disabled = true;
    try {
        const result = await api("/api/edge-partitions/attribute-label", jsonBody({
            graphId, partitionId: data.partitionId, group: group.id, attribute, label,
        }));
        if (graphId !== state.graphId || graph !== state.graph) return;
        for (const key of keys) {
            if (!graph.hasEdge(key)) continue;
            const attrs = { ...(graph.getEdgeAttribute(key, "attrs") || {}) };
            applyLocalAttr(attrs, attribute, label, edgeAttrDefs());
            graph.setEdgeAttribute(key, "attrs", attrs);
        }
        state.networkRevision++;
        result.configuration = data.configuration;
        // Renaming to an existing category merges the groups and can renumber
        // every group. Reuse presentation settings by value and discard old metrics.
        result.globalMetrics = {}; result.individualMetrics = {};
        for (const next of result.groups) {
            const previous = data.groups.find(item => item !== group && item.label === next.label)
                || (next.label === label ? group : data.groups.find(item => item.label === next.label));
            if (previous) { next.color = previous.color; next.notes = previous.notes; }
            if (next.label === label && previous !== group && group.notes)
                next.notes = [previous?.notes, group.notes].filter(Boolean).join("\n");
        }
        // A graph edit may have invalidated this partition while the request was
        // in flight. Update live attributes without restoring an obsolete partition.
        if (EDGE_PARTITION_VIEW.partitions[data.partitionId] === data) {
            EDGE_PARTITION_VIEW.partitions[data.partitionId] = result;
            if (EDGE_PARTITION_VIEW.data === data) selectEdgePartitionResult(result.partitionId);
        }
        for (const metric of state.edgeAttributeMetricOrder.filter(name => state.edgeAttributeMetricData[name]?.attribute === attribute))
            delete state.edgeAttributeMetricData[metric];
        state.edgeAttributeMetricOrder = state.edgeAttributeMetricOrder.filter(name => state.edgeAttributeMetricData[name]);
        delete state.graphMetrics["Number of attribute values (" + attribute + ")"];
        syncEdgePartitionControls();
        applyAppearance(); // Rebuilds displayed edge labels from the updated attribute.
        if (state.selectedEdge && graph.hasEdge(state.selectedEdge)) renderEdgeInfo(state.selectedEdge);
        refreshAfterCompute();
        setStatus("Updated " + attribute + " for " + keys.length + " edges.");
    } catch (error) { setStatus(error.message, "error"); }
    finally { button.disabled = false; }
}

function edgePartitionGroupEdges(group) {
    const data = EDGE_PARTITION_VIEW.data;
    if (!data) return [];
    if (!data.edgesByGroup) {
        data.edgesByGroup = new Map();
        for (const edge of data.edges) {
            if (!data.edgesByGroup.has(edge.partition)) data.edgesByGroup.set(edge.partition, []);
            data.edgesByGroup.get(edge.partition).push(edge);
        }
    }
    return data.edgesByGroup.get(group) || [];
}

// Partitions do not change after detection. Reuse endpoint membership and each
// inspected group's connections instead of rescanning all edges on every refresh.
function edgePartitionConnections(group) {
    const data = EDGE_PARTITION_VIEW.data;
    if (!data.nodeGroups) {
        data.nodeGroups = new Map();
        data.connectionsByGroup = new Map();
        for (const edge of data.edges) for (const node of [edge.source, edge.target]) {
            if (!data.nodeGroups.has(node)) data.nodeGroups.set(node, new Set());
            data.nodeGroups.get(node).add(edge.partition);
        }
    }
    if (!data.connectionsByGroup.has(group)) {
        const shared = new Map(), endpoints = new Set();
        for (const edge of edgePartitionGroupEdges(group)) { endpoints.add(edge.source); endpoints.add(edge.target); }
        for (const node of endpoints) for (const neighbour of data.nodeGroups.get(node)) {
            if (neighbour === group) continue;
            shared.set(neighbour, (shared.get(neighbour) || 0) + 1);
        }
        data.connectionsByGroup.set(group, shared);
    }
    return data.connectionsByGroup.get(group);
}

function renderEdgePartitionMembers() {
    const data = EDGE_PARTITION_VIEW.data, group = selectedEdgePartitionGroup(), esc = escapeEdgePartitionHtml;
    $("edge-partition-inspector").hidden = !group;
    $("edge-partition-workspace").classList.toggle("has-inspector", !!group);
    if (!group) return;
    const edges = edgePartitionGroupEdges(group.id);
    $("edge-partition-inspector-title").textContent = group.label;
    $("edge-partition-inspector-summary").textContent = "ID " + group.id + " · " + data.label;
    $("edge-partition-label").value = group.label;
    const columns = edgePartitionColumns().filter(col => !["id", "label", "notes"].includes(col.key));
    $("edge-partition-details").innerHTML = "<tbody>" + columns.map(col => "<tr><th>" + esc(col.label)
        + "</th><td>" + edgePartitionMetricValue(edgePartitionGroupValue(group, col.key)) + "</td></tr>").join("") + "</tbody>";
    const extra = edgeColumns().filter(col => !["source", "target", "edgeid", "_act"].includes(col.key));
    setSelectOptions("edge-partition-member-column", ["", ...extra.map(col => col.key)], ["None", ...extra.map(col => col.label)]);
    if (!extra.some(col => col.key === EDGE_PARTITION_VIEW.memberColumn)) EDGE_PARTITION_VIEW.memberColumn = "";
    $("edge-partition-member-column").value = EDGE_PARTITION_VIEW.memberColumn;
    renderEdgePartitionEdges();
    const shared = edgePartitionConnections(group.id);
    $("edge-partition-connections").innerHTML = "<thead><tr><th>Group</th><th>Shared nodes</th></tr></thead><tbody>"
        + data.groups.filter(item => shared.has(item.id)).map(item => '<tr><td><button data-ep-neighbour="' + item.id + '">'
            + esc(item.label) + "</button></td><td>" + shared.get(item.id) + "</td></tr>").join("")
        + (shared.size ? "" : '<tr><td colspan="2">No connections.</td></tr>') + "</tbody>";
    $("edge-partition-connections").querySelectorAll("[data-ep-neighbour]").forEach(button =>
        button.onclick = () => selectEdgePartitionGroup(Number(button.dataset.epNeighbour)));
    $("edge-partition-highlight").disabled = $("edge-partition-isolate").disabled = !edges.length;
}

// Cache exact graphology keys once, retaining the stable identity of parallel edges.
function edgePartitionEdgeKey(edge) {
    if (edge.networkKey !== undefined) return edge.networkKey;
    const graph = state.graph;
    if (edge.edgeId != null && graph.hasEdge(String(edge.edgeId))) return edge.networkKey = String(edge.edgeId);
    const candidates = graph.hasNode(edge.source) && graph.hasNode(edge.target) ? graph.edges(edge.source, edge.target)
        .filter(key => !state.directed || graph.source(key) === edge.source && graph.target(key) === edge.target) : [];
    return edge.networkKey = candidates[Math.max(0, edge.index || 0)] ?? null;
}

function edgePartitionMemberRows() {
    const group = selectedEdgePartitionGroup(), query = $("edge-partition-members-search").value.trim().toLowerCase();
    const columns = edgeColumns().filter(col => col.key !== "_act");
    const edges = edgePartitionGroupEdges(group?.id);
    if (!query) return edges;
    return edges.filter(edge => {
        const key = edgePartitionEdgeKey(edge);
        return [edge.source, edge.target, edge.edgeId, ...columns.map(col => key == null ? "" : edgeRowValue(key, col.key))]
            .some(value => String(value ?? "").toLowerCase().includes(query));
    });
}

function renderEdgePartitionEdges() {
    const e = EDGE_PARTITION_VIEW, data = e.data, esc = escapeEdgePartitionHtml, rows = edgePartitionMemberRows();
    if (!data) return;
    e.memberPage = Math.min(e.memberPage, Math.max(0, Math.ceil(rows.length / COMMUNITY_PAGE_SIZE) - 1));
    const columns = edgeColumns().filter(col => ["source", "target", "edgeid", e.memberColumn].includes(col.key));
    $("edge-partition-members").innerHTML = "<thead><tr>" + columns.map(col => "<th>" + esc(col.label) + "</th>").join("") + "</tr></thead><tbody>"
        + rows.slice(e.memberPage * COMMUNITY_PAGE_SIZE, (e.memberPage + 1) * COMMUNITY_PAGE_SIZE).map(edge => {
            const key = edgePartitionEdgeKey(edge);
            return "<tr>" + columns.map(col => {
                const value = col.key === "source" || col.key === "target" ? edge[col.key]
                    : col.key === "edgeid" ? edge.edgeId ?? "#" + edge.index : key == null ? null : edgeRowValue(key, col.key);
                return "<td>" + (col.key === "source" || col.key === "target" ? '<button data-ep-node="' + esc(value) + '">' + esc(value) + "</button>" : esc(value == null ? "—" : fmt(value))) + "</td>";
            }).join("") + "</tr>";
        }).join("") + (rows.length ? "" : '<tr><td colspan="' + columns.length + '">No matching edges.</td></tr>') + "</tbody>";
    $("edge-partition-members").querySelectorAll("[data-ep-node]").forEach(button => button.onclick = () => {
        setSelectionMode("highlight"); selectNode(button.dataset.epNode); switchTab("network");
    });
    $("edge-partition-export").disabled = !rows.length;
    communityPager("edge-partition-members-pager", rows.length, e.memberPage, page => { e.memberPage = page; renderEdgePartitionEdges(); });
}

function exportEdgePartitionTable() {
    const columns = [{ key: "color", label: "Colour" }, ...edgePartitionColumns()], rows = [columns.map(col => col.label), ...edgePartitionRows().map(group => columns.map(col => edgePartitionGroupValue(group, col.key)))];
    download("edge-partition-groups.csv", rows.map(row => row.map(csvCell).join(",")).join("\r\n"), "text/csv");
}

function edgePartitionToNetwork(only) {
    const group = selectedEdgePartitionGroup();
    const keys = edgePartitionGroupEdges(group?.id).map(edgePartitionEdgeKey).filter(key => key != null);
    if (!keys.length) return;
    clearSelection({ refreshTable: false, refreshGraph: false });
    selectEdge(keys[0]);
    state.selection.edgeGroup = keys;
    setSelectionMode(only ? "edge-group-only" : "edge-group-highlight");
    switchTab("network");
    setStatus("Edge group " + selectedEdgePartitionGroup().label + " selected. Existing network filters still apply.");
}

function exportSelectedEdgePartition() {
    const data = EDGE_PARTITION_VIEW.data;
    const group = data?.groups.find(item => item.id === EDGE_PARTITION_VIEW.selected);
    if (!data || !group) return;
    const edges = edgePartitionMemberRows(), extra = EDGE_PARTITION_VIEW.memberColumn;
    const rows = [["source", "target", "edgeId", "partition", "partitionLabel", ...(extra ? [edgeColumns().find(col => col.key === extra)?.label] : [])], ...edges.map(edge =>
        [edge.source, edge.target, data.multigraph ? edge.edgeId ?? "#" + edge.index : "", group.id, group.label,
            ...(extra ? [edgePartitionEdgeKey(edge) == null ? null : edgeRowValue(edgePartitionEdgeKey(edge), extra)] : [])])];
    download("edge-group-" + group.id + ".csv", rows.map(row => row.map(csvCell).join(",")).join("\r\n"), "text/csv");
}

function setEdgePartitionStatus(message, kind) {
    const status = $("edge-partition-status");
    status.textContent = message;
    status.classList.toggle("error", kind === "error");
    status.classList.toggle("busy", kind === "busy");
}

function escapeEdgePartitionHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, character => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[character]);
}

function resetEdgePartitionView() {
    EDGE_PARTITION_VIEW.partitions = {};
    EDGE_PARTITION_VIEW.data = null;
    EDGE_PARTITION_VIEW.selected = null;
    EDGE_PARTITION_VIEW.request++;
    EDGE_PARTITION_VIEW.choiceRequest++;
    EDGE_PARTITION_VIEW.attributeLoads.clear();
    $("edge-partition-groups").replaceChildren();
    $("edge-partition-members").replaceChildren();
    $("edge-partition-globals").replaceChildren();
    $("edge-partition-details").replaceChildren();
    $("edge-partition-export").disabled = true;
    EDGE_PARTITION_VIEW.page = EDGE_PARTITION_VIEW.memberPage = 0;
    EDGE_PARTITION_VIEW.sort = "size"; EDGE_PARTITION_VIEW.descending = true;
    $("edge-partition-search").value = $("edge-partition-members-search").value = "";
    renderEdgePartitionGroups(); renderEdgePartitionMembers();
    setEdgePartitionStatus("");
    syncEdgePartitionControls();
    renderEdgePartitionMetricResults();
}

function selectEdgePartitionResult(id) {
    const data = EDGE_PARTITION_VIEW.partitions[id];
    if (!data) return;
    EDGE_PARTITION_VIEW.data = data;
    EDGE_PARTITION_VIEW.selected = data.selectedGroup ?? null;
    EDGE_PARTITION_VIEW.page = EDGE_PARTITION_VIEW.memberPage = 0;
    if (!edgePartitionColumns().some(col => col.key === EDGE_PARTITION_VIEW.sort)) EDGE_PARTITION_VIEW.sort = "size";
    $("edge-partition-search").value = $("edge-partition-members-search").value = "";
    syncEdgePartitionControls();
    renderEdgePartitionGroups();
    renderEdgePartitionMembers();
}

function loadEdgePartitionMetricCatalog(catalog) {
    state.defs.edgePartitionGlobal = indexById(catalog.edgePartitionGlobal);
    state.defs.edgePartitionIndividual = indexById(catalog.edgePartitionIndividual);
    state.defs.intraEdgePartitionIndividual = indexById(catalog.intraEdgePartitionIndividual);
    fillSelect("ep-global-metric", catalog.edgePartitionGlobal || []);
    fillSelect("ep-individual-metric", [...(catalog.edgePartitionIndividual || []), ...(catalog.intraEdgePartitionIndividual || [])]);
    renderParams("ep-global-params", "edgePartitionGlobal", $("ep-global-metric").value);
    renderEdgePartitionMetricParams();
    syncEdgePartitionControls();
}

function renderEdgePartitionMetricParams() {
    const metric = $("ep-individual-metric").value;
    renderParams("ep-individual-params", metric.startsWith("intra-edge-partition:")
        ? "intraEdgePartitionIndividual" : "edgePartitionIndividual", metric);
}

async function runEdgePartitionMetric(individual) {
    const scope = individual ? "individual" : "global", prefix = "ep-" + scope;
    const button = "btn-" + prefix;
    if (jobBusy(button)) return cancelJob(button);
    if (!requireGraph()) return;
    const choice = $(prefix + "-partition").value;
    const data = EDGE_PARTITION_VIEW.partitions[choice];
    if (!isComputedEdgePartition(data)) { setStatus("Detect an edge partition in the Edge partitions tab first.", "error"); return; }
    const metric = $(prefix + "-metric").value, params = collectParams(prefix + "-params");
    if (!metric) { setStatus("Choose an edge partition metric first.", "error"); return; }
    const graphId = state.graphId, graph = state.graph, revision = state.networkRevision;
    const signal = beginJob(button, individual ? "edgePartitionIndividual" : "edgePartitionGlobal");
    setStatus("Computing edge partition metric…", "busy");
    try {
        if (signal.aborted || graphId !== state.graphId || graph !== state.graph || revision !== state.networkRevision) return;
        const partitionId = data.partitionId;
        const result = await api("/api/edge-partitions/" + scope, {
            ...jsonBody({ graphId, partitionId, metric, params }), signal,
        });
        if (result.cancelled) { setStatus("Stopped."); return; }
        if (graphId !== state.graphId || graph !== state.graph || revision !== state.networkRevision
            || EDGE_PARTITION_VIEW.partitions[partitionId] !== data) return;
        const key = JSON.stringify([metric, params]);
        (individual ? data.individualMetrics : data.globalMetrics)[key] = result;
        if (state.activeTab === "edge-partitions") {
            renderEdgePartitionGroups(); renderEdgePartitionMembers();
        }
        switchSubtab("edgepartitions");
        setStatus("Computed " + result.label + ".");
    } catch (error) {
        if (!isAbort(error)) setStatus(error.message, "error");
    } finally { endJob(button); }
}

function edgePartitionMetricValue(value) {
    return value == null || !Number.isFinite(value) ? "—" : escapeEdgePartitionHtml(fmt(value));
}

function renderEdgePartitionGlobalSummary() {
    const metrics = Object.values(EDGE_PARTITION_VIEW.data?.globalMetrics || {});
    $("edge-partition-globals").hidden = !metrics.length;
    $("edge-partition-globals").textContent = metrics.map(metric => metric.label + ": " + (metric.value == null ? "—" : fmt(metric.value))).join(" · ");
}

function renderEdgePartitionMetricResults() {
    const globalRows = [], averageRows = [], entries = edgePartitionChartEntries();
    for (const data of computedEdgePartitions()) {
        for (const metric of Object.values(data.globalMetrics || {}))
            globalRows.push({ label: metric.label + " · " + data.label, original: metric.value });
        for (const metric of Object.values(data.individualMetrics || {}))
            averageRows.push({ label: metric.label + " · " + data.label, original: metric.average });
    }
    buildAverageTable("ep-global-results", globalRows, []);
    buildAverageTable("ep-average-results", averageRows, []);
    setSelectOptions("ep-chart-metric", entries.map(entry => entry.key), entries.map(entry => entry.metric.label + " · " + entry.data.label));
    syncEdgePartitionChartSort();
    drawEdgePartitionChart();
}

function edgePartitionChartEntries() {
    return computedEdgePartitions().flatMap(data => Object.entries(data.individualMetrics || {})
        .map(([key, metric]) => ({ key: JSON.stringify([data.partitionId, key]), data, metric })));
}

function syncEdgePartitionChartSort() {
    const entries = edgePartitionChartEntries(), entry = entries.find(item => item.key === $("ep-chart-metric").value);
    const comparable = entries.filter(item => item.data === entry?.data);
    setSelectOptions("ep-chart-sort", ["group", ...comparable.map(item => item.key)], ["Group ID", ...comparable.map(item => item.metric.label)]);
}

function drawEdgePartitionChart() {
    const entries = edgePartitionChartEntries(), entry = entries.find(item => item.key === $("ep-chart-metric").value);
    if (!entry) { clearChart("ep-chart"); return; }
    const sort = $("ep-chart-sort").value, compared = entries.find(item => item.key === sort && item.data === entry.data)?.metric || entry.metric;
    const groups = entry.data.groups.slice().sort((a, b) => sort === "group" ? a.id - b.id
        : compareMetricTableValues(compared.values[a.id], compared.values[b.id], -1) || a.id - b.id);
    const definition = state.defs.edgePartitionIndividual?.[entry.metric.metric] || state.defs.intraEdgePartitionIndividual?.[entry.metric.metric];
    drawBarChart("ep-chart", "ep-chart-tip", groups.map(group => ({ label: group.label, value: entry.metric.values[group.id] ?? NaN, color: edgePartitionColor(group, entry.data) })),
        entry.metric.label + " · " + entry.data.label, "", definition?.label || metricTableDetails(entry.metric.label).metric);
}

function exportEdgePartitionMetrics() {
    const rows = [["partition", "scope", "groupId", "groupLabel", "metric", "value"]];
    for (const data of computedEdgePartitions()) {
        for (const metric of Object.values(data.globalMetrics || {}))
            rows.push([data.label, "global", "", "", metric.label, metric.value]);
        for (const metric of Object.values(data.individualMetrics || {}))
            for (const group of data.groups)
                rows.push([data.label, "individual", group.id, group.label, metric.label, metric.values[group.id]]);
    }
    const csv = rows.map(row => row.map(csvCell).join(",")).join("\r\n");
    download("edge-partition-metrics.csv", csv, "text/csv");
}
