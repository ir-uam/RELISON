/* Klein-ball navigation. A Lorentz boost changes focus without Euclidean translation. */
(() => {
    let state = null;
    function parents(graph, root, direction) {
        const nodes = graph.nodes().sort(), seen = new Set(), result = {};
        if (root && !graph.hasNode(root)) throw new Error("H3 root must exist in the network.");
        for (const start of root ? [root, ...nodes.filter(n => n !== root)] : nodes) {
            if (seen.has(start)) continue;
            seen.add(start); const queue = [start];
            for (let k = 0; k < queue.length; k++) {
                const node = queue[k], out = graph.outNeighbors(node), incoming = graph.inNeighbors(node);
                const adjacent = graph.type === "undirected" || direction === "UND" ? [...new Set([...out, ...incoming])]
                    : direction === "OUT" ? out : direction === "IN" ? incoming : out.filter(n => incoming.includes(n));
                for (const target of adjacent.sort()) if (!seen.has(target)) { seen.add(target); result[target] = node; queue.push(target); }
            }
        }
        return result;
    }
    function activate(graph, positions, radius, parentMap) {
        const chosen = new Map();
        graph.forEachEdge?.((id, attrs, source, target) => {
            const child = parentMap[target] === source ? target : parentMap[source] === target ? source : null;
            if (child === null) return;
            const candidate = { id: String(id), forward: parentMap[target] === source }, previous = chosen.get(child);
            if (!previous || (candidate.forward && !previous.forward) || (candidate.forward === previous.forward && candidate.id < previous.id)) chosen.set(child, candidate);
        });
        state = { graph, radius, positions: Object.fromEntries(Object.entries(positions).map(([id, p]) => [id, { ...p }])), parents: { ...parentMap },
            treeEdges: graph.forEachEdge ? new Set([...chosen.values()].map(edge => edge.id)) : null };
    }
    function recenter(positions, radius, node = null) {
        const q = node === null ? { x: 0, y: 0, z: 0 } : positions[node];
        if (!q) throw new Error("Choose a node in the H3 layout to focus.");
        const a = [q.x / radius, q.y / radius, q.z / radius], r2 = a.reduce((s, v) => s + v * v, 0);
        if (r2 >= 1 - 1e-12) throw new Error("This H3 focus is too close to the numerical boundary.");
        const gamma = 1 / Math.sqrt(1 - r2);
        return Object.fromEntries(Object.entries(positions).map(([id, p]) => {
            const v = [p.x / radius, p.y / radius, p.z / radius], dot = v.reduce((s, x, i) => s + x * a[i], 0);
            const factor = r2 === 0 ? 0 : (gamma - 1) * dot / r2 - gamma;
            const w = gamma * (1 - dot), values = v.map((x, i) => radius * (x + factor * a[i]) / w);
            if (values.some(x => !Number.isFinite(x)) || Math.hypot(...values) >= radius)
                throw new Error("H3 focus exceeds numerical precision. Restore the origin or choose a nearer node.");
            return [id, { x: values[0], y: values[1], z: values[2] }];
        }));
    }
    function edgeVisible(graph, source, target, id = null) {
        return !state || state.graph !== graph || document.getElementById("layout-h3-nontree")?.checked
            || (id !== null && state.treeEdges ? state.treeEdges.has(String(id)) : state.parents[source] === target || state.parents[target] === source);
    }
    window.relisonH3 = { parents, activate, recenter, edgeVisible,
        clear: () => { state = null; },
        active: graph => state?.graph === graph,
        focus: (graph, node) => { if (state?.graph !== graph) throw new Error("Apply H3 before changing hyperbolic focus."); return recenter(state.positions, state.radius, node); },
    };
})();
