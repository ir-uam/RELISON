const test = require('node:test'), assert = require('node:assert/strict');
const fs = require('node:fs'), path = require('node:path');
const { createHash } = require('node:crypto');
const { pathToFileURL } = require('node:url');
const { JSDOM } = require('jsdom');
const publicDir = path.join(__dirname, '../../main/resources/public');
const read = file => fs.readFileSync(path.join(publicDir, file), 'utf8');
const familyNames = ['node', 'graph', 'pair', 'community', 'community-global', 'edge-partition', 'edge-partition-global', 'recommendation',
    'diffusion-protocol', 'diffusion-selection', 'diffusion-expiration', 'diffusion-propagation', 'diffusion-update', 'diffusion-sight', 'diffusion-stop', 'diffusion-metric'];
const bundles = Object.fromEntries(familyNames.map(family => [family, JSON.parse(read(`help/${family}-metrics.json`))]));
const flush = () => new Promise(resolve => setImmediate(resolve));

function harness(fetcher) {
    const window = new JSDOM(read('index.html'), { runScripts: 'outside-only' }).window;
    const requests = [];
    window.fetch = async url => {
        requests.push(url);
        if (fetcher) return fetcher(url);
        const family = url.match(/^help\/(.+)-metrics\.json$/)?.[1];
        return { ok: !!bundles[family], json: async () => bundles[family] };
    };
    window.eval(read('vendor/dompurify.min.js'));
    window.eval(read('node-metric-help.js') + '\nwindow.metricHelp = NodiMetricHelp;');
    const document = window.document;
    const buttons = [...document.querySelectorAll('[data-metric-help-for]')];
    for (const button of buttons) {
        const family = button.dataset.metricHelpFamily;
        const [id, entry] = Object.entries(bundles[family].metrics)[0];
        document.getElementById(button.dataset.metricHelpFor).replaceChildren(new window.Option(entry.label, id, false, true));
    }
    window.metricHelp.init();
    return { window, document, buttons, requests, api: window.metricHelp,
        panel: document.getElementById('node-metric-help'), content: document.getElementById('node-metric-help-content'),
        button(id) { return buttons.find(button => button.dataset.metricHelpFor === id); },
        select(id, metric, label = metric) {
            const selector = document.getElementById(id);
            selector.replaceChildren(new window.Option(label, metric, false, true));
            selector.dispatchEvent(new window.Event('change'));
        },
    };
}

test('all metric and model families are extracted reproducibly from documentation with separate identities and source links', async () => {
    const { buildData } = await import(pathToFileURL(path.join(__dirname, '../../../scripts/build-metric-help.mjs')));
    const counts = { node: 18, graph: 17, pair: 22, community: 44, 'community-global': 11, 'edge-partition': 28, 'edge-partition-global': 8, recommendation: 50,
        'diffusion-protocol': 9, 'diffusion-selection': 9, 'diffusion-expiration': 4, 'diffusion-propagation': 4,
        'diffusion-update': 3, 'diffusion-sight': 5, 'diffusion-stop': 7, 'diffusion-metric': 16 };
    for (const family of familyNames) {
        const bundle = bundles[family];
        assert.deepEqual(bundle, await buildData(family), family);
        assert.equal(Object.keys(bundle.metrics).length, counts[family], family);
        for (const metric of Object.values(bundle.metrics)) {
            const section = bundle.sections[metric.section];
            assert.ok(section, metric.section);
            const source = fs.readFileSync(path.join(publicDir, '../../../../../', section.source.path), 'utf8').replace(/\r\n/g, '\n');
            assert.equal(createHash('sha256').update(source).digest('hex'), section.source.sha256);
            assert.ok(read('help/' + section.source.page).includes(`id="${section.anchor}"`));
            assert.doesNotMatch(section.html, /:math:|:code:|\.\. math::/);
        }
    }
    assert.notEqual(bundles.node.sections.Degree.text, bundles.community.sections.Degree.text);
    assert.notEqual(bundles.node.sections.Betweenness.text, bundles.pair.sections.Betweenness.text);
    assert.equal(Object.hasOwn(bundles.graph.metrics, 'Num. Edges'), false);
    assert.equal(Object.hasOwn(bundles['community-global'].metrics, 'Dice Inter-Community Edge Gini'), false);
    assert.equal(Object.hasOwn(bundles['edge-partition'].metrics, 'count'), false);
    assert.equal(bundles.community.metrics.Conductance.section, 'boundary:Conductance');
    assert.equal(bundles.community.metrics['intra-community:ASL'].section, 'graph:Average shortest path length');
});

test('all twenty computation selectors share one panel and preserve focus, family identity and cached documentation', async () => {
    const h = harness();
    try {
        assert.equal(h.buttons.length, 20);
        for (const button of h.buttons) {
            assert.ok(button.closest('.node-metric-picker').contains(h.document.getElementById(button.dataset.metricHelpFor)));
            assert.equal(button.disabled, false);
            button.click(); await flush();
            assert.equal(h.panel.hidden, false);
            assert.equal(button.getAttribute('aria-expanded'), 'true');
            assert.equal(h.buttons.filter(other => other.getAttribute('aria-expanded') === 'true').length, 1);
            const family = button.dataset.metricHelpFamily, selector = h.document.getElementById(button.dataset.metricHelpFor);
            const metric = bundles[family].metrics[selector.value];
            assert.equal(h.document.getElementById('node-metric-help-title').textContent, selector.selectedOptions[0].textContent);
            assert.ok(h.content.textContent.length > 10);
            assert.ok(h.document.getElementById('node-metric-help-source').href.endsWith(bundles[family].sections[metric.section].anchor));
        }
        assert.equal(h.requests.length, familyNames.length, 'attribute controls reuse the same documentation bundles');
        h.select('vertex-metric', 'Degree'); h.button('vertex-metric').click(); await flush();
        assert.match(h.content.textContent, /number of adjacent links/);
        h.select('indiv-comm-metric', 'Degree'); h.button('indiv-comm-metric').click(); await flush();
        assert.match(h.content.textContent, /external adjacent links/);
        h.select('global-comm-metric', 'Dice Inter-Community Edge Gini'); h.button('global-comm-metric').click(); await flush();
        assert.equal(h.document.getElementById('node-metric-help-status').textContent, 'Not found in the project documentation.');
        h.document.getElementById('node-metric-help-close').click();
        assert.equal(h.panel.hidden, true);
        h.button('graph-metric').click(); await flush();
        h.api.workspaceChanged('300px', true);
        assert.equal(h.panel.style.getPropertyValue('--left-panel-width'), '300px');
        h.api.workspaceChanged('270px', false);
        assert.equal(h.panel.hidden, true);
        assert.equal(h.button('graph-metric').getAttribute('aria-expanded'), 'false');
    } finally { h.window.close(); }
});

test('every family retains visual math, accessible MathML, radical SVG paths, definitions and source-specific links after sanitizing', async () => {
    const h = harness();
    try {
        let maths = 0, radicals = 0;
        for (const family of familyNames) {
            const button = h.buttons.find(item => item.dataset.metricHelpFamily === family);
            button.click(); await flush();
            for (const [id, entry] of Object.entries(bundles[family].metrics)) {
                h.select(button.dataset.metricHelpFor, id, entry.label); await flush();
                const template = h.document.createElement('template');
                const section = bundles[family].sections[entry.section];
                template.innerHTML = section.html;
                const expected = template.content.querySelectorAll('.katex').length;
                maths += expected;
                radicals += template.content.querySelectorAll('svg path').length;
                assert.equal(h.content.querySelectorAll('.katex-html').length, expected, `${family}:${id}`);
                assert.equal(h.content.querySelectorAll('.katex-mathml math').length, expected, `${family}:${id}`);
                assert.equal(h.content.querySelectorAll('svg path').length, template.content.querySelectorAll('svg path').length, `${family}:${id}`);
                assert.equal(h.content.querySelectorAll('.katex-error, script, [onerror]').length, 0);
                const source = h.document.getElementById('node-metric-help-source');
                assert.equal(source.getAttribute('href'), `help/${section.source.page}#${section.anchor}`);
            }
        }
        assert.ok(maths > 250);
        assert.ok(radicals > 0, 'Pearson correlation includes a square root');
    } finally { h.window.close(); }
});

test('a late reply from one family cannot replace another family with the same metric identifier', async () => {
    const pending = {};
    const h = harness(url => new Promise(resolve => { pending[url] = resolve; }));
    try {
        h.select('vertex-metric', 'Betweenness'); h.button('vertex-metric').click();
        h.select('pair-metric', 'Betweenness'); h.button('pair-metric').click();
        pending['help/pair-metrics.json']({ ok: true, json: async () => bundles.pair }); await flush();
        pending['help/node-metrics.json']({ ok: true, json: async () => bundles.node }); await flush();
        assert.match(h.content.textContent, /edge joining the pair/);
        assert.equal(h.button('pair-metric').getAttribute('aria-expanded'), 'true');
        assert.equal(h.button('vertex-metric').getAttribute('aria-expanded'), 'false');
    } finally { h.window.close(); }
});

test('model help works in recommendation and prediction, preserves settings, and distinguishes similarly named models and metrics', async () => {
    const h = harness();
    try {
        h.api.workspaceChanged('300px', true, 'recommend');
        const mode = h.document.getElementById('rec-mode'), cutoff = h.document.getElementById('rec-cutoff');
        const params = h.document.getElementById('rec-params');
        params.innerHTML = '<input name="b" value="0.75"><input name="k" value="1.2">';
        cutoff.value = '17';
        // Exercise the real task-change handler without starting the app or any computation.
        const app = read('app.js').replace(/\r\n/g, '\n'), start = app.indexOf('function onRecModeChange(');
        h.window.$ = id => h.document.getElementById(id);
        h.window.eval(app.slice(start, app.indexOf('\n}', start) + 2));
        mode.addEventListener('change', h.window.onRecModeChange);
        h.select('rec-algo', 'BM25');
        h.button('rec-algo').getClientRects = () => [{}];
        h.button('rec-algo').click(); await flush();
        for (const task of ['recommendation', 'prediction']) {
            mode.value = task; mode.dispatchEvent(new h.window.Event('change'));
            assert.equal(h.panel.hidden, false);
            assert.match(h.content.textContent, /BM25 probabilistic information retrieval model/);
            assert.equal(h.document.getElementById('rec-cutoff-label').textContent, task === 'prediction' ? 'Total links' : 'Links per node');
            assert.equal(cutoff.value, '17');
            assert.equal(params.querySelector('[name="b"]').value, '0.75');
            assert.equal(params.querySelector('[name="k"]').value, '1.2');
        }
        assert.match(h.document.getElementById('node-metric-help-intro').textContent, /this model/);
        assert.equal(h.document.getElementById('node-metric-help-subtitle').textContent, 'Let’s understand this model');
        assert.equal(h.document.getElementById('node-metric-help-close').getAttribute('aria-label'), 'Close model help');
        h.select('rec-algo', 'Cosine'); await flush();
        assert.match(h.content.textContent, /Salton index/);
        assert.equal(h.document.getElementById('node-metric-help-source').getAttribute('href'), 'help/models-foaf.html#cosine-similarity');
        h.select('rec-algo', 'Average cosine'); await flush();
        assert.match(h.content.textContent, /authorities the target user is currently following/);
        assert.equal(h.document.getElementById('node-metric-help-source').getAttribute('href'), 'help/models-twitter.html#cosine-similarity');
        h.select('rec-algo', 'PageRank'); await flush();
        assert.match(h.content.textContent, /recommendation \/ prediction method/);
        h.document.getElementById('node-metric-help-close').click();
        assert.equal(h.document.activeElement, h.button('rec-algo'));
        h.api.workspaceChanged('300px', true, 'metrics');
        h.select('vertex-metric', 'PageRank'); h.button('vertex-metric').click(); await flush();
        assert.match(h.content.textContent, /random walk algorithm/);
        assert.doesNotMatch(h.content.textContent, /recommendation \/ prediction method/);
        h.api.workspaceChanged('300px', true, 'recommend');
        assert.equal(h.panel.hidden, true, 'switching workspaces closes the previous context');
        assert.deepEqual(h.requests, ['help/recommendation-metrics.json', 'help/node-metrics.json']);
    } finally { h.window.close(); }
});

test('model documentation failures can be retried, and unknown models use the documented not-found fallback', async () => {
    let attempts = 0;
    const h = harness(() => Promise.resolve({ ok: ++attempts > 1, json: async () => bundles.recommendation }));
    try {
        h.button('rec-algo').click(); await flush();
        assert.equal(h.document.getElementById('node-metric-help-status').textContent, 'Could not load model help.');
        h.document.getElementById('node-metric-help-retry').click(); await flush();
        assert.match(h.content.textContent, /Adamic-Adar algorithm/);
        h.select('rec-algo', 'Undocumented future model'); await flush();
        assert.equal(h.document.getElementById('node-metric-help-status').textContent, 'Not found in the project documentation.');
        assert.match(h.document.getElementById('node-metric-help-intro').textContent, /this model/);
        assert.equal(h.content.innerHTML, '');
        assert.equal(h.document.getElementById('node-metric-help-source').textContent, 'Browse documented models');
        assert.equal(attempts, 2);
    } finally { h.window.close(); }
});

test('preset and custom diffusion help use the correct mechanisms without changing protocol settings or running a simulation', async () => {
    const h = harness();
    try {
        h.api.workspaceChanged('300px', true, 'diffusion');
        const controls = ['diff-protocol', 'diff-selection', 'diff-expiration', 'diff-propagation', 'diff-update', 'diff-sight', 'diff-stop'];
        for (const id of controls) h.document.getElementById(id + '-params').innerHTML = '<input name="test-parameter" value="7">';
        const parameters = () => controls.map(id => h.document.getElementById(id + '-params').innerHTML);
        const original = parameters();
        h.select('diff-protocol', 'Independent cascade model'); h.button('diff-protocol').click(); await flush();
        assert.match(h.content.textContent, /When diffusion works under this protocol/);
        assert.equal(h.document.getElementById('node-metric-help-subtitle').textContent, 'Let’s understand this protocol');
        const app = read('app.js').replace(/\r\n/g, '\n'), start = app.indexOf('function onDiffProtocolType(');
        h.window.$ = id => h.document.getElementById(id);
        h.window.NodiMetricHelp = h.api;
        h.window.toggleHidden = (id, hidden) => { h.document.getElementById(id).hidden = hidden; };
        h.window.eval(app.slice(start, app.indexOf('\n}', start) + 2));
        const type = h.document.getElementById('diff-protocol-type');
        type.value = 'custom'; h.window.onDiffProtocolType();
        assert.equal(h.panel.hidden, true);
        assert.equal(h.document.getElementById('diff-custom-wrap').hidden, false);
        h.select('diff-selection', 'Independent cascade model'); h.button('diff-selection').click(); await flush();
        assert.match(h.content.textContent, /fixed number of the already propagated pieces/);
        assert.equal(h.document.getElementById('node-metric-help-source').getAttribute('href'), 'help/diffusion-selection.html#independent-cascade-model');
        assert.equal(h.document.getElementById('node-metric-help-subtitle').textContent, 'Let’s understand this selection mechanism');
        h.select('diff-selection', 'Count'); await flush();
        assert.match(h.content.textContent, /Pieces are randomly selected/);
        h.select('diff-sight', 'Count'); h.button('diff-sight').click(); await flush();
        assert.match(h.content.textContent, /maximum number of pieces each user sees/);
        assert.doesNotMatch(h.content.textContent, /Pieces are randomly selected/);
        h.select('diff-update', 'Merger'); h.button('diff-update').click(); await flush();
        assert.match(h.content.textContent, /all the information pieces are merged/);
        assert.doesNotMatch(h.content.textContent, /name: Merger|\.\. code::/);
        h.select('diff-stop', 'Num. iter'); h.button('diff-stop').click(); await flush();
        assert.match(h.content.textContent, /fixed number of iterations/);
        assert.equal(h.document.getElementById('node-metric-help-subtitle').textContent, 'Let’s understand this stop condition');
        type.value = 'preset'; h.window.onDiffProtocolType();
        assert.equal(h.panel.hidden, true);
        assert.equal(h.document.getElementById('diff-preset-wrap').hidden, false);
        assert.deepEqual(parameters(), original);
        assert.ok(h.requests.every(url => url.startsWith('help/')));
    } finally { h.window.close(); }
});

test('diffusion metric help browses metrics independently of the simulation multiselect, including when none are selected', async () => {
    const h = harness();
    try {
        h.api.workspaceChanged('300px', true, 'diffusion');
        const selector = h.document.getElementById('diff-metrics'), button = h.button('diff-metrics');
        selector.replaceChildren(...['Speed', 'Creator recall', 'Global feature KLD', 'Unknown future metric'].map(id => new h.window.Option(id, id)));
        h.api.sync();
        assert.equal(selector.selectedOptions.length, 0);
        assert.equal(button.disabled, false);
        button.click(); await flush();
        assert.equal(h.document.getElementById('node-metric-help-choice-wrap').hidden, false);
        assert.match(h.content.textContent, /speed of a simulation/);
        assert.equal(selector.selectedOptions.length, 0, 'opening help must not choose metrics for the run');
        selector.options[0].selected = true; selector.options[1].selected = true;
        selector.dispatchEvent(new h.window.Event('change'));
        const selection = () => [...selector.selectedOptions].map(option => option.value);
        const chosen = selection(), choice = h.document.getElementById('node-metric-help-choice');
        choice.value = 'Global feature KLD'; choice.dispatchEvent(new h.window.Event('change')); await flush();
        assert.match(h.content.textContent, /Kullback-Leibler divergence/);
        assert.equal(h.content.querySelectorAll('.katex').length, 1);
        assert.deepEqual(selection(), chosen);
        assert.equal(h.document.getElementById('node-metric-help-source').getAttribute('href'), 'help/diffusion-feature-metrics.html#feature-kl-divergence');
        choice.value = 'Unknown future metric'; choice.dispatchEvent(new h.window.Event('change')); await flush();
        assert.equal(h.document.getElementById('node-metric-help-status').textContent, 'Not found in the project documentation.');
        assert.deepEqual(selection(), chosen);
        h.select('diff-expiration', 'Exponential decay'); h.button('diff-expiration').click(); await flush();
        assert.equal(h.document.getElementById('node-metric-help-choice-wrap').hidden, true);
        assert.equal(h.content.querySelectorAll('.katex-display').length, 1);
        assert.deepEqual(selection(), chosen);
        selector.replaceChildren(); h.api.sync();
        assert.equal(button.disabled, true);
        assert.deepEqual(h.requests, ['help/diffusion-metric-metrics.json', 'help/diffusion-expiration-metrics.json']);
    } finally { h.window.close(); }
});

test('help chooses animated expressions for the topic and keeps every expression backed by a packaged image', async () => {
    const h = harness();
    try {
        const cases = [
            ['vertex-metric', 'Degree', 'explain'], ['pair-metric', 'Distance', 'path'],
            ['vertex-metric', 'Harmonic', 'path'], ['graph-metric', 'Degree Pearson', 'compare'],
            ['indiv-comm-metric', 'Size', 'discover'], ['rec-algo', 'BM25', 'compute'],
            ['rec-algo', 'Cosine', 'explore'], ['rec-algo', 'Random', 'suggest'], ['rec-algo', 'Katz', 'path'],
            ['diff-protocol', 'Simple', 'diffusion'], ['diff-stop', 'Num. iter', 'suggest'],
            ['diff-expiration', 'Timed', 'curious'], ['diff-update', 'Merger', 'explain'],
            ['diff-metrics', 'Global feature KLD', 'compare'],
        ];
        const css = read('nodi-tutorials.css');
        for (const [id, metric, face] of cases) {
            h.select(id, metric);
            if (h.button(id).getAttribute('aria-expanded') !== 'true') h.button(id).click();
            await flush();
            assert.equal(h.document.getElementById('node-metric-help-avatar').dataset.nodiFace, face, metric);
            const image = h.document.getElementById('node-metric-help-portrait').getAttribute('src');
            assert.equal(image, `img/nodi-2d/nodi-${face}.png`);
            assert.ok(fs.existsSync(path.join(publicDir, image)), image);
            assert.ok(css.includes(`[data-nodi-face="${face}"] img { animation:`), face);
        }
    } finally { h.window.close(); }
});

test('loading, missing documentation and retry failures choose suitable faces, and stale responses cannot change the current face', async () => {
    const pending = {};
    const h = harness(url => new Promise((resolve, reject) => { pending[url] = { resolve, reject }; }));
    const face = () => h.document.getElementById('node-metric-help-avatar').dataset.nodiFace;
    try {
        h.select('vertex-metric', 'Degree'); h.button('vertex-metric').click();
        assert.equal(face(), 'wait');
        h.select('pair-metric', 'Distance'); h.button('pair-metric').click();
        pending['help/node-metrics.json'].reject(new Error('late error')); await flush();
        assert.equal(face(), 'wait');
        pending['help/pair-metrics.json'].resolve({ ok: true, json: async () => bundles.pair }); await flush();
        assert.equal(face(), 'path');
        h.select('pair-metric', 'Unknown metric'); await flush();
        assert.equal(face(), 'attention');
        h.button('vertex-metric').click();
        pending['help/node-metrics.json'].resolve({ ok: false }); await flush();
        assert.equal(face(), 'reassure');
        h.document.getElementById('node-metric-help-retry').click();
        assert.equal(face(), 'wait');
        pending['help/node-metrics.json'].resolve({ ok: true, json: async () => bundles.node }); await flush();
        assert.equal(face(), 'explain');
    } finally { h.window.close(); }
});

function attachTutorialMotion(h, { storage, reduced = false } = {}) {
    const values = new Map(), changes = [];
    storage ||= { getItem: key => values.get(key) || null, setItem: (key, value) => values.set(key, value) };
    const media = { matches: reduced, addEventListener(_name, handler) { changes.push(handler); } };
    h.window.matchMedia = () => media;
    h.window.HTMLDialogElement.prototype.showModal = function() { this.open = true; };
    h.window.HTMLDialogElement.prototype.close = function() { this.open = false; this.dispatchEvent(new h.window.Event('close')); };
    h.window.NodiTutorials = h.window.eval(read('nodi-tutorials.js') + '\nNodiTutorials;');
    const tutorials = h.window.NodiTutorials.init({ storage, onOpen: () => h.api.close(false),
        getContext: () => ({ tab: 'metrics', hasGraph: true, multigraph: false }), navigate() {} });
    return { tutorials, storage, changeReduced(value) { media.matches = value; changes.forEach(handler => handler()); } };
}

test('help and tutorials share a persistent pause control and respond immediately to reduced motion without replacing help content', async () => {
    const h = harness();
    try {
        const motion = attachTutorialMotion(h);
        h.button('vertex-metric').click(); await flush();
        const button = h.panel.querySelector('[data-nodi="motion"]'), content = h.content.innerHTML;
        assert.equal(h.panel.dataset.nodiMotion, 'on');
        button.focus(); button.click();
        assert.equal(h.panel.dataset.nodiMotion, 'off');
        assert.equal(h.document.getElementById('nodi-welcome').dataset.nodiMotion, 'off');
        assert.equal(h.document.getElementById('nodi-guide').dataset.nodiMotion, 'off');
        assert.equal(button.getAttribute('aria-label'), 'Animate Nodi');
        assert.equal(h.document.activeElement, button);
        assert.equal(h.content.innerHTML, content);
        motion.tutorials.openLibrary();
        assert.equal(h.panel.hidden, true);
        h.document.getElementById('nodi-library').querySelector('[data-nodi="motion"]').click();
        assert.equal(h.panel.dataset.nodiMotion, 'on');
        h.document.getElementById('nodi-library').close();
        h.button('vertex-metric').click(); await flush();
        motion.changeReduced(true);
        assert.equal(h.panel.dataset.nodiMotion, 'off');
        assert.equal(button.disabled, true);
        assert.equal(button.getAttribute('aria-label'), 'Motion reduced');
        motion.changeReduced(false);
        assert.equal(h.panel.dataset.nodiMotion, 'on');
        button.click();
        motion.changeReduced(true); motion.changeReduced(false);
        assert.equal(h.panel.dataset.nodiMotion, 'off', 'system changes must preserve a manual pause');
        const reloaded = harness();
        try {
            attachTutorialMotion(reloaded, { storage: motion.storage });
            reloaded.button('vertex-metric').click(); await flush();
            assert.equal(reloaded.panel.dataset.nodiMotion, 'off');
        } finally { reloaded.window.close(); }
        const css = read('nodi-tutorials.css');
        assert.ok(css.includes('@media (prefers-reduced-motion: no-preference)'));
    } finally { h.window.close(); }
});

test('help motion still pauses and resumes when browser storage is unavailable', async () => {
    const h = harness();
    try {
        attachTutorialMotion(h, { storage: { getItem() { throw Error('blocked'); }, setItem() { throw Error('blocked'); } } });
        h.button('vertex-metric').click(); await flush();
        const button = h.panel.querySelector('[data-nodi="motion"]');
        button.click(); assert.equal(h.panel.dataset.nodiMotion, 'off');
        button.click(); assert.equal(h.panel.dataset.nodiMotion, 'on');
    } finally { h.window.close(); }
});
