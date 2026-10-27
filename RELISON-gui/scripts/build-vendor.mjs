// Run `npm ci && npm run build:vendor` from RELISON-gui when updating dependencies.
// Commit the generated assets: normal Maven builds and end users do not need Node or npm.
import { build } from 'esbuild';
import { readFile, writeFile, mkdir, copyFile, readdir, unlink } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { createHash } from 'node:crypto';

const root = fileURLToPath(new URL('../', import.meta.url));
const output = path.join(root, 'src/main/resources/public/vendor');
const modules = path.join(root, 'node_modules');
let adaptedDuckDB = false;
await mkdir(output, { recursive: true });
async function copy(source, destination) {
    const target = path.join(output, destination);
    await mkdir(path.dirname(target), { recursive: true });
    await copyFile(path.join(modules, source), target);
}

for (const [source, destination] of [
    ['graphology/dist/graphology.umd.min.js', 'graphology.min.js'],
    ['graphology-library/dist/graphology-library.min.js', 'graphology-library.min.js'],
    ['sigma/dist/sigma.min.js', 'sigma.min.js'],
    ['echarts/dist/echarts.min.js', 'echarts.min.js'],
    ['marked/lib/marked.umd.js', 'marked.js'],
    ['dompurify/dist/purify.min.js', 'dompurify.min.js'],
    ['turndown/lib/turndown.browser.umd.js', 'turndown.js'],
    ['turndown-plugin-gfm/dist/turndown-plugin-gfm.js', 'turndown-plugin-gfm.js'],
    ['pdfmake/build/pdfmake.min.js', 'pdfmake.min.js'],
    ['pdfmake/build/vfs_fonts.js', 'pdfmake-fonts.js'],
    ['d3/dist/d3.min.js', 'd3.min.js'],
    ['topojson-client/dist/topojson-client.min.js', 'topojson-client.min.js'],
    ['world-atlas/countries-110m.json', 'countries-110m.json'],
    ['leaflet/dist/leaflet.js', 'leaflet/leaflet.js'],
    ['leaflet/dist/leaflet.css', 'leaflet/leaflet.css'],
    ['@fontsource-variable/material-symbols-outlined/files/material-symbols-outlined-latin-full-normal.woff2', 'fonts/material-symbols-outlined.woff2'],
]) await copy(source, destination);
for (const image of await readdir(path.join(modules, 'leaflet/dist/images')))
    await copy(`leaflet/dist/images/${image}`, `leaflet/images/${image}`);
for (const variant of ['mvp', 'eh']) {
    await copy(`@duckdb/duckdb-wasm/dist/duckdb-${variant}.wasm`, `duckdb/duckdb-${variant}.wasm`);
    await copy(`@duckdb/duckdb-wasm/dist/duckdb-browser-${variant}.worker.js`, `duckdb/duckdb-browser-${variant}.worker.js`);
}
await writeFile(path.join(output, 'fonts/material-symbols.css'), `/* Material Symbols Outlined; see ../licenses for the font license. */
@font-face {
  font-family: 'Material Symbols Outlined';
  font-style: normal;
  font-display: block;
  font-weight: 100 700;
  src: url('./material-symbols-outlined.woff2') format('woff2');
}
`);

// Cosmograph's internal DuckDB loader defaults to a CDN even when Cosmograph itself
// is bundled. Rewrite that pinned loader at build time; retain both WASM variants.
// Fail on upstream changes rather than silently producing a partly online bundle.
const localDuckDB = {
    name: 'local-duckdb',
    setup(builder) {
        builder.onLoad({ filter: /[\\/]@cosmograph[\\/]cosmograph[\\/]duckdb[\\/]duckdb-wasm\.js$/ }, async args => {
            let contents = await readFile(args.path, 'utf8');
            const original = 'const JSDELIVR_BUNDLES = duckdb.getJsDelivrBundles();';
            if (!contents.includes(original)) throw new Error('Cosmograph DuckDB loader changed; review the offline adapter.');
            adaptedDuckDB = true;
            const variants = Object.fromEntries(['mvp', 'eh'].map(variant => [variant, {
                mainModule: `duckdb-${variant}.wasm`, mainWorker: `duckdb-browser-${variant}.worker.js`,
            }]));
            const expression = JSON.stringify(variants).replace(/"(duckdb-[^"]+)"/g,
                (_, file) => `new URL("vendor/duckdb/${file}", document.baseURI).href`);
            contents = contents.replace(original, `const JSDELIVR_BUNDLES = ${expression};`)
                .replace('SET max_expression_depth TO 20000000;',
                    'SET autoinstall_known_extensions = false; SET autoload_known_extensions = false; SET max_expression_depth TO 20000000;');
            return { contents, loader: 'js' };
        });
    },
};
const entries = {
    cosmograph: 'export { Cosmograph, prepareCosmographData } from "@cosmograph/cosmograph";',
    'sigma-edge-curve': 'export { default, EdgeCurvedArrowProgram } from "@sigma/edge-curve";',
    globe: 'export { default } from "globe.gl";',
    three: 'export * from "three";',
    topojson: 'export * from "topojson-client";',
    gifenc: 'export { GIFEncoder, quantize, applyPalette } from "gifenc/src/index.js";',
};
const entryDirectory = path.join(root, 'target/vendor-entries');
await mkdir(entryDirectory, { recursive: true });
for (const [name, contents] of Object.entries(entries)) await writeFile(path.join(entryDirectory, `${name}.js`), contents);
const result = await build({
    absWorkingDir: root,
    entryPoints: Object.keys(entries).map(name => path.join(entryDirectory, `${name}.js`)),
    outdir: path.join(output, 'esm'), bundle: true, splitting: true, format: 'esm',
    platform: 'browser', target: ['es2020'], minify: true, legalComments: 'linked',
    chunkNames: 'chunks/[name]-[hash]', metafile: true, write: false,
    define: { 'process.env.NODE_ENV': '"production"' }, plugins: [localDuckDB],
});
if (!adaptedDuckDB) throw new Error('The Cosmograph DuckDB adapter was not applied; refusing to publish online assets.');
for (const built of Object.values(result.metafile.outputs)) for (const dependency of built.imports)
    if (dependency.external) throw new Error(`Unbundled import: ${dependency.path}`);
for (const file of result.outputFiles) {
    await mkdir(path.dirname(file.path), { recursive: true });
    await writeFile(file.path, file.contents);
}
// Remove only obsolete generated ESM files listed by the preceding build. Never
// recursively remove a computed directory or follow paths outside the vendor tree.
let previous = {};
try { previous = JSON.parse(await readFile(path.join(output, 'manifest.json'), 'utf8')); } catch (error) {
    if (error.code !== 'ENOENT') throw error;
}
const generated = new Set(result.outputFiles.map(file => path.resolve(file.path)));
for (const relative of Object.keys(previous).filter(file => file.startsWith('esm/'))) {
    const absolute = path.resolve(output, relative);
    if (!absolute.startsWith(path.resolve(output, 'esm') + path.sep)) throw new Error('Invalid generated asset path.');
    if (!generated.has(absolute)) await unlink(absolute).catch(error => { if (error.code !== 'ENOENT') throw error; });
}

const lock = JSON.parse(await readFile(path.join(root, 'package-lock.json'), 'utf8'));
const packages = [];
for (const [directory, info] of Object.entries(lock.packages)) {
    if (!directory || !directory.includes('node_modules/')) continue;
    const folder = path.join(root, directory);
    let files;
    try { files = await readdir(folder); } catch { continue; } // Other platforms' optional esbuild binaries.
    const name = directory.replaceAll('node_modules/', '').replaceAll('/', '__');
    const notices = files.filter(file => /^(license|licence|copying|notice|copyright)(\.|$)/i.test(file));
    for (const file of notices) await copy(`${directory.slice('node_modules/'.length)}/${file}`, `licenses/${name}/${file}`);
    packages.push({ name: info.name || directory.split('node_modules/').at(-1), version: info.version,
        license: info.license, notices: notices.map(file => `licenses/${name}/${file}`) });
}
await writeFile(path.join(output, 'THIRD-PARTY.json'), JSON.stringify(packages, null, 2) + '\n');
await writeFile(path.join(output, 'README.txt'), `Generated with npm ci && npm run build:vendor in RELISON-gui.
All runtime browser libraries, icon fonts, DuckDB workers/WASM and Natural Earth
country boundaries are served locally. THIRD-PARTY.json lists package licenses.
Cosmograph retains its attribution and CC-BY-NC-4.0 license.
World Atlas boundary data is derived from public-domain Natural Earth data.
Online street tiles are optional and are not included in this directory.
`);
const hashes = {};
async function inventory(folder) {
    for (const file of await readdir(folder, { withFileTypes: true })) {
        const absolute = path.join(folder, file.name);
        if (file.isDirectory()) await inventory(absolute);
        else if (file.name !== 'manifest.json') hashes[path.relative(output, absolute).replaceAll('\\', '/')] =
            createHash('sha256').update(await readFile(absolute)).digest('hex');
    }
}
await inventory(output);
await writeFile(path.join(output, 'manifest.json'), JSON.stringify(hashes, null, 2) + '\n');
console.log(`Packaged ${Object.keys(hashes).length} local assets and notices.`);
