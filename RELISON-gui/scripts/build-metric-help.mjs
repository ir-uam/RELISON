// Generate checked-in, offline help from the project's RST; no service or API is used.
import { readFile, writeFile, mkdir, copyFile, readdir } from 'node:fs/promises';
import katex from 'katex';
import { marked, Renderer } from 'marked';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const moduleRoot = fileURLToPath(new URL('../', import.meta.url));
const escape = text => String(text).replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch]);
const anchor = title => title.toLowerCase().replace(/[^a-z0-9]+/g, '-');

// Typeset the documentation at build time: the browser needs only local CSS/fonts.
// Keep accessible MathML alongside HTML so display does not depend on native MathML.
export function mathMarkup(source, display = false) {
    return katex.renderToString(source, {
        displayMode: display, output: 'htmlAndMathml', throwOnError: true,
        strict: 'error', trust: false,
        // The RST uses the traditional text-box command; keep its upright text.
        macros: { '\\mbox': '\\text{#1}' },
    });
}

function inline(text, page = 'node-metrics.html') {
    const tokens = /:math:`([^`]+)`|:code:`([^`]+)`|`([^`]+)`_|\*\*([^*]+)\*\*|\*([^*]+)\*|:math:((?:\([^()\s]*\)|[^()\s`,;.])+)/g;
    let html = '', end = 0;
    for (const match of text.matchAll(tokens)) {
        html += escape(text.slice(end, match.index));
        if (match[1] || match[6]) html += mathMarkup(match[1] || match[6]);
        else if (match[2]) html += `<code>${escape(match[2])}</code>`;
        else if (match[3]) html += `<a href="help/${page}#${anchor(match[3])}" target="_blank" rel="noopener">${escape(match[3])}</a>`;
        else if (match[4]) html += `<strong>${escape(match[4])}</strong>`;
        else html += `<em>${escape(match[5])}</em>`;
        end = match.index + match[0].length;
    }
    return html + escape(text.slice(end));
}

export function renderSection(raw, { page = 'node-metrics.html' } = {}) {
    const renderInline = text => inline(text, page);
    // YAML is for configuring batch runs, not the interactive metric selector.
    const excerpt = raw.split(/\nConfiguration file\n\^+\n/)[0];
    const lines = excerpt.replace(/\t/g, '    ').split('\n');
    let index = 0;
    const isHeading = i => lines[i]?.trim() && /^\^{3,}$/.test(lines[i + 1]?.trim() || '');
    const isBullet = line => /^(\s*)\* /.test(line || '');
    function list(indent) {
        let html = '<ul>';
        while (index < lines.length) {
            if (!lines[index].trim()) { index++; continue; }
            const match = lines[index].match(/^(\s*)\* (.*)/);
            if (!match || match[1].length !== indent) break;
            index++; const text = [match[2]];
            while (index < lines.length && lines[index].trim() && !isBullet(lines[index]) && /^\s+/.test(lines[index])) text.push(lines[index++].trim());
            html += `<li>${renderInline(text.join(' '))}`;
            while (!lines[index]?.trim() && index < lines.length) index++;
            if (isBullet(lines[index]) && lines[index].match(/^\s*/)[0].length > indent) html += list(lines[index].match(/^\s*/)[0].length);
            html += '</li>';
        }
        return html + '</ul>';
    }
    let html = '';
    while (index < lines.length) {
        const text = lines[index].trim();
        if (!text || /^\.\. _/.test(text)) { index++; continue; }
        if (isHeading(index)) { html += `<h3>${renderInline(text)}</h3>`; index += 2; continue; }
        if (text === '.. math::') {
            index++; const formula = [];
            while (index < lines.length && (!lines[index].trim() || /^\s/.test(lines[index]))) {
                if (lines[index].trim()) formula.push(lines[index].trim());
                index++;
            }
            html += `<div class="metric-help-equation" tabindex="0" aria-label="Formula">${mathMarkup(formula.join(' '), true)}</div>`;
            continue;
        }
        // A few pages place their YAML directly after the description, without a heading.
        if (text === '.. code:: yaml') {
            index++;
            while (index < lines.length && (!lines[index].trim() || /^\s/.test(lines[index]))) index++;
            continue;
        }
        if (isBullet(lines[index])) { html += list(lines[index].match(/^\s*/)[0].length); continue; }
        const paragraph = [];
        while (index < lines.length && lines[index].trim() && !isHeading(index) && !isBullet(lines[index]) && !lines[index].trim().startsWith('.. ')) paragraph.push(lines[index++].trim());
        if (!paragraph.length) throw new Error(`Unsupported documentation directive: ${lines[index]}`);
        html += `<p>${renderInline(paragraph.join(' '))}</p>`;
    }
    return html;
}

const documents = {
    node: ['Node metrics', 'docs/source/sna/metrics/vertex.rst', 'node-metrics.html'],
    graph: ['Graph metrics', 'docs/source/sna/metrics/graph.rst', 'graph-metrics.html'],
    pair: ['Edge and node-pair metrics', 'docs/source/sna/metrics/pair.rst', 'pair-metrics.html'],
    community: ['Individual community metrics', 'docs/source/sna/metrics/indivcomm.rst', 'community-metrics.html'],
    'community-global': ['Global community metrics', 'docs/source/sna/metrics/globalcomm.rst', 'community-global-metrics.html'],
    boundary: ['Community boundary metrics', 'RELISON-sna/COMMUNITY_BOUNDARY_METRICS.md', 'community-boundary-metrics.html'],
    'edge-partition': ['Edge partition metrics', 'RELISON-sna/EDGE_PARTITION_METRICS.md', 'edge-partition-metrics.html'],
    'models-foaf': ['Friends-of-friends models', 'docs/source/link/algorithms/foaf.rst', 'models-foaf.html'],
    'models-ir': ['Information retrieval models', 'docs/source/link/algorithms/ir.rst', 'models-ir.html'],
    'models-paths': ['Path-based models', 'docs/source/link/algorithms/paths.rst', 'models-paths.html'],
    'models-randomwalks': ['Random-walk models', 'docs/source/link/algorithms/randomwalks.rst', 'models-randomwalks.html'],
    'models-twitter': ['Twitter models', 'docs/source/link/algorithms/twitter.rst', 'models-twitter.html'],
    'models-mf': ['Matrix factorization models', 'docs/source/link/algorithms/mf.rst', 'models-mf.html'],
    'models-knn': ['Nearest-neighbor models', 'docs/source/link/algorithms/knn.rst', 'models-knn.html'],
    'models-baselines': ['Baseline models', 'docs/source/link/algorithms/baselines.rst', 'models-baselines.html'],
    'diffusion-protocol': ['Diffusion protocols', 'docs/source/diffusion/protocols/protocols.rst', 'diffusion-protocols.html'],
    'diffusion-selection': ['Diffusion selection mechanisms', 'docs/source/diffusion/protocols/selection.rst', 'diffusion-selection.html'],
    'diffusion-expiration': ['Diffusion expiration mechanisms', 'docs/source/diffusion/protocols/expiration.rst', 'diffusion-expiration.html'],
    'diffusion-propagation': ['Diffusion propagation mechanisms', 'docs/source/diffusion/protocols/propagation.rst', 'diffusion-propagation.html'],
    'diffusion-update': ['Diffusion update mechanisms', 'docs/source/diffusion/protocols/update.rst', 'diffusion-update.html'],
    'diffusion-sight': ['Diffusion sight mechanisms', 'docs/source/diffusion/protocols/sight.rst', 'diffusion-sight.html'],
    'diffusion-stop': ['Diffusion stop conditions', 'docs/source/diffusion/protocols/stop.rst', 'diffusion-stop.html'],
    'diffusion-pieces': ['Diffusion information-piece metrics', 'docs/source/diffusion/metrics/infopiece.rst', 'diffusion-piece-metrics.html'],
    'diffusion-creators': ['Diffusion creator metrics', 'docs/source/diffusion/metrics/user.rst', 'diffusion-creator-metrics.html'],
    'diffusion-features': ['Diffusion feature metrics', 'docs/source/diffusion/metrics/features.rst', 'diffusion-feature-metrics.html'],
};
export const families = ['node', 'graph', 'pair', 'community', 'community-global', 'edge-partition', 'edge-partition-global', 'recommendation',
    'diffusion-protocol', 'diffusion-selection', 'diffusion-expiration', 'diffusion-propagation', 'diffusion-update', 'diffusion-sight', 'diffusion-stop', 'diffusion-metric'];
const aliases = {
    node: {
        'Inverse degree': 'Degree', 'Complementary degree': 'Degree', 'Complementary inverse degree': 'Degree',
        'Complementary local clustering coefficient': 'Local clustering coefficient', 'Complementary PageRank': 'PageRank',
        Eigenvector: 'Eigenvector centrality', Harmonic: 'Harmonic centrality', Katz: 'Katz centrality',
    },
    graph: {
        ASL: 'Average shortest path length', ARSL: 'Average reciprocal shortest path length',
        'Degree Pearson': 'Degree Pearson correlation', Reciprocity: 'Reciprocity rate',
        'Inter edge Gini complement': 'Edge Gini complement', 'Complete edge Gini complement': 'Edge Gini complement',
        'Semi-complete edge Gini complement': 'Edge Gini complement',
    },
    pair: {
        'Reciprocal shortest path length': 'Distance', 'Complementary neighbor overlap': 'Neighbor overlap',
        'Log weighted neighbor overlap': 'Weighted neighbor overlap', 'Expanded common neighbors': 'Extended neighbor overlap',
        'Expanded common neighbors count': 'Extended neighbor overlap', 'Complementary embeddedness': 'Embeddedness',
        Weakness: 'Embeddedness', 'Shrinking ASL': 'Shrinking average shortest path length',
        'Shrinking neighbors ASL': 'Shrinking average shortest path length', 'Shrinking neighbors diameter': 'Shrinking diameter',
    },
    'community-global': {
        'Num. communities': 'Number of communities', 'Community Size Gini': 'Size Gini complement',
        'Inter-community degree Gini': 'Degree Gini complement', 'Complete community degree Gini': 'Degree Gini complement',
        'Inter-community edge Gini complement': 'Edge Gini complement', 'Complete community edge Gini complement': 'Edge Gini complement',
        'Semi-complete community edge Gini complement': 'Edge Gini complement',
        // Size-normalized and Dice variants are not defined in this documentation.
    },
};

function markdown(text) {
    const renderer = new Renderer();
    renderer.html = ({ text }) => escape(text);
    return marked.parse(text, { renderer });
}

async function readDocument(key) {
    const [label, sourcePath, page] = documents[key];
    const raw = (await readFile(path.join(moduleRoot, '..', sourcePath), 'utf8')).replace(/\r\n/g, '\n');
    const url = sourcePath.endsWith('.rst')
        ? 'https://relison.readthedocs.io/en/latest/' + sourcePath.replace(/^docs\/source\//, '').replace(/\.rst$/, '.html') : null;
    const source = { path: sourcePath, sha256: createHash('sha256').update(raw).digest('hex'), url, page };
    const sections = {};
    if (sourcePath.endsWith('.rst')) {
        const headings = [...raw.matchAll(/^([^\n]+)\n~{3,}\s*\n/gm)];
        for (let i = 0; i < headings.length; i++) {
            const title = headings[i][1], text = raw.slice(headings[i].index + headings[i][0].length, headings[i + 1]?.index ?? raw.length).trim();
            sections[title] = { title, anchor: anchor(title), text, html: renderSection(text, { page }), source };
        }
    } else {
        // Markdown definitions are table rows; retain the original prose and shared conventions.
        const headings = [...raw.matchAll(/^## (.+)$/gm)];
        const blocks = Object.fromEntries(headings.map((heading, i) => [heading[1], raw.slice(heading.index, headings[i + 1]?.index ?? raw.length).trim()]));
        const boundary = key === 'boundary';
        for (const match of raw.matchAll(/^\| `([^`]+)` \| (.+) \|$/gm)) {
            const title = match[1], definition = match[2];
            const individual = raw.indexOf(match[0]) < raw.indexOf('## Global metrics');
            const block = boundary ? '' : blocks[individual ? 'Individual metrics' : 'Global metrics'];
            // Table rows are individual definitions, not prose; omit the other metrics' rows.
            const conventions = boundary ? blocks['Direction and weights'] : block.slice(block.lastIndexOf('\n|') + 1).split('\n').slice(1).join('\n').trim();
            const text = [definition, conventions,
                boundary && title === 'ModularityContribution' ? blocks['Modularity contribution'] : '',
                boundary ? blocks.Sources : ''].filter(Boolean).join('\n\n');
            sections[title] = { title, anchor: anchor(title), text, html: markdown(text), source };
        }
        // Retain non-table documentation as linked source sections as well.
        for (const [title, text] of Object.entries(blocks)) {
            sections[title] = { title, anchor: anchor(title), text, html: markdown(text.replace(/^## .+\n/, '')), source };
        }
    }
    return { label, source, sections };
}

async function catalogue(file, method, identifierPath, identifierClass, domain = 'sna') {
    const identifiers = await readFile(path.join(moduleRoot, '../RELISON-examples/src/main/java/es/uam/eps/ir/relison/grid', domain, identifierPath), 'utf8');
    const ids = Object.fromEntries([...identifiers.matchAll(/String\s+(\w+)\s*=\s*"([^"]+)"/g)].map(match => [match[1], match[2]]));
    const java = await readFile(path.join(moduleRoot, 'src/main/java/es/uam/eps/ir/relison/gui', file), 'utf8');
    const matcher = new RegExp(`${method}\\(${identifierClass}\\.(\\w+), "([^"]+)"(?:, "([^"]+)")?`, 'g');
    return [...java.matchAll(matcher)].map(match => {
        if (!ids[match[1]]) throw new Error(`Unresolved metric identifier: ${match[1]}`);
        return { id: ids[match[1]], label: match[2], group: match[3] };
    });
}

async function buildRecommendationData() {
    const groups = { 'Friends of friends': 'foaf', 'Information retrieval': 'ir', 'Path-based': 'paths',
        'Random walks': 'randomwalks', Twitter: 'twitter', 'Matrix factorization': 'mf',
        'Nearest neighbours': 'knn', Baselines: 'baselines' };
    const modelAliases = {
        MCN: 'Most common neighbors', Cosine: 'Cosine similarity', Sorensen: 'Sørensen',
        'Local LHN': 'Local Leicht-Holme-Newman index', EBM25: 'Extreme BM25', VSM: 'Vector space model',
        'Pivoted normalization VSM': 'Pivoted normalization vector space model',
        QLJM: 'Query likelihood', QLD: 'Query likelihood', QLL: 'Query likelihood', BIR: 'Binary independent retrieval',
        Distance: 'Shortest path distance', 'Global LHN': 'Global Leicht-Holme-Newman index',
        'Pseudo-inverse cosine': 'Pseudo inverse cosine',
        'Commute time PageRank': 'Commute time', 'Commute time personalized PageRank': 'Commute time',
        'Hitting time PageRank': 'Hitting time', 'Hitting time personalized PageRank': 'Hitting time',
        'Average cosine': 'Cosine similarity', 'Centroid cosine': 'Cosine similarity', 'Maximum cosine': 'Cosine similarity',
        iMF: 'Implicit matrix factorization', 'Fast iMF': 'Fast implicit matrix factorization',
        'UB kNN': 'User-based kNN', 'IB kNN': 'Item-based kNN',
        Popularity: 'Popularity-based recommendation', Random: 'Random recommendation',
    };
    const sections = {}, metrics = {}, sources = {};
    for (const key of Object.values(groups)) {
        const document = await readDocument('models-' + key);
        sources[key] = document.source;
        for (const [title, section] of Object.entries(document.sections)) sections[key + ':' + title] = section;
    }
    for (const { id, label, group } of await catalogue('RecommendationCatalog.java', 'add', 'AlgorithmIdentifiers.java',
        'AlgorithmIdentifiers', 'links/recommendation/algorithms')) {
        const section = groups[group] + ':' + (modelAliases[id] || id);
        // Identity and aliases select a documented section; they never supply an explanation.
        if (Object.hasOwn(sections, section)) metrics[id] = { label, section };
    }
    return { source: sources.foaf, metrics, sections };
}

async function buildDiffusionData(family) {
    const configurations = {
        'diffusion-protocol': ['protocol', 'protocol', 'ProtocolIdentifiers'],
        'diffusion-selection': ['select', 'selection', 'SelectionMechanismIdentifiers'],
        'diffusion-expiration': ['expir', 'expiration', 'ExpirationMechanismIdentifiers'],
        'diffusion-propagation': ['propag', 'propagation', 'PropagationMechanismIdentifiers'],
        'diffusion-update': ['update', 'update', 'UpdateMechanismIdentifiers'],
        'diffusion-sight': ['sight', 'sight', 'SightMechanismIdentifiers'],
        'diffusion-stop': ['stop', 'stop', 'StopConditionIdentifiers'],
        'diffusion-metric': ['metric', 'metrics', 'MetricIdentifiers'],
    };
    const diffusionAliases = {
        'diffusion-protocol': { 'Bidirectional rumor spreading': 'Rumor spreading', 'Proportion threshold': 'Threshold', 'Count threshold': 'Threshold' },
        'diffusion-selection': { 'Push-pull': 'Pull-push', 'Proportion threshold': 'Threshold', 'Count threshold': 'Threshold',
            'Pure timestamp-based': 'Timestamp-based', 'Loose timestamp-based': 'Timestamp-based' },
        'diffusion-propagation': { 'Push-pull': 'Pull-push' },
        'diffusion-sight': { 'All sight': 'All' },
        'diffusion-stop': { 'Num. iter': 'Num. Iter', 'No more timestamps nor propagated': 'No more timestamps nor propagated information' },
        'diffusion-metric': {
            'Individual real propagated recall': 'Real propagated recall', 'Global real propagated recall': 'Real propagated recall',
            'Global creator Gini complement': 'Creator Gini complement', 'Individual creator Gini complement': 'Creator Gini complement',
            'Global creator entropy': 'Creator entropy', 'Individual creator entropy': 'Creator entropy',
            'Individual feature Gini complement': 'Feature Gini complement', 'Global feature Gini complement': 'Feature Gini complement',
            'Global feature entropy': 'Feature entropy', 'Global feature KLD': 'Feature KL divergence',
        },
    };
    const [method, directory, identifiers] = configurations[family];
    const documentKeys = family === 'diffusion-metric' ? ['diffusion-pieces', 'diffusion-creators', 'diffusion-features'] : [family];
    const sections = {}, metrics = {}, sources = {};
    for (const key of documentKeys) {
        const document = await readDocument(key);
        sources[key] = document.source;
        for (const [title, section] of Object.entries(document.sections)) sections[key + ':' + title] = section;
    }
    const entries = await catalogue('DiffusionCatalog.java', method, identifiers + '.java', identifiers, 'diffusion/' + directory);
    if (family === 'diffusion-metric') entries.push(...await catalogue('DiffusionCatalog.java', 'metricRealProp', identifiers + '.java', identifiers, 'diffusion/' + directory));
    for (const { id, label, group } of entries) {
        const key = family === 'diffusion-metric' ? ({ 'Information pieces': 'diffusion-pieces', Creators: 'diffusion-creators', Features: 'diffusion-features' })[group] : family;
        const section = key + ':' + (diffusionAliases[family]?.[id] || id);
        if (Object.hasOwn(sections, section)) metrics[id] = { label, section };
    }
    return { source: sources[documentKeys[0]], metrics, sections };
}

export async function buildData(family = 'node') {
    if (!families.includes(family)) throw new Error(`Unknown help family: ${family}`);
    if (family === 'recommendation') return buildRecommendationData();
    if (family.startsWith('diffusion-')) return buildDiffusionData(family);
    const sourceKey = family === 'edge-partition-global' ? 'edge-partition' : family;
    const document = await readDocument(sourceKey), sections = { ...document.sections }, metrics = {};
    const add = (id, label, section) => {
        if (Object.hasOwn(sections, section)) metrics[id] = { label, section };
        // Missing entries deliberately have no generated description.
    };
    const javaSources = {
        node: ['MetricCatalog.java', 'vertex', 'vertex/VertexMetricIdentifiers.java', 'VertexMetricIdentifiers'],
        graph: ['MetricCatalog.java', 'graph', 'graph/GraphMetricIdentifiers.java', 'GraphMetricIdentifiers'],
        pair: ['PairMetricCatalog.java', 'pair', 'pair/PairMetricIdentifiers.java', 'PairMetricIdentifiers'],
        community: ['CommunityCatalog.java', 'indiv', 'comm/indiv/IndividualCommunityMetricIdentifiers.java', 'IndividualCommunityMetricIdentifiers'],
        'community-global': ['CommunityCatalog.java', 'global', 'comm/global/GlobalCommunityMetricIdentifiers.java', 'GlobalCommunityMetricIdentifiers'],
    };
    if (javaSources[family]) {
        const entries = await catalogue(...javaSources[family]);
        for (const { id, label } of entries) add(id, label, aliases[family]?.[id] || id);
        if (family === 'community') {
            const boundary = await readDocument('boundary');
            const boundaryRows = {
                'Boundary nodes': 'BoundaryNodes', 'Boundary-node ratio': 'BoundaryNodeRatio',
                'Distinct neighboring communities': 'NeighboringCommunities', 'Internal volume fraction': 'InternalVolumeFraction',
                Conductance: 'Conductance', 'Edge expansion': 'EdgeExpansion', 'Modularity contribution (Newman)': 'ModularityContribution',
            };
            for (const [id, section] of Object.entries(boundaryRows)) {
                sections['boundary:' + section] = boundary.sections[section];
                add(id, entries.find(entry => entry.id === id)?.label || id, 'boundary:' + section);
            }
        }
    } else {
        const edgeRows = {
            'edge-partition': { size: 'Size', 'edge-share': 'EdgeShare', 'node-coverage': 'NodeCoverage',
                'source-coverage': 'NodeCoverage', 'target-coverage': 'NodeCoverage', 'total-weight': 'TotalWeight',
                'shared-nodes': 'SharedNodes', 'shared-node-ratio': 'SharedNodeRatio', 'group-degree': 'GroupDegree',
                components: 'Components', 'largest-component-share': 'LargestComponentShare' },
            'edge-partition-global': { 'num-groups': 'NumberOfGroups', 'nonempty-groups': 'NumberOfGroups',
                'assignment-coverage': 'AssignmentCoverage', 'group-size-gini': 'GroupSizeGini', 'group-weight-gini': 'GroupWeightGini',
                'group-size-entropy': 'GroupSizeEntropy', 'node-mixing-ratio': 'NodeMixingRatio', 'mean-node-participation': 'MeanNodeParticipation' },
        };
        const java = await readFile(path.join(moduleRoot, 'src/main/java/es/uam/eps/ir/relison/gui/EdgePartitionMetricCatalog.java'), 'utf8');
        const matcher = new RegExp(`${family === 'edge-partition' ? 'INDIVIDUAL' : 'GLOBAL'}\\.put\\("([^"]+)", new MetricCatalog\\.MetricDef\\("[^"]+", "([^"]+)"`, 'g');
        for (const match of java.matchAll(matcher)) add(match[1], match[2], edgeRows[family][match[1]]);
    }
    if (family === 'community' || family === 'edge-partition') {
        const graph = await buildData('graph');
        for (const [key, section] of Object.entries(graph.sections)) sections['graph:' + key] = section;
        const prefixes = family === 'community' ? ['intra-community:', 'intra-attribute:'] : ['intra-edge-partition:'];
        for (const prefix of prefixes) for (const [id, entry] of Object.entries(graph.metrics)) {
            add(prefix + id, entry.label, 'graph:' + entry.section);
        }
    }
    return { source: document.source, metrics, sections };
}

export async function buildHelp() {
    const directory = path.join(moduleRoot, 'src/main/resources/public/help');
    await mkdir(directory, { recursive: true });
    const mathAssets = path.join(directory, 'katex');
    const katexDist = path.join(moduleRoot, 'node_modules/katex/dist');
    await mkdir(path.join(mathAssets, 'fonts'), { recursive: true });
    await copyFile(path.join(katexDist, 'katex.min.css'), path.join(mathAssets, 'katex.min.css'));
    await copyFile(path.join(katexDist, '../LICENSE'), path.join(mathAssets, 'LICENSE'));
    for (const font of await readdir(path.join(katexDist, 'fonts'))) {
        await copyFile(path.join(katexDist, 'fonts', font), path.join(mathAssets, 'fonts', font));
    }
    const bundles = {};
    for (const family of families) {
        const data = await buildData(family);
        bundles[family] = data;
        await writeFile(path.join(directory, `${family}-metrics.json`), JSON.stringify(data, null, 2) + '\n');
    }
    for (const key of Object.keys(documents)) {
        const data = await readDocument(key);
        const style = 'body{max-width:1000px;margin:36px auto;padding:0 24px;font:16px/1.7 system-ui;color:#17283a}h1,h2,h3{line-height:1.3}section{border-top:1px solid #ccd7e2;margin-top:36px;padding-top:20px}.metric-help-equation{overflow:auto;padding:14px 0}.katex{font-size:1.05em}.katex-display,.katex-display>.katex{text-align:left}a{color:#1767ac}code{background:#eef3f8;padding:2px 4px}table{border-collapse:collapse}td,th{border:1px solid #ccd7e2;padding:8px;text-align:left}';
        await writeFile(path.join(directory, data.source.page), `<!DOCTYPE html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>RELISON ${data.label}</title><link rel="stylesheet" href="katex/katex.min.css"><style>${style}</style></head><body><nav><a href="metrics.html">All help documentation</a></nav><h1>${data.label}</h1><p>Descriptions, formulas, parameters and references from ${data.source.url ? `<a href="${data.source.url}">the project documentation</a>` : 'the project documentation'}. YAML configuration examples are omitted.</p><p><code>${data.source.path}</code></p><nav>${Object.values(data.sections).map(section => `<a href="#${section.anchor}">${section.title}</a>`).join(' · ')}</nav>${Object.values(data.sections).map(section => `<section id="${section.anchor}"><h2>${section.title}</h2>${section.html.replaceAll('href="help/' + data.source.page + '#', 'href="#')}</section>`).join('\n')}</body></html>\n`);
    }
    await writeFile(path.join(directory, 'metrics.html'), `<!DOCTYPE html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>RELISON help documentation</title></head><body><h1>RELISON help documentation</h1><ul>${Object.values(documents).map(([label, , page]) => `<li><a href="${page}">${label}</a></li>`).join('')}</ul></body></html>\n`);
    return bundles;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    const bundles = await buildHelp();
    for (const [family, data] of Object.entries(bundles)) console.log(`Generated ${family} help: ${Object.keys(data.metrics).length} documented entries.`);
}
