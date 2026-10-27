const test = require("node:test"), assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), vm = require("node:vm");
const domino = require("@mixmark-io/domino");
const source = fs.readFileSync(path.join(__dirname, "../../main/resources/public/report-notebook.js"), "utf8");

function setup(cells) {
    const document = domino.createWindow("<html></html>").document;
    const context = vm.createContext({ document, report: { cells }, ReportMarkdown: { render: html => html } });
    const start = source.indexOf("    function captionLabels()"), end = source.indexOf("    function insertReference(", start);
    const methods = vm.runInContext(source.slice(start, end) + "\n({ captionLabels, renderText })", context);
    return { ...methods, document, cells };
}

test("references follow their target across separate figure and table numbering", () => {
    const graph = { id: 2, type: "graph", options: { caption: "network" } };
    const chart = { id: 3, type: "chart", options: { caption: "degrees" } };
    const table = { id: 4, type: "table", options: { caption: "nodes" } };
    const h = setup([graph, table, chart]);
    const input = '<p>See <a href="#relison-report-cell-3">reference</a> and <a href="#relison-report-cell-4">reference</a>.</p>';
    let result = h.document.createElement("div"); result.innerHTML = h.renderText(input);
    assert.equal(result.querySelector('[data-report-reference="3"]').textContent, "Figure 2");
    assert.equal(result.querySelector('[data-report-reference="4"]').textContent, "Table 1");
    h.cells.splice(0, 3, chart, graph, table);
    result.innerHTML = h.renderText(input);
    assert.equal(result.querySelector('[data-report-reference="3"]').textContent, "Figure 1");
    assert.equal(result.querySelector('[data-report-reference="4"]').textContent, "Table 1");
    assert.equal(result.querySelector('[data-report-reference="3"]').getAttribute("href"), "#relison-report-cell-3");
});

test("references become visibly missing and recover when the caption returns", () => {
    const target = { id: 7, type: "table", options: { caption: "" } };
    const h = setup([target]), root = h.document.createElement("div");
    const input = '<p><a href="#relison-report-cell-7">reference</a> <code>[reference](#relison-report-cell-7)</code></p>';
    root.innerHTML = h.renderText(input);
    assert.equal(root.querySelector('a').textContent, "Missing reference");
    assert.ok(root.querySelector('a').classList.contains("report-reference-missing"));
    assert.equal(root.querySelector('code').textContent, "[reference](#relison-report-cell-7)");
    target.options.caption = "Restored";
    root.innerHTML = h.renderText(input);
    assert.equal(root.querySelector('a').textContent, "Table 1");
    assert.equal(root.querySelector('a').classList.contains("report-reference-missing"), false);
    h.cells.pop(); root.innerHTML = h.renderText(input);
    assert.equal(root.querySelector('a').textContent, "Missing reference");
});
