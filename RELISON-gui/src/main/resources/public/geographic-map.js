/* Geographic graph view. Leaflet owns both the tile and network camera. */
(() => {
    function create(containerId = "geographic-container") {
    const MAX_LATITUDE = 85.0511287798066;
    let library = null, map = null, layers = null, graph = null, options = null;
    let resizeObserver = null, refreshFrame = null;
    let ready = false;
    const graphEvents = ["nodeAdded", "nodeDropped", "edgeAdded", "edgeDropped", "cleared",
        "nodeAttributesUpdated", "edgeAttributesUpdated", "eachNodeAttributesUpdated", "eachEdgeAttributesUpdated"];

    function ensureLoaded() {
        if (ready && window.L) return Promise.resolve();
        if (library) return library;
        library = Promise.all([
            new Promise((resolve, reject) => {
                const css = document.createElement("link");
                css.rel = "stylesheet";
                css.href = "https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css";
                css.onload = resolve;
                css.onerror = () => { css.remove(); reject(new Error("Could not load the map stylesheet. Check your connection.")); };
                document.head.appendChild(css);
            }),
            new Promise((resolve, reject) => {
                if (window.L) { resolve(); return; }
                const script = document.createElement("script");
                script.src = "https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js";
                script.onload = resolve;
                script.onerror = () => { script.remove(); reject(new Error("Could not load the map library. Check your connection.")); };
                document.head.appendChild(script);
            }),
        ]).then(() => { if (!window.L) throw new Error("The map library did not initialize."); ready = true; })
          .catch(error => { library = null; throw error; });
        return library;
    }

    // Choose the smallest longitudinal arc containing the nodes, including date-line networks.
    function readCoordinates(nodes, latitude, longitude, projection = "mercator") {
        const limit = projection === "equal-earth" ? 90 : MAX_LATITUDE;
        const raw = nodes.map(id => {
            const lat = latitude(id), lon = longitude(id);
            if (!Number.isFinite(lat) || Math.abs(lat) > limit)
                throw new Error("Node " + id + ": latitude must be within ±" + limit + "° for this map.");
            if (!Number.isFinite(lon) || lon < -180 || lon > 180)
                throw new Error("Node " + id + ": longitude must be between −180° and 180°.");
            return { id, lat, lon };
        });
        const sorted = raw.map(p => (p.lon + 360) % 360).sort((a, b) => a - b);
        let start = 0, largestGap = -1;
        for (let i = 0; i < sorted.length; i++) {
            const next = i + 1 < sorted.length ? sorted[i + 1] : sorted[0] + 360;
            if (next - sorted[i] > largestGap) { largestGap = next - sorted[i]; start = next % 360; }
        }
        const center = sorted.length ? ((start + (360 - largestGap) / 2 + 180) % 360 + 360) % 360 - 180 : 0;
        const points = new Map(raw.map(p => [p.id, [p.lat, center + ((p.lon - center + 180) % 360 + 360) % 360 - 180]]));
        return { center, points, latitudes: Object.fromEntries(raw.map(p => [p.id, p.lat])),
            longitudes: Object.fromEntries(raw.map(p => [p.id, p.lon])) };
    }
    function text(content) { const element = document.createElement("span"); element.textContent = String(content); return element; }
    function refresh() {
        if (!map || !graph || !options) return;
        const coordinates = options.coordinates(); // Validate before replacing the visible network.
        layers.clearLayers();
        const visible = new Map();
        graph.forEachNode((id, attrs) => {
            const display = options.nodeDisplay(id, attrs);
            if (!display.hidden) visible.set(id, { position: coordinates.points.get(id), display });
        });
        graph.forEachEdge((id, attrs, source, target, sourceAttrs, targetAttrs, undirected) => {
            const a = visible.get(source), b = visible.get(target), display = options.edgeDisplay(id, attrs);
            if (!a || !b || display.hidden) return;
            const style = {
                color: display.color || "#64748b", weight: Math.max(.5, Number(display.size) || 1), opacity: .7,
            };
            const line = (source === target
                ? window.L.circleMarker(a.position, { ...style, radius: Math.max(2, Number(a.display.size) || 4) + 7, fill: false })
                : window.L.polyline([a.position, b.position], style)).addTo(layers);
            line.bindTooltip(text(display.label || (source + (undirected ? " — " : " → ") + target)));
            line.on("click", event => { window.L.DomEvent.stopPropagation(event); options.onEdgeClick(id); });
        });
        for (const edge of options.overlays?.() || []) {
            const a = visible.get(edge.source), b = visible.get(edge.target);
            if (!a || !b) continue;
            const line = window.L.polyline([a.position, b.position], { color: edge.color || "#f59e0b", weight: edge.size || 1.5,
                dashArray: edge.dashed ? "6 4" : undefined, opacity: .85 }).addTo(layers);
            line.bindTooltip(text(edge.label || (edge.source + " → " + edge.target)));
        }
        for (const [id, { position, display }] of visible) {
            const marker = window.L.circleMarker(position, {
                radius: Math.max(2, Number(display.size) || 4), fillColor: display.color || "#2563eb",
                color: options.selectedNode() === id ? "#f59e0b" : "#ffffff", weight: options.selectedNode() === id ? 3 : 1,
                fillOpacity: 1,
            }).addTo(layers);
            marker.bindTooltip(text(display.label || id), { permanent: Boolean(options.labels().nodeShow), direction: "top", className: "geographic-node-label" });
            marker.on("click", event => { window.L.DomEvent.stopPropagation(event); options.onNodeClick(id); });
        }
    }
    function scheduleRefresh() {
        if (refreshFrame !== null || !map) return;
        refreshFrame = requestAnimationFrame(() => {
            refreshFrame = null;
            try { refresh(); } catch (error) { options?.onError(error.message); }
        });
    }
    function detachGraph() {
        if (graph) for (const event of graphEvents) graph.off(event, scheduleRefresh);
        if (refreshFrame !== null) cancelAnimationFrame(refreshFrame);
        refreshFrame = null;
    }
    function fitView() {
        if (!map || !options) return;
        const coordinates = [...options.coordinates().points.values()];
        map.invalidateSize();
        if (coordinates.length) map.fitBounds(window.L.latLngBounds(coordinates), { padding: [35, 35], maxZoom: 12, animate: false });
        else map.setView([0, 0], 2);
    }
    async function render(input, settings) {
        await ensureLoaded();
        if (settings.isCurrent && !settings.isCurrent()) return;
        settings.coordinates();
        detachGraph(); graph = input; options = settings;
        const container = document.getElementById(containerId);
        container.hidden = false;
        if (!map) {
            // Native paths and tiles use the same EPSG:3857 projection and map transforms.
            map = window.L.map(container, { preferCanvas: true, zoomAnimation: false, worldCopyJump: false }).setView([0, 0], 2);
            let tileErrorShown = false;
            window.L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
                maxZoom: 19, crossOrigin: "anonymous", attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
            }).on("tileerror", () => {
                if (!tileErrorShown) { tileErrorShown = true; options?.onError("Background map tiles could not load. Check your internet connection; geographic nodes remain available."); }
            }).addTo(map);
            layers = window.L.layerGroup().addTo(map);
            map.on("click", () => options?.onClearSelection());
            resizeObserver = new ResizeObserver(() => map?.invalidateSize({ pan: false }));
            resizeObserver.observe(container);
        }
        fitView(); refresh();
        for (const event of graphEvents) graph.on(event, scheduleRefresh);
    }
    function destroy() {
        const hadMap = Boolean(map);
        detachGraph(); resizeObserver?.disconnect(); resizeObserver = null;
        map?.remove(); map = null; layers = null; graph = null; options = null;
        if (hadMap) document.getElementById(containerId).hidden = true;
    }
    async function buildExportSvg(includeLegend = true) {
        if (!map || !graph || !options) return null;
        const container = document.getElementById(containerId), rect = container.getBoundingClientRect();
        const width = Math.round(rect.width), height = Math.round(rect.height);
        if (!width || !height) throw new Error("Show the map before exporting it.");
        const exportGraph = graph, exportMap = map;
        const exportCenter = map.getCenter(), exportZoom = map.getZoom();
        const tiles = [...container.querySelectorAll("img.leaflet-tile")].filter(tile => {
            const r = tile.getBoundingClientRect(); return r.right > rect.left && r.left < rect.right && r.bottom > rect.top && r.top < rect.bottom;
        });
        if (!tiles.length) throw new Error("Wait for the background map to load before exporting.");
        await Promise.all(tiles.map(tile => tile.complete ? Promise.resolve() : new Promise((resolve, reject) => {
            const finish = error => { clearTimeout(timer); tile.removeEventListener("load", loaded); tile.removeEventListener("error", failed); error ? reject(error) : resolve(); };
            const loaded = () => finish(), failed = () => finish(new Error("A background tile could not load. Try exporting again when the map has loaded."));
            const timer = setTimeout(failed, 5000); tile.addEventListener("load", loaded, { once: true }); tile.addEventListener("error", failed, { once: true });
        })));
        if (graph !== exportGraph || map !== exportMap) throw new Error("The map changed while preparing export. Try again.");
        const currentCenter = map.getCenter(), currentRect = container.getBoundingClientRect();
        if (map.getZoom() !== exportZoom || currentCenter.lat !== exportCenter.lat || currentCenter.lng !== exportCenter.lng
            || currentRect.width !== rect.width || currentRect.height !== rect.height)
            throw new Error("The map moved or resized while preparing export. Try again.");
        const E = window.relisonGeographicExport, svg = E.root(width, height);
        for (const tile of tiles) {
            if (!tile.naturalWidth) throw new Error("A background tile is unavailable. Try again when the map has loaded.");
            const canvas = document.createElement("canvas"); canvas.width = tile.naturalWidth; canvas.height = tile.naturalHeight;
            canvas.getContext("2d").drawImage(tile, 0, 0);
            let data;
            try { data = canvas.toDataURL("image/png"); } catch (_) { throw new Error("The map tile provider blocked image export (CORS). Reload the page and try again."); }
            const r = tile.getBoundingClientRect();
            svg.appendChild(E.element("image", { x: r.left - rect.left, y: r.top - rect.top, width: r.width, height: r.height, href: data }));
        }
        const coordinates = options.coordinates(), visible = new Map();
        graph.forEachNode((id, attrs) => {
            const display = options.nodeDisplay(id, attrs);
            if (!display.hidden) visible.set(id, { p: map.latLngToContainerPoint(coordinates.points.get(id)), display });
        });
        function edge(source, target, display, dashed = false) {
            const a = visible.get(source), b = visible.get(target); if (!a || !b || display.hidden) return;
            const style = { fill: "none", stroke: display.color || "#64748b", "stroke-width": Math.max(.5, Number(display.size) || 1.5), "stroke-opacity": .7, "stroke-dasharray": dashed ? "6 4" : undefined };
            svg.appendChild(source === target ? E.element("circle", { ...style, cx: a.p.x, cy: a.p.y, r: Math.max(2, Number(a.display.size) || 4) + 7 })
                : E.element("line", { ...style, x1: a.p.x, y1: a.p.y, x2: b.p.x, y2: b.p.y }));
        }
        graph.forEachEdge((id, attrs, source, target) => edge(source, target, options.edgeDisplay(id, attrs)));
        for (const overlay of options.overlays?.() || []) edge(overlay.source, overlay.target, overlay, overlay.dashed);
        for (const [id, { p, display }] of visible) {
            const radius = Math.max(2, Number(display.size) || 4), selected = options.selectedNode() === id;
            svg.appendChild(E.element("circle", { cx: p.x, cy: p.y, r: radius, fill: display.color || "#2563eb", stroke: selected ? "#f59e0b" : "white", "stroke-width": selected ? 3 : 1 }));
            if (options.labels().nodeShow) svg.appendChild(E.element("text", { x: p.x, y: p.y - radius - 6, "text-anchor": "middle", "font-family": "sans-serif", "font-size": 12, fill: "#0f172a", stroke: "white", "stroke-width": 3, "paint-order": "stroke" }, display.label || id));
        }
        if (includeLegend) E.legend(svg, width, height, options.legend?.());
        E.credit(svg, width, height, "© OpenStreetMap contributors · openstreetmap.org/copyright");
        return svg;
    }
    return { ensureLoaded, readCoordinates, render, refresh: scheduleRefresh, destroy, fitView,
        exportPng: (filename = "network.png", settings = {}) => window.relisonGeographicExport.exportImage(() => buildExportSvg(settings.includeLegend !== false), "png", filename),
        exportSvg: (filename = "network.svg", settings = {}) => window.relisonGeographicExport.exportImage(() => buildExportSvg(settings.includeLegend !== false), "svg", filename),
        zoomIn: () => map?.zoomIn(), zoomOut: () => map?.zoomOut() };
    }
    window.createRelisonMercator = create;
    window.relisonGeographic = create();
})();
