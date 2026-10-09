/* Equal Earth: one spherical projection and camera for background and network. */
(() => {
    function create(containerId = "geographic-container") {
    const mercator = window.createRelisonMercator(containerId);
    const events = ["nodeAdded", "nodeDropped", "edgeAdded", "edgeDropped", "cleared", "nodeAttributesUpdated",
        "edgeAttributesUpdated", "eachNodeAttributesUpdated", "eachEdgeAttributesUpdated"];
    let loading, world, svg, scene, network, projection, path, zoom, graph, options, observer, frame = null;
    let active = "mercator", width = 1, height = 1;
    function script(url, global) {
        if (window[global]) return Promise.resolve();
        return new Promise((resolve, reject) => {
            const tag = document.createElement("script"); tag.src = url;
            tag.onload = () => window[global] ? resolve() : reject(new Error("Map library did not initialize."));
            tag.onerror = () => { tag.remove(); reject(new Error("Could not load Equal Earth map libraries. Check your connection.")); };
            document.head.appendChild(tag);
        });
    }
    function load() {
        if (world) return Promise.resolve();
        if (!loading) loading = Promise.all([
            script("https://cdn.jsdelivr.net/npm/d3@7.9.0/dist/d3.min.js", "d3"),
            script("https://cdn.jsdelivr.net/npm/topojson-client@3.1.0/dist/topojson-client.min.js", "topojson"),
            fetch("https://cdn.jsdelivr.net/npm/world-atlas@2.0.2/countries-110m.json").then(r => {
                if (!r.ok) throw new Error("Could not load the Equal Earth background."); return r.json();
            }),
        ]).then(([, , data]) => { world = data; }).catch(e => { loading = null; throw e; });
        return loading;
    }
    function sizes() {
        if (!svg) return;
        const k = window.d3.zoomTransform(svg.node()).k;
        network.selectAll("circle").attr("r", d => d.radius / k);
        network.selectAll("text").attr("font-size", 12 / k).attr("dy", -10 / k);
    }
    function refresh() {
        if (!svg) return;
        const coordinates = options.coordinates(), visible = new Map();
        graph.forEachNode((id, attrs) => {
            const display = options.nodeDisplay(id, attrs), p = coordinates.points.get(id);
            if (!display.hidden) visible.set(id, { id, display, coordinate: [p[1], p[0]], radius: Math.max(2, Number(display.size) || 4) });
        });
        network.selectAll("*").remove();
        graph.forEachEdge((id, attrs, source, target, sa, ta, undirected) => {
            const a = visible.get(source), b = visible.get(target), display = options.edgeDisplay(id, attrs);
            if (!a || !b || display.hidden) return;
            const edge = source === target
                ? network.append("circle").datum({ radius: a.radius + 7 }).attr("cx", projection(a.coordinate)[0]).attr("cy", projection(a.coordinate)[1])
                : network.append("path").attr("d", path({ type: "LineString", coordinates: [a.coordinate, b.coordinate] }));
            edge.attr("fill", "none").attr("stroke", display.color || "#64748b").attr("stroke-width", Math.max(.5, Number(display.size) || 1))
                .attr("vector-effect", "non-scaling-stroke").attr("stroke-opacity", .7).style("cursor", "pointer")
                .on("click", event => { event.stopPropagation(); options.onEdgeClick(id); });
            edge.append("title").text(display.label || source + (undirected ? " — " : " → ") + target);
        });
        for (const edge of options.overlays?.() || []) {
            const a = visible.get(edge.source), b = visible.get(edge.target);
            if (!a || !b) continue;
            network.append("path").attr("d", path({ type: "LineString", coordinates: [a.coordinate, b.coordinate] }))
                .attr("fill", "none").attr("stroke", edge.color || "#f59e0b").attr("stroke-width", edge.size || 1.5)
                .attr("stroke-dasharray", edge.dashed ? "6 4" : null).attr("vector-effect", "non-scaling-stroke")
                .append("title").text(edge.label || edge.source + " → " + edge.target);
        }
        for (const node of visible.values()) {
            const p = projection(node.coordinate), selected = options.selectedNode() === node.id;
            const marker = network.append("circle").datum(node).attr("cx", p[0]).attr("cy", p[1])
                .attr("fill", node.display.color || "#2563eb").attr("stroke", selected ? "#f59e0b" : "white")
                .attr("stroke-width", selected ? 3 : 1).attr("vector-effect", "non-scaling-stroke").style("cursor", "pointer")
                .on("click", event => { event.stopPropagation(); options.onNodeClick(node.id); });
            marker.append("title").text(node.display.label || node.id);
            if (options.labels().nodeShow) network.append("text").attr("x", p[0]).attr("y", p[1]).attr("text-anchor", "middle")
                .attr("fill", "#0f172a").attr("paint-order", "stroke").attr("stroke", "white").attr("stroke-width", 3)
                .attr("vector-effect", "non-scaling-stroke").style("pointer-events", "none").text(node.display.label || node.id);
        }
        sizes();
    }
    function schedule() {
        if (!svg || frame !== null) return;
        frame = requestAnimationFrame(() => { frame = null; try { refresh(); } catch (e) { options?.onError(e.message); } });
    }
    function fit() {
        if (!svg) return;
        const positions = [...options.coordinates().points.values()].map(p => projection([p[1], p[0]]));
        let transform = window.d3.zoomIdentity;
        if (positions.length) {
            const x0 = window.d3.min(positions, p => p[0]), x1 = window.d3.max(positions, p => p[0]);
            const y0 = window.d3.min(positions, p => p[1]), y1 = window.d3.max(positions, p => p[1]);
            const k = Math.max(.2, Math.min(256, (width - 70) / Math.max(1, x1 - x0), (height - 70) / Math.max(1, y1 - y0)));
            transform = transform.translate(width / 2, height / 2).scale(k).translate(-(x0 + x1) / 2, -(y0 + y1) / 2);
        }
        svg.call(zoom.transform, transform);
    }
    function resize() {
        const container = document.getElementById(containerId);
        width = Math.max(1, container.clientWidth); height = Math.max(1, container.clientHeight);
        svg.attr("width", width).attr("height", height);
        zoom.extent([[0, 0], [width, height]]);
        projection.fitExtent([[15, 15], [Math.max(16, width - 15), Math.max(16, height - 15)]], { type: "Sphere" });
        scene.selectAll(".earth-background").attr("d", path);
        refresh();
    }
    function destroyEarth() {
        const hadEarth = Boolean(svg);
        if (graph) for (const event of events) graph.off(event, schedule);
        if (frame !== null) cancelAnimationFrame(frame); frame = null;
        observer?.disconnect(); observer = null;
        svg?.remove(); svg = null; graph = null; options = null;
        if (hadEarth) {
            const container = document.getElementById(containerId);
            container.replaceChildren(); container.hidden = true;
        }
    }
    async function renderEarth(input, settings) {
        await load();
        if (settings.isCurrent && !settings.isCurrent()) return;
        settings.coordinates(); destroyEarth(); mercator.destroy();
        graph = input; options = settings;
        const d3 = window.d3, container = document.getElementById(containerId); container.hidden = false;
        projection = d3.geoEqualEarth().rotate([-settings.coordinates().center, 0]); path = d3.geoPath(projection);
        svg = d3.select(container).append("svg").style("display", "block").style("background", "#e2e8f0");
        scene = svg.append("g");
        scene.append("path").datum({ type: "Sphere" }).attr("class", "earth-background").attr("fill", "#dbeafe");
        scene.append("path").datum(window.topojson.feature(world, world.objects.land)).attr("class", "earth-background")
            .attr("fill", "#f1f5e9").attr("stroke", "#94a3b8").attr("stroke-width", .6).attr("vector-effect", "non-scaling-stroke");
        scene.append("path").datum(window.topojson.mesh(world, world.objects.countries, (a, b) => a !== b))
            .attr("class", "earth-background").attr("fill", "none").attr("stroke", "#94a3b8").attr("stroke-width", .5).attr("vector-effect", "non-scaling-stroke");
        scene.append("path").datum(d3.geoGraticule10()).attr("class", "earth-background").attr("fill", "none")
            .attr("stroke", "#94a3b8").attr("stroke-opacity", .3).attr("stroke-width", .5).attr("vector-effect", "non-scaling-stroke");
        network = scene.append("g");
        zoom = d3.zoom().scaleExtent([.2, 256]).on("zoom", event => { scene.attr("transform", event.transform); sizes(); });
        svg.call(zoom).on("click", () => options?.onClearSelection());
        const credit = document.createElement("a"); credit.href = "https://www.naturalearthdata.com/"; credit.textContent = "Natural Earth · Equal Earth";
        credit.className = "equal-earth-credit"; credit.target = "_blank"; credit.rel = "noopener"; container.appendChild(credit);
        resize(); fit(); observer = new ResizeObserver(resize); observer.observe(container);
        for (const event of events) graph.on(event, schedule);
    }
    function buildExportSvg(includeLegend = true) {
        if (!svg) return null;
        refresh();
        const E = window.relisonGeographicExport, snapshot = svg.node().cloneNode(true);
        snapshot.setAttribute("xmlns", "http://www.w3.org/2000/svg");
        snapshot.setAttribute("viewBox", `0 0 ${width} ${height}`);
        snapshot.insertBefore(E.element("rect", { width, height, fill: "#e2e8f0" }), snapshot.firstChild);
        snapshot.querySelectorAll("text").forEach(node => node.setAttribute("font-family", "sans-serif"));
        if (includeLegend) E.legend(snapshot, width, height, options.legend?.());
        E.credit(snapshot, width, height, "Natural Earth · Equal Earth · naturalearthdata.com");
        return snapshot;
    }
    return {
        readCoordinates: mercator.readCoordinates,
        ensureLoaded: mode => mode === "equal-earth" ? load() : mercator.ensureLoaded(),
        render: async (input, settings) => {
            if (settings.projection === "equal-earth") { await renderEarth(input, settings); active = "equal-earth"; }
            else { destroyEarth(); await mercator.render(input, settings); active = "mercator"; }
        },
        refresh: () => active === "equal-earth" ? schedule() : mercator.refresh(),
        destroy: () => { destroyEarth(); mercator.destroy(); active = "mercator"; },
        fitView: () => active === "equal-earth" ? fit() : mercator.fitView(),
        exportPng: (filename = "network.png", settings = {}) => active === "equal-earth"
            ? window.relisonGeographicExport.exportImage(() => buildExportSvg(settings.includeLegend !== false), "png", filename) : mercator.exportPng(filename, settings),
        exportSvg: (filename = "network.svg", settings = {}) => active === "equal-earth"
            ? window.relisonGeographicExport.exportImage(() => buildExportSvg(settings.includeLegend !== false), "svg", filename) : mercator.exportSvg(filename, settings),
        zoomIn: () => active === "equal-earth" ? svg?.call(zoom.scaleBy, 2) : mercator.zoomIn(),
        zoomOut: () => active === "equal-earth" ? svg?.call(zoom.scaleBy, .5) : mercator.zoomOut(),
    };
    }
    window.createRelisonGeographic = create;
    window.relisonGeographic = create();
})();
