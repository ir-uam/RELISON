/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/public/app.js'), 'utf8');
function extract(name) {
    const start = source.indexOf('function ' + name + '(');
    return source.slice(start, source.indexOf('\n}', start) + 2);
}
function setup() {
    const fields = new Map(), $ = id => { if (!fields.has(id)) fields.set(id, { value: '', checked: false }); return fields.get(id); };
    const graph = { nodes: () => ['2', '10', '1'], degree: id => ({ '1': 4, '10': 2, '2': 2 })[id] };
    const context = vm.createContext({ $, state: { metricData: { score: { '1': 3, '10': -1, '2': 2 } } },
        isFlat3DLayout: type => type.endsWith('-flat-3d'), flatLayoutBase: type => type.replace('-flat-3d', ''), layeredLayoutBase: type => type });
    vm.runInContext(extract('gridMetricOrderActive') + '\n' + extract('layoutNodeOrder'), context);
    return { $, graph, context, order: type => Array.from(context.layoutNodeOrder(type, graph)) };
}
test('unset metric preserves lexicographic ID order for every grid variant', () => {
    const h = setup();
    for (const type of ['grid', 'grid-3d', 'feature-grid', 'feature-grid-3d', 'ego-grid', 'radial-pyramid-3d', 'grid-flat-3d'])
        assert.deepEqual(h.order(type), ['1', '10', '2']);
});
test('metric order and reverse apply to grid variants with stable ID ties', () => {
    const h = setup(); h.$('layout-grid-order').value = 'degree';
    for (const type of ['grid', 'grid-3d', 'feature-grid', 'feature-grid-3d', 'ego-grid', 'radial-pyramid-3d', 'feature-grid-flat-3d']) {
        h.$('layout-grid-order-descending').checked = false;
        assert.deepEqual(h.order(type), ['10', '2', '1']);
        h.$('layout-grid-order-descending').checked = true;
        assert.deepEqual(h.order(type), ['1', '10', '2']);
    }
    assert.deepEqual(h.order('circular'), ['1', '10', '2']);
    assert.deepEqual(h.order('upward-grid-3d'), ['1', '10', '2']);
});
test('computed metric order supports negatives, reverse and resetting to default', () => {
    const h = setup(); h.$('layout-grid-order').value = 'metric:score';
    assert.deepEqual(h.order('grid'), ['10', '2', '1']);
    h.$('layout-grid-order-descending').checked = true;
    assert.deepEqual(h.order('grid'), ['1', '2', '10']);
    h.$('layout-grid-order').value = '';
    assert.deepEqual(h.order('grid'), ['1', '10', '2']);
});
test('missing and nonfinite metrics fail before sending an order', () => {
    const h = setup(); h.$('layout-grid-order').value = 'metric:missing';
    assert.throws(() => h.order('grid'), /missing or non-finite/);
    h.$('layout-grid-order').value = 'metric:score'; h.context.state.metricData.score['1'] = Infinity;
    assert.throws(() => h.order('grid'), /missing or non-finite/);
});
