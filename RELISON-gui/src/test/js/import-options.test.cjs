/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
const test = require('node:test'), assert = require('node:assert/strict');
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/public/app.js'), 'utf8');
function extract(name) {
    const start = source.indexOf('function ' + name + '(');
    return (source.slice(start - 6, start) === 'async ' ? 'async ' : '') + source.slice(start, source.indexOf('\n}', start) + 2);
}
function setup(explicit, filename) {
    const elements = new Map(), requests = [], opened = [], loaded = [];
    const $ = id => { if (!elements.has(id)) elements.set(id, { value: '', checked: false, dataset: {}, files: [] }); return elements.get(id); };
    $('opt-format').value = explicit; if (filename) $('file-input').files = [{ name: filename }];
    const context = vm.createContext({ $, toggleHidden: (id, hidden) => { $(id).hidden = hidden; },
        FormData: class extends Map { append(key, value) { this.set(key, value); } },
        selectedDelimiter: () => '\t', setStatus() {}, openSession: async file => opened.push(file),
        api: async (url, options) => { requests.push(options.body); return { stats: { directed: false, nodes: 3, edges: 4 } }; },
        applyLoadedGraph: (data, multi) => loaded.push(multi),
    });
    vm.runInContext(['selectedImportFormat', 'updateImportFormatHint', 'loadGraph'].map(extract).join('\n'), context);
    return { $, context, requests, opened, loaded };
}
test('manual and auto-detected formats expose only applicable controls', () => {
    for (const [format, filename] of [['edges', 'g.gexf'], ['pajek', 'g.txt'], ['gexf', 'g.txt'], ['relison', 'g.txt'], ['', 'g.csv'], ['', 'g.NET'], ['', 'g.GEXF'], ['', 'g.relison']]) {
        const h = setup(format, filename); h.context.updateImportFormatHint();
        const selected = h.context.selectedImportFormat();
        assert.equal(h.$('edge-import-options').hidden, selected !== 'edges');
        assert.equal(h.$('edge-header-option').hidden, selected !== 'edges');
        assert.equal(h.$('gexf-label-option').hidden, selected !== 'gexf');
        assert.equal(h.$('import-type-options').hidden, ['gexf', 'relison'].includes(selected));
        assert.equal(h.$('btn-load').textContent, selected === 'relison' ? 'Open session' : 'Load network');
    }
});
test('GEXF preserves file features regardless of hidden checkbox values', async () => {
    const h = setup('', 'g.gexf'); h.$('opt-gexf-labels-as-ids').checked = true;
    await h.context.loadGraph(); const form = h.requests[0];
    assert.equal(form.get('format'), 'gexf');
    for (const key of ['weighted', 'multigraph', 'selfloops']) assert.equal(form.get(key), true);
    assert.equal(form.get('labelsAsIds'), true);
    assert.equal(form.has('separator'), false); assert.equal(form.has('header'), false);
    assert.deepEqual(h.loaded, [true]);
});
test('explicit format overrides filename during loading and session handling', async () => {
    const h = setup('edges', 'g.relison'); await h.context.loadGraph();
    assert.equal(h.opened.length, 0); assert.equal(h.requests[0].get('format'), 'edges');
    assert.equal(h.requests[0].has('header'), true); assert.equal(h.requests[0].has('labelsAsIds'), false);
    const session = setup('', 'g.relison'); await session.context.loadGraph();
    assert.equal(session.opened.length, 1); assert.equal(session.requests.length, 0);
});
test('GEXF loader uses server-detected multiplicity rather than permissive import flags', async () => {
    for (const multigraph of [false, true]) {
        const h = setup('', 'g.gexf');
        h.context.api = async () => ({ stats: { directed: true, multigraph, nodes: 3, edges: 4 } });
        await h.context.loadGraph();
        assert.deepEqual(h.loaded, [multigraph]);
    }
});
test('switching formats preserves user flags and manual separators', () => {
    const h = setup('edges', 'g.csv'); h.$('opt-directed').checked = true;
    h.$('opt-separator').value = ';'; h.$('opt-separator').dataset.manual = '1';
    h.$('opt-format').value = 'gexf'; h.context.updateImportFormatHint();
    h.$('opt-format').value = 'edges'; h.context.updateImportFormatHint();
    assert.equal(h.$('opt-directed').checked, true); assert.equal(h.$('opt-separator').value, ';');
});
