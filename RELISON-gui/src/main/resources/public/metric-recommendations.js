"use strict";

const METRIC_RECOMPUTE_ROUTES = new Set(["/api/metrics/vertex", "/api/metrics/graph", "/api/metrics/pair", "/api/metrics/edge-attributes",
    "/api/communities/global", "/api/communities/individual", "/api/edge-partitions/global", "/api/edge-partitions/individual"]);
const METRIC_RECOMPUTE = { running: false, stop: false, signal: null, processId: null, completed: 0, total: 0 };

// Capture the submitted request, not controls which may have changed while it ran.
function rememberMetricComputation(url, requestBody, response) {
    if (!METRIC_RECOMPUTE_ROUTES.has(url) || response.cancelled) return;
    const body = JSON.parse(requestBody || "{}");
    if (body.withRecommendation || body.recommendationKey != null) return;
    let family, recFamily, shape = "map", label = response.label;
    if (url.endsWith("/vertex")) { family = "nodes"; recFamily = "vertex"; }
    else if (url.endsWith("/graph")) { family = "global"; recFamily = "graph"; shape = "scalar"; }
    else if (url.endsWith("/pair")) { family = body.onlyLinks === false ? "pairs" : "edges"; recFamily = family === "pairs" ? "nodePair" : "pair"; shape = family === "pairs" ? "aggregate" : "pairs"; }
    else if (url.includes("/communities/")) {
        family = body.attribute ? "attrcomm" : "comm";
        const global = url.endsWith("/global"); recFamily = global ? "graph" : "comm"; shape = global ? "scalar" : "map";
        label += global ? " (" + response.algorithm + ")" : " · " + response.algorithm;
    } else if (url.endsWith("/edge-attributes")) {
        family = "edgeattr"; shape = response.values ? "map" : "scalar"; recFamily = shape === "map" ? "edgeattr" : "graph";
    } else {
        family = "edgepartitions"; shape = url.endsWith("/individual") ? "map" : "scalar";
        recFamily = shape === "map" ? "edgePartition" : "edgePartitionGlobal";
        label = JSON.stringify([body.partitionId, JSON.stringify([body.metric, body.params || {}])]);
    }
    const { graphId, ...configuration } = body;
    const recipe = { family, recFamily, shape, label, url, body: configuration };
    (state.metricComputations ||= {})[JSON.stringify([recFamily, label])] = recipe;
}

function metricRecipeParameters(definition, text) {
    const params = definition.params || [];
    if (!params.length) return text === "" ? {} : null;
    if (!text.startsWith(" (") || !text.endsWith(")")) return null;
    const content = text.slice(2, -1), result = {};
    let cursor = 0;
    for (let i = 0; i < params.length; i++) {
        const param = params[i], prefix = param.name + "=";
        if (!content.startsWith(prefix, cursor)) return null;
        cursor += prefix.length;
        const end = i + 1 < params.length ? content.indexOf(", " + params[i + 1].name + "=", cursor) : content.length;
        if (end < 0) return null;
        const value = content.slice(cursor, end);
        if (["int", "double"].includes(param.type)) {
            if (!value.trim() || !Number.isFinite(Number(value)) || param.type === "int" && !Number.isInteger(Number(value))) return null;
            result[param.name] = Number(value);
        } else if (param.type === "bool") {
            if (!["true", "false"].includes(value)) return null;
            result[param.name] = value === "true";
        } else result[param.name] = value;
        cursor = end + 2;
    }
    return result;
}

function recoverMetricRecipe(family, label, definitions, url, extra, recFamily, shape, core = label) {
    const candidates = definitions.flatMap(name => Object.entries(state.defs?.[name] || {})).sort((a, b) => b[1].label.length - a[1].label.length);
    for (const [id, definition] of candidates) {
        if (!core.startsWith(definition.label)) continue;
        const params = metricRecipeParameters(definition, core.slice(definition.label.length));
        if (params) return { family, label, url, recFamily, shape, body: { ...extra, metric: id, params } };
    }
    return null;
}

function metricComputationInventory() {
    const recipes = [], skipped = [], remembered = state.metricComputations || {};
    const add = (family, label, recFamily, recover) => {
        const recipe = remembered[JSON.stringify([recFamily, label])] || recover();
        if (recipe) recipes.push(recipe); else skipped.push(label);
    };
    for (const [family, order, definitions, url, recFamily, shape, extra] of [
        ["nodes", state.metricOrder, ["vertex"], "/api/metrics/vertex", "vertex", "map", {}],
        ["edges", state.pairOrder, ["pair"], "/api/metrics/pair", "pair", "pairs", { onlyLinks: true }],
        ["pairs", state.nodePairOrder, ["pair"], "/api/metrics/pair", "nodePair", "aggregate", { onlyLinks: false }],
    ]) for (const label of order || []) add(family, label, recFamily, () => recoverMetricRecipe(family, label, definitions, url, extra, recFamily, shape));
    for (const [family, order, data] of [["comm", state.commMetricOrder, state.commMetricData], ["attrcomm", state.attributeMetricOrder, state.attributeMetricData]])
        for (const label of order || []) {
            const entry = data[label], core = label.slice(0, label.lastIndexOf(" · "));
            add(family, label, "comm", () => recoverMetricRecipe(family, label, ["communityIndividual", "intraCommunityIndividual", "intraAttributeIndividual"],
                "/api/communities/individual", family === "comm" ? { algorithm: entry.algorithm } : { attribute: entry.attribute }, "comm", "map", core));
        }
    for (const label of state.edgeAttributeMetricOrder || []) add("edgeattr", label, "edgeattr", () => ({ family: "edgeattr", label, recFamily: "edgeattr", shape: "map",
        url: "/api/metrics/edge-attributes", body: { attribute: state.edgeAttributeMetricData[label].attribute, metric: "count" } }));
    for (const label of Object.keys(state.graphMetrics || {})) add("global", label, "graph", () => {
        for (const family of ["comm", "attrcomm", "edgeattr"]) {
            if (!metricAnalysisGlobalRows(family).some(row => row.label === label)) continue;
            if (family === "edgeattr") {
                const attribute = state.globalMetricScopes?.[label]?.scope || label.slice("Number of attribute values (".length, -1);
                return { family, label, recFamily: "graph", shape: "scalar", url: "/api/metrics/edge-attributes", body: { attribute, metric: "num-attributes" } };
            }
            const scope = state.globalMetricScopes?.[label]?.scope;
            const algorithm = family === "comm" ? scope || Object.keys(state.communityData || {}).find(id => label.endsWith(" (" + id + ")")) : null;
            const start = family === "comm" ? label.lastIndexOf(" (" + algorithm + ")") : label.lastIndexOf(" (Attribute: ");
            const attribute = family === "attrcomm" ? scope || label.slice(start + " (Attribute: ".length, -1) : null;
            return recoverMetricRecipe(family, label, ["communityGlobal"], "/api/communities/global", attribute ? { attribute } : { algorithm }, "graph", "scalar", label.slice(0, start));
        }
        return recoverMetricRecipe("global", label, ["graph"], "/api/metrics/graph", {}, "graph", "scalar");
    });
    if (typeof computedEdgePartitions === "function") for (const data of computedEdgePartitions()) for (const individual of [true, false])
        for (const key of Object.keys((individual ? data.individualMetrics : data.globalMetrics) || {})) {
            const label = JSON.stringify([data.partitionId, key]), [metric, params] = JSON.parse(key);
            const recFamily = individual ? "edgePartition" : "edgePartitionGlobal";
            add("edgepartitions", label, recFamily, () => ({ family: "edgepartitions", label, recFamily, shape: individual ? "map" : "scalar",
                url: "/api/edge-partitions/" + (individual ? "individual" : "global"), body: { partitionId: data.partitionId, metric, params } }));
        }
    return { recipes, skipped };
}

function metricRecommendationValue(recipe, response) {
    if (recipe.shape === "scalar") return response.value;
    if (recipe.shape === "aggregate") return response;
    if (recipe.shape === "pairs") {
        const map = {};
        for (const edge of response.values || []) {
            const value = edge.value == null ? NaN : Number(edge.value);
            map[pairKey(edge.source, edge.target)] = value;
            if (!state.directed) map[pairKey(edge.target, edge.source)] = value;
        }
        return map;
    }
    const values = numericMap(response.values || {});
    if (recipe.family === "attrcomm" || recipe.family === "edgeattr") {
        const original = recipe.family === "attrcomm" ? state.attributeMetricData[recipe.label] : state.edgeAttributeMetricData[recipe.label];
        const lookup = new Map();
        for (const [id, label] of Object.entries(response.valueLabels || {})) {
            if (!lookup.has(label)) lookup.set(label, id); else lookup.set(label, null);
        }
        if (Object.keys(response.valueLabels || {}).length) {
            const labels = new Set(Object.keys(original.values).map(id => original.valueLabels?.[id] ?? id));
            return Object.fromEntries([...Object.keys(original.values).map(id => {
                const key = lookup.get(original.valueLabels?.[id] ?? id);
                return [id, key == null ? NaN : values[key]];
            }), ...[...lookup].filter(([label, id]) => id != null && !labels.has(label)).map(([label, id]) => ["attribute-value:" + JSON.stringify(label), values[id]])]);
        }
    }
    return values;
}

function metricRecommendationAlreadyComputed(recipe, key) {
    const results = state.recMetricData?.[recipe.recFamily]?.[recipe.label];
    return !!results && Object.hasOwn(results, key);
}

function metricRecommendationBatchCounts(keys, recipes) {
    let alreadyComputed = 0;
    for (const key of keys) for (const recipe of recipes) if (metricRecommendationAlreadyComputed(recipe, key)) alreadyComputed++;
    const total = keys.length * recipes.length;
    return { total, alreadyComputed, missing: total - alreadyComputed };
}

async function runMetricRecommendationBatch(keys, recipes, signal, progress = () => {}, stopped = () => false) {
    const graphId = state.graphId, graph = state.graph, revision = state.networkRevision;
    const models = new Map(keys.map(key => [key, state.rec.models[key]]));
    const current = () => state.graphId === graphId && state.graph === graph && state.networkRevision === revision;
    let completed = 0, skipped = 0, processed = 0; const errors = [], total = keys.length * recipes.length;
    for (const key of keys) for (const recipe of recipes) {
        if (signal?.aborted || stopped() || !current()) return { completed, errors, stopped: true };
        if (!models.get(key) || models.get(key) !== state.rec.models[key]) {
            errors.push(recLabel(key) + ": recommendation removed or changed");
            processed++; progress(processed, total, recLabel(key), recipe.label + " (recommendation changed)");
            continue;
        }
        if (metricRecommendationAlreadyComputed(recipe, key)) {
            skipped++; processed++; progress(processed, total, recLabel(key), recipe.label + " (already computed)"); continue;
        }
        try {
            const response = await api(recipe.url, { ...jsonBody({ ...recipe.body, graphId, withRecommendation: true, recommendationKey: key }), signal });
            if (response.cancelled || stopped() || !current()) return { completed, errors, stopped: true };
            if (models.get(key) !== state.rec.models[key]) continue;
            if (response.recommendation != null && response.recommendation !== key) throw new Error("The response belongs to a different recommendation.");
            const family = (state.recMetricData[recipe.recFamily] ||= {});
            (family[recipe.label] ||= {})[key] = metricRecommendationValue(recipe, response);
            completed++;
        } catch (error) {
            if (isAbort(error) || signal?.aborted || !current()) return { completed, errors, stopped: true };
            errors.push(recLabel(key) + " · " + recipe.label + ": " + error.message);
        } finally { processed++; progress(processed, total, recLabel(key), recipe.label); }
    }
    progress(processed, total, "", "");
    return { completed, skipped, processed, total, errors, stopped: false };
}

async function stopMetricRecommendationBatch() {
    if (!METRIC_RECOMPUTE.running) return;
    METRIC_RECOMPUTE.stop = true;
    if (METRIC_RECOMPUTE.processId && typeof ProcessPanel !== "undefined")
        ProcessPanel.updateLocal(METRIC_RECOMPUTE.processId, "Cancelling saved metric batch", METRIC_RECOMPUTE.completed, METRIC_RECOMPUTE.total, "metrics", "cancelling");
    const signal = METRIC_RECOMPUTE.signal, job = signal?.jobKey ? state.jobs[signal.jobKey] : null;
    if (!job) return;
    if (job.id && typeof ProcessPanel !== "undefined") await ProcessPanel.cancelJob(job.id, job.graphId);
    else await api("/api/jobs/cancel", jsonBody({ graphId: job.graphId, kind: job.kind }));
}

function openMetricRecommendationBatch() {
    if (!requireGraph()) return;
    const dialog = $("metric-recompute-dialog");
    if (METRIC_RECOMPUTE.running) { dialog.showModal(); return; }
    const inventory = metricComputationInventory(), choices = $("metric-recompute-choices"), note = $("metric-recompute-note");
    choices.replaceChildren();
    for (const key of Object.keys(state.rec.models)) {
        const label = metricWorkspaceElement("label", "metric-series-choice"), input = metricWorkspaceElement("input");
        input.type = "checkbox"; input.value = key;
        label.append(input, metricWorkspaceElement("span", "", recLabel(key))); choices.appendChild(label);
    }
    const run = $("metric-recompute-run"), close = $("metric-recompute-close");
    const selected = () => [...choices.querySelectorAll("input")].filter(input => input.checked).map(input => input.value);
    const inventoryNote = inventory.recipes.length + " saved metric configurations. " + (choices.children.length ? "Choose recommendations to compute against." : "Compute recommendations first.")
        + (inventory.skipped.length ? " Recompute these original metrics once to recover their configuration: " + inventory.skipped.join(", ") : "");
    choices.onchange = () => {
        const keys = selected(), counts = metricRecommendationBatchCounts(keys, inventory.recipes);
        run.disabled = !keys.length || !inventory.recipes.length || counts.missing === 0;
        note.textContent = !keys.length ? inventoryNote : counts.missing
            ? inventoryNote + " " + counts.missing + " missing result(s) will be computed; " + counts.alreadyComputed + " already computed will be skipped."
            : inventoryNote + " All " + counts.alreadyComputed + " selected results are already computed.";
    };
    note.textContent = inventoryNote;
    run.disabled = true; close.disabled = false;
    run.onclick = async () => {
        if (METRIC_RECOMPUTE.running) { await stopMetricRecommendationBatch(); return; }
        const keys = selected(); if (!keys.length) return;
        METRIC_RECOMPUTE.running = true; METRIC_RECOMPUTE.stop = false;
        run.textContent = "Stop";
        METRIC_RECOMPUTE.completed = 0; METRIC_RECOMPUTE.total = keys.length * inventory.recipes.length;
        const signal = beginJob(run.id, "metricBatch"); METRIC_RECOMPUTE.signal = signal;
        const processId = typeof ProcessPanel !== "undefined" ? ProcessPanel.registerLocal("Saved recommendation metrics", state.graphId,
            stopMetricRecommendationBatch, "metricBatch", { recommendations: keys.length, metricsPerRecommendation: inventory.recipes.length }) : null;
        METRIC_RECOMPUTE.processId = processId;
        choices.querySelectorAll("input").forEach(input => { input.disabled = true; });
        try {
            const result = await runMetricRecommendationBatch(keys, inventory.recipes, signal, (done, total, name, metric) => {
                METRIC_RECOMPUTE.completed = done; METRIC_RECOMPUTE.total = total;
                if (processId && typeof ProcessPanel !== "undefined") ProcessPanel.updateLocal(processId,
                    name ? (metric.includes("already computed") ? "Skipping " : "Computing ") + metric + " for " + name : "Finishing saved metric batch", done, total, "metrics");
                note.textContent = done + " / " + total + " processed" + (name ? " · " + name + " · " + metric : "");
            }, () => METRIC_RECOMPUTE.stop);
            note.textContent = (result.stopped ? "Stopped. " : "Finished. ") + result.completed + " metric results computed."
                + (result.skipped ? " " + result.skipped + " already computed and skipped." : "")
                + (result.errors.length ? " Failures: " + result.errors.join("; ") : "");
            renderMetricsDashboard();
            if (processId && typeof ProcessPanel !== "undefined") ProcessPanel.finishLocal(processId,
                result.stopped ? "cancelled" : result.errors.length ? "failed" : "completed", result.errors.join("; ") || null);
        } catch (error) {
            note.textContent = error.message;
            if (processId && typeof ProcessPanel !== "undefined") ProcessPanel.finishLocal(processId, "failed", error.message);
        }
        finally {
            METRIC_RECOMPUTE.running = false; METRIC_RECOMPUTE.signal = null; METRIC_RECOMPUTE.processId = null; close.disabled = false;
            run.textContent = "Compute selected";
            choices.querySelectorAll("input").forEach(input => { input.disabled = false; });
            endJob(run.id, signal);
            const counts = metricRecommendationBatchCounts(selected(), inventory.recipes);
            run.disabled = !selected().length || !inventory.recipes.length || counts.missing === 0;
        }
    };
    dialog.showModal();
}

function setupMetricRecommendationBatch(header) {
    const open = metricWorkspaceAction(metricWorkspaceElement("button"), "calculate", "Compute for recommendations");
    open.addEventListener("click", openMetricRecommendationBatch); header.appendChild(open);
    const dialog = metricWorkspaceElement("dialog", "metric-recompute-dialog"); dialog.id = "metric-recompute-dialog";
    dialog.setAttribute("aria-labelledby", "metric-recompute-title");
    const title = metricWorkspaceElement("h2", "", "Compute saved metrics for recommendations"); title.id = "metric-recompute-title";
    const note = metricWorkspaceElement("p", "muted"); note.id = "metric-recompute-note"; note.setAttribute("aria-live", "polite");
    const choices = metricWorkspaceElement("div", "metric-series-picker"); choices.id = "metric-recompute-choices";
    const actions = metricWorkspaceElement("div", "metric-panel-actions");
    const run = metricWorkspaceElement("button", "primary", "Compute selected"); run.type = "button"; run.id = "metric-recompute-run";
    const close = metricWorkspaceElement("button", "", "Close"); close.type = "button"; close.id = "metric-recompute-close"; close.addEventListener("click", () => dialog.close());
    actions.append(run, close); dialog.append(title, note, choices, actions); document.body.appendChild(dialog);
}
