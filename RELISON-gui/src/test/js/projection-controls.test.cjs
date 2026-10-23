/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
const test = require('node:test'), assert = require('node:assert/strict');
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/public/app.js'), 'utf8');
function extract(name) { const start = source.indexOf('function ' + name + '('); return source.slice(start, source.indexOf('\n}', start) + 2); }
test('projection restores 2D transforms and hides every 3D-only camera control', () => {
    const fields = new Map(); let dimensions = 3, gizmos = 0;
    const $ = id => { if (!fields.has(id)) fields.set(id, { value: '', hidden: false }); return fields.get(id); };
    const context = vm.createContext({ $, usingCosmograph: () => true, usingGeographic: () => false,
        window: { relisonCosmograph: { getDimensions: () => dimensions, refreshOrientationGizmo: () => gizmos++ } },
        toggleHidden: (id, hidden) => { $(id).hidden = hidden; }, stopNetworkRotation() {},
    });
    vm.runInContext(['network2DTransformsAvailable', 'activeRotationRenderer', 'syncRotationExportControls', 'syncNetworkDimensionControls'].map(extract).join('\n'), context);
    $('network-dimensions').value = '3'; context.syncNetworkDimensionControls();
    assert.equal($('btn-network-rotate').hidden, true);
    dimensions = 2; $('network-dimensions').value = '2'; context.syncNetworkDimensionControls();
    for (const id of ['cosmograph-sphere-shading-field', 'network-3d-help', 'btn-project-network-2d', 'cosmograph-rotation-controls', 'btn-export-rotation-gif', 'btn-export-rotation-webm', 'rotation-export-resolution-field'])
        assert.equal($(id).hidden, true, id);
    for (const id of ['btn-network-rotate', 'btn-network-mirror-x', 'btn-network-mirror-y']) assert.equal($(id).hidden, false, id);
    assert.equal(gizmos, 2);
});
test('Project to 2D runs the shared control refresh after success and rollback', async () => {
    const start = source.indexOf('$("btn-project-network-2d").addEventListener("click", async () => {');
    const binding = source.slice(start, source.indexOf('\n});', start) + 4);
    for (const fail of [false, true]) {
        let callback, dimensions = 3, syncs = 0;
        const elements = new Map(), $ = id => { if (!elements.has(id)) elements.set(id, { value: '3', addEventListener: (_, handler) => { callback = handler; } }); return elements.get(id); };
        const context = vm.createContext({ $, state: { renderer: null, diffusion: {} }, stopLayout() {}, drawRecOverlay() {}, setStatus() {}, toggleHidden() {},
            syncLayoutOptionsForRenderer: () => { syncs++; assert.equal($('network-dimensions').value, String(dimensions)); },
            window: { relisonCosmograph: { getDimensions: () => dimensions, projectTo2D: async () => { if(fail)throw new Error('projection failed'); dimensions = 2; return 4; } } },
        });
        vm.runInContext(binding, context); await callback();
        assert.equal(syncs, 1); assert.equal($('network-dimensions').disabled, false);
        assert.equal($('btn-project-network-2d').disabled, false);
    }
});
