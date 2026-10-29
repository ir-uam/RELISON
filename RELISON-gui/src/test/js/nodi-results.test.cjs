const test = require('node:test'), assert = require('node:assert/strict');
const fs = require('node:fs'), path = require('node:path');
const { JSDOM } = require('jsdom');
const publicDir = path.join(__dirname, '../../main/resources/public');
const read = file => fs.readFileSync(path.join(publicDir, file), 'utf8');

function setup({ delayedRows = false } = {}) {
    const window = new JSDOM(read('index.html'), { runScripts: 'outside-only' }).window;
    const document = window.document, frames = new Map(), observers = [], scrolls = [];
    let frameId = 0;
    window.requestAnimationFrame = callback => { frames.set(++frameId, callback); return frameId; };
    window.ResizeObserver = class {
        constructor(callback) { this.callback = callback; this.targets = new Set(); observers.push(this); }
        observe(target) { this.targets.add(target); }
        disconnect() { this.targets.clear(); }
    };
    window.HTMLDialogElement.prototype.close = function() { this.open = false; };
    window.HTMLDialogElement.prototype.showModal = function() { this.open = true; };
    const context = { tab: 'metrics', hasGraph: true, graphKey: {}, graphRevision: 0, nodeMetrics: {} };
    const label = 'Degree (orientation=OUT)', values = { a: 2, b: 4 };
    window.state = { defs: {} }; window.$ = id => document.getElementById(id);
    window.formatAuthorityMode = text => text; window.fmt = String; window.recLabel = String;
    window.tdText = text => { const td = document.createElement('td'); td.textContent = text; return td; };
    window.buildAverageTable = window.eval(read('metric-tables.js') + '\nbuildAverageTable;');
    function geometry(element, rect) {
        element.getBoundingClientRect = () => ({ ...rect, width: rect.right - rect.left, height: rect.bottom - rect.top });
        element.getClientRects = () => element.closest('[hidden]') || (element.closest('details') && !element.closest('details').open) ? [] : [element.getBoundingClientRect()];
        element.scrollIntoView = () => scrolls.push(element);
    }
    const layout = document.getElementById('layout');
    const summary = document.createElement('section'), rankings = document.createElement('section'), distributions = document.createElement('section');
    layout.append(summary, rankings, distributions);
    const average = document.getElementById('node-averages-table'), rankingChart = document.getElementById('node-chart');
    summary.append(average); rankings.append(rankingChart);
    const rankingTable = document.getElementById('node-topk-table'), details = document.createElement('details');
    details.append(rankingTable); rankings.append(details);
    geometry(average, { left: 450, top: 180, right: 1100, bottom: 320 });
    geometry(rankingChart, { left: 450, top: 180, right: 1100, bottom: 560 });
    geometry(rankingTable, { left: 450, top: 600, right: 1100, bottom: 820 });
    let distributionChart;
    function locateRows() {
        [...average.querySelectorAll('tbody tr')].forEach((row, i) => geometry(row, { left: 450, top: 220 + i * 34, right: 1100, bottom: 252 + i * 34 }));
        return [...average.querySelectorAll('tbody tr')].find(row => row.dataset.metricLabel === label);
    }
    function renderRows() {
        window.buildAverageTable('node-averages-table', [
            { label: 'Degree (orientation=IN)', original: 7, rec: [] }, { label, original: 3, rec: [] },
        ], []);
        return locateRows();
    }
    const api = window.eval(read('nodi-tutorials.js') + '\nNodiTutorials;');
    const visited = [];
    const ui = api.create({ getContext: () => context, onOpen() {}, navigate(step) {
        visited.push(step); context.tab = step.tab;
        summary.hidden = step.view !== 'node-summary';
        rankings.hidden = !['node-rankings', 'node-ranking-table'].includes(step.view);
        distributions.hidden = step.view !== 'node-distributions';
        if (step.view === 'node-summary' && !delayedRows) renderRows();
        if (step.view === 'node-ranking-table') details.open = true;
        if (step.view === 'node-distributions' && !distributionChart) {
            distributionChart = document.createElement('div'); distributionChart.id = 'node-analysis-distributions';
            geometry(distributionChart, { left: 0, top: 0, right: 0, bottom: 0 });
            distributions.append(distributionChart);
        }
    } });
    const guide = document.getElementById('nodi-guide');
    Object.defineProperty(guide, 'offsetWidth', { value: 360 });
    Object.defineProperty(guide, 'offsetHeight', { value: 300 });
    async function settle() {
        await new Promise(resolve => setImmediate(resolve));
        for (let i = 0; frames.size && i < 10; i++) {
            const callbacks = [...frames.values()]; frames.clear(); callbacks.forEach(callback => callback(0));
        }
        assert.equal(frames.size, 0, 'highlight observers must not create a rendering loop');
    }
    function complete() {
        ui.start('metrics'); ui.move(1);
        context.nodeMetrics[label] = values;
        ui.actionCompleted({ type: 'node-metric', graphKey: context.graphKey, graphRevision: 0, label, values });
    }
    return { window, document, ui, complete, settle, renderRows, locateRows, geometry, scrolls, observers, visited,
        average, rankingChart, rankingTable, details, distributions,
        highlight: document.querySelector('.nodi-highlight'), distributionChart: () => distributionChart };
}

test('the highlight follows the exact computed average row when rows appear late or sorting replaces them', async () => {
    const h = setup({ delayedRows: true });
    try {
        h.complete(); await h.settle();
        assert.equal(h.visited.at(-1).view, 'node-summary');
        const row = h.renderRows(); await h.settle();
        assert.equal(row.children[2].textContent, '3');
        assert.equal(h.highlight.hidden, false);
        assert.equal(h.highlight.style.top, '250px');
        assert.equal(h.highlight.style.height, '40px', 'highlight the result row, not the menu or entire table');
        assert.equal(h.scrolls.at(-1), row);
        h.average.querySelector('[data-sort-key="parameters"]').click();
        h.average.querySelector('[data-sort-key="parameters"]').click();
        const replacement = h.locateRows(); await h.settle();
        assert.notEqual(replacement, row);
        assert.equal(h.highlight.style.top, '216px');
        assert.equal(h.scrolls.at(-1), replacement);
        assert.ok(h.observers.some(observer => observer.targets.has(replacement)));
        assert.ok(h.observers.every(observer => !observer.targets.has(row)));
        h.ui.closeGuide(false); replacement.remove(); await h.settle();
        assert.equal(h.highlight.hidden, true);
        assert.ok(h.observers.every(observer => !observer.targets.has(replacement)));
    } finally { h.window.close(); }
});

test('result steps highlight the ranking chart, expanded ranking table and late-sized distribution chart', async () => {
    const h = setup();
    try {
        h.complete(); await h.settle();
        h.ui.move(1); await h.settle();
        assert.equal(h.visited.at(-1).target, '#node-chart');
        assert.equal(h.highlight.style.height, '388px');
        h.ui.move(1); await h.settle();
        assert.equal(h.visited.at(-1).target, '#node-topk-table');
        assert.equal(h.details.open, true);
        assert.equal(h.highlight.style.height, '168px', 'the expanded result table is clipped to the viewport');
        h.ui.move(1); await h.settle();
        assert.equal(h.visited.at(-1).target, '#node-analysis-distributions');
        assert.equal(h.highlight.hidden, true, 'wait for a chart to have nonzero dimensions');
        const chart = h.distributionChart();
        h.geometry(chart, { left: 450, top: 220, right: 1000, bottom: 650 });
        h.observers.filter(observer => observer.targets.has(chart)).forEach(observer => observer.callback([]));
        await h.settle();
        assert.equal(h.highlight.hidden, false);
        assert.equal(h.highlight.style.height, '438px');
        assert.equal(h.highlight.style.width, '558px');
        assert.equal(h.scrolls.at(-1), chart);
    } finally { h.window.close(); }
});
