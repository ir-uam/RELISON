import { Cosmograph } from "./vendor/esm/cosmograph.js";

const rgb = (hex) => {
    const value = String(hex || "#888888").replace("#", "");
    const expanded = value.length === 3 ? value.split("").map((part) => part + part).join("") : value;
    return [0, 2, 4].map((index) => parseInt(expanded.slice(index, index + 2), 16) / 255).concat(1);
};
const pairId = (source, target) => JSON.stringify([String(source), String(target)]);
const toHex = (rgba) => "#" + rgba.slice(0, 3).map((channel) => Math.round(Math.max(0, Math.min(1, channel)) * 255).toString(16).padStart(2, "0")).join("");

function findGraphApi(instance) {
    const candidates = [instance?.graph, instance?._graph, instance?.cosmos, instance?._cosmos, ...Object.values(instance || {})];
    return candidates.find((candidate) => candidate && typeof candidate.setPointColors === "function" && typeof candidate.setLinkColors === "function") || null;
}

async function create(graph, container, onPointClick, options = {}) {
    const dimensions = Number(options.dimensions) === 3 ? 3 : 2;
    const spatialPositions = options.positions || {};
    const nodes = graph.nodes();
    const nodeIndex = new Map(nodes.map((node, index) => [String(node), index]));
    const points = nodes.map((node) => {
        const attrs = graph.getNodeAttributes(node);
        const spatial = spatialPositions[String(node)] || attrs;
        return { id: String(node), index: nodeIndex.get(String(node)), label: String(attrs.label ?? node), color: attrs.color || "#8993a4",
            size: Number.isFinite(attrs.size) ? attrs.size : 4,
            x: Number.isFinite(spatial.x) ? spatial.x : undefined,
            y: Number.isFinite(spatial.y) ? spatial.y : undefined,
            z: dimensions === 3 && Number.isFinite(spatial.z) ? spatial.z : 0 };
    });
    let pointColorsByNode = new Map(points.map((point) => [point.id, point.color]));
    const baseEdges = graph.edges().map((edge) => {
        const source = String(graph.source(edge)), target = String(graph.target(edge));
        return { id: String(edge), source, target, sourceIndex: nodeIndex.get(source), targetIndex: nodeIndex.get(target),
        color: graph.getEdgeAttribute(edge, "color") || "#888888", width: Number(graph.getEdgeAttribute(edge, "size")) || 1,
        arrow: graph.type === "directed" || (graph.type === "mixed" && !graph.isUndirected(edge)), style: 0, recommendation: false };
    });
    const recModel = window.relisonDiffusionRecModel;
    const links = [...baseEdges];
    if (recModel?.edges) recModel.edges.forEach((edge, index) => {
        if (!nodeIndex.has(String(edge.source)) || !nodeIndex.has(String(edge.target))) return;
        const source = String(edge.source), target = String(edge.target);
        links.push({ id: "recommendation-" + index, source, target,
            sourceIndex: nodeIndex.get(source), targetIndex: nodeIndex.get(target),
            color: window.relisonDiffusionRecColor || "#28c76f", width: 1,
            arrow: Boolean(window.relisonDiffusionDirected), style: window.relisonDiffusionRecDashed ? 1 : 0, recommendation: true });
    });
    const labels = window.relisonCosmographLabelOpts || {};
    const config = {
        points, links,
        pointIdBy: "id", pointIndexBy: "index", pointColorBy: "color", pointSizeBy: "size", pointLabelBy: "label", pointXBy: "x", pointYBy: "y",
        ...(dimensions === 3 ? { pointZBy: "z" } : {}), spaceDimensions: dimensions,
        pointSphereShading: dimensions === 3 && Boolean(document.getElementById("cosmograph-sphere-shading")?.checked),
        pointDepthFade: dimensions === 3 ? 0.1 : 0,
        linkSourceBy: "source", linkSourceIndexBy: "sourceIndex", linkTargetBy: "target", linkTargetIndexBy: "targetIndex",
        linkColorBy: "color", linkWidthBy: "width", linkArrowBy: "arrow", linkStyleBy: "style",
        pointColorStrategy: "direct", pointSizeStrategy: "direct", linkColorStrategy: "direct", linkWidthStrategy: "direct",
        linkWidthByFn: (value) => Number(value), linkDefaultArrows: Boolean(window.relisonDiffusionDirected),
        linkDefaultStyle: 0, linkDashLength: 5, linkDashGap: 4, linkArrowsSizeScale: 1.5,
        enableSimulation: false, rescalePositions: false, fitViewOnInit: false, fitViewDelay: 0,
        transitionDuration: 0, showLabels: Boolean(labels.nodeShow), showHoveredPointLabel: true,
        pointLabelColor: labels.nodeColorMode === "single" ? (labels.nodeColor || "#ffffff") : "#ffffff",
        pointLabelClassName: (_text, index) => {
            const point = points[index];
            const color = labels.nodeColorMode === "single" ? (labels.nodeColor || "#ffffff")
                : labels.nodeColorMode === "node" ? (pointColorsByNode.get(point?.id) || "#ffffff") : "#ffffff";
            return "color: " + color + ";";
        },
        pointLabelFontSize: Number(labels.nodeSize) || 12,
        outlinedPointIndices: document.getElementById("node-border-on")?.checked ? nodes.map((_, index) => index) : [],
        outlinedPointRingColor: document.getElementById("node-border-color")?.value || "#1b1c1f",
        selectPointOnClick: "single", selectLinkOnClick: false,
        onPointClick: (index) => { const point = points[index]; if (point && onPointClick) onPointClick(point.id); },
    };
    const instance = new Cosmograph(container, config);
    instance.stop();
    try { await instance.dataUploaded(); }
    catch (error) { instance.destroy(); throw error; }
    const baseLinkColors = links.map((link) => rgb(link.color)).flat();
    const linkWidths = links.map((link) => link.width);
    const linkStyles = links.map((link) => link.style);
    const graphApi = findGraphApi(instance);
    let fittedVisibleViewport = false;
    let activeLinkColors = links.map((link) => link.color);
    let activeLinkStyles = links.map((link) => link.style);
    let pointSizes = points.map((point) => point.size);
    let pointCoordinates = Float32Array.from(points.flatMap((point) => dimensions === 3
        ? [point.x || 0, point.y || 0, point.z || 0] : [point.x || 0, point.y || 0]));
    let activeLinkWidths = links.map((link) => link.width);
    const sourceToIndex = new Map();
    links.forEach((link, index) => sourceToIndex.set(pairId(link.source, link.target), index));
    const targetToIndex = new Map();
    links.forEach((link, index) => targetToIndex.set(pairId(link.target, link.source), index));
    async function applyFrameData() {
        if (graphApi) {
            graphApi.setPointColors(Float32Array.from(points.flatMap((point) => rgb(pointColorsByNode.get(point.id)))));
            graphApi.setLinkColors(Float32Array.from(activeLinkColors.flatMap(rgb)));
            graphApi.setPointSizes?.(Float32Array.from(pointSizes));
            graphApi.setLinkWidths?.(Float32Array.from(activeLinkWidths));
            graphApi.setLinkStyles?.(Float32Array.from(activeLinkStyles));
            const positionTarget = typeof graphApi.setPointPositions === "function" ? graphApi
                : typeof instance.setPointPositions === "function" ? instance : null;
            if (positionTarget) {
                positionTarget.setPointPositions(pointCoordinates, { dimensions, dontRescale: true });
                if (typeof positionTarget.render === "function") positionTarget.render();
                else instance.render?.();
            } else graphApi.render?.();
            return;
        }

        // Cosmograph 2.5.1 exposes color/size getters on the wrapper but not the
        // underlying Graph setters. Reconfigure the same instance as a fallback,
        // carrying its current coordinates and camera forward between frames.
        const camera = instance.getCameraState?.();
        const framePoints = points.map((point, index) => ({
            ...point,
            color: pointColorsByNode.get(point.id),
            size: pointSizes[index],
            x: pointCoordinates[index * dimensions],
            y: pointCoordinates[index * dimensions + 1],
            z: dimensions === 3 ? pointCoordinates[index * dimensions + 2] : 0,
        }));
        const frameLinks = links.map((link, index) => ({
            ...link,
            color: activeLinkColors[index],
            width: activeLinkWidths[index],
            style: activeLinkStyles[index],
        }));
        await instance.setConfig({ ...config, points: framePoints, links: frameLinks });
        await instance.dataUploaded();
        instance.stop();
        if (camera) instance.setCameraState?.(camera);
    }
    const api = {
        instance,
        async setIteration(iteration, result, palette) {
            if (!result?.iterations?.[iteration]) return;
            const informed = new Set();
            for (let index = 0; index <= iteration; index++) result.iterations[index].newlyInformed.forEach((node) => informed.add(String(node)));
            const newly = new Set(result.iterations[iteration].newlyInformed.map(String));
            const propagating = new Set(result.iterations[iteration].propagating.map(String));
            const nodeColors = [];
            const nodeColorsById = new Map();
            nodes.forEach((node) => {
                let color = palette.base;
                if (informed.has(String(node))) color = palette.informed;
                if (newly.has(String(node))) color = palette.newly;
                if (propagating.has(String(node))) color = palette.prop;
                nodeColors.push(...rgb(color));
                nodeColorsById.set(String(node), color);
            });
            const colors = [...baseLinkColors];
            const styles = [...linkStyles];
            propagating.forEach((source) => {
                const neighbors = [];
                if (graph.type === "directed") graph.forEachOutNeighbor(source, (target) => neighbors.push(String(target)));
                else graph.forEachNeighbor(source, (target) => neighbors.push(String(target)));
                neighbors.forEach((target) => {
                    const linkIndex = sourceToIndex.get(pairId(source, target)) ?? targetToIndex.get(pairId(source, target));
                    if (linkIndex === undefined || links[linkIndex].recommendation) return;
                    colors.splice(linkIndex * 4, 4, ...rgb(palette.prop));
                    styles[linkIndex] = 1;
                });
            });
            pointColorsByNode = nodeColorsById;
            activeLinkColors = links.map((_link, index) => toHex(colors.slice(index * 4, index * 4 + 4)));
            activeLinkStyles = styles;
            await applyFrameData();
            await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)));
        },
        syncAppearance(updatedGraph, updatedLabels, updatedSpatialPositions = {}) {
            if (updatedLabels) Object.assign(labels, updatedLabels);
            pointSizes = points.map((point) => Number(updatedGraph.getNodeAttribute(point.id, "size")) || 4);
            pointCoordinates = new Float32Array(points.length * dimensions);
            points.forEach((point, index) => {
                const spatial = dimensions === 3 ? updatedSpatialPositions[point.id] : null;
                const x = spatial?.x ?? updatedGraph.getNodeAttribute(point.id, "x");
                const y = spatial?.y ?? updatedGraph.getNodeAttribute(point.id, "y");
                const z = dimensions === 3 ? (spatial?.z ?? point.z) : 0;
                if (Number.isFinite(x)) point.x = x;
                if (Number.isFinite(y)) point.y = y;
                if (Number.isFinite(z)) point.z = z;
                pointCoordinates[index * dimensions] = Number.isFinite(point.x) ? point.x : 0;
                pointCoordinates[index * dimensions + 1] = Number.isFinite(point.y) ? point.y : 0;
                if (dimensions === 3) pointCoordinates[index * dimensions + 2] = Number.isFinite(point.z) ? point.z : 0;
            });
            activeLinkWidths = links.map((link) => link.recommendation ? link.width :
                Number(updatedGraph.getEdgeAttribute(link.id, "size")) || 1);
            config.showLabels = Boolean(labels.nodeShow);
            config.pointLabelColor = labels.nodeColorMode === "single" ? (labels.nodeColor || "#ffffff") : "#ffffff";
            config.pointLabelFontSize = Number(labels.nodeSize) || 12;
            const applyLabelConfig = typeof instance.setConfigPartial === "function"
                ? instance.setConfigPartial({ showLabels: config.showLabels, pointLabelColor: config.pointLabelColor,
                    pointLabelFontSize: config.pointLabelFontSize, pointLabelClassName: config.pointLabelClassName })
                : Promise.resolve();
            return Promise.resolve(applyLabelConfig).then(applyFrameData)
                .catch((error) => console.warn("Diffusion Cosmograph appearance sync failed.", error));
        },
        resize() {
            if (!(container.clientWidth > 0 && container.clientHeight > 0)) return;
            // Cosmograph 2.5 has no wrapper resize(). Cosmos.render() refreshes
            // the canvas viewport; fit only once, after it has a real size.
            graphApi?.render?.(undefined, 0);
            if (!fittedVisibleViewport) {
                instance.fitView?.(0, 0.1);
                fittedVisibleViewport = true;
            }
        },
        async captureFrame(targetWidth) {
            let source = instance.getCanvas?.() || container.querySelector("canvas");
            if (!source?.width || !source?.height) return null;
            const cssWidth = source.clientWidth || container.clientWidth, cssHeight = source.clientHeight || container.clientHeight;
            const size = window.relisonAnimationExport.frameSize(cssWidth, cssHeight, targetWidth || source.width);
            const previousRatio = (await instance.getConfig?.())?.pixelRatio || source.width / cssWidth;
            const ratio = size.width / cssWidth;
            const changed = Math.abs(previousRatio - ratio) > 0.001;
            try {
                if (changed) {
                    if (!instance.setConfigPartial) throw new Error("This renderer cannot change its capture resolution.");
                    await instance.setConfigPartial({ pixelRatio: ratio });
                    instance.resize?.(); graphApi?.render?.();
                }
                await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
                source = instance.getCanvas?.() || container.querySelector("canvas");
                if (!source || source.width + 1 < size.width || source.height + 1 < size.height)
                    throw new Error("The renderer could not produce the selected capture resolution.");
                const frame = document.createElement("canvas");
                frame.width = size.width; frame.height = size.height;
                const context = frame.getContext("2d");
                context.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--canvas-bg").trim() || "#18191c";
                context.fillRect(0, 0, size.width, size.height);
                context.drawImage(source, 0, 0, size.width, size.height);
                return frame;
            } finally {
                if (changed && instance.setConfigPartial) {
                    await instance.setConfigPartial({ pixelRatio: previousRatio });
                    instance.resize?.(); graphApi?.render?.();
                }
            }
        },
        getDimensions() { return dimensions; },
        destroy() { instance.stop(); instance.destroy(); container.replaceChildren(); },
    };
    api.resize();
    return api;
}

window.relisonDiffusionCosmograph = { create };
window.dispatchEvent(new CustomEvent("relison-diffusion-cosmograph-ready"));
