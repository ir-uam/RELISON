const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const source = fs.readFileSync(path.join(__dirname, "../../main/resources/public/app.js"), "utf8");

function extract(name) {
    const start = source.indexOf("function " + name + "(");
    assert.ok(start >= 0, name);
    return (source.slice(start - 6, start) === "async " ? "async " : "")
        + source.slice(start, source.indexOf("\n}", start) + 2);
}

function setup() {
    class Element {
        constructor(tag) { this.tagName = tag; this.children = []; this.value = ""; }
        appendChild(child) { this.children.push(child); }
        replaceChildren() { this.children = []; }
    }
    const elements = Object.fromEntries(["rec-computed", "rec-edge-show", "rec-edge-model", "rec-edge-mode", "btn-rec-add-all", "rec-status"]
        .map(id => [id, new Element(id)]));
    const recommendation = "Common Neighbours · recommendation · n=10";
    const prediction = "Common Neighbours · prediction · n=5";
    const reciprocal = "Common Neighbours · recommendation · n=10 · recip";
    const state = { graphId: "graph", rec: { active: recommendation, selecting: false,
        models: {
            [prediction]: { label: "Common Neighbours", mode: "prediction", cutoff: 5, edges: [{ source: "b", target: "a", score: 3 }] },
            [recommendation]: { label: "Common Neighbours", mode: "recommendation", cutoff: 10, edges: [{ source: "a", target: "c", score: 2 }] },
            [reciprocal]: { label: "Common Neighbours", mode: "recommendation", cutoff: 10, edges: [] },
        }, tableEdges: [], show: false }, tables: { rec: { filters: { source: "a" }, page: 4, headerSig: "old" } } };
    const requests = [], refreshes = [], messages = [];
    const context = vm.createContext({ state, $: id => elements[id],
        document: { createElement: tag => new Element(tag), querySelectorAll: () => [] },
        toggleHidden() {}, drawRecOverlay: () => refreshes.push("overlay"), applyReducers() {},
        renderTable: key => refreshes.push(key), refreshAfterCompute: () => refreshes.push("views"),
        setStatus: message => messages.push(message), jobBusy: () => false,
        jsonBody: body => body, api: async (url, body) => { requests.push({ url, body }); return { key: body.key }; } });
    vm.runInContext(["formatAuthorityMode", "renderComputedRecommendations", "activateRecommendation", "selectComputedRecommendation", "updateRecUI", "setRecommendationDisplay"]
        .map(extract).join("\n"), context);
    return { context, state, elements, requests, refreshes, messages, recommendation, prediction, reciprocal };
}

test("computed selector groups every run by task and preserves distinct reciprocal settings", () => {
    const h = setup();
    h.context.updateRecUI();
    const select = h.elements["rec-computed"];
    const groups = select.children.filter(el => el.tagName === "optgroup");
    assert.deepEqual(groups.map(group => group.label), ["Recommendations", "Predictions"]);
    assert.deepEqual(groups[0].children.map(option => option.value), [h.recommendation, h.reciprocal]);
    assert.deepEqual(groups[1].children.map(option => option.value), [h.prediction]);
    assert.equal(select.value, h.recommendation);
    assert.equal(h.elements["rec-edge-model"].value, h.recommendation);
    assert.equal(h.elements["rec-edge-mode"].value, "none");
    assert.equal(select.disabled, false);
    h.state.rec.models = {}; h.state.rec.active = null;
    h.context.updateRecUI();
    assert.equal(select.children.length, 1);
    assert.equal(select.children[0].textContent, "No computed results");
    assert.equal(select.disabled, true);
    assert.equal(h.elements["rec-edge-model"].disabled, true);
    assert.equal(h.elements["rec-edge-mode"].disabled, true);
});

test("selecting a cached prediction synchronizes the server and refreshes table, overlay and dependent views", async () => {
    const h = setup();
    await h.context.selectComputedRecommendation(h.prediction);
    assert.equal(h.requests.length, 1);
    assert.equal(h.requests[0].url, "/api/recommendation/select");
    assert.equal(h.requests[0].body.graphId, "graph");
    assert.equal(h.requests[0].body.key, h.prediction);
    assert.equal(h.state.rec.active, h.prediction);
    assert.equal(h.state.rec.tableEdges, h.state.rec.models[h.prediction].edges);
    assert.equal(h.state.tables.rec.page, 0);
    assert.equal(Object.keys(h.state.tables.rec.filters).length, 0);
    assert.equal(h.elements["rec-edge-show"].checked, true);
    assert.equal(h.elements["rec-computed"].value, h.prediction);
    assert.equal(h.elements["rec-edge-model"].value, h.prediction);
    assert.deepEqual(h.refreshes, ["overlay", "rec", "views"]);
    assert.match(h.elements["rec-status"].textContent, /prediction, cutoff 5, 1 links/);
    await h.context.selectComputedRecommendation(h.prediction);
    assert.equal(h.requests.length, 1);
});

test("a rejected selection restores the current result and re-enables the selector", async () => {
    const h = setup();
    h.context.api = async () => { throw new Error("A process is using this graph."); };
    await h.context.selectComputedRecommendation(h.prediction);
    assert.equal(h.state.rec.active, h.recommendation);
    assert.equal(h.elements["rec-computed"].value, h.recommendation);
    assert.equal(h.elements["rec-computed"].disabled, false);
    assert.equal(h.elements["rec-edge-model"].value, h.recommendation);
    assert.equal(h.elements["rec-edge-model"].disabled, false);
    assert.equal(h.refreshes.length, 0);
    assert.match(h.messages[0], /process is using this graph/);
});

test("network display modes update visibility without changing the selected recommendation", () => {
    const h = setup();
    let reductions = 0; h.context.applyReducers = () => reductions++;
    for (const [mode, show, only] of [["only", true, true], ["highlight", true, false], ["none", false, false]]) {
        h.context.setRecommendationDisplay(mode);
        assert.equal(h.state.rec.show, show); assert.equal(h.state.rec.only, only);
        assert.equal(h.state.rec.active, h.recommendation); assert.equal(h.elements["rec-edge-mode"].value, mode);
        assert.equal(h.elements["rec-edge-model"].value, h.elements["rec-computed"].value);
    }
    assert.equal(reductions, 3); assert.equal(h.requests.length, 0);
});

test("recommendation selectors display named Hubs and Authorities modes while retaining raw result keys", () => {
    const h = setup(); h.state.rec.models = {};
    for (const name of ["HITS", "SALSA", "Personalized HITS", "Personalized SALSA", "Money", "Love"]) {
        for (const mode of [true, false]) {
            const key = name + " (mode=" + mode + ", alpha=0.5) · recommendation · n=10";
            h.state.rec.models[key] = { label: name + " (mode=" + mode + ", alpha=0.5)", mode: "recommendation", edges: [], cutoff: 10 };
        }
    }
    h.context.renderComputedRecommendations();
    for (const id of ["rec-computed", "rec-edge-model"]) {
        const options = h.elements[id].children.find(item => item.tagName === "optgroup").children;
        assert.equal(options.length, 12);
        for (const option of options) {
            assert.match(option.value, /mode=(true|false)/);
            assert.match(option.textContent, option.value.includes("mode=true") ? /mode=Authorities/ : /mode=Hubs/);
        }
    }
});

test("selecting a result preserves recommended-only mode and keeps both result selectors synchronized", async () => {
    const h = setup(); h.context.setRecommendationDisplay("only");
    await h.context.selectComputedRecommendation(h.prediction);
    assert.equal(h.elements["rec-edge-mode"].value, "only");
    assert.equal(h.elements["rec-edge-model"].value, h.prediction); assert.equal(h.elements["rec-computed"].value, h.prediction);
});

test("a pending selection cannot activate a result after its graph or models are replaced", async () => {
    const h = setup();
    let resolve;
    h.context.api = () => new Promise(done => { resolve = done; });
    const selecting = h.context.selectComputedRecommendation(h.prediction);
    assert.equal(h.elements["rec-computed"].disabled, true);
    h.state.graphId = "new graph"; h.state.rec.models = {}; h.state.rec.active = null;
    resolve({ key: h.prediction });
    await selecting;
    assert.equal(h.state.rec.active, null);
    assert.equal(h.refreshes.length, 0);
    assert.equal(h.elements["rec-computed"].children[0].textContent, "No computed results");
});

test("selection waits for an in-progress recommendation and keeps exhausted runs selectable", async () => {
    const h = setup();
    h.context.jobBusy = () => true;
    await h.context.selectComputedRecommendation(h.prediction);
    assert.equal(h.requests.length, 0);
    assert.equal(h.elements["rec-computed"].value, h.recommendation);
    h.context.jobBusy = () => false;
    await h.context.selectComputedRecommendation(h.reciprocal);
    assert.equal(h.state.rec.active, h.reciprocal);
    assert.equal(h.elements["btn-rec-add-all"].disabled, true);
    assert.match(h.elements["rec-status"].textContent, /all recommended links have been added/);
});
