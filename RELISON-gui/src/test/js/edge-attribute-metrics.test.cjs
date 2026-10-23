const test = require("node:test"), assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), vm = require("node:vm");
const read = file => fs.readFileSync(path.join(__dirname, "../../main/resources/public", file), "utf8");
function extract(file, name) {
    const source = read(file), start = source.indexOf("function " + name + "(");
    assert.ok(start >= 0, name);
    return (source.slice(start - 6, start) === "async " ? "async " : "") + source.slice(start, source.indexOf("\n}", start) + 2);
}
const plain = value => JSON.parse(JSON.stringify(value));

test("categorical, string and boolean attributes offer partition metrics; time and numeric types keep basic metrics", () => {
    const elements = {}, types = ["categorical", "string", "bool", "time", "int", "long", "double"];
    const defs = types.map(type => ({ name: type, type, numeric: ["int", "long", "double"].includes(type) }));
    const state = { catalog: {
        edgePartitionGlobal: [{ id: "num-groups", label: "Groups", params: [] }],
        edgePartitionIndividual: [{ id: "edge-share", label: "Edge share", params: [] }],
        intraEdgePartitionIndividual: [{ id: "intra-edge-partition:ASL", label: "Intra-partition ASL", params: [{ name: "orientation" }] }],
    }, defs: {} };
    let renders = 0;
    const context = vm.createContext({ state, $: id => elements[id] ||= { value: "", dataset: {} }, edgeAttrDefs: () => defs,
        jobBusy: () => false, indexById: items => Object.fromEntries(items.map(def => [def.id, def])),
        setSelectOptions(id, values, labels) {
            const element = elements[id] ||= { value: "", dataset: {} };
            element.values = values; element.labels = labels;
            if (!values.includes(element.value)) element.value = values[0] || "";
        }, renderParams() { renders++; } });
    vm.runInContext(["syncEdgeAttributeMetricControls", "renderEdgeAttributeMetricParams"].map(name => extract("app.js", name)).join("\n"), context);
    for (const type of types) {
        context.$("edge-attribute-metric-attribute").value = type;
        context.syncEdgeAttributeMetricControls();
        const basicOnly = defs.find(def => def.name === type).numeric || type === "time";
        assert.equal(elements["edge-attribute-global-metric"].values.length, basicOnly ? 1 : 2);
        assert.equal(elements["edge-attribute-indiv-metric"].values.length, basicOnly ? 1 : 3);
    }
    context.$("edge-attribute-metric-attribute").value = "bool";
    context.syncEdgeAttributeMetricControls();
    elements["edge-attribute-indiv-metric"].value = "intra-edge-partition:ASL";
    context.renderEdgeAttributeMetricParams(false, true);
    assert.equal(elements["edge-attribute-indiv-metric"].labels.at(-1), "Intra-attribute ASL");
    const before = renders;
    context.syncEdgeAttributeMetricControls();
    assert.equal(renders, before);
});

test("metric values align by typed category across computations, inspector rows and recommendations", () => {
    const state = { edgeAttributeMetricData: {}, graph: { size: 3 } };
    const categories = { 0: null, 1: "(missing)", 2: false, 3: "false" };
    const context = vm.createContext({ state, edgePartitionGroupEdges: id => [{ id }], edgePartitionEdgeKey: edge => edge.id,
        edgeAttrVal: id => categories[id], numericMap: values => values });
    vm.runInContext(extract("app.js", "edgeAttributeMetricValues"), context);
    vm.runInContext(extract("edge-partition-explorer.js", "edgePartitionGroupValue"), context);
    vm.runInContext(read("metric-recommendations.js"), context);
    const original = context.edgeAttributeMetricValues({ values: { 0: 10, 1: 20, 2: 30, 3: 40 }, categoryValues: categories });
    const reordered = context.edgeAttributeMetricValues({ values: { 9: 40, 7: 20, 4: 10, 5: 30 },
        categoryValues: { 9: "false", 7: "(missing)", 4: null, 5: false } });
    assert.deepEqual(plain(original.values), plain(reordered.values));
    state.edgeAttributeMetricData.metric = { attribute: "kind", ...original };
    for (const id of [0, 1, 2, 3])
        assert.equal(context.edgePartitionGroupValue({ id }, "attribute-metric:metric"), (id + 1) * 10);
    // JSON round trips must preserve inspector matching without runtime-only Map caches.
    state.edgeAttributeMetricData.metric = plain(state.edgeAttributeMetricData.metric);
    assert.equal(context.edgePartitionGroupValue({ id: 2 }, "attribute-metric:metric"), 30);
    const mapped = context.metricRecommendationValue({ family: "edgeattr", label: "metric", shape: "map" },
        { values: { 6: 31, 4: 11 }, categoryValues: { 6: false, 4: null } });
    assert.equal(mapped['attribute-value:false'], 31);
    assert.equal(mapped['attribute-value:null'], 11);
    assert.ok(Number.isNaN(mapped['attribute-value:"false"']));
});

test("attribute edits discard that attribute's individual and global results and recommendation recipes", () => {
    const state = { edgeAttributeMetricOrder: ["share", "other"],
        edgeAttributeMetricData: { share: { attribute: "kind" }, other: { attribute: "other" } },
        graphMetrics: { entropy: 1, unrelated: 2 },
        globalMetricScopes: { entropy: { family: "edgeattr", scope: "kind" }, unrelated: { family: "global" } },
        recMetricData: { edgeattr: { share: {}, other: {} }, graph: { entropy: {}, unrelated: {} } },
        metricComputations: { old: { family: "edgeattr", body: { attribute: "kind" } },
            keep: { family: "edgeattr", body: { attribute: "other" } } } };
    const context = vm.createContext({ state });
    vm.runInContext(extract("app.js", "clearEdgeAttributeMetricResults"), context);
    context.clearEdgeAttributeMetricResults("kind");
    assert.deepEqual(plain(state.edgeAttributeMetricOrder), ["other"]);
    assert.deepEqual(plain(state.graphMetrics), { unrelated: 2 });
    assert.deepEqual(Object.keys(state.recMetricData.edgeattr), ["other"]);
    assert.deepEqual(Object.keys(state.recMetricData.graph), ["unrelated"]);
    assert.deepEqual(Object.keys(state.metricComputations), ["keep"]);
});

test("category renames retain metrics, globals and recipes and remap chart and recommendation keys", () => {
    const old = 'attribute-value:"old"', renamed = 'attribute-value:"new"';
    const state = { edgeAttributeMetricOrder: ["share", "legacy", "other"], edgeAttributeMetricData: {
        share: { attribute: "kind", values: { [old]: 0, keep: 8 }, valueLabels: { [old]: "old", keep: "keep" }, categoryValues: { [old]: "old", keep: "keep" } },
        legacy: { attribute: "kind", values: { 2: 9 }, valueLabels: { 2: "old" } },
        other: { attribute: "other", values: { [old]: 3 }, valueLabels: { [old]: "old" } },
    }, graphMetrics: { entropy: 7 }, globalMetricScopes: { entropy: { family: "edgeattr", scope: "kind" } },
        recMetricData: { edgeattr: { share: { model: { [old]: 5, keep: 6 } }, legacy: { model: { 2: 10 } } }, graph: { entropy: { model: 4 } } },
        metricComputations: { recipe: { family: "edgeattr", body: { attribute: "kind", metric: "edge-share" } } } };
    const context = vm.createContext({ state });
    vm.runInContext(extract("app.js", "relabelEdgeAttributeMetricResults"), context);
    const before = plain({ globals: state.graphMetrics, recipes: state.metricComputations });
    context.relabelEdgeAttributeMetricResults("kind", "old", "new");
    assert.equal(state.edgeAttributeMetricData.share.values[renamed], 0);
    assert.equal(state.edgeAttributeMetricData.share.valueLabels[renamed], "new");
    assert.equal(state.edgeAttributeMetricData.share.categoryValues[renamed], "new");
    assert.equal(state.edgeAttributeMetricData.share.values[old], undefined);
    assert.equal(state.edgeAttributeMetricData.legacy.values[2], 9);
    assert.equal(state.edgeAttributeMetricData.legacy.valueLabels[2], "new");
    assert.equal(state.recMetricData.edgeattr.share.model[renamed], 5);
    assert.equal(state.recMetricData.edgeattr.share.model[old], undefined);
    assert.equal(state.edgeAttributeMetricData.other.valueLabels[old], "old");
    assert.deepEqual(plain({ globals: state.graphMetrics, recipes: state.metricComputations }), before);
    assert.equal(state.recMetricData.graph.entropy.model, 4);
    // Saving the same label again must retain the values too.
    context.relabelEdgeAttributeMetricResults("kind", "new", "new");
    assert.equal(state.edgeAttributeMetricData.share.values[renamed], 0);
});

test("renaming missing values preserves original metrics but invalidates augmented results which can split", () => {
    const state = { edgeAttributeMetricOrder: ["count"], edgeAttributeMetricData: {
        count: { attribute: "kind", values: { 'attribute-value:null': 2 }, valueLabels: { 'attribute-value:null': "(missing)" }, categoryValues: { 'attribute-value:null': null } },
    }, graphMetrics: { groups: 1 }, globalMetricScopes: { groups: { family: "edgeattr", scope: "kind" } },
        recMetricData: { edgeattr: { count: { model: { 'attribute-value:null': 3 } } }, graph: { groups: { model: 1 } } } };
    const context = vm.createContext({ state });
    vm.runInContext(extract("app.js", "relabelEdgeAttributeMetricResults"), context);
    context.relabelEdgeAttributeMetricResults("kind", null, "known");
    assert.equal(state.edgeAttributeMetricData.count.values['attribute-value:"known"'], 2);
    assert.equal(state.graphMetrics.groups, 1);
    assert.equal(state.recMetricData.edgeattr.count, undefined);
    assert.equal(state.recMetricData.graph.groups, undefined);
});

test("metric removal warns only for affected results, including node attributes and recommendation-only removal", () => {
    const messages = [];
    const state = { edgeAttributeMetricData: { edge: { attribute: "kind" } }, attributeMetricData: { node: { attribute: "kind" } },
        graphMetrics: { global: 1 }, globalMetricScopes: { global: { family: "edgeattr", scope: "kind" } },
        commMetricData: { community: { algorithm: "attribute:kind" } }, recMetricData: { edgeattr: { edge: { model: {} } }, comm: {}, graph: {} } };
    const context = vm.createContext({ state, window: { confirm: message => { messages.push(message); return false; } } });
    vm.runInContext(["attributeMetricResultKeys", "confirmAttributeMetricRemoval"].map(name => extract("app.js", name)).join("\n"), context);
    assert.equal(context.confirmAttributeMetricRemoval("edge", "absent", "Changing a value"), true);
    assert.equal(messages.length, 0);
    assert.equal(context.confirmAttributeMetricRemoval("edge", "kind", "Merging categories"), false);
    assert.match(messages.at(-1), /2 computed metric result.*edge attribute "kind"/);
    assert.equal(context.confirmAttributeMetricRemoval("node", "kind", "Merging categories"), false);
    assert.match(messages.at(-1), /2 computed metric result.*node attribute "kind"/);
    assert.equal(context.confirmAttributeMetricRemoval("edge", "kind", "Renaming missing values", true), false);
    assert.match(messages.at(-1), /1 recommendation metric result/);
    const before = messages.length;
    assert.equal(context.confirmAttributeMetricRemoval("node", "kind", "Renaming missing values", true), true);
    assert.equal(messages.length, before);
});

test("canceling the warning for an edge category merge never writes attributes or removes metrics", async () => {
    const requests = [], data = { type: "edge-attribute", partitionId: "p", configuration: { attribute: "kind" } };
    const elements = { "edge-partition-label": { value: "other" }, "edge-partition-save-label": { disabled: false } };
    const state = { graphId: "g", graph: {}, networkRevision: 1 };
    const context = vm.createContext({ state, EDGE_PARTITION_VIEW: { data }, $: id => elements[id],
        selectedEdgePartitionGroup: () => ({ id: 0 }), edgeAttrDefs: () => [{ name: "kind", type: "categorical" }],
        edgePartitionGroupEdges: () => ["e"], edgePartitionEdgeKey: edge => edge,
        jsonBody: body => body, api: async (url, body) => { requests.push(body); return { membershipChanged: true, previousValue: "old" }; },
        confirmAttributeMetricRemoval: () => false, setStatus: () => {} });
    vm.runInContext(extract("edge-partition-explorer.js", "saveEdgePartitionLabel"), context);
    await context.saveEdgePartitionLabel();
    assert.equal(requests.length, 1);
    assert.equal(requests[0].preview, true);
    assert.equal(state.networkRevision, 1);
    assert.equal(elements["edge-partition-save-label"].disabled, false);
});

test("simple node attribute renames retain values while a merge clears affected node metrics only", () => {
    const state = { attributeMetricOrder: ["metric", "other"], attributeMetricData: {
        metric: { attribute: "kind", values: { 0: 9 }, valueLabels: { 0: "old" } }, other: { attribute: "other" },
    }, commMetricOrder: ["community"], commMetricData: { community: { algorithm: "attribute:kind" } },
        graphMetrics: { global: 2 }, globalMetricScopes: { global: { family: "attrcomm", scope: "kind" } },
        recMetricData: { comm: {}, graph: {} } };
    const context = vm.createContext({ state });
    vm.runInContext(extract("app.js", "attributeMetricResultKeys"), context);
    vm.runInContext(extract("community-explorer.js", "updateNodeAttributeMetricLabels"), context);
    context.updateNodeAttributeMetricLabels("kind", "old", "new", false);
    assert.equal(state.attributeMetricData.metric.valueLabels[0], "new");
    assert.equal(state.attributeMetricData.metric.values[0], 9);
    assert.equal(state.graphMetrics.global, 2);
    context.updateNodeAttributeMetricLabels("kind", "new", "other", true);
    assert.equal(state.attributeMetricData.metric, undefined);
    assert.deepEqual(plain(state.attributeMetricOrder), ["other"]);
    assert.deepEqual(plain(state.commMetricOrder), []);
    assert.equal(state.graphMetrics.global, undefined);
});

test("graph edit warnings detect metrics held only by attributes, all-pairs analyses or edge partitions", () => {
    const state = { metricOrder: [], pairOrder: [], commMetricOrder: [], graphMetrics: {}, communityData: {},
        nodePairOrder: [], attributeMetricOrder: [], edgeAttributeMetricOrder: [] };
    const partitions = {};
    const context = vm.createContext({ state, EDGE_PARTITION_VIEW: { partitions } });
    vm.runInContext(extract("app.js", "computedResultsList"), context);
    assert.deepEqual(plain(context.computedResultsList()), []);
    for (const order of ["nodePairOrder", "attributeMetricOrder", "edgeAttributeMetricOrder"]) {
        state[order] = ["result"];
        assert.deepEqual(plain(context.computedResultsList()), ["computed metrics"]);
        state[order] = [];
    }
    partitions.p = { individualMetrics: { size: { values: { 0: 2 } } } };
    assert.deepEqual(plain(context.computedResultsList()), ["computed metrics"]);
});

test("canceling undo or redo leaves graph history untouched", async () => {
    let edited = false;
    const entry = { label: "add node", clearsGraphResults: true, undo: () => { edited = true; }, redo: () => { edited = true; } };
    const state = { history: { undo: [entry], redo: [], applying: false } };
    const context = vm.createContext({ state, confirmClears: () => false });
    vm.runInContext(["historyUndo", "historyRedo"].map(name => extract("app.js", name)).join("\n"), context);
    await context.historyUndo();
    assert.equal(state.history.undo.length, 1); assert.equal(edited, false);
    state.history.undo = []; state.history.redo = [entry];
    await context.historyRedo();
    assert.equal(state.history.redo.length, 1); assert.equal(edited, false);
});
