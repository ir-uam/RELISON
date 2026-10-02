import { Cosmograph, prepareCosmographData } from "https://esm.sh/@cosmograph/cosmograph@2.5.1?bundle";

let instance = null;
let pointLabelSizes = [];
let graphHasDirectedLinks = false;
let forceLayoutRunning = false;
let forceFitTimer = null;
let sourceGraph = null;
let pointIds = [];
let renderedPoints = new Map();
let renderedLinks = new Map();
let timelineSync = Promise.resolve();
let linkStyleColumnRevision = 0;

function projectGraph(graph) {
    // RELISON supplies timeline visibility as sets of shared graph ids. Keeping
    // this filter at projection time updates only Cosmograph's active snapshot;
    // it never changes the underlying Graphology graph.
    const temporal = window.relisonCosmographTemporal || {};
    const focus = window.relisonCosmographFocus;
    // Cosmograph only reprocesses link styles when the configured column name
    // changes. A fresh internal name ensures a data update (such as showing a
    // recommendation) rebuilds the native style buffer as well.
    const styleColumn = "__relison_link_style_" + (++linkStyleColumnRevision);
    const isFocused = (id) => !focus || !focus.nodes || focus.nodes.has(String(id));
    const isVisibleNode = (id) => (!temporal.nodes || temporal.nodes.has(String(id))) && (!focus?.only || isFocused(id));
    const isVisibleEdge = (id) => !temporal.edges || temporal.edges.has(String(id));
    const points = graph.nodes().filter(isVisibleNode).map((id) => {
        const attrs = graph.getNodeAttributes(id);
        const dimmed = Boolean(focus && !focus.only && !isFocused(id));
        return {
            id: String(id),
            label: dimmed ? "" : (attrs.label == null ? String(id) : String(attrs.label)),
            color: dimmed ? (focus.dimColor || "#3a3c41") : (attrs.color || "#4f9dff"),
            size: dimmed ? Math.max(1, (Number.isFinite(attrs.size) ? attrs.size : 4) * 0.6) : (Number.isFinite(attrs.size) ? attrs.size : 4),
            x: Number.isFinite(attrs.x) ? attrs.x : undefined,
            y: Number.isFinite(attrs.y) ? attrs.y : undefined,
        };
    });
    pointLabelSizes = points.map((point) => point.size);
    pointIds = points.map((point) => point.id);

    const links = graph.edges().filter((edge) => isVisibleEdge(edge) && isVisibleNode(graph.source(edge)) && isVisibleNode(graph.target(edge)) && (!focus?.only || !focus?.matchingEdges || focus.matchingEdges.has(String(edge)))).map((edge) => {
        const directed = graph.type === "directed" || (graph.type === "mixed" && !graph.isUndirected(edge));
        const attrs = graph.getEdgeAttributes(edge);
        const source = graph.source(edge), target = graph.target(edge);
        const dimmed = Boolean(focus && !focus.only && (focus.matchingEdges ? !focus.matchingEdges.has(String(edge)) : (focus.edgeOnly ? String(edge) !== focus.edge : (!isFocused(source) || !isFocused(target)))))
        return {
            id: String(edge),
            source: String(source),
            target: String(target),
            color: dimmed ? (focus.dimColor || "#3a3c41") : (attrs.color || "#888888"),
            arrow: directed,
            width: Number.isFinite(attrs.size) ? attrs.size : 1,
            // Base Graphology links are always continuous. Only the separate
            // transient recommendation projection may opt into dashes.
            style: 0,
            [styleColumn]: 0,
            // applyLabels() keeps this in sync with RELISON's edge-label attribute.
            label: dimmed ? "" : (attrs.label == null ? "" : String(attrs.label)),
        };
    });
    // Recommendation results are intentionally not written into Graphology:
    // they remain a transient RELISON result until the user accepts them. Add
    // them to Cosmograph's projection instead, with stable synthetic ids, so
    // they can use the same native link pipeline as regular graph links.
    const recommendation = window.relisonCosmographRecommendation;
    if (recommendation?.show) {
        const directed = Boolean(recommendation.directed);
        // A recommended link has no source-edge metric from which to derive a
        // per-edge width. Use the current base-link width: this is exact for
        // uniform sizing, and the mean is the least surprising counterpart
        // when base links are scaled by a metric or attribute.
        const baseWidth = links.length
            ? links.reduce((sum, link) => sum + (Number.isFinite(link.width) ? link.width : 1), 0) / links.length
            : 1;
        recommendation.edges.forEach((edge, index) => {
            const source = String(edge.source), target = String(edge.target);
            if (!graph.hasNode(edge.source) || !graph.hasNode(edge.target) || !isVisibleNode(edge.source) || !isVisibleNode(edge.target)) return;
            const dimmed = Boolean(focus && !focus.only && (!isFocused(edge.source) || !isFocused(edge.target)));
            links.push({
                id: "__relison_recommendation__" + index,
                source,
                target,
                color: dimmed ? (focus.dimColor || "#3a3c41") : recommendation.color,
                width: baseWidth,
                style: recommendation.diff ? 1 : 0,
                [styleColumn]: recommendation.diff ? 1 : 0,
                arrow: directed,
                label: "",
                recommendation: true,
            });
        });
    }
    graphHasDirectedLinks = links.some((link) => link.arrow);
    return { points, links, styleColumn };
}

const EDGE_LABEL_LIMIT = 100;
let edgeLabelTimer = null;
let edgeLabelRefreshInFlight = null;

// Cosmograph has native point labels but no native link labels. Its annotation
// layer is graph-positioned, so it follows normal pan/zoom without a separate
// screen-space overlay. Limit persistent labels to keep dense graphs readable.
function edgeLabelAnnotations(snapshot, livePositions) {
    const labels = window.relisonCosmographLabelOpts || {};
    if (!labels.edgeShow) return [];
    const points = new Map(snapshot.points.map((point, index) => {
        const x = livePositions && Number.isFinite(livePositions[index * 2]) ? livePositions[index * 2] : point.x;
        const y = livePositions && Number.isFinite(livePositions[index * 2 + 1]) ? livePositions[index * 2 + 1] : point.y;
        return [point.id, { x, y }];
    }));
    const rawFont = String(labels.edgeFont || "sans-serif");
    const font = /^[a-zA-Z0-9 _,-]+$/.test(rawFont) ? rawFont : "sans-serif";
    const color = labels.edgeColor || "#ffffff";
    return [...snapshot.links]
        .filter((link) => link.label)
        .sort((a, b) => (b.width || 0) - (a.width || 0))
        .slice(0, EDGE_LABEL_LIMIT)
        .map((link) => {
            const source = points.get(link.source), target = points.get(link.target);
            if (!source || !target || !Number.isFinite(source.x) || !Number.isFinite(source.y) ||
                !Number.isFinite(target.x) || !Number.isFinite(target.y)) return null;
            const dx = target.x - source.x, dy = target.y - source.y;
            const distance = Math.hypot(dx, dy);
            // A modest perpendicular offset keeps text from covering the link
            // while remaining stable in graph coordinate space.
            const offset = distance ? Math.min(4, distance * 0.08) : 0;
            const fontSize = labels.edgeProp ? Math.max(5, (link.width || 1) * ((Number(labels.edgeSize) || 9) / 2)) : (Number(labels.edgeSize) || 9);
            return {
                text: link.label,
                position: [
                    (source.x + target.x) / 2 - (dy / (distance || 1)) * offset,
                    (source.y + target.y) / 2 + (dx / (distance || 1)) * offset,
                ],
                color,
                fontSize,
                className: "font-family: " + font + "; pointer-events: none; white-space: nowrap;",
                padding: { left: 1, top: 1, right: 1, bottom: 1 },
                weight: 1,
            };
        })
        .filter(Boolean);
}

function currentSnapshot() {
    return { points: [...renderedPoints.values()], links: [...renderedLinks.values()] };
}

async function refreshEdgeLabels() {
    if (!instance) return;
    if (edgeLabelRefreshInFlight) return edgeLabelRefreshInFlight;
    edgeLabelRefreshInFlight = (async () => {
        const snapshot = currentSnapshot();
        let positions = null;
        if (typeof instance.getPointPositions === "function") {
            const candidate = instance.getPointPositions({ dimensions: 2 });
            if (candidate && candidate.length >= snapshot.points.length * 2) positions = candidate;
        }
        const annotations = edgeLabelAnnotations(snapshot, positions);
        if (typeof instance.setConfigPartial === "function") {
            await instance.setConfigPartial({ annotations });
        } else if (typeof instance.getConfig === "function" && typeof instance.setConfig === "function") {
            // Older builds lack setConfigPartial; retain the full active config
            // before adding annotations so other Cosmograph settings are not reset.
            const config = await instance.getConfig();
            await instance.setConfig({ ...config, annotations });
        }
    })();
    try { await edgeLabelRefreshInFlight; }
    finally { edgeLabelRefreshInFlight = null; }
}

function startEdgeLabelTracking() {
    if (edgeLabelTimer || !(window.relisonCosmographLabelOpts || {}).edgeShow) return;
    edgeLabelTimer = setInterval(() => { refreshEdgeLabels().catch((error) => console.warn("Cosmograph edge-label refresh failed.", error)); }, 125);
}

function stopEdgeLabelTracking() {
    if (edgeLabelTimer) clearInterval(edgeLabelTimer);
    edgeLabelTimer = null;
}
function rememberSnapshot(snapshot) {
    renderedPoints = new Map(snapshot.points.map((point) => [point.id, point]));
    renderedLinks = new Map(snapshot.links.map((link) => [link.id, link]));
    pointIds = snapshot.points.map((point) => point.id);
    pointLabelSizes = snapshot.points.map((point) => point.size);
}

// The data-preparation layer accepts linkStyleBy, but Cosmograph 2.5.1 can
// drop that optional column during a config update. Set the renderer's native
// style buffer explicitly after each upload: 0 = solid, 1 = dashed, 2 = dotted.
function applyLinkStyles(snapshot) {
    if (!instance || typeof instance.setLinkStyles !== "function") return;
    instance.setLinkStyles(Float32Array.from(snapshot.links, (link) => Number.isInteger(link.style) && link.style >= 0 && link.style <= 2 ? link.style : 0));
    // The public setter queues a GPU update. Render immediately so a style
    // change is visible even while the force simulation is paused.
    if (typeof instance.render === "function") instance.render();
}

async function prepareGraph(graph) {
    sourceGraph = graph;
    const snapshot = projectGraph(graph);
    const prepared = await prepareCosmographData(
        {
            points: { pointIdBy: "id", pointColorBy: "color", pointSizeBy: "size", pointLabelBy: "label", pointXBy: "x", pointYBy: "y" },
            links: { linkSourceBy: "source", linkTargetsBy: ["target"], linkColorBy: "color", linkWidthBy: "width", linkStyleBy: snapshot.styleColumn, linkArrowBy: "arrow" },
        },
        snapshot.points,
        snapshot.links,
    );
    return prepared ? { prepared, snapshot } : null;
}
function cosmographConfig(prepared, snapshot, onPointClick, onLinkClick, onStageClick) {
    const labels = window.relisonCosmographLabelOpts || {};
    // Cosmograph exposes point outlines as a ring for selected point indices,
    // rather than Sigma's general node stroke. Apply that ring to every active
    // point when RELISON's shared Node borders option is enabled.
    const borderOn = Boolean(document.getElementById("node-border-on")?.checked);
    const borderColor = document.getElementById("node-border-color")?.value || "#1b1c1f";
    const outlinedPointIndices = borderOn ? pointIds.map((_, index) => index) : [];
    const rawFont = String(labels.nodeFont || "sans-serif");
    const font = /^[a-zA-Z0-9 _,-]+$/.test(rawFont) ? rawFont : "sans-serif";
    const baseSize = Number(labels.nodeSize) || 13;
    const labelColor = labels.nodeColor || "#ffffff";
    const labelStyle = (_text, index) => {
        const size = labels.nodeProp ? Math.max(6, (pointLabelSizes[index] || 4) * (baseSize / 8)) : baseSize;
        // Inline colour wins over Cosmograph's theme CSS, keeping the shared control reliable.
        return "font-family: " + font + "; font-size: " + size + "px; color: " + labelColor + ";";
    };
    const annotations = edgeLabelAnnotations(snapshot);
    return {
        points: prepared.points,
        links: prepared.links,
        ...prepared.cosmographConfig,
        pointColorStrategy: "direct",
        outlinedPointIndices,
        outlinedPointRingColor: borderColor,
        // RELISON already maps its Size by / min / max controls to pixel sizes.
        pointSizeStrategy: "direct",
        linkColorStrategy: "direct",
        linkWidthStrategy: "direct",
        linkDefaultStyle: "solid",
        linkDashLength: 6,
        linkDashGap: 4,
        // RELISON supplies pixel widths. The accessor prevents Cosmograph's
        // automatic width-range remapping from changing those values.
        linkWidthByFn: (width) => Number(width),
        linkDefaultWidth: 1,
        linkWidthScale: 1,
        linkDefaultArrows: graphHasDirectedLinks,
        // Keep the simulation available for the explicit force-layout control.
        // render() stops it immediately unless that layout is running.
        enableSimulation: true,
        rescalePositions: false,
        fitViewOnInit: true,
        fitViewDelay: 0,
        fitViewDuration: 0,
        transitionDuration: 0,
        linkArrowsSizeScale: 1.5,
        showLabels: Boolean(labels.nodeShow),
        // Hover labels remain available even when persistent labels are hidden.
        showHoveredPointLabel: true,
        showDynamicLabels: false,
        // White is Cosmograph's neutral default; the shared label-colour control overrides it.
        pointLabelColor: labelColor,
        pointLabelFontSize: baseSize,
        pointLabelClassName: labelStyle,
        annotations,
        selectPointOnClick: "single",
        selectLinkOnClick: "single",
        onPointClick: (index, _position, event) => {
            const point = snapshot.points[index];
            if (point && onPointClick) onPointClick(point.id, event);
        },
        onLinkClick: (index, event) => {
            const link = snapshot.links[index];
            if (link && onLinkClick) onLinkClick(link.id, event);
        },
        onClick: (index, _position, event) => {
            if ((index == null || index < 0) && onStageClick) onStageClick(event);
        },
    };
}


async function render(graph, { onPointClick, onLinkClick, onStageClick } = {}) {
    const container = document.getElementById("cosmograph-container");
    if (!container || !graph) return;
    const preparedResult = await prepareGraph(graph);
    if (!preparedResult) throw new Error("Cosmograph could not prepare this graph.");
    const { prepared, snapshot } = preparedResult;
    const config = cosmographConfig(prepared, snapshot, onPointClick, onLinkClick, onStageClick);

    if (instance && typeof instance.setConfig === "function") {
        await instance.setConfig(config);
        // Stop before waiting for upload so no simulation frame is presented
        // during a normal renderer change.
        if (!forceLayoutRunning) instance.stop();
        await instance.dataUploaded();
        applyLinkStyles(snapshot);
        rememberSnapshot(snapshot);
        if (forceLayoutRunning) startEdgeLabelTracking(); else stopEdgeLabelTracking();
        return;
    }
    container.replaceChildren();
    instance = new Cosmograph(container, config);
    // Do this synchronously: dataUploaded() can resolve after the first
    // render frame, which is too late to prevent the visible layout jump.
    if (!forceLayoutRunning) instance.stop();
    await instance.dataUploaded();
    applyLinkStyles(snapshot);
    rememberSnapshot(snapshot);
    if (forceLayoutRunning) startEdgeLabelTracking(); else stopEdgeLabelTracking();
}
function hasParallelLinks(links) {
    const pairs = new Set();
    for (const link of links) {
        const key = link.source + "\u0000" + link.target;
        if (pairs.has(key)) return true;
        pairs.add(key);
    }
    return false;
}

async function applyTimelineDelta(graph) {
    sourceGraph = graph;
    if (!instance || graph.multi) return render(graph);
    const snapshot = projectGraph(graph);
    const desiredPoints = new Map(snapshot.points.map((point) => [point.id, point]));
    const desiredLinks = new Map(snapshot.links.map((link) => [link.id, link]));
    const currentLinks = [...renderedLinks.values()];
    // Cosmograph removes links by endpoint pair. Fall back to setConfig for
    // multigraph/parallel-link cases, where an endpoint pair is ambiguous.
    if (hasParallelLinks(snapshot.links) || hasParallelLinks(currentLinks) ||
        typeof instance.addPoints !== "function" || typeof instance.addLinks !== "function" ||
        typeof instance.removePointsByIds !== "function" || typeof instance.removeLinksByPointIdPairs !== "function") {
        return render(graph);
    }

    const pointsToRemove = [...renderedPoints.keys()].filter((id) => !desiredPoints.has(id));
    // Remove every link that is no longer in the temporal snapshot before
    // removing points. This avoids dangling / half-rendered links when an
    // endpoint becomes inactive.
    const linksToRemove = currentLinks.filter((link) => !desiredLinks.has(link.id));
    const pointsToAdd = snapshot.points.filter((point) => !renderedPoints.has(point.id));
    const linksToAdd = snapshot.links.filter((link) => !renderedLinks.has(link.id));

    if (linksToRemove.length) await instance.removeLinksByPointIdPairs(linksToRemove.map((link) => [link.source, link.target]));
    if (pointsToRemove.length) await instance.removePointsByIds(pointsToRemove);
    if (pointsToAdd.length) await instance.addPoints(pointsToAdd);
    if (linksToAdd.length) await instance.addLinks(linksToAdd);
    if (!forceLayoutRunning && typeof instance.stop === "function") instance.stop();
    if (typeof instance.dataUploaded === "function") await instance.dataUploaded();
    applyLinkStyles(snapshot);
    rememberSnapshot(snapshot);
    await refreshEdgeLabels();
}

function syncTimeline(graph) {
    // Timeline events may arrive faster than the data API completes. Serialize
    // them so an older snapshot cannot overwrite the newest selected time.
    // If a browser/runtime does not support a delta operation, rebuild just
    // that snapshot instead of leaving a partially-updated graph on screen.
    timelineSync = timelineSync.catch(() => {}).then(() => applyTimelineDelta(graph))
        .catch((error) => {
            console.warn("Cosmograph timeline delta update failed; rebuilding snapshot.", error);
            return render(graph);
        });
    return timelineSync;
}
function destroy() {
    if (forceFitTimer) { clearTimeout(forceFitTimer); forceFitTimer = null; }
    stopEdgeLabelTracking();
    if (instance && typeof instance.stop === "function") instance.stop();
    if (instance && typeof instance.destroy === "function") instance.destroy();
    instance = null;
    forceLayoutRunning = false;
    const container = document.getElementById("cosmograph-container");
    if (container) container.replaceChildren();
}

function startForceLayout() {
    if (!instance || typeof instance.start !== "function") return false;
    forceLayoutRunning = true;
    instance.start();
    startEdgeLabelTracking();
    // Let the simulation establish its initial structure before fitting. Fitting
    // on the first frame makes a moving, unconverged graph look abruptly tiny.
    if (forceFitTimer) clearTimeout(forceFitTimer);
    forceFitTimer = setTimeout(() => {
        forceFitTimer = null;
        if (forceLayoutRunning && instance && typeof instance.fitView === "function") instance.fitView(250, 0.1);
    }, 1200);
    return true;
}
function zoomBy(factor) {
    if (!instance || typeof instance.getZoomLevel !== "function" || typeof instance.setZoomLevel !== "function") return false;
    const current = instance.getZoomLevel();
    if (!Number.isFinite(current)) return false;
    instance.setZoomLevel(Math.max(0.05, Math.min(100, current * factor)), 150);
    return true;
}

function fitView() {
    if (!instance || typeof instance.fitView !== "function") return false;
    instance.fitView(150, 0.1);
    return true;
}
function stopForceLayout() {
    if (forceFitTimer) { clearTimeout(forceFitTimer); forceFitTimer = null; }
    if (instance && typeof instance.stop === "function") instance.stop();
    forceLayoutRunning = false;
    stopEdgeLabelTracking();
    refreshEdgeLabels().catch((error) => console.warn("Cosmograph edge-label refresh failed.", error));
    return savePointPositions();
}

// Copies the current Cosmograph layout into the shared Graphology graph without
// changing whether Cosmograph's force simulation is running. This keeps data
// exports (notably GEXF viz:position values) aligned with the visible view.
function savePointPositions() {
    if (!instance || !sourceGraph || typeof instance.getPointPositions !== "function") return 0;
    const positions = instance.getPointPositions({ dimensions: 2 });
    if (!positions || positions.length < pointIds.length * 2) return 0;
    let saved = 0;
    pointIds.forEach((id, index) => {
        const x = positions[index * 2], y = positions[index * 2 + 1];
        if (sourceGraph.hasNode(id) && Number.isFinite(x) && Number.isFinite(y)) {
            sourceGraph.setNodeAttribute(id, "x", x);
            sourceGraph.setNodeAttribute(id, "y", y);
            saved += 1;
        }
    });
    return saved;
}

async function exportPng(filename = "network.png") {
    const scene = await buildSvgScene();
    if (!scene) return false;
    const url = URL.createObjectURL(new Blob([scene.svg], { type: "image/svg+xml" }));
    try {
        const image = new Image();
        await new Promise((resolve, reject) => {
            image.onload = resolve;
            image.onerror = () => reject(new Error("Could not rasterize the Cosmograph export."));
            image.src = url;
        });
        const output = document.createElement("canvas");
        // Rasterize vector primitives at export resolution, including text and
        // label boxes, rather than enlarging pixels from the displayed canvas.
        output.width = Math.ceil(scene.width * 4);
        output.height = Math.ceil(scene.height * 4);
        output.getContext("2d").drawImage(image, 0, 0, output.width, output.height);
        const blob = await new Promise((resolve) => output.toBlob(resolve, "image/png"));
        if (!blob) throw new Error("Could not encode the Cosmograph PNG export.");
        const downloadUrl = URL.createObjectURL(blob);
        const anchor = document.createElement("a");
        anchor.href = downloadUrl;
        anchor.download = filename;
        anchor.click();
        setTimeout(() => URL.revokeObjectURL(downloadUrl), 1000);
    } finally {
        URL.revokeObjectURL(url);
    }
    return true;
}

function xmlEscape(value) {
    return String(value).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/\"/g, "&quot;");
}

function downloadSvg(filename, svg) {
    const url = URL.createObjectURL(new Blob([svg], { type: "image/svg+xml" }));
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function renderedRgba(colorBuffer, index, fallback) {
    const offset = index * 4;
    if (!colorBuffer || colorBuffer.length < offset + 4 || !Number.isFinite(colorBuffer[offset])) return fallback;
    const r = Math.round(Math.max(0, Math.min(1, colorBuffer[offset])) * 255);
    const g = Math.round(Math.max(0, Math.min(1, colorBuffer[offset + 1])) * 255);
    const b = Math.round(Math.max(0, Math.min(1, colorBuffer[offset + 2])) * 255);
    const a = Math.max(0, Math.min(1, colorBuffer[offset + 3]));
    return "rgba(" + r + ", " + g + ", " + b + ", " + a + ")";
}

// Reconstruct the active Cosmograph scene as vector primitives. This is a
// genuine SVG export (not a conversion of the WebGL canvas), so it stays crisp
// when enlarged and includes the current camera position, projected links and
// visible node/edge labels.
async function buildSvgScene() {
    if (!instance || typeof instance.getCanvas !== "function" || typeof instance.spaceToScreenPosition !== "function") return false;
    const canvas = instance.getCanvas();
    const config = typeof instance.getConfig === "function" ? await instance.getConfig() : {};
    const snapshot = currentSnapshot();
    const positions = typeof instance.getPointPositions === "function" ? instance.getPointPositions({ dimensions: 2 }) : null;
    if (!canvas || !positions || positions.length < snapshot.points.length * 2) return false;
    // Cosmograph's spaceToScreenPosition() returns CSS-pixel coordinates.
    // Its canvas width/height are device pixels, so use the displayed size for
    // the SVG viewport to keep vector positions and labels aligned.
    const width = canvas.clientWidth || canvas.width, height = canvas.clientHeight || canvas.height;
    if (!width || !height) return false;
    // Read resolved buffers from Cosmograph itself. The app projection remains
    // useful for ids and labels, but these are the exact visual values that
    // Cosmograph sent to its renderer after any internal mapping.
    const pointColors = typeof instance.getPointColors === "function" ? instance.getPointColors() : null;
    const linkColors = typeof instance.getLinkColors === "function" ? instance.getLinkColors() : null;
    const linkWidths = typeof instance.getLinkWidths === "function" ? instance.getLinkWidths() : null;

    const pointScreen = new Map();
    snapshot.points.forEach((point, index) => {
        const position = instance.spaceToScreenPosition([positions[index * 2], positions[index * 2 + 1]]);
        if (!position || !Number.isFinite(position[0]) || !Number.isFinite(position[1])) return;
        const rawRadius = typeof instance.getPointRadiusByIndex === "function" ? instance.getPointRadiusByIndex(index) : point.size;
        const scaledRadius = rawRadius * (config.pointSizeScale ?? 1);
        const radius = typeof instance.spaceToScreenRadius === "function" ? instance.spaceToScreenRadius(scaledRadius) : scaledRadius;
        pointScreen.set(point.id, { x: position[0], y: position[1], radius: Number.isFinite(radius) ? radius : point.size,
            color: renderedRgba(pointColors, index, point.color || "#4f9dff"), point, index });
    });

    const defs = '<filter id="label-shadow" x="-50%" y="-50%" width="200%" height="200%"><feDropShadow dx="0" dy="1" stdDeviation="2" flood-opacity="0.5"/></filter>';
    const background = getComputedStyle(document.body).getPropertyValue("--canvas-bg").trim() || "#18191c";
    const linkMarkup = snapshot.links.map((link, index) => {
        const source = pointScreen.get(link.source), target = pointScreen.get(link.target);
        if (!source || !target) return "";
        const color = renderedRgba(linkColors, index, link.color || "#888888");
        const linkWidth = (Number.isFinite(linkWidths?.[index]) ? linkWidths[index] : (link.width || 1)) * (config.linkWidthScale ?? 1);
        const dx = target.x - source.x, dy = target.y - source.y, distance = Math.hypot(dx, dy) || 1;
        // Cosmograph's shader places the triangular arrow around t=0.5.
        const arrowWidth = linkWidth * 2 * (config.linkArrowsSizeScale ?? 1);
        const arrowLength = Math.min(distance * 0.3, 0.866 * arrowWidth * 2);
        const midX = (source.x + target.x) / 2, midY = (source.y + target.y) / 2;
        const ux = dx / distance, uy = dy / distance;
        const tipX = midX + ux * arrowLength / 2, tipY = midY + uy * arrowLength / 2;
        const baseX = midX - ux * arrowLength / 2, baseY = midY - uy * arrowLength / 2;
        const arrow = link.arrow ? '<polygon points="' + tipX + ',' + tipY + ' '
            + (baseX - uy * arrowWidth / 2) + ',' + (baseY + ux * arrowWidth / 2) + ' '
            + (baseX + uy * arrowWidth / 2) + ',' + (baseY - ux * arrowWidth / 2)
            + '" fill="' + xmlEscape(color) + '"/>' : '';
        const dash = link.style === 1 || link.style === "dashed" ? ' stroke-dasharray="6 4"' : (link.style === 2 || link.style === "dotted" ? ' stroke-dasharray="1 4" stroke-linecap="round"' : "");
        return '<line x1="' + source.x + '" y1="' + source.y + '" x2="' + target.x + '" y2="' + target.y
            + '" stroke="' + xmlEscape(color) + '" stroke-width="' + linkWidth + '"' + dash + '/>' + arrow;
    }).join("");
    const nodes = [...pointScreen.values()].map(({ x, y, radius, color }) =>
        '<circle cx="' + x + '" cy="' + y + '" r="' + Math.max(0, radius) + '" fill="' + xmlEscape(color) + '"/>').join("");
    // Cosmograph uses DOM labels for nodes and annotations. Export those
    // visible boxes at their actual screen positions, preserving padding,
    // font weight, colour and rounded background instead of inventing labels.
    const canvasRect = canvas.getBoundingClientRect();
    const labelMarkup = [...document.querySelectorAll('#cosmograph-container .css-label--label')].map((element) => {
        const style = getComputedStyle(element);
        const rect = element.getBoundingClientRect();
        if (style.display === 'none' || style.visibility === 'hidden' || Number(style.opacity) === 0 ||
            element.closest('.css-label--labels-container-hidden') || !rect.width || !rect.height) return '';
        const x = rect.left - canvasRect.left, y = rect.top - canvasRect.top;
        const fontSize = parseFloat(style.fontSize) || 13;
        const range = document.createRange();
        range.selectNodeContents(element);
        const textRect = range.getBoundingClientRect();
        const textX = textRect.width ? textRect.left - canvasRect.left : x + (parseFloat(style.paddingLeft) || 0);
        const textY = textRect.height ? textRect.top - canvasRect.top + (textRect.height - fontSize) / 2 : y + (parseFloat(style.paddingTop) || 0);
        const shadow = style.boxShadow !== 'none' ? ' filter="url(#label-shadow)"' : '';
        return '<g opacity="' + style.opacity + '" style="filter:' + xmlEscape(style.filter) + '">'
            + '<rect x="' + x + '" y="' + y + '" width="' + rect.width + '" height="' + rect.height
            + '" rx="' + (parseFloat(style.borderRadius) || 0) + '" fill="' + xmlEscape(style.backgroundColor) + '"' + shadow + '/>'
            + '<text x="' + textX + '" y="' + textY + '" dominant-baseline="text-before-edge" fill="' + xmlEscape(style.color)
            + '" font-family="' + xmlEscape(style.fontFamily) + '" font-size="' + fontSize + '" font-weight="' + xmlEscape(style.fontWeight)
            + '">' + xmlEscape(element.textContent || '') + '</text></g>';
    }).join('');
    const svg = '<?xml version="1.0" encoding="UTF-8"?>\n<svg xmlns="http://www.w3.org/2000/svg" width="' + width + '" height="' + height
        + '" viewBox="0 0 ' + width + ' ' + height + '"><defs>' + defs + '</defs><rect width="100%" height="100%" fill="' + xmlEscape(background) + '"/>'
        + linkMarkup + nodes + labelMarkup + '</svg>';
    return { svg, width, height };
}

async function exportSvg(filename = "network.svg") {
    const scene = await buildSvgScene();
    if (!scene) return false;
    downloadSvg(filename, scene.svg);
    return true;
}
async function setFocusedSelection(nodeId, edgeId) {
    if (!instance || typeof instance.setConfigPartial !== "function") return;
    const pointIndex = nodeId == null ? undefined : pointIds.indexOf(String(nodeId));
    const linkIndex = edgeId == null ? undefined : [...renderedLinks.keys()].indexOf(String(edgeId));
    await instance.setConfigPartial({
        focusedPointIndex: pointIndex >= 0 ? pointIndex : undefined,
        focusedLinkIndex: linkIndex >= 0 ? linkIndex : undefined,
    });
}
window.relisonCosmograph = { render, syncTimeline, destroy, isRendered: () => instance !== null, startForceLayout, stopForceLayout, savePointPositions, exportPng, exportSvg, zoomIn: () => zoomBy(1.25), zoomOut: () => zoomBy(0.8), fitView, setFocusedSelection };
window.dispatchEvent(new CustomEvent("relison-cosmograph-ready"));
