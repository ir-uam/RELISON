const test = require("node:test"), assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), vm = require("node:vm");
const { pathToFileURL } = require("node:url");
const publicDir = path.join(__dirname, "../../main/resources/public");
const read = name => fs.readFileSync(path.join(publicDir, name), "utf8");
const bundle = JSON.parse(read("help/node-metrics.json"));
const generator = () => import(pathToFileURL(path.join(__dirname, "../../../scripts/build-metric-help.mjs")));
const flush = () => new Promise(resolve => setImmediate(resolve));

function setup(options = {}) {
    const nodes = new Map(), listeners = {}, requests = [], sanitized = [];
    let openCalls = 0, fail = !!options.fail;
    function node(id) {
        if (!nodes.has(id)) nodes.set(id, { id, hidden: false, attrs: {}, listeners: {}, textContent: "", innerHTML: "",
            setAttribute(name, value) { this.attrs[name] = value; },
            addEventListener(event, callback) { this.listeners[event] = callback; },
            replaceChildren() { this.innerHTML = ""; }, focus() { this.focused = true; }, getClientRects() { return [{}]; } });
        return nodes.get(id);
    }
    const button = node("button"), selector = node("selector"), panel = node("panel");
    panel.hidden = true; panel.querySelector = id => node(id.slice(1));
    selector.value = options.id === undefined ? "Degree" : options.id;
    selector.selectedOptions = [{ textContent: selector.value }];
    const context = vm.createContext({ console,
        document: { addEventListener(event, callback) { listeners[event] = callback; }, querySelector() { return context.modalOpen ? {} : null; } },
        fetch: async url => { requests.push(url); return { ok: !fail, json: async () => bundle }; },
    });
    const api = vm.runInContext(read("node-metric-help.js") + "\nNodiMetricHelp", context);
    const controller = api.create({ button, selector, panel, ...(options.load ? { load: options.load } : {}),
        sanitize(html) { sanitized.push(html); return html; }, onOpen() { openCalls++; } });
    return { controller, context, button, selector, panel, node, requests, sanitized, listeners,
        openCalls: () => openCalls, setFailure(value) { fail = value; },
        select(id, label = id) { selector.value = id; selector.selectedOptions = [{ textContent: label }]; selector.listeners.change(); },
    };
}

test("offline help is reproducibly extracted from the project documentation, including variant mappings", async () => {
    const { buildData } = await generator();
    assert.deepEqual(bundle, await buildData(), "Regenerate with npm run build:help when the documentation changes");
    assert.equal(Object.keys(bundle.metrics).length, 18);
    assert.equal(Object.keys(bundle.sections).length, 13);
    assert.equal(Object.hasOwn(bundle.metrics, "Free discovery"), false);
    assert.equal(bundle.metrics["Inverse degree"].section, "Degree");
    assert.equal(bundle.metrics["Complementary inverse degree"].section, "Degree");
    assert.equal(bundle.metrics["Complementary PageRank"].section, "PageRank");
    assert.equal(bundle.metrics.Eigenvector.section, "Eigenvector centrality");
    for (const section of Object.values(bundle.sections)) {
        assert.ok(section.html.includes("<p>"));
        assert.ok(read("help/node-metrics.html").includes(`id="${section.anchor}"`));
        assert.doesNotMatch(section.html, /:math:|:code:|\.\. math::|type: vertex/);
    }
});

test("documented equations are typeset as HTML with accessible MathML and invalid TeX fails the build", async () => {
    const { mathMarkup, renderSection } = await generator();
    const math = mathMarkup(String.raw`\frac{1}{k} + \sum_{u=1}^{n} \alpha^k`, true);
    assert.match(math, /<mfrac>/); assert.match(math, /<munderover>/); assert.match(math, /<msup>/);
    assert.match(math, /class="katex-html" aria-hidden="true"/);
    assert.match(math, /class="frac-line"/);
    assert.match(math, /encoding="application\/x-tex"/);
    assert.throws(() => mathMarkup(String.raw`\unsupported{x}`), /Undefined control sequence/);
    assert.match(bundle.sections.Betweenness.html, /<math /);
    const escaped = renderSection('<img src=x onerror="bad()">\n\n**A documented heading**');
    assert.doesNotMatch(escaped, /<img/); assert.match(escaped, /&lt;img/);
    assert.match(escaped, /<strong>A documented heading<\/strong>/);
    // Some source roles omit backticks: retain commas inside function arguments.
    assert.match(renderSection('The weight is :math:w(u,v), for node :math:u.'),
        /encoding="application\/x-tex">w\(u,v\)<\/annotation>/);
});

test("every documented equation survives the real panel sanitizer with typesetting and local fonts intact", async () => {
    const { JSDOM } = require('jsdom');
    const window = new JSDOM(read('index.html'), { runScripts: 'outside-only' }).window;
    try {
        window.eval(read('vendor/dompurify.min.js'));
        window.eval(read('node-metric-help.js') + '\nwindow.metricHelp = NodiMetricHelp;');
        const document = window.document, selector = document.getElementById('vertex-metric');
        const content = document.getElementById('node-metric-help-content');
        const panel = document.getElementById('node-metric-help');
        const controller = window.metricHelp.create({ button: document.getElementById('node-metric-help-button'),
            selector, panel, load: async () => bundle, onOpen() {} });
        let equationCount = 0;
        for (const [id, metric] of Object.entries(bundle.metrics)) {
            const section = bundle.sections[metric.section];
            selector.replaceChildren(new window.Option(metric.label, id));
            if (panel.hidden) controller.open(); else controller.sync();
            await flush();
            const before = new JSDOM(section.html).window;
            try {
                const expected = before.document.querySelectorAll('.katex').length;
                equationCount += expected;
                assert.equal(content.querySelectorAll('.katex-html[aria-hidden="true"]').length, expected, id);
                assert.equal(content.querySelectorAll('.katex-mathml math').length, expected, id);
                assert.equal(content.querySelectorAll('.frac-line[style]').length,
                    before.document.querySelectorAll('.frac-line[style]').length, id);
                assert.equal(content.querySelectorAll('.katex-error').length, 0, id);
                assert.equal(content.querySelectorAll('script, [onerror]').length, 0, id);
            } finally { before.close(); }
        }
        assert.ok(equationCount > 80);
        for (const page of ['index.html', 'help/node-metrics.html']) {
            const pageWindow = new JSDOM(read(page)).window;
            try {
                const href = pageWindow.document.querySelector('link[href$="katex.min.css"]').getAttribute('href');
                const cssFile = path.join(publicDir, path.dirname(page), href);
                const css = fs.readFileSync(cssFile, 'utf8');
                assert.match(css, /\.katex \.katex-mathml\{/);
                for (const match of css.matchAll(/url\(([^)]+)\)/g))
                    assert.ok(fs.existsSync(path.resolve(path.dirname(cssFile), match[1])), match[1]);
            } finally { pageWindow.close(); }
        }
    } finally { window.close(); }
});

test("opening help renders the selected metric and changing it reuses the offline bundle without computing", async () => {
    const h = setup(); h.controller.open(); await flush();
    assert.equal(h.panel.hidden, false);
    assert.equal(h.button.attrs["aria-expanded"], "true");
    assert.equal(h.node("node-metric-help-title").textContent, "Degree");
    assert.equal(h.node("node-metric-help-title").focused, true);
    assert.match(h.node("node-metric-help-content").innerHTML, /number of adjacent links/);
    assert.equal(h.node("node-metric-help-source").href, "help/node-metrics.html#degree");
    assert.equal(h.sanitized[0], bundle.sections.Degree.html);
    h.select("PageRank"); await flush();
    assert.equal(h.node("node-metric-help-title").textContent, "PageRank");
    assert.match(h.node("node-metric-help-content").innerHTML, /random walk algorithm/);
    assert.deepEqual(h.requests, ["help/node-metrics.json"]);
    assert.equal(h.openCalls(), 1);
    h.select("Harmonic", "Harmonic centrality"); await flush();
    assert.equal(h.button.attrs["aria-label"], "Help for Harmonic centrality");
});

test("related variants identify their shared source and undocumented metrics show not found", async () => {
    const h = setup({ id: "Inverse degree" }); h.controller.open(); await flush();
    assert.match(h.node("node-metric-help-intro").textContent, /Degree section/);
    assert.match(h.node("node-metric-help-content").innerHTML, /multiplicative inverse/);
    for (const id of ["Free discovery", "Unknown metric", "__proto__"]) {
        h.select(id); await flush();
        assert.equal(h.node("node-metric-help-status").textContent, "Not found in the project documentation.");
        assert.equal(h.node("node-metric-help-content").innerHTML, "");
        assert.equal(h.node("node-metric-help-portrait").src, "img/nodi-2d/nodi-attention.png");
    }
});

test("late documentation cannot overwrite a newer selection or reopen a closed panel", async () => {
    const pending = [];
    const h = setup({ load: () => new Promise(resolve => pending.push(resolve)) });
    h.controller.open(); h.select("PageRank");
    pending[1](bundle); await flush();
    pending[0](bundle); await flush();
    assert.equal(h.node("node-metric-help-title").textContent, "PageRank");
    assert.equal(h.node("node-metric-help-source").href, "help/node-metrics.html#pagerank");
    h.select("Degree"); h.controller.close();
    pending[2](bundle); await flush();
    assert.equal(h.panel.hidden, true);
    assert.equal(h.button.attrs["aria-expanded"], "false");
});

test("loading failures are distinct from missing documentation and can be retried", async () => {
    const h = setup({ fail: true }); h.controller.open(); await flush();
    assert.equal(h.node("node-metric-help-status").textContent, "Could not load metric help.");
    assert.equal(h.node("node-metric-help-retry").hidden, false);
    h.setFailure(false); h.node("node-metric-help-retry").listeners.click(); await flush();
    assert.match(h.node("node-metric-help-content").innerHTML, /number of adjacent links/);
    assert.equal(h.node("node-metric-help-status").textContent, "");
    assert.equal(h.node("node-metric-help-retry").hidden, true);
    assert.equal(h.requests.length, 2);
});

test("an empty selector stays disabled, and Escape respects modal dialogs and returns focus", async () => {
    const h = setup({ id: "" }); h.controller.open(); await flush();
    assert.equal(h.button.disabled, true); assert.equal(h.requests.length, 0);
    h.select("Degree"); assert.equal(h.button.disabled, false);
    h.controller.open(); await flush();
    h.context.modalOpen = true;
    h.listeners.keydown({ key: "Escape", preventDefault() {} });
    assert.equal(h.panel.hidden, false);
    h.context.modalOpen = false;
    h.listeners.keydown({ key: "Escape", preventDefault() {} });
    assert.equal(h.panel.hidden, true); assert.equal(h.button.focused, true);
});
