/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const source = fs.readFileSync(path.join(__dirname, "../../main/resources/public/diffusion-cosmograph.js"), "utf8")
    .replace(/^import .*;\s*/m, "");

function setup() {
    const configs = [], positionUpdates = [];
    class Cosmograph {
        constructor(_container, config) {
            configs.push(config);
            this.config = config;
            this._graph = {
                setPointColors() {}, setLinkColors() {}, setPointSizes() {}, setLinkWidths() {}, setLinkStyles() {},
                setPointPositions: (positions, options) => positionUpdates.push({ positions: Array.from(positions), options }),
                render() {},
            };
        }
        async dataUploaded() {}
        stop() {}
        destroy() {}
        resize() {}
    }
    const graph = {
        type: "directed", nodes: () => ["a", "b"], edges: () => ["ab"],
        getNodeAttributes: node => ({ x: node === "a" ? 1 : 4, y: node === "a" ? 2 : 5, label: node, size: 3, color: "#fff" }),
        getNodeAttribute: (node, key) => ({ x: node === "a" ? 1 : 4, y: node === "a" ? 2 : 5, size: 3 }[key]),
        source: () => "a", target: () => "b", getEdgeAttribute: () => 1,
    };
    const window = { relisonCosmographLabelOpts: {}, dispatchEvent() {} };
    const context = vm.createContext({ Cosmograph, window, graph,
        document: { getElementById: id => id === "cosmograph-sphere-shading" ? { checked: true } : null,
            createElement: () => ({ getContext: () => ({ fillRect() {}, drawImage() {} }) }) },
        CustomEvent: function () {}, Float32Array, Map, Set, Object, Number, String, Math,
        requestAnimationFrame: callback => callback(), getComputedStyle: () => ({ getPropertyValue: () => "#000" }),
    });
    vm.runInContext(source, context);
    return { api: window.relisonDiffusionCosmograph, configs, positionUpdates, graph };
}

for (const dimensions of [2, 3]) test(`diffusion Cosmograph uses network coordinates in ${dimensions}D`, async () => {
    const h = setup();
    const positions = { a: { x: 10, y: 20, z: 30 }, b: { x: 40, y: 50, z: 60 } };
    const view = await h.api.create(h.graph, {}, null, { dimensions, positions });
    const config = h.configs[0];
    assert.equal(config.spaceDimensions, dimensions);
    assert.equal(config.pointZBy, dimensions === 3 ? "z" : undefined);
    assert.equal(config.pointSphereShading, dimensions === 3);
    assert.deepEqual(config.points.map(point => [point.x, point.y, point.z]), dimensions === 3
        ? [[10, 20, 30], [40, 50, 60]] : [[10, 20, 0], [40, 50, 0]]);
    assert.equal(view.getDimensions(), dimensions);

    await view.setIteration(0, { iterations: [{ newlyInformed: ["a"], propagating: [] }] },
        { base: "#000000", informed: "#0000ff", newly: "#00ff00", prop: "#ff0000" });
    assert.equal(h.positionUpdates[0].options.dimensions, dimensions);
    assert.equal(h.positionUpdates[0].positions.length, 2 * dimensions);
    if (dimensions === 3) {
        await view.syncAppearance(h.graph, null, { a: { x: 11, y: 21, z: 31 }, b: { x: 41, y: 51, z: 61 } });
        assert.deepEqual(h.positionUpdates.at(-1).positions, [11, 21, 31, 41, 51, 61]);
    }
});
