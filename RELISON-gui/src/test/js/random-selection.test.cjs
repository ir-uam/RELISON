/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/public/app.js'), 'utf8');
const start = source.indexOf('async function selectRandomNetworkElement(');
const handler = source.slice(start, source.indexOf('\n}', start) + 2);
function setup(type, random = [0]) {
    const inputs = new Map(), calls = [], working = { nodes: new Set(['b', 'c']), edges: new Set(['parallel-1', 'parallel-2', 'self-loop']) };
    const $ = id => { if (!inputs.has(id)) inputs.set(id, { value: id === 'select-path-scope' ? 'filtered' : '' }); return inputs.get(id); };
    let index = 0;
    const context = vm.createContext({ $, Math: { floor: Math.floor, random: () => random[index++ % random.length] },
        state: { selection: { type }, graph: { nodes: () => ['a', 'b', 'c'] } },
        requireGraph: () => true, networkWorkingSet: () => working, setStatus: message => calls.push(['status', message]),
        selectNode: id => calls.push(['node', id]), selectEdge: id => calls.push(['edge', id]),
        findSelectedPaths: async () => calls.push(['path', $('select-path-source').value, $('select-path-target').value]),
    });
    vm.runInContext(handler, context);
    return { $, calls, working, context, run: () => context.selectRandomNetworkElement() };
}
test('random node uses current filters and the standard selection handler', async () => {
    const h = setup('node', [0.99]); await h.run(); assert.deepEqual(h.calls, [['node', 'c']]);
});
test('random edge samples individual parallel edges and self loops', async () => {
    const h = setup('edge', [0, 0.5, 0.99]);
    await h.run(); await h.run(); await h.run();
    assert.deepEqual(h.calls, [['edge', 'parallel-1'], ['edge', 'parallel-2'], ['edge', 'self-loop']]);
});
test('random pair contains distinct endpoints and starts the normal path search', async () => {
    for (const value of [0, 0.5, 0.99]) {
        const h = setup('path', [value]); await h.run();
        assert.equal(h.calls[0][0], 'path'); assert.notEqual(h.calls[0][1], h.calls[0][2]);
        assert.ok(h.calls[0].slice(1).every(id => h.working.nodes.has(id)));
    }
});
test('whole-graph scope can choose endpoints outside filters', async () => {
    const h = setup('path', [0]); h.$('select-path-scope').value = 'whole'; await h.run();
    assert.deepEqual(h.calls, [['path', 'a', 'b']]);
});
test('empty candidates, insufficient endpoints and absent graphs leave selection unchanged', async () => {
    for (const type of ['node', 'edge', 'path']) {
        const h = setup(type); h.working.nodes.clear(); h.working.edges.clear(); await h.run();
        assert.equal(h.calls.length, 1); assert.equal(h.calls[0][0], 'status');
    }
    const h = setup('path'); h.working.nodes = new Set(['a']); await h.run(); assert.equal(h.calls[0][0], 'status');
    h.calls.length = 0; h.context.requireGraph = () => false; await h.run(); assert.equal(h.calls.length, 0);
});
