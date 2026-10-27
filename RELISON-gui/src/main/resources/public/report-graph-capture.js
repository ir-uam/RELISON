"use strict";

// Keeps the destination report/cell stable while the user adjusts the network view.
const ReportGraphCapture = (() => {
    function create({ bar, captureButton, cancelButton, message, getGraph, getViewKey,
        isNetworkActive, openNetwork, openReport, captureView, complete }) {
        let pending = null;
        const guidance = target => `Snapshot for “${target.reportTitle || "Untitled report"}”. Adjust the network view, then capture it.`;
        const noGraph = "Load a network first, then return to Network to capture the snapshot.";
        function sync() {
            bar.hidden = !pending;
            const graph = getGraph();
            captureButton.disabled = !pending || pending.busy || !graph;
            captureButton.textContent = pending?.busy ? "Capturing…" : "Capture for report";
            if (pending && !graph) message.textContent = noGraph;
            else if (pending && message.textContent === noGraph) message.textContent = guidance(pending.target);
        }
        function resume() {
            if (!pending) return false;
            const target = pending.target;
            pending = null;
            sync();
            openReport(target);
            return true;
        }
        function start(target) {
            pending = { target, busy: false };
            message.textContent = guidance(target);
            sync();
            openNetwork();
            cancelButton.focus();
        }
        async function capture() {
            const request = pending, graph = getGraph(), view = getViewKey();
            if (!request || request.busy || !graph || !isNetworkActive()) return;
            request.busy = true;
            message.textContent = "Capturing the current network view…";
            sync();
            let timer;
            try {
                const snapshot = await Promise.race([captureView(), new Promise((_, reject) => {
                    timer = setTimeout(() => reject(new Error("The renderer took too long to capture. Try again once it is ready.")), 15000);
                })]);
                // A cancelled or replaced request must never populate another cell.
                if (pending !== request) return;
                if (graph !== getGraph() || view !== getViewKey() || !isNetworkActive()) {
                    throw new Error("The network view changed during capture. Return to Network and try again.");
                }
                if (!snapshot || !complete(request.target, snapshot)) throw new Error("The snapshot could not be added to the report.");
                resume();
            } catch (error) {
                if (pending === request) message.textContent = `Capture failed: ${error.message} You can retry or return to the report.`;
            } finally {
                clearTimeout(timer);
                request.busy = false;
                sync();
            }
        }
        captureButton.addEventListener("click", capture);
        cancelButton.addEventListener("click", resume);
        sync();
        return { start, resume, sync };
    }
    function sigmaFrame({ renderer, container, overlay, drawOverlay, background }) {
        return new Promise((resolve, reject) => {
            if (!renderer) { reject(new Error("The network renderer is not ready.")); return; }
            const detach = () => {
                clearTimeout(timer);
                if (typeof renderer.off === "function") renderer.off("afterRender", capture);
                else renderer.removeListener?.("afterRender", capture);
            };
            const capture = () => {
                detach();
                try {
                    const canvases = [...container.querySelectorAll("canvas")];
                    const { width, height } = canvases[0] || {};
                    if (!(width > 0 && height > 0)) throw new Error("Show the network before capturing it.");
                    const canvas = document.createElement("canvas");
                    canvas.width = width; canvas.height = height;
                    const context = canvas.getContext("2d");
                    context.fillStyle = background;
                    context.fillRect(0, 0, width, height);
                    canvases.forEach(layer => { if (layer.width && layer.height) context.drawImage(layer, 0, 0, width, height); });
                    drawOverlay?.();
                    if (overlay?.width && overlay.height) context.drawImage(overlay, 0, 0, width, height);
                    resolve(canvas);
                } catch (error) { reject(error); }
            };
            // A disposed renderer or a lost WebGL context may never emit another frame.
            const timer = setTimeout(() => { detach(); reject(new Error("The network did not render. Reload its renderer and try again.")); }, 10000);
            try { renderer.on("afterRender", capture); renderer.refresh(); }
            catch (error) { detach(); reject(error); }
        });
    }
    return { create, sigmaFrame };
})();
