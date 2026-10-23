const test = require("node:test"), assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), vm = require("node:vm");
const source = fs.readFileSync(path.join(__dirname, "../../main/resources/public/app.js"), "utf8");
function setup(api) {
    const elements = new Map(), requests = [], states = [], iterations = [], errors = [];
    const $ = id => {
        if (!elements.has(id)) elements.set(id, { children: [], style: {}, value: "", replaceChildren(...items) { this.children = items; }, append(item) { this.children.push(item); } });
        return elements.get(id);
    };
    const a = { id: "a", label: "First", recKey: null }, b = { id: "b", label: "Second", recKey: "rec" };
    const result = id => ({ runId: id, numIterations: id === "a" ? 2 : 4, iterations: [] });
    const state = { graphId: "g", diffusion: { runs: [a, b], result: result("b"), iteration: 1, selectedNode: "n", rendererType: "sigma" } };
    let stops = 0, legends = 0;
    const context = vm.createContext({ $, state, DIFF_COL: { base: "grey" },
        option: (value, label) => ({ value, label }),
        jsonBody: body => body, api: async (url, body) => { requests.push({ url, body }); return api ? api(url, body) : result(body.runId); },
        diffStop: () => stops++, buildDiffLegend: () => legends++, drawDiffOverlay() {}, restoreDiffCosmograph() {},
        setDiffIteration: i => iterations.push(i), toggleHidden: (id, value) => $(id).hidden = value,
        renderDiffState: res => states.push(res), setStatus: text => errors.push(text),
    });
    for (const name of ["syncDiffPlaybackRuns", "selectDiffPlaybackRun", "activateDiffusionAnalysisRun", "fetchDiffState"]) {
        const start = source.indexOf("function " + name + "("), end = source.indexOf("\n}", start) + 2;
        vm.runInContext((name.startsWith("select") || name.startsWith("fetch") ? "async " : "") + source.slice(start, end), context);
    }
    return { $, state, a, b, context, requests, states, iterations, errors, result, get stops() { return stops; }, get legends() { return legends; } };
}
test("playback selector includes every retained run and disables metrics-only imports", () => {
    const h = setup(); h.state.diffusion.runs.push({ id: "saved", label: "Saved", archived: true });
    h.context.syncDiffPlaybackRuns();
    const select = h.$("diff-playback-run");
    assert.equal(select.children.length, 3); assert.equal(select.value, "b"); assert.equal(select.disabled, false);
    assert.equal(select.children[2].disabled, true); assert.match(select.children[2].label, /metrics only/);
    h.state.diffusion.runs = []; h.state.diffusion.result = null; h.context.syncDiffPlaybackRuns();
    assert.equal(select.disabled, true); assert.equal(select.value, "");
});
test("selecting a run resets playback and restores that run's network and iteration range", async () => {
    const h = setup(); await h.context.selectDiffPlaybackRun("a");
    assert.equal(h.state.diffusion.result.runId, "a"); assert.equal(h.state.diffusion.recKey, null);
    assert.equal(h.$("diff-slider").max, 1); assert.equal(h.$("diff-playback-run").value, "a");
    assert.equal(h.state.diffusion.iteration, 0); assert.deepEqual(h.iterations, [0]);
    await h.context.selectDiffPlaybackRun("b"); assert.equal(h.state.diffusion.recKey, "rec");
    assert.equal(h.$("diff-slider").max, 3); assert.equal(h.requests[0].body.runId, "a");
});
test("a superseded playback request cannot replace the currently selected run", async () => {
    let resolve; const h = setup(() => new Promise(done => { resolve = done; }));
    const pending = h.context.selectDiffPlaybackRun("a");
    await h.context.selectDiffPlaybackRun("b"); resolve(h.result("a")); await pending;
    assert.equal(h.state.diffusion.result.runId, "b");
    const reset = h.context.selectDiffPlaybackRun("a"); h.state.diffusion.runs = []; h.state.diffusion.result = null;
    resolve(h.result("a")); await reset; assert.equal(h.state.diffusion.result, null);
});
test("unavailable playback restores selection and metrics-only runs never request summaries", async () => {
    const h = setup(() => { throw new Error("Unavailable"); }); await h.context.selectDiffPlaybackRun("a");
    assert.equal(h.state.diffusion.result.runId, "b"); assert.equal(h.$("diff-playback-run").value, "b"); assert.deepEqual(h.errors, ["Unavailable"]);
    h.a.archived = true; await h.context.selectDiffPlaybackRun("a"); assert.equal(h.requests.length, 1);
});
test("node inspection requests the displayed run and discards stale iterations and runs", async () => {
    const pending = []; const h = setup(() => new Promise(done => pending.push(done)));
    const first = h.context.fetchDiffState(); assert.equal(h.requests[0].body.runId, "b");
    h.state.diffusion.iteration = 2; pending.shift()({ node: "n", old: true }); await first; assert.equal(h.states.length, 0);
    const second = h.context.fetchDiffState(); h.state.diffusion.result = h.result("a"); pending.shift()({ node: "n", old: true }); await second;
    assert.equal(h.states.length, 0);
    const third = h.context.fetchDiffState(); pending.shift()({ node: "n" }); await third;
    assert.equal(h.requests.at(-1).body.runId, "a"); assert.equal(h.states.length, 1);
});
