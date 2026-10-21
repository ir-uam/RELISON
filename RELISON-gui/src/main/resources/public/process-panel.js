"use strict";

// The server owns job lifetimes. This view also survives tab changes and reloads.
const ProcessPanel = (() => {
    const routes = new Set(["/api/layout", "/api/metrics/vertex", "/api/metrics/graph", "/api/metrics/pair", "/api/metrics/edge-attributes",
        "/api/communities", "/api/communities/attribute-bins", "/api/communities/global", "/api/communities/individual",
        "/api/edge-partitions", "/api/edge-partitions/global", "/api/edge-partitions/individual",
        "/api/recommendation/run", "/api/recommendation/evaluate", "/api/diffusion/run"]);
    let jobs = [], serverJobs = [], received = 0, polling = false, pending = 0, timer = null;
    const localJobs = new Map();
    const active = job => !job.endedAt;
    const duration = ms => {
        const seconds = Math.floor(Math.max(0, ms) / 1000);
        return seconds < 60 ? `${seconds}s` : seconds < 3600
            ? `${Math.floor(seconds / 60)}m ${seconds % 60}s`
            : `${Math.floor(seconds / 3600)}h ${Math.floor(seconds / 60) % 60}m`;
    };
    function progressText(job) {
        if (job.unit && job.total >= 0) return `${job.completed.toLocaleString()} / ${job.total.toLocaleString()} ${job.unit} (${job.total ? Math.min(100, 100 * job.completed / job.total).toFixed(1) : "100"}%)`;
        if (job.unit) return `${job.completed.toLocaleString()} ${job.unit} completed`;
        return "Progress unavailable for this phase";
    }
    function element(tag, text, className) {
        const node = document.createElement(tag);
        if (text != null) node.textContent = text;
        if (className) node.className = className;
        return node;
    }
    function stateBadge(state) {
        const icons = { queued: "schedule", running: "sync", cancelling: "cancel", cancelled: "cancel",
            completed: "check_circle", failed: "error" };
        const badge = element("span", null, `process-state state-${state}`);
        badge.dataset.state = state;
        badge.setAttribute("aria-label", `Status: ${state}`);
        const icon = element("span", icons[state] || "help", "material-symbols-outlined");
        icon.setAttribute("aria-hidden", "true");
        badge.append(icon, element("span", state));
        return badge;
    }
    function processType(job) {
        const kind = String(job.kind || "").toLowerCase();
        if (kind === "layout") return "Layout";
        if (kind === "recommendation") return job.parameters?.mode === "prediction" ? "Prediction" : "Recommendation";
        if (kind === "evaluation") return "Evaluation";
        if (kind === "diffusion") return "Diffusion";
        if (kind === "community" || kind === "edgepartition") return "Method";
        if (["vertex", "graph", "pair", "edgeattribute", "commglobal", "commindividual",
            "edgepartitionglobal", "edgepartitionindividual"].includes(kind)) return "Metric";
        return kind ? kind.charAt(0).toUpperCase() + kind.slice(1) : "Process";
    }
    function processTypeBadge(job) {
        const type = processType(job), badge = element("span", null, `process-type type-${type.toLowerCase()}`);
        badge.setAttribute("aria-label", `Type: ${type}`);
        badge.textContent = type;
        return badge;
    }
    function updateFilterOptions(select, values, allLabel, format = value => value) {
        const selected = select.value || "";
        const options = [element("option", allLabel)]; options[0].value = "";
        [...new Set(values)].forEach(value => {
            const option = element("option", format(value)); option.value = value; options.push(option);
        });
        select.replaceChildren(...options);
        select.value = values.includes(selected) ? selected : "";
    }
    function localSnapshot(job) {
        const now = performance.now();
        return { id: job.id, graphId: job.graphId, kind: "layout", label: job.label, parameters: {}, state: job.state,
            phase: job.phase, completed: job.completed, total: job.total, unit: job.unit,
            progresses: job.progresses, createdAt: job.createdAt, startedAt: job.startedAt, endedAt: job.endedAt,
            elapsedMillis: job.endedAt ? job.elapsedMillis : Math.max(0, now - job.startedPerf), queueMillis: 0,
            cancellable: !job.endedAt && job.state === "running", error: job.error, local: true };
    }
    function mergeJobs() {
        jobs = serverJobs.concat([...localJobs.values()].map(localSnapshot))
            .sort((a, b) => Number(b.createdAt) - Number(a.createdAt));
        received = performance.now();
        render();
    }
    function trimLocalJobs() {
        const completed = [...localJobs.values()].filter(job => job.endedAt).sort((a, b) => a.endedAt - b.endedAt);
        while (completed.length > 100) localJobs.delete(completed.shift().id);
    }
    function render() {
        const button = document.getElementById("processes-button"), list = document.getElementById("processes-list");
        if (!button || !list) return;
        const compact = document.getElementById("processes-panel").classList.contains("processes-compact");
        const showAllGraphs = document.getElementById("processes-all-graphs").checked;
        const graphId = state.graphId;
        const visibleJobs = jobs.filter(job => showAllGraphs || job.graphId === graphId);
        const typeFilter = document.getElementById("process-filter-type");
        const statusFilter = document.getElementById("process-filter-status");
        updateFilterOptions(typeFilter, visibleJobs.map(processType), "All types");
        const statusOrder = ["queued", "running", "cancelling", "cancelled", "completed", "failed"];
        const statuses = [...new Set([...statusOrder, ...visibleJobs.map(job => job.state)])]
            .sort((a, b) => (statusOrder.indexOf(a) < 0 ? 99 : statusOrder.indexOf(a)) - (statusOrder.indexOf(b) < 0 ? 99 : statusOrder.indexOf(b)));
        updateFilterOptions(statusFilter, statuses, "All statuses", value => value.charAt(0).toUpperCase() + value.slice(1));
        const filteredJobs = visibleJobs.filter(job => (!typeFilter.value || processType(job) === typeFilter.value)
            && (!statusFilter.value || job.state === statusFilter.value));
        const count = filteredJobs.filter(active).length;
        document.getElementById("processes-count").textContent = String(count);
        button.setAttribute("aria-label", `Processes, ${count} active`);
        const now = performance.now();
        const focusedCancel = document.activeElement?.dataset?.processCancel;
        list.replaceChildren(...filteredJobs.map(job => {
            const card = element("article", null, "process-card"); card.dataset.jobId = job.id;
            const heading = element("div", null, "process-heading");
            const badges = element("div", null, "process-badges");
            badges.append(processTypeBadge(job), stateBadge(job.state));
            heading.append(element("strong", job.label), badges);
            const details = Object.entries(job.parameters || {}).filter(([key]) => !["metric", "algorithm", "mode"].includes(key))
                .map(([key, value]) => `${key}: ${typeof value === "object" ? JSON.stringify(value) : value}`).join(" · ");
            card.append(heading, element("div", `${job.kind} · Graph ${job.graphId}`, "process-context"));
            if (details) card.append(element("div", details, "process-context process-parameters"));
            card.append(element("div", job.phase, "process-phase"));
            let progresses = Array.isArray(job.progresses) ? job.progresses : [];
            if (!progresses.length && job.state === "completed" && job.total < 0) {
                progresses = [{ phase: "Completed", completed: 1, total: 1, unit: "", done: true }];
            } else if (!progresses.length && (job.total >= 0 || active(job))) {
                progresses = [{ phase: job.phase, completed: job.completed, total: job.total, unit: job.unit, done: false }];
            }
            const visibleProgresses = compact ? progresses.slice(-1) : progresses;
            visibleProgresses.forEach(progress => {
                const row = element("div", null, "process-progress-stage");
                if (!compact) row.append(element("div", progress.phase, "process-progress-label"));
                const meter = element("progress");
                meter.max = Math.max(1, progress.total);
                if (progress.done || (job.state === "completed" && progress.total < 0)) meter.value = meter.max;
                else if (progress.total >= 0) meter.value = progress.total === 0 ? 1 : Math.min(progress.completed, progress.total);
                const amount = progress.unit && progress.total >= 0
                    ? `${progress.completed.toLocaleString()} / ${progress.total.toLocaleString()} ${progress.unit}`
                    : progress.unit ? `${progress.completed.toLocaleString()} ${progress.unit} completed`
                    : progress.total >= 0 ? `${Math.min(100, 100 * progress.completed / Math.max(1, progress.total)).toFixed(1)}%`
                    : progress.done ? "Completed" : "Progress unavailable";
                meter.setAttribute("aria-label", `${job.label}, ${progress.phase}: ${amount}`);
                if (progress.total < 0 && !progress.done && job.state !== "completed") meter.removeAttribute("value");
                row.append(meter);
                if (!compact) row.append(element("div", amount, "process-progress-amount"));
                card.append(row);
            });
            card.append(element("div", progressText(job), "process-count"));
            const footer = element("div", null, "process-footer");
            const elapsed = element("span", null, "process-elapsed");
            elapsed.dataset.elapsed = String(job.elapsedMillis); elapsed.dataset.queue = String(job.queueMillis);
            elapsed.dataset.running = String(active(job) && job.startedAt > 0);
            elapsed.dataset.queued = String(active(job) && !job.startedAt);
            footer.append(elapsed);
            if (active(job)) {
                const cancel = element("button", null, "process-cancel icon-btn");
                cancel.type = "button"; cancel.dataset.processCancel = job.id; cancel.disabled = !job.cancellable;
                cancel.title = job.state === "cancelling" ? "Cancelling process" : "Cancel process";
                cancel.setAttribute("aria-label", `${job.state === "cancelling" ? "Cancelling" : "Cancel"} ${job.label}`);
                cancel.append(element("span", "cancel", "material-symbols-outlined"));
                cancel.addEventListener("click", async () => {
                    cancel.disabled = true;
                    try {
                        if (job.local) await localJobs.get(job.id)?.cancel?.();
                        else await cancelJob(job.id, job.graphId);
                    }
                    catch (error) { document.getElementById("processes-status").textContent = error.message; cancel.disabled = false; }
                });
                footer.append(cancel);
            }
            card.append(footer);
            if (job.error) card.append(element("div", job.error, "process-error"));
            return card;
        }));
        if (!filteredJobs.length) list.append(element("p", !visibleJobs.length
            ? showAllGraphs ? "No processes yet. Computations will appear here."
                : "No processes for this graph. Select “Other graphs” to view processes from other graphs."
            : "No processes match these filters.", "muted"));
        if (focusedCancel) list.querySelector?.('[data-process-cancel="' + focusedCancel + '"]')?.focus();
        tick(now);
    }
    function tick(now = performance.now()) {
        document.querySelectorAll(".process-elapsed").forEach(node => {
            const delta = Math.max(0, now - received);
            const elapsed = Number(node.dataset.elapsed) + (node.dataset.running === "true" ? delta : 0);
            const queue = Number(node.dataset.queue) + (node.dataset.queued === "true" ? delta : 0);
            node.textContent = `${duration(elapsed)} running` + (queue >= 1000 ? ` · ${duration(queue)} queued` : "");
        });
    }
    async function refresh() {
        if (polling) return;
        polling = true;
        try {
            const response = await fetch("/api/jobs");
            if (!response.ok) throw new Error("Could not load processes. Retrying…");
            serverJobs = (await response.json()).jobs;
            document.getElementById("processes-status").textContent = "";
            mergeJobs();
        } catch (error) { document.getElementById("processes-status").textContent = error.message; }
        finally {
            polling = false; clearTimeout(timer);
            timer = setTimeout(refresh, jobs.some(active) || pending || !document.getElementById("processes-panel").hidden ? 1000 : 5000);
        }
    }
    async function cancelJob(jobId, graphId) {
        const response = await fetch("/api/jobs/cancel", { method: "POST", headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ jobId, graphId }) });
        if (!response.ok) throw new Error("Cancellation request failed. Please retry.");
        const result = await response.json();
        await refresh();
        return result.cancelled;
    }
    function init() {
        const button = document.getElementById("processes-button"), panel = document.getElementById("processes-panel");
        if (!button || !panel) return;
        const compactButton = document.getElementById("processes-compact");
        let compact = false;
        try { compact = localStorage.getItem("relison-processes-compact") === "true"; } catch (_) { /* Storage can be disabled. */ }
        function setCompact(value) {
            compact = value;
            panel.classList.toggle("processes-compact", compact);
            compactButton.setAttribute("aria-pressed", String(compact));
            compactButton.setAttribute("aria-label", compact ? "Use detailed view" : "Use compact view");
            compactButton.title = compact ? "Use detailed view" : "Use compact view";
            compactButton.querySelector(".material-symbols-outlined").textContent = compact ? "density_large" : "density_small";
            try { localStorage.setItem("relison-processes-compact", String(compact)); } catch (_) { /* Keep the current setting for this page. */ }
        }
        setCompact(compact);
        compactButton.addEventListener("click", () => setCompact(!compact));
        document.getElementById("processes-all-graphs").addEventListener("change", render);
        document.getElementById("process-filter-type").addEventListener("change", render);
        document.getElementById("process-filter-status").addEventListener("change", render);
        function show(open) { panel.hidden = !open; button.setAttribute("aria-expanded", String(open)); if (open) { render(); document.getElementById("processes-close").focus(); refresh(); } else button.focus(); }
        button.addEventListener("click", () => show(panel.hidden));
        document.getElementById("processes-close").addEventListener("click", () => show(false));
        panel.addEventListener("keydown", event => { if (event.key === "Escape") show(false); });
        setInterval(tick, 1000); refresh();
    }
    function registerLocal(label, graphId, cancel) {
        const now = Date.now(), job = { id: "local-" + crypto.randomUUID(), graphId, label, state: "running",
            phase: "Starting layout", completed: 0, total: -1, unit: "", progresses: [], createdAt: now,
            startedAt: now, startedPerf: performance.now(), endedAt: 0, elapsedMillis: 0, error: null, cancel };
        localJobs.set(job.id, job); trimLocalJobs(); mergeJobs();
        return job.id;
    }
    function updateLocal(id, phase, completed, total, unit) {
        const job = localJobs.get(id);
        if (!job || job.endedAt) return;
        job.phase = phase; job.completed = completed; job.total = total; job.unit = unit;
        if (total >= 0 || unit) job.progresses = [{ phase, completed, total, unit, done: false }];
        mergeJobs();
    }
    function finishLocal(id, state = "completed", error = null) {
        const job = localJobs.get(id);
        if (!job || job.endedAt) return;
        job.state = state; job.error = error; job.endedAt = Date.now();
        job.elapsedMillis = Math.max(0, performance.now() - job.startedPerf);
        if (state === "completed") {
            if (!job.progresses.length) job.progresses = [{ phase: "Completed", completed: 1, total: 1, unit: "", done: true }];
            else job.progresses.forEach(progress => { progress.done = true; });
        }
        job.cancel = null; trimLocalJobs(); mergeJobs();
    }
    return { routes, duration, progressText, processType, cancelJob, refresh, init, registerLocal, updateLocal, finishLocal,
        started() { pending++; refresh(); }, finished() { pending = Math.max(0, pending - 1); refresh(); } };
})();
document.addEventListener("DOMContentLoaded", ProcessPanel.init);
