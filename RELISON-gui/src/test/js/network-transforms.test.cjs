/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/public/app.js'), 'utf8');

function setup(cosmograph = false) {
    const nodes = new Map([['a', { x: 2, y: 2, color: 'red' }], ['b', { x: 8, y: 2 }], ['c', { x: 2, y: 8 }]]);
    const graph = { order: 3, forEachNode: callback => nodes.forEach((data, id) => callback(id, data)),
        updateEachNodeAttributes: callback => nodes.forEach((data, id) => nodes.set(id, callback(id, data))) };
    const calls = [], frames = new Map(), elements = new Map(); let nextFrame = 0;
    const $ = id => {
        if (!elements.has(id)) elements.set(id, { setAttribute: (key, value) => calls.push([key, value]) });
        return elements.get(id);
    };
    const renderer = { getCustomBBox: () => null, getBBox: () => ({ x: [2, 8], y: [2, 8] }),
        refresh: () => {}, setCustomBBox: () => calls.push('freeze'), kill: () => calls.push('kill') };
    const cosmo = { getDimensions: () => 2, savePointPositions: () => calls.push('save'), stopForceLayout: () => {},
        setNodePositions: async positions => { calls.push(['positions', positions]); return true; },
        getNetworkView: () => ({ x: 4, y: 4, pixelsPerUnit: 10 }), destroy: () => calls.push('destroy'),
        setNetworkView: view => calls.push(['view', view]) };
    const context = vm.createContext({ $, state: { graph, renderer, activeTab: 'network' }, window: { relisonCosmograph: cosmo },
        usingCosmograph: () => cosmograph, usingGeographic: () => false,
        stopLayout: () => {}, applyReducers: () => calls.push('apply-selection'), drawRecOverlay: () => {}, setStatus: (...args) => calls.push(['status', ...args]),
        cosmographAppearanceRefreshTimer: 1, cosmographTimelineRefreshTimer: 2, clearTimeout: id => calls.push(['clear-timer', id]),
        syncCosmographSelection: () => calls.push('sync-selection'),
        requestAnimationFrame: callback => { frames.set(++nextFrame, callback); return nextFrame; },
        cancelAnimationFrame: id => frames.delete(id),
        setNetworkRenderer: async () => calls.push('rebuild-cosmograph'),
        sigmaNetworkView: () => ({ x: 4, y: 4 }), restoreSigmaNetworkView: () => calls.push('restore-sigma'),
        initializeNetworkSigma: g => { assert.equal(g, graph); calls.push('rebuild-sigma'); },
    });
    vm.runInContext(source.slice(source.indexOf('let networkRotation ='), source.indexOf('\nfunction zoomIn()')), context);
    return { context, nodes, calls, frames, graph };
}
function near(actual, expected) { assert.ok(Math.abs(actual - expected) < 1e-10, `${actual} != ${expected}`); }

test('rotation uses all-node centroid and preserves distances, styles and absolute frame positions', async () => {
    const { context, nodes } = setup();
    const snapshot = context.networkTransformSnapshot();
    near(snapshot.center.x, 4); near(snapshot.center.y, 4);
    await context.applyNetworkTransform(snapshot, Math.PI / 2);
    near(nodes.get('a').x, 6); near(nodes.get('a').y, 2);
    near(nodes.get('b').x, 6); near(nodes.get('b').y, 8);
    assert.equal(nodes.get('a').color, 'red');
    await context.applyNetworkTransform(snapshot, Math.PI * 2);
    near(nodes.get('a').x, 2); near(nodes.get('a').y, 2);
});

test('horizontal and vertical mirrors are centroid reflections and are reversible', async () => {
    const { context, nodes } = setup();
    await context.mirrorNetwork(true);
    near(nodes.get('a').x, 6); near(nodes.get('a').y, 2);
    near(nodes.get('b').x, 0);
    await context.mirrorNetwork(true);
    near(nodes.get('a').x, 2); near(nodes.get('b').x, 8);
    await context.mirrorNetwork(false);
    near(nodes.get('a').x, 2); near(nodes.get('a').y, 6);
    near(nodes.get('c').y, 0);
});

test('Cosmograph transforms use position-only updates and reload recreates the instance', async () => {
    const { context, calls } = setup(true);
    await context.mirrorNetwork(true);
    assert.equal(calls.filter(call => call[0] === 'positions').length, 1);
    await context.reloadNetworkRenderer();
    const destroy = calls.indexOf('destroy'), rebuild = calls.indexOf('rebuild-cosmograph');
    assert.ok(destroy >= 0 && rebuild > destroy);
    assert.ok(calls.indexOf('apply-selection') > destroy && calls.indexOf('apply-selection') < rebuild);
    assert.ok(calls.indexOf('sync-selection') > rebuild);
    assert.equal(context.state.cosmographViewSignature, null);
    assert.ok(calls.some(call => call[0] === 'view'));
});

test('Sigma reload reuses graph and rebuilds interaction handlers', async () => {
    const { context, calls } = setup();
    await context.reloadNetworkRenderer();
    assert.ok(calls.indexOf('kill') < calls.indexOf('rebuild-sigma'));
    assert.ok(calls.includes('restore-sigma'));
});

test('hold rotation stops scheduling immediately on release and skips replaced graphs', async () => {
    const { context, frames, nodes } = setup();
    context.startNetworkRotation();
    const [id, callback] = [...frames][0]; frames.delete(id); await callback(0);
    assert.equal(frames.size, 1);
    context.stopNetworkRotation(); assert.equal(frames.size, 0);
    const snapshot = context.networkTransformSnapshot();
    context.state.graph = {};
    await context.applyNetworkTransform(snapshot, Math.PI);
    near(nodes.get('a').x, 2);
});

test('2D transforms are unavailable for maps and 3D', () => {
    const { context } = setup(true);
    context.window.relisonCosmograph.getDimensions = () => 3;
    assert.equal(context.networkTransformSnapshot(), null);
    context.usingGeographic = () => true;
    assert.equal(context.network2DTransformsAvailable(), false);
});
