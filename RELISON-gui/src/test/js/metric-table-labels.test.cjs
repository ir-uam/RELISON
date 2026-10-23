const test = require("node:test"), assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), vm = require("node:vm");
const read = file => fs.readFileSync(path.join(__dirname, "../../main/resources/public", file), "utf8");
test("metric parameter cells display authority mode names without rewriting saved metric labels", () => {
    const context = vm.createContext({ state: { defs: { vertex: { HITS: { label: "HITS" } } } } });
    const app = read("app.js"), start = app.indexOf("function formatAuthorityMode(");
    vm.runInContext(app.slice(start, app.indexOf("\n}", start) + 2), context);
    vm.runInContext(read("metric-tables.js"), context);
    for (const [mode, name] of [[true, "Authorities"], [false, "Hubs"]]) {
        const label = "HITS (mode=" + mode + ")";
        const details = context.metricTableDetails(label);
        assert.equal(details.metric, "HITS"); assert.equal(details.parameters, "mode=" + name);
        assert.equal(label, "HITS (mode=" + mode + ")");
    }
    assert.equal(context.metricTableDetails("HITS (mode=false) (estimated)").parameters, "mode=Hubs");
    assert.equal(context.metricTableDetails("HITS (mode=true) · Partition (mode=false)").parameters, "mode=Authorities; Partition (mode=false)");
    assert.equal(context.metricTableDetails("Other metric (mode=true)").parameters, "mode=true");
});
