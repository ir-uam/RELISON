const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { createHash } = require('node:crypto');
const { pathToFileURL } = require('node:url');
const publicDir = path.join(__dirname, '../../main/resources/public');
const read = file => fs.readFileSync(path.join(publicDir, file), 'utf8');
const geometry = vm.createContext({});
vm.runInContext(read('vendor/d3.min.js'), geometry);

test('all packaged assets match their integrity manifest, including fonts, maps and both DuckDB runtimes', () => {
    const manifest = JSON.parse(read('vendor/manifest.json'));
    for (const [file, hash] of Object.entries(manifest)) {
        assert.equal(createHash('sha256').update(fs.readFileSync(path.join(publicDir, 'vendor', file))).digest('hex'), hash, file);
    }
    for (const file of ['fonts/material-symbols-outlined.woff2', 'countries-110m.json',
        'duckdb/duckdb-mvp.wasm', 'duckdb/duckdb-eh.wasm', 'duckdb/duckdb-browser-mvp.worker.js',
        'duckdb/duckdb-browser-eh.worker.js', 'esm/cosmograph.js', 'esm/globe.js', 'esm/gifenc.js'])
        assert.ok(manifest[file], file);
    const world = JSON.parse(read('vendor/countries-110m.json'));
    assert.ok(world.objects.countries.geometries.length > 150);
    assert.ok(world.objects.land);
    const cosmograph = read('vendor/esm/cosmograph.js');
    assert.match(cosmograph, /vendor\/duckdb\/duckdb-eh\.wasm/);
    assert.match(cosmograph, /vendor\/duckdb\/duckdb-mvp\.wasm/);
    assert.match(cosmograph, /SET autoinstall_known_extensions = false/);
});

test('initial page resources and all JavaScript module imports resolve locally without a CDN', () => {
    const html = read('index.html');
    assert.doesNotMatch(html, /rel="preconnect"/);
    for (const match of html.matchAll(/<(?:script[^>]+src|link[^>]+href)="([^"]+)"/g)) {
        assert.doesNotMatch(match[1], /^(?:https?:)?\/\//);
        assert.ok(fs.existsSync(path.join(publicDir, match[1])), match[1]);
    }
    function check(directory) {
        for (const file of fs.readdirSync(directory, { withFileTypes: true })) {
            const full = path.join(directory, file.name);
            if (file.isDirectory()) check(full);
            else if (file.name.endsWith('.js')) {
                const text = fs.readFileSync(full, 'utf8');
                for (const match of text.matchAll(/(?:\bfrom\s*|\bimport\s*\()\s*["']([^"']+)["']/g)) {
                    if (!match[1].startsWith('.')) continue; // Ignore SQL/prose in embedded strings.
                    assert.ok(fs.existsSync(path.resolve(directory, match[1])), `${file.name}: ${match[1]}`);
                }
                assert.doesNotMatch(text, /(?:\bfrom\s*|\bimport\s*\()\s*["']https?:\/\//, file.name);
            }
        }
    }
    check(publicDir);
    const css = read('vendor/fonts/material-symbols.css');
    assert.match(css, /font-family: 'Material Symbols Outlined'/);
    assert.match(css, /url\('\.\/material-symbols-outlined.woff2'\)/);
});

test('packaged GIF module encodes a real frame without fetching anything', async () => {
    const { GIFEncoder, quantize, applyPalette } = await import(pathToFileURL(path.join(publicDir, 'vendor/esm/gifenc.js')));
    const rgba = new Uint8Array([255, 0, 0, 255, 0, 255, 0, 255]);
    const palette = quantize(rgba, 256, { format: 'rgb565' });
    const encoder = GIFEncoder();
    encoder.writeFrame(applyPalette(rgba, palette, 'rgb565'), 2, 1, { palette, delay: 100 });
    encoder.finish();
    const bytes = encoder.bytes();
    assert.equal(Buffer.from(bytes.slice(0, 6)).toString(), 'GIF89a');
    assert.equal(bytes.at(-1), 0x3b);
});

function policy({ stored, online = true, storageFails = false } = {}) {
    const events = new Map(), control = { checked: false, addEventListener(name, fn) { this[name] = fn; } };
    let saved;
    const context = vm.createContext({ window: { addEventListener: (name, fn) => events.set(name, fn) },
        document: { getElementById: () => control }, navigator: { onLine: online },
        localStorage: { getItem() { if (storageFails) throw Error('blocked'); return stored; }, setItem(_key, value) { saved = value; } } });
    vm.runInContext(read('offline-maps.js'), context);
    return { api: context.window.relisonMapSettings, context, control, events, saved: () => saved };
}

test('fresh installs and blocked storage stay offline; street tiles require an explicit remembered preference', () => {
    for (const setup of [{}, { storageFails: true }, { stored: 'false' }]) {
        const h = policy(setup);
        assert.equal(h.api.useOnlineTiles(), false);
        h.control.checked = true; h.control.change();
        assert.equal(h.api.useOnlineTiles(), true);
        h.context.navigator.onLine = false;
        assert.equal(h.api.useOnlineTiles(), false);
    }
    const h = policy({ stored: 'true' });
    assert.equal(h.control.checked, true);
    assert.equal(h.api.useOnlineTiles(), true);
    h.api.setEnabled(false);
    assert.equal(h.saved(), 'false');
});

test('map preference and connectivity changes notify active maps and release subscriptions', () => {
    const h = policy(), calls = [];
    const unsubscribe = h.api.subscribe(() => calls.push(h.api.useOnlineTiles()));
    h.api.setEnabled(true);
    h.context.navigator.onLine = false; h.events.get('offline')();
    h.context.navigator.onLine = true; h.events.get('online')();
    unsubscribe(); h.api.setEnabled(false);
    assert.deepEqual(calls, [true, false, true]);
});

function mapHarness() {
    const calls = [], subscribers = new Set(), attributes = new Map();
    let online = false, tileSettings, exports = 0, projection;
    const selection = { append() { return this; }, style() { return this; }, attr() { return this; },
        datum() { return this; }, on() { return this; }, selectAll() { return this; }, remove() { return this; },
        node: () => ({ cloneNode() { return { setAttribute() {}, insertBefore() {}, querySelectorAll: () => [] }; } }),
        call(fn, ...args) { fn(this, ...args); return this; } };
    const zoom = Object.assign(() => {}, { scaleExtent() { return this; }, on() { return this; }, extent() { return this; }, transform() {} });
    const d3 = { select: () => selection,
        geoEqualEarth: () => { calls.push('projection.equal-earth'); return projection = geometry.d3.geoEqualEarth(); },
        geoMercator: () => { calls.push('projection.mercator'); return projection = geometry.d3.geoMercator(); },
        geoPath: geometry.d3.geoPath, geoGraticule10: geometry.d3.geoGraticule10,
        zoom: () => zoom, zoomTransform: () => ({ k: 1 }), zoomIdentity: {}, min: () => 0, max: () => 1 };
    const container = { hidden: true, clientWidth: 800, clientHeight: 600, replaceChildren() {}, appendChild(node) { attributes.set('credit', node.textContent); } };
    const adapter = name => Object.fromEntries(['readCoordinates', 'ensureLoaded', 'render', 'refresh', 'destroy', 'fitView', 'exportPng', 'exportSvg', 'zoomIn', 'zoomOut']
        .map(method => [method, (...args) => { calls.push(`${name}.${method}`); if (name === 'mercator' && method === 'render') tileSettings = args[1]; }]));
    const context = vm.createContext({ window: { d3, topojson: { feature: () => ({}), mesh: () => ({}) },
        createRelisonMercator: () => adapter('mercator'), createRelisonGlobe: () => adapter('globe'),
        relisonMapSettings: { useOnlineTiles: () => online, subscribe: fn => { subscribers.add(fn); return () => subscribers.delete(fn); } },
        relisonGeographicExport: { element: () => ({}), credit: (...args) => attributes.set('exportCredit', args.at(-1)),
            legend() {}, exportImage: async build => { build(); exports++; } } },
        document: { getElementById: () => container, createElement: () => ({}) },
        fetch: async url => { calls.push(url); return { ok: true, json: async () => ({ objects: { land: {}, countries: {} } }) }; },
        ResizeObserver: class { observe() {} disconnect() {} }, cancelAnimationFrame() {} });
    vm.runInContext(read('equal-earth-map.js'), context);
    const graph = { forEachNode() {}, forEachEdge() {}, on() {}, off() {} };
    const settings = { projection: 'mercator', coordinates: () => ({ points: new Map(), center: 0 }), onError: message => calls.push(message) };
    return { map: context.window.relisonGeographic, calls, graph, settings, attributes, subscribers,
        online: value => { online = value; }, tileError: () => tileSettings.onTileError(), exports: () => exports,
        projection: () => projection,
        change: async () => { for (const fn of subscribers) fn(); await new Promise(resolve => setImmediate(resolve)); } };
}

test('default Mercator choice uses local Earth data, preserves the graph, and exports without street tiles', async () => {
    const h = mapHarness();
    await h.map.render(h.graph, h.settings);
    assert.ok(h.calls.includes('vendor/countries-110m.json'));
    assert.equal(h.calls.includes('mercator.render'), false);
    assert.match(h.attributes.get('credit'), /Offline Earth map.*Web Mercator/);
    await h.map.exportSvg(); await h.map.exportPng();
    assert.equal(h.exports(), 2);
    assert.match(h.attributes.get('exportCredit'), /Web Mercator/);
    h.map.destroy(); assert.equal(h.subscribers.size, 0);
});

test('online tiles fall back after failure, connectivity changes redraw, and destroyed maps cannot be revived', async () => {
    const h = mapHarness(); h.online(true);
    await h.map.render(h.graph, h.settings);
    assert.ok(h.calls.includes('mercator.render'));
    h.tileError(); await new Promise(resolve => setImmediate(resolve));
    assert.match(h.attributes.get('credit'), /Offline Earth map.*Web Mercator/);
    assert.ok(h.calls.includes('projection.mercator'));
    assert.equal(h.calls.includes('projection.equal-earth'), false);
    h.online(false); await h.change();
    h.map.destroy(); h.calls.length = 0;
    h.online(true); await h.change();
    assert.deepEqual(h.calls, []);
});

test('a destroyed map ignores a late local Earth load', async () => {
    const h = mapHarness();
    const rendering = h.map.render(h.graph, h.settings);
    h.map.destroy(); await rendering;
    assert.equal(h.attributes.has('credit'), false);
});

test('offline Mercator preserves its projection math, square world extent and Web Mercator latitude bounds', async () => {
    const h = mapHarness();
    await h.map.render(h.graph, h.settings);
    const projection = h.projection(), origin = projection([0, 0]);
    const north = projection([0, 60]), east = projection([60, 0]);
    const expected = Math.log(Math.tan(Math.PI / 4 + Math.PI / 6)) / (Math.PI / 3);
    assert.ok(Math.abs(Math.abs((north[1] - origin[1]) / (east[0] - origin[0])) - expected) < 1e-10);
    const bounds = geometry.d3.geoPath(projection).bounds({ type: 'Sphere' });
    assert.ok(Math.abs((bounds[1][0] - bounds[0][0]) - (bounds[1][1] - bounds[0][1])) < 1e-8);
    const upperLatitude = projection.invert([origin[0], bounds[0][1]])[1];
    assert.ok(Math.abs(upperLatitude - 85.0511287798066) < 1e-8);
});

test('choosing Equal Earth explicitly retains its projection and attribution offline', async () => {
    const h = mapHarness(); h.settings.projection = 'equal-earth';
    await h.map.render(h.graph, h.settings);
    assert.ok(h.calls.includes('projection.equal-earth'));
    assert.equal(h.calls.includes('projection.mercator'), false);
    assert.equal(h.attributes.get('credit'), 'Natural Earth · Equal Earth');
    await h.map.exportSvg();
    assert.match(h.attributes.get('exportCredit'), /Equal Earth/);
});
