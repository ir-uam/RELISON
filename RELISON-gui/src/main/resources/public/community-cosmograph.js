import { Cosmograph, prepareCosmographData } from "./vendor/esm/cosmograph.js";

// A separate instance: community exploration must not borrow the Network or Diffusion canvas/state.
// This adapter exposes the small viewport/event interface used by the shared community inspector and exports.
export async function create(graph, container, options = {}) {
    const signal = options.signal;
    const checkStartup = () => { if (signal?.aborted) throw signal.reason; };
    checkStartup();
    const dimensions = options.dimensions === 3 ? 3 : 2;
    const ids = graph.nodes(), indices = new Map(ids.map((id, index) => [id, index]));
    const handlers = new Map(), settings = { selectedNode: options.selected, edgeShape: options.edgeShape || "curved" };
    const host = document.createElement("div");
    host.className = "community-cosmograph-host";
    container.appendChild(host);
    let instance, destroyed = false, frame = 0, signature = "", cameraSignature = "", labelSignature = "", hoveredNode = null;
    let refreshQueue = Promise.resolve(), refreshVersion = 0, fitPending = false;
    const emit = (type, value) => { if (!destroyed) (handlers.get(type) || []).forEach(handler => handler(value)); };
    const textColor = () => settings.labelColor?.color || "#ffffff";
    const pointTextColor = point => options.labelColorByNode && point ? options.labelColorByNode(point) : textColor();

    function labelConfiguration(data) {
        const a = options.labelOptions?.() || {}, size = a.labelSize || 12;
        return {
            // Match Network: viewport-dependent labels, plus an unconditional hover label.
            showLabels: a.labelShow !== false, showTopLabels: false,
            showDynamicLabels: true, showDynamicLabelsLimit: 200, showHoveredPointLabel: true,
            pointLabelColor: textColor(), pointLabelFontSize: size,
            pointLabelClassName: (_text, index) => {
                const point = data.points[index];
                const fontSize = a.labelProp ? Math.max(6, (point?.size || 8) / 2 * size / 8) : size;
                return "--relison-community-node-index: " + index + "; font-family: " + (a.labelFont || "sans-serif")
                    + "; font-size: " + fontSize + "px; color: " + pointTextColor(point) + ";";
            },
        };
    }

    function visibleLabelNodes() {
        const visible = new Set();
        if (destroyed) return visible;
        // Read native label selection rather than approximating its zoom/overlap rules.
        for (const element of host.querySelectorAll(".css-label--label")) {
            const style = getComputedStyle(element), rect = element.getBoundingClientRect();
            if (style.display === "none" || style.visibility === "hidden" || Number(style.opacity) === 0
                || element.closest(".css-label--labels-container-hidden") || !rect.width || !rect.height) continue;
            const rawIndex = style.getPropertyValue("--relison-community-node-index").trim();
            const index = rawIndex ? Number(rawIndex) : NaN;
            if (Number.isInteger(index) && ids[index] != null) visible.add(ids[index]);
        }
        return visible;
    }

    function hover(index) {
        const node = Number.isInteger(index) ? ids[index] ?? null : null;
        if (node === hoveredNode || destroyed) return;
        const previous = hoveredNode;
        hoveredNode = node;
        if (previous != null) emit("leaveNode", { node: previous });
        if (node != null) emit("enterNode", { node });
    }

    function dispose() {
        if (destroyed) return;
        destroyed = true; cancelAnimationFrame(frame);
        // Cleanup failures must not prevent a replacement renderer from starting.
        try { instance?.stop(); } catch (error) { console.warn("Community Cosmograph stop failed", error); }
        try { instance?.destroy(); } catch (error) { console.warn("Community Cosmograph cleanup failed", error); }
        host.remove();
    }

    function waitForStartup(promise) {
        if (!signal) return promise;
        return new Promise((resolve, reject) => {
            const aborted = () => reject(signal.reason);
            signal.addEventListener("abort", aborted, { once: true });
            if (signal.aborted) aborted();
            Promise.resolve(promise).then(value => {
                signal.removeEventListener("abort", aborted);
                if (signal.aborted) reject(signal.reason);
                else resolve(value);
            }, error => { signal.removeEventListener("abort", aborted); reject(error); });
        });
    }

    function snapshot() {
        return {
            points: ids.map(id => {
                const attrs = graph.getNodeAttributes(id);
                // Cosmograph's direct size is a diameter; Sigma's size is a radius.
                return { id, label: attrs.label, color: attrs.color, size: attrs.size * 2, x: attrs.x, y: attrs.y, z: attrs.z || 0 };
            }),
            links: graph.edges().filter(id => !graph.getEdgeAttribute(id, "hidden")).map(id => ({ id, source: graph.source(id), target: graph.target(id),
                color: graph.getEdgeAttribute(id, "color"), width: graph.getEdgeAttribute(id, "size"), arrow: graph.type === "directed" })),
        };
    }

    async function configuration(data) {
        const prepared = await prepareCosmographData({
            points: { pointIdBy: "id", pointColorBy: "color", pointSizeBy: "size", pointLabelBy: "label", pointXBy: "x", pointYBy: "y", ...(dimensions === 3 ? { pointZBy: "z" } : {}) },
            links: { linkSourceBy: "source", linkTargetsBy: ["target"], linkColorBy: "color", linkWidthBy: "width", linkArrowBy: "arrow" },
        }, data.points, data.links);
        if (!prepared) throw new Error("Could not prepare community graph data.");
        return {
            points: prepared.points, links: prepared.links, ...prepared.cosmographConfig,
            spaceDimensions: dimensions, pointSphereShading: dimensions === 3, pointDepthFade: dimensions === 3 ? 0.1 : 0,
            pointColorStrategy: "direct", pointSizeStrategy: "direct", linkColorStrategy: "direct", linkWidthStrategy: "direct",
            linkWidthByFn: value => Number(value), linkWidthScale: 1, linkDefaultArrows: graph.type === "directed", linkArrowsSizeScale: 1.5,
            curvedLinks: settings.edgeShape !== "straight", curvedLinkSegments: 19, curvedLinkWeight: 0.8, curvedLinkControlPointDistance: 0.25,
            enableSimulation: false, enableDrag: false, rescalePositions: false, fitViewOnInit: false, fitViewDelay: 0, transitionDuration: 0,
            ...labelConfiguration(data),
            outlinedPointIndices: indices.has(settings.selectedNode) ? [indices.get(settings.selectedNode)] : [],
            outlinedPointRingColor: getComputedStyle(document.body).getPropertyValue("--accent").trim(),
            selectPointOnClick: false, selectLinkOnClick: false, focusPointOnClick: false, focusPointOnLabelClick: false,
            onPointClick: index => { if (ids[index] != null) emit("clickNode", { node: ids[index] }); },
            onPointMouseOver: hover,
            onPointMouseOut: () => hover(null),
            onLinkClick: index => { if (data.links[index]) emit("clickEdge", { edge: data.links[index].id }); },
            onZoom: () => emit("afterRender"), onResize: () => emit("afterRender"), onDrag: () => emit("afterRender"),
        };
    }

    function monitor() {
        if (destroyed) return;
        const camera = JSON.stringify(instance?.getCameraState?.());
        const labels = JSON.stringify([...visibleLabelNodes()]);
        if (camera !== cameraSignature || labels !== labelSignature) {
            cameraSignature = camera; labelSignature = labels; emit("afterRender");
        }
        frame = requestAnimationFrame(monitor);
    }

    signal?.addEventListener("abort", dispose, { once: true });
    try {
        const data = snapshot();
        signature = JSON.stringify(data);
        const config = await waitForStartup(configuration(data));
        checkStartup();
        instance = new Cosmograph(host, config);
        await waitForStartup(instance.dataUploaded());
        checkStartup(); instance.stop();
        if (options.camera) instance.setCameraState?.(options.camera);
        else instance.fitView?.(0, 0.12);
        frame = requestAnimationFrame(monitor);
    } catch (error) { dispose(); throw error; }
    finally { signal?.removeEventListener("abort", dispose); }

    async function sync(version) {
        if (destroyed || version !== refreshVersion) return;
        const data = snapshot(), next = JSON.stringify(data);
        if (next !== signature) {
            const config = await configuration(data), camera = instance.getCameraState?.();
            if (destroyed || version !== refreshVersion) return;
            await instance.setConfig(config); await instance.dataUploaded();
            if (destroyed) return;
            instance.stop(); if (camera && !fitPending) instance.setCameraState?.(camera);
            signature = next;
        } else {
            await instance.setConfigPartial?.({ curvedLinks: settings.edgeShape !== "straight", ...labelConfiguration(data),
                outlinedPointIndices: indices.has(settings.selectedNode) ? [indices.get(settings.selectedNode)] : [] });
        }
        if (!destroyed) {
            instance.resize?.();
            // Fit only after the latest upload. A later reconfiguration must not restore an obsolete camera.
            if (version === refreshVersion && fitPending) { fitPending = false; instance.fitView?.(0, 0.12); }
            emit("afterRender");
        }
    }

    function refresh({ fit = false } = {}) {
        fitPending ||= fit;
        const version = ++refreshVersion;
        refreshQueue = refreshQueue.catch(() => {}).then(() => sync(version));
        return refreshQueue.catch(error => { if (!destroyed) console.error("Community Cosmograph refresh failed", error); });
    }

    function zoom({ factor = 1.25 } = {}) {
        if (!Number.isFinite(factor) || factor <= 0) return Promise.resolve(false);
        refreshQueue = refreshQueue.catch(() => {}).then(() => {
            if (destroyed) return false;
            if (dimensions === 3) {
                const distance = instance.getCameraState?.()?.distance;
                if (!Number.isFinite(distance)) return false;
                instance.setCameraState?.({ distance: Math.max(0.01, Math.min(1e9, distance / factor)) }, 150);
            } else {
                const level = instance.getZoomLevel?.();
                if (!Number.isFinite(level)) return false;
                instance.setZoomLevel?.(Math.max(0.05, Math.min(100, level * factor)), 150);
            }
            emit("afterRender"); return true;
        });
        return refreshQueue;
    }

    return {
        on(type, handler) { if (!handlers.has(type)) handlers.set(type, []); handlers.get(type).push(handler); },
        refresh,
        getVisibleLabelNodes: visibleLabelNodes,
        getHoveredNode: () => destroyed ? null : hoveredNode,
        setSetting(name, value) { settings[name] = value; },
        getCamera() { return { animatedReset: () => refresh({ fit: true }), animatedZoom: zoom, getState: () => instance.getCameraState?.() }; },
        graphToViewport(attrs) {
            const point = instance.spaceToScreenPosition([attrs.x, attrs.y, ...(dimensions === 3 ? [attrs.z || 0] : [])], { dimensions });
            return { x: point?.[0] ?? 0, y: point?.[1] ?? 0 };
        },
        getNodeDisplayData(id) {
            const diameter = instance.getPointScreenRadiusByIndex?.(indices.get(id));
            return { size: Number.isFinite(diameter) ? diameter / 2 : graph.getNodeAttribute(id, "size") };
        },
        kill: dispose,
    };
}
