/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
const test = require("node:test"), assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), vm = require("node:vm");
const raw = fs.readFileSync(path.join(__dirname,"../../main/resources/public/cosmograph-spike.js"),"utf8");

test("native 3D frame uploads preserve z and do not modify the shared 2D graph", async () => {
    const data = new Map([["a",{x:1,y:2}],["b",{x:3,y:4}]]);
    let gpu = new Float32Array([0,0,0,1,1,1]), stopped = 0;
    const graph = { nodes: () => [...data.keys()], hasNode: id => data.has(id), getNodeAttribute: (id,name) => data.get(id)[name] };
    const renderer = {
        setPointSizes() {}, setPointPositions: (positions, options) => { assert.equal(options.dimensions,3); gpu = positions; },
        getPointPositions: () => gpu, stop: () => stopped++, render() {},
    };
    const c = vm.createContext({ graph, renderer, Float32Array, console, setTimeout, clearTimeout, setInterval, clearInterval,
        document: { addEventListener() {}, getElementById() { return null; } }, window: { dispatchEvent() {} }, CustomEvent: function() {} });
    vm.runInContext(raw.slice(raw.indexOf("\n")+1),c);
    vm.runInContext('sourceGraph=graph; instance=renderer; spaceDimensions=3; pointIds=["a","b"];',c);
    const api = c.window.relisonCosmograph;
    await api.setNodePositions({a:{x:10,y:20,z:30},b:{x:40,y:50,z:60}});
    assert.deepEqual(Array.from(gpu),[10,20,30,40,50,60]);
    assert.deepEqual(data.get("a"),{x:1,y:2});
    assert.equal(api.getSpatialNodePositions().b.z,60);
    assert.ok(stopped>0);
    await assert.rejects(api.setNodePositions({a:{x:100,y:200,z:NaN},b:{x:0,y:0,z:0}}),/incomplete/);
    assert.deepEqual(Array.from(gpu),[10,20,30,40,50,60]);
});
