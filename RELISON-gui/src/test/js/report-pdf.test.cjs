const test = require("node:test"), assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), vm = require("node:vm");
const domino = require("@mixmark-io/domino");
const pdfMake = require("pdfmake/build/pdfmake.js");
pdfMake.vfs = require("pdfmake/build/vfs_fonts.js");
const source = fs.readFileSync(path.join(__dirname, "../../main/resources/public/report-pdf.js"), "utf8");

function setup() {
    const document = domino.createWindow("<html><body></body></html>").document;
    const context = vm.createContext({ document, pdfMake, setTimeout, clearTimeout,
        Image: class { set src(_) { queueMicrotask(() => this.onerror()); } } });
    return vm.runInContext(source + "\nReportPdf", context);
}

test("PDF model retains formatting, caption destinations and unavailable content", async () => {
    const pdf = setup();
    const model = { title: "Network report", cells: [
        { type: "text", html: '<h1>Summary</h1><p><strong>Bold</strong> <em>italic</em> <u>underlined</u> <a href="#relison-report-cell-4">Figure 1</a></p><ul><li>one</li><li>two</li></ul><table><thead><tr><th>Node</th><th>Degree</th></tr></thead><tbody><tr><td>Ada</td><td>4</td></tr></tbody></table><p><img src="data:image/png;base64,AAAA" alt="bad image"></p>' },
        { id: 4, type: "graph", label: "Figure 1", caption: "Network view", placeholder: "No snapshot captured." },
        { id: 5, type: "table", placeholder: "Top-k table output is not generated yet." },
    ] };
    const { definition, unavailableImages } = await pdf.buildDefinition(model, "LETTER", "landscape");
    assert.equal(definition.pageSize, "LETTER"); assert.equal(definition.pageOrientation, "landscape");
    assert.equal(unavailableImages, 1);
    const paragraph = definition.content.find(item => Array.isArray(item.text) && item.text.some(part => part.text === "Figure 1"));
    assert.equal(paragraph.text.find(part => part.text === "Bold").bold, true);
    assert.equal(paragraph.text.find(part => part.text === "italic").italics, true);
    assert.equal(paragraph.text.find(part => part.text === "underlined").decoration, "underline");
    assert.equal(paragraph.text.find(part => part.text === "Figure 1").linkToDestination, "report-cell-4");
    assert.equal(definition.content.find(item => item.table).table.body.length, 2);
    assert.equal(definition.content.find(item => item.ul).ul.length, 2);
    assert.match(definition.content.find(item => typeof item.text === "string" && item.text.startsWith("[Image unavailable")).text, /bad image/);
    const figure = definition.content.find(item => item.stack?.some(block => block.id === "report-cell-4"));
    assert.equal(figure.unbreakable, true); assert.equal(figure.stack[1].text[1].text, "Network view");
    assert.ok(definition.content.some(item => /not generated/.test(item.text || "")));
    await assert.rejects(pdf.buildDefinition(model, "invalid", "portrait"), /valid page size/);
});

test("local PDF engine generates multiple landscape Letter pages with named references", async () => {
    const pdf = setup();
    const paragraphs = Array.from({ length: 120 }, (_, i) => `<p>Paragraph ${i}: a moderately long network analysis paragraph with several words and metrics.</p>`).join("");
    const { definition } = await pdf.buildDefinition({ title: "Network", cells: [
        { type: "text", html: '<p>See <a href="#relison-report-cell-2">Figure 1</a>.</p>' + paragraphs },
        { type: "graph", id: 2, label: "Figure 1", caption: "Snapshot", placeholder: "No snapshot captured." },
    ] }, "LETTER", "landscape");
    const bytes = await new Promise(resolve => pdfMake.createPdf(definition).getBuffer(resolve));
    const raw = Buffer.from(bytes).toString("latin1");
    assert.match(raw, /^%PDF-/); assert.ok((raw.match(/\/Type \/Page\b/g) || []).length > 1);
    assert.match(raw, /\/MediaBox \[0 0 792 612\]/); assert.match(raw, /report-cell-2/);
});
