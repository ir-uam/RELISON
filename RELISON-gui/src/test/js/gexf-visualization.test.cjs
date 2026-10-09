const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/public/app.js'), 'utf8');
const start = source.indexOf('function applyAppearance()');
const sizeHandler = source.slice(start, source.indexOf('    const colorMode =', start)) + '\n}';
test('imported sizes survive appearance refreshes and uniform mode remains available', () => {
    const nodes = new Map([['a', { attrs: { 'viz:size': 8.5 } }], ['b', { attrs: {} }], ['c', { attrs: { 'viz:size': 0 } }]]);
    const controls = { 'size-by': { value: 'imported' } };
    const graph = { forEachNode: fn => nodes.forEach((attrs, node) => fn(node)),
        setNodeAttribute: (node, key, value) => { nodes.get(node)[key] = value; } };
    const context = vm.createContext({ state: { graph }, $: id => controls[id], numInput: () => 14,
        nodeAttrVal: (node, name) => nodes.get(node).attrs[name], syncSizeControlVisibility() {},
        syncNodeColorControls() {}, syncEdgeColorControls() {} });
    vm.runInContext(sizeHandler + '\napplyAppearance(); applyAppearance();', context);
    assert.equal(nodes.get('a').size, 8.5);
    assert.equal(nodes.get('b').size, 14);
    assert.equal(nodes.get('c').size, 0);
    controls['size-by'].value = '';
    vm.runInContext('applyAppearance();', context);
    assert.equal(nodes.get('a').size, 14);
});

test('imported colors retain alpha and fall back for missing values', () => {
    const nodes = new Map([['a', { attrs: { 'viz:color': 'rgba(12,34,56,0.5)' } }], ['b', { attrs: {} }]]);
    const graph = { forEachNode: fn => nodes.forEach((attrs, node) => fn(node)),
        setNodeAttribute: (node, key, value) => { nodes.get(node)[key] = value; } };
    const controls = { 'node-color-mode': { value: 'imported' }, 'color-by': { value: '' } };
    const context = vm.createContext({ graph, state: {}, $: id => controls[id],
        nodeAttrVal: (node, name) => nodes.get(node).attrs[name] });
    const colorStart = source.indexOf('    const colorMode =', start);
    const handler = 'function applyColor() {\n' + source.slice(colorStart, source.indexOf('    applyEdgeAppearance();', colorStart)) + '\n}';
    vm.runInContext(handler + '\napplyColor(); applyColor();', context);
    assert.equal(nodes.get('a').color, 'rgba(12,34,56,0.5)');
    assert.equal(nodes.get('b').color, '#4f9dff');
    controls['node-color-mode'].value = 'default';
    vm.runInContext('applyColor();', context);
    assert.equal(nodes.get('a').color, '#4f9dff');
});

test('imported edge styles survive refreshes and retain independent parallel edge values', () => {
    const edges = new Map([['first', { attrs: { 'viz:color': '#123456', 'viz:thickness': 7 } }],
        ['second', { attrs: { 'viz:color': 'rgba(1,2,3,0.5)', 'viz:thickness': 0 } }], ['missing', { attrs: {} }]]);
    const graph = { edges: () => [...edges.keys()], source: () => 'a', target: () => 'b',
        setEdgeAttribute: (edge, key, value) => { edges.get(edge)[key] = value; } };
    const controls = { 'edge-size-by': { value: 'imported' }, 'edge-color-mode': { value: 'imported' },
        'edge-color-single': { value: '#abcdef' }, 'edge-color-by': { value: '' } };
    const context = vm.createContext({ state: { graph, pairData: {} }, $: id => controls[id],
        numInput: () => 0.5, edgeAttrDefs: () => [], edgeAttrVal: (edge, name) => edges.get(edge).attrs[name] });
    const handlerStart = source.indexOf('function applyEdgeAppearance()');
    const handler = source.slice(handlerStart, source.indexOf('// Linear interpolation', handlerStart));
    vm.runInContext(handler + '\napplyEdgeAppearance(); applyEdgeAppearance();', context);
    assert.equal(edges.get('first').size, 7);
    assert.equal(edges.get('second').size, 0);
    assert.equal(edges.get('missing').size, 0.5);
    assert.equal(edges.get('first').color, '#123456');
    assert.equal(edges.get('second').color, 'rgba(1,2,3,0.5)');
    assert.equal(edges.get('missing').color, '#888888');
    controls['edge-size-by'].value = '';
    controls['edge-color-mode'].value = 'single';
    vm.runInContext('applyEdgeAppearance();', context);
    assert.equal(edges.get('first').size, 0.5);
    assert.equal(edges.get('first').color, '#abcdef');
});

test('numeric GEXF export remaps endpoints and preserves model IDs as labels without changing the graph', () => {
    const controls = { 'export-gexf-numeric-ids': { checked: true } };
    const nodes = [['Alice & Bob', { label: 'display alias' }], ['Carol', {}]];
    const graph = { nodes: () => nodes.map(([node]) => node), forEachNode: fn => nodes.forEach(([node, attrs]) => fn(node, attrs)),
        forEachEdge: fn => fn('edge', { weight: 3 }, 'Alice & Bob', 'Carol') };
    let output;
    const context = vm.createContext({ state: { graph, directed: true, metricOrder: [], pairOrder: [], communityData: {} },
        $: id => controls[id], usingCosmograph: () => false, colorToRgb: () => null, nodeAttrDefs: () => [], edgeAttrDefs: () => [],
        download: (name, content) => { output = content; } });
    const handlerStart = source.indexOf('function exportGexf()');
    vm.runInContext(source.slice(handlerStart, source.indexOf('// Parses a CSS colour', handlerStart)) + '\nexportGexf();', context);
    assert.match(output, /<node id="0" label="Alice &amp; Bob">/);
    assert.match(output, /<node id="1" label="Carol">/);
    assert.match(output, /source="0" target="1"/);
    assert.equal(nodes[0][0], 'Alice & Bob');
    controls['export-gexf-numeric-ids'].checked = false;
    vm.runInContext('exportGexf();', context);
    assert.match(output, /<node id="Alice &amp; Bob" label="display alias">/);
    assert.match(output, /source="Alice &amp; Bob" target="Carol"/);
});

test('GEXF exports typed custom attributes, parallel edge values, labels and colliding metrics', () => {
    const graph = { nodes: () => ['a', 'b'], forEachNode: fn => {
        fn('a', { attrs: { score: 0, active: false, text: 'A & "B"', empty: '' } });
        fn('b', { attrs: {} });
    }, forEachEdge: fn => {
        fn('first', { attrs: { label: 'friend & colleague', since: 2020, kind: 'first' } }, 'a', 'b');
        fn('second', { attrs: { label: '', since: 2021, kind: 'second' } }, 'a', 'b');
    }};
    let output;
    const context = vm.createContext({ state: { graph, directed: true, metricOrder: ['score'], pairOrder: [],
        communityData: {}, metricData: { score: { a: 8 } } },
        $: () => ({ checked: false }), usingCosmograph: () => false, colorToRgb: () => null,
        nodeAttrDefs: () => [{name:'score',type:'int'}, {name:'active',type:'bool'}, {name:'text',type:'string'}, {name:'empty',type:'string'}],
        edgeAttrDefs: () => [{name:'label',type:'string'}, {name:'since',type:'long'}, {name:'kind',type:'categorical'}],
        download: (name, content) => { output = content; } });
    const handlerStart = source.indexOf('function exportGexf()');
    vm.runInContext(source.slice(handlerStart, source.indexOf('// Parses a CSS colour', handlerStart)) + '\nexportGexf();', context);
    assert.match(output, /title="score" type="integer"/);
    assert.match(output, /title="active" type="boolean"/);
    assert.match(output, /title="since" type="long"/);
    assert.match(output, /title="kind" type="string"/);
    assert.match(output, /title="computed:score" type="double"/);
    assert.match(output, /for="n0" value="0"/);
    assert.match(output, /for="n1" value="false"/);
    assert.match(output, /for="n2" value="A &amp; &quot;B&quot;"/);
    assert.match(output, /for="n3" value=""/);
    assert.match(output, /for="n4" value="8"/);
    assert.match(output, /label="friend &amp; colleague"/);
    assert.match(output, /label=""/);
    assert.match(output, /for="e1" value="2020"/);
    assert.match(output, /for="e1" value="2021"/);
    assert.match(output, /for="e2" value="first"/);
    assert.match(output, /for="e2" value="second"/);
    assert.doesNotMatch(output, /undefined|null/);
});
